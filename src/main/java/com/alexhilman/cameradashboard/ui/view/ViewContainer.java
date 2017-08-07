package com.alexhilman.cameradashboard.ui.view;

import com.google.inject.Inject;
import com.vaadin.guice.annotation.UIScope;
import com.vaadin.ui.Panel;

/**
 */
@UIScope
public class ViewContainer extends Panel {
    @Inject
    ViewContainer() {
        setSizeFull();
    }
}
