package com.healthdata.events.service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.healthdata.events.model.DLQExhaustionAlert;

import lombok.RequiredArgsConstructor;

/**
 * DLQ Alerting Service
 *
 * Handles alerting and notifications when DLQ events exhaust all retry attempts.
 * Provides email notifications, dashboard entries, and critical event escalation.
 */
@Service
@RequiredArgsConstructor
public class DLQAlertingService {

    private static final Logger log = LoggerFactory.getLogger(DLQAlertingService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
        .withZone(ZoneId.systemDefault());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Configuration - in production these would come from application.properties
    private static final String OPS_EMAIL = "ops-team@healthdata.com";
    private static final String CRITICAL_EMAIL_1 = "oncall-lead@healthdata.com";
    private static final String CRITICAL_EMAIL_2 = "tech-lead@healthdata.com";
    private static final String FROM_EMAIL = "dlq-alerts@healthdata.com";

    private final JavaMailSender mailSender;

    /**
     * Send exhaustion alert to operations team
     *
     * @param alert The DLQ exhaustion alert to send
     */
    public void sendExhaustionAlert(DLQExhaustionAlert alert) {
        try {
            log.info("Sending DLQ exhaustion alert for event: eventId={}, eventType={}, tenantId={}",
                alert.getEventId(), alert.getEventType(), alert.getTenantId());

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(FROM_EMAIL);
            message.setTo(OPS_EMAIL);
            message.setSubject(buildSubject(alert, false));
            message.setText(buildEmailBody(alert, false));

            mailSender.send(message);

            log.info("Successfully sent DLQ exhaustion alert for event: {}", alert.getEventId());

        } catch (Exception e) {
            log.error("Failed to send DLQ exhaustion alert for event: {}", alert.getEventId(), e);
            // Don't throw - alerting failures should not break the DLQ processing
        }
    }

    /**
     * Escalate critical failure to senior team members
     *
     * @param alert The DLQ exhaustion alert to escalate
     */
    public void escalateCriticalFailure(DLQExhaustionAlert alert) {
        try {
            log.warn("ESCALATING critical DLQ failure: eventId={}, eventType={}, patientId={}",
                alert.getEventId(), alert.getEventType(), alert.getAffectedPatientId());

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(FROM_EMAIL);
            message.setTo(CRITICAL_EMAIL_1, CRITICAL_EMAIL_2);
            message.setCc(OPS_EMAIL);
            message.setSubject(buildSubject(alert, true));
            message.setText(buildEmailBody(alert, true));

            mailSender.send(message);

            log.warn("Successfully escalated critical DLQ failure for event: {}", alert.getEventId());

        } catch (Exception e) {
            log.error("Failed to escalate critical DLQ failure for event: {}", alert.getEventId(), e);
            // Don't throw - alerting failures should not break the DLQ processing
        }
    }

    /**
     * Create dashboard entry for monitoring
     *
     * @param alert The DLQ exhaustion alert
     * @return JSON string for dashboard entry
     */
    public String createDashboardEntry(DLQExhaustionAlert alert) {
        try {
            log.debug("Creating dashboard entry for DLQ event: {}", alert.getEventId());

            ObjectNode entry = OBJECT_MAPPER.createObjectNode();
            entry.put("timestamp", System.currentTimeMillis());
            entry.put("eventId", alert.getEventId().toString());
            entry.put("dlqId", alert.getDlqId() != null ? alert.getDlqId().toString() : null);
            entry.put("eventType", alert.getEventType());
            entry.put("tenantId", alert.getTenantId());
            entry.put("patientId", alert.getAffectedPatientId());
            entry.put("retryCount", alert.getRetryCount());
            entry.put("severity", alert.isCritical() ? "CRITICAL" : "HIGH");
            entry.put("firstFailure", alert.getFirstFailureTimestamp() != null
                ? alert.getFirstFailureTimestamp().toEpochMilli() : null);
            entry.put("lastFailure", alert.getLastFailureTimestamp() != null
                ? alert.getLastFailureTimestamp().toEpochMilli() : null);
            entry.put("errorMessage", alert.getOriginalErrorMessage());
            entry.put("topic", alert.getTopic());
            entry.put("status", "EXHAUSTED");

            String json = OBJECT_MAPPER.writeValueAsString(entry);
            log.debug("Dashboard entry created for event: {}", alert.getEventId());
            return json;

        } catch (JsonProcessingException e) {
            log.error("Failed to create dashboard entry for event: {}", alert.getEventId(), e);
            return "{}";
        }
    }

    /**
     * Build email subject line
     */
    private String buildSubject(DLQExhaustionAlert alert, boolean critical) {
        if (critical) {
            return String.format("[CRITICAL] [URGENT] DLQ Event Exhausted - %s - Tenant: %s",
                alert.getEventType(), alert.getTenantId());
        }
        return String.format("DLQ Event Exhausted - %s - Tenant: %s",
            alert.getEventType(), alert.getTenantId());
    }

    /**
     * Build email body content
     */
    private String buildEmailBody(DLQExhaustionAlert alert, boolean critical) {
        StringBuilder body = new StringBuilder();

        if (critical) {
            body.append("=".repeat(80)).append("\n");
            body.append("CRITICAL ALERT - IMMEDIATE ACTION REQUIRED\n");
            body.append("=".repeat(80)).append("\n\n");
        }

        body.append("A DLQ event has exhausted all retry attempts and requires manual intervention.\n\n");

        body.append("EVENT DETAILS:\n");
        body.append("-".repeat(80)).append("\n");
        body.append(String.format("Event ID:          %s\n", alert.getEventId()));
        if (alert.getDlqId() != null) {
            body.append(String.format("DLQ ID:            %s\n", alert.getDlqId()));
        }
        body.append(String.format("Event Type:        %s\n", alert.getEventType()));
        body.append(String.format("Tenant:            %s\n", alert.getTenantId()));
        body.append(String.format("Topic:             %s\n", alert.getTopic() != null ? alert.getTopic() : "N/A"));
        body.append(String.format("Patient ID:        %s\n",
            alert.getAffectedPatientId() != null ? alert.getAffectedPatientId() : "N/A"));
        body.append(String.format("Retry Attempts:    %d\n", alert.getRetryCount()));
        body.append("\n");

        body.append("FAILURE TIMELINE:\n");
        body.append("-".repeat(80)).append("\n");
        if (alert.getFirstFailureTimestamp() != null) {
            body.append(String.format("First Failure:     %s\n",
                DATE_FORMATTER.format(alert.getFirstFailureTimestamp())));
        }
        if (alert.getLastFailureTimestamp() != null) {
            body.append(String.format("Last Failure:      %s\n",
                DATE_FORMATTER.format(alert.getLastFailureTimestamp())));
        }
        body.append("\n");

        body.append("ERROR DETAILS:\n");
        body.append("-".repeat(80)).append("\n");
        body.append(String.format("Error:             %s\n",
            alert.getOriginalErrorMessage() != null ? alert.getOriginalErrorMessage() : "N/A"));
        body.append("\n");

        if (alert.getStackTrace() != null && !alert.getStackTrace().isEmpty()) {
            body.append("STACK TRACE:\n");
            body.append("-".repeat(80)).append("\n");
            body.append(alert.getStackTrace()).append("\n\n");
        }

        body.append("RECOMMENDED ACTIONS:\n");
        body.append("-".repeat(80)).append("\n");
        body.append("1. Review the error message and stack trace\n");
        body.append("2. Check the DLQ dashboard for additional context\n");
        body.append("3. Investigate the root cause (database, external service, data quality, etc.)\n");
        body.append("4. Fix the underlying issue\n");
        body.append("5. Manually retry or discard the event in the DLQ management interface\n");

        if (critical) {
            body.append("\n");
            body.append("=".repeat(80)).append("\n");
            body.append("WARNING: This is a CRITICAL event type that may impact patient care.\n");
            body.append("Please prioritize investigation and resolution.\n");
            body.append("=".repeat(80)).append("\n");
        }

        body.append("\n");
        body.append("This is an automated message from the DLQ Retry Processor.\n");
        body.append("DLQ Management: https://dashboard.healthdata.com/dlq\n");

        return body.toString();
    }
}
