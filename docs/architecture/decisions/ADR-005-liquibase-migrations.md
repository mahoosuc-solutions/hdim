# ADR-005: Liquibase for All Database Migrations

**Status**: Accepted
**Date**: 2026-01-19 (Decision Made: Phase 3, Nov 2025)
**Decision Makers**: Architecture Lead, Platform Team
**Stakeholders**: All Backend Services, Database Team

---

## Context

### Problem Statement

HDIM had mixed database migration approaches (Flyway in some services, no migrations in others) creating inconsistency, version control issues, and unsafe schema changes. Needed standardized migration tool with rollback support for production safety.

---

## Options Considered

### Option 1: Liquibase for All Services

**Description**: Standardize on Liquibase 4.29.2 for all database migrations with explicit rollback SQL

**Pros**:
- Industry standard (OpenSource, widely used)
- Rollback capability (reversible migrations)
- XML + YAML support
- Preconditions (fail-safe migrations)
- Changelog tracking (databasechangelog table)
- Version-agnostic (work with any schema)

**Cons**:
- More verbose than Flyway
- Larger learning curve
- XML can be verbose

**Estimated Effort**: 3 weeks migration
**Risk Level**: Low (well-tested)

---

### Option 2: Keep Flyway (Existing)

**Description**: Continue using Flyway in services where deployed

**Pros**:
- Minimal change
- Simpler syntax

**Cons**:
- No rollback support (forward-only)
- Two tools to maintain (inconsistency)
- Can't reverse failed migrations
- Not suitable for complex changes

**Risk Level**: High (no rollback support)

---

## Decision

**We chose Option 1 (Liquibase for All Services)** because:

1. **Rollback Safety**: Explicit rollback SQL means reversible migrations (critical for production)
2. **Standardization**: One migration tool across all 29 services
3. **Preconditions**: Can add safety checks (e.g., only create if not exists)
4. **Changelog Tracking**: Version-control all schema changes
5. **Compliance**: HIPAA auditing benefits from change tracking
6. **Future-Proof**: Liquibase grows with system complexity

---

## Consequences

### Positive

- 100% rollback coverage (199/199 changesets)
- Safe schema changes with rollback capability
- Unified migration approach across all services
- Preconditions prevent duplicate changes
- Clear migration history

### Negative

- Migration from Flyway took time
- XML verbosity
- Slightly larger migration files

---

## Implementation

### Key Principle

**Every changeset MUST have explicit rollback SQL** - No exceptions

```xml
<changeSet id="0001-create-patients-table" author="dev">
    <createTable tableName="patients">
        <column name="id" type="UUID" primaryKey="true"/>
    </createTable>
    <rollback>
        <dropTable tableName="patients"/>
    </rollback>
</changeSet>
```

### Migration Pattern

**File Structure**:
```
src/main/resources/db/changelog/
├── 0000-enable-extensions.xml
├── 0001-create-patients-table.xml
├── 0002-add-insurance-table.xml
└── db.changelog-master.xml
```

**Naming Convention**: `NNNN-descriptive-name.xml` (4-digit sequential numbers)

### Configuration

```yaml
spring:
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml
  jpa:
    hibernate:
      ddl-auto: validate  # MUST be validate, never create/update
```

### Success Criteria

- ✅ All 29 services use Liquibase
- ✅ 199/199 changesets have rollback SQL (100% coverage)
- ✅ CI/CD validates rollback coverage
- ✅ Zero Flyway usage
- ✅ All schema changes traceable

---

## Monitoring & Validation

### Metrics

| Metric | Target | Current |
|--------|--------|---------|
| Services using Liquibase | 29/29 | 29/29 ✅ |
| Rollback coverage | 100% | 100% ✅ |
| Failed migrations | 0 | 0 |

---

## Validation Script

```bash
# backend/scripts/validate-liquibase-rollback.sh
# Checks every changeset has explicit rollback
```

---

## References

- **[Liquibase Development Workflow](../../backend/docs/LIQUIBASE_DEVELOPMENT_WORKFLOW.md)** ⭐ CRITICAL
- **[Database Architecture Guide](../../backend/docs/DATABASE_ARCHITECTURE_GUIDE.md)**

---

## Footer

**ADR #**: 005
**Version**: 1.0
**Status**: Active and Validated
**Metrics**: 100% Rollback Coverage (199/199 changesets)

_Decision Date: Phase 3 (November 2025)_
_Formalized: January 2026_
_Migration Status: Complete (Flyway → Liquibase)_
