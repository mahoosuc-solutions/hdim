package com.healthdata.fhir.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent.AgentType;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent.DecisionType;
import com.healthdata.audit.service.ai.AIAuditEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Audit integration for FHIR service
 * Publishes AI decision events for FHIR resource access and queries
 */
@Service
@Slf4j
public class FhirAuditIntegration {

    private final AIAuditEventPublisher auditEventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${audit.kafka.enabled:true}")
    private boolean auditEnabled;

    private static final String AGENT_ID = "fhir-data-access";
    private static final String AGENT_VERSION = "1.0.0";

    @Autowired
    public FhirAuditIntegration(
            AIAuditEventPublisher auditEventPublisher,
            ObjectMapper objectMapper) {
        this.auditEventPublisher = auditEventPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish FHIR resource query event
     */
    public void publishFhirQueryEvent(
            String tenantId,
            String resourceType,
            String resourceId,
            String queryType,
            Map<String, Object> queryParams,
            int resultCount,
            long queryTimeMs,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping FHIR query event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("resourceType", resourceType);
            inputMetrics.put("queryType", queryType);
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            if (resourceId != null) {
                inputMetrics.put("resourceId", resourceId);
            }
            inputMetrics.put("resultCount", resultCount);
            
            // Query parameters
            if (queryParams != null) {
                inputMetrics.put("queryParams", queryParams);
            }

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(UUID.randomUUID().toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.PHI_ACCESS)
                    .agentVersion(AGENT_VERSION)
                    .modelName("fhir-r4-api")
                    .decisionType(DecisionType.FHIR_QUERY)
                    .resourceType(resourceType)
                    .resourceId(resourceId != null ? resourceId : tenantId)
                    .inputMetrics(inputMetrics)
                    .inferenceTimeMs(queryTimeMs)
                    .reasoning(String.format("FHIR %s query: %s (%d results)",
                            resourceType, queryType, resultCount))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.APPROVED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.debug("Published FHIR query audit event: tenant={}, resource={}, type={}, count={}",
                    tenantId, resourceType, queryType, resultCount);

        } catch (Exception e) {
            log.error("Failed to publish FHIR query audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish FHIR resource creation/update event
     */
    public void publishFhirResourceChangeEvent(
            String tenantId,
            String resourceType,
            String resourceId,
            String operationType,
            Map<String, Object> resourceMetadata,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping FHIR resource change event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("resourceType", resourceType);
            inputMetrics.put("resourceId", resourceId);
            inputMetrics.put("operationType", operationType);
            inputMetrics.put("executingUser", executingUser);
            
            // Resource metadata
            if (resourceMetadata != null) {
                inputMetrics.putAll(resourceMetadata);
            }

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(UUID.randomUUID().toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.PHI_ACCESS)
                    .agentVersion(AGENT_VERSION)
                    .modelName("fhir-r4-api")
                    .decisionType(DecisionType.PHI_ACCESS)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .inputMetrics(inputMetrics)
                    .reasoning(String.format("FHIR %s %s: %s",
                            resourceType, operationType, resourceId))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.APPROVED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.debug("Published FHIR resource change audit event: tenant={}, resource={}, operation={}, id={}",
                    tenantId, resourceType, operationType, resourceId);

        } catch (Exception e) {
            log.error("Failed to publish FHIR resource change audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish batch FHIR query event
     */
    public void publishBatchFhirQueryEvent(
            String tenantId,
            String resourceType,
            int requestedCount,
            int returnedCount,
            long batchQueryTimeMs,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping batch FHIR query event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("resourceType", resourceType);
            inputMetrics.put("requestedCount", requestedCount);
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("returnedCount", returnedCount);
            inputMetrics.put("cacheHitRate", requestedCount > 0 ?
                    (double) (requestedCount - returnedCount) / requestedCount : 0.0);

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(UUID.randomUUID().toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.PHI_ACCESS)
                    .agentVersion(AGENT_VERSION)
                    .modelName("fhir-r4-batch-api")
                    .decisionType(DecisionType.FHIR_QUERY)
                    .resourceType(resourceType)
                    .resourceId(tenantId)
                    .inputMetrics(inputMetrics)
                    .inferenceTimeMs(batchQueryTimeMs)
                    .reasoning(String.format("Batch FHIR %s query: requested %d, returned %d",
                            resourceType, requestedCount, returnedCount))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.APPROVED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.debug("Published batch FHIR query audit event: tenant={}, resource={}, requested={}, returned={}",
                    tenantId, resourceType, requestedCount, returnedCount);

        } catch (Exception e) {
            log.error("Failed to publish batch FHIR query audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish bulk export event
     */
    public void publishBulkExportEvent(
            String tenantId,
            String exportJobId,
            String[] resourceTypes,
            String since,
            String exportFormat,
            long resourceCount,
            long exportTimeMs,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping bulk export event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("exportJobId", exportJobId);
            inputMetrics.put("resourceTypes", resourceTypes);
            inputMetrics.put("since", since);
            inputMetrics.put("exportFormat", exportFormat);
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("resourceCount", resourceCount);

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(exportJobId)
                    .agentId(AGENT_ID)
                    .agentType(AgentType.PHI_ACCESS)
                    .agentVersion(AGENT_VERSION)
                    .modelName("fhir-bulk-export")
                    .decisionType(DecisionType.PHI_ACCESS)
                    .resourceType("BulkExport")
                    .resourceId(exportJobId)
                    .inputMetrics(inputMetrics)
                    .inferenceTimeMs(exportTimeMs)
                    .reasoning(String.format("Bulk export: %d resources of types [%s]",
                            resourceCount, String.join(", ", resourceTypes)))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.APPROVED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published bulk export audit event: tenant={}, job={}, resources={}",
                    tenantId, exportJobId, resourceCount);

        } catch (Exception e) {
            log.error("Failed to publish bulk export audit event: {}", e.getMessage(), e);
        }
    }
}
