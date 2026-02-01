# Migration Workflow Service

Data migration job orchestration service for healthcare data migrations with progress tracking, error handling, and quality reporting.

## Purpose

The Migration Workflow Service orchestrates large-scale healthcare data migrations from legacy systems to the HDIM platform. It provides comprehensive job management, progress monitoring, error tracking, data quality reporting, and checkpoint recovery for reliable and auditable data migrations.

## Key Features

- **Job Orchestration**: Create, start, pause, resume, and cancel migration jobs
- **Multiple Source Types**: Support for FHIR, HL7v2, SFTP, MLLP, and database sources
- **Progress Tracking**: Real-time progress updates with records processed, success/failure counts
- **Checkpoint Recovery**: Automatic checkpointing every 500 records for fault tolerance
- **Error Management**: Detailed error capture with categorization and CSV export
- **Data Quality Reports**: Post-migration quality analysis with metrics and recommendations
- **Batch Processing**: Configurable batch sizes (default 100 records)
- **Concurrent Job Limits**: Maximum 3 concurrent migrations per tenant
- **CDR Integration**: Seamless integration with CDR Processor Service for HL7v2 parsing
- **Multi-tenant Support**: Complete tenant isolation and security

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/migrations` | Create new migration job |
| GET | `/api/v1/migrations` | List migration jobs with pagination |
| GET | `/api/v1/migrations/{jobId}` | Get migration job details |
| POST | `/api/v1/migrations/{jobId}/start` | Start a migration job |
| POST | `/api/v1/migrations/{jobId}/pause` | Pause a running migration job |
| POST | `/api/v1/migrations/{jobId}/resume` | Resume a paused migration job |
| POST | `/api/v1/migrations/{jobId}/cancel` | Cancel a migration job |
| DELETE | `/api/v1/migrations/{jobId}` | Delete a migration job |
| GET | `/api/v1/migrations/{jobId}/progress` | Get current progress |
| GET | `/api/v1/migrations/{jobId}/summary` | Get completion summary |
| GET | `/api/v1/migrations/{jobId}/errors` | Get errors with pagination |
| GET | `/api/v1/migrations/{jobId}/quality` | Get data quality report |
| GET | `/api/v1/migrations/{jobId}/quality/export` | Export quality report as CSV |
| GET | `/api/v1/migrations/{jobId}/errors/export` | Export errors as CSV |

## Configuration

### Application Properties

```yaml
server:
  port: 8103

migration:
  default-batch-size: 100
  checkpoint-interval: 500
  progress-update-interval: 1000
  max-concurrent-jobs: 3
  sftp-connection-timeout: 30000
  mllp-default-port: 2575

cdr-processor:
  url: http://localhost:8099
  connect-timeout: 10s
  read-timeout: 30s
```

### Database

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/healthdata
    username: healthdata
    password: ${DB_PASSWORD}
  liquibase:
    change-log: classpath:db/changelog/0013-create-migration-jobs.sql
```

### Kafka

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      properties:
        enable.idempotence: true
```

## Job Status Lifecycle

1. **CREATED**: Job created but not started
2. **RUNNING**: Job actively processing records
3. **PAUSED**: Job temporarily paused (can be resumed)
4. **COMPLETED**: Job completed successfully
5. **FAILED**: Job failed with errors
6. **CANCELLED**: Job manually cancelled

## Source Types

### FHIR
- Direct FHIR R4 API integration
- Supports Patient, Observation, Condition, etc.
- Pagination and batch retrieval

### HL7v2
- ADT, ORU, ORM message processing
- Integration with CDR Processor Service
- Batch message parsing

### SFTP
- File-based migrations
- Automatic retry with connection pooling
- Support for CSV, JSON, XML formats

### MLLP
- Real-time HL7v2 message streaming
- Configurable port and timeout
- Acknowledgment handling

### Database
- Direct database-to-database migration
- JDBC connection support
- Custom SQL query support

## Data Quality Metrics

The quality report includes:
- **Completeness**: Percentage of required fields populated
- **Accuracy**: Data validation pass rate
- **Consistency**: Cross-field consistency checks
- **Timeliness**: Migration performance metrics
- **Uniqueness**: Duplicate detection
- **Validity**: Format and range validation

## Error Categories

- **CONNECTION_ERROR**: Source system connectivity issues
- **PARSING_ERROR**: Message/document parsing failures
- **VALIDATION_ERROR**: Data validation failures
- **TRANSFORMATION_ERROR**: Data transformation issues
- **PERSISTENCE_ERROR**: Database write failures
- **UNKNOWN_ERROR**: Unclassified errors

## Running Locally

### Start the Service

```bash
./gradlew :modules:services:migration-workflow-service:bootRun
```

## Testing

The Migration Workflow Service has comprehensive test coverage across 17 test files with 98+ test methods covering unit tests, integration tests, source connector tests, WebSocket tests, multi-tenant isolation, RBAC, HIPAA compliance, and performance benchmarks.

### Quick Start

```bash
# Run all tests
./gradlew :modules:services:migration-workflow-service:test

# Run specific test class
./gradlew :modules:services:migration-workflow-service:test --tests "MigrationJobServiceTest"

# Run tests by category
./gradlew :modules:services:migration-workflow-service:test --tests "*ServiceTest"
./gradlew :modules:services:migration-workflow-service:test --tests "*ControllerTest"
./gradlew :modules:services:migration-workflow-service:test --tests "*ConnectorTest"

# Run with coverage report
./gradlew :modules:services:migration-workflow-service:test jacocoTestReport

# Run integration tests only (requires Docker)
./gradlew :modules:services:migration-workflow-service:integrationTest
```

### Test Coverage Summary

| Test Category | Test Files | Test Methods | Coverage Areas |
|--------------|------------|--------------|----------------|
| Service Layer | 3 | 62 | Job lifecycle, execution, data quality |
| API/Controller | 2 | 12 | REST endpoints, exception handling |
| Source Connectors | 4 | 18 | File, SFTP, MLLP, Factory |
| External Clients | 2 | 14 | CDR Processor, health checks |
| WebSocket | 2 | 8 | Progress updates, session management |
| DTOs/Entities | 3 | 14 | Computation, persistence, config |
| **Total** | **17** | **98+** | **Full service coverage** |

### Test Organization

```
src/test/java/com/healthdata/migration/
├── service/
│   ├── MigrationJobServiceTest.java         # Job lifecycle management (24 tests)
│   ├── MigrationExecutorServiceTest.java    # Async job execution (8 tests)
│   └── DataQualityServiceTest.java          # Quality analysis (30+ tests)
├── controller/
│   ├── MigrationJobControllerTest.java      # REST API endpoints
│   └── GlobalExceptionHandlerTest.java      # Error handling
├── connector/
│   ├── AbstractSourceConnectorTest.java     # Base connector tests
│   ├── FileSourceConnectorTest.java         # File-based sources
│   ├── SftpSourceConnectorTest.java         # SFTP connections
│   ├── MllpSourceConnectorTest.java         # MLLP streaming
│   └── SourceConnectorFactoryTest.java      # Factory pattern
├── client/
│   ├── CdrProcessorClientTest.java          # WebClient integration
│   └── CdrProcessingResultTest.java         # Result DTOs
├── websocket/
│   ├── MigrationWebSocketHandlerTest.java   # Session handling
│   └── MigrationProgressPublisherTest.java  # Progress broadcasting
├── dto/
│   └── MigrationDtoTest.java                # DTO computations
├── persistence/
│   └── MigrationEntityTest.java             # JPA entities
└── config/
    └── CdrProcessorClientConfigTest.java    # Bean configuration
```

---

### Unit Tests (Service Layer)

#### MigrationJobServiceTest - Job Lifecycle Management

Tests for migration job CRUD operations, lifecycle state transitions, progress tracking, checkpoint management, and quality reporting.

**Test Structure (24 tests across 7 nested classes):**

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Migration Job Service Tests")
class MigrationJobServiceTest {

    @Mock
    private MigrationJobRepository jobRepository;

    @Mock
    private MigrationCheckpointRepository checkpointRepository;

    @Mock
    private MigrationErrorRepository errorRepository;

    @Mock
    private DataQualityService dataQualityService;

    @InjectMocks
    private MigrationJobService jobService;

    private static final String TENANT_ID = "tenant-test-001";
    private static final UUID JOB_ID = UUID.randomUUID();

    @Nested
    @DisplayName("Create Job Tests")
    class CreateJobTests {

        @Test
        @DisplayName("Should create job with default settings")
        void shouldCreateJobWithDefaultSettings() {
            // Given
            CreateJobRequest request = CreateJobRequest.builder()
                .name("Epic Patient Migration")
                .sourceType(SourceType.FHIR)
                .sourceConfig(Map.of("fhirServerUrl", "https://fhir.epic.com"))
                .build();

            MigrationJobEntity savedEntity = createJobEntity(TENANT_ID, request.getName());
            when(jobRepository.save(any(MigrationJobEntity.class))).thenReturn(savedEntity);

            // When
            MigrationJobResponse result = jobService.createJob(request, TENANT_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Epic Patient Migration");
            assertThat(result.getStatus()).isEqualTo(JobStatus.CREATED);
            assertThat(result.getBatchSize()).isEqualTo(100); // Default

            verify(jobRepository).save(argThat(entity ->
                entity.getTenantId().equals(TENANT_ID) &&
                entity.getSourceType() == SourceType.FHIR
            ));
        }

        @Test
        @DisplayName("Should create job with custom batch size")
        void shouldCreateJobWithCustomBatchSize() {
            // Given
            CreateJobRequest request = CreateJobRequest.builder()
                .name("Custom Batch Migration")
                .sourceType(SourceType.HL7V2)
                .batchSize(250)
                .build();

            MigrationJobEntity savedEntity = createJobEntity(TENANT_ID, request.getName());
            savedEntity.setBatchSize(250);
            when(jobRepository.save(any())).thenReturn(savedEntity);

            // When
            MigrationJobResponse result = jobService.createJob(request, TENANT_ID);

            // Then
            assertThat(result.getBatchSize()).isEqualTo(250);
        }
    }

    @Nested
    @DisplayName("Get Job Tests")
    class GetJobTests {

        @Test
        @DisplayName("Should return job when exists for tenant")
        void shouldReturnJobWhenExistsForTenant() {
            // Given
            MigrationJobEntity entity = createJobEntity(TENANT_ID, "Test Job");
            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));

            // When
            MigrationJobResponse result = jobService.getJob(JOB_ID, TENANT_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(JOB_ID);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when job not found")
        void shouldThrowWhenJobNotFound() {
            // Given
            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> jobService.getJob(JOB_ID, TENANT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Migration job")
                .hasMessageContaining(JOB_ID.toString());
        }

        @Test
        @DisplayName("Should enforce tenant isolation in queries")
        void shouldEnforceTenantIsolation() {
            // Given
            String differentTenant = "tenant-different";
            when(jobRepository.findByIdAndTenantId(JOB_ID, differentTenant))
                .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> jobService.getJob(JOB_ID, differentTenant))
                .isInstanceOf(ResourceNotFoundException.class);

            // Verify the query included tenant filter
            verify(jobRepository).findByIdAndTenantId(JOB_ID, differentTenant);
            verify(jobRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("Job Lifecycle Operations")
    class JobLifecycleTests {

        @Test
        @DisplayName("Should start job when status is CREATED")
        void shouldStartJobWhenCreated() {
            // Given
            MigrationJobEntity entity = createJobEntity(TENANT_ID, "Test Job");
            entity.setStatus(JobStatus.CREATED);
            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));
            when(jobRepository.save(any())).thenReturn(entity);

            // When
            MigrationJobResponse result = jobService.startJob(JOB_ID, TENANT_ID);

            // Then
            assertThat(result.getStatus()).isEqualTo(JobStatus.RUNNING);
            verify(jobRepository).save(argThat(e -> e.getStatus() == JobStatus.RUNNING));
        }

        @Test
        @DisplayName("Should pause running job")
        void shouldPauseRunningJob() {
            // Given
            MigrationJobEntity entity = createJobEntity(TENANT_ID, "Test Job");
            entity.setStatus(JobStatus.RUNNING);
            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));
            when(jobRepository.save(any())).thenReturn(entity);

            // When
            MigrationJobResponse result = jobService.pauseJob(JOB_ID, TENANT_ID);

            // Then
            assertThat(result.getStatus()).isEqualTo(JobStatus.PAUSED);
        }

        @Test
        @DisplayName("Should resume paused job")
        void shouldResumePausedJob() {
            // Given
            MigrationJobEntity entity = createJobEntity(TENANT_ID, "Test Job");
            entity.setStatus(JobStatus.PAUSED);
            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));
            when(jobRepository.save(any())).thenReturn(entity);

            // When
            MigrationJobResponse result = jobService.resumeJob(JOB_ID, TENANT_ID);

            // Then
            assertThat(result.getStatus()).isEqualTo(JobStatus.RUNNING);
        }

        @Test
        @DisplayName("Should throw when starting non-CREATED job")
        void shouldThrowWhenStartingNonCreatedJob() {
            // Given
            MigrationJobEntity entity = createJobEntity(TENANT_ID, "Test Job");
            entity.setStatus(JobStatus.COMPLETED);
            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));

            // When/Then
            assertThatThrownBy(() -> jobService.startJob(JOB_ID, TENANT_ID))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Cannot start job");
        }
    }

    // Helper methods
    private MigrationJobEntity createJobEntity(String tenantId, String name) {
        return MigrationJobEntity.builder()
            .id(JOB_ID)
            .tenantId(tenantId)
            .name(name)
            .sourceType(SourceType.FHIR)
            .status(JobStatus.CREATED)
            .batchSize(100)
            .totalRecords(0L)
            .processedRecords(0L)
            .successCount(0L)
            .failureCount(0L)
            .createdAt(Instant.now())
            .build();
    }
}
```

#### MigrationExecutorServiceTest - Async Job Execution

Tests for asynchronous job execution, checkpoint recovery, error handling, and cancellation.

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Migration Executor Service Tests")
class MigrationExecutorServiceTest {

    @Mock
    private MigrationJobRepository jobRepository;

    @Mock
    private SourceConnectorFactory connectorFactory;

    @Mock
    private CdrProcessorClient cdrClient;

    @Mock
    private MigrationProgressPublisher progressPublisher;

    @InjectMocks
    private MigrationExecutorService executorService;

    @Test
    @DisplayName("Should execute job with checkpoint restoration")
    void shouldExecuteWithCheckpointRestoration() {
        // Given
        MigrationJobEntity job = createJob(JobStatus.PAUSED);
        MigrationCheckpoint checkpoint = MigrationCheckpoint.builder()
            .jobId(job.getId())
            .lastProcessedOffset(500L)
            .processedRecords(500L)
            .build();

        when(checkpointRepository.findLatestByJobId(job.getId()))
            .thenReturn(Optional.of(checkpoint));

        SourceConnector mockConnector = mock(SourceConnector.class);
        when(connectorFactory.createConnector(any())).thenReturn(mockConnector);
        when(mockConnector.readRecords(eq(500L), anyInt()))
            .thenReturn(List.of(createRecord("patient-501")));

        // When
        executorService.executeJob(job);

        // Then
        verify(mockConnector).readRecords(eq(500L), anyInt());
        verify(progressPublisher).publishProgress(any());
    }

    @Test
    @DisplayName("Should categorize errors correctly")
    void shouldCategorizeErrorsCorrectly() {
        // Given
        MigrationJobEntity job = createJob(JobStatus.RUNNING);
        job.setContinueOnError(true);

        SourceConnector mockConnector = mock(SourceConnector.class);
        when(connectorFactory.createConnector(any())).thenReturn(mockConnector);
        when(mockConnector.readRecords(anyLong(), anyInt()))
            .thenReturn(List.of(createInvalidRecord()));

        // When
        executorService.executeJob(job);

        // Then
        ArgumentCaptor<MigrationError> errorCaptor = ArgumentCaptor.forClass(MigrationError.class);
        verify(errorRepository).save(errorCaptor.capture());

        MigrationError error = errorCaptor.getValue();
        assertThat(error.getErrorCategory()).isIn(
            ErrorCategory.VALIDATION_ERROR,
            ErrorCategory.PARSING_ERROR,
            ErrorCategory.TRANSFORMATION_ERROR
        );
    }

    @Test
    @DisplayName("Should handle job cancellation gracefully")
    void shouldHandleJobCancellation() {
        // Given
        MigrationJobEntity job = createJob(JobStatus.RUNNING);
        AtomicBoolean cancelled = new AtomicBoolean(false);

        SourceConnector mockConnector = mock(SourceConnector.class);
        when(connectorFactory.createConnector(any())).thenReturn(mockConnector);
        when(mockConnector.readRecords(anyLong(), anyInt()))
            .thenAnswer(inv -> {
                cancelled.set(true);
                return Collections.emptyList();
            });

        // When
        CompletableFuture<Void> future = executorService.executeJobAsync(job);
        executorService.cancelJob(job.getId());

        // Then
        await().atMost(5, TimeUnit.SECONDS).until(() -> cancelled.get());
        verify(jobRepository).save(argThat(j -> j.getStatus() == JobStatus.CANCELLED));
    }
}
```

#### DataQualityServiceTest - Quality Analysis and Reporting

Tests for data quality analysis, report generation, error aggregation, and CSV export functionality.

**Test Structure (30+ tests across 7 nested classes):**

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Data Quality Service Tests")
class DataQualityServiceTest {

    @Mock
    private MigrationJobRepository jobRepository;

    @Mock
    private MigrationErrorRepository errorRepository;

    @InjectMocks
    private DataQualityService qualityService;

    @Nested
    @DisplayName("Report Generation Tests")
    class ReportGenerationTests {

        @Test
        @DisplayName("Should generate complete quality report")
        void shouldGenerateCompleteQualityReport() {
            // Given
            UUID jobId = UUID.randomUUID();
            MigrationJobEntity job = createCompletedJob(jobId, 1000L, 950L, 50L);
            when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

            List<MigrationError> errors = createErrorSamples(50);
            when(errorRepository.findByJobId(jobId)).thenReturn(errors);

            // When
            DataQualityReport report = qualityService.generateReport(jobId);

            // Then
            assertThat(report.getJobId()).isEqualTo(jobId);
            assertThat(report.getTotalRecords()).isEqualTo(1000L);
            assertThat(report.getSuccessRate()).isEqualTo(95.0);
            assertThat(report.getQualityScore()).isGreaterThanOrEqualTo(90);
            assertThat(report.getQualityGrade()).isEqualTo("A");
        }

        @Test
        @DisplayName("Should calculate error breakdown by category")
        void shouldCalculateErrorBreakdown() {
            // Given
            UUID jobId = UUID.randomUUID();
            MigrationJobEntity job = createCompletedJob(jobId, 100L, 80L, 20L);
            when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

            List<MigrationError> errors = List.of(
                createError(ErrorCategory.VALIDATION_ERROR, 10),
                createError(ErrorCategory.PARSING_ERROR, 5),
                createError(ErrorCategory.MAPPING_ERROR, 3),
                createError(ErrorCategory.DUPLICATE_RECORD, 2)
            );
            when(errorRepository.findByJobId(jobId)).thenReturn(errors);

            // When
            DataQualityReport report = qualityService.generateReport(jobId);

            // Then
            Map<ErrorCategory, Long> breakdown = report.getErrorBreakdown();
            assertThat(breakdown.get(ErrorCategory.VALIDATION_ERROR)).isEqualTo(10L);
            assertThat(breakdown.get(ErrorCategory.PARSING_ERROR)).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("Quality Metrics Tests")
    class QualityMetricsTests {

        @Test
        @DisplayName("Should assign grade A for 90%+ success rate")
        void shouldAssignGradeAForHighSuccessRate() {
            assertThat(qualityService.calculateGrade(95.0)).isEqualTo("A");
            assertThat(qualityService.calculateGrade(90.0)).isEqualTo("A");
        }

        @Test
        @DisplayName("Should assign grade F for below 60% success rate")
        void shouldAssignGradeFForLowSuccessRate() {
            assertThat(qualityService.calculateGrade(55.0)).isEqualTo("F");
            assertThat(qualityService.calculateGrade(0.0)).isEqualTo("F");
        }

        @Test
        @DisplayName("Should handle zero records gracefully")
        void shouldHandleZeroRecordsGracefully() {
            // Given
            UUID jobId = UUID.randomUUID();
            MigrationJobEntity job = createCompletedJob(jobId, 0L, 0L, 0L);
            when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

            // When
            DataQualityReport report = qualityService.generateReport(jobId);

            // Then
            assertThat(report.getSuccessRate()).isEqualTo(0.0);
            assertThat(report.getQualityGrade()).isEqualTo("N/A");
        }
    }

    @Nested
    @DisplayName("CSV Export Tests")
    class CsvExportTests {

        @Test
        @DisplayName("Should export errors to CSV format")
        void shouldExportErrorsToCsv() {
            // Given
            UUID jobId = UUID.randomUUID();
            List<MigrationError> errors = List.of(
                createDetailedError("REC-001", "Invalid date format", ErrorCategory.VALIDATION_ERROR),
                createDetailedError("REC-002", "Missing required field", ErrorCategory.VALIDATION_ERROR)
            );
            when(errorRepository.findByJobIdWithPagination(eq(jobId), any()))
                .thenReturn(new PageImpl<>(errors));

            // When
            String csv = qualityService.exportErrorsToCsv(jobId);

            // Then
            assertThat(csv).contains("Record ID,Error Category,Error Message,Timestamp");
            assertThat(csv).contains("REC-001,VALIDATION_ERROR,Invalid date format");
            assertThat(csv).contains("REC-002,VALIDATION_ERROR,Missing required field");
        }

        @Test
        @DisplayName("Should escape special characters in CSV")
        void shouldEscapeSpecialCharactersInCsv() {
            // Given
            UUID jobId = UUID.randomUUID();
            List<MigrationError> errors = List.of(
                createDetailedError("REC-001", "Error with \"quotes\" and, commas", ErrorCategory.PARSING_ERROR)
            );
            when(errorRepository.findByJobIdWithPagination(eq(jobId), any()))
                .thenReturn(new PageImpl<>(errors));

            // When
            String csv = qualityService.exportErrorsToCsv(jobId);

            // Then
            assertThat(csv).contains("\"Error with \"\"quotes\"\" and, commas\"");
        }
    }
}
```

---

### Integration Tests (API Endpoints)

#### MigrationJobControllerTest - REST API Testing

Tests for REST API endpoints using MockMvc with gateway trust authentication headers.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
class MigrationJobControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("migration_test")
        .withUsername("test")
        .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MigrationJobRepository jobRepository;

    private static final String TENANT_ID = "tenant-integration-001";
    private static final String BASE_URL = "/api/v1/migrations";

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create migration job with valid request")
    void shouldCreateMigrationJob() throws Exception {
        // Given
        CreateJobRequest request = CreateJobRequest.builder()
            .name("Integration Test Migration")
            .sourceType(SourceType.FHIR)
            .sourceConfig(Map.of(
                "fhirServerUrl", "https://fhir.test.com",
                "resourceType", "Patient"
            ))
            .batchSize(100)
            .build();

        // When/Then
        mockMvc.perform(post(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "user-001")
                .header("X-Auth-Roles", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("Integration Test Migration"))
            .andExpect(jsonPath("$.status").value("CREATED"))
            .andExpect(jsonPath("$.tenantId").value(TENANT_ID));

        // Verify persistence
        List<MigrationJobEntity> jobs = jobRepository.findByTenantId(TENANT_ID);
        assertThat(jobs).hasSize(1);
    }

    @Test
    @DisplayName("Should start job and verify status transition")
    void shouldStartJobAndVerifyStatusTransition() throws Exception {
        // Given
        MigrationJobEntity job = createAndSaveJob(TENANT_ID, "Test Job", JobStatus.CREATED);

        // When/Then
        mockMvc.perform(post(BASE_URL + "/{jobId}/start", job.getId())
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "user-001")
                .header("X-Auth-Roles", "ADMIN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RUNNING"));

        // Verify database state
        MigrationJobEntity updated = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(JobStatus.RUNNING);
    }

    @Test
    @DisplayName("Should return paginated job list with filters")
    void shouldReturnPaginatedJobListWithFilters() throws Exception {
        // Given
        createAndSaveJob(TENANT_ID, "Patient Migration", JobStatus.COMPLETED);
        createAndSaveJob(TENANT_ID, "Observation Migration", JobStatus.RUNNING);
        createAndSaveJob(TENANT_ID, "Condition Migration", JobStatus.CREATED);

        // When/Then - Filter by status
        mockMvc.perform(get(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "user-001")
                .header("X-Auth-Roles", "VIEWER")
                .param("status", "RUNNING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Observation Migration"));
    }

    @Test
    @DisplayName("Should export errors as CSV")
    void shouldExportErrorsAsCsv() throws Exception {
        // Given
        MigrationJobEntity job = createAndSaveJob(TENANT_ID, "Test Job", JobStatus.COMPLETED);
        saveErrors(job.getId(), 5);

        // When/Then
        mockMvc.perform(get(BASE_URL + "/{jobId}/errors/export", job.getId())
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "user-001")
                .header("X-Auth-Roles", "ANALYST"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "text/csv"))
            .andExpect(header().string("Content-Disposition",
                containsString("attachment; filename=")))
            .andExpect(content().string(containsString("Record ID,Error Category")));
    }
}
```

---

### Source Connector Tests

Tests for various source connector implementations (File, SFTP, MLLP).

#### FileSourceConnectorTest

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("File Source Connector Tests")
class FileSourceConnectorTest {

    private FileSourceConnector connector;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        connector = new FileSourceConnector();
    }

    @Test
    @DisplayName("Should read records from file with pattern matching")
    void shouldReadRecordsFromFileWithPatternMatching() throws Exception {
        // Given
        Path dataFile = tempDir.resolve("patients.json");
        Files.writeString(dataFile, """
            [
                {"id": "P001", "name": "Test Patient 1"},
                {"id": "P002", "name": "Test Patient 2"}
            ]
            """);

        SourceConfig config = SourceConfig.builder()
            .filePath(tempDir.toString())
            .filePattern("*.json")
            .build();

        connector.initialize(config);

        // When
        List<MigrationRecord> records = connector.readRecords(0, 10);

        // Then
        assertThat(records).hasSize(2);
        assertThat(records.get(0).getData()).contains("P001");
    }

    @Test
    @DisplayName("Should handle empty files gracefully")
    void shouldHandleEmptyFilesGracefully() throws Exception {
        // Given
        Path emptyFile = tempDir.resolve("empty.json");
        Files.createFile(emptyFile);

        SourceConfig config = SourceConfig.builder()
            .filePath(tempDir.toString())
            .filePattern("*.json")
            .build();

        connector.initialize(config);

        // When
        List<MigrationRecord> records = connector.readRecords(0, 10);

        // Then
        assertThat(records).isEmpty();
    }
}
```

#### SftpSourceConnectorTest

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("SFTP Source Connector Tests")
class SftpSourceConnectorTest {

    @Mock
    private SftpClient sftpClient;

    private SftpSourceConnector connector;

    @BeforeEach
    void setUp() {
        connector = new SftpSourceConnector(sftpClient);
    }

    @Test
    @DisplayName("Should connect with password authentication")
    void shouldConnectWithPasswordAuthentication() {
        // Given
        SourceConfig config = SourceConfig.builder()
            .host("sftp.example.com")
            .port(22)
            .username("testuser")
            .password("testpass")
            .remotePath("/data/exports")
            .build();

        // When
        connector.initialize(config);

        // Then
        verify(sftpClient).connect(
            eq("sftp.example.com"),
            eq(22),
            eq("testuser"),
            eq("testpass"),
            isNull()
        );
    }

    @Test
    @DisplayName("Should connect with private key authentication")
    void shouldConnectWithPrivateKeyAuthentication() {
        // Given
        SourceConfig config = SourceConfig.builder()
            .host("sftp.example.com")
            .port(22)
            .username("testuser")
            .privateKeyPath("/path/to/key.pem")
            .remotePath("/data/exports")
            .build();

        // When
        connector.initialize(config);

        // Then
        verify(sftpClient).connect(
            eq("sftp.example.com"),
            eq(22),
            eq("testuser"),
            isNull(),
            eq("/path/to/key.pem")
        );
    }

    @Test
    @DisplayName("Should handle connection timeout")
    void shouldHandleConnectionTimeout() {
        // Given
        SourceConfig config = SourceConfig.builder()
            .host("unreachable.example.com")
            .connectionTimeout(5000)
            .build();

        when(sftpClient.connect(any(), anyInt(), any(), any(), any()))
            .thenThrow(new ConnectionTimeoutException("Connection timed out"));

        // When/Then
        assertThatThrownBy(() -> connector.initialize(config))
            .isInstanceOf(SourceConnectionException.class)
            .hasMessageContaining("Failed to connect to SFTP");
    }
}
```

#### MllpSourceConnectorTest

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("MLLP Source Connector Tests")
class MllpSourceConnectorTest {

    @Mock
    private MllpServerSocket serverSocket;

    private MllpSourceConnector connector;

    @BeforeEach
    void setUp() {
        connector = new MllpSourceConnector(serverSocket);
    }

    @Test
    @DisplayName("Should start listening on configured port")
    void shouldStartListeningOnConfiguredPort() {
        // Given
        SourceConfig config = SourceConfig.builder()
            .port(2575)
            .useTls(false)
            .build();

        // When
        connector.initialize(config);

        // Then
        verify(serverSocket).bind(eq(2575), eq(false), isNull());
    }

    @Test
    @DisplayName("Should start with TLS enabled")
    void shouldStartWithTlsEnabled() {
        // Given
        SourceConfig config = SourceConfig.builder()
            .port(2576)
            .useTls(true)
            .tlsCertPath("/path/to/cert.pem")
            .tlsKeyPath("/path/to/key.pem")
            .build();

        // When
        connector.initialize(config);

        // Then
        verify(serverSocket).bind(eq(2576), eq(true), any(TlsConfig.class));
    }

    @Test
    @DisplayName("Should receive and parse HL7v2 messages")
    void shouldReceiveAndParseHl7v2Messages() {
        // Given
        String hl7Message = "MSH|^~\\&|EPIC|HOSPITAL|HDIM|HDIM|202312150800||ADT^A01|12345|P|2.5.1\r" +
                          "PID|1||12345^^^MRN||TEST^PATIENT||19800101|M";

        when(serverSocket.receiveMessage())
            .thenReturn(hl7Message)
            .thenReturn(null);

        connector.initialize(SourceConfig.builder().port(2575).build());

        // When
        List<MigrationRecord> records = connector.readRecords(0, 10);

        // Then
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getData()).contains("ADT^A01");
    }
}
```

---

### WebSocket Tests (Real-time Progress)

Tests for WebSocket session management and progress broadcasting.

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Migration WebSocket Handler Tests")
class MigrationWebSocketHandlerTest {

    @Mock
    private WebSocketSession session;

    private MigrationWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MigrationWebSocketHandler();
    }

    @Test
    @DisplayName("Should register session on connection")
    void shouldRegisterSessionOnConnection() throws Exception {
        // Given
        when(session.getId()).thenReturn("session-001");
        when(session.isOpen()).thenReturn(true);

        // When
        handler.afterConnectionEstablished(session);

        // Then
        assertThat(handler.getActiveSessions()).containsKey("session-001");
    }

    @Test
    @DisplayName("Should broadcast progress to all connected sessions")
    void shouldBroadcastProgressToAllSessions() throws Exception {
        // Given
        WebSocketSession session1 = mock(WebSocketSession.class);
        WebSocketSession session2 = mock(WebSocketSession.class);
        when(session1.getId()).thenReturn("session-001");
        when(session2.getId()).thenReturn("session-002");
        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);

        MigrationProgress progress = MigrationProgress.builder()
            .jobId(UUID.randomUUID())
            .processedRecords(500L)
            .totalRecords(1000L)
            .build();

        // When
        handler.broadcastProgress(progress);

        // Then
        verify(session1).sendMessage(any(TextMessage.class));
        verify(session2).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("Should remove closed sessions")
    void shouldRemoveClosedSessions() throws Exception {
        // Given
        when(session.getId()).thenReturn("session-001");
        handler.afterConnectionEstablished(session);

        // When
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        // Then
        assertThat(handler.getActiveSessions()).doesNotContainKey("session-001");
    }
}
```

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Migration Progress Publisher Tests")
class MigrationProgressPublisherTest {

    @Mock
    private MigrationWebSocketHandler webSocketHandler;

    @Mock
    private KafkaTemplate<String, MigrationProgressEvent> kafkaTemplate;

    private MigrationProgressPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new MigrationProgressPublisher(webSocketHandler, kafkaTemplate);
    }

    @Test
    @DisplayName("Should publish progress to WebSocket and Kafka")
    void shouldPublishProgressToWebSocketAndKafka() {
        // Given
        UUID jobId = UUID.randomUUID();
        MigrationProgress progress = MigrationProgress.builder()
            .jobId(jobId)
            .processedRecords(100L)
            .totalRecords(1000L)
            .successCount(95L)
            .failureCount(5L)
            .build();

        // When
        publisher.publishProgress(progress);

        // Then
        verify(webSocketHandler).broadcastProgress(progress);
        verify(kafkaTemplate).send(
            eq("migration.progress"),
            eq(jobId.toString()),
            any(MigrationProgressEvent.class)
        );
    }
}
```

---

### Multi-Tenant Isolation Tests

```java
@SpringBootTest
@Testcontainers
@DisplayName("Multi-Tenant Isolation Tests")
class MultiTenantIsolationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MigrationJobRepository jobRepository;

    @Autowired
    private MigrationJobService jobService;

    @Test
    @DisplayName("Should isolate jobs between tenants at database level")
    void shouldIsolateJobsBetweenTenants() {
        // Given
        String tenant1 = "tenant-001";
        String tenant2 = "tenant-002";

        MigrationJobEntity job1 = createAndSaveJob(tenant1, "Tenant 1 Job");
        MigrationJobEntity job2 = createAndSaveJob(tenant2, "Tenant 2 Job");
        MigrationJobEntity job3 = createAndSaveJob(tenant1, "Tenant 1 Job 2");

        // When
        List<MigrationJobEntity> tenant1Jobs = jobRepository.findByTenantId(tenant1);
        List<MigrationJobEntity> tenant2Jobs = jobRepository.findByTenantId(tenant2);

        // Then
        assertThat(tenant1Jobs)
            .hasSize(2)
            .extracting(MigrationJobEntity::getTenantId)
            .containsOnly(tenant1);

        assertThat(tenant2Jobs)
            .hasSize(1)
            .extracting(MigrationJobEntity::getTenantId)
            .containsOnly(tenant2);

        // Verify no cross-tenant data leakage
        assertThat(tenant1Jobs).doesNotContain(job2);
        assertThat(tenant2Jobs).doesNotContain(job1, job3);
    }

    @Test
    @DisplayName("Should prevent access to jobs from different tenant")
    void shouldPreventCrossTenantAccess() {
        // Given
        String tenant1 = "tenant-001";
        String tenant2 = "tenant-002";
        MigrationJobEntity job = createAndSaveJob(tenant1, "Tenant 1 Job");

        // When/Then - Attempt to access from different tenant
        assertThatThrownBy(() -> jobService.getJob(job.getId(), tenant2))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should enforce concurrent job limit per tenant")
    void shouldEnforceConcurrentJobLimitPerTenant() {
        // Given
        String tenantId = "tenant-limit-test";

        // Create 3 running jobs (max concurrent)
        for (int i = 0; i < 3; i++) {
            MigrationJobEntity job = createAndSaveJob(tenantId, "Job " + i);
            job.setStatus(JobStatus.RUNNING);
            jobRepository.save(job);
        }

        // When/Then - Attempt to start 4th job
        MigrationJobEntity job4 = createAndSaveJob(tenantId, "Job 4");
        assertThatThrownBy(() -> jobService.startJob(job4.getId(), tenantId))
            .isInstanceOf(ConcurrentJobLimitExceededException.class)
            .hasMessageContaining("Maximum concurrent jobs (3) exceeded");
    }
}
```

---

### RBAC/Permission Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DisplayName("RBAC Permission Tests")
class RoleBasedAccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MigrationJobRepository jobRepository;

    private static final String TENANT_ID = "tenant-rbac-001";

    @Test
    @DisplayName("Admin should be able to create and manage jobs")
    void adminCanCreateAndManageJobs() throws Exception {
        // Create job
        mockMvc.perform(post("/api/v1/migrations")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "admin-001")
                .header("X-Auth-Roles", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJobJson()))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Evaluator should be able to start but not delete jobs")
    void evaluatorCanStartButNotDeleteJobs() throws Exception {
        // Given
        MigrationJobEntity job = createAndSaveJob(TENANT_ID, "Test Job", JobStatus.CREATED);

        // Evaluator CAN start jobs
        mockMvc.perform(post("/api/v1/migrations/{id}/start", job.getId())
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "evaluator-001")
                .header("X-Auth-Roles", "EVALUATOR"))
            .andExpect(status().isOk());

        // Evaluator CANNOT delete jobs
        mockMvc.perform(delete("/api/v1/migrations/{id}", job.getId())
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "evaluator-001")
                .header("X-Auth-Roles", "EVALUATOR"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Viewer should have read-only access")
    void viewerHasReadOnlyAccess() throws Exception {
        // Given
        MigrationJobEntity job = createAndSaveJob(TENANT_ID, "Test Job", JobStatus.COMPLETED);

        // Viewer CAN read jobs
        mockMvc.perform(get("/api/v1/migrations/{id}", job.getId())
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "viewer-001")
                .header("X-Auth-Roles", "VIEWER"))
            .andExpect(status().isOk());

        // Viewer CAN view progress
        mockMvc.perform(get("/api/v1/migrations/{id}/progress", job.getId())
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "viewer-001")
                .header("X-Auth-Roles", "VIEWER"))
            .andExpect(status().isOk());

        // Viewer CANNOT create jobs
        mockMvc.perform(post("/api/v1/migrations")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "viewer-001")
                .header("X-Auth-Roles", "VIEWER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJobJson()))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Analyst should be able to view reports and export data")
    void analystCanViewReportsAndExport() throws Exception {
        // Given
        MigrationJobEntity job = createAndSaveJob(TENANT_ID, "Test Job", JobStatus.COMPLETED);

        // Analyst CAN view quality reports
        mockMvc.perform(get("/api/v1/migrations/{id}/quality", job.getId())
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "analyst-001")
                .header("X-Auth-Roles", "ANALYST"))
            .andExpect(status().isOk());

        // Analyst CAN export reports
        mockMvc.perform(get("/api/v1/migrations/{id}/quality/export", job.getId())
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "analyst-001")
                .header("X-Auth-Roles", "ANALYST"))
            .andExpect(status().isOk());
    }
}
```

---

### HIPAA Compliance Tests

```java
@SpringBootTest
@Testcontainers
@DisplayName("HIPAA Compliance Tests")
class HipaaComplianceTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    @DisplayName("Migration data cache TTL must not exceed 5 minutes")
    void migrationDataCacheTtlMustBeCompliant() {
        // Given
        Cache migrationCache = cacheManager.getCache("migrationJobs");

        // Then
        assertThat(migrationCache).isNotNull();

        if (migrationCache instanceof RedisCache) {
            RedisCacheConfiguration config = ((RedisCache) migrationCache).getCacheConfiguration();
            assertThat(config.getTtl().getSeconds())
                .isLessThanOrEqualTo(300)
                .withFailMessage("Migration cache TTL exceeds 5 minutes (HIPAA violation)");
        }
    }

    @Test
    @DisplayName("API responses must include no-cache headers")
    void apiResponsesMustIncludeNoCacheHeaders() throws Exception {
        // Given
        MigrationJobEntity job = createAndSaveJob("tenant-001", "Test Job", JobStatus.COMPLETED);

        // When/Then
        mockMvc.perform(get("/api/v1/migrations/{id}", job.getId())
                .header("X-Tenant-ID", "tenant-001")
                .header("X-Auth-User-Id", "user-001")
                .header("X-Auth-Roles", "VIEWER"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control",
                allOf(
                    containsString("no-store"),
                    containsString("no-cache"),
                    containsString("must-revalidate")
                )))
            .andExpect(header().string("Pragma", "no-cache"));
    }

    @Test
    @DisplayName("Job access must generate audit events")
    void jobAccessMustGenerateAuditEvents() {
        // Given
        UUID jobId = UUID.randomUUID();
        String userId = "user-audit-test";
        String tenantId = "tenant-audit-test";

        // When - Access job (should trigger @Audited annotation)
        MigrationJobEntity job = createAndSaveJob(tenantId, "Audit Test Job");
        jobService.getJob(job.getId(), tenantId);

        // Then - Verify audit event was created
        List<AuditEvent> auditEvents = auditEventRepository.findByResourceIdAndEventType(
            job.getId().toString(), "MIGRATION_JOB_ACCESS");

        assertThat(auditEvents)
            .isNotEmpty()
            .allMatch(event -> event.getTenantId().equals(tenantId))
            .allMatch(event -> event.getResourceType().equals("MigrationJob"));
    }

    @Test
    @DisplayName("Test data must use synthetic patterns - no real PHI")
    void testDataMustUseSyntheticPatterns() {
        // Given
        MigrationJobEntity testJob = createTestJob();

        // Then
        assertThat(testJob.getName())
            .matches("Test-.*|Integration-.*|Unit-.*")
            .withFailMessage("Test job names should be clearly synthetic");

        // Verify source configs don't contain real PHI
        SourceConfig config = testJob.getSourceConfig();
        if (config.getFhirServerUrl() != null) {
            assertThat(config.getFhirServerUrl())
                .matches("https?://(test|mock|example|localhost).*")
                .withFailMessage("Test FHIR URLs should not point to real systems");
        }
    }

    @Test
    @DisplayName("Error records must be sanitized before storage")
    void errorRecordsMustBeSanitizedBeforeStorage() {
        // Given
        MigrationError error = MigrationError.builder()
            .jobId(UUID.randomUUID())
            .recordId("TEST-REC-001")
            .errorCategory(ErrorCategory.VALIDATION_ERROR)
            .errorMessage("Invalid format")
            .rawData("{\"patientId\": \"TEST-P001\", \"name\": \"Test Patient\"}")
            .build();

        // When
        MigrationError savedError = errorRepository.save(error);

        // Then - Verify no real PHI identifiers in stored data
        assertThat(savedError.getRawData())
            .doesNotContainPattern("\\d{3}-\\d{2}-\\d{4}") // No SSN
            .doesNotContainPattern("\\d{10}") // No phone numbers in continuous form
            .withFailMessage("Error data must not contain real PHI identifiers");
    }
}
```

---

### Performance Tests

```java
@SpringBootTest
@Testcontainers
@DisplayName("Performance Tests")
class PerformanceTest {

    @Autowired
    private MigrationJobService jobService;

    @Autowired
    private MigrationExecutorService executorService;

    @Autowired
    private DataQualityService qualityService;

    @Test
    @DisplayName("Job creation should complete within 100ms")
    void jobCreationShouldBeUnder100ms() {
        // Given
        String tenantId = "tenant-perf-001";
        int iterations = 50;
        List<Long> latencies = new ArrayList<>();

        // When
        for (int i = 0; i < iterations; i++) {
            CreateJobRequest request = CreateJobRequest.builder()
                .name("Performance Test Job " + i)
                .sourceType(SourceType.FHIR)
                .sourceConfig(Map.of("fhirServerUrl", "https://test.fhir.com"))
                .build();

            Instant start = Instant.now();
            jobService.createJob(request, tenantId);
            Instant end = Instant.now();

            latencies.add(Duration.between(start, end).toMillis());
        }

        // Then
        Collections.sort(latencies);
        long p95 = latencies.get((int) (iterations * 0.95));

        assertThat(p95)
            .isLessThan(100L)
            .withFailMessage("p95 job creation latency %dms exceeds 100ms target", p95);

        System.out.printf("Job Creation Performance: p50=%dms, p95=%dms, p99=%dms%n",
            latencies.get(iterations / 2),
            p95,
            latencies.get((int) (iterations * 0.99)));
    }

    @Test
    @DisplayName("Batch processing should achieve 100+ records/second")
    void batchProcessingShouldAchieveTargetThroughput() {
        // Given
        int recordCount = 1000;
        MigrationJobEntity job = createJobWithMockSource(recordCount);

        // When
        Instant start = Instant.now();
        executorService.executeJob(job);
        Instant end = Instant.now();

        // Then
        long totalMs = Duration.between(start, end).toMillis();
        double recordsPerSecond = (recordCount * 1000.0) / totalMs;

        assertThat(recordsPerSecond)
            .isGreaterThanOrEqualTo(100.0)
            .withFailMessage("Throughput %.2f records/sec below 100 records/sec target",
                recordsPerSecond);

        System.out.printf("Batch Processing: %d records in %dms (%.2f records/sec)%n",
            recordCount, totalMs, recordsPerSecond);
    }

    @Test
    @DisplayName("Quality report generation should complete within 2 seconds")
    void qualityReportGenerationShouldBeUnder2Seconds() {
        // Given
        MigrationJobEntity job = createCompletedJobWithErrors(10000, 500);

        // When
        Instant start = Instant.now();
        DataQualityReport report = qualityService.generateReport(job.getId());
        Instant end = Instant.now();

        // Then
        long latencyMs = Duration.between(start, end).toMillis();
        assertThat(latencyMs)
            .isLessThan(2000L)
            .withFailMessage("Quality report generation took %dms, exceeds 2s target", latencyMs);

        assertThat(report).isNotNull();
        System.out.printf("Quality Report Generation: %dms for %d records with %d errors%n",
            latencyMs, 10000, 500);
    }

    @Test
    @DisplayName("CSV export should complete within 5 seconds for 10K errors")
    void csvExportShouldComplete Within5Seconds() {
        // Given
        MigrationJobEntity job = createJobWithErrors(10000);

        // When
        Instant start = Instant.now();
        String csv = qualityService.exportErrorsToCsv(job.getId());
        Instant end = Instant.now();

        // Then
        long latencyMs = Duration.between(start, end).toMillis();
        assertThat(latencyMs)
            .isLessThan(5000L)
            .withFailMessage("CSV export took %dms, exceeds 5s target for 10K errors", latencyMs);

        assertThat(csv).isNotEmpty();
        System.out.printf("CSV Export: %dms for 10K error records%n", latencyMs);
    }
}
```

---

### Test Configuration

#### BaseIntegrationTest Setup

```java
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("migration_test")
        .withUsername("test")
        .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Database
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Redis
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        // Migration settings
        registry.add("migration.max-concurrent-jobs", () -> 3);
        registry.add("migration.checkpoint-interval", () -> 100);
    }
}
```

#### Test Data Generators

```java
public class MigrationTestDataGenerator {

    /**
     * Creates synthetic migration job for testing.
     * Uses clearly synthetic names and configurations.
     */
    public static MigrationJobEntity createTestJob(String tenantId, String name) {
        return MigrationJobEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .name("Test-" + name + "-" + UUID.randomUUID().toString().substring(0, 8))
            .sourceType(SourceType.FHIR)
            .sourceConfig(SourceConfig.builder()
                .fhirServerUrl("https://test.fhir.example.com")
                .resourceType("Patient")
                .build())
            .status(JobStatus.CREATED)
            .batchSize(100)
            .totalRecords(0L)
            .processedRecords(0L)
            .successCount(0L)
            .failureCount(0L)
            .createdAt(Instant.now())
            .build();
    }

    /**
     * Creates synthetic error record for testing.
     * No real PHI in error data.
     */
    public static MigrationError createTestError(UUID jobId, ErrorCategory category) {
        return MigrationError.builder()
            .id(UUID.randomUUID())
            .jobId(jobId)
            .recordId("TEST-REC-" + UUID.randomUUID().toString().substring(0, 6))
            .errorCategory(category)
            .errorMessage("Test error: " + category.name())
            .rawData("{\"testId\": \"" + UUID.randomUUID() + "\"}")
            .timestamp(Instant.now())
            .build();
    }
}
```

---

### CI/CD Integration

```yaml
# .github/workflows/migration-service-tests.yml
name: Migration Workflow Service Tests

on:
  push:
    paths:
      - 'backend/modules/services/migration-workflow-service/**'
  pull_request:
    paths:
      - 'backend/modules/services/migration-workflow-service/**'

jobs:
  test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_DB: migration_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Cache Gradle packages
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}

      - name: Run Tests
        run: ./gradlew :modules:services:migration-workflow-service:test
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/migration_test
          SPRING_REDIS_HOST: localhost

      - name: Generate Coverage Report
        run: ./gradlew :modules:services:migration-workflow-service:jacocoTestReport

      - name: Upload Coverage
        uses: codecov/codecov-action@v4
        with:
          file: backend/modules/services/migration-workflow-service/build/reports/jacoco/test/jacocoTestReport.xml
          flags: migration-workflow-service
```

---

### Best Practices

#### Migration-Specific Testing Guidelines

1. **Job Lifecycle Testing**
   - Always test valid state transitions (CREATED→RUNNING→COMPLETED)
   - Test invalid transitions (COMPLETED→RUNNING should fail)
   - Verify status persistence after transitions

2. **Checkpoint/Recovery Testing**
   - Test checkpoint creation at configured intervals
   - Verify resume from last checkpoint
   - Test checkpoint data integrity

3. **Error Categorization**
   - Map exceptions to correct ErrorCategory
   - Test error message sanitization (no PHI in messages)
   - Verify error aggregation for reports

4. **Source Connector Testing**
   - Mock external connections (SFTP, MLLP)
   - Test authentication methods (password, key-based)
   - Test connection timeout handling

5. **Concurrent Job Limits**
   - Test tenant-specific job limits (max 3)
   - Verify limit enforcement across multiple requests
   - Test job completion releases slot

6. **HIPAA Compliance**
   - All test data must use synthetic patterns (TEST-*, Mock-*)
   - Verify cache TTL compliance (≤5 minutes)
   - Verify audit event generation for job access
   - Test no-cache header presence on API responses

#### Code Example: Testing Job State Machine

```java
@Test
@DisplayName("Should enforce valid state transitions")
void shouldEnforceValidStateTransitions() {
    // Valid: CREATED → RUNNING
    MigrationJobEntity job = createJob(JobStatus.CREATED);
    assertDoesNotThrow(() -> jobService.startJob(job.getId(), TENANT_ID));

    // Valid: RUNNING → PAUSED
    job.setStatus(JobStatus.RUNNING);
    assertDoesNotThrow(() -> jobService.pauseJob(job.getId(), TENANT_ID));

    // Valid: PAUSED → RUNNING
    job.setStatus(JobStatus.PAUSED);
    assertDoesNotThrow(() -> jobService.resumeJob(job.getId(), TENANT_ID));

    // Invalid: COMPLETED → RUNNING
    job.setStatus(JobStatus.COMPLETED);
    assertThatThrownBy(() -> jobService.startJob(job.getId(), TENANT_ID))
        .isInstanceOf(InvalidStateException.class);

    // Invalid: CANCELLED → any state
    job.setStatus(JobStatus.CANCELLED);
    assertThatThrownBy(() -> jobService.resumeJob(job.getId(), TENANT_ID))
        .isInstanceOf(InvalidStateException.class);
}
```

---

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| TestContainers timeout | Docker daemon not running | Start Docker Desktop; verify `docker ps` works |
| Connection refused to SFTP mock | Mock not initialized | Ensure `@BeforeEach` initializes mock connector |
| Job status not updating | Transaction not committed | Add `@Transactional` to test or use `saveAndFlush()` |
| Cache TTL test fails | Test cache configuration different from app | Check `application-test.yml` cache settings |
| WebSocket test NPE | Session mock not configured | Configure `when(session.isOpen()).thenReturn(true)` |
| Checkpoint not restored | Wrong offset comparison | Use `eq()` matcher for exact offset values |
| Concurrent job test fails | Jobs from previous tests | Add `@BeforeEach` cleanup with `jobRepository.deleteAll()` |
| CSV export test fails | Special characters not escaped | Verify `escapeForCsv()` method handles quotes, commas |
| Multi-tenant test leakage | Missing tenant filter in query | Verify repository uses `findByTenantId()` not `findAll()` |
| Performance test flaky | CI environment variance | Use percentiles (p95) not averages; increase tolerance |

#### Common Test Execution Issues

```bash
# If tests hang waiting for Kafka
./gradlew test -Dspring.kafka.enabled=false

# If TestContainers can't pull images
export TESTCONTAINERS_RYUK_DISABLED=true

# Run single test class
./gradlew test --tests "MigrationJobServiceTest"

# Run tests with debug output
./gradlew test --info --stacktrace

# Check test containers are starting
docker ps  # Should show postgres, redis containers during test
```

---

### Building

```bash
./gradlew :modules:services:migration-workflow-service:build
```

## Example Usage

### Create Migration Job

```bash
curl -X POST http://localhost:8103/api/v1/migrations \
  -H "X-Tenant-ID: tenant-001" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Epic Patient Migration",
    "sourceType": "FHIR",
    "sourceConfig": {
      "fhirServerUrl": "https://fhir.epic.com/R4",
      "resourceType": "Patient"
    },
    "batchSize": 100
  }'
```

### Get Job Progress

```bash
curl http://localhost:8103/api/v1/migrations/{jobId}/progress \
  -H "X-Tenant-ID: tenant-001"
```

### Export Quality Report

```bash
curl http://localhost:8103/api/v1/migrations/{jobId}/quality/export \
  -H "X-Tenant-ID: tenant-001" \
  -o quality-report.csv
```

## Integration

The Migration Workflow Service integrates with:
- **CDR Processor Service**: HL7v2 and CDA parsing
- **FHIR Service**: Target FHIR resource storage
- **Event Router**: Migration lifecycle event publishing
- **Audit Service**: Comprehensive audit logging

## Authors

HDIM Development Team

## License

Proprietary - Mahoosuc Solutions
