package com.healthdata.investor.service;

import com.healthdata.investor.dto.LoginRequest;
import com.healthdata.investor.dto.LoginResponse;
import com.healthdata.investor.dto.RefreshTokenRequest;
import com.healthdata.investor.entity.InvestorUser;
import com.healthdata.investor.exception.AuthenticationException;
import com.healthdata.investor.exception.AccountLockedException;
import com.healthdata.investor.repository.InvestorUserRepository;
import com.healthdata.investor.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for handling authentication operations.
 * Manages login, token refresh, and account lockout.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final InvestorUserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${security.login.max-attempts}")
    private int maxLoginAttempts;

    @Value("${security.login.lockout-duration}")
    private long lockoutDuration;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        InvestorUser user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        // Check if account is locked
        if (user.isLocked()) {
            throw new AccountLockedException("Account is temporarily locked. Please try again later.");
        }

        // Check if account is active
        if (!user.getActive()) {
            throw new AuthenticationException("Account is disabled. Please contact support.");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new AuthenticationException("Invalid email or password");
        }

        // Reset failed attempts on successful login
        user.resetFailedAttempts();
        user.setLastLogin(Instant.now());
        userRepository.save(user);

        return generateTokenResponse(user);
    }

    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new AuthenticationException("Invalid or expired refresh token");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        InvestorUser user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        if (!user.getActive()) {
            throw new AuthenticationException("Account is disabled");
        }

        return generateTokenResponse(user);
    }

    public LoginResponse.UserDTO getCurrentUser(UUID userId) {
        InvestorUser user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        return LoginResponse.UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();
    }

    private void handleFailedLogin(InvestorUser user) {
        user.incrementFailedAttempts();

        if (user.getFailedLoginAttempts() >= maxLoginAttempts) {
            user.setLockedUntil(Instant.now().plusMillis(lockoutDuration));
            log.warn("Account locked for user: {} due to {} failed login attempts",
                    user.getEmail(), user.getFailedLoginAttempts());
        }

        userRepository.save(user);
    }

    private LoginResponse generateTokenResponse(InvestorUser user) {
        String accessToken = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail(), user.getRole());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000) // Convert to seconds
                .user(LoginResponse.UserDTO.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .role(user.getRole())
                        .build())
                .build();
    }
}
