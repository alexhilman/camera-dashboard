package com.alexhilman.cameradashboard.ui.inject;

import com.alexhilman.cameradashboard.ui.view.Dashboard;
import com.alexhilman.cameradashboard.ui.view.ErrorView;
import com.google.inject.AbstractModule;

/**
 */
public class MyModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Dashboard.class);
        bind(ErrorView.class);
    }
}
