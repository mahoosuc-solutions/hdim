# Architecture Decision

This repo contains two backend implementations:

1) **Microservices stack** in `backend/` with root-level `docker-compose*.yml` and scripts.
2) **Modular monolith** in `healthdata-platform/` with its own `docker-compose.yml` and `start.sh`.

## Current Default

The repository defaults to the **microservices stack** when using root-level Docker Compose or scripts (for example, `docker compose --profile core up -d` in the repo root). The modular monolith is a separate deployment path under `healthdata-platform/`.

If you want a single authoritative architecture, update this file to declare which stack is official and deprecate the other.

## When To Use Which

- **Microservices (`backend/`)**: full service set (gateway, analytics, AI, event processing, etc.), Kafka-based event streaming, per-service databases.
- **Modular monolith (`healthdata-platform/`)**: single deployable service focused on core clinical workflows (patient, fhir, care gap, quality, notification) with simpler operations.

## Parity Map (Core Capabilities)

| Capability | Microservice | Modular Monolith Module |
| --- | --- | --- |
| API Gateway | `backend/modules/services/gateway-service` | Built-in API layer (`healthdata-platform/src/main/java/com/healthdata/api`) |
| Patient | `backend/modules/services/patient-service` | `patient` module |
| FHIR | `backend/modules/services/fhir-service` | `fhir` module |
| CQL | `backend/modules/services/cql-engine-service` | `quality` + CQL integration (no dedicated service) |
| Quality Measures | `backend/modules/services/quality-measure-service` | `quality` module |
| Care Gaps | `backend/modules/services/care-gap-service` | `caregap` module |
| Notifications | `backend/modules/services/notification-service` | `notification` module |

Services present in `backend/` but not in the modular monolith include analytics, AI services, event processing, and specialized payer/authorization workflows.
