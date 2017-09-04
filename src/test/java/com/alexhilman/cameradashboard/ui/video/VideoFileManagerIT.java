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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
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

    @Test
    public void shouldListSavedCameraVideos() throws IOException {
        final File storageDirectory = videoFileManager.getStorageDirectory();

        final File savedDirectory = new File(storageDirectory, "saved");
        savedDirectory.mkdir();

        final File cam1 = new File(savedDirectory, "cam1");
        final File cam2 = new File(savedDirectory, "cam2");
        cam1.mkdirs();
        cam2.mkdirs();

        final File cam1Movie = new File(cam1, "2017-01-01 00:00:00.mov");
        final File cam2Movie = new File(cam2, "2017-01-01 00:00:00.mov");
        cam1Movie.createNewFile();
        cam2Movie.createNewFile();

        final List<File> files = videoFileManager.listSavedMovies();

        assertThat(files, is(notNullValue()));
        assertThat(files, containsInAnyOrder(cam1Movie, cam2Movie));
    }

    @Test
    public void shouldListAllMovies() throws IOException {
        final File storageDirectory = videoFileManager.getStorageDirectory();

        final File savedDirectory = new File(storageDirectory, "saved");
        savedDirectory.mkdir();

        final File cam1Saved = new File(savedDirectory, "cam1");
        final File cam2Saved = new File(savedDirectory, "cam2");
        cam1Saved.mkdirs();
        cam2Saved.mkdirs();

        final File cam1Movie1 = new File(cam1Saved, "2017-01-01 00:00:00.mov");
        final File cam2Movie1 = new File(cam2Saved, "2017-01-01 00:00:00.mov");
        cam1Movie1.createNewFile();
        cam2Movie1.createNewFile();

        final File rotatingDirectory = new File(storageDirectory, "rotating");
        rotatingDirectory.mkdir();

        final File cam1Rotating = new File(rotatingDirectory, "cam1");
        final File cam2Rotating = new File(rotatingDirectory, "cam2");
        cam1Rotating.mkdirs();
        cam2Rotating.mkdirs();

        final File cam1Movie2 = new File(cam1Rotating, "2017-01-01 00:00:00.mov");
        final File cam2Movie2 = new File(cam2Rotating, "2017-01-01 00:00:00.mov");
        cam1Movie2.createNewFile();
        cam2Movie2.createNewFile();

        final List<File> files = videoFileManager.listAllMovies();

        assertThat(files, is(notNullValue()));
        assertThat(files, containsInAnyOrder(cam1Movie2, cam2Movie2, cam1Movie1, cam2Movie1));
    }

    @Test
    public void shouldAddMovie() throws IOException {
        final File tmpfile = new File("/tmp/2017-04-01 00:00:00.mov");
        tmpfile.createNewFile();

        videoFileManager.addMovieToRotatingPool("cam1", tmpfile);

        final List<File> movies = videoFileManager.listAllMovies();
        assertThat(movies, hasSize(1));
        assertThat(movies.get(0).getAbsolutePath(), endsWith("/rotating/cam1/" + tmpfile.getName()));
        assertThat(movies.get(0).exists(), is(true));
    }

    @Test
    public void shouldSaveMovieMovingItFromRotatingToSavedStorage() throws IOException {
        final File tmpfile = new File("/tmp/2017-04-01 00:00:00.mov");
        tmpfile.createNewFile();

        videoFileManager.addMovieToRotatingPool("cam1", tmpfile);

        final List<File> rotatingMovies = videoFileManager.listRotatingMovies();
        assertThat(rotatingMovies, hasSize(1));

        videoFileManager.saveMovie(rotatingMovies.get(0));
        final List<File> savedMovies = videoFileManager.listSavedMovies();
        assertThat(savedMovies, hasSize(1));
        assertThat(savedMovies.get(0).getAbsolutePath(), endsWith("/saved/cam1/" + tmpfile.getName()));
    }
}