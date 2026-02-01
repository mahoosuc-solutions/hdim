# TEST COVERAGE GUIDE

Code coverage measurement, reporting, and enforcement across HDIM microservices.

**Last Updated**: January 19, 2026
**Status**: Phase 1.5 Blocker #3 - Test Coverage Standards
**Coverage Goals**: 80%+ line coverage, 85%+ method coverage for business logic

---

## Overview

This guide standardizes code coverage practices across all 50+ services to maintain code quality and prevent regression.

### Coverage Metrics

| Metric | Definition | Target |
|--------|-----------|--------|
| **Line Coverage** | % of executable lines executed | 80% |
| **Branch Coverage** | % of conditional branches tested | 75% |
| **Method Coverage** | % of methods with test calls | 85% |
| **Instruction Coverage** | % of individual bytecode instructions | 80% |

### Coverage Pyramid

```
100% ────────────────────────────────────────
     │ Mutation Testing (rare issues)       │
 90% ├─────────────────────────────────────┤
     │ Integration Tests (edge cases)      │
 80% ├─────────────────────────────────────┤
     │ Unit Tests (common paths)           │
 70% ├─────────────────────────────────────┤
     │ Legacy Code (unmaintained)          │
  0% ────────────────────────────────────────
```

---

## JaCoCo Configuration

### Setup JaCoCo in Gradle

```gradle
// build.gradle.kts
plugins {
    jacoco
}

jacoco {
    toolVersion = "0.8.10"
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.test)

    reports {
        xml.required = true      // For CI/CD tools like SonarQube
        html.required = true     // Human-readable HTML report
        csv.required = false     // Not typically used
    }

    classDirectories.setFrom(files(classDirectories.files.map { file ->
        fileTree(file) {
            // Exclude classes from coverage calculation
            exclude(
                "**/config/**",           // Spring configuration
                "**/entity/**",           // JPA entities (auto-generated)
                "**/dto/**",              // Data transfer objects
                "**/request/**",          // Request classes
                "**/response/**",         // Response classes
                "**/*Application.class",  // Spring Boot main class
                "**/model/**",            // Domain models
            )
        }
    }))
}
```

### Generate Coverage Reports

```bash
# Run tests and generate coverage report
./gradlew test jacocoTestReport

# View HTML report
open build/reports/jacoco/test/html/index.html  # macOS
xdg-open build/reports/jacoco/test/html/index.html  # Linux
start build/reports/jacoco/test/html/index.html  # Windows

# Generate report without tests
./gradlew jacocoTestReport
```

### Report Output Structure

```
build/reports/jacoco/test/
├── html/
│   ├── index.html                    # Main coverage report
│   ├── status.svg                    # Coverage badge
│   └── sessions.html
├── jacocoTestReport.xml              # Machine-readable (for SonarQube)
└── jacocoTestReport.csv              # Spreadsheet format
```

---

## Coverage Verification

### Enforce Coverage Thresholds

```gradle
// build.gradle.kts
tasks.jacocoTestCoverageVerification {
    violationRules {
        // PACKAGE-LEVEL RULES
        rule {
            element = "PACKAGE"
            excludePattern = "com.healthdata.*.config.*"

            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80"  // 80% minimum line coverage
            }
        }

        // SERVICE-LEVEL RULES
        rule {
            element = "CLASS"
            includes = listOf("*Service", "*Repository", "*Manager")

            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.85"  // Business logic must be well-tested
            }
        }

        // CONTROLLER-LEVEL RULES
        rule {
            element = "CLASS"
            includes = listOf("*Controller")

            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80"
            }
        }

        // BRANCH COVERAGE FOR COMPLEX LOGIC
        rule {
            element = "CLASS"
            includes = listOf("*Calculator", "*Validator", "*Evaluator")

            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.75"  // Complex logic needs branch testing
            }
        }

        // METHOD COVERAGE RULE
        rule {
            element = "CLASS"

            limit {
                counter = "METHOD"
                value = "COVEREDRATIO"
                minimum = "0.80"  // Most methods should have tests
            }
        }
    }
}
```

### Verify Coverage in Build

```bash
# Verify coverage meets thresholds
./gradlew test jacocoTestCoverageVerification

# This will FAIL the build if coverage is below thresholds
# Error output:
# The following classes do not meet the minimum coverage:
# - PatientService: 72% (requires 85%)
# - QualityMeasureCalculator: 68% (requires 85%)
```

### Make Coverage Verification Optional

Use a separate task for optional verification:

```gradle
// build.gradle.kts
tasks.register("verifyTestCoverage") {
    dependsOn(tasks.jacocoTestCoverageVerification)

    doLast {
        println("✅ Coverage verification passed!")
    }
}

// Exclude from default build but run in CI
// Default: ./gradlew build (skips coverage check)
// CI: ./gradlew build verifyTestCoverage (enforces coverage)
```

---

## Coverage Targets by Module Type

### Core Services (Clinical Logic)

```gradle
rule {
    element = "CLASS"
    includes = listOf("com.healthdata.quality.*Service")

    limit {
        counter = "LINE"
        value = "COVEREDRATIO"
        minimum = "0.85"  // Critical business logic
    }
}
```

**Examples**: QualityMeasureService, CQLEngineService, CareGapService

**Why 85%**: These services perform critical clinical calculations. Missing coverage exposes bugs affecting patient care.

### API Controllers

```gradle
rule {
    element = "CLASS"
    includes = listOf("*Controller")

    limit {
        counter = "LINE"
        value = "COVEREDRATIO"
        minimum = "0.80"  // All endpoints must be tested
    }
}
```

**Examples**: PatientController, QualityMeasureController

**Why 80%**: Public API endpoints need integration tests. Cannot miss error handling.

### Data Access (Repositories)

```gradle
rule {
    element = "CLASS"
    includes = listOf("*Repository")

    limit {
        counter = "LINE"
        value = "COVEREDRATIO"
        minimum = "0.90"  // All queries must be tested
    }
}
```

**Examples**: PatientRepository, ConditionRepository

**Why 90%**: Databases are critical. Must test all query variations (happy path + filtering + empty result).

### Utilities & Helpers

```gradle
rule {
    element = "CLASS"
    includes = listOf("*Util", "*Helper")

    limit {
        counter = "LINE"
        value = "COVEREDRATIO"
        minimum = "0.85"  // Utility functions are reused
    }
}
```

**Examples**: DateUtils, ValidationHelper

**Why 85%**: Utilities are used across services. Bug here affects many code paths.

### Configuration Classes

```gradle
rule {
    element = "CLASS"
    includes = listOf("*Config", "*Configuration")

    limit {
        counter = "LINE"
        value = "COVEREDRATIO"
        minimum = "0.60"  // Configuration often not fully exercised
    }
}
```

**Why 60%**: Configuration classes have many conditional branches (profiles, feature flags). Full coverage is difficult and less critical.

### Legacy Code

If you have unmaintained code:

```gradle
// Mark as excluded from coverage calculations
classDirectories.setFrom(files(classDirectories.files.map { file ->
    fileTree(file) {
        exclude("com/healthdata/legacy/**")  // Don't measure legacy
    }
}))
```

---

## Coverage Reporting & Analysis

### Read the HTML Report

```
build/reports/jacoco/test/html/index.html
├── Package Summary (top-level view)
│   ├── com.healthdata.patient
│   │   Line Coverage:    85%
│   │   Branch Coverage:  72%
│   │   Complexity:       2.3
│   └── [Click to drill down]
├── Class Details (method-level view)
│   ├── PatientService.java
│   │   Methods:         12/13 covered (92%)
│   │   Lines:           85/100 (85%)
│   │   Branches:        14/20 covered (70%)
│   └── [Color-coded source view]
└── Source View (line-by-line)
    ├── Green lines:     Executed
    ├── Red lines:       Not executed
    └── Yellow lines:    Partially covered (not all branches)
```

### Identify Uncovered Code

```bash
# Generate report
./gradlew jacocoTestReport

# Look for RED lines in HTML report (not executed)
# Common causes:
# 1. Error handling paths not tested
# 2. Optional features not exercised
# 3. Configuration-dependent code
# 4. Dead code (should be removed)

# Example: Red lines in try-catch block mean exception path untested
try {
    // ✅ Green - tested
    patientRepository.save(patient);
} catch (Exception e) {
    // ❌ Red - not tested
    logger.error("Save failed", e);
    throw new RuntimeException(e);
}

# FIX: Write test that triggers exception
@Test
void shouldRethrowAsRuntimeException_WhenSaveFails() {
    when(patientRepository.save(any())).thenThrow(SQLException.class);

    assertThatThrownBy(() -> patientService.createPatient(patient))
        .isInstanceOf(RuntimeException.class);
}
```

### Coverage Trends

Track coverage over time in a spreadsheet or dashboard:

```
Date       | Coverage | Trend   | Notes
-----------|----------|---------|----------------------------------
2024-01-01 | 75%      | ↑       | Started tracking
2024-01-15 | 77%      | ↑       | Added integration tests
2024-02-01 | 79%      | ↑       | Refactored legacy code
2024-02-15 | 78%      | ↓       | Added new feature (uncovered yet)
2024-03-01 | 82%      | ↑       | Tests written for new feature
```

---

## CI/CD Integration

### GitHub Actions Workflow

```yaml
# .github/workflows/test-coverage.yml
name: Test Coverage

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  coverage:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: gradle

      - name: Run tests with coverage
        run: ./gradlew test jacocoTestReport

      - name: Upload to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./build/reports/jacoco/test/jacocoTestReport.xml
          flags: unittests
          name: codecov-umbrella
          fail_ci_if_error: true

      - name: Comment PR with coverage
        if: github.event_name == 'pull_request'
        uses: romeovs/lcov-reporter-action@v0.3.1
        with:
          github-token: ${{ secrets.GITHUB_TOKEN }}
          lcov-file: ./build/reports/jacoco/test/jacocoTestReport.xml

      - name: Verify coverage thresholds
        run: ./gradlew test jacocoTestCoverageVerification
        continue-on-error: false
```

### Codecov Badge in README

Add coverage badge to your service README:

```markdown
[![codecov](https://codecov.io/gh/your-org/hdim-master/branch/main/graph/badge.svg?flag=patient-service)](https://codecov.io/gh/your-org/hdim-master)

## Coverage Status
- Patient Service: 82% line coverage
- Quality Measure Service: 85% line coverage
- CQL Engine Service: 88% line coverage
```

### Sonar Integration

Integrate with SonarQube for enterprise coverage tracking:

```yaml
# .github/workflows/sonar.yml
name: Sonar Analysis

on:
  push:
    branches: [main, develop]

jobs:
  sonar:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3
        with:
          fetch-depth: 0  # Required by SonarQube

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'

      - name: Run tests
        run: ./gradlew test jacocoTestReport

      - name: SonarQube Scan
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: |
          ./gradlew sonarqube \
            -Dsonar.projectKey=hdim \
            -Dsonar.host.url=https://sonar.example.com \
            -Dsonar.login=$SONAR_TOKEN
```

---

## Improving Test Coverage

### Strategy: Top-Down Coverage Improvement

1. **Get baseline**: Run `jacocoTestReport` to identify gaps
2. **Focus on critical paths**: 85%+ coverage for business logic first
3. **Add integration tests**: For complex workflows
4. **Add edge cases**: Error handling, boundary conditions
5. **Test data variants**: Multi-tenant scenarios, different inputs

### Common Uncovered Patterns

#### Pattern 1: Exception Handling

```java
// ❌ Exception path is NOT covered
public void savePatient(Patient patient) {
    try {
        patientRepository.save(patient);
    } catch (DataIntegrityViolationException e) {
        logger.error("Duplicate patient", e);  // ❌ RED - untested
        throw new DuplicatePatientException(patient.getId());
    }
}

// ✅ FIX: Write exception test
@Test
void shouldThrowDuplicatePatientException_WhenPatientAlreadyExists() {
    // GIVEN
    Patient patient = new Patient();
    when(patientRepository.save(patient))
        .thenThrow(new DataIntegrityViolationException("Duplicate"));

    // WHEN & THEN
    assertThatThrownBy(() -> patientService.savePatient(patient))
        .isInstanceOf(DuplicatePatientException.class);
}
```

#### Pattern 2: Optional Branches

```java
// ❌ else branch NOT covered
public PatientResponse getPatientIfActive(String patientId) {
    Optional<Patient> patient = patientRepository.findById(patientId);
    if (patient.isPresent()) {
        return mapToResponse(patient.get());  // ✅ GREEN - tested
    } else {
        logger.warn("Patient not found");     // ❌ RED - untested
        return null;
    }
}

// ✅ FIX: Write test for empty case
@Test
void shouldReturnNull_WhenPatientNotFound() {
    when(patientRepository.findById(any())).thenReturn(Optional.empty());

    PatientResponse result = patientService.getPatientIfActive("123");

    assertThat(result).isNull();
}
```

#### Pattern 3: Multi-Path Conditionals

```java
// ❌ Multiple uncovered branches
public String getScore(int value) {
    if (value < 0) {
        return "INVALID";       // ❌ untested
    } else if (value < 50) {
        return "LOW";           // ✅ tested
    } else if (value < 100) {
        return "MEDIUM";        // ❌ untested
    } else {
        return "HIGH";          // ❌ untested
    }
}

// ✅ FIX: Use @ParameterizedTest
@ParameterizedTest
@CsvSource({
    "-1, INVALID",
    "0, INVALID",
    "25, LOW",
    "75, MEDIUM",
    "100, HIGH",
    "150, HIGH",
})
void shouldReturnCorrectScore(int value, String expected) {
    assertThat(scoreService.getScore(value)).isEqualTo(expected);
}
```

#### Pattern 4: Configuration-Dependent Code

```java
// ❌ Feature flag path might be untested
@Configuration
public class CacheConfiguration {

    @Bean
    @ConditionalOnProperty(name = "cache.enabled", havingValue = "true")
    public CacheManager cacheManager() {
        return new RedisCacheManager();  // ❌ untested without property
    }
}

// ✅ FIX: Test both configurations
@SpringBootTest
@ActiveProfiles("cache-enabled")
class CacheEnabledTest {
    @Autowired(required = false)
    private CacheManager cacheManager;

    @Test
    void shouldEnableCache() {
        assertThat(cacheManager).isNotNull();
    }
}

@SpringBootTest
@ActiveProfiles("cache-disabled")
class CacheDisabledTest {
    @Autowired(required = false)
    private CacheManager cacheManager;

    @Test
    void shouldDisableCache() {
        assertThat(cacheManager).isNull();
    }
}
```

### Adding Missing Tests Incrementally

```bash
# 1. Generate baseline report
./gradlew jacocoTestReport

# 2. Read HTML report, find uncovered classes
# Example: PatientValidator.java at 62% coverage

# 3. Write tests for uncovered paths
cat > src/test/java/PatientValidatorTest.java << 'EOF'
@Test
void shouldValidateDateOfBirth_WhenInvalid() { ... }

@Test
void shouldValidateSSN_WhenInvalid() { ... }
EOF

# 4. Re-run coverage
./gradlew jacocoTestReport

# 5. Verify improvement
# PatientValidator.java now at 85% coverage ✅
```

---

## Mutation Testing (Advanced)

Mutation testing ensures tests actually validate behavior (not just line coverage).

### PIT Configuration

```gradle
// build.gradle.kts
plugins {
    id("info.solidsoft.pitest") version "1.14.5"
}

pitest {
    junit5PluginVersion = "1.1.2"
    targetClasses = setOf("com.healthdata.*")
    excludedClasses = setOf(
        "com.healthdata.*.config.*",
        "com.healthdata.*.entity.*",
        "com.healthdata.*.dto.*",
    )
    targetTests = setOf("com.healthdata.*Test")
    mutationUnitSize = 0
}
```

### Run Mutation Tests

```bash
# Run PIT mutation testing
./gradlew pitest

# View HTML report
open build/reports/pitest/index.html

# Mutations report shows:
# - SURVIVED: Test didn't catch the mutation (bad test)
# - KILLED: Test caught the mutation (good test)
# - NONCOVERABLE: Code path not executed
```

### Example Mutation Test Report

```
Class: PatientValidator

Mutation 1: Changed >= to >
  int line 45: if (age >= 18) { ... }
  Result: SURVIVED ❌ (test didn't catch this)
  FIX: Test boundary case age=18

Mutation 2: Changed true to false
  boolean line 52: return true;
  Result: KILLED ✅ (test caught this)

Mutation 3: Removed null check
  line 30: if (patient != null) {
  Result: SURVIVED ❌ (test didn't cover null case)
  FIX: Add test for null patient
```

---

## Coverage Exclusions

### When to Exclude Code

Only exclude code when:

1. **Auto-generated** (JPA entities, Lombok, Protocol Buffers)
2. **Infrastructure boilerplate** (Spring configuration, logging setup)
3. **Dead code** (remove it instead of excluding)
4. **Impossible to test** (very rare)

### Exclude via Gradle

```gradle
classDirectories.setFrom(files(classDirectories.files.map { file ->
    fileTree(file) {
        exclude(
            "**/config/**",
            "**/entity/**",
            "**/dto/**",
            "**/*Application.class",
            "**/SpringBootApplication.class",
        )
    }
}))
```

### Exclude via Annotations

```java
// Mark specific methods as excluded
@Generated("Lombok")  // Lombok-generated constructor
public Patient(String id, String name) {
    this.id = id;
    this.name = name;
}

@ExcludeFromJacocoGeneratedReport
public void hibernateInitializer() {
    // Hibernate proxy method - exclude from coverage
}
```

### Don't Exclude

❌ **DON'T** exclude:
- Service methods (test them!)
- Controllers (test endpoints!)
- Business logic (core product)
- Recently added code (should have 90%+ coverage)

---

## Coverage Standards Checklist

### Before Committing Code

- [ ] Run `./gradlew test jacocoTestReport`
- [ ] Open `build/reports/jacoco/test/html/index.html`
- [ ] Verify all new classes have coverage
- [ ] Check for RED lines in coverage report
- [ ] Add tests for any RED lines (untested code)
- [ ] Verify overall coverage >= 80%

### Before Creating PR

- [ ] All test pass: `./gradlew test`
- [ ] Coverage passes thresholds: `./gradlew jacocoTestCoverageVerification`
- [ ] New code has >= 80% coverage
- [ ] No regression in coverage (compare to main branch)
- [ ] Exception handling is tested

### For PR Review

- [ ] Check coverage report in PR comments
- [ ] Verify new code has tests
- [ ] Look for RED lines (untested paths)
- [ ] Request tests for critical logic with < 80% coverage

---

## Common Coverage Metrics

### By Service Type

| Service Type | Typical Coverage | Target |
|--------------|------------------|--------|
| Quality Measure | 82% | 85% |
| Patient Data | 80% | 85% |
| CQL Engine | 84% | 85% |
| FHIR Service | 78% | 80% |
| Care Gap | 81% | 85% |
| Gateway | 76% | 80% |

### By Module Layer

| Layer | Coverage | Target |
|-------|----------|--------|
| Service layer | 85% | 85% |
| Repository layer | 88% | 90% |
| Controller layer | 80% | 80% |
| Utility layer | 83% | 85% |
| Configuration | 62% | 60% |

---

## Troubleshooting Coverage Issues

### Issue 1: JaCoCo Report Not Generated

```bash
# ERROR: build/reports/jacoco/test/html/index.html not found

# FIX:
./gradlew clean test jacocoTestReport --no-build-cache

# Verify Gradle has jacoco plugin:
grep "jacoco" build.gradle.kts
```

### Issue 2: Coverage Lower Than Expected

```bash
# Coverage dropped from 82% to 76% after code change

# Causes:
# 1. New code added without tests
# 2. Large code deletion
# 3. Report generation issue

# Investigate:
git diff main..HEAD build/reports/jacoco/test/html/index.html
# Compare classes to identify what changed

# FIX: Add tests for new code
./gradlew test jacocoTestReport
# Verify coverage >= previous
```

### Issue 3: Verification Fails

```bash
# BUILD FAILED: The following classes do not meet minimum coverage:
# - PatientService: 74% (requires 85%)

# FIX: Add tests to cover missing lines

# Identify missing lines:
# 1. Open build/reports/jacoco/test/html/PatientService.html
# 2. Look for RED lines
# 3. Write tests that execute those lines
# 4. Re-run: ./gradlew jacocoTestCoverageVerification

# Temporary workaround (not recommended):
rule {
    element = "CLASS"
    includes = listOf("*PatientService")
    limit {
        minimum = "0.70"  // ⚠️ Temporary - increase to 0.85 once tests added
    }
}
```

### Issue 4: Too Many Excluded Files

```bash
# Coverage calculation excludes too much code, making target meaningless

# Review exclusions:
grep "exclude(" build.gradle.kts

# Remove overly broad exclusions:
// ❌ DON'T: Excludes entire feature
exclude("**/patient/**")

// ✅ DO: Only exclude auto-generated
exclude("**/entity/**")
exclude("**/dto/**")
```

---

## Coverage Tools & Integration

### Tools Recommended for HDIM

| Tool | Purpose | Cost |
|------|---------|------|
| **JaCoCo** | Local coverage measurement | Free |
| **Codecov** | PR coverage reports | Free tier |
| **SonarQube** | Enterprise coverage tracking | Free Community |
| **PIT** | Mutation testing | Free |

### Set Up Coverage Locally

```bash
# 1. Generate JaCoCo report
./gradlew jacocoTestReport

# 2. Open in browser
# macOS:
open build/reports/jacoco/test/html/index.html

# Linux:
firefox build/reports/jacoco/test/html/index.html

# 3. Drill down into uncovered classes
# Click class → see source code with color coding:
# - Green: Executed
# - Red: Not executed
# - Yellow: Partially covered
```

---

## Summary

**Coverage Best Practices for HDIM:**

✅ **Target 80%+ line coverage** across all services
✅ **Enforce 85%+ for business logic** (services, calculations)
✅ **Test error paths** (exception handling)
✅ **Test boundary conditions** (edge cases)
✅ **Use @ParameterizedTest** for multi-path logic
✅ **Run coverage verification in CI/CD**
✅ **Exclude only auto-generated code**
✅ **Track coverage trends** over time

---

**Last Updated**: January 19, 2026
**Maintained by**: HDIM Platform Team
**Next Step**: Create CI_CD_GUIDE.md (Pipeline documentation)
