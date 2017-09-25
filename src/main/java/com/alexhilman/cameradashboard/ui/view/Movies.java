package com.alexhilman.cameradashboard.ui.view;

import com.alexhilman.cameradashboard.ui.video.MovieFileManager;
import com.google.inject.Inject;
import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.server.FileResource;
import com.vaadin.ui.*;

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

        final HorizontalLayout moviePreviewTiles = new HorizontalLayout();
        todayMovies.setContent(moviePreviewTiles);

        moviePreviewTiles.addComponents(buildVideosFor(movieFileManager.getMoviesSince(Instant.EPOCH)));

        return components;
    }

    private Component[] buildVideosFor(final List<File> moviesSince) {
        return moviesSince.stream()
                          .sorted(Comparator.comparing(File::getName))
                          .map(movie -> {
                              final Video video = new Video();
                              video.setSource(new FileResource(movie));
                              video.setHtmlContentAllowed(true);
                              video.setAltText("Cannot play video");
                              video.setPoster(VaadinIcons.PLAY_CIRCLE_O);
                              video.addContextClickListener(event -> video.play());
                              return video;
                          })
                          .collect(toList())
                          .toArray(new Component[moviesSince.size()]);
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
