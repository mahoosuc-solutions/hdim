# HDIM Service Registry

## Complete Service Catalog (28 Services)

### Core Quality Measurement

#### quality-measure-service
- **Port**: 8087
- **Context**: `/quality-measure`
- **Purpose**: HEDIS measure evaluation engine
- **Key Features**:
  - Quality measure calculation
  - HEDIS measure library
  - Measure result storage
  - Population stratification
- **Dependencies**: cql-engine-service, fhir-service, patient-service
- **Database**: healthdata_qm
- **Build Status**: ✅ Passing

#### cql-engine-service
- **Port**: 8081
- **Context**: `/cql-engine`
- **Purpose**: CQL (Clinical Quality Language) evaluation
- **Key Features**:
  - CQL expression parsing
  - FHIR-based evaluation
  - Library management
  - Value set handling
- **Dependencies**: fhir-service
- **Database**: healthdata_cql
- **Build Status**: ✅ Passing

---

### Data Management

#### fhir-service
- **Port**: 8085
- **Context**: `/fhir`
- **Purpose**: FHIR R4 resource management
- **Key Features**:
  - HAPI FHIR 7.x server
  - Resource CRUD operations
  - Search parameters
  - Bundle processing
- **Supported Resources**:
  - Patient, Practitioner, Organization
  - Observation, Condition, Procedure
  - MedicationRequest, Immunization
  - Encounter, DiagnosticReport
- **Database**: healthdata_fhir
- **Build Status**: ✅ Passing

#### patient-service
- **Port**: 8084
- **Context**: `/patient`
- **Purpose**: Patient demographics and master data
- **Key Features**:
  - Patient demographics
  - Enrollment tracking
  - Attribution management
  - Identity resolution
- **Dependencies**: fhir-service
- **Database**: healthdata_patient
- **Build Status**: ✅ Passing

---

### Clinical Intelligence

#### care-gap-service
- **Port**: 8086
- **Context**: `/care-gap`
- **Purpose**: Care gap detection and tracking
- **Key Features**:
  - Gap identification
  - Gap prioritization
  - Closure tracking
  - Outreach recommendations
- **Dependencies**: quality-measure-service, patient-service
- **Database**: healthdata_caregap
- **Build Status**: ✅ Passing

#### predictive-analytics-service
- **Port**: 8089
- **Context**: `/predictive-analytics`
- **Purpose**: ML-based risk prediction
- **Key Features**:
  - Readmission prediction
  - Emergency department utilization
  - Chronic disease progression
  - Cost forecasting
- **Dependencies**: patient-service, fhir-service
- **Database**: healthdata_analytics
- **Tech**: Python ML models, TensorFlow/PyTorch

#### hcc-service
- **Port**: 8093
- **Context**: `/hcc`
- **Purpose**: HCC (Hierarchical Condition Category) risk adjustment
- **Key Features**:
  - RAF score calculation
  - Condition mapping
  - Coding gap identification
  - CMS-HCC model support
- **Dependencies**: fhir-service
- **Database**: healthdata_hcc

#### sdoh-service
- **Port**: 8098
- **Context**: `/sdoh`
- **Purpose**: Social determinants of health tracking
- **Key Features**:
  - SDOH screening
  - Resource referrals
  - Z-code documentation
  - Community resource integration
- **Dependencies**: patient-service, fhir-service
- **Database**: healthdata_sdoh

---

### Integration Services

#### ehr-connector-service
- **Port**: 8090
- **Context**: `/ehr-connector`
- **Purpose**: EHR system integration
- **Key Features**:
  - Epic integration (FHIR/HL7v2)
  - Cerner integration
  - eCW, athenahealth support
  - Data transformation
- **Dependencies**: fhir-service
- **Protocols**: FHIR R4, HL7v2, Direct messaging

#### cms-connector-service
- **Port**: 8091
- **Context**: `/cms-connector`
- **Purpose**: CMS Data at the Point of Care (DPC) integration
- **Key Features**:
  - Claims data retrieval
  - Part A/B data access
  - Beneficiary attribution
  - Bulk FHIR operations
- **Dependencies**: fhir-service
- **External API**: CMS DPC API

#### analytics-service
- **Port**: 8088
- **Context**: `/analytics`
- **Purpose**: Quality reporting and dashboards
- **Key Features**:
  - HEDIS reporting
  - Star ratings calculation
  - Dashboard metrics
  - Trend analysis
- **Dependencies**: quality-measure-service, care-gap-service
- **Database**: healthdata_analytics

#### qrda-export-service
- **Port**: 8095
- **Context**: `/qrda-export`
- **Purpose**: Quality Reporting Document Architecture (QRDA) export
- **Key Features**:
  - QRDA Category I (patient-level)
  - QRDA Category III (aggregate)
  - CMS submission format
  - PQRS reporting
- **Dependencies**: quality-measure-service
- **Standards**: QRDA I/III R1

---

### Platform Services

#### gateway-service
- **Port**: 8001
- **Context**: `/` (root)
- **Purpose**: API gateway and routing
- **Key Features**:
  - JWT validation
  - Header injection (X-Auth-*)
  - Request routing
  - Rate limiting (tenant-based)
- **Security**: HMAC signature generation
- **Build Status**: ✅ Passing

#### consent-service
- **Port**: 8092
- **Context**: `/consent`
- **Purpose**: Patient consent management
- **Key Features**:
  - Consent directives
  - HIPAA authorization
  - Opt-in/opt-out tracking
  - Consent verification
- **Dependencies**: patient-service, fhir-service
- **Database**: healthdata_consent
- **Standards**: FHIR Consent resource

#### prior-auth-service
- **Port**: 8094
- **Context**: `/prior-auth`
- **Purpose**: Prior authorization workflows
- **Key Features**:
  - PA request submission
  - Status tracking
  - Decision management
  - Appeals processing
- **Dependencies**: fhir-service
- **Standards**: FHIR Coverage, Claim resources

---

### Additional Services

#### agent-runtime-service
- **Port**: 8096
- **Context**: `/agent-runtime`
- **Purpose**: AI agent orchestration
- **Key Features**:
  - Claude API integration
  - CQL generation
  - Prompt management
  - Response streaming
- **Dependencies**: cql-engine-service
- **Tech**: Anthropic Claude 3.5 Sonnet
- **Build Status**: ✅ 84 tests passing

#### agent-builder-service
- **Port**: 8097
- **Context**: `/agent-builder`
- **Purpose**: Agent workflow configuration
- **Key Features**:
  - Workflow design
  - Prompt templates
  - Agent testing
  - Performance monitoring
- **Dependencies**: agent-runtime-service
- **Build Status**: ✅ Passing

#### documentation-service
- **Port**: 8099
- **Context**: `/documentation`
- **Purpose**: Clinical documentation support
- **Key Features**:
  - Template management
  - Note generation
  - Coding assistance
  - Quality measure documentation

#### notification-service
- **Port**: 8100
- **Context**: `/notification`
- **Purpose**: Multi-channel notifications
- **Key Features**:
  - Email notifications
  - SMS alerts
  - In-app messages
  - Notification preferences
- **Integrations**: SendGrid, Twilio

#### scheduling-service
- **Port**: 8101
- **Context**: `/scheduling`
- **Purpose**: Appointment scheduling
- **Key Features**:
  - Availability management
  - Booking workflows
  - Reminders
  - Cancellation handling

#### billing-service
- **Port**: 8102
- **Context**: `/billing`
- **Purpose**: Healthcare billing and claims
- **Key Features**:
  - Claim generation
  - EOB processing
  - Payment posting
  - Denial management

#### referral-service
- **Port**: 8103
- **Context**: `/referral`
- **Purpose**: Referral management
- **Key Features**:
  - Referral requests
  - Network lookup
  - Authorization tracking
  - Status updates

#### pharmacy-service
- **Port**: 8104
- **Context**: `/pharmacy`
- **Purpose**: Pharmacy integration
- **Key Features**:
  - E-prescribing
  - Formulary checks
  - Refill management
  - Drug interaction screening

#### imaging-service
- **Port**: 8105
- **Context**: `/imaging`
- **Purpose**: Medical imaging integration
- **Key Features**:
  - DICOM support
  - Image retrieval
  - Report association
  - PACS integration

#### lab-service
- **Port**: 8106
- **Context**: `/lab`
- **Purpose**: Laboratory data integration
- **Key Features**:
  - Lab order management
  - Result ingestion
  - Abnormal value alerting
  - LIS integration

#### population-health-service
- **Port**: 8107
- **Context**: `/population-health`
- **Purpose**: Population health management
- **Key Features**:
  - Cohort identification
  - Registry management
  - Stratification
  - Intervention tracking

#### clinical-decision-support-service
- **Port**: 8108
- **Context**: `/cds`
- **Purpose**: Clinical decision support
- **Key Features**:
  - CDS Hooks
  - Order sets
  - Clinical pathways
  - Alert management

#### telehealth-service
- **Port**: 8109
- **Context**: `/telehealth`
- **Purpose**: Telehealth encounter support
- **Key Features**:
  - Video session management
  - Remote monitoring
  - Virtual visit documentation
  - Compliance tracking

#### audit-service
- **Port**: 8110
- **Context**: `/audit`
- **Purpose**: HIPAA audit logging
- **Key Features**:
  - PHI access logging
  - User activity tracking
  - Compliance reporting
  - Audit log search

---

## Infrastructure Components

### Kong API Gateway
- **Port**: 8000
- **Purpose**: External API gateway
- **Features**:
  - Rate limiting
  - SSL termination
  - API key management
  - Plugin ecosystem

### PostgreSQL
- **Port**: 5435
- **Version**: 15
- **Databases**:
  - healthdata_qm (quality measures)
  - healthdata_cql (CQL engine)
  - healthdata_fhir (FHIR resources)
  - healthdata_patient (patient data)
  - healthdata_caregap (care gaps)
  - healthdata_analytics (reporting)
  - healthdata_auth (authentication)
  - ... (per-service databases)

### Redis
- **Port**: 6380
- **Version**: 7
- **Usage**:
  - Cache (5-minute TTL for PHI)
  - Session storage
  - Rate limiting counters
  - Distributed locks

### Apache Kafka
- **Port**: 9094
- **Version**: 3.x
- **Topics**:
  - quality-measure-results
  - care-gap-events
  - patient-updates
  - fhir-resource-changes
  - audit-logs

### Prometheus
- **Port**: 9090
- **Purpose**: Metrics collection
- **Metrics**:
  - Service health
  - Request latency
  - Cache hit rates
  - Database connections

### Grafana
- **Port**: 3001
- **Purpose**: Monitoring dashboards
- **Dashboards**:
  - Service overview
  - Database performance
  - Cache metrics
  - Business KPIs

### HashiCorp Vault
- **Port**: 8200
- **Purpose**: Secrets management
- **Secrets**:
  - Database credentials
  - API keys
  - JWT signing keys
  - HMAC secrets

---

## Service Communication Patterns

### Synchronous (REST)
```
Client → Gateway → Backend Service → Database
```

### Asynchronous (Kafka)
```
Service A → Kafka Topic → Service B
```

### Cache Pattern
```
Service → Redis (check) → Database (if miss) → Redis (update)
```

---

## Port Allocation Rules

| Range | Purpose |
|-------|---------|
| 8000-8010 | Gateways and proxies |
| 8081-8099 | Core services |
| 8100-8110 | Support services |
| 5435 | PostgreSQL |
| 6380 | Redis |
| 9090-9099 | Monitoring |

---

## Service Dependencies Map

```
gateway-service (8001)
  └─> quality-measure-service (8087)
        └─> cql-engine-service (8081)
              └─> fhir-service (8085)
  └─> patient-service (8084)
        └─> fhir-service (8085)
  └─> care-gap-service (8086)
        └─> quality-measure-service (8087)
        └─> patient-service (8084)
```

---

## Quick Commands by Service

### Start Individual Service
```bash
# Backend (from backend/ directory)
./gradlew :modules:services:quality-measure-service:bootRun

# Docker
docker compose up -d quality-measure-service
```

### View Service Logs
```bash
docker compose logs -f quality-measure-service
```

### Test Service
```bash
./gradlew :modules:services:quality-measure-service:test
```

### Health Check
```bash
curl http://localhost:8087/actuator/health
```

---

## Service Status Dashboard

| Service | Build | Tests | Docker | Notes |
|---------|-------|-------|--------|-------|
| quality-measure-service | ✅ | ✅ | ✅ | Core service |
| cql-engine-service | ✅ | ✅ | ✅ | Core service |
| fhir-service | ✅ | ✅ | ✅ | Core service |
| patient-service | ✅ | ✅ | ✅ | Core service |
| care-gap-service | ✅ | ✅ | ✅ | Core service |
| gateway-service | ✅ | ✅ | ✅ | Core service |
| agent-runtime-service | ✅ | ✅ (84) | ✅ | AI integration |
| agent-builder-service | ✅ | ✅ | ✅ | AI integration |
| ... | ✅ | ✅ | ✅ | All 28 services |

---

## Resources

- **Architecture**: `docs/architecture/SYSTEM_ARCHITECTURE.md`
- **Service Details**: `docs/services/` (per-service documentation)
- **API Contracts**: `backend/modules/shared/api-contracts/`
