# HDIM Coding Standards & Best Practices

> **This is the comprehensive guide for code quality, consistency, and patterns across HDIM.**
> **These standards ensure production-ready, maintainable, and secure code.**

---

## Overview

### What is This Skill?

Coding standards define how to structure, name, and organize code. They ensure consistency across 51 services, making code reviews faster and onboarding easier.

**Example:**
```java
// ✅ HDIM Standard
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientService {
    private final PatientRepository patientRepository;

    public PatientResponse getPatient(String patientId, String tenantId) {
        return patientRepository
            .findByIdAndTenantId(patientId, tenantId)
            .map(this::mapToResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
    }
}

// ❌ Non-Standard
public class PatientService {
    private PatientRepository repo;

    public PatientService(PatientRepository repository) {
        this.repo = repository;
    }

    public Patient getPatient(String patientId) {  // Missing tenant check
        return repo.findById(patientId).get();  // No null handling
    }
}
```

### Why is This Important for HDIM?

Healthcare systems have strict compliance and reliability requirements:

- **Code Reviews:** Clear standards reduce review time from 30 min to 5 min
- **Production Reliability:** Consistent patterns prevent common bugs
- **HIPAA Compliance:** Standards enforce PHI protection
- **Maintainability:** Developers can quickly understand unfamiliar code

### Business Impact

- **Time to Delivery:** 80% faster code reviews with clear standards
- **Defect Reduction:** 60% fewer bugs with consistent patterns
- **Knowledge Transfer:** New developers productive in 1 week (not 1 month)
- **Compliance:** Automated checks ensure HIPAA enforcement

### Estimated Learning Time

2-3 weeks (hands-on implementation, code review feedback)

---

## File Organization

### Directory Structure

```
backend/
├── modules/
│   └── services/
│       └── patient-service/
│           ├── build.gradle.kts
│           └── src/
│               ├── main/
│               │   ├── java/com/healthdata/patient/
│               │   │   ├── PatientServiceApplication.java
│               │   │   ├── controller/
│               │   │   │   └── PatientController.java
│               │   │   ├── service/
│               │   │   │   └── PatientService.java
│               │   │   ├── repository/
│               │   │   │   └── PatientRepository.java
│               │   │   ├── domain/
│               │   │   │   └── Patient.java
│               │   │   ├── dto/
│               │   │   │   ├── PatientResponse.java
│               │   │   │   └── CreatePatientRequest.java
│               │   │   ├── exception/
│               │   │   │   └── ResourceNotFoundException.java
│               │   │   └── config/
│               │   │       └── SecurityConfig.java
│               │   └── resources/
│               │       ├── application.yml
│               │       └── db/changelog/
│               │           └── db.changelog-master.xml
│               └── test/
│                   ├── java/com/healthdata/patient/
│                   │   ├── PatientServiceTest.java
│                   │   └── PatientControllerIntegrationTest.java
│                   └── resources/
│                       └── application-test.yml
```

### Layer Responsibilities

| Layer | Responsibility | Example |
|-------|-----------------|---------|
| **Controller** | HTTP request/response | `@PostMapping` endpoint |
| **Service** | Business logic | Measure evaluation |
| **Repository** | Data access | Database queries |
| **Domain** | Entity model | JPA `@Entity` |
| **DTO** | Data transfer | Request/response objects |
| **Config** | Configuration | Spring beans |
| **Exception** | Error handling | Custom exceptions |

---

## Naming Conventions

### Classes

```java
// ✅ Domain entities
public class Patient { }              // Singular, matches database table

// ✅ Services
public class PatientService { }       // Suffix: Service

// ✅ Controllers
public class PatientController { }    // Suffix: Controller

// ✅ Repositories
public interface PatientRepository { }    // Suffix: Repository

// ✅ Request/Response DTOs
public class CreatePatientRequest { }      // Prefix: Action/Verb + Request
public class PatientResponse { }           // Suffix: Response

// ✅ Exceptions
public class PatientNotFoundException { }  // Prefix: Entity + Suffix: Exception

// ❌ Non-Standard
public class PatientMgmt { }          // Don't abbreviate
public class P { }                    // Avoid single letters
public class PatientServiceImpl { }    // Don't use "Impl"
```

### Methods

```java
// ✅ GET operations (query, retrieve, find)
public PatientResponse getPatient(String patientId) { }
public List<PatientResponse> queryPatients(QueryRequest request) { }
public Optional<Patient> findByIdAndTenant(UUID id, String tenantId) { }

// ✅ CREATE operations (create, build)
public Patient createPatient(CreatePatientRequest request) { }
public PatientResponse buildResponse(Patient entity) { }

// ✅ UPDATE operations (update, modify)
public Patient updatePatient(String patientId, UpdateRequest request) { }

// ✅ DELETE operations (delete, remove)
public void deletePatient(String patientId) { }

// ✅ Boolean methods (is, has, can)
public boolean isActive(Patient patient) { }
public boolean hasValidDates(PatientRequest request) { }
public boolean canDelete(Patient patient) { }

// ❌ Non-Standard
public PatientResponse fetchPatient(String patientId) { }  // Use get/find
public void processPatient(Patient p) { }                  // Be specific
public PatientResponse method1(String x) { }              // Non-descriptive
```

### Variables

```java
// ✅ Clear, specific names
String patientId = "p-12345";
LocalDate dateOfBirth = patient.getDateOfBirth();
List<PatientResponse> activePatients = repository.findActive();

// ❌ Non-Standard
String id = "p-12345";                    // Too generic
String pat_id = "p-12345";                // Snake case (use camelCase)
PatientResponse p = service.getPatient(); // Single letter abbreviation
```

---

## Code Organization & Structure

### Service Layer Pattern

```java
@Service
@RequiredArgsConstructor                          // Constructor injection
@Transactional(readOnly = true)                   // Read-only by default
@Slf4j                                            // Logging
public class PatientService {
    private final PatientRepository patientRepository;
    private final PatientEventPublisher eventPublisher;
    private final PatientMapper patientMapper;

    // ✅ READ: No @Transactional needed (readOnly at class level)
    public PatientResponse getPatient(String patientId, String tenantId) {
        return patientRepository
            .findByIdAndTenantId(patientId, tenantId)
            .map(patientMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
    }

    // ✅ LIST: No @Transactional
    public List<PatientResponse> listPatients(String tenantId, int page, int size) {
        return patientRepository
            .findByTenantId(tenantId, PageRequest.of(page, size))
            .stream()
            .map(patientMapper::toResponse)
            .collect(Collectors.toList());
    }

    // ✅ CREATE: Explicit @Transactional (overrides class-level readOnly)
    @Transactional
    public PatientResponse createPatient(CreatePatientRequest request, String tenantId) {
        // 1. Validate input
        if (request.getFirstName() == null || request.getFirstName().isBlank()) {
            throw new ValidationException("First name is required");
        }

        // 2. Check business rules
        if (patientRepository.existsByMrnAndTenantId(request.getMrn(), tenantId)) {
            throw new DuplicateResourceException("Patient with MRN already exists");
        }

        // 3. Create and persist
        Patient patient = Patient.builder()
            .tenantId(tenantId)
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .build();

        patient = patientRepository.save(patient);

        // 4. Publish event (after successful persistence)
        eventPublisher.publishPatientCreated(patient);

        // 5. Return response
        return patientMapper.toResponse(patient);
    }

    // ✅ UPDATE: Explicit @Transactional
    @Transactional
    public PatientResponse updatePatient(
            String patientId,
            UpdatePatientRequest request,
            String tenantId) {
        Patient patient = patientRepository
            .findByIdAndTenantId(patientId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        // Apply updates
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());

        patient = patientRepository.save(patient);

        eventPublisher.publishPatientUpdated(patient);

        return patientMapper.toResponse(patient);
    }

    // ✅ DELETE: Explicit @Transactional
    @Transactional
    public void deletePatient(String patientId, String tenantId) {
        Patient patient = patientRepository
            .findByIdAndTenantId(patientId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        if (patient.hasAssociatedData()) {
            throw new BusinessRuleException("Cannot delete patient with existing care plans");
        }

        patientRepository.delete(patient);

        eventPublisher.publishPatientDeleted(patient);
    }
}
```

### Controller Layer Pattern

```java
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
@Tag(name = "Patients", description = "Patient management")
public class PatientController {
    private final PatientService patientService;

    // ✅ GET: Retrieve resource
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    @Audited(eventType = "PATIENT_VIEW")
    public ResponseEntity<PatientResponse> getPatient(
            @PathVariable String id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(patientService.getPatient(id, tenantId));
    }

    // ✅ GET: List resources with pagination
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PatientResponse>> listPatients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(patientService.listPatients(tenantId, page, size));
    }

    // ✅ POST: Create resource
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PatientResponse> createPatient(
            @Valid @RequestBody CreatePatientRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        PatientResponse response = patientService.createPatient(request, tenantId);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .location(URI.create("/api/v1/patients/" + response.getId()))
            .body(response);
    }

    // ✅ PUT: Update resource
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PatientResponse> updatePatient(
            @PathVariable String id,
            @Valid @RequestBody UpdatePatientRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(patientService.updatePatient(id, request, tenantId));
    }

    // ✅ DELETE: Delete resource
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePatient(
            @PathVariable String id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        patientService.deletePatient(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    // ✅ Exception handler: Local to controller
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("RESOURCE_NOT_FOUND", ex.getMessage(), 404));
    }
}
```

### Repository Pattern

```java
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    // ✅ Multi-tenant filtering ALWAYS
    @Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId AND p.id = :id")
    Optional<Patient> findByIdAndTenantId(
        @Param("id") UUID id,
        @Param("tenantId") String tenantId);

    // ✅ List with tenant filtering
    @Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId ORDER BY p.lastName, p.firstName")
    Page<Patient> findByTenantId(
        @Param("tenantId") String tenantId,
        Pageable pageable);

    // ✅ Check existence with tenant
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM Patient p WHERE p.tenantId = :tenantId AND p.mrn = :mrn")
    boolean existsByMrnAndTenantId(
        @Param("mrn") String mrn,
        @Param("tenantId") String tenantId);

    // ❌ WRONG: No tenant filtering (SECURITY VIOLATION)
    // Optional<Patient> findById(UUID id);
    // List<Patient> findAll();
}
```

---

## Exception Handling

### Custom Exceptions

```java
// ✅ Specific exceptions with context
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(String.format("%s with ID %s not found", resourceType, resourceId));
    }
}

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}

public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}

// ✅ Global exception handler
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(
                "RESOURCE_NOT_FOUND",
                ex.getMessage(),
                404,
                Instant.now()
            ));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(
                "VALIDATION_ERROR",
                ex.getMessage(),
                400,
                Instant.now()
            ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                500,
                Instant.now()
            ));
    }
}
```

---

## Code Quality Checklist

### Before Submitting Code

- [ ] **HIPAA Compliance**
  - [ ] No PHI in log messages
  - [ ] Cache-Control headers on sensitive endpoints
  - [ ] @Audited on PHI access methods
  - [ ] Cache TTL ≤ 5 minutes for PHI

- [ ] **Multi-Tenant**
  - [ ] All queries filter by tenantId
  - [ ] No repository method without tenant check
  - [ ] @RequestHeader("X-Tenant-ID") on all endpoints

- [ ] **Authorization**
  - [ ] @PreAuthorize on all endpoints
  - [ ] Appropriate role check (ADMIN, EVALUATOR, VIEWER)
  - [ ] No authorization bypass

- [ ] **Code Quality**
  - [ ] No code duplication (use helper methods)
  - [ ] Clear, descriptive names (not abbreviated)
  - [ ] No magic numbers (use constants)
  - [ ] Proper error handling (specific exceptions)
  - [ ] Logging at appropriate levels

- [ ] **Testing**
  - [ ] Unit tests for all service methods
  - [ ] Integration tests for API endpoints
  - [ ] Multi-tenant isolation tests
  - [ ] Authorization tests
  - [ ] Error handling tests
  - [ ] >80% code coverage

- [ ] **Documentation**
  - [ ] JavaDoc on public methods
  - [ ] @Operation/@ApiResponse on endpoints
  - [ ] README updated if new feature
  - [ ] Complex logic has inline comments

- [ ] **Performance**
  - [ ] No N+1 queries
  - [ ] Appropriate caching used
  - [ ] Pagination implemented for large datasets
  - [ ] Connection pooling configured

---

## Best Practices

- ✅ **DO use constructor injection (not field injection)**
  - Why: Enables testing; immutability; circular dependency detection
  - Example: `@RequiredArgsConstructor` with private final fields

- ✅ **DO validate input at service entry point**
  - Why: Catch errors early; consistent error messages
  - Example: Check nulls, ranges, format before processing

- ✅ **DO use Optional instead of null checks**
  - Why: Explicit null handling; prevents NPE
  - Example: `.orElseThrow(...)` not `.get()`

- ✅ **DO add @Transactional only where needed**
  - Why: Read operations don't need transactions (performance)
  - Example: Class-level `readOnly=true`, override in CRUD methods

- ✅ **DO use mapper classes for DTO conversion**
  - Why: Encapsulates conversion logic; reusable
  - Example: `PatientMapper.toResponse(entity)`

- ❌ **DON'T catch and ignore exceptions**
  - Why: Silent failures hide bugs
  - Example: Don't swallow exception without logging

- ❌ **DON'T use string concatenation for SQL**
  - Why: SQL injection vulnerability
  - Example: Use `@Query` with `@Param`

- ❌ **DON'T hardcode values**
  - Why: Configuration should be externalizable
  - Example: Use `@Value`, properties files

---

## References

- [Backend Coding Standards](../../backend/docs/CODING_STANDARDS.md)
- [HIPAA Compliance Guide](../../backend/HIPAA-CACHE-COMPLIANCE.md)
- [Spring Boot Best Practices](https://spring.io/guides)
- Clean Code by Robert C. Martin

---

**Last Updated:** January 20, 2026
**Difficulty Level:** ⭐⭐⭐ (3/5 stars)
**Time Investment:** 2-3 weeks
**Prerequisite Skills:** Spring Boot 3.x, Java fundamentals

---

**← [Skills Hub](../README.md)** | **End of Foundation Skills Framework**
