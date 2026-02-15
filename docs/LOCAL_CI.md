# Local CI

This repo includes a deterministic local CI runner so we can validate before pushing to GitHub.

## Entry Point

```bash
./scripts/ci/local-ci.sh --help
```

## Lanes

### `quick`

Fast, offline-friendly checks:

- `npm run test:mcp`
- `npm run lint` (scoped RC lint)
- `npm run test` (scoped RC unit tests)

```bash
./scripts/ci/local-ci.sh quick
```

### `pr`

PR gate: quick + deeper checks.

```bash
./scripts/ci/local-ci.sh pr
```

Notes:

- Runs `./scripts/validate-dockerfiles.sh` (if present).
- Runs `./scripts/test-all-local.sh` which can also run the demo stack + Playwright.
- Optional: set `RUN_SLOW_LINT=1` to enable slower lint targets inside `scripts/test-all-local.sh`.
- Playwright: by default `scripts/test-all-local.sh` runs a fast chromium-only smoke suite (`npm run e2e:clinical-portal:smoke`).
  - Set `RUN_FULL_E2E=1` to run the larger demo-safe suite (`npm run e2e:clinical-portal:demo`).

### `demo`

Deterministic demo gate and capture readiness:

```bash
./scripts/ci/local-ci.sh demo
```

This lane:

- builds required backend `bootJar`s
- `docker compose -f docker-compose.demo.yml up -d --build`
- seeds (`SEED_PROFILE=smoke` by default)
- validates (`./validate-system.sh`)
- runs agent-browser smoke screenshots (if available)

### `release`

Release-docs generation helper:

```bash
VERSION=vX.Y.Z-rcN ./scripts/ci/local-ci.sh release
```

## Nx Scope (Best-Practice Note)

This workspace contains multiple Nx project families (Angular MFEs, backend services, and legacy/experimental surfaces).
The default `npm run lint` and `npm run test` are intentionally scoped for the RC lane to keep gates deterministic.

For broader enforcement:

- `npm run lint:all`
- `npm run test:all`

See `docs/releases/v2.7.1-rc1/KNOWN_ISSUES_v2.7.1-rc1.md` for current exclusions.
