package com.alexhilman.cameradashboard.ui.view.layout;

import com.alexhilman.cameradashboard.ui.view.*;
import com.google.inject.Inject;
import com.vaadin.guice.annotation.UIScope;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;

/**
 */
@UIScope
public class RootLayout extends HorizontalLayout {

    @Inject
    RootLayout(ViewContainer viewContainer) {
        setSizeFull();
        setMargin(true);
        setSpacing(true);
        final Component actions = buildQuickLinksComponent();
        addComponent(actions);
        setComponentAlignment(actions, Alignment.MIDDLE_CENTER);
        addComponentsAndExpand(viewContainer);
    }

    private Component buildQuickLinksComponent() {
        final VerticalLayout actions = new VerticalLayout();
        actions.setMargin(new MarginInfo(false, true, false, false));
        actions.setWidthUndefined();
        actions.addStyleNames("root-actions");
        actions.setSpacing(true);

        final Button homeButton = new Button(VaadinIcons.HOME);
        homeButton.addClickListener(event -> ClassNavigator.navigateTo(Dashboard.class));
        homeButton.addStyleNames(ValoTheme.BUTTON_HUGE, "circle-button");
        actions.addComponent(homeButton);

        final Button camerasButton = new Button(VaadinIcons.MOVIE);
        camerasButton.addClickListener(event -> ClassNavigator.navigateTo(Cameras.class));
        camerasButton.addStyleNames(ValoTheme.BUTTON_HUGE, "circle-button");
        actions.addComponent(camerasButton);

        final Button moviesButton = new Button(VaadinIcons.FILM);
        moviesButton.addClickListener(event -> ClassNavigator.navigateTo(Movies.class));
        moviesButton.addStyleNames(ValoTheme.BUTTON_HUGE, "circle-button");
        actions.addComponent(moviesButton);

        return actions;
    }
}