package com.healthdata.sales.client;

import com.healthdata.sales.config.ZohoConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
@RequiredArgsConstructor
public class ZohoOAuthService {

    private final ZohoConfig zohoConfig;
    private final WebClient.Builder webClientBuilder;

    private final AtomicReference<String> cachedAccessToken = new AtomicReference<>();
    private final AtomicReference<Instant> tokenExpiry = new AtomicReference<>(Instant.MIN);

    /**
     * Get a valid access token, refreshing if necessary
     */
    public String getAccessToken() {
        if (!isConfigured()) {
            throw new IllegalStateException("Zoho OAuth is not configured");
        }

        // Check if token is still valid (with 5 min buffer)
        if (cachedAccessToken.get() != null &&
            tokenExpiry.get().isAfter(Instant.now().plusSeconds(300))) {
            return cachedAccessToken.get();
        }

        // Refresh the token
        return refreshAccessToken();
    }

    /**
     * Check if Zoho OAuth is configured
     */
    public boolean isConfigured() {
        return zohoConfig.getOauth().getClientId() != null &&
               !zohoConfig.getOauth().getClientId().isBlank() &&
               zohoConfig.getOauth().getClientSecret() != null &&
               !zohoConfig.getOauth().getClientSecret().isBlank() &&
               zohoConfig.getOauth().getRefreshToken() != null &&
               !zohoConfig.getOauth().getRefreshToken().isBlank();
    }

    private synchronized String refreshAccessToken() {
        log.info("Refreshing Zoho access token");

        try {
            WebClient webClient = webClientBuilder.build();

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "refresh_token");
            formData.add("client_id", zohoConfig.getOauth().getClientId());
            formData.add("client_secret", zohoConfig.getOauth().getClientSecret());
            formData.add("refresh_token", zohoConfig.getOauth().getRefreshToken());

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                .uri(zohoConfig.getApi().getAccountsUrl() + "/oauth/v2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("access_token")) {
                String accessToken = (String) response.get("access_token");
                int expiresIn = response.containsKey("expires_in") ?
                    ((Number) response.get("expires_in")).intValue() : 3600;

                cachedAccessToken.set(accessToken);
                tokenExpiry.set(Instant.now().plusSeconds(expiresIn));

                log.info("Successfully refreshed Zoho access token, expires in {} seconds", expiresIn);
                return accessToken;
            } else {
                throw new RuntimeException("Failed to refresh Zoho access token: " + response);
            }
        } catch (Exception e) {
            log.error("Error refreshing Zoho access token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to refresh Zoho access token", e);
        }
    }

    /**
     * Clear cached token (useful for testing or when token is revoked)
     */
    public void clearCachedToken() {
        cachedAccessToken.set(null);
        tokenExpiry.set(Instant.MIN);
    }
}
