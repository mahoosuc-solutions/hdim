# Validation Summary

- Timestamp (UTC): 2026-02-22T15:45:50Z
- Scope: Race Track FHIR A/B web validation + portal deployment link audit + one-patient-to-N-measures API validation

## Web Claim Visibility vs Execution Truth

- Web claim visibility validation: PASS
  - A/B routing, sticky cookie behavior, override behavior, claim text presence, and telemetry event wiring all validated.
  - Evidence: `web-validation-20260222T154047Z.md`
- Backend claim execution validation: MIXED
  - N=5 baseline: PASS (`one-patient-n-measures-20260222T154252Z.md`)
  - N=10 stress: FAIL due to 500s on `HEDIS-COU`, `HEDIS-FMC`, `HEDIS-AAB` (`one-patient-n-measures-20260222T154304Z.md`)

## Portal Deployment Link Health

- Production link audit: PASS
  - Internal non-200 links: 0
  - External non-200/3xx links: 0
  - Sitemap/discoverability gaps: none
  - Evidence: `portal-link-audit-web-gamma-snowy-38-vercel-app-20260222T154550Z.md`

## Performance Gate Status

- Latest web perf gate sample: FAIL
  - Variant A median total: 0.239465s
  - Variant B median total: 0.129605s
  - Slower-variant delta: 84.77% (>15% threshold)
  - Evidence: `web-validation-20260222T154047Z.md`, `web-performance-20260222T154047Z.csv`

## Notes

- API validation against direct service URL (`http://localhost:8087/quality-measure`) currently returns `403` without auth token in this environment.
- Reproducible baseline/stress API results were produced via gateway URL (`http://localhost:18080/quality-measure`) with `API_MODE=b`.
