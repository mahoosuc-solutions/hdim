# Nx Version Alignment

This repo follows the rule: **pin `nx` and every `@nx/*` package to the same exact version**.

Why:
- Prevents multiple Nx versions in `node_modules` (subtle CLI/runtime differences).
- Keeps `nx` and `@nx/*` plugin executors consistent.
- Avoids non-deterministic behavior from semver ranges.

## Current Policy

- `nx` and all `@nx/*` packages are pinned (no `^` ranges).
- `nx-mcp` is pinned and invoked via a wrapper, so MCP does not depend on `@latest`.

See:
- `.mcp.json`
- `scripts/mcp/README.md`

## Upgrade Nx

1. Update via Nx migrate:
```bash
npx nx migrate nx@<version>
npm install
```

2. If migrations were generated:
```bash
npx nx migrate --run-migrations
npm install
```

3. Verify:
```bash
npx nx report
npm ls nx @nx/angular @nx/devkit --depth=2
```

Expected:
- `npx nx report` shows a single consistent `nx` / `@nx/*` version.
- `npm ls` does not show duplicate Nx versions.

