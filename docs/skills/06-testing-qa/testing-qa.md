# Testing & QA - Comprehensive Test Strategy Guide

> **This is a comprehensive guide for testing HDIM microservices.**
> **Testing is not optional: all public methods require unit tests; all endpoints require integration tests.**

---

## Overview

### What is This Skill?

Testing is a systematic approach to verifying that software works correctly. HDIM uses three test types:

1. **Unit Tests:** Test single service/method in isolation (mocked dependencies)
2. **Integration Tests:** Test multiple layers together (real database, HTTP)
3. **Entity-Migration Validation Tests:** Test entity definitions match Liquibase schema

Together, these tests catch bugs early (at development time) and prevent regressions.

**Example:** PatientService.createPatient() is tested three ways:
- Unit test: Mock PatientRepository, verify service calls it correctly
- Integration test: Real database, verify patient persisted and event published
- Schema validation: Verify entity annotations match Liquibase migration

### Why is This Important for HDIM?

Healthcare systems must be reliable. A single bug might:
- Expose Protected Health Information (cross-tenant data leak)
- Lose patient data (missing field in migration)
- Crash production service (unhandled exception)
- Violate HIPAA (audit trail missing)

Comprehensive testing prevents these. HDIM requires:
- Unit test coverage on all service methods
- Integration test coverage on all API endpoints
- Entity-migration validation on all schema changes
- Multi-tenant isolation testing
- Authorization testing (role-based access)

### Business Impact

- **Reliability:** Catch bugs before production (90%+ issues found in testing)
- **Compliance:** HIPAA requires auditable testing and validation (tests = audit trail)
- **Confidence:** Developers deploy with confidence; fewer production hotfixes
- **Refactoring Safety:** Comprehensive tests enable safe refactoring (no regressions)

### Key Services Using This Skill

All 51 HDIM services:
- patient-event-service: 500+ tests
- quality-measure-event-service: 400+ tests
- care-gap-event-service: 350+ tests
- Every service follows same test patterns

### Estimated Learning Time

2 weeks (hands-on test writing required)

---

## Key Concepts

### Concept 1: Test Pyramid (Unit → Integration → E2E)

**Definition:** Test pyramid shows optimal distribution of tests:
- **Bottom (60%):** Unit tests (fast, isolated, many)
- **Middle (30%):** Integration tests (slower, realistic, moderate)
- **Top (10%):** End-to-end tests (slow, comprehensive, few)

**Why it matters:** Unit tests are fast and cheap to write. Integration tests are slower but more comprehensive. E2E tests are slow and expensive. Balance needed.

**Real-world example:**
```
                  E2E Tests (10%)
                /              \
              /                  \
            /                      \
          Integration Tests (30%)
        /                          \
      /                              \
    /                                  \
  Unit Tests (60%)
/                                        \
```

Each test type catches different bugs:
- **Unit tests:** Logic bugs in individual methods
- **Integration tests:** Interaction bugs between layers (service→repo→database)
- **E2E tests:** End-to-end workflow bugs

### Concept 2: AAA Pattern (Arrange-Act-Assert)

**Definition:** Every test follows three phases:
1. **Arrange:** Set up test data and mocks
2. **Act:** Execute the code being tested
3. **Assert:** Verify the result matches expectations

**Why it matters:** Consistent pattern makes tests readable and maintainable. Readers immediately understand test structure.

**Real-world example:**
```java
@Test
void shouldCreatePatient_WhenValidRequest() {
    // ARRANGE: Set up test data
    CreatePatientRequest request = CreatePatientRequest.builder()
        .firstName("John")
        .lastName("Doe")
        .dateOfBirth(LocalDate.of(1990, 1, 1))
        .build();
    when(patientRepository.save(any(Patient.class)))
        .thenReturn(Patient.builder().id(UUID.randomUUID()).build());

    // ACT: Execute code being tested
    PatientResponse result = patientService.createPatient(request);

    // ASSERT: Verify result
    assertThat(result).isNotNull();
    assertThat(result.getFirstName()).isEqualTo("John");
    verify(patientRepository, times(1)).save(any(Patient.class));
}
```

### Concept 3: Mocking with Mockito

**Definition:** Mockito is a framework for creating mock objects. Mocks simulate dependencies (e.g., mock PatientRepository) so unit tests run in isolation.

**Why it matters:** Mocks enable unit tests without requiring real database, HTTP, Kafka, etc. Unit tests run in milliseconds instead of seconds.

**Real-world example:**
```java
@ExtendWith(MockitoExtension.class)
class PatientServiceTest {
    @Mock
    private PatientRepository mockRepository;  // Mock dependency

    @InjectMocks
    private PatientService service;  // Inject mock into service

    @Test
    void shouldCallRepository() {
        // Mock repository to return test patient
        when(mockRepository.findByIdAndTenantId("123", "tenant1"))
            .thenReturn(Optional.of(testPatient));

        // Call service
        PatientResponse result = service.getPatient("123");

        // Verify mock was called with correct arguments
        verify(mockRepository).findByIdAndTenantId("123", "tenant1");
    }
}
```

### Concept 4: Integration Testing with MockMvc

**Definition:** MockMvc is a Spring testing framework for testing REST controllers without starting full HTTP server. Tests are faster than hitting real server but test HTTP layer.

**Why it matters:** Integration tests verify entire request-response cycle (controller→service→repository→database→JSON response).

**Real-world example:**
```java
@SpringBootTest
@AutoConfigureMockMvc
class PatientControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreatePatient_AndPersistToDatabase() throws Exception {
        CreatePatientRequest request = CreatePatientRequest.builder()
            .firstName("John")
            .lastName("Doe")
            .build();

        mockMvc.perform(post("/api/v1/patients")
                .header("X-Tenant-ID", "anthem")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.firstName").value("John"));

        // Verify persisted to database
        List<Patient> patients = patientRepository.findAll();
        assertThat(patients).hasSize(1);
    }
}
```

### Concept 5: Test Data Builders

**Definition:** Test data builders are utility classes that simplify creating test entities. Instead of manually setting every field, builder has sensible defaults.

**Why it matters:** Test data setup is verbose and error-prone. Builders reduce boilerplate and make test intent clear.

**Real-world example:**
```java
// ❌ Without builder: Verbose, easy to miss fields
Patient patient = new Patient();
patient.setId(UUID.randomUUID());
patient.setTenantId("anthem");
patient.setFirstName("John");
patient.setLastName("Doe");
patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
patient.setCreatedAt(Instant.now());

// ✅ With builder: Clear, concise, defaults for missing fields
Patient patient = PatientTestDataBuilder.aPatient()
    .withFirstName("John")
    .withLastName("Doe")
    .build();

// Builder class:
public class PatientTestDataBuilder {
    private String tenantId = "anthem";  // Sensible default
    private String firstName = "John";
    private String lastName = "Doe";

    public PatientTestDataBuilder withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public Patient build() {
        return Patient.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .firstName(firstName)
            .lastName(lastName)
            .build();
    }
}
```

---

## Architecture Pattern

### Testing Strategy

HDIM uses layered testing approach:

```
┌─────────────────────────────────────────────────┐
│ E2E Tests (10%): Full flow client→service→db    │
│ Examples: Postman, Selenium, API client tests   │
│ Speed: Slow (seconds)                           │
│ Coverage: End-to-end workflows                  │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│ Integration Tests (30%): Controllers+Services    │
│ Examples: @SpringBootTest, MockMvc, @DataJpa    │
│ Speed: Medium (100ms-1s per test)               │
│ Coverage: Layer interactions, database I/O       │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│ Unit Tests (60%): Services+Repositories          │
│ Examples: @ExtendWith(MockitoExtension.class)   │
│ Speed: Fast (1-10ms per test)                   │
│ Coverage: Business logic, error handling        │
└─────────────────────────────────────────────────┘
```

### Test Execution Flow

```
Developer writes code
↓
Run local tests: ./gradlew test
├─ Unit tests (60%)
├─ Integration tests (30%)
└─ Entity-migration validation (10%)
↓
All tests pass locally?
├─ YES: git commit
└─ NO: Fix code, re-run tests
↓
Push to GitHub
↓
GitHub Actions CI runs tests
├─ Same test suite as local
├─ Full environment (PostgreSQL, Kafka, Redis)
└─ Tests must pass for PR merge
↓
Tests pass in CI?
├─ YES: PR approved, merge to main
└─ NO: Fix code, push again
↓
Production deployment uses tested code
```

### Design Decisions

**Decision 1: Why mock dependencies in unit tests?**
- **Trade-off:** Mocking is more setup but tests are fast and isolated. Real dependencies are slow but more realistic.
- **Rationale:** 60% of tests should be unit tests (fast feedback). Real dependencies used in 30% integration tests (realistic verification).
- **Alternative:** All tests use real dependencies (too slow for development feedback).

**Decision 2: Why separate unit and integration tests?**
- **Trade-off:** More test files to maintain but clear distinction of what's being tested. Single test suite would be simpler but less clear.
- **Rationale:** Developers run unit tests frequently (< 5 seconds); integration tests less frequently (slow). Separation enables fast feedback.
- **Alternative:** Single test suite (slower feedback, harder to debug).

**Decision 3: Why test entity-migration synchronization?**
- **Trade-off:** Extra validation adds test time but catches schema mismatches early. Skip it and risk production failures.
- **Rationale:** Schema drift (entity != database) is a silent killer. Test time cost is worth the reliability gain.
- **Alternative:** Skip validation, catch bugs in production (unacceptable).

### Trade-offs

| Aspect | Pro | Con |
|--------|-----|-----|
| **Mocking** | Fast tests, isolated, easy to run | Less realistic, requires setup |
| **Real Dependencies** | Realistic, catches integration bugs | Slow, hard to debug, flaky |
| **Unit Tests** | Fast feedback, easy to debug | Don't catch integration bugs |
| **Integration Tests** | Catches integration bugs, realistic | Slow, harder to debug |
| **Comprehensive Tests** | High confidence, catches bugs | Takes time to write and maintain |

---

## Implementation Guide

### Step 1: Set Up Test Dependencies

Add test dependencies to `build.gradle.kts`:

```kotlin
dependencies {
    // Test frameworks
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")

    // Mocking
    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.2.0")

    // Spring Boot testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // MockMvc and integration testing
    testImplementation("org.springframework:spring-test")
    testImplementation("org.springframework.security:spring-security-test")

    // Assertions
    testImplementation("org.assertj:assertj-core:3.24.1")
}

tasks.test {
    useJUnitPlatform()  // Enable JUnit 5
}
```

### Step 2: Create Unit Test for Service

```java
// File: PatientServiceTest.java
@ExtendWith(MockitoExtension.class)
class PatientServiceTest {
    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PatientEventPublisher eventPublisher;

    @InjectMocks
    private PatientService service;

    private static final String TENANT_ID = "anthem";

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ✅ Happy path: patient exists
    @Test
    void shouldReturnPatient_WhenExists() {
        // ARRANGE
        UUID patientId = UUID.randomUUID();
        Patient patient = PatientTestDataBuilder.aPatient()
            .withId(patientId)
            .withFirstName("John")
            .build();

        when(patientRepository.findByIdAndTenantId(patientId, TENANT_ID))
            .thenReturn(Optional.of(patient));

        // ACT
        PatientResponse result = service.getPatient(patientId.toString());

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(patientId);
        assertThat(result.getFirstName()).isEqualTo("John");

        // Verify mock called
        verify(patientRepository).findByIdAndTenantId(patientId, TENANT_ID);
    }

    // ❌ Error case: patient not found
    @Test
    void shouldThrowNotFound_WhenPatientDoesNotExist() {
        // ARRANGE
        UUID patientId = UUID.randomUUID();

        when(patientRepository.findByIdAndTenantId(patientId, TENANT_ID))
            .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> service.getPatient(patientId.toString()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Patient not found: " + patientId);

        verify(patientRepository).findByIdAndTenantId(patientId, TENANT_ID);
    }

    // Transaction test: create patient and publish event
    @Test
    void shouldCreatePatient_AndPublishEvent() {
        // ARRANGE
        CreatePatientRequest request = CreatePatientRequest.builder()
            .firstName("Jane")
            .lastName("Smith")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();

        Patient savedPatient = PatientTestDataBuilder.aPatient()
            .withFirstName("Jane")
            .withLastName("Smith")
            .build();

        when(patientRepository.save(any(Patient.class)))
            .thenReturn(savedPatient);

        // ACT
        PatientResponse result = service.createPatient(request);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("Jane");

        // Verify repository called
        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(patientCaptor.capture());
        Patient savedArg = patientCaptor.getValue();
        assertThat(savedArg.getTenantId()).isEqualTo(TENANT_ID);

        // Verify event published
        verify(eventPublisher).publishPatientCreatedEvent(savedPatient);
    }

    // Validation test: invalid input
    @Test
    void shouldThrowValidationError_WhenFirstNameEmpty() {
        // ARRANGE
        CreatePatientRequest request = CreatePatientRequest.builder()
            .firstName("")  // Invalid
            .lastName("Smith")
            .build();

        // ACT & ASSERT
        assertThatThrownBy(() -> service.createPatient(request))
            .isInstanceOf(IllegalArgumentException.class);

        // Verify repository NOT called
        verify(patientRepository, never()).save(any());
    }

    // Multi-tenant test: different tenant
    @Test
    void shouldNotReturnPatient_FromDifferentTenant() {
        // ARRANGE
        UUID patientId = UUID.randomUUID();
        TenantContext.setCurrentTenant("bluecross");  // Different tenant

        when(patientRepository.findByIdAndTenantId(patientId, "bluecross"))
            .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> service.getPatient(patientId.toString()))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
```

### Step 3: Create Integration Test for Controller

```java
// File: PatientControllerIntegrationTest.java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PatientControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TENANT_ID = "anthem";
    private static final String BASE_URL = "/api/v1/patients";

    @BeforeEach
    void setUp() {
        patientRepository.deleteAll();
    }

    // ✅ Create patient
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreatePatient_AndPersistToDatabase() throws Exception {
        // ARRANGE
        CreatePatientRequest request = CreatePatientRequest.builder()
            .firstName("John")
            .lastName("Doe")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();

        // ACT & ASSERT
        mockMvc.perform(post(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.firstName").value("John"))
            .andExpect(jsonPath("$.lastName").value("Doe"));

        // Verify persisted
        List<Patient> patients = patientRepository.findAll();
        assertThat(patients).hasSize(1);
        assertThat(patients.get(0).getFirstName()).isEqualTo("John");
    }

    // ✅ Retrieve patient
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRetrievePatient_ByIdAndTenant() throws Exception {
        // ARRANGE
        Patient patient = PatientTestDataBuilder.aPatient()
            .withTenantId(TENANT_ID)
            .withFirstName("Jane")
            .build();
        patientRepository.save(patient);

        // ACT & ASSERT
        mockMvc.perform(get(BASE_URL + "/" + patient.getId())
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Jane"));
    }

    // ❌ Unauthorized: missing role
    @Test
    void shouldReturn401_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(post(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    // ❌ Forbidden: wrong role
    @Test
    @WithMockUser(roles = "VIEWER")  // Only ADMIN allowed
    void shouldReturn403_WhenInsufficientRole() throws Exception {
        mockMvc.perform(post(BASE_URL)
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());
    }

    // ❌ Not found
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn404_WhenPatientNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get(BASE_URL + "/" + nonExistentId)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isNotFound());
    }

    // Multi-tenant isolation
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldNotReturnPatient_FromDifferentTenant() throws Exception {
        // Create patient for Anthem
        Patient anthemPatient = PatientTestDataBuilder.aPatient()
            .withTenantId("anthem")
            .withFirstName("John")
            .build();
        patientRepository.save(anthemPatient);

        // Try to retrieve as Blue Cross
        mockMvc.perform(get(BASE_URL + "/" + anthemPatient.getId())
                .header("X-Tenant-ID", "bluecross"))
            .andExpect(status().isNotFound());
    }

    // Update patient
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUpdatePatient() throws Exception {
        // Create patient
        Patient patient = PatientTestDataBuilder.aPatient()
            .withTenantId(TENANT_ID)
            .withFirstName("John")
            .build();
        patientRepository.save(patient);

        // Update
        UpdatePatientRequest updateRequest = UpdatePatientRequest.builder()
            .firstName("Johnny")
            .lastName("Doe")
            .build();

        mockMvc.perform(put(BASE_URL + "/" + patient.getId())
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Johnny"));

        // Verify persisted
        Patient updated = patientRepository.findById(patient.getId()).orElseThrow();
        assertThat(updated.getFirstName()).isEqualTo("Johnny");
    }

    // Delete patient
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldDeletePatient() throws Exception {
        // Create patient
        Patient patient = PatientTestDataBuilder.aPatient()
            .withTenantId(TENANT_ID)
            .build();
        patientRepository.save(patient);

        // Delete
        mockMvc.perform(delete(BASE_URL + "/" + patient.getId())
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isNoContent());

        // Verify deleted
        assertThat(patientRepository.findById(patient.getId())).isEmpty();
    }
}
```

### Step 4: Create Entity-Migration Validation Test

```java
// File: EntityMigrationValidationTest.java
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = PatientEventServiceApplication.class
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class EntityMigrationValidationTest {

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private DataSource dataSource;

    @Test
    void shouldHaveValidEntityMigrations() {
        // Validate that entities match Liquibase schema
        var metadata = sessionFactory.getSessionFactoryOptions()
            .getMetadataRepository()
            .getMetadataBuilder(dataSource)
            .build();

        SchemaValidator validator = new SchemaValidator(metadata, sessionFactory);

        assertDoesNotThrow(() -> {
            validator.validate();
        }, "Entity definitions should match database schema from Liquibase migrations");
    }

    @Test
    void patientTableShouldExist() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metadata = conn.getMetaData();
            ResultSet tables = metadata.getTables(null, "public", "PATIENTS", null);
            assertTrue(tables.next(), "patients table should exist");
        }
    }

    @Test
    void patientTableShouldHaveTenantIdColumn() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metadata = conn.getMetaData();
            ResultSet columns = metadata.getColumns(null, "public", "PATIENTS", "TENANT_ID");
            assertTrue(columns.next(), "tenant_id column should exist in patients table");

            String typeName = columns.getString("TYPE_NAME");
            assertEquals("varchar", typeName.toLowerCase(), "tenant_id should be varchar type");

            String isNullable = columns.getString("IS_NULLABLE");
            assertEquals("NO", isNullable, "tenant_id should be NOT NULL");
        }
    }

    @Test
    void patientTableShouldHavePrimaryKey() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metadata = conn.getMetaData();
            ResultSet primaryKeys = metadata.getPrimaryKeys(null, "public", "PATIENTS");
            assertTrue(primaryKeys.next(), "patients table should have primary key");

            String pkName = primaryKeys.getString("PK_NAME");
            assertNotNull(pkName, "Primary key name should exist");
        }
    }
}
```

### Step 5: Run Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests PatientServiceTest

# Run specific test method
./gradlew test --tests PatientServiceTest.shouldReturnPatient_WhenExists

# Run with detailed output
./gradlew test --info

# Generate coverage report
./gradlew test jacocoTestReport
# View report: build/reports/jacoco/test/html/index.html
```

---

## Real-World Examples from HDIM

### Example 1: Patient Service Tests

**Where:** `backend/modules/services/patient-event-service/src/test/java/`

**Test Coverage:**
- PatientServiceTest (unit): 20+ tests (mocked dependencies)
- PatientControllerIntegrationTest (integration): 15+ tests (real database)
- EntityMigrationValidationTest (schema): 5+ tests (entity-migration sync)
- Total: 40+ tests for Patient domain

**Test pyramid:**
- 60% unit tests (PatientServiceTest)
- 30% integration tests (PatientControllerIntegrationTest)
- 10% schema validation (EntityMigrationValidationTest)

### Example 2: Quality Measure Service Tests

**Where:** `backend/modules/services/quality-measure-event-service/src/test/java/`

**Special considerations:**
- CQL evaluation testing (complex business logic)
- Measure definition caching tests
- Event publishing tests (Kafka)
- Multi-tenant measure isolation

**Test patterns:**
- Mock CQL engine for service tests
- Real CQL engine for integration tests
- Verify event published within transaction

### Example 3: Care Gap Service Tests

**Where:** `backend/modules/services/care-gap-event-service/src/test/java/`

**Event-driven testing:**
- Event consumption tests (Kafka listeners)
- Event publishing tests
- Transactional outbox pattern tests
- Multi-tenant event isolation

---

## Best Practices

### ✅ DO's

- ✅ **DO write unit tests for all public methods**
  - Why: Catch bugs early; unit tests are fast (1-10ms)
  - Example: Every service method should have unit test

- ✅ **DO use AAA pattern (Arrange-Act-Assert)**
  - Why: Clear structure, easy to read and maintain
  - Example: Every test has setup, execution, verification phases

- ✅ **DO mock external dependencies**
  - Why: Unit tests run fast and isolated (no database, HTTP, etc.)
  - Example: PatientRepository, EventPublisher, CacheManager

- ✅ **DO test error cases and validation**
  - Why: Most bugs are in error handling, not happy path
  - Example: Test null input, empty string, invalid ID

- ✅ **DO test multi-tenant isolation**
  - Why: Cross-tenant data leaks are critical bugs
  - Example: Verify tenant A cannot access tenant B's data

- ✅ **DO test authorization (@PreAuthorize)**
  - Why: Authorization bugs are security vulnerabilities
  - Example: Verify VIEWER cannot POST; only ADMIN can

- ✅ **DO write integration tests for endpoints**
  - Why: Catch integration bugs (layers interacting incorrectly)
  - Example: Controller→Service→Repository→Database flow

- ✅ **DO use test data builders**
  - Why: Reduce boilerplate; make test intent clear
  - Example: PatientTestDataBuilder.aPatient().withFirstName("John").build()

- ✅ **DO verify mocks called with correct arguments**
  - Why: Verify service behavior, not just return value
  - Example: verify(repository).save(patientWithTenant("anthem"))

- ✅ **DO run tests frequently during development**
  - Why: Catch bugs early; TDD catches regressions
  - Example: ./gradlew test before every commit

### ❌ DON'Ts

- ❌ **DON'T skip unit tests for "simple" code**
  - Why: Simple code often has edge cases; tests prevent regressions
  - Example: Even getter/setter methods can have bugs

- ❌ **DON'T use real dependencies in unit tests**
  - Why: Unit tests become integration tests (slow, brittle)
  - Example: ❌ Create real database for unit test (wrong)

- ❌ **DON'T write tests that depend on each other**
  - Why: Tests must be independent (order doesn't matter)
  - Example: ❌ Test B depends on Test A's data (wrong)

- ❌ **DON'T ignore test failures**
  - Why: Test failure = bug in code or test; must fix
  - Example: ❌ Comment out failing test without fixing root cause

- ❌ **DON'T test implementation details**
  - Why: Tests should verify behavior, not how it's implemented
  - Example: ❌ Assert on method call count instead of result

- ❌ **DON'T create complex test fixtures**
  - Why: Complex setup makes tests hard to understand
  - Example: ❌ 100-line setUp() method (use builders instead)

- ❌ **DON'T skip error case tests**
  - Why: Most bugs are in error handling
  - Example: ❌ Only test happy path (wrong)

- ❌ **DON'T use hardcoded values**
  - Why: Makes tests brittle; values change over time
  - Example: ✅ Use constants or test data builders

- ❌ **DON'T skip authorization tests**
  - Why: Authorization bugs are security vulnerabilities
  - Example: ❌ Don't test @PreAuthorize enforcement

- ❌ **DON'T skip entity-migration validation**
  - Why: Schema drift causes production failures
  - Example: ❌ Add entity field but skip Liquibase migration

---

## Testing Checklist

### Before Writing Code

- [ ] Understand what code should do (requirements)
- [ ] Know what could go wrong (edge cases, errors)
- [ ] Plan unit tests (what to mock)
- [ ] Plan integration tests (what to verify)

### While Writing Code

- [ ] Write tests alongside code (TDD or parallel)
- [ ] Unit tests: All service methods
- [ ] Integration tests: All endpoints
- [ ] Error case tests: Exceptions, validation
- [ ] Multi-tenant tests: Tenant isolation
- [ ] Authorization tests: @PreAuthorize enforcement
- [ ] Transaction tests: Rollback on error
- [ ] Event tests: Event publishing

### After Writing Code

- [ ] All tests passing (./gradlew test)
- [ ] Code coverage >80% (./gradlew jacocoTestReport)
- [ ] No hardcoded values
- [ ] Tests use AAA pattern
- [ ] Tests are independent (no dependencies)
- [ ] Mocks verify expected behavior
- [ ] EntityMigrationValidationTest passing
- [ ] Ready for code review

---

## Troubleshooting

### Common Issues

#### Issue 1: "Could Not Initialize Database"

**Symptoms:**
- Integration test fails: `Could not initialize database connection`
- @SpringBootTest starts but can't connect to test database

**Root cause:** Test database not configured; Spring doesn't know where to run integration tests.

**Solution:**
```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class MyIntegrationTest {
    // @AutoConfigureTestDatabase(replace = ANY) tells Spring to use in-memory H2
}

// Or configure in application-test.yml:
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
```

#### Issue 2: "NullPointerException in Test"

**Symptoms:**
- Test fails with NullPointerException
- Mock not initialized; dependency is null

**Root cause:** Mock not created properly; @Mock annotation not processed (forgot @ExtendWith).

**Solution:**
```java
// ❌ WRONG: Forgot @ExtendWith
class PatientServiceTest {
    @Mock
    private PatientRepository repository;

    @Test
    void test() {
        // repository is NULL (mock not created)
    }
}

// ✅ RIGHT: Has @ExtendWith
@ExtendWith(MockitoExtension.class)
class PatientServiceTest {
    @Mock
    private PatientRepository repository;  // Mock initialized

    @Test
    void test() {
        // repository is mocked (not null)
    }
}
```

#### Issue 3: "Test Passes Locally, Fails in CI"

**Symptoms:**
- Test works on developer machine but fails in GitHub Actions
- Different timing, database state, or environment

**Root cause:** Test is flaky (depends on timing, order, or state); passes sometimes, fails others.

**Solution:**
```java
// ❌ FLAKY: Depends on timing
@Test
void test() {
    asyncService.doSomething();
    Thread.sleep(100);  // Hope it finishes within 100ms?
    verify(mock).wasCalled();
}

// ✅ RELIABLE: Wait for expected result
@Test
void test() {
    asyncService.doSomething();
    await()
        .atMost(5, SECONDS)
        .until(() -> mock.wasCalled());  // Wait up to 5 seconds
}

// Or use @BeforeEach to reset state:
@BeforeEach
void setUp() {
    patientRepository.deleteAll();  // Fresh state for each test
}
```

---

## References & Resources

### HDIM Documentation

- [Spring Boot 3.x Patterns](../05-spring-boot/spring-boot-patterns.md) - Service implementation patterns
- [PostgreSQL + Liquibase](../04-data-persistence/postgresql-liquibase.md) - Database and schema setup
- [Multi-Tenant Architecture](../01-architecture/multi-tenant-architecture.md) - Tenant isolation testing

### External Resources

- **[JUnit 5 Documentation](https://junit.org/junit5/)** - Official JUnit 5 reference
- **[Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)** - Mocking framework
- **[Spring Testing](https://docs.spring.io/spring-framework/reference/testing.html)** - Spring testing docs
- **[MockMvc Guide](https://spring.io/guides/gs/testing-web/)** - Spring MVC testing

### Related Skills

- **Prerequisite:** Spring Boot 3.x (service implementation)
- **Complement:** Multi-Tenant Architecture (test isolation)
- **Advanced:** Spring Security & RBAC (authorization testing)

---

## Quick Reference

### Running Tests

```bash
# All tests
./gradlew test

# Specific test
./gradlew test --tests PatientServiceTest

# Specific test method
./gradlew test --tests PatientServiceTest.shouldReturnPatient_WhenExists

# Coverage report
./gradlew test jacocoTestReport
open build/reports/jacoco/test/html/index.html

# Run tests with output
./gradlew test --info
```

### Test Annotations

```java
@ExtendWith(MockitoExtension.class)  // Enable Mockito
@SpringBootTest                      // Full Spring context
@AutoConfigureMockMvc                // MockMvc bean
@Transactional                       // Rollback after test
@WithMockUser(roles = "ADMIN")       // Authenticated user

@Mock                                // Mock dependency
@InjectMocks                         // Inject mocks
@Spy                                 // Partial mock

@BeforeEach                          // Setup before each test
@AfterEach                           // Cleanup after each test
@BeforeAll                           // Setup once before all
@AfterAll                            // Cleanup once after all
```

---

## Key Takeaways

1. **Test Pyramid Matters:** 60% unit tests (fast), 30% integration tests (realistic), 10% E2E (comprehensive). Invest in fast feedback.

2. **Mock External Dependencies:** Unit tests should be isolated. Mock databases, HTTP, Kafka. Real dependencies go in integration tests.

3. **AAA Pattern Works:** Arrange-Act-Assert structure makes tests readable. Every test has setup, execution, verification phases.

4. **Multi-Tenant Testing is Critical:** Verify tenant A cannot read/write tenant B's data. Cross-tenant leaks are security vulnerabilities.

5. **Authorization Testing Prevents Vulnerabilities:** Test that @PreAuthorize enforcement works. Authorization bugs are security holes.

---

## FAQ

**Q: How much test coverage is enough?**
A: Aim for >80% code coverage. 100% coverage is impossible (unreachable code, randomness). Focus on critical paths and error cases.

**Q: Should I test getters and setters?**
A: No, skip trivial getters/setters (Spring generated). Test business logic, validation, error handling.

**Q: Why do my tests fail randomly?**
A: Tests are flaky (timing-dependent). Use await() for async operations; use @BeforeEach to reset state; avoid Thread.sleep().

**Q: How do I test async operations?**
A: Use awaitility library: `await().atMost(5, SECONDS).until(() -> condition)`

**Q: Should I test private methods?**
A: No, test public interface. Private methods are implementation details. Test them indirectly through public methods.

---

## Next Steps

After completing this guide:

1. **Practice:** Write complete test suite for one service
2. **Coverage:** Run coverage report (./gradlew jacocoTestReport)
3. **Review:** Have peer review tests for patterns
4. **CI/CD:** Ensure tests run in GitHub Actions
5. **Learn:** Move to Spring Security & RBAC testing

---

**← Previous Guide:** [Spring Boot 3.x Patterns](../05-spring-boot/spring-boot-patterns.md)
**Skills Hub:** [Skills Center](../README.md)
**Next Guide:** [Spring Security & RBAC](../05-spring-boot/spring-security-rbac.md)

---

**Last Updated:** January 20, 2026
**Version:** 1.0
**Difficulty Level:** ⭐⭐⭐ (3/5 stars - Hands-on complexity)
**Time Investment:** 2 weeks
**Prerequisite Skills:** Spring Boot 3.x, Java 21, JPA/Hibernate
**Related Skills:** Spring Boot, Multi-Tenant Architecture, Spring Security & RBAC

---

**← [Skills Hub](../README.md)** | **→ [Next: Spring Security & RBAC](../05-spring-boot/spring-security-rbac.md)**
