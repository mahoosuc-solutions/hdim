package com.healthdata.ecr.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.audit.repository.AuditEventRepository;
import com.healthdata.audit.service.AuditService;
import com.healthdata.ecr.controller.EcrController;
import com.healthdata.ecr.persistence.*;
import com.healthdata.ecr.persistence.ElectronicCaseReportEntity.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Electronic Case Reporting (eCR) API.
 *
 * Tests eCR listing, retrieval, evaluation, reprocessing, and cancellation endpoints.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.kafka.bootstrap-servers=",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
        "healthdata.persistence.primary.url=jdbc:h2:mem:ecrtestdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS ecr",
        "healthdata.persistence.primary.username=sa",
        "healthdata.persistence.primary.password=",
        "healthdata.persistence.primary.driver-class-name=org.h2.Driver",
        "healthdata.persistence.primary.pool-name=h2-test-pool",
        "healthdata.persistence.rls-enabled=false",
        "healthdata.security.jwt.secret=test-secret-key-for-unit-testing-only-minimum-32-chars",
        "healthdata.security.jwt.accessTokenExpirationMs=900000",
        "healthdata.security.jwt.refreshTokenExpirationMs=604800000"
    }
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EcrApiIntegrationTest {

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
    private AuditService auditService;

    @MockBean
    private AuditEventRepository auditEventRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ElectronicCaseReportRepository ecrRepository;

    @Autowired
    private RctcTriggerCodeRepository triggerCodeRepository;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static UUID testEcrId;

    @BeforeEach
    void setUp() {
        seedRctcTriggerCodes();
    }

    private void seedRctcTriggerCodes() {
        // Seed RCTC trigger codes if not present
        if (triggerCodeRepository.count() == 0) {
            List<RctcTriggerCodeEntity> codes = Arrays.asList(
                RctcTriggerCodeEntity.builder()
                    .code("U07.1")
                    .codeSystem("2.16.840.1.113883.6.90")
                    .display("COVID-19")
                    .triggerType(RctcTriggerCodeEntity.TriggerType.DIAGNOSIS)
                    .conditionName("COVID-19")
                    .urgency(RctcTriggerCodeEntity.Urgency.WITHIN_24_HOURS)
                    .build(),
                RctcTriggerCodeEntity.builder()
                    .code("B05.9")
                    .codeSystem("2.16.840.1.113883.6.90")
                    .display("Measles")
                    .triggerType(RctcTriggerCodeEntity.TriggerType.DIAGNOSIS)
                    .conditionName("Measles")
                    .urgency(RctcTriggerCodeEntity.Urgency.IMMEDIATE)
                    .build(),
                RctcTriggerCodeEntity.builder()
                    .code("A22.9")
                    .codeSystem("2.16.840.1.113883.6.90")
                    .display("Anthrax")
                    .triggerType(RctcTriggerCodeEntity.TriggerType.DIAGNOSIS)
                    .conditionName("Anthrax")
                    .urgency(RctcTriggerCodeEntity.Urgency.IMMEDIATE)
                    .build(),
                RctcTriggerCodeEntity.builder()
                    .code("94500-6")
                    .codeSystem("2.16.840.1.113883.6.1")
                    .display("SARS-CoV-2 RNA")
                    .triggerType(RctcTriggerCodeEntity.TriggerType.LAB_RESULT)
                    .conditionName("COVID-19")
                    .urgency(RctcTriggerCodeEntity.Urgency.WITHIN_24_HOURS)
                    .build()
            );
            triggerCodeRepository.saveAll(codes);
        }
    }

    private ElectronicCaseReportEntity createTestEcr(String tenantId, UUID patientId, EcrStatus status) {
        ElectronicCaseReportEntity ecr = ElectronicCaseReportEntity.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .triggerCode("U07.1")
            .triggerCodeSystem("2.16.840.1.113883.6.90")
            .triggerDisplay("COVID-19")
            .triggerCategory(TriggerCategory.DIAGNOSIS)
            .conditionName("COVID-19")
            .status(status)
            .urgency(EcrUrgency.WITHIN_24_HOURS)
            .triggerDetectedAt(LocalDateTime.now())
            .build();
        return ecrRepository.save(ecr);
    }

    @Nested
    @DisplayName("GET /api/ecr tests")
    class ListEcrsTests {

        @Test
        @Order(1)
        @DisplayName("Should return paginated list of eCRs")
        void listEcrs_shouldReturnPaginatedResults() throws Exception {
            // Create test eCR
            createTestEcr(TENANT_ID, PATIENT_ID, EcrStatus.PENDING);

            mockMvc.perform(get("/api/ecr")
                    .header("X-Tenant-ID", TENANT_ID)
                    .param("page", "0")
                    .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
        }

        @Test
        @Order(2)
        @DisplayName("Should filter by status")
        void listEcrs_withStatusFilter_shouldReturnFiltered() throws Exception {
            // Create eCRs with different statuses
            createTestEcr(TENANT_ID, UUID.randomUUID(), EcrStatus.PENDING);
            createTestEcr(TENANT_ID, UUID.randomUUID(), EcrStatus.ACKNOWLEDGED);

            mockMvc.perform(get("/api/ecr")
                    .header("X-Tenant-ID", TENANT_ID)
                    .param("status", "PENDING")
                    .param("page", "0")
                    .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].status", everyItem(equalTo("PENDING"))));
        }
    }

    @Nested
    @DisplayName("GET /api/ecr/{ecrId} tests")
    class GetEcrTests {

        @Test
        @Order(10)
        @DisplayName("Should return eCR by ID")
        void getEcr_withValidId_shouldReturnEcr() throws Exception {
            ElectronicCaseReportEntity ecr = createTestEcr(TENANT_ID, PATIENT_ID, EcrStatus.PENDING);
            testEcrId = ecr.getId();

            mockMvc.perform(get("/api/ecr/{ecrId}", testEcrId)
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testEcrId.toString()))
                .andExpect(jsonPath("$.triggerCode").value("U07.1"))
                .andExpect(jsonPath("$.conditionName").value("COVID-19"))
                .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @Order(11)
        @DisplayName("Should return 404 for non-existent eCR")
        void getEcr_notFound_shouldReturn404() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(get("/api/ecr/{ecrId}", nonExistentId)
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/ecr/patient/{patientId} tests")
    class GetPatientEcrsTests {

        @Test
        @Order(20)
        @DisplayName("Should return eCRs for patient")
        void getPatientEcrs_shouldReturnList() throws Exception {
            UUID patientId = UUID.randomUUID();
            createTestEcr(TENANT_ID, patientId, EcrStatus.PENDING);
            createTestEcr(TENANT_ID, patientId, EcrStatus.ACKNOWLEDGED);

            mockMvc.perform(get("/api/ecr/patient/{patientId}", patientId)
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].patientId", everyItem(equalTo(patientId.toString()))));
        }

        @Test
        @Order(21)
        @DisplayName("Should return empty list for patient with no eCRs")
        void getPatientEcrs_noEcrs_shouldReturnEmptyList() throws Exception {
            UUID patientWithNoEcrs = UUID.randomUUID();

            mockMvc.perform(get("/api/ecr/patient/{patientId}", patientWithNoEcrs)
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("POST /api/ecr/evaluate tests")
    class EvaluateCodesTests {

        @Test
        @Order(30)
        @DisplayName("Should create eCRs for reportable diagnosis codes")
        void evaluateCodes_withReportableDiagnosis_shouldCreateEcrs() throws Exception {
            EcrController.EvaluationRequest request = new EcrController.EvaluationRequest();
            request.setPatientId(UUID.randomUUID());
            request.setDiagnosisCodes(Arrays.asList("U07.1", "B05.9")); // COVID-19 and Measles
            request.setLabCodes(null);

            mockMvc.perform(post("/api/ecr/evaluate")
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
        }

        @Test
        @Order(31)
        @DisplayName("Should return empty list for non-reportable codes")
        void evaluateCodes_withNonReportable_shouldReturnEmptyList() throws Exception {
            EcrController.EvaluationRequest request = new EcrController.EvaluationRequest();
            request.setPatientId(UUID.randomUUID());
            request.setDiagnosisCodes(Arrays.asList("J06.9")); // Common cold - not reportable
            request.setLabCodes(null);

            mockMvc.perform(post("/api/ecr/evaluate")
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("POST /api/ecr/{ecrId}/cancel tests")
    class CancelEcrTests {

        @Test
        @Order(40)
        @DisplayName("Should cancel pending eCR")
        void cancelEcr_pending_shouldReturnCancelled() throws Exception {
            ElectronicCaseReportEntity ecr = createTestEcr(TENANT_ID, UUID.randomUUID(), EcrStatus.PENDING);

            mockMvc.perform(post("/api/ecr/{ecrId}/cancel", ecr.getId())
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @Order(41)
        @DisplayName("Should not cancel already submitted eCR")
        void cancelEcr_submitted_shouldReturn400() throws Exception {
            ElectronicCaseReportEntity ecr = createTestEcr(TENANT_ID, UUID.randomUUID(), EcrStatus.SUBMITTED);

            mockMvc.perform(post("/api/ecr/{ecrId}/cancel", ecr.getId())
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isBadRequest());
        }

        @Test
        @Order(42)
        @DisplayName("Should return 404 for non-existent eCR")
        void cancelEcr_notFound_shouldReturn404() throws Exception {
            mockMvc.perform(post("/api/ecr/{ecrId}/cancel", UUID.randomUUID())
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/ecr/{ecrId}/reprocess tests")
    class ReprocessEcrTests {

        @Test
        @Order(50)
        @DisplayName("Should reprocess failed eCR")
        void reprocessEcr_failed_shouldQueueForReprocessing() throws Exception {
            ElectronicCaseReportEntity ecr = ElectronicCaseReportEntity.builder()
                .tenantId(TENANT_ID)
                .patientId(UUID.randomUUID())
                .triggerCode("U07.1")
                .triggerCodeSystem("2.16.840.1.113883.6.90")
                .triggerDisplay("COVID-19")
                .triggerCategory(TriggerCategory.DIAGNOSIS)
                .conditionName("COVID-19")
                .status(EcrStatus.FAILED)
                .urgency(EcrUrgency.WITHIN_24_HOURS)
                .errorMessage("Connection timeout")
                .retryCount(3)
                .triggerDetectedAt(LocalDateTime.now())
                .build();
            ecr = ecrRepository.save(ecr);

            mockMvc.perform(post("/api/ecr/{ecrId}/reprocess", ecr.getId())
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("eCR queued for reprocessing"));
        }

        @Test
        @Order(51)
        @DisplayName("Should not reprocess non-failed eCR")
        void reprocessEcr_notFailed_shouldReturn400() throws Exception {
            ElectronicCaseReportEntity ecr = createTestEcr(TENANT_ID, UUID.randomUUID(), EcrStatus.PENDING);

            mockMvc.perform(post("/api/ecr/{ecrId}/reprocess", ecr.getId())
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Only FAILED eCRs can be reprocessed"));
        }
    }

    @Nested
    @DisplayName("GET /api/ecr/summary tests")
    class SummaryTests {

        @Test
        @Order(60)
        @DisplayName("Should return status counts")
        void getSummary_shouldReturnStatusCounts() throws Exception {
            mockMvc.perform(get("/api/ecr/summary")
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap());
        }
    }

    @Nested
    @DisplayName("GET /api/ecr/conditions tests")
    class ConditionsTests {

        @Test
        @Order(70)
        @DisplayName("Should return monitored conditions")
        void getConditions_shouldReturnSet() throws Exception {
            mockMvc.perform(get("/api/ecr/conditions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasItem("COVID-19")));
        }
    }

    @Nested
    @DisplayName("GET /api/ecr/check-trigger tests")
    class CheckTriggerTests {

        @Test
        @Order(80)
        @DisplayName("Should return trigger details for reportable code")
        void checkTrigger_reportable_shouldReturnDetails() throws Exception {
            mockMvc.perform(get("/api/ecr/check-trigger")
                    .param("code", "U07.1")
                    .param("codeSystem", "2.16.840.1.113883.6.90"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("U07.1"))
                .andExpect(jsonPath("$.isReportable").value(true))
                .andExpect(jsonPath("$.conditionName").value("COVID-19"))
                .andExpect(jsonPath("$.urgency").value("WITHIN_24_HOURS"));
        }

        @Test
        @Order(81)
        @DisplayName("Should return isReportable=false for non-reportable code")
        void checkTrigger_nonReportable_shouldReturnFalse() throws Exception {
            mockMvc.perform(get("/api/ecr/check-trigger")
                    .param("code", "J06.9")
                    .param("codeSystem", "2.16.840.1.113883.6.90"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("J06.9"))
                .andExpect(jsonPath("$.isReportable").value(false))
                .andExpect(jsonPath("$.conditionName").doesNotExist());
        }
    }

    @Nested
    @DisplayName("Tenant isolation tests")
    class TenantIsolationTests {

        @Test
        @Order(90)
        @DisplayName("Should not return other tenant's eCRs")
        void getEcr_differentTenant_shouldReturn404() throws Exception {
            ElectronicCaseReportEntity ecr = createTestEcr("tenant1", UUID.randomUUID(), EcrStatus.PENDING);

            mockMvc.perform(get("/api/ecr/{ecrId}", ecr.getId())
                    .header("X-Tenant-ID", "tenant2"))
                .andExpect(status().isNotFound());
        }

        @Test
        @Order(91)
        @DisplayName("Should not return other tenant's patient eCRs")
        void getPatientEcrs_differentTenant_shouldReturnEmptyList() throws Exception {
            UUID patientId = UUID.randomUUID();
            createTestEcr("tenant1", patientId, EcrStatus.PENDING);

            mockMvc.perform(get("/api/ecr/patient/{patientId}", patientId)
                    .header("X-Tenant-ID", "tenant2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("Urgency level tests")
    class UrgencyTests {

        @Test
        @Order(100)
        @DisplayName("Should assign IMMEDIATE urgency for anthrax")
        void evaluate_anthrax_shouldBeImmediate() throws Exception {
            EcrController.EvaluationRequest request = new EcrController.EvaluationRequest();
            request.setPatientId(UUID.randomUUID());
            request.setDiagnosisCodes(Arrays.asList("A22.9")); // Anthrax
            request.setLabCodes(null);

            mockMvc.perform(post("/api/ecr/evaluate")
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].urgency").value("IMMEDIATE"));
        }
    }
}
