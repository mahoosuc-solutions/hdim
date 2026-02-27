# Service Data Validation Refresh

Date: 2026-02-27
Scope: Post-seeding validation for demo stack core services (Wave-1 readiness support)

## Commands Executed

```bash
NON_INTERACTIVE=1 SEED_PROFILE=smoke WAIT_TIMEOUT_SECS=900 ./scripts/seed-all-demo-data.sh
scripts/validate-all-services-data.sh | tee test-results/service-data-validation-refresh-2026-02-27-escalated.log
```

## Seeding Result

- Status: PASS
- Run ID: `20260227-175220`
- Log: `logs/seed-runs/seed-all-demo-data-20260227-175220.log`
- Patient generation: 50 patients loaded for tenant `acme-health`.

## Validation Result

- Source log: `test-results/service-data-validation-refresh-2026-02-27-escalated.log`
- Totals:
  - Tested: 17
  - Passed: 7
  - Empty: 4
  - Skipped: 6
  - Failed: 0

## Key Findings

1. Core wave-aligned services are reachable and return HTTP 200 responses (no transport/runtime failures in this run).
2. Care-gap and FHIR patient paths now return data.
3. Some endpoints remain empty/unknown-format and need targeted follow-up:
   - Patient service list (0 items)
   - CQL libraries (0 items)
   - FHIR conditions/observations response-shape handling in validator
4. Several domain services are intentionally not running in this compose profile and were correctly skipped.

## Operational Note

The initial non-escalated validation run produced `HTTP 000` due local endpoint access limits from sandbox context. The escalated run above is the authoritative post-seeding validation result.
