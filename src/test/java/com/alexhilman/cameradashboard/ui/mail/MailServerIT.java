package com.alexhilman.cameradashboard.ui.mail;

import com.alexhilman.cameradashboard.ui.conf.Camera;
import com.alexhilman.cameradashboard.ui.conf.CameraConfiguration;
import com.alexhilman.cameradashboard.ui.video.MovieFileManager;
import com.alexhilman.cameradashboard.ui.video.MovieHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.subethamail.smtp.client.SmartClient;

import java.util.concurrent.TimeUnit;

import static com.alexhilman.cameradashboard.ui.CameraConfigurationReader.readCameraConfig;

public class MailServerIT {
    public static final String EMAIL_ADDRESS = "recording@earl.pi";
    private MailServer mailServer;
    private Camera camera;
    private MovieFileManager movieFileManager;
    private MessageHandlerFactory messageHandlerFactory;

    @Before
    public void setup() {
        final CameraConfiguration cameraConfiguration = readCameraConfig();
        camera = cameraConfiguration.getCameras().get(0);
        movieFileManager = new MovieFileManager(cameraConfiguration, new MovieHelper(), "/tmp/.camera-dashboard");
        messageHandlerFactory = new MessageHandlerFactory(movieFileManager, EMAIL_ADDRESS, cameraConfiguration);
        mailServer = new MailServer(messageHandlerFactory, 2501);

        mailServer.start();
    }

    @After
    public void tearDown() {
        mailServer.stop();
    }

    @Test
    public void shouldReceiveMail() throws Exception {
        final SmartClient smartClient = new SmartClient("localhost", mailServer.getPort(), "localhost");

        try {
            smartClient.from(camera.getName() + "@localhost.localdomain");
            smartClient.to(EMAIL_ADDRESS);
            smartClient.dataStart();
            final String body = "Some body";
            smartClient.dataWrite(body.getBytes(), body.length());
            smartClient.dataEnd();
        } finally {
            smartClient.quit();
        }
    }

    @Test
    @Ignore
    public void shouldStayRunning() throws InterruptedException {
        Thread.sleep(TimeUnit.HOURS.toMillis(1));
    }
}