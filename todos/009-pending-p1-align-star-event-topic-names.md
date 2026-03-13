---
status: pending
priority: p1
issue_id: "009"
tags: [backend, kafka, star-ratings, integration]
dependencies: []
---

# Align Star Event Topic Names

## Problem Statement

The new stars projection listener and the existing event publishing/configuration use inconsistent Kafka topic names. As written, the listener can miss the gap-close events it depends on, so persisted star projections may never update.

## Findings

- [`backend/modules/services/care-gap-event-service/src/main/java/com/healthdata/caregap/service/CareGapEventApplicationService.java`](/mnt/wdblack/dev/projects/hdim-master/backend/modules/services/care-gap-event-service/src/main/java/com/healthdata/caregap/service/CareGapEventApplicationService.java) publishes close events to `gap.events`.
- [`backend/modules/services/care-gap-event-service/src/main/java/com/healthdata/caregap/listener/StarsGapEventListener.java`](/mnt/wdblack/dev/projects/hdim-master/backend/modules/services/care-gap-event-service/src/main/java/com/healthdata/caregap/listener/StarsGapEventListener.java) also listens on `gap.events`.
- [`backend/modules/services/care-gap-event-service/src/main/java/com/healthdata/caregap/config/KafkaConfig.java`](/mnt/wdblack/dev/projects/hdim-master/backend/modules/services/care-gap-event-service/src/main/java/com/healthdata/caregap/config/KafkaConfig.java) creates `caregap.events`, `gap.detected`, and `gap.closed`, but not `gap.events`.
- The service also contains a separate CQRS listener wired to `care-gap.closed`, which shows the event naming surface is already fragmented.

## Proposed Solutions

### Option 1

Standardize on a single close-event topic contract and update publisher, listener, and topic declarations together.

Pros:
- Removes ambiguity and makes the event path deployable.
- Easier to test end to end.

Cons:
- May require checking downstream consumers before renaming.

Effort: Small
Risk: Medium

### Option 2

Leave current names in place and add more topic beans/listeners for every variant.

Pros:
- Low migration friction in the short term.

Cons:
- Preserves an unclear contract.
- Increases operational/debugging cost.

Effort: Medium
Risk: High

## Recommended Action

Use Option 1. Pick the authoritative topic name for gap-close/star-update events, update all producers and consumers to it, and add an integration test that proves a published close event triggers a stars recalculation.

## Acceptance Criteria

- [ ] Kafka topic declarations include the actual topic used by the stars listener.
- [ ] Gap-close publishing and stars projection consumption use the same topic contract.
- [ ] An integration or listener test proves the event path works end to end.
- [ ] Documentation references the chosen topic name consistently.

## Work Log

### 2026-03-12 - Review Finding Captured

**By:** Codex

**Actions:**
- Compared topic names in Kafka config, event publishing, and the new stars listener.
- Verified a mismatch between declared topics and the topic used by the stars projection path.

**Learnings:**
- The current implementation depends on an undeclared topic name, which is a deployment-time integration risk.
