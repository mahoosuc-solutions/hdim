package com.healthdata.consent.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("ConsentPolicyEntity")
class ConsentPolicyEntityTest {

    @Test
    @DisplayName("Should set defaults on create")
    void shouldSetDefaultsOnCreate() {
        ConsentPolicyEntity entity = new ConsentPolicyEntity();

        ReflectionTestUtils.invokeMethod(entity, "onCreate");

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should update updatedAt on update")
    void shouldUpdateUpdatedAt() {
        ConsentPolicyEntity entity = new ConsentPolicyEntity();
        Instant before = Instant.parse("2025-01-01T00:00:00Z");
        entity.setUpdatedAt(before);

        ReflectionTestUtils.invokeMethod(entity, "onUpdate");

        assertThat(entity.getUpdatedAt()).isAfter(before);
    }
}
