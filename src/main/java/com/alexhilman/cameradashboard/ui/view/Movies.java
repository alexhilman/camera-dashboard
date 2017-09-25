package com.alexhilman.cameradashboard.ui.view;

import com.alexhilman.cameradashboard.ui.video.Movie;
import com.alexhilman.cameradashboard.ui.video.MovieFileManager;
import com.google.inject.Inject;
import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.navigator.View;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.Component;
import com.vaadin.ui.Image;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 */
@GuiceView("movies")
public class Movies implements View {
    private final MovieFileManager movieFileManager;

    @Inject
    public Movies(final MovieFileManager movieFileManager) {
        this.movieFileManager = movieFileManager;
    }

    @Override
    public Component getViewComponent() {
        final VerticalLayout components = new VerticalLayout();

        final Panel todayMovies = new Panel("Movies");
        components.addComponent(todayMovies);

        final VerticalLayout moviePreviewTiles = new VerticalLayout();
        todayMovies.setContent(moviePreviewTiles);

        moviePreviewTiles.addComponents(buildVideoTilesFor(movieFileManager.getMoviesSince(Instant.EPOCH)));

        return components;
    }

    private Component[] buildVideoTilesFor(final List<Movie> moviesSince) {
        return moviesSince.stream()
                          .sorted(Comparator.comparing(Movie::getName))
                          .map(movie -> {
                              final Image image = new Image(null,
                                                            new ExternalResource("/movies/" + contextResourceNameFor(
                                                                    movie.getPosterImageFile())));
                              image.setWidth(20, Sizeable.Unit.EM);

                              image.addClickListener(event -> {
                                  ClassNavigator.navigateTo(WatchMovie.class);
                              });
                              return image;
                          })
                          .collect(toList())
                          .toArray(new Component[moviesSince.size()]);
    }

    private String contextResourceNameFor(final File file) {
        final String absolutePath = movieFileManager.getStorageDirectory().getAbsolutePath();
        final String substring = file.getAbsolutePath().substring(absolutePath.length()+1);
        return substring;
    }

    private Instant midnightThisMorning() {
        return Instant.now()
                      .atZone(ZoneId.systemDefault())
                      .withHour(0)
                      .withMinute(0)
                      .withSecond(0)
                      .withNano(0)
                      .toInstant();
    }
}
