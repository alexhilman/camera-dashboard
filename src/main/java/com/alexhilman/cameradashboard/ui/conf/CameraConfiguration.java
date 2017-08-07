package com.alexhilman.cameradashboard.ui.conf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Immutable configuration read from the {@code cameras.json} file.
 */
@ThreadSafe
@Immutable
public class CameraConfiguration {
    private final List<Camera> cameras;

    @JsonCreator
    public CameraConfiguration(@JsonProperty(value = "cameras", required = true) final List<Camera> cameras) {
        checkNotNull(cameras, "cameras cannot be null");
        this.cameras = ImmutableList.copyOf(cameras);
    }

    public List<Camera> getCameras() {
        return cameras;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final CameraConfiguration that = (CameraConfiguration) o;

        return cameras.equals(that.cameras);
    }

    @Override
    public int hashCode() {
        return cameras.hashCode();
    }

    @Override
    public String toString() {
        return "CameraConfiguration{" +
                "cameras=" + cameras +
                '}';
    }
}
