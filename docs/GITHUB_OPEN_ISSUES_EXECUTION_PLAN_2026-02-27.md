# GitHub Open Issues Execution Plan

**Date:** 2026-02-27  
**Repository:** `webemo-aaron/hdim`  
**Scope:** 11 open issues (#276-#285, #36)

---

## 1. Priority-Ordered Backlog

1. #277 - Epic: TEFCA/HIE Connectivity (`priority:critical`, `domain:interoperability`)
2. #276 - Epic: Revenue Cycle Management Integration (`priority:critical`, `domain:financial`)
3. #282 - Epic: CMS Quality Program Compliance Dashboard (`priority:high`, `domain:regulatory`)
4. #278 - Epic: Price Transparency Compliance (`priority:high`, `domain:regulatory`)
5. #281 - Epic: Patient Attribution & Panel Management (`priority:high`, `domain:value-based-care`)
6. #279 - Epic: Utilization Management & Case Management (`priority:high`, `domain:clinical-ops`)
7. #283 - Epic: Operational Analytics for Hospital Leadership (`priority:medium`, `domain:operations`)
8. #284 - Epic: Benchmarking & Comparative Analytics (`priority:medium`, `domain:analytics`)
9. #280 - Epic: Provider Credentialing & Enrollment (`priority:medium`, `domain:workforce`)
10. #285 - Hospital COO/CIO Feature Roadmap (`documentation`, `roadmap`)
11. #36 - Record Clinical Portal Demo Video (unlabeled)

---

## 2. Milestone Waves (Recommended)

## Wave 0: Planning + Architecture Baseline (Milestone [#13](https://github.com/webemo-aaron/hdim/milestone/13), due 2026-03-05)
- Issues: #285, #36 (pre-production planning artifacts), architecture spikes for #277/#276.
- Outcome:
  - Approved architecture ADR set for interoperability + revenue integrations.
  - Demo/video script aligned to current product state and investor narrative.

## Wave 1: Critical Integration Foundation (Milestone [#14](https://github.com/webemo-aaron/hdim/milestone/14), due 2026-03-19)
- Issues: #277, #276.
- Outcome:
  - TEFCA/HIE integration contract and first production-ready connector path.
  - Revenue cycle integration interface and reconciliation flow baseline.

## Wave 2: Regulatory + Clinical Value (Milestone [#15](https://github.com/webemo-aaron/hdim/milestone/15), due 2026-04-09)
- Issues: #282, #278, #281, #279.
- Outcome:
  - CMS and price-transparency compliance surfaces.
  - Core patient attribution and utilization workflows in place.

## Wave 3: Leadership Analytics + Workforce (Milestone [#16](https://github.com/webemo-aaron/hdim/milestone/16), due 2026-04-23)
- Issues: #283, #284, #280.
- Outcome:
  - Executive analytics/benchmarking package.
  - Credentialing/enrollment workflow integration.

**Status (2026-02-27):** Milestones created and all 11 open issues assigned.

---

## 3. Owner Lanes

## SWE Lane
- Deliver API/service implementation and UI integration by wave.
- Maintain dependency order:
  - #277 and #276 before downstream analytics/compliance epics.
- Required implementation artifacts per issue:
  - Architecture note.
  - Integration contract/schema.
  - Feature flags and rollback path.

## QA Lane
- Create per-wave test plans (unit, integration, e2e).
- Add regression packs before closing each issue.
- Gate each wave with:
  - `npm run test:mcp`
  - `bash scripts/validation/validate-data-access-security.sh`
  - domain-specific integration test runs for implemented epics

## Compliance Lane
- Track SOC2/HIPAA evidence continuity per wave.
- Maintain evidence package updates in:
  - `docs/compliance/SECURITY_COMPLIANCE_VALIDATION_2026-02-27.md`
  - `docs/compliance/HIPAA_SOC2_EVIDENCE_BUNDLE_INDEX_2026-02-27.md`
- Required gate for wave completion:
  - `bash scripts/security/validate-phase4-hipaa-controls.sh`
  - `node scripts/mcp/operator-go-no-go.mjs --mode strict --profile production`

---

## 4. Dependency and Risk Notes

- #277 and #276 are critical path items. Delay here cascades into #282/#283/#284.
- #282 and #278 are regulatory-facing and should complete before investor/customer readiness packaging.
- #285 and #36 should be refreshed after Wave 2 so collateral reflects implemented capabilities, not roadmap-only claims.
- Backend CVE evidence remains an explicit release dependency once `NVD_API_KEY` is available.

---

## 5. Wave Exit Criteria

Each wave is complete only when all conditions pass:

1. Issue acceptance criteria met and linked in issue comments.
2. Automated tests passing for touched modules.
3. Compliance evidence docs updated with new artifacts.
4. Strict go/no-go artifact generated in `logs/mcp-reports/`.
5. No open high/critical security findings in touched dependency surfaces.

---

## 6. Suggested Cadence

- Monday: scope lock + dependency review.
- Wednesday: midpoint quality/compliance checkpoint.
- Friday: wave gate run + evidence packaging + issue status update.

This cadence keeps engineering throughput aligned with investor-grade evidence quality.
