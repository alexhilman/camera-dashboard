package com.alexhilman.cameradashboard.ui.mail;

import com.alexhilman.cameradashboard.ui.conf.Camera;
import com.alexhilman.cameradashboard.ui.conf.CameraConfiguration;
import com.alexhilman.cameradashboard.ui.video.MovieFileManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.RejectException;

import javax.inject.Named;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 */
@Singleton
public class MessageHandlerFactory implements org.subethamail.smtp.MessageHandlerFactory {
    private static final Logger LOG = LoggerFactory.getLogger(MessageHandlerFactory.class);
    private final MovieFileManager movieFileManager;
    private final String videoEmailAddress;
    private final CameraConfiguration cameraConfiguration;
    volatile boolean rejected;

    @Inject
    public MessageHandlerFactory(final MovieFileManager movieFileManager,
                                 final @Named("cameradashboard.smtp.receiveAddress") String videoEmailAddress,
                                 final CameraConfiguration cameraConfiguration) {
        this.movieFileManager = movieFileManager;
        this.videoEmailAddress = videoEmailAddress;
        this.cameraConfiguration = cameraConfiguration;
    }

    @Override
    public MessageHandler create(MessageContext ctx) {
        return new Handler(ctx);
    }

    class Handler implements MessageHandler {
        MessageContext ctx;
        private String from;
        private Camera camera;
        private List<File> attachments = new ArrayList<>();

        public Handler(MessageContext ctx) {
            this.ctx = ctx;
        }

        public void from(String from) throws RejectException {
            this.from = from;
            final String cameraName = Stream.of(from.split("@"))
                                            .findFirst()
                                            .orElseThrow(() -> {
                                                rejected = true;
                                                LOG.info("Rejecting e-mail; unacceptable sender: {}", from);
                                                return new RejectException("Unacceptable sender: " + from);
                                            });

            camera = cameraConfiguration
                    .getCameras()
                    .stream()
                    .filter(camera -> camera.getName()
                                            .equals(cameraName))
                    .findFirst()
                    .orElseThrow(() -> {
                        rejected = true;
                        LOG.info("Rejecting e-mail; unacceptable camera: {}", from);
                        return new RejectException("Unacceptable camera: " + from);
                    });
        }

        public void recipient(String recipient) throws RejectException {
            if (rejected) {
                return;
            }

            if (!videoEmailAddress.equalsIgnoreCase(recipient.trim())) {
                rejected = true;
                LOG.info("Rejecting e-mail from {}; unknown recipient: {}", from, recipient);
                throw new RejectException("Unknown e-mail: " + recipient);
            }
        }

        @Override
        public void data(InputStream data) throws IOException {
            if (rejected) {
                return;
            }

            final File tmpFolder = movieFileManager.getTempFolderForCamera(camera);

            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(data));

            String line = null, boundary = null;
            while ((line = bufferedReader.readLine()) != null) {
                if (boundary == null) {
                    if (line.startsWith("Content-Type: multipart/mixed; boundary=\"")) {
                        boundary = "--" + line.substring("Content-Type: multipart/mixed; boundary=\"".length(),
                                                         line.lastIndexOf('"'));
                    }
                    continue;
                } //else if ()
            }
        }

        public void done() {
            if (!rejected) {
                LOG.info("Received {} files from {}", attachments.size(), from);
            }
        }
    }
}
