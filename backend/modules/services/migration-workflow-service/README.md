# Migration Workflow Service

Data migration job orchestration service for healthcare data migrations with progress tracking, error handling, and quality reporting.

## Purpose

The Migration Workflow Service orchestrates large-scale healthcare data migrations from legacy systems to the HDIM platform. It provides comprehensive job management, progress monitoring, error tracking, data quality reporting, and checkpoint recovery for reliable and auditable data migrations.

## Key Features

- **Job Orchestration**: Create, start, pause, resume, and cancel migration jobs
- **Multiple Source Types**: Support for FHIR, HL7v2, SFTP, MLLP, and database sources
- **Progress Tracking**: Real-time progress updates with records processed, success/failure counts
- **Checkpoint Recovery**: Automatic checkpointing every 500 records for fault tolerance
- **Error Management**: Detailed error capture with categorization and CSV export
- **Data Quality Reports**: Post-migration quality analysis with metrics and recommendations
- **Batch Processing**: Configurable batch sizes (default 100 records)
- **Concurrent Job Limits**: Maximum 3 concurrent migrations per tenant
- **CDR Integration**: Seamless integration with CDR Processor Service for HL7v2 parsing
- **Multi-tenant Support**: Complete tenant isolation and security

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/migrations` | Create new migration job |
| GET | `/api/v1/migrations` | List migration jobs with pagination |
| GET | `/api/v1/migrations/{jobId}` | Get migration job details |
| POST | `/api/v1/migrations/{jobId}/start` | Start a migration job |
| POST | `/api/v1/migrations/{jobId}/pause` | Pause a running migration job |
| POST | `/api/v1/migrations/{jobId}/resume` | Resume a paused migration job |
| POST | `/api/v1/migrations/{jobId}/cancel` | Cancel a migration job |
| DELETE | `/api/v1/migrations/{jobId}` | Delete a migration job |
| GET | `/api/v1/migrations/{jobId}/progress` | Get current progress |
| GET | `/api/v1/migrations/{jobId}/summary` | Get completion summary |
| GET | `/api/v1/migrations/{jobId}/errors` | Get errors with pagination |
| GET | `/api/v1/migrations/{jobId}/quality` | Get data quality report |
| GET | `/api/v1/migrations/{jobId}/quality/export` | Export quality report as CSV |
| GET | `/api/v1/migrations/{jobId}/errors/export` | Export errors as CSV |

## Configuration

### Application Properties

```yaml
server:
  port: 8103

migration:
  default-batch-size: 100
  checkpoint-interval: 500
  progress-update-interval: 1000
  max-concurrent-jobs: 3
  sftp-connection-timeout: 30000
  mllp-default-port: 2575

cdr-processor:
  url: http://localhost:8099
  connect-timeout: 10s
  read-timeout: 30s
```

### Database

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/healthdata
    username: healthdata
    password: ${DB_PASSWORD}
  liquibase:
    change-log: classpath:db/changelog/0013-create-migration-jobs.sql
```

### Kafka

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      properties:
        enable.idempotence: true
```

## Job Status Lifecycle

1. **CREATED**: Job created but not started
2. **RUNNING**: Job actively processing records
3. **PAUSED**: Job temporarily paused (can be resumed)
4. **COMPLETED**: Job completed successfully
5. **FAILED**: Job failed with errors
6. **CANCELLED**: Job manually cancelled

## Source Types

### FHIR
- Direct FHIR R4 API integration
- Supports Patient, Observation, Condition, etc.
- Pagination and batch retrieval

### HL7v2
- ADT, ORU, ORM message processing
- Integration with CDR Processor Service
- Batch message parsing

### SFTP
- File-based migrations
- Automatic retry with connection pooling
- Support for CSV, JSON, XML formats

### MLLP
- Real-time HL7v2 message streaming
- Configurable port and timeout
- Acknowledgment handling

### Database
- Direct database-to-database migration
- JDBC connection support
- Custom SQL query support

## Data Quality Metrics

The quality report includes:
- **Completeness**: Percentage of required fields populated
- **Accuracy**: Data validation pass rate
- **Consistency**: Cross-field consistency checks
- **Timeliness**: Migration performance metrics
- **Uniqueness**: Duplicate detection
- **Validity**: Format and range validation

## Error Categories

- **CONNECTION_ERROR**: Source system connectivity issues
- **PARSING_ERROR**: Message/document parsing failures
- **VALIDATION_ERROR**: Data validation failures
- **TRANSFORMATION_ERROR**: Data transformation issues
- **PERSISTENCE_ERROR**: Database write failures
- **UNKNOWN_ERROR**: Unclassified errors

## Running Locally

### Start the Service

```bash
./gradlew :modules:services:migration-workflow-service:bootRun
```

### Running Tests

```bash
./gradlew :modules:services:migration-workflow-service:test
```

### Building

```bash
./gradlew :modules:services:migration-workflow-service:build
```

## Example Usage

### Create Migration Job

```bash
curl -X POST http://localhost:8103/api/v1/migrations \
  -H "X-Tenant-ID: tenant-001" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Epic Patient Migration",
    "sourceType": "FHIR",
    "sourceConfig": {
      "fhirServerUrl": "https://fhir.epic.com/R4",
      "resourceType": "Patient"
    },
    "batchSize": 100
  }'
```

### Get Job Progress

```bash
curl http://localhost:8103/api/v1/migrations/{jobId}/progress \
  -H "X-Tenant-ID: tenant-001"
```

### Export Quality Report

```bash
curl http://localhost:8103/api/v1/migrations/{jobId}/quality/export \
  -H "X-Tenant-ID: tenant-001" \
  -o quality-report.csv
```

## Integration

The Migration Workflow Service integrates with:
- **CDR Processor Service**: HL7v2 and CDA parsing
- **FHIR Service**: Target FHIR resource storage
- **Event Router**: Migration lifecycle event publishing
- **Audit Service**: Comprehensive audit logging

## Authors

HDIM Development Team

## License

Proprietary - Mahoosuc Solutions
