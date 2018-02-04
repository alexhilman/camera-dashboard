package com.alexhilman.cameradashboard.ui.driver;

import com.alexhilman.cameradashboard.ui.conf.Camera;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;

import static com.alexhilman.cameradashboard.ui.CameraConfigurationReader.readCameraConfig;

public class MotionProcessorIT {
    private static final Logger LOG = LogManager.getLogger(MotionProcessorIT.class);
    private MotionProcessor motionProcessor;
    private Camera camera;

    @Before
    public void setup() throws MalformedURLException {
        camera = readCameraConfig().getCameras().get(0);
        motionProcessor = new MotionProcessor(camera, new File(System.getProperty("tmp.dir")));
    }

    @Ignore("Could identify myself; not committing video")
    @Test
    public void shouldSortMoviesWithMotion() throws Exception {
        motionProcessor.processStream();
    }
}