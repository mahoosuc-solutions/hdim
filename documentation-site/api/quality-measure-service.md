# Quality Measure Service API

The Quality Measure Service provides HEDIS quality measure calculations, population reports, and report management.

**Base Path:** `/quality-measure`

---

## Measure Calculation

### Calculate Measure for Patient

Executes a quality measure calculation for a specific patient.

```http
POST /quality-measure/calculate
```

**Headers:**
| Header | Required | Description |
|--------|----------|-------------|
| `X-Tenant-ID` | Yes | Tenant identifier |
| `Authorization` | Yes | Bearer token |

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `patient` | UUID | Yes | Patient ID |
| `measure` | string | Yes | Measure ID (e.g., "CMS130") |
| `createdBy` | string | No | User initiating calculation (default: "system") |

**Response:** `201 Created`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "patientId": "patient-uuid",
  "measureId": "CMS130",
  "calculationDate": "2025-01-15T10:30:00Z",
  "status": "COMPLETED",
  "inNumerator": true,
  "inDenominator": true,
  "complianceRate": 1.0,
  "durationMs": 1250,
  "createdBy": "user@example.com"
}
```

**Authorization:** `EVALUATOR`, `ADMIN`, `SUPER_ADMIN`

---

### Get Patient Results

Retrieves measure results for a patient.

```http
GET /quality-measure/results
```

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `patient` | UUID | - | Patient ID (optional - returns all if omitted) |
| `page` | integer | 0 | Page number |
| `size` | integer | 20 | Items per page |

**Response:** `200 OK`
```json
[
  {
    "id": "result-uuid",
    "patientId": "patient-uuid",
    "measureId": "CMS130",
    "calculationDate": "2025-01-15T10:30:00Z",
    "status": "COMPLETED",
    "inNumerator": true,
    "complianceRate": 1.0
  }
]
```

**Authorization:** `ANALYST`, `EVALUATOR`, `ADMIN`, `SUPER_ADMIN`

---

### Get Quality Score

Retrieves overall quality score for a patient.

```http
GET /quality-measure/score
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `patient` | UUID | Yes | Patient ID |

**Response:** `200 OK`
```json
{
  "patientId": "patient-uuid",
  "overallScore": 85.5,
  "totalMeasures": 12,
  "compliantMeasures": 10,
  "nonCompliantMeasures": 2,
  "notApplicable": 0,
  "calculationDate": "2025-01-15T10:30:00Z"
}
```

---

## Reports

### Get Patient Quality Report

Generates detailed quality report for a patient.

```http
GET /quality-measure/report/patient
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `patient` | UUID | Yes | Patient ID |

**Response:** `200 OK`
```json
{
  "patientId": "patient-uuid",
  "reportDate": "2025-01-15",
  "overallCompliance": 83.3,
  "measureResults": [
    {
      "measureId": "CMS130",
      "measureName": "Colorectal Cancer Screening",
      "status": "COMPLIANT",
      "lastEvaluated": "2025-01-15T10:30:00Z"
    }
  ],
  "careGaps": [
    {
      "gapId": "gap-uuid",
      "measureId": "CMS125",
      "priority": "HIGH",
      "recommendedAction": "Schedule mammogram"
    }
  ],
  "trends": {
    "previousCompliance": 75.0,
    "change": 8.3,
    "direction": "IMPROVING"
  }
}
```

---

### Get Population Quality Report

Generates aggregate quality report for entire population.

```http
GET /quality-measure/report/population
```

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `year` | integer | Current year | Reporting year |

**Response:** `200 OK`
```json
{
  "reportYear": 2025,
  "totalPatients": 1500,
  "totalEvaluations": 18000,
  "overallCompliance": 78.5,
  "measurePerformance": [
    {
      "measureId": "CMS130",
      "measureName": "Colorectal Cancer Screening",
      "eligiblePatients": 450,
      "compliantPatients": 380,
      "complianceRate": 84.4,
      "target": 80.0,
      "status": "ABOVE_TARGET"
    }
  ],
  "trends": {
    "previousYearCompliance": 72.3,
    "yearOverYearChange": 6.2
  },
  "generatedAt": "2025-01-15T10:30:00Z"
}
```

---

## Population Batch Calculations

### Start Population Calculation

Initiates batch calculation for all patients.

```http
POST /quality-measure/population/calculate
```

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `fhirServerUrl` | string | Configured URL | FHIR server base URL |
| `createdBy` | string | "system" | User initiating calculation |

**Response:** `202 Accepted`
```json
{
  "jobId": "job-uuid",
  "status": "STARTED",
  "message": "Population calculation job started. Use /population/jobs/job-uuid to track progress.",
  "tenantId": "demo-tenant"
}
```

**Authorization:** `ADMIN`, `SUPER_ADMIN`

---

### Get Job Status

Retrieves status of a batch calculation job.

```http
GET /quality-measure/population/jobs/{jobId}
```

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `jobId` | string | Job identifier |

**Response:** `200 OK`
```json
{
  "jobId": "job-uuid",
  "tenantId": "demo-tenant",
  "status": "CALCULATING",
  "createdBy": "admin@example.com",
  "startedAt": "2025-01-15T10:30:00Z",
  "completedAt": null,
  "totalPatients": 1500,
  "totalMeasures": 12,
  "totalCalculations": 18000,
  "completedCalculations": 9500,
  "successfulCalculations": 9450,
  "failedCalculations": 50,
  "progressPercent": 52.8,
  "duration": "PT5M30S",
  "errors": []
}
```

**Job Status Values:**
| Status | Description |
|--------|-------------|
| `PENDING` | Job queued |
| `CALCULATING` | Job running |
| `COMPLETED` | Job finished successfully |
| `FAILED` | Job failed |
| `CANCELLED` | Job cancelled by user |

---

### Get All Jobs

Lists all batch calculation jobs for tenant.

```http
GET /quality-measure/population/jobs
```

**Response:** `200 OK` - Array of job status objects

---

### Cancel Job

Cancels a running batch calculation job.

```http
POST /quality-measure/population/jobs/{jobId}/cancel
```

**Response:** `200 OK`
```json
{
  "jobId": "job-uuid",
  "status": "CANCELLED",
  "message": "Job successfully cancelled"
}
```

**Authorization:** `ADMIN`, `SUPER_ADMIN`

---

## Saved Reports

### Save Patient Report

Saves a patient quality report for later access.

```http
POST /quality-measure/report/patient/save
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `patient` | UUID | Yes | Patient ID |
| `name` | string | Yes | Report name |
| `createdBy` | string | No | User creating report |

**Response:** `201 Created`
```json
{
  "id": "report-uuid",
  "reportName": "Q1 2025 Patient Report",
  "reportType": "PATIENT",
  "patientId": "patient-uuid",
  "createdAt": "2025-01-15T10:30:00Z",
  "createdBy": "user@example.com"
}
```

---

### Save Population Report

```http
POST /quality-measure/report/population/save
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `year` | integer | Yes | Reporting year |
| `name` | string | Yes | Report name |
| `createdBy` | string | No | User creating report |

**Response:** `201 Created`

---

### Get Saved Reports

```http
GET /quality-measure/reports
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `type` | string | No | Filter by report type ("PATIENT" or "POPULATION") |

**Response:** `200 OK` - Array of saved report objects

---

### Get Saved Report by ID

```http
GET /quality-measure/reports/{reportId}
```

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `reportId` | UUID | Report identifier |

**Response:** `200 OK` - Saved report object with full data

---

### Delete Saved Report

```http
DELETE /quality-measure/reports/{reportId}
```

**Response:** `204 No Content`

**Authorization:** `ADMIN`, `SUPER_ADMIN`

---

## Export

### Export Report to CSV

```http
GET /quality-measure/reports/{reportId}/export/csv
```

**Response:** `200 OK`
- Content-Type: `text/csv`
- Content-Disposition: `attachment; filename="report-name.csv"`

---

### Export Report to Excel

```http
GET /quality-measure/reports/{reportId}/export/excel
```

**Response:** `200 OK`
- Content-Type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- Content-Disposition: `attachment; filename="report-name.xlsx"`

---

## Health Check

```http
GET /quality-measure/_health
```

**Response:** `200 OK`
```json
{
  "status": "UP",
  "service": "quality-measure-service",
  "timestamp": "2025-01-15"
}
```

---

## Examples

### Run Single Evaluation

```bash
curl -X POST "https://api.example.com/quality-measure/calculate?patient=550e8400-e29b-41d4-a716-446655440000&measure=CMS130" \
  -H "Authorization: Bearer eyJhbG..." \
  -H "X-Tenant-ID: demo-tenant"
```

### Start Batch Calculation

```bash
curl -X POST "https://api.example.com/quality-measure/population/calculate?createdBy=admin" \
  -H "Authorization: Bearer eyJhbG..." \
  -H "X-Tenant-ID: demo-tenant"
```

### Export Report to Excel

```bash
curl -X GET "https://api.example.com/quality-measure/reports/report-uuid/export/excel" \
  -H "Authorization: Bearer eyJhbG..." \
  -H "X-Tenant-ID: demo-tenant" \
  -o "quality-report.xlsx"
```
