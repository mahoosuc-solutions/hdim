package com.healthdata.quality.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SavedReportEntity Tests")
class SavedReportEntityTest {

    @Test
    @DisplayName("Should populate defaults on create when missing")
    void shouldPopulateDefaultsOnCreate() {
        SavedReportEntity entity = SavedReportEntity.builder()
            .tenantId("tenant-1")
            .reportType("PATIENT")
            .reportName("Test Report")
            .reportData("{}")
            .createdBy("user-1")
            .generatedAt(null)
            .build();

        entity.onCreate();

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getGeneratedAt()).isNotNull();
        assertThat(entity.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("Should preserve provided values on create")
    void shouldPreserveProvidedValuesOnCreate() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime generatedAt = LocalDateTime.now().minusHours(2);

        SavedReportEntity entity = SavedReportEntity.builder()
            .id(id)
            .tenantId("tenant-1")
            .reportType("POPULATION")
            .reportName("Report")
            .reportData("{}")
            .createdBy("user-1")
            .createdAt(createdAt)
            .generatedAt(generatedAt)
            .status("FAILED")
            .build();

        entity.onCreate();

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getGeneratedAt()).isEqualTo(generatedAt);
        assertThat(entity.getStatus()).isEqualTo("FAILED");
    }
}
