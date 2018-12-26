package com.alexhilman.cameradashboard.ui.mail.model;

import java.util.Arrays;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 *
 */
public class Attachment {
    private final String fileName;
    private final byte[] content;

    public Attachment(final String fileName, final byte[] content) {
        this.fileName = fileName;
        checkNotNull(content, "content cannot be null");

        this.content = Arrays.copyOf(content, content.length);
    }

    private Attachment(final Builder builder) {
        this(builder.fileName, builder.content);
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getContent() {
        return Arrays.copyOf(content, content.length);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(fileName);
        result = 31 * result + Arrays.hashCode(content);
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Attachment that = (Attachment) o;
        return Objects.equals(fileName, that.fileName) &&
                Arrays.equals(content, that.content);
    }

    @Override
    public String toString() {
        return "Attachment{" +
                "fileName='" + fileName + '\'' +
                '}';
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(final Attachment copy) {
        Builder builder = new Builder();
        builder.fileName = copy.getFileName();
        builder.content = copy.getContent();
        return builder;
    }


    public static final class Builder {
        private String fileName;
        private byte[] content;

        private Builder() {}

        public Builder setFileName(final String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder setContent(final byte[] content) {
            this.content = content;
            return this;
        }

        public Attachment build() {
            return new Attachment(this);
        }
    }
}
