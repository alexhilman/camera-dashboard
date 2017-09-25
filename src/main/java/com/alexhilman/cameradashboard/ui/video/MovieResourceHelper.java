package com.alexhilman.cameradashboard.ui.video;

import com.google.inject.Inject;

import java.io.File;

/**
 * Aids in getting resource names for a movie.
 */
public class MovieResourceHelper {
    private final MovieFileManager movieFileManager;

    @Inject
    public MovieResourceHelper(final MovieFileManager movieFileManager) {
        this.movieFileManager = movieFileManager;
    }

    public String contextResourceNameFor(final File file) {
        final String absolutePath = movieFileManager.getStorageDirectory().getAbsolutePath();
        return file.getAbsolutePath().substring(absolutePath.length() + 1);
    }
}
