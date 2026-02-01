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

# Traditional per-service test
./gradlew :modules:services:SERVICENAME:test
```

---

## When to Use Each

| Mode | When | Speed | Parallelization |
|------|------|-------|-----------------|
| **testUnit** | Quick feedback on unit tests | 30-60s | Light (2 forks) |
| **testFast** | Daily development | 1.5-2min | High (6 forks) ⚡ |
| **testIntegration** | Integration layer changes | 1.5-2min | High (6 forks) ⚡ |
| **testSlow** | Rare (heavyweight tests) | 3-5min | None (sequential) |
| **testAll** | **Before final merge** | 15-25min | **None (sequential, stable)** |

---

## Performance Gains (Phase 6 Task 5)

```
testFast:        2-3 min → 1.5-2 min  (25-30% faster) ⚡
testIntegration: 2-3 min → 1.5-2 min  (25-30% faster) ⚡
testUnit:        45-60s  → 30-45s     (15-25% faster) ⚡
testSlow:        3-5 min (unchanged, sequential for stability)
testAll:         15-25 min (unchanged, sequential for stability)
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
**Parallel forks:** 6 (CPU / 2)
**Patient service:** Sequential (special case)
**JVM optimizations:** String deduplication, tiered compilation

---

## Full Documentation

👉 **[Gradle Parallel Execution Guide](./GRADLE_PARALLEL_EXECUTION_GUIDE.md)** - Comprehensive reference

---

_Phase 6 Task 5 - January 2026_
