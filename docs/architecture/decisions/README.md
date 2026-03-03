# Architecture Decision Records

This directory contains Architecture Decision Records (ADRs) for the HealthData-in-Motion (HDIM) platform.

## Index

| ADR | Title | Status | Date |
|-----|-------|--------|------|
| [0001](0001-externalize-secrets-and-credentials.md) | Externalize Secrets and Credentials | Accepted | 2025-12-06 |
| [0002](0002-implement-tenant-isolation-security.md) | Implement Tenant Isolation Security | Accepted | 2025-12-06 |
| [0003](0003-adopt-circuit-breaker-pattern.md) | Adopt Circuit Breaker Pattern with Resilience4j | Accepted | 2025-12-06 |
| [0004](0004-hashicorp-vault-secrets-management.md) | HashiCorp Vault for Secrets Management | Accepted | 2025-12-06 |
| [0005](0005-postgresql-read-replicas.md) | PostgreSQL Read Replicas for Scalability | Accepted | 2025-12-06 |
| [0006](0006-api-gateway-resilience.md) | API Gateway with Resilience Patterns | Accepted | 2025-12-06 |
| [ADR-001](ADR-001-event-sourcing-for-clinical-services.md) | Event Sourcing for Clinical Services | Accepted | 2026-02-11 |
| [ADR-002](ADR-002-gateway-modularization.md) | Gateway Modularization | Accepted | 2026-02-11 |
| [ADR-003](ADR-003-kafka-event-streaming.md) | Kafka Event Streaming | Accepted | 2026-02-11 |
| [ADR-004](ADR-004-postgresql-multi-database.md) | PostgreSQL Multi-Database Strategy | Accepted | 2026-02-11 |
| [ADR-005](ADR-005-liquibase-migrations.md) | Liquibase Database Migrations | Accepted | 2026-02-11 |
| [ADR-006](ADR-006-tdd-swarm-methodology.md) | TDD Swarm Methodology | Accepted | 2026-02-11 |
| [ADR-007](ADR-007-gateway-trust-authentication.md) | Gateway Trust Authentication | Accepted | 2026-02-11 |
| [ADR-008](ADR-008-opentelemetry-distributed-tracing.md) | OpenTelemetry Distributed Tracing | Accepted | 2026-02-11 |
| [ADR-009](ADR-009-multi-tenant-isolation.md) | Multi-Tenant Isolation | Accepted | 2026-02-11 |
| [ADR-010](ADR-010-hipaa-phi-cache-ttl.md) | HIPAA PHI Cache TTL | Accepted | 2026-02-11 |
| [ADR-011](ADR-011-shared-module-integration.md) | Shared Module Integration | Accepted | 2026-03-02 |
| [ADR-012](ADR-012-linkedin-integration.md) | LinkedIn Integration | Accepted | 2026-03-02 |
| [ADR-013](ADR-013-hie-data-pipeline.md) | HIE Data Pipeline Architecture | Accepted | 2026-03-02 |

## By Category

### Security
- ADR-0001: Externalize Secrets and Credentials
- ADR-0002: Tenant Isolation Security
- ADR-0004: HashiCorp Vault for Secrets Management

### Reliability
- ADR-0003: Circuit Breaker Pattern with Resilience4j
- ADR-0006: API Gateway with Resilience Patterns

### Scalability
- ADR-0005: PostgreSQL Read Replicas

## By Status

- **Accepted**: 20
- **Proposed**: 0
- **Deprecated**: 0
- **Superseded**: 0

## ADR Process

1. Copy the template from `_template.md`
2. Assign the next available number
3. Fill in all sections
4. Submit for review
5. Update this index when accepted

## References

- [Michael Nygard's ADR Template](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions)
- [ADR GitHub Organization](https://adr.github.io/)
