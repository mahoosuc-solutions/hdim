package com.healthdata.agent.memory;

import com.healthdata.agent.core.AgentContext;
import com.healthdata.agent.llm.model.LLMRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Composite memory manager that combines Redis conversation memory
 * with PostgreSQL task persistence.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompositeMemoryManager implements MemoryManager {

    private final RedisConversationMemory conversationMemory;
    private final PostgresTaskMemory taskMemory;

    @Override
    public Mono<Void> storeMessage(AgentContext context, LLMRequest.Message message) {
        return conversationMemory.storeMessage(context, message);
    }

    @Override
    public Mono<Void> storeMessages(AgentContext context, List<LLMRequest.Message> messages) {
        return conversationMemory.storeMessages(context, messages);
    }

    @Override
    public Mono<List<LLMRequest.Message>> getConversationHistory(AgentContext context, int limit) {
        return conversationMemory.getConversationHistory(context, limit);
    }

    @Override
    public Mono<Void> clearConversation(AgentContext context) {
        return conversationMemory.clearConversation(context);
    }

    @Override
    public Mono<Void> storeTaskExecution(AgentContext context, TaskExecution task) {
        return taskMemory.storeTaskExecution(context, task);
    }

    @Override
    public Mono<List<TaskExecution>> getTaskHistory(AgentContext context, int limit) {
        return taskMemory.getTaskHistory(context, limit);
    }

    @Override
    public Mono<MemoryStats> getStats(AgentContext context) {
        return Mono.zip(
            conversationMemory.getConversationSize(context),
            taskMemory.countTasks(context)
        ).map(tuple -> new MemoryStats(
            tuple.getT1().intValue(),
            tuple.getT2(),
            0L, // Size calculation could be added
            0L,
            null, // Could be added with additional queries
            Instant.now()
        ));
    }

    /**
     * Extend conversation TTL to keep it active.
     */
    public Mono<Boolean> keepAlive(AgentContext context) {
        return conversationMemory.extendTtl(context);
    }

    /**
     * Check if a conversation exists for the session.
     */
    public Mono<Boolean> hasActiveConversation(AgentContext context) {
        return conversationMemory.hasConversation(context);
    }

    /**
     * Get user's task history across all sessions.
     */
    public Mono<List<TaskExecution>> getUserTaskHistory(AgentContext context, int limit) {
        return taskMemory.getTaskHistoryByUser(context, limit);
    }
}
