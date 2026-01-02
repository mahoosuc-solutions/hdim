package com.healthdata.cql.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CqlLibraryRegistryTest {

    private final CqlLibraryRegistry registry = new CqlLibraryRegistry();

    @Test
    @DisplayName("register should store descriptor by identifier")
    void register_shouldStoreDescriptor() {
        CqlLibraryDescriptor descriptor = new CqlLibraryDescriptor("HEDIS_CBP", "1.0.0", """
                library HEDIS_CBP version '1.0.0'
                """);

        registry.register(descriptor);

        Optional<CqlLibraryDescriptor> resolved = registry.resolve("HEDIS_CBP", "1.0.0");
        assertThat(resolved).isPresent();
        assertThat(resolved.get()).isSameAs(descriptor);
    }

    @Test
    @DisplayName("register should reject duplicate identifiers")
    void register_shouldRejectDuplicateIdentifiers() {
        CqlLibraryDescriptor descriptor = new CqlLibraryDescriptor("HEDIS_CBP", "1.0.0", """
                library HEDIS_CBP version '1.0.0'
                """);
        registry.register(descriptor);

        CqlLibraryDescriptor duplicate = new CqlLibraryDescriptor("HEDIS_CBP", "1.0.0", """
                library HEDIS_CBP version '1.0.0'
                """);

        assertThatThrownBy(() -> registry.register(duplicate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    @DisplayName("resolve should return empty when not registered")
    void resolve_shouldReturnEmptyWhenMissing() {
        assertThat(registry.resolve("Unknown", "1.0.0")).isEmpty();
    }
}
