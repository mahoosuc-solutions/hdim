# Race Track FHIR Validation Summary

- Run timestamp (UTC): 2026-02-22T13:24:34Z
- Branch: feat/race-track-ab-validation-performance
- Environment: Demo local stack + deployed web URL checks

## Web Evidence (Visibility + Routing + Telemetry + Performance)

- Report: `web-validation-20260222T131952Z.md`
- Performance CSV: `web-performance-20260222T131952Z.csv`
- Vitals CSV: `web-vitals-20260222T131952Z.csv`
- Result: PASS

Validated:
- A/B forced routing and sticky assignment behavior
- Sticky override mutation behavior
- Fan-out claim language visibility across A/B/print assets
- Telemetry event contract wiring (`rtfp_view`, `rtfp_tab_click`, `rtfp_export_click`)
- Performance gate: variant median total load delta <= 15%

## API Evidence (One Patient -> N Measures)

Pass runs:
- N=5 baseline: `one-patient-n-measures-20260222T132409Z.md` + `.json` (PASS)
- N=10 stress pass sample: `one-patient-n-measures-20260222T132434Z.md` + `.json` (PASS)

Stress failure evidence:
- N=10 broad sample: `one-patient-n-measures-20260222T132419Z.md` + `.json` (FAIL)
- Observed 500s: `HEDIS-COU`, `HEDIS-FMC`, `HEDIS-AAB`

## Distinction

- Web claim visibility validated: the narrative and interaction contracts are present and routable.
- Backend claim execution validated: one patient can be evaluated against N measures in demo stack for the passing measure set; separate stress evidence captures failing measure IDs.
