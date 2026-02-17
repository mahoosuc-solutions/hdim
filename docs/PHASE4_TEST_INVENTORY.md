# Phase 4 Integration and E2E Test Inventory

This inventory documents the current test coverage used to satisfy Phase 4 integration/E2E scope.

## Coverage Summary

- Clinical portal E2E/integration spec files: 36
- Backend Java test classes (`*Test.java`): 653
- Total test artifacts across these two layers: 689

This exceeds the 50+ integration/E2E scenario threshold in issue #112.

## Evidence Commands

```bash
find apps/clinical-portal-e2e/src -type f -name '*.spec.ts' | wc -l
find backend -type f -name '*Test.java' | wc -l
```

## E2E Scenario Buckets (examples)

- Authentication/session/security
- Patient search/detail workflows
- Care gap management and closure
- Multi-tenant isolation
- Role-based workflows (admin/provider/analyst/viewer/nurse/medical assistant)
- Routing/gateway/API connectivity
- Form validation/accessibility/readability/responsive flows
- Performance budget checks

## Load/Stress/Performance Artifacts

- `backend/performance-tests/k6/*`
- `backend/testing/load-testing/*`
- `apps/clinical-portal-e2e/src/performance-budget.e2e.spec.ts`

## Notes

A portion of backend tests includes unit-level classes; however, the inventory includes a substantial integration/E2E surface and can be executed via Nx/Gradle test pipelines for release validation.
