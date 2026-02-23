---
date: 2026-02-21
plan_id: race-track-fhir-pipeline-visualization
status: in-progress
owner: codex
---

# Race Track FHIR Pipeline Visualization Plan

## Objective
Build a tabbed architecture artifact that presents one canonical patient data journey across three audience tiers:
- Non-technical (illustrative race-track)
- Mixed (hybrid metaphor + architecture)
- Technical (full Kafka/Redis/service internals)

## Scope
- Interactive HTML page for demos and stakeholder walkthroughs.
- Static SVG exports for deck/docs reuse.
- Explicit system boundaries:
  - Customer FHIR server and customer CDR/EHR
  - Internal FHIR server(s)
  - Kafka and Redis processing core
  - FHIR-native write-back to customer systems
- Narrative proof points:
  - Healthix
  - HealthInfoNet
  - Verato

## Deliverables
- `docs/marketing/web/race-track-fhir-pipeline.html`
- `docs/marketing/web/assets/race-track-fhir/view-non-technical.svg`
- `docs/marketing/web/assets/race-track-fhir/view-mixed.svg`
- `docs/marketing/web/assets/race-track-fhir/view-technical.svg`
- `docs/marketing/web/assets/race-track-fhir/comparison-3-views.svg`
- `docs/marketing/web/assets/race-track-fhir/legend.svg`

## Design Constraints
- Same canonical journey and labels across all tabs.
- Increasing technical depth from tab 1 to tab 3.
- Simple, presentation-safe language in non-technical view.
- Accurate platform semantics in technical view (topics, retry/DLQ, cache roles).

## Canonical Journey
1. Patient Event Created
2. Data Ingested
3. Kafka Stream Routed
4. Service Processing
5. Redis Context Lookup
6. Care Rule Evaluation
7. Action Generated
8. Consumer Delivery
9. Outcome + Audit

## Acceptance Criteria
- User can switch tabs without losing context of the same journey.
- Customer/internal data boundaries are visually clear in all views.
- Kafka topics and Redis roles are visible in mixed and technical views.
- Healthix, HealthInfoNet, and Verato are present as credibility framing.
- Static SVG assets exist and correspond to each tab + comparison + legend.

## Validation
- Verify files exist in expected paths.
- Open HTML to ensure tab switching works and no missing references.
- Confirm label consistency against canonical flow.

## Next Steps
1. Build first visual version.
2. Review wording with sales/product stakeholders.
3. Iterate color and density for projector readability.
