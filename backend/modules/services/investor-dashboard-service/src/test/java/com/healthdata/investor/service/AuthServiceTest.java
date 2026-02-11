package com.healthdata.investor.service;

import com.healthdata.investor.dto.LoginRequest;
import com.healthdata.investor.dto.LoginResponse;
import com.healthdata.investor.dto.RefreshTokenRequest;
import com.healthdata.investor.entity.InvestorUser;
import com.healthdata.investor.exception.AccountLockedException;
import com.healthdata.investor.exception.AuthenticationException;
import com.healthdata.investor.repository.InvestorUserRepository;
import com.healthdata.investor.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 * Tests authentication operations including login, token refresh, and account lockout.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Service Tests")
@Tag("unit")
class AuthServiceTest {

    @Mock
    private InvestorUserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Captor
    private ArgumentCaptor<InvestorUser> userCaptor;

    private AuthService authService;

    private static final UUID TEST_USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String TEST_EMAIL = "investor@test.com";
    private static final String TEST_PASSWORD = "SecurePass123!";
    private static final String TEST_ENCODED_PASSWORD = "$2a$10$encoded";
    private static final String TEST_ACCESS_TOKEN = "access.token.here";
    private static final String TEST_REFRESH_TOKEN = "refresh.token.here";
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION = 900000L; // 15 minutes
    private static final long JWT_EXPIRATION = 3600000L; // 1 hour

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, jwtTokenProvider, passwordEncoder);
        ReflectionTestUtils.setField(authService, "maxLoginAttempts", MAX_LOGIN_ATTEMPTS);
        ReflectionTestUtils.setField(authService, "lockoutDuration", LOCKOUT_DURATION);
        ReflectionTestUtils.setField(authService, "jwtExpiration", JWT_EXPIRATION);
    }

    private InvestorUser createTestUser() {
        return InvestorUser.builder()
                .id(TEST_USER_ID)
                .email(TEST_EMAIL)
                .passwordHash(TEST_ENCODED_PASSWORD)
                .firstName("John")
                .lastName("Investor")
                .role("USER")
                .active(true)
                .failedLoginAttempts(0)
                .lockedUntil(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfully() {
            // Given
            InvestorUser user = createTestUser();
            LoginRequest request = LoginRequest.builder()
                    .email(TEST_EMAIL)
                    .password(TEST_PASSWORD)
                    .build();

            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(TEST_PASSWORD, TEST_ENCODED_PASSWORD)).thenReturn(true);
            when(jwtTokenProvider.generateToken(any(UUID.class), anyString(), anyString()))
                    .thenReturn(TEST_ACCESS_TOKEN);
            when(jwtTokenProvider.generateRefreshToken(any(UUID.class), anyString(), anyString()))
                    .thenReturn(TEST_REFRESH_TOKEN);
            when(userRepository.save(any(InvestorUser.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            LoginResponse response = authService.login(request);

            // Then
            assertThat(response.getAccessToken()).isEqualTo(TEST_ACCESS_TOKEN);
            assertThat(response.getRefreshToken()).isEqualTo(TEST_REFRESH_TOKEN);
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getExpiresIn()).isEqualTo(JWT_EXPIRATION / 1000);
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getId()).isEqualTo(TEST_USER_ID);
            assertThat(response.getUser().getEmail()).isEqualTo(TEST_EMAIL);

            verify(userRepository).save(userCaptor.capture());
            InvestorUser savedUser = userCaptor.getValue();
            assertThat(savedUser.getFailedLoginAttempts()).isZero();
            assertThat(savedUser.getLastLogin()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowWhenUserNotFound() {
            // Given
            LoginRequest request = LoginRequest.builder()
                    .email("unknown@test.com")
                    .password(TEST_PASSWORD)
                    .build();

            when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("Invalid email or password");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when account is locked")
        void shouldThrowWhenAccountLocked() {
            // Given
            InvestorUser user = createTestUser();
            user.setLockedUntil(Instant.now().plusSeconds(600)); // Locked for 10 more minutes

            LoginRequest request = LoginRequest.builder()
                    .email(TEST_EMAIL)
                    .password(TEST_PASSWORD)
                    .build();

            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));

            // When/Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AccountLockedException.class)
                    .hasMessageContaining("temporarily locked");

            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw exception when account is disabled")
        void shouldThrowWhenAccountDisabled() {
            // Given
            InvestorUser user = createTestUser();
            user.setActive(false);

            LoginRequest request = LoginRequest.builder()
                    .email(TEST_EMAIL)
                    .password(TEST_PASSWORD)
                    .build();

            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));

            // When/Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("Account is disabled");

            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw exception and increment failed attempts on invalid password")
        void shouldIncrementFailedAttemptsOnInvalidPassword() {
            // Given
            InvestorUser user = createTestUser();
            user.setFailedLoginAttempts(0);

            LoginRequest request = LoginRequest.builder()
                    .email(TEST_EMAIL)
                    .password("wrong-password")
                    .build();

            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong-password", TEST_ENCODED_PASSWORD)).thenReturn(false);
            when(userRepository.save(any(InvestorUser.class))).thenAnswer(inv -> inv.getArgument(0));

            // When/Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("Invalid email or password");

            verify(userRepository).save(userCaptor.capture());
            InvestorUser savedUser = userCaptor.getValue();
            assertThat(savedUser.getFailedLoginAttempts()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should lock account after max failed attempts")
        void shouldLockAccountAfterMaxAttempts() {
            // Given
            InvestorUser user = createTestUser();
            user.setFailedLoginAttempts(MAX_LOGIN_ATTEMPTS - 1); // One more attempt will lock

            LoginRequest request = LoginRequest.builder()
                    .email(TEST_EMAIL)
                    .password("wrong-password")
                    .build();

            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong-password", TEST_ENCODED_PASSWORD)).thenReturn(false);
            when(userRepository.save(any(InvestorUser.class))).thenAnswer(inv -> inv.getArgument(0));

            // When/Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AuthenticationException.class);

            verify(userRepository).save(userCaptor.capture());
            InvestorUser savedUser = userCaptor.getValue();
            assertThat(savedUser.getFailedLoginAttempts()).isEqualTo(MAX_LOGIN_ATTEMPTS);
            assertThat(savedUser.getLockedUntil()).isNotNull();
            assertThat(savedUser.getLockedUntil()).isAfter(Instant.now());
        }

        @Test
        @DisplayName("Should reset failed attempts on successful login")
        void shouldResetFailedAttemptsOnSuccess() {
            // Given
            InvestorUser user = createTestUser();
            user.setFailedLoginAttempts(3); // Had some previous failures

            LoginRequest request = LoginRequest.builder()
                    .email(TEST_EMAIL)
                    .password(TEST_PASSWORD)
                    .build();

            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(TEST_PASSWORD, TEST_ENCODED_PASSWORD)).thenReturn(true);
            when(jwtTokenProvider.generateToken(any(UUID.class), anyString(), anyString()))
                    .thenReturn(TEST_ACCESS_TOKEN);
            when(jwtTokenProvider.generateRefreshToken(any(UUID.class), anyString(), anyString()))
                    .thenReturn(TEST_REFRESH_TOKEN);
            when(userRepository.save(any(InvestorUser.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            authService.login(request);

            // Then
            verify(userRepository).save(userCaptor.capture());
            InvestorUser savedUser = userCaptor.getValue();
            assertThat(savedUser.getFailedLoginAttempts()).isZero();
            assertThat(savedUser.getLockedUntil()).isNull();
        }

        @Test
        @DisplayName("Should allow login after lockout period expires")
        void shouldAllowLoginAfterLockoutExpires() {
            // Given
            InvestorUser user = createTestUser();
            user.setLockedUntil(Instant.now().minusSeconds(60)); // Locked until 1 minute ago

            LoginRequest request = LoginRequest.builder()
                    .email(TEST_EMAIL)
                    .password(TEST_PASSWORD)
                    .build();

            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(TEST_PASSWORD, TEST_ENCODED_PASSWORD)).thenReturn(true);
            when(jwtTokenProvider.generateToken(any(UUID.class), anyString(), anyString()))
                    .thenReturn(TEST_ACCESS_TOKEN);
            when(jwtTokenProvider.generateRefreshToken(any(UUID.class), anyString(), anyString()))
                    .thenReturn(TEST_REFRESH_TOKEN);
            when(userRepository.save(any(InvestorUser.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            LoginResponse response = authService.login(request);

            // Then
            assertThat(response.getAccessToken()).isEqualTo(TEST_ACCESS_TOKEN);
        }
    }

    @Nested
    @DisplayName("Refresh Token Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh token successfully with valid refresh token")
        void shouldRefreshTokenSuccessfully() {
            // Given
            InvestorUser user = createTestUser();
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken(TEST_REFRESH_TOKEN)
                    .build();

            when(jwtTokenProvider.validateToken(TEST_REFRESH_TOKEN)).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken(TEST_REFRESH_TOKEN)).thenReturn(TEST_USER_ID);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));
            when(jwtTokenProvider.generateToken(any(UUID.class), anyString(), anyString()))
                    .thenReturn("new.access.token");
            when(jwtTokenProvider.generateRefreshToken(any(UUID.class), anyString(), anyString()))
                    .thenReturn("new.refresh.token");

            // When
            LoginResponse response = authService.refreshToken(request);

            // Then
            assertThat(response.getAccessToken()).isEqualTo("new.access.token");
            assertThat(response.getRefreshToken()).isEqualTo("new.refresh.token");
            assertThat(response.getUser().getId()).isEqualTo(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should throw exception when refresh token is invalid")
        void shouldThrowWhenRefreshTokenInvalid() {
            // Given
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("invalid.token")
                    .build();

            when(jwtTokenProvider.validateToken("invalid.token")).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("Invalid or expired refresh token");
        }

        @Test
        @DisplayName("Should throw exception when user not found during refresh")
        void shouldThrowWhenUserNotFoundOnRefresh() {
            // Given
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken(TEST_REFRESH_TOKEN)
                    .build();

            when(jwtTokenProvider.validateToken(TEST_REFRESH_TOKEN)).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken(TEST_REFRESH_TOKEN)).thenReturn(TEST_USER_ID);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("Should throw exception when user is disabled during refresh")
        void shouldThrowWhenUserDisabledOnRefresh() {
            // Given
            InvestorUser user = createTestUser();
            user.setActive(false);

            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken(TEST_REFRESH_TOKEN)
                    .build();

            when(jwtTokenProvider.validateToken(TEST_REFRESH_TOKEN)).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken(TEST_REFRESH_TOKEN)).thenReturn(TEST_USER_ID);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));

            // When/Then
            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("Account is disabled");
        }
    }

    @Nested
    @DisplayName("Get Current User Tests")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should return current user info")
        void shouldReturnCurrentUser() {
            // Given
            InvestorUser user = createTestUser();
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));

            // When
            LoginResponse.UserDTO userDTO = authService.getCurrentUser(TEST_USER_ID);

            // Then
            assertThat(userDTO.getId()).isEqualTo(TEST_USER_ID);
            assertThat(userDTO.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(userDTO.getFirstName()).isEqualTo("John");
            assertThat(userDTO.getLastName()).isEqualTo("Investor");
            assertThat(userDTO.getRole()).isEqualTo("USER");
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowWhenUserNotFound() {
            // Given
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> authService.getCurrentUser(TEST_USER_ID))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("User not found");
        }
    }
}
