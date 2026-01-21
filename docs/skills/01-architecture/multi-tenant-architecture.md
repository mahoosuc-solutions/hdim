# Multi-Tenant Architecture - Skill Guide

> **This is a comprehensive guide for implementing secure multi-tenant isolation in HDIM.**
> **Multi-tenancy is non-negotiable: one tenant's data must NEVER leak to another.**

---

## Overview

### What is This Skill?

Multi-tenant architecture is a design pattern where a single HDIM deployment serves multiple independent organizations (healthcare payers, health systems, ACOs) with complete data isolation. Each tenant's data is logically and physically separated, ensuring that queries, caches, and events are tenant-scoped—tenant A never sees tenant B's patients, measures, or clinical data.

**Example:** Anthem (tenant: "anthem") and Blue Cross (tenant: "bluecross") both run quality measures on the same HDIM platform, but their patient data is completely isolated. A query for patient P123 by Anthem always returns Anthem's patient P123, never Blue Cross's patient P123.

### Why is This Important for HDIM?

Healthcare organizations are regulated entities with contractual obligations to protect data confidentiality. HIPAA requires strict data isolation between unrelated entities. Multi-tenant isolation is not an optimization—it's a legal requirement. A single data leak or cross-tenant query bug can:

- Expose Protected Health Information (PHI) across contractual boundaries
- Trigger HIPAA violations ($100-$50,000 fines per violation)
- Destroy customer trust and contracts
- Result in service shutdown and criminal liability

### Business Impact

- **Operational Simplicity:** Single HDIM deployment serves unlimited healthcare organizations
- **Cost Efficiency:** Shared infrastructure (1 PostgreSQL, 1 Kafka, 1 Redis) reduces deployment overhead
- **Regulatory Compliance:** Meets HIPAA data isolation requirements
- **Scale:** Enables 1000+ patient evaluation workflows across 100+ tenants simultaneously

### Key Services Using This Skill

All 51 HDIM services implement multi-tenant isolation:

**Core Services:**
- patient-event-service (8084) - Patient data by tenant
- quality-measure-event-service (8087) - Measure results by tenant
- care-gap-event-service (8086) - Care gaps by tenant
- clinical-workflow-event-service - Workflow events by tenant
- fhir-service (8085) - FHIR resources by tenant

**Supporting Services:**
- gateway-service (8001) - Validates X-Tenant-ID header
- cql-engine-service (8081) - Evaluates measures for specific tenant
- analytics-service - Reports by tenant
- audit-service - Audit logs by tenant

### Estimated Learning Time

1.5-2 weeks (foundational skill; requires hands-on implementation)

---

## Key Concepts

### Concept 1: Tenant Identification

**Definition:** Every request in HDIM is scoped to a specific tenant via the `X-Tenant-ID` HTTP header. The tenant ID is a unique identifier for each healthcare organization (e.g., "anthem", "bluecross", "medicare-part-b").

**Why it matters:** Tenant ID is the security boundary. All queries, caches, and events must filter by tenant ID. Without this, data isolation fails.

**Real-world example:**
```java
// Request from Anthem application
GET /api/v1/patients/P123
X-Tenant-ID: anthem

// Request from Blue Cross application
GET /api/v1/patients/P123
X-Tenant-ID: bluecross

// HDIM must return DIFFERENT patient records (same ID, different tenant)
```

### Concept 2: Database-Level Isolation (Row-Level Security)

**Definition:** Multi-tenancy is enforced at the PostgreSQL database layer using Row-Level Security (RLS) policies. Every table has a `tenant_id` column, and queries automatically filter rows by tenant.

**Why it matters:** Database-level enforcement is the strongest guarantee. Even if application code is buggy, the database prevents cross-tenant access.

**Real-world example:**
```sql
-- Every table has tenant_id column
CREATE TABLE patients (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,  -- Security boundary
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    CONSTRAINT fk_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

-- Row-Level Security policy: only show rows for current tenant
CREATE POLICY tenant_isolation ON patients
    USING (tenant_id = current_setting('app.current_tenant_id'));
```

### Concept 3: Cache Namespace Isolation

**Definition:** Redis cache keys include tenant ID as a namespace prefix (e.g., `anthem:patient:P123`, `bluecross:patient:P123`). Caches are tenant-scoped—tenant A's cached data cannot be accessed by tenant B.

**Why it matters:** Caching is a performance optimization, but it must respect tenant boundaries. Mixing tenant data in cache causes cross-tenant data leaks.

**Real-world example:**
```java
// Cache key includes tenant namespace
@Cacheable(value = "patients", key = "'tenant:' + #tenantId + ':patient:' + #patientId")
public Patient getPatient(String patientId, String tenantId) {
    return patientRepository.findByIdAndTenantId(patientId, tenantId);
}

// Cache miss for anthem:patient:P123 does not return bluecross:patient:P123
```

### Concept 4: Event Stream Isolation (Kafka Topics)

**Definition:** Events published to Kafka include tenant context, and consumers subscribe to topic partitions scoped by tenant. Kafka producers partition events by tenant ID, ensuring tenant isolation in the message stream.

**Why it matters:** Events flow between services asynchronously. Tenant isolation must be preserved across event consumption—an event from tenant A cannot trigger processing for tenant B.

**Real-world example:**
```java
// Publish event with tenant context
KafkaTemplate.send("patient.events",
    tenantId,  // Key: partitions by tenant
    PatientCreatedEvent.builder()
        .tenantId(tenantId)  // Payload: includes tenant
        .patientId(patientId)
        .build()
);

// Consumer receives events partitioned by tenant
@KafkaListener(topics = "patient.events", groupId = "patient-projection-group")
public void handlePatientCreated(PatientCreatedEvent event) {
    // Event already scoped to event.getTenantId()
    PatientProjection projection = new PatientProjection();
    projection.setTenantId(event.getTenantId());
    projectionRepository.save(projection);
}
```

### Concept 5: Security Context Propagation

**Definition:** The tenant ID is extracted from the HTTP header by the gateway and propagated through all downstream services via the `X-Tenant-ID` header. Services trust this header (validated once by gateway) and use it to scope all operations.

**Why it matters:** Tenant context must flow through the entire request lifecycle. If tenant ID is lost mid-request, subsequent operations become unscoped (critical security bug).

**Real-world example:**
```
Client Request
↓
Gateway (validates X-Tenant-ID header)
↓
Forward: X-Tenant-ID: anthem
↓
Service A (reads X-Tenant-ID: anthem, scopes queries to tenant)
↓
Calls Service B: X-Tenant-ID: anthem
↓
Service B (reads X-Tenant-ID: anthem, scopes queries to tenant)
↓
Response with anthem's data only
```

---

## Architecture Pattern

### How It Works

HDIM implements multi-tenancy at 5 independent layers, creating overlapping security boundaries:

1. **Gateway Layer:** Validates `X-Tenant-ID` header on every request
2. **Application Layer:** Services read `X-Tenant-ID` header and pass to repositories
3. **Repository Layer:** All JPA queries filter by tenant ID (e.g., `WHERE tenant_id = ?`)
4. **Cache Layer:** Redis keys include tenant namespace (e.g., `anthem:patient:P123`)
5. **Event Layer:** Kafka partitions and events include tenant context
6. **Database Layer:** PostgreSQL Row-Level Security policies enforce tenant isolation at SQL execution time

### Diagram

```
┌─────────────────────────────────────────────────────────────┐
│ Client Application (Anthem)                                 │
│ GET /api/v1/patients/P123                                  │
│ Header: X-Tenant-ID: anthem                                │
└──────────────────────────┬──────────────────────────────────┘
                           │
                    ┌──────▼──────┐
                    │  Gateway    │
                    │  Validates  │
                    │  X-Tenant-ID│
                    │  "anthem"   │
                    └──────┬──────┘
                           │
          ┌────────────────┼────────────────┐
          │                │                │
    ┌─────▼────┐     ┌─────▼────┐    ┌─────▼────┐
    │ Patient  │     │ Quality  │    │ Care Gap │
    │ Service  │     │ Measure  │    │ Service  │
    │ (8084)   │     │ (8087)   │    │ (8086)   │
    └─────┬────┘     └─────┬────┘    └─────┬────┘
          │                │                │
    ┌─────▼──────────────────────────────────▼────┐
    │ Repository Layer                             │
    │ SELECT * FROM patients                      │
    │   WHERE tenant_id = 'anthem'  ← FILTERED   │
    │     AND id = 'P123'                        │
    └─────┬──────────────────────────────────────┘
          │
    ┌─────▼────────────────────────────┐
    │ PostgreSQL Database              │
    │ Row-Level Security Policy        │
    │ (tenant_isolation)               │
    │ USING (tenant_id = 'anthem')     │
    └─────┬────────────────────────────┘
          │
    ┌─────▼────────────────────────────┐
    │ Redis Cache                      │
    │ Key: anthem:patient:P123         │
    │ TTL: 5 minutes (PHI compliant)   │
    └─────┬────────────────────────────┘
          │
    ┌─────▼────────────────────────────┐
    │ Kafka Event Stream               │
    │ Topic: patient.events            │
    │ Partition: hash(tenant_id)       │
    │ Event: {tenantId: "anthem", ...} │
    └──────────────────────────────────┘
```

### Design Decisions

**Decision 1: Why filter by tenant_id in EVERY query instead of separate databases?**
- **Trade-off:** Shared database is simpler to manage but requires discipline in query construction. Separate databases are safer but 100x more expensive and operationally complex.
- **Rationale:** HDIM supports 100+ tenants. Managing 100 separate PostgreSQL instances would be prohibitively expensive. Filtering by tenant_id in queries is acceptable if enforced systematically (see Entity-Migration Validation section in CLAUDE.md).
- **Alternative:** Separate databases per tenant (safety vs. cost trade-off). Not viable at HDIM scale.

**Decision 2: Why trust X-Tenant-ID header instead of validating it at every service?**
- **Trade-off:** Trusting header after gateway validation is performant but concentrates validation risk at gateway. Full validation everywhere is safer but slower.
- **Rationale:** Gateway is the security boundary. If gateway is compromised, system is compromised anyway. Validating at every service adds latency without improving security.
- **Alternative:** Validate at every service (JWT pattern). Not adopted to reduce latency.

**Decision 3: Why partition Kafka by tenant_id instead of separate topics?**
- **Trade-off:** Shared topic with tenant partitioning is simpler. Separate topics per tenant are safer but create O(n) topics where n = number of tenants.
- **Rationale:** HDIM has 100+ tenants. Creating 100+ Kafka topics (1 per tenant) is operationally complex. Partitioning by tenant achieves same isolation goal with single topic.
- **Alternative:** Separate topic per tenant (isolation without risk of cross-partition reads). Not adopted to reduce operational complexity.

### Trade-offs

| Aspect | Pro | Con |
|--------|-----|-----|
| **Shared Database** | Simple to manage (1 PostgreSQL instance) | Requires discipline in query construction; bug = data leak |
| **Row-Level Security** | Database-enforced isolation; prevents app bugs | Small performance overhead; requires PostgreSQL 10+ |
| **Cache Namespacing** | Prevents cross-tenant cache hits; improves hits | Adds complexity to cache keys; requires consistent naming |
| **Tenant in Every Event** | Complete audit trail; easy debugging | Larger event payloads; bandwidth cost |
| **Gateway Trust Pattern** | Low latency; centralized validation | Concentrates security risk; gateway becomes critical component |
| **Performance** | Filters reduce query results early (good) | N+1 queries if tenant scoping forgotten (bad) |

---

## Implementation Guide

### Step-by-Step

#### Step 1: Define Tenant Entity

Create a `Tenant` entity representing each healthcare organization:

```java
@Entity
@Table(name = "tenants")
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String tenantCode;  // "anthem", "bluecross", "medicare"

    @Column(nullable = false)
    private String organizationName;  // "Anthem Blue Cross Blue Shield"

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Boolean active;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        active = true;
    }
}
```

**Liquibase Migration:**
```xml
<changeSet id="0001-create-tenants-table" author="hdim">
    <createTable tableName="tenants">
        <column name="id" type="uuid">
            <constraints primaryKey="true"/>
        </column>
        <column name="tenant_code" type="varchar(255)">
            <constraints nullable="false" unique="true"/>
        </column>
        <column name="organization_name" type="varchar(500)">
            <constraints nullable="false"/>
        </column>
        <column name="created_at" type="timestamp">
            <constraints nullable="false"/>
        </column>
        <column name="active" type="boolean">
            <constraints nullable="false"/>
        </column>
    </createTable>
</changeSet>
```

#### Step 2: Add Tenant ID to Every Entity

Every entity table must have a `tenant_id` column as the first security boundary:

```java
@Entity
@Table(name = "patients")
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;  // Security boundary: filter ALL queries by this

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    // Other fields...

    @PrePersist
    protected void onCreate() {
        // Tenant ID MUST be set before persist
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID is required");
        }
    }
}
```

**Liquibase Migration (for existing table):**
```xml
<changeSet id="0002-add-tenant-id-to-patients" author="hdim">
    <!-- Add column -->
    <addColumn tableName="patients">
        <column name="tenant_id" type="varchar(255)">
            <constraints nullable="false"/>
        </column>
    </addColumn>

    <!-- Add foreign key -->
    <addForeignKeyConstraint
        baseTableName="patients"
        baseColumnNames="tenant_id"
        referencedTableName="tenants"
        referencedColumnNames="tenant_code"
        constraintName="fk_patients_tenant"/>

    <!-- Add index for performance -->
    <createIndex tableName="patients" indexName="idx_patients_tenant_id">
        <column name="tenant_id"/>
    </createIndex>

    <!-- Rollback -->
    <rollback>
        <dropIndex tableName="patients" indexName="idx_patients_tenant_id"/>
        <dropForeignKeyConstraint baseTableName="patients" constraintName="fk_patients_tenant"/>
        <dropColumn tableName="patients" columnName="tenant_id"/>
    </rollback>
</changeSet>
```

#### Step 3: Filter Every Repository Query by Tenant

Every repository method must include tenant filtering:

```java
@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    // ✅ CORRECT: Filters by tenant
    @Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId AND p.id = :id")
    Optional<Patient> findByIdAndTenantId(
        @Param("id") UUID id,
        @Param("tenantId") String tenantId);

    // ✅ CORRECT: List by tenant
    @Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId")
    List<Patient> findAllByTenantId(@Param("tenantId") String tenantId);

    // ✅ CORRECT: Count by tenant
    @Query("SELECT COUNT(p) FROM Patient p WHERE p.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") String tenantId);

    // ❌ WRONG: No tenant filtering (SECURITY VIOLATION)
    // Optional<Patient> findById(UUID id);

    // ❌ WRONG: Finds patient across all tenants (DATA LEAK)
    // Optional<Patient> findByFirstNameAndLastName(String firstName, String lastName);
}
```

#### Step 4: Extract Tenant ID from HTTP Header

Create a filter to extract tenant ID and make it available to all services:

```java
@Component
public class TenantContextFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String tenantId = httpRequest.getHeader("X-Tenant-ID");

        if (tenantId == null || tenantId.isEmpty()) {
            throw new SecurityException("X-Tenant-ID header is required");
        }

        // Store in thread-local for this request
        TenantContext.setCurrentTenant(tenantId);

        try {
            chain.doFilter(request, response);
        } finally {
            // Clean up thread-local
            TenantContext.clear();
        }
    }
}

// Thread-local holder
public class TenantContext {
    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();

    public static void setCurrentTenant(String tenantId) {
        TENANT.set(tenantId);
    }

    public static String getCurrentTenant() {
        String tenantId = TENANT.get();
        if (tenantId == null) {
            throw new SecurityException("No tenant context in current thread");
        }
        return tenantId;
    }

    public static void clear() {
        TENANT.remove();
    }
}
```

**Register Filter in Spring:**
```java
@Configuration
public class WebConfig {
    @Bean
    public FilterRegistrationBean<TenantContextFilter> tenantContextFilter() {
        FilterRegistrationBean<TenantContextFilter> bean =
            new FilterRegistrationBean<>(new TenantContextFilter());
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);  // Run first
        return bean;
    }
}
```

#### Step 5: Pass Tenant ID to Service Layer

Services extract tenant from context and pass to repositories:

```java
@Service
@RequiredArgsConstructor
public class PatientService {
    private final PatientRepository patientRepository;

    public PatientResponse getPatient(String patientId) {
        String tenantId = TenantContext.getCurrentTenant();

        Patient patient = patientRepository
            .findByIdAndTenantId(UUID.fromString(patientId), tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        return PatientResponse.builder()
            .id(patient.getId())
            .firstName(patient.getFirstName())
            .lastName(patient.getLastName())
            .build();
    }

    @Transactional
    public PatientResponse createPatient(CreatePatientRequest request) {
        String tenantId = TenantContext.getCurrentTenant();

        Patient patient = Patient.builder()
            .tenantId(tenantId)  // ✅ Set tenant on entity
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .build();

        patientRepository.save(patient);
        return mapToResponse(patient);
    }
}
```

#### Step 6: Configure Cache Namespace by Tenant

Cache keys must include tenant ID to prevent cross-tenant cache hits:

```java
@Configuration
public class CacheConfig {
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return builder -> builder
            .withCacheConfiguration("patients",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(5))
                    .computePrefixWith(cacheName ->
                        TenantContext.getCurrentTenant() + ":" + cacheName + ":")
            );
    }
}

// In service layer:
@Service
public class PatientService {
    @Cacheable(value = "patients",
               key = "'patient:' + #patientId")  // Cache key: tenant:patients:patient:P123
    public Patient getPatient(String patientId) {
        // Cached separately per tenant
        return patientRepository.findByIdAndTenantId(
            UUID.fromString(patientId),
            TenantContext.getCurrentTenant()
        );
    }
}
```

#### Step 7: Publish Events with Tenant Context

Events must include tenant information for downstream services:

```java
@Service
@RequiredArgsConstructor
public class PatientEventPublisher {
    private final KafkaTemplate<String, PatientCreatedEvent> kafkaTemplate;

    public void publishPatientCreated(Patient patient) {
        PatientCreatedEvent event = PatientCreatedEvent.builder()
            .tenantId(patient.getTenantId())  // ✅ Include tenant in payload
            .patientId(patient.getId())
            .firstName(patient.getFirstName())
            .lastName(patient.getLastName())
            .build();

        // Publish with tenantId as Kafka key (partitions by tenant)
        kafkaTemplate.send("patient.events",
            patient.getTenantId(),  // Key: ensures tenant isolation in partitions
            event);
    }
}

// Consumer receives tenant-scoped events
@Service
public class PatientEventConsumer {
    @KafkaListener(topics = "patient.events", groupId = "patient-projection-group")
    public void handlePatientCreated(PatientCreatedEvent event) {
        // Event already scoped to specific tenant
        PatientProjection projection = new PatientProjection();
        projection.setTenantId(event.getTenantId());  // ✅ Preserve tenant in projection
        projectionRepository.save(projection);
    }
}
```

---

## Real-World Examples from HDIM

### Example 1: Patient Event Service

**Where:** `backend/modules/services/patient-event-service/`

**What it does:** Receives patient data from EHR systems, stores patient projections, and publishes PatientCreatedEvent/PatientUpdatedEvent for other services.

**Key file:** `backend/modules/services/patient-event-service/src/main/java/com/healthdata/patientevent/domain/Patient.java`

**Relevant code:**

```java
@Entity
@Table(name = "patients")
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)  // ✅ Tenant security boundary
    private String tenantId;

    @Column(name = "mrn")  // Medical Record Number
    private String mrn;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    // Service methods
    @Service
    public static class PatientService {
        @Autowired
        private PatientRepository patientRepository;

        public Patient getPatient(String patientId, String tenantId) {
            // ✅ Query filters by tenant
            return patientRepository
                .findByIdAndTenantId(UUID.fromString(patientId), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
        }

        @Transactional
        public Patient createPatient(CreatePatientRequest request, String tenantId) {
            Patient patient = Patient.builder()
                .tenantId(tenantId)  // ✅ Set tenant on entity
                .mrn(request.getMrn())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .build();

            return patientRepository.save(patient);
        }
    }
}
```

**Why this example matters:** Shows foundational multi-tenant pattern. Every entity has tenant_id column; every query filters by tenant. This pattern is replicated across all 51 services.

### Example 2: Quality Measure Event Service

**Where:** `backend/modules/services/quality-measure-event-service/`

**What it does:** Evaluates HEDIS quality measures against patient FHIR data and publishes MeasureEvaluatedEvent. Results are tenant-scoped.

**Key file:** `backend/modules/services/quality-measure-event-service/src/main/java/com/healthdata/qualitymeasure/service/MeasureEvaluationService.java`

**Relevant code:**

```java
@Service
@Transactional(readOnly = true)
public class MeasureEvaluationService {
    private final MeasureResultRepository measureResultRepository;
    private final PatientFhirDataRepository patientFhirDataRepository;

    public MeasureEvaluationResult evaluateMeasure(
            String measureId,
            String patientId,
            String tenantId) {  // ✅ Tenant ID passed explicitly

        // ✅ Load measure definition (scoped to tenant)
        MeasureDefinition measure = measureDefinitionRepository
            .findByIdAndTenantId(UUID.fromString(measureId), tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Measure", measureId));

        // ✅ Load patient FHIR data (scoped to tenant)
        PatientFhirData fhirData = patientFhirDataRepository
            .findByPatientIdAndTenantId(patientId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        // Evaluate CQL against patient data
        CqlExecutionResult result = cqlEngine.execute(
            measure.getCqlLibrary(),
            fhirData
        );

        // ✅ Store result with tenant scoping
        MeasureResult measureResult = MeasureResult.builder()
            .tenantId(tenantId)  // Set tenant on result entity
            .measureId(measure.getId())
            .patientId(patientId)
            .result(result.getResult())
            .build();

        measureResultRepository.save(measureResult);
        return mapToResponse(measureResult);
    }

    @Transactional
    public void publishEvaluationEvent(MeasureResult result) {
        MeasureEvaluatedEvent event = MeasureEvaluatedEvent.builder()
            .tenantId(result.getTenantId())  // ✅ Include tenant in event
            .measureId(result.getMeasureId())
            .patientId(result.getPatientId())
            .result(result.getResult())
            .build();

        // ✅ Publish with tenant as Kafka key
        kafkaTemplate.send("measure.evaluation.complete",
            result.getTenantId(),  // Partition by tenant
            event);
    }
}
```

**Why this example matters:** Shows multi-tenant isolation at the application logic level. Measures and patient data are loaded with tenant scoping; results are stored with tenant context; events are published with tenant key. If any of these steps forget tenant_id, data leaks occur.

### Example 3: Care Gap Event Service

**Where:** `backend/modules/services/care-gap-event-service/`

**What it does:** Consumes MeasureEvaluatedEvents and CareGapDetectedEvents from quality measure service, correlates results, and stores care gap records by tenant.

**Key file:** `backend/modules/services/care-gap-event-service/src/main/java/com/healthdata/caregap/event/CareGapEventHandler.java`

**Relevant code:**

```java
@Service
public class CareGapEventHandler {
    private final CareGapRepository careGapRepository;

    @KafkaListener(topics = "measure.evaluation.complete",
                   groupId = "care-gap-detector")
    public void handleMeasureEvaluated(MeasureEvaluatedEvent event) {
        // ✅ Event includes tenant context
        String tenantId = event.getTenantId();

        if ("CARE_GAP".equals(event.getResult())) {
            // Create care gap record
            CareGap careGap = CareGap.builder()
                .tenantId(tenantId)  // ✅ Set tenant on entity
                .measureId(event.getMeasureId())
                .patientId(event.getPatientId())
                .gapStatus("OPEN")
                .createdAt(Instant.now())
                .build();

            careGapRepository.save(careGap);

            // Publish care gap event with tenant
            publishCareGapDetectedEvent(careGap);
        }
    }

    private void publishCareGapDetectedEvent(CareGap careGap) {
        CareGapDetectedEvent event = CareGapDetectedEvent.builder()
            .tenantId(careGap.getTenantId())  // ✅ Include tenant
            .careGapId(careGap.getId())
            .measureId(careGap.getMeasureId())
            .patientId(careGap.getPatientId())
            .build();

        // ✅ Publish with tenant as key
        kafkaTemplate.send("care-gap.detected",
            careGap.getTenantId(),  // Partition by tenant
            event);
    }
}
```

**Why this example matters:** Shows event-driven multi-tenancy. Events flow between services; tenant context must be preserved at each hop. If care gap service forgets to include tenantId in the event payload or uses wrong Kafka key, downstream services process cross-tenant data.

---

## Best Practices

### ✅ DO's

- ✅ **DO add tenant_id to EVERY entity table**
  - Why: Tenant isolation is only as strong as your weakest query. Every table needs the security boundary.
  - Example: Even `audit_events` table must have `tenant_id` column, so audit logs don't leak across tenants.

- ✅ **DO filter by tenant in EVERY repository query**
  - Why: A single unfiltered query is a data leak. Code reviews must verify every @Query annotation includes `WHERE tenant_id = :tenantId`.
  - Example: `@Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId AND p.id = :id")`

- ✅ **DO use @Query annotations instead of derived query methods**
  - Why: Derived methods (`findById()`) bypass tenant filtering. Explicit @Query makes tenant scoping visible and reviewable.
  - Example: Use `@Query("... WHERE p.tenantId = :tenantId AND p.id = :id")` instead of `findById(id)`.

- ✅ **DO set tenant_id on entities BEFORE persisting**
  - Why: If an entity is saved without tenant_id, it becomes accessible to all tenants (or fails with constraint violation). Use @PrePersist to validate.
  - Example:
    ```java
    @PrePersist
    protected void onCreate() {
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID is required");
        }
    }
    ```

- ✅ **DO include tenant context in all event payloads**
  - Why: Events flow between services. Downstream consumers must know which tenant the event belongs to.
  - Example: `PatientCreatedEvent.builder().tenantId(tenantId).patientId(patientId).build()`

- ✅ **DO use Kafka partition key = tenant_id**
  - Why: Kafka partitioning ensures tenant events stay together. Consumers reading one partition work with one tenant's data only.
  - Example: `kafkaTemplate.send("patient.events", tenantId, event)` where `tenantId` is the partition key.

- ✅ **DO namespace cache keys by tenant**
  - Why: Redis doesn't understand multi-tenancy. Without namespacing, tenant A's cached patient data can be returned to tenant B.
  - Example: Cache key `anthem:patient:P123` instead of just `patient:P123`.

- ✅ **DO verify multi-tenant isolation in tests**
  - Why: The worst security bugs are silent. Test that tenant A cannot read tenant B's data even if code looks correct.
  - Example: Create patient for tenant A, query for it as tenant B, verify empty result.

- ✅ **DO document which queries are multi-tenant safe**
  - Why: Developers joining the team need to know which patterns are safe. Undocumented assumptions lead to bugs.
  - Example: Add comment above repository: `// This query is multi-tenant safe: filters by tenantId`

- ✅ **DO use Row-Level Security (RLS) policies in PostgreSQL**
  - Why: Database-level enforcement catches application bugs. Even if code has a tenant scoping bug, database policies prevent the query from returning cross-tenant data.
  - Example: Create RLS policy that only shows rows where `tenant_id = current_setting('app.current_tenant_id')`

### ❌ DON'Ts

- ❌ **DON'T use derived query methods (findById, findByName, etc.)**
  - Why: Derived methods don't support WHERE clauses for tenant filtering. They're convenient but dangerous.
  - Example: ❌ `Optional<Patient> findById(UUID id)` - no tenant filtering

- ❌ **DON'T mix tenants in a single cache key**
  - Why: Different tenants will get each other's cached data. Cache pollution leads to silent data leaks.
  - Example: ❌ `@Cacheable(key = "'patient:' + #patientId")` - should be `'tenant:' + #tenantId + ':patient:' + #patientId`

- ❌ **DON'T publish events without tenant context**
  - Why: Downstream services won't know which tenant the event belongs to. They'll apply it to wrong tenant or skip it.
  - Example: ❌ `kafkaTemplate.send("patient.events", null, event)` - null key means random partitioning

- ❌ **DON'T trust client-supplied tenant IDs (use header only)**
  - Why: Clients might claim to be a different tenant. Always use the X-Tenant-ID header validated by gateway.
  - Example: ❌ `String tenantId = request.getParameter("tenantId")` - trusts client input

- ❌ **DON'T perform JOIN queries across tenants**
  - Why: Complex JOINs that forget tenant filtering can accidentally combine tenant A's orders with tenant B's customers.
  - Example: ❌ `SELECT o FROM Order o JOIN o.customer c WHERE o.id = ?` - missing `AND o.tenantId = ?`

- ❌ **DON'T create tables without tenant_id column**
  - Why: Even support/audit tables need tenant scoping. A table without tenant_id becomes accessible to all tenants.
  - Example: ❌ `CREATE TABLE audit_logs (id UUID, action VARCHAR(255), ...)` - should have `tenant_id VARCHAR(255) NOT NULL`

- ❌ **DON'T omit tenant from where created_at comparisons or date filters**
  - Why: Date range queries that forget tenant filtering return cross-tenant data within the date range.
  - Example: ❌ `SELECT p FROM Patient p WHERE p.createdAt >= :start AND p.createdAt <= :end` - should also have `AND p.tenantId = :tenantId`

- ❌ **DON'T assume pagination is tenant-safe**
  - Why: LIMIT/OFFSET without tenant filtering returns cross-tenant data. First page might show tenant A data; second page might show tenant B data.
  - Example: ❌ `SELECT p FROM Patient p LIMIT 10 OFFSET 0` - missing tenant filter

- ❌ **DON'T disable Row-Level Security in production**
  - Why: RLS is a safety net. If you disable it, application bugs directly expose cross-tenant data.
  - Example: ❌ `ALTER ROLE app_user SET row_security = off` - never do this

- ❌ **DON'T create backup tables without tenant_id**
  - Why: Backup tables often aren't documented. If someone queries backup table, they get all tenant data.
  - Example: ❌ `CREATE TABLE patients_backup AS SELECT * FROM patients` - should copy tenant_id column

---

## Real-World HIPAA Violations

### Violation 1: Forgotten Tenant Filter

**Scenario:** Developer creates a reporting query to find all patients with diabetes:

```java
// ❌ VIOLATION: No tenant filtering
@Query("SELECT p FROM Patient p WHERE p.hasCondition('diabetes')")
List<Patient> findDiabetesPatients() {
    return patientRepository.findDiabetesPatients();
}

// Result: Query returns diabetes patients from ALL tenants
// Anthem employee sees Blue Cross patients → HIPAA violation
```

**Impact:** Cross-tenant data exposure, HIPAA violation ($100-$50,000 fine), customer contract termination.

**Fix:**

```java
// ✅ CORRECT: Filters by tenant
@Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId AND p.hasCondition('diabetes')")
List<Patient> findDiabetesPatients(@Param("tenantId") String tenantId) {
    return patientRepository.findDiabetesPatients(TenantContext.getCurrentTenant());
}

// Result: Returns diabetes patients for current tenant only
```

### Violation 2: Cache Pollution

**Scenario:** Developer caches patient data without tenant namespace:

```java
// ❌ VIOLATION: Cache key doesn't include tenant
@Cacheable(value = "patients", key = "'patient:' + #patientId")
public Patient getPatient(String patientId) {
    return patientRepository.findById(UUID.fromString(patientId));
}

// Cache miss for patient P123:
// - Anthem requests patient P123 → database returns Anthem's P123
// - Result cached as key "patient:P123"
// - Blue Cross requests patient P123 → cache returns Anthem's data
// → HIPAA violation: Blue Cross sees Anthem's PHI
```

**Impact:** Cross-tenant cache hits, PHI exposure, HIPAA violation.

**Fix:**

```java
// ✅ CORRECT: Cache key includes tenant
@Cacheable(value = "patients", key = "'tenant:' + #tenantId + ':patient:' + #patientId")
public Patient getPatient(String patientId, String tenantId) {
    return patientRepository.findByIdAndTenantId(
        UUID.fromString(patientId),
        tenantId
    );
}

// Cache keys are now separate per tenant:
// - Anthem: "tenant:anthem:patient:P123"
// - Blue Cross: "tenant:bluecross:patient:P123"
```

### Violation 3: Event Without Tenant Context

**Scenario:** Developer publishes events without tenant information:

```java
// ❌ VIOLATION: Event doesn't include tenant
PatientCreatedEvent event = PatientCreatedEvent.builder()
    .patientId(patientId)
    .firstName("John")
    .lastName("Doe")
    .build();

kafkaTemplate.send("patient.events", event);  // No tenantId in payload, no partition key

// Consumer receives event but doesn't know which tenant:
@KafkaListener(topics = "patient.events")
public void handlePatientCreated(PatientCreatedEvent event) {
    // Which tenant does this event belong to?
    // Without tenant context, consumer might create projection for wrong tenant
    PatientProjection projection = new PatientProjection();
    projection.setPatientId(event.getPatientId());  // ❌ Missing tenant!
    projectionRepository.save(projection);  // Projection is unscoped
}

// Result: Event processed without tenant context → projection accessible to all tenants
```

**Impact:** Projection created without tenant scoping, accessible to multiple tenants, HIPAA violation.

**Fix:**

```java
// ✅ CORRECT: Event includes tenant
PatientCreatedEvent event = PatientCreatedEvent.builder()
    .tenantId(tenantId)  // Include in payload
    .patientId(patientId)
    .firstName("John")
    .lastName("Doe")
    .build();

kafkaTemplate.send("patient.events",
    tenantId,  // Use as partition key
    event);

// Consumer processes tenant-scoped event:
@KafkaListener(topics = "patient.events", groupId = "patient-projection-group")
public void handlePatientCreated(PatientCreatedEvent event) {
    PatientProjection projection = new PatientProjection();
    projection.setTenantId(event.getTenantId());  // ✅ Include tenant
    projection.setPatientId(event.getPatientId());
    projectionRepository.save(projection);
}

// Result: Projection created with tenant context, accessible only to correct tenant
```

---

## Testing Strategies

### Unit Testing

```java
@ExtendWith(MockitoExtension.class)
class PatientServiceTest {
    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientService patientService;

    @Test
    void shouldReturnPatient_WhenBelongsToTenant() {
        // ARRANGE
        UUID patientId = UUID.randomUUID();
        String tenantId = "anthem";
        Patient patient = Patient.builder()
            .id(patientId)
            .tenantId(tenantId)
            .firstName("John")
            .lastName("Doe")
            .build();

        when(patientRepository.findByIdAndTenantId(patientId, tenantId))
            .thenReturn(Optional.of(patient));

        TenantContext.setCurrentTenant(tenantId);

        // ACT
        PatientResponse result = patientService.getPatient(patientId.toString());

        // ASSERT
        assertThat(result.getId()).isEqualTo(patientId);
        verify(patientRepository).findByIdAndTenantId(patientId, tenantId);

        TenantContext.clear();
    }

    @Test
    void shouldNotReturnPatient_WhenBelongsToDifferentTenant() {
        // ARRANGE
        UUID patientId = UUID.randomUUID();
        String anthemTenant = "anthem";
        String bluecrossTenant = "bluecross";

        when(patientRepository.findByIdAndTenantId(patientId, bluecrossTenant))
            .thenReturn(Optional.empty());

        TenantContext.setCurrentTenant(bluecrossTenant);

        // ACT & ASSERT: Anthem patient should not be accessible to Blue Cross
        assertThatThrownBy(() -> patientService.getPatient(patientId.toString()))
            .isInstanceOf(ResourceNotFoundException.class);

        verify(patientRepository).findByIdAndTenantId(patientId, bluecrossTenant);

        TenantContext.clear();
    }
}
```

### Integration Testing

```java
@SpringBootTest
@AutoConfigureMockMvc
class PatientControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @Test
    void shouldReturnPatientData_WhenRequestedByCorrectTenant() throws Exception {
        // ARRANGE: Create patient for Anthem tenant
        Patient anthemPatient = Patient.builder()
            .tenantId("anthem")
            .firstName("John")
            .lastName("Doe")
            .build();
        patientRepository.save(anthemPatient);

        // ACT & ASSERT: Anthem can retrieve their patient
        mockMvc.perform(get("/api/v1/patients/" + anthemPatient.getId())
                .header("X-Tenant-ID", "anthem"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void shouldNotReturnPatientData_WhenRequestedByDifferentTenant() throws Exception {
        // ARRANGE: Create patient for Anthem tenant
        Patient anthemPatient = Patient.builder()
            .tenantId("anthem")
            .firstName("John")
            .lastName("Doe")
            .build();
        patientRepository.save(anthemPatient);

        // ACT & ASSERT: Blue Cross cannot retrieve Anthem's patient
        mockMvc.perform(get("/api/v1/patients/" + anthemPatient.getId())
                .header("X-Tenant-ID", "bluecross"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldIsolateCachePerTenant() throws Exception {
        // ARRANGE: Create same patient ID for two tenants
        Patient anthemPatient = Patient.builder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            .tenantId("anthem")
            .firstName("Anthem John")
            .build();
        patientRepository.save(anthemPatient);

        Patient bluecrossPatient = Patient.builder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            .tenantId("bluecross")
            .firstName("Blue Cross John")
            .build();
        patientRepository.save(bluecrossPatient);

        // ACT: Request patient as Anthem
        mockMvc.perform(get("/api/v1/patients/00000000-0000-0000-0000-000000000001")
                .header("X-Tenant-ID", "anthem"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Anthem John"));

        // ACT: Request patient as Blue Cross (should NOT get cached Anthem data)
        mockMvc.perform(get("/api/v1/patients/00000000-0000-0000-0000-000000000001")
                .header("X-Tenant-ID", "bluecross"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Blue Cross John"));
    }
}
```

### Multi-Tenant Isolation Checklist

- [ ] Every entity has `tenant_id` column
- [ ] Every repository query filters by `tenant_id`
- [ ] Tenant ID extracted from HTTP header (not client request body)
- [ ] Cache keys include tenant namespace
- [ ] Events include tenant context in payload
- [ ] Kafka events use tenant as partition key
- [ ] Unit tests verify tenant isolation (same patient ID, different tenants)
- [ ] Integration tests verify cross-tenant access is denied
- [ ] No unfiltered queries that span multiple tenants
- [ ] Row-Level Security policies enabled in PostgreSQL

---

## Troubleshooting

### Common Issues

#### Issue 1: Cross-Tenant Data Leak in Query Results

**Symptoms:**
- Anthem employee queries patient P123, gets Blue Cross's patient P123
- Reporting endpoint returns patients from multiple tenants
- Cache returns data for wrong tenant

**Root cause:** Repository query forgot `WHERE tenant_id = ?` clause or cache key doesn't include tenant namespace.

**Solution:**

```bash
# Step 1: Find the query in codebase
grep -r "findByPatient\|SELECT.*FROM patients" backend/modules/services/*/src/

# Step 2: Verify query includes tenant filter
# ❌ WRONG: SELECT p FROM Patient p WHERE p.id = :id
# ✅ RIGHT: SELECT p FROM Patient p WHERE p.tenantId = :tenantId AND p.id = :id

# Step 3: Verify cache key includes tenant
# ❌ WRONG: @Cacheable(key = "'patient:' + #patientId")
# ✅ RIGHT: @Cacheable(key = "'tenant:' + #tenantId + ':patient:' + #patientId")

# Step 4: Verify tenant passed to method
# Make sure TenantContext.getCurrentTenant() is called before query
```

**Prevention:** Code review checklist must verify every @Query and @Cacheable includes tenant scoping.

#### Issue 2: Events Published Without Tenant Context

**Symptoms:**
- Projections created without tenant_id
- Downstream services process events out of order across tenants
- Care gaps appear for wrong tenant

**Root cause:** Event payload doesn't include tenantId, or Kafka key is null.

**Solution:**

```bash
# Step 1: Verify event payload includes tenant
# ❌ WRONG:
# PatientCreatedEvent event = PatientCreatedEvent.builder()
#     .patientId(patientId)
#     .build();

# ✅ RIGHT:
# PatientCreatedEvent event = PatientCreatedEvent.builder()
#     .tenantId(tenantId)  // Include tenant
#     .patientId(patientId)
#     .build();

# Step 2: Verify Kafka key includes tenant
# ❌ WRONG: kafkaTemplate.send("topic", event)
# ✅ RIGHT: kafkaTemplate.send("topic", tenantId, event)

# Step 3: Verify consumer preserves tenant context
# ✅ RIGHT: projection.setTenantId(event.getTenantId())
```

**Prevention:** Event contracts should include tenantId field. Code reviews should verify both payload and Kafka key.

#### Issue 3: Missing Tenant ID on Entity

**Symptoms:**
- Constraint violation: "tenant_id cannot be null"
- Entity saves successfully but is inaccessible (no tenant scoping)
- @PrePersist validation fails

**Root cause:** Entity created without setting tenantId before save.

**Solution:**

```bash
# Step 1: Verify @PrePersist validates tenant_id
@PrePersist
protected void onCreate() {
    if (tenantId == null) {
        throw new IllegalStateException("Tenant ID is required");
    }
}

# Step 2: Verify service sets tenant before save
Patient patient = Patient.builder()
    .tenantId(TenantContext.getCurrentTenant())  // Must be set
    .firstName("John")
    .build();
patientRepository.save(patient);

# Step 3: Add test to verify constraint
@Test
void shouldFailWhenTenantIdNotSet() {
    Patient patient = Patient.builder()
        .firstName("John")
        // Missing .tenantId(...)
        .build();

    assertThatThrownBy(() -> patientRepository.save(patient))
        .isInstanceOf(ConstraintViolationException.class);
}
```

**Prevention:** @PrePersist annotation should validate tenantId is set.

### Debug Techniques

```bash
# Check tenant context in logs
# Add to TenantContextFilter:
log.info("Processing request for tenant: {}", tenantId);

# Query database directly to verify tenant isolation
docker exec -it hdim-postgres psql -U healthdata -d patient_db
SELECT COUNT(*) FROM patients WHERE tenant_id = 'anthem';
SELECT COUNT(*) FROM patients WHERE tenant_id = 'bluecross';

# Check cache keys include tenant
docker exec -it hdim-redis redis-cli
KEYS "anthem:patient:*"  # Should see anthem's keys
KEYS "bluecross:patient:*"  # Should see bluecross's keys
KEYS "patient:*" # Should be empty (no tenant-less keys)

# Verify Kafka partitions by tenant
docker exec -it hdim-kafka kafka-topics.sh --describe --topic patient.events --bootstrap-server localhost:9092
# Look at Leader/Replicas to verify tenant partitioning

# Check for unfiltered queries in code
grep -r "@Query(" backend/modules/services/ | grep -v "tenant_id"
# Any matches = potential security bug
```

---

## References & Resources

### HDIM Documentation

- [Entity-Migration Guide](./backend/docs/ENTITY_MIGRATION_GUIDE.md) - Synchronizing entities with Liquibase migrations
- [HIPAA Compliance Guide](./docs/skills/03-security-compliance/hipaa-compliance.md) - PHI protection requirements
- [CQRS + Event Sourcing Guide](./docs/skills/01-architecture/cqrs-event-sourcing.md) - Event publishing patterns
- [Service Catalog](./docs/services/SERVICE_CATALOG.md) - All 51 services and their ports
- [Database Architecture Guide](./backend/docs/DATABASE_ARCHITECTURE_GUIDE.md) - PostgreSQL multi-tenant design
- [Liquibase Development Workflow](./backend/docs/LIQUIBASE_DEVELOPMENT_WORKFLOW.md) - Migration best practices

### External Resources

- **[PostgreSQL Row-Level Security](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)** - Official RLS documentation
- **[Spring Data JPA @Query](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html)** - JPA query methods and @Query
- **[Spring Cloud Stream with Kafka](https://spring.io/projects/spring-cloud-stream)** - Event publishing patterns
- **[HIPAA 45 CFR 164.508](https://www.hhs.gov/hipaa/for-professionals/privacy/guidance/index.html)** - HIPAA de-identification requirements

### Related Skills

- **Prerequisite:** [CQRS + Event Sourcing](./01-architecture/cqrs-event-sourcing.md) - Understand event flow before multi-tenancy
- **Complement:** [HIPAA Compliance](./03-security-compliance/hipaa-compliance.md) - Learn why multi-tenancy is required
- **Advanced:** [PostgreSQL + Liquibase](./04-data-persistence/postgresql-liquibase.md) - Advanced multi-tenant schema patterns

---

## Quick Reference Checklist

### Before You Start

- [ ] Understand why multi-tenancy is critical (HIPAA, contracts)
- [ ] Know which tenants will use this deployment (Anthem, Blue Cross, etc.)
- [ ] Understand X-Tenant-ID header extraction and propagation
- [ ] Have reviewed CQRS/Event Sourcing guide (events need tenant context)

### While Implementing

- [ ] Adding tenant_id column to new entity tables
- [ ] Every @Query includes `WHERE tenant_id = :tenantId`
- [ ] Derived query methods (findById) are NOT used
- [ ] Entity @PrePersist validates tenant_id is set
- [ ] Service layer passes tenant from TenantContext to repository
- [ ] Cache keys include tenant namespace prefix
- [ ] Events include tenantId in payload
- [ ] Kafka send() uses tenant as partition key
- [ ] Tests verify cross-tenant queries return empty results
- [ ] Code reviewed for tenant scoping by peer

### After Implementation

- [ ] All multi-tenant tests passing
- [ ] Code review verifies tenant scoping in all queries
- [ ] Performance acceptable (tenant filtering doesn't significantly slow queries)
- [ ] Cache hit rates verified per tenant (not mixed)
- [ ] Production runbook includes tenant isolation verification steps
- [ ] Ready for production deployment

---

## Key Takeaways

1. **Multi-tenancy is a Security Boundary:** Every query, cache key, and event must include tenant context. A single forgotten WHERE clause = HIPAA violation.

2. **Layered Isolation:** Use multiple security layers (gateway → filter → repository → cache → database) so a bug at one layer doesn't cause data leak. Defense in depth.

3. **Test Cross-Tenant Scenarios:** The worst security bugs are silent. Always test that tenant A cannot read/write tenant B's data, even if code looks correct.

4. **Tenant Context Matters:** Tenant ID must flow through entire request lifecycle. Use thread-local holder (TenantContext) to avoid passing tenantId parameter through every method.

5. **Events Need Tenant Context:** In event-driven architecture, every event must include and preserve tenant identity. Missing tenant in event = downstream data leak.

---

## FAQ

**Q: What if we want to add a new tenant?**
A: Create new row in `tenants` table with unique `tenant_code`. All existing queries automatically work for new tenant (WHERE tenant_id = ?) because isolation is database-level, not application-hardcoded.

**Q: Can tenants ever see aggregated data across tenants?**
A: Only if explicitly designed for cross-tenant reports with HIPAA-compliant aggregation (de-identified, HIPAA safe harbor, or expert determination). Regular queries should never return cross-tenant data.

**Q: What's the performance impact of tenant filtering?**
A: Minimal if indexed properly. Add composite index on (tenant_id, id) or (tenant_id, status) for common queries. Query planner uses tenant filter to reduce result set early.

**Q: How do we handle reporting where tenants want to compare metrics?**
A: Aggregate at application layer (service calls separate queries per tenant, then combines results). Never query across tenants in single SQL query.

**Q: Can we disable Row-Level Security in development?**
A: No. RLS should be enabled everywhere (dev, test, prod) so developers catch bugs early. Disabling RLS in dev leads to production data leaks.

---

## Next Steps

After completing this guide:

1. **Practice:** Implement multi-tenant isolation in an existing service
2. **Review:** Have peer review your entity, repositories, service layer for tenant scoping
3. **Test:** Write multi-tenant unit and integration tests
4. **Learn:** Move to [PostgreSQL + Liquibase](./04-data-persistence/postgresql-liquibase.md) for schema design
5. **Contribute:** Help code review other PRs for tenant scoping compliance

---

**← Previous Guide:** [HIPAA Compliance](./03-security-compliance/hipaa-compliance.md)
**Skills Hub:** [Skills Center](./README.md)
**Next Guide:** [PostgreSQL + Liquibase](./04-data-persistence/postgresql-liquibase.md)

---

**Last Updated:** January 20, 2026
**Version:** 1.0
**Difficulty Level:** ⭐⭐⭐⭐ (4/5 stars - Foundational complexity)
**Time Investment:** 1.5-2 weeks
**Prerequisite Skills:** CQRS + Event Sourcing, HIPAA Compliance
**Related Skills:** CQRS, Event Sourcing, PostgreSQL, Liquibase, Spring Data JPA, Spring Security

---

**← [Skills Hub](./README.md)** | **→ [Next: PostgreSQL + Liquibase Guide](../04-data-persistence/postgresql-liquibase.md)**
