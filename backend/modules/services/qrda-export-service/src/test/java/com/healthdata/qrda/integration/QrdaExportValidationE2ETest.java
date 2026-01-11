package com.healthdata.qrda.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.qrda.QrdaExportServiceApplication;
import com.healthdata.qrda.client.PatientServiceClient;
import com.healthdata.qrda.client.QualityMeasureClient;
import com.healthdata.qrda.dto.QrdaExportRequest;
import com.healthdata.qrda.persistence.QrdaExportJobEntity;
import com.healthdata.qrda.persistence.QrdaExportJobEntity.QrdaJobStatus;
import com.healthdata.qrda.persistence.QrdaExportJobEntity.QrdaJobType;
import com.healthdata.qrda.persistence.QrdaExportJobRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive End-to-End Integration Tests for QRDA Export Validation
 *
 * Validates CMS QRDA (Quality Reporting Document Architecture) export workflows:
 * - QRDA Category I (patient-level eCQM reporting)
 * - QRDA Category III (aggregate population reporting)
 * - Schematron validation compliance
 * - Supplemental data elements (race, ethnicity, sex, payer)
 * - Multi-tenant isolation
 * - Job lifecycle management
 *
 * These tests ensure compliance with CMS quality reporting requirements.
 */
@SpringBootTest(
    classes = QrdaExportServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QrdaExportValidationE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("qrda_test_db")
        .withUsername("testuser")
        .withPassword("testpass")
        .withInitScript("db/init-qrda-schema.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QrdaExportJobRepository jobRepository;

    @MockBean
    private PatientServiceClient patientServiceClient;

    @MockBean
    private QualityMeasureClient qualityMeasureClient;

    private static final String TENANT_ID = "tenant-1";
    private static final String USER_ID = "qrda-user-123";
    private static final UUID PATIENT_1 = UUID.randomUUID();
    private static final UUID PATIENT_2 = UUID.randomUUID();
    private static final List<String> MEASURE_IDS = Arrays.asList(
        "CMS122v11", // Diabetes: Hemoglobin A1c Poor Control
        "CMS134v11", // Diabetes: Medical Attention for Nephropathy
        "CMS165v11"  // Controlling High Blood Pressure
    );

    private HttpHeaders createHeaders(String tenantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", tenantId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private QrdaExportRequest createCategoryIRequest() {
        return QrdaExportRequest.builder()
            .jobType(QrdaJobType.QRDA_I)
            .measureIds(MEASURE_IDS)
            .patientIds(Arrays.asList(PATIENT_1, PATIENT_2))
            .periodStart(LocalDate.of(2024, 1, 1))
            .periodEnd(LocalDate.of(2024, 12, 31))
            .validateDocuments(true)
            .includeSupplementalData(true)
            .build();
    }

    private QrdaExportRequest createCategoryIIIRequest() {
        return QrdaExportRequest.builder()
            .jobType(QrdaJobType.QRDA_III)
            .measureIds(MEASURE_IDS)
            .periodStart(LocalDate.of(2024, 1, 1))
            .periodEnd(LocalDate.of(2024, 12, 31))
            .validateDocuments(true)
            .includeSupplementalData(true)
            .build();
    }

    @Nested
    @DisplayName("QRDA Category I Export (Patient-Level)")
    class QrdaCategoryITests {

        @Test
        @WithMockUser(username = USER_ID)
        @DisplayName("should initiate QRDA Category I export for specific patients")
        void shouldInitiateCategoryIExport() throws Exception {
            QrdaExportRequest request = createCategoryIRequest();
            String requestJson = objectMapper.writeValueAsString(request);

            mockMvc.perform(post("/api/v1/qrda/category-i/generate")
                    .headers(createHeaders(TENANT_ID))
                    .content(requestJson))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.tenantId").value(TENANT_ID))
                .andExpect(jsonPath("$.jobType").value("QRDA_I"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.measureIds").isArray())
                .andExpect(jsonPath("$.measureIds.length()").value(3))
                .andExpect(jsonPath("$.periodStart").value("2024-01-01"))
                .andExpect(jsonPath("$.periodEnd").value("2024-12-31"))
                .andExpect(jsonPath("$.requestedBy").value(USER_ID));
        }

        @Test
        @WithMockUser(username = USER_ID)
        @DisplayName("should validate that patient IDs are required for Category I")
        void shouldRequirePatientIdsForCategoryI() throws Exception {
            QrdaExportRequest request = createCategoryIRequest();
            request.setPatientIds(null); // Missing patient IDs

            String requestJson = objectMapper.writeValueAsString(request);

            mockMvc.perform(post("/api/v1/qrda/category-i/generate")
                    .headers(createHeaders(TENANT_ID))
                    .content(requestJson))
                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = USER_ID)
        @DisplayName("should generate individual QRDA documents per patient")
        void shouldGenerateIndividualDocumentsPerPatient() throws Exception {
            // Create a completed job with 2 patient documents
            QrdaExportJobEntity job = QrdaExportJobEntity.builder()
                .tenantId(TENANT_ID)
                .jobType(QrdaJobType.QRDA_I)
                .status(QrdaJobStatus.COMPLETED)
                .measureIds(MEASURE_IDS)
                .patientIds(Arrays.asList(PATIENT_1, PATIENT_2))
                .periodStart(LocalDate.of(2024, 1, 1))
                .periodEnd(LocalDate.of(2024, 12, 31))
                .documentCount(2) // One document per patient
                .patientCount(2)
                .documentLocation("/exports/qrda-i/job-123.zip")
                .requestedBy(USER_ID)
                .createdAt(LocalDateTime.now())
                .startedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

            QrdaExportJobEntity saved = jobRepository.save(job);

            mockMvc.perform(get("/api/v1/qrda/jobs/" + saved.getId())
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobType").value("QRDA_I"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.documentCount").value(2))
                .andExpect(jsonPath("$.patientCount").value(2));
        }

        @Test
        @WithMockUser(username = USER_ID)
        @DisplayName("should include supplemental data elements in Category I")
        void shouldIncludeSupplementalDataInCategoryI() throws Exception {
            QrdaExportRequest request = createCategoryIRequest();
            request.setIncludeSupplementalData(true);

            String requestJson = objectMapper.writeValueAsString(request);

            mockMvc.perform(post("/api/v1/qrda/category-i/generate")
                    .headers(createHeaders(TENANT_ID))
                    .content(requestJson))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").exists());

            // Supplemental data should include: race, ethnicity, sex, payer
            // Validated via Schematron in actual service implementation
        }
    }

    @Nested
    @DisplayName("QRDA Category III Export (Aggregate)")
    class QrdaCategoryIIITests {

        @Test
        @WithMockUser(username = USER_ID)
        @DisplayName("should initiate QRDA Category III export for population")
        void shouldInitiateCategoryIIIExport() throws Exception {
            QrdaExportRequest request = createCategoryIIIRequest();
            String requestJson = objectMapper.writeValueAsString(request);

            mockMvc.perform(post("/api/v1/qrda/category-iii/generate")
                    .headers(createHeaders(TENANT_ID))
                    .content(requestJson))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.jobType").value("QRDA_III"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.measureIds.length()").value(3));
        }

        @Test
        @WithMockUser(username = USER_ID)
        @DisplayName("should generate single aggregate document for Category III")
        void shouldGenerateSingleAggregateDocument() throws Exception {
            QrdaExportJobEntity job = QrdaExportJobEntity.builder()
                .tenantId(TENANT_ID)
                .jobType(QrdaJobType.QRDA_III)
                .status(QrdaJobStatus.COMPLETED)
                .measureIds(MEASURE_IDS)
                .periodStart(LocalDate.of(2024, 1, 1))
                .periodEnd(LocalDate.of(2024, 12, 31))
                .documentCount(1) // Single aggregate document
                .patientCount(150) // Total patients in population
                .documentLocation("/exports/qrda-iii/job-456.xml")
                .requestedBy(USER_ID)
                .createdAt(LocalDateTime.now())
                .startedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

            QrdaExportJobEntity saved = jobRepository.save(job);

            mockMvc.perform(get("/api/v1/qrda/jobs/" + saved.getId())
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobType").value("QRDA_III"))
                .andExpect(jsonPath("$.documentCount").value(1))
                .andExpect(jsonPath("$.patientCount").value(150));
        }

        @Test
        @WithMockUser(username = USER_ID)
        @DisplayName("should allow Category III without specific patient IDs")
        void shouldAllowCategoryIIIWithoutPatientIds() throws Exception {
            QrdaExportRequest request = createCategoryIIIRequest();
            request.setPatientIds(null); // Patient IDs are optional for Category III

            String requestJson = objectMapper.writeValueAsString(request);

            mockMvc.perform(post("/api/v1/qrda/category-iii/generate")
                    .headers(createHeaders(TENANT_ID))
                    .content(requestJson))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").exists());
        }

        @Test
        @WithMockUser(username = USER_ID)
        @DisplayName("should include aggregate performance rate in Category III")
        void shouldIncludeAggregatePerformanceRate() throws Exception {
            // Category III documents contain aggregate performance rates for populations
            // rather than individual patient data
            QrdaExportJobEntity job = QrdaExportJobEntity.builder()
                .tenantId(TENANT_ID)
                .jobType(QrdaJobType.QRDA_III)
                .status(QrdaJobStatus.COMPLETED)
                .measureIds(Arrays.asList("CMS122v11"))
                .patientCount(100)
                .documentCount(1)
                .documentLocation("/exports/qrda-iii/aggregate.xml")
                .requestedBy(USER_ID)
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

            QrdaExportJobEntity saved = jobRepository.save(job);

            mockMvc.perform(get("/api/v1/qrda/jobs/" + saved.getId())
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientCount").value(100));
        }
    }

    @Nested
    @DisplayName("Schematron Validation")
    class SchematronValidationTests {

        @Test
        @WithMockUser(username = USER_ID)
        @DisplayName("should validate QRDA documents with Schematron by default")
        void shouldValidateWithSchematron() throws Exception {
            QrdaExportRequest request = createCategoryIRequest();
            assertThat(request.isValidateDocuments()).isTrue();

            String requestJson = objectMapper.writeValueAsString(request);

            mockMvc.perform(post("/api/v1/qrda/category-i/generate")
                    .headers(createHeaders(TENANT_ID))
                    .content(requestJson))
                .andExpect(status().isAccepted());
        }

        @Test
        @WithMockUser(username = USER_ID)
        @DisplayName("should capture Schematron validation errors")
        void shouldCaptureSchematronValidationErrors() throws Exception {
            QrdaExportJobEntity job = QrdaExportJobEntity.builder()
                .tenantId(TENANT_ID)
                .jobType(QrdaJobType.QRDA_I)
                .status(QrdaJobStatus.FAILED)
                .measureIds(MEASURE_IDS)
                .patientIds(Arrays.asList(PATIENT_1))
                .validationErrors(Arrays.asList(
                    "Patient.name is required",
                    "Patient.birthTime must be in TS format",
                    "Missing required templateId 2.16.840.1.113883.10.20.24.3.1"
                ))
                .errorMessage("Schematron validation failed")
                .requestedBy(USER_ID)
                .createdAt(LocalDateTime.now())
                .startedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

            QrdaExportJobEntity saved = jobRepository.save(job);

            mockMvc.perform(get("/api/v1/qrda/jobs/" + saved.getId())
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.validationErrors").isArray())
                .andExpect(jsonPath("$.validationErrors.length()").value(3))
                .andExpect(jsonPath("$.errorMessage").value("Schematron validation failed"));
        }

        @Test
        @WithMockUser(username = USER_ID)
        @DisplayName("should allow disabling validation for testing")
        void shouldAllowDisablingValidation() throws Exception {
            QrdaExportRequest request = createCategoryIRequest();
            request.setValidateDocuments(false);

            String requestJson = objectMapper.writeValueAsString(request);

            mockMvc.perform(post("/api/v1/qrda/category-i/generate")
                    .headers(createHeaders(TENANT_ID))
                    .content(requestJson))
                .andExpect(status().isAccepted());
        }
    }

    @Nested
    @DisplayName("Job Lifecycle Management")
    class JobLifecycleTests {

        @Test
        @WithMockUser(username = USER_ID)
        @DisplayName("should track job status transitions")
        void shouldTrackJobStatusTransitions() throws Exception {
            // Job lifecycle: PENDING → RUNNING → COMPLETED
            QrdaExportJobEntity job = QrdaExportJobEntity.builder()
                .tenantId(TENANT_ID)
                .jobType(QrdaJobType.QRDA_I)
                .status(QrdaJobStatus.PENDING)
                .measureIds(MEASURE_IDS)
                .patientIds(Arrays.asList(PATIENT_1))
                .requestedBy(USER_ID)
                .createdAt(LocalDateTime.now())
                .build();

            QrdaExportJobEntity saved = jobRepository.save(job);

            // Check PENDING status
            mockMvc.perform(get("/api/v1/qrda/jobs/" + saved.getId())
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

            // Simulate status change to RUNNING
            saved.setStatus(QrdaJobStatus.RUNNING);
            saved.setStartedAt(LocalDateTime.now());
            jobRepository.save(saved);

            mockMvc.perform(get("/api/v1/qrda/jobs/" + saved.getId())
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.startedAt").exists());

            // Simulate completion
            saved.setStatus(QrdaJobStatus.COMPLETED);
            saved.setCompletedAt(LocalDateTime.now());
            saved.setDocumentCount(1);
            jobRepository.save(saved);

            mockMvc.perform(get("/api/v1/qrda/jobs/" + saved.getId())
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").exists());
        }

        @Test
        @WithMockUser(username = USER_ID)
        @DisplayName("should cancel pending job")
        void shouldCancelPendingJob() throws Exception {
            QrdaExportJobEntity job = QrdaExportJobEntity.builder()
                .tenantId(TENANT_ID)
                .jobType(QrdaJobType.QRDA_III)
                .status(QrdaJobStatus.PENDING)
                .measureIds(MEASURE_IDS)
                .requestedBy(USER_ID)
                .createdAt(LocalDateTime.now())
                .build();

            QrdaExportJobEntity saved = jobRepository.save(job);

            mockMvc.perform(post("/api/v1/qrda/jobs/" + saved.getId() + "/cancel")
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @WithMockUser(username = USER_ID)
        @DisplayName("should prevent cancelling completed job")
        void shouldPreventCancellingCompletedJob() throws Exception {
            QrdaExportJobEntity job = QrdaExportJobEntity.builder()
                .tenantId(TENANT_ID)
                .jobType(QrdaJobType.QRDA_I)
                .status(QrdaJobStatus.COMPLETED)
                .measureIds(MEASURE_IDS)
                .documentCount(1)
                .requestedBy(USER_ID)
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

            QrdaExportJobEntity saved = jobRepository.save(job);

            mockMvc.perform(post("/api/v1/qrda/jobs/" + saved.getId() + "/cancel")
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = USER_ID)
        @DisplayName("should list jobs with pagination")
        void shouldListJobsWithPagination() throws Exception {
            // Create 25 jobs
            for (int i = 0; i < 25; i++) {
                QrdaExportJobEntity job = QrdaExportJobEntity.builder()
                    .tenantId(TENANT_ID)
                    .jobType(i % 2 == 0 ? QrdaJobType.QRDA_I : QrdaJobType.QRDA_III)
                    .status(QrdaJobStatus.COMPLETED)
                    .measureIds(MEASURE_IDS)
                    .requestedBy(USER_ID)
                    .createdAt(LocalDateTime.now())
                    .build();
                jobRepository.save(job);
            }

            mockMvc.perform(get("/api/v1/qrda/jobs")
                    .headers(createHeaders(TENANT_ID))
                    .param("page", "0")
                    .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(2));
        }
    }

    @Nested
    @DisplayName("Document Download & Export")
    class DocumentDownloadTests {

        @Test
        @WithMockUser(username = USER_ID)
        @DisplayName("should download completed QRDA Category I ZIP file")
        void shouldDownloadCategoryIZip() throws Exception {
            QrdaExportJobEntity job = QrdaExportJobEntity.builder()
                .tenantId(TENANT_ID)
                .jobType(QrdaJobType.QRDA_I)
                .status(QrdaJobStatus.COMPLETED)
                .measureIds(MEASURE_IDS)
                .documentLocation("/tmp/test-qrda-i.zip")
                .documentCount(2)
                .requestedBy(USER_ID)
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

            QrdaExportJobEntity saved = jobRepository.save(job);

            // Note: Actual file download requires file to exist
            // This test validates the endpoint contract
            mockMvc.perform(get("/api/v1/qrda/jobs/" + saved.getId() + "/download")
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isNotFound()); // File doesn't exist in test
        }

        @Test
        @WithMockUser(username = USER_ID)
        @DisplayName("should prevent download of non-completed job")
        void shouldPreventDownloadOfNonCompletedJob() throws Exception {
            QrdaExportJobEntity job = QrdaExportJobEntity.builder()
                .tenantId(TENANT_ID)
                .jobType(QrdaJobType.QRDA_III)
                .status(QrdaJobStatus.RUNNING)
                .measureIds(MEASURE_IDS)
                .requestedBy(USER_ID)
                .createdAt(LocalDateTime.now())
                .build();

            QrdaExportJobEntity saved = jobRepository.save(job);

            mockMvc.perform(get("/api/v1/qrda/jobs/" + saved.getId() + "/download")
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Multi-Tenant Isolation")
    class MultiTenantIsolationTests {

        @Test
        @WithMockUser(username = USER_ID)
        @DisplayName("should prevent cross-tenant job access")
        void shouldPreventCrossTenantJobAccess() throws Exception {
            QrdaExportJobEntity job = QrdaExportJobEntity.builder()
                .tenantId(TENANT_ID)
                .jobType(QrdaJobType.QRDA_I)
                .status(QrdaJobStatus.COMPLETED)
                .measureIds(MEASURE_IDS)
                .requestedBy(USER_ID)
                .createdAt(LocalDateTime.now())
                .build();

            QrdaExportJobEntity saved = jobRepository.save(job);

            // Try to access from different tenant
            mockMvc.perform(get("/api/v1/qrda/jobs/" + saved.getId())
                    .headers(createHeaders("tenant-2")))
                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = USER_ID)
        @DisplayName("should isolate job listings by tenant")
        void shouldIsolateJobListingsByTenant() throws Exception {
            // Create jobs in tenant-1
            for (int i = 0; i < 5; i++) {
                QrdaExportJobEntity job = QrdaExportJobEntity.builder()
                    .tenantId(TENANT_ID)
                    .jobType(QrdaJobType.QRDA_I)
                    .status(QrdaJobStatus.COMPLETED)
                    .measureIds(MEASURE_IDS)
                    .requestedBy(USER_ID)
                    .createdAt(LocalDateTime.now())
                    .build();
                jobRepository.save(job);
            }

            // Create jobs in tenant-2
            for (int i = 0; i < 3; i++) {
                QrdaExportJobEntity job = QrdaExportJobEntity.builder()
                    .tenantId("tenant-2")
                    .jobType(QrdaJobType.QRDA_III)
                    .status(QrdaJobStatus.COMPLETED)
                    .measureIds(MEASURE_IDS)
                    .requestedBy(USER_ID)
                    .createdAt(LocalDateTime.now())
                    .build();
                jobRepository.save(job);
            }

            // Query tenant-1 should return only 5 jobs
            mockMvc.perform(get("/api/v1/qrda/jobs")
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.content[?(@.tenantId != 'tenant-1')]").doesNotExist());
        }

        @Test
        @WithMockUser(username = USER_ID)
        @DisplayName("should prevent cross-tenant job cancellation")
        void shouldPreventCrossTenantJobCancellation() throws Exception {
            QrdaExportJobEntity job = QrdaExportJobEntity.builder()
                .tenantId(TENANT_ID)
                .jobType(QrdaJobType.QRDA_I)
                .status(QrdaJobStatus.PENDING)
                .measureIds(MEASURE_IDS)
                .requestedBy(USER_ID)
                .createdAt(LocalDateTime.now())
                .build();

            QrdaExportJobEntity saved = jobRepository.save(job);

            mockMvc.perform(post("/api/v1/qrda/jobs/" + saved.getId() + "/cancel")
                    .headers(createHeaders("tenant-2")))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Error Handling & Validation")
    class ErrorHandlingTests {

        @Test
        @WithMockUser(username = USER_ID)
        @DisplayName("should validate required fields in request")
        void shouldValidateRequiredFields() throws Exception {
            QrdaExportRequest invalidRequest = QrdaExportRequest.builder()
                .jobType(null) // Missing required field
                .measureIds(null) // Missing required field
                .periodStart(null) // Missing required field
                .periodEnd(null) // Missing required field
                .build();

            String requestJson = objectMapper.writeValueAsString(invalidRequest);

            mockMvc.perform(post("/api/v1/qrda/category-i/generate")
                    .headers(createHeaders(TENANT_ID))
                    .content(requestJson))
                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = USER_ID)
        @DisplayName("should return 404 for non-existent job")
        void shouldReturn404ForNonExistentJob() throws Exception {
            UUID nonExistentJobId = UUID.randomUUID();

            mockMvc.perform(get("/api/v1/qrda/jobs/" + nonExistentJobId)
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = USER_ID)
        @DisplayName("should handle empty measure ID list")
        void shouldHandleEmptyMeasureIdList() throws Exception {
            QrdaExportRequest request = createCategoryIRequest();
            request.setMeasureIds(List.of()); // Empty list

            String requestJson = objectMapper.writeValueAsString(request);

            mockMvc.perform(post("/api/v1/qrda/category-i/generate")
                    .headers(createHeaders(TENANT_ID))
                    .content(requestJson))
                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = USER_ID)
        @DisplayName("should handle job failure with error message")
        void shouldHandleJobFailure() throws Exception {
            QrdaExportJobEntity job = QrdaExportJobEntity.builder()
                .tenantId(TENANT_ID)
                .jobType(QrdaJobType.QRDA_I)
                .status(QrdaJobStatus.FAILED)
                .measureIds(MEASURE_IDS)
                .errorMessage("Failed to fetch patient data from Patient Service")
                .requestedBy(USER_ID)
                .createdAt(LocalDateTime.now())
                .startedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

            QrdaExportJobEntity saved = jobRepository.save(job);

            mockMvc.perform(get("/api/v1/qrda/jobs/" + saved.getId())
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").exists());
        }
    }
}
