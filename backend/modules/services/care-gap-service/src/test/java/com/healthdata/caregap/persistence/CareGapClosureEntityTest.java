package com.healthdata.caregap.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("CareGapClosureEntity")
class CareGapClosureEntityTest {

    @Test
    @DisplayName("Should set defaults on create")
    void shouldSetDefaultsOnCreate() {
        CareGapClosureEntity entity = new CareGapClosureEntity();

        ReflectionTestUtils.invokeMethod(entity, "onCreate");

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getClosureDate()).isNotNull();
    }

    @Test
    @DisplayName("Should not override existing values")
    void shouldNotOverrideExistingValues() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2025-01-01T00:00:00Z");
        Instant closureDate = Instant.parse("2025-01-02T00:00:00Z");
        CareGapClosureEntity entity = new CareGapClosureEntity();
        entity.setId(id);
        entity.setCreatedAt(createdAt);
        entity.setClosureDate(closureDate);

        ReflectionTestUtils.invokeMethod(entity, "onCreate");

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getClosureDate()).isEqualTo(closureDate);
    }
}
