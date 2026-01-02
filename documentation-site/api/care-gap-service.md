# Care Gap Service API

The Care Gap Service provides care gap identification, management, and reporting functionality.

**Base Path:** `/care-gap`

---

## Care Gap Identification

### Identify All Care Gaps

Identifies all care gaps for a patient by evaluating all applicable quality measures.

```http
POST /care-gap/identify
```

**Headers:**
| Header | Required | Description |
|--------|----------|-------------|
| `X-Tenant-ID` | Yes | Tenant identifier |
| `Authorization` | Yes | Bearer token |

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `patient` | UUID | Yes | Patient identifier |
| `createdBy` | string | No | User initiating identification (default: "system") |

**Response:** `201 Created`
```json
[
  {
    "id": "gap-uuid",
    "tenantId": "demo-tenant",
    "patientId": "patient-uuid",
    "measureId": "CMS125",
    "measureName": "Breast Cancer Screening",
    "measureCategory": "Preventive",
    "gapType": "SCREENING",
    "priority": "HIGH",
    "status": "OPEN",
    "identifiedDate": "2025-01-15",
    "dueDate": "2025-03-15",
    "daysOverdue": 0,
    "recommendedAction": "Schedule mammogram",
    "createdBy": "system",
    "createdAt": "2025-01-15T10:30:00Z"
  }
]
```

**Authorization:** `EVALUATOR`, `ADMIN`, `SUPER_ADMIN`

---

### Identify Care Gaps for Specific Measure

Identifies care gaps for a specific CQL library/measure.

```http
POST /care-gap/identify/{library}
```

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `library` | string | CQL library name |

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `patient` | UUID | Yes | Patient identifier |
| `createdBy` | string | No | User initiating identification |

**Response:** `201 Created` - List of identified care gaps for that measure

---

### Refresh Care Gaps

Re-evaluates all measures and updates care gap status.

```http
POST /care-gap/refresh
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `patient` | UUID | Yes | Patient identifier |
| `createdBy` | string | No | User initiating refresh |

**Response:** `200 OK` - List of current active care gaps

---

### Close Care Gap

Closes a care gap with documentation.

```http
POST /care-gap/close
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `gapId` | UUID | Yes | Care gap identifier |
| `closedBy` | string | Yes | User closing the gap |
| `closureReason` | string | No | Reason for closure |
| `closureAction` | string | No | Action taken to close gap |

**Closure Reasons:**
| Reason | Description |
|--------|-------------|
| `COMPLETED` | Gap fully addressed |
| `NOT_APPLICABLE` | Patient excluded from measure |
| `PATIENT_DECLINED` | Patient refused intervention |
| `OTHER` | Custom reason (add in notes) |

**Response:** `200 OK`
```json
{
  "id": "gap-uuid",
  "status": "CLOSED",
  "closedDate": "2025-01-15",
  "closedBy": "nurse@example.com",
  "closureReason": "COMPLETED",
  "closureAction": "Mammogram completed 2025-01-15"
}
```

---

## Care Gap Queries

### Get Open Care Gaps

Retrieves all open care gaps for a patient.

```http
GET /care-gap/open
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `patient` | UUID | Yes | Patient identifier |

**Response:** `200 OK` - List of open care gaps

---

### Get High Priority Care Gaps

Retrieves high priority care gaps for a patient.

```http
GET /care-gap/high-priority
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `patient` | UUID | Yes | Patient identifier |

**Response:** `200 OK` - List of high priority care gaps

**Priority Levels:**
| Priority | Description |
|----------|-------------|
| `CRITICAL` | Overdue >90 days |
| `HIGH` | Overdue 31-90 days |
| `MEDIUM` | Overdue 1-30 days |
| `LOW` | Not yet overdue |

---

### Get Overdue Care Gaps

Retrieves care gaps that are past their due date.

```http
GET /care-gap/overdue
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `patient` | UUID | Yes | Patient identifier |

**Response:** `200 OK` - List of overdue care gaps

---

### Get Upcoming Care Gaps

Retrieves care gaps due within a specified timeframe.

```http
GET /care-gap/upcoming
```

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `patient` | UUID | - | Patient identifier (required) |
| `days` | integer | 30 | Days to look ahead |

**Response:** `200 OK` - List of upcoming care gaps

---

## Statistics and Reports

### Get Care Gap Statistics

Retrieves summary statistics for a patient's care gaps.

```http
GET /care-gap/stats
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `patient` | UUID | Yes | Patient identifier |

**Response:** `200 OK`
```json
{
  "patientId": "patient-uuid",
  "totalGaps": 5,
  "openGaps": 3,
  "closedGaps": 2,
  "highPriorityGaps": 1,
  "overdueGaps": 1,
  "averageDaysOpen": 45,
  "closureRate": 40.0,
  "lastUpdated": "2025-01-15T10:30:00Z"
}
```

---

### Get Care Gap Summary

Retrieves detailed summary of patient's care gaps.

```http
GET /care-gap/summary
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `patient` | UUID | Yes | Patient identifier |

**Response:** `200 OK`
```json
{
  "patientId": "patient-uuid",
  "overallStatus": "ATTENTION_NEEDED",
  "openGaps": [
    {
      "id": "gap-uuid",
      "measureName": "Breast Cancer Screening",
      "priority": "HIGH",
      "daysOverdue": 15,
      "recommendedAction": "Schedule mammogram"
    }
  ],
  "recentlyClosedGaps": [
    {
      "id": "gap-uuid-2",
      "measureName": "Colorectal Cancer Screening",
      "closedDate": "2025-01-10"
    }
  ],
  "upcomingGaps": [],
  "recommendations": [
    "Schedule mammogram appointment within 2 weeks",
    "Review HbA1c results at next visit"
  ]
}
```

---

### Get Gaps by Category

Groups care gaps by measure category.

```http
GET /care-gap/by-category
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `patient` | UUID | Yes | Patient identifier |

**Response:** `200 OK`
```json
{
  "Preventive": 2,
  "Chronic Disease": 1,
  "Behavioral Health": 0,
  "Medication": 1
}
```

---

### Get Gaps by Priority

Groups care gaps by priority level.

```http
GET /care-gap/by-priority
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `patient` | UUID | Yes | Patient identifier |

**Response:** `200 OK`
```json
{
  "CRITICAL": 0,
  "HIGH": 2,
  "MEDIUM": 1,
  "LOW": 1
}
```

---

### Get Population Gap Report

Generates population-level care gap report for the tenant.

```http
GET /care-gap/population-report
```

**Response:** `200 OK`
```json
{
  "tenantId": "demo-tenant",
  "reportDate": "2025-01-15",
  "totalPatients": 1500,
  "patientsWithGaps": 450,
  "totalOpenGaps": 1200,
  "gapsByCategory": {
    "Preventive": 600,
    "Chronic Disease": 350,
    "Behavioral Health": 150,
    "Medication": 100
  },
  "gapsByPriority": {
    "CRITICAL": 50,
    "HIGH": 200,
    "MEDIUM": 400,
    "LOW": 550
  },
  "topMeasuresWithGaps": [
    {
      "measureId": "CMS125",
      "measureName": "Breast Cancer Screening",
      "gapCount": 180
    }
  ],
  "closureRateTrend": {
    "currentMonth": 25.5,
    "previousMonth": 22.3,
    "change": 3.2
  }
}
```

---

## Care Gap Object Structure

### Care Gap Entity

```json
{
  "id": "gap-uuid",
  "tenantId": "demo-tenant",
  "patientId": "patient-uuid",
  "measureId": "CMS125",
  "measureName": "Breast Cancer Screening",
  "measureCategory": "Preventive",
  "gapType": "SCREENING",
  "priority": "HIGH",
  "status": "OPEN",
  "identifiedDate": "2025-01-01",
  "dueDate": "2025-03-15",
  "daysOverdue": 0,
  "recommendedAction": "Schedule mammogram",
  "interventions": [
    {
      "type": "PHONE_CALL",
      "date": "2025-01-10",
      "outcome": "LEFT_MESSAGE",
      "notes": "Left voicemail for callback"
    }
  ],
  "closedDate": null,
  "closedBy": null,
  "closureReason": null,
  "closureAction": null,
  "createdBy": "system",
  "createdAt": "2025-01-01T10:00:00Z",
  "updatedAt": "2025-01-10T14:30:00Z"
}
```

### Gap Types

| Type | Description |
|------|-------------|
| `SCREENING` | Preventive screening needed |
| `LAB` | Laboratory test required |
| `MEDICATION` | Medication-related action |
| `ASSESSMENT` | Clinical assessment needed |
| `FOLLOW_UP` | Follow-up visit required |
| `IMMUNIZATION` | Vaccination needed |
| `REFERRAL` | Specialty referral needed |

### Gap Status

| Status | Description |
|--------|-------------|
| `OPEN` | Gap is active and unresolved |
| `PENDING` | Action in progress |
| `CLOSED` | Gap has been resolved |

---

## Health Check

```http
GET /care-gap/_health
```

**Response:** `200 OK`
```json
{
  "status": "UP",
  "service": "care-gap-service",
  "timestamp": "2025-01-15"
}
```

---

## Authorization

| Operation | Required Roles |
|-----------|----------------|
| Identify gaps (POST) | `EVALUATOR`, `ADMIN`, `SUPER_ADMIN` |
| Close gap (POST) | `EVALUATOR`, `ADMIN`, `SUPER_ADMIN` |
| Query gaps (GET) | `ANALYST`, `EVALUATOR`, `ADMIN`, `SUPER_ADMIN` |

---

## Examples

### Identify Care Gaps for Patient

```bash
curl -X POST "https://api.example.com/care-gap/identify?patient=patient-uuid&createdBy=nurse" \
  -H "Authorization: Bearer eyJhbG..." \
  -H "X-Tenant-ID: demo-tenant"
```

### Get High Priority Gaps

```bash
curl -X GET "https://api.example.com/care-gap/high-priority?patient=patient-uuid" \
  -H "Authorization: Bearer eyJhbG..." \
  -H "X-Tenant-ID: demo-tenant"
```

### Close a Care Gap

```bash
curl -X POST "https://api.example.com/care-gap/close?gapId=gap-uuid&closedBy=nurse&closureReason=COMPLETED&closureAction=Mammogram+completed" \
  -H "Authorization: Bearer eyJhbG..." \
  -H "X-Tenant-ID: demo-tenant"
```

### Get Population Report

```bash
curl -X GET "https://api.example.com/care-gap/population-report" \
  -H "Authorization: Bearer eyJhbG..." \
  -H "X-Tenant-ID: demo-tenant"
```
