package com.alexhilman.cameradashboard.ui.view;

import com.alexhilman.cameradashboard.ui.video.MovieFileManager;
import com.google.inject.Inject;
import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.Component;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.VerticalLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 */
@GuiceView("")
public class Dashboard implements View {
    private static final Logger LOG = LogManager.getLogger(Dashboard.class);

    private final MovieFileManager movieFileManager;
    private VerticalLayout rootLayout;

    @Inject
    public Dashboard(final MovieFileManager movieFileManager) {
        this.movieFileManager = movieFileManager;
    }

    @Override
    public Component getViewComponent() {
        rootLayout = new VerticalLayout();
        rootLayout.addComponent(buildFileSystemThermometer());

        return rootLayout;
    }

    private Component buildFileSystemThermometer() {
        final long usableSpace = movieFileManager.getUsableSpace();
        final long totalSpace = movieFileManager.getTotalSpace();
        final long usedSpace = totalSpace - usableSpace;

        final float progress = (float) ((double) usedSpace / (double) totalSpace);
        final ProgressBar progressBar = new ProgressBar(progress);
        progressBar.setCaption("Free space: " + humanReadableByteCount(usableSpace));
        progressBar.setWidth(100, Sizeable.Unit.PERCENTAGE);

        return progressBar;
    }

    @Override
    public void enter(final ViewChangeListener.ViewChangeEvent event) {
    }

    public static String humanReadableByteCount(long bytes) {
        final int unit = 1000;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), "kMGTPE".charAt(exp - 1));
    }
}
