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
import com.vaadin.server.VaadinServlet;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.UI;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.net.URL;

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
        private static final org.slf4j.Logger getLogger() {
            return LoggerFactory.getLogger(VaadinServlet.class);
        }

        @Override
        protected boolean isAllowedVAADINResourceUrl(HttpServletRequest request, URL resourceUrl) {
            if ("jar".equals(resourceUrl.getProtocol())) {
                if (!resourceUrl.getPath().contains("!/VAADIN/")) {
                    getLogger().warn("Blocked attempt to access a JAR entry not starting with /VAADIN/: " + resourceUrl);
                    return false;
                }
                getLogger().debug("Accepted access to a JAR entry using a class loader: " + resourceUrl);
                return true;

            } else if ("rsrc".equals(resourceUrl.getProtocol())) {
                // This branch was added to allow access to RSRC files that start with "VAADIN/" (not "/VAADIN/")
                if (!resourceUrl.getPath().contains("VAADIN/")
                        || resourceUrl.getPath().contains("/../")) {
                    getLogger().info("Blocked attempt to access resource: " + resourceUrl);
                    return false;
                }
                getLogger().info("Accepted access to a resource using a class loader: " + resourceUrl);
                return true;

            } else {
                if (!resourceUrl.getPath().contains("/VAADIN/")
                        || resourceUrl.getPath().contains("/../")) {
                    getLogger().info("Blocked attempt to access file: " + resourceUrl);
                    return false;
                }
                getLogger().info("Accepted access to a file using a class loader: " + resourceUrl);
                return true;
            }
        }

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