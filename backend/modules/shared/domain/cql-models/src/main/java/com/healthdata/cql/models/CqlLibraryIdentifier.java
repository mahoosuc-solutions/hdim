package com.healthdata.cql.models;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

/**
 * Canonical identifier for a CQL library (name + semantic version).
 */
public record CqlLibraryIdentifier(String libraryName, String version) {

    public CqlLibraryIdentifier {
        if (StringUtils.isBlank(libraryName)) {
            throw new IllegalArgumentException("libraryName must not be blank");
        }
        if (StringUtils.isBlank(version)) {
            throw new IllegalArgumentException("version must not be blank");
        }
        libraryName = libraryName.trim();
        version = version.trim();
    }

    @Override
    public String toString() {
        return libraryName + ":" + version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CqlLibraryIdentifier that)) {
            return false;
        }
        return libraryName.equalsIgnoreCase(that.libraryName)
                && version.equals(that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(libraryName.toLowerCase(), version);
    }
}
