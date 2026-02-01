# API Design & OpenAPI - RESTful API Standards Guide

> **This is a comprehensive guide for designing and documenting RESTful APIs in HDIM.**
> **OpenAPI/Swagger provides machine-readable API contracts for developers and tools.**

---

## Overview

### What is This Skill?

RESTful API design defines how microservices communicate via HTTP. OpenAPI (formerly Swagger) documents APIs in machine-readable format—enabling code generation, mock servers, and client libraries.

**Example:**
```
GET /api/v1/patients/{patientId}  ← Endpoint
Response: { id, firstName, dateOfBirth, ...}  ← Documented response
```

### Why is This Important for HDIM?

HDIM has 51 microservices with 200+ endpoints. Without standardization:

- **API Inconsistency:** Some use `/patients`, others use `/patient`; some return 400, others 422
- **Documentation Drift:** API docs outdated; developers confused
- **Client Integration:** External systems don't know API contract
- **Testing Overhead:** Manual API testing for every change

OpenAPI ensures:
- **Consistency:** Standardized endpoint design
- **Self-Documenting:** Auto-generated Swagger UI
- **Contract Testing:** Clients test against specification
- **Code Generation:** Auto-generate client libraries

### Business Impact

- **Faster Integration:** Partners can integrate without calling support
- **Reduced Bugs:** Clear API contracts prevent misunderstandings
- **API Versioning:** Multiple API versions for backward compatibility
- **Rate Limiting:** Predictable API usage allows fair limits
- **Analytics:** Track which endpoints used most

### Key Services Using OpenAPI

All HDIM REST services use Swagger:
- Gateway Service - Central API entry point
- Patient Service - Patient CRUD operations
- Quality Measure Service - Measure evaluation
- Care Gap Service - Gap management
- FHIR Service - FHIR R4 endpoints

### Estimated Learning Time

1 week (hands-on API design, OpenAPI documentation, client integration)

---

## Key Concepts

### Concept 1: RESTful Resource Design

**REST Principles:**
- **Resource-Oriented:** Model system as resources (Patient, Measure, CarePlan)
- **HTTP Methods:** GET (read), POST (create), PUT (update), DELETE (delete)
- **Status Codes:** 200 (success), 201 (created), 400 (bad request), 404 (not found), 500 (error)
- **Representations:** JSON, XML

**Example Patient Endpoints:**
```
GET    /api/v1/patients              → List all patients
GET    /api/v1/patients/{id}         → Get one patient
POST   /api/v1/patients              → Create patient
PUT    /api/v1/patients/{id}         → Update patient
DELETE /api/v1/patients/{id}         → Delete patient
```

### Concept 2: Request/Response Contracts

**Request Contract:** What client sends
```json
POST /api/v1/patients
Content-Type: application/json

{
  "firstName": "Jane",
  "lastName": "Doe",
  "dateOfBirth": "1990-01-15"
}
```

**Response Contract:** What server returns
```json
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": "p-12345",
  "firstName": "Jane",
  "lastName": "Doe",
  "dateOfBirth": "1990-01-15",
  "createdAt": "2024-01-20T10:30:00Z"
}
```

### Concept 3: OpenAPI Specification

**OpenAPI:** Machine-readable API description
```yaml
openapi: 3.0.0
info:
  title: Patient Service API
  version: 1.0.0
paths:
  /api/v1/patients/{id}:
    get:
      summary: Get patient by ID
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
      responses:
        200:
          description: Patient found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Patient'
```

### Concept 4: API Versioning

**URL Versioning (HDIM uses this):**
```
/api/v1/patients    ← Version 1
/api/v2/patients    ← Version 2 (if breaking changes)
```

**Header Versioning (Alternative):**
```
GET /api/patients
X-API-Version: 1
```

### Concept 5: Error Response Standardization

All errors follow standard format:
```json
{
  "code": "PATIENT_NOT_FOUND",
  "message": "Patient with ID p-123 not found",
  "status": 404,
  "timestamp": "2024-01-20T10:30:00Z",
  "path": "/api/v1/patients/p-123"
}
```

---

## Architecture Pattern

### HDIM API Architecture

```
┌─── External Clients ───┐
│ Web App, Mobile, EHR   │
└───────────┬────────────┘
            │
            ▼
┌─ API Gateway (Port 8001) ─────────┐
│ • JWT validation                   │
│ • Rate limiting                    │
│ • Request routing                  │
│ • Response transformation          │
└─────┬──────────────────────────────┘
      │
      ├──────────────────────────────────┐
      │                                  │
      ▼                                  ▼
┌─ Patient Service ──┐         ┌─ Quality Service ──┐
│ GET  /patients     │         │ POST /measures/    │
│ POST /patients     │         │      evaluate      │
│ PUT  /patients/{id}│         │ GET  /results      │
└────────────────────┘         └────────────────────┘
```

### Request Flow with OpenAPI

```
1. Client reads OpenAPI spec
   ↓
2. Client understands contract
   ↓
3. Client constructs request
   ↓
4. Gateway validates (JWT, rate limit)
   ↓
5. Route to appropriate service
   ↓
6. Service processes request
   ↓
7. Return response matching spec
   ↓
8. Client parses response
```

---

## Implementation Guide

### Step 1: Add SpringDoc OpenAPI Dependency

```gradle
dependencies {
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-api:2.2.0'
}
```

### Step 2: Configure OpenAPI

**application.yml:**
```yaml
springdoc:
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    operations-sorter: method
  api-docs:
    path: /v3/api-docs
  show-actuator: false
```

### Step 3: Annotate Controller

```java
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
@Tag(
    name = "Patients",
    description = "Patient management endpoints"
)
public class PatientController {
    private final PatientService patientService;

    @GetMapping("/{id}")
    @Operation(
        summary = "Get patient by ID",
        description = "Retrieve a single patient by their unique identifier"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Patient found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PatientResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Patient not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<PatientResponse> getPatient(
            @Parameter(
                description = "Patient ID",
                example = "p-12345"
            )
            @PathVariable String id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(patientService.getPatient(id, tenantId));
    }

    @PostMapping
    @Operation(
        summary = "Create new patient",
        description = "Create a new patient record"
    )
    @ApiResponse(
        responseCode = "201",
        description = "Patient created successfully"
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PatientResponse> createPatient(
            @Valid @RequestBody CreatePatientRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        PatientResponse response = patientService.createPatient(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update patient",
        description = "Update existing patient information"
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PatientResponse> updatePatient(
            @PathVariable String id,
            @Valid @RequestBody UpdatePatientRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(patientService.updatePatient(id, request, tenantId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete patient")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePatient(
            @PathVariable String id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        patientService.deletePatient(id, tenantId);
        return ResponseEntity.noContent().build();
    }
}
```

### Step 4: Document DTOs

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Patient response DTO")
public class PatientResponse {
    @Schema(
        description = "Unique patient identifier",
        example = "p-12345"
    )
    private String id;

    @Schema(
        description = "Patient first name",
        example = "Jane"
    )
    private String firstName;

    @Schema(
        description = "Patient last name",
        example = "Doe"
    )
    private String lastName;

    @Schema(
        description = "Date of birth",
        example = "1990-01-15",
        format = "date"
    )
    private LocalDate dateOfBirth;

    @Schema(
        description = "Creation timestamp",
        format = "date-time"
    )
    private Instant createdAt;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Create patient request")
public class CreatePatientRequest {
    @NotBlank(message = "First name required")
    @Schema(description = "Patient first name", example = "Jane")
    private String firstName;

    @NotBlank(message = "Last name required")
    @Schema(description = "Patient last name", example = "Doe")
    private String lastName;

    @NotNull(message = "Date of birth required")
    @Schema(description = "Date of birth", example = "1990-01-15", format = "date")
    private LocalDate dateOfBirth;
}
```

### Step 5: Access API Documentation

```
# Swagger UI (interactive documentation)
http://localhost:8084/swagger-ui.html

# OpenAPI JSON (machine-readable)
http://localhost:8084/v3/api-docs

# JSON for specific service
http://localhost:8084/v3/api-docs.json
```

---

## HDIM API Standards

### URL Naming Conventions

```
✅ GOOD:
GET    /api/v1/patients
POST   /api/v1/patients
GET    /api/v1/patients/{patientId}
PUT    /api/v1/patients/{patientId}
DELETE /api/v1/patients/{patientId}

❌ WRONG:
GET    /getPatient              (verb in URL)
POST   /patient                 (singular, not plural)
GET    /api/patient/{id}        (missing version)
DELETE /removePatientById       (overly verbose)
```

### Status Code Standards

```
200 OK              ← GET, PUT, PATCH success
201 Created         ← POST success (resource created)
204 No Content      ← DELETE success (no response body)
400 Bad Request     ← Invalid input
401 Unauthorized    ← Missing/invalid authentication
403 Forbidden       ← Valid auth but insufficient permissions
404 Not Found       ← Resource doesn't exist
409 Conflict        ← Operation conflicts with existing data
500 Server Error    ← Unexpected server error
```

### Pagination Standards

```
GET /api/v1/patients?page=0&size=20&sort=firstName,asc

Response:
{
  "content": [...],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 500,
  "totalPages": 25
}
```

---

## Best Practices

- ✅ **DO use nouns for resources, not verbs**
  - Why: RESTful convention; HTTP methods specify action
  - Example: `/patients` (not `/getPatients`)

- ✅ **DO include API version in URL**
  - Why: Enables breaking changes without breaking clients
  - Example: `/api/v1/patients` (not `/api/patients`)

- ✅ **DO use appropriate HTTP methods**
  - Why: Enables correct caching; clients understand intent
  - Example: GET for read-only; POST for create

- ✅ **DO document all parameters and responses**
  - Why: Clients understand contract; reduces support questions
  - Example: Use @Parameter, @ApiResponse annotations

- ✅ **DO standardize error responses**
  - Why: Clients can handle errors consistently
  - Example: All errors return `{code, message, status, timestamp}`

- ❌ **DON'T use verbs in URLs**
  - Why: HTTP methods define verbs
  - Example: Don't use `/patients/create` (use POST /patients)

- ❌ **DON'T nest resources too deeply**
  - Why: URLs become complex; hard to document
  - Example: `/api/v1/patients/{patientId}/measures/{measureId}` (OK)
  - Example: `/api/v1/patients/{patientId}/measures/{measureId}/results/{resultId}/details` (too deep)

- ❌ **DON'T return HTML error pages from API**
  - Why: API clients expect JSON, not HTML
  - Example: Don't return 404 HTML page; return JSON error

---

## Testing API Design

### Integration Test

```java
@SpringBootTest
@AutoConfigureMockMvc
class PatientApiDocumentationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGenerateOpenApiSpecification() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"));
    }

    @Test
    void patientGetEndpointShouldReturn200() throws Exception {
        mockMvc.perform(
            get("/api/v1/patients/p-123")
                .header("X-Tenant-ID", "tenant-001"))
            .andExpect(status().isOk());
    }

    @Test
    void patientCreateEndpointShouldReturn201() throws Exception {
        mockMvc.perform(
            post("/api/v1/patients")
                .header("X-Tenant-ID", "tenant-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Jane\",\"lastName\":\"Doe\"}"))
            .andExpect(status().isCreated());
    }
}
```

---

## References

- OpenAPI 3.0 Specification: https://spec.openapis.org/oas/v3.0.0
- SpringDoc OpenAPI: https://springdoc.org
- REST API Best Practices: https://restfulapi.net
- HDIM Service Catalog: `docs/services/SERVICE_CATALOG.md`

---

**Last Updated:** January 20, 2026
**Difficulty Level:** ⭐⭐⭐ (3/5 stars)
**Time Investment:** 1 week
**Prerequisite Skills:** Spring Boot 3.x, HTTP basics

---

**← [Skills Hub](../README.md)** | **→ [Next: Coding Standards & Best Practices](../10-coding-standards/hdim-standards.md)**
