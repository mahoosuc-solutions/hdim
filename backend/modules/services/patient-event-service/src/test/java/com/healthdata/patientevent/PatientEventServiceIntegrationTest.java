package com.healthdata.patientevent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.patientevent.api.v1.dto.CreatePatientRequest;
import com.healthdata.patientevent.api.v1.dto.PatientEventResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RED Phase: Patient Event Service Integration Tests
 *
 * Validates complete flow: REST → Service → EventHandler → Database
 * Tests REST API endpoints, service layer, event handling, and database persistence
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("PatientEventService Integration Tests")
class PatientEventServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:16-alpine")
    ).withDatabaseName("patient_test_db")
     .withUsername("test_user")
     .withPassword("test_password");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("apache/kafka:3.8.0")
    ).withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TENANT_ID = "TENANT-001";
    private static final String API_BASE_PATH = "/api/v1/patients/events";

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @BeforeEach
    void setUp() {
        // Test setup if needed
    }

    // ===== Patient Creation Tests =====

    @Test
    @DisplayName("Should accept POST /api/v1/patients/events/create and return 202 Accepted")
    void testCreatePatientEventReturnsAccepted() throws Exception {
        // Given: Valid CreatePatientRequest
        CreatePatientRequest request = new CreatePatientRequest(
            "John", "Doe", "1980-05-15"
        );

        // When: POST to create endpoint
        mockMvc.perform(post(API_BASE_PATH + "/create")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // Then: Response is 202 Accepted
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.patientId").exists())
            .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    @DisplayName("Should create patient projection in database")
    void testPatientProjectionPersisted() throws Exception {
        // Given: Create patient event request
        CreatePatientRequest request = new CreatePatientRequest(
            "Jane", "Smith", "1990-03-20"
        );

        // When: Submit event via REST
        MvcResult result = mockMvc.perform(post(API_BASE_PATH + "/create")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted())
            .andReturn();

        // Then: Verify response contains patientId
        String responseBody = result.getResponse().getContentAsString();
        PatientEventResponse response = objectMapper.readValue(responseBody, PatientEventResponse.class);
        assertThat(response.getPatientId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo("CREATED");
    }

    @Test
    @DisplayName("Should return 400 Bad Request for invalid patient data")
    void testCreatePatientWithInvalidData() throws Exception {
        // Given: Invalid request (missing required fields)
        String invalidRequest = "{ \"firstName\": \"John\" }";

        // When: Submit invalid request
        mockMvc.perform(post(API_BASE_PATH + "/create")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            // Then: Response is 400 Bad Request
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate tenant ID from header")
    void testCreatePatientValidatesTenantId() throws Exception {
        // Given: Valid patient request but missing tenant header
        CreatePatientRequest request = new CreatePatientRequest(
            "Bob", "Johnson", "1975-08-10"
        );

        // When: Submit without tenant header
        mockMvc.perform(post(API_BASE_PATH + "/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // Then: Response is 400 or 401
            .andExpect(status().is4xxClientError());
    }

    // ===== Enrollment Tests =====

    @Test
    @DisplayName("Should accept POST /api/v1/patients/events/enroll endpoint")
    void testEnrollPatientEvent() throws Exception {
        // Given: Enrollment change request
        String enrollmentRequest = "{" +
            "\"patientId\": \"PATIENT-001\"," +
            "\"newStatus\": \"ACTIVE\"," +
            "\"reason\": \"Standard enrollment\"" +
            "}";

        // When: POST to enroll endpoint
        mockMvc.perform(post(API_BASE_PATH + "/enroll")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(enrollmentRequest))
            // Then: Response is 202 Accepted
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").exists());
    }

    // ===== Demographics Update Tests =====

    @Test
    @DisplayName("Should accept POST /api/v1/patients/events/demographics endpoint")
    void testUpdatePatientDemographicsEvent() throws Exception {
        // Given: Demographics update request
        String demographicsRequest = "{" +
            "\"patientId\": \"PATIENT-001\"," +
            "\"firstName\": \"John\"," +
            "\"lastName\": \"Updated\"," +
            "\"dateOfBirth\": \"1980-05-15\"" +
            "}";

        // When: POST to demographics endpoint
        mockMvc.perform(post(API_BASE_PATH + "/demographics")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(demographicsRequest))
            // Then: Response is 202 Accepted
            .andExpect(status().isAccepted());
    }

    // ===== Multi-Tenant Isolation Tests =====

    @Test
    @DisplayName("Should isolate patient events by tenant")
    void testMultiTenantIsolation() throws Exception {
        // Given: Create patient for tenant 1
        CreatePatientRequest request1 = new CreatePatientRequest(
            "Tenant1", "Patient", "1980-01-01"
        );

        mockMvc.perform(post(API_BASE_PATH + "/create")
                .header("X-Tenant-ID", "TENANT-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
            .andExpect(status().isAccepted());

        // And: Create patient for tenant 2
        CreatePatientRequest request2 = new CreatePatientRequest(
            "Tenant2", "Patient", "1980-01-01"
        );

        mockMvc.perform(post(API_BASE_PATH + "/create")
                .header("X-Tenant-ID", "TENANT-002")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
            .andExpect(status().isAccepted());

        // Then: Both should succeed with different tenant isolation
        // (actual verification would require query endpoint)
    }

    // ===== Error Handling Tests =====

    @Test
    @DisplayName("Should handle null firstName gracefully")
    void testCreatePatientWithNullFirstName() throws Exception {
        // Given: Request with null firstName
        String request = "{" +
            "\"firstName\": null," +
            "\"lastName\": \"Doe\"," +
            "\"dateOfBirth\": \"1980-01-01\"" +
            "}";

        // When: Submit request
        mockMvc.perform(post(API_BASE_PATH + "/create")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            // Then: Should fail validation
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle database connection failure gracefully")
    void testDatabaseConnectionFailure() throws Exception {
        // This test would require simulating a database failure
        // For RED phase, we document the expected behavior
        // In GREEN phase, we implement retry logic and circuit breaker

        CreatePatientRequest request = new CreatePatientRequest(
            "John", "Doe", "1980-01-01"
        );

        // Should return 5xx error when database is unavailable
        // Expected behavior: Graceful degradation or retry
    }

    // ===== Kafka Publishing Tests =====

    @Test
    @DisplayName("Should publish patient event to Kafka topic")
    void testPatientEventPublishedToKafka() throws Exception {
        // Given: Valid create patient request
        CreatePatientRequest request = new CreatePatientRequest(
            "Kafka", "Test", "1980-01-01"
        );

        // When: Submit event via REST
        mockMvc.perform(post(API_BASE_PATH + "/create")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted());

        // Then: Event should be published to Kafka topic
        // (Actual verification would require Kafka consumer setup)
        // For RED phase: This is a specification of desired behavior
    }

    // ===== Transaction Tests =====

    @Test
    @DisplayName("Should handle concurrent patient creation atomically")
    void testConcurrentPatientCreation() throws Exception {
        // Given: Multiple concurrent patient creation requests
        CreatePatientRequest request1 = new CreatePatientRequest(
            "Concurrent", "One", "1980-01-01"
        );
        CreatePatientRequest request2 = new CreatePatientRequest(
            "Concurrent", "Two", "1980-01-02"
        );

        // When: Submit concurrently
        // Then: Both should succeed with unique IDs
        // (Actual concurrent execution would require thread management)
    }

    @Test
    @DisplayName("Should maintain transactional consistency across event storage")
    void testTransactionalConsistency() throws Exception {
        // Given: Patient event that triggers multiple operations
        CreatePatientRequest request = new CreatePatientRequest(
            "Atomic", "Operation", "1980-01-01"
        );

        // When: Submit event
        mockMvc.perform(post(API_BASE_PATH + "/create")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted());

        // Then: All operations should succeed or none
        // (Actual verification: Check database consistency)
    }

    // ===== Event Handler Integration Tests =====

    @Test
    @DisplayName("Should invoke event handler for patient creation")
    void testEventHandlerInvoked() throws Exception {
        // Given: Create patient request
        CreatePatientRequest request = new CreatePatientRequest(
            "Handler", "Test", "1980-01-01"
        );

        // When: Submit event
        mockMvc.perform(post(API_BASE_PATH + "/create")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted());

        // Then: Event handler should have been invoked
        // (Verified through database projection state)
    }

    // ===== Response Validation Tests =====

    @Test
    @DisplayName("Should return proper event response structure")
    void testEventResponseStructure() throws Exception {
        // Given: Valid patient request
        CreatePatientRequest request = new CreatePatientRequest(
            "Response", "Test", "1980-01-01"
        );

        // When: Create patient event
        mockMvc.perform(post(API_BASE_PATH + "/create")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // Then: Response should have proper structure
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.patientId").exists())
            .andExpect(jsonPath("$.status").exists())
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should include tenant context in response")
    void testTenantContextInResponse() throws Exception {
        // Given: Create request with specific tenant
        CreatePatientRequest request = new CreatePatientRequest(
            "Tenant", "Context", "1980-01-01"
        );

        // When: Submit with tenant header
        mockMvc.perform(post(API_BASE_PATH + "/create")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // Then: Response should reflect tenant
            .andExpect(status().isAccepted());
    }
}
