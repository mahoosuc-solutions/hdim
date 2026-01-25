---
name: db:migrate
description: Execute Liquibase database migrations with validation and rollback support
category: database
priority: high
---

# Database Migration Management

Execute Liquibase database migrations across HDIM microservices with automatic validation and rollback capabilities.

## Usage

```bash
/db:migrate [options]
```

## Options

- `--service <name>` - Target service (patient, care-gap, quality-measure, fhir, etc.)
- `--all` - Run migrations for all services
- `--validate-only` - Validate without executing
- `--rollback <count>` - Rollback N changesets
- `--tag <name>` - Create rollback tag
- `--dry-run` - Show SQL without executing
- `--force` - Skip validation warnings

## Examples

### Run Migrations for Single Service

```bash
/db:migrate --service patient-service --validate-only
```

Validates Patient Service migrations without executing.

### Execute All Service Migrations

```bash
/db:migrate --all
```

Runs migrations for all 29 HDIM databases in dependency order.

### Rollback Recent Migration

```bash
/db:migrate --service care-gap-service --rollback 1
```

Rolls back the most recent changeset for Care Gap Service.

### Create Rollback Tag

```bash
/db:migrate --service patient-service --tag "v2.0-release"
```

Creates a rollback tag for the current database state.

## HDIM Migration Workflow

```mermaid
graph TD
    A[/db:migrate] --> B{Validate First}
    B -->|Validation| C[Run EntityMigrationValidationTest]
    C -->|Pass| D{Execute or Rollback?}
    C -->|Fail| E[Report Schema Drift]
    E --> F[Fix Entity/Migration Mismatch]
    F --> C
    D -->|Execute| G[Run Liquibase Update]
    D -->|Rollback| H[Run Liquibase Rollback]
    G --> I[Verify Entity Sync]
    H --> I
    I -->|Success| J[Update Changelog Status]
    I -->|Fail| K[Trigger Rollback]
    K --> L[Restore Previous State]
```

## Pre-Migration Validation

Before running migrations, this command automatically:

1. **Entity-Migration Sync Check**
   - Runs `EntityMigrationValidationTest` for target service
   - Verifies JPA entities match Liquibase schema
   - Detects schema drift before execution

2. **Rollback Coverage**
   - Validates all changesets have rollback directives
   - Checks rollback syntax correctness
   - Ensures safe rollback capability

3. **Database State**
   - Checks for locked tables
   - Verifies connection pool availability
   - Validates tenant isolation

## Services and Databases

| Service | Database | Port | Changesets |
|---------|----------|------|------------|
| patient-service | patient_db | 5435 | 47 |
| care-gap-service | care_gap_db | 5435 | 38 |
| quality-measure-service | quality_measure_db | 5435 | 29 |
| fhir-service | fhir_db | 5435 | 52 |
| cql-engine-service | cql_engine_db | 5435 | 18 |
| documentation-service | documentation_db | 5435 | 15 |

Total: **29 databases, 199 changesets**

## Migration Order (Dependency Resolution)

```
1. patient-service (no dependencies)
2. fhir-service (depends on patient-service)
3. quality-measure-service (depends on patient-service)
4. care-gap-service (depends on patient + quality-measure)
5. cql-engine-service (depends on quality-measure)
6. documentation-service (independent)
...
```

## Output Format

```
╔══════════════════════════════════════════════════════════════╗
║              Database Migration - patient-service            ║
╚══════════════════════════════════════════════════════════════╝

[1/5] Validating entity-migration sync...
✓ EntityMigrationValidationTest passed

[2/5] Checking rollback coverage...
✓ All 47 changesets have rollback directives

[3/5] Verifying database state...
✓ Database connection: OK
✓ No locked tables
✓ Tenant isolation: OK

[4/5] Executing migrations...
Running changeset: 0001-create-patients-table.xml
Running changeset: 0002-add-tenant-id-column.xml
Running changeset: 0003-create-patient-demographics-table.xml
✓ 3 changesets executed successfully

[5/5] Post-migration validation...
✓ Entity sync verified
✓ Indexes created
✓ Constraints enabled

Migration completed: patient-service
  Changesets executed: 3
  Duration: 1.2 seconds
  Rollback tag: migration-2026-01-25-001
```

## Rollback Process

```bash
# Rollback to specific tag
/db:migrate --service patient-service --rollback-to-tag "v1.9-release"

# Rollback N changesets
/db:migrate --service care-gap-service --rollback 5

# Emergency rollback (all pending)
/db:migrate --service fhir-service --rollback-all
```

## Common Issues & Fixes

| Error | Fix |
|-------|-----|
| `Schema-validation: missing table` | Create Liquibase migration for entity |
| `Changeset already applied` | Check `DATABASECHANGELOG` table |
| `Rollback directive missing` | Add `<rollback>` to changeset XML |
| `Entity-migration validation failed` | Run `/db:validate --service <name>` |

## HIPAA Compliance

- All migrations logged to audit trail
- PHI-containing tables have encryption enabled
- Tenant isolation enforced via `tenant_id` column
- Rollback capability maintains data integrity

## Performance

- Single service migration: ~1-3 seconds
- All services (29 databases): ~45-60 seconds
- Rollback operation: ~500ms per changeset

## Safety Features

1. **Automatic Backup**
   - Creates database snapshot before migration
   - Snapshot retained for 7 days
   - Automatic cleanup of old snapshots

2. **Dry-Run Mode**
   - Shows SQL without executing
   - Validates syntax and dependencies
   - Preview changes before applying

3. **Validation Gates**
   - Entity-migration sync required
   - Rollback coverage required
   - Database health check required

## Related Commands

- `/db:validate` - Validate entity-migration synchronization
- `/db:rollback` - Dedicated rollback operations
- `/db:tenant` - Multi-tenant database operations
- `/db:backup` - Create database backups

## See Also

- [Liquibase Development Workflow](../backend/docs/LIQUIBASE_DEVELOPMENT_WORKFLOW.md)
- [Entity-Migration Guide](../backend/docs/ENTITY_MIGRATION_GUIDE.md)
- [Database Architecture Guide](../backend/docs/DATABASE_ARCHITECTURE_GUIDE.md)
