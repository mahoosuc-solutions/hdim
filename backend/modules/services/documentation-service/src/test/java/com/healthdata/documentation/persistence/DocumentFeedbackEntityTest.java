package com.healthdata.documentation.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Document Feedback Entity Tests")
class DocumentFeedbackEntityTest {

    @Test
    @DisplayName("Should set defaults on create")
    void shouldSetDefaultsOnCreate() {
        DocumentFeedbackEntity entity = new DocumentFeedbackEntity();

        ReflectionTestUtils.invokeMethod(entity, "onCreate");

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should evaluate rating helpers")
    void shouldEvaluateRatingHelpers() {
        DocumentFeedbackEntity positive = new DocumentFeedbackEntity();
        positive.setRating(5);
        DocumentFeedbackEntity negative = new DocumentFeedbackEntity();
        negative.setRating(2);
        DocumentFeedbackEntity neutral = new DocumentFeedbackEntity();

        assertThat(positive.isPositive()).isTrue();
        assertThat(positive.isNegative()).isFalse();
        assertThat(negative.isNegative()).isTrue();
        assertThat(negative.isPositive()).isFalse();
        assertThat(neutral.isPositive()).isFalse();
        assertThat(neutral.isNegative()).isFalse();
    }
}
