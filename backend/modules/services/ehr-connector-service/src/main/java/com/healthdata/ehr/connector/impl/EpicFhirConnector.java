package com.healthdata.ehr.connector.impl;

import com.healthdata.ehr.connector.FhirConnector;
import com.healthdata.ehr.dto.EhrConnectionConfig;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Epic FHIR R4 connector implementation.
 * Supports Epic's Backend Services OAuth2 authentication flow (JWT assertion).
 */
@Slf4j
public class EpicFhirConnector extends FhirConnector {

    public EpicFhirConnector(EhrConnectionConfig config, WebClient.Builder webClientBuilder) {
        super(config, webClientBuilder);
    }

    @Override
    protected Mono<String> authenticate() {
        log.debug("Authenticating with Epic using OAuth2 client credentials");

        String tokenUrl = config.getTokenUrl() != null ?
                config.getTokenUrl() :
                config.getBaseUrl() + "/oauth2/token";

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", config.getClientId());
        formData.add("client_secret", config.getClientSecret());

        if (config.getScope() != null) {
            formData.add("scope", config.getScope());
        }

        return WebClient.create()
                .post()
                .uri(tokenUrl)
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .map(response -> {
                    accessToken = response.access_token;
                    // Set expiry time (default 1 hour if not provided)
                    int expiresIn = response.expires_in != null ? response.expires_in : 3600;
                    tokenExpiryTime = LocalDateTime.now().plusSeconds(expiresIn - 60); // 60s buffer
                    log.debug("Epic authentication successful, token expires in {} seconds", expiresIn);
                    return accessToken;
                })
                .doOnError(error -> log.error("Epic authentication failed", error));
    }

    @Override
    protected Mono<String> fetchFhirResource(String resourcePath, String accessToken) {
        return webClient.get()
                .uri(resourcePath)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(error -> log.error("Failed to fetch FHIR resource: {}", resourcePath, error));
    }

    @Override
    protected Flux<String> fetchFhirBundle(String queryParams, String accessToken) {
        return webClient.get()
                .uri(queryParams)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(String.class)
                .flatMapMany(bundleJson -> {
                    Bundle bundle = fhirParser.parseResource(Bundle.class, bundleJson);
                    return Flux.fromIterable(bundle.getEntry())
                            .map(entry -> fhirParser.encodeResourceToString(entry.getResource()));
                })
                .doOnError(error -> log.error("Failed to fetch FHIR bundle: {}", queryParams, error));
    }

    /**
     * OAuth2 token response model.
     */
    private record TokenResponse(
            String access_token,
            String token_type,
            Integer expires_in,
            String scope
    ) {}
}
