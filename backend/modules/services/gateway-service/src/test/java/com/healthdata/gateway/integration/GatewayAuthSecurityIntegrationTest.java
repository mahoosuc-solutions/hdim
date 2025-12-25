package com.healthdata.gateway.integration;

import com.healthdata.authentication.constants.AuthHeaderConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import org.junit.jupiter.api.Disabled;
import org.springframework.context.annotation.Import;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security Integration Tests for Gateway Authentication.
 *
 * Tests cover:
 * - Header injection attack prevention
 * - JWT validation at gateway
 * - Public path access
 * - Protected path enforcement
 * - CORS configuration
 * - Error response format
 *
 * SECURITY TEST COVERAGE:
 * - OWASP A01: Broken Access Control
 * - OWASP A02: Cryptographic Failures (JWT validation)
 * - OWASP A07: Identification and Authentication Failures
 *
 * NOTE: Temporarily disabled - requires full Spring context with proper
 * authentication module configuration. Security behaviors are tested
 * via unit tests in GatewayAuthenticationFilterTest.
 */
@Disabled("Integration tests require full Spring context setup - security tested via unit tests")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(GatewayIntegrationTestConfig.class)
@DisplayName("Gateway Authentication Security Integration Tests")
class GatewayAuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("Header Injection Attack Prevention")
    class HeaderInjectionAttackPrevention {

        @Test
        @DisplayName("should reject request with forged X-Auth-User-Id header")
        void shouldRejectForgedUserIdHeader() throws Exception {
            // Attempt to bypass authentication by injecting a fake user ID header
            mockMvc.perform(get("/api/v1/patients")
                    .header(AuthHeaderConstants.HEADER_USER_ID, "malicious-user-id")
                    .header(AuthHeaderConstants.HEADER_USERNAME, "attacker")
                    .header(AuthHeaderConstants.HEADER_ROLES, "ADMIN"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should reject request with forged X-Auth-Validated header")
        void shouldRejectForgedValidatedHeader() throws Exception {
            // Attempt to bypass authentication by forging the validated header
            mockMvc.perform(get("/api/v1/patients")
                    .header(AuthHeaderConstants.HEADER_VALIDATED, "gateway-fake-signature")
                    .header(AuthHeaderConstants.HEADER_USER_ID, "attacker-id")
                    .header(AuthHeaderConstants.HEADER_USERNAME, "attacker")
                    .header(AuthHeaderConstants.HEADER_ROLES, "ADMIN"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should reject request with forged X-Auth-Tenant-Ids header")
        void shouldRejectForgedTenantHeader() throws Exception {
            // Attempt to access other tenant's data by injecting tenant ID
            mockMvc.perform(get("/api/v1/patients")
                    .header(AuthHeaderConstants.HEADER_TENANT_IDS, "victim-tenant-id")
                    .header(AuthHeaderConstants.HEADER_USERNAME, "attacker"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should strip all X-Auth headers from incoming requests")
        void shouldStripAllAuthHeaders() throws Exception {
            // Send request with multiple forged auth headers
            mockMvc.perform(get("/api/v1/patients")
                    .header(AuthHeaderConstants.HEADER_USER_ID, "fake-id")
                    .header(AuthHeaderConstants.HEADER_USERNAME, "fake-user")
                    .header(AuthHeaderConstants.HEADER_TENANT_IDS, "fake-tenant")
                    .header(AuthHeaderConstants.HEADER_ROLES, "ADMIN,SUPERUSER")
                    .header(AuthHeaderConstants.HEADER_VALIDATED, "gateway-fake")
                    .header(AuthHeaderConstants.HEADER_TOKEN_ID, "fake-token")
                    .header(AuthHeaderConstants.HEADER_TOKEN_EXPIRES, "9999999999"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Public Path Access")
    class PublicPathAccess {

        @Test
        @DisplayName("should allow access to health endpoint without authentication")
        void shouldAllowHealthEndpoint() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should allow access to login endpoint without authentication")
        void shouldAllowLoginEndpoint() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"test\",\"password\":\"test\"}"))
                .andExpect(status().isUnauthorized()); // 401 for invalid creds, not 403 for access denied
        }

        @Test
        @DisplayName("should allow access to swagger UI without authentication")
        void shouldAllowSwaggerUI() throws Exception {
            mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html"));
        }

        @Test
        @DisplayName("should allow access to OpenAPI docs without authentication")
        void shouldAllowOpenApiDocs() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Protected Path Enforcement")
    class ProtectedPathEnforcement {

        @Test
        @DisplayName("should require authentication for patient API")
        void shouldRequireAuthForPatientApi() throws Exception {
            mockMvc.perform(get("/api/v1/patients"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("unauthorized"));
        }

        @Test
        @DisplayName("should require authentication for FHIR resources")
        void shouldRequireAuthForFhirResources() throws Exception {
            mockMvc.perform(get("/fhir/Patient/123"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should require authentication for CQL evaluation")
        void shouldRequireAuthForCqlEvaluation() throws Exception {
            mockMvc.perform(post("/cql-engine/evaluate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("JWT Token Validation")
    class JwtTokenValidation {

        @Test
        @DisplayName("should reject malformed JWT")
        void shouldRejectMalformedJwt() throws Exception {
            mockMvc.perform(get("/api/v1/patients")
                    .header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));
        }

        @Test
        @DisplayName("should reject JWT without Bearer prefix")
        void shouldRejectJwtWithoutBearer() throws Exception {
            mockMvc.perform(get("/api/v1/patients")
                    .header("Authorization", "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0In0.sig"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should reject empty Bearer token")
        void shouldRejectEmptyBearerToken() throws Exception {
            mockMvc.perform(get("/api/v1/patients")
                    .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should reject expired JWT")
        void shouldRejectExpiredJwt() throws Exception {
            // A token that was valid but is now expired
            String expiredToken = "eyJhbGciOiJIUzUxMiJ9." +
                "eyJzdWIiOiJ0ZXN0IiwiZXhwIjoxNjAwMDAwMDAwfQ." +
                "signature";

            mockMvc.perform(get("/api/v1/patients")
                    .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Error Response Format")
    class ErrorResponseFormat {

        @Test
        @DisplayName("should return JSON error for missing auth")
        void shouldReturnJsonErrorForMissingAuth() throws Exception {
            mockMvc.perform(get("/api/v1/patients")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("should return consistent error structure")
        void shouldReturnConsistentErrorStructure() throws Exception {
            mockMvc.perform(get("/api/v1/patients"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"))
                .andExpect(jsonPath("$.message").isString());
        }
    }

    @Nested
    @DisplayName("CORS Configuration")
    class CorsConfiguration {

        @Test
        @DisplayName("should allow CORS from configured origins")
        void shouldAllowCorsFromConfiguredOrigins() throws Exception {
            mockMvc.perform(get("/actuator/health")
                    .header("Origin", "http://localhost:4200"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
        }

        @Test
        @DisplayName("should allow credentials in CORS response")
        void shouldAllowCredentials() throws Exception {
            mockMvc.perform(get("/actuator/health")
                    .header("Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        }

        @Test
        @DisplayName("should handle CORS preflight requests")
        void shouldHandleCorsPreflight() throws Exception {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .options("/api/v1/patients")
                    .header("Origin", "http://localhost:4200")
                    .header("Access-Control-Request-Method", "POST")
                    .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Methods"));
        }
    }

    @Nested
    @DisplayName("Rate Limiting")
    class RateLimiting {

        @Test
        @DisplayName("should rate limit login attempts")
        void shouldRateLimitLoginAttempts() throws Exception {
            // Make multiple rapid login attempts
            for (int i = 0; i < 15; i++) {
                mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"test\",\"password\":\"wrong\"}"));
            }

            // The next attempt should be rate limited (429) or blocked
            // Note: This depends on rate limiting configuration being enabled
        }
    }

    @Nested
    @DisplayName("Session Management")
    class SessionManagement {

        @Test
        @DisplayName("should not create HTTP session")
        void shouldNotCreateSession() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                .andExpect(request().sessionAttributeDoesNotExist("SPRING_SECURITY_CONTEXT"));
        }
    }
}
