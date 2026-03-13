---
status: pending
priority: p2
issue_id: "010"
tags: [backend, api, star-ratings, trends]
dependencies: []
---

# Separate Star Trend Granularity

## Problem Statement

The new trend endpoint mixes weekly and monthly snapshots into one time series. Once both schedulers run, clients will receive duplicate dates with no way to request or distinguish a clean weekly versus monthly trend.

## Findings

- [`backend/modules/services/care-gap-event-service/src/main/java/com/healthdata/caregap/service/StarsProjectionService.java`](/mnt/wdblack/dev/projects/hdim-master/backend/modules/services/care-gap-event-service/src/main/java/com/healthdata/caregap/service/StarsProjectionService.java) writes both `WEEKLY` and `MONTHLY` snapshots.
- The same service reads trend data via `findByTenantIdAndSnapshotDateGreaterThanEqualOrderBySnapshotDateAsc(...)`, which filters only by tenant and date.
- The API in [`backend/modules/services/care-gap-event-service/src/main/java/com/healthdata/caregap/api/v1/controller/StarRatingController.java`](/mnt/wdblack/dev/projects/hdim-master/backend/modules/services/care-gap-event-service/src/main/java/com/healthdata/caregap/api/v1/controller/StarRatingController.java) exposes only a `weeks` parameter, so callers cannot request a specific granularity.

## Proposed Solutions

### Option 1

Add a granularity filter to the repository, service, and controller, defaulting to weekly.

Pros:
- Produces deterministic charts and reports.
- Keeps monthly snapshots usable without polluting weekly views.

Cons:
- Small API change.

Effort: Small
Risk: Low

### Option 2

Keep a single endpoint and deduplicate points in memory.

Pros:
- No API expansion.

Cons:
- Hides which data points were weekly versus monthly.
- Makes behavior surprising when both exist for the same date.

Effort: Small
Risk: Medium

## Recommended Action

Use Option 1. Treat granularity as part of the query contract and add coverage for mixed snapshot datasets.

## Acceptance Criteria

- [ ] Trend queries can request a specific snapshot granularity.
- [ ] Weekly and monthly snapshots no longer appear interleaved in the same default response.
- [ ] Tests cover a tenant with both weekly and monthly snapshots on overlapping dates.

## Work Log

### 2026-03-12 - Review Finding Captured

**By:** Codex

**Actions:**
- Reviewed snapshot creation and retrieval logic for the new stars trend feature.
- Verified writes include granularity while reads ignore it.

**Learnings:**
- The bug is in the query contract, not just the UI. The backend currently cannot return a clean single-granularity trend.
