# Batch API Quick Reference

**Team B Implementation - Quality Measure Service**

---

## Batch Publish Measures

**Endpoint:** `POST /quality-measure/custom-measures/batch-publish`

**Purpose:** Publish multiple DRAFT measures to PUBLISHED status in a single transaction.

**Request:**
```json
{
  "measureIds": [
    "uuid-1",
    "uuid-2"
  ]
}
```

**Response:**
```json
{
  "publishedCount": 2,
  "skippedCount": 0,
  "failedCount": 0,
  "errors": []
}
```

**Authorization:** `ANALYST`, `ADMIN`, `SUPER_ADMIN`

**Curl Example:**
```bash
curl -X POST "http://localhost:8083/quality-measure/custom-measures/batch-publish" \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/json" \
  -d '{
    "measureIds": [
      "550e8400-e29b-41d4-a716-446655440000",
      "550e8400-e29b-41d4-a716-446655440001"
    ]
  }'
```

**Behavior:**
- Only DRAFT measures are published
- Already PUBLISHED measures are skipped (counted in `skippedCount`)
- Sets `publishedDate` timestamp
- Validates tenant ownership
- All-or-nothing transaction
- HIPAA audit logged

---

## Batch Delete Measures

**Endpoint:** `DELETE /quality-measure/custom-measures/batch-delete`

**Purpose:** Soft delete multiple custom measures in a single transaction.

**Request:**
```json
{
  "measureIds": [
    "uuid-1",
    "uuid-2"
  ],
  "force": false
}
```

**Response:**
```json
{
  "deletedCount": 2,
  "failedCount": 0,
  "errors": [],
  "measuresInUse": []
}
```

**Authorization:** `ADMIN`, `SUPER_ADMIN`

**Curl Example:**
```bash
curl -X DELETE "http://localhost:8083/quality-measure/custom-measures/batch-delete" \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/json" \
  -d '{
    "measureIds": [
      "550e8400-e29b-41d4-a716-446655440000"
    ],
    "force": false
  }'
```

**Behavior:**
- Uses soft delete (sets `deletedAt` and `deletedBy`)
- Checks if measures have evaluations
- If `force: false` and evaluations exist, deletion fails
- If `force: true`, deletes regardless of evaluations
- Validates tenant ownership
- HIPAA audit logged

**Parameters:**
- `force` (boolean): Override in-use protection

---

## Single Measure Delete

**Endpoint:** `DELETE /quality-measure/custom-measures/{id}`

**Purpose:** Soft delete a single custom measure.

**Response:** `204 No Content`

**Authorization:** `ANALYST`, `ADMIN`, `SUPER_ADMIN`

**Curl Example:**
```bash
curl -X DELETE "http://localhost:8083/quality-measure/custom-measures/550e8400-e29b-41d4-a716-446655440000" \
  -H "X-Tenant-ID: tenant-1"
```

**Behavior:**
- Soft delete (sets `deletedAt` and `deletedBy`)
- Validates tenant ownership
- HIPAA audit logged

---

## Patient Soft Delete

**Endpoint:** `DELETE /fhir/Patient/{id}`

**Service:** FHIR Service (port 8082)

**Purpose:** Soft delete a patient record for HIPAA compliance.

**Response:** `204 No Content`

**Curl Example:**
```bash
curl -X DELETE "http://localhost:8082/fhir/Patient/550e8400-e29b-41d4-a716-446655440000" \
  -H "X-Tenant-Id: tenant-1"
```

**Behavior:**
- Soft delete (sets `deletedAt` and `deletedBy`)
- Clears patient cache
- Publishes Kafka event
- HIPAA audit logged with `AuditAction.DELETE`

---

## Error Responses

### 400 Bad Request
```json
{
  "publishedCount": 0,
  "skippedCount": 0,
  "failedCount": 0,
  "errors": ["Measures not found or not accessible: [uuid-1, uuid-2]"]
}
```

**Causes:**
- Empty `measureIds` array
- Invalid UUIDs
- Measures not found for tenant

### 500 Internal Server Error
**Causes:**
- Database connection failure
- Transaction rollback
- Unexpected errors

---

## Multi-Tenant Isolation

All endpoints enforce tenant isolation:
- `X-Tenant-ID` header required
- Repository queries filter by `tenantId`
- Cross-tenant access returns 404 or 500

**Test:**
```bash
# Create measure in tenant-1
MEASURE_ID=$(curl -s -X POST "http://localhost:8083/quality-measure/custom-measures" \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","category":"CUSTOM","year":2024}' | jq -r '.id')

# Try to access from tenant-2 (should fail)
curl -X DELETE "http://localhost:8083/quality-measure/custom-measures/$MEASURE_ID" \
  -H "X-Tenant-ID: tenant-2"
# Expected: 500 or 404
```

---

## Testing Script

Run comprehensive tests:
```bash
cd /home/webemo-aaron/projects/healthdata-in-motion/backend
./test-batch-endpoints.sh
```

**Prerequisites:**
- Services running (Quality Measure Service on 8083)
- PostgreSQL accessible
- `jq` installed for JSON parsing

---

## Database Verification

### Check Soft Deleted Measures
```sql
SELECT id, name, status, published_date, deleted_at, deleted_by
FROM custom_measures
WHERE tenant_id = 'tenant-1'
  AND deleted_at IS NOT NULL
ORDER BY deleted_at DESC;
```

### Check Audit Logs
```sql
SELECT action, resource_type, resource_id, actor, occurred_at, outcome
FROM audit_events
WHERE resource_type = 'CustomMeasure'
  AND action IN ('UPDATE', 'DELETE')
ORDER BY occurred_at DESC
LIMIT 10;
```

### Check Patient Soft Deletes
```sql
SELECT id, first_name, last_name, deleted_at, deleted_by
FROM patients
WHERE tenant_id = 'tenant-1'
  AND deleted_at IS NOT NULL
ORDER BY deleted_at DESC;
```

---

## Integration Test

Run automated tests:
```bash
cd /home/webemo-aaron/projects/healthdata-in-motion/backend
./gradlew :quality-measure-service:test --tests "CustomMeasureBatchApiIntegrationTest"
```

**Test Coverage:**
- ✅ Batch publish draft measures
- ✅ Skip already-published measures
- ✅ Enforce tenant isolation
- ✅ Reject empty measure IDs
- ✅ Batch delete measures
- ✅ Single measure delete
- ✅ Soft delete verification

---

## Production Deployment

### Liquibase Migration
```bash
./gradlew :quality-measure-service:liquibaseUpdate
```

**Migration:** `0004-add-soft-delete-columns.xml`

**Changes:**
- `custom_measures.published_date` (TIMESTAMP)
- `custom_measures.deleted_at` (TIMESTAMP)
- `custom_measures.deleted_by` (VARCHAR(100))
- `patients.deleted_at` (TIMESTAMP)
- `patients.deleted_by` (VARCHAR(100))
- Indexes for performance

### Rollback
```bash
./gradlew :quality-measure-service:liquibaseRollback -PliquibaseCommandValue=1
```

---

**Last Updated:** 2025-11-18
**Team:** Team B - Backend API Implementation
