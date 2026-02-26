# HDIM — Platform Architecture

*For solution architects, engineering leads, and technical due diligence.*

---

## Design Philosophy

HDIM was built with five architectural principles:

1. **Standards-first**: FHIR R4 is the data model. CQL is the measure language. No proprietary formats.
2. **Event-driven**: Every mutation produces an immutable event. The system can always answer "what happened and when."
3. **Tenant-isolated**: Every query, cache entry, and event is scoped to a tenant. There is no global state.
4. **API-first**: Every capability is exposed via REST. The clinical portal consumes the same APIs a partner would.
5. **Operationally transparent**: Distributed tracing, structured logging, and metrics are built in — not bolted on.

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           CLIENTS                                       │
│  Clinical Portal (Angular 17)  │  Partner APIs  │  EHR (CDS Hooks)     │
└──────────────────────┬──────────────────────────────────────────────────┘
                       │ HTTPS
┌──────────────────────▼──────────────────────────────────────────────────┐
│                    GATEWAY EDGE (nginx :18080)                           │
│  Rate limiting · TLS termination · Route dispatch                       │
│                                                                         │
│  ┌─────────────────┐  ┌──────────────────┐  ┌───────────────────────┐  │
│  │ Gateway Admin    │  │ Gateway FHIR     │  │ Gateway Clinical      │  │
│  │ Auth, Audit,     │  │ FHIR, Patient,   │  │ CareGap, Events,     │  │
│  │ Agent Builder,   │  │ CQL Engine,      │  │ HCC, QRDA, Consent,  │  │
│  │ Sales, Ops       │  │ Quality Measure  │  │ Prior Auth            │  │
│  └────────┬─────────┘  └────────┬─────────┘  └────────┬──────────────┘  │
│           │ JWT validation + trusted header injection  │                 │
└───────────┼─────────────────────┼──────────────────────┼────────────────┘
            │                     │                      │
┌───────────▼─────────────────────▼──────────────────────▼────────────────┐
│                       DOMAIN SERVICES                                   │
│                                                                         │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌──────────────────┐  │
│  │ Patient    │  │ FHIR       │  │ CQL Engine │  │ Quality Measure  │  │
│  │ Service    │  │ Service    │  │ Service    │  │ Service          │  │
│  │ :8084      │  │ :8085      │  │ :8081      │  │ :8087            │  │
│  └────────────┘  └────────────┘  └────────────┘  └──────────────────┘  │
│                                                                         │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌──────────────────┐  │
│  │ Care Gap   │  │ HCC        │  │ Audit Query│  │ Event Processing │  │
│  │ Service    │  │ Service    │  │ Service    │  │ Service          │  │
│  │ :8086      │  │ :8105      │  │ :8088      │  │ :8083            │  │
│  └────────────┘  └────────────┘  └────────────┘  └──────────────────┘  │
└────────────────────┬────────────────────────────────────┬───────────────┘
                     │                                    │
        ┌────────────▼────────────┐          ┌────────────▼────────────┐
        │    DATA LAYER           │          │    EVENT BUS            │
        │                         │          │                         │
        │  PostgreSQL 16          │          │  Apache Kafka (KRaft)   │
        │  29 databases           │          │  Topic-per-aggregate    │
        │  Liquibase migrations   │          │  Dead letter queues     │
        │                         │          │  Event replay           │
        │  Redis 7                │          │                         │
        │  HIPAA-compliant cache  │          │  Event Handler Services │
        │  TTL ≤ 5 min for PHI   │          │  (read-side projections)│
        └─────────────────────────┘          └─────────────────────────┘
```

---

## Gateway Layer

HDIM uses a **4-gateway modularized architecture** to separate concerns and limit blast radius.

### Gateway Edge (nginx)

The edge router handles TLS termination, rate limiting, and dispatches requests to domain-specific gateway services based on URL prefix.

| URL Prefix | Gateway | Services Behind It |
|------------|---------|-------------------|
| `/api/v1/auth/*`, `/api/v1/audit/*`, `/agent-builder/*` | gateway-admin | Auth, audit, agent builder, ops |
| `/fhir/*`, `/patient/*`, `/cql-engine/*`, `/quality-measure/*` | gateway-fhir | FHIR resources, patient, CQL, measures |
| `/care-gap/*`, `/events/*`, `/hcc/*`, `/qrda/*`, `/consent/*` | gateway-clinical | Care gaps, events, HCC, reporting |
| `/demo/*` | demo-seeding-service | Demo data management (non-prod) |

### Gateway Services (Spring Boot)

Each gateway service:

1. **Validates JWT tokens** from HttpOnly cookies (`hdim_access_token`)
2. **Resolves user identity** and tenant access
3. **Injects trusted headers**: `X-Auth-User-Id`, `X-Auth-Username`, `X-Auth-Roles`, `X-Auth-Tenant-Ids`, `X-Auth-Validated`
4. **Routes** to backend domain services
5. **Enforces rate limits** and request size limits

All three gateways share a `gateway-core` module that standardizes authentication, rate limiting, and header propagation.

**Auth enforcement** is configurable: `GATEWAY_AUTH_ENFORCED=true` in production, `false` in demo mode for unauthenticated access.

---

## Domain Services

### FHIR Service (:8085)

Full **FHIR R4** resource server implementing 20+ resource types.

| Capability | Implementation |
|------------|----------------|
| Resource CRUD | `/fhir/Patient`, `/fhir/Observation`, `/fhir/Condition`, etc. |
| CapabilityStatement | `/fhir/metadata` |
| SMART on FHIR | Authorization + configuration endpoints |
| Bulk Data Export | `$export` operation |
| Subscriptions | Real-time change notifications |
| Content type | `application/fhir+json` |

FHIR resources are tenant-scoped and stored in `fhir_db` (PostgreSQL). The service supports all standard FHIR operations: create, read, update, delete, search, history.

### Patient Service (:8084)

Aggregates patient data from the FHIR service and presents it in clinically useful formats.

| Endpoint | Purpose |
|----------|---------|
| `/patient/health-record` | Complete FHIR Bundle for a patient |
| `/patient/timeline` | Chronological clinical event timeline |
| `/patient/health-status` | Dashboard health status summary |
| `/patient/risk-assessment/{id}` | Risk stratification assessment |
| `/api/v1/providers/{id}/panel` | Provider panel management |
| `/api/v1/providers/{id}/pre-visit-plan` | Pre-visit gap closure planning |

### CQL Engine Service (:8081)

Executes **Clinical Quality Language (CQL)** expressions against patient FHIR data.

**Evaluation pipeline:**

```
CQL Library (source) → ELM (compiled) → Patient FHIR Bundle → Evaluation → Results
```

1. CQL libraries stored as versioned, tenant-scoped entities
2. CQL compiled to ELM (Expression Logical Model) for efficient evaluation
3. Patient data fetched as FHIR Bundle from FHIR service
4. CQL engine evaluates expressions against the bundle
5. Results include numerator/denominator compliance, individual outputs

| Endpoint | Purpose |
|----------|---------|
| `POST /api/v1/cql/evaluations` | Execute single-patient evaluation |
| `POST /api/v1/cql/evaluations/batch` | Batch evaluation across population |
| `GET /api/v1/cql/libraries` | Library CRUD (versioned, tenant-scoped) |
| `GET /api/v1/cql/valuesets` | Value set management |

### Quality Measure Service (:8087)

Manages the measure registry and provides production-grade HEDIS calculators.

**Built-in HEDIS measures:**

| Measure ID | Name | Sub-measures |
|-----------|------|-------------|
| **CDC** | Comprehensive Diabetes Care | HbA1c Testing, HbA1c <8%, HbA1c >9% (inverse), Eye Exam, Nephropathy Screening, BP Control |
| **BCS** | Breast Cancer Screening | Mammogram within 2 years |
| **CCS** | Cervical Cancer Screening | Pap smear / HPV testing |
| **COL** | Colorectal Cancer Screening | Colonoscopy / FIT / FOBT |
| **CBP** | Controlling High Blood Pressure | BP < 140/90 |
| **SPC** | Statin Therapy (CVD) | Statin adherence PDC ≥ 80% |

Additional capabilities:
- Custom measure builder (visual CQL editor)
- AI-assisted measure generation from natural language
- CDS Hooks integration for EHR-embedded alerts
- Provider performance analytics
- Patient health score aggregation
- Measure version management with audit trail

### Care Gap Service (:8086)

Identifies, tracks, and manages care gap closure across populations.

| Endpoint | Purpose |
|----------|---------|
| `POST /care-gap/identify` | Evaluate all applicable measures for a patient |
| `POST /care-gap/identify/{library}` | Evaluate specific measure |
| `POST /care-gap/close` | Close gap with intervention documentation |
| `POST /care-gap/bulk-close` | Bulk close at population scale |
| `GET /care-gap/open` | Open gaps (filterable) |
| `GET /care-gap/high-priority` | Priority-sorted gap list |
| `GET /care-gap/population-report` | Aggregate population reporting |

**Care gap lifecycle:**

```
OPEN → IN_PROGRESS → CLOSED (with closure documentation)
                   → CANCELLED (exclusion applied)
```

Each gap includes: patient ID, measure ID, category (HEDIS/CMS/custom), priority, severity, Star Rating impact, due date, and recommended intervention.

### HCC Service (:8105)

Hierarchical Condition Category risk adjustment for Medicare Advantage revenue optimization.

- **RAF score calculation** using CMS-HCC V24/V28 model blending
- **Documentation gap identification**: ICD-10 codes that should be recaptured
- **Recapture opportunity tracking**: Revenue impact per patient per condition

### Audit Query Service (:8088)

HIPAA audit trail for all PHI access.

- Every API call logged via HTTP Audit Interceptor (100% coverage)
- Queryable audit logs with statistics
- Kafka-backed ingestion from all services
- Retention policies per tenant

---

## Event Sourcing Architecture (CQRS)

HDIM uses **Command Query Responsibility Segregation (CQRS)** with event sourcing for core domain aggregates.

### Components

| Layer | Services | Purpose |
|-------|----------|---------|
| **Command** | patient-event-service, care-gap-event-service, quality-measure-event-service, clinical-workflow-event-service | Accept commands, validate, produce events |
| **Event Store** | event-store-service | Append-only event log (PostgreSQL) |
| **Event Bus** | Apache Kafka | Distribute events to consumers |
| **Event Router** | event-router-service | Intelligent routing to downstream handlers |
| **Read Side** | *-event-handler-services | Materialize projections from events |
| **Event Replay** | event-replay-service | Rebuild projections from event history |
| **Dead Letter** | event-processing-service | Failed event management and retry |

### Event Flow

```
Command → Event Service → Event Store (persist) → Kafka (publish)
                                                       │
                                              ┌────────▼────────┐
                                              │  Event Handlers  │
                                              │  (projections)   │
                                              └────────┬─────────┘
                                                       │
                                              ┌────────▼────────┐
                                              │  Read Models     │
                                              │  (query-optimized│
                                              │   materialized   │
                                              │   views)         │
                                              └──────────────────┘
```

### Why Event Sourcing

- **Audit trail**: Every clinical event is immutable — critical for HIPAA
- **Replay**: Can rebuild any projection from event history
- **Temporal queries**: "What was this patient's status as of [date]?"
- **Loose coupling**: Services communicate via events, not direct calls
- **Scalability**: Read and write sides scale independently

---

## Data Architecture

### Database Strategy

HDIM uses **database-per-service** with PostgreSQL 16.

| Database | Service | Key Tables |
|----------|---------|-----------|
| `fhir_db` | FHIR Service | FHIR R4 resource tables |
| `patient_db` | Patient Service | Patient demographics, timeline |
| `cql_db` | CQL Engine | CQL libraries, evaluations, value sets |
| `quality_db` | Quality Measure | Measures, results, health scores, CDS rules, care teams |
| `caregap_db` | Care Gap | Gaps, closures, recommendations |
| `hcc_db` | HCC | Patient risk profiles, HCC mappings, recapture opportunities |
| `event_db` | Event Processing | Dead letter queue |
| `audit_db` | Audit Query | Audit log entries |
| `gateway_db` | Gateway Admin | Users, roles, tenants, refresh tokens |

All tables include `tenant_id` column. All queries filter by tenant. Unique constraints are scoped to `(tenant_id, ...)`.

### Schema Management

- **Liquibase** for all migrations (never Flyway, never Hibernate auto-DDL)
- `ddl-auto: validate` in all environments — Hibernate validates entities match the actual schema at startup
- Every migration includes rollback directives
- Entity-migration validation runs in CI for every PR that touches entities or migrations
- 199/199 changesets have rollback coverage

### Caching Layer

- **Redis 7** for hot-path caching
- **HIPAA-compliant TTLs**: ≤ 5 minutes for any PHI data
- `Cache-Control: no-store, no-cache, must-revalidate` on all PHI responses
- Tenant-scoped cache keys

---

## Multi-Tenancy

Multi-tenancy is enforced at every layer.

| Layer | Enforcement |
|-------|-------------|
| **Gateway** | Validates `X-Tenant-ID` header, checks user-tenant association |
| **Service** | `TenantFilter` in shared persistence module applies tenant scope |
| **Database** | `tenant_id` column on all tables, included in unique constraints |
| **Cache** | Tenant-prefixed cache keys |
| **Events** | Tenant ID included in every event payload |
| **Audit** | Every audit entry records tenant context |

Tenants are fully isolated: Tenant A cannot see, query, or affect Tenant B's data at any layer.

---

## Observability

### Distributed Tracing

- **OpenTelemetry** SDK in all services
- Trace propagation across HTTP (RestTemplate/Feign) and Kafka
- **Jaeger** UI for trace visualization (:16686)
- Custom spans for business operations (measure evaluation, gap identification)

### Metrics

- **Micrometer** metrics in all services
- **Prometheus** scraping (:9090)
- **Grafana** dashboards (:3001)
- Key metrics: evaluation latency, gap closure rate, API response times, Kafka consumer lag

### Logging

- Structured JSON logging
- Correlation IDs propagated across services
- PHI filtering via `LoggerService` (frontend) and backend sanitization
- No PHI in log messages (enforced by ESLint `no-console` + code review)

---

## Clinical Portal Architecture

### Technology

| Concern | Choice |
|---------|--------|
| Framework | Angular 17+ |
| State | RxJS (service-based) |
| UI Library | Angular Material |
| Build | Nx monorepo |
| Testing | Jest (unit), Playwright (E2E) |
| Hosting | nginx container |

### Key Capabilities

- **33 routes** covering clinical workflow, admin, analytics, and AI features
- **Role-based navigation**: Menu items filtered by user roles
- **HIPAA-compliant frontend**: LoggerService with PHI filtering, no `console.log`, session timeout with audit
- **Offline support**: PWA with offline indicator
- **Guided tours**: Interactive onboarding for new users
- **Responsive design**: Desktop-first with mobile support

### Portal Screens (by Workflow)

**Clinical Operations:**
- Dashboard (role-specific: MA, provider, RN)
- Patient roster and search
- Patient 360° detail (demographics, conditions, medications, labs, gaps, timeline)
- Care gap manager (identify, track, close, bulk operations)
- Pre-visit planning
- Care recommendations
- Outreach campaigns

**Quality Measurement:**
- Quality measure browser (HEDIS, CMS, custom)
- Measure detail with compliance trends
- Measure comparison (side-by-side)
- CQL evaluation runner (single + batch)
- Results explorer with export
- Custom report builder
- QRDA export (CMS reporting)

**Analytics & Intelligence:**
- Risk stratification (population segmentation)
- Population insights
- Visualization suite (batch monitor, quality constellation, flow network, measure matrix)
- AI clinical assistant
- AI agent builder

**Administration:**
- User management
- Tenant settings
- Audit log viewer
- Demo seeding (non-prod)
- Compliance dashboard
- QA / clinical / MPI audit dashboards

---

## Deployment Architecture

### Docker Compose (Demo / Development)

```
                    ┌──────────────────┐
                    │  clinical-portal  │ :4200
                    │  (nginx)          │
                    └────────┬──────────┘
                             │
                    ┌────────▼──────────┐
                    │  gateway-edge     │ :18080
                    │  (nginx)          │
                    └────────┬──────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
     gateway-admin    gateway-fhir   gateway-clinical
              │              │              │
     ┌────────┴──────────────┴──────────────┴────────┐
     │              Domain Services                    │
     │  patient · fhir · cql-engine · quality-measure  │
     │  care-gap · hcc · audit-query · events          │
     ├─────────────────────────────────────────────────┤
     │  Event Services (CQRS)                          │
     │  *-event-service · *-event-handler-service      │
     └──────────┬────────────────────┬─────────────────┘
                │                    │
         ┌──────▼──────┐      ┌──────▼──────┐
         │ PostgreSQL   │      │ Kafka       │
         │ (29 DBs)     │      │ (KRaft)     │
         │ :5435        │      │ :9094       │
         └──────────────┘      └─────────────┘
                │
         ┌──────▼──────┐      ┌──────────────┐
         │ Redis        │      │ Jaeger       │
         │ :6380        │      │ :16686       │
         └──────────────┘      └──────────────┘
```

### Kubernetes (Production)

- Helm charts and Kustomize overlays in `k8s/` and `kubernetes/`
- Horizontal Pod Autoscaler for domain services
- Separate node pools for compute-intensive CQL evaluation
- PersistentVolumeClaims for PostgreSQL and Kafka
- Ingress controller replacing nginx edge gateway
- Secrets via HashiCorp Vault or Kubernetes Secrets

### Memory Requirements (Demo Stack)

| Component | Memory |
|-----------|--------|
| PostgreSQL | 512 MB |
| Kafka (KRaft) | 1024 MB |
| Redis | 128 MB |
| Gateway services (×3) | 512 MB each |
| Domain services (×5) | 512–2048 MB each |
| Demo seeding service | 2048 MB |
| Clinical portal (nginx) | 256 MB |
| **Total demo stack** | **~10 GB** |

---

## Technology Stack Summary

| Layer | Technology | Version |
|-------|-----------|---------|
| **Language** | Java | 21 (LTS) |
| **Framework** | Spring Boot | 3.x |
| **Build** | Gradle | 8.11+ (Kotlin DSL) |
| **FHIR** | HAPI FHIR | 7.x |
| **Database** | PostgreSQL | 16 |
| **Cache** | Redis | 7 |
| **Messaging** | Apache Kafka | 3.x (KRaft) |
| **Migrations** | Liquibase | Latest |
| **Frontend** | Angular | 17+ |
| **UI Library** | Angular Material | 17+ |
| **Monorepo** | Nx | Latest |
| **Testing** | JUnit 5 / Mockito / Jest / Playwright | Latest |
| **Tracing** | OpenTelemetry + Jaeger | Latest |
| **Metrics** | Micrometer + Prometheus + Grafana | Latest |
| **Containers** | Docker + Docker Compose | Latest |
| **Orchestration** | Kubernetes (optional) | 1.28+ |
| **Gateway** | nginx (edge) + Spring Cloud Gateway (domain) | Latest |
| **Security** | Spring Security + JWT | Latest |

---

*HealthData-in-Motion | https://healthdatainmotion.com | February 2026*
