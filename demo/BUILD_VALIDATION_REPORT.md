# Demo Build & Validation Report

Date: 2026-01-16 09:32 EST
Environment: Local Docker Compose (`demo/docker-compose.demo.yml`)

## Objective
Provide a concise, reproducible record showing the demo can be deployed quickly and validated end-to-end with seeded data.

## Build & Deploy Summary
- Build strategy: reuse existing images where possible; rebuild only when code changed.
- Deployment target: local Docker Compose demo stack.
- Outcome: all core demo services up and healthy.

## Services Confirmed Healthy
- gateway-service (8080)
- quality-measure-service (8087)
- care-gap-service (8086)
- cql-engine-service (8081)
- fhir-service (8085)
- patient-service (8084)
- postgres (5435)
- redis (6380)
- kafka (9094)
- elasticsearch (9200)

## Data Seeding
Command:
```bash
bash demo/seed-demo-data.sh --wait
```
Result:
- 10 patients created
- 16 care gaps across 11 HEDIS measures
- Priority distribution: 10 HIGH, 6 MEDIUM

## Internal Validation (Seeded Data)
Command:
```bash
bash demo/validate-demo-data.sh
```
Result:
- Patients: 10/10 OK
- Care gaps: 16/16 OK
- Per-patient gap counts, measures, and open counts: OK

## External Validation (Independent Checks)
Command:
```bash
bash demo/validate-external.sh
```
Result:
- 0 failures; all checks passed (patients, gap counts, measures, open gap stats)

## Demo Accounts
- demo_admin / demo123 (ADMIN, EVALUATOR)
- demo_analyst / demo123 (ANALYST, EVALUATOR)
- demo_viewer / demo123 (VIEWER)
- demo_user / demo123 (VIEWER)
- demo.developer / demo123 (MEASURE_DEVELOPER, EVALUATOR)

## Notes
- Validation confirms seeded data consistency across FHIR, care-gap, and quality-measure flows.
- The demo is ready for screenshots and customer walkthroughs.
