package com.healthdata.devops.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class FhirServiceClient {
    private final WebClient webClient;
    
    public FhirServiceClient(@Value("${hdim.services.fhir.url:http://fhir-service:8085/fhir}") String fhirServiceUrl) {
        this.webClient = WebClient.builder()
            .baseUrl(fhirServiceUrl)
            .defaultHeader("Accept", "application/fhir+json")
            .build();
    }
    
    public Integer getResourceCount(String resourceType) {
        try {
            JsonNode response = webClient.get()
                .uri("/{resourceType}?_summary=count", resourceType)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
            
            if (response != null && response.has("total")) {
                return response.get("total").asInt();
            }
            return 0;
        } catch (Exception e) {
            log.error("Failed to get resource count for {}: {}", resourceType, e.getMessage());
            return 0;
        }
    }
    
    public Integer getResourceCountByCode(String resourceType, String code) {
        try {
            JsonNode response = webClient.get()
                .uri("/{resourceType}?code={code}&_summary=count", resourceType, code)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
            
            if (response != null && response.has("total")) {
                return response.get("total").asInt();
            }
            return 0;
        } catch (Exception e) {
            log.warn("Failed to get resource count by code for {} with code {}: {}", 
                resourceType, code, e.getMessage());
            return 0;
        }
    }
    
    public JsonNode getMetadata() {
        try {
            return webClient.get()
                .uri("/metadata")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        } catch (Exception e) {
            log.error("Failed to get FHIR metadata: {}", e.getMessage());
            return null;
        }
    }
    
    public JsonNode getResourceSamples(String resourceType, int count) {
        try {
            return webClient.get()
                .uri("/{resourceType}?_count={count}", resourceType, count)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        } catch (Exception e) {
            log.error("Failed to get resource samples for {}: {}", resourceType, e.getMessage());
            return null;
        }
    }
}
