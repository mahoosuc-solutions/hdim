package com.healthdata.cql.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.cql.config.TestRedisConfiguration;
import com.healthdata.cql.entity.CqlEvaluation;
import com.healthdata.cql.entity.CqlLibrary;
import com.healthdata.cql.repository.CqlEvaluationRepository;
import com.healthdata.cql.repository.CqlLibraryRepository;
import com.healthdata.cql.test.CqlTestcontainersBase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CQL Evaluation Controller
 *
 * Tests all CQL evaluation endpoints including:
 * - Creating and executing evaluations
 * - Retrieving evaluation results
 * - Filtering by patient, library, status
 * - Batch operations
 * - Error handling
 * - Multi-tenant isolation
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
public class CqlEvaluationControllerIntegrationTest extends CqlTestcontainersBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CqlEvaluationRepository evaluationRepository;

    @Autowired
    private CqlLibraryRepository libraryRepository;

    private static final String BASE_URL = "/api/v1/cql/evaluations";
    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private CqlLibrary testLibrary;
    private CqlEvaluation testEvaluation;

    @BeforeEach
    void setUp() {
        // Create test library
        testLibrary = new CqlLibrary(TENANT_ID, "TestLibrary", "1.0.0");
        testLibrary.setStatus("ACTIVE");
        testLibrary.setCqlContent("library TestLibrary version '1.0.0'");
        testLibrary = libraryRepository.save(testLibrary);

        // Create test evaluation
        testEvaluation = new CqlEvaluation(TENANT_ID, testLibrary, PATIENT_ID);
        testEvaluation.setStatus("SUCCESS");
        testEvaluation.setEvaluationDate(Instant.now());
        testEvaluation.setDurationMs(100L);
        testEvaluation = evaluationRepository.save(testEvaluation);
    }

    @Test
    @Order(1)
    @DisplayName("Should create and execute evaluation successfully")
    void testCreateAndExecuteEvaluation() throws Exception {
        UUID newPatientId = UUID.randomUUID();
        mockMvc.perform(post(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .param("libraryId", testLibrary.getId().toString())
                .param("patientId", newPatientId.toString()))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.tenantId").value(TENANT_ID))
                .andExpect(jsonPath("$.patientId").value(newPatientId.toString()))
                .andExpect(jsonPath("$.status").isNotEmpty())
                .andExpect(jsonPath("$.evaluationDate").exists())
                .andExpect(jsonPath("$.library.id").value(testLibrary.getId().toString()));
    }

    @Test
    @Order(2)
    @DisplayName("Should fail to create evaluation with invalid library ID")
    void testCreateEvaluationWithInvalidLibrary() throws Exception {
        UUID invalidLibraryId = UUID.randomUUID();

        mockMvc.perform(post(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .param("libraryId", invalidLibraryId.toString())
                .param("patientId", PATIENT_ID.toString()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(3)
    @DisplayName("Should fail to create evaluation without tenant header")
    void testCreateEvaluationWithoutTenant() throws Exception {
        mockMvc.perform(post(BASE_URL)
                .param("libraryId", testLibrary.getId().toString())
                .param("patientId", PATIENT_ID.toString()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(4)
    @DisplayName("Should execute existing evaluation")
    void testExecuteEvaluation() throws Exception {
        // Create a pending evaluation
        CqlEvaluation pendingEval = new CqlEvaluation(TENANT_ID, testLibrary, UUID.randomUUID());
        pendingEval.setStatus("PENDING");
        pendingEval = evaluationRepository.save(pendingEval);

        mockMvc.perform(post(BASE_URL + "/" + pendingEval.getId() + "/execute")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pendingEval.getId().toString()))
                .andExpect(jsonPath("$.status").isNotEmpty())
                .andExpect(jsonPath("$.durationMs").exists());
    }

    @Test
    @Order(5)
    @DisplayName("Should get all evaluations for tenant with pagination")
    void testGetAllEvaluations() throws Exception {
        mockMvc.perform(get(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists())
                .andExpect(jsonPath("$.content[0].tenantId").value(TENANT_ID));
    }

    @Test
    @Order(6)
    @DisplayName("Should get evaluation by ID")
    void testGetEvaluationById() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + testEvaluation.getId())
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testEvaluation.getId().toString()))
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID.toString()))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.library.id").value(testLibrary.getId().toString()));
    }

    @Test
    @Order(7)
    @DisplayName("Should return 404 for non-existent evaluation")
    void testGetNonExistentEvaluation() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get(BASE_URL + "/" + nonExistentId)
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(8)
    @DisplayName("Should get evaluations for patient")
    void testGetEvaluationsForPatient() throws Exception {
        mockMvc.perform(get(BASE_URL + "/patient/" + PATIENT_ID)
                .header("X-Tenant-ID", TENANT_ID)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.content[0].patientId").value(PATIENT_ID.toString()));
    }

    @Test
    @Order(9)
    @DisplayName("Should get evaluations for library")
    void testGetEvaluationsForLibrary() throws Exception {
        mockMvc.perform(get(BASE_URL + "/library/" + testLibrary.getId())
                .header("X-Tenant-ID", TENANT_ID)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.content[0].library.id").value(testLibrary.getId().toString()));
    }

    @Test
    @Order(10)
    @DisplayName("Should get latest evaluation for patient and library")
    void testGetLatestEvaluationForPatientAndLibrary() throws Exception {
        UUID patientId = UUID.randomUUID();
        // Create multiple evaluations
        CqlEvaluation older = new CqlEvaluation(TENANT_ID, testLibrary, patientId);
        older.setEvaluationDate(Instant.now().minus(1, ChronoUnit.HOURS));
        evaluationRepository.save(older);

        CqlEvaluation newer = new CqlEvaluation(TENANT_ID, testLibrary, patientId);
        newer.setEvaluationDate(Instant.now());
        newer = evaluationRepository.save(newer);

        mockMvc.perform(get(BASE_URL + "/patient/" + patientId + "/library/" + testLibrary.getId() + "/latest")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(newer.getId().toString()))
                .andExpect(jsonPath("$.patientId").value(patientId.toString()));
    }

    @Test
    @Order(11)
    @DisplayName("Should get evaluations by status")
    void testGetEvaluationsByStatus() throws Exception {
        mockMvc.perform(get(BASE_URL + "/by-status/SUCCESS")
                .header("X-Tenant-ID", TENANT_ID)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.content[*].status", everyItem(is("SUCCESS"))));
    }

    @Test
    @Order(12)
    @DisplayName("Should get evaluations by date range")
    void testGetEvaluationsByDateRange() throws Exception {
        Instant now = Instant.now();
        Instant start = now.minus(24, ChronoUnit.HOURS);
        Instant end = now.plus(1, ChronoUnit.HOURS);

        mockMvc.perform(get(BASE_URL + "/date-range")
                .header("X-Tenant-ID", TENANT_ID)
                .param("start", start.toString())
                .param("end", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    @Order(13)
    @DisplayName("Should get evaluations for patient by date range")
    void testGetEvaluationsForPatientByDateRange() throws Exception {
        Instant now = Instant.now();
        Instant start = now.minus(24, ChronoUnit.HOURS);
        Instant end = now.plus(1, ChronoUnit.HOURS);

        mockMvc.perform(get(BASE_URL + "/patient/" + PATIENT_ID + "/date-range")
                .header("X-Tenant-ID", TENANT_ID)
                .param("start", start.toString())
                .param("end", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(14)
    @DisplayName("Should get successful evaluations for patient")
    void testGetSuccessfulEvaluationsForPatient() throws Exception {
        mockMvc.perform(get(BASE_URL + "/patient/" + PATIENT_ID + "/successful")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].status", everyItem(is("SUCCESS"))));
    }

    @Test
    @Order(15)
    @DisplayName("Should retry failed evaluation")
    void testRetryEvaluation() throws Exception {
        // Create a failed evaluation
        CqlEvaluation failed = new CqlEvaluation(TENANT_ID, testLibrary, UUID.randomUUID());
        failed.setStatus("FAILED");
        failed.setErrorMessage("Connection timeout");
        failed = evaluationRepository.save(failed);

        mockMvc.perform(post(BASE_URL + "/" + failed.getId() + "/retry")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(failed.getId().toString()))
                .andExpect(jsonPath("$.status").isNotEmpty());
    }

    @Test
    @Order(16)
    @DisplayName("Should fail to retry non-failed evaluation")
    void testRetryNonFailedEvaluation() throws Exception {
        // Try to retry a successful evaluation
        mockMvc.perform(post(BASE_URL + "/" + testEvaluation.getId() + "/retry")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(17)
    @DisplayName("Should perform batch evaluation")
    void testBatchEvaluate() throws Exception {
        List<UUID> patientIds = Arrays.asList(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        String requestBody = objectMapper.writeValueAsString(patientIds);

        mockMvc.perform(post(BASE_URL + "/batch")
                .header("X-Tenant-ID", TENANT_ID)
                .param("libraryId", testLibrary.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].patientId", containsInAnyOrder(
                        patientIds.get(0).toString(),
                        patientIds.get(1).toString(),
                        patientIds.get(2).toString()
                )));
    }

    @Test
    @Order(18)
    @DisplayName("Should get failed evaluations for retry")
    void testGetFailedEvaluationsForRetry() throws Exception {
        // Create some recent failed evaluations
        CqlEvaluation failed1 = new CqlEvaluation(TENANT_ID, testLibrary, UUID.randomUUID());
        failed1.setStatus("FAILED");
        failed1.setEvaluationDate(Instant.now().minus(2, ChronoUnit.HOURS));
        evaluationRepository.save(failed1);

        mockMvc.perform(get(BASE_URL + "/failed-for-retry")
                .header("X-Tenant-ID", TENANT_ID)
                .param("hoursBack", "24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].status", everyItem(is("FAILED"))));
    }

    @Test
    @Order(19)
    @DisplayName("Should count evaluations by status")
    void testCountEvaluationsByStatus() throws Exception {
        mockMvc.perform(get(BASE_URL + "/count/by-status/SUCCESS")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber())
                .andExpect(jsonPath("$").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(20)
    @DisplayName("Should count evaluations for library")
    void testCountEvaluationsForLibrary() throws Exception {
        mockMvc.perform(get(BASE_URL + "/count/library/" + testLibrary.getId())
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber())
                .andExpect(jsonPath("$").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(21)
    @DisplayName("Should count evaluations for patient")
    void testCountEvaluationsForPatient() throws Exception {
        mockMvc.perform(get(BASE_URL + "/count/patient/" + PATIENT_ID)
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber())
                .andExpect(jsonPath("$").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(22)
    @DisplayName("Should get average duration for library")
    void testGetAverageDurationForLibrary() throws Exception {
        mockMvc.perform(get(BASE_URL + "/avg-duration/library/" + testLibrary.getId())
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }

    @Test
    @Order(23)
    @DisplayName("Should delete old evaluations")
    void testDeleteOldEvaluations() throws Exception {
        // Create an old evaluation
        CqlEvaluation old = new CqlEvaluation(TENANT_ID, testLibrary, UUID.randomUUID());
        old.setEvaluationDate(Instant.now().minus(100, ChronoUnit.DAYS));
        evaluationRepository.save(old);

        mockMvc.perform(delete(BASE_URL + "/old")
                .header("X-Tenant-ID", TENANT_ID)
                .param("daysToRetain", "90"))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(24)
    @DisplayName("Should enforce multi-tenant isolation")
    void testMultiTenantIsolation() throws Exception {
        String otherTenant = "other-tenant";

        // Create library and evaluation for other tenant
        CqlLibrary otherLibrary = new CqlLibrary(otherTenant, "OtherLibrary", "1.0.0");
        otherLibrary = libraryRepository.save(otherLibrary);

        CqlEvaluation otherEvaluation = new CqlEvaluation(otherTenant, otherLibrary, UUID.randomUUID());
        otherEvaluation = evaluationRepository.save(otherEvaluation);

        // Try to access other tenant's evaluation with current tenant ID
        mockMvc.perform(get(BASE_URL + "/" + otherEvaluation.getId())
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());

        // Verify own tenant's data is accessible
        mockMvc.perform(get(BASE_URL + "/" + testEvaluation.getId())
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk());
    }

    @Test
    @Order(25)
    @DisplayName("Should handle concurrent evaluations")
    void testConcurrentEvaluations() throws Exception {
        // Create multiple evaluations in quick succession
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post(BASE_URL)
                    .header("X-Tenant-ID", TENANT_ID)
                    .param("libraryId", testLibrary.getId().toString())
                    .param("patientId", UUID.randomUUID().toString()))
                    .andExpect(status().isCreated());
        }

        // Verify all evaluations were created
        mockMvc.perform(get(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .param("page", "0")
                .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(5)));
    }
}
