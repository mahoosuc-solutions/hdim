# Race Track FHIR A/B Rollout Note (2026-02-21)

## What Changed
- Added Vercel-compatible static A/B routing for `race-track-fhir-pipeline` content.
- Introduced Variant B page: `docs/marketing/web/race-track-fhir-pipeline-b.html`.
- Introduced experiment router page: `docs/marketing/web/race-track-fhir-experiment.html`.
- Variant assignment uses sticky cookie `ab_rtfp` with 50/50 split and query override support (`?ab=a` or `?ab=b`).

## Files
- `docs/marketing/web/race-track-fhir-experiment.html`
- `docs/marketing/web/race-track-fhir-pipeline.html` (Variant A label + override links)
- `docs/marketing/web/race-track-fhir-pipeline-b.html` (Variant B content)
- `docs/marketing/web/vercel.json` (rewrite for experiment route)

## Routing Behavior
- Target paths:
  - `/race-track-fhir-pipeline` -> `/race-track-fhir-experiment.html`
- Experiment router redirect:
  - Variant A -> `/race-track-fhir-pipeline.html`
  - Variant B -> `/race-track-fhir-pipeline-b.html`
- Sticky assignment:
  - Cookie `ab_rtfp=a|b` (60 days)

## Review Steps
1. Open `/race-track-fhir-pipeline?ab=a` and confirm Variant A header text.
2. Open `/race-track-fhir-pipeline?ab=b` and confirm Variant B header text.
3. Open `/race-track-fhir-pipeline` without query params and confirm sticky behavior after first visit.

## Owner
- Marketing Web / Product Architecture collateral
