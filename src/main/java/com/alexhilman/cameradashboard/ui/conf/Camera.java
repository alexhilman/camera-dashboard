package com.alexhilman.cameradashboard.ui.conf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 */
public class Camera {
    private final String name;
    private final String username;
    private final String password;
    private final List<StreamSource> streams;

    @JsonCreator
    public Camera(@JsonProperty(value = "name", required = true) final String name,
                  @JsonProperty("username") final String username,
                  @JsonProperty("password") final String password,
                  @JsonProperty("streams") final List<StreamSource> streams) {
        this.name = checkNotNull(name, "name cannot be null");
        this.username = username;
        this.password = password;
        this.streams = ImmutableList.copyOf(checkNotNull(streams, "streams cannot be null"));
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public List<StreamSource> getStreams() {
        return streams;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Camera camera = (Camera) o;

        if (name != null ? !name.equals(camera.name) : camera.name != null) return false;
        if (username != null ? !username.equals(camera.username) : camera.username != null) return false;
        if (password != null ? !password.equals(camera.password) : camera.password != null) return false;
        return streams != null ? streams.equals(camera.streams) : camera.streams == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (streams != null ? streams.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Camera{" +
                "name='" + name + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", streams=" + streams +
                '}';
    }
}
