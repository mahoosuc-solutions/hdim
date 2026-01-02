package com.healthdata.authentication.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Service for generating and validating MFA tokens.
 *
 * MFA tokens are short-lived JWTs that prove password authentication succeeded.
 * They are used to complete the second step of MFA-protected login.
 *
 * Token Properties:
 * - Valid for 5 minutes
 * - Contains user ID and username
 * - Signed with the same secret as access tokens
 * - Cannot be used to access protected resources (different token type)
 */
@Slf4j
@Service
public class MfaTokenService {

    private static final String TOKEN_TYPE = "mfa";
    private static final int MFA_TOKEN_EXPIRATION_MINUTES = 5;

    private final SecretKey signingKey;

    public MfaTokenService(@Value("${jwt.secret}") String jwtSecret) {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate a short-lived MFA token after password authentication.
     *
     * @param userId User's UUID
     * @param username User's username
     * @return JWT token for MFA verification step
     */
    public String generateMfaToken(UUID userId, String username) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(MFA_TOKEN_EXPIRATION_MINUTES * 60L);

        return Jwts.builder()
            .subject(userId.toString())
            .claim("username", username)
            .claim("type", TOKEN_TYPE)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .id(UUID.randomUUID().toString())
            .signWith(signingKey)
            .compact();
    }

    /**
     * Validate MFA token and extract user ID.
     *
     * @param token MFA token to validate
     * @return User ID if token is valid, null otherwise
     */
    public UUID validateMfaToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            // Verify token type
            String type = claims.get("type", String.class);
            if (!TOKEN_TYPE.equals(type)) {
                log.warn("Invalid MFA token type: {}", type);
                return null;
            }

            return UUID.fromString(claims.getSubject());

        } catch (ExpiredJwtException e) {
            log.warn("MFA token expired");
            return null;
        } catch (MalformedJwtException | UnsupportedJwtException | SignatureException e) {
            log.warn("Invalid MFA token: {}", e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            log.warn("MFA token validation failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract username from MFA token.
     *
     * @param token MFA token
     * @return Username if token is valid, null otherwise
     */
    public String extractUsername(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            return claims.get("username", String.class);

        } catch (Exception e) {
            log.warn("Failed to extract username from MFA token: {}", e.getMessage());
            return null;
        }
    }
}
