package com.alexhilman.cameradashboard.ui.conf;

import org.junit.Test;

import java.io.IOException;

import static com.alexhilman.cameradashboard.ui.CameraConfigurationReader.readCameraConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class CameraConfigurationTest {
    @Test
    public void shouldReadCameraConfiguration() throws IOException {
        final CameraConfiguration cameraConfiguration = readCameraConfig();

        assertThat(cameraConfiguration, is(notNullValue()));
    }
}