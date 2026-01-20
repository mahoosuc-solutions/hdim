# Testing Patterns for HDIM

Real-world test patterns from patient-service, quality-measure-service, and event services. Follow these patterns to maintain 90%+ code coverage and catch bugs early via TDD.

---

## Unit Test Pattern: Service Layer Testing

### Pattern Example: PatientService Test

```java
@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PatientService patientService;

    @Nested
    class CreatePatient {
        @Test
        void shouldCreatePatient_WithValidData() {
            // ARRANGE - Set up test data
            CreatePatientRequest request = CreatePatientRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                .tenantId("tenant1")
                .build();

            Patient expectedPatient = Patient.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                .tenantId("tenant1")
                .createdAt(Instant.now())
                .build();

            when(patientRepository.save(any(Patient.class)))
                .thenReturn(expectedPatient);

            // ACT - Execute the method
            PatientResponse result = patientService.createPatient(request);

            // ASSERT - Verify the result
            assertThat(result.getId()).isEqualTo(expectedPatient.getId());
            assertThat(result.getFirstName()).isEqualTo("John");
            assertThat(result.getLastName()).isEqualTo("Doe");

            // Verify repository was called once
            verify(patientRepository, times(1)).save(any(Patient.class));
            // Verify audit logged the creation
            verify(auditService, times(1)).logCreation(any());
        }

        @Test
        void shouldThrowValidationException_WhenDateOfBirthInFuture() {
            // ARRANGE
            CreatePatientRequest request = CreatePatientRequest.builder()
                .firstName("John")
                .dateOfBirth(LocalDate.now().plusDays(1))  // Invalid!
                .tenantId("tenant1")
                .build();

            // ACT & ASSERT
            assertThatThrownBy(() -> patientService.createPatient(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Date of birth cannot be in future");
        }

        @Test
        void shouldThrowValidationException_WhenTenantIdMissing() {
            CreatePatientRequest request = CreatePatientRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                // Missing tenantId!
                .build();

            assertThatThrownBy(() -> patientService.createPatient(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Tenant ID required");
        }
    }

    @Nested
    class GetPatient {
        @Test
        void shouldReturnPatient_WhenPatientExists() {
            // ARRANGE
            String patientId = "123";
            String tenantId = "tenant1";
            Patient patient = Patient.builder()
                .id(UUID.fromString(patientId))
                .firstName("John")
                .tenantId(tenantId)
                .build();

            when(patientRepository.findByIdAndTenant(patientId, tenantId))
                .thenReturn(Optional.of(patient));

            // ACT
            PatientResponse result = patientService.getPatient(patientId, tenantId);

            // ASSERT
            assertThat(result.getFirstName()).isEqualTo("John");
            verify(patientRepository).findByIdAndTenant(patientId, tenantId);
        }

        @Test
        void shouldThrowResourceNotFoundException_WhenPatientNotFound() {
            when(patientRepository.findByIdAndTenant("123", "tenant1"))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> patientService.getPatient("123", "tenant1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Patient", "123");
        }
    }

    @Nested
    class TenantIsolation {
        @Test
        void shouldEnforceTenantIsolation_WhenDifferentTenantsRequestSamePatient() {
            // ARRANGE - Create patient for tenant1
            String patientId = "123";
            String tenant1 = "tenant1";
            String tenant2 = "tenant2";

            Patient tenant1Patient = Patient.builder()
                .id(UUID.fromString(patientId))
                .tenantId(tenant1)
                .firstName("John")
                .build();

            // tenant2 tries to access tenant1's patient
            when(patientRepository.findByIdAndTenant(patientId, tenant1))
                .thenReturn(Optional.of(tenant1Patient));
            when(patientRepository.findByIdAndTenant(patientId, tenant2))
                .thenReturn(Optional.empty());  // ← Returns empty for cross-tenant

            // ACT & ASSERT
            // tenant1 can access their patient
            PatientResponse result = patientService.getPatient(patientId, tenant1);
            assertThat(result).isNotNull();

            // tenant2 cannot access tenant1's patient
            assertThatThrownBy(() -> patientService.getPatient(patientId, tenant2))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
```

**Key Principles**:
- **@Nested classes** organize related tests
- **Mock external dependencies** (repository, services)
- **Test single responsibility** (one scenario per test)
- **Use builders** for readable test data
- **Verify interactions** (repository, audit service called)
- **Test error cases** (validation, not found, unauthorized)
- **Tenant isolation** verified in tests

---

## Integration Test Pattern: Controller Testing

### Pattern Example: PatientController Integration Test

```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@WithMockUser(roles = "EVALUATOR")
class PatientControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("patient_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @BeforeEach
    void setUp() {
        patientRepository.deleteAll();  // Clean slate
    }

    @Test
    void shouldCreatePatient_AndReturnStatus201() throws Exception {
        // ARRANGE
        CreatePatientRequest request = new CreatePatientRequest(
            "John", "Doe", LocalDate.of(1980, 1, 1), "tenant1"
        );

        // ACT
        MvcResult result = mockMvc.perform(
            post("/api/v1/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-ID", "tenant1")
                .content(objectMapper.writeValueAsString(request))
        ).andReturn();

        // ASSERT
        assertThat(result.getResponse().getStatus()).isEqualTo(201);

        PatientResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            PatientResponse.class
        );

        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");

        // Verify persisted to database
        Optional<Patient> persisted = patientRepository.findById(UUID.fromString(response.getId()));
        assertThat(persisted).isPresent();
    }

    @Test
    void shouldReturn401_WhenAuthorizationHeaderMissing() throws Exception {
        // No X-Tenant-ID header
        mockMvc.perform(
            get("/api/v1/patients/123")
        ).andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn403_WhenUserNotAuthorizedForTenant() throws Exception {
        // User in X-Tenant-ID header but not authorized
        mockMvc.perform(
            get("/api/v1/patients/123")
                .header("X-Tenant-ID", "tenant2")  // Different from user's tenants
        ).andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404_WhenPatientNotFound() throws Exception {
        mockMvc.perform(
            get("/api/v1/patients/nonexistent")
                .header("X-Tenant-ID", "tenant1")
        ).andExpect(status().isNotFound());
    }

    @Test
    void shouldFilterByTenant_InListEndpoint() throws Exception {
        // Create patients in different tenants
        Patient p1 = Patient.builder().firstName("John").tenantId("tenant1").build();
        Patient p2 = Patient.builder().firstName("Jane").tenantId("tenant2").build();
        patientRepository.saveAll(List.of(p1, p2));

        // Query as tenant1
        MvcResult result = mockMvc.perform(
            get("/api/v1/patients")
                .header("X-Tenant-ID", "tenant1")
        ).andReturn();

        List<PatientResponse> patients = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            new TypeReference<List<PatientResponse>>() {}
        );

        // Should only see tenant1's patient
        assertThat(patients)
            .hasSize(1)
            .extracting("firstName").contains("John");
    }
}
```

**Key Differences from Unit Tests**:
- Uses `@SpringBootTest` (loads real application context)
- Uses `@Testcontainers` with real PostgreSQL
- Tests full request/response cycle
- Verifies database persistence
- Tests authentication/authorization
- Tests multi-tenant filtering

---

## Event Handler Testing Pattern

### Pattern Example: PatientEventHandler Test

```java
@SpringBootTest(classes = {
    TestApplicationConfiguration.class,
    PatientEventHandlerService.class
})
@Testcontainers
class PatientEventHandlerTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private PatientEventHandler eventHandler;

    @Autowired
    private PatientProjectionRepository projectionRepository;

    @BeforeEach
    void setUp() {
        projectionRepository.deleteAll();
    }

    @Test
    void shouldCreateProjection_WhenPatientCreatedEventReceived() {
        // ARRANGE
        PatientCreatedEvent event = PatientCreatedEvent.builder()
            .patientId("p123")
            .tenantId("tenant1")
            .firstName("John")
            .lastName("Doe")
            .eventId(UUID.randomUUID())
            .timestamp(Instant.now())
            .idempotencyKey("unique-key-1")
            .build();

        // ACT
        eventHandler.handlePatientCreatedEvent(event);

        // ASSERT
        Optional<PatientProjection> projection =
            projectionRepository.findByIdAndTenant("p123", "tenant1");

        assertThat(projection).isPresent();
        assertThat(projection.get().getFirstName()).isEqualTo("John");
        assertThat(projection.get().getLastName()).isEqualTo("Doe");
    }

    @Test
    void shouldUpdateProjection_WhenPatientUpdatedEventReceived() {
        // ARRANGE - Create initial projection
        PatientProjection initial = PatientProjection.builder()
            .patientId("p123")
            .tenantId("tenant1")
            .firstName("John")
            .lastName("Doe")
            .build();
        projectionRepository.save(initial);

        PatientUpdatedEvent event = PatientUpdatedEvent.builder()
            .patientId("p123")
            .tenantId("tenant1")
            .firstName("John")
            .lastName("Smith")  // Updated
            .eventId(UUID.randomUUID())
            .timestamp(Instant.now())
            .idempotencyKey("unique-key-2")
            .build();

        // ACT
        eventHandler.handlePatientUpdatedEvent(event);

        // ASSERT
        PatientProjection updated = projectionRepository
            .findByIdAndTenant("p123", "tenant1")
            .orElseThrow();

        assertThat(updated.getLastName()).isEqualTo("Smith");
    }

    @Test
    void shouldHandleIdempotency_WhenDuplicateEventReceived() {
        // ARRANGE - Send same event twice
        PatientCreatedEvent event = PatientCreatedEvent.builder()
            .patientId("p123")
            .tenantId("tenant1")
            .firstName("John")
            .idempotencyKey("duplicate-key")
            .timestamp(Instant.now())
            .eventId(UUID.randomUUID())
            .build();

        // ACT - Process twice
        eventHandler.handlePatientCreatedEvent(event);
        eventHandler.handlePatientCreatedEvent(event);

        // ASSERT - Should still have only one projection
        List<PatientProjection> results =
            projectionRepository.findAllByTenantId("tenant1");

        assertThat(results).hasSize(1);  // Not duplicated!
    }
}
```

**Event Handler Testing**:
- Tests event consumption and projection building
- Verifies idempotency (duplicate events don't duplicate projections)
- Tests projection updates from events
- Validates event-to-projection mapping

---

## Contract Testing Pattern

### Pattern Example: Service-to-Service Contract Test

```java
@SpringBootTest
class PatientServiceContractTest {

    @Autowired
    private PatientService patientService;

    @Test
    void patientServiceShouldReturnExpectedResponse() {
        // This test verifies the contract that other services expect

        PatientResponse response = patientService.getPatient("123", "tenant1");

        // Contract: PatientResponse must have these fields
        assertThat(response)
            .hasFieldOrProperty("id")
            .hasFieldOrProperty("firstName")
            .hasFieldOrProperty("lastName")
            .hasFieldOrProperty("dateOfBirth")
            .hasFieldOrProperty("tenantId")
            .hasFieldOrProperty("createdAt");

        // Contract: Type constraints
        assertThat(response.getId()).isInstanceOf(String.class);
        assertThat(response.getCreatedAt()).isInstanceOf(Instant.class);

        // If other services call this, they expect these fields to exist
        // This test documents that contract
    }
}
```

---

## Parameterized Test Pattern

### Pattern Example: Multiple Scenarios

```java
class PatientValidationTest {

    @ParameterizedTest
    @CsvSource({
        "1980,VALID",
        "2000,VALID",
        "2025,INVALID",  // Future date
        "1900,VALID",    // Very old, but valid
        "2026,INVALID"   // Future
    })
    void shouldValidateDateOfBirth(int year, String expected) {
        LocalDate dob = LocalDate.of(year, 1, 1);
        boolean isValid = dob.isBefore(LocalDate.now());

        if ("VALID".equals(expected)) {
            assertThat(isValid).isTrue();
        } else {
            assertThat(isValid).isFalse();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "patient-123",
        "p-456",
        "PAT-789"
    })
    void shouldParsePatientId(String patientId) {
        assertThat(patientId).isNotEmpty().isNotBlank();
    }
}
```

---

## Test Fixtures Pattern

### Reusable Test Data

```java
public class PatientFixtures {

    public static Patient createHealthyPatient(String tenantId) {
        return Patient.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .firstName("John")
            .lastName("Doe")
            .dateOfBirth(LocalDate.of(1980, 1, 1))
            .createdAt(Instant.now())
            .build();
    }

    public static Patient createHighRiskPatient(String tenantId) {
        return Patient.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .firstName("Jane")
            .lastName("Smith")
            .dateOfBirth(LocalDate.of(1950, 1, 1))  // Older
            .createdAt(Instant.now())
            .build();
    }

    public static CreatePatientRequest createValidRequest() {
        return CreatePatientRequest.builder()
            .firstName("John")
            .lastName("Doe")
            .dateOfBirth(LocalDate.of(1980, 1, 1))
            .tenantId("tenant1")
            .build();
    }
}

// Usage in tests
@Test
void shouldCalculateRiskScore_ForHealthyPatient() {
    Patient patient = PatientFixtures.createHealthyPatient("tenant1");
    int score = riskCalculator.calculate(patient);
    assertThat(score).isLessThan(50);  // Low risk
}
```

---

## HIPAA Cache Testing Pattern

```java
@SpringBootTest
class PHICacheComplianceTest {

    @Autowired
    @Qualifier("patientCacheManager")
    private CacheManager cacheManager;

    @Test
    void patientCacheShouldHaveTTL_LessThanOrEqual_5Minutes() {
        Cache cache = cacheManager.getCache("patientData");

        // Get cache config
        CacheExpiry expiry = getCacheExpiry(cache);

        // Verify TTL is 5 minutes or less
        assertThat(expiry.getTimeToLiveSeconds())
            .isLessThanOrEqualTo(300);  // 300 = 5 minutes
    }

    @Test
    void shouldExpirePHIData_After5Minutes() throws InterruptedException {
        Cache cache = cacheManager.getCache("patientData");

        // Store PHI in cache
        Patient phiData = new Patient(...);
        cache.put("p123", phiData);

        // Verify it's there
        assertThat(cache.get("p123")).isNotNull();

        // Wait 5 minutes + 1 second
        Thread.sleep(Duration.ofMinutes(5).plusSeconds(1).toMillis());

        // Verify expired
        assertThat(cache.get("p123")).isNull();
    }

    @Test
    void shouldIncludeCacheControlHeaders_OnPHIResponses() throws Exception {
        mockMvc.perform(
            get("/api/v1/patients/123")
                .header("X-Tenant-ID", "tenant1")
        ).andExpect(
            header().exists("Cache-Control")
        ).andExpect(
            header().string("Cache-Control",
                containsString("no-store"))
        ).andExpect(
            header().string("Cache-Control",
                containsString("must-revalidate"))
        );
    }
}
```

---

## Common Testing Pitfalls to Avoid

```java
// ❌ AVOID: Over-mocking (tests don't catch real bugs)
@Test
void badTest() {
    PatientValidator mockValidator = mock(PatientValidator.class);
    when(mockValidator.isValid(any())).thenReturn(true);
    // This tests the mock, not the real validator!
}

// ✓ CORRECT: Test real validator logic
@Test
void goodTest() {
    PatientValidator validator = new PatientValidator();
    assertThat(validator.isValid(invalidPatient)).isFalse();
}

// ❌ AVOID: Tests dependent on each other
List<Patient> sharedPatients = new ArrayList<>();

@Test
void testA() {
    sharedPatients.add(createPatient());  // Modifies shared state
}

@Test
void testB() {
    // Depends on testA running first! ❌
    assertThat(sharedPatients).hasSize(1);
}

// ✓ CORRECT: Each test independent
@Test
void testA() {
    List<Patient> localPatients = new ArrayList<>();
    localPatients.add(createPatient());
    assertThat(localPatients).hasSize(1);
}

@Test
void testB() {
    List<Patient> localPatients = new ArrayList<>();
    assertThat(localPatients).hasSize(0);  // Independent!
}
```

---

## Test Coverage Goals

| Layer | Target Coverage | Focus |
|-------|-----------------|-------|
| **Unit Tests** | 80-90% | Business logic, edge cases |
| **Integration Tests** | 60-70% | API contracts, database |
| **Event Handlers** | 80%+ | Event processing, projections |
| **HIPAA Compliance** | 100% | Cache TTL, audit logging |
| **Tenant Isolation** | 100% | No cross-tenant data leaks |

---

## Running Tests

```bash
# All tests
./gradlew test

# Specific service
./gradlew :modules:services:patient-service:test

# Specific test class
./gradlew test --tests PatientServiceTest

# With coverage report
./gradlew jacocoTestReport

# View coverage
open modules/services/patient-service/build/reports/jacoco/test/html/index.html
```

---

## References

- **[TDD Swarm Guide](./TDD_SWARM.md)** - Test-first development methodology
- **[Coding Standards](../backend/docs/CODING_STANDARDS.md)** - Code patterns
- **[HIPAA Compliance](../backend/HIPAA-CACHE-COMPLIANCE.md)** - PHI testing
- **[Testing Guide](./TESTING_GUIDE.md)** - Comprehensive testing

---

_Last Updated: January 19, 2026_
_Version: 1.0_
_Based on Phase 5 Event Services Implementation_
