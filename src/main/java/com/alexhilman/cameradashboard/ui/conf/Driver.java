package com.alexhilman.cameradashboard.ui.conf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 */
public class Driver {
    private final Type type;
    private final String implementation;
    private final String networkAddress;

    @JsonCreator
    public Driver(@JsonProperty(value = "type", required = true) final Type type,
                  @JsonProperty(value = "implementation", required = true) final String implementation,
                  @JsonProperty(value = "networkAddress", required = true) final String networkAddress) {
        this.type = checkNotNull(type, "type cannot be null");
        this.implementation = checkNotNull(implementation, "implementation cannot be null");
        this.networkAddress = checkNotNull(networkAddress, "networkAddress cannot be null");
    }

    public Type getType() {
        return type;
    }

    public String getImplementation() {
        return implementation;
    }

    public String getNetworkAddress() {
        return networkAddress;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Driver driver = (Driver) o;

        if (type != driver.type) return false;
        if (!implementation.equals(driver.implementation)) return false;
        return networkAddress.equals(driver.networkAddress);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + implementation.hashCode();
        result = 31 * result + networkAddress.hashCode();
        return result;
    }
}
