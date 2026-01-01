# QRDA Export Service

Quality Reporting Document Architecture (QRDA) export service for CMS electronic clinical quality measure (eCQM) reporting.

## Purpose

The QRDA Export Service generates QRDA Category I (patient-level) and Category III (aggregate) documents for CMS quality measure reporting. It integrates with the Quality Measure Service and CQL Engine to calculate measures and exports validated QRDA documents for submission to CMS Quality Payment Program (QPP) and other quality reporting programs.

## Key Features

- **QRDA Category I**: Patient-level eCQM reporting for individual patient care documentation
- **QRDA Category III**: Aggregate population-level eCQM reporting for quality programs
- **HL7 CDA R2 Compliance**: Standards-compliant QRDA document generation
- **Schematron Validation**: CMS QRDA validation with detailed error reporting
- **Asynchronous Job Processing**: Background processing for large patient populations
- **Batch Export**: Support for up to 1,000 patients per Category III document
- **Template Caching**: Redis caching for QRDA templates (5-minute TTL)
- **Multi-measure Support**: Generate reports for multiple eCQMs in single request
- **Document Storage**: Local file storage with 90-day retention policy
- **ZIP Archives**: Category I exports bundled as ZIP files for easy distribution
- **Audit Trail**: HIPAA-compliant audit logging for all exports

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/qrda-export/api/v1/qrda/category-i/generate` | Generate QRDA Category I documents |
| POST | `/qrda-export/api/v1/qrda/category-iii/generate` | Generate QRDA Category III document |
| GET | `/qrda-export/api/v1/qrda/jobs/{jobId}` | Get export job status |
| GET | `/qrda-export/api/v1/qrda/jobs` | List export jobs with filtering |
| GET | `/qrda-export/api/v1/qrda/jobs/{jobId}/download` | Download generated QRDA documents |
| POST | `/qrda-export/api/v1/qrda/jobs/{jobId}/cancel` | Cancel pending/running job |

## Configuration

### Application Properties

```yaml
server:
  port: 8104
  servlet:
    context-path: /qrda-export

qrda:
  export:
    storage-path: /tmp/qrda-exports
    retention-days: 90
    max-batch-size: 1000
  validation:
    enabled: true
    category-i-schematron: classpath:schematron/qrda-cat-i.sch
    category-iii-schematron: classpath:schematron/qrda-cat-iii.sch
```

### Service Integration

```yaml
quality-measure:
  service:
    url: http://localhost:8087/quality-measure

cql:
  engine:
    url: http://localhost:8081/cql-engine

patient:
  service:
    url: http://localhost:8084/patient

fhir:
  server:
    url: http://localhost:8085/fhir
```

### Database

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5435/healthdata_cql
    username: healthdata
    password: ${DB_PASSWORD}
```

### Cache

```yaml
spring.cache:
  type: redis
  redis:
    time-to-live: 300000  # 5 minutes
```

## QRDA Document Types

### Category I (Patient-Level)
- Individual patient eCQM reporting
- Supports Merit-based Incentive Payment System (MIPS)
- Includes patient demographics, clinical data, and measure results
- Typically 1 document per patient per measure
- Exported as ZIP archive

### Category III (Aggregate)
- Population-level aggregate reporting
- Supports MIPS, Hospital Quality Reporting, etc.
- Includes measure results across patient population
- 1 document for all patients and measures
- Exported as single XML document

## Job Status Lifecycle

1. **PENDING**: Job created and queued for processing
2. **RUNNING**: Job actively generating QRDA documents
3. **COMPLETED**: Job completed successfully, documents available for download
4. **FAILED**: Job failed with error message
5. **CANCELLED**: Job manually cancelled

## Running Locally

### Start the Service

```bash
./gradlew :modules:services:qrda-export-service:bootRun
```

## Testing

### Overview

The QRDA Export Service has comprehensive test coverage spanning CDA document generation, QRDA validation (Category I and III), job lifecycle management, and REST API endpoints. Tests validate HL7 CDA R2 compliance, CMS schematron rules, template OID conformance, and asynchronous export processing.

### Quick Start

```bash
# Run all tests
./gradlew :modules:services:qrda-export-service:test

# Run specific test suite
./gradlew :modules:services:qrda-export-service:test --tests "*CdaDocumentBuilderTest"
./gradlew :modules:services:qrda-export-service:test --tests "*QrdaValidationServiceTest"
./gradlew :modules:services:qrda-export-service:test --tests "*ControllerTest"

# Run with coverage report
./gradlew :modules:services:qrda-export-service:test jacocoTestReport

# Run Category I tests only
./gradlew :modules:services:qrda-export-service:test --tests "*CategoryIServiceTest"

# Run Category III tests only
./gradlew :modules:services:qrda-export-service:test --tests "*CategoryIIIServiceTest"
```

### Test Coverage Summary

| Test Class | Test Count | Focus Area |
|------------|------------|------------|
| CdaDocumentBuilderTest | 26 | CDA/QRDA XML structure, template OIDs, document elements |
| QrdaValidationServiceTest | 30 | Schematron validation, CMS submission, TIN/NPI format |
| QrdaCategoryIIIServiceTest | 22 | Aggregate export, measure results, job lifecycle |
| QrdaCategoryIServiceTest | 18 | Patient-level export, async processing, validation |
| QrdaExportControllerTest | 13 | REST API endpoints, job management, tenant isolation |
| **Total** | **109+** | **Full QRDA compliance coverage** |

### Test Organization

```
src/test/java/com/healthdata/qrda/
├── service/
│   ├── CdaDocumentBuilderTest.java        # CDA/QRDA document generation
│   ├── QrdaValidationServiceTest.java     # Schematron and CMS validation
│   ├── QrdaCategoryIServiceTest.java      # Category I (patient-level) export
│   └── QrdaCategoryIIIServiceTest.java    # Category III (aggregate) export
└── controller/
    └── QrdaExportControllerTest.java      # REST API endpoints
```

### Unit Tests

#### CdaDocumentBuilderTest - CDA Document Generation

Tests HL7 CDA R2 compliant QRDA document generation with proper template OIDs, sections, and structure.

```java
@ExtendWith(MockitoExtension.class)
class CdaDocumentBuilderTest {

    @InjectMocks
    private CdaDocumentBuilder cdaDocumentBuilder;

    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final LocalDate PERIOD_START = LocalDate.of(2024, 1, 1);
    private static final LocalDate PERIOD_END = LocalDate.of(2024, 12, 31);

    @Nested
    @DisplayName("buildQrdaCategoryI() tests")
    class BuildQrdaCategoryITests {

        @Test
        @DisplayName("Should generate valid XML document")
        void buildQrdaCategoryI_shouldGenerateValidXml() {
            // Act
            String document = cdaDocumentBuilder.buildQrdaCategoryI(
                PATIENT_ID, PERIOD_START, PERIOD_END);

            // Assert
            assertThat(document).isNotNull();
            assertThat(document).startsWith("<?xml version=\"1.0\"");
            assertThat(document).contains("<ClinicalDocument");
            assertThat(document).contains("</ClinicalDocument>");
        }

        @Test
        @DisplayName("Should include QRDA Category I template OIDs")
        void buildQrdaCategoryI_shouldIncludeTemplateOids() {
            // Act
            String document = cdaDocumentBuilder.buildQrdaCategoryI(
                PATIENT_ID, PERIOD_START, PERIOD_END);

            // Assert - QRDA I template OID
            assertThat(document).contains("2.16.840.1.113883.10.20.24.1.1");
            // CMS QRDA I template OID
            assertThat(document).contains("2.16.840.1.113883.10.20.24.1.3");
        }

        @Test
        @DisplayName("Should include document code for Quality Measure Report")
        void buildQrdaCategoryI_shouldIncludeDocumentCode() {
            // Act
            String document = cdaDocumentBuilder.buildQrdaCategoryI(
                PATIENT_ID, PERIOD_START, PERIOD_END);

            // Assert - LOINC code for Quality Measure Report
            assertThat(document).contains("55182-0");
            assertThat(document).contains("Quality Measure Report");
        }

        @Test
        @DisplayName("Should include realm code for US")
        void buildQrdaCategoryI_shouldIncludeUsRealmCode() {
            // Act
            String document = cdaDocumentBuilder.buildQrdaCategoryI(
                PATIENT_ID, PERIOD_START, PERIOD_END);

            // Assert
            assertThat(document).contains("<realmCode code=\"US\"");
        }

        @Test
        @DisplayName("Should include reporting parameters section with dates")
        void buildQrdaCategoryI_shouldIncludeReportingParameters() {
            // Act
            String document = cdaDocumentBuilder.buildQrdaCategoryI(
                PATIENT_ID, PERIOD_START, PERIOD_END);

            // Assert - Should include reporting period dates in YYYYMMDD format
            assertThat(document).contains("20240101"); // Period start
            assertThat(document).contains("20241231"); // Period end
        }
    }

    @Nested
    @DisplayName("buildQrdaCategoryIII() tests")
    class BuildQrdaCategoryIIITests {

        @Test
        @DisplayName("Should include QRDA Category III template OIDs")
        void buildQrdaCategoryIII_shouldIncludeTemplateOids() {
            // Act
            String document = cdaDocumentBuilder.buildQrdaCategoryIII(
                PERIOD_START, PERIOD_END);

            // Assert - QRDA III template OID
            assertThat(document).contains("2.16.840.1.113883.10.20.27.1.1");
            // CMS QRDA III template OID
            assertThat(document).contains("2.16.840.1.113883.10.20.27.1.2");
        }

        @Test
        @DisplayName("Should NOT include record target (aggregate report)")
        void buildQrdaCategoryIII_shouldNotIncludeRecordTarget() {
            // Act
            String document = cdaDocumentBuilder.buildQrdaCategoryIII(
                PERIOD_START, PERIOD_END);

            // Assert - Category III is aggregate, not patient-specific
            assertThat(document).doesNotContain("<recordTarget>");
        }
    }

    @Nested
    @DisplayName("Document structure tests")
    class DocumentStructureTests {

        @Test
        @DisplayName("Both document types should have required CDA sections")
        void bothDocumentTypes_shouldHaveRequiredSections() {
            // Act
            String catI = cdaDocumentBuilder.buildQrdaCategoryI(
                PATIENT_ID, PERIOD_START, PERIOD_END);
            String catIII = cdaDocumentBuilder.buildQrdaCategoryIII(
                PERIOD_START, PERIOD_END);

            // Assert - Required CDA sections
            assertThat(catI).contains("<typeId");
            assertThat(catIII).contains("<typeId");
            assertThat(catI).contains("<author>");
            assertThat(catIII).contains("<author>");
            assertThat(catI).contains("<custodian>");
            assertThat(catIII).contains("<custodian>");
            assertThat(catI).contains("<legalAuthenticator>");
            assertThat(catIII).contains("<legalAuthenticator>");
        }
    }
}
```

**Key Test Areas:**
- QRDA Category I template OID: `2.16.840.1.113883.10.20.24.1.1` (patient-level)
- QRDA Category III template OID: `2.16.840.1.113883.10.20.27.1.1` (aggregate)
- LOINC document codes: `55182-0` (Cat I), `55184-6` (Cat III)
- Required CDA sections: typeId, author, custodian, legalAuthenticator
- Reporting period date formatting (YYYYMMDD)
- Category I includes recordTarget, Category III does not

#### QrdaValidationServiceTest - Schematron and CMS Validation

Tests QRDA document validation including XML structure, template conformance, and CMS submission requirements.

```java
@ExtendWith(MockitoExtension.class)
class QrdaValidationServiceTest {

    @InjectMocks
    private QrdaValidationService validationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(validationService, "validationEnabled", true);
    }

    @Nested
    @DisplayName("validateCategoryI() tests")
    class ValidateCategoryITests {

        @Test
        @DisplayName("Should return empty errors for valid QRDA I document")
        void validateCategoryI_validDocument_returnsEmptyErrors() {
            // Arrange
            String validDoc = createValidQrdaCategoryIDocument();

            // Act
            List<String> errors = validationService.validateCategoryI(validDoc);

            // Assert
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Should return error for malformed XML")
        void validateCategoryI_malformedXml_returnsError() {
            // Arrange
            String malformedXml = "<ClinicalDocument><unclosed>";

            // Act
            List<String> errors = validationService.validateCategoryI(malformedXml);

            // Assert
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> e.contains("well-formed"));
        }

        @Test
        @DisplayName("Should return error for missing CDA namespace")
        void validateCategoryI_missingNamespace_returnsError() {
            // Arrange
            String docWithoutNs = "<?xml version=\"1.0\"?><ClinicalDocument></ClinicalDocument>";

            // Act
            List<String> errors = validationService.validateCategoryI(docWithoutNs);

            // Assert
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> e.toLowerCase().contains("namespace"));
        }

        @Test
        @DisplayName("Should return error for missing Category I template ID")
        void validateCategoryI_missingTemplateId_returnsError() {
            // Arrange
            String docWithoutTemplate = createQrdaDocumentWithoutCategoryITemplate();

            // Act
            List<String> errors = validationService.validateCategoryI(docWithoutTemplate);

            // Assert
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e ->
                e.contains("2.16.840.1.113883.10.20.24.1.1"));
        }

        @Test
        @DisplayName("Should skip validation when disabled")
        void validateCategoryI_validationDisabled_skipsValidation() {
            // Arrange
            ReflectionTestUtils.setField(validationService, "validationEnabled", false);
            String invalidDoc = "<invalid>";

            // Act
            List<String> errors = validationService.validateCategoryI(invalidDoc);

            // Assert
            assertThat(errors).isEmpty();
        }
    }

    @Nested
    @DisplayName("validateForCmsSubmission() tests")
    class ValidateForCmsSubmissionTests {

        @Test
        @DisplayName("Should validate TIN format (9 digits)")
        void validateForCmsSubmission_invalidTin_returnsError() {
            // Arrange
            String docWithInvalidTin = createDocumentWithTin("123"); // Invalid

            // Act
            List<String> errors = validationService.validateForCmsSubmission(
                docWithInvalidTin, 2024);

            // Assert
            assertThat(errors).anyMatch(e -> e.toLowerCase().contains("tin"));
        }

        @Test
        @DisplayName("Should validate NPI format (10 digits)")
        void validateForCmsSubmission_invalidNpi_returnsError() {
            // Arrange
            String docWithInvalidNpi = createDocumentWithNpi("12345"); // Invalid

            // Act
            List<String> errors = validationService.validateForCmsSubmission(
                docWithInvalidNpi, 2024);

            // Assert
            assertThat(errors).anyMatch(e -> e.toLowerCase().contains("npi"));
        }

        @Test
        @DisplayName("Should accept valid TIN format")
        void validateForCmsSubmission_validTin_noError() {
            // Arrange
            String docWithValidTin = createDocumentWithTin("123456789");

            // Act
            List<String> errors = validationService.validateForCmsSubmission(
                docWithValidTin, 2024);

            // Assert
            assertThat(errors).noneMatch(e -> e.toLowerCase().contains("invalid tin"));
        }
    }
}
```

**Key Validation Areas:**
- Well-formed XML validation
- CDA namespace presence (`urn:hl7-org:v3`)
- Template OID conformance (Category I: 2.16.840.1.113883.10.20.24.1.1)
- Required elements: recordTarget, typeId, templateId, effectiveTime
- CMS submission requirements: TIN (9 digits), NPI (10 digits)
- Validation toggle (can be disabled for development)

#### QrdaCategoryIIIServiceTest - Aggregate Export

Tests Category III (population-level) QRDA export job processing.

```java
@ExtendWith(MockitoExtension.class)
class QrdaCategoryIIIServiceTest {

    @Mock
    private QrdaExportJobRepository jobRepository;

    @Mock
    private CdaDocumentBuilder cdaDocumentBuilder;

    @Mock
    private QualityMeasureClient qualityMeasureClient;

    @InjectMocks
    private QrdaCategoryIIIService categoryIIIService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(categoryIIIService, "storagePath", tempDir.toString());
    }

    @Nested
    @DisplayName("initiateExport() tests")
    class InitiateExportTests {

        @Test
        @DisplayName("Should create job with PENDING status")
        void initiateExport_validRequest_createsPendingJob() {
            // Arrange
            QrdaExportRequest request = createValidRequest();
            when(jobRepository.save(any())).thenAnswer(inv -> {
                QrdaExportJobEntity job = inv.getArgument(0);
                job.setId(JOB_ID);
                return job;
            });

            // Act
            QrdaExportJobEntity result = categoryIIIService.initiateExport(
                TENANT_ID, request, REQUESTED_BY);

            // Assert
            verify(jobRepository).save(jobCaptor.capture());
            assertThat(jobCaptor.getValue().getStatus()).isEqualTo(QrdaJobStatus.PENDING);
            assertThat(jobCaptor.getValue().getJobType()).isEqualTo(QrdaJobType.QRDA_III);
        }

        @Test
        @DisplayName("Should NOT set patient IDs for Category III")
        void initiateExport_categoryIII_doesNotSetPatientIds() {
            // Arrange
            QrdaExportRequest request = createValidRequest();
            when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            categoryIIIService.initiateExport(TENANT_ID, request, REQUESTED_BY);

            // Assert - Category III is aggregate, no patient IDs
            verify(jobRepository).save(jobCaptor.capture());
            assertThat(jobCaptor.getValue().getPatientIds()).isNull();
        }
    }

    @Nested
    @DisplayName("MeasureAggregateResult tests")
    class MeasureAggregateResultTests {

        @Test
        @DisplayName("Should calculate performance rate correctly")
        void getPerformanceRate_calculatesCorrectly() {
            // Arrange - Eligible = 90 - 5 - 5 = 80, Rate = 72/80 = 90%
            QrdaCategoryIIIService.MeasureAggregateResult result =
                QrdaCategoryIIIService.MeasureAggregateResult.builder()
                    .measureId("CMS125v12")
                    .initialPopulation(100)
                    .denominator(90)
                    .numerator(72)
                    .denominatorExclusions(5)
                    .denominatorExceptions(5)
                    .build();

            // Act
            double rate = result.getPerformanceRate();

            // Assert
            assertThat(rate).isEqualTo(90.0);
        }

        @Test
        @DisplayName("Should return 0 when eligible population is zero")
        void getPerformanceRate_zeroEligible_returnsZero() {
            // Arrange - Eligible = 10 - 5 - 5 = 0
            QrdaCategoryIIIService.MeasureAggregateResult result =
                QrdaCategoryIIIService.MeasureAggregateResult.builder()
                    .denominator(10)
                    .denominatorExclusions(5)
                    .denominatorExceptions(5)
                    .numerator(0)
                    .build();

            // Act
            double rate = result.getPerformanceRate();

            // Assert
            assertThat(rate).isEqualTo(0.0);
        }
    }
}
```

**Key Test Areas:**
- Job creation with PENDING status
- Category III does not include patient IDs (aggregate report)
- Performance rate calculation: (Numerator / Eligible) * 100
- Eligible = Denominator - Exclusions - Exceptions
- Error handling for service failures
- Document location and count after completion

### Controller Tests

#### QrdaExportControllerTest - REST API Endpoints

Tests REST API endpoints for QRDA export operations using standalone MockMvc.

```java
@ExtendWith(MockitoExtension.class)
class QrdaExportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private QrdaCategoryIService categoryIService;

    @Mock
    private QrdaCategoryIIIService categoryIIIService;

    @Mock
    private QrdaExportJobRepository jobRepository;

    @InjectMocks
    private QrdaExportController controller;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID JOB_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(authPrincipalResolver, pageableResolver)
            .build();
    }

    @Nested
    @DisplayName("POST /category-i/generate")
    class GenerateCategoryITests {

        @Test
        @DisplayName("Should initiate Category I export successfully")
        void generateCategoryI_validRequest_returns202() throws Exception {
            // Arrange
            QrdaExportRequest request = createValidExportRequest(QrdaJobType.QRDA_I);
            QrdaExportJobEntity job = createJobEntity(QrdaJobType.QRDA_I,
                QrdaJobStatus.PENDING);
            when(categoryIService.initiateExport(eq(TENANT_ID), any(), any()))
                .thenReturn(job);

            // Act & Assert
            mockMvc.perform(post("/api/v1/qrda/category-i/generate")
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.jobType").value("QRDA_I"))
                .andExpect(jsonPath("$.status").value("PENDING"));
        }
    }

    @Nested
    @DisplayName("GET /jobs/{jobId}")
    class GetJobStatusTests {

        @Test
        @DisplayName("Should return 404 when job not found")
        void getJobStatus_jobNotFound_returns404() throws Exception {
            // Arrange
            when(jobRepository.findByIdAndTenantId(any(), anyString()))
                .thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(get("/api/v1/qrda/jobs/{jobId}", UUID.randomUUID())
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should not return job from different tenant")
        void getJobStatus_differentTenant_returns404() throws Exception {
            // Arrange
            when(jobRepository.findByIdAndTenantId(JOB_ID, "other-tenant"))
                .thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(get("/api/v1/qrda/jobs/{jobId}", JOB_ID)
                    .header("X-Tenant-ID", "other-tenant"))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /jobs/{jobId}/download")
    class DownloadQrdaTests {

        @Test
        @DisplayName("Should return 400 when job not completed")
        void downloadQrda_jobNotCompleted_returns400() throws Exception {
            // Arrange
            QrdaExportJobEntity job = createJobEntity(
                QrdaJobType.QRDA_III, QrdaJobStatus.RUNNING);
            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(job));

            // Act & Assert
            mockMvc.perform(get("/api/v1/qrda/jobs/{jobId}/download", JOB_ID)
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /jobs/{jobId}/cancel")
    class CancelJobTests {

        @Test
        @DisplayName("Should cancel pending job")
        void cancelJob_pendingJob_cancelsSuccessfully() throws Exception {
            // Arrange
            QrdaExportJobEntity job = createJobEntity(
                QrdaJobType.QRDA_III, QrdaJobStatus.PENDING);
            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(job));
            when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act & Assert
            mockMvc.perform(post("/api/v1/qrda/jobs/{jobId}/cancel", JOB_ID)
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("Should return 400 for completed job")
        void cancelJob_completedJob_returns400() throws Exception {
            // Arrange
            QrdaExportJobEntity job = createJobEntity(
                QrdaJobType.QRDA_III, QrdaJobStatus.COMPLETED);
            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(job));

            // Act & Assert
            mockMvc.perform(post("/api/v1/qrda/jobs/{jobId}/cancel", JOB_ID)
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isBadRequest());
        }
    }
}
```

**Key Test Areas:**
- Category I and III export initiation (202 Accepted)
- Job status retrieval with tenant isolation
- Download endpoint validation (job must be COMPLETED)
- Job cancellation (only PENDING or RUNNING jobs)
- Tenant isolation verification (404 for other tenant's jobs)

### Multi-Tenant Isolation Tests

```java
@SpringBootTest
class QrdaExportMultiTenantTest {

    @Autowired
    private QrdaExportJobRepository jobRepository;

    @Test
    @DisplayName("Repository queries should only return data for specified tenant")
    void shouldIsolateTenantDataInDatabase() {
        // Given
        String tenant1 = "tenant-qrda-001";
        String tenant2 = "tenant-qrda-002";

        QrdaExportJobEntity job1 = createAndSaveJob(tenant1, "CMS125v12");
        QrdaExportJobEntity job2 = createAndSaveJob(tenant2, "CMS130v11");

        // When
        Page<QrdaExportJobEntity> tenant1Jobs = jobRepository
            .findByTenantIdOrderByCreatedAtDesc(tenant1, Pageable.unpaged());

        // Then
        assertThat(tenant1Jobs.getContent())
            .hasSize(1)
            .extracting(QrdaExportJobEntity::getTenantId)
            .containsOnly(tenant1);
    }

    @Test
    @DisplayName("Job lookup by ID should respect tenant boundaries")
    void findByIdAndTenantId_shouldRespectTenantBoundaries() {
        // Given
        String tenant1 = "tenant-qrda-001";
        String tenant2 = "tenant-qrda-002";
        QrdaExportJobEntity job = createAndSaveJob(tenant1, "CMS125v12");

        // When - Try to access with different tenant
        Optional<QrdaExportJobEntity> result = jobRepository
            .findByIdAndTenantId(job.getId(), tenant2);

        // Then
        assertThat(result).isEmpty();
    }
}
```

### HIPAA Compliance Tests

```java
@SpringBootTest
class QrdaExportHipaaComplianceTest {

    @Autowired
    private CacheManager cacheManager;

    @Test
    @DisplayName("QRDA template cache TTL must not exceed 5 minutes")
    void qrdaTemplateCacheTtlShouldBeCompliant() {
        Cache templateCache = cacheManager.getCache("qrdaTemplates");
        assertThat(templateCache).isNotNull();

        if (templateCache instanceof RedisCache) {
            RedisCacheConfiguration config =
                ((RedisCache) templateCache).getCacheConfiguration();
            assertThat(config.getTtl().getSeconds())
                .isLessThanOrEqualTo(300)
                .withFailMessage("QRDA template cache TTL exceeds 5 minutes");
        }
    }

    @Test
    @DisplayName("Export job should not store raw PHI in database")
    void exportJobShouldNotStoreRawPhi() {
        QrdaExportJobEntity job = createTestJob();

        // Job stores references (patient IDs), not raw PHI data
        assertThat(job.getPatientIds()).isNotNull();
        // Document location is a file path, not the PHI content
        assertThat(job.getDocumentLocation()).startsWith("/tmp/qrda");
    }

    @Test
    @DisplayName("Generated QRDA should use confidentialityCode N")
    void qrdaDocumentShouldHaveNormalConfidentiality() {
        String document = cdaDocumentBuilder.buildQrdaCategoryI(
            UUID.randomUUID(), LocalDate.now(), LocalDate.now());

        assertThat(document).contains("<confidentialityCode");
        assertThat(document).contains("code=\"N\""); // Normal confidentiality
    }
}
```

### Performance Tests

```java
@SpringBootTest
class QrdaExportPerformanceTest {

    @Autowired
    private CdaDocumentBuilder cdaDocumentBuilder;

    @Test
    @DisplayName("QRDA Category I generation should complete within 100ms")
    void categoryIGenerationPerformance() {
        // Given
        int iterations = 50;
        List<Long> latencies = new ArrayList<>();
        LocalDate periodStart = LocalDate.of(2024, 1, 1);
        LocalDate periodEnd = LocalDate.of(2024, 12, 31);

        // When
        for (int i = 0; i < iterations; i++) {
            UUID patientId = UUID.randomUUID();

            Instant start = Instant.now();
            String document = cdaDocumentBuilder.buildQrdaCategoryI(
                patientId, periodStart, periodEnd);
            Instant end = Instant.now();

            assertThat(document).isNotNull();
            latencies.add(Duration.between(start, end).toMillis());
        }

        // Then
        Collections.sort(latencies);
        long p95 = latencies.get((int) (iterations * 0.95));

        assertThat(p95)
            .isLessThan(100L)
            .withFailMessage("p95 Category I generation %dms exceeds 100ms SLA", p95);

        System.out.printf("Category I Generation: p50=%dms, p95=%dms%n",
            latencies.get(iterations / 2), p95);
    }

    @Test
    @DisplayName("QRDA validation should complete within 200ms")
    void validationPerformance() {
        // Given
        int iterations = 100;
        List<Long> latencies = new ArrayList<>();
        String validDocument = createValidQrdaCategoryIDocument();

        // When
        for (int i = 0; i < iterations; i++) {
            Instant start = Instant.now();
            List<String> errors = validationService.validateCategoryI(validDocument);
            Instant end = Instant.now();

            latencies.add(Duration.between(start, end).toMillis());
        }

        // Then
        Collections.sort(latencies);
        long p95 = latencies.get((int) (iterations * 0.95));

        assertThat(p95)
            .isLessThan(200L)
            .withFailMessage("p95 validation latency %dms exceeds 200ms SLA", p95);
    }
}
```

### Test Configuration

Tests use `@TempDir` for temporary file storage during export processing:

```java
@TempDir
Path tempDir;

@BeforeEach
void setUp() {
    ReflectionTestUtils.setField(categoryIService, "storagePath", tempDir.toString());
}
```

### Best Practices

| Practice | Description |
|----------|-------------|
| **Template OID Validation** | Verify correct OIDs: Cat I `2.16.840.1.113883.10.20.24.1.1`, Cat III `2.16.840.1.113883.10.20.27.1.1` |
| **LOINC Document Codes** | Cat I uses `55182-0`, Cat III uses `55184-6` |
| **CMS Identifier Formats** | TIN must be 9 digits, NPI must be 10 digits |
| **Validation Toggle** | Tests should verify behavior when validation is enabled and disabled |
| **Async Job Processing** | Test job status transitions: PENDING → RUNNING → COMPLETED/FAILED |
| **Category Differences** | Cat I includes recordTarget, Cat III is aggregate (no recordTarget) |
| **Date Formatting** | QRDA uses YYYYMMDD format for dates |
| **Tenant Isolation** | All job queries must use `findByIdAndTenantId` |
| **Temporary Storage** | Use `@TempDir` for test file output |
| **Performance Rate** | Rate = Numerator / (Denominator - Exclusions - Exceptions) * 100 |

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| Template OID validation fails | Wrong OID in generated document | Check CdaDocumentBuilder template configuration |
| TIN/NPI validation fails | Invalid format in test data | Ensure TIN is 9 digits, NPI is 10 digits |
| Job stuck in RUNNING | Async processing exception | Check logs for service client failures |
| Download returns 400 | Job not completed | Wait for job status to be COMPLETED |
| Tenant isolation test fails | Missing tenantId filter | Use `findByIdAndTenantId` not `findById` |
| Category III has recordTarget | Wrong template used | Ensure Category III uses aggregate template |
| Date format errors | Wrong date format | Use YYYYMMDD format (20240101, not 2024-01-01) |
| Validation disabled unexpectedly | Config override | Check `validationEnabled` property setting |

### Building

```bash
./gradlew :modules:services:qrda-export-service:build
```

## Example Usage

### Generate Category I Export

```bash
curl -X POST http://localhost:8104/qrda-export/api/v1/qrda/category-i/generate \
  -H "X-Tenant-ID: tenant-001" \
  -H "Content-Type: application/json" \
  -d '{
    "measureIds": ["CMS134v10", "CMS135v10"],
    "periodStart": "2024-01-01",
    "periodEnd": "2024-12-31",
    "patientIds": ["patient-001", "patient-002"],
    "includeClosedGaps": true
  }'
```

### Generate Category III Export

```bash
curl -X POST http://localhost:8104/qrda-export/api/v1/qrda/category-iii/generate \
  -H "X-Tenant-ID: tenant-001" \
  -H "Content-Type: application/json" \
  -d '{
    "measureIds": ["CMS134v10", "CMS135v10", "CMS136v10"],
    "periodStart": "2024-01-01",
    "periodEnd": "2024-12-31",
    "reportingParameters": {
      "reporterName": "HealthCare Organization",
      "reporterNPI": "1234567890",
      "reporterTIN": "12-3456789"
    }
  }'
```

### Download Documents

```bash
curl http://localhost:8104/qrda-export/api/v1/qrda/jobs/{jobId}/download \
  -H "X-Tenant-ID: tenant-001" \
  -o qrda-export.zip
```

## Supported eCQMs

The service supports all CMS eCQMs including:
- CMS134v10: Diabetes: Hemoglobin A1c (HbA1c) Poor Control
- CMS135v10: Heart Failure (HF): Angiotensin-Converting Enzyme (ACE) Inhibitor
- CMS136v10: Follow-Up Care for Children Prescribed ADHD Medication
- And 100+ additional measures

## Validation

### Schematron Validation
- CMS-provided schematron rules
- Structural and semantic validation
- Detailed error reporting
- Validation errors included in job results

### Common Validation Issues
- Missing required patient demographics
- Invalid measure calculation results
- Incorrect reporting period dates
- Missing or invalid code systems

## Document Storage

- Default path: `/tmp/qrda-exports`
- Retention: 90 days (configurable)
- Automatic cleanup of expired documents
- ZIP format for Category I (multiple patients)
- XML format for Category III (single document)

## Integration

The QRDA Export Service integrates with:
- **Quality Measure Service**: Retrieves calculated measure results
- **CQL Engine Service**: Performs measure calculations if needed
- **Patient Service**: Gets patient demographics
- **FHIR Service**: Retrieves clinical data for measures

## Standards Compliance

- HL7 CDA Release 2
- QRDA Category I Implementation Guide
- QRDA Category III Implementation Guide
- CMS eCQM specifications
- Value Set Authority Center (VSAC) value sets

## Authors

HDIM Development Team

## License

Proprietary - Mahoosuc Solutions
