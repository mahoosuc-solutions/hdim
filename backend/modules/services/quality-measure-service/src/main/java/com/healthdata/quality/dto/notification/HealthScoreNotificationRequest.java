package com.healthdata.quality.dto.notification;

import com.healthdata.quality.dto.HealthScoreDTO;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Score Notification Request
 *
 * Used for real-time health score updates sent via WebSocket.
 * Notifies clinicians when a patient's health score changes significantly.
 *
 * Routing logic:
 * - WebSocket only (real-time updates to dashboard)
 * - No email or SMS (too frequent for these channels)
 */
@Builder
@Getter
public class HealthScoreNotificationRequest implements NotificationRequest {

    private final HealthScoreDTO healthScore;
    private final String tenantId;
    private final String patientId;
    private final Double previousScore;
    private final Double newScore;
    private final Double changeAmount;
    private final String changeDirection; // "INCREASED", "DECREASED", "STABLE"
    private final Map<String, String> recipients;

    @Override
    public String getNotificationType() {
        return "HEALTH_SCORE_UPDATE";
    }

    @Override
    public String getTemplateId() {
        return "health-score";
    }

    @Override
    public String getTitle() {
        if ("DECREASED".equals(changeDirection)) {
            return String.format("Health Score Decreased: %.1f → %.1f", previousScore, newScore);
        } else if ("INCREASED".equals(changeDirection)) {
            return String.format("Health Score Improved: %.1f → %.1f", previousScore, newScore);
        }
        return String.format("Health Score Update: %.1f", newScore);
    }

    @Override
    public String getMessage() {
        if (Math.abs(changeAmount) >= 10.0) {
            return String.format("Significant health score change detected for patient %s. " +
                    "Score changed from %.1f to %.1f (%.1f point %s). Immediate review recommended.",
                    patientId, previousScore, newScore, Math.abs(changeAmount),
                    changeAmount > 0 ? "increase" : "decrease");
        }
        return String.format("Health score updated for patient %s: %.1f (previous: %.1f)",
                patientId, newScore, previousScore);
    }

    @Override
    public String getSeverity() {
        // Determine severity based on score change magnitude
        double absChange = Math.abs(changeAmount);
        if (absChange >= 15.0) {
            return "HIGH";
        } else if (absChange >= 10.0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    @Override
    public Instant getTimestamp() {
        return healthScore != null && healthScore.getCalculatedAt() != null
                ? healthScore.getCalculatedAt()
                : Instant.now();
    }

    @Override
    public Map<String, Object> getTemplateVariables() {
        Map<String, Object> variables = new HashMap<>();

        // Patient information
        variables.put("patientId", patientId);
        variables.put("patientName", "Patient " + patientId); // TODO: Fetch from FHIR

        // Health score data
        variables.put("currentScore", String.format("%.1f", newScore));
        variables.put("previousScore", String.format("%.1f", previousScore));
        variables.put("changeAmount", String.format("%.1f", Math.abs(changeAmount)));
        variables.put("changeDirection", changeDirection);
        variables.put("changePercentage", String.format("%.1f%%",
                (changeAmount / previousScore) * 100));

        // Score component breakdown (if available)
        if (healthScore != null) {
            Map<String, Object> components = new HashMap<>();
            components.put("physical", healthScore.getPhysicalHealthScore());
            components.put("mental", healthScore.getMentalHealthScore());
            components.put("social", healthScore.getSocialDeterminantsScore());
            components.put("preventive", healthScore.getPreventiveCareScore());
            components.put("chronicDisease", healthScore.getChronicDiseaseScore());
            variables.put("components", components);
        }

        // Timestamp
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime calculatedTime = LocalDateTime.ofInstant(getTimestamp(), ZoneId.systemDefault());
        variables.put("timestamp", calculatedTime.format(formatter));

        // Action URL
        variables.put("actionUrl", "https://healthdata-in-motion.com/patients/" + patientId);

        // Facility name
        variables.put("facilityName", "HealthData Clinical System");

        return variables;
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("previousScore", previousScore);
        metadata.put("newScore", newScore);
        metadata.put("changeAmount", changeAmount);
        metadata.put("changeDirection", changeDirection);
        if (healthScore != null) {
            metadata.put("healthScoreId", healthScore.getId() != null ?
                    healthScore.getId().toString() : null);
            metadata.put("physicalHealthScore", healthScore.getPhysicalHealthScore());
            metadata.put("mentalHealthScore", healthScore.getMentalHealthScore());
            metadata.put("socialDeterminantsScore", healthScore.getSocialDeterminantsScore());
            metadata.put("preventiveCareScore", healthScore.getPreventiveCareScore());
            metadata.put("chronicDiseaseScore", healthScore.getChronicDiseaseScore());
        }
        return metadata;
    }

    @Override
    public boolean shouldSendEmail() {
        // Only send email for significant decreases (10+ points)
        return "DECREASED".equals(changeDirection) && Math.abs(changeAmount) >= 10.0;
    }

    @Override
    public boolean shouldSendSms() {
        // Only send SMS for critical decreases (15+ points)
        return "DECREASED".equals(changeDirection) && Math.abs(changeAmount) >= 15.0;
    }

    @Override
    public boolean shouldSendWebSocket() {
        // Always send health score updates via WebSocket for real-time monitoring
        return true;
    }

    @Override
    public String getNotificationId() {
        return healthScore != null && healthScore.getId() != null ?
                healthScore.getId().toString() : null;
    }

    @Override
    public String getRelatedEntityId() {
        return healthScore != null && healthScore.getId() != null ?
                healthScore.getId().toString() : null;
    }
}
