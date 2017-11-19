package com.alexhilman.cameradashboard.ui.video;

import com.alexhilman.cameradashboard.ui.conf.Camera;
import com.alexhilman.cameradashboard.ui.conf.CameraConfiguration;
import com.alexhilman.dlink.dcs936.model.DcsFile;
import com.alexhilman.dlink.helper.IOStreams;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.reactivex.Flowable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Named;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

/**
 */
@Singleton
public class MovieFileManager {
    public static final DateTimeFormatter STORAGE_FILE_DATET_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Logger LOG = LogManager.getLogger(MovieFileManager.class);
    private final CameraConfiguration cameraConfiguration;
    private final MovieHelper movieHelper;
    private final File storageDirectory;
    private final File rotatingDirectory;
    private final File savedDirectory;

    @Inject
    public MovieFileManager(final CameraConfiguration cameraConfiguration,
                            final MovieHelper movieHelper,
                            @Named("cameradashboard.video.location") final String storageDirectory) {
        this.cameraConfiguration = cameraConfiguration;
        this.movieHelper = movieHelper;
        this.storageDirectory = new File(checkNotNull(storageDirectory, "storageDirectory cannot be null"));

        mkDirsIfMissing(this.storageDirectory);

        rotatingDirectory = new File(storageDirectory, "rotating");
        mkDirsIfMissing(rotatingDirectory);

        savedDirectory = new File(storageDirectory, "saved");
        mkDirsIfMissing(savedDirectory);
    }

    public File getStorageDirectory() {
        return storageDirectory;
    }

    /**
     * Gets all video files saved in the directories.
     * <p>
     * Structure:
     * <pre>
     * {@code
     * rootDir
     * |-- saved
     *     |-- cam1
     *         |-- 2017-01-01 00:00:00.000.mov
     *         |-- 2017-01-01 00:00:00.000.jpg
     *         |-- ...
     *     |-- cam2
     *         |-- 2017-01-01 00:01:00.000.mov
     *         |-- 2017-01-01 00:01:00.000.jpg
     *         |-- ...
     * |-- rotating
     *     |-- cam1
     *         |-- 2017-10-01 00:00:00.000.mov
     *         |-- 2017-10-01 00:00:00.000.jpg
     *         |-- ...
     *     |-- cam2
     *         |-- 2017-11-01 00:01:00.000.mov
     *         |-- 2017-11-01 00:01:00.000.jpg
     *         |-- ...
     * }
     * </pre>
     *
     * @return List of saved movies
     */
    List<Movie> listAllMovies() {
        final ArrayList<Movie> files = Lists.newArrayList(listRotatingMovies());
        files.addAll(listSavedMovies());

        return files.stream()
                    .sorted(Comparator.comparing(Movie::getName))
                    .collect(toList());
    }

    List<Movie> listRotatingMovies() {
        final File[] firstDirectory = storageDirectory.listFiles();
        if (firstDirectory == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(firstDirectory)
                     .filter(f -> f.getName().equalsIgnoreCase("rotating"))
                     .findFirst()
                     .map(this::listCameraMovies)
                     .orElse(Collections.emptyList());
    }

    List<Movie> listSavedMovies() {
        final File[] firstDirectory = storageDirectory.listFiles();
        if (firstDirectory == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(firstDirectory)
                     .filter(f -> f.getName().equalsIgnoreCase("saved"))
                     .findFirst()
                     .map(this::listCameraMovies)
                     .orElse(Collections.emptyList());
    }

    public void addMoviesToRotatingPool(final Camera camera,
                                        final List<DcsFile> files) {
        checkNotNull(camera, "camera cannot be null");
        checkNotNull(files, "files cannot be null");

        final File rotatingPool = getRotatingDirectoryForCamera(camera);
        final File tmpDir = getTempFolderForCamera(camera);

        final int smallBatchSize = Runtime.getRuntime().availableProcessors();

        final Queue<DcsFile> remaining = Lists.newLinkedList(files);
        while (!remaining.isEmpty()) {
            Flowable.fromIterable(takeFromQueue(smallBatchSize, remaining))
                    .parallel()
                    .map(dcsFile -> {
                        final File tmpFile =
                                new File(tmpDir,
                                         movieFileNameFor(dcsFile.getCreatedInstant(),
                                                          extensionForFileName(dcsFile.getFileName())));

                        if (tmpFile.exists()) {
                            if (!tmpFile.delete()) {
                                throw new IllegalArgumentException("Could not delete old tmp file: " + tmpFile.getAbsolutePath());
                            }
                        }

                        if (!tmpFile.createNewFile()) {
                            throw new RuntimeException("Could not create tmp file: " + tmpFile.getAbsolutePath());
                        }

                        LOG.info("Downloading {} to temporary location", dcsFile.getAbsoluteFileName());

                        try (final FileOutputStream outputStream = new FileOutputStream(tmpFile);
                             final InputStream inputStream = dcsFile.open()) {
                            IOStreams.redirect(inputStream, outputStream);
                        } catch (Exception e) {
                            throw new RuntimeException("Could not save file: " + dcsFile.getAbsoluteFileName(), e);
                        }

                        return tmpFile;
                    })
                    .sorted(Comparator.comparing(File::getName))
                    .toList()
                    .blockingGet()
                    .forEach(tmpFile -> {
                        final File newFile = new File(rotatingPool, tmpFile.getName());
                        LOG.info("Adding new movie {} to rotating pool for camera {} at {}",
                                 tmpFile.getName(),
                                 camera.getName(),
                                 camera.getNetworkAddress());

                        if (!tmpFile.renameTo(newFile)) {
                            throw new RuntimeException("Could not move file to rotating pool");
                        }

                        final File posterImageFile = getPosterImageFileFrom(newFile);
                        if (!posterImageFile.exists()) {
                            throw new RuntimeException("Expected the poster image file for " + newFile.getAbsolutePath() +
                                                               " to be created, but it cannot be found");
                        }
                    });
        }
    }

    static String movieFileNameFor(final Instant fileInstant, final String fileExtension) {
        checkNotNull(fileInstant, "fileInstant cannot be null");
        checkNotNull(fileExtension, "fileExtension cannot be null");
        return fileInstant
                .atZone(ZoneId.systemDefault())
                .format(STORAGE_FILE_DATET_TIME_FORMAT) + "." + fileExtension;
    }

    private <T> List<T> takeFromQueue(final int smallBatchSize, final Queue<T> queue) {
        final List<T> list = Lists.newArrayListWithCapacity(smallBatchSize);

        for (int i = 0; i < smallBatchSize; i++) {
            final T poll = queue.poll();
            if (poll == null) break;
            list.add(poll);
        }

        return list;
    }

    private String extensionForFileName(final String fileName) {
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    void moveRotatingPoolVideoToSavedPool(final Movie movie) {
        checkNotNull(movie, "movieFile cannot be null");
        checkArgument(movie.getMovieFile().exists(), "movie file must exist");
        checkArgument(movie.getMovieFile().getAbsolutePath().contains(getRotatingDirectory().getAbsolutePath()),
                      "movie file must be in the rotating directory");

        final File cameraDir = new File(getSavedDirectory(), movie.getMovieFile().getParentFile().getName());
        mkDirsIfMissing(cameraDir);

        final File newMovieFile = new File(cameraDir, movie.getName());
        final File newPosterFile = new File(getSavedDirectory(), movie.getPosterImageFile().getName());

        if (!movie.getMovieFile().renameTo(newMovieFile)) {
            throw new RuntimeException("Could not rename/move file " +
                                               movie.getMovieFile().getAbsolutePath() + " to " +
                                               newMovieFile.getAbsolutePath());
        }

        if (!movie.getPosterImageFile().renameTo(newPosterFile)) {
            throw new RuntimeException("Could not rename/move file " +
                                               movie.getPosterImageFile().getAbsolutePath() + " to " +
                                               newPosterFile.getAbsolutePath());
        }
    }

    private List<Movie> listCameraMovies(final File rotatingDir) {
        final File[] rotatingCameraDirectories = rotatingDir.listFiles();
        if (rotatingCameraDirectories == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(rotatingCameraDirectories)
                     .map(this::listMoviesInDirectory)
                     .flatMap(List::stream)
                     .map(movieFile -> new Movie(movieFile, getPosterImageFileFrom(movieFile)))
                     .collect(toList());
    }

    private File getPosterImageFileFrom(final File movieFile) {
        assert movieFile != null;

        final File posterImageFile = new File(movieFile.getParentFile(), fileNameWithoutExtension(movieFile) + ".jpg");
        if (!posterImageFile.exists()) {
            try (final FileOutputStream out = new FileOutputStream(posterImageFile)) {
                out.write(movieHelper.grabJpgFrame(movieFile,
                                                   3000));
            } catch (Exception e) {
                throw new RuntimeException("Could not grab poster image from movie: " + movieFile.getAbsolutePath(), e);
            }
        }
        return posterImageFile;
    }

    private Camera getCameraForMovie(final File movieFile) {
        assert movieFile != null;

        final String cameraName = movieFile.getParentFile().getName();
        final Optional<Camera> camera = cameraConfiguration.getCameras()
                                                           .stream()
                                                           .filter(c -> c.getName().equals(cameraName))
                                                           .findFirst();

        if (!camera.isPresent()) {
            LOG.info(
                    "No camera configuration found for {}; using a dummy configuration instead",
                    cameraName);
            return new Camera(cameraName,
                              "http://localhost",
                              "admin",
                              "password");
        }
        return camera.get();
    }

    private List<File> listMoviesInDirectory(final File directory) {
        assert directory != null;
        assert directory.exists();

        final File[] files = directory.listFiles((dir, name) -> !name.endsWith(".jpg"));
        if (files == null) {
            return Collections.emptyList();
        }

        return Lists.newArrayList(files);
    }

    private void mkDirsIfMissing(final File dir) {
        if (!dir.exists()) {
            mkDirsOrThrow(dir);
        }
    }

    private void mkDirsOrThrow(final File directory) {
        if (!directory.mkdirs()) {
            throw new IllegalStateException("Cannot create storage directory: " + directory);
        }
    }

    File getRotatingDirectory() {
        return rotatingDirectory;
    }

    File getSavedDirectory() {
        return savedDirectory;
    }

    public Instant lastMovieInstantFor(final Camera camera) {
        final File rotatingDir = getRotatingDirectoryForCamera(camera);
        final File[] rotatingFiles = rotatingDir.listFiles();
        if (!rotatingDir.isDirectory() || rotatingFiles == null) {
            throw new IllegalStateException(rotatingDir.getAbsolutePath() + " was expected to be a directory");
        }

        final File savedDir = getSavedDirectoryForCamera(camera);
        final File[] savedFiles = savedDir.listFiles();
        if (!savedDir.isDirectory() || savedFiles == null) {
            throw new IllegalStateException(savedDir.getAbsolutePath() + " was expected to be a directory");
        }

        final File[] allFiles = new File[savedFiles.length + rotatingFiles.length];
        System.arraycopy(rotatingFiles, 0, allFiles, 0, rotatingFiles.length);
        System.arraycopy(savedFiles, 0, allFiles, rotatingFiles.length, savedFiles.length);

        final Optional<File> latestFile = Arrays.stream(allFiles)
                                                .reduce((f1, f2) -> f1.getName().compareTo(f2.getName()) > 0 ? f1 : f2);

        return latestFile.map(file -> LocalDateTime.parse(fileNameWithoutExtension(file),
                                                          STORAGE_FILE_DATET_TIME_FORMAT)
                                                   .atZone(ZoneId.systemDefault())
                                                   .toInstant())
                         .orElse(Instant.EPOCH);
    }

    File getRotatingDirectoryForCamera(final Camera camera) {
        final File file = new File(getRotatingDirectory(), camera.getName());
        mkDirsIfMissing(file);
        return file;
    }

    File getSavedDirectoryForCamera(final Camera camera) {
        final File file = new File(getSavedDirectory(), camera.getName());
        mkDirsIfMissing(file);
        return file;
    }

    private String fileNameWithoutExtension(final File file) {
        return file.getName().substring(0, file.getName().lastIndexOf('.'));
    }

    File getTempFolderForCamera(final Camera camera) {
        final File file = new File(getDownloadingDirectory(), camera.getName());
        mkDirsIfMissing(file);
        return file;
    }

    private File getDownloadingDirectory() {
        final File file = new File(storageDirectory, ".downloading");
        mkDirsIfMissing(file);
        return file;
    }

    public long getUsableSpace() {
        return storageDirectory.getUsableSpace();
    }

    public long getTotalSpace() {
        return storageDirectory.getTotalSpace();
    }

    public List<Movie> getMoviesInRange(final Instant from, final Instant to) {
        checkNotNull(from, "from cannot be null");
        checkNotNull(to, "to cannot be null");

        final String fromDateString = from.atZone(ZoneId.systemDefault()).format(STORAGE_FILE_DATET_TIME_FORMAT);
        final String toDateString = to.atZone(ZoneId.systemDefault()).format(STORAGE_FILE_DATET_TIME_FORMAT);
        return Stream.of(rotatingDirectory, savedDirectory)
                     .map(File::listFiles)
                     .flatMap(Arrays::stream)
                     .map(rotatingCameraDirs -> {
                         return Arrays.stream(rotatingCameraDirs.listFiles())
                                      .filter(file -> {
                                          return file.getName().endsWith(".mp4") &&
                                                  fromDateString.compareTo(file.getName()
                                                                               .substring(0,
                                                                                          fromDateString.length())) <= 0;
                                      })
                                      .filter(file -> {
                                          return file.getName().endsWith(".mp4") &&
                                                  toDateString.compareTo(file.getName()
                                                                             .substring(0,
                                                                                        toDateString.length())) >= 0;
                                      })
                                      .collect(toList());
                     })
                     .flatMap(List::stream)
                     .collect(toList())
                     .stream()
                     .parallel() // in case we need to generate the poster images: 4c is better than 1c
                     .map(movieFile -> new Movie(movieFile, getPosterImageFileFrom(movieFile)))
                     .sorted((m1, m2) -> m2.getDateTime().compareTo(m1.getDateTime()))
                     .collect(toList());
    }

    public Camera getCameraForMovie(final Movie movie) {
        checkNotNull(movie, "movie cannot be null");
        return getCameraForMovie(movie.getMovieFile());
    }

    public Optional<Movie> findMovie(final Camera camera, final String fileName) {
        checkNotNull(camera, "camera cannot be null");
        checkNotNull(fileName, "fileName cannot be null");

        File[] files = getSavedDirectoryForCamera(camera).listFiles((dir, name) -> name.equals(fileName));
        if (files == null || files.length == 0) {
            files = getRotatingDirectoryForCamera(camera).listFiles((dir, name) -> name.equals(fileName));
        }

        if (files != null && files.length > 0) {
            return Optional.of(new Movie(files[0], getPosterImageFileFrom(files[0])));
        }

        return Optional.empty();
    }
}
