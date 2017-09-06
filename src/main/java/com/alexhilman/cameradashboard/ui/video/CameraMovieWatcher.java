package com.alexhilman.cameradashboard.ui.video;

import com.alexhilman.cameradashboard.ui.conf.Camera;
import com.alexhilman.cameradashboard.ui.conf.CameraConfiguration;
import com.alexhilman.dlink.dcs936.AccessCredentials;
import com.alexhilman.dlink.dcs936.Dcs936Client;
import com.alexhilman.dlink.dcs936.model.DcsFile;
import com.alexhilman.dlink.helper.IOStreams;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 */
@Singleton
public class CameraMovieWatcher {
    private final CameraConfiguration cameraConfiguration;
    private final MovieFileManager movieFileManager;

    @Inject
    public CameraMovieWatcher(final CameraConfiguration cameraConfiguration,
                              final MovieFileManager movieFileManager) {
        this.cameraConfiguration = checkNotNull(cameraConfiguration, "cameraConfiguration cannot be null");
        this.movieFileManager = movieFileManager;
    }

    List<Camera> getCameras() {
        return cameraConfiguration.getCameras();
    }

    Dcs936Client driverFor(final Camera camera) {
        if (!camera.getDriver().getImplementation().equals(Dcs936Client.class.getName())) {
            throw new IllegalArgumentException("Camera driver not supported: " + camera.getDriver()
                                                                                       .getImplementation());
        }

        try {
            return new Dcs936Client(
                    new AccessCredentials(camera.getUsername(),
                                          camera.getPassword(),
                                          new URL(camera.getNetworkAddress())
                    )
            );
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed URL for camera: " + camera.getNetworkAddress(), e);
        }
    }

    public void downloadNewFiles() {
        cameraConfiguration.getCameras().forEach(camera -> {
            final Dcs936Client dcs936Client = driverFor(camera);

            final Instant lastInstant = movieFileManager.lastMovieInstantFor(camera);
            dcs936Client.findNewMoviesSince(lastInstant)
                        .forEach(file -> {
                            final Instant fileInstant = dcs936Client.getFileInstant(file);
                            try (final InputStream inputStream = dcs936Client.open(file)) {
                                movieFileManager.addMovieToRotatingPool(camera,
                                                                        inputStream,
                                                                        extensionForFile(file),
                                                                        fileInstant);
                            } catch (IOException e) {
                                throw new RuntimeException(
                                        "Could not get file " + file.getAbsoluteFileName() + " for camera " + camera.getName(),
                                        e);
                            }
                        });
        });
    }

    private String extensionForFile(final DcsFile file) {
        return file.getFileName().substring(file.getFileName().lastIndexOf('.') + 1);
    }
}
