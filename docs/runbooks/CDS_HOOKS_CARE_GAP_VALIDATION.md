# CDS Hooks Care Gap Validation

Run this local validation before submitting CDS Hooks changes tied to care-gap recommendations.

## Scope

- CDS Hooks discovery + patient-view + order-select controller behavior
- Deterministic care-gap fixture paths (happy/empty/error)
- Integration/E2E verification via MockMvc

## Command

```bash
./scripts/validate-cds-hooks-care-gap.sh
```

## Expected Result

- Gradle exits successfully
- `CdsHooksControllerTest` passes
- `CdsHooksCareGapIntegrationTest` passes
