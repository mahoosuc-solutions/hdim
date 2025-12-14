package com.healthdata.agent.tool.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.agent.core.AgentContext;
import com.healthdata.agent.tool.Tool;
import com.healthdata.agent.tool.ToolDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tool for publishing events to the event bus (Kafka).
 * Enables AI agents to trigger workflows, notifications, and integrations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisherTool implements Tool {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final ToolDefinition DEFINITION = ToolDefinition.builder()
        .name("publish_event")
        .description("""
            Publish events to the healthcare event bus to trigger workflows,
            notifications, and integrations. Events are processed asynchronously
            by downstream services.

            Use this tool to:
            - Trigger care gap outreach workflows
            - Notify care teams of clinical findings
            - Start quality improvement actions
            - Request follow-up tasks
            - Alert on urgent findings (requires approval)
            """)
        .inputSchema(Map.of(
            "type", "object",
            "properties", Map.of(
                "eventType", Map.of(
                    "type", "string",
                    "description", "Type of event to publish",
                    "enum", List.of(
                        "CARE_GAP_IDENTIFIED",
                        "CARE_GAP_OUTREACH_REQUESTED",
                        "CLINICAL_FINDING",
                        "QUALITY_MEASURE_ALERT",
                        "FOLLOW_UP_REQUESTED",
                        "CARE_TEAM_NOTIFICATION",
                        "PATIENT_ENGAGEMENT_TRIGGER",
                        "RISK_SCORE_CHANGE",
                        "DOCUMENTATION_REQUIRED"
                    )
                ),
                "patientId", Map.of(
                    "type", "string",
                    "description", "Patient ID associated with the event"
                ),
                "priority", Map.of(
                    "type", "string",
                    "description", "Event priority level",
                    "enum", List.of("LOW", "NORMAL", "HIGH", "URGENT"),
                    "default", "NORMAL"
                ),
                "payload", Map.of(
                    "type", "object",
                    "description", "Event-specific data payload"
                ),
                "targetService", Map.of(
                    "type", "string",
                    "description", "Specific service to route event to (optional)"
                ),
                "scheduledFor", Map.of(
                    "type", "string",
                    "format", "date-time",
                    "description", "Schedule event for future delivery (ISO 8601 format)"
                )
            ),
            "required", List.of("eventType")
        ))
        .requiredParams(List.of("eventType"))
        .requiresApproval(false)
        .category(ToolDefinition.ToolCategory.NOTIFICATION)
        .build();

    @Override
    public ToolDefinition getDefinition() {
        return DEFINITION;
    }

    @Override
    public Mono<ToolResult> execute(Map<String, Object> arguments, AgentContext context) {
        String eventType = (String) arguments.get("eventType");
        String patientId = (String) arguments.getOrDefault("patientId", context.getPatientId());
        String priority = (String) arguments.getOrDefault("priority", "NORMAL");
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) arguments.getOrDefault("payload", Map.of());
        String targetService = (String) arguments.get("targetService");
        String scheduledFor = (String) arguments.get("scheduledFor");

        log.info("Publishing event: type={}, patientId={}, priority={}, tenant={}",
            eventType, patientId, priority, context.getTenantId());

        // Check if URGENT priority requires approval
        if ("URGENT".equals(priority) && getDefinition().isRequiresApproval()) {
            return Mono.just(ToolResult.error(
                "URGENT priority events require human approval. Please confirm this action."));
        }

        return Mono.fromCallable(() -> {
            try {
                // Build event envelope
                Map<String, Object> event = buildEvent(
                    eventType, patientId, priority, payload,
                    targetService, scheduledFor, context
                );

                String eventJson = objectMapper.writeValueAsString(event);
                String eventId = (String) event.get("eventId");

                // Determine topic based on event type and priority
                String topic = determineEventTopic(eventType, priority);

                // Publish to Kafka
                kafkaTemplate.send(topic, patientId != null ? patientId : eventId, eventJson)
                    .get(); // Block for confirmation

                log.info("Event published: eventId={}, topic={}", eventId, topic);

                return ToolResult.success(
                    String.format("Event %s published successfully (ID: %s) to topic %s",
                        eventType, eventId, topic),
                    Map.of("eventId", eventId, "topic", topic)
                );

            } catch (Exception e) {
                log.error("Failed to publish event: {}", e.getMessage(), e);
                return ToolResult.error("Failed to publish event: " + e.getMessage());
            }
        });
    }

    @Override
    public ValidationResult validate(Map<String, Object> arguments) {
        List<String> errors = new java.util.ArrayList<>();

        String eventType = (String) arguments.get("eventType");
        if (eventType == null || eventType.isBlank()) {
            errors.add("eventType is required");
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
    }

    @Override
    public boolean isAvailable(AgentContext context) {
        return context.getTenantId() != null;
    }

    private Map<String, Object> buildEvent(
            String eventType,
            String patientId,
            String priority,
            Map<String, Object> payload,
            String targetService,
            String scheduledFor,
            AgentContext context) {

        Map<String, Object> event = new java.util.LinkedHashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", eventType);
        event.put("timestamp", Instant.now().toString());
        event.put("tenantId", context.getTenantId());
        event.put("userId", context.getUserId());
        event.put("correlationId", context.getCorrelationId());
        event.put("sessionId", context.getSessionId());
        event.put("agentId", context.getAgentId());

        if (patientId != null) {
            event.put("patientId", patientId);
        }

        event.put("priority", priority);
        event.put("payload", payload);

        if (targetService != null) {
            event.put("targetService", targetService);
        }

        if (scheduledFor != null) {
            event.put("scheduledFor", scheduledFor);
        }

        // Add source metadata
        event.put("source", Map.of(
            "type", "AI_AGENT",
            "agentType", context.getAgentType() != null ? context.getAgentType() : "unknown",
            "origin", context.getOrigin() != null ? context.getOrigin() : "agent-runtime"
        ));

        return event;
    }

    private String determineEventTopic(String eventType, String priority) {
        // Route urgent events to high-priority topic
        if ("URGENT".equals(priority)) {
            return "hdim.events.urgent";
        }

        // Route by event type
        return switch (eventType) {
            case "CARE_GAP_IDENTIFIED", "CARE_GAP_OUTREACH_REQUESTED" -> "hdim.events.care-gaps";
            case "CLINICAL_FINDING", "QUALITY_MEASURE_ALERT" -> "hdim.events.clinical";
            case "FOLLOW_UP_REQUESTED", "DOCUMENTATION_REQUIRED" -> "hdim.events.tasks";
            case "CARE_TEAM_NOTIFICATION", "PATIENT_ENGAGEMENT_TRIGGER" -> "hdim.events.notifications";
            case "RISK_SCORE_CHANGE" -> "hdim.events.risk";
            default -> "hdim.events.general";
        };
    }
}
