package com.healthdata.consent.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("ConsentHistoryEntity")
class ConsentHistoryEntityTest {

    @Test
    @DisplayName("Should set defaults on create when missing")
    void shouldSetDefaultsOnCreate() {
        ConsentHistoryEntity entity = new ConsentHistoryEntity();

        ReflectionTestUtils.invokeMethod(entity, "onCreate");

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getChangedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should not override existing values on create")
    void shouldNotOverrideExistingValues() {
        UUID id = UUID.randomUUID();
        Instant timestamp = Instant.parse("2025-01-01T00:00:00Z");
        ConsentHistoryEntity entity = new ConsentHistoryEntity();
        entity.setId(id);
        entity.setChangedAt(timestamp);

        ReflectionTestUtils.invokeMethod(entity, "onCreate");

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getChangedAt()).isEqualTo(timestamp);
    }
}
