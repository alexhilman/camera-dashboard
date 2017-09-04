package com.alexhilman.cameradashboard.ui.conf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 */
public class Camera {
    private final String make;
    private final String model;
    private final Driver driver;

    @JsonCreator
    public Camera(@JsonProperty(value = "make", required = true) final String make,
                  @JsonProperty(value = "model", required = true) final String model,
                  @JsonProperty(value = "driver", required = true) final Driver driver) {
        this.make = checkNotNull(make, "make cannot be null");
        this.model = checkNotNull(model, "model cannot be null");
        this.driver = checkNotNull(driver, "driver cannot be null");
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Camera camera = (Camera) o;

        if (!make.equals(camera.make)) return false;
        if (!model.equals(camera.model)) return false;
        return driver.equals(camera.driver);
    }

    @Override
    public int hashCode() {
        int result = make.hashCode();
        result = 31 * result + model.hashCode();
        result = 31 * result + driver.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Camera{" +
                "make='" + make + '\'' +
                ", model='" + model + '\'' +
                ", driver=" + driver +
                '}';
    }
}