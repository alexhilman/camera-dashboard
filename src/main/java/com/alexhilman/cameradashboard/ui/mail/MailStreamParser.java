package com.alexhilman.cameradashboard.ui.mail;

import com.alexhilman.cameradashboard.ui.mail.model.Attachment;
import com.alexhilman.cameradashboard.ui.mail.model.Email;
import com.alexhilman.cameradashboard.ui.mail.model.Header;
import com.helger.commons.base64.Base64;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class MailStreamParser {
    private static final Pattern BOUNDARY_PATTERN = Pattern.compile(".*boundary ?= ?\"([^\"]+)\".*");
    private static final Pattern ATTACHMENT_NAME_PATTERN = Pattern.compile(".*name ?= ?\"([^\"]+)\".*");
    private static final Pattern ATTACHMENT_FILE_NAME_PATTERN = Pattern.compile(".*filename ?= ?\"([^\"]+)\".*");

    public Email parse(final InputStream in) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        final Email.Builder email = Email.newBuilder();
        String line;
        boolean inHeaders = true;
        final PartParser partParser = new PartParser();
        while ((line = reader.readLine()) != null) {
            if (inHeaders) {
                if (line.isEmpty()) {
                    inHeaders = false;
                    continue;
                }
                final Header header = Header.of(line);
                if (isContentTypeHeader(header)) {
                    if (isMultipartMixed(header)) {
                        partParser.setBoundary(extractBoundary(header));
                    }
                }
                email.addHeader(header);
            } else {
                partParser.digest(line);
            }
        }

        email.setBody(partParser.getBody());
        email.addAttachments(partParser.getAttachments());

        return email.build();
    }

    private String extractBoundary(final Header header) {
        final String boundary;
        final Matcher matcher = BOUNDARY_PATTERN.matcher(header.getValue());
        if (!matcher.matches()) {
            throw new RuntimeException("Mixed body without a boundary");
        }
        boundary = "--" + matcher.group(1);
        return boundary;
    }

    private enum ParsingStage implements StageAdvancer {
        HEADERS {
            @Override
            public ParsingStage advance() {
                return BODY;
            }
        },
        BODY {
            @Override
            public ParsingStage advance() {
                return ATTACHMENT;
            }
        },
        ATTACHMENT {
            @Override
            public ParsingStage advance() {
                return this;
            }
        }
    }

    private interface StageAdvancer {
        ParsingStage advance();
    }

    private static boolean isMultipartMixed(final Header header) {
        return header.getValue().contains("multipart/mixed");
    }

    private static boolean isContentTypeHeader(final Header header) {
        return header.getName().equalsIgnoreCase("Content-Type");
    }

    private static boolean isContentDisposition(final Header header) {
        return header.getName().equalsIgnoreCase("Content-Disposition");
    }

    private static class PartParser {
        private final List<Attachment> attachments = new ArrayList<>();
        private StringBuilder contentBuilder;
        private String body;
        private ParsingStage stage = ParsingStage.HEADERS;
        private String boundary = null;
        private String attachmentFileName;

        public String getBody() {
            return body;
        }

        public List<Attachment> getAttachments() {
            return attachments;
        }

        private PartParser setBoundary(final String boundary) {
            this.boundary = boundary;
            return this;
        }

        private void digest(final String line) {
            assert line != null;

            if (line.startsWith(boundary)) {
                finalizeStage();
                stage = ParsingStage.HEADERS;
                return;
            }

            switch (stage) {
                case HEADERS: {
                    if (line.isEmpty()) {
                        stage = attachmentFileName == null ? ParsingStage.BODY : ParsingStage.ATTACHMENT;
                        contentBuilder = new StringBuilder();
                        return;
                    }

                    parseHeader(line);
                }
                break;
                case BODY: {
                    contentBuilder.append(line).append("\n");
                }
                break;
                case ATTACHMENT: {
                    contentBuilder.append(line);
                }
                break;
                default:
                    throw new UnsupportedOperationException("Unknown stage: " + stage);
            }
        }

        private void parseHeader(final String line) {
            final Header header = Header.of(line);
            final String value = header.getValue();
            if (isContentTypeHeader(header)) {
                if (value.startsWith("text")) {
                    attachmentFileName = null;
                } else if (value.startsWith("image") || value.startsWith("video")) {
                    final Matcher matcher = ATTACHMENT_NAME_PATTERN.matcher(value);
                    attachmentFileName = matcher.matches() ? matcher.group(1) : null;
                } else {
                    throw new UnsupportedOperationException("Unknown Content-Type: " + value);
                }
            } else if (isContentDisposition(header) && header.getValue().startsWith("attachment")) {
                final Matcher matcher = ATTACHMENT_FILE_NAME_PATTERN.matcher(value);
                if (matcher.matches()) {
                    attachmentFileName = matcher.group(1);
                }
            }
        }


        private void finalizeStage() {
            switch (stage) {
                case BODY:
                    body = contentBuilder.toString();
                    break;
                case ATTACHMENT:
                    final byte[] decode;
                    try {
                        decode = Base64.decode(contentBuilder.toString());
                    } catch (IOException e) {
                        throw new RuntimeException("Could not decode attachment: " + attachmentFileName, e);
                    }
                    attachments.add(Attachment.newBuilder()
                                              .setFileName(attachmentFileName)
                                              .setContent(decode)
                                              .build());
            }
        }
    }
}
