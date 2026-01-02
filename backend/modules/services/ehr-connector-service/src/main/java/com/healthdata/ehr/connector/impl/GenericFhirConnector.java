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
 * Generic FHIR R4 connector implementation.
 * Works with any FHIR-compliant server that supports standard OAuth2 client credentials flow.
 */
@Slf4j
public class GenericFhirConnector extends FhirConnector {

    public GenericFhirConnector(EhrConnectionConfig config, WebClient.Builder webClientBuilder) {
        super(config, webClientBuilder);
    }

    @Override
    protected Mono<String> authenticate() {
        log.debug("Authenticating with generic FHIR server using OAuth2");

        if (config.getTokenUrl() == null) {
            log.warn("No token URL provided for generic FHIR connector, using client secret as bearer token");
            accessToken = config.getClientSecret();
            tokenExpiryTime = LocalDateTime.now().plusYears(1);
            return Mono.just(accessToken);
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", config.getClientId());
        formData.add("client_secret", config.getClientSecret());

        if (config.getScope() != null) {
            formData.add("scope", config.getScope());
        }

        return WebClient.create()
                .post()
                .uri(config.getTokenUrl())
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .map(response -> {
                    accessToken = response.access_token;
                    int expiresIn = response.expires_in != null ? response.expires_in : 3600;
                    tokenExpiryTime = LocalDateTime.now().plusSeconds(expiresIn - 60);
                    log.debug("Generic FHIR authentication successful, token expires in {} seconds", expiresIn);
                    return accessToken;
                })
                .doOnError(error -> log.error("Generic FHIR authentication failed", error));
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

    private record TokenResponse(
            String access_token,
            String token_type,
            Integer expires_in,
            String scope
    ) {}
}
