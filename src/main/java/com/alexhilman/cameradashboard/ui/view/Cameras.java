package com.alexhilman.cameradashboard.ui.view;

import com.alexhilman.cameradashboard.ui.conf.Camera;
import com.alexhilman.cameradashboard.ui.video.CameraWatcher;
import com.google.inject.Inject;
import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.navigator.View;
import com.vaadin.server.StreamResource;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Video;

/**
 */
@GuiceView("cameras")
public class Cameras implements View {
    private final CameraWatcher cameraWatcher;
    private VerticalLayout layout;

    @Inject
    public Cameras(final CameraWatcher cameraWatcher) {
        this.cameraWatcher = cameraWatcher;
    }

    @Override
    public Component getViewComponent() {
        final Camera camera = cameraWatcher.getCameras().get(0);

        layout = new VerticalLayout();
        layout.setSizeFull();
        final StreamResource source = new StreamResource(() -> cameraWatcher.observe(camera), "live.mjpg");
        source.setMIMEType("video/x-motion-jpeg");
        final Video liveVideo = new Video("Live View of " + camera.getName(), source);
        liveVideo.setAutoplay(true);
        layout.addComponentsAndExpand(liveVideo);

        return layout;
    }
}
