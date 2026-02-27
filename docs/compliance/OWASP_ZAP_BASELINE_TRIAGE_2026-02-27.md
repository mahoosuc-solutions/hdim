# OWASP ZAP Baseline Triage

**Date:** 2026-02-27  
**Tracking Issue:** #500  
**Workflow:** `.github/workflows/owasp-zap-baseline.yml`

## Summary

- Current status: Baseline workflow dispatched; waiting for runner completion.
- Latest dispatch:
  - Workflow run: `https://github.com/webemo-aaron/hdim/actions/runs/22501847597`
  - Status at update: `queued` (2026-02-27T20:04:31Z)
- Required artifacts after each run:
  - `report_html.html`
  - `report_md.md`
  - `report_json.json`
  - `validation-reports/zap-baseline-risk-summary.md`

## Triage Rules

- Critical findings: must be fixed before release.
- High findings: must be fixed before release.
- Medium findings: must be fixed or have explicit risk acceptance with owner and due date.
- Low/Informational findings: track in backlog with remediation window.

## Finding Register

| Alert | Risk | URL/Endpoint | Owner | Decision | Due Date | Evidence |
|---|---|---|---|---|---|---|
| Pending first scan | N/A | N/A | Security Eng | Pending run | 2026-03-01 | Populate from `report_json.json` |

## Sign-off

| Role | Name | Date (UTC) | Decision |
|---|---|---|---|
| Security Lead | mahoosuc-solutions | 2026-02-27T20:45:00Z | Pending run |
| QA Lead | mahoosuc-solutions | 2026-02-27T20:45:00Z | Pending run |
