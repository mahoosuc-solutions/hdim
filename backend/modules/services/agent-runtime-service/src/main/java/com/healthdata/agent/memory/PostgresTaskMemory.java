package com.healthdata.agent.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.agent.core.AgentContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * PostgreSQL-based task execution memory for long-term persistence.
 * Stores agent task history for auditing, analytics, and learning.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PostgresTaskMemory {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private static final String INSERT_TASK = """
        INSERT INTO agent_task_executions (
            id, tenant_id, session_id, user_id, agent_id, agent_type,
            task_type, input_summary, output_summary, status,
            duration_ms, started_at, completed_at, metadata
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
        """;

    private static final String SELECT_TASKS = """
        SELECT id, tenant_id, session_id, user_id, agent_id, agent_type,
               task_type, input_summary, output_summary, status,
               duration_ms, started_at, completed_at, metadata
        FROM agent_task_executions
        WHERE tenant_id = ? AND session_id = ?
        ORDER BY started_at DESC
        LIMIT ?
        """;

    private static final String SELECT_TASKS_BY_USER = """
        SELECT id, tenant_id, session_id, user_id, agent_id, agent_type,
               task_type, input_summary, output_summary, status,
               duration_ms, started_at, completed_at, metadata
        FROM agent_task_executions
        WHERE tenant_id = ? AND user_id = ?
        ORDER BY started_at DESC
        LIMIT ?
        """;

    private static final String COUNT_TASKS = """
        SELECT COUNT(*) FROM agent_task_executions
        WHERE tenant_id = ? AND session_id = ?
        """;

    /**
     * Store a task execution record.
     */
    public Mono<Void> storeTaskExecution(AgentContext context, MemoryManager.TaskExecution task) {
        return Mono.fromRunnable(() -> {
            try {
                String metadataJson = objectMapper.writeValueAsString(task.metadata());

                jdbcTemplate.update(INSERT_TASK,
                    UUID.fromString(task.taskId()),
                    context.getTenantId(),
                    context.getSessionId(),
                    context.getUserId(),
                    context.getAgentId(),
                    context.getAgentType(),
                    task.taskType(),
                    truncate(task.input(), 1000),
                    truncate(task.output(), 2000),
                    task.status(),
                    task.durationMs(),
                    Timestamp.from(task.startedAt()),
                    task.completedAt() != null ? Timestamp.from(task.completedAt()) : null,
                    metadataJson
                );

                log.debug("Stored task execution: {}", task.taskId());

            } catch (Exception e) {
                log.error("Failed to store task execution: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to store task execution", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * Retrieve task history for a session.
     */
    public Mono<List<MemoryManager.TaskExecution>> getTaskHistory(AgentContext context, int limit) {
        return Mono.fromCallable(() -> {
            return jdbcTemplate.query(SELECT_TASKS,
                taskExecutionRowMapper(),
                context.getTenantId(),
                context.getSessionId(),
                limit
            );
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Retrieve task history for a user across sessions.
     */
    public Mono<List<MemoryManager.TaskExecution>> getTaskHistoryByUser(AgentContext context, int limit) {
        return Mono.fromCallable(() -> {
            return jdbcTemplate.query(SELECT_TASKS_BY_USER,
                taskExecutionRowMapper(),
                context.getTenantId(),
                context.getUserId(),
                limit
            );
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Count tasks in a session.
     */
    public Mono<Integer> countTasks(AgentContext context) {
        return Mono.fromCallable(() -> {
            return jdbcTemplate.queryForObject(COUNT_TASKS, Integer.class,
                context.getTenantId(), context.getSessionId());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private RowMapper<MemoryManager.TaskExecution> taskExecutionRowMapper() {
        return (rs, rowNum) -> {
            Map<String, Object> metadata;
            try {
                String metadataJson = rs.getString("metadata");
                metadata = metadataJson != null
                    ? objectMapper.readValue(metadataJson, Map.class)
                    : Map.of();
            } catch (Exception e) {
                metadata = Map.of();
            }

            Timestamp completedAt = rs.getTimestamp("completed_at");

            return new MemoryManager.TaskExecution(
                rs.getString("id"),
                rs.getString("task_type"),
                rs.getString("input_summary"),
                rs.getString("output_summary"),
                rs.getString("status"),
                rs.getLong("duration_ms"),
                rs.getTimestamp("started_at").toInstant(),
                completedAt != null ? completedAt.toInstant() : null,
                metadata
            );
        };
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength - 3) + "...";
    }
}
