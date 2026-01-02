# CMS Connector Service

**Phase 1: Foundation & Architecture** | [PHASE-1-FOUNDATION.md](../../../../../../PHASE-1-FOUNDATION.md)

Integrates HDIM with Centers for Medicare & Medicaid Services (CMS) APIs for real-time Medicare quality measurement and beneficiary data access.

## Features

### Supported CMS APIs

1. **BCDA** (Beneficiary Claims Data API)
   - Weekly bulk export of Medicare Part A, B, D claims
   - NDJSON format (newline-delimited JSON)
   - For: ACOs, IPPs, Multi-payer organizations
   - Data lag: 5-10 days

2. **DPC** (Data at Point of Care)
   - Real-time claim queries during patient encounters
   - <500ms latency target
   - For: Individual providers, EHR vendors
   - FHIR REST API

3. **Blue Button 2.0**
   - Beneficiary-initiated data sharing
   - For: Individuals, SMART apps, research
   - OAuth2 beneficiary consent

4. **AB2D** (Medicare Part D Claims)
   - Bulk Part D claims for PDP sponsors
   - For: Prescription drug plans
   - Medication adherence measures

5. **QPP Submissions**
   - Real-time quality measure submissions
   - For: MIPS-eligible providers

## Architecture

### Core Components

```
CMS Connector Service
├── Auth Layer
│   └── OAuth2Manager (token lifecycle, refresh, caching)
├── API Clients
│   ├── BcdaClient (bulk exports)
│   ├── DpcClient (real-time queries)
│   ├── BlueButtonClient (beneficiary consent)
│   ├── Ab2dClient (Part D claims)
│   └── QppClient (submissions)
├── Data Layer
│   ├── CmsClaim (FHIR ExplanationOfBenefit storage)
│   ├── CmsIntegrationConfig (tenant configuration)
│   └── Repositories (data access)
├── FHIR Layer
│   ├── FhirBundleParser (NDJSON parsing)
│   ├── FhirValidator (data quality)
│   └── FhirTransformer (to HDIM schema)
└── Integration Layer
    ├── CMS Sync Service (bulk import scheduling)
    ├── CMS Cache Service (Redis)
    └── CMS Data Quality Service (validation)
```

### Data Flow

```
CMS API (BCDA/DPC)
    ↓ OAuth2 Bearer Token
CMS Connector Service
    ↓ FHIR Bundle
FHIR Parser + Validator
    ↓ Normalized Claims
PostgreSQL (cms_claims table)
    ↓ Indexed by patient/measure
Redis Cache (TTL-based)
    ↓ Query-optimized
CQL Engine (measure evaluation)
    ↓
Clinical Insights Dashboard
```

## Phase 1 Implementation (Weeks 1-4)

### ✅ Week 1: Foundation (COMPLETE)

**Completed Tasks:**
- [x] CMS API credentials acquisition (placeholder - requires manual CMS registration)
- [x] OAuth2 flow design and documentation
- [x] FHIR data model mapping (ExplanationOfBenefit → Patient, Condition, Procedure, Observation)
- [x] Project scaffolding and structure setup
- [x] Application class and dependency configuration
- [x] CMS API provider enumeration
- [x] OAuth2Manager implementation (token lifecycle)
- [x] BCDA and DPC client stubs
- [x] CmsClaim and CmsIntegrationConfig entities
- [x] Repository interfaces (CmsClaimRepository, CmsIntegrationConfigRepository)
- [x] Configuration files (application.yml, application-test.yml)

**Deliverables:**
```
backend/modules/services/cms-connector-service/
├── build.gradle.kts (Spring Boot + HAPI FHIR deps)
├── project.json (Nx configuration)
├── README.md (this file)
├── src/main/java/com/healthdata/cms/
│   ├── CmsConnectorServiceApplication.java
│   ├── model/
│   │   ├── CmsApiProvider.java
│   │   ├── CmsClaim.java
│   │   └── CmsIntegrationConfig.java
│   ├── auth/
│   │   └── OAuth2Manager.java
│   ├── client/
│   │   ├── BcdaClient.java
│   │   ├── DpcClient.java
│   │   └── (BlueButtonClient, Ab2dClient, QppClient - Week 2)
│   ├── repository/
│   │   ├── CmsClaimRepository.java
│   │   └── CmsIntegrationConfigRepository.java
│   └── (config/, service/, exception/ - Week 2)
├── src/main/resources/
│   └── application.yml
└── src/test/resources/
    └── application-test.yml
```

### ⏳ Week 2: OAuth2 & API Clients (NEXT)

**Tasks:**
- [ ] Build OAuth2Manager token exchange logic
- [ ] Implement CMS API client wrappers (REST calls)
- [ ] Create token refresh scheduler (50-minute interval)
- [ ] Unit tests for OAuth2 flow
- [ ] Implement remaining CMS API clients (BlueButton, AB2D, QPP)

### ⏳ Week 3: FHIR Parsing & Validation (TBD)

**Tasks:**
- [ ] Build FHIR bundle parser for NDJSON
- [ ] Implement data validation pipeline
- [ ] Add duplicate detection (content hash)
- [ ] Test with 100+ CMS sandbox claims
- [ ] Integration testing (end-to-end)

### ⏳ Week 4: Architecture Review & Completion (TBD)

**Tasks:**
- [ ] Complete integration tests
- [ ] Prepare architecture review document
- [ ] Performance benchmarking
- [ ] Go/no-go decision for Phase 2

## Configuration

### Environment Variables

```bash
# CMS OAuth2
CMS_SANDBOX_MODE=true
CMS_OAUTH2_CLIENT_ID=your-client-id
CMS_OAUTH2_CLIENT_SECRET=your-client-secret

# Database
DB_USER=postgres
DB_PASSWORD=postgres
REDIS_HOST=localhost
REDIS_PORT=6380

# API Base URLs (override defaults if needed)
CMS_BCDA_BASE_URL=https://sandbox.bcda.cms.gov
CMS_DPC_BASE_URL=https://sandbox.dpc.cms.gov
CMS_BLUE_BUTTON_BASE_URL=https://sandbox.bluebutton.cms.gov
```

### Database Setup

```sql
-- Create CMS-specific schema
CREATE SCHEMA cms_data;

-- Create tables (Flyway migrations)
-- migrations/V001__Create_cms_claims.sql
-- migrations/V002__Create_cms_integration_config.sql
```

## Building

```bash
# Build the service
./gradlew :modules:services:cms-connector-service:build

# Run tests
./gradlew :modules:services:cms-connector-service:test

# Build Docker image
./gradlew :modules:services:cms-connector-service:bootBuildImage

# Run locally
java -jar build/libs/cms-connector-service-1.0.0.jar
```

## Testing

### Unit Tests

```bash
./gradlew :modules:services:cms-connector-service:test
```

### Integration Tests (Week 3)

- FHIR bundle parsing with real CMS test data
- OAuth2 token refresh cycle
- Claim deduplication
- Data quality validation

### Sandbox Testing (Week 3)

- 100+ BCDA test claims
- DPC real-time queries
- Blue Button beneficiary consent flow
- AB2D Part D claims

## API Endpoints (Week 2)

```
POST   /api/v1/cms/integration/config          - Configure CMS integration
GET    /api/v1/cms/integration/config          - Get current configuration
POST   /api/v1/cms/claims/import/bcda          - Trigger BCDA bulk import
GET    /api/v1/cms/claims/import/status        - Get import status
GET    /api/v1/cms/patient/{patientId}/claims  - Real-time patient claims (DPC)
GET    /api/v1/cms/claims                      - Query claims (paginated)
GET    /api/v1/cms/claims/{claimId}            - Get specific claim
POST   /api/v1/cms/claims/validate             - Validate claims
GET    /api/v1/cms/health                      - Service health
```

## Security Considerations

### OAuth2 Token Management
- Client credentials flow (application-to-application)
- Token stored in Redis with encrypted credentials in Vault
- Automatic refresh 5 minutes before expiry
- Support for multiple CMS providers with separate tokens

### Data Security
- HIPAA compliance: Encrypted claims in transit and at rest
- Field-level encryption for sensitive data (patient IDs, SSNs)
- Audit logging of all data access
- Multi-tenant data isolation
- Row-level security (RLS) in PostgreSQL

### API Security
- OAuth2 bearer token validation on all endpoints
- Rate limiting per tenant
- Request/response logging with PII redaction
- CORS configuration for gateway

## Performance Targets

| Metric | Target |
|--------|--------|
| DPC Query Latency | <500ms p95 |
| BCDA Bulk Import | <4 hours for 500K claims |
| Cache Hit Rate | >80% |
| Data Accuracy | >99.5% |
| Error Rate | <0.1% |
| API Uptime | 99.9% |

## Dependencies

### Core
- Spring Boot 3.2.x
- Spring Data JPA
- Spring Security + OAuth2
- Spring Cloud OpenFeign
- PostgreSQL driver
- Redis client (Lettuce)

### FHIR
- HAPI FHIR R4 (org.hl7.fhir.r4)
- HAPI FHIR Client
- Jackson for JSON processing

### Resilience
- Resilience4j (circuit breaker, retry)
- Micrometer (metrics)

### Testing
- JUnit 5
- Mockito
- TestContainers
- WireMock
- Spring Test

## License

Part of HDIM (HealthData-in-Motion) platform. See parent LICENSE.

## Support

For questions or issues:
1. Check PHASE-1-FOUNDATION.md for implementation details
2. Review CMS API documentation: https://developer.cms.gov/
3. Contact HDIM development team

---

**Status**: Phase 1 Foundation Complete ✅  
**Next**: Phase 1 Week 2 - OAuth2 & API Client Implementation  
**Timeline**: On schedule for Phase 2 integration by end of Week 8
