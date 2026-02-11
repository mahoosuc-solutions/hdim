package com.healthdata.quality.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.authentication.config.AuthenticationAutoConfiguration;
import com.healthdata.quality.client.CqlEngineServiceClient;
import com.healthdata.quality.persistence.JobExecutionEntity;
import com.healthdata.quality.persistence.QualityMeasureResultEntity;
import com.healthdata.quality.persistence.JobExecutionRepository;
import com.healthdata.quality.persistence.QualityMeasureResultRepository;
import com.healthdata.testfixtures.security.GatewayTrustTestHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-End Functional Tests for Population Batch Quality Measure Calculation.
 *
 * Tests the complete asynchronous batch calculation workflow including:
 * - Batch job creation and submission
 * - Progress tracking and status updates
 * - Async processing with CompletableFuture
 * - Error handling and recovery
 * - Job cancellation
 * - Performance optimization
 * - Resource cleanup
 *
 * FUNCTIONAL TEST COVERAGE:
 * - Population-wide quality measure evaluation
 * - Batch job management and monitoring
 * - Asynchronous processing patterns
 * - Error recovery and resilience
 * - Performance at scale
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {AuthenticationAutoConfiguration.class})
@ActiveProfiles("test")
@Testcontainers
@Transactional
@DisplayName("Population Batch Calculation E2E Tests")
@Tag("e2e")
@Tag("heavyweight")
class PopulationBatchCalculationE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QualityMeasureResultRepository measureResultRepository;

    @Autowired
    private JobExecutionRepository jobExecutionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CqlEngineServiceClient cqlEngineServiceClient;

    @MockBean
    private RestTemplate restTemplate;

    private static final String TENANT_ID = "test-tenant-batch";

    @BeforeEach
    void setUp() {
        measureResultRepository.deleteAll();
        jobExecutionRepository.deleteAll();
        reset(cqlEngineServiceClient);
        reset(restTemplate);

        // Mock FHIR server patient fetch - return 3 test patients
        UUID patient1 = UUID.randomUUID();
        UUID patient2 = UUID.randomUUID();
        UUID patient3 = UUID.randomUUID();

        Map<String, Object> mockFhirBundle = Map.of(
            "resourceType", "Bundle",
            "type", "searchset",
            "entry", List.of(
                Map.of("resource", Map.of("resourceType", "Patient", "id", patient1.toString())),
                Map.of("resource", Map.of("resourceType", "Patient", "id", patient2.toString())),
                Map.of("resource", Map.of("resourceType", "Patient", "id", patient3.toString()))
            )
        );

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
            .thenReturn(mockFhirBundle);
    }

    @Nested
    @DisplayName("Batch Job Creation and Submission")
    class BatchJobCreationAndSubmission {

        @Test
        @DisplayName("should create and submit population batch calculation job")
        void shouldCreateAndSubmitBatchJob() throws Exception {
            // Mock CQL Engine responses
            when(cqlEngineServiceClient.evaluateCql(
                anyString(),
                anyString(),
                any(UUID.class),
                anyString()
            )).thenReturn("""
                {
                    "libraryName": "HEDIS_CDC_2024",
                    "measureResult": {
                        "measureName": "Diabetes Care",
                        "inNumerator": true,
                        "inDenominator": true,
                        "complianceRate": 90.0,
                        "score": 95.0
                    }
                }
                """);

            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            // Submit batch calculation job
            var response = mockMvc.perform(post("/quality-measure/population/calculate")
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "measureIds": ["HEDIS_CDC_A1C9", "HEDIS_CBP"],
                            "patientFilter": {
                                "ageRange": {"min": 18, "max": 75},
                                "conditions": ["Diabetes"]
                            }
                        }
                        """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").exists())
                .andExpect(jsonPath("$.status").value("STARTING"))
                .andExpect(jsonPath("$.totalPatients").exists())
                .andReturn();

            String jobId = objectMapper.readTree(response.getResponse().getContentAsString())
                .get("jobId").asText();

            // Verify job entity created
            var job = jobExecutionRepository.findById(UUID.fromString(jobId));
            assertThat(job).isPresent();
            assertThat(job.get().getTenantId()).isEqualTo(TENANT_ID);
            assertThat(job.get().getStatus()).isIn("STARTING", "CALCULATING");
        }

        @Test
        @DisplayName("should return job ID immediately for async processing")
        void shouldReturnJobIdImmediately() throws Exception {
            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            long startTime = System.currentTimeMillis();

            mockMvc.perform(post("/quality-measure/population/calculate")
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "measureIds": ["HEDIS_CDC_A1C9"]
                        }
                        """))
                .andExpect(status().isAccepted());

            long duration = System.currentTimeMillis() - startTime;

            // Should return immediately (< 1 second) even for large populations
            assertThat(duration).isLessThan(1000);
        }
    }

    @Nested
    @DisplayName("Job Progress Tracking")
    class JobProgressTracking {

        @Test
        @DisplayName("should track job progress through status updates")
        void shouldTrackJobProgress() throws Exception {
            when(cqlEngineServiceClient.evaluateCql(
                anyString(),
                anyString(),
                any(UUID.class),
                anyString()
            )).thenReturn("""
                {
                    "libraryName": "HEDIS_CDC_2024",
                    "measureResult": {
                        "measureName": "Diabetes Care",
                        "inNumerator": true,
                        "inDenominator": true,
                        "complianceRate": 90.0,
                        "score": 95.0
                    }
                }
                """);

            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            // Submit job
            var submitResponse = mockMvc.perform(post("/quality-measure/population/calculate")
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "measureIds": ["HEDIS_CDC_A1C9"]
                        }
                        """))
                .andReturn();

            String jobId = objectMapper.readTree(submitResponse.getResponse().getContentAsString())
                .get("jobId").asText();

            // Poll job status
            await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    mockMvc.perform(get("/quality-measure/population/jobs/" + jobId)
                            .headers(headers))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value(
                            anyOf(is("CALCULATING"), is("COMPLETED"))
                        ));
                });

            // Eventually should complete
            await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    mockMvc.perform(get("/quality-measure/population/jobs/" + jobId)
                            .headers(headers))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("COMPLETED"))
                        .andExpect(jsonPath("$.completedCalculations").value(greaterThan(0)))
                        .andExpect(jsonPath("$.progressPercent").value(100));
                });
        }

        @Test
        @DisplayName("should provide detailed progress metrics")
        void shouldProvideDetailedProgressMetrics() throws Exception {
            when(cqlEngineServiceClient.evaluateCql(
                anyString(),
                anyString(),
                any(UUID.class),
                anyString()
            )).thenReturn("""
                {
                    "libraryName": "HEDIS_CDC_2024",
                    "measureResult": {
                        "measureName": "Diabetes Care",
                        "inNumerator": true,
                        "inDenominator": true,
                        "complianceRate": 90.0,
                        "score": 95.0
                    }
                }
                """);

            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            var submitResponse = mockMvc.perform(post("/quality-measure/population/calculate")
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "measureIds": ["HEDIS_CDC_A1C9"]
                        }
                        """))
                .andReturn();

            String jobId = objectMapper.readTree(submitResponse.getResponse().getContentAsString())
                .get("jobId").asText();

            // Wait for job to start calculating
            await().atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    var statusResponse = mockMvc.perform(get("/quality-measure/population/jobs/" + jobId)
                            .headers(headers))
                        .andExpect(status().isOk())
                        .andReturn();

                    String json = statusResponse.getResponse().getContentAsString();
                    var status = objectMapper.readTree(json).get("status").asText();
                    assertThat(status).isIn("CALCULATING", "COMPLETED");
                });

            // Check progress metrics
            mockMvc.perform(get("/quality-measure/population/jobs/" + jobId)
                    .headers(headers))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPatients").exists())
                .andExpect(jsonPath("$.completedCalculations").exists())
                .andExpect(jsonPath("$.successfulCalculations").exists())
                .andExpect(jsonPath("$.failedCalculations").exists())
                .andExpect(jsonPath("$.progressPercent").exists())
                .andExpect(jsonPath("$.estimatedTimeRemaining").exists());
        }
    }

    @Nested
    @DisplayName("Error Handling and Recovery")
    class ErrorHandlingAndRecovery {

        @Test
        @DisplayName("should handle CQL Engine failures gracefully")
        void shouldHandleCqlEngineFailures() throws Exception {
            // Simulate CQL Engine intermittent failures
            when(cqlEngineServiceClient.evaluateCql(
                anyString(),
                anyString(),
                any(UUID.class),
                anyString()
            ))
                .thenThrow(new RuntimeException("CQL Engine temporarily unavailable"))
                .thenThrow(new RuntimeException("CQL Engine temporarily unavailable"))
                .thenReturn("""
                    {
                        "libraryName": "HEDIS_CDC_2024",
                        "measureResult": {
                            "measureName": "Diabetes Care",
                            "inNumerator": true,
                            "inDenominator": true,
                            "complianceRate": 90.0,
                            "score": 95.0
                        }
                    }
                    """);

            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            var submitResponse = mockMvc.perform(post("/quality-measure/population/calculate")
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "measureIds": ["HEDIS_CDC_A1C9"]
                        }
                        """))
                .andReturn();

            String jobId = objectMapper.readTree(submitResponse.getResponse().getContentAsString())
                .get("jobId").asText();

            // Job should eventually complete despite some failures
            await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    mockMvc.perform(get("/quality-measure/population/jobs/" + jobId)
                            .headers(headers))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value(anyOf(is("COMPLETED"), is("FAILED"))))
                        .andExpect(jsonPath("$.failedCalculations").value(greaterThanOrEqualTo(0)));
                });
        }

        @Test
        @DisplayName("should mark job as FAILED if all calculations fail")
        void shouldMarkJobAsFailedIfAllCalculationsFail() throws Exception {
            when(cqlEngineServiceClient.evaluateCql(
                anyString(),
                anyString(),
                any(UUID.class),
                anyString()
            )).thenThrow(new RuntimeException("CQL Engine down"));

            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            var submitResponse = mockMvc.perform(post("/quality-measure/population/calculate")
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "measureIds": ["HEDIS_CDC_A1C9"]
                        }
                        """))
                .andReturn();

            String jobId = objectMapper.readTree(submitResponse.getResponse().getContentAsString())
                .get("jobId").asText();

            await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    mockMvc.perform(get("/quality-measure/population/jobs/" + jobId)
                            .headers(headers))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("FAILED"))
                        .andExpect(jsonPath("$.errorMessage").exists());
                });
        }
    }

    @Nested
    @DisplayName("Job Cancellation")
    class JobCancellation {

        @Test
        @DisplayName("should cancel running batch job")
        void shouldCancelRunningBatchJob() throws Exception {
            when(cqlEngineServiceClient.evaluateCql(
                anyString(),
                anyString(),
                any(UUID.class),
                anyString()
            )).thenAnswer(invocation -> {
                Thread.sleep(800); // Simulate slow processing (increased to prevent race condition)
                return """
                    {
                        "libraryName": "HEDIS_CDC_2024",
                        "measureResult": {
                            "measureName": "Diabetes Care",
                            "inNumerator": true,
                            "inDenominator": true,
                            "complianceRate": 90.0,
                            "score": 95.0
                        }
                    }
                    """;
            });

            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            var submitResponse = mockMvc.perform(post("/quality-measure/population/calculate")
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "measureIds": ["HEDIS_CDC_A1C9"]
                        }
                        """))
                .andReturn();

            String jobId = objectMapper.readTree(submitResponse.getResponse().getContentAsString())
                .get("jobId").asText();

            // Wait for job to start processing (but not complete)
            // With 800ms per patient * 3 patients = 2.4s total
            // Cancel after 500ms to catch it mid-execution
            Thread.sleep(500);

            // Cancel job while it's still running
            mockMvc.perform(put("/quality-measure/population/jobs/" + jobId + "/cancel")
                    .headers(headers))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

            // Verify job status
            mockMvc.perform(get("/quality-measure/population/jobs/" + jobId)
                    .headers(headers))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        }
    }

    @Nested
    @DisplayName("Performance and Scalability")
    class PerformanceAndScalability {

        @Test
        @DisplayName("should handle large populations efficiently")
        void shouldHandleLargePopulationsEfficiently() throws Exception {
            when(cqlEngineServiceClient.evaluateCql(
                anyString(),
                anyString(),
                any(UUID.class),
                anyString()
            )).thenReturn("""
                {
                    "libraryName": "HEDIS_CDC_2024",
                    "measureResult": {
                        "measureName": "Diabetes Care",
                        "inNumerator": true,
                        "inDenominator": true,
                        "complianceRate": 90.0,
                        "score": 95.0
                    }
                }
                """);

            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            // Submit job for large population (simulated)
            var submitResponse = mockMvc.perform(post("/quality-measure/population/calculate")
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "measureIds": ["HEDIS_CDC_A1C9", "HEDIS_CBP", "HEDIS_BCS"],
                            "maxConcurrency": 10
                        }
                        """))
                .andExpect(status().isAccepted())
                .andReturn();

            String jobId = objectMapper.readTree(submitResponse.getResponse().getContentAsString())
                .get("jobId").asText();

            // Job should use concurrent processing
            await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    mockMvc.perform(get("/quality-measure/population/jobs/" + jobId)
                            .headers(headers))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value(anyOf(is("CALCULATING"), is("COMPLETED"))));
                });
        }
    }

    @Nested
    @DisplayName("Multi-Tenant Isolation")
    class MultiTenantIsolation {

        @Test
        @DisplayName("should isolate batch jobs by tenant")
        void shouldIsolateBatchJobsByTenant() throws Exception {
            String tenant1 = "tenant-batch-001";
            String tenant2 = "tenant-batch-002";

            when(cqlEngineServiceClient.evaluateCql(
                anyString(),
                anyString(),
                any(UUID.class),
                anyString()
            )).thenReturn("""
                {
                    "libraryName": "HEDIS_CDC_2024",
                    "measureResult": {
                        "measureName": "Diabetes Care",
                        "inNumerator": true,
                        "inDenominator": true,
                        "complianceRate": 90.0,
                        "score": 95.0
                    }
                }
                """);

            // Submit job for tenant 1
            var headers1 = GatewayTrustTestHeaders.adminHeaders(tenant1);
            var submitResponse1 = mockMvc.perform(post("/quality-measure/population/calculate")
                    .headers(headers1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "measureIds": ["HEDIS_CDC_A1C9"]
                        }
                        """))
                .andReturn();

            String jobId1 = objectMapper.readTree(submitResponse1.getResponse().getContentAsString())
                .get("jobId").asText();

            // Tenant 2 should not see tenant 1's job
            var headers2 = GatewayTrustTestHeaders.adminHeaders(tenant2);
            mockMvc.perform(get("/quality-measure/population/jobs/" + jobId1)
                    .headers(headers2))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("not found")));

            // Tenant 1 should see their job
            mockMvc.perform(get("/quality-measure/population/jobs/" + jobId1)
                    .headers(headers1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId1));
        }
    }

    @Nested
    @DisplayName("Results Export")
    class ResultsExport {

        @Test
        @DisplayName("should export batch calculation results to CSV")
        void shouldExportBatchResultsToCsv() throws Exception {
            when(cqlEngineServiceClient.evaluateCql(
                anyString(),
                anyString(),
                any(UUID.class),
                anyString()
            )).thenReturn("""
                {
                    "libraryName": "HEDIS_CDC_2024",
                    "measureResult": {
                        "measureName": "Diabetes Care",
                        "inNumerator": true,
                        "inDenominator": true,
                        "complianceRate": 90.0,
                        "score": 95.0
                    }
                }
                """);

            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            var submitResponse = mockMvc.perform(post("/quality-measure/population/calculate")
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "measureIds": ["HEDIS_CDC_A1C9"]
                        }
                        """))
                .andReturn();

            String jobId = objectMapper.readTree(submitResponse.getResponse().getContentAsString())
                .get("jobId").asText();

            // Wait for completion
            await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    mockMvc.perform(get("/quality-measure/population/jobs/" + jobId)
                            .headers(headers))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("COMPLETED"));
                });

            // Export results
            mockMvc.perform(get("/quality-measure/population/jobs/" + jobId + "/export/csv")
                    .headers(headers))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Cache-Control", "no-store, no-cache, must-revalidate"))
                .andExpect(content().string(containsString("Patient ID,Measure ID,Numerator,Denominator,Score")));
        }
    }
}
