Local implementation update (2026-02-23):

- Added quality-measure library fallback (raw measure ID and HEDIS-prefixed ID) in `MeasureCalculationService`.
- Seeded additional quality-measure definitions for `AAB`, `COU`, `FMC`.
- Added cql-engine Liquibase seed change for `HEDIS-SPC`, `HEDIS-AAB`, `HEDIS-COU`, `HEDIS-FMC` (demo tenant).
- Docker validation now passes for broad/default N=10 without curated fallback.

Evidence:
- `docs/marketing/web/evidence/one-patient-n-measures-20260223T005155Z.md` (N=10 broad/default source)
- `docs/marketing/web/evidence/one-patient-n-measures-20260223T005159Z.md` (N=10 active CQL source)
- `docs/marketing/web/evidence/validation-summary-20260223T005239Z.md`

Remaining for closure:
- production-hardening parity and CI workflow queue resolution (#475).
