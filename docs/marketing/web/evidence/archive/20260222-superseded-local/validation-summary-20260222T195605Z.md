# Validation Summary

- Timestamp (UTC): 2026-02-22T19:56:05Z
- Scope: Post-merge local validation refresh + workflow dispatch verification

## Local Post-Merge Validation (Canonical)

- Web A/B + performance: PASS
  - `web-validation-20260222T195532Z.md`
  - `web-performance-20260222T195532Z.csv`
  - `web-vitals-20260222T195532Z.csv`
- Portal link audit (local): PASS
  - `portal-link-audit-localhost-4174-20260222T195532Z.md`
  - `portal-link-audit-localhost-4174-20260222T195532Z.csv`
- One patient to N measures (local API): PASS
  - N=5: `one-patient-n-measures-20260222T195559Z.md`
  - N=10: `one-patient-n-measures-20260222T195605Z.md`

## Production Canonical Reference

- Portal link audit (production): PASS
  - `portal-link-audit-web-gamma-snowy-38-vercel-app-20260222T154550Z.md`
  - `portal-link-audit-web-gamma-snowy-38-vercel-app-20260222T154550Z.csv`

## Workflow Dispatch Verification

- Workflow: `portal-deployment-validation.yml`
- Branch: `master`
- Run URL (initial): https://github.com/webemo-aaron/hdim/actions/runs/22283112071
- Run URL (re-dispatch): https://github.com/webemo-aaron/hdim/actions/runs/22284191276
- Status: `queued` (both runs)
- Conclusion: pending
- Blocker issue: https://github.com/webemo-aaron/hdim/issues/475

## Known Backend Follow-Up

- Open issues for missing broad/default measure support:
  - #466 (`HEDIS-COU`)
  - #467 (`HEDIS-FMC`)
  - #468 (`HEDIS-AAB`)
  - #474 (`HEDIS-SPC`)
  - #475 (`portal-deployment-validation` workflow queue-stuck)
