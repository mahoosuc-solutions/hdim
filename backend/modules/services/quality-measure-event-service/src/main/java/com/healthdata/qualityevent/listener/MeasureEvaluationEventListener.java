package com.healthdata.qualityevent.listener;

import com.healthdata.qualityevent.projection.MeasureEvaluationProjection;
import com.healthdata.qualityevent.repository.MeasureEvaluationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Measure Evaluation Event Listener (CQRS Event Handler)
 *
 * Consumes domain events from Kafka and updates the measure evaluation projection.
 *
 * Events consumed:
 * - measure.evaluated: Quality measure evaluated for patient
 * - measure.score.updated: Score changed
 * - measure.compliance.changed: Compliance status changed
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MeasureEvaluationEventListener {

    private final MeasureEvaluationRepository measureEvaluationRepository;

    /**
     * Handle measure.evaluated event
     * Creates a new evaluation projection when measure is evaluated
     */
    @KafkaListener(
        topics = "measure.evaluated",
        groupId = "quality-measure-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onMeasureEvaluated(String tenantId, String measureId, UUID patientId, Double score,
                                   String complianceStatus, Integer numerator, Integer denominator,
                                   Integer exclusions, String measureName, String measureVersion) {
        log.debug("Processing measure.evaluated event for measure {} (patient: {}, score: {})",
            measureId, patientId, score);

        boolean isCompliant = "COMPLIANT".equalsIgnoreCase(complianceStatus);

        MeasureEvaluationProjection projection = MeasureEvaluationProjection.builder()
            .tenantId(tenantId)
            .measureId(measureId)
            .patientId(patientId)
            .score(score)
            .complianceStatus(complianceStatus)
            .numerator(numerator != null ? numerator : 0)
            .denominator(denominator != null ? denominator : 0)
            .exclusions(exclusions != null ? exclusions : 0)
            .measureName(measureName)
            .measureVersion(measureVersion)
            .isCompliant(isCompliant)
            .meetsThreshold(score >= 90.0)  // Typical threshold
            .evaluatedAt(Instant.now())
            .createdAt(Instant.now())
            .lastUpdatedAt(Instant.now())
            .eventVersion(1L)
            .build();

        measureEvaluationRepository.save(projection);
        log.info("Created measure evaluation projection for measure {} (patient: {})", measureId, patientId);
    }

    /**
     * Handle measure.score.updated event
     * Updates evaluation score and compliance status
     */
    @KafkaListener(
        topics = "measure.score.updated",
        groupId = "quality-measure-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onScoreUpdated(String tenantId, String measureId, UUID patientId, Double newScore,
                               String newComplianceStatus) {
        log.debug("Processing measure.score.updated for measure {} (patient: {}, new score: {})",
            measureId, patientId, newScore);

        measureEvaluationRepository.findByTenantIdAndMeasureIdAndPatientId(tenantId, measureId, patientId)
            .ifPresentOrElse(
                projection -> {
                    projection.setScore(newScore);
                    projection.setComplianceStatus(newComplianceStatus);
                    projection.setIsCompliant("COMPLIANT".equalsIgnoreCase(newComplianceStatus));
                    projection.setMeetsThreshold(newScore >= 90.0);
                    projection.setLastUpdatedAt(Instant.now());
                    measureEvaluationRepository.save(projection);
                    log.info("Updated score for measure {} (patient: {})", measureId, patientId);
                },
                () -> log.warn("Measure evaluation projection not found for measure {} / patient {} in tenant {}",
                    measureId, patientId, tenantId)
            );
    }

    /**
     * Handle measure.compliance.changed event
     * Updates compliance rate for population
     */
    @KafkaListener(
        topics = "measure.compliance.changed",
        groupId = "quality-measure-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onComplianceChanged(String tenantId, String measureId, Double newComplianceRate) {
        log.debug("Processing measure.compliance.changed for measure {}: new rate={}",
            measureId, newComplianceRate);

        // This event updates all evaluations for the measure with the new population compliance rate
        // In a real scenario, you'd update a separate population-level projection
        // For now, we just log it
        log.info("Population compliance rate for measure {} updated to {}", measureId, newComplianceRate);
    }

    /**
     * Handle measure.numerator.updated event
     * Updates numerator count
     */
    @KafkaListener(
        topics = "measure.numerator.updated",
        groupId = "quality-measure-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onNumeratorUpdated(String tenantId, String measureId, UUID patientId, Integer newNumerator) {
        log.debug("Processing measure.numerator.updated for measure {} (patient: {}, numerator: {})",
            measureId, patientId, newNumerator);

        measureEvaluationRepository.findByTenantIdAndMeasureIdAndPatientId(tenantId, measureId, patientId)
            .ifPresentOrElse(
                projection -> {
                    projection.setNumerator(newNumerator);
                    // Recalculate score if denominator exists
                    if (projection.getDenominator() > 0) {
                        projection.setScore((double) newNumerator / projection.getDenominator() * 100);
                    }
                    projection.setLastUpdatedAt(Instant.now());
                    measureEvaluationRepository.save(projection);
                    log.debug("Updated numerator for measure {} (patient: {})", measureId, patientId);
                },
                () -> log.warn("Measure evaluation projection not found for measure {} / patient {}",
                    measureId, patientId)
            );
    }

    /**
     * Handle measure.denominator.updated event
     * Updates denominator count
     */
    @KafkaListener(
        topics = "measure.denominator.updated",
        groupId = "quality-measure-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onDenominatorUpdated(String tenantId, String measureId, UUID patientId, Integer newDenominator) {
        log.debug("Processing measure.denominator.updated for measure {} (patient: {}, denominator: {})",
            measureId, patientId, newDenominator);

        measureEvaluationRepository.findByTenantIdAndMeasureIdAndPatientId(tenantId, measureId, patientId)
            .ifPresentOrElse(
                projection -> {
                    projection.setDenominator(newDenominator);
                    // Recalculate score if numerator exists
                    if (newDenominator > 0) {
                        projection.setScore((double) projection.getNumerator() / newDenominator * 100);
                    }
                    projection.setLastUpdatedAt(Instant.now());
                    measureEvaluationRepository.save(projection);
                    log.debug("Updated denominator for measure {} (patient: {})", measureId, patientId);
                },
                () -> log.warn("Measure evaluation projection not found for measure {} / patient {}",
                    measureId, patientId)
            );
    }

    /**
     * Handle measure.exclusion.updated event
     * Updates exclusion count
     */
    @KafkaListener(
        topics = "measure.exclusion.updated",
        groupId = "quality-measure-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onExclusionUpdated(String tenantId, String measureId, UUID patientId, Integer newExclusions) {
        log.debug("Processing measure.exclusion.updated for measure {} (patient: {}, exclusions: {})",
            measureId, patientId, newExclusions);

        measureEvaluationRepository.findByTenantIdAndMeasureIdAndPatientId(tenantId, measureId, patientId)
            .ifPresentOrElse(
                projection -> {
                    projection.setExclusions(newExclusions);
                    projection.setLastUpdatedAt(Instant.now());
                    measureEvaluationRepository.save(projection);
                    log.debug("Updated exclusions for measure {} (patient: {})", measureId, patientId);
                },
                () -> log.warn("Measure evaluation projection not found for measure {} / patient {}",
                    measureId, patientId)
            );
    }
}
