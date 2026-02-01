package com.healthdata.{{SERVICE_NAME}}.api.v1;

import com.healthdata.{{SERVICE_NAME}}.domain.model.{{ENTITY_CLASS_NAME}};
import com.healthdata.{{SERVICE_NAME}}.domain.repository.{{ENTITY_CLASS_NAME}}Repository;
import com.healthdata.{{SERVICE_NAME}}.dto.{{ENTITY_CLASS_NAME}}Request;
import com.healthdata.{{SERVICE_NAME}}.dto.{{ENTITY_CLASS_NAME}}Response;
import com.healthdata.shared.authentication.test.GatewayTrustTestHeaders;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {{ENTITY_CLASS_NAME}}Controller.
 *
 * Tests use:
 * - Testcontainers for PostgreSQL (no external DB needed)
 * - Gateway trust test headers (no JWT needed)
 * - MockMvc for HTTP testing
 *
 * Best Practices (Phase 21):
 * - Mock external dependencies (@MockBean for RestTemplate/FeignClients)
 * - Deterministic timing (calculate based on mock delays)
 * - Proper status codes (404 for tenant isolation, not 403)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class {{ENTITY_CLASS_NAME}}ControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("{{SERVICE_NAME}}_db")
            .withUsername("healthdata")
            .withPassword("testpassword");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private {{ENTITY_CLASS_NAME}}Repository repository;

    private GatewayTrustTestHeaders testHeaders;
    private static final String TENANT_ID = "test-tenant-001";
    private static final String USER_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        // Create gateway trust test headers (simulates authenticated request from gateway)
        testHeaders = GatewayTrustTestHeaders.builder()
                .userId(UUID.fromString(USER_ID))
                .username("test_admin")
                .tenantIds(TENANT_ID)
                .roles("ADMIN,EVALUATOR")
                .build();

        // Clean database before each test
        repository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("Should create {{ENTITY_CLASS_NAME}} successfully with valid request")
    void shouldCreate{{ENTITY_CLASS_NAME}}Successfully() throws Exception {
        // Given
        String requestBody = """
                {
                    "field1": "value1",
                    "field2": "value2"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/v1/{{RESOURCE_PATH}}")
                        .headers(testHeaders.toHttpHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.tenantId").value(TENANT_ID))
                .andExpect(jsonPath("$.createdBy").value(USER_ID));
    }

    @Test
    @Order(2)
    @DisplayName("Should get {{ENTITY_CLASS_NAME}} by ID with tenant isolation")
    void shouldGet{{ENTITY_CLASS_NAME}}ByIdWithTenantIsolation() throws Exception {
        // Given - Create test entity
        {{ENTITY_CLASS_NAME}} entity = {{ENTITY_CLASS_NAME}}.builder()
                .tenantId(TENANT_ID)
                .createdBy(USER_ID)
                .build();
        {{ENTITY_CLASS_NAME}} saved = repository.save(entity);

        // When & Then - Should return entity for correct tenant
        mockMvc.perform(get("/api/v1/{{RESOURCE_PATH}}/{id}", saved.getId())
                        .headers(testHeaders.toHttpHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.tenantId").value(TENANT_ID));
    }

    @Test
    @Order(3)
    @DisplayName("Should return 404 for entity belonging to different tenant (not 403)")
    void shouldReturn404ForDifferentTenant() throws Exception {
        // Given - Create entity for different tenant
        {{ENTITY_CLASS_NAME}} entity = {{ENTITY_CLASS_NAME}}.builder()
                .tenantId("different-tenant")
                .createdBy("different-user")
                .build();
        {{ENTITY_CLASS_NAME}} saved = repository.save(entity);

        // When & Then - Should return 404 (not 403) to prevent information disclosure
        mockMvc.perform(get("/api/v1/{{RESOURCE_PATH}}/{id}", saved.getId())
                        .headers(testHeaders.toHttpHeaders()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(4)
    @DisplayName("Should update {{ENTITY_CLASS_NAME}} with tenant isolation")
    void shouldUpdate{{ENTITY_CLASS_NAME}}WithTenantIsolation() throws Exception {
        // Given - Create test entity
        {{ENTITY_CLASS_NAME}} entity = {{ENTITY_CLASS_NAME}}.builder()
                .tenantId(TENANT_ID)
                .createdBy(USER_ID)
                .build();
        {{ENTITY_CLASS_NAME}} saved = repository.save(entity);

        String updateRequestBody = """
                {
                    "field1": "updated_value1",
                    "field2": "updated_value2"
                }
                """;

        // When & Then
        mockMvc.perform(put("/api/v1/{{RESOURCE_PATH}}/{id}", saved.getId())
                        .headers(testHeaders.toHttpHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.updatedBy").value(USER_ID));
    }

    @Test
    @Order(5)
    @DisplayName("Should delete {{ENTITY_CLASS_NAME}} with tenant isolation")
    void shouldDelete{{ENTITY_CLASS_NAME}}WithTenantIsolation() throws Exception {
        // Given - Create test entity
        {{ENTITY_CLASS_NAME}} entity = {{ENTITY_CLASS_NAME}}.builder()
                .tenantId(TENANT_ID)
                .createdBy(USER_ID)
                .build();
        {{ENTITY_CLASS_NAME}} saved = repository.save(entity);

        // When & Then
        mockMvc.perform(delete("/api/v1/{{RESOURCE_PATH}}/{id}", saved.getId())
                        .headers(testHeaders.toHttpHeaders()))
                .andExpect(status().isNoContent());

        // Verify deletion
        mockMvc.perform(get("/api/v1/{{RESOURCE_PATH}}/{id}", saved.getId())
                        .headers(testHeaders.toHttpHeaders()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(6)
    @DisplayName("Should return 401 when X-Auth-Validated header is missing")
    void shouldReturn401WhenAuthHeaderMissing() throws Exception {
        // When & Then - Request without gateway trust headers should be rejected
        mockMvc.perform(get("/api/v1/{{RESOURCE_PATH}}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    @DisplayName("Should return 403 when user lacks required role")
    void shouldReturn403WhenUserLacksRole() throws Exception {
        // Given - Create headers with VIEWER role (insufficient for DELETE)
        GatewayTrustTestHeaders viewerHeaders = GatewayTrustTestHeaders.builder()
                .userId(UUID.randomUUID())
                .username("test_viewer")
                .tenantIds(TENANT_ID)
                .roles("VIEWER")  // VIEWER cannot DELETE
                .build();

        {{ENTITY_CLASS_NAME}} entity = {{ENTITY_CLASS_NAME}}.builder()
                .tenantId(TENANT_ID)
                .createdBy(USER_ID)
                .build();
        {{ENTITY_CLASS_NAME}} saved = repository.save(entity);

        // When & Then
        mockMvc.perform(delete("/api/v1/{{RESOURCE_PATH}}/{id}", saved.getId())
                        .headers(viewerHeaders.toHttpHeaders()))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(8)
    @DisplayName("Should get all {{ENTITY_CLASS_NAME}}s for tenant with pagination")
    void shouldGetAll{{ENTITY_CLASS_NAME}}sWithPagination() throws Exception {
        // Given - Create multiple entities for tenant
        for (int i = 0; i < 5; i++) {
            {{ENTITY_CLASS_NAME}} entity = {{ENTITY_CLASS_NAME}}.builder()
                    .tenantId(TENANT_ID)
                    .createdBy(USER_ID)
                    .build();
            repository.save(entity);
        }

        // When & Then - Should return paginated results
        mockMvc.perform(get("/api/v1/{{RESOURCE_PATH}}")
                        .headers(testHeaders.toHttpHeaders())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements").value(5));
    }
}
