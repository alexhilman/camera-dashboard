package com.alexhilman.cameradashboard.ui.video;

import com.google.inject.Inject;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Resource;

import java.io.File;

import static com.google.common.base.Preconditions.checkNotNull;

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

    public Resource posterResourceFor(final Movie movie) {
        checkNotNull(movie, "movie cannot be null");
        return new ExternalResource("/movies/" + contextResourceNameFor(movie.getPosterImageFile()));
    }

    public Resource movieResourceFor(final Movie movie) {
        checkNotNull(movie, "movie cannot be null");
        return new ExternalResource("/movies/" + contextResourceNameFor(movie.getMovieFile()));
    }
}
