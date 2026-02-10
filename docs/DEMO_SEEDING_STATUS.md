# Demo Seeding Status

This document captures the current expected demo seeding configuration.

## Multi-tenant seeding controls

The multi-tenant scenario uses configuration values from the demo seeding service,
and supports optional per-run overrides via the scenario load API.

- `DEMO_MULTI_TENANT_PATIENTS_PER_TENANT` (default: 200)
- `DEMO_MULTI_TENANT_CARE_GAP_PERCENTAGE` (default: 30)

These are injected into `MultiTenantStrategy` and used when loading the
`multi-tenant` scenario via:

```
POST /api/v1/demo/scenarios/multi-tenant
```

Optional override payload:
```
{
  "patientsPerTenant": 200,
  "careGapPercentage": 30
}
```

## Quick seed command

```
DEMO_MULTI_TENANT_PATIENTS_PER_TENANT=200 \
DEMO_MULTI_TENANT_CARE_GAP_PERCENTAGE=30 \
./scripts/seed-multi-tenant-demo.sh
```
