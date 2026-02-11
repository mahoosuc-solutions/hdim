# Demo Seeding Population Spec (Local)

## Targets (Per Tenant)
- Patients: 1200
- Time coverage: 2 years historical + 1 year future
- Mix targets (overall):
  - Chronic: 25–35% (diabetes, HTN, CHF, COPD, CKD)
  - Behavioral health: 10–15% (depression/anxiety)
  - Preventive-only (healthy adults): 10–15%
  - Pediatrics (0–17): 8–12%
  - Geriatrics (65+): 12–18%

## Defaults Implemented
These defaults are set in `backend/modules/services/demo-seeding-service/src/main/resources/application.yml`:
- `demo.population.pediatric-pct: 0.10`
- `demo.population.geriatric-pct: 0.15`
- `demo.population.chronic-pct: 0.30`
- `demo.population.behavioral-pct: 0.12`
- `demo.population.preventive-pct: 0.12`
- `demo.encounters.past-years: 2`
- `demo.encounters.future-years: 1`
- `demo.encounters.future-count: 1`
- `demo.multi-tenant.patients-per-tenant: 1200`
- `demo.multi-tenant.tenant-ids: summit-care-2026,valley-health-2026`

## How To Seed
- Ensure demo stack is up: `docker compose -f docker-compose.demo.yml up -d`
- Rebuild demo-seeding image after code/config changes:
  - `./backend/gradlew -p backend :modules:services:demo-seeding-service:bootJar`
  - `docker compose -f docker-compose.demo.yml build demo-seeding-service`
  - `docker compose -f docker-compose.demo.yml up -d --no-deps demo-seeding-service`
- Seed multi-tenant scenario via gateway:
  - `POST http://localhost:8084/clinical/demo/api/v1/demo/scenarios/multi-tenant`
  - Optional JSON body: `{ "patientsPerTenant": 1200, "careGapPercentage": 30 }`

## Reset Behavior
- Demo reset now clears both:
  - Demo service tables (when present)
  - FHIR service database tables by `tenant_id`
- Care-gap service database tables by `tenant_id`
- This ensures prior FHIR data is removed before re-seeding.

## Monitor Progress
- Current session status: `GET http://localhost:8098/demo/api/v1/demo/status`
- Progress details: `GET http://localhost:8098/demo/api/v1/demo/sessions/current/progress`
- Container logs: `docker logs -f hdim-demo-seeding`

## Validation
- Quick counts:
  - `./scripts/verify-seeding-counts.sh` with `TENANTS` and `EXPECTED_PATIENTS_PER_TENANT` as needed.
- FHIR counts per tenant:
  - `GET /fhir/Patient?_summary=count&_count=0` with `X-Tenant-ID` header.

## Current Status (2026-02-11)
- Reset now clears FHIR + care-gap databases by `tenant_id`.
- Seeding restarted for `summit-care-2026` and `valley-health-2026` via multi-tenant scenario.
- Latest validation snapshot (in-progress):
  - summit-care-2026: Patients 100, Care Gaps 0
  - valley-health-2026: Patients 0, Care Gaps 0
- Continue monitoring until counts reach 1200 per tenant.

## Notes
- Future data is modeled as planned Encounters within the next year.
- Behavioral health uses ICD-10 F32.9 (Depression) and F41.9 (Anxiety).
- Pediatrics may include asthma (J45.909) for realism.
