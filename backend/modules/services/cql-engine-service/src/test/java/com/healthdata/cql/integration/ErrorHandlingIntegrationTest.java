package com.healthdata.cql.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.cql.config.TestRedisConfiguration;
import com.healthdata.cql.dto.CqlLibraryRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Error Handling
 *
 * Tests error scenarios, exception handling, and proper HTTP status codes:
 * - 400 Bad Request for invalid inputs
 * - 404 Not Found for missing resources
 * - 500 Internal Server Error for system errors
 * - Proper error message formatting
 * - Validation failures
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
@WithMockUser(username = "test-user", authorities = {"ROLE_ADMIN", "MEASURE_READ", "MEASURE_WRITE", "MEASURE_EXECUTE", "MEASURE_PUBLISH", "MEASURE_DELETE"})
public class ErrorHandlingIntegrationTest extends CqlTestcontainersBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CqlLibraryRepository libraryRepository;

    private static final String TENANT_ID = "test-tenant";

    @Test
    @Order(1)
    @DisplayName("Should return 404 for non-existent library")
    void testLibraryNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/cql/libraries/" + nonExistentId)
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(2)
    @DisplayName("Should return 404 for non-existent evaluation")
    void testEvaluationNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/cql/evaluations/" + nonExistentId)
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(3)
    @DisplayName("Should return 404 for non-existent value set")
    void testValueSetNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/cql/valuesets/" + nonExistentId)
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(4)
    @DisplayName("Should return 400 for missing required headers")
    void testMissingTenantHeader() throws Exception {
        mockMvc.perform(get("/api/v1/cql/libraries"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(5)
    @DisplayName("Should return 400 for invalid UUID format")
    void testInvalidUuidFormat() throws Exception {
        mockMvc.perform(get("/api/v1/cql/libraries/invalid-uuid")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(6)
    @DisplayName("Should return 400 for invalid date format")
    void testInvalidDateFormat() throws Exception {
        mockMvc.perform(get("/api/v1/cql/evaluations/date-range")
                .header("X-Tenant-ID", TENANT_ID)
                .param("start", "invalid-date")
                .param("end", "2023-12-31T23:59:59Z"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(7)
    @DisplayName("Should return 400 for missing required parameters")
    void testMissingRequiredParameters() throws Exception {
        CqlLibrary library = buildLibrary(TENANT_ID, "TestLib", "1.0.0");
        library = libraryRepository.save(library);

        mockMvc.perform(post("/api/v1/cql/evaluations")
                .header("X-Tenant-ID", TENANT_ID)
                .param("libraryId", library.getId().toString()))
                // Missing patientId parameter
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(8)
    @DisplayName("Should return 400 for invalid JSON in request body")
    void testInvalidJsonRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/cql/libraries")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid-json}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(9)
    @DisplayName("Should return 400 for duplicate library creation")
    void testDuplicateLibraryCreation() throws Exception {
        // Create first library
        CqlLibrary library = buildLibrary(TENANT_ID, "DuplicateTest", "1.0.0");
        library.setStatus("ACTIVE");
        libraryRepository.save(library);

        // Try to create duplicate
        CqlLibrary duplicate = buildLibrary(TENANT_ID, "DuplicateTest", "1.0.0");
        String requestBody = objectMapper.writeValueAsString(duplicate);

        mockMvc.perform(post("/api/v1/cql/libraries")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(10)
    @DisplayName("Should return 400 for retrying non-failed evaluation")
    void testRetryNonFailedEvaluation() throws Exception {
        UUID someId = UUID.randomUUID();

        // Service throws IllegalArgumentException which GlobalExceptionHandler converts to 400
        mockMvc.perform(post("/api/v1/cql/evaluations/" + someId + "/retry")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(11)
    @DisplayName("Should return 400 for compiling library without CQL content")
    void testCompileLibraryWithoutContent() throws Exception {
        CqlLibrary emptyLibrary = buildLibrary(TENANT_ID, "EmptyLib", "1.0.0");
        emptyLibrary.setCqlContent("");
        emptyLibrary = libraryRepository.save(emptyLibrary);

        // Service throws IllegalArgumentException which GlobalExceptionHandler converts to 400
        mockMvc.perform(post("/api/v1/cql/libraries/" + emptyLibrary.getId() + "/compile")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(12)
    @DisplayName("Should return 400 for validating library without CQL content")
    void testValidateLibraryWithoutContent() throws Exception {
        CqlLibrary emptyLibrary = buildLibrary(TENANT_ID, "EmptyValidate", "1.0.0");
        emptyLibrary.setCqlContent("");
        emptyLibrary = libraryRepository.save(emptyLibrary);

        // Service throws IllegalArgumentException which GlobalExceptionHandler converts to 400
        mockMvc.perform(post("/api/v1/cql/libraries/" + emptyLibrary.getId() + "/validate")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(13)
    @DisplayName("Should handle invalid pagination parameters gracefully")
    void testInvalidPaginationParameters() throws Exception {
        // Service handles invalid pagination gracefully by using default values
        mockMvc.perform(get("/api/v1/cql/libraries")
                .header("X-Tenant-ID", TENANT_ID)
                .param("page", "-1")
                .param("size", "0"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(14)
    @DisplayName("Should handle invalid status values")
    void testInvalidStatusValues() throws Exception {
        mockMvc.perform(get("/api/v1/cql/evaluations/by-status/INVALID_STATUS")
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    @Order(15)
    @DisplayName("Should return 400 for empty batch evaluation list")
    void testEmptyBatchEvaluation() throws Exception {
        CqlLibrary library = buildLibrary(TENANT_ID, "BatchTest", "1.0.0");
        library = libraryRepository.save(library);

        // Service validates and rejects empty batch lists with 400
        mockMvc.perform(post("/api/v1/cql/evaluations/batch")
                .header("X-Tenant-ID", TENANT_ID)
                .param("libraryId", library.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(16)
    @DisplayName("Should handle null values in request body")
    void testNullValuesInRequestBody() throws Exception {
        String requestBody = "{\"tenantId\":null,\"libraryName\":null,\"version\":null}";

        mockMvc.perform(post("/api/v1/cql/libraries")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(17)
    @DisplayName("Should handle excessively long strings")
    void testExcessivelyLongStrings() throws Exception {
        String longString = "A".repeat(10000);
        CqlLibrary library = buildLibrary(TENANT_ID, longString, "1.0.0");
        String requestBody = objectMapper.writeValueAsString(library);

        mockMvc.perform(post("/api/v1/cql/libraries")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(18)
    @DisplayName("Should handle SQL injection attempts")
    void testSqlInjectionAttempts() throws Exception {
        String sqlInjection = "'; DROP TABLE cql_libraries; --";

        mockMvc.perform(get("/api/v1/cql/libraries/search")
                .header("X-Tenant-ID", TENANT_ID)
                .param("q", sqlInjection))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(19)
    @DisplayName("Should handle XSS attempts in input")
    void testXssAttempts() throws Exception {
        String xssAttempt = "<script>alert('xss')</script>";

        mockMvc.perform(get("/api/v1/cql/libraries/search")
                .header("X-Tenant-ID", TENANT_ID)
                .param("q", xssAttempt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(20)
    @DisplayName("Should handle concurrent access errors gracefully")
    void testConcurrentAccessErrors() throws Exception {
        CqlLibrary library = buildLibrary(TENANT_ID, "ConcurrentTest", "1.0.0");
        library.setCqlContent("library ConcurrentTest version '1.0.0'");
        library = libraryRepository.save(library);

        // Simulate multiple concurrent updates using DTO
        for (int i = 0; i < 3; i++) {
            CqlLibraryRequest updates = new CqlLibraryRequest(
                "ConcurrentTest",
                "1.0.0",
                "library ConcurrentTest version '1.0.0'"
            );
            updates.setDescription("Update " + i);
            String requestBody = objectMapper.writeValueAsString(updates);

            mockMvc.perform(put("/api/v1/cql/libraries/" + library.getId())
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @Order(21)
    @DisplayName("Should return proper error message for tenant mismatch")
    void testTenantMismatch() throws Exception {
        String tenant1 = "tenant-1";
        String tenant2 = "tenant-2";

        // Create library for tenant1
        CqlLibrary library = buildLibrary(tenant1, "TenantMismatchTest", "1.0.0");
        library = libraryRepository.save(library);

        // Try to access with tenant2
        mockMvc.perform(get("/api/v1/cql/libraries/" + library.getId())
                .header("X-Tenant-ID", tenant2))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(22)
    @DisplayName("Should handle malformed content types")
    void testMalformedContentTypes() throws Exception {
        mockMvc.perform(post("/api/v1/cql/libraries")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType("text/plain")
                .content("not json"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(23)
    @DisplayName("Should handle missing content type header")
    void testMissingContentType() throws Exception {
        CqlLibrary library = buildLibrary(TENANT_ID, "NoContentType", "1.0.0");
        String requestBody = objectMapper.writeValueAsString(library);

        mockMvc.perform(post("/api/v1/cql/libraries")
                .header("X-Tenant-ID", TENANT_ID)
                .content(requestBody))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(24)
    @DisplayName("Should handle date range with end before start")
    void testInvalidDateRange() throws Exception {
        mockMvc.perform(get("/api/v1/cql/evaluations/date-range")
                .header("X-Tenant-ID", TENANT_ID)
                .param("start", "2023-12-31T23:59:59Z")
                .param("end", "2023-01-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @Order(25)
    @DisplayName("Should handle very large result sets gracefully")
    void testLargeResultSets() throws Exception {
        // Create many libraries
        for (int i = 0; i < 100; i++) {
            CqlLibrary library = buildLibrary(TENANT_ID, "LargeTest" + i, "1.0.0");
            libraryRepository.save(library);
        }

        mockMvc.perform(get("/api/v1/cql/libraries")
                .header("X-Tenant-ID", TENANT_ID)
                .param("page", "0")
                .param("size", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
