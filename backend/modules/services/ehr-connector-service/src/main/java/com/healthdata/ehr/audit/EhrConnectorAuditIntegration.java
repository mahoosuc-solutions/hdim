package com.healthdata.ehr.audit;

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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Audit integration for EHR connector service
 * Publishes AI decision events for PHI data fetch from external EHR systems
 * Critical for HIPAA compliance when accessing PHI from Epic, Cerner, etc.
 */
@Service
@Slf4j
public class EhrConnectorAuditIntegration {

    private final AIAuditEventPublisher auditEventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${audit.kafka.enabled:true}")
    private boolean auditEnabled;

    private static final String AGENT_ID = "ehr-connector";
    private static final String AGENT_VERSION = "1.0.0";

    @Autowired
    public EhrConnectorAuditIntegration(
            AIAuditEventPublisher auditEventPublisher,
            ObjectMapper objectMapper) {
        this.auditEventPublisher = auditEventPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish EHR patient fetch event
     */
    public void publishEhrPatientFetchEvent(
            String tenantId,
            String connectionId,
            String ehrVendor,
            String ehrPatientId,
            boolean fetchSuccess,
            String errorMessage,
            long fetchTimeMs,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping EHR patient fetch event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("connectionId", connectionId);
            inputMetrics.put("ehrVendor", ehrVendor);
            inputMetrics.put("ehrPatientId", ehrPatientId);
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("fetchSuccess", fetchSuccess);
            if (!fetchSuccess && errorMessage != null) {
                inputMetrics.put("errorMessage", errorMessage);
            }

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(UUID.randomUUID().toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.PHI_ACCESS)
                    .agentVersion(AGENT_VERSION)
                    .modelName("ehr-fhir-" + ehrVendor.toLowerCase())
                    .decisionType(DecisionType.EHR_DATA_FETCH)
                    .resourceType("Patient")
                    .resourceId(ehrPatientId)
                    .inputMetrics(inputMetrics)
                    .inferenceTimeMs(fetchTimeMs)
                    .reasoning(String.format("Fetched patient from %s EHR: %s",
                            ehrVendor, fetchSuccess ? "SUCCESS" : "FAILED"))
                    .outcome(fetchSuccess ? 
                            AIAgentDecisionEvent.DecisionOutcome.APPROVED : 
                            AIAgentDecisionEvent.DecisionOutcome.BLOCKED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published EHR patient fetch audit event: tenant={}, vendor={}, patient={}, success={}",
                    tenantId, ehrVendor, ehrPatientId, fetchSuccess);

        } catch (Exception e) {
            log.error("Failed to publish EHR patient fetch audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish EHR data sync event
     */
    public void publishEhrDataSyncEvent(
            String tenantId,
            String connectionId,
            String ehrVendor,
            String ehrPatientId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int encountersRetrieved,
            int observationsRetrieved,
            boolean syncSuccess,
            String errorMessage,
            long syncTimeMs,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping EHR data sync event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("connectionId", connectionId);
            inputMetrics.put("ehrVendor", ehrVendor);
            inputMetrics.put("ehrPatientId", ehrPatientId);
            inputMetrics.put("startDate", startDate);
            inputMetrics.put("endDate", endDate);
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("encountersRetrieved", encountersRetrieved);
            inputMetrics.put("observationsRetrieved", observationsRetrieved);
            inputMetrics.put("totalResources", encountersRetrieved + observationsRetrieved);
            inputMetrics.put("syncSuccess", syncSuccess);
            if (!syncSuccess && errorMessage != null) {
                inputMetrics.put("errorMessage", errorMessage);
            }

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(UUID.randomUUID().toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.PHI_ACCESS)
                    .agentVersion(AGENT_VERSION)
                    .modelName("ehr-sync-" + ehrVendor.toLowerCase())
                    .decisionType(DecisionType.EHR_DATA_PUSH)
                    .resourceType("Patient")
                    .resourceId(ehrPatientId)
                    .inputMetrics(inputMetrics)
                    .inferenceTimeMs(syncTimeMs)
                    .reasoning(String.format("Synced %d resources from %s EHR (%d encounters, %d observations)",
                            encountersRetrieved + observationsRetrieved, ehrVendor, 
                            encountersRetrieved, observationsRetrieved))
                    .outcome(syncSuccess ? 
                            AIAgentDecisionEvent.DecisionOutcome.APPROVED : 
                            AIAgentDecisionEvent.DecisionOutcome.BLOCKED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published EHR data sync audit event: tenant={}, vendor={}, patient={}, resources={}",
                    tenantId, ehrVendor, ehrPatientId, encountersRetrieved + observationsRetrieved);

        } catch (Exception e) {
            log.error("Failed to publish EHR data sync audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish EHR patient search event
     */
    public void publishEhrPatientSearchEvent(
            String tenantId,
            String connectionId,
            String ehrVendor,
            String familyName,
            String givenName,
            String dateOfBirth,
            int resultsCount,
            boolean searchSuccess,
            long searchTimeMs,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping EHR patient search event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("connectionId", connectionId);
            inputMetrics.put("ehrVendor", ehrVendor);
            inputMetrics.put("familyName", familyName);
            inputMetrics.put("givenName", givenName);
            inputMetrics.put("dateOfBirth", dateOfBirth);
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("resultsCount", resultsCount);
            inputMetrics.put("searchSuccess", searchSuccess);

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(UUID.randomUUID().toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.PHI_ACCESS)
                    .agentVersion(AGENT_VERSION)
                    .modelName("ehr-search-" + ehrVendor.toLowerCase())
                    .decisionType(DecisionType.EHR_DATA_FETCH)
                    .resourceType("Patient")
                    .resourceId(tenantId)
                    .inputMetrics(inputMetrics)
                    .inferenceTimeMs(searchTimeMs)
                    .reasoning(String.format("Searched %s EHR: %s, %s (%d results)",
                            ehrVendor, familyName, givenName, resultsCount))
                    .outcome(searchSuccess ? 
                            AIAgentDecisionEvent.DecisionOutcome.APPROVED : 
                            AIAgentDecisionEvent.DecisionOutcome.BLOCKED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published EHR patient search audit event: tenant={}, vendor={}, name={} {}, results={}",
                    tenantId, ehrVendor, givenName, familyName, resultsCount);

        } catch (Exception e) {
            log.error("Failed to publish EHR patient search audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish EHR connection test event
     */
    public void publishEhrConnectionTestEvent(
            String tenantId,
            String connectionId,
            String ehrVendor,
            boolean connectionSuccessful,
            String errorMessage,
            long testTimeMs,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping EHR connection test event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("connectionId", connectionId);
            inputMetrics.put("ehrVendor", ehrVendor);
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("connectionSuccessful", connectionSuccessful);
            if (!connectionSuccessful && errorMessage != null) {
                inputMetrics.put("errorMessage", errorMessage);
            }

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(connectionId)
                    .agentId(AGENT_ID)
                    .agentType(AgentType.CONFIGURATION_ADVISOR)
                    .agentVersion(AGENT_VERSION)
                    .modelName("ehr-connection-" + ehrVendor.toLowerCase())
                    .decisionType(DecisionType.AI_RECOMMENDATION)
                    .resourceType("EhrConnection")
                    .resourceId(connectionId)
                    .inputMetrics(inputMetrics)
                    .inferenceTimeMs(testTimeMs)
                    .reasoning(String.format("Tested %s EHR connection: %s",
                            ehrVendor, connectionSuccessful ? "SUCCESS" : "FAILED"))
                    .outcome(connectionSuccessful ? 
                            AIAgentDecisionEvent.DecisionOutcome.APPROVED : 
                            AIAgentDecisionEvent.DecisionOutcome.BLOCKED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published EHR connection test audit event: tenant={}, vendor={}, connection={}, success={}",
                    tenantId, ehrVendor, connectionId, connectionSuccessful);

        } catch (Exception e) {
            log.error("Failed to publish EHR connection test audit event: {}", e.getMessage(), e);
        }
    }
}
