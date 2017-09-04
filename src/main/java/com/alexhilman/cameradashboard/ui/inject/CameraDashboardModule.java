package com.alexhilman.cameradashboard.ui.inject;

import com.alexhilman.cameradashboard.ui.video.VideoFileManager;
import com.google.inject.AbstractModule;

import java.util.Properties;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 */
public class CameraDashboardModule extends AbstractModule {
    private final Properties properties;

    public CameraDashboardModule(final Properties properties) {
        checkNotNull(properties, "properties cannot be null");
        this.properties = properties;
    }

    @Override
    @SuppressWarnings("PointlessBinding")
    protected void configure() {
        bind(VideoFileManager.class);
    }
}
