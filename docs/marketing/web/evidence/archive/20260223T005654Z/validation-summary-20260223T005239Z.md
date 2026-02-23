# Validation Summary

- Timestamp (UTC): 2026-02-23T00:52:39Z
- Scope: Docker demo-stack retest after backend fixes for broad/default measure support

## Key Result

- Broad/default N=10 validation now passes without fallback curation for:
  - `AAB`
  - `COU`
  - `FMC`
  - `SPC`

## Local Canonical Validation (Latest)

- Web A/B + performance: PASS
  - `web-validation-20260222T230000Z.md`
  - `web-performance-20260222T230000Z.csv`
  - `web-vitals-20260222T230000Z.csv`
- Portal link audit (local): PASS
  - `portal-link-audit-localhost-4174-20260222T230000Z.md`
  - `portal-link-audit-localhost-4174-20260222T230000Z.csv`
- One patient to N measures (post-fix): PASS
  - N=5 broad/default source: `one-patient-n-measures-20260223T005239Z.md`
  - N=10 broad/default source: `one-patient-n-measures-20260223T005155Z.md`
  - N=10 active CQL library source: `one-patient-n-measures-20260223T005159Z.md`

## Platform Validation (Demo Profile)

- `scripts/validate-containers.sh`: PASS (0 failed, optional warnings only)
- `validate-fhir-data.sh`: PASS (17 passed, 0 failed)
- `scripts/validate-all-services-data.sh`: PASS (0 failed, optional empty/skipped only)

## Production Canonical Reference

- Portal link audit (production): PASS
  - `portal-link-audit-web-gamma-snowy-38-vercel-app-20260222T154550Z.md`
  - `portal-link-audit-web-gamma-snowy-38-vercel-app-20260222T154550Z.csv`

## Workflow Dispatch Verification

- Workflow: `portal-deployment-validation.yml`
- Branch: `master`
- Run URL: https://github.com/webemo-aaron/hdim/actions/runs/22283112071 (queued)
- Run URL: https://github.com/webemo-aaron/hdim/actions/runs/22284191276 (queued)
- Run URL: https://github.com/webemo-aaron/hdim/actions/runs/22286089694 (queued)
- Blocker issue: https://github.com/webemo-aaron/hdim/issues/475

## Backend Tracking

- Umbrella tracker: #476
- Measure-specific historical threads: #466, #467, #468, #474
