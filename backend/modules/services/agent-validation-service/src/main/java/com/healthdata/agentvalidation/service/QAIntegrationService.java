package com.healthdata.agentvalidation.service;

import com.healthdata.agentvalidation.client.ApprovalServiceClient;
import com.healthdata.agentvalidation.client.dto.ApprovalRequest;
import com.healthdata.agentvalidation.client.dto.ApprovalResponse;
import com.healthdata.agentvalidation.config.ValidationProperties;
import com.healthdata.agentvalidation.domain.entity.TestExecution;
import com.healthdata.agentvalidation.domain.enums.HarmLevel;
import com.healthdata.agentvalidation.domain.enums.TestStatus;
import com.healthdata.agentvalidation.repository.TestExecutionRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for integrating with QA review workflow.
 * Flags executions for human review and processes QA decisions.
 */
@Slf4j
@Service
public class QAIntegrationService {

    private static final String QA_TOPIC = "agent-validation-qa-requests";
    private static final String QA_DECISION_TOPIC = "agent-validation-qa-decisions";

    private final ApprovalServiceClient approvalServiceClient;
    private final TestExecutionRepository testExecutionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ValidationProperties validationProperties;
    private final MeterRegistry meterRegistry;

    private final Counter qaFlaggedCounter;
    private final Counter qaApprovedCounter;
    private final Counter qaRejectedCounter;

    public QAIntegrationService(
            ApprovalServiceClient approvalServiceClient,
            TestExecutionRepository testExecutionRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            ValidationProperties validationProperties,
            MeterRegistry meterRegistry) {
        this.approvalServiceClient = approvalServiceClient;
        this.testExecutionRepository = testExecutionRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.validationProperties = validationProperties;
        this.meterRegistry = meterRegistry;

        this.qaFlaggedCounter = Counter.builder("agent.validation.qa.flagged")
            .description("Count of executions flagged for QA review")
            .register(meterRegistry);
        this.qaApprovedCounter = Counter.builder("agent.validation.qa.approved")
            .description("Count of QA-approved executions")
            .register(meterRegistry);
        this.qaRejectedCounter = Counter.builder("agent.validation.qa.rejected")
            .description("Count of QA-rejected executions")
            .register(meterRegistry);
    }

    /**
     * Check if an execution should be flagged for QA review.
     */
    public boolean shouldFlagForReview(TestExecution execution) {
        ValidationProperties.QaConfig qaConfig = validationProperties.getQa();

        // Check overall score threshold
        if (execution.getEvaluationScore() != null &&
            execution.getEvaluationScore().compareTo(qaConfig.getAutoFlagScoreThreshold()) < 0) {
            log.info("Flagging execution {} for low score: {}",
                execution.getId(), execution.getEvaluationScore());
            return true;
        }

        // Check individual metric thresholds
        if (execution.getMetricResults() != null) {
            for (TestExecution.MetricResult result : execution.getMetricResults().values()) {
                if (result.getScore() != null &&
                    result.getScore().compareTo(qaConfig.getAutoFlagMetricThreshold()) < 0) {
                    log.info("Flagging execution {} for low metric {}: {}",
                        execution.getId(), result.getMetricType(), result.getScore());
                    return true;
                }
            }
        }

        // Check reflection results
        if (execution.getReflectionResult() != null) {
            TestExecution.ReflectionResult reflection = execution.getReflectionResult();

            // Check confidence miscalibration
            if (reflection.isMiscalibrated() &&
                reflection.getCalibrationDelta() != null &&
                reflection.getCalibrationDelta().abs().compareTo(
                    qaConfig.getConfidenceMiscalibrationThreshold()) > 0) {
                log.info("Flagging execution {} for confidence miscalibration: {}",
                    execution.getId(), reflection.getCalibrationDelta());
                return true;
            }

            // Check harm level
            if (reflection.getPotentialHarmLevel() != null &&
                qaConfig.getFlagHarmLevels().contains(reflection.getPotentialHarmLevel().name())) {
                log.info("Flagging execution {} for potential harm level: {}",
                    execution.getId(), reflection.getPotentialHarmLevel());
                return true;
            }
        }

        // Check regression results
        if (execution.getRegressionResult() != null &&
            execution.getRegressionResult().isRegressionDetected()) {
            log.info("Flagging execution {} for regression detected",
                execution.getId());
            return true;
        }

        return false;
    }

    /**
     * Create an approval request for a flagged execution.
     */
    @Transactional
    public void createApprovalRequest(TestExecution execution, String tenantId, String userId) {
        log.info("Creating approval request for execution {}", execution.getId());

        String flagReason = determineFlagReason(execution);
        String riskLevel = determineRiskLevel(execution);

        // Build context data
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("testCaseId", execution.getTestCase().getId().toString());
        contextData.put("testCaseName", execution.getTestCase().getName());
        contextData.put("evaluationScore", execution.getEvaluationScore());
        contextData.put("llmProvider", execution.getLlmProvider());
        if (execution.getReflectionResult() != null) {
            contextData.put("harmLevel", execution.getReflectionResult().getPotentialHarmLevel());
            contextData.put("confidenceLevel", execution.getReflectionResult().getConfidenceLevel());
        }

        ApprovalRequest request = ApprovalRequest.builder()
            .category("AGENT_VALIDATION")
            .itemType("TEST_EXECUTION")
            .itemId(execution.getId().toString())
            .title("Agent Response Review: " + execution.getTestCase().getName())
            .description(String.format(
                "Test execution flagged for review.\n\n" +
                "Test Case: %s\n" +
                "Score: %s\n" +
                "Flag Reason: %s",
                execution.getTestCase().getName(),
                execution.getEvaluationScore(),
                flagReason))
            .riskLevel(riskLevel)
            .flagReason(flagReason)
            .contextData(contextData)
            .requiredRole("QUALITY_OFFICER")
            .priority(riskLevel.equals("HIGH") ? 1 : riskLevel.equals("MEDIUM") ? 2 : 3)
            .build();

        try {
            ApprovalResponse response = approvalServiceClient.createApprovalRequest(
                tenantId, userId, request);
            log.info("Created approval request {} for execution {}",
                response.getId(), execution.getId());

            // Update execution status
            execution.setStatus(TestStatus.FLAGGED_FOR_REVIEW);
            execution.setQaReviewStatus("PENDING");
            testExecutionRepository.save(execution);

            qaFlaggedCounter.increment();

            // Publish to Kafka for real-time notifications
            kafkaTemplate.send(QA_TOPIC, execution.getId().toString(),
                new QAReviewEvent(execution.getId().toString(), flagReason, riskLevel, Instant.now()));

        } catch (Exception e) {
            log.error("Failed to create approval request: {}", e.getMessage(), e);
            // Still mark as flagged even if approval service fails
            execution.setStatus(TestStatus.FLAGGED_FOR_REVIEW);
            execution.setQaReviewStatus("PENDING_RETRY");
            testExecutionRepository.save(execution);
        }
    }

    /**
     * Handle QA decision from approval service.
     */
    @KafkaListener(topics = QA_DECISION_TOPIC, groupId = "agent-validation-service")
    @Transactional
    public void handleQADecision(QADecisionEvent event) {
        log.info("Received QA decision for execution {}: {}", event.executionId(), event.decision());

        testExecutionRepository.findById(java.util.UUID.fromString(event.executionId()))
            .ifPresentOrElse(
                execution -> {
                    execution.setQaReviewStatus(event.decision());
                    execution.setQaReviewComments(event.comments());
                    execution.setQaReviewerId(event.reviewerId());
                    execution.setQaReviewedAt(event.reviewedAt());

                    if ("APPROVED".equals(event.decision())) {
                        execution.setStatus(TestStatus.QA_APPROVED);
                        qaApprovedCounter.increment();
                    } else {
                        execution.setStatus(TestStatus.QA_REJECTED);
                        qaRejectedCounter.increment();
                    }

                    testExecutionRepository.save(execution);

                    // Record metric
                    meterRegistry.counter("agent.validation.qa.decisions",
                        "decision", event.decision(),
                        "risk_level", event.riskLevel() != null ? event.riskLevel() : "UNKNOWN"
                    ).increment();
                },
                () -> log.warn("Execution not found for QA decision: {}", event.executionId())
            );
    }

    /**
     * Determine the primary reason for flagging.
     */
    private String determineFlagReason(TestExecution execution) {
        ValidationProperties.QaConfig qaConfig = validationProperties.getQa();

        if (execution.getEvaluationScore() != null &&
            execution.getEvaluationScore().compareTo(qaConfig.getAutoFlagScoreThreshold()) < 0) {
            return "LOW_OVERALL_SCORE";
        }

        if (execution.getReflectionResult() != null) {
            HarmLevel harmLevel = execution.getReflectionResult().getPotentialHarmLevel();
            if (harmLevel == HarmLevel.MEDIUM || harmLevel == HarmLevel.HIGH) {
                return "POTENTIAL_HARM_" + harmLevel.name();
            }

            if (execution.getReflectionResult().isMiscalibrated()) {
                return "CONFIDENCE_MISCALIBRATION";
            }
        }

        if (execution.getRegressionResult() != null &&
            execution.getRegressionResult().isRegressionDetected()) {
            return "REGRESSION_DETECTED";
        }

        if (execution.getMetricResults() != null) {
            for (TestExecution.MetricResult result : execution.getMetricResults().values()) {
                if (result.getScore() != null &&
                    result.getScore().compareTo(qaConfig.getAutoFlagMetricThreshold()) < 0) {
                    return "LOW_METRIC_" + result.getMetricType();
                }
            }
        }

        return "UNKNOWN";
    }

    /**
     * Determine risk level based on execution results.
     */
    private String determineRiskLevel(TestExecution execution) {
        if (execution.getReflectionResult() != null) {
            HarmLevel harmLevel = execution.getReflectionResult().getPotentialHarmLevel();
            if (harmLevel == HarmLevel.HIGH) {
                return "HIGH";
            }
            if (harmLevel == HarmLevel.MEDIUM) {
                return "MEDIUM";
            }
        }

        if (execution.getEvaluationScore() != null) {
            if (execution.getEvaluationScore().compareTo(new BigDecimal("0.50")) < 0) {
                return "HIGH";
            }
            if (execution.getEvaluationScore().compareTo(new BigDecimal("0.70")) < 0) {
                return "MEDIUM";
            }
        }

        return "LOW";
    }

    // Event DTOs
    public record QAReviewEvent(
        String executionId,
        String flagReason,
        String riskLevel,
        Instant flaggedAt
    ) {}

    public record QADecisionEvent(
        String executionId,
        String decision,
        String comments,
        String reviewerId,
        String riskLevel,
        Instant reviewedAt
    ) {}
}
