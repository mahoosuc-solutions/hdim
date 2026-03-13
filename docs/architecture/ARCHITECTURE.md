# HDIM Architecture Summary

**Validated Against Code**: March 12, 2026

This file is the short-form architecture index. Use it when you need the current shape of the platform quickly.

## Current State

- `59` Gradle-managed backend service modules in [`backend/settings.gradle.kts`](/mnt/wdblack/dev/projects/hdim-master/backend/settings.gradle.kts)
- `61` service directories under [`backend/modules/services`](/mnt/wdblack/dev/projects/hdim-master/backend/modules/services)
- `61` service READMEs currently present
- `3` gateway services: `gateway-admin-service`, `gateway-clinical-service`, `gateway-fhir-service`
- `4` event services: patient, quality measure, care gap, clinical workflow
- `4` paired event-handler libraries
- `5` shared domain modules
- `15` shared infrastructure modules

## Architecture Model

### Edge and ingress

- Kong is the external edge.
- Domain-specific gateway services handle internal ingress and routing concerns.

### Domain services

- Clinical and interoperability capabilities live in independent Spring Boot modules.
- Service code mostly depends on shared modules instead of other service modules directly.

### Event-sourced domains

- Event services own write-side orchestration and projection persistence.
- Handler modules encapsulate projection logic and event-side effects.
- Replay and event-store infrastructure exist as first-class platform modules.

### Shared platform foundation

The platform standardizes heavily on:

- PostgreSQL + Liquibase
- shared auth and security
- tracing and audit
- selective Kafka/event-sourcing

## Known Documentation Drift Resolved Here

The repo previously contained stale claims about:

- total service count
- gateway topology
- service coverage in documentation
- missing reference docs such as port and dependency maps

Those are now tracked in:

- [System Architecture](/mnt/wdblack/dev/projects/hdim-master/docs/architecture/SYSTEM_ARCHITECTURE.md)
- [Service Catalog](/mnt/wdblack/dev/projects/hdim-master/docs/services/SERVICE_CATALOG.md)
- [Port Reference](/mnt/wdblack/dev/projects/hdim-master/docs/services/PORT_REFERENCE.md)
- [Dependency Map](/mnt/wdblack/dev/projects/hdim-master/docs/services/DEPENDENCY_MAP.md)
- [Shared Module Catalog](/mnt/wdblack/dev/projects/hdim-master/docs/architecture/SHARED_MODULE_CATALOG.md)

## Recommended Next Steps

1. Generate architecture inventories from code instead of hand-editing counts.
2. Require a README and service metadata file for every deployable service.
3. Add CI checks for duplicate default ports and missing runtime metadata.
4. Add an ADR that freezes gateway strategy and service lifecycle states.
