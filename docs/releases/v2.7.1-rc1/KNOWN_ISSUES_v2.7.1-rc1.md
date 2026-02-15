# Known Issues: v2.7.1-rc1

**Release Version:** v2.7.1-rc1  
**Last Updated:** 2026-02-15

## Critical

- None currently identified for the demo gate (`./scripts/ci/local-ci.sh demo`).

## High

- `clinical-portal` unit tests are currently unstable and fail locally.
  - Symptom examples: `MatDialog is not defined`, missing mocks, spec import issues.
  - Impact: `nx run clinical-portal:test` is not a reliable gate for this RC.
  - Workaround: rely on `./scripts/ci/local-ci.sh demo` (boot/seed/validate + UI smoke) and `npm run e2e:clinical-portal`.

## Medium

- Legacy `frontend` project lint is not clean.
  - Impact: `nx run frontend:lint` fails with many pre-existing errors.
  - Workaround: keep it out of the default RC lint lane; track cleanup separately.

- `shared-notifications` unit tests are currently failing under the default TypeScript/Jest resolution.
  - Impact: `nx run shared-notifications:test` fails.
  - Workaround: excluded from the default `npm run test` lane for this RC.

- `libs/shared/testing` project has an incomplete Jest/TS config surface (missing tsconfig/jest config files).
  - Impact: `nx run testing:test` is not usable as a gate.
  - Workaround: excluded from `npm run test` lane.

## Notes

- Default local CI gates for this RC:
  - `./scripts/ci/local-ci.sh quick` (MCP + scoped lint + scoped unit tests)
  - `./scripts/ci/local-ci.sh demo` (full demo stack, deterministic seed, validate, screenshot smoke)
  - `./scripts/ci/local-ci.sh pr` (quick + dockerfile checks + `scripts/test-all-local.sh`)

