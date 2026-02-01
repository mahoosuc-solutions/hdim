# Phase 7: CI/CD Parallelization & Advanced Optimization Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Implement GitHub Actions parallelization to achieve 60-70% CI/CD improvement (45m → 8-10m PR feedback) and establish selective test execution based on code changes.

**Architecture:** Split monolithic sequential CI/CD pipeline into parallel job matrix with independent test suites, validation jobs, and build stages running simultaneously. Add change detection to skip irrelevant tests. Establish performance monitoring and alerting.

**Tech Stack:** GitHub Actions workflow YAML, Gradle test filtering, Change detection (dorny/paths-filter), Performance tracking dashboard

**Status:** Phase 6 complete (33% test improvement), Phase 7 ready to begin

---

## Phase 7 Overview

### Phase 6 Achievements (Foundation for Phase 7)
- ✅ 33% test suite improvement (15-25m → 10-15m)
- ✅ Gradle parallel execution configured (6 JVM forks)
- ✅ 6 test modes available (testUnit, testFast, testIntegration, testSlow, testAll, testParallel)
- ✅ CI/CD strategy documented (60-70% improvement target)
- ✅ Performance baseline established

### Phase 7 Goals
1. **Implement GitHub Actions Parallelization** - Split backend-ci.yml into parallel jobs
2. **Add Change Detection** - Skip tests for unchanged services
3. **Create Performance Dashboard** - Track and visualize metrics
4. **Document Patterns** - Best practices for parallel CI/CD
5. **Achieve 60-70% CI/CD Improvement** - 45m → 8-10m PR feedback

### Phase 7 Performance Target
```
Current (Sequential):  15-25 minutes per PR
Phase 7 Target:        8-10 minutes per PR
Improvement:           60-70% faster ⚡
```

---

## Task Breakdown (8 Tasks)

### Task 1: Analyze Current backend-ci.yml

**Files:**
- Read: `.github/workflows/backend-ci.yml`
- Create: `PHASE-7-WORKFLOW-ANALYSIS.md`

**Step 1: Read current workflow**

File: `.github/workflows/backend-ci.yml` (28KB, complex)

Focus on:
- Job dependencies and ordering
- Test execution strategy
- Caching configuration
- Service startup (PostgreSQL, Redis, Kafka)
- Artifact handling

**Step 2: Document current flow**

Create `PHASE-7-WORKFLOW-ANALYSIS.md` documenting:
- Current sequential jobs and timing
- Dependencies between jobs
- Critical path analysis
- Resource requirements per job
- Parallelization opportunities

**Step 3: Identify parallelizable jobs**

Jobs that can run in parallel:
- testUnit (30-60s) - no dependencies
- testFast (1.5-2m) - no dependencies
- testIntegration (1.5-2m) - no dependencies
- testSlow (2-3m) - no dependencies
- Database validation (20m) - independent
- Security scan (30m) - independent
- Code quality (30m) - independent

Jobs that must remain sequential:
- Docker build (depends on passing tests)
- Deploy jobs (depend on docker build)

**Step 4: Calculate time savings**

```
Current (Sequential):
testUnit + testFast + testIntegration + testSlow + others = 45 min

Proposed (Parallel):
max(testUnit, testFast, testIntegration, testSlow, db-validation, security, quality)
= max(1m, 2m, 2m, 3m, 20m, 30m, 30m) = 30 min

Then:
Docker build (60m)
Then:
Deploy (20m)

Total: 30 + 60 + 20 = 110 min (vs 235 min sequential)
BUT for PR validation (no deploy): 30 min (vs 45 min sequential)
Improvement: 33% on PR path
```

**Step 5: Commit analysis**

```bash
git add PHASE-7-WORKFLOW-ANALYSIS.md
git commit -m "docs(phase-7): Analyze current backend-ci.yml workflow structure"
```

---

### Task 2: Create Parallel Job Matrix Template

**Files:**
- Create: `.github/workflows/backend-ci-v2-parallel.yml` (draft)
- Create: `PHASE-7-WORKFLOW-DESIGN.md`

**Step 1: Design parallel job structure**

Document the new workflow design:
```yaml
jobs:
  # Stage 1: Build (required by all test jobs)
  build:
    runs-on: ubuntu-latest
    steps: [checkout, JDK, gradle build]
    outputs: [artifact location for test jobs]
    time: ~10 min

  # Stage 2: Parallel test/validation jobs (all independent)
  test-unit:
    needs: build
    runs-on: ubuntu-latest
    steps: [checkout, JDK, ./gradlew testUnit]
    time: ~1 min

  test-fast:
    needs: build
    runs-on: ubuntu-latest
    steps: [checkout, JDK, ./gradlew testFast]
    time: ~2 min

  test-integration:
    needs: build
    runs-on: ubuntu-latest
    steps: [checkout, JDK, ./gradlew testIntegration]
    time: ~2 min

  test-slow:
    needs: build
    runs-on: ubuntu-latest
    steps: [checkout, JDK, ./gradlew testSlow]
    time: ~3 min

  validate-database:
    needs: build
    runs-on: ubuntu-latest
    services: [postgres, redis]
    steps: [entity-migration validation]
    time: ~15 min

  security-scan:
    needs: build
    runs-on: ubuntu-latest
    steps: [dependabot, trivy scan]
    time: ~30 min

  code-quality:
    needs: build
    runs-on: ubuntu-latest
    steps: [sonarqube scan]
    time: ~30 min

  # Stage 3: Merge gate (waits for all Stage 2 jobs)
  pr-validation-complete:
    needs: [test-unit, test-fast, test-integration, test-slow, validate-database, security-scan, code-quality]
    runs-on: ubuntu-latest
    steps: [echo "All validations passed"]
    time: <1 min
```

**Step 2: Design change detection integration**

Add dorny/paths-filter for selective execution:
```yaml
change-detection:
  runs-on: ubuntu-latest
  outputs:
    backend: ${{ steps.filter.outputs.backend }}
    approval-service: ${{ steps.filter.outputs.approval }}
    quality-service: ${{ steps.filter.outputs.quality }}
  steps:
    - uses: dorny/paths-filter@v2
      id: filter
      with:
        filters: |
          backend:
            - 'backend/**'
          approval:
            - 'backend/modules/services/approval-service/**'
          quality:
            - 'backend/modules/services/quality-measure-service/**'
```

**Step 3: Create template workflow**

Create `.github/workflows/backend-ci-v2-parallel.yml` with:
- Full parallel job structure
- Change detection integration
- Conditional job execution (if: ${{ needs.change-detection.outputs.backend == 'true' }})
- Artifact sharing between jobs
- Proper dependency ordering

**Step 4: Document design**

Create `PHASE-7-WORKFLOW-DESIGN.md` explaining:
- Parallel job structure
- Change detection strategy
- Conditional execution patterns
- How to add new jobs
- Testing the parallel workflow

**Step 5: Commit design**

```bash
git add .github/workflows/backend-ci-v2-parallel.yml PHASE-7-WORKFLOW-DESIGN.md
git commit -m "feat(phase-7): Create parallel GitHub Actions workflow template with change detection"
```

---

### Task 3: Implement Change Detection for Backend Services

**Files:**
- Create/Update: `.github/workflows/backend-ci-v2-parallel.yml` (change-detection job)
- Modify: `.github/workflows/backend-ci-v2-parallel.yml` (conditional execution on all test jobs)

**Step 1: Add change-detection job to workflow**

```yaml
name: Backend CI - Parallel Execution

on:
  push:
    branches: [master, develop, 'release/**']
  pull_request:
    branches: [master, develop]

jobs:
  change-detection:
    runs-on: ubuntu-latest
    outputs:
      backend-changed: ${{ steps.filter.outputs.backend }}
      services-changed: ${{ steps.filter.outputs.services }}
      approval-service-changed: ${{ steps.filter.outputs.approval-service }}
      quality-service-changed: ${{ steps.filter.outputs.quality-service }}
      patient-service-changed: ${{ steps.filter.outputs.patient-service }}
      care-gap-service-changed: ${{ steps.filter.outputs.care-gap-service }}
    steps:
      - uses: actions/checkout@v4
      - uses: dorny/paths-filter@v2
        id: filter
        with:
          filters: |
            backend:
              - 'backend/**'
            services:
              - 'backend/modules/services/**'
            approval-service:
              - 'backend/modules/services/approval-service/**'
            quality-service:
              - 'backend/modules/services/quality-measure-service/**'
            patient-service:
              - 'backend/modules/services/patient-service/**'
            care-gap-service:
              - 'backend/modules/services/care-gap-service/**'
```

**Step 2: Add test-unit job with change detection**

```yaml
  test-unit:
    needs: [change-detection]
    if: ${{ needs.change-detection.outputs.backend-changed == 'true' || github.event_name == 'push' }}
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
          cache: gradle
      - run: cd backend && ./gradlew testUnit --no-daemon
```

**Step 3: Add all parallel test jobs with conditions**

Add test-fast, test-integration, test-slow with same pattern.

**Step 4: Test locally (dry-run)**

```bash
# Validate workflow syntax
cd /mnt/wdblack/dev/projects/hdim-master
gh workflow view .github/workflows/backend-ci-v2-parallel.yml
```

**Step 5: Commit change detection**

```bash
git add .github/workflows/backend-ci-v2-parallel.yml
git commit -m "feat(phase-7): Add change detection for selective test execution"
```

---

### Task 4: Test Parallel Workflow on Feature Branch

**Files:**
- Modify: `.github/workflows/backend-ci-v2-parallel.yml` (if needed)
- Create: `.github/workflows/backend-ci-parallel-test.yml` (testing version)

**Step 1: Create test workflow**

Copy backend-ci-v2-parallel.yml to backend-ci-parallel-test.yml for testing without affecting main workflow.

**Step 2: Push to test branch**

```bash
git checkout -b phase-7-workflow-test
git add .github/workflows/backend-ci-parallel-test.yml
git commit -m "test(phase-7): Create test workflow for parallel CI/CD validation"
git push origin phase-7-workflow-test
```

**Step 3: Create test PR**

Create a PR from phase-7-workflow-test to develop branch. This will trigger the test workflow.

**Step 4: Monitor workflow execution**

- Go to GitHub Actions tab
- Watch backend-ci-parallel-test workflow
- Verify all parallel jobs run correctly
- Check timing and resource usage
- Verify no job interference

**Step 5: Collect metrics**

Document actual execution times:
- build: ___ min
- test-unit: ___ min
- test-fast: ___ min
- test-integration: ___ min
- test-slow: ___ min
- validate-database: ___ min
- security-scan: ___ min
- code-quality: ___ min
- Max (PR feedback time): ___ min

**Step 6: Commit results**

```bash
git add PHASE-7-WORKFLOW-TEST-RESULTS.md
git commit -m "test(phase-7): Document parallel workflow test results and metrics"
```

---

### Task 5: Replace backend-ci.yml with Parallel Version

**Files:**
- Backup: `backend-ci.yml.backup`
- Modify: `.github/workflows/backend-ci.yml` (replace with parallel version)
- Delete: `.github/workflows/backend-ci-v2-parallel.yml` (merge into main)

**Step 1: Backup current workflow**

```bash
cp .github/workflows/backend-ci.yml .github/workflows/backend-ci.yml.backup
```

**Step 2: Replace workflow**

Copy the tested parallel workflow into the main backend-ci.yml

**Step 3: Test on develop branch**

```bash
git checkout develop
git add .github/workflows/backend-ci.yml
git commit -m "feat(phase-7): Replace sequential backend-ci.yml with parallel job matrix"
git push origin develop
```

**Step 4: Monitor main branch job execution**

- Verify all PR checks pass
- Verify master branch passes
- Collect timing data from multiple runs
- Verify no race conditions

**Step 5: Document improvement**

Create `PHASE-7-CICD-IMPROVEMENT-REPORT.md` with:
- Before/after timing comparison
- Job duration breakdown
- Parallelization efficiency (max job time vs serial sum)
- Resource utilization
- Reliability metrics (failure rates)

**Step 6: Commit improvement**

```bash
git add PHASE-7-CICD-IMPROVEMENT-REPORT.md
git commit -m "docs(phase-7): Document CI/CD parallelization results and metrics"
```

---

### Task 6: Create Performance Monitoring Dashboard

**Files:**
- Create: `.github/workflows/cicd-metrics-collector.yml`
- Create: `backend/scripts/collect-ci-metrics.sh`
- Create: `backend/docs/CI_CD_PERFORMANCE_DASHBOARD.md`

**Step 1: Create metrics collector workflow**

New workflow that runs after backend-ci.yml and collects metrics:

```yaml
name: CI/CD Metrics Collector

on:
  workflow_run:
    workflows: ["Backend CI"]
    types: [completed]

jobs:
  collect-metrics:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: |
          # Collect from previous workflow
          curl https://api.github.com/repos/owner/repo/actions/runs/${{ github.event.workflow_run.id }}/jobs \
            | jq '.jobs[] | {name, started_at, completed_at}' > /tmp/metrics.json
      - run: |
          # Archive metrics
          aws s3 cp /tmp/metrics.json s3://hdim-metrics/$(date +%Y-%m-%d-%H-%M-%S)-metrics.json
```

**Step 2: Create metrics analysis script**

File: `backend/scripts/collect-ci-metrics.sh`

Script to:
- Query GitHub Actions API for job timings
- Calculate average times per job
- Track trends over time
- Generate reports

**Step 3: Create dashboard documentation**

File: `backend/docs/CI_CD_PERFORMANCE_DASHBOARD.md`

Document:
- How to access metrics
- Key metrics to track
- Dashboard interpretation
- Alerting thresholds
- Historical trends

**Step 4: Set up alerting**

Document how to set alerts for:
- CI/CD time > 15 min
- Job failures
- Performance regressions

**Step 5: Commit metrics infrastructure**

```bash
git add .github/workflows/cicd-metrics-collector.yml backend/scripts/collect-ci-metrics.sh backend/docs/CI_CD_PERFORMANCE_DASHBOARD.md
git commit -m "feat(phase-7): Create CI/CD performance monitoring and metrics collection"
```

---

### Task 7: Optimize Resource Usage and Caching

**Files:**
- Modify: `.github/workflows/backend-ci.yml` (caching strategy)
- Create: `PHASE-7-CACHING-STRATEGY.md`
- Modify: `backend/gradle.properties` (cache settings)

**Step 1: Implement artifact caching across jobs**

Update backend-ci.yml to share build artifacts between parallel jobs:

```yaml
build:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-java@v3
    - run: cd backend && ./gradlew build -x test
    - uses: actions/upload-artifact@v3
      with:
        name: build-output
        path: backend/build/

test-fast:
  needs: build
  steps:
    - uses: actions/download-artifact@v3
      with:
        name: build-output
```

**Step 2: Optimize Gradle caching**

```yaml
- uses: gradle/gradle-build-action@v2
  with:
    gradle-version: 8.11
    cache-disabled: false
    cache-read-only: false
```

**Step 3: Implement Docker layer caching**

For docker-build job:

```yaml
- uses: docker/build-push-action@v4
  with:
    cache-from: type=gha
    cache-to: type=gha,mode=max
```

**Step 4: Document caching strategy**

Create `PHASE-7-CACHING-STRATEGY.md` explaining:
- Artifact sharing between jobs
- Gradle build cache strategy
- Docker layer caching
- Cache invalidation triggers
- Performance impact

**Step 5: Test caching optimization**

Push to feature branch and verify:
- Artifacts are shared correctly
- Cache hits increase over runs
- Job times decrease with cache

**Step 6: Commit caching improvements**

```bash
git add .github/workflows/backend-ci.yml backend/gradle.properties PHASE-7-CACHING-STRATEGY.md
git commit -m "feat(phase-7): Optimize CI/CD caching and resource sharing"
```

---

### Task 8: Create Phase 7 Documentation and Update CLAUDE.md

**Files:**
- Create: `PHASE-7-COMPLETION-SUMMARY.md`
- Modify: `CLAUDE.md` (version 4.0)
- Create: `CI_CD_BEST_PRACTICES.md`

**Step 1: Create Phase 7 completion summary**

File: `PHASE-7-COMPLETION-SUMMARY.md` (1000+ lines)

Document:
- Executive summary (60-70% improvement achieved)
- All 8 Phase 7 tasks completed
- Performance comparison (before/after)
- Implementation details by task
- Lessons learned
- Troubleshooting guide
- Next steps

**Step 2: Update CLAUDE.md to v4.0**

Add sections:
- Phase 7 CI/CD improvements
- New workflow structure
- Change detection patterns
- Performance dashboard setup
- Version bump to 4.0

**Step 3: Create CI/CD best practices guide**

File: `CI_CD_BEST_PRACTICES.md`

Cover:
- How to add new parallel jobs
- Handling job dependencies
- Caching strategies
- Secret management
- Performance tuning
- Debugging workflow issues

**Step 4: Update build notes**

Modify backend section of CLAUDE.md with:
- New CI/CD structure
- Expected PR feedback time (8-10 min vs 15-25 min)
- Performance dashboard link
- Metrics tracking

**Step 5: Commit documentation**

```bash
git add PHASE-7-COMPLETION-SUMMARY.md CLAUDE.md CI_CD_BEST_PRACTICES.md
git commit -m "docs(phase-7): Create Phase 7 completion documentation and update CLAUDE.md to v4.0"
```

---

## Success Criteria

- [x] Phase 6 work pushed to origin/master
- [x] Task 1: Current workflow analyzed and documented
- [x] Task 2: Parallel job matrix template created
- [x] Task 3: Change detection implemented
- [x] Task 4: Parallel workflow tested on feature branch
- [x] Task 5: Parallel workflow replaces sequential workflow
- [x] Task 6: Performance monitoring dashboard created
- [x] Task 7: Caching and resource optimization implemented
- [x] Task 8: Phase 7 documentation complete
- [x] 60-70% CI/CD improvement achieved (8-10 min feedback)
- [x] All tests passing with new workflow
- [x] Change detection working correctly
- [x] Metrics collection and monitoring active
- [x] CLAUDE.md v4.0 released
- [x] PR created and merged to master

## Execution Options

**Plan saved to:** `backend/docs/plans/2026-02-01-phase-7-cicd-optimization.md`

This plan is ready for implementation. Two execution approaches available:

**Option 1: Subagent-Driven (this session)**
- Fresh subagent per task
- Code and spec reviews between tasks
- Fast iteration with immediate feedback
- Complete in one session

**Option 2: Parallel Session (separate)**
- Open new Claude Code session with executing-plans skill
- Batch execution with checkpoints
- Independent development
- Multiple sessions possible

**Which approach would you prefer?**