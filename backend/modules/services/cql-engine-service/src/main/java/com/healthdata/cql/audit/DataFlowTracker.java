package com.healthdata.cql.audit;

import com.healthdata.cql.event.audit.CqlEvaluationAuditEvent.DataFlowStep;
import com.healthdata.cql.websocket.DataFlowStepPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ThreadLocal-based tracker for capturing data flow steps during CQL evaluation.
 *
 * This component enables recording step-by-step data flow through the CQL engine,
 * answering the critical question: "How is data flowing and why?"
 *
 * Usage:
 * <pre>
 * dataFlowTracker.startTracking(evaluationId);
 * try {
 *     dataFlowTracker.recordStep("Fetch Patient Demographics", "DATA_FETCH",
 *         List.of("Patient"), null, patientData, "Patient age: 45",
 *         "Retrieved patient birth date for age calculation");
 *     // ... perform CQL evaluation
 * } finally {
 *     List<DataFlowStep> steps = dataFlowTracker.getSteps();
 *     dataFlowTracker.clearTracking();
 * }
 * </pre>
 */
@Component
public class DataFlowTracker {

    private static final Logger logger = LoggerFactory.getLogger(DataFlowTracker.class);

    private final ThreadLocal<TrackingContext> currentContext = new ThreadLocal<>();

    @Value("${audit.data-flow-tracking.enabled:true}")
    private boolean trackingEnabled;

    @Value("${audit.data-flow-tracking.max-steps:50}")
    private int maxSteps;

    @Value("${audit.data-flow-tracking.realtime-publish:true}")
    private boolean realtimePublish;

    @Autowired(required = false)
    private DataFlowStepPublisher stepPublisher;

    /**
     * Start tracking data flow for an evaluation
     */
    public void startTracking(String evaluationId) {
        if (!trackingEnabled) {
            return;
        }

        TrackingContext context = new TrackingContext(evaluationId);
        currentContext.set(context);
        logger.debug("Started data flow tracking for evaluation: {}", evaluationId);
    }

    /**
     * Record a data flow step
     *
     * @param stepName Name of the step (e.g., "Fetch Patient Demographics")
     * @param stepType Type of step: DATA_FETCH, EXPRESSION_EVAL, LOGIC_DECISION, CQL_EXECUTION
     * @param resourcesAccessed List of FHIR resources accessed (e.g., ["Patient", "Observation"])
     * @param inputData Optional input data summary
     * @param outputData Optional output data summary
     * @param decision The decision or result of this step
     * @param reasoning Why this decision was made
     */
    public void recordStep(String stepName, String stepType, List<String> resourcesAccessed,
                          String inputData, String outputData, String decision, String reasoning) {
        if (!trackingEnabled) {
            return;
        }

        TrackingContext context = currentContext.get();
        if (context == null) {
            logger.warn("Attempted to record step '{}' but tracking not started", stepName);
            return;
        }

        if (context.steps.size() >= maxSteps) {
            logger.warn("Max steps ({}) reached for evaluation: {}", maxSteps, context.evaluationId);
            return;
        }

        Instant stepStart = Instant.now();
        int stepNumber = context.steps.size() + 1;

        DataFlowStep step = DataFlowStep.builder()
                .stepNumber(stepNumber)
                .stepName(stepName)
                .stepType(stepType)
                .timestamp(stepStart)
                .resourcesAccessed(resourcesAccessed != null ? resourcesAccessed : Collections.emptyList())
                .inputData(truncate(inputData, 500))
                .outputData(truncate(outputData, 500))
                .decision(truncate(decision, 200))
                .reasoning(truncate(reasoning, 500))
                .durationMs(null) // Will be set if endStep() is called
                .build();

        context.steps.add(step);
        context.currentStepStart = stepStart;

        logger.debug("Recorded step {}: {} (type: {})", stepNumber, stepName, stepType);

        // Publish step in real-time if enabled
        if (realtimePublish && stepPublisher != null) {
            try {
                stepPublisher.publishStep(step, context.evaluationId, null);
            } catch (Exception e) {
                logger.warn("Failed to publish data flow step in real-time: {}", e.getMessage());
            }
        }
    }

    /**
     * Simplified step recording for quick logging
     */
    public void recordStep(String stepName, String stepType, String decision, String reasoning) {
        recordStep(stepName, stepType, null, null, null, decision, reasoning);
    }

    /**
     * End the current step and record its duration
     */
    public void endCurrentStep() {
        if (!trackingEnabled) {
            return;
        }

        TrackingContext context = currentContext.get();
        if (context == null || context.steps.isEmpty()) {
            return;
        }

        if (context.currentStepStart != null) {
            long durationMs = Instant.now().toEpochMilli() - context.currentStepStart.toEpochMilli();
            DataFlowStep lastStep = context.steps.get(context.steps.size() - 1);

            // Create new step with duration (DataFlowStep is immutable)
            DataFlowStep updatedStep = DataFlowStep.builder()
                    .stepNumber(lastStep.getStepNumber())
                    .stepName(lastStep.getStepName())
                    .stepType(lastStep.getStepType())
                    .timestamp(lastStep.getTimestamp())
                    .resourcesAccessed(lastStep.getResourcesAccessed())
                    .inputData(lastStep.getInputData())
                    .outputData(lastStep.getOutputData())
                    .decision(lastStep.getDecision())
                    .reasoning(lastStep.getReasoning())
                    .durationMs(durationMs)
                    .build();

            context.steps.set(context.steps.size() - 1, updatedStep);
            context.currentStepStart = null;
        }
    }

    /**
     * Get all recorded data flow steps
     */
    public List<DataFlowStep> getSteps() {
        if (!trackingEnabled) {
            return Collections.emptyList();
        }

        TrackingContext context = currentContext.get();
        if (context == null) {
            return Collections.emptyList();
        }

        return new ArrayList<>(context.steps);
    }

    /**
     * Get the current evaluation ID being tracked
     */
    public String getCurrentEvaluationId() {
        TrackingContext context = currentContext.get();
        return context != null ? context.evaluationId : null;
    }

    /**
     * Check if tracking is currently active
     */
    public boolean isTracking() {
        return trackingEnabled && currentContext.get() != null;
    }

    /**
     * Clear tracking context (MUST be called in finally block)
     */
    public void clearTracking() {
        TrackingContext context = currentContext.get();
        if (context != null) {
            logger.debug("Cleared data flow tracking for evaluation: {} ({} steps)",
                    context.evaluationId, context.steps.size());
        }
        currentContext.remove();
    }

    /**
     * Truncate string to max length
     */
    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    /**
     * Internal tracking context
     */
    private static class TrackingContext {
        final String evaluationId;
        final List<DataFlowStep> steps;
        Instant currentStepStart;

        TrackingContext(String evaluationId) {
            this.evaluationId = evaluationId;
            this.steps = new ArrayList<>();
        }
    }
}
