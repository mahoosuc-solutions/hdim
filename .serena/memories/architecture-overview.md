# HDIM Architecture Overview

## System Summary
**HealthData-in-Motion (HDIM)** - Enterprise healthcare interoperability platform for HEDIS quality measure evaluation, FHIR R4 compliance, and clinical decision support.

## Primary Purpose
Enable healthcare organizations to:
- Evaluate clinical quality measures (CQL/HEDIS)
- Identify care gaps
- Perform risk stratification
- Generate quality reports for value-based care contracts

## Target Users
- Healthcare payers
- ACOs (Accountable Care Organizations)
- Health systems
- Clinical quality teams

## Tech Stack Summary

### Backend
- **Language**: Java 21 (LTS)
- **Framework**: Spring Boot 3.x
- **Build**: Gradle 8.11+ (Kotlin DSL)
- **FHIR**: HAPI FHIR 7.x (R4)
- **Database**: PostgreSQL 15
- **Cache**: Redis 7
- **Messaging**: Apache Kafka 3.x
- **API Gateway**: Kong
- **Security**: Spring Security + JWT

### Frontend
- **Framework**: Angular 17+
- **State**: RxJS
- **UI**: Angular Material

### Infrastructure
- **Containers**: Docker + Docker Compose
- **Orchestration**: Kubernetes (optional)
- **Monitoring**: Prometheus + Grafana
- **Secrets**: HashiCorp Vault

## Core Services (28 Total)

### Quality Measurement (Core)
| Service | Port | Purpose |
|---------|------|---------|
| quality-measure-service | 8087 | HEDIS measure evaluation engine |
| cql-engine-service | 8081 | CQL expression evaluation |

### Data Management (Core)
| Service | Port | Purpose |
|---------|------|---------|
| fhir-service | 8085 | FHIR R4 resource management |
| patient-service | 8084 | Patient demographics & data |

### Clinical Intelligence
| Service | Port | Purpose |
|---------|------|---------|
| care-gap-service | 8086 | Care gap detection & tracking |
| predictive-analytics-service | 8089 | ML-based predictions |
| hcc-service | 8093 | HCC risk adjustment |
| sdoh-service | 8098 | Social determinants of health |

### Integration
| Service | Port | Purpose |
|---------|------|---------|
| ehr-connector-service | 8090 | EHR system integration |
| cms-connector-service | 8091 | CMS data access |
| analytics-service | 8088 | Quality reporting & dashboards |
| qrda-export-service | 8095 | QRDA I/III export |

### Platform
| Service | Port | Purpose |
|---------|------|---------|
| gateway-service | 8001 | API gateway & routing |
| consent-service | 8092 | Patient consent management |
| prior-auth-service | 8094 | Prior authorization workflows |

### Infrastructure
| Component | Port | Purpose |
|-----------|------|---------|
| Kong API Gateway | 8000 | External API gateway |
| PostgreSQL | 5435 | Primary database |
| Redis | 6380 | Cache & sessions |
| Kafka | 9094 | Event streaming |
| Prometheus | 9090 | Metrics collection |
| Grafana | 3001 | Monitoring dashboards |

## Request Flow

```
Client
  → Kong Gateway (8000)
    → Gateway Service (8001) - JWT validation, injects X-Auth-* headers
      → Backend Service - Trusts gateway headers
        → PostgreSQL / Redis / Kafka
```

## Key Architecture Patterns

### 1. Gateway Trust Authentication
- Gateway validates JWT tokens
- Injects trusted `X-Auth-*` headers (User-Id, Tenant-Ids, Roles)
- Backend services trust these headers (no JWT re-validation)
- Production: HMAC signature validation
- Development: `GATEWAY_AUTH_DEV_MODE=true`

### 2. Multi-Tenant Isolation
- All data queries MUST filter by `tenantId`
- Enforced at repository layer
- TrustedTenantAccessFilter validates tenant access

### 3. HIPAA Compliance
- PHI cache TTL ≤ 5 minutes
- No-cache headers on PHI responses
- Audit logging on all PHI access
- Encrypted at rest and in transit

### 4. FHIR R4 Interoperability
- HAPI FHIR 7.x for resource handling
- Support for core clinical resources (Patient, Observation, Procedure, etc.)
- Custom extensions for quality measures

### 5. Event-Driven Architecture
- Kafka for async communication
- Care gap events, quality measure updates
- Predictive analytics triggers

## Project Structure

```
hdim-master/
├── backend/
│   ├── modules/
│   │   ├── services/           # 28 microservices
│   │   └── shared/             # Shared libraries
│   │       ├── domain/
│   │       ├── infrastructure/
│   │       └── api-contracts/
│   └── platform/
├── apps/                       # Angular frontend
│   ├── clinical-portal/
│   └── clinical-portal-e2e/
├── docker/                     # Infrastructure configs
└── docs/                       # Documentation
```

## Build Status
✅ All 28 services compile successfully (verified December 2025)
✅ agent-runtime-service: 84 tests passing
✅ agent-builder-service: Build successful

## Critical Documentation References
- `CLAUDE.md` - AI coding guidelines (THIS FILE IS LAW)
- `backend/docs/GATEWAY_TRUST_ARCHITECTURE.md` - Auth architecture
- `backend/HIPAA-CACHE-COMPLIANCE.md` - PHI handling rules
- `backend/docs/ENTITY_MIGRATION_GUIDE.md` - Database synchronization
- `docs/architecture/SYSTEM_ARCHITECTURE.md` - Complete system design

## Development Workflow
1. Use Gradle from `backend/` directory
2. Use Docker Compose for local environment
3. Follow entity-migration sync rules (JPA ↔ Liquibase)
4. Run validation tests before commits
5. Never use `ddl-auto: update` or `create`
