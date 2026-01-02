package com.healthdata.documentation.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Document Rating Entity Tests")
class DocumentRatingEntityTest {

    @Test
    @DisplayName("Should set createdAt on create when missing")
    void shouldSetCreatedAtOnCreate() {
        DocumentRatingEntity entity = new DocumentRatingEntity();

        ReflectionTestUtils.invokeMethod(entity, "onCreate");

        assertThat(entity.getCreatedAt()).isNotNull();
    }
}
