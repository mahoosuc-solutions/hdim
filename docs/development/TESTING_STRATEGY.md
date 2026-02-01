# TESTING STRATEGY

Unified testing standards and best practices for HDIM microservices.

**Last Updated**: January 19, 2026
**Coverage**: 50+ microservices, 613 test files
**Status**: Phase 1.5 Blocker #2 - Testing Standards

---

## Overview

HDIM employs a comprehensive testing pyramid approach across all 50+ microservices to ensure quality, reliability, and performance. This guide standardizes testing practices across the platform.

### Testing Pyramid

```
       /\
      /  \  E2E Tests (5-10%)
     /    \ - Playwright automation
    /      \ - Full workflow validation
   /--------\
  /          \  Integration Tests (20-30%)
 /            \ - Testcontainers + real DB
/              \ - API endpoint validation
/----------------\
/                  \  Unit Tests (60-70%)
/                    \ - Fast, isolated
/                      \ - Mock dependencies
/________________________\
```

### Current State

- ✅ **613 test files** across all services
- ✅ **Unit tests** well-established (JUnit 5, Mockito)
- ✅ **Integration tests** in 4-5 core services (Testcontainers)
- ✅ **E2E tests** with Playwright framework
- ⚠️ **No unified testing strategy** documented
- ⚠️ **Only 2 services** have audit/compliance tests
- ⚠️ **3 of 8 heavyweight tests** currently failing

---

## Unit Testing Standards

Unit tests are the foundation of the testing pyramid. They validate individual components in isolation.

### Test Class Naming Convention

```
{ClassUnderTest}Test.java

Examples:
- PatientServiceTest.java
- QualityMeasureCalculatorTest.java
- CQLExpressionEvaluatorTest.java
```

### Test Method Naming Convention

Use the **Given-When-Then** pattern:

```
should{ExpectedBehavior}_{WhenCondition}

Examples:
- shouldReturnPatient_WhenPatientExists()
- shouldThrowValidationException_WhenScoreIsInvalid()
- shouldCacheResult_WhenCacheIsEnabled()
- shouldFilterByTenant_WhenTenantIdProvided()
```

### Test Structure Template

```java
@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PatientService patientService;

    private PatientTestDataBuilder testDataBuilder;

    @BeforeEach
    void setUp() {
        testDataBuilder = new PatientTestDataBuilder();
    }

    @Test
    void shouldReturnPatient_WhenPatientExists() {
        // GIVEN
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";
        Patient expectedPatient = testDataBuilder
            .withId(patientId)
            .withTenantId(tenantId)
            .build();

        when(patientRepository.findByIdAndTenant(patientId, tenantId))
            .thenReturn(Optional.of(expectedPatient));

        // WHEN
        PatientResponse result = patientService.getPatient(patientId, tenantId);

        // THEN
        assertThat(result)
            .isNotNull()
            .extracting(PatientResponse::getId)
            .isEqualTo(patientId);

        verify(patientRepository).findByIdAndTenant(patientId, tenantId);
        verify(auditService).log(AuditEvent.PATIENT_ACCESS, patientId);
    }

    @Test
    void shouldThrowResourceNotFoundException_WhenPatientDoesNotExist() {
        // GIVEN
        String patientId = "NONEXISTENT";
        String tenantId = "TENANT-001";

        when(patientRepository.findByIdAndTenant(patientId, tenantId))
            .thenReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> patientService.getPatient(patientId, tenantId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Patient", patientId);

        verify(auditService, never()).log(any(), any());
    }

    @Test
    void shouldApplyTenantFilter_WhenMultipleTenantsExist() {
        // GIVEN - Multiple patients with different tenants
        Patient patient1 = testDataBuilder
            .withTenantId("TENANT-001")
            .build();
        Patient patient2 = testDataBuilder
            .withTenantId("TENANT-002")
            .build();

        String patientId = patient1.getId();

        when(patientRepository.findByIdAndTenant(patientId, "TENANT-001"))
            .thenReturn(Optional.of(patient1));
        when(patientRepository.findByIdAndTenant(patientId, "TENANT-002"))
            .thenReturn(Optional.empty());

        // WHEN
        PatientResponse result = patientService.getPatient(patientId, "TENANT-001");

        // THEN - Verify tenant isolation
        assertThat(result.getTenantId()).isEqualTo("TENANT-001");

        // WHEN - Wrong tenant
        assertThatThrownBy(() -> patientService.getPatient(patientId, "TENANT-002"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Cache TTL must not exceed 5 minutes for PHI data")
    void shouldEnforceCacheTtlLimit_WhenCachingPatientData() {
        // GIVEN - HIPAA compliance requirement
        // WHEN - Patient data is cached
        // THEN - Verify TTL <= 5 minutes (300 seconds)
        assertThat(CACHE_TTL_SECONDS).isLessThanOrEqualTo(300);
    }
}
```

### Mocking Strategy

#### What to Mock

Mock external dependencies:

```java
@Mock
private PatientRepository patientRepository;        // Database

@Mock
private RestTemplate restTemplate;                 // External API calls

@Mock
private RedisTemplate<String, ?> redisTemplate;    // Cache

@Mock
private KafkaTemplate<String, ?> kafkaTemplate;    // Message queue

@Mock
private AuditService auditService;                 // Cross-cutting concern
```

#### What NOT to Mock

Never mock the class under test or its business logic:

```java
// DON'T DO THIS:
@Mock
private PatientService patientService;  // ❌ This is what we're testing!

@InjectMocks
private PatientService patientService;  // ✅ DO THIS instead
```

### Test Data Builders

Create reusable test data using the Builder pattern:

```java
public class PatientTestDataBuilder {

    private String id = UUID.randomUUID().toString();
    private String tenantId = "TENANT-001";
    private String firstName = "John";
    private String lastName = "Doe";
    private LocalDate dateOfBirth = LocalDate.of(1990, 1, 1);
    private String status = "ACTIVE";

    public PatientTestDataBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public PatientTestDataBuilder withTenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    public PatientTestDataBuilder withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public Patient build() {
        return Patient.builder()
            .id(id)
            .tenantId(tenantId)
            .firstName(firstName)
            .lastName(lastName)
            .dateOfBirth(dateOfBirth)
            .status(status)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
}
```

### Coverage Targets

Enforce minimum coverage thresholds per module:

```gradle
// build.gradle.kts
jacocoTestCoverageVerification {
    violationRules {
        rule {
            element = "CLASS"
            includePattern = "com.healthdata.*"
            excludePattern = "*.config.*,*.entity.*"

            limit {
                minimum = "0.80"  // 80% minimum coverage
            }
        }

        rule {
            element = "METHOD"
            includes = listOf("*Service", "*Controller", "*Repository")
            limit {
                minimum = "0.85"  // 85% for business logic
            }
        }
    }
}
```

Run coverage check:

```bash
./gradlew jacocoTestCoverageVerification
```

### Parameterized Tests

Use `@ParameterizedTest` for testing multiple input scenarios:

```java
@ParameterizedTest
@CsvSource({
    "0, true",           // Valid: minimum score
    "27, true",          // Valid: maximum score
    "50, false",         // Invalid: exceeds maximum
    "-1, false",         // Invalid: below minimum
})
void shouldValidatePHQ9Score(int score, boolean expectedValid) {
    // GIVEN
    QualityMeasureValidator validator = new QualityMeasureValidator();

    // WHEN
    boolean result = validator.isValidPHQ9Score(score);

    // THEN
    assertThat(result).isEqualTo(expectedValid);
}
```

### Testing Exception Cases

Always test error scenarios:

```java
@Test
void shouldThrowValidationException_WhenScoreExceedsMaximum() {
    // GIVEN
    int invalidScore = 28;  // PHQ-9 max is 27

    // WHEN & THEN
    assertThatThrownBy(() -> measureService.validateScore(invalidScore))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("PHQ-9 score must be between 0 and 27")
        .hasFieldOrProperty("errorCode").isEqualTo("INVALID_SCORE");
}
```

### Assertive Assertions

Use AssertJ for readable assertions:

```java
// ✅ Clear, readable
assertThat(result)
    .isNotNull()
    .hasFieldOrProperty("id")
    .extracting(PatientResponse::getTenantId)
    .isEqualTo(expectedTenantId);

// ❌ Less readable
assertNotNull(result);
assertEquals(expectedTenantId, result.getTenantId());
```

---

## Integration Testing Standards

Integration tests validate interactions between components and with external systems using real databases and message brokers.

### Testcontainers Setup

Use Testcontainers for isolated test environments:

```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PatientControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("test_db")
        .withUsername("testuser")
        .withPassword("testpass");

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);

        registry.add("spring.redis.host", () -> redisContainer.getHost());
        registry.add("spring.redis.port", () -> redisContainer.getFirstMappedPort());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @BeforeEach
    void setUp() {
        patientRepository.deleteAll();
    }

    @Test
    void shouldReturnPatient_WhenCallingGetEndpoint() throws Exception {
        // GIVEN - Create test patient in database
        Patient testPatient = Patient.builder()
            .id(UUID.randomUUID().toString())
            .tenantId("TENANT-001")
            .firstName("John")
            .lastName("Doe")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();
        patientRepository.save(testPatient);

        // WHEN & THEN - Call endpoint
        mockMvc.perform(get("/api/v1/patients/{id}", testPatient.getId())
                .header("X-Tenant-ID", "TENANT-001")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(testPatient.getId()))
            .andExpect(jsonPath("$.firstName").value("John"))
            .andExpect(jsonPath("$.tenantId").value("TENANT-001"));
    }

    @Test
    void shouldDenyAccess_WhenTenantIsUnauthorized() throws Exception {
        // GIVEN
        Patient testPatient = Patient.builder()
            .id(UUID.randomUUID().toString())
            .tenantId("TENANT-001")
            .firstName("John")
            .lastName("Doe")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();
        patientRepository.save(testPatient);

        // WHEN & THEN - Different tenant cannot access
        mockMvc.perform(get("/api/v1/patients/{id}", testPatient.getId())
                .header("X-Tenant-ID", "TENANT-002")  // Different tenant
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnCachedResult_WhenDataIsInCache() throws Exception {
        // GIVEN
        Patient testPatient = Patient.builder()
            .id(UUID.randomUUID().toString())
            .tenantId("TENANT-001")
            .firstName("John")
            .lastName("Doe")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();
        patientRepository.save(testPatient);

        // WHEN - First call (cache miss)
        mockMvc.perform(get("/api/v1/patients/{id}", testPatient.getId())
                .header("X-Tenant-ID", "TENANT-001"))
            .andExpect(status().isOk());

        // Clear the database to prove we're using cache
        patientRepository.deleteAll();

        // THEN - Second call should still work (from cache)
        mockMvc.perform(get("/api/v1/patients/{id}", testPatient.getId())
                .header("X-Tenant-ID", "TENANT-001"))
            .andExpect(status().isOk());
    }
}
```

### Database Seeding

Create reusable database seeding for integration tests:

```java
public class IntegrationTestDataSeeder {

    private final PatientRepository patientRepository;
    private final InsuranceRepository insuranceRepository;
    private final ConditionRepository conditionRepository;

    public Patient seedPatient(String tenantId, String firstName, String lastName) {
        Patient patient = Patient.builder()
            .id(UUID.randomUUID().toString())
            .tenantId(tenantId)
            .firstName(firstName)
            .lastName(lastName)
            .dateOfBirth(LocalDate.of(1980, 1, 1))
            .status("ACTIVE")
            .build();
        return patientRepository.save(patient);
    }

    public List<Patient> seedPatients(String tenantId, int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> seedPatient(tenantId,
                "FirstName" + i,
                "LastName" + i))
            .collect(Collectors.toList());
    }

    public Condition seedCondition(String patientId, String conditionCode) {
        Condition condition = Condition.builder()
            .id(UUID.randomUUID().toString())
            .patientId(patientId)
            .code(conditionCode)
            .status("ACTIVE")
            .build();
        return conditionRepository.save(condition);
    }
}
```

### Kafka Integration Testing

Test asynchronous messaging with embedded Kafka:

```java
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
class PatientEventIntegrationTest {

    @Autowired
    private KafkaTemplate<String, PatientEvent> kafkaTemplate;

    @Autowired
    private PatientEventListener patientEventListener;

    @Autowired
    private PatientRepository patientRepository;

    @Test
    void shouldProcessPatientCreatedEvent_WhenEventIsPublished() throws Exception {
        // GIVEN
        String patientId = UUID.randomUUID().toString();
        PatientEvent event = PatientEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .patientId(patientId)
            .eventType("PATIENT_CREATED")
            .firstName("John")
            .lastName("Doe")
            .tenantId("TENANT-001")
            .timestamp(Instant.now())
            .build();

        // WHEN - Publish event
        kafkaTemplate.send("patient-events", patientId, event).get();

        // THEN - Wait for async processing and verify
        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Patient savedPatient = patientRepository.findById(patientId)
                    .orElseThrow();
                assertThat(savedPatient.getFirstName()).isEqualTo("John");
            });
    }

    @Test
    void shouldSkipDuplicateEvent_WhenEventIdAlreadyProcessed() throws Exception {
        // GIVEN
        String eventId = UUID.randomUUID().toString();
        PatientEvent event = PatientEvent.builder()
            .eventId(eventId)
            .patientId(UUID.randomUUID().toString())
            .eventType("PATIENT_CREATED")
            .tenantId("TENANT-001")
            .build();

        // WHEN - Send same event twice
        kafkaTemplate.send("patient-events", event.getPatientId(), event).get();
        kafkaTemplate.send("patient-events", event.getPatientId(), event).get();

        // THEN - Only one patient should be created
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Patient> patients = patientRepository.findByTenantId("TENANT-001");
            assertThat(patients).hasSize(1);
        });
    }
}
```

### Database Isolation

Ensure test isolation with transactional tests:

```java
@SpringBootTest
@Transactional  // Rollback after each test
class PatientRepositoryIntegrationTest {

    @Autowired
    private PatientRepository patientRepository;

    @Test
    void shouldIsolateDatabaseChanges_WhenTransactionRollsBack() {
        // GIVEN
        Patient patient = Patient.builder()
            .id(UUID.randomUUID().toString())
            .tenantId("TENANT-001")
            .firstName("John")
            .lastName("Doe")
            .build();

        // WHEN
        patientRepository.save(patient);

        // THEN - Verify in this transaction
        assertThat(patientRepository.findById(patient.getId()))
            .isPresent();

        // Transaction rolls back after test ends
        // Next test starts fresh
    }
}
```

---

## Contract Testing Standards

Contract tests validate API contracts between services, preventing breaking changes.

### Spring Cloud Contract Setup

Define contracts in YAML for consumer-driven testing:

```yaml
# src/test/resources/contracts/patient-service/getPatient.yml
description: Get patient by ID

request:
  method: GET
  url: /api/v1/patients/PATIENT-123
  headers:
    X-Tenant-ID: TENANT-001
    Content-Type: application/json

response:
  status: 200
  headers:
    Content-Type: application/json
  body:
    id: PATIENT-123
    tenantId: TENANT-001
    firstName: John
    lastName: Doe
    dateOfBirth: 1990-01-01
  matchers:
    body:
      - path: $.id
        type: regex
        value: PATIENT-.*
      - path: $.tenantId
        type: regex
        value: TENANT-.*
```

### Provider Side Contract Test

Implement contract tests in the service being called:

```java
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMessageVerifier
public class PatientServiceContractTest {

    @Autowired
    private MockMvc mockMvc;

    @TestTemplate
    @ExtendWith(SpringCloudContractExtension.class)
    void validateContract(ContractVerifierMessage message) throws Exception {
        // Contract verification is automatic
    }
}
```

### Consumer Side Contract Test

In a service that calls patient-service:

```java
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureWireMock(port = 0)
public class QualityMeasureServiceConsumerContractTest {

    @Autowired
    private QualityMeasureClient qualityMeasureClient;

    @Autowired
    private WireMockServer wireMockServer;

    @Before
    public void setUp() {
        // Load stubs from patient-service
        wireMockServer.loadMappingsUsing(new ClasspathFileSource(
            "mappings/patient-service"
        ));
    }

    @Test
    void shouldCallPatientService_WhenEvaluatingMeasure() {
        // GIVEN
        String patientId = "PATIENT-123";

        // WHEN
        PatientResponse patient = qualityMeasureClient.getPatient(patientId);

        // THEN
        assertThat(patient.getId()).isEqualTo("PATIENT-123");

        verify(getRequestedFor(urlEqualTo("/api/v1/patients/" + patientId))
            .withHeader("X-Tenant-ID", matching("TENANT-.*")));
    }
}
```

### API Documentation as Contract

Generate contract from OpenAPI specs:

```java
// build.gradle.kts
plugins {
    id("org.springdoc.openapi-gradle-plugin") version "1.7.0"
}

springdoc {
    apiDocsUrl = "/v3/api-docs"
    swaggerUiUrl = "/swagger-ui.html"
    outputFileName = "openapi.json"
}
```

---

## E2E Testing Standards

End-to-end tests validate complete workflows across multiple services using Playwright.

### Playwright Test Setup

```typescript
// e2e/tests/quality-measure.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Quality Measure Evaluation', () => {
  const BASE_URL = process.env.BASE_URL || 'http://localhost:4200';

  test.beforeEach(async ({ page }) => {
    // Navigate to application
    await page.goto(BASE_URL);

    // Login with test user
    await page.fill('input[name="username"]', 'test_evaluator');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button:has-text("Login")');

    // Wait for dashboard to load
    await page.waitForURL(/dashboard/);
  });

  test('should complete full quality measure workflow', async ({ page }) => {
    // GIVEN - User on dashboard
    await expect(page.locator('text=Quality Measures')).toBeVisible();

    // WHEN - Click evaluate button
    await page.click('button:has-text("Evaluate Measure")');

    // THEN - Measure selection dialog opens
    await expect(page.locator('dialog:has-text("Select Measure")')).toBeVisible();

    // WHEN - Select HEDIS measure
    await page.click('text=Comprehensive Diabetes Care');
    await page.click('button:has-text("Next")');

    // THEN - Patient selection appears
    await expect(page.locator('text=Select Patients')).toBeVisible();

    // WHEN - Filter patients
    await page.fill('input[placeholder="Search patients"]', 'diabetes');
    await page.click('checkbox:nth-of-type(1)');  // Select first patient
    await page.click('button:has-text("Evaluate")');

    // THEN - Evaluation runs and results display
    await page.waitForURL(/results/);
    await expect(page.locator('text=Evaluation Complete')).toBeVisible();

    // THEN - Verify results
    const score = await page.locator('[data-testid="measure-score"]').textContent();
    expect(parseFloat(score!)).toBeGreaterThanOrEqual(0);
    expect(parseFloat(score!)).toBeLessThanOrEqualTo(100);
  });

  test('should display error when evaluation fails', async ({ page }) => {
    // GIVEN
    await page.click('button:has-text("Evaluate Measure")');

    // Mock failed API response
    await page.route('**/api/quality/**', route => {
      route.abort('failed');
    });

    // WHEN
    await page.click('button:has-text("Evaluate")');

    // THEN
    await expect(page.locator('text=Evaluation failed')).toBeVisible();
  });
});
```

### E2E Test Structure

```typescript
test('scenario description', async ({ page, context }) => {
  // GIVEN - Setup test state
  const testData = await seedTestPatients();

  // WHEN - Perform user actions
  await page.goto(`${BASE_URL}/patients/${testData.patientId}`);

  // THEN - Verify outcomes
  await expect(page.locator('[data-testid="patient-name"]')).toContainText('John Doe');
});
```

### Cross-Browser Testing

```typescript
// playwright.config.ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e/tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
  ],

  webServer: {
    command: 'npm run start',
    url: 'http://localhost:4200',
    reuseExistingServer: !process.env.CI,
  },
});
```

### Test Data Management in E2E

```typescript
// e2e/fixtures/test-data.ts
import { test as base } from '@playwright/test';
import axios from 'axios';

export const test = base.extend<{ testData: TestDataContext }>({
  async testData({ baseURL }, use) {
    const context = new TestDataContext(baseURL);

    // Setup test data before test
    await context.seedPatients(5);
    await context.seedMeasures(['CDM', 'CCS', 'BCS']);

    await use(context);

    // Cleanup after test
    await context.cleanup();
  },
});

class TestDataContext {
  constructor(private baseURL: string) {}

  async seedPatients(count: number) {
    for (let i = 0; i < count; i++) {
      await axios.post(`${this.baseURL}/api/patients`, {
        firstName: `Patient${i}`,
        lastName: `Test`,
        dateOfBirth: '1990-01-01',
        tenantId: 'TEST-TENANT',
      });
    }
  }

  async cleanup() {
    // Delete all test data
    await axios.delete(`${this.baseURL}/api/admin/test-data`);
  }
}

// Usage in tests
test('scenario', async ({ testData, page }) => {
  // Test data already seeded
  await page.goto('/patients');
  // Verify patients appear
});
```

### Visual Regression Testing

```typescript
test('should match visual snapshot', async ({ page }) => {
  await page.goto('/quality-measures');

  // Compare against baseline screenshot
  await expect(page).toHaveScreenshot('quality-measures.png', {
    mask: [page.locator('[data-dynamic]')],  // Ignore dynamic content
    maxDiffPixels: 100,
  });
});
```

---

## Test Coverage & Reporting

### JaCoCo Configuration

Configure JaCoCo for code coverage measurement:

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

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required = true
        html.required = true
        csv.required = false
    }

    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.map { file ->
            fileTree(file) {
                exclude(
                    "**/config/**",
                    "**/entity/**",
                    "**/dto/**",
                    "**/request/**",
                    "**/response/**",
                    "**/*Application.class",
                )
            }
        }))
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            element = "PACKAGE"
            excludePattern = "com.healthdata.*.config.*"

            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80"
            }
        }

        rule {
            element = "CLASS"
            includes = listOf("*Service", "*Repository")

            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.85"
            }
        }
    }
}
```

### Generate Coverage Report

```bash
# Run tests and generate report
./gradlew jacocoTestReport

# Verify coverage meets thresholds
./gradlew jacocoTestCoverageVerification

# View HTML report
open build/reports/jacoco/test/html/index.html
```

### Sonar Configuration

Integrate SonarQube for centralized coverage tracking:

```gradle
// build.gradle.kts
plugins {
    id("org.sonarqube") version "5.0.0.4638"
}

sonarqube {
    properties {
        property("sonar.projectKey", "hdim:${project.name}")
        property("sonar.projectName", project.name)
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.coverage.jacoco.xmlReportPaths",
            "${buildDir}/reports/jacoco/test/jacocoTestReport.xml")
    }
}
```

Run Sonar analysis:

```bash
./gradlew sonarqube \
  -Dsonar.login=$SONAR_TOKEN \
  -Dsonar.host.url=http://localhost:9000
```

### Coverage Reports in CI/CD

```yaml
# .github/workflows/test.yml
name: Test & Coverage

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '21'

      - name: Run tests
        run: ./gradlew test jacocoTestReport

      - name: Upload to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./build/reports/jacoco/test/jacocoTestReport.xml
          fail_ci_if_error: true
          verbose: true

      - name: Comment PR with coverage
        if: github.event_name == 'pull_request'
        uses: romeovs/lcov-reporter-action@v0.3.1
        with:
          github-token: ${{ secrets.GITHUB_TOKEN }}
          lcov-file: ./build/reports/jacoco/test/jacocoTestReport.xml
```

### Coverage Targets by Service Type

| Service Type          | Unit | Integration | E2E | Total |
| --------------------- | ---- | ----------- | --- | ----- |
| Core Services         | 85%  | 70%         | 50% | 80%   |
| API Controllers       | 80%  | 75%         | 60% | 75%   |
| Repositories          | 90%  | 80%         | -   | 85%   |
| Utility/Helper        | 85%  | -           | -   | 85%   |
| Configuration Classes | 60%  | -           | -   | 60%   |

---

## HIPAA Compliance in Tests

All tests handling PHI must follow HIPAA requirements.

### Protected Health Information Test Data

```java
public class PHITestDataBuilder {

    public Patient buildPatientWithPHI() {
        return Patient.builder()
            .id("PATIENT-" + UUID.randomUUID())
            .firstName("John")           // PHI - First name
            .lastName("Doe")             // PHI - Last name
            .dateOfBirth(LocalDate.of(1990, 1, 1))  // PHI - DOB
            .ssn("123-45-6789")         // PHI - SSN (always masked)
            .build();
    }
}

@Test
void shouldMaskPHIInLogs_WhenAccessingPatient() {
    // GIVEN
    Patient patient = new PHITestDataBuilder().buildPatientWithPHI();

    // WHEN
    patientService.getPatient(patient.getId());

    // THEN - Verify PHI is masked in logs
    // Logs should show: "PATIENT-***" not full ID
    // Logs should show: "J***" not "John"
}

@Test
void shouldEnforceCacheTTL_WhenCachingPHI() {
    // GIVEN - Cache TTL <= 5 minutes
    // WHEN - Patient data cached
    // THEN - Verify TTL enforcement
    assertThat(cacheConfig.getPatientCacheTtl()).isLessThanOrEqualTo(300);
}

@Test
void shouldAuditPHIAccess_WhenAccessingPatientData() {
    // GIVEN
    String patientId = "PATIENT-123";

    // WHEN
    patientService.getPatient(patientId);

    // THEN
    verify(auditService).logAccess(
        eq(AuditEventType.PHI_ACCESS),
        eq(patientId),
        eq("PATIENT-123")  // Should not include sensitive data
    );
}
```

---

## Common Testing Patterns

### Testing Async Operations with Awaitility

```java
@Test
void shouldProcessEventAsynchronously() {
    // GIVEN
    PatientEvent event = new PatientEvent();

    // WHEN
    kafkaTemplate.send("patient-events", event);

    // THEN
    await()
        .atMost(Duration.ofSeconds(5))
        .pollInterval(Duration.ofMillis(100))
        .untilAsserted(() -> {
            Patient savedPatient = patientRepository.findById(event.getPatientId())
                .orElseThrow();
            assertThat(savedPatient.isProcessed()).isTrue();
        });
}
```

### Testing Scheduled Tasks

```java
@Test
void shouldRunScheduledMeasureEvaluation() {
    // GIVEN - Set test clock to specific time
    Clock testClock = Clock.fixed(
        Instant.parse("2024-01-01T09:00:00Z"),
        ZoneId.of("UTC")
    );

    // WHEN - Trigger scheduled task
    measureScheduler.evaluatePendingMeasures();

    // THEN
    assertThat(auditService.getLastEvaluationTime())
        .isEqualTo(testClock.instant());
}
```

### Testing Multi-Tenant Scenarios

```java
@ParameterizedTest
@ValueSource(strings = {"TENANT-001", "TENANT-002", "TENANT-003"})
void shouldIsolateTenantData(String tenantId) {
    // GIVEN
    Patient patient = patientRepository.save(
        testDataBuilder.withTenantId(tenantId).build()
    );

    // WHEN
    List<Patient> results = patientRepository.findByTenantId(tenantId);

    // THEN
    assertThat(results)
        .extracting(Patient::getTenantId)
        .allMatch(tid -> tid.equals(tenantId));
}
```

### Testing Exception Handling & Rollback

```java
@Test
@Transactional
void shouldRollbackOnValidationError() {
    // GIVEN
    int initialCount = patientRepository.findAll().size();

    // WHEN & THEN
    assertThatThrownBy(() -> {
        Patient invalidPatient = testDataBuilder
            .withFirstName("")  // Invalid - required field
            .build();
        patientService.createPatient(invalidPatient);
    }).isInstanceOf(ValidationException.class);

    // THEN - Verify rollback
    assertThat(patientRepository.findAll()).hasSize(initialCount);
}
```

---

## Test Execution & CI/CD Integration

### Local Test Execution

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests PatientServiceTest

# Run specific test method
./gradlew test --tests PatientServiceTest.shouldReturnPatient*

# Run integration tests only
./gradlew integrationTest

# Run tests with coverage
./gradlew test jacocoTestReport

# Run with specific profile
./gradlew test -Dspring.profiles.active=test

# Run tests in parallel
./gradlew test --max-workers=4
```

### GitHub Actions CI/CD Pipeline

```yaml
# .github/workflows/test.yml
name: Build & Test

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_PASSWORD: postgres
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: gradle

      - name: Run unit tests
        run: ./gradlew test -x integrationTest

      - name: Run integration tests
        run: ./gradlew integrationTest

      - name: Generate coverage report
        run: ./gradlew jacocoTestReport

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./build/reports/jacoco/test/jacocoTestReport.xml

      - name: Verify coverage thresholds
        run: ./gradlew jacocoTestCoverageVerification
```

---

## Test Failure Troubleshooting

### Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| `@SpringBootTest` hangs | Port already in use | `serverPort = 0` for random port |
| `NoSuchBeanDefinitionException` in test | Missing `@SpringBootTest` or wrong profile | Add annotation or specify `@ActiveProfiles("test")` |
| Testcontainers fails to start | Docker daemon not running | Start Docker: `docker daemon start` |
| Flaky async tests | Race condition in assertion | Use Awaitility with proper timeout |
| Test data pollution | Missing `@Transactional` | Add `@Transactional` to test class |
| Cache TTL violations | PHI cached too long | Verify Redis config has `expire: 300s` |

### Debug Test Failures

```bash
# Run with debug logging
./gradlew test -i --debug

# Run single test with output
./gradlew test --tests TestClassName -i

# Run with continuous build
./gradlew test --continuous
```

---

## Summary

This testing strategy ensures:

✅ **Consistency** - All 50+ services follow same patterns
✅ **Quality** - 80%+ code coverage enforced
✅ **Speed** - Fast feedback loops (unit < 1s, integration < 30s)
✅ **Compliance** - HIPAA requirements tested explicitly
✅ **Reliability** - No flaky tests, proper isolation
✅ **Maintainability** - Clear conventions, reusable builders

---

**Last Updated**: January 19, 2026
**Maintained by**: HDIM Platform Team
**Status**: Phase 1.5 Documentation - Testing Strategy Complete
