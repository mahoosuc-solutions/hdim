---
name: migration-review
description: Core skill for analyzing Liquibase migration files - validates syntax, checks rollback SQL, detects breaking changes, and verifies entity-migration sync
---

# Migration Review Skill

## Purpose
Provide systematic analysis of Liquibase XML migration files to ensure they are safe, correct, and maintainable.

## Validation Checklist

When reviewing a migration, check the following:

### 1. File Naming & Structure
- [ ] Format: `NNNN-description.xml` (4-digit sequential number + descriptive name)
- [ ] Sequential number (no gaps, no reuse)
- [ ] Descriptive verb-noun name (add-column, rename-table, fix-type)

### 2. XML Validity
- [ ] Valid Liquibase XML schema declaration
- [ ] Well-formed XML (no syntax errors)
- [ ] Unique changeSet ID
- [ ] Author specified
- [ ] Comment explaining the change

### 3. Rollback SQL
- [ ] Rollback block present
- [ ] Rollback operation is reversible
- [ ] Rollback matches forward operation

**Common Rollback Patterns:**
```xml
<!-- CREATE TABLE → DROP TABLE -->
<createTable tableName="foo"/>
<rollback><dropTable tableName="foo"/></rollback>

<!-- ADD COLUMN → DROP COLUMN -->
<addColumn tableName="foo">
  <column name="bar" type="VARCHAR(100)"/>
</addColumn>
<rollback><dropColumn tableName="foo" columnName="bar"/></rollback>

<!-- RENAME COLUMN → REVERSE RENAME -->
<renameColumn tableName="foo" oldColumnName="bar" newColumnName="baz"/>
<rollback>
  <renameColumn tableName="foo" oldColumnName="baz" newColumnName="bar"/>
</rollback>

<!-- DROP COLUMN → Cannot rollback (data loss) -->
<dropColumn tableName="foo" columnName="bar"/>
<rollback>
  <!-- WARNING: Cannot restore dropped data -->
  <addColumn tableName="foo">
    <column name="bar" type="VARCHAR(100)"/>
  </addColumn>
</rollback>
```

### 4. Column Type Mapping (JPA ↔ PostgreSQL)

Ensure migration column types match JPA entity annotations:

| Java Type | JPA Annotation | PostgreSQL | Liquibase XML |
|-----------|---------------|------------|---------------|
| String (≤255) | `@Column` | VARCHAR(N) | `type="VARCHAR(N)"` |
| String (large) | `@Column(columnDefinition = "TEXT")` | TEXT | `type="TEXT"` |
| UUID | `@Column` | uuid | `type="UUID"` |
| Instant | `@Column` | timestamp with time zone | `type="TIMESTAMP WITH TIME ZONE"` |
| LocalDate | `@Column` | date | `type="DATE"` |
| LocalDateTime | `@Column` | timestamp | `type="TIMESTAMP"` |
| Boolean | `@Column` | boolean | `type="BOOLEAN"` |
| Integer | `@Column` | integer | `type="INT"` |
| Long | `@Column` | bigint | `type="BIGINT"` |
| Double | `@Column` | double precision | `type="DOUBLE PRECISION"` |
| BigDecimal | `@Column(precision=P, scale=S)` | numeric(P,S) | `type="DECIMAL(P,S)"` |

**Common Type Mismatches:**
- ❌ `decimal(5,2)` for `Double` → Should be `DOUBLE PRECISION`
- ❌ `NUMERIC` for `Double` → Should be `DOUBLE PRECISION`
- ❌ `VARCHAR` for `UUID` → Should be `UUID`
- ❌ `TIMESTAMP` for `Instant` → Should be `TIMESTAMP WITH TIME ZONE`

### 5. Tenant Isolation (Multi-Tenant Tables)

For new tables in multi-tenant services:
- [ ] `tenant_id` column included (VARCHAR(100), NOT NULL)
- [ ] Index on `tenant_id` created
- [ ] Unique constraints include `tenant_id`

```xml
<createTable tableName="new_table">
  <column name="id" type="UUID">
    <constraints primaryKey="true"/>
  </column>
  <column name="tenant_id" type="VARCHAR(100)">
    <constraints nullable="false"/>
  </column>
  <!-- other columns -->
</createTable>

<!-- Tenant isolation index -->
<createIndex indexName="idx_new_table_tenant" tableName="new_table">
  <column name="tenant_id"/>
</createIndex>
```

### 6. Breaking Change Detection

Classify changes by risk level:

**SAFE (Low Risk):**
- Add table (new entity)
- Add column (nullable, no default)
- Add column (with default value)
- Add index
- Widen column (VARCHAR(50) → VARCHAR(100))
- Add comment
- Add foreign key (new relationship)

**MODERATE (Medium Risk):**
- Add column (NOT NULL with default) - Existing rows get default
- Modify column (widen numeric precision)
- Drop index - May affect performance
- Rename index - No functional impact

**BREAKING (High Risk):**
- Drop column - Data loss, breaks queries
- Rename column - Breaks raw SQL queries (JPA safe if entity updated)
- Narrow column (VARCHAR(100) → VARCHAR(50)) - May truncate data
- Change type (incompatible) - May cause data conversion errors
- Add NOT NULL (no default) - Fails if existing nulls
- Drop foreign key - May allow orphaned records

**CRITICAL (Highest Risk):**
- Drop table - Complete data loss
- Rename table - Breaks all queries
- Change primary key - Breaks foreign key relationships
- Modify foreign key cascade - Changes deletion behavior

### 7. Sequential Migration Numbering

Check that migration number follows the last migration:

```bash
# Find latest migration number
ls backend/modules/services/SERVICE/src/main/resources/db/changelog/*.xml | \
  grep -oE '[0-9]{4}' | \
  sort -n | \
  tail -1

# New migration should be: latest + 1
```

**Example:**
- Latest: `0043-add-composite-indexes.xml`
- New migration: `0044-rename-column.xml` ✓ CORRECT
- Bad migration: `0050-rename-column.xml` ✗ SKIPPED NUMBERS

### 8. Entity Synchronization Validation

Compare migration DDL to JPA entity annotations:

**Step 1:** Find entity for table
```bash
grep -r "@Table.*${TABLE_NAME}" backend/modules/services/SERVICE/src/main/java/
```

**Step 2:** Check entity columns
```bash
grep -E "@Column" EntityFile.java
```

**Step 3:** Match types, names, constraints
- Column names must match (accounting for camelCase → snake_case conversion)
- Column types must map correctly (see table above)
- NOT NULL constraints must match `nullable = false`
- Unique constraints must match `@UniqueConstraint`

**Common Mismatches:**
- Migration creates `user_name` but entity expects `username`
- Migration uses `NUMERIC(5,2)` but entity is `Double` (expects DOUBLE PRECISION)
- Migration has `NOT NULL` but entity has `nullable = true`

## Review Algorithm

When invoked, perform these steps in order:

### Step 1: Read Migration File
```bash
cat backend/modules/services/${SERVICE}/src/main/resources/db/changelog/${MIGRATION_FILE}
```

### Step 2: Parse XML Structure
Extract:
- changeSet ID
- Author
- Comment
- Operations (createTable, addColumn, etc.)
- Affected tables/columns
- Rollback block

### Step 3: Validate Against Checklist
Run through all 8 validation categories above.

### Step 4: Detect Breaking Changes
Classify the operation type and assess risk level.

### Step 5: Check Entity Sync
If operation affects columns:
1. Find corresponding entity
2. Check entity annotations
3. Compare types, names, constraints
4. Flag mismatches

### Step 6: Generate Report
Produce structured review with:
- Status: APPROVE | REJECT | NEEDS_REVIEW
- Findings (checklist results)
- Breaking changes (if any)
- Entity sync status
- Recommendation
- Next steps

## Example Review Output

```
========================================
MIGRATION REVIEW: 0044-fix-measure-modification-audit-columns.xml
========================================

Service: quality-measure-service
Skill: migration-review
Timestamp: 2026-01-21 15:00:00 UTC

Status: APPROVE ✓

----------------------------------------
VALIDATION RESULTS
----------------------------------------

File Naming & Structure:
✓ Sequential number (follows 0043)
✓ Descriptive name (fix-measure-modification-audit-columns)
✓ Standard format (NNNN-description.xml)

XML Validity:
✓ Valid Liquibase schema
✓ Well-formed XML
✓ Unique changeSet ID
✓ Author specified (tdd-swarm-team)
✓ Comment present

Rollback SQL:
✓ Rollback block present
✓ Reverse rename operation
✓ Fully reversible

Column Type Mapping:
✓ TEXT type preserved
✓ No type conversions

Tenant Isolation:
N/A (modifying existing table)

Breaking Changes:
! RENAME COLUMN detected
  Severity: LOW
  Impact: Raw SQL queries (none found in codebase)
  JPA Impact: NONE (entity already uses new name)

Sequential Numbering:
✓ Latest: 0043
✓ Current: 0044
✓ No gaps

Entity Synchronization:
Target Entity: MeasureModificationAuditEntity
Location: src/main/java/com/healthdata/quality/persistence/MeasureModificationAuditEntity.java

Before Migration:
  Database: change_summary (TEXT)
  Entity: change_description (TEXT)
  Status: MISMATCH ⚠️

After Migration:
  Database: change_description (TEXT)
  Entity: change_description (TEXT)
  Status: SYNCHRONIZED ✓

Purpose: Fix entity-migration drift

----------------------------------------
RECOMMENDATION
----------------------------------------

APPROVE ✓ - Migration is safe to apply

This migration corrects an entity-migration synchronization issue.
The column rename aligns the database with the JPA entity definition.

No cross-service impact detected.
No application code changes required (JPA handles mapping).

Next Steps:
1. Apply migration (docker compose up quality-measure-service)
2. Verify Liquibase execution (check logs)
3. Run EntityMigrationValidationTest
4. Run full test suite

========================================
```

## Common Patterns & Best Practices

### Pattern 1: Add Nullable Column (Safe)
```xml
<changeSet id="NNNN-add-status-column" author="developer">
  <addColumn tableName="patients">
    <column name="status" type="VARCHAR(50)"/>
  </addColumn>
  <rollback>
    <dropColumn tableName="patients" columnName="status"/>
  </rollback>
</changeSet>
```
**Review:** ✓ SAFE - Nullable column, no existing data affected

### Pattern 2: Add Column with Default (Safe)
```xml
<changeSet id="NNNN-add-active-flag" author="developer">
  <addColumn tableName="patients">
    <column name="active" type="BOOLEAN" defaultValueBoolean="true">
      <constraints nullable="false"/>
    </column>
  </addColumn>
  <rollback>
    <dropColumn tableName="patients" columnName="active"/>
  </rollback>
</changeSet>
```
**Review:** ✓ SAFE - Default value provided for existing rows

### Pattern 3: Drop Column (Breaking)
```xml
<changeSet id="NNNN-drop-legacy-id" author="developer">
  <dropColumn tableName="patients" columnName="legacy_id"/>
  <rollback>
    <addColumn tableName="patients">
      <column name="legacy_id" type="VARCHAR(50)"/>
    </addColumn>
  </rollback>
</changeSet>
```
**Review:** ✗ BREAKING - Data loss, requires deprecation period

**Recommended Alternative:** Two-phase migration
```xml
<!-- Phase 1: Deprecate (add comment) -->
<changeSet id="NNNN-deprecate-legacy-id" author="developer">
  <sql>
    COMMENT ON COLUMN patients.legacy_id IS 'DEPRECATED: Will be removed 2026-02-20';
  </sql>
</changeSet>

<!-- Phase 2: Drop (30 days later) -->
<changeSet id="NNNN+N-drop-legacy-id" author="developer">
  <preConditions>
    <sqlCheck expectedResult="0">
      SELECT COUNT(*) FROM information_schema.columns
      WHERE table_name = 'patients' AND column_name = 'legacy_id'
      AND column_comment NOT LIKE '%DEPRECATED%'
    </sqlCheck>
  </preConditions>
  <dropColumn tableName="patients" columnName="legacy_id"/>
</changeSet>
```

### Pattern 4: Type Conversion (Safe if widening)
```xml
<changeSet id="NNNN-widen-name-column" author="developer">
  <modifyDataType tableName="patients"
                   columnName="name"
                   newDataType="VARCHAR(200)"/>  <!-- was VARCHAR(100) -->
  <rollback>
    <modifyDataType tableName="patients"
                     columnName="name"
                     newDataType="VARCHAR(100)"/>
  </rollback>
</changeSet>
```
**Review:** ✓ SAFE - Widening column, no data loss

### Pattern 5: Type Conversion (Breaking if narrowing)
```xml
<changeSet id="NNNN-narrow-code-column" author="developer">
  <modifyDataType tableName="patients"
                   columnName="code"
                   newDataType="VARCHAR(10)"/>  <!-- was VARCHAR(50) -->
</changeSet>
```
**Review:** ✗ BREAKING - May truncate data, check max length first

**Recommended:** Add validation
```xml
<changeSet id="NNNN-narrow-code-column" author="developer">
  <preConditions onFail="HALT">
    <sqlCheck expectedResult="0">
      SELECT COUNT(*) FROM patients WHERE LENGTH(code) > 10
    </sqlCheck>
  </preConditions>
  <modifyDataType tableName="patients"
                   columnName="code"
                   newDataType="VARCHAR(10)"/>
</changeSet>
```

## Integration with Agents

Service agents invoke this skill during migration review:

```
Agent: quality-measure-liquibase-agent
↓
Skill: migration-review
↓
Analysis: Validates migration against all checklist items
↓
Report: Structured findings with APPROVE/REJECT recommendation
```

The agent adds service-specific context (cross-service dependencies, etc.) to the skill's core validation logic.
