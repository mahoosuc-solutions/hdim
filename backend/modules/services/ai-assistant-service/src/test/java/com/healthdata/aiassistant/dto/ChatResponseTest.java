package com.healthdata.aiassistant.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChatResponse")
class ChatResponseTest {

    @Test
    @DisplayName("Should apply default builder values")
    void shouldApplyDefaults() {
        ChatResponse response = ChatResponse.builder().build();

        assertThat(response.isCached()).isFalse();
        assertThat(response.isError()).isFalse();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should allow all fields to be set")
    void shouldSetFields() {
        Instant now = Instant.parse("2025-01-01T00:00:00Z");

        ChatResponse response = ChatResponse.builder()
            .id("resp-1")
            .queryType("care_gaps")
            .response("Result")
            .model("model-1")
            .inputTokens(10)
            .outputTokens(20)
            .cached(true)
            .timestamp(now)
            .processingTimeMs(150)
            .error(true)
            .errorMessage("failure")
            .metadata(Map.of("source", "test"))
            .sessionId("session-1")
            .suggestions(new String[] {"a", "b"})
            .build();

        assertThat(response.getId()).isEqualTo("resp-1");
        assertThat(response.getQueryType()).isEqualTo("care_gaps");
        assertThat(response.getResponse()).isEqualTo("Result");
        assertThat(response.getModel()).isEqualTo("model-1");
        assertThat(response.getInputTokens()).isEqualTo(10);
        assertThat(response.getOutputTokens()).isEqualTo(20);
        assertThat(response.isCached()).isTrue();
        assertThat(response.getTimestamp()).isEqualTo(now);
        assertThat(response.getProcessingTimeMs()).isEqualTo(150);
        assertThat(response.isError()).isTrue();
        assertThat(response.getErrorMessage()).isEqualTo("failure");
        assertThat(response.getMetadata()).containsEntry("source", "test");
        assertThat(response.getSessionId()).isEqualTo("session-1");
        assertThat(response.getSuggestions()).containsExactly("a", "b");
    }
}
