package com.healthdata.aiassistant.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChatMessage")
class ChatMessageTest {

    @Test
    @DisplayName("Should build user and assistant messages")
    void shouldBuildHelperMessages() {
        ChatMessage user = ChatMessage.user("hello");
        ChatMessage assistant = ChatMessage.assistant("hi");

        assertThat(user.getRole()).isEqualTo("user");
        assertThat(user.getContent()).isEqualTo("hello");
        assertThat(assistant.getRole()).isEqualTo("assistant");
        assertThat(assistant.getContent()).isEqualTo("hi");
    }

    @Test
    @DisplayName("Should support builder properties")
    void shouldSupportBuilder() {
        ChatMessage message = ChatMessage.builder()
            .role("system")
            .content("note")
            .timestamp("2025-01-01T00:00:00Z")
            .build();

        assertThat(message.getRole()).isEqualTo("system");
        assertThat(message.getContent()).isEqualTo("note");
        assertThat(message.getTimestamp()).isEqualTo("2025-01-01T00:00:00Z");
    }
}
