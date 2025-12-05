package com.healthdata.quality.service;

import com.healthdata.quality.dto.ClinicalAlertDTO;
import com.healthdata.quality.persistence.*;
import com.healthdata.quality.service.notification.ClinicalAlertNotificationTrigger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Clinical Alert Service
 *
 * Core service for creating, managing, and tracking clinical alerts:
 * - Mental health crisis detection (PHQ-9 ≥20, GAD-7 ≥15, suicide risk)
 * - Risk level escalation alerts
 * - Health score decline alerts
 * - Alert deduplication (24-hour window)
 * - Alert prioritization by severity
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClinicalAlertService {

    private final ClinicalAlertRepository alertRepository;
    private final AlertEvaluationService alertEvaluationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ClinicalAlertNotificationTrigger notificationTrigger;

    // Alert thresholds
    private static final int PHQ9_CRITICAL_THRESHOLD = 20;  // Severe depression
    private static final int GAD7_HIGH_THRESHOLD = 15;       // Severe anxiety
    private static final int HEALTH_SCORE_DECLINE_THRESHOLD = 15; // Points

    // Deduplication window (24 hours)
    private static final long DEDUPLICATION_WINDOW_HOURS = 24;

    /**
     * Evaluate mental health assessment and create alert if needed
     */
    @Transactional
    public ClinicalAlertDTO evaluateMentalHealthAssessment(
        String tenantId,
        MentalHealthAssessmentEntity assessment
    ) {
        log.info("Evaluating mental health assessment {} for patient {}",
            assessment.getType(), assessment.getPatientId());

        // Check for suicide risk (PHQ-9 item 9 > 0) - CRITICAL
        if (assessment.getType() == MentalHealthAssessmentEntity.AssessmentType.PHQ_9) {
            Integer item9Response = assessment.getResponses().get("item_9");
            if (item9Response != null && item9Response > 0) {
                return createSuicideRiskAlert(tenantId, assessment);
            }
        }

        // Check for severe depression (PHQ-9 ≥ 20) - CRITICAL
        if (assessment.getType() == MentalHealthAssessmentEntity.AssessmentType.PHQ_9 &&
            assessment.getScore() >= PHQ9_CRITICAL_THRESHOLD) {
            return createSevereDepressionAlert(tenantId, assessment);
        }

        // Check for severe anxiety (GAD-7 ≥ 15) - HIGH
        if (assessment.getType() == MentalHealthAssessmentEntity.AssessmentType.GAD_7 &&
            assessment.getScore() >= GAD7_HIGH_THRESHOLD) {
            return createSevereAnxietyAlert(tenantId, assessment);
        }

        // No alert needed
        return null;
    }

    /**
     * Evaluate risk assessment and create alert if escalated to VERY_HIGH
     */
    @Transactional
    public ClinicalAlertDTO evaluateRiskAssessment(
        String tenantId,
        RiskAssessmentEntity riskAssessment
    ) {
        log.info("Evaluating risk assessment for patient {}, level: {}",
            riskAssessment.getPatientId(), riskAssessment.getRiskLevel());

        // Alert on VERY_HIGH risk level - HIGH severity
        if (riskAssessment.getRiskLevel() == RiskAssessmentEntity.RiskLevel.VERY_HIGH) {
            return createRiskEscalationAlert(tenantId, riskAssessment);
        }

        return null;
    }

    /**
     * Evaluate health score change and create alert if declined significantly
     */
    @Transactional
    public ClinicalAlertDTO evaluateHealthScoreChange(
        String tenantId,
        String patientId,
        int previousScore,
        int currentScore
    ) {
        int decline = previousScore - currentScore;

        log.info("Evaluating health score change for patient {}: {} -> {} (decline: {})",
            patientId, previousScore, currentScore, decline);

        // Alert on decline ≥ 15 points - MEDIUM severity
        if (decline >= HEALTH_SCORE_DECLINE_THRESHOLD) {
            return createHealthScoreDeclineAlert(
                tenantId, patientId, previousScore, currentScore, decline
            );
        }

        return null;
    }

    /**
     * Get active alerts for a patient (sorted by severity and time)
     */
    public List<ClinicalAlertDTO> getActiveAlerts(String tenantId, String patientId) {
        List<ClinicalAlertEntity> alerts = alertRepository.findActiveAlertsForPatient(
            tenantId, patientId
        );

        return alerts.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Acknowledge an alert
     */
    @Transactional
    public ClinicalAlertDTO acknowledgeAlert(
        String tenantId,
        String alertId,
        String acknowledgedBy
    ) {
        UUID id = UUID.fromString(alertId);
        ClinicalAlertEntity alert = alertRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));

        // Verify tenant ownership
        if (!alert.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Alert does not belong to tenant");
        }

        alert.setStatus(ClinicalAlertEntity.AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedAt(Instant.now());
        alert.setAcknowledgedBy(acknowledgedBy);

        alert = alertRepository.save(alert);

        log.info("Alert {} acknowledged by {}", alertId, acknowledgedBy);

        // Convert to DTO
        ClinicalAlertDTO alertDTO = mapToDTO(alert);

        // Trigger notification for alert acknowledgment
        try {
            notificationTrigger.onAlertAcknowledged(tenantId, alertDTO);
        } catch (Exception e) {
            log.error("Failed to trigger alert acknowledgment notification for alert {}: {}",
                    alertId, e.getMessage(), e);
            // Don't fail the acknowledgment if notification fails
        }

        return alertDTO;
    }

    /**
     * Resolve an alert
     */
    @Transactional
    public ClinicalAlertDTO resolveAlert(
        String tenantId,
        String alertId,
        String resolvedBy
    ) {
        UUID id = UUID.fromString(alertId);
        ClinicalAlertEntity alert = alertRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));

        // Verify tenant ownership
        if (!alert.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Alert does not belong to tenant");
        }

        alert.setStatus(ClinicalAlertEntity.AlertStatus.RESOLVED);
        alert = alertRepository.save(alert);

        log.info("Alert {} resolved by {}", alertId, resolvedBy);

        return mapToDTO(alert);
    }

    /**
     * Create suicide risk alert (CRITICAL)
     */
    private ClinicalAlertDTO createSuicideRiskAlert(
        String tenantId,
        MentalHealthAssessmentEntity assessment
    ) {
        // Check for duplicates
        if (isDuplicate(tenantId, assessment.getPatientId(),
                ClinicalAlertEntity.AlertType.MENTAL_HEALTH_CRISIS)) {
            log.info("Duplicate suicide risk alert suppressed for patient {}",
                assessment.getPatientId());
            return null;
        }

        ClinicalAlertEntity alert = ClinicalAlertEntity.builder()
            .tenantId(tenantId)
            .patientId(assessment.getPatientId())
            .alertType(ClinicalAlertEntity.AlertType.MENTAL_HEALTH_CRISIS)
            .severity(ClinicalAlertEntity.AlertSeverity.CRITICAL)
            .title("URGENT: Suicide Risk Detected")
            .message(String.format(
                "Patient reported suicidal ideation on PHQ-9 assessment. " +
                "PHQ-9 score: %d/27. IMMEDIATE intervention required. " +
                "Contact patient and assess safety immediately.",
                assessment.getScore()
            ))
            .sourceEventType("mental-health-assessment")
            .sourceEventId(assessment.getId().toString())
            .triggeredAt(Instant.now())
            .escalated(true)
            .escalatedAt(Instant.now())
            .status(ClinicalAlertEntity.AlertStatus.ACTIVE)
            .build();

        alert = alertRepository.save(alert);

        // Publish alert event
        publishAlertEvent(alert);

        log.warn("CRITICAL ALERT: Suicide risk detected for patient {}", assessment.getPatientId());

        // Convert to DTO
        ClinicalAlertDTO alertDTO = mapToDTO(alert);

        // Trigger notification for critical alert
        try {
            notificationTrigger.onAlertTriggered(tenantId, alertDTO);
        } catch (Exception e) {
            log.error("Failed to trigger suicide risk alert notification for patient {}: {}",
                    assessment.getPatientId(), e.getMessage(), e);
            // Don't fail the alert creation if notification fails
        }

        return alertDTO;
    }

    /**
     * Create severe depression alert (CRITICAL)
     */
    private ClinicalAlertDTO createSevereDepressionAlert(
        String tenantId,
        MentalHealthAssessmentEntity assessment
    ) {
        // Check for duplicates
        if (isDuplicate(tenantId, assessment.getPatientId(),
                ClinicalAlertEntity.AlertType.MENTAL_HEALTH_CRISIS)) {
            return null;
        }

        ClinicalAlertEntity alert = ClinicalAlertEntity.builder()
            .tenantId(tenantId)
            .patientId(assessment.getPatientId())
            .alertType(ClinicalAlertEntity.AlertType.MENTAL_HEALTH_CRISIS)
            .severity(ClinicalAlertEntity.AlertSeverity.CRITICAL)
            .title("Severe Depression Detected")
            .message(String.format(
                "PHQ-9 score: %d/27 (severe range). Patient requires urgent mental health " +
                "evaluation and treatment. Consider safety assessment and care coordination.",
                assessment.getScore()
            ))
            .sourceEventType("mental-health-assessment")
            .sourceEventId(assessment.getId().toString())
            .triggeredAt(Instant.now())
            .escalated(false)
            .status(ClinicalAlertEntity.AlertStatus.ACTIVE)
            .build();

        alert = alertRepository.save(alert);
        publishAlertEvent(alert);

        log.warn("CRITICAL ALERT: Severe depression detected for patient {}",
            assessment.getPatientId());

        // Convert to DTO
        ClinicalAlertDTO alertDTO = mapToDTO(alert);

        // Trigger notification for critical alert
        try {
            notificationTrigger.onAlertTriggered(tenantId, alertDTO);
        } catch (Exception e) {
            log.error("Failed to trigger severe depression alert notification for patient {}: {}",
                    assessment.getPatientId(), e.getMessage(), e);
            // Don't fail the alert creation if notification fails
        }

        return alertDTO;
    }

    /**
     * Create severe anxiety alert (HIGH)
     */
    private ClinicalAlertDTO createSevereAnxietyAlert(
        String tenantId,
        MentalHealthAssessmentEntity assessment
    ) {
        // Check for duplicates
        if (isDuplicate(tenantId, assessment.getPatientId(),
                ClinicalAlertEntity.AlertType.MENTAL_HEALTH_CRISIS)) {
            return null;
        }

        ClinicalAlertEntity alert = ClinicalAlertEntity.builder()
            .tenantId(tenantId)
            .patientId(assessment.getPatientId())
            .alertType(ClinicalAlertEntity.AlertType.MENTAL_HEALTH_CRISIS)
            .severity(ClinicalAlertEntity.AlertSeverity.HIGH)
            .title("Severe Anxiety Detected")
            .message(String.format(
                "GAD-7 score: %d/21 (severe range). Patient requires mental health evaluation " +
                "and treatment for severe anxiety symptoms.",
                assessment.getScore()
            ))
            .sourceEventType("mental-health-assessment")
            .sourceEventId(assessment.getId().toString())
            .triggeredAt(Instant.now())
            .escalated(false)
            .status(ClinicalAlertEntity.AlertStatus.ACTIVE)
            .build();

        alert = alertRepository.save(alert);
        publishAlertEvent(alert);

        log.warn("HIGH ALERT: Severe anxiety detected for patient {}",
            assessment.getPatientId());

        // Convert to DTO
        ClinicalAlertDTO alertDTO = mapToDTO(alert);

        // Trigger notification for high severity alert
        try {
            notificationTrigger.onAlertTriggered(tenantId, alertDTO);
        } catch (Exception e) {
            log.error("Failed to trigger severe anxiety alert notification for patient {}: {}",
                    assessment.getPatientId(), e.getMessage(), e);
            // Don't fail the alert creation if notification fails
        }

        return alertDTO;
    }

    /**
     * Create risk escalation alert (HIGH)
     */
    private ClinicalAlertDTO createRiskEscalationAlert(
        String tenantId,
        RiskAssessmentEntity riskAssessment
    ) {
        // Check for duplicates
        if (isDuplicate(tenantId, riskAssessment.getPatientId(),
                ClinicalAlertEntity.AlertType.RISK_ESCALATION)) {
            return null;
        }

        ClinicalAlertEntity alert = ClinicalAlertEntity.builder()
            .tenantId(tenantId)
            .patientId(riskAssessment.getPatientId())
            .alertType(ClinicalAlertEntity.AlertType.RISK_ESCALATION)
            .severity(ClinicalAlertEntity.AlertSeverity.HIGH)
            .title("Patient Risk Level: Very High")
            .message(String.format(
                "Risk score: %d/100 (very high). Patient requires immediate care coordination, " +
                "comprehensive care plan review, and weekly monitoring.",
                riskAssessment.getRiskScore()
            ))
            .sourceEventType("risk-assessment")
            .sourceEventId(riskAssessment.getId().toString())
            .triggeredAt(Instant.now())
            .escalated(false)
            .status(ClinicalAlertEntity.AlertStatus.ACTIVE)
            .build();

        alert = alertRepository.save(alert);
        publishAlertEvent(alert);

        log.warn("HIGH ALERT: Risk escalated to VERY_HIGH for patient {}",
            riskAssessment.getPatientId());

        // Convert to DTO
        ClinicalAlertDTO alertDTO = mapToDTO(alert);

        // Trigger notification for high severity alert
        try {
            notificationTrigger.onAlertTriggered(tenantId, alertDTO);
        } catch (Exception e) {
            log.error("Failed to trigger risk escalation alert notification for patient {}: {}",
                    riskAssessment.getPatientId(), e.getMessage(), e);
            // Don't fail the alert creation if notification fails
        }

        return alertDTO;
    }

    /**
     * Create health score decline alert (MEDIUM)
     */
    private ClinicalAlertDTO createHealthScoreDeclineAlert(
        String tenantId,
        String patientId,
        int previousScore,
        int currentScore,
        int decline
    ) {
        // Check for duplicates
        if (isDuplicate(tenantId, patientId,
                ClinicalAlertEntity.AlertType.HEALTH_DECLINE)) {
            return null;
        }

        ClinicalAlertEntity alert = ClinicalAlertEntity.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .alertType(ClinicalAlertEntity.AlertType.HEALTH_DECLINE)
            .severity(ClinicalAlertEntity.AlertSeverity.MEDIUM)
            .title("Health Score Decline Detected")
            .message(String.format(
                "Overall health score declined by %d points (%d → %d). " +
                "Review patient status and address contributing factors.",
                decline, previousScore, currentScore
            ))
            .sourceEventType("health-score-change")
            .sourceEventId(null)
            .triggeredAt(Instant.now())
            .escalated(false)
            .status(ClinicalAlertEntity.AlertStatus.ACTIVE)
            .build();

        alert = alertRepository.save(alert);
        publishAlertEvent(alert);

        log.info("MEDIUM ALERT: Health score declined by {} points for patient {}",
            decline, patientId);

        // Convert to DTO
        ClinicalAlertDTO alertDTO = mapToDTO(alert);

        // Trigger notification for medium severity alert
        try {
            notificationTrigger.onAlertTriggered(tenantId, alertDTO);
        } catch (Exception e) {
            log.error("Failed to trigger health score decline alert notification for patient {}: {}",
                    patientId, e.getMessage(), e);
            // Don't fail the alert creation if notification fails
        }

        return alertDTO;
    }

    /**
     * Check if duplicate alert exists within 24-hour window
     */
    private boolean isDuplicate(
        String tenantId,
        String patientId,
        ClinicalAlertEntity.AlertType alertType
    ) {
        Instant since = Instant.now().minus(DEDUPLICATION_WINDOW_HOURS, ChronoUnit.HOURS);

        List<ClinicalAlertEntity> recentAlerts = alertRepository.findRecentDuplicates(
            tenantId, patientId, alertType, since
        );

        return !recentAlerts.isEmpty();
    }

    /**
     * Publish alert event to Kafka
     */
    private void publishAlertEvent(ClinicalAlertEntity alert) {
        Map<String, Object> event = new HashMap<>();
        event.put("alertId", alert.getId().toString());
        event.put("patientId", alert.getPatientId());
        event.put("tenantId", alert.getTenantId());
        event.put("alertType", alert.getAlertType().name());
        event.put("severity", alert.getSeverity().name());
        event.put("triggeredAt", alert.getTriggeredAt().toString());

        kafkaTemplate.send("clinical-alert.triggered", event);

        log.info("Published clinical-alert.triggered event for alert {}", alert.getId());
    }

    /**
     * Map entity to DTO
     */
    private ClinicalAlertDTO mapToDTO(ClinicalAlertEntity entity) {
        return ClinicalAlertDTO.builder()
            .id(entity.getId().toString())
            .patientId(entity.getPatientId())
            .tenantId(entity.getTenantId())
            .alertType(entity.getAlertType().name())
            .severity(entity.getSeverity().name())
            .title(entity.getTitle())
            .message(entity.getMessage())
            .sourceEventType(entity.getSourceEventType())
            .sourceEventId(entity.getSourceEventId())
            .triggeredAt(entity.getTriggeredAt())
            .acknowledgedAt(entity.getAcknowledgedAt())
            .acknowledgedBy(entity.getAcknowledgedBy())
            .escalated(entity.isEscalated())
            .escalatedAt(entity.getEscalatedAt())
            .status(entity.getStatus().name())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
