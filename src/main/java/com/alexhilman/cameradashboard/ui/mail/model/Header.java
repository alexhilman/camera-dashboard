package com.alexhilman.cameradashboard.ui.mail.model;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 */
public class Header {
    private final String name;
    private final String value;

    private Header(final Builder builder) {
        name = builder.name;
        value = builder.value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Header header = (Header) o;
        return Objects.equals(name, header.name) &&
                Objects.equals(value, header.value);
    }

    @Override
    public String toString() {
        return name + ": " + value;
    }

    public static final class Builder {
        private String name;
        private String value;

        private Builder() {}

        public Builder setName(final String name) {
            this.name = name;
            return this;
        }

        public Builder setValue(final String value) {
            this.value = value;
            return this;
        }

        public Header build() {
            return new Header(this);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(final Header copy) {
        Builder builder = new Builder();
        builder.name = copy.getName();
        builder.value = copy.getValue();
        return builder;
    }

    public static Header of(final String header) {
        checkNotNull(header, "header cannot be null");

        final int i = header.indexOf(":");
        if (i < 0) {
            throw new IllegalArgumentException("Invalid header: " + header);
        }

        return Header.newBuilder()
                     .setName(header.substring(0, i).trim())
                     .setValue(header.substring(i + 1).trim())
                     .build();
    }
}
