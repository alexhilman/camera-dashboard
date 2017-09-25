package com.alexhilman.cameradashboard.ui.view;

import com.alexhilman.cameradashboard.ui.video.MovieFileManager;
import com.google.inject.Inject;
import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.navigator.View;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;

/**
 */
@GuiceView("watch-movie")
public class WatchMovie implements View {
    private final MovieFileManager movieFileManager;

    @Inject
    public WatchMovie(final MovieFileManager movieFileManager) {
        this.movieFileManager = movieFileManager;
    }

    @Override
    public Component getViewComponent() {
        return new Label("Work in Progress");
    }
}
