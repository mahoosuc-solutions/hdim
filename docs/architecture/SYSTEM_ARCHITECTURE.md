# HDIM System Architecture

**Validated Against Code**: March 12, 2026  
**Primary Sources**: `backend/settings.gradle.kts`, `backend/modules/services/*`, service `application.yml`, service `build.gradle.kts`

## Overview

HealthData-in-Motion (HDIM) is a multi-service healthcare platform organized as a Gradle monorepo with Spring Boot microservices, shared domain/infrastructure modules, and a smaller set of Python agent services.

This document is intentionally code-validated rather than aspirational. It reflects what is currently present in the repository:

- `59` Gradle-managed backend service modules in [`backend/settings.gradle.kts`](/mnt/wdblack/dev/projects/hdim-master/backend/settings.gradle.kts)
- `61` service directories under [`backend/modules/services`](/mnt/wdblack/dev/projects/hdim-master/backend/modules/services) including the Python services `ai-sales-agent` and `live-call-sales-agent`
- `61` service-level `README.md` files currently present
- shared module coverage is tracked in [Shared Module Catalog](/mnt/wdblack/dev/projects/hdim-master/docs/architecture/SHARED_MODULE_CATALOG.md)
- `5` shared domain modules, `15` shared infrastructure modules, and `4` shared API-contract modules

## Platform Shape

### Runtime Layers

1. **Experience and edge**
   - Kong sits at the external edge.
   - The current backend gateway layer is split into `gateway-admin-service`, `gateway-clinical-service`, and `gateway-fhir-service`.
   - Earlier docs that reference a single `gateway-service` are stale relative to the current codebase.

2. **Domain services**
   - Clinical, interoperability, analytics, platform, and business capabilities are implemented as separate service modules.
   - Most services depend on shared domain/infrastructure modules rather than directly importing other service code.

3. **Event-sourced read/write paths**
   - Event-sourced services exist for patient, quality measure, care gap, and clinical workflow domains.
   - Each event service is paired with a handler library that owns projection/update logic.
   - `event-store-service`, `event-replay-service`, and `fhir-event-bridge-service` support the event pipeline.

4. **Shared modules**
   - Shared libraries provide security, authentication, persistence, messaging, tracing, audit, feature flags, and domain models.
   - The new shared `star-ratings` domain module now carries reusable Stars calculation logic.

## Service Topology

### Clinical and care management

- `patient-service`
- `fhir-service`
- `cql-engine-service`
- `quality-measure-service`
- `care-gap-service`
- `consent-service`
- `clinical-workflow-service`
- `nurse-workflow-service`
- `hcc-service`
- `prior-auth-service`
- `sdoh-service`
- `ecr-service`
- `qrda-export-service`

### Event-sourced services and handlers

- `patient-event-service`
- `quality-measure-event-service`
- `care-gap-event-service`
- `clinical-workflow-event-service`
- `patient-event-handler-service`
- `quality-measure-event-handler-service`
- `care-gap-event-handler-service`
- `clinical-workflow-event-handler-service`
- `event-store-service`
- `event-replay-service`
- `fhir-event-bridge-service`

### Data, integration, and adapters

- `event-processing-service`
- `event-router-service`
- `ehr-connector-service`
- `cms-connector-service`
- `cdr-processor-service`
- `data-enrichment-service`
- `data-ingestion-service`
- `documentation-service`
- `corehive-adapter-service`
- `healthix-adapter-service`
- `hedis-adapter-service`
- `ihe-gateway-service`

### Analytics, payer, and operational workflows

- `analytics-service`
- `predictive-analytics-service`
- `cost-analysis-service`
- `payer-workflows-service`
- `migration-workflow-service`
- `audit-query-service`
- `query-api-service`
- `cqrs-query-service`

### Platform, admin, and notifications

- `gateway-admin-service`
- `gateway-clinical-service`
- `gateway-fhir-service`
- `admin-service`
- `approval-service`
- `notification-service`
- `demo-orchestrator-service`
- `demo-seeding-service`

### AI and business-facing services

- `ai-assistant-service`
- `agent-builder-service`
- `agent-runtime-service`
- `agent-validation-service`
- `devops-agent-service`
- `sales-automation-service`
- `investor-dashboard-service`
- `ai-sales-agent`
- `live-call-sales-agent`

## Communication Patterns

### 1. Gateway-mediated synchronous requests

The intended synchronous path is:

`Client -> Kong -> domain gateway -> service`

The repo currently has three explicit gateway services, which indicates domain-specific ingress rather than a single internal gateway.

### 2. Service-to-service HTTP

OpenFeign is present in multiple services, including:

- `agent-builder-service`
- `agent-runtime-service`
- `care-gap-service`
- `care-gap-event-service`
- `clinical-workflow-service`
- `clinical-workflow-event-service`
- `cms-connector-service`
- `cql-engine-service`
- `ehr-connector-service`
- `payer-workflows-service`
- `quality-measure-service`
- `sales-automation-service`

This means runtime dependency edges are broader than the build-time module graph alone suggests.

### 3. Kafka event streaming

Kafka dependencies are present across the event services, event infrastructure, and several domain services, including:

- `event-processing-service`
- `event-router-service`
- `event-store-service`
- `patient-event-service`
- `quality-measure-event-service`
- `care-gap-event-service`
- `clinical-workflow-event-service`
- `notification-service`
- `sales-automation-service`

### 4. Event sourcing and projections

The current event-sourcing pattern is concrete, not theoretical:

- event service modules consume/emit domain events
- paired handler modules encapsulate projection logic
- projection read models live in dedicated event-service schemas
- replay capability exists through `event-replay-service`

The newest example is `care-gap-event-service`, which now owns a Stars projection path:

- current Stars projection persistence
- weekly and monthly snapshot capture
- on-demand simulation API
- shared Stars calculation logic in `shared:domain:star-ratings`

## Build-Time Dependency Rules

A useful validated property of the codebase: direct build-time service-to-service dependencies are still relatively constrained.

Current direct Gradle service-module dependencies found in `build.gradle.kts` files:

- `care-gap-event-service -> care-gap-event-handler-service`
- `clinical-workflow-event-service -> clinical-workflow-event-handler-service`
- `patient-event-service -> patient-event-handler-service`
- `quality-measure-event-service -> quality-measure-event-handler-service`
- `data-ingestion-service -> demo-seeding-service`
- `migration-workflow-service -> cdr-processor-service`

That is a healthy sign. Most services still integrate via shared modules, Feign, and Kafka rather than by importing one another directly.

## Shared Module Backbone

The most reused shared infrastructure modules are:

| Shared Module | Referenced By |
|---|---:|
| `database-config` | 54 services |
| `persistence` | 53 services |
| `security` | 44 services |
| `audit` | 42 services |
| `tracing` | 40 services |
| `authentication` | 39 services |
| `api-docs` | 23 services |
| `messaging` | 16 services |
| `event-sourcing` | 11 services |
| `metrics` | 10 services |
| `gateway-core` | 9 services |

This shows the platform is operationally standardized around:

- PostgreSQL + Liquibase persistence
- shared auth/security posture
- tracing and audit as first-class concerns
- selective Kafka/event-sourcing adoption

## Operational Reality

### Documentation coverage

Current service documentation coverage has been brought to baseline completeness through generated README files:

- `61` service READMEs exist under `backend/modules/services`
- shared-module README coverage is tracked separately in the shared module catalog

### Port and context-path consistency

The codebase does not yet have a clean, uniform runtime metadata model:

- some services define explicit ports and servlet context paths
- some services rely entirely on environment placeholders
- some services omit port or context-path metadata from `application.yml`
- several services share the same default placeholder values, which is a deployment risk if not overridden by compose/k8s manifests

### Service metadata drift

There is visible drift between code and docs today:

- stale claims about “9 services”, “28 services”, and “60 services”
- stale references to files that do not exist
- stale gateway topology references
- stale architecture diagrams that omit event services, handler libraries, adapters, and AI/business services

## Recommended Improvements

### Documentation improvements

1. Generate the service catalog from `settings.gradle.kts` plus each service's `application.yml` and `README.md` presence.
2. Add a required `README.md` template for every service module and fail CI when new services ship undocumented.
3. Treat `docs/services/PORT_REFERENCE.md` as generated reference data, not hand-maintained prose.
4. Add a lightweight dependency map generator that scans Gradle project deps, Feign clients, and Kafka listeners/producers.

### Architecture improvements

1. Standardize event-service conventions so every event domain has the same naming, projection ownership, replay story, and README structure.
2. Introduce a canonical `service-metadata.yml` per service with owner, domain, lifecycle, port, context path, database, Kafka topics, health endpoints, and runbook links.
3. Make the gateway strategy explicit in one ADR: which traffic belongs on admin, clinical, and FHIR gateways, and whether any legacy gateway remains supported.
4. Formalize which services are production-grade versus incubating or demo-only to reduce topology ambiguity.

### Operational improvements

1. Add CI validation for duplicate default ports and missing servlet context paths.
2. Require `health`, `info`, and `prometheus` actuator conventions across all deployable services.
3. Add a platform-wide runtime inventory check that verifies each service has:
   - explicit port policy
   - database/schema owner
   - audit posture
   - tracing/metrics posture
   - deployment/runbook link
4. Expand entity-migration validation usage so every persistence-owning service has a schema sync test.

## Authoritative Companion Docs

- [Architecture Summary](/mnt/wdblack/dev/projects/hdim-master/docs/architecture/ARCHITECTURE.md)
- [Service Catalog](/mnt/wdblack/dev/projects/hdim-master/docs/services/SERVICE_CATALOG.md)
- [Port Reference](/mnt/wdblack/dev/projects/hdim-master/docs/services/PORT_REFERENCE.md)
- [Dependency Map](/mnt/wdblack/dev/projects/hdim-master/docs/services/DEPENDENCY_MAP.md)
- [Shared Module Catalog](/mnt/wdblack/dev/projects/hdim-master/docs/architecture/SHARED_MODULE_CATALOG.md)
- [Event Sourcing Architecture](/mnt/wdblack/dev/projects/hdim-master/docs/architecture/EVENT_SOURCING_ARCHITECTURE.md)
