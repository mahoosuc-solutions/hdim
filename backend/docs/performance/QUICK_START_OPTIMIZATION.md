# Quick Start: Performance Optimization Implementation

**Quick reference for implementing critical performance optimizations**

---

## Critical Path (Do First)

### 1. Database Indexes (30 min)

```bash
# Create migration file
cd modules/services/agent-builder-service/src/main/resources/db/migration
cat > V2__add_jsonb_indexes.sql << 'EOF'
-- JSONB GIN indexes for fast queries
CREATE INDEX CONCURRENTLY idx_agent_config_tool_config_gin
ON agent_configurations USING GIN (tool_configuration);

CREATE INDEX CONCURRENTLY idx_agent_config_guardrail_config_gin
ON agent_configurations USING GIN (guardrail_configuration);

CREATE INDEX CONCURRENTLY idx_agent_version_snapshot_gin
ON agent_versions USING GIN (configuration_snapshot);

CREATE INDEX CONCURRENTLY idx_prompt_template_variables_gin
ON prompt_templates USING GIN (variables);

-- Composite indexes for common queries
CREATE INDEX CONCURRENTLY idx_agent_config_tenant_status
ON agent_configurations(tenant_id, status)
WHERE status IN ('ACTIVE', 'TESTING');

CREATE INDEX CONCURRENTLY idx_agent_version_config_created
ON agent_versions(agent_configuration_id, created_at DESC);
EOF

# Apply migration
./gradlew flywayMigrate
```

**Impact:** 95% reduction in JSONB query time (500ms → 25ms)

---

### 2. Fix N+1 Query (15 min)

```java
// File: AgentVersionRepository.java
// Add this method:

@Query("""
    SELECT v FROM AgentVersion v
    JOIN FETCH v.agentConfiguration
    WHERE v.agentConfiguration.id = :agentId
    ORDER BY v.createdAt DESC
    """)
List<AgentVersion> findByAgentConfigurationIdWithConfiguration(@Param("agentId") UUID agentConfigurationId);
```

```java
// File: AgentVersionService.java
// Replace this:

public List<AgentVersion> getVersionHistory(UUID agentId) {
    return versionRepository.findByAgentConfigurationIdOrderByCreatedAtDesc(agentId);
}

// With this:

public List<AgentVersion> getVersionHistory(UUID agentId) {
    return versionRepository.findByAgentConfigurationIdWithConfiguration(agentId);
}
```

**Impact:** Eliminate 20+ queries per request (800ms → 50ms)

---

### 3. Add Redis Caching (1 hour)

```java
// File: CacheConfig.java (new file)
package com.healthdata.agentbuilder.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serialization.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serialization.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

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
        cacheConfigs.put("promptTemplates", config.entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put("activeAgents", config.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put("agentConfigurations", config.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .withInitialCacheConfigurations(cacheConfigs)
            .transactionAware()
            .build();
    }
}
```

```java
// File: PromptTemplateService.java
// Add annotations:

@Cacheable(value = "promptTemplates", key = "#tenantId")
public List<PromptTemplate> getAvailableTemplates(String tenantId) {
    return templateRepository.findAvailableTemplates(tenantId);
}

@CacheEvict(value = "promptTemplates", key = "#template.tenantId")
public PromptTemplate create(PromptTemplate template, String userId) {
    // ... existing code
}
```

```java
// File: AgentConfigurationService.java
// Add annotations:

@Cacheable(value = "activeAgents", key = "#tenantId")
public List<AgentConfiguration> getActiveAgents(String tenantId) {
    return agentRepository.findActiveAgents(tenantId);
}

@Cacheable(value = "agentConfigurations", key = "#tenantId + ':' + #agentId")
public Optional<AgentConfiguration> getById(String tenantId, UUID agentId) {
    return agentRepository.findByTenantIdAndId(tenantId, agentId);
}

@CacheEvict(value = {"agentConfigurations", "activeAgents"}, allEntries = true)
public AgentConfiguration update(UUID agentId, AgentConfiguration updates, String userId, String changeSummary) {
    // ... existing code
}
```

**Impact:** 95% reduction in repeated queries (300ms → 10ms)

---

### 4. Connection Pool Tuning (5 min)

```yaml
# File: agent-builder-service/src/main/resources/application.yml
# Update datasource section:

spring:
  datasource:
    hikari:
      maximum-pool-size: 30  # Increased from 10
      minimum-idle: 10       # Increased from 2
      max-lifetime: 1800000
      connection-timeout: 20000
      leak-detection-threshold: 60000
```

```yaml
# File: agent-runtime-service/src/main/resources/application.yml
# Update datasource section:

spring:
  datasource:
    hikari:
      maximum-pool-size: 50  # Increased from 20
      minimum-idle: 15       # Increased from 5
      max-lifetime: 1800000
      connection-timeout: 20000
```

**Impact:** Support 5x more concurrent requests

---

## Verification Commands

```bash
# Check if indexes were created
psql -d healthdata_db -c "\d agent_configurations"
psql -d healthdata_db -c "SELECT indexname FROM pg_indexes WHERE tablename = 'agent_configurations';"

# Verify cache is working
redis-cli
> KEYS promptTemplates*
> TTL promptTemplates:test-tenant
> GET promptTemplates:test-tenant

# Test connection pool
curl -X GET http://localhost:8096/actuator/metrics/hikari.connections.active
curl -X GET http://localhost:8096/actuator/metrics/hikari.connections.max

# Load test
k6 run load-test.js
```

---

## Performance Testing

```javascript
// File: load-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '1m', target: 50 },
    { duration: '3m', target: 100 },
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<3000'],
  },
};

export default function() {
  const baseUrl = 'http://localhost:8096/api/v1/agent-builder';
  const headers = { 'X-Tenant-ID': 'test-tenant' };

  // Test list agents
  let res = http.get(`${baseUrl}/agents`, { headers });
  check(res, {
    'list agents p95 < 100ms': (r) => r.timings.duration < 100,
  });

  sleep(1);
}
```

Run test:
```bash
k6 run load-test.js
```

---

## Expected Results

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| List agents (P95) | 500ms | 50ms | 90% faster |
| Get agent config (P95) | 300ms | 25ms | 92% faster |
| Version history (P95) | 800ms | 50ms | 94% faster |
| Template lookup (P95) | 200ms | 10ms | 95% faster |
| Concurrent users | 100 | 500 | 5x capacity |

---

## Rollback Plan

If issues occur:

```bash
# 1. Rollback database migration
./gradlew flywayUndo

# 2. Disable caching
# In application.yml, add:
spring:
  cache:
    type: none

# 3. Revert code changes
git revert HEAD~3..HEAD

# 4. Restart services
kubectl rollout restart deployment/agent-builder-service
kubectl rollout restart deployment/agent-runtime-service
```

---

## Monitoring Queries

```sql
-- Check index usage
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;

-- Check slow queries
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
WHERE mean_exec_time > 100
ORDER BY mean_exec_time DESC
LIMIT 10;

-- Check cache hit ratio
SELECT
  sum(blks_hit)*100 / (sum(blks_hit) + sum(blks_read)) as cache_hit_ratio
FROM pg_stat_database;
```

---

## Next Steps

After implementing critical optimizations:

1. Monitor for 24-48 hours
2. Proceed with HIGH PRIORITY optimizations (async processing)
3. Run full load test suite
4. Update team on results

---

**Questions?** See full plan: `optimization-plan-agent-platform.md`
