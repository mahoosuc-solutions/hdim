# CQL Engine Service API

The CQL Engine Service provides Clinical Quality Language (CQL) expression evaluation and management.

**Base Path:** `/api/v1/cql`

---

## Evaluation Management

### Create and Execute Evaluation

Creates a new CQL evaluation and executes it immediately.

```http
POST /api/v1/cql/evaluations
```

**Headers:**
| Header | Required | Description |
|--------|----------|-------------|
| `X-Tenant-ID` | Yes | Tenant identifier |
| `Authorization` | Yes | Bearer token |

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `libraryId` | UUID | Yes | CQL library identifier |
| `patientId` | UUID | Yes | Patient identifier |

**Response:** `201 Created`
```json
{
  "id": "eval-uuid",
  "tenantId": "demo-tenant",
  "libraryId": "library-uuid",
  "patientId": "patient-uuid",
  "status": "COMPLETED",
  "result": {
    "inInitialPopulation": true,
    "inDenominator": true,
    "inNumerator": true,
    "exclusions": [],
    "complianceStatus": "COMPLIANT"
  },
  "executedAt": "2025-01-15T10:30:00Z",
  "durationMs": 1250,
  "errorMessage": null
}
```

**Authorization:** `EVALUATOR`, `ADMIN`, `SUPER_ADMIN`

---

### Execute Existing Evaluation

Re-executes an existing evaluation.

```http
POST /api/v1/cql/evaluations/{id}/execute
```

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Evaluation identifier |

**Response:** `200 OK` - Updated evaluation object

---

### Get All Evaluations

Retrieves paginated list of evaluations for tenant.

```http
GET /api/v1/cql/evaluations
```

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | integer | 0 | Page number |
| `size` | integer | 20 | Items per page |
| `sort` | string | - | Sort field and direction |

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": "eval-uuid",
      "libraryId": "library-uuid",
      "patientId": "patient-uuid",
      "status": "COMPLETED",
      "executedAt": "2025-01-15T10:30:00Z"
    }
  ],
  "totalElements": 1500,
  "totalPages": 75,
  "number": 0,
  "size": 20
}
```

---

### Get Evaluation by ID

```http
GET /api/v1/cql/evaluations/{id}
```

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Evaluation identifier |

**Response:** `200 OK` - Evaluation object
**Response:** `404 Not Found` - If evaluation doesn't exist

---

### Get Evaluations for Patient

```http
GET /api/v1/cql/evaluations/patient/{patientId}
```

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `patientId` | UUID | Patient identifier |

**Query Parameters:** Standard pagination (page, size)

**Response:** `200 OK` - Paginated list of patient evaluations

---

### Get Evaluations for Library

```http
GET /api/v1/cql/evaluations/library/{libraryId}
```

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `libraryId` | UUID | Library identifier |

**Query Parameters:** Standard pagination (page, size)

**Response:** `200 OK` - Paginated list of library evaluations

---

### Get Latest Evaluation

Gets the most recent evaluation for a patient and library combination.

```http
GET /api/v1/cql/evaluations/patient/{patientId}/library/{libraryId}/latest
```

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `patientId` | UUID | Patient identifier |
| `libraryId` | UUID | Library identifier |

**Response:** `200 OK` - Latest evaluation object
**Response:** `404 Not Found` - If no evaluation exists

---

### Get Evaluations by Status

```http
GET /api/v1/cql/evaluations/by-status/{status}
```

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `status` | string | Evaluation status |

**Status Values:**
| Status | Description |
|--------|-------------|
| `PENDING` | Awaiting execution |
| `RUNNING` | Currently executing |
| `COMPLETED` | Successfully completed |
| `FAILED` | Execution failed |

**Query Parameters:** Standard pagination (page, size)

**Response:** `200 OK` - Paginated list of evaluations

---

### Get Evaluations by Date Range

```http
GET /api/v1/cql/evaluations/date-range
```

**Query Parameters:**
| Parameter | Type | Format | Description |
|-----------|------|--------|-------------|
| `start` | datetime | ISO 8601 | Start datetime |
| `end` | datetime | ISO 8601 | End datetime |

**Response:** `200 OK` - List of evaluations within range

---

### Get Patient Evaluations by Date Range

```http
GET /api/v1/cql/evaluations/patient/{patientId}/date-range
```

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `patientId` | UUID | Patient identifier |

**Query Parameters:**
| Parameter | Type | Format | Description |
|-----------|------|--------|-------------|
| `start` | datetime | ISO 8601 | Start datetime |
| `end` | datetime | ISO 8601 | End datetime |

**Response:** `200 OK` - List of patient evaluations within range

---

### Get Successful Evaluations for Patient

```http
GET /api/v1/cql/evaluations/patient/{patientId}/successful
```

**Response:** `200 OK` - List of successful evaluations

---

## Retry and Batch Operations

### Retry Failed Evaluation

Re-attempts a failed evaluation.

```http
POST /api/v1/cql/evaluations/{id}/retry
```

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Evaluation identifier |

**Response:** `200 OK` - Retried evaluation object

**Authorization:** `EVALUATOR`, `ADMIN`, `SUPER_ADMIN`

---

### Batch Evaluate

Evaluates multiple patients for a single library.

```http
POST /api/v1/cql/evaluations/batch
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `libraryId` | UUID | Yes | CQL library identifier |

**Request Body:**
```json
[
  "patient-uuid-1",
  "patient-uuid-2",
  "patient-uuid-3"
]
```

**Response:** `201 Created`
```json
[
  {
    "id": "eval-uuid-1",
    "patientId": "patient-uuid-1",
    "status": "COMPLETED"
  },
  {
    "id": "eval-uuid-2",
    "patientId": "patient-uuid-2",
    "status": "COMPLETED"
  }
]
```

---

### Get Failed Evaluations for Retry

```http
GET /api/v1/cql/evaluations/failed-for-retry
```

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `hoursBack` | integer | 24 | Hours to look back |

**Response:** `200 OK` - List of failed evaluations eligible for retry

---

## Statistics and Counts

### Count Evaluations by Status

```http
GET /api/v1/cql/evaluations/count/by-status/{status}
```

**Response:** `200 OK`
```json
150
```

---

### Count Evaluations for Library

```http
GET /api/v1/cql/evaluations/count/library/{libraryId}
```

**Response:** `200 OK` - Count of evaluations

---

### Count Evaluations for Patient

```http
GET /api/v1/cql/evaluations/count/patient/{patientId}
```

**Response:** `200 OK` - Count of evaluations

---

### Get Average Duration for Library

```http
GET /api/v1/cql/evaluations/avg-duration/library/{libraryId}
```

**Response:** `200 OK`
```json
1250.5
```

(Average duration in milliseconds)

---

## Data Retention

### Delete Old Evaluations

Removes evaluations older than specified retention period.

```http
DELETE /api/v1/cql/evaluations/old
```

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `daysToRetain` | integer | 90 | Days of data to keep |

**Response:** `204 No Content`

**Authorization:** `ADMIN`, `SUPER_ADMIN`

---

## Evaluation Result Structure

### Result Object

```json
{
  "id": "eval-uuid",
  "tenantId": "demo-tenant",
  "libraryId": "library-uuid",
  "libraryName": "CMS130 Colorectal Cancer Screening",
  "libraryVersion": "1.0.0",
  "patientId": "patient-uuid",
  "status": "COMPLETED",
  "result": {
    "inInitialPopulation": true,
    "inDenominator": true,
    "inDenominatorExclusion": false,
    "inNumerator": true,
    "inNumeratorExclusion": false,
    "exclusions": [],
    "complianceStatus": "COMPLIANT",
    "rawResults": {
      "InitialPopulation": true,
      "Denominator": true,
      "Numerator": true
    }
  },
  "createdAt": "2025-01-15T10:30:00Z",
  "executedAt": "2025-01-15T10:30:01Z",
  "durationMs": 1250,
  "errorMessage": null
}
```

### Compliance Status Values

| Status | Description |
|--------|-------------|
| `COMPLIANT` | In numerator, meets measure criteria |
| `NON_COMPLIANT` | In denominator but not numerator |
| `EXCLUDED` | Excluded from measure |
| `NOT_APPLICABLE` | Not in initial population |

---

## Examples

### Create and Run Evaluation

```bash
curl -X POST "https://api.example.com/api/v1/cql/evaluations?libraryId=lib-uuid&patientId=patient-uuid" \
  -H "Authorization: Bearer eyJhbG..." \
  -H "X-Tenant-ID: demo-tenant"
```

### Batch Evaluate Patients

```bash
curl -X POST "https://api.example.com/api/v1/cql/evaluations/batch?libraryId=lib-uuid" \
  -H "Authorization: Bearer eyJhbG..." \
  -H "X-Tenant-ID: demo-tenant" \
  -H "Content-Type: application/json" \
  -d '["patient-1", "patient-2", "patient-3"]'
```

### Get Evaluation Statistics

```bash
curl -X GET "https://api.example.com/api/v1/cql/evaluations/count/by-status/COMPLETED" \
  -H "Authorization: Bearer eyJhbG..." \
  -H "X-Tenant-ID: demo-tenant"
```
