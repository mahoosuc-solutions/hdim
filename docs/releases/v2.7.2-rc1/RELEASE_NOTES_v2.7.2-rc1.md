# Release Notes: v2.7.2-rc1

**Release Date (UTC):** 2026-02-27  
**Release Type:** Pre-release (compliance hardening and evidence tracking)  
**Tag:** `v2.7.2-rc1`

## Summary

This release consolidates technical investor diligence workstreams: test stabilization, compliance evidence gating, SOC2/HIPAA tracking, and release-audit traceability.

## Included Commits

- `9c16e483d` - Gate Testcontainers suites without Docker and stabilize analytics tests
- `b31388379` - Scope gateway clinical entity migration validation to module entities
- `662f9dd17` - Add investor stability report and compliance hardening backlog
- `c4badc488` - Add compliance evidence gate workflow and track backlog issues
- `97e3b5223` - Add compliance cadence policy, monthly snapshots, and ZAP baseline workflow

## Validation and Evidence Highlights

- Service test aggregation snapshot: `5852 tests`, `0 failures`, `0 errors`.
- Compliance gate workflow added:
  - `.github/workflows/compliance-evidence-gate.yml`
- Monthly evidence snapshot workflow added:
  - `.github/workflows/compliance-evidence-snapshot.yml`
- OWASP baseline workflow added:
  - `.github/workflows/owasp-zap-baseline.yml`
- Evidence and mapping documents:
  - `docs/TECHNICAL_INVESTOR_STABILITY_REPORT_2026-02-27.md`
  - `docs/compliance/SOC2_HIPAA_CVE_HARDENING_BACKLOG_2026-02-27.md`
  - `docs/compliance/SOC2_CC_CONTROL_EVIDENCE_MATRIX_2026-02-27.md`
  - `docs/compliance/COMPLIANCE_EVIDENCE_RETENTION_AND_CADENCE_POLICY_2026-02-27.md`

## Issue Tracking Status

- Closed:
  - `#499` P1: Add CI compliance evidence gate
  - `#501` P2: Define compliance evidence retention and cadence policy
- Open (expected):
  - `#497` P0: Complete backend CVE evidence with NVD data
  - `#498` P0: Sign and publish SOC2 control-evidence matrix (pending named sign-off)
  - `#500` P1: Add OWASP ZAP baseline artifact to evidence bundle (workflow dispatched)

## Known Release Blockers

1. Backend dependency-check strict evidence completion requires `NVD_API_KEY` to close `#497`.
2. SOC2 matrix sign-off names/date required to close `#498`.
3. First successful OWASP ZAP artifact run required to close `#500`.

## Traceability Tags

- `stability-baseline-2026-02-27` -> `b31388379`
- `compliance-gate-baseline-2026-02-27` -> `c4badc488`
- `v2.7.2-rc1` -> `97e3b5223`
