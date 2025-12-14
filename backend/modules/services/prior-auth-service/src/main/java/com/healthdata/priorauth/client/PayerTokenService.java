package com.healthdata.priorauth.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.priorauth.persistence.PayerEndpointEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing OAuth tokens for payer API authentication.
 *
 * Handles token acquisition, caching, and refresh for multiple payers.
 * Supports OAuth2 Client Credentials and SMART on FHIR flows.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PayerTokenService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Token cache with expiration tracking
    private final Map<String, TokenInfo> tokenCache = new ConcurrentHashMap<>();

    /**
     * Get a valid access token for a payer endpoint.
     *
     * @param endpoint The payer endpoint configuration
     * @return A valid access token
     */
    public String getAccessToken(PayerEndpointEntity endpoint) {
        String cacheKey = endpoint.getPayerId();

        // Check cache first
        TokenInfo cachedToken = tokenCache.get(cacheKey);
        if (cachedToken != null && !cachedToken.isExpired()) {
            log.debug("Using cached token for payer: {}", endpoint.getPayerId());
            return cachedToken.getAccessToken();
        }

        // Acquire new token
        log.info("Acquiring new token for payer: {}", endpoint.getPayerId());
        TokenInfo newToken = acquireToken(endpoint);

        // Cache the token
        tokenCache.put(cacheKey, newToken);

        return newToken.getAccessToken();
    }

    /**
     * Invalidate cached token for a payer.
     */
    public void invalidateToken(String payerId) {
        tokenCache.remove(payerId);
        log.info("Invalidated token cache for payer: {}", payerId);
    }

    private TokenInfo acquireToken(PayerEndpointEntity endpoint) {
        return switch (endpoint.getAuthType()) {
            case OAUTH2_CLIENT_CREDENTIALS -> acquireClientCredentialsToken(endpoint);
            case SMART_ON_FHIR -> acquireSmartToken(endpoint);
            default -> throw new UnsupportedOperationException(
                "Auth type not supported for token acquisition: " + endpoint.getAuthType());
        };
    }

    private TokenInfo acquireClientCredentialsToken(PayerEndpointEntity endpoint) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", endpoint.getClientId());
        body.add("client_secret", endpoint.getClientSecret());

        if (endpoint.getScope() != null) {
            body.add("scope", endpoint.getScope());
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                endpoint.getTokenEndpointUrl(),
                HttpMethod.POST,
                request,
                String.class
            );

            return parseTokenResponse(response.getBody());

        } catch (Exception e) {
            log.error("Failed to acquire token for payer {}: {}", endpoint.getPayerId(), e.getMessage());
            throw new RuntimeException("Token acquisition failed: " + e.getMessage(), e);
        }
    }

    private TokenInfo acquireSmartToken(PayerEndpointEntity endpoint) {
        // SMART on FHIR uses similar flow but may require additional parameters
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", endpoint.getClientId());

        // SMART backends typically use client assertion instead of secret
        if (endpoint.getClientSecret() != null) {
            body.add("client_secret", endpoint.getClientSecret());
        }

        if (endpoint.getScope() != null) {
            body.add("scope", endpoint.getScope());
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                endpoint.getTokenEndpointUrl(),
                HttpMethod.POST,
                request,
                String.class
            );

            return parseTokenResponse(response.getBody());

        } catch (Exception e) {
            log.error("Failed to acquire SMART token: {}", e.getMessage());
            throw new RuntimeException("SMART token acquisition failed: " + e.getMessage(), e);
        }
    }

    private TokenInfo parseTokenResponse(String responseBody) {
        try {
            JsonNode json = objectMapper.readTree(responseBody);

            String accessToken = json.get("access_token").asText();
            int expiresIn = json.has("expires_in") ? json.get("expires_in").asInt() : 3600;
            String tokenType = json.has("token_type") ? json.get("token_type").asText() : "Bearer";

            return TokenInfo.builder()
                .accessToken(accessToken)
                .tokenType(tokenType)
                .expiresAt(LocalDateTime.now().plusSeconds(expiresIn - 60)) // 60s buffer
                .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse token response: " + e.getMessage(), e);
        }
    }

    /**
     * Token information holder.
     */
    @lombok.Data
    @lombok.Builder
    public static class TokenInfo {
        private String accessToken;
        private String tokenType;
        private LocalDateTime expiresAt;

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }
}
