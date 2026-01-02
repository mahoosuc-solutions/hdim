# Reports API - Quick Start Guide

## TL;DR

```bash
# 1. Save a patient report
POST /quality-measure/report/patient/save?patient={uuid}&name={name}

# 2. List all reports
GET /quality-measure/reports

# 3. Export to CSV
GET /quality-measure/reports/{id}/export/csv

# 4. Export to Excel
GET /quality-measure/reports/{id}/export/excel

# 5. Delete a report
DELETE /quality-measure/reports/{id}
```

---

## Quick Examples

### 1. Generate Patient Report

```bash
curl -X POST "http://localhost:8082/quality-measure/report/patient/save?patient=550e8400-e29b-41d4-a716-446655440000&name=Patient%20Report" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-Tenant-ID: your-tenant"
```

### 2. Generate Population Report

```bash
curl -X POST "http://localhost:8082/quality-measure/report/population/save?year=2024&name=2024%20Report" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-Tenant-ID: your-tenant"
```

### 3. List All Reports

```bash
curl -X GET "http://localhost:8082/quality-measure/reports" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-Tenant-ID: your-tenant"
```

### 4. Filter by Type

```bash
curl -X GET "http://localhost:8082/quality-measure/reports?type=PATIENT" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-Tenant-ID: your-tenant"
```

### 5. Get Specific Report

```bash
curl -X GET "http://localhost:8082/quality-measure/reports/{report-id}" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-Tenant-ID: your-tenant"
```

### 6. Export to CSV

```bash
curl -X GET "http://localhost:8082/quality-measure/reports/{report-id}/export/csv" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-Tenant-ID: your-tenant" \
  -o report.csv
```

### 7. Export to Excel

```bash
curl -X GET "http://localhost:8082/quality-measure/reports/{report-id}/export/excel" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-Tenant-ID: your-tenant" \
  -o report.xlsx
```

### 8. Delete Report

```bash
curl -X DELETE "http://localhost:8082/quality-measure/reports/{report-id}" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-Tenant-ID: your-tenant"
```

---

## Common Use Cases

### Use Case 1: Monthly Quality Reporting

```bash
#!/bin/bash
# Generate monthly patient reports

TENANT_ID="your-tenant"
TOKEN="your-jwt-token"

# Get list of patients
PATIENTS=("patient-id-1" "patient-id-2" "patient-id-3")

# Generate report for each patient
for patient_id in "${PATIENTS[@]}"; do
  curl -X POST "http://localhost:8082/quality-measure/report/patient/save?patient=$patient_id&name=Monthly%20Report&createdBy=automated" \
    -H "Authorization: Bearer $TOKEN" \
    -H "X-Tenant-ID: $TENANT_ID"
done
```

### Use Case 2: Annual Compliance Report

```bash
#!/bin/bash
# Generate and export annual population report

TENANT_ID="your-tenant"
TOKEN="your-jwt-token"
YEAR=2024

# Generate report
RESPONSE=$(curl -s -X POST "http://localhost:8082/quality-measure/report/population/save?year=$YEAR&name=Annual%20Compliance%20Report" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: $TENANT_ID")

# Extract report ID
REPORT_ID=$(echo $RESPONSE | jq -r '.id')

# Export to Excel
curl -X GET "http://localhost:8082/quality-measure/reports/$REPORT_ID/export/excel" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: $TENANT_ID" \
  -o "annual_report_$YEAR.xlsx"
```

### Use Case 3: Report Cleanup

```bash
#!/bin/bash
# Delete reports older than 90 days

TENANT_ID="your-tenant"
TOKEN="your-jwt-token"

# Get all reports
REPORTS=$(curl -s -X GET "http://localhost:8082/quality-measure/reports" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: $TENANT_ID")

# Parse and delete old reports (implementation depends on your scripting)
# This is a placeholder - actual implementation would parse dates and delete
```

---

## JavaScript/TypeScript Example

```typescript
// Reports API Client
class ReportsApiClient {
  private baseUrl = 'http://localhost:8082/quality-measure';
  private token: string;
  private tenantId: string;

  constructor(token: string, tenantId: string) {
    this.token = token;
    this.tenantId = tenantId;
  }

  private getHeaders() {
    return {
      'Authorization': `Bearer ${this.token}`,
      'X-Tenant-ID': this.tenantId,
      'Content-Type': 'application/json'
    };
  }

  async savePatientReport(patientId: string, reportName: string, createdBy = 'system') {
    const response = await fetch(
      `${this.baseUrl}/report/patient/save?patient=${patientId}&name=${encodeURIComponent(reportName)}&createdBy=${createdBy}`,
      {
        method: 'POST',
        headers: this.getHeaders()
      }
    );
    return response.json();
  }

  async savePopulationReport(year: number, reportName: string, createdBy = 'system') {
    const response = await fetch(
      `${this.baseUrl}/report/population/save?year=${year}&name=${encodeURIComponent(reportName)}&createdBy=${createdBy}`,
      {
        method: 'POST',
        headers: this.getHeaders()
      }
    );
    return response.json();
  }

  async getAllReports(type?: 'PATIENT' | 'POPULATION' | 'CARE_GAP') {
    const url = type
      ? `${this.baseUrl}/reports?type=${type}`
      : `${this.baseUrl}/reports`;

    const response = await fetch(url, {
      headers: this.getHeaders()
    });
    return response.json();
  }

  async getReportById(reportId: string) {
    const response = await fetch(
      `${this.baseUrl}/reports/${reportId}`,
      {
        headers: this.getHeaders()
      }
    );
    return response.json();
  }

  async exportToCsv(reportId: string): Promise<Blob> {
    const response = await fetch(
      `${this.baseUrl}/reports/${reportId}/export/csv`,
      {
        headers: {
          'Authorization': `Bearer ${this.token}`,
          'X-Tenant-ID': this.tenantId
        }
      }
    );
    return response.blob();
  }

  async exportToExcel(reportId: string): Promise<Blob> {
    const response = await fetch(
      `${this.baseUrl}/reports/${reportId}/export/excel`,
      {
        headers: {
          'Authorization': `Bearer ${this.token}`,
          'X-Tenant-ID': this.tenantId
        }
      }
    );
    return response.blob();
  }

  async deleteReport(reportId: string) {
    const response = await fetch(
      `${this.baseUrl}/reports/${reportId}`,
      {
        method: 'DELETE',
        headers: this.getHeaders()
      }
    );
    return response.status === 204;
  }
}

// Usage
const client = new ReportsApiClient('your-jwt-token', 'your-tenant-id');

// Generate patient report
const report = await client.savePatientReport(
  '550e8400-e29b-41d4-a716-446655440000',
  'John Doe Quality Report',
  'dr.smith'
);

// List all patient reports
const reports = await client.getAllReports('PATIENT');

// Export to CSV
const csvBlob = await client.exportToCsv(report.id);
const url = URL.createObjectURL(csvBlob);
const a = document.createElement('a');
a.href = url;
a.download = 'report.csv';
a.click();
```

---

## Postman Collection

Import this collection into Postman:

```json
{
  "info": {
    "name": "Reports API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8082/quality-measure"
    },
    {
      "key": "token",
      "value": "your-jwt-token"
    },
    {
      "key": "tenantId",
      "value": "your-tenant-id"
    }
  ],
  "item": [
    {
      "name": "Save Patient Report",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          },
          {
            "key": "X-Tenant-ID",
            "value": "{{tenantId}}"
          }
        ],
        "url": {
          "raw": "{{baseUrl}}/report/patient/save?patient=550e8400-e29b-41d4-a716-446655440000&name=Test Report",
          "host": ["{{baseUrl}}"],
          "path": ["report", "patient", "save"],
          "query": [
            {
              "key": "patient",
              "value": "550e8400-e29b-41d4-a716-446655440000"
            },
            {
              "key": "name",
              "value": "Test Report"
            }
          ]
        }
      }
    },
    {
      "name": "Get All Reports",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          },
          {
            "key": "X-Tenant-ID",
            "value": "{{tenantId}}"
          }
        ],
        "url": {
          "raw": "{{baseUrl}}/reports",
          "host": ["{{baseUrl}}"],
          "path": ["reports"]
        }
      }
    },
    {
      "name": "Export to CSV",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          },
          {
            "key": "X-Tenant-ID",
            "value": "{{tenantId}}"
          }
        ],
        "url": {
          "raw": "{{baseUrl}}/reports/{reportId}/export/csv",
          "host": ["{{baseUrl}}"],
          "path": ["reports", "{reportId}", "export", "csv"]
        }
      }
    }
  ]
}
```

---

## Testing

```bash
# Run all Reports tests
./gradlew :modules:services:quality-measure-service:test --tests "*SavedReport*" --tests "*QualityReportServiceSave*" --tests "*ReportExport*"

# Test coverage
# - 16 Repository tests
# - 10 Service tests
# - 20 CRUD API tests
# - 9 Export Service tests
# - 10 Export API tests
# Total: 65 tests
```

---

## Troubleshooting

### Issue: 400 Bad Request - Tenant ID is required

**Solution:** Ensure `X-Tenant-ID` header is included in all requests.

```bash
-H "X-Tenant-ID: your-tenant-id"
```

### Issue: 404 Not Found

**Possible causes:**
1. Report doesn't exist
2. Report belongs to different tenant
3. Invalid report ID format

**Solution:** Verify report ID and tenant ID are correct.

### Issue: 403 Forbidden

**Cause:** Insufficient permissions

**Solution:** Check user role requirements for the endpoint.

### Issue: Export file is empty

**Cause:** Report might have no data or export failed

**Solution:** Check report status is "COMPLETED" before exporting.

---

## Best Practices

1. **Always include error handling** in your client code
2. **Cache report data** when possible
3. **Use meaningful report names** for better organization
4. **Delete old reports** to manage storage
5. **Filter by type** when listing reports for better performance
6. **Validate UUIDs** before making requests
7. **Use the createdBy parameter** to track who generated reports

---

## Performance Tips

1. **CSV vs Excel**: Use CSV for large datasets (faster, smaller file size)
2. **Pagination**: For large lists, consider implementing client-side pagination
3. **Caching**: Report data is cached server-side for 15 minutes
4. **Batch operations**: Generate multiple reports in parallel when needed

---

## Next Steps

1. Read the [Full API Documentation](./REPORTS_API_DOCUMENTATION.md)
2. Check out the [Architecture Guide](./REPORTS_INTEGRATION_ARCHITECTURE.md)
3. Explore the [Frontend Integration Guide](#) (coming soon)

---

**Quick Start Version:** 1.0.0
**Last Updated:** 2024-11-14
