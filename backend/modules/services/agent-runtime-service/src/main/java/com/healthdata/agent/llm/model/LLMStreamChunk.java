package com.healthdata.agent.llm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Streaming response chunk from LLM providers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LLMStreamChunk {

    /**
     * Chunk type.
     */
    private ChunkType type;

    /**
     * Text content delta.
     */
    private String delta;

    /**
     * Index of the content block (for multi-part responses).
     */
    private int index;

    /**
     * Tool call in progress (if type is TOOL_USE).
     */
    private LLMRequest.ToolCall toolCall;

    /**
     * Final token usage (only in DONE chunk).
     */
    private LLMResponse.TokenUsage usage;

    /**
     * Stop reason (only in DONE chunk).
     */
    private String stopReason;

    /**
     * Chunk types for streaming.
     */
    public enum ChunkType {
        /**
         * Start of message.
         */
        MESSAGE_START,

        /**
         * Start of content block.
         */
        CONTENT_BLOCK_START,

        /**
         * Text content delta.
         */
        CONTENT_BLOCK_DELTA,

        /**
         * End of content block.
         */
        CONTENT_BLOCK_STOP,

        /**
         * Tool use start.
         */
        TOOL_USE_START,

        /**
         * Tool use delta (arguments streaming).
         */
        TOOL_USE_DELTA,

        /**
         * Tool use end.
         */
        TOOL_USE_STOP,

        /**
         * Message delta (stop reason, usage).
         */
        MESSAGE_DELTA,

        /**
         * Stream complete.
         */
        DONE,

        /**
         * Error occurred.
         */
        ERROR
    }

    public static LLMStreamChunk textDelta(String delta) {
        return LLMStreamChunk.builder()
            .type(ChunkType.CONTENT_BLOCK_DELTA)
            .delta(delta)
            .build();
    }

    public static LLMStreamChunk done(LLMResponse.TokenUsage usage, String stopReason) {
        return LLMStreamChunk.builder()
            .type(ChunkType.DONE)
            .usage(usage)
            .stopReason(stopReason)
            .build();
    }

    public static LLMStreamChunk error(String errorMessage) {
        return LLMStreamChunk.builder()
            .type(ChunkType.ERROR)
            .delta(errorMessage)
            .build();
    }
}
