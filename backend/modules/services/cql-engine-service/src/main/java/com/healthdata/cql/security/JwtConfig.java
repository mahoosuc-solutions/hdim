package com.healthdata.cql.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Base64;

/**
 * Configuration properties for JWT authentication.
 *
 * Properties are loaded from application.yml under the 'jwt' prefix.
 * All properties are validated at startup to ensure secure configuration.
 *
 * Security Requirements:
 * - Secret key must be at least 256 bits (32 bytes) for HS512 algorithm
 * - Access tokens should be short-lived (default: 15 minutes)
 * - Refresh tokens should have reasonable expiration (default: 7 days)
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Validated
public class JwtConfig {

    /**
     * Secret key for signing JWT tokens.
     * MUST be at least 256 bits (32 bytes) for HS512 algorithm.
     */
    @NotBlank(message = "JWT secret key cannot be blank")
    private String secret;

    /**
     * Access token expiration duration.
     * Default: 15 minutes
     */
    @NotNull(message = "Access token expiration cannot be null")
    private Duration accessTokenExpiration = Duration.ofMinutes(15);

    /**
     * Refresh token expiration duration.
     * Default: 7 days
     */
    @NotNull(message = "Refresh token expiration cannot be null")
    private Duration refreshTokenExpiration = Duration.ofDays(7);

    /**
     * JWT issuer claim (iss).
     * Default: "healthdata-in-motion"
     */
    @NotBlank(message = "JWT issuer cannot be blank")
    private String issuer = "healthdata-in-motion";

    /**
     * JWT audience claim (aud).
     * Default: "healthdata-api"
     */
    @NotBlank(message = "JWT audience cannot be blank")
    private String audience = "healthdata-api";

    @PostConstruct
    public void validateConfiguration() {
        log.info("Initializing JWT configuration...");
        validateSecretKeyLength();
        validateExpirationTimes();
        log.info("JWT configuration validated successfully");
        log.info("  Issuer: {}", issuer);
        log.info("  Audience: {}", audience);
        log.info("  Access token expiration: {}", accessTokenExpiration);
        log.info("  Refresh token expiration: {}", refreshTokenExpiration);
    }

    private void validateSecretKeyLength() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret key is not configured. " +
                "Please set 'jwt.secret' property or JWT_SECRET environment variable.");
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            keyBytes = secret.getBytes();
        }

        int minKeyLength = 32; // 256 bits
        if (keyBytes.length < minKeyLength) {
            throw new IllegalStateException(String.format(
                "JWT secret key is too short. Key length: %d bytes, minimum required: %d bytes (256 bits). " +
                "Please use a stronger secret key.",
                keyBytes.length, minKeyLength
            ));
        }

        log.debug("JWT secret key length validated: {} bytes", keyBytes.length);
    }

    private void validateExpirationTimes() {
        if (accessTokenExpiration.toHours() > 24) {
            log.warn("Access token expiration is very long ({}). Consider using shorter expiration for better security.",
                accessTokenExpiration);
        }

        if (refreshTokenExpiration.toDays() > 90) {
            log.warn("Refresh token expiration is very long ({}). Consider using shorter expiration.",
                refreshTokenExpiration);
        }

        if (refreshTokenExpiration.compareTo(accessTokenExpiration) <= 0) {
            throw new IllegalStateException(
                "Refresh token expiration must be longer than access token expiration. " +
                "Access: " + accessTokenExpiration + ", Refresh: " + refreshTokenExpiration
            );
        }
    }

    public long getAccessTokenExpirationMillis() {
        return accessTokenExpiration.toMillis();
    }

    public long getRefreshTokenExpirationMillis() {
        return refreshTokenExpiration.toMillis();
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpiration.toSeconds();
    }

    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpiration.toSeconds();
    }
}
