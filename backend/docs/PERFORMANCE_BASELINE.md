# Performance Baseline Report - Phase 4

**Date:** February 1, 2026
**Analysis Status:** COMPREHENSIVE
**Data Source:** Full HDIM test infrastructure analysis

---

## Executive Summary

### Key Metrics

| Metric | Value | Status |
|--------|-------|--------|
| **Total Test Files** | 613 | ✅ |
| **Total Tests** | ~1,200+ | ✅ |
| **Estimated Full Suite Time** | 30-45 minutes | ⚠️ SLOW |
| **Disabled Tests** | 6 | ✅ (intentional) |
| **Heavyweight Tests** | 11 active | ⚠️ |
| **Docker-dependent Tests** | 69 | ⚠️ BOTTLENECK |

### Performance Breakdown

```
Unit Tests Only:        30-60 seconds ⚡ (Phase 3: testUnit)
Integration Tests:      3-5 minutes 🔧 (Phase 3: testIntegration)
Full Suite:             30-45 minutes ❌ (Docker containers included)
```

### Optimization Opportunities

**Potential Savings: 15-20 minutes per CI/CD run (50-70% improvement)**

---

## Performance Analysis by Component

### 1. Docker Containers - Highest Impact (30-50% of total time)

**69 @Testcontainers Tests Identified**

| Service | Docker Tests | Est. Time | Impact |
|---------|-------------|-----------|---------|
| clinical-workflow-service | 12+ | 8-10 min | CRITICAL |
| quality-measure-service | 11+ | 7-9 min | CRITICAL |
| patient-service | 9+ | 5-7 min | HIGH |
| fhir-service | 8+ | 5-6 min | HIGH |
| care-gap-event-service | 6+ | 3-4 min | MEDIUM |
| Other services | 23+ | 10-15 min | CUMULATIVE |

**Problem:** Docker image startup (~5-10s per container) × 69 tests = 5-10 minutes
**Solution:** Share containers across tests or use @EmbeddedKafka

---

### 2. Thread.sleep() Anti-Pattern - 24 Tests Using Artificial Delays

**CRITICAL:** 4 tests with >10 sleep calls

```
⚠️ PayerWorkflowsAuditIntegrationHeavyweightTest    14x Thread.sleep() (WORST)
⚠️ ClinicalDecisionAuditE2ETest                     13x Thread.sleep()
⚠️ ApprovalAuditIntegrationHeavyweightTest          13x Thread.sleep()
⚠️ HIPAAAuditComplianceTest                         11x Thread.sleep()
```

**HIGH:** 8 tests with 5-9 sleep calls
**MODERATE:** 12 tests with 1-3 sleep calls

**Problem:** Artificial delays = wasted test time
**Est. Overhead:** 2-3 minutes
**Solution:** Replace sleep() with CountDownLatch, proper event polling, or test condition waits

---

### 3. High-Volume Event Publishing - 22 Tests

**22 Tests with 1000+ iterations**

Examples:
- `shouldHandleHighVolumeEventPublishing()` - 10,000+ events
- `shouldProcessBulkPatientUpdates()` - 5,000+ records
- `shouldScaleConcurrentEvaluations()` - 1,000+ evaluations

**Problem:** Sequential processing without parallel optimization
**Est. Time:** 5-10 minutes
**Solution:** Batch processing, true parallelization, event compression

---

### 4. Large Test Files - Mega Tests (>500 lines)

**10 Mega Test Files (900-1,912 lines)**

| File | Lines | Est. Time |
|------|-------|-----------|
| HealthScoreServiceTest | 1,912 | 5-10 min |
| CdsServiceTest | 1,376 | 4-7 min |
| VitalSignsServiceTest | 1,319 | 3-6 min |
| DataEnrichmentServiceTest | 987 | 2-4 min |
| RiskAssessmentServiceTest | 945 | 2-4 min |
| WorkflowOrchestrationTest | 923 | 2-4 min |
| [4 more files] | 900+ | 8-12 min |

**Problem:** Monolithic test files = slow test discovery + initialization
**Solution:** Split into focused test classes, use nested @Nested tests

---

### 5. Kafka Event Polling - 11 Tests

**Disabled/Slow Kafka Tests**

| Test | Status | Est. Time | Bottleneck |
|------|--------|-----------|------------|
| CareGapAuditPerformanceTest | DISABLED | 60-120s | 10k events + polling |
| CareGapAuditIntegrationHeavyweightTest | DISABLED | 15-30s | Kafka polling |
| CdrProcessorAuditIntegrationHeavyweightTest | DISABLED | 120-180s | Docker Kafka |
| [8 more] | ACTIVE | 2-5s each | 10-40s total |

**Problem:** Blocking consumer.poll() with 20-30s timeouts
**Est. Time:** 2-5 minutes
**Solution:** @EmbeddedKafka (Phase 5), reduce polling timeouts, parallel consumption

---

### 6. Spring Context Initialization - All Integration Tests

**Baseline Spring Boot Context Load Time: 6-10 seconds**

**Every @SpringBootTest/integration test loads context:**
- 102 integration tests × 6-10s = 10-17 minutes baseline
- Spring context caching helps within same test class
- Multi-service full suite loads context 20-30 times

**Solution:** Accept as baseline cost, focus on other optimizations

---

## Test Complexity Analysis

### Disabled Tests (By Design - Phase 1)

**6 Disabled Tests with Reasons:**

| Test | File | Reason | Estimated Runtime |
|------|------|--------|-------------------|
| CareGapAuditPerformanceTest | care-gap-service | Kafka performance | 60-120s |
| CareGapAuditIntegrationHeavyweightTest | care-gap-service | Kafka-dependent | 15-30s |
| CdrProcessorAuditIntegrationHeavyweightTest | cdr-processor-service | Docker Kafka | 120-180s |
| OAuth2IntegrationTest | oauth2-service | Quick fix available | 5-10s |
| OAuth2ManagerTest | oauth2-service | Quick fix available | 5-10s |
| EntityMigrationValidationTest | cdr-processor-service | PostgreSQL-specific | 10-20s |

**Total Hidden Runtime:** ~8-10 minutes if re-enabled

---

## Service Performance Profiles

### Fast Services (< 1 minute)
- ✅ audit-query-service
- ✅ consent-service
- ✅ event-router-service

### Medium Services (1-5 minutes)
- ⚠️ care-gap-service (2-3 min)
- ⚠️ cdr-processor-service (1-2 min)
- ⚠️ agent-builder-service (1-3 min)

### Slow Services (5-15 minutes)
- ❌ clinical-workflow-service (8-10 min)
- ❌ quality-measure-service (7-9 min)
- ❌ patient-service (5-7 min)

### Very Slow Services (15+ minutes)
- ❌ fhir-service (requires setup)
- ❌ comprehensive E2E suites

---

## Detailed Test Profile - Phase 3 Classified Tests

### Unit Tests Performance (33 files tagged)

| Service | Test Count | Est. Time | Per Test |
|---------|-----------|-----------|----------|
| audit-query-service | 0 | 0s | — |
| care-gap-service | 8 | 0.5-1s | <150ms |
| cdr-processor-service | 13 | 0.8-1.2s | <100ms |
| **TOTAL UNIT TESTS** | **21** | **1.3-2.2s** | **<100ms** |

### Integration Tests Performance (33 files tagged)

| Service | Test Count | Est. Time | Per Test |
|---------|-----------|-----------|----------|
| audit-query-service | 6 | 2-3s | 300-500ms |
| care-gap-service | 8 | 3-5s | 400-600ms |
| cdr-processor-service | 2 | 0.5-1s | 250-500ms |
| **TOTAL INTEGRATION TESTS** | **16** | **5-9s** | **300-600ms** |

### All Phase 3 Tests Combined

| Category | Count | Time | Parallel? |
|----------|-------|------|-----------|
| Unit tests | 21 | 1-2s | ✅ Full |
| Integration tests | 16 | 5-9s | ⚠️ Limited |
| **TOTAL PHASE 3** | **37** | **6-11s** | ✅ Good |

---

## Optimization Roadmap

### Priority 1: High Impact, Low Effort (15-20 min savings)

1. **Add @Tag("slow")** to 24 tests with Thread.sleep()
   - Impact: Identify slow tests, enable filtering
   - Effort: 1 hour
   - Time Savings: 0 min (identification only)

2. **Create testFast Gradle task** (excluding @Tag("slow"))
   - Impact: Fast developer feedback
   - Effort: 15 minutes
   - Time Savings: 5-10 min per dev run

3. **Reduce Kafka polling timeouts** from 30s to 5s in 11 tests
   - Impact: Faster integration tests
   - Effort: 30 minutes
   - Time Savings: 2-3 minutes

### Priority 2: Medium Impact, Medium Effort (10-15 min savings)

4. **Split mega test files** (1,900+ lines into focused classes)
   - Impact: Better test organization
   - Effort: 2-4 hours
   - Time Savings: 2-5 minutes

5. **Replace Thread.sleep()** with proper event conditions
   - Impact: Faster tests + more reliable
   - Effort: 4-8 hours
   - Time Savings: 2-3 minutes

6. **Enable Docker container sharing** via @Testcontainers static containers
   - Impact: Parallel container reuse
   - Effort: 2-3 hours
   - Time Savings: 5-10 minutes

### Priority 3: Phase 5 (Embedded Kafka - 20-30 min savings)

7. **Replace Testcontainers Kafka with @EmbeddedKafka**
   - Impact: No Docker startup overhead
   - Effort: 8-12 hours
   - Time Savings: 10-15 minutes
   - Timeline: Phase 5 (Q2 2026)

8. **Re-enable disabled heavyweight tests**
   - Impact: Comprehensive testing
   - Effort: 4-6 hours
   - Time Savings: -8 minutes (adds tests, but faster with EmbeddedKafka)
   - Timeline: Phase 5

---

## Baseline Metrics Summary

### Before Phase 4
```
✅ testUnit:        ~30-60s (Phase 3)
✅ testIntegration: ~3-5m (Phase 3)
❌ Full suite:      ~30-45m (with Docker, 613 tests)
```

### After Phase 4 (Projected)
```
✅ testUnit:        ~30-60s (unchanged)
✅ testIntegration: ~3-5m (unchanged)
✅ testFast:        ~2-3m (unit + quick integration)
⚠️  Full suite:     ~25-40m (5-10m faster)
```

### After Phase 5 (Goal)
```
✅ testUnit:        ~30-60s (unchanged)
✅ testIntegration: ~2-3m (faster without Docker)
✅ testFast:        ~1-2m (more tests included)
✅ Full suite:      ~15-25m (50% reduction!)
```

---

## Recommendations

### For Developers

1. **Use `./gradlew testUnit`** for development (30-60s feedback)
2. **Use `./gradlew testFast`** before commits (1-2m feedback) - coming in Phase 4
3. **Run full suite** only before PR creation

### For CI/CD

1. **Split test jobs** - unit and integration in parallel
2. **Enable service-specific tests** based on code changes
3. **Cache Docker images** to reduce startup time

### For Next Phases

1. **Phase 4:** Add @Tag("slow"), create testFast task
2. **Phase 5:** Replace Docker Kafka with @EmbeddedKafka
3. **Phase 6:** Split mega test files and refactor patterns

---

## Success Criteria (Phase 4)

- [ ] Performance baseline established (this document)
- [ ] Slow tests identified and tagged
- [ ] @Tag("slow") applied to 20+ tests
- [ ] testFast and testSlow Gradle tasks created
- [ ] Kafka polling timeouts reduced
- [ ] Updated CLAUDE.md with performance tips
- [ ] Phase 5 kickoff documented

---

**Status:** BASELINE ESTABLISHED ✅
**Next Step:** Task #9 - Analyze slow tests and implement optimizations
