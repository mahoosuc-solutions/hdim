# PostgreSQL + Liquibase - Skill Guide

> **This is a comprehensive guide for database schema management in HDIM.**
> **Liquibase is the ONLY tool for schema migrations; Hibernate's ddl-auto is NEVER used in production.**

---

## Overview

### What is This Skill?

PostgreSQL is HDIM's relational database (version 16) storing all persistent data: patients, clinical observations, quality measures, care gaps, audit logs. Liquibase is the schema migration tool that evolves the database schema in a controlled, reversible, auditable way. Together, PostgreSQL + Liquibase enable:

1. **Schema Versioning:** Every schema change is tracked in version control with complete audit trail
2. **Safe Rollbacks:** Every migration includes rollback logic to undo changes if needed
3. **Multi-Environment Consistency:** Dev, test, staging, prod all apply migrations in same order
4. **Data Integrity:** Liquibase validates schema matches before application startup (prevents mismatches)
5. **Team Collaboration:** Developers merge migrations in version control without conflicts

**Example:** When creating new Appointment entity, developer writes JPA entity + Liquibase migration that creates table. Liquibase tracks which migrations have been applied (databasechangelog table). Application startup validates entity matches database schema.

### Why is This Important for HDIM?

Healthcare systems cannot tolerate data loss or schema inconsistencies. A single misconfigured migration can:

- Cause production downtime (schema validation failures at startup)
- Lose patient data (DROP TABLE without backup)
- Break multi-tenant isolation (missing tenant_id column)
- Violate HIPAA audit requirements (unauditable schema changes)
- Block deployments (stuck migrations that can't be rolled back)

HDIM currently manages **29 PostgreSQL databases** with **199+ Liquibase changesets**. Without disciplined Liquibase practices, managing 29 schemas becomes operationally impossible.

### Business Impact

- **Operational Safety:** Schema changes validated before production (no surprises at 3 AM)
- **Compliance Audit Trail:** HIPAA requires auditable schema changes (Liquibase changelog = audit trail)
- **Developer Velocity:** Developers can work on different migrations in parallel (Liquibase merges them)
- **Zero-Downtime Deployments:** Liquibase enables blue-green deployments with schema versioning

### Key Services Using This Skill

All 51 HDIM services use PostgreSQL + Liquibase:

**Event Services:**
- patient-event-service (8084) - Manages patient_db schema (199+ changesets)
- quality-measure-event-service (8087) - Manages quality_measure_db schema
- care-gap-event-service (8086) - Manages care_gap_db schema
- clinical-workflow-event-service - Manages clinical_workflow_db schema

**Supporting Services:**
- fhir-service (8085) - Manages fhir_db schema
- gateway-service (8001) - Manages gateway_db schema
- analytics-service - Manages analytics_db schema
- audit-service - Manages audit_db schema

**Total:** 29 independent PostgreSQL databases with independent Liquibase migration chains

### Estimated Learning Time

1.5-2 weeks (hands-on practice with migrations required)

---

## Key Concepts

### Concept 1: Migration-Entity Synchronization (CRITICAL)

**Definition:** Every JPA entity must correspond to a Liquibase migration that creates/modifies the table. Hibernate's `ddl-auto: validate` mode checks at startup that entity definitions match actual database schema. If they don't match, application fails to start (fail-fast pattern).

**Why it matters:** Schema drift (entity definition != database schema) is a silent killer. Without validation, code works in dev (where Hibernate auto-creates tables) but fails in production (where tables already exist). HDIM enforces entity-migration synchronization to catch this at test time.

**Real-world example:**
```java
// Entity definition
@Entity
@Table(name = "patients")
public class Patient {
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
}

// Liquibase migration (must match entity)
<createTable tableName="patients">
    <column name="date_of_birth" type="date"/>
</createTable>

// Hibernate validation at startup:
// ✅ Column exists in database, type matches entity definition
// Application starts successfully

// ❌ If migration forgot date_of_birth column:
// Application fails: "Missing column date_of_birth in patients table"
```

### Concept 2: Changesets and Change Logs

**Definition:** Liquibase migrations are organized in XML (or YAML/SQL) files in `db/changelog/` directory. Each file contains one or more `<changeSet>` elements describing discrete schema changes. The master file `db.changelog-master.xml` includes all changesets in order.

**Why it matters:** Changesets are atomic units that can be tracked, rolled back, and validated. The master file is the source of truth for schema version history.

**Real-world example:**
```xml
<!-- db/changelog/0001-create-patients-table.xml -->
<databaseChangeLog>
    <changeSet id="0001" author="hdim-team">
        <createTable tableName="patients">
            <column name="id" type="uuid">
                <constraints primaryKey="true"/>
            </column>
            <column name="tenant_id" type="varchar(255)">
                <constraints nullable="false"/>
            </column>
            <column name="mrn" type="varchar(255)"/>
        </createTable>
    </changeSet>
</databaseChangeLog>

<!-- db/changelog/db.changelog-master.xml -->
<include file="db/changelog/0001-create-patients-table.xml"/>
<include file="db/changelog/0002-add-patient-observations-table.xml"/>
<include file="db/changelog/0003-add-index-on-tenant-mrn.xml"/>
<!-- Changesets applied in order -->
```

### Concept 3: Rollback Directives

**Definition:** Every changeset must include `<rollback>` section describing how to undo the change. Rollback logic is NOT automatically generated—it must be explicitly written. HDIM requires 100% rollback coverage (199/199 changesets have rollback logic).

**Why it matters:** If a migration causes issues in production, you need to quickly roll back without manual database cleanup. Incomplete rollback logic means manual intervention required (dangerous and slow).

**Real-world example:**
```xml
<!-- ✅ CORRECT: Includes rollback -->
<changeSet id="0001-create-patients-table" author="hdim">
    <createTable tableName="patients">
        <column name="id" type="uuid">
            <constraints primaryKey="true"/>
        </column>
    </createTable>

    <!-- Rollback: drops the table -->
    <rollback>
        <dropTable tableName="patients"/>
    </rollback>
</changeSet>

<!-- ❌ WRONG: No rollback (if this fails, manual cleanup required) -->
<changeSet id="0002-add-column" author="hdim">
    <addColumn tableName="patients">
        <column name="first_name" type="varchar(255)"/>
    </addColumn>
    <!-- Missing <rollback> section -->
</changeSet>
```

### Concept 4: Context-Aware Migrations

**Definition:** Liquibase supports conditional migrations using `<preconditions>` (e.g., "only run if column doesn't exist") and contexts (e.g., "only run in production"). This prevents duplicate migrations and allows environment-specific changes.

**Why it matters:** Developers might create the same column independently. Without preconditions, both migrations try to create the column; second one fails. Preconditions make migrations idempotent (safe to run multiple times).

**Real-world example:**
```xml
<!-- ✅ CORRECT: Precondition prevents duplicate creation -->
<changeSet id="0004-add-email-to-patients" author="hdim">
    <preConditions onFail="MARK_RAN">
        <not>
            <columnExists tableName="patients" columnName="email"/>
        </not>
    </preConditions>

    <addColumn tableName="patients">
        <column name="email" type="varchar(255)"/>
    </addColumn>
</changeSet>

<!-- ❌ WRONG: No precondition, if column exists migration fails -->
<changeSet id="0005-add-email-to-patients-v2" author="hdim">
    <addColumn tableName="patients">
        <column name="email" type="varchar(255)"/>
    </addColumn>
</changeSet>
```

### Concept 5: Liquibase Tracking Table

**Definition:** Liquibase maintains a `databasechangelog` table storing which changesets have been applied. When application starts, Liquibase checks: "has changeset 0001 been applied?" If not, it runs it. This table is the source of truth for schema version history.

**Why it matters:** Tracking table prevents re-running changesets and enables auditing schema changes (who applied what when).

**Real-world example:**
```sql
-- Liquibase tracking table
SELECT * FROM databasechangelog;

-- Output:
-- id      | author    | filename                          | dateexecuted        | orderexecuted | exectype | md5sum | version | description
-- 0001    | hdim-team | db/changelog/0001-*.xml           | 2024-01-20 10:00:00 | 1             | EXECUTED | ...    | 4.0     | createTable
-- 0002    | hdim-team | db/changelog/0002-*.xml           | 2024-01-20 10:05:00 | 2             | EXECUTED | ...    | 4.0     | createIndex
```

---

## Architecture Pattern

### How It Works

HDIM uses Liquibase to manage PostgreSQL schema across 29 independent databases:

1. **Development:** Developer adds JPA entity + writes Liquibase migration
2. **Commit:** Migration XML committed to Git alongside entity code
3. **Test:** Entity-migration validation test runs (`EntityMigrationValidationTest`):
   - Starts fresh test database
   - Runs Liquibase migrations (actual schema creation)
   - Sets Hibernate to `validate` mode
   - Compares entity definitions to actual schema
   - Fails if mismatch found (entity != database schema)
4. **CI/CD:** GitHub Actions validates all migrations compile
5. **Production:** Liquibase applies pending migrations at application startup
6. **Rollback:** If migration fails, rollback logic reverts change

### Diagram

```
Developer commits entity + migration
↓
├─ JPA Entity: @Table(name="patients"), @Column(name="first_name")
└─ Liquibase: <createTable><column name="first_name"/>
↓
Local Test: EntityMigrationValidationTest
├─ Start test database
├─ Run Liquibase migrations
├─ Enable Hibernate validate mode
├─ Compare entity to schema
└─ ✅ PASS: Entity matches database
↓
Git commit + push
↓
GitHub Actions CI
├─ Run same validation tests
├─ Check all migrations have rollback
└─ ✅ All checks pass
↓
Production Deployment
├─ Application starts
├─ Liquibase checks: "Applied migrations: 0001-0198, Pending: 0199"
├─ Runs pending migration 0199
├─ Updates databasechangelog table
├─ Hibernate validate mode: ✅ Matches
└─ Application ready to serve
↓
If production migration fails:
├─ Rollback runs migration.rollback()
├─ Schema reverted to previous state
└─ Application can start with known-good schema
```

### Design Decisions

**Decision 1: Why Liquibase instead of Hibernate's ddl-auto?**
- **Trade-off:** Liquibase requires manual migration writing (work) but provides auditability and rollback. Hibernate's ddl-auto is convenient (automatic) but provides no auditability or rollback (risk).
- **Rationale:** HDIM is healthcare system (HIPAA-regulated). Must have complete audit trail of schema changes. Liquibase stores audit trail in databasechangelog table. Hibernates doesn't.
- **Alternative:** Hibernate ddl-auto (not acceptable for regulated systems).

**Decision 2: Why XML migrations instead of pure SQL?**
- **Trade-off:** XML is verbose but database-agnostic. Pure SQL is concise but tightly coupled to PostgreSQL dialect (doesn't work on MySQL, Oracle, etc.).
- **Rationale:** HDIM uses PostgreSQL but Liquibase XML enables future migration to different database if needed. XML is readable for auditing.
- **Alternative:** Pure SQL (faster to write, harder to audit, harder to migrate databases).

**Decision 3: Why entity-migration validation at test time?**
- **Trade-off:** Validation test adds ~30 seconds to test suite but catches schema drift before production. Without it, bugs slip to production.
- **Rationale:** Schema mismatches are silent (work in dev, fail in prod). Early validation catches bugs at test time (fail-fast pattern).
- **Alternative:** Skip validation, catch bugs in production (unacceptable).

### Trade-offs

| Aspect | Pro | Con |
|--------|-----|-----|
| **Liquibase vs. Hibernate** | Audit trail, rollback, safe | Manual effort, more complex |
| **XML vs. SQL** | Database-agnostic, readable | Verbose, learning curve |
| **Validation at Test Time** | Catches drift before production | Adds test execution time (~30s) |
| **Rollback Coverage** | 100% rollback = fast recovery | Requires discipline, more code |
| **Preconditions** | Idempotent migrations (safe) | Added complexity |

---

## Implementation Guide

### Step-by-Step

#### Step 1: Create Entity with JPA Annotations

Start with JPA entity using proper annotations:

```java
@Entity
@Table(name = "patients")
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "mrn", length = 255, unique = true)
    private String mrn;

    @Column(name = "first_name", length = 255)
    private String firstName;

    @Column(name = "last_name", length = 255)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
```

**Key JPA annotations:**
- `@Table(name="patients")` - Maps to database table
- `@Column(name="first_name")` - Maps to database column
- `@Column(nullable = false)` - NOT NULL constraint
- `@Column(unique = true)` - UNIQUE constraint
- `@Column(length = 255)` - VARCHAR(255) size
- `@GeneratedValue(strategy = GenerationType.UUID)` - UUID generation

#### Step 2: Create Liquibase Migration File

Create numbered migration file in `db/changelog/` directory:

```xml
<!-- File: backend/modules/services/patient-event-service/src/main/resources/db/changelog/0100-create-patients-table.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.1.xsd">

    <changeSet id="0100-create-patients-table" author="hdim-team">
        <comment>Create patients table for storing patient demographic data</comment>

        <createTable tableName="patients">
            <!-- Primary key -->
            <column name="id" type="uuid">
                <constraints primaryKey="true" primaryKeyName="pk_patients"/>
            </column>

            <!-- Tenant scoping -->
            <column name="tenant_id" type="varchar(255)">
                <constraints nullable="false" foreignKeyName="fk_patients_tenant"
                             references="tenants(tenant_code)"/>
            </column>

            <!-- Medical record number -->
            <column name="mrn" type="varchar(255)">
                <constraints unique="true" uniqueConstraintName="uk_patients_mrn"/>
            </column>

            <!-- Name fields -->
            <column name="first_name" type="varchar(255)"/>
            <column name="last_name" type="varchar(255)"/>

            <!-- Date of birth -->
            <column name="date_of_birth" type="date"/>

            <!-- Audit timestamp -->
            <column name="created_at" type="timestamp">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <!-- Index for common queries -->
        <createIndex tableName="patients" indexName="idx_patients_tenant_id">
            <column name="tenant_id"/>
        </createIndex>

        <!-- Rollback: drop table -->
        <rollback>
            <dropIndex tableName="patients" indexName="idx_patients_tenant_id"/>
            <dropTable tableName="patients"/>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

**Naming convention:** `NNNN-description-of-change.xml` where NNNN is sequential number (0001, 0002, etc.)

**Key elements:**
- `<changeSet id="..." author="...">` - Unique identifier and author
- `<createTable>` - Create table with columns
- `<column name="..." type="...">` - Column with type
- `<constraints>` - NULL, UNIQUE, PRIMARY KEY, FOREIGN KEY
- `<createIndex>` - Performance optimization
- `<rollback>` - Undo logic (REQUIRED)

#### Step 3: Add Migration to Master Changelog

Include migration in master changelog file:

```xml
<!-- File: backend/modules/services/patient-event-service/src/main/resources/db/changelog/db.changelog-master.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.1.xsd">

    <!-- Tenant management (foundational) -->
    <include file="db/changelog/0001-create-tenants-table.xml"/>

    <!-- Patient data (domain entities) -->
    <include file="db/changelog/0100-create-patients-table.xml"/>
    <include file="db/changelog/0101-create-patient-observations-table.xml"/>

    <!-- Audit tracking (compliance) -->
    <include file="db/changelog/9999-create-audit-events-table.xml"/>
</databaseChangeLog>
```

**Convention:** Include files in order (1 → 100 → 101 → 9999)

#### Step 4: Configure Hibernate to Validate Mode

Set `hibernate.ddl-auto: validate` in Spring Boot configuration:

```yaml
# application.yml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # ✅ REQUIRED: Check entity matches database schema
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        generate_statistics: false

logging:
  level:
    org.hibernate: WARN
    org.liquibase: INFO
```

**Modes:**
- `validate` - ✅ REQUIRED: Check entity matches database; fail if mismatch
- `update` - ❌ NEVER: Auto-modify database (loses auditability)
- `create` - ❌ NEVER: Drop and recreate database (destructive)
- `create-drop` - ❌ NEVER: Drop database on shutdown (dangerous)
- `none` - ❌ NEVER: Disable validation (schema drift risk)

#### Step 5: Enable Liquibase in Spring Boot

Configure Liquibase to run on application startup:

```yaml
# application.yml
spring:
  liquibase:
    enabled: true  # Run migrations on startup
    change-log: classpath:/db/changelog/db.changelog-master.xml
    drop-first: false  # ✅ REQUIRED: Never drop database automatically
    default-schema: public

# Or in Java:
@Configuration
public class LiquibaseConfig {
    @Bean
    public SpringLiquibase liquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:/db/changelog/db.changelog-master.xml");
        liquibase.setDefaultSchema("public");
        return liquibase;
    }
}
```

#### Step 6: Write Entity-Migration Validation Test

Create test that validates entity matches database schema:

```java
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = PatientEventServiceApplication.class
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional
class EntityMigrationValidationTest {

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private DataSource dataSource;

    @Test
    void shouldHaveValidEntityMigrations() throws Exception {
        // This test validates that:
        // 1. Liquibase migrations ran successfully (actual schema created)
        // 2. Hibernate entity definitions match actual database schema
        // 3. No schema drift exists

        // Hibernate's SchemaValidator compares entity definitions to database
        var metadata = sessionFactory.getSessionFactoryOptions()
            .getMetadataRepository()
            .getMetadataBuilder(dataSource)
            .build();

        SchemaValidator validator = new SchemaValidator(
            metadata,
            sessionFactory.getSessionFactoryOptions()
        );

        // Validate throws if mismatch found
        assertDoesNotThrow(() -> {
            validator.validate();
            log.info("✅ All entities match database schema");
        });
    }

    @Test
    void patientTableShouldExist() throws Exception {
        // Verify specific table exists after Liquibase migration
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metadata = conn.getMetaData();
            ResultSet tables = metadata.getTables(null, "public", "PATIENTS", null);
            assertTrue(tables.next(), "Patient table should exist");
        }
    }

    @Test
    void patientTableShouldHaveTenantIdColumn() throws Exception {
        // Verify multi-tenant scoping column exists
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metadata = conn.getMetaData();
            ResultSet columns = metadata.getColumns(
                null, "public", "PATIENTS", "TENANT_ID");
            assertTrue(columns.next(), "tenant_id column should exist in patients table");
        }
    }
}
```

Run validation test:
```bash
./gradlew :modules:services:patient-event-service:test --tests "*EntityMigrationValidationTest"
```

#### Step 7: Add Index for Multi-Tenant Queries

Create separate changeset for indexes (performance optimization):

```xml
<!-- File: 0101-create-patients-indices.xml -->
<changeSet id="0101-create-patients-indices" author="hdim-team">
    <comment>Add indices for common query patterns</comment>

    <!-- Index on tenant_id (multi-tenant filtering) -->
    <createIndex tableName="patients" indexName="idx_patients_tenant_id">
        <column name="tenant_id"/>
    </createIndex>

    <!-- Composite index on tenant_id + mrn (find patient by MRN) -->
    <createIndex tableName="patients" indexName="idx_patients_tenant_mrn">
        <column name="tenant_id"/>
        <column name="mrn"/>
    </createIndex>

    <!-- Index on tenant_id + created_at (list recent patients) -->
    <createIndex tableName="patients" indexName="idx_patients_tenant_created">
        <column name="tenant_id"/>
        <column name="created_at"/>
    </createIndex>

    <rollback>
        <dropIndex tableName="patients" indexName="idx_patients_tenant_created"/>
        <dropIndex tableName="patients" indexName="idx_patients_tenant_mrn"/>
        <dropIndex tableName="patients" indexName="idx_patients_tenant_id"/>
    </rollback>
</changeSet>
```

---

## Real-World Examples from HDIM

### Example 1: Patient Event Service Schema

**Where:** `backend/modules/services/patient-event-service/src/main/resources/db/changelog/`

**What it does:** Stores patient demographic data, clinical observations, and patient event projections.

**Key files:**
- `0001-create-tenants-table.xml` - Foundational tenant management
- `0100-create-patients-table.xml` - Patient demographics (Patient entity)
- `0101-create-patient-observations-table.xml` - Clinical data
- `0102-create-patient-projections-table.xml` - Event-driven projections

**Relevant migration:**
```xml
<!-- 0100-create-patients-table.xml -->
<changeSet id="0100-create-patients-table" author="hdim-team">
    <createTable tableName="patients">
        <column name="id" type="uuid">
            <constraints primaryKey="true"/>
        </column>
        <column name="tenant_id" type="varchar(255)">
            <constraints nullable="false"/>
        </column>
        <column name="mrn" type="varchar(255)">
            <constraints unique="true"/>
        </column>
        <column name="first_name" type="varchar(255)"/>
        <column name="last_name" type="varchar(255)"/>
        <column name="date_of_birth" type="date"/>
        <column name="created_at" type="timestamp">
            <constraints nullable="false"/>
        </column>
    </createTable>
    <rollback>
        <dropTable tableName="patients"/>
    </rollback>
</changeSet>
```

**Why this example matters:** Shows foundational patient entity with tenant scoping and proper rollback. This pattern is replicated across all 51 services.

### Example 2: Quality Measure Service Schema with CQL

**Where:** `backend/modules/services/quality-measure-event-service/src/main/resources/db/changelog/`

**What it does:** Stores HEDIS measure definitions (CQL code) and evaluation results.

**Key migration:**
```xml
<!-- 0200-create-measure-definitions-table.xml -->
<changeSet id="0200-create-measure-definitions-table" author="hdim-team">
    <createTable tableName="measure_definitions">
        <column name="id" type="uuid">
            <constraints primaryKey="true"/>
        </column>
        <column name="tenant_id" type="varchar(255)">
            <constraints nullable="false"/>
        </column>
        <column name="measure_code" type="varchar(50)">
            <constraints nullable="false" unique="true"/>
        </column>
        <!-- CQL code stored as TEXT (large documents) -->
        <column name="cql_library" type="text">
            <constraints nullable="false"/>
        </column>
        <!-- NCQA version: "HEDIS 2024", "HEDIS 2025" -->
        <column name="ncqa_version" type="varchar(50)">
            <constraints nullable="false"/>
        </column>
        <column name="measurement_period_start" type="date"/>
        <column name="measurement_period_end" type="date"/>
        <column name="created_at" type="timestamp">
            <constraints nullable="false"/>
        </column>
    </createTable>
    <rollback>
        <dropTable tableName="measure_definitions"/>
    </rollback>
</changeSet>
```

**Why this example matters:** Shows how to store large text documents (CQL library) in PostgreSQL. TEXT type can store up to 4GB; ideal for CQL code.

### Example 3: Modifying Existing Table (Adding Column)

**Where:** `backend/modules/services/care-gap-event-service/src/main/resources/db/changelog/`

**What it does:** Adds new column to existing care gaps table without losing data.

**Key migration:**
```xml
<!-- 0301-add-clinical-notes-to-care-gaps.xml -->
<changeSet id="0301-add-clinical-notes-to-care-gaps" author="hdim-team">
    <comment>Add clinical notes field for documenting gap resolution steps</comment>

    <!-- Precondition: Only run if column doesn't exist -->
    <preConditions onFail="MARK_RAN">
        <not>
            <columnExists tableName="care_gaps" columnName="clinical_notes"/>
        </not>
    </preConditions>

    <!-- Add new column (allows NULL for existing rows) -->
    <addColumn tableName="care_gaps">
        <column name="clinical_notes" type="text"/>
    </addColumn>

    <!-- Rollback: remove column -->
    <rollback>
        <dropColumn tableName="care_gaps" columnName="clinical_notes"/>
    </rollback>
</changeSet>
```

**Why this example matters:** Shows safe schema evolution (adding columns). Precondition prevents duplicate additions; rollback removes column cleanly.

---

## Best Practices

### ✅ DO's

- ✅ **DO number changesets sequentially (0001, 0002, 0003...)**
  - Why: Easy to understand migration order and spot gaps
  - Example: `0100-create-patients-table.xml`, `0101-add-index-patients.xml`

- ✅ **DO include <rollback> section in EVERY changeset**
  - Why: 100% rollback coverage enables fast recovery if migration fails
  - Example: Every CREATE has DROP; every ADD has DROP/MODIFY; every DELETE has INSERT

- ✅ **DO use preconditions to make migrations idempotent**
  - Why: Safe to run migration twice without errors
  - Example: `<preConditions onFail="MARK_RAN"><not><columnExists.../></not></preConditions>`

- ✅ **DO add comments describing changeset purpose**
  - Why: Audit trail explains WHY schema changed, not just WHAT changed
  - Example: `<comment>Add clinical notes field for documenting gap resolution</comment>`

- ✅ **DO include tenant_id in EVERY table**
  - Why: Multi-tenant isolation must be enforced at database level
  - Example: Every `<createTable>` includes `<column name="tenant_id" type="varchar(255)"/>`

- ✅ **DO create indices for common query patterns**
  - Why: Tenant filtering queries (WHERE tenant_id = ?) must be fast
  - Example: `<createIndex ... ><column name="tenant_id"/></createIndex>`

- ✅ **DO keep migrations focused (one logical change per changeset)**
  - Why: If one part fails, you know exactly which part
  - Example: Separate changeset for table creation vs. index creation

- ✅ **DO run EntityMigrationValidationTest after writing entity + migration**
  - Why: Catch schema drift before committing to Git
  - Example: `./gradlew test --tests "*EntityMigrationValidationTest"`

- ✅ **DO set Hibernate to `validate` mode in all environments**
  - Why: Fail-fast if entity doesn't match database schema
  - Example: `spring.jpa.hibernate.ddl-auto: validate`

- ✅ **DO document schema changes in commit message**
  - Why: Team understands what changed and why
  - Example: "Add clinical_notes column to care_gaps table for HEDIS documentation requirements"

### ❌ DON'Ts

- ❌ **DON'T use Hibernate's ddl-auto: update or create**
  - Why: Loses auditability (no way to know what changed); destructive
  - Example: ❌ `spring.jpa.hibernate.ddl-auto: update`

- ❌ **DON'T create migrations without rollback sections**
  - Why: Can't recover from failed migrations; manual intervention required
  - Example: ❌ Changeset with no `<rollback>` element

- ❌ **DON'T modify existing migrations (always create new ones)**
  - Why: Existing migrations might already be applied in production
  - Example: ❌ Edit `0100-create-patients-table.xml` after it's deployed

- ❌ **DON'T skip tenant_id column in new tables**
  - Why: Multi-tenant isolation requires tenant_id on every table
  - Example: ❌ `<createTable tableName="observations"><column name="value"/></createTable>`

- ❌ **DON'T forget FOREIGN KEY constraints for tenant_id**
  - Why: Database-level referential integrity prevents orphaned records
  - Example: ❌ Missing `<constraints foreignKeyName="fk_*" references="tenants(id)"/>`

- ❌ **DON'T create bare tables without indices**
  - Why: Multi-tenant queries (WHERE tenant_id = ?) without index are slow
  - Example: ❌ Create patient table, forget `<createIndex tableName="patients" indexName="idx_tenant_id">`

- ❌ **DON'T reuse changeset IDs (even in different files)**
  - Why: Liquibase uses ID to track which migrations ran; duplicate IDs cause confusion
  - Example: ❌ Two changesets with `id="0001"` in different files

- ❌ **DON'T use Hibernate's @Entity with ddl-auto: none**
  - Why: Entity definition becomes source of truth, but database isn't updated (drift)
  - Example: ❌ Entity `@Column(name="email")` but database has no email column

- ❌ **DON'T forget to include migration in db.changelog-master.xml**
  - Why: Migration exists but never runs (dangling changeset)
  - Example: ❌ Create `0200-create-audit-table.xml` but forget to add `<include>` in master

- ❌ **DON'T skip EntityMigrationValidationTest**
  - Why: Schema drift bugs slip to production (fail silently in dev, crash in prod)
  - Example: ❌ Add entity field but forget Liquibase migration, test still passes (because Hibernate auto-creates in test DB)

---

## Real-World Schema Migration Scenarios

### Scenario 1: Adding New Column with Constraint

**Requirement:** Add `email` column to patients table (required, unique per tenant).

```xml
<changeSet id="0102-add-email-to-patients" author="hdim-team">
    <comment>Add email column for patient contact information</comment>

    <preConditions onFail="MARK_RAN">
        <not><columnExists tableName="patients" columnName="email"/></not>
    </preConditions>

    <addColumn tableName="patients">
        <column name="email" type="varchar(255)">
            <constraints nullable="false"/>  <!-- Required field -->
        </column>
    </addColumn>

    <!-- Add unique constraint (per tenant) -->
    <addUniqueConstraint
        tableName="patients"
        columnNames="tenant_id, email"
        constraintName="uk_patients_tenant_email"/>

    <!-- Add index for email lookups -->
    <createIndex tableName="patients" indexName="idx_patients_email">
        <column name="email"/>
    </createIndex>

    <!-- Rollback -->
    <rollback>
        <dropIndex tableName="patients" indexName="idx_patients_email"/>
        <dropUniqueConstraint
            tableName="patients"
            constraintName="uk_patients_tenant_email"/>
        <dropColumn tableName="patients" columnName="email"/>
    </rollback>
</changeSet>
```

**Entity update:**
```java
@Entity
@Table(name = "patients")
public class Patient {
    // ... existing fields ...

    @Column(name = "email", nullable = false, unique = true)
    private String email;
}
```

### Scenario 2: Creating JSONB Column for Flexible Data

**Requirement:** Store patient preferences as JSON (flexible schema).

```xml
<changeSet id="0103-add-preferences-to-patients" author="hdim-team">
    <comment>Add preferences JSONB column for flexible patient settings</comment>

    <preConditions onFail="MARK_RAN">
        <not><columnExists tableName="patients" columnName="preferences"/></not>
    </preConditions>

    <!-- PostgreSQL JSONB type for flexible schema -->
    <addColumn tableName="patients">
        <column name="preferences" type="jsonb" defaultValue="'{}'">
            <!-- Default empty JSON object -->
        </column>
    </addColumn>

    <!-- Rollback -->
    <rollback>
        <dropColumn tableName="patients" columnName="preferences"/>
    </rollback>
</changeSet>
```

**Entity update:**
```java
@Entity
@Table(name = "patients")
public class Patient {
    @Type(JsonType.class)  // Hibernate JSON type mapping
    @Column(name = "preferences", columnDefinition = "jsonb")
    private Map<String, Object> preferences = new HashMap<>();
}
```

### Scenario 3: Renaming Column Safely

**Requirement:** Rename `mrn` (Medical Record Number) to `external_id` (more generic).

```xml
<changeSet id="0104-rename-mrn-to-external-id" author="hdim-team">
    <comment>Rename mrn column to external_id for consistency across services</comment>

    <preConditions onFail="MARK_RAN">
        <columnExists tableName="patients" columnName="mrn"/>
        <not><columnExists tableName="patients" columnName="external_id"/></not>
    </preConditions>

    <!-- PostgreSQL: rename column -->
    <sql dbms="postgresql">
        ALTER TABLE patients RENAME COLUMN mrn TO external_id;
    </sql>

    <!-- Rollback -->
    <rollback>
        <sql dbms="postgresql">
            ALTER TABLE patients RENAME COLUMN external_id TO mrn;
        </sql>
    </rollback>
</changeSet>
```

**Entity update:**
```java
@Entity
@Table(name = "patients")
public class Patient {
    @Column(name = "external_id", unique = true)
    private String externalId;  // Renamed field
}
```

---

## Testing Strategies

### Unit Testing: Entity Validation

```java
@ExtendWith(MockitoExtension.class)
class PatientEntityTest {

    @Test
    void shouldMapEntityToDatabase() {
        // Verify entity annotations match expected database columns
        EntityDescriptor descriptor = new EntityDescriptor(Patient.class);

        assertThat(descriptor.getColumn("id"))
            .hasFieldOrPropertyWithValue("name", "id")
            .hasFieldOrPropertyWithValue("nullable", false);

        assertThat(descriptor.getColumn("tenant_id"))
            .hasFieldOrPropertyWithValue("name", "tenant_id")
            .hasFieldOrPropertyWithValue("nullable", false);
    }
}
```

### Integration Testing: Liquibase Execution

```java
@SpringBootTest
class LiquibaseIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void shouldApplyAllMigrationsSuccessfully() throws Exception {
        // Verify Liquibase applied all migrations
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metadata = conn.getMetaData();

            // Check databasechangelog table exists
            ResultSet tables = metadata.getTables(
                null, "public", "DATABASECHANGELOG", null);
            assertTrue(tables.next(), "databasechangelog table should exist");

            // Count applied migrations
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) as count FROM databasechangelog");
            rs.next();
            int migrationCount = rs.getInt("count");

            assertThat(migrationCount).isGreaterThan(100);  // 100+ migrations applied
        }
    }

    @Test
    void shouldHaveNoSchemaDrift() throws Exception {
        // Verify entity definitions match database schema
        // (This is what EntityMigrationValidationTest does)
    }
}
```

### Entity-Migration Validation Checklist

- [ ] Entity has `@Entity` and `@Table(name="tablename")` annotations
- [ ] Every `@Column` matches database column name and type
- [ ] `@Column(nullable = false)` matches database NOT NULL
- [ ] `@Column(unique = true)` matches database UNIQUE constraint
- [ ] Foreign keys have `@ManyToOne` or `@OneToMany` with `@JoinColumn`
- [ ] Liquibase migration creates table with all columns from entity
- [ ] Migration includes rollback that completely undoes change
- [ ] Preconditions prevent duplicate migrations (idempotent)
- [ ] Migration included in `db.changelog-master.xml`
- [ ] EntityMigrationValidationTest passes: `./gradlew test --tests "*EntityMigrationValidationTest"`

---

## Troubleshooting

### Common Issues

#### Issue 1: "Column Not Found" at Runtime

**Symptoms:**
- Application crashes: `ERROR: column "email" of relation "patients" does not exist`
- Entity has `@Column(name="email")` but database table doesn't
- Works in dev (Hibernate auto-creates), fails in prod (schema already exists)

**Root cause:** Entity added but Liquibase migration not written or not applied.

**Solution:**
```bash
# Step 1: Verify Liquibase migration exists
ls backend/modules/services/patient-event-service/src/main/resources/db/changelog/
# Look for migration file with the column

# Step 2: Verify migration is included in master
grep "email" backend/.../db/changelog/db.changelog-master.xml
# Should see <include file="...add-email-to-patients.xml"/>

# Step 3: Run EntityMigrationValidationTest
./gradlew test --tests "*EntityMigrationValidationTest"
# Should catch mismatch before production

# Step 4: If test passes but runtime fails:
# Migration applied to test DB but not to production DB
# Check production database:
docker exec -it hdim-postgres psql -U healthdata -d patient_db -c "\d patients"
# If column missing, manually apply migration or rollback application
```

**Prevention:** Always run EntityMigrationValidationTest before committing.

#### Issue 2: "Liquibase Lock Timeout"

**Symptoms:**
- Multiple services try to apply migrations simultaneously
- One acquires lock, others wait
- Timeout: `Timeout waiting for change log lock`

**Root cause:** Liquibase uses database lock to prevent concurrent migrations. If one service hangs, lock isn't released.

**Solution:**
```bash
# Step 1: Check lock table
docker exec -it hdim-postgres psql -U healthdata -d patient_db \
    -c "SELECT * FROM databasechangeloglock;"

# Output:
# id | locked | lockgranted | lockedby | lockuntil
# 1  | t      | timestamp   | service1 | timestamp

# Step 2: Release lock (if necessary)
docker exec -it hdim-postgres psql -U healthdata -d patient_db \
    -c "UPDATE databasechangeloglock SET locked=false;"

# Step 3: Restart application (will reacquire lock and continue)
docker compose restart patient-event-service
```

**Prevention:** Set reasonable lock timeout; ensure migrations complete quickly.

#### Issue 3: "Precondition Failed"

**Symptoms:**
- Migration doesn't apply: `MARK_RAN: Precondition does not match`
- Column already exists in database but migration tries to create it
- Changeset marked as RAN but change wasn't applied

**Root cause:** Precondition evaluated to false (column exists) so Liquibase marked changeset as RAN without applying it.

**Solution:**
```bash
# Step 1: Check what Liquibase thinks is applied
docker exec -it hdim-postgres psql -U healthdata -d patient_db \
    -c "SELECT * FROM databasechangelog WHERE id LIKE '%email%';"

# Step 2: Verify database schema
docker exec -it hdim-postgres psql -U healthdata -d patient_db \
    -c "\d patients" | grep email
# If column exists, precondition is correct (don't re-apply)

# Step 3: If precondition was wrong, fix migration and retry
# Never modify existing migration (already applied)
# Create new migration to add missing constraint if needed
```

**Prevention:** Test preconditions carefully; use `onFail="MARK_RAN"` only when appropriate.

### Debug Techniques

```bash
# View all applied migrations
docker exec -it hdim-postgres psql -U healthdata -d patient_db -c \
    "SELECT id, dateexecuted, description FROM databasechangelog ORDER BY dateexecuted;"

# Check schema of specific table
docker exec -it hdim-postgres psql -U healthdata -d patient_db -c "\d patients"

# View Liquibase status (pending migrations)
./gradlew bootRun --args='liquibase.status'

# Check Liquibase documentation at startup
docker compose logs -f patient-event-service | grep -i liquibase

# Manually verify entity matches schema
# Compare @Column annotations to \d output
```

---

## References & Resources

### HDIM Documentation

- [Entity-Migration Guide](./backend/docs/ENTITY_MIGRATION_GUIDE.md) - Complete synchronization patterns
- [Database Architecture Guide](./backend/docs/DATABASE_ARCHITECTURE_GUIDE.md) - Schema design patterns
- [Liquibase Development Workflow](./backend/docs/LIQUIBASE_DEVELOPMENT_WORKFLOW.md) - Best practices
- [Multi-Tenant Architecture](./01-architecture/multi-tenant-architecture.md) - Tenant scoping in schema

### External Resources

- **[Liquibase Documentation](https://docs.liquibase.com/)** - Official reference
- **[Liquibase XML Format](https://docs.liquibase.com/concepts/changelogs/xml-format.html)** - XML migration syntax
- **[PostgreSQL Documentation](https://www.postgresql.org/docs/16/index.html)** - PostgreSQL 16 reference
- **[Spring Data JPA @Query](https://docs.spring.io/spring-data/jpa/reference/)** - JPA entity mapping

### Related Skills

- **Prerequisite:** [Multi-Tenant Architecture](./01-architecture/multi-tenant-architecture.md) - Understand tenant scoping first
- **Complement:** [Spring Data JPA](./05-spring-boot/spring-data-jpa.md) - Repository patterns using entities
- **Advanced:** [Performance Tuning](./08-infrastructure/performance-optimization.md) - Index optimization strategies

---

## Quick Reference Checklist

### Before Creating Entity

- [ ] Understand which table this entity represents
- [ ] Identify all columns from business requirements
- [ ] Determine which columns are required (NOT NULL)
- [ ] Determine which columns must be unique (UNIQUE)
- [ ] Identify foreign keys (FOREIGN KEY constraints)
- [ ] Plan indices for common query patterns
- [ ] Include tenant_id for multi-tenant isolation

### While Writing Entity + Migration

- [ ] Entity has `@Entity` and `@Table(name="...")` annotations
- [ ] Every JPA `@Column` matches Liquibase `<column>` definition
- [ ] Migration numbered sequentially (`NNNN-description.xml`)
- [ ] Migration includes `<rollback>` section (100% coverage required)
- [ ] Preconditions prevent duplicate migrations (idempotent)
- [ ] Migration included in `db.changelog-master.xml` in correct order
- [ ] Comments explain WHY schema changed
- [ ] Indices created for tenant_id and common query columns

### After Writing Entity + Migration

- [ ] Run EntityMigrationValidationTest: `./gradlew test --tests "*EntityMigrationValidationTest"`
- [ ] Test passes: entity matches database schema
- [ ] Commit entity + migration together (atomic change)
- [ ] Commit message describes schema change
- [ ] Code review verifies migration + entity alignment
- [ ] Ready for production deployment (Liquibase will apply at startup)

---

## Key Takeaways

1. **Entity-Migration Synchronization is CRITICAL:** Entity definitions must match database schema exactly. EntityMigrationValidationTest catches mismatches at test time (fail-fast), preventing production crashes.

2. **Liquibase is the Source of Truth:** Never use Hibernate's `ddl-auto: update`. Liquibase provides auditability (who changed schema and when) and rollback capability (recover from bad migrations).

3. **Preconditions and Rollbacks are Mandatory:** Preconditions make migrations idempotent (safe to run twice). Rollbacks enable fast recovery (revert bad migration). 100% rollback coverage is required.

4. **Multi-Tenant Isolation Starts at Schema:** Every table must have `tenant_id` column with index. This enforces isolation at database level, preventing application bugs from causing cross-tenant data leaks.

5. **Migrations are Permanent:** Never modify existing migrations (already applied in production). Create new migrations to make additional changes. Old migrations become part of production history.

---

## FAQ

**Q: What if I need to rename a column in production?**
A: Create new migration with `<sql>ALTER TABLE ... RENAME COLUMN ...</sql>`. Liquibase tracks this as new changeset. Old column name is gone; new name exists. Include rollback that renames back.

**Q: Can I delete a migration if I haven't committed it yet?**
A: Yes, if it's only in your local dev environment (not pushed to Git). Delete the file and remove the `<include>` from master. If committed, create new migration that does the opposite (DELETE/DROP instead of CREATE/ADD).

**Q: How do I handle schema drift if it already happened in production?**
A: Create corrective migration that matches entity definition. Entity-migration validation will catch the mismatch and fail tests, forcing you to fix before deploying.

**Q: Why is EntityMigrationValidationTest so important?**
A: It catches schema drift **before** production. If entity doesn't match database schema, test fails (fail-fast). This prevents silent bugs where code works in dev (Hibernate auto-creates) but crashes in prod (schema already exists).

**Q: Can different services have different migrations?**
A: Yes! Each service manages its own `patient_db` vs. `quality_measure_db` etc. Each has independent Liquibase changelog. This enables services to evolve schemas independently without coordinating across team.

---

## Next Steps

After completing this guide:

1. **Practice:** Create new entity + write Liquibase migration from scratch
2. **Test:** Run EntityMigrationValidationTest and verify it passes
3. **Review:** Have peer review entity + migration for alignment
4. **Learn:** Move to [Spring Data JPA](./05-spring-boot/spring-data-jpa.md) to learn repository patterns using entities
5. **Contribute:** Help review PRs for entity-migration alignment

---

**← Previous Guide:** [Multi-Tenant Architecture](./01-architecture/multi-tenant-architecture.md)
**Skills Hub:** [Skills Center](./README.md)
**Next Guide:** [Spring Boot 3.x & Spring Data JPA](./05-spring-boot/spring-boot-spring-data.md)

---

**Last Updated:** January 20, 2026
**Version:** 1.0
**Difficulty Level:** ⭐⭐⭐⭐ (4/5 stars - Technical complexity)
**Time Investment:** 1.5-2 weeks
**Prerequisite Skills:** Multi-Tenant Architecture, Basic SQL, JPA/Hibernate basics
**Related Skills:** Multi-Tenant Architecture, Spring Data JPA, Liquibase Workflow, Entity-Migration Validation

---

**← [Skills Hub](./README.md)** | **→ [Next: Spring Boot & Spring Data JPA](../05-spring-boot/spring-boot-spring-data.md)**
