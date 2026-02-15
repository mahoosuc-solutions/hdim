package com.healthdata.authentication.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.authentication.config.TestSecurityConfig;
import com.healthdata.authentication.domain.Tenant;
import com.healthdata.authentication.domain.TenantStatus;
import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.domain.UserRole;
import com.healthdata.authentication.dto.LoginRequest;
import com.healthdata.authentication.dto.RefreshTokenRequest;
import com.healthdata.authentication.entity.RefreshToken;
import com.healthdata.authentication.repository.RefreshTokenRepository;
import com.healthdata.authentication.repository.TenantRepository;
import com.healthdata.authentication.repository.UserRepository;
import com.healthdata.authentication.service.CookieService;
import jakarta.servlet.http.Cookie;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestSecurityConfig.class)
@DisplayName("JWT Refresh Token Integration Tests")
class TokenRefreshIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String PASSWORD = "TestPassword123!";
    private static final String TENANT_1 = "tenant1";

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        createTenant(TENANT_1, "Tenant 1");
        createUser("demo-user", "demo-user@test.com", Set.of(TENANT_1), Set.of(UserRole.EVALUATOR), true);
    }

    @Test
    @DisplayName("Should refresh token with body refreshToken, rotate refresh token, and revoke old token")
    void shouldRefreshWithBodyAndRotateToken() throws Exception {
        String oldRefreshToken = loginAndGetRefreshToken("demo-user", PASSWORD);

        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
            .refreshToken(oldRefreshToken)
            .build();

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
            .andReturn();

        JsonNode refreshJson = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String newRefreshToken = refreshJson.get("refreshToken").asText();

        assertThat(newRefreshToken).isNotBlank();
        assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);

        RefreshToken oldTokenEntity = refreshTokenRepository.findByToken(oldRefreshToken).orElseThrow();
        assertThat(oldTokenEntity.getRevokedAt()).isNotNull();

        RefreshToken newTokenEntity = refreshTokenRepository.findByToken(newRefreshToken).orElseThrow();
        assertThat(newTokenEntity.getRevokedAt()).isNull();
        assertThat(newTokenEntity.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("Should reject refresh attempts with a revoked refresh token")
    void shouldRejectRevokedRefreshToken() throws Exception {
        String oldRefreshToken = loginAndGetRefreshToken("demo-user", PASSWORD);

        // First refresh rotates and revokes old token.
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RefreshTokenRequest.builder().refreshToken(oldRefreshToken).build())))
            .andExpect(status().isOk());

        // Attempt to reuse revoked token should fail.
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RefreshTokenRequest.builder().refreshToken(oldRefreshToken).build())))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should refresh using HttpOnly refresh token cookie when body is empty")
    void shouldRefreshUsingCookie() throws Exception {
        String refreshToken = loginAndGetRefreshToken("demo-user", PASSWORD);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new Cookie(CookieService.REFRESH_TOKEN_COOKIE, refreshToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 401 when no refresh token is provided")
    void shouldRejectMissingRefreshToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when refresh token is expired in DB even if JWT is still valid")
    void shouldRejectExpiredRefreshTokenInDatabase() throws Exception {
        String refreshToken = loginAndGetRefreshToken("demo-user", PASSWORD);

        RefreshToken tokenEntity = refreshTokenRepository.findByToken(refreshToken).orElseThrow();
        tokenEntity.setExpiresAt(Instant.now().minusSeconds(1));
        refreshTokenRepository.save(tokenEntity);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RefreshTokenRequest.builder().refreshToken(refreshToken).build())))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when user account becomes inactive before refresh")
    void shouldRejectRefreshForInactiveAccount() throws Exception {
        String refreshToken = loginAndGetRefreshToken("demo-user", PASSWORD);

        User user = userRepository.findByUsername("demo-user").orElseThrow();
        user.setActive(false);
        userRepository.save(user);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RefreshTokenRequest.builder().refreshToken(refreshToken).build())))
            .andExpect(status().isUnauthorized());
    }

    private String loginAndGetRefreshToken(String username, String password) throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
            .username(username)
            .password(password)
            .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("refreshToken").asText();
    }

    private User createUser(
        String username,
        String email,
        Set<String> tenantIds,
        Set<UserRole> roles,
        boolean active
    ) {
        User user = User.builder()
            .id(UUID.randomUUID())
            .username(username)
            .email(email)
            .passwordHash(passwordEncoder.encode(PASSWORD))
            .firstName("Test")
            .lastName("User")
            .tenantIds(new java.util.HashSet<>(tenantIds))
            .roles(new java.util.HashSet<>(roles))
            .active(active)
            .emailVerified(true)
            .failedLoginAttempts(0)
            .build();

        return userRepository.save(user);
    }

    private void createTenant(String id, String name) {
        Tenant tenant = Tenant.builder()
            .id(id)
            .name(name)
            .status(TenantStatus.ACTIVE)
            .build();

        tenantRepository.save(tenant);
    }
}

