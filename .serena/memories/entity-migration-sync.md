# Entity-Migration Synchronization Guide

## ⚠️ CRITICAL: Prevents Production Schema Drift

**This practice prevents authentication bugs and production failures.**

Example: The RefreshToken authentication bug was caused by JPA entity and Liquibase migration mismatch.

**Full Documentation**: `backend/docs/ENTITY_MIGRATION_GUIDE.md`

---

## The Golden Rule

**JPA entities and Liquibase migrations MUST be synchronized at all times.**

```
JPA Entity (@Entity) ←→ Liquibase Migration (.xml) = Database Schema
```

Any mismatch leads to:
- Authentication failures
- Runtime exceptions
- Data corruption
- Production downtime

---

## Quick Checklist

### When Creating a New Entity

- [ ] Create JPA entity with `@Entity`, `@Table`, `@Column` annotations
- [ ] Create Liquibase migration file (`NNNN-create-table.xml`)
- [ ] Add migration include to `db.changelog-master.xml`
- [ ] Use sequential migration numbers (no gaps, no reuse)
- [ ] Run validation test: `./gradlew test --tests "*EntityMigrationValidationTest"`

### When Modifying an Entity

- [ ] Update `@Column` annotations in entity
- [ ] Create NEW migration (never modify existing ones)
- [ ] Use descriptive migration ID: `NNNN-add-field-to-table.xml`
- [ ] Run validation test to ensure sync

---

## Hibernate DDL Auto Configuration

### ✅ CORRECT - Always Use `validate`

```yaml
# application.yml (ALL environments)
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Enforces schema matches entities

  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml
```

### ❌ WRONG - Never Use These

```yaml
# DO NOT USE IN ANY ENVIRONMENT
spring:
  jpa:
    hibernate:
      ddl-auto: update   # ❌ Creates schema drift
      ddl-auto: create   # ❌ Destroys data
      ddl-auto: create-drop  # ❌ Destroys data
```

**Why?**
- `update` creates schema changes that aren't in migrations
- `create` wipes the database
- `validate` ensures entity/migration synchronization

---

## Column Type Mapping Reference

### String Types

| Java Type | JPA Annotation | Liquibase Type | PostgreSQL Type |
|-----------|----------------|----------------|-----------------|
| String (short) | `@Column(length = 255)` | `VARCHAR(255)` | `character varying(255)` |
| String (large) | `@Column(columnDefinition = "TEXT")` | `TEXT` | `text` |

### Numeric Types

| Java Type | JPA Annotation | Liquibase Type | PostgreSQL Type |
|-----------|----------------|----------------|-----------------|
| Integer | `@Column` | `INT` | `integer` |
| Long | `@Column` | `BIGINT` | `bigint` |
| BigDecimal | `@Column(precision = 19, scale = 2)` | `DECIMAL(19,2)` | `numeric(19,2)` |
| Double | `@Column` | `DOUBLE` | `double precision` |

### Date/Time Types

| Java Type | JPA Annotation | Liquibase Type | PostgreSQL Type |
|-----------|----------------|----------------|-----------------|
| Instant | `@Column` | `TIMESTAMP WITH TIME ZONE` | `timestamp with time zone` |
| LocalDateTime | `@Column` | `TIMESTAMP` | `timestamp without time zone` |
| LocalDate | `@Column` | `DATE` | `date` |

### Other Types

| Java Type | JPA Annotation | Liquibase Type | PostgreSQL Type |
|-----------|----------------|----------------|-----------------|
| UUID | `@Column(columnDefinition = "uuid")` | `UUID` | `uuid` |
| Boolean | `@Column` | `BOOLEAN` | `boolean` |
| byte[] | `@Lob` | `BLOB` | `bytea` |
| Enum | `@Enumerated(STRING)` | `VARCHAR(50)` | `character varying(50)` |

---

## Entity Pattern

### Complete Example

```java
package com.healthdata.patient.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "patients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "fhir_id", nullable = false, length = 255)
    private String fhirId;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "date_of_birth")
    private Instant dateOfBirth;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
```

---

## Migration Pattern

### Create Table Migration

**File**: `src/main/resources/db/changelog/0001-create-patients-table.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="0001-create-patients-table" author="system">
        <createTable tableName="patients">
            <column name="id" type="uuid">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="tenant_id" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="fhir_id" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="first_name" type="VARCHAR(100)"/>
            <column name="last_name" type="VARCHAR(100)"/>
            <column name="date_of_birth" type="TIMESTAMP WITH TIME ZONE"/>
            <column name="active" type="BOOLEAN" defaultValueBoolean="true">
                <constraints nullable="false"/>
            </column>
            <column name="created_at" type="TIMESTAMP WITH TIME ZONE">
                <constraints nullable="false"/>
            </column>
            <column name="updated_at" type="TIMESTAMP WITH TIME ZONE"/>
        </createTable>

        <createIndex tableName="patients" indexName="idx_patients_tenant_id">
            <column name="tenant_id"/>
        </createIndex>

        <createIndex tableName="patients" indexName="idx_patients_fhir_id">
            <column name="fhir_id"/>
        </createIndex>
    </changeSet>
</databaseChangeLog>
```

### Add Column Migration

**File**: `src/main/resources/db/changelog/0002-add-email-to-patients.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="0002-add-email-to-patients" author="system">
        <addColumn tableName="patients">
            <column name="email" type="VARCHAR(255)"/>
        </addColumn>
    </changeSet>
</databaseChangeLog>
```

### Master Changelog

**File**: `src/main/resources/db/changelog/db.changelog-master.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <include file="db/changelog/0001-create-patients-table.xml"/>
    <include file="db/changelog/0002-add-email-to-patients.xml"/>
    <!-- Add new migrations here -->
</databaseChangeLog>
```

---

## Validation Testing

### Automated Validation Test

Every critical service has this test:

```java
package com.healthdata.patient.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.liquibase.enabled=true"
})
class EntityMigrationValidationTest {

    @Test
    void contextLoads() {
        // If this test passes, JPA entities match Liquibase migrations
        // If it fails, there's a schema mismatch
    }
}
```

### Run Validation

```bash
# Run validation test for specific service
./gradlew :modules:services:patient-service:test --tests "*EntityMigrationValidationTest"

# Run all validation tests
./gradlew test --tests "*EntityMigrationValidationTest"
```

### Expected Output

✅ **Success**: Entities match migrations
```
EntityMigrationValidationTest > contextLoads() PASSED
```

❌ **Failure**: Schema mismatch detected
```
org.hibernate.tool.schema.spi.SchemaManagementException:
Schema-validation: missing column [email] in table [patients]
```

---

## Common Scenarios

### Scenario 1: Adding a New Field

**Step 1**: Update entity
```java
@Entity
@Table(name = "patients")
public class Patient {
    // ... existing fields

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;  // NEW FIELD
}
```

**Step 2**: Create migration
```xml
<!-- 0003-add-phone-to-patients.xml -->
<changeSet id="0003-add-phone-to-patients" author="developer">
    <addColumn tableName="patients">
        <column name="phone_number" type="VARCHAR(20)"/>
    </addColumn>
</changeSet>
```

**Step 3**: Add to master changelog
```xml
<include file="db/changelog/0003-add-phone-to-patients.xml"/>
```

**Step 4**: Validate
```bash
./gradlew test --tests "*EntityMigrationValidationTest"
```

### Scenario 2: Renaming a Field

**Step 1**: Create migration (rename column)
```xml
<changeSet id="0004-rename-patient-fhir-id" author="developer">
    <renameColumn tableName="patients"
                  oldColumnName="fhir_id"
                  newColumnName="external_id"
                  columnDataType="VARCHAR(255)"/>
</changeSet>
```

**Step 2**: Update entity
```java
@Column(name = "external_id", nullable = false, length = 255)
private String externalId;  // Renamed from fhirId
```

### Scenario 3: Changing Column Type

**Step 1**: Create migration
```xml
<changeSet id="0005-change-tenant-id-length" author="developer">
    <modifyDataType tableName="patients"
                    columnName="tenant_id"
                    newDataType="VARCHAR(100)"/>
</changeSet>
```

**Step 2**: Update entity
```java
@Column(name = "tenant_id", nullable = false, length = 100)  // Changed from 50
private String tenantId;
```

---

## Services with Validation Tests

✅ Validated services (as of January 2026):
- authentication module
- patient-service
- quality-measure-service
- care-gap-service
- fhir-service
- sales-automation-service

---

## Migration Best Practices

### 1. Sequential Numbering

```
✅ Good
0001-create-patients-table.xml
0002-add-email-to-patients.xml
0003-add-phone-to-patients.xml

❌ Bad
001-patients.xml
100-email.xml  // Gap in numbering
002-phone.xml  // Out of order
```

### 2. Descriptive IDs

```
✅ Good
id="0012-add-care-gap-auto-closure-fields"

❌ Bad
id="migration-12"
id="update"
id="v1.2.3"
```

### 3. Never Modify Existing Migrations

```
✅ Good
Create new migration: 0013-fix-care-gap-column-type.xml

❌ Bad
Edit existing: 0012-add-care-gap-auto-closure-fields.xml
```

### 4. Always Include Indexes for Foreign Keys

```xml
<changeSet id="0010-add-patient-observations" author="system">
    <createTable tableName="observations">
        <column name="id" type="uuid">
            <constraints primaryKey="true"/>
        </column>
        <column name="patient_id" type="uuid">
            <constraints nullable="false"/>
        </column>
    </createTable>

    <!-- Add index for foreign key -->
    <createIndex tableName="observations" indexName="idx_observations_patient_id">
        <column name="patient_id"/>
    </createIndex>
</changeSet>
```

---

## Troubleshooting

### Error: Schema-validation: missing column

```
org.hibernate.tool.schema.spi.SchemaManagementException:
Schema-validation: missing column [phone_number] in table [patients]
```

**Cause**: Entity has field that doesn't exist in database.

**Solution**:
1. Create migration to add column
2. Run `./gradlew bootRun` to apply migration
3. Run validation test again

### Error: Schema-validation: wrong column type

```
Schema-validation: wrong column type encountered in column [tenant_id]
in table [patients]; found [varchar(50)], but expecting [varchar(100)]
```

**Cause**: Entity annotation doesn't match database column type.

**Solution**:
1. Create migration with `modifyDataType`
2. Apply migration
3. Validate

### Error: Validation failed for changeset

```
Validation Failed:
1 change sets check sum
  db/changelog/0001-create-patients-table.xml::0001-create-patients-table::system
  was: 8:abc123
  but is now: 8:def456
```

**Cause**: Existing migration was modified.

**Solution**:
1. Revert changes to existing migration
2. Create NEW migration for changes
3. NEVER modify applied migrations

---

## Pre-Commit Checklist

Before committing entity/migration changes:

- [ ] Entity annotations match migration types exactly
- [ ] All new columns have corresponding migrations
- [ ] Master changelog includes new migrations
- [ ] Migration IDs are sequential and descriptive
- [ ] `ddl-auto: validate` is set in all environment configs
- [ ] Validation test passes: `./gradlew test --tests "*EntityMigrationValidationTest"`
- [ ] No modifications to existing migrations
- [ ] Indexes created for all foreign keys
- [ ] Column names use snake_case (database convention)
- [ ] Field names use camelCase (Java convention)

---

## Emergency Recovery

If production schema drift is detected:

1. **Assess damage**: Compare entity vs database schema
2. **Create corrective migration**: Align database to current entities
3. **Test in staging**: Verify migration works
4. **Deploy to production**: Apply migration during maintenance window
5. **Add validation test**: Prevent future drift
6. **Document incident**: Update runbook

---

## Resources

- **Full Guide**: `backend/docs/ENTITY_MIGRATION_GUIDE.md`
- **Liquibase Docs**: https://docs.liquibase.com/
- **Hibernate Validation**: https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html#schema-generation
