package com.healthdata.agent.memory;

import com.healthdata.agent.core.AgentContext;
import com.healthdata.agent.llm.model.LLMRequest;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Interface for managing agent memory (conversation and task history).
 */
public interface MemoryManager {

    /**
     * Store a message in conversation memory.
     */
    Mono<Void> storeMessage(AgentContext context, LLMRequest.Message message);

    /**
     * Store multiple messages in conversation memory.
     */
    Mono<Void> storeMessages(AgentContext context, List<LLMRequest.Message> messages);

    /**
     * Retrieve conversation history for a session.
     */
    Mono<List<LLMRequest.Message>> getConversationHistory(AgentContext context, int limit);

    /**
     * Clear conversation memory for a session.
     */
    Mono<Void> clearConversation(AgentContext context);

    /**
     * Store task execution record.
     */
    Mono<Void> storeTaskExecution(AgentContext context, TaskExecution task);

    /**
     * Retrieve task execution history.
     */
    Mono<List<TaskExecution>> getTaskHistory(AgentContext context, int limit);

    /**
     * Get memory usage statistics.
     */
    Mono<MemoryStats> getStats(AgentContext context);

    /**
     * Task execution record.
     */
    record TaskExecution(
        String taskId,
        String taskType,
        String input,
        String output,
        String status,
        long durationMs,
        java.time.Instant startedAt,
        java.time.Instant completedAt,
        java.util.Map<String, Object> metadata
    ) {
        public static TaskExecution create(String taskId, String taskType, String input) {
            return new TaskExecution(
                taskId, taskType, input, null, "IN_PROGRESS",
                0, java.time.Instant.now(), null, new java.util.HashMap<>()
            );
        }

        public TaskExecution complete(String output, String status) {
            java.time.Instant now = java.time.Instant.now();
            return new TaskExecution(
                taskId, taskType, input, output, status,
                java.time.Duration.between(startedAt, now).toMillis(),
                startedAt, now, metadata
            );
        }
    }

    /**
     * Memory statistics.
     */
    record MemoryStats(
        int conversationMessageCount,
        int taskExecutionCount,
        long conversationMemorySizeBytes,
        long taskMemorySizeBytes,
        java.time.Instant oldestMessage,
        java.time.Instant newestMessage
    ) {}
}
