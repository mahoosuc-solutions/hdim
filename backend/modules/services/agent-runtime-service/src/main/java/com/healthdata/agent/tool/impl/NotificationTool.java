package com.healthdata.agent.tool.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.agent.client.NotificationServiceClient;
import com.healthdata.agent.core.AgentContext;
import com.healthdata.agent.tool.Tool;
import com.healthdata.agent.tool.ToolDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Map.entry;

/**
 * Tool for sending notifications via multiple channels.
 * Supports in-app, email, SMS, push notifications, and care team alerts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationTool implements Tool {

    private final NotificationServiceClient notificationClient;
    private final ObjectMapper objectMapper;

    private static final ToolDefinition DEFINITION = ToolDefinition.builder()
        .name("send_notification")
        .description("""
            Send notifications to users, care teams, or patients through various channels.
            Use this tool to alert staff about clinical findings, remind patients about
            care gaps, or notify care teams about important updates.

            Channels:
            - IN_APP: Dashboard notifications for staff
            - EMAIL: Formal communications (requires consent for patients)
            - SMS: Quick alerts (requires consent for patients)
            - PUSH: Mobile app notifications
            - CARE_TEAM_ALERT: Urgent alerts to entire care team

            Note: Patient notifications require prior consent verification.
            """)
        .inputSchema(Map.of(
            "type", "object",
            "properties", Map.ofEntries(
                entry("channel", Map.of(
                    "type", "string",
                    "description", "Notification delivery channel",
                    "enum", List.of("IN_APP", "EMAIL", "SMS", "PUSH", "CARE_TEAM_ALERT")
                )),
                entry("recipientType", Map.of(
                    "type", "string",
                    "description", "Type of recipient",
                    "enum", List.of("USER", "PATIENT", "CARE_TEAM", "ROLE")
                )),
                entry("recipientId", Map.of(
                    "type", "string",
                    "description", "Recipient identifier (user ID, patient ID, or role name)"
                )),
                entry("subject", Map.of(
                    "type", "string",
                    "description", "Notification subject/title"
                )),
                entry("message", Map.of(
                    "type", "string",
                    "description", "Notification message content"
                )),
                entry("priority", Map.of(
                    "type", "string",
                    "description", "Notification priority",
                    "enum", List.of("LOW", "NORMAL", "HIGH", "URGENT"),
                    "default", "NORMAL"
                )),
                entry("templateId", Map.of(
                    "type", "string",
                    "description", "Use a predefined template ID instead of custom message"
                )),
                entry("templateVariables", Map.of(
                    "type", "object",
                    "description", "Variables to populate in the template"
                )),
                entry("actionUrl", Map.of(
                    "type", "string",
                    "description", "URL for notification action button"
                )),
                entry("expiresIn", Map.of(
                    "type", "integer",
                    "description", "Minutes until notification expires (0 = no expiry)",
                    "default", 0
                )),
                entry("metadata", Map.of(
                    "type", "object",
                    "description", "Additional metadata for tracking"
                ))
            ),
            "required", List.of("channel", "recipientType", "recipientId")
        ))
        .requiredParams(List.of("channel", "recipientType", "recipientId"))
        .requiresApproval(false)
        .category(ToolDefinition.ToolCategory.NOTIFICATION)
        .build();

    @Override
    public ToolDefinition getDefinition() {
        return DEFINITION;
    }

    @Override
    public Mono<ToolResult> execute(Map<String, Object> arguments, AgentContext context) {
        String channel = (String) arguments.get("channel");
        String recipientType = (String) arguments.get("recipientType");
        String recipientId = (String) arguments.get("recipientId");
        String subject = (String) arguments.get("subject");
        String message = (String) arguments.get("message");
        String priority = (String) arguments.getOrDefault("priority", "NORMAL");
        String templateId = (String) arguments.get("templateId");
        @SuppressWarnings("unchecked")
        Map<String, Object> templateVariables = (Map<String, Object>) arguments.get("templateVariables");
        String actionUrl = (String) arguments.get("actionUrl");
        int expiresIn = ((Number) arguments.getOrDefault("expiresIn", 0)).intValue();
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) arguments.getOrDefault("metadata", Map.of());

        log.info("Sending notification: channel={}, recipientType={}, recipientId={}, tenant={}",
            channel, recipientType, recipientId, context.getTenantId());

        // Validate that message or template is provided
        if (message == null && templateId == null) {
            return Mono.just(ToolResult.error("Either message or templateId must be provided"));
        }

        return Mono.fromCallable(() -> {
            try {
                // Build notification request
                Map<String, Object> notification = buildNotification(
                    channel, recipientType, recipientId, subject, message,
                    priority, templateId, templateVariables, actionUrl,
                    expiresIn, metadata, context
                );

                // For patient notifications, verify consent
                if ("PATIENT".equals(recipientType)) {
                    boolean hasConsent = verifyPatientConsent(recipientId, channel, context);
                    if (!hasConsent) {
                        return ToolResult.error(
                            String.format("Patient %s has not consented to %s notifications",
                                recipientId, channel));
                    }
                }

                // Send notification
                Map<String, Object> result = notificationClient.sendNotification(
                    context.getTenantId(),
                    notification
                );

                // Parse response - notification-service returns id (UUID) and status (enum name)
                String notificationId = result.get("id") != null
                    ? result.get("id").toString()
                    : (String) result.get("notificationId");
                String status = (String) result.getOrDefault("status", "PENDING");

                log.info("Notification sent: id={}, status={}, channel={}",
                    notificationId, status, channel);

                return ToolResult.success(
                    String.format("Notification sent successfully via %s (ID: %s, Status: %s)",
                        channel, notificationId, status),
                    Map.of(
                        "notificationId", notificationId != null ? notificationId : "unknown",
                        "status", status,
                        "channel", channel
                    )
                );

            } catch (Exception e) {
                log.error("Failed to send notification: {}", e.getMessage(), e);
                return ToolResult.error("Failed to send notification: " + e.getMessage());
            }
        });
    }

    @Override
    public ValidationResult validate(Map<String, Object> arguments) {
        List<String> errors = new java.util.ArrayList<>();

        String channel = (String) arguments.get("channel");
        if (channel == null || channel.isBlank()) {
            errors.add("channel is required");
        }

        String recipientType = (String) arguments.get("recipientType");
        if (recipientType == null || recipientType.isBlank()) {
            errors.add("recipientType is required");
        }

        String recipientId = (String) arguments.get("recipientId");
        if (recipientId == null || recipientId.isBlank()) {
            errors.add("recipientId is required");
        }

        String message = (String) arguments.get("message");
        String templateId = (String) arguments.get("templateId");
        if ((message == null || message.isBlank()) && (templateId == null || templateId.isBlank())) {
            errors.add("Either message or templateId must be provided");
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
    }

    @Override
    public boolean isAvailable(AgentContext context) {
        return context.getTenantId() != null && context.getUserId() != null;
    }

    private Map<String, Object> buildNotification(
            String channel,
            String recipientType,
            String recipientId,
            String subject,
            String message,
            String priority,
            String templateId,
            Map<String, Object> templateVariables,
            String actionUrl,
            int expiresIn,
            Map<String, Object> metadata,
            AgentContext context) {

        Map<String, Object> notification = new java.util.LinkedHashMap<>();

        // Map channel - notification-service supports: EMAIL, SMS, PUSH, IN_APP
        // CARE_TEAM_ALERT maps to IN_APP with high priority
        String mappedChannel = mapChannel(channel);
        notification.put("channel", mappedChannel);

        // Recipient info
        notification.put("recipientId", recipientId);

        // Priority (default to HIGH for CARE_TEAM_ALERT)
        String mappedPriority = "CARE_TEAM_ALERT".equals(channel) ? "URGENT" : priority;
        notification.put("priority", mappedPriority);

        if (subject != null) {
            notification.put("subject", subject);
        }

        // Template or direct message
        if (templateId != null) {
            notification.put("templateCode", templateId);
            if (templateVariables != null) {
                notification.put("variables", templateVariables);
            }
        } else {
            notification.put("body", message);
        }

        // Correlation ID for tracing
        notification.put("correlationId", context.getCorrelationId());

        // Build metadata
        Map<String, Object> metadataMap = new java.util.LinkedHashMap<>();
        metadataMap.put("source", "AI_AGENT");
        metadataMap.put("agentId", context.getAgentId() != null ? context.getAgentId() : "unknown");
        metadataMap.put("sessionId", context.getSessionId());
        metadataMap.put("recipientType", recipientType);

        if (actionUrl != null) {
            metadataMap.put("actionUrl", actionUrl);
        }

        if (expiresIn > 0) {
            metadataMap.put("expiresAt",
                java.time.Instant.now().plusSeconds(expiresIn * 60L).toString());
        }

        if (metadata != null && !metadata.isEmpty()) {
            metadataMap.putAll(metadata);
        }

        notification.put("metadata", metadataMap);

        return notification;
    }

    /**
     * Map tool channel to notification-service channel.
     * Notification service supports: EMAIL, SMS, PUSH, IN_APP
     */
    private String mapChannel(String channel) {
        return switch (channel) {
            case "CARE_TEAM_ALERT" -> "IN_APP";
            default -> channel;
        };
    }

    private boolean verifyPatientConsent(String patientId, String channel, AgentContext context) {
        try {
            // Check if AI data sharing is consented in context
            if (!context.isAiDataSharingConsented()) {
                return false;
            }

            // Additional channel-specific consent checks would be performed here
            // via consent service integration
            return true;

        } catch (Exception e) {
            log.warn("Failed to verify patient consent: {}", e.getMessage());
            return false;
        }
    }
}
