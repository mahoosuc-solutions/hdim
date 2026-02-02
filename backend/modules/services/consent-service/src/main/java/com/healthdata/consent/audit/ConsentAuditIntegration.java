package com.healthdata.consent.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent.AgentType;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent.DecisionType;
import com.healthdata.audit.service.ai.AIAuditEventPublisher;
import com.healthdata.consent.persistence.ConsentEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Audit integration for consent service
 * Publishes AI decision events for consent grants, revocations, and updates
 * Critical for HIPAA 42 CFR Part 2 compliance
 */
@Service
@Slf4j
public class ConsentAuditIntegration {

    private final AIAuditEventPublisher auditEventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${audit.kafka.enabled:true}")
    private boolean auditEnabled;

    private static final String AGENT_ID = "consent-manager";
    private static final String AGENT_VERSION = "1.0.0";

    @Autowired
    public ConsentAuditIntegration(
            @Autowired(required = false) AIAuditEventPublisher auditEventPublisher,
            ObjectMapper objectMapper) {
        this.auditEventPublisher = auditEventPublisher;
        this.objectMapper = objectMapper;
        if (auditEventPublisher == null) {
            log.warn("AIAuditEventPublisher not available - consent audit events will not be published to Kafka");
        }
    }

    /**
     * Publish consent grant event
     */
    public void publishConsentGrantEvent(
            String tenantId,
            ConsentEntity consent,
            String executingUser) {

        if (!auditEnabled || auditEventPublisher == null) {
            log.debug("Audit disabled or publisher not available, skipping consent grant event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("consentId", consent.getId().toString());
            inputMetrics.put("patientId", consent.getPatientId().toString());
            inputMetrics.put("scope", consent.getScope());
            inputMetrics.put("category", consent.getCategory());
            inputMetrics.put("purpose", consent.getPurpose());
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("status", consent.getStatus());
            inputMetrics.put("dataClass", consent.getDataClass());
            inputMetrics.put("policyRule", consent.getPolicyRule());
            inputMetrics.put("authorizedPartyType", consent.getAuthorizedPartyType());
            inputMetrics.put("authorizedPartyId", consent.getAuthorizedPartyId());
            inputMetrics.put("validFrom", consent.getValidFrom());
            inputMetrics.put("validTo", consent.getValidTo());

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(consent.getId().toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.CONSENT_VALIDATOR)
                    .agentVersion(AGENT_VERSION)
                    .modelName("hipaa-42cfr-part2-consent")
                    .decisionType(DecisionType.CONSENT_GRANT)
                    .resourceType("Patient")
                    .resourceId(consent.getPatientId().toString())
                    .inputMetrics(inputMetrics)
                    .reasoning(String.format("Consent granted: %s for %s (scope: %s, data class: %s)",
                            consent.getCategory(), consent.getPurpose(), consent.getScope(), consent.getDataClass()))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.APPROVED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published consent grant audit event: tenant={}, patient={}, consent={}",
                    tenantId, consent.getPatientId(), consent.getId());

        } catch (Exception e) {
            log.error("Failed to publish consent grant audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish consent revocation event
     */
    public void publishConsentRevokeEvent(
            String tenantId,
            ConsentEntity consent,
            String revocationReason,
            String executingUser) {

        if (!auditEnabled || auditEventPublisher == null) {
            log.debug("Audit disabled or publisher not available, skipping consent revoke event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("consentId", consent.getId().toString());
            inputMetrics.put("patientId", consent.getPatientId().toString());
            inputMetrics.put("originalScope", consent.getScope());
            inputMetrics.put("originalCategory", consent.getCategory());
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("revocationReason", revocationReason);
            inputMetrics.put("revocationDate", consent.getRevocationDate());
            inputMetrics.put("revokedBy", consent.getRevokedBy());
            inputMetrics.put("newStatus", "revoked");

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(consent.getId().toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.CONSENT_VALIDATOR)
                    .agentVersion(AGENT_VERSION)
                    .modelName("hipaa-42cfr-part2-consent")
                    .decisionType(DecisionType.CONSENT_REVOKE)
                    .resourceType("Patient")
                    .resourceId(consent.getPatientId().toString())
                    .inputMetrics(inputMetrics)
                    .reasoning(String.format("Consent revoked: %s for patient %s (reason: %s)",
                            consent.getCategory(), consent.getPatientId(), revocationReason))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.APPROVED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published consent revoke audit event: tenant={}, patient={}, consent={}, reason={}",
                    tenantId, consent.getPatientId(), consent.getId(), revocationReason);

        } catch (Exception e) {
            log.error("Failed to publish consent revoke audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish consent update event
     */
    public void publishConsentUpdateEvent(
            String tenantId,
            ConsentEntity originalConsent,
            ConsentEntity updatedConsent,
            String executingUser) {

        if (!auditEnabled || auditEventPublisher == null) {
            log.debug("Audit disabled or publisher not available, skipping consent update event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("consentId", updatedConsent.getId().toString());
            inputMetrics.put("patientId", updatedConsent.getPatientId().toString());
            inputMetrics.put("executingUser", executingUser);
            
            // Track changes
            Map<String, Object> changes = new HashMap<>();
            if (!originalConsent.getScope().equals(updatedConsent.getScope())) {
                changes.put("scope", Map.of("from", originalConsent.getScope(), "to", updatedConsent.getScope()));
            }
            if (!originalConsent.getStatus().equals(updatedConsent.getStatus())) {
                changes.put("status", Map.of("from", originalConsent.getStatus(), "to", updatedConsent.getStatus()));
            }
            if (!originalConsent.getCategory().equals(updatedConsent.getCategory())) {
                changes.put("category", Map.of("from", originalConsent.getCategory(), "to", updatedConsent.getCategory()));
            }
            inputMetrics.put("changes", changes);
            inputMetrics.put("changeCount", changes.size());

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(updatedConsent.getId().toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.CONSENT_VALIDATOR)
                    .agentVersion(AGENT_VERSION)
                    .modelName("hipaa-42cfr-part2-consent")
                    .decisionType(DecisionType.CONSENT_UPDATE)
                    .resourceType("Patient")
                    .resourceId(updatedConsent.getPatientId().toString())
                    .inputMetrics(inputMetrics)
                    .reasoning(String.format("Consent updated: %d field(s) changed for patient %s",
                            changes.size(), updatedConsent.getPatientId()))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.APPROVED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published consent update audit event: tenant={}, patient={}, consent={}, changes={}",
                    tenantId, updatedConsent.getPatientId(), updatedConsent.getId(), changes.size());

        } catch (Exception e) {
            log.error("Failed to publish consent update audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish consent verification event
     */
    public void publishConsentVerificationEvent(
            String tenantId,
            UUID patientId,
            String requestedAccess,
            boolean consentVerified,
            String consentScope,
            String denialReason,
            String executingUser) {

        if (!auditEnabled || auditEventPublisher == null) {
            log.debug("Audit disabled or publisher not available, skipping consent verification event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("patientId", patientId.toString());
            inputMetrics.put("requestedAccess", requestedAccess);
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("consentVerified", consentVerified);
            inputMetrics.put("consentScope", consentScope);
            if (!consentVerified && denialReason != null) {
                inputMetrics.put("denialReason", denialReason);
            }

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(UUID.randomUUID().toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.CONSENT_VALIDATOR)
                    .agentVersion(AGENT_VERSION)
                    .modelName("consent-verification-engine")
                    .decisionType(DecisionType.PHI_ACCESS)
                    .resourceType("Patient")
                    .resourceId(patientId.toString())
                    .inputMetrics(inputMetrics)
                    .reasoning(String.format("Consent verification for %s: %s (scope: %s)",
                            requestedAccess, consentVerified ? "APPROVED" : "DENIED", consentScope))
                    .outcome(consentVerified ? 
                            AIAgentDecisionEvent.DecisionOutcome.APPROVED : 
                            AIAgentDecisionEvent.DecisionOutcome.BLOCKED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published consent verification audit event: tenant={}, patient={}, verified={}, access={}",
                    tenantId, patientId, consentVerified, requestedAccess);

        } catch (Exception e) {
            log.error("Failed to publish consent verification audit event: {}", e.getMessage(), e);
        }
    }
}
