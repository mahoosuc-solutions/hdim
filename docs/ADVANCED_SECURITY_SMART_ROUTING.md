# Advanced Security + Smart Routing Implementation

Repo: webemo-aaron/hdim.git  
Target date: 2026-01-14

## Purpose
Document the agreed security architecture, smart routing policy, and demo seeding changes for the external FHIR
server. This is the implementation baseline for the advanced security milestone.

## Security Architecture Summary
- Gateway-trust pattern: gateway validates JWTs and injects trusted `X-Auth-*` headers.
- Backend services validate gateway signatures and enforce tenant access.
- Outbound calls to external FHIR must be tenant-aware and auditable.

References:
- `docs/GATEWAY_TRUST_DEPLOYMENT_GUIDE.md`
- `backend/AUTHENTICATION-ARCHITECTURE.md`
- `deployment-content/01-ARCHITECTURE-DIAGRAMS.md`
- `deployment-content/02-INTEGRATION-PATTERNS.md`

## Smart Routing Policy (Authoritative Sources)
Authoritative source decisions (confirmed):
- **Patient**: authoritative source is the external/customer FHIR server.
- **Generated resources from quality measures/CQL**: authoritative source is internal advanced FHIR (versioned
  history, no impact to operational EHR).

Routing rules:
- **Reads**
  - Patient: read from external FHIR.
  - Generated resources: read from internal advanced FHIR.
- **Writes**
  - Patient: do not write back to internal unless explicitly needed for caching.
  - Generated resources: write to internal advanced FHIR only, preserve versioning.

Tenant requirement:
- Tenant ID is `demo-tenant` for demo scenarios.

## Demo Seeding Updates (External FHIR Population)
Goal: seed Patients to the external FHIR server while keeping generated resources internal.

### Scripted seeding (manual)
- `demo/seed-demo-data.sh` now supports `FHIR_PATIENT_BASE`:
  - Example (external authoritative Patient seeding):
    - `FHIR_PATIENT_BASE=http://localhost:8088/fhir ./demo/seed-demo-data.sh --wait`

### Demo seeding service (automated)
Configuration supports internal/external routing:
- `demo.services.fhir.internal-url`: internal FHIR base (default: `http://fhir-service:8085/fhir`)
- `demo.services.fhir.external-url`: external FHIR base (optional)
- `demo.services.fhir.target`: `internal`, `external`, `both`, or `hybrid`

Mode `hybrid`:
- Patient -> external FHIR
- All other resources -> internal FHIR

Docker demo defaults:
- `docker-compose.demo.yml` sets:
  - `FHIR_EXTERNAL_URL=http://external-fhir-server:8080/fhir`
  - `FHIR_TARGET=hybrid`

External FHIR server:
- `docker-compose.fhir-server.yml` uses `external-fhir-server` on the `hdim-demo-network`
- Metadata endpoint: `http://localhost:8088/fhir/metadata`

## Advanced Security Milestone (Draft)
Title: Advanced Security Implementation (Gateway Trust + Smart Routing)

Scope issues:
1. Routing policy matrix and enforcement for smart routing.
2. Outbound auth bridge for external FHIR (OAuth2/bearer/basic, per-tenant config).
3. Tenant mapping validation for external FHIR endpoints (fail-closed).
4. Audit/provenance for routed reads/writes.
5. Write-back constraints and idempotency for generated resources.
6. Egress security hardening (mTLS/allowlist) for external FHIR.
7. Integration tests for routing and multi-tenant isolation.

Definition of done:
- Routing policy implemented and documented.
- Outbound auth for external FHIR operational.
- Tenant mapping enforced and validated.
- Audit/provenance emitted for routed traffic.
- Generated resources versioned in internal FHIR.
- Tests green for routing + tenant isolation.
