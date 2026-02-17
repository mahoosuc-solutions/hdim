# MCP Context-Aware Release Gate

This runbook defines how to run policy-aware platform validation and persist knowledge artifacts for audit and troubleshooting.

## Purpose

Use this gate to make release decisions with context:

- Runtime topology and readiness
- Configuration completeness
- Tenant isolation policy
- Seed/validation health

The gate supports `strict` and `permissive` policy modes.

## Command

Run from repository root:

```bash
npm run mcp:release-gate -- --mode strict
```

Generated artifacts:

- JSON report: `logs/mcp-reports/release-gate-<timestamp>.json`
- Markdown summary: `logs/mcp-reports/release-gate-<timestamp>.md`

## Modes

- `strict`: fails gate when tenant header is not enforced for non-allowlisted endpoints.
- `permissive`: allows gate pass but emits warnings for those same findings.

## Common Options

- `--mode strict|permissive`
- `--allow-no-header <endpoint-prefix>` (repeatable)
- `--include-logs`
- `--no-system-validate`
- `--skip-frontend`
- `--skip-fhir-query`
- `--compose-file <path>`
- `--gateway-url <url>`
- `--demo-seeding-url <url>`
- `--tenant-a <id>`
- `--tenant-b <id>`
- `--out-dir <path>`

## Example Profiles

### Production Candidate (blocking)

```bash
npm run mcp:release-gate -- \
  --mode strict \
  --compose-file docker-compose.demo.yml \
  --gateway-url http://localhost:18080 \
  --demo-seeding-url http://localhost:8098
```

### Staging/Iteration (warn-only on tenant header)

```bash
npm run mcp:release-gate -- \
  --mode permissive \
  --allow-no-header /fhir/metadata \
  --allow-no-header /actuator/health \
  --no-system-validate
```

## Interpretation

- `pass=true`: gate conditions satisfied for selected mode.
- `pass=false`: at least one blocking condition failed.
- `summary.tenantPolicyPass=false`: tenant policy violated for current mode.
- `tenantIsolation.violations`: blocking findings.
- `tenantIsolation.warnings`: non-blocking findings in permissive mode.

## Recommended Policy Lifecycle

1. Start with `permissive` during rollout and monitor warnings.
2. Add explicit allowlist entries only for truly public endpoints.
3. Move to `strict` for release candidates and production promotion.
4. Track recurring violations and create remediation issues per endpoint.

## CI/CD Integration (Code Changes Only)

This repository uses a change-triggered workflow (not nightly cron) for MCP gate checks:

- Workflow: `.github/workflows/mcp-release-gate.yml`
- Triggers: `pull_request` and `push` on MCP/platform-related file changes
- CI mode: `permissive` (non-blocking on known tenant-header warnings)
- Artifacts: uploaded `logs/mcp-reports/*` for each run

Use strict mode manually for promotion decisions:

```bash
npm run mcp:release-gate -- --mode strict
```
