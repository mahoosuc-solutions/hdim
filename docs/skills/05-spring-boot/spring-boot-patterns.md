# Spring Boot 3.x Microservices - Skill Guide

> **This is a comprehensive guide for building HDIM microservices with Spring Boot 3.x.**
> **Spring Boot 3.x is the framework for all 51 HDIM services; understanding it is foundational.**

---

## Overview

### What is This Skill?

Spring Boot 3.x is a Java framework that simplifies building production-grade microservices. It provides:

1. **Dependency Injection:** Automatic bean management (no boilerplate)
2. **Auto-Configuration:** Sensible defaults (no XML configuration)
3. **Embedded Servers:** Tomcat/Netty built-in (no application server needed)
4. **Spring Data JPA:** Repository pattern for database access
5. **Spring Security:** Authentication and authorization framework
6. **Spring Kafka:** Event-driven messaging integration
7. **Actuator:** Health checks, metrics, observability endpoints

**Example:** Create `@RestController` endpoint, Spring Boot auto-configures HTTP server, Hibernate ORM, database connection pool, JSON serialization. Single `main()` method starts entire service.

### Why is This Important for HDIM?

All 51 HDIM services are built on Spring Boot 3.x. Understanding core Spring Boot patterns is prerequisite for:
- Writing microservices that work with event-driven architecture
- Implementing multi-tenant isolation in application layer
- Following HIPAA compliance patterns (caching, auditing, security)
- Building testable services (dependency injection, mocking)
- Deploying services in containers (12-factor app principles)

### Business Impact

- **Developer Velocity:** Spring Boot reduces boilerplate; developers focus on business logic
- **Operational Consistency:** All services follow same patterns (easier onboarding)
- **Production Readiness:** Built-in health checks, metrics, logging enable observability
- **Security:** Spring Security framework prevents common vulnerabilities

### Key Services Using This Skill

All 51 HDIM services:

**Event Services:**
- patient-event-service (8084)
- quality-measure-event-service (8087)
- care-gap-event-service (8086)
- clinical-workflow-event-service

**Domain Services:**
- fhir-service (8085)
- cql-engine-service (8081)
- analytics-service
- audit-service

### Estimated Learning Time

1.5-2 weeks (hands-on coding required)

---

## Key Concepts

### Concept 1: Spring Boot Application Class

**Definition:** Entry point for Spring Boot service annotated with `@SpringBootApplication`. This single annotation enables component scanning, auto-configuration, and embedded server.

**Why it matters:** `@SpringBootApplication` is shorthand for:
- `@Configuration` (enables bean definitions)
- `@ComponentScan` (auto-discovers Spring components)
- `@EnableAutoConfiguration` (loads auto-configuration classes)

**Real-world example:**
```java
@SpringBootApplication
public class PatientEventServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PatientEventServiceApplication.class, args);
    }
}

// This single line does:
// 1. Starts embedded Tomcat server on port 8084
// 2. Initializes datasource and connection pool
// 3. Runs Liquibase migrations
// 4. Sets Hibernate to validate mode
// 5. Enables component scanning (@Component, @Service, @Repository)
// 6. Loads application.yml configuration
// 7. Ready to handle HTTP requests
```

### Concept 2: Layered Architecture (Service → Repository → Entity)

**Definition:** HDIM services follow 3-layer architecture:
1. **Controller Layer (REST API):** HTTP endpoints, request/response handling
2. **Service Layer (Business Logic):** Domain logic, orchestration, transactions
3. **Repository Layer (Data Access):** JPA queries, database operations

**Why it matters:** Separation of concerns enables testing each layer independently (unit tests) and together (integration tests).

**Real-world example:**
```
HTTP Request
↓
Controller (REST endpoint)
├─ Validates input
├─ Extracts X-Tenant-ID header
├─ Calls Service
↓
Service (Business Logic)
├─ Implements domain logic
├─ Orchestrates repositories
├─ Handles transactions (@Transactional)
├─ Publishes events
├─ Calls repository
↓
Repository (Data Access)
├─ JPA query methods
├─ Filters by tenant_id
├─ Returns Entity from database
↓
Response
```

### Concept 3: Dependency Injection via Constructor

**Definition:** Spring automatically injects dependencies through constructor parameters. Constructor-based injection is preferred over field-based (`@Autowired` on fields).

**Why it matters:** Constructor injection enables:
- Immutable fields (final)
- Easy testing (mock dependencies in test)
- Explicit dependency declaration (clear what service depends on)

**Real-world example:**
```java
@Service
@RequiredArgsConstructor  // Lombok generates constructor from final fields
public class PatientService {
    private final PatientRepository patientRepository;
    private final PatientEventPublisher eventPublisher;
    private final RedisCacheManager cacheManager;

    // Spring automatically injects:
    // - PatientRepository (Spring Data auto-creates proxy)
    // - PatientEventPublisher (finds @Component bean)
    // - RedisCacheManager (auto-configured from application.yml)

    public Patient getPatient(String patientId, String tenantId) {
        // Dependencies ready to use
        return patientRepository.findByIdAndTenantId(patientId, tenantId);
    }
}

// Test: Easy to inject mocks
@ExtendWith(MockitoExtension.class)
class PatientServiceTest {
    @Mock
    private PatientRepository mockRepository;

    @InjectMocks
    private PatientService service = new PatientService(
        mockRepository,
        mockEventPublisher,
        mockCache
    );

    @Test
    void shouldReturnPatient() {
        when(mockRepository.findByIdAndTenantId("123", "tenant1"))
            .thenReturn(Optional.of(testPatient));

        Patient result = service.getPatient("123", "tenant1");
        assertThat(result).isNotNull();
    }
}
```

### Concept 4: @Transactional for Transaction Boundaries

**Definition:** `@Transactional` annotation marks method as transactional. Spring wraps method execution in database transaction; commits on success, rolls back on exception.

**Why it matters:** Transactions ensure consistency. Multi-step operations (create patient, publish event, update cache) either all succeed or all fail—no partial success.

**Real-world example:**
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // Default: read-only (no transaction overhead)
public class PatientService {
    private final PatientRepository patientRepository;
    private final EventPublisher eventPublisher;

    // Read operation: read-only transaction
    public Patient getPatient(String patientId, String tenantId) {
        return patientRepository.findByIdAndTenantId(patientId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
    }

    // Write operation: full transactional
    @Transactional  // Overrides class-level readOnly=true
    public Patient createPatient(CreatePatientRequest request, String tenantId) {
        // All steps in single transaction:

        // 1. Create entity
        Patient patient = Patient.builder()
            .tenantId(tenantId)
            .firstName(request.getFirstName())
            .build();

        // 2. Persist to database (within transaction)
        patientRepository.save(patient);

        // 3. Publish event (within transaction - transactional outbox pattern)
        eventPublisher.publishPatientCreatedEvent(patient);

        // If any step fails, entire transaction rolls back
        // No orphaned patient record in database
        // No event published without corresponding patient
        return patient;
    }

    // Rollback on specific exception
    @Transactional(rollbackFor = IllegalArgumentException.class)
    public void deletePatient(String patientId, String tenantId) {
        Patient patient = patientRepository.findByIdAndTenantId(patientId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        if (patient.hasActiveCarePlans()) {
            throw new IllegalArgumentException("Cannot delete patient with active care plans");
            // Transaction rolled back; patient not deleted
        }

        patientRepository.delete(patient);
    }
}
```

### Concept 5: REST Controller and HTTP Endpoints

**Definition:** `@RestController` annotation marks class as Spring REST controller. Methods handle HTTP requests mapped via `@GetMapping`, `@PostMapping`, etc.

**Why it matters:** RESTful API is contract between services. HTTP verb (GET/POST/PUT) indicates operation type; response codes indicate success/failure.

**Real-world example:**
```java
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {
    private final PatientService patientService;

    // GET /api/v1/patients/{id}
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<PatientResponse> getPatient(
            @PathVariable String id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        PatientResponse patient = patientService.getPatient(id, tenantId);
        return ResponseEntity.ok(patient);
    }

    // POST /api/v1/patients
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PatientResponse> createPatient(
            @RequestBody CreatePatientRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        PatientResponse patient = patientService.createPatient(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(patient);
    }

    // PUT /api/v1/patients/{id}
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PatientResponse> updatePatient(
            @PathVariable String id,
            @RequestBody UpdatePatientRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        PatientResponse patient = patientService.updatePatient(id, request, tenantId);
        return ResponseEntity.ok(patient);
    }

    // DELETE /api/v1/patients/{id}
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePatient(
            @PathVariable String id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        patientService.deletePatient(id, tenantId);
        return ResponseEntity.noContent().build();
    }
}
```

---

## Architecture Pattern

### How It Works

HDIM microservices follow standard Spring Boot layered architecture:

```
┌─────────────────────────────────────────────────────────┐
│ HTTP Request from Client                                │
│ GET /api/v1/patients/P123                              │
│ X-Tenant-ID: anthem                                    │
└────────────────────┬────────────────────────────────────┘
                     │
         ┌───────────▼──────────────┐
         │ Spring DispatcherServlet │
         │ (routes HTTP requests)   │
         └───────────┬──────────────┘
                     │
         ┌───────────▼──────────────────────┐
         │ PatientController                │
         │ @GetMapping("/{id}")             │
         │ - Validates input                │
         │ - Extracts headers               │
         │ - Calls service                  │
         └───────────┬──────────────────────┘
                     │
         ┌───────────▼──────────────────────┐
         │ PatientService (@Transactional)  │
         │ - Implements business logic      │
         │ - Orchestrates dependencies      │
         │ - Manages transaction boundary   │
         │ - Calls repository               │
         └───────────┬──────────────────────┘
                     │
         ┌───────────▼──────────────────────┐
         │ PatientRepository                │
         │ @Query("SELECT p FROM...")       │
         │ WHERE tenant_id = :tenantId      │
         │ - Executes JPA query             │
         │ - Returns Entity from database   │
         └───────────┬──────────────────────┘
                     │
         ┌───────────▼──────────────────────┐
         │ PostgreSQL Database              │
         │ Liquibase migrations applied     │
         │ - patients table exists          │
         │ - Returns data row               │
         └───────────┬──────────────────────┘
                     │
         ┌───────────▼──────────────────────┐
         │ Response Building                │
         │ - Entity → PatientResponse DTO   │
         │ - Serialize to JSON              │
         │ - Set response headers           │
         │ - HTTP 200 OK                    │
         └───────────┬──────────────────────┘
                     │
         ┌───────────▼──────────────────────┐
         │ HTTP Response to Client          │
         │ {                                │
         │   "id": "P123",                 │
         │   "firstName": "John",          │
         │   "lastName": "Doe"             │
         │ }                                │
         └──────────────────────────────────┘
```

### Design Decisions

**Decision 1: Why Spring Boot instead of raw Servlet API?**
- **Trade-off:** Spring Boot adds dependency but provides framework (DI, auto-config, security). Raw Servlets require manual configuration (XML, boilerplate).
- **Rationale:** Healthcare microservices need consistency, security, observability. Spring Boot provides all three. Manual Servlet development is slower and error-prone.
- **Alternative:** Raw Servlets (not acceptable for enterprise systems).

**Decision 2: Why constructor injection instead of field injection?**
- **Trade-off:** Constructor injection is more verbose but enables immutability and explicit dependencies. Field injection is convenient but makes testing harder.
- **Rationale:** Explicit dependencies enable unit testing (pass mocks to constructor). Immutability (final fields) prevents accidental mutation.
- **Alternative:** Field injection with `@Autowired` (easier to write, harder to test).

**Decision 3: Why @Transactional at service layer?**
- **Trade-off:** Service-level transactions are explicit but don't catch all consistency issues. Repository-level transactions are granular but harder to coordinate across multiple operations.
- **Rationale:** HDIM uses transactional outbox pattern (publish event within transaction). Requires transaction at service level to ensure event published with patient record atomically.
- **Alternative:** Repository-level transactions (doesn't work for multi-repository operations).

### Trade-offs

| Aspect | Pro | Con |
|--------|-----|-----|
| **Spring Boot** | Framework, conventions, productivity | Added dependency, complexity learning curve |
| **Constructor Injection** | Explicit, testable, immutable | More verbose than field injection |
| **Layered Architecture** | Separation of concerns, testability | More files, method calls through layers |
| **@Transactional** | Transaction safety, ACID guarantees | Overhead, requires understanding transaction semantics |
| **DTOs (Response Objects)** | API contract stability, security | Extra mapping code |

---

## Implementation Guide

### Step 1: Create Spring Boot Application Class

```java
// PatientEventServiceApplication.java
@SpringBootApplication
public class PatientEventServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PatientEventServiceApplication.class, args);
    }
}
```

**That's it!** Spring Boot does the rest:
- Scans `com.healthdata.*` packages for components
- Loads `application.yml` configuration
- Starts embedded Tomcat on configured port
- Initializes datasource and Liquibase
- Enables health checks at `/actuator/health`

### Step 2: Create Entity Class

```java
@Entity
@Table(name = "patients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
```

### Step 3: Create Repository

```java
@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    @Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId AND p.id = :id")
    Optional<Patient> findByIdAndTenantId(
            @Param("id") UUID id,
            @Param("tenantId") String tenantId);

    @Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId ORDER BY p.createdAt DESC")
    List<Patient> findAllByTenantId(@Param("tenantId") String tenantId, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Patient p WHERE p.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") String tenantId);
}
```

**Spring Data automatically creates proxy:** No implementation needed. Spring generates database query from method name + @Query annotation.

### Step 4: Create Service Class

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientService {
    private final PatientRepository patientRepository;
    private final PatientEventPublisher eventPublisher;
    private final TenantContext tenantContext;

    // Read operation: readOnly transaction (no write lock)
    public PatientResponse getPatient(String patientId) {
        String tenantId = tenantContext.getCurrentTenant();
        Patient patient = patientRepository
            .findByIdAndTenantId(UUID.fromString(patientId), tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
        return mapToResponse(patient);
    }

    // Write operation: full transaction (write lock, rollback on failure)
    @Transactional
    public PatientResponse createPatient(CreatePatientRequest request) {
        String tenantId = tenantContext.getCurrentTenant();

        // Validate request
        if (request.getFirstName() == null || request.getFirstName().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }

        // Create entity
        Patient patient = Patient.builder()
            .tenantId(tenantId)
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .dateOfBirth(request.getDateOfBirth())
            .build();

        // Persist to database (within transaction)
        patientRepository.save(patient);

        // Publish event (within transaction - transactional outbox)
        eventPublisher.publishPatientCreatedEvent(patient);

        return mapToResponse(patient);
    }

    @Transactional
    public PatientResponse updatePatient(
            String patientId,
            UpdatePatientRequest request) {
        String tenantId = tenantContext.getCurrentTenant();

        Patient patient = patientRepository
            .findByIdAndTenantId(UUID.fromString(patientId), tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        // Update fields
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());

        // Persist changes (within transaction)
        patientRepository.save(patient);

        // Publish event
        eventPublisher.publishPatientUpdatedEvent(patient);

        return mapToResponse(patient);
    }

    @Transactional
    public void deletePatient(String patientId) {
        String tenantId = tenantContext.getCurrentTenant();

        Patient patient = patientRepository
            .findByIdAndTenantId(UUID.fromString(patientId), tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        patientRepository.delete(patient);
        eventPublisher.publishPatientDeletedEvent(patient);
    }

    private PatientResponse mapToResponse(Patient patient) {
        return PatientResponse.builder()
            .id(patient.getId())
            .firstName(patient.getFirstName())
            .lastName(patient.getLastName())
            .dateOfBirth(patient.getDateOfBirth())
            .createdAt(patient.getCreatedAt())
            .build();
    }
}
```

### Step 5: Create REST Controller

```java
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
@Slf4j
public class PatientController {
    private final PatientService patientService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    @Audited(eventType = "PATIENT_VIEW")
    public ResponseEntity<PatientResponse> getPatient(
            @PathVariable String id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.info("Retrieving patient: {} for tenant: {}", id, tenantId);
        PatientResponse patient = patientService.getPatient(id);
        return ResponseEntity.ok(patient);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PatientResponse> createPatient(
            @RequestBody CreatePatientRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.info("Creating patient for tenant: {}", tenantId);
        PatientResponse patient = patientService.createPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(patient);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PatientResponse> updatePatient(
            @PathVariable String id,
            @RequestBody UpdatePatientRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.info("Updating patient: {} for tenant: {}", id, tenantId);
        PatientResponse patient = patientService.updatePatient(id, request);
        return ResponseEntity.ok(patient);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePatient(
            @PathVariable String id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.info("Deleting patient: {} for tenant: {}", id, tenantId);
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }
}
```

### Step 6: Configure in application.yml

```yaml
# application.yml
spring:
  application:
    name: patient-event-service

  # Datasource configuration
  datasource:
    url: jdbc:postgresql://hdim-postgres:5435/patient_db
    username: healthdata
    password: ${DB_PASSWORD:password}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

  # JPA configuration
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQL10Dialect
    hibernate:
      ddl-auto: validate  # ✅ REQUIRED: Validate only, never update
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true

  # Liquibase configuration
  liquibase:
    enabled: true
    change-log: classpath:/db/changelog/db.changelog-master.xml
    default-schema: public

  # Redis caching
  redis:
    host: hdim-redis
    port: 6379
    database: 0
    timeout: 2000ms
    jedis:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0

  # Kafka configuration
  kafka:
    bootstrap-servers: hdim-kafka:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: patient-event-service
      auto-offset-reset: earliest

# Server configuration
server:
  port: 8084
  servlet:
    context-path: /
  error:
    include-message: always
    include-binding-errors: always

# Logging
logging:
  level:
    root: INFO
    com.healthdata: DEBUG
    org.hibernate: WARN
    org.springframework.security: DEBUG
    org.liquibase: INFO

# Actuator (health, metrics, observability)
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,info
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

---

## Real-World Examples from HDIM

### Example 1: Patient Event Service

**Where:** `backend/modules/services/patient-event-service/`

**What it does:** REST API for patient data; stores patient demographics, publishes PatientCreatedEvent/PatientUpdatedEvent.

**Architecture:**
- Controller: `PatientController` (endpoints)
- Service: `PatientService` (business logic)
- Repository: `PatientRepository` (data access)
- Entity: `Patient` (JPA entity)
- Event: `PatientCreatedEvent` (domain event)

**Key patterns:**
- Constructor injection (Lombok `@RequiredArgsConstructor`)
- Service-level transactions (`@Transactional`)
- Multi-tenant filtering in repository
- Event publishing within transaction
- REST endpoints with `@PreAuthorize` for authorization

### Example 2: Quality Measure Service

**Where:** `backend/modules/services/quality-measure-event-service/`

**What it does:** REST API for HEDIS quality measures; evaluates measures, stores results, publishes MeasureEvaluatedEvent.

**Key patterns:**
- Complex business logic in service layer (CQL evaluation)
- Multiple repositories coordinated in service
- Event publishing on measure evaluation
- Caching of measure definitions (`@Cacheable`)
- Async event consumption (`@KafkaListener`)

### Example 3: Care Gap Service

**Where:** `backend/modules/services/care-gap-event-service/`

**What it does:** REST API for care gaps; consumes MeasureEvaluatedEvents, creates care gaps, publishes CareGapDetectedEvent.

**Key patterns:**
- Event-driven architecture (listens to Kafka events)
- Service coordinates multiple domain entities
- Transactional consistency between care gap creation and event publication
- REST endpoints to query care gaps by tenant

---

## Best Practices

### ✅ DO's

- ✅ **DO use constructor injection with @RequiredArgsConstructor**
  - Why: Makes dependencies explicit; enables testing with mocks
  - Example: `private final PatientRepository patientRepository;`

- ✅ **DO make service methods @Transactional**
  - Why: Ensures transaction boundaries; enables rollback on failure
  - Example: `@Transactional public PatientResponse createPatient(...)`

- ✅ **DO use @Query for complex queries**
  - Why: Explicit query is visible for code review; easy to verify tenant scoping
  - Example: `@Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId AND p.id = :id")`

- ✅ **DO create DTOs for API responses**
  - Why: Decouples API contract from entity definition; hides internal fields
  - Example: `PatientResponse` DTO instead of returning `Patient` entity

- ✅ **DO add @PreAuthorize on endpoints**
  - Why: Enforces authorization; prevents unauthorized access
  - Example: `@PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")`

- ✅ **DO use application.yml for configuration**
  - Why: Externalize config from code; different values per environment
  - Example: `spring.datasource.url: ${DATASOURCE_URL:localhost}`

- ✅ **DO set Hibernate ddl-auto to validate**
  - Why: Fail-fast if entity doesn't match database schema
  - Example: `spring.jpa.hibernate.ddl-auto: validate`

- ✅ **DO publish events within service transaction**
  - Why: Ensures consistency (patient created AND event published atomically)
  - Example: Event published within `@Transactional` method

- ✅ **DO use Spring Data repository methods**
  - Why: JPA abstracts database operations; easy to test with mocks
  - Example: `patientRepository.findByIdAndTenantId(...)`

- ✅ **DO add logging at service boundaries**
  - Why: Debugging and auditing (who called what service when)
  - Example: `log.info("Creating patient for tenant: {}", tenantId);`

### ❌ DON'Ts

- ❌ **DON'T use field injection (@Autowired on fields)**
  - Why: Hard to test (can't inject mocks in constructor); hides dependencies
  - Example: ❌ `@Autowired private PatientRepository repo;`

- ❌ **DON'T call repositories directly from controller**
  - Why: Skips business logic layer; violates layered architecture
  - Example: ❌ Controller directly calling `patientRepository.save()`

- ❌ **DON'T forget @Transactional on write operations**
  - Why: No transaction boundary; partial success possible
  - Example: ❌ Service method without `@Transactional` that modifies database

- ❌ **DON'T return entity objects from API endpoints**
  - Why: Exposes internal structure; couples API to entity definition
  - Example: ❌ `@GetMapping public ResponseEntity<Patient> getPatient()`

- ❌ **DON'T skip multi-tenant filtering in repository**
  - Why: Data leak! Other tenants can access your data
  - Example: ❌ `findById(UUID id)` without tenant filter

- ❌ **DON'T put business logic in controller**
  - Why: Logic is untestable; mixed responsibilities
  - Example: ❌ Complex calculation in controller method

- ❌ **DON'T use Hibernate's ddl-auto: update or create**
  - Why: Loses auditability; destructive changes
  - Example: ❌ `spring.jpa.hibernate.ddl-auto: update`

- ❌ **DON'T ignore exceptions**
  - Why: Hides failures; complicates debugging
  - Example: ❌ `try { ... } catch (Exception e) { }`

- ❌ **DON'T cache sensitive data without tenant namespace**
  - Why: Cache pollution (tenant A sees tenant B's data)
  - Example: ❌ `@Cacheable(key = "'patient:' + #id")` without tenant

- ❌ **DON'T publish events outside transaction**
  - Why: Event published but entity not persisted (or vice versa)
  - Example: ❌ Event publisher called outside `@Transactional` method

---

## Testing Strategies

### Unit Testing: Service Layer

```java
@ExtendWith(MockitoExtension.class)
class PatientServiceTest {
    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PatientEventPublisher eventPublisher;

    @InjectMocks
    private PatientService service;

    @Test
    void shouldReturnPatient_WhenExists() {
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
        PatientResponse result = service.getPatient(patientId.toString());

        // ASSERT
        assertThat(result.getId()).isEqualTo(patientId);
        assertThat(result.getFirstName()).isEqualTo("John");

        TenantContext.clear();
    }

    @Test
    void shouldThrowNotFound_WhenPatientDoesNotExist() {
        // ARRANGE
        UUID patientId = UUID.randomUUID();
        String tenantId = "anthem";

        when(patientRepository.findByIdAndTenantId(patientId, tenantId))
            .thenReturn(Optional.empty());

        TenantContext.setCurrentTenant(tenantId);

        // ACT & ASSERT
        assertThatThrownBy(() -> service.getPatient(patientId.toString()))
            .isInstanceOf(ResourceNotFoundException.class);

        TenantContext.clear();
    }

    @Test
    void shouldCreatePatient_AndPublishEvent() {
        // ARRANGE
        String tenantId = "anthem";
        CreatePatientRequest request = CreatePatientRequest.builder()
            .firstName("Jane")
            .lastName("Smith")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();

        when(patientRepository.save(any(Patient.class)))
            .thenReturn(Patient.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .firstName("Jane")
                .lastName("Smith")
                .build());

        TenantContext.setCurrentTenant(tenantId);

        // ACT
        PatientResponse result = service.createPatient(request);

        // ASSERT
        assertThat(result.getFirstName()).isEqualTo("Jane");

        // Verify event was published
        verify(eventPublisher, times(1))
            .publishPatientCreatedEvent(any(Patient.class));

        TenantContext.clear();
    }
}
```

### Integration Testing: Controller and Database

```java
@SpringBootTest
@AutoConfigureMockMvc
class PatientControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @BeforeEach
    void setUp() {
        patientRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreatePatient() throws Exception {
        CreatePatientRequest request = CreatePatientRequest.builder()
            .firstName("John")
            .lastName("Doe")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();

        mockMvc.perform(post("/api/v1/patients")
                .header("X-Tenant-ID", "anthem")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.firstName").value("John"))
            .andExpect(jsonPath("$.lastName").value("Doe"));

        // Verify persisted to database
        List<Patient> patients = patientRepository.findAll();
        assertThat(patients).hasSize(1);
        assertThat(patients.get(0).getFirstName()).isEqualTo("John");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldNotReturnPatient_FromDifferentTenant() throws Exception {
        // Create patient for Anthem
        Patient anthemPatient = Patient.builder()
            .tenantId("anthem")
            .firstName("John")
            .lastName("Doe")
            .build();
        patientRepository.save(anthemPatient);

        // Try to retrieve as Blue Cross
        mockMvc.perform(get("/api/v1/patients/" + anthemPatient.getId())
                .header("X-Tenant-ID", "bluecross"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnUnauthorized_WhenNoRole() throws Exception {
        mockMvc.perform(post("/api/v1/patients")
                .header("X-Tenant-ID", "anthem")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }
}
```

### Test Checklist

- [ ] Service layer: Unit tests for all public methods (mocked dependencies)
- [ ] Controller layer: Integration tests for all endpoints (MockMvc)
- [ ] Database: Integration tests verify data persistence
- [ ] Multi-tenant: Unit/integration tests verify cross-tenant isolation
- [ ] Authorization: Tests verify @PreAuthorize enforcement
- [ ] Transactions: Test rollback on exception
- [ ] Events: Test event publication within transaction
- [ ] Error handling: Tests verify exception handling and status codes

---

## Troubleshooting

### Common Issues

#### Issue 1: "Could Not Autowire" Bean Not Found

**Symptoms:**
- Application fails to start: `NoSuchBeanDefinitionException: No qualifying bean of type 'PatientRepository'`
- Dependency not injected
- NullPointerException when trying to use dependency

**Root cause:** Spring cannot find bean to inject. Either:
- Class missing `@Component`, `@Service`, `@Repository` annotation
- Classpath scanning not finding component (wrong package)
- Bean definition has configuration issue

**Solution:**
```bash
# Step 1: Verify annotation exists
# ❌ WRONG: Missing @Service
class PatientService {

# ✅ RIGHT: Has @Service
@Service
class PatientService {

# Step 2: Verify classpath scanning includes package
# @SpringBootApplication scans com.healthdata.* by default
# If component is in different package, add to @ComponentScan:
@SpringBootApplication
@ComponentScan(basePackages = {"com.healthdata", "com.other"})

# Step 3: Check application logs
docker compose logs -f patient-event-service | grep -i "bean\|autowire"
```

**Prevention:** Every dependency-injected class needs `@Component` (or `@Service`, `@Repository`, `@Controller`).

#### Issue 2: "No Qualifying Bean of Type: Multiple Beans Found"

**Symptoms:**
- Application fails: `NoUniqueBeanDefinitionException: No qualifying bean of type available: expected single matching bean but found 2`
- Multiple implementations of same interface
- Spring can't decide which bean to inject

**Root cause:** Multiple beans of same type exist. Spring doesn't know which one to inject.

**Solution:**
```java
// ❌ WRONG: Multiple PatientService beans, Spring confused
@Service
public class PatientService {
}

@Service
public class PatientServiceV2 {
}

// ✅ RIGHT: Use @Primary or @Qualifier
@Service
@Primary  // Use this one by default
public class PatientService {
}

@Service
public class PatientServiceV2 {
}

// Or in injection point:
@Service
public class PatientController {
    @Qualifier("patientService")  // Explicitly specify which bean
    private PatientService service;
}
```

**Prevention:** Avoid duplicate bean names; use `@Primary` or `@Qualifier` when multiple implementations exist.

#### Issue 3: "LazyInitializationException: Could Not Initialize Proxy"

**Symptoms:**
- Error at runtime: `LazyInitializationException: could not initialize proxy – no Session`
- Accessing lazy-loaded associations after transaction ended
- Works with `@Transactional`, fails without it

**Root cause:** Hibernate lazy-loads associations. If entity accessed outside transaction context, Hibernate cannot load the association (no database session).

**Solution:**
```java
// ❌ WRONG: Lazy-loading outside transaction
@Service
public class PatientService {
    @Autowired
    private PatientRepository patientRepository;

    public Patient getPatient(String patientId) {
        Patient patient = patientRepository.findById(patientId);
        // Transaction ends here
        // If accessing patient.getObservations() here, lazy-load fails
        return patient;
    }
}

// ✅ RIGHT: Wrap in @Transactional
@Service
@Transactional(readOnly = true)
public class PatientService {
    public Patient getPatient(String patientId) {
        Patient patient = patientRepository.findById(patientId);
        // Still within transaction
        // Can access patient.getObservations() here (lazy-loaded)
        return patient;
    }
}

// Or use eager loading:
@Entity
public class Patient {
    @OneToMany(fetch = FetchType.EAGER)
    private List<Observation> observations;
}
```

**Prevention:** Ensure entity access happens within `@Transactional` context; use eager loading if needed.

---

## References & Resources

### HDIM Documentation

- [Multi-Tenant Architecture](../01-architecture/multi-tenant-architecture.md) - Tenant scoping in application layer
- [PostgreSQL + Liquibase](../04-data-persistence/postgresql-liquibase.md) - Database and schema management
- [HIPAA Compliance](../03-security-compliance/hipaa-compliance.md) - Security requirements in controllers/services
- [CQRS + Event Sourcing](../01-architecture/cqrs-event-sourcing.md) - Event publishing patterns

### External Resources

- **[Spring Boot Documentation](https://spring.io/projects/spring-boot)** - Official Spring Boot reference
- **[Spring Data JPA](https://spring.io/projects/spring-data-jpa)** - Repository pattern documentation
- **[Spring Security](https://spring.io/projects/spring-security)** - Authorization framework
- **[Baeldung Spring Tutorials](https://www.baeldung.com/spring-boot)** - Practical Spring Boot guides

### Related Skills

- **Prerequisite:** PostgreSQL + Liquibase (entities persist to database)
- **Complement:** Multi-Tenant Architecture (tenant scoping at application layer)
- **Advanced:** Spring Security & RBAC (authorization patterns)

---

## Quick Reference Checklist

### Before Creating Service

- [ ] Understand business logic that service implements
- [ ] Identify which repositories service needs
- [ ] Know what events service publishes
- [ ] Understand multi-tenant isolation requirements
- [ ] Design request/response DTOs

### While Implementing Service

- [ ] Service has `@Service` annotation
- [ ] Dependencies injected via constructor (Lombok `@RequiredArgsConstructor`)
- [ ] All final fields (immutable, safer)
- [ ] Class-level `@Transactional(readOnly = true)` for read-heavy services
- [ ] Write operations override with `@Transactional` (no readOnly)
- [ ] Repository queries filter by tenant_id
- [ ] Business logic throws specific exceptions (not generic Exception)
- [ ] Events published within transaction
- [ ] Service tested with mocked dependencies (unit tests)

### While Implementing Controller

- [ ] Controller has `@RestController` and `@RequestMapping`
- [ ] All endpoints have `@PreAuthorize` authorization
- [ ] HTTP verbs correct (GET, POST, PUT, DELETE)
- [ ] Request headers extracted (`@RequestHeader("X-Tenant-ID")`)
- [ ] Path variables mapped (`@PathVariable`)
- [ ] Request body deserialized (`@RequestBody`)
- [ ] Response wrapped in `ResponseEntity` with correct status code
- [ ] DTOs used instead of entities returned directly
- [ ] Controller tested with MockMvc (integration tests)

### After Implementation

- [ ] All unit tests pass (service layer with mocks)
- [ ] All integration tests pass (controller + database)
- [ ] Code review verifies tenant scoping
- [ ] EntityMigrationValidationTest passes
- [ ] Application.yml configured for environment
- [ ] Ready for deployment

---

## Key Takeaways

1. **Layered Architecture Works:** Controller → Service → Repository separation enables testing each layer independently and together. Service layer is where business logic lives.

2. **Constructor Injection is Better:** Constructor injection makes dependencies explicit and enables testing with mocks. Field injection is convenient but harder to test.

3. **Transactions Matter:** `@Transactional` ensures consistency. Multi-step operations (create entity, publish event) either all succeed or all fail—no partial success.

4. **Multi-Tenancy at Application Layer:** Controllers extract X-Tenant-ID header; services pass to repositories; repositories filter queries by tenant. Must be consistent throughout.

5. **DTOs Decouple API:** Return response DTOs instead of entities. Protects internal structure from API changes. API contract stays stable while entity evolves.

---

## FAQ

**Q: Why use DTOs instead of returning entities directly?**
A: DTOs (Data Transfer Objects) decouple API contract from entity definition. If entity adds field, API doesn't expose it (unless DTO updated). Provides security (hide internal fields) and stability (API contract doesn't change when entity changes).

**Q: Can I skip @Transactional if I'm only doing reads?**
A: You can (readOnly doesn't acquire write lock). But include `@Transactional(readOnly = true)` anyway for consistency and to catch if someone later adds writes to method.

**Q: How do I test service with database calls?**
A: Use `@SpringBootTest` + `@AutoConfigureMockMvc` + `MockMvc`. Spring Boot starts test database (in-memory if configured), Liquibase applies migrations, tests run against real database.

**Q: What if my service needs to call another service?**
A: Inject the other service as dependency. If it's in different deployment, use RestTemplate or WebClient (HTTP calls). If same deployment, direct injection works.

**Q: How do I handle validation errors?**
A: Throw specific exception (IllegalArgumentException, ValidationException). Spring translates to HTTP 400. Use @ControllerAdvice to define global exception handler that converts exceptions to HTTP status codes.

---

## Next Steps

After completing this guide:

1. **Practice:** Build complete CRUD service (create, read, update, delete)
2. **Test:** Write unit tests (mocked) and integration tests (real database)
3. **Review:** Have peer review service for layered architecture compliance
4. **Learn:** Move to [Spring Security & RBAC](./spring-security-rbac.md) for authorization patterns
5. **Deploy:** Containerize and deploy service to Docker Compose

---

**← Previous Guide:** [PostgreSQL + Liquibase](../04-data-persistence/postgresql-liquibase.md)
**Skills Hub:** [Skills Center](../README.md)
**Next Guide:** [Spring Security & RBAC](./spring-security-rbac.md)

---

**Last Updated:** January 20, 2026
**Version:** 1.0
**Difficulty Level:** ⭐⭐⭐ (3/5 stars - Core concepts)
**Time Investment:** 1.5-2 weeks
**Prerequisite Skills:** PostgreSQL + Liquibase, Java 21 basics, REST API concepts
**Related Skills:** Multi-Tenant Architecture, Spring Security & RBAC, Unit Testing, Integration Testing

---

**← [Skills Hub](../README.md)** | **→ [Next: Spring Security & RBAC](./spring-security-rbac.md)**
