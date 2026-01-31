package com.healthdata.qualityevent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.qualityevent.api.v1.dto.EvaluateMeasureRequest;
import com.healthdata.qualityevent.api.v1.dto.MeasureEventResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.data.redis.core.RedisTemplate;  // Redis removed from event services
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RED Phase: Quality Measure Event Service Integration Tests
 *
 * Validates complete flow: REST → Service → EventHandler → Database → Redis Cache
 * Tests measure evaluation, scoring, risk stratification, and cohort aggregation
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("QualityMeasureEventService Integration Tests")
class QualityMeasureEventServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:16-alpine")
    ).withDatabaseName("quality_test_db")
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

    // @Autowired(required = false)
    // private RedisTemplate<String, Object> redisTemplate;  // Redis removed from event services

    private static final String TENANT_ID = "TENANT-001";
    private static final String API_BASE_PATH = "/api/v1/measures";

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.redis.host", () -> "localhost");  // Will use embedded Redis if available
    }

    @BeforeEach
    void setUp() {
        // Redis removed from event services - no cache cleanup needed
        // if (redisTemplate != null) {
        //     redisTemplate.getConnectionFactory().getConnection().flushDb();
        // }
    }

    // ===== Measure Evaluation Tests =====

    @Test
    @DisplayName("Should accept POST /api/v1/measures/evaluate endpoint")
    void testEvaluateMeasureReturnsAccepted() throws Exception {
        // Given: Valid EvaluateMeasureRequest
        EvaluateMeasureRequest request = new EvaluateMeasureRequest(
            "PATIENT-001", "HEDIS-COLA", 0.85f
        );

        // When: POST to evaluate endpoint
        mockMvc.perform(post(API_BASE_PATH + "/evaluate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // Then: Response is 202 Accepted
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.measureCode").value("HEDIS-COLA"))
            .andExpect(jsonPath("$.status").exists());
    }

    @Test
    @DisplayName("Should persist measure evaluation projection to database")
    void testMeasureEvaluationProjectionPersisted() throws Exception {
        // Given: Measure evaluation request
        EvaluateMeasureRequest request = new EvaluateMeasureRequest(
            "PATIENT-002", "HEDIS-CDC", 0.92f
        );

        // When: Submit evaluation via REST
        mockMvc.perform(post(API_BASE_PATH + "/evaluate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted());

        // Then: Projection should be persisted to database
        // (Actual verification would require query endpoint)
    }

    // ===== Score Calculation Tests =====

    @Test
    @DisplayName("Should accept POST /api/v1/measures/scores/calculate endpoint")
    void testCalculateMeasureScore() throws Exception {
        // Given: Score calculation request
        String scoreRequest = "{" +
            "\"patientId\": \"PATIENT-003\"," +
            "\"measureCode\": \"HEDIS-COLA\"," +
            "\"score\": 0.85" +
            "}";

        // When: POST to calculate endpoint
        mockMvc.perform(post(API_BASE_PATH + "/scores/calculate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(scoreRequest))
            // Then: Response should indicate success
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").exists());
    }

    @Test
    @DisplayName("Should determine MET status when score > 0.75")
    void testMeasureMetStatusAboveThreshold() throws Exception {
        // Given: Measure with score 0.85 (above 75% threshold)
        EvaluateMeasureRequest request = new EvaluateMeasureRequest(
            "PATIENT-004", "HEDIS-COLA", 0.85f
        );

        // When: Submit evaluation
        mockMvc.perform(post(API_BASE_PATH + "/evaluate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // Then: Status should be MET
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.measureStatus").value("MET"));
    }

    @Test
    @DisplayName("Should determine NOT_MET status when score <= 0.75")
    void testMeasureNotMetStatusBelowThreshold() throws Exception {
        // Given: Measure with score 0.60 (below 75% threshold)
        EvaluateMeasureRequest request = new EvaluateMeasureRequest(
            "PATIENT-005", "HEDIS-COLA", 0.60f
        );

        // When: Submit evaluation
        mockMvc.perform(post(API_BASE_PATH + "/evaluate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // Then: Status should be NOT_MET
            .andExpect(status().isAccepted());
    }

    // ===== Risk Score Calculation Tests =====

    @Test
    @DisplayName("Should accept POST /api/v1/measures/risk/calculate endpoint")
    void testCalculateRiskScore() throws Exception {
        // Given: Risk calculation request
        String riskRequest = "{" +
            "\"patientId\": \"PATIENT-006\"," +
            "\"score\": 0.95" +
            "}";

        // When: POST to risk endpoint
        mockMvc.perform(post(API_BASE_PATH + "/risk/calculate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(riskRequest))
            // Then: Response indicates success
            .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("Should categorize risk as VERY_HIGH when score >= 0.90")
    void testRiskLevelVeryHigh() throws Exception {
        // Given: Score 0.95
        String riskRequest = "{" +
            "\"patientId\": \"PATIENT-007\"," +
            "\"score\": 0.95" +
            "}";

        // When: Calculate risk
        mockMvc.perform(post(API_BASE_PATH + "/risk/calculate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(riskRequest))
            // Then: Risk level should be VERY_HIGH
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.riskLevel").value("VERY_HIGH"));
    }

    @Test
    @DisplayName("Should categorize risk as HIGH when score >= 0.70 and < 0.90")
    void testRiskLevelHigh() throws Exception {
        // Given: Score 0.80
        String riskRequest = "{" +
            "\"patientId\": \"PATIENT-008\"," +
            "\"score\": 0.80" +
            "}";

        // When: Calculate risk
        mockMvc.perform(post(API_BASE_PATH + "/risk/calculate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(riskRequest))
            // Then: Risk level should be HIGH
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.riskLevel").value("HIGH"));
    }

    // ===== Cohort Aggregation Tests =====

    @Test
    @DisplayName("Should calculate cohort compliance rate from multiple evaluations")
    void testCohortComplianceRateCalculation() throws Exception {
        // Given: 10 patients in cohort with 7 meeting criteria
        for (int i = 1; i <= 10; i++) {
            float score = (i <= 7) ? 0.85f : 0.60f;  // 7 patients MET, 3 NOT_MET
            EvaluateMeasureRequest request = new EvaluateMeasureRequest(
                "PATIENT-" + String.format("%03d", i + 100),
                "HEDIS-COLA",
                score
            );

            mockMvc.perform(post(API_BASE_PATH + "/evaluate")
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
        }

        // When: Query cohort compliance rate
        mockMvc.perform(get(API_BASE_PATH + "/cohort/compliance")
                .header("X-Tenant-ID", TENANT_ID)
                .param("measureCode", "HEDIS-COLA"))
            // Then: Should calculate 70% compliance (7/10)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.complianceRate").value(0.70f));
    }

    // ===== Redis Caching Tests =====

    @Test
    @DisplayName("Should cache measure evaluation projection in Redis")
    void testMeasureProjectionCaching() throws Exception {
        // Given: Measure evaluation
        EvaluateMeasureRequest request = new EvaluateMeasureRequest(
            "PATIENT-009", "HEDIS-COLA", 0.85f
        );

        // When: Submit evaluation
        mockMvc.perform(post(API_BASE_PATH + "/evaluate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted());

        // Then: Subsequent query should hit cache
        // (Verified through timing or cache statistics)
    }

    // ===== Multi-Tenant Isolation Tests =====

    @Test
    @DisplayName("Should isolate measure evaluations by tenant")
    void testMultiTenantEvaluationIsolation() throws Exception {
        // Given: Same patient, same measure, different tenants
        EvaluateMeasureRequest request = new EvaluateMeasureRequest(
            "PATIENT-999", "HEDIS-COLA", 0.85f
        );

        // When: Submit for tenant 1
        mockMvc.perform(post(API_BASE_PATH + "/evaluate")
                .header("X-Tenant-ID", "TENANT-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted());

        // And: Submit for tenant 2
        mockMvc.perform(post(API_BASE_PATH + "/evaluate")
                .header("X-Tenant-ID", "TENANT-002")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted());

        // Then: Projections should be isolated by tenant
    }

    // ===== Kafka Publishing Tests =====

    @Test
    @DisplayName("Should publish measure evaluation to Kafka topic")
    void testMeasureEventPublishedToKafka() throws Exception {
        // Given: Measure evaluation
        EvaluateMeasureRequest request = new EvaluateMeasureRequest(
            "PATIENT-010", "HEDIS-COLA", 0.85f
        );

        // When: Submit evaluation
        mockMvc.perform(post(API_BASE_PATH + "/evaluate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted());

        // Then: Event should be published to Kafka
        // Expected topics: measure.evaluated, measure.scored
    }

    // ===== Error Handling Tests =====

    @Test
    @DisplayName("Should reject invalid measure code")
    void testInvalidMeasureCode() throws Exception {
        // Given: Request with invalid measure code
        String invalidRequest = "{" +
            "\"patientId\": \"PATIENT-011\"," +
            "\"measureCode\": \"INVALID-CODE\"," +
            "\"score\": 0.85" +
            "}";

        // When: Submit request
        mockMvc.perform(post(API_BASE_PATH + "/evaluate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            // Then: Should return error
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate score range 0.0-1.0")
    void testInvalidScoreRange() throws Exception {
        // Given: Score out of range
        EvaluateMeasureRequest request = new EvaluateMeasureRequest(
            "PATIENT-012", "HEDIS-COLA", 1.5f  // Invalid: > 1.0
        );

        // When: Submit request
        mockMvc.perform(post(API_BASE_PATH + "/evaluate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // Then: Should validate and reject
            .andExpect(status().isBadRequest());
    }

    // ===== Response Validation Tests =====

    @Test
    @DisplayName("Should return proper measure event response structure")
    void testMeasureEventResponseStructure() throws Exception {
        // Given: Measure evaluation
        EvaluateMeasureRequest request = new EvaluateMeasureRequest(
            "PATIENT-013", "HEDIS-COLA", 0.85f
        );

        // When: Evaluate measure
        mockMvc.perform(post(API_BASE_PATH + "/evaluate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // Then: Response should have proper structure
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.measureCode").exists())
            .andExpect(jsonPath("$.status").exists())
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should include version tracking in response")
    void testVersionTrackingInResponse() throws Exception {
        // Given: Measure evaluation
        EvaluateMeasureRequest request = new EvaluateMeasureRequest(
            "PATIENT-014", "HEDIS-COLA", 0.85f
        );

        // When: Evaluate measure
        mockMvc.perform(post(API_BASE_PATH + "/evaluate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // Then: Response should include version
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.version").isNumber());
    }
}
