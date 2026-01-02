# Health Data In Motion — Architecture Portal

> This document is the entry point into visual/system documentation. It links to modular C4 models, detailed service specs, and operational runbooks. Update each section as new features land to keep stakeholders aligned.

## High-Level System Map

- **Context Diagram**: `docs/architecture/C4-models/c1-system-context.md`
- **Container Diagram**: `docs/architecture/C4-models/c2-container.md`
- **Component Diagrams**: per service (links below)

## Services & Modules

| Service | Description | Detailed Spec | Runbook |
| --- | --- | --- | --- |
| FHIR Service | R4 resource operations, search, validation | `docs/services/fhir-service.md` | `docs/runbooks/fhir-service.md` |
| Consent Service | Consent policy enforcement | `docs/services/consent-service.md` | `docs/runbooks/consent-service.md` |
| CQL Engine | Measure evaluation & clinical logic | `docs/services/cql-engine-service.md` | `docs/runbooks/cql-engine-service.md` |
| Event Processing | Kafka streaming & alerts | `docs/services/event-processing-service.md` | `docs/runbooks/event-processing-service.md` |
| Admin Portal | Ops UI & tooling | `docs/services/admin-portal.md` | N/A (frontend) |

Add new services to the table as they are introduced.

## Data Flow

- **Messaging Topics**: `docs/architecture/messaging-topics.md`
- **Database Schema Reference**: `docs/architecture/database-schemas.md` (create per service)
- **API Contracts**: `docs/api/openapi` (REST) and `docs/api/asyncapi` (Kafka)

## Operations & Deployment

- **Boot Sequence**: `docs/architecture/SYSTEM_BOOT_SEQUENCE.md`
- **Infrastructure Diagrams**: `docs/architecture/diagrams`
- **CI/CD Pipeline**: `docs/architecture/ci-cd.md`

## UX & Product

- **Product Overview**: `docs/product/overview.md`
- **Feature Roadmap**: `docs/product/roadmap.md`
- **Release Notes**: `docs/product/releases.md`

---

### How to Update

1. Update the related Markdown in `docs/services` or `docs/architecture`.
2. Regenerate diagrams (PlantUML/Mermaid) and export to `docs/architecture/diagrams`.
3. Commit changes with descriptive messages, e.g., `docs: add consent service component diagram`.
