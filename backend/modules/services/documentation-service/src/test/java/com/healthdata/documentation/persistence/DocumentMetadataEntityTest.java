package com.healthdata.documentation.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Document Metadata Entity Tests")
class DocumentMetadataEntityTest {

    @Test
    @DisplayName("Should set defaults on create")
    void shouldSetDefaultsOnCreate() {
        DocumentMetadataEntity entity = new DocumentMetadataEntity();

        ReflectionTestUtils.invokeMethod(entity, "onCreate");

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getCreatedDate()).isNotNull();
    }

    @Test
    @DisplayName("Should update updatedAt on update")
    void shouldUpdateUpdatedAt() {
        DocumentMetadataEntity entity = new DocumentMetadataEntity();
        LocalDateTime before = LocalDateTime.now().minusDays(1);
        entity.setUpdatedAt(before);

        ReflectionTestUtils.invokeMethod(entity, "onUpdate");

        assertThat(entity.getUpdatedAt()).isAfter(before);
    }

    @Test
    @DisplayName("Should keep initializer defaults for fields")
    void shouldKeepInitializerDefaults() {
        DocumentMetadataEntity entity = new DocumentMetadataEntity();

        assertThat(entity.getRelatedDocuments()).isEmpty();
        assertThat(entity.getHasVideo()).isFalse();
        assertThat(entity.getViewCount()).isZero();
        assertThat(entity.getFeedbackCount()).isZero();
        assertThat(entity.getTenantId()).isEqualTo("default");
    }

    @Test
    @DisplayName("Should report status helpers correctly")
    void shouldReportStatusHelpers() {
        DocumentMetadataEntity entity = new DocumentMetadataEntity();
        entity.setStatus("published");

        assertThat(entity.isPublished()).isTrue();
        assertThat(entity.isDraft()).isFalse();
        assertThat(entity.isArchived()).isFalse();
    }

    @Test
    @DisplayName("Should determine when review is needed")
    void shouldDetermineNeedsReview() {
        DocumentMetadataEntity entity = new DocumentMetadataEntity();
        entity.setNextReviewDate(LocalDate.now().minusDays(1));

        assertThat(entity.needsReview()).isTrue();

        entity.setNextReviewDate(LocalDate.now().plusDays(1));
        assertThat(entity.needsReview()).isFalse();
    }
}
