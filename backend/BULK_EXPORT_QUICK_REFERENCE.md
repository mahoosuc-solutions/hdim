# FHIR Bulk Data Export - Quick Reference

## API Endpoints

### Start Export
```bash
# System-level export
curl -X GET "http://localhost:8085/fhir/api/v1/\$export" \
  -H "Authorization: Bearer <TOKEN>"

# With filters
curl -X GET "http://localhost:8085/fhir/api/v1/\$export?_type=Patient,Observation&_since=2024-01-01T00:00:00Z" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Prefer: respond-async"

# Patient-level export
curl -X GET "http://localhost:8085/fhir/api/v1/Patient/\$export" \
  -H "Authorization: Bearer <TOKEN>"

# Group-level export
curl -X GET "http://localhost:8085/fhir/api/v1/Group/{groupId}/\$export" \
  -H "Authorization: Bearer <TOKEN>"
```

### Check Status
```bash
curl -X GET "http://localhost:8085/fhir/api/v1/\$export-poll-status/{jobId}" \
  -H "Authorization: Bearer <TOKEN>"
```

### Cancel Export
```bash
curl -X DELETE "http://localhost:8085/fhir/api/v1/\$export-poll-status/{jobId}" \
  -H "Authorization: Bearer <TOKEN>"
```

### Download File
```bash
curl -X GET "http://localhost:8085/fhir/api/v1/download/{jobId}/{fileName}" \
  -H "Authorization: Bearer <TOKEN>" \
  -o output.ndjson
```

## File Locations

```
/home/mahoosuc-solutions/projects/hdim-master/hdim-master/backend/modules/services/fhir-service/

src/main/java/com/healthdata/fhir/bulk/
├── BulkExportConfig.java          # Configuration
├── BulkExportJob.java             # Entity
├── BulkExportRepository.java      # Repository
├── BulkExportService.java         # Service
├── BulkExportProcessor.java       # Async processor
└── BulkExportController.java      # REST API

src/main/java/com/healthdata/fhir/config/
└── AsyncConfig.java               # Async configuration

src/main/resources/db/changelog/
└── 0011-create-bulk-export-jobs-table.xml

src/main/resources/
└── application.yml                # Updated with bulk-export config
```

## Configuration (application.yml)

```yaml
fhir:
  bulk-export:
    export-directory: /tmp/fhir-exports
    max-concurrent-exports: 5
    chunk-size: 1000
    retention-days: 7
    base-url: http://localhost:8085/fhir
    require-access-token: true
    async-executor-pool-size: 3
```

## Supported Resource Types

1. Patient
2. Observation
3. Condition
4. MedicationRequest
5. Procedure
6. Encounter
7. AllergyIntolerance
8. Immunization

## Query Parameters

| Parameter | Description | Example |
|-----------|-------------|---------|
| _type | Resource types to export | `?_type=Patient,Observation` |
| _since | Export only resources modified since | `?_since=2024-01-01T00:00:00Z` |
| _typeFilter | FHIR search parameters | `?_typeFilter=Patient?gender=female` |

## Response Codes

| Code | Status | Meaning |
|------|--------|---------|
| 202 | Accepted | Export initiated or in progress |
| 200 | OK | Export completed (returns manifest) |
| 404 | Not Found | Job or file not found |
| 429 | Too Many Requests | Concurrent limit exceeded |
| 500 | Internal Server Error | Export failed |
| 410 | Gone | Export was cancelled |

## Export Manifest Example

```json
{
  "transactionTime": "2024-12-05T16:00:00Z",
  "request": "http://localhost:8085/fhir/api/v1/$export?_type=Patient",
  "requiresAccessToken": true,
  "output": [
    {
      "type": "Patient",
      "url": "http://localhost:8085/fhir/api/v1/download/{jobId}/patient-{jobId}.ndjson",
      "count": 1500
    }
  ],
  "error": []
}
```

## Job Status Values

- `PENDING` - Job created, waiting to start
- `IN_PROGRESS` - Currently processing
- `COMPLETED` - Successfully completed
- `FAILED` - Processing failed
- `CANCELLED` - Cancelled by user

## Kafka Events

- `fhir.bulk-export.initiated` - Export job started
- `fhir.bulk-export.completed` - Export finished successfully
- `fhir.bulk-export.failed` - Export failed
- `fhir.bulk-export.cancelled` - Export cancelled

## Build & Deploy

```bash
# Build
./gradlew :modules:services:fhir-service:build

# Run
./gradlew :modules:services:fhir-service:bootRun

# Run migrations
./gradlew :modules:services:fhir-service:update
```

## Troubleshooting

### Export not starting
- Check concurrent export limit
- Verify JWT token is valid
- Check tenant ID in token

### Export fails
- Check export directory permissions
- Verify database connection
- Check Kafka connectivity
- Review error_message in bulk_export_jobs table

### Download fails
- Verify file exists in export directory
- Check JWT token
- Verify tenant ownership of export job

## Database Queries

```sql
-- View all export jobs
SELECT job_id, tenant_id, status, export_level, requested_at, completed_at
FROM bulk_export_jobs
ORDER BY requested_at DESC;

-- View active exports
SELECT job_id, status, requested_at, started_at, exported_resources, total_resources
FROM bulk_export_jobs
WHERE status IN ('PENDING', 'IN_PROGRESS');

-- View failed exports
SELECT job_id, tenant_id, error_message, requested_at
FROM bulk_export_jobs
WHERE status = 'FAILED'
ORDER BY requested_at DESC;

-- Cleanup old exports (manual)
DELETE FROM bulk_export_jobs
WHERE completed_at < NOW() - INTERVAL '7 days';
```

## Security Notes

- All endpoints require JWT authentication
- Tenant isolation enforced at repository level
- Download URLs expire based on job retention policy
- Access tokens validated on every request
- No PHI logged in audit events

## Performance Tips

- Use `_type` parameter to limit resource types
- Use `_since` for incremental exports
- Adjust `chunk-size` based on memory availability
- Monitor concurrent export limit
- Schedule large exports during off-peak hours
