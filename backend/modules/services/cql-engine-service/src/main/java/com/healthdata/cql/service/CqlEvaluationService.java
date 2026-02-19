package com.healthdata.cql.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.cql.engine.MeasureTemplateEngine;
import com.healthdata.cql.entity.CqlEvaluation;
import com.healthdata.cql.entity.CqlLibrary;
import com.healthdata.cql.measure.MeasureResult;
import com.healthdata.cql.repository.CqlEvaluationRepository;
import com.healthdata.cql.repository.CqlLibraryRepository;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import com.healthdata.metrics.HealthcareMetrics;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing CQL Evaluations
 *
 * Provides business logic for executing CQL expressions using the template-driven
 * measure engine, storing evaluation results, and retrieving historical evaluation data.
 */
@Service
@Transactional
public class CqlEvaluationService {

    private static final Logger logger = LoggerFactory.getLogger(CqlEvaluationService.class);

    private final CqlEvaluationRepository evaluationRepository;
    private final CqlLibraryRepository libraryRepository;
    private final MeasureTemplateEngine templateEngine;
    private final ObjectMapper objectMapper;
    private final CqlAuditIntegration cqlAuditIntegration;
    private final Tracer tracer;
    private final HealthcareMetrics healthcareMetrics;

    public CqlEvaluationService(
            CqlEvaluationRepository evaluationRepository,
            CqlLibraryRepository libraryRepository,
            MeasureTemplateEngine templateEngine,
            ObjectMapper objectMapper,
            CqlAuditIntegration cqlAuditIntegration,
            Tracer tracer,
            HealthcareMetrics healthcareMetrics) {
        this.evaluationRepository = evaluationRepository;
        this.libraryRepository = libraryRepository;
        this.templateEngine = templateEngine;
        this.objectMapper = objectMapper;
        this.cqlAuditIntegration = cqlAuditIntegration;
        this.tracer = tracer;
        this.healthcareMetrics = healthcareMetrics;
    }

    /**
     * Create a new evaluation record
     */
    public CqlEvaluation createEvaluation(String tenantId, UUID libraryId, UUID patientId) {
        logger.info("Creating evaluation for patient: {} with library: {}", patientId, libraryId);

        CqlLibrary library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new IllegalArgumentException("Library not found: " + libraryId));

        if (!library.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Library tenant mismatch");
        }

        CqlEvaluation evaluation = new CqlEvaluation(tenantId, library, patientId);
        evaluation.setStatus("PENDING");
        evaluation.setEvaluationDate(Instant.now());

        CqlEvaluation savedEvaluation = evaluationRepository.save(evaluation);
        logger.info("Created evaluation with ID: {}", savedEvaluation.getId());
        return savedEvaluation;
    }

    /**
     * Execute CQL evaluation for a patient using the template-driven measure engine
     */
    public CqlEvaluation executeEvaluation(UUID evaluationId, String tenantId) {
        logger.info("Executing evaluation: {}", evaluationId);

        CqlEvaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluation not found: " + evaluationId));

        if (!evaluation.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Evaluation tenant mismatch");
        }

        Span span = tracer.spanBuilder("cql.evaluate_measure")
                .setAttribute("evaluation.id", evaluationId.toString())
                .setAttribute("tenant.id", tenantId)
                .startSpan();

        long startTime = System.currentTimeMillis();
        // Extract measureId before try block so it's accessible in catch blocks
        CqlLibrary library = evaluation.getLibrary();
        String measureId = library.getLibraryName();

        try (Scope scope = span.makeCurrent()) {
            // Library and measureId extracted before try block
            UUID patientId = evaluation.getPatientId();

            span.setAttribute("measure.id", measureId);
            span.setAttribute("patient.id", patientId.toString());

            logger.info("Evaluating measure {} for patient {} using template engine", measureId, patientId);

            // Execute measure using template engine
            MeasureResult result = templateEngine.evaluateMeasure(measureId, patientId, tenantId);

            // Convert result to JSON
            String resultJson = objectMapper.writeValueAsString(result);

            // Update evaluation record
            evaluation.setEvaluationResult(resultJson);
            evaluation.setStatus("SUCCESS");
            long durationMs = System.currentTimeMillis() - startTime;
            evaluation.setDurationMs(durationMs);
            healthcareMetrics.recordEvaluation(measureId, true, java.time.Duration.ofMillis(durationMs));

            span.setAttribute("result.in_denominator", result.isInDenominator());
            span.setAttribute("result.in_numerator", result.isInNumerator());
            span.setAttribute("duration_ms", durationMs);
            span.setStatus(StatusCode.OK);

            logger.info("Evaluation completed successfully: measure={}, patient={}, inDenominator={}, inNumerator={}",
                    measureId, patientId, result.isInDenominator(), result.isInNumerator());

            // Publish audit event for CQL evaluation
            String evaluatedBy = getAuthenticatedUsername();
            cqlAuditIntegration.publishCqlEvaluationEvent(
                    tenantId,
                    patientId.toString(),
                    measureId,
                    evaluationId.toString(),
                    result,
                    evaluatedBy,
                    durationMs
            );

        } catch (JsonProcessingException e) {
            logger.error("Error serializing evaluation result: {}", e.getMessage(), e);
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, "Result serialization error");
            evaluation.setStatus("FAILED");
            evaluation.setErrorMessage("Result serialization error: " + e.getMessage());
            evaluation.setDurationMs(System.currentTimeMillis() - startTime);
            healthcareMetrics.recordEvaluation(measureId, false, java.time.Duration.ofMillis(System.currentTimeMillis() - startTime));
        } catch (Exception e) {
            logger.error("Evaluation failed: {}", e.getMessage(), e);
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            evaluation.setStatus("FAILED");
            evaluation.setErrorMessage(e.getMessage());
            evaluation.setDurationMs(System.currentTimeMillis() - startTime);
            healthcareMetrics.recordEvaluation(measureId, false, java.time.Duration.ofMillis(System.currentTimeMillis() - startTime));
        } finally {
            span.end();
        }

        CqlEvaluation updatedEvaluation = evaluationRepository.save(evaluation);
        logger.info("Completed evaluation: {} with status: {}", evaluationId, evaluation.getStatus());
        return updatedEvaluation;
    }

    /**
     * Update evaluation result
     */
    public CqlEvaluation updateEvaluationResult(
            UUID evaluationId, String tenantId, String result, String status) {
        logger.info("Updating evaluation result: {}", evaluationId);

        CqlEvaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluation not found: " + evaluationId));

        if (!evaluation.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Evaluation tenant mismatch");
        }

        evaluation.setEvaluationResult(result);
        evaluation.setStatus(status);

        CqlEvaluation updatedEvaluation = evaluationRepository.save(evaluation);
        logger.info("Updated evaluation: {}", evaluationId);
        return updatedEvaluation;
    }

    /**
     * Get all evaluations for a tenant with pagination
     */
    @Transactional(readOnly = true)
    public Page<CqlEvaluation> getAllEvaluations(String tenantId, Pageable pageable) {
        return evaluationRepository.findByTenantId(tenantId, pageable);
    }

    /**
     * Get evaluation by ID
     */
    @Transactional(readOnly = true)
    public Optional<CqlEvaluation> getEvaluationById(UUID evaluationId, String tenantId) {
        return evaluationRepository.findById(evaluationId)
                .filter(evaluation -> evaluation.getTenantId().equals(tenantId));
    }

    /**
     * Get all evaluations for a patient
     */
    @Transactional(readOnly = true)
    public List<CqlEvaluation> getEvaluationsForPatient(String tenantId, UUID patientId) {
        return evaluationRepository.findByTenantIdAndPatientIdOrderByEvaluationDateDesc(
                tenantId, patientId);
    }

    /**
     * Get evaluations for a patient with pagination
     */
    @Transactional(readOnly = true)
    public Page<CqlEvaluation> getEvaluationsForPatient(
            String tenantId, UUID patientId, Pageable pageable) {
        return evaluationRepository.findByTenantIdAndPatientId(tenantId, patientId, pageable);
    }

    /**
     * Get evaluations for a library
     */
    @Transactional(readOnly = true)
    public List<CqlEvaluation> getEvaluationsForLibrary(String tenantId, UUID libraryId) {
        return evaluationRepository.findByTenantIdAndLibrary_Id(tenantId, libraryId);
    }

    /**
     * Get evaluations for a library with pagination
     */
    @Transactional(readOnly = true)
    public Page<CqlEvaluation> getEvaluationsForLibrary(
            String tenantId, UUID libraryId, Pageable pageable) {
        return evaluationRepository.findByTenantIdAndLibrary_Id(tenantId, libraryId, pageable);
    }

    /**
     * Get the most recent evaluation for a patient and library
     */
    @Transactional(readOnly = true)
    public Optional<CqlEvaluation> getLatestEvaluationForPatientAndLibrary(
            String tenantId, UUID patientId, UUID libraryId) {
        return evaluationRepository.findLatestByPatientAndLibrary(tenantId, patientId, libraryId);
    }

    /**
     * Get evaluations by status
     */
    @Transactional(readOnly = true)
    public List<CqlEvaluation> getEvaluationsByStatus(String tenantId, String status) {
        return evaluationRepository.findByTenantIdAndStatus(tenantId, status);
    }

    /**
     * Get evaluations by status with pagination
     */
    @Transactional(readOnly = true)
    public Page<CqlEvaluation> getEvaluationsByStatus(
            String tenantId, String status, Pageable pageable) {
        return evaluationRepository.findByTenantIdAndStatus(tenantId, status, pageable);
    }

    /**
     * Get evaluations within a date range
     */
    @Transactional(readOnly = true)
    public List<CqlEvaluation> getEvaluationsByDateRange(
            String tenantId, Instant startDate, Instant endDate) {
        return evaluationRepository.findByDateRange(tenantId, startDate, endDate);
    }

    /**
     * Get evaluations for a patient within a date range
     */
    @Transactional(readOnly = true)
    public List<CqlEvaluation> getEvaluationsForPatientByDateRange(
            String tenantId, UUID patientId, Instant startDate, Instant endDate) {
        return evaluationRepository.findByPatientAndDateRange(
                tenantId, patientId, startDate, endDate);
    }

    /**
     * Get successful evaluations for a patient
     */
    @Transactional(readOnly = true)
    public List<CqlEvaluation> getSuccessfulEvaluationsForPatient(String tenantId, UUID patientId) {
        return evaluationRepository.findByTenantIdAndPatientIdAndStatus(
                tenantId, patientId, "SUCCESS");
    }

    /**
     * Get failed evaluations for retry
     */
    @Transactional(readOnly = true)
    public List<CqlEvaluation> getFailedEvaluationsForRetry(String tenantId, int hoursBack) {
        Instant cutoffDate = Instant.now().minus(hoursBack, ChronoUnit.HOURS);
        return evaluationRepository.findFailedEvaluationsForRetry(tenantId, cutoffDate);
    }

    /**
     * Count evaluations by status
     */
    @Transactional(readOnly = true)
    public long countEvaluationsByStatus(String tenantId, String status) {
        return evaluationRepository.countByTenantIdAndStatus(tenantId, status);
    }

    /**
     * Count evaluations for a library
     */
    @Transactional(readOnly = true)
    public long countEvaluationsForLibrary(String tenantId, UUID libraryId) {
        return evaluationRepository.countByTenantIdAndLibrary_Id(tenantId, libraryId);
    }

    /**
     * Count evaluations for a patient
     */
    @Transactional(readOnly = true)
    public long countEvaluationsForPatient(String tenantId, UUID patientId) {
        return evaluationRepository.countByTenantIdAndPatientId(tenantId, patientId);
    }

    /**
     * Get average evaluation duration for a library
     */
    @Transactional(readOnly = true)
    public Double getAverageDurationForLibrary(String tenantId, UUID libraryId) {
        return evaluationRepository.getAverageDurationForLibrary(tenantId, libraryId);
    }

    /**
     * Retry a failed evaluation
     */
    public CqlEvaluation retryEvaluation(UUID evaluationId, String tenantId) {
        logger.info("Retrying evaluation: {}", evaluationId);

        CqlEvaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluation not found: " + evaluationId));

        if (!evaluation.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Evaluation tenant mismatch");
        }

        if (!"FAILED".equals(evaluation.getStatus())) {
            throw new IllegalArgumentException("Can only retry failed evaluations");
        }

        // Reset evaluation for retry
        evaluation.setStatus("PENDING");
        evaluation.setErrorMessage(null);
        evaluation.setEvaluationDate(Instant.now());

        evaluationRepository.save(evaluation);

        // Execute the evaluation
        return executeEvaluation(evaluationId, tenantId);
    }

    /**
     * Batch evaluate CQL for multiple patients using concurrent template engine
     */
    public List<CqlEvaluation> batchEvaluate(
            String tenantId, UUID libraryId, List<UUID> patientIds) {
        logger.info("Starting concurrent batch evaluation for {} patients with library: {}",
                patientIds.size(), libraryId);

        long startTime = System.currentTimeMillis();

        // Get library for measure ID
        CqlLibrary library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new IllegalArgumentException("Library not found: " + libraryId));

        if (!library.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Library tenant mismatch");
        }

        String measureId = library.getLibraryName();

        // Use template engine's concurrent batch evaluation
        Map<UUID, MeasureResult> results = templateEngine.evaluateBatch(measureId, patientIds, tenantId);

        // Create and save evaluation records for each patient
        List<CqlEvaluation> evaluations = patientIds.stream()
                .map(patientId -> {
                    try {
                        // Create evaluation record
                        CqlEvaluation evaluation = new CqlEvaluation(tenantId, library, patientId);
                        evaluation.setStatus("PENDING");
                        evaluation.setEvaluationDate(Instant.now());

                        // Get result from batch
                        MeasureResult result = results.get(patientId);
                        if (result != null) {
                            String resultJson = objectMapper.writeValueAsString(result);
                            evaluation.setEvaluationResult(resultJson);
                            evaluation.setStatus("SUCCESS");
                        } else {
                            evaluation.setStatus("FAILED");
                            evaluation.setErrorMessage("No result returned from engine");
                        }

                        return evaluationRepository.save(evaluation);
                    } catch (Exception e) {
                        logger.error("Failed to save evaluation for patient {}: {}", patientId, e.getMessage());
                        return null;
                    }
                })
                .filter(evaluation -> evaluation != null)
                .toList();

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Completed concurrent batch evaluation: {} successful out of {} patients in {}ms",
                evaluations.size(), patientIds.size(), duration);
        healthcareMetrics.recordBatchEvaluation(measureId, evaluations.size(), java.time.Duration.ofMillis(duration));

        return evaluations;
    }

    /**
     * Delete old evaluations (data retention)
     */
    public void deleteOldEvaluations(String tenantId, int daysToRetain) {
        logger.info("Deleting evaluations older than {} days for tenant: {}",
                daysToRetain, tenantId);

        Instant cutoffDate = Instant.now().minus(daysToRetain, ChronoUnit.DAYS);
        evaluationRepository.deleteByTenantIdAndEvaluationDateBefore(tenantId, cutoffDate);

        logger.info("Deleted old evaluations for tenant: {}", tenantId);
    }

    /**
     * Get the authenticated username from security context
     *
     * @return The username of the authenticated user, or "system" if no user is authenticated
     */
    private String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "system";
    }
}
