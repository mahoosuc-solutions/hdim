# Nx Operational Readiness Runbook

## Purpose

This runbook standardizes how to get the Clinical Portal operational on port `4210` before running release tests.

## Scope

- Local runner release validation for Measure Builder
- Nx serve startup reliability
- Playwright readiness-first execution

## Primary commands

```bash
npm run test:measure-builder:readiness
npm run test:measure-builder:group-b:local
npm run test:measure-builder:centralized:local
npm run test:measure-builder:queue:local
```

## Operator go/no-go commands

```bash
# Local runner decision (tenant policy permissive)
npm run mcp:operator:go-no-go:local

# Target environment promotion decision (tenant policy strict)
npm run mcp:operator:go-no-go:target
```

## Required service endpoints

- Clinical Portal UI: `http://localhost:4210`
- Gateway health: `http://localhost:18080/actuator/health`
- Quality endpoint via gateway: `/quality-measure/results?page=0&size=1`
- Care gap endpoint via gateway: `/care-gap/api/v1/care-gaps?page=0&size=1`

## Readiness-first flow

1. Run readiness check first.
2. Do not run Group B tests unless readiness passes.
3. Use queue mode for repeatability and release confidence.

## Known failure classes and actions

### `PORT_CONFLICT_4210`
- Cause: non-clinical process owns `4210`.
- Action:
```bash
ss -ltnp | rg 4210
lsof -i :4210 -n -P
```
- Stop conflicting process or set an explicit alternate `PORT` and `BASE_URL`.

### `UI_NOT_RUNNING`
- Cause: Clinical Portal not reachable.
- Action:
```bash
npx nx run clinical-portal:serve --port=4210 --verbose
```

### `STARTUP_FAILED`
- Cause: start command exits before readiness.
- Action:
1. Inspect startup logs under `reports/measure-builder/clinical-portal-startup-*.log`.
2. Run `npx nx reset`.
3. Remove stale lock if present: `.nx/workspace-data/project-graph.lock`.
4. Retry serve command.

### `UI_ROUTE_NOT_FOUND`
- Cause: non-SPA static server returns 404 for `/measure-builder`.
- Action:
- Use an SPA-aware server command from readiness flow.
- Avoid plain static servers that do not rewrite unknown paths to `index.html`.

### `GATEWAY_DOWN`
- Cause: Gateway on `18080` not healthy.
- Action:
```bash
./validate-system.sh
```

### `CORE_API_UNREACHABLE`
- Cause: quality/care-gap gateway routes unavailable.
- Action:
- Verify demo stack and route wiring.
- Re-run `./validate-system.sh` and inspect service health.

## Watcher (ENOSPC) mitigation

If dev serve fails with Watchpack ENOSPC:

1. Prefer build + static serving for release tests.
2. Stop excess watcher processes.
3. If needed, increase inotify limits on runner.

## Release-point criteria

- Readiness passes
- Group B passes
- Two consecutive queue summaries are `PASS`
- Latest centralized summary is `PASS`

## Artifacts

- `reports/measure-builder/operational-readiness-*.log`
- `reports/measure-builder/group-a-*.log`
- `reports/measure-builder/group-b-*.log`
- `reports/measure-builder/centralized-test-summary-*.md`
- `reports/measure-builder/local-runner-queue-summary-*.md`
