# HDIM System Architecture

**Version**: 1.0
**Last Updated**: December 30, 2025
**Status**: Production

---

## Table of Contents

1. [Overview](#overview)
2. [High-Level Architecture](#high-level-architecture)
3. [Service Catalog](#service-catalog)
4. [Communication Patterns](#communication-patterns)
5. [Data Flow Examples](#data-flow-examples)
6. [Infrastructure](#infrastructure)
7. [Security Architecture](#security-architecture)
8. [Technology Decisions](#technology-decisions)

---

## Overview

HealthData-in-Motion (HDIM) is a distributed healthcare interoperability platform consisting of **28 microservices** organized into 5 functional domains:

| Domain | Services | Purpose |
|--------|----------|---------|
| **Core Clinical** | 7 | HEDIS measures, CQL execution, care gaps, risk adjustment |
| **Integration** | 8 | FHIR R4, patient data, EHR connectivity, event processing |
| **Analytics** | 5 | Reporting, predictive models, QRDA export |
| **Platform** | 5 | Gateway, notifications, approvals, documentation |
| **Business** | 3 | Sales automation, AI agents |

### Key Architectural Principles

1. **FHIR R4 Native**: No translation layers; FHIR resources used directly throughout
2. **CQL-Native Execution**: Direct execution of NCQA CQL specifications
3. **Gateway Trust Authentication**: Services trust gateway-validated headers
4. **Multi-Tenant Isolation**: All data segregated by `tenantId`
5. **HIPAA Compliance**: 5-minute PHI cache TTL, comprehensive audit logging
6. **Event-Driven**: Asynchronous communication via Apache Kafka

---

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            CLIENT APPLICATIONS                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │  Clinical   │  │   Admin     │  │  External   │  │   Health Plan      │ │
│  │   Portal    │  │   Portal    │  │    EHRs     │  │     Systems        │ │
│  │  (Angular)  │  │  (Angular)  │  │ (Epic,etc)  │  │   (Payers)         │ │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └─────────┬───────────┘ │
└─────────┼────────────────┼────────────────┼───────────────────┼─────────────┘
          │                │                │                   │
          └────────────────┴────────────────┴───────────────────┘
                                    │
                                    │ HTTPS (TLS 1.3)
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         KONG API GATEWAY (8000)                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │     JWT     │  │    Rate     │  │   Request   │  │      Header         │ │
│  │ Validation  │  │  Limiting   │  │   Routing   │  │    Injection        │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  │  (X-Auth-*)         │ │
│                                                      └─────────────────────┘ │
└─────────────────────────────────────┬───────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       GATEWAY SERVICE (8001)                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                          │
│  │   Service   │  │   Circuit   │  │   Load      │                          │
│  │  Discovery  │  │  Breaking   │  │  Balancing  │                          │
│  └─────────────┘  └─────────────┘  └─────────────┘                          │
└─────────────────────────────────────┬───────────────────────────────────────┘
                                      │
          ┌───────────────────────────┼───────────────────────────┐
          │                           │                           │
          ▼                           ▼                           ▼
┌─────────────────────┐   ┌─────────────────────┐   ┌─────────────────────┐
│   CORE CLINICAL     │   │    INTEGRATION      │   │     ANALYTICS       │
│     SERVICES        │   │      SERVICES       │   │      SERVICES       │
├─────────────────────┤   ├─────────────────────┤   ├─────────────────────┤
│ • Quality Measure   │   │ • FHIR Service      │   │ • Analytics         │
│   (8087)            │   │   (8085)            │   │   Service (8098)    │
│ • CQL Engine        │   │ • Patient Service   │   │ • Predictive        │
│   (8081)            │   │   (8084)            │   │   Analytics (8099)  │
│ • Care Gap (8086)   │   │ • EHR Connector     │   │ • QRDA Export       │
│ • HCC (8088)        │   │   (8092)            │   │   (8100)            │
│ • Prior Auth (8089) │   │ • CDR Processor     │   │ • Payer Workflows   │
│ • SDOH (8090)       │   │   (8093)            │   │   (8101)            │
│ • Consent (8091)    │   │ • ECR (8094)        │   │ • Migration         │
│                     │   │ • Data Enrichment   │   │   Workflow (8102)   │
│                     │   │   (8095)            │   │                     │
│                     │   │ • Event Processing  │   │                     │
│                     │   │   (8096)            │   │                     │
│                     │   │ • Event Router      │   │                     │
│                     │   │   (8097)            │   │                     │
└─────────────────────┘   └─────────────────────┘   └─────────────────────┘
          │                           │                           │
          └───────────────────────────┼───────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          PLATFORM SERVICES                                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │ Notification│  │  Approval   │  │Documentation│  │   AI Assistant      │ │
│  │   (8103)    │  │   (8104)    │  │   (8105)    │  │     (8106)          │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          BUSINESS SERVICES                                   │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐  │
│  │  Sales Automation   │  │   Agent Builder     │  │   Agent Runtime     │  │
│  │       (8107)        │  │      (8108)         │  │      (8109)         │  │
│  └─────────────────────┘  └─────────────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       DATA & MESSAGING LAYER                                 │
├─────────────────────────┬─────────────────────────┬─────────────────────────┤
│     PostgreSQL (5435)   │      Redis (6380)       │     Kafka (9094)        │
│  ┌─────────────────┐    │  ┌─────────────────┐    │  ┌─────────────────┐    │
│  │  Persistent     │    │  │  Cache Layer    │    │  │ Event Streaming │    │
│  │  Data Store     │    │  │  (5-min TTL)    │    │  │ Async Messages  │    │
│  │                 │    │  │                 │    │  │                 │    │
│  │ Multi-tenant    │    │  │ Session Store   │    │  │ Service         │    │
│  │ via tenantId    │    │  │                 │    │  │ Integration     │    │
│  └─────────────────┘    │  └─────────────────┘    │  └─────────────────┘    │
└─────────────────────────┴─────────────────────────┴─────────────────────────┘
```

---

## Service Catalog

### Core Clinical Services (7)

Services responsible for clinical quality measurement, care gap detection, and patient outcomes.

| Service | Port | Purpose | Key Dependencies |
|---------|------|---------|------------------|
| **Quality Measure Service** | 8087 | HEDIS measure evaluation, 56 measures | CQL Engine, FHIR Service |
| **CQL Engine Service** | 8081 | Clinical Quality Language execution | FHIR Service |
| **Care Gap Service** | 8086 | Care gap detection and tracking | Quality Measure, Patient Service |
| **HCC Service** | 8088 | Hierarchical Condition Category risk scoring | Patient Service, FHIR Service |
| **Prior Authorization Service** | 8089 | Prior auth workflow management | FHIR Service, Patient Service |
| **SDOH Service** | 8090 | Social determinants of health tracking | Patient Service, FHIR Service |
| **Consent Service** | 8091 | Patient consent management (HIPAA) | Patient Service |

### Integration Services (8)

Services responsible for data ingestion, FHIR processing, and external system connectivity.

| Service | Port | Purpose | Key Dependencies |
|---------|------|---------|------------------|
| **FHIR Service** | 8085 | FHIR R4 resource management (HAPI FHIR 7.x) | PostgreSQL, Redis |
| **Patient Service** | 8084 | Patient demographics and registry | FHIR Service |
| **EHR Connector Service** | 8092 | EHR integration (Epic, Cerner, Athena) | FHIR Service |
| **CDR Processor Service** | 8093 | Clinical data repository processing | FHIR Service |
| **ECR Service** | 8094 | Electronic case reporting | FHIR Service, Patient Service |
| **Data Enrichment Service** | 8095 | Clinical data enrichment | FHIR Service |
| **Event Processing Service** | 8096 | Clinical event processing | Kafka |
| **Event Router Service** | 8097 | Event routing and orchestration | Kafka |

### Analytics Services (5)

Services responsible for reporting, analytics, and data export.

| Service | Port | Purpose | Key Dependencies |
|---------|------|---------|------------------|
| **Analytics Service** | 8098 | Quality measure reporting and dashboards | Quality Measure, Care Gap |
| **Predictive Analytics Service** | 8099 | Risk prediction and ML models | Patient Service, HCC Service |
| **QRDA Export Service** | 8100 | QRDA I/III export generation | Quality Measure, Patient Service |
| **Payer Workflows Service** | 8101 | Payer-specific workflow automation | Quality Measure, Analytics |
| **Migration Workflow Service** | 8102 | Data migration orchestration | All services |

### Platform Services (5)

Services providing cross-cutting platform capabilities.

| Service | Port | Purpose | Key Dependencies |
|---------|------|---------|------------------|
| **Gateway Service** | 8001 | Internal routing and service discovery | Kong |
| **Notification Service** | 8103 | Email/SMS notifications | Kafka |
| **Approval Service** | 8104 | Workflow approvals | Kafka |
| **Documentation Service** | 8105 | Documentation generation | FHIR Service |
| **AI Assistant Service** | 8106 | AI-powered clinical assistance | Various |

### Business Services (3)

Services supporting business operations and AI capabilities.

| Service | Port | Purpose | Key Dependencies |
|---------|------|---------|------------------|
| **Sales Automation Service** | 8107 | CRM and sales pipeline | PostgreSQL |
| **Agent Builder Service** | 8108 | AI agent configuration | PostgreSQL |
| **Agent Runtime Service** | 8109 | AI agent execution | Agent Builder |

---

## Communication Patterns

### Synchronous (REST over HTTPS)

**Use Case**: Direct request/response operations requiring immediate results.

```
Clinical Portal → Kong (8000) → Gateway (8001) → Quality Measure (8087) → CQL Engine (8081)
```

**Protocol**: HTTPS with TLS 1.3
**Authentication**: Gateway Trust (X-Auth-* headers)
**Latency Target**: <200ms p95

**Required Headers**:
| Header | Source | Purpose |
|--------|--------|---------|
| `X-Tenant-ID` | Client | Multi-tenant isolation |
| `X-Auth-User-Id` | Gateway | User identification |
| `X-Auth-Roles` | Gateway | Authorization |
| `X-Auth-Tenant-Ids` | Gateway | Authorized tenants |
| `X-Auth-Validated` | Gateway | HMAC signature |

### Asynchronous (Apache Kafka)

**Use Case**: Event-driven updates, cross-service notifications, audit logging.

**Kafka Topics**:

| Topic | Producer(s) | Consumer(s) | Purpose |
|-------|-------------|-------------|---------|
| `patient.events` | Patient Service | Care Gap, Analytics, Notification | Patient CRUD events |
| `measure.evaluation.complete` | Quality Measure | Care Gap, Analytics | Evaluation results |
| `care-gap.detected` | Care Gap Service | Notification | New care gaps identified |
| `care-gap.closed` | Care Gap Service | Analytics | Care gap closures |
| `audit.events` | All services | Audit Service | HIPAA audit trail |
| `notification.requests` | Various | Notification Service | Email/SMS requests |

**Message Schema**:
```json
{
  "eventId": "uuid",
  "eventType": "PATIENT_CREATED",
  "tenantId": "string",
  "timestamp": "ISO-8601",
  "source": "patient-service",
  "payload": {}
}
```

### Caching Strategy (Redis)

**PHI Data**: 5-minute TTL maximum (HIPAA compliance)
**Reference Data**: 1-hour TTL (measure definitions, code systems)
**Session Data**: 15-minute TTL (user sessions)

**Key Pattern**: `{tenantId}:{resourceType}:{resourceId}`

**Example**:
```
TENANT001:Patient:12345 → Patient FHIR resource (TTL 300s)
TENANT001:MeasureDefinition:BCS → Breast Cancer Screening definition (TTL 3600s)
```

---

## Data Flow Examples

### HEDIS Measure Evaluation

```
1. Clinical Portal (Angular)
   │
   │ POST /api/v1/evaluations
   │ Body: { "patientIds": [...], "measureId": "BCS" }
   ▼
2. Kong API Gateway (8000)
   │ - Validates JWT token
   │ - Injects X-Auth-* headers (User ID, Roles, Tenants)
   │ - Adds X-Auth-Validated (HMAC signature)
   ▼
3. Gateway Service (8001)
   │ - Routes to Quality Measure Service
   │ - Circuit breaker check
   ▼
4. Quality Measure Service (8087)
   │ - Fetches measure definition (BCS - Breast Cancer Screening)
   │ - Fetches patient FHIR data via FHIR Service
   │ - Calls CQL Engine for evaluation
   │
   ├──▶ FHIR Service (8085)
   │    │ - Returns Patient, Observation, Condition resources
   │    │ - Cache hit/miss (5-min TTL for PHI)
   │    ▼
   │    PostgreSQL (5435)
   │
   └──▶ CQL Engine Service (8081)
        │ - Executes CQL against FHIR data
        │ - Returns: { "numerator": true, "denominator": true }
        ▼
5. Quality Measure Service (8087)
   │ - Stores results in PostgreSQL
   │ - Publishes to Kafka: measure.evaluation.complete
   │ - Returns results to portal
   │
   └──▶ Kafka (9094)
        │ Topic: measure.evaluation.complete
        ▼
6. Care Gap Service (8086) [async]
   │ - Consumes measure.evaluation.complete
   │ - Identifies care gaps (numerator=false, denominator=true)
   │ - Publishes to Kafka: care-gap.detected
   │
   └──▶ Kafka (9094)
        │ Topic: care-gap.detected
        ▼
7. Notification Service (8103) [async]
   │ - Consumes care-gap.detected
   │ - Sends provider notification (email/SMS)
   ▼
   External Email/SMS Provider
```

### Patient Data Ingestion (FHIR Bulk Import)

```
1. EHR System (Epic/Cerner)
   │
   │ FHIR Bulk Data Export (NDJSON)
   ▼
2. EHR Connector Service (8092)
   │ - Authenticates with EHR (OAuth2)
   │ - Downloads NDJSON files
   │ - Validates FHIR R4 compliance
   ▼
3. FHIR Service (8085)
   │ - Stores Patient, Observation, Condition resources
   │ - Multi-tenant isolation (tenantId)
   │ - Publishes to Kafka: patient.events
   │
   └──▶ Kafka (9094)
        │ Topic: patient.events
        │
        ├──▶ Care Gap Service (8086)
        │    - Triggers care gap recalculation
        │
        └──▶ Analytics Service (8098)
             - Updates population health metrics
```

---

## Infrastructure

### Development Environment

**Deployment**: Docker Compose (`docker-compose.yml`)

| Component | Configuration |
|-----------|---------------|
| Services | All 28 services in containers |
| PostgreSQL | Single database, multi-tenant via tenantId |
| Redis | Single instance, namespaced by tenant |
| Kafka | Single broker, multiple topics |
| Kong | API gateway with JWT plugin |

**Quick Start**:
```bash
# Start all services
docker compose up -d

# Start core services only
docker compose --profile core up -d

# View logs
docker compose logs -f quality-measure-service
```

### Production Environment (Target)

**Deployment**: Kubernetes

| Component | Configuration |
|-----------|---------------|
| Services | Kubernetes Deployments with HPA |
| PostgreSQL | Multi-tenant clusters with PgBouncer |
| Redis | Redis Cluster with Sentinel |
| Kafka | 3+ broker cluster with replication |
| Kong | HA API gateway with database backend |

**Scaling Targets**:
- 10,000+ patients per tenant
- 200+ concurrent evaluations
- <200ms p95 latency for FHIR queries
- 99.9% uptime SLA

---

## Security Architecture

### Authentication Flow (Gateway Trust)

```
Client → Kong (JWT validation) → Gateway (header injection) → Service (trusts headers)
```

**Why Gateway Trust?**
- Traditional: Every service validates JWT + queries user database
- Gateway Trust: Gateway validates once, injects signed headers
- **Result**: 40% reduction in database load, 200ms faster latency

See: [Gateway Trust Architecture](../backend/docs/GATEWAY_TRUST_ARCHITECTURE.md)

### Multi-Tenant Isolation

**Database Level**: All queries filtered by `tenantId`
```sql
SELECT * FROM patients WHERE tenant_id = :tenantId AND id = :patientId
```

**Cache Level**: Keys namespaced by tenant
```
{tenantId}:{resourceType}:{resourceId}
```

**Kafka Level**: Tenant ID in message payload
```json
{ "tenantId": "TENANT001", "payload": {} }
```

### HIPAA Compliance Controls

| Control | Implementation |
|---------|----------------|
| PHI Cache TTL | 5 minutes maximum (Redis) |
| Encryption at Rest | AES-256 (PostgreSQL) |
| Encryption in Transit | TLS 1.3 (all services) |
| Audit Logging | All PHI access logged (Kafka → Audit Service) |
| Access Control | RBAC with tenant isolation |
| Browser Caching | Prevented via Cache-Control headers |

---

## Technology Decisions

| Technology | Version | Rationale | ADR |
|------------|---------|-----------|-----|
| **Java** | 21 LTS | Long-term support, modern features | - |
| **Spring Boot** | 3.x | Enterprise Java, security integrations | [ADR-0009](decisions/ADR-0009-spring-boot-framework.md) |
| **HAPI FHIR** | 7.x | Industry-standard FHIR R4, battle-tested | [ADR-0005](decisions/ADR-0005-hapi-fhir-selection.md) |
| **PostgreSQL** | 15 | ACID compliance, HIPAA-compliant, multi-tenant | [ADR-0007](decisions/ADR-0007-postgresql-database.md) |
| **Redis** | 7 | Fast caching with TTL support for HIPAA | [ADR-0008](decisions/ADR-0008-redis-caching-strategy.md) |
| **Apache Kafka** | 3.x | Event streaming for async communication | [ADR-0006](decisions/ADR-0006-kafka-event-streaming.md) |
| **Kong** | Latest | API gateway with plugin ecosystem | [ADR-0010](decisions/ADR-0010-kong-api-gateway.md) |
| **Angular** | 17+ | Enterprise frontend framework | - |

---

## References

- [Gateway Trust Architecture](../backend/docs/GATEWAY_TRUST_ARCHITECTURE.md) - Detailed authentication flow
- [HIPAA Cache Compliance](../backend/HIPAA-CACHE-COMPLIANCE.md) - PHI cache requirements
- [Backend API Specification](../BACKEND_API_SPECIFICATION.md) - API design patterns
- [Terminology Glossary](TERMINOLOGY_GLOSSARY.md) - Official terminology

---

## Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-12-30 | Architecture Team | Initial creation |

---

*This document is the authoritative source for HDIM system architecture. For questions, contact the Architecture Team.*
