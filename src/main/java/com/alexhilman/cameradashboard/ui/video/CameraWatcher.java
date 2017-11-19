package com.alexhilman.cameradashboard.ui.video;

import com.alexhilman.cameradashboard.ui.conf.Camera;
import com.alexhilman.cameradashboard.ui.conf.CameraConfiguration;
import com.alexhilman.cameradashboard.ui.driver.StreamingDriver;
import com.alexhilman.dlink.dcs936.model.DcsFile;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 */
@Singleton
public class CameraWatcher {
    private static final Logger LOG = LogManager.getLogger(CameraWatcher.class);

    private final CameraConfiguration cameraConfiguration;
    private final MovieFileManager movieFileManager;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final ConcurrentMap<Camera, StreamingDriver> streamingDriversByCamera = new ConcurrentHashMap<>();
    private volatile boolean running;

    @Inject
    public CameraWatcher(final CameraConfiguration cameraConfiguration,
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
                    getCameras().forEach(camera -> {
                        executorService.submit(() -> {
                            final StreamingDriver streamingDriver;
                            try {
                                streamingDriver =
                                        new StreamingDriver(new URL(camera.getNetworkAddress()),
                                                            camera.getUsername(),
                                                            camera.getPassword());
                            } catch (MalformedURLException e) {
                                throw new RuntimeException("Invalid configuration for " + camera.getName(), e);
                            }

                            streamingDriversByCamera.put(camera, streamingDriver);

                            final Thread currentThread = Thread.currentThread();
                            while (!currentThread.isInterrupted()) {
                                try {
                                    streamingDriver.processStream();
                                } catch (Exception e) {
                                    LOG.error("Encountered error while processing camera video stream", e);
                                }
                            }
                        });
                    });
                    running = this.running = true;
                }
            }
        }
    }

    List<Camera> getCameras() {
        return cameraConfiguration.getCameras();
    }

    private String extensionForFile(final DcsFile file) {
        return file.getFileName().substring(file.getFileName().lastIndexOf('.') + 1);
    }
}
