# ADR-0005: PostgreSQL Read Replicas for Scalability

**Status**: Accepted
**Date**: 2025-12-06
**Decision Makers**: Architecture Team
**Consulted**: Database Team, SRE
**Informed**: All Development Teams

## Context

**Problem Statement**:
The HDIM platform faced database scalability challenges:

1. Single PostgreSQL instance handling all read/write traffic
2. HEDIS measure calculations perform heavy read queries
3. Reporting and analytics compete with transactional workloads
4. No read scaling without vertical scaling (expensive)

**Business Context**:
- HEDIS quarterly reporting generates massive read load
- Real-time dashboards require responsive queries
- Cost optimization needed vs. continued vertical scaling
- 99.9% availability SLA requires database redundancy

**Technical Context**:
- PostgreSQL 15 as primary database
- ~500GB database size across all tenants
- Read-heavy workload (80% reads, 20% writes)
- Spring Data JPA for data access
- Connection pooling via HikariCP

## Decision

**We will implement PostgreSQL streaming replication with read replicas and application-level read/write routing.**

**Specific Implementation**:

1. **Replication Topology**:
   - 1 Primary (read-write)
   - 2 Replicas (read-only)
   - Synchronous replication for durability
   - PgBouncer for connection pooling

2. **Routing Strategy**:
   - Write operations → Primary
   - Read operations in transactions → Primary (consistency)
   - Read-only queries → Replicas (round-robin)
   - Explicit `@Transactional(readOnly = true)` for routing

3. **Failover**:
   - Automatic replica promotion on primary failure
   - Application reconnection with retry logic
   - Manual intervention for split-brain scenarios

## Alternatives Considered

### Alternative 1: Citus (Distributed PostgreSQL)
**Description**: Horizontal sharding with Citus extension
**Pros**:
- True horizontal scaling
- Distributed queries
- Same PostgreSQL compatibility

**Cons**:
- Complex shard key selection
- Cross-shard query limitations
- Operational complexity

**Why Not Chosen**: Read replicas sufficient for current scale; Citus may be needed at 10x growth

### Alternative 2: Amazon Aurora
**Description**: AWS-managed PostgreSQL-compatible database
**Pros**:
- Fully managed
- Automatic storage scaling
- Built-in read replicas

**Cons**:
- AWS lock-in
- Higher cost than self-managed
- Less control over configuration

**Why Not Chosen**: Need cloud-agnostic solution; self-managed provides more control

### Alternative 3: Application-Level Caching (Redis)
**Description**: Cache frequently-read data in Redis
**Pros**:
- Faster than database reads
- Reduces database load
- Already have Redis infrastructure

**Cons**:
- Cache invalidation complexity
- Doesn't solve all read scaling
- Data consistency challenges

**Why Not Chosen**: Caching is complementary, not replacement for read scaling

## Consequences

### Positive Consequences
- **Scalability**: 3x read capacity with 2 replicas
- **Performance**: Read queries offloaded from primary
- **Availability**: Automatic failover capability
- **Cost**: Horizontal scaling cheaper than vertical

### Negative Consequences
- **Replication Lag**: Replicas may be milliseconds behind
- **Complexity**: Application must handle routing correctly
- **Consistency**: Stale reads possible on replicas
- **Operations**: More infrastructure to monitor

### Mitigation
- Use `@Transactional(readOnly = true)` explicitly
- Monitor replication lag (alert if >1 second)
- Critical reads go to primary
- Comprehensive monitoring and alerting

## Configuration

### Primary Configuration (postgresql.conf)
```conf
# Replication Settings
wal_level = replica
max_wal_senders = 10
max_replication_slots = 10
synchronous_commit = on
synchronous_standby_names = 'replica1,replica2'

# Performance
shared_buffers = 4GB
effective_cache_size = 12GB
maintenance_work_mem = 1GB
```

### Replica Configuration (postgresql.conf)
```conf
hot_standby = on
hot_standby_feedback = on
max_standby_streaming_delay = 30s
```

### Spring Data Source Routing
```java
public class ReadWriteRoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        boolean isReadOnly = TransactionSynchronizationManager
            .isCurrentTransactionReadOnly();
        return isReadOnly ? DataSourceType.REPLICA : DataSourceType.PRIMARY;
    }
}
```

### Application Configuration
```yaml
datasource:
  primary:
    url: jdbc:postgresql://primary:5432/hdim
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  replica:
    url: jdbc:postgresql://replica1:5432,replica2:5432/hdim
    username: ${DB_READONLY_USERNAME}
    password: ${DB_READONLY_PASSWORD}
    readOnly: true
```

## Implementation Plan

1. **Phase 1 (Completed)**: Docker Compose HA setup for development
2. **Phase 2 (Completed)**: ReadWriteRoutingDataSource implementation
3. **Phase 3**: Production replica provisioning
4. **Phase 4**: PgBouncer connection pooling
5. **Phase 5**: Monitoring and alerting setup

## Files Created

**Infrastructure**:
- `docker-compose.ha.yml`
- `docker/postgres/primary/postgresql.conf`
- `docker/postgres/primary/pg_hba.conf`
- `docker/postgres/replica/postgresql.conf`
- `docker/postgres/init-replication.sh`

**Application**:
- `backend/modules/shared/infrastructure/persistence/src/main/java/com/healthdata/persistence/routing/ReadWriteRoutingDataSource.java`
- `backend/modules/shared/infrastructure/persistence/src/main/java/com/healthdata/persistence/routing/ReadWriteDataSourceConfig.java`

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Read query latency p95 | <100ms | APM metrics |
| Replication lag | <100ms | pg_stat_replication |
| Primary CPU utilization | <60% | Infrastructure monitoring |
| Read throughput | 10K QPS | Database metrics |

## Related Decisions

- **Related to**: [ADR-0003](0003-adopt-circuit-breaker-pattern.md) - Circuit breakers for database failures

## References

- [PostgreSQL Streaming Replication](https://www.postgresql.org/docs/current/warm-standby.html)
- [Spring Read/Write Routing](https://spring.io/blog/2020/05/20/routing-write-and-read-operations-to-different-datasources)
- [PgBouncer Documentation](https://www.pgbouncer.org/)
