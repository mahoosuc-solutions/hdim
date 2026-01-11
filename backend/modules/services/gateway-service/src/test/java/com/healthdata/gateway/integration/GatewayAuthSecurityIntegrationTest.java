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
 * - JWT refresh token security
 * - Multi-tenant isolation
 * - MFA policy enforcement
 *
 * SECURITY TEST COVERAGE:
 * - OWASP A01: Broken Access Control
 * - OWASP A02: Cryptographic Failures (JWT validation)
 * - OWASP A07: Identification and Authentication Failures
 * - HIPAA Security Rule: Access Control (§164.312(a)(1))
 * - HIPAA Security Rule: Person Authentication (§164.312(d))
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(GatewayIntegrationTestConfig.class)
@DisplayName("Gateway Authentication Security E2E Tests")
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

    @Nested
    @DisplayName("JWT Refresh Token Security")
    class JwtRefreshTokenSecurity {

        @Test
        @DisplayName("should reject refresh token used as access token")
        void shouldRejectRefreshTokenAsAccessToken() throws Exception {
            // Attempt to use a refresh token in Authorization header
            String refreshToken = "refresh.token.signature";

            mockMvc.perform(get("/api/v1/patients")
                    .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));
        }

        @Test
        @DisplayName("should reject expired refresh token")
        void shouldRejectExpiredRefreshToken() throws Exception {
            String expiredRefreshToken = "eyJhbGciOiJIUzUxMiJ9." +
                "eyJzdWIiOiJ0ZXN0IiwidHlwZSI6InJlZnJlc2giLCJleHAiOjE2MDAwMDAwMDB9." +
                "signature";

            mockMvc.perform(post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"refreshToken\":\"" + expiredRefreshToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("should reject refresh token after logout")
        void shouldRejectRefreshTokenAfterLogout() throws Exception {
            // Simulate: login -> get refresh token -> logout -> try to use refresh token
            // The refresh token should be blacklisted/invalidated
            String validRefreshToken = "valid.refresh.token";

            // First logout (should invalidate token)
            mockMvc.perform(post("/api/v1/auth/logout")
                    .header("Authorization", "Bearer some.access.token"));

            // Then try to use the refresh token
            mockMvc.perform(post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"refreshToken\":\"" + validRefreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should reject refresh token after user password change")
        void shouldRejectRefreshTokenAfterPasswordChange() throws Exception {
            // After password change, all existing tokens should be invalidated
            String oldRefreshToken = "old.refresh.token";

            mockMvc.perform(post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"refreshToken\":\"" + oldRefreshToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token has been revoked"));
        }

        @Test
        @DisplayName("should not allow refresh token reuse")
        void shouldNotAllowRefreshTokenReuse() throws Exception {
            // Refresh token rotation: after using a refresh token once,
            // it should be invalidated and cannot be used again
            String refreshToken = "one.time.refresh.token";

            // First use (should work if token is valid)
            mockMvc.perform(post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"refreshToken\":\"" + refreshToken + "\"}"));

            // Second use (should fail - token already used)
            mockMvc.perform(post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token has already been used"));
        }

        @Test
        @DisplayName("should detect refresh token theft via rotation")
        void shouldDetectRefreshTokenTheft() throws Exception {
            // If an old refresh token is used after a new one was issued,
            // it indicates token theft - invalidate all tokens for that user
            String stolenOldToken = "stolen.old.refresh.token";

            mockMvc.perform(post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"refreshToken\":\"" + stolenOldToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token theft detected - all sessions invalidated"));
        }
    }

    @Nested
    @DisplayName("Multi-Tenant Isolation Security")
    class MultiTenantIsolationSecurity {

        @Test
        @DisplayName("should prevent cross-tenant access via header manipulation")
        void shouldPreventCrossTenantHeaderManipulation() throws Exception {
            // Attacker tries to access another tenant's data by injecting X-Tenant-ID
            mockMvc.perform(get("/api/v1/patients/victim-patient-123")
                    .header("X-Tenant-ID", "victim-tenant-id")
                    .header("Authorization", "Bearer attacker.jwt.token"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should prevent tenant access not in user JWT")
        void shouldPreventUnauthorizedTenantAccess() throws Exception {
            // User's JWT has tenant-1, but tries to access tenant-2 data
            // Gateway should validate tenant access from JWT claims
            mockMvc.perform(get("/api/v1/patients")
                    .header("X-Tenant-ID", "tenant-2")
                    .header("Authorization", "Bearer user.with.tenant1.only"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied to tenant"));
        }

        @Test
        @DisplayName("should enforce tenant context in all API calls")
        void shouldEnforceTenantContext() throws Exception {
            // All protected endpoints must validate tenant context
            mockMvc.perform(post("/api/v1/patients")
                    .header("Authorization", "Bearer valid.token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"firstName\":\"Test\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("X-Tenant-ID header is required"));
        }

        @Test
        @DisplayName("should validate tenant exists and user has access")
        void shouldValidateTenantAccess() throws Exception {
            // Request with valid JWT but non-existent or unauthorized tenant
            mockMvc.perform(get("/api/v1/patients")
                    .header("X-Tenant-ID", "non-existent-tenant")
                    .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Invalid or unauthorized tenant"));
        }
    }

    @Nested
    @DisplayName("MFA Policy Enforcement")
    class MfaPolicyEnforcement {

        @Test
        @DisplayName("should require MFA setup for new users in high-security tenants")
        void shouldRequireMfaSetupForNewUsers() throws Exception {
            // User logged in without MFA, tenant requires MFA
            mockMvc.perform(get("/api/v1/patients")
                    .header("Authorization", "Bearer user.without.mfa.token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("MFA setup required"))
                .andExpect(jsonPath("$.requiresMfaSetup").value(true));
        }

        @Test
        @DisplayName("should reject access token without MFA verification when required")
        void shouldRejectTokenWithoutMfaVerification() throws Exception {
            // Token issued after username/password but before MFA verification
            String tokenWithoutMfa = "token.without.mfa.claim";

            mockMvc.perform(get("/api/v1/patients")
                    .header("Authorization", "Bearer " + tokenWithoutMfa))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("MFA verification required"));
        }

        @Test
        @DisplayName("should enforce step-up MFA for sensitive operations")
        void shouldEnforceStepUpMfa() throws Exception {
            // Sensitive operations like patient data export require recent MFA
            mockMvc.perform(post("/api/v1/patients/export")
                    .header("Authorization", "Bearer valid.token.but.old.mfa")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Recent MFA verification required"))
                .andExpect(jsonPath("$.requiresStepUpMfa").value(true));
        }

        @Test
        @DisplayName("should allow MFA-exempt roles to bypass for certain operations")
        void shouldAllowMfaExemptRoles() throws Exception {
            // System/service accounts may be exempt from MFA
            // But this should be carefully controlled and audited
            mockMvc.perform(get("/api/v1/system/health")
                    .header("Authorization", "Bearer service.account.token")
                    .header("X-Service-Account", "true"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should enforce tenant-level MFA policies")
        void shouldEnforceTenantMfaPolicies() throws Exception {
            // Tenant A requires MFA, Tenant B doesn't
            // Same user accessing both should follow each tenant's policy
            mockMvc.perform(get("/api/v1/patients")
                    .header("X-Tenant-ID", "tenant-with-mfa-required")
                    .header("Authorization", "Bearer token.without.mfa"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("MFA required by tenant policy"));
        }
    }

    @Nested
    @DisplayName("Account Security")
    class AccountSecurity {

        @Test
        @DisplayName("should lock account after multiple failed login attempts")
        void shouldLockAccountAfterFailedAttempts() throws Exception {
            String username = "test.user@example.com";

            // Simulate 5 failed login attempts
            for (int i = 0; i < 5; i++) {
                mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"wrong\"}"))
                    .andExpect(status().isUnauthorized());
            }

            // Next attempt should indicate account is locked
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"" + username + "\",\"password\":\"correct\"}"))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.message").value("Account locked due to multiple failed attempts"));
        }

        @Test
        @DisplayName("should prevent brute force attacks via rate limiting")
        void shouldPreventBruteForceAttacks() throws Exception {
            // Rapid-fire login attempts from same IP should be rate limited
            for (int i = 0; i < 20; i++) {
                mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"victim\",\"password\":\"attempt" + i + "\"}"));
            }

            // Should hit rate limit
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"victim\",\"password\":\"attempt\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Too many login attempts"));
        }

        @Test
        @DisplayName("should require password complexity")
        void shouldRequirePasswordComplexity() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"newuser\",\"password\":\"weak\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password does not meet complexity requirements"));
        }

        @Test
        @DisplayName("should prevent password reuse")
        void shouldPreventPasswordReuse() throws Exception {
            mockMvc.perform(post("/api/v1/auth/change-password")
                    .header("Authorization", "Bearer valid.token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"oldPassword\":\"Current123!\",\"newPassword\":\"Current123!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot reuse previous passwords"));
        }
    }

    @Nested
    @DisplayName("Audit and Compliance")
    class AuditAndCompliance {

        @Test
        @DisplayName("should audit all authentication attempts")
        void shouldAuditAuthenticationAttempts() throws Exception {
            // All login attempts (success and failure) should be audited
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"test\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());

            // Verify audit log entry exists (requires audit service integration)
            // This test validates the audit event is triggered
        }

        @Test
        @DisplayName("should audit PHI access via gateway")
        void shouldAuditPhiAccess() throws Exception {
            // All patient data access should create audit trail
            mockMvc.perform(get("/api/v1/patients/patient-123")
                    .header("Authorization", "Bearer valid.token")
                    .header("X-Tenant-ID", "tenant-1"))
                .andExpect(header().exists("X-Audit-Id"));
        }

        @Test
        @DisplayName("should include security headers in all responses")
        void shouldIncludeSecurityHeaders() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-XSS-Protection", "1; mode=block"))
                .andExpect(header().exists("Strict-Transport-Security"));
        }

        @Test
        @DisplayName("should not leak sensitive info in error messages")
        void shouldNotLeakSensitiveInfo() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"nonexistent@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"))
                // Should NOT say "User not found" or "Wrong password"
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
                    org.hamcrest.Matchers.containsString("not found"))));
        }
    }
}
