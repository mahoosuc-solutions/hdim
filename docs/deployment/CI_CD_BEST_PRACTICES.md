# CI/CD Best Practices for HDIM Phase 7

**Date:** February 1, 2026
**Status:** Production-Ready
**Audience:** Development Team

---

## Executive Summary

This guide documents best practices for working with the Phase 7 CI/CD pipeline. It covers parallel job execution, change detection patterns, caching strategies, monitoring, and troubleshooting.

---

## Table of Contents

1. Parallel Job Execution
2. Change Detection Patterns
3. Caching Strategies
4. Performance Monitoring
5. Team Workflows
6. Troubleshooting Guide
7. Common Scenarios
8. Performance Tips

---

## 1. Parallel Job Execution

### Understanding Parallel Execution

**Before Phase 7:**
```
Build (10m) → Test-Unit (2m) → Test-Fast (3m) → Test-Integration (3m)
→ Test-Slow (5m) → Validation (10m) → Docker (86m)
= Sequential: 40 minutes
```

**After Phase 7:**
```
Build (10m) → [Test-Unit (2m), Test-Fast (3m), Test-Integration (3m),
Test-Slow (5m)] parallel → [Validation jobs] parallel → Docker (20m)
= Parallel: 23-25 minutes
```

### Key Concept: Job Dependencies

Jobs run in parallel when they have no hard dependencies. The workflow uses GitHub Actions job dependencies:

```yaml
jobs:
  build:
    # Critical path: must complete before tests start
    runs-on: ubuntu-latest
    outputs:
      status: ${{ job.status }}

  test-unit:
    needs: build  # Hard dependency
    if: ${{ needs.build.result == 'success' || needs.build.result == 'skipped' }}

  test-fast:
    needs: build  # Hard dependency
    if: ${{ needs.build.result == 'success' || needs.build.result == 'skipped' }}
```

### Best Practices for Parallel Jobs

**1. Keep Job Times Balanced**
- If one test job takes 10min and another takes 1min, max time is still 10min
- Ideal: All parallel jobs complete at similar times
- Review: Identify slow tests and optimize

**2. Minimize Dependencies**
- Reduce hard dependencies to critical paths only
- Use soft dependencies (if: conditions) for optionally dependent jobs
- Parallel execution happens when no hard dependency exists

**3. Manage Artifacts Efficiently**
- Build produces artifacts needed by tests
- Tests download same artifacts multiple times
- Use caching to avoid redundant downloads

**4. Job Timeouts**
- Set realistic timeouts for each job type
- Account for network latency, resource contention
- Build: 20 minutes (includes Gradle download)
- Tests: 15 minutes each (with overhead)
- Docker: 60 minutes (per service)

### Monitoring Parallel Execution

```bash
# Check job execution timeline
# In GitHub Actions → Workflow run → View timing graph

# Key metrics to watch:
# - Total workflow time
# - Job start/end times
# - Artifact upload/download times
# - Resource utilization
```

---

## 2. Change Detection Patterns

### How Change Detection Works

Phase 7 uses `dorny/paths-filter` to detect which services changed:

```yaml
- uses: dorny/paths-filter@v2
  id: changes
  with:
    filters: |
      patient-service:
        - 'backend/modules/services/patient-service/**'
      care-gap-service:
        - 'backend/modules/services/care-gap-service/**'
      # ... 21 total filters
```

Output: `changes.patient-service` = 'true' or 'false'

### The 21 Change Detection Outputs

**Infrastructure & Configuration:**
- `backend-changed` - Any backend file
- `infrastructure-changed` - Workflows, docker-compose, gradle config
- `gradle-changed` - Gradle build files

**Shared Modules (affects all services):**
- `shared-changed` - Audit, messaging, security libs

**Core Services:**
- `patient-service-changed`
- `care-gap-service-changed`
- `quality-service-changed`
- `fhir-service-changed`
- `cql-engine-changed`

**Event Services:**
- `patient-event-service-changed`
- `care-gap-event-service-changed`
- `quality-measure-event-service-changed`
- `clinical-workflow-service-changed`

**Supporting Services:**
- `approval-service-changed`
- `agent-builder-service-changed`
- `agent-runtime-service-changed`
- `audit-query-service-changed`
- `cdr-processor-service-changed`
- `event-router-service-changed`

**Composite Filters:**
- `event-services-changed` - Multiple event services
- `gateway-services-changed` - Multiple gateway services

### Using Change Detection in Jobs

**Pattern 1: Run Only If Service Changed**
```yaml
test-patient:
  if: |
    ${{
      needs.change-detection.outputs.patient-service-changed == 'true' ||
      needs.change-detection.outputs.shared-changed == 'true'
    }}
```

**Pattern 2: Run If Anything Changed**
```yaml
test-all:
  if: ${{ needs.change-detection.outputs.backend-changed == 'true' }}
```

**Pattern 3: Run If Infrastructure Changed**
```yaml
security-scan:
  if: ${{ needs.change-detection.outputs.infrastructure-changed == 'true' }}
```

### Best Practices for Change Detection

**1. Keep Filters Up-to-Date**
- When adding a new service, add a filter
- When moving services, update paths
- Review filters quarterly

**2. Use Composite Filters**
- Group related services (all event services)
- Reduces condition complexity
- Makes jobs more maintainable

**3. Test Change Detection**
```bash
# Simulate change to specific service
git diff main...HEAD backend/modules/services/patient-service/

# Verify filter would trigger
# Check GitHub Actions → Workflow run → Details
```

**4. Conservative Approach**
- When in doubt, run tests (opt-in approach)
- False negatives worse than false positives
- Better to run unnecessary tests than skip needed ones

### Handling Edge Cases

**Scenario: Shared Library Change**
- Filter: `shared-changed`
- Action: Run ALL tests (because all services affected)
- Time impact: Full suite runs (15-18m)

**Scenario: Docs-Only Change**
- Filter: No matches
- Action: Build skipped, tests skipped, docker skipped
- Time impact: Minimal (1-2m)
- Benefit: 85% faster for documentation PRs

**Scenario: New Service**
- Filter: Not yet created
- Action: Service not tested initially
- Fix: Add filter, merge new service code, PR with filter creates workflow

---

## 3. Caching Strategies

### Gradle Caching

**Location:** `backend/gradle.properties`

```properties
org.gradle.caching=true
org.gradle.jvmargs=-Xmx4g -XX:+UseStringDeduplication
org.gradle.parallel=true
org.gradle.workers.max=4
```

**How It Works:**
1. Gradle caches task outputs in ~/.gradle/build-cache
2. GitHub Actions caches ~/.gradle across runs
3. If inputs match previous run, task output reused
4. Speeds up builds for unchanged tasks

**Cache Keys (Automatically Generated):**
- Source file hashes
- Classpath contents
- Task configuration
- Build tool version

**Performance Impact:**
- First run: Full build (10-12 min)
- Subsequent runs (no changes): 2-3 min
- Changes in one module: 3-5 min (other modules cached)

**Best Practices:**
1. Keep build stable - avoid changing Gradle frequently
2. Use descriptive task names
3. Avoid non-deterministic tasks
4. Check cache hit rates in logs

### Docker Layer Caching

**Concept:** Docker caches build layers based on dockerfile instructions

**Dockerfile Structure:**
```dockerfile
# Layer 1: Base image (cached)
FROM openjdk:21-slim

# Layer 2: System dependencies (rarely changes)
RUN apt-get update && apt-get install -y ...

# Layer 3: App dependencies (changes more often)
COPY build/libs/app.jar /app/

# Layer 4: Application (changes frequently)
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

**Cache Behavior:**
- Unchanged base image: Use cache (2 sec)
- Unchanged system deps: Use cache (2 sec)
- Changed app deps: Rebuild layer (2 min)
- Application always rebuilt: (1 min)

**Performance Impact:**
- First build: 10 min (all layers)
- No changes: 2-3 min (all from cache)
- App change only: 3-4 min (layers 1-3 cached)
- Large change: 8-10 min (multiple layers)

**Best Practices:**
1. Order dockerfile instructions: Most stable first
2. Use `.dockerignore` to exclude unnecessary files
3. Minimize layer count - combine RUN commands
4. Cache invalidation - understand when layers rebuild

### Artifact Caching

**GitHub Artifacts vs Gradle Cache:**

| Type | Size | Speed | Duration | Use Case |
|------|------|-------|----------|----------|
| Gradle Cache | 200MB+ | Fast | 5 days | Build optimization |
| Artifacts | 100-150MB | Medium | 24 hours | Job communication |

**Best Practice:**
- Use both
- Gradle cache for build task optimization
- Artifacts for passing built code between jobs

**Configuration:**
```yaml
- uses: actions/upload-artifact@v4
  with:
    name: build-artifacts
    path: backend/build/

- uses: actions/download-artifact@v4
  with:
    name: build-artifacts
    path: backend/build/
```

---

## 4. Performance Monitoring

### Metrics to Track

**Critical Metrics (Monitor Daily):**
1. Total PR feedback time
   - Target: 23-25 min
   - Alert: >28 min
   - Action: Investigate cause

2. Build job duration
   - Target: 6-8 min
   - Alert: >10 min
   - Action: Check for large commits

3. Test job durations
   - Target: 1-5 min per job
   - Alert: >8 min
   - Action: Review test performance

4. Docker build time
   - Target: 20-25 min
   - Alert: >30 min
   - Action: Check for many service changes

**Secondary Metrics (Monitor Weekly):**
1. Cache hit rates
   - Target: 60%+
   - Indicator: Build efficiency
   - Action: Optimize if <40%

2. Artifact sizes
   - Target: 100-150 MB
   - Indicator: Storage usage
   - Action: Optimize if >200 MB

3. Job failure rate
   - Target: 0%
   - Alert: Any failures
   - Action: Investigate immediately

### Viewing Metrics

**In GitHub Actions UI:**
1. Go to Workflow run
2. Click "Summary" tab
3. View job execution timeline
4. Check durations for each job

**Via Dashboard:**
```bash
# View performance dashboard
cat backend/docs/dashboards/cicd-performance.md
```

**Historical Data:**
```bash
# View metrics artifacts
gh run list --json name,durationMinutes,status
```

### Setting Alerts

**GitHub Actions Native:**
```yaml
- name: Check Performance
  if: always()
  run: |
    # Custom script to check metrics
    # Fail if performance regressed
    python3 scripts/check-performance.py
```

**Manual Review Schedule:**
- Daily: Spot check last PR times
- Weekly: Review trend and averages
- Monthly: Deep analysis and optimization

---

## 5. Team Workflows

### Pre-PR Checklist

Before pushing to GitHub:
```bash
# 1. Run local tests
./gradlew testFast              # 1.5-2 min

# 2. Verify build
./gradlew build                 # 5-8 min

# 3. Check for changes
git status

# 4. Create PR
gh pr create --title "..." --body "..."
```

### During PR Review

**While waiting for feedback:**
- Monitor Actions tab for job progress
- Expected time: 23-25 min total
- Can start next task after 5-10 min

**Breakdown of feedback time:**
- Change detection: 1 min
- Build: 6-8 min (or skipped)
- Tests: 5-6 min (parallel)
- Validation: 10-12 min (parallel)
- Merge gate: 1-2 min

**Check PR status:**
```bash
gh pr checks <PR_NUMBER>
```

### Post-Merge Workflow

After PR merged:
1. Monitor Docker builds in Actions tab
2. Verify deployment to staging
3. Run smoke tests in staging
4. Confirm in production

---

## 6. Troubleshooting Guide

### Build Job Hangs

**Symptoms:**
- Build job stuck for 20+ minutes
- No progress in logs

**Investigation:**
```bash
# Check for large files
git diff main...HEAD --stat | sort -k3 -rn | head -20

# Check for dependency issues
cd backend && ./gradlew dependencies

# View recent commits
git log --oneline -10
```

**Solutions:**
1. **Large files committed:** Remove with git filter-branch
2. **Dependency conflict:** Update gradle.properties
3. **Resource constraint:** Check GitHub Actions runner specs
4. **Gradle cache corruption:** Clear cache
   ```bash
   gh cache delete-all
   # Next run will rebuild cache
   ```

### Test Job Timeout

**Symptoms:**
- Job exceeds 15-minute timeout
- Tests still running but timeout triggered

**Investigation:**
```bash
# View test execution logs
gh run view --log <RUN_ID> | grep -A 5 "test-slow"

# Check test duration locally
./gradlew testSlow --profile
```

**Solutions:**
1. **Slow tests:** Profile and optimize
   ```bash
   ./gradlew testSlow -P profile
   # Review report in build/reports/profile/
   ```
2. **Increase timeout:** Update workflow yaml
3. **Split tests:** Create additional test job
4. **Parallel execution:** Use testParallel task

### Change Detection Not Triggering

**Symptoms:**
- Expected job skipped
- But files clearly changed

**Investigation:**
```bash
# Check which files changed
git diff main...HEAD --name-only

# Check filter in workflow
grep -A 20 "patient-service:" .github/workflows/backend-ci.yml

# Manually validate filter
grep -r "backend/modules/services/patient-service/" .github/workflows/
```

**Solutions:**
1. **Path mismatch:** Update filter paths
2. **New service:** Add filter for new service
3. **Nested services:** Check path wildcards
4. **Base branch:** Ensure comparing to correct branch

### Docker Build Fails

**Symptoms:**
- Docker build job fails
- Error in build logs

**Investigation:**
```bash
# View error logs
gh run view --log <RUN_ID> | tail -100

# Build locally first
docker build -f Dockerfile.patient-service .
```

**Common Issues:**
1. **Build artifact missing:** Check build job completed
2. **Base image unavailable:** Check registry connectivity
3. **Dependency missing:** Check dockerfile dependencies
4. **Permission denied:** Check dockerfile user permissions

**Solutions:**
```bash
# Clear docker cache and rebuild
docker system prune -a
docker build --no-cache -f Dockerfile.patient-service .

# Check base image
docker pull openjdk:21-slim
```

### Artifact Download Fails

**Symptoms:**
- "Unable to download artifact" error
- Test job fails with 404

**Investigation:**
```bash
# Check artifact exists
gh run view <RUN_ID> --json artifacts

# Check artifact size
gh run view <RUN_ID> --json artifacts | grep size
```

**Solutions:**
1. **Artifact expired:** GitHub keeps for 24 hours
2. **Artifact too large:** Compress before upload
3. **Network error:** Retry job
4. **Workflow changed:** Update artifact names

---

## 7. Common Scenarios

### Scenario 1: Docs-Only PR

**Change:** Update README.md only

**Expected Behavior:**
- Change detection: docs-changed (no backend change)
- Build: Skipped
- Tests: Skipped
- Docker: Skipped
- Total time: 1-2 min

**Verification:**
```bash
gh pr checks <PR_NUMBER>
# Should show:
# - Build: Skipped
# - Test-Unit: Skipped
# - All jobs: Skipped except change-detection
```

**Result:** 85% faster PR feedback (15m → 1m)

### Scenario 2: Single Service Change

**Change:** Update patient-service logic

**Expected Behavior:**
- Change detection: patient-service-changed = true
- Build: Runs
- Tests: Run (patient-service tests only, if possible)
- Validation: Run (security-scan, lint)
- Docker: Build patient-service image only
- Total time: 8-12 min

**Verification:**
```bash
gh pr checks <PR_NUMBER>
# Should show:
# - Build: Success (10-12m)
# - Test-Patient: Success (3-5m)
# - Security-Scan: Success (5-7m)
# - Docker-Patient: Success (5-8m)
```

**Result:** 50% faster PR feedback (15m → 8m)

### Scenario 3: Shared Module Change

**Change:** Update audit library (used by all services)

**Expected Behavior:**
- Change detection: shared-changed = true
- Build: Runs
- Tests: All test jobs run (because all services affected)
- Validation: All validation jobs run
- Docker: All service images rebuilt
- Total time: 25-30 min (full suite)

**Verification:**
```bash
gh pr checks <PR_NUMBER>
# Should show:
# - All jobs: Running or Passed
# - No skipped jobs (except docs)
```

**Result:** No improvement (0% faster) - as expected

### Scenario 4: Infrastructure Change

**Change:** Update .github/workflows/backend-ci.yml

**Expected Behavior:**
- Change detection: infrastructure-changed = true
- Build: Runs
- Tests: Run all (infrastructure affects entire pipeline)
- Validation: Run all
- Docker: Run all
- Total time: 25-30 min

**Verification:**
```bash
# Note: Workflow changes take effect on next run
# Test workflow change on feature branch first
```

### Scenario 5: Large Multi-Service Change

**Change:** Update audit library + patient service

**Expected Behavior:**
- Change detection: shared-changed = true (supercedes service-specific)
- Build: Runs
- Tests: All run (due to shared library)
- Validation: All run
- Docker: All services
- Total time: 25-30 min

**Verification:**
```bash
# Shared module overrides service-specific optimization
# This is correct behavior
```

---

## 8. Performance Tips

### For Developers

**Tip 1: Batch Small Changes**
- Instead of 5 small PRs (5 × 25m = 125m)
- Create 1 larger PR (1 × 25m = 25m)
- Reduces feedback overhead

**Tip 2: Separate Refactoring**
- Don't mix refactoring with features
- Refactoring-only PR: 1-2m (if docs-only recognized)
- Feature PR: 15-20m (fewer files changed)

**Tip 3: Keep Tests Fast**
- Profile slow tests locally
- Slow tests block entire job
- Target: <1 second per test

**Tip 4: Minimize Dependencies**
- Reduce import bloat
- Only import what you use
- Faster compilation = faster build

**Tip 5: Leverage Parallel Execution**
- Tests run in parallel automatically
- No special code needed
- Just avoid shared state between tests

### For Build Engineers

**Tip 1: Monitor Cache Hit Rates**
- Track Gradle cache effectiveness
- Target: 70%+ hit rate for repeating PRs
- Indicate optimization opportunities

**Tip 2: Optimize Dockerfile**
- Order instructions: stable first
- Use .dockerignore for efficiency
- Multi-stage builds for smaller images

**Tip 3: Review Job Dependencies**
- Minimize hard dependencies
- Maximize parallelization
- Use conditional execution

**Tip 4: Set Realistic Timeouts**
- Account for network latency (2-3 min)
- Account for resource contention
- Too tight: false failures
- Too loose: don't catch real hangs

**Tip 5: Automate Validation**
- Script all checks possible
- Run validation early
- Fail fast on obvious issues

### For Platform Team

**Tip 1: Trend Analysis**
- Monitor average feedback times
- Watch for degradation
- Investigate spikes

**Tip 2: Capacity Planning**
- Monitor GitHub Actions usage
- Plan for growth
- Budget for runners if needed

**Tip 3: Artifact Management**
- Review artifact sizes
- Archive old artifacts
- Optimize storage

**Tip 4: Continuous Improvement**
- Gather team feedback
- Identify bottlenecks
- Plan Phase 8+ optimizations

**Tip 5: Documentation**
- Keep change filters updated
- Document team procedures
- Share performance data

---

## Quick Reference

### Test Commands

```bash
# Local testing (before PR)
./gradlew testUnit              # 30-45 sec (fastest)
./gradlew testFast              # 1.5-2 min
./gradlew testIntegration       # 2-3 min
./gradlew testSlow              # 3-5 min
./gradlew testAll               # 10-15 min (comprehensive)
./gradlew testParallel          # 5-8 min (experimental)
```

### Build Commands

```bash
# Building
./gradlew build                 # Full build
./gradlew build -x test         # Skip tests
./gradlew :modules:services:SERVICENAME:build  # Single service

# Cache management
./gradlew cleanBuildCache       # Clear Gradle cache
gh cache delete-all             # Clear GitHub cache
```

### Workflow Commands

```bash
# Check PR status
gh pr checks <PR_NUMBER>

# View workflow run
gh run view <RUN_ID> --log

# Rerun jobs
gh run rerun <RUN_ID>

# List artifacts
gh run view <RUN_ID> --json artifacts
```

### Performance Investigation

```bash
# Check build times
./gradlew build --profile
# Review: build/reports/profile/

# Check test times
./gradlew testAll --profile
# Review individual test durations

# Check Docker build time
docker build --progress=plain -f Dockerfile.patient-service .
```

---

## Support & Escalation

### Issues with Phase 7 Workflow

**Level 1: Self-Service**
- Check CI_CD_BEST_PRACTICES.md (this document)
- Review Phase 7 completion summary
- Check GitHub Actions logs

**Level 2: Team Lead**
- Discuss unusual patterns
- Review performance trends
- Plan optimizations

**Level 3: Platform Team**
- Major workflow issues
- Performance regressions
- Infrastructure problems

---

## Continuous Improvement

### Feedback Channels

- Weekly standup: Share workflow feedback
- Slack #platform: Quick questions
- GitHub Issues: File bugs/feature requests
- Monthly retro: Discuss improvements

### Metrics Review

- Daily: Spot-check PR times
- Weekly: Calculate averages, trends
- Monthly: Deep analysis, optimization planning
- Quarterly: Lessons learned, strategy adjustment

---

## Related Documentation

- **PHASE-7-COMPLETION-SUMMARY.md** - Detailed Phase 7 overview
- **PHASE-7-FINAL-REPORT.md** - Executive summary
- **PHASE-7-CHANGE-DETECTION-GUIDE.md** - Detailed change detection
- **PHASE-7-CACHING-STRATEGY.md** - Caching deep dive
- **CLAUDE.md v4.0** - Quick project reference

---

_**Created:** February 1, 2026_
_**Status:** Production-Ready_
_**Version:** 1.0_

*This guide is designed for the development team to understand and optimize the Phase 7 CI/CD pipeline. Refer to specific sections as needed for troubleshooting and optimization.*
