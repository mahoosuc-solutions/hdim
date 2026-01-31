package com.healthdata.authentication.service;

import com.healthdata.authentication.config.SmartOnFhirConfig;
import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for handling SMART on FHIR authorization flows.
 *
 * Manages:
 * - Authorization code generation and validation
 * - PKCE (Proof Key for Code Exchange) validation
 * - Client authentication
 * - Token generation and refresh
 * - JWKS (JSON Web Key Set) generation
 *
 * Security Features:
 * - Authorization codes expire after 10 minutes (SMART requirement)
 * - PKCE support for public clients
 * - Refresh token rotation for enhanced security
 *
 * Note: In production, authorization codes should be stored in Redis or a database
 * for distributed deployments. This implementation uses in-memory storage for
 * single-instance deployments and testing.
 */
@Slf4j
@Service
@ConditionalOnBean(UserRepository.class)
@ConditionalOnProperty(prefix = "smart", name = "enabled", havingValue = "true")
public class SmartAuthService {

    private final SmartOnFhirConfig smartConfig;
    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;

    // In-memory storage for authorization codes (use Redis in production)
    private final Map<String, AuthorizationCodeData> authorizationCodes = new ConcurrentHashMap<>();

    // In-memory storage for refresh tokens mapped to user context (use DB in production)
    private final Map<String, RefreshTokenData> refreshTokens = new ConcurrentHashMap<>();

    // Registered SMART clients (in production, load from database)
    private final Map<String, SmartClient> registeredClients = new ConcurrentHashMap<>();

    // SMART authorization code validity (10 minutes per spec)
    private static final long AUTH_CODE_VALIDITY_SECONDS = 600;

    /**
     * Constructor for dependency injection.
     */
    public SmartAuthService(SmartOnFhirConfig smartConfig, JwtTokenService jwtTokenService,
                            UserRepository userRepository) {
        this.smartConfig = smartConfig;
        this.jwtTokenService = jwtTokenService;
        this.userRepository = userRepository;
    }

    /**
     * Initialize with some default test clients.
     * In production, clients would be registered via admin API or database.
     */
    @PostConstruct
    public void init() {
        // Register default test client for development
        registerClient(SmartClient.builder()
                .clientId("hdim-smart-app")
                .clientSecret("hdim-smart-secret") // Confidential client
                .name("HDIM SMART Application")
                .redirectUris(Set.of("http://localhost:3000/callback", "https://app.healthdata.com/callback"))
                .scopes(Set.of("launch", "launch/patient", "patient/*.read", "openid", "profile", "fhirUser"))
                .confidential(true)
                .build());

        // Register public client (no secret, requires PKCE)
        registerClient(SmartClient.builder()
                .clientId("hdim-patient-portal")
                .name("HDIM Patient Portal")
                .redirectUris(Set.of("http://localhost:3001/callback", "https://patient.healthdata.com/callback"))
                .scopes(Set.of("launch/patient", "patient/*.read", "openid", "profile"))
                .confidential(false)
                .build());

        log.info("SmartAuthService initialized with {} default clients", registeredClients.size());
    }

    /**
     * Register a SMART client.
     */
    public void registerClient(SmartClient client) {
        registeredClients.put(client.getClientId(), client);
        log.debug("Registered SMART client: {}", client.getClientId());
    }

    /**
     * Validate client credentials.
     *
     * @param clientId client ID
     * @param clientSecret client secret (null for public clients)
     * @return true if client is valid
     */
    public boolean validateClient(String clientId, String clientSecret) {
        SmartClient client = registeredClients.get(clientId);
        if (client == null) {
            log.warn("Unknown client ID: {}", clientId);
            return false;
        }

        if (client.isConfidential()) {
            // Confidential client requires secret
            if (clientSecret == null || !clientSecret.equals(client.getClientSecret())) {
                log.warn("Invalid client secret for client: {}", clientId);
                return false;
            }
        }

        return true;
    }

    /**
     * Validate redirect URI for a client.
     */
    public boolean validateRedirectUri(String clientId, String redirectUri) {
        SmartClient client = registeredClients.get(clientId);
        if (client == null) {
            return false;
        }
        return client.getRedirectUris().contains(redirectUri);
    }

    /**
     * Generate an authorization code for the user.
     *
     * @param userId user ID
     * @param clientId client ID
     * @param redirectUri redirect URI
     * @param scope granted scopes
     * @param codeChallenge PKCE code challenge (optional)
     * @param codeChallengeMethod PKCE code challenge method (S256)
     * @param patientId patient context (for launch/patient)
     * @return authorization code
     */
    public String generateAuthorizationCode(UUID userId, String clientId, String redirectUri,
                                             String scope, String codeChallenge,
                                             String codeChallengeMethod, String patientId) {
        String code = UUID.randomUUID().toString();

        AuthorizationCodeData codeData = AuthorizationCodeData.builder()
                .code(code)
                .userId(userId)
                .clientId(clientId)
                .redirectUri(redirectUri)
                .scope(scope)
                .codeChallenge(codeChallenge)
                .codeChallengeMethod(codeChallengeMethod)
                .patientId(patientId)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(AUTH_CODE_VALIDITY_SECONDS))
                .build();

        authorizationCodes.put(code, codeData);
        log.debug("Generated authorization code for user {} and client {}", userId, clientId);

        // Cleanup expired codes periodically
        cleanupExpiredCodes();

        return code;
    }

    /**
     * Exchange authorization code for tokens.
     *
     * @param code authorization code
     * @param clientId client ID
     * @param clientSecret client secret
     * @param redirectUri redirect URI
     * @param codeVerifier PKCE code verifier
     * @return token response or null if invalid
     */
    public TokenResponse exchangeAuthorizationCode(String code, String clientId, String clientSecret,
                                                    String redirectUri, String codeVerifier) {
        AuthorizationCodeData codeData = authorizationCodes.remove(code);

        if (codeData == null) {
            log.warn("Authorization code not found or already used: {}", code);
            return null;
        }

        // Validate code hasn't expired
        if (codeData.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Authorization code expired");
            return null;
        }

        // Validate client
        if (!codeData.getClientId().equals(clientId)) {
            log.warn("Client ID mismatch for authorization code");
            return null;
        }

        // Validate redirect URI
        if (!codeData.getRedirectUri().equals(redirectUri)) {
            log.warn("Redirect URI mismatch for authorization code");
            return null;
        }

        // Validate client credentials
        if (!validateClient(clientId, clientSecret)) {
            return null;
        }

        // Validate PKCE if code challenge was provided
        if (codeData.getCodeChallenge() != null) {
            if (!validatePkce(codeVerifier, codeData.getCodeChallenge(), codeData.getCodeChallengeMethod())) {
                log.warn("PKCE validation failed");
                return null;
            }
        } else {
            // For public clients without PKCE, require PKCE if configured
            SmartClient client = registeredClients.get(clientId);
            if (client != null && !client.isConfidential() && smartConfig.isRequirePkce()) {
                log.warn("PKCE required for public client but not provided");
                return null;
            }
        }

        // Get user and generate tokens
        Optional<User> userOpt = userRepository.findById(codeData.getUserId());
        if (userOpt.isEmpty()) {
            log.warn("User not found for authorization code");
            return null;
        }

        User user = userOpt.get();
        String accessToken = jwtTokenService.generateAccessToken(user);
        String refreshToken = generateAndStoreRefreshToken(user, clientId, codeData.getScope());

        log.info("Successfully exchanged authorization code for tokens for user: {}", user.getUsername());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(smartConfig.getAccessTokenLifetime())
                .refreshToken(refreshToken)
                .scope(codeData.getScope())
                .patientId(codeData.getPatientId())
                .build();
    }

    /**
     * Refresh access token using refresh token.
     *
     * @param refreshToken refresh token
     * @param clientId client ID
     * @param clientSecret client secret
     * @param scope optional new scope (must be subset of original)
     * @return new token response or null if invalid
     */
    public TokenResponse refreshAccessToken(String refreshToken, String clientId,
                                             String clientSecret, String scope) {
        RefreshTokenData tokenData = refreshTokens.get(refreshToken);

        if (tokenData == null) {
            log.warn("Refresh token not found");
            return null;
        }

        // Validate expiration
        if (tokenData.getExpiresAt().isBefore(Instant.now())) {
            refreshTokens.remove(refreshToken);
            log.warn("Refresh token expired");
            return null;
        }

        // Validate client
        if (!tokenData.getClientId().equals(clientId)) {
            log.warn("Client ID mismatch for refresh token");
            return null;
        }

        if (!validateClient(clientId, clientSecret)) {
            return null;
        }

        // Validate scope (new scope must be subset of original)
        String grantedScope = tokenData.getScope();
        if (scope != null && !scope.isBlank()) {
            if (!isSubsetScope(scope, grantedScope)) {
                log.warn("Requested scope exceeds original grant");
                return null;
            }
            grantedScope = scope;
        }

        // Get user and generate new tokens
        Optional<User> userOpt = userRepository.findById(tokenData.getUserId());
        if (userOpt.isEmpty()) {
            log.warn("User not found for refresh token");
            return null;
        }

        User user = userOpt.get();

        // Rotate refresh token (remove old, create new)
        refreshTokens.remove(refreshToken);
        String newAccessToken = jwtTokenService.generateAccessToken(user);
        String newRefreshToken = generateAndStoreRefreshToken(user, clientId, grantedScope);

        log.info("Successfully refreshed tokens for user: {}", user.getUsername());

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(smartConfig.getAccessTokenLifetime())
                .refreshToken(newRefreshToken)
                .scope(grantedScope)
                .build();
    }

    /**
     * Issue tokens for client credentials grant (backend services).
     *
     * @param clientId client ID
     * @param clientSecret client secret
     * @param scope requested scope
     * @return token response or null if invalid
     */
    public TokenResponse issueClientCredentialsToken(String clientId, String clientSecret, String scope) {
        if (!validateClient(clientId, clientSecret)) {
            return null;
        }

        SmartClient client = registeredClients.get(clientId);
        if (client == null || !client.isConfidential()) {
            log.warn("Client credentials grant requires confidential client");
            return null;
        }

        // Validate requested scope
        String grantedScope = scope != null ? scope : "system/*.read";
        if (!isValidClientScope(clientId, grantedScope)) {
            log.warn("Invalid scope for client credentials: {}", grantedScope);
            return null;
        }

        // Generate system access token (no user context)
        String accessToken = generateSystemAccessToken(clientId, grantedScope);

        log.info("Issued client credentials token for client: {}", clientId);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(smartConfig.getAccessTokenLifetime())
                .scope(grantedScope)
                .build();
    }

    /**
     * Validate access token and return user info.
     *
     * @param token access token
     * @return user info map or null if invalid
     */
    public Map<String, Object> validateTokenAndGetUserInfo(String token) {
        try {
            if (!jwtTokenService.validateToken(token)) {
                return null;
            }

            String username = jwtTokenService.extractUsername(token);
            UUID userId = jwtTokenService.extractUserId(token);

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return null;
            }

            User user = userOpt.get();
            Map<String, Object> userInfo = new LinkedHashMap<>();
            userInfo.put("sub", user.getId().toString());
            userInfo.put("name", user.getFullName());
            userInfo.put("email", user.getEmail());
            userInfo.put("email_verified", user.getEmailVerified());

            // Add FHIR user reference (Practitioner for now)
            userInfo.put("fhirUser", "Practitioner/" + user.getId());

            return userInfo;

        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Generate JWKS (JSON Web Key Set) for token validation.
     * Returns public keys used to verify JWT signatures.
     */
    public Map<String, Object> getJwks() {
        // In production, this would load RSA keys from a keystore
        // For HS512 (symmetric), JWKS is not typically published
        // This is a placeholder structure for RS256 keys
        Map<String, Object> jwks = new LinkedHashMap<>();
        List<Map<String, Object>> keys = new ArrayList<>();

        // Placeholder RSA key (in production, generate from actual keystore)
        Map<String, Object> key = new LinkedHashMap<>();
        key.put("kty", "RSA");
        key.put("use", "sig");
        key.put("alg", "RS256");
        key.put("kid", "hdim-auth-key-1");
        // In production, these would be actual Base64url-encoded RSA key components
        key.put("n", Base64.getUrlEncoder().withoutPadding()
                .encodeToString("placeholder-modulus-would-be-rsa-key".getBytes(StandardCharsets.UTF_8)));
        key.put("e", "AQAB");

        keys.add(key);
        jwks.put("keys", keys);

        return jwks;
    }

    /**
     * Validate PKCE code verifier against code challenge.
     */
    private boolean validatePkce(String codeVerifier, String codeChallenge, String method) {
        if (codeVerifier == null || codeChallenge == null) {
            return false;
        }

        // S256 method: SHA256(code_verifier) == code_challenge
        if ("S256".equals(method)) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
                String computed = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
                return codeChallenge.equals(computed);
            } catch (NoSuchAlgorithmException e) {
                log.error("SHA-256 not available for PKCE validation", e);
                return false;
            }
        }

        // Plain method (not recommended, but supported)
        if ("plain".equals(method)) {
            return codeChallenge.equals(codeVerifier);
        }

        log.warn("Unsupported PKCE code challenge method: {}", method);
        return false;
    }

    /**
     * Generate and store a refresh token.
     */
    private String generateAndStoreRefreshToken(User user, String clientId, String scope) {
        String refreshToken = UUID.randomUUID().toString();

        RefreshTokenData tokenData = RefreshTokenData.builder()
                .token(refreshToken)
                .userId(user.getId())
                .clientId(clientId)
                .scope(scope)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(smartConfig.getRefreshTokenLifetime()))
                .build();

        refreshTokens.put(refreshToken, tokenData);
        return refreshToken;
    }

    /**
     * Generate a system access token for client credentials grant.
     */
    private String generateSystemAccessToken(String clientId, String scope) {
        // Create a pseudo-user for system access tokens
        User systemUser = User.builder()
                .id(UUID.nameUUIDFromBytes(("system:" + clientId).getBytes(StandardCharsets.UTF_8)))
                .username("system:" + clientId)
                .email(clientId + "@system.local")
                .firstName("System")
                .lastName(clientId)
                .build();

        return jwtTokenService.generateAccessToken(systemUser);
    }

    /**
     * Check if requested scope is a subset of granted scope.
     */
    private boolean isSubsetScope(String requested, String granted) {
        Set<String> requestedScopes = new HashSet<>(Arrays.asList(requested.split("\\s+")));
        Set<String> grantedScopes = new HashSet<>(Arrays.asList(granted.split("\\s+")));
        return grantedScopes.containsAll(requestedScopes);
    }

    /**
     * Check if scope is valid for client.
     */
    private boolean isValidClientScope(String clientId, String scope) {
        SmartClient client = registeredClients.get(clientId);
        if (client == null) {
            return false;
        }

        Set<String> requestedScopes = new HashSet<>(Arrays.asList(scope.split("\\s+")));
        for (String s : requestedScopes) {
            if (!client.getScopes().contains(s) && !smartConfig.isScopeSupported(s)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Clean up expired authorization codes.
     */
    private void cleanupExpiredCodes() {
        Instant now = Instant.now();
        authorizationCodes.entrySet().removeIf(entry ->
                entry.getValue().getExpiresAt().isBefore(now));
    }

    /**
     * Authorization code data.
     */
    @lombok.Data
    @lombok.Builder
    public static class AuthorizationCodeData {
        private String code;
        private UUID userId;
        private String clientId;
        private String redirectUri;
        private String scope;
        private String codeChallenge;
        private String codeChallengeMethod;
        private String patientId;
        private Instant createdAt;
        private Instant expiresAt;
    }

    /**
     * Refresh token data.
     */
    @lombok.Data
    @lombok.Builder
    public static class RefreshTokenData {
        private String token;
        private UUID userId;
        private String clientId;
        private String scope;
        private Instant createdAt;
        private Instant expiresAt;
    }

    /**
     * Token response.
     */
    @lombok.Data
    @lombok.Builder
    public static class TokenResponse {
        private String accessToken;
        private String tokenType;
        private int expiresIn;
        private String refreshToken;
        private String scope;
        private String patientId;
    }

    /**
     * SMART client registration.
     */
    @lombok.Data
    @lombok.Builder
    public static class SmartClient {
        private String clientId;
        private String clientSecret;
        private String name;
        private Set<String> redirectUris;
        private Set<String> scopes;
        private boolean confidential;
    }
}
