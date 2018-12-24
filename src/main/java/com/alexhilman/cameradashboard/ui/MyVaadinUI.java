package com.alexhilman.cameradashboard.ui;

import com.alexhilman.cameradashboard.ui.inject.CameraDashboardModule;
import com.google.inject.Injector;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.VaadinServletConfiguration;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.guice.annotation.Import;
import com.vaadin.guice.annotation.PackagesToScan;
import com.vaadin.guice.server.GuiceVaadinServlet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import java.lang.reflect.Field;

@Theme(value = Lumo.class, variant = Lumo.DARK)
@Push(value = PushMode.AUTOMATIC, transport = Transport.WEBSOCKET)
public class MyVaadinUI extends UI {
    @Import(CameraDashboardModule.class)
    @interface MyAnnotation {
    }

    @WebServlet(urlPatterns = "/*", name = "Security Dashboard", asyncSupported = true)
    @VaadinServletConfiguration(ui = MyVaadinUI.class, productionMode = false)
    @PackagesToScan("com.alexhilman.cameradashboard.ui")
    @MyAnnotation
    public static class MyUIServlet extends GuiceVaadinServlet {
        private static final Logger LOG = LogManager.getLogger(MyUIServlet.class);

        @Override
        public void init(final ServletConfig servletConfig) throws ServletException {
            super.init(servletConfig);

            final Injector injector;
            try {
                final Field injectorField = GuiceVaadinServlet.class.getDeclaredField("injector");
                injectorField.setAccessible(true);
                injector = (Injector) injectorField.get(this);
            } catch (Exception e) {
                throw new IllegalStateException("No injector", e);
            }
        }
    }
}