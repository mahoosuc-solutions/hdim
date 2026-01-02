package com.healthdata.caregap.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("CareGapRecommendationEntity")
class CareGapRecommendationEntityTest {

    @Test
    @DisplayName("Should set defaults on create")
    void shouldSetDefaultsOnCreate() {
        CareGapRecommendationEntity entity = new CareGapRecommendationEntity();

        ReflectionTestUtils.invokeMethod(entity, "onCreate");

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getPriority()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should not override existing values")
    void shouldNotOverrideExistingValues() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2025-01-01T00:00:00Z");
        CareGapRecommendationEntity entity = new CareGapRecommendationEntity();
        entity.setId(id);
        entity.setCreatedAt(createdAt);
        entity.setPriority(5);

        ReflectionTestUtils.invokeMethod(entity, "onCreate");

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getPriority()).isEqualTo(5);
    }
}
