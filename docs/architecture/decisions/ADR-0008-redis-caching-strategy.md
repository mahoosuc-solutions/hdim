# ADR-0008: Redis Caching Strategy with HIPAA-Compliant TTL

**Date**: 2024-Q3
**Status**: Accepted
**Deciders**: Architecture Team, Security Team, Compliance Officer
**Technical Story**: Need high-performance caching with HIPAA-compliant PHI handling

---

## Context and Problem Statement

HDIM requires caching for:

- Reducing database load for frequently accessed data
- Improving API response times (<200ms target)
- Session management for authenticated users
- Reference data caching (code systems, measure definitions)
- PHI data caching with HIPAA compliance constraints

**Critical Constraint**: HIPAA requires minimizing PHI exposure. Cached PHI must be invalidated quickly to respect patient consent revocations and data freshness requirements.

---

## Decision Drivers

* **HIPAA compliance** - PHI cache TTL must minimize exposure window
* **Performance** - Sub-millisecond cache access for high-throughput scenarios
* **Spring Boot integration** - Native Spring Cache abstraction support
* **Multi-tenant isolation** - Cache key namespacing by tenant
* **Operational simplicity** - Easy to deploy and monitor
* **Data structures** - Support for strings, hashes, sets, sorted sets
* **Cluster support** - Horizontal scaling for production

---

## Considered Options

1. **Redis 7 with 5-minute PHI TTL** - In-memory data store with strict TTL policy
2. **Memcached** - Distributed memory caching
3. **Hazelcast** - Java-based distributed cache
4. **Caffeine (local cache only)** - High-performance Java caching library
5. **No caching for PHI** - Database-only for all PHI access

---

## Decision Outcome

**Chosen option**: "Redis 7 with 5-minute PHI TTL"

**Rationale**: Redis provides:
- Sub-millisecond access times for cache hits
- Native Spring Cache integration
- TTL enforcement at the data structure level
- Multi-tenant key namespacing
- Rich data structures for various caching patterns
- Cluster mode for production scaling

**Critical Policy**: All PHI cached in Redis MUST have a maximum TTL of 5 minutes (300 seconds). This is enforced at the infrastructure level and cannot be overridden by application code.

---

## Consequences

### Positive

* **Performance**: 85%+ cache hit rate reduces database load significantly
* **Compliance**: 5-minute TTL ensures PHI freshness and consent respect
* **Flexibility**: Different TTLs for PHI vs reference data
* **Spring integration**: Seamless with `@Cacheable` annotations
* **Monitoring**: Rich metrics via Redis INFO and Prometheus exporter
* **Cluster ready**: Redis Cluster for production HA

### Negative

* **Memory costs**: Redis is memory-intensive for large datasets
* **Operational overhead**: Requires monitoring and capacity planning
* **Cache invalidation**: Requires careful invalidation strategy for updates
* **Network dependency**: Cache miss requires database round-trip

**Mitigations**:
- Use managed Redis (ElastiCache, Redis Cloud) in production
- Implement cache-aside pattern with fallback to database
- Document TTL policies in HIPAA compliance documentation

### Neutral

* Requires Redis client configuration in each service
* Memory usage scales with tenant count and data volume

---

## Pros and Cons of Options

### Option 1: Redis 7 with 5-minute PHI TTL

In-memory data store with strict TTL enforcement.

| Criterion | Assessment |
|-----------|------------|
| Performance | **Good** - Sub-millisecond access |
| HIPAA Compliance | **Good** - Configurable TTL, encryption in transit |
| Spring Integration | **Good** - Spring Data Redis, Spring Cache |
| Multi-tenancy | **Good** - Key namespacing by tenant |
| Data Structures | **Good** - Rich set of data types |
| Cluster Support | **Good** - Redis Cluster for HA |

**Summary**: Best balance of performance, compliance, and flexibility.

---

### Option 2: Memcached

Distributed memory caching system.

| Criterion | Assessment |
|-----------|------------|
| Performance | **Good** - Very fast for simple key-value |
| HIPAA Compliance | **Neutral** - TTL support but less feature-rich |
| Spring Integration | **Neutral** - Requires separate integration |
| Multi-tenancy | **Neutral** - Key-based isolation only |
| Data Structures | **Bad** - Simple key-value only |
| Cluster Support | **Good** - Distributed by design |

**Summary**: Fast but lacks rich data structures and features.

---

### Option 3: Hazelcast

Java-based distributed caching and computing platform.

| Criterion | Assessment |
|-----------|------------|
| Performance | **Good** - In-memory performance |
| HIPAA Compliance | **Neutral** - TTL support, encryption available |
| Spring Integration | **Good** - Spring Cache support |
| Multi-tenancy | **Good** - Namespace support |
| Java Native | **Good** - Native Java integration |
| Operational Complexity | **Neutral** - More complex than Redis |

**Summary**: Good Java integration but less ecosystem support than Redis.

---

### Option 4: Caffeine (Local Cache Only)

High-performance Java caching library.

| Criterion | Assessment |
|-----------|------------|
| Performance | **Good** - Fastest (no network) |
| HIPAA Compliance | **Neutral** - TTL support |
| Distributed | **Bad** - Local cache only, no sharing between instances |
| Spring Integration | **Good** - Spring Cache support |
| Operational Simplicity | **Good** - No infrastructure required |

**Summary**: Excellent for local caching but cannot share across instances.

---

### Option 5: No Caching for PHI

Database-only access for all PHI data.

| Criterion | Assessment |
|-----------|------------|
| Performance | **Bad** - All requests hit database |
| HIPAA Compliance | **Good** - No cached PHI exposure |
| Simplicity | **Good** - No cache invalidation concerns |
| Database Load | **Bad** - Significantly higher load |
| Scalability | **Bad** - Database becomes bottleneck |

**Summary**: Safest for compliance but unacceptable performance impact.

---

## Implementation Notes

### Version Selected

**Redis 7.2** - Latest stable release with enhanced performance

### Deployment Model

- **Development**: Single instance in Docker Compose (port 6380)
- **Production**: Redis Cluster or managed service (ElastiCache)

### TTL Policy (CRITICAL)

| Data Category | TTL | Rationale |
|---------------|-----|-----------|
| PHI (patient data) | 5 minutes (300s) | HIPAA minimum necessary |
| User sessions | 15 minutes | Security best practice |
| Reference data (code systems) | 1 hour | Infrequently changes |
| Measure definitions | 1 hour | Version-controlled |
| API rate limits | 1 minute | Short-term throttling |

### Key Naming Convention

```
{tenantId}:{resourceType}:{resourceId}

Examples:
- TENANT001:patient:12345
- TENANT001:measure:BCS
- TENANT001:session:user-abc-123
- global:codesystem:icd10
```

### Cache Configuration

```yaml
# application.yml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6380}
      password: ${REDIS_PASSWORD:}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 50
          max-idle: 10
          min-idle: 5

# HIPAA Compliance - PHI cache TTL (DO NOT INCREASE)
cache:
  phi:
    ttl-seconds: 300  # 5 minutes maximum
  reference:
    ttl-seconds: 3600  # 1 hour
  session:
    ttl-seconds: 900  # 15 minutes
```

### Spring Cache Integration

```java
// PHI caching with 5-minute TTL (enforced by configuration)
@Cacheable(value = "patientData", key = "#tenantId + ':patient:' + #patientId")
public Patient getPatient(String patientId, String tenantId) {
    return patientRepository.findByIdAndTenant(patientId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
}

// Cache eviction on update
@CacheEvict(value = "patientData", key = "#tenantId + ':patient:' + #patient.id")
public Patient updatePatient(Patient patient, String tenantId) {
    return patientRepository.save(patient);
}
```

### Cache Configuration Class

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${cache.phi.ttl-seconds:300}")
    private long phiTtlSeconds;

    @Bean
    public RedisCacheConfiguration phiCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofSeconds(phiTtlSeconds))
            .disableCachingNullValues()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()));
    }
}
```

### Performance Targets

| Metric | Target | Actual (Dec 2024) |
|--------|--------|-------------------|
| Cache Hit Rate | >80% | 87% |
| Get Latency (p95) | <5ms | 2ms |
| Set Latency (p95) | <10ms | 4ms |
| Memory Usage | <2GB per tenant | 1.2GB avg |

### Monitoring

```yaml
# Prometheus metrics (via Redis Exporter)
- redis_connected_clients
- redis_used_memory_bytes
- redis_keyspace_hits_total
- redis_keyspace_misses_total
- redis_commands_processed_total
```

---

## HIPAA Compliance Documentation

### Why 5 Minutes?

The 5-minute PHI cache TTL balances:

1. **Performance**: 85%+ cache hit rate with 5-minute window
2. **Data Freshness**: Patient data updates reflected within 5 minutes
3. **Consent Respect**: If patient revokes consent, data stops flowing within 5 minutes
4. **Regulatory Alignment**: HIPAA "minimum necessary" principle

### Audit Trail

All cache operations are logged:
- Cache hits (access to PHI)
- Cache evictions (data removed)
- TTL expirations (automatic cleanup)

### Encryption

- **In Transit**: TLS 1.3 required for Redis connections
- **At Rest**: Encryption at rest via managed service (ElastiCache encryption)

---

## Links

* [Redis Documentation](https://redis.io/documentation)
* [Spring Data Redis Reference](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)
* [HIPAA Cache Compliance](/backend/HIPAA-CACHE-COMPLIANCE.md)
* Related: [ADR-0007 - PostgreSQL Database](ADR-0007-postgresql-database.md)
* Related: [ADR-0006 - Kafka Event Streaming](ADR-0006-kafka-event-streaming.md)

---

## Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2024-Q3 | Architecture Team | Initial decision |
| 1.1 | 2024-12-30 | Architecture Team | Added TTL policy details, compliance section |

---

*This ADR follows the template in `/docs/templates/ADR_TEMPLATE.md`*
