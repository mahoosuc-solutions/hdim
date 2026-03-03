# ADR-013: HIE Data Pipeline Architecture

**Status:** Accepted
**Date:** 2026-03-02
**Deciders:** Aaron (founder)

---

## Context

HDIM's data source connectors (EHR connector, CDR processor, CMS connector) and evaluation engine (CQL engine, care gap service) were implemented as independent services. Data could be pulled from external systems but had no automated path to FHIR persistence or downstream quality measure evaluation. The result: manual orchestration was required to move data from source to care gap detection.

Specifically:
1. **EHR Connector** could pull from Epic/Cerner/Athena but didn't persist to the FHIR store
2. **CDR Processor** could parse HL7v2/CDA but results weren't forwarded to FHIR service
3. **CMS Connector** could export BCDA data but had no ingest path to FHIR service
4. **FHIR Service** had no bulk import endpoint (only individual resource CRUD)
5. **CQL Engine** had no trigger to auto-evaluate when new data arrived

The gap: **zero automated data flow from source systems to care gap detection**.

---

## Decision

Implement an **event-driven pipeline** using Kafka topics to chain data flow end-to-end:

```
Data Source → FHIR Bundle POST → fhir-service persistence → Kafka event → CQL auto-evaluation → care gap detection
```

### Key Design Choices

1. **Kafka event chaining (not REST chaining):** Each pipeline stage publishes a completion event. Downstream consumers react asynchronously. This decouples services and enables replay.

2. **Opt-in feature flags:** All pipeline features are gated by `@ConditionalOnProperty` with `matchIfMissing = false`. Operators enable pipeline components per-deployment:
   - `cql.auto-evaluation.enabled` — CQL triggers on FHIR data events
   - `cql.lazy-fetch.enabled` — Redis stale-check with on-demand EHR sync
   - `ehr.sync.consumer.enabled` — EHR sync request consumer

3. **FHIR Bundle transaction endpoint:** New `POST /Bundle` endpoint accepts FHIR transaction/batch bundles. Routes entries to 19 existing resource services. No new persistence logic — pure orchestration.

4. **Bulk NDJSON `$import`:** Async import with streaming (BufferedReader, 200-resource batches). Prevents OOM on large files. Async polling pattern: `POST /$import → 202 + Content-Location → GET /$import-poll-status/{jobId}`.

5. **`rawFhirResource` reconstruction:** Rather than modifying all connector interfaces to output FHIR Bundles, each connector's persistence service reconstructs a FHIR Bundle from the connector's existing output format. This minimizes interface changes.

6. **Redis stale-check for lazy fetch:** CQL data provider checks Redis for staleness (24h TTL). If stale, publishes `ehr.sync.requested` Kafka event rather than blocking on synchronous EHR pull. Trade-off: slightly stale data vs. guaranteed low-latency CQL evaluation.

---

## Alternatives Considered

### Alternative 1: Synchronous REST chaining

Each service calls the next via REST: EHR → FHIR service → CQL engine → care gap service.

**Rejected because:**
- Tight temporal coupling — any service being down breaks the chain
- No replay capability — failed requests are lost
- Backpressure propagates upstream, causing timeout cascades
- Violates HDIM's event-sourcing architecture (Phases 4-5)

### Alternative 2: Central orchestrator service

A new `pipeline-orchestrator` service coordinates all steps via a state machine.

**Rejected because:**
- Single point of failure for the entire data flow
- Requires new service (deployment, monitoring, ops burden)
- Tight coupling to all pipeline services
- State machine complexity grows with each new data source

### Alternative 3: Database polling

Each service polls a shared `pipeline_events` table for new work.

**Rejected because:**
- Polling latency (seconds to minutes vs. Kafka's milliseconds)
- Wasted database resources on empty polls
- Schema coupling between services via shared table
- Doesn't leverage existing Kafka infrastructure

---

## Consequences

### Positive
- **Zero manual intervention** from data receipt to care gap detection
- **Replay capability** — Kafka topics retain events for reprocessing
- **Independent scaling** — each pipeline stage scales independently
- **Graceful degradation** — feature flags allow partial pipeline deployment
- **Existing service reuse** — Bundle endpoint delegates to 19 existing resource services

### Negative
- **Eventual consistency** — care gaps may lag seconds behind data arrival
- **Kafka dependency** — pipeline requires Kafka infrastructure
- **Configuration surface** — operators must enable correct flags per deployment
- **Debugging complexity** — tracing a record through 4+ services requires distributed tracing (mitigated by OpenTelemetry)

---

## Implementation

### Components Delivered

| Component | Service | Purpose |
|-----------|---------|---------|
| `BundleController` | fhir-service | POST /Bundle transaction/batch endpoint |
| `BundleTransactionService` | fhir-service | Routes entries to 19 resource services |
| `BulkImportController` | fhir-service | POST /$import async NDJSON endpoint |
| `BulkImportService` | fhir-service | Streaming NDJSON processing with batching |
| `EhrFhirPersistenceService` | ehr-connector-service | WebClient to fhir-service Bundle endpoint |
| `EhrConnectionConfigEntity` | ehr-connector-service | JPA entity for connection persistence |
| `CdrFhirPersistenceService` | cdr-processor-service | WebClient to fhir-service |
| `CmsBulkIngestService` | cms-connector-service | Batch NDJSON ingest via fhir-service |
| `FhirDataAvailableConsumer` | cql-engine-service | 5 Kafka listeners triggering auto-evaluation |
| `CqlDataProviderService` | cql-engine-service | Redis stale-check + lazy EHR fetch |

### Kafka Topics (Pipeline)

| Topic | Producer | Consumer |
|-------|----------|----------|
| `ehr.sync.fhir-persisted` | ehr-connector-service | cql-engine-service |
| `cdr.hl7v2.fhir-persisted` | cdr-processor-service | cql-engine-service |
| `cdr.cda.fhir-persisted` | cdr-processor-service | cql-engine-service |
| `fhir.bulk-import.completed` | fhir-service | cql-engine-service |
| `cms.bcda.ingest-completed` | cms-connector-service | cql-engine-service |
| `ehr.sync.requested` | cql-engine-service | ehr-connector-service |
| `cql.pipeline.auto-evaluation-completed` | cql-engine-service | care-gap-service |

---

## Related Decisions

- **ADR-011:** Shared Module Integration — gateway-core pattern informed the decision to avoid a central orchestrator
- **Event Sourcing Architecture (Phases 4-5):** Kafka event pattern established the convention this pipeline follows

---

## References

- [Event Sourcing Architecture](./EVENT_SOURCING_ARCHITECTURE.md)
- [FHIR R4 Bundle specification](https://www.hl7.org/fhir/bundle.html)
- [FHIR Bulk Data Access (Flat FHIR)](https://hl7.org/fhir/uv/bulkdata/)

---

*ADR-013 | Version 1.0 | Last Updated: 2026-03-02*
