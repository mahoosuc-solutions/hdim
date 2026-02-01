# API Documentation Patterns

**Purpose:** Standardized patterns for OpenAPI documentation across all HDIM microservices.

**Audience:** Backend developers implementing API documentation with Springdoc OpenAPI.

**Status:** ✅ ACTIVE - Phase 1 (Patient, Care Gap, Quality Measure, FHIR services)

---

## Table of Contents

1. [Common Patterns](#common-patterns)
2. [Controller Annotations](#controller-annotations)
3. [Method Annotations](#method-annotations)
4. [DTO Annotations](#dto-annotations)
5. [Error Response Documentation](#error-response-documentation)
6. [Multi-Tenancy Pattern](#multi-tenancy-pattern)
7. [Authentication Pattern](#authentication-pattern)
8. [Pagination Pattern](#pagination-pattern)
9. [HIPAA Compliance Notes](#hipaa-compliance-notes)
10. [Examples](#examples)

---

## Common Patterns

### 1. Import Statements

All controllers should include these imports:

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
```

### 2. Consistent HTTP Status Codes

| Code | Usage | When to Use |
|------|-------|-------------|
| 200 | OK | Successful GET/PUT/PATCH operations |
| 201 | Created | Successful POST operations creating a resource |
| 204 | No Content | Successful DELETE operations |
| 400 | Bad Request | Invalid request payload or parameters |
| 401 | Unauthorized | Missing or invalid JWT token |
| 403 | Forbidden | Valid token but insufficient permissions |
| 404 | Not Found | Resource does not exist |
| 409 | Conflict | Resource already exists or state conflict |
| 500 | Internal Server Error | Unexpected server error |

---

## Controller Annotations

### Pattern

```java
@RestController
@RequestMapping("/api/v1/{resource}")
@RequiredArgsConstructor
@Tag(
    name = "Resource Management",
    description = "APIs for managing {resource} resources. " +
                  "All endpoints require JWT authentication and X-Tenant-ID header."
)
public class ResourceController {
    // Controller implementation
}
```

### Examples by Service

**Patient Service:**
```java
@Tag(
    name = "Patient Management",
    description = "APIs for managing patient records and clinical data. " +
                  "Supports patient CRUD, timeline, allergies, medications, immunizations, and procedures."
)
```

**Care Gap Service:**
```java
@Tag(
    name = "Care Gap Management",
    description = "APIs for care gap identification, closure, and tracking. " +
                  "Supports gap detection, bulk operations, provider assignment, and analytics."
)
```

**Quality Measure Service:**
```java
@Tag(
    name = "Quality Measures",
    description = "APIs for HEDIS and CMS quality measure evaluation and reporting. " +
                  "Supports measure calculation, batch jobs, and report generation."
)
```

**FHIR Service:**
```java
@Tag(
    name = "FHIR R4 Resources",
    description = "FHIR R4-compliant resource management APIs. " +
                  "Supports Patient, Observation, Condition, Encounter, MedicationRequest, AllergyIntolerance."
)
```

---

## Method Annotations

### Complete Pattern (GET Operation)

```java
@GetMapping("/{resourceId}")
@PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
@Audited(eventType = "RESOURCE_ACCESS")
@Operation(
    summary = "Get resource by ID",
    description = "Retrieves a resource by ID with multi-tenant isolation. " +
                  "Includes full resource details and related metadata.",
    security = @SecurityRequirement(name = "Bearer Authentication")
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Resource found and returned",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ResourceResponse.class),
            examples = @ExampleObject(
                name = "Example Resource",
                value = "{\"id\": \"123\", \"name\": \"Example\", \"status\": \"ACTIVE\"}"
            )
        )
    ),
    @ApiResponse(
        responseCode = "404",
        description = "Resource not found for the given ID and tenant",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(
                name = "Not Found Error",
                value = "{\"error\": \"Resource not found\", \"message\": \"No resource found with ID: 123\", \"timestamp\": \"2026-01-25T10:00:00Z\"}"
            )
        )
    ),
    @ApiResponse(
        responseCode = "403",
        description = "Access denied - insufficient permissions or wrong tenant"
    )
})
public ResponseEntity<ResourceResponse> getResource(
    @Parameter(
        description = "Resource ID (UUID format)",
        required = true,
        example = "550e8400-e29b-41d4-a716-446655440000"
    )
    @PathVariable String resourceId,

    @Parameter(
        description = "Tenant ID for multi-tenant isolation (must match authenticated user's tenant)",
        required = true,
        example = "tenant-123"
    )
    @RequestHeader("X-Tenant-ID") String tenantId
) {
    return ResponseEntity.ok(resourceService.getResource(resourceId, tenantId));
}
```

### Complete Pattern (POST Operation)

```java
@PostMapping
@PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
@Audited(eventType = "RESOURCE_CREATE")
@Operation(
    summary = "Create new resource",
    description = "Creates a new resource with the provided data. " +
                  "The resource is automatically associated with the tenant from X-Tenant-ID header.",
    security = @SecurityRequirement(name = "Bearer Authentication")
)
@ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Resource created successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ResourceResponse.class),
            examples = @ExampleObject(
                name = "Created Resource",
                value = "{\"id\": \"123\", \"name\": \"New Resource\", \"status\": \"ACTIVE\", \"createdAt\": \"2026-01-25T10:00:00Z\"}"
            )
        )
    ),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid request payload or validation errors",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(
                name = "Validation Error",
                value = "{\"error\": \"Validation failed\", \"message\": \"name: must not be blank\", \"timestamp\": \"2026-01-25T10:00:00Z\"}"
            )
        )
    ),
    @ApiResponse(
        responseCode = "409",
        description = "Resource already exists with the same identifier"
    )
})
public ResponseEntity<ResourceResponse> createResource(
    @Parameter(
        description = "Tenant ID for multi-tenant isolation",
        required = true,
        example = "tenant-123"
    )
    @RequestHeader("X-Tenant-ID") String tenantId,

    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Resource data to create",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = CreateResourceRequest.class),
            examples = @ExampleObject(
                name = "Create Request",
                value = "{\"name\": \"New Resource\", \"description\": \"Example resource\", \"type\": \"TYPE_A\"}"
            )
        )
    )
    @RequestBody @Valid CreateResourceRequest request
) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(resourceService.createResource(request, tenantId));
}
```

### Complete Pattern (Search/List Operation)

```java
@GetMapping
@PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST')")
@Operation(
    summary = "Search resources",
    description = "Search and filter resources with pagination support. " +
                  "Results are automatically filtered by tenant from X-Tenant-ID header.",
    security = @SecurityRequirement(name = "Bearer Authentication")
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Resources retrieved successfully (may be empty list)",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = PagedResourceResponse.class),
            examples = @ExampleObject(
                name = "Paginated Results",
                value = "{\"content\": [{\"id\": \"123\", \"name\": \"Resource 1\"}], \"page\": 0, \"size\": 20, \"totalElements\": 1, \"totalPages\": 1}"
            )
        )
    )
})
public ResponseEntity<Page<ResourceResponse>> searchResources(
    @Parameter(
        description = "Tenant ID for multi-tenant isolation",
        required = true,
        example = "tenant-123"
    )
    @RequestHeader("X-Tenant-ID") String tenantId,

    @Parameter(
        description = "Page number (0-indexed)",
        example = "0"
    )
    @RequestParam(defaultValue = "0") int page,

    @Parameter(
        description = "Page size (max 100)",
        example = "20"
    )
    @RequestParam(defaultValue = "20") int size,

    @Parameter(
        description = "Sort field and direction (e.g., 'name,asc' or 'createdAt,desc')",
        example = "name,asc"
    )
    @RequestParam(defaultValue = "createdAt,desc") String sort,

    @Parameter(
        description = "Filter by resource status",
        example = "ACTIVE"
    )
    @RequestParam(required = false) String status
) {
    return ResponseEntity.ok(
        resourceService.searchResources(tenantId, page, size, sort, status)
    );
}
```

---

## DTO Annotations

### Request DTO Pattern

```java
@Schema(description = "Request to create a new resource")
public class CreateResourceRequest {

    @Schema(
        description = "Resource name",
        example = "Example Resource",
        required = true,
        minLength = 1,
        maxLength = 255
    )
    @NotBlank(message = "name is required")
    @Size(max = 255, message = "name must not exceed 255 characters")
    private String name;

    @Schema(
        description = "Resource description",
        example = "This is an example resource for demonstration purposes",
        maxLength = 1000
    )
    @Size(max = 1000, message = "description must not exceed 1000 characters")
    private String description;

    @Schema(
        description = "Resource type",
        example = "TYPE_A",
        allowableValues = {"TYPE_A", "TYPE_B", "TYPE_C"},
        required = true
    )
    @NotNull(message = "type is required")
    private ResourceType type;

    @Schema(
        description = "Resource metadata (key-value pairs)",
        example = "{\"key1\": \"value1\", \"key2\": \"value2\"}"
    )
    private Map<String, String> metadata;

    // Getters and setters...
}
```

### Response DTO Pattern

```java
@Schema(description = "Resource information response")
public class ResourceResponse {

    @Schema(
        description = "Resource unique identifier (UUID)",
        example = "550e8400-e29b-41d4-a716-446655440000",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    private String id;

    @Schema(
        description = "Tenant ID (automatically set from authentication context)",
        example = "tenant-123",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    private String tenantId;

    @Schema(
        description = "Resource name",
        example = "Example Resource"
    )
    private String name;

    @Schema(
        description = "Resource status",
        example = "ACTIVE",
        allowableValues = {"ACTIVE", "INACTIVE", "ARCHIVED"}
    )
    private String status;

    @Schema(
        description = "Creation timestamp (ISO 8601)",
        example = "2026-01-25T10:00:00Z",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    private Instant createdAt;

    @Schema(
        description = "Last update timestamp (ISO 8601)",
        example = "2026-01-25T15:30:00Z",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    private Instant updatedAt;

    // Getters and setters...
}
```

---

## Error Response Documentation

### Standard Error Response DTO

```java
@Schema(description = "Standard error response format")
public class ErrorResponse {

    @Schema(
        description = "Error type/category",
        example = "VALIDATION_ERROR",
        allowableValues = {"VALIDATION_ERROR", "NOT_FOUND", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_ERROR"}
    )
    private String error;

    @Schema(
        description = "Human-readable error message (HIPAA-compliant, no PHI)",
        example = "Resource not found"
    )
    private String message;

    @Schema(
        description = "Error timestamp (ISO 8601)",
        example = "2026-01-25T10:00:00Z"
    )
    private Instant timestamp;

    @Schema(
        description = "Request path that caused the error",
        example = "/api/v1/patients/123"
    )
    private String path;

    @Schema(
        description = "Validation errors (for 400 Bad Request responses)",
        example = "[{\"field\": \"name\", \"message\": \"must not be blank\"}]"
    )
    private List<FieldError> fieldErrors;

    // Getters and setters...
}
```

### Error Response Examples

**404 Not Found:**
```json
{
  "error": "NOT_FOUND",
  "message": "Resource not found with ID: 123",
  "timestamp": "2026-01-25T10:00:00Z",
  "path": "/api/v1/patients/123"
}
```

**400 Bad Request (Validation):**
```json
{
  "error": "VALIDATION_ERROR",
  "message": "Validation failed for one or more fields",
  "timestamp": "2026-01-25T10:00:00Z",
  "path": "/api/v1/patients",
  "fieldErrors": [
    {"field": "firstName", "message": "must not be blank"},
    {"field": "dateOfBirth", "message": "must be in the past"}
  ]
}
```

**403 Forbidden:**
```json
{
  "error": "FORBIDDEN",
  "message": "Access denied: insufficient permissions",
  "timestamp": "2026-01-25T10:00:00Z",
  "path": "/api/v1/patients/123"
}
```

---

## Multi-Tenancy Pattern

### Header Documentation

All endpoints must document the `X-Tenant-ID` header:

```java
@Parameter(
    description = "Tenant ID for multi-tenant isolation. " +
                  "Must match the authenticated user's tenant. " +
                  "All operations are automatically scoped to this tenant to prevent cross-tenant data access.",
    required = true,
    example = "tenant-123",
    schema = @Schema(type = "string", pattern = "^tenant-[a-zA-Z0-9-]+$")
)
@RequestHeader("X-Tenant-ID") String tenantId
```

### Multi-Tenant Query Pattern

```java
// Service layer example (reference in API docs)
public Page<Resource> findByTenant(String tenantId, Pageable pageable) {
    // All queries automatically filter by tenant
    return repository.findByTenantId(tenantId, pageable);
}
```

---

## Authentication Pattern

### JWT Bearer Authentication

**Header Format:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**How to Obtain Token:**
1. POST to `/api/v1/auth/login` with credentials
2. Extract `accessToken` from response
3. Include in all subsequent requests as `Authorization: Bearer <token>`

**Token Expiration:**
- Access Token: 1 hour (default)
- Refresh Token: 7 days (default)

**Required Roles:**
- `ADMIN`: Full access to all endpoints
- `EVALUATOR`: Read/write access to clinical operations
- `ANALYST`: Read-only access to reports and analytics
- `VIEWER`: Read-only access to basic resources

---

## Pagination Pattern

### Standard Pagination Parameters

```java
@Parameter(
    description = "Page number (0-indexed)",
    example = "0",
    schema = @Schema(type = "integer", minimum = "0", defaultValue = "0")
)
@RequestParam(defaultValue = "0") int page

@Parameter(
    description = "Page size (number of items per page, max 100)",
    example = "20",
    schema = @Schema(type = "integer", minimum = "1", maximum = "100", defaultValue = "20")
)
@RequestParam(defaultValue = "20") int size

@Parameter(
    description = "Sort criteria in format: field,direction " +
                  "(e.g., 'name,asc' or 'createdAt,desc'). " +
                  "Multiple sort criteria can be specified as comma-separated values.",
    example = "createdAt,desc",
    schema = @Schema(type = "string", pattern = "^[a-zA-Z]+,(asc|desc)$")
)
@RequestParam(defaultValue = "createdAt,desc") String sort
```

### Paginated Response Format

```json
{
  "content": [
    {"id": "1", "name": "Item 1"},
    {"id": "2", "name": "Item 2"}
  ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3,
  "first": true,
  "last": false
}
```

---

## HIPAA Compliance Notes

### PHI Protection in API Documentation

**✅ DO:**
- Use realistic but non-identifiable examples (e.g., "John Doe", generic dates)
- Document cache headers: `Cache-Control: no-store, no-cache, must-revalidate`
- Include audit logging notes in endpoint descriptions
- Specify PHI data elements in schema descriptions

**❌ DON'T:**
- Include real patient data in examples
- Use actual MRNs, SSNs, or identifiable information
- Expose PHI in error messages
- Log PHI in API documentation

### HIPAA-Compliant Example Data

**Good Examples:**
```json
{
  "patientId": "550e8400-e29b-41d4-a716-446655440000",
  "firstName": "John",
  "lastName": "Doe",
  "dateOfBirth": "1980-05-15",
  "mrn": "MRN-123456"
}
```

**Bad Examples:**
```json
{
  "patientId": "real-patient-id",
  "firstName": "ActualFirstName",
  "lastName": "ActualLastName",
  "ssn": "123-45-6789"
}
```

### Cache Control Headers

All PHI endpoints must include in documentation:

```java
@Operation(
    description = "... Response includes Cache-Control: no-store header " +
                  "to prevent caching of PHI data (HIPAA §164.312(a)(1) compliance)."
)
```

---

## Examples

### Complete Patient Endpoint Example

```java
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
@Tag(
    name = "Patient Management",
    description = "APIs for managing patient records and clinical data"
)
public class PatientController {

    private final PatientService patientService;

    @GetMapping("/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    @Audited(eventType = "PATIENT_ACCESS")
    @Operation(
        summary = "Get patient by ID",
        description = "Retrieves a patient record by ID with multi-tenant isolation. " +
                      "Includes demographics, identifiers, and basic clinical information. " +
                      "Response includes Cache-Control: no-store header to prevent caching of PHI.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Patient found and returned",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PatientResponse.class),
                examples = @ExampleObject(
                    name = "Example Patient",
                    value = """
                        {
                          "id": "550e8400-e29b-41d4-a716-446655440000",
                          "firstName": "John",
                          "lastName": "Doe",
                          "dateOfBirth": "1980-05-15",
                          "gender": "MALE",
                          "mrn": "MRN-123456",
                          "status": "ACTIVE",
                          "createdAt": "2026-01-01T10:00:00Z"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Patient not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - insufficient permissions or wrong tenant"
        )
    })
    public ResponseEntity<PatientResponse> getPatient(
        @Parameter(
            description = "Patient ID (UUID)",
            required = true,
            example = "550e8400-e29b-41d4-a716-446655440000"
        )
        @PathVariable String patientId,

        @Parameter(
            description = "Tenant ID for multi-tenant isolation",
            required = true,
            example = "tenant-123"
        )
        @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        return ResponseEntity.ok(patientService.getPatient(patientId, tenantId));
    }
}
```

---

## Summary Checklist

Before submitting API documentation, verify:

- [ ] All controllers have `@Tag` annotations with clear descriptions
- [ ] All methods have `@Operation` with summary and description
- [ ] All parameters have `@Parameter` with descriptions and examples
- [ ] All responses (200, 400, 403, 404, 500) are documented
- [ ] All DTOs have `@Schema` annotations
- [ ] Error responses use standard ErrorResponse format
- [ ] Multi-tenant `X-Tenant-ID` header documented
- [ ] Authentication requirements documented
- [ ] Pagination parameters follow standard pattern
- [ ] Examples use non-identifiable data (HIPAA compliant)
- [ ] Cache headers documented for PHI endpoints

---

**Last Updated:** January 25, 2026
**Version:** 1.0
**Maintainer:** HDIM Development Team
