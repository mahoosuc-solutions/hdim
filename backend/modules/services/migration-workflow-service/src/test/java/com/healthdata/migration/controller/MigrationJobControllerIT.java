package com.healthdata.migration.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.migration.MigrationWorkflowApplication;
import com.healthdata.migration.config.TestCacheConfiguration;
import com.healthdata.migration.config.TestJpaConfiguration;
import com.healthdata.migration.config.TestSecurityConfiguration;
import com.healthdata.migration.dto.DataType;
import com.healthdata.migration.dto.JobStatus;
import com.healthdata.migration.dto.MigrationJobRequest;
import com.healthdata.migration.dto.SourceConfig;
import com.healthdata.migration.dto.SourceType;
import com.healthdata.migration.persistence.MigrationJobEntity;
import com.healthdata.migration.repository.MigrationJobRepository;

/**
 * Integration tests for MigrationJobController.
 * Tests the complete migration job lifecycle.
 */
@SpringBootTest(
    classes = {
        MigrationWorkflowApplication.class,
        TestSecurityConfiguration.class,
        TestCacheConfiguration.class,
        TestJpaConfiguration.class
    },
    properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "spring.flyway.enabled=false"
    }
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class MigrationJobControllerIT {

    private static final String H2_URL = "jdbc:tc:postgresql:15-alpine:///testdb";
    private static final String TENANT_ID = "test-tenant";
    private static final String CONTENT_TYPE = MediaType.APPLICATION_JSON_VALUE;

    @DynamicPropertySource
    static void overrideDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> H2_URL);
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.testcontainers.jdbc.ContainerDatabaseDriver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MigrationJobRepository jobRepository;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
    }

    @Test
    void shouldCreateMigrationJob() throws Exception {
        // Given
        MigrationJobRequest request = createJobRequest("Test Job");

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/migrations")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(CONTENT_TYPE)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jobName").value("Test Job"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.tenantId").value(TENANT_ID))
                .andReturn();

        // Then
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(response.has("id")).isTrue();
        assertThat(response.get("sourceType").asText()).isEqualTo("FILE");
    }

    @Test
    void shouldListMigrationJobs() throws Exception {
        // Given
        createTestJob("Job 1", JobStatus.PENDING);
        createTestJob("Job 2", JobStatus.RUNNING);
        createTestJob("Job 3", JobStatus.COMPLETED);

        // When / Then
        mockMvc.perform(get("/api/v1/migrations")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void shouldFilterJobsByStatus() throws Exception {
        // Given
        createTestJob("Running Job 1", JobStatus.RUNNING);
        createTestJob("Running Job 2", JobStatus.RUNNING);
        createTestJob("Completed Job", JobStatus.COMPLETED);

        // When / Then
        mockMvc.perform(get("/api/v1/migrations")
                .header("X-Tenant-ID", TENANT_ID)
                .param("status", "RUNNING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void shouldGetMigrationJobById() throws Exception {
        // Given
        MigrationJobEntity job = createTestJob("Get Test Job", JobStatus.PENDING);

        // When / Then
        mockMvc.perform(get("/api/v1/migrations/{jobId}", job.getId())
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(job.getId().toString()))
                .andExpect(jsonPath("$.jobName").value("Get Test Job"));
    }

    @Test
    void shouldReturn404ForNonExistentJob() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/v1/migrations/{jobId}", UUID.randomUUID())
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCancelMigrationJob() throws Exception {
        // Given
        MigrationJobEntity job = createTestJob("Cancel Test Job", JobStatus.RUNNING);

        // When / Then
        mockMvc.perform(post("/api/v1/migrations/{jobId}/cancel", job.getId())
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldPauseMigrationJob() throws Exception {
        // Given
        MigrationJobEntity job = createTestJob("Pause Test Job", JobStatus.RUNNING);

        // When / Then
        mockMvc.perform(post("/api/v1/migrations/{jobId}/pause", job.getId())
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));
    }

    @Test
    void shouldResumeMigrationJob() throws Exception {
        // Given
        MigrationJobEntity job = createTestJob("Resume Test Job", JobStatus.PAUSED);

        // When / Then
        mockMvc.perform(post("/api/v1/migrations/{jobId}/resume", job.getId())
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    @Test
    void shouldDeleteMigrationJob() throws Exception {
        // Given
        MigrationJobEntity job = createTestJob("Delete Test Job", JobStatus.PENDING);
        UUID jobId = job.getId();

        // When
        mockMvc.perform(delete("/api/v1/migrations/{jobId}", jobId)
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNoContent());

        // Then
        assertThat(jobRepository.findById(jobId)).isEmpty();
    }

    @Test
    void shouldGetJobProgress() throws Exception {
        // Given
        MigrationJobEntity job = createTestJob("Progress Test Job", JobStatus.RUNNING);
        job.setTotalRecords(1000L);
        job.setProcessedCount(500L);
        job.setSuccessCount(480L);
        job.setFailureCount(20L);
        jobRepository.save(job);

        // When / Then
        mockMvc.perform(get("/api/v1/migrations/{jobId}/progress", job.getId())
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").value(1000))
                .andExpect(jsonPath("$.processedCount").value(500))
                .andExpect(jsonPath("$.successCount").value(480))
                .andExpect(jsonPath("$.failureCount").value(20));
    }

    @Test
    void shouldGetJobSummary() throws Exception {
        // Given
        MigrationJobEntity job = createTestJob("Summary Test Job", JobStatus.COMPLETED);
        job.setTotalRecords(1000L);
        job.setProcessedCount(1000L);
        job.setSuccessCount(980L);
        job.setFailureCount(20L);
        jobRepository.save(job);

        // When / Then
        mockMvc.perform(get("/api/v1/migrations/{jobId}/summary", job.getId())
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").value(1000))
                .andExpect(jsonPath("$.successRate").exists());
    }

    @Test
    void shouldGetQualityReport() throws Exception {
        // Given
        MigrationJobEntity job = createTestJob("Quality Test Job", JobStatus.COMPLETED);
        job.setTotalRecords(100L);
        job.setSuccessCount(95L);
        job.setFailureCount(5L);
        jobRepository.save(job);

        // When / Then
        mockMvc.perform(get("/api/v1/migrations/{jobId}/quality", job.getId())
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(job.getId().toString()));
    }

    @Test
    void shouldEnforceTenantIsolation() throws Exception {
        // Given
        MigrationJobEntity job = createTestJob("Tenant 1 Job", JobStatus.PENDING);

        // When / Then - Different tenant should not see the job
        mockMvc.perform(get("/api/v1/migrations/{jobId}", job.getId())
                .header("X-Tenant-ID", "different-tenant"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRequireTenantHeader() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/v1/migrations"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldValidateJobRequest() throws Exception {
        // Given - Invalid request missing required fields
        String invalidRequest = "{\"description\": \"Missing required fields\"}";

        // When / Then
        mockMvc.perform(post("/api/v1/migrations")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(CONTENT_TYPE)
                .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    private MigrationJobRequest createJobRequest(String jobName) {
        SourceConfig sourceConfig = SourceConfig.builder()
                .sourceType(SourceType.FILE)
                .path("/data/hl7")
                .filePattern("*.hl7")
                .encoding("UTF-8")
                .recursive(true)
                .build();

        return MigrationJobRequest.builder()
                .jobName(jobName)
                .description("Test migration job")
                .sourceType(SourceType.FILE)
                .sourceConfig(sourceConfig)
                .dataType(DataType.HL7V2)
                .convertToFhir(true)
                .continueOnError(true)
                .batchSize(100)
                .resumable(true)
                .build();
    }

    private MigrationJobEntity createTestJob(String name, JobStatus status) {
        SourceConfig sourceConfig = SourceConfig.builder()
                .sourceType(SourceType.FILE)
                .path("/data/hl7")
                .filePattern("*.hl7")
                .encoding("UTF-8")
                .recursive(true)
                .build();

        MigrationJobEntity entity = MigrationJobEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .jobName(name)
                .status(status)
                .sourceType(SourceType.FILE)
                .sourceConfig(sourceConfig)
                .dataType(DataType.HL7V2)
                .convertToFhir(true)
                .continueOnError(true)
                .batchSize(100)
                .resumable(true)
                .totalRecords(0L)
                .processedCount(0L)
                .successCount(0L)
                .failureCount(0L)
                .retryCount(0)
                .maxRetries(3)
                .build();

        return jobRepository.save(entity);
    }
}
