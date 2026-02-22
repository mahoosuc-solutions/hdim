# Race Track FHIR Validation Summary

- Run timestamp (UTC): 2026-02-22T13:48:02Z
- Branch: feat/race-track-ab-validation-performance
- Environment: Demo local stack + deployed web URL checks

## Web Evidence (Visibility + Routing + Telemetry + Performance)

- Report: `web-validation-20260222T134504Z.md`
- Performance CSV: `web-performance-20260222T134504Z.csv`
- Vitals CSV: `web-vitals-20260222T134504Z.csv`
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

Broad N=10 rerun status:
- Latest rerun: `one-patient-n-measures-20260222T134802Z.md` + `.json` (FAIL)
- Observed 500s: `HEDIS-COU`, `HEDIS-FMC`, `HEDIS-AAB`

## Bug Tickets Created

- HEDIS-COU: https://github.com/webemo-aaron/hdim/issues/466
- HEDIS-FMC: https://github.com/webemo-aaron/hdim/issues/467
- HEDIS-AAB: https://github.com/webemo-aaron/hdim/issues/468

## Distinction

- Web claim visibility validated: the narrative and interaction contracts are present and routable.
- Backend claim execution validated for known pass sets; broad stress set remains blocked by measure-specific 500 errors.
