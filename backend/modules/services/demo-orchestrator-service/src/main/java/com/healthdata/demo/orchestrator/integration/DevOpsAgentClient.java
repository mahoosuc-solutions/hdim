package com.healthdata.demo.orchestrator.integration;

import com.healthdata.demo.orchestrator.model.DevOpsLogMessage;
import com.healthdata.demo.orchestrator.model.DevOpsStatusUpdate;
import com.healthdata.demo.orchestrator.model.FhirValidationResultDto;
import com.healthdata.demo.orchestrator.websocket.DevOpsLogWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * DevOps agent client for FHIR validation and real-time log publishing.
 *
 * Publishes deployment logs, data seeding progress, clearing operations,
 * and FHIR validation results to connected WebSocket clients.
 *
 * ★ Insight ─────────────────────────────────────
 * - WebSocket integration: Real-time log streaming for DevOps dashboards
 * - Tenant isolation: Logs automatically tagged with demo tenant ID
 * - Fire-and-forget: WebSocket publishing doesn't block operations
 * - Fallback logging: Always logs to SLF4J even if no WS clients connected
 * ─────────────────────────────────────────────────
 */
@Slf4j
@Component
public class DevOpsAgentClient {
    private final WebClient webClient;
    private final DevOpsLogWebSocketHandler webSocketHandler;
    private final String demoTenantId;

    public DevOpsAgentClient(
            @Value("${hdim.services.devops-agent.url:http://devops-agent-service:8090}") String devopsAgentUrl,
            @Value("${hdim.demo.tenant-id:demo-tenant}") String demoTenantId,
            DevOpsLogWebSocketHandler webSocketHandler) {
        this.webClient = WebClient.builder()
            .baseUrl(devopsAgentUrl)
            .build();
        this.demoTenantId = demoTenantId;
        this.webSocketHandler = webSocketHandler;
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

    /**
     * Publish log message to WebSocket clients and standard logs.
     *
     * @param level Log level: DEBUG, INFO, WARN, ERROR
     * @param message Log message content
     * @param category Log category: SEED, CLEAR, VALIDATION, DEPLOY
     */
    public void publishLog(String level, String message, String category) {
        // Always log to SLF4J
        log.info("[{}] {}: {}", category, level, message);

        // Publish to WebSocket clients
        try {
            DevOpsLogMessage logMessage = DevOpsLogMessage.builder()
                .level(level)
                .message(message)
                .category(category)
                .tenantId(demoTenantId)
                .component("demo-orchestrator")
                .build();

            webSocketHandler.publishLog(logMessage);
        } catch (Exception e) {
            log.warn("Failed to publish log to WebSocket: {}", e.getMessage());
        }
    }

    /**
     * Update component status and publish to WebSocket clients.
     *
     * @param component Component name: FHIR_VALIDATION, DATA_SEEDING, etc.
     * @param status Status: PENDING, IN_PROGRESS, COMPLETED, FAILED
     * @param details Status details (counts, metrics, error messages)
     */
    public void updateStatus(String component, String status, java.util.Map<String, Object> details) {
        log.info("Status update [{}]: {} - {}", component, status, details);

        // Publish to WebSocket clients
        try {
            DevOpsStatusUpdate statusUpdate = DevOpsStatusUpdate.builder()
                .component(component)
                .status(status)
                .tenantId(demoTenantId)
                .details(details)
                .build();

            webSocketHandler.publishStatus(statusUpdate);
        } catch (Exception e) {
            log.warn("Failed to publish status to WebSocket: {}", e.getMessage());
        }
    }
}
