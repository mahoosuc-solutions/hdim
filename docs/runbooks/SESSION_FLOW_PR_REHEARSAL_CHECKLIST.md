# Session Flow PR Rehearsal Checklist

Use this checklist to validate merge-gate behavior before rollout.

## Goal

Confirm CI gating works as designed:

- Non-auth frontend PRs run `session-flow` only.
- Auth/login frontend PRs run both:
  - `session-flow`
  - `session-flow-external-auth`

## Rehearsal A — Non-auth Frontend Change

1. Create a branch with a small frontend-only change outside auth/session files (for example a copy update in non-auth UI).
2. Open PR targeting `develop` (or `main`, per policy).
3. Verify checks:
   - `Frontend Session Flow E2E / session-flow` appears and runs.
   - `session-flow-external-auth` does **not** run.
4. Verify PR template checklist completion.

## Rehearsal B — Auth/Login Change

1. Create a branch with a small change in one of:
   - `frontend/src/components/LoginPage.tsx`
   - `frontend/src/components/ConnectionStatus.tsx`
   - `frontend/src/components/AppShell.tsx`
   - `frontend/src/app/integrations/*`
2. Open PR targeting `develop` (or `main`).
3. Verify checks:
   - `Frontend Session Flow E2E / session-flow` runs.
   - `Frontend Session Flow E2E / session-flow-external-auth` runs.
4. Verify CODEOWNERS reviewer is requested for touched auth/session files.

## Evidence to Capture

- PR links for rehearsal A and B
- Screenshot of check list in each PR
- Final pass/fail summary in PR description
- Output of `npm --prefix frontend run detect:session-flow-checks` (including `session-flow`, `session-flow-external-auth`, and `auth-callback` predictions)
- GitHub Actions summary showing `gate-status-summary` output for each rehearsal PR

## Local Validation Commands

```bash
npm --prefix frontend run test:session-flow
npm --prefix frontend run e2e:session-flow
npm --prefix frontend run e2e:session-flow:external-auth
```
