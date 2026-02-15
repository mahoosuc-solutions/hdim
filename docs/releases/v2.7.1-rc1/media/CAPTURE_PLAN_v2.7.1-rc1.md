# Capture Plan: v2.7.1-rc1

This plan is aligned to the market-validated differentiators in `docs/PHASE_2_MARKET_RESEARCH_SUMMARY.md` (Section 10).

## Preconditions

- Demo readiness gate passes: `docs/DEMO_READINESS_GATE.md`
- Local demo gate run:
```bash
./scripts/ci/local-ci.sh demo
```

## Output Locations

- Screenshots: `docs/releases/v2.7.1-rc1/media/screenshots/`
- Videos: `docs/releases/v2.7.1-rc1/media/videos/`

## Screenshot “Hero Set” (Required)

1. Predictive care gaps on dashboard (30-60 day look-ahead)
   - `01-predictive-care-gaps-dashboard.png`
2. Drilldown to patient context
   - `02-patient-details.png`
3. Care gaps execution loop (prioritization and next best action)
   - `03-care-gaps-list.png`
   - `04-care-gap-details.png`
4. FHIR-native proof point (gateway)
   - `05-fhir-metadata.png` (show `/fhir/metadata`)
5. Auditability proof point
   - `06-audit-log.png`
   - `07-audit-event-details.png`

## Automated Screenshot Pass (Agent Browser)

When the portal is running at `http://localhost:4200`:

```bash
./scripts/media/capture-release-screenshots.sh v2.7.1-rc1 smoke
```

If you want the full suite:

```bash
./scripts/media/capture-release-screenshots.sh v2.7.1-rc1 all
```

## Video Clips (Recommended)

Keep each clip 30-90 seconds. Use a fixed viewport (1440x900) and seeded demo data.

1. Predictive to action (differentiator: predictive care gaps)
   - Start: dashboard predicted gaps
   - End: open a patient/care gap detail and state the next step
2. Care gaps execution loop (differentiator: execution, not reporting)
   - Start: care gaps list (prioritized)
   - End: document an action or update status (if enabled)
3. FHIR-native platform (differentiator: interoperability/ECDS posture)
   - Start: `/fhir/metadata`
   - End: fetch one resource type and show consistent patient identifiers with the UI

## Messaging Guardrails (No Over-Claim)

- If the real-time financial ROI dashboard is not demonstrably working end-to-end in this build, do not record it as a primary differentiator. Position as preview/roadmap only.
