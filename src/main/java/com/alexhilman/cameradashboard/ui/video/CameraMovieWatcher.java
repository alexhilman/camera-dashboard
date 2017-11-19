package com.alexhilman.cameradashboard.ui.video;

import com.alexhilman.cameradashboard.ui.conf.Camera;
import com.alexhilman.cameradashboard.ui.conf.CameraConfiguration;
import com.alexhilman.dlink.dcs936.Dcs936Client;
import com.alexhilman.dlink.dcs936.model.DcsFile;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 */
@Singleton
public class CameraMovieWatcher {
    private static final Logger LOG = LogManager.getLogger(CameraMovieWatcher.class);

    private final CameraConfiguration cameraConfiguration;
    private final MovieFileManager movieFileManager;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean running;

    @Inject
    public CameraMovieWatcher(final CameraConfiguration cameraConfiguration,
                              final MovieFileManager movieFileManager) {
        this.cameraConfiguration = checkNotNull(cameraConfiguration, "cameraConfiguration cannot be null");
        this.movieFileManager = movieFileManager;
    }

    public void start() {
        boolean running = this.running;
        if (!running) {
            synchronized (this) {
                running = this.running;
                if (!running) {
                    executorService.scheduleWithFixedDelay(
                            () -> {
                                try {
                                    watchCameras();
                                } catch (Exception e) {
                                    LOG.error("Could not fully iterate through new files", e);
                                }
                            },
                            0,
                            5,
                            TimeUnit.SECONDS
                    );
                }
                running = true;
            }
        }
    }

    List<Camera> getCameras() {
        return cameraConfiguration.getCameras();
    }

    Dcs936Client driverFor(final Camera camera) {
        throw new UnsupportedOperationException("Implementation changing");
    }

    public void watchCameras() {
        cameraConfiguration.getCameras().forEach(camera -> {
            LOG.info("Looking for new files in {} at {}", camera.getName(), camera.getNetworkAddress());

            final Dcs936Client dcs936Client = driverFor(camera);

            final Instant lastInstant = movieFileManager.lastMovieInstantFor(camera);
            movieFileManager.addMoviesToRotatingPool(
                    camera,
                    dcs936Client.findNewMoviesSince(lastInstant)
                                .toList()
                                .blockingGet());
        });
    }

    private String extensionForFile(final DcsFile file) {
        return file.getFileName().substring(file.getFileName().lastIndexOf('.') + 1);
    }
}
