package com.healthdata.demo.orchestrator.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.demo.orchestrator.model.DevOpsLogMessage;
import com.healthdata.demo.orchestrator.model.DevOpsStatusUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DevOpsLogWebSocketHandler.
 *
 * Tests verify:
 * - WebSocket connection establishment with tenant isolation
 * - Log level filtering (DEBUG, INFO, WARN, ERROR)
 * - Real-time log publishing to connected clients
 * - Status update broadcasting
 * - Ping/pong keepalive
 * - Session management and cleanup
 */
@ExtendWith(MockitoExtension.class)
class DevOpsLogWebSocketHandlerTest {

    private DevOpsLogWebSocketHandler handler;
    private ObjectMapper objectMapper;

    @Captor
    private ArgumentCaptor<TextMessage> messageCaptor;

    private static final String TENANT_ID = "demo-tenant";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new DevOpsLogWebSocketHandler(objectMapper);
    }

    @Test
    void afterConnectionEstablished_ShouldAcceptValidConnection() throws Exception {
        // Given: WebSocket session with tenant ID
        WebSocketSession session = createMockSession(TENANT_ID, "INFO");

        // When
        handler.afterConnectionEstablished(session);

        // Then: Connection acknowledged
        verify(session).sendMessage(messageCaptor.capture());
        String ackMessage = messageCaptor.getValue().getPayload();
        assertThat(ackMessage).contains("\"type\":\"connected\"");
        assertThat(ackMessage).contains("\"tenantId\":\"" + TENANT_ID + "\"");
        assertThat(ackMessage).contains("\"level\":\"INFO\"");

        // Verify session tracked
        assertThat(handler.getSessionCount(TENANT_ID)).isEqualTo(1);
        assertThat(handler.getTotalSessionCount()).isEqualTo(1);
    }

    @Test
    void afterConnectionEstablished_ShouldRejectConnectionWithoutTenantId() throws Exception {
        // Given: WebSocket session without tenant ID
        WebSocketSession session = createMockSession(null, "INFO");

        // When
        handler.afterConnectionEstablished(session);

        // Then: Connection closed with policy violation
        verify(session).close(CloseStatus.POLICY_VIOLATION);
        verify(session, never()).sendMessage(any());

        assertThat(handler.getTotalSessionCount()).isEqualTo(0);
    }

    @Test
    void afterConnectionClosed_ShouldCleanupSession() throws Exception {
        // Given: Established connection
        WebSocketSession session = createMockSession(TENANT_ID, "INFO");
        handler.afterConnectionEstablished(session);
        reset(session); // Clear connection message

        // When
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        // Then: Session removed
        assertThat(handler.getSessionCount(TENANT_ID)).isEqualTo(0);
        assertThat(handler.getTotalSessionCount()).isEqualTo(0);
    }

    @Test
    void publishLog_ShouldSendToAllSessionsForTenant() throws Exception {
        // Given: Two sessions for same tenant
        WebSocketSession session1 = createMockSession(TENANT_ID, "INFO");
        WebSocketSession session2 = createMockSession(TENANT_ID, "INFO");
        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);
        reset(session1, session2);

        // When: Log published
        DevOpsLogMessage logMessage = DevOpsLogMessage.builder()
            .level("INFO")
            .message("Data seeding started")
            .category("SEED")
            .tenantId(TENANT_ID)
            .component("demo-orchestrator")
            .build();

        handler.publishLog(logMessage);

        // Then: Both sessions receive message
        verify(session1).sendMessage(messageCaptor.capture());
        verify(session2).sendMessage(any(TextMessage.class));

        String payload = messageCaptor.getValue().getPayload();
        assertThat(payload).contains("\"type\":\"log\"");
        assertThat(payload).contains("\"level\":\"INFO\"");
        assertThat(payload).contains("\"message\":\"Data seeding started\"");
        assertThat(payload).contains("\"category\":\"SEED\"");
    }

    @Test
    void publishLog_ShouldFilterByLogLevel() throws Exception {
        // Given: Session with WARN level filter
        WebSocketSession session = createMockSession(TENANT_ID, "WARN");
        handler.afterConnectionEstablished(session);
        reset(session);

        // When: INFO log published (below WARN threshold)
        DevOpsLogMessage infoLog = DevOpsLogMessage.builder()
            .level("INFO")
            .message("Info message")
            .category("SEED")
            .tenantId(TENANT_ID)
            .build();

        handler.publishLog(infoLog);

        // Then: INFO message NOT sent (filtered out)
        verify(session, never()).sendMessage(any());

        // When: ERROR log published (above WARN threshold)
        DevOpsLogMessage errorLog = DevOpsLogMessage.builder()
            .level("ERROR")
            .message("Error message")
            .category("SEED")
            .tenantId(TENANT_ID)
            .build();

        handler.publishLog(errorLog);

        // Then: ERROR message sent
        verify(session).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getPayload()).contains("\"level\":\"ERROR\"");
    }

    @Test
    void publishLog_ShouldIsolateTenants() throws Exception {
        // Given: Two sessions for different tenants
        WebSocketSession session1 = createMockSession("tenant-1", "INFO");
        WebSocketSession session2 = createMockSession("tenant-2", "INFO");
        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);
        reset(session1, session2);

        // When: Log published for tenant-1
        DevOpsLogMessage logMessage = DevOpsLogMessage.builder()
            .level("INFO")
            .message("Tenant 1 log")
            .category("SEED")
            .tenantId("tenant-1")
            .build();

        handler.publishLog(logMessage);

        // Then: Only tenant-1 session receives message
        verify(session1).sendMessage(any(TextMessage.class));
        verify(session2, never()).sendMessage(any());
    }

    @Test
    void publishStatus_ShouldBroadcastToTenant() throws Exception {
        // Given: Session connected
        WebSocketSession session = createMockSession(TENANT_ID, "INFO");
        handler.afterConnectionEstablished(session);
        reset(session);

        // When: Status update published
        DevOpsStatusUpdate statusUpdate = DevOpsStatusUpdate.builder()
            .component("FHIR_VALIDATION")
            .status("COMPLETED")
            .tenantId(TENANT_ID)
            .details(Map.of(
                "totalChecks", 10,
                "passedChecks", 8,
                "failedChecks", 2
            ))
            .build();

        handler.publishStatus(statusUpdate);

        // Then: Session receives status update
        verify(session).sendMessage(messageCaptor.capture());
        String payload = messageCaptor.getValue().getPayload();
        assertThat(payload).contains("\"type\":\"status\"");
        assertThat(payload).contains("\"component\":\"FHIR_VALIDATION\"");
        assertThat(payload).contains("\"status\":\"COMPLETED\"");
        assertThat(payload).contains("\"totalChecks\":10");
    }

    @Test
    void handleTextMessage_ShouldRespondToPing() throws Exception {
        // Given: Connected session
        WebSocketSession session = createMockSession(TENANT_ID, "INFO");
        handler.afterConnectionEstablished(session);
        reset(session);

        // When: Ping command received
        TextMessage pingMessage = new TextMessage("{\"type\":\"ping\"}");
        handler.handleTextMessage(session, pingMessage);

        // Then: Pong response sent
        verify(session).sendMessage(messageCaptor.capture());
        String pongMessage = messageCaptor.getValue().getPayload();
        assertThat(pongMessage).contains("\"type\":\"pong\"");
        assertThat(pongMessage).contains("\"timestamp\":");
    }

    @Test
    void handleTextMessage_ShouldChangeLogLevel() throws Exception {
        // Given: Connected session with INFO level
        WebSocketSession session = createMockSession(TENANT_ID, "INFO");
        handler.afterConnectionEstablished(session);
        reset(session);

        // When: setLevel command received
        TextMessage setLevelMessage = new TextMessage("{\"type\":\"setLevel\",\"payload\":\"ERROR\"}");
        handler.handleTextMessage(session, setLevelMessage);

        // Then: Level changed confirmation sent
        verify(session).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getPayload()).contains("\"type\":\"levelChanged\"");
        assertThat(messageCaptor.getValue().getPayload()).contains("\"level\":\"ERROR\"");

        // Verify filtering now uses ERROR level
        reset(session);
        DevOpsLogMessage warnLog = DevOpsLogMessage.builder()
            .level("WARN")
            .message("Warning")
            .category("SEED")
            .tenantId(TENANT_ID)
            .build();

        handler.publishLog(warnLog);
        verify(session, never()).sendMessage(any()); // WARN < ERROR, filtered out
    }

    @Test
    void publishLog_ShouldHandleNoConnectedSessions() {
        // Given: No connected sessions

        // When: Log published
        DevOpsLogMessage logMessage = DevOpsLogMessage.builder()
            .level("INFO")
            .message("No listeners")
            .category("SEED")
            .tenantId(TENANT_ID)
            .build();

        // Then: No exception thrown
        handler.publishLog(logMessage);

        assertThat(handler.getTotalSessionCount()).isEqualTo(0);
    }

    @Test
    void getSessionCount_ShouldReturnCorrectCounts() throws Exception {
        // Given: Multiple sessions across tenants
        WebSocketSession session1 = createMockSession("tenant-1", "INFO");
        WebSocketSession session2 = createMockSession("tenant-1", "INFO");
        WebSocketSession session3 = createMockSession("tenant-2", "INFO");

        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);
        handler.afterConnectionEstablished(session3);

        // Then: Counts correct
        assertThat(handler.getSessionCount("tenant-1")).isEqualTo(2);
        assertThat(handler.getSessionCount("tenant-2")).isEqualTo(1);
        assertThat(handler.getTotalSessionCount()).isEqualTo(3);
    }

    /**
     * Create mock WebSocket session with tenant and level.
     */
    private WebSocketSession createMockSession(String tenantId, String level) throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);

        String query = (tenantId != null ? "tenant=" + tenantId : "") +
                       (level != null ? "&level=" + level : "");
        URI uri = new URI("ws://localhost:8090/ws/devops/logs?" + query);

        when(session.getUri()).thenReturn(uri);
        when(session.isOpen()).thenReturn(true);
        when(session.getId()).thenReturn("session-" + System.nanoTime());

        return session;
    }
}
