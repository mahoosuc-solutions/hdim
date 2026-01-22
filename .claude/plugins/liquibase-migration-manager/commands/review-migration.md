---
name: review-migration
command: /review-migration
description: Review a Liquibase migration file using the service's specialized agent
usage: /review-migration <service-name> <migration-file>
examples:
  - /review-migration quality-measure-service 0044-fix-measure-modification-audit-columns.xml
  - /review-migration patient-service 0050-add-insurance-column.xml
  - /review-migration fhir-service 0030-add-r4-support.xml
---

# Review Migration Command

## Purpose
Invoke a service-specific Liquibase agent to perform comprehensive review of a migration file.

## Usage

```
/review-migration <service-name> <migration-file>
```

**Parameters:**
- `service-name`: Name of the microservice (e.g., quality-measure-service, patient-service)
- `migration-file`: Migration filename (e.g., 0044-rename-column.xml)

## Examples

### Example 1: Review Current Migration
```
/review-migration quality-measure-service 0044-fix-measure-modification-audit-columns.xml
```

**Agent Invoked:** `quality-measure-liquibase-agent`
**Skills Used:** `migration-review`

**Output:**
```
========================================
MIGRATION REVIEW: 0044-fix-measure-modification-audit-columns.xml
========================================

Service: quality-measure-service
Status: APPROVE ✓

Findings:
✓ Sequential migration number
✓ Valid Liquibase XML
✓ Rollback SQL present
! Breaking change: RENAME COLUMN (low risk)
✓ Entity synchronization will be fixed

Recommendation: APPROVE - Safe to apply
========================================
```

### Example 2: Review with Issues
```
/review-migration patient-service 0099-drop-legacy-table.xml
```

**Output:**
```
========================================
MIGRATION REVIEW: 0099-drop-legacy-table.xml
========================================

Service: patient-service
Status: REJECT ✗

Findings:
✓ Valid Liquibase XML
✗ Non-sequential number (expected 0052, got 0099)
✗ Breaking change: DROP TABLE (critical risk)
✗ No rollback SQL (data loss)
! Cross-service impact: 3 services reference this table

Recommendation: REJECT - Critical issues found

Blocking Issues:
1. Non-sequential migration number
2. DROP TABLE without deprecation period
3. Cross-service dependencies not resolved

========================================
```

## Command Flow

```
User: /review-migration <service> <file>
  ↓
1. Parse command arguments
  ↓
2. Load service registry
  ↓
3. Find service configuration
  ↓
4. Locate service agent
  ↓
5. Invoke agent with migration file
  ↓
6. Agent applies migration-review skill
  ↓
7. Generate comprehensive report
  ↓
8. Return to user
```

## Service Registry Integration

The command reads service metadata from:
`.claude/plugins/liquibase-migration-manager/config/service-registry.json`

**Example Service Entry:**
```json
{
  "name": "quality-measure-service",
  "database": "quality_db",
  "port": 8087,
  "agent": "quality-measure-liquibase-agent",
  "migration_path": "backend/modules/services/quality-measure-service/src/main/resources/db/changelog",
  "entity_path": "backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/persistence"
}
```

## Error Handling

### Error 1: Migration File Not Found
```
ERROR: Migration file not found

Service: quality-measure-service
File: 0099-nonexistent.xml
Path: backend/modules/services/quality-measure-service/src/main/resources/db/changelog/0099-nonexistent.xml

Recent migrations:
- 0042-fix-health-score-column-types.xml
- 0043-add-measure-id-to-config-profiles.xml
- 0044-fix-measure-modification-audit-columns.xml

Suggestion: Check filename and try again
```

### Error 2: Service Not Found
```
ERROR: Service not registered

Service: invalid-service-name

Available services:
- quality-measure-service
- patient-service
- care-gap-service
- fhir-service
- cql-engine-service
(+ 24 more)

Suggestion: Check service name and try again
Or run: /list-services
```

### Error 3: Agent Not Available
```
ERROR: Agent not initialized

Service: patient-service
Agent: patient-liquibase-agent

The agent for this service has not been created yet.

Status: MVP phase - only quality-measure-service agent available

Planned agents:
- patient-liquibase-agent (Phase 2)
- care-gap-liquibase-agent (Phase 2)
- fhir-liquibase-agent (Phase 2)
(+ 25 more in future phases)

For now, use: /review-migration quality-measure-service <file>
```

## Implementation Details

When this command runs, it performs:

1. **Argument Parsing**
```bash
SERVICE_NAME=$1  # quality-measure-service
MIGRATION_FILE=$2  # 0044-fix-measure-modification-audit-columns.xml
```

2. **Service Lookup**
```bash
# Read service registry
SERVICE_CONFIG=$(jq ".services[] | select(.name == \"$SERVICE_NAME\")" \
  .claude/plugins/liquibase-migration-manager/config/service-registry.json)

# Extract paths
MIGRATION_PATH=$(echo "$SERVICE_CONFIG" | jq -r '.migration_path')
AGENT_NAME=$(echo "$SERVICE_CONFIG" | jq -r '.agent')
```

3. **Agent Invocation**
```
Invoke agent: ${AGENT_NAME}
Context: {
  "service": "${SERVICE_NAME}",
  "migration_file": "${MIGRATION_FILE}",
  "migration_path": "${MIGRATION_PATH}"
}
```

4. **Report Generation**
The agent produces a structured review report using the migration-review skill.

## Output Format

All reviews follow this standard format:

```
========================================
MIGRATION REVIEW: {FILENAME}
========================================

Service: {SERVICE_NAME}
Agent: {AGENT_NAME}
Timestamp: {TIMESTAMP}

Status: APPROVE | REJECT | NEEDS_REVIEW

----------------------------------------
VALIDATION RESULTS
----------------------------------------

{CHECKLIST_RESULTS}

----------------------------------------
RECOMMENDATION
----------------------------------------

{DETAILED_RECOMMENDATION}

Next Steps:
1. {STEP_1}
2. {STEP_2}
3. {STEP_3}

========================================
```

## Integration with Workflow

This command is typically used in the development workflow:

```
Developer creates migration
  ↓
/review-migration <service> <file>
  ↓
Review report generated
  ↓
If APPROVED:
  - Apply migration
  - Run tests
  - Commit changes

If REJECTED:
  - Fix issues
  - Re-run review
  - Iterate until approved
```

## Future Enhancements (Post-MVP)

- Auto-review on git pre-commit hook
- Batch review: `/review-migration <service> --all-pending`
- Compare mode: `/review-migration <service> <file1> <file2>`
- History: `/review-migration <service> --history`
- Approval workflow: Integrate with `/approve-migration` for multi-agent coordination

## Testing the MVP

To test the MVP plugin with migration 0044:

```
/review-migration quality-measure-service 0044-fix-measure-modification-audit-columns.xml
```

Expected result: Comprehensive review confirming migration is safe to apply.
