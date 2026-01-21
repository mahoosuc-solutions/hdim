---
description: Generate sequential Liquibase migration file for HDIM service
arguments:
  service_name:
    description: Service name (e.g., patient-service, quality-measure-service)
    type: string
    required: true
  description:
    description: Brief description of the migration (e.g., "add-status-to-patients")
    type: string
    required: true
---

# Add Migration Command

Generate a sequential Liquibase migration file for schema changes to an existing HDIM service.

## What This Command Does

1. **Finds Next Migration Number** - Automatically determines the next sequential number
2. **Creates Migration File** - Generates Liquibase changeset with proper structure
3. **Updates Master Changelog** - Adds migration to db.changelog-master.xml
4. **Validates Rollback** - Ensures rollback SQL is included (100% coverage)

## Usage

```bash
/add-migration {{service_name}} "{{description}}"
```

## Examples

```bash
# Add status column to patients table
/add-migration patient-service "add-status-to-patients"

# Create index on last_name column
/add-migration patient-service "add-index-on-patient-last-name"

# Add foreign key relationship
/add-migration care-gap-service "add-patient-fk-to-care-gaps"

# Modify column type
/add-migration quality-measure-service "modify-score-to-decimal"
```

## Implementation

You are tasked with creating a new Liquibase migration file.

### Step 1: Validate Service Exists

```bash
ls backend/modules/services/{{service_name}}
```

### Step 2: Find Next Migration Number

```bash
# List existing migrations
ls backend/modules/services/{{service_name}}/src/main/resources/db/changelog/

# Find highest number (e.g., 0004-xxx.xml)
# Next number: 0005
```

**Critical:** Use sequential numbering (no gaps, no reuse).

### Step 3: Create Migration File

**File Path:**
```
backend/modules/services/{{service_name}}/src/main/resources/db/changelog/{{MIGRATION_ID}}-{{description}}.xml
```

**Template Structure:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="{{MIGRATION_ID}}-{{description}}" author="{{git_user}}">
        <comment>{{description}}</comment>

        <!-- TODO: Add your migration changes here -->
        <!-- Examples below - remove what you don't need -->

        <!-- ADD COLUMN -->
        <!--
        <addColumn tableName="table_name">
            <column name="new_column" type="VARCHAR(255)">
                <constraints nullable="true"/>
            </column>
        </addColumn>
        -->

        <!-- MODIFY COLUMN -->
        <!--
        <modifyDataType tableName="table_name" columnName="column_name" newDataType="TEXT"/>
        -->

        <!-- CREATE INDEX -->
        <!--
        <createIndex indexName="idx_table_column" tableName="table_name">
            <column name="column_name"/>
        </createIndex>
        -->

        <!-- ADD FOREIGN KEY -->
        <!--
        <addForeignKeyConstraint
            constraintName="fk_table_reference"
            baseTableName="base_table"
            baseColumnNames="column_id"
            referencedTableName="referenced_table"
            referencedColumnNames="id"/>
        -->

        <!-- CUSTOM SQL -->
        <!--
        <sql>
            UPDATE table_name SET status = 'ACTIVE' WHERE created_at > NOW() - INTERVAL '30 days';
        </sql>
        -->

        <!-- ROLLBACK (REQUIRED) -->
        <rollback>
            <!-- TODO: Add explicit rollback SQL here -->
            <!-- Example: <dropColumn tableName="table_name" columnName="new_column"/> -->
        </rollback>
    </changeSet>

</databaseChangeLog>
```

### Step 4: Update Master Changelog

Add migration to db.changelog-master.xml:

**File Path:**
```
backend/modules/services/{{service_name}}/src/main/resources/db/changelog/db.changelog-master.xml
```

**Add include:**
```xml
<include file="db/changelog/{{MIGRATION_ID}}-{{description}}.xml"/>
```

### Step 5: Validate Rollback SQL

**Check for rollback coverage:**
```bash
cd backend
./scripts/test-liquibase-rollback.sh
```

**Expected:** 100% changesets have rollback SQL.

### Step 6: Test Migration

**Run service to apply migration:**
```bash
docker compose up {{service_name}}
```

**Check logs for:**
```
Liquibase update successful
```

### Step 7: Run Validation Test

```bash
cd backend
./gradlew :modules:services:{{service_name}}:test --tests "*EntityMigrationValidationTest"
```

**Expected:** All tests pass (if entity was modified, update entity class too).

### Step 8: Summary

```
✅ Migration created successfully!

**Files Created:**
- Migration: backend/modules/services/{{service_name}}/src/main/resources/db/changelog/{{MIGRATION_ID}}-{{description}}.xml

**Files Updated:**
- Master Changelog: db.changelog-master.xml

**Next Steps:**
1. Edit migration file to add your database changes
2. Add rollback SQL (REQUIRED - 100% coverage enforced)
3. If modifying existing entity fields, update @Column annotations
4. Test migration: docker compose up {{service_name}}
5. Run validation: ./gradlew :modules:services:{{service_name}}:test --tests "*EntityMigrationValidationTest"
```

## Common Migration Patterns

### Add Column to Existing Table

```xml
<changeSet id="{{MIGRATION_ID}}-add-status-to-patients" author="{{git_user}}">
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
<changeSet id="{{MIGRATION_ID}}-modify-score-to-decimal" author="{{git_user}}">
    <comment>Change score from integer to decimal for precision</comment>

    <modifyDataType tableName="quality_measures" columnName="score" newDataType="DECIMAL(5,2)"/>

    <rollback>
        <modifyDataType tableName="quality_measures" columnName="score" newDataType="INT"/>
    </rollback>
</changeSet>
```

### Create Index

```xml
<changeSet id="{{MIGRATION_ID}}-add-index-on-last-name" author="{{git_user}}">
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
<changeSet id="{{MIGRATION_ID}}-add-patient-fk" author="{{git_user}}">
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

### Add Composite Index (Multi-Tenant Performance)

```xml
<changeSet id="{{MIGRATION_ID}}-add-composite-index" author="{{git_user}}">
    <comment>Add composite index on tenant_id and created_at for faster queries</comment>

    <createIndex indexName="idx_patients_tenant_created" tableName="patients">
        <column name="tenant_id"/>
        <column name="created_at"/>
    </createIndex>

    <rollback>
        <dropIndex tableName="patients" indexName="idx_patients_tenant_created"/>
    </rollback>
</changeSet>
```

### Enable PostgreSQL Extension

```xml
<changeSet id="{{MIGRATION_ID}}-enable-pg-trgm" author="{{git_user}}">
    <comment>Enable pg_trgm for fuzzy text search</comment>

    <sql>CREATE EXTENSION IF NOT EXISTS pg_trgm;</sql>

    <rollback>
        <sql>DROP EXTENSION IF EXISTS pg_trgm;</sql>
    </rollback>
</changeSet>
```

### Data Migration

```xml
<changeSet id="{{MIGRATION_ID}}-migrate-status-data" author="{{git_user}}">
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

## Column Type Mapping

| Java Type | PostgreSQL Type | Liquibase Type |
|-----------|----------------|----------------|
| UUID | uuid | UUID |
| String (≤255) | VARCHAR(N) | VARCHAR(N) |
| String (>255) | TEXT | TEXT |
| Instant | timestamp with time zone | TIMESTAMP WITH TIME ZONE |
| LocalDate | date | DATE |
| LocalDateTime | timestamp | TIMESTAMP |
| Boolean | boolean | BOOLEAN |
| Integer | integer | INT |
| Long | bigint | BIGINT |
| BigDecimal | decimal(P,S) | DECIMAL(P,S) |

## Best Practices

### 1. Sequential Numbering
**ALWAYS** use the next sequential number. No gaps, no reuse.

### 2. Rollback SQL (REQUIRED)
**EVERY** changeset MUST include explicit rollback SQL. 100% coverage enforced.

### 3. Descriptive IDs
Use descriptive migration IDs:
- ✅ `0005-add-status-to-patients`
- ❌ `0005-update-table`

### 4. Tenant Isolation
When adding indexes, consider adding composite index with `tenant_id` first for multi-tenant performance.

### 5. Entity Synchronization
If modifying columns for existing entities, update `@Column` annotations to match.

### 6. Test Before Commit
**ALWAYS** test migration locally:
```bash
docker compose up {{service_name}}
```

Check logs for: `Liquibase update successful`

## Rollback SQL Patterns

| Operation | Rollback |
|-----------|----------|
| `createTable` | `<dropTable tableName="..."/>` |
| `addColumn` | `<dropColumn tableName="..." columnName="..."/>` |
| `modifyDataType` | `<modifyDataType tableName="..." columnName="..." newDataType="ORIGINAL_TYPE"/>` |
| `createIndex` | `<dropIndex tableName="..." indexName="..."/>` |
| `addForeignKeyConstraint` | `<dropForeignKeyConstraint baseTableName="..." constraintName="..."/>` |
| `insert` | `<delete tableName="..."><where>...</where></delete>` |
| `update` | `<update tableName="...">` (reverse values) |
| `sql` | `<sql>` (reverse operation) |
| `addNotNullConstraint` | `<dropNotNullConstraint tableName="..." columnName="..."/>` |
| `ANALYZE` | Empty `<rollback/>` (statistics only) |

## Troubleshooting

### Migration already exists
**Error:** `Changeset already ran with different SQL`
**Fix:** Create NEW migration (never modify existing ones)

### Rollback validation fails
**Error:** `Changeset lacks rollback SQL`
**Fix:** Add explicit `<rollback>` tags to changeset

### Entity-migration validation fails
**Error:** `Schema-validation: wrong column type`
**Fix:** Update entity `@Column` annotation to match migration OR update migration to match entity

### Migration numbering conflict
**Error:** Another developer used same number
**Fix:** Rename migration to next available number, update master changelog

## Related Commands

- `/add-entity` - Add entity with synchronized migration
- `/validate-schema` - Run EntityMigrationValidationTest

## Related Skills

- `database-migrations` - Liquibase best practices

## Related Agents

- `migration-validator` - Auto-validates migrations (proactive)

## Documentation

- See `backend/docs/ENTITY_MIGRATION_GUIDE.md` for complete guide
- See `backend/docs/DATABASE_MIGRATION_RUNBOOK.md` for operations
- See `DATABASE_ARCHITECTURE_MIGRATION_PLAN.md` for architecture
