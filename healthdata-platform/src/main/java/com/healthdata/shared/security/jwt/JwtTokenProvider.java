package com.healthdata.shared.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JWT Token Provider - Generates and validates JWT tokens
 *
 * Uses JJWT library with HS256 (HMAC SHA-256) algorithm for token signing.
 * Tokens include user ID, username, and roles as claims.
 *
 * Spring Boot 3.3.5 compatible - Uses Jakarta EE (jakarta.*)
 */
@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long tokenExpirationMs;
    private final long refreshTokenExpirationMs;

    /**
     * Initialize JWT Token Provider with secret key and expiration times
     */
    public JwtTokenProvider(
            @Value("${spring.security.jwt.secret}") String jwtSecret,
            @Value("${spring.security.jwt.expiration}") long tokenExpirationMs,
            @Value("${spring.security.jwt.refresh-expiration}") long refreshTokenExpirationMs) {

        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        this.tokenExpirationMs = tokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;

        log.info("JWT Token Provider initialized");
        log.debug("Token expiration: {}ms, Refresh token expiration: {}ms",
                tokenExpirationMs, refreshTokenExpirationMs);
    }

    /**
     * Generate JWT token from Authentication object
     *
     * @param authentication Spring Security Authentication containing user details
     * @return JWT token string
     */
    public String generateToken(Authentication authentication) {
        return generateToken(authentication.getName(), extractRoles(authentication));
    }

    /**
     * Generate JWT token with username, roles, and tenant context.
     *
     * @param username User's username
     * @param roles User's roles/authorities
     * @param userId User's unique identifier (optional)
     * @param tenantId Tenant identifier (optional)
     * @return JWT token string
     */
    public String generateToken(String username, List<String> roles, String userId, String tenantId) {
        return createToken(username, roles, tokenExpirationMs, userId, tenantId);
    }

    /**
     * Generate JWT token with username and roles
     *
     * @param username User's username
     * @param roles User's roles/authorities
     * @return JWT token string
     */
    public String generateToken(String username, List<String> roles) {
        return createToken(username, roles, tokenExpirationMs, null, null);
    }

    /**
     * Generate refresh token for obtaining new access tokens
     *
     * @param username User's username
     * @param roles User's roles/authorities
     * @return Refresh token string
     */
    public String generateRefreshToken(String username, List<String> roles) {
        return createToken(username, roles, refreshTokenExpirationMs, null, null);
    }

    /**
     * Generate refresh token with username, roles, and tenant context.
     *
     * @param username User's username
     * @param roles User's roles/authorities
     * @param userId User's unique identifier (optional)
     * @param tenantId Tenant identifier (optional)
     * @return Refresh token string
     */
    public String generateRefreshToken(String username, List<String> roles, String userId, String tenantId) {
        return createToken(username, roles, refreshTokenExpirationMs, userId, tenantId);
    }

    /**
     * Generate both access and refresh tokens
     *
     * @param username User's username
     * @param roles User's roles/authorities
     * @return Map containing "accessToken" and "refreshToken"
     */
    public Map<String, String> generateTokenPair(String username, List<String> roles) {
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", generateToken(username, roles));
        tokens.put("refreshToken", generateRefreshToken(username, roles));
        return tokens;
    }

    /**
     * Generate both access and refresh tokens with tenant context.
     *
     * @param username User's username
     * @param roles User's roles/authorities
     * @param userId User's unique identifier (optional)
     * @param tenantId Tenant identifier (optional)
     * @return Map containing "accessToken" and "refreshToken"
     */
    public Map<String, String> generateTokenPair(
            String username,
            List<String> roles,
            String userId,
            String tenantId) {
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", generateToken(username, roles, userId, tenantId));
        tokens.put("refreshToken", generateRefreshToken(username, roles, userId, tenantId));
        return tokens;
    }

    /**
     * Create JWT token with specified expiration time
     *
     * @param username Username claim
     * @param roles Roles claim
     * @param expirationMs Token expiration time in milliseconds
     * @return Signed JWT token
     */
    private String createToken(
            String username,
            List<String> roles,
            long expirationMs,
            String userId,
            String tenantId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtConstants.CLAIM_ROLES, roles);
        if (userId != null && !userId.isBlank()) {
            claims.put(JwtConstants.CLAIM_USER_ID, userId);
        }
        if (tenantId != null && !tenantId.isBlank()) {
            claims.put(JwtConstants.CLAIM_TENANT_ID, tenantId);
        }

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        log.debug("Generated JWT token for user: {}, expires at: {}", username, expiryDate);
        return token;
    }

    /**
     * Validate JWT token signature and expiration
     *
     * @param token JWT token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);

            log.debug("JWT token validation successful");
            return true;
        } catch (io.jsonwebtoken.security.SecurityException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (io.jsonwebtoken.MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (io.jsonwebtoken.UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Extract username (subject) from JWT token
     *
     * @param token JWT token
     * @return Username
     */
    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * Extract user ID from JWT token (if present)
     *
     * @param token JWT token
     * @return User ID or null if not present
     */
    public String getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Object userId = claims.get(JwtConstants.CLAIM_USER_ID);
        return userId != null ? userId.toString() : null;
    }

    /**
     * Extract roles from JWT token
     *
     * @param token JWT token
     * @return List of roles
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Object roles = claims.get(JwtConstants.CLAIM_ROLES);
        return roles != null ? (List<String>) roles : List.of();
    }

    /**
     * Extract tenant ID from JWT token (if present)
     *
     * @param token JWT token
     * @return Tenant ID or null if not present
     */
    public String getTenantIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Object tenantId = claims.get(JwtConstants.CLAIM_TENANT_ID);
        return tenantId != null ? tenantId.toString() : null;
    }

    /**
     * Extract all claims from JWT token
     *
     * @param token JWT token
     * @return Claims object containing all token claims
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Check if token has expired
     *
     * @param token JWT token
     * @return true if token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.warn("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Get remaining time until token expiration in milliseconds
     *
     * @param token JWT token
     * @return Milliseconds until expiration, negative if already expired
     */
    public long getTimeUntilExpiration(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration().getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            log.warn("Error getting token expiration time: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Extract roles from Spring Security Authentication
     *
     * @param authentication Spring Security Authentication object
     * @return List of role names
     */
    private List<String> extractRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(auth -> {
                    String authority = auth.getAuthority();
                    // Remove ROLE_ prefix if present
                    return authority.startsWith("ROLE_") ? authority.substring(5) : authority;
                })
                .toList();
    }

    /**
     * Get token expiration time
     *
     * @return Token expiration time in milliseconds
     */
    public long getTokenExpirationMs() {
        return tokenExpirationMs;
    }

    /**
     * Get refresh token expiration time
     *
     * @return Refresh token expiration time in milliseconds
     */
    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }
}
