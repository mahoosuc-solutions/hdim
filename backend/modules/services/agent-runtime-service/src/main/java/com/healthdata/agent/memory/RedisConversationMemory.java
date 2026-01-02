package com.healthdata.agent.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.agent.core.AgentContext;
import com.healthdata.agent.llm.model.LLMRequest;
import com.healthdata.agent.security.PHIEncryption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Redis-based conversation memory with PHI encryption.
 * Provides short-term memory for agent conversations with automatic expiration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisConversationMemory {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final PHIEncryption phiEncryption;

    @Value("${hdim.agent.memory.conversation.ttl-minutes:15}")
    private int conversationTtlMinutes;

    @Value("${hdim.agent.memory.conversation.max-messages:100}")
    private int maxMessages;

    private static final String KEY_PREFIX = "agent:conversation:";

    /**
     * Store a message in conversation memory.
     */
    public Mono<Void> storeMessage(AgentContext context, LLMRequest.Message message) {
        String key = buildKey(context);
        String messageJson = serializeMessage(message);
        String encryptedMessage = phiEncryption.encrypt(messageJson, context.getTenantId());

        return redisTemplate.opsForList()
            .rightPush(key, encryptedMessage)
            .flatMap(size -> {
                // Trim to max messages
                if (size > maxMessages) {
                    return redisTemplate.opsForList()
                        .trim(key, size - maxMessages, -1)
                        .then();
                }
                return Mono.empty();
            })
            .then(redisTemplate.expire(key, Duration.ofMinutes(conversationTtlMinutes)))
            .then()
            .doOnSuccess(v -> log.debug("Stored message for session: {}", context.getSessionId()))
            .doOnError(e -> log.error("Failed to store message: {}", e.getMessage()));
    }

    /**
     * Store multiple messages.
     */
    public Mono<Void> storeMessages(AgentContext context, List<LLMRequest.Message> messages) {
        return Flux.fromIterable(messages)
            .flatMap(msg -> storeMessage(context, msg))
            .then();
    }

    /**
     * Retrieve conversation history.
     */
    public Mono<List<LLMRequest.Message>> getConversationHistory(AgentContext context, int limit) {
        String key = buildKey(context);
        int effectiveLimit = Math.min(limit, maxMessages);

        return redisTemplate.opsForList()
            .range(key, -effectiveLimit, -1)
            .map(encrypted -> {
                String decrypted = phiEncryption.decrypt(encrypted, context.getTenantId());
                return deserializeMessage(decrypted);
            })
            .collectList()
            .doOnSuccess(msgs -> log.debug("Retrieved {} messages for session: {}",
                msgs.size(), context.getSessionId()));
    }

    /**
     * Clear conversation memory.
     */
    public Mono<Void> clearConversation(AgentContext context) {
        String key = buildKey(context);
        return redisTemplate.delete(key)
            .then()
            .doOnSuccess(v -> log.info("Cleared conversation memory for session: {}", context.getSessionId()));
    }

    /**
     * Get conversation size.
     */
    public Mono<Long> getConversationSize(AgentContext context) {
        String key = buildKey(context);
        return redisTemplate.opsForList().size(key);
    }

    /**
     * Extend conversation TTL.
     */
    public Mono<Boolean> extendTtl(AgentContext context) {
        String key = buildKey(context);
        return redisTemplate.expire(key, Duration.ofMinutes(conversationTtlMinutes));
    }

    /**
     * Check if conversation exists.
     */
    public Mono<Boolean> hasConversation(AgentContext context) {
        String key = buildKey(context);
        return redisTemplate.hasKey(key);
    }

    private String buildKey(AgentContext context) {
        return KEY_PREFIX + context.getTenantId() + ":" + context.getSessionId();
    }

    private String serializeMessage(LLMRequest.Message message) {
        try {
            return objectMapper.writeValueAsString(new MessageRecord(
                message.getRole(),
                message.getContent(),
                Instant.now().toString()
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize message", e);
        }
    }

    private LLMRequest.Message deserializeMessage(String json) {
        try {
            MessageRecord record = objectMapper.readValue(json, MessageRecord.class);
            return LLMRequest.Message.builder()
                .role(record.role())
                .content(record.content())
                .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize message", e);
        }
    }

    /**
     * Internal message record for serialization.
     */
    private record MessageRecord(String role, String content, String timestamp) {}
}
