# Care Gap Service

Automated care gap identification, tracking, and reporting for HEDIS/quality measure compliance.

## Purpose

Identifies and tracks gaps in care based on CQL evaluation results, addressing the challenge that:
- Quality measure compliance requires proactive identification of missing screenings, tests, and interventions
- Care teams need prioritized lists of gaps (high priority, overdue, upcoming)
- Population health management requires aggregated gap reports across patient panels
- Care gap closure tracking is essential for value-based care reimbursement

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Care Gap Service                              │
│                         (Port 8086)                              │
├─────────────────────────────────────────────────────────────────┤
│  Controller Layer                                                │
│  └── CareGapController (25+ REST endpoints)                     │
├─────────────────────────────────────────────────────────────────┤
│  Service Layer                                                   │
│  ├── CareGapIdentificationService                               │
│  │   ├── Identify all gaps         - Run all CQL libraries      │
│  │   ├── Identify by library       - Single measure evaluation  │
│  │   ├── Refresh gaps              - Re-evaluate patient        │
│  │   ├── Close gaps                - Mark as addressed          │
│  │   ├── Query by status           - Open, high priority        │
│  │   └── Statistics                - Gap counts, priorities     │
│  └── CareGapReportService                                       │
│      ├── Patient summaries         - Gap overview per patient   │
│      ├── Category grouping         - By measure category        │
│      ├── Priority grouping         - By urgency                 │
│      ├── Overdue gaps              - Past due date              │
│      ├── Upcoming gaps             - Due within N days          │
│      └── Population reports        - Tenant-wide analytics      │
├─────────────────────────────────────────────────────────────────┤
│  Repository Layer                                                │
│  └── CareGapRepository (JPA + custom queries)                   │
├─────────────────────────────────────────────────────────────────┤
│  Domain Entities                                                 │
│  └── CareGapEntity                                              │
│      ├── Gap Metadata         - Patient, measure, category      │
│      ├── Priority             - LOW, MEDIUM, HIGH, CRITICAL     │
│      ├── Status               - OPEN, CLOSED                    │
│      ├── Dates                - Identified, due, closed         │
│      └── Closure Tracking     - Reason, action, closed by       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Feign (HTTP) + Circuit Breaker
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  CQL Engine (8081)        Patient Service (8084)                │
│  - CQL evaluation          - Patient data aggregation           │
│  - Measure logic           - Health record access               │
└─────────────────────────────────────────────────────────────────┘
```

## API Endpoints

### Care Gap Identification
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/care-gap/identify?patient={id}` | Identify all gaps for patient |
| POST | `/care-gap/identify/{library}?patient={id}` | Identify gaps for measure |
| POST | `/care-gap/refresh?patient={id}` | Re-evaluate patient (refresh) |
| POST | `/care-gap/close?gapId={id}&closedBy={user}` | Close a care gap |

### Care Gap Queries
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/care-gap/open?patient={id}` | Open gaps for patient |
| GET | `/care-gap/high-priority?patient={id}` | High priority gaps |
| GET | `/care-gap/overdue?patient={id}` | Overdue gaps (past due date) |
| GET | `/care-gap/upcoming?patient={id}&days=30` | Due within N days |

### Statistics & Reports
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/care-gap/stats?patient={id}` | Gap statistics for patient |
| GET | `/care-gap/summary?patient={id}` | Gap summary for patient |
| GET | `/care-gap/by-category?patient={id}` | Gaps grouped by category |
| GET | `/care-gap/by-priority?patient={id}` | Gaps grouped by priority |
| GET | `/care-gap/population-report` | Tenant-wide gap report |

## Care Gap Entity Structure

```json
{
  "id": "uuid",
  "tenantId": "tenant-1",
  "patientId": "patient-123",
  "measureName": "Colorectal Cancer Screening",
  "measureId": "COL",
  "measureCategory": "Cancer Screening",
  "gapDescription": "Patient is due for colorectal cancer screening (age 55, last colonoscopy 2015)",
  "priority": "HIGH",  // LOW, MEDIUM, HIGH, CRITICAL
  "status": "OPEN",  // OPEN, CLOSED
  "identifiedDate": "2024-01-15T10:00:00Z",
  "dueDate": "2024-06-30",
  "closedDate": null,
  "closedBy": null,
  "closureReason": null,
  "closureAction": null,
  "cqlEvaluationId": "eval-uuid",
  "createdBy": "system"
}
```

## Gap Identification Logic

1. **Trigger**: Manual API call or scheduled job
2. **Evaluation**: CQL Engine evaluates all quality measures for patient
3. **Gap Detection**: If measure result = false, create CareGapEntity
4. **Priority Assignment**: Based on measure category, overdue status
5. **Notification**: Kafka event published for care team alerts

## Configuration

```yaml
server:
  port: 8086
  servlet:
    context-path: /care-gap

# Service integrations
cql:
  engine:
    url: http://localhost:8081/cql-engine
patient:
  service:
    url: http://localhost:8084/patient
fhir:
  server:
    url: http://localhost:8085/fhir

# Cache configuration
spring.cache:
  type: redis
  redis:
    time-to-live: 300000  # 5 minutes for gap results

# Kafka configuration
spring.kafka:
  bootstrap-servers: localhost:9092
  producer:
    key-serializer: org.apache.kafka.common.serialization.StringSerializer
    value-serializer: org.apache.kafka.common.serialization.StringSerializer
```

## Response Examples

**Gap Statistics**:

```json
{
  "totalGaps": 12,
  "openGaps": 8,
  "closedGaps": 4,
  "highPriorityGaps": 3,
  "overdueGaps": 2,
  "gapsByCategory": {
    "Cancer Screening": 3,
    "Diabetes Management": 2,
    "Preventive Care": 3
  }
}
```

**Population Report**:

```json
{
  "tenantId": "tenant-1",
  "totalPatients": 5000,
  "patientsWithGaps": 1200,
  "totalOpenGaps": 3500,
  "averageGapsPerPatient": 2.9,
  "topGapCategories": [
    {"category": "Cancer Screening", "count": 800},
    {"category": "Diabetes Management", "count": 600},
    {"category": "Preventive Care", "count": 500}
  ],
  "priorityDistribution": {
    "HIGH": 1000,
    "MEDIUM": 1500,
    "LOW": 1000
  }
}
```

## Dependencies

- **Spring Boot**: Web, JPA, Validation, Cache
- **Database**: PostgreSQL with Liquibase migrations
- **Cache**: Redis (5 min TTL for gap results)
- **HTTP Client**: OpenFeign for CQL Engine, Patient Service integration
- **Messaging**: Kafka for gap identification events
- **Resilience**: Circuit breakers for service failures

## Running Locally

```bash
# Start dependencies (CQL Engine, Patient Service)
docker compose up -d cql-engine-service patient-service

# From backend directory
./gradlew :modules:services:care-gap-service:bootRun

# Or via Docker
docker compose --profile care-gap up care-gap-service
```

## Testing

```bash
# Unit tests
./gradlew :modules:services:care-gap-service:test

# Identify all care gaps for patient
curl -X POST http://localhost:8086/care-gap/identify?patient=p123 \
  -H "X-Tenant-ID: tenant-1"

# Get open care gaps
curl http://localhost:8086/care-gap/open?patient=p123 \
  -H "X-Tenant-ID: tenant-1"

# Get high priority gaps
curl http://localhost:8086/care-gap/high-priority?patient=p123 \
  -H "X-Tenant-ID: tenant-1"

# Get overdue gaps
curl http://localhost:8086/care-gap/overdue?patient=p123 \
  -H "X-Tenant-ID: tenant-1"

# Close a gap
curl -X POST http://localhost:8086/care-gap/close \
  -H "X-Tenant-ID: tenant-1" \
  -d "gapId=gap-uuid&closedBy=provider-123&closureReason=Screening completed&closureAction=Colonoscopy performed"

# Get population report
curl http://localhost:8086/care-gap/population-report \
  -H "X-Tenant-ID: tenant-1"
```

## Use Cases

- **Quality Measure Reporting**: Identify gaps before CMS submission deadlines
- **Care Coordination**: Prioritized worklists for care managers
- **Value-Based Care**: Track closure rates for HEDIS/Stars improvement
- **Population Health**: Identify cohorts needing outreach (e.g., overdue cancer screenings)
