# CQL Engine Service

Clinical Quality Language (CQL) execution engine for evaluating quality measures, clinical logic, and HEDIS calculations.

## Purpose

Executes CQL expressions against patient FHIR data, addressing the challenge that:
- Healthcare quality measures require complex clinical logic evaluation
- CQL libraries need versioning, validation, and dependency management
- Measure evaluation must scale across thousands of patients (batch processing)
- Results must be cached (5 min TTL) while maintaining HIPAA compliance

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    CQL Engine Service                            │
│                         (Port 8081)                              │
├─────────────────────────────────────────────────────────────────┤
│  Controller Layer                                                │
│  ├── CqlEvaluationController      - Execute CQL, batch runs     │
│  ├── CqlLibraryController         - CRUD for CQL libraries      │
│  ├── ValueSetController           - Manage value sets           │
│  └── VisualizationController      - Real-time progress (WS)     │
├─────────────────────────────────────────────────────────────────┤
│  Service Layer                                                   │
│  ├── CqlEvaluationService         - Execute CQL, retry logic    │
│  ├── CqlLibraryService            - Library versioning          │
│  ├── ValueSetService              - VSAC integration            │
│  └── CqlEngineExecutor            - CQF Engine wrapper          │
├─────────────────────────────────────────────────────────────────┤
│  Repository Layer                                                │
│  ├── CqlEvaluationRepository                                    │
│  ├── CqlLibraryRepository                                       │
│  └── ValueSetRepository                                         │
├─────────────────────────────────────────────────────────────────┤
│  Domain Entities                                                 │
│  ├── CqlEvaluation    - Execution results, status tracking      │
│  ├── CqlLibrary       - CQL code, version, dependencies         │
│  └── ValueSet         - Clinical codes, OIDs, expansion         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Feign (HTTP) / Circuit Breaker
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      FHIR Service (Port 8085)                    │
│  - Patient data retrieval for CQL evaluation                    │
│  - Observation, Condition, MedicationRequest queries            │
└─────────────────────────────────────────────────────────────────┘
```

## API Endpoints

### CQL Evaluation
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/cql/evaluations` | Execute CQL for patient |
| POST | `/api/v1/cql/evaluations/{id}/execute` | Execute existing evaluation |
| POST | `/api/v1/cql/evaluations/batch` | Batch evaluate patients |
| POST | `/api/v1/cql/evaluations/{id}/retry` | Retry failed evaluation |
| GET | `/api/v1/cql/evaluations` | List evaluations (paginated) |
| GET | `/api/v1/cql/evaluations/{id}` | Get evaluation details |
| GET | `/api/v1/cql/evaluations/patient/{patientId}` | Get patient evaluations |
| GET | `/api/v1/cql/evaluations/by-status/{status}` | Filter by status |

### CQL Libraries
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/cql/libraries` | Create library |
| PUT | `/api/v1/cql/libraries/{id}` | Update library |
| GET | `/api/v1/cql/libraries/{id}` | Get library |
| GET | `/api/v1/cql/libraries` | List libraries |
| POST | `/api/v1/cql/libraries/{id}/validate` | Validate CQL syntax |

### Value Sets
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/cql/valuesets` | Create value set |
| PUT | `/api/v1/cql/valuesets/{id}` | Update value set |
| GET | `/api/v1/cql/valuesets` | List value sets |
| GET | `/api/v1/cql/valuesets/oid/{oid}` | Get by OID |

## Configuration

```yaml
server:
  port: 8081
  servlet:
    context-path: /cql-engine

# FHIR server for data retrieval
fhir:
  server:
    url: http://localhost:8085/fhir

# Redis cache (HIPAA-compliant TTL)
spring.cache:
  type: redis
  redis:
    time-to-live: 300000  # 5 minutes for PHI

# HEDIS measures
hedis:
  measures:
    enabled: true
    cache-ttl-hours: 0.083  # 5 minutes

# Real-time visualization
visualization:
  websocket:
    enabled: true
  kafka:
    enabled: true
    topics:
      evaluation-completed: "evaluation.completed"
```

## Dependencies

- **Spring Boot**: Web, JPA, Validation, Cache
- **Database**: PostgreSQL with Liquibase migrations
- **Cache**: Redis (5 min TTL for HIPAA compliance)
- **CQF**: OpenCDS CQF Engine for CQL execution
- **FHIR**: HAPI FHIR R4 for resource parsing
- **Messaging**: Kafka for evaluation events
- **Resilience**: Resilience4j for FHIR service integration

## Running Locally

```bash
# From backend directory
./gradlew :modules:services:cql-engine-service:bootRun

# Or via Docker
docker compose --profile cql up cql-engine-service
```

## Testing

```bash
# Unit tests
./gradlew :modules:services:cql-engine-service:test

# Create evaluation
curl -X POST http://localhost:8081/cql-engine/api/v1/cql/evaluations \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/json" \
  -d '{"libraryId":"lib-uuid","patientId":"patient-123"}'

# Batch evaluation (10 patients)
curl -X POST http://localhost:8081/cql-engine/api/v1/cql/evaluations/batch \
  -H "X-Tenant-ID: tenant-1" \
  -d 'libraryId=lib-uuid&patientIds=p1,p2,p3'
```

## Performance

- Single evaluation: 50-200ms (cached FHIR data)
- Batch evaluation: 10 patients/sec
- Redis caching: 5 min TTL (HIPAA compliant)
- WebSocket: Real-time progress for batch jobs
