package com.healthdata.quality.service.notification;

import com.healthdata.quality.dto.HealthScoreDTO;
import com.healthdata.quality.dto.notification.HealthScoreNotificationRequest;
import com.healthdata.quality.model.NotificationRecipient;
import com.healthdata.quality.persistence.NotificationEntity;
import com.healthdata.quality.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Health Score Notification Trigger
 *
 * Automatically sends notifications when health scores change significantly.
 * Integrates with HealthScoreService to trigger real-time notifications
 * when patient health scores are calculated.
 *
 * Notification Rules:
 * - WebSocket: All score changes (real-time dashboard updates)
 * - Email: Score decreases of 10+ points
 * - SMS: Score decreases of 15+ points (critical changes)
 *
 * Usage:
 * <pre>
 * healthScoreNotificationTrigger.onHealthScoreCalculated(
 *     tenantId, healthScore, previousScore
 * );
 * </pre>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HealthScoreNotificationTrigger {

    private final NotificationService notificationService;
    private final RecipientResolutionService recipientResolutionService;

    /**
     * Trigger notification when health score is calculated
     * Called by HealthScoreService after calculating a new health score
     *
     * @param tenantId Tenant ID
     * @param healthScore New health score
     * @param previousScore Previous health score (null if first calculation)
     */
    public void onHealthScoreCalculated(String tenantId, HealthScoreDTO healthScore, Double previousScore) {
        try {
            // Skip notification if no significant change
            if (!isSignificantChange(healthScore, previousScore)) {
                log.debug("Skipping health score notification - no significant change for patient {}",
                        healthScore.getPatientId());
                return;
            }

            // Calculate change metrics
            Double newScore = healthScore.getOverallScore();
            Double changeAmount = previousScore != null ? newScore - previousScore : 0.0;
            String changeDirection = determineChangeDirection(changeAmount);

            // Get recipients for this patient/tenant
            Map<String, String> recipients = getRecipients(tenantId, healthScore.getPatientId());

            // Build notification request
            HealthScoreNotificationRequest request = HealthScoreNotificationRequest.builder()
                    .healthScore(healthScore)
                    .tenantId(tenantId)
                    .patientId(healthScore.getPatientId())
                    .previousScore(previousScore != null ? previousScore : newScore)
                    .newScore(newScore)
                    .changeAmount(changeAmount)
                    .changeDirection(changeDirection)
                    .recipients(recipients)
                    .build();

            // Send notification via NotificationService
            NotificationService.NotificationStatus status = notificationService.sendNotification(request);

            if (status.isAllSuccessful()) {
                log.info("Health score notification sent successfully for patient {} (score: {} → {})",
                        healthScore.getPatientId(), previousScore, newScore);
            } else {
                log.warn("Health score notification partially failed for patient {}: {}",
                        healthScore.getPatientId(), status.getChannelStatus());
            }

        } catch (Exception e) {
            log.error("Failed to send health score notification for patient {}: {}",
                    healthScore.getPatientId(), e.getMessage(), e);
        }
    }

    /**
     * Determine if the health score change is significant enough to notify
     * Prevents notification fatigue from minor fluctuations
     *
     * @param healthScore New health score
     * @param previousScore Previous health score
     * @return true if change is significant
     */
    private boolean isSignificantChange(HealthScoreDTO healthScore, Double previousScore) {
        // Always notify on first calculation
        if (previousScore == null) {
            return true;
        }

        Double newScore = healthScore.getOverallScore();
        Double changeAmount = Math.abs(newScore - previousScore);

        // Notify on changes of 5+ points
        if (changeAmount >= 5.0) {
            return true;
        }

        // Notify if health score drops below critical threshold (< 40)
        if (newScore < 40.0 && previousScore >= 40.0) {
            return true;
        }

        // Notify if health score enters excellent range (>= 90)
        if (newScore >= 90.0 && previousScore < 90.0) {
            return true;
        }

        return false;
    }

    /**
     * Determine the direction of score change
     *
     * @param changeAmount Amount of change (positive = increase, negative = decrease)
     * @return "INCREASED", "DECREASED", or "STABLE"
     */
    private String determineChangeDirection(Double changeAmount) {
        if (changeAmount > 1.0) {
            return "INCREASED";
        } else if (changeAmount < -1.0) {
            return "DECREASED";
        }
        return "STABLE";
    }

    /**
     * Get recipients for health score notifications
     * Resolves recipients from patient's care team and user preferences
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return Map of channel -> recipient ID
     */
    private Map<String, String> getRecipients(String tenantId, String patientId) {
        Map<String, String> recipients = new HashMap<>();

        // Determine notification severity for recipient resolution
        NotificationEntity.NotificationSeverity severity = NotificationEntity.NotificationSeverity.MEDIUM;

        // Resolve recipients for EMAIL channel
        List<NotificationRecipient> emailRecipients = recipientResolutionService.resolveRecipients(
            tenantId, patientId, NotificationEntity.NotificationChannel.EMAIL, severity
        );
        if (!emailRecipients.isEmpty()) {
            // Use primary care provider's email, or first recipient if no primary
            String email = emailRecipients.stream()
                .filter(NotificationRecipient::isPrimary)
                .findFirst()
                .orElse(emailRecipients.get(0))
                .getEmailAddress();
            recipients.put("EMAIL", email);
        }

        // Resolve recipients for SMS channel
        List<NotificationRecipient> smsRecipients = recipientResolutionService.resolveRecipients(
            tenantId, patientId, NotificationEntity.NotificationChannel.SMS, severity
        );
        if (!smsRecipients.isEmpty()) {
            // Use primary care provider's phone, or first recipient if no primary
            String phone = smsRecipients.stream()
                .filter(NotificationRecipient::isPrimary)
                .findFirst()
                .orElse(smsRecipients.get(0))
                .getPhoneNumber();
            recipients.put("SMS", phone);
        }

        log.debug("Resolved {} recipients for patient {} health score", recipients.size(), patientId);

        return recipients;
    }

    /**
     * Trigger notification for manual health score refresh
     * Used when clinicians manually request a health score recalculation
     *
     * @param tenantId Tenant ID
     * @param healthScore Recalculated health score
     */
    public void onManualHealthScoreRefresh(String tenantId, HealthScoreDTO healthScore) {
        log.info("Manual health score refresh triggered for patient {}", healthScore.getPatientId());
        // For manual refreshes, always send notification (WebSocket only)
        try {
            Map<String, String> recipients = getRecipients(tenantId, healthScore.getPatientId());

            HealthScoreNotificationRequest request = HealthScoreNotificationRequest.builder()
                    .healthScore(healthScore)
                    .tenantId(tenantId)
                    .patientId(healthScore.getPatientId())
                    .previousScore(healthScore.getPreviousScore())
                    .newScore(healthScore.getOverallScore())
                    .changeAmount(healthScore.getScoreDelta() != null ? healthScore.getScoreDelta() : 0.0)
                    .changeDirection(determineChangeDirection(
                            healthScore.getScoreDelta() != null ? healthScore.getScoreDelta() : 0.0))
                    .recipients(recipients)
                    .build();

            notificationService.sendNotification(request);
        } catch (Exception e) {
            log.error("Failed to send manual health score notification: {}", e.getMessage(), e);
        }
    }
}
