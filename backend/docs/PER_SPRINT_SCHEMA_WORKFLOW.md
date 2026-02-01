# Per-Sprint Schema Generation Workflow

**Status**: ⚠️ SUPERSEDED - See RELEASE_DRIVEN_SCHEMA_WORKFLOW.md
**Effective Date**: 2026-01-21 (pilot concept)
**Superseded By**: RELEASE_DRIVEN_SCHEMA_WORKFLOW.md (2026-01-21)
**Reason**: Release-driven approach provides better production stability (1 migration per release vs. 3-5 per release)

> **Note**: This document is preserved for historical reference. The team approved
> the release-driven workflow instead, which batches entity changes across multiple
> sprints into a single migration at release time. See RELEASE_DRIVEN_SCHEMA_WORKFLOW.md
> for the current approved workflow.

---

## Executive Summary

This workflow eliminates entity-migration synchronization issues by **generating database schemas at sprint boundaries** instead of manually creating migrations during development.

**Key Benefits:**
- ✅ **Zero entity-migration drift**: Schema generated from actual entity code
- ✅ **Faster development**: No migration creation during feature development
- ✅ **Better code reviews**: Schema + code reviewed together
- ✅ **Clear audit trail**: One migration per sprint = one coherent feature set
- ✅ **Reduced errors**: Automated generation prevents human mistakes (like forgetting `created_by` column)

---

## Workflow Overview

```
┌─────────────┐
│  Sprint     │
│  Planning   │
└──────┬──────┘
       │
       ▼
┌──────────────────────────────────────┐
│  DEVELOPMENT PHASE (Days 1-9)       │
│  ════════════════════════════════   │
│  • Code features with H2 database   │
│  • Entity changes auto-generate     │
│    schema in H2                     │
│  • Tests run against H2 (fast!)     │
│  • No manual migrations required    │
└──────┬───────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│  SPRINT END (Day 10)                 │
│  ════════════════════════════════   │
│  1. Run sprint-schema-export.sh     │
│  2. Review generated migration      │
│  3. Test against PostgreSQL         │
│  4. Commit schema + code together   │
└──────┬───────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│  PRODUCTION DEPLOYMENT               │
│  ════════════════════════════════   │
│  • Liquibase applies migration      │
│  • Schema validated against entities│
│  • No surprises - already tested    │
└──────────────────────────────────────┘
```

---

## Phase 1: Development (Days 1-9 of Sprint)

### Developer Configuration

**Create `application-dev.yml` for local development:**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:devdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: update          # ← Auto-generates schema from entities
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
  liquibase:
    enabled: false              # ← Disable migrations during development

  h2:
    console:
      enabled: true             # ← Enable H2 console at /h2-console
```

### Running Tests During Development

```bash
# Unit tests (no database)
./gradlew test --tests "*ServiceTest"

# Integration tests (H2 auto-schema)
./gradlew test --tests "*IntegrationTest" -Dspring.profiles.active=dev

# Fast feedback loop (tests run in seconds)
```

### Adding New Entities

```java
// 1. Create entity (NO migration file yet!)
@Entity
@Table(name = "care_team_members")
public class CareTeamMemberEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "role", nullable = false, length = 50)
    private String role;  // PRIMARY, SPECIALIST, COORDINATOR

    // H2 will auto-create this table when you run tests!
}

// 2. Write tests
@Test
void shouldAssignCareTeamMember() {
    // Test works immediately - no migration needed!
}

// 3. Develop feature
// 4. At sprint end: Generate migration from entities
```

---

## Phase 2: Sprint End - Schema Export (Day 10)

### Step 1: Run Schema Export Tool

```bash
cd backend
./scripts/sprint-schema-export.sh quality-measure-service sprint-24
```

**This script:**
1. Exports current PostgreSQL schema
2. Generates H2 schema from entities
3. Compares schemas using Liquibase diff
4. Creates migration file (e.g., `0046-sprint-24-schema-changes.xml`)
5. Adds migration to master changelog

### Step 2: Review Generated Migration

```bash
# Review the migration
cat modules/services/quality-measure-service/src/main/resources/db/changelog/0046-sprint-24-schema-changes.xml
```

**Check for:**
- ✅ All new tables created
- ✅ All new columns added
- ✅ Correct data types (DOUBLE PRECISION vs NUMERIC)
- ✅ Indexes on foreign keys
- ✅ NOT NULL constraints match entity
- ⚠️ **Manual adjustments needed**:
  - Add comments for documentation
  - Optimize index names
  - Add CHECK constraints
  - Improve rollback logic

### Step 3: Manual Refinement (Optional but Recommended)

```xml
<!-- BEFORE (auto-generated) -->
<createTable tableName="care_team_members">
    <column name="role" type="VARCHAR(50)"/>
</createTable>

<!-- AFTER (manually refined) -->
<createTable tableName="care_team_members">
    <column name="role" type="VARCHAR(50)">
        <constraints nullable="false"/>
    </column>
</createTable>

<!-- Add CHECK constraint -->
<sql>
    ALTER TABLE care_team_members
    ADD CONSTRAINT chk_care_team_role
    CHECK (role IN ('PRIMARY', 'SPECIALIST', 'COORDINATOR'));

    COMMENT ON TABLE care_team_members IS
        'Tracks care team membership for coordinated patient care';
</sql>
```

### Step 4: Validate Migration

```bash
# Test migration against fresh PostgreSQL database
docker compose down postgres
docker compose up -d postgres
sleep 5

# Create test database
docker exec healthdata-postgres psql -U healthdata -c "CREATE DATABASE quality_db_test;"

# Run entity-migration validation
./gradlew :modules:services:quality-measure-service:test \
    --tests "*EntityMigrationValidationTest" \
    -Dspring.datasource.url=jdbc:postgresql://localhost:5435/quality_db_test

# Expected: PASS - entities match migrations
```

### Step 5: Commit Schema + Code Together

```bash
# Stage all sprint changes
git add modules/services/quality-measure-service/src/main/java/.../*.java
git add modules/services/quality-measure-service/src/main/resources/db/changelog/0046-sprint-24-schema-changes.xml
git add modules/services/quality-measure-service/src/main/resources/db/changelog/db.changelog-master.xml

# Single atomic commit for sprint
git commit -m "feat(sprint-24): Care team assignment feature with schema migration

Features:
- Add CareTeamMemberEntity for multi-provider coordination
- Implement care team assignment API
- Add care team member search and filtering
- Automated notifications to care team members

Database Changes (migration 0046):
- Create care_team_members table
- Add indexes on patient_id, provider_id, tenant_id
- Add CHECK constraint for role validation
- Full audit trail (created_by, created_at, updated_at)

Tests: 42 new tests (100% coverage)
"
```

---

## Phase 3: Production Deployment

### Pre-Deployment Validation

```bash
# Run full test suite against PostgreSQL (not H2!)
./gradlew :modules:services:quality-measure-service:test \
    -Dspring.profiles.active=test

# Verify all migrations have rollback
./scripts/verify-rollback-coverage.sh quality-measure-service

# Check migration order
./scripts/validate-migration-sequence.sh quality-measure-service
```

### Deployment

```bash
# Liquibase runs migrations automatically on service startup
docker compose up -d quality-measure-service

# Verify migration applied
docker exec healthdata-postgres psql -U healthdata -d quality_db \
    -c "SELECT id, author, dateexecuted FROM databasechangelog WHERE id LIKE '%sprint-24%';"
```

---

## Configuration by Environment

### Development (`application-dev.yml`)
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update          # Auto-generate from entities
  liquibase:
    enabled: false              # No migrations in dev
  datasource:
    url: jdbc:h2:mem:devdb      # In-memory H2
```

### Testing (`application-test.yml`)
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate        # Validate entities match migrations
  liquibase:
    enabled: true               # Run migrations
  datasource:
    url: jdbc:postgresql://localhost:5435/SERVICE_db
```

### Production (`application.yml`)
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate        # READ-ONLY validation
  liquibase:
    enabled: true               # Run migrations ONLY
  datasource:
    url: jdbc:postgresql://prod-db:5432/SERVICE_db
```

---

## Troubleshooting

### Problem: Schema export script fails

**Error**: `Failed to generate H2 schema`

**Solution**:
```bash
# Manually generate schema using Hibernate
./gradlew :modules:services:quality-measure-service:bootJar
java -jar modules/services/quality-measure-service/build/libs/*.jar \
    --spring.profiles.active=schema-export \
    --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create \
    --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=/tmp/schema.sql
```

### Problem: Liquibase diff generates too many changes

**Error**: Migration includes 50+ changes for a simple feature

**Cause**: Entities changed significantly during sprint

**Solution**:
```bash
# Review each change carefully
# Split into multiple migrations if needed:
# - 0046-sprint-24-core-schema.xml (new tables)
# - 0047-sprint-24-indexes.xml (performance)
# - 0048-sprint-24-constraints.xml (data integrity)
```

### Problem: Entity-migration validation fails after applying migration

**Error**: `Schema-validation: wrong column type`

**Solution**:
```bash
# Check data type mapping
# Entity: Double → PostgreSQL: DOUBLE PRECISION (NOT NUMERIC)
# Entity: String → PostgreSQL: TEXT (NOT VARCHAR with default length)

# Fix migration manually:
<modifyDataType tableName="scores" columnName="value" newDataType="DOUBLE PRECISION"/>
```

---

## Metrics & Success Criteria

### Before Per-Sprint Workflow (Current State)
- **Fix migration rate**: 15.4% (83 out of 537 migrations are corrections)
- **Entity-migration mismatches**: 2-3 per sprint
- **Time to create migration**: 1-2 hours (manual translation)
- **Production schema incidents**: 2-3 per year

### After Per-Sprint Workflow (Target)
- **Fix migration rate**: <2% (mostly refinements)
- **Entity-migration mismatches**: 0 (automated generation prevents drift)
- **Time to create migration**: 15 minutes (automated + review)
- **Production schema incidents**: 0 per year

---

## Rollout Plan

### Week 1: Pilot (quality-measure-service)
- ✅ Create sprint-schema-export.sh script
- ✅ Document workflow
- Test on quality-measure-service
- Gather feedback from team

### Week 2: Refinement
- Improve script based on feedback
- Add error handling
- Create validation helpers
- Update CI/CD pipeline

### Week 3-4: Rollout (5 High-Activity Services)
- fhir-service
- patient-service
- care-gap-service
- analytics-service
- hcc-service

### Weeks 5-8: Organization-Wide
- Remaining 30+ services adopt on their own schedule
- Create training materials
- Update CLAUDE.md

---

## FAQs

### Q: What if I need to create a migration mid-sprint for a hotfix?

**A**: Create manual migration as before. At sprint end, schema-export will detect it already exists and skip that change.

### Q: Can I still use manual Liquibase migrations for complex schema changes?

**A**: YES! This workflow is for standard entity-driven changes. For complex data migrations, stored procedures, or performance optimizations, manual migrations are still recommended.

### Q: How does this work with feature branches?

**A**:
1. Develop feature in branch with H2
2. Merge to main without migration
3. At sprint end, generate ONE migration for ALL merged features
4. This batches related changes into coherent sprint migration

### Q: What about database-specific features (PostgreSQL extensions, etc.)?

**A**: Add them manually to the generated migration during review step:
```xml
<!-- Auto-generated table -->
<createTable tableName="search_index">
    ...
</createTable>

<!-- Manually add PostgreSQL-specific index -->
<sql>
    CREATE INDEX idx_search_tsvector ON search_index USING GIN(search_vector);
</sql>
```

---

## Related Documentation

- [LIQUIBASE_DEVELOPMENT_WORKFLOW.md](./LIQUIBASE_DEVELOPMENT_WORKFLOW.md) - Legacy manual workflow (being replaced)
- [ENTITY_MIGRATION_GUIDE.md](./ENTITY_MIGRATION_GUIDE.md) - Entity-migration synchronization best practices
- [DATABASE_ARCHITECTURE_GUIDE.md](./DATABASE_ARCHITECTURE_GUIDE.md) - Multi-tenant database architecture
- [CLAUDE.md](../../CLAUDE.md) - Project quick reference

---

**Approved By**: Development Team
**Last Updated**: 2026-01-21
**Version**: 1.0 - Initial Implementation
