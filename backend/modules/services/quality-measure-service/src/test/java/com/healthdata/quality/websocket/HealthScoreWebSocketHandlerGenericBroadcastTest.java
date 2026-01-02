package com.healthdata.quality.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for Generic Notification Broadcasting in HealthScoreWebSocketHandler
 *
 * Tests cover the new methods:
 * - broadcastGenericNotification()
 * - broadcastToUser()
 * - broadcastToRole()
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HealthScoreWebSocketHandler - Generic Broadcast Tests")
class HealthScoreWebSocketHandlerGenericBroadcastTest {

    @Mock
    private WebSocketSession session1;

    @Mock
    private WebSocketSession session2;

    @Mock
    private WebSocketSession session3;

    @Mock
    private AuditLoggingInterceptor auditLoggingInterceptor;

    @Mock
    private SessionTimeoutManager sessionTimeoutManager;

    private ObjectMapper objectMapper;
    private HealthScoreWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new HealthScoreWebSocketHandler(objectMapper, auditLoggingInterceptor, sessionTimeoutManager);
    }

    /**
     * Helper method to create authenticated session attributes
     */
    private Map<String, Object> createAuthenticatedAttributes(String tenantId, String username, String role) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("authenticated", true);
        attributes.put("username", username != null ? username : "testuser");
        attributes.put("tenantId", tenantId);
        if (role != null) {
            attributes.put("role", role);
        }
        return attributes;
    }

    @Test
    @DisplayName("Should broadcast generic notification to tenant")
    void testBroadcastGenericNotification() throws Exception {
        // Given - Two sessions for TENANT001
        Map<String, Object> attrs1 = createAuthenticatedAttributes("TENANT001", "user1", "DOCTOR");
        Map<String, Object> attrs2 = createAuthenticatedAttributes("TENANT001", "user2", "NURSE");

        when(session1.getId()).thenReturn("session-1");
        when(session1.isOpen()).thenReturn(true);
        when(session1.getAttributes()).thenReturn(attrs1);

        when(session2.getId()).thenReturn("session-2");
        when(session2.isOpen()).thenReturn(true);
        when(session2.getAttributes()).thenReturn(attrs2);

        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);

        // Clear welcome messages
        reset(session1, session2);
        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);

        // When - Broadcast generic notification
        Map<String, Object> notification = Map.of(
                "type", "NOTIFICATION",
                "notificationType", "CARE_GAP",
                "title", "Care Gap Identified",
                "message", "Patient has an overdue screening",
                "patientId", "patient-123"
        );

        boolean result = handler.broadcastGenericNotification("TENANT001", notification);

        // Then
        assertThat(result).isTrue();
        verify(session1).sendMessage(any(TextMessage.class));
        verify(session2).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("Should return false when no sessions for tenant")
    void testBroadcastGenericNotificationNoSessions() {
        // Given - No sessions

        // When
        Map<String, Object> notification = Map.of(
                "type", "NOTIFICATION",
                "notificationType", "ALERT"
        );

        boolean result = handler.broadcastGenericNotification("TENANT999", notification);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should broadcast to specific user only")
    void testBroadcastToUser() throws Exception {
        // Given - Multiple users in same tenant
        Map<String, Object> attrs1 = createAuthenticatedAttributes("TENANT001", "doctor1", "DOCTOR");
        Map<String, Object> attrs2 = createAuthenticatedAttributes("TENANT001", "doctor2", "DOCTOR");
        Map<String, Object> attrs3 = createAuthenticatedAttributes("TENANT001", "nurse1", "NURSE");

        when(session1.getId()).thenReturn("session-1");
        when(session1.isOpen()).thenReturn(true);
        when(session1.getAttributes()).thenReturn(attrs1);

        when(session2.getId()).thenReturn("session-2");
        when(session2.isOpen()).thenReturn(true);
        when(session2.getAttributes()).thenReturn(attrs2);

        when(session3.getId()).thenReturn("session-3");
        when(session3.isOpen()).thenReturn(true);
        when(session3.getAttributes()).thenReturn(attrs3);

        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);
        handler.afterConnectionEstablished(session3);

        // Clear welcome messages but keep session state
        // Note: broadcastToUser uses sessionUsers map, not session.getAttributes()
        // Only session2 needs isOpen stubbed since only it will call sendMessage
        reset(session1, session2, session3);
        when(session2.isOpen()).thenReturn(true);

        // When - Broadcast to doctor2 only
        Map<String, Object> notification = Map.of(
                "type", "NOTIFICATION",
                "notificationType", "APPOINTMENT_REMINDER",
                "title", "Upcoming Appointment",
                "message", "You have an appointment at 2pm"
        );

        boolean result = handler.broadcastToUser("doctor2", "TENANT001", notification);

        // Then - Only doctor2's session should receive
        assertThat(result).isTrue();
        verify(session1, never()).sendMessage(any(TextMessage.class));
        verify(session2).sendMessage(any(TextMessage.class));
        verify(session3, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("Should return false when user not found")
    void testBroadcastToUserNotFound() throws Exception {
        // Given - Session for different user
        Map<String, Object> attrs = createAuthenticatedAttributes("TENANT001", "user1", "DOCTOR");

        when(session1.getId()).thenReturn("session-1");
        when(session1.isOpen()).thenReturn(true);
        when(session1.getAttributes()).thenReturn(attrs);

        handler.afterConnectionEstablished(session1);

        // When - Broadcast to non-existent user
        Map<String, Object> notification = Map.of("type", "NOTIFICATION");
        boolean result = handler.broadcastToUser("user999", "TENANT001", notification);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should enforce tenant isolation in user broadcast")
    void testBroadcastToUserTenantIsolation() throws Exception {
        // Given - Same username in different tenants
        Map<String, Object> attrs1 = createAuthenticatedAttributes("TENANT001", "doctor1", "DOCTOR");
        Map<String, Object> attrs2 = createAuthenticatedAttributes("TENANT002", "doctor1", "DOCTOR");

        when(session1.getId()).thenReturn("session-1");
        when(session1.isOpen()).thenReturn(true);
        when(session1.getAttributes()).thenReturn(attrs1);

        when(session2.getId()).thenReturn("session-2");
        when(session2.isOpen()).thenReturn(true);
        when(session2.getAttributes()).thenReturn(attrs2);

        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);

        // Clear welcome messages but keep session state
        // Note: Only stub session1 because session2 will be filtered out by tenant check
        reset(session1, session2);
        when(session1.isOpen()).thenReturn(true);

        // When - Broadcast to doctor1 in TENANT001
        Map<String, Object> notification = Map.of("type", "NOTIFICATION");
        boolean result = handler.broadcastToUser("doctor1", "TENANT001", notification);

        // Then - Only TENANT001 session should receive
        assertThat(result).isTrue();
        verify(session1).sendMessage(any(TextMessage.class));
        verify(session2, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("Should broadcast to all users with specific role")
    void testBroadcastToRole() throws Exception {
        // Given - Multiple users with different roles
        Map<String, Object> attrs1 = createAuthenticatedAttributes("TENANT001", "doctor1", "DOCTOR");
        Map<String, Object> attrs2 = createAuthenticatedAttributes("TENANT001", "doctor2", "DOCTOR");
        Map<String, Object> attrs3 = createAuthenticatedAttributes("TENANT001", "nurse1", "NURSE");

        when(session1.getId()).thenReturn("session-1");
        when(session1.isOpen()).thenReturn(true);
        when(session1.getAttributes()).thenReturn(attrs1);

        when(session2.getId()).thenReturn("session-2");
        when(session2.isOpen()).thenReturn(true);
        when(session2.getAttributes()).thenReturn(attrs2);

        when(session3.getId()).thenReturn("session-3");
        when(session3.isOpen()).thenReturn(true);
        when(session3.getAttributes()).thenReturn(attrs3);

        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);
        handler.afterConnectionEstablished(session3);

        // Clear welcome messages but keep session state
        reset(session1, session2, session3);
        when(session1.isOpen()).thenReturn(true);
        when(session1.getAttributes()).thenReturn(attrs1);
        when(session2.isOpen()).thenReturn(true);
        when(session2.getAttributes()).thenReturn(attrs2);
        // session3 needs getAttributes to check role, but not isOpen since it won't receive message
        when(session3.getAttributes()).thenReturn(attrs3);

        // When - Broadcast to DOCTOR role only
        Map<String, Object> notification = Map.of(
                "type", "NOTIFICATION",
                "notificationType", "CRITICAL_ALERT",
                "title", "Critical Patient Alert",
                "message", "Patient requires immediate attention"
        );

        boolean result = handler.broadcastToRole("TENANT001", "DOCTOR", notification);

        // Then - Only doctors should receive
        assertThat(result).isTrue();
        verify(session1).sendMessage(any(TextMessage.class));
        verify(session2).sendMessage(any(TextMessage.class));
        verify(session3, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("Should return false when no users with role found")
    void testBroadcastToRoleNotFound() throws Exception {
        // Given - Session with different role
        Map<String, Object> attrs = createAuthenticatedAttributes("TENANT001", "user1", "NURSE");

        when(session1.getId()).thenReturn("session-1");
        when(session1.isOpen()).thenReturn(true);
        when(session1.getAttributes()).thenReturn(attrs);

        handler.afterConnectionEstablished(session1);

        // When - Broadcast to non-existent role
        Map<String, Object> notification = Map.of("type", "NOTIFICATION");
        boolean result = handler.broadcastToRole("TENANT001", "ADMIN", notification);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should enforce tenant isolation in role broadcast")
    void testBroadcastToRoleTenantIsolation() throws Exception {
        // Given - Same role in different tenants
        Map<String, Object> attrs1 = createAuthenticatedAttributes("TENANT001", "doctor1", "DOCTOR");
        Map<String, Object> attrs2 = createAuthenticatedAttributes("TENANT002", "doctor2", "DOCTOR");

        when(session1.getId()).thenReturn("session-1");
        when(session1.isOpen()).thenReturn(true);
        when(session1.getAttributes()).thenReturn(attrs1);

        when(session2.getId()).thenReturn("session-2");
        when(session2.isOpen()).thenReturn(true);
        when(session2.getAttributes()).thenReturn(attrs2);

        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);

        // Clear welcome messages but keep session state
        // Note: Only stub session1 because session2 will be filtered out by tenant check
        reset(session1, session2);
        when(session1.isOpen()).thenReturn(true);
        when(session1.getAttributes()).thenReturn(attrs1);

        // When - Broadcast to DOCTOR role in TENANT001
        Map<String, Object> notification = Map.of("type", "NOTIFICATION");
        boolean result = handler.broadcastToRole("TENANT001", "DOCTOR", notification);

        // Then - Only TENANT001 doctor should receive
        assertThat(result).isTrue();
        verify(session1).sendMessage(any(TextMessage.class));
        verify(session2, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("Should handle user with multiple sessions")
    void testBroadcastToUserMultipleSessions() throws Exception {
        // Given - Same user with multiple sessions (e.g., multiple browser tabs)
        Map<String, Object> attrs1 = createAuthenticatedAttributes("TENANT001", "doctor1", "DOCTOR");
        Map<String, Object> attrs2 = createAuthenticatedAttributes("TENANT001", "doctor1", "DOCTOR");

        when(session1.getId()).thenReturn("session-1");
        when(session1.isOpen()).thenReturn(true);
        when(session1.getAttributes()).thenReturn(attrs1);

        when(session2.getId()).thenReturn("session-2");
        when(session2.isOpen()).thenReturn(true);
        when(session2.getAttributes()).thenReturn(attrs2);

        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);

        // Clear welcome messages
        reset(session1, session2);
        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);

        // When - Broadcast to doctor1
        Map<String, Object> notification = Map.of("type", "NOTIFICATION");
        boolean result = handler.broadcastToUser("doctor1", "TENANT001", notification);

        // Then - Both sessions should receive
        assertThat(result).isTrue();
        verify(session1).sendMessage(any(TextMessage.class));
        verify(session2).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("Should broadcast correct message format for generic notification")
    void testGenericNotificationMessageFormat() throws Exception {
        // Given
        Map<String, Object> attrs = createAuthenticatedAttributes("TENANT001", "user1", "DOCTOR");

        when(session1.getId()).thenReturn("session-1");
        when(session1.isOpen()).thenReturn(true);
        when(session1.getAttributes()).thenReturn(attrs);

        handler.afterConnectionEstablished(session1);

        // Clear welcome message
        reset(session1);
        when(session1.isOpen()).thenReturn(true);

        // When
        Map<String, Object> notification = Map.of(
                "type", "NOTIFICATION",
                "notificationType", "HEALTH_SCORE_UPDATE",
                "title", "Health Score Updated",
                "message", "Patient health score improved",
                "patientId", "patient-123",
                "tenantId", "TENANT001",
                "severity", "MEDIUM",
                "timestamp", 1234567890L
        );

        handler.broadcastGenericNotification("TENANT001", notification);

        // Then
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session1).sendMessage(messageCaptor.capture());

        String payload = messageCaptor.getValue().getPayload();
        Map<String, Object> parsedMessage = objectMapper.readValue(payload, Map.class);

        assertThat(parsedMessage.get("type")).isEqualTo("NOTIFICATION");
        assertThat(parsedMessage.get("notificationType")).isEqualTo("HEALTH_SCORE_UPDATE");
        assertThat(parsedMessage.get("title")).isEqualTo("Health Score Updated");
        assertThat(parsedMessage.get("message")).isEqualTo("Patient health score improved");
        assertThat(parsedMessage.get("patientId")).isEqualTo("patient-123");
    }
}
