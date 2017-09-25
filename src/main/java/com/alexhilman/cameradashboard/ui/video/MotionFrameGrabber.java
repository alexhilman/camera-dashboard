package com.alexhilman.cameradashboard.ui.video;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_objdetect;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 */
public class MotionFrameGrabber {
    public byte[] grabJpgFrame(final File movieFile, final int frameTimestampMillis) throws Exception {
        Loader.load(opencv_objdetect.class);

        try (final FFmpegFrameGrabber frameGrabber = FFmpegFrameGrabber.createDefault(movieFile)) {
            frameGrabber.start();
            frameGrabber.setTimestamp(frameTimestampMillis);
            final Java2DFrameConverter converter = new Java2DFrameConverter();
            final BufferedImage image = converter.convert(frameGrabber.grabFrame());
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", output);

            return output.toByteArray();
        }
    }
}
