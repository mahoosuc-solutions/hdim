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

## Workflow Queue State

- https://github.com/webemo-aaron/hdim/actions/runs/22286089694 (queued)
- https://github.com/webemo-aaron/hdim/actions/runs/22284191276 (queued)
- https://github.com/webemo-aaron/hdim/actions/runs/22283112071 (queued)
- Queue blocker: https://github.com/webemo-aaron/hdim/issues/475

## Current Demo Stack Validation

- `VALIDATION_PROFILE=demo ./scripts/validate-containers.sh`
  - Result: PASS (0 failures, warnings only for optional services)
- `VALIDATION_PROFILE=demo bash ./validate-fhir-data.sh`
  - Result: PASS (17 passed, 0 failed)
- `VALIDATION_PROFILE=demo bash ./scripts/validate-all-services-data.sh`
  - Result: PASS with warnings (0 failures, some optional/empty endpoints)

## Backend Tracking

- Umbrella: https://github.com/webemo-aaron/hdim/issues/476
