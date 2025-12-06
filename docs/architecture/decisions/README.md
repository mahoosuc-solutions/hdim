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

- **Accepted**: 6
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
