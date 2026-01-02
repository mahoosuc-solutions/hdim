# Database Sharding Strategy

## Overview

This document outlines the database sharding strategy for the HDIM platform to support horizontal scaling as tenant and data volume grows.

## Current State

- **Single PostgreSQL cluster** with primary + 2 read replicas
- **~500GB total database size** across all tenants
- **Multi-tenant shared database** with tenant_id column

## When to Implement Sharding

Consider implementing sharding when:

| Metric | Threshold | Current | Status |
|--------|-----------|---------|--------|
| Total tenants | >500 | ~50 | OK |
| Database size | >2TB | ~500GB | OK |
| Queries per second | >10,000 | ~2,000 | OK |
| Single tenant data | >100GB | <10GB | OK |
| Write throughput | >5,000/s | ~500/s | OK |

**Recommendation**: Implement sharding when approaching 500 tenants or 2TB total data.

## Sharding Strategy

### Option 1: Tenant-Based Sharding (Recommended)

Shard data by `tenant_id` - each tenant's data lives entirely on one shard.

```
┌─────────────────────────────────────────────────────┐
│                  Application Layer                   │
│              (Shard-Aware Repository)                │
└─────────────────┬───────────────────────────────────┘
                  │
      ┌───────────┼───────────┐
      ▼           ▼           ▼
┌─────────┐ ┌─────────┐ ┌─────────┐
│ Shard 0 │ │ Shard 1 │ │ Shard 2 │
│Tenant 1 │ │Tenant 2 │ │Tenant 3 │
│Tenant 4 │ │Tenant 5 │ │Tenant 6 │
│...      │ │...      │ │...      │
└─────────┘ └─────────┘ └─────────┘
```

**Pros**:
- No cross-shard queries for tenant operations
- Simple tenant isolation
- Easy backup/restore per tenant
- Compliance-friendly (data locality)

**Cons**:
- Tenant hotspots (large tenants)
- Uneven shard distribution
- No cross-tenant analytics without aggregation

### Option 2: Time-Based Sharding

Shard data by time periods (e.g., monthly partitions).

**Use case**: FHIR resources with mostly append-only patterns.

```
┌───────────┐ ┌───────────┐ ┌───────────┐
│ 2025-Q1   │ │ 2025-Q2   │ │ 2025-Q3   │
│ Resources │ │ Resources │ │ Resources │
└───────────┘ └───────────┘ └───────────┘
```

**Pros**:
- Even distribution over time
- Old data archival friendly
- Good for time-series queries

**Cons**:
- Cross-shard queries for patient history
- Complexity for current + historical queries

### Option 3: Hybrid Sharding (Tenant + Time)

Combine tenant and time-based sharding.

```
Tenant 1 ─┬─ 2024 data ─ Shard 1A
          └─ 2025 data ─ Shard 1B

Tenant 2 ─┬─ 2024 data ─ Shard 2A
          └─ 2025 data ─ Shard 2B
```

## Shard Key Selection

### Primary Shard Key: `tenant_id`

```sql
-- All tables include tenant_id for sharding
CREATE TABLE patient (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,  -- Shard key
    mrn VARCHAR(50),
    -- ... other columns
    CONSTRAINT fk_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

-- Index on shard key
CREATE INDEX idx_patient_tenant ON patient(tenant_id);
```

### Shard Routing

```java
@Service
public class ShardRoutingService {

    private final Map<String, DataSource> shardDataSources;

    public DataSource getShardForTenant(String tenantId) {
        int shardNumber = calculateShard(tenantId);
        return shardDataSources.get("shard-" + shardNumber);
    }

    private int calculateShard(String tenantId) {
        // Consistent hashing for stable shard assignment
        return Math.abs(tenantId.hashCode() % totalShards);
    }
}
```

## Implementation Phases

### Phase 1: Prepare for Sharding (Current)

1. ✅ Ensure all tables have `tenant_id` column
2. ✅ Add tenant_id to all queries and indexes
3. ✅ Implement ReadWriteRoutingDataSource
4. ⬜ Add shard routing abstraction layer

### Phase 2: Logical Sharding

1. Implement shard-aware repository layer
2. Add tenant-to-shard mapping table
3. Create shard configuration management
4. Test with single shard (same physical DB)

```java
@Repository
public class ShardAwarePatientRepository {

    @Autowired
    private ShardRoutingService shardRouter;

    public Patient findByTenantAndId(String tenantId, UUID patientId) {
        DataSource shard = shardRouter.getShardForTenant(tenantId);
        // Execute query against specific shard
    }
}
```

### Phase 3: Physical Sharding

1. Provision additional PostgreSQL clusters
2. Migrate tenants to target shards
3. Update shard mapping
4. Implement cross-shard query handling

### Phase 4: Automated Shard Management

1. Implement shard rebalancing
2. Add tenant migration tooling
3. Create shard monitoring dashboards
4. Automate shard provisioning

## Cross-Shard Queries

For analytics requiring cross-tenant data:

### Option A: Scatter-Gather

```java
public List<Metric> getGlobalMetrics() {
    return shards.parallelStream()
        .map(shard -> queryMetrics(shard))
        .flatMap(List::stream)
        .collect(Collectors.toList());
}
```

### Option B: Analytics Data Warehouse

Replicate aggregated data to a central analytics database:

```
Shard 1 ─┐
Shard 2 ─┼─ CDC → Analytics Warehouse (BigQuery/Snowflake)
Shard 3 ─┘
```

### Option C: Citus Extension

Use Citus for distributed PostgreSQL:

```sql
-- Distribute table by tenant_id
SELECT create_distributed_table('patient', 'tenant_id');

-- Queries automatically routed to shards
SELECT * FROM patient WHERE tenant_id = 'abc-123';
```

## Shard Configuration

```yaml
# application.yml
sharding:
  enabled: false  # Enable when ready
  strategy: tenant  # tenant, time, or hybrid
  total-shards: 4
  default-shard: 0

  shards:
    shard-0:
      url: jdbc:postgresql://shard0-primary:5432/hdim
      read-replicas:
        - jdbc:postgresql://shard0-replica1:5432/hdim
        - jdbc:postgresql://shard0-replica2:5432/hdim

    shard-1:
      url: jdbc:postgresql://shard1-primary:5432/hdim
      read-replicas:
        - jdbc:postgresql://shard1-replica1:5432/hdim

  tenant-mapping:
    # Override automatic shard assignment for specific tenants
    large-tenant-1: shard-1
    enterprise-tenant: shard-2
```

## Migration Strategy

### Zero-Downtime Tenant Migration

1. **Dual-Write Phase**: Write to both old and new shard
2. **Backfill Phase**: Copy historical data to new shard
3. **Verify Phase**: Validate data consistency
4. **Cutover Phase**: Update routing, stop old writes
5. **Cleanup Phase**: Remove data from old shard

```java
@Service
public class TenantMigrationService {

    public void migrateTenant(String tenantId, String targetShard) {
        // 1. Enable dual-write
        enableDualWrite(tenantId, targetShard);

        // 2. Backfill historical data
        backfillData(tenantId, targetShard);

        // 3. Verify consistency
        verifyDataConsistency(tenantId, targetShard);

        // 4. Update routing
        updateShardMapping(tenantId, targetShard);

        // 5. Disable dual-write
        disableDualWrite(tenantId);

        // 6. Schedule cleanup
        scheduleDataCleanup(tenantId, oldShard);
    }
}
```

## Monitoring

### Key Metrics

| Metric | Alert Threshold | Description |
|--------|-----------------|-------------|
| Shard query latency p99 | >500ms | Query performance per shard |
| Shard data size | >500GB | Individual shard size |
| Cross-shard query rate | >10% | Queries hitting multiple shards |
| Tenant distribution skew | >30% variance | Uneven tenant distribution |
| Replication lag | >10s | Replica synchronization delay |

### Grafana Dashboard Queries

```promql
# Queries per shard
sum(rate(pg_stat_statements_calls_total[5m])) by (shard)

# Shard size
pg_database_size_bytes{database="hdim"} by (shard)

# Cross-shard query percentage
cross_shard_queries_total / total_queries_total * 100
```

## References

- [Citus Data Documentation](https://docs.citusdata.com/)
- [PostgreSQL Partitioning](https://www.postgresql.org/docs/current/ddl-partitioning.html)
- [Vitess for MySQL](https://vitess.io/) (for reference patterns)
- [Stripe's Online Migration](https://stripe.com/blog/online-migrations)
