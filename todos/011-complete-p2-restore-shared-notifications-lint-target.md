---
status: complete
priority: p2
issue_id: "011"
tags: [frontend, nx, lint, notifications]
dependencies: []
---

# Restore Shared Notifications Lint Target

## Problem Statement

The `shared-notifications` lint target was changed from the standard Nx ESLint executor to a custom command target. That introduces two regressions: it starts linting spec files that were previously excluded, and it stops participating in the usual Nx lint caching/input behavior.

## Findings

- [`libs/shared/notifications/project.json`](/mnt/wdblack/dev/projects/hdim-master/libs/shared/notifications/project.json) now uses `nx:run-commands` with a raw TypeScript glob for linting.
- That glob includes spec files, but existing tests still rely on `any`-based doubles in [`libs/shared/notifications/src/lib/alert.component.spec.ts`](/mnt/wdblack/dev/projects/hdim-master/libs/shared/notifications/src/lib/alert.component.spec.ts) and [`libs/shared/notifications/src/lib/notification.service.spec.ts`](/mnt/wdblack/dev/projects/hdim-master/libs/shared/notifications/src/lib/notification.service.spec.ts).
- Because the target no longer uses `@nx/eslint:lint`, it also stops inheriting the workspace lint executor behavior and cache/input invalidation patterns configured in [`nx.json`](/mnt/wdblack/dev/projects/hdim-master/nx.json).

## Proposed Solutions

### Option 1

Restore the standard Nx ESLint executor and make file inclusions/exclusions explicit there.

Pros:
- Matches the rest of the workspace.
- Preserves cache behavior and standard invalidation.
- Avoids accidental linting of tests unless intended.

Cons:
- Requires a small target/config rewrite.

Effort: Small
Risk: Low

### Option 2

Keep `nx:run-commands` and manually replicate exclusions plus cache inputs.

Pros:
- Minimal change to the current approach.

Cons:
- Easy to drift from workspace conventions again.
- Harder to maintain than the built-in executor.

Effort: Medium
Risk: Medium

## Recommended Action

Use Option 1. Revert the project to the standard Nx lint executor unless there is a strong reason not to, and only expand lint coverage to specs after fixing those test files intentionally.

## Acceptance Criteria

- [x] `shared-notifications:lint` uses the workspace-standard ESLint executor or an equivalent configuration with matching cache/input semantics.
- [x] Spec files are excluded unless the team explicitly decides to lint them.
- [x] If specs are included, the existing `any`-based tests are updated so the target passes cleanly.

## Work Log

### 2026-03-12 - Review Finding Captured

**By:** Codex

**Actions:**
- Reviewed the `shared-notifications` lint target change and compared it with workspace conventions.
- Confirmed the new target widens the lint surface to existing spec files and bypasses normal Nx lint target behavior.

**Learnings:**
- This is a tooling/config regression rather than a user-facing runtime defect, but it can still break CI and make lint results less trustworthy.

### 2026-03-12 - Partial Remediation Applied

**By:** Codex

**Actions:**
- Restored [`libs/shared/notifications/project.json`](/mnt/wdblack/dev/projects/hdim-master/libs/shared/notifications/project.json) to the workspace-standard `@nx/eslint:lint` executor.
- Confirmed through Nx workspace metadata that the lint target once again uses the standard executor and inherits normal cache/input handling.
- Re-ran `npm run hygiene:audit` successfully.

**Learnings:**
- The local Nx CLI was slow/stalled for direct command validation in this workspace, but the Nx MCP project details confirmed the target shape and inherited cache inputs.

### 2026-03-12 - Todo Closed With Direct Validation

**By:** Codex

**Actions:**
- Verified [`libs/shared/notifications/project.json`](/mnt/wdblack/dev/projects/hdim-master/libs/shared/notifications/project.json) uses the standard `@nx/eslint:lint` executor.
- Ran `npm run nx -- run shared-notifications:lint` and confirmed the target now passes cleanly under the standard Nx executor.
- Confirmed the custom-command regression is resolved and no broader lint-debt cleanup is required for this library as part of issue `011`.

**Learnings:**
- The executor regression and the warning backlog in other frontend projects are separate concerns; they should not share a tracker.
