---
description: Run EntityMigrationValidationTest to verify entity-migration synchronization
arguments:
  service_name:
    description: Service name (e.g., patient-service, quality-measure-service) or "all" for all services
    type: string
    required: true
---

# Validate Schema Command

Run EntityMigrationValidationTest to ensure JPA entities and Liquibase migrations are synchronized.

## What This Command Does

1. **Runs EntityMigrationValidationTest** - Validates entity-migration synchronization
2. **Checks Rollback Coverage** - Ensures 100% changesets have rollback SQL
3. **Verifies ddl-auto Setting** - Confirms `ddl-auto: validate` (not create/update)
4. **Reports Drift Issues** - Identifies missing tables, wrong column types, etc.

## Why This Matters

**Production Safety:** The RefreshToken authentication bug (December 2025) was caused by entity-migration drift. This validation prevents similar issues.

**From CLAUDE.md:**
> "Entity-migration synchronization prevents production schema drift issues (like the RefreshToken authentication bug)."

## Usage

```bash
# Validate specific service
/validate-schema {{service_name}}

# Validate all services with EntityMigrationValidationTest
/validate-schema all
```

## Examples

```bash
# Validate patient service
/validate-schema patient-service

# Validate quality measure service
/validate-schema quality-measure-service

# Validate all services
/validate-schema all
```

## Implementation

You are tasked with running entity-migration validation tests.

### Step 1: Validate Service Name

If `{{service_name}}` is NOT "all":

```bash
# Check service exists
ls backend/modules/services/{{service_name}}
```

### Step 2: Run EntityMigrationValidationTest

#### For Specific Service:

```bash
cd backend
./gradlew :modules:services:{{service_name}}:test --tests "*EntityMigrationValidationTest"
```

#### For All Services:

```bash
cd backend
./gradlew test --tests "*EntityMigrationValidationTest"
```

### Step 3: Check Rollback SQL Coverage

```bash
cd backend
./scripts/test-liquibase-rollback.sh
```

**Expected:** `✅ 100% of changesets have rollback SQL (XXX/XXX)`

### Step 4: Verify ddl-auto Configuration

Check application.yml for each service:

```bash
grep -r "ddl-auto:" backend/modules/services/{{service_name}}/src/main/resources/application*.yml
```

**Expected:** `ddl-auto: validate`
**NOT:** `ddl-auto: create`, `ddl-auto: update`, `ddl-auto: create-drop`

### Step 5: Parse Test Results

**Success Output:**
```
EntityMigrationValidationTest > shouldValidateEntitiesMatchMigrations() PASSED
```

**Failure Output (Schema Drift Detected):**
```
Schema-validation: missing table [table_name]
Schema-validation: wrong column type encountered in column [column_name]
  Expected: VARCHAR(255)
  Actual: TEXT
```

### Step 6: Generate Summary

#### Success Summary:
```
✅ Schema validation PASSED for {{service_name}}

**Validation Results:**
- EntityMigrationValidationTest: ✅ PASSED
- Rollback SQL Coverage: ✅ 100% (XXX/XXX changesets)
- ddl-auto Setting: ✅ validate

**Entities Validated:** X entities
**Migrations Validated:** Y migrations

All entities and migrations are synchronized. Safe to deploy.
```

#### Failure Summary:
```
❌ Schema validation FAILED for {{service_name}}

**Issues Detected:**

1. Missing Table: `appointments`
   - Entity: Appointment.java exists
   - Migration: No corresponding Liquibase migration found
   - Fix: Run /add-migration {{service_name}} "create-appointments-table"

2. Column Type Mismatch: `patients.date_of_birth`
   - Entity: @Column(type = "DATE")
   - Migration: type="TIMESTAMP"
   - Fix: Update migration to use type="DATE" OR update entity to LocalDateTime

3. Missing Column: `patients.risk_score`
   - Entity: Has @Column(name = "risk_score")
   - Migration: Column not in database schema
   - Fix: Run /add-migration {{service_name}} "add-risk-score-to-patients"

**Next Steps:**
1. Fix entity-migration mismatches (see above)
2. Re-run validation: /validate-schema {{service_name}}
3. Commit changes after validation passes
```

## Services with EntityMigrationValidationTest

**Core Services:**
- ✅ authentication module
- ✅ patient-service
- ✅ quality-measure-service
- ✅ care-gap-service
- ✅ fhir-service
- ✅ sales-automation-service

**Event Services (CQRS):**
- ✅ patient-event-service
- ✅ quality-measure-event-service
- ✅ care-gap-event-service
- ✅ clinical-workflow-event-service

**Additional Services:** (Check for EntityMigrationValidationTest in each service's test directory)

## Common Validation Errors

### Missing Table

**Error:**
```
Schema-validation: missing table [table_name]
```

**Cause:** Entity exists but no Liquibase migration creates the table.

**Fix:**
```bash
/add-entity {{service_name}} EntityName
# OR
/add-migration {{service_name}} "create-table-name-table"
```

### Wrong Column Type

**Error:**
```
Schema-validation: wrong column type encountered in column [column_name]
Expected: VARCHAR(255)
Actual: TEXT
```

**Cause:** Entity `@Column` annotation doesn't match Liquibase migration type.

**Fix Options:**
1. **Update migration** (preferred if not in production):
```xml
<modifyDataType tableName="table_name" columnName="column_name" newDataType="VARCHAR(255)"/>
```

2. **Update entity** (if migration is correct):
```java
@Column(name = "column_name", length = 255)  // Change from TEXT to VARCHAR(255)
```

### Missing Column

**Error:**
```
Schema-validation: missing column [column_name] in table [table_name]
```

**Cause:** Entity has `@Column` but migration doesn't create it.

**Fix:**
```bash
/add-migration {{service_name}} "add-column-name-to-table-name"
```

Then edit migration to add column:
```xml
<addColumn tableName="table_name">
    <column name="column_name" type="VARCHAR(255)"/>
</addColumn>
```

### Extra Column

**Error:**
```
Schema-validation: column [column_name] found in database but not in entity
```

**Cause:** Migration creates column but entity doesn't have `@Column`.

**Fix Options:**
1. **Add to entity** (if column should exist):
```java
@Column(name = "column_name")
private String columnName;
```

2. **Remove from database** (if column is obsolete):
```bash
/add-migration {{service_name}} "drop-unused-column-name"
```

### Wrong Table Name

**Error:**
```
Schema-validation: table [expected_name] doesn't match @Table annotation [actual_name]
```

**Cause:** `@Table(name = "...")` doesn't match migration `tableName="..."`.

**Fix:** Ensure both match (prefer snake_case plural in both).

## Hibernate ddl-auto Settings

**Valid Settings:**
| Setting | Behavior | Production Use |
|---------|----------|----------------|
| `validate` | Validates schema matches entities | ✅ REQUIRED |
| `none` | No schema management | ❌ Not recommended |
| `create` | Drops and recreates schema | ❌ NEVER (data loss!) |
| `create-drop` | Create on startup, drop on shutdown | ❌ NEVER (data loss!) |
| `update` | Auto-updates schema | ❌ NEVER (causes drift!) |

**HDIM Standard:** `ddl-auto: validate` in ALL environments.

## Entity-Migration Synchronization Checklist

Use this checklist when creating/modifying entities:

- [ ] Entity has `@Entity` and `@Table` annotations
- [ ] Table name is snake_case plural (e.g., `patients`)
- [ ] All fields have `@Column(name = "snake_case")` annotations
- [ ] Liquibase migration exists with matching table name
- [ ] All entity columns exist in migration
- [ ] Column types match (use type mapping table)
- [ ] Nullable constraints match (`nullable = false` ↔ `constraints nullable="false"`)
- [ ] Primary key uses UUID with `gen_random_uuid()` default
- [ ] Tenant isolation field exists (`tenant_id`)
- [ ] Audit fields exist (`created_at`, `updated_at`, `created_by`, `updated_by`)
- [ ] Migration includes rollback SQL
- [ ] Migration added to db.changelog-master.xml
- [ ] EntityMigrationValidationTest passes

## Column Type Mapping Reference

| Java Type | JPA Annotation | PostgreSQL Type | Liquibase Type |
|-----------|---------------|----------------|----------------|
| UUID | @Id @GeneratedValue(strategy = UUID) | uuid | UUID |
| String | @Column(length = N) | VARCHAR(N) | VARCHAR(N) |
| String | @Column(columnDefinition = "TEXT") | TEXT | TEXT |
| Instant | @Column | timestamp with time zone | TIMESTAMP WITH TIME ZONE |
| LocalDate | @Column | date | DATE |
| LocalDateTime | @Column | timestamp | TIMESTAMP |
| Boolean | @Column | boolean | BOOLEAN |
| Integer | @Column | integer | INT |
| Long | @Column | bigint | BIGINT |
| BigDecimal | @Column(precision = P, scale = S) | decimal(P,S) | DECIMAL(P,S) |

## CI/CD Integration

**GitHub Actions:** EntityMigrationValidationTest runs automatically on PRs.

**Pre-commit Hook:**
```bash
# Install hook
ln -s ../../backend/scripts/pre-commit-db-validation.sh .git/hooks/pre-commit

# Validates entities when modified
./backend/scripts/pre-commit-db-validation.sh
```

## Best Practices (From Phase 21)

### 1. Validate Before Commit
**ALWAYS** run validation before committing entity or migration changes.

### 2. Fix Drift Immediately
**NEVER** ignore validation failures. Fix immediately to prevent production issues.

### 3. Use Liquibase Only
**NEVER** use `ddl-auto: create` or `ddl-auto: update`. Use Liquibase migrations exclusively.

### 4. Synchronize Changes
When modifying an entity field, **ALWAYS** create a corresponding migration.

### 5. Test Migration
**ALWAYS** test migration locally:
```bash
docker compose up {{service_name}}
```

Check logs for: `Liquibase update successful`

## Troubleshooting

### Test Not Found

**Error:** `No tests found for given includes: [*EntityMigrationValidationTest]`

**Cause:** Service doesn't have EntityMigrationValidationTest yet.

**Fix:** Create test file:
```java
// backend/modules/services/{{service_name}}/src/test/java/.../EntityMigrationValidationTest.java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Tag("entity-migration-validation")
class EntityMigrationValidationTest {
    // Test validates entities match migrations
}
```

### PostgreSQL Container Fails

**Error:** `Could not start PostgreSQLContainer`

**Cause:** Docker not running or insufficient resources.

**Fix:**
1. Start Docker: `systemctl start docker`
2. Increase Docker memory (4GB+ recommended)

### False Positive: Inherited Fields

**Error:** Validation reports missing fields from `@MappedSuperclass`

**Cause:** Entity inherits from base class with `@MappedSuperclass`.

**Fix:** Ensure base class fields have corresponding columns in migration.

## Related Commands

- `/add-entity` - Generate entity with synchronized migration
- `/add-migration` - Generate standalone migration

## Related Skills

- `database-migrations` - Liquibase best practices

## Related Agents

- `migration-validator` - Auto-runs validation after entity changes (proactive)

## Documentation

- See `backend/docs/ENTITY_MIGRATION_GUIDE.md` for complete guide
- See `backend/docs/DATABASE_MIGRATION_RUNBOOK.md` for operations
- See `CLAUDE.md` for entity-migration synchronization standards
