# HDIM Skills Roadmap & Capability Development Framework

**Last Updated:** January 20, 2026
**Version:** 1.0 - Foundation Release
**Audience:** Developers, Architects, QA Engineers, DevOps Engineers

---

## 📋 Quick Navigation

| Section | Purpose |
|---------|---------|
| [Executive Summary](#executive-summary) | High-level overview of 10 skill categories |
| [Skill Categories](#10-skill-categories) | Detailed breakdown of each skill area |
| [Capability Matrix](#capability-matrix) | Priority, complexity, and implementation status |
| [Learning Paths](#learning-paths) | 4-phase progression (8+ weeks) |
| [Quick Reference](#quick-reference-checklist) | Checklists and core concepts by role |
| [Getting Started](#getting-started) | How to begin learning |

---

## Executive Summary

HDIM (HealthData-in-Motion) is an **enterprise healthcare interoperability platform** requiring specialized capabilities across 10 interconnected skill domains:

- **Architecture & Design** - Microservices, CQRS/event sourcing, multi-gateway, distributed tracing
- **Healthcare Domain** - HEDIS measures, FHIR R4, CQL execution, care gaps, risk stratification
- **Security & Compliance** - HIPAA requirements, authentication, data protection, audit logging
- **Data Persistence** - PostgreSQL, Liquibase, JPA/Hibernate, Redis caching
- **Messaging** - Apache Kafka, event-driven patterns, async processing
- **Testing & QA** - Unit testing, integration testing, entity-migration validation, performance testing
- **Spring Boot & Java** - Spring Boot 3.x, Spring Data JPA, Spring Security, REST API design
- **Infrastructure** - Docker/Compose, Kubernetes, monitoring, observability
- **API Design** - OpenAPI/Swagger, API integration testing
- **Coding Standards** - HDIM patterns, code review, best practices

**Success Criteria:**
- ✅ 90%+ code review pass rate on first review
- ✅ <5 production issues per quarter from new code
- ✅ <2% HIPAA compliance violations in audit
- ✅ Entity-migration validation: 100% coverage
- ✅ Test coverage: >80% on all new code
- ✅ Onboarding time: New dev productive in <2 weeks

---

## 10 Skill Categories

### 1. ARCHITECTURE & DESIGN (🔴 CRITICAL)

**Why Critical:** Core patterns that define system behavior, scalability, and maintainability. All 51 microservices depend on these patterns.

#### 1.1 CQRS + Event Sourcing (🔴 HIGHEST PRIORITY)

**Purpose:** Immutable event log with command/query separation for complete audit trail and temporal queries.

**Key Concepts:**
- **Event Sourcing:** Store all state changes as immutable events
- **CQRS:** Separate read and write models for optimization
- **Event Store:** PostgreSQL table holding all events
- **Projections:** Denormalized read models updated by event handlers
- **Eventual Consistency:** Eventual correctness across services
- **Event Replay:** Reconstruct state by replaying events

**Implementation Examples in HDIM:**
- `patient-event-service` (8084) - Patient lifecycle events
- `quality-measure-event-service` (8087) - Measure evaluation events
- `care-gap-event-service` (8086) - Care gap detection events
- `clinical-workflow-event-service` - Workflow state events

**Learning Resources:**
- Full Guide: `docs/skills/01-architecture/cqrs-event-sourcing.md`
- Code Examples: `docs/skills/01-architecture/examples/cqrs-event-sourcing-examples.java`
- Diagrams: `docs/skills/01-architecture/diagrams/event-sourcing-flow.md`

**Estimated Learning Time:** 1 week

---

#### 1.2 Microservices Architecture (🔴 CRITICAL)

**Purpose:** 51 independent services with clear boundaries, API contracts, and resilience patterns.

**Key Concepts:**
- **Service Boundaries:** Domain-driven boundaries
- **API Contracts:** RESTful endpoints with versioning
- **Service-to-Service Communication:** Feign (sync), Kafka (async)
- **Resilience Patterns:** Circuit breakers, timeouts, retries
- **Service Discovery:** Dynamic service location
- **Bulkhead Isolation:** Failure containment

**Implementation Examples:**
- Patient Service → FHIR Service (Feign)
- Quality Measure Service → Kafka → Care Gap Service
- Notification Service → Multiple event topics

**Learning Resources:**
- Full Guide: `docs/skills/01-architecture/microservices-patterns.md`

**Estimated Learning Time:** 1 week

---

#### 1.3 Multi-Gateway Architecture (🔴 CRITICAL - NEW January 2026)

**Purpose:** Domain-specific gateways (admin, clinical, FHIR) with shared `gateway-core` module.

**Key Concepts:**
- **Gateway Trust Pattern:** Single JWT validation point
- **Header Injection:** Gateway injects trusted headers with HMAC signature
- **Tenant-Aware Routing:** Route requests to appropriate services
- **Rate Limiting:** Per-gateway and per-tenant throttling
- **Protocol Translation:** REST/gRPC gateway capabilities

**Gateway Services:**
- **gateway-service** (8001) - Legacy/core gateway
- **gateway-admin-service** (8002) - Admin operations, tenant config
- **gateway-clinical-service** (8003) - Clinical data, high volume (public API)
- **gateway-fhir-service** (8004) - FHIR R4-compliant resources

**Trust Boundary Model:**
```
┌─── UNTRUSTED (Internet) ───┐
│  Clients can forge headers  │
└───────────┬─────────────────┘
            │
            ▼
┌─── GATEWAY (Trust Boundary) ───┐
│ • Validates JWT                 │
│ • STRIPS all X-Auth-* headers   │
│ • Injects NEW headers + HMAC    │
└───────────┬─────────────────────┘
            │
            ▼
┌─── TRUSTED (Internal Network) ───┐
│ • Services trust headers          │
│ • Signed by gateway only          │
│ • Internal-only communication     │
└────────────────────────────────────┘
```

**Learning Resources:**
- Full Guide: `docs/skills/01-architecture/multi-gateway-architecture.md`

**Estimated Learning Time:** 1 week

---

#### 1.4 Multi-Tenant Architecture (🔴 CRITICAL)

**Purpose:** Complete isolation between 1000+ tenants at database, cache, and messaging layers.

**Key Concepts:**
- **Database-Level Filtering:** All queries filter by `tenantId`
- **Cache Isolation:** Keys namespaced `{tenantId}:{resourceType}:{id}`
- **Kafka Tenant Routing:** Tenant ID in event payload
- **Rate Limiting per Tenant:** Independent throttling
- **Row-Level Security:** Database-enforced access control

**Isolation Layers:**
```
1. DATABASE:     WHERE tenantId = :tenantId AND ...
2. CACHE:        redis key: "tenant-001:Patient:p123"
3. KAFKA:        event.tenantId = "tenant-001"
4. RATE LIMIT:   per-tenant bucket
5. RESPONSE:     Tenant header in response
```

**Learning Resources:**
- Full Guide: `docs/skills/01-architecture/multi-tenant-architecture.md`

**Estimated Learning Time:** 1 week

---

#### 1.5 Distributed Tracing & Observability (🟡 IMPORTANT)

**Purpose:** End-to-end request visibility across all 51 microservices.

**Key Concepts:**
- **OpenTelemetry:** Vendor-agnostic tracing standard
- **Trace Context Propagation:** W3C and B3 header formats
- **Span Creation:** Instrument key operations
- **Metrics Collection:** Latency, error rates, throughput
- **Service Dependency Mapping:** Visualize service interactions

**Implementation Examples:**
- HTTP requests: Automatic via Feign/RestTemplate
- Kafka: Producer/consumer span context preservation
- Custom spans: Manual instrumentation for business logic

**Learning Resources:**
- Full Guide: `docs/skills/01-architecture/distributed-tracing.md`
- Observability Guide: `backend/docs/DISTRIBUTED_TRACING_GUIDE.md`

**Estimated Learning Time:** 3 days

---

### 2. HEALTHCARE DOMAIN (🔴 CRITICAL)

**Why Critical:** HEDIS quality measures and FHIR standards are business-critical. Wrong implementation = wrong patient care assessment.

#### 2.1 HEDIS Quality Measure Evaluation (🔴 HIGHEST PRIORITY)

**Purpose:** Evaluate NCQA-compliant quality measures for patient population (56+ measures).

**What is HEDIS?**
- **HEDIS** = Healthcare Effectiveness Data and Information Set
- **NCQA** = National Committee for Quality Assurance (standards body)
- **Measures** = Standardized metrics for quality assessment
- **Denominator** = Patient population eligible for measure
- **Numerator** = Patients in denominator meeting measure criteria
- **Gap** = Patient in denominator but NOT numerator

**Key Measures in HDIM:**
| Measure | Code | Purpose | Example |
|---------|------|---------|---------|
| Breast Cancer Screening | BCS | Annual mammography screening | Women 40-74 with mammogram in past 2 years |
| Diabetes Care | HbA1c | Glycemic control | Diabetes patients with HbA1c < 8% |
| Hypertension Control | HTN | Blood pressure management | HTN patients with BP < 140/90 |
| Comprehensive Diabetes | CDC | Multi-indicator diabetes quality | HbA1c, LDL, microalbumin control |
| Medication Adherence | RAS/Beta/Statin | Drug compliance | 80%+ days covered for chronic meds |
| Immunizations | Pneumo/Zoster | Preventive care | Age-appropriate vaccine administration |

**Measure Evaluation Flow:**
```
1. Measure Definition
   ↓
2. CQL Library (NCQA spec)
   ↓
3. Load Patient FHIR Data
   ↓
4. Execute CQL Against Data
   ↓
5. Evaluate Denominator
   ├─ True → Continue to numerator
   └─ False → Patient not applicable
   ↓
6. Evaluate Numerator
   ├─ True → Patient "passes" measure
   └─ False → Patient has CARE GAP
   ↓
7. Store Result + Fire Events
```

**Learning Resources:**
- Full Guide: `docs/skills/02-healthcare-domain/hedis-quality-measures.md`
- Service Catalog: `docs/services/SERVICE_CATALOG.md` (Quality Measure Service)
- CQL Reference: `docs/skills/02-healthcare-domain/cql-execution.md`

**Estimated Learning Time:** 2 weeks

---

#### 2.2 CQL (Clinical Quality Language) Integration (🔴 CRITICAL)

**Purpose:** NCQA standard language for expressing quality measure logic.

**Key Concepts:**
- **CQL Syntax:** Clinical expression language
- **FHIR Data Binding:** CQL references FHIR resources
- **Value Sets:** Code groupings (ICD-10, SNOMED CT, etc.)
- **Libraries:** Reusable CQL logic
- **Temporal Operators:** Date/time calculations for measure periods
- **Logic Gates:** AND/OR/NOT combinations

**Example CQL for Diabetes HbA1c Control:**
```
define "Denominator":
  AgeInYearsAt(end of "Measurement Period") >= 18
    AND exists (
      [Condition: "Diabetes"] D
        where D.onset during "Measurement Period"
    )

define "Numerator":
  exists (
    [Observation: "HbA1c"] O
      where O.value < 8
        AND O.effective during "Measurement Period"
  )
```

**Learning Resources:**
- Full Guide: `docs/skills/02-healthcare-domain/cql-execution.md`
- CQL Engine Service: Port 8081
- Value Sets Management: Value set vocabulary management

**Estimated Learning Time:** 1.5 weeks

---

#### 2.3 FHIR R4 Integration (🔴 CRITICAL)

**Purpose:** FHIR (Fast Healthcare Interoperability Resources) standard for clinical data representation.

**What is FHIR?**
- **FHIR** = Fast Healthcare Interoperability Resources (pronounced "fire")
- **R4** = Release 4 (current stable version)
- **Standard Resources:** 100+ resource types defined
- **RESTful API:** Resource-oriented endpoints
- **Bundled Operations:** Batch create/update operations

**Key Resources in HDIM:**
| Resource | Purpose | Example |
|----------|---------|---------|
| Patient | Demographics | John Doe, DOB 1980-01-01, MRN #123 |
| Observation | Measurements | HbA1c 7.2, Blood pressure 120/80 |
| Condition | Diagnoses | Type 2 Diabetes, Hypertension |
| Medication | Pharmacy data | Metformin 500mg, Lisinopril 10mg |
| MedicationRequest | Prescriptions | Order for Metformin, start date, refills |
| Procedure | Clinical procedures | Knee surgery, endoscopy |
| Immunization | Vaccines | COVID-19 vaccine, pneumococcal vaccine |
| CarePlan | Care coordination | Diabetes management plan |
| Goal | Patient goals | Weight loss, HbA1c control |
| Appointment | Scheduling | Doctor visit Jan 15, 2024 |

**FHIR Search Examples:**
```
GET /fhir/Patient?name=John&birthdate=1980-01-01
GET /fhir/Observation?code=17855-8&date=gt2024-01-01
GET /fhir/Condition?patient=p123&code=E11
```

**Learning Resources:**
- Full Guide: `docs/skills/02-healthcare-domain/fhir-r4-integration.md`
- FHIR Service: Port 8085
- HAPI FHIR: www.hapifhir.io
- Official FHIR Spec: www.hl7.org/fhir

**Estimated Learning Time:** 2 weeks

---

#### 2.4 Care Gap Detection & Management (🔴 CRITICAL)

**Purpose:** Identify patients who should receive interventions to improve quality measure performance.

**What is a Care Gap?**
- **Definition:** Patient is in measure denominator but NOT numerator
- **Clinically:** Patient hasn't received recommended care (e.g., mammogram overdue)
- **Business:** Opportunity for provider intervention to close gap
- **Example:** Diabetic patient (denominator) without HbA1c test in past 2 years (numerator)

**Care Gap Lifecycle:**
```
1. Measure Evaluation Results
   ├─ Denominator = True
   └─ Numerator = False
        ↓
2. Care Gap Created
   ├─ Assigned to provider
   ├─ Risk-scored
   └─ Added to notification queue
        ↓
3. Provider Notification
   ├─ Email alert
   ├─ Portal display
   └─ EHR integration
        ↓
4. Patient Intervention
   ├─ Appointment scheduled
   ├─ Lab test ordered
   └─ Follow-up tracking
        ↓
5. Gap Closure
   ├─ Patient completes intervention
   ├─ Measure re-evaluated
   ├─ New data captured
   └─ Gap marked closed
```

**Care Gap Prioritization:**
- **Risk Scoring:** HCC risk, comorbidities, recent utilization
- **Clinical Impact:** Which gaps have greatest health effect
- **Volume:** How many patients in same gap
- **Provider Capacity:** What can provider handle

**Learning Resources:**
- Full Guide: `docs/skills/02-healthcare-domain/care-gap-detection.md`
- Care Gap Service: Port 8086
- Care Gap Event Service: Event-driven gap detection

**Estimated Learning Time:** 1.5 weeks

---

#### 2.5 Risk Stratification Models (🟡 IMPORTANT)

**Purpose:** Segment patient population by risk for targeted interventions and cost prediction.

**Risk Models Implemented:**
| Model | Purpose | Score Range | Use Case |
|-------|---------|-------------|----------|
| HCC (Hierarchical Condition Categories) | Risk adjustment | 0.5 - 4.0+ | Capitated payment adjustment |
| Charlson Comorbidity Index | Chronic disease burden | 0 - 40+ | Mortality prediction, LOS estimate |
| Elixhauser Comorbidity Index | Clinical severity | Variable | Readmission risk, cost prediction |
| Frailty Index | Geriatric risk | 0 - 1.0 | Falls risk, hospitalization |
| LACE Index | Hospital readmission | 0 - 19 | 30-day readmission prediction |

**Example HCC Scoring:**
```
Patient: 65M with Diabetes, HTN, CHF, CKD

HCC Categories:
- HCC 18: Diabetes w/ complications  → 0.25
- HCC 81: HTN w/ complications       → 0.19
- HCC 85: CHF                         → 0.41
- HCC 136: End-stage renal disease   → 1.30

Total Risk Score = 2.15 (moderate-high risk)
→ Recommend: Intensive case management
```

**Learning Resources:**
- Full Guide: `docs/skills/02-healthcare-domain/risk-stratification.md`
- Predictive Analytics Service: Risk calculation engines

**Estimated Learning Time:** 1 week

---

#### 2.6 EHR Integration & Data Standards (🟡 IMPORTANT)

**Purpose:** Ingest clinical data from hospital/outpatient EHR systems.

**Data Standards:**
- **HL7v2:** Legacy clinical message format (still widely used)
- **FHIR:** Modern FHIR API endpoints
- **CDA:** Clinical Document Architecture (XML clinical documents)
- **Value Set Mapping:** Convert legacy codes to standard codes

**EHR Integration Flow:**
```
EHR System (Epic, Cerner)
    ↓
HL7v2/FHIR API
    ↓
HDIM Data Ingestion Service
    ├─ Parse HL7v2 / FHIR JSON
    ├─ Map to standard terminologies
    ├─ Quality checks
    └─ Store to FHIR repository
    ↓
Quality Measure Engine
```

**Learning Resources:**
- Full Guide: `docs/skills/02-healthcare-domain/ehr-integration.md`
- EHR Connector Service: Data ingestion from external systems

**Estimated Learning Time:** 1 week

---

#### 2.7 SDOH (Social Determinants of Health) (🟡 IMPORTANT)

**Purpose:** Capture non-clinical factors affecting health outcomes.

**SDOH Domains:**
- **Housing Stability:** Homelessness, housing quality
- **Food Security:** Access to food, nutrition
- **Transportation:** Ability to reach care
- **Social Isolation:** Loneliness, community connection
- **Financial Strain:** Ability to afford treatment
- **Legal Assistance:** Need for legal services

**Learning Resources:**
- Full Guide: `docs/skills/02-healthcare-domain/sdoh-screening.md`

**Estimated Learning Time:** 3 days

---

### 3. SECURITY & COMPLIANCE (🔴 CRITICAL)

**Why Critical:** HIPAA violations carry $100k+ fines. PHI protection is non-negotiable.

#### 3.1 HIPAA Compliance (🔴 HIGHEST PRIORITY)

**Purpose:** Ensure all code complies with HIPAA Privacy and Security Rules.

**What is HIPAA?**
- **HIPAA** = Health Insurance Portability and Accountability Act
- **Privacy Rule:** Controls use/disclosure of PHI (Protected Health Information)
- **Security Rule:** Safeguards physical/technical/administrative
- **Breach Notification:** Mandatory if >500 people affected

**PHI Definition:**
"Any information in patient's medical record or health plan that can identify individual"
- Name, DOB, SSN, MRN, email, phone
- Medical diagnoses, treatment plans, test results
- Insurance information, payment history

**HIPAA Requirements in Code:**

**1. Cache Control for PHI (CRITICAL)**
```
Maximum TTL: 5 minutes for any PHI data
Must have Cache-Control header: no-store, no-cache, must-revalidate, private
Must set HTTP response headers preventing browser cache
```

**Example:**
```java
@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {
    @GetMapping("/{patientId}")
    public ResponseEntity<PatientResponse> getPatient(@PathVariable String patientId) {
        PatientResponse response = patientService.getPatient(patientId);

        // REQUIRED: Cache control headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cache-Control", "no-store, no-cache, must-revalidate, private");
        headers.set("Pragma", "no-cache");
        headers.set("Expires", "0");

        return ResponseEntity.ok().headers(headers).body(response);
    }
}
```

**2. Audit Logging of PHI Access (CRITICAL)**
```
Must log all access to PHI: who, when, what, why
Include successful and failed access attempts
Cannot be modified after creation (immutable audit log)
```

**Example:**
```java
@Service
public class PatientService {
    @Audited(eventType = "PHI_ACCESS")  // Automatic audit logging
    public PatientResponse getPatient(String patientId, String tenantId) {
        return patientRepository.findByIdAndTenant(patientId, tenantId)
            .map(this::mapToResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
    }
}
```

**3. Multi-Tenant Isolation (CRITICAL)**
```
Must ensure tenant A cannot access tenant B's data
All queries must filter by tenantId
Database-level enforcement required
```

**Example:**
```java
@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    @Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId AND p.id = :id")
    Optional<Patient> findByIdAndTenant(
        @Param("id") UUID id,
        @Param("tenantId") String tenantId
    );
}
```

**4. Encryption (CRITICAL)**
```
Data at rest: AES-256 encryption
Data in transit: TLS 1.3+
Key management: HashiCorp Vault
```

**HIPAA Compliance Checklist:**
- ✅ Cache TTL ≤ 5 minutes
- ✅ Cache-Control headers on PHI endpoints
- ✅ @Audited annotation on PHI access
- ✅ Multi-tenant filtering in queries
- ✅ @PreAuthorize on sensitive endpoints
- ✅ No PHI in log messages
- ✅ Encryption at rest & in transit
- ✅ Audit logs retained 7+ years
- ✅ Breach notification procedures

**Learning Resources:**
- Full Guide: `docs/skills/03-security-compliance/hipaa-compliance.md`
- HIPAA Compliance Guide: `backend/HIPAA-CACHE-COMPLIANCE.md`

**Estimated Learning Time:** 1.5 weeks

---

#### 3.2 Authentication & Authorization (🟡 IMPORTANT)

**Purpose:** Verify user identity (authentication) and permissions (authorization).

**Key Concepts:**
- **Authentication:** "Who are you?" - JWT validation
- **Authorization:** "What can you do?" - Role-based access control
- **JWT (JSON Web Token):** Signed token containing user claims
- **Roles:** SUPER_ADMIN, ADMIN, EVALUATOR, ANALYST, VIEWER
- **Scopes:** Tenant-specific permissions

**Authentication Flow:**
```
Client
  ↓
1. POST /auth/login with credentials
  ↓
2. Gateway validates JWT
  ↓
3. Gateway injects X-Auth-* headers
  ↓
4. Services trust headers (no re-validation)
```

**Role Hierarchy:**
```
SUPER_ADMIN
  ├─ ADMIN (tenant-level)
  │   ├─ EVALUATOR (run measures, view results)
  │   │   ├─ ANALYST (view reports)
  │   │   │   └─ VIEWER (read-only)
```

**Authorization Example:**
```java
@RestController
@RequestMapping("/api/v1/measures")
public class MeasureController {
    @PostMapping("/evaluate")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")  // Role check
    public ResponseEntity<EvaluationResult> evaluate(
        @Valid @RequestBody EvaluationRequest request,
        @RequestHeader("X-Tenant-ID") String tenantId) {
        // Only ADMIN or EVALUATOR can execute
        return ResponseEntity.ok(measureService.evaluate(request, tenantId));
    }
}
```

**Learning Resources:**
- Full Guide: `docs/skills/03-security-compliance/authentication-authorization.md`
- Gateway Trust Pattern: `backend/docs/GATEWAY_TRUST_ARCHITECTURE.md`

**Estimated Learning Time:** 1 week

---

#### 3.3 Data Protection & Privacy (🟡 IMPORTANT)

**Purpose:** Encryption, key management, data retention, deletion policies.

**Key Components:**
- **Encryption at Rest:** AES-256 for sensitive data
- **Encryption in Transit:** TLS 1.3 minimum
- **Key Management:** HashiCorp Vault
- **Data Classification:** Public, internal, sensitive (PHI)
- **Data Retention:** How long to keep data
- **Right to be Forgotten:** GDPR-like deletion requests

**Learning Resources:**
- Full Guide: `docs/skills/03-security-compliance/data-protection.md`

**Estimated Learning Time:** 1 week

---

### 4. DATA PERSISTENCE (🔴 CRITICAL)

**Why Critical:** Database is single source of truth. Wrong design = poor performance and compliance violations.

#### 4.1 PostgreSQL Multi-Tenant Database (🔴 CRITICAL)

**Purpose:** Primary SQL database for all services with multi-tenant isolation.

**Key Concepts:**
- **29 Databases:** One database per service
- **Multi-Tenant:** Single DB with 1000+ tenants
- **Row-Level Security:** Database enforces tenant isolation
- **Query Filtering:** All queries filter by `tenantId`
- **Performance:** Indexes optimized for tenant + resource ID

**Database Schema Pattern:**
```sql
CREATE TABLE patients (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,  -- REQUIRED for filtering
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    date_of_birth DATE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    -- INDEX: Enable efficient filtering
    CONSTRAINT uk_patient_tenant_id UNIQUE(tenant_id, id),
    INDEX idx_patient_tenant ON patients(tenant_id, id)
);

-- ALWAYS filter by tenant
SELECT * FROM patients
WHERE tenant_id = 'tenant-001' AND id = 'p-123';
```

**Learning Resources:**
- Full Guide: `docs/skills/04-database/postgresql-multi-tenant.md`
- Database Architecture Guide: `backend/docs/DATABASE_ARCHITECTURE_GUIDE.md`

**Estimated Learning Time:** 1.5 weeks

---

#### 4.2 Liquibase Migration Management (🔴 CRITICAL)

**Purpose:** Version-controlled database schema migrations with rollback support.

**Key Concepts:**
- **Migration Files:** XML changelogs defining schema changes
- **Rollback:** Every change has reversible rollback
- **Validation:** Entity-migration synchronization at test time
- **Pre-Build Checks:** Catch schema mismatches before Docker build

**Migration Lifecycle:**
```
1. Create entity (JPA class)
2. Create migration file (XML)
3. Add to db.changelog-master.xml
4. Run validation tests
5. Verify entity-migration sync
6. Commit & push
```

**Example Migration File:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog">
    <changeSet id="0001-create-patients-table" author="dev">
        <createTable tableName="patients">
            <column name="id" type="UUID">
                <constraints primaryKey="true"/>
            </column>
            <column name="tenant_id" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="first_name" type="VARCHAR(255)"/>
            <column name="created_at" type="TIMESTAMP">
                <constraints nullable="false" defaultValueDate="NOW()"/>
            </column>
        </createTable>
        <createIndex tableName="patients" indexName="idx_patient_tenant">
            <column name="tenant_id"/>
            <column name="id"/>
        </createIndex>

        <!-- ROLLBACK: Required for reversibility -->
        <rollback>
            <dropIndex tableName="patients" indexName="idx_patient_tenant"/>
            <dropTable tableName="patients"/>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

**Learning Resources:**
- Full Guide: `docs/skills/04-database/liquibase-migrations.md`
- Liquibase Workflow: `backend/docs/LIQUIBASE_DEVELOPMENT_WORKFLOW.md`
- Entity-Migration Guide: `backend/docs/ENTITY_MIGRATION_GUIDE.md` ⭐ CRITICAL

**Estimated Learning Time:** 1.5 weeks

---

#### 4.3 JPA/Hibernate Entity Design (🔴 CRITICAL)

**Purpose:** ORM mapping with strict entity-migration validation.

**Entity Pattern:**
```java
@Entity
@Table(name = "patients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;  // Multi-tenant isolation

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
```

**Critical Rules:**
- ✅ All entities must have `tenantId` column
- ✅ All entities must have `createdAt` and `updatedAt` timestamps
- ✅ Use UUID primary keys (not auto-increment)
- ✅ Use `@PrePersist`/`@PreUpdate` for timestamps
- ✅ Always create Liquibase migration
- ✅ Run entity-migration validation test

**Learning Resources:**
- Full Guide: `docs/skills/04-database/jpa-hibernate-design.md`
- Coding Standards: `backend/docs/CODING_STANDARDS.md`

**Estimated Learning Time:** 1 week

---

#### 4.4 Redis Caching Strategy (🟡 IMPORTANT)

**Purpose:** High-performance caching with HIPAA-compliant TTL.

**Cache Patterns:**
```
Cache Key Format: {tenantId}:{resourceType}:{resourceId}

Examples:
- tenant-001:Patient:p-123
- tenant-001:Measure:m-456
- tenant-001:MeasureDefinition:md-789

TTL Rules:
- PHI data: Maximum 5 minutes
- Reference data: 1 hour
- CQL templates: 24 hours
- Sessions: 15 minutes
```

**Cache-Aside Pattern:**
```java
@Service
public class PatientService {
    private final Cache cache;
    private final PatientRepository repository;

    public Patient getPatient(String patientId, String tenantId) {
        String cacheKey = String.format("%s:Patient:%s", tenantId, patientId);

        // Try cache first
        Patient cached = cache.get(cacheKey, Patient.class);
        if (cached != null) {
            return cached;
        }

        // Cache miss - fetch from DB
        Patient patient = repository.findByIdAndTenant(patientId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        // Store in cache (5 min TTL for PHI)
        cache.set(cacheKey, patient, Duration.ofMinutes(5));

        return patient;
    }
}
```

**Learning Resources:**
- Full Guide: `docs/skills/04-database/redis-caching.md`

**Estimated Learning Time:** 3 days

---

### 5. MESSAGING & EVENT-DRIVEN (🟡 IMPORTANT)

#### 5.1 Apache Kafka Event Streaming (🟡 IMPORTANT)

**Purpose:** Asynchronous, loosely coupled service communication via event topics.

**Key Concepts:**
- **Topics:** Named event streams
- **Producers:** Services publishing events
- **Consumers:** Services subscribing to events
- **Partitions:** Parallel processing within topic
- **Consumer Groups:** Load balancing across consumers
- **Offsets:** Track consumption position

**HDIM Kafka Topics:**
```
patient.events
├─ PatientCreated
├─ PatientUpdated
├─ PatientDeleted

measure.evaluation.complete
├─ MeasureEvaluated
├─ ResultsGenerated

care-gap.detected
├─ CareGapCreated
├─ CareGapClosed

audit.events
├─ PHIAccess
├─ DataModification
```

**Event Publishing:**
```java
@Service
public class PatientService {
    private final PatientRepository repository;
    private final KafkaTemplate<String, PatientEvent> kafkaTemplate;

    @Transactional
    public Patient createPatient(CreatePatientRequest request, String tenantId) {
        // 1. Create entity
        Patient patient = repository.save(toEntity(request, tenantId));

        // 2. Publish event (Transactional Outbox pattern)
        PatientCreatedEvent event = new PatientCreatedEvent(
            patient.getId(),
            patient.getTenantId(),
            patient.getFirstName(),
            patient.getLastName()
        );
        kafkaTemplate.send("patient.events", patient.getTenantId(), event);

        // 3. Return response
        return patient;
    }
}
```

**Event Consumption:**
```java
@Service
public class CareGapService {
    private final CareGapRepository repository;

    @KafkaListener(topics = "measure.evaluation.complete", groupId = "care-gap-service")
    public void handleMeasureEvaluated(MeasureEvaluatedEvent event) {
        if (event.getNumerator() == false && event.getDenominator() == true) {
            // Care gap detected
            CareGap gap = CareGap.builder()
                .patientId(event.getPatientId())
                .measureId(event.getMeasureId())
                .tenantId(event.getTenantId())
                .status("OPEN")
                .build();

            repository.save(gap);
        }
    }
}
```

**Learning Resources:**
- Full Guide: `docs/skills/05-messaging/kafka-event-streaming.md`

**Estimated Learning Time:** 1.5 weeks

---

#### 5.2 Event-Driven Patterns (🟡 IMPORTANT)

**Purpose:** Asynchronous patterns for scalability and decoupling.

**Key Patterns:**
- **Transactional Outbox:** Guarantee event publishing with database transaction
- **Event Versioning:** Handle schema evolution
- **Dead-Letter Queue (DLQ):** Handle failed events
- **Exponential Backoff:** Retry failed messages
- **Idempotent Processing:** Handle duplicate events safely

**Learning Resources:**
- Full Guide: `docs/skills/05-messaging/event-driven-patterns.md`

**Estimated Learning Time:** 1 week

---

### 6. TESTING & QUALITY ASSURANCE (🔴 CRITICAL)

#### 6.1 Unit Testing (🔴 CRITICAL)

**Purpose:** Test individual service methods in isolation.

**JUnit 5 + Mockito Pattern:**
```java
@ExtendWith(MockitoExtension.class)
class PatientServiceTest {
    @Mock
    private PatientRepository patientRepository;

    @Mock
    private KafkaTemplate<String, PatientEvent> kafkaTemplate;

    @InjectMocks
    private PatientService patientService;

    @Test
    void shouldCreatePatient_WhenValidRequest() {
        // ARRANGE
        CreatePatientRequest request = new CreatePatientRequest();
        request.setFirstName("John");
        request.setLastName("Doe");

        Patient expected = Patient.builder()
            .id(UUID.randomUUID())
            .tenantId("tenant-001")
            .firstName("John")
            .lastName("Doe")
            .build();

        when(patientRepository.save(any(Patient.class)))
            .thenReturn(expected);

        // ACT
        Patient result = patientService.createPatient(request, "tenant-001");

        // ASSERT
        assertThat(result.getId()).isEqualTo(expected.getId());
        assertThat(result.getFirstName()).isEqualTo("John");

        // Verify Kafka event published
        verify(kafkaTemplate).send(
            eq("patient.events"),
            eq("tenant-001"),
            any(PatientCreatedEvent.class)
        );
    }

    @Test
    void shouldThrowException_WhenPatientNotFound() {
        // ARRANGE
        when(patientRepository.findByIdAndTenant("p-999", "tenant-001"))
            .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() ->
            patientService.getPatient("p-999", "tenant-001")
        ).isInstanceOf(ResourceNotFoundException.class);
    }
}
```

**Testing Checklist:**
- ✅ Test happy path (success case)
- ✅ Test exception cases
- ✅ Test edge cases (null, empty, boundary values)
- ✅ Verify mock interactions (dependencies called correctly)
- ✅ Test multi-tenant isolation
- ✅ Test RBAC permissions

**Learning Resources:**
- Full Guide: `docs/skills/06-testing-qa/unit-testing.md`

**Estimated Learning Time:** 1 week

---

#### 6.2 Integration Testing (🔴 CRITICAL)

**Purpose:** Test API endpoints with real database interactions.

**MockMvc + Embedded Database Pattern:**
```java
@SpringBootTest
@AutoConfigureMockMvc
class PatientControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @BeforeEach
    void setUp() {
        patientRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "EVALUATOR")
    void shouldCreatePatient_WhenValid() throws Exception {
        // ARRANGE
        CreatePatientRequest request = new CreatePatientRequest();
        request.setFirstName("Jane");
        request.setLastName("Smith");

        // ACT & ASSERT
        mockMvc.perform(post("/api/v1/patients")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Tenant-ID", "tenant-001")
            .content(new ObjectMapper().writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Jane"));

        // Verify saved to DB
        assertThat(patientRepository.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void shouldForbid_WhenInsufficientRole() throws Exception {
        // VIEWER role cannot create patients
        mockMvc.perform(post("/api/v1/patients")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Tenant-ID", "tenant-001")
            .content("{}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EVALUATOR")
    void shouldIsolateTenants() throws Exception {
        // Create patient in tenant-001
        Patient p1 = Patient.builder()
            .tenantId("tenant-001")
            .firstName("John")
            .build();
        patientRepository.save(p1);

        // Try to access from tenant-002 (should fail)
        mockMvc.perform(get("/api/v1/patients/" + p1.getId())
            .header("X-Tenant-ID", "tenant-002"))
            .andExpect(status().isNotFound());
    }
}
```

**Testing Checklist:**
- ✅ Test valid requests (200 OK)
- ✅ Test authorization (403 Forbidden for wrong role)
- ✅ Test multi-tenant isolation (cross-tenant access denied)
- ✅ Test validation errors (400 Bad Request)
- ✅ Test error responses (500 Server Error)
- ✅ Test database state after operations

**Learning Resources:**
- Full Guide: `docs/skills/06-testing-qa/integration-testing.md`

**Estimated Learning Time:** 1.5 weeks

---

#### 6.3 Entity-Migration Validation Testing (🔴 CRITICAL - NEW)

**Purpose:** Prevent schema drift by validating entities match database migrations.

**Why Critical?**
- JPA entities must match actual database schema
- Mismatches cause runtime failures in production
- Pre-build validation catches errors early
- 100% rollback coverage prevents data loss

**Validation Test Pattern:**
```java
@SpringBootTest
class EntityMigrationValidationTest {
    @Autowired
    private SessionFactory sessionFactory;

    @Test
    void shouldValidatePatientEntityMatchesDatabaseSchema() {
        // 1. Run Liquibase migrations (creates actual schema)
        // 2. Set Hibernate validation mode: "validate"
        // 3. SessionFactory creation validates entities against schema
        // 4. If mismatch → Error thrown → Test fails

        // If we reach here, schema and entities are in sync
        assertThat(sessionFactory).isNotNull();
    }
}
```

**Pre-Build Validation Script:**
```bash
# Run before docker compose build
./scripts/validate-before-docker-build.sh

# This validates:
# 1. Database configuration
# 2. Entity-migration synchronization
# 3. Liquibase rollback coverage
```

**Common Validation Errors:**
| Error | Fix |
|-------|-----|
| Missing table | Create Liquibase migration for new entity |
| Wrong column type | Create migration to alter column type |
| Missing column | Add migration: `<addColumn>` changeset |
| Duplicate table | Check for duplicate migrations across services |

**Learning Resources:**
- Full Guide: `docs/skills/06-testing-qa/entity-migration-validation.md`
- Entity-Migration Guide: `backend/docs/ENTITY_MIGRATION_GUIDE.md` ⭐ CRITICAL
- Validation Reference: `backend/docs/DATABASE_ARCHITECTURE_GUIDE.md`

**Estimated Learning Time:** 1.5 weeks

---

#### 6.4 Performance & Load Testing (🟡 IMPORTANT)

**Purpose:** Validate latency, throughput, and resource utilization under load.

**Performance Targets:**
- Measure evaluation latency: <200ms p95
- API response time: <500ms p95
- Cache hit rate: >90%
- Kafka throughput: >1000 events/sec
- Database queries: <100ms p95

**Learning Resources:**
- Full Guide: `docs/skills/06-testing-qa/performance-testing.md`

**Estimated Learning Time:** 1 week

---

### 7. SPRING BOOT & JAVA (🔴 CRITICAL)

#### 7.1 Spring Boot 3.x Microservices (🔴 CRITICAL)

**Purpose:** Modern Spring framework for building 51 microservices.

**Key Components:**
- **Spring Core:** Dependency injection, AOP
- **Spring Web:** REST endpoints, request/response handling
- **Spring Data:** Database abstraction
- **Spring Security:** Authentication/authorization
- **Spring Cloud:** Distributed systems patterns
- **Spring Kafka:** Event streaming integration
- **Actuator:** Health checks, metrics

**Service Startup Pattern:**
```java
@SpringBootApplication
@EnableScheduling
@EnableAspectJAutoProxy
public class PatientEventServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PatientEventServiceApplication.class, args);
    }
}
```

**Configuration Pattern:**
```yaml
# application.yml
spring:
  application:
    name: patient-event-service
  datasource:
    url: jdbc:postgresql://postgres:5435/patient_db
    username: healthdata
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate  # REQUIRED: Never "create" or "update"
    properties:
      hibernate.dialect: org.hibernate.dialect.PostgreSQLDialect
  kafka:
    bootstrap-servers: kafka:9093
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
  cache:
    type: redis
    redis:
      host: redis
      port: 6380
      timeout: 5m

server:
  port: 8084
  servlet:
    context-path: /

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

**Learning Resources:**
- Full Guide: `docs/skills/07-spring-boot-java/spring-boot-3x-patterns.md`
- Spring Boot Official: spring.io/projects/spring-boot

**Estimated Learning Time:** 2 weeks

---

#### 7.2 Spring Data JPA (🔴 CRITICAL)

**Purpose:** Data access abstraction with query derivation.

**Repository Pattern:**
```java
@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    // Query derivation
    Optional<Patient> findByIdAndTenant(UUID id, String tenantId);

    List<Patient> findByTenantIdAndFirstNameContainingIgnoreCase(
        String tenantId,
        String firstName
    );

    // Custom queries
    @Query("SELECT p FROM Patient p " +
           "WHERE p.tenantId = :tenantId " +
           "AND p.createdAt >= :startDate " +
           "ORDER BY p.createdAt DESC")
    Page<Patient> findRecentPatients(
        @Param("tenantId") String tenantId,
        @Param("startDate") Instant startDate,
        Pageable pageable
    );
}
```

**Learning Resources:**
- Full Guide: `docs/skills/07-spring-boot-java/spring-data-jpa.md`

**Estimated Learning Time:** 1 week

---

#### 7.3 Spring Security & RBAC (🔴 CRITICAL)

**Purpose:** Role-based access control with authorization annotations.

**Security Configuration:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/v1/measures/evaluate").hasAnyRole("ADMIN", "EVALUATOR")
                .requestMatchers("/api/v1/*/delete").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(trustedHeaderAuthFilter, BasicAuthenticationFilter.class)
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        return http.build();
    }
}
```

**Authorization Annotations:**
```java
@RestController
@RequestMapping("/api/v1/measures")
public class MeasureController {
    @GetMapping
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ANALYST', 'VIEWER')")  // Multiple roles
    public ResponseEntity<List<MeasureResponse>> listMeasures() { }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")  // Admin only
    public ResponseEntity<MeasureResponse> createMeasure() { }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")  // Super admin only
    public ResponseEntity<Void> deleteMeasure() { }
}
```

**Learning Resources:**
- Full Guide: `docs/skills/07-spring-boot-java/spring-security-rbac.md`

**Estimated Learning Time:** 1.5 weeks

---

#### 7.4 REST API Design (🟡 IMPORTANT)

**Purpose:** RESTful API conventions and best practices.

**Resource-Oriented Design:**
```
GET    /api/v1/patients          → List patients
POST   /api/v1/patients          → Create patient
GET    /api/v1/patients/{id}     → Get patient
PUT    /api/v1/patients/{id}     → Update patient
DELETE /api/v1/patients/{id}     → Delete patient
```

**Request/Response Pattern:**
```java
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {
    private final PatientService patientService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    @Audited(eventType = "PATIENT_CREATE")
    public ResponseEntity<PatientResponse> createPatient(
            @Valid @RequestBody CreatePatientRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId,
            HttpServletResponse response) {

        // HIPAA: Set cache control headers
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");

        // Business logic
        PatientResponse result = patientService.createPatient(request, tenantId);

        // Return 201 Created with Location header
        return ResponseEntity
            .created(URI.create("/api/v1/patients/" + result.getId()))
            .body(result);
    }
}
```

**Error Handling:**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(HdimValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            HdimValidationException ex) {
        ErrorResponse error = new ErrorResponse(
            "VALIDATION_ERROR",
            ex.getMessage(),
            400
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        ErrorResponse error = new ErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            500
        );
        return ResponseEntity.internalServerError().body(error);
    }
}
```

**Learning Resources:**
- Full Guide: `docs/skills/07-spring-boot-java/rest-api-design.md`

**Estimated Learning Time:** 1 week

---

### 8. INFRASTRUCTURE & DEVOPS (🟡 IMPORTANT)

#### 8.1 Docker & Docker Compose (🟡 IMPORTANT)

**Purpose:** Container orchestration for local development environment.

**Docker Compose Services:**
```yaml
version: '3.9'

services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: healthdata
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports:
      - "5435:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6380:6379"

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9093,PLAINTEXT_HOST://localhost:9094
    depends_on:
      - zookeeper

  patient-event-service:
    build: ./backend/modules/services/patient-event-service
    ports:
      - "8084:8084"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5435/patient_db
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9093
      SPRING_REDIS_HOST: redis
    depends_on:
      - postgres
      - kafka
      - redis

volumes:
  postgres_data:
```

**Common Commands:**
```bash
# Start all services
docker compose up -d

# View logs
docker compose logs -f patient-event-service

# Execute command in container
docker compose exec patient-event-service bash

# Stop all services
docker compose down

# Rebuild single service
docker compose build patient-event-service
```

**Learning Resources:**
- Full Guide: `docs/skills/08-infrastructure/docker-compose.md`
- Docker Compose Usage: `docs/DOCKER_COMPOSE_USAGE_GUIDE.md`

**Estimated Learning Time:** 1 week

---

#### 8.2 Kubernetes (Optional - 🟢 OPTIONAL)

**Purpose:** Production container orchestration and scaling.

**Key Concepts:**
- **Deployments:** Service definition and scaling
- **Services:** Network exposure and load balancing
- **ConfigMaps:** Configuration management
- **Secrets:** Sensitive data management
- **Ingress:** External access routing

**Learning Resources:**
- Full Guide: `docs/skills/08-infrastructure/kubernetes-deployment.md`

**Estimated Learning Time:** 2 weeks (optional)

---

#### 8.3 Monitoring & Observability (🟡 IMPORTANT)

**Purpose:** Prometheus metrics and Grafana dashboards.

**Metrics Collection:**
```java
@Service
public class MeasureService {
    private final MeterRegistry meterRegistry;

    public EvaluationResult evaluate(MeasureRequest request) {
        Timer timer = Timer.start(meterRegistry);

        try {
            EvaluationResult result = performEvaluation(request);

            // Record success metric
            meterRegistry.counter(
                "measures.evaluated",
                "measure_id", request.getMeasureId(),
                "result", result.getNumerator() ? "pass" : "fail"
            ).increment();

            return result;
        } catch (Exception ex) {
            meterRegistry.counter(
                "measures.evaluation_errors",
                "measure_id", request.getMeasureId()
            ).increment();
            throw ex;
        } finally {
            timer.stop(Timer.builder("measures.evaluation_time")
                .register(meterRegistry));
        }
    }
}
```

**Learning Resources:**
- Full Guide: `docs/skills/08-infrastructure/monitoring-observability.md`
- Distributed Tracing: `backend/docs/DISTRIBUTED_TRACING_GUIDE.md`

**Estimated Learning Time:** 1 week

---

### 9. API DESIGN (🟡 IMPORTANT)

#### 9.1 OpenAPI/Swagger Documentation (🟡 IMPORTANT)

**Purpose:** Auto-generated API documentation and client generation.

**Springdoc OpenAPI Annotation:**
```java
@RestController
@RequestMapping("/api/v1/patients")
@Tag(name = "Patients", description = "Patient management API")
public class PatientController {

    @PostMapping
    @Operation(summary = "Create patient",
               description = "Create new patient in system")
    @ApiResponse(responseCode = "201", description = "Patient created",
                 content = @Content(schema = @Schema(implementation = PatientResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    public ResponseEntity<PatientResponse> createPatient(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Patient creation request",
                required = true) CreatePatientRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(patientService.createPatient(request, tenantId));
    }
}
```

**Access Swagger UI:**
```
http://localhost:8084/swagger-ui.html
http://localhost:8084/v3/api-docs  (JSON spec)
```

**Learning Resources:**
- Full Guide: `docs/skills/09-api-design/openapi-integration.md`

**Estimated Learning Time:** 3 days

---

### 10. CODING STANDARDS & PATTERNS (🟡 IMPORTANT)

#### 10.1 HDIM Coding Patterns (🟡 IMPORTANT)

**Purpose:** Consistency across all 51 services.

**Service Layer Pattern:**
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientService {
    private final PatientRepository patientRepository;
    private final KafkaTemplate<String, PatientEvent> kafkaTemplate;

    // READ operations (no @Transactional needed)
    public PatientResponse getPatient(String patientId, String tenantId) {
        return patientRepository.findByIdAndTenant(patientId, tenantId)
            .map(this::mapToResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
    }

    // WRITE operations (@Transactional required)
    @Transactional
    public PatientResponse createPatient(CreatePatientRequest request, String tenantId) {
        Patient patient = patientRepository.save(toEntity(request, tenantId));

        // Publish event
        PatientCreatedEvent event = new PatientCreatedEvent(patient);
        kafkaTemplate.send("patient.events", tenantId, event);

        return mapToResponse(patient);
    }

    private PatientResponse mapToResponse(Patient patient) {
        return PatientResponse.builder()
            .id(patient.getId())
            .firstName(patient.getFirstName())
            .lastName(patient.getLastName())
            .build();
    }
}
```

**Controller Pattern:**
```java
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {
    private final PatientService patientService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    @Audited(eventType = "PATIENT_CREATE")
    public ResponseEntity<PatientResponse> createPatient(
            @Valid @RequestBody CreatePatientRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId,
            HttpServletResponse response) {

        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");

        PatientResponse result = patientService.createPatient(request, tenantId);

        return ResponseEntity
            .created(URI.create("/api/v1/patients/" + result.getId()))
            .body(result);
    }
}
```

**Learning Resources:**
- Full Guide: `docs/skills/10-coding-standards/hdim-coding-patterns.md`
- Coding Standards: `backend/docs/CODING_STANDARDS.md`

**Estimated Learning Time:** 1 week

---

## Capability Matrix

| Skill | Priority | Importance | Complexity | Phase | Weeks | Status |
|-------|----------|-----------|-----------|-------|-------|--------|
| CQRS + Event Sourcing | 🔴 | Core | Very High | 1 | 1 | Foundation |
| HEDIS Quality Measures | 🔴 | Business | Very High | 1 | 2 | Foundation |
| HIPAA Compliance | 🔴 | Regulatory | High | 1 | 1.5 | Foundation |
| FHIR R4 Integration | 🔴 | Standards | High | 1 | 2 | Foundation |
| Multi-Tenant Architecture | 🔴 | Infrastructure | High | 1 | 1 | Foundation |
| PostgreSQL + Liquibase | 🔴 | Persistence | High | 1 | 1.5 | Foundation |
| Spring Boot 3.x | 🔴 | Framework | Medium | 1 | 2 | Foundation |
| CQL Execution | 🔴 | Healthcare | Very High | 2 | 1.5 | Intermediate |
| Care Gap Detection | 🔴 | Business | High | 2 | 1.5 | Intermediate |
| Authentication/RBAC | 🟡 | Security | High | 2 | 1 | Intermediate |
| Kafka Event Streaming | 🟡 | Messaging | High | 2 | 1.5 | Intermediate |
| Entity-Migration Validation | 🟡 | QA | Medium | 2 | 1.5 | Intermediate |
| Docker/Compose | 🟡 | Infrastructure | Medium | 2 | 1 | Intermediate |
| Risk Stratification | 🟡 | Analytics | High | 3 | 1 | Advanced |
| OpenTelemetry Tracing | 🟡 | Observability | Medium | 3 | 1 | Advanced |
| REST API Design | 🟡 | Architecture | Medium | 3 | 1 | Advanced |
| EHR Integration | 🟡 | Healthcare | High | 4 | 1 | Specialized |
| Kubernetes | 🟢 | Infrastructure | High | 4 | 2 | Optional |
| Performance Testing | 🟢 | QA | Medium | 4 | 1 | Optional |

---

## Learning Paths

### Path 1: Backend Engineer (11 weeks)

**Week 1:** Foundation
1. CQRS + Event Sourcing (1 week)
2. HEDIS Quality Measures (2 weeks)
3. HIPAA Compliance (1.5 weeks)

**Week 2-3:** Healthcare Standards
1. FHIR R4 Integration (2 weeks)
2. CQL Execution (1.5 weeks)

**Week 4:** Core Architecture
1. Multi-Tenant Architecture (1 week)
2. PostgreSQL + Liquibase (1.5 weeks)

**Week 5-6:** Platform
1. Spring Boot 3.x (2 weeks)
2. Spring Security RBAC (1 week)

**Week 7:** Messaging & Events
1. Kafka Event Streaming (1.5 weeks)
2. Event-Driven Patterns (1 week)

**Week 8-9:** Quality & Testing
1. Unit Testing (1 week)
2. Integration Testing (1.5 weeks)
3. Entity-Migration Validation (1.5 weeks)

**Week 10-11:** Advanced
1. Care Gap Detection (1.5 weeks)
2. Risk Stratification (1 week)
3. OpenTelemetry Tracing (1 week)

---

### Path 2: DevOps/Infrastructure (8 weeks)

**Week 1:** Foundation
1. Docker/Compose (1 week)
2. Multi-Tenant Architecture (1 week)

**Week 2-3:** Monitoring
1. OpenTelemetry Tracing (1 week)
2. Monitoring & Observability (1 week)

**Week 4-6:** Kubernetes
1. Kubernetes Deployment (2 weeks)
2. Container Security (1 week)

**Week 7-8:** Advanced
1. Performance Testing (1 week)
2. Disaster Recovery (1 week)

---

### Path 3: Security/Compliance (6 weeks)

**Week 1:** Foundation
1. HIPAA Compliance (1.5 weeks)
2. Data Protection (1 week)

**Week 2-3:** Application Security
1. Authentication/Authorization (1 week)
2. Spring Security RBAC (1 week)

**Week 4-5:** Infrastructure Security
1. Container Security (1 week)
2. Network Security (1 week)

**Week 6:** Audit & Compliance
1. Audit Logging (1 week)

---

### Path 4: Quality Assurance (7 weeks)

**Week 1-2:** Testing Foundation
1. Unit Testing (1 week)
2. Integration Testing (1.5 weeks)

**Week 3:** HDIM-Specific
1. Entity-Migration Validation (1.5 weeks)

**Week 4-5:** Advanced Testing
1. Performance Testing (1 week)
2. Security Testing (1 week)

**Week 6-7:** Test Automation
1. Test Architecture (1 week)
2. Continuous Testing (1 week)

---

## Quick Reference Checklist

### Backend Developer Quick Start
- [ ] Understand CQRS event sourcing pattern
- [ ] Learn HEDIS measure evaluation basics
- [ ] Know HIPAA cache requirements (5 min TTL)
- [ ] Implement multi-tenant filtering in queries
- [ ] Create Spring Boot service with repository
- [ ] Write unit + integration tests
- [ ] Publish events to Kafka
- [ ] Add @Audited annotation for PHI access

### HIPAA Compliance Checklist
- [ ] All PHI endpoints have Cache-Control headers
- [ ] @Audited annotation on PHI access methods
- [ ] All queries filter by tenantId
- [ ] No PHI in log messages
- [ ] Redis cache TTL ≤ 5 minutes
- [ ] @PreAuthorize on sensitive endpoints
- [ ] Encryption enabled (TLS, AES-256)

### Quality Assurance Checklist
- [ ] All service methods have unit tests
- [ ] All API endpoints have integration tests
- [ ] Multi-tenant isolation tested
- [ ] RBAC permissions validated
- [ ] Entity-migration synchronization verified
- [ ] 80%+ code coverage maintained
- [ ] Performance targets met

### DevOps Checklist
- [ ] Docker images built successfully
- [ ] All services start in Docker Compose
- [ ] Health checks passing
- [ ] Metrics exported to Prometheus
- [ ] Logs aggregated and searchable
- [ ] Kubernetes manifests ready (optional)

---

## Getting Started

### 1. **Read the Foundation**
- Start with: `docs/skills/01-architecture/cqrs-event-sourcing.md`
- Why: CQRS/Event Sourcing is the foundation for all other patterns

### 2. **Understand the Domain**
- Read: `docs/skills/02-healthcare-domain/hedis-quality-measures.md`
- Why: Understand what the system actually does (quality measure evaluation)

### 3. **Know the Compliance**
- Study: `docs/skills/03-security-compliance/hipaa-compliance.md`
- Why: All code must be HIPAA-compliant (non-negotiable)

### 4. **Follow Your Path**
- Backend? → Complete "Backend Engineer" learning path
- DevOps? → Complete "DevOps/Infrastructure" learning path
- Security? → Complete "Security/Compliance" learning path
- QA? → Complete "Quality Assurance" learning path

### 5. **Get Hands-On**
- Clone the repo
- Start Docker Compose
- Make a small code change
- Write tests
- Verify entity-migration validation
- Commit and push

---

## Additional Resources

### Documentation Hub
- **Main Documentation:** `docs/README.md` (1,411+ pages)
- **Backend Docs:** `backend/docs/README.md` (Complete technical index)
- **Service Catalog:** `docs/services/SERVICE_CATALOG.md` (All 51 services)
- **Architecture:** `docs/architecture/` (System design patterns)

### Key Technical Guides
- ⭐ **Liquibase Workflow:** `backend/docs/LIQUIBASE_DEVELOPMENT_WORKFLOW.md` (Database migrations)
- ⭐ **Entity-Migration:** `backend/docs/ENTITY_MIGRATION_GUIDE.md` (Critical for schema validation)
- **Event Sourcing:** `docs/architecture/EVENT_SOURCING_ARCHITECTURE.md` (CQRS patterns)
- **Gateway Design:** `docs/architecture/GATEWAY_ARCHITECTURE.md` (Multi-gateway architecture)
- **Build Management:** `backend/docs/BUILD_MANAGEMENT_GUIDE.md` (Build troubleshooting)
- **HIPAA Compliance:** `backend/HIPAA-CACHE-COMPLIANCE.md` (PHI protection)

### Quick References
- **Command Reference:** `backend/docs/COMMAND_REFERENCE.md` (All common commands)
- **Coding Standards:** `backend/docs/CODING_STANDARDS.md` (Patterns and conventions)
- **Troubleshooting:** `docs/troubleshooting/README.md` (Problem-solving decision trees)

### External Resources
- **Spring Boot:** spring.io/projects/spring-boot
- **FHIR:** hl7.org/fhir (FHIR R4 specification)
- **CQL:** cql.hl7.org (Clinical Quality Language specification)
- **HEDIS:** ncqa.org (HEDIS measure specifications)
- **Kafka:** kafka.apache.org (Kafka documentation)
- **PostgreSQL:** postgresql.org (PostgreSQL documentation)

---

## Success Metrics

### Code Quality
- ✅ 90%+ code review pass rate on first review
- ✅ <5 production issues per quarter from new code
- ✅ >80% test coverage on all new code

### Compliance
- ✅ <2% HIPAA violations in audit
- ✅ 100% entity-migration validation coverage
- ✅ All PHI access logged and auditable

### Productivity
- ✅ New developer productive in <2 weeks
- ✅ <1 hour average time to onboard on new service
- ✅ >95% architecture pattern adherence

### System Health
- ✅ 99.9% uptime SLA
- ✅ <200ms p95 measure evaluation latency
- ✅ <500ms p95 API response time

---

## Next Steps

1. **This Week:** Read CQRS/Event Sourcing and HEDIS Quality Measures guides
2. **Next Week:** Set up local environment and run first Docker Compose
3. **Week 3:** Make first code change with proper testing
4. **Week 4:** Complete first small feature with full lifecycle (design, code, test, commit)

---

**Questions?** Refer to:
- `docs/troubleshooting/README.md` - Problem-solving guide
- `docs/README.md` - Complete documentation portal
- Service-specific guides in `docs/skills/` directory

---

_Last Updated: January 20, 2026_
_Version: 1.0 - Foundation Release_
_Maintained by: HDIM Development Team_
