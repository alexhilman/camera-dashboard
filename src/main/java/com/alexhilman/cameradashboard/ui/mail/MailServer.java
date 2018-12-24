package com.alexhilman.cameradashboard.ui.mail;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.subethamail.smtp.server.SMTPServer;

/**
 */
@Singleton
public class MailServer {
    private final int port;
    private final MessageHandlerFactory myFactory;
    private volatile SMTPServer smtpServer;

    @Inject
    public MailServer(final MessageHandlerFactory myFactory) {
        this(myFactory, 2500);
    }

    MailServer(final MessageHandlerFactory myFactory, final int port) {
        this.myFactory = myFactory;
        this.port = port;
    }

    public void start() {
        getSmtpServer().start();
    }

    public void stop() {
        synchronized (this) {
            getSmtpServer().stop();
            smtpServer = null;
        }
    }

    public int getPort() {
        return port;
    }

    private SMTPServer getSmtpServer() {
        SMTPServer server = smtpServer;

        if (server == null) {
            synchronized (this) {
                server = smtpServer;

                if (server == null) {
                    server = smtpServer = buildSmtpServer();
                }
            }
        }
        return server;
    }

    private SMTPServer buildSmtpServer() {
        final SMTPServer smtpServer = new SMTPServer(myFactory);
        smtpServer.setPort(port);
        return smtpServer;
    }
}
