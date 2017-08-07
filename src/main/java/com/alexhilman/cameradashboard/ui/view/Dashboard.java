package com.alexhilman.cameradashboard.ui.view;

import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.navigator.View;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;

/**
 */
@GuiceView("")
public class Dashboard implements View {
    @Override
    public Component getViewComponent() {
        final Button dashboard = new Button("Go To Secondary");
        dashboard.addClickListener(event -> {
            UI.getCurrent().getNavigator().navigateTo("secondary");
        });
        return dashboard;
    }
}
