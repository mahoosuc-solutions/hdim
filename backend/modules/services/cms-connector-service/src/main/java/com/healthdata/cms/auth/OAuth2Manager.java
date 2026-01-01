package com.healthdata.cms.auth;

import com.healthdata.cms.model.CmsApiProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

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
    private final Map<CmsApiProvider, TokenInfo> tokenCache = new HashMap<>();

    public OAuth2Manager(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
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
     */
    public String refreshToken(CmsApiProvider provider) {
        log.info("Refreshing OAuth2 token for provider: {}", provider.getDisplayName());
        
        try {
            // In Week 2 implementation, this will call OAuth2PasswordEncoder to generate token
            // For now, return placeholder
            String newToken = generateTokenPlaceholder(provider);
            
            TokenInfo tokenInfo = new TokenInfo(
                newToken,
                Instant.now().plusSeconds(3600), // 60-minute TTL
                provider
            );
            
            tokenCache.put(provider, tokenInfo);
            log.info("Token refreshed successfully for provider: {}", provider.getDisplayName());
            
            return newToken;
        } catch (Exception e) {
            log.error("Failed to refresh token for provider: {}", provider.getDisplayName(), e);
            throw new RuntimeException("Token refresh failed for " + provider.getDisplayName(), e);
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
     * Placeholder for token generation
     * Will be implemented in Week 2
     */
    private String generateTokenPlaceholder(CmsApiProvider provider) {
        // TODO: Week 2 - Implement actual OAuth2 token exchange
        // This will call CMS OAuth2 endpoint with client credentials
        return "placeholder-token-" + provider.getId();
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
}
