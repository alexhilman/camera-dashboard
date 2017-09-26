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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.time.ZoneId;

/**
 */
@GuiceView("")
public class Dashboard implements View {
    private static final Logger LOG = LogManager.getLogger(Dashboard.class);

    private final MovieFileManager movieFileManager;
    private final MovieViewHelper movieViewHelper;
    private VerticalLayout rootLayout;
    private ProgressBar progressBar;
    private Panel todayMovies;

    @Inject
    public Dashboard(final MovieFileManager movieFileManager,
                     final MovieViewHelper movieViewHelper) {
        this.movieFileManager = movieFileManager;
        this.movieViewHelper = movieViewHelper;
    }

    @Override
    public Component getViewComponent() {
        rootLayout = new VerticalLayout();
        rootLayout.setMargin(true);
        rootLayout.setSpacing(true);

        progressBar = new ProgressBar(0f);
        progressBar.setWidth(100, Sizeable.Unit.PERCENTAGE);
        rootLayout.addComponent(progressBar);

        todayMovies = new Panel("Today's Movies");
        todayMovies.setIcon(VaadinIcons.FILM);
        todayMovies.setSizeFull();
        rootLayout.addComponentsAndExpand(todayMovies);

        return rootLayout;
    }

    @Override
    public void enter(final ViewChangeListener.ViewChangeEvent event) {
        final long usableSpace = movieFileManager.getUsableSpace();
        final long totalSpace = movieFileManager.getTotalSpace();
        final long usedSpace = totalSpace - usableSpace;

        final float progress = (float) ((double) usedSpace / (double) totalSpace);
        progressBar.setValue(progress);
        progressBar.setCaption("Storage space: " + humanReadableByteCount(usedSpace) +
                                       " used out of " + humanReadableByteCount(totalSpace) +
                                       " (" + humanReadableByteCount(usableSpace) + " free)");

        todayMovies.setContent(buildLoadingSpinner());

        final UI ui = UI.getCurrent();

        Flowable.fromCallable(() -> movieFileManager.getMoviesInRange(midnightThisMorning(), Instant.now()))
                .map(movieViewHelper::buildPostersFor)
                .subscribeOn(Schedulers.computation())
                .subscribe(components -> {
                    ui.access(() -> {
                        if (components.length == 0) {
                            todayMovies.setContent(new Label("No videos for this day"));
                        } else {
                            final int columns = 3;
                            final int rows = components.length / columns + (components.length % columns > 0 ? 1 : 0);
                            final GridLayout content = new GridLayout(columns, rows, components);
                            content.setSpacing(true);
                            content.setMargin(true);
                            content.setWidth(100, Sizeable.Unit.PERCENTAGE);
                            todayMovies.setContent(content);
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

    public static String humanReadableByteCount(long bytes) {
        final int unit = 1000;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), "kMGTPE".charAt(exp - 1));
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
