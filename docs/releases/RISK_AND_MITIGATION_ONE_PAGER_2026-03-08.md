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
| Clinical safety governance | Medium | Approved workflow-level safety cases + escalation SLAs | Clinical Ops | Controlled |
| Modeled-to-observed drift | Medium | Weekly KPI/ROI refresh and trend snapshots | Finance + Clinical Ops | Controlled |

## Control Anchors
- Regulatory controls: `docs/compliance/REGULATORY_CONTROL_MATRIX_2026-03-07.md`
- Gap state: `docs/compliance/GAP_REGISTER_2026-03-07.md`
- Readiness decision: `docs/compliance/RELEASE_READINESS_SCORECARD_2026-03-07.md`
- Full-go closure: `docs/releases/v0.0.0-test/validation/full-go-readiness-report.md`
- Reconciliation note: `docs/compliance/SECURITY_COMPLIANCE_RECONCILIATION_2026-03-08.md`

## Trigger Conditions (Automatic NO-GO)
1. Any critical control failure (`RC-SEC`, `RC-TEN`, `RC-RBAC`, `RC-REL`, `RC-CNT`, `RC-CI`).
2. Missing or stale required evidence beyond cadence window.
3. Reopened critical/high compliance gaps without closure evidence.
4. Reintroduction of pending sign-offs in strict full-go artifacts.

## Next 30-Day Hardening Focus
1. Maintain weekly trend snapshots for KPI and ROI evidence.
2. Run scheduled strict readiness workflow and archive artifacts.
3. Keep residual-risk section current in all customer/investor decks.
