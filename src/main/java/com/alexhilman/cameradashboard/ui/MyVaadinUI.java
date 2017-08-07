package com.alexhilman.cameradashboard.ui;

import com.alexhilman.cameradashboard.ui.inject.CameraDashboardInitializer;
import com.alexhilman.cameradashboard.ui.layout.RootLayout;
import com.alexhilman.cameradashboard.ui.view.ErrorView;
import com.alexhilman.cameradashboard.ui.view.ViewContainer;
import com.google.inject.Inject;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.guice.annotation.GuiceUI;
import com.vaadin.guice.annotation.PackagesToScan;
import com.vaadin.guice.server.GuiceVaadinServlet;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.UI;

import javax.servlet.annotation.WebServlet;

@Theme("mytheme")
@GuiceUI(viewContainer = ViewContainer.class, path = "/", errorView = ErrorView.class)
@Push(transport = Transport.WEBSOCKET)
public class MyVaadinUI extends UI {

    @Inject
    private RootLayout root;

    @Override
    protected void init(VaadinRequest request) {
        setContent(root);
    }

    @WebServlet(urlPatterns = {"/*"}, name = "MyVaadinServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = MyVaadinUI.class, productionMode = false)
    @PackagesToScan(value = "com.alexhilman.cameradashboard.ui")
    @CameraDashboardInitializer
    public static class MyVaadinServlet extends GuiceVaadinServlet {
    }
}