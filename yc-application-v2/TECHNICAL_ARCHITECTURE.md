# HDIM Technical Architecture

> Deep-dive into the technical foundation of Health Data In Motion.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Core Design Principles](#core-design-principles)
3. [Service Architecture](#service-architecture)
4. [FHIR R4 Implementation](#fhir-r4-implementation)
5. [CQL Engine](#cql-engine)
6. [Data Flow](#data-flow)
7. [Integration Patterns](#integration-patterns)
8. [Infrastructure](#infrastructure)
9. [Security Architecture](#security-architecture)
10. [Performance & Scalability](#performance--scalability)
11. [API Design](#api-design)
12. [Monitoring & Observability](#monitoring--observability)

---

## Architecture Overview

HDIM is built as a cloud-native, microservices-based platform designed for real-time healthcare quality measurement.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              EXTERNAL SYSTEMS                               │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐          │
│  │  Epic   │  │ Cerner  │  │ athena  │  │  n8n    │  │   CSV   │          │
│  │  FHIR   │  │  FHIR   │  │  FHIR   │  │Workflows│  │ Upload  │          │
│  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘          │
│       │            │            │            │            │               │
└───────┼────────────┼────────────┼────────────┼────────────┼───────────────┘
        │            │            │            │            │
        └────────────┴────────────┼────────────┴────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            API GATEWAY LAYER                                │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      Gateway Service                                 │   │
│  │  • OAuth 2.0 / JWT Authentication                                   │   │
│  │  • Rate Limiting & Throttling                                       │   │
│  │  • Request Routing                                                   │   │
│  │  • TLS Termination                                                   │   │
│  │  • API Versioning                                                    │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           CORE SERVICES LAYER                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐       │
│  │    FHIR     │  │     CQL     │  │   Quality   │  │   Patient   │       │
│  │   Service   │  │   Engine    │  │   Measure   │  │   Service   │       │
│  │             │  │   Service   │  │   Service   │  │             │       │
│  │ • Patient   │  │             │  │             │  │ • Timeline  │       │
│  │ • Condition │  │ • 61 HEDIS  │  │ • Dashboard │  │ • Health    │       │
│  │ • Obs/Labs  │  │   Measures  │  │ • Alerts    │  │   Status    │       │
│  │ • Meds      │  │ • Real-time │  │ • Reports   │  │ • Risk      │       │
│  │ • Encounter │  │   Eval      │  │ • Trends    │  │   Score     │       │
│  │ • Immunize  │  │ • <200ms    │  │             │  │             │       │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘       │
│         │                │                │                │               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐       │
│  │  Care Gap   │  │   Event     │  │   EHR       │  │  Consent    │       │
│  │   Service   │  │   Router    │  │  Connector  │  │   Service   │       │
│  │             │  │   Service   │  │   Service   │  │             │       │
│  │ • Gap ID    │  │             │  │             │  │ • Patient   │       │
│  │ • Worklists │  │ • Kafka     │  │ • SMART on  │  │   Consent   │       │
│  │ • Closure   │  │ • Routing   │  │   FHIR      │  │ • Org       │       │
│  │   Tracking  │  │ • DLQ       │  │ • OAuth     │  │   Policies  │       │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        ANALYTICS & AI LAYER                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐       │
│  │  Analytics  │  │ Predictive  │  │     AI      │  │    SDOH     │       │
│  │   Service   │  │  Analytics  │  │  Assistant  │  │   Service   │       │
│  │             │  │   Service   │  │   Service   │  │             │       │
│  │ • Reporting │  │ • Risk      │  │ • NLP       │  │ • Social    │       │
│  │ • Trends    │  │   Models    │  │ • Query     │  │   Factors   │       │
│  │ • Exports   │  │ • ML        │  │ • Insights  │  │ • Z-codes   │       │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           DATA LAYER                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────────────┐  ┌──────────────────────┐  ┌─────────────────┐  │
│  │     PostgreSQL       │  │       Kafka          │  │      Redis      │  │
│  │                      │  │                      │  │                 │  │
│  │  • FHIR Resources    │  │  • Event Streaming   │  │  • Session      │  │
│  │  • Quality Results   │  │  • Async Processing  │  │  • Cache        │  │
│  │  • Audit Logs        │  │  • Dead Letter Queue │  │  • Rate Limit   │  │
│  │  • Tenant Data       │  │                      │  │                 │  │
│  └──────────────────────┘  └──────────────────────┘  └─────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Core Design Principles

### 1. FHIR-Native Architecture

Unlike legacy systems that transform data into proprietary models, HDIM stores and processes data in native FHIR R4 format:

```
Traditional Approach:
EHR → ETL → Proprietary Model → Analytics → Reports
     (hours)    (complex)       (batch)    (stale)

HDIM Approach:
EHR → FHIR API → FHIR Storage → CQL Engine → Real-time Results
     (direct)    (standard)     (<200ms)     (current)
```

**Benefits:**
- No data model translation errors
- Standard terminology (SNOMED, LOINC, RxNorm)
- Direct CQL execution against FHIR resources
- Future-proof as FHIR adoption grows

### 2. Real-Time by Default

Every component is designed for real-time operation:

| Component | Latency Target | Actual |
|-----------|----------------|--------|
| FHIR API response | <100ms | 45ms avg |
| CQL measure evaluation | <200ms | 142ms avg |
| Care gap identification | <500ms | 280ms avg |
| Dashboard refresh | <1s | 650ms avg |
| Webhook delivery | <5s | 2.1s avg |

### 3. Multi-Tenant Isolation

Complete data isolation with tenant-aware architecture:

```java
// Every database query includes tenant context
@Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId")
List<Patient> findByTenantId(@Param("tenantId") String tenantId);

// Tenant extracted from JWT and propagated through request context
@Component
public class TenantAccessInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, ...) {
        String tenantId = extractTenantFromJwt(request);
        TenantContext.setCurrentTenant(tenantId);
        return true;
    }
}
```

### 4. Event-Driven Processing

Asynchronous event processing for scalability:

```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│  Incoming   │─────►│   Kafka     │─────►│  Consumer   │
│  FHIR Data  │      │   Topic     │      │  Services   │
└─────────────┘      └─────────────┘      └──────┬──────┘
                                                  │
                     ┌────────────────────────────┼────────────────────────────┐
                     │                            │                            │
              ┌──────▼──────┐            ┌────────▼────────┐          ┌───────▼───────┐
              │    CQL      │            │   Care Gap      │          │    Alert      │
              │  Evaluation │            │   Detection     │          │   Generation  │
              └─────────────┘            └─────────────────┘          └───────────────┘
```

---

## Service Architecture

### Microservices Inventory

| Service | Purpose | Tech Stack | Port |
|---------|---------|------------|------|
| **gateway-service** | API gateway, auth, routing | Spring Cloud Gateway | 8080 |
| **fhir-service** | FHIR resource CRUD | Spring Boot, HAPI FHIR | 8081 |
| **cql-engine-service** | CQL evaluation, measures | Spring Boot, CQL Engine | 8082 |
| **quality-measure-service** | Dashboards, reporting | Spring Boot, WebSocket | 8083 |
| **patient-service** | Patient aggregation | Spring Boot, Feign | 8084 |
| **care-gap-service** | Gap identification | Spring Boot | 8085 |
| **event-router-service** | Event routing, DLQ | Spring Boot, Kafka | 8086 |
| **consent-service** | Consent management | Spring Boot | 8087 |
| **ehr-connector-service** | EHR integrations | Spring Boot, OAuth | 8088 |
| **analytics-service** | Reporting, exports | Spring Boot | 8089 |
| **predictive-analytics-service** | ML models, risk scores | Python, FastAPI | 8090 |
| **ai-assistant-service** | NLP, query interface | Python, LangChain | 8091 |
| **sdoh-service** | SDOH tracking | Spring Boot | 8092 |

### Service Communication

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Communication Patterns                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  SYNCHRONOUS (REST/HTTP)                                           │
│  ├── Service-to-service via Feign clients                         │
│  ├── API Gateway routing                                            │
│  └── Health checks and discovery                                    │
│                                                                     │
│  ASYNCHRONOUS (Kafka)                                              │
│  ├── FHIR resource events (create/update/delete)                   │
│  ├── CQL evaluation events                                          │
│  ├── Care gap closure events                                        │
│  ├── Audit events                                                   │
│  └── Alert/notification events                                      │
│                                                                     │
│  REAL-TIME (WebSocket)                                             │
│  ├── Dashboard live updates                                         │
│  ├── Evaluation progress                                            │
│  └── Alert notifications                                            │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## FHIR R4 Implementation

### Supported Resources

| Resource | Operations | Use Case |
|----------|------------|----------|
| **Patient** | CRUD, Search, $match | Demographics, identification |
| **Condition** | CRUD, Search | Diagnoses, problem list |
| **Observation** | CRUD, Search | Labs, vitals, assessments |
| **MedicationRequest** | CRUD, Search | Prescriptions |
| **Immunization** | CRUD, Search | Vaccine records |
| **Encounter** | CRUD, Search | Visits, admissions |
| **Procedure** | CRUD, Search | Procedures performed |
| **AllergyIntolerance** | CRUD, Search | Allergies |
| **DiagnosticReport** | CRUD, Search | Lab reports |
| **Consent** | CRUD, Search | Patient consent |

### FHIR Service Architecture

```java
@RestController
@RequestMapping("/fhir/r4")
public class PatientController {

    @GetMapping("/Patient/{id}")
    public Patient read(@PathVariable String id) {
        return patientService.findById(id);
    }

    @GetMapping("/Patient")
    public Bundle search(
        @RequestParam(required = false) String family,
        @RequestParam(required = false) String given,
        @RequestParam(required = false) String birthdate,
        @RequestParam(required = false) String identifier
    ) {
        return patientService.search(family, given, birthdate, identifier);
    }

    @PostMapping("/Patient")
    public Patient create(@RequestBody Patient patient) {
        validatePatient(patient);
        Patient created = patientService.create(patient);
        eventProducer.send("fhir.patient.created", created);
        return created;
    }
}
```

### Data Model

FHIR resources stored in PostgreSQL with JSONB:

```sql
CREATE TABLE fhir_resources (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(100) NOT NULL,
    version INTEGER DEFAULT 1,
    resource JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    UNIQUE(tenant_id, resource_type, resource_id)
);

-- Indexes for common queries
CREATE INDEX idx_patient_identifier ON fhir_resources
    USING GIN ((resource->'identifier'));

CREATE INDEX idx_condition_code ON fhir_resources
    USING GIN ((resource->'code'->'coding'));

CREATE INDEX idx_observation_date ON fhir_resources
    ((resource->>'effectiveDateTime'));
```

---

## CQL Engine

### Overview

The CQL (Clinical Quality Language) engine is the core of HDIM's real-time quality measurement capability. It evaluates standardized clinical logic against patient data in under 200ms.

### Implemented Measures (61 Total)

| Category | Measures |
|----------|----------|
| **Diabetes** | CDC (HbA1c Control), EED (Eye Exam), NCS (Nephropathy), HBD (Blood Pressure in Diabetes) |
| **Cardiovascular** | CBP (Blood Pressure), SPC (Statin Therapy), PBH (Beta-Blocker), COA (Care for Older Adults) |
| **Cancer Screening** | BCS (Breast), CCS (Cervical), COL (Colorectal) |
| **Behavioral Health** | DSF (Depression Screening), DRR (Depression Remission), AMM (Antidepressant Mgmt), ADD (ADHD), FUA/FUH/FUM (Follow-up) |
| **Respiratory** | AMR (Asthma Medication Ratio), SPR (Spirometry), CWP (Appropriate Treatment) |
| **Immunization** | FVA (Flu Adult), IMA (Adolescent), CIS (Childhood) |
| **Preventive** | ABA (Adult BMI), WCC (Weight Child), LSC (Lead Screening), W15/W34 (Well-Child) |
| **Medication Safety** | MMA (Medication Adherence), OMW (Osteoporosis), PCE (Pharmacotherapy Opioid) |
| **Care Coordination** | MRP (Medication Reconciliation), PCR (Plan All-Cause Readmission) |

### Measure Implementation Pattern

```java
@Component
public class CDCMeasure extends AbstractHedisMeasure {

    @Override
    public String getMeasureId() {
        return "CDC";
    }

    @Override
    public String getMeasureName() {
        return "Comprehensive Diabetes Care";
    }

    @Override
    public MeasureResult evaluate(PatientData patient, MeasurementPeriod period) {
        // Initial Population: Age 18-75 with diabetes diagnosis
        if (!isInInitialPopulation(patient, period)) {
            return MeasureResult.notApplicable();
        }

        // Exclusions: Hospice, advanced illness, frailty
        if (hasExclusion(patient, period)) {
            return MeasureResult.excluded();
        }

        // Denominator: Patients with diabetes
        boolean inDenominator = hasDiabetesDiagnosis(patient, period);

        // Numerator: HbA1c < 8% (or < 9% for older adults)
        boolean inNumerator = false;
        if (inDenominator) {
            Observation latestA1c = getLatestHbA1c(patient, period);
            if (latestA1c != null) {
                double value = latestA1c.getValueQuantity().getValue().doubleValue();
                double threshold = patient.getAge() > 65 ? 9.0 : 8.0;
                inNumerator = value < threshold;
            }
        }

        return MeasureResult.builder()
            .measureId(getMeasureId())
            .patientId(patient.getId())
            .inDenominator(inDenominator)
            .inNumerator(inNumerator)
            .evaluatedAt(Instant.now())
            .build();
    }
}
```

### Real-Time Evaluation Flow

```
┌──────────────────────────────────────────────────────────────────────────┐
│                     CQL Evaluation Pipeline                              │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  1. REQUEST                                                              │
│  POST /cql/evaluate                                                      │
│  { "patientId": "12345", "measures": ["CDC", "CBP", "BCS"] }            │
│                                                                          │
│  2. DATA RETRIEVAL (Parallel)                            ~50ms          │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐           │
│  │ Patient │ │Condition│ │  Meds   │ │  Labs   │ │Encounter│           │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘           │
│                                                                          │
│  3. MEASURE EVALUATION (Parallel)                        ~80ms          │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐                                   │
│  │   CDC   │ │   CBP   │ │   BCS   │                                   │
│  │Diabetes │ │   BP    │ │ Breast  │                                   │
│  └─────────┘ └─────────┘ └─────────┘                                   │
│                                                                          │
│  4. RESULT AGGREGATION                                   ~10ms          │
│  {                                                                       │
│    "patientId": "12345",                                                │
│    "evaluatedAt": "2024-10-15T10:30:00Z",                              │
│    "measures": [                                                        │
│      { "id": "CDC", "status": "met", "value": 7.2 },                   │
│      { "id": "CBP", "status": "met", "value": "128/82" },              │
│      { "id": "BCS", "status": "gap", "lastScreening": null }           │
│    ],                                                                   │
│    "careGaps": ["BCS"],                                                │
│    "evaluationTimeMs": 142                                              │
│  }                                                                       │
│                                                                          │
│  TOTAL: ~142ms                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

### Value Set Management

```java
@Entity
@Table(name = "value_sets")
public class ValueSet {
    @Id
    private String oid;
    private String name;
    private String version;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private List<ValueSetConcept> concepts;

    // E.g., "2.16.840.1.113883.3.464.1003.103.12.1001" = Diabetes
    // Contains ICD-10 codes: E10.*, E11.*, E13.*, etc.
}

@Service
public class ValueSetService {

    @Cacheable("valueSets")
    public boolean containsCode(String valueSetOid, String system, String code) {
        ValueSet vs = valueSetRepository.findByOid(valueSetOid);
        return vs.getConcepts().stream()
            .anyMatch(c -> c.getSystem().equals(system) && c.getCode().equals(code));
    }
}
```

---

## Data Flow

### Inbound Data Processing

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        INBOUND DATA FLOW                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────┐                                                          │
│  │  EHR FHIR    │──────┐                                                   │
│  │   Endpoint   │      │                                                   │
│  └──────────────┘      │                                                   │
│                        │                                                    │
│  ┌──────────────┐      │    ┌─────────────────────────────────────────┐   │
│  │ n8n Workflow │──────┼───►│           EHR Connector Service         │   │
│  │   Output     │      │    │                                         │   │
│  └──────────────┘      │    │  • OAuth token management               │   │
│                        │    │  • Rate limiting                         │   │
│  ┌──────────────┐      │    │  • Retry with backoff                   │   │
│  │  CSV Upload  │──────┘    │  • Transform to FHIR                    │   │
│  │   Portal     │           └──────────────┬──────────────────────────┘   │
│  └──────────────┘                          │                               │
│                                            ▼                               │
│                           ┌────────────────────────────────┐              │
│                           │         FHIR Service           │              │
│                           │                                │              │
│                           │  • Validate against US Core    │              │
│                           │  • Patient matching            │              │
│                           │  • Deduplication               │              │
│                           │  • Persist to PostgreSQL       │              │
│                           └────────────────┬───────────────┘              │
│                                            │                               │
│                                            ▼                               │
│                           ┌────────────────────────────────┐              │
│                           │      Kafka: fhir.events        │              │
│                           │                                │              │
│                           │  { "type": "Patient.created",  │              │
│                           │    "resourceId": "12345",      │              │
│                           │    "tenantId": "acme-health" } │              │
│                           └────────────────┬───────────────┘              │
│                                            │                               │
│                    ┌───────────────────────┼───────────────────────┐      │
│                    │                       │                       │       │
│                    ▼                       ▼                       ▼       │
│           ┌──────────────┐       ┌──────────────┐       ┌──────────────┐ │
│           │ CQL Engine   │       │  Care Gap    │       │  Analytics   │ │
│           │  Consumer    │       │  Consumer    │       │  Consumer    │ │
│           │              │       │              │       │              │ │
│           │ Re-evaluate  │       │ Update gap   │       │ Update       │ │
│           │ measures     │       │ status       │       │ dashboards   │ │
│           └──────────────┘       └──────────────┘       └──────────────┘ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Real-Time Alert Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        REAL-TIME ALERT FLOW                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  TRIGGER: Lab result HbA1c = 10.2%                                         │
│                                                                             │
│  1. FHIR Service receives Observation                              [0ms]   │
│     └─► Kafka: fhir.observation.created                                    │
│                                                                             │
│  2. CQL Engine consumes event                                     [50ms]   │
│     └─► Evaluates CDC measure                                              │
│     └─► Result: HbA1c > 9% (poor control)                                  │
│     └─► Kafka: cql.evaluation.completed                                    │
│                                                                             │
│  3. Alert Evaluation Service consumes                            [100ms]   │
│     └─► Checks alert rules                                                 │
│     └─► Rule matched: "HbA1c > 9% for diabetic patient"                   │
│     └─► Kafka: alert.triggered                                             │
│                                                                             │
│  4. Notification Service consumes                                [150ms]   │
│     └─► Determines recipients (PCP, care coordinator)                      │
│     └─► Sends to configured channels:                                      │
│         • Epic In-Basket (via SMART on FHIR)                              │
│         • Email to care coordinator                                        │
│         • Dashboard WebSocket notification                                 │
│                                                                             │
│  TOTAL TIME: ~150ms from lab result to provider notification              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Integration Patterns

### SMART on FHIR (Epic, Cerner)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    SMART on FHIR Authorization Flow                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. LAUNCH                                                                 │
│     User clicks "Connect EHR" in HDIM                                      │
│     └─► HDIM redirects to EHR authorization endpoint                       │
│         GET https://ehr.example.com/oauth2/authorize                       │
│         ?response_type=code                                                │
│         &client_id=hdim-client                                             │
│         &redirect_uri=https://app.hdim.com/callback                        │
│         &scope=launch openid fhirUser patient/*.read                       │
│         &state=xyz123                                                      │
│                                                                             │
│  2. AUTHORIZATION                                                          │
│     User authenticates with EHR                                            │
│     User approves scopes                                                   │
│     └─► EHR redirects back with authorization code                         │
│         https://app.hdim.com/callback?code=abc789&state=xyz123            │
│                                                                             │
│  3. TOKEN EXCHANGE                                                         │
│     HDIM exchanges code for tokens                                         │
│     POST https://ehr.example.com/oauth2/token                              │
│     └─► Returns: access_token, refresh_token, patient context             │
│                                                                             │
│  4. DATA ACCESS                                                            │
│     HDIM fetches FHIR resources                                            │
│     GET https://ehr.example.com/fhir/r4/Patient/123                        │
│     Authorization: Bearer {access_token}                                   │
│                                                                             │
│  5. TOKEN REFRESH                                                          │
│     Before expiration, HDIM refreshes                                      │
│     POST https://ehr.example.com/oauth2/token                              │
│     grant_type=refresh_token                                               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### n8n Workflow Integration

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    n8n Workflow Architecture                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        n8n Instance                                  │   │
│  │                   (HDIM-managed or on-prem)                          │   │
│  │                                                                      │   │
│  │  ┌──────────────────────────────────────────────────────────────┐   │   │
│  │  │                    Workflow: Lab Import                       │   │   │
│  │  │                                                               │   │   │
│  │  │  ┌────────┐   ┌────────┐   ┌────────┐   ┌────────┐          │   │   │
│  │  │  │  SFTP  │──►│ Parse  │──►│Validate│──►│ FHIR   │          │   │   │
│  │  │  │Trigger │   │  CSV   │   │  Data  │   │Transform│         │   │   │
│  │  │  └────────┘   └────────┘   └────────┘   └───┬────┘          │   │   │
│  │  │                                             │                │   │   │
│  │  │                                    ┌────────▼────────┐       │   │   │
│  │  │                                    │  HDIM FHIR API  │       │   │   │
│  │  │                                    │  POST /fhir/r4  │       │   │   │
│  │  │                                    └─────────────────┘       │   │   │
│  │  └──────────────────────────────────────────────────────────────┘   │   │
│  │                                                                      │   │
│  │  Credentials stored securely:                                        │   │
│  │  • SFTP: Encrypted in n8n vault                                      │   │
│  │  • HDIM API: OAuth client credentials                                │   │
│  │                                                                      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  Monitoring:                                                               │
│  • Execution logs stored for 30 days                                       │
│  • Failure alerts via Slack/Email                                          │
│  • Retry with exponential backoff                                          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Infrastructure

### Deployment Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    KUBERNETES DEPLOYMENT                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐ │
│  │                         Kubernetes Cluster                            │ │
│  │                                                                       │ │
│  │  NAMESPACE: hdim-production                                          │ │
│  │                                                                       │ │
│  │  ┌─────────────────────────────────────────────────────────────────┐ │ │
│  │  │  INGRESS (nginx)                                                │ │ │
│  │  │  • TLS termination (Let's Encrypt)                              │ │ │
│  │  │  • Rate limiting                                                │ │ │
│  │  │  • WAF rules                                                    │ │ │
│  │  └───────────────────────────────┬─────────────────────────────────┘ │ │
│  │                                  │                                    │ │
│  │  ┌─────────────────────────────────────────────────────────────────┐ │ │
│  │  │  SERVICES (Deployments)                                         │ │ │
│  │  │                                                                 │ │ │
│  │  │  gateway-service (3 replicas)                                   │ │ │
│  │  │  fhir-service (3 replicas)                                      │ │ │
│  │  │  cql-engine-service (5 replicas)  ← Auto-scaled based on load  │ │ │
│  │  │  quality-measure-service (2 replicas)                           │ │ │
│  │  │  care-gap-service (2 replicas)                                  │ │ │
│  │  │  event-router-service (3 replicas)                              │ │ │
│  │  │  ...                                                            │ │ │
│  │  └─────────────────────────────────────────────────────────────────┘ │ │
│  │                                                                       │ │
│  │  ┌─────────────────────────────────────────────────────────────────┐ │ │
│  │  │  STATEFUL SERVICES                                              │ │ │
│  │  │                                                                 │ │ │
│  │  │  PostgreSQL (StatefulSet, 3 replicas HA)                        │ │ │
│  │  │  Kafka (StatefulSet, 3 brokers)                                 │ │ │
│  │  │  Redis (StatefulSet, 3 replicas sentinel)                       │ │ │
│  │  └─────────────────────────────────────────────────────────────────┘ │ │
│  │                                                                       │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
│  CLOUD SERVICES:                                                           │
│  • AWS S3 / Azure Blob: Document storage, backups                          │
│  • AWS KMS / Azure Key Vault: Encryption keys                              │
│  • AWS CloudWatch / Azure Monitor: Logging, metrics                        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Resource Specifications

| Service | CPU Request | CPU Limit | Memory Request | Memory Limit |
|---------|-------------|-----------|----------------|--------------|
| gateway-service | 250m | 1000m | 512Mi | 1Gi |
| fhir-service | 500m | 2000m | 1Gi | 4Gi |
| cql-engine-service | 1000m | 4000m | 2Gi | 8Gi |
| quality-measure-service | 500m | 2000m | 1Gi | 4Gi |
| PostgreSQL | 2000m | 4000m | 8Gi | 16Gi |
| Kafka (per broker) | 1000m | 2000m | 4Gi | 8Gi |

---

## Security Architecture

### Authentication & Authorization

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    AUTHENTICATION FLOW                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. User authenticates via OAuth 2.0 / OIDC                                │
│     └─► Identity Provider (Auth0, Azure AD, Okta)                          │
│                                                                             │
│  2. IdP issues JWT with claims                                             │
│     {                                                                       │
│       "sub": "user-123",                                                   │
│       "tenant_id": "acme-health",                                          │
│       "roles": ["quality_admin", "viewer"],                                │
│       "org_units": ["clinic-a", "clinic-b"],                               │
│       "exp": 1729012800                                                    │
│     }                                                                       │
│                                                                             │
│  3. Gateway validates JWT                                                   │
│     └─► Verify signature against IdP public keys                           │
│     └─► Check expiration                                                   │
│     └─► Extract tenant context                                             │
│                                                                             │
│  4. RBAC enforcement at service level                                       │
│     @PreAuthorize("hasRole('QUALITY_ADMIN')")                              │
│     public void updateMeasure(...) { ... }                                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Encryption

| Layer | Method | Key Management |
|-------|--------|----------------|
| **Data at Rest** | AES-256-GCM | AWS KMS / Azure Key Vault |
| **Data in Transit** | TLS 1.3 | Let's Encrypt / ACM |
| **Database** | PostgreSQL TDE | Customer-managed keys (enterprise) |
| **Backups** | AES-256 | Separate backup keys |
| **PHI Fields** | Application-level encryption | Per-tenant keys |

### Audit Logging

```java
@Aspect
@Component
public class DataAccessAuditAspect {

    @Around("@annotation(AuditDataAccess)")
    public Object auditDataAccess(ProceedingJoinPoint joinPoint) {
        AuditEvent event = AuditEvent.builder()
            .timestamp(Instant.now())
            .userId(SecurityContext.getCurrentUser())
            .tenantId(TenantContext.getCurrentTenant())
            .action(joinPoint.getSignature().getName())
            .resourceType(extractResourceType(joinPoint))
            .resourceId(extractResourceId(joinPoint))
            .ipAddress(RequestContext.getClientIp())
            .userAgent(RequestContext.getUserAgent())
            .build();

        try {
            Object result = joinPoint.proceed();
            event.setStatus("SUCCESS");
            return result;
        } catch (Exception e) {
            event.setStatus("FAILURE");
            event.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            auditEventProducer.send("audit.data-access", event);
        }
    }
}
```

---

## Performance & Scalability

### Benchmarks

| Operation | P50 | P95 | P99 | Target |
|-----------|-----|-----|-----|--------|
| FHIR Patient read | 12ms | 28ms | 45ms | <100ms |
| FHIR Bundle (100 resources) | 85ms | 180ms | 320ms | <500ms |
| CQL single measure eval | 45ms | 95ms | 142ms | <200ms |
| CQL population eval (1000 pts) | 2.1s | 3.8s | 5.2s | <10s |
| Dashboard load | 180ms | 420ms | 650ms | <1s |
| Real-time alert delivery | 95ms | 180ms | 280ms | <500ms |

### Horizontal Scaling

```yaml
# Kubernetes HPA for CQL Engine
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: cql-engine-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: cql-engine-service
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Pods
    pods:
      metric:
        name: cql_evaluation_queue_depth
      target:
        type: AverageValue
        averageValue: 100
```

### Caching Strategy

| Cache | TTL | Purpose |
|-------|-----|---------|
| Value Sets | 24 hours | Clinical code sets (rarely change) |
| Measure Templates | 1 hour | Compiled CQL (invalidated on update) |
| Patient Summary | 5 minutes | Aggregated patient view |
| Dashboard Data | 1 minute | Aggregated metrics |
| Session | 30 minutes | User sessions |

---

## API Design

### REST API Conventions

```
Base URL: https://api.healthdatainmotion.com/v1

Authentication: Bearer token (JWT)
Content-Type: application/json (or application/fhir+json for FHIR)

Standard Endpoints:
GET    /fhir/r4/{resourceType}             # Search
GET    /fhir/r4/{resourceType}/{id}        # Read
POST   /fhir/r4/{resourceType}             # Create
PUT    /fhir/r4/{resourceType}/{id}        # Update
DELETE /fhir/r4/{resourceType}/{id}        # Delete

POST   /cql/evaluate                        # Evaluate measures
GET    /quality/dashboard                   # Dashboard data
GET    /quality/measures                    # Measure definitions
GET    /care-gaps/{patientId}              # Patient care gaps
POST   /alerts/rules                        # Configure alerts
```

### API Response Format

```json
{
  "success": true,
  "data": {
    "patientId": "12345",
    "measures": [
      {
        "id": "CDC",
        "name": "Comprehensive Diabetes Care",
        "status": "met",
        "numerator": true,
        "denominator": true,
        "value": 7.2,
        "unit": "%",
        "evaluatedAt": "2024-10-15T10:30:00Z"
      }
    ]
  },
  "meta": {
    "requestId": "req-abc123",
    "processingTimeMs": 142
  }
}
```

### WebSocket API

```javascript
// Connect to real-time updates
const ws = new WebSocket('wss://api.hdim.com/ws/quality');

ws.onopen = () => {
  // Subscribe to patient updates
  ws.send(JSON.stringify({
    action: 'subscribe',
    channel: 'patient.12345.measures'
  }));
};

ws.onmessage = (event) => {
  const update = JSON.parse(event.data);
  // { type: 'measure.updated', data: { ... } }
  updateDashboard(update);
};
```

---

## Monitoring & Observability

### Metrics (Prometheus)

```
# Custom metrics exposed
hdim_cql_evaluation_duration_seconds{measure="CDC",status="success"}
hdim_fhir_request_duration_seconds{resource="Patient",operation="read"}
hdim_care_gaps_total{status="open",measure="BCS"}
hdim_kafka_consumer_lag{topic="fhir.events",consumer="cql-engine"}
hdim_active_websocket_connections{service="quality-measure"}
```

### Logging (Structured JSON)

```json
{
  "timestamp": "2024-10-15T10:30:00.000Z",
  "level": "INFO",
  "service": "cql-engine-service",
  "traceId": "abc123def456",
  "spanId": "789ghi",
  "tenantId": "acme-health",
  "userId": "user-123",
  "message": "CQL evaluation completed",
  "patientId": "12345",
  "measures": ["CDC", "CBP"],
  "durationMs": 142
}
```

### Distributed Tracing (OpenTelemetry)

```
Trace: abc123def456

├── gateway-service (12ms)
│   └── jwt-validation (2ms)
├── cql-engine-service (142ms)
│   ├── fhir-data-retrieval (48ms)
│   │   ├── Patient (8ms)
│   │   ├── Condition (12ms)
│   │   ├── Observation (15ms)
│   │   └── MedicationRequest (13ms)
│   └── measure-evaluation (94ms)
│       ├── CDC (32ms)
│       ├── CBP (28ms)
│       └── BCS (34ms)
└── response-serialization (8ms)

Total: 162ms
```

### Alerting Rules

| Alert | Condition | Severity | Action |
|-------|-----------|----------|--------|
| High Error Rate | >1% 5xx in 5 min | Critical | Page on-call |
| CQL Evaluation Slow | P95 > 500ms | Warning | Slack notification |
| Kafka Consumer Lag | >10,000 messages | Warning | Slack notification |
| Database Connection Pool | >80% utilized | Warning | Scale up |
| Memory Pressure | >85% for 10 min | Critical | Page on-call |

---

## Technology Stack Summary

| Layer | Technology | Version |
|-------|------------|---------|
| **Language** | Java | 17 LTS |
| **Framework** | Spring Boot | 3.2.x |
| **FHIR** | HAPI FHIR | 6.x |
| **CQL** | CQL Engine | 2.x |
| **Database** | PostgreSQL | 15 |
| **Message Queue** | Apache Kafka | 3.x |
| **Cache** | Redis | 7.x |
| **Container Runtime** | Docker | 24.x |
| **Orchestration** | Kubernetes | 1.28+ |
| **API Gateway** | Spring Cloud Gateway | 4.x |
| **Monitoring** | Prometheus + Grafana | Latest |
| **Logging** | ELK Stack / CloudWatch | Latest |
| **Tracing** | OpenTelemetry | 1.x |
| **CI/CD** | GitHub Actions | N/A |

---

*Technical Architecture Version: 1.0*
*Last Updated: December 2025*
