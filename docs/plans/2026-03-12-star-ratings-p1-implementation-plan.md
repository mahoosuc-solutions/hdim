# Star Ratings P1 Cluster — Detailed Implementation Plan

**Todos:** 008 (Projection Correctness), 009 (Topic Alignment), 012 (Lifecycle Event Refresh)  
**Date:** March 12, 2026  
**Author:** Lead Architect  
**Status:** APPROVED — Ready for implementation

---

## Table of Contents

1. [User Types & Journeys](#1-user-types--journeys)
2. [User Stories (Epics → Stories)](#2-user-stories-epics--stories)
3. [Implementation Plan (Ordered Tasks)](#3-implementation-plan-ordered-tasks)
4. [Success Criteria](#4-success-criteria)
5. [Risk Analysis](#5-risk-analysis)

---

## 1. User Types & Journeys

### 1.1 Quality Director

| Attribute | Detail |
|-----------|--------|
| **Primary goal** | See the overall CMS Star Rating and trend to steer the quality program |
| **Journey** | Login → Dashboard → `GET /api/v1/star-ratings/current` → review domain/measure breakdown → `GET /api/v1/star-ratings/trend?weeks=12` → compare monthly trajectory |
| **Data needed** | Overall rounded rating, domain stars, quality-bonus eligibility, measure-level performance rates, weekly/monthly trend |
| **Frequency** | Daily for current, weekly for trend review |
| **Acceptance criteria** | (1) Current rating reflects ALL lifecycle events processed within the last 60 s. (2) Trend returns ONLY the requested granularity (no mixed weekly/monthly). (3) Rating recalculates when a gap is detected, closed, reopened, qualified, or receives an intervention. |

### 1.2 Care Manager

| Attribute | Detail |
|-----------|--------|
| **Primary goal** | Understand which gap closures would move the Star Rating the most |
| **Journey** | Login → Star Ratings → `POST /api/v1/star-ratings/simulate` with candidate closures → compare simulated vs current → prioritize outreach |
| **Data needed** | Simulated rating, per-measure numerator/denominator deltas, domain impact |
| **Frequency** | Multiple times per day during outreach cycles |
| **Acceptance criteria** | (1) Simulation uses the latest gap state (not stale projection). (2) Simulation does NOT persist. (3) Detect, close, reopen, qualify, and intervention events are all reflected in the base gap state the simulation reads. |

### 1.3 Quality Analyst

| Attribute | Detail |
|-----------|--------|
| **Primary goal** | Audit historical Star Rating snapshots for accuracy and compliance reporting |
| **Journey** | Login → Reports → `GET /api/v1/star-ratings/trend?weeks=52&granularity=MONTHLY` → export → validate against CMS submission |
| **Data needed** | Historical snapshots at weekly and monthly granularity, gap counts at each snapshot point |
| **Frequency** | Weekly or on-demand |
| **Acceptance criteria** | (1) Snapshots are not duplicated for the same tenant+date+granularity. (2) Monthly and weekly snapshots are stored independently. (3) Snapshots reflect the correct gap state at capture time. |

### 1.4 System Administrator

| Attribute | Detail |
|-----------|--------|
| **Primary goal** | Ensure Kafka topics, consumers, and event flows are healthy |
| **Journey** | Kafka console → verify `gap.events` consumer group `care-gap-stars-projection` is caught up → verify no orphan consumers on unused topics |
| **Data needed** | Consumer lag, topic health, audit trail of trigger events |
| **Frequency** | On-demand (alert-driven) |
| **Acceptance criteria** | (1) Only one canonical topic (`gap.events`) carries all gap lifecycle events. (2) Orphan topic declarations (`gap.detected`, `gap.closed`) are removed. (3) The CQRS `CareGapEventListener` on `care-gap.identified`/`care-gap.closed` is out of scope for this cluster but is documented as a separate event namespace. |

---

## 2. User Stories (Epics → Stories)

### Epic 1: Topic Alignment (Todo 009)

> **Goal:** Establish `gap.events` as the single authoritative Kafka topic for all gap lifecycle events consumed by the Stars projection subsystem, and remove orphan topic declarations.

#### Story 1.1 — Remove orphan topic beans

**As a** system administrator,  
**I want** the orphan `gap.detected` and `gap.closed` topic beans removed from `KafkaConfig`,  
**So that** Kafka does not create unused topics that confuse operators.

**Acceptance Criteria:**
- `KafkaConfig.gapDetectedTopic()` bean is removed.
- `KafkaConfig.gapClosedTopic()` bean is removed.
- The `gap.events` topic bean (`careGapEventsTopic()`) remains.
- `intervention.recommended` topic bean remains (used by a separate flow).
- The service starts without errors.
- No existing producer or consumer references `gap.detected` or `gap.closed` string literals.

**Priority:** Must-have (P1)

#### Story 1.2 — Eliminate double recalculation

**As a** Quality Director,  
**I want** the Stars projection to recalculate exactly once per gap event,  
**So that** the projection is consistent and the system does not waste resources.

**Acceptance Criteria:**
- `CareGapEventApplicationService.detectGap()` no longer calls `starsProjectionService.recalculateCurrentProjection()` synchronously.
- `CareGapEventApplicationService.closeGap()` (both overloads) no longer calls `starsProjectionService.recalculateCurrentProjection()` synchronously.
- `CareGapEventApplicationService.qualifyPatient()` does NOT call recalculate (deferred to Story 3.2).
- `CareGapEventApplicationService.recommendIntervention()` does NOT call recalculate (deferred to Story 3.3).
- Instead, all recalculation happens asynchronously via `StarsGapEventListener` consuming from `gap.events`.
- Unit test `CareGapEventApplicationServiceTest` is updated to remove `verify` on `starsProjectionService`.
- Listener test proves each event type triggers exactly one recalculation.

**Priority:** Must-have (P1)

#### Story 1.3 — Integration test for event-driven recalculation

**As a** developer,  
**I want** an integration test proving that a `CareGapDetectedEvent` published to `gap.events` triggers a Stars recalculation,  
**So that** the Kafka wiring is validated end-to-end.

**Acceptance Criteria:**
- A `@SpringBootTest` + `@EmbeddedKafka` test publishes a `CareGapDetectedEvent` to `gap.events`.
- The test asserts that `StarRatingProjection` for the tenant is created/updated within 10 s.
- The test also verifies a `GapClosedEvent` triggers recalculation.
- Uses `TestEventWaiter` for event-driven synchronization (no `Thread.sleep`).

**Priority:** Must-have (P1)

---

### Epic 2: Projection Correctness (Todo 008)

> **Goal:** Ensure the persisted `StarRatingProjection` and on-demand responses always reflect the true gap state, and the Liquibase migrations are rollback-safe.

#### Story 2.1 — Add `@Version` for optimistic locking

**As a** developer,  
**I want** the `StarRatingProjection.version` field to use JPA `@Version` for optimistic locking,  
**So that** concurrent recalculations do not silently overwrite each other.

**Acceptance Criteria:**
- `StarRatingProjection.version` is annotated with `@jakarta.persistence.Version`.
- Manual `projection.setVersion(projection.getVersion() + 1)` in `StarsProjectionService.recalculateCurrentProjection()` is removed (JPA manages it).
- Unit test verifies that calling `recalculateCurrentProjection()` no longer manually increments version on the captured entity.

**Priority:** Must-have (P1)

#### Story 2.2 — Add `@Audited` annotations to controller

**As a** compliance officer,  
**I want** all Star Rating controller endpoints to have `@Audited` annotations,  
**So that** HIPAA audit trails are generated for every access.

**Acceptance Criteria:**
- `StarRatingController.getCurrent()` has `@Audited(eventType = "STAR_RATING_READ")`.
- `StarRatingController.getTrend()` has `@Audited(eventType = "STAR_RATING_TREND_READ")`.
- `StarRatingController.simulate()` has `@Audited(eventType = "STAR_RATING_SIMULATE")`.

**Priority:** Must-have (P1)

#### Story 2.3 — Fix `@PreAuthorize` to use standard role-based pattern

**As a** developer,  
**I want** the Star Rating controller to use `hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST')` instead of `hasPermission('CARE_GAP_READ')`,  
**So that** it follows the project-wide RBAC pattern.

**Acceptance Criteria:**
- All three endpoints in `StarRatingController` use `@PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST')")`.
- The `hasPermission('CARE_GAP_READ')` expression is removed.

**Priority:** Must-have (P1)

#### Story 2.4 — Add Liquibase rollback directives

**As a** database administrator,  
**I want** both star-rating migration files to have `<rollback>` blocks,  
**So that** schema changes can be safely reverted.

**Acceptance Criteria:**
- `0004-create-star-rating-projections.xml` has `<rollback><dropTable tableName="star_rating_projections"/></rollback>`.
- `0005-create-star-rating-snapshots.xml` has rollback that drops the unique constraint, index, and table in the correct order.
- Rollback coverage audit passes: `./gradlew test --tests "*EntityMigrationValidationTest"`.

**Priority:** Must-have (P1)

#### Story 2.5 — Metadata consistency on `getCurrentRating` and `simulate`

**As a** Quality Director,  
**I want** the `calculatedAt` and `lastTriggerEvent` fields in API responses to reflect the actual computation,  
**So that** I know the data freshness.

**Acceptance Criteria:**
- `getCurrentRating()` returns `lastTriggerEvent = "on-demand-read"` and a freshly computed `calculatedAt` (already implemented — verify test coverage).
- `simulate()` returns `lastTriggerEvent = "simulation"` and a freshly computed `calculatedAt` (already implemented — verify test coverage).
- Existing tests in `StarsProjectionServiceTest` already validate this. No code change needed — mark as verified.

**Priority:** Verified (no-op)

---

### Epic 3: Lifecycle Event Refresh (Todo 012)

> **Goal:** Make `StarsGapEventListener` handle all gap lifecycle events that materially change the Star Rating (detect, close, qualify, reopen via re-detect, intervention).

#### Story 3.1 — Handle `PatientQualifiedEvent` in listener

**As a** Quality Director,  
**I want** the Stars projection to refresh when a patient is qualified for a gap,  
**So that** the denominator changes are reflected immediately.

**Acceptance Criteria:**
- `StarsGapEventListener.onGapEvent()` recognizes `PatientQualifiedEvent` and calls `recalculateCurrentProjection(tenantId, "gap.qualified:" + gapCode)`.
- Map-based payload matching checks for `qualified` key.
- Unit test with `PatientQualifiedEvent` verifies recalculation.
- Unit test with `Map` payload containing `"qualified"` key verifies recalculation.

**Priority:** Must-have (P1)

#### Story 3.2 — Handle `InterventionRecommendedEvent` in listener

**As a** Care Manager,  
**I want** the Stars projection to refresh when an intervention is recommended,  
**So that** intervention-driven status changes are reflected in the rating.

**Acceptance Criteria:**
- `StarsGapEventListener.onGapEvent()` recognizes `InterventionRecommendedEvent` and calls `recalculateCurrentProjection(tenantId, "gap.intervention:" + gapCode)`.
- Map-based payload matching checks for `intervention` key.
- Unit test with `InterventionRecommendedEvent` verifies recalculation.
- Unit test with `Map` payload containing `"intervention"` key verifies recalculation.

**Priority:** Must-have (P1)

#### Story 3.3 — Handle re-detection (reopen) events in listener

**As a** Quality Director,  
**I want** the Stars projection to refresh when a previously closed gap is re-detected (reopened),  
**So that** the numerator/denominator changes are reflected correctly.

**Acceptance Criteria:**
- Re-detection is already handled by the `CareGapDetectedEvent` path (a detect on an existing closed gap effectively reopens it).
- The existing `CareGapDetectedEvent` handler in the listener already triggers `recalculateAfterDetection()`.
- Verify with a test that detects a gap, closes it, then detects again — the projection's openGapCount and closedGapCount reflect the reopen.

**Priority:** Must-have (P1) — test-only story

#### Story 3.4 — Add `@JsonCreator` to `PatientQualifiedEvent` and `InterventionRecommendedEvent`

**As a** developer,  
**I want** all event DTOs consumed from Kafka to have Jackson `@JsonCreator` constructors,  
**So that** JSON deserialization works correctly.

**Acceptance Criteria:**
- `PatientQualifiedEvent` has a `@JsonCreator` constructor matching `CareGapDetectedEvent`'s pattern.
- `InterventionRecommendedEvent` has a `@JsonCreator` constructor matching `CareGapDetectedEvent`'s pattern.
- Both can be deserialized from a JSON string in a unit test.

**Priority:** Must-have (P1)

---

## 3. Implementation Plan (Ordered Tasks)

> **Principle:** Each task can be implemented, tested, and committed independently. Tasks are ordered by dependency.

---

### Task 1: Add `@JsonCreator` to `PatientQualifiedEvent` and `InterventionRecommendedEvent`

**Stories:** 3.4  
**Depends on:** None  
**Estimated LOC changed:** ~40

**Files to modify:**

#### 1a. `backend/modules/services/care-gap-event-handler-service/src/main/java/com/healthdata/caregap/event/PatientQualifiedEvent.java`

**Current:**
```java
public PatientQualifiedEvent(String tenantId, String patientId, String gapCode, boolean qualified, String reason) {
    this.tenantId = tenantId;
    this.patientId = patientId;
    this.gapCode = gapCode;
    this.qualified = qualified;
    this.reason = reason;
    this.timestamp = Instant.now();
}
```

**Change to:**
```java
public PatientQualifiedEvent(String tenantId, String patientId, String gapCode, boolean qualified, String reason) {
    this(tenantId, patientId, gapCode, qualified, reason, Instant.now());
}

@JsonCreator
public PatientQualifiedEvent(
    @JsonProperty("tenantId") String tenantId,
    @JsonProperty("patientId") String patientId,
    @JsonProperty("gapCode") String gapCode,
    @JsonProperty("qualified") boolean qualified,
    @JsonProperty("reason") String reason,
    @JsonProperty("timestamp") Instant timestamp
) {
    this.tenantId = tenantId;
    this.patientId = patientId;
    this.gapCode = gapCode;
    this.qualified = qualified;
    this.reason = reason;
    this.timestamp = timestamp == null ? Instant.now() : timestamp;
}
```

Add imports:
```java
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
```

**Why:** The Kafka JSON deserializer needs `@JsonCreator` to reconstruct events from JSON. Without it, `PatientQualifiedEvent` payloads fail deserialization and are silently dropped.

#### 1b. `backend/modules/services/care-gap-event-handler-service/src/main/java/com/healthdata/caregap/event/InterventionRecommendedEvent.java`

**Current:**
```java
public InterventionRecommendedEvent(String tenantId, String patientId, String gapCode, String intervention, String priority) {
    this.tenantId = tenantId;
    this.patientId = patientId;
    this.gapCode = gapCode;
    this.intervention = intervention;
    this.priority = priority;
    this.timestamp = Instant.now();
}
```

**Change to:**
```java
public InterventionRecommendedEvent(String tenantId, String patientId, String gapCode, String intervention, String priority) {
    this(tenantId, patientId, gapCode, intervention, priority, Instant.now());
}

@JsonCreator
public InterventionRecommendedEvent(
    @JsonProperty("tenantId") String tenantId,
    @JsonProperty("patientId") String patientId,
    @JsonProperty("gapCode") String gapCode,
    @JsonProperty("intervention") String intervention,
    @JsonProperty("priority") String priority,
    @JsonProperty("timestamp") Instant timestamp
) {
    this.tenantId = tenantId;
    this.patientId = patientId;
    this.gapCode = gapCode;
    this.intervention = intervention;
    this.priority = priority;
    this.timestamp = timestamp == null ? Instant.now() : timestamp;
}
```

Add imports:
```java
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
```

**Why:** Same as 1a — enables Kafka JSON deserialization.

---

### Task 2: Remove orphan Kafka topic beans

**Stories:** 1.1  
**Depends on:** None  
**Estimated LOC changed:** ~20 (deletions)

**File:** `backend/modules/services/care-gap-event-service/src/main/java/com/healthdata/caregap/config/KafkaConfig.java`

**Remove** the `gapDetectedTopic()` and `gapClosedTopic()` bean methods (lines ~33–54 of the current file). Keep `careGapEventsTopic()` and `interventionRecommendedTopic()`.

**Resulting file:**
```java
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic careGapEventsTopic() {
        return TopicBuilder.name("gap.events")
            .partitions(3)
            .replicas(1)
            .config("retention.ms", "86400000")
            .build();
    }

    @Bean
    public NewTopic interventionRecommendedTopic() {
        return TopicBuilder.name("intervention.recommended")
            .partitions(1)
            .replicas(1)
            .config("retention.ms", "604800000")
            .build();
    }
}
```

**Why:** `gap.detected` and `gap.closed` are never produced to or consumed from. They were declared in anticipation of a fine-grained topic design that was superseded by the unified `gap.events` topic. Keeping them creates Kafka topics that confuse operators and add noise to monitoring.

**Verification:** `grep -r "gap\.detected\|gap\.closed" backend/modules/services/care-gap-event-service/src/main/` should return zero results after this change.

---

### Task 3: Eliminate double recalculation in `CareGapEventApplicationService`

**Stories:** 1.2  
**Depends on:** Task 2 (topic alignment clarity, not a code dependency)  
**Estimated LOC changed:** ~15 (deletions)

**File:** `backend/modules/services/care-gap-event-service/src/main/java/com/healthdata/caregap/service/CareGapEventApplicationService.java`

**Changes (4 call sites to remove):**

1. **`detectGap()`** — Remove line:
   ```java
   starsProjectionService.recalculateCurrentProjection(tenantId, "gap.detected:" + request.getGapCode());
   ```

2. **`closeGap(String gapId, String tenantId)`** — Remove line:
   ```java
   starsProjectionService.recalculateCurrentProjection(tenantId, "gap.closed:" + projection.getGapCode());
   ```

3. **`closeGap(CloseGapRequest request, String tenantId)`** — Remove line:
   ```java
   starsProjectionService.recalculateCurrentProjection(tenantId, "gap.closed:" + request.getGapCode());
   ```

4. **Remove the `StarsProjectionService` field** if no other method references it (check: `qualifyPatient` and `recommendIntervention` don't call it). Remove:
   ```java
   private final StarsProjectionService starsProjectionService;
   ```
   and update the constructor (Lombok `@RequiredArgsConstructor` handles this automatically — just remove the field).

**Why:** The synchronous call duplicates work because the same event is also published to `gap.events`, where `StarsGapEventListener` picks it up and calls the identical `recalculateCurrentProjection()`. This causes (1) wasted computation, (2) two projection writes per event, and (3) potential race conditions on the `version` column.

**Test update:**

In `CareGapEventApplicationServiceTest.java`, remove any `verify(starsProjectionService)` calls and the mock setup for `StarsProjectionService`. The constructor under test should no longer accept it.

---

### Task 4: Expand `StarsGapEventListener` to handle all lifecycle events

**Stories:** 3.1, 3.2, 3.3  
**Depends on:** Task 1 (JsonCreator on event DTOs)  
**Estimated LOC changed:** ~40

**File:** `backend/modules/services/care-gap-event-service/src/main/java/com/healthdata/caregap/listener/StarsGapEventListener.java`

**Current `onGapEvent` method handles:**
- `CareGapDetectedEvent` → detect
- `GapClosedEvent` → close
- `Map` with `closureStatus`/`closureReason` → close
- `Map` with `severity`/`gapDescription` → detect

**New method replaces existing:**

```java
@KafkaListener(topics = "gap.events", groupId = "care-gap-stars-projection")
public void onGapEvent(@Payload Object payload) {
    if (payload instanceof CareGapDetectedEvent event) {
        recalculate(event.getTenantId(), "gap.detected:" + event.getGapCode());
        return;
    }

    if (payload instanceof GapClosedEvent event) {
        recalculate(event.getTenantId(), "gap.closed:" + event.getGapCode());
        return;
    }

    if (payload instanceof PatientQualifiedEvent event) {
        recalculate(event.getTenantId(), "gap.qualified:" + event.getGapCode());
        return;
    }

    if (payload instanceof InterventionRecommendedEvent event) {
        recalculate(event.getTenantId(), "gap.intervention:" + event.getGapCode());
        return;
    }

    if (payload instanceof Map<?, ?> mapPayload) {
        String tenantId = asString(mapPayload.get("tenantId"));
        String gapCode = asString(mapPayload.get("gapCode"));
        if (tenantId == null || gapCode == null) {
            return;
        }

        if (mapPayload.containsKey("closureStatus") || mapPayload.containsKey("closureReason")) {
            recalculate(tenantId, "gap.closed:" + gapCode);
            return;
        }

        if (mapPayload.containsKey("qualified")) {
            recalculate(tenantId, "gap.qualified:" + gapCode);
            return;
        }

        if (mapPayload.containsKey("intervention")) {
            recalculate(tenantId, "gap.intervention:" + gapCode);
            return;
        }

        if (mapPayload.containsKey("severity") || mapPayload.containsKey("gapDescription")) {
            recalculate(tenantId, "gap.detected:" + gapCode);
            return;
        }
    }

    log.debug("Ignoring unrecognized gap event payload type: {}", payload.getClass().getSimpleName());
}

private void recalculate(String tenantId, String triggerEvent) {
    log.debug("Recalculating Stars projection for tenant {} (trigger: {})", tenantId, triggerEvent);
    starsProjectionService.recalculateCurrentProjection(tenantId, triggerEvent);
}
```

**Add imports:**
```java
import com.healthdata.caregap.event.PatientQualifiedEvent;
import com.healthdata.caregap.event.InterventionRecommendedEvent;
```

**Remove** the now-unused `recalculateAfterDetection()` and `recalculateAfterClosure()` private methods (replaced by the unified `recalculate()` method).

**Why:** The listener currently ignores `PatientQualifiedEvent` and `InterventionRecommendedEvent`. These events change the gap state (qualification affects denominator eligibility; intervention may change status), so the persisted projection becomes stale.

---

### Task 5: Update `StarsGapEventListenerTest` for new event types

**Stories:** 3.1, 3.2, 3.3  
**Depends on:** Task 4  
**Estimated LOC changed:** ~60

**File:** `backend/modules/services/care-gap-event-service/src/test/java/com/healthdata/caregap/listener/StarsGapEventListenerTest.java`

**Add tests:**

```java
@Test
void onGapEvent_recalculatesProjectionForQualifiedEvent() {
    listener.onGapEvent(new PatientQualifiedEvent("tenant-a", "patient-1", "COL", true, "Met criteria"));

    verify(starsProjectionService).recalculateCurrentProjection("tenant-a", "gap.qualified:COL");
}

@Test
void onGapEvent_recalculatesProjectionForInterventionEvent() {
    listener.onGapEvent(new InterventionRecommendedEvent("tenant-a", "patient-1", "COL", "Schedule screening", "HIGH"));

    verify(starsProjectionService).recalculateCurrentProjection("tenant-a", "gap.intervention:COL");
}

@Test
void onGapEvent_recalculatesProjectionForQualifiedMapPayload() {
    listener.onGapEvent(Map.of(
        "tenantId", "tenant-a",
        "gapCode", "COL",
        "qualified", true
    ));

    verify(starsProjectionService).recalculateCurrentProjection("tenant-a", "gap.qualified:COL");
}

@Test
void onGapEvent_recalculatesProjectionForInterventionMapPayload() {
    listener.onGapEvent(Map.of(
        "tenantId", "tenant-a",
        "gapCode", "COL",
        "intervention", "Schedule screening"
    ));

    verify(starsProjectionService).recalculateCurrentProjection("tenant-a", "gap.intervention:COL");
}
```

**Update existing test method names** to use consistent naming if desired (optional).

**Add imports:**
```java
import com.healthdata.caregap.event.PatientQualifiedEvent;
import com.healthdata.caregap.event.InterventionRecommendedEvent;
```

---

### Task 6: Add `@Version` to `StarRatingProjection` and remove manual increment

**Stories:** 2.1  
**Depends on:** None  
**Estimated LOC changed:** ~5

#### 6a. `backend/modules/services/care-gap-event-service/src/main/java/com/healthdata/caregap/projection/StarRatingProjection.java`

**Change:**
```java
@Column(name = "version", nullable = false)
@Builder.Default
private long version = 0L;
```
**To:**
```java
@jakarta.persistence.Version
@Column(name = "version", nullable = false)
@Builder.Default
private long version = 0L;
```

#### 6b. `backend/modules/services/care-gap-event-service/src/main/java/com/healthdata/caregap/service/StarsProjectionService.java`

**Remove line** in `recalculateCurrentProjection()`:
```java
projection.setVersion(projection.getVersion() + 1);
```

JPA will auto-increment the `@Version` field on every `save()` call.

**Why:** Without `@Version`, concurrent Kafka consumer threads could overwrite each other's projections silently. JPA optimistic locking throws `OptimisticLockException` on conflict, which is the correct behavior (the next event retry will re-read and re-apply).

---

### Task 7: Fix `@PreAuthorize` and add `@Audited` to `StarRatingController`

**Stories:** 2.2, 2.3  
**Depends on:** None  
**Estimated LOC changed:** ~15

**File:** `backend/modules/services/care-gap-event-service/src/main/java/com/healthdata/caregap/api/v1/controller/StarRatingController.java`

**Change all three endpoints from:**
```java
@PreAuthorize("hasPermission('CARE_GAP_READ')")
```
**To:**
```java
@PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST')")
```

**Add audit annotations (import `com.healthdata.audit.Audited` or the project's `@Audited` annotation):**

```java
@GetMapping("/current")
@PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST')")
@Audited(eventType = "STAR_RATING_READ")
@Operation(summary = "Get current Stars projection")
public ResponseEntity<StarRatingResponse> getCurrent(...) { ... }

@GetMapping("/trend")
@PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST')")
@Audited(eventType = "STAR_RATING_TREND_READ")
@Operation(summary = "Get weekly/monthly Stars trend")
public ResponseEntity<StarRatingTrendResponse> getTrend(...) { ... }

@PostMapping("/simulate")
@PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST')")
@Audited(eventType = "STAR_RATING_SIMULATE")
@Operation(summary = "Run on-demand Stars simulation")
public ResponseEntity<StarRatingResponse> simulate(...) { ... }
```

**Why:** `hasPermission('CARE_GAP_READ')` is non-standard in the HDIM codebase. Every other controller uses `hasAnyRole(...)`. The `@Audited` annotation ensures HIPAA audit trail generation.

---

### Task 8: Add Liquibase rollback directives

**Stories:** 2.4  
**Depends on:** None  
**Estimated LOC changed:** ~20

#### 8a. `backend/modules/services/care-gap-event-service/src/main/resources/db/changelog/0004-create-star-rating-projections.xml`

**Add inside the `<changeSet>` element, after `</createTable>`:**
```xml
        <rollback>
            <dropTable tableName="star_rating_projections"/>
        </rollback>
```

#### 8b. `backend/modules/services/care-gap-event-service/src/main/resources/db/changelog/0005-create-star-rating-snapshots.xml`

**Add inside the `<changeSet>` element, after the `</addUniqueConstraint>` element:**
```xml
        <rollback>
            <dropUniqueConstraint tableName="star_rating_snapshots"
                                  constraintName="uk_star_rating_snapshot_tenant_date_granularity"/>
            <dropIndex tableName="star_rating_snapshots"
                       indexName="idx_star_rating_snapshots_tenant_date"/>
            <dropTable tableName="star_rating_snapshots"/>
        </rollback>
```

**Why:** HDIM requires 100% Liquibase rollback coverage (currently 199/199 changesets). These two migrations break that contract. Adding rollbacks restores compliance and enables safe schema revert in production incidents.

---

### Task 9: Integration test — event-driven Stars recalculation

**Stories:** 1.3  
**Depends on:** Tasks 2, 3, 4  
**Estimated LOC changed:** ~80

**File (new):** `backend/modules/services/care-gap-event-service/src/test/java/com/healthdata/caregap/integration/StarsProjectionIntegrationTest.java`

```java
package com.healthdata.caregap.integration;

import com.healthdata.caregap.event.CareGapDetectedEvent;
import com.healthdata.caregap.event.GapClosedEvent;
import com.healthdata.caregap.event.PatientQualifiedEvent;
import com.healthdata.caregap.persistence.CareGapProjectionRepository;
import com.healthdata.caregap.persistence.StarRatingProjectionRepository;
import com.healthdata.caregap.projection.CareGapProjection;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Tag("integration")
@SpringBootTest
@EmbeddedKafka(topics = "gap.events", partitions = 1)
@ActiveProfiles("test")
class StarsProjectionIntegrationTest {

    private static final String TENANT = "integration-test-tenant";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private CareGapProjectionRepository careGapProjectionRepository;

    @Autowired
    private StarRatingProjectionRepository starRatingProjectionRepository;

    @Test
    void publishedDetectEvent_triggersProjectionCreation() {
        // Arrange: seed a care-gap projection row so the calculator has data
        CareGapProjection gap = new CareGapProjection("patient-1", TENANT, "COL", "Screening", "HIGH");
        gap.setStatus("OPEN");
        careGapProjectionRepository.save(gap);

        // Act: publish detect event to Kafka
        kafkaTemplate.send("gap.events", "patient-1",
            new CareGapDetectedEvent(TENANT, "patient-1", "COL", "Screening", "HIGH"));

        // Assert: projection row appears within 10 s
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var projection = starRatingProjectionRepository.findById(TENANT);
            assertThat(projection).isPresent();
            assertThat(projection.get().getOpenGapCount()).isGreaterThanOrEqualTo(1);
            assertThat(projection.get().getLastTriggerEvent()).startsWith("gap.detected:");
        });
    }

    @Test
    void publishedCloseEvent_updatesProjectionCounts() {
        // Arrange
        CareGapProjection gap = new CareGapProjection("patient-2", TENANT, "CBP", "BP Control", "HIGH");
        gap.setStatus("CLOSED");
        careGapProjectionRepository.save(gap);

        // Act
        kafkaTemplate.send("gap.events", "patient-2",
            new GapClosedEvent(TENANT, "patient-2", "CBP", "Completed", "CLOSED"));

        // Assert
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var projection = starRatingProjectionRepository.findById(TENANT);
            assertThat(projection).isPresent();
            assertThat(projection.get().getLastTriggerEvent()).startsWith("gap.closed:");
        });
    }

    @Test
    void publishedQualifyEvent_triggersRecalculation() {
        // Arrange
        CareGapProjection gap = new CareGapProjection("patient-3", TENANT, "BCS", "Screening", "MEDIUM");
        gap.setStatus("OPEN");
        careGapProjectionRepository.save(gap);

        // Act
        kafkaTemplate.send("gap.events", "patient-3",
            new PatientQualifiedEvent(TENANT, "patient-3", "BCS", true, "Met criteria"));

        // Assert
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var projection = starRatingProjectionRepository.findById(TENANT);
            assertThat(projection).isPresent();
            assertThat(projection.get().getLastTriggerEvent()).startsWith("gap.qualified:");
        });
    }
}
```

**Why:** This is the final integration proof that todo 008 requires. It validates the complete path: Kafka publish → `StarsGapEventListener` → `StarsProjectionService.recalculateCurrentProjection()` → `StarRatingProjectionRepository.save()`.

---

### Task 10: Reopen (re-detect) end-to-end test

**Stories:** 3.3  
**Depends on:** Task 4  
**Estimated LOC changed:** ~25

**File:** `backend/modules/services/care-gap-event-service/src/test/java/com/healthdata/caregap/service/StarsProjectionServiceTest.java`

**Add test:**

```java
@Test
void recalculateCurrentProjection_reflectsReopenedGap() {
    // Initially: gap closed
    when(careGapProjectionRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(
        gap("patient-1", "COL", "CLOSED")
    ));
    when(starRatingProjectionRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

    StarRatingResponse closed = starsProjectionService.recalculateCurrentProjection(TENANT_ID, "gap.closed:COL");
    assertThat(closed.getClosedGapCount()).isEqualTo(1);
    assertThat(closed.getOpenGapCount()).isEqualTo(0);

    // After re-detect: gap reopened (status = OPEN)
    when(careGapProjectionRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(
        gap("patient-1", "COL", "OPEN")
    ));
    when(starRatingProjectionRepository.findById(TENANT_ID)).thenReturn(
        Optional.of(StarRatingProjection.builder().tenantId(TENANT_ID).build())
    );

    StarRatingResponse reopened = starsProjectionService.recalculateCurrentProjection(TENANT_ID, "gap.detected:COL");
    assertThat(reopened.getOpenGapCount()).isEqualTo(1);
    assertThat(reopened.getClosedGapCount()).isEqualTo(0);
}
```

**Why:** Validates that re-detection (reopen) of a previously closed gap correctly flips the gap counts. This is the acceptance criterion for Story 3.3.

---

## Task Execution Order Summary

| Order | Task | Stories | Depends On | Independent? |
|-------|------|---------|------------|--------------|
| 1 | Add `@JsonCreator` to event DTOs | 3.4 | — | ✅ |
| 2 | Remove orphan Kafka topic beans | 1.1 | — | ✅ |
| 3 | Eliminate double recalculation | 1.2 | — | ✅ |
| 4 | Expand listener for lifecycle events | 3.1, 3.2, 3.3 | Task 1 | ❌ |
| 5 | Update listener tests | 3.1, 3.2, 3.3 | Task 4 | ❌ |
| 6 | Add `@Version` + remove manual increment | 2.1 | — | ✅ |
| 7 | Fix `@PreAuthorize` + add `@Audited` | 2.2, 2.3 | — | ✅ |
| 8 | Add Liquibase rollback directives | 2.4 | — | ✅ |
| 9 | Integration test | 1.3 | Tasks 2, 3, 4 | ❌ |
| 10 | Reopen test | 3.3 | Task 4 | ❌ |

**Parallelizable groups:**
- **Group A (independent):** Tasks 1, 2, 3, 6, 7, 8 can all be done in parallel.
- **Group B (sequential):** Task 4 → Task 5 → Tasks 9, 10.

---

## 4. Success Criteria

### Per-task verification

| Task | Verification Command | Expected Output |
|------|---------------------|------------------|
| 1 | `./gradlew :modules:services:care-gap-event-handler-service:test` | All tests pass |
| 2 | `grep -r "gap.detected\|gap.closed" backend/modules/services/care-gap-event-service/src/main/java/com/healthdata/caregap/config/` → 0 matches | No orphan topics |
| 3 | `./gradlew :modules:services:care-gap-event-service:test --tests "*CareGapEventApplicationServiceTest"` | Pass, no starsProjectionService verify |
| 4 | `./gradlew :modules:services:care-gap-event-service:test --tests "*StarsGapEventListenerTest"` | 9 tests pass (was 5) |
| 5 | Same as Task 4 | 9 tests pass |
| 6 | `./gradlew :modules:services:care-gap-event-service:test --tests "*StarsProjectionServiceTest"` | Pass, version auto-managed |
| 7 | `grep -c "hasPermission" backend/modules/services/care-gap-event-service/src/main/java/com/healthdata/caregap/api/v1/controller/StarRatingController.java` → 0 | No non-standard auth |
| 8 | `./gradlew :modules:services:care-gap-event-service:test --tests "*EntityMigrationValidationTest"` | Pass with rollback coverage |
| 9 | `./gradlew :modules:services:care-gap-event-service:testIntegration --tests "*StarsProjectionIntegrationTest"` | 3 integration tests pass |
| 10 | `./gradlew :modules:services:care-gap-event-service:test --tests "*StarsProjectionServiceTest"` | 6 unit tests pass (was 5) |

### Global regression check

```bash
# Full test suite for care-gap-event-service
./gradlew :modules:services:care-gap-event-service:test

# Cross-service check (ensure no breakage from event DTO changes)
./gradlew :modules:services:care-gap-event-handler-service:test

# Entity-migration validation
./gradlew test --tests "*EntityMigrationValidationTest"

# Rollback coverage
# (manual) Confirm 201/201 changesets have rollback directives (was 199/199 + 2 new)
```

### Final acceptance gate

| Criterion | How to verify |
|-----------|---------------|
| No double recalculation | `grep -rn "starsProjectionService" CareGapEventApplicationService.java` → 0 matches |
| Single topic contract | `gap.events` is the only topic in `KafkaConfig` (besides `intervention.recommended`) |
| All lifecycle events handled | `StarsGapEventListenerTest` has tests for detect, close, qualify, intervention (typed + map) = 9 tests |
| Optimistic locking enabled | `@Version` annotation visible on `StarRatingProjection.version` |
| HIPAA audit trail | `@Audited` on all 3 controller endpoints |
| Standard RBAC | `@PreAuthorize("hasAnyRole(...")` on all 3 controller endpoints |
| Rollback coverage | Both migration XMLs contain `<rollback>` blocks |
| Integration proof | `StarsProjectionIntegrationTest` has 3 green tests |

---

## 5. Risk Analysis

### 5.1 Breaking Changes

| Change | Breaking? | Mitigation |
|--------|-----------|------------|
| Remove `gap.detected`/`gap.closed` topic beans | **No** — never produced to or consumed | Verify with `grep` before merge |
| Remove sync `recalculateCurrentProjection()` from app service | **No** — async path via listener already works | Integration test proves it |
| Add `@Version` | **Low risk** — requires `version` column to already exist (it does) | JPA manages increment; no schema change needed |
| Change `@PreAuthorize` | **Low risk** — if `CARE_GAP_READ` permission was used by an external system it would break | Audit shows no external callers; internal Clinical Portal uses role-based auth |
| Add `@Audited` | **No** — additive only | N/A |
| Add Jackson constructors to event DTOs | **No** — additive; existing constructors still work | N/A |
| Expand listener for new event types | **No** — additive; previously ignored events now trigger benign recalculation | N/A |

### 5.2 Backward Compatibility

| Concern | Analysis |
|---------|----------|
| Kafka topics | `gap.events` is unchanged. Orphans `gap.detected`/`gap.closed` have no producers or consumers — removing their auto-creation beans is safe. If a Kafka cluster already has these topics, they'll remain as empty stale topics (harmless). |
| API contracts | All three Star Rating endpoints retain the same URL, HTTP method, request/response schema. Only the auth annotation changes (`hasPermission` → `hasAnyRole`). |
| Database schema | No schema changes. `@Version` is a JPA annotation on an existing column. |
| Event schema | `PatientQualifiedEvent` and `InterventionRecommendedEvent` get new constructors (additive). Existing callers are unaffected. |

### 5.3 Migration Strategy

1. **Deploy order:** These changes are all within `care-gap-event-service` and `care-gap-event-handler-service`. No cross-service deployment coordination required.
2. **Feature flag:** Not needed — changes are correctness fixes, not new features.
3. **Data migration:** None required. The `star_rating_projections` table schema is unchanged. Existing rows will be re-computed on next event or on-demand read.
4. **Kafka:** No topic migration needed. `gap.events` already exists and is the authoritative topic.

### 5.4 Rollback Plan

| Scenario | Rollback Action |
|----------|-----------------|
| Listener fails to handle new event types | Revert Task 4 commit; old listener ignores unknown events gracefully |
| `@Version` causes `OptimisticLockException` storms | Revert Task 6; add retry logic to listener (Spring Kafka retry already configured) |
| Integration test unstable | Tag as `@Disabled` and file follow-up; core unit tests still validate logic |
| `@PreAuthorize` change breaks access | Revert Task 7 to restore `hasPermission`; investigate role mapping |

### 5.5 Residual Risks (Out of Scope for P1)

| Risk | Todo | Notes |
|------|------|-------|
| CQRS `CareGapEventListener` listens on `care-gap.identified`/`care-gap.closed` which are never produced to | Future P2 | Different package (`caregapevent.listener` vs `caregap.listener`); separate concern |
| H2 test profile uses `create-drop` + disabled Liquibase | Todo 008 (remaining) | Requires test infrastructure change; not blocking P1 fixes |
| Test coverage for domain scoring, overall rating edge cases | Todo 014 (P2) | Tracked separately |
| Snapshot freshness metadata | Todo 013 (P2) | Tracked separately |
| Granularity separation in trend API | Todo 010 (P2) | Tracked separately |

---

## Appendix: File Change Summary

| File | Tasks | Type |
|------|-------|------|
| `care-gap-event-handler-service/.../event/PatientQualifiedEvent.java` | 1 | Modify |
| `care-gap-event-handler-service/.../event/InterventionRecommendedEvent.java` | 1 | Modify |
| `care-gap-event-service/.../config/KafkaConfig.java` | 2 | Modify |
| `care-gap-event-service/.../service/CareGapEventApplicationService.java` | 3 | Modify |
| `care-gap-event-service/.../listener/StarsGapEventListener.java` | 4 | Modify |
| `care-gap-event-service/.../listener/StarsGapEventListenerTest.java` | 5 | Modify |
| `care-gap-event-service/.../projection/StarRatingProjection.java` | 6 | Modify |
| `care-gap-event-service/.../service/StarsProjectionService.java` | 6 | Modify |
| `care-gap-event-service/.../controller/StarRatingController.java` | 7 | Modify |
| `care-gap-event-service/.../db/changelog/0004-create-star-rating-projections.xml` | 8 | Modify |
| `care-gap-event-service/.../db/changelog/0005-create-star-rating-snapshots.xml` | 8 | Modify |
| `care-gap-event-service/.../service/CareGapEventApplicationServiceTest.java` | 3 | Modify |
| `care-gap-event-service/.../service/StarsProjectionServiceTest.java` | 10 | Modify |
| `care-gap-event-service/.../integration/StarsProjectionIntegrationTest.java` | 9 | **New** |

**Total files:** 14 (13 modified, 1 new)  
**Estimated total LOC changed:** ~300
