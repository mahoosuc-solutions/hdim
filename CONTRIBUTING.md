# Contributing to HDIM

Thank you for contributing to HealthData-in-Motion.

## Pull Request Workflow

1. Create a focused branch from `develop` (or `main`, based on team policy).
2. Keep changes scoped to a single objective.
3. Run relevant local validation before opening a PR.
4. Complete the PR checklist in `.github/pull_request_template.md`.

## Required Validation

- Run tests relevant to the files changed.
- For frontend session/auth or connection-status changes, run:
  - `npm --prefix frontend run test:session-flow`
- For browser validation of session expiry flow, run:
  - `npm --prefix frontend run e2e:session-flow`

## Merge Gates and Branch Protection

Before merge, ensure branch protection and required checks are satisfied:

- `MCP Release Gate / release-gate`
- `Frontend Session Flow E2E / session-flow` (for `frontend/**` changes)

Reference: `docs/runbooks/CI_BRANCH_PROTECTION_CHECKLIST.md`

## CI/CD Policy

- CI runs on code changes and manual workflow dispatch.
- No nightly/scheduled requirement for these merge gates.

## Additional References

- PR template: `.github/pull_request_template.md`
- Session flow workflow: `.github/workflows/frontend-session-flow-e2e.yml`
- Frontend test commands: `frontend/README.md`
