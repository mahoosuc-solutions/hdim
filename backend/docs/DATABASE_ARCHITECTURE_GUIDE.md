# Database Architecture & Schema Management Guide

---
**Navigation:** [CLAUDE.md](../../CLAUDE.md#database-architecture--schema-management) | [Documentation Portal](../../docs/README.md) | [Backend Docs Index](./README.md)
---

## Overview

HDIM uses the **Database-per-Service** pattern with **Liquibase** for all schema migrations. Each of the 29 microservices has its own logical database on a shared PostgreSQL instance, ensuring service isolation and independent schema evolution.

**Key Principles:**

- ✅ One database per service (29 databases total)
- ✅ Liquibase for ALL services (standard tool)
- ✅ `ddl-auto: validate` in all environments
- ✅ Entity-migration synchronization enforced
- ❌ Never use `ddl-auto: create` or `update` (causes data loss/drift)

---

## Database Inventory

**PostgreSQL Version:** 16-alpine
**Total Databases:** 29 (see `DATABASE_ARCHITECTURE_MIGRATION_PLAN.md` for complete list)

### Core Databases

| Database | Purpose | Service | Port |
|----------|---------|---------|------|
| `fhir_db` | FHIR R4 resources | fhir-service | 8085 |
| `patient_db` | Patient demographics | patient-service | 8084 |
| `quality_db` | HEDIS measures | quality-measure-service | 8087 |
| `cql_db` | CQL evaluation | cql-engine-service | 8081 |
| `caregap_db` | Care gap detection | care-gap-service | 8086 |
| `gateway_db` | Authentication | gateway-service | 8001 |

**Additional Databases:** 23 more services (see `DATABASE_ARCHITECTURE_MIGRATION_PLAN.md`)

---

## Migration Standards

### Liquibase: The Only Tool

All services MUST use Liquibase for database migrations. Flyway is NOT supported.

#### Gradle Configuration

```kotlin
// build.gradle.kts - Liquibase included via shared persistence module
dependencies {
    implementation(project(":modules:shared:infrastructure:persistence"))
    // This includes: Liquibase 4.29.2, PostgreSQL driver, HikariCP
}
```

#### Spring Configuration

```yaml
# docker-compose.yml or application.yml
spring:
  liquibase:
    enabled: true # MUST be true
    change-log: classpath:db/changelog/db.changelog-master.xml
  jpa:
    hibernate:
      ddl-auto: validate # MUST be validate (never create/update)
```

**Critical:** Never set `ddl-auto` to `create` or `update` in production or Docker. This allows Hibernate to auto-create tables, bypassing Liquibase versioning and causing schema drift.

---

## Migration File Structure

### Directory Layout

```
src/main/resources/db/changelog/
├── 0000-enable-extensions.xml           # PostgreSQL extensions (pg_trgm, etc.)
├── 0001-create-patients-table.xml       # Initial schema
├── 0002-create-insurance-table.xml      # Related tables
├── 0003-add-composite-indexes.xml       # Performance indexes
├── 0004-add-risk-score-column.xml       # Schema evolution
└── db.changelog-master.xml               # Master file including all migrations
```

### Naming Convention

- **Use 4-digit sequential numbers:** `0001`, `0002`, `0003`
- **Use descriptive names:** `create-TABLE-table`, `add-FIELD-to-TABLE`
- **Never reuse numbers** or modify existing migrations
- **Never skip numbers** (no gaps in sequence)
- **Extensions first:** Always use `0000-enable-extensions.xml` as first migration

---

## Master Changelog Template

```xml
<!-- db.changelog-master.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <include file="db/changelog/0000-enable-extensions.xml"/>
    <include file="db/changelog/0001-create-patients-table.xml"/>
    <include file="db/changelog/0002-create-insurance-table.xml"/>
    <!-- Add new migrations here, never modify existing includes -->
</databaseChangeLog>
```

---

## Migration File Template

```xml
<!-- 0001-create-patients-table.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="0001-create-patients-table" author="developer-name">
        <comment>Create patients table with tenant isolation</comment>

        <createTable tableName="patients">
            <column name="id" type="UUID" defaultValueComputed="gen_random_uuid()">
                <constraints primaryKey="true" primaryKeyName="pk_patients"/>
            </column>
            <column name="tenant_id" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="first_name" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="last_name" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="date_of_birth" type="DATE">
                <constraints nullable="false"/>
            </column>
            <column name="created_at" type="TIMESTAMP WITH TIME ZONE"
                    defaultValueComputed="CURRENT_TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="updated_at" type="TIMESTAMP WITH TIME ZONE"
                    defaultValueComputed="CURRENT_TIMESTAMP">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <createIndex indexName="idx_patients_tenant_id" tableName="patients">
            <column name="tenant_id"/>
        </createIndex>

        <!-- ALWAYS provide explicit rollback -->
        <rollback>
            <dropTable tableName="patients"/>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

**Key Points:**
- Every changeset has a unique `id` matching the filename
- Always include a descriptive `<comment>`
- Always provide explicit `<rollback>` directives
- Use `defaultValueComputed="gen_random_uuid()"` for UUID generation
- Use `TIMESTAMP WITH TIME ZONE` for temporal data

---

## Rollback SQL Coverage

**Status:** ✅ **100% coverage achieved** (199/199 changesets)

Every Liquibase changeset in HDIM includes explicit rollback SQL, ensuring safe reversion of database changes in production.

### Validation & Enforcement

- **Automated Testing:** `backend/scripts/test-liquibase-rollback.sh`
- **CI/CD Enforcement:** GitHub Actions validates on every PR
- **Documentation:** `backend/docs/DATABASE_MIGRATION_RUNBOOK.md`

### Common Rollback Patterns

| Operation | Rollback |
|-----------|----------|
| Create table | `<dropTable tableName="..."/>` |
| Add column | `<dropColumn tableName="..." columnName="..."/>` |
| Insert data | `<delete tableName="..."><where>...</where></delete>` |
| Update data | Reverse update with original values |
| Create index | `<dropIndex tableName="..." indexName="..."/>` |
| Add comments | `COMMENT ON ... IS NULL` |
| Add constraint | `<dropConstraint tableName="..." constraintName="..."/>` |
| ANALYZE | Empty rollback (statistics only) |

### Why 100% Coverage Matters

- ✅ Enables safe production rollbacks without data loss
- ✅ Validates migration reversibility during development
- ✅ Reduces deployment risk for database changes
- ✅ Provides disaster recovery capability
- ✅ Enforces discipline (forces thinking about reversibility)

---

## PostgreSQL Extensions

Extensions should be managed in Liquibase migrations, not initialization scripts.

### Extension Management

```xml
<!-- 0000-enable-extensions.xml -->
<changeSet id="0000-enable-extensions" author="hdim-platform-team">
    <comment>Enable PostgreSQL extensions for full-text search</comment>
    <sql>CREATE EXTENSION IF NOT EXISTS pg_trgm;</sql>
    <rollback>DROP EXTENSION IF EXISTS pg_trgm;</rollback>
</changeSet>
```

### Common Extensions

| Extension | Purpose | Used By |
|-----------|---------|---------|
| `pg_trgm` | Trigram matching for fuzzy text search | FHIR, CQL, Quality, Patient services |
| `uuid-ossp` | UUID generation utilities | Optional (prefer `gen_random_uuid()`) |
| `btree_gin` | Generalized inverted index | Advanced search (if needed) |
| `plpgsql` | PL/pgSQL language (usually pre-installed) | Stored procedures |

**Best Practice:** Enable extensions in your service's first migration (`0000-enable-extensions.xml`).

---

## Database Initialization

### Startup Workflow

1. **Docker Compose starts PostgreSQL container**
2. **`docker/postgres/init-multi-db.sh` creates all 29 databases**
3. **Each service boots and runs Liquibase migrations**
4. **Services become available**

### Init Script Responsibility

The `docker/postgres/init-multi-db.sh` script:

```bash
# Creates databases only - NO tables, NO extensions
CREATE DATABASE fhir_db;
CREATE DATABASE patient_db;
# ... (all 29 databases)

GRANT ALL PRIVILEGES ON DATABASE fhir_db TO healthdata;
# ... (all grants)
```

**What it DOES:**
- ✅ Creates empty databases
- ✅ Grants privileges to database user
- ✅ Sets encoding and locale

**What it DOES NOT do:**
- ❌ Create tables (Liquibase does this)
- ❌ Create extensions (Liquibase does this)
- ❌ Run migrations (Liquibase does this on service startup)

This separation ensures:
- Services own their schemas
- Migrations are version-controlled
- Rollbacks are possible
- Independent service deployment

---

## Migration Workflow

### Scenario 1: Creating a New Entity

**Step 1:** Create JPA entity with proper annotations

```java
@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "status", nullable = true)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
```

**Step 2:** Create Liquibase migration file

```xml
<!-- 0005-create-appointments-table.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="0005-create-appointments-table" author="your-name">
        <comment>Create appointments table for scheduling</comment>

        <createTable tableName="appointments">
            <column name="id" type="UUID" defaultValueComputed="gen_random_uuid()">
                <constraints primaryKey="true" primaryKeyName="pk_appointments"/>
            </column>
            <column name="tenant_id" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="appointment_date" type="DATE">
                <constraints nullable="false"/>
            </column>
            <column name="status" type="VARCHAR(50)">
                <constraints nullable="true"/>
            </column>
            <column name="created_at" type="TIMESTAMP WITH TIME ZONE"
                    defaultValueComputed="CURRENT_TIMESTAMP">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <createIndex indexName="idx_appointments_tenant" tableName="appointments">
            <column name="tenant_id"/>
        </createIndex>

        <rollback>
            <dropTable tableName="appointments"/>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

**Step 3:** Add migration to master changelog

```xml
<!-- db.changelog-master.xml -->
<include file="db/changelog/0005-create-appointments-table.xml"/>
```

**Step 4:** Run validation test

```bash
./gradlew :modules:services:YOUR-SERVICE:test --tests "*EntityMigrationValidationTest"
```

**Step 5:** Verify migration runs successfully

```bash
docker compose up YOUR-SERVICE
# Check logs for: "Liquibase update successful" or "Liquibase locked"
# Monitor: docker compose logs -f YOUR-SERVICE | grep -i liquibase
```

### Scenario 2: Modifying an Existing Entity

**⚠️ CRITICAL RULE:** Never modify existing migrations!

**Step 1:** Update entity with new field

```java
// Add new field to existing entity
@Entity
@Table(name = "appointments")
public class Appointment {
    // ... existing fields

    @Column(name = "priority", nullable = true)  // NEW FIELD
    private String priority;
}
```

**Step 2:** Create NEW migration (not update old one)

```xml
<!-- 0006-add-priority-to-appointments.xml -->
<changeSet id="0006-add-priority-to-appointments" author="your-name">
    <comment>Add priority field for appointment scheduling</comment>
    <addColumn tableName="appointments">
        <column name="priority" type="VARCHAR(50)" defaultValue="NORMAL">
            <constraints nullable="true"/>  <!-- Allow null for existing rows -->
        </column>
    </addColumn>
    <rollback>
        <dropColumn tableName="appointments" columnName="priority"/>
    </rollback>
</changeSet>
```

**Step 3:** Add to master changelog

```xml
<!-- db.changelog-master.xml -->
<include file="db/changelog/0006-add-priority-to-appointments.xml"/>
```

**Step 4:** Validate and test

```bash
./gradlew :modules:services:YOUR-SERVICE:test --tests "*EntityMigrationValidationTest"
docker compose up YOUR-SERVICE
```

---

## Common Liquibase Operations

### Create Table

```xml
<createTable tableName="table_name">
    <column name="id" type="UUID" defaultValueComputed="gen_random_uuid()">
        <constraints primaryKey="true"/>
    </column>
    <column name="data" type="VARCHAR(255)">
        <constraints nullable="false"/>
    </column>
</createTable>
```

### Add Column

```xml
<addColumn tableName="table_name">
    <column name="new_column" type="VARCHAR(255)" defaultValue="default">
        <constraints nullable="true"/>
    </column>
</addColumn>
```

### Create Index

```xml
<createIndex indexName="idx_table_column" tableName="table_name">
    <column name="column_name"/>
</createIndex>
```

### Add Foreign Key

```xml
<addForeignKeyConstraint
    constraintName="fk_appointments_patient"
    baseTableName="appointments"
    baseColumnNames="patient_id"
    referencedTableName="patients"
    referencedColumnNames="id"
    onDelete="CASCADE"/>
```

### Modify Column Type

```xml
<modifyDataType tableName="table_name" columnName="column_name" newDataType="TEXT"/>
```

### Add Unique Constraint

```xml
<addUniqueConstraint tableName="table_name" columnNames="unique_field"
    constraintName="uk_table_unique_field"/>
```

### Run Custom SQL

```xml
<sql>
    UPDATE patients SET status = 'ACTIVE'
    WHERE created_at > NOW() - INTERVAL '30 days';
</sql>
<rollback>
    UPDATE patients SET status = NULL
    WHERE created_at > NOW() - INTERVAL '30 days';
</rollback>
```

### Add Column with Constraint

```xml
<addColumn tableName="appointments">
    <column name="status" type="VARCHAR(50)">
        <constraints nullable="false" check="status IN ('SCHEDULED','COMPLETED','CANCELLED')"/>
    </column>
</addColumn>
```

---

## Column Type Mapping

### Java ↔ PostgreSQL ↔ Liquibase

| Java Type | PostgreSQL | Liquibase | Notes |
|-----------|-----------|-----------|-------|
| String (≤255) | VARCHAR(255) | VARCHAR(255) | Standard string |
| String (large) | TEXT | TEXT | Unlimited length |
| UUID | uuid | UUID | `gen_random_uuid()` function |
| Instant | timestamp with time zone | TIMESTAMP WITH TIME ZONE | Always use with timezone |
| LocalDate | date | DATE | Date only, no time |
| LocalDateTime | timestamp | TIMESTAMP | Timestamp without timezone |
| Boolean | boolean | BOOLEAN | True/false |
| Integer | integer | INT | 32-bit integer |
| Long | bigint | BIGINT | 64-bit integer |
| BigDecimal | decimal(x,y) | DECIMAL | For money amounts |
| byte[] | bytea | BLOB | Binary data |

---

## Troubleshooting

### Error: Schema-Validation Missing Table

**Error Message:**
```
Schema-validation: missing table [appointments]
```

**Cause:** JPA entity exists but no Liquibase migration created

**Fix:**
1. Create migration file `000N-create-appointments-table.xml`
2. Add to master changelog
3. Restart service to run migration
4. Verify: `docker compose logs SERVICE | grep -i liquibase`

### Error: Wrong Column Type

**Error Message:**
```
Schema-validation: wrong column type encountered in column [appointment_date]
Expected: date, Actual: timestamp with time zone
```

**Cause:** JPA entity uses `LocalDate` but database has `TIMESTAMP`

**Fix:** Create migration to fix column type

```xml
<modifyDataType tableName="appointments" columnName="appointment_date" newDataType="DATE"/>
```

### Error: Relation Already Exists

**Error Message:**
```
Liquibase: relation "patients" already exists
```

**Cause:** Trying to create table that already exists (usually from old `ddl-auto: create`)

**Fix:**
```bash
# Check what's already in database
docker exec healthdata-postgres psql -U healthdata -d SERVICE_db \
  -c "SELECT id, filename FROM databasechangelog ORDER BY orderexecuted;"

# Option 1: If Liquibase hasn't recorded it, add baseline
# Option 2: If it exists but wasn't created by Liquibase, drop and migrate
# Option 3: Create preCondition to skip if exists
```

### Error: Liquibase Locked

**Error Message:**
```
Liquibase: Could not acquire change log lock
```

**Cause:** Previous migration crashed, leaving lock

**Fix:**
```bash
# Method 1: Auto-clear via script
./backend/scripts/clear-liquibase-locks.sh SERVICE_db

# Method 2: Manual clear
docker exec healthdata-postgres psql -U healthdata -d SERVICE_db \
  -c "DELETE FROM databasechangeloglock WHERE ID=1;"

# Restart service
docker compose restart SERVICE
```

### Error: Migration Failed, Need to Rollback

**Error Message:** Service won't start due to bad migration

**Fix:**
```bash
# View migration history
docker exec healthdata-postgres psql -U healthdata -d SERVICE_db \
  -c "SELECT id, orderexecuted FROM databasechangelog ORDER BY orderexecuted DESC LIMIT 5;"

# Remove last changeset from history
docker exec healthdata-postgres psql -U healthdata -d SERVICE_db \
  -c "DELETE FROM databasechangelog WHERE id = '0006-add-priority-to-appointments';"

# Manually rollback the changes (if not automated)
docker exec healthdata-postgres psql -U healthdata -d SERVICE_db \
  -c "ALTER TABLE appointments DROP COLUMN priority;"

# Restart service (will try migration again)
docker compose restart SERVICE
```

---

## Database Architecture Evolution

### Current Status (Phase 4 Complete)

**Date:** January 10, 2026

- ✅ Phase 1: Fixed critical `ddl-auto` issues
- ✅ Phase 2: Migrated Flyway services to Liquibase
- ✅ Phase 3: Moved gateway auth tables to Liquibase
- ✅ Phase 4: Service-owned extension management
- 🔄 Phase 5: CI/CD enforcement (in progress)

### Achievements

- PostgreSQL 16 running with 29 databases
- Init script simplified to database creation only
- All schema management moved to service Liquibase migrations
- Gateway authentication schema version-controlled
- PostgreSQL extensions managed by services
- 199 changesets with 100% rollback coverage

### Future Direction (Phase 5)

- Automated validation on all PRs
- Integration test coverage for database changes
- Performance monitoring for schema evolution
- Multi-environment schema comparison tooling

---

## Related Documentation

- **[Build Management Guide](./BUILD_MANAGEMENT_GUIDE.md)** - Docker builds and Gradle dependency management
- **[Liquibase Development Workflow](./LIQUIBASE_DEVELOPMENT_WORKFLOW.md)** ⭐ CRITICAL - Day-to-day database development practices
- **[Database Migration Runbook](./DATABASE_MIGRATION_RUNBOOK.md)** - Operational procedures
- **[Entity Migration Guide](./ENTITY_MIGRATION_GUIDE.md)** - Complete JPA/Liquibase synchronization
- **[Database Architecture Migration Plan](../../DATABASE_ARCHITECTURE_MIGRATION_PLAN.md)** - Phase-by-phase evolution

---

_Last Updated: January 19, 2026_
_Version: 1.0_