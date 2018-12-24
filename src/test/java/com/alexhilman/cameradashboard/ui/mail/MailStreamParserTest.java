package com.alexhilman.cameradashboard.ui.mail;

import com.alexhilman.cameradashboard.ui.mail.model.Email;
import com.alexhilman.cameradashboard.ui.mail.model.Header;
import com.sun.mail.smtp.SMTPMessage;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Properties;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class MailStreamParserTest {
    private static final Logger LOG = LoggerFactory.getLogger(MailStreamParserTest.class);
    private final MailStreamParser mailStreamParser = new MailStreamParser();
    private URL emailResource;

    @Before
    public void setupMailResource() {
        emailResource = getClass().getResource("/com/alexhilman/cameradashboard/mail/email.raw");

        assertThat(emailResource, is(notNullValue()));
    }

    @Test
    public void shouldParse() throws IOException, MessagingException {
        final SMTPMessage smtpMessage = new SMTPMessage(Session.getDefaultInstance(new Properties()),
                                                        emailResource.openStream());

        LOG.info("Headers:\n{}", String.join("\n", Collections.list(smtpMessage.getAllHeaderLines())));

        final MimeMultipart content = (MimeMultipart) smtpMessage.getContent();
        IntStream.range(0, content.getCount())
                 .mapToObj(i -> {
                     try {
                         return content.getBodyPart(i);
                     } catch (Exception e) {
                         throw new RuntimeException(e);
                     }
                 })
                 .map(bodyPart -> {
                     try {
                         LOG.info("Part headers:\n{}", Collections.list(bodyPart.getAllHeaders())
                                                                  .stream()
                                                                  .map(header -> header.getName() + ": " + header
                                                                          .getValue())
                                                                  .collect(joining("\n")));

                         final Object content1 = bodyPart.getContent();
                         return content1;
                     } catch (Exception e) {
                         throw new RuntimeException(e);
                     }
                 }).forEach(bodyPart -> {});
    }

    @Test
    public void shouldParseHeaders() throws Exception {
        final Email email;
        try (final InputStream in = emailResource.openStream()) {
            email = mailStreamParser.parse(in);
        }

        assertThat(email, is(notNullValue()));

        assertThat(email.getHeaders(),
                   hasItems(Header.of("Date: Thu, 08 Mar 2018 09:52:25 -0700"),
                            Header.of("From: dcs-936l@earl.pi"),
                            Header.of("User-Agent: msmtp"),
                            Header.of("MIME-Version: 1.0"),
                            Header.of("To: recording@earl.pi"),
                            Header.of("Subject: This is a snapshot test mail from DCS-936L"),
                            Header.of("Content-Type: multipart/mixed; boundary=\"00137867=\"")));
    }

    @Test
    public void shouldParseBody() throws IOException {
        final Email email;
        try (final InputStream in = emailResource.openStream()) {
            email = mailStreamParser.parse(in);
        }

        assertThat(email.getBody(), is("Camera Name: DCS-936L\n" +
                                               "MAC Address: B0:C5:54:37:0E:92\n" +
                                               "IP Address: 192.168.1.4\n" +
                                               "Time: 2018/03/08 09:52:25"));
    }
}