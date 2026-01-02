# Health Score API Implementation - Phase 3.3

## Overview
This document describes the implementation of the Health Score API endpoints following TDD principles.

## Implemented Endpoints

### 1. GET /quality-measure/patients/{patientId}/health-score
**Purpose**: Retrieve the current health score for a specific patient

**Features**:
- Returns most recent health score with all component breakdowns
- Supports FHIR-style patient IDs with slashes (e.g., `Patient/123`)
- Returns 404 if no score exists for the patient
- Returns DTO, not entity

**Authorization**: `ANALYST`, `EVALUATOR`, `ADMIN`, `SUPER_ADMIN`

**Response**: `HealthScoreDTO`
```json
{
  "id": "uuid",
  "patientId": "Patient/456",
  "tenantId": "tenant-123",
  "overallScore": 75.5,
  "physicalHealthScore": 80.0,
  "mentalHealthScore": 70.0,
  "socialDeterminantsScore": 75.0,
  "preventiveCareScore": 72.0,
  "chronicDiseaseScore": 78.0,
  "calculatedAt": "2025-12-04T10:30:00Z",
  "previousScore": 70.0,
  "scoreDelta": 5.5,
  "significantChange": false,
  "scoreLevel": "good",
  "interpretation": "Good overall health. Minor improvements may be beneficial.",
  "trend": "improving"
}
```

---

### 2. GET /quality-measure/patients/{patientId}/health-score/history
**Purpose**: Retrieve historical health scores showing trends over time

**Features**:
- Returns list of historical scores ordered by date (most recent first)
- Configurable limit parameter (default: 50, max: 100)
- Useful for trend visualization and analysis

**Authorization**: `ANALYST`, `EVALUATOR`, `ADMIN`, `SUPER_ADMIN`

**Query Parameters**:
- `limit` (optional, default: 50, max: 100): Number of records to return

**Response**: `List<HealthScoreDTO>`

---

### 3. GET /quality-measure/patients/health-scores/at-risk
**Purpose**: Retrieve patients with health scores below a specified threshold

**Features**:
- Paginated results for efficient data retrieval
- Configurable threshold (default: 60.0, range: 0-100)
- Only returns latest score for each patient
- Sorted by overall score (ascending) - most at-risk first
- Page size capped at 100 to prevent excessive data retrieval

**Authorization**: `ANALYST`, `EVALUATOR`, `ADMIN`, `SUPER_ADMIN`

**Query Parameters**:
- `threshold` (optional, default: 60.0): Score threshold (0-100)
- `page` (optional, default: 0): Page number (0-based)
- `size` (optional, default: 20, max: 100): Page size

**Response**: `Page<HealthScoreDTO>`
```json
{
  "content": [
    {
      "patientId": "Patient/1",
      "overallScore": 45.0,
      ...
    },
    {
      "patientId": "Patient/2",
      "overallScore": 50.0,
      ...
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 42,
  "totalPages": 3
}
```

---

### 4. GET /quality-measure/patients/health-scores/significant-changes
**Purpose**: Retrieve patients with significant health score changes (≥10 points)

**Features**:
- Paginated results
- Configurable lookback period (default: 7 days, max: 90 days)
- Returns only scores marked as significant changes
- Sorted by calculation date (descending) - most recent first
- Useful for identifying patients requiring immediate attention

**Authorization**: `ANALYST`, `EVALUATOR`, `ADMIN`, `SUPER_ADMIN`

**Query Parameters**:
- `days` (optional, default: 7, max: 90): Lookback period in days
- `page` (optional, default: 0): Page number (0-based)
- `size` (optional, default: 20, max: 100): Page size

**Response**: `Page<HealthScoreDTO>`

---

## Technical Implementation

### Files Created/Modified

#### 1. HealthScoreController.java
**Location**: `/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/controller/HealthScoreController.java`

**Responsibilities**:
- REST endpoint definitions
- Request validation
- Parameter validation and capping
- Multi-tenant isolation via X-Tenant-ID header
- Role-based authorization via @PreAuthorize

**Key Features**:
- All endpoints require `X-Tenant-ID` header
- Patient IDs support FHIR format with slashes (`{patientId:.+}` pattern)
- Comprehensive input validation
- Automatic limit/size capping for safety
- Returns DTOs, never entities

#### 2. HealthScoreRepository.java (Enhanced)
**Location**: `/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/persistence/HealthScoreRepository.java`

**New Methods**:
```java
// Find latest scores below threshold with pagination
Page<HealthScoreEntity> findLatestScoresBelowThreshold(
    String tenantId,
    Double threshold,
    Pageable pageable
);

// Find significant changes since a given time with pagination
Page<HealthScoreEntity> findSignificantChangesSince(
    String tenantId,
    Instant since,
    Pageable pageable
);
```

**Implementation Details**:
- Native SQL query for at-risk patients to ensure only latest score per patient
- JPQL query for significant changes with time filtering
- Both methods support Spring Data pagination

#### 3. HealthScoreService.java (Enhanced)
**Location**: `/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/service/HealthScoreService.java`

**New Methods**:
```java
// Get at-risk patients (paginated)
Page<HealthScoreDTO> getAtRiskPatients(
    String tenantId,
    Double threshold,
    Pageable pageable
);

// Get significant changes (paginated)
Page<HealthScoreDTO> getSignificantChanges(
    String tenantId,
    Instant since,
    Pageable pageable
);
```

**Features**:
- Tenant-isolated data access
- Entity-to-DTO conversion
- Read-only transactions for query endpoints

#### 4. HealthScoreControllerTest.java
**Location**: `/modules/services/quality-measure-service/src/test/java/com/healthdata/quality/controller/HealthScoreControllerTest.java`

**Test Coverage**:
- ✅ Current health score retrieval (200, 404 cases)
- ✅ Patient IDs with slashes handling
- ✅ Tenant isolation verification
- ✅ History endpoint with limit parameter
- ✅ Limit capping at maximum values
- ✅ At-risk patients pagination
- ✅ Threshold validation
- ✅ Page size capping
- ✅ Significant changes pagination
- ✅ Lookback period capping
- ✅ Proper sorting verification

**Test Statistics**: 18+ test cases covering all endpoints and edge cases

---

## Security & Multi-Tenancy

### Authentication
All endpoints require authentication with one of the following roles:
- `ANALYST`
- `EVALUATOR`
- `ADMIN`
- `SUPER_ADMIN`

Implemented via Spring Security's `@PreAuthorize` annotation.

### Multi-Tenant Isolation
- **Required Header**: `X-Tenant-ID`
- **Validation**: `@NotBlank` constraint ensures header is present and non-empty
- **Service Layer**: All queries filter by `tenantId`
- **Repository Layer**: All queries include `tenantId` in WHERE clauses

### Data Protection
- Returns DTOs only (never entities)
- No sensitive data exposure
- Tenant data cannot cross tenant boundaries

---

## Performance Considerations

### Pagination
- **Purpose**: Prevent excessive data retrieval
- **Default Sizes**: Conservative defaults (20-50 records)
- **Maximum Caps**: Hard limits prevent abuse (100 max page size, 100 max history limit)
- **Database Efficiency**: Queries use indexed fields (patient_id, calculated_at, significant_change)

### Database Indexes
Existing indexes on `health_scores` table:
```sql
idx_hs_patient_calc: patient_id, calculated_at DESC
idx_hs_tenant_patient: tenant_id, patient_id
idx_hs_significant_change: significant_change, calculated_at DESC
```

These indexes support efficient queries for:
- Latest score retrieval
- Tenant isolation
- Significant change filtering

### Query Optimization
- **At-Risk Query**: Uses native SQL with subquery to get only latest score per patient
- **Significant Changes**: Uses indexed `significantChange` flag
- **History**: Leverages composite index on patient_id + calculated_at

---

## Usage Examples

### Example 1: Get Current Health Score
```bash
curl -X GET \
  'http://localhost:8080/quality-measure/patients/Patient/123/health-score' \
  -H 'X-Tenant-ID: tenant-abc' \
  -H 'Authorization: Bearer <token>'
```

### Example 2: Get Health Score History
```bash
curl -X GET \
  'http://localhost:8080/quality-measure/patients/Patient/123/health-score/history?limit=30' \
  -H 'X-Tenant-ID: tenant-abc' \
  -H 'Authorization: Bearer <token>'
```

### Example 3: Get At-Risk Patients
```bash
curl -X GET \
  'http://localhost:8080/quality-measure/patients/health-scores/at-risk?threshold=60.0&page=0&size=50' \
  -H 'X-Tenant-ID: tenant-abc' \
  -H 'Authorization: Bearer <token>'
```

### Example 4: Get Recent Significant Changes
```bash
curl -X GET \
  'http://localhost:8080/quality-measure/patients/health-scores/significant-changes?days=14&page=0&size=25' \
  -H 'X-Tenant-ID: tenant-abc' \
  -H 'Authorization: Bearer <token>'
```

---

## Integration Points

### Existing Services
The controller integrates with:
- **HealthScoreService**: Core business logic for score calculation and retrieval
- **HealthScoreRepository**: Data access layer with tenant isolation
- **Spring Security**: Authentication and authorization
- **Spring Data JPA**: Pagination and sorting

### DTOs Used
- **HealthScoreDTO**: Primary response object
  - Includes all component scores
  - Provides score interpretation and level
  - Calculates trend based on score delta
  - Backward compatible with nested ComponentScoresDTO

---

## Error Handling

### Validation Errors
- Missing `X-Tenant-ID` header → 400 Bad Request
- Invalid threshold (< 0 or > 100) → 400 Bad Request (IllegalArgumentException)
- Invalid pagination parameters → 400 Bad Request

### Not Found
- Patient has no health score → 404 Not Found

### Authorization Errors
- Missing authentication → 401 Unauthorized
- Insufficient role → 403 Forbidden

---

## Testing Strategy

### Unit Tests (TDD Approach)
1. **Tests Written First**: All tests in `HealthScoreControllerTest.java` define expected behavior
2. **Mock Dependencies**: Service layer mocked to isolate controller logic
3. **Comprehensive Coverage**: Tests cover happy paths, edge cases, and error scenarios
4. **Validation Testing**: All parameter validation and capping tested

### Integration Testing Recommendations
Future integration tests should verify:
- End-to-end flows with real database
- Tenant isolation at database level
- Performance with large datasets
- Security configuration

---

## Compliance with Requirements

✅ **All endpoints require X-Tenant-ID header** - Implemented via `@RequestHeader` with validation
✅ **All endpoints require authentication** - Implemented via `@PreAuthorize` with roles
✅ **Return DTOs, not entities** - All methods return `HealthScoreDTO` or `Page<HealthScoreDTO>`
✅ **Include pagination for list endpoints** - At-risk and significant changes use Spring Data pagination
✅ **Support FHIR patient IDs** - Path variable pattern `{patientId:.+}` supports slashes
✅ **TDD approach** - Comprehensive test suite written (18+ tests)
✅ **Input validation** - Threshold ranges, limit capping, non-null validation
✅ **Performance safeguards** - Maximum page sizes, capped lookback periods

---

## Future Enhancements

### Potential Improvements
1. **Filtering**: Add filters for score level (excellent, good, fair, poor, critical)
2. **Sorting**: Allow custom sorting on different fields
3. **Export**: Add CSV/Excel export for reports
4. **Caching**: Implement Redis caching for frequently accessed scores
5. **Webhooks**: Notify external systems of significant changes
6. **Bulk Operations**: Batch score calculation for multiple patients
7. **Analytics**: Aggregate statistics across patient populations

### Monitoring Recommendations
1. **Metrics**: Track endpoint usage, response times, error rates
2. **Alerts**: Monitor for high at-risk patient counts or unusual significant changes
3. **Audit Logging**: Log all score retrievals for compliance
4. **Performance**: Monitor query performance, especially for at-risk patients query

---

## Conclusion

The Health Score API endpoints have been successfully implemented following TDD principles with:
- Complete test coverage
- Robust security and multi-tenant isolation
- Performance optimizations
- Comprehensive documentation
- Production-ready code

All requirements have been met, and the implementation is ready for integration testing and deployment.
