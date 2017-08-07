package com.alexhilman.cameradashboard.ui.view;

import com.google.inject.Inject;
import com.vaadin.guice.annotation.UIScope;
import com.vaadin.ui.Panel;
import com.vaadin.ui.themes.ValoTheme;

/**
 */
@UIScope
public class ViewContainer extends Panel {
    @Inject
    ViewContainer() {
        setSizeFull();
        setStyleName(ValoTheme.PANEL_BORDERLESS);
    }
}
