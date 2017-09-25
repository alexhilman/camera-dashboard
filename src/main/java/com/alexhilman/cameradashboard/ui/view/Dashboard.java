package com.alexhilman.cameradashboard.ui.view;

import com.alexhilman.cameradashboard.ui.video.MovieFileManager;
import com.google.inject.Inject;
import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 */
@GuiceView("")
public class Dashboard implements View {
    private static final Logger LOG = LogManager.getLogger(Dashboard.class);

    private final MovieFileManager movieFileManager;
    private VerticalLayout rootLayout;
    private ProgressBar progressBar;
    private Panel todayMovies;

    @Inject
    public Dashboard(final MovieFileManager movieFileManager) {
        this.movieFileManager = movieFileManager;
    }

    @Override
    public Component getViewComponent() {
        rootLayout = new VerticalLayout();

        progressBar = new ProgressBar(0f);
        progressBar.setWidth(100, Sizeable.Unit.PERCENTAGE);
        rootLayout.addComponent(progressBar);

        todayMovies = new Panel("Today's Movies");
        todayMovies.setIcon(VaadinIcons.FILM);
        todayMovies.setWidth(100, Sizeable.Unit.PERCENTAGE);
        rootLayout.addComponent(todayMovies);

        return rootLayout;
    }

    private Component buildMoviesComponent() {
        return new Label("Placeholder");
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

        todayMovies.setContent(buildMoviesComponent());
    }

    public static String humanReadableByteCount(long bytes) {
        final int unit = 1000;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), "kMGTPE".charAt(exp - 1));
    }
}
