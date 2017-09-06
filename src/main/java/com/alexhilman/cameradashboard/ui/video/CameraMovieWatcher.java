package com.alexhilman.cameradashboard.ui.video;

import com.alexhilman.cameradashboard.ui.conf.Camera;
import com.alexhilman.cameradashboard.ui.conf.CameraConfiguration;
import com.alexhilman.dlink.dcs936.AccessCredentials;
import com.alexhilman.dlink.dcs936.Dcs936Client;
import com.alexhilman.dlink.dcs936.model.DcsFile;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 */
@Singleton
public class CameraMovieWatcher {
    private CameraConfiguration cameraConfiguration;

    @Inject
    public CameraMovieWatcher(final CameraConfiguration cameraConfiguration) {
        this.cameraConfiguration = checkNotNull(cameraConfiguration, "cameraConfiguration cannot be null");
    }

    List<Camera> getCameras() {
        return cameraConfiguration.getCameras();
    }

    CameraSelector select(final Camera camera) {
        return new CameraSelector(camera);
    }

    private Dcs936Client driverFor(final Camera camera) {
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

    //TODO this needs to be interfaced properly; usernames and passwords should be universal, etc
    public class CameraSelector {
        private final Camera camera;

        private CameraSelector(final Camera camera) {
            this.camera = camera;
        }

        public List<DcsFile> listFilesNewerThan(final Instant instant) {
            return driverFor(camera).findNewMoviesSince(instant);
        }
    }
}
