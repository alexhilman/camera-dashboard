package com.alexhilman.cameradashboard.ui.video;

import com.alexhilman.cameradashboard.ui.view.ClassNavigator;
import com.alexhilman.cameradashboard.ui.view.WatchMovie;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Resource;
import com.vaadin.server.Sizeable;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

/**
 * Aids in getting resource names for a movie.
 */
@Singleton
public class MovieViewHelper {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:m:s a");

    private final MovieFileManager movieFileManager;
    private final MovieHelper movieHelper;

    @Inject
    public MovieViewHelper(final MovieFileManager movieFileManager,
                           final MovieHelper movieHelper) {
        this.movieFileManager = movieFileManager;
        this.movieHelper = movieHelper;
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

    public Component[] buildPostersFor(final List<Movie> movies) {
        checkNotNull(movies, "movies cannot be null");

        return movies.stream()
                     .sorted((m1, m2) -> m2.getName().compareTo(m1.getName()))
                     .map(movie -> {
                         final VerticalLayout posterLayout = new VerticalLayout();
                         posterLayout.setMargin(false);
                         posterLayout.setSpacing(true);
                         posterLayout.setSizeFull();
                         final Image image = new Image(null, posterResourceFor(movie));
                         image.setSizeFull();
                         image.addClickListener(event -> {
                             ClassNavigator.navigateTo(WatchMovie.class, movieContextPathFor(movie));
                         });
                         posterLayout.addComponent(image);

                         final GridLayout movieDetails = new GridLayout(2, 3);
                         movieDetails.setMargin(false);
                         movieDetails.setSpacing(false);
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
                     .collect(toList())
                     .toArray(new Component[movies.size()]);
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

    private String movieContextPathFor(final Movie movie) {
        try {
            return URLEncoder.encode(movieFileManager.getCameraForMovie(movie).getName(),
                                     "utf-8") + "/" + URLEncoder.encode(movie.getName(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
