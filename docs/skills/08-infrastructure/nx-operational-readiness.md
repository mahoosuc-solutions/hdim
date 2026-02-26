# Nx Operational Readiness Skill

## Intent

Use this skill when you need to stabilize Nx-based local execution before running release tests.

## When to apply

- `nx serve` exits unexpectedly
- Playwright tests fail before assertions
- Port conflicts or watcher limits block startup
- Need deterministic readiness gating for release

## Standard command set

```bash
npm run test:measure-builder:readiness
npm run test:measure-builder:group-b:local
npm run test:measure-builder:centralized:local
npm run test:measure-builder:queue:local
```

## Decision rules

1. Readiness must pass before Group B.
2. If readiness fails, classify failure code and remediate.
3. Prefer existing-server mode when UI is already up.
4. Use queue mode for repeatability evidence.

## Failure-to-action map

- `PORT_CONFLICT_4210` -> identify owner, stop/switch
- `STARTUP_FAILED` -> inspect startup logs, reset Nx, retry
- `UI_ROUTE_NOT_FOUND` -> switch to SPA-aware server
- `GATEWAY_DOWN` -> restore gateway health on 18080
- `CORE_API_UNREACHABLE` -> restore quality/care-gap routes

## Evidence requirements

Collect these for release review:

- readiness logs
- group-b logs
- centralized summary
- queue summary (2 passing runs)
