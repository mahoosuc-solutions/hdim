# Measure Builder Centralized Testing and TDD Patch Plan

## Two-group execution model

- Operational readiness: `npm run test:measure-builder:readiness`
- Group A (static and unit): `npm run test:measure-builder:group-a`
- Group B (e2e and integration): `npm run test:measure-builder:group-b`
- Centralized orchestrator: `npm run test:measure-builder:centralized`
- Local runner shortcuts:
  - `npm run test:measure-builder:group-b:local`
  - `npm run test:measure-builder:centralized:local`
  - `npm run test:measure-builder:queue:local`

The orchestrator writes logs and a summary to `reports/measure-builder/`.

## Current findings

- Group A: PASS
  - TypeScript compile and focused Jest pass.
- Group B: FAIL in constrained local runtime
  - Web server startup can fail when port binding is restricted.
  - Chromium launch can fail in sandbox-restricted environments.
  - Failures occur before UI assertions execute.

## Error review

Observed e2e blockers are environment execution issues, not application assertion regressions:

- `PermissionError: [Errno 1] Operation not permitted` when web server tries to bind.
- Chromium launch sandbox fatal error (`sandbox_host_linux.cc`) in constrained runtime.

## TDD patch architecture

1. Test preflight contract first
- Add a preflight test that verifies:
  - `BASE_URL` is reachable before running UI flows.
  - Browser launch prerequisites are met for current runtime.
- Expected red state: clear preflight failure with actionable reason.

2. Implement preflight guardrails
- Add a reusable e2e preflight helper used by focused e2e suites.
- Fail fast with deterministic diagnostics when environment is not runnable.
- Preflight script: `scripts/validation/playwright-preflight.sh`

3. Harden browser runtime config
- Add explicit Chromium launch flags for restricted environments (for example: `--no-sandbox`, `--disable-setuid-sandbox`) behind an env toggle.
- Keep default secure behavior for CI where full sandbox is available.

4. Keep behavior assertions strict
- Preserve metadata dialog assertions:
  - transient `500` shows `role=alert` and `Retry`
  - retry success closes dialog and updates row text
  - field-level `400` errors remain inline while dialog stays open

5. Promote release gate
- Gate release on `test:measure-builder:centralized` in CI.
- Publish artifacts from `reports/measure-builder/` for audit and triage.

## Next release-point checks

- Run centralized gate in CI executor with browser and port permissions.
- Confirm Group B passes in at least one stable pipeline run.
- Freeze release candidate after one green centralized run and one repeatability run.

## Local runner queue mode

- Readiness script: `scripts/validation/ensure-system-operational.sh`
- Queue script: `scripts/validation/measure-builder-local-runner-queue.sh`
- Group B runner script: `scripts/validation/run-measure-builder-group-b.sh`
- Default local queue command: `npm run test:measure-builder:queue:local`
- Tunables:
  - `QUEUE_MAX_ATTEMPTS` (default `3`)
  - `QUEUE_SLEEP_SECONDS` (default `45`)
  - `PORT` (default `4210`)
  - `BASE_URL` (default `http://localhost:$PORT`)
  - `GATEWAY_URL` (default `http://localhost:18080`)
  - `AUTO_START_CLINICAL_PORTAL` (default `1`, overridden to `0` in `GROUP_B_SERVER_MODE=existing`)
  - `PW_CHROMIUM_NO_SANDBOX` (default `1` for local constrained runners)
  - `GROUP_B_SERVER_MODE` (`auto|existing|managed`, default `auto`)
- Artifacts:
  - `reports/measure-builder/local-runner-queue-*.log`
  - `reports/measure-builder/local-runner-queue-summary-*.md`
