---
description: CQRS and event sourcing patterns for HDIM event-driven architecture
tags:
  - cqrs
  - event-sourcing
  - kafka
  - eventual-consistency
  - read-models
  - projections
---

# CQRS Event-Driven Skill

Comprehensive guidance for CQRS (Command Query Responsibility Segregation) and event-driven architecture in the HDIM platform.

<skill_instructions>

## Overview

HDIM implements **CQRS** pattern with **Kafka-based event sourcing** across 4+ event services (patient, care-gap, quality-measure, clinical-workflow). This skill provides patterns, best practices, and troubleshooting guidance.

**Key Concepts:**
- **Command Side (Write):** Services that handle writes and publish events
- **Event Store:** Apache Kafka (persistent event log)
- **Query Side (Read):** Event services that build optimized projections
- **Eventual Consistency:** < 500ms SLA from event to projection

---

## Architecture Pattern

### CQRS Flow

```
┌──────────────┐         ┌──────────┐        ┌───────────────┐
│  Command     │ publish │  Kafka   │consume │  Query        │
│  Service     ├────────>│  Topics  ├───────>│  Service      │
│              │         │          │        │  (Event)      │
└──────────────┘         └──────────┘        └───────────────┘
      │                                              │
      v                                              v
┌──────────────┐                            ┌───────────────┐
│ Write        │                            │ Read Model    │
│ Database     │                            │ (Projection)  │
│ (Normalized) │                            │ (Denormalized)│
└──────────────┘                            └───────────────┘
```

**Benefits:**
- **Independent Scaling:** Read and write sides scale separately
- **Optimized Reads:** Denormalized projections for fast queries (< 100ms)
- **Event Replay:** Rebuild projections by replaying Kafka events
- **Audit Trail:** All changes captured as immutable events

---

## Event Services (4 Implemented)

### 1. Patient Event Service (Port 8110)

**Consumes Events:**
- `patient.created`
- `patient.updated`
- `patient.status.changed`
- `care-gap.identified` (cross-domain)
- `care-gap.closed`
- `risk-assessment.updated`
- `mental-health.updated`
- `clinical-alert.triggered`
- `clinical-alert.resolved`

**Projection:** `PatientProjection`
- Aggregates patient data + care gaps + alerts + risk scores
- Denormalized for dashboard queries

---

### 2. Care Gap Event Service (Port 8111)

**Consumes Events:**
- `care-gap.identified`
- `care-gap.closed`
- `care-gap.auto-closed`
- `care-gap.priority.changed`
- `care-gap.waived`
- `care-gap.assigned`
- `care-gap.due-date-updated`

**Projection:** `CareGapProjection`
- Pre-calculated gap counts, overdue flags, priority levels

---

### 3. Quality Measure Event Service (Port 8110)

**Consumes Events:**
- `measure.evaluated`
- `measure.score.updated`
- `measure.compliance.changed`
- `measure.numerator.updated`
- `measure.denominator.updated`
- `measure.exclusion.updated`

**Projection:** `QualityMeasureProjection`
- HEDIS measure results, compliance status, scores

---

### 4. Clinical Workflow Event Service (Port 8113)

**Consumes Events:**
- `workflow.started`
- `workflow.assigned`
- `workflow.reassigned`
- `workflow.progress.updated`
- `workflow.completed`
- `workflow.cancelled`
- `workflow.review.required`
- `workflow.blocking.issue`

**Projection:** `WorkflowProjection`
- Workflow status, assignment, progress tracking

---

## Event Schema Patterns

### Standard Event Structure

**All events MUST include:**

```json
{
  "eventId": "UUID (unique per event)",
  "eventType": "domain.action (past tense)",
  "tenantId": "tenant-001 (REQUIRED for HIPAA)",
  "timestamp": "2024-01-20T10:00:00Z (ISO-8601)",
  "eventVersion": 1,

  // Domain-specific payload
  "patientId": "UUID",
  "field1": "value1",
  "field2": "value2"
}
```

**Naming Conventions:**
- **Event Type:** `{domain}.{past-tense-action}`
  - ✅ `patient.created` (past tense)
  - ❌ `patient.create` (imperative)
- **Hyphenated:** `care-gap.auto-closed` (not `careGapAutoClosed`)
- **Consistent:** Use same field names across events

---

## Projection Patterns

### Denormalization Strategy

**Projections aggregate data from multiple events:**

```java
@Entity
@Table(name = "patient_projections")
public class PatientProjection {
    // From patient.created event
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;

    // From risk-assessment.updated event
    private Double riskScore;
    private String riskLevel;

    // From care-gap.identified events (aggregated)
    private Integer openCareGapsCount;      // Incremented
    private Integer urgentCareGapsCount;
    private Integer closedCareGapsCount;    // Decremented

    // From clinical-alert.triggered events (aggregated)
    private Integer activeAlertsCount;
    private Boolean hasCriticalAlert;

    // Pre-calculated flags (business logic)
    private Boolean isHighRisk;  // riskScore > 0.7
    private Boolean hasOverdueCareGaps;
}
```

**Benefits:**
- Single database query (no joins)
- Pre-calculated aggregates and flags
- Optimized for dashboard/reporting

**Tradeoffs:**
- Storage overhead (duplicated data)
- Eventual consistency (slight delay)

---

## Idempotency Patterns

### Find-or-Create Pattern

**All event handlers MUST be idempotent** (safe to process same event multiple times).

**Pattern:**

```java
@KafkaListener(topics = "patient.created")
@Transactional
public void onPatientCreated(String message) {
    JsonNode event = objectMapper.readTree(message);

    String tenantId = event.get("tenantId").asText();
    UUID patientId = UUID.fromString(event.get("patientId").asText());

    // Idempotent: find existing or create new
    projectionRepository.findByTenantIdAndPatientId(tenantId, patientId)
        .ifPresentOrElse(
            projection -> {
                // Projection exists - log warning and skip
                log.warn("Projection already exists for patient={}, skipping duplicate event", patientId);
            },
            () -> {
                // Create new projection
                PatientProjection projection = PatientProjection.builder()
                    .tenantId(tenantId)
                    .patientId(patientId)
                    .firstName(event.get("firstName").asText())
                    .lastName(event.get("lastName").asText())
                    .eventVersion(0L)
                    .build();

                projectionRepository.save(projection);
                log.info("Created projection for patient={}", patientId);
            }
        );
}
```

**Why Idempotency Matters:**
- Kafka may redeliver messages (at-least-once semantics)
- Event replay scenarios (projection rebuild)
- Out-of-order event delivery

---

## Eventual Consistency

### SLA and Testing

**Target SLA:** < 500ms from event publication to projection update

**How to Test:**

```java
@Test
void shouldMeetEventualConsistencySLA() throws Exception {
    UUID patientId = UUID.randomUUID();
    String eventJson = createPatientCreatedEvent(patientId);

    long startTime = System.currentTimeMillis();
    kafkaTemplate.send("patient.created", eventJson).get();

    // Poll until projection exists (max 500ms)
    await().atMost(500, TimeUnit.MILLISECONDS)
           .pollInterval(10, TimeUnit.MILLISECONDS)
           .until(() -> repository.findByPatientId(patientId).isPresent());

    long latency = System.currentTimeMillis() - startTime;
    log.info("Eventual consistency latency: {}ms", latency);

    assertThat(latency).isLessThan(500);  // Assert SLA met
}
```

**Use Awaitility library** for polling (not `Thread.sleep`):

```kotlin
// build.gradle.kts
testImplementation("org.awaitility:awaitility:4.2.0")
```

---

## Kafka Configuration

### Shared Messaging Module

**Location:** `modules/shared/infrastructure/messaging/`

**Auto-Configuration:**
- Producer: acks=all, idempotence=true, compression=snappy
- Consumer: earliest offset, manual commit, JSON deserialization

**Usage in Event Services:**

```kotlin
// build.gradle.kts
implementation(project(":modules:shared:infrastructure:messaging"))
```

**application.yml:**

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9094}
    consumer:
      group-id: patient-event-service  # Unique per service
      auto-offset-reset: earliest      # Replay from beginning
      enable-auto-commit: false        # Manual commit
      value-deserializer: JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"  # Trust all (dev only)
        interceptor.classes: com.healthdata.tracing.KafkaConsumerTraceInterceptor
    listener:
      ack-mode: record  # Commit after each record
```

---

## Topic Naming Conventions

**Pattern:** `{domain}.{past-tense-action}`

**Examples:**
- ✅ `patient.created`
- ✅ `care-gap.identified`
- ✅ `measure.evaluated`
- ✅ `workflow.assigned`

**Cross-Domain Events:**
- Patient event service consumes `care-gap.identified` (cross-domain)
- Allows aggregation across bounded contexts

---

## Multi-Tenant Isolation

**CRITICAL:** Every projection MUST isolate by tenant.

### Database Level

```sql
-- Unique constraint: one projection per entity per tenant
CREATE UNIQUE INDEX idx_pp_tenant_patient
ON patient_projections(tenant_id, patient_id);

-- Index for query performance
CREATE INDEX idx_pp_tenant_id
ON patient_projections(tenant_id);
```

### Repository Level

```java
// All queries MUST include tenantId
Optional<PatientProjection> findByTenantIdAndPatientId(
    String tenantId,
    UUID patientId
);

// Paginated queries
Page<PatientProjection> findByTenantIdOrderByLastUpdatedAtDesc(
    String tenantId,
    Pageable pageable
);
```

### Controller Level

```java
@GetMapping("/{id}")
@PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'VIEWER')")
public ResponseEntity<PatientProjection> getProjection(
        @PathVariable UUID id,
        @RequestHeader("X-Tenant-ID") String tenantId) {  // REQUIRED

    return repository.findByTenantIdAndPatientId(tenantId, id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());  // 404, not 403
}
```

**Why 404 instead of 403:**
- Prevents information disclosure (doesn't reveal entity exists)
- HIPAA compliance (no tenant enumeration)

---

## Performance Optimization

### Query Response Time

**Target:** < 100ms (99th percentile)

**Optimization Strategies:**

1. **Denormalization** - Pre-calculate aggregates
```java
// Don't join at query time
private Integer openCareGapsCount;  // Pre-calculated from events

// Not this:
// @OneToMany
// private List<CareGap> careGaps;  // Expensive join
```

2. **Indexes** - Optimize for common queries
```xml
<!-- Composite index for filtered queries -->
<createIndex indexName="idx_pp_tenant_status" tableName="patient_projections">
    <column name="tenant_id"/>
    <column name="status"/>
</createIndex>

<!-- Index for sorting -->
<createIndex indexName="idx_pp_updated_at" tableName="patient_projections">
    <column name="last_updated_at"/>
</createIndex>
```

3. **Pagination** - Limit result sets
```java
// Always use Pageable for large datasets
Page<PatientProjection> findByTenantId(String tenantId, Pageable pageable);
```

4. **Caching** - NO Redis caching for event services
```yaml
spring:
  cache:
    type: none  # Stateless projections, no caching needed
```

---

## Error Handling

### Event Processing Failures

**Current Pattern:** Fail-fast (throw exception → Kafka retries)

```java
@KafkaListener(topics = "patient.created")
@Transactional
public void onPatientCreated(String message) {
    try {
        // Process event
    } catch (Exception e) {
        log.error("Failed to process event: {}", message, e);
        throw new RuntimeException("Event processing failed", e);
        // Kafka will retry based on consumer config
    }
}
```

**Retry Configuration:**

```yaml
spring:
  kafka:
    listener:
      ack-mode: record  # Commit after success
      # On failure, Kafka retries automatically
```

**Dead Letter Queue (DLQ):**
- NOT currently implemented
- **Potential improvement:** Add DLQ for poison messages

---

## Event Versioning

### Schema Evolution

**Backward Compatibility:**
- Add new fields with defaults
- Never remove required fields
- Use semantic versioning in events

**Example:**

```json
// v1
{
  "eventVersion": 1,
  "patientId": "UUID",
  "firstName": "John"
}

// v2 (backward compatible)
{
  "eventVersion": 2,
  "patientId": "UUID",
  "firstName": "John",
  "middleName": "William"  // New optional field
}
```

**Handler Pattern:**

```java
@KafkaListener(topics = "patient.created")
public void onPatientCreated(String message) {
    JsonNode event = objectMapper.readTree(message);

    int version = event.get("eventVersion").asInt(1);  // Default v1

    switch (version) {
        case 1 -> handleV1(event);
        case 2 -> handleV2(event);
        default -> throw new UnsupportedOperationException("Unsupported version: " + version);
    }
}
```

---

## Projection Rebuild

### Replaying Events

**Scenario:** Need to rebuild projections (new field, bug fix, data migration)

**Pattern:**

1. **Create new consumer group:**
```yaml
spring:
  kafka:
    consumer:
      group-id: patient-event-service-rebuild  # New group ID
      auto-offset-reset: earliest  # Start from beginning
```

2. **Truncate projection table:**
```sql
TRUNCATE TABLE patient_projections;
```

3. **Start service** - Will replay all events from Kafka

4. **Monitor progress:**
```bash
docker logs -f hdim-patient-event-service | grep "Created projection"
```

5. **Switch back to original group** after rebuild

**Potential Improvement:** Add `/admin/rebuild` endpoint for automated rebuilds.

---

## Testing Patterns

### Integration Test Structure

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class PatientEventFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientProjectionRepository repository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void shouldProjectEventToReadModel() throws Exception {
        // Given: Event published to Kafka
        String eventJson = createPatientCreatedEvent();
        kafkaTemplate.send("patient.created", eventJson).get(5, TimeUnit.SECONDS);

        // When: Wait for eventual consistency
        await().atMost(2, TimeUnit.SECONDS)
               .until(() -> repository.count() > 0);

        // Then: Query projection via REST API
        mockMvc.perform(get("/api/v1/patient-projections")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].patientId").exists());
    }
}
```

**Key Testing Practices:**
- ✅ Use Testcontainers (no external dependencies)
- ✅ Measure eventual consistency timing
- ✅ Verify multi-tenant isolation
- ✅ Test idempotency (send same event twice)
- ✅ Test out-of-order events

---

## HIPAA Compliance

### PHI Handling in Projections

**Projections may contain PHI** - Apply same controls as command side:

1. **Cache Control:**
```java
@GetMapping("/{id}")
public ResponseEntity<PatientProjection> get(@PathVariable UUID id) {
    return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())  // No caching
            .header("X-Content-Type-Options", "nosniff")
            .body(projection);
}
```

2. **Audit Logging:** (Recommended)
```java
@Audited(eventType = "PHI_ACCESS")
public PatientProjection getProjection(UUID id, String tenantId) {
    // ...
}
```

3. **Tenant Isolation:** Enforced at all levels (DB, repo, controller)

---

## Monitoring & Observability

### Key Metrics

**Consumer Lag:**
```bash
# Check Kafka consumer lag
docker exec hdim-kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe --group patient-event-service
```

**Projection Freshness:**
```sql
-- Find stale projections (not updated in 24h)
SELECT COUNT(*)
FROM patient_projections
WHERE last_updated_at < NOW() - INTERVAL '24 hours';
```

**Event Processing Rate:**
```java
@Timed(value = "event.processing.time", description = "Event processing duration")
public void onPatientCreated(String message) {
    // Metrics tracked automatically
}
```

---

## Common Pitfalls

### 1. Missing Tenant Filter

**❌ Wrong:**
```java
Optional<PatientProjection> findByPatientId(UUID patientId);
// Cross-tenant data leak!
```

**✅ Correct:**
```java
Optional<PatientProjection> findByTenantIdAndPatientId(String tenantId, UUID patientId);
```

---

### 2. Blocking Event Handlers

**❌ Wrong:**
```java
@KafkaListener(topics = "patient.created")
public void onPatientCreated(String message) {
    CompletableFuture.supplyAsync(() -> {
        // Process event asynchronously
    }).get();  // BLOCKS Kafka consumer thread!
}
```

**✅ Correct:**
```java
@KafkaListener(topics = "patient.created")
@Transactional
public void onPatientCreated(String message) {
    // Process synchronously in transaction
    // Kafka consumer thread is OK to block briefly
}
```

---

### 3. Non-Idempotent Handlers

**❌ Wrong:**
```java
@KafkaListener(topics = "care-gap.identified")
public void onCareGapIdentified(String message) {
    // Always increment count (not idempotent!)
    projection.setOpenCareGapsCount(projection.getOpenCareGapsCount() + 1);
}
```

**✅ Correct:**
```java
@KafkaListener(topics = "care-gap.identified")
public void onCareGapIdentified(String message) {
    // Idempotent: check if gap already counted
    if (gapRepository.existsByTenantIdAndGapId(tenantId, gapId)) {
        log.warn("Gap already counted, skipping");
        return;
    }
    projection.setOpenCareGapsCount(projection.getOpenCareGapsCount() + 1);
}
```

---

## Best Practices Summary

**DO:**
- ✅ Use find-or-create pattern for idempotency
- ✅ Filter all queries by tenantId
- ✅ Denormalize for read performance (< 100ms)
- ✅ Measure eventual consistency (< 500ms SLA)
- ✅ Use Awaitility for async testing
- ✅ Include eventId, tenantId, timestamp in all events
- ✅ Use past-tense event names (`created`, not `create`)
- ✅ Add indexes on tenant_id and query fields
- ✅ Return 404 (not 403) for unauthorized access

**DON'T:**
- ❌ Skip tenantId validation
- ❌ Block Kafka consumer threads with async calls
- ❌ Forget idempotency handling
- ❌ Use joins in projections (denormalize instead)
- ❌ Cache PHI data (use Cache-Control: no-store)
- ❌ Modify existing events (backward compatibility)
- ❌ Use Thread.sleep in tests (use Awaitility)

---

## Quick Reference

**Create Event Service:**
```bash
/create-event-service medication 8114 medication_event_db
```

**Add Event Handler:**
```bash
/add-event-handler patient-event-service patient.discharge
```

**Test Eventual Consistency:**
```java
await().atMost(500, TimeUnit.MILLISECONDS)
       .until(() -> repository.findById(id).isPresent());
```

**Query Projection:**
```bash
curl http://localhost:8110/patient-event/api/v1/patient-projections/{id} \
  -H "X-Tenant-ID: tenant-001"
```

---

## Documentation

- **CQRS Guide:** `backend/docs/CQRS_INTEGRATION_TESTING_GUIDE.md`
- **Implementation Summary:** `backend/docs/CQRS_IMPLEMENTATION_FINAL_SUMMARY.md`
- **Messaging Module:** `backend/modules/shared/infrastructure/messaging/README.md`
- **Event Catalog:** `docs/events/EVENT_CATALOG.md` (TODO)

</skill_instructions>
