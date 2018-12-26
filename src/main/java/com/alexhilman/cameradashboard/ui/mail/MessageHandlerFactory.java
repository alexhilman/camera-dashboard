package com.alexhilman.cameradashboard.ui.mail;

import com.alexhilman.cameradashboard.ui.conf.Camera;
import com.alexhilman.cameradashboard.ui.conf.CameraConfiguration;
import com.alexhilman.cameradashboard.ui.mail.model.Email;
import com.alexhilman.cameradashboard.ui.video.MovieFileManager;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.RejectException;

import javax.inject.Named;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 *
 */
@Singleton
public class MessageHandlerFactory implements org.subethamail.smtp.MessageHandlerFactory {
    private static final Logger LOG = LoggerFactory.getLogger(MessageHandlerFactory.class);
    private final MailStreamParser mailStreamParser;
    private final MovieFileManager movieFileManager;
    private final String videoEmailAddress;
    private final CameraConfiguration cameraConfiguration;
    volatile boolean rejected;

    @Inject
    public MessageHandlerFactory(final MailStreamParser mailStreamParser,
                                 final MovieFileManager movieFileManager,
                                 final @Named("cameradashboard.smtp.receiveAddress") String videoEmailAddress,
                                 final CameraConfiguration cameraConfiguration) {
        this.mailStreamParser = mailStreamParser;
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
        private Email email;

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

            camera = cameraConfiguration.getCameras()
                                        .stream()
                                        .filter(camera -> camera.getName().equalsIgnoreCase(cameraName))
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

            email = mailStreamParser.parse(data);
        }

        public void done() {
            if (!rejected) {
                LOG.info("Received {} files from {}", email.getAttachments().size(), from);
            }

            final File tempDir = Files.createTempDir();

            final List<File> attachmentFiles =
                    email.getAttachments()
                         .stream()
                         .map(attachment -> {
                             final File file = new File(tempDir, attachment.getFileName());
                             try (OutputStream out = new FileOutputStream(file)) {
                                 out.write(attachment.getContent());
                             } catch (Exception e) {
                                 throw new RuntimeException("Could not save file", e);
                             }
                             return file;
                         })
                         .collect(toList());

            final List<File> movieAttachments = attachmentFiles.stream()
                                                               .filter(file -> file.getName().endsWith(".mp4"))
                                                               .collect(toList());

            movieFileManager.addMoviesToRotatingPool(camera, movieAttachments);

            attachmentFiles.forEach(file -> {
                if (file.exists()) {
                    file.delete();
                }
            });
            tempDir.delete();
        }
    }
}
