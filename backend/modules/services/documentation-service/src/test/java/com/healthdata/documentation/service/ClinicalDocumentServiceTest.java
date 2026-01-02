package com.healthdata.documentation.service;

import com.healthdata.documentation.dto.ClinicalDocumentDto;
import com.healthdata.documentation.dto.DocumentAttachmentDto;
import com.healthdata.documentation.persistence.ClinicalDocumentEntity;
import com.healthdata.documentation.persistence.DocumentAttachmentEntity;
import com.healthdata.documentation.repository.ClinicalDocumentRepository;
import com.healthdata.documentation.repository.DocumentAttachmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ClinicalDocumentService.
 * Tests clinical document and attachment CRUD operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Clinical Document Service Tests")
class ClinicalDocumentServiceTest {

    @Mock
    private ClinicalDocumentRepository documentRepository;

    @Mock
    private DocumentAttachmentRepository attachmentRepository;

    private ClinicalDocumentService service;

    private static final String TENANT_ID = "tenant-123";
    private static final String PATIENT_ID = "patient-456";
    private static final UUID DOCUMENT_ID = UUID.randomUUID();
    private static final UUID ATTACHMENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ClinicalDocumentService(documentRepository, attachmentRepository);
    }

    @Nested
    @DisplayName("Get Documents Tests")
    class GetDocumentsTests {

        @Test
        @DisplayName("Should return all documents for tenant")
        void shouldReturnAllDocuments() {
            // Given
            List<ClinicalDocumentEntity> entities = List.of(
                    createDocument("Discharge Summary", "discharge-summary"),
                    createDocument("Progress Note", "progress-note")
            );
            when(documentRepository.findByTenantId(TENANT_ID)).thenReturn(entities);

            // When
            List<ClinicalDocumentDto> result = service.getDocuments(TENANT_ID);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getTitle()).isEqualTo("Discharge Summary");
        }

        @Test
        @DisplayName("Should return empty list when no documents exist")
        void shouldReturnEmptyList() {
            // Given
            when(documentRepository.findByTenantId(TENANT_ID)).thenReturn(List.of());

            // When
            List<ClinicalDocumentDto> result = service.getDocuments(TENANT_ID);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return paginated documents")
        void shouldReturnPaginatedDocuments() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<ClinicalDocumentEntity> entities = List.of(createDocument("Doc 1", "type-1"));
            Page<ClinicalDocumentEntity> page = new PageImpl<>(entities, pageable, 1);
            when(documentRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);

            // When
            Page<ClinicalDocumentDto> result = service.getDocumentsPaginated(TENANT_ID, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Get Single Document Tests")
    class GetSingleDocumentTests {

        @Test
        @DisplayName("Should return document by ID with attachments")
        void shouldReturnDocumentById() {
            // Given
            ClinicalDocumentEntity entity = createDocument("Lab Report", "lab-report");
            List<DocumentAttachmentEntity> attachments = List.of(
                    createAttachment("report.pdf", "application/pdf")
            );

            when(documentRepository.findByIdAndTenantId(DOCUMENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(entity));
            when(attachmentRepository.findByClinicalDocumentIdAndTenantId(DOCUMENT_ID, TENANT_ID))
                    .thenReturn(attachments);

            // When
            Optional<ClinicalDocumentDto> result = service.getDocument(DOCUMENT_ID, TENANT_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("Lab Report");
            assertThat(result.get().getAttachments()).hasSize(1);
        }

        @Test
        @DisplayName("Should return empty when document not found")
        void shouldReturnEmptyWhenNotFound() {
            // Given
            when(documentRepository.findByIdAndTenantId(DOCUMENT_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            // When
            Optional<ClinicalDocumentDto> result = service.getDocument(DOCUMENT_ID, TENANT_ID);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Patient Documents Tests")
    class GetPatientDocumentsTests {

        @Test
        @DisplayName("Should return all documents for patient")
        void shouldReturnPatientDocuments() {
            // Given
            List<ClinicalDocumentEntity> entities = List.of(
                    createDocument("Visit Summary", "visit-summary"),
                    createDocument("Lab Results", "lab-results")
            );
            when(documentRepository.findByTenantIdAndPatientId(TENANT_ID, PATIENT_ID))
                    .thenReturn(entities);

            // When
            List<ClinicalDocumentDto> result = service.getPatientDocuments(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should return paginated patient documents")
        void shouldReturnPaginatedPatientDocuments() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<ClinicalDocumentEntity> entities = List.of(createDocument("Doc 1", "type-1"));
            Page<ClinicalDocumentEntity> page = new PageImpl<>(entities, pageable, 1);
            when(documentRepository.findByTenantIdAndPatientId(TENANT_ID, PATIENT_ID, pageable))
                    .thenReturn(page);

            // When
            Page<ClinicalDocumentDto> result = service.getPatientDocumentsPaginated(TENANT_ID, PATIENT_ID, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Search Documents Tests")
    class SearchDocumentsTests {

        @Test
        @DisplayName("Should search documents by query")
        void shouldSearchDocuments() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<ClinicalDocumentEntity> entities = List.of(
                    createDocument("Discharge Summary", "discharge-summary")
            );
            Page<ClinicalDocumentEntity> page = new PageImpl<>(entities, pageable, 1);
            when(documentRepository.searchDocuments(TENANT_ID, "discharge", pageable))
                    .thenReturn(page);

            // When
            Page<ClinicalDocumentDto> result = service.searchDocuments(TENANT_ID, "discharge", pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).contains("Discharge");
        }
    }

    @Nested
    @DisplayName("Create Document Tests")
    class CreateDocumentTests {

        @Test
        @DisplayName("Should create document successfully")
        void shouldCreateDocument() {
            // Given
            ClinicalDocumentDto dto = ClinicalDocumentDto.builder()
                    .patientId(PATIENT_ID)
                    .documentType("progress-note")
                    .title("Progress Note")
                    .description("Patient progress update")
                    .authorName("Dr. Smith")
                    .build();

            ClinicalDocumentEntity savedEntity = createDocument("Progress Note", "progress-note");
            when(documentRepository.save(any(ClinicalDocumentEntity.class))).thenReturn(savedEntity);

            // When
            ClinicalDocumentDto result = service.createDocument(dto, TENANT_ID);

            // Then
            assertThat(result.getTitle()).isEqualTo("Progress Note");
            verify(documentRepository).save(any(ClinicalDocumentEntity.class));
        }

        @Test
        @DisplayName("Should set default status when not provided")
        void shouldSetDefaultStatus() {
            // Given
            ClinicalDocumentDto dto = ClinicalDocumentDto.builder()
                    .patientId(PATIENT_ID)
                    .documentType("lab-result")
                    .title("Lab Results")
                    .build();

            ArgumentCaptor<ClinicalDocumentEntity> captor = ArgumentCaptor.forClass(ClinicalDocumentEntity.class);
            when(documentRepository.save(captor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.createDocument(dto, TENANT_ID);

            // Then
            assertThat(captor.getValue().getStatus()).isEqualTo("current");
        }

        @Test
        @DisplayName("Should set tenant ID correctly")
        void shouldSetTenantId() {
            // Given
            ClinicalDocumentDto dto = ClinicalDocumentDto.builder()
                    .patientId(PATIENT_ID)
                    .documentType("note")
                    .title("Note")
                    .build();

            ArgumentCaptor<ClinicalDocumentEntity> captor = ArgumentCaptor.forClass(ClinicalDocumentEntity.class);
            when(documentRepository.save(captor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.createDocument(dto, TENANT_ID);

            // Then
            assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT_ID);
        }
    }

    @Nested
    @DisplayName("Update Document Tests")
    class UpdateDocumentTests {

        @Test
        @DisplayName("Should update document successfully")
        void shouldUpdateDocument() {
            // Given
            ClinicalDocumentEntity existing = createDocument("Old Title", "old-type");
            ClinicalDocumentDto dto = ClinicalDocumentDto.builder()
                    .title("Updated Title")
                    .documentType("new-type")
                    .status("superseded")
                    .description("Updated description")
                    .build();

            when(documentRepository.findByIdAndTenantId(DOCUMENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(documentRepository.save(any(ClinicalDocumentEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            Optional<ClinicalDocumentDto> result = service.updateDocument(DOCUMENT_ID, dto, TENANT_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("Updated Title");
        }

        @Test
        @DisplayName("Should return empty when updating non-existent document")
        void shouldReturnEmptyWhenUpdatingNonExistent() {
            // Given
            when(documentRepository.findByIdAndTenantId(DOCUMENT_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            // When
            Optional<ClinicalDocumentDto> result = service.updateDocument(DOCUMENT_ID, new ClinicalDocumentDto(), TENANT_ID);

            // Then
            assertThat(result).isEmpty();
            verify(documentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Document Tests")
    class DeleteDocumentTests {

        @Test
        @DisplayName("Should delete document and attachments")
        void shouldDeleteDocumentAndAttachments() {
            // Given
            when(documentRepository.existsByIdAndTenantId(DOCUMENT_ID, TENANT_ID)).thenReturn(true);

            // When
            boolean result = service.deleteDocument(DOCUMENT_ID, TENANT_ID);

            // Then
            assertThat(result).isTrue();
            verify(attachmentRepository).deleteByClinicalDocumentIdAndTenantId(DOCUMENT_ID, TENANT_ID);
            verify(documentRepository).deleteById(DOCUMENT_ID);
        }

        @Test
        @DisplayName("Should return false when document not found")
        void shouldReturnFalseWhenNotFound() {
            // Given
            when(documentRepository.existsByIdAndTenantId(DOCUMENT_ID, TENANT_ID)).thenReturn(false);

            // When
            boolean result = service.deleteDocument(DOCUMENT_ID, TENANT_ID);

            // Then
            assertThat(result).isFalse();
            verify(documentRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Attachment Management Tests")
    class AttachmentManagementTests {

        @Test
        @DisplayName("Should add attachment to document")
        void shouldAddAttachment() {
            // Given
            when(documentRepository.existsByIdAndTenantId(DOCUMENT_ID, TENANT_ID)).thenReturn(true);

            DocumentAttachmentDto dto = DocumentAttachmentDto.builder()
                    .contentType("application/pdf")
                    .fileName("report.pdf")
                    .fileSize(1024L)
                    .storagePath("/docs/report.pdf")
                    .build();

            DocumentAttachmentEntity savedEntity = createAttachment("report.pdf", "application/pdf");
            when(attachmentRepository.save(any(DocumentAttachmentEntity.class))).thenReturn(savedEntity);

            // When
            DocumentAttachmentDto result = service.addAttachment(DOCUMENT_ID, dto, TENANT_ID);

            // Then
            assertThat(result.getFileName()).isEqualTo("report.pdf");
            verify(attachmentRepository).save(any(DocumentAttachmentEntity.class));
        }

        @Test
        @DisplayName("Should throw exception when adding attachment to non-existent document")
        void shouldThrowExceptionWhenDocumentNotFound() {
            // Given
            when(documentRepository.existsByIdAndTenantId(DOCUMENT_ID, TENANT_ID)).thenReturn(false);
            DocumentAttachmentDto dto = DocumentAttachmentDto.builder()
                    .contentType("application/pdf")
                    .fileName("report.pdf")
                    .build();

            // When/Then
            assertThatThrownBy(() -> service.addAttachment(DOCUMENT_ID, dto, TENANT_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Document not found");
        }

        @Test
        @DisplayName("Should get attachment by ID")
        void shouldGetAttachment() {
            // Given
            DocumentAttachmentEntity entity = createAttachment("file.pdf", "application/pdf");
            when(attachmentRepository.findByIdAndTenantId(ATTACHMENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(entity));

            // When
            Optional<DocumentAttachmentDto> result = service.getAttachment(ATTACHMENT_ID, TENANT_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getFileName()).isEqualTo("file.pdf");
        }

        @Test
        @DisplayName("Should return empty when attachment not found")
        void shouldReturnEmptyWhenAttachmentNotFound() {
            // Given
            when(attachmentRepository.findByIdAndTenantId(ATTACHMENT_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            // When
            Optional<DocumentAttachmentDto> result = service.getAttachment(ATTACHMENT_ID, TENANT_ID);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should delete attachment successfully")
        void shouldDeleteAttachment() {
            // Given
            when(attachmentRepository.existsByIdAndTenantId(ATTACHMENT_ID, TENANT_ID)).thenReturn(true);

            // When
            boolean result = service.deleteAttachment(ATTACHMENT_ID, TENANT_ID);

            // Then
            assertThat(result).isTrue();
            verify(attachmentRepository).deleteById(ATTACHMENT_ID);
        }

        @Test
        @DisplayName("Should return false when deleting non-existent attachment")
        void shouldReturnFalseWhenDeletingNonExistent() {
            // Given
            when(attachmentRepository.existsByIdAndTenantId(ATTACHMENT_ID, TENANT_ID)).thenReturn(false);

            // When
            boolean result = service.deleteAttachment(ATTACHMENT_ID, TENANT_ID);

            // Then
            assertThat(result).isFalse();
            verify(attachmentRepository, never()).deleteById(any());
        }
    }

    // ==================== Helper Methods ====================

    private ClinicalDocumentEntity createDocument(String title, String type) {
        return ClinicalDocumentEntity.builder()
                .id(DOCUMENT_ID)
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .documentType(type)
                .status("current")
                .title(title)
                .authorName("Dr. Test")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private DocumentAttachmentEntity createAttachment(String fileName, String contentType) {
        return DocumentAttachmentEntity.builder()
                .id(ATTACHMENT_ID)
                .clinicalDocumentId(DOCUMENT_ID)
                .tenantId(TENANT_ID)
                .fileName(fileName)
                .contentType(contentType)
                .fileSize(1024L)
                .storagePath("/docs/" + fileName)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
