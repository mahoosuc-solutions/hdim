package com.healthdata.documentation.service;

import com.healthdata.documentation.dto.DocumentFeedbackDto;
import com.healthdata.documentation.dto.DocumentVersionDto;
import com.healthdata.documentation.dto.ProductDocumentDto;
import com.healthdata.documentation.persistence.DocumentFeedbackEntity;
import com.healthdata.documentation.persistence.DocumentMetadataEntity;
import com.healthdata.documentation.persistence.DocumentVersionEntity;
import com.healthdata.documentation.repository.DocumentFeedbackRepository;
import com.healthdata.documentation.repository.DocumentMetadataRepository;
import com.healthdata.documentation.repository.DocumentVersionRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProductDocumentService.
 * Tests product documentation CRUD, versioning, and feedback operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Product Document Service Tests")
class ProductDocumentServiceTest {

    @Mock
    private DocumentMetadataRepository metadataRepository;

    @Mock
    private DocumentVersionRepository versionRepository;

    @Mock
    private DocumentFeedbackRepository feedbackRepository;

    private ProductDocumentService service;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final String DOCUMENT_ID = "getting-started";
    private static final UUID VERSION_ID = UUID.randomUUID();
    private static final UUID FEEDBACK_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ProductDocumentService(metadataRepository, versionRepository, feedbackRepository);
    }

    @Nested
    @DisplayName("Get Documents Tests")
    class GetDocumentsTests {

        @Test
        @DisplayName("Should return all documents for tenant")
        void shouldReturnAllDocuments() {
            // Given
            List<DocumentMetadataEntity> entities = List.of(
                    createDocument("getting-started", "Getting Started"),
                    createDocument("api-reference", "API Reference")
            );
            when(metadataRepository.findByTenantId(TENANT_ID)).thenReturn(entities);

            // When
            List<ProductDocumentDto> result = service.getDocuments(TENANT_ID);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getTitle()).isEqualTo("Getting Started");
        }

        @Test
        @DisplayName("Should return empty list when no documents exist")
        void shouldReturnEmptyList() {
            // Given
            when(metadataRepository.findByTenantId(TENANT_ID)).thenReturn(List.of());

            // When
            List<ProductDocumentDto> result = service.getDocuments(TENANT_ID);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return paginated documents")
        void shouldReturnPaginatedDocuments() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<DocumentMetadataEntity> entities = List.of(createDocument("doc-1", "Doc 1"));
            Page<DocumentMetadataEntity> page = new PageImpl<>(entities, pageable, 1);
            when(metadataRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);

            // When
            Page<ProductDocumentDto> result = service.getDocumentsPaginated(TENANT_ID, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return published documents only")
        void shouldReturnPublishedDocuments() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            DocumentMetadataEntity publishedDoc = createDocument("pub-doc", "Published Doc");
            publishedDoc.setStatus("published");
            Page<DocumentMetadataEntity> page = new PageImpl<>(List.of(publishedDoc), pageable, 1);
            when(metadataRepository.findPublishedDocuments(TENANT_ID, pageable)).thenReturn(page);

            // When
            Page<ProductDocumentDto> result = service.getPublishedDocuments(TENANT_ID, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Get Single Document Tests")
    class GetSingleDocumentTests {

        @Test
        @DisplayName("Should return document by ID and increment view count")
        void shouldReturnDocumentByIdAndIncrementViewCount() {
            // Given
            DocumentMetadataEntity entity = createDocument(DOCUMENT_ID, "Test Document");
            when(metadataRepository.findByIdAndTenantId(DOCUMENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(entity));

            // When
            Optional<ProductDocumentDto> result = service.getDocument(DOCUMENT_ID, TENANT_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("Test Document");
            verify(metadataRepository).incrementViewCount(DOCUMENT_ID);
        }

        @Test
        @DisplayName("Should return empty when document not found")
        void shouldReturnEmptyWhenNotFound() {
            // Given
            when(metadataRepository.findByIdAndTenantId(DOCUMENT_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            // When
            Optional<ProductDocumentDto> result = service.getDocument(DOCUMENT_ID, TENANT_ID);

            // Then
            assertThat(result).isEmpty();
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
            List<DocumentMetadataEntity> entities = List.of(
                    createDocument("api-guide", "API Guide")
            );
            Page<DocumentMetadataEntity> page = new PageImpl<>(entities, pageable, 1);
            when(metadataRepository.searchDocuments(TENANT_ID, "API", pageable)).thenReturn(page);

            // When
            Page<ProductDocumentDto> result = service.searchDocuments(TENANT_ID, "API", pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).contains("API");
        }
    }

    @Nested
    @DisplayName("Get Categories Tests")
    class GetCategoriesTests {

        @Test
        @DisplayName("Should return distinct categories")
        void shouldReturnCategories() {
            // Given
            when(metadataRepository.findDistinctCategories(TENANT_ID))
                    .thenReturn(List.of("Guides", "Reference", "Tutorials"));

            // When
            List<String> result = service.getCategories(TENANT_ID);

            // Then
            assertThat(result).containsExactly("Guides", "Reference", "Tutorials");
        }
    }

    @Nested
    @DisplayName("Create Document Tests")
    class CreateDocumentTests {

        @Test
        @DisplayName("Should create document successfully")
        void shouldCreateDocument() {
            // Given
            ProductDocumentDto dto = ProductDocumentDto.builder()
                    .title("New Guide")
                    .category("Guides")
                    .summary("A new guide")
                    .owner("admin")
                    .accessLevel("public")
                    .reviewCycle("quarterly")
                    .targetAudience(new String[]{"developers"})
                    .tags(new String[]{"api", "guide"})
                    .build();

            DocumentMetadataEntity savedEntity = createDocument("new-guide", "New Guide");
            when(metadataRepository.save(any(DocumentMetadataEntity.class))).thenReturn(savedEntity);

            // When
            ProductDocumentDto result = service.createDocument(dto, TENANT_ID);

            // Then
            assertThat(result.getTitle()).isEqualTo("New Guide");
            verify(metadataRepository).save(any(DocumentMetadataEntity.class));
        }

        @Test
        @DisplayName("Should generate document ID from title")
        void shouldGenerateDocumentId() {
            // Given
            ProductDocumentDto dto = ProductDocumentDto.builder()
                    .title("Getting Started Guide")
                    .category("Guides")
                    .summary("Start here")
                    .owner("admin")
                    .accessLevel("public")
                    .reviewCycle("monthly")
                    .targetAudience(new String[]{"beginners"})
                    .tags(new String[]{"getting-started"})
                    .build();

            ArgumentCaptor<DocumentMetadataEntity> captor = ArgumentCaptor.forClass(DocumentMetadataEntity.class);
            when(metadataRepository.save(captor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.createDocument(dto, TENANT_ID);

            // Then
            assertThat(captor.getValue().getId()).isEqualTo("getting-started-guide");
        }

        @Test
        @DisplayName("Should set default values")
        void shouldSetDefaultValues() {
            // Given
            ProductDocumentDto dto = ProductDocumentDto.builder()
                    .title("Test Doc")
                    .category("Test")
                    .summary("Test summary")
                    .owner("admin")
                    .accessLevel("public")
                    .reviewCycle("monthly")
                    .targetAudience(new String[]{"all"})
                    .tags(new String[]{"test"})
                    .build();

            ArgumentCaptor<DocumentMetadataEntity> captor = ArgumentCaptor.forClass(DocumentMetadataEntity.class);
            when(metadataRepository.save(captor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.createDocument(dto, TENANT_ID);

            // Then
            DocumentMetadataEntity saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo("draft");
            assertThat(saved.getVersion()).isEqualTo("1.0");
            assertThat(saved.getHasVideo()).isFalse();
        }

        @Test
        @DisplayName("Should use provided ID if specified")
        void shouldUseProvidedId() {
            // Given
            ProductDocumentDto dto = ProductDocumentDto.builder()
                    .id("custom-id")
                    .title("Custom Doc")
                    .category("Test")
                    .summary("Test")
                    .owner("admin")
                    .accessLevel("public")
                    .reviewCycle("monthly")
                    .targetAudience(new String[]{"all"})
                    .tags(new String[]{"test"})
                    .build();

            ArgumentCaptor<DocumentMetadataEntity> captor = ArgumentCaptor.forClass(DocumentMetadataEntity.class);
            when(metadataRepository.save(captor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.createDocument(dto, TENANT_ID);

            // Then
            assertThat(captor.getValue().getId()).isEqualTo("custom-id");
        }
    }

    @Nested
    @DisplayName("Update Document Tests")
    class UpdateDocumentTests {

        @Test
        @DisplayName("Should update document successfully")
        void shouldUpdateDocument() {
            // Given
            DocumentMetadataEntity existing = createDocument(DOCUMENT_ID, "Old Title");
            ProductDocumentDto dto = ProductDocumentDto.builder()
                    .title("Updated Title")
                    .category("Updated Category")
                    .summary("Updated summary")
                    .build();

            when(metadataRepository.findByIdAndTenantId(DOCUMENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(metadataRepository.save(any(DocumentMetadataEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            Optional<ProductDocumentDto> result = service.updateDocument(DOCUMENT_ID, dto, TENANT_ID, USER_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("Updated Title");
        }

        @Test
        @DisplayName("Should return empty when updating non-existent document")
        void shouldReturnEmptyWhenUpdatingNonExistent() {
            // Given
            when(metadataRepository.findByIdAndTenantId(DOCUMENT_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            // When
            Optional<ProductDocumentDto> result = service.updateDocument(DOCUMENT_ID, new ProductDocumentDto(), TENANT_ID, USER_ID);

            // Then
            assertThat(result).isEmpty();
            verify(metadataRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should update version when provided")
        void shouldUpdateVersionWhenProvided() {
            // Given
            DocumentMetadataEntity existing = createDocument(DOCUMENT_ID, "Doc");
            existing.setVersion("1.0");

            ProductDocumentDto dto = ProductDocumentDto.builder()
                    .title("Doc")
                    .version("2.0")
                    .build();

            when(metadataRepository.findByIdAndTenantId(DOCUMENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));

            ArgumentCaptor<DocumentMetadataEntity> captor = ArgumentCaptor.forClass(DocumentMetadataEntity.class);
            when(metadataRepository.save(captor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.updateDocument(DOCUMENT_ID, dto, TENANT_ID, USER_ID);

            // Then
            assertThat(captor.getValue().getVersion()).isEqualTo("2.0");
        }
    }

    @Nested
    @DisplayName("Publish Document Tests")
    class PublishDocumentTests {

        @Test
        @DisplayName("Should publish document successfully")
        void shouldPublishDocument() {
            // Given
            DocumentMetadataEntity existing = createDocument(DOCUMENT_ID, "Draft Doc");
            existing.setStatus("draft");

            when(metadataRepository.findByIdAndTenantId(DOCUMENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));

            ArgumentCaptor<DocumentMetadataEntity> captor = ArgumentCaptor.forClass(DocumentMetadataEntity.class);
            when(metadataRepository.save(captor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            Optional<ProductDocumentDto> result = service.publishDocument(DOCUMENT_ID, TENANT_ID, USER_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(captor.getValue().getStatus()).isEqualTo("published");
        }

        @Test
        @DisplayName("Should return empty when publishing non-existent document")
        void shouldReturnEmptyWhenPublishingNonExistent() {
            // Given
            when(metadataRepository.findByIdAndTenantId(DOCUMENT_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            // When
            Optional<ProductDocumentDto> result = service.publishDocument(DOCUMENT_ID, TENANT_ID, USER_ID);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Archive Document Tests")
    class ArchiveDocumentTests {

        @Test
        @DisplayName("Should archive document successfully")
        void shouldArchiveDocument() {
            // Given
            DocumentMetadataEntity existing = createDocument(DOCUMENT_ID, "Published Doc");
            existing.setStatus("published");

            when(metadataRepository.findByIdAndTenantId(DOCUMENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));

            ArgumentCaptor<DocumentMetadataEntity> captor = ArgumentCaptor.forClass(DocumentMetadataEntity.class);
            when(metadataRepository.save(captor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            Optional<ProductDocumentDto> result = service.archiveDocument(DOCUMENT_ID, TENANT_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(captor.getValue().getStatus()).isEqualTo("archived");
        }
    }

    @Nested
    @DisplayName("Delete Document Tests")
    class DeleteDocumentTests {

        @Test
        @DisplayName("Should delete document successfully")
        void shouldDeleteDocument() {
            // Given
            when(metadataRepository.existsByIdAndTenantId(DOCUMENT_ID, TENANT_ID)).thenReturn(true);

            // When
            boolean result = service.deleteDocument(DOCUMENT_ID, TENANT_ID);

            // Then
            assertThat(result).isTrue();
            verify(metadataRepository).deleteById(DOCUMENT_ID);
        }

        @Test
        @DisplayName("Should return false when document not found")
        void shouldReturnFalseWhenNotFound() {
            // Given
            when(metadataRepository.existsByIdAndTenantId(DOCUMENT_ID, TENANT_ID)).thenReturn(false);

            // When
            boolean result = service.deleteDocument(DOCUMENT_ID, TENANT_ID);

            // Then
            assertThat(result).isFalse();
            verify(metadataRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Version Management Tests")
    class VersionManagementTests {

        @Test
        @DisplayName("Should get all versions for document")
        void shouldGetVersions() {
            // Given
            List<DocumentVersionEntity> versions = List.of(
                    createVersion("2.0", true),
                    createVersion("1.0", false)
            );
            when(versionRepository.findVersionsOrderByCreatedAtDesc(DOCUMENT_ID, TENANT_ID))
                    .thenReturn(versions);

            // When
            List<DocumentVersionDto> result = service.getVersions(DOCUMENT_ID, TENANT_ID);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getVersionNumber()).isEqualTo("2.0");
        }

        @Test
        @DisplayName("Should create new version")
        void shouldCreateVersion() {
            // Given
            DocumentVersionDto dto = DocumentVersionDto.builder()
                    .versionNumber("2.0")
                    .content("Updated content")
                    .changeSummary("Major update")
                    .isMajorVersion(true)
                    .build();

            DocumentVersionEntity savedEntity = createVersion("2.0", true);
            when(versionRepository.save(any(DocumentVersionEntity.class))).thenReturn(savedEntity);

            // When
            DocumentVersionDto result = service.createVersion(DOCUMENT_ID, dto, TENANT_ID, USER_ID);

            // Then
            assertThat(result.getVersionNumber()).isEqualTo("2.0");
            verify(versionRepository).save(any(DocumentVersionEntity.class));
        }

        @Test
        @DisplayName("Should set changedBy to current user")
        void shouldSetChangedBy() {
            // Given
            DocumentVersionDto dto = DocumentVersionDto.builder()
                    .versionNumber("1.1")
                    .content("Content")
                    .build();

            ArgumentCaptor<DocumentVersionEntity> captor = ArgumentCaptor.forClass(DocumentVersionEntity.class);
            when(versionRepository.save(captor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.createVersion(DOCUMENT_ID, dto, TENANT_ID, USER_ID);

            // Then
            assertThat(captor.getValue().getChangedBy()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("Should set default values for version")
        void shouldSetVersionDefaults() {
            // Given
            DocumentVersionDto dto = DocumentVersionDto.builder()
                    .versionNumber("1.0")
                    .content("Content")
                    .build();

            ArgumentCaptor<DocumentVersionEntity> captor = ArgumentCaptor.forClass(DocumentVersionEntity.class);
            when(versionRepository.save(captor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.createVersion(DOCUMENT_ID, dto, TENANT_ID, USER_ID);

            // Then
            DocumentVersionEntity saved = captor.getValue();
            assertThat(saved.getIsMajorVersion()).isFalse();
            assertThat(saved.getIsPublished()).isFalse();
        }
    }

    @Nested
    @DisplayName("Feedback Management Tests")
    class FeedbackManagementTests {

        @Test
        @DisplayName("Should submit new feedback")
        void shouldSubmitNewFeedback() {
            // Given
            DocumentFeedbackDto dto = DocumentFeedbackDto.builder()
                    .rating(5)
                    .comment("Great documentation!")
                    .helpful(true)
                    .build();

            when(feedbackRepository.findByDocumentIdAndUserId(DOCUMENT_ID, USER_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            DocumentFeedbackEntity savedEntity = createFeedback(5, "Great documentation!");
            when(feedbackRepository.save(any(DocumentFeedbackEntity.class))).thenReturn(savedEntity);
            when(feedbackRepository.calculateAverageRating(DOCUMENT_ID)).thenReturn(5.0);
            when(feedbackRepository.countByDocumentId(DOCUMENT_ID)).thenReturn(1L);

            // When
            DocumentFeedbackDto result = service.submitFeedback(DOCUMENT_ID, dto, TENANT_ID, USER_ID);

            // Then
            assertThat(result.getRating()).isEqualTo(5);
            verify(feedbackRepository).save(any(DocumentFeedbackEntity.class));
        }

        @Test
        @DisplayName("Should update existing feedback")
        void shouldUpdateExistingFeedback() {
            // Given
            DocumentFeedbackEntity existingFeedback = createFeedback(3, "Okay");
            DocumentFeedbackDto dto = DocumentFeedbackDto.builder()
                    .rating(5)
                    .comment("Actually great!")
                    .helpful(true)
                    .build();

            when(feedbackRepository.findByDocumentIdAndUserId(DOCUMENT_ID, USER_ID, TENANT_ID))
                    .thenReturn(Optional.of(existingFeedback));
            when(feedbackRepository.save(any(DocumentFeedbackEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(feedbackRepository.calculateAverageRating(DOCUMENT_ID)).thenReturn(5.0);
            when(feedbackRepository.countByDocumentId(DOCUMENT_ID)).thenReturn(1L);

            // When
            DocumentFeedbackDto result = service.submitFeedback(DOCUMENT_ID, dto, TENANT_ID, USER_ID);

            // Then
            assertThat(result.getRating()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should set default feedback type")
        void shouldSetDefaultFeedbackType() {
            // Given
            DocumentFeedbackDto dto = DocumentFeedbackDto.builder()
                    .rating(4)
                    .comment("Nice")
                    .build();

            when(feedbackRepository.findByDocumentIdAndUserId(DOCUMENT_ID, USER_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            ArgumentCaptor<DocumentFeedbackEntity> captor = ArgumentCaptor.forClass(DocumentFeedbackEntity.class);
            when(feedbackRepository.save(captor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(feedbackRepository.calculateAverageRating(DOCUMENT_ID)).thenReturn(4.0);
            when(feedbackRepository.countByDocumentId(DOCUMENT_ID)).thenReturn(1L);

            // When
            service.submitFeedback(DOCUMENT_ID, dto, TENANT_ID, USER_ID);

            // Then
            assertThat(captor.getValue().getFeedbackType()).isEqualTo("GENERAL");
        }

        @Test
        @DisplayName("Should update document rating after feedback")
        void shouldUpdateDocumentRating() {
            // Given
            DocumentFeedbackDto dto = DocumentFeedbackDto.builder()
                    .rating(5)
                    .comment("Great!")
                    .build();

            DocumentMetadataEntity document = createDocument(DOCUMENT_ID, "Test Doc");

            when(feedbackRepository.findByDocumentIdAndUserId(DOCUMENT_ID, USER_ID, TENANT_ID))
                    .thenReturn(Optional.empty());
            when(feedbackRepository.save(any(DocumentFeedbackEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(feedbackRepository.calculateAverageRating(DOCUMENT_ID)).thenReturn(4.5);
            when(feedbackRepository.countByDocumentId(DOCUMENT_ID)).thenReturn(2L);
            when(metadataRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
            when(metadataRepository.save(any(DocumentMetadataEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.submitFeedback(DOCUMENT_ID, dto, TENANT_ID, USER_ID);

            // Then
            verify(metadataRepository).save(argThat(entity ->
                    entity.getAvgRating() != null &&
                            entity.getAvgRating().compareTo(BigDecimal.valueOf(4.5)) == 0 &&
                            entity.getFeedbackCount() == 2
            ));
        }

        @Test
        @DisplayName("Should get paginated feedback")
        void shouldGetPaginatedFeedback() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<DocumentFeedbackEntity> feedbacks = List.of(createFeedback(5, "Great!"));
            Page<DocumentFeedbackEntity> page = new PageImpl<>(feedbacks, pageable, 1);
            when(feedbackRepository.findByDocumentIdAndTenantId(DOCUMENT_ID, TENANT_ID, pageable))
                    .thenReturn(page);

            // When
            Page<DocumentFeedbackDto> result = service.getFeedback(DOCUMENT_ID, TENANT_ID, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getRating()).isEqualTo(5);
        }
    }

    // ==================== Helper Methods ====================

    private DocumentMetadataEntity createDocument(String id, String title) {
        return DocumentMetadataEntity.builder()
                .id(id)
                .tenantId(TENANT_ID)
                .title(title)
                .category("Guides")
                .summary("Test summary")
                .status("draft")
                .version("1.0")
                .owner("admin")
                .accessLevel("public")
                .reviewCycle("monthly")
                .targetAudience(new String[]{"developers"})
                .tags(new String[]{"test"})
                .lastUpdated(LocalDate.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private DocumentVersionEntity createVersion(String versionNumber, boolean isMajor) {
        return DocumentVersionEntity.builder()
                .id(VERSION_ID)
                .documentId(DOCUMENT_ID)
                .tenantId(TENANT_ID)
                .versionNumber(versionNumber)
                .content("Version content")
                .changeSummary("Changes")
                .changedBy(USER_ID)
                .isMajorVersion(isMajor)
                .isPublished(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private DocumentFeedbackEntity createFeedback(int rating, String comment) {
        return DocumentFeedbackEntity.builder()
                .id(FEEDBACK_ID)
                .documentId(DOCUMENT_ID)
                .tenantId(TENANT_ID)
                .userId(USER_ID)
                .rating(rating)
                .comment(comment)
                .helpful(true)
                .feedbackType("GENERAL")
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
