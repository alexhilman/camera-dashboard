package com.alexhilman.cameradashboard.ui;

import com.google.common.base.Strings;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.ShutdownHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;

/**
 * Hello world!
 */
public class App {
    private static final Logger LOG = LoggerFactory.getLogger(App.class);
    private static volatile Properties cameraDashboardProperties;

    public static final int PORT = 9090;
    public static final String CAMERAS_CONFIG_FILE = "cameras.json";
    public static final String DASHBOARD_CONFIG_FILE = "camera-dashboard.properties";
    public static final String CONFIGURATION_DIRECTORY_SYSTEM_PROPERTY = "camdash.configurationDir";

    public static void main(String[] args) {
        validateStateOrThrow();

        try {
            final Server server = new Server(PORT);

            final ResourceHandler resourceHandler = new ResourceHandler();
            resourceHandler.setDirectoriesListed(true);
            resourceHandler.setResourceBase(cameraDashboardProperties.getProperty("cameradashboard.video.location"));
            final ContextHandler moviesContext = new ContextHandler("/movies");
            moviesContext.setHandler(resourceHandler);

            final WebAppContext webAppContext = new WebAppContext() {
                @Override
                protected void doStart() throws Exception {
                    super.doStart();

                    if (getUnavailableException() != null) {
                        throw (Exception) getUnavailableException();
                    }
                }
            };
            webAppContext.setDisplayName("Camera Dashboard");
            webAppContext.setContextPath("/ui");
            final ServletHolder servletHolder = webAppContext.addServlet(MyVaadinUI.MyVaadinServlet.class, "/*");
            servletHolder.setInitOrder(0);
            webAppContext.setBaseResource(Resource.newClassPathResource("webapp"));

            final ShutdownHandler shutdownHandler = new ShutdownHandler("dccb2e0d-3f38-4ba1-9d05-4e810294aa18",
                                                                        true,
                                                                        true);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    shutdownHandler.sendShutdown();
                } catch (IOException e) {
                    LOG.error("Could not send shutdown", e);
                }
            }));

            final HandlerList handlerList = new HandlerList();
            handlerList.setHandlers(new Handler[]{shutdownHandler, moviesContext, webAppContext});
            server.setHandler(handlerList);

            server.start();
//            System.out.println(server.dump());

            System.out.println("Jetty started, please go to http://localhost:" + PORT + "/");
            server.join();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Validates the state of the installation. Throws {@link IllegalStateException} if the state is invalid.
     *
     * @throws IllegalStateException If the installation is invalid
     */
    private static void validateStateOrThrow() {
        final String configurationDirectory = System.getProperty(CONFIGURATION_DIRECTORY_SYSTEM_PROPERTY);

        checkState(!Strings.isNullOrEmpty(configurationDirectory),
                   "The system property \"" + CONFIGURATION_DIRECTORY_SYSTEM_PROPERTY +
                           "\" must be set to the directory containing the " + CAMERAS_CONFIG_FILE + " configuration file");

        final File confDir = new File(configurationDirectory);
        checkState(confDir.exists(),
                   "The specified configuration directory \"" + configurationDirectory +
                           "\" does not exist");

        checkState(confDir.isDirectory(),
                   "The specified configuration directory \"" + configurationDirectory +
                           "\" is not a directory");

        final File cameraConfigfile =
                Stream.of(nullToEmptyArray(confDir.listFiles()))
                      .filter(f -> f.getName().equals(CAMERAS_CONFIG_FILE))
                      .findFirst()
                      .orElseThrow(() -> new IllegalStateException(
                              "The " + CAMERAS_CONFIG_FILE + " configuration file was not found in " + confDir
                                      .getAbsolutePath()));

        checkState(cameraConfigfile.exists(), CAMERAS_CONFIG_FILE + " does not exist");

        final File dashboardConfigFile =
                Stream.of(nullToEmptyArray(confDir.listFiles()))
                      .filter(f -> f.getName().equals(DASHBOARD_CONFIG_FILE))
                      .findFirst()
                      .orElseThrow(() -> new IllegalStateException(
                              "The " + DASHBOARD_CONFIG_FILE + " configuration file was not found in " + confDir
                                      .getAbsolutePath()));

        checkState(dashboardConfigFile.exists(), DASHBOARD_CONFIG_FILE + " file does not exist");

        final Properties tempProperties = new Properties();
        final String storageLocation;
        try (final InputStream inputStream = new FileInputStream(dashboardConfigFile)) {
            tempProperties.load(inputStream);
            storageLocation = tempProperties.getProperty("cameradashboard.video.location");
            checkState(!Strings.isNullOrEmpty(storageLocation), "Property not found: cameradashboard.video.location");
        } catch (Exception e) {
            throw new IllegalStateException("Invalid properties found in " + DASHBOARD_CONFIG_FILE, e);
        }
        cameraDashboardProperties = tempProperties;

        final File storageLocationDirectory = new File(storageLocation);
        if (!storageLocationDirectory.exists()) {
            if (!storageLocationDirectory.mkdirs()) {
                throw new IllegalStateException("Could not make storage location");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T[] nullToEmptyArray(final T[] possibleArray) {
        if (possibleArray == null) {
            return (T[]) new Object[0];
        }

        return possibleArray;
    }

    public static Properties getCameraDashboardProperties() {
        if (cameraDashboardProperties == null) {
            throw new IllegalStateException(
                    "Must call validateStateOrThrow() first, I'm sorry the guice-vaadin plugin won't let me define a non-default constructor for the Guice module");
        }

        final Properties copy = new Properties();
        copy.putAll(cameraDashboardProperties);
        return copy;
    }
}
