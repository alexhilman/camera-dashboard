package com.alexhilman.cameradashboard.ui.conf;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class CameraConfigurationTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void shouldReadCameraConfiguration() throws IOException {
        final File file = new File("src/main/assembly/base/conf/cameras.json.sample");
        assertThat(file.exists(), is(true));

        final CameraConfiguration cameraConfiguration = OBJECT_MAPPER.readValue(file, CameraConfiguration.class);

        assertThat(cameraConfiguration, is(notNullValue()));
    }
}