# Validation Summary

- Timestamp (UTC): 2026-02-22T18:49:51Z
- Scope: Post-merge local validation + production link health + workflow dispatch verification

## Web Claim Visibility Validation

Status: PASS (local post-merge)

- Forced routing/override/sticky behavior: PASS
- Claim language presence across A/B/print: PASS
- Telemetry wiring (`rtfp_view`, `rtfp_tab_click`, `rtfp_export_click`): PASS
- Performance gate: PASS under current local low-latency gate config

Artifacts:
- `web-validation-20260222T165902Z.md`
- `web-performance-20260222T165902Z.csv`
- `web-vitals-20260222T165902Z.csv`

## Backend Claim Execution Validation

Status: PASS (local post-merge)

- N=5 pass run: `one-patient-n-measures-20260222T165544Z.md`
- N=10 pass run: `one-patient-n-measures-20260222T165626Z.md`

Notes:
- Local default fallback measure set now uses supported IDs in this environment.

## Link Health Validation

Status: PASS

- Production: `portal-link-audit-web-gamma-snowy-38-vercel-app-20260222T154550Z.md`
- Local: `portal-link-audit-localhost-4174-20260222T184951Z.md`

Both runs report:
- Internal non-200 links: none
- External non-200/3xx links: none
- Discoverability gaps: none

## Workflow Dispatch Verification

Status: DISPATCHED (execution queued)

- Workflow: `.github/workflows/portal-deployment-validation.yml`
- Trigger time (UTC): 2026-02-22T18:47:09Z
- Run URL: https://github.com/webemo-aaron/hdim/actions/runs/22283103778
- Current status at publish time: `queued`
- Queue-stuck evidence: runs `22280100044`, `22281392909`, and `22283103778` are all in queued status.

## Backend Follow-up

- Umbrella tracking issue: https://github.com/webemo-aaron/hdim/issues/473
- Related existing issues: #466, #467, #468
