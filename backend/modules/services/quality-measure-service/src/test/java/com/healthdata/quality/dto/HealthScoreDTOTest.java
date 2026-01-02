package com.healthdata.quality.dto;

import com.healthdata.quality.persistence.HealthScoreEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HealthScoreDTOTest {

    @Test
    void shouldBuildComponentScoresAndTrend() {
        HealthScoreDTO dto = HealthScoreDTO.builder()
            .physicalHealthScore(80.0)
            .mentalHealthScore(70.0)
            .socialDeterminantsScore(60.0)
            .preventiveCareScore(50.0)
            .chronicDiseaseScore(40.0)
            .scoreDelta(6.0)
            .build();

        HealthScoreDTO.ComponentScoresDTO components = dto.getComponentScores();

        assertThat(components.getPhysical()).isEqualTo(80);
        assertThat(components.getMental()).isEqualTo(70);
        assertThat(components.getSocial()).isEqualTo(60);
        assertThat(components.getPreventive()).isEqualTo(50);
        assertThat(components.getChronicDisease()).isEqualTo(40);
        assertThat(dto.getTrend()).isEqualTo("improving");
    }

    @Test
    void shouldReturnStableTrendWhenDeltaSmall() {
        HealthScoreDTO dto = HealthScoreDTO.builder()
            .scoreDelta(2.0)
            .build();

        assertThat(dto.getTrend()).isEqualTo("stable");
    }

    @Test
    void shouldReturnNewTrendWhenDeltaMissing() {
        HealthScoreDTO dto = new HealthScoreDTO();

        assertThat(dto.getTrend()).isEqualTo("new");
    }

    @Test
    void shouldMapFromEntityWithInterpretation() {
        HealthScoreEntity entity = HealthScoreEntity.builder()
            .id(UUID.randomUUID())
            .patientId(UUID.randomUUID())
            .tenantId("tenant-1")
            .overallScore(92.0)
            .physicalHealthScore(90.0)
            .mentalHealthScore(88.0)
            .socialDeterminantsScore(85.0)
            .preventiveCareScore(95.0)
            .chronicDiseaseScore(90.0)
            .calculatedAt(Instant.now())
            .previousScore(80.0)
            .significantChange(true)
            .changeReason("increase")
            .build();

        HealthScoreDTO dto = HealthScoreDTO.fromEntity(entity);

        assertThat(dto.getScoreLevel()).isEqualTo("excellent");
        assertThat(dto.getInterpretation()).contains("Excellent overall health");
        assertThat(dto.getScoreDelta()).isEqualTo(12.0);
    }
}
