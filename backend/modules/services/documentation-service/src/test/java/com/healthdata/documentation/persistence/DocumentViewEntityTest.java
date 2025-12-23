package com.healthdata.documentation.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Document View Entity Tests")
class DocumentViewEntityTest {

    @Test
    @DisplayName("Should set viewedAt on create when missing")
    void shouldSetViewedAtOnCreate() {
        DocumentViewEntity entity = new DocumentViewEntity();

        ReflectionTestUtils.invokeMethod(entity, "onCreate");

        assertThat(entity.getViewedAt()).isNotNull();
    }
}
