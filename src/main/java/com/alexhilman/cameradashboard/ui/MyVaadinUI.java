package com.alexhilman.cameradashboard.ui;

import com.alexhilman.cameradashboard.ui.inject.CameraDashboardInitializer;
import com.alexhilman.cameradashboard.ui.layout.RootLayout;
import com.alexhilman.cameradashboard.ui.video.CameraMovieWatcher;
import com.alexhilman.cameradashboard.ui.view.ErrorView;
import com.alexhilman.cameradashboard.ui.view.ViewContainer;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.guice.annotation.GuiceUI;
import com.vaadin.guice.annotation.PackagesToScan;
import com.vaadin.guice.server.GuiceVaadinServlet;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.UI;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import java.lang.reflect.Field;

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
        @Override
        public void init(final ServletConfig servletConfig) throws ServletException {
            super.init(servletConfig);

            final Injector injector;
            try {
                final Field injectorField = GuiceVaadinServlet.class.getDeclaredField("injector");
                injectorField.setAccessible(true);
                injector = (Injector) injectorField.get(this);
            } catch (Exception e) {
                throw new IllegalStateException("Cannot get the injector");
            }

            injector.getInstance(CameraMovieWatcher.class).start();
        }
    }
}