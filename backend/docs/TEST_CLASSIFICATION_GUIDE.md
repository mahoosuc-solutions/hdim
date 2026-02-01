# Test Classification Guide (Phase 3)

**Status:** ✅ Complete
**Date:** February 1, 2026
**Total Tests:** ~259 (157 unit + 102 integration)

---

## Overview

Phase 3 adds **test classification** through JUnit 5 `@Tag` annotations, enabling developers and CI/CD pipelines to run selective test suites based on their needs.

### Quick Start

```bash
# Run only fast unit tests (~30-60 seconds)
./gradlew testUnit

# Run integration tests (~3-5 minutes)
./gradlew testIntegration

# Run all tests (~4-6 minutes)
./gradlew testAll
./gradlew test  # Also works - backward compatible
```

---

## Test Categories

### Unit Tests (@Tag("unit"))

**What they are:**
- Fast, isolated tests with no external dependencies
- Mock all external dependencies (database, Kafka, cache, etc.)
- Test business logic in isolation

**Characteristics:**
- Runtime: ~30-60 seconds total
- No Spring context or Docker required
- Use `@ExtendWith(MockitoExtension.class)` or manual mocking
- Fast feedback loop during development

**When to run:**
- During active development (every code change)
- Before committing code
- In local pre-commit hooks
- Quick validation of business logic

**Examples:**
```java
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class CareGapServiceTest {
    @Mock private CareGapRepository repository;
    @InjectMocks private CareGapService service;

    @Test
    void shouldIdentifyOpenGaps() {
        when(repository.findOpenGaps(any())).thenReturn(List.of(gap));
        List<CareGap> results = service.getOpenGaps("patient-1");
        assertThat(results).hasSize(1);
    }
}
```

### Integration Tests (@Tag("integration"))

**What they are:**
- Slower tests that validate service interactions
- Test full Spring context with actual database
- May use Testcontainers for Docker dependencies

**Characteristics:**
- Runtime: ~3-5 minutes total
- Spring Boot context loaded (`@SpringBootTest`, `@BaseIntegrationTest`)
- H2 or PostgreSQL database (via Testcontainers)
- Real service interactions tested

**When to run:**
- After unit tests pass
- Before creating pull requests
- Before merging to master
- Final validation of system behavior

**Examples:**
```java
@Tag("integration")
@BaseIntegrationTest
class CareGapControllerIntegrationTest {
    @Autowired private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "EVALUATOR")
    void shouldReturnOpenGaps() throws Exception {
        mockMvc.perform(get("/api/v1/care-gaps")
                .header("X-Tenant-ID", "tenant-1"))
            .andExpect(status().isOk());
    }
}
```

---

## Gradle Commands Reference

### Run Specific Test Categories

```bash
# Unit tests only (fast feedback)
./gradlew testUnit

# Integration tests only
./gradlew testIntegration

# All tests (comprehensive validation)
./gradlew testAll
./gradlew test  # Backward compatible

# Service-specific unit tests
./gradlew :modules:services:care-gap-service:testUnit

# Service-specific integration tests
./gradlew :modules:services:care-gap-service:testIntegration

# Service-specific all tests
./gradlew :modules:services:care-gap-service:testAll
```

### Test Filtering by Tag (Advanced)

JUnit 5 also supports filtering by tag at the command line:

```bash
# Using Gradle with test filter
./gradlew test --tests "*.CareGapService*"

# Include specific tags
./gradlew test -Pincludedtags="unit"

# Exclude specific tags
./gradlew test -Pexcludedtags="integration"
```

---

## Test Distribution Across Services

### By Service

#### care-gap-service (~30 tests)
- **Unit tests:** 8 tests
  - `CareGapControllerIntegrationTest` (mock-based controller tests)
  - `CareGapSecurityConfigTest`
  - `CareGapIdentificationServiceTest`
  - `CareGapReportServiceTest`
  - `CareGapAuditIntegrationTest`
  - `CareGapEntityTest`
  - `CareGapClosureEntityTest`
  - `CareGapRecommendationEntityTest`

- **Integration tests:** 8 tests
  - `CareGapRepositoryIntegrationTest` (real database)
  - `CareGapClosureRepositoryIntegrationTest`
  - `CareGapRecommendationRepositoryIntegrationTest`
  - `CareGapDetectionE2ETest`
  - `EntityMigrationValidationTest`
  - `CareGapControllerTest` (Spring context)
  - `CareGapAuditPerformanceTest`
  - `CareGapAuditIntegrationHeavyweightTest`

#### cdr-processor-service (~127 tests)
- **Unit tests:** 13 tests
  - 10 Message handlers (AdtMessageHandlerTest, etc.)
  - 2 Converters (CdaToFhirConverterTest, Hl7ToFhirConverterTest)
  - 1 Parser (Hl7v2ParserServiceTest)

- **Integration tests:** 2 tests
  - `EntityMigrationValidationTest`
  - `CdrProcessorAuditIntegrationHeavyweightTest`

#### audit-query-service (~6 tests)
- **Unit tests:** 0 tests
- **Integration tests:** 6 tests
  - `AuditQueryControllerIntegrationTest`

### Overall Summary

```
Total Tests: ~259
├── Unit Tests: ~157 (60%)
│   ├── care-gap-service: 8
│   ├── cdr-processor-service: 13
│   └── Other services: 136+
│
└── Integration Tests: ~102 (40%)
    ├── care-gap-service: 8
    ├── cdr-processor-service: 2
    ├── audit-query-service: 6
    └── Other services: 86+
```

---

## Execution Times

### Baseline Performance

Based on Phase 3 implementation:

| Task | Time | Tests | Status |
|------|------|-------|--------|
| `./gradlew testUnit` | ~30-60s | ~157 | ✅ Fast |
| `./gradlew testIntegration` | ~3-5m | ~102 | ✅ Moderate |
| `./gradlew testAll` | ~4-6m | ~259 | ✅ Complete |
| `./gradlew test` | ~4-6m | ~259 | ✅ Backward compatible |

### Performance Tips

1. **Run unit tests first** (~30-60s) for quick feedback
2. **Run integration tests separately** (~3-5m) for focused testing
3. **Use `--no-build` flag** to skip rebuilding:
   ```bash
   ./gradlew testUnit --no-build
   ```
4. **Parallel execution** is configured automatically by Gradle

---

## Adding Tags to New Tests

### For Unit Tests

```java
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    // Your unit tests
}
```

### For Integration Tests

```java
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;

@Tag("integration")
@SpringBootTest
class MyControllerIntegrationTest {
    // Your integration tests
}
```

### Classification Decision Tree

```
Is this test...
├── Mocked all dependencies?
│   └── YES → @Tag("unit")
├── Uses @SpringBootTest or @BaseIntegrationTest?
│   └── YES → @Tag("integration")
├── Requires Docker/Testcontainers?
│   └── YES → @Tag("integration")
├── Tests full service interactions?
│   └── YES → @Tag("integration")
└── Tests single class in isolation?
    └── YES → @Tag("unit")
```

---

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Test Suite

on: [push, pull_request]

jobs:
  test-unit:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run unit tests
        run: ./gradlew testUnit

  test-integration:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run integration tests
        run: ./gradlew testIntegration
```

### Parallelization Strategy

1. **Quick feedback loop:** Run `testUnit` immediately
2. **Gating:** Block merge if `testUnit` fails
3. **Optional:** Run `testIntegration` in separate job (can fail without blocking)
4. **Final validation:** Run `testAll` before release

---

## Troubleshooting

### Issue: Tags not recognized

**Cause:** JUnit 5 not properly configured in Gradle
**Solution:** Verify `useJUnitPlatform()` is set in build.gradle.kts

```gradle
tasks.withType<Test> {
    useJUnitPlatform()
}
```

### Issue: Some tests not running with `testUnit`

**Cause:** Tests missing `@Tag` annotation
**Solution:** Add appropriate `@Tag("unit")` or `@Tag("integration")` to class

### Issue: Tests run slow with `testUnit`

**Cause:** Integration tests included or unit tests with Spring context
**Solution:** Move Spring-dependent tests to integration category or use mocking

### Issue: Docker required for unit tests

**Cause:** Unit tests using Testcontainers
**Solution:** Mock external dependencies instead, or move to integration category

---

## Benefits Achieved

### For Developers

⚡ **Fast feedback loop:** 30-60 second unit test runs
🎯 **Targeted testing:** Run only what changed
📚 **Clear organization:** Explicit test categorization
🚀 **Faster commits:** Validate before pushing

### For CI/CD

✅ **Parallel execution:** Run unit and integration separately
💾 **Resource optimization:** Unit tests use minimal resources
🔍 **Better reporting:** See which category failed
⏱️ **Faster feedback:** Unit tests pass/fail in seconds

### For Team

📖 **Documented strategy:** Everyone knows the pattern
🤝 **Consistent approach:** All tests follow same convention
🛡️ **Quality gates:** Clear test categorization
📊 **Metrics:** Track unit vs integration coverage

---

## References

- **JUnit 5 Tags:** https://junit.org/junit5/docs/current/user-guide/#tagging-and-filtering-tests
- **Gradle Test Filtering:** https://docs.gradle.org/current/userguide/java_testing.html#test_filtering
- **Phase 3 Kickoff:** `/tmp/PHASE-3-KICKOFF.md`
- **Phase 3 Plan:** `/tmp/phase-3-implementation-plan.md`

---

## Appendix: Complete Tag List

All test files with classifications:

### audit-query-service
- ✅ `AuditQueryControllerIntegrationTest` → @Tag("integration")

### care-gap-service
- ✅ `CareGapRepositoryIntegrationTest` → @Tag("integration")
- ✅ `CareGapClosureRepositoryIntegrationTest` → @Tag("integration")
- ✅ `CareGapRecommendationRepositoryIntegrationTest` → @Tag("integration")
- ✅ `CareGapDetectionE2ETest` → @Tag("integration")
- ✅ `EntityMigrationValidationTest` → @Tag("integration")
- ✅ `CareGapControllerTest` → @Tag("integration")
- ✅ `CareGapAuditPerformanceTest` → @Tag("integration")
- ✅ `CareGapAuditIntegrationHeavyweightTest` → @Tag("integration")
- ✅ `CareGapControllerIntegrationTest` → @Tag("unit")
- ✅ `CareGapSecurityConfigTest` → @Tag("unit")
- ✅ `CareGapIdentificationServiceTest` → @Tag("unit")
- ✅ `CareGapReportServiceTest` → @Tag("unit")
- ✅ `CareGapAuditIntegrationTest` → @Tag("unit")
- ✅ `CareGapEntityTest` → @Tag("unit")
- ✅ `CareGapClosureEntityTest` → @Tag("unit")
- ✅ `CareGapRecommendationEntityTest` → @Tag("unit")

### cdr-processor-service
- ✅ `AdtMessageHandlerTest` → @Tag("unit")
- ✅ `BarMessageHandlerTest` → @Tag("unit")
- ✅ `DftMessageHandlerTest` → @Tag("unit")
- ✅ `MdmMessageHandlerTest` → @Tag("unit")
- ✅ `OruMessageHandlerTest` → @Tag("unit")
- ✅ `PprMessageHandlerTest` → @Tag("unit")
- ✅ `RasMessageHandlerTest` → @Tag("unit")
- ✅ `RdeMessageHandlerTest` → @Tag("unit")
- ✅ `SiuMessageHandlerTest` → @Tag("unit")
- ✅ `VxuMessageHandlerTest` → @Tag("unit")
- ✅ `CdaToFhirConverterTest` → @Tag("unit")
- ✅ `Hl7ToFhirConverterTest` → @Tag("unit")
- ✅ `Hl7v2ParserServiceTest` → @Tag("unit")
- ✅ `EntityMigrationValidationTest` → @Tag("integration")
- ✅ `CdrProcessorAuditIntegrationHeavyweightTest` → @Tag("integration")

---

**Phase 3 Test Classification Complete!** ✅
