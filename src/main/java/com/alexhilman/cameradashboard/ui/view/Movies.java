package com.alexhilman.cameradashboard.ui.view;

import com.alexhilman.cameradashboard.ui.video.Movie;
import com.alexhilman.cameradashboard.ui.video.MovieFileManager;
import com.alexhilman.cameradashboard.ui.video.MovieHelper;
import com.alexhilman.cameradashboard.ui.video.MovieResourceHelper;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.server.Sizeable;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 */
@GuiceView("movies")
public class Movies implements View {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:m:s a");
    private final MovieFileManager movieFileManager;
    private final MovieHelper movieHelper;
    private final MovieResourceHelper movieResourceHelper;

    @Inject
    public Movies(final MovieFileManager movieFileManager,
                  final MovieHelper movieHelper,
                  final MovieResourceHelper movieResourceHelper) {
        this.movieFileManager = movieFileManager;
        this.movieHelper = movieHelper;
        this.movieResourceHelper = movieResourceHelper;
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
                          .collect(groupingBy(movie -> {
                              return movie.getDateTime().toLocalDate();
                          }))
                          .entrySet()
                          .stream()
                          .sorted(Comparator.comparing(Map.Entry::getKey))
                          .map(movieEntry -> {
                              final LocalDate movieDate = movieEntry.getKey();
                              final List<Component> dateComponents =
                                      Lists.newArrayListWithCapacity(movieEntry.getValue().size() + 1);
                              dateComponents.add(dateSeparatorComponentFor(movieDate));

                              dateComponents.addAll(
                                      movieEntry.getValue()
                                                .stream()
                                                .sorted((m1, m2) -> m2.getName().compareTo(m1.getName()))
                                                .map(movie -> {
                                                    final HorizontalLayout posterLayout = new HorizontalLayout();
                                                    final Image image =
                                                            new Image(null,
                                                                      movieResourceHelper.posterResourceFor(movie));

                                                    image.setWidth(20, Sizeable.Unit.EM);
                                                    image.addClickListener(event -> {
                                                        ClassNavigator.navigateTo(
                                                                WatchMovie.class,
                                                                movieContextPathFor(movie));
                                                    });
                                                    posterLayout.addComponent(image);

                                                    final GridLayout movieDetails = new GridLayout(2, 3);
                                                    posterLayout.addComponents(movieDetails);
                                                    final LocalDateTime movieDateTime = movie.getDateTime();
                                                    movieDetails.addComponents(new Label("Date: "),
                                                                               new Label(movieDateTime.toLocalDate()
                                                                                                      .format(DATE_FORMATTER)));
                                                    movieDetails.addComponents(new Label("Time: "),
                                                                               new Label(movieDateTime.toLocalTime()
                                                                                                      .format(TIME_FORMATTER)));
                                                    final Duration runningTime = movieHelper.runningLengthFor(movie);
                                                    movieDetails.addComponents(new Label("Length:&nbsp;",
                                                                                         ContentMode.HTML),
                                                                               new Label(runningTime.toMinutes() + ":" +
                                                                                                 runningTime.minus(
                                                                                                         runningTime.toMinutes(),
                                                                                                         ChronoUnit.MINUTES)
                                                                                                            .getSeconds()));
                                                    return posterLayout;
                                                })
                                                .collect(toList()));
                              return dateComponents;
                          })
                          .flatMap(List::stream)
                          .collect(toList())
                          .toArray(new Component[moviesSince.size()]);
    }

    private String movieContextPathFor(final Movie movie) {
        try {
            return URLEncoder.encode(movieFileManager.getCameraForMovie(movie).getName(),
                                     "utf-8") + "/" + URLEncoder.encode(movie.getName(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private Component dateSeparatorComponentFor(final LocalDate date) {
        final HorizontalLayout components = new HorizontalLayout();
        components.setWidth(100, Sizeable.Unit.PERCENTAGE);
        final Label firstRule = new Label("<hr/>", ContentMode.HTML);
        firstRule.setWidth(100, Sizeable.Unit.PERCENTAGE);
        final Label secondRule = new Label("<hr/>", ContentMode.HTML);
        secondRule.setWidth(100, Sizeable.Unit.PERCENTAGE);
        components.addComponents(firstRule, new Label(date.format(DATE_FORMATTER)), secondRule);
        components.setExpandRatio(firstRule, 1);
        components.setExpandRatio(secondRule, 1);
        return components;
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
