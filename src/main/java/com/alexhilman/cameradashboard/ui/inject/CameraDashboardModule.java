package com.alexhilman.cameradashboard.ui.inject;

import com.alexhilman.cameradashboard.ui.App;
import com.alexhilman.cameradashboard.ui.conf.CameraConfiguration;
import com.alexhilman.cameradashboard.ui.video.MovieFileManager;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

import java.util.Properties;

/**
 */
public class CameraDashboardModule extends AbstractModule {
    @Override
    @SuppressWarnings("PointlessBinding")
    protected void configure() {
        Names.bindProperties(binder(), readProperties());
        bind(MovieFileManager.class);

        bind(CameraConfiguration.class).toProvider(CameraConfigurationProvider.class)
                                       .in(Singleton.class);
    }

    protected Properties readProperties() {
        return App.getCameraDashboardProperties();
    }
}
