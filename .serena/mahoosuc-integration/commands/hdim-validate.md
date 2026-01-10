---
name: hdim-validate
description: Run HDIM validation checks (HIPAA, multi-tenant, entity-migration, health)
category: hdim
---

# HDIM Validation Command

Run comprehensive validation checks for HDIM development.

## Usage

```
/hdim-validate [check-type]
```

**Options**:
- `hipaa` - Run HIPAA compliance check only
- `tenant` - Run multi-tenant query check only
- `entity` - Run entity-migration validation only
- `health` - Run service health check only
- `all` (default) - Run all checks

## What This Command Does

Execute the appropriate validation tool(s) from `.serena/tools/`:

1. **HIPAA Compliance**: Scan for cache TTL violations, missing headers, PHI in logs
2. **Multi-Tenant**: Ensure all queries filter by tenantId
3. **Entity-Migration**: Validate JPA entities match Liquibase migrations
4. **Health**: Check all services are running and healthy

## Examples

```bash
# Run all checks
/hdim-validate

# Run specific check
/hdim-validate hipaa
/hdim-validate tenant
/hdim-validate entity
/hdim-validate health
```

## Implementation

```bash
#!/bin/bash

CHECK_TYPE=${1:-all}

case "$CHECK_TYPE" in
  hipaa)
    echo "Running HIPAA compliance check..."
    bash .serena/tools/check-hipaa-compliance.sh
    ;;
  tenant)
    echo "Running multi-tenant query check..."
    bash .serena/tools/check-multitenant-queries.sh
    ;;
  entity)
    echo "Running entity-migration validation..."
    bash .serena/tools/validate-entity-migration-sync.sh
    ;;
  health)
    echo "Running service health check..."
    bash .serena/tools/check-service-health.sh
    ;;
  all)
    echo "Running all HDIM validation checks..."
    bash .serena/workflows/pre-commit-check.sh
    ;;
  *)
    echo "Unknown check type: $CHECK_TYPE"
    echo "Valid options: hipaa, tenant, entity, health, all"
    exit 1
    ;;
esac
```

## When to Use

- Before committing code
- After making security-related changes
- When debugging compliance issues
- As part of CI/CD pipeline
- Daily development workflow

## Related Commands

- `/hdim-service-create` - Create new HDIM service
- `/hdim-memory` - Access Serena memories
- `/hdim-health` - Quick service health check
