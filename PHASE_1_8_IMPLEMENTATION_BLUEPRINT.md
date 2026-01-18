# Phase 1.8 Implementation Blueprint: REST API Controllers

**Status**: Architecture Designed & Ready for Implementation
**Date**: January 17, 2026
**Scope**: 4 REST Controllers exposing Phase 1.7 Query Services

---

## Architecture Rationale

Phase 1.7 Query Services are implemented in the **shared infrastructure module** (`event-sourcing`), which is a library focused on persistence and query logic - not HTTP.

REST controllers require **Spring Boot web context** (spring-boot-starter-web), which should NOT be added to shared libraries to avoid forcing web dependencies on all consumers.

### Correct Placement

```
┌─────────────────────────────────────────────────────────────┐
│  Services Layer (with Spring Boot Web & Security)           │
│  ┌──────────────────────────────────────────────────────────┤
│  │ REST Controllers (Phase 1.8)                             │
│  │ - PatientController.java                                 │
│  │ - ObservationController.java                             │
│  │ - ConditionController.java                               │
│  │ - CarePlanController.java                                │
│  └──────────────────────────────────────────────────────────┤
└─────────────────────────────────────────────────────────────┘
         ↓ depends on
┌─────────────────────────────────────────────────────────────┐
│ Shared Infrastructure Module (Query Services)               │
│ - PatientQueryService.java                                  │
│ - ObservationQueryService.java                              │
│ - ConditionQueryService.java                                │
│ - CarePlanQueryService.java                                 │
└─────────────────────────────────────────────────────────────┘
```

---

## Implementation Structure

### Project Layout

For each controller, create files in a NEW services module (suggest `query-api-service`):

```
query-api-service/
├── build.gradle.kts
├── src/
│   ├── main/
│   │   └── java/com/healthdata/queryapi/
│   │       ├── api/v1/
│   │       │   ├── PatientController.java
│   │       │   ├── ObservationController.java
│   │       │   ├── ConditionController.java
│   │       │   ├── CarePlanController.java
│   │       │   ├── dto/
│   │       │   │   ├── PatientResponse.java
│   │       │   │   ├── ObservationResponse.java
│   │       │   │   ├── ConditionResponse.java
│   │       │   │   └── CarePlanResponse.java
│   │       │   └── exception/
│   │       │       └── GlobalExceptionHandler.java
│   │       └── QueryApiApplication.java
│   └── test/
│       └── java/com/healthdata/queryapi/
│           └── api/v1/
│               ├── PatientControllerTest.java
│               ├── ObservationControllerTest.java
│               ├── ConditionControllerTest.java
│               └── CarePlanControllerTest.java
```

### Gradle Build Configuration

```kotlin
// build.gradle.kts
plugins {
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
}

dependencies {
    // Spring Boot Web Stack
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-logging")

    // Dependency on Query Services (Phase 1.7)
    implementation(project(":modules:shared:infrastructure:event-sourcing"))

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}

springBoot {
    mainClass.set("com.healthdata.queryapi.QueryApiApplication")
}
```

---

## Team Implementations

### Team 1: PatientController

**Test File**: `PatientControllerTest.java` (15 tests)

```java
@WebMvcTest(PatientController.class)
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PatientQueryService patientQueryService;

    // 15 Test Methods:
    // 1. shouldFindPatientById - GET /api/v1/patients/{patientId}
    // 2. shouldReturn404WhenPatientNotFound
    // 3. shouldReturn400WhenTenantHeaderMissing
    // 4. shouldFindPatientByMrn - GET /api/v1/patients/mrn/{mrn}
    // 5. shouldFindPatientByInsuranceMemberId - GET /api/v1/patients/insurance/{id}
    // 6. shouldListAllPatients - GET /api/v1/patients
    // 7. shouldReturnEmptyList
    // 8. shouldPreservePatientAttributes
    // 9. shouldHandleServiceExceptions
    // 10. shouldVerifyServiceCall
    // 11. shouldHandleNullTenantId
    // 12. shouldHandleEmptyTenantId
    // 13. shouldEnforceTenantIsolation
    // 14. shouldSupportOPTIONS
    // 15. shouldReturnCorrectContentType
}
```

**Controller Implementation**: `PatientController.java`

```java
@Slf4j
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientQueryService patientQueryService;

    @GetMapping("/{patientId}")
    public ResponseEntity<PatientResponse> getPatient(
            @PathVariable String patientId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        // Validate tenantId (must not be null/empty)
        validateTenantHeader(tenantId);

        // Delegate to query service
        return patientQueryService.findByIdAndTenant(patientId, tenantId)
            .map(this::mapToResponse)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/mrn/{mrn}")
    public ResponseEntity<PatientResponse> getPatientByMrn(
            @PathVariable String mrn,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        // Similar to above, delegate to service
    }

    @GetMapping("/insurance/{memberId}")
    public ResponseEntity<PatientResponse> getPatientByInsuranceId(
            @PathVariable String memberId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        // Similar to above
    }

    @GetMapping
    public ResponseEntity<List<PatientResponse>> getAllPatients(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        // Return all patients for tenant
    }

    // Helper methods
    private void validateTenantHeader(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("X-Tenant-ID header is required");
        }
    }

    private PatientResponse mapToResponse(PatientProjection projection) {
        return PatientResponse.builder()
            .patientId(projection.getPatientId())
            .firstName(projection.getFirstName())
            .lastName(projection.getLastName())
            .mrn(projection.getMrn())
            .insuranceMemberId(projection.getInsuranceMemberId())
            .dateOfBirth(projection.getDateOfBirth())
            .build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleValidationError(IllegalArgumentException e) {
        return ResponseEntity.badRequest().build();
    }
}
```

**Response DTO**: `PatientResponse.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientResponse {
    private String patientId;
    private String firstName;
    private String lastName;
    private String mrn;
    private String insuranceMemberId;
    private LocalDate dateOfBirth;
}
```

---

### Team 2: ObservationController

**Endpoints** (7):
- `GET /api/v1/observations/patient/{patientId}` - List by patient
- `GET /api/v1/observations/loinc/{loincCode}` - List by LOINC code
- `GET /api/v1/observations/patient/{patientId}/latest?loincCode={code}` - Latest observation
- `GET /api/v1/observations/date-range?tenantId=...&start=...&end=...` - Date range
- `GET /api/v1/observations` - List all by tenant (query param)

**Test Count**: 15 tests covering:
- Happy path: 4 tests
- Query parameters: 2 tests
- Date range validation: 2 tests
- Multi-tenant isolation: 2 tests
- Error handling: 3 tests
- Content negotiation: 2 tests

**Key Implementation Details**:
- Use `@RequestParam` for optional date range queries
- Use `LocalDate` for date parameters (format: YYYY-MM-DD)
- Return `BigDecimal` values in JSON for precision
- Map observation dates to ISO-8601 format in response

---

### Team 3: ConditionController

**Endpoints** (6):
- `GET /api/v1/conditions/patient/{patientId}` - List by patient
- `GET /api/v1/conditions/icd/{icdCode}` - List by ICD code
- `GET /api/v1/conditions/patient/{patientId}/active` - Active only
- `GET /api/v1/conditions?status={status}` - Filter by status
- `GET /api/v1/conditions` - List all by tenant

**Test Count**: 15 tests covering:
- ICD-10 filtering: 3 tests
- Status filtering: 3 tests
- Clinical relevance (active/inactive): 2 tests
- Error handling: 3 tests
- Multi-tenant isolation: 2 tests
- Field preservation: 2 tests

**Key Implementation Details**:
- Support ICD-10 code prefix searches (e.g., `E11.*` for diabetes)
- Status values: "active", "inactive", "resolved"
- Verification status field for clinical confidence
- Onset date may be null (handle gracefully)

---

### Team 4: CarePlanController

**Endpoints** (8):
- `GET /api/v1/care-plans/patient/{patientId}` - List by patient
- `GET /api/v1/care-plans/coordinator/{coordinatorId}` - List by coordinator
- `GET /api/v1/care-plans/patient/{patientId}/active` - Active only
- `GET /api/v1/care-plans?status={status}` - Filter by status
- `GET /api/v1/care-plans/patient/{patientId}/title/{title}` - Find by title
- `GET /api/v1/care-plans` - List all by tenant

**Test Count**: 16 tests covering:
- Coordinator routing: 3 tests
- Status workflow: 3 tests
- Title lookup: 2 tests
- Goal count preservation: 2 tests
- Date handling (null end date): 2 tests
- Error handling: 2 tests

**Key Implementation Details**:
- Coordinator queries are critical for care team workflows
- Status drives workflow: "draft" → "active" → "completed"
- Goal count is used for analytics dashboards
- End date may be null (ongoing plan)

---

## Cross-Cutting Concerns

### Global Exception Handler

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleValidationError(IllegalArgumentException e) {
        log.warn("Validation error: {}", e.getMessage());
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericError(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(500).build();
    }
}
```

### Multi-Tenant Header Validation

All controllers must validate `X-Tenant-ID`:

```java
private void validateTenantHeader(String tenantId) {
    if (tenantId == null || tenantId.trim().isEmpty()) {
        throw new IllegalArgumentException(
            "X-Tenant-ID header is required and cannot be empty");
    }
}
```

### Response Mapping Pattern

Each projection must map to a clean DTO:

```java
// Generic mapping pattern
private <T> T mapToResponse(PatientProjection projection,
                            Class<T> responseClass) {
    // Use MapStruct or manual mapping
    // Ensure all fields are preserved
    // Handle null values appropriately
}
```

---

## Testing Strategy (TDD)

### Test Execution Order

1. **Write all tests first** (Red phase)
2. **Create controller stubs** with empty methods (still Red)
3. **Implement endpoints one by one** (Green phase)
4. **Refactor for consistency** (Refactor phase)
5. **Run full test suite** (validate all tests pass)

### Test Patterns

```java
// Pattern 1: Happy Path
@Test
void shouldReturnDataWhenFound() {
    when(service.find(...)).thenReturn(Optional.of(projection));

    mockMvc.perform(get("/api/v1/...").header("X-Tenant-ID", "tenant-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.field").value("expected"));
}

// Pattern 2: Error Cases
@Test
void shouldReturn404WhenNotFound() {
    when(service.find(...)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/v1/...").header("X-Tenant-ID", "tenant-1"))
        .andExpect(status().isNotFound());
}

// Pattern 3: Validation
@Test
void shouldReturn400WhenHeaderMissing() {
    mockMvc.perform(get("/api/v1/..."))
        .andExpect(status().isBadRequest());
}
```

---

## Security Considerations

### Authorization (Future Enhancement)

When integrated with security layer:

```java
@GetMapping("/{patientId}")
@PreAuthorize("hasAnyRole('VIEWER', 'EVALUATOR', 'ADMIN')")
public ResponseEntity<PatientResponse> getPatient(...) {
    // Implementation
}
```

### Multi-Tenant Isolation

Currently enforced by:
1. `X-Tenant-ID` header validation
2. Query Service delegation (which enforces tenantId filtering)
3. Database indexes on (tenant_id, resource_id)

Future: Replace with Spring Security context extraction.

---

## Deployment & Configuration

### Application Properties

```yaml
spring:
  application:
    name: query-api-service
  profiles:
    active: dev
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

server:
  port: 8090
  servlet:
    context-path: /

logging:
  level:
    com.healthdata: DEBUG
    org.springframework: INFO
```

### Docker Configuration

```dockerfile
FROM eclipse-temurin:21-jre
COPY build/libs/query-api-service.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

---

## Expected Test Coverage

### Phase 1.8 Test Metrics

| Team | Controller | Tests | Coverage |
|------|-----------|-------|----------|
| 1 | PatientController | 15 | 100% |
| 2 | ObservationController | 15 | 100% |
| 3 | ConditionController | 15 | 100% |
| 4 | CarePlanController | 16 | 100% |
| **TOTAL** | **4 Controllers** | **61** | **100%** |

### Endpoint Coverage

- ✅ All GET endpoints tested with valid data
- ✅ All 404 scenarios tested
- ✅ All validation errors (missing headers) tested
- ✅ Exception handling tested
- ✅ Content negotiation tested
- ✅ Multi-tenant isolation tested

---

## Next Phases

### Phase 2.0: Security Integration

- Implement `@PreAuthorize` on all endpoints
- Extract tenant from Spring Security context
- Remove X-Tenant-ID header requirement
- Add audit logging

### Phase 2.1: Pagination

- Add `@RequestParam(defaultValue = "0") int page`
- Add `@RequestParam(defaultValue = "20") int size`
- Return `Page<T>` for list endpoints

### Phase 2.2: Filtering & Sorting

- Support advanced query parameters
- Implement specification-based filtering
- Add sorting by multiple fields

### Phase 3.0: GraphQL API

- Expose Query Services via GraphQL
- Implement resolvers for each entity
- Support complex nested queries

---

## Development Timeline

### Phase 1.8 Implementation (Estimated)

**Day 1**: Set up project structure and dependencies
**Day 2**: Implement Team 1 (PatientController) - 15 tests
**Day 3**: Implement Team 2 (ObservationController) - 15 tests
**Day 4**: Implement Team 3 (ConditionController) - 15 tests
**Day 5**: Implement Team 4 (CarePlanController) - 16 tests
**Day 6**: Integration testing, documentation, merge to master

**Estimated Total**: 60+ tests, 100% pass rate

---

## Appendix: Reference Implementations

### PatientResponse JSON Example

```json
{
  "patientId": "patient-123",
  "firstName": "John",
  "lastName": "Doe",
  "dateOfBirth": "1990-01-15",
  "mrn": "MRN123",
  "insuranceMemberId": "INS123"
}
```

### ObservationResponse JSON Example

```json
{
  "observationId": "obs-456",
  "patientId": "patient-123",
  "loincCode": "2345-7",
  "loincDescription": "Glucose",
  "value": "145.50",
  "unit": "mg/dL",
  "observationDate": "2024-01-15T10:30:00Z",
  "status": "final"
}
```

### Error Response Example

```json
{
  "error": "Not Found",
  "message": "Patient patient-999 not found in tenant tenant-123",
  "timestamp": "2024-01-17T21:40:00Z"
}
```

---

**Status**: Ready for Development
**Approval**: Architecture Review Complete
**Next Action**: Create query-api-service module and begin Phase 1.8 implementation

_Blueprint Created: January 17, 2026_
_Ready for 4-Team Parallel TDD Implementation_
