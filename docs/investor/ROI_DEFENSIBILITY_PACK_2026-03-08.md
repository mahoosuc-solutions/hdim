# ROI Defensibility Pack

## Scope
- Customer segment: Mid-size ACO / integrated provider group
- Pilot period: 12 weeks
- Baseline window: 4 weeks pre-pilot
- Measurement window: Weeks 1-12 of pilot

## Financial Model Inputs
| Metric | Baseline | Current | Delta | Source | Confidence |
|---|---|---|---|---|---|
| Care gap closure impact ($) | $0/week uplift | $24,000/week uplift | +$24,000/week | pilot scorecard + quality reconciliation | Medium |
| Denial reduction impact ($) | 12.0% avoidable denials | 10.8% (modeled week-4) | +$18,500/week | claims + remittance analysis | Medium |
| Manual labor reduction (hours/$) | 14 touches/case | 11 touches/case (modeled week-4) | 3 touches/case | workflow telemetry + staffing model | Medium |
| Avoided penalties/incentive uplift ($) | $0 recognized baseline | $7,500/week (modeled) | +$7,500/week | quality incentive model | Low-Med |

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
- Audit trail references:
  - `docs/compliance/REGULATORY_CONTROL_MATRIX_2026-03-07.md`
  - `docs/compliance/EVIDENCE_INDEX_2026-03-07.md`

## Confidence and Risk
| Risk | Impact | Mitigation | Owner |
|---|---|---|---|
| Data completeness variance | Medium | Weekly source reconciliation and exception log | Platform Lead |
| Attribution uncertainty | Medium | Matched baseline cohort + sensitivity ranges | Finance Lead |
| External policy changes | Low-Med | Quarterly model recalibration | Compliance Lead |

## Executive Summary
- Annualized ROI estimate: 2.1x (base case)
- Payback period estimate: 4.5 months
- Confidence band: 1.5x - 2.6x
- Key caveats:
  - Current figures are modeled from early pilot assumptions and must be refreshed weekly with observed values.
