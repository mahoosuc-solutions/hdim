# HDIM EHR Integration Roadmap

**Connecting to Every Major Electronic Health Record**

*Version 1.0 | December 2025*

---

## Executive Summary

HDIM is built on FHIR R4, the modern healthcare interoperability standard. Our integration strategy prioritizes the EHRs most used by our target customers: small-to-mid-size healthcare organizations.

**Current State:**
- ✅ FHIR R4 native architecture
- ✅ Bulk FHIR import
- ✅ Manual data upload (CSV, Excel)
- ✅ API for custom integrations

**2025-2026 Roadmap:**
- Q1 2025: Epic (App Orchard)
- Q2 2025: Cerner (CODE Program)
- Q3 2025: athenahealth
- Q4 2025: eClinicalWorks
- 2026: AllScripts, NextGen, Greenway, DrChrono

---

## 1. Integration Architecture

### 1.1 Current Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         HDIM INTEGRATION LAYER                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                        DATA SOURCES                                 │   │
│   │                                                                     │   │
│   │   ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐   │   │
│   │   │  EHRs   │  │  Labs   │  │ Claims  │  │   HIE   │  │ Manual  │   │   │
│   │   │         │  │         │  │         │  │         │  │ Upload  │   │   │
│   │   └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘   │   │
│   │        │            │            │            │            │        │   │
│   └────────┼────────────┼────────────┼────────────┼────────────┼────────┘   │
│            │            │            │            │            │            │
│            ▼            ▼            ▼            ▼            ▼            │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                    INTEGRATION HUB                                  │   │
│   │                                                                     │   │
│   │   ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐       │   │
│   │   │   FHIR    │  │   HL7v2   │  │   CSV/    │  │  Custom   │       │   │
│   │   │   R4      │  │  (Future) │  │   Excel   │  │   API     │       │   │
│   │   └───────────┘  └───────────┘  └───────────┘  └───────────┘       │   │
│   │                                                                     │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                    FHIR R4 DATA STORE                               │   │
│   │                                                                     │   │
│   │   Patient │ Observation │ Condition │ Procedure │ Medication │ ...  │   │
│   │                                                                     │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                    CQL EVALUATION ENGINE                            │   │
│   │                    (Real-time Quality Measurement)                  │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 FHIR Resources Supported

| Resource | Status | Used For |
|----------|--------|----------|
| **Patient** | ✅ Supported | Demographics, attribution |
| **Observation** | ✅ Supported | Vitals, labs, screenings |
| **Condition** | ✅ Supported | Diagnoses, problem list |
| **Procedure** | ✅ Supported | Procedures performed |
| **MedicationRequest** | ✅ Supported | Prescriptions |
| **MedicationDispense** | ✅ Supported | Pharmacy fills |
| **Immunization** | ✅ Supported | Vaccines administered |
| **Encounter** | ✅ Supported | Visits, admissions |
| **DiagnosticReport** | ✅ Supported | Lab results, imaging |
| **Coverage** | ✅ Supported | Insurance information |
| **Claim** | 🔄 Planned | Claims data |
| **ExplanationOfBenefit** | 🔄 Planned | Adjudicated claims |

---

## 2. EHR Market Analysis

### 2.1 EHR Market Share (Target Segments)

| EHR | Ambulatory Share | Hospital Share | Our Priority |
|-----|------------------|----------------|--------------|
| **Epic** | 31% | 38% | High |
| **Oracle Cerner** | 12% | 25% | High |
| **athenahealth** | 10% | 3% | High |
| **eClinicalWorks** | 8% | 1% | Medium |
| **Meditech** | 4% | 15% | Medium |
| **AllScripts** | 5% | 5% | Medium |
| **NextGen** | 5% | 1% | Medium |
| **Greenway** | 3% | <1% | Medium |
| **DrChrono** | 2% | <1% | Medium |
| **Practice Fusion** | 2% | <1% | Low |
| **Other** | 18% | 11% | FHIR Generic |

### 2.2 Target Customer EHR Usage

| Customer Segment | Primary EHRs |
|------------------|--------------|
| **Small Practices** | athenahealth, eClinicalWorks, DrChrono |
| **FQHCs** | athenahealth, eClinicalWorks, NextGen, Greenway |
| **Rural Hospitals** | Meditech, CPSI, Cerner |
| **Small ACOs** | Epic, Cerner, athenahealth |
| **Mid-size ACOs** | Epic, Cerner |
| **IPAs** | Multiple (aggregation needed) |

---

## 3. Integration Roadmap

### 3.1 Timeline Overview

```
2025                                    2026
──────────────────────────────────────────────────────────────────────────────
Q1        Q2        Q3        Q4        Q1        Q2        Q3        Q4
│         │         │         │         │         │         │         │
├── Epic  │         │         │         │         │         │         │
│   App   ├── Cerner│         │         │         │         │         │
│   Orchard   CODE  ├── athena│         │         │         │         │
│         │   Program   health├── eCW   │         │         │         │
│         │         │         │  ├────────── AllScripts    │         │
│         │         │         │         ├────────── NextGen│         │
│         │         │         │         │         ├── Greenway       │
│         │         │         │         │         │  ├── DrChrono    │
│         │         │         │         │         │         ├── Meditech
│         │         │         │         │         │         │         │
──────────────────────────────────────────────────────────────────────────────
```

### 3.2 Detailed Roadmap

#### Phase 1: Q1 2025 — Epic

| Milestone | Target Date | Status |
|-----------|-------------|--------|
| App Orchard application | January 2025 | 📋 Planned |
| Development environment | January 2025 | 📋 Planned |
| SMART on FHIR implementation | February 2025 | 📋 Planned |
| Epic testing & certification | March 2025 | 📋 Planned |
| App Orchard listing | March 2025 | 📋 Planned |

**Integration Approach:**
- SMART on FHIR (OAuth 2.0)
- Epic FHIR R4 APIs
- Bulk FHIR for historical data
- Real-time webhooks for updates

**Epic Capabilities:**
| Feature | Support |
|---------|---------|
| Patient demographics | ✅ |
| Encounters | ✅ |
| Conditions/Problems | ✅ |
| Medications | ✅ |
| Lab results | ✅ |
| Vitals | ✅ |
| Immunizations | ✅ |
| Procedures | ✅ |
| Bulk export | ✅ |

---

#### Phase 2: Q2 2025 — Oracle Cerner

| Milestone | Target Date | Status |
|-----------|-------------|--------|
| CODE Program enrollment | April 2025 | 📋 Planned |
| Cerner sandbox access | April 2025 | 📋 Planned |
| SMART on FHIR implementation | May 2025 | 📋 Planned |
| Cerner validation | June 2025 | 📋 Planned |
| CODE marketplace listing | June 2025 | 📋 Planned |

**Integration Approach:**
- Cerner FHIR R4 (Ignite APIs)
- SMART on FHIR authorization
- Millennium Data Services (as fallback)

**Cerner Capabilities:**
| Feature | Support |
|---------|---------|
| Patient demographics | ✅ |
| Encounters | ✅ |
| Conditions | ✅ |
| Medications | ✅ |
| Lab results | ✅ |
| Vitals | ✅ |
| Procedures | ✅ |
| Documents | ✅ |

---

#### Phase 3: Q3 2025 — athenahealth

| Milestone | Target Date | Status |
|-----------|-------------|--------|
| Marketplace application | July 2025 | 📋 Planned |
| API access approval | July 2025 | 📋 Planned |
| Integration development | August 2025 | 📋 Planned |
| Testing & certification | September 2025 | 📋 Planned |
| Marketplace listing | September 2025 | 📋 Planned |

**Integration Approach:**
- athenahealth REST APIs
- FHIR R4 (where available)
- OAuth 2.0 authorization
- Webhook subscriptions

**athenahealth Capabilities:**
| Feature | Support |
|---------|---------|
| Patient demographics | ✅ |
| Appointments/Encounters | ✅ |
| Clinical data | ✅ |
| Lab results | ✅ |
| Medications | ✅ |
| Quality measures (native) | Partial |

---

#### Phase 4: Q4 2025 — eClinicalWorks

| Milestone | Target Date | Status |
|-----------|-------------|--------|
| Partner program enrollment | October 2025 | 📋 Planned |
| API documentation review | October 2025 | 📋 Planned |
| Integration development | November 2025 | 📋 Planned |
| Testing | December 2025 | 📋 Planned |
| Production release | December 2025 | 📋 Planned |

**Integration Approach:**
- eCW FHIR APIs
- eCW proprietary APIs (where needed)
- HL7v2 interfaces (legacy option)

---

#### Phase 5: 2026 — Expanded Coverage

| EHR | Target Quarter | Approach |
|-----|----------------|----------|
| **AllScripts** | Q1 2026 | Sunrise APIs, FHIR |
| **NextGen** | Q1-Q2 2026 | NextGen Connect, FHIR |
| **Greenway** | Q2 2026 | Greenway APIs |
| **DrChrono** | Q2-Q3 2026 | REST APIs |
| **Meditech** | Q3-Q4 2026 | Meditech FHIR |
| **CPSI** | Q4 2026 | CPSI APIs |

---

## 4. Integration Methods

### 4.1 SMART on FHIR (Preferred)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         SMART ON FHIR FLOW                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌──────────┐      ┌──────────┐      ┌──────────┐      ┌──────────┐       │
│   │   User   │      │   HDIM   │      │   EHR    │      │   FHIR   │       │
│   │          │      │          │      │  Auth    │      │  Server  │       │
│   └────┬─────┘      └────┬─────┘      └────┬─────┘      └────┬─────┘       │
│        │                 │                 │                 │              │
│        │  1. Launch      │                 │                 │              │
│        ├────────────────►│                 │                 │              │
│        │                 │                 │                 │              │
│        │                 │  2. Auth Request│                 │              │
│        │                 ├────────────────►│                 │              │
│        │                 │                 │                 │              │
│        │       3. Login/Consent            │                 │              │
│        │◄──────────────────────────────────┤                 │              │
│        │                 │                 │                 │              │
│        │  4. Approve     │                 │                 │              │
│        ├──────────────────────────────────►│                 │              │
│        │                 │                 │                 │              │
│        │                 │  5. Auth Code   │                 │              │
│        │                 │◄────────────────┤                 │              │
│        │                 │                 │                 │              │
│        │                 │  6. Token       │                 │              │
│        │                 ├────────────────►│                 │              │
│        │                 │◄────────────────┤                 │              │
│        │                 │                 │                 │              │
│        │                 │          7. FHIR API Calls        │              │
│        │                 ├──────────────────────────────────►│              │
│        │                 │◄──────────────────────────────────┤              │
│        │                 │                 │                 │              │
│        │  8. Data in HDIM│                 │                 │              │
│        │◄────────────────┤                 │                 │              │
│        │                 │                 │                 │              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Advantages:**
- Industry standard (ONC mandated)
- No custom development per EHR
- Patient-authorized access
- Automatic updates via 21st Century Cures

**Requirements:**
- EHR must support SMART on FHIR
- Customer must enable HDIM app
- One-time authorization per user/location

---

### 4.2 Bulk FHIR Export

For historical data and batch synchronization:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         BULK FHIR EXPORT                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   1. HDIM initiates bulk export request                                     │
│      POST /Patient/$export                                                  │
│      Parameters: _type=Patient,Observation,Condition,...                    │
│                                                                             │
│   2. EHR returns job status URL                                             │
│      202 Accepted                                                           │
│      Content-Location: /jobs/12345                                          │
│                                                                             │
│   3. HDIM polls for completion                                              │
│      GET /jobs/12345                                                        │
│      → 202 (in progress) or 200 (complete)                                  │
│                                                                             │
│   4. Download NDJSON files                                                  │
│      GET /output/Patient.ndjson                                             │
│      GET /output/Observation.ndjson                                         │
│      ...                                                                    │
│                                                                             │
│   5. Process and import to HDIM                                             │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Use Cases:**
- Initial data load (historical)
- Nightly batch sync
- Large population updates

---

### 4.3 Manual Data Import

For organizations without API access:

| Format | Supported | Use Case |
|--------|-----------|----------|
| **CSV** | ✅ | Simple patient lists, care gaps |
| **Excel** | ✅ | Complex data with multiple sheets |
| **FHIR Bundle (JSON)** | ✅ | Standard FHIR export files |
| **CCDA (XML)** | ✅ | Clinical document exchange |
| **HL7v2** | 🔄 Planned | Legacy interfaces |

**CSV Templates Available:**
- Patient demographics
- Conditions/Diagnoses
- Medications
- Lab results
- Vitals
- Immunizations
- Encounters

---

### 4.4 Custom API Integration

For customers with existing data warehouses:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         HDIM INBOUND API                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   Endpoint: POST /api/v1/fhir/Bundle                                        │
│                                                                             │
│   Authentication: Bearer token (API key)                                    │
│                                                                             │
│   Rate Limit: 1,000 requests/minute (Enterprise+)                           │
│                                                                             │
│   Payload: FHIR R4 Bundle (transaction or batch)                            │
│                                                                             │
│   Example:                                                                  │
│   {                                                                         │
│     "resourceType": "Bundle",                                               │
│     "type": "transaction",                                                  │
│     "entry": [                                                              │
│       {                                                                     │
│         "resource": {                                                       │
│           "resourceType": "Patient",                                        │
│           "id": "patient-123",                                              │
│           "name": [{"given": ["John"], "family": "Doe"}],                   │
│           ...                                                               │
│         }                                                                   │
│       },                                                                    │
│       ...                                                                   │
│     ]                                                                       │
│   }                                                                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Integration by EHR

### 5.1 Epic Integration

| Aspect | Details |
|--------|---------|
| **Program** | Epic App Orchard |
| **API Type** | FHIR R4, SMART on FHIR |
| **Authorization** | OAuth 2.0 |
| **Certification** | Epic App Orchard Review |
| **Timeline** | Q1 2025 |

**Epic-Specific Features:**
- MyChart patient access (optional)
- Epic CDS Hooks integration (future)
- Epic Care Gaps interoperability

**Customer Requirements:**
- Epic 2020 or later
- FHIR enabled
- App Orchard app approval by Epic admin

---

### 5.2 Cerner Integration

| Aspect | Details |
|--------|---------|
| **Program** | Cerner CODE Program |
| **API Type** | Cerner Ignite (FHIR R4) |
| **Authorization** | OAuth 2.0 |
| **Certification** | CODE validation |
| **Timeline** | Q2 2025 |

**Cerner-Specific Features:**
- PowerChart integration
- Cerner Millennium data access
- Real-time notifications

**Customer Requirements:**
- Cerner Millennium
- Ignite APIs enabled
- CODE app approval

---

### 5.3 athenahealth Integration

| Aspect | Details |
|--------|---------|
| **Program** | athenahealth Marketplace |
| **API Type** | athenaClinicals API, FHIR |
| **Authorization** | OAuth 2.0 |
| **Certification** | Marketplace review |
| **Timeline** | Q3 2025 |

**athenahealth-Specific Features:**
- athenaCollector integration
- Quality reporting alignment
- Patient outreach integration

**Customer Requirements:**
- athenaOne platform
- API access enabled
- Marketplace app approval

---

### 5.4 Generic FHIR (Any Certified EHR)

For EHRs with ONC-certified FHIR APIs:

| Aspect | Details |
|--------|---------|
| **API Type** | FHIR R4 (USCDI) |
| **Authorization** | SMART on FHIR |
| **Certification** | None required (standard) |
| **Timeline** | Available now |

**Supported Under 21st Century Cures:**
- All ONC-certified EHRs must provide FHIR access
- Patients can authorize third-party apps
- Information blocking prohibited

---

## 6. Data Synchronization

### 6.1 Sync Frequency Options

| Method | Frequency | Latency | Best For |
|--------|-----------|---------|----------|
| **Real-time** | Immediate | <1 minute | Care gap alerts |
| **Near real-time** | Every 15 min | 15 minutes | Active care management |
| **Hourly** | Every hour | 1 hour | Standard operations |
| **Nightly** | Once daily | 24 hours | Reporting, batch |
| **On-demand** | Manual trigger | Variable | Initial load, refresh |

### 6.2 Sync Configuration

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    SYNC CONFIGURATION OPTIONS                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   Data Types to Sync:                                                       │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │ ☑ Patient Demographics     ☑ Conditions/Diagnoses                  │   │
│   │ ☑ Encounters               ☑ Medications                           │   │
│   │ ☑ Lab Results              ☑ Vitals                                │   │
│   │ ☑ Immunizations            ☑ Procedures                            │   │
│   │ ☐ Documents (optional)     ☐ Notes (optional)                      │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│   Sync Frequency:                                                           │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │ ○ Real-time (webhooks)                                              │   │
│   │ ○ Every 15 minutes                                                  │   │
│   │ ● Every hour (recommended)                                          │   │
│   │ ○ Nightly (12:00 AM - 4:00 AM)                                      │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│   Patient Filter:                                                           │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │ ● All patients                                                      │   │
│   │ ○ Attributed patients only                                          │   │
│   │ ○ Specific payer/plan                                               │   │
│   │ ○ Custom filter                                                     │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.3 Conflict Resolution

| Scenario | Resolution |
|----------|------------|
| **Duplicate patient** | Match on MRN → merge |
| **Conflicting data** | EHR is source of truth |
| **Deleted in EHR** | Mark inactive in HDIM |
| **Historical changes** | Track version history |

---

## 7. Implementation Process

### 7.1 Typical Integration Timeline

```
Week 1          Week 2          Week 3          Week 4
──────          ──────          ──────          ──────

┌─────────────┐
│ Discovery   │
│ & Planning  │
└─────────────┘
                ┌─────────────┐
                │ EHR App     │
                │ Approval    │
                └─────────────┘
                                ┌─────────────┐
                                │ Connection  │
                                │ Setup       │
                                └─────────────┘
                                                ┌─────────────┐
                                                │ Data Sync   │
                                                │ & Validation│
                                                └─────────────┘
```

### 7.2 Step-by-Step Process

| Step | Activities | Duration | Responsibility |
|------|------------|----------|----------------|
| **1. Discovery** | Identify EHR, version, APIs available | 1-2 days | HDIM + Customer |
| **2. EHR Approval** | Request app/integration approval | 1-5 days | Customer |
| **3. Credentials** | Obtain API credentials, configure OAuth | 1-2 days | Customer |
| **4. Connection** | Configure HDIM connection to EHR | 1-2 days | HDIM |
| **5. Initial Sync** | Load historical data | 1-3 days | HDIM |
| **6. Validation** | Verify data accuracy | 2-3 days | HDIM + Customer |
| **7. Go-Live** | Enable production sync | 1 day | HDIM |

### 7.3 Customer Checklist

**Before Integration:**
- [ ] Confirm EHR vendor and version
- [ ] Identify EHR administrator contact
- [ ] Verify FHIR/API is enabled
- [ ] Understand data sharing policies
- [ ] Obtain necessary approvals (IT, compliance, privacy)

**During Integration:**
- [ ] Approve HDIM app in EHR marketplace (if applicable)
- [ ] Provide API credentials or OAuth configuration
- [ ] Authorize data access scopes
- [ ] Identify test patients for validation
- [ ] Review and approve data mapping

**After Integration:**
- [ ] Verify data quality in HDIM
- [ ] Configure sync frequency
- [ ] Set up alerts for sync failures
- [ ] Train staff on integrated workflow

---

## 8. Security & Compliance

### 8.1 Integration Security

| Control | Implementation |
|---------|----------------|
| **Authentication** | OAuth 2.0 / SMART on FHIR |
| **Authorization** | Scoped access tokens |
| **Encryption (Transit)** | TLS 1.3 |
| **Encryption (Rest)** | AES-256 |
| **Token Storage** | Encrypted, rotated |
| **Audit Logging** | All API calls logged |

### 8.2 Data Minimization

HDIM only requests the minimum data needed for quality measurement:

| Data Category | Requested | Reason |
|---------------|-----------|--------|
| Demographics | Yes | Patient identification |
| Conditions | Yes | Measure denominators/exclusions |
| Medications | Yes | Medication measures |
| Labs | Yes | Lab-based measures |
| Vitals | Yes | Vital-based measures |
| Immunizations | Yes | Immunization measures |
| Encounters | Yes | Visit-based measures |
| Documents/Notes | No | Not needed for HEDIS |
| Financial | No | Not needed for quality |

### 8.3 Compliance Considerations

| Requirement | HDIM Approach |
|-------------|---------------|
| **HIPAA** | BAA with customers, PHI encrypted |
| **42 CFR Part 2** | Substance abuse data segregation |
| **State Privacy** | Configurable consent requirements |
| **Information Blocking** | Compliant with 21st Century Cures |
| **TEFCA** | Monitoring for future participation |

---

## 9. Pricing

### 9.1 Integration Costs

| Component | Cost | Notes |
|-----------|------|-------|
| **Standard FHIR Integration** | Included | Epic, Cerner, athena (when available) |
| **Custom Integration Setup** | $2,500 one-time | Non-standard EHRs |
| **HL7v2 Interface** | $5,000 one-time | Legacy systems |
| **HIE Connection** | $2,500-5,000 | Depends on HIE |
| **Custom Development** | $150/hour | Beyond standard scope |

### 9.2 What's Included (By Tier)

| Feature | Community | Professional | Enterprise |
|---------|-----------|--------------|------------|
| Manual upload (CSV) | ✅ | ✅ | ✅ |
| FHIR API (inbound) | ❌ | ✅ | ✅ |
| Standard EHR connections | ❌ | 1 included | 3 included |
| Additional connections | N/A | $500/mo each | $300/mo each |
| Real-time sync | ❌ | ❌ | ✅ |
| Custom integration | ❌ | ❌ | Available |

---

## 10. Support & Troubleshooting

### 10.1 Integration Support

| Issue Type | Response Time | Contact |
|------------|---------------|---------|
| **Connection failure** | 2 hours | support@healthdatainmotion.com |
| **Data sync errors** | 4 hours | support@healthdatainmotion.com |
| **Data quality issues** | 8 hours | support@healthdatainmotion.com |
| **New integration request** | 2 business days | sales@healthdatainmotion.com |

### 10.2 Common Issues & Solutions

| Issue | Likely Cause | Solution |
|-------|--------------|----------|
| **Auth failures** | Token expired | Re-authorize in EHR |
| **Missing data** | Scope too narrow | Expand OAuth scopes |
| **Slow sync** | Large dataset | Switch to bulk export |
| **Duplicate patients** | Multiple MRNs | Configure matching rules |
| **Stale data** | Sync disabled | Check sync schedule |

### 10.3 Monitoring

HDIM provides integration monitoring:

- Sync status dashboard
- Failure alerts (email)
- Data freshness indicators
- API call logs (Enterprise+)

---

## 11. Future Roadmap

### 11.1 2026 and Beyond

| Initiative | Timeline | Description |
|------------|----------|-------------|
| **TEFCA Participation** | 2026 | Connect via national framework |
| **CDS Hooks** | Q2 2026 | Real-time clinical decision support |
| **Bi-directional Sync** | Q3 2026 | Write care gaps back to EHR |
| **AI-Powered Matching** | Q4 2026 | Improved patient matching |
| **IoT/Remote Monitoring** | 2027 | Device data integration |

### 11.2 Customer Requests

We prioritize integrations based on customer demand. To request an EHR integration:

1. Email integrations@healthdatainmotion.com
2. Include: EHR name, version, number of patients
3. We evaluate feasibility and timeline
4. High-demand integrations prioritized

---

## Appendix

### A. EHR Contact Information

| EHR | Integration Program | URL |
|-----|---------------------|-----|
| Epic | App Orchard | apporchard.epic.com |
| Cerner | CODE Program | code.cerner.com |
| athenahealth | Marketplace | marketplace.athenahealth.com |
| eClinicalWorks | Partner Program | eclinicalworks.com/partners |
| AllScripts | Developer Program | developer.allscripts.com |
| NextGen | NextGen Connect | nextgen.com/connect |

### B. FHIR Resources Reference

| USCDI Element | FHIR Resource | HDIM Usage |
|---------------|---------------|------------|
| Patient demographics | Patient | Identification |
| Problems | Condition | Measure logic |
| Medications | MedicationRequest | Medication measures |
| Allergies | AllergyIntolerance | Exclusions |
| Vital signs | Observation | Vital measures |
| Lab results | Observation, DiagnosticReport | Lab measures |
| Procedures | Procedure | Procedure measures |
| Immunizations | Immunization | Immunization measures |
| Encounters | Encounter | Visit measures |

### C. Glossary

| Term | Definition |
|------|------------|
| **FHIR** | Fast Healthcare Interoperability Resources |
| **SMART** | Substitutable Medical Applications, Reusable Technologies |
| **USCDI** | United States Core Data for Interoperability |
| **TEFCA** | Trusted Exchange Framework and Common Agreement |
| **HIE** | Health Information Exchange |
| **CDS** | Clinical Decision Support |

---

*EHR Integration Roadmap Version: 1.0*
*Last Updated: December 2025*
*Contact: integrations@healthdatainmotion.com*
