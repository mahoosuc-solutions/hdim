# Repository Hygiene Standard (Folder-Level)

This repository uses a clean-tree contract for both humans and AI agents.

## 1) Folder Classification

- `source`: hand-authored code and docs that are expected to be committed.
- `generated`: reproducible build artifacts that must not be committed.
- `runtime`: local temp/state files that must not be committed.
- `assets`: intentional binary/static assets that may be committed with explicit scope.

## 2) Forbidden Dirty Paths

The following paths are treated as generated output and must not be tracked or left dirty:

- `backend/modules/services/*/bin/main/**`
- `backend/modules/services/*/bin/test/**`
- `backend/modules/shared/*/bin/main/**`
- `backend/modules/shared/*/bin/test/**`
- `backend/modules/apps/*/bin/main/**`
- `backend/modules/apps/*/bin/test/**`
- `backend/platform/*/bin/main/**`
- `backend/platform/*/bin/test/**`
- `backend/tools/*/bin/main/**`
- `backend/tools/*/bin/test/**`

## 3) Agent Operating Contract

Every agent task must run this sequence:

1. Preflight:
   - `git status --short --branch`
   - `npm run hygiene:audit`
2. Work:
   - Make only task-scoped edits.
3. Postflight:
   - `npm run hygiene:audit`
   - `git status --short --branch`
   - Verify no forbidden dirty paths and no out-of-scope files.

## 4) Definition Of Done

A task is not done unless:

- Hygiene audit passes.
- No forbidden generated paths are dirty.
- No newly added forbidden generated paths are present.
- Diff scope matches the requested work.

## 5) Recovery Runbook (WSL/Crash)

1. Verify remotes and branch:
   - `git remote -v`
   - `git status --short --branch`
2. Audit hygiene:
   - `npm run hygiene:audit`
3. Clean generated dirtiness (safe defaults):
   - `npm run hygiene:clean`
4. Re-audit:
   - `npm run hygiene:audit`
5. Sync before new work:
   - `git pull --ff-only origin master`

## 6) Enforcement

- Local: `scripts/repo-hygiene-audit.sh` and `scripts/repo-hygiene-clean.sh`
- CI: `.github/workflows/repo-hygiene.yml`
