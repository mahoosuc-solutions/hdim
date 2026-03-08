# Risk and Mitigation Register (One-Pager)

**Date:** 2026-03-08  
**Release Lane:** `v0.0.0-test`  
**Policy:** No-waiver for pilot critical controls

## Top Risk Domains
| Risk Domain | Current Risk | Mitigation | Owner | Status |
|---|---|---|---|---|
| Security/AuthZ regression | Medium | Critical control gates + tenant/auth regression logs | Security Lead | Controlled |
| Evidence staleness | Medium | Freshness validator with fail-closed decision | Release Manager | Controlled |
| Access governance drift | Medium | Current-cycle access review + recertification checks | Security Lead | Controlled |
| Third-party compliance drift | Medium | Vendor risk register + scheduled cadence reviews | Compliance Lead | Controlled |
| Operational recovery failure | Medium | DR drill evidence with RTO/RPO validation | SRE Lead | Controlled |
| Clinical safety edge cases | Medium | Workflow-level safety cases + escalation SLAs | Clinical Ops | In progress |

## Control Anchors
- Regulatory controls: `docs/compliance/REGULATORY_CONTROL_MATRIX_2026-03-07.md`
- Gap state: `docs/compliance/GAP_REGISTER_2026-03-07.md`
- Readiness decision: `docs/compliance/RELEASE_READINESS_SCORECARD_2026-03-07.md`
- Freshness enforcement: `docs/releases/v0.0.0-test/validation/evidence-freshness-report.md`

## Trigger Conditions (Automatic NO-GO)
1. Any critical control failure (`RC-SEC`, `RC-TEN`, `RC-RBAC`, `RC-REL`, `RC-CNT`, `RC-CI`).
2. Missing or stale required evidence beyond cadence window.
3. Reopened critical/high compliance gaps without closure evidence.

## Immediate Next 24h Focus
1. Complete clinical safety case approvals for top workflows.
2. Re-run readiness validators after any material change.
3. Confirm customer-facing go-live packet is internally signed.
