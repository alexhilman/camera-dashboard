package com.alexhilman.cameradashboard.ui.video;

import java.io.File;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Container for both the movie file and the poster image for the video.
 */
public class Movie {
    private final File movieFile;
    private final File posterImageFile;

    public Movie(final File movieFile, final File posterImageFile) {
        this.movieFile = checkNotNull(movieFile, "movieFile cannot be null");
        this.posterImageFile = checkNotNull(posterImageFile, "posterImageFile cannot be null");
    }

    public File getMovieFile() {
        return movieFile;
    }

    public File getPosterImageFile() {
        return posterImageFile;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Movie movie = (Movie) o;

        if (!movieFile.equals(movie.movieFile)) return false;
        return posterImageFile.equals(movie.posterImageFile);
    }

    @Override
    public int hashCode() {
        int result = movieFile.hashCode();
        result = 31 * result + posterImageFile.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Movie{" +
                "movieFile=" + movieFile.getAbsolutePath() +
                ", posterImageFile=" + posterImageFile.getAbsolutePath() +
                '}';
    }

    public String getName() {
        return movieFile.getName();
    }
}
