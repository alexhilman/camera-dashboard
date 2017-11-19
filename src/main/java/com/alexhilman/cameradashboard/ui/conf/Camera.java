package com.alexhilman.cameradashboard.ui.conf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 */
public class Camera {
    private final String name;
    private final String networkAddress;
    private final String username;
    private final String password;

    @JsonCreator
    public Camera(@JsonProperty(value = "name", required = true) final String name,
                  @JsonProperty(value = "networkAddress", required = true) final String networkAddress,
                  @JsonProperty("username") final String username,
                  @JsonProperty("password") final String password) {
        this.name = checkNotNull(name, "name cannot be null");
        this.networkAddress = checkNotNull(networkAddress, "networkAddress cannot be null");
        this.username = username;
        this.password = password;
    }

    public String getName() {
        return name;
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Camera camera = (Camera) o;

        if (name != null ? !name.equals(camera.name) : camera.name != null) return false;
        if (networkAddress != null ? !networkAddress.equals(camera.networkAddress) : camera.networkAddress != null)
            return false;
        if (username != null ? !username.equals(camera.username) : camera.username != null) return false;
        return password != null ? password.equals(camera.password) : camera.password == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (networkAddress != null ? networkAddress.hashCode() : 0);
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Camera{" +
                "name='" + name + '\'' +
                ", networkAddress='" + networkAddress + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
