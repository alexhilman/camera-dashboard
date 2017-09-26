package com.alexhilman.cameradashboard.ui.view;

import com.alexhilman.cameradashboard.ui.conf.Camera;
import com.alexhilman.cameradashboard.ui.conf.CameraConfiguration;
import com.alexhilman.cameradashboard.ui.video.Movie;
import com.alexhilman.cameradashboard.ui.video.MovieFileManager;
import com.alexhilman.cameradashboard.ui.video.MovieHelper;
import com.alexhilman.cameradashboard.ui.video.MovieViewHelper;
import com.google.inject.Inject;
import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Optional;

/**
 */
@GuiceView("watch-movie")
public class WatchMovie implements View {
    private final MovieFileManager movieFileManager;
    private final MovieHelper movieHelper;
    private final MovieViewHelper movieViewHelper;
    private final CameraConfiguration cameraConfiguration;

    private Video video;

    @Inject
    public WatchMovie(final MovieFileManager movieFileManager,
                      final MovieHelper movieHelper,
                      final MovieViewHelper movieViewHelper,
                      final CameraConfiguration cameraConfiguration) {
        this.movieFileManager = movieFileManager;
        this.movieHelper = movieHelper;
        this.movieViewHelper = movieViewHelper;
        this.cameraConfiguration = cameraConfiguration;
    }

    @Override
    public Component getViewComponent() {
        final Panel container = new Panel();
        container.setSizeFull();
        container.setStyleName(ValoTheme.PANEL_BORDERLESS);

        final VerticalLayout layout = new VerticalLayout();
        container.setContent(layout);
        layout.setSizeFull();

        video = new Video();
        video.setWidth(90, Sizeable.Unit.PERCENTAGE);
        video.setSizeFull();
        layout.addComponentsAndExpand(video);

        return container;
    }

    @Override
    public void enter(final ViewChangeListener.ViewChangeEvent event) {
        final String[] params = event.getParameters().split("\\/");

        final String cameraName;
        final String fileName;
        try {
            cameraName = URLDecoder.decode(params[0], "utf-8");
            fileName = URLDecoder.decode(params[1], "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        final Camera camera =
                cameraConfiguration.getCameras()
                                   .stream()
                                   .filter(configuredCamera -> configuredCamera.getName().equals(cameraName))
                                   .findFirst()
                                   .orElseThrow(() -> new RuntimeException(
                                           "Could not find configuration for camera \"" + cameraName + "\""));

        final Optional<Movie> optionalMovie = movieFileManager.findMovie(camera, fileName);

        if (!optionalMovie.isPresent()) {
            Notification.show("Could not find the requested movie");
        }

        final Movie movie = optionalMovie.get();

        video.setSource(movieViewHelper.movieResourceFor(movie));
        video.play();
    }
}
