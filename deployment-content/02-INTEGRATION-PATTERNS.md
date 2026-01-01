# HDIM Integration Patterns

How HDIM integrates with your existing healthcare infrastructure.

---

## Table of Contents
1. [FHIR Server Integration](#1-fhir-server-integration)
2. [EHR System Integration](#2-ehr-system-integration)
3. [Authentication Integration](#3-authentication--sso-integration)
4. [Data Ingestion Patterns](#4-data-ingestion-patterns)
5. [Outbound Integration](#5-outbound-integration)
6. [Real-Time vs Batch Patterns](#6-real-time-vs-batch-patterns)

---

## 1. FHIR Server Integration

### Pattern: Direct REST Query

HDIM queries your existing FHIR server directly via REST API. No data copying, no ETL pipeline required.

```
HDIM Quality Measure Service
         ↓ (HTTP/REST)
    Your FHIR Server
    (Epic, Cerner, Generic FHIR)
         ↓ (Returns JSON)
    FHIR Resources
    ├─ Patient
    ├─ Observation
    ├─ Condition
    ├─ Medication
    └─ ...
```

### Configuration

**Environment Variables** (.env):

```bash
# Your FHIR Server Configuration
FHIR_SERVER_URL=https://your-ehr.example.com/fhir
FHIR_SERVER_USERNAME=hdim_api_user
FHIR_SERVER_PASSWORD=<encrypted_password>

# Authentication
FHIR_AUTH_TYPE=basic  # or: oauth2, bearer_token
FHIR_AUTH_TOKEN=<bearer_token>
FHIR_OAUTH2_CLIENT_ID=hdim_client
FHIR_OAUTH2_CLIENT_SECRET=<secret>
FHIR_OAUTH2_TOKEN_URL=https://your-auth.example.com/oauth/token

# Connection Settings
FHIR_CONNECT_TIMEOUT=10s
FHIR_READ_TIMEOUT=30s
FHIR_MAX_CONNECTIONS=50
FHIR_VERIFY_SSL=true
FHIR_PROXY_URL=<optional_proxy>

# Retry Policy
FHIR_RETRY_ATTEMPTS=3
FHIR_RETRY_BACKOFF=exponential
FHIR_CIRCUIT_BREAKER_THRESHOLD=10
FHIR_CIRCUIT_BREAKER_TIMEOUT=60s
```

### Supported FHIR Servers

**Epic FHIR Server**
- Standard: FHIR R4
- Authentication: OAuth2 with Epic credentials
- Bulk Export: Supported via $export operation
- Custom Extensions: Epic-specific extensions supported

**Cerner FHIR Server**
- Standard: FHIR R4
- Authentication: OAuth2 with Cerner open platform
- Bulk Export: Supported
- Rate Limiting: 100 requests/minute per app

**Generic FHIR Servers**
- OpenFHIR, Hapi FHIR open source, etc.
- Standard: FHIR R4 (or R3 with adapter)
- Authentication: HTTP Basic, OAuth2, Bearer token
- Custom: Any FHIR-compliant server

### Query Patterns

**Pattern 1: Patient Demographics**

```bash
# Query
GET https://your-ehr.example.com/fhir/Patient/patient-123

# Response
{
  "resourceType": "Patient",
  "id": "patient-123",
  "name": [{"given": ["John"], "family": "Doe"}],
  "birthDate": "1970-05-15",
  "gender": "male",
  "address": [...],
  "contact": [...]
}

# HDIM Usage
Quality Measure Service
├─ Extracts birthDate
├─ Calculates age
├─ Uses for age-based measure eligibility
└─ Returns to clinical portal
```

**Pattern 2: Patient Observations (Labs & Vitals)**

```bash
# Query
GET https://your-ehr.example.com/fhir/Observation?patient=patient-123
    &code=http://loinc.org|2345-7  # Glucose measurement

# Response
{
  "resourceType": "Bundle",
  "entry": [
    {
      "resource": {
        "resourceType": "Observation",
        "id": "obs-456",
        "subject": {"reference": "Patient/patient-123"},
        "code": {"coding": [{"code": "2345-7", "system": "http://loinc.org"}]},
        "valueQuantity": {"value": 95, "unit": "mg/dL"},
        "effectiveDateTime": "2024-11-20T09:00:00Z"
      }
    },
    ...
  ]
}

# HDIM Usage
Quality Measure Service (HbA1c measure)
├─ Searches for glucose observations
├─ Filters by date range (last 12 months)
├─ Evaluates glucose control
├─ Determines numerator/denominator status
└─ Reports to clinician
```

**Pattern 3: Patient Conditions (Diagnoses)**

```bash
# Query
GET https://your-ehr.example.com/fhir/Condition?patient=patient-123

# Response
{
  "resourceType": "Bundle",
  "entry": [
    {
      "resource": {
        "resourceType": "Condition",
        "code": {"coding": [{"code": "E11", "system": "http://hl7.org/fhir/sid/icd-10-cm"}]},
        "subject": {"reference": "Patient/patient-123"},
        "clinicalStatus": {"coding": [{"code": "active"}]},
        "onsetDate": "2018-03-15"
      }
    }
  ]
}

# HDIM Usage
HCC Risk Adjustment Service
├─ Identifies conditions (Diabetes, Hypertension, etc.)
├─ Maps to HCC categories
├─ Calculates risk score
└─ Used for payment adjustment
```

**Pattern 4: Bulk Export for Population Data**

```bash
# Initiate bulk export (for population health)
POST https://your-ehr.example.com/fhir/$export
?_type=Patient,Observation,Condition
&_since=2024-01-01

# Response
{
  "transactionTime": "2024-11-21T10:00:00Z",
  "request": "fhir/$export?...",
  "output": [
    {"type": "Patient", "url": "https://...patient-1.ndjson"},
    {"type": "Observation", "url": "https://...obs-1.ndjson"},
    {"type": "Condition", "url": "https://...cond-1.ndjson"}
  ],
  "error": []
}

# HDIM Usage (Batch Measure Evaluation)
CDR Processor Service
├─ Downloads NDJSON files
├─ Batch imports patient data
├─ Runs population-level measures
├─ Generates quarterly reports
└─ Stores results in PostgreSQL
```

### Caching & Performance

```
HDIM Patient FHIR Data Cache
│
├─ Patient Resource (demographics)
│  └─ TTL: 24 hours (rarely changes)
│  └─ Key: cache:patient:{patient-id}
│
├─ Observation Resources (labs, vitals)
│  └─ TTL: 5 minutes (changes frequently, HIPAA compliant)
│  └─ Key: cache:observation:{patient-id}:{type}
│
├─ Condition Resources (diagnoses)
│  └─ TTL: 12 hours (relatively stable)
│  └─ Key: cache:condition:{patient-id}
│
└─ Medication Resources
   └─ TTL: 4 hours (can change frequently)
   └─ Key: cache:medication:{patient-id}

Cache Invalidation Strategy:
├─ TTL-based expiration (automatic)
├─ Event-based (when patient data changes in your EHR)
├─ On-demand invalidation API (for immediate refresh)
└─ Cache warming (pre-load high-frequency patients)
```

### Error Handling & Resilience

```
FHIR Query Fails
    ↓
Check Circuit Breaker
├─ If CLOSED: Attempt query
├─ If OPEN: Return cached data
│  └─ Fallback: Stale cache (< 5min old) acceptable
│  └─ Otherwise: Return error to user
└─ If HALF_OPEN: Test query, transition to CLOSED/OPEN

Retry Logic:
├─ Retry 1: 100ms delay
├─ Retry 2: 300ms delay (exponential backoff)
├─ Retry 3: 900ms delay
├─ Give up: Return last known good data

Monitoring:
├─ Alert if FHIR server down > 5 minutes
├─ Alert if error rate > 5%
├─ Alert if latency > 5 seconds p99
└─ Auto-page on-call engineer if critical
```

---

## 2. EHR System Integration

### Pattern: Bi-directional Integration

HDIM integrates with your EHR system for:
1. **Inbound**: Get patient data (via FHIR Server integration above)
2. **Outbound**: Send clinical decisions back to EHR

```
Your EHR System
├─ Patient Data ──→ HDIM (via FHIR REST)
│                   ├─ Calculate measures
│                   ├─ Detect care gaps
│                   └─ Assess risk
│
└─ Clinical Decisions ←── HDIM
    ├─ Care gap alerts
    ├─ Quality measure scores
    ├─ Risk stratification
    └─ Clinical recommendations
```

### Integration Methods

**Method 1: HL7 FHIR (REST API) - RECOMMENDED**

Most modern EHR systems support FHIR REST API:

```bash
# HDIM → Your EHR System
POST https://your-ehr.example.com/fhir/CarePlan
Authorization: Bearer <oauth_token>
Content-Type: application/fhir+json

{
  "resourceType": "CarePlan",
  "status": "active",
  "intent": "plan",
  "subject": {"reference": "Patient/patient-123"},
  "activity": [
    {
      "detail": {
        "status": "scheduled",
        "code": {
          "coding": [{"code": "mammography"}]
        },
        "scheduledString": "Within 6 months"
      }
    }
  ],
  "extension": [
    {
      "url": "https://hdim.example.com/careGap",
      "valueString": "Breast Cancer Screening overdue"
    }
  ]
}

# Your EHR stores the care plan
# Clinician sees it in their workflow
```

**Method 2: HL7 v2 (Legacy) - If FHIR Not Available**

For older EHR systems:

```
HDIM → HL7 v2 Adapter → Your EHR System

Example HL7 v2 Message:
MSH|^~\&|HDIM|HDIM|||20241121100000||ORU^R01|||2.5
PID|||patient-123||Doe^John
OBX|1|ST|BCS^Breast Cancer Screening||OVERDUE|
OBX|2|ST|LAST_SCREENING||2023-05-15|
```

**Method 3: Direct Secure Email**

For systems without modern API support:

```
HDIM → Direct Email Gateway → Your EHR User
Subject: Care Gap Alert - John Doe
From: alerts@hdim.example.com
To: clinician@your-ehr.example.com

Message Body:
Patient: John Doe (DOB: 1970-05-15)
Measure: Breast Cancer Screening
Status: OVERDUE
Last Screening: 2023-05-15 (600+ days ago)
Recommendation: Schedule mammography within 30 days

[View in Portal] [Mark Complete]
```

### Supported EHR Systems

| EHR System | FHIR Support | HL7 v2 Support | Direct Support | Preferred Method |
|-----------|--------------|----------------|---|---|
| **Epic** | ✓ (Full R4) | ✓ | ✓ | FHIR REST API |
| **Cerner** | ✓ (Full R4) | ✓ | ✓ | FHIR REST API |
| **Athena** | ✓ (FHIR-lite) | ✓ | ✓ | FHIR REST API |
| **NextGen** | ✓ (Partial) | ✓ | ✓ | HL7 v2 or FHIR |
| **Medidata** | ✓ (Full) | ✓ | ✓ | FHIR REST API |
| **Allscripts** | ✓ (Full) | ✓ | ✓ | FHIR REST API |
| **Any FHIR** | ✓ | Optional | ✓ | FHIR REST API |

### Outbound Data Formats

**Format 1: FHIR Resource (Standard)**
```json
{
  "resourceType": "Bundle",
  "type": "transaction",
  "entry": [
    {
      "resource": {
        "resourceType": "Observation",
        "code": {"coding": [{"code": "BCS-score"}]},
        "valueString": "DENOMINATOR",
        "subject": {"reference": "Patient/patient-123"},
        "effectiveDateTime": "2024-11-21T10:00:00Z"
      }
    }
  ]
}
```

**Format 2: Clinical Document (CCD)**
```xml
<?xml version="1.0"?>
<ClinicalDocument>
  <component>
    <structuredBody>
      <component>
        <section>
          <code code="BCS" codeSystem="2.16.840.1.113883.3.464.1003.198.11"/>
          <title>Breast Cancer Screening</title>
          <text>Patient eligible for screening. Last screening: 2024-03-15</text>
        </section>
      </structuredBody>
    </component>
  </ClinicalDocument>
</ClinicalDocument>
```

**Format 3: CSV Report (for data warehouse)**
```csv
patient_id,measure_id,measure_name,classification,date_evaluated,clinician_id
patient-123,BCS,Breast Cancer Screening,NUMERATOR,2024-11-21,physician-456
```

---

## 3. Authentication & SSO Integration

### Pattern: Centralized OIDC/OAuth2

HDIM integrates with your existing identity provider.

```
Your Enterprise
└─ OIDC Provider (Okta, Azure AD, Keycloak)
   └─ User credentials stored in your system
   └─ MFA if configured

Clinical User
├─ Opens HDIM portal
├─ Clicks "Login with SSO"
└─ Redirected to your OIDC provider
   └─ Logs in with corporate credentials
   └─ Approves HDIM access scope
   └─ Redirected back to HDIM with JWT token
   └─ HDIM validates token (Kong API Gateway)
   └─ User logged in
```

### Configuration

**Kong API Gateway OIDC Configuration**:

```bash
# 1. Register HDIM as OAuth client in your OIDC provider
OIDC_PROVIDER=https://your-okta.okta.com
OIDC_CLIENT_ID=hdim_app_client
OIDC_CLIENT_SECRET=<secret>
OIDC_DISCOVERY_URL=${OIDC_PROVIDER}/.well-known/openid-configuration

# 2. Configure Kong with OIDC plugin
# /etc/kong/kong.yaml
plugins:
  - name: openid-connect
    config:
      issuer: ${OIDC_PROVIDER}
      client_id: ${OIDC_CLIENT_ID}
      client_secret: ${OIDC_CLIENT_SECRET}
      discovery: ${OIDC_DISCOVERY_URL}
      scopes:
        - openid
        - profile
        - email
        - roles
      claims_forward:
        - sub
        - email
        - name
        - roles
        - tenant_ids
```

### Supported Providers

**Okta**
- Configuration: Standard OIDC setup
- User attributes: Custom attributes for tenant_ids, roles
- MFA: Fully supported
- Group sync: Use Okta groups for HDIM roles

**Azure AD**
- Configuration: Microsoft identity platform
- User attributes: Extension attributes for custom claims
- MFA: Conditional access policies
- Group sync: Use Azure AD groups for HDIM roles

**Keycloak (Open Source)**
- Configuration: OpenID Connect Server (OIDC)
- User attributes: Custom mappers for claims
- MFA: OTP, U2F support
- Group sync: Keycloak groups → JWT claims

**Generic OIDC**
- Any OIDC-compliant provider
- User attributes via claims mapping
- Standard flow + PKCE for mobile

### Claim Mapping

```
OIDC Provider Claims → HDIM Context

User ID (sub)
└─ User-123
   └─ Maps to X-Auth-User-Id header

User Email (email)
└─ john.doe@hospital.example.com
   └─ Maps to X-Auth-Username header

User Roles (custom claim: hdim_roles)
└─ ["EVALUATOR", "VIEWER"]
   └─ Maps to X-Auth-Roles header

Tenant Access (custom claim: hdim_tenant_ids)
└─ ["hospital-corp", "clinic-chain"]
   └─ Maps to X-Auth-Tenant-Ids header
```

### Custom Claim Configuration

**Okta Example**:
```
Settings → Authorization Servers → default
├─ Claims:
│  ├─ sub: Okta user ID
│  ├─ email: Okta user email
│  ├─ name: Okta user name
│  ├─ groups: User groups (custom)
│  └─ tenant_id: Custom attribute (user attribute)
│
└─ Scope Consent:
   ├─ openid: Required
   ├─ profile: User name, email
   └─ custom: Custom scopes for HDIM
```

### JWT Token Example

```json
{
  "sub": "user-123",
  "email": "john.doe@hospital.example.com",
  "name": "John Doe",
  "groups": ["hospital-corp-admins", "clinical-users"],
  "hdim_roles": ["EVALUATOR", "VIEWER"],
  "hdim_tenant_ids": ["hospital-corp"],
  "iat": 1700563200,
  "exp": 1700649600,
  "aud": "hdim_app_client"
}
```

---

## 4. Data Ingestion Patterns

### Pattern 1: Real-Time (FHIR Push)

Your EHR pushes FHIR resources to HDIM as they're created/updated.

```
Your EHR System
├─ Patient created
├─ Observation recorded
└─ Condition diagnosed
    └─ Each event triggers FHIR POST to HDIM
       └─ HDIM FHIR Service receives
       └─ Stores in PostgreSQL (optional)
       └─ Publishes event to Kafka
       └─ Services react in real-time
```

**Configuration in Your EHR**:

```
Epic Fhir Subscription:
├─ Trigger: Patient changed, Observation created
├─ Endpoint: https://hdim.example.com/fhir/Patient
├─ Method: POST
├─ Payload: FHIR JSON
├─ Retry: 3x with exponential backoff
└─ Auth: OAuth2 token

Cerner Fhir Subscription:
├─ Trigger: Condition created, Medication updated
├─ Endpoint: https://hdim.example.com/fhir/Condition
├─ Method: POST
├─ Payload: FHIR JSON
└─ Auth: Bearer token
```

### Pattern 2: Scheduled Bulk Import

Batch import of patient data at scheduled intervals.

```
Daily at 2 AM (off-peak):
├─ Your EHR initiates $export operation
├─ Generates NDJSON files (1 line = 1 resource)
├─ HDIM CDR Processor Service downloads files
├─ Imports 100,000+ patients
├─ Updates measure definitions
└─ Runs batch quality reports

Weekly at 6 PM:
├─ Population-level aggregation
├─ Measure trending analysis
└─ Generates reports for leadership
```

### Pattern 3: On-Demand Query

Measure evaluation triggers query of your FHIR server.

```
User requests measure in portal
    └─ HDIM calls your FHIR server
    └─ Fetches fresh data
    └─ Calculates in real-time
    └─ Returns result (no cache)
```

### Data Ingestion Configuration

```bash
# .env configuration

# Pattern 1: Real-Time Push
FHIR_SUBSCRIPTION_ENABLED=true
FHIR_SUBSCRIPTION_ENDPOINT=https://hdim.example.com/fhir
FHIR_SUBSCRIPTION_TRIGGER=Patient,Observation,Condition
FHIR_SUBSCRIPTION_RETRY_ENABLED=true
FHIR_SUBSCRIPTION_RETRY_COUNT=3

# Pattern 2: Bulk Import
CDR_BULK_IMPORT_ENABLED=true
CDR_BULK_IMPORT_SCHEDULE=cron(0 2 * * ?) # Daily 2 AM UTC
CDR_BULK_IMPORT_ENDPOINT=https://your-ehr.example.com/fhir/$export
CDR_BULK_IMPORT_BATCH_SIZE=10000
CDR_BULK_IMPORT_PARALLEL_JOBS=4

# Pattern 3: Query (default)
FHIR_QUERY_ON_DEMAND=true
FHIR_QUERY_CACHE_TTL=5m
FHIR_QUERY_TIMEOUT=30s
```

---

## 5. Outbound Integration

### Care Gap Notifications

When HDIM detects a care gap (e.g., overdue screening):

```
Care Gap Service detects gap
    └─ Publishes event: care-gap.detected
    └─ Notification Service consumes
    └─ Multiple notification channels:

├─ Email notification
│  └─ Clinician: "Patient John Doe overdue for mammography"
│  └─ Patient: "Your screening is due"
│
├─ SMS notification
│  └─ Patient: "Your annual screening is due. Call [clinic]"
│
├─ Direct Secure Email
│  └─ To clinician's Direct account
│  └─ Electronic delivery with audit trail
│
├─ Integration with your EHR
│  └─ Creates task in EHR workflow
│  └─ Sends HL7 message to EHR system
│
└─ HDIM Portal Alert
   └─ "23 patients overdue for screening"
   └─ Clickable list with one-click ordering
```

### Configuration

```bash
# Notification Settings
NOTIFICATION_EMAIL_ENABLED=true
NOTIFICATION_EMAIL_SMTP=smtp.example.com:587
NOTIFICATION_EMAIL_FROM=alerts@hdim.example.com

NOTIFICATION_SMS_ENABLED=true
NOTIFICATION_SMS_PROVIDER=twilio  # or: nexmo, aws-sns
NOTIFICATION_SMS_API_KEY=<key>

NOTIFICATION_DIRECT_ENABLED=true
NOTIFICATION_DIRECT_PROVIDER=directtrust
NOTIFICATION_DIRECT_ENDPOINT=https://your-direct.example.com

NOTIFICATION_EHR_ENABLED=true
NOTIFICATION_EHR_TYPE=hl7v2  # or: fhir
NOTIFICATION_EHR_ENDPOINT=https://your-ehr.example.com/hl7
```

### Outbound API for Custom Integrations

```bash
# Get list of patients with care gaps
GET /api/care-gaps?measure=BCS&gap_status=OPEN&limit=100

Response:
{
  "gaps": [
    {
      "patientId": "patient-123",
      "patientName": "John Doe",
      "measure": "BCS",
      "gapType": "DENOMINATOR",
      "daysOverdue": 620,
      "recommendedAction": "Schedule mammography"
    }
  ]
}

# Mark care gap as closed
POST /api/care-gaps/gap-456/close
{
  "closureReason": "Scheduled mammography",
  "scheduledDate": "2024-12-15"
}
```

---

## 6. Real-Time vs Batch Patterns

### Real-Time Pattern

**Best for**: Urgent decisions, individual patient evaluation

```
Clinical user requests measure
    └─ HDIM queries your FHIR server NOW
    └─ Fresh data
    └─ Result in <500ms
    └─ Clinical decision made immediately
```

**Configuration**:
```bash
MEASURE_EVALUATION_MODE=realtime
FHIR_CACHE_ENABLED=true
FHIR_CACHE_TTL=5m
RESULT_CACHING=enabled
RESULT_CACHE_TTL=disabled  # Don't cache final result
```

### Batch Pattern

**Best for**: Population health reporting, historical analysis

```
Scheduled job (nightly)
    └─ Fetch all patients via $export
    └─ Calculate measures for all
    └─ Store results in PostgreSQL
    └─ Generate population reports
    └─ Available next morning
```

**Configuration**:
```bash
BATCH_EVALUATION_SCHEDULE=cron(0 2 * * *)  # Daily 2 AM
BATCH_EVALUATION_MODE=population
FHIR_BULK_IMPORT=enabled
BATCH_RESULT_STORAGE=postgresql
BATCH_REPORT_GENERATION=enabled
BATCH_PARALLEL_WORKERS=4
```

### Hybrid Pattern

**Best for**: Most production deployments

```
Real-time for:
    └─ Individual patient measures
    └─ Care gap detection
    └─ Clinical decision support

Batch for:
    └─ Population trending
    └─ Monthly quality reporting
    └─ Predictive analytics
    └─ Benchmarking
```

---

## Integration Checklist

### Pre-Deployment

- [ ] Identify FHIR server (Epic, Cerner, Generic?)
- [ ] Confirm FHIR R4 support
- [ ] Get API credentials (OAuth2 client, Basic auth, etc.)
- [ ] Test connectivity from HDIM server to FHIR server
- [ ] Identify authentication provider (Okta, AD, Keycloak?)
- [ ] Create HDIM OAuth client in auth provider
- [ ] Map user attributes (roles, tenant IDs)
- [ ] Plan data ingestion (real-time, batch, or hybrid?)
- [ ] Identify outbound notification requirements (email, SMS, EHR)?
- [ ] Plan for high availability (failover, redundancy)?

### Post-Deployment

- [ ] Test end-to-end data flow
- [ ] Validate measure calculations against manual reviews
- [ ] Monitor FHIR query latency
- [ ] Verify audit logging includes PHI access
- [ ] Test failover scenarios
- [ ] Train clinical users on workflows
- [ ] Set up escalation procedures for gaps

---

## Summary

HDIM integrates with your existing healthcare infrastructure through:

1. **FHIR Server Integration**: Direct REST queries to your EHR (Epic, Cerner, etc.)
2. **EHR Integration**: Bi-directional HL7/FHIR messaging for clinical workflows
3. **Authentication Integration**: Your existing SSO (Okta, AD, Keycloak)
4. **Data Ingestion**: Real-time, batch, or hybrid patterns
5. **Outbound Notifications**: Alerts via email, SMS, Direct, or EHR integration

Your data stays in your infrastructure. HDIM simply adds a layer of clinical intelligence on top.
