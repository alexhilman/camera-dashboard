package com.alexhilman.cameradashboard.ui.video;

import com.alexhilman.cameradashboard.ui.Fixtures;
import com.alexhilman.cameradashboard.ui.conf.Camera;
import com.alexhilman.dlink.dcs936.model.DcsFile;
import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MovieFileManagerTest {
    private MovieFileManager movieFileManager;
    private Camera camera;
    private List<DcsFile> mockFiles;

    @Before
    public void setup() {
        movieFileManager = new MovieFileManager("/tmp/.camera-dashboard");

        camera = mock(Camera.class);
        when(camera.getName()).thenReturn("cam1");

        mockFiles = IntStream.range(0, 5)
                             .mapToObj(i -> Fixtures.randomDcsFile())
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
        assertThat(Lists.newArrayList(rotatingDirectoryForCamera.listFiles()), hasSize(5));
    }
}