# High-Priority TODO Remediation Plan

**Date Created**: January 2, 2026
**Last Updated**: January 2, 2026
**Total Issues**: 16 high-priority TODOs
**Estimated Effort**: 3-4 weeks (developer-dependent)

---

## Executive Summary

This document outlines a detailed remediation plan for all high-priority TODOs in the HDIM project. The plan is organized into three categories:

1. **CMS Connector Service** - 8+ TODOs (critical for CMS data integration)
2. **Security Architecture - Tenant Isolation** - 3 TODOs (critical for multi-tenant compliance)
3. **Feature TODOs** - 5+ TODOs (medium priority, feature completeness)

The plan includes implementation order, dependencies, complexity assessment, and success criteria.

---

## Category 1: CMS Connector Service Implementation (Week 2 Placeholders)

### Overview
The CMS Connector service contains placeholder implementations for OAuth2 and CMS API client integrations. These are critical for connecting to CMS data sources (DPC and BCDA).

**Files Affected**:
- `backend/modules/services/cms-connector-service/src/main/java/com/healthdata/cms/auth/OAuth2Manager.java`
- `backend/modules/services/cms-connector-service/src/main/java/com/healthdata/cms/client/DpcClient.java`
- `backend/modules/services/cms-connector-service/src/main/java/com/healthdata/cms/client/BcdaClient.java`

**Dependencies**:
- CMS API Documentation (DPC, BCDA specs)
- OAuth2 client credentials configuration
- PostgreSQL (for claim storage)
- Kafka (for event streaming)

### Task 1.1: OAuth2 Manager - Token Exchange Implementation

**Status**: TODO
**Complexity**: MEDIUM
**Estimated Time**: 2-3 days

**Description**:
Implement actual OAuth2 token exchange to authenticate with CMS services.

**Current State**:
```java
// TODO: Week 2 - Implement actual OAuth2 token exchange
public String obtainAccessToken() {
    // PLACEHOLDER
}
```

**Requirements**:
- [ ] Implement OAuth2 client credential flow
- [ ] Support token refresh mechanism
- [ ] Cache tokens in Redis with TTL
- [ ] Log token exchanges (audit trail)
- [ ] Handle CMS OAuth2 endpoints (DPC, BCDA)
- [ ] Unit tests for token exchange scenarios
- [ ] Integration test with mock CMS OAuth2 server

**Success Criteria**:
- Obtains valid access tokens from CMS OAuth2 endpoint
- Tokens are cached and reused until expiration
- Failed token exchanges trigger proper error handling
- Audit logs capture all token operations

**Related Tests**:
- `OAuth2ManagerTest.java` (unit tests)
- `CmsAuthenticationIntegrationTest.java` (integration tests)

---

### Task 1.2: DPC Client - Patient Retrieval Implementation

**Status**: TODO
**Complexity**: MEDIUM
**Estimated Time**: 2-3 days

**Description**:
Implement patient data retrieval from CMS Data and Post-acute Care (DPC) API.

**Current State**:
```java
// TODO: Week 2 - Implement patient retrieval
public List<Patient> retrievePatients(String organizationId) {
    // PLACEHOLDER
}
```

**Requirements**:
- [ ] Query DPC API for patients by organization
- [ ] Parse DPC patient FHIR resources
- [ ] Map DPC patient data to internal Patient entity
- [ ] Handle pagination for large patient lists
- [ ] Store retrieved patients in PostgreSQL
- [ ] Implement retry logic for transient failures
- [ ] Unit tests for patient mapping logic
- [ ] Integration tests with mock DPC server

**Success Criteria**:
- Successfully retrieves patients from DPC API
- FHIR patient resources are correctly parsed
- Data is persisted without duplicates
- Pagination works for large datasets

**Related Tests**:
- `DpcPatientRetrieverTest.java`
- `DpcIntegrationTest.java`

---

### Task 1.3: DPC Client - EOB (Explanation of Benefits) Retrieval

**Status**: TODO
**Complexity**: MEDIUM-HIGH
**Estimated Time**: 3-4 days

**Description**:
Implement EOB (Explanation of Benefits) retrieval from DPC API for claims-based quality measures.

**Current State**:
```java
// TODO: Week 2 - Implement EOB retrieval
public List<ExplanationOfBenefits> retrieveEOB(String patientId) {
    // PLACEHOLDER
}
```

**Requirements**:
- [ ] Query DPC API for EOB by patient ID
- [ ] Parse FHIR ExplanationOfBenefits resources
- [ ] Extract claims, procedures, diagnoses from EOB
- [ ] Map EOB data to internal claim entities
- [ ] Handle multiple insurance plans per patient
- [ ] Implement batch retrieval optimization
- [ ] Store EOB data in PostgreSQL (cms_eob_line_items, cms_claims tables)
- [ ] Unit tests for EOB parsing logic
- [ ] Integration tests with mock DPC server

**Success Criteria**:
- Retrieves EOB records from DPC API
- Correctly extracts claim details
- Data persists without data loss
- Supports historical EOB data (multiple years)

**Dependencies**:
- Task 1.1 (OAuth2Manager)
- cms_claims table schema

**Related Tests**:
- `DpcEobRetrieverTest.java`
- `ClaimStorageTest.java`

---

### Task 1.4: DPC Client - Clinical Data Retrieval (Conditions, Procedures, Medications, Observations)

**Status**: TODO
**Complexity**: MEDIUM-HIGH
**Estimated Time**: 4-5 days

**Description**:
Implement retrieval of clinical data (conditions, procedures, medications, observations) from DPC API.

**Current State**:
```java
// TODO: Week 2 - Implement condition retrieval
// TODO: Week 2 - Implement procedure retrieval
// TODO: Week 2 - Implement medication retrieval
// TODO: Week 2 - Implement observation retrieval
```

**Requirements for Each Resource Type**:
- [ ] **Conditions**: Query and parse FHIR Condition resources
  - Extract diagnosis codes (ICD-10-CM)
  - Map to internal Condition entity
  - Link to patients

- [ ] **Procedures**: Query and parse FHIR Procedure resources
  - Extract procedure codes (CPT, SNOMED)
  - Map to internal Procedure entity
  - Track procedure dates

- [ ] **Medications**: Query and parse FHIR MedicationRequest resources
  - Extract medication information
  - Map to internal Medication entity
  - Track medication orders and history

- [ ] **Observations**: Query and parse FHIR Observation resources
  - Extract lab results, vital signs, assessment scores
  - Map to internal Observation entity
  - Handle various value types (numeric, categorical, text)

**General Requirements**:
- [ ] Implement streaming/batch API calls
- [ ] Handle pagination for large datasets
- [ ] Deduplication logic to prevent duplicate records
- [ ] Transaction management for batch inserts
- [ ] Error handling and retry logic
- [ ] Comprehensive unit tests for each resource type
- [ ] Integration tests with mock DPC server

**Success Criteria**:
- All four clinical data types are successfully retrieved
- FHIR resources are correctly parsed
- Data is persisted accurately and completely
- No data loss or corruption during retrieval
- Performance is acceptable for large patient populations

**Dependencies**:
- Task 1.1 (OAuth2Manager)
- Entity schemas for Condition, Procedure, Medication, Observation

---

### Task 1.5: DPC Client - Health Check Implementation

**Status**: TODO
**Complexity**: LOW
**Estimated Time**: 1 day

**Description**:
Implement health check endpoint for DPC API connectivity.

**Current State**:
```java
// TODO: Week 2 - Implement health check
public boolean isDpcHealthy() {
    // PLACEHOLDER
}
```

**Requirements**:
- [ ] Implement /health or /metadata endpoint call to DPC
- [ ] Return connectivity status
- [ ] Integrate with Spring Boot health indicator
- [ ] Unit test for health check

**Success Criteria**:
- Health check correctly reports DPC API status
- Integrates with application health endpoint

**Dependencies**:
- Task 1.1 (OAuth2Manager)

---

### Task 1.6: BCDA Client - Feign Client Implementation

**Status**: TODO
**Complexity**: MEDIUM
**Estimated Time**: 2-3 days

**Description**:
Implement Feign client for BCDA (Beneficiary Claims Data API) bulk data export requests.

**Current State**:
```java
// TODO: Week 2 - Implement Feign client or RestTemplate call
```

**Requirements**:
- [ ] Create Feign client interface for BCDA endpoints
- [ ] Implement request/response interceptors
- [ ] Add request signing (if required by BCDA)
- [ ] Handle OAuth2 token injection via interceptor
- [ ] Implement error handling and retry logic
- [ ] Create integration tests with mock BCDA server
- [ ] Document BCDA API contract

**Success Criteria**:
- Feign client successfully communicates with BCDA
- OAuth2 tokens are properly injected
- Request/response logging works

**Dependencies**:
- Task 1.1 (OAuth2Manager)
- BCDA API documentation

---

### Task 1.7: BCDA Client - Export Request Implementation

**Status**: TODO
**Complexity**: MEDIUM
**Estimated Time**: 2-3 days

**Description**:
Implement bulk data export request submission to BCDA API.

**Current State**:
```java
// TODO: Week 2 - Implement export request
public ExportResponse requestExport(ExportParameters params) {
    // PLACEHOLDER
}
```

**Requirements**:
- [ ] Submit bulk data export request to BCDA
- [ ] Include patient IDs, date ranges, resource types
- [ ] Handle BCDA response with job ID
- [ ] Store export job metadata in PostgreSQL
- [ ] Return job ID for status polling
- [ ] Unit tests for export request logic
- [ ] Integration tests with mock BCDA server

**Success Criteria**:
- Export request is successfully submitted
- Job ID is returned and stored
- Job metadata is persisted correctly

**Dependencies**:
- Task 1.6 (BCDA Feign Client)
- Export job tracking table schema

---

### Task 1.8: BCDA Client - Status Polling and File Management

**Status**: TODO
**Complexity**: MEDIUM-HIGH
**Estimated Time**: 3-4 days

**Description**:
Implement export job status polling and file download from BCDA.

**Current State**:
```java
// TODO: Week 2 - Implement status polling
// TODO: Week 2 - Implement file path retrieval
// TODO: Week 2 - Implement file download
// TODO: Week 2 - Implement metadata retrieval
```

**Requirements**:
- [ ] **Status Polling**: Query BCDA for export job status
  - Implement exponential backoff for polling
  - Handle job completion events
  - Track polling attempts/errors

- [ ] **File Retrieval**: Download exported files from BCDA URLs
  - Implement secure file transfer (HTTPS)
  - Store files in secure storage (S3/local filesystem)
  - Validate file integrity (checksums)

- [ ] **Metadata Management**: Parse and store export metadata
  - Extract file counts, record counts
  - Store file manifest
  - Track export completion dates

- [ ] **Error Handling**: Handle failed exports
  - Retry failed downloads
  - Log export errors
  - Notify administrators of failures

**Success Criteria**:
- Export jobs are polled until completion
- Files are successfully downloaded and stored
- Export metadata is captured and persisted
- Failed exports are handled gracefully

**Dependencies**:
- Task 1.7 (BCDA Export Request)
- File storage infrastructure
- S3/storage bucket configuration

---

### CMS Connector Service - Implementation Order

**Phase 1 (Week 1)**:
1. Task 1.1 - OAuth2Manager
2. Task 1.5 - DPC Health Check

**Phase 2 (Week 2)**:
1. Task 1.2 - DPC Patient Retrieval
2. Task 1.3 - DPC EOB Retrieval
3. Task 1.6 - BCDA Feign Client

**Phase 3 (Week 3)**:
1. Task 1.4 - DPC Clinical Data Retrieval
2. Task 1.7 - BCDA Export Request

**Phase 4 (Week 4)**:
1. Task 1.8 - BCDA Status Polling & File Management

**Rationale**:
- OAuth2 must be done first (dependency for all CMS calls)
- DPC implementation can proceed in parallel with BCDA
- File management comes last (depends on export requests)

---

## Category 2: Security Architecture - Tenant Isolation Re-implementation

### Overview
Tenant isolation was disabled during the Gateway service centralization. Once Gateway is complete, these filters must be re-enabled in the microservices.

**Files Affected**:
- `backend/modules/services/patient-service/src/main/java/com/healthdata/patient/config/PatientSecurityConfig.java`
- `backend/modules/services/care-gap-service/src/main/java/com/healthdata/caregap/config/CareGapSecurityConfig.java`
- `backend/modules/services/fhir-service/src/main/java/com/healthdata/fhir/config/FhirSecurityConfig.java`

**Critical**: Tenant isolation is required for HIPAA compliance. Multi-tenant data leaks are severe security violations.

### Task 2.1: Patient Service - Tenant Isolation Re-implementation

**Status**: TODO (Blocked by Gateway completion)
**Complexity**: MEDIUM
**Estimated Time**: 2 days

**Description**:
Re-enable TenantAccessFilter in Patient Service security configuration.

**Current State**:
- TenantAccessFilter exists but is disabled
- Query-level tenant filtering is NOT implemented in repositories

**Requirements**:
- [ ] Re-enable TenantAccessFilter in SecurityConfig
- [ ] Add X-Tenant-ID header validation
- [ ] Implement query-level tenant filtering in all repositories
  - All Patient queries must filter by tenant_id
  - Verify no tenant_id leakage in responses
- [ ] Add unit tests for tenant isolation
- [ ] Add integration tests with multiple tenants
- [ ] Audit logging for cross-tenant access attempts

**Success Criteria**:
- All patient queries are filtered by tenant
- Cross-tenant access is rejected
- Audit logs capture all access attempts
- Integration tests verify tenant isolation

**Database Changes**:
```sql
-- Verify patient table has tenant_id column
ALTER TABLE patients ADD COLUMN tenant_id VARCHAR(255) NOT NULL DEFAULT 'DEFAULT';
-- Create index for performance
CREATE INDEX idx_patients_tenant_id ON patients(tenant_id);
```

---

### Task 2.2: Care Gap Service - Tenant Isolation Re-implementation

**Status**: TODO (Blocked by Gateway completion)
**Complexity**: MEDIUM
**Estimated Time**: 2 days

**Description**:
Re-enable TenantAccessFilter in Care Gap Service security configuration.

**Requirements**:
- [ ] Re-enable TenantAccessFilter in SecurityConfig
- [ ] Add X-Tenant-ID header validation
- [ ] Implement query-level tenant filtering in all repositories
  - All CareGap queries must filter by tenant_id
  - Verify Care gaps are not leaked between tenants
- [ ] Add unit tests for tenant isolation
- [ ] Add integration tests with multiple tenants
- [ ] Audit logging for cross-tenant access attempts

**Success Criteria**:
- All care gap queries are filtered by tenant
- Cross-tenant access is rejected
- Audit logs capture all access attempts
- Integration tests verify tenant isolation

---

### Task 2.3: FHIR Service - Tenant Isolation Re-implementation

**Status**: TODO (Blocked by Gateway completion)
**Complexity**: MEDIUM
**Estimated Time**: 2 days

**Description**:
Re-enable TenantAccessFilter in FHIR Service security configuration.

**Requirements**:
- [ ] Re-enable TenantAccessFilter in SecurityConfig
- [ ] Add X-Tenant-ID header validation
- [ ] Implement query-level tenant filtering in FHIR resource repositories
  - All FHIR queries must filter by tenant_id
  - Verify FHIR resources are not leaked between tenants
- [ ] Verify HAPI FHIR client enforces tenant isolation
- [ ] Add unit tests for tenant isolation
- [ ] Add integration tests with multiple tenants
- [ ] Audit logging for cross-tenant access attempts

**Success Criteria**:
- All FHIR queries are filtered by tenant
- Cross-tenant access is rejected
- Audit logs capture all access attempts
- Integration tests verify tenant isolation

---

### Tenant Isolation - Implementation Order

**Pre-requisite**:
- Gateway service must be completed and deployed

**Phase 1** (After Gateway completion):
1. Verify Gateway trust authentication is working correctly
2. Confirm X-Tenant-ID headers are being injected by Gateway

**Phase 2** (Same day):
1. Task 2.1 - Patient Service Tenant Isolation
2. Task 2.2 - Care Gap Service Tenant Isolation
3. Task 2.3 - FHIR Service Tenant Isolation (can be parallel)

**Testing**:
- Run existing multi-tenant integration tests
- Perform penetration testing for cross-tenant data access
- Load test with multi-tenant queries

---

## Category 3: Feature TODOs

### Task 3.1: Report Detail View - Detail View Navigation

**Status**: TODO
**Complexity**: LOW-MEDIUM
**Estimated Time**: 1-2 days

**Description**:
Implement report detail view navigation/dialog in Angular frontend.

**Location**: `phase2-worktree2-fhir/REPORT_DETAIL_VIEWER_COMPLETE.md`

**Requirements**:
- [ ] Create detail view component
- [ ] Implement navigation from report list to detail view
- [ ] Implement dialog to display report details
- [ ] Add breadcrumb navigation
- [ ] Style component to match design system
- [ ] Add unit tests for component
- [ ] Add E2E tests for navigation flow

**Success Criteria**:
- Users can navigate to report details
- Detail view displays all relevant report information
- Navigation is intuitive and matches UX guidelines

---

### Task 3.2: Quality Measure Service - Complete E2E Test Scenarios

**Status**: TODO
**Complexity**: MEDIUM
**Estimated Time**: 3-4 days

**Description**:
Add comprehensive E2E test scenarios using Testcontainers for quality measure service.

**Location**: `phase2-worktree2-fhir/backend/modules/services/quality-measure-service/TESTCONTAINERS_E2E_RESOLVED.md`

**Requirements**:
- [ ] Create additional test scenarios for each HEDIS measure
- [ ] Implement care gap calculation E2E tests
- [ ] Test data feed to quality reporting
- [ ] Performance testing with large datasets
- [ ] Error condition testing
- [ ] Documentation of test scenarios

**Success Criteria**:
- E2E test suite covers all major quality measure scenarios
- Tests pass consistently
- Performance is acceptable

---

### Task 3.3: Quality Measure Service - CI/CD Pipeline Integration

**Status**: TODO
**Complexity**: LOW
**Estimated Time**: 1 day

**Description**:
Integrate E2E tests into GitHub Actions or GitLab CI pipeline.

**Requirements**:
- [ ] Add E2E test stage to CI/CD pipeline
- [ ] Configure Docker Compose for test environment
- [ ] Set up test reporting
- [ ] Add pass/fail gates

**Success Criteria**:
- E2E tests run automatically on pull requests
- Pipeline fails if tests fail
- Test reports are visible in CI/CD UI

---

### Task 3.4: Mental Health Assessment Service - Implement Unsupported Types

**Status**: TODO
**Complexity**: LOW-MEDIUM
**Estimated Time**: 1-2 days

**Description**:
Implement handlers for currently unsupported mental health assessment types.

**Location**: `phase2-worktree2-fhir/backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/service/MentalHealthAssessmentService.java`

**Current State**:
```java
default -> throw new UnsupportedOperationException("Assessment type not yet implemented: " + type);
```

**Requirements**:
- [ ] Identify all unsupported assessment types
- [ ] Implement logic for each type
- [ ] Add unit tests for each type
- [ ] Verify assessment scores are calculated correctly

**Success Criteria**:
- All mental health assessment types are supported
- No UnsupportedOperationException is thrown
- Assessment calculations are accurate

---

### Task 3.5: Payer Workflows Service - Dashboard Financial Endpoint

**Status**: TODO
**Complexity**: MEDIUM
**Estimated Time**: 2-3 days

**Description**:
Implement dashboard financial endpoint for Payer Workflows service.

**Location**: `phase2-worktree2-fhir/backend/modules/services/payer-workflows-service/`

**Current State**:
```java
@Disabled("Dashboard financial endpoint not yet implemented")
```

**Requirements**:
- [ ] Design financial metrics data structure
- [ ] Implement backend endpoint to calculate financial metrics
- [ ] Integrate with quality measure data
- [ ] Connect to dashboard frontend
- [ ] Add unit tests
- [ ] Add integration tests

**Success Criteria**:
- Financial metrics are calculated and returned
- Dashboard displays financial data
- Metrics are accurate and up-to-date

---

## Summary Table

| Category | Task | Priority | Complexity | Est. Time | Dependencies |
|----------|------|----------|-----------|-----------|--------------|
| **CMS Connector** | OAuth2 Manager | HIGH | MEDIUM | 2-3d | None |
| **CMS Connector** | DPC Patient Retrieval | HIGH | MEDIUM | 2-3d | 1.1 |
| **CMS Connector** | DPC EOB Retrieval | HIGH | MEDIUM-HIGH | 3-4d | 1.1 |
| **CMS Connector** | DPC Clinical Data | HIGH | MEDIUM-HIGH | 4-5d | 1.1 |
| **CMS Connector** | DPC Health Check | HIGH | LOW | 1d | 1.1 |
| **CMS Connector** | BCDA Feign Client | HIGH | MEDIUM | 2-3d | 1.1 |
| **CMS Connector** | BCDA Export Request | HIGH | MEDIUM | 2-3d | 1.6 |
| **CMS Connector** | BCDA Polling & Files | HIGH | MEDIUM-HIGH | 3-4d | 1.7 |
| **Security** | Patient Service Tenant Isolation | HIGH | MEDIUM | 2d | Gateway |
| **Security** | Care Gap Service Tenant Isolation | HIGH | MEDIUM | 2d | Gateway |
| **Security** | FHIR Service Tenant Isolation | HIGH | MEDIUM | 2d | Gateway |
| **Features** | Report Detail View | MEDIUM | LOW-MEDIUM | 1-2d | None |
| **Features** | E2E Test Scenarios | MEDIUM | MEDIUM | 3-4d | None |
| **Features** | CI/CD Integration | MEDIUM | LOW | 1d | None |
| **Features** | Mental Health Assessments | MEDIUM | LOW-MEDIUM | 1-2d | None |
| **Features** | Financial Endpoint | MEDIUM | MEDIUM | 2-3d | None |

---

## Implementation Timeline

### Week 1
- **Days 1-2**: Task 1.1 (OAuth2Manager) - CRITICAL PATH
- **Days 3-4**: Task 1.5 (DPC Health Check) + Task 1.2 (DPC Patient Retrieval - starts)
- **Day 5**: Task 1.2 (continues) + Task 3.1 (Report Detail View)

### Week 2
- **Days 1-2**: Task 1.3 (DPC EOB Retrieval)
- **Days 3-4**: Task 1.6 (BCDA Feign Client) + Task 1.4 (DPC Clinical Data - starts)
- **Day 5**: Task 1.4 (continues) + Task 3.4 (Mental Health Assessments)

### Week 3
- **Days 1-2**: Task 1.7 (BCDA Export Request)
- **Days 3-4**: Task 1.8 (BCDA Polling & Files - starts)
- **Day 5**: Task 1.8 (continues) + Task 3.5 (Financial Endpoint)

### Week 4
- **Days 1-2**: Task 1.8 (completes) + Testing & Integration
- **Days 3-5**: Security Tenant Isolation (after Gateway completion)
- **Parallel**: Tasks 3.2, 3.3 (E2E Tests & CI/CD)

---

## Risk Mitigation

### High Risks

**Risk 1**: CMS API Changes or Unavailability
- **Mitigation**: Use mock CMS servers for development/testing
- **Contingency**: Have fallback to manual data import

**Risk 2**: Gateway Service Delays (blocks Tenant Isolation)
- **Mitigation**: Proceed with other tasks first
- **Contingency**: Schedule Tenant Isolation tasks after Gateway

**Risk 3**: Complex BCDA Polling Logic
- **Mitigation**: Start early, test extensively with mock servers
- **Contingency**: Implement simpler polling initially, optimize later

### Medium Risks

**Risk 4**: Performance Issues with Large Patient Datasets
- **Mitigation**: Load test early, implement pagination
- **Contingency**: Implement batch processing and caching

**Risk 5**: HIPAA Compliance Issues (Tenant Isolation)
- **Mitigation**: Security code review before merging
- **Contingency**: Penetration testing by security team

---

## Success Criteria - All TODOs

- [ ] All 8 CMS Connector tasks implemented and tested
- [ ] All 3 Tenant Isolation tasks re-enabled and tested
- [ ] All 5 Feature TODOs implemented
- [ ] 100% unit test coverage for critical paths
- [ ] Integration tests passing
- [ ] Security audit for tenant isolation (PASSED)
- [ ] Performance baseline established
- [ ] Documentation updated
- [ ] Code review completed for all changes

---

## Appendix A: Testing Strategy

### Unit Tests
- Mock all external CMS APIs
- Test error handling and retry logic
- Test data transformation logic

### Integration Tests
- Use Testcontainers for PostgreSQL/Redis
- Mock CMS APIs with WireMock or similar
- Test end-to-end workflows

### E2E Tests
- Test complete data flows from CMS to dashboard
- Verify data integrity
- Performance testing with realistic datasets

### Security Tests
- Penetration testing for cross-tenant data access
- Audit logging verification
- HIPAA compliance verification

---

## Appendix B: Database Schema Updates

### CMS Connector Tables
```sql
-- Export jobs tracking
CREATE TABLE bcda_export_jobs (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    job_id VARCHAR(255) NOT NULL,
    status VARCHAR(50),
    created_at TIMESTAMP,
    completed_at TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

-- Claims storage
CREATE TABLE cms_claims (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    patient_id VARCHAR(255),
    claim_id VARCHAR(255),
    claim_date DATE,
    amount DECIMAL(10, 2),
    created_at TIMESTAMP
);

-- EOB line items
CREATE TABLE cms_eob_line_items (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    eob_id VARCHAR(255),
    line_item_number INT,
    service_date DATE,
    amount DECIMAL(10, 2)
);
```

### Tenant Isolation - Verify Index
```sql
-- Ensure all tables have tenant_id column and index
CREATE INDEX idx_patients_tenant_id ON patients(tenant_id);
CREATE INDEX idx_care_gaps_tenant_id ON care_gaps(tenant_id);
CREATE INDEX idx_fhir_resources_tenant_id ON fhir_resources(tenant_id);
```

---

## Appendix C: Configuration Templates

### CMS Connector Configuration
```yaml
cms:
  dpc:
    base-url: https://dpc.cms.gov
    oauth2:
      client-id: ${DPC_CLIENT_ID}
      client-secret: ${DPC_CLIENT_SECRET}
      token-url: https://dpc.cms.gov/oauth/token

  bcda:
    base-url: https://bcda.cms.gov
    oauth2:
      client-id: ${BCDA_CLIENT_ID}
      client-secret: ${BCDA_CLIENT_SECRET}
      token-url: https://bcda.cms.gov/oauth/token

  polling:
    initial-delay-seconds: 30
    max-retries: 120
    backoff-multiplier: 1.5
```

---

*End of Remediation Plan*
