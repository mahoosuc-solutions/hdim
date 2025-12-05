package com.healthdata.quality.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for Health Score WebSocket Handler (Phase 3.2)
 *
 * Tests cover:
 * - Connection management with authentication
 * - Tenant-based filtering
 * - Broadcasting health score updates
 * - Broadcasting significant change alerts
 * - Connection cleanup
 * - Multi-client broadcasting
 * - Error handling
 * - Security validations
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HealthScoreWebSocketHandler Tests - Phase 3.2")
class HealthScoreWebSocketHandlerTest {

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
    private Map<String, Object> createAuthenticatedAttributes(String tenantId, String username) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("authenticated", true);
        attributes.put("username", username != null ? username : "testuser");
        attributes.put("tenantId", tenantId);
        return attributes;
    }

    @Test
    @DisplayName("Should establish WebSocket connection and send welcome message")
    void testConnectionEstablished() throws Exception {
        // Given
        Map<String, Object> attributes = createAuthenticatedAttributes("TENANT001", "testuser");

        when(session1.getId()).thenReturn("session-1");
        when(session1.isOpen()).thenReturn(true);
        when(session1.getAttributes()).thenReturn(attributes);

        // When
        handler.afterConnectionEstablished(session1);

        // Then
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session1).sendMessage(messageCaptor.capture());

        String payload = messageCaptor.getValue().getPayload();
        assertThat(payload).contains("CONNECTION_ESTABLISHED");
        assertThat(payload).contains("session-1");
        assertThat(payload).contains("TENANT001");
        assertThat(payload).contains("testuser");
        assertThat(handler.getConnectionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should reject unauthenticated connection")
    void testRejectUnauthenticated() throws Exception {
        // Given
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("authenticated", false);

        when(session1.getId()).thenReturn("session-1");
        when(session1.getAttributes()).thenReturn(attributes);

        // When
        handler.afterConnectionEstablished(session1);

        // Then
        verify(session1).close(any(CloseStatus.class));
        assertThat(handler.getConnectionCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should reject connection without tenant ID")
    void testRejectWithoutTenantId() throws Exception {
        // Given
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("authenticated", true);
        attributes.put("username", "testuser");
        attributes.put("tenantId", null);

        when(session1.getId()).thenReturn("session-1");
        when(session1.getAttributes()).thenReturn(attributes);

        // When
        handler.afterConnectionEstablished(session1);

        // Then
        verify(session1).close(any(CloseStatus.class));
        assertThat(handler.getConnectionCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should close connection and cleanup session")
    void testConnectionClosed() throws Exception {
        // Given
        Map<String, Object> attributes = createAuthenticatedAttributes("TENANT001", "testuser");

        when(session1.getId()).thenReturn("session-1");
        when(session1.isOpen()).thenReturn(true);
        when(session1.getAttributes()).thenReturn(attributes);

        handler.afterConnectionEstablished(session1);
        assertThat(handler.getConnectionCount()).isEqualTo(1);

        // When
        handler.afterConnectionClosed(session1, CloseStatus.NORMAL);

        // Then
        assertThat(handler.getConnectionCount()).isEqualTo(0);
        verify(auditLoggingInterceptor).logDisconnectEvent(eq("session-1"), eq("testuser"), eq("TENANT001"), anyLong());
    }

    @Test
    @DisplayName("Should broadcast health score update to all clients")
    void testBroadcastHealthScoreUpdate() throws Exception {
        // Given - Multiple connected clients
        Map<String, Object> attrs1 = createAuthenticatedAttributes("TENANT001", "user1");
        Map<String, Object> attrs2 = createAuthenticatedAttributes("TENANT001", "user2");

        when(session1.getId()).thenReturn("session-1");
        when(session1.getUri()).thenReturn(new URI("ws://localhost:8087/quality-measure/ws/health-scores?tenantId=TENANT001"));
        when(session1.isOpen()).thenReturn(true);
        when(session1.getAttributes()).thenReturn(attrs1);

        when(session2.getId()).thenReturn("session-2");
        when(session2.getUri()).thenReturn(new URI("ws://localhost:8087/quality-measure/ws/health-scores?tenantId=TENANT001"));
        when(session2.isOpen()).thenReturn(true);
        when(session2.getAttributes()).thenReturn(attrs2);

        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);

        // Clear welcome messages
        reset(session1, session2);
        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);

        // When - Broadcast health score update
        Map<String, Object> healthScoreUpdate = Map.of(
            "patientId", "patient-123",
            "tenantId", "TENANT001",
            "overallScore", 75.5,
            "previousScore", 72.0,
            "scoreDelta", 3.5
        );

        handler.broadcastHealthScoreUpdate(healthScoreUpdate, "TENANT001");

        // Then - Both sessions should receive the message
        verify(session1).sendMessage(any(TextMessage.class));
        verify(session2).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("Should filter broadcasts by tenant ID")
    void testTenantBasedFiltering() throws Exception {
        // Given - Clients from different tenants
        Map<String, Object> attrs1 = createAuthenticatedAttributes("TENANT001", "user1");
        Map<String, Object> attrs2 = createAuthenticatedAttributes("TENANT002", "user2");

        when(session1.getId()).thenReturn("session-1");
        when(session1.getUri()).thenReturn(new URI("ws://localhost:8087/quality-measure/ws/health-scores?tenantId=TENANT001"));
        when(session1.isOpen()).thenReturn(true);
        when(session1.getAttributes()).thenReturn(attrs1);

        when(session2.getId()).thenReturn("session-2");
        when(session2.getUri()).thenReturn(new URI("ws://localhost:8087/quality-measure/ws/health-scores?tenantId=TENANT002"));
        when(session2.isOpen()).thenReturn(true);
        when(session2.getAttributes()).thenReturn(attrs2);

        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);

        // Clear welcome messages and re-stub only session1
        reset(session1, session2);
        when(session1.isOpen()).thenReturn(true);

        // When - Broadcast to TENANT001 only
        Map<String, Object> healthScoreUpdate = Map.of(
            "patientId", "patient-123",
            "tenantId", "TENANT001",
            "overallScore", 75.5
        );

        handler.broadcastHealthScoreUpdate(healthScoreUpdate, "TENANT001");

        // Then - Only TENANT001 session should receive the message
        verify(session1).sendMessage(any(TextMessage.class));
        verify(session2, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("Should broadcast significant change alert")
    void testBroadcastSignificantChange() throws Exception {
        // Given
        Map<String, Object> attributes = createAuthenticatedAttributes("TENANT001", "testuser");

        when(session1.getId()).thenReturn("session-1");
        when(session1.getUri()).thenReturn(new URI("ws://localhost:8087/quality-measure/ws/health-scores?tenantId=TENANT001"));
        when(session1.isOpen()).thenReturn(true);
        when(session1.getAttributes()).thenReturn(attributes);

        handler.afterConnectionEstablished(session1);

        // Clear welcome message
        reset(session1);
        when(session1.isOpen()).thenReturn(true);

        // When - Broadcast significant change
        Map<String, Object> significantChange = Map.of(
            "patientId", "patient-123",
            "tenantId", "TENANT001",
            "overallScore", 45.0,
            "previousScore", 75.0,
            "scoreDelta", -30.0,
            "significantChange", true,
            "changeReason", "Large decline in mental health score"
        );

        handler.broadcastSignificantChange(significantChange, "TENANT001");

        // Then
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session1).sendMessage(messageCaptor.capture());

        String payload = messageCaptor.getValue().getPayload();
        assertThat(payload).contains("SIGNIFICANT_CHANGE");
        assertThat(payload).contains("patient-123");
        assertThat(payload).contains("Large decline in mental health score");
        assertThat(payload).contains("high"); // priority
    }

    @Test
    @DisplayName("Should broadcast to all tenants when tenantId is null")
    void testBroadcastToAllTenants() throws Exception {
        // Given - Clients from different tenants
        Map<String, Object> attrs1 = createAuthenticatedAttributes("TENANT001", "user1");
        Map<String, Object> attrs2 = createAuthenticatedAttributes("TENANT002", "user2");

        when(session1.getId()).thenReturn("session-1");
        when(session1.getUri()).thenReturn(new URI("ws://localhost:8087/quality-measure/ws/health-scores?tenantId=TENANT001"));
        when(session1.isOpen()).thenReturn(true);
        when(session1.getAttributes()).thenReturn(attrs1);

        when(session2.getId()).thenReturn("session-2");
        when(session2.getUri()).thenReturn(new URI("ws://localhost:8087/quality-measure/ws/health-scores?tenantId=TENANT002"));
        when(session2.isOpen()).thenReturn(true);
        when(session2.getAttributes()).thenReturn(attrs2);

        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);

        // Clear welcome messages
        reset(session1, session2);
        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);

        // When - Broadcast without tenant filter
        Map<String, Object> systemAlert = Map.of(
            "type", "SYSTEM_ALERT",
            "message", "Health score calculation service updated"
        );

        handler.broadcastHealthScoreUpdate(systemAlert, null);

        // Then - All sessions should receive the message
        verify(session1).sendMessage(any(TextMessage.class));
        verify(session2).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("Should handle send message failure gracefully")
    void testHandleSendMessageFailure() throws Exception {
        // Given
        Map<String, Object> attributes = createAuthenticatedAttributes("TENANT001", "testuser");

        when(session1.getId()).thenReturn("session-1");
        when(session1.getUri()).thenReturn(new URI("ws://localhost:8087/quality-measure/ws/health-scores?tenantId=TENANT001"));
        when(session1.isOpen()).thenReturn(true);
        when(session1.getAttributes()).thenReturn(attributes);
        doThrow(new IOException("Connection lost")).when(session1).sendMessage(any(TextMessage.class));

        handler.afterConnectionEstablished(session1);

        // Clear the exception from welcome message
        reset(session1);
        when(session1.isOpen()).thenReturn(true);
        doThrow(new IOException("Connection lost")).when(session1).sendMessage(any(TextMessage.class));

        // When - Attempt to broadcast
        Map<String, Object> healthScoreUpdate = Map.of(
            "patientId", "patient-123",
            "overallScore", 75.5
        );

        // Then - Should not throw exception
        handler.broadcastHealthScoreUpdate(healthScoreUpdate, "TENANT001");
    }

    @Test
    @DisplayName("Should not send to closed sessions")
    void testDoNotSendToClosedSessions() throws Exception {
        // Given
        Map<String, Object> attributes = createAuthenticatedAttributes("TENANT001", "testuser");

        when(session1.getId()).thenReturn("session-1");
        when(session1.getUri()).thenReturn(new URI("ws://localhost:8087/quality-measure/ws/health-scores?tenantId=TENANT001"));
        when(session1.isOpen()).thenReturn(true);
        when(session1.getAttributes()).thenReturn(attributes);

        handler.afterConnectionEstablished(session1);

        // Clear welcome message
        reset(session1);

        // Session is now closed
        when(session1.isOpen()).thenReturn(false);

        // When - Attempt to broadcast
        Map<String, Object> healthScoreUpdate = Map.of(
            "patientId", "patient-123",
            "overallScore", 75.5
        );

        handler.broadcastHealthScoreUpdate(healthScoreUpdate, "TENANT001");

        // Then - Should not attempt to send
        verify(session1, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("Should get connection count for specific tenant")
    void testGetConnectionCountByTenant() throws Exception {
        // Given - Multiple clients from different tenants
        Map<String, Object> attrs1 = createAuthenticatedAttributes("TENANT001", "user1");
        Map<String, Object> attrs2 = createAuthenticatedAttributes("TENANT001", "user2");
        Map<String, Object> attrs3 = createAuthenticatedAttributes("TENANT002", "user3");

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

        // Then
        assertThat(handler.getConnectionCount()).isEqualTo(3);
        assertThat(handler.getConnectionCount("TENANT001")).isEqualTo(2);
        assertThat(handler.getConnectionCount("TENANT002")).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle transport error and close session")
    void testHandleTransportError() throws Exception {
        // Given
        when(session1.getId()).thenReturn("session-1");

        // When
        handler.handleTransportError(session1, new RuntimeException("Network error"));

        // Then
        verify(session1).close(CloseStatus.SERVER_ERROR);
    }

    @Test
    @DisplayName("Should handle incoming client messages")
    void testHandleTextMessage() throws Exception {
        // Given
        when(session1.getId()).thenReturn("session-1");
        TextMessage message = new TextMessage("{\"action\":\"ping\"}");

        // When - Should not throw exception
        handler.handleTextMessage(session1, message);

        // Then - Verify timeout manager was called
        verify(sessionTimeoutManager).updateLastActivity("session-1");
    }

    @Test
    @DisplayName("Should broadcast message in correct JSON format")
    void testBroadcastMessageFormat() throws Exception {
        // Given
        Map<String, Object> attributes = createAuthenticatedAttributes("TENANT001", "testuser");

        when(session1.getId()).thenReturn("session-1");
        when(session1.getUri()).thenReturn(new URI("ws://localhost:8087/quality-measure/ws/health-scores?tenantId=TENANT001"));
        when(session1.isOpen()).thenReturn(true);
        when(session1.getAttributes()).thenReturn(attributes);

        handler.afterConnectionEstablished(session1);

        // Clear welcome message
        reset(session1);
        when(session1.isOpen()).thenReturn(true);

        // When
        Map<String, Object> healthScoreUpdate = new HashMap<>();
        healthScoreUpdate.put("patientId", "patient-123");
        healthScoreUpdate.put("overallScore", 75.5);

        handler.broadcastHealthScoreUpdate(healthScoreUpdate, "TENANT001");

        // Then
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session1).sendMessage(messageCaptor.capture());

        String payload = messageCaptor.getValue().getPayload();
        Map<String, Object> parsedMessage = objectMapper.readValue(payload, Map.class);

        assertThat(parsedMessage).containsKey("type");
        assertThat(parsedMessage).containsKey("data");
        assertThat(parsedMessage).containsKey("timestamp");
        assertThat(parsedMessage.get("type")).isEqualTo("HEALTH_SCORE_UPDATE");
    }

    @Test
    @DisplayName("Should broadcast clinical alert")
    void testBroadcastClinicalAlert() throws Exception {
        // Given
        Map<String, Object> attributes = createAuthenticatedAttributes("TENANT001", "testuser");

        when(session1.getId()).thenReturn("session-1");
        when(session1.getUri()).thenReturn(new URI("ws://localhost:8087/quality-measure/ws/health-scores?tenantId=TENANT001"));
        when(session1.isOpen()).thenReturn(true);
        when(session1.getAttributes()).thenReturn(attributes);

        handler.afterConnectionEstablished(session1);

        // Clear welcome message
        reset(session1);
        when(session1.isOpen()).thenReturn(true);

        // When
        Map<String, Object> alert = Map.of(
            "patientId", "patient-123",
            "alertType", "MENTAL_HEALTH_CRISIS",
            "severity", "HIGH",
            "message", "PHQ-9 score >= 20"
        );

        boolean result = handler.broadcastClinicalAlert(alert, "TENANT001");

        // Then
        assertThat(result).isTrue();
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session1).sendMessage(messageCaptor.capture());

        String payload = messageCaptor.getValue().getPayload();
        assertThat(payload).contains("CLINICAL_ALERT");
        assertThat(payload).contains("patient-123");
    }

    @Test
    @DisplayName("Should return false when no sessions for clinical alert")
    void testBroadcastClinicalAlertNoSessions() {
        // When - No sessions connected
        Map<String, Object> alert = Map.of(
            "patientId", "patient-123",
            "alertType", "TEST"
        );

        boolean result = handler.broadcastClinicalAlert(alert, "TENANT999");

        // Then
        assertThat(result).isFalse();
    }
}
