# Clinical Portal Release Candidate

This document defines the release-ready path for the clinical portal surface in this repository. Use it when a branch is past feature work and needs a deterministic promotion path to staging and production.

## Release Scope

Primary shipped surface:

- `apps/clinical-portal`

Required coupled validation surfaces:

- backend APIs and dependencies exercised by `npm run e2e:clinical-portal:smoke`
- MCP release evidence and operator go/no-go scripts under `scripts/mcp/`
- GitHub Actions gates that cover hygiene, frontend CI, end-to-end validation, and release security

Explicitly out of scope unless the release owner adds them to the candidate packet:

- unrelated MFEs
- investor or marketing deployments
- net-new feature work that is not a release blocker

## Canonical Local Gate Order

Run from repo root:

```bash
bash scripts/release-validation/run-clinical-portal-release-candidate.sh baseline
```

This executes:

1. `npm run hygiene:audit`
2. `npm run lint:all`
3. `npm run test:all`
4. `npm run build:clinical-portal`
5. `npm run test:mcp`

Follow with environment-backed gates:

```bash
bash scripts/release-validation/run-clinical-portal-release-candidate.sh smoke
bash scripts/release-validation/run-clinical-portal-release-candidate.sh evidence
GO_NO_GO_MODE=strict bash scripts/release-validation/run-clinical-portal-release-candidate.sh go-no-go
```

Artifacts from the runner are written to `logs/release-candidate/<timestamp>/`.

## Required GitHub Workflows

These workflows form the minimum merge and promotion set for this release path:

- `.github/workflows/repo-hygiene.yml`
- `.github/workflows/frontend-ci.yml`
- `.github/workflows/e2e-tests.yml`
- `.github/workflows/mcp-release-gate.yml`
- `.github/workflows/release-security-gate.yml`

If one of these is intentionally waived, record the waiver and reason in the release notes before promotion.

## Staging And Production Path

1. Run the local baseline gate and fix every blocking failure before deployment.
2. Deploy to staging using the environment’s standard deploy path.
3. Run `npm run e2e:clinical-portal:smoke` against staging.
4. Generate the evidence pack with `npm run mcp:evidence-pack`.
5. Run `npm run mcp:operator:go-no-go -- --mode strict`.
6. Promote to production only after evidence and go/no-go are attached to the release packet.
7. If production validation fails, execute the rollback path in `docs/runbooks/CLINICAL_PORTAL_RELEASE_OPERATIONS.md`.

## Environment And Secrets Matrix

The release owner must verify these categories before staging promotion:

- Node 20+, Java 21, Docker, and Nx dependencies installed locally or in CI
- environment-specific API base URLs for the clinical portal
- authentication and tenant configuration required by the staging environment
- any secrets consumed by `scripts/mcp/*`, deployment workflows, or smoke suites
- registry, cluster, and cloud credentials required by the target deployment lane

Keep actual secret values out of git. Record only the source of truth for each secret in the release packet.

## Release Packet Checklist

- Scope locked to the clinical portal release candidate
- Baseline gate logs captured from `logs/release-candidate/<timestamp>/`
- Staging deploy identifier recorded
- Smoke test result recorded
- MCP evidence pack path recorded
- Operator go/no-go result recorded
- Rollback target identified before production deploy

## Failure Policy

- A failing baseline step blocks release until fixed or explicitly waived.
- A failing smoke, evidence, or strict go/no-go step blocks production promotion.
- Waivers must be documented with owner, date, risk, and rollback trigger.
