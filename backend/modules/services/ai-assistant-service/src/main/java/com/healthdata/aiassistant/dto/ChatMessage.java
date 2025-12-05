package com.healthdata.aiassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single message in a chat conversation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    /**
     * Role of the message sender.
     * Values: "user", "assistant"
     */
    private String role;

    /**
     * Content of the message.
     */
    private String content;

    /**
     * Timestamp when message was created.
     */
    private String timestamp;

    /**
     * Create a user message.
     */
    public static ChatMessage user(String content) {
        return ChatMessage.builder()
            .role("user")
            .content(content)
            .build();
    }

    /**
     * Create an assistant message.
     */
    public static ChatMessage assistant(String content) {
        return ChatMessage.builder()
            .role("assistant")
            .content(content)
            .build();
    }
}
