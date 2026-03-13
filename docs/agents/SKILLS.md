# Agent Skills Standard

This file defines required operational capabilities for user-level AI agents in this repository.

## Core Capability Set

1. Repository hygiene gatekeeping
- Run `git status --short --branch` and `npm run hygiene:audit` before and after work.
- Fail fast when forbidden generated paths are dirty or newly added.

2. Scoped change discipline
- Modify only task-owned files.
- Report any unrelated dirty paths before proceeding with risky actions.

3. Recovery readiness
- Use the WSL/crash recovery sequence in `HYGIENE.md`.
- Re-establish clean-tree baseline before feature development.

4. Build artifact containment
- Treat `backend/**/bin/**` paths as generated, non-authoritative outputs.
- Never commit generated runtime artifacts unless explicitly requested.

## Mandatory Runbook Hooks

For every implementation task, include these steps:

- Preflight:
  - `git status --short --branch`
  - `npm run hygiene:audit`
- Postflight:
  - `npm run hygiene:audit`
  - `git status --short --branch`

If hygiene fails:

- Run `npm run hygiene:clean`
- Re-run `npm run hygiene:audit`
- If new forbidden files were staged, unstage/remove them and re-run audit.

## Definition Of Done Addendum

A task is complete only when:

- No forbidden generated path is dirty.
- No newly added forbidden generated path is present.
- Final diff matches the user-requested scope.

## P1 Cluster Resolution Template

When resolving a P1 feature cluster, follow this systematic process:

1. **Audit**: Run a deep code audit identifying all bugs, gaps, and risks. Produce a structured findings report.
2. **Plan**: Produce an implementation plan with user types, epics, stories, acceptance criteria, and ordered tasks.
3. **Implement**: Fix bugs in priority order (correctness → security → observability). Expand test coverage proportionally.
4. **Validate**: Compile both source and test code. Run affected test suites. Verify 0 failures.
5. **Commit**: Stage modified files. Write a detailed conventional commit message referencing todo IDs.
6. **Close todos**: Update todo files (status: done, acceptance criteria checked, work log entry).
7. **Postflight**: `npm run hygiene:audit` + `git status --short --branch` confirms clean tree.

### Completed P1 Clusters

| Cluster | Todos | Commit | Status |
|---------|-------|--------|--------|
| Star Ratings | 008, 009, 012 | 802640e9e | ✅ Done |
| Auth/Tenant Controls | 004 | — | Pending |
| Seeding Verification | 001 | — | Pending |
