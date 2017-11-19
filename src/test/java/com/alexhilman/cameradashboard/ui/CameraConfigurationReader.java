package com.alexhilman.cameradashboard.ui;

import com.alexhilman.cameradashboard.ui.conf.CameraConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 */
public class CameraConfigurationReader {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static CameraConfiguration readCameraConfig() {
        final CameraConfiguration cameraConfiguration;
        try {
            final URL camerasJsonFile =
                    new File(System.getProperty("user.dir") + "/src/main/assembly/base/conf/cameras.json")
                            .toURI()
                            .toURL();
            cameraConfiguration = OBJECT_MAPPER.readValue(camerasJsonFile, CameraConfiguration.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return cameraConfiguration;
    }
}
