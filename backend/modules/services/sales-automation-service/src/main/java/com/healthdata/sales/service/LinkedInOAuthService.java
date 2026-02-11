package com.healthdata.sales.service;

import com.healthdata.sales.config.LinkedInConfig;
import com.healthdata.sales.entity.LinkedInToken;
import com.healthdata.sales.entity.LinkedInToken.TokenStatus;
import com.healthdata.sales.repository.LinkedInTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * LinkedIn OAuth 2.0 Service
 *
 * Handles the OAuth 2.0 authorization code flow for LinkedIn:
 * 1. Generate authorization URL for user consent
 * 2. Exchange authorization code for access/refresh tokens
 * 3. Refresh expired access tokens
 * 4. Revoke tokens when disconnecting
 *
 * Requires LinkedIn Developer App with:
 * - Sign In with LinkedIn using OpenID Connect
 * - Share on LinkedIn
 * - Marketing Developer Platform (for connections API)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LinkedInOAuthService {

    private final LinkedInConfig linkedInConfig;
    private final LinkedInTokenRepository tokenRepository;
    private final RestTemplate restTemplate;

    // State storage for CSRF protection (in production, use Redis)
    private final Map<String, OAuthState> pendingStates = new HashMap<>();

    /**
     * Generate the authorization URL for LinkedIn OAuth
     */
    public AuthorizationUrlResponse getAuthorizationUrl(UUID tenantId, UUID userId, String redirectUri) {
        if (!linkedInConfig.getApi().isEnabled()) {
            throw new IllegalStateException("LinkedIn API integration is not enabled");
        }

        String state = generateState();
        pendingStates.put(state, new OAuthState(tenantId, userId, Instant.now()));

        String effectiveRedirectUri = redirectUri != null ? redirectUri : linkedInConfig.getOauth().getRedirectUri();

        String authUrl = UriComponentsBuilder.fromHttpUrl(linkedInConfig.getApi().getAuthUrl() + "/authorization")
            .queryParam("response_type", "code")
            .queryParam("client_id", linkedInConfig.getOauth().getClientId())
            .queryParam("redirect_uri", effectiveRedirectUri)
            .queryParam("state", state)
            .queryParam("scope", linkedInConfig.getOauth().getScope())
            .build()
            .toUriString();

        return new AuthorizationUrlResponse(authUrl, state);
    }

    /**
     * Exchange authorization code for tokens
     */
    @Transactional
    public LinkedInTokenResponse exchangeCodeForTokens(String code, String state, String redirectUri) {
        // Validate state
        OAuthState oauthState = pendingStates.remove(state);
        if (oauthState == null) {
            throw new IllegalArgumentException("Invalid or expired state parameter");
        }

        // Check state expiration (10 minutes)
        if (oauthState.createdAt().isBefore(Instant.now().minusSeconds(600))) {
            throw new IllegalArgumentException("State parameter has expired");
        }

        // Exchange code for tokens
        String effectiveRedirectUri = redirectUri != null ? redirectUri : linkedInConfig.getOauth().getRedirectUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", effectiveRedirectUri);
        body.add("client_id", linkedInConfig.getOauth().getClientId());
        body.add("client_secret", linkedInConfig.getOauth().getClientSecret());

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                linkedInConfig.getApi().getAuthUrl() + "/accessToken",
                new HttpEntity<>(body, headers),
                Map.class
            );

            Map<String, Object> tokenData = response.getBody();
            if (tokenData == null) {
                throw new RuntimeException("Empty response from LinkedIn token endpoint");
            }

            String accessToken = (String) tokenData.get("access_token");
            String refreshToken = (String) tokenData.get("refresh_token");
            Integer expiresIn = (Integer) tokenData.get("expires_in");
            Integer refreshExpiresIn = (Integer) tokenData.get("refresh_token_expires_in");
            String scope = (String) tokenData.get("scope");

            // Fetch user profile
            ProfileInfo profile = fetchUserProfile(accessToken);

            // Store or update token
            LinkedInToken token = tokenRepository.findByTenantIdAndUserId(
                oauthState.tenantId(), oauthState.userId()
            ).orElse(
                LinkedInToken.builder()
                    .tenantId(oauthState.tenantId())
                    .userId(oauthState.userId())
                    .build()
            );

            token.setAccessToken(accessToken);
            token.setRefreshToken(refreshToken);
            token.setExpiresAt(Instant.now().plusSeconds(expiresIn != null ? expiresIn : 5184000)); // 60 days default
            if (refreshExpiresIn != null) {
                token.setRefreshExpiresAt(Instant.now().plusSeconds(refreshExpiresIn));
            }
            token.setScope(scope);
            token.setLinkedInMemberId(profile.id());
            token.setDisplayName(profile.name());
            token.setEmail(profile.email());
            token.setStatus(TokenStatus.ACTIVE);
            token.setErrorMessage(null);

            tokenRepository.save(token);

            log.info("Successfully connected LinkedIn account for user {} (member: {})",
                oauthState.userId(), profile.id());

            return new LinkedInTokenResponse(
                true,
                profile.name(),
                profile.email(),
                token.getExpiresAt()
            );

        } catch (RestClientException e) {
            log.error("Failed to exchange code for LinkedIn tokens: {}", e.getMessage());
            throw new RuntimeException("Failed to exchange authorization code: " + e.getMessage());
        }
    }

    /**
     * Refresh an expired access token
     */
    @Transactional
    public boolean refreshToken(UUID tenantId, UUID userId) {
        LinkedInToken token = tokenRepository.findByTenantIdAndUserId(tenantId, userId)
            .orElseThrow(() -> new IllegalArgumentException("No LinkedIn token found for user"));

        if (token.getRefreshToken() == null) {
            log.warn("No refresh token available for user {}", userId);
            return false;
        }

        if (token.isRefreshTokenExpired()) {
            log.warn("Refresh token expired for user {}", userId);
            token.markError("Refresh token expired - user must re-authorize");
            tokenRepository.save(token);
            return false;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", token.getRefreshToken());
        body.add("client_id", linkedInConfig.getOauth().getClientId());
        body.add("client_secret", linkedInConfig.getOauth().getClientSecret());

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                linkedInConfig.getApi().getAuthUrl() + "/accessToken",
                new HttpEntity<>(body, headers),
                Map.class
            );

            Map<String, Object> tokenData = response.getBody();
            if (tokenData == null) {
                throw new RuntimeException("Empty response from refresh endpoint");
            }

            String newAccessToken = (String) tokenData.get("access_token");
            String newRefreshToken = (String) tokenData.get("refresh_token");
            Integer expiresIn = (Integer) tokenData.get("expires_in");
            Integer refreshExpiresIn = (Integer) tokenData.get("refresh_token_expires_in");

            token.updateToken(
                newAccessToken,
                newRefreshToken,
                expiresIn != null ? expiresIn : 5184000,
                refreshExpiresIn != null ? (long) refreshExpiresIn : null
            );

            tokenRepository.save(token);
            log.info("Successfully refreshed LinkedIn token for user {}", userId);
            return true;

        } catch (RestClientException e) {
            log.error("Failed to refresh LinkedIn token for user {}: {}", userId, e.getMessage());
            token.markError("Token refresh failed: " + e.getMessage());
            tokenRepository.save(token);
            return false;
        }
    }

    /**
     * Disconnect LinkedIn account (revoke tokens)
     */
    @Transactional
    public void disconnect(UUID tenantId, UUID userId) {
        LinkedInToken token = tokenRepository.findByTenantIdAndUserId(tenantId, userId)
            .orElse(null);

        if (token == null) {
            log.info("No LinkedIn token found to disconnect for user {}", userId);
            return;
        }

        // Note: LinkedIn doesn't have a programmatic token revocation endpoint
        // The user must manually revoke access in their LinkedIn settings
        // We just mark it as revoked in our system
        token.markRevoked();
        tokenRepository.save(token);

        log.info("Disconnected LinkedIn account for user {}", userId);
    }

    /**
     * Check if user has an active LinkedIn connection
     */
    @Transactional(readOnly = true)
    public ConnectionStatus getConnectionStatus(UUID tenantId, UUID userId) {
        Optional<LinkedInToken> tokenOpt = tokenRepository.findByTenantIdAndUserId(tenantId, userId);

        if (tokenOpt.isEmpty()) {
            return new ConnectionStatus(false, null, null, null, null);
        }

        LinkedInToken token = tokenOpt.get();
        return new ConnectionStatus(
            token.getStatus() == TokenStatus.ACTIVE,
            token.getDisplayName(),
            token.getEmail(),
            token.getStatus().name(),
            token.getErrorMessage()
        );
    }

    /**
     * Get a valid access token for API calls, refreshing if needed
     */
    @Transactional
    public Optional<String> getValidAccessToken(UUID tenantId, UUID userId) {
        LinkedInToken token = tokenRepository.findByTenantIdAndUserId(tenantId, userId)
            .orElse(null);

        if (token == null || token.getStatus() != TokenStatus.ACTIVE) {
            return Optional.empty();
        }

        if (token.isAccessTokenExpired()) {
            boolean refreshed = refreshToken(tenantId, userId);
            if (!refreshed) {
                return Optional.empty();
            }
            // Reload after refresh
            token = tokenRepository.findByTenantIdAndUserId(tenantId, userId).orElse(null);
            if (token == null) {
                return Optional.empty();
            }
        }

        token.markUsed();
        tokenRepository.save(token);

        return Optional.of(token.getAccessToken());
    }

    /**
     * Scheduled job to proactively refresh tokens expiring soon
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional
    public void refreshExpiringSoonTokens() {
        if (!linkedInConfig.getApi().isEnabled()) {
            return;
        }

        // Find tokens expiring in the next 24 hours
        Instant threshold = Instant.now().plusSeconds(86400);
        List<LinkedInToken> expiringTokens = tokenRepository.findTokensExpiringSoon(threshold);

        log.info("Found {} LinkedIn tokens expiring soon", expiringTokens.size());

        for (LinkedInToken token : expiringTokens) {
            try {
                refreshToken(token.getTenantId(), token.getUserId());
            } catch (Exception e) {
                log.error("Failed to proactively refresh token for user {}: {}",
                    token.getUserId(), e.getMessage());
            }
        }
    }

    // ==================== Helper Methods ====================

    private String generateState() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private ProfileInfo fetchUserProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        try {
            // Fetch basic profile
            ResponseEntity<Map> profileResponse = restTemplate.exchange(
                linkedInConfig.getApi().getBaseUrl() + "/userinfo",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );

            Map<String, Object> profile = profileResponse.getBody();
            if (profile == null) {
                return new ProfileInfo(null, "Unknown", null);
            }

            String id = (String) profile.get("sub");
            String name = (String) profile.get("name");
            String email = (String) profile.get("email");

            return new ProfileInfo(id, name, email);

        } catch (RestClientException e) {
            log.warn("Failed to fetch LinkedIn profile: {}", e.getMessage());
            return new ProfileInfo(null, "Unknown", null);
        }
    }

    // ==================== DTOs ====================

    public record AuthorizationUrlResponse(String authorizationUrl, String state) {}

    public record LinkedInTokenResponse(
        boolean success,
        String displayName,
        String email,
        Instant expiresAt
    ) {}

    public record ConnectionStatus(
        boolean connected,
        String displayName,
        String email,
        String status,
        String errorMessage
    ) {}

    private record OAuthState(UUID tenantId, UUID userId, Instant createdAt) {}

    private record ProfileInfo(String id, String name, String email) {}
}
