---
description: Generate JPA entity + synchronized Liquibase migration for HDIM service
arguments:
  service_name:
    description: Service name (e.g., patient-service, quality-measure-service)
    type: string
    required: true
  entity_name:
    description: Entity name in PascalCase (e.g., Patient, QualityMeasure, CareGap)
    type: string
    required: true
  description:
    description: Brief description of the entity's purpose
    type: string
    required: false
    default: "Entity for managing domain data"
---

# Add Entity Command

Generate a complete JPA entity with synchronized Liquibase migration for an HDIM microservice.

## What This Command Does

1. **Generates JPA Entity** - Creates entity class with:
   - UUID primary key
   - Tenant isolation (critical for multi-tenant)
   - Audit fields (createdAt, updatedAt, createdBy, updatedBy)
   - Pre-persist/pre-update hooks

2. **Generates Repository** - Creates Spring Data JPA repository with:
   - Tenant-filtered queries
   - Proper 404 (not 403) for unauthorized access
   - Common query methods

3. **Generates Liquibase Migration** - Creates synchronized database migration with:
   - Proper column types matching JPA annotations
   - Tenant isolation index (critical for performance)
   - Rollback SQL (100% coverage enforced)
   - Sequential migration numbering

4. **Updates Master Changelog** - Adds migration to db.changelog-master.xml

5. **Runs Validation** - Executes EntityMigrationValidationTest to ensure synchronization

## Usage

```bash
/add-entity {{service_name}} {{entity_name}} [description]
```

## Examples

```bash
# Add Appointment entity to patient-service
/add-entity patient-service Appointment "Manages patient appointments"

# Add MeasureResult entity to quality-measure-service
/add-entity quality-measure-service MeasureResult "Stores quality measure evaluation results"

# Add CareGapAlert entity to care-gap-service
/add-entity care-gap-service CareGapAlert "Tracks care gap notifications"
```

## Implementation

You are tasked with generating a complete entity-migration pair for the HDIM platform.

### Step 1: Validate Service Exists

Check that the service exists:
```bash
ls backend/modules/services/{{service_name}}
```

If the service doesn't exist, suggest using `/create-service` instead.

### Step 2: Determine Next Migration Number

Find the next sequential migration number by examining existing migrations:

```bash
# List existing migrations
ls backend/modules/services/{{service_name}}/src/main/resources/db/changelog/

# Example output: 0000-enable-extensions.xml, 0001-create-patients-table.xml
# Next number would be: 0002
```

**Critical:** Use the next sequential number (no gaps, no reuse).

### Step 3: Generate Variables

Convert entity name to required formats:
- `ENTITY_CLASS_NAME`: {{entity_name}} (e.g., "Patient")
- `TABLE_NAME`: Snake case plural (e.g., "patients")
- `RESOURCE_PATH`: Kebab case plural (e.g., "patients")
- `SERVICE_NAME`: Extract from service name (e.g., "patient" from "patient-service")
- `MIGRATION_ID`: Next migration number (e.g., "0002")
- `AUTHOR`: Current git user
- `MIGRATION_DESCRIPTION`: "Create {{TABLE_NAME}} table with tenant isolation"

### Step 4: Generate Entity File

Use the entity template at `.claude/plugins/hdim-accelerator/templates/entity/entity-template.java`:

**File Location:**
```
backend/modules/services/{{service_name}}/src/main/java/com/healthdata/{{SERVICE_NAME}}/domain/model/{{ENTITY_CLASS_NAME}}.java
```

**Replace template variables:**
- `{{SERVICE_NAME}}` → service name (e.g., "patient")
- `{{ENTITY_CLASS_NAME}}` → entity name (e.g., "Patient")
- `{{TABLE_NAME}}` → table name (e.g., "patients")
- `{{ENTITY_DESCRIPTION}}` → provided description

### Step 5: Generate Repository File

Use the repository template at `.claude/plugins/hdim-accelerator/templates/entity/repository-template.java`:

**File Location:**
```
backend/modules/services/{{service_name}}/src/main/java/com/healthdata/{{SERVICE_NAME}}/domain/repository/{{ENTITY_CLASS_NAME}}Repository.java
```

**Replace template variables** (same as entity).

### Step 6: Generate Liquibase Migration

Use the migration template at `.claude/plugins/hdim-accelerator/templates/migration/migration-template.xml`:

**File Location:**
```
backend/modules/services/{{service_name}}/src/main/resources/db/changelog/{{MIGRATION_ID}}-create-{{TABLE_NAME}}-table.xml
```

**Replace template variables:**
- `{{MIGRATION_ID}}` → migration number (e.g., "0002")
- `{{TABLE_NAME}}` → table name (e.g., "patients")
- `{{AUTHOR}}` → git user
- `{{MIGRATION_DESCRIPTION}}` → description

### Step 7: Update Master Changelog

Add the new migration to `db.changelog-master.xml`:

**File Location:**
```
backend/modules/services/{{service_name}}/src/main/resources/db/changelog/db.changelog-master.xml
```

**Add include before closing `</databaseChangeLog>`:**
```xml
<include file="db/changelog/{{MIGRATION_ID}}-create-{{TABLE_NAME}}-table.xml"/>
```

### Step 8: Run Validation Test

Execute EntityMigrationValidationTest to ensure entity and migration are synchronized:

```bash
cd backend
./gradlew :modules:services:{{service_name}}:test --tests "*EntityMigrationValidationTest"
```

**Expected Result:**
- ✅ All tests pass
- ✅ No schema-validation errors
- ✅ Entity and migration are synchronized

**If tests fail:**
- Review column type mappings (Java ↔ PostgreSQL ↔ Liquibase)
- Ensure table name matches `@Table(name = "...")`
- Verify all columns have corresponding `@Column` annotations

### Step 9: Summary

Provide a summary to the user:

```
✅ Entity created successfully!

**Files Created:**
- Entity: backend/modules/services/{{service_name}}/src/main/java/com/healthdata/{{SERVICE_NAME}}/domain/model/{{ENTITY_CLASS_NAME}}.java
- Repository: backend/modules/services/{{service_name}}/src/main/java/com/healthdata/{{SERVICE_NAME}}/domain/repository/{{ENTITY_CLASS_NAME}}Repository.java
- Migration: backend/modules/services/{{service_name}}/src/main/resources/db/changelog/{{MIGRATION_ID}}-create-{{TABLE_NAME}}-table.xml

**Files Updated:**
- Master Changelog: db.changelog-master.xml

**Validation:**
- EntityMigrationValidationTest: ✅ PASSED

**Next Steps:**
1. Add custom fields to {{ENTITY_CLASS_NAME}}.java (update both entity and migration)
2. Add custom query methods to {{ENTITY_CLASS_NAME}}Repository.java
3. Create service class: {{ENTITY_CLASS_NAME}}Service.java
4. Create DTO classes: {{ENTITY_CLASS_NAME}}Request.java, {{ENTITY_CLASS_NAME}}Response.java
5. Create controller using: /add-endpoint {{service_name}} {{RESOURCE_PATH}} GET
```

## Best Practices (From CLAUDE.md)

### Multi-Tenant Isolation
**CRITICAL:** Every entity MUST have `tenantId` field. Every query MUST filter by tenant.

```java
@Column(name = "tenant_id", nullable = false, length = 100)
private String tenantId;
```

```xml
<column name="tenant_id" type="VARCHAR(100)">
    <constraints nullable="false"/>
</column>
```

### Audit Fields
Include audit fields for compliance:
- `created_at` - Record creation timestamp
- `updated_at` - Last modification timestamp
- `created_by` - User who created record
- `updated_by` - User who last modified record

### Column Type Mapping

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

### Rollback SQL
**REQUIRED:** Every migration MUST include rollback SQL (100% coverage enforced).

```xml
<rollback>
    <dropTable tableName="{{TABLE_NAME}}"/>
</rollback>
```

### Indexes
Always create index on `tenant_id` for multi-tenant query performance:

```xml
<createIndex indexName="idx_{{TABLE_NAME}}_tenant_id" tableName="{{TABLE_NAME}}">
    <column name="tenant_id"/>
</createIndex>
```

## HIPAA Compliance Notes

If this entity contains PHI (Protected Health Information):
1. Add `@Audited` annotation to entity class
2. Ensure cache TTL ≤ 5 minutes (if caching)
3. Add Cache-Control headers to API endpoints
4. Document PHI fields in comments

See `backend/HIPAA-CACHE-COMPLIANCE.md` for details.

## Troubleshooting

### Schema-validation: missing table
**Cause:** Liquibase migration didn't run.
**Fix:** Restart service with `docker compose up {{service_name}}`

### Schema-validation: wrong column type
**Cause:** Entity annotation doesn't match migration column type.
**Fix:** Update migration to match entity OR update entity to match migration (prefer updating migration).

### EntityMigrationValidationTest fails
**Cause:** Entity and migration out of sync.
**Fix:** Review column names, types, nullable constraints. Use column type mapping table above.

### Migration number conflict
**Cause:** Another developer used the same migration number.
**Fix:** Rename your migration to next available number, update master changelog.

## Related Commands

- `/add-migration` - Add migration without entity
- `/validate-schema` - Run EntityMigrationValidationTest only
- `/add-endpoint` - Add REST API endpoint for entity

## Related Skills

- `database-migrations` - Liquibase best practices
- `hipaa-compliance` - PHI handling requirements

## Related Agents

- `migration-validator` - Auto-validates entity-migration sync (proactive)
