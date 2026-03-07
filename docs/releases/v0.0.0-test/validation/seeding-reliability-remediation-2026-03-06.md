# Seeding Reliability Remediation - 2026-03-06

## Scope
Implemented multi-tenant seeding reliability hardening and deterministic session status behavior in `demo-seeding-service`.

## Changes Implemented
- Multi-tenant seeding now receives and uses active `sessionId`.
- Session lifecycle states expanded to include `RUNNING`, `FAILED`, and `CANCELLED`.
- Multi-tenant seeding fails when persisted patient totals do not meet requested per-tenant totals.
- `DemoStatus` now reflects live progress stage when available.
- FHIR and care-gap downstream POST operations now include retry/backoff boundaries.
- Added release-gate script: `scripts/release-validation/validate-demo-seeding-counts.sh`.
- Local CI/demo scripts now enforce tenant-scoped seeded-count checks in non-smoke mode.

## Validation Executed
```bash
./backend/gradlew -p backend \
  :modules:services:demo-seeding-service:test \
  --tests '*ScenarioLoaderServiceTest' \
  --tests '*MultiTenantStrategyTest' \
  --tests '*DemoSeedingServiceTest'

VERSION=v0.0.0-test \
TENANTS=summit-care-2026,valley-health-2026 \
EXPECTED_PATIENTS_BY_TENANT=summit-care-2026=1200,valley-health-2026=1200 \
./scripts/release-validation/validate-demo-seeding-counts.sh
```

## Result
- **PASS**: 22 tests passed, 0 failed.
- **EXPECTED FAIL (gate check):** seeding count gate failed against current seeded data baseline (counts below 1200/tenant), and generated:
  - `docs/releases/v0.0.0-test/validation/demo-seeding-count-validation-report.md`
- Regression coverage includes:
  - multi-tenant incomplete output failure behavior
  - scenario loader failure-to-session-status propagation
  - demo status preference for live progress stage
