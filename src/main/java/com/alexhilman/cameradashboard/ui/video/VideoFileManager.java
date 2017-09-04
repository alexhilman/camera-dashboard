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

        if (!this.storageDirectory.exists()) {
            if (!this.storageDirectory.mkdirs()) {
                throw new IllegalStateException("Cannot create storage directory: " + storageDirectory);
            }
        }
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
}
