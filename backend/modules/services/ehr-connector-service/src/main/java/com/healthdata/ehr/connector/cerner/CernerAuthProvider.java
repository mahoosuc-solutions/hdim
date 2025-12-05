package com.healthdata.ehr.connector.cerner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.ehr.connector.cerner.config.CernerConnectionConfig;
import com.healthdata.ehr.connector.cerner.model.CernerTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class CernerAuthProvider {

    private final RestTemplate restTemplate;
    private final CernerConnectionConfig config;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;

    private static final String TOKEN_CACHE_NAME = "cernerTokens";
    private static final String TOKEN_CACHE_KEY = "token";

    public String getAccessToken() {
        CernerTokenResponse cachedToken = getCachedToken();
        
        if (cachedToken != null && !cachedToken.isExpired()) {
            log.debug("Using cached Cerner access token");
            return cachedToken.getAccessToken();
        }

        log.info("Requesting new Cerner access token");
        CernerTokenResponse tokenResponse = requestNewToken();
        
        if (tokenResponse == null) {
            throw new RuntimeException("Failed to obtain Cerner access token: null response");
        }

        tokenResponse.setIssuedAt(Instant.now());
        cacheToken(tokenResponse);
        
        return tokenResponse.getAccessToken();
    }

    public String getAuthorizationHeader() {
        String accessToken = getAccessToken();
        return "Bearer " + accessToken;
    }

    public void clearTokenCache() {
        Cache cache = cacheManager.getCache(TOKEN_CACHE_NAME);
        if (cache != null) {
            cache.evict(TOKEN_CACHE_KEY);
            log.info("Cerner token cache cleared");
        }
    }

    public boolean isTokenValid(CernerTokenResponse token) {
        return token != null && !token.isExpired();
    }

    private CernerTokenResponse getCachedToken() {
        Cache cache = cacheManager.getCache(TOKEN_CACHE_NAME);
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(TOKEN_CACHE_KEY);
            if (wrapper != null) {
                return (CernerTokenResponse) wrapper.get();
            }
        }
        return null;
    }

    private void cacheToken(CernerTokenResponse token) {
        Cache cache = cacheManager.getCache(TOKEN_CACHE_NAME);
        if (cache != null) {
            cache.put(TOKEN_CACHE_KEY, token);
        }
    }

    private CernerTokenResponse requestNewToken() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set(HttpHeaders.AUTHORIZATION, createBasicAuthHeader());

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");
            body.add("scope", config.getScope());

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<CernerTokenResponse> response = restTemplate.exchange(
                    config.getTokenUrl(),
                    HttpMethod.POST,
                    request,
                    CernerTokenResponse.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to obtain Cerner access token", e);
            throw new RuntimeException("Failed to obtain Cerner access token", e);
        }
    }

    private String createBasicAuthHeader() {
        String credentials = config.getClientId() + ":" + config.getClientSecret();
        String encodedCredentials = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedCredentials;
    }
}
