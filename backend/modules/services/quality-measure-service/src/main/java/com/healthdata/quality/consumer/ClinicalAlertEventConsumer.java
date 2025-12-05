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
            String patientId = (String) event.get("patientId");
            Integer previousScore = ((Number) event.get("previousScore")).intValue();
            Integer currentScore = ((Number) event.get("currentScore")).intValue();

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
            String patientId = (String) event.get("patientId");
            String condition = (String) event.get("condition");
            String metric = (String) event.get("metric");
            String severity = (String) event.get("severity");

            // TODO: Implement chronic disease deterioration alert logic
            // This would create CHRONIC_DETERIORATION type alerts
            // For now, just log the event

            log.info("Chronic disease deterioration detected for patient {}: {} - {} ({})",
                patientId, condition, metric, severity);

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

            // This listener could be used for:
            // - Sending notifications to external systems
            // - Triggering workflow automations
            // - Logging to audit trail
            // - Updating dashboards

            String alertId = (String) event.get("alertId");
            String severity = (String) event.get("severity");

            log.info("Alert {} triggered with severity {}", alertId, severity);

            // TODO: Implement additional alert processing logic

        } catch (Exception e) {
            log.error("Error processing alert triggered event: {}", e.getMessage(), e);
        }
    }
}
