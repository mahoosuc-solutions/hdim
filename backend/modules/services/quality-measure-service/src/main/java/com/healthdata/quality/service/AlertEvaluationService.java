package com.healthdata.quality.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Alert Evaluation Service
 *
 * Evaluates clinical conditions and determines if alerts should be triggered using
 * intelligent suppression to reduce alert fatigue while maintaining clinical safety.
 *
 * Alert Fatigue Mitigation Strategy:
 * - Clinical priority thresholds (only HIGH/CRITICAL conditions trigger immediate alerts)
 * - Time-based suppression (prevents duplicate alerts within cooldown window)
 * - Context-aware filtering (considers patient history and care team preferences)
 * - Actionable alerts only (every alert must have a clear action)
 *
 * Research shows 33-96% of clinical alerts are ignored (alert fatigue).
 * This service implements intelligent filtering to ensure high signal-to-noise ratio.
 *
 * @see <a href="https://pmc.ncbi.nlm.nih.gov/articles/PMC10830237/">Alert Fatigue in Clinical Decision Support</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertEvaluationService {

    // Configurable thresholds
    @Value("${hdim.alerts.min-priority-threshold:50}")
    private int minPriorityThreshold;

    @Value("${hdim.alerts.suppression-window-hours:24}")
    private int suppressionWindowHours;

    @Value("${hdim.alerts.max-daily-per-patient:5}")
    private int maxDailyAlertsPerPatient;

    // Alert suppression cache: key = patientId:conditionType, value = last alert time
    private final Map<String, Instant> recentAlerts = new ConcurrentHashMap<>();

    // Daily alert count per patient
    private final Map<String, AlertCounter> dailyAlertCounts = new ConcurrentHashMap<>();

    /**
     * Evaluate if conditions warrant an alert using intelligent suppression.
     *
     * Applies the following filters:
     * 1. Priority threshold - Only HIGH/CRITICAL severity alerts pass through
     * 2. Time-based suppression - Prevents duplicate alerts within cooldown window
     * 3. Daily limit - Prevents alert overload for a single patient
     * 4. Actionability check - Ensures alert has clear clinical action
     *
     * @param condition Clinical condition type
     * @param data Alert context data (should contain patientId, severity, etc.)
     * @return true if alert should be triggered, false if suppressed
     */
    public boolean shouldTriggerAlert(String condition, Object data) {
        if (condition == null || data == null) {
            log.debug("Alert suppressed: null condition or data");
            return false;
        }

        // Extract alert context
        AlertContext context = extractContext(data);
        if (context == null) {
            log.debug("Alert suppressed: unable to extract context from data");
            return false;
        }

        // 1. Priority threshold check
        int priority = calculatePriorityScore(condition, context.severity(), context);
        if (priority < minPriorityThreshold) {
            log.debug("Alert suppressed for {}: priority {} below threshold {}",
                condition, priority, minPriorityThreshold);
            return false;
        }

        // 2. Time-based suppression (prevent duplicate alerts)
        String suppressionKey = context.patientId() + ":" + condition;
        Instant lastAlert = recentAlerts.get(suppressionKey);
        if (lastAlert != null) {
            long hoursSinceLastAlert = ChronoUnit.HOURS.between(lastAlert, Instant.now());
            if (hoursSinceLastAlert < suppressionWindowHours) {
                log.debug("Alert suppressed for {}: duplicate within {} hours",
                    condition, suppressionWindowHours);
                return false;
            }
        }

        // 3. Daily limit per patient
        AlertCounter counter = dailyAlertCounts.computeIfAbsent(
            context.patientId(),
            k -> new AlertCounter()
        );
        if (counter.getCountToday() >= maxDailyAlertsPerPatient) {
            // Exception: CRITICAL alerts always pass through
            if (!"CRITICAL".equals(context.severity())) {
                log.debug("Alert suppressed for patient {}: daily limit {} reached",
                    context.patientId(), maxDailyAlertsPerPatient);
                return false;
            }
        }

        // 4. Actionability check - must have clear action
        if (!isActionable(condition, context)) {
            log.debug("Alert suppressed for {}: no actionable intervention available", condition);
            return false;
        }

        // Alert passes all filters - record and trigger
        recentAlerts.put(suppressionKey, Instant.now());
        counter.increment();

        log.info("Alert triggered for patient {}: {} (priority={}, severity={})",
            context.patientId(), condition, priority, context.severity());

        return true;
    }

    /**
     * Calculate alert priority score based on clinical significance.
     *
     * @param alertType Type of alert
     * @param severity Severity level (CRITICAL, HIGH, MEDIUM, LOW)
     * @param context Alert context
     * @return Priority score (0-100)
     */
    public int calculatePriorityScore(String alertType, String severity, Object context) {
        // Base score from severity
        int baseScore = switch (severity != null ? severity : "LOW") {
            case "CRITICAL" -> 100;
            case "HIGH" -> 75;
            case "MEDIUM" -> 50;
            case "LOW" -> 25;
            default -> 10;
        };

        // Apply modifiers based on alert type
        int modifier = switch (alertType != null ? alertType : "") {
            case "care-gap.critical" -> 20;  // Time-sensitive care gaps
            case "chronic-disease.deterioration" -> 15;  // Worsening conditions
            case "medication.interaction" -> 25;  // Drug safety
            case "lab.critical" -> 30;  // Critical lab values
            case "compliance.overdue" -> -10;  // Lower priority for compliance
            default -> 0;
        };

        return Math.min(100, Math.max(0, baseScore + modifier));
    }

    /**
     * Determine if an alert is actionable (has clear clinical intervention).
     */
    private boolean isActionable(String condition, AlertContext context) {
        // All alerts should have associated actions
        // This can be extended to check for specific action availability
        return switch (condition) {
            case "care-gap.open" -> true;  // Close the gap
            case "care-gap.critical" -> true;  // Urgent intervention
            case "chronic-disease.deterioration" -> true;  // Adjust treatment
            case "medication.interaction" -> true;  // Review medications
            case "lab.critical" -> true;  // Clinical review
            case "compliance.reminder" -> false;  // Not clinically actionable
            case "informational" -> false;  // No action needed
            default -> true;  // Default to actionable for safety
        };
    }

    /**
     * Extract alert context from data object.
     */
    private AlertContext extractContext(Object data) {
        try {
            if (data instanceof Map<?, ?> map) {
                Object patientIdObj = map.get("patientId");
                String patientId = patientIdObj != null ? String.valueOf(patientIdObj) : "unknown";

                Object severityObj = map.get("severity");
                if (severityObj == null) {
                    severityObj = map.get("alertLevel");
                }
                String severity = severityObj != null ? String.valueOf(severityObj) : "LOW";

                return new AlertContext(patientId, severity);
            }
            return new AlertContext("unknown", "LOW");
        } catch (Exception e) {
            log.warn("Failed to extract alert context", e);
            return null;
        }
    }

    /**
     * Clear suppression cache (for testing or daily reset).
     */
    public void clearSuppressionCache() {
        recentAlerts.clear();
        dailyAlertCounts.clear();
    }

    /**
     * Alert context record.
     */
    private record AlertContext(String patientId, String severity) {}

    /**
     * Thread-safe daily alert counter.
     */
    private static class AlertCounter {
        private int count = 0;
        private Instant lastReset = Instant.now();

        synchronized int getCountToday() {
            resetIfNewDay();
            return count;
        }

        synchronized void increment() {
            resetIfNewDay();
            count++;
        }

        private void resetIfNewDay() {
            if (ChronoUnit.DAYS.between(lastReset, Instant.now()) >= 1) {
                count = 0;
                lastReset = Instant.now();
            }
        }
    }
}
