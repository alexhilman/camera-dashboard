package com.alexhilman.cameradashboard.ui.video;

import com.alexhilman.cameradashboard.ui.Fixtures;
import com.alexhilman.cameradashboard.ui.conf.Camera;
import com.alexhilman.cameradashboard.ui.conf.CameraConfiguration;
import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static com.alexhilman.cameradashboard.ui.CameraConfigurationReader.readCameraConfig;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class MovieFileManagerTest {
    private MovieFileManager movieFileManager;
    private Camera camera;
    private List<File> mockFiles;

    @Before
    public void setup() {
        final CameraConfiguration cameraConfiguration = readCameraConfig();
        movieFileManager = new MovieFileManager(cameraConfiguration, new MovieHelper(), "/tmp/.camera-dashboard");

        camera = cameraConfiguration.getCameras().get(0);

        mockFiles = IntStream.range(0, 5)
                             .mapToObj(i -> Fixtures.randomFile())
                             .collect(toList());
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
    public void shouldGetTempFolder() {
        final File tempFolder = movieFileManager.getTempFolderForCamera(camera);

        assertThat(tempFolder, is(notNullValue()));
        assertThat(tempFolder.getAbsolutePath(), endsWith(".downloading/" + camera.getName()));
    }

    @Test
    public void shouldAddMoviesToRotatingPool() {
        movieFileManager.addMoviesToRotatingPool(camera, mockFiles);

        final File rotatingDirectoryForCamera = movieFileManager.getRotatingDirectoryForCamera(camera);
        assertThat(
                Lists.newArrayList(rotatingDirectoryForCamera.listFiles((dir, name) -> name.endsWith(".mp4"))),
                hasSize(5)
        );
    }

    @Test
    public void shouldGetCameraForMovie() {
        movieFileManager.addMoviesToRotatingPool(camera, mockFiles);

        movieFileManager.getMoviesInRange(Instant.EPOCH, Instant.now())
                        .forEach(movie -> {
                            assertThat(movieFileManager.getCameraForMovie(movie), is(camera));
                        });
    }
}