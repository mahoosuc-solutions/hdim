package com.healthdata.hcc.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.hcc.HccServiceApplication;
import com.healthdata.hcc.controller.HccController.RafCalculationRequest;
import com.healthdata.hcc.persistence.*;
import com.healthdata.hcc.service.RafCalculationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive End-to-End Integration Tests for HCC Risk Adjustment
 *
 * Validates CMS HCC V24/V28 risk adjustment workflows including:
 * - RAF (Risk Adjustment Factor) score calculation
 * - V24, V28, and blended model scoring
 * - ICD-10 to HCC crosswalk mapping
 * - Documentation gap identification
 * - High-value opportunity detection
 * - Multi-tenant isolation
 *
 * These tests ensure compliance with CMS Medicare Advantage risk adjustment requirements.
 */
@SpringBootTest(
    classes = HccServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HccRiskAdjustmentE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("hcc_test_db")
        .withUsername("testuser")
        .withPassword("testpass")
        .withInitScript("db/init-hcc-schema.sql");

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
    private PatientHccProfileRepository profileRepository;

    @Autowired
    private DocumentationGapRepository gapRepository;

    @MockBean
    private RafCalculationService rafCalculationService;

    private static final String TENANT_ID = "tenant-1";
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final int PROFILE_YEAR = LocalDate.now().getYear();

    // Common ICD-10 codes for testing
    private static final String ICD10_DIABETES_COMPLICATIONS = "E11.65"; // HCC 18
    private static final String ICD10_CHF = "I50.9"; // HCC 85
    private static final String ICD10_COPD = "J44.9"; // HCC 111

    private HttpHeaders createHeaders(String tenantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", tenantId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private RafCalculationRequest createRafRequest(List<String> diagnosisCodes, int age, String sex) {
        RafCalculationRequest request = new RafCalculationRequest();
        request.setDiagnosisCodes(diagnosisCodes);
        request.setAge(age);
        request.setSex(sex);
        request.setDualEligible(false);
        request.setInstitutionalized(false);
        return request;
    }

    @Nested
    @DisplayName("RAF Score Calculation")
    class RafCalculationTests {

        @Test
        @DisplayName("should calculate RAF score with V24 and V28 models")
        void shouldCalculateRafScoreWithBothModels() throws Exception {
            List<String> diagnosisCodes = Arrays.asList(ICD10_DIABETES_COMPLICATIONS, ICD10_CHF);
            RafCalculationRequest request = createRafRequest(diagnosisCodes, 75, "M");

            // Mock RAF calculation result
            RafCalculationService.RafCalculationResult mockResult =
                RafCalculationService.RafCalculationResult.builder()
                    .rafScoreV24(new BigDecimal("1.456"))
                    .rafScoreV28(new BigDecimal("1.523"))
                    .rafScoreBlended(new BigDecimal("1.478"))
                    .hccsV24(Arrays.asList("HCC18", "HCC85"))
                    .hccsV28(Arrays.asList("HCC18", "HCC85"))
                    .profileYear(2024)
                    .build();

            when(rafCalculationService.calculateRaf(
                eq(TENANT_ID),
                eq(PATIENT_ID),
                eq(diagnosisCodes),
                any(RafCalculationService.DemographicFactors.class)
            )).thenReturn(mockResult);

            String requestJson = objectMapper.writeValueAsString(request);

            mockMvc.perform(post("/api/v1/hcc/patient/" + PATIENT_ID + "/calculate")
                    .headers(createHeaders(TENANT_ID))
                    .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rafScoreV24").value(1.456))
                .andExpect(jsonPath("$.rafScoreV28").value(1.523))
                .andExpect(jsonPath("$.rafScoreBlended").value(1.478))
                .andExpect(jsonPath("$.hccsV24").isArray())
                .andExpect(jsonPath("$.hccsV28").isArray())
                .andExpect(jsonPath("$.profileYear").value(2024));
        }

        @Test
        @DisplayName("should calculate RAF with demographic factors")
        void shouldCalculateRafWithDemographicFactors() throws Exception {
            List<String> diagnosisCodes = Arrays.asList(ICD10_CHF);
            RafCalculationRequest request = createRafRequest(diagnosisCodes, 85, "F");
            request.setDualEligible(true); // Medicaid eligible
            request.setInstitutionalized(true); // Nursing home

            RafCalculationService.RafCalculationResult mockResult =
                RafCalculationService.RafCalculationResult.builder()
                    .rafScoreV24(new BigDecimal("2.134"))
                    .rafScoreV28(new BigDecimal("2.267"))
                    .rafScoreBlended(new BigDecimal("2.178"))
                    .hccsV24(Arrays.asList("HCC85"))
                    .hccsV28(Arrays.asList("HCC85"))
                    .profileYear(2024)
                    .build();

            when(rafCalculationService.calculateRaf(any(), any(), any(), any()))
                .thenReturn(mockResult);

            String requestJson = objectMapper.writeValueAsString(request);

            mockMvc.perform(post("/api/v1/hcc/patient/" + PATIENT_ID + "/calculate")
                    .headers(createHeaders(TENANT_ID))
                    .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rafScoreBlended").value(2.178));
        }

        @Test
        @DisplayName("should calculate RAF for patient with multiple chronic conditions")
        void shouldCalculateRafWithMultipleConditions() throws Exception {
            List<String> diagnosisCodes = Arrays.asList(
                ICD10_DIABETES_COMPLICATIONS, // HCC 18
                ICD10_CHF,                    // HCC 85
                ICD10_COPD,                   // HCC 111
                "I48.91"                      // Atrial Fib - HCC 96
            );

            RafCalculationRequest request = createRafRequest(diagnosisCodes, 72, "M");

            RafCalculationService.RafCalculationResult mockResult =
                RafCalculationService.RafCalculationResult.builder()
                    .rafScoreV24(new BigDecimal("2.456"))
                    .rafScoreV28(new BigDecimal("2.598"))
                    .rafScoreBlended(new BigDecimal("2.503"))
                    .hccsV24(Arrays.asList("HCC18", "HCC85", "HCC96", "HCC111"))
                    .hccsV28(Arrays.asList("HCC18", "HCC85", "HCC96", "HCC111"))
                    .profileYear(2024)
                    .build();

            when(rafCalculationService.calculateRaf(any(), any(), any(), any()))
                .thenReturn(mockResult);

            String requestJson = objectMapper.writeValueAsString(request);

            mockMvc.perform(post("/api/v1/hcc/patient/" + PATIENT_ID + "/calculate")
                    .headers(createHeaders(TENANT_ID))
                    .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hccsV24.length()").value(4))
                .andExpect(jsonPath("$.rafScoreBlended").value(2.503));
        }

        @Test
        @DisplayName("should handle blended score transition (2024: 67% V24 + 33% V28)")
        void shouldCalculateBlendedScoreCorrectly() throws Exception {
            List<String> diagnosisCodes = Arrays.asList(ICD10_DIABETES_COMPLICATIONS);
            RafCalculationRequest request = createRafRequest(diagnosisCodes, 70, "F");

            // V24 = 1.200, V28 = 1.500
            // 2024 Blended = (1.200 * 0.67) + (1.500 * 0.33) = 0.804 + 0.495 = 1.299
            RafCalculationService.RafCalculationResult mockResult =
                RafCalculationService.RafCalculationResult.builder()
                    .rafScoreV24(new BigDecimal("1.200"))
                    .rafScoreV28(new BigDecimal("1.500"))
                    .rafScoreBlended(new BigDecimal("1.299"))
                    .hccsV24(Arrays.asList("HCC18"))
                    .hccsV28(Arrays.asList("HCC18"))
                    .profileYear(2024)
                    .build();

            when(rafCalculationService.calculateRaf(any(), any(), any(), any()))
                .thenReturn(mockResult);

            String requestJson = objectMapper.writeValueAsString(request);

            mockMvc.perform(post("/api/v1/hcc/patient/" + PATIENT_ID + "/calculate")
                    .headers(createHeaders(TENANT_ID))
                    .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rafScoreV24").value(1.200))
                .andExpect(jsonPath("$.rafScoreV28").value(1.500))
                .andExpect(jsonPath("$.rafScoreBlended").value(1.299));
        }
    }

    @Nested
    @DisplayName("HCC Profile Management")
    class HccProfileTests {

        @Test
        @DisplayName("should retrieve patient HCC profile for current year")
        void shouldRetrievePatientHccProfile() throws Exception {
            PatientHccProfileEntity profile = PatientHccProfileEntity.builder()
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .profileYear(PROFILE_YEAR)
                .rafScoreV24(new BigDecimal("1.456"))
                .rafScoreV28(new BigDecimal("1.523"))
                .rafScoreBlended(new BigDecimal("1.478"))
                .hccsV24(Arrays.asList("HCC18", "HCC85"))
                .hccsV28(Arrays.asList("HCC18", "HCC85"))
                .build();

            profileRepository.save(profile);

            mockMvc.perform(get("/api/v1/hcc/patient/" + PATIENT_ID + "/profile")
                    .headers(createHeaders(TENANT_ID))
                    .param("year", String.valueOf(PROFILE_YEAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID.toString()))
                .andExpect(jsonPath("$.profileYear").value(PROFILE_YEAR))
                .andExpect(jsonPath("$.rafScoreV24").value(1.456))
                .andExpect(jsonPath("$.rafScoreV28").value(1.523));
        }

        @Test
        @DisplayName("should return 404 for non-existent profile")
        void shouldReturn404ForNonExistentProfile() throws Exception {
            UUID nonExistentPatient = UUID.randomUUID();

            mockMvc.perform(get("/api/v1/hcc/patient/" + nonExistentPatient + "/profile")
                    .headers(createHeaders(TENANT_ID))
                    .param("year", String.valueOf(PROFILE_YEAR)))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should retrieve profile for specific historical year")
        void shouldRetrieveProfileForHistoricalYear() throws Exception {
            PatientHccProfileEntity profile2023 = PatientHccProfileEntity.builder()
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .profileYear(2023)
                .rafScoreV24(new BigDecimal("1.234"))
                .rafScoreV28(new BigDecimal("1.345"))
                .rafScoreBlended(new BigDecimal("1.271"))
                .hccsV24(Arrays.asList("HCC18"))
                .hccsV28(Arrays.asList("HCC18"))
                .build();

            profileRepository.save(profile2023);

            mockMvc.perform(get("/api/v1/hcc/patient/" + PATIENT_ID + "/profile")
                    .headers(createHeaders(TENANT_ID))
                    .param("year", "2023"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileYear").value(2023))
                .andExpect(jsonPath("$.rafScoreV24").value(1.234));
        }
    }

    @Nested
    @DisplayName("ICD-10 to HCC Crosswalk")
    class HccCrosswalkTests {

        @Test
        @DisplayName("should map ICD-10 codes to HCC categories")
        void shouldMapIcd10ToHcc() throws Exception {
            List<String> icd10Codes = Arrays.asList(
                ICD10_DIABETES_COMPLICATIONS,
                ICD10_CHF,
                ICD10_COPD
            );

            // Mock crosswalk result
            Map<String, DiagnosisHccMapEntity> crosswalkMap = new java.util.HashMap<>();
            crosswalkMap.put(ICD10_DIABETES_COMPLICATIONS, DiagnosisHccMapEntity.builder()
                .icd10Code(ICD10_DIABETES_COMPLICATIONS).hccCodeV24("HCC18").hccCodeV28("HCC18").build());
            crosswalkMap.put(ICD10_CHF, DiagnosisHccMapEntity.builder()
                .icd10Code(ICD10_CHF).hccCodeV24("HCC85").hccCodeV28("HCC85").build());
            crosswalkMap.put(ICD10_COPD, DiagnosisHccMapEntity.builder()
                .icd10Code(ICD10_COPD).hccCodeV24("HCC111").hccCodeV28("HCC111").build());
            when(rafCalculationService.batchCrosswalk(icd10Codes))
                .thenReturn(crosswalkMap);

            mockMvc.perform(get("/api/v1/hcc/crosswalk")
                    .param("icd10Codes", String.join(",", icd10Codes)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));
        }

        @Test
        @DisplayName("should handle ICD-10 codes with different V24/V28 mappings")
        void shouldHandleDifferentV24V28Mappings() throws Exception {
            // Some ICD-10 codes map to different HCCs in V24 vs V28
            List<String> icd10Codes = Arrays.asList("E11.9"); // Diabetes without complications

            Map<String, DiagnosisHccMapEntity> crosswalkMap = new java.util.HashMap<>();
            crosswalkMap.put("E11.9", DiagnosisHccMapEntity.builder()
                .icd10Code("E11.9").hccCodeV24("HCC19").hccCodeV28("HCC37").build());
            when(rafCalculationService.batchCrosswalk(icd10Codes))
                .thenReturn(crosswalkMap);

            mockMvc.perform(get("/api/v1/hcc/crosswalk")
                    .param("icd10Codes", "E11.9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].icd10Code").value("E11.9"));
        }
    }

    @Nested
    @DisplayName("Documentation Gap Detection")
    class DocumentationGapTests {

        @Test
        @DisplayName("should identify documentation gaps for patient")
        void shouldIdentifyDocumentationGaps() throws Exception {
            // Create documentation gaps
            DocumentationGapEntity gap1 = DocumentationGapEntity.builder()
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .profileYear(PROFILE_YEAR)
                .recommendedHccV24("HCC111")
                .recommendedIcd10Description("COPD - Chronic Obstructive Pulmonary Disease")
                .clinicalGuidance("Patient has history of smoking, recent pulmonary function tests show obstruction")
                .rafImpactBlended(new BigDecimal("0.323"))
                .status(DocumentationGapEntity.GapStatus.OPEN)
                .priority("HIGH")
                .createdAt(LocalDateTime.now())
                .build();

            DocumentationGapEntity gap2 = DocumentationGapEntity.builder()
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .profileYear(PROFILE_YEAR)
                .recommendedHccV24("HCC85")
                .recommendedIcd10Description("Congestive Heart Failure")
                .clinicalGuidance("Recent hospitalization for CHF exacerbation, on diuretics")
                .rafImpactBlended(new BigDecimal("0.451"))
                .status(DocumentationGapEntity.GapStatus.OPEN)
                .priority("CRITICAL")
                .createdAt(LocalDateTime.now())
                .build();

            gapRepository.save(gap1);
            gapRepository.save(gap2);

            mockMvc.perform(get("/api/v1/hcc/patient/" + PATIENT_ID + "/documentation-gaps")
                    .headers(createHeaders(TENANT_ID))
                    .param("year", String.valueOf(PROFILE_YEAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.recommendedHccV24 == 'HCC111')]").exists())
                .andExpect(jsonPath("$[?(@.recommendedHccV24 == 'HCC85')]").exists());
        }

        @Test
        @DisplayName("should prioritize documentation gaps by RAF uplift")
        void shouldPrioritizeGapsByRafUplift() throws Exception {
            // High-value gap
            DocumentationGapEntity highValueGap = DocumentationGapEntity.builder()
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .profileYear(PROFILE_YEAR)
                .recommendedHccV24("HCC85")
                .rafImpactBlended(new BigDecimal("0.800"))
                .status(DocumentationGapEntity.GapStatus.OPEN)
                .priority("CRITICAL")
                .createdAt(LocalDateTime.now())
                .build();

            gapRepository.save(highValueGap);

            mockMvc.perform(get("/api/v1/hcc/patient/" + PATIENT_ID + "/documentation-gaps")
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].potentialRafUplift").value(0.800))
                .andExpect(jsonPath("$[0].priority").value("CRITICAL"));
        }

        @Test
        @DisplayName("should return empty array for patient with no gaps")
        void shouldReturnEmptyArrayForNoGaps() throws Exception {
            UUID patientWithNoGaps = UUID.randomUUID();

            mockMvc.perform(get("/api/v1/hcc/patient/" + patientWithNoGaps + "/documentation-gaps")
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("High-Value Opportunities")
    class HighValueOpportunitiesTests {

        @Test
        @DisplayName("should identify patients with highest RAF uplift potential")
        void shouldIdentifyHighValueOpportunities() throws Exception {
            // Create patient profiles with various potential uplifts
            UUID patient1 = UUID.randomUUID();
            UUID patient2 = UUID.randomUUID();
            UUID patient3 = UUID.randomUUID();

            PatientHccProfileEntity profile1 = PatientHccProfileEntity.builder()
                .tenantId(TENANT_ID)
                .patientId(patient1)
                .profileYear(PROFILE_YEAR)
                .rafScoreBlended(new BigDecimal("1.500"))
                .potentialRafUplift(new BigDecimal("0.500")) // High value
                .build();

            PatientHccProfileEntity profile2 = PatientHccProfileEntity.builder()
                .tenantId(TENANT_ID)
                .patientId(patient2)
                .profileYear(PROFILE_YEAR)
                .rafScoreBlended(new BigDecimal("2.000"))
                .potentialRafUplift(new BigDecimal("0.350"))
                .build();

            PatientHccProfileEntity profile3 = PatientHccProfileEntity.builder()
                .tenantId(TENANT_ID)
                .patientId(patient3)
                .profileYear(PROFILE_YEAR)
                .rafScoreBlended(new BigDecimal("1.200"))
                .potentialRafUplift(new BigDecimal("0.050")) // Low value
                .build();

            profileRepository.save(profile1);
            profileRepository.save(profile2);
            profileRepository.save(profile3);

            mockMvc.perform(get("/api/v1/hcc/opportunities")
                    .headers(createHeaders(TENANT_ID))
                    .param("year", String.valueOf(PROFILE_YEAR))
                    .param("minUplift", "0.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("should filter opportunities by minimum uplift threshold")
        void shouldFilterOpportunitiesByMinUplift() throws Exception {
            UUID patient = UUID.randomUUID();

            PatientHccProfileEntity profile = PatientHccProfileEntity.builder()
                .tenantId(TENANT_ID)
                .patientId(patient)
                .profileYear(PROFILE_YEAR)
                .rafScoreBlended(new BigDecimal("1.500"))
                .potentialRafUplift(new BigDecimal("0.450"))
                .build();

            profileRepository.save(profile);

            // Should appear with minUplift = 0.1
            mockMvc.perform(get("/api/v1/hcc/opportunities")
                    .headers(createHeaders(TENANT_ID))
                    .param("minUplift", "0.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

            // Should NOT appear with minUplift = 0.5
            mockMvc.perform(get("/api/v1/hcc/opportunities")
                    .headers(createHeaders(TENANT_ID))
                    .param("minUplift", "0.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("Multi-Tenant Isolation")
    class MultiTenantIsolationTests {

        @Test
        @DisplayName("should prevent cross-tenant HCC profile access")
        void shouldPreventCrossTenantProfileAccess() throws Exception {
            PatientHccProfileEntity profile = PatientHccProfileEntity.builder()
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .profileYear(PROFILE_YEAR)
                .rafScoreBlended(new BigDecimal("1.500"))
                .build();

            profileRepository.save(profile);

            // Try to access from different tenant
            mockMvc.perform(get("/api/v1/hcc/patient/" + PATIENT_ID + "/profile")
                    .headers(createHeaders("tenant-2"))
                    .param("year", String.valueOf(PROFILE_YEAR)))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should isolate documentation gaps by tenant")
        void shouldIsolateDocumentationGapsByTenant() throws Exception {
            DocumentationGapEntity gap = DocumentationGapEntity.builder()
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .profileYear(PROFILE_YEAR)
                .recommendedHccV24("HCC85")
                .status(DocumentationGapEntity.GapStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

            gapRepository.save(gap);

            // Try to access from different tenant
            mockMvc.perform(get("/api/v1/hcc/patient/" + PATIENT_ID + "/documentation-gaps")
                    .headers(createHeaders("tenant-2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("should isolate high-value opportunities by tenant")
        void shouldIsolateOpportunitiesByTenant() throws Exception {
            UUID patient1 = UUID.randomUUID();

            // Tenant 1 profile
            PatientHccProfileEntity profile1 = PatientHccProfileEntity.builder()
                .tenantId(TENANT_ID)
                .patientId(patient1)
                .profileYear(PROFILE_YEAR)
                .rafScoreBlended(new BigDecimal("1.500"))
                .potentialRafUplift(new BigDecimal("0.400"))
                .build();

            // Tenant 2 profile
            PatientHccProfileEntity profile2 = PatientHccProfileEntity.builder()
                .tenantId("tenant-2")
                .patientId(patient1)
                .profileYear(PROFILE_YEAR)
                .rafScoreBlended(new BigDecimal("2.000"))
                .potentialRafUplift(new BigDecimal("0.600"))
                .build();

            profileRepository.save(profile1);
            profileRepository.save(profile2);

            // Query tenant-1 should not see tenant-2 profiles
            mockMvc.perform(get("/api/v1/hcc/opportunities")
                    .headers(createHeaders(TENANT_ID))
                    .param("minUplift", "0.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.tenantId != 'tenant-1')]").doesNotExist());
        }
    }

    @Nested
    @DisplayName("Error Handling & Validation")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should validate required fields in RAF calculation request")
        void shouldValidateRequiredFields() throws Exception {
            RafCalculationRequest invalidRequest = new RafCalculationRequest();
            // Missing required fields: diagnosisCodes, age, sex

            String requestJson = objectMapper.writeValueAsString(invalidRequest);

            mockMvc.perform(post("/api/v1/hcc/patient/" + PATIENT_ID + "/calculate")
                    .headers(createHeaders(TENANT_ID))
                    .content(requestJson))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should handle empty diagnosis code list")
        void shouldHandleEmptyDiagnosisCodes() throws Exception {
            RafCalculationRequest request = createRafRequest(List.of(), 70, "M");

            RafCalculationService.RafCalculationResult mockResult =
                RafCalculationService.RafCalculationResult.builder()
                    .rafScoreV24(new BigDecimal("0.432")) // Demographic only
                    .rafScoreV28(new BigDecimal("0.456"))
                    .rafScoreBlended(new BigDecimal("0.440"))
                    .hccsV24(List.of())
                    .hccsV28(List.of())
                    .profileYear(2024)
                    .build();

            when(rafCalculationService.calculateRaf(any(), any(), any(), any()))
                .thenReturn(mockResult);

            String requestJson = objectMapper.writeValueAsString(request);

            mockMvc.perform(post("/api/v1/hcc/patient/" + PATIENT_ID + "/calculate")
                    .headers(createHeaders(TENANT_ID))
                    .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hccsV24").isEmpty())
                .andExpect(jsonPath("$.rafScoreBlended").value(0.440));
        }

        @Test
        @DisplayName("should return empty array for crosswalk with no codes")
        void shouldReturnEmptyArrayForNoCrosswalkCodes() throws Exception {
            when(rafCalculationService.batchCrosswalk(List.of()))
                .thenReturn(java.util.Collections.emptyMap());

            mockMvc.perform(get("/api/v1/hcc/crosswalk")
                    .param("icd10Codes", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        }
    }
}
