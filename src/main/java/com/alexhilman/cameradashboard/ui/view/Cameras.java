package com.alexhilman.cameradashboard.ui.view;

import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.navigator.View;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;

/**
 */
@GuiceView("secondary")
public class Cameras implements View {
    @Override
    public Component getViewComponent() {
        return new Label("Cameras");
    }
}
