package com.healthdata.quality.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.WebSocketSession;

@DisplayName("Session Timeout Manager Tests")
class SessionTimeoutManagerTest {

    @Test
    @DisplayName("Should register and unregister sessions")
    void shouldRegisterAndUnregisterSessions() {
        SessionTimeoutManager manager = new SessionTimeoutManager();
        WebSocketSession session = Mockito.mock(WebSocketSession.class);

        manager.registerSession("session-1", session);
        assertThat(manager.getActiveSessionCount()).isEqualTo(1);

        manager.unregisterSession("session-1");
        assertThat(manager.getActiveSessionCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should close timed out sessions")
    void shouldCloseTimedOutSessions() throws Exception {
        SessionTimeoutManager manager = new SessionTimeoutManager();
        ReflectionTestUtils.setField(manager, "sessionTimeoutMinutes", 0);

        WebSocketSession session = Mockito.mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("username", "user1");
        attributes.put("tenantId", "tenant-1");
        when(session.getAttributes()).thenReturn(attributes);
        when(session.isOpen()).thenReturn(true);

        manager.registerSession("session-1", session);

        @SuppressWarnings("unchecked")
        Map<String, Long> lastActivity =
            (Map<String, Long>) ReflectionTestUtils.getField(manager, "sessionLastActivity");
        lastActivity.put("session-1", System.currentTimeMillis() - 60000);

        manager.checkSessionTimeouts();

        verify(session).close(Mockito.any());
        assertThat(manager.getActiveSessionCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should cleanup when session already closed")
    void shouldCleanupWhenSessionClosed() {
        SessionTimeoutManager manager = new SessionTimeoutManager();
        ReflectionTestUtils.setField(manager, "sessionTimeoutMinutes", 0);

        WebSocketSession session = Mockito.mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(false);

        manager.registerSession("session-2", session);

        @SuppressWarnings("unchecked")
        Map<String, Long> lastActivity =
            (Map<String, Long>) ReflectionTestUtils.getField(manager, "sessionLastActivity");
        lastActivity.put("session-2", System.currentTimeMillis() - 60000);

        manager.checkSessionTimeouts();

        assertThat(manager.getActiveSessionCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should cleanup when close throws exception")
    void shouldCleanupWhenCloseThrowsException() throws Exception {
        SessionTimeoutManager manager = new SessionTimeoutManager();
        ReflectionTestUtils.setField(manager, "sessionTimeoutMinutes", 0);

        WebSocketSession session = Mockito.mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        doThrow(new java.io.IOException("close failed")).when(session).close(Mockito.any());
        when(session.getAttributes()).thenReturn(Map.of());

        manager.registerSession("session-3", session);

        @SuppressWarnings("unchecked")
        Map<String, Long> lastActivity =
            (Map<String, Long>) ReflectionTestUtils.getField(manager, "sessionLastActivity");
        lastActivity.put("session-3", System.currentTimeMillis() - 60000);

        manager.checkSessionTimeouts();

        assertThat(manager.getActiveSessionCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return -1 for inactivity when session missing")
    void shouldReturnMinusOneWhenSessionMissing() {
        SessionTimeoutManager manager = new SessionTimeoutManager();

        assertThat(manager.getSessionInactivityDuration("missing")).isEqualTo(-1);
    }
}
