package com.healthdata.hcc.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.audit.repository.AuditEventRepository;
import com.healthdata.audit.service.AuditService;
import com.healthdata.authentication.filter.JwtAuthenticationFilter;
import com.healthdata.authentication.repository.UserRepository;
import com.healthdata.hcc.controller.HccController;
import com.healthdata.hcc.persistence.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for HCC Risk Adjustment API.
 *
 * Tests RAF calculation, HCC profile retrieval, crosswalk lookup,
 * and documentation gap management endpoints.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.kafka.bootstrap-servers=",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration",
        "spring.cache.type=simple",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
        "spring.jpa.properties.hibernate.default_schema=hcc",
        "healthdata.security.jwt.secret=test-secret-key-for-unit-testing-only-minimum-32-chars",
        "healthdata.security.jwt.accessTokenExpirationMs=900000",
        "healthdata.security.jwt.refreshTokenExpirationMs=604800000",
        "hcc.v24-weight=0.33",
        "hcc.v28-weight=0.67"
    }
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Testcontainers(disabledWithoutDocker = true)
class HccApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("hcctest")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
    private AuditService auditService;

    @MockBean
    private AuditEventRepository auditEventRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DiagnosisHccMapRepository diagnosisMapRepository;

    @Autowired
    private PatientHccProfileRepository profileRepository;

    @Autowired
    private DocumentationGapRepository gapRepository;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final int CURRENT_YEAR = java.time.LocalDate.now().getYear();

    @BeforeEach
    void setUp() {
        // Seed test crosswalk data
        seedCrosswalkData();
    }

    private void seedCrosswalkData() {
        // Only seed if not already present
        if (diagnosisMapRepository.findByIcd10Code("E11.9").isEmpty()) {
            List<DiagnosisHccMapEntity> mappings = Arrays.asList(
                DiagnosisHccMapEntity.builder()
                    .icd10Code("E11.9")
                    .hccCodeV24("HCC19")
                    .hccCodeV28("HCC37")
                    .hccNameV24("Diabetes without Complication")
                    .coefficientV24(0.105)
                    .coefficientV28(0.118)
                    .requiresSpecificity(false)
                    .build(),
                DiagnosisHccMapEntity.builder()
                    .icd10Code("I10")
                    .hccCodeV24(null)
                    .hccCodeV28(null)
                    .hccNameV24(null)
                    .coefficientV24(null)
                    .coefficientV28(null)
                    .requiresSpecificity(false)
                    .build(),
                DiagnosisHccMapEntity.builder()
                    .icd10Code("J44.1")
                    .hccCodeV24("HCC111")
                    .hccCodeV28("HCC280")
                    .hccNameV24("Chronic Obstructive Pulmonary Disease")
                    .coefficientV24(0.335)
                    .coefficientV28(0.312)
                    .requiresSpecificity(false)
                    .build(),
                DiagnosisHccMapEntity.builder()
                    .icd10Code("I48.91")
                    .hccCodeV24("HCC96")
                    .hccCodeV28("HCC223")
                    .hccNameV24("Atrial Fibrillation")
                    .coefficientV24(0.288)
                    .coefficientV28(0.256)
                    .requiresSpecificity(false)
                    .build()
            );
            diagnosisMapRepository.saveAll(mappings);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/hcc/patient/{patientId}/calculate tests")
    class CalculateRafTests {

        @Test
        @Order(1)
        @DisplayName("Should calculate RAF score with valid diagnosis codes")
        void calculateRaf_withValidDiagnoses_shouldReturn200() throws Exception {
            HccController.RafCalculationRequest request = new HccController.RafCalculationRequest();
            request.setDiagnosisCodes(Arrays.asList("E11.9", "J44.1"));
            request.setAge(65);
            request.setSex("M");
            request.setDualEligible(false);
            request.setInstitutionalized(false);

            mockMvc.perform(post("/api/v1/hcc/patient/{patientId}/calculate", PATIENT_ID)
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID.toString()))
                .andExpect(jsonPath("$.rafScoreV24").isNumber())
                .andExpect(jsonPath("$.rafScoreV28").isNumber())
                .andExpect(jsonPath("$.rafScoreBlended").isNumber())
                .andExpect(jsonPath("$.hccsV24").isArray())
                .andExpect(jsonPath("$.hccsV28").isArray())
                .andExpect(jsonPath("$.v24Weight").value(0.33))
                .andExpect(jsonPath("$.v28Weight").value(0.67));
        }

        @Test
        @Order(2)
        @DisplayName("Should calculate RAF score with empty diagnosis list")
        void calculateRaf_withNoDiagnoses_shouldReturnBaselineScore() throws Exception {
            HccController.RafCalculationRequest request = new HccController.RafCalculationRequest();
            request.setDiagnosisCodes(Arrays.asList());
            request.setAge(70);
            request.setSex("F");
            request.setDualEligible(true);
            request.setInstitutionalized(false);

            mockMvc.perform(post("/api/v1/hcc/patient/{patientId}/calculate", PATIENT_ID)
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosisCount").value(0))
                .andExpect(jsonPath("$.hccsV24").isEmpty())
                .andExpect(jsonPath("$.rafScoreV24").isNumber());
        }

        @Test
        @Order(3)
        @DisplayName("Should filter non-HCC diagnoses")
        void calculateRaf_withNonHccDiagnosis_shouldFilterOut() throws Exception {
            HccController.RafCalculationRequest request = new HccController.RafCalculationRequest();
            request.setDiagnosisCodes(Arrays.asList("I10")); // Hypertension - not HCC
            request.setAge(65);
            request.setSex("M");
            request.setDualEligible(false);
            request.setInstitutionalized(false);

            mockMvc.perform(post("/api/v1/hcc/patient/{patientId}/calculate", PATIENT_ID)
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosisCount").value(1))
                .andExpect(jsonPath("$.hccCountV24").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/hcc/patient/{patientId}/profile tests")
    class GetProfileTests {

        @Test
        @Order(10)
        @DisplayName("Should return 404 when profile does not exist")
        void getProfile_notFound_shouldReturn404() throws Exception {
            UUID nonExistentPatient = UUID.randomUUID();

            mockMvc.perform(get("/api/v1/hcc/patient/{patientId}/profile", nonExistentPatient)
                    .header("X-Tenant-ID", TENANT_ID)
                    .param("year", String.valueOf(CURRENT_YEAR)))
                .andExpect(status().isNotFound());
        }

        @Test
        @Order(11)
        @DisplayName("Should return profile after RAF calculation creates it")
        void getProfile_afterCalculation_shouldReturnProfile() throws Exception {
            // First calculate RAF to create profile
            UUID patientForProfile = UUID.randomUUID();
            HccController.RafCalculationRequest request = new HccController.RafCalculationRequest();
            request.setDiagnosisCodes(Arrays.asList("E11.9"));
            request.setAge(65);
            request.setSex("M");
            request.setDualEligible(false);
            request.setInstitutionalized(false);

            mockMvc.perform(post("/api/v1/hcc/patient/{patientId}/calculate", patientForProfile)
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

            // Now get the profile
            mockMvc.perform(get("/api/v1/hcc/patient/{patientId}/profile", patientForProfile)
                    .header("X-Tenant-ID", TENANT_ID)
                    .param("year", String.valueOf(CURRENT_YEAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(patientForProfile.toString()))
                .andExpect(jsonPath("$.profileYear").value(CURRENT_YEAR));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/hcc/crosswalk tests")
    class CrosswalkTests {

        @Test
        @Order(20)
        @DisplayName("Should return crosswalk mappings for valid ICD-10 codes")
        void getCrosswalk_withValidCodes_shouldReturnMappings() throws Exception {
            mockMvc.perform(get("/api/v1/hcc/crosswalk")
                    .param("icd10Codes", "E11.9", "J44.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['E11.9']").exists())
                .andExpect(jsonPath("$['E11.9'].hccCodeV24").value("HCC19"))
                .andExpect(jsonPath("$['E11.9'].hccCodeV28").value("HCC37"))
                .andExpect(jsonPath("$['J44.1']").exists())
                .andExpect(jsonPath("$['J44.1'].hccCodeV24").value("HCC111"));
        }

        @Test
        @Order(21)
        @DisplayName("Should return empty map for unknown codes")
        void getCrosswalk_withUnknownCodes_shouldReturnEmptyMap() throws Exception {
            mockMvc.perform(get("/api/v1/hcc/crosswalk")
                    .param("icd10Codes", "INVALID1", "INVALID2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @Order(22)
        @DisplayName("Should return non-HCC codes with null HCC values")
        void getCrosswalk_withNonHccCode_shouldHaveNullHccCodes() throws Exception {
            mockMvc.perform(get("/api/v1/hcc/crosswalk")
                    .param("icd10Codes", "I10", "E11.9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.I10").exists())
                .andExpect(jsonPath("$.I10.hccCodeV24").doesNotExist())  // I10 has no HCC mapping
                .andExpect(jsonPath("$.I10.hccCodeV28").doesNotExist())
                .andExpect(jsonPath("$['E11.9']").exists())
                .andExpect(jsonPath("$['E11.9'].hccCodeV24").value("HCC19"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/hcc/patient/{patientId}/documentation-gaps tests")
    class DocumentationGapsTests {

        @Test
        @Order(30)
        @DisplayName("Should return empty list when no gaps exist")
        void getDocumentationGaps_noGaps_shouldReturnEmptyList() throws Exception {
            UUID patientWithNoGaps = UUID.randomUUID();

            mockMvc.perform(get("/api/v1/hcc/patient/{patientId}/documentation-gaps", patientWithNoGaps)
                    .header("X-Tenant-ID", TENANT_ID)
                    .param("year", String.valueOf(CURRENT_YEAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @Order(31)
        @DisplayName("Should return documentation gaps when they exist")
        void getDocumentationGaps_withGaps_shouldReturnList() throws Exception {
            // Create a documentation gap
            UUID patientWithGaps = UUID.randomUUID();
            DocumentationGapEntity gap = DocumentationGapEntity.builder()
                .tenantId(TENANT_ID)
                .patientId(patientWithGaps)
                .profileYear(CURRENT_YEAR)
                .currentIcd10("E11.9")
                .recommendedIcd10("E11.65")
                .gapType(DocumentationGapEntity.GapType.UNSPECIFIED)
                .rafImpactBlended(new BigDecimal("0.325"))
                .status(DocumentationGapEntity.GapStatus.OPEN)
                .build();
            gapRepository.save(gap);

            mockMvc.perform(get("/api/v1/hcc/patient/{patientId}/documentation-gaps", patientWithGaps)
                    .header("X-Tenant-ID", TENANT_ID)
                    .param("year", String.valueOf(CURRENT_YEAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].currentIcd10").value("E11.9"))
                .andExpect(jsonPath("$[0].recommendedIcd10").value("E11.65"))
                .andExpect(jsonPath("$[0].gapType").value("UNSPECIFIED"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/hcc/opportunities tests")
    class OpportunitiesTests {

        @Test
        @Order(40)
        @DisplayName("Should return high-value opportunities")
        void getOpportunities_shouldReturnFilteredList() throws Exception {
            mockMvc.perform(get("/api/v1/hcc/opportunities")
                    .header("X-Tenant-ID", TENANT_ID)
                    .param("year", String.valueOf(CURRENT_YEAR))
                    .param("minUplift", "0.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("Tenant isolation tests")
    class TenantIsolationTests {

        @Test
        @Order(50)
        @DisplayName("Should not return other tenant's profiles")
        void getProfile_differentTenant_shouldReturn404() throws Exception {
            // Create profile for tenant1
            UUID patientId = UUID.randomUUID();
            PatientHccProfileEntity profile = PatientHccProfileEntity.builder()
                .tenantId("tenant1")
                .patientId(patientId)
                .profileYear(CURRENT_YEAR)
                .rafScoreV24(new BigDecimal("1.5"))
                .rafScoreV28(new BigDecimal("1.4"))
                .rafScoreBlended(new BigDecimal("1.433"))
                .build();
            profileRepository.save(profile);

            // Try to access from tenant2
            mockMvc.perform(get("/api/v1/hcc/patient/{patientId}/profile", patientId)
                    .header("X-Tenant-ID", "tenant2")
                    .param("year", String.valueOf(CURRENT_YEAR)))
                .andExpect(status().isNotFound());
        }
    }
}
