# Electronic Case Reporting Service

Automated public health reporting service implementing CDC electronic Initial Case Report (eICR) submission for reportable conditions.

## Purpose

The ECR Service automates the detection, generation, and submission of electronic case reports to public health agencies. It monitors clinical data for Reportable Condition Trigger Codes (RCTC) and automatically generates eICR documents compliant with HL7 CDA R2 standards for submission to the AIMS Platform.

## Key Features

- **RCTC Rules Engine**: Real-time monitoring of reportable condition trigger codes
- **Automated eICR Generation**: HL7 CDA R2-compliant eICR document creation
- **AIMS Platform Integration**: Direct submission to CDC AIMS (Association of Immunization Managers Information System)
- **Urgency-based Processing**: Immediate, 24-hour, and 72-hour reporting based on condition severity
- **Trigger Categories**: Monitors diagnosis codes, lab results, medications, and procedures
- **Multi-tenant Support**: Complete tenant isolation for healthcare organizations
- **Retry Logic**: Automatic retry with exponential backoff for failed submissions
- **Status Tracking**: Complete audit trail from trigger detection to acknowledgment
- **Weekly RCTC Updates**: Automated value set updates from CDC sources

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/ecr` | List eCRs with pagination and status filtering |
| GET | `/api/ecr/{ecrId}` | Get specific eCR by ID |
| GET | `/api/ecr/patient/{patientId}` | Get all eCRs for a patient |
| POST | `/api/ecr/evaluate` | Manually evaluate clinical codes for reportability |
| POST | `/api/ecr/{ecrId}/reprocess` | Reprocess a failed eCR |
| POST | `/api/ecr/{ecrId}/cancel` | Cancel a pending eCR |
| GET | `/api/ecr/summary` | Get eCR status summary for dashboard |
| GET | `/api/ecr/conditions` | Get list of monitored reportable conditions |
| GET | `/api/ecr/check-trigger` | Check if a code triggers reportable condition |

## Configuration

### Application Properties

```yaml
server:
  port: 8101

ecr:
  aims:
    enabled: false
    base-url: https://ecr.aimsplatform.org/api
    client-id: ${ECR_AIMS_CLIENT_ID}
    client-secret: ${ECR_AIMS_CLIENT_SECRET}
    retry:
      max-attempts: 3
      initial-interval-ms: 1000
      multiplier: 2.0

  rctc:
    value-set-update-cron: "0 0 2 * * SUN"  # Weekly Sunday 2 AM
    cache-ttl-hours: 168  # 7 days

  eicr:
    author-organization: HealthData-in-Motion
    custodian-oid: 2.16.840.1.113883.3.xxx
    include-phi: true

  triggers:
    categories:
      - diagnosis
      - lab_result
      - medication
      - procedure
```

### Urgency Levels

```yaml
ecr:
  urgency:
    immediate:
      - Anthrax
      - Botulism
      - Plague
      - Smallpox
      - Viral Hemorrhagic Fever
    within-24-hours:
      - Measles
      - Pertussis
      - Meningococcal Disease
    within-72-hours:
      - Hepatitis A
      - Salmonellosis
      - Tuberculosis
```

### Database

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/healthdata
    username: healthdata
    password: ${DB_PASSWORD}
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml
    default-schema: ecr
```

## Reportable Conditions

The RCTC rules engine monitors for these condition categories:
- Communicable diseases (measles, tuberculosis, COVID-19, etc.)
- Foodborne illnesses (salmonella, E. coli, listeria, etc.)
- Vaccine-preventable diseases (pertussis, mumps, rubella, etc.)
- Sexually transmitted infections (HIV, syphilis, gonorrhea, etc.)
- Zoonotic diseases (rabies, anthrax, plague, etc.)
- Emerging threats (novel influenza, Ebola, Zika, etc.)

## eCR Status Lifecycle

1. **PENDING**: Trigger detected, eICR generation queued
2. **GENERATING**: eICR document being created
3. **READY**: eICR ready for submission
4. **SUBMITTED**: eICR submitted to AIMS Platform
5. **ACKNOWLEDGED**: Public health agency acknowledged receipt
6. **FAILED**: Submission failed (eligible for reprocessing)
7. **CANCELLED**: Manually cancelled before submission

## Running Locally

### Start the Service

```bash
./gradlew :modules:services:ecr-service:bootRun
```

### Running Tests

```bash
./gradlew :modules:services:ecr-service:test
```

### Building

```bash
./gradlew :modules:services:ecr-service:build
```

## Example Usage

### Evaluate Clinical Codes

```bash
curl -X POST http://localhost:8101/api/ecr/evaluate \
  -H "X-Tenant-ID: tenant-001" \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "patient-123",
    "diagnosisCodes": ["A01.0", "B20"],
    "labCodes": ["94500-6"]
  }'
```

### Check Trigger Code

```bash
curl "http://localhost:8101/api/ecr/check-trigger?code=A01.0&codeSystem=2.16.840.1.113883.6.90"
```

## Integration

The ECR Service integrates with:
- **FHIR Service**: Retrieves patient clinical data
- **Patient Service**: Gets patient demographics
- **AIMS Platform**: Submits eICR documents to public health
- **Event Router**: Publishes eCR lifecycle events

## Standards Compliance

- HL7 CDA R2 for eICR documents
- HL7 FHIR R4 for data retrieval
- CDC RCTC value sets
- ICD-10-CM, LOINC, RxNorm code systems

## Authors

HDIM Development Team

## License

Proprietary - Mahoosuc Solutions
