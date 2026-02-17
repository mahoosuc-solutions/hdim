# Session Flow Release Readiness Evidence

Use this template to capture final validation before merge/release.

## Local Validation

- [x] `npm --prefix frontend run detect:session-flow-checks`
- [x] `npm --prefix frontend run test:session-flow`
- [ ] `npm --prefix frontend run e2e:session-flow`
- [ ] `npm --prefix frontend run e2e:session-flow:external-auth`
- [ ] `npm --prefix frontend run e2e:auth-callback`

Attach outputs/links:

- detect output: `session-flow=true`, `session-flow-external-auth=true`, `auth-callback=true` (local diff fallback mode)
- test suite output: `test:session-flow` passed (`7 files`, `19 tests`, `0 failed`)
- e2e session-flow run:
- e2e external-auth run:
- e2e auth-callback run:

Latest execution note:
- `npm --prefix frontend run run:session-flow-handoff -- --with-e2e` reached e2e stage after `test:session-flow` passed (`19/19`), but Playwright webServer startup failed in this environment (`Process from config.webServer was not able to start. Exit code: 1`).

## Manual Workflow Dispatch Rehearsal

Workflow: `Frontend Session Flow E2E` (`.github/workflows/frontend-session-flow-e2e.yml`)

Modes to run:

- [ ] `gate_mode=base`
- [ ] `gate_mode=auth`
- [ ] `gate_mode=all`

Optional helper script:

```bash
./scripts/ci/run-session-flow-workflow-modes.sh
```

Current status:
- Attempted dispatch script; blocked because `gh` auth is not configured in this environment (`gh auth login` required).

Run links and outcomes:

| Mode | Run URL | `session-flow` | `session-flow-external-auth` | `auth-callback` | `gate-status-summary` |
|------|---------|----------------|------------------------------|-----------------|-----------------------|
| base |         |                |                              |                 |                       |
| auth |         |                |                              |                 |                       |
| all  |         |                |                              |                 |                       |

## PR Rehearsal Evidence

- [ ] Rehearsal A (non-auth frontend change) completed
- [ ] Rehearsal B (auth/login change) completed

PR links:

- Rehearsal A:
- Rehearsal B:

## Branch Protection Confirmation

- [ ] `MCP Release Gate / release-gate` required
- [ ] `Frontend Session Flow E2E / session-flow` required
- [ ] `Frontend Session Flow E2E / session-flow-external-auth` required for auth/login PRs
- [ ] `Frontend Session Flow E2E / auth-callback` required for auth/login PRs
- [ ] CODEOWNERS requested on auth/session file changes

## Sign-Off

- Reviewer:
- Date:
- Final decision: Go / No-Go
