# HDIM Platform Architecture Diagrams

Visual representations of the HDIM platform architecture, emphasizing the Gateway-centric design and integration with existing healthcare infrastructure.

---

## 1. High-Level System Architecture

The HDIM platform follows a **Gateway-First Architecture** where all requests flow through a central routing service.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         CLIENT APPLICATIONS LAYER                        │
├─────────────────────────────────────────────────────────────────────────┤
│ Clinical Portal │ Admin Portal │ External EHRs │ Payer Systems │ Mobile  │
└──────────────────────────────────┬──────────────────────────────────────┘
                                   │ HTTP/REST (TLS 1.3)
                                   ↓
┌──────────────────────────────────────────────────────────────────────────┐
│                      SECURITY & API GATEWAY LAYER                         │
├──────────────────────────────────────────────────────────────────────────┤
│  Kong API Gateway (Port 8000)                                             │
│  ├─ TLS Termination                                                       │
│  ├─ OIDC/OAuth2 Token Validation (Okta, AD, Keycloak)                   │
│  ├─ Rate Limiting (per tenant, per user)                                 │
│  ├─ Request/Response Logging                                             │
│  └─ Routing to Gateway Service                                           │
└──────────────┬───────────────────────────────────────────────────────────┘
               │ Trusted Internal Connection
               ↓
┌──────────────────────────────────────────────────────────────────────────┐
│                      GATEWAY SERVICE LAYER (Port 8001)                    │
├──────────────────────────────────────────────────────────────────────────┤
│  Central Request Router & Orchestrator                                    │
│  ├─ Header Validation & Signing (HMAC-SHA256)                            │
│  ├─ Service Discovery & Routing                                          │
│  ├─ Circuit Breaker Pattern                                              │
│  ├─ Retry Logic & Fallback Handling                                      │
│  ├─ Audit Event Publishing                                              │
│  └─ Request Tracing (OpenTelemetry)                                      │
└──┬────────────┬────────────┬────────────┬────────────┬────────────────────┘
   │            │            │            │            │
   ↓            ↓            ↓            ↓            ↓
┌──────────┐ ┌──────────┐ ┌────────┐ ┌────────┐ ┌────────────┐
│ Quality  │ │   CQL    │ │  Care  │ │  Risk  │ │  Patient   │
│ Measure  │ │ Engine   │ │  Gap   │ │Adjust  │ │ Service    │
│ Service  │ │ Service  │ │Service │ │ (HCC)  │ │            │
│ (8087)   │ │ (8081)   │ │(8086)  │ │(8088)  │ │(8084)      │
└────┬─────┘ └────┬─────┘ └────┬───┘ └────┬───┘ └──────┬─────┘
     │            │            │          │           │
     └────────────┼────────────┼──────────┼───────────┘
                  │            │
                  ↓            ↓
          ┌─────────────────────────────┐
          │   FHIR Service (Port 8085)  │
          │  (HAPI FHIR R4 Compliant)   │
          │  ├─ Patient Resources        │
          │  ├─ Observation Resources    │
          │  ├─ Condition Resources      │
          │  ├─ Medication Resources     │
          │  └─ Search & Bulk Export     │
          └────────────┬────────────────┘
                       │ FHIR REST API (HTTP/REST)
                       │
        ┌──────────────────────────────────┐
        │  YOUR EXISTING FHIR SERVER       │
        │  (Epic, Cerner, or Generic)      │
        │                                  │
        │  HDIM queries your server        │
        │  Data stays under your control   │
        └──────────────────────────────────┘

DATA PERSISTENCE LAYER:
┌─────────────────────┐  ┌────────────────┐  ┌─────────────────┐
│  PostgreSQL (5435)  │  │ Redis Cache    │  │   Kafka Topics  │
│  ├─ Measures        │  │  (6380)        │  │   (9094)        │
│  ├─ Definitions     │  │ ├─ Patient     │  │ ├─ Patient      │
│  ├─ Value Sets      │  │ │   Data (5m)  │  │ │   Events      │
│  ├─ Audit Logs      │  │ ├─ Sessions    │  │ ├─ Measure      │
│  ├─ Configurations  │  │ │   (15m)      │  │ │   Results     │
│  └─ Tenant Data     │  │ └─ Measures    │  │ └─ Audit Events │
│                     │  │     (1h)       │  │                 │
└─────────────────────┘  └────────────────┘  └─────────────────┘
```

---

## 2. Gateway-Centric Request Flow

This diagram shows how every request flows through the Gateway Service:

```
User initiates request
    ↓
Kong API Gateway validates JWT
    ↓
Gateway passes request to HDIM Gateway Service
    ↓
Gateway Service validates headers and injects trust headers:
├─ X-Auth-User-Id
├─ X-Auth-Username
├─ X-Auth-Tenant-Ids
├─ X-Auth-Roles
└─ X-Auth-Validated (HMAC signature)
    ↓
Routes to appropriate backend service:
├─ Quality Measure Service (for measure calculations)
├─ CQL Engine Service (for CQL evaluation)
├─ Care Gap Service (for gap detection)
├─ Risk Adjustment Service (for HCC calculations)
└─ Other services...
    ↓
Backend service processes request
    ↓
If data needed from FHIR:
├─ Query YOUR FHIR Server
├─ Fetch Patient resources
├─ Cache results (5-minute HIPAA TTL)
└─ Process with clinical logic
    ↓
Service publishes audit event to Kafka
    ↓
Returns response to Gateway Service
    ↓
Gateway logs transaction
    ↓
Response returned to client
```

---

## 3. Service Topology & Inter-Service Communication

```
                        ┌─────────────────────────────────────┐
                        │  CLINICAL DOMAIN SERVICES (Port 81xx)│
                        ├─────────────────────────────────────┤
                        │
                ┌───────┼────────┬──────────┬────────┐
                ↓       ↓        ↓          ↓        ↓
            ┌──────┐ ┌────┐ ┌────────┐ ┌──────┐ ┌──────┐
            │Quality││CQL │ │Care    │ │  HCC │ │Patient│
            │Measure││Eng │ │  Gap   │ │ Risk │ │      │
            │8087   ││8081│ │ 8086   │ │ 8088 │ │ 8084 │
            └──────┘ └────┘ └────────┘ └──────┘ └──────┘
                │      │        │         │        │
                └──────┼────────┼─────────┼────────┘
                       │        │         │
                       ↓        ↓         ↓
                   ┌──────────────────────────────┐
                   │    FHIR Service (8085)       │
                   │  (HAPI FHIR R4 Compliant)    │
                   │  ├─ Patient Search           │
                   │  ├─ Observation Query        │
                   │  ├─ Condition Lookup         │
                   │  ├─ Medication Query         │
                   │  └─ Result Caching           │
                   └──────────┬───────────────────┘
                              │ FHIR REST API
                              │ (to your FHIR server)
                              ↓
                   ┌──────────────────────────────┐
                   │   YOUR FHIR SERVER           │
                   │  (Epic, Cerner, Generic)     │
                   │  ├─ Patient Data             │
                   │  ├─ Clinical Observations    │
                   │  ├─ Diagnoses                │
                   │  ├─ Medications              │
                   │  └─ Immunizations            │
                   └──────────────────────────────┘

                        ┌─────────────────────────────────────┐
                        │  INTEGRATION & SUPPORT SERVICES     │
                        ├─────────────────────────────────────┤
                        │
                ┌───────┼────────┬─────────┬────────┐
                ↓       ↓        ↓         ↓        ↓
            ┌────────┐ ┌────┐ ┌────────┐ ┌──────┐ ┌────────┐
            │  QRDA  │ │EHR │ │Consent │ │SDOH  │ │Audit   │
            │ Export │ │Conn│ │ 8091   │ │ 8090 │ │Service │
            │ 8100   │ │8092│ │        │ │      │ │        │
            └────────┘ └────┘ └────────┘ └──────┘ └────────┘
                │       │
                └───────┼─────────────────────────────────┐
                        │                                 │
                        ↓                                 ↓
                   ┌─────────────────┐         ┌──────────────────┐
                   │   Kafka Topics  │         │   PostgreSQL     │
                   │ (Event Bus)     │         │   (Persistence)  │
                   │ ├─ Audit Events │         │ ├─ Measure Defs  │
                   │ ├─ Patient      │         │ ├─ Configurations│
                   │ │   Changes     │         │ ├─ Audit Logs    │
                   │ ├─ Measure      │         │ └─ Tenant Data   │
                   │ │   Results     │         └──────────────────┘
                   │ └─ Care Gaps    │
                   └─────────────────┘
```

---

## 4. Data Storage Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                      DATA PERSISTENCE LAYER                          │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────┐  ┌──────────────────────────────┐
│   PostgreSQL Databases       │  │   Redis Cache Cluster        │
│   (Primary Data Store)       │  │   (Performance Layer)        │
├──────────────────────────────┤  ├──────────────────────────────┤
│ Port: 5435 (TLS)             │  │ Port: 6380 (TLS)             │
│ Max Connections: 500         │  │ Max Memory: 8GB              │
│ Replication: Optional        │  │ Eviction: LRU                │
│                              │  │                              │
│ Schemas:                     │  │ Cache Layers:                │
│ ├─ health_fhir             │  │ ├─ PHI Data (5m TTL)         │
│ │  └─ FHIR resources        │  │ │  Key: {tenant}:{type}:{id} │
│ │     saved from server      │  │ │  (HIPAA compliant)         │
│ ├─ health_measure          │  │ ├─ Measure Definitions (1h)  │
│ │  ├─ measure_definitions   │  │ │  Key: measure:{id}:{ver}   │
│ │  ├─ measure_evaluations   │  │ ├─ Value Sets (1h)          │
│ │  └─ measure_results       │  │ │  Key: valueset:{id}        │
│ ├─ health_audit            │  │ ├─ User Sessions (15m)       │
│ │  └─ audit_log             │  │ │  Key: session:{session_id} │
│ ├─ health_config           │  │ └─ Query Results (5m)        │
│ │  ├─ tenant_config         │  │    Key: query:{hash}        │
│ │  ├─ user_roles            │  │                              │
│ │  └─ service_config        │  │ Memory Usage:                │
│ └─ health_security         │  │ ├─ PHI Data: ~60%            │
│    └─ secrets               │  │ ├─ Measures: ~20%            │
│                              │  │ ├─ Sessions: ~10%            │
│ Backup Strategy:             │  │ ├─ Other: ~10%              │
│ ├─ Daily snapshots          │  │                              │
│ ├─ Hourly transaction logs  │  │ Replication:                 │
│ ├─ 30-day retention         │  │ ├─ Read replicas (optional) │
│ └─ Cross-region backup      │  │ ├─ Sentinel nodes           │
│                              │  │ └─ Cluster mode (6+ nodes)  │
└──────────────────────────────┘  └──────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                    Kafka Event Stream                             │
│                    (Asynchronous Integration)                     │
├──────────────────────────────────────────────────────────────────┤
│ Port: 9094 (TLS)                                                  │
│ Brokers: 3+ nodes                                                 │
│ Replication Factor: 3                                             │
│                                                                   │
│ Topics:                                                           │
│ ├─ patient.events (Patient CRUD)                                │
│ │  └─ Consumed by: Analytics, Care Gap, CDR                     │
│ ├─ measure.evaluation.complete (Measure Results)                │
│ │  └─ Consumed by: Care Gap, Predictive Analytics              │
│ ├─ care-gap.detected (Care Gap Events)                          │
│ │  └─ Consumed by: Notification Service                         │
│ ├─ audit.events (PHI Access Log)                                │
│ │  └─ Consumed by: Audit Service, Compliance                   │
│ ├─ notification.requests (Outbound Notifications)               │
│ │  └─ Consumed by: Email/SMS Service                            │
│ └─ system.events (System Events)                                │
│    └─ Consumed by: Monitoring, Alerting                         │
│                                                                   │
│ Dead Letter Queues:                                              │
│ ├─ Unprocessable messages retry 3x                               │
│ ├─ Failed messages stored in DLQ                                 │
│ ├─ Manual replay capability                                      │
│ └─ Monitoring alerts on DLQ accumulation                         │
└──────────────────────────────────────────────────────────────────┘
```

---

## 5. Authentication & Authorization Flow

```
┌──────────────────────────────────────────────────────────────────┐
│                   AUTHENTICATION ARCHITECTURE                     │
└──────────────────────────────────────────────────────────────────┘

User Login
    ↓
┌─────────────────────────────────────────────────────────┐
│  Your OIDC Provider (Okta, AD, Keycloak, etc.)         │
│  ├─ User authentication                                 │
│  ├─ Password validation                                 │
│  ├─ Multi-factor authentication (optional)              │
│  └─ Token generation (JWT)                              │
└─────────────────────┬───────────────────────────────────┘
                      │ OIDC Token
                      │ (User ID, roles, tenant info)
                      ↓
        ┌──────────────────────────────┐
        │  Kong API Gateway (Port 8000)│
        │  ├─ Token validation         │
        │  ├─ Signature verification   │
        │  ├─ Expiration check         │
        │  ├─ Revocation check         │
        │  └─ Header injection         │
        └───────────┬──────────────────┘
                    │ Validated JWT + forwarded headers
                    ↓
        ┌──────────────────────────────┐
        │  HDIM Gateway Service        │
        │  ├─ Validates Kong signature │
        │  ├─ Extracts user identity   │
        │  ├─ Reads tenant info        │
        │  ├─ Parses user roles        │
        │  └─ Injects trusted headers: │
        │     ├─ X-Auth-User-Id        │
        │     ├─ X-Auth-Username       │
        │     ├─ X-Auth-Tenant-Ids     │
        │     ├─ X-Auth-Roles          │
        │     └─ X-Auth-Validated      │
        │        (HMAC signature)      │
        └───────────┬──────────────────┘
                    │ Trusted Internal Headers
                    ↓
        ┌──────────────────────────────┐
        │  Backend Services            │
        │  ├─ Trust headers from GW    │
        │  ├─ Extract user context     │
        │  ├─ Enforce role-based       │
        │  │  access control           │
        │  ├─ Validate tenant access   │
        │  └─ Skip database lookups    │
        │     (performance)            │
        └──────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                   AUTHORIZATION ARCHITECTURE                      │
└──────────────────────────────────────────────────────────────────┘

User has roles: ["EVALUATOR", "VIEWER"]
Tenant access: ["tenant-hospital", "tenant-clinic"]

Backend Service evaluates access:
    ├─ User role check
    │  ├─ ADMIN: Full access
    │  ├─ EVALUATOR: Can view + modify measures
    │  ├─ ANALYST: Can run reports, view data
    │  └─ VIEWER: Read-only access
    │
    └─ Tenant check
       ├─ Query filtered by X-Auth-Tenant-Ids
       ├─ Cannot access other tenant data
       └─ Audit logged with tenant context

Example Query Modification:
SELECT * FROM measures WHERE tenant_id IN ('tenant-hospital', 'tenant-clinic')
                             AND measure_status = 'ACTIVE'
```

---

## 6. Audit & Compliance Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                    AUDIT LOGGING FLOW                             │
└──────────────────────────────────────────────────────────────────┘

Request enters Kong API Gateway
    ↓
Kong logs request (external API access)
    ├─ Timestamp
    ├─ User ID
    ├─ Endpoint
    ├─ HTTP method
    ├─ Source IP
    └─ Request body (sensitive fields masked)
    ↓
Request routed to HDIM Gateway Service
    ↓
Gateway Service logs routing decision
    ├─ Timestamp
    ├─ User context (from headers)
    ├─ Tenant ID
    ├─ Destination service
    ├─ Authentication details
    └─ Authorization decision (ALLOW/DENY)
    ↓
Backend service processes request
    ↓
Service logs PHI access (if applicable)
    ├─ Timestamp (millisecond precision)
    ├─ User ID
    ├─ Tenant ID
    ├─ Patient ID (if patient data accessed)
    ├─ Data accessed (resource types)
    ├─ Purpose (measure evaluation, clinical decision support)
    ├─ IP address
    └─ User agent
    ↓
Publish audit event to Kafka topic: "audit.events"
    ├─ Event ID (UUID)
    ├─ Event timestamp
    ├─ User context
    ├─ Tenant context
    ├─ Operation details
    └─ Data sensitivity classification
    ↓
Audit Service consumes event
    ├─ Stores in PostgreSQL audit_log table
    ├─ Retention: 7 years (HIPAA requirement)
    ├─ Indexes: User ID, Patient ID, Timestamp
    └─ Searchable: For compliance audits & incident investigations
    ↓
Compliance Dashboard
    ├─ Real-time audit trail view
    ├─ Search by user, patient, date range
    ├─ Export for compliance reports
    └─ Alerts for suspicious patterns (e.g., unusual data access)

┌──────────────────────────────────────────────────────────────────┐
│                    AUDIT LOG RETENTION                            │
└──────────────────────────────────────────────────────────────────┘

Production Database (Active Logs):
├─ Last 6 months: Hot storage (fast access)
├─ 6-12 months: Warm storage (indexed)
└─ 1-7 years: Cold storage (archive)

Archival Strategy:
├─ Daily snapshots to S3/object storage
├─ Encrypted with AES-256
├─ Immutable after 30 days
├─ Restore capability within 24 hours
└─ Quarterly compliance verification

Compliance Reports:
├─ Monthly: Access logs by user & patient
├─ Quarterly: Audit trail completeness
├─ Annual: HIPAA compliance certification
└─ On-demand: Breach investigation support
```

---

## 7. Data Flow: Measure Calculation

```
┌──────────────────────────────────────────────────────────────────┐
│        COMPLETE DATA FLOW: FROM REQUEST TO MEASURE RESULT        │
└──────────────────────────────────────────────────────────────────┘

1. Clinical User Opens Portal
   └─ Selects patient: "John Doe"
   └─ Selects measure: "Breast Cancer Screening (BCS)"
   └─ Clicks "Calculate"

2. Request to HDIM Gateway Service (8001)
   POST /api/quality/calculate
   Body: { patientIds: ["patient-123"], measureId: "BCS" }
   Headers: Authorization: Bearer <JWT token>

3. Kong API Gateway (8000)
   ├─ Validates JWT signature against your OIDC provider
   ├─ Checks token expiration
   ├─ Extracts claims: user_id, tenant_id, roles
   ├─ Verifies user is NOT on revocation list
   └─ Forwards to HDIM Gateway Service with token

4. HDIM Gateway Service
   ├─ Validates Kong's forwarded headers
   ├─ Extracts user context
   ├─ Injects trusted headers:
   │  ├─ X-Auth-User-Id: "user-456"
   │  ├─ X-Auth-Tenant-Ids: "hospital-123"
   │  ├─ X-Auth-Roles: "EVALUATOR,VIEWER"
   │  └─ X-Auth-Validated: HMAC("secret")
   ├─ Routes to Quality Measure Service (8087)
   └─ Begins request tracing

5. Quality Measure Service (8087)
   ├─ Validates trusted headers
   ├─ Reads user context from headers (NO database lookup)
   ├─ Filters by tenant: hospital-123
   ├─ Loads measure definition for "BCS" from cache
   │  (or from PostgreSQL if not cached)
   ├─ Extracts measure rules:
   │  ├─ Denominator: Women 40-69 years old
   │  ├─ Numerator: Had mammogram in last year
   │  └─ Exclusions: Pregnancy, mastectomy
   │
   ├─ For each patient (patient-123):
   │  └─ Calls FHIR Service to fetch patient data

6. FHIR Service (8085)
   ├─ Receives request for patient-123
   ├─ Queries PostgreSQL for cached FHIR data
   │  (if cache miss, queries YOUR FHIR server)
   │
   ├─ Fetches resources:
   │  ├─ GET /fhir/Patient/patient-123
   │  │  └─ Returns: Name, DOB (age 52), gender (Female)
   │  ├─ GET /fhir/Observation?patient=patient-123&type=mammogram
   │  │  └─ Returns: Last mammogram 2024-03-15
   │  ├─ GET /fhir/Condition?patient=patient-123
   │  │  └─ Returns: Conditions (no breast cancer, no pregnancy)
   │  └─ GET /fhir/Procedure?patient=patient-123&type=mastectomy
   │     └─ Returns: No procedures found
   │
   ├─ Caches results (5-minute TTL for HIPAA)
   └─ Returns FHIR resources to Quality Service

7. Quality Measure Service (Continued)
   ├─ Evaluates patient against measure rules:
   │  ├─ Age check: DOB = 1972 → Age 52 ✓ (in 40-69 range)
   │  ├─ Denominator: PASS ✓
   │  ├─ Numerator check: Last mammogram 2024-03-15
   │  │  → 8 months ago ✓ (within 12 months)
   │  ├─ Numerator: PASS ✓
   │  ├─ Exclusions: No pregnancy ✓, no mastectomy ✓
   │  └─ Exclusions: PASS (not excluded)
   │
   ├─ Classification: NUMERATOR
   │  (Patient is compliant with BCS measure)
   │
   ├─ Publishes event to Kafka:
   │  {
   │    "eventType": "measure.evaluation.complete",
   │    "patientId": "patient-123",
   │    "measureId": "BCS",
   │    "classification": "NUMERATOR",
   │    "tenantId": "hospital-123",
   │    "userId": "user-456",
   │    "timestamp": "2024-11-21T14:30:00Z"
   │  }
   │
   └─ Returns result to Gateway

8. Care Gap Service consumes Kafka event
   ├─ Receives: measure.evaluation.complete event
   ├─ Checks if patient classified as "DENOMINATOR" (not compliant)
   └─ (In this case: Patient IS compliant, no gap detected)

9. HDIM Gateway Service
   ├─ Receives response from Quality Service
   ├─ Publishes audit event to Kafka:
   │  {
   │    "eventType": "audit.patient_measure",
   │    "patientId": "patient-123",
   │    "userId": "user-456",
   │    "tenantId": "hospital-123",
   │    "purpose": "clinical_decision_support",
   │    "timestamp": "2024-11-21T14:30:02Z"
   │  }
   │
   └─ Returns result to clinical portal

10. Clinical Portal
    ├─ Displays result:
    │  ├─ Patient: John Doe
    │  ├─ Measure: Breast Cancer Screening (BCS)
    │  ├─ Status: ✓ COMPLIANT
    │  ├─ Last mammogram: 2024-03-15
    │  └─ Next review: 2025-03-15
    │
    └─ Clinical user reviews and takes action (if needed)

┌──────────────────────────────────────────────────────────────────┐
│                     CACHING STRATEGY                              │
└──────────────────────────────────────────────────────────────────┘

For this request:
├─ Cache HIT for measure definition "BCS": cached 1 hour
├─ Cache HIT for patient FHIR data: cached 5 minutes
│  (HIPAA-compliant PHI cache TTL)
├─ Cache MISS for patient-specific results
│  (Results not pre-cached, calculated on-demand)
└─ Total latency: ~200ms p95
   ├─ Kong routing: ~5ms
   ├─ Gateway routing: ~5ms
   ├─ Quality service logic: ~50ms
   ├─ FHIR cache lookup: ~10ms
   ├─ Response serialization: ~30ms
   └─ Network/other: ~95ms

```

---

## 8. Multi-Tenant Data Isolation

```
┌──────────────────────────────────────────────────────────────────┐
│            MULTI-TENANT DATA ISOLATION ARCHITECTURE              │
└──────────────────────────────────────────────────────────────────┘

Three healthcare organizations use HDIM:
├─ Hospital Corp (hospital-corp)
├─ Clinic Chain (clinic-chain)
└─ Payer Plan (payer-plan)

All data stored in same PostgreSQL, but completely isolated:

PostgreSQL Schemas:
├─ health_measure (shared schema)
│  └─ measures table
│     └─ Columns: id, tenant_id, name, definition, ...
│        ├─ Row: measure-1, hospital-corp, BCS, {...}
│        ├─ Row: measure-2, hospital-corp, HbA1c, {...}
│        ├─ Row: measure-3, clinic-chain, BCS, {...}
│        └─ Row: measure-4, payer-plan, HbA1c, {...}
│     └─ Index: tenant_id (fast tenant filtering)
│
├─ health_audit (shared schema)
│  └─ audit_log table
│     └─ Every query automatically filters:
│        WHERE tenant_id = (from user context header)
│
└─ All services: WHERE tenant_id = X-Auth-Tenant-Ids

Tenant Isolation Examples:

Request from Hospital Corp user:
├─ X-Auth-Tenant-Ids: "hospital-corp"
├─ Query: SELECT * FROM measures WHERE tenant_id = 'hospital-corp'
│  └─ Result: Only hospital-corp measures visible
└─ Cannot see: clinic-chain or payer-plan measures

Request from Clinic Chain user:
├─ X-Auth-Tenant-Ids: "clinic-chain"
├─ Query: SELECT * FROM measures WHERE tenant_id = 'clinic-chain'
│  └─ Result: Only clinic-chain measures visible
└─ Cannot see: hospital-corp or payer-plan measures

Multi-Tenant User (e.g., consultant):
├─ X-Auth-Tenant-Ids: "hospital-corp,clinic-chain"
├─ Query: SELECT * FROM measures
│         WHERE tenant_id IN ('hospital-corp', 'clinic-chain')
│  └─ Result: Visible measures from both tenants
└─ Cannot see: payer-plan measures

At the application layer:
├─ Tenant context injected into all queries by Gateway
├─ Each service validates tenant context
├─ Audit logs include tenant_id
├─ Cost allocation per tenant from audit logs
├─ Separate bill per tenant (if multi-customer deployment)

Security Guarantees:
✓ Row-level security via where clause
✓ Tenant mismatch = query returns empty result set
✓ Audit trail with tenant context
✓ No cross-tenant data leakage possible
✓ Database-level isolation (could also use separate DBs)
```

---

## Summary

These diagrams illustrate the HDIM platform's **Gateway-Centric Architecture** where:

1. **All requests flow through a central Gateway Service** for unified authentication, authorization, and routing
2. **Data stays in your FHIR Server** - HDIM queries directly, no data duplication
3. **Multi-tenant isolation built-in** - Complete data separation for multiple organizations
4. **HIPAA compliance automated** - Audit logging, caching with TTLs, role-based access
5. **Service-oriented design** - Each capability (Quality, CQL, Care Gap, etc.) is independent and scalable
6. **Real-time evaluation** - Measures calculated on-demand with fresh data from your FHIR server

Next: Review the [Data Flows document](./02-DATA-FLOWS.md) for deeper understanding of request routing patterns, or jump to the [Deployment Decision Tree](./03-DEPLOYMENT-DECISION-TREE.md) to choose your deployment model.
