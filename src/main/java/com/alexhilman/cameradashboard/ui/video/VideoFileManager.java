package com.alexhilman.cameradashboard.ui.video;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

/**
 */
@Singleton
public class VideoFileManager {
    private final File storageDirectory;

    @Inject
    public VideoFileManager(final String storageDirectory) {
        this.storageDirectory = new File(checkNotNull(storageDirectory, "storageDirectory cannot be null"));

        mkDirsIfMissing(this.storageDirectory);
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
     *         |-- 2017-01-01 00:00:00.mov
     *         |-- ...
     *     |-- cam2
     *         |-- 2017-01-01 00:01:00.mov
     *         |-- ...
     * |-- rotating
     *     |-- cam1
     *         |-- 2017-10-01 00:00:00.mov
     *         |-- ...
     *     |-- cam2
     *         |-- 2017-11-01 00:01:00.mov
     *         |-- ...
     * }
     * </pre>
     *
     * @return List of saved movies
     */
    public List<File> listAllMovies() {
        final ArrayList<File> files = Lists.newArrayList(listRotatingMovies());
        files.addAll(listSavedMovies());

        return files.stream()
                    .sorted(Comparator.comparing(File::getName))
                    .collect(toList());
    }

    public List<File> listRotatingMovies() {
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

    public List<File> listSavedMovies() {
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

    public void addMovieToRotatingPool(final String cameraName, final File movieFile) {
        checkNotNull(cameraName, "cameraName cannot be null");
        checkNotNull(movieFile, "movieFile cannot be null");
        checkArgument(movieFile.exists(), "movieFile must exist");

        final File cameraDir = new File(getRotatingDirectory(), cameraName);
        mkDirsIfMissing(cameraDir);

        final File newFile = new File(cameraDir, movieFile.getName());
        if (newFile.exists()) {
            throw new IllegalArgumentException("Movie file already exists in the camera storage directory: " + newFile.getAbsolutePath());
        }

        if (!movieFile.renameTo(newFile)) {
            throw new RuntimeException("Could not rename/move file " + movieFile.getAbsolutePath() + " to " + newFile.getAbsolutePath());
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

    public File getRotatingDirectory() {
        return new File(storageDirectory, "rotating");
    }

    public File getSavedDirectory() {
        return new File(storageDirectory, "saved");
    }
}
