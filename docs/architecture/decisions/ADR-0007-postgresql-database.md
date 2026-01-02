# ADR-0007: PostgreSQL for Primary Database

**Date**: 2024-Q3
**Status**: Accepted
**Deciders**: Architecture Team, Data Engineering Team
**Technical Story**: Need HIPAA-compliant relational database for multi-tenant healthcare data

---

## Context and Problem Statement

HDIM requires a primary database for:

- Persistent storage of patient data, quality measures, care gaps
- Multi-tenant data isolation (HIPAA requirement)
- ACID compliance for healthcare transactions
- Complex queries for quality reporting and analytics
- Integration with Spring Data JPA and Hibernate
- FHIR resource storage (via HAPI FHIR JPA)

The database must support:
- Multi-tenant row-level isolation
- Encryption at rest (HIPAA)
- High availability and disaster recovery
- Performance at scale (1M+ patients across tenants)
- Connection pooling for microservices architecture

---

## Decision Drivers

* **HIPAA compliance** - Encryption, audit logging, access controls
* **Multi-tenant isolation** - Row-level security or application-level filtering
* **ACID compliance** - Transactional integrity for healthcare data
* **Spring Boot integration** - Native JPA/Hibernate support
* **FHIR compatibility** - HAPI FHIR JPA uses PostgreSQL as primary backend
* **Operational maturity** - Production-proven at scale
* **Cost** - Open source preferred to reduce licensing costs
* **Team expertise** - PostgreSQL skills available on team

---

## Considered Options

1. **PostgreSQL 15** - Open source relational database
2. **MySQL 8** - Open source relational database
3. **Microsoft SQL Server** - Enterprise relational database
4. **Amazon Aurora PostgreSQL** - AWS managed PostgreSQL
5. **CockroachDB** - Distributed SQL database

---

## Decision Outcome

**Chosen option**: "PostgreSQL 15"

**Rationale**: PostgreSQL provides the best combination of:
- HAPI FHIR first-class support (recommended database)
- Strong HIPAA compliance features (pgaudit, encryption, RLS)
- Excellent Spring Data JPA integration
- Rich query capabilities (JSONB for FHIR extensions)
- Open source with no licensing costs
- Mature ecosystem with proven healthcare deployments
- Team familiarity and expertise

---

## Consequences

### Positive

* **HAPI FHIR optimized**: First-class support for FHIR resource storage
* **HIPAA features**: pgaudit for audit logging, native encryption support
* **JSON support**: JSONB columns for flexible FHIR extension storage
* **Row-level security**: Native RLS for multi-tenant isolation option
* **Performance**: Proven at healthcare scale with proper indexing
* **Cost**: No licensing fees (open source)
* **Spring integration**: Excellent Spring Data JPA support

### Negative

* **Operational overhead**: Requires DBA expertise for tuning and maintenance
* **Scaling limits**: Vertical scaling primarily; read replicas for read scaling
* **Backup complexity**: Requires careful backup/restore planning for HIPAA

**Mitigations**:
- Use managed PostgreSQL (RDS, Cloud SQL) in production for reduced ops burden
- Implement connection pooling (PgBouncer) for microservices
- Document backup/restore procedures in runbook

### Neutral

* Requires proper indexing strategy for performance
* Connection pooling essential for microservices architecture

---

## Pros and Cons of Options

### Option 1: PostgreSQL 15

Open source relational database.

| Criterion | Assessment |
|-----------|------------|
| HAPI FHIR Support | **Good** - First-class, recommended database |
| HIPAA Compliance | **Good** - pgaudit, encryption, RLS available |
| Multi-tenancy | **Good** - RLS or application-level filtering |
| Spring Integration | **Good** - Excellent Spring Data JPA support |
| JSON Support | **Good** - Native JSONB for FHIR extensions |
| Cost | **Good** - Open source, no licensing |
| Healthcare Adoption | **Good** - Widely used in healthcare |

**Summary**: Optimal choice for HAPI FHIR with strong compliance features.

---

### Option 2: MySQL 8

Open source relational database.

| Criterion | Assessment |
|-----------|------------|
| HAPI FHIR Support | **Neutral** - Supported but PostgreSQL preferred |
| HIPAA Compliance | **Neutral** - Less robust audit logging than PostgreSQL |
| Multi-tenancy | **Neutral** - Application-level filtering required |
| Spring Integration | **Good** - Excellent Spring Data JPA support |
| JSON Support | **Neutral** - JSON support less mature than PostgreSQL |
| Cost | **Good** - Open source (but Oracle ownership concerns) |

**Summary**: Viable but PostgreSQL better suited for FHIR workloads.

---

### Option 3: Microsoft SQL Server

Enterprise relational database.

| Criterion | Assessment |
|-----------|------------|
| HAPI FHIR Support | **Neutral** - Supported but not optimized |
| HIPAA Compliance | **Good** - Enterprise audit and encryption features |
| Multi-tenancy | **Good** - Row-level security available |
| Spring Integration | **Good** - JDBC driver available |
| Cost | **Bad** - Significant licensing costs |
| Healthcare Adoption | **Good** - Common in enterprise healthcare |

**Summary**: Capable but licensing costs prohibitive for startup.

---

### Option 4: Amazon Aurora PostgreSQL

AWS managed PostgreSQL-compatible database.

| Criterion | Assessment |
|-----------|------------|
| HAPI FHIR Support | **Good** - PostgreSQL compatible |
| HIPAA Compliance | **Good** - AWS BAA available, encryption built-in |
| Multi-tenancy | **Good** - RLS and encryption |
| Operational Burden | **Good** - Fully managed |
| Cost | **Neutral** - Higher than self-managed but includes ops |
| Vendor Lock-in | **Bad** - AWS-specific |

**Summary**: Excellent managed option for AWS deployments, considered for production.

---

### Option 5: CockroachDB

Distributed SQL database.

| Criterion | Assessment |
|-----------|------------|
| HAPI FHIR Support | **Bad** - Not tested/supported |
| HIPAA Compliance | **Good** - Encryption, audit logging |
| Multi-tenancy | **Good** - Schema-level isolation |
| Distributed | **Good** - Built for global distribution |
| Operational Complexity | **Neutral** - Different operational model |
| Healthcare Adoption | **Neutral** - Limited healthcare deployments |

**Summary**: Interesting for global scale but HAPI FHIR compatibility uncertain.

---

## Implementation Notes

### Version Selected

**PostgreSQL 15** - Latest stable LTS release

### Deployment Model

- **Development**: Single instance in Docker Compose (port 5435)
- **Production**: Managed PostgreSQL (RDS/Cloud SQL) with read replicas

### Database Schema

```
healthdata_qm (main database)
├── public schema (shared tables)
│   ├── users
│   ├── tenants
│   └── audit_logs
├── fhir schema (HAPI FHIR tables)
│   ├── hfj_resource
│   ├── hfj_res_ver
│   └── ... (HAPI FHIR managed)
└── tenant-specific schemas (optional)
    ├── tenant_001
    └── tenant_002
```

### Multi-Tenant Strategy

**Chosen approach**: Application-level filtering via `tenantId` column

```sql
-- All tables include tenantId
CREATE TABLE patients (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    fhir_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    -- Additional columns
    CONSTRAINT fk_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

-- Required index for tenant filtering
CREATE INDEX idx_patients_tenant_id ON patients(tenant_id);

-- All queries MUST filter by tenant
SELECT * FROM patients WHERE tenant_id = :tenantId AND id = :patientId;
```

### HIPAA Compliance Configuration

```yaml
# postgresql.conf
# Encryption at rest (managed by cloud provider in production)
ssl = on
ssl_cert_file = '/path/to/server.crt'
ssl_key_file = '/path/to/server.key'

# Audit logging (pgaudit extension)
shared_preload_libraries = 'pgaudit'
pgaudit.log = 'all'
pgaudit.log_catalog = off
```

### Connection Pooling

```yaml
# Spring Boot configuration
spring:
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5435}/${POSTGRES_DB:healthdata_qm}
    username: ${POSTGRES_USER:healthdata}
    password: ${POSTGRES_PASSWORD}
    hikari:
      maximum-pool-size: 20  # Per service instance
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### Performance Targets

| Metric | Target | Actual (Dec 2024) |
|--------|--------|-------------------|
| Query Latency (simple) | <10ms | 5ms |
| Query Latency (complex) | <100ms | 75ms |
| Write Latency | <50ms | 35ms |
| Connections | 500+ concurrent | 600 tested |
| Data Volume | 10M+ rows | 8M current |

### Backup Strategy

| Type | Frequency | Retention | Tool |
|------|-----------|-----------|------|
| Full backup | Daily | 30 days | pg_dump / RDS snapshots |
| WAL archiving | Continuous | 7 days | pg_basebackup |
| Point-in-time | On-demand | 30 days | RDS PITR |

---

## Links

* [PostgreSQL Documentation](https://www.postgresql.org/docs/15/)
* [HAPI FHIR PostgreSQL Configuration](https://hapifhir.io/hapi-fhir/docs/server_jpa/database_support.html)
* [pgaudit Extension](https://www.pgaudit.org/)
* [Database Migration Scripts](/backend/modules/shared/infrastructure/persistence/)
* Related: [ADR-0005 - HAPI FHIR Selection](ADR-0005-hapi-fhir-selection.md)
* Related: [ADR-0008 - Redis Caching](ADR-0008-redis-caching-strategy.md)

---

## Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2024-Q3 | Architecture Team | Initial decision |
| 1.1 | 2024-12-30 | Architecture Team | Added schema details, performance metrics |

---

*This ADR follows the template in `/docs/templates/ADR_TEMPLATE.md`*
