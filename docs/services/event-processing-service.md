# Event Processing Service

## Purpose
Consumes Kafka event streams to detect care gaps, trigger alerts, and orchestrate downstream workflows within seconds.

## Responsibilities
- Subscribe to resource, consent, and external event topics.
- Run streaming rules to highlight care gaps (<5s latency target).
- Emit webhook notifications and enrich events for analytics.
- Dead-letter failed events for remediation.

## Dependencies
- Kafka (`healthdata.resources`, `healthdata.consents`, `external.events`).
- FHIR and CQL services for enrichment calls.
- Redis (optional) for deduplication.

## Interfaces
- Events: `docs/api/asyncapi/event-processing.yaml`
- Webhooks: defined per integration partner.

## Observability
- Metrics: consumer lag, processing latency, alert counts.
- DLQ monitoring for failed events.

## Runbook
See `docs/runbooks/event-processing-service.md`.
