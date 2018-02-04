package com.alexhilman.cameradashboard.ui.conf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URL;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 */
public class StreamSource {
    private final StreamQuality quality;
    private final URL url;

    @JsonCreator
    public StreamSource(@JsonProperty(value = "quality", required = true) final StreamQuality quality,
                        @JsonProperty(value = "url", required = true) final URL url) {
        this.quality = checkNotNull(quality, "quality cannot be null");
        this.url = checkNotNull(url, "url cannot be null");
    }

    public StreamQuality getQuality() {
        return quality;
    }

    public URL getUrl() {
        return url;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final StreamSource that = (StreamSource) o;

        if (quality != that.quality) return false;
        return url != null ? url.equals(that.url) : that.url == null;
    }

    @Override
    public int hashCode() {
        int result = quality != null ? quality.hashCode() : 0;
        result = 31 * result + (url != null ? url.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "StreamSource{" +
                "quality=" + quality +
                ", url=" + url +
                '}';
    }

    public enum StreamQuality {
        high, medium, low;
    }
}
