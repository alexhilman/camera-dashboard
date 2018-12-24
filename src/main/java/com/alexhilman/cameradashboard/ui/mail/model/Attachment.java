package com.alexhilman.cameradashboard.ui.mail.model;

import com.google.common.collect.Lists;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 */
public class Attachment {
    private final List<Header> headers;

    private Attachment(final Builder builder) {headers = builder.headers;}

    public List<Header> getHeaders() {
        return headers;
    }


    public static final class Builder {
        private List<Header> headers = Lists.newArrayListWithCapacity(5);

        private Builder() {}

        public Builder addHeader(final Header header) {
            checkNotNull(header, "header cannot be null");

            headers.add(header);
            return this;
        }

        public Attachment build() {
            return new Attachment(this);
        }

        public List<Header> getHeaders() {
            return headers;
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(final Attachment copy) {
        Builder builder = new Builder();
        builder.headers = copy.getHeaders();
        return builder;
    }
}
