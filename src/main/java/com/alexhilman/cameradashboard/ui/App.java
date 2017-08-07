package com.alexhilman.cameradashboard.ui;

import com.google.common.base.Strings;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.net.URI;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;

/**
 * Hello world!
 */
public class App {
    public static final int PORT = 9090;
    public static final String CAMERAS_CONFIG_FILE = "cameras.json";
    public static final String CONFIGURATION_DIRECTORY_SYSTEM_PROPERTY = "camdash.configurationDir";

    public static void main(String[] args) {
        validateStateOrThrow();

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

        final File confDir = new File(URI.create(configurationDirectory));
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
    }

    @SuppressWarnings("unchecked")
    private static <T> T[] nullToEmptyArray(final T[] possibleArray) {
        if (possibleArray == null) {
            return (T[]) new Object[0];
        }

        return possibleArray;
    }
}
