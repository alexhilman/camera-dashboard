package com.alexhilman.cameradashboard.ui.video;

import com.alexhilman.cameradashboard.ui.Fixtures;
import com.alexhilman.cameradashboard.ui.conf.Camera;
import com.alexhilman.cameradashboard.ui.conf.CameraConfiguration;
import com.alexhilman.dlink.dcs936.model.DcsFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.alexhilman.cameradashboard.ui.video.MovieFileManager.movieFileNameFor;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class MovieFileManagerIT {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private MovieFileManager movieFileManager;

    @Before
    public void setup() {
        movieFileManager = new MovieFileManager(readCameraConfig(), new MovieHelper(), "/tmp/.camera-dashboard");
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
        final ArrayList<File> expectedFiles = Lists.newArrayList(cam1Movie, cam2Movie);

        final List<Movie> files = movieFileManager.listRotatingMovies();

        assertThat(files, is(notNullValue()));
        files.forEach(f -> {
            assertThat(expectedFiles, hasItem(f.getMovieFile()));
        });
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
        final ArrayList<File> expectedFiles = Lists.newArrayList(cam1Movie, cam2Movie);

        final List<Movie> files = movieFileManager.listSavedMovies();

        assertThat(files, is(notNullValue()));
        files.forEach(f -> {
            assertThat(expectedFiles, hasItem(f.getMovieFile()));
        });
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
        final ArrayList<File> expectedFiles = Lists.newArrayList(cam1Movie1, cam2Movie1, cam1Movie2, cam2Movie2);

        final List<Movie> files = movieFileManager.listAllMovies();

        assertThat(files, is(notNullValue()));
        files.forEach(f -> {
            assertThat(expectedFiles, hasItem(f.getMovieFile()));
        });
    }

    @Test
    public void shouldAddMovie() throws IOException {
        final Camera camera = readCameraConfig().getCameras().get(0);
        final DcsFile dcsFile = Fixtures.randomDcsFile();
        movieFileManager.addMoviesToRotatingPool(camera,
                                                 Lists.newArrayList(dcsFile));

        final List<Movie> movies = movieFileManager.listAllMovies();
        assertThat(movies, hasSize(1));
        assertThat(movies.get(0).getMovieFile().getAbsolutePath(),
                   endsWith("/rotating/" + camera.getName() + "/" +
                                    movieFileNameFor(dcsFile.getCreatedInstant(), "mp4")));
        assertThat(movies.get(0).getMovieFile().exists(), is(true));
    }

    @Test
    public void shouldSaveMovieMovingItFromRotatingToSavedStorage() throws IOException {
        final Camera camera = readCameraConfig().getCameras().get(0);
        final DcsFile dcsFile = Fixtures.randomDcsFile();
        movieFileManager.addMoviesToRotatingPool(camera,
                                                 Lists.newArrayList(dcsFile));

        final List<Movie> rotatingMovies = movieFileManager.listRotatingMovies();
        assertThat(rotatingMovies, hasSize(1));

        movieFileManager.moveRotatingPoolVideoToSavedPool(rotatingMovies.get(0));
        final List<Movie> savedMovies = movieFileManager.listSavedMovies();
        assertThat(savedMovies, hasSize(1));
        assertThat(savedMovies.get(0).getMovieFile().getAbsolutePath(),
                   endsWith("/saved/" + camera.getName() + "/" +
                                    movieFileNameFor(dcsFile.getCreatedInstant(), "mp4")));
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
                 movieFileNameFor(expectedInstant, "mp4")).createNewFile();

        final Instant lastMovieInstant = movieFileManager.lastMovieInstantFor(camera);
        assertThat(lastMovieInstant, is(expectedInstant));

        expectedInstant = Instant.now().minus(10, DAYS);
        new File(savedStorageDirectory,
                 movieFileNameFor(expectedInstant, "mp4")).createNewFile();

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

    @Test
    public void shouldListMoviesSinceInstant() throws IOException {
        final List<File> expectedFiles = Lists.newArrayList();

        final CameraConfiguration cameraConfiguration = readCameraConfig();
        final File unknownStorage = new File(movieFileManager.getRotatingDirectory(), "unknowncam1");
        unknownStorage.mkdir();

        final Instant threeHoursAgo = Instant.now().minus(3, HOURS);
        assertThat(movieFileManager.getMoviesSince(threeHoursAgo), is(empty()));

        new File(unknownStorage, movieFileNameFor(Instant.EPOCH, "mp4")).createNewFile();
        assertThat(movieFileManager.getMoviesSince(threeHoursAgo), is(empty()));

        final File rotatingDirectoryForCamera =
                movieFileManager.getRotatingDirectoryForCamera(cameraConfiguration.getCameras().get(0));
        File file = new File(rotatingDirectoryForCamera, movieFileNameFor(Instant.now(), "mp4"));
        file.createNewFile();
        expectedFiles.add(file);
        assertThat(movieFileManager.getMoviesSince(threeHoursAgo), contains(file));

        new File(unknownStorage, movieFileNameFor(Instant.now(), "jpg")).createNewFile();
        assertThat(movieFileManager.getMoviesSince(threeHoursAgo), contains(file));

        file = new File(unknownStorage, movieFileNameFor(Instant.now(), "mp4"));
        file.createNewFile();
        expectedFiles.add(file);
        assertThat(movieFileManager.getMoviesSince(threeHoursAgo), containsInAnyOrder(expectedFiles.toArray()));

        final File savedDirectory =
                movieFileManager.getSavedDirectoryForCamera(cameraConfiguration.getCameras().get(0));
        file = new File(savedDirectory, movieFileNameFor(threeHoursAgo, "mp4"));
        file.createNewFile();
        expectedFiles.add(file);
        assertThat(movieFileManager.getMoviesSince(threeHoursAgo), containsInAnyOrder(expectedFiles.toArray()));
    }

    @Test
    public void shouldHavePosterImageWithMovie() throws IOException {
        final File storageDirectory = movieFileManager.getStorageDirectory();

        final File savedDirectory = new File(storageDirectory, "saved");
        savedDirectory.mkdir();

        final File cam1 = new File(savedDirectory, "cam1");
        cam1.mkdirs();

        final File cam1Movie = new File(cam1, "2017-01-01 00:00:00.000.mov");
        cam1Movie.createNewFile();

        final List<Movie> files = movieFileManager.listSavedMovies();

        assertThat(files, is(notNullValue()));
        assertThat(files.get(0).getPosterImageFile().exists(), is(true));
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