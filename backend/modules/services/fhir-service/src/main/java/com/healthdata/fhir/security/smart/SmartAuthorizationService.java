package com.healthdata.fhir.security.smart;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for SMART on FHIR Authorization.
 *
 * Implements OAuth 2.0 Authorization Code Flow with PKCE support
 * for SMART App Launch Framework.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmartAuthorizationService {

    private final SmartClientRepository clientRepository;

    @Value("${smart.issuer:http://localhost:8085/fhir}")
    private String issuer;

    @Value("${smart.jwt.secret:smart-on-fhir-secret-key-minimum-256-bits-required}")
    private String jwtSecret;

    @Value("${smart.authorization-code.lifetime:600}")
    private int authorizationCodeLifetime; // 10 minutes

    // In-memory storage for authorization codes (use Redis in production)
    private final Map<String, AuthorizationCodeData> authorizationCodes = new ConcurrentHashMap<>();

    // In-memory storage for refresh tokens (use database in production)
    private final Map<String, RefreshTokenData> refreshTokens = new ConcurrentHashMap<>();

    /**
     * Generate authorization code for the given request.
     */
    public String generateAuthorizationCode(
            String clientId,
            String redirectUri,
            Set<String> scopes,
            String state,
            String codeChallenge,
            String codeChallengeMethod,
            SmartLaunchContext launchContext) {

        String code = generateSecureCode();

        AuthorizationCodeData codeData = AuthorizationCodeData.builder()
            .clientId(clientId)
            .redirectUri(redirectUri)
            .scopes(scopes)
            .state(state)
            .codeChallenge(codeChallenge)
            .codeChallengeMethod(codeChallengeMethod)
            .launchContext(launchContext)
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plus(authorizationCodeLifetime, ChronoUnit.SECONDS))
            .build();

        authorizationCodes.put(code, codeData);
        log.debug("Generated authorization code for client: {}", clientId);

        return code;
    }

    /**
     * Exchange authorization code for tokens.
     */
    public TokenResponse exchangeAuthorizationCode(
            String code,
            String clientId,
            String redirectUri,
            String codeVerifier) {

        AuthorizationCodeData codeData = authorizationCodes.remove(code);

        if (codeData == null) {
            throw new SmartAuthorizationException("Invalid authorization code");
        }

        if (Instant.now().isAfter(codeData.getExpiresAt())) {
            throw new SmartAuthorizationException("Authorization code expired");
        }

        if (!codeData.getClientId().equals(clientId)) {
            throw new SmartAuthorizationException("Client ID mismatch");
        }

        if (!codeData.getRedirectUri().equals(redirectUri)) {
            throw new SmartAuthorizationException("Redirect URI mismatch");
        }

        SmartClient client = clientRepository.findByClientIdAndActiveTrue(clientId)
            .orElseThrow(() -> new SmartAuthorizationException("Client not found"));

        boolean pkceRequired = client.isPublicClient() || client.isRequirePkce();
        boolean hasCodeChallenge = codeData.getCodeChallenge() != null && !codeData.getCodeChallenge().isBlank();

        if (pkceRequired && !hasCodeChallenge) {
            throw new SmartAuthorizationException("invalid_grant", "PKCE is required for this client");
        }

        if (hasCodeChallenge) {
            if (!"S256".equals(codeData.getCodeChallengeMethod())) {
                throw new SmartAuthorizationException("invalid_grant", "Only S256 PKCE is supported");
            }
            if (codeVerifier == null || codeVerifier.isBlank()) {
                throw new SmartAuthorizationException("Code verifier required");
            }
            if (!validatePkce(codeVerifier, codeData.getCodeChallenge(), codeData.getCodeChallengeMethod())) {
                throw new SmartAuthorizationException("Invalid code verifier");
            }
        }

        // Generate tokens
        String accessToken = generateAccessToken(client, codeData.getScopes(), codeData.getLaunchContext());
        String refreshToken = null;

        if (codeData.getScopes().contains("offline_access")) {
            refreshToken = generateRefreshToken(clientId, codeData.getScopes(), codeData.getLaunchContext());
        }

        return TokenResponse.builder()
            .accessToken(accessToken)
            .tokenType("Bearer")
            .expiresIn(client.getAccessTokenLifetime())
            .refreshToken(refreshToken)
            .scope(String.join(" ", codeData.getScopes()))
            .patient(codeData.getLaunchContext() != null ? codeData.getLaunchContext().getPatient() : null)
            .encounter(codeData.getLaunchContext() != null ? codeData.getLaunchContext().getEncounter() : null)
            .fhirUser(codeData.getLaunchContext() != null ? codeData.getLaunchContext().getFhirUser() : null)
            .needPatientBanner(codeData.getLaunchContext() != null ? codeData.getLaunchContext().getNeedPatientBanner() : null)
            .smartStyleUrl(codeData.getLaunchContext() != null ? codeData.getLaunchContext().getSmartStyleUrl() : null)
            .build();
    }

    /**
     * Refresh access token using refresh token.
     */
    public TokenResponse refreshAccessToken(String refreshToken, String clientId) {
        RefreshTokenData tokenData = refreshTokens.get(refreshToken);

        if (tokenData == null) {
            throw new SmartAuthorizationException("Invalid refresh token");
        }

        if (Instant.now().isAfter(tokenData.getExpiresAt())) {
            refreshTokens.remove(refreshToken);
            throw new SmartAuthorizationException("Refresh token expired");
        }

        if (!tokenData.getClientId().equals(clientId)) {
            throw new SmartAuthorizationException("Client ID mismatch");
        }

        SmartClient client = clientRepository.findByClientIdAndActiveTrue(clientId)
            .orElseThrow(() -> new SmartAuthorizationException("Client not found"));

        String accessToken = generateAccessToken(client, tokenData.getScopes(), tokenData.getLaunchContext());

        return TokenResponse.builder()
            .accessToken(accessToken)
            .tokenType("Bearer")
            .expiresIn(client.getAccessTokenLifetime())
            .scope(String.join(" ", tokenData.getScopes()))
            .build();
    }

    /**
     * Validate client credentials for client_credentials grant.
     */
    public TokenResponse clientCredentialsGrant(String clientId, String clientSecret, Set<String> scopes) {
        SmartClient client = clientRepository.findByClientIdAndActiveTrue(clientId)
            .orElseThrow(() -> new SmartAuthorizationException("Client not found"));

        if (!client.isConfidentialClient()) {
            throw new SmartAuthorizationException("Client credentials grant only for confidential clients");
        }

        if (!client.getClientSecret().equals(clientSecret)) {
            throw new SmartAuthorizationException("Invalid client credentials");
        }

        // Validate requested scopes
        for (String scope : scopes) {
            if (!client.isAllowedScope(scope)) {
                throw new SmartAuthorizationException("Scope not allowed: " + scope);
            }
        }

        String accessToken = generateAccessToken(client, scopes, null);

        return TokenResponse.builder()
            .accessToken(accessToken)
            .tokenType("Bearer")
            .expiresIn(client.getAccessTokenLifetime())
            .scope(String.join(" ", scopes))
            .build();
    }

    /**
     * Revoke a token.
     */
    public void revokeToken(String token, String tokenTypeHint) {
        // Try to revoke as refresh token
        if (refreshTokens.remove(token) != null) {
            log.debug("Revoked refresh token");
            return;
        }

        // Access tokens are stateless - we could add to a blacklist here
        log.debug("Token revocation requested (access tokens are stateless)");
    }

    /**
     * Generate JWT access token.
     */
    private String generateAccessToken(SmartClient client, Set<String> scopes, SmartLaunchContext context) {
        Instant now = Instant.now();
        Instant expiry = now.plus(client.getAccessTokenLifetime(), ChronoUnit.SECONDS);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        var builder = Jwts.builder()
            .setSubject(client.getClientId())
            .setIssuer(issuer)
            .setAudience(issuer)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiry))
            .setId(UUID.randomUUID().toString())
            .claim("scope", String.join(" ", scopes))
            .claim("client_id", client.getClientId());

        if (context != null) {
            if (context.getPatient() != null) {
                builder.claim("patient", context.getPatient());
            }
            if (context.getEncounter() != null) {
                builder.claim("encounter", context.getEncounter());
            }
            if (context.getFhirUser() != null) {
                builder.claim("fhirUser", context.getFhirUser());
            }
            if (context.getTenant() != null) {
                builder.claim("tenant", context.getTenant());
            }
        }

        return builder.signWith(key, SignatureAlgorithm.HS256).compact();
    }

    /**
     * Generate refresh token.
     */
    private String generateRefreshToken(String clientId, Set<String> scopes, SmartLaunchContext context) {
        String token = generateSecureCode();

        SmartClient client = clientRepository.findByClientIdAndActiveTrue(clientId)
            .orElseThrow(() -> new SmartAuthorizationException("Client not found"));

        RefreshTokenData tokenData = RefreshTokenData.builder()
            .clientId(clientId)
            .scopes(scopes)
            .launchContext(context)
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plus(client.getRefreshTokenLifetime(), ChronoUnit.SECONDS))
            .build();

        refreshTokens.put(token, tokenData);
        return token;
    }

    /**
     * Validate PKCE code verifier.
     */
    private boolean validatePkce(String codeVerifier, String codeChallenge, String method) {
        if ("plain".equals(method)) {
            return codeVerifier.equals(codeChallenge);
        } else if ("S256".equals(method)) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
                String computed = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
                return computed.equals(codeChallenge);
            } catch (NoSuchAlgorithmException e) {
                throw new SmartAuthorizationException("PKCE validation failed");
            }
        }
        return false;
    }

    /**
     * Generate secure random code.
     */
    private String generateSecureCode() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Validate access token and extract claims.
     */
    public Claims validateAccessToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * Authorization code data structure.
     */
    @lombok.Data
    @lombok.Builder
    private static class AuthorizationCodeData {
        private String clientId;
        private String redirectUri;
        private Set<String> scopes;
        private String state;
        private String codeChallenge;
        private String codeChallengeMethod;
        private SmartLaunchContext launchContext;
        private Instant createdAt;
        private Instant expiresAt;
    }

    /**
     * Refresh token data structure.
     */
    @lombok.Data
    @lombok.Builder
    private static class RefreshTokenData {
        private String clientId;
        private Set<String> scopes;
        private SmartLaunchContext launchContext;
        private Instant createdAt;
        private Instant expiresAt;
    }
}
