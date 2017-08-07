package com.alexhilman.cameradashboard.ui;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * Hello world!
 */
public class App {
    public static final int PORT = 9090;

    public static void main(String[] args) {
        try {
            final Server server = new Server(PORT);

            final WebAppContext webAppContext = new WebAppContext();
            webAppContext.setDisplayName("Camera Dashboard");
            webAppContext.setContextPath("/");
            webAppContext.addServlet(MyVaadinUI.MyVaadinServlet.class, "/*");
            webAppContext.setBaseResource(Resource.newClassPathResource("webapp"));

            server.setHandler(webAppContext);

            server.start();
//            System.out.println(server.dump());

            System.out.println("Jetty started, please go to http://localhost:" + PORT + "/");
            server.join();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
