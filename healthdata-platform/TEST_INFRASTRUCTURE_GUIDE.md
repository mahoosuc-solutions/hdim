# HealthData Platform - Test Infrastructure Guide

## Overview

This document describes the comprehensive Spring Boot test infrastructure for the HealthData Platform modular monolith. The test infrastructure provides reusable base classes, configuration, and utilities for testing all layers of the application.

## Architecture

### Test Base Classes

The test infrastructure consists of four main base classes, organized by testing scope:

```
BaseRepositoryTest
    ├── For JPA repository tests
    ├── Lightweight testing environment
    └── No web/security configuration

BaseServiceTest
    ├── For service layer tests
    ├── Full Spring context
    └── Transaction management

BaseIntegrationTest
    ├── For full integration tests
    ├── Web environment (random port)
    ├── MockMvc support
    └── TestRestTemplate support

BaseWebControllerTest
    ├── Extends BaseIntegrationTest
    ├── REST controller specific utilities
    └── Enhanced HTTP assertion helpers
```

## Files

### 1. Test Base Classes

#### `/src/test/java/com/healthdata/HealthDataTestConfiguration.java`
- Spring Boot test configuration class
- Provides test-specific bean definitions
- Configures mock email service, REST template, and auditing
- Contains test data constants and feature flags

**Key Components:**
- `@TestConfiguration` annotation for test beans
- Mock `JavaMailSender` for email testing
- Test data constants (`TestDataConfig`)
- Feature flags for controlling test behavior (`TestFeatureFlags`)
- Timeout configurations (`TestTimeouts`)
- External service URL mappings

**Usage:**
```java
@SpringBootTest
@Import(HealthDataTestConfiguration.class)
public class MyTest {
    // Uses test beans from configuration
}
```

#### `/src/test/java/com/healthdata/BaseRepositoryTest.java`
- Base class for JPA repository tests
- Uses `@DataJpaTest` for lightweight testing
- H2 in-memory database
- Auto-rollback after each test

**Features:**
- `waitMillis(long)` - Thread sleep utility
- `waitSeconds(long)` - Second-based sleep utility
- Automatic transaction rollback

**Usage:**
```java
@DataJpaTest
public class PatientRepositoryTest extends BaseRepositoryTest {
    @Autowired
    private PatientRepository patientRepository;

    @Test
    public void testFindByEmail() {
        // Repository test
    }
}
```

#### `/src/test/java/com/healthdata/BaseServiceTest.java`
- Base class for service layer tests
- Full Spring Boot test context
- Transaction management
- Common assertion utilities

**Features:**
- `generateRandomId()` - Generate random UUID strings
- `generateRandomId(String prefix)` - Generate prefixed IDs
- `waitMillis(long)` and `waitSeconds(long)` - Async testing utilities
- Assertion helper methods: `assertNotNull()`, `assertEqual()`, `assertTrue()`, etc.
- Access to test configuration via helper methods

**Usage:**
```java
@SpringBootTest
public class PatientServiceTest extends BaseServiceTest {
    @Autowired
    private PatientService patientService;

    @Test
    public void testGetPatient() {
        String patientId = generateRandomId("patient");
        // Test service logic
    }
}
```

#### `/src/test/java/com/healthdata/BaseIntegrationTest.java`
- Base class for integration tests
- Full web environment with random port
- MockMvc for Spring MVC testing
- TestRestTemplate for HTTP testing
- Common HTTP operation helpers

**Features:**
- `mockMvc` - Auto-wired MockMvc instance
- `restTemplate` - Auto-wired TestRestTemplate
- `objectMapper` - Auto-wired ObjectMapper for JSON handling
- HTTP method helpers with authentication:
  - `performGet()`, `performPost()`, `performPut()`, `performDelete()`, `performPatch()`
  - `performGetWithAuth()`, `performPostWithAuth()`, etc.
- Response parsing and assertion utilities:
  - `parseResponseContent()` - Parse response to object
  - `getResponseContent()` - Get response as string
  - `getStatusCode()` - Get HTTP status code
  - `assertStatus()` - Assert specific status code
  - `isSuccessResponse()` - Check for 2xx status
  - `isClientErrorResponse()` - Check for 4xx status
  - `isServerErrorResponse()` - Check for 5xx status
- JSON serialization utilities:
  - `toJson()` - Convert object to JSON
  - `fromJson()` - Parse JSON string to object

**Usage:**
```java
@SpringBootTest
public class PatientControllerTest extends BaseIntegrationTest {
    @Test
    public void testListPatients() throws Exception {
        MvcResult result = performGet("/api/patients");
        assertSuccessResponse(result);
        List<PatientDTO> patients = parseResponseContent(result, new TypeReference<List<PatientDTO>>(){});
        assertNotNull(patients, "Patients list should not be null");
    }
}
```

#### `/src/test/java/com/healthdata/BaseWebControllerTest.java`
- Extends `BaseIntegrationTest`
- REST controller-specific testing utilities
- Convenience assertion methods for HTTP status codes
- JSON field assertions and parsing

**Features:**
- HTTP status assertion methods:
  - `assertOkStatus()` - Assert HTTP 200
  - `assertCreatedStatus()` - Assert HTTP 201
  - `assertNoContentStatus()` - Assert HTTP 204
  - `assertBadRequestStatus()` - Assert HTTP 400
  - `assertUnauthorizedStatus()` - Assert HTTP 401
  - `assertForbiddenStatus()` - Assert HTTP 403
  - `assertNotFoundStatus()` - Assert HTTP 404
  - `assertConflictStatus()` - Assert HTTP 409
  - `assertUnprocessableEntityStatus()` - Assert HTTP 422
  - `assertInternalServerErrorStatus()` - Assert HTTP 500
- Response category assertions:
  - `assertSuccessResponse()` - Assert 2xx status
  - `assertClientErrorResponse()` - Assert 4xx status
  - `assertServerErrorResponse()` - Assert 5xx status
- Combined assertions:
  - `assertStatusAndParse()` - Assert status and parse response
  - `assertOkAndParse()` - Assert 200 and parse response
  - `assertCreatedAndParse()` - Assert 201 and parse response
- Header assertions:
  - `assertLocationHeaderPresent()` - Check for Location header
  - `assertContentType()` - Assert specific content type
  - `assertJsonContentType()` - Assert JSON content type
- JSON assertions:
  - `getJsonFieldValue()` - Extract specific JSON field
  - `assertJsonFieldExists()` - Assert field is present
  - `assertJsonFieldValue()` - Assert field has expected value

**Usage:**
```java
@SpringBootTest
public class PatientControllerTest extends BaseWebControllerTest {
    @Test
    public void testCreatePatient() throws Exception {
        PatientDTO patient = new PatientDTO();
        patient.setName("John Doe");

        MvcResult result = performPost("/api/patients", toJson(patient));
        PatientDTO created = assertCreatedAndParse(result, PatientDTO.class);

        assertJsonFieldValue(result, "name", "John Doe");
        String locationHeader = assertLocationHeaderPresent(result);
        assertTrue(locationHeader.contains("/api/patients/"), "Location should contain patient ID");
    }
}
```

### 2. Test Configuration

#### `/src/test/resources/application-test.yml`

Comprehensive test-specific Spring Boot configuration file. Activated with `@ActiveProfiles("test")`.

**Key Configuration Sections:**

1. **Database Configuration**
   - H2 in-memory database for fast, isolated tests
   - Configuration: `jdbc:h2:mem:testdb`
   - PostgreSQL dialect for compatibility
   - Hibernate `ddl-auto: create-drop` for schema recreation

2. **JPA/Hibernate Configuration**
   - Small connection pool (5 max connections)
   - Disabled second-level caching
   - `lazy-load-no-trans: true` for more realistic testing
   - SQL formatting disabled by default for performance

3. **Caching**
   - Simple in-memory cache for tests
   - No external cache dependencies

4. **Security**
   - Test JWT secret and tokens
   - No external auth provider required

5. **Email (Disabled)**
   - Configured for MailHog test server
   - No actual emails sent during tests

6. **WebSocket (Disabled)**
   - Configured but not active during tests
   - Can be enabled in specialized test profiles

7. **Task Execution**
   - Small thread pools for tests (2-4 threads)
   - Fast queue processing

8. **Logging**
   - WARNING level for most packages
   - DEBUG level for HealthData packages
   - Suitable for CI/CD environments

9. **Application-Specific Settings**
   - All external services disabled
   - Notification channels disabled
   - Feature flags for conditional feature testing
   - Single-threaded execution for deterministic tests

**Test Profiles:**

The configuration file includes additional profiles for specialized testing scenarios:

- `test-integration` - Full integration tests with Liquibase and caching
- `test-performance` - Performance tests with larger caches and parallelization
- `test-security` - Security tests with full security configuration

**Activation:**
```java
@SpringBootTest
@ActiveProfiles("test")
public class MyTest { }

// Or use multiple profiles
@ActiveProfiles({"test", "test-security"})
public class MySecurityTest { }
```

## Usage Examples

### Example 1: Repository Test

```java
@DataJpaTest
public class PatientRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private PatientRepository patientRepository;

    @Test
    public void testFindPatientByEmail() {
        // Given
        PatientEntity patient = new PatientEntity();
        patient.setEmail("john@example.com");
        patient.setFirstName("John");
        patientRepository.save(patient);

        // When
        PatientEntity found = patientRepository.findByEmail("john@example.com");

        // Then
        assertNotNull(found, "Patient should be found");
        assertEqual("John", found.getFirstName(), "First name should match");
    }
}
```

### Example 2: Service Test

```java
@SpringBootTest
public class PatientServiceTest extends BaseServiceTest {

    @Autowired
    private PatientService patientService;

    @MockBean
    private PatientRepository patientRepository;

    @Test
    public void testGetPatientById() {
        // Given
        String patientId = generateRandomId("patient");
        PatientDTO expected = new PatientDTO();
        expected.setId(patientId);
        expected.setName("John Doe");

        when(patientRepository.findById(patientId))
            .thenReturn(Optional.of(expected));

        // When
        PatientDTO result = patientService.getPatientById(patientId);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEqual(patientId, result.getId(), "IDs should match");
    }
}
```

### Example 3: Integration Test

```java
@SpringBootTest
public class PatientControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PatientRepository patientRepository;

    @Test
    public void testGetPatientEndpoint() throws Exception {
        // Given
        PatientEntity patient = new PatientEntity();
        patient.setId("patient-123");
        patient.setName("John Doe");
        patientRepository.save(patient);

        // When
        MvcResult result = performGet("/api/patients/patient-123");

        // Then
        assertSuccessResponse(result);
        PatientDTO dto = parseResponseContent(result, PatientDTO.class);
        assertEqual("John Doe", dto.getName(), "Names should match");
    }
}
```

### Example 4: REST Controller Test with Authentication

```java
@SpringBootTest
public class QualityMeasureControllerTest extends BaseWebControllerTest {

    @Test
    public void testCalculateMeasure() throws Exception {
        // Given
        String token = HealthDataTestConfiguration.TestDataConfig.TEST_JWT_TOKEN;
        MeasureRequestDTO request = new MeasureRequestDTO();
        request.setMeasureId("measure-001");
        request.setPatientId("patient-123");

        // When
        MvcResult result = performPostWithAuth(
            "/api/measures/calculate",
            toJson(request),
            token
        );

        // Then
        MeasureResultDTO resultDto = assertOkAndParse(result, MeasureResultDTO.class);
        assertJsonFieldValue(result, "status", "SUCCESS");
        assertLocationHeaderPresent(result);
    }

    @Test
    public void testUnauthorizedAccess() throws Exception {
        // When
        MvcResult result = performGet("/api/measures/protected");

        // Then
        assertUnauthorizedStatus(result);
    }
}
```

### Example 5: Using Test Data Configuration

```java
@SpringBootTest
public class NotificationServiceTest extends BaseServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Test
    public void testSendNotification() {
        // Given
        String patientId = getTestDataConfig().TEST_PATIENT_ID;
        String userId = getTestDataConfig().TEST_USER_ID;

        // When
        notificationService.sendNotification(patientId, userId, "Test message");

        // Then - verification logic
        assertTrue(getFeatureFlags().ENABLE_ASYNC_PROCESSING, "Async should be enabled");
    }
}
```

## Best Practices

### 1. Choose the Right Base Class

- Use `BaseRepositoryTest` for repository tests only
- Use `BaseServiceTest` for service/business logic tests
- Use `BaseIntegrationTest` for full integration tests
- Use `BaseWebControllerTest` for REST controller tests

### 2. Test Isolation

- Each test runs with a fresh database (H2 in-memory)
- Transactions are automatically rolled back
- No state carries between tests
- Use `@Transactional` for consistent test behavior

### 3. Async Testing

- Use `waitMillis()` or `waitSeconds()` for async operations
- Keep timeouts short (default 5 seconds)
- Consider using `@EnableAsync` in test configuration if needed
- Use `CountDownLatch` for more complex async scenarios

### 4. Mock External Services

- Mock all external HTTP calls
- Use test URLs from `ExternalServiceConfig`
- Disable notification channels in tests
- Use `@MockBean` for Spring beans

### 5. Authentication Testing

- Use provided test JWT token: `HealthDataTestConfiguration.TestDataConfig.TEST_JWT_TOKEN`
- Use `performGetWithAuth()`, `performPostWithAuth()` for authenticated endpoints
- Test both authorized and unauthorized scenarios

### 6. Assertion Patterns

```java
// Good: Specific assertion
assertOkStatus(result);
PatientDTO patient = parseResponseContent(result, PatientDTO.class);
assertNotNull(patient, "Patient should be returned");

// Avoid: Generic assertions
assertTrue(result.getResponse().getStatus() == 200);
```

### 7. Performance Considerations

- Run tests with `@DataJpaTest` when possible (lighter weight)
- Disable features not needed for the test
- Use smaller batch sizes in test config
- Single-threaded execution is the default for determinism

### 8. JSON Assertions

```java
// Extract and assert specific fields
assertJsonFieldExists(result, "user.id");
assertJsonFieldValue(result, "status", "ACTIVE");

// Or parse entire response
MyObject obj = parseResponseContent(result, MyObject.class);
assertEqual(expected, obj.getValue(), "Message");
```

## Configuration Customization

### Creating Test-Specific Profiles

Add new profiles to `application-test.yml`:

```yaml
---
spring:
  config:
    activate:
      on-profile: test-custom
  # Custom configuration here

healthdata:
  features:
    special-feature: true
```

### Overriding Test Configuration

In your test class:

```java
@SpringBootTest
@ActiveProfiles("test-custom")
@Import(HealthDataTestConfiguration.class)
@TestPropertySource(properties = {
    "server.port=9999",
    "spring.cache.type=redis"
})
public class MyCustomTest { }
```

## Troubleshooting

### Issue: Tests Fail with "Connection Refused"

**Cause:** External service is being called during test.

**Solution:** Mock the service or disable it in test configuration.

```java
@MockBean
private ExternalServiceClient externalService;

@BeforeEach
public void setUp() {
    when(externalService.call()).thenReturn(mockResponse);
}
```

### Issue: Database Locked Errors

**Cause:** Concurrent test execution with H2 in-memory database.

**Solution:** Configure Gradle to run tests serially:

```gradle
test {
    maxParallelForks = 1
}
```

### Issue: Async Tests Timeout

**Cause:** Async operations not completing within timeout.

**Solution:** Increase timeout or make test synchronous:

```yaml
test:
  timeout:
    async-timeout: 10000  # Increase to 10 seconds
```

### Issue: Entity Not Found After Save

**Cause:** Transaction not committed due to test transaction rollback.

**Solution:** Use `@Transactional(propagation = Propagation.REQUIRES_NEW)` for operations that need separate transactions.

## Maven Integration

The test infrastructure is compatible with Maven and Gradle. No additional configuration is needed beyond what's provided in the `build.gradle.kts` file.

To run tests:

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests PatientRepositoryTest

# Run with specific profile
./gradlew test -Dspring.profiles.active=test-security

# Run with coverage
./gradlew test --coverage
```

## Security Testing

The test infrastructure includes support for security testing:

```java
@SpringBootTest
public class SecurityTest extends BaseWebControllerTest {

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testAdminAccess() throws Exception {
        MvcResult result = performGet("/api/admin/settings");
        assertOkStatus(result);
    }

    @Test
    public void testUnauthorizedAccess() throws Exception {
        MvcResult result = performGet("/api/admin/settings");
        assertUnauthorizedStatus(result);
    }
}
```

## Future Enhancements

Potential improvements for the test infrastructure:

1. Test data builder pattern for complex entities
2. Performance benchmarking utilities
3. Contract testing for APIs
4. Database cleanup strategies (clean, truncate, drop)
5. Test container support for real databases
6. OpenAPI/Swagger contract validation
7. Mutation testing integration
8. Test result reporting and analytics

## Summary

The HealthData Platform test infrastructure provides:

- **4 base classes** for different testing scopes
- **Comprehensive configuration** via `application-test.yml`
- **Helper utilities** for common testing patterns
- **Security testing support** with authentication helpers
- **Best practices** and patterns for Spring Boot testing
- **Performance optimizations** for fast test execution
- **Extensibility** for customization and specialization

This infrastructure enables rapid test development while maintaining quality, consistency, and maintainability across the entire HealthData Platform application.
