package com.alexhilman.cameradashboard.ui.view;

import com.alexhilman.cameradashboard.ui.video.Movie;
import com.alexhilman.cameradashboard.ui.video.MovieFileManager;
import com.alexhilman.cameradashboard.ui.video.MovieHelper;
import com.google.inject.Inject;
import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.*;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 */
@GuiceView("movies")
public class Movies implements View {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("LL d, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:m:s a");
    private final MovieFileManager movieFileManager;
    private final MovieHelper movieHelper;

    @Inject
    public Movies(final MovieFileManager movieFileManager,
                  final MovieHelper movieHelper) {
        this.movieFileManager = movieFileManager;
        this.movieHelper = movieHelper;
    }

    @Override
    public Component getViewComponent() {
        final Panel movies = new Panel("Movies");
        movies.setIcon(VaadinIcons.FILM);
        movies.setSizeFull();

        final VerticalLayout moviePosterContainer = new VerticalLayout();
        movies.setContent(moviePosterContainer);

        moviePosterContainer.addComponents(
                buildPostersFor(movieFileManager.getMoviesSince(midnightThisMorning().minus(7, ChronoUnit.DAYS)))
        );

        return movies;
    }

    private Component[] buildPostersFor(final List<Movie> moviesSince) {
        return moviesSince.stream()
                          .sorted((m1, m2) -> m2.getName().compareTo(m1.getName()))
                          .map(movie -> {
                              final HorizontalLayout posterLayout = new HorizontalLayout();
                              final Image image = new Image(null,
                                                            new ExternalResource("/movies/" + contextResourceNameFor(
                                                                    movie.getPosterImageFile())));
                              image.setWidth(20, Sizeable.Unit.EM);
                              image.addClickListener(event -> {
                                  ClassNavigator.navigateTo(WatchMovie.class);
                              });
                              posterLayout.addComponent(image);

                              final GridLayout movieDetails = new GridLayout(2, 3);
                              posterLayout.addComponents(movieDetails);
                              final LocalDateTime movieDateTime = movie.getDateTime();
                              movieDetails.addComponents(new Label("Date: "),
                                                         new Label(movieDateTime.toLocalDate().format(DATE_FORMATTER)));
                              movieDetails.addComponents(new Label("Time: "),
                                                         new Label(movieDateTime.toLocalTime().format(TIME_FORMATTER)));
                              final Duration runningTime = movieHelper.runningLengthFor(movie);
                              movieDetails.addComponents(new Label("Length: "),
                                                         new Label(runningTime.toMinutes() + ":" +
                                                                           runningTime.minus(runningTime.toMinutes(),
                                                                                             ChronoUnit.MINUTES)
                                                                                      .getSeconds()));
                              return posterLayout;
                          })
                          .collect(toList())
                          .toArray(new Component[moviesSince.size()]);
    }

    private String contextResourceNameFor(final File file) {
        final String absolutePath = movieFileManager.getStorageDirectory().getAbsolutePath();
        final String substring = file.getAbsolutePath().substring(absolutePath.length() + 1);
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
