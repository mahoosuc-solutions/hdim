# Quality Measure Service

Healthcare quality measure calculations service supporting HEDIS, CMS, and custom measures with population-level analytics.

## Overview

The Quality Measure Service provides comprehensive quality measure calculation and reporting capabilities for healthcare organizations. It implements HEDIS (Healthcare Effectiveness Data and Information Set) measures, CMS (Centers for Medicare & Medicaid Services) quality measures, and supports custom measure definitions for value-based care programs.

## Key Features

### HEDIS Measure Calculations
- 50+ standard HEDIS measures supported
- Patient-level and population-level calculations
- Integration with CQL Engine Service for measure logic
- Real-time measure evaluation with FHIR R4 data

### Quality Reporting
- Patient quality reports with measure breakdowns
- Population quality reports by measurement year
- Report export to CSV and Excel formats
- Saved reports with versioning and audit trail

### Batch Processing
- Asynchronous population-level calculations
- Job tracking with progress monitoring
- Cancellable long-running jobs
- Error tracking and partial success handling

### Custom Measures
- Define organization-specific quality measures
- CQL-based measure definitions
- Custom scoring algorithms
- Measure versioning and lifecycle management

### Health Scoring
- Aggregate quality scores per patient
- Percentile rankings and benchmarks
- Quality trend analysis over time
- Care gap identification

### Clinical Decision Support (CDS)
- Real-time quality measure guidance
- Risk assessment integration
- Template-based reporting
- Patient health summary generation

## Technology Stack

- **Spring Boot 3.x**: Core framework
- **PostgreSQL**: Persistent storage (shared healthdata_cql database)
- **Redis**: Caching (HIPAA-compliant 2-minute TTL)
- **Apache Kafka**: Event streaming for audit
- **Liquibase**: Database migrations
- **WebSocket**: Real-time notifications
- **Apache POI**: Excel export capabilities

## API Endpoints

### Measure Calculation
```
POST /quality-measure/calculate
     - Calculate quality measure for a patient
     - Params: patient, measure, createdBy

GET  /quality-measure/results?patient={id}
     - Get measure results for a patient

GET  /quality-measure/score?patient={id}
     - Get aggregate quality score
```

### Quality Reports
```
GET  /quality-measure/report/patient?patient={id}
     - Generate patient quality report

GET  /quality-measure/report/population?year={year}
     - Generate population quality report

POST /quality-measure/report/patient/save
     - Save patient report

POST /quality-measure/report/population/save
     - Save population report
```

### Saved Reports
```
GET    /quality-measure/reports?type={type}
       - List saved reports

GET    /quality-measure/reports/{reportId}
       - Get specific report

DELETE /quality-measure/reports/{reportId}
       - Delete saved report

GET    /quality-measure/reports/{reportId}/export/csv
       - Export report to CSV

GET    /quality-measure/reports/{reportId}/export/excel
       - Export report to Excel
```

### Batch Population Calculations
```
POST /quality-measure/population/calculate
     - Start batch calculation for all patients
     - Returns job ID for tracking

GET  /quality-measure/population/jobs/{jobId}
     - Get job status and progress

GET  /quality-measure/population/jobs
     - List all jobs for tenant

POST /quality-measure/population/jobs/{jobId}/cancel
     - Cancel running job
```

### Health Check
```
GET /quality-measure/_health
    - Service health status
```

## Configuration

### Application Properties
```yaml
server.port: 8087
spring.datasource.url: jdbc:postgresql://localhost:5435/healthdata_cql
fhir.server.url: http://localhost:8085/fhir
cql.engine.url: http://localhost:8081/cql-engine
```

### HIPAA Compliance
- Redis cache TTL: 2 minutes (HIPAA-compliant for PHI)
- All API calls audited via Kafka
- Tenant isolation enforced on all operations
- WebSocket connections: 15-minute automatic logoff

### WebSocket Configuration
- Real-time measure calculation progress
- Session timeout: 15 minutes (HIPAA §164.312(a)(2)(iii))
- Maximum 3 concurrent connections per user
- Rate limiting: 10 connections/minute per IP

## Running Locally

### Prerequisites
- Java 21
- PostgreSQL 14+
- Redis 6+
- Kafka 3+

### Build
```bash
./gradlew :modules:services:quality-measure-service:build
```

### Run
```bash
./gradlew :modules:services:quality-measure-service:bootRun
```

### Run Tests
```bash
./gradlew :modules:services:quality-measure-service:test
```

## Integration

This service integrates with:
- **FHIR Service**: Retrieve patient clinical data
- **CQL Engine Service**: Execute measure logic
- **Care Gap Service**: Identify care gaps from measures
- **Patient Service**: Patient demographics
- **Payer Workflows Service**: Star Ratings and compliance

## Security

- JWT-based authentication
- Role-based access control (EVALUATOR, ANALYST, ADMIN, SUPER_ADMIN)
- Tenant isolation via X-Tenant-ID header
- HIPAA-compliant audit logging
- Rate limiting on all endpoints

## API Documentation

Swagger UI available at:
```
http://localhost:8087/quality-measure/swagger-ui.html
```

## Monitoring

Actuator endpoints:
- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`

## License

Copyright (c) 2024 Mahoosuc Solutions
