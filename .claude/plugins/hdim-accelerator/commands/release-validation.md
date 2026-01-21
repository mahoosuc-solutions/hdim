---
description: Execute comprehensive release validation workflow for HDIM Platform using Ralph Wiggum autonomous loops
arguments:
  version:
    description: Release version (e.g., v1.3.0, v2.0.0)
    type: string
    required: true
---

# Release Validation Command

Execute the comprehensive 5-phase release validation workflow for HDIM Platform releases using autonomous Ralph Wiggum loops.

## What This Command Does

This command orchestrates a complete release validation workflow consisting of:

1. **Phase 1: Code Quality & Testing** - Entity-migration sync, test suite, HIPAA compliance
2. **Phase 2: Documentation & Examples** - Release notes, upgrade guide, version matrix
3. **Phase 3: Integration Testing** - Jaeger tracing, HikariCP config, Kafka, gateway auth
4. **Phase 4: Deployment Readiness** - Docker images, health checks, environment security
5. **Phase 5: Final Release Preparation** - Version matrix, git status, documentation review

**Total Tasks:** 20 validation tasks across 5 phases
**Estimated Duration:** 90-120 minutes (semi-automated)
**Automation:** Ralph Wiggum autonomous loops with phase confirmations

## Why This Matters

**Production Safety:** Ensures all critical validations pass before tagging a release:
- 100% entity-migration synchronization
- HIPAA compliance (cache TTL ≤5 min)
- Gateway trust authentication pattern
- HikariCP timing formula validation
- Kafka trace propagation
- No hardcoded secrets
- Complete documentation

**From Workflow Design:**
> "Semi-automated workflow with human confirmation at phase boundaries ensures thorough validation while maintaining autonomous execution within phases."

## Prerequisites

Before running this command:

1. **Ralph Wiggum Plugin Installed** - Required for autonomous loops
   ```bash
   # Verify installation
   ls ~/.claude/plugins/marketplaces/claude-code-plugins/plugins/ralph-wiggum
   ```

2. **All Changes Committed** - Working directory should be clean
   ```bash
   git status  # Should show no unstaged changes
   ```

3. **Docker Running** - Required for integration tests
   ```bash
   docker ps  # Should list running containers
   ```

4. **All Services Building** - Backend should compile
   ```bash
   cd backend && ./gradlew build
   ```

## Usage

```bash
# Execute release validation for version v1.3.0
/release-validation v1.3.0

# Execute release validation for version v2.0.0
/release-validation v2.0.0
```

## Examples

```bash
# Validate release v1.3.0
/release-validation v1.3.0

# Validate major version release
/release-validation v2.0.0

# Validate patch release
/release-validation v1.2.1
```

## Implementation

You are tasked with executing the HDIM Platform release validation workflow using the autonomous Ralph Wiggum loop system.

### Step 1: Validate Prerequisites

#### Check Version Format

```bash
VERSION="{{version}}"

# Validate format vX.Y.Z
if ! [[ "$VERSION" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "❌ ERROR: VERSION must be in format vX.Y.Z (e.g., v1.3.0)"
    exit 1
fi

echo "✅ Version format valid: $VERSION"
```

#### Verify Ralph Wiggum Plugin

```bash
# Check plugin is installed
if [ ! -d "$HOME/.claude/plugins/marketplaces/claude-code-plugins/plugins/ralph-wiggum" ]; then
    echo "❌ ERROR: Ralph Wiggum plugin not found"
    echo "Install via Claude Code marketplace"
    exit 1
fi

echo "✅ Ralph Wiggum plugin installed"
```

#### Check Git Status

```bash
cd /mnt/wdblack/dev/projects/hdim-master

# Verify working directory is clean
UNSTAGED=$(git status --porcelain | wc -l)
if [ "$UNSTAGED" -gt 0 ]; then
    echo "⚠️  WARNING: Uncommitted changes detected"
    echo "Consider committing changes before release validation"
    git status --short
fi
```

#### Verify Docker

```bash
# Check Docker daemon is running
if ! docker ps &>/dev/null; then
    echo "❌ ERROR: Docker is not running"
    echo "Start Docker and try again"
    exit 1
fi

echo "✅ Docker is running"
```

### Step 2: Launch Workflow Script

Execute the main workflow launcher script:

```bash
cd /mnt/wdblack/dev/projects/hdim-master

./scripts/release-validation/run-release-validation.sh {{version}}
```

**Expected Output:**
```
==========================================
HDIM Platform Release Validation
Version: {{version}}
Mode: Semi-Automated (5 Phase Confirmations)
==========================================

Step 1: Generating Release Documentation
Calling generate-release-docs.sh...
✓ RELEASE_NOTES_{{version}}.md created
✓ UPGRADE_GUIDE_{{version}}.md created
✓ VERSION_MATRIX_{{version}}.md created
✓ PRODUCTION_DEPLOYMENT_CHECKLIST_{{version}}.md created
✓ KNOWN_ISSUES_{{version}}.md created

Step 2: Loading Workflow Configuration
Workflow file: docs/releases/release-validation-workflow.json
Phases detected: 5

Step 3: Phase Execution Instructions
...
```

The script will:
1. Generate all release documentation from templates
2. Display phase execution instructions
3. Provide exact ralph-loop commands for each phase
4. Wait for confirmation between phases

### Step 3: Execute Phase 1 (Code Quality & Testing)

The workflow script will display the Phase 1 ralph-loop command. Copy and execute it:

```bash
/ralph-loop "Execute Phase 1 of the HDIM Platform release validation workflow for version {{version}}.

Read the workflow configuration from docs/releases/release-validation-workflow.json and execute all tasks in phase-1-validation:

1. Run scripts/release-validation/test-entity-migration-sync.sh
2. Run scripts/release-validation/run-full-test-suite.sh
3. Run scripts/release-validation/validate-hipaa-compliance.sh

For each validation script:
- Execute the script and capture output
- Review the generated markdown reports in docs/releases/{{version}}/validation/
- If any script exits with non-zero code, investigate failures
- Document results in phase execution log

When all Phase 1 tasks are complete and all validation scripts have passed (exit code 0), output exactly: PHASE_1_COMPLETE

Store all validation reports in docs/releases/{{version}}/validation/
Log execution details to docs/releases/{{version}}/logs/" --max-iterations 10 --completion-promise "PHASE_1_COMPLETE"
```

**Phase 1 Validation Scripts:**
- `test-entity-migration-sync.sh` - Validates JPA entities match Liquibase migrations
- `run-full-test-suite.sh` - Executes full Gradle test suite with coverage analysis
- `validate-hipaa-compliance.sh` - Checks PHI cache TTL, Cache-Control headers, @Audited annotations

**Success Criteria:** All scripts exit with code 0, PHASE_1_COMPLETE output

### Step 4: Execute Phase 2 (Documentation & Examples)

After Phase 1 completes and you confirm continuation, execute Phase 2:

```bash
/ralph-loop "Execute Phase 2 of the HDIM Platform release validation workflow for version {{version}}.

Read the workflow configuration from docs/releases/release-validation-workflow.json and execute all tasks in phase-2-documentation:

1. Read and review docs/releases/{{version}}/RELEASE_NOTES_{{version}}.md
   - Verify all auto-generated features from git log are accurate
   - Check that breaking changes are documented
   - Ensure known issues are listed

2. Read and review docs/releases/{{version}}/UPGRADE_GUIDE_{{version}}.md
   - Validate upgrade steps are complete and accurate
   - Check that environment variable changes are documented
   - Verify rollback procedure is included

3. Run scripts/release-validation/validate-version-matrix.sh
   - Ensure all 34 microservices are listed with correct versions
   - Verify infrastructure component versions
   - Check dependency versions match gradle/libs.versions.toml

When all Phase 2 tasks are complete and all documentation is validated, output exactly: PHASE_2_COMPLETE

Log review findings to docs/releases/{{version}}/logs/" --max-iterations 10 --completion-promise "PHASE_2_COMPLETE"
```

**Phase 2 Documentation Files:**
- `RELEASE_NOTES_{{version}}.md` - Auto-generated from git log and templates
- `UPGRADE_GUIDE_{{version}}.md` - Step-by-step upgrade instructions
- `VERSION_MATRIX_{{version}}.md` - Complete version inventory
- `PRODUCTION_DEPLOYMENT_CHECKLIST_{{version}}.md` - Production deployment checklist
- `KNOWN_ISSUES_{{version}}.md` - Known issues and limitations

**Success Criteria:** All documentation reviewed and validated, PHASE_2_COMPLETE output

### Step 5: Execute Phase 3 (Integration Testing)

After Phase 2 completes, execute Phase 3:

```bash
/ralph-loop "Execute Phase 3 of the HDIM Platform release validation workflow for version {{version}}.

Read the workflow configuration from docs/releases/release-validation-workflow.json and execute all tasks in phase-3-integration:

1. Run scripts/release-validation/validate-jaeger-integration.sh
   - Verify OTLP configuration in docker-compose.yml
   - Check Jaeger UI accessibility
   - Validate trace propagation across services

2. Run scripts/release-validation/validate-hikaricp-config.sh
   - Validate HikariCP timing formula: max-lifetime ≥ 6 × idle-timeout
   - Check traffic tier pool sizes (HIGH=50, MEDIUM=20, LOW=10)
   - Verify connection pool configuration

3. Run scripts/release-validation/validate-kafka-tracing.sh
   - Check Kafka interceptor configuration
   - Validate type headers are disabled
   - Verify no ClassNotFoundException in logs

4. Run scripts/release-validation/validate-gateway-trust-auth.sh
   - Validate SecurityConfig uses TrustedHeaderAuthFilter
   - Check filter ordering
   - Verify @PreAuthorize annotations on endpoints

For each validation script, review generated reports in docs/releases/{{version}}/validation/

When all Phase 3 tasks are complete and all integration tests pass, output exactly: PHASE_3_COMPLETE

Log execution details to docs/releases/{{version}}/logs/" --max-iterations 10 --completion-promise "PHASE_3_COMPLETE"
```

**Phase 3 Validation Scripts:**
- `validate-jaeger-integration.sh` - OpenTelemetry OTLP configuration
- `validate-hikaricp-config.sh` - HikariCP timing formula and pool sizes
- `validate-kafka-tracing.sh` - Kafka trace propagation and type headers
- `validate-gateway-trust-auth.sh` - Gateway trust authentication pattern

**Success Criteria:** All integration validations pass, PHASE_3_COMPLETE output

### Step 6: Execute Phase 4 (Deployment Readiness)

After Phase 3 completes, execute Phase 4:

```bash
/ralph-loop "Execute Phase 4 of the HDIM Platform release validation workflow for version {{version}}.

Read the workflow configuration from docs/releases/release-validation-workflow.json and execute all tasks in phase-4-deployment:

1. Run scripts/release-validation/build-and-validate-images.sh
   - Build all Docker images via docker compose build
   - Validate non-root user (UID 1001)
   - Check JVM optimization flags
   - Generate image manifest

2. Run scripts/release-validation/validate-health-checks.sh
   - Validate health check configuration in docker-compose.yml
   - Check interval, timeout, retries, start-period settings
   - Verify all services have health checks

3. Run scripts/release-validation/validate-environment-vars.sh
   - Scan for hardcoded secrets in docker-compose.yml
   - Ensure all sensitive values use environment variable interpolation
   - Check for password/secret/key without \${} placeholders

4. Review docs/releases/{{version}}/PRODUCTION_DEPLOYMENT_CHECKLIST_{{version}}.md
   - Validate all deployment steps are included
   - Check pre-deployment checklist completeness
   - Verify rollback procedure is documented

For each validation script, review generated reports in docs/releases/{{version}}/validation/

When all Phase 4 tasks are complete and deployment readiness is validated, output exactly: PHASE_4_COMPLETE

Log execution details to docs/releases/{{version}}/logs/" --max-iterations 10 --completion-promise "PHASE_4_COMPLETE"
```

**Phase 4 Validation Scripts:**
- `build-and-validate-images.sh` - Docker image build and security validation
- `validate-health-checks.sh` - Health check configuration validation
- `validate-environment-vars.sh` - Environment variable security scan

**Success Criteria:** All deployment validations pass, PHASE_4_COMPLETE output

### Step 7: Execute Phase 5 (Final Release Preparation)

After Phase 4 completes, execute the final phase:

```bash
/ralph-loop "Execute Phase 5 of the HDIM Platform release validation workflow for version {{version}}.

Read the workflow configuration from docs/releases/release-validation-workflow.json and execute all tasks in phase-5-release:

1. Run scripts/release-validation/validate-version-matrix.sh
   - Verify VERSION_MATRIX_{{version}}.md exists and is complete
   - Check all 34 microservices are documented
   - Validate infrastructure and dependency versions

2. Run scripts/release-validation/validate-git-status.sh
   - Check no unstaged changes
   - Verify no merge conflicts
   - Validate recent commits follow conventional commits format
   - Ensure branch is ready for tagging

3. Final Documentation Review
   - Review all 5 generated documentation files
   - Check for any TODO placeholders that need manual completion
   - Validate all version numbers are consistent

4. Git Tag Creation (MANUAL STEP - DO NOT EXECUTE AUTOMATICALLY)
   - Inform user to create git tag manually: git tag -a {{version}} -m \"Release {{version}}\"
   - Remind user to push tag: git push origin {{version}}

When all Phase 5 tasks are complete and release is ready, output exactly: PHASE_5_COMPLETE

Generate final workflow execution summary in docs/releases/{{version}}/logs/workflow-summary.md

Log all execution details to docs/releases/{{version}}/logs/" --max-iterations 10 --completion-promise "PHASE_5_COMPLETE"
```

**Phase 5 Validation Scripts:**
- `validate-version-matrix.sh` - Final version matrix validation
- `validate-git-status.sh` - Git repository readiness validation

**Success Criteria:** All final validations pass, workflow summary generated, PHASE_5_COMPLETE output

### Step 8: Review Final Summary

After all 5 phases complete, the workflow script will display a comprehensive summary:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✓ RELEASE VALIDATION WORKFLOW COMPLETE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Release Version: {{version}}

GENERATED ARTIFACTS:
  📄 Release Notes: docs/releases/{{version}}/RELEASE_NOTES_{{version}}.md
  📄 Upgrade Guide: docs/releases/{{version}}/UPGRADE_GUIDE_{{version}}.md
  📄 Version Matrix: docs/releases/{{version}}/VERSION_MATRIX_{{version}}.md
  📄 Deployment Checklist: docs/releases/{{version}}/PRODUCTION_DEPLOYMENT_CHECKLIST_{{version}}.md
  📄 Known Issues: docs/releases/{{version}}/KNOWN_ISSUES_{{version}}.md

VALIDATION REPORTS: (12 reports in docs/releases/{{version}}/validation/)
  📊 Entity-Migration Synchronization
  📊 Test Coverage (with JaCoCo metrics)
  📊 HIPAA Compliance
  📊 Jaeger Integration
  📊 HikariCP Configuration
  📊 Kafka Tracing
  📊 Gateway Trust Authentication
  📊 Docker Image Manifest
  📊 Health Check Configuration
  📊 Environment Variable Security
  📊 Version Matrix Validation
  📊 Git Repository Status

NEXT STEPS:
  1. Review all validation reports for any failures
  2. Address any issues found during validation
  3. Review and customize documentation files as needed
  4. Create git tag: git tag -a {{version}} -m "Release {{version}}"
  5. Push tag: git push origin {{version}}
  6. Prepare for production deployment
```

### Step 9: Generate Completion Summary

Create a final summary report:

```markdown
# Release Validation Summary - {{version}}

**Date:** [Current date]
**Status:** ✅ COMPLETE
**Total Phases:** 5
**Total Tasks:** 20
**Total Validations:** 12 scripts

## Phase Results

### Phase 1: Code Quality & Testing ✅
- Entity-Migration Sync: PASSED
- Full Test Suite: PASSED (X/X tests)
- HIPAA Compliance: PASSED

### Phase 2: Documentation & Examples ✅
- Release Notes: REVIEWED
- Upgrade Guide: REVIEWED
- Version Matrix: VALIDATED

### Phase 3: Integration Testing ✅
- Jaeger Integration: PASSED
- HikariCP Configuration: PASSED
- Kafka Tracing: PASSED
- Gateway Trust Auth: PASSED

### Phase 4: Deployment Readiness ✅
- Docker Images: BUILT & VALIDATED
- Health Checks: VALIDATED
- Environment Vars: SECURE

### Phase 5: Final Release Preparation ✅
- Version Matrix: VALIDATED
- Git Status: READY
- Documentation: COMPLETE

## Generated Artifacts

All artifacts available in `docs/releases/{{version}}/`

## Next Steps

1. ✅ Validation complete
2. ⏳ Create git tag: `git tag -a {{version}} -m "Release {{version}}"`
3. ⏳ Push tag: `git push origin {{version}}`
4. ⏳ Deploy to staging
5. ⏳ Deploy to production (follow PRODUCTION_DEPLOYMENT_CHECKLIST)
```

## Workflow Structure

**Phase 1: Code Quality & Testing (5 tasks)**
- Task 01: Entity-Migration Synchronization Validation
- Task 02: ddl-auto Setting Validation
- Task 03: Full Test Suite Execution
- Task 04: HIPAA Compliance Validation
- Task 05: Code Coverage Analysis

**Phase 2: Documentation & Examples (5 tasks)**
- Task 06: Review RELEASE_NOTES
- Task 07: Review UPGRADE_GUIDE
- Task 08: Validate VERSION_MATRIX
- Task 09: Review PRODUCTION_DEPLOYMENT_CHECKLIST
- Task 10: Review KNOWN_ISSUES

**Phase 3: Integration Testing (3 tasks)**
- Task 11: Jaeger Distributed Tracing Integration
- Task 12: HikariCP Connection Pool Configuration
- Task 13: Kafka Trace Propagation
- Task 14: Gateway Trust Authentication

**Phase 4: User Acceptance & Deployment (4 tasks)**
- Task 15: Docker Image Build & Security Validation
- Task 16: Health Check Configuration Validation
- Task 17: Environment Variable Security Validation
- Task 18: Production Deployment Checklist Review

**Phase 5: Final Release Preparation (3 tasks)**
- Task 19: Final VERSION_MATRIX Validation
- Task 20: Git Repository Status Validation
- Task 21: Final Documentation Review
- Task 22: Git Tag Creation (Manual)

## Validation Scripts Reference

All scripts located in `scripts/release-validation/`:

**Phase 1 Scripts:**
- `test-entity-migration-sync.sh` - Entity-migration synchronization
- `run-full-test-suite.sh` - Full Gradle test suite + JaCoCo coverage
- `validate-hipaa-compliance.sh` - PHI cache TTL, headers, audit logging

**Phase 3 Scripts:**
- `validate-jaeger-integration.sh` - OpenTelemetry OTLP configuration
- `validate-hikaricp-config.sh` - Connection pool timing formula
- `validate-kafka-tracing.sh` - Kafka interceptors and type headers
- `validate-gateway-trust-auth.sh` - Gateway trust authentication pattern

**Phase 4 Scripts:**
- `build-and-validate-images.sh` - Docker image build and security
- `validate-health-checks.sh` - Health check configuration
- `validate-environment-vars.sh` - Environment variable security scan

**Phase 5 Scripts:**
- `validate-version-matrix.sh` - Version matrix completeness
- `validate-git-status.sh` - Git repository readiness

**Documentation Scripts:**
- `generate-release-docs.sh` - Auto-generates all 5 documentation files

## Common Validation Failures

### Entity-Migration Sync Failure

**Error:**
```
❌ Entity-migration validation FAILED
Missing table: appointments
```

**Fix:**
1. Create Liquibase migration for missing table
2. Re-run validation: `/validate-schema SERVICE_NAME`
3. Continue release validation: `/release-validation {{version}}`

### HIPAA Compliance Failure

**Error:**
```
❌ HIPAA compliance validation FAILED
Cache TTL exceeds 5 minutes: patient_cache (600000ms)
```

**Fix:**
1. Update cache configuration in application.yml
2. Set TTL ≤ 300000ms (5 minutes)
3. Re-run validation script
4. Continue release validation

### HikariCP Timing Violation

**Error:**
```
❌ HikariCP timing formula violation
max-lifetime (300000) < 6 × idle-timeout (60000)
Required: 360000
```

**Fix:**
1. Update HikariCP configuration
2. Ensure `max-lifetime ≥ 6 × idle-timeout`
3. Re-run validation script
4. Continue release validation

### Hardcoded Secrets Detected

**Error:**
```
❌ Hardcoded secrets detected in docker-compose.yml
Line 42: POSTGRES_PASSWORD=hardcodedpassword
```

**Fix:**
1. Replace hardcoded value with environment variable
2. Update: `POSTGRES_PASSWORD=${POSTGRES_PASSWORD}`
3. Re-run validation script
4. Continue release validation

## Troubleshooting

### Ralph Wiggum Plugin Not Found

**Error:** Ralph Wiggum plugin not installed

**Fix:**
1. Install Ralph Wiggum plugin from Claude Code marketplace
2. Verify installation: `ls ~/.claude/plugins/marketplaces/claude-code-plugins/plugins/ralph-wiggum`
3. Retry command

### Workflow Script Not Found

**Error:** `run-release-validation.sh` not found

**Fix:**
1. Ensure you're in project root: `/mnt/wdblack/dev/projects/hdim-master`
2. Check script exists: `ls -la scripts/release-validation/run-release-validation.sh`
3. Make executable: `chmod +x scripts/release-validation/run-release-validation.sh`

### Docker Not Running

**Error:** Docker daemon not running

**Fix:**
1. Start Docker: `systemctl start docker` (Linux) or Docker Desktop (Mac/Windows)
2. Verify: `docker ps`
3. Retry command

### Validation Script Fails

**Error:** Validation script exits with non-zero code

**Fix:**
1. Review validation report in `docs/releases/{{version}}/validation/`
2. Identify specific failure(s)
3. Address failures (see Common Validation Failures above)
4. Re-run specific validation script
5. Continue with release validation

## Related Commands

- `/validate-schema` - Run entity-migration validation for specific service
- `/clean-build` - Clean build before release validation

## Related Skills

- `database-migrations` - Liquibase best practices
- `gateway-trust-auth` - Gateway trust authentication pattern

## Related Agents

- `migration-validator` - Auto-validates entity-migration synchronization
- `docker-agent` - Docker build and deployment expertise

## Documentation

**Workflow Configuration:**
- `docs/releases/release-validation-workflow.json` - Complete workflow definition

**Scripts:**
- `scripts/release-validation/run-release-validation.sh` - Main workflow launcher
- `scripts/release-validation/generate-release-docs.sh` - Documentation generator
- `scripts/release-validation/*.sh` - Individual validation scripts (12 scripts)

**Templates:**
- `docs/releases/templates/` - All documentation templates (5 files)

**Generated Artifacts (per release):**
- `docs/releases/{{version}}/` - All release documentation
- `docs/releases/{{version}}/validation/` - All validation reports
- `docs/releases/{{version}}/logs/` - Execution logs and summaries

**CLAUDE.md References:**
- Entity-Migration Synchronization section
- HIPAA Compliance Requirements section
- Gateway Trust Authentication Architecture section
- Database Architecture & Schema Management section
- Distributed Tracing section

## Best Practices

### 1. Run Before Tagging

**ALWAYS** run release validation before creating git tag. This prevents releasing untested code.

### 2. Address Failures Immediately

**NEVER** skip validation failures. Fix immediately and re-run validation.

### 3. Review All Documentation

**ALWAYS** manually review all 5 generated documentation files. Templates auto-generate content but may need customization.

### 4. Verify Git Status Clean

**ENSURE** working directory is clean before starting. Commit all changes first.

### 5. Test in Staging First

**ALWAYS** deploy to staging environment first, following the PRODUCTION_DEPLOYMENT_CHECKLIST.

### 6. Keep Validation Reports

**PRESERVE** all validation reports in version control. They serve as release audit trail.

## Workflow Execution Logs

All execution logs saved to:
```
docs/releases/{{version}}/logs/validation-execution-[timestamp].log
```

**Log Contents:**
- Script execution timestamps
- Validation results (pass/fail)
- Error messages and stack traces
- Phase completion confirmations
- Final workflow summary

## CI/CD Integration (Future)

**Planned:** GitHub Actions workflow to automate release validation on tag creation.

**Current:** Manual execution via `/release-validation` command.

## Success Criteria

Release validation is considered **COMPLETE** when:

- ✅ All 5 phases execute successfully
- ✅ All 12 validation scripts exit with code 0
- ✅ All 5 documentation files generated and reviewed
- ✅ No unstaged changes in git
- ✅ PHASE_5_COMPLETE output received
- ✅ Workflow summary generated

After completion, you're ready to:
1. Create git tag
2. Push to remote
3. Deploy to staging
4. Deploy to production (following checklist)
