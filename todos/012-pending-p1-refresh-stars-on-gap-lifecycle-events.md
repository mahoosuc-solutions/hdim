---
status: pending
priority: p1
issue_id: "012"
tags: [backend, star-ratings, events, projections]
dependencies: []
---

# Refresh Stars On Gap Lifecycle Events

## Problem Statement

The new persisted stars projection only recomputes on `GapClosedEvent`. New gaps, reopened gaps, or other lifecycle changes that affect denominators and open-gap counts can leave the persisted projection stale.

## Findings

- [`backend/modules/services/care-gap-event-service/src/main/java/com/healthdata/caregap/listener/StarsGapEventListener.java`](/mnt/wdblack/dev/projects/hdim-master/backend/modules/services/care-gap-event-service/src/main/java/com/healthdata/caregap/listener/StarsGapEventListener.java) ignores all payloads except `GapClosedEvent`.
- The stars projection feature persists a “current” summary in [`backend/modules/services/care-gap-event-service/src/main/java/com/healthdata/caregap/service/StarsProjectionService.java`](/mnt/wdblack/dev/projects/hdim-master/backend/modules/services/care-gap-event-service/src/main/java/com/healthdata/caregap/service/StarsProjectionService.java), so other event types can leave the stored view behind reality.
- This affects any consumer that relies on the persisted projection rather than on-demand recomputation.

## Proposed Solutions

### Option 1

Recalculate projections on every gap lifecycle event that changes numerators/denominators or status.

Pros:
- Keeps the persisted projection honest.
- Matches the event-driven projection model.

Cons:
- Requires clarifying which event types materially change the stars view.

Effort: Small
Risk: Medium

### Option 2

Stop persisting the “current” projection and compute everything on demand.

Pros:
- Avoids stale event-driven state.

Cons:
- Changes the feature design and may increase read cost.

Effort: Medium
Risk: Medium

## Recommended Action

Use Option 1. Define the gap lifecycle events that should trigger a stars refresh and cover them with listener/integration tests.

## Acceptance Criteria

- [ ] Stars projections refresh on all gap lifecycle events that change counts or status.
- [ ] Listener or integration tests prove open, close, and reopen-like transitions keep the persisted projection accurate.
- [ ] The feature documentation states which events drive projection refreshes.

## Work Log

### 2026-03-12 - Review Finding Captured

**By:** Codex

**Actions:**
- Reviewed the new stars listener and compared it with the persisted projection behavior.
- Confirmed only close events currently trigger recomputation.

**Learnings:**
- Event-driven projections are only as good as their trigger coverage; right now this one is too narrow.
