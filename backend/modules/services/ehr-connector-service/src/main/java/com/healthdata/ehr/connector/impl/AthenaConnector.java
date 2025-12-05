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

/**
 * athenahealth connector implementation.
 * Supports both athenahealth's proprietary API and FHIR endpoints.
 */
@Slf4j
public class AthenaConnector extends FhirConnector {

    public AthenaConnector(EhrConnectionConfig config, WebClient.Builder webClientBuilder) {
        super(config, webClientBuilder);
    }

    @Override
    protected Mono<String> authenticate() {
        log.debug("Authenticating with athenahealth using OAuth2");

        String tokenUrl = config.getTokenUrl() != null ?
                config.getTokenUrl() :
                config.getBaseUrl() + "/oauth2/v1/token";

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("scope", config.getScope() != null ? config.getScope() : "system/*.read");

        return WebClient.create()
                .post()
                .uri(tokenUrl)
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + getBasicAuthHeader())
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .map(response -> {
                    accessToken = response.access_token;
                    int expiresIn = response.expires_in != null ? response.expires_in : 3600;
                    tokenExpiryTime = LocalDateTime.now().plusSeconds(expiresIn - 60);
                    log.debug("Athena authentication successful, token expires in {} seconds", expiresIn);
                    return accessToken;
                })
                .doOnError(error -> log.error("Athena authentication failed", error));
    }

    @Override
    protected Mono<String> fetchFhirResource(String resourcePath, String accessToken) {
        return webClient.get()
                .uri("/fhir/r4/" + resourcePath)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(error -> log.error("Failed to fetch FHIR resource: {}", resourcePath, error));
    }

    @Override
    protected Flux<String> fetchFhirBundle(String queryParams, String accessToken) {
        return webClient.get()
                .uri("/fhir/r4/" + queryParams)
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

    private String getBasicAuthHeader() {
        String credentials = config.getClientId() + ":" + config.getClientSecret();
        return java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    private record TokenResponse(
            String access_token,
            String token_type,
            Integer expires_in,
            String scope
    ) {}
}
