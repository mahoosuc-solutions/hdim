package com.healthdata.migration.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@DisplayName("MigrationWebSocketHandler")
class MigrationWebSocketHandlerTest {

    @Test
    @DisplayName("Should subscribe and send ack on connect")
    void shouldSubscribeAndSendAckOnConnect() throws Exception {
        MigrationProgressPublisher publisher = mock(MigrationProgressPublisher.class);
        WebSocketSession session = mock(WebSocketSession.class);
        UUID jobId = UUID.randomUUID();

        when(session.getId()).thenReturn("session-1");
        when(session.getUri()).thenReturn(URI.create("ws://localhost/api/v1/migrations/" + jobId + "/stream"));

        MigrationWebSocketHandler handler = new MigrationWebSocketHandler(publisher);
        handler.afterConnectionEstablished(session);

        verify(publisher).subscribe(eq(jobId), eq(session));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        assertThat(captor.getValue().getPayload()).contains("subscribed").contains(jobId.toString());
    }

    @Test
    @DisplayName("Should close connection when job ID missing")
    void shouldCloseWhenJobIdMissing() throws Exception {
        MigrationProgressPublisher publisher = mock(MigrationProgressPublisher.class);
        WebSocketSession session = mock(WebSocketSession.class);

        when(session.getId()).thenReturn("session-1");
        when(session.getUri()).thenReturn(URI.create("ws://localhost/api/v1/migrations/not-a-uuid/stream"));

        MigrationWebSocketHandler handler = new MigrationWebSocketHandler(publisher);
        handler.afterConnectionEstablished(session);

        verify(session).close(eq(CloseStatus.BAD_DATA));
    }

    @Test
    @DisplayName("Should respond to ping and handle subscriptions")
    void shouldHandlePingAndSubscriptions() throws Exception {
        MigrationProgressPublisher publisher = mock(MigrationProgressPublisher.class);
        WebSocketSession session = mock(WebSocketSession.class);
        UUID jobId = UUID.randomUUID();
        UUID newJobId = UUID.randomUUID();

        when(session.getId()).thenReturn("session-1");
        when(session.getUri()).thenReturn(URI.create("ws://localhost/api/v1/migrations/" + jobId + "/stream"));

        MigrationWebSocketHandler handler = new MigrationWebSocketHandler(publisher);
        handler.afterConnectionEstablished(session);

        handler.handleTextMessage(session, new TextMessage("{\"action\":\"ping\"}"));
        handler.handleTextMessage(session, new TextMessage("{\"action\":\"subscribe\",\"jobId\":\"" + newJobId + "\"}"));
        handler.handleTextMessage(session, new TextMessage("{\"action\":\"unsubscribe\"}"));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, org.mockito.Mockito.atLeastOnce()).sendMessage(captor.capture());

        List<TextMessage> messages = captor.getAllValues();
        assertThat(messages.stream().anyMatch(msg -> msg.getPayload().contains("pong"))).isTrue();

        verify(publisher).unsubscribe(eq(jobId), eq(session));
        verify(publisher).subscribe(eq(newJobId), eq(session));
    }

    @Test
    @DisplayName("Should clean up on close and error")
    void shouldCleanupOnCloseAndError() throws Exception {
        MigrationProgressPublisher publisher = mock(MigrationProgressPublisher.class);
        WebSocketSession session = mock(WebSocketSession.class);
        UUID jobId = UUID.randomUUID();

        when(session.getId()).thenReturn("session-1");
        when(session.getUri()).thenReturn(URI.create("ws://localhost/api/v1/migrations/" + jobId + "/stream"));

        MigrationWebSocketHandler handler = new MigrationWebSocketHandler(publisher);
        handler.afterConnectionEstablished(session);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);
        handler.handleTransportError(session, new RuntimeException("boom"));

        verify(publisher).unsubscribe(eq(jobId), eq(session));
        verify(publisher).unsubscribeAll(eq(session));
    }
}
