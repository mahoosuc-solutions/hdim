# Entity-Migration Synchronization Guide

## Overview

This guide establishes standards and best practices for maintaining synchronization between JPA entity definitions and Liquibase database migrations across all HDIM microservices.

**Problem Solved**: The RefreshToken authentication issue (missing `tokenHash` and `revoked` columns) demonstrated that entity-migration drift can cause production issues. This guide prevents future incidents.

---

## Critical Principle

> **Every JPA `@Entity` must have a corresponding Liquibase migration that creates its table and columns.**

Any mismatch between entity annotations and database schema will be caught by automated validation tests and CI/CD checks.

---

## Quick Start: Creating a New Entity with Migration

### Step 1: Create the JPA Entity

```java
package com.healthdata.service.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "my_entities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Index(name = "idx_my_entities_tenant")
    @Column(name = "tenant_id")
    private String tenantIdForIndex;  // Mark fields needing indexes

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
```

### Step 2: Create Corresponding Liquibase Migration

**File**: `src/main/resources/db/changelog/NNNN-create-my-entities-table.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.23.xsd">

    <changeSet id="NNNN-create-my-entities-table" author="hdim-platform">
        <comment>
            Create my_entities table
            Entity: com.healthdata.service.domain.model.MyEntity
        </comment>

        <createTable tableName="my_entities">
            <column name="id" type="UUID">
                <constraints primaryKey="true" nullable="false"/>
            </column>

            <column name="tenant_id" type="VARCHAR(64)">
                <constraints nullable="false"/>
            </column>

            <column name="name" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>

            <column name="description" type="TEXT"/>

            <column name="is_active" type="BOOLEAN">
                <constraints nullable="false"/>
            </column>

            <column name="created_at" type="TIMESTAMP WITH TIME ZONE">
                <constraints nullable="false"/>
            </column>

            <column name="updated_at" type="TIMESTAMP WITH TIME ZONE"/>
        </createTable>

        <!-- Required Indexes -->
        <createIndex indexName="idx_my_entities_tenant" tableName="my_entities">
            <column name="tenant_id"/>
        </createIndex>
    </changeSet>

</databaseChangeLog>
```

### Step 3: Register in Master Changelog

**File**: `src/main/resources/db/changelog/db.changelog-master.xml`

Add at the end (before closing tag):

```xml
    <include file="db/changelog/NNNN-create-my-entities-table.xml"/>
```

### Step 4: Run Validation Test

The validation tests will automatically verify that your entity matches the migration:

```bash
./gradlew :modules:services:my-service:test --tests "*EntityMigrationValidationTest"
```

✅ **Test passes** = Entity and migration are in sync
❌ **Test fails** = See error message for exact mismatch

---

## Validation Test Framework

### Using the Validation Framework

Every critical service has an `EntityMigrationValidationTest` that:
- Introspects all `@Entity` classes using JPA Metamodel API
- Queries the actual database schema using JDBC
- Compares entity definitions with database reality
- Reports mismatches with severity (ERROR, WARNING, INFO)

### Services with Validation Tests

- ✅ authentication module
- ✅ patient-service
- ✅ quality-measure-service
- ✅ care-gap-service
- ✅ fhir-service
- ✅ sales-automation-service

### Running Validation Tests

Run for specific service:
```bash
./gradlew :modules:services:patient-service:test \
  --tests "*EntityMigrationValidationTest"
```

Run for all services:
```bash
./gradlew test --tests "*EntityMigrationValidationTest"
```

---

## Common Scenarios

### Scenario 1: Adding a Column to Existing Entity

**1. Update Entity**:
```java
@Entity
public class Patient {
    // ... existing fields ...

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
}
```

**2. Create NEW Migration** (never modify existing ones):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="...">
    <changeSet id="0006-add-phone-to-patients" author="hdim-platform">
        <comment>Add phoneNumber field to Patient entity</comment>
        <addColumn tableName="patients">
            <column name="phone_number" type="VARCHAR(20)"/>
        </addColumn>
    </changeSet>
</databaseChangeLog>
```

**3. Update Master Changelog**:
```xml
<include file="db/changelog/0006-add-phone-to-patients.xml"/>
```

**4. Test**:
```bash
./gradlew test --tests "*EntityMigrationValidationTest"
```

### Scenario 2: Adding Foreign Key

**1. Update Entity**:
```java
@Entity
public class PatientEncounter {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false,
                foreignKeyName = "fk_encounter_patient")
    private Patient patient;
}
```

**2. Create Migration**:
```xml
<changeSet id="NNNN-add-encounter-patient-fk" author="hdim-platform">
    <comment>Add foreign key from encounters to patients</comment>

    <addColumn tableName="patient_encounters">
        <column name="patient_id" type="UUID">
            <constraints nullable="false"
                        foreignKeyName="fk_encounter_patient"
                        references="patients(id)"/>
        </column>
    </addColumn>
</changeSet>
```

### Scenario 3: Creating Index for Performance

**1. Update Entity** (document the index):
```java
@Entity
@Table(name = "care_gaps", indexes = {
    @Index(name = "idx_gaps_patient_measure",
           columnList = "patient_id,measure_id")
})
public class CareGap {
    // ...
}
```

**2. Create Migration**:
```xml
<changeSet id="NNNN-add-composite-index" author="hdim-platform">
    <comment>Add composite index for care gap lookups</comment>
    <createIndex indexName="idx_gaps_patient_measure"
                 tableName="care_gaps">
        <column name="patient_id"/>
        <column name="measure_id"/>
    </createIndex>
</changeSet>
```

---

## Column Type Mapping Reference

### Java → PostgreSQL Type Mapping

| Java Field Type | Entity Annotation | Liquibase Type | PostgreSQL Type | Notes |
|-----------------|------------------|----------------|-----------------|-------|
| `UUID` | `@Column` | `UUID` | `uuid` | HDIM standard for PKs |
| `String` (small) | `@Column(length=255)` | `VARCHAR(255)` | `varchar(255)` | Default for names |
| `String` (large) | `@Column(columnDefinition="TEXT")` | `TEXT` | `text` | For descriptions |
| `Boolean` | `@Column` | `BOOLEAN` | `boolean` | Use NOT NULL |
| `Integer` | `@Column` | `INT` | `integer` | Use for counts |
| `Long` | `@Column` | `BIGINT` | `bigint` | Use for large numbers |
| `BigDecimal` | `@Column` | `DECIMAL(p,s)` | `numeric(p,s)` | For money/percentages |
| `Instant` | `@Column` | `TIMESTAMP WITH TIME ZONE` | `timestamp with time zone` | Use UTC always |
| `LocalDate` | `@Column` | `DATE` | `date` | For dates without time |
| `byte[]` | `@Column(columnDefinition="BYTEA")` | `BLOB` | `bytea` | For binary data |
| `Set<T>` | `@OneToMany` | *(separate table)* | *(via FK)* | Use @ElementCollection or separate table |
| `Enum` | `@Enumerated(STRING)` | `VARCHAR(50)` | `varchar(50)` | Store enum name, not ordinal |

### Examples

**String Name Field** (most common):
```java
@Column(name = "first_name", length = 100, nullable = false)
private String firstName;
```
```xml
<column name="first_name" type="VARCHAR(100)">
    <constraints nullable="false"/>
</column>
```

**Timestamp Field** (use Instant for UTC):
```java
@Column(name = "created_at", nullable = false, updatable = false)
private Instant createdAt;

@PrePersist
protected void onCreate() {
    createdAt = Instant.now();
}
```
```xml
<column name="created_at" type="TIMESTAMP WITH TIME ZONE">
    <constraints nullable="false"/>
</column>
```

**Enum Field**:
```java
@Column(name = "status", nullable = false, length = 50)
@Enumerated(EnumType.STRING)
private PatientStatus status;

public enum PatientStatus { ACTIVE, INACTIVE, DECEASED }
```
```xml
<column name="status" type="VARCHAR(50)">
    <constraints nullable="false"/>
</column>
```

---

## Entity Annotation Checklist

When creating or modifying entities, ensure:

- [ ] **Table Name**: Use `@Table(name = "snake_case_table")`
- [ ] **Column Names**: Use explicit `@Column(name = "snake_case_column")`
- [ ] **Primary Key**: Use `@Id` + `@GeneratedValue(strategy = GenerationType.UUID)`
- [ ] **Tenant ID**: Every entity has `@Column(name = "tenant_id")` for multi-tenancy
- [ ] **Nullable**: Mark with `@Column(nullable = false)` where required
- [ ] **Length**: Specify for string columns: `@Column(length = 255)`
- [ ] **Default Values**: Use `columnDefinition` if needed: `@Column(columnDefinition = "BOOLEAN DEFAULT false")`
- [ ] **Indexes**: Mark with `@Index` for frequently queried fields
- [ ] **Foreign Keys**: Use `@JoinColumn(foreignKeyName = "fk_...")`
- [ ] **Timestamps**: Use `Instant` type with `@PrePersist/@PreUpdate` for created_at/updated_at
- [ ] **No Hardcoded Values**: Use database defaults, not Java defaults
- [ ] **Avoid**: `@GeneratedValue(strategy = GenerationType.IDENTITY)` - use UUID instead
- [ ] **Avoid**: `BigInteger` - use `UUID` or `Long` instead
- [ ] **Avoid**: `Date` - use `Instant` or `LocalDate` instead

---

## Migration File Naming Convention

### Format
```
NNNN-descriptive-name.xml
```

### Rules
- **NNNN**: Sequential 4-digit number (0001, 0002, 0003, ...)
- **descriptive-name**: Short description of change (lowercase, hyphen-separated)
- Examples:
  - `0001-create-patients-table.xml`
  - `0002-create-patient-encounters-table.xml`
  - `0003-add-phone-to-patients.xml`
  - `0004-create-composite-indexes.xml`

### Numbering Rules

**CRITICAL**: Numbers are sequential and immutable
- Never reuse a number
- Never skip numbers
- Never modify existing migration files
- Numbers must match the order in db.changelog-master.xml

**What to do when mistakes happen**:
1. Create a NEW migration to fix the issue
2. Document the fix in the new migration's comment
3. Example: If 0005 was created incorrectly, create 0006-fix-previous-issue.xml

---

## Liquibase Configuration Reference

### application-prod.yml
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # CRITICAL: Never use 'update' in production
    show-sql: false

  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml
    default-schema: public
```

### application-docker.yml
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Use validate in Docker too

  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml
```

### application-dev.yml (Local Development)
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Even in dev, use Liquibase
      show-sql: true  # Enable SQL logging for debugging
      format_sql: true

  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml
```

### application-test.yml (Unit Tests)
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop  # Tests create schema from scratch

  liquibase:
    enabled: false  # Tests don't use Liquibase
```

---

## Hibernate Configuration Policy

### DDL Auto Setting Rules

| Environment | Setting | Reason | Risk |
|------------|---------|--------|------|
| Production | `validate` | Schema managed by Liquibase only | 🔴 CRITICAL: Use only `validate` |
| Docker/Staging | `validate` | Schema managed by Liquibase only | 🔴 CRITICAL: Use only `validate` |
| Development | `validate` | Keep in sync with Docker/Prod | 🟡 Using `update` loses discipline |
| Unit Tests | `create-drop` | Tests need fresh schema each run | ✅ OK: Tests don't use migrations |

### What Hibernate DDL Auto Does

- **`create-drop`**: Creates schema at startup, drops at shutdown (tests only)
- **`create`**: Creates schema at startup (DANGEROUS in production)
- **`update`**: Auto-modifies schema to match entities (DANGEROUS everywhere except dev)
- **`validate`**: Validates schema matches entities, fails if mismatch (REQUIRED in production)
- **`none`**: No automatic schema management (manual only)

### Why `validate` is Required

The `validate` setting ensures:
1. ✅ Entity-migration synchronization is enforced
2. ✅ Unintended schema changes are caught
3. ✅ Database migrations are the source of truth
4. ✅ Code reviews catch schema changes in PRs
5. ✅ No surprise database modifications

---

## Troubleshooting Validation Test Failures

### Error: "Entity-migration validation failed"

**Symptom**: Test fails with message like:
```
Entity-migration validation failed:
  ERROR: Table missing: my_entities
  ERROR: Column missing: my_entities.phone_number
```

**Solution**:
1. Check entity definition: `@Table(name = "my_entities")`
2. Check migration file exists in `db/changelog/`
3. Check migration is included in `db.changelog-master.xml`
4. Check column name matches: `@Column(name = "phone_number")`
5. Run test again: `./gradlew test --tests "*EntityMigrationValidationTest"`

### Error: "Column type mismatch"

**Symptom**: Test fails with message like:
```
ERROR: Column type mismatch: employees.salary
  Entity: java.math.BigDecimal
  Database: integer
```

**Solution**:
1. Check entity column type: `@Column(...)` on field
2. Check migration XML: `<column type="DECIMAL(10,2)">`
3. Ensure types match from [Column Type Mapping](#column-type-mapping-reference)
4. Create migration to fix type: `NNNN-fix-salary-column-type.xml`
5. Run test again

### Error: "Liquibase execution failed"

**Symptom**: Application won't start:
```
ERROR: Liquibase execution failed
  Reason: Duplicate changeset ID: 0005-create-users
```

**Solution**:
1. Check for duplicate migration numbers in `db/changelog/`
2. Each changeset ID must be unique
3. Rename duplicates to next sequential number
4. Update `db.changelog-master.xml` includes
5. See [Phase 3b Note](#phase-3b-known-issues) for quality-measure-service duplicate fix

---

## Known Issues & Workarounds

### Phase 3b: Quality Measure Service Duplicate Migrations

**Issue**: quality-measure-service has 8+ migration files with duplicate numbers (0001, 0002, 0003, 0008, etc.)

**Status**: Identified but deferred due to complexity of sequencing dependencies

**Workaround**:
- Validation tests in place will catch this
- Team should plan focused sprint to:
  1. List all files and their table dependencies
  2. Sequence them correctly
  3. Renumber sequentially (0001-0030+)
  4. Update db.changelog-master.xml
  5. Test validation passes

**Timeline**: Plan for Q1 2026 sprint

### Services Pending Liquibase Enablement

**Issue**: Several services enabled Liquibase in config but have no migration files:
- migration-workflow-service
- sdoh-service
- gateway-service
- agent-builder-service

**Status**: Deferred - Requires entity analysis and baseline migration creation

**Workaround**:
1. Option A (Recommended): Create initial baseline migration using:
   ```bash
   ./gradlew :modules:services:service-name:build
   # Manually review entities and create 0001-baseline.xml
   ```

2. Option B: Disable Liquibase temporarily until migrations created:
   ```yaml
   spring:
     liquibase:
       enabled: false
   ```

3. Option C: Use Liquibase generateChangeLog:
   ```bash
   # Requires connected database
   liquibase --changeLogFile=db/changelog/db.changelog-baseline.xml generateChangeLog
   ```

---

## Best Practices

### DO ✅

- ✅ Create migrations IMMEDIATELY when adding entities
- ✅ Use sequential numbering (no gaps, no reuse)
- ✅ Keep entity annotations and migrations in sync
- ✅ Document table/column purpose in migration comments
- ✅ Reference entity class path in migration comments
- ✅ Create indexes for frequently queried fields
- ✅ Use multi-tenant filtering (tenant_id in every table)
- ✅ Use `Instant` for timestamps (UTC)
- ✅ Use `UUID` for primary keys
- ✅ Mark required fields with `nullable = false`
- ✅ Specify column lengths: `length = 255`
- ✅ Use snake_case for table/column names
- ✅ Run validation tests after changes
- ✅ Review entity-migration pairs in code review

### DON'T ❌

- ❌ Never use `ddl-auto: update` in production
- ❌ Never use `ddl-auto: create` in any environment
- ❌ Never modify existing migration files
- ❌ Never reuse migration numbers
- ❌ Never skip migration numbers
- ❌ Never add columns without corresponding migrations
- ❌ Never use `GenerationType.IDENTITY`
- ❌ Never use `java.util.Date` (use `Instant`)
- ❌ Never use `BigInteger` (use `UUID` or `Long`)
- ❌ Never hardcode default values in Java (use database defaults)
- ❌ Never bypass Liquibase with raw SQL outside migrations
- ❌ Never assume schema validation happens automatically

---

## Team Responsibilities

### Developers (Creating/Modifying Entities)

1. Create entity class with proper annotations
2. Create Liquibase migration file immediately
3. Add migration include to db.changelog-master.xml
4. Run validation tests: `./gradlew test --tests "*EntityMigrationValidationTest"`
5. Commit both entity and migration together
6. Reference entity and migration in commit message

### Code Reviewers

1. Verify entity class exists for every migration
2. Verify migration exists for every entity change
3. Verify migration numbering is sequential
4. Verify types match [Column Type Mapping](#column-type-mapping-reference)
5. Verify Liquibase enable in all application*.yml files
6. Check that ddl-auto is NOT 'update' in production configs

### DevOps / Release Management

1. Run full validation test suite before production deployment
2. Verify migrations execute successfully in staging first
3. Monitor Liquibase execution in production logs
4. Keep database backups before applying new migrations
5. Document any manual fixes needed

---

## References

- **HAPI FHIR**: [Column definitions guide](https://hapifhir.io/)
- **Liquibase Documentation**: [Change Types](https://docs.liquibase.com/change-types/home.html)
- **PostgreSQL Types**: [Data Types Reference](https://www.postgresql.org/docs/15/datatype.html)
- **Hibernate JPA**: [Entity Mapping Guide](https://hibernate.org/orm/documentation/)
- **HDIM CLAUDE.md**: [Project Guidelines](../CLAUDE.md)

---

## Getting Help

- **Validation Test Fails**: Check changelog includes in db.changelog-master.xml
- **Type Mismatch**: Use [Column Type Mapping](#column-type-mapping-reference) reference
- **Missing Migrations**: Review [Quick Start](#quick-start-creating-a-new-entity-with-migration)
- **Duplicate Numbers**: See [Phase 3b Known Issues](#phase-3b-known-issues)

---

*Last Updated: January 1, 2026*
*Version: 1.0*
