package com.alexhilman.cameradashboard.ui.inject;

import com.alexhilman.cameradashboard.ui.conf.CameraConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Provider;

import java.io.File;
import java.io.IOException;

import static com.alexhilman.cameradashboard.ui.App.CONFIGURATION_DIRECTORY_SYSTEM_PROPERTY;

/**
 */
public class CameraConfigurationProvider implements Provider<CameraConfiguration> {
    @Override
    public CameraConfiguration get() {
        try {
            return new ObjectMapper().readValue(new File(System.getProperty(CONFIGURATION_DIRECTORY_SYSTEM_PROPERTY),
                                                         "cameras.json"), CameraConfiguration.class);
        } catch (IOException e) {
            throw new IllegalStateException("Camera configuration is incorrect", e);
        }
    }
}
