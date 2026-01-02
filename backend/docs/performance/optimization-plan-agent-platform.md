# AI Agent Platform - Performance Optimization Plan

**Version:** 1.0
**Date:** 2025-12-06
**Services:** agent-builder-service (8096), agent-runtime-service (8088)
**Target:** Sub-3-second P95 latency for agent operations

---

## Executive Summary

This document provides a comprehensive performance optimization roadmap for the newly implemented AI Agent Platform. Analysis of the codebase reveals several critical areas requiring immediate optimization to achieve production-ready performance targets.

**Current Architecture:**
- **Agent Builder Service** (8096): No-code agent configuration, versioning, testing
- **Agent Runtime Service** (8088): Agent execution, LLM orchestration, tool integration
- **Database:** PostgreSQL with JSONB columns for flexible configuration storage
- **Cache:** Redis for conversation memory (15-minute TTL)
- **Inter-service:** OpenFeign with circuit breakers and fallbacks

**Key Findings:**
- JSONB queries lack GIN indexes (critical)
- N+1 query patterns in version history retrieval
- Synchronous LLM calls in agent loop (120s timeout)
- Missing cache layers for frequently accessed data
- ObjectMapper serialization overhead in hot paths
- Feign client configuration needs optimization

---

## 1. CRITICAL OPTIMIZATIONS (Do This Week)

### 1.1 Add GIN Indexes for JSONB Columns

**Problem:**
- `agent_configurations.tool_configuration` (JSONB) - queried by tool name
- `agent_configurations.guardrail_configuration` (JSONB) - accessed frequently
- `agent_versions.configuration_snapshot` (JSONB) - large payloads (avg 50-100KB)
- `prompt_templates.variables` (JSONB) - searched by variable name
- `agent_test_sessions.messages`, `tool_invocations`, `metrics` (JSONB)

**Impact:**
- Current: Full table scans on JSONB queries (~500ms for 1000 agents)
- Expected: 95% reduction in query time (~25ms)

**Implementation:**

```sql
-- Migration: V2__add_jsonb_indexes.sql

-- Agent configuration JSONB indexes
CREATE INDEX idx_agent_config_tool_config_gin
ON agent_configurations USING GIN (tool_configuration);

CREATE INDEX idx_agent_config_guardrail_config_gin
ON agent_configurations USING GIN (guardrail_configuration);

CREATE INDEX idx_agent_config_ui_config_gin
ON agent_configurations USING GIN (ui_configuration);

-- Agent version snapshot index (for version comparison queries)
CREATE INDEX idx_agent_version_snapshot_gin
ON agent_versions USING GIN (configuration_snapshot);

-- Prompt template variables index
CREATE INDEX idx_prompt_template_variables_gin
ON prompt_templates USING GIN (variables);

-- Test session JSONB indexes
CREATE INDEX idx_test_session_messages_gin
ON agent_test_sessions USING GIN (messages);

CREATE INDEX idx_test_session_metrics_gin
ON agent_test_sessions USING GIN (metrics);

-- Specific path indexes for common queries
CREATE INDEX idx_agent_config_enabled_tools
ON agent_configurations USING GIN ((tool_configuration -> 'enabled'));

-- Comment for operational visibility
COMMENT ON INDEX idx_agent_config_tool_config_gin IS
'GIN index for fast JSONB queries on tool configurations';
```

**Rollback Strategy:**
```sql
-- Rollback: Drop indexes if causing lock contention during migration
DROP INDEX CONCURRENTLY IF EXISTS idx_agent_config_tool_config_gin;
DROP INDEX CONCURRENTLY IF EXISTS idx_agent_config_guardrail_config_gin;
-- ... repeat for all indexes
```

**Effort Estimate:** 2 hours (including testing on production replica)

---

### 1.2 Fix N+1 Query in Version History Retrieval

**Problem:**

Current code in `AgentVersionService.java`:
```java
// CURRENT - N+1 QUERY PATTERN
List<AgentVersion> history = versionRepository.findByAgentConfigurationIdOrderByCreatedAtDesc(agentId);
// Each version lazy-loads AgentConfiguration when accessed
```

In `AgentVersionRepository.java`:
```java
@Entity
@Table(name = "agent_versions")
public class AgentVersion {
    @ManyToOne(fetch = FetchType.LAZY)  // ❌ LAZY causes N+1
    @JoinColumn(name = "agent_configuration_id", nullable = false)
    private AgentConfiguration agentConfiguration;
}
```

**Impact:**
- Current: 1 query + N queries for 20 versions = 21 queries (~800ms)
- Expected: 1 query with JOIN (~50ms)

**Implementation:**

```java
// OPTIMIZED - Add to AgentVersionRepository.java

@Query("""
    SELECT v FROM AgentVersion v
    JOIN FETCH v.agentConfiguration
    WHERE v.agentConfiguration.id = :agentId
    ORDER BY v.createdAt DESC
    """)
List<AgentVersion> findByAgentConfigurationIdWithConfiguration(@Param("agentId") UUID agentConfigurationId);

@Query("""
    SELECT v FROM AgentVersion v
    JOIN FETCH v.agentConfiguration
    WHERE v.agentConfiguration.id = :agentId
    ORDER BY v.createdAt DESC
    """)
Page<AgentVersion> findByAgentConfigurationIdWithConfiguration(
    @Param("agentId") UUID agentConfigurationId,
    Pageable pageable
);
```

Update service layer:
```java
// BEFORE
public List<AgentVersion> getVersionHistory(UUID agentId) {
    return versionRepository.findByAgentConfigurationIdOrderByCreatedAtDesc(agentId);
}

// AFTER
public List<AgentVersion> getVersionHistory(UUID agentId) {
    return versionRepository.findByAgentConfigurationIdWithConfiguration(agentId);
}
```

**Rollback Strategy:**
- Revert to original method calls
- No schema changes required

**Effort Estimate:** 1 hour

---

### 1.3 Add Caching for Frequently Accessed Data

**Problem:**
- Prompt templates queried on every agent build (10-50 templates per tenant)
- Active agents list queried repeatedly for UI dropdowns
- LLM provider metadata fetched on every execution
- Tool definitions loaded on every agent invocation

**Impact:**
- Current: 200-300ms per template/agent lookup from DB
- Expected: 5-10ms from Redis cache

**Implementation:**

Add Spring Cache configuration:
```java
// CacheConfig.java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // Prompt templates - 30 min TTL
        cacheConfigs.put("promptTemplates", config.entryTtl(Duration.ofMinutes(30)));

        // Active agents - 15 min TTL
        cacheConfigs.put("activeAgents", config.entryTtl(Duration.ofMinutes(15)));

        // Agent configurations - 10 min TTL
        cacheConfigs.put("agentConfigurations", config.entryTtl(Duration.ofMinutes(10)));

        // Tool definitions - 1 hour TTL (rarely changes)
        cacheConfigs.put("toolDefinitions", config.entryTtl(Duration.ofHours(1)));

        // LLM provider metadata - 1 hour TTL
        cacheConfigs.put("llmProviders", config.entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .withInitialCacheConfigurations(cacheConfigs)
            .transactionAware()
            .build();
    }
}
```

Update service methods:
```java
// PromptTemplateService.java - AFTER
@Cacheable(value = "promptTemplates", key = "#tenantId")
public List<PromptTemplate> getAvailableTemplates(String tenantId) {
    return templateRepository.findAvailableTemplates(tenantId);
}

@CacheEvict(value = "promptTemplates", key = "#template.tenantId")
public PromptTemplate create(PromptTemplate template, String userId) {
    // ... existing code
}

// AgentConfigurationService.java - AFTER
@Cacheable(value = "activeAgents", key = "#tenantId")
public List<AgentConfiguration> getActiveAgents(String tenantId) {
    return agentRepository.findActiveAgents(tenantId);
}

@Cacheable(value = "agentConfigurations", key = "#tenantId + ':' + #agentId")
public Optional<AgentConfiguration> getById(String tenantId, UUID agentId) {
    return agentRepository.findByTenantIdAndId(tenantId, agentId);
}

@CacheEvict(value = {"agentConfigurations", "activeAgents"},
            key = "#agent.tenantId + ':' + #agent.id",
            allEntries = true)
public AgentConfiguration update(UUID agentId, AgentConfiguration updates, String userId, String changeSummary) {
    // ... existing code
}
```

Add cache warming on application startup:
```java
@Component
@RequiredArgsConstructor
public class CacheWarmer implements ApplicationListener<ApplicationReadyEvent> {

    private final PromptTemplateService templateService;
    private final AgentConfigurationService agentService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // Warm cache for system templates
        templateService.getAvailableTemplates("system");

        // Optionally warm cache for top tenants
        List<String> topTenants = getTopTenants(); // from config or DB
        topTenants.forEach(tenantId -> {
            agentService.getActiveAgents(tenantId);
        });
    }
}
```

**Rollback Strategy:**
- Remove `@Cacheable` and `@CacheEvict` annotations
- No data loss; falls back to DB queries

**Effort Estimate:** 4 hours

---

### 1.4 Optimize Agent Version Snapshot Storage

**Problem:**
- ObjectMapper serialization on every version save (~50-100ms)
- Large JSONB payloads stored redundantly (full configuration snapshot)
- `createConfigSnapshot()` converts entire AgentConfiguration to Map

Current code:
```java
private Map<String, Object> createConfigSnapshot(AgentConfiguration agent) {
    return objectMapper.convertValue(agent, Map.class);  // ❌ Expensive
}
```

**Impact:**
- Current: 50-100ms per version save
- Expected: 10-20ms with optimized serialization

**Implementation:**

```java
// OPTIMIZED VERSION
@Autowired
private ObjectMapper objectMapper;

// Configure ObjectMapper for performance
@Bean
@Primary
public ObjectMapper optimizedObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);

    // Performance optimizations
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);

    return mapper;
}

// Use custom snapshot creation with field exclusions
private Map<String, Object> createConfigSnapshot(AgentConfiguration agent) {
    try {
        // Exclude heavy fields that don't need versioning
        ObjectWriter writer = objectMapper.writerWithView(SnapshotView.class);
        String json = writer.writeValueAsString(agent);
        return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    } catch (JsonProcessingException e) {
        throw new RuntimeException("Failed to create configuration snapshot", e);
    }
}

// Add JSON View for selective serialization
public static class SnapshotView {
    // Only include essential fields
}

@Entity
@Table(name = "agent_configurations")
@JsonView(SnapshotView.class)
public class AgentConfiguration {
    @Id
    @JsonView(SnapshotView.class)
    private UUID id;

    @JsonView(SnapshotView.class)
    private String name;

    // Exclude relationships from snapshot
    @OneToMany(mappedBy = "agentConfiguration")
    @JsonIgnore
    private List<AgentVersion> versions = new ArrayList<>();
}
```

**Alternative: Delta-based versioning**
```java
// Store only what changed, not full snapshot
private Map<String, Object> createDeltaSnapshot(
    AgentConfiguration current,
    AgentConfiguration previous
) {
    Map<String, Object> delta = new HashMap<>();

    if (!Objects.equals(current.getName(), previous.getName())) {
        delta.put("name", current.getName());
    }
    if (!Objects.equals(current.getSystemPrompt(), previous.getSystemPrompt())) {
        delta.put("systemPrompt", current.getSystemPrompt());
    }
    // ... compare other fields

    return delta;
}
```

**Rollback Strategy:**
- Revert to original `objectMapper.convertValue()` call
- Existing snapshots remain readable

**Effort Estimate:** 3 hours

---

## 2. HIGH PRIORITY (Next Sprint)

### 2.1 Implement Async Processing for Long-Running Operations

**Problem:**
- Agent testing blocks HTTP thread for 60+ seconds
- Version comparison is synchronous and CPU-intensive
- LLM streaming not fully utilized in agent builder

**Impact:**
- Current: Request threads blocked, potential thread pool exhaustion
- Expected: 10x increase in concurrent request capacity

**Implementation:**

```java
// AgentTestService.java - AFTER
@Async("agentTestExecutor")
public CompletableFuture<TestMessageResult> sendTestMessageAsync(
    String tenantId,
    UUID sessionId,
    String message
) {
    return CompletableFuture.supplyAsync(() -> {
        return sendTestMessage(tenantId, sessionId, message);
    });
}

// AsyncConfig.java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "agentTestExecutor")
    public Executor agentTestExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("agent-test-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean(name = "versionComparisonExecutor")
    public Executor versionComparisonExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("version-compare-");
        executor.initialize();
        return executor;
    }
}

// Controller update to use async
@PostMapping("/test/sessions/{sessionId}/message")
public DeferredResult<ResponseEntity<TestMessageResult>> sendTestMessage(
    @PathVariable UUID sessionId,
    @RequestBody MessageRequest request,
    @RequestHeader("X-Tenant-ID") String tenantId
) {
    DeferredResult<ResponseEntity<TestMessageResult>> result = new DeferredResult<>(65000L);

    testService.sendTestMessageAsync(tenantId, sessionId, request.message())
        .thenAccept(testResult -> result.setResult(ResponseEntity.ok(testResult)))
        .exceptionally(ex -> {
            result.setErrorResult(ResponseEntity.internalServerError().build());
            return null;
        });

    return result;
}
```

**Rollback Strategy:**
- Remove `@Async` annotations
- Revert controller to synchronous ResponseEntity returns

**Effort Estimate:** 6 hours

---

### 2.2 Optimize Connection Pool Configuration

**Problem:**
- Agent Builder Service: HikariCP pool too small (max=10, min-idle=2)
- Agent Runtime Service: Pool configured but not tuned for concurrent agent executions
- Redis connection pool default settings

**Impact:**
- Current: Connection starvation under load (>100 concurrent requests)
- Expected: Handle 500+ concurrent requests

**Implementation:**

```yaml
# agent-builder-service/application.yml - AFTER
spring:
  datasource:
    hikari:
      # Increase pool size for concurrent agent operations
      maximum-pool-size: 30
      minimum-idle: 10

      # Connection lifecycle
      max-lifetime: 1800000  # 30 minutes
      connection-timeout: 20000  # 20 seconds
      idle-timeout: 600000  # 10 minutes

      # Performance tuning
      leak-detection-threshold: 60000  # 1 minute
      register-mbeans: true

      # Connection validation
      connection-test-query: SELECT 1
      validation-timeout: 5000

      # Pool optimization
      pool-name: "AgentBuilderHikariPool"
      auto-commit: true
      initialization-fail-timeout: 1

# agent-runtime-service/application.yml - AFTER
spring:
  datasource:
    hikari:
      maximum-pool-size: 50  # Higher for agent runtime
      minimum-idle: 15
      max-lifetime: 1800000
      connection-timeout: 20000
      leak-detection-threshold: 60000
      pool-name: "AgentRuntimeHikariPool"

  data:
    redis:
      lettuce:
        pool:
          max-active: 50  # Increased from 20
          max-idle: 20    # Increased from 10
          min-idle: 10    # Increased from 5
          max-wait: 5000
        shutdown-timeout: 200ms
```

**Monitoring Configuration:**
```yaml
# Add to both services
management:
  metrics:
    export:
      prometheus:
        enabled: true
    enable:
      hikari: true

  endpoint:
    metrics:
      enabled: true
```

**Effort Estimate:** 2 hours

---

### 2.3 Implement Redis Cache Warming Strategy

**Problem:**
- Cold cache on service startup causes initial request slowness
- Cache misses cause thundering herd to database

**Implementation:**

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class AgentPlatformCacheWarmer implements ApplicationListener<ApplicationReadyEvent> {

    private final PromptTemplateService templateService;
    private final AgentConfigurationService agentService;
    private final ToolRegistry toolRegistry;
    private final LLMProviderFactory providerFactory;

    @Value("${hdim.cache.warmup.enabled:true}")
    private boolean warmupEnabled;

    @Value("${hdim.cache.warmup.top-tenants:}")
    private List<String> topTenants;

    @Async
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!warmupEnabled) {
            log.info("Cache warmup disabled");
            return;
        }

        log.info("Starting cache warmup...");
        long startTime = System.currentTimeMillis();

        try {
            // Warm system-level caches
            warmSystemCaches();

            // Warm tenant-level caches
            warmTenantCaches();

            long duration = System.currentTimeMillis() - startTime;
            log.info("Cache warmup completed in {}ms", duration);

        } catch (Exception e) {
            log.error("Cache warmup failed", e);
        }
    }

    private void warmSystemCaches() {
        // Warm tool registry
        toolRegistry.listAvailableTools(AgentContext.system());

        // Warm LLM provider metadata
        providerFactory.listProviders();
        providerFactory.getHealthStatus();

        // Warm system prompt templates
        templateService.getAvailableTemplates("system");
    }

    private void warmTenantCaches() {
        if (topTenants == null || topTenants.isEmpty()) {
            log.info("No top tenants configured for cache warmup");
            return;
        }

        topTenants.parallelStream().forEach(tenantId -> {
            try {
                // Warm active agents
                agentService.getActiveAgents(tenantId);

                // Warm prompt templates
                templateService.getAvailableTemplates(tenantId);

                log.debug("Warmed cache for tenant: {}", tenantId);
            } catch (Exception e) {
                log.error("Failed to warm cache for tenant: {}", tenantId, e);
            }
        });
    }
}
```

Configuration:
```yaml
# application.yml
hdim:
  cache:
    warmup:
      enabled: true
      top-tenants:
        - tenant-1
        - tenant-2
        - tenant-3
```

**Effort Estimate:** 3 hours

---

### 2.4 Add Database Indexes for Common Query Patterns

**Problem:**
- Missing composite indexes for tenant-scoped queries
- Array column searches (tags, allowed_roles) not optimized

**Implementation:**

```sql
-- Migration: V3__add_composite_indexes.sql

-- Composite index for tenant + status filtering (most common query pattern)
CREATE INDEX idx_agent_config_tenant_status
ON agent_configurations(tenant_id, status)
WHERE status IN ('ACTIVE', 'TESTING');

-- Composite index for version queries
CREATE INDEX idx_agent_version_config_created
ON agent_versions(agent_configuration_id, created_at DESC);

-- Index for test session queries
CREATE INDEX idx_test_session_tenant_created
ON agent_test_sessions(tenant_id, created_at DESC);

-- Index for analytics queries
CREATE INDEX idx_agent_analytics_date_range
ON agent_usage_analytics(tenant_id, analytics_date DESC, agent_configuration_id);

-- GIN index for array searches (tags)
CREATE INDEX idx_agent_config_tags_search
ON agent_configurations USING GIN(tags);

-- Index for published agents
CREATE INDEX idx_agent_config_published
ON agent_configurations(tenant_id, published_at DESC)
WHERE status = 'ACTIVE' AND published_at IS NOT NULL;

-- Index for agent test performance queries
CREATE INDEX idx_test_session_metrics_lookup
ON agent_test_sessions(agent_configuration_id, status, completed_at DESC)
WHERE status = 'COMPLETED';

-- Runtime service indexes
CREATE INDEX idx_task_exec_tenant_started
ON agent_task_executions(tenant_id, started_at DESC);

CREATE INDEX idx_task_exec_session_status
ON agent_task_executions(session_id, status);

-- Analyze tables after index creation
ANALYZE agent_configurations;
ANALYZE agent_versions;
ANALYZE agent_test_sessions;
ANALYZE agent_usage_analytics;
```

**Effort Estimate:** 2 hours

---

## 3. MEDIUM PRIORITY (Nice to Have)

### 3.1 Optimize Feign Client Configuration

**Problem:**
- Default Feign connection pool not configured
- Missing request/response compression
- No connection keep-alive optimization

**Implementation:**

```yaml
# agent-builder-service/application.yml
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 60000
        loggerLevel: basic
      agent-runtime-service:
        connectTimeout: 5000
        readTimeout: 120000
        loggerLevel: full

  # Enable compression
  compression:
    request:
      enabled: true
      mime-types: application/json,application/xml
      min-request-size: 2048
    response:
      enabled: true
      useGzipDecoder: true

  # HTTP client optimization
  httpclient:
    enabled: true
    max-connections: 200
    max-connections-per-route: 50
    time-to-live: 900
    time-to-live-unit: seconds
    follow-redirects: true
    connection-timeout: 5000
    connection-timer-repeat: 3000

  # Circuit breaker already configured
  circuitbreaker:
    enabled: true
```

Add OkHttp client for better performance:
```java
@Configuration
public class FeignConfig {

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(50, 5, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)
            .build();
    }
}
```

**Effort Estimate:** 2 hours

---

### 3.2 Implement Query Result Pagination Improvements

**Problem:**
- Version history returns all versions (potentially 100+ per agent)
- Test session list queries load full JSONB payloads

**Implementation:**

```java
// AgentVersionService.java - Add pagination with projection
public interface AgentVersionSummary {
    UUID getId();
    String getVersionNumber();
    String getStatus();
    Instant getCreatedAt();
    String getChangeSummary();
    // Exclude large configuration_snapshot JSONB
}

@Query("""
    SELECT v.id as id, v.versionNumber as versionNumber,
           v.status as status, v.createdAt as createdAt,
           v.changeSummary as changeSummary
    FROM AgentVersion v
    WHERE v.agentConfiguration.id = :agentId
    ORDER BY v.createdAt DESC
    """)
Page<AgentVersionSummary> findVersionSummariesByAgent(
    @Param("agentId") UUID agentId,
    Pageable pageable
);

// Controller update
@GetMapping("/agents/{agentId}/versions")
public ResponseEntity<Page<AgentVersionSummary>> getVersions(
    @PathVariable UUID agentId,
    @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
) {
    Page<AgentVersionSummary> versions = versionService.getVersionSummaries(agentId, pageable);
    return ResponseEntity.ok(versions);
}
```

**Effort Estimate:** 2 hours

---

### 3.3 Enable Response Compression in API Gateway

**Problem:**
- Large JSON payloads (agent configs, version snapshots) sent uncompressed
- No gzip compression on API responses

**Implementation:**

```yaml
# application.yml (both services)
server:
  compression:
    enabled: true
    mime-types:
      - application/json
      - application/xml
      - text/html
      - text/xml
      - text/plain
    min-response-size: 1024  # 1KB
```

**Effort Estimate:** 30 minutes

---

### 3.4 Optimize Redis Memory Usage

**Problem:**
- Conversation memory TTL set to 15 minutes (potentially high memory usage)
- No memory eviction policy configured
- Large encrypted message payloads stored in Redis

**Implementation:**

```yaml
# agent-runtime-service/application.yml
spring:
  data:
    redis:
      # Memory optimization
      lettuce:
        pool:
          max-active: 50
          max-idle: 20
          min-idle: 10

# Add Redis configuration bean
@Configuration
public class RedisConfig {

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(15))
            .disableCachingNullValues()
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new GenericJackson2JsonRedisSerializer())
            )
            // Enable compression for large values
            .computePrefixWith(cacheName -> cacheName + "::");
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
        RedisConnectionFactory connectionFactory
    ) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use compressed serializers
        Jackson2JsonRedisSerializer<Object> serializer =
            new Jackson2JsonRedisSerializer<>(Object.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }
}
```

**Effort Estimate:** 2 hours

---

### 3.5 Add SQL Query Logging and Monitoring

**Problem:**
- No visibility into slow queries
- Missing query performance metrics

**Implementation:**

```yaml
# application.yml
spring:
  jpa:
    properties:
      hibernate:
        # Enable query logging for development
        show_sql: false  # Keep false in production
        format_sql: true
        use_sql_comments: true

        # Performance monitoring
        generate_statistics: true

        # Slow query logging
        session:
          events:
            log:
              LOG_QUERIES_SLOWER_THAN_MS: 100

logging:
  level:
    org.hibernate.SQL: WARN
    org.hibernate.type.descriptor.sql.BasicBinder: WARN
    org.hibernate.stat: INFO

    # Log slow queries
    org.hibernate.engine.internal.StatisticalLoggingSessionEventListener: INFO
```

Add monitoring bean:
```java
@Component
public class HibernateStatisticsMonitor {

    @Autowired
    private EntityManagerFactory emf;

    @Scheduled(fixedRate = 60000)  // Every minute
    public void logStatistics() {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();

        if (stats.isStatisticsEnabled()) {
            log.info("Query execution count: {}", stats.getQueryExecutionCount());
            log.info("Query execution max time: {}ms", stats.getQueryExecutionMaxTime());
            log.info("Query execution max time query: {}", stats.getQueryExecutionMaxTimeQueryString());
            log.info("Second level cache hit count: {}", stats.getSecondLevelCacheHitCount());
        }
    }
}
```

**Effort Estimate:** 1 hour

---

## 4. PERFORMANCE TESTING STRATEGY

### 4.1 Load Testing Scenarios

```yaml
# k6 load test script - agent-builder-service
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '2m', target: 50 },   // Ramp up
    { duration: '5m', target: 100 },  // Sustained load
    { duration: '2m', target: 200 },  // Peak load
    { duration: '2m', target: 0 },    // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<3000'],  // 95% under 3s
    http_req_failed: ['rate<0.01'],     // <1% errors
  },
};

export default function() {
  // Test agent listing
  let listRes = http.get('http://localhost:8096/api/v1/agent-builder/agents', {
    headers: { 'X-Tenant-ID': 'test-tenant' },
  });
  check(listRes, { 'list status 200': (r) => r.status === 200 });

  // Test agent retrieval
  let agentId = 'test-agent-id';
  let getRes = http.get(`http://localhost:8096/api/v1/agent-builder/agents/${agentId}`, {
    headers: { 'X-Tenant-ID': 'test-tenant' },
  });
  check(getRes, { 'get status 200': (r) => r.status === 200 });

  sleep(1);
}
```

### 4.2 Performance Benchmarks

Target metrics after optimizations:

| Operation | Current P95 | Target P95 | Critical Threshold |
|-----------|-------------|------------|--------------------|
| List agents | 500ms | 100ms | 200ms |
| Get agent config | 300ms | 50ms | 100ms |
| Create agent | 800ms | 200ms | 500ms |
| Update agent | 600ms | 150ms | 300ms |
| Get version history | 800ms | 100ms | 200ms |
| Test agent message | 5000ms | 2000ms | 3000ms |
| Agent execution | 8000ms | 3000ms | 5000ms |

---

## 5. ROLLBACK & MONITORING

### 5.1 Rollback Procedures

**Database Changes:**
```bash
# Rollback GIN indexes if causing issues
flyway migrate -target=V1

# Or drop specific indexes
psql -c "DROP INDEX CONCURRENTLY idx_agent_config_tool_config_gin;"
```

**Application Changes:**
```bash
# Revert to previous deployment
kubectl rollout undo deployment/agent-builder-service
kubectl rollout undo deployment/agent-runtime-service

# Or use specific revision
kubectl rollout undo deployment/agent-builder-service --to-revision=2
```

### 5.2 Monitoring Dashboards

**Key Metrics to Track:**

```yaml
# Prometheus metrics
- hikari_connections_active
- hikari_connections_pending
- redis_commands_processed_total
- http_server_requests_seconds_bucket
- jvm_memory_used_bytes
- agent_tasks_total
- agent_task_duration_seconds
```

**Alerts:**
```yaml
# alerts.yml
groups:
  - name: agent_platform_performance
    rules:
      - alert: HighAgentExecutionLatency
        expr: histogram_quantile(0.95, agent_task_duration_seconds_bucket) > 3
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Agent execution P95 latency > 3s"

      - alert: DatabaseConnectionPoolExhausted
        expr: hikari_connections_pending > 5
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Database connection pool exhausted"
```

---

## 6. IMPLEMENTATION TIMELINE

### Week 1 (Critical)
- Day 1-2: Add GIN indexes (1.1)
- Day 3: Fix N+1 queries (1.2)
- Day 4-5: Implement caching (1.3, 1.4)

### Week 2 (High Priority)
- Day 1-2: Async processing (2.1)
- Day 3: Connection pool tuning (2.2)
- Day 4: Cache warming (2.3)
- Day 5: Additional indexes (2.4)

### Week 3 (Medium Priority + Testing)
- Day 1: Feign optimization (3.1)
- Day 2: Pagination improvements (3.2, 3.3)
- Day 3-5: Load testing and validation

---

## 7. SUCCESS CRITERIA

**Performance Targets Met:**
- ✅ P95 latency < 3s for all agent operations
- ✅ Support 500+ concurrent users per service
- ✅ Database query time < 100ms for 95% of queries
- ✅ Cache hit ratio > 80% for frequently accessed data

**Operational Metrics:**
- ✅ No connection pool exhaustion under peak load
- ✅ Zero Redis memory eviction errors
- ✅ < 1% error rate under sustained load
- ✅ Successful rollback test completed

---

## 8. REFERENCES

**Related Documentation:**
- `/home/mahoosuc-solutions/projects/hdim-master/hdim-master/backend/modules/services/agent-builder-service/`
- `/home/mahoosuc-solutions/projects/hdim-master/hdim-master/backend/modules/services/agent-runtime-service/`

**Configuration Files:**
- `agent-builder-service/src/main/resources/application.yml`
- `agent-runtime-service/src/main/resources/application.yml`

**Database Schemas:**
- `agent-builder-service/src/main/resources/db/migration/V1__create_agent_builder_tables.sql`
- `agent-runtime-service/src/main/resources/db/migration/V1__create_agent_tables.sql`

**Key Java Files:**
- `AgentConfigurationService.java` - Agent CRUD operations
- `AgentOrchestrator.java` - Agent execution loop
- `RedisConversationMemory.java` - Redis memory management
- `AgentRuntimeClient.java` - Inter-service communication

---

**Document Owner:** Backend Engineering Team
**Last Updated:** 2025-12-06
**Next Review:** After Week 1 implementation
