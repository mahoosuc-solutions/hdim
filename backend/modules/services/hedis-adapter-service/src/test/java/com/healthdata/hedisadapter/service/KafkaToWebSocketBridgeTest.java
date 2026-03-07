package com.healthdata.hedisadapter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.hedisadapter.websocket.KafkaToWebSocketBridge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class KafkaToWebSocketBridgeTest {

    private KafkaToWebSocketBridge bridge;

    @BeforeEach
    void setUp() {
        bridge = new KafkaToWebSocketBridge(new ObjectMapper());
    }

    @Test
    void connectionEstablished_shouldTrackSession() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);

        bridge.afterConnectionEstablished(session);

        assertThat(bridge.getActiveConnectionCount()).isEqualTo(1);
    }

    @Test
    void connectionClosed_shouldRemoveSession() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);

        bridge.afterConnectionEstablished(session);
        bridge.afterConnectionClosed(session, CloseStatus.NORMAL);

        assertThat(bridge.getActiveConnectionCount()).isEqualTo(0);
    }

    @Test
    void onHdimEvent_shouldBroadcastToAllSessions() throws Exception {
        WebSocketSession session1 = mock(WebSocketSession.class);
        WebSocketSession session2 = mock(WebSocketSession.class);
        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);

        bridge.afterConnectionEstablished(session1);
        bridge.afterConnectionEstablished(session2);

        bridge.onHdimEvent(Map.of("eventType", "care-gap-closed"));

        verify(session1).sendMessage(any(TextMessage.class));
        verify(session2).sendMessage(any(TextMessage.class));
    }

    @Test
    void onHdimEvent_withNoSessions_shouldNotFail() {
        bridge.onHdimEvent(Map.of("eventType", "test"));
        // No exception thrown
    }

    @Test
    void onHdimEvent_shouldRemoveFailedSessions() throws Exception {
        WebSocketSession goodSession = mock(WebSocketSession.class);
        WebSocketSession badSession = mock(WebSocketSession.class);
        when(goodSession.isOpen()).thenReturn(true);
        when(badSession.isOpen()).thenReturn(true);
        doThrow(new java.io.IOException("Connection reset")).when(badSession).sendMessage(any());

        bridge.afterConnectionEstablished(goodSession);
        bridge.afterConnectionEstablished(badSession);

        bridge.onHdimEvent(Map.of("eventType", "test"));

        assertThat(bridge.getActiveConnectionCount()).isEqualTo(1);
    }
}
