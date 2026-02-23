# Validation Summary

- Timestamp (UTC): 2026-02-22T23:00:34Z
- Scope: Local docker demo-stack validation refresh + evidence canonicalization

## Local Canonical Validation (Latest)

- Web A/B + performance: PASS
  - `web-validation-20260222T230000Z.md`
  - `web-performance-20260222T230000Z.csv`
  - `web-vitals-20260222T230000Z.csv`
- Portal link audit (local): PASS
  - `portal-link-audit-localhost-4174-20260222T230000Z.md`
  - `portal-link-audit-localhost-4174-20260222T230000Z.csv`
- One patient to N measures (local API): PASS
  - N=5: `one-patient-n-measures-20260222T230031Z.md`
  - N=10: `one-patient-n-measures-20260222T230034Z.md`

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

## Known Backend Follow-Up

- Open issues for missing broad/default measure support:
  - #476 (umbrella tracker)
  - #466 (`HEDIS-COU`)
  - #467 (`HEDIS-FMC`)
  - #468 (`HEDIS-AAB`)
  - #474 (`HEDIS-SPC`)
