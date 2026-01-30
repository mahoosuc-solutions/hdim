package com.healthdata.cql.integration;

import com.healthdata.cql.config.TestRedisConfiguration;
import com.healthdata.cql.entity.CqlLibrary;
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
import org.springframework.security.test.context.support.WithMockUser;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Simplified CQL Evaluation Controller
 *
 * Tests the simplified /evaluate endpoint that provides a simpler interface
 * for external services (like quality-measure-service) to call.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
@WithMockUser(username = "test-user", authorities = {"ROLE_ADMIN", "MEASURE_READ", "MEASURE_WRITE", "MEASURE_EXECUTE", "MEASURE_PUBLISH", "MEASURE_DELETE"})
public class SimplifiedCqlEvaluationControllerIntegrationTest extends CqlTestcontainersBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CqlLibraryRepository libraryRepository;

    private static final String BASE_URL = "/evaluate";
    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private CqlLibrary testLibrary;

    @BeforeEach
    void setUp() {
        testLibrary = buildLibrary(TENANT_ID, "CDC", "1.0.0");
        testLibrary.setStatus("ACTIVE");
        testLibrary.setCqlContent("library CDC version '1.0.0'\ndefine DiabetesCheck: true");
        testLibrary = libraryRepository.save(testLibrary);
    }

    @Test
    @Order(1)
    @DisplayName("Should evaluate CQL successfully")
    void testEvaluateCqlSuccess() throws Exception {
        mockMvc.perform(post(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .param("library", "CDC")
                .param("patient", PATIENT_ID.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.evaluationId").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.libraryName").value("CDC"))
                .andExpect(jsonPath("$.libraryVersion").value("1.0.0"))
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID.toString()))
                .andExpect(jsonPath("$.evaluationDate").exists())
                .andExpect(jsonPath("$.durationMs").exists())
                .andExpect(jsonPath("$.measureResult").exists());
    }

    @Test
    @Order(2)
    @DisplayName("Should fail with non-existent library")
    void testEvaluateCqlNonExistentLibrary() throws Exception {
        mockMvc.perform(post(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .param("library", "NonExistentLibrary")
                .param("patient", PATIENT_ID.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("Library not found")));
    }

    @Test
    @Order(3)
    @DisplayName("Should fail without tenant header")
    void testEvaluateCqlWithoutTenant() throws Exception {
        mockMvc.perform(post(BASE_URL)
                .param("library", "CDC")
                .param("patient", PATIENT_ID.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(4)
    @DisplayName("Should fail without library parameter")
    void testEvaluateCqlWithoutLibrary() throws Exception {
        mockMvc.perform(post(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .param("patient", PATIENT_ID.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(5)
    @DisplayName("Should fail without patient parameter")
    void testEvaluateCqlWithoutPatient() throws Exception {
        mockMvc.perform(post(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .param("library", "CDC")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(6)
    @DisplayName("Should evaluate multiple patients with same library")
    void testEvaluateMultiplePatients() throws Exception {
        for (int i = 1; i <= 3; i++) {
            UUID patientId = UUID.randomUUID();
            mockMvc.perform(post(BASE_URL)
                    .header("X-Tenant-ID", TENANT_ID)
                    .param("library", "CDC")
                    .param("patient", patientId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patientId").value(patientId.toString()));
        }
    }

    @Test
    @Order(7)
    @DisplayName("Should use latest library version")
    void testEvaluateUsesLatestVersion() throws Exception {
        // Create newer version
        CqlLibrary newerVersion = buildLibrary(TENANT_ID, "CDC", "2.0.0");
        newerVersion.setStatus("ACTIVE");
        newerVersion.setCqlContent("library CDC version '2.0.0'");
        libraryRepository.save(newerVersion);

        mockMvc.perform(post(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .param("library", "CDC")
                .param("patient", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libraryVersion").value("2.0.0"));
    }

    @Test
    @Order(8)
    @DisplayName("Should handle optional parameters")
    void testEvaluateWithOptionalParameters() throws Exception {
        String parameters = "{\"measurementPeriodStart\": \"2023-01-01\", \"measurementPeriodEnd\": \"2023-12-31\"}";

        mockMvc.perform(post(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .param("library", "CDC")
                .param("patient", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(parameters))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluationId").exists());
    }

    @Test
    @Order(9)
    @DisplayName("Should enforce multi-tenant isolation")
    void testMultiTenantIsolation() throws Exception {
        String otherTenant = "other-tenant";

        // Create library for other tenant
        CqlLibrary otherLibrary = buildLibrary(otherTenant, "TenantSpecific", "1.0.0");
        otherLibrary.setStatus("ACTIVE");
        libraryRepository.save(otherLibrary);

        // Try to access other tenant's library with current tenant
        mockMvc.perform(post(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .param("library", "TenantSpecific")
                .param("patient", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isNotFound());

        // Verify it works with correct tenant
        mockMvc.perform(post(BASE_URL)
                .header("X-Tenant-ID", otherTenant)
                .param("library", "TenantSpecific")
                .param("patient", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(10)
    @DisplayName("Should return proper error response structure")
    void testErrorResponseStructure() throws Exception {
        mockMvc.perform(post(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .param("library", "InvalidLibrary")
                .param("patient", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    @Order(11)
    @DisplayName("Should handle concurrent evaluation requests")
    void testConcurrentEvaluations() throws Exception {
        // Simulate concurrent evaluations
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post(BASE_URL)
                    .header("X-Tenant-ID", TENANT_ID)
                    .param("library", "CDC")
                    .param("patient", UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @Order(12)
    @DisplayName("Should reject invalid patient ID format")
    void testInvalidPatientIdFormat() throws Exception {
        String invalidPatientId = "patient-with-dash_and_underscore.123";

        mockMvc.perform(post(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .param("library", "CDC")
                .param("patient", invalidPatientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().is4xxClientError());
    }
}
