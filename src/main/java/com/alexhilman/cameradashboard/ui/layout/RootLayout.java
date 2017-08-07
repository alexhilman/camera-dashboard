package com.alexhilman.cameradashboard.ui.layout;

import com.alexhilman.cameradashboard.ui.view.ViewContainer;
import com.google.inject.Inject;
import com.vaadin.guice.annotation.UIScope;
import com.vaadin.ui.VerticalLayout;

/**
 */
@UIScope
public class RootLayout extends VerticalLayout {

    @Inject
    RootLayout(ViewContainer viewContainer) {
        setSizeFull();
        setMargin(true);
        setSpacing(true);
        addComponents(viewContainer);
        setExpandRatio(viewContainer, 1);
    }
}