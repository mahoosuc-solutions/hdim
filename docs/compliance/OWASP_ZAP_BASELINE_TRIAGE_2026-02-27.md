# OWASP ZAP Baseline Triage

**Date:** 2026-02-27  
**Tracking Issue:** #500  
**Workflow:** `.github/workflows/owasp-zap-baseline.yml`

## Summary

- Current status: Baseline evidence captured locally using `securecodebox/zap` due hosted-runner billing block.
- Execution path:
  - Scanner image: `securecodebox/zap:latest` (direct daemon mode)
  - Target: `http://localhost:18080` (demo gateway)
  - Execution timestamp (UTC): `2026-02-27T22:37:00Z`
- Artifact location:
  - `test-results/zap-local-2026-02-27/report_html.html`
  - `test-results/zap-local-2026-02-27/report_json.json`
  - `test-results/zap-local-2026-02-27/alerts.json`
  - `test-results/zap-local-2026-02-27/alerts-summary.json`
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
| Timestamp Disclosure - Unix | Informational | `http://localhost:18080/robots.txt` | Security Eng | Accept (informational only, no sensitive payload exposure observed) | 2026-03-31 | `test-results/zap-local-2026-02-27/report_json.json` |
| Security finding summary | High: 0, Medium: 0, Low: 0, Informational: 3 | `http://localhost:18080` | Security Eng | Pass gate for Medium+ threshold | 2026-02-27 | `test-results/zap-local-2026-02-27/alerts-summary.json` |

## Sign-off

| Role | Name | Date (UTC) | Decision |
|---|---|---|---|
| Security Lead | mahoosuc-solutions | 2026-02-27T22:40:00Z | Approved (informational findings only) |
| QA Lead | mahoosuc-solutions | 2026-02-27T22:40:00Z | Approved (artifact captured and triaged) |
