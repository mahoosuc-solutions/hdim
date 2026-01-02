# Patient Service API

The Patient Service provides comprehensive patient health record aggregation, timeline views, and health status dashboards.

**Base Path:** `/patient`

---

## Health Record Endpoints

### Get Comprehensive Health Record

Retrieves all patient health data as a FHIR Bundle.

```http
GET /patient/health-record
```

**Headers:**
| Header | Required | Description |
|--------|----------|-------------|
| `X-Tenant-ID` | Yes | Tenant identifier |
| `Authorization` | Yes | Bearer token |

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `patient` | string | Yes | Patient ID |

**Response:** `200 OK`
```json
{
  "resourceType": "Bundle",
  "type": "collection",
  "entry": [
    {
      "resource": {
        "resourceType": "Patient",
        "id": "patient-123"
      }
    }
  ]
}
```

**Content-Type:** `application/fhir+json`

---

### Get Allergies

```http
GET /patient/allergies
```

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `patient` | string | - | Patient ID (required) |
| `onlyCritical` | boolean | false | Return only critical allergies |

**Response:** `200 OK` - FHIR Bundle with AllergyIntolerance resources

---

### Get Immunizations

```http
GET /patient/immunizations
```

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `patient` | string | - | Patient ID (required) |
| `onlyCompleted` | boolean | false | Return only completed immunizations |

**Response:** `200 OK` - FHIR Bundle with Immunization resources

---

### Get Medications

```http
GET /patient/medications
```

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `patient` | string | - | Patient ID (required) |
| `onlyActive` | boolean | true | Return only active medications |

**Response:** `200 OK` - FHIR Bundle with MedicationRequest resources

---

### Get Conditions

```http
GET /patient/conditions
```

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `patient` | string | - | Patient ID (required) |
| `onlyActive` | boolean | true | Return only active conditions |

**Response:** `200 OK` - FHIR Bundle with Condition resources

---

### Get Procedures

```http
GET /patient/procedures
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `patient` | string | Yes | Patient ID |

**Response:** `200 OK` - FHIR Bundle with Procedure resources

---

### Get Vital Signs

```http
GET /patient/vitals
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `patient` | string | Yes | Patient ID |

**Response:** `200 OK` - FHIR Bundle with Observation (vital signs) resources

---

### Get Lab Results

```http
GET /patient/labs
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `patient` | string | Yes | Patient ID |

**Response:** `200 OK` - FHIR Bundle with Observation (laboratory) resources

---

### Get Encounters

```http
GET /patient/encounters
```

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `patient` | string | - | Patient ID (required) |
| `onlyActive` | boolean | false | Return only active encounters |

**Response:** `200 OK` - FHIR Bundle with Encounter resources

---

### Get Care Plans

```http
GET /patient/care-plans
```

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `patient` | string | - | Patient ID (required) |
| `onlyActive` | boolean | true | Return only active care plans |

**Response:** `200 OK` - FHIR Bundle with CarePlan resources

---

## Timeline Endpoints

### Get Patient Timeline

Returns chronological list of all patient events.

```http
GET /patient/timeline
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `patient` | string | Yes | Patient ID |

**Response:** `200 OK`
```json
[
  {
    "date": "2025-01-15",
    "resourceType": "Encounter",
    "resourceId": "enc-123",
    "description": "Office visit",
    "category": "encounter"
  }
]
```

---

### Get Timeline by Date Range

```http
GET /patient/timeline/by-date
```

**Query Parameters:**
| Parameter | Type | Format | Description |
|-----------|------|--------|-------------|
| `patient` | string | - | Patient ID (required) |
| `startDate` | date | YYYY-MM-DD | Start date (required) |
| `endDate` | date | YYYY-MM-DD | End date (required) |

**Response:** `200 OK` - List of timeline events within date range

---

### Get Timeline by Resource Type

```http
GET /patient/timeline/by-type
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `patient` | string | Yes | Patient ID |
| `resourceType` | string | Yes | FHIR resource type (e.g., "Encounter", "Observation") |

**Response:** `200 OK` - List of timeline events of specified type

---

### Get Timeline Summary by Month

```http
GET /patient/timeline/summary
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `patient` | string | Yes | Patient ID |
| `year` | integer | Yes | Year to summarize |

**Response:** `200 OK`
```json
{
  "January": 5,
  "February": 3,
  "March": 8
}
```

---

## Health Status Endpoints

### Get Health Status Summary

```http
GET /patient/health-status
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `patient` | string | Yes | Patient ID |

**Response:** `200 OK`
```json
{
  "overallStatus": "STABLE",
  "activeConditions": 3,
  "activeMedications": 5,
  "recentEncounters": 2,
  "openCareGaps": 1,
  "riskLevel": "MODERATE"
}
```

---

### Get Medication Summary

```http
GET /patient/medication-summary
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `patient` | string | Yes | Patient ID |

**Response:** `200 OK`
```json
{
  "totalMedications": 8,
  "activeMedications": 5,
  "highRiskMedications": 1,
  "controlledSubstances": 0,
  "recentChanges": 2
}
```

---

### Get Allergy Summary

```http
GET /patient/allergy-summary
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `patient` | string | Yes | Patient ID |

**Response:** `200 OK`
```json
{
  "totalAllergies": 3,
  "criticalAllergies": 1,
  "drugAllergies": 2,
  "foodAllergies": 1
}
```

---

### Get Condition Summary

```http
GET /patient/condition-summary
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `patient` | string | Yes | Patient ID |

**Response:** `200 OK`
```json
{
  "totalConditions": 5,
  "activeConditions": 3,
  "chronicConditions": 2,
  "resolvedConditions": 2
}
```

---

### Get Immunization Summary

```http
GET /patient/immunization-summary
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `patient` | string | Yes | Patient ID |

**Response:** `200 OK`
```json
{
  "totalImmunizations": 15,
  "completedImmunizations": 14,
  "dueImmunizations": 1,
  "overdueImmunizations": 0
}
```

---

## Health Check

### Service Health

```http
GET /patient/_health
```

**Response:** `200 OK`
```json
{
  "status": "UP",
  "service": "patient-service",
  "timestamp": "2025-01-15"
}
```

---

## Authorization

All endpoints require one of these roles:
- `ANALYST`
- `EVALUATOR`
- `ADMIN`
- `SUPER_ADMIN`

---

## Examples

### Get All Patient Data

```bash
curl -X GET "https://api.example.com/patient/health-record?patient=patient-123" \
  -H "Authorization: Bearer eyJhbG..." \
  -H "X-Tenant-ID: demo-tenant" \
  -H "Accept: application/fhir+json"
```

### Get Active Medications

```bash
curl -X GET "https://api.example.com/patient/medications?patient=patient-123&onlyActive=true" \
  -H "Authorization: Bearer eyJhbG..." \
  -H "X-Tenant-ID: demo-tenant"
```

### Get Timeline for 2025

```bash
curl -X GET "https://api.example.com/patient/timeline/by-date?patient=patient-123&startDate=2025-01-01&endDate=2025-12-31" \
  -H "Authorization: Bearer eyJhbG..." \
  -H "X-Tenant-ID: demo-tenant"
```
