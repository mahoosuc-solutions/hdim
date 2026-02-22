# Race Track FHIR Validation Runbook

## Scope

This runbook enforces branch-isolated validation artifacts for the Race Track FHIR A/B and one-patient-to-N-measures claim.

- Allowed paths:
  - `docs/marketing/**`
  - `docs/marketing/web/**`
- Out of scope:
  - Any cleanup/reset of unrelated working tree changes
  - Destructive git operations

## Branch and Commit Safety

1. Create the validation branch from current `HEAD`.
   - `git switch -c feat/race-track-ab-validation-performance`
2. Keep unrelated file modifications untouched.
3. Stage only scoped files with explicit pathspecs.
4. Validate staged scope before every commit.
   - `git diff --name-only --cached`

## Validation Interfaces (Locked)

- Experiment route: `/race-track-fhir-pipeline`
- Variants:
  - `/race-track-fhir-pipeline.html` (A)
  - `/race-track-fhir-pipeline-b.html` (B)
- Evidence page: `/race-track-fhir-evidence.html`
- Telemetry events:
  - `rtfp_view`
  - `rtfp_tab_click`
  - `rtfp_export_click`
- Sticky cookie: `ab_rtfp=a|b`
- API validator script:
  - `docs/marketing/web/scripts/validate-one-patient-n-measures.sh`
  - Required env: `QUALITY_MEASURE_BASE_URL`, `PATIENT_ID`
  - Optional env: `N_MEASURES`, `TENANT_ID`, `AUTH_TOKEN`, `OUT_DIR`

## Web Validation

Run:

```bash
bash docs/marketing/web/scripts/validate-web-ab-performance.sh
```

Outputs:

- `docs/marketing/web/evidence/web-validation-<timestamp>.md`
- `docs/marketing/web/evidence/web-performance-<timestamp>.csv`
- `docs/marketing/web/evidence/web-vitals-<timestamp>.csv`

Gates:

- Forced routing and sticky checks must pass.
- Fan-out claim text presence must pass.
- Telemetry contract wiring must pass.
- Variant median total-load delta must be `<= 15%`.
- Route-level median total-load regression vs baseline must be `<= 20%` when baseline file is provided.

## API Validation

Demo defaults:

- `QUALITY_MEASURE_BASE_URL=http://localhost:18080/quality-measure`
- `TENANT_ID=demo-tenant`

Run baseline and stress:

```bash
QUALITY_MEASURE_BASE_URL=http://localhost:18080/quality-measure \
PATIENT_ID=<patient-id> \
TENANT_ID=demo-tenant \
N_MEASURES=5 \
bash docs/marketing/web/scripts/validate-one-patient-n-measures.sh

QUALITY_MEASURE_BASE_URL=http://localhost:18080/quality-measure \
PATIENT_ID=<patient-id> \
TENANT_ID=demo-tenant \
N_MEASURES=10 \
bash docs/marketing/web/scripts/validate-one-patient-n-measures.sh
```

Outputs:

- `docs/marketing/web/evidence/one-patient-n-measures-<timestamp>.json`
- `docs/marketing/web/evidence/one-patient-n-measures-<timestamp>.md`

Required evidence fields:

- Per-measure pass/fail
- Per-measure HTTP code
- Per-measure runtime milliseconds
- Total runtime milliseconds
- Requested and successful measure counts

## Evidence Publication

After each run:

1. Update `docs/marketing/web/race-track-fhir-evidence.html` with latest timestamp and artifact links.
2. Keep previous timestamped evidence files immutable.
3. Explicitly separate:
   - Web claim visibility validation
   - Backend claim execution validation
