---
status: pending
priority: p2
issue_id: "015"
tags: [frontend, admin-portal, lint, eslint, nx]
dependencies: []
---

# Remediate Admin Portal Lint Backlog

## Problem Statement

The `admin-portal` project now uses the workspace-standard Nx lint executor again, but the target still fails in practice because the codebase carries a large warning backlog. Leaving that state untracked makes the lint migration look complete when the project is still not operationally clean.

## Findings

- [`apps/admin-portal/project.json`](/mnt/wdblack/dev/projects/hdim-master/apps/admin-portal/project.json) now uses `@nx/eslint:lint`, which removes the custom target-shape regression.
- `npm run nx -- run admin-portal:lint` still exits non-zero with a generic Nx failure message, so the project is not in a clean lintable state yet.
- Direct ESLint execution shows the underlying lint surface is warning-heavy rather than config-broken:
  - `npx eslint "apps/admin-portal/**/*.{ts,html}" --config apps/admin-portal/eslint.config.mjs`
  - Result: `558 problems (0 errors, 558 warnings)`
- The warnings cluster around a few broad categories:
  - `@typescript-eslint/no-explicit-any`
  - `@typescript-eslint/no-unused-vars`
  - `@typescript-eslint/no-non-null-assertion`
  - `@angular-eslint/prefer-inject`
  - Angular template accessibility and control-flow warnings
- Representative hot spots include:
  - [`apps/admin-portal/src/app/pages/alert-config/alert-config.component.html`](/mnt/wdblack/dev/projects/hdim-master/apps/admin-portal/src/app/pages/alert-config/alert-config.component.html)
  - [`apps/admin-portal/src/app/pages/alert-config/alert-config.component.ts`](/mnt/wdblack/dev/projects/hdim-master/apps/admin-portal/src/app/pages/alert-config/alert-config.component.ts)
  - [`apps/admin-portal/src/app/services/alert.service.ts`](/mnt/wdblack/dev/projects/hdim-master/apps/admin-portal/src/app/services/alert.service.ts)
  - [`apps/admin-portal/src/app/services/investor.service.ts`](/mnt/wdblack/dev/projects/hdim-master/apps/admin-portal/src/app/services/investor.service.ts)
  - [`apps/admin-portal/src/app/services/logger.service.ts`](/mnt/wdblack/dev/projects/hdim-master/apps/admin-portal/src/app/services/logger.service.ts)

## Proposed Solutions

### Option 1

Reduce the warning backlog in focused batches by rule family and feature area.

Pros:
- Produces real lint progress without hiding debt.
- Lets the team tackle the worst files first.
- Keeps changes reviewable and testable.

Cons:
- Requires multiple passes.
- Some warnings may expose deeper API or template cleanup work.

Effort: Medium
Risk: Low

### Option 2

Relax the lint profile for `admin-portal` to silence the current warning classes.

Pros:
- Faster short-term green target.

Cons:
- Hides known quality debt.
- Pushes the problem into config instead of code.
- Risks diverging from workspace lint expectations.

Effort: Small
Risk: Medium

## Recommended Action

Use Option 1. Keep the standard Nx executor in place and reduce the warning backlog in bounded slices, starting with the highest-density files and the warning classes that are easiest to fix safely (`no-unused-vars`, trivial inferrable types, low-risk `prefer-inject` migrations, then explicit `any` and template accessibility/control-flow work).

## Acceptance Criteria

- [x] `admin-portal:lint` runs through the standard Nx executor with actionable output and no target-shape regressions.
- [ ] The current warning inventory is reduced through intentional code cleanup rather than lint-rule suppression.
- [x] The highest-density files in alert config and core services have an agreed first-pass remediation plan.

## Work Log

### 2026-03-12 - Backlog Captured After Executor Restore

**By:** Codex

**Actions:**
- Confirmed [`apps/admin-portal/project.json`](/mnt/wdblack/dev/projects/hdim-master/apps/admin-portal/project.json) now uses the standard `@nx/eslint:lint` executor.
- Ran `npm run nx -- run admin-portal:lint` and observed the target still fails at execution time.
- Ran direct ESLint with `npx eslint "apps/admin-portal/**/*.{ts,html}" --config apps/admin-portal/eslint.config.mjs` to capture the real warning surface.
- Recorded the current warning count (`558 warnings, 0 errors`) and the main file/rule clusters for follow-up work.

**Learnings:**
- The remaining `admin-portal` issue is code debt, not Nx executor misconfiguration.
- This should be handled as a separate lint-remediation effort rather than folded into the completed target-restore work.

### 2026-03-12 - First Warning Reduction Pass

**By:** Codex

**Actions:**
- Cleaned low-risk warnings in service files including [`investor-auth.service.ts`](/mnt/wdblack/dev/projects/hdim-master/apps/admin-portal/src/app/services/investor-auth.service.ts), [`logger.service.ts`](/mnt/wdblack/dev/projects/hdim-master/apps/admin-portal/src/app/services/logger.service.ts), [`prometheus.service.ts`](/mnt/wdblack/dev/projects/hdim-master/apps/admin-portal/src/app/services/prometheus.service.ts), [`sales-linkedin.service.ts`](/mnt/wdblack/dev/projects/hdim-master/apps/admin-portal/src/app/services/sales-linkedin.service.ts), [`sales-sequence.service.ts`](/mnt/wdblack/dev/projects/hdim-master/apps/admin-portal/src/app/services/sales-sequence.service.ts), [`sales.service.ts`](/mnt/wdblack/dev/projects/hdim-master/apps/admin-portal/src/app/services/sales.service.ts), [`investor.service.ts`](/mnt/wdblack/dev/projects/hdim-master/apps/admin-portal/src/app/services/investor.service.ts), and [`alert.service.ts`](/mnt/wdblack/dev/projects/hdim-master/apps/admin-portal/src/app/services/alert.service.ts).
- Removed unused imports and callback variables, dropped trivial type annotations, and converted a small set of services from constructor injection to `inject()`.
- Cleaned dead test locals in [`prometheus.service.spec.ts`](/mnt/wdblack/dev/projects/hdim-master/apps/admin-portal/src/app/services/prometheus.service.spec.ts).
- Validated touched files with direct ESLint runs and recalculated the project-wide warning count.

**Learnings:**
- The low-risk service pass reduced the warning inventory from `558` to `534` without any lint-rule suppression.
- The remaining highest-density work is now concentrated in template/control-flow accessibility warnings and `no-explicit-any` debt, especially in [`alert-config.component.html`](/mnt/wdblack/dev/projects/hdim-master/apps/admin-portal/src/app/pages/alert-config/alert-config.component.html) and [`admin.service.ts`](/mnt/wdblack/dev/projects/hdim-master/apps/admin-portal/src/app/services/admin.service.ts).

### 2026-03-12 - Alert Config Template Slice

**By:** Codex

**Actions:**
- Reworked [`alert-config.component.html`](/mnt/wdblack/dev/projects/hdim-master/apps/admin-portal/src/app/pages/alert-config/alert-config.component.html) from structural directives to Angular built-in control flow.
- Added keyboard/focus handling to modal overlays and associated dialog ARIA attributes.
- Fixed label associations and removed the component’s remaining low-risk TypeScript warnings in [`alert-config.component.ts`](/mnt/wdblack/dev/projects/hdim-master/apps/admin-portal/src/app/pages/alert-config/alert-config.component.ts).
- Re-ran direct ESLint for the component and for the full `admin-portal` surface.

**Learnings:**
- The `alert-config` slice alone removed `46` warnings and reduced the overall project warning count from `534` to `488`.
- The next highest-yield frontend targets are other Angular template-heavy pages such as the audit logs screens, not the already-trimmed service layer.

### 2026-03-12 - Enhanced Audit Logs Template Slice

**By:** Codex

**Actions:**
- Reworked [`audit-logs-enhanced.component.html`](/mnt/wdblack/dev/projects/hdim-master/apps/admin-portal/src/app/pages/audit-logs/audit-logs-enhanced.component.html) to use Angular built-in control flow throughout the statistics panel, table, pagination, and details modal.
- Added focusable modal-overlay behavior for the event-details dialog to satisfy template accessibility checks.
- Re-ran direct ESLint for the enhanced audit logs page and the full `admin-portal` surface.

**Learnings:**
- The enhanced audit logs page had `26` template warnings and is now clean.
- This reduced the overall `admin-portal` warning count from `488` to `462`.
- The remaining high-yield frontend targets are now the older audit logs page and the config versions page, while the largest TypeScript debt remains concentrated in [`admin.service.ts`](/mnt/wdblack/dev/projects/hdim-master/apps/admin-portal/src/app/services/admin.service.ts).

### 2026-03-12 - Legacy Audit Logs Page Slice

**By:** Codex

**Actions:**
- Updated the inline template in [`audit-logs.component.ts`](/mnt/wdblack/dev/projects/hdim-master/apps/admin-portal/src/app/pages/audit-logs/audit-logs.component.ts) to use Angular built-in control flow for table, empty state, pagination, loading state, and modal metadata.
- Converted the component from constructor injection to `inject()`.
- Added an explicit `closeDetailsModal()` path and improved the modal dialog semantics.
- Re-ran direct ESLint for the component and the full `admin-portal` surface.

**Learnings:**
- The legacy audit logs page had `13` warnings and is now clean.
- This reduced the overall `admin-portal` warning count from `462` to `449`.
- The next best frontend cleanup target is [`config-versions.component.ts`](/mnt/wdblack/dev/projects/hdim-master/apps/admin-portal/src/app/pages/config-versions/config-versions.component.ts), while the largest TypeScript-only debt still sits in [`admin.service.ts`](/mnt/wdblack/dev/projects/hdim-master/apps/admin-portal/src/app/services/admin.service.ts).
