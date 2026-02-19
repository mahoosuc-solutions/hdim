# CI Branch Protection Checklist

Operational checklist to enforce merge gates on `main` and `develop`.

## Scope

- Applies to GitHub branch protection rules.
- CI runs on code changes and manual dispatch.
- No nightly/scheduled requirement for this checklist.

## Required Branch Protection Settings

Configure for both `main` and `develop`:

1. **Require pull request before merging**
2. **Require approvals** (minimum: 1; recommended: 2 for production-critical changes)
3. **Dismiss stale approvals when new commits are pushed**
4. **Require status checks to pass before merging**
5. **Require branches to be up to date before merging**
6. **Include administrators** (recommended)
7. **Block force pushes and deletions**

## Recommended Required Status Checks

Use the exact job/check names from GitHub Actions UI after first successful run.

- `MCP Release Gate / release-gate`
- `Frontend Session Flow E2E / session-flow`
- `Frontend Session Flow E2E / session-flow-external-auth` (required for auth/login-related PRs; workflow runs conditionally)
- `Frontend Session Flow E2E / auth-callback` (required for auth/login-related PRs; workflow runs conditionally)
- `Landing Page Validation / content-validation` (required for `landing-page-v0/**` PRs)
- `Landing Page Validation / link-check` (required for `landing-page-v0/**` PRs)
- `Landing Page Validation / lint-and-build` (required for `landing-page-v0/**` PRs)
- `Landing Page Core Validation / validate-ci` (optional fallback gate for `landing-page-v0/**` PRs when heavy jobs are queued)
- Existing backend CI checks already required by your policy (`backend-ci*` workflows)

## Frontend Session Flow Gate

Workflow: `.github/workflows/frontend-session-flow-e2e.yml`

Expected behavior:

- Runs on `push`/`pull_request` affecting `frontend/**`
- Can be triggered manually with `workflow_dispatch`
- Executes browser flow for session expiry and login recovery
- Executes external-auth browser flow when auth/login-related files change
- Executes auth-callback browser flow when auth/login-related files change
- Uploads Playwright report artifact on success/failure
- Publishes final gate status summary (`gate-status-summary` job) indicating executed/skipped state

## Verification Steps

1. Open GitHub repository settings.
2. Go to **Branches** → branch protection rule for `main` and `develop`.
3. Confirm all settings above are enabled.
4. Confirm required checks include session-flow and release gate checks.
5. Open a PR touching `frontend/**` and verify `Frontend Session Flow E2E` appears and must pass.
6. Open a PR touching `landing-page-v0/**` and verify `Landing Page Validation` core checks appear and must pass.
7. Confirm PR checklist references this runbook (`.github/pull_request_template.md`).
8. Confirm CODEOWNERS requests review for auth/session files (`.github/CODEOWNERS`).

## Rollback (If Misconfigured)

1. Temporarily remove only the failing required check from branch protection.
2. Fix workflow/job naming mismatch.
3. Re-add the required check immediately after validation.
