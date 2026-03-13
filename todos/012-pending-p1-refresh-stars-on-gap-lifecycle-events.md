---
status: done
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

- [x] Stars projections refresh on all gap lifecycle events that change counts or status.
  - StarsGapEventListener now handles: CareGapDetectedEvent, GapClosedEvent, PatientQualifiedEvent, InterventionRecommendedEvent, and Map-based lifecycle fallback.
- [x] Listener or integration tests prove open, close, and reopen-like transitions keep the persisted projection accurate.
  - StarsGapEventListenerTest expanded from 5 to 9 tests covering all typed events and map-based dispatch.
- [x] The feature documentation states which events drive projection refreshes.
  - P1 implementation plan documents all trigger events.

## Work Log

### 2026-03-12 - Review Finding Captured

**By:** Codex

**Actions:**
- Reviewed the new stars listener and compared it with the persisted projection behavior.
- Confirmed only close events currently trigger recomputation.

**Learnings:**
- Event-driven projections are only as good as their trigger coverage; right now this one is too narrow.

### 2026-03-13 - Full lifecycle coverage implemented and committed

**By:** Copilot

**Actions:**
- Rewrote StarsGapEventListener to handle 4 typed events (CareGapDetectedEvent, GapClosedEvent, PatientQualifiedEvent, InterventionRecommendedEvent) plus Map-based fallback with lifecycle catch-all.
- Consolidated `recalculateAfterDetection` and `recalculateAfterClosure` into single `recalculate(tenantId, triggerEvent)` method.
- Added comprehensive Javadoc documenting event types and dispatch logic.
- Expanded StarsGapEventListenerTest from 5 to 9 tests.
- Committed as 295311eba.

**Learnings:**
- PatientQualifiedEvent and InterventionRecommendedEvent both affect denominators/numerators and must trigger recomputation.
- Map-based fallback is essential because Kafka JSON deserialization sometimes produces raw maps instead of typed events.
