# QRDA Export Service

Quality Reporting Document Architecture (QRDA) export service for CMS electronic clinical quality measure (eCQM) reporting.

## Purpose

The QRDA Export Service generates QRDA Category I (patient-level) and Category III (aggregate) documents for CMS quality measure reporting. It integrates with the Quality Measure Service and CQL Engine to calculate measures and exports validated QRDA documents for submission to CMS Quality Payment Program (QPP) and other quality reporting programs.

## Key Features

- **QRDA Category I**: Patient-level eCQM reporting for individual patient care documentation
- **QRDA Category III**: Aggregate population-level eCQM reporting for quality programs
- **HL7 CDA R2 Compliance**: Standards-compliant QRDA document generation
- **Schematron Validation**: CMS QRDA validation with detailed error reporting
- **Asynchronous Job Processing**: Background processing for large patient populations
- **Batch Export**: Support for up to 1,000 patients per Category III document
- **Template Caching**: Redis caching for QRDA templates (5-minute TTL)
- **Multi-measure Support**: Generate reports for multiple eCQMs in single request
- **Document Storage**: Local file storage with 90-day retention policy
- **ZIP Archives**: Category I exports bundled as ZIP files for easy distribution
- **Audit Trail**: HIPAA-compliant audit logging for all exports

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/qrda-export/api/v1/qrda/category-i/generate` | Generate QRDA Category I documents |
| POST | `/qrda-export/api/v1/qrda/category-iii/generate` | Generate QRDA Category III document |
| GET | `/qrda-export/api/v1/qrda/jobs/{jobId}` | Get export job status |
| GET | `/qrda-export/api/v1/qrda/jobs` | List export jobs with filtering |
| GET | `/qrda-export/api/v1/qrda/jobs/{jobId}/download` | Download generated QRDA documents |
| POST | `/qrda-export/api/v1/qrda/jobs/{jobId}/cancel` | Cancel pending/running job |

## Configuration

### Application Properties

```yaml
server:
  port: 8104
  servlet:
    context-path: /qrda-export

qrda:
  export:
    storage-path: /tmp/qrda-exports
    retention-days: 90
    max-batch-size: 1000
  validation:
    enabled: true
    category-i-schematron: classpath:schematron/qrda-cat-i.sch
    category-iii-schematron: classpath:schematron/qrda-cat-iii.sch
```

### Service Integration

```yaml
quality-measure:
  service:
    url: http://localhost:8087/quality-measure

cql:
  engine:
    url: http://localhost:8081/cql-engine

patient:
  service:
    url: http://localhost:8084/patient

fhir:
  server:
    url: http://localhost:8085/fhir
```

### Database

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5435/healthdata_cql
    username: healthdata
    password: ${DB_PASSWORD}
```

### Cache

```yaml
spring.cache:
  type: redis
  redis:
    time-to-live: 300000  # 5 minutes
```

## QRDA Document Types

### Category I (Patient-Level)
- Individual patient eCQM reporting
- Supports Merit-based Incentive Payment System (MIPS)
- Includes patient demographics, clinical data, and measure results
- Typically 1 document per patient per measure
- Exported as ZIP archive

### Category III (Aggregate)
- Population-level aggregate reporting
- Supports MIPS, Hospital Quality Reporting, etc.
- Includes measure results across patient population
- 1 document for all patients and measures
- Exported as single XML document

## Job Status Lifecycle

1. **PENDING**: Job created and queued for processing
2. **RUNNING**: Job actively generating QRDA documents
3. **COMPLETED**: Job completed successfully, documents available for download
4. **FAILED**: Job failed with error message
5. **CANCELLED**: Job manually cancelled

## Running Locally

### Start the Service

```bash
./gradlew :modules:services:qrda-export-service:bootRun
```

### Running Tests

```bash
./gradlew :modules:services:qrda-export-service:test
```

### Building

```bash
./gradlew :modules:services:qrda-export-service:build
```

## Example Usage

### Generate Category I Export

```bash
curl -X POST http://localhost:8104/qrda-export/api/v1/qrda/category-i/generate \
  -H "X-Tenant-ID: tenant-001" \
  -H "Content-Type: application/json" \
  -d '{
    "measureIds": ["CMS134v10", "CMS135v10"],
    "periodStart": "2024-01-01",
    "periodEnd": "2024-12-31",
    "patientIds": ["patient-001", "patient-002"],
    "includeClosedGaps": true
  }'
```

### Generate Category III Export

```bash
curl -X POST http://localhost:8104/qrda-export/api/v1/qrda/category-iii/generate \
  -H "X-Tenant-ID: tenant-001" \
  -H "Content-Type: application/json" \
  -d '{
    "measureIds": ["CMS134v10", "CMS135v10", "CMS136v10"],
    "periodStart": "2024-01-01",
    "periodEnd": "2024-12-31",
    "reportingParameters": {
      "reporterName": "HealthCare Organization",
      "reporterNPI": "1234567890",
      "reporterTIN": "12-3456789"
    }
  }'
```

### Download Documents

```bash
curl http://localhost:8104/qrda-export/api/v1/qrda/jobs/{jobId}/download \
  -H "X-Tenant-ID: tenant-001" \
  -o qrda-export.zip
```

## Supported eCQMs

The service supports all CMS eCQMs including:
- CMS134v10: Diabetes: Hemoglobin A1c (HbA1c) Poor Control
- CMS135v10: Heart Failure (HF): Angiotensin-Converting Enzyme (ACE) Inhibitor
- CMS136v10: Follow-Up Care for Children Prescribed ADHD Medication
- And 100+ additional measures

## Validation

### Schematron Validation
- CMS-provided schematron rules
- Structural and semantic validation
- Detailed error reporting
- Validation errors included in job results

### Common Validation Issues
- Missing required patient demographics
- Invalid measure calculation results
- Incorrect reporting period dates
- Missing or invalid code systems

## Document Storage

- Default path: `/tmp/qrda-exports`
- Retention: 90 days (configurable)
- Automatic cleanup of expired documents
- ZIP format for Category I (multiple patients)
- XML format for Category III (single document)

## Integration

The QRDA Export Service integrates with:
- **Quality Measure Service**: Retrieves calculated measure results
- **CQL Engine Service**: Performs measure calculations if needed
- **Patient Service**: Gets patient demographics
- **FHIR Service**: Retrieves clinical data for measures

## Standards Compliance

- HL7 CDA Release 2
- QRDA Category I Implementation Guide
- QRDA Category III Implementation Guide
- CMS eCQM specifications
- Value Set Authority Center (VSAC) value sets

## Authors

HDIM Development Team

## License

Proprietary - Mahoosuc Solutions
