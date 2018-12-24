package com.alexhilman.cameradashboard.ui.mail;

import com.alexhilman.cameradashboard.ui.mail.model.Attachment;
import com.alexhilman.cameradashboard.ui.mail.model.Email;
import com.alexhilman.cameradashboard.ui.mail.model.Header;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 */
public class MailStreamParser {
    private static final Pattern BOUNDARY_PATTERN = Pattern.compile(".*boundary *= *\"([^\"])+\".*");

    public Email parse(final InputStream in) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        final Email.Builder email = Email.newBuilder();
        String line = null;
        Header contentType = null;
        String boundaryKey = null;
        Attachment.Builder attachmentBuilder = null;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                if (attachmentBuilder != null) {
                    final Attachment attachment = attachmentBuilder.build();
                    if (email.getBody() == null &
                            attachment.getHeaders()
                                      .stream()
                                      .anyMatch(header -> header.getValue().contains("text/plain"))) {
                        // TODO get content of body
//                        email.setBody(attachment.getContent)
                    }
                }
                attachmentBuilder = Attachment.newBuilder();
                continue;
            }

            if (attachmentBuilder == null) {
                if (line.matches("^[a-zA-Z0-9_-]+:.*")) {
                    final Header header = Header.of(line);
                    email.addHeader(header);

                    if (isContentType(header)) {
                        contentType = header;
                        if (isMultipart(contentType)) {
                            boundaryKey = "--" + extractBoundaryKey(contentType);
                        }
                    }
                }
            } else {
                if (line.equals(boundaryKey)) {

                } else {
                    attachmentBuilder.addHeader(Header.of(line));
                }
            }
        }

        return email.build();
    }

    private String extractBoundaryKey(final Header header) {
        checkNotNull(header, "header cannot be null");
        checkArgument(isContentType(header), "header is not a Content-Type specification");

        final Matcher matcher = BOUNDARY_PATTERN.matcher(header.getValue());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("No boundary found in header: " + header);
        }
        return matcher.group(1);
    }

    private boolean isMultipart(final Header contentType) {
        checkNotNull(contentType, "contentType cannot be null");
        checkArgument(isContentType(contentType), "not a content-type");

        return contentType.getValue().contains("multipart");
    }

    private boolean isContentType(final Header header) {
        return header.getName().equalsIgnoreCase("content-type");
    }
}
