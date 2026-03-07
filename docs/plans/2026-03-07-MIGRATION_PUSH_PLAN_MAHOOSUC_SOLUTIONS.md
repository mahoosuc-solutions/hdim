# Migration and Push Plan: `webemo-aaron/hdim` -> `mahoosuc-solutions/hdim`

**Date:** 2026-03-07

## 1) Scope for Initial Push (Curated Baseline)

Recommended initial migration scope:

- `LICENSE`
- `SECURITY.md`
- `NOTICE`
- `README.md`
- `CONTRIBUTING.md`
- `CODE_OF_CONDUCT.md`
- `.github/ISSUE_TEMPLATE/bug-report.md`
- `.github/ISSUE_TEMPLATE/feature-request.md`
- `.github/ISSUE_TEMPLATE/config.yml`
- `.github/pull_request_template.md`
- `docs/legal/*`
- `docs/commercial/*`
- `docs/finance/CURRENT_STRUCTURE_FINANCIAL_FORECAST_2026-2028.md`
- `docs/compliance/ENV_SECRET_AUDIT_2026-03-07.md`
- `docs/compliance/GITLEAKS_HISTORY_SCAN_2026-03-07.md`
- `docs/compliance/COPYRIGHT_HEADER_COVERAGE_2026-03-07.md`
- `docs/plans/2026-03-07-himss-open-source-launch-punchlist.md`
- `docs/plans/2026-03-07-CODE_COMPLETENESS_AND_MIGRATION_READINESS.md`

## 2) Preconditions

- `mahoosuc-solutions/hdim` exists and is writable by active account.
- External launch blockers tracked separately (domain/email/attorney).
- Curated baseline reviewed and approved.

## 3) Execution Steps (after approval)

1. Create migration branch:
   - `git checkout -b migration/mahoosuc-curated-baseline`
2. Stage only curated files.
3. Commit:
   - `git commit -m "Prepare curated launch baseline for mahoosuc-solutions"`
4. Add target remote:
   - `git remote add mahoosuc https://github.com/mahoosuc-solutions/hdim.git`
5. Push baseline branch:
   - `git push -u mahoosuc migration/mahoosuc-curated-baseline:main`
6. Validate on GitHub:
   - files present, branch default, protections enabled.

## 4) Known Constraint

Attempt to create target repo failed due to permission error:

- `webemo-aaron cannot create a repository for mahoosuc-solutions`

Action required: create repo from `mahoosuc-solutions` account/org admin or grant
permissions.

