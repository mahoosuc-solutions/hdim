# HDIM — Integration & Interoperability Guide

*For integration engineers, technical partners, and EHR vendors evaluating connectivity.*

---

## Integration Philosophy

HDIM is designed to fit into existing healthcare data ecosystems — not replace them. Every capability is accessible via standards-based APIs. The platform consumes standard clinical data (FHIR R4) and produces standard quality outputs (QRDA I/III, CDS Hooks).

**Core principle:** If your system speaks FHIR, integration takes weeks. If it doesn't, we provide the transformation layer.

---

## Integration Patterns

### Pattern 1: FHIR API Integration (Recommended)

The simplest and most powerful integration. HDIM exposes a full **FHIR R4 resource server** and consumes FHIR data natively.

```
┌────────────────┐        FHIR R4 REST        ┌─────────────────┐
│                │  ──────────────────────────► │                 │
│   Your System  │        POST /fhir/Patient   │   HDIM          │
│   (EHR, HIE,   │  ◄────────────────────────  │   FHIR Service  │
│    Data Lake)  │        GET /fhir/Patient/123 │   :8085         │
│                │                              │                 │
└────────────────┘                              └─────────────────┘
```

**Supported FHIR Resources (20+):**

| Resource | Use in HDIM |
|----------|-------------|
| `Patient` | Demographics, eligibility |
| `Observation` | Labs (HbA1c, BP, cholesterol), vitals |
| `Condition` | Diagnoses (diabetes, hypertension) |
| `Procedure` | Screenings (mammogram, colonoscopy, pap) |
| `MedicationRequest` | Prescriptions (statins, antihypertensives) |
| `MedicationAdministration` | Medication administration records |
| `Immunization` | Vaccination history |
| `Encounter` | Visit history |
| `Coverage` | Insurance coverage (plan, member ID) |
| `AllergyIntolerance` | Allergy documentation |
| `DiagnosticReport` | Diagnostic results |
| `CarePlan` | Active care plans |
| `Goal` | Patient health goals |
| `Task` | Clinical tasks and follow-ups |
| `Appointment` | Scheduled appointments |
| `DocumentReference` | Clinical documents |
| `Practitioner` | Provider demographics |
| `PractitionerRole` | Provider specialties and affiliations |
| `Organization` | Organization hierarchy |

**Content type:** `application/fhir+json`

**CapabilityStatement:** `GET /fhir/metadata` returns the full server capability declaration.

---

### Pattern 2: Bulk Data Import

For initial data loads or periodic batch synchronization.

```
┌────────────────┐                           ┌─────────────────┐
│  Data Source    │  FHIR $export / ndjson    │  HDIM Data      │
│  (Claims DB,   │ ────────────────────────►  │  Ingestion      │
│   EHR export,  │                            │  Service        │
│   Flat files)  │                            │                 │
└────────────────┘                            └─────────────────┘
```

**Supported formats:**
- FHIR Bulk Data Export (`$export` → NDJSON)
- FHIR Bundles (JSON)
- Custom data feeds via Data Ingestion Service

**Recommended workflow:**
1. Export clinical data from source system as FHIR Bundles
2. POST bundles to HDIM's FHIR Service
3. HDIM normalizes and stores as native FHIR resources
4. CQL evaluation runs against loaded data

---

### Pattern 3: Event-Driven Integration (Kafka)

For real-time streaming from EHRs, ADT feeds, or lab systems.

```
┌────────────────┐                           ┌─────────────────┐
│  HL7v2/ADT     │  Kafka / FHIR Events      │  HDIM Event     │
│  Feed / EHR    │ ────────────────────────►  │  Bridge         │
│  Integration   │                            │  Service        │
│  Engine        │                            │                 │
└────────────────┘                            └─────────────────┘
```

HDIM's **FHIR Event Bridge** accepts clinical events via Kafka and translates them to FHIR resources for real-time evaluation. This enables:

- ADT messages → Patient/Encounter resources → immediate gap re-evaluation
- Lab results (ORU/ORM) → Observation resources → measure compliance check
- Medication orders → MedicationRequest resources → adherence tracking

**Kafka topics** are partitioned by tenant for isolation.

---

### Pattern 4: CDS Hooks (EHR-Embedded Alerts)

HDIM implements the [CDS Hooks](https://cds-hooks.hl7.org/) standard for embedding quality alerts directly into EHR workflows.

```
┌────────────────┐       CDS Hooks            ┌─────────────────┐
│  EHR System    │  ──────────────────────────► │  HDIM Quality   │
│  (Epic, Cerner,│  Hook: patient-view         │  Measure Service│
│   athena)      │  ◄────────────────────────  │                 │
│                │  Cards: care gap alerts      │  /cds-services  │
└────────────────┘                              └─────────────────┘
```

**Supported hooks:**
- `patient-view`: Triggered when clinician opens patient chart → returns open care gaps
- `order-select`: Triggered on order entry → returns measure-relevant recommendations

**Response format:** CDS Cards with:
- Gap description and clinical rationale
- Suggested actions (order screenings, schedule follow-ups)
- Links to detailed measure information
- Priority indicators (critical, warning, info)

---

### Pattern 5: QRDA Export (CMS Reporting)

HDIM generates standard CMS quality reporting documents.

| Format | Use Case |
|--------|----------|
| **QRDA Category I** | Individual patient-level quality data (per-patient XML) |
| **QRDA Category III** | Aggregate population-level quality data (summary XML) |

**Workflow:**
1. Run quality evaluations across target population
2. Export results as QRDA I (individual) or QRDA III (aggregate)
3. Submit to CMS, NCQA, or state reporting agencies

---

### Pattern 6: SMART on FHIR (Third-Party Apps)

HDIM supports **SMART on FHIR** for launching third-party applications within the HDIM context.

| Endpoint | Purpose |
|----------|---------|
| `/fhir/.well-known/smart-configuration` | SMART configuration discovery |
| `/fhir/authorize` | Authorization endpoint |
| `/fhir/token` | Token endpoint |

This enables partner applications to:
- Launch within HDIM's clinical portal context
- Access FHIR resources with appropriate scopes
- Single sign-on via the same JWT infrastructure

---

## API Reference Summary

### Authentication

All API calls require authentication via JWT tokens.

**Login:**
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@org.com",
  "password": "..."
}

→ Set-Cookie: hdim_access_token=<JWT>; HttpOnly; Secure; SameSite=Strict
→ Set-Cookie: hdim_refresh_token=<JWT>; HttpOnly; Secure; SameSite=Strict
```

**Tenant context:**
```http
GET /care-gap/open
X-Tenant-ID: acme-health
Cookie: hdim_access_token=<JWT>
```

Every API call requires the `X-Tenant-ID` header to scope the request.

### Key API Endpoints

#### Patient Data

```http
# Complete patient health record (FHIR Bundle)
GET /patient/health-record?patientId={id}

# Patient timeline
GET /patient/timeline?patientId={id}

# Risk assessment
GET /patient/risk-assessment/{patientId}

# Provider panel
GET /api/v1/providers/{providerId}/panel

# Pre-visit planning
GET /api/v1/providers/{providerId}/pre-visit-plan/{patientId}
```

#### CQL Evaluation

```http
# Single patient evaluation
POST /api/v1/cql/evaluations
Content-Type: application/json

{
  "libraryId": "diabetes-care-cql",
  "patientId": "patient-123",
  "parameters": {}
}

# Batch evaluation
POST /api/v1/cql/evaluations/batch
Content-Type: application/json

{
  "libraryId": "diabetes-care-cql",
  "patientIds": ["patient-123", "patient-456", "patient-789"]
}
```

#### Care Gap Management

```http
# Identify all gaps for a patient
POST /care-gap/identify
Content-Type: application/json

{
  "patientId": "patient-123"
}

# Close a gap with documentation
POST /care-gap/close
Content-Type: application/json

{
  "gapId": "gap-001",
  "closureReason": "Screening completed",
  "interventionType": "MAMMOGRAM_ORDERED",
  "documentation": "Bilateral mammogram performed 2026-02-15"
}

# Population report
GET /care-gap/population-report?category=HEDIS

# Bulk operations
POST /care-gap/bulk-close
POST /care-gap/bulk-assign-intervention
PUT /care-gap/bulk-update-priority
```

#### Quality Measures

```http
# Browse measure registry
GET /measures

# Local HEDIS calculation
GET /quality-measure/calculate-local?measureId=BCS&patientId={id}

# Patient health score
GET /patients/{id}/health-score

# Provider performance
GET /api/v1/providers/{id}/performance

# AI-assisted measure generation
POST /api/v1/measures/ai/generate
Content-Type: application/json

{
  "description": "Patients with diabetes who had an eye exam in the last year"
}
```

#### FHIR Resources

```http
# Standard FHIR operations
GET /fhir/Patient/{id}
POST /fhir/Patient
PUT /fhir/Patient/{id}
DELETE /fhir/Patient/{id}
GET /fhir/Patient?name=Smith&birthdate=1960-01-15

# Bulk export
GET /fhir/$export

# CapabilityStatement
GET /fhir/metadata
```

---

## Data Requirements

### Minimum Data for Quality Measurement

To evaluate HEDIS measures, HDIM needs:

| Data Type | FHIR Resource | Example |
|-----------|---------------|---------|
| **Demographics** | Patient | Age, gender (for eligibility) |
| **Diagnoses** | Condition | Diabetes (E11.*), Hypertension (I10) |
| **Lab results** | Observation | HbA1c, blood pressure, LDL cholesterol |
| **Procedures** | Procedure | Mammogram, colonoscopy, pap smear |
| **Medications** | MedicationRequest | Statin prescriptions, insulin |
| **Encounters** | Encounter | Visit dates, types |
| **Insurance** | Coverage | Plan type, member ID |

### Data Quality Expectations

| Dimension | Requirement |
|-----------|-------------|
| **Coding** | ICD-10-CM, CPT, LOINC, RxNorm, SNOMED CT |
| **Completeness** | Demographics + at least one clinical resource type |
| **Timeliness** | Real-time preferred; batch within 24 hours acceptable |
| **Format** | FHIR R4 JSON (preferred) or transformable to FHIR |

---

## Integration Timeline

| Phase | Activities | Duration |
|-------|-----------|----------|
| **Discovery** | Map source data to FHIR resources, identify available APIs | 1–2 weeks |
| **Connectivity** | Establish FHIR endpoints, configure authentication | 1–2 weeks |
| **Data Load** | Initial bulk load + validation | 1–2 weeks |
| **Validation** | Run CQL evaluations, verify results against known benchmarks | 1–2 weeks |
| **Go-Live** | Enable real-time feeds, configure CDS Hooks, train users | 1–2 weeks |

**Total: 5–10 weeks** for FHIR-native sources. Add 2–4 weeks for data transformation if source doesn't support FHIR.

---

## Partner Integration Scenarios

### Scenario A: MDM Platform (e.g., Verato)

```
Verato CIEMA Pipeline:
  Connect → Identify → Enrich → Manage → [Gap: Activate]

With HDIM:
  Connect → Identify → Enrich → Manage → HDIM evaluates → Activate with quality data
```

Verato produces a unified patient record. HDIM evaluates quality measures against that record. The combined output is a patient identity + their real-time quality status.

### Scenario B: EHR Integration (CDS Hooks)

```
Clinician opens patient chart in Epic/Cerner
  → Epic fires patient-view CDS Hook to HDIM
  → HDIM returns care gap cards
  → Clinician sees: "BCS: Mammogram overdue (last: 2024-01-15)"
  → Clinician orders screening from within the EHR
```

### Scenario C: Health Plan Analytics Platform (Arcadia, Databricks)

```
Platform aggregates member data → HDIM CQL Engine evaluates measures
  → Results flow back to platform dashboards
  → Platform triggers outreach workflows for open gaps
```

### Scenario D: ACO Enablement (Aledade, Pearl Health)

```
ACO platform manages provider network
  → HDIM evaluates quality scores per provider panel
  → Provider sees pre-visit gap list in ACO portal
  → Gap closures reported back to HDIM
  → ACO tracks MSSP quality performance in real-time
```

---

## Environments

| Environment | URL | Auth | Purpose |
|-------------|-----|------|---------|
| **Demo** | https://healthdatainmotion.com | Pre-seeded demo accounts | Evaluation and demonstrations |
| **Sandbox** | Provisioned per partner | Partner API keys | Integration development |
| **Production** | Customer-specific | SSO / JWT | Live deployment |

---

*HealthData-in-Motion | https://healthdatainmotion.com | February 2026*
