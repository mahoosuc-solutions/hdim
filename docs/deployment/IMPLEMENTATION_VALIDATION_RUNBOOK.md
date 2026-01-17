# Implementation Validation Runbook (On-Prem & Cloud)

This runbook validates tenant enforcement, UUID-only FHIR data, and core demo services for both on-prem and cloud-hosted deployments. The same containers and scripts are used in all environments.

## Prerequisites
- Docker/Compose or Kubernetes access
- Gateway and core services running
- Demo tenant/users seeded (see `docker/demo/demo-users.sql`)

## Required Configuration
- Tenant hardblock enabled:
  - `TENANT_ENFORCEMENT_MODE=enforce`
  - `TENANT_ENFORCEMENT_MISSING_TENANT_PATHS=/actuator,/swagger-ui,/v3/api-docs,/fhir/metadata,/metadata,/api/v1/auth`
- Gateway always injects `X-Tenant-ID` for authenticated traffic.
- FHIR IDs must be UUIDs; patient references must be `Patient/<uuid>`.

## Validation Steps (On-Prem or Cloud)
1) System health and tenant enforcement
```
bash ./validate-system.sh
```
Expected:
- Health checks return 200.
- Missing tenant on protected endpoints returns 400 or 403.
- Allowlisted public endpoints work without tenant headers.

2) FHIR UUID and reference integrity
```
bash ./validate-fhir-data.sh
```
Expected:
- All patient IDs are UUIDs.
- Condition/Observation/MedicationRequest/Encounter references use `Patient/<uuid>`.

3) Service data validation
```
bash ./scripts/validate-all-services-data.sh
```
Expected:
- Core services return data; optional services may be skipped if not deployed.

## Full Demo Reset Cycle (Optional)
For a clean rebuild/seed/validate loop:
```
bash ./scripts/demo-reset-cycle.sh
```

## Notes for On-Prem vs Cloud
- On-prem: ensure gateway routes and TLS termination are local; PHI stays on-prem.
- Cloud: ensure gateway is publicly reachable and restrict direct service access.
- Hybrid: gateway on-prem, optional analytics in cloud; keep tenant enforcement consistent.

## Tenant Hardblock FAQ

**What is the hardblock?**  
Any request missing `X-Tenant-ID` to protected endpoints is rejected (400/403). This ensures every request has tenant context for auditing and isolation.

**Why allowlist any paths?**  
Auth and health endpoints (e.g., `/api/v1/auth`, `/actuator/health`, `/fhir/metadata`) are intentionally public. The allowlist keeps those endpoints reachable without tenant headers.

**What should be allowlisted?**  
Only public endpoints that do not touch tenant data. Current recommended allowlist:
`/api/v1/auth`, `/actuator`, `/fhir/metadata`, `/metadata`, `/swagger-ui`, `/v3/api-docs`.

**How do I validate it?**  
- Public endpoint without tenant should return 200.
- Protected endpoint without tenant should return 400/403.
Use `bash ./validate-system.sh`.

**Does this break gateway traffic?**  
No. The gateway injects `X-Tenant-ID` for authenticated traffic, so normal requests keep working.
