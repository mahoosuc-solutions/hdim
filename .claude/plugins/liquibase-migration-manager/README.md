# Liquibase Migration Manager Plugin

**Version:** 0.1.0-mvp
**Status:** MVP - Pilot Phase
**Created:** 2026-01-21
**Author:** HDIM Platform Team

## Overview

Multi-agent system for managing Liquibase migrations across HDIM's 29+ microservices. Each service has a dedicated agent that understands its data model, reviews migration files, validates entity-migration synchronization, and coordinates with peer agents for cross-service impacts.

## MVP Scope (Current)

The MVP includes:
- ✅ **quality-measure-agent** - Pilot service agent
- ✅ **migration-review skill** - Core validation logic
- ✅ **/review-migration command** - User interface
- ✅ **Service registry** - Configuration system

**Supported Services:** quality-measure-service only

## Architecture

```
.claude/plugins/liquibase-migration-manager/
├── plugin.json                           # Plugin manifest
├── README.md                             # This file
├── agents/                               # Per-service agent definitions
│   └── quality-measure-agent.md          # ✅ MVP
├── skills/                               # Shared agent skills
│   └── migration-review.md               # ✅ MVP
├── commands/                             # User-facing commands
│   └── review-migration.md               # ✅ MVP
└── config/                               # Configuration
    └── service-registry.json             # ✅ MVP
```

## Quick Start

### Using the Plugin

**Review migration 0044:**
```
/review-migration quality-measure-service 0044-fix-measure-modification-audit-columns.xml
```

**Or directly invoke the agent:**
```
Use the quality-measure-liquibase-agent to review migration 0044
```

**Or ask questions:**
```
Is migration 0044 safe to apply?
```

### Expected Output

```
========================================
MIGRATION REVIEW: 0044-fix-measure-modification-audit-columns.xml
========================================

Service: quality-measure-service
Status: APPROVE ✓

Findings:
✓ Sequential migration number (follows 0043)
✓ Valid Liquibase XML
✓ Rollback SQL present
✓ Entity synchronization will be fixed
! Breaking change: RENAME COLUMN (low risk, JPA safe)

Recommendation: APPROVE - Safe to apply

Next Steps:
1. Apply migration: docker compose up quality-measure-service
2. Verify Liquibase execution: docker logs quality-measure-service
3. Run validation test: ./gradlew test --tests "*EntityMigrationValidationTest"
4. Run full test suite: ./gradlew :modules:services:quality-measure-service:test

========================================
```

## Components

### 1. Quality Measure Agent

**File:** `agents/quality-measure-agent.md`

**Responsibilities:**
- Review Liquibase migrations for quality-measure-service
- Validate entity-migration synchronization
- Detect breaking changes
- Provide approval/rejection recommendations

**Expertise:**
- Knows all 7 entities in quality-measure-service
- Understands 44 migrations (0000-0044)
- Tracks entity-migration mappings
- Detects type mismatches, missing columns, naming drift

**Current Mission:**
Helping fix 389 test failures by validating migrations 0042-0044 that resolve entity-migration drift issues.

### 2. Migration Review Skill

**File:** `skills/migration-review.md`

**Capabilities:**
- Validate Liquibase XML syntax
- Check rollback SQL completeness
- Detect breaking changes (DROP, RENAME, type narrowing)
- Verify column type mappings (JPA ↔ PostgreSQL)
- Ensure sequential migration numbering
- Check tenant isolation patterns
- Validate entity-migration synchronization

**Validation Checklist:**
1. File naming & structure
2. XML validity
3. Rollback SQL
4. Column type mapping
5. Tenant isolation
6. Breaking change detection
7. Sequential numbering
8. Entity synchronization

### 3. Review Migration Command

**File:** `commands/review-migration.md`

**Usage:**
```
/review-migration <service-name> <migration-file>
```

**Flow:**
1. Parse command arguments
2. Load service registry
3. Locate service agent
4. Invoke agent with migration file
5. Generate comprehensive report
6. Return to user

**Error Handling:**
- Migration file not found
- Service not registered
- Agent not available (future services)

### 4. Service Registry

**File:** `config/service-registry.json`

**Current Services:** 1 (quality-measure-service)
**Future Services:** 28 (listed but not yet implemented)

**Service Entry Structure:**
```json
{
  "name": "quality-measure-service",
  "database": "quality_db",
  "port": 8087,
  "agent": "quality-measure-liquibase-agent",
  "migration_path": "backend/modules/services/.../db/changelog",
  "entity_path": "backend/modules/services/.../persistence",
  "entities": [...],
  "migration_count": 44,
  "last_migration": "0044-fix-measure-modification-audit-columns.xml",
  "status": "active"
}
```

## Use Cases

### Use Case 1: Review New Migration

**Scenario:** Developer created migration 0044 to rename a column.

**Steps:**
1. Developer: `/review-migration quality-measure-service 0044-fix-measure-modification-audit-columns.xml`
2. Agent analyzes migration
3. Agent checks entity synchronization
4. Agent provides APPROVE recommendation
5. Developer applies migration with confidence

**Benefit:** Catches issues before applying migrations to database.

### Use Case 2: Detect Entity-Migration Drift

**Scenario:** Entity expects `change_description` but database has `change_summary`.

**Steps:**
1. Agent reads entity: `@Column(name = "change_description")`
2. Agent reads migration 0039: Creates column `change_summary`
3. Agent detects mismatch
4. Agent recommends migration 0044: Rename column
5. Developer creates and applies fix

**Benefit:** Prevents schema validation failures in production.

### Use Case 3: Prevent Breaking Changes

**Scenario:** Developer attempts to drop a column without deprecation.

**Steps:**
1. Developer: `/review-migration quality-measure-service 0099-drop-legacy-column.xml`
2. Agent detects: DROP COLUMN (CRITICAL risk)
3. Agent checks: No deprecation period
4. Agent provides REJECT recommendation
5. Agent suggests: Two-phase migration approach
6. Developer follows recommendation

**Benefit:** Prevents production outages from breaking changes.

## Current Context: Fixing 389 Test Failures

The plugin is being used to fix critical entity-migration synchronization issues:

**Problem:** 389 tests failing due to schema validation errors
**Root Cause:** Entity-migration drift (mismatched types, missing columns, wrong names)

**Migrations Created:**
- `0042-fix-health-score-column-types.xml` - Convert 15 columns to DOUBLE PRECISION
- `0043-add-measure-id-to-config-profiles.xml` - Add missing `measure_id` column
- `0044-fix-measure-modification-audit-columns.xml` - Rename `change_summary` → `change_description`

**Plugin Role:**
- Validate these migrations are safe to apply
- Provide confidence they will fix the issues
- Guide application process
- Prepare for potential additional issues

## Implementation Roadmap

### Phase 1: MVP (COMPLETE ✅)
- [x] Plugin infrastructure
- [x] quality-measure-agent
- [x] migration-review skill
- [x] /review-migration command
- [x] Service registry

### Phase 2: Core Services (Future)
- [ ] patient-agent
- [ ] care-gap-agent
- [ ] fhir-agent
- [ ] cql-engine-agent
- [ ] entity-analysis skill
- [ ] schema-validation skill

### Phase 3: Coordination (Future)
- [ ] Cross-service dependencies mapping
- [ ] breaking-change-detection skill
- [ ] cross-service-coordination skill
- [ ] /approve-migration command (multi-agent voting)
- [ ] Approval policy framework

### Phase 4: Scale-Out (Future)
- [ ] Remaining 24 service agents
- [ ] Complete cross-service dependency graph
- [ ] Agent dashboard
- [ ] Migration status tracking

### Phase 5: Automation (Future)
- [ ] CI/CD integration (GitHub Actions)
- [ ] Auto-review on PR creation
- [ ] Slack notifications
- [ ] Migration approval workflow

## Configuration

### Service Registry

Add new services to `config/service-registry.json`:

```json
{
  "name": "your-service-name",
  "database": "your_db",
  "port": 8XXX,
  "agent": "your-service-liquibase-agent",
  "migration_path": "backend/modules/services/your-service/src/main/resources/db/changelog",
  "entity_path": "backend/modules/services/your-service/src/main/java/com/healthdata/yourpackage/persistence",
  "entities": [],
  "migration_count": 0,
  "last_migration": "",
  "status": "planned"
}
```

### Agent Creation

Create new service agent in `agents/your-service-agent.md` following the quality-measure-agent template.

## Testing

### Manual Testing

```bash
# Test the MVP
/review-migration quality-measure-service 0044-fix-measure-modification-audit-columns.xml

# Expected: APPROVE ✓ recommendation
```

### Validation

Confirm agent provides:
1. Status (APPROVE/REJECT)
2. Validation checklist results
3. Breaking change analysis
4. Entity synchronization check
5. Recommendation
6. Next steps

## Design Principles

1. **Service Autonomy** - Each service has its own agent with deep domain knowledge
2. **Distributed Intelligence** - Agents coordinate but operate independently
3. **Safety First** - Blocking dangerous operations (DROP, RENAME without deprecation)
4. **Human-in-the-Loop** - Agents provide recommendations, humans make final decisions
5. **Audit Trail** - All reviews logged for compliance
6. **Extensibility** - Easy to add new services, skills, commands

## Benefits

**For Developers:**
- Confidence in migration safety
- Fast feedback (< 30 seconds)
- Clear guidance on fixes
- Prevents production incidents

**For Platform:**
- Prevents entity-migration drift
- Enforces best practices
- Cross-service coordination
- Audit trail for compliance

**For Operations:**
- Reduces schema-related outages
- Faster incident resolution
- Better change visibility
- Coordinated rollouts

## Known Limitations (MVP)

- ❌ Only quality-measure-service supported
- ❌ No cross-service coordination yet
- ❌ No approval workflow (manual apply)
- ❌ No CI/CD integration
- ❌ No automated discovery of migrations

**Future versions will address these limitations.**

## Support

**Documentation:**
- Plugin README: This file
- Agent Guide: `agents/quality-measure-agent.md`
- Skill Guide: `skills/migration-review.md`
- Command Guide: `commands/review-migration.md`

**Usage Questions:**
```
/review-migration quality-measure-service --help
```

**Issues:**
Report plugin issues in the main HDIM repository.

## Success Metrics

**MVP Goals:**
- ✅ Review migration 0044 successfully
- ✅ Validate migrations 0042-0044 are safe
- ✅ Provide clear recommendations
- ⏸️ Help achieve 100% test pass rate (in progress)

**Full System Goals (Future):**
- 80% of migrations reviewed by agents before application
- 90% of breaking changes detected before production
- Zero schema-related production incidents
- 70% reduction in manual review time

## Version History

**0.1.0-mvp (2026-01-21)**
- Initial MVP release
- quality-measure-agent (pilot)
- migration-review skill
- /review-migration command
- Service registry foundation

**Future Versions:**
- 0.2.0: Add 4 more service agents (patient, care-gap, fhir, cql-engine)
- 0.3.0: Cross-service coordination
- 0.4.0: Approval workflow
- 1.0.0: All 29 services, CI/CD integration

---

## Quick Links

- [Full Architecture Design](../../docs/architecture/LIQUIBASE_MIGRATION_MANAGER_ARCHITECTURE.md) (see Plan agent output)
- [quality-measure-agent](agents/quality-measure-agent.md)
- [migration-review skill](skills/migration-review.md)
- [/review-migration command](commands/review-migration.md)
- [Service Registry](config/service-registry.json)

---

*Plugin created: 2026-01-21*
*Current mission: Fix 389 test failures in quality-measure-service*
*Next expansion: Phase 2 - Add 4 more service agents*
