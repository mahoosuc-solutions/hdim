# Patient Service

Patient data aggregation, timeline visualization, and health status dashboards with consent-aware data access.

## Purpose

Aggregates patient data from FHIR service into comprehensive views, addressing the challenge that:
- Clinicians need a unified view of patient records scattered across FHIR resources
- Timeline views require chronological ordering of encounters, medications, conditions, labs
- Health status dashboards need real-time aggregation of active conditions, medications, allergies
- Consent validation must occur before returning PHI

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Patient Service                              │
│                         (Port 8084)                              │
├─────────────────────────────────────────────────────────────────┤
│  Controller Layer                                                │
│  └── PatientController (30+ REST endpoints)                     │
├─────────────────────────────────────────────────────────────────┤
│  Service Layer                                                   │
│  ├── PatientAggregationService                                  │
│  │   ├── Comprehensive health record  - All resources           │
│  │   ├── Filtered queries            - Active only, critical    │
│  │   └── Resource-specific views     - Meds, allergies, vitals  │
│  ├── PatientTimelineService                                     │
│  │   ├── Chronological timeline      - All events sorted        │
│  │   ├── Date range filtering        - Custom time windows      │
│  │   ├── Resource type filtering     - Condition, Observation   │
│  │   └── Monthly summaries           - Event counts by month    │
│  └── PatientHealthStatusService                                 │
│      ├── Health status dashboard     - Overview with counts     │
│      └── Resource summaries          - Med, allergy, condition  │
├─────────────────────────────────────────────────────────────────┤
│  Client Layer (Feign)                                           │
│  ├── FhirClient         - Query FHIR resources                  │
│  └── ConsentClient      - Validate data access                  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Feign (HTTP) + Circuit Breaker
                              ▼
┌────────────────────────────────────────────────────────────────┐
│  FHIR Service (8085)       Consent Service (8082)              │
│  - Patient resources       - Authorization validation           │
│  - Observations, Meds      - Scope/category checking            │
└────────────────────────────────────────────────────────────────┘
```

## API Endpoints

### Patient Aggregation
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/patient/health-record?patient={id}` | Comprehensive health record |
| GET | `/patient/allergies?patient={id}` | Patient allergies |
| GET | `/patient/immunizations?patient={id}` | Immunizations |
| GET | `/patient/medications?patient={id}` | Medications (active/all) |
| GET | `/patient/conditions?patient={id}` | Conditions (active/all) |
| GET | `/patient/procedures?patient={id}` | Procedures |
| GET | `/patient/vitals?patient={id}` | Vital signs |
| GET | `/patient/labs?patient={id}` | Lab results |
| GET | `/patient/encounters?patient={id}` | Encounters |
| GET | `/patient/care-plans?patient={id}` | Care plans |

### Timeline Views
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/patient/timeline?patient={id}` | Full patient timeline |
| GET | `/patient/timeline/by-date?startDate={d1}&endDate={d2}` | Date range timeline |
| GET | `/patient/timeline/by-type?resourceType={type}` | Filter by resource type |
| GET | `/patient/timeline/summary?year={year}` | Monthly summary |

### Health Status Dashboards
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/patient/health-status?patient={id}` | Health status overview |
| GET | `/patient/medication-summary?patient={id}` | Medication summary |
| GET | `/patient/allergy-summary?patient={id}` | Allergy summary |
| GET | `/patient/condition-summary?patient={id}` | Condition summary |
| GET | `/patient/immunization-summary?patient={id}` | Immunization summary |

## Response Formats

All aggregation endpoints return FHIR Bundles (`application/fhir+json`):

```json
{
  "resourceType": "Bundle",
  "type": "searchset",
  "total": 42,
  "entry": [
    {
      "resource": {
        "resourceType": "MedicationRequest",
        "id": "med-123",
        "status": "active",
        "medicationCodeableConcept": {
          "coding": [{
            "system": "http://www.nlm.nih.gov/research/umls/rxnorm",
            "code": "197361",
            "display": "Lisinopril 10 MG"
          }]
        }
      }
    }
  ]
}
```

Timeline endpoints return JSON arrays:

```json
[
  {
    "date": "2024-01-15T10:30:00Z",
    "resourceType": "Condition",
    "resourceId": "cond-123",
    "title": "Hypertension",
    "description": "Essential hypertension diagnosis",
    "category": "CONDITION"
  }
]
```

## Configuration

```yaml
server:
  port: 8084
  servlet:
    context-path: /patient

# FHIR service integration
fhir:
  server:
    url: http://localhost:8085/fhir

# Consent service integration
consent:
  server:
    url: http://localhost:8086/consent

# Redis cache (HIPAA-compliant TTL)
spring.cache:
  type: redis
  redis:
    time-to-live: 120000  # 2 minutes for PHI

# HIPAA audit
audit:
  enabled: true
  encryption:
    enabled: true
```

## Dependencies

- **Spring Boot**: Web, JPA, Validation, Cache
- **Database**: PostgreSQL with Liquibase migrations
- **Cache**: Redis (2 min TTL for HIPAA compliance)
- **FHIR**: HAPI FHIR R4 for resource parsing
- **HTTP Client**: OpenFeign for FHIR/Consent service integration
- **Resilience**: Circuit breakers for service failures

## Running Locally

```bash
# Start dependencies (FHIR service, Consent service)
docker compose up -d fhir-service consent-service

# From backend directory
./gradlew :modules:services:patient-service:bootRun

# Or via Docker
docker compose --profile patient up patient-service
```

## Testing

```bash
# Unit tests
./gradlew :modules:services:patient-service:test

# Get comprehensive health record
curl http://localhost:8084/patient/health-record?patient=p123 \
  -H "X-Tenant-ID: tenant-1" \
  -H "Accept: application/fhir+json"

# Get active medications only
curl http://localhost:8084/patient/medications?patient=p123&onlyActive=true \
  -H "X-Tenant-ID: tenant-1"

# Get patient timeline for 2024
curl http://localhost:8084/patient/timeline/by-date?patient=p123&startDate=2024-01-01&endDate=2024-12-31 \
  -H "X-Tenant-ID: tenant-1"

# Get health status dashboard
curl http://localhost:8084/patient/health-status?patient=p123 \
  -H "X-Tenant-ID: tenant-1"
```

## Performance

- Comprehensive health record: 200-500ms (includes 10+ FHIR queries)
- Timeline generation: 100-300ms (cached)
- Redis caching: 2 min TTL (HIPAA compliant)
- Consent validation: Automatic for all endpoints
