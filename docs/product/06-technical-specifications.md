# HDIM — Technical Specifications

*For technical evaluators, integration architects, and engineering teams conducting due diligence.*

---

## Platform Summary

| Dimension | Specification |
|-----------|---------------|
| **Backend language** | Java 21 (LTS) |
| **Backend framework** | Spring Boot 3.x |
| **Frontend framework** | Angular 17+ |
| **FHIR implementation** | HAPI FHIR 7.x (R4) |
| **Build system** | Gradle 8.11+ (Kotlin DSL), Nx (frontend) |
| **Databases** | PostgreSQL 16 (40 databases) |
| **Cache** | Redis 7 |
| **Message broker** | Apache Kafka (KRaft mode) |
| **Observability** | OpenTelemetry → Jaeger (tracing), Prometheus → Grafana (metrics) |
| **API gateway** | nginx (edge) + Spring Boot (domain gateways) |
| **Container runtime** | Docker + Docker Compose; Kubernetes optional |
| **Minimum RAM (demo)** | ~12 GB |
| **REST endpoints** | ~1,643 across all services |
| **Kafka topics** | 54 event topics |
| **Angular routes** | 37 portal routes |

---

## Service Inventory

### Core Demo Services (18 services)

| Service | Port | Memory Limit | JVM Heap | Database | Purpose |
|---------|------|-------------|----------|----------|---------|
| **Edge Gateway** | 18080 | — | — | — | nginx reverse proxy, TLS termination |
| **Admin Gateway** | internal | 512M | 128m–256m | gateway_db | Auth endpoints, user management |
| **FHIR Gateway** | internal | 512M | 128m–256m | gateway_db | FHIR API routing |
| **Clinical Gateway** | internal | 512M | 128m–256m | gateway_db | Clinical API routing |
| **FHIR Service** | 8085 | 2048M | 900m–1800m | fhir_db | FHIR R4 resource server |
| **CQL Engine** | 8081 | 512M | 512m–2g | cql_db | CQL evaluation, measure logic |
| **Patient Service** | 8084 | 512M | 512m–2g | patient_db | Patient demographics, health records |
| **Quality Measure** | 8087 | 512M | 512m–2048m | quality_db | HEDIS measure evaluation, scoring |
| **Care Gap Service** | 8086 | 512M | 256m–1024m | caregap_db | Gap identification, closure, tracking |
| **Event Processing** | 8083 | 512M | 512m–1024m | event_db | Event routing, processing |
| **HCC Service** | 8105 | 512M | 512m–1024m | hcc_db | HCC risk adjustment coding |
| **Audit Query** | 8088 | 512M | 512m–3g | audit_db | Compliance audit log queries |
| **Demo Seeding** | 8098 | 2048M | 192m–384m | healthdata_demo | Demo data population |
| **Ops Service** | 4710 | 256M | — | — | Operational monitoring |
| **Clinical Portal** | 4200 | 256M | — | — | Angular SPA (nginx) |
| **PostgreSQL** | 5435 | 512M | — | — | Primary RDBMS |
| **Redis** | 6380 | 128M | — | — | Cache + session store |
| **Kafka** | 9094 | 1024M | 256m–512m | — | Event streaming (KRaft) |

### Extended Services (Full Stack — 30+ additional)

| Service | Port | Database | Purpose |
|---------|------|----------|---------|
| Consent Service | 8082 | consent_db | Patient consent management |
| AI Assistant | 8090 | ai_assistant_db | AI-powered clinical assistant |
| Analytics Service | 8092 | analytics_db | Population analytics, dashboards |
| Predictive Analytics | 8093 | predictive_db | ML-based risk prediction |
| SDoH Service | 8094 | sdoh_db | Social determinants of health |
| Data Enrichment | 8089 | enrichment_db | Third-party data enrichment |
| Event Router | 8095 | event_router_db | Event routing rules engine |
| Event Store | 8090 | event_store_db | Immutable event log (CQRS) |
| Agent Builder | 8096 | agent_db | AI agent configuration |
| Approval Service | 8097 | approval_db | Multi-step approval workflows |
| Payer Workflows | 8098 | payer_db | Payer-specific business logic |
| CDR Processor | 8099 | cdr_db | Clinical data repository processing |
| EHR Connector | 8100 | ehr_connector_db | EHR integration adapters |
| eCR Service | 8101 | ecr_db | Electronic case reporting |
| Prior Auth | 8102 | prior_auth_db | Prior authorization workflows |
| Migration Workflow | 8103 | migration_db | Data migration orchestration |
| QRDA Export | 8104 | qrda_db | CMS quality reporting export |
| Sales Automation | 8106 | sales_automation_db | Sales pipeline management |
| Notification | 8107 | notification_db | Multi-channel notifications |
| Patient Events | 8110 | patient_event_db | Patient event sourcing |
| Care Gap Events | 8111 | care_gap_event_db | Care gap event sourcing |
| Quality Measure Events | 8112 | quality_event_db | Quality measure event sourcing |
| Clinical Workflow Events | 8113 | clinical_workflow_event_db | Clinical workflow event sourcing |
| Agent Validation | 8114 | agent_validation_db | Agent testing and validation |
| Investor Dashboard | 8120 | investor_dashboard_db | Investor metrics and reporting |
| Data Ingestion | 8200 | data_ingestion_db | Bulk data import |
| Documentation | 8091 | docs_db | API documentation service |
| Agent Runtime | 8088 | agent_runtime_db | Agent execution runtime |

### Observability Stack

| Service | Port | Purpose |
|---------|------|---------|
| Prometheus | 9090 | Metrics collection |
| Grafana | 3001 | Dashboards and alerting |
| Jaeger | 16686 | Distributed trace viewer |

---

## HEDIS Measure Coverage

### Production Calculators (6)

| Measure ID | Full Name | Sub-Measures | Domain |
|------------|-----------|-------------|--------|
| **CDC** | Comprehensive Diabetes Care | HbA1c Testing, HbA1c <8% (good control), HbA1c >9% (poor control), Eye Exam, Nephropathy Screening, BP Control <140/90 | Chronic Disease |
| **BCS** | Breast Cancer Screening | — | Prevention |
| **CCS** | Cervical Cancer Screening | — | Prevention |
| **COL** | Colorectal Cancer Screening | — | Prevention |
| **CBP** | Controlling High Blood Pressure | — | Chronic Disease |
| **SPC** | Statin Therapy for Cardiovascular Disease | — | Treatment |

### Defined Measures (4 additional)

| Measure ID | Full Name | Domain |
|------------|-----------|--------|
| **EED** | Eye Exam for Patients With Diabetes | Chronic Disease Management |
| **AAB** | Avoidance of Antibiotic Treatment for Acute Bronchitis | Appropriate Use |
| **COU** | Risk of Continued Opioid Use | Behavioral Health |
| **FMC** | Follow-Up after Mental Health Encounter | Behavioral Health |

### CQL Library Support

HDIM's CQL engine can evaluate any CQL-based measure definition, including:
- NCQA HEDIS value sets
- Custom organization-specific measures
- AI-assisted measure generation from natural language descriptions

---

## Database Architecture

### Schema Organization

40 independent databases, one per service (database-per-service pattern):

```
PostgreSQL Instance (port 5435)
  ├── gateway_db          ─ Shared by 3 gateway services
  ├── fhir_db             ─ FHIR resource storage
  ├── cql_db              ─ CQL library storage, evaluation cache
  ├── quality_db          ─ Measure definitions, evaluation results
  ├── patient_db          ─ Patient demographics, health records
  ├── caregap_db          ─ Care gap state, interventions
  ├── event_db            ─ Event processing state
  ├── event_store_db      ─ Immutable event log (CQRS)
  ├── event_router_db     ─ Event routing configuration
  ├── audit_db            ─ Compliance audit trail
  ├── hcc_db              ─ HCC risk adjustment
  ├── consent_db          ─ Patient consent records
  ├── ai_assistant_db     ─ AI conversation history
  ├── analytics_db        ─ Population analytics
  ├── predictive_db       ─ ML model results
  ├── sdoh_db             ─ Social determinants data
  ├── enrichment_db       ─ Data enrichment state
  ├── agent_db            ─ Agent configurations
  ├── agent_runtime_db    ─ Agent execution state
  ├── agent_validation_db ─ Agent test results
  ├── approval_db         ─ Approval workflow state
  ├── payer_db            ─ Payer-specific data
  ├── cdr_db              ─ Clinical data repository
  ├── ehr_connector_db    ─ EHR connection config
  ├── ecr_db              ─ Electronic case reports
  ├── prior_auth_db       ─ Prior authorization state
  ├── migration_db        ─ Migration workflow state
  ├── qrda_db             ─ QRDA report storage
  ├── notification_db     ─ Notification state
  ├── patient_event_db    ─ Patient event stream
  ├── care_gap_event_db   ─ Care gap event stream
  ├── quality_event_db    ─ Quality measure event stream
  ├── clinical_workflow_event_db ─ Clinical workflow events
  ├── data_ingestion_db   ─ Ingestion pipeline state
  ├── docs_db             ─ Documentation metadata
  ├── sales_automation_db ─ Sales pipeline data
  ├── investor_dashboard_db ─ Investor metrics
  ├── healthdata_demo     ─ Demo data
  └── ...
```

### Migration Strategy

- **Tool:** Liquibase (never Flyway)
- **Validation:** Hibernate `ddl-auto: validate` (never `create` or `update`)
- **Rollback coverage:** 100% (199/199 changesets)
- **Enforcement:** CI/CD entity-migration validation tests on every PR

---

## Event Architecture

### Kafka Topic Inventory (54 topics)

**Patient Domain (4):**
`patient.created` · `patient.updated` · `patient.merged` · `patient.status.changed`

**FHIR Domain (7):**
`fhir.patient.created` · `fhir.patient.updated` · `fhir.patient.linked` · `fhir.observations.created` · `fhir.conditions.created` · `fhir.conditions.updated` · `fhir.procedures.created`

**Clinical Domain (4):**
`clinical.observation.created` · `clinical.procedure.created` · `clinical.condition.created` · `clinical.medicationadministration.created`

**Care Gap Domain (8):**
`care-gap.identified` · `care-gap.closed` · `care-gap.auto-closed` · `care-gap.addressed` · `care-gap.assigned` · `care-gap.waived` · `care-gap.due-date-updated` · `care-gap.priority.changed`

**Quality Measure Domain (7+):**
`measure.evaluated` · `measure.compliance.changed` · `measure.score.updated` · `measure.numerator.updated` · `measure.denominator.updated` · `measure.exclusion.updated` · `measure-calculated`

**Health & Risk (4):**
`health-score.updated` · `health-score.significant-change` · `risk-assessment.updated` · `chronic-disease.deterioration`

**Clinical Alerts (3):**
`clinical-alert.triggered` · `clinical-alert.resolved` · `clinical-alerts`

**Mental Health (2):**
`mental-health-assessment.submitted` · `mental-health.updated`

**Workflow Domain (8):**
`workflow.started` · `workflow.completed` · `workflow.cancelled` · `workflow.assigned` · `workflow.reassigned` · `workflow.progress.updated` · `workflow.review.required` · `workflow.blocking.issue`

**Infrastructure (4+):**
`audit-events` · `approval-events` · `care-gap-events` · `agent-config-events` · visualization topics

---

## Clinical Portal Routes

### Public Routes (7)

| Route | Purpose |
|-------|---------|
| `/login` | Authentication |
| `/register` | User registration |
| `/unauthorized` | Access denied page |
| `/compliance` | Compliance information |
| `/testing` | QA test interface |
| `/demo-startup` | Demo initialization |
| `/event-processing` | Event processing monitor |

### Clinical Workflow Routes (15)

| Route | Purpose |
|-------|---------|
| `/dashboard` | Home dashboard (default) |
| `/patients` | Patient list / search |
| `/patients/:id` | Patient detail view |
| `/care-gaps` | Care gap management |
| `/care-recommendations` | AI care recommendations |
| `/pre-visit` | Pre-visit planning |
| `/patient-health` | Patient health overview |
| `/risk-stratification` | Population risk stratification |
| `/outreach-campaigns` | Outreach campaign management |
| `/evaluations` | CQL evaluation runs |
| `/results` | Evaluation results |
| `/quality-measures` | Measure library browser |
| `/quality-measures/:id` | Measure detail view |
| `/measure-comparison` | Side-by-side measure comparison |
| `/insights` | AI-powered clinical insights |

### Analytics & Reporting Routes (6)

| Route | Purpose |
|-------|---------|
| `/reports` | Report library |
| `/report-builder` | Custom report builder |
| `/report-builder/:id` | Edit existing report |
| `/qa-audit-dashboard` | QA audit dashboard |
| `/clinical-audit-dashboard` | Clinical audit dashboard |
| `/mpi-audit-dashboard` | MPI audit dashboard |

### AI & Builder Routes (4)

| Route | Purpose |
|-------|---------|
| `/ai-assistant` | AI clinical assistant |
| `/measure-builder` | Visual measure builder |
| `/measure-builder/new` | Create new measure |
| `/agent-builder` | AI agent builder |

### Visualization Routes (4)

| Route | Purpose |
|-------|---------|
| `/visualization/live-monitor` | Real-time system monitor |
| `/visualization/quality-constellation` | Quality measure visualization |
| `/visualization/flow-network` | Data flow visualization |
| `/visualization/measure-matrix` | Measure performance matrix |

### Admin Routes (4)

| Route | Purpose |
|-------|---------|
| `/admin/users` | User management |
| `/admin/tenant-settings` | Tenant configuration |
| `/admin/audit-logs` | Audit log viewer |
| `/admin/demo-seeding` | Demo data management |

---

## FHIR R4 Conformance

### Supported Resources (20+)

| Resource | CRUD | Search | Custom Operations |
|----------|------|--------|------------------|
| Patient | ✅ | ✅ name, birthdate, identifier | $everything, $match |
| Observation | ✅ | ✅ code, patient, date | — |
| Condition | ✅ | ✅ code, patient, onset | — |
| Procedure | ✅ | ✅ code, patient, date | — |
| MedicationRequest | ✅ | ✅ patient, medication | — |
| MedicationAdministration | ✅ | ✅ patient, medication | — |
| Immunization | ✅ | ✅ patient, vaccine-code | — |
| Encounter | ✅ | ✅ patient, date, type | — |
| Coverage | ✅ | ✅ patient, plan | — |
| AllergyIntolerance | ✅ | ✅ patient, code | — |
| DiagnosticReport | ✅ | ✅ patient, code | — |
| CarePlan | ✅ | ✅ patient, status | — |
| Goal | ✅ | ✅ patient, status | — |
| Task | ✅ | ✅ patient, status | — |
| Appointment | ✅ | ✅ patient, date | — |
| DocumentReference | ✅ | ✅ patient, type | — |
| Practitioner | ✅ | ✅ name, identifier | — |
| PractitionerRole | ✅ | ✅ practitioner, specialty | — |
| Organization | ✅ | ✅ name, identifier | — |

### FHIR Operations

| Operation | Endpoint | Purpose |
|-----------|----------|---------|
| `$export` | `GET /fhir/$export` | Bulk Data Export (NDJSON) |
| `metadata` | `GET /fhir/metadata` | CapabilityStatement |
| SMART Config | `GET /fhir/.well-known/smart-configuration` | SMART on FHIR discovery |

### Supported Code Systems

| System | OID | Use |
|--------|-----|-----|
| ICD-10-CM | 2.16.840.1.113883.6.90 | Diagnoses |
| CPT | 2.16.840.1.113883.6.12 | Procedures |
| LOINC | 2.16.840.1.113883.6.1 | Lab observations |
| RxNorm | 2.16.840.1.113883.6.88 | Medications |
| SNOMED CT | 2.16.840.1.113883.6.96 | Clinical terms |
| NDC | 2.16.840.1.113883.6.69 | Drug codes |
| HCPCS | 2.16.840.1.113883.6.285 | Healthcare services |
| CVX | 2.16.840.1.113883.12.292 | Vaccines |

---

## Technology Stack

### Backend

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Java | 21 (LTS) |
| Framework | Spring Boot | 3.x |
| Build | Gradle | 8.11+ (Kotlin DSL) |
| FHIR | HAPI FHIR | 7.x |
| ORM | Hibernate / JPA | 6.x |
| Migrations | Liquibase | Latest |
| Security | Spring Security | 6.x |
| API Docs | SpringDoc OpenAPI | 3.0 |
| Testing | JUnit 5, Mockito, Spring Test | Latest |

### Frontend

| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | Angular | 17+ |
| Language | TypeScript | 5.x |
| State | RxJS | 7.x |
| UI Components | Angular Material | 17+ |
| Build | Nx Workspace | Latest |
| Charts | Chart.js / D3.js | — |
| Testing | Jest (unit), Playwright (E2E) | Latest |

### Infrastructure

| Component | Technology | Version |
|-----------|-----------|---------|
| Database | PostgreSQL | 16 |
| Cache | Redis | 7 |
| Message Broker | Apache Kafka | KRaft mode |
| Containers | Docker | Latest |
| Orchestration | Docker Compose / Kubernetes | — |
| Reverse Proxy | nginx | Latest |
| Tracing | OpenTelemetry → Jaeger | Latest |
| Metrics | Prometheus → Grafana | Latest |
| Secrets | HashiCorp Vault | — |
| CI/CD | GitHub Actions | — |

---

## Performance Characteristics

| Metric | Specification |
|--------|---------------|
| **Single patient CQL evaluation** | <2 seconds |
| **Batch evaluation throughput** | Parallelized across Kafka consumers |
| **FHIR resource read** | <100ms (cached), <500ms (uncached) |
| **Care gap identification** | Real-time on data ingest events |
| **Portal page load** | <2 seconds (SPA, lazy-loaded modules) |
| **API response P95** | <500ms for standard CRUD |
| **Event propagation** | <1 second (Kafka consumer lag) |
| **Concurrent users** | Scales horizontally via Kubernetes |

---

## Deployment Requirements

### Minimum (Demo / Evaluation)

| Resource | Specification |
|----------|---------------|
| **CPU** | 4 cores |
| **RAM** | 12 GB |
| **Disk** | 50 GB SSD |
| **OS** | Linux (Ubuntu 22.04+, RHEL 8+) |
| **Docker** | 24.0+ with Compose v2 |
| **Network** | Outbound HTTPS (for dependency resolution) |

### Production (Small — up to 100K members)

| Resource | Specification |
|----------|---------------|
| **CPU** | 16 cores |
| **RAM** | 64 GB |
| **Disk** | 500 GB SSD |
| **Database** | PostgreSQL 16 (dedicated or managed) |
| **Cache** | Redis cluster (3 nodes minimum) |
| **Kafka** | 3-broker cluster (KRaft) |
| **Network** | Load balancer, TLS termination |

### Production (Large — 500K+ members)

| Resource | Specification |
|----------|---------------|
| **Compute** | Kubernetes cluster (8+ nodes) |
| **RAM** | 256 GB+ (distributed) |
| **Disk** | 2 TB+ SSD (NVMe preferred) |
| **Database** | PostgreSQL cluster with read replicas |
| **Cache** | Redis Sentinel or Cluster (6+ nodes) |
| **Kafka** | 5+ broker cluster with rack awareness |
| **Storage** | Object storage for QRDA exports, documents |

---

*HealthData-in-Motion | https://healthdatainmotion.com | February 2026*
