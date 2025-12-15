package com.healthdata.authentication.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.authentication.config.TestSecurityConfig;
import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.domain.UserRole;
import com.healthdata.authentication.dto.LoginRequest;
import com.healthdata.authentication.dto.RegisterRequest;
import com.healthdata.authentication.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for authentication endpoints.
 * Tests all authentication functionality including login, registration, logout, and user info retrieval.
 *
 * SECURITY CRITICAL: These tests validate that authentication API works correctly.
 * Phase 20: Authentication Integration Tests
 *
 * Coverage:
 * - Login endpoint: Success, failures, account locking, validation
 * - Register endpoint: Success, authorization, conflicts, validation
 * - Logout endpoint: Success, authentication requirements
 * - Get current user endpoint: Success, authentication, data validation
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestSecurityConfig.class)
@DisplayName("Authentication Endpoints Integration Tests")
class AuthenticationEndpointsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String PASSWORD = "TestPassword123!";
    private static final String TENANT_1 = "tenant1";
    private static final String TENANT_2 = "tenant2";

    private User testAdmin;
    private User testSuperAdmin;
    private User testEvaluator;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        // Clean up database
        userRepository.deleteAll();

        // Create test admin user
        testAdmin = createUser(
            "admin",
            "admin@test.com",
            Set.of(TENANT_1),
            Set.of(UserRole.ADMIN),
            true
        );

        // Create test super admin user
        testSuperAdmin = createUser(
            "superadmin",
            "superadmin@test.com",
            Set.of(TENANT_1, TENANT_2),
            Set.of(UserRole.SUPER_ADMIN),
            true
        );

        // Create test evaluator user
        testEvaluator = createUser(
            "evaluator",
            "evaluator@test.com",
            Set.of(TENANT_1),
            Set.of(UserRole.EVALUATOR),
            true
        );

        // Create inactive user
        inactiveUser = createUser(
            "inactive",
            "inactive@test.com",
            Set.of(TENANT_1),
            Set.of(UserRole.EVALUATOR),
            false
        );
    }

    // ==================== LOGIN ENDPOINT TESTS ====================

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginSuccessfullyWithValidCredentials() throws Exception {
        LoginRequest request = LoginRequest.builder()
            .username("admin")
            .password(PASSWORD)
            .build();

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("admin"))
            .andExpect(jsonPath("$.email").value("admin@test.com"))
            .andExpect(jsonPath("$.roles").isArray())
            .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
            .andExpect(jsonPath("$.tenantIds").isArray())
            .andExpect(jsonPath("$.tenantIds[0]").value(TENANT_1))
            .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    @DisplayName("Should login successfully with email instead of username")
    void shouldLoginSuccessfullyWithEmail() throws Exception {
        LoginRequest request = LoginRequest.builder()
            .username("admin@test.com")  // Using email
            .password(PASSWORD)
            .build();

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("admin"))
            .andExpect(jsonPath("$.email").value("admin@test.com"));
    }

    @Test
    @DisplayName("Should reject login with invalid username")
    void shouldRejectLoginWithInvalidUsername() throws Exception {
        LoginRequest request = LoginRequest.builder()
            .username("nonexistent")
            .password(PASSWORD)
            .build();

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.error").value("Unauthorized"))
            .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("Should reject login with invalid password")
    void shouldRejectLoginWithInvalidPassword() throws Exception {
        LoginRequest request = LoginRequest.builder()
            .username("admin")
            .password("WrongPassword123!")
            .build();

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.error").value("Unauthorized"))
            .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("Should lock account after 5 failed login attempts")
    void shouldLockAccountAfter5FailedAttempts() throws Exception {
        LoginRequest request = LoginRequest.builder()
            .username("evaluator")
            .password("WrongPassword!")
            .build();

        // Attempt 5 failed logins
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        }

        // 6th attempt should indicate account is locked
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Account is locked. Please try again later."));
    }

    @Test
    @DisplayName("Should show account locked error even with correct password")
    void shouldShowAccountLockedErrorEvenWithCorrectPassword() throws Exception {
        // Lock the account by setting accountLockedUntil
        User user = userRepository.findByUsername("evaluator").orElseThrow();
        user.setAccountLockedUntil(Instant.now().plusSeconds(900));  // Lock for 15 minutes
        userRepository.save(user);

        LoginRequest request = LoginRequest.builder()
            .username("evaluator")
            .password(PASSWORD)
            .build();

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Account is locked. Please try again later."));
    }

    @Test
    @DisplayName("Should reject login for inactive account")
    void shouldRejectLoginForInactiveAccount() throws Exception {
        LoginRequest request = LoginRequest.builder()
            .username("inactive")
            .password(PASSWORD)
            .build();

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Account is disabled. Please contact an administrator."));
    }

    @Test
    @DisplayName("Should validate empty username in login request")
    void shouldValidateEmptyUsernameInLoginRequest() throws Exception {
        LoginRequest request = LoginRequest.builder()
            .username("")
            .password(PASSWORD)
            .build();

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate empty password in login request")
    void shouldValidateEmptyPasswordInLoginRequest() throws Exception {
        LoginRequest request = LoginRequest.builder()
            .username("admin")
            .password("")
            .build();

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate missing username in login request")
    void shouldValidateMissingUsernameInLoginRequest() throws Exception {
        String requestJson = "{\"password\":\"" + PASSWORD + "\"}";

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reset failed login attempts on successful login")
    void shouldResetFailedLoginAttemptsOnSuccessfulLogin() throws Exception {
        // Make a failed login attempt first
        LoginRequest failedRequest = LoginRequest.builder()
            .username("admin")
            .password("WrongPassword!")
            .build();

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(failedRequest)))
            .andExpect(status().isUnauthorized());

        // Now make a successful login
        LoginRequest successRequest = LoginRequest.builder()
            .username("admin")
            .password(PASSWORD)
            .build();

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(successRequest)))
            .andExpect(status().isOk());

        // Verify failed attempts were reset
        User user = userRepository.findByUsername("admin").orElseThrow();
        assert user.getFailedLoginAttempts() == 0;
    }

    // ==================== REGISTER ENDPOINT TESTS ====================

    @Test
    @DisplayName("Should register user successfully as ADMIN")
    void shouldRegisterUserSuccessfullyAsAdmin() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
            .username("newuser")
            .email("newuser@test.com")
            .password("NewPassword123!")
            .firstName("New")
            .lastName("User")
            .tenantIds(Set.of(TENANT_1))
            .roles(Set.of(UserRole.EVALUATOR))
            .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .with(httpBasic("admin", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.username").value("newuser"))
            .andExpect(jsonPath("$.email").value("newuser@test.com"))
            .andExpect(jsonPath("$.firstName").value("New"))
            .andExpect(jsonPath("$.lastName").value("User"))
            .andExpect(jsonPath("$.roles").isArray())
            .andExpect(jsonPath("$.roles[0]").value("EVALUATOR"))
            .andExpect(jsonPath("$.tenantIds").isArray())
            .andExpect(jsonPath("$.tenantIds[0]").value(TENANT_1))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.emailVerified").value(false))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());  // Should never be in response
    }

    @Test
    @DisplayName("Should register user successfully as SUPER_ADMIN")
    void shouldRegisterUserSuccessfullyAsSuperAdmin() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
            .username("newuser2")
            .email("newuser2@test.com")
            .password("NewPassword123!")
            .firstName("New")
            .lastName("User2")
            .tenantIds(Set.of(TENANT_1, TENANT_2))
            .roles(Set.of(UserRole.ADMIN))
            .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .with(httpBasic("superadmin", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("newuser2"))
            .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
            .andExpect(jsonPath("$.tenantIds", hasSize(2)));
    }

    @Test
    @DisplayName("Should reject registration by EVALUATOR (403 Forbidden)")
    void shouldRejectRegistrationByEvaluator() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
            .username("newuser3")
            .email("newuser3@test.com")
            .password("NewPassword123!")
            .firstName("New")
            .lastName("User3")
            .tenantIds(Set.of(TENANT_1))
            .roles(Set.of(UserRole.EVALUATOR))
            .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .with(httpBasic("evaluator", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject registration without authentication (401 Unauthorized)")
    void shouldRejectRegistrationWithoutAuthentication() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
            .username("newuser4")
            .email("newuser4@test.com")
            .password("NewPassword123!")
            .firstName("New")
            .lastName("User4")
            .tenantIds(Set.of(TENANT_1))
            .roles(Set.of(UserRole.EVALUATOR))
            .build();

        // Note: With HTTP Basic auth enabled, unauthenticated requests return 401 (Unauthorized)
        // before authorization rules (403) can be evaluated - this is standard HTTP behavior
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject duplicate username (409 Conflict)")
    void shouldRejectDuplicateUsername() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
            .username("admin")  // Already exists
            .email("newemail@test.com")
            .password("NewPassword123!")
            .firstName("New")
            .lastName("User")
            .tenantIds(Set.of(TENANT_1))
            .roles(Set.of(UserRole.EVALUATOR))
            .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .with(httpBasic("admin", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value("Username already exists: admin"));
    }

    @Test
    @DisplayName("Should reject duplicate email (409 Conflict)")
    void shouldRejectDuplicateEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
            .username("newusername")
            .email("admin@test.com")  // Already exists
            .password("NewPassword123!")
            .firstName("New")
            .lastName("User")
            .tenantIds(Set.of(TENANT_1))
            .roles(Set.of(UserRole.EVALUATOR))
            .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .with(httpBasic("admin", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Email already exists: admin@test.com"));
    }

    @Test
    @DisplayName("Should validate invalid email format")
    void shouldValidateInvalidEmailFormat() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
            .username("newuser5")
            .email("invalid-email")  // Invalid format
            .password("NewPassword123!")
            .firstName("New")
            .lastName("User")
            .tenantIds(Set.of(TENANT_1))
            .roles(Set.of(UserRole.EVALUATOR))
            .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .with(httpBasic("admin", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate short password")
    void shouldValidateShortPassword() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
            .username("newuser6")
            .email("newuser6@test.com")
            .password("Short1!")  // Less than 8 characters
            .firstName("New")
            .lastName("User")
            .tenantIds(Set.of(TENANT_1))
            .roles(Set.of(UserRole.EVALUATOR))
            .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .with(httpBasic("admin", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate short username")
    void shouldValidateShortUsername() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
            .username("ab")  // Less than 3 characters
            .email("newuser7@test.com")
            .password("NewPassword123!")
            .firstName("New")
            .lastName("User")
            .tenantIds(Set.of(TENANT_1))
            .roles(Set.of(UserRole.EVALUATOR))
            .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .with(httpBasic("admin", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate missing required fields")
    void shouldValidateMissingRequiredFields() throws Exception {
        // Missing firstName
        String requestJson = "{\"username\":\"newuser8\",\"email\":\"newuser8@test.com\"," +
            "\"password\":\"NewPassword123!\",\"lastName\":\"User\"," +
            "\"tenantIds\":[\"" + TENANT_1 + "\"],\"roles\":[\"EVALUATOR\"]}";

        mockMvc.perform(post("/api/v1/auth/register")
                .with(httpBasic("admin", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate empty tenant IDs")
    void shouldValidateEmptyTenantIds() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
            .username("newuser9")
            .email("newuser9@test.com")
            .password("NewPassword123!")
            .firstName("New")
            .lastName("User")
            .tenantIds(Set.of())  // Empty
            .roles(Set.of(UserRole.EVALUATOR))
            .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .with(httpBasic("admin", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate empty roles")
    void shouldValidateEmptyRoles() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
            .username("newuser10")
            .email("newuser10@test.com")
            .password("NewPassword123!")
            .firstName("New")
            .lastName("User")
            .tenantIds(Set.of(TENANT_1))
            .roles(Set.of())  // Empty
            .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .with(httpBasic("admin", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    // ==================== LOGOUT ENDPOINT TESTS ====================

    @Test
    @DisplayName("Should logout successfully with authentication")
    void shouldLogoutSuccessfullyWithAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                .with(httpBasic("admin", PASSWORD)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should reject logout without authentication (401 Unauthorized)")
    void shouldRejectLogoutWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should handle multiple logout calls (idempotency)")
    void shouldHandleMultipleLogoutCalls() throws Exception {
        // First logout
        mockMvc.perform(post("/api/v1/auth/logout")
                .with(httpBasic("admin", PASSWORD)))
            .andExpect(status().isOk());

        // Second logout (should still succeed - idempotent)
        mockMvc.perform(post("/api/v1/auth/logout")
                .with(httpBasic("admin", PASSWORD)))
            .andExpect(status().isOk());
    }

    // ==================== GET CURRENT USER TESTS ====================

    @Test
    @DisplayName("Should return current user details with authentication")
    void shouldReturnCurrentUserDetailsWithAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                .with(httpBasic("admin", PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.username").value("admin"))
            .andExpect(jsonPath("$.email").value("admin@test.com"))
            .andExpect(jsonPath("$.firstName").value("Test"))
            .andExpect(jsonPath("$.lastName").value("User"))
            .andExpect(jsonPath("$.roles").isArray())
            .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
            .andExpect(jsonPath("$.tenantIds").isArray())
            .andExpect(jsonPath("$.tenantIds[0]").value(TENANT_1))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.emailVerified").value(true));
    }

    @Test
    @DisplayName("Should reject get current user without authentication (401 Unauthorized)")
    void shouldRejectGetCurrentUserWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return correct user details for different users")
    void shouldReturnCorrectUserDetailsForDifferentUsers() throws Exception {
        // Get evaluator user details
        mockMvc.perform(get("/api/v1/auth/me")
                .with(httpBasic("evaluator", PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("evaluator"))
            .andExpect(jsonPath("$.email").value("evaluator@test.com"))
            .andExpect(jsonPath("$.roles[0]").value("EVALUATOR"));

        // Get super admin user details
        mockMvc.perform(get("/api/v1/auth/me")
                .with(httpBasic("superadmin", PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("superadmin"))
            .andExpect(jsonPath("$.roles[0]").value("SUPER_ADMIN"))
            .andExpect(jsonPath("$.tenantIds", hasSize(2)));
    }

    @Test
    @DisplayName("Should never include password hash in response")
    void shouldNotIncludePasswordHashInResponse() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                .with(httpBasic("admin", PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("Should reject get current user for inactive account")
    void shouldRejectGetCurrentUserForInactiveAccount() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                .with(httpBasic("inactive", PASSWORD)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return user with multiple tenants")
    void shouldReturnUserWithMultipleTenants() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                .with(httpBasic("superadmin", PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantIds").isArray())
            .andExpect(jsonPath("$.tenantIds", hasSize(2)))
            .andExpect(jsonPath("$.tenantIds", containsInAnyOrder(TENANT_1, TENANT_2)));
    }

    // ==================== SECURITY AND EDGE CASE TESTS ====================

    @Test
    @DisplayName("Should handle malformed JSON in login request")
    void shouldHandleMalformedJsonInLoginRequest() throws Exception {
        String malformedJson = "{username: 'admin', password: 'test'}";  // Missing quotes

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
            .andExpect(status().is5xxServerError());  // JSON parse error returns 500
    }

    @Test
    @DisplayName("Should handle malformed JSON in register request")
    void shouldHandleMalformedJsonInRegisterRequest() throws Exception {
        String malformedJson = "{username: 'newuser'}";

        mockMvc.perform(post("/api/v1/auth/register")
                .with(httpBasic("admin", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
            .andExpect(status().is5xxServerError());  // JSON parse error returns 500
    }

    @Test
    @DisplayName("Should handle very long username")
    void shouldHandleVeryLongUsername() throws Exception {
        String longUsername = "a".repeat(100);  // Exceeds 50 character limit
        RegisterRequest request = RegisterRequest.builder()
            .username(longUsername)
            .email("long@test.com")
            .password("Password123!")
            .firstName("Test")
            .lastName("User")
            .tenantIds(Set.of(TENANT_1))
            .roles(Set.of(UserRole.EVALUATOR))
            .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .with(httpBasic("admin", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle special characters in username")
    void shouldHandleSpecialCharactersInUsername() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
            .username("user@123#$")
            .email("special@test.com")
            .password("Password123!")
            .firstName("Test")
            .lastName("User")
            .tenantIds(Set.of(TENANT_1))
            .roles(Set.of(UserRole.EVALUATOR))
            .build();

        // Should allow registration with special characters
        mockMvc.perform(post("/api/v1/auth/register")
                .with(httpBasic("admin", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("user@123#$"));
    }

    @Test
    @DisplayName("Should handle case sensitivity in login")
    void shouldHandleCaseSensitivityInLogin() throws Exception {
        LoginRequest request = LoginRequest.builder()
            .username("ADMIN")  // Different case
            .password(PASSWORD)
            .build();

        // Should fail because username is case-sensitive
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should handle SQL injection attempts in username")
    void shouldHandleSqlInjectionAttempts() throws Exception {
        LoginRequest request = LoginRequest.builder()
            .username("admin' OR '1'='1")
            .password(PASSWORD)
            .build();

        // Should safely reject SQL injection attempts
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should verify password is hashed in database")
    void shouldVerifyPasswordIsHashedInDatabase() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
            .username("hashtest")
            .email("hashtest@test.com")
            .password("PlaintextPassword123!")
            .firstName("Hash")
            .lastName("Test")
            .tenantIds(Set.of(TENANT_1))
            .roles(Set.of(UserRole.EVALUATOR))
            .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .with(httpBasic("admin", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        // Verify password is hashed and not stored in plaintext
        User user = userRepository.findByUsername("hashtest").orElseThrow();
        assert !user.getPasswordHash().equals("PlaintextPassword123!");
        assert user.getPasswordHash().startsWith("$2a$");  // BCrypt hash format
    }

    // ==================== HELPER METHODS ====================

    /**
     * Create a test user with specified attributes.
     */
    private User createUser(String username, String email, Set<String> tenantIds,
                           Set<UserRole> roles, boolean active) {
        User user = User.builder()
            .username(username)
            .email(email)
            .passwordHash(passwordEncoder.encode(PASSWORD))
            .firstName("Test")
            .lastName("User")
            .tenantIds(new java.util.HashSet<>(tenantIds))  // Create mutable copy
            .roles(new java.util.HashSet<>(roles))          // Create mutable copy
            .active(active)
            .emailVerified(true)
            .failedLoginAttempts(0)
            .build();

        return userRepository.save(user);
    }
}
