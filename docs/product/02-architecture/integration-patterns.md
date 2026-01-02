---
id: "product-integration-patterns"
title: "Integration Patterns & APIs"
portalType: "product"
path: "product/02-architecture/integration-patterns.md"
category: "architecture"
subcategory: "integration"
tags: ["integration", "API", "FHIR", "interoperability", "EHR-integration", "REST"]
summary: "Comprehensive guide to integrating HealthData in Motion with EHR systems, FHIR servers, and third-party applications. Covers REST APIs, FHIR standards, batch import patterns, real-time event streaming, and vendor-specific integration approaches."
estimatedReadTime: 20
difficulty: "advanced"
targetAudience: ["cio", "architect", "technical-lead", "integration-engineer"]
owner: "Product Architecture"
reviewCycle: "quarterly"
status: "published"
version: "1.0"
seoKeywords: ["FHIR integration", "EHR integration", "REST API", "interoperability", "healthcare integration", "HL7 FHIR"]
relatedDocuments: ["system-architecture", "data-model-specification", "security-architecture"]
lastUpdated: "2025-12-01"
---

# Integration Patterns & APIs

## Executive Summary

HealthData in Motion uses a **standards-based, event-driven integration approach** that works with virtually any EHR system. Multiple integration patterns support real-time data streaming, batch imports, and pull-based data retrieval, ensuring seamless data flow while maintaining HIPAA compliance and data integrity.

**Integration Approaches**:
1. **FHIR REST API** - Pull-based access to patient data
2. **FHIR Bulk Export** - Scheduled batch data exports
3. **HL7 v2 Messaging** - Legacy EHR compatibility
4. **Direct & SFTP** - Secure file transfers
5. **Webhook/Event Streaming** - Real-time push notifications
6. **Vendor-Specific SDKs** - Epic, Cerner, Athena connectors

## REST API Integration

### FHIR API Endpoints

#### Patient Management
```http
GET /fhir/Patient/{id}
GET /fhir/Patient?given=John&family=Doe&birthdate=1990-01-01
GET /fhir/Patient?active=true&_sort=-birthDate&_count=100
```

#### Clinical Data Retrieval
```http
GET /fhir/Observation?patient={patientId}&code=loinc-code&date=ge2025-01-01
GET /fhir/Condition?patient={patientId}&clinical-status=active
GET /fhir/Encounter?patient={patientId}&type=outpatient
GET /fhir/MedicationRequest?patient={patientId}&status=active
GET /fhir/Procedure?patient={patientId}&date=ge2025-01-01
```

#### Batch Submission
```http
POST /fhir/Bundle
Content-Type: application/fhir+json

{
  "resourceType": "Bundle",
  "type": "transaction",
  "entry": [
    { "resource": { "resourceType": "Patient", ... } },
    { "resource": { "resourceType": "Observation", ... } }
  ]
}
```

### Authentication & Authorization

**OAuth 2.0 / OIDC**:
- Confidential client credentials flow for server-to-server
- Authorization code flow for user-initiated access
- Scope-based permission (read:patients, write:observations, etc.)
- Token expiration: 1 hour (refresh token: 30 days)

**API Keys** (for legacy systems):
- Long-lived keys with scope restrictions
- Rotation every 90 days
- Rate limiting: 10,000 requests/hour per key

**Audit Logging**:
- Every API call logged with user, action, timestamp, patient ID
- Immutable audit trail in PostgreSQL
- Compliance reports available monthly

### Rate Limiting & Quotas

| Endpoint | Limit | Tier |
|----------|-------|------|
| Patient retrieval | 1,000 req/min | All |
| Observation queries | 500 req/min | All |
| Batch submissions | 100 req/min | All |
| Full exports | 10 concurrent | Enterprise |

## Batch Import Patterns

### Scheduled Daily Import
```
7:00 AM: EHR exports patient data to SFTP
7:05 AM: HDiM polls SFTP, downloads file
7:10 AM: Validate data schema and content
7:15 AM: Transform to FHIR format
7:20 AM: Bulk insert into database (batch)
7:25 AM: Publish domain events
7:30 AM: Quality measures evaluate new data
7:45 AM: Dashboards update with new metrics
```

**Configuration**:
- Scheduling: Flexible (daily, weekly, monthly)
- Format: CSV, HL7 v2, or native FHIR JSON
- Transformation: Mapping file (configurable)
- Validation: Schema + business rule checks
- Retry logic: 3 attempts with exponential backoff

### Files Supported
- **CSV**: Column mapping to FHIR attributes
- **HL7 v2**: ORM, ORU, ADT message types
- **FHIR JSON/XML**: Bundle or individual resources
- **Delimited text**: Tab, pipe, or custom delimiter

### Incremental vs. Full Import
- **Incremental** (recommended): Only new/changed records (daily, <30 min)
- **Full**: All records each cycle (weekly, <2 hours for 100K patients)
- **Delta**: Changed records since last import (timestamp-based)

## Real-Time Data Streaming

### Webhook Integration
```
EHR System publishes event →
POST https://hdim.example.com/webhooks/observation-recorded →
HDiM receives → Validates signature → Processes event →
Updates database → Publishes domain event →
Quality measures triggered → Care gaps evaluated →
Results streamed to portal
```

**Webhook Configuration**:
- Multiple webhook endpoints per event type
- HMAC-SHA256 signature verification (required)
- Automatic retry (3 attempts, exponential backoff)
- Dead-letter queue for failed deliveries
- Maximum latency: <5 seconds end-to-end

**Event Types**:
- `observation.recorded` - New lab/vital result
- `condition.diagnosed` - New diagnosis
- `encounter.started` - Encounter begins
- `encounter.ended` - Encounter complete
- `medication.prescribed` - New medication order
- `patient.admitted` - Hospital admission

### Kafka Event Streaming
For EHR systems with Kafka integration:
```
EHR Kafka topic (ehr.clinical-events) →
HDiM Kafka consumer →
Validates and transforms event →
Publishes to domain-events topic →
Care gap service consumes →
Triggers measure evaluation
```

**Topics Consumed**:
- `ehr.observations` - Clinical results
- `ehr.diagnoses` - Condition changes
- `ehr.encounters` - Admission/discharge
- `ehr.medications` - Medication orders
- `ehr.admissions` - Hospital/ED admits

## FHIR Bulk Export

### Background Job Export
Large data exports handled asynchronously:

```
POST /fhir/$export?_type=Observation,Condition,Encounter
Content-Type: application/fhir+json

Returns:
{
  "transactionTime": "2025-12-01T10:30:00Z",
  "request": "GET /fhir/$export?_type=Observation,Condition",
  "requiresAccessToken": true,
  "output": [
    {
      "type": "Observation",
      "url": "https://hdim.example.com/export/obs_1.ndjson"
    },
    {
      "type": "Condition",
      "url": "https://hdim.example.com/export/cond_1.ndjson"
    }
  ],
  "error": []
}
```

**Parameters**:
- `_type`: Resource types (Observation, Condition, Encounter, etc.)
- `_since`: Only records modified after date
- `_outputFormat`: application/fhir+ndjson (default), application/fhir+json
- `_elements`: Specific fields to include

## HL7 v2 Legacy Integration

### Message Types Supported
- **ADT**: Admit, discharge, transfer (patient demographics)
- **ORM**: Order messages (test orders, procedures)
- **ORU**: Observation result unsolicited (lab results, vitals)
- **DFT**: Detailed financial transaction (billing data)

### Example ADT^A01 Processing
```
MSH|^~\&|ehr_system|facility|hdim|hdim|20251201100000||ADT^A01|MSG123456|P|2.5
EVN||A01|20251201100000
PID|||12345^^^MRN||Doe^John||19900101|M
...

Processing:
1. Parse HL7 message
2. Extract patient demographics
3. Transform to FHIR Patient resource
4. Update database
5. Publish domain event
6. Trigger workflows
```

### Configuration
- EDI/VAN for secure transmission
- MLLP (Minimal Lower Layer Protocol) wrapper
- Character encoding: ASCII, UTF-8
- Message validation: MSH segment verification

## Vendor-Specific Integrations

### Epic Systems
**Integration Method**: FHIR API + HL7 v2

**Features**:
- Epic FHIR R4 endpoint access
- Patient search via Chart Search API
- In-basket notifications for care gaps
- Smart App integration support

**Setup**:
1. Register application with Epic App Orchard
2. Obtain OAuth credentials
3. Deploy SMART app in Epic environment
4. Configure webhook endpoints
5. Enable in-basket notifications

### Cerner/Oracle Health
**Integration Method**: FHIR APIs + CDS Hooks

**Features**:
- Cerner FHIR endpoints (Millennium API)
- CDS Hooks for clinical decision support
- Care gap alerts in care provider workflow
- HL7 v2 messaging support

**Setup**:
1. Request Cerner developer account
2. Register application in Cerner sandbox
3. Implement CDS Hooks endpoints
4. Configure clinical workflows
5. Deploy to production

### Athena
**Integration Method**: REST API + Events

**Features**:
- Athenahealth REST APIs
- Real-time event subscriptions
- Patient list synchronization
- Care gap tracking in portal

**Setup**:
1. OAuth 2.0 credentials from Athena
2. Configure API base URL (https://api.athenahealth.com)
3. Subscribe to patient events
4. Map Athena patient IDs to HDiM
5. Deploy webhooks

### Allscripts
**Integration Method**: FHIR + HL7 v2

**Features**:
- Allscripts FHIR endpoints
- Direct integration for secure messaging
- Chart integration via custom portlets
- Care gap alerts in workflow

## Data Synchronization Strategy

### Master Patient Index (MPI) Matching
When importing patient data from EHR:

```
1. Extract patient demographics (name, DOB, SSN, MRN)
2. Check existing patient records in HDiM
3. Apply probabilistic matching algorithm:
   - Exact MRN match (100%)
   - First + last + DOB match (95%)
   - First + last + DOB partial (85%)
   - Last + DOB match (75%)
4. If match found: Link/update patient
5. If no match: Create new patient record
6. Handle duplicates: Merge records (keep parent ID)
```

**Configuration**:
- Matching threshold: 75% (configurable)
- Manual review queue for 75-85% matches
- Duplicate consolidation daily
- Audit trail of merges

### Conflict Resolution
When data conflicts occur:

```
Source Data              → Conflict Rules          → Result
EHR medication list        Take newer timestamp     Use EHR
  vs. HDiM medication
EHR diagnosis date       Take EHR source            Use EHR
  vs. HDiM date          (authoritative)
HDiM care gap status     Take most recent change    Merge
  vs. EHR encounter
```

## Data Transformation

### FHIR Transformation Engine
Convert from various formats to FHIR R4:

**CSV to FHIR**:
```yaml
Mapping:
  PatientID: → Patient.id
  FirstName: → Patient.name[0].given
  LastName: → Patient.name[0].family
  DateOfBirth: → Patient.birthDate
  LabValue: → Observation.value[x]
  LabDate: → Observation.effectiveDateTime
  LabCode: → Observation.code.coding[0].code
```

**HL7 v2 to FHIR**:
```
ORM^O01 message:
  PID segment → Patient resource
  OBR segment → ServiceRequest resource
  OBX segment → Observation resource
  Mapped with XSD transformation
```

**Athena CSV to FHIR**:
```
Import file: patients_2025-12-01.csv
Columns: PATID, FIRST, LAST, DOB, SSN, MRN
Transform to FHIR Patient resources
Validate against StructureDefinition
Bulk insert
```

## API Documentation & SDKs

### OpenAPI Specification
Full OpenAPI 3.0 specification available at `/api/docs/openapi.json`

**Interactive Documentation**:
- Swagger UI at https://hdim.example.com/api/docs
- Try-it-out functionality with test credentials
- Endpoint descriptions, parameters, response examples
- Code generation: Python, JavaScript, Java, C#, Go

### Client SDKs
Pre-built libraries available:

| Language | Package | Features |
|----------|---------|----------|
| Python | `hdim-fhir` | Full API coverage, async support |
| JavaScript | `@hdim/fhir-client` | Browser & Node.js, TypeScript |
| Java | `com.hdim:fhir-client` | Spring integration, reactive |
| C# | `HealthDataInMotion.FhirClient` | .NET Framework, async/await |

### Example: Python SDK
```python
from hdim.fhir import Client

client = Client(
    base_url="https://api.hdim.example.com",
    credentials=OAuth2Credentials(
        client_id="your_client_id",
        client_secret="your_client_secret"
    )
)

# Get patient
patient = client.get_patient("patient-123")

# Search observations
observations = client.search_observations(
    patient_id="patient-123",
    code="loinc:2345-7",
    date_from="2025-01-01",
    date_to="2025-12-01"
)

# Submit batch
bundle = FhirBundle(
    entries=[
        FhirPatient(...),
        FhirObservation(...)
    ]
)
response = client.submit_bundle(bundle)
```

## Error Handling & Resilience

### Retry Strategy
```
Failed request → Wait 1 sec → Retry 1
  → Failed → Wait 2 sec → Retry 2
  → Failed → Wait 4 sec → Retry 3
  → Failed → Log error, alert ops, dead-letter queue
```

**Retriable Errors**:
- Network timeouts (5xx, 0ms response)
- Rate limiting (429)
- Database locked (5 minutes max)

**Non-Retriable**:
- Invalid authentication (401)
- Insufficient permissions (403)
- Invalid data format (400)

### Circuit Breaker Pattern
If EHR integration fails:
```
Failures > 5 in 5 minutes
  → Open circuit (reject requests)
  → Wait 60 seconds
  → Test endpoint
  → If working: Close circuit
  → If still failing: Remain open, retry in 60s
```

## Compliance & Security

### HIPAA Compliance
- **Encryption**: TLS 1.2+ in transit, AES-256 at rest
- **Access Control**: OAuth 2.0 + MFA for sensitive operations
- **Audit Logging**: All access logged and immutable
- **Minimal Necessary**: Query only data required
- **Business Associate Agreements**: In place with all integrators

### Data Validation
- FHIR schema validation (StructureDefinition compliance)
- Business rule validation (no future dates, valid codes)
- PII validation (no direct identifiers in certain fields)
- Data quality scoring

### API Security
- Rate limiting: 10,000 req/hour per API key
- IP whitelisting: Optional for B2B integrations
- API versioning: Backward compatibility guaranteed
- Penetration testing: Quarterly assessments

## Integration Testing

### Sandbox Environment
- Full replica of production
- Test data included (1,000 synthetic patients)
- Webhook testing tools available
- Performance testing allowed

### Test Scenarios
```
1. Happy path: Submit valid data → Processed successfully
2. Validation failure: Invalid data → Rejection with error details
3. Duplicate handling: Existing patient → Merge or update
4. Data transformation: Legacy format → FHIR conversion
5. Rate limiting: Exceed quota → 429 response
6. Failure recovery: Network error → Automatic retry
```

## Performance Characteristics

| Operation | Latency (p95) | Throughput | Example |
|-----------|---------------|-----------|---------|
| Patient lookup | 50ms | 1,000 req/sec | Single patient retrieval |
| Observation search | 200ms | 500 req/sec | Lab results for patient |
| Batch submission | 5s | 100 req/min | 1,000 records per request |
| Bulk export | 30s | 10 concurrent | 100K patient records |
| Event webhook | 100ms | 10K events/sec | Real-time observation |

## Troubleshooting & Support

### Common Integration Issues

**Issue**: Authentication failures
- **Solution**: Verify OAuth token, check clock synchronization, refresh credentials

**Issue**: Data not appearing
- **Solution**: Check webhook delivery (confirm HMAC signature), verify MPI matching, check audit log

**Issue**: Slow batch imports
- **Solution**: Reduce batch size, increase parallelism, check database CPU

**Issue**: Duplicate patients
- **Solution**: Adjust MPI matching threshold, manually merge records, review match logs

### Support Resources
- **Documentation**: https://docs.hdim.example.com
- **API Sandbox**: https://sandbox.hdim.example.com
- **Status Page**: https://status.hdim.example.com
- **Support Portal**: https://support.hdim.example.com
- **Email**: integrations@hdim.example.com

---

## Conclusion

HealthData in Motion's flexible, standards-based integration approach works with virtually any EHR system through multiple integration patterns. Whether through FHIR APIs, batch imports, real-time webhooks, or vendor-specific connectors, data flows securely into the platform while maintaining HIPAA compliance and data integrity.

The modular design allows starting simple (batch imports) and evolving to real-time streaming as sophistication increases, without requiring major architectural changes.

[Content to be added by content writers]

## Overview

This document is a placeholder for content to be written by the documentation team.

## Status

- Status: Draft
- Owner: Engineering
- Last Updated: 2025-12-01

