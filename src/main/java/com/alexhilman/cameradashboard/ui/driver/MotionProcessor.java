package com.alexhilman.cameradashboard.ui.driver;

import com.alexhilman.cameradashboard.ui.conf.Camera;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_objdetect;
import org.bytedeco.javacpp.opencv_video;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.cvCountNonZero;

/**
 */
public class MotionProcessor {
    private static final Logger LOG = LogManager.getLogger(MotionProcessor.class);
    private static final DecimalFormat decimalFormat = new DecimalFormat("000.0000");
    private static final NumberFormat intFormat = new DecimalFormat("000,000");

    static {
        Loader.load(opencv_objdetect.class); // documented hack :\ <barf/>
    }

    private final Camera camera;
    private final File tmpFolder;

    private volatile ObservableInputStream cameraStream;
    private volatile MotionCaptureListener listener;

    public MotionProcessor(final Camera camera, final File tmpFolder) {
        this.camera = camera;
        this.tmpFolder = tmpFolder;
    }

    public void processStream() throws Exception {
        try (final InputStream inputStream = openStreamToCamera();
             final FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputStream)) {
            grabber.start();

            final OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
            grabber.setFrameNumber(0);
            Frame frame = grabber.grab();
            opencv_core.IplImage grab = converter.convert(frame);
            int width = grab.width();
            int height = grab.height();
            final int pixelsPerFrame = width * height;

            final double frameRate = grabber.getFrameRate();
            LOG.debug("Frame rate: {}", frameRate);
            final int numFramesForMargin = (int) (frameRate * 3);
            final List<Frame> threeSecondsOfFrames = new LinkedList<>();
            FFmpegFrameRecorder frameRecorder = null;
            final AtomicReference<File> motionVideo = new AtomicReference<>(null);
            int cooloffFrames = 0;

            final IntegerSampler integerSampler = IntegerSampler.forSamples(5);
            long framesSampled = 0;

            try (opencv_core.IplImage fgMask = opencv_core.IplImage.create(width, height, IPL_DEPTH_8U, 1);
                 opencv_core.IplImage background = opencv_core.IplImage.create(width, height, IPL_DEPTH_8U, 3);
                 opencv_video.BackgroundSubtractorMOG2 mog =
                         opencv_video.createBackgroundSubtractorMOG2(300, 64, false)) {
                mog.setNMixtures(3);

                while ((grab = converter.convert((frame = grabber.grab()))) != null) {
                    if (threeSecondsOfFrames.size() >= numFramesForMargin) {
                        do {
                            threeSecondsOfFrames.remove(0);
                        } while (!threeSecondsOfFrames.get(0).keyFrame);
                    }
                    threeSecondsOfFrames.add(frame.clone());

                    try (final opencv_core.Mat imageMat = new opencv_core.Mat(grab);
                         final opencv_core.Mat fgMaskMat = new opencv_core.Mat(fgMask);
                         final opencv_core.Mat backgroundMat = new opencv_core.Mat(background)) {
                        mog.apply(imageMat, fgMaskMat, .1);  // -1);

                        mog.getBackgroundImage(backgroundMat);
                        final int nonZero = cvCountNonZero(fgMask);
                        integerSampler.sample(nonZero);
                        framesSampled++;
                        final int average = integerSampler.average();
                        final double percentMotion = (double) average / (double) pixelsPerFrame * 100.0;
//                        LOG.debug("Timeline {}: average motion pixels: {}; pixels in motion: {}%",
//                                  decimalFormat.format(grabber.getTimestamp() / 1000000.0),
//                                  intFormat.format(average),
//                                  decimalFormat.format(percentMotion));

                        // If int average of motion pixels is > 1% then we start recording
                        if (framesSampled > 5) {
                            try {
                                if (percentMotion > 1.0) {
                                    cooloffFrames = 0;
                                    if (frameRecorder == null) {
                                        LOG.info("Motion detected on {}", camera.getName());
                                        motionVideo.set(tmpFile());
                                        frameRecorder = new FFmpegFrameRecorder(motionVideo.get(), 0);
                                        frameRecorder.setImageWidth(width);
                                        frameRecorder.setImageHeight(height);
                                        frameRecorder.setFrameRate(frameRate);
                                        frameRecorder.setAudioChannels(0);
                                        frameRecorder.setFormat(grabber.getFormat());
                                        frameRecorder.start();

                                        while (!threeSecondsOfFrames.isEmpty()) {
                                            frameRecorder.record(threeSecondsOfFrames.remove(0));
                                        }
                                    }
                                } else {
                                    cooloffFrames++;
                                    if (cooloffFrames > numFramesForMargin) {
                                        if (frameRecorder != null) {
                                            LOG.info("Motion ceased on {}", camera.getName());
                                            frameRecorder.record(frame);
                                            frameRecorder.close();
                                        }
                                        frameRecorder = null;
                                        Optional.ofNullable(listener)
                                                .ifPresent(l -> l.motionObserved(camera, motionVideo.get()));
                                    }
                                }

                                if (frameRecorder != null) {
                                    frameRecorder.record(frame);
                                }
                            } catch (FrameRecorder.Exception e) {
                                LOG.warn("Could not motion frame to file", e);
                            }
                        }
                    }
                }
            }
        }
    }

    public MotionProcessor onMotionCaptured(final MotionCaptureListener listener) {
        this.listener = checkNotNull(listener, "listener cannot be null");
        return this;
    }

    public InputStream observeStream() {
        final ObservableInputStream cameraStream = this.cameraStream;
        if (cameraStream == null) {
            throw new RuntimeException("Camera stream is not operational");
        }
        try {
            return cameraStream.newObserver();
        } catch (IOException e) {
            throw new RuntimeException("Cannot spawn observer", e);
        }
    }

    private File tmpFile() {
        return new File(tmpFolder, UUID.randomUUID() + ".mp4");
    }

    private InputStream openStreamToCamera() throws IOException {
        final HttpURLConnection connection =
                (HttpURLConnection) camera.getStreams()
                                          .stream()
                                          .min(Comparator.comparingInt(o -> o.getQuality().ordinal()))
                                          .orElseThrow(() -> new RuntimeException("No streams available for " + camera.getName()))
                                          .getUrl()
                                          .openConnection();
        if (camera.getUsername() != null && camera.getPassword() != null) {
            String userpass = camera.getUsername() + ":" + camera.getPassword();
            String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
            connection.setRequestProperty("Authorization", basicAuth);
        }

        LOG.info("Opening stream to camera {}", camera.getName());
        cameraStream = new ObservableInputStream(connection.getInputStream());
        return cameraStream;
    }

    @FunctionalInterface
    public interface MotionCaptureListener {
        void motionObserved(final Camera camera, final File motionVideo);
    }
}
