package com.healthdata.demo.orchestrator.integration;

import com.healthdata.demo.orchestrator.model.FhirValidationResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class DevOpsAgentClient {
    private final WebClient webClient;
    
    public DevOpsAgentClient(@Value("${hdim.services.devops-agent.url:http://devops-agent-service:8090}") String devopsAgentUrl) {
        this.webClient = WebClient.builder()
            .baseUrl(devopsAgentUrl)
            .build();
    }
    
    public FhirValidationResultDto validateFhirDemoData() {
        try {
            return webClient.post()
                .uri("/api/v1/devops/fhir-validation/validate")
                .retrieve()
                .bodyToMono(FhirValidationResultDto.class)
                .block();
        } catch (Exception e) {
            log.error("Failed to validate FHIR demo data: {}", e.getMessage());
            return null;
        }
    }
    
    public void publishLog(String level, String message, String category) {
        log.info("[{}] {}: {}", category, level, message);
        // TODO: Implement WebSocket publishing when WebSocket handler is ready
    }
    
    public void updateStatus(String component, String status, java.util.Map<String, Object> details) {
        log.info("Status update [{}]: {} - {}", component, status, details);
        // TODO: Implement WebSocket publishing when WebSocket handler is ready
    }
}
