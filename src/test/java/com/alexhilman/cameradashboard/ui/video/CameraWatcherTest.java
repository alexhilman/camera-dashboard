package com.alexhilman.cameradashboard.ui.video;

import com.alexhilman.cameradashboard.ui.Fixtures;
import com.alexhilman.cameradashboard.ui.conf.Camera;
import com.alexhilman.cameradashboard.ui.conf.CameraConfiguration;
import com.alexhilman.dlink.dcs936.Dcs936Client;
import com.alexhilman.dlink.dcs936.model.DcsFile;
import com.google.common.collect.Lists;
import io.reactivex.Flowable;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class CameraWatcherTest {
    private CameraWatcher cameraWatcher;
    private CameraConfiguration cameraConfiguration;
    private MovieFileManager movieFileManager;
    private Dcs936Client driver;

    @Before
    public void setup() {
        cameraConfiguration = mock(CameraConfiguration.class);
        when(cameraConfiguration.getCameras()).thenReturn(
                Lists.newArrayList(
                        new Camera("dummy",
                                   "http://localhost:8080",
                                   "user",
                                   "pass")
                )
        );

        movieFileManager = mock(MovieFileManager.class);
        driver = mock(Dcs936Client.class);
        cameraWatcher = new CameraWatcher(cameraConfiguration, movieFileManager) {
            @Override
            Dcs936Client driverFor(final Camera camera) {
                return driver;
            }
        };
    }

    @Test
    public void shouldRequestNewMoviesSinceEpochForCameraWithNoFiles() {
        when(movieFileManager.lastMovieInstantFor(any())).thenReturn(Instant.EPOCH);
        when(driver.findNewMoviesSince(any())).thenReturn(Flowable.empty());

        cameraWatcher.watchCameras();

        verify(driver, times(1)).findNewMoviesSince(Instant.EPOCH);
    }

    @Test
    public void shouldGetNewFilesSinceLastInstant() throws IOException {
        final Instant instant = Instant.now().minus(15, ChronoUnit.MINUTES);
        when(movieFileManager.lastMovieInstantFor(any())).thenReturn(instant);
        when(driver.open(any()))
                .thenReturn(new ByteArrayInputStream(new byte[0]));

        final DcsFile expectedFile = Fixtures.randomDcsFile();

        when(driver.findNewMoviesSince(any())).thenReturn(
                Flowable.just(expectedFile)
        );

        cameraWatcher.watchCameras();

        verify(movieFileManager, times(1))
                .addMoviesToRotatingPool(eq(cameraConfiguration.getCameras().get(0)),
                                         eq(Lists.newArrayList(expectedFile)));

    }
}