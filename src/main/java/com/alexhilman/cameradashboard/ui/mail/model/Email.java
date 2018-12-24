package com.alexhilman.cameradashboard.ui.mail.model;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 */
public class Email {
    private final List<Header> headers;
    private final String body;
    private final List<Attachment> attachments;

    private Email(final Builder builder) {
        headers = builder.headers;
        body = builder.body;
        attachments = builder.attachments;
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    @Override
    public int hashCode() {
        return Objects.hash(headers, body, attachments);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Email email = (Email) o;
        return Objects.equals(headers, email.headers) &&
                Objects.equals(body, email.body) &&
                Objects.equals(attachments, email.attachments);
    }

    public static final class Builder {
        private List<Header> headers = Lists.newArrayListWithCapacity(5);
        private String body;
        private List<Attachment> attachments = Lists.newArrayListWithCapacity(2);

        private Builder() {}

        public List<Header> getHeaders() {
            return headers;
        }

        public String getBody() {
            return body;
        }

        public Builder setBody(final String body) {
            this.body = body;
            return this;
        }

        public List<Attachment> getAttachments() {
            return attachments;
        }

        public Builder setAttachments(final List<Attachment> attachments) {
            this.attachments = attachments;
            return this;
        }

        public Email build() {
            return new Email(this);
        }

        public void addHeader(final Header header) {
            checkNotNull(header, "header cannot be null");

            headers.add(header);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(final Email copy) {
        Builder builder = new Builder();
        builder.headers = copy.getHeaders();
        builder.body = copy.getBody();
        builder.attachments = copy.getAttachments();
        return builder;
    }
}
