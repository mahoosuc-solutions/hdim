# Session Flow Handoff Summary

Consolidated reference for session-expiry implementation, validation, and CI merge gates.

## Core Commands (Local / WSL)

```bash
# Run the full local handoff sequence (detect + tests)
npm --prefix frontend run run:session-flow-handoff

# Predict which CI checks should run from your current branch diff
npm --prefix frontend run detect:session-flow-checks

# Run targeted session/auth test suite
npm --prefix frontend run test:session-flow

# Run live/manual checklist output
npm --prefix frontend run verify:session-flow

# Run browser e2e (standard session-expiry flow)
npm --prefix frontend run e2e:session-flow

# Run browser e2e (external auth redirect flow)
npm --prefix frontend run e2e:session-flow:external-auth

# Run browser e2e (auth callback token handling)
npm --prefix frontend run e2e:auth-callback

# Run full handoff including all browser e2e
npm --prefix frontend run run:session-flow-handoff -- --with-e2e
```

## CI Workflows

- Frontend session gates: `.github/workflows/frontend-session-flow-e2e.yml`
  - Job: `session-flow`
  - Job: `session-flow-external-auth` (conditional)
  - Job: `auth-callback` (conditional)
  - Job: `gate-status-summary` (always runs, explains decisions/results)
  - Manual dispatch input: `gate_mode` = `all` | `base` | `auth`
- Release gate: `.github/workflows/mcp-release-gate.yml`

## Required / Recommended Status Checks

Use exact check names from GitHub UI:

- `MCP Release Gate / release-gate`
- `Frontend Session Flow E2E / session-flow`
- `Frontend Session Flow E2E / session-flow-external-auth` (required for auth/login-related PRs)
- `Frontend Session Flow E2E / auth-callback` (required for auth/login-related PRs)

## Conditional Gate Logic

`session-flow-external-auth` runs when:

- workflow event is `workflow_dispatch`, or
- auth/login files changed (detected by `detect-session-auth-changes`).

`auth-callback` uses the same conditional trigger.

Reference filter source: `.github/workflows/frontend-session-flow-e2e.yml`.

## Key Files Implemented

- Session-aware integrations client/polling:
  - `frontend/src/app/integrations/client.ts`
  - `frontend/src/app/integrations/healthPolling.ts`
  - `frontend/src/app/integrations/useIntegrationsHealth.ts`
  - `frontend/src/app/integrations/sessionExpiry.ts`
- UI/session recovery:
  - `frontend/src/components/ConnectionStatus.tsx`
  - `frontend/src/components/LoginPage.tsx`
  - `frontend/src/components/ExternalAuthMockPage.tsx`
  - `frontend/src/components/AppShell.tsx`
- Browser e2e:
  - `frontend/e2e/tests/session-expiry.spec.ts`
  - `frontend/e2e/tests/session-expiry-external-auth.spec.ts`
  - `frontend/e2e/playwright.session.config.ts`
  - `frontend/e2e/playwright.session.external-auth.config.ts`

## Governance and Review

- PR template: `.github/pull_request_template.md`
- CODEOWNERS: `.github/CODEOWNERS`
- Branch protection checklist: `docs/runbooks/CI_BRANCH_PROTECTION_CHECKLIST.md`
- PR rehearsal checklist: `docs/runbooks/SESSION_FLOW_PR_REHEARSAL_CHECKLIST.md`
- GitHub Actions troubleshooting: `.github/GITHUB_ACTIONS_TROUBLESHOOTING.md`

## Suggested Final Verification Order

1. Run `detect:session-flow-checks`
2. Run `test:session-flow`
3. Run `e2e:session-flow`
4. Run `e2e:session-flow:external-auth`
5. Run `e2e:auth-callback`
6. Open rehearsal PRs (non-auth + auth) and confirm expected CI gate behavior
7. Confirm branch protection required checks

For release sign-off evidence, use:

- `docs/runbooks/SESSION_FLOW_RELEASE_READINESS_EVIDENCE.md`
- `scripts/ci/run-session-flow-workflow-modes.sh`
