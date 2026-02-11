# Measure Builder - API Documentation

**Version:** 1.0
**API Version:** v1
**Last Updated:** February 10, 2026
**Base URLs:**
- Service context path: `/quality-measure`
- Custom measure management: `/quality-measure/custom-measures`
- Measure versioning: `/quality-measure/api/v1/measures`
- AI endpoints: `/quality-measure/api/v1/measures/ai`
**Authentication:** JWT Bearer Token (or trusted headers in demo)

---

## Table of Contents

1. [Authentication](#authentication)
2. [Endpoints](#endpoints)
3. [Data Models](#data-models)
4. [Error Handling](#error-handling)
5. [Examples](#examples)
6. [Rate Limiting](#rate-limiting)

---

## Authentication

All API endpoints require JWT Bearer token authentication in production.
Demo environments may rely on trusted headers via the gateway.

**Header:**
```
Authorization: Bearer {token}
X-Tenant-ID: {tenant_id}
X-Auth-User-Id: {user_id} (demo/trusted headers)
X-Auth-Username: {username} (demo/trusted headers)
X-Auth-Roles: {roles} (demo/trusted headers)
X-Auth-Tenant-Ids: {tenant_ids} (demo/trusted headers)
X-Auth-Validated: {validator} (demo/trusted headers)
```

**Token Lifecycle:**
- Access token: 15 minutes
- Refresh token: 7 days
- Obtain via: `/api/v1/auth/login`

---

## Endpoints

### 1. Create Measure (Custom Measure Draft)

**POST** `/quality-measure/custom-measures`

Create a new measure.

**Request:**
```json
{
  "name": "Diabetes Care Quality",
  "description": "Measures HbA1C testing and control",
  "category": "CHRONIC_CONDITIONS",
  "version": "1.0"
}
```

**Response:** (201 Created)
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Diabetes Care Quality",
  "status": "DRAFT",
  "createdAt": "2026-01-18T10:30:00Z",
  "createdBy": "user@example.com",
  "tenant": "TENANT001"
}
```

---

### 2. Get Measure

**GET** `/quality-measure/custom-measures/{id}`

Retrieve measure details.

**Response:** (200 OK)
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Diabetes Care Quality",
  "description": "Measures HbA1C testing and control",
  "category": "CHRONIC_CONDITIONS",
  "blocks": [...],
  "cql": "define \"Denominator\": ...",
  "complexity": 6,
  "status": "DRAFT",
  "version": "1.0"
}
```

---

### 3. List Measures

**GET** `/quality-measure/custom-measures?status=PUBLISHED`

List all measures with filters.

**Parameters:**
- `status`: DRAFT, PUBLISHED, ARCHIVED
- `category`: Filter by category
- `status`: DRAFT, PUBLISHED, ARCHIVED

**Response:** (200 OK)
```json
{
  "items": [...],
  "total": 150,
  "limit": 20,
  "offset": 0
}
```

---

### 4. Update Measure

**PUT** `/quality-measure/custom-measures/{id}`

Update measure definition.

**Request:**
```json
{
  "name": "Diabetes Care Quality v2",
  "description": "Updated description",
  "blocks": [...],
  "sliders": {...}
}
```

**Response:** (200 OK)
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "DRAFT",
  "lastModified": "2026-01-18T11:00:00Z"
}
```

---

### 5. Delete Measure

**DELETE** `/quality-measure/custom-measures/{id}`

Delete a measure (DRAFT only).

**Response:** (204 No Content)

---

### 6. Validate Measure

**POST** `/api/v1/measures/{id}/validate`

Validate measure structure and CQL.

**Response:** (200 OK)
```json
{
  "isValid": true,
  "errors": [],
  "warnings": ["Consider adding more test data"],
  "estimatedComplexity": 6
}
```

---

### 7. Generate CQL

**POST** `/api/v1/measures/{id}/generate-cql`

Generate CQL from visual blocks.

**Request:**
```json
{
  "blocks": [...],
  "configuration": {...}
}
```

**Response:** (200 OK)
```json
{
  "cql": "define \"Denominator\": ...",
  "isValid": true,
  "warnings": []
}
```

---

### 8. Publish Measure

**POST** `/quality-measure/custom-measures/{id}/publish`

Publish measure (makes it read-only).

**Request:**
```json
{
  "notes": "Initial release - v1.0",
  "autoVersion": true
}
```

**Response:** (200 OK)
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PUBLISHED",
  "version": "1.0",
  "publishedAt": "2026-01-18T12:00:00Z"
}
```

---

### 9. Create New Version

**POST** `/api/v1/measures/{id}/versions`

Create new version of published measure.

**Request:**
```json
{
  "notes": "Fix A1C threshold from 8% to 9%"
}
```

**Response:** (201 Created)
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "version": "1.1",
  "status": "DRAFT",
  "createdAt": "2026-01-18T13:00:00Z"
}
```

---

### 10. Execute Evaluation

**POST** `/api/v1/measures/{id}/evaluate`

Run measure evaluation against patient cohort.

**Request:**
```json
{
  "patientIds": ["p001", "p002", "p003"],
  "measurementPeriod": {
    "start": "2025-01-01",
    "end": "2025-12-31"
  }
}
```

**Response:** (202 Accepted)
```json
{
  "evaluationId": "eval-550e8400",
  "status": "QUEUED",
  "startTime": "2026-01-18T14:00:00Z",
  "estimatedCompletionTime": "2026-01-18T14:05:00Z"
}
```

---

### 11. Get Evaluation Results

**GET** `/api/v1/evaluations/{evaluationId}`

Get evaluation results.

**Response:** (200 OK)
```json
{
  "evaluationId": "eval-550e8400",
  "status": "COMPLETED",
  "results": {
    "denominator": 150,
    "numerator": 120,
    "rate": 0.80,
    "passFail": "PASS"
  },
  "completionTime": "2026-01-18T14:04:30Z"
}
```

---

## Data Models

### Measure Object

```json
{
  "id": "uuid",
  "name": "string (required, max 255)",
  "description": "string (max 1000)",
  "category": "HEDIS | CUSTOM | OTHER",
  "version": "semantic version",
  "status": "DRAFT | PUBLISHED | ARCHIVED",
  "complexity": "1-10",
  "blocks": [...],
  "cql": "string (generated)",
  "createdAt": "ISO 8601",
  "createdBy": "email",
  "lastModified": "ISO 8601",
  "lastModifiedBy": "email",
  "tenant": "string",
  "metadata": {}
}
```

### Block Object

```json
{
  "id": "uuid",
  "type": "CONDITION | PROCEDURE | OBSERVATION | CONNECTOR | CALCULATION",
  "position": { "x": 100, "y": 200 },
  "configuration": {},
  "connections": ["block-id-1", "block-id-2"]
}
```

### Error Response

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Measure blocks have circular reference",
    "details": [
      {
        "field": "blocks[2]",
        "message": "Connects back to block[0]"
      }
    ]
  }
}
```

---

## Error Handling

### Status Codes

| Code | Meaning | Example |
|------|---------|---------|
| 200 | OK | Measure retrieved successfully |
| 201 | Created | New measure created |
| 202 | Accepted | Async operation queued |
| 204 | No Content | Measure deleted |
| 400 | Bad Request | Invalid measure data |
| 401 | Unauthorized | Missing/invalid token |
| 403 | Forbidden | Access denied |
| 404 | Not Found | Measure not found |
| 409 | Conflict | Cannot publish draft |
| 422 | Unprocessable | Validation errors |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Server Error | Internal server error |
| 503 | Unavailable | Service temporarily down |

---

## Examples

### Example 1: Create and Publish Measure

```bash
# 1. Create measure
curl -X POST http://localhost:8087/quality-measure/custom-measures \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: TENANT001" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Diabetes Care",
    "category": "CHRONIC_CONDITIONS"
  }'

# Response: { "id": "550e8400-..." }

MEASURE_ID="550e8400-..."

# 2. Add blocks and update
curl -X PUT http://localhost:8087/quality-measure/custom-measures/$MEASURE_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: TENANT001" \
  -H "Content-Type: application/json" \
  -d '{
    "blocks": [...]
  }'

# 3. Validate
curl -X POST http://localhost:8087/quality-measure/api/v1/measures/$MEASURE_ID/validate \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: TENANT001"

# Response: { "isValid": true }

# 4. Publish
curl -X POST http://localhost:8087/quality-measure/custom-measures/$MEASURE_ID/publish \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: TENANT001" \
  -H "Content-Type: application/json" \
  -d '{"notes": "v1.0 Release"}'
```

---

### Example 2: Run Evaluation

```bash
# Execute evaluation
curl -X POST http://localhost:8087/quality-measure/api/v1/measures/$MEASURE_ID/evaluate \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: TENANT001" \
  -H "Content-Type: application/json" \
  -d '{
    "patientIds": ["p001", "p002", "p003"],
    "measurementPeriod": {
      "start": "2025-01-01",
      "end": "2025-12-31"
    }
  }'

# Response: { "evaluationId": "eval-550e8400" }

EVAL_ID="eval-550e8400"

# Check results (after completion)
curl -X GET http://localhost:8087/quality-measure/api/v1/evaluations/$EVAL_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: TENANT001"

# Response:
# {
#   "results": {
#     "denominator": 3,
#     "numerator": 2,
#     "rate": 0.667,
#     "passFail": "PASS"
#   }
# }
```

---

## Rate Limiting

**Limits:**
- 1000 requests per minute (per user)
- 10000 requests per hour (per organization)

**Headers:**
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1642515600
```

---

## Related Documentation

- **OpenAPI Spec:** `openapi.yaml`
- **Integration Guide:** `MEASURE_BUILDER_INTEGRATION_GUIDE.md`
- **Webhooks:** `webhooks.md`
- **SDKs:** Available for Python, JavaScript, Java

---

**Status:** ✅ Complete
**Last Updated:** January 18, 2026
**Contact:** api-support@healthdatainmotion.com
