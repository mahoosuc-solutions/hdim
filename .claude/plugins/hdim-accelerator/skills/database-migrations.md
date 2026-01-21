---
description: Liquibase database migration best practices for HDIM platform
tags:
  - database
  - liquibase
  - postgresql
  - migrations
  - entity-migration-sync
---

# Database Migrations Skill

Comprehensive guidance for Liquibase database migrations in the HDIM platform.

<skill_instructions>

## Overview

The HDIM platform uses **Liquibase 4.29.2** for all database schema management across 29 microservices. This skill provides best practices, patterns, and troubleshooting guidance based on HDIM's production-tested standards.

**Key Principles:**
- ✅ Database-per-service pattern (29 separate databases)
- ✅ Liquibase for ALL services (no Flyway, no Hibernate ddl-auto)
- ✅ `ddl-auto: validate` in all environments (NEVER create/update)
- ✅ 100% rollback SQL coverage (enforced)
- ✅ Sequential migration numbering (no gaps)
- ✅ Entity-migration synchronization (prevents drift)

---

## HDIM Migration Standards

### 1. Sequential Numbering

**Format:** `NNNN-descriptive-name.xml`

**Examples:**
- ✅ `0000-enable-extensions.xml`
- ✅ `0001-create-patients-table.xml`
- ✅ `0002-create-insurance-table.xml`
- ✅ `0003-add-composite-indexes.xml`
- ❌ `0001-update.xml` (not descriptive)
- ❌ `create-patients.xml` (no number)

**Rules:**
- Start at 0000 (or 0001 for first table)
- No gaps in numbering
- Never reuse numbers
- Never modify existing migrations

### 2. Migration File Structure

**Template:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="NNNN-descriptive-name" author="developer-name">
        <comment>Clear description of what this migration does</comment>

        <!-- Database changes here -->

        <!-- REQUIRED: Explicit rollback -->
        <rollback>
            <!-- Reverse operation here -->
        </rollback>
    </changeSet>

</databaseChangeLog>
```

**Key Elements:**
- `id`: Must match filename (e.g., `0001-create-patients-table`)
- `author`: Git username (for audit trail)
- `comment`: Clear description of purpose
- `rollback`: **REQUIRED** - explicit rollback SQL (100% coverage)

### 3. Master Changelog

**Location:** `src/main/resources/db/changelog/db.changelog-master.xml`

**Structure:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <include file="db/changelog/0000-enable-extensions.xml"/>
    <include file="db/changelog/0001-create-patients-table.xml"/>
    <include file="db/changelog/0002-create-insurance-table.xml"/>
    <!-- Add new migrations here -->

</databaseChangeLog>
```

**Rules:**
- Includes are executed in order
- Never modify existing includes
- Add new migrations at end

### 4. Rollback SQL Coverage (100% Required)

**Why It Matters:**
- Enables production rollbacks
- Validates reversibility during development
- Reduces deployment risk

**Validation:**
```bash
cd backend
./scripts/test-liquibase-rollback.sh
```

**Expected:** `✅ 100% of changesets have rollback SQL (XXX/XXX)`

**Common Patterns:**

| Operation | Rollback |
|-----------|----------|
| `<createTable>` | `<dropTable tableName="..."/>` |
| `<addColumn>` | `<dropColumn tableName="..." columnName="..."/>` |
| `<modifyDataType>` | `<modifyDataType ... newDataType="ORIGINAL_TYPE"/>` |
| `<createIndex>` | `<dropIndex tableName="..." indexName="..."/>` |
| `<addForeignKeyConstraint>` | `<dropForeignKeyConstraint baseTableName="..." constraintName="..."/>` |
| `<insert>` | `<delete tableName="..."><where>...</where></delete>` |
| `<update>` | `<update tableName="...">` (reverse values) |
| `<sql>CREATE EXTENSION` | `<sql>DROP EXTENSION IF EXISTS</sql>` |
| `<sql>ANALYZE` | `<rollback/>` (empty - statistics only) |

---

## Entity-Migration Synchronization

**Critical Pattern:** Prevents production schema drift (e.g., RefreshToken bug).

### The Problem

Without synchronization:
- Developer creates JPA entity
- Forgets to create migration
- `ddl-auto: create` runs in dev (auto-creates table)
- Migration never created
- Production deployment fails (no table exists)

### The Solution

**HDIM Standard:** Entity-migration validation enforced at multiple levels.

**1. Development (Local)**
```bash
./gradlew :modules:services:SERVICE-NAME:test --tests "*EntityMigrationValidationTest"
```

**2. Pre-commit Hook**
```bash
# Auto-runs when entity or migration files change
ln -s ../../backend/scripts/pre-commit-db-validation.sh .git/hooks/pre-commit
```

**3. CI/CD (GitHub Actions)**
```yaml
# Runs on every PR
- name: Validate Entity-Migration Sync
  run: ./gradlew test --tests "*EntityMigrationValidationTest"
```

### Synchronization Workflow

**When creating entity:**
1. Create JPA entity with `@Entity`, `@Table`, `@Column` annotations
2. Create Liquibase migration with matching table/columns
3. Add migration to db.changelog-master.xml
4. Run validation: `/validate-schema SERVICE-NAME`
5. Commit both files together

**When modifying entity:**
1. Update `@Column` annotation
2. Create NEW migration (never modify existing)
3. Add migration to master changelog
4. Run validation: `/validate-schema SERVICE-NAME`
5. Commit both files together

### Column Type Mapping

**Critical:** JPA types must match Liquibase types.

| Java Type | JPA Annotation | PostgreSQL | Liquibase |
|-----------|---------------|------------|-----------|
| UUID | `@Id @GeneratedValue(strategy = UUID)` | uuid | UUID |
| String (≤255) | `@Column(length = N)` | VARCHAR(N) | VARCHAR(N) |
| String (>255) | `@Column(columnDefinition = "TEXT")` | TEXT | TEXT |
| Instant | `@Column` | timestamp with time zone | TIMESTAMP WITH TIME ZONE |
| LocalDate | `@Column` | date | DATE |
| LocalDateTime | `@Column` | timestamp | TIMESTAMP |
| Boolean | `@Column` | boolean | BOOLEAN |
| Integer | `@Column` | integer | INT |
| Long | `@Column` | bigint | BIGINT |
| BigDecimal | `@Column(precision = P, scale = S)` | decimal(P,S) | DECIMAL(P,S) |

---

## Multi-Tenant Patterns

**CRITICAL:** Every table MUST support tenant isolation.

### Required Fields

```xml
<!-- Tenant isolation (REQUIRED) -->
<column name="tenant_id" type="VARCHAR(100)">
    <constraints nullable="false"/>
</column>
```

```java
@Column(name = "tenant_id", nullable = false, length = 100)
private String tenantId;
```

### Required Indexes

**Performance Critical:** Tenant queries MUST be fast.

```xml
<!-- Index on tenant_id (REQUIRED for multi-tenant) -->
<createIndex indexName="idx_{{TABLE_NAME}}_tenant_id" tableName="{{TABLE_NAME}}">
    <column name="tenant_id"/>
</createIndex>
```

### Composite Indexes

For better query performance:
```xml
<!-- Composite index: tenant_id + frequently queried column -->
<createIndex indexName="idx_patients_tenant_created" tableName="patients">
    <column name="tenant_id"/>
    <column name="created_at"/>
</createIndex>
```

**Why tenant_id first:**
- PostgreSQL uses left-most column for index selection
- All queries filter by tenant_id
- Improves query performance 10-100x

---

## Common Migration Operations

### Create Table with Tenant Isolation

```xml
<changeSet id="0001-create-patients-table" author="developer">
    <comment>Create patients table with tenant isolation</comment>

    <createTable tableName="patients">
        <!-- Primary Key -->
        <column name="id" type="UUID" defaultValueComputed="gen_random_uuid()">
            <constraints primaryKey="true" primaryKeyName="pk_patients"/>
        </column>

        <!-- Multi-tenant Isolation (REQUIRED) -->
        <column name="tenant_id" type="VARCHAR(100)">
            <constraints nullable="false"/>
        </column>

        <!-- Domain Fields -->
        <column name="first_name" type="VARCHAR(100)">
            <constraints nullable="false"/>
        </column>
        <column name="last_name" type="VARCHAR(100)">
            <constraints nullable="false"/>
        </column>
        <column name="date_of_birth" type="DATE">
            <constraints nullable="false"/>
        </column>

        <!-- Audit Fields -->
        <column name="created_at" type="TIMESTAMP WITH TIME ZONE" defaultValueComputed="CURRENT_TIMESTAMP">
            <constraints nullable="false"/>
        </column>
        <column name="updated_at" type="TIMESTAMP WITH TIME ZONE" defaultValueComputed="CURRENT_TIMESTAMP">
            <constraints nullable="false"/>
        </column>
        <column name="created_by" type="VARCHAR(255)"/>
        <column name="updated_by" type="VARCHAR(255)"/>
    </createTable>

    <!-- Tenant index (REQUIRED) -->
    <createIndex indexName="idx_patients_tenant_id" tableName="patients">
        <column name="tenant_id"/>
    </createIndex>

    <!-- Rollback (REQUIRED) -->
    <rollback>
        <dropTable tableName="patients"/>
    </rollback>
</changeSet>
```

### Add Column to Existing Table

```xml
<changeSet id="0005-add-status-to-patients" author="developer">
    <comment>Add status field for patient tracking</comment>

    <addColumn tableName="patients">
        <column name="status" type="VARCHAR(50)" defaultValue="ACTIVE">
            <constraints nullable="false"/>
        </column>
    </addColumn>

    <rollback>
        <dropColumn tableName="patients" columnName="status"/>
    </rollback>
</changeSet>
```

### Modify Column Type

```xml
<changeSet id="0006-modify-score-to-decimal" author="developer">
    <comment>Change score from integer to decimal for precision</comment>

    <modifyDataType tableName="quality_measures" columnName="score" newDataType="DECIMAL(5,2)"/>

    <rollback>
        <modifyDataType tableName="quality_measures" columnName="score" newDataType="INT"/>
    </rollback>
</changeSet>
```

### Create Index

```xml
<changeSet id="0007-add-index-on-last-name" author="developer">
    <comment>Add index on last_name for faster patient search</comment>

    <createIndex indexName="idx_patients_last_name" tableName="patients">
        <column name="last_name"/>
    </createIndex>

    <rollback>
        <dropIndex tableName="patients" indexName="idx_patients_last_name"/>
    </rollback>
</changeSet>
```

### Add Foreign Key

```xml
<changeSet id="0008-add-patient-fk" author="developer">
    <comment>Add foreign key constraint to patients table</comment>

    <addForeignKeyConstraint
        constraintName="fk_care_gaps_patient"
        baseTableName="care_gaps"
        baseColumnNames="patient_id"
        referencedTableName="patients"
        referencedColumnNames="id"
        onDelete="CASCADE"/>

    <rollback>
        <dropForeignKeyConstraint
            baseTableName="care_gaps"
            constraintName="fk_care_gaps_patient"/>
    </rollback>
</changeSet>
```

### Enable PostgreSQL Extension

```xml
<changeSet id="0000-enable-pg-trgm" author="platform-team">
    <comment>Enable pg_trgm for fuzzy text search</comment>

    <sql>CREATE EXTENSION IF NOT EXISTS pg_trgm;</sql>

    <rollback>
        <sql>DROP EXTENSION IF EXISTS pg_trgm;</sql>
    </rollback>
</changeSet>
```

**Common Extensions:**
- `pg_trgm` - Fuzzy text search (used by fhir, cql, quality, patient services)
- `uuid-ossp` - UUID generation (optional, prefer `gen_random_uuid()`)

### Data Migration

```xml
<changeSet id="0009-migrate-status-data" author="developer">
    <comment>Set default status for existing patients</comment>

    <sql>
        UPDATE patients
        SET status = 'ACTIVE'
        WHERE status IS NULL AND created_at > NOW() - INTERVAL '1 year';
    </sql>

    <sql>
        UPDATE patients
        SET status = 'INACTIVE'
        WHERE status IS NULL AND created_at <= NOW() - INTERVAL '1 year';
    </sql>

    <rollback>
        <sql>UPDATE patients SET status = NULL;</sql>
    </rollback>
</changeSet>
```

---

## Hibernate Configuration

**CRITICAL:** Use `ddl-auto: validate` in ALL environments.

### application.yml

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # REQUIRED (never create/update)
    show-sql: false  # Set true only in dev for debugging

  liquibase:
    enabled: true  # REQUIRED
    change-log: classpath:db/changelog/db.changelog-master.xml
```

### Why validate-only?

| Setting | Behavior | Risk |
|---------|----------|------|
| `validate` ✅ | Validates schema matches entities | None - safe for production |
| `none` | No validation | High - drift can occur unnoticed |
| `create` ❌ | Drops and recreates schema | **CRITICAL - DATA LOSS** |
| `create-drop` ❌ | Create on start, drop on shutdown | **CRITICAL - DATA LOSS** |
| `update` ❌ | Auto-updates schema | High - causes unpredictable drift |

**HDIM Standard:** `ddl-auto: validate` enforced via EntityMigrationValidationTest.

---

## Testing Migrations

### Local Testing

**1. Start service:**
```bash
docker compose up SERVICE-NAME
```

**2. Check logs:**
```
Liquibase update successful
Successfully applied X changesets
```

**3. Verify database:**
```bash
docker exec -it hdim-postgres psql -U healthdata -d SERVICE_db

# Check table exists
\dt

# Check table structure
\d table_name

# Check migration history
SELECT * FROM databasechangelog ORDER BY orderexecuted DESC LIMIT 10;
```

### Validation Testing

**Run EntityMigrationValidationTest:**
```bash
cd backend
./gradlew :modules:services:SERVICE-NAME:test --tests "*EntityMigrationValidationTest"
```

**Expected:**
```
EntityMigrationValidationTest > shouldValidateEntitiesMatchMigrations() PASSED
```

### Rollback Testing

**Check rollback coverage:**
```bash
cd backend
./scripts/test-liquibase-rollback.sh
```

**Expected:**
```
✅ 100% of changesets have rollback SQL (XXX/XXX)
```

---

## Troubleshooting

### Migration Fails to Run

**Error:** `Liquibase update failed: Validation Failed`

**Common Causes:**
1. **Syntax error in XML**
   - Fix: Validate XML syntax
   - Check closing tags, quotes, attributes

2. **Column already exists**
   - Cause: Migration ran before, or `ddl-auto: update` created column
   - Fix: Check `databasechangelog` table to see what ran

3. **Table already exists**
   - Cause: Migration ran before, or manual table creation
   - Fix: Add `<preConditions><not><tableExists tableName="..."/></not></preConditions>`

### Entity-Migration Validation Fails

**Error:** `Schema-validation: missing table [table_name]`

**Cause:** Entity exists but no migration creates table.

**Fix:** Run `/add-entity SERVICE-NAME EntityName`

---

**Error:** `Schema-validation: wrong column type encountered in column [column_name]`

**Cause:** Entity `@Column` type doesn't match Liquibase column type.

**Fix:** Update migration to match entity OR update entity to match migration.

### Rollback Coverage Fails

**Error:** `Changeset XXX lacks rollback SQL`

**Cause:** Changeset missing `<rollback>` tag.

**Fix:** Add explicit rollback:
```xml
<rollback>
    <!-- Reverse operation here -->
</rollback>
```

### Migration Number Conflict

**Error:** Another developer used same migration number.

**Fix:**
1. Rename your migration to next available number
2. Update changeset `id` attribute to match filename
3. Update db.changelog-master.xml include

---

## Best Practices Summary

**DO:**
- ✅ Use sequential numbering (0000, 0001, 0002...)
- ✅ Include explicit rollback SQL (100% coverage)
- ✅ Add migrations to db.changelog-master.xml
- ✅ Test migrations locally before committing
- ✅ Run EntityMigrationValidationTest
- ✅ Include tenant_id in every table
- ✅ Create index on tenant_id for performance
- ✅ Use `ddl-auto: validate` exclusively
- ✅ Commit entity and migration together

**DON'T:**
- ❌ Modify existing migrations
- ❌ Skip migration numbers
- ❌ Reuse migration numbers
- ❌ Use `ddl-auto: create` or `update`
- ❌ Forget rollback SQL
- ❌ Create tables without tenant_id
- ❌ Commit entity without migration (or vice versa)

---

## Quick Reference

**Commands:**
```bash
# Create entity with migration
/add-entity SERVICE-NAME EntityName "Description"

# Create standalone migration
/add-migration SERVICE-NAME "description"

# Validate schema
/validate-schema SERVICE-NAME

# Test rollback coverage
./scripts/test-liquibase-rollback.sh

# View migration history
docker exec hdim-postgres psql -U healthdata -d SERVICE_db -c "SELECT * FROM databasechangelog;"
```

**Files:**
- Entity: `src/main/java/com/healthdata/SERVICE/domain/model/Entity.java`
- Repository: `src/main/java/com/healthdata/SERVICE/domain/repository/EntityRepository.java`
- Migration: `src/main/resources/db/changelog/NNNN-description.xml`
- Master: `src/main/resources/db/changelog/db.changelog-master.xml`

**Documentation:**
- Complete guide: `backend/docs/ENTITY_MIGRATION_GUIDE.md`
- Runbook: `backend/docs/DATABASE_MIGRATION_RUNBOOK.md`
- Architecture: `DATABASE_ARCHITECTURE_MIGRATION_PLAN.md`

</skill_instructions>
