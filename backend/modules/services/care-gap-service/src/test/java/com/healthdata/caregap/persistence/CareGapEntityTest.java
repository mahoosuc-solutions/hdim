package com.healthdata.caregap.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("CareGapEntity")
@Tag("unit")
class CareGapEntityTest {

    @Test
    @DisplayName("Should set defaults on create")
    void shouldSetDefaultsOnCreate() {
        CareGapEntity entity = new CareGapEntity();

        ReflectionTestUtils.invokeMethod(entity, "onCreate");

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getIdentifiedDate()).isNotNull();
        assertThat(entity.getGapStatus()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("Should update updatedAt on update")
    void shouldUpdateOnUpdate() {
        CareGapEntity entity = new CareGapEntity();
        entity.setUpdatedAt(Instant.parse("2025-01-01T00:00:00Z"));

        ReflectionTestUtils.invokeMethod(entity, "onUpdate");

        assertThat(entity.getUpdatedAt()).isAfter(Instant.parse("2025-01-01T00:00:00Z"));
    }
}
