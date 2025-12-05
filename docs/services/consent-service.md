# Consent Service

## Purpose
Centralized consent policy management enforcing HIPAA, 42 CFR Part 2, and GDPR requirements across all data access flows.

## Responsibilities
- CRUD for consent policies, patient authorizations, emergency overrides.
- Field-level access decisions (ABAC/RBAC) consumed by downstream services.
- Audit logging for consent evaluations and policy updates.
- Consent change events published to Kafka (`healthdata.consents`).

## Dependencies
- PostgreSQL (`healthdata_consent`).
- Kafka for propagating policy updates.
- External identity provider for role tokens.

## Interfaces
- REST: `docs/api/openapi/consent-service.yaml`
- Events: `docs/api/asyncapi/consent-events.yaml`

## Observability
- Metrics: decision latency, allow/deny ratios, policy cache hit rate.
- Alerts: misconfiguration, policy evaluation failures.

## Runbook
See `docs/runbooks/consent-service.md`.
