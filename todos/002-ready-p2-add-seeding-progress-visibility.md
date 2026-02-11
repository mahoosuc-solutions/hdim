---
status: ready
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
- [ ] Progress percent and phase visible in UI
- [ ] Last error surfaced in UI
- [ ] Works without manual log digging

## Work Log

### 2026-02-11 - Created

**By:** Codex

**Actions:**
- Logged need for structured seeding progress

**Learnings:**
- TBD
