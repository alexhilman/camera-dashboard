package com.alexhilman.cameradashboard.ui.video;

import com.alexhilman.cameradashboard.ui.Fixtures;
import com.alexhilman.cameradashboard.ui.conf.Camera;
import com.alexhilman.cameradashboard.ui.conf.CameraConfiguration;
import com.alexhilman.dlink.dcs936.model.DcsFile;
import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;

import static com.alexhilman.cameradashboard.ui.CameraConfigurationReader.readCameraConfig;
import static com.alexhilman.cameradashboard.ui.video.MovieFileManager.movieFileNameFor;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class MovieFileManagerIT {
    private MovieFileManager movieFileManager;
    private Camera camera;

    @Before
    public void setup() {
        final CameraConfiguration cameraConfiguration = readCameraConfig();
        camera = cameraConfiguration.getCameras().get(0);
        movieFileManager = new MovieFileManager(cameraConfiguration, new MovieHelper(), "/tmp/.camera-dashboard");
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

        final File cam1Movie = Fixtures.randomRealMovieFile();
        final File cam2Movie = Fixtures.randomRealMovieFile();
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

        final File cam1Movie = Fixtures.randomRealMovieFile();
        final File cam2Movie = Fixtures.randomRealMovieFile();
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

        final File cam1Movie1 = Fixtures.randomRealMovieFile();
        final File cam2Movie1 = Fixtures.randomRealMovieFile();
        cam1Movie1.createNewFile();
        cam2Movie1.createNewFile();

        final File rotatingDirectory = new File(storageDirectory, "rotating");
        rotatingDirectory.mkdir();

        final File cam1Rotating = new File(rotatingDirectory, "cam1");
        final File cam2Rotating = new File(rotatingDirectory, "cam2");
        cam1Rotating.mkdirs();
        cam2Rotating.mkdirs();

        final File cam1Movie2 = Fixtures.randomRealMovieFile();
        final File cam2Movie2 = Fixtures.randomRealMovieFile();
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
        final List<DcsFile> dcsFiles =
                IntStream.range(0, 5)
                         .mapToObj(i -> Fixtures.randomDcsFile())
                         .distinct()
                         .collect(toList());

        movieFileManager.addMoviesToRotatingPool(camera, dcsFiles);

        final Instant oldestInstant = dcsFiles.stream()
                                              .sorted(Comparator.comparing(DcsFile::getCreatedInstant))
                                              .map(DcsFile::getCreatedInstant)
                                              .findFirst()
                                              .get();

        assertThat(movieFileManager.getMoviesInRange(Instant.now(), Instant.now()), is(empty()));
        assertThat(movieFileManager.getMoviesInRange(oldestInstant, Instant.now()), hasSize(5));
    }

    @Test
    public void shouldHavePosterImageWithMovie() throws IOException {
        final DcsFile file = Fixtures.randomDcsFile();
        movieFileManager.addMoviesToRotatingPool(camera, Lists.newArrayList(file));

        final List<Movie> movies = movieFileManager.getMoviesInRange(Instant.EPOCH, Instant.now());
        assertThat(movies, hasSize(1));

        assertThat(movies.get(0).getPosterImageFile(), is(notNullValue()));
        assertThat(movies.get(0).getPosterImageFile().exists(), is(true));
    }

    @Test
    public void shouldFindMovieForCamera() {
        final List<DcsFile> files = IntStream.range(0, 10)
                                             .mapToObj(i -> Fixtures.randomDcsFile())
                                             .collect(toList());

        movieFileManager.addMoviesToRotatingPool(camera, files);
        final List<Movie> allMovies = movieFileManager.getMoviesInRange(Instant.EPOCH, Instant.now());
        movieFileManager.moveRotatingPoolVideoToSavedPool(allMovies.get(0));

        final Optional<Movie> optionallySavedMovie = movieFileManager.findMovie(camera, allMovies.get(0).getName());
        assertThat(optionallySavedMovie.isPresent(), is(true));

        final Movie savedMovie = optionallySavedMovie.get();
        assertThat(savedMovie.getName(), is(allMovies.get(0).getName()));
        assertThat(savedMovie.getMovieFile().getParentFile().getParentFile().getName(), is("saved"));

        assertThat(movieFileManager.findMovie(camera, allMovies.get(4).getName()).get(), is(allMovies.get(4)));
    }
}