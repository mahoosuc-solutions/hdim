# Seeding Count Verification - 2026-03-06

## Scope
Validate seeded record counts per tenant across:
- FHIR `Patient?_summary=count`
- Care-gap `GET /care-gap/api/v1/care-gaps?page=0&size=1`

## Environment
- Date: 2026-03-06
- Stack: `docker-compose.demo.yml`
- Services used: `fhir-service` (8085), `care-gap-service` (8086), `demo-seeding-service` (8098)

## Commands Executed
```bash
# Bring demo data dependencies online
docker compose -f docker-compose.demo.yml up -d postgres db-init

docker compose -f docker-compose.demo.yml up -d

docker compose -f docker-compose.demo.yml up -d demo-seeding-service

# Trigger multi-tenant seed request
curl -X POST 'http://localhost:8098/demo/api/v1/demo/scenarios/multi-tenant' \
  -H 'Content-Type: application/json' \
  -d '{"patientsPerTenant":1200,"careGapPercentage":30}'

# Verify counts
TENANTS=summit-care-2026,valley-health-2026 \
EXPECTED_PATIENTS_BY_TENANT=summit-care-2026=1200,valley-health-2026=1200 \
./scripts/verify-seeding-counts.sh
```

## Verification Snapshot
Observed snapshot during active seeding run:

```text
Tenant                 | Patients     | Care Gaps     | Patient OK    | CareGap OK
summit-care-2026       | 100          | 0             | no            | n/a
valley-health-2026     | 0            | 0             | no            | n/a
```

## Findings
- Verification tooling now supports per-tenant expected values and care-gap range validation.
- Current multi-tenant seeding run does not yet satisfy expected targets (1200 patients per tenant).
- Seeder logs show active processing for `summit-care-2026` and a progress stall after `Persisted 100 patients...` during this run.

## Disposition
- `todo 001` verification workflow is implemented and executable.
- Current data validation result is **not yet passing** for target population counts; seeding behavior requires follow-up remediation.
