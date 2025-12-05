package com.healthdata.cql.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.cql.audit.DataFlowTracker;
import com.healthdata.cql.entity.CqlEvaluation;
import com.healthdata.cql.event.audit.AuditEvent;
import com.healthdata.cql.event.audit.AuditEventProducer;
import com.healthdata.cql.event.audit.CqlEvaluationAuditEvent;
import com.healthdata.cql.event.audit.CqlEvaluationAuditEvent.DataFlowStep;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AOP Aspect for auditing CQL evaluation operations.
 *
 * This aspect automatically captures audit events for all CQL evaluations,
 * tracking:
 * - When evaluations start and complete
 * - What data was used
 * - Who initiated the evaluation
 * - Results and performance metrics
 *
 * Events are published asynchronously to Kafka for processing by the audit consumer service.
 */
@Aspect
@Component
public class CqlEvaluationAuditAspect {

    private static final Logger logger = LoggerFactory.getLogger(CqlEvaluationAuditAspect.class);

    private final AuditEventProducer auditEventProducer;
    private final ObjectMapper objectMapper;
    private final DataFlowTracker dataFlowTracker;

    @Value("${audit.kafka.enabled:true}")
    private boolean auditEnabled;

    public CqlEvaluationAuditAspect(
            AuditEventProducer auditEventProducer,
            ObjectMapper objectMapper,
            DataFlowTracker dataFlowTracker) {
        this.auditEventProducer = auditEventProducer;
        this.objectMapper = objectMapper;
        this.dataFlowTracker = dataFlowTracker;
    }

    /**
     * Audit CQL evaluation execution.
     *
     * Intercepts calls to CqlEvaluationService.executeEvaluation() and automatically
     * emits audit events before and after execution.
     */
    @Around("execution(* com.healthdata.cql.service.CqlEvaluationService.executeEvaluation(..))")
    public Object auditEvaluationExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!auditEnabled) {
            return joinPoint.proceed();
        }

        // Extract method parameters
        Object[] args = joinPoint.getArgs();
        UUID evaluationId = (UUID) args[0];
        String tenantId = (String) args[1];

        Instant startTime = Instant.now();
        String eventId = UUID.randomUUID().toString();

        logger.debug("Auditing CQL evaluation: evaluationId={}, tenantId={}", evaluationId, tenantId);

        // Start data flow tracking for this evaluation
        dataFlowTracker.startTracking(evaluationId.toString());

        try {
            // Execute the actual evaluation
            Object result = joinPoint.proceed();

            // After successful execution, emit audit event with data flow steps
            if (result instanceof CqlEvaluation) {
                CqlEvaluation evaluation = (CqlEvaluation) result;
                List<DataFlowStep> dataFlowSteps = dataFlowTracker.getSteps();
                emitSuccessAuditEvent(evaluation, startTime, eventId, dataFlowSteps);
            }

            return result;

        } catch (Exception e) {
            // On failure, emit failure audit event (may include partial data flow)
            List<DataFlowStep> dataFlowSteps = dataFlowTracker.getSteps();
            emitFailureAuditEvent(evaluationId, tenantId, startTime, eventId, e, dataFlowSteps);
            throw e;
        } finally {
            // Always clear tracking context
            dataFlowTracker.clearTracking();
        }
    }

    /**
     * Emit audit event for successful evaluation
     */
    private void emitSuccessAuditEvent(CqlEvaluation evaluation, Instant startTime, String eventId,
                                       List<DataFlowStep> dataFlowSteps) {
        try {
            Instant endTime = Instant.now();
            long durationMs = endTime.toEpochMilli() - startTime.toEpochMilli();

            // Extract user from security context (simplified for now)
            String performedBy = "system"; // TODO: Extract from SecurityContextHolder

            // Build audit event
            CqlEvaluationAuditEvent auditEvent = CqlEvaluationAuditEvent.builder()
                    .eventId(eventId)
                    .timestamp(endTime)
                    .tenantId(evaluation.getTenantId())
                    .performedBy(performedBy)
                    .action("EVALUATE_CQL")
                    .resourceType("CQL_EVALUATION")
                    .resourceId(evaluation.getId().toString())
                    .result(AuditEvent.OperationResult.SUCCESS)
                    .details(buildDetails(evaluation))
                    .clientIp(null) // TODO: Extract from request context
                    .requestId(null) // TODO: Extract from MDC or request
                    // Evaluation-specific fields
                    .evaluationId(evaluation.getId())
                    .cqlLibraryId(evaluation.getLibrary().getId())
                    .cqlLibraryName(evaluation.getLibrary().getLibraryName())
                    .cqlLibraryVersion(evaluation.getLibrary().getVersion())
                    .patientId(evaluation.getPatientId())
                    .measureIdentifier(evaluation.getLibrary().getLibraryName())
                    .fhirResourcesAccessed(Collections.emptyList()) // TODO: Track from FHIR client
                    .fhirResourceCount(Collections.emptyMap()) // TODO: Track from FHIR client
                    .evaluationStartTime(startTime)
                    .evaluationEndTime(endTime)
                    .durationMs(durationMs)
                    .resultSummary("Evaluation completed: " + evaluation.getStatus())
                    .numerator(parseNumerator(evaluation))
                    .denominator(parseDenominator(evaluation))
                    .exclusion(parseExclusion(evaluation))
                    .errorMessage(null)
                    .cacheHit(false) // TODO: Track cache hits
                    .dataFlowSteps(dataFlowSteps != null ? dataFlowSteps : Collections.emptyList())
                    .build();

            // Publish audit event asynchronously
            auditEventProducer.publishEvaluationAudit(auditEvent);

            logger.info("Emitted evaluation audit event: evaluationId={}, duration={}ms",
                    evaluation.getId(), durationMs);

        } catch (Exception e) {
            // Never let audit failures affect business logic
            logger.error("Failed to emit evaluation audit event: evaluationId={}, error={}",
                    evaluation.getId(), e.getMessage(), e);
        }
    }

    /**
     * Emit audit event for failed evaluation
     */
    private void emitFailureAuditEvent(UUID evaluationId, String tenantId, Instant startTime,
                                       String eventId, Exception error, List<DataFlowStep> dataFlowSteps) {
        try {
            Instant endTime = Instant.now();
            long durationMs = endTime.toEpochMilli() - startTime.toEpochMilli();

            String performedBy = "system"; // TODO: Extract from SecurityContextHolder

            CqlEvaluationAuditEvent auditEvent = CqlEvaluationAuditEvent.builder()
                    .eventId(eventId)
                    .timestamp(endTime)
                    .tenantId(tenantId)
                    .performedBy(performedBy)
                    .action("EVALUATE_CQL")
                    .resourceType("CQL_EVALUATION")
                    .resourceId(evaluationId.toString())
                    .result(AuditEvent.OperationResult.FAILURE)
                    .details("Evaluation failed: " + error.getMessage())
                    .clientIp(null)
                    .requestId(null)
                    .evaluationId(evaluationId)
                    .cqlLibraryId(null)
                    .cqlLibraryName(null)
                    .cqlLibraryVersion(null)
                    .patientId(null)
                    .measureIdentifier(null)
                    .fhirResourcesAccessed(Collections.emptyList())
                    .fhirResourceCount(Collections.emptyMap())
                    .evaluationStartTime(startTime)
                    .evaluationEndTime(endTime)
                    .durationMs(durationMs)
                    .resultSummary("Evaluation failed")
                    .numerator(null)
                    .denominator(null)
                    .exclusion(null)
                    .errorMessage(error.getMessage())
                    .cacheHit(false)
                    .dataFlowSteps(dataFlowSteps != null ? dataFlowSteps : Collections.emptyList())
                    .build();

            auditEventProducer.publishEvaluationAudit(auditEvent);

            logger.warn("Emitted evaluation failure audit event: evaluationId={}, error={}",
                    evaluationId, error.getMessage());

        } catch (Exception e) {
            logger.error("Failed to emit evaluation failure audit event: evaluationId={}, error={}",
                    evaluationId, e.getMessage(), e);
        }
    }

    /**
     * Build detailed information about the evaluation
     */
    private String buildDetails(CqlEvaluation evaluation) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "evaluationId", evaluation.getId(),
                    "libraryName", evaluation.getLibrary().getLibraryName(),
                    "libraryVersion", evaluation.getLibrary().getVersion(),
                    "patientId", evaluation.getPatientId(),
                    "status", evaluation.getStatus(),
                    "evaluationDate", evaluation.getEvaluationDate()
            ));
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Parse numerator from evaluation results
     */
    private Boolean parseNumerator(CqlEvaluation evaluation) {
        try {
            String results = evaluation.getEvaluationResult();
            if (results != null && results.contains("\"numerator\"")) {
                // TODO: Parse actual numerator value from results JSON
                return true;
            }
        } catch (Exception e) {
            logger.debug("Could not parse numerator from evaluation results", e);
        }
        return null;
    }

    /**
     * Parse denominator from evaluation results
     */
    private Boolean parseDenominator(CqlEvaluation evaluation) {
        try {
            String results = evaluation.getEvaluationResult();
            if (results != null && results.contains("\"denominator\"")) {
                // TODO: Parse actual denominator value from results JSON
                return true;
            }
        } catch (Exception e) {
            logger.debug("Could not parse denominator from evaluation results", e);
        }
        return null;
    }

    /**
     * Parse exclusion from evaluation results
     */
    private Boolean parseExclusion(CqlEvaluation evaluation) {
        try {
            String results = evaluation.getEvaluationResult();
            if (results != null && results.contains("\"exclusion\"")) {
                // TODO: Parse actual exclusion value from results JSON
                return false;
            }
        } catch (Exception e) {
            logger.debug("Could not parse exclusion from evaluation results", e);
        }
        return null;
    }
}
