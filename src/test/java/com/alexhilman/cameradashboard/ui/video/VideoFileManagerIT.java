package com.alexhilman.cameradashboard.ui.video;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class VideoFileManagerIT {
    private VideoFileManager videoFileManager;

    @Before
    public void setup() {
        videoFileManager = new VideoFileManager("/tmp/.cameradashboard");
    }

    @After
    public void tearDown() {
        recurseDelete(videoFileManager.getStorageDirectory());
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
    public void shouldGetStorageDirectory() {
        final File dir = videoFileManager.getStorageDirectory();

        assertThat(dir, is(notNullValue()));
    }

    @Test
    public void shouldListRotatingCameraVideos() throws IOException {
        final File storageDirectory = videoFileManager.getStorageDirectory();

        final File rotatingDirectory = new File(storageDirectory, "rotating");
        rotatingDirectory.mkdir();

        final File cam1 = new File(rotatingDirectory, "cam1");
        final File cam2 = new File(rotatingDirectory, "cam2");
        cam1.mkdirs();
        cam2.mkdirs();

        final File cam1Movie = new File(cam1, "2017-01-01 00:00:00.mov");
        final File cam2Movie = new File(cam2, "2017-01-01 00:00:00.mov");
        cam1Movie.createNewFile();
        cam2Movie.createNewFile();

        final List<File> files = videoFileManager.listRotatingMovies();

        assertThat(files, is(notNullValue()));
        assertThat(files, containsInAnyOrder(cam1Movie, cam2Movie));
    }
}