---
status: pending
priority: p1
issue_id: "008"
tags: [backend, star-ratings, care-gap-event-service, api, scheduling]
dependencies: []
---

# Fix star-ratings projection correctness

## Problem Statement

The new star-ratings feature in `care-gap-event-service` initially had multiple correctness issues across scoring, event-driven recomputation, and trend retrieval. Most of the core implementation defects are now fixed, but the final container-backed integration proof is still open because the local Docker/Testcontainers environment is unstable.

## Findings

- `StarRatingCalculator.calculateStarsForMeasure(...)` was fixed to use the full cutpoint ladder for both normal and inverted measures.
- `StarsGapEventListener` was expanded to handle both detection and closure events, and also tolerate generic JSON-map payloads from Kafka conversion.
- `StarsProjectionService.getTrend(...)` now filters by explicit granularity instead of mixing weekly and monthly snapshots.
- `StarsProjectionService.getCurrentRating(...)` and `simulate(...)` now return fresh metadata that matches the values in the payload.
- The care-gap detect flow had a separate bug where `CareGapDetectedEvent` arguments were passed in the wrong order, which meant Stars recomputation was using the wrong tenant context.
- The care-gap event test profile was incorrectly using `StringSerializer`/`StringDeserializer` for Kafka values; that was fixed to use JSON serialization.
- The event DTOs now have Jackson constructors so Kafka JSON deserialization can rebuild them.
- The remaining open issue is validation depth, not a known unresolved compile/runtime defect: the dedicated `StarsProjectionIntegrationTest` still needs one clean green run in a working Docker/Testcontainers environment.

## Proposed Solutions

### Option 1

Finish the current implementation stabilization:
- keep the corrected scoring, projection refresh, metadata, and granularity changes
- keep the direct projection persistence/recalculation path that now backs the feature
- complete the missing Docker-backed integration validation
- close the issue once the end-to-end proof passes

Pros:
- Preserves the current API and event contract
- Builds on the fixes already landed
- Limits further churn to validation and cleanup

Cons:
- Still depends on local/container test stability for the final proof

### Option 2

Do a larger architectural rewrite:
- reintroduce a separate projection-updater path for care-gap rows
- decouple synchronous projection writes from asynchronous Kafka updates
- redesign the feature contract around more explicit event typing and projection ownership

Pros:
- Cleaner long-term separation of concerns
- Easier to evolve if the feature expands substantially

Cons:
- Not justified by the current remaining issue
- Adds coordination and regression risk after the correctness fixes are already in place

## Recommended Action

Continue with Option 1. Treat this todo as the single tracker for the stars correctness bundle. The remaining work is to get one passing container-backed integration run and then close the issue if no further runtime defects appear.

## Acceptance Criteria

- [x] Normal and inverted measure cut-point tests cover all star bands and pass.
- [x] Persisted current projections refresh when gap state changes would alter counts or scores.
- [x] Trend responses can distinguish or filter weekly vs monthly snapshots.
- [x] `getCurrentRating` response metadata matches the calculation returned to the client.
- [x] Targeted unit tests cover calculator boundaries, trend retrieval, listener dispatch, and scheduled snapshot behavior.
- [ ] `StarsProjectionIntegrationTest` passes in a working Docker/Testcontainers environment.

## Work Log

### 2026-03-12 - Review findings captured

**By:** Codex

**Actions:**
- Reviewed the new star-ratings controller, service, listener, repositories, entities, migrations, shared domain calculator, and unit tests.
- Ran `./gradlew :modules:services:care-gap-event-service:test` from `/mnt/wdblack/dev/projects/hdim-master/backend`.
- Confirmed the service test suite passes while the feature still has uncovered correctness gaps.

**Learnings:**
- The highest-risk issues are semantic rather than compile-time: wrong cut-point logic, stale projection refresh behavior, and ambiguous trend aggregation.

### 2026-03-12 - Consolidated duplicate issue 008

**By:** Codex

**Actions:**
- Consolidated the narrower cutpoint-only tracker into this broader stars correctness todo.
- Updated this file to reflect the fixes already implemented across scoring, Kafka serialization, event handling, metadata freshness, and trend granularity.
- Narrowed the remaining scope to the single unresolved validation item: one green Docker-backed integration run.

**Learnings:**
- The duplicate `008` files were describing the same workstream at different levels of scope.
- The broad tracker is the right source of truth because the cutpoint fix was only one defect inside the larger projection-correctness bundle.
