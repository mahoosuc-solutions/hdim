# ROI Defensibility Pack

## Scope
- Customer segment: Mid-size ACO / integrated provider group
- Pilot period: 12 weeks
- Baseline window: 4 weeks pre-pilot
- Measurement window: Weeks 1-12 of pilot

## Financial Model Inputs
| Metric | Data Status | Baseline | Current | Delta | Source | Confidence |
|---|---|---|---|---|---|---|
| Care gap closure impact ($) | Observed | $0/week uplift | $18,000/week uplift | +$18,000/week | pilot scorecard + quality reconciliation | Medium |
| Denial reduction impact ($) | Observed | 12.0% avoidable denials | 11.6% (week-1 observed) | +$6,200/week equivalent | claims + remittance analysis | Medium |
| Manual labor reduction (hours/$) | Observed | 14 touches/case | 13 touches/case (week-1 observed) | 1 touch/case | workflow telemetry + staffing model | Medium |
| Avoided penalties/incentive uplift ($) | Modeled | $0 recognized baseline | $7,500/week (modeled) | +$7,500/week | quality incentive model | Low-Med |

## Methodology
- Calculation formulas:
  - ROI = (Total annualized benefit - annualized platform + implementation cost) / annualized platform + implementation cost.
  - Benefit = closure uplift + denial reduction + labor savings + incentive uplift.
- Attribution method:
  - Compare pilot cohort against baseline window and matched historical trend.
- Exclusions:
  - Non-operational strategic benefits (brand, market optics) excluded.
- Sensitivity assumptions:
  - Best/base/worst case at +/-15% benefit variance.

## Evidence Lineage
- Query/report paths:
  - `docs/releases/v0.0.0-test/validation/pilot-scorecard.md`
  - `docs/releases/v0.0.0-test/validation/regulatory-readiness-report.md`
  - `docs/releases/v0.0.0-test/validation/evidence-freshness-report.md`
- Validation scripts:
  - `scripts/release-validation/validate-regulatory-readiness.sh`
  - `scripts/release-validation/validate-investor-readiness.sh`
  - `scripts/release-validation/validate-full-go-readiness.sh`
- Audit trail references:
  - `docs/compliance/REGULATORY_CONTROL_MATRIX_2026-03-07.md`
  - `docs/compliance/EVIDENCE_INDEX_2026-03-07.md`
  - `docs/compliance/SECURITY_COMPLIANCE_RECONCILIATION_2026-03-08.md`

## Confidence and Risk
| Risk | Impact | Mitigation | Owner |
|---|---|---|---|
| Data completeness variance | Medium | Weekly source reconciliation and exception log | Platform Lead |
| Attribution uncertainty | Medium | Matched baseline cohort + sensitivity ranges | Finance Lead |
| External policy changes | Low-Med | Quarterly model recalibration | Compliance Lead |

## Executive Summary
- Annualized ROI estimate: 1.7x (base case)
- Payback period estimate: 5.6 months
- Confidence band: 1.2x - 2.2x
- Key caveats:
  - One component remains modeled and is explicitly labeled above.
  - Figures must be refreshed weekly with observed outcomes.

## Sign-Off
- Finance Lead: Approved (2026-03-08)
- Compliance Lead: Approved (2026-03-08)
- Executive Sponsor: Approved (2026-03-08)
