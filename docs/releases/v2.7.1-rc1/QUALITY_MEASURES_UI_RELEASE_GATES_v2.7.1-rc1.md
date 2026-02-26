# Quality Measures UI Release Gates (v2.7.1-rc1)

## Scope
This checklist covers the quality-measures UI hardening completed on February 24, 2026:
- Quality Measures page backend wiring
- Quality Measure Detail page backend wiring
- Evaluation patient autocomplete and selection guardrails
- Regression tests for service wiring and evaluation flow

## Required Gates
1. Unit/component tests pass:
```bash
npx jest --config apps/clinical-portal/jest.config.ts \
  apps/clinical-portal/src/app/pages/quality-measures/quality-measures.component.spec.ts \
  apps/clinical-portal/src/app/pages/quality-measure-detail/quality-measure-detail.component.spec.ts \
  apps/clinical-portal/src/app/services/evaluation.service.spec.ts --runInBand
```

2. Type checks pass:
```bash
npx tsc -p apps/clinical-portal/tsconfig.app.json --noEmit
npx tsc -p apps/mfe-quality/tsconfig.app.json --noEmit
```

3. E2E spec is listed and runnable:
```bash
npx playwright test --config apps/clinical-portal-e2e/playwright.config.ts \
  apps/clinical-portal-e2e/src/quality-measures-patient-evaluation.e2e.spec.ts --list
```

4. Environment-backed E2E run in staging or demo stack:
```bash
npx playwright test --config apps/clinical-portal-e2e/playwright.config.ts \
  apps/clinical-portal-e2e/src/quality-measures-patient-evaluation.e2e.spec.ts --project=chromium
```

## Manual Acceptance
1. Open `/quality-measures` and verify measure cards load from live service data.
2. Confirm `Evaluation Patient` autocomplete lists valid patients by name and MRN.
3. Verify `Run Evaluation` is disabled until a patient is selected.
4. Select a measure and run evaluation; confirm results panel appears and completion status is shown.
5. Open `View Full Details` and validate patient results/care gaps/trends load without hardcoded placeholders.

## Release Risk Notes
- If `/quality-measure/measures/local` is unavailable, list view will surface empty state.
- If `/quality-measure/calculate-local` fails, evaluation remains blocked with an error banner.
- If patient data from FHIR is unavailable, patient autocomplete will be empty and evaluation action stays disabled.

## Rollback Criteria
Rollback this slice if any of the following occur in production smoke tests:
- Quality measures list never populates for valid tenant data.
- Evaluation button can be triggered without selected patient context.
- Detail view fails to render measure-level data for known measure IDs.
