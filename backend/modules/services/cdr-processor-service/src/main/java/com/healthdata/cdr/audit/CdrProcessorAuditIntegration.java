package com.healthdata.cdr.audit;

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
 * Audit integration for CDR processor service
 * Publishes AI decision events for HL7/CDA data ingestion and transformation
 * Critical for HIPAA compliance when ingesting PHI from external systems
 */
@Service
@Slf4j
public class CdrProcessorAuditIntegration {

    private final AIAuditEventPublisher auditEventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${audit.kafka.enabled:true}")
    private boolean auditEnabled;

    private static final String AGENT_ID = "cdr-processor";
    private static final String AGENT_VERSION = "1.0.0";

    @Autowired
    public CdrProcessorAuditIntegration(
            AIAuditEventPublisher auditEventPublisher,
            ObjectMapper objectMapper) {
        this.auditEventPublisher = auditEventPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish CDR HL7 message ingest event
     */
    public void publishHl7MessageIngestEvent(
            String tenantId,
            String messageType,
            String messageControlId,
            String patientId,
            int segmentCount,
            boolean ingestSuccess,
            String errorMessage,
            long processingTimeMs,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping HL7 message ingest event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("messageType", messageType);
            inputMetrics.put("messageControlId", messageControlId);
            inputMetrics.put("patientId", patientId);
            inputMetrics.put("segmentCount", segmentCount);
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("ingestSuccess", ingestSuccess);
            if (!ingestSuccess && errorMessage != null) {
                inputMetrics.put("errorMessage", errorMessage);
            }

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(messageControlId)
                    .agentId(AGENT_ID)
                    .agentType(AgentType.PHI_ACCESS)
                    .agentVersion(AGENT_VERSION)
                    .modelName("hl7v2-parser")
                    .decisionType(DecisionType.CDR_INGEST)
                    .resourceType("Patient")
                    .resourceId(patientId != null ? patientId : tenantId)
                    .inputMetrics(inputMetrics)
                    .inferenceTimeMs(processingTimeMs)
                    .reasoning(String.format("Ingested HL7 %s message: %d segments, %s",
                            messageType, segmentCount, ingestSuccess ? "SUCCESS" : "FAILED"))
                    .outcome(ingestSuccess ? 
                            AIAgentDecisionEvent.DecisionOutcome.APPROVED : 
                            AIAgentDecisionEvent.DecisionOutcome.BLOCKED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published HL7 ingest audit event: tenant={}, type={}, control={}, success={}",
                    tenantId, messageType, messageControlId, ingestSuccess);

        } catch (Exception e) {
            log.error("Failed to publish HL7 ingest audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish CDR CDA document ingest event
     */
    public void publishCdaDocumentIngestEvent(
            String tenantId,
            String documentType,
            String documentId,
            String patientId,
            int resourceCount,
            boolean ingestSuccess,
            String errorMessage,
            long processingTimeMs,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping CDA document ingest event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("documentType", documentType);
            inputMetrics.put("documentId", documentId);
            inputMetrics.put("patientId", patientId);
            inputMetrics.put("resourceCount", resourceCount);
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("ingestSuccess", ingestSuccess);
            if (!ingestSuccess && errorMessage != null) {
                inputMetrics.put("errorMessage", errorMessage);
            }

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(documentId)
                    .agentId(AGENT_ID)
                    .agentType(AgentType.PHI_ACCESS)
                    .agentVersion(AGENT_VERSION)
                    .modelName("cda-c-cda-parser")
                    .decisionType(DecisionType.CDR_INGEST)
                    .resourceType("Patient")
                    .resourceId(patientId != null ? patientId : tenantId)
                    .inputMetrics(inputMetrics)
                    .inferenceTimeMs(processingTimeMs)
                    .reasoning(String.format("Ingested CDA %s document: %d resources, %s",
                            documentType, resourceCount, ingestSuccess ? "SUCCESS" : "FAILED"))
                    .outcome(ingestSuccess ? 
                            AIAgentDecisionEvent.DecisionOutcome.APPROVED : 
                            AIAgentDecisionEvent.DecisionOutcome.BLOCKED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published CDA ingest audit event: tenant={}, type={}, doc={}, success={}",
                    tenantId, documentType, documentId, ingestSuccess);

        } catch (Exception e) {
            log.error("Failed to publish CDA ingest audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish CDR data transformation event (HL7/CDA to FHIR)
     */
    public void publishDataTransformationEvent(
            String tenantId,
            String sourceFormat,
            String targetFormat,
            String sourceIdentifier,
            int transformedResourceCount,
            boolean transformSuccess,
            String errorMessage,
            long transformTimeMs,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping data transformation event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("sourceFormat", sourceFormat);
            inputMetrics.put("targetFormat", targetFormat);
            inputMetrics.put("sourceIdentifier", sourceIdentifier);
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("transformedResourceCount", transformedResourceCount);
            inputMetrics.put("transformSuccess", transformSuccess);
            if (!transformSuccess && errorMessage != null) {
                inputMetrics.put("errorMessage", errorMessage);
            }

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(sourceIdentifier)
                    .agentId(AGENT_ID)
                    .agentType(AgentType.PHI_ACCESS)
                    .agentVersion(AGENT_VERSION)
                    .modelName("interop-transformer")
                    .decisionType(DecisionType.CDR_TRANSFORM)
                    .resourceType("Bundle")
                    .resourceId(sourceIdentifier)
                    .inputMetrics(inputMetrics)
                    .inferenceTimeMs(transformTimeMs)
                    .reasoning(String.format("Transformed %d resources from %s to %s: %s",
                            transformedResourceCount, sourceFormat, targetFormat, 
                            transformSuccess ? "SUCCESS" : "FAILED"))
                    .outcome(transformSuccess ? 
                            AIAgentDecisionEvent.DecisionOutcome.APPROVED : 
                            AIAgentDecisionEvent.DecisionOutcome.BLOCKED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published transformation audit event: tenant={}, {} -> {}, resources={}, success={}",
                    tenantId, sourceFormat, targetFormat, transformedResourceCount, transformSuccess);

        } catch (Exception e) {
            log.error("Failed to publish transformation audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish CDR batch processing event
     */
    public void publishBatchProcessingEvent(
            String tenantId,
            String batchId,
            int totalMessages,
            int successfulMessages,
            int failedMessages,
            long batchProcessingTimeMs,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping batch processing event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("batchId", batchId);
            inputMetrics.put("totalMessages", totalMessages);
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("successfulMessages", successfulMessages);
            inputMetrics.put("failedMessages", failedMessages);
            inputMetrics.put("successRate", totalMessages > 0 ? 
                    (double) successfulMessages / totalMessages * 100.0 : 0.0);

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(batchId)
                    .agentId(AGENT_ID)
                    .agentType(AgentType.PHI_ACCESS)
                    .agentVersion(AGENT_VERSION)
                    .modelName("batch-processor")
                    .decisionType(DecisionType.BATCH_EVALUATION)
                    .resourceType("Batch")
                    .resourceId(batchId)
                    .inputMetrics(inputMetrics)
                    .inferenceTimeMs(batchProcessingTimeMs)
                    .reasoning(String.format("Processed batch: %d/%d messages successful (%.1f%%)",
                            successfulMessages, totalMessages, 
                            totalMessages > 0 ? (double) successfulMessages / totalMessages * 100.0 : 0.0))
                    .outcome(successfulMessages == totalMessages ? 
                            AIAgentDecisionEvent.DecisionOutcome.APPROVED : 
                            AIAgentDecisionEvent.DecisionOutcome.APPROVED) // Still approved even with partial failures
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published batch processing audit event: tenant={}, batch={}, success={}/{}, time={}ms",
                    tenantId, batchId, successfulMessages, totalMessages, batchProcessingTimeMs);

        } catch (Exception e) {
            log.error("Failed to publish batch processing audit event: {}", e.getMessage(), e);
        }
    }
}
