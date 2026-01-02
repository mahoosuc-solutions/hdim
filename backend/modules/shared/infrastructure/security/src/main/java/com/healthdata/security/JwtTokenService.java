package com.healthdata.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Issues and validates JSON Web Tokens (JWT) using the shared security configuration.
 */
public class JwtTokenService {

    private final SecurityProperties properties;
    private final Clock clock;
    private final SecretKey signingKey;

    public JwtTokenService(SecurityProperties properties) {
        this(properties, Clock.systemUTC());
    }

    JwtTokenService(SecurityProperties properties, Clock clock) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        String secret = Objects.requireNonNull(properties.getJwt().getSecret(), "jwt.secret must be configured");
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Issues a signed token for the given subject with an optional override for the expiration duration.
     */
    public String issueToken(String subject, Duration overrideExpiration) {
        Objects.requireNonNull(subject, "subject must not be null");
        Duration expiration = overrideExpiration != null ? overrideExpiration : properties.getJwt().getExpiration();
        if (expiration == null || expiration.isZero() || expiration.isNegative()) {
            throw new IllegalArgumentException("Token expiration must be a positive duration");
        }

        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(expiration);

        return Jwts.builder()
                .subject(subject)
                .issuer(properties.getJwt().getIssuer())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parses a token using the configured clock.
     */
    public JwtClaims parse(String token) {
        return parse(token, Duration.ZERO);
    }

    /**
     * Parses a token applying the supplied clock offset.
     * A negative offset simulates token validation in the future (useful for testing expiry scenarios).
     */
    public JwtClaims parse(String token, Duration clockOffset) {
        Objects.requireNonNull(token, "token must not be null");
        Duration safeOffset = clockOffset == null ? Duration.ZERO : clockOffset;
        Instant comparisonInstant = clock.instant().minus(safeOffset);

        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);

            Claims claims = jws.getPayload();
            Instant expiresAt = claims.getExpiration().toInstant();
            if (!expiresAt.isAfter(comparisonInstant)) {
                throw new JwtValidationException("Token is expired");
            }

            return new JwtClaims(
                    claims.getSubject(),
                    claims.getIssuedAt().toInstant(),
                    expiresAt
            );
        } catch (JwtValidationException ex) {
            throw ex;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new JwtValidationException("Invalid JWT token", ex);
        }
    }

    /**
     * Simplified claim view for downstream services.
     */
    public record JwtClaims(String subject, Instant issuedAt, Instant expiresAt) {
    }
}
