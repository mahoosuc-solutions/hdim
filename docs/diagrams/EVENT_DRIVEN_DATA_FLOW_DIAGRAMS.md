# Event-Driven Data Flow Diagrams - Professional Draw.io Specifications

**Purpose:** Technical diagram specifications for HDIM event-driven architecture including security controls, data flows, and user interactions.

**Tools:** Draw.io (diagrams.net), PlantUML, or Lucidchart

**Classification:** Internal Technical Documentation

---

## Diagram 1: Quality Measure Evaluation - Complete Data Flow with Security

### Overview
End-to-end data flow showing how a quality measure evaluation request flows through HDIM's event-driven architecture with IT security controls, HIPAA compliance checkpoints, and user interaction points.

### Diagram Specifications

**Diagram Type:** Swimlane Flowchart + Security Overlay

**Swimlanes (Vertical):**
1. **Clinical User** (Provider/MA/RN)
2. **Web Application Layer** (Angular Clinical Portal)
3. **API Gateway** (Kong + Spring Cloud Gateway)
4. **Event Services** (REST APIs)
5. **Event Bus** (Apache Kafka)
6. **Event Handlers** (Business Logic)
7. **Data Stores** (Event Store + Projections + FHIR)
8. **Security Layer** (JWT, Audit, Encryption)

---

### Step-by-Step Flow

#### Phase 1: User Authentication & Authorization

```
┌─────────────────────────────────────────────────────────────────┐
│ PHASE 1: AUTHENTICATION & AUTHORIZATION                         │
│ Security Controls: JWT, RBAC, Multi-Tenant Isolation           │
└─────────────────────────────────────────────────────────────────┘

[Clinical User]
    └──[1. Login]──▶ [Angular Portal]
                        └──[2. POST /auth/login]──▶ [API Gateway]
                                                       ├─[3. Validate Credentials]
                                                       ├─[4. Generate JWT Token]
                                                       │  • HS256 signature
                                                       │  • 15-min expiration
                                                       │  • Tenant ID claim
                                                       │  • Role claims (ADMIN, EVALUATOR)
                                                       └─[5. Audit: LoginEvent]──▶ [Event Bus]
                                                            └──▶ [Audit Store]
    ◀──[6. Return JWT + User Profile]──┘

🔒 SECURITY CHECKPOINT #1: User Identity Verified
   • Action: User authenticated with credentials
   • Control: Bcrypt password hashing, account lockout after 5 failures
   • Compliance: HIPAA § 164.312(a)(2)(i) - Unique User Identification
```

**Draw.io Elements:**
- **Shape:** Swimlane background color: `#E3F2FD` (light blue)
- **User Icon:** Rounded rectangle with person icon
- **Security Shield:** Red shield icon at checkpoint
- **Arrows:** Solid lines with labels
- **Callout Box:** Yellow box for security controls

---

#### Phase 2: Authorization & Tenant Isolation

```
┌─────────────────────────────────────────────────────────────────┐
│ PHASE 2: AUTHORIZATION & TENANT ISOLATION                       │
│ Security Controls: RBAC, Row-Level Security, Tenant Validation │
└─────────────────────────────────────────────────────────────────┘

[Clinical User]
    └──[7. Navigate to Quality Measures]──▶ [Angular Portal]
            └──[8. GET /api/v1/quality-measures]──▶ [API Gateway]
                    │ Headers:
                    │  • Authorization: Bearer {JWT}
                    │  • X-Tenant-ID: org-123
                    │
                    ├─[9. Validate JWT Signature]
                    ├─[10. Extract Claims]
                    │      • User ID: user-456
                    │      • Tenant ID: org-123
                    │      • Roles: ["ADMIN", "EVALUATOR"]
                    │
                    ├─[11. Check Permissions]
                    │      @PreAuthorize("hasRole('EVALUATOR')")
                    │      Required: VIEW_EVALUATIONS
                    │
                    ├─[12. Validate Tenant Access]
                    │      • User belongs to org-123? ✓
                    │      • User has access to requested tenant? ✓
                    │
                    └─[13. Forward Request]──▶ [Quality Measure Service]
                            Headers Injected:
                              • X-Auth-User-ID: user-456
                              • X-Auth-Tenant-ID: org-123
                              • X-Auth-Roles: ADMIN,EVALUATOR
                              • X-Gateway-Trust-Token: {signed}

🔒 SECURITY CHECKPOINT #2: Authorization & Tenant Access Verified
   • Action: User authorized for quality measure operations
   • Control: Role-based access control (RBAC), tenant isolation
   • Compliance: HIPAA § 164.308(a)(4) - Access Authorization
```

**Draw.io Elements:**
- **Gateway Shape:** Hexagon for API Gateway
- **Decision Diamond:** For permission checks
- **Header Box:** Code block style with monospace font
- **Security Annotation:** Green checkmark for passed checks

---

#### Phase 3: Evaluation Request & Event Emission

```
┌─────────────────────────────────────────────────────────────────┐
│ PHASE 3: EVALUATION REQUEST & EVENT EMISSION                    │
│ Security Controls: Input Validation, PHI Logging Prevention     │
└─────────────────────────────────────────────────────────────────┘

[Clinical User]
    └──[14. Click "Run Evaluation"]──▶ [Angular Portal]
            │ Form Data:
            │  • Measure: HEDIS Diabetes HbA1c Control (CDC)
            │  • Population: All diabetic patients
            │  • Measurement Period: 2025-01-01 to 2025-12-31
            │
            └──[15. POST /api/v1/evaluations]──▶ [Quality Measure Event Service]
                    │
                    ├─[16. Validate Input]
                    │      • Measure ID exists? ✓
                    │      • Date range valid? ✓
                    │      • Tenant has access to measure? ✓
                    │
                    ├─[17. Create Command]
                    │      CreateEvaluationCommand {
                    │        evaluationId: "eval-789"
                    │        measureId: "CDC-HbA1c"
                    │        tenantId: "org-123"
                    │        requestedBy: "user-456"
                    │        measurementPeriod: {start, end}
                    │      }
                    │
                    ├─[18. Emit Event]──▶ [Kafka Topic: evaluation-requests]
                    │      EvaluationRequestedEvent {
                    │        eventId: "evt-001"
                    │        aggregateId: "eval-789"
                    │        eventType: "EvaluationRequested"
                    │        timestamp: "2026-01-25T10:15:00Z"
                    │        tenantId: "org-123"  // ⚠️ SENSITIVE
                    │        payload: {
                    │          measureId: "CDC-HbA1c",
                    │          requestedBy: "user-456"
                    │        }
                    │      }
                    │
                    ├─[19. Write to Event Store]──▶ [PostgreSQL event_store table]
                    │      • Immutable append-only log
                    │      • Row-level security by tenant_id
                    │      • Encrypted at rest (AES-256)
                    │
                    └─[20. Audit Log]──▶ [Audit Service]
                            AuditEvent {
                              action: "EVALUATION_REQUESTED",
                              userId: "user-456",
                              tenantId: "org-123",
                              resourceType: "QualityMeasure",
                              resourceId: "CDC-HbA1c",
                              timestamp: "2026-01-25T10:15:00Z",
                              ipAddress: "10.0.1.45",  // ⚠️ PII - hashed in storage
                              userAgent: "Mozilla/5.0..."
                            }

🔒 SECURITY CHECKPOINT #3: Input Validation & Audit Trail Created
   • Action: Evaluation request validated and logged
   • Control: Input sanitization, audit logging, immutable event store
   • Compliance: HIPAA § 164.312(b) - Audit Controls
```

**Draw.io Elements:**
- **Kafka Topic:** Cloud shape with "Kafka" label
- **Event Store:** Cylinder (database) with lock icon
- **Code Blocks:** Monospace font, light gray background
- **Warning Icon:** Orange exclamation for sensitive data annotations

---

#### Phase 4: Cohort Identification (CQL Initial Population)

```
┌─────────────────────────────────────────────────────────────────┐
│ PHASE 4: COHORT IDENTIFICATION                                  │
│ Security Controls: Data Minimization, Encrypted Queries         │
└─────────────────────────────────────────────────────────────────┘

[Event Bus: evaluation-requests]
    └──[21. Consume Event]──▶ [Quality Measure Event Handler]
            │
            ├─[22. Parse CQL Initial Population]
                    CQL Expression:
                    ```cql
                    define "Initial Population":
                      ["Patient"] P
                        where AgeInYearsAt(start of MeasurementPeriod) >= 18
                          and AgeInYearsAt(start of MeasurementPeriod) < 75
                          and exists (["Condition": "Diabetes"] D
                            where D.clinicalStatus = 'active')
                    ```
            │
            ├─[23. Query FHIR Service]──▶ [FHIR Service]
                    GET /fhir/Patient?
                      birthdate=ge1951-01-01&birthdate=le2007-12-31
                      &_has:Condition:patient:code=http://snomed.info/sct|73211009
                      &_tenant=org-123  // ⚠️ Multi-tenant filter

                    Response: 1,500 patient IDs (PHI FILTERED - IDs only)
                    [ "pat-001", "pat-002", ..., "pat-1500" ]
            │
            ├─[24. Emit CohortIdentifiedEvent]──▶ [Kafka: cohort-events]
                    CohortIdentifiedEvent {
                      evaluationId: "eval-789",
                      patientCount: 1500,
                      patientIds: [...],  // ⚠️ PHI - encrypted in transit (TLS 1.3)
                      tenantId: "org-123"
                    }
            │
            └─[25. Update Projection]──▶ [Evaluation Projection (Read Model)]
                    UPDATE evaluation_status
                    SET status = 'COHORT_IDENTIFIED',
                        patient_count = 1500,
                        updated_at = NOW()
                    WHERE evaluation_id = 'eval-789'
                      AND tenant_id = 'org-123';  // ⚠️ Row-level security

🔒 SECURITY CHECKPOINT #4: Data Minimization & Encryption
   • Action: Patient cohort identified with minimal data exposure
   • Control: TLS 1.3 in-transit encryption, row-level security
   • Compliance: HIPAA § 164.514(b) - De-Identification (IDs only, no demographics)
```

**Draw.io Elements:**
- **CQL Code Block:** Light purple background, courier font
- **FHIR API:** REST endpoint box with HTTP method label
- **Database Update:** Cylinder with UPDATE arrow
- **Encryption Icon:** Padlock next to data in transit

---

#### Phase 5: Clinical Data Retrieval (FHIR Resources)

```
┌─────────────────────────────────────────────────────────────────┐
│ PHASE 5: CLINICAL DATA RETRIEVAL                                │
│ Security Controls: PHI Encryption, Cache TTL, Access Logging    │
└─────────────────────────────────────────────────────────────────┘

[Event Bus: cohort-events]
    └──[26. Consume CohortIdentifiedEvent]──▶ [Quality Measure Event Handler]
            │
            ├─[27. For Each Patient in Cohort (Parallel Processing)]
            │       Batch Size: 100 patients/batch
            │       Total Batches: 15 (1,500 / 100)
            │
            │   For patient "pat-001":
            │   ├─[28. Check Cache]──▶ [Redis Cache]
            │   │       Key: "fhir:pat-001:observations:2025"
            │   │       TTL: 2 minutes (HIPAA compliant)
            │   │       Cache Hit? ✗ (miss)
            │   │
            │   ├─[29. Fetch FHIR Resources]──▶ [FHIR Service]
            │   │       GET /fhir/Observation?
            │   │         patient=pat-001
            │   │         &code=http://loinc.org|4548-4  // HbA1c LOINC code
            │   │         &date=ge2025-01-01&date=le2025-12-31
            │   │         &_tenant=org-123
            │   │
            │   │       Response (FHIR Bundle - ⚠️ PHI):
            │   │       {
            │   │         "resourceType": "Bundle",
            │   │         "entry": [
            │   │           {
            │   │             "resource": {
            │   │               "resourceType": "Observation",
            │   │               "id": "obs-123",
            │   │               "code": { "coding": [{ "system": "http://loinc.org", "code": "4548-4" }] },
            │   │               "valueQuantity": { "value": 7.2, "unit": "%" },
            │   │               "effectiveDateTime": "2025-06-15T10:30:00Z"
            │   │             }
            │   │           }
            │   │         ]
            │   │       }
            │   │
            │   ├─[30. Store in Cache (Encrypted)]──▶ [Redis Cache]
            │   │       Key: "fhir:pat-001:observations:2025"
            │   │       Value: {encrypted FHIR bundle}  // AES-256-GCM
            │   │       TTL: 120 seconds
            │   │
            │   ├─[31. Emit DataRetrievalCompletedEvent]──▶ [Kafka: data-retrieval-events]
            │   │       DataRetrievalCompletedEvent {
            │   │         patientId: "pat-001",
            │   │         resourceCount: 1,
            │   │         evaluationId: "eval-789"
            │   │       }
            │   │
            │   └─[32. Audit PHI Access]──▶ [Audit Service]
            │           AuditEvent {
            │             action: "PHI_ACCESS",
            │             userId: "user-456",
            │             resourceType: "Observation",
            │             resourceId: "obs-123",
            │             patientId: "pat-001",  // ⚠️ PHI
            │             purpose: "QUALITY_MEASURE_EVALUATION",
            │             timestamp: "2026-01-25T10:16:05Z"
            │           }

🔒 SECURITY CHECKPOINT #5: PHI Access Logged & Encrypted
   • Action: Clinical data retrieved for quality measure evaluation
   • Control: Cache encryption (AES-256), TTL ≤ 2 min, audit trail
   • Compliance: HIPAA § 164.312(a)(1) - Access Control, § 164.312(e)(2)(ii) - Encryption
```

**Draw.io Elements:**
- **Cache Icon:** Circular cache symbol with timer
- **Parallel Processing:** Multiple arrows splitting from single source
- **FHIR Bundle:** JSON code block with syntax highlighting
- **Audit Trail:** Document icon with timestamp

---

#### Phase 6: CQL Evaluation (Clinical Logic Execution)

```
┌─────────────────────────────────────────────────────────────────┐
│ PHASE 6: CQL EVALUATION                                         │
│ Security Controls: Sandboxed Execution, Result Validation       │
└─────────────────────────────────────────────────────────────────┘

[Event Bus: data-retrieval-events]
    └──[33. Consume DataRetrievalCompletedEvent]──▶ [CQL Engine Service]
            │
            ├─[34. Load CQL Library]
                    Measure: HEDIS Diabetes HbA1c Control (CDC)
                    CQL Version: 1.5.0 (NCQA HEDIS 2025)

                    CQL Logic:
                    ```cql
                    library DiabetesHbA1cControl version '1.5.0'

                    define "Denominator":
                      "Initial Population"  // Diabetic patients 18-75

                    define "Numerator":
                      "Denominator" D
                        where exists (
                          ["Observation": "HbA1c Laboratory Test"] HbA1c
                            where HbA1c.value < 8.0 '%'
                              and HbA1c.effective during "Measurement Period"
                        )

                    define "Result":
                      case
                        when "Numerator" then 'COMPLIANT'
                        when "Denominator" and not "Numerator" then 'NON_COMPLIANT'
                        else 'NOT_ELIGIBLE'
                      end
                    ```
            │
            ├─[35. Execute CQL for Patient pat-001]
                    Context: Patient = pat-001
                    Data: { Observation: [{ value: 7.2%, effectiveDateTime: 2025-06-15 }] }

                    Evaluation Steps:
                      1. "Initial Population" → TRUE (age 58, diabetic)
                      2. "Denominator" → TRUE
                      3. "Numerator" → TRUE (HbA1c 7.2% < 8.0%)
                      4. "Result" → 'COMPLIANT'
            │
            ├─[36. Emit PatientEvaluatedEvent]──▶ [Kafka: patient-evaluation-events]
                    PatientEvaluatedEvent {
                      evaluationId: "eval-789",
                      patientId: "pat-001",
                      measureId: "CDC-HbA1c",
                      result: "COMPLIANT",
                      details: {
                        hba1cValue: 7.2,
                        testDate: "2025-06-15",
                        complianceThreshold: 8.0
                      },
                      tenantId: "org-123"
                    }
            │
            └─[37. Update Patient Projection]──▶ [Patient Evaluation Projection]
                    INSERT INTO patient_evaluation_results (
                      evaluation_id, patient_id, measure_id, result, tenant_id
                    ) VALUES (
                      'eval-789', 'pat-001', 'CDC-HbA1c', 'COMPLIANT', 'org-123'
                    );

🔒 SECURITY CHECKPOINT #6: CQL Execution Sandboxed
   • Action: Clinical logic evaluated for individual patient
   • Control: CQL execution in isolated JVM context, result validation
   • Compliance: HIPAA § 164.308(a)(5)(ii)(D) - Application Security
```

**Draw.io Elements:**
- **CQL Engine:** Cog/gear icon with "CQL" label
- **Decision Tree:** Flowchart for CQL logic evaluation
- **Evaluation Result:** Diamond shape with COMPLIANT/NON_COMPLIANT
- **Database Insert:** Arrow to database with INSERT label

---

#### Phase 7: Results Aggregation & Care Gap Generation

```
┌─────────────────────────────────────────────────────────────────┐
│ PHASE 7: AGGREGATION & CARE GAP GENERATION                      │
│ Security Controls: De-Identified Aggregates, Access Control     │
└─────────────────────────────────────────────────────────────────┘

[Event Bus: patient-evaluation-events]
    │ (After all 1,500 patients evaluated)
    │
    └──[38. Trigger Aggregation]──▶ [Quality Measure Event Handler]
            │
            ├─[39. Calculate Measure Results]
                    SQL Query:
                    ```sql
                    SELECT
                      COUNT(*) FILTER (WHERE result = 'COMPLIANT') as numerator,
                      COUNT(*) as denominator,
                      (COUNT(*) FILTER (WHERE result = 'COMPLIANT')::float /
                       COUNT(*)::float * 100) as compliance_rate
                    FROM patient_evaluation_results
                    WHERE evaluation_id = 'eval-789'
                      AND tenant_id = 'org-123';  -- Tenant isolation
                    ```

                    Results:
                      • Numerator: 1,200 (compliant)
                      • Denominator: 1,500 (eligible)
                      • Compliance Rate: 80.0%
            │
            ├─[40. Emit EvaluationCompletedEvent]──▶ [Kafka: evaluation-results]
                    EvaluationCompletedEvent {
                      evaluationId: "eval-789",
                      measureId: "CDC-HbA1c",
                      numerator: 1200,
                      denominator: 1500,
                      complianceRate: 80.0,
                      completedAt: "2026-01-25T10:18:30Z",
                      tenantId: "org-123"
                    }
            │
            ├─[41. Identify Care Gaps]──▶ [Care Gap Service]
                    Query for non-compliant patients:
                    ```sql
                    SELECT patient_id, details
                    FROM patient_evaluation_results
                    WHERE evaluation_id = 'eval-789'
                      AND result = 'NON_COMPLIANT'
                      AND tenant_id = 'org-123';
                    ```

                    Care Gaps Identified: 300 patients

                    For each:
                      • Determine gap reason (e.g., "No HbA1c test in past year")
                      • Calculate priority score (days overdue, clinical risk)
                      • Generate outreach recommendation
            │
            ├─[42. Emit CareGapIdentifiedEvent (×300)]──▶ [Kafka: care-gap-events]
                    CareGapIdentifiedEvent {
                      gapId: "gap-001",
                      patientId: "pat-042",
                      measureId: "CDC-HbA1c",
                      gapReason: "NO_HBA1C_TEST_PAST_YEAR",
                      priority: "HIGH",  // >90 days overdue
                      recommendedAction: "Schedule HbA1c lab test",
                      dueDate: "2026-02-15",
                      tenantId: "org-123"
                    }
            │
            └─[43. Update Evaluation Projection]──▶ [Evaluation Results Projection]
                    UPDATE evaluations
                    SET status = 'COMPLETED',
                        numerator = 1200,
                        denominator = 1500,
                        compliance_rate = 80.0,
                        care_gaps_identified = 300,
                        completed_at = NOW()
                    WHERE id = 'eval-789'
                      AND tenant_id = 'org-123';

🔒 SECURITY CHECKPOINT #7: Aggregated Results (De-Identified)
   • Action: Evaluation results aggregated, care gaps generated
   • Control: De-identified aggregates (counts only), row-level security
   • Compliance: HIPAA § 164.514(b)(1) - De-Identification (aggregate data safe harbor)
```

**Draw.io Elements:**
- **Aggregation Symbol:** Funnel icon
- **SQL Query:** Code block with SELECT statement
- **Care Gap Alert:** Warning triangle with exclamation
- **Batch Processing:** Multiple event arrows (×300)

---

#### Phase 8: Real-Time Dashboard Update (WebSocket Push)

```
┌─────────────────────────────────────────────────────────────────┐
│ PHASE 8: REAL-TIME DASHBOARD UPDATE                             │
│ Security Controls: WebSocket Authentication, Tenant Filtering   │
└─────────────────────────────────────────────────────────────────┘

[Event Bus: evaluation-results]
    └──[44. Consume EvaluationCompletedEvent]──▶ [Dashboard Update Service]
            │
            ├─[45. Identify Connected Clients]
                    WebSocket Sessions:
                      • user-456@org-123: CONNECTED (requesting user)
                      • user-789@org-123: CONNECTED (same tenant)
                      • user-111@org-999: CONNECTED (different tenant - EXCLUDED)

                    Tenant Filter: org-123
                    Eligible Recipients: [user-456, user-789]
            │
            ├─[46. Push Update via WebSocket]──▶ [Angular Portal (Connected Clients)]
                    WebSocket Message (⚠️ Encrypted TLS 1.3):
                    {
                      "type": "EVALUATION_COMPLETED",
                      "payload": {
                        "evaluationId": "eval-789",
                        "measureName": "Diabetes HbA1c Control",
                        "complianceRate": 80.0,
                        "numerator": 1200,
                        "denominator": 1500,
                        "careGapsIdentified": 300,
                        "completedAt": "2026-01-25T10:18:30Z"
                      }
                    }
            │
            └──[47. Trigger UI Refresh]──▶ [Clinical User Dashboard]
                    Dashboard Updates:
                      ✓ Compliance gauge: 80.0% (animated update)
                      ✓ Care gaps widget: 300 new gaps (red badge)
                      ✓ Recent evaluations list: "CDC HbA1c" added to top
                      ✓ Toast notification: "Evaluation completed: 80% compliance"

🔒 SECURITY CHECKPOINT #8: Real-Time Update with Tenant Isolation
   • Action: Dashboard updated in real-time for authorized users only
   • Control: WebSocket authentication, tenant-based filtering
   • Compliance: HIPAA § 164.312(e)(1) - Transmission Security (TLS 1.3)
```

**Draw.io Elements:**
- **WebSocket Icon:** Lightning bolt with bidirectional arrow
- **Client Filtering:** Decision diamond with tenant check
- **UI Update:** Browser window with animated refresh icon
- **Toast Notification:** Speech bubble with success checkmark

---

### Security Controls Summary Table

Create a table at the bottom of the diagram:

| Layer | Security Control | HIPAA Compliance Reference |
|-------|------------------|----------------------------|
| **Authentication** | JWT HS256 (15-min expiration) | § 164.312(d) - Person or Entity Authentication |
| **Authorization** | RBAC + Row-Level Security | § 164.308(a)(4) - Access Authorization |
| **Data in Transit** | TLS 1.3 (all network calls) | § 164.312(e)(1) - Transmission Security |
| **Data at Rest** | AES-256-GCM (database + cache) | § 164.312(a)(2)(iv) - Encryption |
| **Audit Logging** | Immutable append-only log | § 164.312(b) - Audit Controls |
| **Access Control** | Multi-tenant isolation (tenant_id filter) | § 164.308(a)(3) - Workforce Access Management |
| **Cache Management** | TTL ≤ 2 minutes (PHI), encrypted | § 164.306(e) - Security Awareness |
| **Session Management** | 15-min idle timeout, auto-logout | § 164.312(a)(2)(iii) - Automatic Logoff |

---

### Draw.io Color Scheme

- **Security Layer**: `#FFE082` (amber 200)
- **User Swimlane**: `#C5E1A5` (light green 300)
- **Application Layer**: `#90CAF9` (blue 200)
- **Event Bus**: `#CE93D8` (purple 200)
- **Data Stores**: `#FFAB91` (deep orange 200)
- **PHI Annotations**: `#EF5350` (red 400) with warning icon
- **Encryption Indicators**: `#66BB6A` (green 400) with padlock
- **Audit Trail**: `#FFA726` (orange 400) with document icon

---

## Diagram 2: CQL Evaluation Architecture - Technical Deep Dive

### Overview
Detailed architecture diagram showing CQL engine components, expression parsing, data retrieval, and clinical logic execution flow.

### Diagram Type
Component Diagram + Sequence Flow

### Components

```
┌──────────────────────────────────────────────────────────────────────┐
│                     CQL EVALUATION ARCHITECTURE                       │
│                                                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ CQL LIBRARY REPOSITORY                                         │ │
│  │ ┌────────────┐ ┌────────────┐ ┌────────────┐                  │ │
│  │ │ HEDIS 2025 │ │ HEDIS 2024 │ │ Custom CQL │                  │ │
│  │ │ (NCQA)     │ │ (NCQA)     │ │ Libraries  │                  │ │
│  │ └────────────┘ └────────────┘ └────────────┘                  │ │
│  │                                                                │ │
│  │ 📦 Storage: PostgreSQL cql_libraries table                    │ │
│  │ 🔒 Security: Version control, digital signatures (SHA-256)    │ │
│  │ 📊 Metrics: 50+ HEDIS measures, 200+ custom measures         │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                    │                                 │
│                                    ▼                                 │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ CQL PARSER & COMPILER                                          │ │
│  │                                                                │ │
│  │  CQL Source Code                                              │ │
│  │  ─────────────────                                            │ │
│  │  define "Initial Population":                                 │ │
│  │    ["Patient"] P                                              │ │
│  │      where AgeInYearsAt(...) >= 18                           │ │
│  │                                                                │ │
│  │              │                                                 │ │
│  │              ▼                                                 │ │
│  │  ┌──────────────────────────┐                                │ │
│  │  │ ANTLR Parser             │ Parse CQL grammar              │ │
│  │  │ (CQL 1.5 spec)           │ Generate AST                   │ │
│  │  └──────────────────────────┘                                │ │
│  │              │                                                 │ │
│  │              ▼                                                 │ │
│  │  ┌──────────────────────────┐                                │ │
│  │  │ Expression Tree Builder  │ Build expression tree          │ │
│  │  │                          │ Validate syntax                │ │
│  │  └──────────────────────────┘                                │ │
│  │              │                                                 │ │
│  │              ▼                                                 │ │
│  │  ┌──────────────────────────┐                                │ │
│  │  │ Type Checker             │ Validate data types            │ │
│  │  │                          │ Resolve functions              │ │
│  │  └──────────────────────────┘                                │ │
│  │              │                                                 │ │
│  │              ▼                                                 │ │
│  │  ┌──────────────────────────┐                                │ │
│  │  │ Execution Plan Generator │ Optimize query plan            │ │
│  │  │                          │ Cache compiled plans           │ │
│  │  └──────────────────────────┘                                │ │
│  │                                                                │ │
│  │ ⚡ Performance: ~500ms compile time, cached for 1 hour        │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                    │                                 │
│                                    ▼                                 │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ DATA RETRIEVAL LAYER (FHIR)                                    │ │
│  │                                                                │ │
│  │  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐   │ │
│  │  │ Patient      │───▶│ Observation  │───▶│ Condition    │   │ │
│  │  │ Repository   │    │ Repository   │    │ Repository   │   │ │
│  │  └──────────────┘    └──────────────┘    └──────────────┘   │ │
│  │         │                    │                    │           │ │
│  │         └────────────────────┴────────────────────┘           │ │
│  │                              │                                 │ │
│  │                              ▼                                 │ │
│  │               ┌────────────────────────────┐                  │ │
│  │               │ FHIR Resource Cache        │                  │ │
│  │               │ (Redis)                    │                  │ │
│  │               │ TTL: 120 seconds           │                  │ │
│  │               │ Encryption: AES-256-GCM    │                  │ │
│  │               └────────────────────────────┘                  │ │
│  │                                                                │ │
│  │ 🔐 Security: Row-level security (tenant_id), PHI encryption   │ │
│  │ 📊 Performance: 10-20ms per resource fetch (cached)          │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                    │                                 │
│                                    ▼                                 │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ CQL EXECUTION ENGINE                                           │ │
│  │                                                                │ │
│  │  ┌──────────────────────────────────────────────────────────┐ │ │
│  │  │ Evaluation Context (per patient)                         │ │ │
│  │  │                                                          │ │ │
│  │  │ Context Variables:                                       │ │ │
│  │  │  • Patient: { id: "pat-001", birthDate: "1967-05-15" } │ │ │
│  │  │  • MeasurementPeriod: { start: "2025-01-01", end: ... } │ │ │
│  │  │                                                          │ │ │
│  │  │ Data:                                                    │ │ │
│  │  │  • Observations: [{ value: 7.2, date: "2025-06-15" }] │ │ │
│  │  │  • Conditions: [{ code: "E11.9", status: "active" }]  │ │ │
│  │  └──────────────────────────────────────────────────────────┘ │ │
│  │                              │                                 │ │
│  │                              ▼                                 │ │
│  │  ┌──────────────────────────────────────────────────────────┐ │ │
│  │  │ Expression Evaluator                                     │ │ │
│  │  │                                                          │ │ │
│  │  │ Step 1: Evaluate "Initial Population"                   │ │ │
│  │  │   └─ AgeInYearsAt(MeasurementPeriod.start)             │ │ │
│  │  │      = CalculateAge("1967-05-15", "2025-01-01")        │ │ │
│  │  │      = 57 years ✓ (>= 18 AND < 75)                     │ │ │
│  │  │                                                          │ │ │
│  │  │ Step 2: Evaluate "Numerator"                            │ │ │
│  │  │   └─ exists(Observations where value < 8.0%)           │ │ │
│  │  │      = TRUE (HbA1c 7.2% < 8.0%)                        │ │ │
│  │  │                                                          │ │ │
│  │  │ Step 3: Determine "Result"                              │ │ │
│  │  │   └─ case when Numerator then 'COMPLIANT'              │ │ │
│  │  │      = 'COMPLIANT' ✓                                    │ │ │
│  │  └──────────────────────────────────────────────────────────┘ │ │
│  │                                                                │ │
│  │ ⚙️ Execution: Sandboxed JVM, 30-60 seconds for 1,500 patients│ │
│  │ 🔒 Security: Isolated execution context, no file system access│ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                    │                                 │
│                                    ▼                                 │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ RESULTS AGGREGATION                                            │ │
│  │                                                                │ │
│  │  Individual Results (1,500 patients)                          │ │
│  │  ──────────────────────────────────                           │ │
│  │  pat-001: COMPLIANT (HbA1c 7.2%)                             │ │
│  │  pat-002: NON_COMPLIANT (HbA1c 9.1%)                         │ │
│  │  pat-003: COMPLIANT (HbA1c 6.8%)                             │ │
│  │  ...                                                           │ │
│  │  pat-1500: COMPLIANT (HbA1c 7.5%)                            │ │
│  │                                                                │ │
│  │             │                                                  │ │
│  │             ▼                                                  │ │
│  │  ┌────────────────────────────┐                              │ │
│  │  │ Aggregate Calculator       │                              │ │
│  │  │                            │                              │ │
│  │  │ Numerator:   1,200        │ (COMPLIANT count)            │ │
│  │  │ Denominator: 1,500        │ (Total eligible)             │ │
│  │  │ Rate:        80.0%        │ (1200/1500 * 100)            │ │
│  │  │                            │                              │ │
│  │  │ Stratifications:           │                              │ │
│  │  │  • Age 18-44:  85% (n=500)│                              │ │
│  │  │  • Age 45-64:  78% (n=700)│                              │ │
│  │  │  • Age 65-75:  77% (n=300)│                              │ │
│  │  └────────────────────────────┘                              │ │
│  │                                                                │ │
│  │ 📊 Output: MeasureReport (FHIR R4 format)                     │ │
│  └────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────┘
```

**Draw.io Layout:**
- **Top-to-Bottom Flow**: Library → Parser → Data Retrieval → Execution → Aggregation
- **Component Boxes**: Rounded rectangles with drop shadows
- **Connectors**: Solid arrows for data flow, dashed for configuration
- **Code Blocks**: Monospace font, light gray background
- **Security Badges**: Shield icons with padlocks
- **Performance Metrics**: Stopwatch icons with timing annotations

---

## Diagram 3: Multi-Tenant Data Isolation Architecture

### Overview
Security-focused diagram showing how HDIM enforces strict data isolation between healthcare organizations (tenants) at every layer of the architecture.

### Components

```
┌──────────────────────────────────────────────────────────────────────┐
│            MULTI-TENANT DATA ISOLATION ARCHITECTURE                   │
│                                                                       │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │ TENANT ONBOARDING                                             │  │
│  │                                                                │  │
│  │  Healthcare Organization A         Healthcare Organization B  │  │
│  │  ─────────────────────────         ──────────────────────────│  │
│  │  • Tenant ID: org-acme-123         • Tenant ID: org-health-456│  │
│  │  • Name: ACME Health Partners      • Name: HealthFirst       │  │
│  │  • Domain: acme.hdim.io            • Domain: healthfirst...  │  │
│  │  • Encryption Key: [SHA-256]       • Encryption Key: [SHA-256]│  │
│  │                                                                │  │
│  │  🔐 Database Row: tenants table                               │  │
│  │     ├─ id: org-acme-123                                       │  │
│  │     ├─ name: "ACME Health Partners"                          │  │
│  │     ├─ encryption_key_hash: "abc123..."                      │  │
│  │     ├─ status: ACTIVE                                         │  │
│  │     ├─ created_at: 2025-01-15                                │  │
│  │     └─ allowed_domains: ["acme.hdim.io"]                     │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                │                                      │
│                                ▼                                      │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │ USER AUTHENTICATION (JWT Tokens)                              │  │
│  │                                                                │  │
│  │  User Login: alice@acme.hdim.io                              │  │
│  │  ──────────────────────────────                               │  │
│  │  POST /auth/login                                             │  │
│  │    { "username": "alice@acme.hdim.io", "password": "..." }   │  │
│  │                                                                │  │
│  │              ▼                                                 │  │
│  │  ┌──────────────────────────────────────────────────────┐   │  │
│  │  │ Validate Credentials                                 │   │  │
│  │  │  1. Lookup user by email                            │   │  │
│  │  │  2. Verify bcrypt password hash                     │   │  │
│  │  │  3. Extract tenant_id from user record: org-acme-123│   │  │
│  │  │  4. Load user roles: [ADMIN, EVALUATOR]            │   │  │
│  │  └──────────────────────────────────────────────────────┘   │  │
│  │              │                                                 │  │
│  │              ▼                                                 │  │
│  │  ┌──────────────────────────────────────────────────────┐   │  │
│  │  │ Generate JWT Token (HS256)                          │   │  │
│  │  │                                                      │   │  │
│  │  │ Header:                                             │   │  │
│  │  │   { "alg": "HS256", "typ": "JWT" }                 │   │  │
│  │  │                                                      │   │  │
│  │  │ Payload:                                            │   │  │
│  │  │   {                                                 │   │  │
│  │  │     "sub": "user-alice-789",                       │   │  │
│  │  │     "tenant_id": "org-acme-123",  ⚠️ CRITICAL      │   │  │
│  │  │     "email": "alice@acme.hdim.io",                 │   │  │
│  │  │     "roles": ["ADMIN", "EVALUATOR"],               │   │  │
│  │  │     "iat": 1737828000,                             │   │  │
│  │  │     "exp": 1737828900  // 15-min expiration        │   │  │
│  │  │   }                                                 │   │  │
│  │  │                                                      │   │  │
│  │  │ Signature:                                          │   │  │
│  │  │   HMACSHA256(base64(header) + "." + base64(payload),│   │  │
│  │  │              SECRET_KEY)                            │   │  │
│  │  └──────────────────────────────────────────────────────┘   │  │
│  │                                                                │  │
│  │  ✅ JWT Token Issued: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9... │  │
│  │                                                                │  │
│  │  🔒 Security: Tenant ID embedded in token (tamper-proof)     │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                │                                      │
│                                ▼                                      │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │ API GATEWAY - TENANT VALIDATION                               │  │
│  │                                                                │  │
│  │  Incoming Request:                                            │  │
│  │  GET /api/v1/patients                                         │  │
│  │  Headers:                                                      │  │
│  │    Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9 │  │
│  │    X-Tenant-ID: org-acme-123  ⚠️ User-provided (untrusted)   │  │
│  │                                                                │  │
│  │              ▼                                                 │  │
│  │  ┌──────────────────────────────────────────────────────┐   │  │
│  │  │ TrustedTenantAccessFilter                           │   │  │
│  │  │                                                      │   │  │
│  │  │ Step 1: Verify JWT signature (HS256)               │   │  │
│  │  │   └─ Signature valid? ✓                            │   │  │
│  │  │                                                      │   │  │
│  │  │ Step 2: Extract tenant_id from JWT payload         │   │  │
│  │  │   └─ JWT tenant_id: org-acme-123 (TRUSTED)        │   │  │
│  │  │                                                      │   │  │
│  │  │ Step 3: Compare with X-Tenant-ID header            │   │  │
│  │  │   └─ Header tenant_id: org-acme-123               │   │  │
│  │  │   └─ Match? ✓                                      │   │  │
│  │  │                                                      │   │  │
│  │  │ Step 4: Validate user has access to tenant         │   │  │
│  │  │   └─ Query: SELECT 1 FROM users                    │   │  │
│  │  │             WHERE id = 'user-alice-789'            │   │  │
│  │  │               AND tenant_id = 'org-acme-123';      │   │  │
│  │  │   └─ Result: 1 row (access granted) ✓             │   │  │
│  │  └──────────────────────────────────────────────────────┘   │  │
│  │              │                                                 │  │
│  │              ▼                                                 │  │
│  │  ┌──────────────────────────────────────────────────────┐   │  │
│  │  │ Inject Trusted Headers                              │   │  │
│  │  │                                                      │   │  │
│  │  │ X-Auth-User-ID: user-alice-789                      │   │  │
│  │  │ X-Auth-Tenant-ID: org-acme-123  ⚠️ TRUSTED (signed) │   │  │
│  │  │ X-Auth-Roles: ADMIN,EVALUATOR                       │   │  │
│  │  │ X-Gateway-Trust-Token: [HMAC signature]            │   │  │
│  │  └──────────────────────────────────────────────────────┘   │  │
│  │                                                                │  │
│  │  ✅ Request forwarded to backend with trusted tenant ID      │  │
│  │                                                                │  │
│  │  ❌ BLOCKED SCENARIOS:                                        │  │
│  │     • JWT tenant_id ≠ X-Tenant-ID header                     │  │
│  │     • User not associated with requested tenant              │  │
│  │     • JWT signature invalid or expired                       │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                │                                      │
│                                ▼                                      │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │ DATABASE - ROW-LEVEL SECURITY (PostgreSQL RLS)                │  │
│  │                                                                │  │
│  │  Patient Query Example:                                       │  │
│  │  ─────────────────────                                        │  │
│  │  SELECT * FROM patients WHERE id = 'pat-001';                │  │
│  │                                                                │  │
│  │              ▼                                                 │  │
│  │  ┌──────────────────────────────────────────────────────┐   │  │
│  │  │ PostgreSQL Row-Level Security Policy                 │   │  │
│  │  │                                                      │   │  │
│  │  │ ALTER TABLE patients ENABLE ROW LEVEL SECURITY;     │   │  │
│  │  │                                                      │   │  │
│  │  │ CREATE POLICY tenant_isolation_policy               │   │  │
│  │  │ ON patients                                          │   │  │
│  │  │ FOR ALL                                              │   │  │
│  │  │ USING (tenant_id = current_setting('app.tenant_id'));│   │  │
│  │  │                                                      │   │  │
│  │  │ Session Variable Set by Application:                │   │  │
│  │  │   SET app.tenant_id = 'org-acme-123';              │   │  │
│  │  └──────────────────────────────────────────────────────┘   │  │
│  │              │                                                 │  │
│  │              ▼                                                 │  │
│  │  ┌──────────────────────────────────────────────────────┐   │  │
│  │  │ Query Rewrite by PostgreSQL                         │   │  │
│  │  │                                                      │   │  │
│  │  │ Original Query:                                     │   │  │
│  │  │   SELECT * FROM patients WHERE id = 'pat-001';     │   │  │
│  │  │                                                      │   │  │
│  │  │ Rewritten Query (automatic):                        │   │  │
│  │  │   SELECT * FROM patients                            │   │  │
│  │  │   WHERE id = 'pat-001'                              │   │  │
│  │  │     AND tenant_id = 'org-acme-123';  ⚠️ ENFORCED   │   │  │
│  │  └──────────────────────────────────────────────────────┘   │  │
│  │                                                                │  │
│  │  ✅ Result: Only patients belonging to org-acme-123 returned │  │
│  │  ❌ Cross-tenant data access: IMPOSSIBLE (enforced by DB)    │  │
│  │                                                                │  │
│  │  📊 All Tables with RLS:                                      │  │
│  │     • patients, observations, conditions, procedures          │  │
│  │     • evaluations, care_gaps, reports, audit_logs           │  │
│  │     • event_store, projections, fhir_resources                │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                │                                      │
│                                ▼                                      │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │ EVENT BUS - TOPIC PARTITIONING (Apache Kafka)                 │  │
│  │                                                                │  │
│  │  Event Publication:                                           │  │
│  │  PatientCreatedEvent {                                        │  │
│  │    patientId: "pat-001",                                      │  │
│  │    tenantId: "org-acme-123"  ⚠️ Partition Key                │  │
│  │  }                                                             │  │
│  │                                                                │  │
│  │              ▼                                                 │  │
│  │  ┌──────────────────────────────────────────────────────┐   │  │
│  │  │ Kafka Partition Assignment                           │   │  │
│  │  │                                                      │   │  │
│  │  │ Hash Function:                                       │   │  │
│  │  │   partition = hash(tenantId) % numPartitions        │   │  │
│  │  │   partition = hash("org-acme-123") % 10             │   │  │
│  │  │   partition = 3                                      │   │  │
│  │  │                                                      │   │  │
│  │  │ Topic: patient-events                                │   │  │
│  │  │  ├─ Partition 0 (org-xyz-999 events)                │   │  │
│  │  │  ├─ Partition 1 (org-def-456 events)                │   │  │
│  │  │  ├─ Partition 2 (org-ghi-789 events)                │   │  │
│  │  │  ├─ Partition 3 (org-acme-123 events)  ⚠️ TARGET    │   │  │
│  │  │  ├─ Partition 4 (org-jkl-012 events)                │   │  │
│  │  │  └─ ...                                              │   │  │
│  │  └──────────────────────────────────────────────────────┘   │  │
│  │                                                                │  │
│  │  ✅ Tenant Isolation: Events partitioned by tenant_id        │  │
│  │  ⚡ Performance: Parallel processing per tenant               │  │
│  │  🔒 Security: Consumer filters by tenant_id before processing│  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                │                                      │
│                                ▼                                      │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │ CACHE - TENANT-SCOPED KEYS (Redis)                            │  │
│  │                                                                │  │
│  │  Cache Key Strategy:                                          │  │
│  │  ───────────────────                                          │  │
│  │  Key Format: {tenant_id}:{resource_type}:{resource_id}       │  │
│  │                                                                │  │
│  │  Examples:                                                     │  │
│  │    org-acme-123:patient:pat-001  ⚠️ ACME Health data         │  │
│  │    org-health-456:patient:pat-042  ⚠️ HealthFirst data       │  │
│  │                                                                │  │
│  │              ▼                                                 │  │
│  │  ┌──────────────────────────────────────────────────────┐   │  │
│  │  │ Cache Lookup Logic                                   │   │  │
│  │  │                                                      │   │  │
│  │  │ Request: GET patient pat-001 (tenant: org-acme-123) │   │  │
│  │  │                                                      │   │  │
│  │  │ Cache Key: org-acme-123:patient:pat-001             │   │  │
│  │  │ Cache Hit? ✓                                        │   │  │
│  │  │ TTL Remaining: 90 seconds                           │   │  │
│  │  │ Encrypted Value: [AES-256-GCM ciphertext]           │   │  │
│  │  │   └─ Decrypt with tenant-specific key              │   │  │
│  │  │   └─ Return patient data                            │   │  │
│  │  └──────────────────────────────────────────────────────┘   │  │
│  │                                                                │  │
│  │  ❌ IMPOSSIBLE SCENARIOS:                                     │  │
│  │     • Tenant A accessing cache key for Tenant B              │  │
│  │     • Cache key collisions between tenants                   │  │
│  │                                                                │  │
│  │  🔐 Security: Tenant-specific encryption keys                │  │
│  │  📊 TTL: 120 seconds max (HIPAA compliant)                   │  │
│  └───────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────┘
```

**Security Layers Summary:**

| Layer | Isolation Mechanism | Enforcement Point |
|-------|-------------------|-------------------|
| **Authentication** | JWT `tenant_id` claim (signed) | Gateway validates signature |
| **Authorization** | User-tenant association check | Gateway queries user table |
| **Database** | PostgreSQL Row-Level Security (RLS) | Database enforces automatically |
| **Event Bus** | Kafka partition by `tenant_id` | Consumer filters events |
| **Cache** | Tenant-scoped keys + encryption | Application enforces key format |
| **Audit Logs** | `tenant_id` indexed column | Application filters queries |

---

**Draw.io Color Coding:**
- **Tenant A**: `#4CAF50` (green)
- **Tenant B**: `#2196F3` (blue)
- **Security Controls**: `#FF9800` (orange) with shield icons
- **Critical Points**: `#F44336` (red) with warning triangles
- **Enforcement Layers**: `#9C27B0` (purple) with lock icons

---

## Implementation Checklist

To create these diagrams in Draw.io:

### General Setup
- [ ] Use A3 landscape (420mm × 297mm) for large diagrams
- [ ] Enable grid (View → Grid → Show Grid)
- [ ] Enable snap to grid (View → Grid → Snap to Grid)
- [ ] Set grid size to 10px for precision

### Swimlane Diagrams (Diagram 1)
- [ ] Use Swimlane template (More Shapes → Flowchart → Swimlane)
- [ ] Create 8 horizontal swimlanes (User, Web, Gateway, Services, Bus, Handlers, Data, Security)
- [ ] Use connectors with arrows (not manual lines)
- [ ] Add labels to every connector
- [ ] Use decision diamonds for security checkpoints
- [ ] Add security shield icons from icon library

### Component Diagrams (Diagram 2)
- [ ] Use UML Component template (More Shapes → UML → Component)
- [ ] Group related components in containers
- [ ] Use dashed lines for configuration flows
- [ ] Use solid lines for data flows
- [ ] Add timing annotations with stopwatch icons
- [ ] Color-code by performance tier (green fast, yellow medium, red slow)

### Security Diagrams (Diagram 3)
- [ ] Use color coding for different tenants
- [ ] Add warning icons (⚠️) for sensitive data
- [ ] Add lock icons (🔒) for security controls
- [ ] Use code blocks for SQL/JSON examples
- [ ] Highlight critical enforcement points in red boxes
- [ ] Add summary tables at bottom

### Export Settings
- [ ] Export as PDF (high quality, 300 DPI)
- [ ] Export as PNG (transparent background, 2x scale)
- [ ] Export as SVG (for web embedding)
- [ ] Save Draw.io source (.drawio or .xml) for future edits

---

## Next Steps

1. **Create Diagrams**: Use this specification to build professional diagrams in Draw.io
2. **Review with Team**: Share with security, compliance, and engineering teams
3. **Update Documentation**: Add diagrams to `/docs/architecture/diagrams/`
4. **Training Materials**: Use diagrams in onboarding presentations
5. **Compliance Audits**: Reference diagrams in HIPAA audit documentation

---

**Document Version**: 1.0
**Last Updated**: January 25, 2026
**Author**: HDIM Architecture Team
**Review Cycle**: Quarterly
**Classification**: Internal Technical Documentation
