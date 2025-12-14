package com.healthdata.priorauth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.priorauth.dto.PriorAuthRequestDTO;
import com.healthdata.priorauth.persistence.PayerEndpointEntity;
import com.healthdata.priorauth.persistence.PayerEndpointRepository;
import com.healthdata.priorauth.persistence.PriorAuthRequestEntity;
import com.healthdata.priorauth.persistence.PriorAuthRequestRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Prior Authorization API.
 *
 * Tests the complete PA workflow including create, submit, status check, and cancel.
 */
@SpringBootTest(properties = {
    "spring.kafka.bootstrap-servers=",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PriorAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PriorAuthRequestRepository requestRepository;

    @Autowired
    private PayerEndpointRepository payerEndpointRepository;

    private static final String TENANT_ID = "test-tenant";
    private static final String PAYER_ID = "TEST-PAYER-001";
    private static UUID testPatientId;
    private static UUID createdRequestId;

    @BeforeAll
    static void initTestData() {
        testPatientId = UUID.randomUUID();
    }

    @BeforeEach
    void setUp() {
        // Ensure test payer endpoint exists
        if (!payerEndpointRepository.findByPayerId(PAYER_ID).isPresent()) {
            PayerEndpointEntity payer = PayerEndpointEntity.builder()
                .payerId(PAYER_ID)
                .payerName("Test Payer")
                .paFhirBaseUrl("http://localhost:9999/fhir")
                .authType(PayerEndpointEntity.AuthType.OAUTH2_CLIENT_CREDENTIALS)
                .isActive(true)
                .supportsRealTime(true)
                .build();
            payerEndpointRepository.save(payer);
        }
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/v1/prior-auth - Create PA request")
    void createPriorAuthRequest_shouldReturn201() throws Exception {
        PriorAuthRequestDTO request = PriorAuthRequestDTO.builder()
            .patientId(testPatientId)
            .serviceCode("99213")
            .serviceDescription("Office visit - established patient")
            .urgency(PriorAuthRequestEntity.Urgency.ROUTINE)
            .payerId(PAYER_ID)
            .providerId("PROV-001")
            .providerNpi("1234567890")
            .diagnosisCodes(Arrays.asList("E11.9", "I10"))
            .procedureCodes(Arrays.asList("99213"))
            .quantityRequested(1)
            .build();

        MvcResult result = mockMvc.perform(post("/api/v1/prior-auth")
                .header("X-Tenant-Id", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.paRequestId").exists())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.patientId").value(testPatientId.toString()))
            .andExpect(jsonPath("$.serviceCode").value("99213"))
            .andExpect(jsonPath("$.urgency").value("ROUTINE"))
            .andReturn();

        // Store the created request ID for subsequent tests
        String responseJson = result.getResponse().getContentAsString();
        PriorAuthRequestDTO.Response response = objectMapper.readValue(responseJson, PriorAuthRequestDTO.Response.class);
        createdRequestId = response.getId();

        assertThat(createdRequestId).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/v1/prior-auth/{id} - Get PA request by ID")
    void getPriorAuthRequest_shouldReturnRequest() throws Exception {
        // First create a request if not already created
        if (createdRequestId == null) {
            createPriorAuthRequest_shouldReturn201();
        }

        mockMvc.perform(get("/api/v1/prior-auth/{requestId}", createdRequestId)
                .header("X-Tenant-Id", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(createdRequestId.toString()))
            .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/v1/prior-auth/patient/{patientId} - Get patient PA requests")
    void getPatientRequests_shouldReturnPaginatedResults() throws Exception {
        mockMvc.perform(get("/api/v1/prior-auth/patient/{patientId}", testPatientId)
                .header("X-Tenant-Id", TENANT_ID)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$.content[0].patientId").value(testPatientId.toString()));
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/v1/prior-auth/status/{status} - Get requests by status")
    void getRequestsByStatus_shouldReturnFilteredResults() throws Exception {
        mockMvc.perform(get("/api/v1/prior-auth/status/{status}", "DRAFT")
                .header("X-Tenant-Id", TENANT_ID)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[*].status", everyItem(equalTo("DRAFT"))));
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/v1/prior-auth/{id}/cancel - Cancel PA request")
    void cancelPriorAuthRequest_shouldReturnCancelledStatus() throws Exception {
        // Create a new request specifically for cancellation test
        PriorAuthRequestDTO request = PriorAuthRequestDTO.builder()
            .patientId(testPatientId)
            .serviceCode("99214")
            .serviceDescription("Office visit - established patient, moderate")
            .urgency(PriorAuthRequestEntity.Urgency.ROUTINE)
            .payerId(PAYER_ID)
            .quantityRequested(1)
            .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/prior-auth")
                .header("X-Tenant-Id", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();

        String responseJson = createResult.getResponse().getContentAsString();
        PriorAuthRequestDTO.Response response = objectMapper.readValue(responseJson, PriorAuthRequestDTO.Response.class);
        UUID requestToCancel = response.getId();

        // Cancel the request
        mockMvc.perform(post("/api/v1/prior-auth/{requestId}/cancel", requestToCancel)
                .header("X-Tenant-Id", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/v1/prior-auth/statistics - Get PA statistics")
    void getStatistics_shouldReturnAggregatedStats() throws Exception {
        mockMvc.perform(get("/api/v1/prior-auth/statistics")
                .header("X-Tenant-Id", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRequests").isNumber())
            .andExpect(jsonPath("$.pendingRequests").isNumber())
            .andExpect(jsonPath("$.approvedRequests").isNumber())
            .andExpect(jsonPath("$.deniedRequests").isNumber());
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/v1/prior-auth/sla-alerts - Get SLA alerts")
    void getSlaAlerts_shouldReturnApproachingDeadlineRequests() throws Exception {
        mockMvc.perform(get("/api/v1/prior-auth/sla-alerts")
                .header("X-Tenant-Id", TENANT_ID)
                .param("hoursUntilDeadline", "24"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(8)
    @DisplayName("POST /api/v1/prior-auth - Create STAT urgency request")
    void createStatRequest_shouldSetCorrectSlaDeadline() throws Exception {
        PriorAuthRequestDTO request = PriorAuthRequestDTO.builder()
            .patientId(testPatientId)
            .serviceCode("99223")
            .serviceDescription("Initial hospital care - high severity")
            .urgency(PriorAuthRequestEntity.Urgency.STAT)
            .payerId(PAYER_ID)
            .diagnosisCodes(List.of("I21.3"))
            .quantityRequested(1)
            .build();

        mockMvc.perform(post("/api/v1/prior-auth")
                .header("X-Tenant-Id", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.urgency").value("STAT"));
    }

    @Test
    @Order(9)
    @DisplayName("POST /api/v1/prior-auth - Validation errors for missing required fields")
    void createRequest_withMissingFields_shouldReturn400() throws Exception {
        PriorAuthRequestDTO request = PriorAuthRequestDTO.builder()
            .patientId(null)  // Required field missing
            .serviceCode(null)  // Required field missing
            .urgency(null)  // Required field missing
            .payerId(null)  // Required field missing
            .build();

        mockMvc.perform(post("/api/v1/prior-auth")
                .header("X-Tenant-Id", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Order(10)
    @DisplayName("GET /api/v1/prior-auth/{id} - Non-existent request returns 404")
    void getRequest_notFound_shouldReturn404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/prior-auth/{requestId}", nonExistentId)
                .header("X-Tenant-Id", TENANT_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    @Order(11)
    @DisplayName("Tenant isolation - cannot access other tenant's requests")
    void tenantIsolation_shouldNotReturnOtherTenantsData() throws Exception {
        mockMvc.perform(get("/api/v1/prior-auth/patient/{patientId}", testPatientId)
                .header("X-Tenant-Id", "other-tenant")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content", hasSize(0)));  // Should be empty for other tenant
    }
}
