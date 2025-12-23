package com.healthdata.quality.consumer;

import com.healthdata.quality.dto.ClinicalAlertDTO;
import com.healthdata.quality.persistence.MentalHealthAssessmentEntity;
import com.healthdata.quality.persistence.MentalHealthAssessmentRepository;
import com.healthdata.quality.persistence.RiskAssessmentEntity;
import com.healthdata.quality.persistence.RiskAssessmentRepository;
import com.healthdata.quality.service.ClinicalAlertService;
import com.healthdata.quality.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Clinical Alert Event Consumer
 *
 * Listens for events that may trigger clinical alerts:
 * - mental-health-assessment.submitted
 * - risk-assessment.updated
 * - health-score.significant-change
 * - chronic-disease.deterioration
 *
 * Evaluates conditions and creates alerts as needed, then routes
 * notifications to appropriate channels.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClinicalAlertEventConsumer {

    private final ClinicalAlertService clinicalAlertService;
    private final NotificationService notificationService;
    private final MentalHealthAssessmentRepository mentalHealthAssessmentRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;

    /**
     * Listen for mental health assessment events
     */
    @KafkaListener(
        topics = "mental-health-assessment.submitted",
        groupId = "clinical-alert-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleMentalHealthAssessment(Map<String, Object> event) {
        try {
            log.info("Received mental-health-assessment.submitted event: {}", event);

            String tenantId = (String) event.get("tenantId");
            String assessmentId = (String) event.get("assessmentId");

            // Fetch full assessment from database
            MentalHealthAssessmentEntity assessment = mentalHealthAssessmentRepository
                .findById(UUID.fromString(assessmentId))
                .orElseThrow(() -> new IllegalArgumentException(
                    "Assessment not found: " + assessmentId
                ));

            // Evaluate for alerts
            ClinicalAlertDTO alert = clinicalAlertService.evaluateMentalHealthAssessment(
                tenantId, assessment
            );

            // Send notifications if alert was created
            if (alert != null) {
                notificationService.sendNotification(tenantId, alert);
                log.info("Alert {} created and notifications sent for assessment {}",
                    alert.getId(), assessmentId);
            }

        } catch (Exception e) {
            log.error("Error processing mental health assessment event: {}", e.getMessage(), e);
        }
    }

    /**
     * Listen for risk assessment update events
     */
    @KafkaListener(
        topics = "risk-assessment.updated",
        groupId = "clinical-alert-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleRiskAssessmentUpdate(Map<String, Object> event) {
        try {
            log.info("Received risk-assessment.updated event: {}", event);

            String tenantId = (String) event.get("tenantId");
            String assessmentId = (String) event.get("assessmentId");
            String riskLevel = (String) event.get("riskLevel");

            // Only alert on VERY_HIGH risk
            if (!"VERY_HIGH".equals(riskLevel)) {
                log.debug("Risk level {} does not warrant alert", riskLevel);
                return;
            }

            // Fetch full assessment
            RiskAssessmentEntity assessment = riskAssessmentRepository
                .findById(UUID.fromString(assessmentId))
                .orElseThrow(() -> new IllegalArgumentException(
                    "Risk assessment not found: " + assessmentId
                ));

            // Evaluate for alerts
            ClinicalAlertDTO alert = clinicalAlertService.evaluateRiskAssessment(
                tenantId, assessment
            );

            // Send notifications if alert was created
            if (alert != null) {
                notificationService.sendNotification(tenantId, alert);
                log.warn("Risk escalation alert {} created for patient {}",
                    alert.getId(), assessment.getPatientId());
            }

        } catch (Exception e) {
            log.error("Error processing risk assessment event: {}", e.getMessage(), e);
        }
    }

    /**
     * Listen for health score significant change events
     */
    @KafkaListener(
        topics = "health-score.significant-change",
        groupId = "clinical-alert-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleHealthScoreChange(Map<String, Object> event) {
        try {
            log.info("Received health-score.significant-change event: {}", event);

            String tenantId = (String) event.get("tenantId");
            UUID patientId = parsePatientIdValue(event.get("patientId"));
            Integer previousScore = ((Number) event.get("previousScore")).intValue();
            Integer currentScore = ((Number) event.get("currentScore")).intValue();

            if (patientId == null) {
                log.warn("Missing patientId in health-score.significant-change event");
                return;
            }

            // Only alert on decline (not improvement)
            if (currentScore >= previousScore) {
                log.debug("Health score improved or stable, no alert needed");
                return;
            }

            // Evaluate for alerts
            ClinicalAlertDTO alert = clinicalAlertService.evaluateHealthScoreChange(
                tenantId, patientId, previousScore, currentScore
            );

            // Send notifications if alert was created
            if (alert != null) {
                notificationService.sendNotification(tenantId, alert);
                log.info("Health score decline alert {} created for patient {}",
                    alert.getId(), patientId);
            }

        } catch (Exception e) {
            log.error("Error processing health score change event: {}", e.getMessage(), e);
        }
    }

    /**
     * Listen for chronic disease deterioration events
     */
    @KafkaListener(
        topics = "chronic-disease.deterioration",
        groupId = "clinical-alert-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleChronicDiseaseDeterioration(Map<String, Object> event) {
        try {
            log.info("Received chronic-disease.deterioration event: {}", event);

            String tenantId = (String) event.get("tenantId");
            UUID patientId = parsePatientIdValue(event.get("patientId"));
            String condition = (String) event.get("condition");
            String metric = (String) event.get("metric");
            String severity = (String) event.get("severity");

            if (patientId == null) {
                log.warn("Missing patientId in chronic-disease.deterioration event");
                return;
            }

            // Evaluate for chronic deterioration alert
            ClinicalAlertDTO alert = clinicalAlertService.evaluateChronicDiseaseDeterioration(
                tenantId, patientId, condition, metric, severity
            );

            // Send notifications if alert was created
            if (alert != null) {
                notificationService.sendNotification(tenantId, alert);
                log.warn("Chronic disease deterioration alert {} created for patient {}: {} - {} ({})",
                    alert.getId(), patientId, condition, metric, severity);
            } else {
                log.debug("Chronic disease deterioration for patient {} did not warrant alert: {} - {} ({})",
                    patientId, condition, metric, severity);
            }

        } catch (Exception e) {
            log.error("Error processing chronic disease deterioration event: {}",
                e.getMessage(), e);
        }
    }

    /**
     * Listen for alert triggered events (for cascading notifications)
     */
    @KafkaListener(
        topics = "clinical-alert.triggered",
        groupId = "clinical-alert-notification-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAlertTriggered(Map<String, Object> event) {
        try {
            log.info("Received clinical-alert.triggered event: {}", event);

            String alertId = (String) event.get("alertId");
            UUID patientId = parsePatientIdValue(event.get("patientId"));
            String tenantId = (String) event.get("tenantId");
            String alertType = (String) event.get("alertType");
            String severity = (String) event.get("severity");
            String triggeredAt = (String) event.get("triggeredAt");

            if (patientId == null) {
                log.warn("Missing patientId in clinical-alert.triggered event");
                return;
            }

            // Log to audit trail
            log.info("AUDIT: Clinical alert triggered - Alert ID: {}, Patient: {}, Type: {}, Severity: {}, Timestamp: {}",
                alertId, patientId, alertType, severity, triggeredAt);

            // Send notifications to external systems
            // Note: NotificationService already handles the primary notification channels
            // This additional processing is for secondary/cascading notifications
            try {
                notificationService.sendNotificationWithStatus(tenantId,
                    buildAlertDTOFromEvent(event));
                log.debug("External system notifications sent for alert {}", alertId);
            } catch (Exception e) {
                log.error("Failed to send external notifications for alert {}: {}",
                    alertId, e.getMessage());
                // Continue processing despite notification failure
            }

            // Dashboard update is handled via WebSocket in NotificationService
            // The alert has already been persisted to the database by ClinicalAlertService
            // Dashboards can query the database for real-time updates
            log.debug("Alert {} available for dashboard display via database query", alertId);

            log.info("Alert {} processing completed - Type: {}, Severity: {}",
                alertId, alertType, severity);

        } catch (Exception e) {
            log.error("Error processing alert triggered event: {}", e.getMessage(), e);
        }
    }

    private UUID parsePatientIdValue(Object patientId) {
        if (patientId instanceof UUID) {
            return (UUID) patientId;
        }
        if (patientId == null) {
            return null;
        }
        try {
            return UUID.fromString(patientId.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Build ClinicalAlertDTO from event map for notification purposes
     */
    private ClinicalAlertDTO buildAlertDTOFromEvent(Map<String, Object> event) {
        return ClinicalAlertDTO.builder()
            .id((String) event.get("alertId"))
            .patientId(parsePatientIdValue(event.get("patientId")))
            .tenantId((String) event.get("tenantId"))
            .alertType((String) event.get("alertType"))
            .severity((String) event.get("severity"))
            .build();
    }
}
