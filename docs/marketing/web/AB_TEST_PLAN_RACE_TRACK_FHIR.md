# A/B Test Plan: Race Track FHIR Pipeline

Date: 2026-02-21

## Objective
Determine which narrative structure better improves understanding and engagement for the architecture story.

- Variant A: Story-first progression.
- Variant B: Boundary-first progression.

## Experiment Entry
- URL: `/race-track-fhir-pipeline`
- Router: `race-track-fhir-experiment.html`
- Sticky cookie: `ab_rtfp`
- Forced review params: `?ab=a` and `?ab=b`

## Measurement Events
Tracked client-side events (stored in `window.dataLayer` and localStorage key `rtfp_ab_metrics`):
- `rtfp_view`
- `rtfp_tab_click`
- `rtfp_export_click`

## Primary Metric
- Engaged View Rate
  - Definition: sessions with at least one tab click or export click
  - Win threshold: +10% versus alternate variant

## Secondary Metrics
- Technical Drilldown Rate
  - Definition: sessions with click to technical tab (`panel-3`)
  - Win threshold: +8%
- Bounce Proxy
  - Definition: view event only (no follow-up interactions)
  - Target: -8%
- Evidence Intent Proxy
  - Definition: clicks on `race-track-fhir-evidence.html`
  - Win threshold: +5%

## Experiment Window
- Minimum 14 days
- Minimum 500 sessions per variant before decision

## Decision Rule
Choose winner only if:
1. Primary metric threshold is met.
2. No significant degradation in Bounce Proxy.
3. Technical Drilldown Rate does not decrease by more than 3%.

If no clear winner, keep A for simplicity and refine B copy for a follow-up test.

## Notes
This experiment validates communication effectiveness, not backend correctness. Backend claim validation is tracked separately via:
- `scripts/validate-one-patient-n-measures.sh`
- `race-track-fhir-evidence.html`
