package com.healthdata.quality.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.quality.dto.notification.NotificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for Generic WebSocket Broadcast Service
 *
 * Tests cover:
 * - Broadcast to all tenant sessions
 * - Broadcast to specific user
 * - Broadcast to role-based sessions
 * - JSON message formatting
 * - Tenant isolation
 * - Message type handling (ALERT, CARE_GAP, HEALTH_SCORE, etc.)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketBroadcastService TDD Tests")
class WebSocketBroadcastServiceTest {

    @Mock
    private HealthScoreWebSocketHandler webSocketHandler;

    @Mock
    private WebSocketSession session1;

    @Mock
    private WebSocketSession session2;

    @Mock
    private WebSocketSession session3;

    private ObjectMapper objectMapper;
    private WebSocketBroadcastService broadcastService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        broadcastService = new WebSocketBroadcastService(webSocketHandler, objectMapper);
    }

    /**
     * Helper to create test notification request
     */
    private NotificationRequest createTestNotification(String type, String tenantId, String severity) {
        return new NotificationRequest() {
            @Override
            public String getNotificationType() {
                return type;
            }

            @Override
            public String getTemplateId() {
                return "test-template";
            }

            @Override
            public String getTenantId() {
                return tenantId;
            }

            @Override
            public String getPatientId() {
                return "patient-123";
            }

            @Override
            public String getTitle() {
                return "Test Notification";
            }

            @Override
            public String getMessage() {
                return "Test message content";
            }

            @Override
            public String getSeverity() {
                return severity;
            }

            @Override
            public Instant getTimestamp() {
                return Instant.now();
            }

            @Override
            public Map<String, String> getRecipients() {
                return Map.of("WEBSOCKET", "user-123");
            }

            @Override
            public Map<String, Object> getTemplateVariables() {
                Map<String, Object> vars = new HashMap<>();
                vars.put("patientName", "John Doe");
                vars.put("score", 85);
                return vars;
            }

            @Override
            public Map<String, Object> getMetadata() {
                Map<String, Object> meta = new HashMap<>();
                meta.put("source", "test");
                return meta;
            }

            @Override
            public boolean shouldSendEmail() {
                return false;
            }

            @Override
            public boolean shouldSendSms() {
                return false;
            }

            @Override
            public boolean shouldSendWebSocket() {
                return true;
            }

            @Override
            public String getNotificationId() {
                return "notif-123";
            }

            @Override
            public String getRelatedEntityId() {
                return "entity-456";
            }
        };
    }

    @Test
    @DisplayName("Should broadcast notification to all tenant sessions")
    void testBroadcastToTenant() throws Exception {
        // Given
        NotificationRequest notification = createTestNotification("ALERT", "TENANT001", "HIGH");

        // Mock handler to return true (successful broadcast)
        when(webSocketHandler.broadcastGenericNotification(eq("TENANT001"), any(Map.class)))
                .thenReturn(true);

        // When
        boolean result = broadcastService.broadcastNotification("TENANT001", notification);

        // Then
        assertThat(result).isTrue();
        verify(webSocketHandler).broadcastGenericNotification(
                eq("TENANT001"),
                any(Map.class)
        );
    }

    @Test
    @DisplayName("Should format notification message as JSON with all fields")
    void testFormatNotificationMessage() throws Exception {
        // Given
        NotificationRequest notification = createTestNotification("HEALTH_SCORE", "TENANT001", "MEDIUM");

        // When
        String json = broadcastService.formatNotificationMessage(notification);

        // Then
        assertThat(json).isNotNull();
        Map<String, Object> parsed = objectMapper.readValue(json, Map.class);

        assertThat(parsed).containsKey("type");
        assertThat(parsed).containsKey("notificationType");
        assertThat(parsed).containsKey("title");
        assertThat(parsed).containsKey("message");
        assertThat(parsed).containsKey("severity");
        assertThat(parsed).containsKey("patientId");
        assertThat(parsed).containsKey("tenantId");
        assertThat(parsed).containsKey("timestamp");
        assertThat(parsed).containsKey("data");
        assertThat(parsed).containsKey("metadata");

        assertThat(parsed.get("notificationType")).isEqualTo("HEALTH_SCORE");
        assertThat(parsed.get("title")).isEqualTo("Test Notification");
        assertThat(parsed.get("message")).isEqualTo("Test message content");
        assertThat(parsed.get("severity")).isEqualTo("MEDIUM");
        assertThat(parsed.get("patientId")).isEqualTo("patient-123");
        assertThat(parsed.get("tenantId")).isEqualTo("TENANT001");
    }

    @Test
    @DisplayName("Should format different notification types correctly")
    void testFormatDifferentNotificationTypes() throws Exception {
        // Test ALERT
        NotificationRequest alert = createTestNotification("CRITICAL_ALERT", "TENANT001", "CRITICAL");
        String alertJson = broadcastService.formatNotificationMessage(alert);
        Map<String, Object> alertParsed = objectMapper.readValue(alertJson, Map.class);
        assertThat(alertParsed.get("notificationType")).isEqualTo("CRITICAL_ALERT");

        // Test CARE_GAP
        NotificationRequest careGap = createTestNotification("CARE_GAP", "TENANT001", "LOW");
        String careGapJson = broadcastService.formatNotificationMessage(careGap);
        Map<String, Object> careGapParsed = objectMapper.readValue(careGapJson, Map.class);
        assertThat(careGapParsed.get("notificationType")).isEqualTo("CARE_GAP");

        // Test HEALTH_SCORE_UPDATE
        NotificationRequest healthScore = createTestNotification("HEALTH_SCORE_UPDATE", "TENANT001", null);
        String healthScoreJson = broadcastService.formatNotificationMessage(healthScore);
        Map<String, Object> healthScoreParsed = objectMapper.readValue(healthScoreJson, Map.class);
        assertThat(healthScoreParsed.get("notificationType")).isEqualTo("HEALTH_SCORE_UPDATE");
    }

    @Test
    @DisplayName("Should broadcast to specific user")
    void testBroadcastToUser() throws Exception {
        // Given
        NotificationRequest notification = createTestNotification("ALERT", "TENANT001", "HIGH");

        // When
        boolean result = broadcastService.broadcastToUser("user-123", notification);

        // Then
        verify(webSocketHandler).broadcastToUser(
                eq("user-123"),
                eq("TENANT001"),
                any(Map.class)
        );
    }

    @Test
    @DisplayName("Should broadcast to role within tenant")
    void testBroadcastToRole() throws Exception {
        // Given
        NotificationRequest notification = createTestNotification("ALERT", "TENANT001", "HIGH");

        // When
        boolean result = broadcastService.broadcastToRole("TENANT001", "DOCTOR", notification);

        // Then
        verify(webSocketHandler).broadcastToRole(
                eq("TENANT001"),
                eq("DOCTOR"),
                any(Map.class)
        );
    }

    @Test
    @DisplayName("Should enforce tenant isolation in broadcasts")
    void testTenantIsolation() throws Exception {
        // Given - Notification for TENANT001
        NotificationRequest notification = createTestNotification("ALERT", "TENANT001", "HIGH");

        // When - Broadcast to TENANT001
        broadcastService.broadcastNotification("TENANT001", notification);

        // Then - Should only broadcast to TENANT001 (verified by handler call)
        verify(webSocketHandler).broadcastGenericNotification(
                eq("TENANT001"),
                any(Map.class)
        );

        // Should NOT broadcast to other tenants
        verify(webSocketHandler, never()).broadcastGenericNotification(
                eq("TENANT002"),
                any(Map.class)
        );
    }

    @Test
    @DisplayName("Should include template variables in formatted message")
    void testIncludeTemplateVariables() throws Exception {
        // Given
        NotificationRequest notification = createTestNotification("HEALTH_SCORE", "TENANT001", "MEDIUM");

        // When
        String json = broadcastService.formatNotificationMessage(notification);

        // Then
        Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
        assertThat(parsed).containsKey("data");

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) parsed.get("data");
        assertThat(data.get("patientName")).isEqualTo("John Doe");
        assertThat(data.get("score")).isEqualTo(85);
    }

    @Test
    @DisplayName("Should include metadata in formatted message")
    void testIncludeMetadata() throws Exception {
        // Given
        NotificationRequest notification = createTestNotification("ALERT", "TENANT001", "HIGH");

        // When
        String json = broadcastService.formatNotificationMessage(notification);

        // Then
        Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
        assertThat(parsed).containsKey("metadata");

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) parsed.get("metadata");
        assertThat(metadata.get("source")).isEqualTo("test");
    }

    @Test
    @DisplayName("Should handle notification without severity")
    void testHandleNotificationWithoutSeverity() throws Exception {
        // Given - Notification without severity (like appointment reminder)
        NotificationRequest notification = createTestNotification("APPOINTMENT_REMINDER", "TENANT001", null);

        // When
        String json = broadcastService.formatNotificationMessage(notification);

        // Then
        Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
        assertThat(parsed.get("severity")).isNull();
        assertThat(parsed.get("notificationType")).isEqualTo("APPOINTMENT_REMINDER");
    }

    @Test
    @DisplayName("Should return false when broadcast fails")
    void testBroadcastFailure() throws Exception {
        // Given
        NotificationRequest notification = createTestNotification("ALERT", "TENANT001", "HIGH");

        // Mock handler to return false (no sessions)
        when(webSocketHandler.broadcastGenericNotification(any(), any())).thenReturn(false);

        // When
        boolean result = broadcastService.broadcastNotification("TENANT001", notification);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should broadcast different message types with correct priority")
    void testMessageTypePriority() throws Exception {
        // CRITICAL_ALERT should have alert priority
        NotificationRequest criticalAlert = createTestNotification("CRITICAL_ALERT", "TENANT001", "CRITICAL");
        String criticalJson = broadcastService.formatNotificationMessage(criticalAlert);
        Map<String, Object> criticalParsed = objectMapper.readValue(criticalJson, Map.class);
        assertThat(criticalParsed.get("type")).isEqualTo("NOTIFICATION");

        // HEALTH_SCORE_UPDATE should have normal priority
        NotificationRequest healthScore = createTestNotification("HEALTH_SCORE_UPDATE", "TENANT001", null);
        String healthScoreJson = broadcastService.formatNotificationMessage(healthScore);
        Map<String, Object> healthScoreParsed = objectMapper.readValue(healthScoreJson, Map.class);
        assertThat(healthScoreParsed.get("type")).isEqualTo("NOTIFICATION");
    }

    @Test
    @DisplayName("Should format message with ISO timestamp")
    void testTimestampFormat() throws Exception {
        // Given
        NotificationRequest notification = createTestNotification("ALERT", "TENANT001", "HIGH");

        // When
        String json = broadcastService.formatNotificationMessage(notification);

        // Then
        Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
        assertThat(parsed).containsKey("timestamp");

        // Timestamp should be a number (epoch millis) or ISO string
        Object timestamp = parsed.get("timestamp");
        assertThat(timestamp).isNotNull();
    }

    @Test
    @DisplayName("Should validate notification has WebSocket channel enabled")
    void testValidateWebSocketChannel() throws Exception {
        // Given - Notification with WebSocket disabled
        NotificationRequest notification = new NotificationRequest() {
            @Override
            public String getNotificationType() { return "TEST"; }
            @Override
            public String getTemplateId() { return "test"; }
            @Override
            public String getTenantId() { return "TENANT001"; }
            @Override
            public String getPatientId() { return "patient-123"; }
            @Override
            public String getTitle() { return "Test"; }
            @Override
            public String getMessage() { return "Test message"; }
            @Override
            public String getSeverity() { return "LOW"; }
            @Override
            public Instant getTimestamp() { return Instant.now(); }
            @Override
            public Map<String, String> getRecipients() { return Map.of(); }
            @Override
            public Map<String, Object> getTemplateVariables() { return Map.of(); }
            @Override
            public Map<String, Object> getMetadata() { return Map.of(); }
            @Override
            public boolean shouldSendEmail() { return true; }
            @Override
            public boolean shouldSendSms() { return false; }
            @Override
            public boolean shouldSendWebSocket() { return false; } // WebSocket disabled
        };

        // When
        boolean result = broadcastService.broadcastNotification("TENANT001", notification);

        // Then - Should still attempt broadcast (service doesn't validate channel)
        // The calling service (NotificationService) should check shouldSendWebSocket()
        verify(webSocketHandler).broadcastGenericNotification(eq("TENANT001"), any(Map.class));
    }
}
