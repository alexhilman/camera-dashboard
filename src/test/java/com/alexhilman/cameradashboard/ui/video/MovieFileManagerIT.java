package com.alexhilman.cameradashboard.ui.video;

import com.alexhilman.cameradashboard.ui.conf.Camera;
import com.alexhilman.cameradashboard.ui.conf.CameraConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import static com.alexhilman.cameradashboard.ui.video.MovieFileManager.STORAGE_FILE_DATET_TIME_FORMAT;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class MovieFileManagerIT {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private MovieFileManager movieFileManager;

    @Before
    public void setup() {
        movieFileManager = new MovieFileManager("/tmp/.cameradashboard");
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
    public void shouldGetStorageDirectory() {
        final File dir = movieFileManager.getStorageDirectory();

        assertThat(dir, is(notNullValue()));
    }

    @Test
    public void shouldListRotatingCameraVideos() throws IOException {
        final File storageDirectory = movieFileManager.getStorageDirectory();

        final File rotatingDirectory = new File(storageDirectory, "rotating");
        rotatingDirectory.mkdir();

        final File cam1 = new File(rotatingDirectory, "cam1");
        final File cam2 = new File(rotatingDirectory, "cam2");
        cam1.mkdirs();
        cam2.mkdirs();

        final File cam1Movie = new File(cam1, "2017-01-01 00:00:00.000.mov");
        final File cam2Movie = new File(cam2, "2017-01-01 00:00:00.000.mov");
        cam1Movie.createNewFile();
        cam2Movie.createNewFile();

        final List<File> files = movieFileManager.listRotatingMovies();

        assertThat(files, is(notNullValue()));
        assertThat(files, containsInAnyOrder(cam1Movie, cam2Movie));
    }

    @Test
    public void shouldListSavedCameraVideos() throws IOException {
        final File storageDirectory = movieFileManager.getStorageDirectory();

        final File savedDirectory = new File(storageDirectory, "saved");
        savedDirectory.mkdir();

        final File cam1 = new File(savedDirectory, "cam1");
        final File cam2 = new File(savedDirectory, "cam2");
        cam1.mkdirs();
        cam2.mkdirs();

        final File cam1Movie = new File(cam1, "2017-01-01 00:00:00.000.mov");
        final File cam2Movie = new File(cam2, "2017-01-01 00:00:00.000.mov");
        cam1Movie.createNewFile();
        cam2Movie.createNewFile();

        final List<File> files = movieFileManager.listSavedMovies();

        assertThat(files, is(notNullValue()));
        assertThat(files, containsInAnyOrder(cam1Movie, cam2Movie));
    }

    @Test
    public void shouldListAllMovies() throws IOException {
        final File storageDirectory = movieFileManager.getStorageDirectory();

        final File savedDirectory = new File(storageDirectory, "saved");
        savedDirectory.mkdir();

        final File cam1Saved = new File(savedDirectory, "cam1");
        final File cam2Saved = new File(savedDirectory, "cam2");
        cam1Saved.mkdirs();
        cam2Saved.mkdirs();

        final File cam1Movie1 = new File(cam1Saved, "2017-01-01 00:00:00.000.mov");
        final File cam2Movie1 = new File(cam2Saved, "2017-01-01 00:00:00.000.mov");
        cam1Movie1.createNewFile();
        cam2Movie1.createNewFile();

        final File rotatingDirectory = new File(storageDirectory, "rotating");
        rotatingDirectory.mkdir();

        final File cam1Rotating = new File(rotatingDirectory, "cam1");
        final File cam2Rotating = new File(rotatingDirectory, "cam2");
        cam1Rotating.mkdirs();
        cam2Rotating.mkdirs();

        final File cam1Movie2 = new File(cam1Rotating, "2017-01-01 00:00:00.000.mov");
        final File cam2Movie2 = new File(cam2Rotating, "2017-01-01 00:00:00.000.mov");
        cam1Movie2.createNewFile();
        cam2Movie2.createNewFile();

        final List<File> files = movieFileManager.listAllMovies();

        assertThat(files, is(notNullValue()));
        assertThat(files, containsInAnyOrder(cam1Movie2, cam2Movie2, cam1Movie1, cam2Movie1));
    }

    @Test
    public void shouldAddMovie() throws IOException {
        final File tmpfile = new File("/tmp/2017-04-01 00:00:00.000.mov");
        tmpfile.createNewFile();

        movieFileManager.addMovieToRotatingPool("cam1", tmpfile);

        final List<File> movies = movieFileManager.listAllMovies();
        assertThat(movies, hasSize(1));
        assertThat(movies.get(0).getAbsolutePath(), endsWith("/rotating/cam1/" + tmpfile.getName()));
        assertThat(movies.get(0).exists(), is(true));
    }

    @Test
    public void shouldSaveMovieMovingItFromRotatingToSavedStorage() throws IOException {
        final File tmpfile = new File("/tmp/2017-04-01 00:00:00.000.mov");
        tmpfile.createNewFile();

        movieFileManager.addMovieToRotatingPool("cam1", tmpfile);

        final List<File> rotatingMovies = movieFileManager.listRotatingMovies();
        assertThat(rotatingMovies, hasSize(1));

        movieFileManager.saveMovie(rotatingMovies.get(0));
        final List<File> savedMovies = movieFileManager.listSavedMovies();
        assertThat(savedMovies, hasSize(1));
        assertThat(savedMovies.get(0).getAbsolutePath(), endsWith("/saved/cam1/" + tmpfile.getName()));
    }

    @Test
    public void shouldFindInstantOfLastMovieForCamera() throws IOException {
        final CameraConfiguration cameraConfiguration = readCameraConfig();
        final Camera camera = cameraConfiguration.getCameras().get(0);
        final File rotatingStorageDirectory = movieFileManager.getRotatingDirectoryForCamera(camera);
        final File savedStorageDirectory = movieFileManager.getSavedDirectoryForCamera(camera);
        rotatingStorageDirectory.mkdirs();
        savedStorageDirectory.mkdirs();

        Instant expectedInstant = Instant.now().minus(365, DAYS);
        new File(rotatingStorageDirectory, "2015-01-01 00:00:00.000.mp4").createNewFile();
        new File(rotatingStorageDirectory,
                 expectedInstant.atZone(ZoneId.systemDefault())
                                .format(STORAGE_FILE_DATET_TIME_FORMAT) + ".mp4").createNewFile();

        final Instant lastMovieInstant = movieFileManager.lastMovieInstantFor(camera);
        assertThat(lastMovieInstant, is(expectedInstant));

        expectedInstant = Instant.now().minus(10, DAYS);
        new File(savedStorageDirectory,
                 expectedInstant.atZone(ZoneId.systemDefault())
                                .format(STORAGE_FILE_DATET_TIME_FORMAT) + ".mp4").createNewFile();

        final Instant lastSavedMovieInstant = movieFileManager.lastMovieInstantFor(camera);
        assertThat(lastSavedMovieInstant, is(expectedInstant));
    }

    @Test
    public void shouldReturnEpochForCameraWhichIsMissingFiles() {
        final CameraConfiguration cameraConfiguration = readCameraConfig();
        final Camera camera = cameraConfiguration.getCameras().get(0);

        final Instant instant = movieFileManager.lastMovieInstantFor(camera);
        assertThat(instant, is(Instant.EPOCH));
    }

    private CameraConfiguration readCameraConfig() {
        final CameraConfiguration cameraConfiguration;
        try {
            cameraConfiguration =
                    OBJECT_MAPPER.readValue(getClass().getResource("/com/alexhilman/cameradashboard/ui/cameras.json"),
                                            CameraConfiguration.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return cameraConfiguration;
    }
}