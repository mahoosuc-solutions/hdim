package com.healthdata.documentation.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CDA Document Entity Tests")
class CdaDocumentEntityTest {

    @Test
    @DisplayName("Should set defaults on create")
    void shouldSetDefaultsOnCreate() {
        CdaDocumentEntity entity = new CdaDocumentEntity();

        ReflectionTestUtils.invokeMethod(entity, "onCreate");

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getValidationStatus()).isEqualTo("NOT_VALIDATED");
    }

    @Test
    @DisplayName("Should evaluate validation helpers")
    void shouldEvaluateValidationHelpers() {
        CdaDocumentEntity valid = new CdaDocumentEntity();
        valid.setValidationStatus("VALID");
        CdaDocumentEntity warnings = new CdaDocumentEntity();
        warnings.setValidationStatus("WARNINGS");

        assertThat(valid.isValid()).isTrue();
        assertThat(valid.hasWarnings()).isFalse();
        assertThat(warnings.isValid()).isFalse();
        assertThat(warnings.hasWarnings()).isTrue();
    }
}
