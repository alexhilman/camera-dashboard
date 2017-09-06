package com.alexhilman.cameradashboard.ui.video;

import com.alexhilman.cameradashboard.ui.conf.Camera;
import com.alexhilman.cameradashboard.ui.conf.CameraConfiguration;
import com.alexhilman.dlink.dcs936.model.DcsFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Ignore
public class CameraMovieWatcherIT {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private CameraMovieWatcher cameraMovieWatcher;

    @Before
    public void setup() {
        final CameraConfiguration cameraConfiguration;
        try {
            cameraConfiguration =
                    OBJECT_MAPPER.readValue(getClass().getResource("/com/alexhilman/cameradashboard/ui/cameras.json"),
                                            CameraConfiguration.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        cameraMovieWatcher = new CameraMovieWatcher(cameraConfiguration);
    }

    @Test
    public void shouldGetCameras() {
        final List<Camera> cameras = cameraMovieWatcher.getCameras();

        assertThat(cameras, hasSize(1));
    }

    @Test
    public void shouldListNewFilesSinceInstant() {
        final Instant instant = Instant.now().minus(3, ChronoUnit.HOURS);
        final Camera camera = cameraMovieWatcher.getCameras().get(0);
        final List<DcsFile> files = cameraMovieWatcher.select(camera)
                                                      .listFilesNewerThan(instant);

        assertThat(files, is(notNullValue()));
        assertThat(files, hasSize(greaterThanOrEqualTo(1)));
    }
}