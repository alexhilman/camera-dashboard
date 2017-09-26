package com.alexhilman.cameradashboard.ui.view;

import com.alexhilman.cameradashboard.ui.video.MovieFileManager;
import com.alexhilman.cameradashboard.ui.video.MovieViewHelper;
import com.google.inject.Inject;
import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.*;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;

import java.time.*;

/**
 */
@GuiceView("movies")
public class Movies implements View {
    private final MovieFileManager movieFileManager;
    private final MovieViewHelper movieViewHelper;
    private Panel movies;
    private DateField dateControl;

    @Inject
    public Movies(final MovieFileManager movieFileManager,
                  final MovieViewHelper movieViewHelper) {
        this.movieFileManager = movieFileManager;
        this.movieViewHelper = movieViewHelper;
    }

    @Override
    public Component getViewComponent() {
        final VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        layout.setSpacing(true);

        final Component actions = buildActions();
        layout.addComponent(actions);

        movies = new Panel("Movies");
        movies.setIcon(VaadinIcons.FILM);
        movies.setSizeFull();

        layout.addComponentsAndExpand(movies);

        return layout;
    }

    private Component buildActions() {
        dateControl = new DateField("Find movies on this date");
        dateControl.setDateFormat("MMM d, yyyy");
        dateControl.addValueChangeListener(event -> {
            final ZonedDateTime fromDateTime = dateControl.getValue()
                                                          .atTime(LocalTime.MIDNIGHT)
                                                          .atZone(ZoneId.systemDefault());

            final ZonedDateTime toDateTime = dateControl.getValue()
                                                        .plusDays(1)
                                                        .atTime(LocalTime.MIDNIGHT)
                                                        .atZone(ZoneId.systemDefault());

            loadMovies(fromDateTime.toInstant(), toDateTime.toInstant());
        });

        return dateControl;
    }

    @Override
    public void enter(final ViewChangeListener.ViewChangeEvent event) {
        dateControl.setValue(LocalDate.now());
        dateControl.setRangeEnd(LocalDate.now());
        loadMovies(midnightThisMorning(), Instant.now());
    }

    private void loadMovies(final Instant from, final Instant to) {
        final UI ui = UI.getCurrent();

        movies.setContent(buildLoadingSpinner());

        Flowable.fromCallable(() -> movieFileManager.getMoviesInRange(from, to))
                .map(movieViewHelper::buildPostersFor)
                .subscribeOn(Schedulers.computation())
                .subscribe(components -> {
                    ui.access(() -> {
                        if (components.length == 0) {
                            movies.setContent(new Label("No videos for this day"));
                        } else {
                            final int columns = 3;
                            final int rows = components.length / columns + (components.length % columns > 0 ? 1 : 0);
                            final GridLayout content = new GridLayout(columns, rows, components);
                            content.setSpacing(true);
                            content.setMargin(true);
                            content.setWidth(100, Sizeable.Unit.PERCENTAGE);
                            movies.setContent(content);
                        }

                        ui.push();
                    });
                });
    }

    private Component buildLoadingSpinner() {
        final HorizontalLayout layout = new HorizontalLayout();
        layout.setMargin(true);
        layout.setSpacing(true);
        layout.setSizeFull();

        final ProgressBar progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);

        layout.addComponent(progressBar);
        layout.setComponentAlignment(progressBar, Alignment.MIDDLE_CENTER);

        return layout;
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
