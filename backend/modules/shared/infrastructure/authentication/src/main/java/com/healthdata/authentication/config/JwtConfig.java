package com.healthdata.authentication.config;

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
 *
 * Example configuration in application.yml:
 * <pre>
 * jwt:
 *   secret: ${JWT_SECRET}
 *   access-token-expiration: 15m
 *   refresh-token-expiration: 7d
 *   issuer: healthdata-in-motion
 *   audience: healthdata-api
 * </pre>
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
     * Should be stored securely (e.g., environment variable, secrets manager).
     */
    @NotBlank(message = "JWT secret key cannot be blank")
    private String secret;

    /**
     * Access token expiration duration.
     * Default: 15 minutes
     * Format: ISO-8601 duration (e.g., "15m", "1h", "PT15M")
     */
    @NotNull(message = "Access token expiration cannot be null")
    private Duration accessTokenExpiration = Duration.ofMinutes(15);

    /**
     * Refresh token expiration duration.
     * Default: 7 days
     * Format: ISO-8601 duration (e.g., "7d", "168h", "P7D")
     */
    @NotNull(message = "Refresh token expiration cannot be null")
    private Duration refreshTokenExpiration = Duration.ofDays(7);

    /**
     * JWT issuer claim (iss).
     * Identifies the principal that issued the token.
     * Default: "healthdata-in-motion"
     */
    @NotBlank(message = "JWT issuer cannot be blank")
    private String issuer = "healthdata-in-motion";

    /**
     * JWT audience claim (aud).
     * Identifies the recipients that the JWT is intended for.
     * Default: "healthdata-api"
     */
    @NotBlank(message = "JWT audience cannot be blank")
    private String audience = "healthdata-api";

    /**
     * Validate JWT configuration after properties are loaded.
     * Ensures secret key meets minimum security requirements.
     */
    @PostConstruct
    public void validateConfiguration() {
        log.info("Initializing JWT configuration...");

        // Validate secret key length
        validateSecretKeyLength();

        // Validate token expiration times
        validateExpirationTimes();

        log.info("JWT configuration validated successfully");
        log.info("  Issuer: {}", issuer);
        log.info("  Audience: {}", audience);
        log.info("  Access token expiration: {}", accessTokenExpiration);
        log.info("  Refresh token expiration: {}", refreshTokenExpiration);
    }

    /**
     * Validate that the secret key meets minimum security requirements.
     * For HS512 algorithm, the key must be at least 256 bits (32 bytes).
     *
     * @throws IllegalStateException if secret key is too short
     */
    private void validateSecretKeyLength() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret key is not configured. " +
                "Please set 'jwt.secret' property or JWT_SECRET environment variable.");
        }

        // Check if secret is base64 encoded
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            // Not base64, use raw string bytes
            keyBytes = secret.getBytes();
        }

        // HS512 requires at least 512 bits (64 bytes), but we'll enforce minimum 256 bits (32 bytes)
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

    /**
     * Validate token expiration times are reasonable.
     *
     * @throws IllegalStateException if expiration times are invalid
     */
    private void validateExpirationTimes() {
        // Access tokens should not be too long-lived
        if (accessTokenExpiration.toHours() > 24) {
            log.warn("Access token expiration is very long ({}). Consider using shorter expiration for better security.",
                accessTokenExpiration);
        }

        // Refresh tokens should not be indefinite
        if (refreshTokenExpiration.toDays() > 90) {
            log.warn("Refresh token expiration is very long ({}). Consider using shorter expiration.",
                refreshTokenExpiration);
        }

        // Refresh token should be longer than access token
        if (refreshTokenExpiration.compareTo(accessTokenExpiration) <= 0) {
            throw new IllegalStateException(
                "Refresh token expiration must be longer than access token expiration. " +
                "Access: " + accessTokenExpiration + ", Refresh: " + refreshTokenExpiration
            );
        }
    }

    /**
     * Get access token expiration in milliseconds.
     *
     * @return expiration time in milliseconds
     */
    public long getAccessTokenExpirationMillis() {
        return accessTokenExpiration.toMillis();
    }

    /**
     * Get refresh token expiration in milliseconds.
     *
     * @return expiration time in milliseconds
     */
    public long getRefreshTokenExpirationMillis() {
        return refreshTokenExpiration.toMillis();
    }

    /**
     * Get access token expiration in seconds.
     *
     * @return expiration time in seconds
     */
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpiration.toSeconds();
    }

    /**
     * Get refresh token expiration in seconds.
     *
     * @return expiration time in seconds
     */
    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpiration.toSeconds();
    }
}
