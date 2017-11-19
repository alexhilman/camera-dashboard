package com.alexhilman.cameradashboard.ui.driver;

import com.alexhilman.cameradashboard.ui.conf.Camera;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import static com.alexhilman.cameradashboard.ui.CameraConfigurationReader.readCameraConfig;

public class StreamingDriverIT {
    private static final Logger LOG = LogManager.getLogger(StreamingDriverIT.class);
    private StreamingDriver streamingDriver;

    @Before
    public void setup() throws MalformedURLException {
        final Camera camera = readCameraConfig().getCameras().get(0);
        streamingDriver = new StreamingDriver(new URL(camera.getNetworkAddress()),
                                              camera.getUsername(),
                                              camera.getPassword());

    }

    @Ignore("Could identify myself; not committing video")
    @Test
    public void shouldSortMoviesWithMotion() throws MalformedURLException {
        streamingDriver.start();
    }

    private void recursivelyDelete(final File file) {
        if (!file.exists()) {
            return;
        }
        final File[] files = file.listFiles();
        if (files == null) {
            if (!file.delete()) {
                throw new IllegalStateException("Could not delete file: " + file.getAbsolutePath());
            }
            return;
        }

        Arrays.stream(files)
              .forEach(this::recursivelyDelete);
        if (!file.delete()) {
            throw new IllegalStateException("Could not delete file: " + file.getAbsolutePath());
        }
    }
}