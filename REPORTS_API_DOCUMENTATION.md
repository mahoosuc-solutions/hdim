# Reports API Documentation

## Overview

The Reports API provides comprehensive functionality for generating, managing, and exporting quality measure reports. All endpoints support multi-tenant isolation and role-based access control.

**Base URL:** `http://localhost:8082/quality-measure`

**Version:** 1.0.0

**Service:** Quality Measure Service

---

## Table of Contents

1. [Authentication](#authentication)
2. [Common Headers](#common-headers)
3. [Error Responses](#error-responses)
4. [Endpoints](#endpoints)
   - [Save Patient Report](#1-save-patient-report)
   - [Save Population Report](#2-save-population-report)
   - [Get Saved Reports](#3-get-saved-reports)
   - [Get Report by ID](#4-get-report-by-id)
   - [Delete Report](#5-delete-report)
   - [Export Report to CSV](#6-export-report-to-csv)
   - [Export Report to Excel](#7-export-report-to-excel)
5. [Data Models](#data-models)
6. [Usage Examples](#usage-examples)

---

## Authentication

All endpoints require JWT authentication via Bearer token.

**Required Roles:**
- `ANALYST` - Read-only access to reports
- `EVALUATOR` - Can create and read reports
- `ADMIN` - Full access to reports
- `SUPER_ADMIN` - Full access across all tenants

---

## Common Headers

All requests must include the following headers:

```http
Authorization: Bearer <JWT_TOKEN>
X-Tenant-ID: <tenant-identifier>
Content-Type: application/json
```

### Header Descriptions

| Header | Required | Description |
|--------|----------|-------------|
| `Authorization` | Yes | JWT Bearer token for authentication |
| `X-Tenant-ID` | Yes | Tenant identifier for multi-tenant isolation |
| `Content-Type` | Yes* | application/json (except for export endpoints) |

*Not required for GET requests

---

## Error Responses

### Standard Error Format

```json
{
  "error": "Bad Request",
  "message": "Tenant ID is required",
  "timestamp": "2024-11-14T12:00:00"
}
```

### HTTP Status Codes

| Status Code | Description |
|-------------|-------------|
| `200 OK` | Request successful |
| `201 Created` | Resource created successfully |
| `204 No Content` | Resource deleted successfully |
| `400 Bad Request` | Invalid request parameters or missing required fields |
| `401 Unauthorized` | Missing or invalid authentication token |
| `403 Forbidden` | Insufficient permissions for the requested operation |
| `404 Not Found` | Requested resource does not exist |
| `500 Internal Server Error` | Server error occurred |

---

## Endpoints

### 1. Save Patient Report

Generate and save a quality report for a specific patient.

**Endpoint:** `POST /report/patient/save`

**Required Role:** `EVALUATOR`, `ADMIN`, or `SUPER_ADMIN`

#### Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `patient` | String (UUID) | Yes | Patient identifier |
| `name` | String | Yes | Report name/title |
| `createdBy` | String | No | User who created the report (defaults to "system") |

#### Request Example

```http
POST /quality-measure/report/patient/save?patient=550e8400-e29b-41d4-a716-446655440000&name=John%20Doe%20Quality%20Report&createdBy=dr.smith
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
X-Tenant-ID: acme-health
Content-Type: application/json
```

#### Response (201 Created)

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "tenantId": "acme-health",
  "reportType": "PATIENT",
  "reportName": "John Doe Quality Report",
  "description": null,
  "patientId": "550e8400-e29b-41d4-a716-446655440000",
  "year": null,
  "reportData": "{\"patientId\":\"550e8400-e29b-41d4-a716-446655440000\",\"totalMeasures\":5,\"compliantMeasures\":4,\"qualityScore\":80.0,\"measuresByCategory\":{\"HEDIS\":3,\"CMS\":2},\"careGapSummary\":\"{\\\"gaps\\\":1}\"}",
  "createdBy": "dr.smith",
  "createdAt": "2024-11-14T12:00:00",
  "generatedAt": "2024-11-14T12:00:00",
  "status": "COMPLETED",
  "version": 0
}
```

#### Error Responses

| Status | Condition |
|--------|-----------|
| `400 Bad Request` | Missing required parameters (patient, name, or X-Tenant-ID) |
| `400 Bad Request` | Invalid patient ID format (not a valid UUID) |
| `403 Forbidden` | User does not have EVALUATOR, ADMIN, or SUPER_ADMIN role |

---

### 2. Save Population Report

Generate and save a quality report for a population/year.

**Endpoint:** `POST /report/population/save`

**Required Role:** `EVALUATOR`, `ADMIN`, or `SUPER_ADMIN`

#### Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `year` | Integer | Yes | Report year (must be positive) |
| `name` | String | Yes | Report name/title |
| `createdBy` | String | No | User who created the report (defaults to "system") |

#### Request Example

```http
POST /quality-measure/report/population/save?year=2024&name=2024%20Population%20Quality%20Report&createdBy=admin
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
X-Tenant-ID: acme-health
Content-Type: application/json
```

#### Response (201 Created)

```json
{
  "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "tenantId": "acme-health",
  "reportType": "POPULATION",
  "reportName": "2024 Population Quality Report",
  "description": null,
  "patientId": null,
  "year": 2024,
  "reportData": "{\"year\":2024,\"uniquePatients\":1500,\"totalMeasures\":7500,\"compliantMeasures\":6000,\"overallScore\":80.0,\"measuresByCategory\":{\"HEDIS\":4500,\"CMS\":3000}}",
  "createdBy": "admin",
  "createdAt": "2024-11-14T12:00:00",
  "generatedAt": "2024-11-14T12:00:00",
  "status": "COMPLETED",
  "version": 0
}
```

#### Error Responses

| Status | Condition |
|--------|-----------|
| `400 Bad Request` | Missing required parameters (year, name, or X-Tenant-ID) |
| `400 Bad Request` | Invalid year (must be a positive number) |
| `403 Forbidden` | User does not have EVALUATOR, ADMIN, or SUPER_ADMIN role |

---

### 3. Get Saved Reports

Retrieve all saved reports for the tenant, with optional filtering by report type.

**Endpoint:** `GET /reports`

**Required Role:** `ANALYST`, `EVALUATOR`, `ADMIN`, or `SUPER_ADMIN`

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `type` | String | No | Filter by report type: `PATIENT`, `POPULATION`, or `CARE_GAP` |

#### Request Example

```http
GET /quality-measure/reports?type=PATIENT
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
X-Tenant-ID: acme-health
```

#### Response (200 OK)

```json
[
  {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "tenantId": "acme-health",
    "reportType": "PATIENT",
    "reportName": "John Doe Quality Report",
    "patientId": "550e8400-e29b-41d4-a716-446655440000",
    "createdBy": "dr.smith",
    "createdAt": "2024-11-14T12:00:00",
    "status": "COMPLETED"
  },
  {
    "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "tenantId": "acme-health",
    "reportType": "PATIENT",
    "reportName": "Jane Smith Quality Report",
    "patientId": "650e8400-e29b-41d4-a716-446655440001",
    "createdBy": "dr.jones",
    "createdAt": "2024-11-13T15:30:00",
    "status": "COMPLETED"
  }
]
```

#### Error Responses

| Status | Condition |
|--------|-----------|
| `400 Bad Request` | Missing X-Tenant-ID header |
| `403 Forbidden` | User does not have required role |

---

### 4. Get Report by ID

Retrieve a specific saved report by its ID.

**Endpoint:** `GET /reports/{reportId}`

**Required Role:** `ANALYST`, `EVALUATOR`, `ADMIN`, or `SUPER_ADMIN`

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `reportId` | String (UUID) | Yes | Report identifier |

#### Request Example

```http
GET /quality-measure/reports/a1b2c3d4-e5f6-7890-abcd-ef1234567890
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
X-Tenant-ID: acme-health
```

#### Response (200 OK)

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "tenantId": "acme-health",
  "reportType": "PATIENT",
  "reportName": "John Doe Quality Report",
  "description": "Comprehensive quality report for patient John Doe",
  "patientId": "550e8400-e29b-41d4-a716-446655440000",
  "year": null,
  "reportData": "{\"patientId\":\"550e8400-e29b-41d4-a716-446655440000\",\"totalMeasures\":5,\"compliantMeasures\":4,\"qualityScore\":80.0,\"measuresByCategory\":{\"HEDIS\":3,\"CMS\":2},\"careGapSummary\":\"{\\\"gaps\\\":1}\"}",
  "createdBy": "dr.smith",
  "createdAt": "2024-11-14T12:00:00",
  "generatedAt": "2024-11-14T12:00:00",
  "status": "COMPLETED",
  "version": 0
}
```

#### Error Responses

| Status | Condition |
|--------|-----------|
| `400 Bad Request` | Invalid report ID format (not a valid UUID) |
| `400 Bad Request` | Missing X-Tenant-ID header |
| `404 Not Found` | Report does not exist or belongs to different tenant |
| `403 Forbidden` | User does not have required role |

---

### 5. Delete Report

Delete a saved report by its ID.

**Endpoint:** `DELETE /reports/{reportId}`

**Required Role:** `ADMIN` or `SUPER_ADMIN`

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `reportId` | String (UUID) | Yes | Report identifier |

#### Request Example

```http
DELETE /quality-measure/reports/a1b2c3d4-e5f6-7890-abcd-ef1234567890
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
X-Tenant-ID: acme-health
```

#### Response (204 No Content)

No response body. HTTP status 204 indicates successful deletion.

#### Error Responses

| Status | Condition |
|--------|-----------|
| `400 Bad Request` | Invalid report ID format (not a valid UUID) |
| `400 Bad Request` | Missing X-Tenant-ID header |
| `404 Not Found` | Report does not exist or belongs to different tenant |
| `403 Forbidden` | User does not have ADMIN or SUPER_ADMIN role |

---

### 6. Export Report to CSV

Export a saved report to CSV format.

**Endpoint:** `GET /reports/{reportId}/export/csv`

**Required Role:** `ANALYST`, `EVALUATOR`, `ADMIN`, or `SUPER_ADMIN`

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `reportId` | String (UUID) | Yes | Report identifier |

#### Request Example

```http
GET /quality-measure/reports/a1b2c3d4-e5f6-7890-abcd-ef1234567890/export/csv
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
X-Tenant-ID: acme-health
```

#### Response (200 OK)

**Content-Type:** `text/csv`

**Content-Disposition:** `attachment; filename="John_Doe_Quality_Report.csv"`

```csv
Field,Value
Report ID,a1b2c3d4-e5f6-7890-abcd-ef1234567890
Report Name,John Doe Quality Report
Report Type,PATIENT
Description,Comprehensive quality report for patient John Doe
Tenant ID,acme-health
Patient ID,550e8400-e29b-41d4-a716-446655440000
Created By,dr.smith
Created At,2024-11-14 12:00:00
Generated At,2024-11-14 12:00:00
Status,COMPLETED

Report Data,
patientId,550e8400-e29b-41d4-a716-446655440000
totalMeasures,5
compliantMeasures,4
qualityScore,80.0
measuresByCategory.HEDIS,3
measuresByCategory.CMS,2
careGapSummary,"{\"gaps\":1}"
```

#### Error Responses

| Status | Condition |
|--------|-----------|
| `400 Bad Request` | Invalid report ID format (not a valid UUID) |
| `400 Bad Request` | Missing X-Tenant-ID header |
| `404 Not Found` | Report does not exist or belongs to different tenant |
| `403 Forbidden` | User does not have required role |
| `500 Internal Server Error` | Error generating CSV file |

---

### 7. Export Report to Excel

Export a saved report to Excel (.xlsx) format.

**Endpoint:** `GET /reports/{reportId}/export/excel`

**Required Role:** `ANALYST`, `EVALUATOR`, `ADMIN`, or `SUPER_ADMIN`

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `reportId` | String (UUID) | Yes | Report identifier |

#### Request Example

```http
GET /quality-measure/reports/a1b2c3d4-e5f6-7890-abcd-ef1234567890/export/excel
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
X-Tenant-ID: acme-health
```

#### Response (200 OK)

**Content-Type:** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`

**Content-Disposition:** `attachment; filename="John_Doe_Quality_Report.xlsx"`

Binary Excel file with two sheets:
1. **Report Metadata** - Contains all report metadata (ID, name, type, created by, etc.)
2. **Report Data** - Contains the parsed report data in a structured format

#### Excel Structure

**Sheet 1: Report Metadata**
| Field | Value |
|-------|-------|
| Report ID | a1b2c3d4-e5f6-7890-abcd-ef1234567890 |
| Report Name | John Doe Quality Report |
| Report Type | PATIENT |
| Tenant ID | acme-health |
| Patient ID | 550e8400-e29b-41d4-a716-446655440000 |
| Created By | dr.smith |
| Created At | 2024-11-14 12:00:00 |
| Status | COMPLETED |

**Sheet 2: Report Data**
| Field | Value |
|-------|-------|
| patientId | 550e8400-e29b-41d4-a716-446655440000 |
| totalMeasures | 5 |
| compliantMeasures | 4 |
| qualityScore | 80.0 |
| measuresByCategory.HEDIS | 3 |
| measuresByCategory.CMS | 2 |

#### Error Responses

| Status | Condition |
|--------|-----------|
| `400 Bad Request` | Invalid report ID format (not a valid UUID) |
| `400 Bad Request` | Missing X-Tenant-ID header |
| `404 Not Found` | Report does not exist or belongs to different tenant |
| `403 Forbidden` | User does not have required role |
| `500 Internal Server Error` | Error generating Excel file |

---

## Data Models

### SavedReportEntity

```json
{
  "id": "UUID",
  "tenantId": "string",
  "reportType": "PATIENT | POPULATION | CARE_GAP",
  "reportName": "string",
  "description": "string | null",
  "patientId": "UUID | null",
  "year": "integer | null",
  "reportData": "JSON string",
  "createdBy": "string",
  "createdAt": "ISO 8601 datetime",
  "generatedAt": "ISO 8601 datetime",
  "status": "GENERATING | COMPLETED | FAILED",
  "errorMessage": "string | null",
  "version": "integer"
}
```

### Report Data Structures

#### Patient Report Data

```json
{
  "patientId": "550e8400-e29b-41d4-a716-446655440000",
  "totalMeasures": 5,
  "compliantMeasures": 4,
  "qualityScore": 80.0,
  "measuresByCategory": {
    "HEDIS": 3,
    "CMS": 2
  },
  "careGapSummary": "{\"gaps\": 1}"
}
```

#### Population Report Data

```json
{
  "year": 2024,
  "uniquePatients": 1500,
  "totalMeasures": 7500,
  "compliantMeasures": 6000,
  "overallScore": 80.0,
  "measuresByCategory": {
    "HEDIS": 4500,
    "CMS": 3000
  }
}
```

---

## Usage Examples

### Example 1: Generate and Export Patient Report

```bash
# Step 1: Generate and save patient report
curl -X POST "http://localhost:8082/quality-measure/report/patient/save?patient=550e8400-e29b-41d4-a716-446655440000&name=John%20Doe%20Report&createdBy=dr.smith" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..." \
  -H "X-Tenant-ID: acme-health" \
  -H "Content-Type: application/json"

# Response:
# { "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890", ... }

# Step 2: Export to CSV
curl -X GET "http://localhost:8082/quality-measure/reports/a1b2c3d4-e5f6-7890-abcd-ef1234567890/export/csv" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..." \
  -H "X-Tenant-ID: acme-health" \
  --output john_doe_report.csv
```

### Example 2: List and Filter Reports

```bash
# Get all patient reports
curl -X GET "http://localhost:8082/quality-measure/reports?type=PATIENT" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..." \
  -H "X-Tenant-ID: acme-health"

# Get all reports (no filter)
curl -X GET "http://localhost:8082/quality-measure/reports" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..." \
  -H "X-Tenant-ID: acme-health"
```

### Example 3: Generate Population Report for 2024

```bash
curl -X POST "http://localhost:8082/quality-measure/report/population/save?year=2024&name=2024%20Annual%20Report&createdBy=admin" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..." \
  -H "X-Tenant-ID: acme-health" \
  -H "Content-Type: application/json"
```

### Example 4: Delete Old Report

```bash
curl -X DELETE "http://localhost:8082/quality-measure/reports/a1b2c3d4-e5f6-7890-abcd-ef1234567890" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..." \
  -H "X-Tenant-ID: acme-health"
```

### Example 5: Export to Excel

```bash
curl -X GET "http://localhost:8082/quality-measure/reports/a1b2c3d4-e5f6-7890-abcd-ef1234567890/export/excel" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..." \
  -H "X-Tenant-ID: acme-health" \
  --output report.xlsx
```

---

## Rate Limiting

Currently, there are no rate limits on the Reports API. However, it is recommended to:

- Cache report data on the client side when possible
- Avoid generating the same report multiple times
- Use saved reports for historical data instead of regenerating

---

## Multi-Tenant Isolation

All endpoints enforce strict tenant isolation:

1. The `X-Tenant-ID` header is **required** on all requests
2. Users can only access reports belonging to their tenant
3. Attempting to access reports from other tenants returns `404 Not Found`
4. Super admins can access reports across tenants by changing the `X-Tenant-ID` header

---

## Security Considerations

1. **Authentication**: All endpoints require valid JWT tokens
2. **Authorization**: Role-based access control is enforced on all endpoints
3. **Tenant Isolation**: Strict multi-tenant data isolation
4. **Filename Sanitization**: Export filenames are sanitized to prevent path traversal attacks
5. **Input Validation**: All request parameters are validated
6. **UUID Validation**: Report IDs must be valid UUIDs

---

## Performance Considerations

1. **Caching**: Report data is cached to improve performance
2. **Pagination**: Consider adding pagination for large report lists (future enhancement)
3. **Async Generation**: Large reports may benefit from async generation (future enhancement)
4. **Export Formats**: CSV is lighter than Excel for large datasets

---

## Future Enhancements

Planned features for future releases:

1. **PDF Export**: Export reports to PDF format with charts and visualizations
2. **Scheduled Reports**: Automated report generation on a schedule
3. **Email Distribution**: Automatically email reports to stakeholders
4. **Report Templates**: Customizable report templates
5. **Advanced Filtering**: More sophisticated filtering options
6. **Bulk Export**: Export multiple reports at once
7. **Report Comparison**: Compare reports side-by-side
8. **Data Retention**: Configurable data retention policies

---

## Support

For API support, please contact:
- **Email**: api-support@healthdata.com
- **Documentation**: https://docs.healthdata.com/api
- **Issue Tracker**: https://github.com/healthdata/platform/issues

---

**Document Version:** 1.0.0
**Last Updated:** 2024-11-14
**API Version:** 1.0.0
