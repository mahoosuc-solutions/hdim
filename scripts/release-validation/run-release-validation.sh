#!/bin/bash
# HDIM Platform Release Validation Workflow Launcher
# Orchestrates 5-phase release validation using Ralph Wiggum loops
set -euo pipefail

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

VERSION="${1:-}"
if [ -z "$VERSION" ]; then
    echo -e "${RED}ERROR: VERSION required${NC}"
    echo "Usage: $0 v1.3.0"
    exit 1
fi

# Validate version format (vX.Y.Z or vX.Y.Z-suffix for testing)
if ! [[ "$VERSION" =~ ^v[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9]+)?$ ]]; then
    echo -e "${RED}ERROR: VERSION must be in format vX.Y.Z or vX.Y.Z-suffix (e.g., v1.3.0, v1.3.0-test)${NC}"
    exit 1
fi

echo "=========================================="
echo "HDIM Platform Release Validation"
echo "Version: $VERSION"
echo "Mode: Semi-Automated (5 Phase Confirmations)"
echo "=========================================="
echo ""

cd "$(dirname "$0")/../.." || exit 1

RELEASE_DIR="docs/releases/${VERSION}"
VALIDATION_DIR="$RELEASE_DIR/validation"
WORKFLOW_FILE="docs/releases/release-validation-workflow.json"
SCRIPTS_DIR="scripts/release-validation"

# Create release directory structure
mkdir -p "$VALIDATION_DIR"
mkdir -p "$RELEASE_DIR/logs"

LOG_FILE="$RELEASE_DIR/logs/validation-execution-$(date +%Y%m%d-%H%M%S).log"

log() {
    echo "$1" | tee -a "$LOG_FILE"
}

log_section() {
    log ""
    log "=========================================="
    log "$1"
    log "=========================================="
    log ""
}

# Step 1: Generate release documentation
log_section "Step 1: Generating Release Documentation"
log "Calling generate-release-docs.sh..."

if ! "$SCRIPTS_DIR/generate-release-docs.sh" "$VERSION" | tee -a "$LOG_FILE"; then
    echo -e "${RED}ERROR: Documentation generation failed${NC}"
    exit 1
fi

log ""
log -e "${GREEN}✓${NC} Release documentation generated successfully"
log ""

# Step 2: Validate workflow JSON exists
if [ ! -f "$WORKFLOW_FILE" ]; then
    echo -e "${RED}ERROR: Workflow file not found: $WORKFLOW_FILE${NC}"
    exit 1
fi

log_section "Step 2: Loading Workflow Configuration"
log "Workflow file: $WORKFLOW_FILE"

# Extract phase count (simple grep-based parsing for bash)
PHASE_COUNT=$(grep -c '"phase_id"' "$WORKFLOW_FILE" || echo "0")
log "Phases detected: $PHASE_COUNT"

if [ "$PHASE_COUNT" != "5" ]; then
    echo -e "${YELLOW}WARNING: Expected 5 phases, found $PHASE_COUNT${NC}"
fi

log ""

# Step 3: Phase execution instructions
log_section "Step 3: Phase Execution Instructions"

cat <<'EOF' | tee -a "$LOG_FILE"

This workflow uses the Ralph Wiggum plugin to execute 5 autonomous validation phases.

HOW IT WORKS:
1. Each phase runs as a separate ralph-loop with a completion promise
2. Claude will autonomously execute all tasks within the phase
3. You will be prompted for confirmation between phases
4. All validation reports will be saved to docs/releases/v{VERSION}/validation/

REQUIREMENTS:
- Ralph Wiggum plugin must be installed
- Claude Code session must be active
- All validation scripts must be executable

WORKFLOW STRUCTURE:
- Phase 1: Code Quality & Testing (5 tasks)
- Phase 2: Documentation & Examples (5 tasks)
- Phase 3: Integration Testing (3 tasks)
- Phase 4: User Acceptance & Deployment (4 tasks)
- Phase 5: Release & Communication (3 tasks)

EOF

echo ""
read -p "Press ENTER to begin Phase 1..." -r
echo ""

# Phase 1: Code Quality & Testing Validation
log_section "PHASE 1: Code Quality & Testing Validation"

cat <<EOF | tee -a "$LOG_FILE"

${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}
${BLUE}PHASE 1: Code Quality & Testing Validation${NC}
${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}

TASKS:
  01. Entity-Migration Synchronization Validation
  02. ddl-auto Setting Validation
  03. Full Test Suite Execution
  04. HIPAA Compliance Validation
  05. Code Coverage Analysis

VALIDATION SCRIPTS:
  - scripts/release-validation/test-entity-migration-sync.sh
  - scripts/release-validation/run-full-test-suite.sh
  - scripts/release-validation/validate-hipaa-compliance.sh

COMPLETION PROMISE: "PHASE_1_COMPLETE"

${YELLOW}COPY AND PASTE THIS COMMAND INTO CLAUDE CODE:${NC}

/ralph-loop "Execute Phase 1 of the HDIM Platform release validation workflow for version $VERSION.

Read the workflow configuration from docs/releases/release-validation-workflow.json and execute all tasks in phase-1-validation:

1. Run scripts/release-validation/test-entity-migration-sync.sh
2. Run scripts/release-validation/run-full-test-suite.sh
3. Run scripts/release-validation/validate-hipaa-compliance.sh

For each validation script:
- Execute the script and capture output
- Review the generated markdown reports in docs/releases/$VERSION/validation/
- If any script exits with non-zero code, investigate failures
- Document results in phase execution log

When all Phase 1 tasks are complete and all validation scripts have passed (exit code 0), output exactly: PHASE_1_COMPLETE

Store all validation reports in docs/releases/$VERSION/validation/
Log execution details to docs/releases/$VERSION/logs/" --max-iterations 10 --completion-promise "PHASE_1_COMPLETE"

${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}

EOF

read -p "After Phase 1 completes, press ENTER to continue to Phase 2..." -r
echo ""

# Phase 2: Documentation & Examples
log_section "PHASE 2: Documentation & Examples"

cat <<EOF | tee -a "$LOG_FILE"

${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}
${BLUE}PHASE 2: Documentation & Examples${NC}
${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}

TASKS:
  06. Review and customize RELEASE_NOTES_$VERSION.md
  07. Review and customize UPGRADE_GUIDE_$VERSION.md
  08. Validate VERSION_MATRIX_$VERSION.md completeness

DOCUMENTATION FILES:
  - docs/releases/$VERSION/RELEASE_NOTES_$VERSION.md
  - docs/releases/$VERSION/UPGRADE_GUIDE_$VERSION.md
  - docs/releases/$VERSION/VERSION_MATRIX_$VERSION.md
  - docs/releases/$VERSION/PRODUCTION_DEPLOYMENT_CHECKLIST_$VERSION.md
  - docs/releases/$VERSION/KNOWN_ISSUES_$VERSION.md

COMPLETION PROMISE: "PHASE_2_COMPLETE"

${YELLOW}COPY AND PASTE THIS COMMAND INTO CLAUDE CODE:${NC}

/ralph-loop "Execute Phase 2 of the HDIM Platform release validation workflow for version $VERSION.

Read the workflow configuration from docs/releases/release-validation-workflow.json and execute all tasks in phase-2-documentation:

1. Read and review docs/releases/$VERSION/RELEASE_NOTES_$VERSION.md
   - Verify all auto-generated features from git log are accurate
   - Check that breaking changes are documented
   - Ensure known issues are listed

2. Read and review docs/releases/$VERSION/UPGRADE_GUIDE_$VERSION.md
   - Validate upgrade steps are complete and accurate
   - Check that environment variable changes are documented
   - Verify rollback procedure is included

3. Run scripts/release-validation/validate-version-matrix.sh
   - Ensure all 34 microservices are listed with correct versions
   - Verify infrastructure component versions
   - Check dependency versions match gradle/libs.versions.toml

When all Phase 2 tasks are complete and all documentation is validated, output exactly: PHASE_2_COMPLETE

Log review findings to docs/releases/$VERSION/logs/" --max-iterations 10 --completion-promise "PHASE_2_COMPLETE"

${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}

EOF

read -p "After Phase 2 completes, press ENTER to continue to Phase 3..." -r
echo ""

# Phase 3: Integration Testing
log_section "PHASE 3: Integration Testing"

cat <<EOF | tee -a "$LOG_FILE"

${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}
${BLUE}PHASE 3: Integration Testing${NC}
${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}

TASKS:
  09. Jaeger Distributed Tracing Integration Validation
  10. HikariCP Connection Pool Configuration Validation
  11. Kafka Trace Propagation Validation
  12. Gateway Trust Authentication Validation

VALIDATION SCRIPTS:
  - scripts/release-validation/validate-jaeger-integration.sh
  - scripts/release-validation/validate-hikaricp-config.sh
  - scripts/release-validation/validate-kafka-tracing.sh
  - scripts/release-validation/validate-gateway-trust-auth.sh

COMPLETION PROMISE: "PHASE_3_COMPLETE"

${YELLOW}COPY AND PASTE THIS COMMAND INTO CLAUDE CODE:${NC}

/ralph-loop "Execute Phase 3 of the HDIM Platform release validation workflow for version $VERSION.

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

For each validation script, review generated reports in docs/releases/$VERSION/validation/

When all Phase 3 tasks are complete and all integration tests pass, output exactly: PHASE_3_COMPLETE

Log execution details to docs/releases/$VERSION/logs/" --max-iterations 10 --completion-promise "PHASE_3_COMPLETE"

${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}

EOF

read -p "After Phase 3 completes, press ENTER to continue to Phase 4..." -r
echo ""

# Phase 4: User Acceptance & Deployment Readiness
log_section "PHASE 4: User Acceptance & Deployment Readiness"

cat <<EOF | tee -a "$LOG_FILE"

${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}
${BLUE}PHASE 4: User Acceptance & Deployment Readiness${NC}
${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}

TASKS:
  13. Docker Image Build & Security Validation
  14. Health Check Configuration Validation
  15. Environment Variable Security Validation
  16. Production Deployment Checklist Review

VALIDATION SCRIPTS:
  - scripts/release-validation/build-and-validate-images.sh
  - scripts/release-validation/validate-health-checks.sh
  - scripts/release-validation/validate-environment-vars.sh

COMPLETION PROMISE: "PHASE_4_COMPLETE"

${YELLOW}COPY AND PASTE THIS COMMAND INTO CLAUDE CODE:${NC}

/ralph-loop "Execute Phase 4 of the HDIM Platform release validation workflow for version $VERSION.

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

4. Review docs/releases/$VERSION/PRODUCTION_DEPLOYMENT_CHECKLIST_$VERSION.md
   - Validate all deployment steps are included
   - Check pre-deployment checklist completeness
   - Verify rollback procedure is documented

For each validation script, review generated reports in docs/releases/$VERSION/validation/

When all Phase 4 tasks are complete and deployment readiness is validated, output exactly: PHASE_4_COMPLETE

Log execution details to docs/releases/$VERSION/logs/" --max-iterations 10 --completion-promise "PHASE_4_COMPLETE"

${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}

EOF

read -p "After Phase 4 completes, press ENTER to continue to Phase 5..." -r
echo ""

# Phase 5: Final Release Preparation
log_section "PHASE 5: Final Release Preparation"

cat <<EOF | tee -a "$LOG_FILE"

${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}
${BLUE}PHASE 5: Final Release Preparation${NC}
${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}

TASKS:
  17. Final VERSION_MATRIX validation
  18. Git repository status validation
  19. Final release documentation review
  20. Create git tag for release

VALIDATION SCRIPTS:
  - scripts/release-validation/validate-version-matrix.sh
  - scripts/release-validation/validate-git-status.sh

COMPLETION PROMISE: "PHASE_5_COMPLETE"

${YELLOW}COPY AND PASTE THIS COMMAND INTO CLAUDE CODE:${NC}

/ralph-loop "Execute Phase 5 of the HDIM Platform release validation workflow for version $VERSION.

Read the workflow configuration from docs/releases/release-validation-workflow.json and execute all tasks in phase-5-release:

1. Run scripts/release-validation/validate-version-matrix.sh
   - Verify VERSION_MATRIX_$VERSION.md exists and is complete
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
   - Inform user to create git tag manually: git tag -a $VERSION -m \"Release $VERSION\"
   - Remind user to push tag: git push origin $VERSION

When all Phase 5 tasks are complete and release is ready, output exactly: PHASE_5_COMPLETE

Generate final workflow execution summary in docs/releases/$VERSION/logs/workflow-summary.md

Log all execution details to docs/releases/$VERSION/logs/" --max-iterations 10 --completion-promise "PHASE_5_COMPLETE"

${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}

EOF

read -p "After Phase 5 completes, press ENTER to view final summary..." -r
echo ""

# Final Summary
log_section "RELEASE VALIDATION WORKFLOW COMPLETE"

cat <<EOF | tee -a "$LOG_FILE"

${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}
${GREEN}✓ RELEASE VALIDATION WORKFLOW COMPLETE${NC}
${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}

Release Version: $VERSION
Execution Log: $LOG_FILE

GENERATED ARTIFACTS:
  📄 Release Notes: docs/releases/$VERSION/RELEASE_NOTES_$VERSION.md
  📄 Upgrade Guide: docs/releases/$VERSION/UPGRADE_GUIDE_$VERSION.md
  📄 Version Matrix: docs/releases/$VERSION/VERSION_MATRIX_$VERSION.md
  📄 Deployment Checklist: docs/releases/$VERSION/PRODUCTION_DEPLOYMENT_CHECKLIST_$VERSION.md
  📄 Known Issues: docs/releases/$VERSION/KNOWN_ISSUES_$VERSION.md

VALIDATION REPORTS:
  📊 Entity-Migration: docs/releases/$VERSION/validation/entity-migration-report.md
  📊 Test Coverage: docs/releases/$VERSION/validation/test-coverage-report.md
  📊 HIPAA Compliance: docs/releases/$VERSION/validation/hipaa-compliance-report.md
  📊 Jaeger Integration: docs/releases/$VERSION/validation/jaeger-integration-report.md
  📊 HikariCP Config: docs/releases/$VERSION/validation/hikaricp-config-report.md
  📊 Kafka Tracing: docs/releases/$VERSION/validation/kafka-tracing-report.md
  📊 Gateway Auth: docs/releases/$VERSION/validation/gateway-trust-auth-report.md
  📊 Docker Images: docs/releases/$VERSION/validation/docker-image-manifest.json
  📊 Health Checks: docs/releases/$VERSION/validation/health-check-report.md
  📊 Environment Vars: docs/releases/$VERSION/validation/environment-security-report.md
  📊 Version Matrix: docs/releases/$VERSION/validation/version-matrix-validation.md
  📊 Git Status: docs/releases/$VERSION/validation/git-status-report.md

NEXT STEPS:
  1. Review all validation reports for any failures
  2. Address any issues found during validation
  3. Review and customize documentation files as needed
  4. Create git tag: git tag -a $VERSION -m "Release $VERSION"
  5. Push tag: git push origin $VERSION
  6. Prepare for production deployment

${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}

EOF

echo ""
echo -e "${GREEN}Release validation workflow completed successfully!${NC}"
echo -e "Review the execution log at: ${CYAN}$LOG_FILE${NC}"
echo ""

exit 0
