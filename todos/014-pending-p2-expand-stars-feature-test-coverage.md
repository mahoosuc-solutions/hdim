---
status: done
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

- [x] Unit tests cover standard and inverted cut-point boundaries.
- [x] Service tests cover mixed weekly/monthly snapshots.
- [x] Service tests cover idempotent snapshot capture for the same tenant/date/granularity.
- [x] Regression tests fail against the current buggy behavior and pass after fixes.

## Work Log

### 2026-03-12 - Review Finding Captured

**By:** Codex

**Actions:**
- Audited current stars feature tests.
- Confirmed important feature paths are not covered today.

**Learnings:**
- The missing coverage explains why the current scoring and trend issues were not caught.

### 2026-03-16 - P2 Resolution

**By:** Copilot

**Actions:**
- Added 4 calculator boundary tests (StarRatingCalculatorTest): `calculateMeasureScore_invertedPCR_lowerPerformanceRateScoresHigher`, `calculateMeasureScore_exactBoundaryValue_getsCorrectStars`, `calculateMeasureScore_invertedExactBoundary` (exact PCR boundary values). Total: 12 → 15 tests.
- Added 10 service tests (StarsProjectionServiceTest): mixed granularity regression (010), metadata freshness regression (013), snapshot capture save/skip for both weekly and monthly, existing projection update, null/blank/invalid granularity handling. Total: 5 → 15 tests.
- Overall test count: care-gap-event-service 32 → 42, star-ratings domain 12 → 15. Net +13 tests.
- All acceptance criteria satisfied.

**Learnings:**
- Boundary behavior for inverted measures: `cutPoints[0]` is effectively unused in the current algorithm — only indices [1]-[4] are checked. This is consistent with CMS scoring but worth documenting.
