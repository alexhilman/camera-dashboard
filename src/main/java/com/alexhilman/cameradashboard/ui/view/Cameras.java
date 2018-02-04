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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Base64;
import java.util.Comparator;

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

        // TODO maybe I just need to open a new stream to the camera's viewport and redirect that stream, rather than observing the recording stream
        layout = new VerticalLayout();
        layout.setSizeFull();
        final StreamResource source =
                new StreamResource(() -> {
                    try {
                        return camera.getStreams()
                                     .stream()
                                     .sorted(Comparator.comparingInt(s -> s.getQuality().ordinal()))
                                     .findFirst()
                                     .map(s -> {
                                         final HttpURLConnection urlConnection;
                                         try {
                                             urlConnection = (HttpURLConnection) s.getUrl().openConnection();
                                             if (camera.getUsername() != null && camera.getPassword() != null) {
                                                 String userpass = camera.getUsername() + ":" + camera.getPassword();
                                                 String basicAuth = "Basic " + new String(Base64.getEncoder()
                                                                                                .encode(userpass.getBytes()));
                                                 urlConnection.setRequestProperty("Authorization", basicAuth);
                                             }
                                         } catch (IOException e) {
                                             throw new RuntimeException(e);
                                         }
                                         return urlConnection;
                                     })
                                     .orElseThrow(() -> new RuntimeException("No stream found"))
                                     .getInputStream();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                                   "live.mp4");
        source.setMIMEType("video/x-motion-jpeg");
        final Video liveVideo = new Video("Live View of " + camera.getName(), source);
        liveVideo.setAutoplay(true);
        layout.addComponentsAndExpand(liveVideo);

        return layout;
    }
}
