package com.alexhilman.cameradashboard.ui.driver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_objdetect;
import org.bytedeco.javacpp.opencv_video;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Base64;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.cvCountNonZero;

/**
 */
public class StreamingDriver {
    private static final Logger LOG = LogManager.getLogger(StreamingDriver.class);

    static {
        Loader.load(opencv_objdetect.class); // documented hack :\ <barf/>
    }

    private static final DecimalFormat decimalFormat = new DecimalFormat("000.0000");
    private static final NumberFormat intFormat = new DecimalFormat("000,000");

    private final URL streamingUrl;
    private final String username;
    private final String password;

    public StreamingDriver(final URL streamingUrl, final String username, final String password) {
        this.streamingUrl = checkNotNull(streamingUrl, "streamingUrl cannot be null");
        this.username = username;
        this.password = password;
    }

    public void start() {
        try (final InputStream inputStream = openStreamToCamera();
             final FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputStream)) {
            grabber.start();

            final OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
            grabber.setFrameNumber(0);
            opencv_core.IplImage grab = converter.convert(grabber.grab());
            int width = grab.width();
            int height = grab.height();
            final int pixelsPerFrame = width * height;

            final IntegerSampler integerSampler = IntegerSampler.forSamples(5);

            try (opencv_core.IplImage fgMask = opencv_core.IplImage.create(width, height, IPL_DEPTH_8U, 1);
                 opencv_core.IplImage background = opencv_core.IplImage.create(width, height, IPL_DEPTH_8U, 3);
                 opencv_video.BackgroundSubtractorMOG2 mog = opencv_video.createBackgroundSubtractorMOG2(300,
                                                                                                         64,
                                                                                                         false)) {
                mog.setNMixtures(3);

                while ((grab = converter.convert(grabber.grab())) != null) {
                    try (final opencv_core.Mat imageMat = new opencv_core.Mat(grab);
                         final opencv_core.Mat fgMaskMat = new opencv_core.Mat(fgMask);
                         final opencv_core.Mat backgroundMat = new opencv_core.Mat(background)) {
                        mog.apply(imageMat, fgMaskMat, .1);  // -1);

                        mog.getBackgroundImage(backgroundMat);
                        final int nonZero = cvCountNonZero(fgMask);
                        integerSampler.sample(nonZero);
                        final int average = integerSampler.average();
                        final double percentMotion = (double) average / (double) pixelsPerFrame * 100.0;
                        System.out.println("Timeline " + decimalFormat.format(grabber.getTimestamp() / 1000000.0) +
                                                   ": average motion pixels: " + intFormat.format(average) +
                                                   "; pixels in motion: " + decimalFormat.format(percentMotion) + "%");

                        // If int average of motion pixels is > 1% then we start recording
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream openStreamToCamera() throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) streamingUrl.openConnection();
        if (username != null && password != null) {
            String userpass = username + ":" + password;
            String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
            connection.setRequestProperty("Authorization", basicAuth);
        }

        LOG.info("Opening stream to camera");
        return connection.getInputStream();
    }
}
