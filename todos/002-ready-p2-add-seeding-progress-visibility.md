---
status: done
priority: p2
issue_id: "002"
tags: [seeding, observability, ui, ops]
dependencies: []
---

# Add Seeding Progress Visibility

## Problem Statement
Operators need clear progress and last-error visibility during seeding to know when it is safe to proceed or retry.

## Findings
- Deployment console currently shows tail lines and status, but does not expose structured progress.
- Ops service captures status and logs but does not present a progress summary.

## Proposed Solutions
- Option A: Extend ops-service `/ops/status` to include counts + phase.
- Option B: Add a dedicated `/ops/seeding-progress` endpoint.
- Option C: Poll seeding service actuator metrics and display in UI.

## Recommended Action
Extend ops-service `/ops/status` to include a summarized seeding progress block (phase, counts, last error). Update the deployment console to render this block.

## Acceptance Criteria
- [x] Progress percent and phase visible in UI
- [x] Last error surfaced in UI
- [x] Works without manual log digging

## Work Log

### 2026-02-11 - Created

**By:** Codex

**Actions:**
- Logged need for structured seeding progress

**Learnings:**
- TBD

### 2026-03-15 - Verified already implemented — closing

**By:** Copilot

**Actions:**
- Verified ops-server `tools/ops-server/server.js` already returns structured `seedingProgress` in `GET /ops/status` (phase, percent, counts, lastError, updatedAt)
- Verified `parseSeedingProgress()` extracts phase transitions (idle → waiting-service → seeding → syncing-cql → completed/failed) with regex pattern matching on docker log lines
- Verified deployment console (`apps/mfe-deployment/src/app/deployment-console.component.html`) renders all three acceptance criteria:
  - Progress bar with `[style.width.%]` bound to percent, phase label via `formatPhase()`
  - Patient counts (created/loaded) displayed when present
  - `lastError` surfaced with CSS class `progress-error`
- Verified polling runs automatically every 10s — no manual log digging required
- E2E spec exists at `apps/shell-app-e2e/src/deployment-console.e2e.spec.ts` validating the `/ops/status` contract

**Learnings:**
- Implementation predates the todo — ops-server and deployment console were built with full seeding progress support from the start
