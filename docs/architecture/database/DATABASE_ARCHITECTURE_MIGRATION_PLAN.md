# HDIM Database Architecture & Migration Plan

**Date:** 2026-01-10
**Version:** 1.0
**Status:** Draft for Review
**Author:** Claude (HDIM Platform Team)

---

## Executive Summary

This document analyzes the current database architecture of the HDIM healthcare platform and provides a comprehensive migration plan to align with industry best practices for microservices database management. The plan addresses critical production-readiness issues including schema management, migration tooling consistency, and HIPAA compliance requirements.

**Current State:** 29 separate databases on single PostgreSQL instance, migrations disabled, DDL auto set to "create" (destructive)
**Target State:** Liquibase-managed schemas with validation mode, entity-migration synchronization, and proper isolation
**Timeline:** 3-4 weeks across 4 phases
**Risk Level:** Medium (requires careful sequencing, data preservation)

---

## Table of Contents

1. [Current State Analysis](#current-state-analysis)
2. [Problems Identified](#problems-identified)
3. [Proper Database Architecture](#proper-database-architecture)
4. [Migration Plan](#migration-plan)
5. [Success Metrics](#success-metrics)
6. [Validation Criteria](#validation-criteria)
7. [Risk Mitigation](#risk-mitigation)
8. [References](#references)

---

## Current State Analysis

### Database Inventory

**PostgreSQL Instance:** Single instance running PostgreSQL 16-alpine
**Total Databases:** 29 separate logical databases
**Migration Tools:** Mixed (Liquibase 4.29.2 standard, Flyway for agent-builder only)
**Total Services:** 34 microservices (24 with migration files, 10 without)

#### Database List by Service Category

##### Core Clinical Services (8 databases)
| Database | Service | Port | Migration Files | Status |
|----------|---------|------|----------------|--------|
| fhir_db | fhir-service | 8085 | ✓ Liquibase | DISABLED |
| cql_db | cql-engine-service | 8081 | ✓ Liquibase | DISABLED |
| quality_db | quality-measure-service | 8087 | ✓ Liquibase | DISABLED |
| patient_db | patient-service | 8084 | ✓ Liquibase | DISABLED |
| caregap_db | care-gap-service | 8086 | ✓ Liquibase | DISABLED |
| consent_db | consent-service | 8082 | ✓ Liquibase | DISABLED |
| event_db | event-processing-service | 8083 | ✓ Liquibase | DISABLED |
| gateway_db | gateway-*-service | 8080 | ✗ None | Script-based |

##### AI Services (3 databases)
| Database | Service | Port | Migration Files | Status |
|----------|---------|------|----------------|--------|
| agent_db | agent-builder-service | 8096 | ✓ Flyway (V1-V3) | **ENABLED** ✓ |
| agent_runtime_db | agent-runtime-service | 8088 | ✓ Liquibase | DISABLED |
| ai_assistant_db | ai-assistant-service | 8090 | ✗ None | DDL auto |

##### Analytics Services (3 databases)
| Database | Service | Port | Migration Files | Status |
|----------|---------|------|----------------|--------|
| analytics_db | analytics-service | 8092 | ✓ Liquibase | DISABLED |
| predictive_db | predictive-analytics-service | 8093 | ✓ Liquibase | DISABLED |
| sdoh_db | sdoh-service | 8094 | ✓ Liquibase | DISABLED |

##### Data Processing Services (3 databases)
| Database | Service | Port | Migration Files | Status |
|----------|---------|------|----------------|--------|
| enrichment_db | data-enrichment-service | 8089 | ✗ None | DDL auto |
| event_router_db | event-router-service | 8095 | ✓ Liquibase | DISABLED |
| cdr_db | cdr-processor-service | 8099 | ✗ None | DDL auto |

##### Healthcare Services (4 databases)
| Database | Service | Port | Migration Files | Status |
|----------|---------|------|----------------|--------|
| ecr_db | ecr-service | 8101 | ✓ Liquibase | DISABLED |
| qrda_db | qrda-export-service | 8104 | ✓ Liquibase | DISABLED |
| hcc_db | hcc-service | 8105 | ✓ Liquibase | DISABLED |
| prior_auth_db | prior-auth-service | 8102 | ✓ Flyway | DISABLED |

##### Workflow Services (3 databases)
| Database | Service | Port | Migration Files | Status |
|----------|---------|------|----------------|--------|
| approval_db | approval-service | 8097 | ✓ Liquibase | DISABLED |
| payer_db | payer-workflows-service | 8098 | ✗ None | DDL auto |
| migration_db | migration-workflow-service | 8103 | ✓ Liquibase | DISABLED |

##### Support Services (4 databases)
| Database | Service | Port | Migration Files | Status |
|----------|---------|------|----------------|--------|
| ehr_connector_db | ehr-connector-service | 8100 | ✗ None | DDL auto |
| docs_db | documentation-service | 8091 | ✓ Liquibase | DISABLED |
| notification_db | notification-service | 8107 | ✓ Liquibase | DISABLED |
| sales_automation_db | sales-automation-service | 8106 | ✓ Liquibase | DISABLED |

##### Demo/Testing (1 database)
| Database | Service | Port | Migration Files | Status |
|----------|---------|------|----------------|--------|
| healthdata_demo | demo-seeding-service | 8098 | ✓ Liquibase | DISABLED |

**Summary Statistics:**
- **Total Services:** 34
- **With migration files:** 24 (71%)
- **Without migration files:** 10 (29%)
- **Migrations ENABLED:** 1 (agent-builder only)
- **Migrations DISABLED:** 23 (96% of services with migrations!)

### Current Configuration Analysis

#### docker-compose.yml Settings (per service)

##### **Critical Issue 1: DDL Auto Settings**

```yaml
# MOST SERVICES (18 services) - DESTRUCTIVE!
SPRING_JPA_HIBERNATE_DDL_AUTO: create
# Effect: Schema destroyed and recreated on EVERY startup
# Data loss: YES - all data wiped on container restart

# SOME SERVICES (8 services) - DRIFT RISK!
SPRING_JPA_HIBERNATE_DDL_AUTO: update
# Effect: Schema auto-modified based on entities
# Data loss: NO - but creates untracked schema drift
# Used by: cql-engine, quality-measure, sales-automation

# ONLY 1 SERVICE - CORRECT!
SPRING_JPA_HIBERNATE_DDL_AUTO: validate
# Service: agent-builder-service
# Effect: Validates schema matches entities, fails if mismatch
```

##### **Critical Issue 2: Migration Tool Disabled**

```yaml
# ALMOST ALL SERVICES
SPRING_FLYWAY_ENABLED: "false"
SPRING_LIQUIBASE_ENABLED: "false"

# ONLY agent-builder-service
SPRING_FLYWAY_ENABLED: "true"
SPRING_FLYWAY_BASELINE_ON_MIGRATE: "true"
SPRING_JPA_HIBERNATE_DDL_AUTO: validate  # CORRECT!
```

##### **Critical Issue 3: PostgreSQL Version**

```yaml
# PostgreSQL 16-alpine
postgres:
  image: postgres:16-alpine
  command: postgres -c max_connections=300 -c shared_buffers=256MB
```

**Problem:** From CORE_SERVICES_TEST_REPORT.md:
```
Caused by: org.flywaydb.core.api.FlywayException: Unsupported Database: PostgreSQL 16.11
```

**Root Cause:** Spring Boot 3.5.9's managed Flyway version may not support PostgreSQL 16.
**Impact:** Services cannot start if Flyway is enabled (blocking Phase 1 migration).

#### Database Initialization Script

**File:** `docker/postgres/init-multi-db.sh`

**Issues identified:**
1. **Mixed concerns** - Creates databases AND tables (lines 104-172)
2. **Hardcoded schema** - gateway_db tables created in shell script instead of migrations
3. **No versioning** - Changes to this script aren't tracked or reversible
4. **Extension management** - pg_trgm enabled manually for 4 databases

**Current script does:**
- ✓ Creates all 29 databases
- ✓ Grants privileges to `healthdata` user
- ✓ Enables `pg_trgm` extension for search indexes
- ✗ Creates 6 tables in gateway_db (users, user_roles, user_tenants, audit_logs, refresh_tokens, etc.)

**Should be:**
- ✓ Creates databases only
- ✓ Grants privileges
- ✓ Enables required extensions
- ✓ Migrations handle ALL table creation

### Migration Files Inventory

#### Liquibase Changelogs (24 services)

**Example: patient-service**
```
db/changelog/
├── 0001-create-patient-demographics-table.xml
├── 0002-create-patient-insurance-table.xml
├── 0003-create-patient-risk-scores-table.xml
├── 0004-add-composite-indexes-and-jsonb.xml
├── 0005-create-provider-panel-assignment.xml
└── db.changelog-master.xml  (references all migrations)
```

**Format:** Liquibase XML changelogs with sequential numbering
**Naming Pattern:** `NNNN-description.xml`
**Master File:** `db.changelog-master.xml` includes all migrations
**Status:** Files exist but DISABLED in docker-compose.yml

#### Flyway Migrations (2 services)

**agent-builder-service:** (ENABLED ✓)
```
db/migration/
├── V1__create_agent_builder_tables.sql
├── V2__add_performance_indexes.sql
└── V3__create_audit_events_table.sql
```

**prior-auth-service:** (DISABLED)
```
db/migration/
└── (Flyway SQL files)
```

**Format:** Flyway versioned SQL migrations
**Naming Pattern:** `V{version}__{description}.sql`
**Status:** Only agent-builder has Flyway enabled

#### Services WITHOUT Migration Files (10 services)

1. ai-assistant-service
2. cdr-processor-service
3. cost-analysis-service
4. data-enrichment-service
5. ehr-connector-service
6. gateway-admin-service
7. gateway-clinical-service
8. gateway-fhir-service
9. gateway-service
10. payer-workflows-service

**Impact:** These services rely entirely on DDL auto = "create", losing all data on restart.

---

## Problems Identified

### Critical (Production-Blocking)

#### **P1: Data Loss on Service Restart**

**Severity:** CRITICAL
**Impact:** ALL services except agent-builder lose data on container restart
**Root Cause:** `ddl-auto: create` destroys and recreates schema on startup

**Evidence:**
```yaml
# From docker-compose.yml (18 services)
SPRING_JPA_HIBERNATE_DDL_AUTO: create
```

**Affected Services:** fhir, patient, care-gap, consent, event-processing, analytics, predictive, sdoh, ecr, qrda, hcc, notification, documentation, and 5 more.

**HIPAA Compliance Risk:** ⚠️ HIGH - PHI data lost without audit trail

#### **P2: Migrations Disabled Despite Existing Files**

**Severity:** CRITICAL
**Impact:** 24 services have complete migration files but they're never executed
**Root Cause:** `SPRING_LIQUIBASE_ENABLED: "false"` in docker-compose.yml

**Evidence:**
- patient-service has 5 Liquibase changelogs (DISABLED)
- care-gap-service has complete migration history (DISABLED)
- 22 other services with migrations (ALL DISABLED)

**Result:** Migrations exist but aren't tracked in `databasechangelog` table.

#### **P3: Schema Drift (Update Mode)**

**Severity:** HIGH
**Impact:** Schema changes aren't tracked, creating production vs. development drift
**Affected Services:** cql-engine, quality-measure, sales-automation

**Evidence:**
```yaml
SPRING_JPA_HIBERNATE_DDL_AUTO: update
```

**Problem:** Hibernate modifies schema automatically based on @Entity classes, but changes aren't versioned.

#### **P4: PostgreSQL 16 / Flyway Incompatibility**

**Severity:** HIGH (blocks migration to Flyway)
**Impact:** Cannot enable Flyway on services until version compatibility resolved
**Root Cause:** Flyway version from Spring Boot 3.5.9 BOM doesn't support PostgreSQL 16

**Evidence:** From test report:
```
org.flywaydb.core.api.FlywayException: Unsupported Database: PostgreSQL 16.11
```

**Options:**
1. Downgrade to PostgreSQL 15 (quick fix, loses features)
2. Upgrade Flyway explicitly in build.gradle.kts (proper fix)
3. Standardize on Liquibase for ALL services (recommended)

### High (Production Risk)

#### **P5: Mixed Migration Tools**

**Severity:** HIGH
**Impact:** Inconsistent tooling increases maintenance burden
**Details:**
- 24 services: Liquibase (standard)
- 2 services: Flyway (agent-builder, prior-auth)
- 8 services: No migrations

**Problem:** Team must maintain expertise in multiple tools, different debugging procedures.

#### **P6: Schema Initialization in Shell Script**

**Severity:** HIGH
**Impact:** `gateway_db` tables created outside version control
**Location:** `docker/postgres/init-multi-db.sh` lines 104-172

**Tables created directly:**
- users (authentication)
- user_roles
- user_tenants (multi-tenant support)
- audit_logs (HIPAA compliance)
- refresh_tokens

**Problem:** Schema changes to these tables aren't versioned or trackable.

#### **P7: Single Database User**

**Severity:** MEDIUM
**Impact:** All 29 databases use same user `healthdata`
**Security Risk:** Service compromise grants access to ALL databases

**Best Practice Violation:** Each service should have separate DB credentials (principle of least privilege).

### Medium (Operational Issues)

#### **P8: Missing Entity-Migration Validation**

**Severity:** MEDIUM
**Impact:** No automated checks that entities match migration schemas
**Status:** Only 1 service (agent-builder) has validation tests

**From CLAUDE.md:**
> When creating a new entity:
> - [ ] Create JPA entity with @Entity, @Table, @Column annotations
> - [ ] Create Liquibase migration file
> - [ ] Run validation test: ./gradlew test --tests "*EntityMigrationValidationTest"

**Problem:** 33 of 34 services lack validation tests, risking RefreshToken-style bugs.

#### **P9: Inconsistent Extension Management**

**Severity:** MEDIUM
**Impact:** pg_trgm enabled manually, not tracked in migrations
**Location:** init-multi-db.sh lines 94-99

```bash
for db in fhir_db cql_db quality_db patient_db; do
    psql -c "CREATE EXTENSION IF NOT EXISTS pg_trgm;"
done
```

**Problem:** Other services may need pg_trgm but script only enables for 4 databases.

#### **P10: No Shared Database**

**Severity:** LOW (design decision)
**Impact:** Cross-service queries impossible
**Status:** ACCEPTABLE for microservices architecture

**Note:** This is by design (database-per-service pattern), but team should be aware of trade-offs.

---

## Proper Database Architecture

### Design Principles (Industry Best Practices)

Based on 2025 microservices patterns and HIPAA compliance requirements:

#### **1. Database Per Service Pattern**

**Source:** [Microservices.io - Database per service](https://microservices.io/patterns/data/database-per-service.html)

**HDIM Implementation:** ✓ CORRECT - 29 separate databases for service isolation

**Recommendation:** Keep current approach (one logical database per service on shared PostgreSQL instance)

**Alternatives considered:**
- **Schema-per-service** (lower overhead, clearer ownership) - Could be considered for future optimization
- **Database-server-per-service** (maximum isolation) - Overkill for HDIM scale
- **Private-tables-per-service** (minimal isolation) - Insufficient for HIPAA compliance

**Quote from research:**
> "Schema-per-service – each service has a database schema that's private to that service. Using a schema per service is appealing since it makes ownership clearer." — [GeeksforGeeks](https://www.geeksforgeeks.org/sql/microservices-database-design-patterns/)

#### **2. Standardize on Liquibase**

**Source:** [Flyway vs Liquibase 2025 Comparison](https://www.bytebase.com/blog/flyway-vs-liquibase/)

**Decision Rationale:**
- ✅ Already used by 24 of 26 services (89%)
- ✅ Database-agnostic (supports PostgreSQL, MySQL, Oracle)
- ✅ Rollback support (crucial for production)
- ✅ Spring Boot fully supports Liquibase
- ✅ No PostgreSQL 16 compatibility issues
- ❌ Flyway only used by 2 services (7%)
- ❌ Flyway has PostgreSQL 16 compatibility issues
- ❌ Flyway rollback requires manual scripts (paid version)

**Recommendation:** Migrate agent-builder and prior-auth from Flyway to Liquibase

**Quote from research:**
> "Use hbm2ddl.auto=validate in production to check schema correctness and apply schema changes using Flyway or Liquibase." — [Baeldung](https://www.baeldung.com/liquibase-vs-flyway)

#### **3. Always Use DDL Auto = Validate**

**Source:** [Database Schema Generation with Hibernate](https://prgrmmng.com/database-schema-generation-and-migration-with-hibernate)

**Settings by Environment:**

```yaml
# PRODUCTION (REQUIRED)
SPRING_JPA_HIBERNATE_DDL_AUTO: validate
SPRING_LIQUIBASE_ENABLED: true

# DEVELOPMENT (ACCEPTABLE)
SPRING_JPA_HIBERNATE_DDL_AUTO: validate  # Recommended
# OR
SPRING_JPA_HIBERNATE_DDL_AUTO: update     # Only for rapid prototyping

# TESTING (WITH TESTCONTAINERS)
SPRING_JPA_HIBERNATE_DDL_AUTO: create-drop  # Acceptable for integration tests
```

**Effect of `validate`:**
- ✅ Hibernate validates entities match database schema
- ✅ Application fails to start if mismatch detected
- ✅ Forces proper migration workflow
- ❌ Requires complete migration files

**Quote from research:**
> "When set to validate, Hibernate validates that the already existing schema matches the Java entities - a useful option if the database is managed externally." — [Bootify](https://bootify.io/spring-data/database-generation-with-hibernate-liquibase-flyway.html)

#### **4. Entity-Migration Synchronization**

**Source:** HDIM CLAUDE.md (internal best practice)

**Process:**
1. Create/modify JPA entity with proper annotations
2. Create corresponding Liquibase migration
3. Run validation test: `*EntityMigrationValidationTest`
4. Never modify existing migrations (create new ones)

**From CLAUDE.md:**
> "This practice prevents production schema drift issues (like the RefreshToken authentication bug)."

**Validation Test Template:**
```java
@ExtendWith(MockitoExtension.class)
class EntityMigrationValidationTest {
    @Test
    void shouldValidateEntityMatchesMigrationSchema() {
        // Test that @Entity annotations match Liquibase changeset
        // Fail if column types, constraints, or names mismatch
    }
}
```

#### **5. Separate Database Users (Least Privilege)**

**Source:** [Microservices Database Management](https://medium.com/design-microservices-architecture-with-patterns/microservices-database-management-patterns-and-principles-9121e25619f1)

**Current:** Single user `healthdata` for all 29 databases

**Target:** One user per service with restricted grants

**Example:**
```sql
-- Create service-specific users
CREATE USER fhir_service_user WITH PASSWORD 'secure_password';
CREATE USER patient_service_user WITH PASSWORD 'secure_password';

-- Grant access to ONLY their database
GRANT ALL PRIVILEGES ON DATABASE fhir_db TO fhir_service_user;
GRANT ALL PRIVILEGES ON DATABASE patient_db TO patient_service_user;

-- Revoke access to other databases (deny by default)
REVOKE ALL PRIVILEGES ON DATABASE patient_db FROM fhir_service_user;
```

**Benefits:**
- ✅ Compromised service can't access other services' data
- ✅ HIPAA audit trail (which service accessed which database)
- ✅ Easier to revoke access during incident response

**Quote from research:**
> "You could, for example, assign a different database user id to each service and use a database access control mechanism such as grants." — [Microservices.io](https://microservices.io/patterns/data/database-per-service.html)

#### **6. PostgreSQL Version Strategy**

**Options:**

| Option | Pros | Cons | Recommendation |
|--------|------|------|----------------|
| **PostgreSQL 15** | Stable, Flyway support, Spring Boot tested | Lacks PostgreSQL 16 features | ✓ **Phase 1 (short-term)** |
| **PostgreSQL 16 + Explicit Flyway Upgrade** | Latest features, Flyway support with explicit version | Requires build.gradle.kts changes | ✗ Unnecessary if using Liquibase |
| **PostgreSQL 16 + Liquibase Only** | Latest features, no compatibility issues, standard tool | Requires migrating 2 Flyway services | ✓ **Phase 2+ (long-term)** |

**Recommendation:**
- Phase 1: Downgrade to PostgreSQL 15 for quick unblocking
- Phase 2: Migrate all services to Liquibase
- Phase 3: Upgrade back to PostgreSQL 16 (Liquibase has no compatibility issues)

### Target Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│           PostgreSQL 15/16 Instance (Single Server)           │
│                                                               │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐   │
│  │   fhir_db     │  │  patient_db   │  │  quality_db   │   │
│  │ (fhir_user)   │  │(patient_user) │  │(quality_user) │   │
│  │               │  │               │  │               │   │
│  │ Schema: v5    │  │ Schema: v8    │  │ Schema: v12   │   │
│  │ Liquibase ✓   │  │ Liquibase ✓   │  │ Liquibase ✓   │   │
│  └───────────────┘  └───────────────┘  └───────────────┘   │
│                          ...                                  │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐   │
│  │   agent_db    │  │   gateway_db  │  │  caregap_db   │   │
│  │ (agent_user)  │  │(gateway_user) │  │(caregap_user) │   │
│  │               │  │               │  │               │   │
│  │ Schema: v3    │  │ Schema: v6    │  │ Schema: v9    │   │
│  │ Liquibase ✓   │  │ Liquibase ✓   │  │ Liquibase ✓   │   │
│  └───────────────┘  └───────────────┘  └───────────────┘   │
│                                                               │
│  Extensions: pg_trgm (managed in migrations, not init script)│
└─────────────────────────────────────────────────────────────┘
         ▲                  ▲                  ▲
         │                  │                  │
    ┌────┴────┐        ┌────┴────┐        ┌────┴────┐
    │  FHIR   │        │ Patient │        │ Quality │
    │ Service │        │ Service │        │ Service │
    │         │        │         │        │         │
    │ DDL:    │        │ DDL:    │        │ DDL:    │
    │ validate│        │ validate│        │ validate│
    └─────────┘        └─────────┘        └─────────┘
```

**Key Changes from Current:**
1. ✅ All services use Liquibase (no Flyway)
2. ✅ All services use `ddl-auto: validate`
3. ✅ Separate database users per service
4. ✅ Schema versions tracked in `databasechangelog` table
5. ✅ Extensions managed in Liquibase migrations
6. ✅ No schema creation in init scripts

### Configuration Template (Target State)

**docker-compose.yml (per service):**
```yaml
quality-measure-service:
  environment:
    # Database connection
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/quality_db
    SPRING_DATASOURCE_USERNAME: quality_service_user
    SPRING_DATASOURCE_PASSWORD: ${QUALITY_SERVICE_DB_PASSWORD}

    # Migration tool
    SPRING_LIQUIBASE_ENABLED: "true"
    SPRING_LIQUIBASE_CHANGE_LOG: classpath:db/changelog/db.changelog-master.xml

    # DDL auto (MUST be validate)
    SPRING_JPA_HIBERNATE_DDL_AUTO: validate

    # Flyway explicitly disabled (if previously used)
    SPRING_FLYWAY_ENABLED: "false"
```

**build.gradle.kts (per service):**
```kotlin
dependencies {
    // Persistence
    implementation(project(":modules:shared:infrastructure:persistence"))
    // This brings in: Liquibase 4.29.2, PostgreSQL driver, HikariCP

    // DO NOT add Flyway
    // implementation("org.flywaydb:flyway-core") ❌
}
```

**Liquibase Changelog Master (per service):**
```xml
<!-- db/changelog/db.changelog-master.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <include file="db/changelog/0001-create-quality-measures-table.xml"/>
    <include file="db/changelog/0002-create-evaluation-results-table.xml"/>
    <include file="db/changelog/0003-add-performance-indexes.xml"/>
    <!-- Add new migrations here, never modify existing -->
</databaseChangeLog>
```

---

## Migration Plan

### Overview

**Total Timeline:** 3-4 weeks
**Phases:** 4 phases (Foundation, Core Services, All Services, Validation)
**Risk Level:** Medium (requires data preservation, careful sequencing)
**Rollback Strategy:** Database backups before each phase, migration rollback via Liquibase

### Phase 1: Foundation & Quick Fixes (Week 1)

**Goal:** Unblock development, fix critical issues, establish baseline

**Duration:** 5 days
**Risk:** Low
**Can rollback:** Yes (database backup)

#### **Task 1.1: Downgrade PostgreSQL to 15** ⚠️

**Priority:** P0 (unblocks all work)
**Effort:** 2 hours
**Risk:** Low (development environment only)

**Steps:**
1. Backup all databases (via postgres-backup service or pg_dumpall)
2. Stop all services: `docker compose down`
3. Remove PostgreSQL volume: `docker volume rm healthdata_postgres_data`
4. Update `docker-compose.yml`:
   ```yaml
   postgres:
     image: postgres:15-alpine  # Changed from postgres:16-alpine
   ```
5. Restart infrastructure: `docker compose --profile light up -d`
6. Verify PostgreSQL version: `docker exec healthdata-postgres psql --version`
7. Run init script to recreate databases

**Validation:**
```bash
# Should output: PostgreSQL 15.x
docker exec healthdata-postgres psql -U healthdata -c "SELECT version();"
```

**Rollback:** Restore from backup, revert to postgres:16-alpine

#### **Task 1.2: Fix gateway_db Schema Initialization**

**Priority:** P1
**Effort:** 4 hours
**Risk:** Medium (authentication tables)

**Problem:** Tables created in init-multi-db.sh instead of migrations

**Solution:**
1. Create `gateway-admin-service/src/main/resources/db/changelog/` directory
2. Create migration files:

**File:** `0001-create-users-table.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="0001-create-users-table" author="hdim-platform-team">
        <createTable tableName="users">
            <column name="id" type="UUID" defaultValueComputed="gen_random_uuid()">
                <constraints primaryKey="true" primaryKeyName="pk_users"/>
            </column>
            <column name="username" type="VARCHAR(50)">
                <constraints nullable="false" unique="true"/>
            </column>
            <column name="email" type="VARCHAR(100)">
                <constraints nullable="false" unique="true"/>
            </column>
            <column name="password_hash" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="first_name" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="last_name" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="active" type="BOOLEAN" defaultValueBoolean="true">
                <constraints nullable="false"/>
            </column>
            <column name="email_verified" type="BOOLEAN" defaultValueBoolean="false">
                <constraints nullable="false"/>
            </column>
            <column name="mfa_enabled" type="BOOLEAN" defaultValueBoolean="false">
                <constraints nullable="false"/>
            </column>
            <column name="created_at" type="TIMESTAMP WITH TIME ZONE" defaultValueComputed="CURRENT_TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="updated_at" type="TIMESTAMP WITH TIME ZONE" defaultValueComputed="CURRENT_TIMESTAMP">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <createIndex indexName="idx_users_username" tableName="users">
            <column name="username"/>
        </createIndex>
        <createIndex indexName="idx_users_email" tableName="users">
            <column name="email"/>
        </createIndex>
        <createIndex indexName="idx_users_active" tableName="users">
            <column name="active"/>
        </createIndex>
    </changeSet>
</databaseChangeLog>
```

**Similar files needed:**
- `0002-create-user-roles-table.xml`
- `0003-create-user-tenants-table.xml`
- `0004-create-audit-logs-table.xml`
- `0005-create-refresh-tokens-table.xml`

3. Create `db.changelog-master.xml` that includes all 5 migrations
4. Remove table creation from `init-multi-db.sh` (lines 104-172)
5. Update `docker-compose.yml` for all 3 gateway services:
   ```yaml
   gateway-admin-service:
     environment:
       SPRING_LIQUIBASE_ENABLED: "true"
       SPRING_JPA_HIBERNATE_DDL_AUTO: validate
   ```

**Validation:**
```bash
# Check databasechangelog table was created
docker exec healthdata-postgres psql -U healthdata -d gateway_db \
  -c "SELECT id, author, filename FROM databasechangelog ORDER BY orderexecuted;"

# Should show 5 migrations executed
```

**Rollback:** Revert docker-compose.yml, restore init script

#### **Task 1.3: Update init-multi-db.sh**

**Priority:** P2
**Effort:** 1 hour
**Risk:** Low

**Changes:**
1. Remove table creation for gateway_db (lines 104-172)
2. Keep database creation (lines 9-89)
3. Move pg_trgm extension to migrations (for Phase 2)

**New init-multi-db.sh:**
```bash
#!/bin/bash
set -e

echo "Creating HDIM databases..."

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Create all 29 databases (keep existing)
    CREATE DATABASE fhir_db;
    CREATE DATABASE cql_db;
    -- ... (all 29 databases)

    -- Grant privileges (keep existing)
    GRANT ALL PRIVILEGES ON DATABASE fhir_db TO "$POSTGRES_USER";
    -- ... (all grants)
EOSQL

# REMOVED: Table creation for gateway_db (moved to Liquibase)
# REMOVED: pg_trgm extension (moved to Liquibase)

echo "All HDIM databases created successfully!"
```

**Validation:**
```bash
# Run script
docker exec healthdata-postgres /docker-entrypoint-initdb.d/init-multi-db.sh

# Verify databases exist
docker exec healthdata-postgres psql -U healthdata -c "\l" | grep "_db"

# Should show 29 databases
```

#### **Task 1.4: Create Validation Test Template**

**Priority:** P2
**Effort:** 3 hours
**Risk:** Low

**Goal:** Establish testing pattern for entity-migration synchronization

**Create file:** `platform/test-fixtures/src/main/java/com/healthdata/test/EntityMigrationValidationTestBase.java`

```java
package com.healthdata.test;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.EntityType;
import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for entity-migration validation tests.
 *
 * CRITICAL: This test ensures JPA entities match Liquibase migrations.
 * Run this test after every entity or migration change.
 *
 * Prevents production schema drift bugs like RefreshToken authentication issue.
 */
@DataJpaTest
@Testcontainers
public abstract class EntityMigrationValidationTestBase {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.liquibase.enabled", () -> "true");
    }

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    /**
     * Validates that Liquibase migrations execute successfully
     * and Hibernate validation passes (entities match schema).
     */
    @Test
    void shouldValidateEntitiesMatchMigrations() throws Exception {
        // Step 1: Run Liquibase migrations
        try (Connection connection = dataSource.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(
                "db/changelog/db.changelog-master.xml",
                new ClassLoaderResourceAccessor(),
                database
            );
            liquibase.update("");
        }

        // Step 2: Hibernate validation (via Spring Boot autoconfiguration)
        // If entities don't match schema, this test will fail at startup

        // Step 3: Verify entities are mapped
        var entities = entityManager.getMetamodel().getEntities();
        assertThat(entities).isNotEmpty()
            .as("Service should have at least one JPA entity");

        // Log mapped entities for debugging
        entities.stream()
            .map(EntityType::getName)
            .forEach(entityName ->
                System.out.println("✓ Validated entity: " + entityName));
    }

    /**
     * Validates that all tables have corresponding entities.
     * Prevents orphaned tables from manual SQL or forgotten migrations.
     */
    @Test
    void shouldHaveEntityForEveryTable() throws Exception {
        // Implementation: Query information_schema.tables, verify each has @Entity
        // Left as exercise - add if needed per service
    }
}
```

**Usage in service (example: patient-service):**

**File:** `patient-service/src/test/java/com/healthdata/patient/EntityMigrationValidationTest.java`
```java
package com.healthdata.patient;

import com.healthdata.test.EntityMigrationValidationTestBase;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EntityMigrationValidationTest extends EntityMigrationValidationTestBase {
    // Inherits shouldValidateEntitiesMatchMigrations() test
    // No additional code needed unless service-specific validation required
}
```

**Validation:**
```bash
# Run test for patient-service
cd backend
./gradlew :modules:services:patient-service:test --tests "*EntityMigrationValidationTest"

# Should pass if entities match migrations
```

**Deliverables (Phase 1):**
- ✅ PostgreSQL 15 running
- ✅ gateway_db tables managed by Liquibase
- ✅ Updated init script (databases only)
- ✅ Validation test template created
- ✅ Documentation updated

---

### Phase 2: Core Services Migration (Week 2)

**Goal:** Enable Liquibase on 4 core services, validate pattern works

**Duration:** 7 days
**Risk:** Medium (production-critical services)
**Services:** fhir, patient, quality-measure, cql-engine

#### **Task 2.1: Enable Liquibase for Core Services**

**Priority:** P0
**Effort:** 2 days (0.5 day per service)
**Risk:** Medium

**Per Service Steps (example: patient-service):**

1. **Verify migration files exist:**
   ```bash
   ls -la backend/modules/services/patient-service/src/main/resources/db/changelog/
   # Should show: 0001-create-patient-demographics-table.xml, 0002-..., db.changelog-master.xml
   ```

2. **Add pg_trgm extension to migrations (if needed):**

   **File:** `0000-enable-extensions.xml` (create BEFORE 0001)
   ```xml
   <changeSet id="0000-enable-extensions" author="hdim-platform-team">
       <sql>CREATE EXTENSION IF NOT EXISTS pg_trgm;</sql>
       <rollback>DROP EXTENSION IF EXISTS pg_trgm;</rollback>
   </changeSet>
   ```

   Update `db.changelog-master.xml`:
   ```xml
   <include file="db/changelog/0000-enable-extensions.xml"/>  <!-- NEW -->
   <include file="db/changelog/0001-create-patient-demographics-table.xml"/>
   ...
   ```

3. **Create validation test:**
   ```bash
   # Copy template from Phase 1
   cp platform/test-fixtures/.../EntityMigrationValidationTestBase.java \
      modules/services/patient-service/src/test/java/.../EntityMigrationValidationTest.java
   ```

4. **Update docker-compose.yml:**
   ```yaml
   patient-service:
     environment:
       # Enable Liquibase
       SPRING_LIQUIBASE_ENABLED: "true"
       # Switch DDL auto to validate
       SPRING_JPA_HIBERNATE_DDL_AUTO: validate  # Changed from "create"
       # Disable Flyway (if present)
       SPRING_FLYWAY_ENABLED: "false"
   ```

5. **Test in isolation:**
   ```bash
   # Start infrastructure only
   docker compose --profile light up -d

   # Start ONLY patient-service
   docker compose up patient-service

   # Check logs for migration success
   docker logs healthdata-patient-service | grep -i liquibase
   # Should show: "Successfully applied X changesets"
   ```

6. **Verify database state:**
   ```bash
   # Check databasechangelog table
   docker exec healthdata-postgres psql -U healthdata -d patient_db \
     -c "SELECT id, filename, orderexecuted FROM databasechangelog ORDER BY orderexecuted;"

   # Should show all migration files executed
   ```

7. **Run validation test:**
   ```bash
   ./gradlew :modules:services:patient-service:test --tests "*EntityMigrationValidationTest"
   # Should PASS
   ```

8. **Repeat for 3 other core services** (fhir, quality-measure, cql-engine)

**Validation Criteria (per service):**
- [ ] Liquibase migrations execute successfully
- [ ] Service starts without errors
- [ ] Health check returns UP: `curl http://localhost:808X/service/actuator/health`
- [ ] databasechangelog table populated
- [ ] Validation test passes
- [ ] No data loss from previous runs (if any test data existed)

**Rollback Plan:**
1. Stop service
2. Restore database from backup
3. Revert docker-compose.yml to `ddl-auto: create`
4. Restart service

#### **Task 2.2: Migrate agent-builder from Flyway to Liquibase**

**Priority:** P1
**Effort:** 1 day
**Risk:** Medium (only service with working migrations currently)

**Agent-builder is special:** It's the ONLY service with migrations currently enabled!

**Strategy:** Convert Flyway SQL to Liquibase XML

**Existing Flyway migrations:**
```
V1__create_agent_builder_tables.sql
V2__add_performance_indexes.sql
V3__create_audit_events_table.sql
```

**Steps:**

1. **Create Liquibase changelog directory:**
   ```bash
   mkdir -p backend/modules/services/agent-builder-service/src/main/resources/db/changelog
   ```

2. **Convert V1 to Liquibase:**

   **File:** `0001-create-agent-builder-tables.xml`
   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
           http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

       <changeSet id="0001-create-agent-builder-tables" author="hdim-platform-team">
           <!-- Convert SQL from V1__create_agent_builder_tables.sql -->
           <!-- Use Liquibase <createTable>, <addColumn>, etc. -->
           <createTable tableName="agents">
               <!-- ... columns from SQL file ... -->
           </createTable>
           <!-- Repeat for all tables in V1 -->
       </changeSet>
   </databaseChangeLog>
   ```

3. **Convert V2 and V3 similarly** (0002, 0003)

4. **Create master changelog:**
   ```xml
   <databaseChangeLog>
       <include file="db/changelog/0001-create-agent-builder-tables.xml"/>
       <include file="db/changelog/0002-add-performance-indexes.xml"/>
       <include file="db/changelog/0003-create-audit-events-table.xml"/>
   </databaseChangeLog>
   ```

5. **Update build.gradle.kts:**
   ```kotlin
   dependencies {
       // REMOVE Flyway
       // implementation("org.flywaydb:flyway-core") ❌
       // implementation("org.flywaydb:flyway-database-postgresql") ❌

       // Liquibase already included via persistence module
   }
   ```

6. **Update docker-compose.yml:**
   ```yaml
   agent-builder-service:
     environment:
       SPRING_FLYWAY_ENABLED: "false"  # Disable Flyway
       SPRING_LIQUIBASE_ENABLED: "true"  # Enable Liquibase
       SPRING_JPA_HIBERNATE_DDL_AUTO: validate  # Keep validate
   ```

7. **Test migration:**
   ```bash
   # Backup agent_db first!
   docker exec healthdata-postgres pg_dump -U healthdata agent_db > agent_db_backup.sql

   # Drop and recreate agent_db (testing clean slate)
   docker exec healthdata-postgres psql -U healthdata -c "DROP DATABASE agent_db;"
   docker exec healthdata-postgres psql -U healthdata -c "CREATE DATABASE agent_db;"

   # Start service with Liquibase
   docker compose up agent-builder-service

   # Check logs
   docker logs healthdata-agent-builder-service | grep -i liquibase
   # Should show successful migration
   ```

8. **Verify schema matches:**
   ```bash
   # Compare tables created by Liquibase vs. original Flyway schema
   docker exec healthdata-postgres psql -U healthdata -d agent_db -c "\dt"
   ```

**Rollback Plan:**
1. Restore agent_db from backup: `psql < agent_db_backup.sql`
2. Revert build.gradle.kts (restore Flyway dependencies)
3. Revert docker-compose.yml (enable Flyway, disable Liquibase)

#### **Task 2.3: Create Entity Validation Tests for Core Services**

**Priority:** P2
**Effort:** 2 days (0.5 day per service)
**Risk:** Low

**Per Service:**
1. Create `*EntityMigrationValidationTest.java` extending base class
2. Run test: `./gradlew :modules:services:SERVICE:test --tests "*EntityMigrationValidationTest"`
3. Fix any failures (entity vs. migration mismatches)
4. Document results in test report

**Deliverables (Phase 2):**
- ✅ 4 core services using Liquibase (fhir, patient, quality-measure, cql-engine)
- ✅ agent-builder migrated from Flyway to Liquibase
- ✅ All 5 services using `ddl-auto: validate`
- ✅ Entity validation tests passing
- ✅ Database migration history tracked in databasechangelog

---

### Phase 3: All Services Migration (Week 3)

**Goal:** Enable Liquibase on remaining 23 services

**Duration:** 7 days
**Risk:** Low-Medium (non-critical services, established pattern)
**Services:** Remaining 23 services (see inventory above)

#### **Task 3.1: Services With Existing Liquibase Changelogs (19 services)**

**Effort:** 3 days (batch processing)
**Risk:** Low (migrations already exist)

**Services:**
- analytics, approval, care-gap, consent, cms-connector, cql-engine, demo-seeding
- documentation, ecr, event-processing, event-router, fhir, hcc, migration-workflow
- notification, predictive-analytics, qrda-export, sales-automation, sdoh

**Batch Script Approach:**

**File:** `scripts/enable-liquibase-batch.sh`
```bash
#!/bin/bash
# Enable Liquibase for all services with existing changelogs

SERVICES=(
  "analytics-service"
  "approval-service"
  # ... (list all 19 services)
)

for SERVICE in "${SERVICES[@]}"; do
  echo "Enabling Liquibase for $SERVICE..."

  # Update docker-compose.yml (requires yq or manual edit)
  # Set SPRING_LIQUIBASE_ENABLED: "true"
  # Set SPRING_JPA_HIBERNATE_DDL_AUTO: validate

  # Test service startup
  docker compose up -d $SERVICE
  sleep 30  # Wait for startup

  # Check health
  # curl http://localhost:PORT/SERVICE/actuator/health

  # Check logs for errors
  docker logs healthdata-$SERVICE | grep -i "error\|exception"

  echo "✓ $SERVICE migration complete"
done
```

**Manual steps per service:**
1. Update docker-compose.yml environment variables
2. Test service starts successfully
3. Verify health check
4. Check databasechangelog table populated
5. Mark as complete in tracking spreadsheet

#### **Task 3.2: Services WITHOUT Liquibase Changelogs (10 services)**

**Effort:** 4 days (0.4 day per service)
**Risk:** Medium (must create migrations from scratch)

**Services:**
- ai-assistant-service, cdr-processor-service, cost-analysis-service
- data-enrichment-service, ehr-connector-service, gateway-admin/clinical/fhir/service
- payer-workflows-service

**Strategy:** Create migrations from existing database schema

**Steps per service:**

1. **Capture current schema:**
   ```bash
   # Dump schema only (no data)
   docker exec healthdata-postgres pg_dump -U healthdata -d SERVICE_db --schema-only > /tmp/SERVICE_schema.sql
   ```

2. **Convert SQL to Liquibase XML:**
   - Use Liquibase's `generateChangeLog` feature:
   ```bash
   liquibase \
     --url=jdbc:postgresql://localhost:5435/SERVICE_db \
     --username=healthdata \
     --password=PASSWORD \
     --changeLogFile=db/changelog/0001-baseline-schema.xml \
     generateChangeLog
   ```

3. **Review generated changelog:**
   - Clean up autogenerated IDs
   - Add proper changeSet descriptions
   - Organize into logical changesets

4. **Create baseline migration:**
   ```xml
   <changeSet id="0001-baseline-schema" author="hdim-platform-team">
       <comment>Baseline schema from existing database</comment>
       <!-- Generated tables, indexes, constraints -->
   </changeSet>
   ```

5. **Test on clean database:**
   ```bash
   # Drop and recreate database
   docker exec healthdata-postgres psql -U healthdata -c "DROP DATABASE SERVICE_db;"
   docker exec healthdata-postgres psql -U healthdata -c "CREATE DATABASE SERVICE_db;"

   # Start service (will run Liquibase)
   docker compose up SERVICE
   ```

6. **Enable validation mode:**
   ```yaml
   SERVICE:
     environment:
       SPRING_LIQUIBASE_ENABLED: "true"
       SPRING_JPA_HIBERNATE_DDL_AUTO: validate
   ```

#### **Task 3.3: prior-auth-service (Flyway → Liquibase)**

**Effort:** 4 hours
**Risk:** Low (same as agent-builder conversion)

**Steps:** Same as Task 2.2 (convert Flyway SQL to Liquibase XML)

**Deliverables (Phase 3):**
- ✅ All 34 services using Liquibase
- ✅ All 34 services using `ddl-auto: validate`
- ✅ 10 new services have baseline migrations created
- ✅ No Flyway dependencies remaining

---

### Phase 4: PostgreSQL 16 Upgrade & User Isolation (Week 4)

**Goal:** Upgrade to PostgreSQL 16, implement separate database users

**Duration:** 5 days
**Risk:** Low (optional optimization)

#### **Task 4.1: Upgrade to PostgreSQL 16**

**Priority:** P3 (nice-to-have)
**Effort:** 2 hours
**Risk:** Low (Liquibase has no compatibility issues)

**Steps:**
1. Backup all databases: `pg_dumpall > full_backup.sql`
2. Stop all services: `docker compose down`
3. Update docker-compose.yml:
   ```yaml
   postgres:
     image: postgres:16-alpine  # Upgrade from 15
   ```
4. Remove volume: `docker volume rm healthdata_postgres_data`
5. Start infrastructure: `docker compose --profile light up -d`
6. Restore databases: `psql < full_backup.sql`
7. Restart all services: `docker compose --profile core up -d`
8. Verify all services healthy

**Validation:**
```bash
docker exec healthdata-postgres psql -U healthdata -c "SELECT version();"
# Should show: PostgreSQL 16.x
```

#### **Task 4.2: Implement Separate Database Users**

**Priority:** P3 (security hardening)
**Effort:** 1 day
**Risk:** Low

**Strategy:** One database user per service

**Script:** `scripts/create-service-users.sh`
```bash
#!/bin/bash
# Create separate database users for each service

SERVICES=(
  "fhir:fhir_db"
  "patient:patient_db"
  "quality:quality_db"
  # ... (all 29 services)
)

for SERVICE_PAIR in "${SERVICES[@]}"; do
  IFS=":" read -r SERVICE DB <<< "$SERVICE_PAIR"
  USER="${SERVICE}_user"
  PASSWORD=$(openssl rand -base64 32)  # Generate secure password

  echo "Creating user: $USER for database: $DB"

  # Create user
  docker exec healthdata-postgres psql -U healthdata <<-EOSQL
    CREATE USER $USER WITH PASSWORD '$PASSWORD';
    GRANT ALL PRIVILEGES ON DATABASE $DB TO $USER;
    REVOKE ALL PRIVILEGES ON DATABASE $DB FROM healthdata;  # Remove default user access
EOSQL

  # Save credentials to .env file
  echo "${SERVICE^^}_DB_USER=$USER" >> .env.users
  echo "${SERVICE^^}_DB_PASSWORD=$PASSWORD" >> .env.users
done
```

**Update docker-compose.yml per service:**
```yaml
fhir-service:
  environment:
    SPRING_DATASOURCE_USERNAME: ${FHIR_DB_USER}
    SPRING_DATASOURCE_PASSWORD: ${FHIR_DB_PASSWORD}
```

**Update .env.example:**
```bash
# Service-specific database users
FHIR_DB_USER=fhir_user
FHIR_DB_PASSWORD=CHANGE_ME
PATIENT_DB_USER=patient_user
PATIENT_DB_PASSWORD=CHANGE_ME
# ... (all 29 services)
```

**Validation:**
```bash
# Test fhir_user can access fhir_db only
docker exec healthdata-postgres psql -U fhir_user -d fhir_db -c "SELECT 1;"
# Should succeed

docker exec healthdata-postgres psql -U fhir_user -d patient_db -c "SELECT 1;"
# Should fail with permission denied
```

**Deliverables (Phase 4):**
- ✅ PostgreSQL 16 running (optional)
- ✅ 29 separate database users created
- ✅ Least privilege access enforced
- ✅ Credentials documented in .env.example
- ✅ HIPAA compliance improved (audit trail per service)

---

## Success Metrics

### Phase Completion Metrics

#### **Phase 1 Success Criteria**

| Metric | Target | Measurement |
|--------|--------|-------------|
| PostgreSQL version | 15.x | `SELECT version();` |
| gateway_db tables | Managed by Liquibase | `SELECT COUNT(*) FROM databasechangelog;` should be > 0 |
| Init script | Databases only (no tables) | Manual inspection of init-multi-db.sh |
| Validation test template | Created and working | Template file exists in test-fixtures |

#### **Phase 2 Success Criteria**

| Metric | Target | Measurement |
|--------|--------|-------------|
| Core services with Liquibase | 4/4 (100%) | Docker logs show "Liquibase update successful" |
| Services using `validate` | 4/4 (100%) | Docker inspect shows `ddl-auto: validate` |
| agent-builder migration | Flyway → Liquibase | No Flyway dependencies in build.gradle.kts |
| Validation tests passing | 4/4 (100%) | `./gradlew test --tests "*EntityMigrationValidationTest"` |
| Zero data loss | 100% | Manual verification or data checksums |

#### **Phase 3 Success Criteria**

| Metric | Target | Measurement |
|--------|--------|-------------|
| All services with Liquibase | 34/34 (100%) | All services have `SPRING_LIQUIBASE_ENABLED: true` |
| Services using `validate` | 34/34 (100%) | No service has `ddl-auto: create` or `update` |
| Services without migrations | 0/34 (0%) | All services have db/changelog directory |
| Baseline migrations created | 10/10 (100%) | New services have 0001-baseline-schema.xml |

#### **Phase 4 Success Criteria**

| Metric | Target | Measurement |
|--------|--------|-------------|
| PostgreSQL version | 16.x (optional) | `SELECT version();` |
| Separate database users | 29/29 (100%) | `SELECT usename FROM pg_user;` shows all service users |
| Least privilege access | 100% | Service A cannot access Service B's database |
| Credentials externalized | 100% | No hardcoded passwords in docker-compose.yml |

### Overall Success Metrics

#### **Schema Management**

| Metric | Current | Target | Formula |
|--------|---------|--------|---------|
| **Migrations enabled** | 3% (1/34) | 100% (34/34) | `(Services with Liquibase enabled) / Total services` |
| **Migrations tracked** | 3% (1/34) | 100% (34/34) | `(Services with databasechangelog populated) / Total` |
| **DDL auto = validate** | 3% (1/34) | 100% (34/34) | `(Services with validate mode) / Total` |
| **Data loss risk** | 53% (18/34) | 0% (0/34) | `(Services with ddl-auto: create) / Total` |

#### **Migration Consistency**

| Metric | Current | Target | Formula |
|--------|---------|--------|---------|
| **Single migration tool** | 71% Liquibase, 6% Flyway, 23% None | 100% Liquibase | Standardization percentage |
| **Services with validation tests** | 3% (1/34) | 100% (34/34) | Services with `*EntityMigrationValidationTest` |
| **Sequential migration numbers** | ~70% | 100% | Manual audit of changelog numbering |

#### **Security & Compliance**

| Metric | Current | Target | Formula |
|--------|---------|--------|---------|
| **Separate DB users** | 0% (1 user for all) | 100% (29 users) | `(Services with dedicated DB user) / Total` |
| **Least privilege** | 0% | 100% | Percentage of services that can ONLY access their own DB |
| **Schema versioning** | 3% | 100% | Databases with migration history tracked |
| **Audit trail** | Partial (audit_logs table) | Complete | All schema changes traceable to migration |

#### **Operational Metrics**

| Metric | Current | Target | Measurement |
|--------|---------|--------|-------------|
| **Zero downtime restarts** | ❌ (data loss) | ✅ (data preserved) | Manual test: restart container, verify data persists |
| **Migration rollback capability** | ❌ | ✅ | `liquibase rollback --count=1` works |
| **Schema drift detection** | Manual | Automatic | Hibernate validation fails on mismatch |
| **Build-time validation** | 3% | 100% | CI/CD runs EntityMigrationValidationTest |

### Reporting Dashboard

**Create file:** `backend/MIGRATION_STATUS.md`

```markdown
# Database Migration Status

Last Updated: 2026-01-XX

## Overall Progress

- **Phase 1:** ✅ Complete (5/5 tasks)
- **Phase 2:** 🔄 In Progress (3/5 tasks)
- **Phase 3:** ⏳ Pending
- **Phase 4:** ⏳ Pending

## Service Status Matrix

| Service | Database | Migrations | DDL Auto | Validated | Status |
|---------|----------|-----------|----------|-----------|--------|
| fhir-service | fhir_db | ✅ Liquibase | ✅ validate | ✅ Yes | ✅ COMPLETE |
| patient-service | patient_db | ✅ Liquibase | ✅ validate | ✅ Yes | ✅ COMPLETE |
| quality-measure | quality_db | 🔄 Liquibase | ⚠️ update | ❌ No | 🔄 IN PROGRESS |
| cql-engine | cql_db | 🔄 Liquibase | ⚠️ update | ❌ No | 🔄 IN PROGRESS |
| agent-builder | agent_db | 🔄 Converting | ✅ validate | ✅ Yes | 🔄 FLYWAY→LIQUIBASE |
| ... | ... | ... | ... | ... | ... |

## Metrics

- **Services Complete:** 2 / 34 (6%)
- **Services In Progress:** 3 / 34 (9%)
- **Services Pending:** 29 / 34 (85%)

## Blockers

- None currently

## Next Steps

1. Complete quality-measure and cql-engine migration (Phase 2)
2. Complete agent-builder Flyway→Liquibase conversion (Phase 2)
3. Begin Phase 3 batch migration (19 services with existing changelogs)
```

---

## Validation Criteria

### Pre-Migration Checklist

Before starting each phase, verify:

- [ ] **Database backup completed** (`pg_dumpall > backup_YYYYMMDD.sql`)
- [ ] **Test environment available** (separate from production)
- [ ] **Rollback plan documented** (per phase)
- [ ] **Team notification sent** (maintenance window if needed)
- [ ] **Docker images built** (if Dockerfile changes)
- [ ] **Environment variables prepared** (.env file with all required secrets)

### Post-Migration Validation (Per Service)

#### **1. Service Startup Validation**

```bash
# Start service
docker compose up -d SERVICE_NAME

# Check service is running
docker ps | grep SERVICE_NAME
# Should show: Up X seconds (healthy)

# Check logs for errors
docker logs healthdata-SERVICE_NAME | grep -i "error\|exception\|failed"
# Should show: No critical errors

# Verify health check
curl http://localhost:PORT/SERVICE/actuator/health
# Expected: {"status":"UP","components":{"db":{"status":"UP"},...}}
```

#### **2. Database Migration Validation**

```bash
# Check Liquibase changelog table
docker exec healthdata-postgres psql -U healthdata -d SERVICE_db \
  -c "SELECT id, filename, orderexecuted, exectype, md5sum
      FROM databasechangelog
      ORDER BY orderexecuted;"

# Expected output:
# id                               | filename                          | orderexecuted | exectype | md5sum
# ---------------------------------|-----------------------------------|---------------|----------|--------
# 0001-create-...-table            | db/changelog/0001-create-...xml   | 1             | EXECUTED | abc123...
# 0002-add-...-indexes             | db/changelog/0002-add-...xml      | 2             | EXECUTED | def456...

# Verify row count matches number of migration files
# ls backend/modules/services/SERVICE/src/main/resources/db/changelog/*.xml | wc -l
```

#### **3. Schema Validation**

```bash
# Check all tables exist
docker exec healthdata-postgres psql -U healthdata -d SERVICE_db -c "\dt"

# Check indexes exist
docker exec healthdata-postgres psql -U healthdata -d SERVICE_db -c "\di"

# Check constraints
docker exec healthdata-postgres psql -U healthdata -d SERVICE_db \
  -c "SELECT conname, contype FROM pg_constraint WHERE conrelid = 'TABLE_NAME'::regclass;"
```

#### **4. Hibernate Validation**

```bash
# Service should start with ddl-auto: validate
# If entities don't match schema, service will fail to start

# Check logs for validation success
docker logs healthdata-SERVICE_NAME | grep -i "hibernate"
# Should show: "HHH000400: Using dialect: org.hibernate.dialect.PostgreSQLDialect"
# Should NOT show: "Schema-validation: missing table", "Schema-validation: wrong column type"
```

#### **5. Entity-Migration Test Validation**

```bash
# Run validation test
./gradlew :modules:services:SERVICE:test --tests "*EntityMigrationValidationTest"

# Expected output:
# > Task :modules:services:SERVICE:test
# EntityMigrationValidationTest > shouldValidateEntitiesMatchMigrations() PASSED
# BUILD SUCCESSFUL
```

#### **6. Data Integrity Validation**

```bash
# If service had data before migration, verify data still exists

# Example for patient-service:
docker exec healthdata-postgres psql -U healthdata -d patient_db \
  -c "SELECT COUNT(*) FROM patient_demographics;"
# Should match pre-migration count

# Check sample data
docker exec healthdata-postgres psql -U healthdata -d patient_db \
  -c "SELECT id, first_name, last_name FROM patient_demographics LIMIT 5;"
# Should show expected data
```

#### **7. API Functional Testing**

```bash
# Test basic CRUD operations via API

# Example: Create patient
curl -X POST http://localhost:8084/patient/api/v1/patients \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: demo-tenant" \
  -d '{"firstName":"Test","lastName":"Patient","dateOfBirth":"1990-01-01"}'
# Expected: 201 Created with patient ID

# Example: Get patient
curl http://localhost:8084/patient/api/v1/patients/{id} \
  -H "X-Tenant-ID: demo-tenant"
# Expected: 200 OK with patient data
```

### Phase Acceptance Criteria

#### **Phase 1 Acceptance**

Before proceeding to Phase 2, verify:

- [ ] PostgreSQL 15 running (or 16 if skipping Phase 4)
- [ ] `docker exec healthdata-postgres psql --version` shows correct version
- [ ] gateway_db has 5 migrations in databasechangelog table
- [ ] `init-multi-db.sh` creates databases only (no tables)
- [ ] Validation test template exists and compiles
- [ ] All 29 databases created successfully
- [ ] pg_trgm extension enabled via migrations (not init script)

**Sign-off:** Team Lead approves Phase 1 completion

#### **Phase 2 Acceptance**

Before proceeding to Phase 3, verify:

- [ ] 4 core services using Liquibase (fhir, patient, quality-measure, cql-engine)
- [ ] All 4 services using `ddl-auto: validate`
- [ ] agent-builder migrated from Flyway to Liquibase
- [ ] No Flyway dependencies in any build.gradle.kts
- [ ] All 5 services have validation tests passing
- [ ] All 5 services start successfully from clean database
- [ ] Health checks return UP for all 5 services
- [ ] Sample API calls work (create, read, update, delete)
- [ ] Zero data loss verified (test data still accessible)

**Sign-off:** Team Lead + Senior Developer approve Phase 2 completion

#### **Phase 3 Acceptance**

Before proceeding to Phase 4, verify:

- [ ] All 34 services using Liquibase
- [ ] All 34 services using `ddl-auto: validate`
- [ ] No service has `ddl-auto: create` or `update`
- [ ] 10 services without prior migrations now have baseline schema
- [ ] All 34 services start successfully
- [ ] Health checks return UP for all services
- [ ] Full system test passes (end-to-end workflow)
- [ ] Performance testing shows no regression

**Sign-off:** Team Lead + QA Lead approve Phase 3 completion

#### **Phase 4 Acceptance**

Final acceptance criteria:

- [ ] PostgreSQL 16 running (optional)
- [ ] 29 separate database users created
- [ ] Each service can ONLY access its own database (tested)
- [ ] Credentials stored in .env file (not docker-compose.yml)
- [ ] `.env.example` updated with all new credentials
- [ ] Security audit passed (principle of least privilege enforced)
- [ ] Full system test passes with new user permissions

**Sign-off:** Team Lead + DevOps Lead + Security Officer approve final migration

---

## Risk Mitigation

### High-Risk Scenarios

#### **Risk 1: Data Loss During Migration**

**Probability:** Medium
**Impact:** HIGH (HIPAA violation, patient data loss)
**Mitigation:**

1. **Before each phase:**
   ```bash
   # Full database backup
   docker exec healthdata-postgres pg_dumpall -U healthdata > backup_$(date +%Y%m%d_%H%M%S).sql

   # Verify backup is valid
   docker exec healthdata-postgres pg_restore --list backup_*.sql
   ```

2. **Test on copy first:**
   - Create staging environment with copy of production data
   - Run migration on staging
   - Verify results before production migration

3. **Atomic rollback plan:**
   - Keep backup accessible during migration
   - Document exact restoration steps
   - Test restoration process before migration

**Rollback Procedure:**
```bash
# Stop all services
docker compose down

# Drop all databases
docker exec healthdata-postgres psql -U healthdata -c "DROP DATABASE fhir_db;" # Repeat for all

# Restore from backup
docker exec -i healthdata-postgres psql -U healthdata < backup_20260110.sql

# Restart services with old configuration
git checkout PREVIOUS_COMMIT docker-compose.yml
docker compose up -d
```

#### **Risk 2: Service Downtime During Migration**

**Probability:** Medium
**Impact:** MEDIUM (service unavailable)
**Mitigation:**

1. **Maintenance window:** Schedule migration during low-usage hours (2-4 AM)
2. **Gradual rollout:** Migrate services one at a time (not all at once)
3. **Blue-green deployment:** Keep old version running while testing new version
4. **Quick rollback:** Automate rollback script

**Rollback Time:** < 5 minutes per service

#### **Risk 3: Migration Fails Midway**

**Probability:** LOW
**Impact:** HIGH (inconsistent state)
**Mitigation:**

1. **Liquibase transactions:** Migrations run in transaction (automatic rollback on failure)
2. **Validation before commit:**
   ```yaml
   <changeSet id="..." author="...">
       <!-- Migration SQL -->
       <rollback>
           <!-- Explicit rollback SQL -->
       </rollback>
   </changeSet>
   ```

3. **Manual rollback capability:**
   ```bash
   # Liquibase can roll back last N changesets
   liquibase --url=jdbc:postgresql://... rollback --count=1
   ```

4. **Checkpoint backups:** Backup database after each successful service migration

#### **Risk 4: PostgreSQL 15 → 16 Upgrade Issues**

**Probability:** LOW
**Impact:** MEDIUM (requires downgrade)
**Mitigation:**

1. **Phase 4 is optional:** Can stay on PostgreSQL 15 indefinitely
2. **Test in staging first:** Verify all services work on PostgreSQL 16 before production
3. **Keep PostgreSQL 15 backup:** Don't delete old backup until verification complete

**If upgrade fails:**
```bash
# Revert to PostgreSQL 15
docker compose down
docker volume rm healthdata_postgres_data
# Update docker-compose.yml to postgres:15-alpine
docker compose --profile light up -d
# Restore from Phase 3 backup
docker exec -i healthdata-postgres psql -U healthdata < backup_phase3.sql
```

#### **Risk 5: Entity-Migration Mismatch Discovered Late**

**Probability:** MEDIUM (without validation tests)
**Impact:** HIGH (service fails to start)
**Mitigation:**

1. **Mandatory validation tests:** Every service MUST have `*EntityMigrationValidationTest`
2. **CI/CD enforcement:** Tests run automatically on every commit
3. **Pre-migration audit:**
   ```bash
   # Run all validation tests before starting migration
   ./gradlew test --tests "*EntityMigrationValidationTest"
   # All tests MUST pass before proceeding
   ```

4. **Migration review:** Senior developer reviews all new migration files

### Medium-Risk Scenarios

#### **Risk 6: Team Unfamiliar with Liquibase**

**Probability:** HIGH
**Impact:** MEDIUM (slower migration, mistakes)
**Mitigation:**

1. **Training session:** 2-hour Liquibase workshop before Phase 1
2. **Documentation:** Provide Liquibase cheat sheet and examples
3. **Pair programming:** Senior developer assists junior developers
4. **Code reviews:** All migration PRs reviewed by Liquibase-experienced developer

#### **Risk 7: Dependency Version Conflicts**

**Probability:** LOW
**Impact:** MEDIUM (build failures)
**Mitigation:**

1. **Version catalog:** All dependencies managed in `gradle/libs.versions.toml`
2. **Spring Boot BOM:** Use Spring Boot's dependency management
3. **Build verification:**
   ```bash
   # After every change, verify all services still build
   ./gradlew build --no-daemon
   ```

### Rollback Decision Tree

```
Migration Failed?
├─ Yes
│  ├─ Data Corrupted?
│  │  ├─ Yes → STOP IMMEDIATELY → Restore from backup → Incident report
│  │  └─ No → Continue
│  ├─ Single Service Issue?
│  │  ├─ Yes → Rollback single service → Fix migration → Retry
│  │  └─ No → Continue
│  └─ Infrastructure Issue?
│     ├─ Yes → Rollback entire phase → Fix infrastructure → Retry
│     └─ No → Debug and fix inline
└─ No → Continue to next phase
```

**Rollback Authority:**
- **Single service:** Developer can rollback
- **Multiple services:** Team Lead approval required
- **Data loss:** Executive approval + incident report required

---

## References

### Industry Best Practices

1. [Microservices.io - Database per service](https://microservices.io/patterns/data/database-per-service.html)
2. [GeeksforGeeks - Microservices Database Design Patterns](https://www.geeksforgeeks.org/sql/microservices-database-design-patterns/)
3. [Medium - Database Per Service Pattern](https://medium.com/@abhi.strike/microservices-patterns-database-per-service-pattern-44e8e2df4612)
4. [PostgreSQL in Microservices Architecture](https://reintech.io/blog/postgresql-microservices-architecture)
5. [Flyway vs Liquibase 2025](https://www.bytebase.com/blog/flyway-vs-liquibase/)
6. [Baeldung - Liquibase vs Flyway](https://www.baeldung.com/liquibase-vs-flyway)
7. [Database Schema Generation with Hibernate](https://prgrmmng.com/database-schema-generation-and-migration-with-hibernate)

### HDIM Documentation

1. **CORE_SERVICES_TEST_REPORT.md** - Current state analysis, Flyway/PostgreSQL 16 issue
2. **CLAUDE.md** - Entity-migration synchronization requirements
3. **backend/docs/ENTITY_MIGRATION_GUIDE.md** - Detailed migration guidelines
4. **backend/HIPAA-CACHE-COMPLIANCE.md** - HIPAA requirements for PHI data

### Migration Tools Documentation

1. [Liquibase Documentation](https://docs.liquibase.com/home.html)
2. [Liquibase Best Practices](https://www.liquibase.com/blog/liquibase-best-practices)
3. [Spring Boot + Liquibase](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration-tool)
4. [Hibernate Validation](https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html#schema-generation)

---

## Appendix

### A. Liquibase Changelog Template

**File:** `db/changelog/NNNN-description.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="NNNN-description" author="developer-name">
        <comment>Brief description of what this migration does</comment>

        <!-- Example: Create table -->
        <createTable tableName="example_table">
            <column name="id" type="UUID" defaultValueComputed="gen_random_uuid()">
                <constraints primaryKey="true" primaryKeyName="pk_example_table"/>
            </column>
            <column name="tenant_id" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="name" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="created_at" type="TIMESTAMP WITH TIME ZONE" defaultValueComputed="CURRENT_TIMESTAMP">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <!-- Example: Create index -->
        <createIndex indexName="idx_example_table_tenant_id" tableName="example_table">
            <column name="tenant_id"/>
        </createIndex>

        <!-- ALWAYS provide rollback -->
        <rollback>
            <dropTable tableName="example_table"/>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

### B. Entity Validation Test Template

See Phase 1, Task 1.4 for complete template.

### C. Environment Variables Reference

**Required environment variables after migration:**

```bash
# PostgreSQL
POSTGRES_PASSWORD=<secure_password>

# Per-service database users (Phase 4)
FHIR_DB_USER=fhir_user
FHIR_DB_PASSWORD=<secure_password>
PATIENT_DB_USER=patient_user
PATIENT_DB_PASSWORD=<secure_password>
# ... (all 29 services)

# Application secrets (existing)
JWT_SECRET=<64-char-hex>
GATEWAY_AUTH_SIGNING_SECRET=<64-char-hex>

# LLM providers (existing)
HDIM_AGENT_LLM_PROVIDERS_CLAUDE_API_KEY=<api_key>
```

### D. Migration Checklist (Per Service)

**Print this checklist for each service migration:**

```
Service: ___________________________
Date: _______________________________
Developer: __________________________

□ Pre-Migration
  □ Service builds successfully
  □ Migration files exist in db/changelog/
  □ db.changelog-master.xml includes all migrations
  □ Validation test created (*EntityMigrationValidationTest)
  □ Database backup completed

□ Migration
  □ docker-compose.yml updated (LIQUIBASE_ENABLED: true, DDL_AUTO: validate)
  □ Service starts successfully
  □ Health check returns UP
  □ Logs show "Liquibase update successful"
  □ No errors in logs

□ Validation
  □ databasechangelog table populated
  □ Row count in databasechangelog matches migration file count
  □ All tables exist (SELECT * FROM information_schema.tables)
  □ All indexes exist (\di in psql)
  □ Validation test passes (./gradlew test --tests "*EntityMigrationValidationTest")
  □ API functional test passes (CRUD operations work)
  □ Data integrity verified (if applicable)

□ Documentation
  □ MIGRATION_STATUS.md updated
  □ PR created with migration changes
  □ PR reviewed and approved
  □ PR merged to main

Sign-off: _________________________ Date: _____________
```

---

**End of Document**

*For questions or issues during migration, contact: HDIM Platform Team*
