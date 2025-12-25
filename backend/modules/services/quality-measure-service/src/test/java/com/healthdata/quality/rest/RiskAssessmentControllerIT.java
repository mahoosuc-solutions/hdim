package com.healthdata.quality.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import com.healthdata.quality.config.TestMessagingConfiguration;
import com.healthdata.quality.config.TestWebSocketConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.test.context.support.WithMockUser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.quality.persistence.RiskAssessmentEntity;
import com.healthdata.quality.persistence.RiskAssessmentRepository;

/**
 * Integration tests for RiskAssessmentController REST endpoints.
 * Tests HTTP request/response handling and REST API compliance for risk assessment.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(SpringExtension.class)
@Transactional
@Import({TestMessagingConfiguration.class, TestWebSocketConfiguration.class})
@WithMockUser(username = "testuser", roles = {"EVALUATOR", "ADMIN"})
class RiskAssessmentControllerIT {

    private static final String H2_URL = "jdbc:tc:postgresql:15-alpine:///testdb";
    private static final String TENANT_ID = "tenant-1";
    private static final UUID PATIENT_ID = UUID.fromString("99999999-aaaa-bbbb-cccc-111111111111");
    private static final UUID PATIENT_ID_2 = UUID.fromString("99999999-aaaa-bbbb-cccc-222222222222");

    @DynamicPropertySource
    static void overrideDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> H2_URL);
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.testcontainers.jdbc.ContainerDatabaseDriver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.liquibase.enabled", () -> "false");
        registry.add("spring.flyway.enabled", () -> "false");
        // JWT configuration for tests
        registry.add("jwt.secret", () -> "test-secret-key-that-is-at-least-256-bits-long-for-HS256-algorithm");
        registry.add("jwt.expiration", () -> "3600000");
        registry.add("jwt.issuer", () -> "test-issuer");
        // Disable cache for tests
        registry.add("spring.cache.type", () -> "none");
        // Disable Redis for tests
        registry.add("spring.data.redis.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RiskAssessmentRepository riskAssessmentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        riskAssessmentRepository.deleteAll();
    }

    // ===== GET /api/patients/{patientId}/risk-assessment Tests =====

    @Test
    void getCurrentRiskAssessmentShouldReturn200WhenExists() throws Exception {
        // Given
        RiskAssessmentEntity assessment = createAndSaveRiskAssessment(PATIENT_ID, 65,
            RiskAssessmentEntity.RiskLevel.HIGH, null);

        // When / Then
        MvcResult result = mockMvc.perform(get("/api/patients/" + PATIENT_ID + "/risk-assessment")
                        .header("X-Tenant-ID", TENANT_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID.toString()))
                .andExpect(jsonPath("$.riskScore").value(65))
                .andExpect(jsonPath("$.riskLevel").value("high"))
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);
        assertThat(response.get("patientId").asText()).isEqualTo(PATIENT_ID.toString());
        assertThat(response.get("riskScore").asInt()).isEqualTo(65);
    }

    @Test
    void getCurrentRiskAssessmentShouldReturn404WhenNotExists() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/patients/" + PATIENT_ID + "/risk-assessment")
                        .header("X-Tenant-ID", TENANT_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCurrentRiskAssessmentShouldReturn400WhenTenantIdMissing() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/patients/" + PATIENT_ID + "/risk-assessment")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCurrentRiskAssessmentShouldReturnMostRecentWhenMultipleExist() throws Exception {
        // Given - Create multiple assessments
        createAndSaveRiskAssessment(PATIENT_ID, 45,
            RiskAssessmentEntity.RiskLevel.MODERATE, null, Instant.now().minus(5, ChronoUnit.DAYS));
        createAndSaveRiskAssessment(PATIENT_ID, 65,
            RiskAssessmentEntity.RiskLevel.HIGH, null, Instant.now().minus(1, ChronoUnit.DAYS));
        RiskAssessmentEntity mostRecent = createAndSaveRiskAssessment(PATIENT_ID, 70,
            RiskAssessmentEntity.RiskLevel.HIGH, null, Instant.now());

        // When / Then - Should return the most recent (70)
        mockMvc.perform(get("/api/patients/" + PATIENT_ID + "/risk-assessment")
                        .header("X-Tenant-ID", TENANT_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskScore").value(70))
                .andExpect(jsonPath("$.id").value(mostRecent.getId().toString()));
    }

    // ===== GET /api/patients/{patientId}/risk-history Tests =====

    @Test
    void getRiskHistoryShouldReturn200WithAllAssessments() throws Exception {
        // Given - Create historical assessments
        createAndSaveRiskAssessment(PATIENT_ID, 30,
            RiskAssessmentEntity.RiskLevel.MODERATE, null, Instant.now().minus(10, ChronoUnit.DAYS));
        createAndSaveRiskAssessment(PATIENT_ID, 50,
            RiskAssessmentEntity.RiskLevel.HIGH, null, Instant.now().minus(5, ChronoUnit.DAYS));
        createAndSaveRiskAssessment(PATIENT_ID, 65,
            RiskAssessmentEntity.RiskLevel.HIGH, null, Instant.now());

        // When / Then
        MvcResult result = mockMvc.perform(get("/api/patients/" + PATIENT_ID + "/risk-history")
                        .header("X-Tenant-ID", TENANT_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);
        assertThat(response.isArray()).isTrue();
        assertThat(response.size()).isEqualTo(3);

        // Should be ordered by date descending (most recent first)
        assertThat(response.get(0).get("riskScore").asInt()).isEqualTo(65);
        assertThat(response.get(1).get("riskScore").asInt()).isEqualTo(50);
        assertThat(response.get(2).get("riskScore").asInt()).isEqualTo(30);
    }

    @Test
    void getRiskHistoryShouldReturnEmptyArrayWhenNoHistory() throws Exception {
        // When / Then
        MvcResult result = mockMvc.perform(get("/api/patients/" + PATIENT_ID + "/risk-history")
                        .header("X-Tenant-ID", TENANT_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);
        assertThat(response.isArray()).isTrue();
        assertThat(response.size()).isEqualTo(0);
    }

    // ===== GET /api/patients/{patientId}/risk-by-category/{category} Tests =====

    @Test
    void getRiskByCategoryShouldReturn200WhenExists() throws Exception {
        // Given
        createAndSaveRiskAssessment(PATIENT_ID, 75,
            RiskAssessmentEntity.RiskLevel.VERY_HIGH, "CARDIOVASCULAR");

        // When / Then
        mockMvc.perform(get("/api/patients/" + PATIENT_ID + "/risk-by-category/CARDIOVASCULAR")
                        .header("X-Tenant-ID", TENANT_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID.toString()))
                .andExpect(jsonPath("$.riskScore").value(75))
                .andExpect(jsonPath("$.riskLevel").value("very-high"))
                .andExpect(jsonPath("$.riskCategory").value("CARDIOVASCULAR"));
    }

    @Test
    void getRiskByCategoryShouldReturn404WhenNotExists() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/patients/" + PATIENT_ID + "/risk-by-category/DIABETES")
                        .header("X-Tenant-ID", TENANT_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRiskByCategoryShouldReturnMostRecentForCategory() throws Exception {
        // Given - Multiple assessments for same category
        createAndSaveRiskAssessment(PATIENT_ID, 60,
            RiskAssessmentEntity.RiskLevel.HIGH, "DIABETES",
            Instant.now().minus(5, ChronoUnit.DAYS));
        RiskAssessmentEntity mostRecent = createAndSaveRiskAssessment(PATIENT_ID, 70,
            RiskAssessmentEntity.RiskLevel.HIGH, "DIABETES",
            Instant.now());

        // When / Then
        mockMvc.perform(get("/api/patients/" + PATIENT_ID + "/risk-by-category/DIABETES")
                        .header("X-Tenant-ID", TENANT_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskScore").value(70))
                .andExpect(jsonPath("$.id").value(mostRecent.getId().toString()));
    }

    // ===== POST /api/patients/{patientId}/recalculate-risk Tests =====

    @Test
    void recalculateRiskShouldReturn200AndCreateNewAssessment() throws Exception {
        // When / Then
        MvcResult result = mockMvc.perform(post("/api/patients/" + PATIENT_ID + "/recalculate-risk")
                        .header("X-Tenant-ID", TENANT_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID.toString()))
                .andExpect(jsonPath("$.riskScore").exists())
                .andExpect(jsonPath("$.riskLevel").exists())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);
        assertThat(response.get("patientId").asText()).isEqualTo(PATIENT_ID.toString());

        // Verify a new assessment was created in the database
        List<RiskAssessmentEntity> assessments = riskAssessmentRepository
            .findByTenantIdAndPatientIdOrderByAssessmentDateDesc(TENANT_ID, PATIENT_ID);
        assertThat(assessments).hasSize(1);
    }

    @Test
    void recalculateRiskShouldReturn400WhenTenantIdMissing() throws Exception {
        // When / Then
        mockMvc.perform(post("/api/patients/" + PATIENT_ID + "/recalculate-risk")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recalculateRiskShouldCreateMultipleAssessmentsOverTime() throws Exception {
        // Given - First calculation
        mockMvc.perform(post("/api/patients/" + PATIENT_ID + "/recalculate-risk")
                        .header("X-Tenant-ID", TENANT_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // When - Second calculation
        mockMvc.perform(post("/api/patients/" + PATIENT_ID + "/recalculate-risk")
                        .header("X-Tenant-ID", TENANT_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Then - Should have 2 assessments
        List<RiskAssessmentEntity> assessments = riskAssessmentRepository
            .findByTenantIdAndPatientIdOrderByAssessmentDateDesc(TENANT_ID, PATIENT_ID);
        assertThat(assessments).hasSize(2);
    }

    // ===== GET /api/risk/population-stats Tests =====

    @Test
    void getPopulationStatsShouldReturn200WithStats() throws Exception {
        // Given - Create assessments for multiple patients
        createAndSaveRiskAssessment(PATIENT_ID, 20, RiskAssessmentEntity.RiskLevel.LOW, null);
        createAndSaveRiskAssessment(PATIENT_ID_2, 85, RiskAssessmentEntity.RiskLevel.VERY_HIGH, null);
        createAndSaveRiskAssessment(UUID.fromString("99999999-aaaa-bbbb-cccc-333333333333"), 40,
            RiskAssessmentEntity.RiskLevel.MODERATE, null);
        createAndSaveRiskAssessment(UUID.fromString("99999999-aaaa-bbbb-cccc-444444444444"), 60,
            RiskAssessmentEntity.RiskLevel.HIGH, null);

        // When / Then
        MvcResult result = mockMvc.perform(get("/api/risk/population-stats")
                        .header("X-Tenant-ID", TENANT_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalPatients").value(4))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);

        assertThat(response.get("totalPatients").asInt()).isEqualTo(4);
        assertThat(response.has("riskLevelDistribution")).isTrue();

        JsonNode distribution = response.get("riskLevelDistribution");
        assertThat(distribution.has("low")).isTrue();
        assertThat(distribution.has("moderate")).isTrue();
        assertThat(distribution.has("high")).isTrue();
        assertThat(distribution.has("very-high")).isTrue();
    }

    @Test
    void getPopulationStatsShouldReturn200WithZeroWhenNoData() throws Exception {
        // When / Then
        MvcResult result = mockMvc.perform(get("/api/risk/population-stats")
                        .header("X-Tenant-ID", TENANT_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalPatients").value(0))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);
        assertThat(response.get("totalPatients").asInt()).isEqualTo(0);
    }

    @Test
    void getPopulationStatsShouldReturn400WhenTenantIdMissing() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/risk/population-stats")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ===== Multi-tenant Isolation Tests =====

    @Test
    void shouldEnforceTenantIsolation() throws Exception {
        // Given - Create assessments in different tenants
        String tenant2 = "tenant-2";

        createAndSaveRiskAssessment(PATIENT_ID, 50,
            RiskAssessmentEntity.RiskLevel.HIGH, null, TENANT_ID);

        RiskAssessmentEntity tenant2Assessment = RiskAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenant2)
            .patientId(PATIENT_ID)
            .riskScore(80)
            .riskLevel(RiskAssessmentEntity.RiskLevel.VERY_HIGH)
            .riskFactors(new ArrayList<>())
            .predictedOutcomes(new ArrayList<>())
            .recommendations(new ArrayList<>())
            .assessmentDate(Instant.now())
            .build();
        riskAssessmentRepository.save(tenant2Assessment);

        // When / Then - Tenant-1 should only see their data
        mockMvc.perform(get("/api/patients/" + PATIENT_ID + "/risk-assessment")
                        .header("X-Tenant-ID", TENANT_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskScore").value(50));

        // When / Then - Tenant-2 should only see their data
        mockMvc.perform(get("/api/patients/" + PATIENT_ID + "/risk-assessment")
                        .header("X-Tenant-ID", tenant2)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskScore").value(80));
    }

    @Test
    void populationStatsShouldBeTenantIsolated() throws Exception {
        // Given - Create assessments in different tenants
        String tenant2 = "tenant-2";

        createAndSaveRiskAssessment(PATIENT_ID, 50,
            RiskAssessmentEntity.RiskLevel.HIGH, null, TENANT_ID);
        createAndSaveRiskAssessment(PATIENT_ID_2, 30,
            RiskAssessmentEntity.RiskLevel.MODERATE, null, TENANT_ID);

        RiskAssessmentEntity tenant2Assessment = RiskAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenant2)
            .patientId(UUID.fromString("99999999-aaaa-bbbb-cccc-555555555555"))
            .riskScore(80)
            .riskLevel(RiskAssessmentEntity.RiskLevel.VERY_HIGH)
            .riskFactors(new ArrayList<>())
            .predictedOutcomes(new ArrayList<>())
            .recommendations(new ArrayList<>())
            .assessmentDate(Instant.now())
            .build();
        riskAssessmentRepository.save(tenant2Assessment);

        // When / Then - Tenant-1 should only see 2 patients
        mockMvc.perform(get("/api/risk/population-stats")
                        .header("X-Tenant-ID", TENANT_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPatients").value(2));

        // When / Then - Tenant-2 should only see 1 patient
        mockMvc.perform(get("/api/risk/population-stats")
                        .header("X-Tenant-ID", tenant2)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPatients").value(1));
    }

    // ===== Helper Methods =====

    private RiskAssessmentEntity createAndSaveRiskAssessment(UUID patientId, int riskScore,
                                                              RiskAssessmentEntity.RiskLevel riskLevel,
                                                              String categoryOrNull) {
        // If categoryOrNull is null, treat as category null
        return createAndSaveRiskAssessmentWithCategoryAndTime(patientId, riskScore, riskLevel,
            categoryOrNull, Instant.now(), TENANT_ID);
    }

    private RiskAssessmentEntity createAndSaveRiskAssessment(UUID patientId, int riskScore,
                                                              RiskAssessmentEntity.RiskLevel riskLevel,
                                                              String category, Instant assessmentDate) {
        return createAndSaveRiskAssessmentWithCategoryAndTime(patientId, riskScore, riskLevel,
            category, assessmentDate, TENANT_ID);
    }

    private RiskAssessmentEntity createAndSaveRiskAssessment(UUID patientId, int riskScore,
                                                              RiskAssessmentEntity.RiskLevel riskLevel,
                                                              String category, String tenantId) {
        return createAndSaveRiskAssessmentWithCategoryAndTime(patientId, riskScore, riskLevel,
            category, Instant.now(), tenantId);
    }

    private RiskAssessmentEntity createAndSaveRiskAssessmentWithCategoryAndTime(UUID patientId, int riskScore,
                                                              RiskAssessmentEntity.RiskLevel riskLevel,
                                                              String category, Instant assessmentDate,
                                                              String tenantId) {
        List<Map<String, Object>> riskFactors = new ArrayList<>();
        riskFactors.add(Map.of(
            "factor", "Test Risk Factor",
            "category", "test-category",
            "weight", 10,
            "severity", "moderate",
            "evidence", "Test evidence"
        ));

        List<Map<String, Object>> predictedOutcomes = new ArrayList<>();
        predictedOutcomes.add(Map.of(
            "outcome", "Hospital admission",
            "probability", 0.25,
            "timeframe", "next 90 days"
        ));

        List<String> recommendations = Arrays.asList(
            "Schedule follow-up",
            "Review medications"
        );

        RiskAssessmentEntity entity = RiskAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(patientId)
            .riskCategory(category)
            .riskScore(riskScore)
            .riskLevel(riskLevel)
            .riskFactors(riskFactors)
            .predictedOutcomes(predictedOutcomes)
            .recommendations(recommendations)
            .assessmentDate(assessmentDate)
            .build();

        return riskAssessmentRepository.save(entity);
    }
}
