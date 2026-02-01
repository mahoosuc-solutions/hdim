# Liquibase Development Workflow Guide
## Complete Lifecycle Management for HDIM Microservices

**Document Version:** 1.0
**Last Updated:** January 2026
**Status:** Primary Reference for Database Development

---

## Table of Contents

1. [Overview & Philosophy](#overview--philosophy)
2. [Critical Rules: When to Use `create` vs `validate`](#critical-rules-when-to-use-create-vs-validate)
3. [Development Lifecycle Phases](#development-lifecycle-phases)
4. [Day-to-Day Development Workflows](#day-to-day-development-workflows)
5. [Safe Database Resets & Switching Modes](#safe-database-resets--switching-modes)
6. [Shared Tables Pattern (Auth Tables)](#shared-tables-pattern-auth-tables)
7. [Liquibase Migration Strategies](#liquibase-migration-strategies)
8. [Testing & Validation](#testing--validation)
9. [Troubleshooting Guide](#troubleshooting-guide)
10. [Command Reference](#command-reference)

---

## Overview & Philosophy

HDIM uses **database-per-service** architecture with **Liquibase** for schema management. The critical challenge is managing the relationship between:

- **Hibernate's `ddl-auto` setting** - Controls whether JPA automatically creates/updates schema
- **Liquibase migrations** - Version-controlled, auditable schema changes
- **Database persistence** - Docker volumes survive container restarts with stale schema

**The Problem:** If Hibernate's `ddl-auto: create` runs before Liquibase migrations execute, it creates tables that Liquibase then fails to create, causing "relation already exists" errors and restart loops.

**The Solution:** Understand the lifecycle and apply the correct settings at each phase.

---

## Critical Rules: When to Use `create` vs `validate`

### Decision Matrix

| Scenario | `ddl-auto` Setting | Why | Risk |
|----------|-------------------|-----|------|
| **Fresh local database** | `create` | Schema must be created from scratch | Low - fresh state |
| **First service startup** | `create` (ONE TIME ONLY) | Liquibase migration table doesn't exist yet | Medium - must switch to `validate` |
| **Normal development** | `validate` | Liquibase handles all schema changes | Low - idempotent |
| **CI/CD pipeline** | `validate` | Database already initialized by `init-multi-db.sh` | Low - controlled |
| **Production** | `validate` | NEVER use `create` (causes data loss) | CRITICAL - data loss risk |
| **Integration tests** | `create` (ephemeral DB) | Test container provides fresh database | Low - isolated |
| **Switching environments** | `validate` | Database already exists in target | Low - safe |

### The Golden Rule

> **Use `create` only when the database is truly empty and has never had schema before. Use `validate` for all persistent environments and after the initial setup.**

### Anti-Pattern: The Restart Loop

This is what CAUSES restart loops:

```yaml
# WRONG - This causes "relation already exists" errors
environment:
  SPRING_JPA_HIBERNATE_DDL_AUTO: create  # Tries to create tables
  # Liquibase ALSO tries to create tables
  # First to run wins, second fails
  # Container restarts, tries again... infinite loop
```

### The Safe Pattern

This is what PREVENTS restart loops:

```yaml
# CORRECT - Two-phase initialization
environment:
  SPRING_JPA_HIBERNATE_DDL_AUTO: create  # ONE TIME: fresh database
  # After first successful startup:
  SPRING_JPA_HIBERNATE_DDL_AUTO: validate  # All subsequent startups
```

---

## Development Lifecycle Phases

### Phase 1: Initial Setup (First Time Only)

**When:** Setting up a brand new service or completely resetting database

**Configuration:**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create  # Create from scratch
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml
```

**What Happens:**
1. Service starts
2. Hibernate sees empty database, creates schema (via JPA entities)
3. Liquibase migrations table (`databasechangelog`) is created
4. Liquibase tracks all migrations as "already executed" (since schema exists)

**Duration:** Startup takes 10-30 seconds

**Next Step:** Immediately proceed to Phase 2

### Phase 2: Active Development (99% of the time)

**When:** Normal development, after initial setup

**Configuration:**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # VALIDATE - do not modify schema
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml
```

**What Happens:**
1. Service starts
2. Hibernate validates schema matches entities (fails if mismatch)
3. Liquibase executes any pending migrations
4. Service runs normally

**Duration:** Startup takes 5-15 seconds

**How Long It Lasts:** Until you need to reset the database completely (rare)

### Phase 3: Production (Forever)

**Configuration:**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # ALWAYS validate in production
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml
```

**Critical Rules:**
- ✅ Use Liquibase for ALL schema changes
- ✅ Test schema changes in staging first
- ✅ Plan rollbacks for every migration
- ❌ Never use `create` (causes data loss)
- ❌ Never use `update` (unpredictable behavior)

---

## Day-to-Day Development Workflows

### Workflow 1: Local Development (Most Common)

**Goal:** Add a new feature with database changes

**Steps:**

1. **Check current mode:**
   ```bash
   docker compose config | grep "HIBERNATE_DDL_AUTO"
   # Should see: SPRING_JPA_HIBERNATE_DDL_AUTO: validate
   ```

2. **Add JPA entity:**
   ```java
   @Entity
   @Table(name = "appointments")
   public class Appointment {
       @Id
       @GeneratedValue(strategy = GenerationType.UUID)
       private UUID id;

       @Column(name = "appointment_date")
       private LocalDate appointmentDate;
   }
   ```

3. **Create Liquibase migration:**
   ```xml
   <!-- src/main/resources/db/changelog/0005-create-appointments-table.xml -->
   <changeSet id="0005-create-appointments-table" author="your-name">
       <createTable tableName="appointments">
           <column name="id" type="UUID">
               <constraints primaryKey="true"/>
           </column>
           <column name="appointment_date" type="DATE"/>
       </createTable>
       <rollback>
           <dropTable tableName="appointments"/>
       </rollback>
   </changeSet>
   ```

4. **Add to master changelog:**
   ```xml
   <!-- db.changelog-master.xml -->
   <include file="db/changelog/0005-create-appointments-table.xml"/>
   ```

5. **Rebuild JAR and restart service:**
   ```bash
   cd backend
   ./gradlew :modules:services:YOUR-SERVICE:bootJar
   docker compose up --build YOUR-SERVICE
   ```

6. **Verify:**
   ```bash
   docker compose logs YOUR-SERVICE | grep "Liquibase"
   # Should see: "Update successful"
   ```

### Workflow 2: Complete Database Reset (When Needed)

**When:** You've corrupted the database and want a completely fresh start

**Steps:**

1. **Stop the service:**
   ```bash
   docker compose stop YOUR-SERVICE
   ```

2. **Drop the database:**
   ```bash
   docker exec healthdata-postgres psql -U healthdata -c "DROP DATABASE IF EXISTS YOUR_db;"
   docker exec healthdata-postgres psql -U healthdata -c "CREATE DATABASE YOUR_db;"
   ```

3. **Switch to `create` mode in docker-compose.override.yml:**
   ```yaml
   # docker-compose.override.yml (temporary, not committed)
   services:
     YOUR-SERVICE:
       environment:
         SPRING_JPA_HIBERNATE_DDL_AUTO: create
   ```

4. **Start the service:**
   ```bash
   docker compose up --build YOUR-SERVICE
   ```

5. **Wait for startup (watch logs):**
   ```bash
   docker compose logs -f YOUR-SERVICE | grep -E "ERROR|Started|Liquibase"
   ```

6. **Once running successfully, IMMEDIATELY switch back to `validate`:**
   ```bash
   # Remove the docker-compose.override.yml or change setting back
   docker compose stop YOUR-SERVICE
   # Edit docker-compose.yml to restore ddl-auto: validate
   git checkout docker-compose.yml  # If you modified it
   docker compose up --build YOUR-SERVICE
   ```

7. **Verify it stays running:**
   ```bash
   sleep 10 && docker compose ps | grep YOUR-SERVICE
   # Should show "Up ... seconds"
   ```

**Critical:** Do NOT leave in `create` mode. Always switch back to `validate`.

### Workflow 3: Testing Schema Migrations

**Goal:** Verify a new migration works correctly

**Steps:**

1. **Create integration test:**
   ```java
   @SpringBootTest
   @AutoConfigureMockMvc
   class DatabaseMigrationTest {

       @Autowired
       private TestEntityManager entityManager;

       @Test
       void shouldCreateAppointmentsTable() {
           // Test that migration executed and table exists
           Query query = entityManager.getEntityManager()
               .createNativeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_name='appointments'");
           Long count = ((Number) query.getSingleResult()).longValue();
           assertThat(count).isEqualTo(1);
       }

       @Test
       void shouldAllowInsertingAppointment() {
           Appointment apt = Appointment.builder()
               .appointmentDate(LocalDate.now())
               .build();
           entityManager.persistAndFlush(apt);
           entityManager.clear();

           // Query back to verify
           Appointment found = entityManager.find(Appointment.class, apt.getId());
           assertThat(found).isNotNull();
       }
   }
   ```

2. **Run tests:**
   ```bash
   ./gradlew :modules:services:YOUR-SERVICE:test
   ```

3. **Verify test output:**
   - Liquibase should execute migrations
   - Tests should pass
   - No "relation already exists" errors

### Workflow 4: Migrating Between Environments

**Goal:** Deploy schema changes from dev → staging → production

**Steps:**

1. **In dev environment:**
   - Create migration file
   - Add to master changelog
   - Test locally with `ddl-auto: validate`
   - Verify in logs: "Liquibase update successful"

2. **In staging environment:**
   - Pull latest code with migration
   - Deployment automatically runs migrations (Liquibase)
   - Verify: logs show all migrations executed
   - Test application functionality

3. **In production environment:**
   - Pull code with migration
   - Deployment runs migrations (Liquibase)
   - Monitor: logs should show "Liquibase update successful"
   - Verify: application services respond normally
   - Alert: watch for any "relation already exists" errors (indicates problem)

---

## Safe Database Resets & Switching Modes

### When You MUST Switch Modes

| Scenario | Switch From | Switch To | Why |
|----------|------------|----------|-----|
| Initial dev setup | N/A | `create` | Database empty, needs creation |
| First startup succeeds | `create` | `validate` | Schema now exists, Liquibase takes over |
| Local reset needed | `validate` | `create` | Need fresh schema for development |
| Production deployment | Test | `validate` | Production database already exists |
| Test environment setup | N/A | `create` | Test databases are ephemeral |

### Switching Safely: create → validate

This is the most critical transition:

**Before switching:**
1. Ensure service is running successfully
2. Check logs for "Successfully acquired change log lock" (Liquibase is managing schema)
3. Verify all entities load without validation errors

**The switch:**

```bash
# Option 1: Modify docker-compose.yml (if it's not already correct)
# Change: SPRING_JPA_HIBERNATE_DDL_AUTO: create
# To:     SPRING_JPA_HIBERNATE_DDL_AUTO: validate

# Option 2: Use docker-compose.override.yml to override
cat > docker-compose.override.yml << 'EOF'
services:
  YOUR-SERVICE:
    environment:
      SPRING_JPA_HIBERNATE_DDL_AUTO: validate
EOF

# Restart service
docker compose restart YOUR-SERVICE
```

**After switching:**
1. Check logs: should show "Liquibase: Update successful" (or "No pending migrations")
2. Service should start in < 20 seconds
3. No errors about missing tables or columns

**If it fails:**
- Check for "Schema-validation: wrong column type" errors
  - Means migration file has wrong type vs entity
  - Fix migration, rebuild, restart
- Check for "Schema-validation: missing column" errors
  - Means entity has column not in migration
  - Add migration to create column, rebuild, restart

### Switching Safely: validate → create (Nuclear Option)

Only do this when database is completely corrupted:

```bash
# Step 1: Stop service
docker compose stop YOUR-SERVICE

# Step 2: Completely remove database volume (DELETES ALL DATA)
docker compose down -v YOUR-SERVICE

# Step 3: Recreate empty database
docker exec healthdata-postgres psql -U healthdata -c "CREATE DATABASE YOUR_db;"

# Step 4: Modify docker-compose.yml to use create
# TEMPORARILY set: SPRING_JPA_HIBERNATE_DDL_AUTO: create

# Step 5: Start service
docker compose up YOUR-SERVICE

# Step 6: Wait for successful startup
# Watch logs: should see "Started [ServiceName]"

# Step 7: IMMEDIATELY modify docker-compose.yml back to validate
# Change: SPRING_JPA_HIBERNATE_DDL_AUTO: create
# To:     SPRING_JPA_HIBERNATE_DDL_AUTO: validate

# Step 8: Restart service
docker compose restart YOUR-SERVICE

# Step 9: Verify stable state
sleep 10 && docker compose ps | grep YOUR-SERVICE
```

**Recovery Checklist:**
- [ ] Service started successfully (Step 5)
- [ ] Switched back to `validate` (Step 7)
- [ ] Service still running after restart (Step 9)
- [ ] No "relation already exists" errors
- [ ] Application responding to requests

---

## Shared Tables Pattern (Auth Tables)

### The Challenge

Multiple Phase 5 services need to share authentication tables:
- `patient-event-service`
- `quality-measure-event-service`
- `care-gap-event-service`
- `clinical-workflow-event-service`

Each has its own database (`patient_db`, `quality_db`, etc.) but needs user authentication tables.

### The Solution: Liquibase Replication

Each service has an IDENTICAL `0002-create-auth-tables.xml` file:

```
patient-event-service/db/changelog/0002-create-auth-tables.xml
quality-measure-event-service/db/changelog/0002-create-auth-tables.xml
care-gap-event-service/db/changelog/0002-create-auth-tables.xml
clinical-workflow-event-service/db/changelog/0002-create-auth-tables.xml
```

Each migration:
- Has IDENTICAL `<changeSet id="0002-create-users-table">` block
- Creates the SAME table schema
- Runs independently in each service's database
- Results in parallel `users`, `user_tenants`, `user_roles` tables

### Why This Works

```
patient_db:           quality_db:           care_gap_db:
├── users             ├── users             ├── users
├── user_tenants      ├── user_tenants      ├── user_tenants
├── user_roles        ├── user_roles        ├── user_roles
└── databasechangelog └── databasechangelog └── databasechangelog
```

Each database:
- Has independent schema
- Owns its migration history
- Can evolve independently
- But maintains structural consistency

### Rules for Shared Tables

1. **Use Liquibase replication** - Copy entire changeset to other services
2. **Use identical changeSet IDs** - Ensures consistency across databases
3. **Test in isolation** - Each service can be tested independently
4. **Document the sharing** - Add comment in changeset:

```xml
<changeSet id="0002-create-users-table" author="platform-team">
    <comment>Create users table for authentication - REPLICATED across patient-event-service, quality-measure-event-service, care-gap-event-service, clinical-workflow-event-service</comment>
```

5. **Keep identical across services** - If you modify one, modify all
6. **No foreign keys between databases** - Each database is isolated

### Updating Shared Tables

If you need to modify authentication tables:

1. **Create new migration in ONE service:**
   ```xml
   <!-- 0003-add-mfa-to-users.xml -->
   <changeSet id="0003-add-mfa-to-users" author="your-name">
       <addColumn tableName="users">
           <column name="mfa_enabled" type="BOOLEAN" defaultValue="false"/>
       </addColumn>
   </changeSet>
   ```

2. **Copy to ALL other services:**
   ```bash
   cp backend/modules/services/patient-event-service/src/main/resources/db/changelog/0003-add-mfa-to-users.xml \
      backend/modules/services/quality-measure-event-service/src/main/resources/db/changelog/
   cp backend/modules/services/patient-event-service/src/main/resources/db/changelog/0003-add-mfa-to-users.xml \
      backend/modules/services/care-gap-event-service/src/main/resources/db/changelog/
   cp backend/modules/services/patient-event-service/src/main/resources/db/changelog/0003-add-mfa-to-users.xml \
      backend/modules/services/clinical-workflow-event-service/src/main/resources/db/changelog/
   ```

3. **Add to master changelog in all services:**
   ```xml
   <!-- In each service's db.changelog-master.xml -->
   <include file="db/changelog/0003-add-mfa-to-users.xml"/>
   ```

4. **Rebuild and test all services:**
   ```bash
   ./gradlew build
   docker compose up --build patient-event-service quality-measure-event-service care-gap-event-service clinical-workflow-event-service
   ```

5. **Verify in all databases:**
   ```bash
   docker exec healthdata-postgres psql -U healthdata -d patient_db -c "\\d users"
   docker exec healthdata-postgres psql -U healthdata -d quality_db -c "\\d users"
   docker exec healthdata-postgres psql -U healthdata -d care_gap_db -c "\\d users"
   docker exec healthdata-postgres psql -U healthdata -d clinical_workflow_db -c "\\d users"
   ```

---

## Liquibase Migration Strategies

### Strategy 1: Safe Add (Backward Compatible)

Use when adding new functionality that doesn't break existing code:

```xml
<changeSet id="0006-add-notification-settings" author="your-name">
    <comment>Add notification settings - backward compatible with default values</comment>

    <addColumn tableName="users">
        <column name="notification_email_enabled" type="BOOLEAN" defaultValue="true">
            <constraints nullable="false"/>
        </column>
        <column name="notification_frequency" type="VARCHAR(50)" defaultValue="DAILY">
            <constraints nullable="false"/>
        </column>
    </addColumn>

    <rollback>
        <dropColumn tableName="users" columnName="notification_email_enabled"/>
        <dropColumn tableName="users" columnName="notification_frequency"/>
    </rollback>
</changeSet>
```

**Why it works:**
- Default values allow existing rows to be populated
- No code changes required before applying migration
- Can rollback cleanly if needed

### Strategy 2: Safe Remove (Migration + Code Removal)

Use when removing fields that are no longer used:

**Phase 1 - Deprecation (1-2 releases):**
```java
// Keep the field, but stop using it in code
@Column(name = "legacy_field")
@Deprecated  // Mark as deprecated
private String legacyField;
```

**Phase 2 - Migration + Code Removal:**
```xml
<changeSet id="0007-remove-legacy-field" author="your-name">
    <comment>Remove deprecated legacy_field - safe after 2+ releases</comment>
    <dropColumn tableName="users" columnName="legacy_field"/>
    <rollback>
        <addColumn tableName="users">
            <column name="legacy_field" type="VARCHAR(255)"/>
        </addColumn>
    </rollback>
</changeSet>
```

**And remove from entity:**
```java
// Remove @Column and field entirely
```

### Strategy 3: Safe Rename (Two-Step Process)

Use when renaming columns to avoid downtime:

**Step 1: Add new column, keep old**
```xml
<changeSet id="0008-rename-field-step1" author="your-name">
    <comment>Add new column for field rename</comment>
    <addColumn tableName="users">
        <column name="full_name" type="VARCHAR(200)"/>
    </addColumn>

    <!-- Copy existing data -->
    <sql>UPDATE users SET full_name = (first_name || ' ' || last_name);</sql>
</changeSet>
```

**Step 2: Remove old column (after 1+ releases)**
```xml
<changeSet id="0009-rename-field-step2" author="your-name">
    <comment>Remove old columns after rename</comment>
    <dropColumn tableName="users" columnName="first_name"/>
    <dropColumn tableName="users" columnName="last_name"/>
</changeSet>
```

---

## Testing & Validation

### Local Entity-Migration Validation

Every service has validation tests to ensure entities match migrations:

```bash
# Run validation test for a service
./gradlew :modules:services:YOUR-SERVICE:test --tests "*EntityMigrationValidationTest"
```

**What it checks:**
- All @Entity classes have corresponding tables
- All @Column annotations have corresponding columns
- Column types match (VARCHAR in DB = String in Java)
- No orphaned entities or migrations

### Integration Tests with Fresh Database

```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PatientServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldPersistAndRetrievePatient() throws Exception {
        // Test uses fresh database from testcontainer
        // Migrations run automatically
        // Schema validated against entities

        mockMvc.perform(post("/api/v1/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{...}"))
            .andExpect(status().isCreated());
    }
}
```

### Manual Testing Checklist

Before committing a new migration:

- [ ] Liquibase validation passes (grep for errors in logs)
- [ ] Migration executes successfully (no "Already marked" or duplicate errors)
- [ ] Service starts within 30 seconds
- [ ] Entity validation passes (no schema mismatch errors)
- [ ] Application endpoints respond correctly
- [ ] Rollback migration is defined and correct
- [ ] No hardcoded values (use defaults or data migration)

---

## Troubleshooting Guide

### Problem: "relation 'users' already exists"

**Root Cause:** Hibernate created schema BEFORE Liquibase ran migrations

**Solution 1 (Quick):** Switch to `validate` mode
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Change from 'create'
```

**Solution 2 (Complete Reset):**
```bash
# Stop service
docker compose stop YOUR-SERVICE

# Drop database completely
docker exec healthdata-postgres psql -U healthdata \
  -c "DROP DATABASE IF EXISTS your_db; CREATE DATABASE your_db;"

# Start with fresh database
docker compose up --build YOUR-SERVICE
```

### Problem: "Schema-validation: missing column X"

**Root Cause:** Entity has @Column that migration doesn't create

**Solution:**
1. Check entity for new @Column annotations
2. Create migration to add the column:
```xml
<changeSet id="000X-add-missing-column" author="your-name">
    <addColumn tableName="table_name">
        <column name="missing_column" type="VARCHAR(255)"/>
    </addColumn>
</changeSet>
```
3. Add to master changelog
4. Rebuild JAR: `./gradlew :modules:services:YOUR-SERVICE:bootJar`
5. Restart: `docker compose up --build YOUR-SERVICE`

### Problem: "wrong column type encountered"

**Root Cause:** Entity type doesn't match database column type

**Solution:**
1. Identify the column (error message shows it)
2. Either:
   - **Option A:** Fix the entity's @Column type
   - **Option B:** Create migration to change column type

**If changing column type:**
```xml
<changeSet id="000X-change-column-type" author="your-name">
    <modifyDataType tableName="table_name" columnName="column_name" newDataType="TEXT"/>
</changeSet>
```

### Problem: "Liquibase lock could not be acquired"

**Root Cause:** Previous migration is stuck, orphaned lock exists

**Solution:**
```bash
# Check locked state
docker exec healthdata-postgres psql -U healthdata -d your_db \
  -c "SELECT * FROM databasechangeloglock;"

# Clear lock (ONLY if previous migration process is dead)
docker exec healthdata-postgres psql -U healthdata -d your_db \
  -c "DELETE FROM databasechangeloglock WHERE ID=1; UPDATE databasechangeloglock SET LOCKED=false WHERE ID=1;"

# Restart service
docker compose restart YOUR-SERVICE
```

### Problem: Service keeps restarting (infinite loop)

**Root Cause:** Either `ddl-auto: create` with persistent database OR Liquibase migration failing

**Diagnosis:**
```bash
# Check logs for the first error
docker compose logs YOUR-SERVICE | head -50 | grep -E "ERROR|Exception|failed"

# Is it "relation already exists"?
# → Fix: Switch to ddl-auto: validate

# Is it something else?
# → Check migration syntax (XML validation)
# → Check for constraint violations
# → Check for missing foreign key targets
```

**Recovery:**
1. Identify the root cause (see logs)
2. If XML validation error:
   - Fix the XML file
   - Rebuild: `./gradlew :modules:services:YOUR-SERVICE:bootJar --no-cache`
   - Restart: `docker compose up --build YOUR-SERVICE`
3. If database error:
   - Clean database (see previous section)
   - Reset to `create` for one startup
   - Switch back to `validate`

### Problem: Different tables in different databases

**Root Cause:** Services didn't all get the same migration (shared tables issue)

**Solution:**
1. Identify which service is missing the table:
```bash
docker exec healthdata-postgres psql -U healthdata -d patient_db \
  -c "SELECT table_name FROM information_schema.tables WHERE table_schema='public' ORDER BY table_name;"
```

2. Find the migration file in the service that HAS the table
3. Copy it to the service missing the table:
```bash
cp src/main/resources/db/changelog/XXXX-table-name.xml \
   ../other-service/src/main/resources/db/changelog/
```

4. Add to other service's master changelog
5. Rebuild and restart both services

---

## Command Reference

### Docker Commands

```bash
# Start all services
docker compose up -d

# Start specific service
docker compose up -d YOUR-SERVICE

# Rebuild service image
docker compose up -d --build YOUR-SERVICE

# View service logs (last 50 lines)
docker compose logs --tail=50 YOUR-SERVICE

# Follow logs in real-time
docker compose logs -f YOUR-SERVICE

# Check service health
docker compose ps YOUR-SERVICE

# Stop service gracefully
docker compose stop YOUR-SERVICE

# Restart service
docker compose restart YOUR-SERVICE

# Remove containers (keeps volumes)
docker compose down

# Remove containers and volumes (DELETES DATA)
docker compose down -v

# Check running services
docker compose ps
```

### Database Commands

```bash
# Connect to PostgreSQL
docker exec -it healthdata-postgres psql -U healthdata

# List all databases
docker exec healthdata-postgres psql -U healthdata -c "\l"

# Connect to specific database
docker exec healthdata-postgres psql -U healthdata -d your_db

# List tables in database
docker exec healthdata-postgres psql -U healthdata -d your_db -c "\dt"

# Describe table
docker exec healthdata-postgres psql -U healthdata -d your_db -c "\d table_name"

# Drop database completely
docker exec healthdata-postgres psql -U healthdata \
  -c "DROP DATABASE IF EXISTS your_db;"

# Recreate empty database
docker exec healthdata-postgres psql -U healthdata \
  -c "CREATE DATABASE your_db; GRANT ALL ON DATABASE your_db TO healthdata;"

# Check Liquibase status
docker exec healthdata-postgres psql -U healthdata -d your_db \
  -c "SELECT id, filename FROM databasechangelog ORDER BY orderexecuted;"

# View Liquibase lock state
docker exec healthdata-postgres psql -U healthdata -d your_db \
  -c "SELECT * FROM databasechangeloglock;"
```

### Gradle Commands

```bash
# Build specific service JAR
./gradlew :modules:services:YOUR-SERVICE:bootJar

# Build without running tests
./gradlew :modules:services:YOUR-SERVICE:bootJar -x test

# Run unit tests
./gradlew :modules:services:YOUR-SERVICE:test

# Run specific test
./gradlew :modules:services:YOUR-SERVICE:test --tests "*MyTestName"

# Run entity-migration validation
./gradlew :modules:services:YOUR-SERVICE:test --tests "*EntityMigrationValidationTest"

# Clean build artifacts
./gradlew :modules:services:YOUR-SERVICE:clean

# Full build with tests
./gradlew :modules:services:YOUR-SERVICE:build

# Build all services
./gradlew build
```

### Liquibase Commands (via Logs)

```bash
# Watch Liquibase execution
docker compose logs -f YOUR-SERVICE | grep -i liquibase

# See all migrations executed
docker exec healthdata-postgres psql -U healthdata -d your_db \
  -c "SELECT id, filename, orderexecuted FROM databasechangelog ORDER BY orderexecuted;"

# Count pending migrations
docker exec healthdata-postgres psql -U healthdata -d your_db \
  -c "SELECT COUNT(*) FROM databasechangelog;"
```

### Emergency Scripts

```bash
# Clear Liquibase lock (use ONLY when migration process is dead)
docker exec healthdata-postgres psql -U healthdata -d your_db -c \
  "UPDATE databasechangeloglock SET LOCKED=false WHERE ID=1;"

# Reset entire database (DANGEROUS - deletes all data)
docker exec healthdata-postgres psql -U healthdata -c \
  "DROP DATABASE IF EXISTS your_db; CREATE DATABASE your_db; GRANT ALL ON DATABASE your_db TO healthdata;"

# Check Docker disk usage
docker system df

# Prune unused images/volumes (frees space)
docker system prune -a -v
```

---

## Quick Reference: Common Scenarios

### Scenario 1: "I added a field to an entity, now service won't start"

1. Create migration file (next sequential number)
2. Use `<addColumn>` to add the field
3. Add to master changelog
4. Rebuild: `./gradlew :modules:services:YOUR-SERVICE:bootJar`
5. Restart: `docker compose up --build YOUR-SERVICE`

### Scenario 2: "I need to reset my local database completely"

1. `docker compose stop YOUR-SERVICE`
2. `docker exec healthdata-postgres psql -U healthdata -c "DROP DATABASE IF EXISTS your_db; CREATE DATABASE your_db;"`
3. Update docker-compose to temporarily use `ddl-auto: create`
4. `docker compose up --build YOUR-SERVICE`
5. Once running: switch back to `ddl-auto: validate`
6. `docker compose restart YOUR-SERVICE`

### Scenario 3: "Service keeps restarting with 'relation already exists'"

1. Check: is `ddl-auto` set to `create` in docker-compose.yml?
2. Change it to `validate`
3. Clean database: Drop and recreate empty database
4. Restart service with `validate` mode
5. Verify: should start and stay running

### Scenario 4: "Multiple services share a table, how do I update it?"

1. Create/modify migration in ONE service
2. Copy migration file to ALL other services
3. Add to master changelog in ALL services
4. Rebuild all services
5. Restart all services together
6. Verify table schema matches in all databases

### Scenario 5: "How do I deploy a schema change to production?"

1. Test migration locally with `ddl-auto: validate`
2. Verify service runs without errors
3. Commit migration and code changes
4. Production deployment pulls code with migration
5. Service starts with migration in progress
6. Liquibase executes pending migrations automatically
7. Monitor logs for "Successfully updated database" message

---

## Summary: The Three Critical Rules

1. **Fresh database = `create` ONE TIME ONLY**
   - Use when database is empty and has never had schema
   - Immediately switch to `validate` after successful startup

2. **Normal development = `validate` ALWAYS**
   - Use for all persistent environments
   - Liquibase handles schema changes
   - Database schema survives container restarts

3. **Production = `validate` FOREVER**
   - Never use `create` (causes data loss)
   - All schema changes via Liquibase migrations
   - Plan and test rollbacks for every migration

---

## Integration with CLAUDE.md

This guide is referenced in `CLAUDE.md` under "Database Migration Standards" section. For questions about:
- Database architecture: See `DATABASE_ARCHITECTURE_MIGRATION_PLAN.md`
- Entity-migration sync: See `ENTITY_MIGRATION_GUIDE.md`
- Production deployments: See `DEPLOYMENT_RUNBOOK.md`
- Distributed tracing: See `DISTRIBUTED_TRACING_GUIDE.md`

---

_Last Updated: January 2026_
_Maintained by: Platform Engineering Team_
_Status: Primary Reference for Liquibase Development_
