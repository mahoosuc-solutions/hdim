# Gradle Test Quick Reference

**Quick command reference for HDIM test modes (Phase 6 Task 5)**

---

## Test Commands

```bash
# Development - Fastest feedback (1.5-2 min)
./gradlew testFast

# Integration tests (1.5-2 min)
./gradlew testIntegration

# Unit tests only (30-60 sec)
./gradlew testUnit

# Slow/heavyweight tests (3-5 min)
./gradlew testSlow

# ALL tests - final validation before merge (15-25 min)
./gradlew testAll

# ALL tests - Parallel mode (EXPERIMENTAL - 5-8 min on 8+ cores)
./gradlew testParallel

# Traditional per-service test
./gradlew :modules:services:SERVICENAME:test
```

---

## When to Use Each

| Mode | When | Speed | Parallelization | Stability |
|------|------|-------|-----------------|-----------|
| **testUnit** | Quick feedback on unit tests | 30-60s | Light (2 forks) | ✅ Stable |
| **testFast** | Daily development | 1.5-2min | High (6 forks) | ✅ Stable |
| **testIntegration** | Integration layer changes | 1.5-2min | High (6 forks) | ✅ Stable |
| **testSlow** | Rare (heavyweight tests) | 3-5min | None (sequential) | ✅ Stable |
| **testAll** | **Before final merge** | 15-25min | **None (sequential)** | **✅ 100% Stable** |
| **testParallel** | Quick feedback on 8+ core machines (EXPERIMENTAL) | 5-8min | Aggressive (CPU) | ⚠️ May be flaky |

---

## Performance Gains (Phase 6 Task 5 + 7)

```
testFast:        2-3 min → 1.5-2 min  (25-30% faster) ⚡
testIntegration: 2-3 min → 1.5-2 min  (25-30% faster) ⚡
testUnit:        45-60s  → 30-45s     (15-25% faster) ⚡
testSlow:        3-5 min (unchanged, sequential for stability)
testAll:         15-25 min (sequential, 100% stable for merge validation)
testParallel:    15-25 min → 5-8 min (60-70% faster on 8+ core systems) ⚡⚡⚡ EXPERIMENTAL
```

---

## Developer Workflow

### Feature Development
```bash
# After code changes
./gradlew testFast

# Before committing
./gradlew testFast && git commit
```

### Integration Changes
```bash
# Validate integration layer
./gradlew testIntegration
```

### Final Validation (Before Push)
```bash
# Maximum stability
./gradlew testAll

# If all pass, push to main
git push origin <branch>
```

### Parallel Testing on Powerful Machines (EXPERIMENTAL)
```bash
# Quick feedback on 8+ core systems (EXPERIMENTAL)
./gradlew testParallel

# If you get flaky failures, verify with sequential
./gradlew testAll

# If testAll passes but testParallel fails, it's likely a race condition
# Use testAll before merging to main
```

---

## Tagging Tests

Mark slow tests to exclude from parallel modes:

```java
// Regular unit/integration test (included in testFast)
@Test
void myTest() { }

// Slow/heavyweight test (only in testSlow, testAll)
@Test
@Tag("slow")
void mySlowTest() { }

// Integration test
@Test
@Tag("integration")
void myIntegrationTest() { }

// Performance benchmark
@Test
@Tag("performance")
void myPerformanceTest() { }
```

---

## Troubleshooting

### Tests fail in parallel but pass sequential?
→ Check for shared state (statics, singletons)
→ Run: `./gradlew testAll` (sequential)

### Out of memory?
→ Use: `./gradlew testSlow` (sequential, less overhead)
→ Or reduce: `maxParallelForks = 3` in build.gradle.kts

### Need to disable parallel?
→ Just use: `./gradlew testAll` (already sequential)

---

## System Configuration

**System:** 12 CPUs
**Parallel forks:** 6 (CPU / 2) for testFast/testIntegration
**Patient service:** Sequential (special case)
**JVM optimizations:** String deduplication, tiered compilation
**testParallel:** Full CPU count (12), higher memory (-Xmx2g)

---

## testParallel - Experimental Mode Warnings

**⚠️ testParallel is EXPERIMENTAL and may produce flaky results.**

### When to Use
- You have a powerful machine with 8+ cores and 16+ GB RAM
- You want rapid feedback during development (5-8 min for all tests)
- You're willing to run `testAll` for final merge validation

### When NOT to Use
- System has fewer than 8 cores (likely OOM or resource starvation)
- Shared systems or laptops (will cause slowdowns for everyone)
- Pre-commit hooks or CI/CD (use `testAll` for stability)
- Final validation before merging to main (use `testAll`)

### Troubleshooting Flaky Tests
1. **Run sequential version:** `./gradlew testAll`
2. **If testAll passes but testParallel fails:** It's a race condition
3. **Solution:** Use `testAll` before pushing
4. **Report:** Open issue if consistent failure pattern

---

## Full Documentation

👉 **[Gradle Parallel Execution Guide](./GRADLE_PARALLEL_EXECUTION_GUIDE.md)** - Comprehensive reference

---

_Phase 6 Task 5 + 7 - January 2026_
