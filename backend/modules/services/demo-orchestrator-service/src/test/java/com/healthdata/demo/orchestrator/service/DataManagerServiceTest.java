package com.healthdata.demo.orchestrator.service;

import com.healthdata.demo.orchestrator.integration.DevOpsAgentClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DataManagerService clearAllData() functionality
 *
 * Tests verify:
 * - Successful clearing of all 5 demo services
 * - Circuit breaker behavior when individual services fail
 * - Audit logging via DevOpsAgentClient
 * - Tenant isolation (only demo tenant data cleared)
 * - ClearDataResult DTO population
 */
@ExtendWith(MockitoExtension.class)
class DataManagerServiceTest {

    @Mock
    private DevOpsAgentClient devopsAgent;

    @Captor
    private ArgumentCaptor<String> logMessageCaptor;

    private MockWebServer mockWebServer;
    private DataManagerService dataManagerService;

    private static final String DEMO_TENANT_ID = "demo-tenant";

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        dataManagerService = new DataManagerService(devopsAgent);

        // Inject test configuration
        ReflectionTestUtils.setField(dataManagerService, "demoTenantId", DEMO_TENANT_ID);
        ReflectionTestUtils.setField(dataManagerService, "gatewayUrl",
            "http://localhost:" + mockWebServer.getPort());
        ReflectionTestUtils.setField(dataManagerService, "demoSeedingServiceUrl",
            "http://localhost:" + mockWebServer.getPort());
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void clearAllData_ShouldClearAllServices_WhenAllServicesSucceed() throws InterruptedException {
        // Given: All services return success counts
        mockWebServer.enqueue(new MockResponse().setBody("25").addHeader("Content-Type", "application/json"));  // patients
        mockWebServer.enqueue(new MockResponse().setBody("15").addHeader("Content-Type", "application/json"));  // care gaps
        mockWebServer.enqueue(new MockResponse().setBody("30").addHeader("Content-Type", "application/json"));  // evaluations
        mockWebServer.enqueue(new MockResponse().setBody("40").addHeader("Content-Type", "application/json"));  // FHIR
        mockWebServer.enqueue(new MockResponse().setBody("10").addHeader("Content-Type", "application/json"));  // predictions

        // When
        DataManagerService.ClearDataResult result = dataManagerService.clearAllData();

        // Then: All services cleared successfully
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotalEntitiesCleared()).isEqualTo(120);
        assertThat(result.getSuccessfulOperations()).hasSize(5);
        assertThat(result.getFailedOperations()).isEmpty();

        // Verify correct endpoints called with tenant header
        RecordedRequest request1 = mockWebServer.takeRequest();
        assertThat(request1.getPath()).isEqualTo("/api/v1/patients/demo/clear");
        assertThat(request1.getMethod()).isEqualTo("DELETE");
        assertThat(request1.getHeader("X-Tenant-ID")).isEqualTo(DEMO_TENANT_ID);

        RecordedRequest request2 = mockWebServer.takeRequest();
        assertThat(request2.getPath()).isEqualTo("/api/v1/care-gaps/demo/clear");

        RecordedRequest request3 = mockWebServer.takeRequest();
        assertThat(request3.getPath()).isEqualTo("/api/v1/evaluations/demo/clear");

        RecordedRequest request4 = mockWebServer.takeRequest();
        assertThat(request4.getPath()).isEqualTo("/api/v1/fhir/demo/clear");

        RecordedRequest request5 = mockWebServer.takeRequest();
        assertThat(request5.getPath()).isEqualTo("/api/v1/predictions/demo/clear");

        // Verify audit logging
        verify(devopsAgent, times(11)).publishLog(anyString(), anyString(), eq("CLEAR"));
        verify(devopsAgent).publishLog(eq("INFO"),
            contains("Starting data clearing for demo tenant: " + DEMO_TENANT_ID), eq("CLEAR"));
        verify(devopsAgent).publishLog(eq("INFO"),
            contains("Data clearing completed: 120 entities cleared"), eq("CLEAR"));
    }

    @Test
    void clearAllData_ShouldContinue_WhenIndividualServiceFails() {
        // Given: Patient service fails, others succeed
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));  // patients fail
        mockWebServer.enqueue(new MockResponse().setBody("15").addHeader("Content-Type", "application/json"));  // care gaps
        mockWebServer.enqueue(new MockResponse().setBody("30").addHeader("Content-Type", "application/json"));  // evaluations
        mockWebServer.enqueue(new MockResponse().setBody("40").addHeader("Content-Type", "application/json"));  // FHIR
        mockWebServer.enqueue(new MockResponse().setBody("10").addHeader("Content-Type", "application/json"));  // predictions

        // When: Circuit breaker allows continuation
        DataManagerService.ClearDataResult result = dataManagerService.clearAllData();

        // Then: Operation completes with partial success
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotalEntitiesCleared()).isEqualTo(95);  // 15 + 30 + 40 + 10
        assertThat(result.getSuccessfulOperations()).hasSize(4);
        assertThat(result.getFailedOperations()).hasSize(1);
        assertThat(result.getFailedOperations().get(0)).startsWith("Patients:");

        // Verify warning logged for failure
        verify(devopsAgent).publishLog(eq("WARN"),
            contains("Failed to clear patient data"), eq("CLEAR"));

        // Verify final summary uses WARN due to failures
        verify(devopsAgent).publishLog(eq("WARN"),
            contains("1 failed operations"), eq("CLEAR"));
    }

    @Test
    void clearAllData_ShouldHandleAllServiceFailures_Gracefully() {
        // Given: All services fail
        for (int i = 0; i < 5; i++) {
            mockWebServer.enqueue(new MockResponse().setResponseCode(503).setBody("Service Unavailable"));
        }

        // When
        DataManagerService.ClearDataResult result = dataManagerService.clearAllData();

        // Then: Returns result with all failures
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotalEntitiesCleared()).isEqualTo(0);
        assertThat(result.getSuccessfulOperations()).isEmpty();
        assertThat(result.getFailedOperations()).hasSize(5);

        // Verify all failures logged
        verify(devopsAgent, times(5)).publishLog(eq("WARN"), contains("Failed to clear"), eq("CLEAR"));
    }

    @Test
    void clearAllData_ShouldHandleNullResponseCounts() {
        // Given: Services return null bodies (edge case)
        mockWebServer.enqueue(new MockResponse().setBody("").addHeader("Content-Type", "application/json"));  // empty body
        mockWebServer.enqueue(new MockResponse().setBody("15").addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse().setBody("30").addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse().setBody("40").addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse().setBody("10").addHeader("Content-Type", "application/json"));

        // When: Null handling treats as 0
        DataManagerService.ClearDataResult result = dataManagerService.clearAllData();

        // Then: Completes with 4 successful operations
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotalEntitiesCleared()).isEqualTo(95);  // 0 + 15 + 30 + 40 + 10
        assertThat(result.getSuccessfulOperations()).hasSize(5);
    }

    @Test
    void clearAllData_ShouldLogAuditTrail_ForComplianceTracking() {
        // Given
        mockWebServer.enqueue(new MockResponse().setBody("25").addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse().setBody("15").addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse().setBody("30").addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse().setBody("40").addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse().setBody("10").addHeader("Content-Type", "application/json"));

        // When
        dataManagerService.clearAllData();

        // Then: Comprehensive audit trail logged
        verify(devopsAgent).publishLog(eq("INFO"),
            eq("Starting data clearing for demo tenant: " + DEMO_TENANT_ID), eq("CLEAR"));
        verify(devopsAgent).publishLog(eq("INFO"), eq("Clearing patient data..."), eq("CLEAR"));
        verify(devopsAgent).publishLog(eq("INFO"), eq("Cleared 25 patients"), eq("CLEAR"));
        verify(devopsAgent).publishLog(eq("INFO"), eq("Clearing care gap data..."), eq("CLEAR"));
        verify(devopsAgent).publishLog(eq("INFO"), eq("Cleared 15 care gaps"), eq("CLEAR"));
        verify(devopsAgent).publishLog(eq("INFO"), eq("Clearing quality measure evaluations..."), eq("CLEAR"));
        verify(devopsAgent).publishLog(eq("INFO"), eq("Cleared 30 evaluations"), eq("CLEAR"));
        verify(devopsAgent).publishLog(eq("INFO"), eq("Clearing FHIR resources..."), eq("CLEAR"));
        verify(devopsAgent).publishLog(eq("INFO"), eq("Cleared 40 FHIR resources"), eq("CLEAR"));
        verify(devopsAgent).publishLog(eq("INFO"), eq("Clearing predictive analytics data..."), eq("CLEAR"));
        verify(devopsAgent).publishLog(eq("INFO"), eq("Cleared 10 predictions"), eq("CLEAR"));

        // Verify summary
        verify(devopsAgent).publishLog(eq("INFO"),
            contains("Data clearing completed: 120 entities cleared, 5 successful operations, 0 failed operations"),
            eq("CLEAR"));
    }

    @Test
    void clearAllData_ShouldIncludeServiceNamesInResults() {
        // Given
        mockWebServer.enqueue(new MockResponse().setBody("25").addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse().setBody("15").addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse().setBody("30").addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse().setBody("40").addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse().setBody("10").addHeader("Content-Type", "application/json"));

        // When
        DataManagerService.ClearDataResult result = dataManagerService.clearAllData();

        // Then: Results include service names with counts
        assertThat(result.getSuccessfulOperations()).contains("Patients: 25");
        assertThat(result.getSuccessfulOperations()).contains("Care Gaps: 15");
        assertThat(result.getSuccessfulOperations()).contains("Evaluations: 30");
        assertThat(result.getSuccessfulOperations()).contains("FHIR Resources: 40");
        assertThat(result.getSuccessfulOperations()).contains("Predictions: 10");
    }

    // ===== seedBaseData() tests =====

    @Test
    void seedBaseData_ShouldInitializeAndGeneratePatients_WhenServicesSucceed() throws InterruptedException {
        // Given: Initialize and generate endpoints succeed
        String initResponse = "{\"success\": true, \"message\": \"Demo platform initialized successfully\"}";
        String generateResponse = "{"
            + "\"success\": true,"
            + "\"tenantId\": \"" + DEMO_TENANT_ID + "\","
            + "\"patientCount\": 100,"
            + "\"careGapCount\": 40,"
            + "\"medicationCount\": 150,"
            + "\"observationCount\": 200,"
            + "\"encounterCount\": 120,"
            + "\"procedureCount\": 80,"
            + "\"generationTimeMs\": 2500"
            + "}";

        mockWebServer.enqueue(new MockResponse()
            .setBody(initResponse)
            .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse()
            .setBody(generateResponse)
            .addHeader("Content-Type", "application/json"));

        // When
        dataManagerService.seedBaseData();

        // Then: Both endpoints called correctly
        RecordedRequest initRequest = mockWebServer.takeRequest();
        assertThat(initRequest.getPath()).isEqualTo("/demo/api/v1/demo/initialize");
        assertThat(initRequest.getMethod()).isEqualTo("POST");
        assertThat(initRequest.getHeader("X-Tenant-ID")).isEqualTo(DEMO_TENANT_ID);

        RecordedRequest generateRequest = mockWebServer.takeRequest();
        assertThat(generateRequest.getPath()).isEqualTo("/demo/api/v1/demo/patients/generate");
        assertThat(generateRequest.getMethod()).isEqualTo("POST");
        assertThat(generateRequest.getHeader("X-Tenant-ID")).isEqualTo(DEMO_TENANT_ID);
        assertThat(generateRequest.getBody().readUtf8())
            .contains("\"count\": 100")
            .contains("\"tenantId\": \"" + DEMO_TENANT_ID + "\"")
            .contains("\"careGapPercentage\": 40");

        // Verify audit logging
        verify(devopsAgent).publishLog(eq("INFO"), eq("Seeding base demo data..."), eq("SEED"));
        verify(devopsAgent).publishLog(eq("INFO"), eq("Initializing demo scenarios..."), eq("SEED"));
        verify(devopsAgent).publishLog(eq("INFO"), eq("Generating patient cohort..."), eq("SEED"));
        verify(devopsAgent).publishLog(eq("INFO"),
            contains("Base data seeding completed: 100 patients, 40 care gaps"), eq("SEED"));
    }

    @Test
    void seedBaseData_ShouldContinue_WhenInitializationFails() throws InterruptedException {
        // Given: Initialize fails but generate succeeds
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setBody("Initialization failed"));
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"success\": true, \"patientCount\": 100, \"careGapCount\": 40,"
                + "\"medicationCount\": 150, \"observationCount\": 200,"
                + "\"encounterCount\": 120, \"procedureCount\": 80,"
                + "\"generationTimeMs\": 2500, \"tenantId\": \"" + DEMO_TENANT_ID + "\"}")
            .addHeader("Content-Type", "application/json"));

        // When: Should continue despite initialization failure
        dataManagerService.seedBaseData();

        // Then: Both endpoints called
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);

        RecordedRequest initRequest = mockWebServer.takeRequest();
        assertThat(initRequest.getPath()).isEqualTo("/demo/api/v1/demo/initialize");

        RecordedRequest generateRequest = mockWebServer.takeRequest();
        assertThat(generateRequest.getPath()).isEqualTo("/demo/api/v1/demo/patients/generate");

        // Verify successful completion logged despite init failure
        verify(devopsAgent).publishLog(eq("INFO"),
            contains("Base data seeding completed: 100 patients"), eq("SEED"));
    }

    @Test
    void seedBaseData_ShouldHandleGenerationFailure_Gracefully() throws InterruptedException {
        // Given: Initialize succeeds, generate fails
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"success\": true, \"message\": \"Initialized\"}")
            .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"success\": false, \"errorMessage\": \"Database connection failed\"}")
            .addHeader("Content-Type", "application/json"));

        // When
        dataManagerService.seedBaseData();

        // Then: Failure logged
        verify(devopsAgent).publishLog(eq("ERROR"),
            contains("Base data seeding failed: Database connection failed"), eq("SEED"));
    }

    @Test
    void seedBaseData_ShouldHandleServiceUnavailable() {
        // Given: Service completely unavailable (503 returns failure response)
        mockWebServer.enqueue(new MockResponse().setResponseCode(503).setBody("Service Unavailable"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(503).setBody("Service Unavailable"));

        // When
        dataManagerService.seedBaseData();

        // Then: Error logged (503 from WebClient throws exception, caught and logged as failure)
        verify(devopsAgent).publishLog(eq("ERROR"),
            contains("Base data seeding failed"), eq("SEED"));
    }

    @Test
    void seedBaseData_ShouldIncludeAllCounts_InSuccessMessage() throws InterruptedException {
        // Given: Full successful response
        String generateResponse = "{"
            + "\"success\": true,"
            + "\"tenantId\": \"" + DEMO_TENANT_ID + "\","
            + "\"patientCount\": 100,"
            + "\"careGapCount\": 40,"
            + "\"medicationCount\": 150,"
            + "\"observationCount\": 200,"
            + "\"encounterCount\": 120,"
            + "\"procedureCount\": 80,"
            + "\"generationTimeMs\": 2500"
            + "}";

        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"success\": true}")
            .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse()
            .setBody(generateResponse)
            .addHeader("Content-Type", "application/json"));

        // When
        dataManagerService.seedBaseData();

        // Then: Summary includes all resource counts
        verify(devopsAgent).publishLog(eq("INFO"),
            argThat(msg -> msg.contains("100 patients")
                && msg.contains("40 care gaps")
                && msg.contains("150 medications")
                && msg.contains("200 observations")
                && msg.contains("120 encounters")
                && msg.contains("80 procedures")
                && msg.contains("took 2500 ms")),
            eq("SEED"));
    }

    @Test
    void seedBaseData_ShouldUse100PatientsAnd40PercentCareGaps() throws InterruptedException {
        // Given
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"success\": true}")
            .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"success\": true, \"patientCount\": 100, \"careGapCount\": 40,"
                + "\"medicationCount\": 0, \"observationCount\": 0,"
                + "\"encounterCount\": 0, \"procedureCount\": 0,"
                + "\"generationTimeMs\": 0, \"tenantId\": \"" + DEMO_TENANT_ID + "\"}")
            .addHeader("Content-Type", "application/json"));

        // When
        dataManagerService.seedBaseData();

        // Then: Verify request parameters
        mockWebServer.takeRequest();  // Skip initialize request
        RecordedRequest generateRequest = mockWebServer.takeRequest();
        String requestBody = generateRequest.getBody().readUtf8();

        assertThat(requestBody).contains("\"count\": 100");
        assertThat(requestBody).contains("\"careGapPercentage\": 40");
        assertThat(requestBody).contains("\"tenantId\": \"" + DEMO_TENANT_ID + "\"");
    }
}
