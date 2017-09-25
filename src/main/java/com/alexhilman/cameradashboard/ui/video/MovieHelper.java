package com.alexhilman.cameradashboard.ui.video;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_objdetect;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 */
public class MovieHelper {
    static {
        Loader.load(opencv_objdetect.class); // documented hack :\ <barf/>
    }

    public byte[] grabJpgFrame(final File movieFile, final int frameTimestampMillis) {
        try (final FFmpegFrameGrabber frameGrabber = FFmpegFrameGrabber.createDefault(movieFile)) {
            frameGrabber.start();
            frameGrabber.setTimestamp(frameTimestampMillis);
            final Java2DFrameConverter converter = new Java2DFrameConverter();
            final BufferedImage image = converter.convert(frameGrabber.grabFrame());
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", output);

            return output.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Duration runningLengthFor(final Movie movie) {
        checkNotNull(movie, "movie cannot be null");

        try (final FFmpegFrameGrabber frameGrabber = FFmpegFrameGrabber.createDefault(movie.getMovieFile())) {
            frameGrabber.start();
            final long runningLength = frameGrabber.getLengthInTime();
            return Duration.of(runningLength, ChronoUnit.MICROS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
