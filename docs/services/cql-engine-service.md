# CQL Engine Service

## Purpose
Evaluates Clinical Quality Language (CQL) expressions and HEDIS measures, providing real-time clinical insights and compliance reporting.

## Responsibilities
- Execute CQL libraries against FHIR data.
- Maintain measure bundles, Star ratings, and measure result caching.
- Publish measure evaluation events (`healthdata.metrics`).

## Dependencies
- FHIR Service (HTTP client) for patient data.
- PostgreSQL (`healthdata_metrics`) for definitions/results.
- Redis for caching compiled libraries.

## Interfaces
- REST: `docs/api/openapi/cql-engine-service.yaml`
- Events: `docs/api/asyncapi/measure-events.yaml`

## Observability
- Metrics: evaluation latency, cache hit rate, throughput.
- Tracing: propagate trace IDs through FHIR calls.

## Runbook
See `docs/runbooks/cql-engine-service.md`.
