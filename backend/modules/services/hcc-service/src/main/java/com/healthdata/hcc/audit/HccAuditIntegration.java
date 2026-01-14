package com.healthdata.hcc.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent.AgentType;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent.DecisionType;
import com.healthdata.audit.service.ai.AIAuditEventPublisher;
import com.healthdata.hcc.persistence.DocumentationGapEntity;
import com.healthdata.hcc.service.RafCalculationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Audit integration for HCC service
 * Publishes AI decision events for HCC coding, RAF calculations, and documentation gaps
 */
@Service
@Slf4j
public class HccAuditIntegration {

    private final AIAuditEventPublisher auditEventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${audit.kafka.enabled:true}")
    private boolean auditEnabled;

    private static final String AGENT_ID = "hcc-coding";
    private static final String AGENT_VERSION = "1.0.0";

    @Autowired
    public HccAuditIntegration(
            AIAuditEventPublisher auditEventPublisher,
            ObjectMapper objectMapper) {
        this.auditEventPublisher = auditEventPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish RAF calculation event
     */
    public void publishRafCalculationEvent(
            String tenantId,
            UUID patientId,
            RafCalculationService.RafCalculationResult result,
            long inferenceTimeMs,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping RAF calculation event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("diagnosisCount", result.getDiagnosisCount());
            inputMetrics.put("profileYear", result.getProfileYear());
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("rafScoreV24", result.getRafScoreV24());
            inputMetrics.put("rafScoreV28", result.getRafScoreV28());
            inputMetrics.put("rafScoreBlended", result.getRafScoreBlended());
            inputMetrics.put("hccCountV24", result.getHccCountV24());
            inputMetrics.put("hccCountV28", result.getHccCountV28());
            inputMetrics.put("hccsV24", result.getHccsV24());
            inputMetrics.put("hccsV28", result.getHccsV28());
            inputMetrics.put("v24Weight", result.getV24Weight());
            inputMetrics.put("v28Weight", result.getV28Weight());

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(UUID.randomUUID().toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.PREDICTIVE_ANALYTICS)
                    .agentVersion(AGENT_VERSION)
                    .modelName("cms-hcc-v24-v28-blended")
                    .decisionType(DecisionType.RAF_CALCULATION)
                    .resourceType("Patient")
                    .resourceId(patientId.toString())
                    .inputMetrics(inputMetrics)
                    .inferenceTimeMs(inferenceTimeMs)
                    .reasoning(String.format("Calculated RAF score for %d diagnoses: V24=%.4f, V28=%.4f, Blended=%.4f (%.0f%% V24, %.0f%% V28)",
                            result.getDiagnosisCount(),
                            result.getRafScoreV24(),
                            result.getRafScoreV28(),
                            result.getRafScoreBlended(),
                            result.getV24Weight() * 100,
                            result.getV28Weight() * 100))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.APPROVED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published RAF calculation audit event: tenant={}, patient={}, score={}",
                    tenantId, patientId, result.getRafScoreBlended());

        } catch (Exception e) {
            log.error("Failed to publish RAF calculation audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish HCC coding event for individual code assignments
     */
    public void publishHccCodingEvent(
            String tenantId,
            UUID patientId,
            String icd10Code,
            String hccCodeV24,
            String hccCodeV28,
            Map<String, Object> codeMetadata,
            long processingTimeMs,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping HCC coding event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("icd10Code", icd10Code);
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("hccCodeV24", hccCodeV24);
            inputMetrics.put("hccCodeV28", hccCodeV28);
            
            // Additional metadata
            if (codeMetadata != null) {
                inputMetrics.putAll(codeMetadata);
            }

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(UUID.randomUUID().toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.CQL_ENGINE)
                    .agentVersion(AGENT_VERSION)
                    .modelName("cms-hcc-crosswalk")
                    .decisionType(DecisionType.HCC_CODING)
                    .resourceType("DiagnosisCode")
                    .resourceId(icd10Code)
                    .inputMetrics(inputMetrics)
                    .inferenceTimeMs(processingTimeMs)
                    .reasoning(String.format("Mapped ICD-10 code %s to HCC: V24=%s, V28=%s",
                            icd10Code, 
                            hccCodeV24 != null ? hccCodeV24 : "none",
                            hccCodeV28 != null ? hccCodeV28 : "none"))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.APPROVED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published HCC coding audit event: tenant={}, patient={}, code={}",
                    tenantId, patientId, icd10Code);

        } catch (Exception e) {
            log.error("Failed to publish HCC coding audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish documentation gap identification event
     */
    public void publishDocumentationGapEvent(
            String tenantId,
            UUID patientId,
            List<DocumentationGapEntity> identifiedGaps,
            int totalGaps,
            BigDecimal potentialRafUplift,
            long analysisTimeMs,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping documentation gap event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("patientId", patientId.toString());
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("totalGaps", totalGaps);
            inputMetrics.put("potentialRafUplift", potentialRafUplift);
            
            // Gap breakdown by type
            Map<String, Long> gapsByType = identifiedGaps.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            g -> g.getGapType() != null ? g.getGapType().name() : "UNKNOWN",
                            java.util.stream.Collectors.counting()));
            inputMetrics.put("gapsByType", gapsByType);
            
            // Gap breakdown by priority
            Map<String, Long> gapsByPriority = identifiedGaps.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            g -> g.getPriority() != null ? g.getPriority() : "UNKNOWN",
                            java.util.stream.Collectors.counting()));
            inputMetrics.put("gapsByPriority", gapsByPriority);
            
            // High priority gap count
            long highPriorityGaps = identifiedGaps.stream()
                    .filter(g -> "HIGH".equals(g.getPriority()))
                    .count();
            inputMetrics.put("highPriorityGapCount", highPriorityGaps);

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(UUID.randomUUID().toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.CARE_GAP_IDENTIFIER)
                    .agentVersion(AGENT_VERSION)
                    .modelName("hcc-documentation-analyzer")
                    .decisionType(DecisionType.CARE_GAP_IDENTIFICATION)
                    .resourceType("Patient")
                    .resourceId(patientId.toString())
                    .inputMetrics(inputMetrics)
                    .inferenceTimeMs(analysisTimeMs)
                    .reasoning(String.format("Identified %d documentation gaps with potential RAF uplift of %.4f (%d high priority)",
                            totalGaps, potentialRafUplift, highPriorityGaps))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.APPROVED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published documentation gap audit event: tenant={}, patient={}, gaps={}",
                    tenantId, patientId, totalGaps);

        } catch (Exception e) {
            log.error("Failed to publish documentation gap audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish gap address event (when a documentation gap is resolved)
     */
    public void publishGapAddressedEvent(
            String tenantId,
            UUID patientId,
            DocumentationGapEntity gap,
            String addressedBy,
            String newIcd10Code) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping gap addressed event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("gapId", gap.getId().toString());
            inputMetrics.put("gapType", gap.getGapType() != null ? gap.getGapType().name() : null);
            inputMetrics.put("originalIcd10", gap.getCurrentIcd10());
            inputMetrics.put("addressedBy", addressedBy);
            
            // Output/result data
            inputMetrics.put("newIcd10Code", newIcd10Code);
            inputMetrics.put("rafImpactV24", gap.getRafImpactV24());
            inputMetrics.put("rafImpactV28", gap.getRafImpactV28());
            inputMetrics.put("rafImpactBlended", gap.getRafImpactBlended());
            inputMetrics.put("priority", gap.getPriority());

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(UUID.randomUUID().toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.CARE_GAP_IDENTIFIER)
                    .agentVersion(AGENT_VERSION)
                    .modelName("hcc-gap-closure")
                    .decisionType(DecisionType.CARE_GAP_CLOSURE)
                    .resourceType("DocumentationGap")
                    .resourceId(gap.getId().toString())
                    .inputMetrics(inputMetrics)
                    .reasoning(String.format("Documentation gap addressed: %s → %s (RAF impact: %.4f)",
                            gap.getCurrentIcd10(),
                            newIcd10Code,
                            gap.getRafImpactBlended()))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.APPROVED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published gap addressed audit event: tenant={}, patient={}, gap={}",
                    tenantId, patientId, gap.getId());

        } catch (Exception e) {
            log.error("Failed to publish gap addressed audit event: {}", e.getMessage(), e);
        }
    }
}
