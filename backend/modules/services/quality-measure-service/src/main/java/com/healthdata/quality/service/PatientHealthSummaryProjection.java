package com.healthdata.quality.service;

import com.healthdata.quality.dto.PopulationMetricsDTO;
import com.healthdata.quality.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Patient Health Summary Projection (CQRS Read Model)
 *
 * Event-driven service that materializes patient health summaries from events.
 * Subscribes to Kafka events from write model and updates read model.
 *
 * Events Subscribed:
 * - health-score.updated
 * - care-gap.auto-closed
 * - risk-assessment.updated
 * - clinical-alert.triggered
 *
 * Benefits:
 * - Eventual consistency
 * - Fast read queries (no joins)
 * - Independent scaling
 * - Rebuild capability
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatientHealthSummaryProjection {

    private final PatientHealthSummaryRepository readRepository;
    private final CareGapRepository careGapRepository;
    private final RiskAssessmentRepository riskRepository;
    private final MentalHealthAssessmentRepository mentalHealthRepository;
    private final PopulationMetricsRepository populationMetricsRepository;
    private final HealthScoreHistoryRepository healthScoreHistoryRepository;
    private final ClinicalAlertRepository clinicalAlertRepository;

    /**
     * Handle health score updated event
     */
    @KafkaListener(topics = "health-score.updated", groupId = "patient-health-summary-projection")
    @Transactional(rollbackFor = Exception.class)
    public void onHealthScoreUpdated(String tenantId, UUID patientId, Double score, String trend) {
        log.debug("Processing health score update for patient {}: score={}, trend={}",
            patientId, score, trend);

        PatientHealthSummaryEntity summary = getOrCreateSummary(tenantId, patientId);

        summary.setLatestHealthScore(score);
        summary.setHealthTrend(trend);
        summary.setHealthScoreUpdatedAt(Instant.now());

        readRepository.save(summary);
        log.info("Updated health score in read model for patient {}", patientId);
    }

    /**
     * Handle health score updated event with timestamp (for eventual consistency)
     */
    public void onHealthScoreUpdatedWithTimestamp(
        String tenantId,
        UUID patientId,
        Double score,
        String trend,
        Instant eventTimestamp
    ) {
        PatientHealthSummaryEntity summary = getOrCreateSummary(tenantId, patientId);

        // Check if event is newer than current data (handle out-of-order events)
        if (summary.getHealthScoreUpdatedAt() != null &&
            eventTimestamp.isBefore(summary.getHealthScoreUpdatedAt())) {
            log.debug("Ignoring older health score event for patient {}", patientId);
            return;
        }

        summary.setLatestHealthScore(score);
        summary.setHealthTrend(trend);
        summary.setHealthScoreUpdatedAt(eventTimestamp);

        readRepository.save(summary);
    }

    /**
     * Handle care gap auto-closed event
     */
    @KafkaListener(topics = "care-gap.auto-closed", groupId = "patient-health-summary-projection")
    @Transactional(rollbackFor = Exception.class)
    public void onCareGapAutoClosed(String tenantId, UUID patientId, String gapId) {
        log.debug("Processing care gap closure for patient {}: gapId={}", patientId, gapId);

        PatientHealthSummaryEntity summary = getOrCreateSummary(tenantId, patientId);

        // Recalculate counts from source
        long openCount = careGapRepository.countOpenCareGaps(tenantId, patientId);
        long urgentCount = careGapRepository.countUrgentCareGaps(tenantId, patientId);

        summary.setOpenCareGapsCount((int) openCount);
        summary.setUrgentGapsCount((int) urgentCount);
        summary.setCareGapsUpdatedAt(Instant.now());

        readRepository.save(summary);
        log.info("Updated care gap counts in read model for patient {}", patientId);
    }

    /**
     * Handle risk assessment updated event
     */
    @KafkaListener(topics = "risk-assessment.updated", groupId = "patient-health-summary-projection")
    @Transactional(rollbackFor = Exception.class)
    public void onRiskAssessmentUpdated(String tenantId, UUID patientId, String riskLevel, Double riskScore) {
        log.debug("Processing risk assessment update for patient {}: level={}, score={}",
            patientId, riskLevel, riskScore);

        PatientHealthSummaryEntity summary = getOrCreateSummary(tenantId, patientId);

        summary.setRiskLevel(riskLevel);
        summary.setRiskScore(riskScore);
        summary.setRiskUpdatedAt(Instant.now());

        readRepository.save(summary);
        log.info("Updated risk assessment in read model for patient {}", patientId);
    }

    /**
     * Handle clinical alert triggered event
     */
    @KafkaListener(topics = "clinical-alert.triggered", groupId = "patient-health-summary-projection")
    @Transactional(rollbackFor = Exception.class)
    public void onClinicalAlertTriggered(String tenantId, UUID patientId, String severity, String message) {
        log.debug("Processing clinical alert for patient {}: severity={}", patientId, severity);

        PatientHealthSummaryEntity summary = getOrCreateSummary(tenantId, patientId);

        // Increment alert counts
        summary.setActiveAlertsCount(summary.getActiveAlertsCount() + 1);
        if ("critical".equalsIgnoreCase(severity)) {
            summary.setCriticalAlertsCount(summary.getCriticalAlertsCount() + 1);
        }
        summary.setAlertsUpdatedAt(Instant.now());

        readRepository.save(summary);
        log.info("Updated alert counts in read model for patient {}", patientId);
    }

    /**
     * Rebuild projection for a single patient from write model
     *
     * Used for:
     * - Initial population of read model
     * - Fixing inconsistencies
     * - Recovery from errors
     */
    @Transactional
    public void rebuildProjectionForPatient(String tenantId, UUID patientId) {
        log.info("Rebuilding projection for patient {}", patientId);

        PatientHealthSummaryEntity summary = new PatientHealthSummaryEntity();
        summary.setTenantId(tenantId);
        summary.setPatientId(patientId);

        // Aggregate care gap data
        long openGaps = careGapRepository.countOpenCareGaps(tenantId, patientId);
        long urgentGaps = careGapRepository.countUrgentCareGaps(tenantId, patientId);
        summary.setOpenCareGapsCount((int) openGaps);
        summary.setUrgentGapsCount((int) urgentGaps);

        // Get latest risk assessment
        Optional<RiskAssessmentEntity> latestRisk =
            riskRepository.findLatestByTenantIdAndPatientId(tenantId, patientId);
        latestRisk.ifPresent(risk -> {
            summary.setRiskLevel(risk.getRiskLevel().name().toLowerCase().replace("_", "-"));
            summary.setRiskScore((double) risk.getRiskScore());
            summary.setRiskUpdatedAt(risk.getAssessmentDate());
        });

        // Aggregate health score data
        List<HealthScoreHistoryEntity> recentScores =
            healthScoreHistoryRepository.findRecentScores(tenantId, patientId, 2);
        if (!recentScores.isEmpty()) {
            HealthScoreHistoryEntity latestScore = recentScores.get(0);
            summary.setLatestHealthScore(latestScore.getOverallScore());
            summary.setHealthScoreUpdatedAt(latestScore.getCalculatedAt());

            // Calculate trend from last 2 scores
            if (recentScores.size() >= 2) {
                double latest = latestScore.getOverallScore();
                double previous = recentScores.get(1).getOverallScore();
                if (latest > previous + 5) {
                    summary.setHealthTrend("improving");
                } else if (latest < previous - 5) {
                    summary.setHealthTrend("declining");
                } else {
                    summary.setHealthTrend("stable");
                }
            }
        }

        // Aggregate clinical alert data
        long activeAlerts = clinicalAlertRepository.countByTenantIdAndPatientIdAndStatus(
            tenantId, patientId, ClinicalAlertEntity.AlertStatus.ACTIVE);
        summary.setActiveAlertsCount((int) activeAlerts);

        List<ClinicalAlertEntity> criticalAlerts = clinicalAlertRepository.findByTenantIdAndSeverityAndStatusOrderByTriggeredAtDesc(
            tenantId, ClinicalAlertEntity.AlertSeverity.CRITICAL, ClinicalAlertEntity.AlertStatus.ACTIVE);
        long criticalCount = criticalAlerts.stream()
            .filter(a -> patientId.equals(a.getPatientId()))
            .count();
        summary.setCriticalAlertsCount((int) criticalCount);
        summary.setAlertsUpdatedAt(Instant.now());

        // Aggregate mental health data
        Optional<MentalHealthAssessmentEntity> latestMentalHealth =
            mentalHealthRepository.findFirstByTenantIdAndPatientIdOrderByAssessmentDateDesc(tenantId, patientId);
        latestMentalHealth.ifPresent(assessment -> {
            summary.setLatestMentalHealthScore(assessment.getScore());
            summary.setMentalHealthSeverity(assessment.getSeverity());
            summary.setMentalHealthUpdatedAt(assessment.getAssessmentDate());
        });

        readRepository.save(summary);
        log.info("Rebuilt projection for patient {}", patientId);
    }

    /**
     * Rebuild all projections for a tenant
     *
     * Run this as a scheduled job or manually for full rebuild
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2 AM
    @Transactional
    public void rebuildAllProjections() {
        log.info("Starting scheduled rebuild of all projections");

        // Get all tenants from existing projections and care gaps
        Set<String> tenantIds = new HashSet<>();
        tenantIds.addAll(readRepository.findDistinctTenantIds());
        tenantIds.addAll(careGapRepository.findDistinctTenantIds());

        int totalRebuilt = 0;
        for (String tenantId : tenantIds) {
            log.info("Rebuilding projections for tenant {}", tenantId);

            // Get all patients from both the read model and care gaps
            Set<UUID> patientIds = new HashSet<>();
            patientIds.addAll(readRepository.findDistinctPatientIdsByTenantId(tenantId));
            patientIds.addAll(careGapRepository.findDistinctPatientIdsByTenantId(tenantId));

            for (UUID patientId : patientIds) {
                try {
                    rebuildProjectionForPatient(tenantId, patientId);
                    totalRebuilt++;
                } catch (Exception e) {
                    log.error("Failed to rebuild projection for patient {} in tenant {}: {}",
                        patientId, tenantId, e.getMessage());
                }
            }

            log.info("Rebuilt {} projections for tenant {}", patientIds.size(), tenantId);
        }

        log.info("Completed scheduled rebuild of {} projections across {} tenants",
            totalRebuilt, tenantIds.size());
    }

    /**
     * Get population metrics (aggregated from read model)
     */
    @Transactional(readOnly = true)
    public PopulationMetricsDTO getPopulationMetrics(String tenantId) {
        Long totalPatients = readRepository.countByTenantId(tenantId);
        Double avgHealthScore = readRepository.averageHealthScoreByTenantId(tenantId);
        Long highRisk = readRepository.countHighRiskPatients(tenantId);
        Long mediumRisk = readRepository.countMediumRiskPatients(tenantId);
        Long totalGaps = readRepository.totalOpenCareGaps(tenantId);

        return PopulationMetricsDTO.builder()
            .totalPatients(totalPatients != null ? totalPatients.intValue() : 0)
            .averageHealthScore(avgHealthScore != null ? avgHealthScore : 0.0)
            .highRiskCount(highRisk != null ? highRisk.intValue() : 0)
            .mediumRiskCount(mediumRisk != null ? mediumRisk.intValue() : 0)
            .totalCareGaps(totalGaps != null ? totalGaps.intValue() : 0)
            .build();
    }

    /**
     * Calculate and save daily population metrics
     *
     * Run this as a scheduled job to create daily snapshots
     */
    @Scheduled(cron = "0 30 1 * * ?") // Run daily at 1:30 AM
    @Transactional
    public void calculateDailyPopulationMetrics() {
        log.info("Calculating daily population metrics");

        // Get all tenants from the read model
        List<String> tenantIds = readRepository.findDistinctTenantIds();

        if (tenantIds.isEmpty()) {
            log.warn("No tenants found for population metrics calculation");
            return;
        }

        for (String tenantId : tenantIds) {
            calculateMetricsForTenant(tenantId);
        }

        log.info("Completed daily population metrics calculation for {} tenants", tenantIds.size());
    }

    /**
     * Calculate and save metrics for a specific tenant
     */
    private void calculateMetricsForTenant(String tenantId) {
        log.debug("Calculating metrics for tenant {}", tenantId);

        LocalDate today = LocalDate.now();

        // Check if metrics already exist for today
        Optional<PopulationMetricsEntity> existing =
            populationMetricsRepository.findByTenantIdAndMetricDate(tenantId, today);

        PopulationMetricsEntity metrics = existing.orElse(new PopulationMetricsEntity());
        metrics.setTenantId(tenantId);
        metrics.setMetricDate(today);

        // Aggregate from read model
        Long totalPatients = readRepository.countByTenantId(tenantId);
        metrics.setTotalPatients(totalPatients != null ? totalPatients.intValue() : 0);

        Double avgScore = readRepository.averageHealthScoreByTenantId(tenantId);
        metrics.setAverageHealthScore(avgScore);

        Long highRisk = readRepository.countHighRiskPatients(tenantId);
        metrics.setHighRiskCount(highRisk != null ? highRisk.intValue() : 0);

        Long mediumRisk = readRepository.countMediumRiskPatients(tenantId);
        metrics.setMediumRiskCount(mediumRisk != null ? mediumRisk.intValue() : 0);

        Long totalGaps = readRepository.totalOpenCareGaps(tenantId);
        metrics.setTotalCareGaps(totalGaps != null ? totalGaps.intValue() : 0);

        metrics.setCalculatedAt(Instant.now());

        populationMetricsRepository.save(metrics);
        log.info("Saved daily population metrics for tenant {} date {}", tenantId, today);
    }

    /**
     * Get or create patient health summary
     */
    private PatientHealthSummaryEntity getOrCreateSummary(String tenantId, UUID patientId) {
        return readRepository.findByTenantIdAndPatientId(tenantId, patientId)
            .orElseGet(() -> {
                PatientHealthSummaryEntity newSummary = new PatientHealthSummaryEntity();
                newSummary.setTenantId(tenantId);
                newSummary.setPatientId(patientId);
                return newSummary;
            });
    }
}
