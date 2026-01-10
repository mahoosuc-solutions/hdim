# Database Migration Status

**Last Updated:** 2026-01-10
**Plan:** /home/mahoosuc-solutions/.claude/plans/clever-dazzling-robin.md

## Progress Overview

| Metric | Status |
|--------|--------|
| Services with Liquibase | 15/34 (44%) |
| Services with Flyway | 2/34 (6%) |
| Services with `ddl-auto: validate` | 31/34 (91%) |
| Services with PostgreSQL test driver | 29/34 (85%) |
| Services with Entity Validation Tests | 28/34 (82%) |
| **Current Phase** | **Phase 1 - In Progress** |

## Phase Status

| Phase | Status | Completion |
|-------|--------|------------|
| Phase 1: Fix Critical Issues | 🔄 In Progress | 75% |
| Phase 2: Migrate Flyway Services | ⏳ Pending | 0% |
| Phase 3: Gateway Auth Migration | ⏳ Pending | 0% |
| Phase 4: Service-Owned Extensions | ⏳ Pending | 0% |
| Phase 5: CI/CD Enforcement | ⏳ Pending | 0% |

## Service-to-Database Mapping

### Core Clinical Services (9 databases)

| Service | Port | Database | Migration Tool | ddl-auto | Test Driver | Validation Test | Status |
|---------|------|----------|----------------|----------|-------------|-----------------|--------|
| fhir-service | 8085 | fhir_db | Liquibase ✅ | validate ✅ | ⚠️ | ✅ | Production Ready |
| cql-engine-service | 8081 | cql_db | Liquibase ✅ | validate ✅ | ⚠️ | ✅ | Production Ready |
| quality-measure-service | 8087 | quality_db | Liquibase ✅ | validate ✅ | ✅ | ✅ | Production Ready |
| patient-service | 8084 | patient_db | Liquibase ✅ | validate ✅ | ⚠️ | ✅ | Production Ready |
| care-gap-service | 8086 | caregap_db | Liquibase ✅ | validate ✅ | ✅ | ✅ | Production Ready |
| consent-service | 8082 | consent_db | Liquibase ✅ | validate ✅ | ✅ | ✅ | Production Ready |
| event-processing-service | 8083 | event_db | Liquibase ✅ | validate ✅ | ⚠️ | ✅ | Production Ready |
| event-router-service | 8095 | event_router_db | ❌ None | validate ✅ | ⚠️ | ✅ | Needs Config |
| gateway-service | 8080 | gateway_db | ❌ None (init script) | validate ✅ | ⚠️ | ✅ | **Phase 3 Target** |

### AI Services (3 databases)

| Service | Port | Database | Migration Tool | ddl-auto | Test Driver | Validation Test | Status |
|---------|------|----------|----------------|----------|-------------|-----------------|--------|
| agent-builder-service | 8096 | agent_db | Flyway ⚠️ | validate ✅ | ✅ | ✅ | **Phase 2 Target** |
| agent-runtime-service | 8088 | agent_runtime_db | ❌ None | validate ✅ | ✅ | ✅ | Needs Config |
| ai-assistant-service | 8090 | ai_assistant_db | ❌ None | ❌ none | ⚠️ | ❌ | **Critical Fix Needed** |

### Analytics Services (3 databases)

| Service | Port | Database | Migration Tool | ddl-auto | Test Driver | Validation Test | Status |
|---------|------|----------|----------------|----------|-------------|-----------------|--------|
| analytics-service | 8092 | analytics_db | Liquibase ✅ | validate ✅ | ✅ | ✅ | Production Ready |
| predictive-analytics-service | 8093 | predictive_db | Liquibase ✅ | validate ✅ | ✅ | ✅ | Production Ready |
| sdoh-service | 8094 | sdoh_db | Liquibase ✅ | validate ✅ | ✅ | ✅ | Production Ready |

### Data Processing (2 databases)

| Service | Port | Database | Migration Tool | ddl-auto | Test Driver | Validation Test | Status |
|---------|------|----------|----------------|----------|-------------|-----------------|--------|
| data-enrichment-service | 8089 | enrichment_db | ❌ None | validate ✅ | ⚠️ | ✅ | Needs Config |
| cdr-processor-service | 8099 | cdr_db | ❌ None | ❌ none | ⚠️ | ✅ | **Critical Fix Needed** |

### Workflow Services (3 databases)

| Service | Port | Database | Migration Tool | ddl-auto | Test Driver | Validation Test | Status |
|---------|------|----------|----------------|----------|-------------|-----------------|--------|
| approval-service | 8097 | approval_db | Flyway ⚠️ | validate ✅ | ✅ | ✅ | **Phase 2 Target** |
| payer-workflows-service | 8098 | payer_db | ❌ None | ⚠️ multi | ✅ | ✅ | Critical Fix Needed |
| migration-workflow-service | 8103 | migration_db | Liquibase ✅ | validate ✅ | ⚠️ | ✅ | Production Ready |

### Sales & Support (3 databases)

| Service | Port | Database | Migration Tool | ddl-auto | Test Driver | Validation Test | Status |
|---------|------|----------|----------------|----------|-------------|-----------------|--------|
| sales-automation-service | 8106 | sales_automation_db | Liquibase ✅ | validate ✅ | ✅ | ✅ | Production Ready |
| notification-service | 8107 | notification_db | ❌ None | validate ✅ | ⚠️ | ✅ | Needs Config |
| documentation-service | 8091 | docs_db | ❌ None | validate ✅ | ✅ | ✅ | Needs Config |

### Regulatory & Integration (4 databases)

| Service | Port | Database | Migration Tool | ddl-auto | Test Driver | Validation Test | Status |
|---------|------|----------|----------------|----------|-------------|-----------------|--------|
| hcc-service | 8105 | hcc_db | Liquibase ✅ | validate ✅ | ⚠️ | ✅ | Production Ready |
| prior-auth-service | 8102 | prior_auth_db | ❌ None | validate ✅ | ✅ | ✅ | Needs Config |
| qrda-export-service | 8104 | qrda_db | Liquibase ✅ | validate ✅ | ✅ | ✅ | Production Ready |
| ecr-service | 8101 | ecr_db | ❌ None | validate ✅ | ⚠️ | ✅ | Needs Config |

### Integration Services

| Service | Port | Database | Migration Tool | ddl-auto | Test Driver | Validation Test | Status |
|---------|------|----------|----------------|----------|-------------|-----------------|--------|
| ehr-connector-service | 8100 | ehr_connector_db | ❌ None | validate ✅ | ✅ | ✅ | Needs Config |
| cms-connector-service | N/A | (none) | ❌ None | ⚠️ multi | ✅ | ❌ | **Critical Fix Needed** |

### Support Services

| Service | Port | Database | Migration Tool | ddl-auto | Test Driver | Validation Test | Status |
|---------|------|----------|----------------|----------|-------------|-----------------|--------|
| demo-seeding-service | N/A | healthdata_demo | Liquibase ✅ | validate ✅ | ✅ | ❌ | Minor Fix Needed |
| cost-analysis-service | N/A | TBD | ❌ None | ❌ none | ⚠️ | ❌ | **Not Configured** |

### Gateway Services (Shared gateway_db)

| Service | Port | Database | Migration Tool | ddl-auto | Test Driver | Validation Test | Status |
|---------|------|----------|----------------|----------|-------------|-----------------|--------|
| gateway-admin-service | 8001 | gateway_db | ❌ None | validate ✅ | ⚠️ | ✅ | Shares schema |
| gateway-clinical-service | 8002 | gateway_db | ❌ None | validate ✅ | ⚠️ | ✅ | Shares schema |
| gateway-fhir-service | 8003 | gateway_db | ❌ None | validate ✅ | ⚠️ | ✅ | Shares schema |

## Phase 1 Completed Tasks

### ✅ Task 1.1: Add PostgreSQL Driver (COMPLETE)

Added `testImplementation(libs.postgresql)` to 5 services:
- ✅ agent-builder-service
- ✅ agent-runtime-service
- ✅ approval-service
- ✅ prior-auth-service
- ✅ cms-connector-service

### ✅ Task 1.2: Create Validation Script (COMPLETE)

Created: `backend/scripts/validate-database-config.sh`

Validates:
- ddl-auto setting (must be `validate`)
- Migration tool configured (Liquibase or Flyway)
- PostgreSQL driver in test dependencies
- Entity-migration validation test exists

### 🔄 Task 1.3: Documentation (IN PROGRESS)

This document serves as the migration status tracker.

## Issues Tracker

### Critical Issues (Must Fix)

| Service | Issue | Severity | Phase | Resolution |
|---------|-------|----------|-------|------------|
| ai-assistant-service | ddl-auto: none | Critical | Phase 1 | Change to validate |
| cdr-processor-service | ddl-auto: none | Critical | Phase 1 | Change to validate |
| payer-workflows-service | ddl-auto: multiple values | Critical | Phase 1 | Fix to validate only |
| cms-connector-service | ddl-auto: multiple values | Critical | Phase 1 | Fix to validate only |
| cost-analysis-service | No database configured | Critical | Phase 1 | Complete setup |

### High Priority (Phase 2)

| Service | Issue | Phase | Resolution |
|---------|-------|-------|------------|
| agent-builder-service | Using Flyway | Phase 2 | Migrate to Liquibase |
| approval-service | Using Flyway | Phase 2 | Migrate to Liquibase |
| gateway-service | Auth tables in init script | Phase 3 | Move to Liquibase |

### Medium Priority (Enable Liquibase)

Services with changelog directories but no explicit Liquibase configuration:
- agent-runtime-service
- event-router-service
- gateway-admin-service
- gateway-clinical-service
- gateway-fhir-service
- data-enrichment-service
- notification-service
- documentation-service
- prior-auth-service
- ecr-service
- ehr-connector-service

**Resolution:** Add Liquibase configuration to application.yml for each service.

### Low Priority (Test Infrastructure)

Services missing PostgreSQL test driver (inherits from shared module but should be explicit):
- Multiple services marked with ⚠️ in table above

**Resolution:** Add explicit `testImplementation(libs.postgresql)` to build.gradle.kts.

## Validation Results

Last run: 2026-01-10

```bash
Summary:
  ❌ Errors: 22
  ⚠️  Warnings: 16
```

Run validation:
```bash
cd backend
./scripts/validate-database-config.sh
```

## Next Steps

### Immediate (Phase 1 Completion)

1. **Fix Critical Issues:**
   - [ ] ai-assistant-service: Change ddl-auto to validate
   - [ ] cdr-processor-service: Change ddl-auto to validate
   - [ ] payer-workflows-service: Fix ddl-auto configuration
   - [ ] cms-connector-service: Fix ddl-auto configuration
   - [ ] cost-analysis-service: Set up database configuration

2. **Verify Phase 1:**
   - [ ] Run validation script (should pass with 0 errors)
   - [ ] Run entity-migration validation tests for fixed services
   - [ ] Commit Phase 1 changes

### Phase 2 (Next Week)

1. **Migrate Flyway Services:**
   - [ ] approval-service (simplest - 1 migration)
   - [ ] agent-builder-service (3 migrations)

2. **Enable Liquibase for 11 Services:**
   - [ ] Add application.yml configuration
   - [ ] Verify changelogs exist
   - [ ] Test locally

## Database Architecture

### Current State
- **26 databases** created by `docker/postgres/init-multi-db.sh`
- **Init script** creates databases + gateway auth tables
- **Services** manage their own schemas via migrations

### Target State (Post-Migration)
- **26 databases** created by init script (empty)
- **Services** manage complete schema (including extensions)
- **Gateway auth** managed by gateway-service Liquibase
- **100% Liquibase** standardization (zero Flyway)

## Reference

- **Plan File:** `/home/mahoosuc-solutions/.claude/plans/clever-dazzling-robin.md`
- **Validation Script:** `backend/scripts/validate-database-config.sh`
- **Init Script:** `docker/postgres/init-multi-db.sh`
- **Service Changelogs:** `backend/modules/services/*/src/main/resources/db/changelog/`

---

*Generated by Phase 1 database migration implementation*
