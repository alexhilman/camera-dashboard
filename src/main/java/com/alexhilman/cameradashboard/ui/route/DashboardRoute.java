package com.alexhilman.cameradashboard.ui.route;

import com.google.inject.Inject;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 */
@Route(value = "", layout = RootRouterLayout.class)
public class DashboardRoute extends VerticalLayout {
    private static final Logger LOG = LogManager.getLogger(DashboardRoute.class);

    @Inject
    public DashboardRoute() {
        add(new Label("Hello, world"));
    }
}