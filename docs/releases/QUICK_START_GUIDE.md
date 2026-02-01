# Release Validation Workflow - Quick Start Guide

**Version:** 1.0.0
**Last Updated:** 2026-01-20

---

## 🚀 Quick Start (5 Minutes)

### Step 1: Run the Command

```bash
# Option A: Via HDIM Accelerator plugin (recommended)
/release-validation v1.3.0

# Option B: Direct script execution
./scripts/release-validation/run-release-validation.sh v1.3.0
```

### Step 2: Follow the Instructions

The script will:
1. ✅ Auto-generate all release documentation
2. 📋 Display copy/paste ralph-loop commands for each phase
3. ⏸️ Wait for your confirmation between phases

### Step 3: Execute Each Phase

**You'll see instructions like this:**

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PHASE 1: Code Quality & Testing Validation
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

COPY AND PASTE THIS COMMAND INTO CLAUDE CODE:

/ralph-loop "Execute Phase 1 of the HDIM Platform release validation..."
--max-iterations 10 --completion-promise "PHASE_1_COMPLETE"
```

**Simply:**
1. Copy the `/ralph-loop` command
2. Paste into Claude Code
3. Wait for `PHASE_1_COMPLETE` output
4. Press ENTER to continue to next phase

### Step 4: Review Results

After all 5 phases complete, review:

```bash
# Check validation reports
ls -lh docs/releases/v1.3.0/validation/

# Read specific reports
cat docs/releases/v1.3.0/validation/entity-migration-report.md
cat docs/releases/v1.3.0/validation/test-coverage-report.md
cat docs/releases/v1.3.0/validation/hipaa-compliance-report.md
```

### Step 5: Create Git Tag

When all validations pass:

```bash
git tag -a v1.3.0 -m "Release v1.3.0"
git push origin v1.3.0
```

---

## 📊 What Gets Validated

### Phase 1: Code Quality & Testing (5 min)
- ✅ Entity-migration synchronization (JPA ↔ Liquibase)
- ✅ Full test suite (1,577+ tests)
- ✅ HIPAA compliance (cache TTL ≤5 min)
- ✅ Code coverage (≥70%)

### Phase 2: Documentation & Examples (10 min)
- ✅ Auto-generates 5 release documents
- ✅ Validates version matrix completeness
- ✅ Reviews upgrade guide

### Phase 3: Integration Testing (15 min)
- ✅ Jaeger distributed tracing
- ✅ HikariCP timing formula (max-lifetime ≥ 6× idle-timeout)
- ✅ Kafka trace propagation
- ✅ Gateway trust authentication

### Phase 4: Deployment Readiness (10 min)
- ✅ Docker image security (non-root user)
- ✅ Health check configuration
- ✅ Environment variable security (no hardcoded secrets)

### Phase 5: Final Release Preparation (5 min)
- ✅ Version matrix validation
- ✅ Git repository readiness
- ✅ Final documentation review

**Total Time:** ~90-120 minutes (vs 4-6 hours manual)

---

## 📁 Generated Artifacts

After completion, you'll have:

```
docs/releases/v1.3.0/
├── RELEASE_NOTES_v1.3.0.md               # Auto-generated from git log
├── UPGRADE_GUIDE_v1.3.0.md               # Step-by-step upgrade instructions
├── VERSION_MATRIX_v1.3.0.md              # Complete dependency inventory
├── PRODUCTION_DEPLOYMENT_CHECKLIST_v1.3.0.md  # Production deployment checklist
├── KNOWN_ISSUES_v1.3.0.md                # Known issues template
├── validation/
│   ├── entity-migration-report.md        # JPA/Liquibase sync results
│   ├── test-coverage-report.md           # Test suite results + coverage
│   ├── hipaa-compliance-report.md        # HIPAA compliance check
│   ├── jaeger-integration-report.md      # Distributed tracing validation
│   ├── hikaricp-config-report.md         # Connection pool validation
│   ├── kafka-tracing-report.md           # Kafka trace propagation
│   ├── gateway-trust-auth-report.md      # Gateway auth pattern validation
│   ├── docker-image-manifest.json        # Docker image inventory
│   ├── health-check-report.md            # Health check configuration
│   ├── environment-security-report.md    # Environment variable security
│   ├── version-matrix-validation.md      # Version matrix completeness
│   └── git-status-report.md              # Git repository status
└── logs/
    ├── validation-execution-[timestamp].log  # Complete execution log
    └── workflow-summary.md               # Final workflow summary
```

**Total:** 19 artifacts (5 docs + 12 reports + 2 logs)

---

## 🔧 Troubleshooting

### Issue: "Ralph Wiggum plugin not found"

**Fix:**
```bash
# Install Ralph Wiggum from Claude Code marketplace
# Verify: ls ~/.claude/plugins/marketplaces/claude-code-plugins/plugins/ralph-wiggum
```

### Issue: "Validation script fails"

**Fix:**
1. Review the report: `cat docs/releases/v1.3.0/validation/[REPORT_NAME].md`
2. Fix the issue identified in the report
3. Re-run the specific validation script:
   ```bash
   ./scripts/release-validation/validate-hipaa-compliance.sh v1.3.0
   ```
4. Continue with release validation

### Issue: "Documentation has placeholders"

**Fix:**
1. Review generated docs: `ls docs/releases/v1.3.0/*.md`
2. Manually update placeholders like `{FEATURE_NAME}`, `{DESCRIPTION}`, etc.
3. This is expected - templates include placeholders for manual customization

### Issue: "Unstaged changes detected"

**Fix:**
```bash
# This is a warning, not an error
# Commit your changes before tagging:
git add .
git commit -m "feat: Prepare release v1.3.0"

# Then re-run git status validation:
./scripts/release-validation/validate-git-status.sh v1.3.0
```

---

## 💡 Pro Tips

### Tip 1: Run Validation Before Finalizing

Run the validation workflow **before** you think you're ready to release. It will catch issues early.

```bash
# Run validation on your current branch
/release-validation v1.3.0-rc1  # Release candidate

# Fix any issues found
# ...

# Run final validation when ready
/release-validation v1.3.0
```

### Tip 2: Keep Validation Reports

Validation reports serve as an audit trail. Keep them in version control:

```bash
# Add validation reports to git
git add docs/releases/v1.3.0/
git commit -m "docs: Add v1.3.0 release validation reports"
```

### Tip 3: Customize Documentation Templates

The templates in `docs/releases/templates/` can be customized:

```bash
# Edit templates to match your organization's standards
vi docs/releases/templates/RELEASE_NOTES_TEMPLATE.md

# Next release will use updated template
/release-validation v1.4.0
```

### Tip 4: Use Test Versions

Test the workflow with `-test` or `-rc` versions first:

```bash
# Test run
/release-validation v1.3.0-test

# Review generated artifacts
ls docs/releases/v1.3.0-test/

# Clean up test artifacts
rm -rf docs/releases/v1.3.0-test/

# Production run
/release-validation v1.3.0
```

### Tip 5: Re-run Individual Validations

If a validation fails, fix the issue and re-run just that validation:

```bash
# Re-run HIPAA validation after fixing cache TTL
./scripts/release-validation/validate-hipaa-compliance.sh v1.3.0

# Re-run entity-migration validation after fixing schema drift
./scripts/release-validation/test-entity-migration-sync.sh v1.3.0

# No need to re-run entire workflow
```

---

## 📚 Additional Resources

**Complete Documentation:**
- Implementation Summary: `docs/releases/RELEASE_VALIDATION_IMPLEMENTATION_SUMMARY.md`
- Plugin Command: `.claude/plugins/hdim-accelerator/commands/release-validation.md`
- Plugin README: `.claude/plugins/hdim-accelerator/README.md`

**HDIM Platform Documentation:**
- Entity-Migration Guide: `backend/docs/ENTITY_MIGRATION_GUIDE.md`
- HIPAA Compliance: `backend/HIPAA-CACHE-COMPLIANCE.md`
- Gateway Trust Auth: `backend/docs/GATEWAY_TRUST_ARCHITECTURE.md`
- Database Runbook: `backend/docs/DATABASE_MIGRATION_RUNBOOK.md`

**Validation Scripts:**
- All scripts: `scripts/release-validation/*.sh`
- Workflow JSON: `docs/releases/release-validation-workflow.json`
- Templates: `docs/releases/templates/*.md`

---

## ✅ Success Criteria

You're ready to release when:

- ✅ All 5 phases complete successfully
- ✅ All 12 validation scripts exit with code 0
- ✅ All 5 documentation files generated and reviewed
- ✅ No unstaged changes in git (or all changes committed)
- ✅ PHASE_5_COMPLETE output received
- ✅ Workflow summary generated

**Then:**
1. Create git tag: `git tag -a v1.3.0 -m "Release v1.3.0"`
2. Push tag: `git push origin v1.3.0`
3. Deploy to staging (follow PRODUCTION_DEPLOYMENT_CHECKLIST)
4. Deploy to production (after staging validation)

---

## 🎯 Next Steps

After successful validation:

1. **Create GitHub Release**
   - Use generated RELEASE_NOTES_v1.3.0.md as release description
   - Attach Docker image manifests
   - Link to documentation

2. **Deploy to Staging**
   - Follow PRODUCTION_DEPLOYMENT_CHECKLIST_v1.3.0.md
   - Run smoke tests
   - Verify all health checks pass

3. **Deploy to Production**
   - Schedule maintenance window
   - Execute deployment steps
   - Monitor for 24-48 hours

4. **Post-Release**
   - Update KNOWN_ISSUES if new issues discovered
   - Create v1.3.1 hotfix branch if needed
   - Begin planning v1.4.0 features

---

**Status:** ✅ Ready to Use

**First Release:** Run `/release-validation v1.3.0` when ready!

---

*For questions or issues, see the complete implementation summary or contact the HDIM platform team.*
