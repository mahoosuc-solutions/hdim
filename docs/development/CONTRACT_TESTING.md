# CONTRACT TESTING GUIDE

Consumer-driven contract testing standards for HDIM microservices using Spring Cloud Contract.

**Last Updated**: January 19, 2026
**Status**: Phase 2, P1 Critical Guide
**Focus**: Preventing breaking changes between services, API compatibility verification

---

## Overview

**Contract Testing** validates that services communicate according to agreed-upon contracts, catching incompatibilities before they reach production.

### Testing Pyramid with Contracts

```
Unit Tests (60-70%) → Component isolation
Integration Tests (20-30%) → Service + real dependencies
Contract Tests (5-10%) → Service-to-service communication
E2E Tests (5-10%) → Full user workflows
```

### Problem Solved

**Without Contract Testing:**
- Service A adds new required field to API response
- Service B still expects old field format
- Bug discovered only in staging/production
- Requires rollback and emergency fix

**With Contract Testing:**
- Contract defines exact API format both services must follow
- Consumer defines what it needs, Producer verifies it provides it
- Breaking changes caught during local development
- CI/CD pipeline prevents incompatible changes from merging

---

## Spring Cloud Contract Overview

Spring Cloud Contract enables **Consumer-Driven Contract Testing** (CDCT) in two modes:

### Mode 1: Consumer-Side (Consumer Driven)

```
1. Consumer defines contract: "I need GET /api/v1/patients/{id} to return {...}"
2. Contract shared with Producer
3. Producer generates & runs tests from contract
4. Producer can't merge code that violates contract
```

**When to use**: APIs where consumers define requirements

### Mode 2: Producer-Side (Producer Defined)

```
1. Producer defines contract: "GET /api/v1/patients/{id} returns {...}"
2. Contract shared with Consumers
3. Consumers generate test stubs from contract
4. Consumers develop against stub, then swap for real service
```

**When to use**: Public APIs, multiple consumers

### HDIM Approach (Hybrid)

For microservices with defined internal APIs:

```
Service A (Consumer) ← Contract ← Service B (Producer)
    ↓
  Uses contract to:
  - Define expectations
  - Generate stubs for development
  - Validate producer compliance
```

---

## Setting Up Contract Testing

### Dependencies (Already Included)

All HDIM services have Spring Cloud Contract dependencies via the shared testing module:

```gradle
// Included via modules/shared/testing/build.gradle.kts
testImplementation("org.springframework.cloud:spring-cloud-contract-wiremock:4.1.0")
testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner:4.1.0")
testImplementation("org.springframework.cloud:spring-cloud-contract-gradle-plugin:4.1.0")
```

### Directory Structure

```
src/test/resources/contracts/
├── patient-service/
│   ├── patient-controller.groovy
│   ├── patient-api.groovy
│   └── patient-query.groovy
├── quality-measure-service/
│   ├── quality-measure-controller.groovy
│   └── evaluation-api.groovy
└── care-gap-service/
    └── care-gap-controller.groovy
```

---

## Writing Contracts with Groovy DSL

### Contract File Format

```groovy
// src/test/resources/contracts/patient-service/get-patient-by-id.groovy

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description 'should return patient when patient exists'

    request {
        method GET()
        url('/api/v1/patients/550e8400-e29b-41d4-a716-446655440000')
        headers {
            contentType applicationJson()
        }
    }

    response {
        status 200
        headers {
            contentType applicationJson()
        }
        body("""
            {
                "id": "550e8400-e29b-41d4-a716-446655440000",
                "tenantId": "TENANT-001",
                "firstName": "John",
                "lastName": "Doe",
                "dateOfBirth": "1990-01-01",
                "createdAt": "2025-01-19T10:00:00Z"
            }
        """)
    }
}
```

### Contract with Request/Response Matchers

```groovy
// More flexible contract allowing dynamic values
Contract.make {
    description 'should return patient matching provided ID'

    request {
        method GET()
        urlPath('/api/v1/patients') {
            queryParameters {
                parameter 'id': regex(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/)
            }
        }
        headers {
            contentType applicationJson()
        }
    }

    response {
        status 200
        headers {
            contentType applicationJson()
        }
        body([
            id: regex(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/),
            tenantId: "TENANT-001",
            firstName: "John",
            lastName: "Doe",
            dateOfBirth: "1990-01-01",
            createdAt: regex(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z/)
        ])
    }
}
```

### Contract with Named Groups (Data Extraction)

```groovy
// Contract that captures values for reuse
Contract.make {
    description 'should create patient and return ID'

    request {
        method POST()
        url('/api/v1/patients')
        headers {
            contentType applicationJson()
        }
        body([
            firstName: "Jane",
            lastName: "Smith",
            dateOfBirth: "1995-05-15"
        ])
    }

    response {
        status 201
        headers {
            contentType applicationJson()
        }
        body([
            id: $(regex(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/)),
            tenantId: "TENANT-001",
            firstName: "Jane",
            lastName: "Smith",
            dateOfBirth: "1995-05-15",
            createdAt: $(regex(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z/))
        ])
    }
}
```

---

## Contract Testing Patterns

### Pattern 1: Request-Response Contracts

```groovy
// Get patient details
Contract.make {
    name("should return patient details")
    description("Retrieve patient by ID")
    priority(1)  // Execute order

    request {
        method GET()
        url('/api/v1/patients/550e8400-e29b-41d4-a716-446655440000')
        headers {
            header('X-Tenant-ID', 'TENANT-001')
            contentType(applicationJson())
        }
    }

    response {
        status 200
        headers {
            contentType applicationJson()
            header('X-Request-ID', regex(/[0-9a-f\-]{36}/))
        }
        body([
            id: '550e8400-e29b-41d4-a716-446655440000',
            firstName: 'John',
            lastName: 'Doe',
            status: anyOf(
                'ACTIVE',
                'INACTIVE',
                'SUSPENDED'
            )
        ])
    }
}
```

### Pattern 2: Error Response Contracts

```groovy
// Patient not found
Contract.make {
    name("should return 404 when patient not found")

    request {
        method GET()
        url('/api/v1/patients/99999999-9999-9999-9999-999999999999')
        headers {
            header('X-Tenant-ID', 'TENANT-001')
        }
    }

    response {
        status 404
        headers {
            contentType applicationJson()
        }
        body([
            status: 404,
            error: 'NOT_FOUND',
            message: 'Patient not found',
            timestamp: regex(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z/)
        ])
    }
}

// Unauthorized access
Contract.make {
    name("should return 403 when accessing other tenant's patient")

    request {
        method GET()
        url('/api/v1/patients/550e8400-e29b-41d4-a716-446655440000')
        headers {
            header('X-Tenant-ID', 'TENANT-OTHER')  // Different tenant
        }
    }

    response {
        status 403
        body([
            status: 403,
            error: 'FORBIDDEN',
            message: 'Access to this patient is forbidden'
        ])
    }
}
```

### Pattern 3: List/Search Contracts

```groovy
// Search patients with filters
Contract.make {
    name("should return filtered patients")

    request {
        method GET()
        url('/api/v1/patients') {
            queryParameters {
                parameter 'firstName': 'John'
                parameter 'lastName': 'Doe'
                parameter 'limit': '10'
                parameter 'offset': '0'
            }
        }
        headers {
            header('X-Tenant-ID', 'TENANT-001')
        }
    }

    response {
        status 200
        body([
            content: $(notEmpty()),  // Must have at least 1 item
            total: regex(/\d+/),
            page: 0,
            pageSize: 10
        ])
    }
}
```

### Pattern 4: Async/Event Contracts

```groovy
// Message to Kafka topic
Contract.make {
    name("should publish PatientCreatedEvent to patient-events topic")

    input {
        triggeredBy('patientService.createPatient()')
    }

    outputMessage {
        sentTo('patient-events')
        body([
            eventId: $(regex(/[0-9a-f\-]{36}/)),
            eventType: 'PATIENT_CREATED',
            tenantId: 'TENANT-001',
            patientId: $(regex(/^[0-9a-f]{8}-[0-9a-f]{4}.*$/)),
            firstName: 'John',
            lastName: 'Doe',
            timestamp: $(regex(/\d{4}-\d{2}-\d{2}T.*/))
        ])
    }
}
```

---

## Implementing Contract Tests (Producer Side)

The producer generates and runs tests from contracts to verify compliance.

### Auto-Generated Test Class

Spring Cloud Contract gradle plugin generates test classes:

```gradle
// build.gradle.kts
plugins {
    id("spring-cloud-contract")
}

contracts {
    testFramework = org.springframework.cloud.contract.verifier.config.TestFramework.JUNIT5
    generatedTestSourcesDir = "src/test/java"
    generatedTestClassName = "{PackageName}ContractVerificationTest"
    packageWithBaseClasses = "com.healthdata.patient.contracts"
}
```

Generated test: `PatientServiceContractVerificationTest.java`

```java
// Auto-generated from contracts in src/test/resources/contracts/
@SpringBootTest
@AutoConfigureMockMvc
public class PatientServiceContractVerificationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    public void validate_should_return_patient_when_patient_exists() throws Exception {
        // Auto-generated test body based on contract
        mockMvc.perform(get("/api/v1/patients/550e8400-e29b-41d4-a716-446655440000")
                .header("X-Tenant-ID", "TENANT-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.firstName").value("John"));
    }
}
```

### Base Class for Generated Tests

```java
// src/test/java/com/healthdata/patient/contracts/PatientServiceContractBase.java

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class PatientServiceContractBase {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected PatientRepository patientRepository;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void setUp() {
        // Insert test data that contracts expect
        Patient testPatient = Patient.builder()
            .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
            .tenantId("TENANT-001")
            .firstName("John")
            .lastName("Doe")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();
        patientRepository.save(testPatient);
    }
}
```

### Running Contract Tests

```bash
# Generate and run contract verification tests
./gradlew :modules:services:patient-service:contractTest

# Run specific contract test
./gradlew :modules:services:patient-service:contractTest --tests "PatientServiceContractVerificationTest"

# With detailed output
./gradlew :modules:services:patient-service:contractTest -i
```

---

## Using Contracts in Consumer Services

Consumer services use generated stubs for development before switching to real services.

### Gradle Configuration (Consumer)

```gradle
// build.gradle.kts for consumer service

dependencies {
    // Stub runner downloads contracts from producer
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner")
}

// Enable stub runner
tasks.test {
    useJUnitPlatform()
    systemProperty("stubrunner.stubs-mode", "LOCAL")
    systemProperty("stubrunner.repositoryRoot", "file:///../patient-service/build/stubs")
}
```

### Consumer Test Using Stubs

```java
// Consumer service (e.g., analytics-service)

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureStubServer
public class AnalyticsServiceConsumerTest {

    @Value("${stubrunner.runningstubs.patient-service.port}")
    private int patientServiceStubPort;

    @Autowired
    private RestTemplate restTemplate;

    @Test
    void shouldCallPatientServiceThroughStub() {
        // GIVEN - Stub provides contract-compliant responses
        String patientServiceUrl = "http://localhost:" + patientServiceStubPort;

        // WHEN - Call patient service
        ResponseEntity<PatientResponse> response = restTemplate.getForEntity(
            patientServiceUrl + "/api/v1/patients/550e8400-e29b-41d4-a716-446655440000",
            PatientResponse.class
        );

        // THEN - Verify response matches contract
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
            .extracting(PatientResponse::getFirstName)
            .isEqualTo("John");  // From contract
    }
}
```

### Consumer Runtime Configuration

```yaml
# application-test.yml for consumer service

spring:
  cloud:
    contract:
      verifier:
        # Download stubs from local build directories
        repositoryRoot: file://../patient-service/build/stubs

# Or download from artifact repository
stubrunner:
  ids: com.healthdata:patient-service:+:stubs
  repositoryRoot: http://nexus.company.com/nexus/content/repositories/releases
  stubs-mode: LOCAL
```

---

## Contract Verification in CI/CD

### Publishing Stubs

Producer services publish generated stubs to artifact repository:

```gradle
// build.gradle.kts

plugins {
    id("spring-cloud-contract")
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("stubs") {
            artifact(tasks.contractsStubsJar) {
                classifier = "stubs"
            }
        }
    }

    repositories {
        maven {
            url = uri("https://nexus.company.com/nexus/content/repositories/releases")
            credentials {
                username = System.getenv("NEXUS_USER")
                password = System.getenv("NEXUS_PASSWORD")
            }
        }
    }
}
```

### GitHub Actions Workflow

```yaml
# .github/workflows/contract-tests.yml

name: Contract Tests

on: [pull_request]

jobs:
  contract-tests:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'

      - name: Run Producer Contract Tests
        run: |
          ./gradlew :modules:services:patient-service:contractTest \
                    :modules:services:quality-measure-service:contractTest \
                    :modules:services:care-gap-service:contractTest

      - name: Verify Consumer Can Use Stubs
        run: |
          ./gradlew :modules:services:analytics-service:test \
                    :modules:services:reports-service:test

      - name: Publish Test Results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: contract-test-results
          path: '**/build/reports/contract-tests/**'
```

---

## Best Practices for Contract Testing

### ✅ DO

- **One contract per interaction**: One file per API endpoint/message
- **Name contracts clearly**: `get-patient-by-id.groovy`, not `test1.groovy`
- **Include error cases**: 404, 403, 400 responses
- **Use realistic data**: First names "John", "Jane", not "a", "b"
- **Match field types**: UUID format, date format, numeric ranges
- **Version contracts**: Track changes to contracts
- **Share with consumers**: Make contracts easily accessible
- **Keep contracts in source control**: Version control is essential

### ❌ DON'T

- **Over-specify contracts**: Don't lock down timestamps, IDs that vary
- **Use too-strict regexes**: `.*` for any value is usually better than `exact-uuid-12345`
- **Mix multiple interactions**: Don't put multiple API calls in one contract
- **Test business logic**: Contracts validate format, not logic
- **Ignore consumer needs**: Review what consumers actually need

---

## Common Contract Patterns for HDIM

### Pattern: Multi-Tenant Isolation

```groovy
// Tenant-aware contract
Contract.make {
    name("should isolate patient data by tenant")

    request {
        method GET()
        url('/api/v1/patients/550e8400-e29b-41d4-a716-446655440000')
        headers {
            header('X-Tenant-ID', 'TENANT-001')  // Required header
        }
    }

    response {
        status 200
        body([
            id: '550e8400-e29b-41d4-a716-446655440000',
            tenantId: 'TENANT-001',  // Must match request header
            firstName: 'John'
        ])
    }
}
```

### Pattern: HIPAA Compliance

```groovy
// PHI caching contract
Contract.make {
    name("should include cache-control headers for PHI")

    response {
        status 200
        headers {
            header('Cache-Control', 'no-store, no-cache, must-revalidate')
            header('Pragma', 'no-cache')
            contentType applicationJson()
        }
    }
}
```

### Pattern: Pagination

```groovy
Contract.make {
    name("should return paginated results with metadata")

    request {
        method GET()
        url('/api/v1/patients') {
            queryParameters {
                parameter 'page': '0'
                parameter 'size': '10'
            }
        }
    }

    response {
        status 200
        body([
            content: $(notEmpty()),
            pageable: [
                pageNumber: 0,
                pageSize: 10
            ],
            totalElements: regex(/\d+/),
            totalPages: regex(/\d+/),
            last: $(anyOf(true, false))
        ])
    }
}
```

---

## Troubleshooting Contract Tests

### Issue 1: "Contract file not found"

```
ERROR: Contract file src/test/resources/contracts/get-patient.groovy not found
```

**Solution:**
```bash
# Verify contract location
ls -la src/test/resources/contracts/

# Rebuild to generate tests
./gradlew clean contractTest
```

### Issue 2: "Generated test doesn't match implementation"

```
AssertionError: expected status 200 but got 404
```

**Cause**: Contract expects response that service doesn't provide

**Solution**:
```groovy
// Update contract to match actual service behavior
Contract.make {
    // ...
    response {
        status 200  // Match what service actually returns
        // ...
    }
}
```

### Issue 3: "Stub Runner can't find stubs"

```
ERROR: Could not find stub in repository for com.healthdata:patient-service
```

**Solution**:
```gradle
// Ensure stubs are published
./gradlew publishToMavenLocal

// Or configure repository
stubrunner.repositoryRoot = "file:///local/path/to/stubs"
```

---

## Evolution: From Manual Testing to Contract Testing

### Before Contract Testing

```
Service A Developer:
- Calls Service B manually
- Gets 200 with { firstName: "John" }
- Writes code assuming this format
- Doesn't commit contract

3 months later...
Service B updates API to { name: "John Smith" }
Service A breaks in production
Requires emergency hotfix
```

### After Contract Testing

```
Service B Developer:
- Defines contract: { firstName: "John", lastName: "Doe" }
- Commits contract to version control
- Generates & runs verification tests

Service A Developer:
- Sees contract in Version Control
- Runs against stub during development
- Code guaranteed to work with Service B
- Service B can't merge breaking changes (test fails)
```

---

## Running Full Contract Test Suite

```bash
# All contract tests in all services
./gradlew contractTest

# Specific service
./gradlew :modules:services:patient-service:contractTest

# With logging
./gradlew contractTest --info --stacktrace

# Generate reports
./gradlew contractTest --warning-mode all
```

**Output**: Check `build/reports/contract-tests/` for detailed HTML reports

---

## Integration with existing tests

Contract tests work alongside unit and integration tests:

```bash
# Unit tests (60-70% of effort)
./gradlew test

# Integration tests (20-30% of effort)
./gradlew test --tests "*IntegrationTest"

# Contract tests (5-10% of effort)
./gradlew contractTest

# All tests (full suite)
./gradlew check  # Runs unit + integration + contract tests
```

---

## Related Documentation

- **INTEGRATION_TESTING.md** - Service + database testing
- **TESTING_STRATEGY.md** - Unit test standards
- **TEST_COVERAGE.md** - Coverage measurement
- **BACKEND_API_SPECIFICATION.md** - API design patterns
- **GATEWAY_TRUST_ARCHITECTURE.md** - Service authentication

---

## Next Steps

1. **Define contracts** for critical service-to-service interactions (patient ↔ quality-measure, quality-measure ↔ care-gap)
2. **Generate contract tests** in producer services
3. **Update consumer tests** to use stubs for development
4. **Integrate into CI/CD** to run contract tests on every PR
5. **Review contracts** in code review: "Does this API change match what consumers expect?"

---

_Last Updated: January 19, 2026_
_Version: 1.0_
