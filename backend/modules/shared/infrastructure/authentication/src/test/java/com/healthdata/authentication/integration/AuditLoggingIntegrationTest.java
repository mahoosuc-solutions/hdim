package com.healthdata.authentication.integration;

import com.healthdata.authentication.config.TestSecurityConfig;
import com.healthdata.authentication.dto.LoginRequest;
import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.domain.UserRole;
import com.healthdata.authentication.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
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

import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for authentication audit trail verification.
 *
 * NOTE: Audit logging is currently disabled in the authentication module (*.disabled files).
 * These tests validate that authentication events work correctly and can be audited when
 * audit logging is re-enabled in the future.
 *
 * Tests validate:
 * - Successful logins with proper headers
 * - Failed login attempts
 * - IP address and User-Agent header handling
 * - Logout flow
 * - Error resilience
 *
 * CRITICAL: This test class uses EntityManager flush/clear pattern to ensure
 * test users are visible to Spring Security authentication layer.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestSecurityConfig.class)
public class AuditLoggingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User testAdmin;
    private User testUser;

    private static final String PASSWORD = "TestPassword123!";
    private static final String TENANT_1 = "TENANT001";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // Create test admin user
        testAdmin = User.builder()
                .username("auditTestAdmin")
                .email("audit.admin@test.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .firstName("Audit")
                .lastName("Admin")
                .tenantIds(Set.of(TENANT_1))
                .roles(Set.of(UserRole.ADMIN))
                .active(true)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .build();
        testAdmin = userRepository.save(testAdmin);

        // Create regular test user
        testUser = User.builder()
                .username("auditTestUser")
                .email("audit.user@test.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .firstName("Audit")
                .lastName("User")
                .tenantIds(Set.of(TENANT_1))
                .roles(Set.of(UserRole.VIEWER))
                .active(true)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .build();
        testUser = userRepository.save(testUser);

        // CRITICAL FIX: Make users visible to Spring Security auth layer
        // Without this, MockMvc authentication queries run in separate transaction
        // and cannot see uncommitted entities from test transaction
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Should successfully login with audit headers")
    void testSuccessfulLoginWithAuditHeaders() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("auditTestUser")
                .password(PASSWORD)
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-Forwarded-For", "192.168.1.100")
                        .header("User-Agent", "Mozilla/5.0 Test Browser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());

        // Note: When audit logging is re-enabled, this test should verify
        // that LOGIN_SUCCESS event was logged with IP and User-Agent
    }

    @Test
    @DisplayName("Should fail login with wrong password and audit headers")
    void testFailedLoginWithAuditHeaders() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("auditTestUser")
                .password("WrongPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-Forwarded-For", "192.168.1.200")
                        .header("User-Agent", "Mozilla/5.0 Test Browser"))
                .andExpect(status().isUnauthorized());

        // Note: When audit logging is re-enabled, this test should verify
        // that LOGIN_FAILED event was logged with IP and User-Agent
    }

    @Test
    @DisplayName("Should handle login with X-Forwarded-For IP address")
    void testLoginWithForwardedIpAddress() throws Exception {
        String testIpAddress = "203.0.113.42";

        LoginRequest request = LoginRequest.builder()
                .username("auditTestUser")
                .password(PASSWORD)
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-Forwarded-For", testIpAddress)
                        .header("User-Agent", "Test Agent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());

        // Note: When audit logging is re-enabled, verify IP address was captured
    }

    @Test
    @DisplayName("Should handle login with User-Agent header")
    void testLoginWithUserAgentHeader() throws Exception {
        String testUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

        LoginRequest request = LoginRequest.builder()
                .username("auditTestUser")
                .password(PASSWORD)
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-Forwarded-For", "192.168.1.100")
                        .header("User-Agent", testUserAgent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());

        // Note: When audit logging is re-enabled, verify User-Agent was captured
    }

    @Test
    @DisplayName("Should successfully logout with audit headers")
    void testLogoutWithAuditHeaders() throws Exception {
        // Perform logout with HTTP Basic auth and audit headers
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(httpBasic("auditTestUser", PASSWORD))
                        .header("X-Forwarded-For", "192.168.1.100")
                        .header("User-Agent", "Mozilla/5.0"))
                .andExpect(status().isOk());

        // Note: When audit logging is re-enabled, verify LOGOUT event was logged
    }

    @Test
    @DisplayName("Should handle login without audit headers gracefully")
    void testLoginWithoutAuditHeaders() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("auditTestUser")
                .password(PASSWORD)
                .build();

        // Login should succeed even without X-Forwarded-For and User-Agent headers
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());

        // Note: When audit logging is re-enabled, verify it handles missing headers gracefully
    }
}
