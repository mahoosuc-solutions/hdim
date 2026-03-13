---
status: pending
priority: p2
issue_id: "013"
tags: [backend, star-ratings, api]
dependencies: []
---

# Fix Stars Current Metadata Freshness

## Problem Statement

The `/api/v1/star-ratings/current` endpoint recomputes score data on demand but returns `lastTriggerEvent` and `calculatedAt` from the previously persisted projection. That can produce a payload with fresh scores but stale or null metadata.

## Findings

- [`backend/modules/services/care-gap-event-service/src/main/java/com/healthdata/caregap/service/StarsProjectionService.java`](/mnt/wdblack/dev/projects/hdim-master/backend/modules/services/care-gap-event-service/src/main/java/com/healthdata/caregap/service/StarsProjectionService.java) recomputes the projection in `getCurrentRating(...)`.
- The response assembly in the same file still reads `lastTriggerEvent` and `calculatedAt` from the persisted `StarRatingProjection`.
- For tenants without a persisted row, the endpoint can return `null` metadata even though it just produced a current score.

## Proposed Solutions

### Option 1

Return metadata that reflects the current computation, even for on-demand reads.

Pros:
- Makes the response internally consistent.
- Easier for clients and operators to reason about.

Cons:
- Requires deciding whether on-demand reads should update or merely report metadata.

Effort: Small
Risk: Low

### Option 2

Remove these metadata fields from on-demand responses.

Pros:
- Avoids misleading data.

Cons:
- Loses useful operational context.

Effort: Small
Risk: Low

## Recommended Action

Use Option 1. Define clear semantics for `calculatedAt` and `lastTriggerEvent` on on-demand reads and implement them consistently.

## Acceptance Criteria

- [ ] `/current` no longer returns stale metadata alongside fresh scores.
- [ ] First-time tenants receive sensible metadata rather than nulls for a fresh computation.
- [ ] Tests cover both persisted and never-persisted tenants.

## Work Log

### 2026-03-12 - Review Finding Captured

**By:** Codex

**Actions:**
- Reviewed the `/current` response path and compared computed values with the metadata source.
- Confirmed the endpoint can combine fresh numbers with stale persisted timestamps/events.

**Learnings:**
- The bug is semantic rather than computational, but it still makes the API misleading.
