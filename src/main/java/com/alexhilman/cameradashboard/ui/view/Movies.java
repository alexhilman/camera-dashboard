package com.alexhilman.cameradashboard.ui.view;

import com.alexhilman.cameradashboard.ui.video.MovieFileManager;
import com.alexhilman.cameradashboard.ui.video.MovieViewHelper;
import com.google.inject.Inject;
import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.ui.Component;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 */
@GuiceView("movies")
public class Movies implements View {
    private final MovieFileManager movieFileManager;
    private final MovieViewHelper movieViewHelper;

    @Inject
    public Movies(final MovieFileManager movieFileManager,
                  final MovieViewHelper movieViewHelper) {
        this.movieFileManager = movieFileManager;
        this.movieViewHelper = movieViewHelper;
    }

    @Override
    public Component getViewComponent() {
        final Panel movies = new Panel("Movies");
        movies.setIcon(VaadinIcons.FILM);
        movies.setSizeFull();

        final VerticalLayout moviePosterContainer = new VerticalLayout();
        movies.setContent(moviePosterContainer);

        moviePosterContainer.addComponents(
                movieViewHelper.buildPostersFor(
                        movieFileManager.getMoviesInRange(midnightThisMorning().minus(7, ChronoUnit.DAYS),
                                                          Instant.now()))
        );

        return movies;
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
