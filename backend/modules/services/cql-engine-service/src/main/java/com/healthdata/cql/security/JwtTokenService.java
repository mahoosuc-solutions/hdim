package com.healthdata.cql.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * Service for validating JWT tokens.
 *
 * This is a simplified version for microservices that only need to validate tokens,
 * not generate them. Token generation is handled by the authentication service/gateway.
 *
 * Security Features:
 * - HS512 algorithm for validation
 * - Comprehensive claims extraction
 * - Proper expiration handling
 * - Exception handling for all JWT operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtConfig jwtConfig;

    /**
     * Validate a JWT token.
     * Checks signature, expiration, issuer, and audience.
     *
     * @param token JWT token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .requireIssuer(jwtConfig.getIssuer())
                .requireAudience(jwtConfig.getAudience())
                .build()
                .parseSignedClaims(token);

            log.debug("Token validation successful");
            return true;

        } catch (ExpiredJwtException e) {
            log.debug("Token expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("JWT token compact of handler are invalid: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error validating JWT token", e);
            return false;
        }
    }

    /**
     * Extract username from JWT token.
     *
     * @param token JWT token
     * @return username (subject claim)
     * @throws JwtException if token is invalid
     */
    public String extractUsername(String token) {
        Claims claims = extractAllClaims(token);
        return claims.getSubject();
    }

    /**
     * Extract user ID from JWT token.
     *
     * @param token JWT token
     * @return user UUID
     * @throws JwtException if token is invalid
     */
    public UUID extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        String userId = claims.get("userId", String.class);
        return UUID.fromString(userId);
    }

    /**
     * Extract tenant IDs from JWT token.
     *
     * @param token JWT token
     * @return set of tenant IDs
     * @throws JwtException if token is invalid
     */
    public Set<String> extractTenantIds(String token) {
        Claims claims = extractAllClaims(token);
        String tenantIds = claims.get("tenantIds", String.class);
        return deserializeTenantIds(tenantIds);
    }

    /**
     * Extract roles from JWT token.
     *
     * @param token JWT token
     * @return set of role names
     * @throws JwtException if token is invalid
     */
    public Set<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        String roles = claims.get("roles", String.class);
        return deserializeRoles(roles);
    }

    /**
     * Check if token is expired.
     *
     * @param token JWT token
     * @return true if token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDate(token);
            return expiration.before(new Date());
        } catch (JwtException e) {
            log.debug("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Get token expiration date.
     *
     * @param token JWT token
     * @return expiration date
     * @throws JwtException if token is invalid
     */
    public Date getExpirationDate(String token) {
        Claims claims = extractAllClaims(token);
        return claims.getExpiration();
    }

    /**
     * Extract JWT ID (jti) from token.
     *
     * @param token JWT token
     * @return JWT ID
     * @throws JwtException if token is invalid
     */
    public String extractJwtId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.getId();
    }

    /**
     * Extract issued at time from token.
     *
     * @param token JWT token
     * @return issued at date
     * @throws JwtException if token is invalid
     */
    public Date getIssuedAt(String token) {
        Claims claims = extractAllClaims(token);
        return claims.getIssuedAt();
    }

    /**
     * Extract all claims from JWT token.
     *
     * @param token JWT token
     * @return claims
     * @throws JwtException if token is invalid
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * Get the signing key for JWT operations.
     *
     * @return signing key
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Deserialize comma-separated string to tenant IDs set.
     *
     * @param tenantIds comma-separated string
     * @return set of tenant IDs
     */
    private Set<String> deserializeTenantIds(String tenantIds) {
        if (tenantIds == null || tenantIds.isBlank()) {
            return Set.of();
        }
        return Set.of(tenantIds.split(","));
    }

    /**
     * Deserialize comma-separated string to role names set.
     *
     * @param roles comma-separated string
     * @return set of role names
     */
    private Set<String> deserializeRoles(String roles) {
        if (roles == null || roles.isBlank()) {
            return Set.of();
        }
        return Set.of(roles.split(","));
    }
}
