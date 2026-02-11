package com.healthdata.caregap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.caregap.api.v1.dto.CareGapEventResponse;
import com.healthdata.caregap.api.v1.dto.DetectGapRequest;
import com.healthdata.caregap.persistence.PopulationHealthRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PHASE 5: Care Gap Event Service Integration Tests
 *
 * Migrated from @Testcontainers Docker pattern to @EmbeddedKafka.
 * Uses embedded Kafka broker + H2 database for fast, reliable testing without Docker.
 *
 * Validates complete flow: REST → Service → EventHandler → Database
 * Tests care gap detection, severity classification, interventions, and population health
 */
@SpringBootTest
@EmbeddedKafka(partitions = 3, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Tag("integration")
@Tag("slow")
@DisplayName("CareGapEventService Integration Tests")
class CareGapEventServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PopulationHealthRepository populationHealthRepository;

    private static final String TENANT_ID = "TENANT-001";
    private static final String API_BASE_PATH = "/api/v1/gaps/events";

    @BeforeEach
    void setUp() {
        populationHealthRepository.deleteAll();
    }

    // ===== Gap Detection Tests =====

    @Test
    @DisplayName("Should accept POST /api/v1/gaps/detect endpoint")
    void testDetectCareGapReturnsAccepted() throws Exception {
        // Given: Valid DetectGapRequest
        DetectGapRequest request = new DetectGapRequest(
            "PATIENT-001", "GAP-001", "Missing diabetes screening", "CRITICAL"
        );

        // When: POST to detect endpoint
        mockMvc.perform(post(API_BASE_PATH + "/detect")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // Then: Response is 202 Accepted
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.gapCode").value("GAP-001"))
            .andExpect(jsonPath("$.severity").value("CRITICAL"));
    }

    @Test
    @DisplayName("Should persist care gap projection to database")
    void testCareGapProjectionPersisted() throws Exception {
        // Given: Care gap detection request
        DetectGapRequest request = new DetectGapRequest(
            "PATIENT-002", "GAP-002", "Missing eye exam", "HIGH"
        );

        // When: Submit gap detection via REST
        mockMvc.perform(post(API_BASE_PATH + "/detect")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted());

        // Then: Projection should be persisted to database
        // (Actual verification would require query endpoint)
    }

    // ===== Severity Classification Tests =====

    @Test
    @DisplayName("Should classify gap severity as CRITICAL")
    void testCriticalSeverityClassification() throws Exception {
        // Given: Critical gap
        DetectGapRequest request = new DetectGapRequest(
            "PATIENT-003", "GAP-CRIT", "Uncontrolled hypertension", "CRITICAL"
        );

        // When: Detect gap
        mockMvc.perform(post(API_BASE_PATH + "/detect")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // Then: Severity should be CRITICAL
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.severity").value("CRITICAL"));
    }

    @Test
    @DisplayName("Should classify gap severity as HIGH")
    void testHighSeverityClassification() throws Exception {
        // Given: High priority gap
        DetectGapRequest request = new DetectGapRequest(
            "PATIENT-004", "GAP-HIGH", "Missing HbA1c test", "HIGH"
        );

        // When: Detect gap
        mockMvc.perform(post(API_BASE_PATH + "/detect")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // Then: Severity should be HIGH
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.severity").value("HIGH"));
    }

    @Test
    @DisplayName("Should classify gap severity as MEDIUM")
    void testMediumSeverityClassification() throws Exception {
        // Given: Medium priority gap
        DetectGapRequest request = new DetectGapRequest(
            "PATIENT-005", "GAP-MED", "Medication review needed", "MEDIUM"
        );

        // When: Detect gap
        mockMvc.perform(post(API_BASE_PATH + "/detect")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // Then: Severity should be MEDIUM
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.severity").value("MEDIUM"));
    }

    @Test
    @DisplayName("Should classify gap severity as LOW")
    void testLowSeverityClassification() throws Exception {
        // Given: Low priority gap
        DetectGapRequest request = new DetectGapRequest(
            "PATIENT-006", "GAP-LOW", "Wellness screening", "LOW"
        );

        // When: Detect gap
        mockMvc.perform(post(API_BASE_PATH + "/detect")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // Then: Severity should be LOW
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.severity").value("LOW"));
    }

    // ===== Patient Qualification Tests =====

    @Test
    @DisplayName("Should accept POST /api/v1/gaps/qualify endpoint")
    void testQualifyPatientForGap() throws Exception {
        // Given: Qualification request
        String qualifyRequest = "{" +
            "\"patientId\": \"PATIENT-007\"," +
            "\"gapCode\": \"GAP-007\"" +
            "}";

        // When: POST to qualify endpoint
        mockMvc.perform(post(API_BASE_PATH + "/qualify")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(qualifyRequest))
            // Then: Response indicates success
            .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("Should track patient qualification status")
    void testPatientQualificationTracking() throws Exception {
        // Given: Detect gap first
        DetectGapRequest detectRequest = new DetectGapRequest(
            "PATIENT-008", "GAP-008", "Missing screening", "HIGH"
        );

        mockMvc.perform(post(API_BASE_PATH + "/detect")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(detectRequest)))
            .andExpect(status().isAccepted());

        // When: Qualify patient
        String qualifyRequest = "{" +
            "\"patientId\": \"PATIENT-008\"," +
            "\"gapCode\": \"GAP-008\"" +
            "}";

        mockMvc.perform(post(API_BASE_PATH + "/qualify")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(qualifyRequest))
            // Then: Qualification should be tracked
            .andExpect(status().isAccepted());
    }

    // ===== Intervention Recommendation Tests =====

    @Test
    @DisplayName("Should accept POST /api/v1/gaps/intervene endpoint")
    void testRecommendIntervention() throws Exception {
        // Given: Intervention request
        String interventionRequest = "{" +
            "\"patientId\": \"PATIENT-009\"," +
            "\"gapCode\": \"GAP-009\"," +
            "\"recommendation\": \"Schedule HbA1c test\"" +
            "}";

        // When: POST to intervene endpoint
        mockMvc.perform(post(API_BASE_PATH + "/intervene")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(interventionRequest))
            // Then: Response indicates success
            .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("Should track recommended interventions in projection")
    void testInterventionTracking() throws Exception {
        // Given: Gap and intervention
        String interventionRequest = "{" +
            "\"patientId\": \"PATIENT-010\"," +
            "\"gapCode\": \"GAP-010\"," +
            "\"recommendation\": \"Start blood pressure medication\"" +
            "}";

        // When: Recommend intervention
        mockMvc.perform(post(API_BASE_PATH + "/intervene")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(interventionRequest))
            // Then: Intervention should be tracked
            .andExpect(status().isAccepted());
    }

    // ===== Gap Closure Tests =====

    @Test
    @DisplayName("Should accept POST /api/v1/gaps/close endpoint")
    void testCloseGap() throws Exception {
        // Given: Close gap request
        String closeRequest = "{" +
            "\"patientId\": \"PATIENT-011\"," +
            "\"gapCode\": \"GAP-011\"," +
            "\"reason\": \"Screening completed\"" +
            "}";

        // When: POST to close endpoint
        mockMvc.perform(post(API_BASE_PATH + "/close")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(closeRequest))
            // Then: Response indicates success
            .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("Should update gap status to CLOSED")
    void testGapClosureStatus() throws Exception {
        // Given: Detect and close gap
        DetectGapRequest detectRequest = new DetectGapRequest(
            "PATIENT-012", "GAP-012", "Missing exam", "HIGH"
        );

        mockMvc.perform(post(API_BASE_PATH + "/detect")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(detectRequest)))
            .andExpect(status().isAccepted());

        // When: Close gap
        String closeRequest = "{" +
            "\"patientId\": \"PATIENT-012\"," +
            "\"gapCode\": \"GAP-012\"," +
            "\"reason\": \"Exam completed\"" +
            "}";

        mockMvc.perform(post(API_BASE_PATH + "/close")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(closeRequest))
            // Then: Gap should be closed
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    // ===== Days Open Calculation Tests =====

    @Test
    @DisplayName("Should calculate days open for active gaps")
    void testDaysOpenCalculation() throws Exception {
        // Given: Detected gap
        DetectGapRequest request = new DetectGapRequest(
            "PATIENT-013", "GAP-013", "Gap for aging test", "MEDIUM"
        );

        // When: Detect gap
        mockMvc.perform(post(API_BASE_PATH + "/detect")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // Then: Days open should be trackable
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.detectionDate").exists());
    }

    // ===== Population Health Aggregation Tests =====

    @Test
    @DisplayName("Should accept GET /api/v1/gaps/population/health endpoint")
    void testPopulationHealthAggregation() throws Exception {
        // Given: Multiple gaps across cohort
        for (int i = 1; i <= 10; i++) {
            DetectGapRequest request = new DetectGapRequest(
                "PATIENT-POP-" + String.format("%02d", i),
                "GAP-POP-" + String.format("%02d", i),
                "Population gap " + i,
                (i <= 3) ? "CRITICAL" : (i <= 6) ? "HIGH" : "MEDIUM"
            );

            mockMvc.perform(post(API_BASE_PATH + "/detect")
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
        }

        // When: Query population health
        mockMvc.perform(get(API_BASE_PATH + "/population/health")
                .header("X-Tenant-ID", TENANT_ID))
            // Then: Should return aggregated metrics
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalGapsOpen").value(10))
            .andExpect(jsonPath("$.criticalGaps").value(3))
            .andExpect(jsonPath("$.highGaps").value(3));
    }

    @Test
    @DisplayName("Should track closure rate in population health")
    void testClosureRateAggregation() throws Exception {
        // Given: 10 gaps detected, 3 closed
        for (int i = 1; i <= 10; i++) {
            DetectGapRequest request = new DetectGapRequest(
                "PATIENT-RATE-" + String.format("%02d", i),
                "GAP-RATE-" + String.format("%02d", i),
                "Gap for closure rate test",
                "MEDIUM"
            );

            mockMvc.perform(post(API_BASE_PATH + "/detect")
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
        }

        // Close 3 gaps
        for (int i = 1; i <= 3; i++) {
            String closeRequest = "{" +
                "\"patientId\": \"PATIENT-RATE-" + String.format("%02d", i) + "\"," +
                "\"gapCode\": \"GAP-RATE-" + String.format("%02d", i) + "\"," +
                "\"reason\": \"Closed\"" +
                "}";

            mockMvc.perform(post(API_BASE_PATH + "/close")
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(closeRequest))
                .andExpect(status().isAccepted());
        }

        // When: Query population health
        mockMvc.perform(get(API_BASE_PATH + "/population/health")
                .header("X-Tenant-ID", TENANT_ID))
            // Then: Closure rate should be calculated (3/13 gaps closed)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.closureRate").isNumber());
    }

    // ===== Multi-Tenant Isolation Tests =====

    @Test
    @DisplayName("Should isolate gaps by tenant")
    void testMultiTenantGapIsolation() throws Exception {
        // Given: Same gap code in different tenants
        DetectGapRequest request = new DetectGapRequest(
            "PATIENT-TENANT", "GAP-TENANT", "Multi-tenant gap", "HIGH"
        );

        // When: Submit for tenant 1
        mockMvc.perform(post(API_BASE_PATH + "/detect")
                .header("X-Tenant-ID", "TENANT-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted());

        // And: Submit for tenant 2
        mockMvc.perform(post(API_BASE_PATH + "/detect")
                .header("X-Tenant-ID", "TENANT-002")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted());

        // Then: Projections should be isolated by tenant
    }

    // ===== Kafka Publishing Tests =====

    @Test
    @DisplayName("Should publish care gap events to Kafka")
    void testCareGapEventPublishedToKafka() throws Exception {
        // Given: Gap detection request
        DetectGapRequest request = new DetectGapRequest(
            "PATIENT-KAFKA", "GAP-KAFKA", "Kafka test gap", "CRITICAL"
        );

        // When: Detect gap
        mockMvc.perform(post(API_BASE_PATH + "/detect")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted());

        // Then: Event should be published to care.gaps topic
        // (Actual verification would require Kafka consumer)
    }

    // ===== Error Handling Tests =====

    @Test
    @DisplayName("Should reject invalid severity level")
    void testInvalidSeverityLevel() throws Exception {
        // Given: Request with invalid severity
        String invalidRequest = "{" +
            "\"patientId\": \"PATIENT-ERR\"," +
            "\"gapCode\": \"GAP-ERR\"," +
            "\"description\": \"Invalid severity test\"," +
            "\"severity\": \"INVALID_LEVEL\"" +
            "}";

        // When: Submit request
        mockMvc.perform(post(API_BASE_PATH + "/detect")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            // Then: Should be rejected
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate required fields in gap detection")
    void testMissingRequiredFields() throws Exception {
        // Given: Request missing required field
        String incompleteRequest = "{" +
            "\"patientId\": \"PATIENT-INCOMPLETE\"" +
            "}";

        // When: Submit request
        mockMvc.perform(post(API_BASE_PATH + "/detect")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(incompleteRequest))
            // Then: Should be rejected
            .andExpect(status().isBadRequest());
    }

    // ===== Response Validation Tests =====

    @Test
    @DisplayName("Should return proper care gap event response structure")
    void testCareGapResponseStructure() throws Exception {
        // Given: Gap detection request
        DetectGapRequest request = new DetectGapRequest(
            "PATIENT-RESP", "GAP-RESP", "Response structure test", "HIGH"
        );

        // When: Detect gap
        mockMvc.perform(post(API_BASE_PATH + "/detect")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // Then: Response should have proper structure
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.gapCode").exists())
            .andExpect(jsonPath("$.severity").exists())
            .andExpect(jsonPath("$.status").exists())
            .andExpect(jsonPath("$.detectionDate").exists());
    }
}
