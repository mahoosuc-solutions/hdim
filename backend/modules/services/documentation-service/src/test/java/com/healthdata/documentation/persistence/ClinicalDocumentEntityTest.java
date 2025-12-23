package com.healthdata.documentation.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Clinical Document Entity Tests")
class ClinicalDocumentEntityTest {

    @Test
    @DisplayName("Should set defaults on create")
    void shouldSetDefaultsOnCreate() {
        ClinicalDocumentEntity entity = new ClinicalDocumentEntity();

        ReflectionTestUtils.invokeMethod(entity, "onCreate");

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getStatus()).isEqualTo("current");
    }

    @Test
    @DisplayName("Should update updatedAt on update")
    void shouldUpdateUpdatedAt() {
        ClinicalDocumentEntity entity = new ClinicalDocumentEntity();
        LocalDateTime before = LocalDateTime.now().minusDays(1);
        entity.setUpdatedAt(before);

        ReflectionTestUtils.invokeMethod(entity, "onUpdate");

        assertThat(entity.getUpdatedAt()).isAfter(before);
    }

    @Test
    @DisplayName("Should evaluate status helpers")
    void shouldEvaluateStatusHelpers() {
        ClinicalDocumentEntity current = new ClinicalDocumentEntity();
        current.setStatus("current");
        ClinicalDocumentEntity superseded = new ClinicalDocumentEntity();
        superseded.setStatus("superseded");

        assertThat(current.isCurrent()).isTrue();
        assertThat(current.isSuperseded()).isFalse();
        assertThat(superseded.isCurrent()).isFalse();
        assertThat(superseded.isSuperseded()).isTrue();
    }
}
