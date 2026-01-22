# Release-Driven Schema Workflow

**Status**: ✅ APPROVED - Official workflow for HDIM v1.3.0+
**Effective Date**: 2026-01-21
**Supersedes**: Per-Sprint Schema Workflow (pilot concept)

---

## Executive Summary

**Key Principle**: Data model is **versioned at release boundaries**, not per-commit or per-sprint.

**Workflow:**
1. **Sprint Start**: Begin with clean H2 development database
2. **Sprint Development**: Code freely with auto-generated schema (H2)
3. **Sprint End**: Feature complete, but NO schema migration yet
4. **Release Preparation**: Generate ONE schema migration for entire release
5. **Release Deployment**: Apply migration + deploy code atomically
6. **Post-Release**: Version data model, ready for next sprint

**Benefits:**
- ✅ **Fewer migrations**: 1 per release vs. 10+ per sprint
- ✅ **Production stability**: Schema changes only with tested releases
- ✅ **Atomic deployment**: Code + schema versioned together
- ✅ **Clear audit trail**: Data model version = release version
- ✅ **Rollback safety**: Can rollback entire release including schema

---

## Workflow Phases

### Phase 1: Sprint Start

**Trigger**: New sprint begins (e.g., Sprint 24)

**Actions:**
```bash
# 1. Verify current release baseline
git checkout main
git pull origin main

# 2. Check current data model version
ls -l modules/services/quality-measure-service/src/main/resources/db/changelog/
# Last migration should be from previous release (e.g., 0045-release-v1.2.0.xml)

# 3. Create feature branch
git checkout -b feature/sprint-24-care-team-assignment

# 4. Configure development environment (H2)
cp backend/config/application-dev-template.yml backend/config/application-dev.yml
```

**Developer Environment (`application-dev.yml`):**
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
    enabled: false              # ← No migrations during development

  h2:
    console:
      enabled: true
      path: /h2-console
```

**Status**: ✅ Ready for development with clean slate

---

### Phase 2: Sprint Development (Days 1-9)

**Development Loop:**
```
Code Feature → H2 Auto-Generates Schema → Write Tests → Tests Pass → Iterate
```

**Example: Adding Care Team Feature**
```java
// 1. Create entity (NO migration file!)
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
    private String role;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;  // ← H2 creates this column automatically

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}

// 2. Write service
@Service
public class CareTeamService {
    public CareTeamMemberEntity addTeamMember(...) {
        // Business logic
    }
}

// 3. Write tests (H2 schema auto-created!)
@SpringBootTest
@ActiveProfiles("dev")
class CareTeamServiceTest {
    @Test
    void shouldAddTeamMember() {
        // Test passes immediately - H2 has the schema!
    }
}
```

**During Development:**
- ✅ Entities created/modified freely
- ✅ H2 auto-updates schema
- ✅ Tests run in seconds (no Docker startup)
- ❌ NO migration files created yet
- ❌ NO PostgreSQL testing yet

**Commits During Sprint:**
```bash
git add src/main/java/.../CareTeamMemberEntity.java
git add src/main/java/.../CareTeamService.java
git add src/test/java/.../CareTeamServiceTest.java
git commit -m "feat: Add care team member assignment (WIP - schema migration pending release)"
```

**Note**: Commits do NOT include migration files during development.

---

### Phase 3: Sprint End - Feature Complete

**Trigger**: Sprint review day (Day 10)

**Actions:**
1. ✅ All features complete
2. ✅ All tests passing with H2
3. ✅ Code review complete
4. ✅ Merge to main (WITHOUT migration)

```bash
# Merge feature branch
git checkout main
git merge feature/sprint-24-care-team-assignment

# Push to main
git push origin main
```

**Status**: Features in `main` branch, but schema migration NOT yet created.

**Important**: Main branch has entity changes but no corresponding migration. This is INTENTIONAL and TEMPORARY - migration will be created at release time.

---

### Phase 4: Release Preparation

**Trigger**: Multiple sprints complete, ready for release (e.g., v1.3.0)

**Example Scenario:**
- Sprint 23: Patient health scoring feature
- Sprint 24: Care team assignment feature
- Sprint 25: Advanced reporting feature
- **Decision**: Release v1.3.0 including all three features

**Actions:**

#### Step 1: Identify Release Scope
```bash
# Review entity changes since last release
git log --oneline v1.2.0..HEAD -- "**/entity/**/*.java" "**/persistence/**/*.java"

# Example output:
# a1b2c3d feat: Add care team member entity
# d4e5f6g feat: Add health score entity
# g7h8i9j feat: Add advanced report entity
```

#### Step 2: Generate Release Schema Migration
```bash
cd backend

# Run release schema export tool
./scripts/release-schema-export.sh v1.3.0 quality-measure-service

# This script:
# 1. Starts service with H2 (generates schema from all entities since v1.2.0)
# 2. Exports current production PostgreSQL schema
# 3. Generates diff: "what changed since v1.2.0?"
# 4. Creates migration: 0046-release-v1.3.0.xml
```

**Generated Migration File** (`0046-release-v1.3.0.xml`):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog">

    <!--
        Release v1.3.0 - Data Model Changes

        This migration includes schema changes from:
        - Sprint 23: Patient health scoring (health_scores table)
        - Sprint 24: Care team assignment (care_team_members table)
        - Sprint 25: Advanced reporting (report_templates table)

        Total entities added: 3
        Total columns added: 47
        Total indexes added: 12
    -->

    <changeSet id="0046-release-v1.3.0-health-scores" author="release-v1.3.0">
        <createTable tableName="health_scores">
            <column name="id" type="UUID" defaultValueComputed="gen_random_uuid()">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="tenant_id" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="patient_id" type="UUID">
                <constraints nullable="false"/>
            </column>
            <column name="overall_score" type="DOUBLE PRECISION">
                <constraints nullable="false"/>
            </column>
            <column name="created_by" type="UUID">
                <constraints nullable="false"/>
            </column>
            <column name="created_at" type="TIMESTAMP WITH TIME ZONE">
                <constraints nullable="false"/>
            </column>
            <!-- ... more columns ... -->
        </createTable>

        <createIndex indexName="idx_health_scores_patient" tableName="health_scores">
            <column name="patient_id"/>
        </createIndex>
    </changeSet>

    <changeSet id="0046-release-v1.3.0-care-team-members" author="release-v1.3.0">
        <createTable tableName="care_team_members">
            <!-- ... care team schema ... -->
        </createTable>
    </changeSet>

    <changeSet id="0046-release-v1.3.0-report-templates" author="release-v1.3.0">
        <createTable tableName="report_templates">
            <!-- ... reporting schema ... -->
        </createTable>
    </changeSet>

    <rollback>
        <dropTable tableName="report_templates"/>
        <dropTable tableName="care_team_members"/>
        <dropTable tableName="health_scores"/>
    </rollback>

</databaseChangeLog>
```

#### Step 3: Review & Refine Migration
```bash
# Human review checklist:
# ✓ All new entities included
# ✓ Column types correct (DOUBLE PRECISION not NUMERIC!)
# ✓ NOT NULL constraints match entities
# ✓ Indexes on foreign keys
# ✓ Rollback script complete
# ✓ Comments for documentation
```

#### Step 4: Test Migration
```bash
# Test against fresh PostgreSQL database
docker compose down postgres
docker compose up -d postgres
sleep 5

# Create test database
docker exec healthdata-postgres psql -U healthdata -c "CREATE DATABASE quality_db_test;"

# Apply all migrations including new release migration
./gradlew :modules:services:quality-measure-service:test \
    --tests "*EntityMigrationValidationTest" \
    -Dspring.datasource.url=jdbc:postgresql://localhost:5435/quality_db_test \
    -Dspring.profiles.active=test

# Expected: PASS ✅
```

#### Step 5: Commit Release Migration
```bash
# Add migration to repository
git add modules/services/quality-measure-service/src/main/resources/db/changelog/0046-release-v1.3.0.xml
git add modules/services/quality-measure-service/src/main/resources/db/changelog/db.changelog-master.xml

# Commit with release message
git commit -m "chore(release): Data model v1.3.0 schema migration

Release v1.3.0 schema changes:
- Sprint 23: health_scores table (patient health scoring)
- Sprint 24: care_team_members table (care coordination)
- Sprint 25: report_templates table (advanced reporting)

Migration: 0046-release-v1.3.0.xml
Entities: 3 new tables, 47 columns, 12 indexes
Validated: EntityMigrationValidationTest PASSED
"

# Create release tag
git tag -a v1.3.0 -m "Release v1.3.0

Features:
- Patient health scoring system
- Care team assignment and coordination
- Advanced reporting and analytics

Data Model: v1.3.0 (migration 0046)
"

git push origin main --tags
```

---

### Phase 5: Release Deployment

**Trigger**: Release tag created (v1.3.0)

**Pre-Deployment Checklist:**
```bash
# 1. Verify all tests pass
./gradlew :modules:services:quality-measure-service:test

# 2. Verify migration rollback works
./gradlew :modules:services:quality-measure-service:liquibaseRollbackCount -PliquibaseCommandValue=1

# 3. Generate deployment plan
./scripts/generate-deployment-plan.sh v1.3.0

# 4. Backup production database
./scripts/backup-production-db.sh quality_db
```

**Deployment Steps:**
```bash
# 1. Put service in maintenance mode (optional)
kubectl scale deployment quality-measure-service --replicas=0

# 2. Deploy new version (Liquibase runs migrations automatically)
kubectl apply -f k8s/quality-measure-service-v1.3.0.yaml

# 3. Verify migration applied
kubectl logs -f quality-measure-service-xxx | grep "Liquibase"
# Expected: "Successfully applied 1 changeSet(s)"

# 4. Verify schema validation
kubectl logs -f quality-measure-service-xxx | grep "Schema-validation"
# Expected: NO errors

# 5. Run smoke tests
./scripts/smoke-test.sh quality-measure-service v1.3.0

# 6. Scale to production replicas
kubectl scale deployment quality-measure-service --replicas=3

# 7. Monitor for errors
kubectl logs -f quality-measure-service-xxx --tail=100
```

**Post-Deployment Verification:**
```bash
# Check migration in database
docker exec healthdata-postgres psql -U healthdata -d quality_db -c \
    "SELECT id, author, dateexecuted FROM databasechangelog WHERE id LIKE '%v1.3.0%';"

# Verify tables exist
docker exec healthdata-postgres psql -U healthdata -d quality_db -c \
    "SELECT tablename FROM pg_tables WHERE tablename IN ('health_scores', 'care_team_members', 'report_templates');"

# Expected: All 3 tables present ✅
```

---

### Phase 6: Post-Release - Data Model Versioning

**Trigger**: Release deployed successfully

**Actions:**

#### Step 1: Document Data Model Version
```bash
# Create data model documentation
./scripts/generate-data-model-docs.sh v1.3.0 quality-measure-service

# Generates:
# - docs/data-model/v1.3.0/quality-measure-service-schema.sql
# - docs/data-model/v1.3.0/quality-measure-service-erd.png
# - docs/data-model/v1.3.0/CHANGELOG.md
```

**Example `docs/data-model/v1.3.0/CHANGELOG.md`:**
```markdown
# Data Model v1.3.0 - Quality Measure Service

**Release Date**: 2026-01-28
**Previous Version**: v1.2.0 (migration 0045)
**Current Version**: v1.3.0 (migration 0046)

## Schema Changes

### New Tables (3)

1. **health_scores**
   - Purpose: Track patient health scoring metrics
   - Columns: 15 (id, tenant_id, patient_id, 7 score types, audit fields)
   - Indexes: 4 (patient, tenant, composite scoring)
   - Sprint: 23

2. **care_team_members**
   - Purpose: Coordinate multi-provider patient care
   - Columns: 12 (id, patient_id, provider_id, role, contact, audit fields)
   - Indexes: 5 (patient, provider, tenant, role)
   - Sprint: 24

3. **report_templates**
   - Purpose: Advanced analytics and reporting
   - Columns: 20 (id, template_name, query_definition JSONB, schedule, audit fields)
   - Indexes: 3 (tenant, category, active)
   - Sprint: 25

### Modified Tables (0)

No existing tables modified in this release.

### Data Migration Required

None - all new tables.

## Upgrade Path

**From v1.2.0 → v1.3.0:**
- Migration 0046 creates 3 new tables
- No data transformation required
- Zero downtime deployment supported

## Rollback Plan

```bash
# Rollback to v1.2.0
kubectl rollout undo deployment/quality-measure-service

# Rollback migration (if needed - data loss!)
liquibase rollbackCount 1

# Verify rollback
psql -U healthdata -d quality_db -c "SELECT id FROM databasechangelog ORDER BY dateexecuted DESC LIMIT 5;"
```

## Testing

- Entity-migration validation: ✅ PASSED
- Integration tests: ✅ 1,568/1,568 PASSED (100%)
- Performance tests: ✅ <100ms p95
- Security tests: ✅ HIPAA compliant

## Breaking Changes

None.

## Deprecations

None.
```

#### Step 2: Archive Sprint Branches
```bash
# Sprint branches no longer needed
git branch -d feature/sprint-23-health-scoring
git branch -d feature/sprint-24-care-team
git branch -d feature/sprint-25-reporting

# Clean up remote branches
git push origin --delete feature/sprint-23-health-scoring
git push origin --delete feature/sprint-24-care-team
git push origin --delete feature/sprint-25-reporting
```

#### Step 3: Update Release Notes
```bash
# Add to CHANGELOG.md
cat >> CHANGELOG.md <<EOF

## [1.3.0] - 2026-01-28

### Added
- Patient health scoring system (Sprint 23)
- Care team assignment and coordination (Sprint 24)
- Advanced reporting and analytics (Sprint 25)

### Data Model
- Migration 0046: Added 3 tables (health_scores, care_team_members, report_templates)
- Total schema size: 42 tables, 487 columns
- Full entity-migration validation passing

### Fixed
- Entity-migration sync issues from v1.2.0

### Migration Guide
See docs/data-model/v1.3.0/CHANGELOG.md
EOF
```

---

## Sprint Start Determination

### How to Start a Sprint

**Decision Point**: When does a new sprint formally begin?

**Answer**: Sprint begins when:
1. ✅ Previous release deployed to production (if applicable)
2. ✅ Data model documented for previous release
3. ✅ Sprint planning meeting complete
4. ✅ User stories prioritized and assigned

**Sprint Start Checklist:**
```bash
# 1. Verify current release state
git describe --tags --abbrev=0
# Expected: v1.3.0 (current production version)

# 2. Create sprint tracking issue
gh issue create --title "Sprint 26: Epic Name" --body "Sprint goals..."

# 3. Developers create feature branches from main
git checkout main
git pull origin main
git checkout -b feature/sprint-26-my-feature

# 4. Configure development environment (H2)
# Each developer uses application-dev.yml with H2
```

**What if release is pending?**
- Sprints can start even if previous release not deployed yet
- Developers work on `main` branch (with entity changes, no migrations)
- All pending entity changes will be included in next release migration

**Example Timeline:**
```
Week 1-2: Sprint 23 (health scoring)
Week 3-4: Sprint 24 (care team)
Week 5-6: Sprint 25 (reporting)
Week 7: Release prep v1.3.0 (generate migration for sprints 23-25)
Week 8: Deploy v1.3.0, document data model
Week 9-10: Sprint 26 (NEW sprint starts)
```

---

## Migration Naming Convention

**Format**: `NNNN-release-vX.Y.Z.xml`

**Examples:**
- `0046-release-v1.3.0.xml` - Release 1.3.0 schema changes
- `0047-release-v1.3.1-hotfix.xml` - Hotfix release schema changes
- `0048-release-v1.4.0.xml` - Release 1.4.0 schema changes

**NOT**:
- ❌ `0046-sprint-24.xml` (sprint != release)
- ❌ `0046-add-care-team.xml` (feature-specific, not release-scoped)
- ❌ `0046-january-2026.xml` (date-based, not version-based)

---

## Testing Strategy

### Development Testing (H2)
```yaml
# application-dev.yml
spring:
  jpa:
    hibernate:
      ddl-auto: update          # Auto-schema for rapid iteration
  liquibase:
    enabled: false              # No migrations during development
```

**Tests**: Fast, in-memory, auto-schema

### Integration Testing (PostgreSQL)
```yaml
# application-test.yml
spring:
  jpa:
    hibernate:
      ddl-auto: validate        # Validate entities match migrations
  liquibase:
    enabled: true               # Run migrations
```

**Tests**: Full validation against production-like database

**IMPORTANT**: Integration tests run ONLY during:
1. **Release preparation** (when migration is generated)
2. **CI/CD pipeline** (pre-deployment validation)
3. **NOT during development** (too slow, not necessary)

---

## Rollback Strategy

### Code Rollback
```bash
# Rollback to previous release
kubectl rollout undo deployment/quality-measure-service

# Or specific version
kubectl rollout undo deployment/quality-measure-service --to-revision=42
```

### Schema Rollback

**Option A**: Liquibase Rollback (Data Loss!)
```bash
liquibase rollbackCount 1
```

**Option B**: Keep Schema, Deploy Previous Code
```bash
# Keep new tables (empty, unused)
# Deploy previous code version
# Old code ignores new tables

# Safer! No data loss
```

**Recommendation**: Option B (keep schema, rollback code only)

---

## Benefits of Release-Driven Approach

### vs. Per-Commit Migrations

| Metric | Per-Commit | Release-Driven | Improvement |
|--------|------------|----------------|-------------|
| **Migrations per release** | 50+ | 1 | **98% reduction** |
| **Schema review overhead** | High (50 reviews) | Low (1 review) | **98% reduction** |
| **Production schema changes** | 50+ times | 1 time | **98% reduction** |
| **Rollback complexity** | High (50 migrations) | Low (1 migration) | **98% simpler** |
| **Audit trail clarity** | Scattered | Clear | **Significantly better** |

### vs. Per-Sprint Migrations

| Metric | Per-Sprint | Release-Driven | Improvement |
|--------|------------|----------------|-------------|
| **Migrations per release** | 3-5 | 1 | **75% reduction** |
| **Coordination overhead** | Medium | Low | **Easier** |
| **Production stability** | Good | Excellent | **Better** |

---

## FAQs

### Q: What if a hotfix needs a schema change?

**A**: Create hotfix release migration:
```bash
# Example: v1.3.1 hotfix
./scripts/release-schema-export.sh v1.3.1-hotfix quality-measure-service

# Creates: 0047-release-v1.3.1-hotfix.xml
```

### Q: Can multiple services have different release cadences?

**A**: YES! Each service has independent data model versioning:
- quality-measure-service might be at v1.3.0
- patient-service might be at v2.1.0
- fhir-service might be at v1.8.0

Each service generates its own release migrations.

### Q: How do we handle database-wide changes (e.g., PostgreSQL extensions)?

**A**: Create special infrastructure migration:
```bash
# 0000-infrastructure-pgcrypto.xml (runs once across all databases)
```

### Q: What if entity changes during sprint need to be reverted?

**A**: Just revert the code commit:
```bash
git revert a1b2c3d

# Entity gone from codebase
# H2 schema updated automatically (next restart)
# No migration created yet (so nothing to undo)
```

---

## Success Metrics

### Target (6 Months)

- **Migrations per release**: 1 (vs. current 10-15)
- **Entity-migration drift**: 0% (vs. current 15.4%)
- **Production schema incidents**: 0 (vs. current 2-3/year)
- **Developer satisfaction**: ≥4.5/5 (survey after rollout)

---

## Related Documentation

- [ENTITY_MIGRATION_GUIDE.md](./ENTITY_MIGRATION_GUIDE.md) - Entity-migration best practices
- [DATABASE_ARCHITECTURE_GUIDE.md](./DATABASE_ARCHITECTURE_GUIDE.md) - Multi-tenant database design
- [LIQUIBASE_DEVELOPMENT_WORKFLOW.md](./LIQUIBASE_DEVELOPMENT_WORKFLOW.md) - Legacy manual workflow
- [CLAUDE.md](../../CLAUDE.md) - Project quick reference

---

**Approved By**: Development Team
**Effective Date**: 2026-01-21
**Version**: 1.0 - Release-Driven Workflow
**Next Review**: After v1.3.0 release (validate approach)
