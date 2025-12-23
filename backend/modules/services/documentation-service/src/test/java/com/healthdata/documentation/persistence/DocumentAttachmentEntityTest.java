package com.healthdata.documentation.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Document Attachment Entity Tests")
class DocumentAttachmentEntityTest {

    @Test
    @DisplayName("Should set defaults on create")
    void shouldSetDefaultsOnCreate() {
        DocumentAttachmentEntity entity = new DocumentAttachmentEntity();

        ReflectionTestUtils.invokeMethod(entity, "onCreate");

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should detect PDF and image types")
    void shouldDetectContentTypes() {
        DocumentAttachmentEntity pdf = new DocumentAttachmentEntity();
        pdf.setContentType("application/pdf");
        DocumentAttachmentEntity image = new DocumentAttachmentEntity();
        image.setContentType("image/png");

        assertThat(pdf.isPdf()).isTrue();
        assertThat(pdf.isImage()).isFalse();
        assertThat(image.isPdf()).isFalse();
        assertThat(image.isImage()).isTrue();
    }
}
