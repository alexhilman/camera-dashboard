package com.alexhilman.cameradashboard.ui.video;

import com.alexhilman.cameradashboard.ui.conf.Camera;
import com.alexhilman.cameradashboard.ui.conf.CameraConfiguration;
import com.alexhilman.cameradashboard.ui.driver.MotionProcessor;
import com.alexhilman.dlink.dcs936.model.DcsFile;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
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
    private final ConcurrentMap<Camera, MotionProcessor> streamingDriversByCamera = new ConcurrentHashMap<>();
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
                            final MotionProcessor motionProcessor = new MotionProcessor(camera);

                            streamingDriversByCamera.put(camera, motionProcessor);

                            final Thread currentThread = Thread.currentThread();
                            while (!currentThread.isInterrupted()) {
                                try {
                                    motionProcessor.processStream();
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

    public List<Camera> getCameras() {
        return cameraConfiguration.getCameras();
    }

    private String extensionForFile(final DcsFile file) {
        return file.getFileName().substring(file.getFileName().lastIndexOf('.') + 1);
    }

    public InputStream observe(final Camera camera) {
        LOG.info("Observing {}", camera.getName());

        return streamingDriversByCamera.get(camera).observeStream();
    }
}
