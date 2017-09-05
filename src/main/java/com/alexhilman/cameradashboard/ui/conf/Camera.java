package com.alexhilman.cameradashboard.ui.conf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 */
public class Camera {
    private final String make;
    private final String model;
    private final String networkAddress;
    private final String username;
    private final String password;
    private final Driver driver;

    @JsonCreator
    public Camera(@JsonProperty(value = "make", required = true) final String make,
                  @JsonProperty(value = "model", required = true) final String model,
                  @JsonProperty(value = "networkAddress", required = true) final String networkAddress,
                  @JsonProperty("username") final String username,
                  @JsonProperty("password") final String password,
                  @JsonProperty(value = "driver", required = true) final Driver driver) {
        this.make = checkNotNull(make, "make cannot be null");
        this.model = checkNotNull(model, "model cannot be null");
        this.networkAddress = checkNotNull(networkAddress, "networkAddress cannot be null");
        this.username = username;
        this.password = password;
        this.driver = checkNotNull(driver, "driver cannot be null");
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public String getNetworkAddress() {
        return networkAddress;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Driver getDriver() {
        return driver;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Camera camera = (Camera) o;

        if (!make.equals(camera.make)) return false;
        if (!model.equals(camera.model)) return false;
        if (!networkAddress.equals(camera.networkAddress)) return false;
        if (username != null ? !username.equals(camera.username) : camera.username != null) return false;
        if (password != null ? !password.equals(camera.password) : camera.password != null) return false;
        return driver.equals(camera.driver);
    }

    @Override
    public int hashCode() {
        int result = make.hashCode();
        result = 31 * result + model.hashCode();
        result = 31 * result + networkAddress.hashCode();
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + driver.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Camera{" +
                "make='" + make + '\'' +
                ", model='" + model + '\'' +
                ", networkAddress='" + networkAddress + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", driver=" + driver +
                '}';
    }
}
