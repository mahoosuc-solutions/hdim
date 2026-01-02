package com.healthdata.agent.tool.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.agent.client.NotificationServiceClient;
import com.healthdata.agent.core.AgentContext;
import com.healthdata.agent.tool.Tool.ToolResult;
import com.healthdata.agent.tool.Tool.ValidationResult;
import com.healthdata.agent.tool.ToolDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationTool Tests")
class NotificationToolTest {

    @Mock
    private NotificationServiceClient notificationClient;

    private NotificationTool notificationTool;
    private ObjectMapper objectMapper;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final String SESSION_ID = "session-789";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        notificationTool = new NotificationTool(notificationClient, objectMapper);
    }

    private AgentContext createContext() {
        return createContext(true);
    }

    private AgentContext createContext(boolean aiDataSharingConsented) {
        return AgentContext.builder()
            .tenantId(TENANT_ID)
            .userId(USER_ID)
            .sessionId(SESSION_ID)
            .correlationId("corr-001")
            .agentId("agent-001")
            .agentType("clinical-assistant")
            .roles(Set.of("CLINICAL_USER"))
            .aiDataSharingConsented(aiDataSharingConsented)
            .build();
    }

    @Nested
    @DisplayName("Tool Definition Tests")
    class DefinitionTests {

        @Test
        @DisplayName("should have correct tool name")
        void hasCorrectName() {
            assertThat(notificationTool.getName()).isEqualTo("send_notification");
        }

        @Test
        @DisplayName("should have correct category")
        void hasCorrectCategory() {
            ToolDefinition definition = notificationTool.getDefinition();
            assertThat(definition.getCategory()).isEqualTo(ToolDefinition.ToolCategory.NOTIFICATION);
        }

        @Test
        @DisplayName("should not require approval")
        void doesNotRequireApproval() {
            ToolDefinition definition = notificationTool.getDefinition();
            assertThat(definition.isRequiresApproval()).isFalse();
        }

        @Test
        @DisplayName("should define required parameters")
        void hasRequiredParams() {
            ToolDefinition definition = notificationTool.getDefinition();
            assertThat(definition.getRequiredParams()).containsExactlyInAnyOrder(
                "channel", "recipientType", "recipientId"
            );
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("should validate successfully with all required fields")
        void validateSuccessWithAllFields() {
            Map<String, Object> args = Map.of(
                "channel", "IN_APP",
                "recipientType", "USER",
                "recipientId", "user-001",
                "message", "Test notification"
            );

            ValidationResult result = notificationTool.validate(args);

            assertThat(result.isValid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("should fail validation when channel is missing")
        void failValidationMissingChannel() {
            Map<String, Object> args = Map.of(
                "recipientType", "USER",
                "recipientId", "user-001",
                "message", "Test notification"
            );

            ValidationResult result = notificationTool.validate(args);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).anyMatch(e -> e.contains("channel"));
        }

        @Test
        @DisplayName("should fail validation when recipientType is missing")
        void failValidationMissingRecipientType() {
            Map<String, Object> args = Map.of(
                "channel", "EMAIL",
                "recipientId", "user-001",
                "message", "Test notification"
            );

            ValidationResult result = notificationTool.validate(args);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).anyMatch(e -> e.contains("recipientType"));
        }

        @Test
        @DisplayName("should fail validation when both message and templateId are missing")
        void failValidationNoMessageOrTemplate() {
            Map<String, Object> args = Map.of(
                "channel", "EMAIL",
                "recipientType", "USER",
                "recipientId", "user-001"
            );

            ValidationResult result = notificationTool.validate(args);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).anyMatch(e -> e.contains("message") || e.contains("templateId"));
        }

        @Test
        @DisplayName("should validate successfully with templateId instead of message")
        void validateSuccessWithTemplate() {
            Map<String, Object> args = Map.of(
                "channel", "EMAIL",
                "recipientType", "USER",
                "recipientId", "user-001",
                "templateId", "clinical-alert"
            );

            ValidationResult result = notificationTool.validate(args);

            assertThat(result.isValid()).isTrue();
        }
    }

    @Nested
    @DisplayName("Availability Tests")
    class AvailabilityTests {

        @Test
        @DisplayName("should be available when tenant and user are set")
        void availableWithTenantAndUser() {
            AgentContext context = createContext();
            assertThat(notificationTool.isAvailable(context)).isTrue();
        }

        @Test
        @DisplayName("should not be available without tenant ID")
        void notAvailableWithoutTenant() {
            AgentContext context = AgentContext.builder()
                .userId(USER_ID)
                .sessionId(SESSION_ID)
                .build();

            assertThat(notificationTool.isAvailable(context)).isFalse();
        }

        @Test
        @DisplayName("should not be available without user ID")
        void notAvailableWithoutUser() {
            AgentContext context = AgentContext.builder()
                .tenantId(TENANT_ID)
                .sessionId(SESSION_ID)
                .build();

            assertThat(notificationTool.isAvailable(context)).isFalse();
        }
    }

    @Nested
    @DisplayName("Execution Tests - User Notifications")
    class UserNotificationTests {

        @Test
        @DisplayName("should send IN_APP notification to user successfully")
        void sendInAppNotification() {
            // Given
            Map<String, Object> args = new HashMap<>();
            args.put("channel", "IN_APP");
            args.put("recipientType", "USER");
            args.put("recipientId", "user-001");
            args.put("subject", "Care Gap Alert");
            args.put("message", "You have 3 patients with care gaps");
            args.put("priority", "HIGH");

            UUID notificationId = UUID.randomUUID();
            when(notificationClient.sendNotification(eq(TENANT_ID), any()))
                .thenReturn(Map.of("id", notificationId.toString(), "status", "PENDING"));

            // When
            ToolResult toolResult = notificationTool.execute(args, createContext()).block();

            // Then
            assertThat(toolResult.success()).isTrue();
            assertThat(toolResult.content()).contains("successfully");
            assertThat(toolResult.content()).contains("IN_APP");
            assertThat(toolResult.data()).containsEntry("channel", "IN_APP");
            assertThat(toolResult.data()).containsEntry("status", "PENDING");

            // Verify notification structure
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> notificationCaptor = ArgumentCaptor.forClass(Map.class);
            verify(notificationClient).sendNotification(eq(TENANT_ID), notificationCaptor.capture());

            Map<String, Object> sentNotification = notificationCaptor.getValue();
            assertThat(sentNotification).containsEntry("channel", "IN_APP");
            assertThat(sentNotification).containsEntry("recipientId", "user-001");
            assertThat(sentNotification).containsEntry("subject", "Care Gap Alert");
            assertThat(sentNotification).containsEntry("body", "You have 3 patients with care gaps");
            assertThat(sentNotification).containsEntry("priority", "HIGH");
        }

        @Test
        @DisplayName("should send EMAIL notification successfully")
        void sendEmailNotification() {
            // Given
            Map<String, Object> args = new HashMap<>();
            args.put("channel", "EMAIL");
            args.put("recipientType", "USER");
            args.put("recipientId", "user-001");
            args.put("subject", "Weekly Quality Report");
            args.put("message", "Your weekly quality report is ready");

            when(notificationClient.sendNotification(eq(TENANT_ID), any()))
                .thenReturn(Map.of("id", "notif-123", "status", "SENT"));

            // When
            ToolResult toolResult = notificationTool.execute(args, createContext()).block();

            // Then
            assertThat(toolResult.success()).isTrue();
            assertThat(toolResult.data()).containsEntry("channel", "EMAIL");
            assertThat(toolResult.data()).containsEntry("status", "SENT");
        }
    }

    @Nested
    @DisplayName("Execution Tests - Patient Notifications")
    class PatientNotificationTests {

        @Test
        @DisplayName("should send notification to patient with consent")
        void sendPatientNotificationWithConsent() {
            // Given
            Map<String, Object> args = new HashMap<>();
            args.put("channel", "SMS");
            args.put("recipientType", "PATIENT");
            args.put("recipientId", "patient-001");
            args.put("message", "Reminder: Your A1C test is due");

            when(notificationClient.sendNotification(eq(TENANT_ID), any()))
                .thenReturn(Map.of("id", "notif-456", "status", "SENT"));

            // When
            ToolResult toolResult = notificationTool.execute(args, createContext(true)).block();

            // Then
            assertThat(toolResult.success()).isTrue();
            assertThat(toolResult.data()).containsEntry("channel", "SMS");

            verify(notificationClient).sendNotification(eq(TENANT_ID), any());
        }

        @Test
        @DisplayName("should reject patient notification without consent")
        void rejectPatientNotificationWithoutConsent() {
            // Given
            Map<String, Object> args = new HashMap<>();
            args.put("channel", "SMS");
            args.put("recipientType", "PATIENT");
            args.put("recipientId", "patient-001");
            args.put("message", "Reminder message");

            // When
            ToolResult toolResult = notificationTool.execute(args, createContext(false)).block();

            // Then
            assertThat(toolResult.success()).isFalse();
            assertThat(toolResult.errorMessage()).contains("not consented");
            assertThat(toolResult.errorMessage()).contains("SMS");

            verifyNoInteractions(notificationClient);
        }
    }

    @Nested
    @DisplayName("Execution Tests - Care Team Alerts")
    class CareTeamAlertTests {

        @Test
        @DisplayName("should map CARE_TEAM_ALERT to IN_APP with URGENT priority")
        void careTeamAlertMapping() {
            // Given
            Map<String, Object> args = new HashMap<>();
            args.put("channel", "CARE_TEAM_ALERT");
            args.put("recipientType", "CARE_TEAM");
            args.put("recipientId", "team-001");
            args.put("subject", "Critical Patient Alert");
            args.put("message", "Patient vitals are critical");
            args.put("priority", "NORMAL"); // Should be overridden to URGENT

            when(notificationClient.sendNotification(eq(TENANT_ID), any()))
                .thenReturn(Map.of("id", "notif-789", "status", "PENDING"));

            // When
            ToolResult toolResult = notificationTool.execute(args, createContext()).block();

            // Then
            assertThat(toolResult.success()).isTrue();

            // Verify channel was mapped
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> notificationCaptor = ArgumentCaptor.forClass(Map.class);
            verify(notificationClient).sendNotification(eq(TENANT_ID), notificationCaptor.capture());

            Map<String, Object> sentNotification = notificationCaptor.getValue();
            assertThat(sentNotification).containsEntry("channel", "IN_APP");
            assertThat(sentNotification).containsEntry("priority", "URGENT");
        }
    }

    @Nested
    @DisplayName("Execution Tests - Templates")
    class TemplateTests {

        @Test
        @DisplayName("should send notification using template")
        void sendWithTemplate() {
            // Given
            Map<String, Object> args = new HashMap<>();
            args.put("channel", "EMAIL");
            args.put("recipientType", "USER");
            args.put("recipientId", "user-001");
            args.put("templateId", "clinical-alert");
            args.put("templateVariables", Map.of(
                "patientName", "John Doe",
                "alertType", "A1C Out of Range"
            ));

            when(notificationClient.sendNotification(eq(TENANT_ID), any()))
                .thenReturn(Map.of("id", "notif-template", "status", "PENDING"));

            // When
            ToolResult toolResult = notificationTool.execute(args, createContext()).block();

            // Then
            assertThat(toolResult.success()).isTrue();

            // Verify template was sent correctly
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> notificationCaptor = ArgumentCaptor.forClass(Map.class);
            verify(notificationClient).sendNotification(eq(TENANT_ID), notificationCaptor.capture());

            Map<String, Object> sentNotification = notificationCaptor.getValue();
            assertThat(sentNotification).containsEntry("templateCode", "clinical-alert");
            assertThat(sentNotification).containsKey("variables");
            assertThat(sentNotification).doesNotContainKey("body");
        }
    }

    @Nested
    @DisplayName("Execution Tests - Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should return error when message and template are both missing")
        void errorWhenNoMessageOrTemplate() {
            // Given
            Map<String, Object> args = new HashMap<>();
            args.put("channel", "IN_APP");
            args.put("recipientType", "USER");
            args.put("recipientId", "user-001");

            // When
            ToolResult toolResult = notificationTool.execute(args, createContext()).block();

            // Then
            assertThat(toolResult.success()).isFalse();
            assertThat(toolResult.errorMessage()).contains("message or templateId must be provided");

            verifyNoInteractions(notificationClient);
        }

        @Test
        @DisplayName("should handle notification service failure")
        void handleServiceFailure() {
            // Given
            Map<String, Object> args = new HashMap<>();
            args.put("channel", "EMAIL");
            args.put("recipientType", "USER");
            args.put("recipientId", "user-001");
            args.put("message", "Test message");

            when(notificationClient.sendNotification(eq(TENANT_ID), any()))
                .thenThrow(new RuntimeException("Service unavailable"));

            // When
            ToolResult toolResult = notificationTool.execute(args, createContext()).block();

            // Then
            assertThat(toolResult.success()).isFalse();
            assertThat(toolResult.errorMessage()).contains("Failed to send notification");
            assertThat(toolResult.errorMessage()).contains("Service unavailable");
        }
    }

    @Nested
    @DisplayName("Metadata Tests")
    class MetadataTests {

        @Test
        @DisplayName("should include agent metadata in notification")
        void includeAgentMetadata() {
            // Given
            Map<String, Object> args = new HashMap<>();
            args.put("channel", "IN_APP");
            args.put("recipientType", "USER");
            args.put("recipientId", "user-001");
            args.put("message", "Test message");
            args.put("actionUrl", "/patients/123/care-gaps");
            args.put("metadata", Map.of("customKey", "customValue"));

            when(notificationClient.sendNotification(eq(TENANT_ID), any()))
                .thenReturn(Map.of("id", "notif-meta", "status", "PENDING"));

            // When
            ToolResult toolResult = notificationTool.execute(args, createContext()).block();
            assertThat(toolResult.success()).isTrue();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> notificationCaptor = ArgumentCaptor.forClass(Map.class);
            verify(notificationClient).sendNotification(eq(TENANT_ID), notificationCaptor.capture());

            Map<String, Object> sentNotification = notificationCaptor.getValue();
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) sentNotification.get("metadata");

            assertThat(metadata).containsEntry("source", "AI_AGENT");
            assertThat(metadata).containsEntry("agentId", "agent-001");
            assertThat(metadata).containsEntry("sessionId", SESSION_ID);
            assertThat(metadata).containsEntry("recipientType", "USER");
            assertThat(metadata).containsEntry("actionUrl", "/patients/123/care-gaps");
            assertThat(metadata).containsEntry("customKey", "customValue");
        }

        @Test
        @DisplayName("should include correlation ID for tracing")
        void includeCorrelationId() {
            // Given
            Map<String, Object> args = new HashMap<>();
            args.put("channel", "IN_APP");
            args.put("recipientType", "USER");
            args.put("recipientId", "user-001");
            args.put("message", "Test message");

            when(notificationClient.sendNotification(eq(TENANT_ID), any()))
                .thenReturn(Map.of("id", "notif-corr", "status", "PENDING"));

            // When
            ToolResult toolResult = notificationTool.execute(args, createContext()).block();
            assertThat(toolResult.success()).isTrue();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> notificationCaptor = ArgumentCaptor.forClass(Map.class);
            verify(notificationClient).sendNotification(eq(TENANT_ID), notificationCaptor.capture());

            Map<String, Object> sentNotification = notificationCaptor.getValue();
            assertThat(sentNotification).containsEntry("correlationId", "corr-001");
        }
    }
}
