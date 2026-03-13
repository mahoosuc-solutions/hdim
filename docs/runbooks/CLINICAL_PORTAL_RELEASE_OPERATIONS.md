# Clinical Portal Release Operations

This runbook is the operator-facing execution path for a clinical portal release. Use it with `docs/release/CLINICAL_PORTAL_RELEASE_CANDIDATE.md`.

## Preconditions

- Release scope is locked.
- Staging and production owners are identified.
- Rollback target is known before deployment starts.
- Required GitHub workflows are green or have written waivers.

## Preflight

Run from repo root:

```bash
git status --short --branch
npm run hygiene:audit
bash scripts/release-validation/run-clinical-portal-release-candidate.sh baseline
```

Stop here if any command fails.

## Staging Rehearsal

1. Deploy the candidate build to staging with the normal environment-specific deployment mechanism.
2. Confirm the deployed revision or image digest and record it in the release ticket.
3. Run the clinical portal smoke gate:

```bash
bash scripts/release-validation/run-clinical-portal-release-candidate.sh smoke
```

4. Generate the evidence bundle:

```bash
bash scripts/release-validation/run-clinical-portal-release-candidate.sh evidence
```

5. Run the strict operator decision gate:

```bash
GO_NO_GO_MODE=strict bash scripts/release-validation/run-clinical-portal-release-candidate.sh go-no-go
```

Promote only if all three staging checks succeed.

## Production Promotion

1. Announce the deployment window and rollback target.
2. Deploy the same artifact that passed staging.
3. Re-run the smoke suite against production if the environment permits it.
4. Record:
   - production deploy identifier
   - evidence pack location
   - go/no-go output
   - operator owner and timestamp

## Rollback

Trigger rollback immediately if:

- the smoke suite fails after deploy
- the operator go/no-go fails in strict mode
- the portal is unavailable or materially degraded

Execute the environment rollback command or script for the last known good artifact, then:

1. verify the portal is reachable
2. verify the key health endpoints used by the environment
3. re-run the smoke suite if possible
4. attach the rollback identifier and reason to the release ticket

If Kubernetes is the active lane, use the existing rollback helper:

```bash
bash scripts/k8s-rollback.sh <environment> --force --verify
```

## Required Evidence

Keep these artifacts with the release record:

- baseline logs from `logs/release-candidate/<timestamp>/`
- staging and production deployment IDs
- MCP evidence pack output
- strict go/no-go output
- rollback target and rollback result, if used

## Common Failure Modes

- `nx-mcp` startup is slow in WSL or cold dependency states
  Fix: use the repo-local wrapper and timeout in `.codex/config.toml`.
- smoke tests fail because staging data or auth configuration is incomplete
  Fix: verify the target environment secrets and tenant config before redeploying.
- evidence pack or go/no-go fails because upstream reports are missing
  Fix: run baseline first, then regenerate evidence.
