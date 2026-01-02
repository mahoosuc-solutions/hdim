# FHIR Service

## Purpose
The FHIR Service manages CRUD, search, and transactional operations across 150+ FHIR R4 resources, enforcing consent and audit policies at scale.

## Responsibilities
- REST endpoints for Patients, Observations, CarePlans, Bundles, etc.
- FHIR search parameters, paging, sorting.
- Validation pipeline with HAPI FHIR validators.
- Elided PHI storage with field-level encryption where required.
- Resource change events published to Kafka (`healthdata.resources`).

## Dependencies
- PostgreSQL (`healthdata_fhir`) for persistence.
- Redis for resource caching / conditional writes.
- Kafka for change events and downstream analytics.

## Interfaces
- REST: `docs/api/openapi/fhir-service.yaml`
- Events: `docs/api/asyncapi/resource-events.yaml`

## Observability
- Prometheus metrics: request latency, search performance, validation failures.
- Structured logging with correlation IDs.

## Runbook
See `docs/runbooks/fhir-service.md` for operational procedures.
