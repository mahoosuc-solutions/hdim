---
status: pending
priority: p2
issue_id: "014"
tags: [backend, star-ratings, tests]
dependencies: []
---

# Expand Stars Feature Test Coverage

## Problem Statement

The new stars feature ships with only two narrow happy-path service tests. Key behavior around scoring boundaries, trend retrieval, and snapshot scheduling is currently untested.

## Findings

- [`backend/modules/services/care-gap-event-service/src/test/java/com/healthdata/caregap/service/StarsProjectionServiceTest.java`](/mnt/wdblack/dev/projects/hdim-master/backend/modules/services/care-gap-event-service/src/test/java/com/healthdata/caregap/service/StarsProjectionServiceTest.java) covers only basic recalc and simulate paths.
- There is no direct calculator coverage for cut-point boundaries in [`backend/modules/shared/domain/star-ratings/src/main/java/com/healthdata/starrating/service/StarRatingCalculator.java`](/mnt/wdblack/dev/projects/hdim-master/backend/modules/shared/domain/star-ratings/src/main/java/com/healthdata/starrating/service/StarRatingCalculator.java).
- There is no test coverage for mixed weekly/monthly snapshot retrieval or scheduled snapshot idempotency in [`backend/modules/services/care-gap-event-service/src/main/java/com/healthdata/caregap/service/StarsProjectionService.java`](/mnt/wdblack/dev/projects/hdim-master/backend/modules/services/care-gap-event-service/src/main/java/com/healthdata/caregap/service/StarsProjectionService.java).

## Proposed Solutions

### Option 1

Add focused unit tests for the calculator plus service tests for trend and snapshot behavior.

Pros:
- Guards the feature at the right abstraction levels.
- Prevents the current defects from reappearing.

Cons:
- Requires a modest increase in test scaffolding.

Effort: Medium
Risk: Low

### Option 2

Rely on broader integration tests later.

Pros:
- Less immediate work.

Cons:
- Slower feedback.
- Easier for subtle scoring/query bugs to slip through again.

Effort: Small now, larger later
Risk: Medium

## Recommended Action

Use Option 1. Add direct coverage for scoring boundaries and service-level coverage for trend/snapshot behavior before broadening the feature further.

## Acceptance Criteria

- [ ] Unit tests cover standard and inverted cut-point boundaries.
- [ ] Service tests cover mixed weekly/monthly snapshots.
- [ ] Service tests cover idempotent snapshot capture for the same tenant/date/granularity.
- [ ] Regression tests fail against the current buggy behavior and pass after fixes.

## Work Log

### 2026-03-12 - Review Finding Captured

**By:** Codex

**Actions:**
- Audited current stars feature tests.
- Confirmed important feature paths are not covered today.

**Learnings:**
- The missing coverage explains why the current scoring and trend issues were not caught.
