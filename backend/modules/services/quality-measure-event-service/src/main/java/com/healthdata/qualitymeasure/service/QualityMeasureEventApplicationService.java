package com.healthdata.qualitymeasure.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.qualityevent.api.v1.dto.EvaluateMeasureRequest;
import com.healthdata.qualityevent.api.v1.dto.MeasureEventResponse;
import com.healthdata.qualityevent.event.MeasureScoreCalculatedEvent;
import com.healthdata.qualityevent.eventhandler.QualityMeasureEventHandler;
import com.healthdata.qualitymeasure.persistence.MeasureEvaluationRepository;
import com.healthdata.qualitymeasure.persistence.CohortMeasureRateRepository;
import com.healthdata.qualityevent.projection.MeasureEvaluationProjection;
import com.healthdata.qualityevent.projection.CohortMeasureRateProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Quality Measure Event Application Service
 *
 * Orchestrates measure evaluation event processing:
 * 1. Receives REST requests from QualityMeasureEventController
 * 2. Creates domain events (MeasureScoreCalculatedEvent)
 * 3. Delegates to Phase 4 QualityMeasureEventHandler for business logic
 * 4. Publishes events to Kafka for other services
 * 5. Persists projections to database for query optimization
 *
 * Business Logic:
 * - Score > 0.75 = MET
 * - Risk stratification: VERY_HIGH (>=0.90), HIGH (>=0.70), MEDIUM (>=0.40), LOW
 * - Cohort aggregation: compliance rate = numerator / denominator
 *
 * ★ Insight ─────────────────────────────────────
 * Quality measure evaluation requires careful threshold logic:
 * - Score thresholding determines measure compliance (MET/NOT_MET)
 * - Risk stratification enables prioritization for interventions
 * - Cohort aggregation provides population health analytics
 * - All data partitioned by tenant for multi-tenant isolation
 * ─────────────────────────────────────────────────
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class QualityMeasureEventApplicationService {

    private final QualityMeasureEventHandler measureEventHandler;
    private final MeasureEvaluationRepository evaluationRepository;
    private final CohortMeasureRateRepository cohortRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String MEASURE_EVENTS_TOPIC = "measure.events";

    /**
     * Evaluate measure for patient
     *
     * Processes measure score and updates projections
     */
    public MeasureEventResponse evaluateMeasure(EvaluateMeasureRequest request, String tenantId) {
        log.info("Evaluating measure: {}, patientId: {}, score: {}, tenant: {}",
            request.getMeasureCode(), request.getPatientId(), request.getScore(), tenantId);

        // Validate score range
        if (request.getScore() < 0.0f || request.getScore() > 1.0f) {
            throw new IllegalArgumentException("Score must be between 0.0 and 1.0");
        }

        // Create domain event
        MeasureScoreCalculatedEvent event = new MeasureScoreCalculatedEvent(
            tenantId,
            request.getPatientId(),
            request.getMeasureCode(),
            request.getScore(),
            "Measure evaluation"
        );

        // Delegate to Phase 4 event handler for business logic
        measureEventHandler.handle(event);

        // Update cohort aggregation
        updateCohortMetrics(request.getMeasureCode(), tenantId, request.getScore());

        // Publish to Kafka
        kafkaTemplate.send(MEASURE_EVENTS_TOPIC, request.getPatientId(), event);

        // Determine measure status based on score
        String status = request.getScore() > 0.75f ? "MET" : (request.getScore() > 0.0f ? "PARTIAL" : "NOT_MET");

        log.info("Measure evaluated: {}, status: {}", request.getMeasureCode(), status);

        return MeasureEventResponse.builder()
            .measureCode(request.getMeasureCode())
            .patientId(request.getPatientId())
            .score(request.getScore())
            .measureStatus(status)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Get patient risk score
     *
     * Retrieves calculated risk score and stratification
     */
    @Transactional(readOnly = true)
    public MeasureEventResponse getRiskScore(String patientId, String tenantId) {
        log.info("Getting risk score for patient: {}, tenant: {}", patientId, tenantId);

        Optional<MeasureEvaluationProjection> evaluation = evaluationRepository.findByPatientIdAndTenant(patientId, tenantId);

        if (evaluation.isEmpty()) {
            throw new RuntimeException("No evaluation found for patient: " + patientId);
        }

        MeasureEvaluationProjection proj = evaluation.get();

        return MeasureEventResponse.builder()
            .patientId(patientId)
            .riskLevel(calculateRiskLevel(proj.getScore()))  // Calculate risk level from score
            .score(proj.getScore().floatValue())
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Calculate risk level from measure score
     * LOW: score >= 80%, MEDIUM: 60-80%, HIGH: 40-60%, VERY_HIGH: < 40%
     */
    private String calculateRiskLevel(Double score) {
        if (score == null) return "UNKNOWN";
        if (score >= 80.0) return "LOW";
        if (score >= 60.0) return "MEDIUM";
        if (score >= 40.0) return "HIGH";
        return "VERY_HIGH";
    }

    /**
     * Get cohort compliance rate
     *
     * Aggregates compliance metrics across all patients for a measure
     */
    @Transactional(readOnly = true)
    public MeasureEventResponse getCohortCompliance(String measureCode, String tenantId) {
        log.info("Getting cohort compliance for measure: {}, tenant: {}", measureCode, tenantId);

        Optional<CohortMeasureRateProjection> cohort = cohortRepository.findByMeasureCodeAndTenant(measureCode, tenantId);

        if (cohort.isEmpty()) {
            throw new RuntimeException("No cohort data found for measure: " + measureCode);
        }

        CohortMeasureRateProjection cohortProj = cohort.get();

        return MeasureEventResponse.builder()
            .measureCode(measureCode)
            .complianceRate(cohortProj.getComplianceRate())
            .denominatorCount(cohortProj.getDenominatorCount())
            .numeratorCount(cohortProj.getNumeratorCount())
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Update cohort metrics
     *
     * Aggregates patient evaluations into cohort compliance rates
     */
    private void updateCohortMetrics(String measureCode, String tenantId, float score) {
        CohortMeasureRateProjection cohort = cohortRepository
            .findByMeasureCodeAndTenant(measureCode, tenantId)
            .orElseGet(() -> {
                CohortMeasureRateProjection newCohort = new CohortMeasureRateProjection(tenantId, measureCode);
                newCohort.setDenominatorCount(0);
                newCohort.setNumeratorCount(0);
                newCohort.setComplianceRate(0.0f);
                newCohort.setVersion(0);
                return newCohort;
            });

        // Update denominator (all patients evaluated)
        cohort.setDenominatorCount(cohort.getDenominatorCount() + 1);

        // Update numerator (patients meeting measure)
        if (score > 0.75f) {
            cohort.setNumeratorCount(cohort.getNumeratorCount() + 1);
        }

        // Calculate compliance rate
        if (cohort.getDenominatorCount() > 0) {
            cohort.setComplianceRate((float) cohort.getNumeratorCount() / cohort.getDenominatorCount());
        }

        cohort.incrementVersion();
        cohortRepository.save(cohort);

        log.info("Updated cohort metrics: measure={}, compliance={}, denominator={}, numerator={}",
            measureCode, cohort.getComplianceRate(), cohort.getDenominatorCount(), cohort.getNumeratorCount());
    }
}
