package com.healthdata.fhir.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("Subscription WebSocket Handler Tests")
class SubscriptionWebSocketHandlerTest {

    @Mock
    private WebSocketSession session;

    @Test
    @DisplayName("Should accept connection and send ack")
    void shouldAcceptConnection() throws Exception {
        SubscriptionWebSocketHandler handler = new SubscriptionWebSocketHandler(new ObjectMapper());
        when(session.getUri()).thenReturn(new URI("ws://localhost/ws/subscriptions?tenant=tenant-1"));
        when(session.getId()).thenReturn("session-1");
        when(session.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, org.mockito.Mockito.atLeastOnce()).sendMessage(captor.capture());
        assertThat(captor.getValue().getPayload()).contains("connected");
    }

    @Test
    @DisplayName("Should accept connection from header")
    void shouldAcceptConnectionFromHeader() throws Exception {
        SubscriptionWebSocketHandler handler = new SubscriptionWebSocketHandler(new ObjectMapper());
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", "tenant-1");
        when(session.getUri()).thenReturn(new URI("ws://localhost/ws/subscriptions"));
        when(session.getHandshakeHeaders()).thenReturn(headers);
        when(session.getId()).thenReturn("session-1");
        when(session.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session);

        verify(session, org.mockito.Mockito.atLeastOnce()).sendMessage(any(TextMessage.class));
        assertThat(handler.getSessionCount("tenant-1")).isEqualTo(1);
    }

    @Test
    @DisplayName("Should close connection without tenant")
    void shouldCloseConnectionWithoutTenant() throws Exception {
        SubscriptionWebSocketHandler handler = new SubscriptionWebSocketHandler(new ObjectMapper());
        when(session.getUri()).thenReturn(new URI("ws://localhost/ws/subscriptions"));
        when(session.getHandshakeHeaders()).thenReturn(new HttpHeaders());

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.POLICY_VIOLATION);
    }

    @Test
    @DisplayName("Should handle invalid message format")
    void shouldHandleInvalidMessageFormat() throws Exception {
        SubscriptionWebSocketHandler handler = new SubscriptionWebSocketHandler(new ObjectMapper());
        when(session.getUri()).thenReturn(new URI("ws://localhost/ws/subscriptions?tenant=tenant-1"));
        when(session.getId()).thenReturn("session-1");
        when(session.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, new TextMessage("{invalid"));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, org.mockito.Mockito.atLeast(2)).sendMessage(captor.capture());
        assertThat(captor.getAllValues().stream().anyMatch(msg -> msg.getPayload().contains("error"))).isTrue();
    }

    @Test
    @DisplayName("Should handle unknown command")
    void shouldHandleUnknownCommand() throws Exception {
        SubscriptionWebSocketHandler handler = new SubscriptionWebSocketHandler(new ObjectMapper());
        when(session.getUri()).thenReturn(new URI("ws://localhost/ws/subscriptions?tenant=tenant-1"));
        when(session.getId()).thenReturn("session-1");
        when(session.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"unknown\"}"));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, org.mockito.Mockito.atLeast(2)).sendMessage(captor.capture());
        assertThat(captor.getAllValues().stream().anyMatch(msg -> msg.getPayload().contains("Unknown command"))).isTrue();
    }

    @Test
    @DisplayName("Should handle subscribe and unsubscribe commands")
    void shouldHandleSubscribeAndUnsubscribe() throws Exception {
        SubscriptionWebSocketHandler handler = new SubscriptionWebSocketHandler(new ObjectMapper());
        when(session.getUri()).thenReturn(new URI("ws://localhost/ws/subscriptions?tenant=tenant-1"));
        when(session.getId()).thenReturn("session-1");
        when(session.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"subscribe\",\"payload\":{}}"));
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"unsubscribe\",\"payload\":{}}"));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, org.mockito.Mockito.atLeast(3)).sendMessage(captor.capture());
        assertThat(captor.getAllValues().stream().anyMatch(msg -> msg.getPayload().contains("subscribed"))).isTrue();
        assertThat(captor.getAllValues().stream().anyMatch(msg -> msg.getPayload().contains("unsubscribed"))).isTrue();
    }

    @Test
    @DisplayName("Should clear sessions on close")
    void shouldClearSessionsOnClose() throws Exception {
        SubscriptionWebSocketHandler handler = new SubscriptionWebSocketHandler(new ObjectMapper());
        when(session.getUri()).thenReturn(new URI("ws://localhost/ws/subscriptions?tenant=tenant-1"));
        when(session.getId()).thenReturn("session-1");
        when(session.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session);
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        assertThat(handler.getSessionCount("tenant-1")).isZero();
        assertThat(handler.getTotalSessionCount()).isZero();
    }

    @Test
    @DisplayName("Should send pong on ping")
    void shouldSendPongOnPing() throws Exception {
        SubscriptionWebSocketHandler handler = new SubscriptionWebSocketHandler(new ObjectMapper());
        when(session.getUri()).thenReturn(new URI("ws://localhost/ws/subscriptions?tenant=tenant-1"));
        when(session.getId()).thenReturn("session-1");
        when(session.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"ping\"}"));

        verify(session, org.mockito.Mockito.atLeast(2)).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("Should send notifications to sessions")
    void shouldSendNotifications() throws Exception {
        SubscriptionWebSocketHandler handler = new SubscriptionWebSocketHandler(new ObjectMapper());
        when(session.getUri()).thenReturn(new URI("ws://localhost/ws/subscriptions?tenant=tenant-1"));
        when(session.getId()).thenReturn("session-1");
        when(session.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session);

        SubscriptionNotification notification = SubscriptionNotification.builder()
                .id(UUID.randomUUID().toString())
                .tenantId("tenant-1")
                .eventType(SubscriptionNotification.EventType.CREATED)
                .build();

        handler.sendNotification("tenant-1", notification);

        verify(session, org.mockito.Mockito.atLeast(2)).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("Should skip notifications when no sessions")
    void shouldSkipNotificationsWhenNoSessions() {
        SubscriptionWebSocketHandler handler = new SubscriptionWebSocketHandler(new ObjectMapper());
        SubscriptionNotification notification = SubscriptionNotification.builder()
                .id(UUID.randomUUID().toString())
                .tenantId("tenant-1")
                .eventType(SubscriptionNotification.EventType.CREATED)
                .build();

        handler.sendNotification("tenant-1", notification);
    }

    @Test
    @DisplayName("Should ignore messages without tenant mapping")
    void shouldIgnoreMessagesWithoutTenantMapping() throws Exception {
        SubscriptionWebSocketHandler handler = new SubscriptionWebSocketHandler(new ObjectMapper());
        when(session.getId()).thenReturn("session-1");

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"ping\"}"));

        verify(session, org.mockito.Mockito.never()).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("Should skip closed sessions when sending notifications")
    void shouldSkipClosedSessionsOnNotification() throws Exception {
        SubscriptionWebSocketHandler handler = new SubscriptionWebSocketHandler(new ObjectMapper());
        when(session.getUri()).thenReturn(new URI("ws://localhost/ws/subscriptions?tenant=tenant-1"));
        when(session.getId()).thenReturn("session-1");
        when(session.isOpen()).thenReturn(true, false);

        handler.afterConnectionEstablished(session);

        SubscriptionNotification notification = SubscriptionNotification.builder()
                .id(UUID.randomUUID().toString())
                .tenantId("tenant-1")
                .eventType(SubscriptionNotification.EventType.UPDATED)
                .build();

        handler.sendNotification("tenant-1", notification);

        verify(session, org.mockito.Mockito.times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("Should handle send failures gracefully")
    void shouldHandleSendFailuresGracefully() throws Exception {
        SubscriptionWebSocketHandler handler = new SubscriptionWebSocketHandler(new ObjectMapper());
        when(session.getUri()).thenReturn(new URI("ws://localhost/ws/subscriptions?tenant=tenant-1"));
        when(session.getId()).thenReturn("session-1");
        when(session.isOpen()).thenReturn(true);
        org.mockito.Mockito.doThrow(new IOException("fail"))
                .when(session).sendMessage(any(TextMessage.class));

        handler.afterConnectionEstablished(session);

        handler.broadcast("{\"type\":\"test\"}");
    }

    @Test
    @DisplayName("Should skip closed sessions on broadcast")
    void shouldSkipClosedSessionsOnBroadcast() throws Exception {
        SubscriptionWebSocketHandler handler = new SubscriptionWebSocketHandler(new ObjectMapper());
        when(session.getUri()).thenReturn(new URI("ws://localhost/ws/subscriptions?tenant=tenant-1"));
        when(session.getId()).thenReturn("session-1");
        when(session.isOpen()).thenReturn(false);

        handler.afterConnectionEstablished(session);
        handler.broadcast("{\"type\":\"notice\"}");

        verify(session, org.mockito.Mockito.never()).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("Should handle notification serialization failure")
    void shouldHandleNotificationSerializationFailure() throws Exception {
        ObjectMapper mapper = org.mockito.Mockito.mock(ObjectMapper.class);
        SubscriptionWebSocketHandler handler = new SubscriptionWebSocketHandler(mapper);
        when(session.getUri()).thenReturn(new URI("ws://localhost/ws/subscriptions?tenant=tenant-1"));
        when(session.getId()).thenReturn("session-1");
        when(session.isOpen()).thenReturn(true);
        org.mockito.Mockito.when(mapper.writeValueAsString(any()))
                .thenThrow(new RuntimeException("serialize"));

        handler.afterConnectionEstablished(session);

        SubscriptionNotification notification = SubscriptionNotification.builder()
                .id(UUID.randomUUID().toString())
                .tenantId("tenant-1")
                .eventType(SubscriptionNotification.EventType.CREATED)
                .build();

        handler.sendNotification("tenant-1", notification);

        verify(session, org.mockito.Mockito.atLeastOnce()).sendMessage(any(TextMessage.class));
        verify(mapper).writeValueAsString(any());
    }
}
