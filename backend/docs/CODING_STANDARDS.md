# Coding Standards & Conventions

---
**Navigation:** [CLAUDE.md](../../CLAUDE.md#coding-patterns--conventions) | [Documentation Portal](../../docs/README.md) | [Backend Docs Index](./README.md)
---

## Overview

This guide establishes the coding patterns and conventions for HDIM microservices. All services follow the same package structure, layer conventions, and design patterns to ensure consistency, maintainability, and interoperability.

**Core Principle:** Layered architecture with clear separation of concerns - Controllers (REST API), Services (Business Logic), Repositories (Data Access), Entities (Domain Models).

---

## Package Structure

### Standard Service Layout

Every microservice follows this package structure:

```
com.healthdata.{service}/
├── api/                    # REST controllers & DTOs
│   ├── v1/                 # API versioning (v1, v2, etc.)
│   ├── dto/                # Request/Response DTOs
│   └── controller/         # REST controllers
├── application/            # Application services (use cases)
├── domain/                 # Domain models, business logic
│   ├── model/              # Entity classes
│   ├── repository/         # Repository interfaces
│   └── exception/          # Domain exceptions
├── infrastructure/         # External integrations
│   ├── persistence/        # Repository implementations
│   ├── messaging/          # Kafka producers/consumers
│   └── external/           # Third-party API clients
└── config/                 # Spring configuration beans
```

### Package Naming Conventions

| Package | Purpose | Examples |
|---------|---------|----------|
| `api` | REST layer | Controllers, DTOs, exception handlers |
| `application` | Business layer | Services, use cases, orchestration |
| `domain` | Domain layer | Entities, value objects, domain logic |
| `infrastructure` | Infrastructure layer | Persistence, messaging, external services |
| `config` | Configuration | Spring beans, filter chains, converters |

---

## Layer Responsibilities

### API Layer (Controllers & DTOs)

Handles HTTP request/response communication.

**Responsibilities:**
- Parse HTTP requests
- Validate input (using `@Validated`)
- Call application services
- Return HTTP responses
- Handle cross-cutting concerns (CORS, versioning)

**NOT responsible for:**
- Business logic
- Database operations
- Transactions

### Application Layer (Services)

Orchestrates business operations and coordinates between layers.

**Responsibilities:**
- Implement use cases
- Coordinate repositories and external services
- Manage transactions (`@Transactional`)
- Perform validation and transformation
- Handle audit logging

**NOT responsible for:**
- HTTP handling
- Database implementation details
- Persistence logic

### Domain Layer (Entities)

Represents core business concepts and enforces invariants.

**Responsibilities:**
- Define entities with business rules
- Enforce domain invariants
- Implement value objects
- Define repository interfaces (not implementations)

**NOT responsible for:**
- HTTP handling
- Database implementation
- Transaction management

### Infrastructure Layer (Persistence & Messaging)

Implements technical concerns and external integrations.

**Responsibilities:**
- Implement repository interfaces (JPA)
- Kafka producers and consumers
- External API clients
- Database queries
- Message serialization

**NOT responsible for:**
- Business logic
- Validation (except format validation)
- Transaction management (delegated to service layer)

---

## Controller Pattern

### Basic Structure

```java
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
@Validated
public class PatientController {

    private final PatientService patientService;

    @GetMapping("/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'VIEWER')")
    @Audited(eventType = "PATIENT_ACCESS")
    public ResponseEntity<PatientResponse> getPatient(
            @PathVariable String patientId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(patientService.getPatient(patientId, tenantId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(eventType = "PATIENT_CREATE")
    public ResponseEntity<PatientResponse> createPatient(
            @Valid @RequestBody CreatePatientRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        PatientResponse response = patientService.createPatient(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{patientId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(eventType = "PATIENT_UPDATE")
    public ResponseEntity<PatientResponse> updatePatient(
            @PathVariable String patientId,
            @Valid @RequestBody UpdatePatientRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(patientService.updatePatient(patientId, request, tenantId));
    }

    @DeleteMapping("/{patientId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(eventType = "PATIENT_DELETE")
    public ResponseEntity<Void> deletePatient(
            @PathVariable String patientId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        patientService.deletePatient(patientId, tenantId);
        return ResponseEntity.noContent().build();
    }
}
```

### Decorators & Annotations

| Decorator | Purpose |
|-----------|---------|
| `@RestController` | Marks class as REST controller (returns JSON by default) |
| `@RequestMapping("/api/v1/patients")` | Base URL path for all endpoints |
| `@RequiredArgsConstructor` | Constructor injection (Lombok) |
| `@Validated` | Enable method-level validation |
| `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` | HTTP method mappings |
| `@PathVariable` | Extract variable from URL path |
| `@RequestBody` | Deserialize request body to object |
| `@Valid` | Trigger JSR-380 validation |
| `@PreAuthorize` | Role-based access control |
| `@Audited` | Log access for compliance (PHI tracking) |
| `@RequestHeader` | Extract HTTP header (especially "X-Tenant-ID") |

### Request/Response DTOs

Keep DTOs simple - they're just data carriers.

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientResponse {
    private String id;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String status;
    private Instant createdAt;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePatientRequest {
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;
}
```

### HTTP Status Codes

| Code | Use Case | Example |
|------|----------|---------|
| 200 OK | Successful GET/PUT/PATCH | `return ResponseEntity.ok(response);` |
| 201 Created | Successful POST | `return ResponseEntity.status(HttpStatus.CREATED).body(response);` |
| 204 No Content | Successful DELETE | `return ResponseEntity.noContent().build();` |
| 400 Bad Request | Invalid input | Thrown by `@Valid` validation |
| 401 Unauthorized | Missing/invalid JWT | Handled by `GatewayAuthenticationFilter` |
| 403 Forbidden | Insufficient permissions | `@PreAuthorize` denial |
| 404 Not Found | Resource doesn't exist | Throw `ResourceNotFoundException` |
| 500 Internal Server Error | Unexpected error | Caught by `GlobalExceptionHandler` |

---

## Service Pattern

### Application Service Structure

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;
    private final AuditService auditService;
    private final Tracer tracer;  // OpenTelemetry

    // READ operation (no transaction needed - readOnly=true at class level)
    public PatientResponse getPatient(String patientId, String tenantId) {
        return patientRepository.findByIdAndTenant(patientId, tenantId)
            .map(patientMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
    }

    // WRITE operation (needs explicit @Transactional)
    @Transactional
    public PatientResponse createPatient(CreatePatientRequest request, String tenantId) {
        // Validate business rules
        if (isDuplicateMRN(request.getMrn(), tenantId)) {
            throw new ValidationException("MRN already exists in tenant: " + request.getMrn());
        }

        // Create entity
        Patient patient = Patient.builder()
            .tenantId(tenantId)
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .dateOfBirth(request.getDateOfBirth())
            .status("ACTIVE")
            .build();

        // Persist
        Patient saved = patientRepository.save(patient);

        // Audit
        auditService.logPatientCreation(saved.getId(), tenantId);

        return patientMapper.toResponse(saved);
    }

    // READ with custom query
    public List<PatientResponse> searchPatients(PatientSearchCriteria criteria, String tenantId) {
        return patientRepository.search(criteria, tenantId)
            .stream()
            .map(patientMapper::toResponse)
            .collect(Collectors.toList());
    }

    private boolean isDuplicateMRN(String mrn, String tenantId) {
        return patientRepository.existsByMrnAndTenant(mrn, tenantId);
    }
}
```

### Key Principles

1. **Inject, don't create:** Use constructor injection via `@RequiredArgsConstructor`
2. **Single Responsibility:** One service = one use case or domain concept
3. **Fail fast:** Validate business rules early, throw specific exceptions
4. **Transactional boundaries:** Mark write methods with `@Transactional`
5. **Immutability:** Use `@Data @Builder` to ensure predictable object construction

---

## Entity Pattern

### Domain Entity Structure

```java
@Entity
@Table(name = "patients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "mrn", nullable = false, unique = true)
    private String mrn;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // Relationships
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Encounter> encounters = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    // Lifecycle hooks
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        status = "ACTIVE";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Domain methods (business logic)
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public void merge(Patient other) {
        if (!this.tenantId.equals(other.tenantId)) {
            throw new TenantAccessDeniedException(other.tenantId);
        }
        this.mrn = other.getMrn();
        this.status = "MERGED";
    }
}
```

### Annotations Explained

| Annotation | Purpose |
|-----------|---------|
| `@Entity` | JPA entity class |
| `@Table(name = "...")` | Explicitly map to table name |
| `@Id` | Primary key field |
| `@GeneratedValue(strategy = GenerationType.UUID)` | Auto-generate UUID |
| `@Column(...)` | Configure column properties |
| `@Column(nullable = false)` | NOT NULL constraint |
| `@Column(unique = true)` | UNIQUE constraint |
| `@Column(updatable = false)` | Immutable after insert |
| `@PrePersist` | Hook before insert |
| `@PreUpdate` | Hook before update |
| `@OneToMany` / `@ManyToOne` / `@ManyToMany` | Relationships |
| `@JoinColumn` | Foreign key configuration |
| `@FetchType.LAZY` | Load on access (usually preferred) |
| `@FetchType.EAGER` | Load immediately (use sparingly) |
| `@CascadeType.ALL` | Cascade operations to related entities |

### Design Guidelines

1. **Use UUIDs for IDs:** Better for distributed systems, `GenerationType.UUID`
2. **Include tenant_id in all multi-tenant tables:** Required for isolation
3. **Always include created_at and updated_at:** Audit trail and sorting
4. **Use `@Column(updatable = false)` for created_at:** Prevent accidental changes
5. **Use `@FetchType.LAZY` by default:** Load relationships only when accessed
6. **Include domain methods:** Business logic belongs in entities (DDD principle)
7. **Don't use entity inheritance:** Use composition instead (simpler to map to tables)

---

## Repository Pattern

### Repository Interface (Domain Layer)

```java
@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    // Auto-implemented by Spring Data JPA
    Optional<Patient> findById(UUID id);
    List<Patient> findAll();
    List<Patient> findByStatus(String status);

    // Custom queries for domain concepts
    Optional<Patient> findByIdAndTenant(UUID id, String tenantId);

    @Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId AND p.mrn = :mrn")
    Optional<Patient> findByMrnAndTenant(@Param("mrn") String mrn, @Param("tenantId") String tenantId);

    @Query("SELECT COUNT(p) FROM Patient p WHERE p.tenantId = :tenantId AND p.mrn = :mrn")
    boolean existsByMrnAndTenant(@Param("mrn") String mrn, @Param("tenantId") String tenantId);

    @Query("""
        SELECT p FROM Patient p
        WHERE p.tenantId = :tenantId
        AND p.status = 'ACTIVE'
        AND p.dateOfBirth > :startDate
        ORDER BY p.createdAt DESC
        """)
    List<Patient> findActivePatientsSince(@Param("tenantId") String tenantId,
                                         @Param("startDate") LocalDate startDate);

    // Pagination support
    Page<Patient> findByTenantId(String tenantId, Pageable pageable);
}
```

### Key Principles

1. **Always filter by tenant_id:** Multi-tenant applications must isolate data
2. **Use @Query for complex queries:** More readable and maintainable
3. **Return Optional for single results:** Cleaner null handling
4. **Return List for multiple results:** More common than Stream
5. **Support pagination:** `Page<T>` for large result sets
6. **Use named parameters:** `@Param` for clarity

---

## Exception Handling

### Custom Exception Hierarchy

```java
// Base exception
public class HdimException extends RuntimeException {
    public HdimException(String message) {
        super(message);
    }

    public HdimException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Domain exceptions (specific, actionable)
public class ResourceNotFoundException extends HdimException {
    public ResourceNotFoundException(String resourceType, String id) {
        super(String.format("%s not found: %s", resourceType, id));
    }
}

public class ValidationException extends HdimException {
    public ValidationException(String message) {
        super(message);
    }
}

public class TenantAccessDeniedException extends HdimException {
    public TenantAccessDeniedException(String tenantId) {
        super("Access denied to tenant: " + tenantId);
    }
}

public class ConflictException extends HdimException {
    public ConflictException(String message) {
        super(message);
    }
}

// Checked exception (rare, use sparingly)
public class ExternalServiceException extends HdimException {
    public ExternalServiceException(String service, String message) {
        super(String.format("External service error [%s]: %s", service, message));
    }
}
```

### Global Exception Handler

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(TenantAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleTenantAccess(TenantAccessDeniedException ex) {
        log.error("Tenant access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse(ex.getMessage(), HttpStatus.FORBIDDEN.value()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorResponse {
        private String message;
        private int status;
        private Instant timestamp = Instant.now();
    }
}
```

### Exception Guidelines

**When to throw custom exceptions:**
- ✅ Domain validation failures (ValidationException)
- ✅ Resource not found (ResourceNotFoundException)
- ✅ Multi-tenant isolation violations (TenantAccessDeniedException)
- ✅ Concurrent modification (ConflictException)

**When to log and handle:**
- ✅ Null pointer → Indicates programming error, log and throw
- ✅ IOException from external service → Wrap in HdimException
- ✅ Database connection lost → Wrap and throw
- ✅ HTTP client errors → Wrap with context

**When NOT to throw:**
- ❌ Control flow exceptions (never)
- ❌ Checked exceptions (unless from external libraries)
- ❌ Generic Exception (use specific subclasses)

---

## Validation

### Input Validation

```java
// DTO with JSR-380 annotations
@Data
public class CreatePatientRequest {
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 100, message = "First name must be 2-100 characters")
    private String firstName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "MRN is required")
    @Pattern(regexp = "^[A-Z0-9]{8,12}$", message = "MRN must be 8-12 alphanumeric characters")
    private String mrn;

    @Email(message = "Email must be valid")
    private String email;

    @Min(value = 0, message = "Age must be non-negative")
    @Max(value = 150, message = "Age must be less than 150")
    private Integer age;
}

// Controller uses @Valid to trigger validation
@PostMapping
public ResponseEntity<PatientResponse> createPatient(
        @Valid @RequestBody CreatePatientRequest request,
        @RequestHeader("X-Tenant-ID") String tenantId) {
    // If validation fails, MethodArgumentNotValidException is thrown
    // and handled by GlobalExceptionHandler
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(patientService.createPatient(request, tenantId));
}
```

### Business Logic Validation

```java
// Service layer validates business rules
@Service
public class PatientService {
    @Transactional
    public PatientResponse createPatient(CreatePatientRequest request, String tenantId) {
        // Check unique constraint (business rule)
        if (patientRepository.existsByMrnAndTenant(request.getMrn(), tenantId)) {
            throw new ValidationException(
                "Patient with MRN " + request.getMrn() + " already exists");
        }

        // Check cross-service rule (if needed)
        if (!organizationService.exists(request.getOrganizationId(), tenantId)) {
            throw new ValidationException("Organization not found");
        }

        // Proceed with creation
        Patient patient = patientMapper.toEntity(request);
        return patientMapper.toResponse(patientRepository.save(patient));
    }
}
```

---

## Related Guides

- [HIPAA Compliance](../../CLAUDE.md#critical-hipaa-compliance-requirements) - PHI handling requirements
- [Authentication & Authorization](../../CLAUDE.md#authentication--authorization) - Security patterns
- [Testing Standards](../../testing/TESTING_STANDARDS.md) - Unit and integration tests
- [Code Review Checklist](../../testing/CODE_REVIEW_CHECKLIST.md) - Pre-commit validation

---

_Last Updated: January 19, 2026_
_Version: 1.0_