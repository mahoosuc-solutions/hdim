## Summary
Current local/docker validation passes with curated/evaluable measure selection, but broad/default measure pools are still not truly supported for:
- `HEDIS-COU`
- `HEDIS-FMC`
- `HEDIS-AAB`
- `HEDIS-SPC`

This issue tracks the backend work required so stress validation (`N_MEASURES=10+`) can run without fallback curation.

## Current State (2026-02-22)
- Local web + API validation passes for curated sets:
  - `docs/marketing/web/evidence/one-patient-n-measures-20260222T230031Z.md` (N=5)
  - `docs/marketing/web/evidence/one-patient-n-measures-20260222T230034Z.md` (N=10)
- Existing measure-specific failures are tracked in:
  - #466 (`HEDIS-COU`)
  - #467 (`HEDIS-FMC`)
  - #468 (`HEDIS-AAB`)
  - #474 (`HEDIS-SPC`)

## Goal
Make broad/default measure pools first-class and reliable in `demo-tenant` and standard validation paths, eliminating curated fallback as a requirement.

## Required Backend Deliverables
1. Ensure measure definitions + executable CQL libraries exist and resolve for COU/FMC/AAB/SPC in `demo-tenant`.
2. Ensure `/quality-measure/calculate` returns success (`201`) for all four measures against seeded demo patients.
3. Ensure seeding path guarantees these artifacts in docker demo deployments.
4. Ensure failures are surfaced as actionable diagnostics (missing library/version/tenant mismatch), not opaque 500s.

## Acceptance Criteria
- Broad/default validator run passes with no curated fallback:
  - `QUALITY_MEASURE_BASE_URL=http://localhost:18080/quality-measure TENANT_ID=demo-tenant N_MEASURES=10 bash docs/marketing/web/scripts/validate-one-patient-n-measures.sh`
- No measure-level 500 responses for COU/FMC/AAB/SPC.
- New evidence artifact attached proving broad/default pass.
- Update evidence summary page to remove fallback caveat after pass.

## Suggested Implementation Order
1. `HEDIS-SPC` library resolution (known missing library path).
2. `HEDIS-COU`, `HEDIS-FMC`, `HEDIS-AAB` execution-path fixes.
3. Seeder hardening + integration validation.
4. Re-run end-to-end local validation and publish canonical artifacts.

## Notes
- This is a tracking umbrella; keep measure-specific debugging in #466/#467/#468/#474 and link resulting PRs here.
