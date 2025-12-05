# HealthData Platform - Test Infrastructure Quick Reference

## Choosing Your Base Class

```
Need to test...          → Use This Base Class
────────────────────────────────────────────────────────────
JPA repository layer     → BaseRepositoryTest (@DataJpaTest)
Business/service logic   → BaseServiceTest (@SpringBootTest)
Full integration flows   → BaseIntegrationTest (@SpringBootTest)
REST controllers         → BaseWebControllerTest (@SpringBootTest)
```

## Quick Examples

### Repository Test
```java
@DataJpaTest
public class PatientRepositoryTest extends BaseRepositoryTest {
    @Autowired private PatientRepository repo;

    @Test public void test() {
        PatientEntity p = new PatientEntity();
        repo.save(p);
        PatientEntity found = repo.findByEmail("test@test.com");
        assertNotNull(found, "Should find patient");
    }
}
```

### Service Test
```java
@SpringBootTest
public class PatientServiceTest extends BaseServiceTest {
    @Autowired private PatientService service;
    @MockBean private PatientRepository repo;

    @Test public void test() {
        String id = generateRandomId("patient");
        when(repo.findById(id)).thenReturn(Optional.of(patient));
        PatientDTO result = service.getPatientById(id);
        assertNotNull(result, "Should return patient");
    }
}
```

### Integration Test
```java
@SpringBootTest
public class PatientControllerTest extends BaseWebControllerTest {
    @Autowired private PatientRepository repo;

    @Test public void test() throws Exception {
        PatientEntity p = repo.save(new PatientEntity());
        MvcResult result = performGet("/api/patients/" + p.getId());
        assertOkStatus(result);
        PatientDTO dto = parseResponseContent(result, PatientDTO.class);
        assertNotNull(dto, "Should return DTO");
    }
}
```

### REST with Authentication
```java
@SpringBootTest
public class SecuredControllerTest extends BaseWebControllerTest {
    @Test public void test() throws Exception {
        String token = HealthDataTestConfiguration.TestDataConfig.TEST_JWT_TOKEN;
        MvcResult result = performGetWithAuth("/api/protected", token);
        assertOkStatus(result);
    }
}
```

## Common HTTP Operations

```java
// GET request
MvcResult result = performGet("/api/patients");

// POST request
MvcResult result = performPost("/api/patients", jsonBody);

// PUT request
MvcResult result = performPut("/api/patients/1", jsonBody);

// DELETE request
MvcResult result = performDelete("/api/patients/1");

// PATCH request
MvcResult result = performPatch("/api/patients/1", jsonBody);

// With authentication
MvcResult result = performGetWithAuth("/api/patients", token);
MvcResult result = performPostWithAuth("/api/patients", jsonBody, token);
```

## Common Assertions

```java
// Status assertions
assertOkStatus(result);                    // HTTP 200
assertCreatedStatus(result);               // HTTP 201
assertBadRequestStatus(result);            // HTTP 400
assertUnauthorizedStatus(result);          // HTTP 401
assertForbiddenStatus(result);             // HTTP 403
assertNotFoundStatus(result);              // HTTP 404
assertConflictStatus(result);              // HTTP 409
assertInternalServerErrorStatus(result);   // HTTP 500

// Response category assertions
assertSuccessResponse(result);             // 2xx
assertClientErrorResponse(result);         // 4xx
assertServerErrorResponse(result);         // 5xx

// Combined assertions and parsing
PatientDTO dto = assertOkAndParse(result, PatientDTO.class);
PatientDTO dto = assertCreatedAndParse(result, PatientDTO.class);
PatientDTO dto = assertStatusAndParse(result, 200, PatientDTO.class);

// Header assertions
assertHeaderPresent(result, "X-Custom-Header");
String location = assertLocationHeaderPresent(result);
assertContentType(result, "application/json");
assertJsonContentType(result);

// JSON field assertions
assertJsonFieldExists(result, "id");
assertJsonFieldValue(result, "status", "ACTIVE");
String value = getJsonFieldValue(result, "user.email");

// Custom assertions
assertNotNull(value, "Should not be null");
assertEqual(expected, actual, "Values should match");
assertTrue(condition, "Condition should be true");
```

## Test Data Access

```java
// In any test class extending BaseServiceTest or similar:

// Test configuration constants
HealthDataTestConfiguration.TestDataConfig.TEST_PATIENT_ID
HealthDataTestConfiguration.TestDataConfig.TEST_USER_ID
HealthDataTestConfiguration.TestDataConfig.TEST_JWT_TOKEN

// Or through helper methods (BaseServiceTest only)
getTestDataConfig().TEST_PATIENT_ID
getFeatureFlags().ENABLE_EMAIL_SENDING
getTestTimeouts().ASYNC_TIMEOUT_MILLIS
```

## JSON Handling

```java
// Convert object to JSON
String json = toJson(patientDTO);

// Parse JSON string
PatientDTO dto = fromJson(jsonString, PatientDTO.class);

// Parse response to object
PatientDTO dto = parseResponseContent(result, PatientDTO.class);

// Get raw response content
String content = getResponseContent(result);

// Extract JSON field
String email = getJsonFieldValue(result, "email");
```

## Async Testing

```java
// Wait for async operations
waitMillis(1000);      // Wait 1000ms
waitSeconds(5);        // Wait 5 seconds

// Or use CountDownLatch for more control
CountDownLatch latch = new CountDownLatch(1);
asyncService.doSomething(() -> latch.countDown());
boolean completed = latch.await(5, TimeUnit.SECONDS);
assertTrue(completed, "Operation should complete");
```

## Test Configuration

```yaml
# Profiles in application-test.yml
@ActiveProfiles("test")                 # Default test profile
@ActiveProfiles("test-integration")     # Integration tests
@ActiveProfiles("test-performance")     # Performance tests
@ActiveProfiles("test-security")        # Security tests

# Combine multiple profiles
@ActiveProfiles({"test", "test-security"})
```

## ID Generation

```java
// In BaseServiceTest
String id = generateRandomId();           // Random UUID
String id = generateRandomId("patient");  // "patient-<uuid>"
String id = generateRandomId("measure");  // "measure-<uuid>"
```

## Database

All tests use:
- **Database**: H2 in-memory
- **URL**: `jdbc:h2:mem:testdb`
- **DDL**: Auto create-drop (fresh schema per test)
- **Transactions**: Auto-rollback per test
- **Isolation**: Full per-test isolation

## Performance Tips

1. Use `@DataJpaTest` for repository tests (lightweight)
2. Use `@Transactional` on test class for consistency
3. Disable unnecessary features in test config
4. Keep thread pools small (2-4 threads)
5. Run tests serially for determinism: `maxParallelForks = 1`
6. Single-threaded execution is default

## Running Tests

```bash
# All tests
./gradlew test

# Specific test class
./gradlew test --tests PatientRepositoryTest

# Specific test method
./gradlew test --tests PatientRepositoryTest.testFindByEmail

# With specific profile
./gradlew test -Dspring.profiles.active=test-security

# Verbose output
./gradlew test -i

# With coverage
./gradlew test --coverage
```

## Common Patterns

### Testing Business Logic with Mocks
```java
@SpringBootTest
public class ServiceTest extends BaseServiceTest {
    @Autowired private MyService service;
    @MockBean private ExternalService external;

    @BeforeEach public void setUp() {
        when(external.call()).thenReturn(mockData);
    }

    @Test public void test() {
        MyResult result = service.doSomething();
        assertNotNull(result, "Should return result");
        verify(external).call();
    }
}
```

### Testing REST Endpoints
```java
@SpringBootTest
public class ControllerTest extends BaseWebControllerTest {
    @Test public void testCreate() throws Exception {
        MyDTO dto = new MyDTO();
        dto.setName("Test");

        MvcResult result = performPost("/api/items", toJson(dto));
        MyDTO created = assertCreatedAndParse(result, MyDTO.class);

        String id = getJsonFieldValue(result, "id");
        assertNotNull(id, "Should have ID");
    }
}
```

### Testing with Authentication
```java
@SpringBootTest
public class SecurityTest extends BaseWebControllerTest {
    @Test public void testAuthorized() throws Exception {
        String token = HealthDataTestConfiguration.TestDataConfig.TEST_JWT_TOKEN;
        MvcResult result = performGetWithAuth("/api/secure", token);
        assertOkStatus(result);
    }

    @Test public void testUnauthorized() throws Exception {
        MvcResult result = performGet("/api/secure");
        assertUnauthorizedStatus(result);
    }
}
```

## Files and Locations

```
├── src/test/java/com/healthdata/
│   ├── HealthDataTestConfiguration.java   ← Test beans and config
│   ├── BaseRepositoryTest.java            ← Repository test base
│   ├── BaseServiceTest.java               ← Service test base
│   ├── BaseIntegrationTest.java           ← Integration test base
│   └── BaseWebControllerTest.java         ← Controller test base
│
└── src/test/resources/
    └── application-test.yml               ← Test configuration

Documentation:
├── TEST_INFRASTRUCTURE_GUIDE.md           ← Complete guide
└── TEST_QUICK_REFERENCE.md                ← This file
```

## Key Classes and Annotations

```
@DataJpaTest                - Lightweight JPA testing (repo only)
@SpringBootTest             - Full application context
@AutoConfigureMockMvc       - Enable MockMvc
@Transactional              - Automatic rollback
@ActiveProfiles("test")     - Use test configuration
@Import(...)                - Import test beans
@MockBean                   - Mock Spring bean
@Mock                       - Mock non-Spring object
@BeforeEach                 - Setup before each test
@Test                       - Test method
```

## Troubleshooting Checklist

- Tests failing to connect? → Check H2 is properly configured
- External service calls? → Use @MockBean or disable in config
- Async tests timing out? → Increase timeout or use CountDownLatch
- Database locked? → Set maxParallelForks = 1 in build.gradle
- Authentication failing? → Use correct token from TestDataConfig
- Transaction issues? → Ensure @Transactional is on class/method

## Documentation

- Full guide: `TEST_INFRASTRUCTURE_GUIDE.md`
- This quick reference: `TEST_QUICK_REFERENCE.md`
- Example tests in each module's test directory
