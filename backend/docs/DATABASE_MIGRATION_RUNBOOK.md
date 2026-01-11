# Database Migration Runbook

**Version:** 1.0
**Last Updated:** 2026-01-10
**Status:** Production Ready

## Overview

This runbook provides comprehensive guidance for database schema management in the HDIM platform. All 34 microservices use the **Database-per-Service** pattern with **Liquibase** for version-controlled schema migrations.

## Table of Contents

- [Quick Reference](#quick-reference)
- [Creating New Entities](#creating-new-entities)
- [Modifying Existing Entities](#modifying-existing-entities)
- [Common Liquibase Operations](#common-liquibase-operations)
- [Validation and Testing](#validation-and-testing)
- [Troubleshooting](#troubleshooting)
- [Rollback Procedures](#rollback-procedures)
- [Best Practices](#best-practices)

---

## Quick Reference

### Key Principles

✅ **DO:**
- Use `ddl-auto: validate` in ALL environments
- Create Liquibase migrations for ALL schema changes
- Run validation tests before committing
- Use sequential migration numbers (0001, 0002, 0003...)
- Include explicit rollback in every changeset
- Create extensions in `0000-enable-extensions.xml`

❌ **DON'T:**
- Use `ddl-auto: create`, `update`, or `create-drop`
- Modify existing migration files
- Skip migration numbers or reuse them
- Commit entity changes without migrations
- Create tables in init scripts

### Essential Commands

```bash
# Validate entity-migration synchronization
./gradlew test --tests "*EntityMigrationValidationTest"

# Validate specific service
./gradlew :modules:services:SERVICE-NAME:test --tests "*EntityMigrationValidationTest"

# Build service (includes validation if ddl-auto: validate)
./gradlew :modules:services:SERVICE-NAME:build

# Check Liquibase status
docker exec healthdata-postgres psql -U healthdata -d SERVICE_db \
  -c "SELECT id, author, filename FROM databasechangelog ORDER BY orderexecuted;"

# View database schema
docker exec healthdata-postgres psql -U healthdata -d SERVICE_db -c "\d+ table_name"
```

---

## Creating New Entities

### Workflow

#### Step 1: Create JPA Entity

```java
package com.healthdata.SERVICE.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

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

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "appointment_time", nullable = false)
    private LocalTime appointmentTime;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        status = "SCHEDULED";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
```

**Key Annotations:**
- `@Entity` - Marks class as JPA entity
- `@Table(name = "...")` - Specifies database table name
- `@Column(name = "...")` - Maps field to column
- `@Id` - Primary key field
- `@GeneratedValue(strategy = GenerationType.UUID)` - Auto-generate UUID

#### Step 2: Determine Next Migration Number

```bash
# List existing migrations
ls -1 src/main/resources/db/changelog/

# Example output:
# 0000-enable-extensions.xml
# 0001-create-patients-table.xml
# 0002-create-insurance-table.xml
# 0003-add-composite-indexes.xml

# Next number: 0004
```

#### Step 3: Create Migration SQL File

**File:** `src/main/resources/db/changelog/sql/0004-create-appointments-table.sql`

```sql
-- Create appointments table
CREATE TABLE appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    patient_id UUID NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    status VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE
);

-- Indexes for performance
CREATE INDEX idx_appointments_tenant_id ON appointments(tenant_id);
CREATE INDEX idx_appointments_patient_id ON appointments(patient_id);
CREATE INDEX idx_appointments_date ON appointments(appointment_date);

-- Composite index for common queries
CREATE INDEX idx_appointments_tenant_patient ON appointments(tenant_id, patient_id);

-- Foreign key constraint (if patient table exists in same database)
ALTER TABLE appointments
    ADD CONSTRAINT fk_appointments_patient
    FOREIGN KEY (patient_id)
    REFERENCES patients(id)
    ON DELETE CASCADE;
```

#### Step 4: Create Migration XML File

**File:** `src/main/resources/db/changelog/0004-create-appointments-table.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.23.xsd">

    <changeSet id="0004-create-appointments-table" author="developer-name">
        <comment>Create appointments table for scheduling patient visits</comment>

        <sqlFile path="db/changelog/sql/0004-create-appointments-table.sql"
                 relativeToChangelogFile="true"/>

        <rollback>
            <sql>
                DROP TABLE IF EXISTS appointments;
            </sql>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

#### Step 5: Update Master Changelog

**File:** `src/main/resources/db/changelog/db.changelog-master.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.23.xsd">

    <!-- Extensions must come FIRST -->
    <include file="db/changelog/0000-enable-extensions.xml"/>

    <!-- Schema migrations -->
    <include file="db/changelog/0001-create-patients-table.xml"/>
    <include file="db/changelog/0002-create-insurance-table.xml"/>
    <include file="db/changelog/0003-add-composite-indexes.xml"/>
    <include file="db/changelog/0004-create-appointments-table.xml"/> <!-- NEW -->

</databaseChangeLog>
```

#### Step 6: Run Validation Test

```bash
# Run entity-migration validation
cd backend
./gradlew :modules:services:SERVICE-NAME:test --tests "*EntityMigrationValidationTest"

# Expected output:
# ✅ EntityMigrationValidationTest > testEntityMigrationSynchronization() PASSED
```

#### Step 7: Test Migration Locally

```bash
# Stop service
docker compose stop SERVICE-NAME

# Drop and recreate database (optional - for fresh test)
docker exec healthdata-postgres psql -U healthdata -c "DROP DATABASE IF EXISTS SERVICE_db;"
docker exec healthdata-postgres psql -U healthdata -c "CREATE DATABASE SERVICE_db;"

# Start service (Liquibase will run migrations)
docker compose up SERVICE-NAME

# Verify table created
docker exec healthdata-postgres psql -U healthdata -d SERVICE_db -c "\d+ appointments"

# Check Liquibase changelog
docker exec healthdata-postgres psql -U healthdata -d SERVICE_db \
  -c "SELECT id, author FROM databasechangelog WHERE id = '0004-create-appointments-table';"
```

#### Step 8: Commit Changes

```bash
git add src/main/java/com/healthdata/SERVICE/domain/model/Appointment.java
git add src/main/resources/db/changelog/sql/0004-create-appointments-table.sql
git add src/main/resources/db/changelog/0004-create-appointments-table.xml
git add src/main/resources/db/changelog/db.changelog-master.xml
git commit -m "feat(SERVICE): Add Appointment entity with Liquibase migration"
```

---

## Modifying Existing Entities

### Workflow

**CRITICAL:** Never modify existing migration files. Always create a NEW migration.

#### Step 1: Modify Entity

```java
@Entity
@Table(name = "appointments")
public class Appointment {
    // ... existing fields

    // NEW FIELD
    @Column(name = "provider_id")
    private UUID providerId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
```

#### Step 2: Create Migration SQL

**File:** `src/main/resources/db/changelog/sql/0005-add-provider-notes-to-appointments.sql`

```sql
-- Add provider_id and notes columns to appointments
ALTER TABLE appointments
    ADD COLUMN provider_id UUID,
    ADD COLUMN notes TEXT;

-- Index for provider queries
CREATE INDEX idx_appointments_provider_id ON appointments(provider_id);

-- Foreign key to provider table (if applicable)
ALTER TABLE appointments
    ADD CONSTRAINT fk_appointments_provider
    FOREIGN KEY (provider_id)
    REFERENCES providers(id)
    ON DELETE SET NULL;
```

#### Step 3: Create Migration XML

**File:** `src/main/resources/db/changelog/0005-add-provider-notes-to-appointments.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.23.xsd">

    <changeSet id="0005-add-provider-notes-to-appointments" author="developer-name">
        <comment>Add provider_id and notes columns to appointments table</comment>

        <sqlFile path="db/changelog/sql/0005-add-provider-notes-to-appointments.sql"
                 relativeToChangelogFile="true"/>

        <rollback>
            <sql>
                ALTER TABLE appointments
                    DROP CONSTRAINT IF EXISTS fk_appointments_provider,
                    DROP COLUMN IF EXISTS provider_id,
                    DROP COLUMN IF EXISTS notes;
                DROP INDEX IF EXISTS idx_appointments_provider_id;
            </sql>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

#### Step 4: Update Master Changelog

```xml
<include file="db/changelog/0004-create-appointments-table.xml"/>
<include file="db/changelog/0005-add-provider-notes-to-appointments.xml"/> <!-- NEW -->
```

#### Step 5: Test and Commit

```bash
# Validate
./gradlew :modules:services:SERVICE-NAME:test --tests "*EntityMigrationValidationTest"

# Restart service to apply migration
docker compose restart SERVICE-NAME

# Commit
git add src/main/java/com/healthdata/SERVICE/domain/model/Appointment.java
git add src/main/resources/db/changelog/sql/0005-add-provider-notes-to-appointments.sql
git add src/main/resources/db/changelog/0005-add-provider-notes-to-appointments.xml
git add src/main/resources/db/changelog/db.changelog-master.xml
git commit -m "feat(SERVICE): Add provider and notes fields to appointments"
```

---

## Common Liquibase Operations

### Add Column

```xml
<changeSet id="NNNN-add-column-to-table" author="developer-name">
    <addColumn tableName="table_name">
        <column name="new_column" type="VARCHAR(255)">
            <constraints nullable="true"/>
        </column>
    </addColumn>
    <rollback>
        <dropColumn tableName="table_name" columnName="new_column"/>
    </rollback>
</changeSet>
```

### Modify Column Type

```xml
<changeSet id="NNNN-modify-column-type" author="developer-name">
    <modifyDataType tableName="table_name"
                    columnName="column_name"
                    newDataType="TEXT"/>
    <rollback>
        <modifyDataType tableName="table_name"
                        columnName="column_name"
                        newDataType="VARCHAR(255)"/>
    </rollback>
</changeSet>
```

### Add Not Null Constraint

```xml
<changeSet id="NNNN-add-not-null-constraint" author="developer-name">
    <!-- First, set default value for existing rows -->
    <update tableName="table_name">
        <column name="column_name" value="DEFAULT_VALUE"/>
        <where>column_name IS NULL</where>
    </update>

    <!-- Then add constraint -->
    <addNotNullConstraint tableName="table_name"
                          columnName="column_name"
                          columnDataType="VARCHAR(100)"/>

    <rollback>
        <dropNotNullConstraint tableName="table_name" columnName="column_name"/>
    </rollback>
</changeSet>
```

### Create Index

```xml
<changeSet id="NNNN-create-index" author="developer-name">
    <createIndex indexName="idx_table_column" tableName="table_name">
        <column name="column_name"/>
    </createIndex>
    <rollback>
        <dropIndex indexName="idx_table_column" tableName="table_name"/>
    </rollback>
</changeSet>
```

### Add Foreign Key

```xml
<changeSet id="NNNN-add-foreign-key" author="developer-name">
    <addForeignKeyConstraint
        constraintName="fk_child_parent"
        baseTableName="child_table"
        baseColumnNames="parent_id"
        referencedTableName="parent_table"
        referencedColumnNames="id"
        onDelete="CASCADE"/>

    <rollback>
        <dropForeignKeyConstraint
            baseTableName="child_table"
            constraintName="fk_child_parent"/>
    </rollback>
</changeSet>
```

### Rename Column

```xml
<changeSet id="NNNN-rename-column" author="developer-name">
    <renameColumn tableName="table_name"
                  oldColumnName="old_name"
                  newColumnName="new_name"
                  columnDataType="VARCHAR(100)"/>

    <rollback>
        <renameColumn tableName="table_name"
                      oldColumnName="new_name"
                      newColumnName="old_name"
                      columnDataType="VARCHAR(100)"/>
    </rollback>
</changeSet>
```

### Drop Table

```xml
<changeSet id="NNNN-drop-table" author="developer-name">
    <dropTable tableName="table_name"/>

    <!-- Include CREATE TABLE in rollback -->
    <rollback>
        <createTable tableName="table_name">
            <column name="id" type="UUID">
                <constraints primaryKey="true"/>
            </column>
            <!-- ... other columns ... -->
        </createTable>
    </rollback>
</changeSet>
```

---

## Validation and Testing

### Entity-Migration Validation Test

Every service should have this test:

**File:** `src/test/java/com/healthdata/SERVICE/persistence/EntityMigrationValidationTest.java`

```java
@SpringBootTest
@Testcontainers
class EntityMigrationValidationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.liquibase.enabled", () -> "true");
    }

    @Test
    void testEntityMigrationSynchronization() {
        // If this test passes, entities match Liquibase schema
        assertTrue(true, "Entity-migration synchronization validated");
    }
}
```

### Running Validation

```bash
# All services
./gradlew test --tests "*EntityMigrationValidationTest"

# Specific service
./gradlew :modules:services:patient-service:test --tests "*EntityMigrationValidationTest"

# With detailed output
./gradlew :modules:services:patient-service:test --tests "*EntityMigrationValidationTest" --info
```

### CI/CD Integration

**GitHub Actions** automatically runs validation on PRs:

```yaml
# .github/workflows/database-validation.yml
on:
  pull_request:
    paths:
      - 'backend/modules/services/*/src/main/java/**/*Entity.java'
      - 'backend/modules/services/*/src/main/resources/db/changelog/**'
```

### Pre-Commit Hook

```bash
# Install pre-commit hook
ln -s ../../backend/scripts/pre-commit-db-validation.sh .git/hooks/pre-commit

# Hook runs automatically on git commit
git commit -m "feat: add new entity"
# 🔍 JPA entities changed, running entity-migration validation...
# ✅ Entity-migration validation passed
```

---

## Troubleshooting

### Problem: Validation Failure - Missing Table

**Error:**
```
Schema-validation: missing table [appointments]
```

**Cause:** JPA entity exists but Liquibase migration not created.

**Fix:**
1. Create Liquibase migration for the table
2. Update master changelog
3. Restart service
4. Run validation test

### Problem: Validation Failure - Wrong Column Type

**Error:**
```
Schema-validation: wrong column type encountered in column [appointment_date] in table [appointments]
Expected: date
Actual: timestamp with time zone
```

**Cause:** Entity annotation doesn't match database column type.

**Fix Option 1: Update Entity** (if database is correct)
```java
// Change from:
@Column(name = "appointment_date")
private LocalDate appointmentDate;

// To:
@Column(name = "appointment_date")
private Instant appointmentDate;
```

**Fix Option 2: Update Database** (if entity is correct)
```xml
<changeSet id="NNNN-fix-appointment-date-type" author="developer-name">
    <modifyDataType tableName="appointments"
                    columnName="appointment_date"
                    newDataType="DATE"/>
</changeSet>
```

### Problem: Migration Already Applied

**Error:**
```
Liquibase: relation "appointments" already exists
```

**Cause:** Table exists but migration wasn't recorded in `databasechangelog`.

**Fix:**
```bash
# Check what migrations ran
docker exec healthdata-postgres psql -U healthdata -d SERVICE_db \
  -c "SELECT id FROM databasechangelog ORDER BY orderexecuted;"

# Manually insert changeset record
docker exec healthdata-postgres psql -U healthdata -d SERVICE_db \
  -c "INSERT INTO databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase)
      VALUES ('0004-create-appointments-table', 'developer-name', 'db/changelog/0004-create-appointments-table.xml', NOW(), 4, 'EXECUTED', '8:abc123', 'sqlFile', 'Create appointments table', NULL, '4.23.0');"
```

### Problem: Service Won't Start - Connection Error

**Error:**
```
Failed to obtain JDBC Connection
Connection to localhost:5435 refused
```

**Cause:** PostgreSQL not running or wrong connection settings.

**Fix:**
```bash
# Check PostgreSQL is running
docker compose ps | grep postgres

# Start PostgreSQL
docker compose up -d postgres

# Check connection settings in application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5435/SERVICE_db
    username: healthdata
    password: ${POSTGRES_PASSWORD}
```

### Problem: Duplicate Migration Numbers

**Error:**
```
Multiple changesets with id '0005-...' found
```

**Cause:** Two migration files use the same number.

**Fix:**
1. Rename one migration file to next available number
2. Update changeset id inside XML file
3. Update master changelog

---

## Rollback Procedures

### Rollback Last Migration

**Step 1: Identify Last Migration**
```bash
docker exec healthdata-postgres psql -U healthdata -d SERVICE_db \
  -c "SELECT id, filename, orderexecuted FROM databasechangelog ORDER BY orderexecuted DESC LIMIT 1;"
```

**Step 2: Execute Rollback SQL**
```bash
# Get rollback SQL from migration XML file
# Example: DROP TABLE appointments;

docker exec healthdata-postgres psql -U healthdata -d SERVICE_db \
  -c "DROP TABLE IF EXISTS appointments;"
```

**Step 3: Remove from Changelog**
```bash
docker exec healthdata-postgres psql -U healthdata -d SERVICE_db \
  -c "DELETE FROM databasechangelog WHERE id = '0004-create-appointments-table';"
```

**Step 4: Remove Migration Files**
```bash
git rm src/main/resources/db/changelog/sql/0004-create-appointments-table.sql
git rm src/main/resources/db/changelog/0004-create-appointments-table.xml
# Update db.changelog-master.xml to remove include
git commit -m "rollback: remove appointments table migration"
```

### Emergency Rollback - Recreate Database

**WARNING:** This deletes ALL data in the database.

```bash
# Stop service
docker compose stop SERVICE-NAME

# Drop database
docker exec healthdata-postgres psql -U healthdata -c "DROP DATABASE SERVICE_db;"

# Recreate database
docker exec healthdata-postgres psql -U healthdata -c "CREATE DATABASE SERVICE_db;"
docker exec healthdata-postgres psql -U healthdata -c "GRANT ALL PRIVILEGES ON DATABASE SERVICE_db TO healthdata;"

# Restart service (Liquibase will recreate schema)
docker compose up SERVICE-NAME
```

---

## Best Practices

### Migration File Organization

```
db/changelog/
├── sql/                           # SQL files (actual DDL)
│   ├── 0000-enable-extensions.sql
│   ├── 0001-create-schema.sql
│   └── 0002-add-indexes.sql
├── 0000-enable-extensions.xml     # XML wrappers
├── 0001-create-schema.xml
├── 0002-add-indexes.xml
└── db.changelog-master.xml         # Master include list
```

### Naming Conventions

**Migration Files:**
- `NNNN-verb-noun-to-table.xml`
- `0001-create-patients-table.xml`
- `0002-add-status-to-appointments.xml`
- `0003-modify-provider-name-type.xml`

**ChangeSet IDs:**
- Match filename: `id="0001-create-patients-table"`
- Use descriptive author: `author="developer-name"` or `author="team-name"`

**SQL Files:**
- Match XML filename: `0001-create-patients-table.sql`
- Include comments explaining purpose

### Column Type Mapping

| Java Type | PostgreSQL Type | Liquibase Type |
|-----------|-----------------|----------------|
| `String` (short) | `VARCHAR(255)` | `VARCHAR(255)` |
| `String` (long) | `TEXT` | `TEXT` |
| `UUID` | `uuid` | `UUID` |
| `Instant` | `timestamp with time zone` | `TIMESTAMP WITH TIME ZONE` |
| `LocalDate` | `date` | `DATE` |
| `LocalTime` | `time` | `TIME` |
| `LocalDateTime` | `timestamp` | `TIMESTAMP` |
| `Boolean` | `boolean` | `BOOLEAN` |
| `Integer` | `integer` | `INT` |
| `Long` | `bigint` | `BIGINT` |
| `BigDecimal` | `numeric` | `NUMERIC(19,2)` |
| `byte[]` | `bytea` | `BYTEA` |

### Performance Considerations

**Index Creation:**
```sql
-- Good: Indexes for foreign keys
CREATE INDEX idx_appointments_patient_id ON appointments(patient_id);

-- Good: Composite indexes for common queries
CREATE INDEX idx_appointments_tenant_date ON appointments(tenant_id, appointment_date);

-- Good: Partial indexes for filtered queries
CREATE INDEX idx_active_appointments ON appointments(appointment_date)
    WHERE status = 'SCHEDULED';
```

**Large Tables:**
```xml
<!-- Use batching for data migrations -->
<changeSet id="NNNN-update-large-table" author="developer-name">
    <sql>
        UPDATE appointments SET status = 'ACTIVE'
        WHERE created_at > NOW() - INTERVAL '30 days'
        AND status IS NULL;
    </sql>
</changeSet>
```

### Multi-Tenant Isolation

**Always include tenant_id:**
```sql
CREATE TABLE table_name (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,  -- REQUIRED
    -- ... other columns
);

-- Index for tenant filtering
CREATE INDEX idx_table_tenant_id ON table_name(tenant_id);
```

### Audit Trail

**Include audit columns:**
```sql
CREATE TABLE table_name (
    id UUID PRIMARY KEY,
    -- ... business columns ...
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(100)
);
```

### Comments and Documentation

```xml
<changeSet id="0005-add-risk-score" author="analytics-team">
    <comment>
        Add risk_score column for ML predictions.
        Related to: JIRA-1234
        Migration safe for production: Yes (nullable column)
    </comment>
    <!-- ... -->
</changeSet>
```

---

## Rollback Testing Framework

### Overview

The HDIM platform includes automated tools for testing Liquibase rollback functionality. These tools ensure all migrations have proper rollback SQL defined and can be safely reversed if needed.

### Test Rollback SQL Definitions

**Script:** `backend/scripts/test-liquibase-rollback.sh`

This script analyzes all Liquibase changesets across all services and verifies that each has a proper `<rollback>` tag defined.

**Usage:**
```bash
cd backend
./scripts/test-liquibase-rollback.sh
```

**Output:**
```
═══════════════════════════════════════════════════════════════════
  Liquibase Rollback Testing Framework
═══════════════════════════════════════════════════════════════════

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Service: patient-service
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Analyzing changesets for patient-service...
    ✓ 0001-create-patient-demographics-table - Has rollback
    ✓ 0002-create-patient-insurance-table - Has rollback
    ✗ 0003-add-composite-indexes - Missing rollback

  Service Summary:
    Total changesets: 3
    With rollback: 2
    Without rollback: 1

═══════════════════════════════════════════════════════════════════
  Final Report
═══════════════════════════════════════════════════════════════════

Services:
  Total services scanned: 34
  Services with migrations: 22

Changesets:
  Total changesets: 156
  With rollback: 148 (95%)
  Without rollback: 8 (5%)

⚠ Services with missing rollback SQL:
  ✗ patient-service (1 missing)
  ✗ quality-measure-service (2 missing)
```

**Integration with CI/CD:**

Add to `.github/workflows/database-validation.yml`:

```yaml
- name: Test Rollback SQL
  run: |
    cd backend
    ./scripts/test-liquibase-rollback.sh
```

### Execute Rollback

**Script:** `backend/scripts/rollback-migration.sh`

This script executes actual rollback for a specific service and changeset.

**⚠️ WARNING:** This modifies the database. Always backup first!

**Usage:**
```bash
# Rollback last changeset
./scripts/rollback-migration.sh patient-service count:1

# Rollback last 3 changesets
./scripts/rollback-migration.sh quality-measure-service count:3

# Rollback specific changeset
./scripts/rollback-migration.sh fhir-service changeset:0004-add-risk-score-column
```

**Safety Checklist:**
1. ✅ Backup database before rollback
2. ✅ Test in non-production environment first
3. ✅ Verify rollback SQL in XML file
4. ✅ Confirm application still works after rollback
5. ✅ Run entity-migration validation test

**Example Workflow:**

```bash
# 1. Create backup
docker exec healthdata-postgres pg_dump -U healthdata patient_db > backup_patient_db_$(date +%Y%m%d_%H%M%S).sql

# 2. Execute rollback
./scripts/rollback-migration.sh patient-service changeset:0005-add-provider-notes

# 3. Verify schema
docker exec healthdata-postgres psql -U healthdata -d patient_db -c "\d+ appointments"

# 4. Run validation test
./gradlew :modules:services:patient-service:test --tests "*EntityMigrationValidationTest"

# 5. Restart service
docker compose restart patient-service
```

### Manual Rollback Process

If the script doesn't work or you need more control:

**Step 1: Identify Changeset**
```bash
docker exec healthdata-postgres psql -U healthdata -d patient_db -c \
  "SELECT orderexecuted, id, author FROM databasechangelog ORDER BY orderexecuted DESC LIMIT 5;"
```

**Step 2: Extract Rollback SQL**
```bash
# Find the changeset XML file
grep -r "id=\"0005-add-provider-notes\"" backend/modules/services/patient-service/src/main/resources/db/changelog --include="*.xml" -l

# View the rollback section
cat backend/modules/services/patient-service/src/main/resources/db/changelog/0005-add-provider-notes.xml
```

**Step 3: Execute Rollback SQL**
```bash
# Execute the SQL from the <rollback> tag
docker exec healthdata-postgres psql -U healthdata -d patient_db -c \
  "ALTER TABLE appointments DROP COLUMN IF EXISTS provider_notes;"
```

**Step 4: Remove from Changelog**
```bash
docker exec healthdata-postgres psql -U healthdata -d patient_db -c \
  "DELETE FROM databasechangelog WHERE id = '0005-add-provider-notes';"
```

**Step 5: Verify**
```bash
# Check table structure
docker exec healthdata-postgres psql -U healthdata -d patient_db -c "\d+ appointments"

# Run validation test
./gradlew :modules:services:patient-service:test --tests "*EntityMigrationValidationTest"
```

### Best Practices

**1. Always Include Rollback SQL**
```xml
<changeSet id="0005-add-column" author="developer-name">
    <addColumn tableName="table_name">
        <column name="new_column" type="VARCHAR(255)"/>
    </addColumn>

    <!-- REQUIRED: Explicit rollback -->
    <rollback>
        <dropColumn tableName="table_name" columnName="new_column"/>
    </rollback>
</changeSet>
```

**2. Test Rollback Before Production**
```bash
# In dev environment
./scripts/rollback-migration.sh patient-service changeset:0005-add-column

# Verify application still works
# Re-apply migration
docker compose restart patient-service
```

**3. Document Non-Reversible Changes**
```xml
<changeSet id="0010-data-migration" author="developer-name">
    <comment>
        Migrates patient data to new format.
        WARNING: This migration is NOT reversible - data transformation is lossy.
        Backup required before applying.
    </comment>

    <sql>UPDATE patients SET status = 'ACTIVE' WHERE status IS NULL;</sql>

    <rollback>
        <comment>Cannot rollback - original NULL values cannot be restored</comment>
        <sql>-- Manual intervention required to restore data from backup</sql>
    </rollback>
</changeSet>
```

**4. Use Transactions**
```xml
<changeSet id="0011-multi-step" author="developer-name" runInTransaction="true">
    <!-- Multiple operations in one transaction -->
    <addColumn tableName="table_name">
        <column name="column1" type="VARCHAR(100)"/>
    </addColumn>
    <addColumn tableName="table_name">
        <column name="column2" type="INT"/>
    </addColumn>

    <rollback>
        <dropColumn tableName="table_name" columnName="column2"/>
        <dropColumn tableName="table_name" columnName="column1"/>
    </rollback>
</changeSet>
```

**5. Rollback Order Matters**
```xml
<!-- Rollback operations execute in REVERSE order within <rollback> tag -->
<rollback>
    <sql>DROP TABLE child_table;</sql>      <!-- Executes FIRST -->
    <sql>DROP TABLE parent_table;</sql>     <!-- Executes SECOND -->
</rollback>
```

### Troubleshooting Rollback

**Problem: Rollback fails due to foreign key**
```
ERROR: cannot drop column because other objects depend on it
```

**Solution:** Drop dependent objects first
```xml
<rollback>
    <sql>ALTER TABLE child_table DROP CONSTRAINT fk_child_parent;</sql>
    <sql>ALTER TABLE parent_table DROP COLUMN parent_id;</sql>
</rollback>
```

**Problem: Cannot rollback data migration**
```
Cannot restore original NULL values
```

**Solution:** Document as non-reversible, require backup
```xml
<rollback>
    <comment>
        This migration cannot be automatically rolled back.
        Restore data from backup created before migration.
        Backup file: backup_YYYYMMDD_HHMMSS.sql
    </comment>
</rollback>
```

**Problem: Rollback SQL not found**
```
ERROR: No rollback SQL found for changeset
```

**Solution:** Add explicit rollback
```xml
<changeSet id="0012-complex-change" author="developer-name">
    <sql>/* Complex SQL */</sql>

    <!-- Add this -->
    <rollback>
        <sql>/* Reverse complex SQL */</sql>
    </rollback>
</changeSet>
```

---

## References

- **Migration Status:** `backend/docs/DATABASE_MIGRATION_STATUS.md`
- **Entity-Migration Guide:** `backend/docs/ENTITY_MIGRATION_GUIDE.md`
- **Migration Plan:** `DATABASE_ARCHITECTURE_MIGRATION_PLAN.md`
- **CLAUDE.md:** Database Architecture section
- **Liquibase Docs:** https://docs.liquibase.com/

---

*For questions or issues, see the troubleshooting section or consult the database migration team.*
