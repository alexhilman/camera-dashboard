package com.alexhilman.cameradashboard.ui.video;

import com.alexhilman.cameradashboard.ui.conf.Camera;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static com.alexhilman.cameradashboard.ui.CameraConfigurationReader.readCameraConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

@Ignore
public class CameraWatcherIT {
    private MovieFileManager movieFileManager;
    private CameraWatcher cameraWatcher;

    @Before
    public void setup() {
        movieFileManager = new MovieFileManager(readCameraConfig(), new MovieHelper(), "/tmp/.cameradashboard");

        cameraWatcher = new CameraWatcher(readCameraConfig(), movieFileManager);
    }

    @After
    public void tearDown() {
        recurseDelete(movieFileManager.getStorageDirectory());
    }

    private void recurseDelete(final File file) {
        final File[] files = file.listFiles();
        if (files == null) {
            if (!file.delete()) {
                throw new IllegalStateException("Could not delete file: " + file.getAbsolutePath());
            }
            return;
        }

        Arrays.stream(files)
              .forEach(this::recurseDelete);
        if (!file.delete()) {
            throw new IllegalStateException("Could not delete file: " + file.getAbsolutePath());
        }
    }

    @Test
    public void shouldGetCameras() {
        final List<Camera> cameras = cameraWatcher.getCameras();

        assertThat(cameras, hasSize(1));
    }
}