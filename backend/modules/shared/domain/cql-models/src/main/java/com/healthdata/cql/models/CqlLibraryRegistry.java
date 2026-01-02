package com.healthdata.cql.models;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.healthdata.common.validation.ValidationResult;

/**
 * Thread-safe registry of CQL library descriptors.
 */
public class CqlLibraryRegistry {

    private final Map<CqlLibraryIdentifier, CqlLibraryDescriptor> descriptors = new ConcurrentHashMap<>();

    /**
     * Registers a new descriptor after validation.
     *
     * @throws IllegalStateException if the identifier is already registered
     * @throws IllegalArgumentException if validation fails
     */
    public void register(CqlLibraryDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        ValidationResult result = descriptor.validate();
        if (!result.isValid()) {
            throw new IllegalArgumentException("Descriptor failed validation: " + result.getErrors());
        }
        CqlLibraryIdentifier identifier = descriptor.toIdentifier();
        CqlLibraryDescriptor previous = descriptors.putIfAbsent(identifier, descriptor);
        if (previous != null) {
            throw new IllegalStateException("Library %s already registered".formatted(identifier));
        }
    }

    /**
     * Resolves a descriptor for the supplied identifier.
     */
    public Optional<CqlLibraryDescriptor> resolve(String libraryName, String version) {
        if (libraryName == null || version == null) {
            return Optional.empty();
        }
        CqlLibraryIdentifier identifier = new CqlLibraryIdentifier(libraryName, version);
        return Optional.ofNullable(descriptors.get(identifier));
    }
}
