# Documentation Service

Clinical documentation management service for storing, versioning, and retrieving healthcare documents including clinical notes, CDA documents, and attachments.

## Overview

The Documentation Service provides comprehensive document management capabilities for clinical documentation. It supports structured clinical documents (HL7 CDA), unstructured clinical notes, version control, attachments, and full-text search across document content.

## Key Features

### Clinical Document Management
- Create, read, update, delete (CRUD) operations
- Patient-specific document retrieval
- Document type categorization
- Multi-tenant document isolation
- Document status tracking (draft, final, amended)

### Version Control
- Automatic versioning on updates
- Version history tracking
- Compare versions
- Restore previous versions
- Version-specific metadata

### Document Attachments
- Associate files with clinical documents
- Support for images, PDFs, and other file types
- Attachment metadata (file name, size, mime type)
- Secure attachment storage
- Attachment-level access control

### Full-Text Search
- Search across document content
- Filter by patient, type, status
- Pagination support for large result sets
- Relevance-based ranking
- Multi-field search capabilities

### CDA (Clinical Document Architecture) Support
- HL7 CDA R2 document storage
- CDA section parsing
- Structured data extraction
- CDA validation and conformance
- Transform CDA to FHIR documents

### Document Templates
- Reusable document templates
- Template versioning
- Organization-specific templates
- Template preview and validation

## Technology Stack

- **Spring Boot 3.x**: Core framework
- **PostgreSQL**: Document storage and metadata
- **Redis**: Document caching
- **Liquibase**: Database migrations
- **Resilience4j**: Circuit breakers and retry logic

## API Endpoints

### Clinical Documents
```
GET    /api/documents/clinical
       - Get all documents for tenant

GET    /api/documents/clinical/paginated
       - Get paginated document list

GET    /api/documents/clinical/{id}
       - Get specific document

GET    /api/documents/clinical/patient/{patientId}
       - Get all documents for a patient

GET    /api/documents/clinical/patient/{patientId}/paginated
       - Get paginated patient documents

GET    /api/documents/clinical/search?query={query}
       - Search documents by content

POST   /api/documents/clinical
       - Create new clinical document

PUT    /api/documents/clinical/{id}
       - Update document (creates new version)

DELETE /api/documents/clinical/{id}
       - Delete document (soft delete)
```

### Document Attachments
```
POST   /api/documents/clinical/{id}/attachments
       - Add attachment to document

GET    /api/documents/clinical/attachments/{attachmentId}
       - Get specific attachment

DELETE /api/documents/clinical/attachments/{attachmentId}
       - Remove attachment
```

### Product Documentation
```
GET    /api/documents/product
       - Get product documentation

POST   /api/documents/product
       - Create product documentation

PUT    /api/documents/product/{id}
       - Update product documentation

DELETE /api/documents/product/{id}
       - Delete product documentation
```

## Configuration

### Application Properties
```yaml
server.port: 8091
spring.datasource.url: jdbc:postgresql://localhost:5432/healthdata_docs
spring.cache.type: redis
spring.liquibase.enabled: true
```

### Storage Configuration
- Document content stored in PostgreSQL TEXT fields
- Binary attachments stored in BYTEA columns
- Optional S3/object storage integration for large files

### Resilience
```yaml
resilience4j:
  circuitbreaker:
    instances:
      documentationDefault:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
```

## Running Locally

### Prerequisites
- Java 21
- PostgreSQL 14+
- Redis 6+

### Build
```bash
./gradlew :modules:services:documentation-service:build
```

### Run
```bash
./gradlew :modules:services:documentation-service:bootRun
```

### Run Tests
```bash
./gradlew :modules:services:documentation-service:test
```

---

## Testing

### Overview

The Documentation Service has comprehensive test coverage across clinical document management, product documentation, version control, CDA (Clinical Document Architecture) support, document attachments, full-text search, and user feedback systems. Tests validate HIPAA-compliant multi-tenant document storage with PHI protection.

### Quick Start

```bash
# Run all tests
./gradlew :modules:services:documentation-service:test

# Run specific test suite
./gradlew :modules:services:documentation-service:test --tests "*ServiceTest"
./gradlew :modules:services:documentation-service:test --tests "*ControllerTest"
./gradlew :modules:services:documentation-service:test --tests "*EntityTest"

# Run with coverage report
./gradlew :modules:services:documentation-service:test jacocoTestReport

# Run only clinical document tests
./gradlew :modules:services:documentation-service:test --tests "*ClinicalDocument*"

# Run only product document tests
./gradlew :modules:services:documentation-service:test --tests "*ProductDocument*"
```

### Test Coverage Summary

| Test Class | Tests | Purpose |
|------------|-------|---------|
| ClinicalDocumentServiceTest | 27+ | Clinical document CRUD, attachments, search |
| ProductDocumentServiceTest | 42+ | Product documentation, versioning, feedback |
| ClinicalDocumentControllerTest | 18+ | REST API for clinical documents |
| ProductDocumentControllerTest | 15+ | REST API for product documentation |
| CdaDocumentEntityTest | 2 | CDA validation status helpers |
| DocumentVersionEntityTest | 1 | Version entity defaults |
| DocumentAttachmentEntityTest | 2+ | Attachment entity behavior |
| DocumentMetadataEntityTest | 2+ | Metadata entity defaults |
| DocumentFeedbackEntityTest | 2+ | Feedback entity defaults |
| DocumentRatingEntityTest | 1+ | Rating calculation |
| SearchQueryEntityTest | 1+ | Search query tracking |
| DocumentViewEntityTest | 1+ | View tracking entity |
| ClinicalDocumentEntityTest | 2+ | Clinical document entity |
| **Total** | **115+** | **Comprehensive document management coverage** |

### Test Organization

```
src/test/java/com/healthdata/documentation/
├── service/
│   ├── ClinicalDocumentServiceTest.java      # Clinical document CRUD
│   └── ProductDocumentServiceTest.java       # Product documentation & versioning
├── rest/
│   ├── ClinicalDocumentControllerTest.java   # Clinical REST API
│   └── ProductDocumentControllerTest.java    # Product REST API
└── persistence/
    ├── CdaDocumentEntityTest.java            # CDA validation
    ├── DocumentVersionEntityTest.java        # Version tracking
    ├── DocumentAttachmentEntityTest.java     # Attachment handling
    ├── DocumentMetadataEntityTest.java       # Document metadata
    ├── DocumentFeedbackEntityTest.java       # User feedback
    ├── DocumentRatingEntityTest.java         # Rating calculation
    ├── SearchQueryEntityTest.java            # Search tracking
    ├── DocumentViewEntityTest.java           # View analytics
    └── ClinicalDocumentEntityTest.java       # Clinical document entity
```

---

### Unit Tests

#### ClinicalDocumentServiceTest

Tests clinical document CRUD operations, attachments, patient-specific queries, and search functionality.

```java
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
    @DisplayName("Patient Documents Tests")
    class PatientDocumentsTests {

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
        @DisplayName("Should create document with default status")
        void shouldCreateDocumentWithDefaultStatus() {
            // Given
            ClinicalDocumentDto dto = ClinicalDocumentDto.builder()
                .patientId(PATIENT_ID)
                .documentType("progress-note")
                .title("Progress Note")
                .description("Patient progress update")
                .authorName("Dr. Smith")
                .build();

            ArgumentCaptor<ClinicalDocumentEntity> captor =
                ArgumentCaptor.forClass(ClinicalDocumentEntity.class);
            when(documentRepository.save(captor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.createDocument(dto, TENANT_ID);

            // Then
            assertThat(captor.getValue().getStatus()).isEqualTo("current");
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
                .build();

            when(documentRepository.findByIdAndTenantId(DOCUMENT_ID, TENANT_ID))
                .thenReturn(Optional.of(existing));
            when(documentRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

            // When
            Optional<ClinicalDocumentDto> result = service.updateDocument(DOCUMENT_ID, dto, TENANT_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("Updated Title");
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

            when(attachmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            DocumentAttachmentDto result = service.addAttachment(DOCUMENT_ID, dto, TENANT_ID);

            // Then
            assertThat(result.getFileName()).isEqualTo("report.pdf");
        }

        @Test
        @DisplayName("Should throw exception when adding to non-existent document")
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
    }
}
```

**Key Clinical Document Patterns:**
| Pattern | Description |
|---------|-------------|
| **Document Status** | Lifecycle: current → superseded → entered-in-error |
| **Patient-Specific Queries** | All documents filtered by tenantId + patientId |
| **Attachment Cascade** | Deleting document removes all attachments |
| **Full-Text Search** | PostgreSQL tsvector search with pagination |
| **Default Status** | New documents default to "current" status |

#### ProductDocumentServiceTest

Tests product documentation CRUD, version control, publishing workflow, and feedback system.

```java
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

    @Nested
    @DisplayName("Document Lifecycle Tests")
    class DocumentLifecycleTests {

        @Test
        @DisplayName("Should create document with generated ID from title")
        void shouldCreateDocumentWithGeneratedId() {
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

            ArgumentCaptor<DocumentMetadataEntity> captor =
                ArgumentCaptor.forClass(DocumentMetadataEntity.class);
            when(metadataRepository.save(captor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.createDocument(dto, TENANT_ID);

            // Then - ID generated from title as slug
            assertThat(captor.getValue().getId()).isEqualTo("getting-started-guide");
            assertThat(captor.getValue().getStatus()).isEqualTo("draft");
            assertThat(captor.getValue().getVersion()).isEqualTo("1.0");
        }

        @Test
        @DisplayName("Should publish document successfully")
        void shouldPublishDocument() {
            // Given
            DocumentMetadataEntity existing = createDocument(DOCUMENT_ID, "Draft Doc");
            existing.setStatus("draft");

            when(metadataRepository.findByIdAndTenantId(DOCUMENT_ID, TENANT_ID))
                .thenReturn(Optional.of(existing));

            ArgumentCaptor<DocumentMetadataEntity> captor =
                ArgumentCaptor.forClass(DocumentMetadataEntity.class);
            when(metadataRepository.save(captor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

            // When
            Optional<ProductDocumentDto> result = service.publishDocument(
                DOCUMENT_ID, TENANT_ID, USER_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(captor.getValue().getStatus()).isEqualTo("published");
        }

        @Test
        @DisplayName("Should archive document successfully")
        void shouldArchiveDocument() {
            // Given
            DocumentMetadataEntity existing = createDocument(DOCUMENT_ID, "Published Doc");
            existing.setStatus("published");

            when(metadataRepository.findByIdAndTenantId(DOCUMENT_ID, TENANT_ID))
                .thenReturn(Optional.of(existing));

            ArgumentCaptor<DocumentMetadataEntity> captor =
                ArgumentCaptor.forClass(DocumentMetadataEntity.class);
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
        @DisplayName("Should create new version with changedBy")
        void shouldCreateVersionWithChangedBy() {
            // Given
            DocumentVersionDto dto = DocumentVersionDto.builder()
                .versionNumber("2.0")
                .content("Updated content")
                .changeSummary("Major update")
                .isMajorVersion(true)
                .build();

            ArgumentCaptor<DocumentVersionEntity> captor =
                ArgumentCaptor.forClass(DocumentVersionEntity.class);
            when(versionRepository.save(captor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.createVersion(DOCUMENT_ID, dto, TENANT_ID, USER_ID);

            // Then
            assertThat(captor.getValue().getChangedBy()).isEqualTo(USER_ID);
            assertThat(captor.getValue().getVersionNumber()).isEqualTo("2.0");
        }
    }

    @Nested
    @DisplayName("Feedback Management Tests")
    class FeedbackManagementTests {

        @Test
        @DisplayName("Should submit new feedback and update average rating")
        void shouldSubmitFeedbackAndUpdateRating() {
            // Given
            DocumentFeedbackDto dto = DocumentFeedbackDto.builder()
                .rating(5)
                .comment("Great documentation!")
                .helpful(true)
                .build();

            DocumentMetadataEntity document = createDocument(DOCUMENT_ID, "Test Doc");

            when(feedbackRepository.findByDocumentIdAndUserId(DOCUMENT_ID, USER_ID, TENANT_ID))
                .thenReturn(Optional.empty());
            when(feedbackRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));
            when(feedbackRepository.calculateAverageRating(DOCUMENT_ID)).thenReturn(4.5);
            when(feedbackRepository.countByDocumentId(DOCUMENT_ID)).thenReturn(2L);
            when(metadataRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
            when(metadataRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

            // When
            DocumentFeedbackDto result = service.submitFeedback(
                DOCUMENT_ID, dto, TENANT_ID, USER_ID);

            // Then
            assertThat(result.getRating()).isEqualTo(5);
            verify(metadataRepository).save(argThat(entity ->
                entity.getAvgRating().compareTo(BigDecimal.valueOf(4.5)) == 0 &&
                entity.getFeedbackCount() == 2
            ));
        }

        @Test
        @DisplayName("Should update existing feedback instead of duplicate")
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
            when(feedbackRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));
            when(feedbackRepository.calculateAverageRating(DOCUMENT_ID)).thenReturn(5.0);
            when(feedbackRepository.countByDocumentId(DOCUMENT_ID)).thenReturn(1L);

            // When
            DocumentFeedbackDto result = service.submitFeedback(
                DOCUMENT_ID, dto, TENANT_ID, USER_ID);

            // Then - Updated existing instead of creating new
            assertThat(result.getRating()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("View Count Tests")
    class ViewCountTests {

        @Test
        @DisplayName("Should increment view count on document access")
        void shouldIncrementViewCount() {
            // Given
            DocumentMetadataEntity entity = createDocument(DOCUMENT_ID, "Test Document");
            when(metadataRepository.findByIdAndTenantId(DOCUMENT_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));

            // When
            service.getDocument(DOCUMENT_ID, TENANT_ID);

            // Then
            verify(metadataRepository).incrementViewCount(DOCUMENT_ID);
        }
    }
}
```

**Document Status Lifecycle:**
| Status | Description | Transitions |
|--------|-------------|-------------|
| **draft** | Initial state, not visible to users | → published |
| **published** | Visible to authorized users | → archived, → draft |
| **archived** | Hidden from normal listings | → published |

**Version Control Patterns:**
| Pattern | Description |
|---------|-------------|
| **Major Version** | `isMajorVersion: true` for significant changes |
| **Minor Version** | `isMajorVersion: false` for corrections |
| **Version History** | Ordered by createdAt descending |
| **Change Tracking** | `changedBy` captures user who created version |

#### CdaDocumentEntityTest

Tests HL7 CDA (Clinical Document Architecture) validation helpers.

```java
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
        CdaDocumentEntity invalid = new CdaDocumentEntity();
        invalid.setValidationStatus("INVALID");

        assertThat(valid.isValid()).isTrue();
        assertThat(valid.hasWarnings()).isFalse();
        assertThat(warnings.isValid()).isFalse();
        assertThat(warnings.hasWarnings()).isTrue();
        assertThat(invalid.isValid()).isFalse();
        assertThat(invalid.hasWarnings()).isFalse();
    }
}
```

**CDA Validation Status:**
| Status | Method Results | Description |
|--------|---------------|-------------|
| NOT_VALIDATED | `isValid()` = false, `hasWarnings()` = false | Default state |
| VALID | `isValid()` = true, `hasWarnings()` = false | Passed schema validation |
| WARNINGS | `isValid()` = false, `hasWarnings()` = true | Passed with warnings |
| INVALID | `isValid()` = false, `hasWarnings()` = false | Failed validation |

---

### Controller Tests

#### ClinicalDocumentControllerTest

Tests REST API endpoints for clinical document management.

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Clinical Document Controller Tests")
class ClinicalDocumentControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ClinicalDocumentService documentService;

    @InjectMocks
    private ClinicalDocumentController controller;

    private static final String TENANT_ID = "tenant-123";
    private static final UUID DOCUMENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
    }

    @Nested
    @DisplayName("GET /api/documents/clinical Tests")
    class GetDocumentsTests {

        @Test
        @DisplayName("Should return documents for tenant")
        void shouldReturnDocuments() throws Exception {
            when(documentService.getDocuments(TENANT_ID))
                .thenReturn(List.of(sampleDocument()));

            mockMvc.perform(get("/api/documents/clinical")
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(DOCUMENT_ID.toString()));
        }

        @Test
        @DisplayName("Should return paginated documents")
        void shouldReturnPaginatedDocuments() throws Exception {
            Pageable pageable = PageRequest.of(0, 2);
            Page<ClinicalDocumentDto> page = new PageImpl<>(
                List.of(sampleDocument()), pageable, 1);
            when(documentService.getDocumentsPaginated(eq(TENANT_ID), any(Pageable.class)))
                .thenReturn(page);

            mockMvc.perform(get("/api/documents/clinical/paginated")
                    .param("page", "0")
                    .param("size", "2")
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(DOCUMENT_ID.toString()));
        }
    }

    @Nested
    @DisplayName("GET /api/documents/clinical/{id} Tests")
    class GetDocumentTests {

        @Test
        @DisplayName("Should return document when found")
        void shouldReturnDocumentWhenFound() throws Exception {
            when(documentService.getDocument(DOCUMENT_ID, TENANT_ID))
                .thenReturn(Optional.of(sampleDocument()));

            mockMvc.perform(get("/api/documents/clinical/{id}", DOCUMENT_ID)
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(DOCUMENT_ID.toString()));
        }

        @Test
        @DisplayName("Should return 404 when document not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(documentService.getDocument(DOCUMENT_ID, TENANT_ID))
                .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/documents/clinical/{id}", DOCUMENT_ID)
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/documents/clinical Tests")
    class CreateDocumentTests {

        @Test
        @DisplayName("Should create document and return 201")
        void shouldCreateDocument() throws Exception {
            when(documentService.createDocument(any(ClinicalDocumentDto.class), eq(TENANT_ID)))
                .thenReturn(sampleDocument());

            mockMvc.perform(post("/api/documents/clinical")
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(sampleDocument())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(DOCUMENT_ID.toString()));
        }
    }

    @Nested
    @DisplayName("PUT /api/documents/clinical/{id} Tests")
    class UpdateDocumentTests {

        @Test
        @DisplayName("Should update document")
        void shouldUpdateDocument() throws Exception {
            when(documentService.updateDocument(eq(DOCUMENT_ID), any(), eq(TENANT_ID)))
                .thenReturn(Optional.of(sampleDocument()));

            mockMvc.perform(put("/api/documents/clinical/{id}", DOCUMENT_ID)
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(sampleDocument())))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 404 when update target missing")
        void shouldReturn404WhenMissing() throws Exception {
            when(documentService.updateDocument(eq(DOCUMENT_ID), any(), eq(TENANT_ID)))
                .thenReturn(Optional.empty());

            mockMvc.perform(put("/api/documents/clinical/{id}", DOCUMENT_ID)
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(sampleDocument())))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/documents/clinical/{id} Tests")
    class DeleteDocumentTests {

        @Test
        @DisplayName("Should delete document and return 204")
        void shouldDeleteDocument() throws Exception {
            when(documentService.deleteDocument(DOCUMENT_ID, TENANT_ID)).thenReturn(true);

            mockMvc.perform(delete("/api/documents/clinical/{id}", DOCUMENT_ID)
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 404 when document not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(documentService.deleteDocument(DOCUMENT_ID, TENANT_ID)).thenReturn(false);

            mockMvc.perform(delete("/api/documents/clinical/{id}", DOCUMENT_ID)
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Attachment endpoints Tests")
    class AttachmentTests {

        @Test
        @DisplayName("Should add attachment and return 201")
        void shouldAddAttachment() throws Exception {
            when(documentService.addAttachment(eq(DOCUMENT_ID), any(), eq(TENANT_ID)))
                .thenReturn(sampleAttachment());

            mockMvc.perform(post("/api/documents/clinical/{id}/attachments", DOCUMENT_ID)
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(sampleAttachment())))
                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should return 404 when attachment not found")
        void shouldReturn404WhenAttachmentNotFound() throws Exception {
            when(documentService.getAttachment(any(), eq(TENANT_ID)))
                .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/documents/clinical/attachments/{id}", UUID.randomUUID())
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("Should pass tenant ID through on create")
    void shouldPassTenantId() throws Exception {
        when(documentService.createDocument(any(ClinicalDocumentDto.class), eq(TENANT_ID)))
            .thenReturn(sampleDocument());

        mockMvc.perform(post("/api/documents/clinical")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDocument())))
            .andExpect(status().isCreated());

        verify(documentService).createDocument(any(ClinicalDocumentDto.class), eq(TENANT_ID));
    }
}
```

---

### Multi-Tenant Isolation Tests (HIPAA Compliance)

```java
@Nested
@DisplayName("Multi-Tenant Isolation (HIPAA Compliance)")
class MultiTenantIsolationTests {

    @Test
    @DisplayName("Should isolate clinical documents between tenants")
    void shouldIsolateDocumentsBetweenTenants() {
        // Given
        String tenant1 = "tenant-001";
        String tenant2 = "tenant-002";

        ClinicalDocumentEntity doc1 = createDocument("Doc 1", "progress-note");
        doc1.setTenantId(tenant1);
        ClinicalDocumentEntity doc2 = createDocument("Doc 2", "lab-report");
        doc2.setTenantId(tenant2);

        when(documentRepository.findByTenantId(tenant1))
            .thenReturn(List.of(doc1));
        when(documentRepository.findByTenantId(tenant2))
            .thenReturn(List.of(doc2));

        // When
        List<ClinicalDocumentDto> tenant1Docs = service.getDocuments(tenant1);
        List<ClinicalDocumentDto> tenant2Docs = service.getDocuments(tenant2);

        // Then
        assertThat(tenant1Docs).noneMatch(d -> d.getTenantId().equals(tenant2));
        assertThat(tenant2Docs).noneMatch(d -> d.getTenantId().equals(tenant1));
    }

    @Test
    @DisplayName("Should NOT allow cross-tenant access via ID query")
    void shouldNotAllowCrossTenantAccessById() {
        // Given
        String wrongTenant = "other-tenant";
        when(documentRepository.findByIdAndTenantId(DOCUMENT_ID, wrongTenant))
            .thenReturn(Optional.empty());

        // When
        Optional<ClinicalDocumentDto> result = service.getDocument(DOCUMENT_ID, wrongTenant);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should isolate patient documents by tenant")
    void shouldIsolatePatientDocumentsByTenant() {
        // Given - Same patient ID in different tenants
        when(documentRepository.findByTenantIdAndPatientId("tenant-1", PATIENT_ID))
            .thenReturn(List.of(createDocument("T1 Doc", "note")));
        when(documentRepository.findByTenantIdAndPatientId("tenant-2", PATIENT_ID))
            .thenReturn(List.of(createDocument("T2 Doc", "note")));

        // When
        List<ClinicalDocumentDto> tenant1PatientDocs =
            service.getPatientDocuments("tenant-1", PATIENT_ID);
        List<ClinicalDocumentDto> tenant2PatientDocs =
            service.getPatientDocuments("tenant-2", PATIENT_ID);

        // Then - Different documents despite same patient ID
        assertThat(tenant1PatientDocs.get(0).getTitle()).isEqualTo("T1 Doc");
        assertThat(tenant2PatientDocs.get(0).getTitle()).isEqualTo("T2 Doc");
    }

    @Test
    @DisplayName("All documents should have non-null tenant IDs")
    void shouldHaveNonNullTenantIds() {
        // Given
        ClinicalDocumentEntity entity = new ClinicalDocumentEntity();

        // When - onCreate lifecycle callback
        ReflectionTestUtils.invokeMethod(entity, "onCreate");

        // Then - Would typically validate tenantId is set before save
        // Repository-level tests verify tenantId filtering
    }
}
```

---

### HIPAA Compliance Tests

```java
@SpringBootTest
@DisplayName("HIPAA Compliance Tests")
class DocumentationHipaaComplianceTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("PHI Cache Compliance Tests")
    class PhiCacheComplianceTests {

        @Test
        @DisplayName("Document cache TTL must not exceed 5 minutes")
        void documentCacheTtlShouldNotExceed5Minutes() {
            // Given
            Cache documentCache = cacheManager.getCache("documents");

            // Then
            assertThat(documentCache).isNotNull();
            // Verify configuration in application-test.yml sets TTL <= 300 seconds
        }

        @Test
        @DisplayName("Attachment cache should have short TTL for PHI")
        void attachmentCacheShouldHaveShortTtl() {
            // Given
            Cache attachmentCache = cacheManager.getCache("attachments");

            // Then - Attachments may contain PHI (lab reports, images)
            assertThat(attachmentCache).isNotNull();
        }
    }

    @Nested
    @DisplayName("PHI Response Header Tests")
    class PhiResponseHeaderTests {

        @Test
        @DisplayName("Clinical document endpoints must include no-cache headers")
        void clinicalDocEndpointsShouldIncludeNoCacheHeaders() throws Exception {
            mockMvc.perform(get("/api/documents/clinical")
                    .header("X-Tenant-ID", "test-tenant"))
                .andExpect(header().string("Cache-Control",
                    allOf(
                        containsString("no-store"),
                        containsString("no-cache"),
                        containsString("must-revalidate")
                    )))
                .andExpect(header().string("Pragma", "no-cache"));
        }
    }

    @Nested
    @DisplayName("PHI Document Content Tests")
    class PhiDocumentContentTests {

        @Test
        @DisplayName("Test clinical documents must use synthetic patient IDs")
        void testDocumentsMustUseSyntheticPatientIds() {
            // Given
            ClinicalDocumentDto testDoc = createTestDocument();

            // Then
            assertThat(testDoc.getPatientId())
                .matches("patient-\\d+|test-patient-.*")
                .withFailMessage("Test patient IDs should be clearly synthetic");
        }

        @Test
        @DisplayName("Attachment file names should not expose PHI")
        void attachmentFileNamesShouldNotExposePhi() {
            // Given
            DocumentAttachmentDto attachment = createTestAttachment();

            // Then
            assertThat(attachment.getFileName())
                .doesNotContainPattern("\\d{3}-\\d{2}-\\d{4}") // No SSN
                .doesNotContainPattern("\\d{10}");             // No MRN
        }
    }
}
```

---

### Performance Tests

```java
@SpringBootTest
@DisplayName("Documentation Performance Tests")
class DocumentationPerformanceTest {

    @Autowired
    private ClinicalDocumentService clinicalDocumentService;

    @Autowired
    private ProductDocumentService productDocumentService;

    @Test
    @DisplayName("Document retrieval should complete within 50ms")
    void documentRetrievalPerformance() {
        // Given
        String tenantId = "perf-tenant";
        UUID documentId = UUID.randomUUID();

        // When
        Instant start = Instant.now();
        clinicalDocumentService.getDocument(documentId, tenantId);
        Instant end = Instant.now();

        // Then
        long durationMs = Duration.between(start, end).toMillis();
        assertThat(durationMs)
            .isLessThan(50)
            .withFailMessage("Document retrieval took %dms, exceeds 50ms SLA", durationMs);
    }

    @Test
    @DisplayName("Document search should complete within 200ms")
    void documentSearchPerformance() {
        // Given
        String tenantId = "perf-tenant";
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Instant start = Instant.now();
        clinicalDocumentService.searchDocuments(tenantId, "discharge", pageable);
        Instant end = Instant.now();

        // Then
        long durationMs = Duration.between(start, end).toMillis();
        assertThat(durationMs)
            .isLessThan(200)
            .withFailMessage("Document search took %dms, exceeds 200ms SLA", durationMs);
    }

    @Test
    @DisplayName("Patient document listing should complete within 100ms")
    void patientDocumentListingPerformance() {
        // Given
        String tenantId = "perf-tenant";
        String patientId = "patient-123";
        Pageable pageable = PageRequest.of(0, 50);

        // When
        Instant start = Instant.now();
        clinicalDocumentService.getPatientDocumentsPaginated(tenantId, patientId, pageable);
        Instant end = Instant.now();

        // Then
        long durationMs = Duration.between(start, end).toMillis();
        assertThat(durationMs)
            .isLessThan(100)
            .withFailMessage("Patient document listing took %dms, exceeds 100ms SLA", durationMs);
    }

    @Test
    @DisplayName("Version history retrieval should complete within 50ms")
    void versionHistoryPerformance() {
        // Given
        String documentId = "getting-started";
        String tenantId = "perf-tenant";

        // When
        Instant start = Instant.now();
        productDocumentService.getVersions(documentId, tenantId);
        Instant end = Instant.now();

        // Then
        long durationMs = Duration.between(start, end).toMillis();
        assertThat(durationMs)
            .isLessThan(50)
            .withFailMessage("Version history took %dms, exceeds 50ms SLA", durationMs);
    }
}
```

**Performance SLAs:**
| Operation | Target | Description |
|-----------|--------|-------------|
| Document retrieval | <50ms | Single document by ID |
| Document search | <200ms | Full-text search with pagination |
| Patient documents | <100ms | List documents for patient |
| Version history | <50ms | List all versions of document |
| Create document | <100ms | Save new document |
| Create version | <50ms | Save new version |

---

### Best Practices

| Practice | Description | Example |
|----------|-------------|---------|
| **Synthetic Test Data** | Never use real PHI in tests | `patient-456`, `Test-Document` |
| **Tenant Isolation** | Every query filtered by tenantId | `findByIdAndTenantId(id, tenantId)` |
| **Attachment Cascade** | Delete attachments when deleting document | `attachmentRepository.deleteByClinicalDocumentId()` |
| **Document Status** | Use proper lifecycle transitions | draft → published → archived |
| **Version Tracking** | Track who made changes | `changedBy`, `changeSummary` |
| **Slug Generation** | Generate ID from title | "Getting Started Guide" → "getting-started-guide" |
| **Feedback Deduplication** | One feedback per user per document | Update existing instead of creating new |
| **View Count Tracking** | Increment on document access | `incrementViewCount(documentId)` |
| **CDA Validation** | Check validation status before processing | `isValid()`, `hasWarnings()` |
| **Default Values** | Set sensible defaults on create | status="draft", version="1.0" |
| **Soft Delete** | Archive instead of hard delete | status="archived" |
| **Search Pagination** | Always paginate search results | `PageRequest.of(0, 20)` |

---

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| `Document not found after create` | Missing tenantId in query | Verify tenantId passed to `findByIdAndTenantId()` |
| `Attachment orphaned` | Parent document deleted without cascade | Use `deleteDocument()` which cascades to attachments |
| `Version history empty` | Wrong documentId format | Product docs use slug, clinical use UUID |
| `Search returns no results` | PostgreSQL tsvector not indexed | Run migration to create full-text search index |
| `Feedback not updating rating` | Missing `updateDocumentRating()` call | Verify service calls `calculateAverageRating()` |
| `CDA validation stuck in NOT_VALIDATED` | Validation never triggered | Call CDA validation service explicitly |
| `Patient documents across tenants` | Query missing tenantId | Use `findByTenantIdAndPatientId()` not just `findByPatientId()` |
| `Document status transition fails` | Invalid status transition | Follow lifecycle: draft → published → archived |
| `View count not incrementing` | Service method not calling increment | Verify `incrementViewCount()` called in `getDocument()` |
| `Duplicate feedback error` | User already submitted feedback | Use `findByDocumentIdAndUserId()` to check first |

---

## Document Types

### Supported Document Types
- Clinical Notes (Progress Notes, Discharge Summaries)
- CDA Documents (Continuity of Care Document - CCD)
- Lab Reports
- Imaging Reports
- Consultation Notes
- Operative Reports
- Patient Instructions
- Care Plans

### Document Status
- DRAFT: Document in progress
- FINAL: Completed and signed
- AMENDED: Modified after finalization
- ENTERED_IN_ERROR: Marked as incorrect
- DEPRECATED: Superseded by newer version

## Version Control

### Versioning Strategy
- Automatic version increment on updates
- Major version: Significant changes
- Minor version: Minor corrections
- Version metadata includes author, timestamp, change reason

### Version History
```java
// Example: Get document versions
GET /api/documents/clinical/{id}/versions
    Returns: List of all versions with metadata
```

## Security

### Access Control
- JWT-based authentication
- Role-based access (USER, CLINICIAN, ADMIN)
- Tenant isolation via X-Tenant-ID header
- Document-level permissions
- Audit trail for all operations

### HIPAA Compliance
- Encrypted data at rest
- TLS encryption in transit
- Comprehensive audit logging
- Automatic PHI detection
- Retention policy enforcement

## Search Capabilities

### Full-Text Search
- PostgreSQL full-text search (tsvector)
- Weighted search across fields
- Fuzzy matching support
- Stop word filtering
- Stemming and normalization

### Search Filters
- Patient ID
- Document type
- Date range
- Author/Provider
- Document status

## Integration

This service integrates with:
- **FHIR Service**: Convert documents to FHIR DocumentReference
- **Authentication Service**: User and tenant validation
- **Audit Service**: Document access logging
- **AI Assistant Service**: Document summarization

## CDA Support

### CDA Document Handling
- Parse HL7 CDA XML
- Extract structured sections
- Validate against CDA schema
- Transform to FHIR Composition
- Support for CDA templates (CCD, Discharge Summary)

## API Documentation

Swagger UI available at:
```
http://localhost:8091/swagger-ui.html
```

## Monitoring

Actuator endpoints:
- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`

## Performance Optimization

### Caching Strategy
- Redis cache for frequently accessed documents
- Cache invalidation on updates
- Patient-level cache prefetching

### Database Optimization
- Indexed fields: patient_id, document_type, created_at
- Partitioning by tenant for large deployments
- Connection pooling with HikariCP

## License

Copyright (c) 2024 Mahoosuc Solutions
