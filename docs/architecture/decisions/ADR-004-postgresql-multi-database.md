# ADR-004: PostgreSQL Multi-Database Architecture

**Status**: Accepted
**Date**: 2026-01-19 (Decision Made: Phase 1, Sept 2025)
**Decision Makers**: Architecture Lead, Platform Team
**Stakeholders**: All Backend Services

---

## Context

### Problem Statement

HDIM needed database isolation strategy for 29+ microservices. Shared database would create coupling; separate database servers would be expensive and complex. PostgreSQL supports multiple logical databases per instance—enabling service isolation with single infrastructure footprint.

---

## Options Considered

### Option 1: Database-per-Service (Multiple Logical Databases on Single PostgreSQL Instance)

**Description**: Each of 29 microservices has own logical database within single PostgreSQL 16 instance

**Pros**:
- Complete service isolation (no shared tables)
- Independent schema evolution
- Single infrastructure to manage
- Cost-effective (1 PostgreSQL instance, 29 databases)
- Backup/restore per service possible
- Zero inter-service database dependencies

**Cons**:
- Single PostgreSQL instance (potential bottleneck)
- All resources shared (CPU, memory, disk)
- Service isolation enforced by application, not database

**Estimated Effort**: 1 week setup
**Risk Level**: Low (proven pattern)

---

### Option 2: Shared Database with Schema-per-Service

**Description**: Single database with 29 schemas, one per service

**Pros**:
- Same as Option 1
- Easier schema organization

**Cons**:
- Shared database (tight coupling)
- Cross-schema queries tempt developers to violate isolation
- Hard to enforce no shared tables
- More difficult backup/restore per service

**Risk Level**: High (coupling risk)

---

### Option 3: Separate PostgreSQL Instances per Service

**Description**: 29 separate PostgreSQL servers (or 3 instances × 10 services)

**Pros**:
- Complete isolation
- True service independence

**Cons**:
- 29 PostgreSQL instances to manage
- Extremely expensive (storage, CPU, monitoring)
- Complex backup strategy
- Replication overhead

**Estimated Effort**: 4 weeks
**Risk Level**: High (operational complexity)

---

## Decision

**We chose Option 1 (Database-per-Service on Single PostgreSQL Instance)** because:

1. **Cost-Effective**: Single PostgreSQL 16 instance with 29 logical databases
2. **Service Isolation**: Each service has own database, no shared tables
3. **Independent Evolution**: Schemas evolve independently
4. **Operational Simplicity**: 1 database to manage, not 29
5. **Scalability**: Can upgrade single PostgreSQL if needed
6. **Backup Strategy**: Per-database backups possible
7. **Standard Pattern**: Recommended by microservices literature

---

## Consequences

### Positive

- Services truly decoupled (no shared database)
- Schema changes don't affect other services
- Performance isolated (one slow service doesn't affect others)
- 29 databases created successfully

### Negative

- Single PostgreSQL instance potential bottleneck
- All services compete for same CPU/memory/disk
- If PostgreSQL fails, all services affected
- Monitoring complexity (29 databases to track)

---

## Implementation

### Database Inventory (29 Total)

**Core Databases**:
- fhir_db (FHIR R4 resources)
- patient_db (patient demographics)
- quality_db (HEDIS measures)
- cql_db (CQL evaluation)
- caregap_db (care gaps)
- gateway_db (authentication)

**Additional Databases** (23 more):
- See DATABASE_ARCHITECTURE_MIGRATION_PLAN.md for complete list

### PostgreSQL Configuration

```yaml
# docker-compose.yml
postgres:
  image: postgres:16-alpine
  environment:
    POSTGRES_USER: healthdata
    POSTGRES_PASSWORD: <secret>
    POSTGRES_DB: template1
  volumes:
    - ./docker/postgres/init-multi-db.sh:/docker-entrypoint-initdb.d/init-databases.sh
  ports:
    - "5435:5432"
```

### Init Script

```bash
# docker/postgres/init-multi-db.sh
CREATE DATABASE fhir_db;
CREATE DATABASE patient_db;
CREATE DATABASE quality_db;
# ... (all 29)
GRANT ALL PRIVILEGES ON DATABASE fhir_db TO healthdata;
```

### Success Criteria

- ✅ 29 databases created successfully
- ✅ Each service connects to correct database only
- ✅ Zero shared tables across databases
- ✅ Backup/restore per database working
- ✅ Monitoring tracks all 29 databases
- ✅ Each service can evolve schema independently

---

## Monitoring & Validation

### Metrics

| Metric | Target |
|--------|--------|
| Database count | 29 |
| Shared tables | 0 |
| Service database isolation | 100% |
| Backup success rate | 99.9% |

---

## References

- **[Database Architecture Guide](../../backend/docs/DATABASE_ARCHITECTURE_GUIDE.md)**
- **[Liquibase Workflow](../../backend/docs/LIQUIBASE_DEVELOPMENT_WORKFLOW.md)**

---

## Footer

**ADR #**: 004
**Version**: 1.0
**Status**: Active and Deployed
**Last Updated**: 2026-01-19

_Decision Date: Phase 1 (September 2025)_
_Formalized: January 2026_
