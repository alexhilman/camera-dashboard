package com.alexhilman.cameradashboard.ui;

import com.google.common.base.Strings;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ShutdownHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.FragmentConfiguration;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.MetaInfConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
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
    public static final int PORT = 9090;
    public static final String CAMERAS_CONFIG_FILE = "cameras.json";
    public static final String DASHBOARD_CONFIG_FILE = "camera-dashboard.properties";
    public static final String CONFIGURATION_DIRECTORY_SYSTEM_PROPERTY = "camdash.configurationDir";
    private static final Logger LOG = LoggerFactory.getLogger(App.class);
    private static volatile Properties cameraDashboardProperties;

    public static void main(String[] args) throws Exception {
        validateStateOrThrow();

        Resource.setDefaultUseCaches(false);

        final Server server = new Server(PORT);

        final ThrowyWebAppContext webAppContext = new ThrowyWebAppContext();
        webAppContext.setAttribute("org.eclipse.jetty.websocket.jsr356", Boolean.TRUE);
        webAppContext.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                                   ".*\\.jar$|.*/classes/.*");
        webAppContext.setAttribute("org.eclipse.jetty.server.webapp.WebInfIncludeJarPattern",
                                   ".*\\.jar$|.*/classes/.*");

        webAppContext.setDisplayName("Security Dashboard");
        webAppContext.setContextPath("/");

        webAppContext.setConfigurations(new Configuration[]{
                new AnnotationConfiguration(),
                new PlusConfiguration(),
                new WebInfConfiguration(),
                new JettyWebXmlConfiguration(),
                new WebXmlConfiguration(),
                new MetaInfConfiguration(),
                new FragmentConfiguration(),
                new EnvConfiguration(),
        });

        webAppContext.setBaseResource(Resource.newClassPathResource("webapp"));

        final ShutdownHandler shutdownHandler = new ShutdownHandler("ee2d422c-c149-4cf8-a8bd-e2c1c2130c16",
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
        handlerList.setHandlers(new Handler[]{shutdownHandler, webAppContext});
        server.setHandler(handlerList);

        try {
            server.start();
            if (server.isFailed()) {
                server.stop();
                LOG.error("Error in application initialization, cannot start up");
                System.exit(1);

            } else {
                LOG.info("Jetty started, please go to http://localhost:" + PORT + webAppContext.getContextPath());
                server.join();
            }
        } catch (Exception e) {
            LOG.error("Error Starting " + webAppContext.getDisplayName(), e);
            //Just go ahead and quit
            System.exit(1);
        }
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

    public static class ThrowyWebAppContext extends WebAppContext {
        @Override
        protected void doStart() throws Exception {
            super.doStart();
            final Throwable unavailableException = getUnavailableException();
            if (unavailableException != null) {
                throw (Exception) unavailableException;
            }
        }
    }
}
