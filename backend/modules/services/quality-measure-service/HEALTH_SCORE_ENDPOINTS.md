# Health Score API Endpoints - Quick Reference

## Base Path
`/quality-measure/patients`

## Authentication
All endpoints require:
- **Authorization**: Bearer token with roles: `ANALYST`, `EVALUATOR`, `ADMIN`, or `SUPER_ADMIN`
- **X-Tenant-ID**: Required header for multi-tenant isolation

---

## Endpoints

### 1. Get Current Health Score
```
GET /quality-measure/patients/{patientId}/health-score
```

**Parameters:**
- `patientId` (path): Patient identifier (supports FHIR format like `Patient/123`)

**Response:** `200 OK` with `HealthScoreDTO` or `404 Not Found`

**Example:**
```bash
curl -X GET \
  'http://localhost:8080/quality-measure/patients/Patient/123/health-score' \
  -H 'X-Tenant-ID: tenant-abc' \
  -H 'Authorization: Bearer eyJhbGc...'
```

---

### 2. Get Health Score History
```
GET /quality-measure/patients/{patientId}/health-score/history
```

**Parameters:**
- `patientId` (path): Patient identifier
- `limit` (query, optional): Max records to return (default: 50, max: 100)

**Response:** `200 OK` with `List<HealthScoreDTO>`

**Example:**
```bash
curl -X GET \
  'http://localhost:8080/quality-measure/patients/Patient/123/health-score/history?limit=30' \
  -H 'X-Tenant-ID: tenant-abc' \
  -H 'Authorization: Bearer eyJhbGc...'
```

---

### 3. Get At-Risk Patients
```
GET /quality-measure/patients/health-scores/at-risk
```

**Parameters:**
- `threshold` (query, optional): Score threshold (default: 60.0, range: 0-100)
- `page` (query, optional): Page number, 0-based (default: 0)
- `size` (query, optional): Page size (default: 20, max: 100)

**Response:** `200 OK` with `Page<HealthScoreDTO>`

**Example:**
```bash
curl -X GET \
  'http://localhost:8080/quality-measure/patients/health-scores/at-risk?threshold=60.0&page=0&size=50' \
  -H 'X-Tenant-ID: tenant-abc' \
  -H 'Authorization: Bearer eyJhbGc...'
```

**Response Structure:**
```json
{
  "content": [
    {
      "patientId": "Patient/1",
      "overallScore": 45.0,
      "scoreLevel": "poor",
      ...
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 50,
    "sort": {
      "sorted": true,
      "unsorted": false
    }
  },
  "totalElements": 42,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

---

### 4. Get Significant Changes
```
GET /quality-measure/patients/health-scores/significant-changes
```

**Parameters:**
- `days` (query, optional): Lookback period in days (default: 7, max: 90)
- `page` (query, optional): Page number, 0-based (default: 0)
- `size` (query, optional): Page size (default: 20, max: 100)

**Response:** `200 OK` with `Page<HealthScoreDTO>`

**Example:**
```bash
curl -X GET \
  'http://localhost:8080/quality-measure/patients/health-scores/significant-changes?days=14&page=0&size=25' \
  -H 'X-Tenant-ID: tenant-abc' \
  -H 'Authorization: Bearer eyJhbGc...'
```

---

## HealthScoreDTO Structure

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
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
  "changeReason": null,
  "scoreLevel": "good",
  "interpretation": "Good overall health. Minor improvements may be beneficial.",
  "trend": "improving",
  "componentScores": {
    "physical": 80,
    "mental": 70,
    "social": 75,
    "preventive": 72,
    "chronicDisease": 78
  }
}
```

### Score Levels
- **excellent**: 90-100
- **good**: 75-89
- **fair**: 60-74
- **poor**: 40-59
- **critical**: 0-39

### Trends
- **improving**: Score delta > 5.0
- **declining**: Score delta < -5.0
- **stable**: Score delta between -5.0 and 5.0
- **new**: No previous score

---

## Error Responses

### 400 Bad Request
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Threshold must be between 0 and 100"
}
```

### 401 Unauthorized
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication required"
}
```

### 403 Forbidden
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Insufficient permissions"
}
```

### 404 Not Found
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Health score not found for patient"
}
```

---

## Common Use Cases

### Dashboard: Show At-Risk Patients
```bash
# Get patients with scores below 60, sorted by score (worst first)
GET /quality-measure/patients/health-scores/at-risk?threshold=60.0&size=50
```

### Care Coordinator: Review Significant Changes
```bash
# Get patients with major score changes in last 7 days
GET /quality-measure/patients/health-scores/significant-changes?days=7
```

### Patient Chart: Show Health Trend
```bash
# Get last 30 scores for trend chart
GET /quality-measure/patients/Patient/123/health-score/history?limit=30
```

### Provider Dashboard: Current Patient Score
```bash
# Get latest score during patient visit
GET /quality-measure/patients/Patient/123/health-score
```

---

## Performance Notes

- **Pagination**: Always use pagination for list endpoints to avoid large data transfers
- **Caching**: Current scores can be cached (5 minute TTL recommended)
- **History**: Limit parameter prevents excessive data retrieval
- **At-Risk Query**: Optimized to return only latest score per patient
- **Indexes**: All queries use database indexes for efficient execution

---

## Testing

Run controller tests:
```bash
./gradlew :modules:services:quality-measure-service:test --tests HealthScoreControllerTest
```

Run all quality-measure-service tests:
```bash
./gradlew :modules:services:quality-measure-service:test
```
