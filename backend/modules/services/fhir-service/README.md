# FHIR Service

HL7 FHIR R4 server with SMART on FHIR authorization, bulk data export, and multi-tenant resource management.

## Purpose

Provides a FHIR R4-compliant API for healthcare data storage and retrieval, addressing the challenge that:
- Healthcare interoperability requires standardized FHIR R4 resource management
- SMART on FHIR authorization enables secure app integration
- Bulk data export supports quality reporting and population health analytics
- Multi-tenant isolation ensures data separation across healthcare organizations

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                       FHIR Service                               │
│                         (Port 8085)                              │
├─────────────────────────────────────────────────────────────────┤
│  Controller Layer (REST + FHIR Operations)                      │
│  ├── PatientController           - CRUD, search                 │
│  ├── ObservationController       - Labs, vitals                 │
│  ├── ConditionController         - Diagnoses                    │
│  ├── MedicationRequestController - Prescriptions                │
│  ├── AllergyIntoleranceController                               │
│  ├── ImmunizationController                                     │
│  ├── EncounterController         - Visits                       │
│  ├── ProcedureController                                        │
│  ├── CarePlanController                                         │
│  ├── DiagnosticReportController                                │
│  └── 10+ additional FHIR resource controllers                   │
├─────────────────────────────────────────────────────────────────┤
│  Service Layer                                                   │
│  ├── Resource Services         - Business logic per resource    │
│  ├── SearchService             - FHIR search parameters         │
│  └── ValidationService         - FHIR validation                │
├─────────────────────────────────────────────────────────────────┤
│  SMART on FHIR Layer                                            │
│  ├── SmartAuthorizationController - OAuth2 authorization        │
│  ├── SmartConfigurationController - .well-known/smart-config    │
│  └── ScopeValidator              - patient/*.read, user/*.*     │
├─────────────────────────────────────────────────────────────────┤
│  Bulk Export Layer                                              │
│  ├── BulkExportController      - $export operations             │
│  ├── BulkExportService         - Async NDJSON generation        │
│  └── ExportJobManager          - Job tracking, polling          │
├─────────────────────────────────────────────────────────────────┤
│  Repository Layer                                                │
│  └── JPA Repositories (one per FHIR resource type)              │
├─────────────────────────────────────────────────────────────────┤
│  Domain Entities                                                 │
│  └── FHIR R4 Resources (mapped to PostgreSQL)                   │
│      ├── PatientEntity, ObservationEntity, etc.                 │
│      └── Metadata: tenantId, version, lastUpdated               │
└─────────────────────────────────────────────────────────────────┘
```

## API Endpoints

### FHIR CRUD Operations
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/fhir/Patient` | Create patient |
| GET | `/fhir/Patient/{id}` | Read patient |
| GET | `/fhir/Patient?name={name}` | Search patients |
| PUT | `/fhir/Patient/{id}` | Update patient |
| DELETE | `/fhir/Patient/{id}` | Soft delete patient |

**Supported Resources**: Patient, Observation, Condition, MedicationRequest, AllergyIntolerance, Immunization, Encounter, Procedure, CarePlan, DiagnosticReport, DocumentReference, Coverage, Goal, and more.

### SMART on FHIR
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/fhir/.well-known/smart-configuration` | SMART configuration |
| GET | `/fhir/oauth/authorize` | Authorization endpoint |
| POST | `/fhir/oauth/token` | Token endpoint |

### Bulk Data Export
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/fhir/$export` | System-level export |
| GET | `/fhir/Patient/$export` | Patient-level export |
| GET | `/fhir/Group/{id}/$export` | Group-level export |
| GET | `/fhir/bulkstatus/{jobId}` | Poll export status |
| GET | `/fhir/bulkdata/{fileName}` | Download NDJSON file |

## FHIR Search Parameters

Common search parameters supported across resources:

| Parameter | Example | Description |
|-----------|---------|-------------|
| `_id` | `?_id=123` | Search by logical ID |
| `_lastUpdated` | `?_lastUpdated=gt2024-01-01` | Last modified date |
| `_count` | `?_count=20` | Results per page |
| `patient` | `?patient=Patient/123` | Filter by patient |
| `date` | `?date=ge2024-01-01` | Date range filter |
| `code` | `?code=http://loinc.org|8480-6` | Filter by code |

## Configuration

```yaml
server:
  port: 8085
  servlet:
    context-path: /fhir

# FHIR settings
fhir:
  version: R4
  validation:
    enabled: true
    strict: false

  # Bulk export settings
  bulk-export:
    export-directory: /tmp/fhir-exports
    max-concurrent-exports: 5
    chunk-size: 1000
    retention-days: 7

# Redis cache (HIPAA-compliant TTL)
spring.cache:
  type: redis
  redis:
    time-to-live: 120000  # 2 minutes for PHI

# HIPAA audit
audit:
  enabled: true
  retention-days: 2555  # 7 years
  include-phi: false

# Kafka integration
spring.kafka:
  bootstrap-servers: localhost:9092
  producer:
    acks: all
    retries: 3
```

## FHIR Resource Example

**Patient Resource (FHIR R4)**:

```json
{
  "resourceType": "Patient",
  "id": "patient-123",
  "meta": {
    "versionId": "1",
    "lastUpdated": "2024-01-15T10:00:00Z"
  },
  "identifier": [{
    "system": "http://hospital.org/mrn",
    "value": "MRN12345"
  }],
  "name": [{
    "family": "Smith",
    "given": ["John", "Robert"]
  }],
  "gender": "male",
  "birthDate": "1980-05-15",
  "address": [{
    "line": ["123 Main St"],
    "city": "Boston",
    "state": "MA",
    "postalCode": "02101"
  }]
}
```

## Dependencies

- **Spring Boot**: Web, JPA, Validation, Cache, Actuator
- **Database**: PostgreSQL with Liquibase migrations
- **Cache**: Redis (2 min TTL for HIPAA compliance)
- **FHIR**: HAPI FHIR R4 (parsing, validation, structure definitions)
- **Messaging**: Kafka for resource change events
- **Security**: JWT + SMART on FHIR OAuth2
- **Audit**: HIPAA audit service (7-year retention)

## Running Locally

```bash
# From backend directory
./gradlew :modules:services:fhir-service:bootRun

# Or via Docker
docker compose --profile fhir up fhir-service
```

## Testing

```bash
# Unit tests
./gradlew :modules:services:fhir-service:test

# Create patient
curl -X POST http://localhost:8085/fhir/Patient \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Patient",
    "name": [{"family": "Smith", "given": ["John"]}],
    "gender": "male",
    "birthDate": "1980-05-15"
  }'

# Search patients
curl http://localhost:8085/fhir/Patient?name=Smith \
  -H "X-Tenant-ID: tenant-1"

# Bulk export (async)
curl -X GET http://localhost:8085/fhir/$export \
  -H "X-Tenant-ID: tenant-1" \
  -H "Prefer: respond-async"

# Poll export status
curl http://localhost:8085/fhir/bulkstatus/{jobId}
```

## SMART on FHIR Scopes

Supported SMART scopes:

- `patient/*.read` - Read all patient data
- `patient/Patient.read` - Read patient demographics
- `patient/Observation.read` - Read observations
- `user/*.read` - Read all data (provider access)
- `user/*.write` - Write all data (provider access)

## Bulk Export Format

NDJSON (newline-delimited JSON) files:

```
{"resourceType":"Patient","id":"p1","name":[{"family":"Smith"}]}
{"resourceType":"Patient","id":"p2","name":[{"family":"Jones"}]}
```

## Performance

- Single resource CRUD: 10-50ms (cached)
- Search queries: 50-200ms (indexed)
- Bulk export: 1000 resources/sec
- Redis caching: 2 min TTL (HIPAA compliant)
