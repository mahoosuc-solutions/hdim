package com.healthdata.cms.auth;

import com.healthdata.cms.dto.OAuth2TokenResponse;
import com.healthdata.cms.exception.CmsApiException;
import com.healthdata.cms.model.CmsApiProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OAuth2 Token Manager for CMS API Authentication
 * 
 * Manages OAuth2 client credentials flow for CMS API integrations.
 * Features:
 * - Token caching with TTL-based expiry
 * - Automatic token refresh (50-minute interval for 60-minute tokens)
 * - Support for multiple CMS API providers
 * - Resilience and retry logic
 */
@Slf4j
@Component
public class OAuth2Manager {

    @Value("${cms.oauth2.client-id:}")
    private String clientId;

    @Value("${cms.oauth2.client-secret:}")
    private String clientSecret;

    @Value("${cms.oauth2.token-endpoint:https://auth.cms.gov/oauth/token}")
    private String tokenEndpoint;

    @Value("${cms.oauth2.scopes:beneficiary-claims}")
    private String scopes;

    @Value("${cms.oauth2.token-refresh-interval:3000}")
    private Long tokenRefreshIntervalMs; // 50 minutes in ms

    private final RestTemplate restTemplate;
    private final Map<CmsApiProvider, TokenInfo> tokenCache = new ConcurrentHashMap<>();

    // Provider-specific configuration
    private final Map<CmsApiProvider, ProviderConfig> providerConfigs = new ConcurrentHashMap<>();

    public OAuth2Manager(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        initializeProviderConfigs();
    }

    /**
     * Initialize provider-specific OAuth2 configurations
     */
    private void initializeProviderConfigs() {
        // DPC (Data at Point of Care) - Real-time queries
        providerConfigs.put(CmsApiProvider.DPC, new ProviderConfig(
            "https://sandbox.dpc.cms.gov/api/v1/Token/auth",
            "system/*.read"
        ));

        // BCDA (Beneficiary Claims Data API) - Bulk export
        providerConfigs.put(CmsApiProvider.BCDA, new ProviderConfig(
            "https://sandbox.bcda.cms.gov/auth/token",
            "bcda-api"
        ));

        // Blue Button 2.0 - Beneficiary-authorized
        providerConfigs.put(CmsApiProvider.BLUE_BUTTON_2_0, new ProviderConfig(
            "https://sandbox.bluebutton.cms.gov/v2/o/token/",
            "patient/*.read"
        ));

        // AB2D (Medicare Part D Claims) - Bulk export for PDP sponsors
        providerConfigs.put(CmsApiProvider.AB2D, new ProviderConfig(
            "https://sandbox.ab2d.cms.gov/api/v1/fhir/token",
            "ab2d-api"
        ));

        // QPP Submissions - Quality measure submission
        providerConfigs.put(CmsApiProvider.QPP_SUBMISSIONS, new ProviderConfig(
            "https://sandbox.qpp.cms.gov/oauth/token",
            "qpp-api"
        ));

        log.info("Initialized OAuth2 configurations for {} CMS API providers", providerConfigs.size());
    }

    /**
     * Get valid access token for a CMS API provider
     * Returns cached token if valid, otherwise refreshes
     */
    @Cacheable(value = "cms-tokens", key = "#provider.id", unless = "#result == null")
    public String getAccessToken(CmsApiProvider provider) {
        log.debug("Fetching access token for provider: {}", provider.getDisplayName());
        
        TokenInfo cachedToken = tokenCache.get(provider);
        
        if (cachedToken != null && isTokenValid(cachedToken)) {
            log.debug("Using cached token for provider: {}", provider.getDisplayName());
            return cachedToken.getAccessToken();
        }

        return refreshToken(provider);
    }

    /**
     * Refresh token for a CMS API provider
     * Performs actual OAuth2 client credentials exchange
     *
     * @param provider The CMS API provider
     * @return The new access token
     * @throws CmsApiException if token exchange fails
     */
    public String refreshToken(CmsApiProvider provider) {
        log.info("Refreshing OAuth2 token for provider: {}", provider.getDisplayName());

        try {
            // Perform actual OAuth2 token exchange
            String newToken = performTokenExchange(provider);

            // Cache the token with TTL based on provider response (default 60 min)
            TokenInfo tokenInfo = new TokenInfo(
                newToken,
                Instant.now().plusSeconds(3600), // 60-minute TTL (will be updated by response)
                provider
            );

            tokenCache.put(provider, tokenInfo);
            log.info("Token refreshed successfully for provider: {}", provider.getDisplayName());

            return newToken;

        } catch (CmsApiException e) {
            log.error("Failed to refresh token for provider: {}", provider.getDisplayName(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error refreshing token for provider: {}", provider.getDisplayName(), e);
            throw new CmsApiException("Token refresh failed for " + provider.getDisplayName(), e);
        }
    }

    /**
     * Check if token is still valid
     * Considers token expired if less than 5 minutes remaining
     */
    private boolean isTokenValid(TokenInfo tokenInfo) {
        long expiresInMs = tokenInfo.getExpiresAt().toEpochMilli() - System.currentTimeMillis();
        long bufferMs = 5 * 60 * 1000; // 5-minute buffer
        return expiresInMs > bufferMs;
    }

    /**
     * Scheduled task to proactively refresh tokens
     * Runs every 50 minutes to refresh 60-minute tokens
     */
    @Scheduled(fixedRateString = "${cms.oauth2.token-refresh-interval:3000000}")
    @CacheEvict(value = "cms-tokens", allEntries = true)
    public void proactiveTokenRefresh() {
        log.info("Starting proactive token refresh for all CMS API providers");
        
        for (CmsApiProvider provider : CmsApiProvider.values()) {
            try {
                refreshToken(provider);
            } catch (Exception e) {
                log.warn("Failed to proactively refresh token for {}", provider.getDisplayName(), e);
            }
        }
        
        log.info("Proactive token refresh completed");
    }

    /**
     * Get token information for a specific provider
     */
    public TokenInfo getTokenInfo(CmsApiProvider provider) {
        return tokenCache.get(provider);
    }

    /**
     * Clear all cached tokens (useful for testing or forced refresh)
     */
    public void clearAllTokens() {
        log.info("Clearing all cached OAuth2 tokens");
        tokenCache.clear();
    }

    /**
     * Clear token for specific provider
     */
    public void clearToken(CmsApiProvider provider) {
        log.info("Clearing cached token for provider: {}", provider.getDisplayName());
        tokenCache.remove(provider);
    }

    /**
     * Perform actual OAuth2 client credentials token exchange
     * Calls the CMS OAuth2 endpoint with client credentials
     *
     * @param provider The CMS API provider to get token for
     * @return Access token from OAuth2 response
     * @throws CmsApiException if token exchange fails
     */
    private String performTokenExchange(CmsApiProvider provider) {
        ProviderConfig config = providerConfigs.get(provider);
        if (config == null) {
            throw new CmsApiException("No OAuth2 configuration found for provider: " + provider.getDisplayName());
        }

        // Build request body for client_credentials grant
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "client_credentials");
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("scope", config.getScopes());

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);

        String tokenUrl = config.getTokenEndpoint();
        log.debug("Requesting token from {} for provider {}", tokenUrl, provider.getDisplayName());

        try {
            ResponseEntity<OAuth2TokenResponse> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                request,
                OAuth2TokenResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                OAuth2TokenResponse tokenResponse = response.getBody();
                log.info("Successfully obtained OAuth2 token for provider: {} (expires in {} seconds)",
                    provider.getDisplayName(), tokenResponse.getExpiresIn());
                return tokenResponse.getAccessToken();
            }

            throw new CmsApiException("Empty response from OAuth2 token endpoint for " + provider.getDisplayName());

        } catch (HttpClientErrorException e) {
            log.error("Client error during token exchange for {}: {} - {}",
                provider.getDisplayName(), e.getStatusCode(), e.getResponseBodyAsString());
            throw new CmsApiException("OAuth2 token request failed for " + provider.getDisplayName() +
                ": " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);

        } catch (HttpServerErrorException e) {
            log.error("Server error during token exchange for {}: {} - {}",
                provider.getDisplayName(), e.getStatusCode(), e.getResponseBodyAsString());
            throw new CmsApiException("OAuth2 server error for " + provider.getDisplayName() +
                ": " + e.getStatusCode(), e);

        } catch (RestClientException e) {
            log.error("Network error during token exchange for {}: {}",
                provider.getDisplayName(), e.getMessage());
            throw new CmsApiException("Network error during OAuth2 token exchange for " +
                provider.getDisplayName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Inner class to hold token information
     */
    public static class TokenInfo {
        private final String accessToken;
        private final Instant expiresAt;
        private final CmsApiProvider provider;

        public TokenInfo(String accessToken, Instant expiresAt, CmsApiProvider provider) {
            this.accessToken = accessToken;
            this.expiresAt = expiresAt;
            this.provider = provider;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }

        public CmsApiProvider getProvider() {
            return provider;
        }
    }

    /**
     * Inner class to hold provider-specific OAuth2 configuration
     */
    public static class ProviderConfig {
        private final String tokenEndpoint;
        private final String scopes;

        public ProviderConfig(String tokenEndpoint, String scopes) {
            this.tokenEndpoint = tokenEndpoint;
            this.scopes = scopes;
        }

        public String getTokenEndpoint() {
            return tokenEndpoint;
        }

        public String getScopes() {
            return scopes;
        }
    }
}
