# Validation Summary

- Timestamp (UTC): 2026-02-23T00:56:54Z
- Scope: Local post-fix validation closure on docker demo stack + local web evidence refresh

## Current Status

- Local web A/B + performance: PASS
- Local portal link audit: PASS
- Local one-patient-to-N-measures: PASS for N=5 and N=10
- Production reference link audit: PASS (latest available artifact set)
- Portal deployment workflow dispatches are still queue-blocked on GitHub Actions

## Canonical Evidence (Latest)

- Web A/B + performance (local)
  - `web-validation-20260223T005654Z.md`
  - `web-performance-20260223T005654Z.csv`
  - `web-vitals-20260223T005654Z.csv`
- Portal link audit (local)
  - `portal-link-audit-localhost-4174-20260223T005837Z.md`
  - `portal-link-audit-localhost-4174-20260223T005837Z.csv`
- One patient to N measures (local)
  - N=5: `one-patient-n-measures-20260223T005554Z.md`
  - N=10: `one-patient-n-measures-20260223T005557Z.md`
- Production link audit reference
  - `portal-link-audit-web-gamma-snowy-38-vercel-app-20260222T154550Z.md`
  - `portal-link-audit-web-gamma-snowy-38-vercel-app-20260222T154550Z.csv`

## Workflow Queue State (summary)

- 6 runs queued, none started — see full list in Pull Request section below
- Queue blocker: https://github.com/webemo-aaron/hdim/issues/475

## Current Demo Stack Validation

- `VALIDATION_PROFILE=demo ./scripts/validate-containers.sh`
  - Result: PASS (0 failures, warnings only for optional services)
- `VALIDATION_PROFILE=demo bash ./validate-fhir-data.sh`
  - Result: PASS (17 passed, 0 failed)
- `VALIDATION_PROFILE=demo bash ./scripts/validate-all-services-data.sh`
  - Result: PASS with warnings (0 failures, some optional/empty endpoints)

## Pull Request

- PR #477: https://github.com/webemo-aaron/hdim/pull/477
  - Commit: 7f238a7ac — Finalize post-merge validation evidence and broad measure support
  - Links issue #476; consolidates fixes for #466/#467/#468/#474

## Local Validation Run

> **Note:** GitHub Actions is not used for this project. All validation is run locally via the direct scripts.

- Runner: local (`webemo-aaron@AaronMSI`)
- Portal link audit: 2026-02-23T04:14:51Z — `docs/marketing/web/scripts/validate-portal-links.sh` — **PASS** (46 internal + 17 external links checked; 404s are local evidence artifacts not deployed to Vercel — expected)
- A/B performance validation: 2026-02-23T04:14:51Z — `docs/marketing/web/scripts/validate-web-ab-performance.sh` — **PASS**
- Artifact: `portal-link-audit-web-gamma-snowy-38-vercel-app-20260223T041451Z.md`

## Note on GitHub Actions Queue (closed)

Seven runs accumulated in the `portal-deployment-validation.yml` queue during development (2026-02-22T15:38Z – 2026-02-23T04:13Z). These are superseded by the local run above and are not part of the validation evidence; GitHub Actions is not used in this project's test workflow.

## Backend Tracking

- Umbrella: https://github.com/webemo-aaron/hdim/issues/476
