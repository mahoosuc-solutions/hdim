package com.healthdata.fhir.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CoverageEntity Tests")
class CoverageEntityTest {

    @Test
    void shouldSetAuditFieldsOnCreateAndUpdate() {
        CoverageEntity entity = CoverageEntity.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .resourceJson("{}")
                .patientId(UUID.randomUUID())
                .build();

        entity.onCreate();
        Instant createdAt = entity.getCreatedAt();
        Instant modifiedAt = entity.getLastModifiedAt();

        assertThat(createdAt).isNotNull();
        assertThat(modifiedAt).isNotNull();

        entity.onUpdate();
        assertThat(entity.getLastModifiedAt()).isAfterOrEqualTo(modifiedAt);
    }

    @Test
    void shouldEvaluateActiveCoverageWindows() {
        CoverageEntity active = CoverageEntity.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .resourceJson("{}")
                .patientId(UUID.randomUUID())
                .status("active")
                .periodStart(Instant.now().minusSeconds(3600))
                .periodEnd(Instant.now().plusSeconds(3600))
                .build();

        CoverageEntity inactive = CoverageEntity.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .resourceJson("{}")
                .patientId(UUID.randomUUID())
                .status("cancelled")
                .build();

        CoverageEntity expired = CoverageEntity.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .resourceJson("{}")
                .patientId(UUID.randomUUID())
                .status("active")
                .periodEnd(Instant.now().minusSeconds(10))
                .build();

        assertThat(active.isCurrentlyActive()).isTrue();
        assertThat(inactive.isCurrentlyActive()).isFalse();
        assertThat(expired.isCurrentlyActive()).isFalse();
    }
}
