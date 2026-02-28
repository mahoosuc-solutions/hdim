# Wave-1 Gateway Validation Report (2026-02-28)

## Scope
Validated Wave-1 gateway routing and end-to-end flow for:
- Revenue workflows (`/api/v1/revenue/**`)
- ADT interoperability workflows (`/api/v1/interoperability/adt/**`)

## Implementation Artifacts
- Gateway routing and tests are included in commit `37732b417`.
- Smoke runner: `scripts/validation/validate-wave1-edge-gateway-flow.sh`
- Stack orchestration:
  - `scripts/validation/start-wave1-validation-stack.sh`
  - `scripts/validation/stop-wave1-validation-stack.sh`

## Validation Evidence
- Canonical passing smoke artifact:
  - `test-results/wave1-edge-gateway-smoke-20260228T022934Z.json`
- Summary from artifact:
  - `totalChecks`: 7
  - `passed`: 7
  - `failed`: 0
  - `passRate`: 100%

## Operational Notes
- For deterministic local validation, run against the demo edge gateway on `http://127.0.0.1:18080` after starting the dedicated Wave-1 validation stack.
- If local host routing conflicts are observed, execute from a clean Docker state and re-run the stack start script.

## Runbook
1. `./scripts/validation/start-wave1-validation-stack.sh`
2. `GATEWAY_URL=http://127.0.0.1:18080 ./scripts/validation/validate-wave1-edge-gateway-flow.sh`
3. `./scripts/validation/stop-wave1-validation-stack.sh`
