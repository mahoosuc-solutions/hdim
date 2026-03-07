# AI-Human Release Orchestration (HDIM)

## Purpose
Define explicit handoff and approval steps between AI agents and human operators from validation start through release tag.

## Roles
- AI Operator: runs scripted validation, generates evidence, summarizes failures and retries.
- Release Manager (Human): approves phase transitions, validates risk posture, owns final go/no-go decision.
- Service Owners (Human): approve exceptions for failing gates in their domain.

## Required Inputs Before Phase 1
- Target version (for example `v0.0.0-test`)
- Clean branch strategy agreed
- Environment selected (`local`, `staging`, `production-candidate`)
- Incident/rollback contacts confirmed
- Runtime polling budget confirmed:
  - frontend `__HDIM_OPS_POLL_MS` default `10000` (clamped `2000-60000`)
  - ops-server `STATUS_CACHE_TTL_MS` default `5000` (min `500`)

## Preflight Stability Gate (Mandatory)
1. AI runs stack readiness checks before phase execution:
   - `scripts/release-validation/validate-release-preflight.sh <version>`
   - Confirm all required services are `Up` and `healthy` (especially `hdim-demo-fhir`, `hdim-demo-seeding`, gateways).
2. AI records preflight status artifact in:
   - `docs/releases/<version>/validation/preflight-stability-report.md`
3. Human release manager explicitly approves preflight before Phase 1 begins.
4. If any required service is unhealthy, mark `NO-GO` until remediated.

## Phase Handoffs (Mandatory)
1. AI executes one phase from `scripts/release-validation/run-release-validation.sh`.
2. AI posts:
   - phase completion token (`PHASE_X_COMPLETE`)
   - pass/fail summary
   - links to generated reports under `docs/releases/<version>/validation/`
3. Human validates summary and either:
   - approves transition to next phase, or
   - blocks and assigns remediation owner.
4. Human approval token is required between phases (`PHASE_X_APPROVED`).

## Evidence Checklist Per Phase
- Exit code evidence for each required script.
- Generated report paths and timestamps.
- Explicit list of waived/failed checks (must include owner + ETA).
- Diff of workflow-impacting config changes when applicable.

## Go/No-Go Rules
- `NO-GO` if any blocking task in workflow JSON fails without signed waiver.
- `NO-GO` if authz/tenant-isolation validation fails.
- `NO-GO` if production rollback procedure is missing or untested for the target version.
- `GO` requires:
  - all blocking checks passing or approved waivers,
  - release manager approval,
  - tag command performed manually by human:
    - `git tag -a <version> -m "Release <version>"`
    - `git push origin <version>`

## Exception Protocol
- Any waiver must document:
  - failing check
  - impact/risk
  - mitigation
  - owner
  - expiration date
- Waivers are recorded in `docs/releases/<version>/logs/workflow-summary.md`.

## Operational Notes
- Use strict contract testing by default for deployment console ops responses.
- Use legacy compatibility mode only for transitional environments and record usage in evidence.
