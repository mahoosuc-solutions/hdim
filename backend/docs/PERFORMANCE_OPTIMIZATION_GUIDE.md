# Performance Optimization Guide (Phase 4)

**Status:** COMPLETE
**Date:** February 1, 2026
**Focus:** Test execution optimization and performance profiling

---

## Quick Start

### For Developers

```bash
# Fast feedback during development (2-3 minutes)
./gradlew testFast

# Only unit tests (30-60 seconds)
./gradlew testUnit

# Only integration tests (3-5 minutes)
./gradlew testIntegration

# Full test suite with slow tests (30-45 minutes)
./gradlew testAll
```

### For CI/CD

```bash
# Run tests in parallel jobs
./gradlew testUnit        # Job 1: ~30-60s
./gradlew testIntegration # Job 2: ~3-5m (parallel to Job 1)
./gradlew testSlow        # Job 3: ~5-10m (optional, after Jobs 1-2)
```

---

## Test Execution Modes (Phase 4)

### Mode 1: testFast ⚡ (2-3 minutes)

**Recommended for:** Development, pre-commit validation

```bash
./gradlew testFast
```

**What it runs:**
- ✅ All unit tests (~157 tests, ~30-60s)
- ✅ Fast integration tests (quick, no Docker)
- ❌ Excludes all @Tag("slow") tests

**Use when:**
- Active development
- Before pushing code
- Quick feedback loops needed

**Impact:**
- 75% faster than full suite
- Still tests service interactions
- Catches most bugs

---

### Mode 2: testUnit ⚡⚡ (30-60 seconds)

**Recommended for:** Real-time development feedback

```bash
./gradlew testUnit
```

**What it runs:**
- ✅ All unit tests only (~157 tests)
- ✅ Fast execution (<100ms per test)
- ❌ No service integration testing

**Use when:**
- Writing service layer code
- Testing business logic
- Maximum feedback speed needed

**Benefits:**
- Fastest possible feedback
- No Spring context overhead
- Great for TDD workflows

---

### Mode 3: testIntegration 🔧 (3-5 minutes)

**Recommended for:** Integration/API testing

```bash
./gradlew testIntegration
```

**What it runs:**
- ✅ All integration tests (~102 tests)
- ✅ Spring context with H2 database
- ❌ No slow/Docker-dependent tests

**Use when:**
- Testing API endpoints
- Database interaction changes
- Before PR creation

**Benefits:**
- Validates service interactions
- Tests actual database queries
- More realistic scenarios

---

### Mode 4: testSlow 🐢 (5-10 minutes)

**Recommended for:** Comprehensive validation

```bash
./gradlew testSlow
```

**What it runs:**
- ✅ All tests tagged with @Tag("slow")
- ✅ Heavyweight tests (24+ tests)
- ✅ Docker containers, Kafka, etc.
- ✅ High-volume event publishing

**Use when:**
- Before merging to master
- Complete validation needed
- Pre-release testing

**Note:** These tests are slow but comprehensive

---

### Mode 5: testAll ✅ (30-45 minutes)

**Recommended for:** Final validation, release

```bash
./gradlew testAll
```

**What it runs:**
- ✅ ALL tests (259+ tests)
- ✅ Including slow tests
- ✅ Including Docker containers
- ✅ Complete coverage

**Use when:**
- Final pre-merge validation
- Release preparation
- Complete regression testing
- Master branch health check

**Note:** Slowest option, most comprehensive

---

## Performance Baseline - Phase 4

### Execution Times by Mode

| Mode | Tests | Time | Speed | Use Case |
|------|-------|------|-------|----------|
| **testUnit** | ~157 | 30-60s | ⚡⚡⚡ | Development |
| **testFast** | ~235 | 2-3m | ⚡⚡ | Pre-commit |
| **testIntegration** | ~102 | 3-5m | ⚡ | API testing |
| **testSlow** | ~24 | 5-10m | 🐢 | Final validation |
| **testAll** | ~259 | 30-45m | 🐢🐢 | Release |

### Per-Service Performance (Phase 3 Services)

```
audit-query-service:       0.5s (6 tests)
care-gap-service:          3-5s (16 tests)
cdr-processor-service:     1-2s (15 tests)
────────────────────────────────
TOTAL (Phase 3):           4-7s (37 tests)
```

---

## Slow Tests Identified (Phase 4 Analysis)

### By Sleep Call Count

**CRITICAL (>10 Thread.sleep() calls):**
- EmailNotificationServiceTest (23 calls)
- WebhookCallbackServiceTest (16 calls)
- ApprovalEventPublisherTest (14 calls)
- PayerWorkflowsAuditIntegrationHeavyweightTest (14 calls)

**HIGH (5-9 calls):**
- 8 tests with 5-9 sleep calls
- Estimated 2-5 minutes overhead

**MODERATE (1-3 calls):**
- 12 tests with 1-3 sleep calls
- Estimated 1-2 minutes overhead

### By Docker Usage

**69 @Testcontainers Tests:**
- PostgreSQL: 35 tests (~5-10 min overhead)
- Kafka: 20 tests (~10-15 min overhead if enabled)
- Other: 14 tests (~3-5 min overhead)

**Total Docker Overhead:** ~15-25 minutes removed by running `testFast`

---

## Optimization Tips for Developers

### 1. Use testUnit During Development

```bash
# Bad: Running full suite during development
./gradlew test

# Good: Fast feedback while coding
./gradlew testUnit

# Better: Add unit tests to pre-commit hook
echo "./gradlew testUnit || exit 1" > .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

### 2. Run testFast Before Creating PR

```bash
# Before pushing to remote
./gradlew testFast

# This runs in 2-3 minutes and catches most issues
```

### 3. Only Run testAll Before Merging

```bash
# Only when you're ready to merge to master
./gradlew testAll

# This is comprehensive but slow (30-45 min)
```

### 4. Run Service-Specific Tests When Focused

```bash
# Change only made to care-gap-service?
./gradlew :modules:services:care-gap-service:testFast

# Much faster than full suite
```

---

## CI/CD Integration Strategy

### Recommended Pipeline

```yaml
# Job 1: Fast validation (parallel)
- name: Unit Tests
  run: ./gradlew testUnit
  # ~30-60 seconds

# Job 2: Integration tests (parallel to Job 1)
- name: Integration Tests
  run: ./gradlew testIntegration
  # ~3-5 minutes

# Job 3: Wait for Jobs 1-2, then run slow tests
- name: Slow Tests
  run: ./gradlew testSlow
  if: previous_jobs_passed
  # ~5-10 minutes (optional)
```

### Timeline

**With Sequential Execution:**
- Total time: 30-45 minutes

**With Parallel Execution (recommended):**
- Unit + Integration in parallel: ~max(1m, 5m) = 5m
- Add slow tests: ~10m
- **Total: ~15 minutes** (60% faster! ⚡)

---

## Troubleshooting Slow Tests

### Issue: Test taking >10 seconds

**Potential causes:**
1. Spring context initialization (6-10s)
2. Testcontainers startup (5-15s)
3. Database operations (1-3s)
4. Thread.sleep() delays (variable)
5. Kafka polling (10-30s)

**How to identify:**

```bash
# Enable Gradle test logging
./gradlew testSlow --info 2>&1 | grep -E "test|STARTED"

# Or use IDE test profiler
```

**Solutions:**

| Issue | Solution |
|-------|----------|
| Spring context | Accept as baseline, focus on reducing overhead |
| Docker startup | Use @EmbeddedKafka instead (Phase 5) |
| Database ops | Profile queries, add indexes |
| Thread.sleep() | Replace with CountDownLatch or event waits |
| Kafka polling | Reduce timeout, use non-blocking APIs |

---

## Phase 4 Success Metrics

### Completed ✅

- [x] Performance baseline established (PERFORMANCE_BASELINE.md)
- [x] Slow tests identified (613 tests analyzed)
- [x] 5 Gradle test execution modes created
- [x] testFast task enables 2-3 minute feedback
- [x] testSlow task separates heavy tests
- [x] Documentation complete

### Results

**Before Phase 4:**
```
testUnit: 30-60s (Phase 3 - only unit tests, 37 files)
testIntegration: 3-5m (Phase 3 - only integration tests, 37 files)
testAll: 30-45m (everything including Docker)
```

**After Phase 4:**
```
testUnit: 30-60s (unchanged)
testIntegration: 3-5m (unchanged)
testFast: 2-3m (new - fastest with service integration)
testSlow: 5-10m (new - separates heavy tests)
testAll: 30-45m (unchanged - but better organized)
```

### Performance Improvements Achieved

| Improvement | Impact |
|-------------|--------|
| Separated slow tests | Developers skip 5-10m of overhead |
| Parallel unit/integration | CI/CD can run in parallel |
| Identified bottlenecks | Foundation for Phase 5 |
| Created testFast mode | 2-3m feedback in development |
| Documented strategy | Team alignment on test execution |

---

## Next Steps (Phase 5)

### Phase 5: Embedded Kafka Migration

**Goal:** Reduce test suite from 30-45m to 15-25m

**Strategy:**
1. Replace @Testcontainers Kafka with @EmbeddedKafka
2. Re-enable 3 disabled heavyweight tests
3. Keep EmbeddedKafka running across test methods
4. Parallel container sharing for PostgreSQL

**Expected Savings:** 10-15 minutes

---

### Phase 5 Optimization Roadmap

1. **Weeks 1-2: Embedded Kafka Setup**
   - Replace Testcontainers with @EmbeddedKafka
   - Verify test compatibility
   - Measure improvements

2. **Weeks 3-4: Re-enable Heavyweight Tests**
   - Enable CareGapAuditPerformanceTest
   - Enable CdrProcessorAuditIntegrationHeavyweightTest
   - Verify no regressions

3. **Weeks 5-6: Optimization Pass**
   - Replace Thread.sleep() with proper waits
   - Profile and optimize slowest tests
   - Final performance report

---

## References

### Documents
- **PERFORMANCE_BASELINE.md** - Detailed performance analysis
- **TEST_CLASSIFICATION_GUIDE.md** - Test categorization (Phase 3)
- **TEST_INFRASTRUCTURE_ANALYSIS.md** - Complete slow test inventory

### Commands Reference
```bash
# Quick tests
./gradlew testUnit          # ~1m
./gradlew testFast          # ~2-3m

# Comprehensive tests
./gradlew testIntegration   # ~3-5m
./gradlew testSlow          # ~5-10m
./gradlew testAll           # ~30-45m

# Service-specific
./gradlew :modules:services:care-gap-service:testFast
./gradlew :modules:services:care-gap-service:testSlow
```

---

## Summary

Phase 4 successfully establishes performance optimization foundation:

✅ **5 test execution modes** for different workflows
✅ **Performance baseline** documented (30-45m full suite)
✅ **Slow tests identified** (24+ tests, 5-10 minutes overhead)
✅ **Docker bottleneck** quantified (15-25 minutes)
✅ **Phase 5 prepared** (Embedded Kafka migration plan)

**Result: Developers now have faster feedback loops and CI/CD can parallelize execution**

🚀 Ready for Phase 5 - Embedded Kafka migration and performance target of 15-25 minutes!
