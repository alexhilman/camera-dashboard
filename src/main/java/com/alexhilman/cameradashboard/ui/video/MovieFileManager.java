package com.alexhilman.cameradashboard.ui.video;

import com.alexhilman.cameradashboard.ui.conf.Camera;
import com.alexhilman.dlink.helper.IOStreams;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

/**
 */
@Singleton
public class MovieFileManager {
    private static final Logger LOG = LogManager.getLogger(MovieFileManager.class);

    static final DateTimeFormatter STORAGE_FILE_DATET_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private final File storageDirectory;
    private final File rotatingDirectory;
    private final File savedDirectory;

    @Inject
    public MovieFileManager(final String storageDirectory) {
        this.storageDirectory = new File(checkNotNull(storageDirectory, "storageDirectory cannot be null"));

        mkDirsIfMissing(this.storageDirectory);

        rotatingDirectory = new File(storageDirectory, "rotating");
        mkDirsIfMissing(rotatingDirectory);

        savedDirectory = new File(storageDirectory, "saved");
        mkDirsIfMissing(savedDirectory);
    }

    File getStorageDirectory() {
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
     *         |-- ...
     *     |-- cam2
     *         |-- 2017-01-01 00:01:00.000.mov
     *         |-- ...
     * |-- rotating
     *     |-- cam1
     *         |-- 2017-10-01 00:00:00.000.mov
     *         |-- ...
     *     |-- cam2
     *         |-- 2017-11-01 00:01:00.000.mov
     *         |-- ...
     * }
     * </pre>
     *
     * @return List of saved movies
     */
    List<File> listAllMovies() {
        final ArrayList<File> files = Lists.newArrayList(listRotatingMovies());
        files.addAll(listSavedMovies());

        return files.stream()
                    .sorted(Comparator.comparing(File::getName))
                    .collect(toList());
    }

    List<File> listRotatingMovies() {
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

    List<File> listSavedMovies() {
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

    public void addMovieToRotatingPool(final Camera camera,
                                       final InputStream inputStream,
                                       final String fileExtension,
                                       final Instant fileInstant) {
        checkNotNull(camera, "camera cannot be null");
        checkNotNull(inputStream, "inputStream cannot be null");
        checkNotNull(fileExtension, "fileExtension cannot be null");
        checkNotNull(fileInstant, "fileInstant cannot be null");

        final File cameraDir = getRotatingDirectoryForCamera(camera);

        final File newFile = new File(cameraDir,
                                      fileInstant.atZone(ZoneId.systemDefault())
                                                 .format(STORAGE_FILE_DATET_TIME_FORMAT) + "." + fileExtension);

        if (newFile.exists()) {
            throw new IllegalArgumentException("Movie file already exists in the camera storage directory: " + newFile.getAbsolutePath());
        }

        LOG.info("Adding new movie {} to rotating pool for camera {} at {}",
                 newFile.getName(),
                 camera.getName(),
                 camera.getNetworkAddress());

        try (final FileOutputStream outputStream = new FileOutputStream(newFile)) {
            IOStreams.redirect(inputStream, outputStream);
        } catch (Exception e) {
            throw new RuntimeException("Could not save file", e);
        }
    }

    public void saveMovie(final File movieFile) {
        checkNotNull(movieFile, "movieFile cannot be null");
        checkArgument(movieFile.exists(), "movieFile must exist");
        checkArgument(movieFile.getAbsolutePath().contains(getRotatingDirectory().getAbsolutePath()),
                      "movieFile must be in the rotating directory");

        final File cameraDir = new File(getSavedDirectory(), movieFile.getParentFile().getName());
        mkDirsIfMissing(cameraDir);

        final File newFile = new File(getSavedDirectory(),
                                      movieFile.getParentFile().getName() + "/" + movieFile.getName());

        if (!movieFile.renameTo(newFile)) {
            throw new RuntimeException("Could not rename/move file " + movieFile.getAbsolutePath() + " to " + newFile.getAbsolutePath());
        }
    }

    private List<File> listCameraMovies(final File rotatingDir) {
        final File[] rotatingCameraDirectories = rotatingDir.listFiles();
        if (rotatingCameraDirectories == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(rotatingCameraDirectories)
                     .map(this::listMoviesInDirectory)
                     .flatMap(List::stream)
                     .collect(toList());
    }

    private List<File> listMoviesInDirectory(final File directory) {
        assert directory != null;
        assert directory.exists();

        final File[] files = directory.listFiles();
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
}
