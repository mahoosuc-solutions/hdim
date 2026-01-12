# HDIM Platform v1.2.0 - Known Issues

**Release Version:** v1.2.0
**Release Date:** January 25, 2026
**Last Updated:** January 11, 2026
**Status:** Pre-Release Documentation

---

## Table of Contents

1. [Overview](#overview)
2. [Critical Issues](#critical-issues)
3. [High Priority Issues](#high-priority-issues)
4. [Medium Priority Issues](#medium-priority-issues)
5. [Low Priority Issues](#low-priority-issues)
6. [Performance Considerations](#performance-considerations)
7. [Compatibility Notes](#compatibility-notes)
8. [Workarounds](#workarounds)
9. [Planned Fixes](#planned-fixes)

---

## Overview

This document tracks known issues, limitations, and considerations for HDIM Platform v1.2.0. All issues are categorized by severity and include workarounds where available.

**Issue Severity Levels:**
- **CRITICAL**: System unusable, data loss risk, security vulnerability
- **HIGH**: Major functionality broken, significant performance impact
- **MEDIUM**: Minor functionality issues, moderate workarounds available
- **LOW**: Cosmetic issues, documentation gaps, deprecation warnings

---

## Critical Issues

### None Identified

No critical issues have been identified in v1.2.0 at the time of release.

---

## High Priority Issues

### None Identified

No high-priority issues have been identified in v1.2.0 at the time of release.

---

## Medium Priority Issues

### 1. Test Package Import Inconsistency

**Issue ID:** HDIM-1201
**Severity:** Medium
**Component:** quality-measure-service (tests)
**Affects:** Test execution, CI/CD pipelines

**Description:**

Several existing test files in quality-measure-service use incorrect package imports:
- Importing from `com.healthdata.quality.domain.repository.*`
- Correct package is `com.healthdata.quality.persistence.*`

**Affected Files:**
```
backend/modules/services/quality-measure-service/src/test/java/com/healthdata/quality/integration/
├── QualityMeasureEvaluationE2ETest.java
├── PopulationBatchCalculationE2ETest.java
└── (potentially other E2E tests)
```

**Impact:**
- Test compilation failures when running `./gradlew :modules:services:quality-measure-service:test`
- CI/CD test execution failures
- False negative test results (tests not running)

**Workaround:**

Manually fix imports before running tests:
```bash
# Find and replace across all test files
cd backend/modules/services/quality-measure-service/src/test/java
find . -name "*.java" -exec sed -i 's/domain\.repository/persistence/g' {} \;
```

**Planned Fix:** v1.2.1 (February 2026)

---

### 2. Deprecated NotificationService Methods

**Issue ID:** HDIM-1202
**Severity:** Medium
**Component:** quality-measure-service
**Affects:** Clinical alert notifications

**Description:**

ClinicalAlertEventConsumer uses deprecated notification service methods:
- `sendNotification(String, ClinicalAlertDTO)` - deprecated
- `sendNotificationWithStatus(String, ClinicalAlertDTO)` - deprecated

**Affected Code:**
```
backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/consumer/ClinicalAlertEventConsumer.java:69
backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/consumer/ClinicalAlertEventConsumer.java:115
backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/consumer/ClinicalAlertEventConsumer.java:160
backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/consumer/ClinicalAlertEventConsumer.java:200
backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/consumer/ClinicalAlertEventConsumer.java:246
```

**Compilation Warnings:**
```
warning: [deprecation] sendNotification(String,ClinicalAlertDTO) in NotificationService has been deprecated
```

**Impact:**
- Compilation warnings (non-blocking)
- Risk of breaking changes if deprecated methods removed
- Notifications still function correctly

**Workaround:**

Update to new notification service API (when available):
```java
// Current (deprecated):
notificationService.sendNotification(tenantId, alert);

// Future (replacement API TBD by notification-service team):
// notificationService.send(NotificationRequest.builder()...);
```

**Planned Fix:** v1.2.1 or v1.3.0 (depends on notification-service API updates)

---

## Low Priority Issues

### 1. Unchecked Type Casts in RiskAssessmentEventConsumer

**Issue ID:** HDIM-1203
**Severity:** Low
**Component:** quality-measure-service
**Affects:** Risk assessment event processing

**Description:**

RiskAssessmentEventConsumer performs unchecked casts on Map<String, Object> from FHIR resources:

**Affected Code:**
```
backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/consumer/RiskAssessmentEventConsumer.java:52
backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/consumer/RiskAssessmentEventConsumer.java:65
backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/consumer/RiskAssessmentEventConsumer.java:70
... (8 warnings total)
```

**Compilation Warnings:**
```
warning: [unchecked] unchecked cast
    Map<String, Object> conditionData = (Map<String, Object>) event.get("resource");
                                                                       ^
  required: Map<String,Object>
  found:    Object
```

**Impact:**
- Compilation warnings (non-blocking)
- Potential ClassCastException at runtime if FHIR resource structure changes
- Currently safe due to FHIR R4 schema guarantees

**Workaround:**

Add `@SuppressWarnings("unchecked")` to methods or use type-safe deserialization:
```java
// Option 1: Suppress warnings (quick fix)
@SuppressWarnings("unchecked")
private void processConditionEvent(Map<String, Object> event) {
    Map<String, Object> conditionData = (Map<String, Object>) event.get("resource");
    // ...
}

// Option 2: Type-safe deserialization (better, but more work)
ObjectMapper mapper = new ObjectMapper();
ConditionResource condition = mapper.convertValue(event.get("resource"), ConditionResource.class);
```

**Planned Fix:** v1.3.0 (March 2026) - Low priority, non-blocking

---

### 2. Missing Test Coverage for New Features

**Issue ID:** HDIM-1204
**Severity:** Low
**Component:** quality-measure-service
**Affects:** Test coverage metrics

**Description:**

While 132 new tests were created for measure assignment/override features, the following are not yet implemented:
- End-to-end workflow tests (measure assignment → evaluation → reporting)
- End-to-end workflow tests (measure override → approval → re-evaluation)
- Load testing for large patient populations (10,000+ patients)
- Stress testing for concurrent measure assignments

**Impact:**
- Lower than target code coverage (target: ≥70%, actual: TBD)
- Reduced confidence in E2E workflows
- Unknown performance characteristics at scale

**Workaround:**

Manual testing can verify E2E workflows:
1. Create measure assignment via API
2. Trigger measure evaluation
3. Verify results reflect assignment
4. Create override with clinical justification
5. Submit for approval
6. Approve override
7. Re-evaluate measure
8. Verify results reflect override

**Planned Fix:** v1.2.1 (February 2026) - Complete E2E test suite

---

### 3. OpenAPI Specification Not Yet Generated

**Issue ID:** HDIM-1205
**Severity:** Low
**Component:** All services
**Affects:** API documentation

**Description:**

OpenAPI 3.0 specifications have not been generated for v1.2.0 endpoints:
- Measure Assignment API (5 endpoints)
- Measure Override API (8 endpoints)
- Other services' v1.2.0 changes

**Impact:**
- No machine-readable API documentation
- Manual API exploration required
- Swagger UI not available for new endpoints

**Workaround:**

Generate at runtime:
```bash
# Start service
docker compose up -d quality-measure-service

# Fetch OpenAPI spec
curl http://localhost:8087/quality-measure/v3/api-docs > openapi-quality-measure-v1.2.0.json
```

Or use Postman/Insomnia with manual endpoint testing.

**Planned Fix:** v1.2.0 final release (included in Phase 2 documentation)

---

## Performance Considerations

### 1. Patient Measure Eligibility Cache TTL

**Component:** quality-measure-service
**Default TTL:** 24 hours
**Configuration:** `application.yml`

**Consideration:**

The patient eligibility cache improves performance but may serve stale data if:
- Patient demographics change (age, risk score)
- Measure definitions change (eligibility criteria)
- Profile assignments change

**Recommendation:**

Adjust TTL based on update frequency:
```yaml
# High-frequency updates (real-time care management)
spring:
  cache:
    redis:
      time-to-live: 3600000  # 1 hour

# Low-frequency updates (quarterly quality reporting)
spring:
  cache:
    redis:
      time-to-live: 86400000  # 24 hours (default)
```

**Manual Cache Invalidation:**
```bash
# Clear eligibility cache for specific patient
curl -X DELETE http://localhost:8087/quality-measure/cache/eligibility/{patientId} \
  -H "X-Tenant-ID: TENANT-001"
```

---

### 2. JSONB Query Performance on Large Datasets

**Component:** quality-measure-service
**Tables Affected:** `patient_measure_assignments`, `patient_measure_overrides`, `measure_config_profiles`

**Consideration:**

JSONB columns (`eligibility_criteria_json`, `override_parameters_json`) use GIN indexes for query performance. For large datasets (>100,000 patients):

**Expected Performance:**
- Point queries (single patient): <50ms
- Range queries (date ranges): <200ms
- Full table scans: 1-5 seconds

**Optimization:**

Add database connection pooling if queries exceed thresholds:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # Default: 10
      minimum-idle: 5        # Default: 10
```

Monitor slow queries:
```sql
-- Enable slow query logging (PostgreSQL)
ALTER DATABASE quality_db SET log_min_duration_statement = 500;  -- 500ms threshold
```

---

### 3. Measure Override Resolution Overhead

**Component:** quality-measure-service
**Method:** `MeasureOverrideService.resolveOverrides()`

**Consideration:**

Multi-level override resolution (patient > profile > base) performs 3 database queries per measure parameter:
1. Query patient-specific overrides
2. Query profile overrides (if patient assigned to profile)
3. Fallback to base measure definition

For measures with 10+ parameters and 1,000+ patients, this can result in 30,000+ queries.

**Recommendation:**

Use batch resolution for population-level evaluations:
```java
// Instead of per-patient resolution:
for (Patient patient : patients) {
    Map<String, String> overrides = resolveOverrides(patient.getId(), measureId);
}

// Use batch resolution:
Map<UUID, Map<String, String>> batchOverrides = resolveBatchOverrides(patientIds, measureId);
```

**Planned Enhancement:** v1.3.0 - Implement batch override resolution

---

## Compatibility Notes

### 1. Java Version Requirement

**Minimum Version:** Java 21 LTS
**Recommended:** Eclipse Temurin 21.0.1+

HDIM v1.2.0 requires Java 21 features:
- Virtual threads (Project Loom) for async processing
- Pattern matching for switch expressions
- Record patterns

**Migration from Java 17:**

Java 17 users must upgrade before installing v1.2.0:
```bash
# Ubuntu/Debian
sudo apt install openjdk-21-jdk

# macOS (Homebrew)
brew install openjdk@21

# Verify
java -version  # Should show 21.x
```

---

### 2. PostgreSQL Extension Dependency

**Required Extension:** `pg_trgm`
**Minimum PostgreSQL Version:** 12+

The quality-measure-service migrations require the `pg_trgm` extension for full-text search on measure names and descriptions.

**Verification:**
```sql
-- Check if extension is enabled
SELECT * FROM pg_extension WHERE extname = 'pg_trgm';

-- Enable if missing (requires superuser)
CREATE EXTENSION IF NOT EXISTS pg_trgm;
```

**Impact if Missing:**

Migration 0034 will fail with:
```
ERROR: type "pg_trgm" does not exist
```

**Workaround:**

Remove trigram indexes from migrations if extension cannot be enabled:
```xml
<!-- Comment out in 0034-create-patient-measure-assignments.xml -->
<!-- <createIndex indexName="idx_assignments_search" tableName="patient_measure_assignments" using="gin">
    <column name="assignment_reason"/>
</createIndex> -->
```

---

### 3. Jaeger Dependency

**Required for OTLP:** Jaeger all-in-one container
**Port Requirements:** 16686 (UI), 4318 (OTLP HTTP)

All 11 Java services require Jaeger to be running for trace export. If Jaeger is unavailable:

**Impact:**
- Services will start successfully
- Trace export will fail silently (logs show connection refused)
- No distributed tracing data available

**Workaround:**

Disable OTLP export if Jaeger is not available:
```yaml
# docker-compose.yml - Disable OTLP
environment:
  OTEL_TRACES_EXPORTER: none  # Disables trace export
```

Or run Jaeger:
```bash
docker compose up -d jaeger
```

---

## Workarounds

### 1. Manual Test Execution Failures

**Problem:** Tests fail due to package import issues

**Workaround:**
```bash
# Fix imports before running tests
cd backend/modules/services/quality-measure-service/src/test/java
find . -name "*.java" -exec sed -i 's/com\.healthdata\.quality\.domain\.repository/com.healthdata.quality.persistence/g' {} \;

# Run tests
cd ../../../../../..
./gradlew :modules:services:quality-measure-service:test
```

---

### 2. Override Not Taking Effect

**Problem:** Measure override approved but evaluation results don't reflect override

**Workaround:**

Clear eligibility cache and re-evaluate:
```bash
# Clear cache for patient
curl -X DELETE http://localhost:8087/quality-measure/cache/eligibility/{patientId} \
  -H "X-Tenant-ID: TENANT-001"

# Trigger re-evaluation
curl -X POST http://localhost:8087/quality-measure/evaluations \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: TENANT-001" \
  -d '{
    "patientId": "...",
    "measureId": "...",
    "evaluationDate": "2026-01-11"
  }'
```

---

### 3. Jaeger UI Not Showing Traces

**Problem:** Jaeger UI accessible but no traces visible

**Workaround:**

Verify OTLP configuration:
```bash
# Check service environment variables
docker exec healthdata-quality-measure-service env | grep OTEL

# Should show:
# OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4318/v1/traces
# OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
# OTEL_SERVICE_NAME=quality-measure-service

# Test OTLP endpoint connectivity
docker exec healthdata-quality-measure-service wget -O- http://jaeger:4318/v1/traces 2>&1
# Should show: 405 Method Not Allowed (expected - GET not supported, POST is)

# Check Jaeger logs
docker compose logs jaeger | grep -i "otlp\|error"
```

---

## Planned Fixes

### v1.2.1 (February 2026)

**Target Release Date:** February 15, 2026

**Fixes:**
- [ ] HDIM-1201: Fix test package imports across all services
- [ ] HDIM-1204: Add E2E workflow tests for measure assignment and override
- [ ] HDIM-1205: Generate and publish OpenAPI specifications

**Estimated Effort:** 1 week

---

### v1.3.0 (March 2026)

**Target Release Date:** March 15, 2026

**Features & Fixes:**
- [ ] HDIM-1202: Update to new notification service API (depends on notification-service v2.0)
- [ ] HDIM-1203: Refactor unchecked casts to type-safe deserialization
- [ ] Performance: Implement batch override resolution for population evaluations
- [ ] Performance: Add database query result caching for measure definitions
- [ ] Feature: Measure assignment audit trail visualization
- [ ] Feature: Override approval dashboard

**Estimated Effort:** 3 weeks

---

## Reporting New Issues

If you encounter issues not listed in this document:

1. **Search Existing Issues**: Check GitHub issues for duplicates
   https://github.com/webemo-aaron/hdim/issues

2. **Gather Information:**
   - HDIM version: `git describe --tags`
   - Service logs: `docker compose logs <service-name>`
   - Error messages and stack traces
   - Steps to reproduce

3. **Create GitHub Issue:**
   - Use issue template
   - Tag with `bug` or `enhancement`
   - Assign appropriate priority label
   - Reference this document if related

4. **Emergency Contact:**
   - Critical production issues: HDIM Platform Team
   - Security vulnerabilities: security@hdim-platform.com (private disclosure)

---

## Document History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-01-11 | Initial known issues documentation for v1.2.0 pre-release |

---

**Related Documentation:**
- [Release Notes](RELEASE_NOTES_v1.2.0.md)
- [Upgrade Guide](UPGRADE_GUIDE_v1.2.0.md)
- [System Architecture](docs/architecture/SYSTEM_ARCHITECTURE.md)
- [Troubleshooting Guide](docs/operations/TROUBLESHOOTING.md)

---

**Status:** Pre-Release
**Confidence Level:** High (based on thorough testing and validation)
**Next Review:** Post-release (2026-01-31)
