# Contract Testing Guide

This guide covers consumer-driven contract testing (CDC) and OpenAPI compliance validation for HDIM microservices.

## Table of Contents

- [Quick Start Commands](#quick-start-commands)
- [Architecture Overview](#architecture-overview)
- [Writing Consumer Tests](#writing-consumer-tests-angular)
- [Writing Provider Tests](#writing-provider-tests-spring-boot)
- [Pact Broker Setup](#pact-broker-setup)
- [OpenAPI Validation](#openapi-validation)
- [CI/CD Integration](#cicd-integration)
- [Test Constants Reference](#test-constants-reference)
- [Troubleshooting](#troubleshooting)

---

## Quick Start Commands

### Consumer Tests (Angular/Jest/Pact)

```bash
# Run all consumer contract tests
cd apps/clinical-portal
npm run test:contracts

# Or directly with Jest
npx jest --config jest.pact.config.js --runInBand

# List available contract tests
npx jest --listTests --config=jest.pact.config.js
```

### Provider Verification (Spring Boot/JUnit/Pact)

```bash
cd backend

# Verify Patient Service contracts
./gradlew :modules:services:patient-service:test --tests "*ProviderTest"

# Verify Care Gap Service contracts
./gradlew :modules:services:care-gap-service:test --tests "*ProviderTest"

# Verify specific consumer
./gradlew :modules:services:patient-service:test --tests "*ProviderTest" \
  -Dpact.filter.consumers=ClinicalPortal
```

### OpenAPI Validation

```bash
cd backend

# Run OpenAPI compliance tests for Patient Service
./gradlew :modules:services:patient-service:test --tests "*ComplianceTest"

# Run all OpenAPI validation tests
./gradlew :modules:services:patient-service:test --tests "*OpenApiValidationTest"
```

### Pact Broker

```bash
# Start Pact Broker
docker network create hdim-network 2>/dev/null || true
docker compose -f docker/pact-broker/docker-compose.pact.yml up -d

# Access UI
open http://localhost:9292
# Credentials: hdim / hdimcontract

# Publish pacts
npm run pact:publish

# Stop Pact Broker
docker compose -f docker/pact-broker/docker-compose.pact.yml down
```

---

## Architecture Overview

### Consumer-Driven Contract Testing Flow

```
+------------------+        +----------------+        +------------------+
|  Angular Portal  |  --->  |  Pact Broker   |  <---  |  Java Services   |
|  (Consumer)      |        |  (Contract Hub)|        |  (Providers)     |
+------------------+        +----------------+        +------------------+
       |                           |                          |
       | 1. Define contracts       | 3. Store contracts       | 4. Retrieve contracts
       | 2. Generate pact files    |                          | 5. Verify implementation
       v                           v                          v
+------------------+        +----------------+        +------------------+
| Pact Consumer    |        | Contract       |        | Pact Provider    |
| Tests (Jest)     |        | Repository     |        | Tests (JUnit)    |
+------------------+        +----------------+        +------------------+
       |                                                       |
       | Generate JSON pacts                                   | Verify responses
       v                                                       v
+----------------------------------------------------------------+
|                    CI/CD Pipeline (GitHub Actions)              |
|  1. Consumer tests -> 2. Publish pacts -> 3. Provider verify   |
+----------------------------------------------------------------+
```

### Key Concepts

| Concept | Description |
|---------|-------------|
| **Consumer** | Application that uses an API (Clinical Portal) |
| **Provider** | API service that fulfills requests (Patient Service, Care Gap Service) |
| **Contract** | JSON file describing expected interactions |
| **Provider State** | Test data setup required before verification |
| **Pact Broker** | Central repository for contract storage and verification results |

### Contract Flow

1. **Consumer defines expectations** - Angular tests define what API responses should look like
2. **Pact generates contracts** - JSON files capturing request/response pairs
3. **Contracts published to broker** - Central storage for all teams
4. **Provider verifies contracts** - Spring Boot tests replay requests and validate responses
5. **CI/CD gates merges** - PRs blocked if contracts break

---

## Writing Consumer Tests (Angular)

Consumer tests define the API contract from the frontend perspective using Jest and Pact.

### File Location

```
apps/clinical-portal/
  pact/
    pact-config.ts          # Shared configuration
  src/test/contracts/
    patient-service.consumer.pact.spec.ts
    care-gap-service.consumer.pact.spec.ts
    pact-setup.ts           # Test utilities
```

### Example: Patient Service Consumer Test

```typescript
// apps/clinical-portal/src/test/contracts/patient-service.consumer.pact.spec.ts
import { MatchersV3 } from '@pact-foundation/pact';
import {
  createPactProvider,
  PROVIDER_NAMES,
  TEST_CONSTANTS,
  Matchers,
  COMMON_HEADERS,
} from '../../../pact/pact-config';
import { createFetchTestClient } from './pact-setup';

describe('Patient Service Consumer Contract', () => {
  // Create Pact provider mock
  const provider = createPactProvider(PROVIDER_NAMES.PATIENT_SERVICE);

  describe('GET /api/v1/patients/:id', () => {
    describe('when patient exists', () => {
      it('should return the patient data', async () => {
        // Define expected response structure using matchers
        const expectedPatient = {
          id: Matchers.uuid(TEST_CONSTANTS.PATIENT_JOHN_DOE_ID),
          tenantId: Matchers.nonEmptyString(TEST_CONSTANTS.TENANT_ID),
          firstName: Matchers.nonEmptyString('John'),
          lastName: Matchers.nonEmptyString('Doe'),
          dateOfBirth: Matchers.fhirDate('1980-05-15'),
          gender: Matchers.fhirGender('male'),
          active: Matchers.boolean(true),
        };

        await provider
          .addInteraction()
          // Provider state name - must match @State in provider test
          .given(`patient exists with id ${TEST_CONSTANTS.PATIENT_JOHN_DOE_ID}`)
          .uponReceiving('a request to get patient by ID')
          .withRequest('GET', `/api/v1/patients/${TEST_CONSTANTS.PATIENT_JOHN_DOE_ID}`, (builder) => {
            builder.headers(COMMON_HEADERS.withTenant());
          })
          .willRespondWith(200, (builder) => {
            builder
              .headers({ 'Content-Type': 'application/json' })
              .jsonBody(expectedPatient);
          })
          .executeTest(async (mockServer) => {
            const client = createFetchTestClient(mockServer.url);
            const response = await client.get<Record<string, unknown>>(
              `/api/v1/patients/${TEST_CONSTANTS.PATIENT_JOHN_DOE_ID}`
            );

            // Verify essential fields
            expect(response).toBeDefined();
            expect(response['id']).toBe(TEST_CONSTANTS.PATIENT_JOHN_DOE_ID);
            expect(response['firstName']).toBe('John');
          });
      });
    });

    describe('when patient does not exist', () => {
      it('should return 404 Not Found', async () => {
        const nonExistentId = TEST_CONSTANTS.NON_EXISTENT_PATIENT_ID;

        await provider
          .addInteraction()
          .given(`no patient exists with id ${nonExistentId}`)
          .uponReceiving('a request to get a non-existent patient')
          .withRequest('GET', `/api/v1/patients/${nonExistentId}`, (builder) => {
            builder.headers(COMMON_HEADERS.withTenant());
          })
          .willRespondWith(404, (builder) => {
            builder
              .headers({ 'Content-Type': 'application/json' })
              .jsonBody({
                status: MatchersV3.integer(404),
                error: MatchersV3.string('Not Found'),
                message: MatchersV3.string(`Patient not found with id: ${nonExistentId}`),
              });
          })
          .executeTest(async (mockServer) => {
            const client = createFetchTestClient(mockServer.url);
            try {
              await client.get<unknown>(`/api/v1/patients/${nonExistentId}`);
              fail('Expected 404 error');
            } catch (error) {
              expect((error as Error).message).toContain('404');
            }
          });
      });
    });
  });
});
```

### Key Patterns

**1. Use Shared Configuration**
```typescript
import { createPactProvider, PROVIDER_NAMES, TEST_CONSTANTS, Matchers } from '../../../pact/pact-config';
```

**2. Use FHIR-Compliant Matchers**
```typescript
// UUID format
Matchers.uuid(TEST_CONSTANTS.PATIENT_JOHN_DOE_ID)

// FHIR date (YYYY-MM-DD)
Matchers.fhirDate('1980-05-15')

// FHIR gender enum
Matchers.fhirGender('male')  // matches: male|female|other|unknown

// ISO datetime
Matchers.isoDateTime('2024-01-15T10:30:00Z')
```

**3. Provider State Naming Convention**
```typescript
// Format: "[entity] [condition] [identifier]"
.given('patient exists with id f47ac10b-58cc-4372-a567-0e02b2c3d479')
.given('no patient exists with id 00000000-0000-0000-0000-000000000000')
.given('multiple patients exist')
.given('care gap HBA1C exists')
```

---

## Writing Provider Tests (Spring Boot)

Provider tests verify that the service implementation fulfills consumer contracts.

### File Location

```
backend/modules/services/patient-service/
  src/test/java/com/healthdata/patient/contracts/
    PatientServiceProviderTest.java      # Pact verification
    PatientContractStateSetup.java       # Test data setup
```

### Example: Provider Verification Test

```java
// PatientServiceProviderTest.java
package com.healthdata.patient.contracts;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerConsumerVersionSelectors;
import au.com.dius.pact.provider.junitsupport.loader.SelectorBuilder;
import com.healthdata.contracts.ContractTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Provider("PatientService")  // Must match PROVIDER_NAMES.PATIENT_SERVICE
@ExtendWith(SpringExtension.class)
public class PatientServiceProviderTest extends ContractTestBase {

    @Autowired
    private PatientContractStateSetup stateSetup;

    // Configure which consumer versions to verify
    @PactBrokerConsumerVersionSelectors
    public static SelectorBuilder consumerVersionSelectors() {
        return new SelectorBuilder()
            .mainBranch()           // Production contracts
            .matchingBranch()       // PR branch contracts
            .deployedOrReleased();  // Verified production
    }

    @BeforeEach
    void setupMocks(PactVerificationContext context) {
        super.setupTestTarget(context);
    }

    @AfterEach
    void cleanup() {
        stateSetup.cleanupTestData();
    }

    // Pact verification template - runs for each interaction
    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    // Provider state handlers - must match consumer .given() strings exactly

    @State("patient exists with id f47ac10b-58cc-4372-a567-0e02b2c3d479")
    void patientJohnDoeExists() {
        stateSetup.setupPatientJohnDoe();
    }

    @State("no patient exists with id 00000000-0000-0000-0000-000000000000")
    void noPatientExists() {
        // No setup needed - patient doesn't exist
    }

    @State("multiple patients exist")
    void multiplePatientsExist() {
        stateSetup.setupPatientJohnDoe();
        stateSetup.setupPatientJaneSmith();
    }

    @State("patient exists with MRN MRN-12345")
    void patientExistsWithMrn() {
        stateSetup.setupPatientJohnDoe();
    }
}
```

### Example: State Setup Class

```java
// PatientContractStateSetup.java
package com.healthdata.patient.contracts;

import com.healthdata.patient.entity.PatientDemographicsEntity;
import com.healthdata.patient.repository.PatientDemographicsRepository;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class PatientContractStateSetup {

    public static final String TEST_TENANT_ID = "test-tenant-contracts";
    public static final String PATIENT_JOHN_DOE_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
    public static final String PATIENT_JANE_SMITH_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
    public static final String TEST_MRN_JOHN_DOE = "MRN-12345";

    private final PatientDemographicsRepository repository;

    public PatientContractStateSetup(PatientDemographicsRepository repository) {
        this.repository = repository;
    }

    public void setupPatientJohnDoe() {
        PatientDemographicsEntity patient = PatientDemographicsEntity.builder()
            .id(UUID.fromString(PATIENT_JOHN_DOE_ID))
            .tenantId(TEST_TENANT_ID)
            .fhirPatientId(PATIENT_JOHN_DOE_ID)
            .mrn(TEST_MRN_JOHN_DOE)
            .firstName("John")
            .lastName("Doe")
            .dateOfBirth(LocalDate.of(1980, 5, 15))
            .gender("male")
            .active(true)
            .email("john.doe@example.com")
            .build();
        repository.save(patient);
    }

    public void setupPatientJaneSmith() {
        PatientDemographicsEntity patient = PatientDemographicsEntity.builder()
            .id(UUID.fromString(PATIENT_JANE_SMITH_ID))
            .tenantId(TEST_TENANT_ID)
            .fhirPatientId(PATIENT_JANE_SMITH_ID)
            .mrn("MRN-67890")
            .firstName("Jane")
            .lastName("Smith")
            .dateOfBirth(LocalDate.of(1975, 8, 20))
            .gender("female")
            .active(true)
            .build();
        repository.save(patient);
    }

    public void cleanupTestData() {
        repository.deleteAllByTenantId(TEST_TENANT_ID);
    }
}
```

### Base Class: ContractTestBase

```java
// backend/modules/shared/contract-testing/src/main/java/com/healthdata/contracts/ContractTestBase.java
@Tag("contract")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@PactBroker(
    url = "${pact.broker.url:http://localhost:9292}",
    authentication = @PactBrokerAuth(
        username = "${pact.broker.username:hdim}",
        password = "${pact.broker.password:hdimcontract}"
    )
)
public abstract class ContractTestBase {

    @LocalServerPort
    protected int port;

    @BeforeEach
    protected void setupTestTarget(PactVerificationContext context) {
        if (context != null) {
            context.setTarget(new HttpTestTarget("localhost", port));
        }
    }

    protected String getTestTenantId() {
        return "test-tenant-contracts";
    }
}
```

---

## Pact Broker Setup

### Starting the Broker

```bash
# Create network (if not exists)
docker network create hdim-network 2>/dev/null || true

# Start Pact Broker and PostgreSQL
docker compose -f docker/pact-broker/docker-compose.pact.yml up -d

# Check status
docker compose -f docker/pact-broker/docker-compose.pact.yml ps

# View logs
docker compose -f docker/pact-broker/docker-compose.pact.yml logs -f pact-broker
```

### Environment Variables

```bash
# docker/pact-broker/.env.example
PACT_BROKER_USERNAME=hdim
PACT_BROKER_PASSWORD=hdimcontract
PACT_BROKER_DB_PASSWORD=pactpassword
PACT_BROKER_BASE_URL=http://localhost:9292
```

### Accessing the UI

- **URL:** http://localhost:9292
- **Username:** hdim
- **Password:** hdimcontract

### Publishing Contracts

**From Angular (npm):**
```bash
cd apps/clinical-portal
npm run test:contracts  # Generate pacts
npm run pact:publish    # Publish to broker
```

**CI/CD automatically publishes** when pushing to `master` or `develop`.

### Docker Compose Configuration

```yaml
# docker/pact-broker/docker-compose.pact.yml
services:
  pact-broker:
    image: pactfoundation/pact-broker:latest
    ports:
      - "9292:9292"
    environment:
      PACT_BROKER_DATABASE_URL: "postgres://pact:${PACT_BROKER_DB_PASSWORD}@pact-db/pact_broker"
      PACT_BROKER_BASIC_AUTH_USERNAME: "${PACT_BROKER_USERNAME:-hdim}"
      PACT_BROKER_BASIC_AUTH_PASSWORD: "${PACT_BROKER_PASSWORD:-hdimcontract}"
    depends_on:
      pact-db:
        condition: service_healthy

  pact-db:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: pact
      POSTGRES_PASSWORD: "${PACT_BROKER_DB_PASSWORD:-pactpassword}"
      POSTGRES_DB: pact_broker
    volumes:
      - pact-db-data:/var/lib/postgresql/data
```

---

## OpenAPI Validation

OpenAPI validation ensures API responses conform to documented specifications.

### Adding Compliance Tests

**1. Extend OpenApiComplianceTestBase:**

```java
// PatientApiComplianceTest.java
package com.healthdata.patient.api;

import com.healthdata.openapi.OpenApiComplianceTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@WithMockUser(roles = {"ADMIN"})
@DisplayName("Patient API OpenAPI Compliance")
class PatientApiComplianceTest extends OpenApiComplianceTestBase {

    private static final String TENANT_ID = "test-tenant";

    @Test
    @DisplayName("GET /api/v1/patients should match OpenAPI spec")
    void listPatients_shouldReturn200WithPatients() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/patients")
                .header("X-Tenant-ID", TENANT_ID)
                .param("page", "0")
                .param("size", "20")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        // Validate response against OpenAPI spec
        assertResponseMatchesSpec(result);
    }

    @Test
    @DisplayName("GET /patient/health-record should match OpenAPI spec")
    void getHealthRecord_shouldReturn200() throws Exception {
        MvcResult result = mockMvc.perform(get("/patient/health-record")
                .header("X-Tenant-ID", TENANT_ID)
                .param("patient", "patient-123")
                .accept("application/fhir+json"))
            .andExpect(status().isOk())
            .andReturn();

        assertResponseMatchesSpec(result);
    }
}
```

**2. Validation Methods:**

```java
// Assert response matches spec
assertResponseMatchesSpec(result);

// Assert request matches spec
assertRequestMatchesSpec(result);

// Assert both request and response match
assertRequestAndResponseMatchSpec(result);
```

### Base Class: OpenApiComplianceTestBase

```java
// backend/modules/shared/openapi-validation/src/main/java/com/healthdata/openapi/OpenApiComplianceTestBase.java
@Tag("integration")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class OpenApiComplianceTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @LocalServerPort
    protected int port;

    protected OpenApiValidator validator;

    @BeforeEach
    void setUpOpenApiValidator() {
        String specUrl = String.format("http://localhost:%d/v3/api-docs", port);
        this.validator = OpenApiValidator.forSpecUrl(specUrl);
    }

    protected void assertResponseMatchesSpec(MvcResult result) {
        ValidationReport report = validator.validate(
            result.getRequest(),
            result.getResponse()
        );
        assertFalse(report.hasErrors(),
            () -> "Response does not match spec: " + OpenApiValidator.formatErrors(report));
    }
}
```

### Adding OpenAPI Validation to a Service

1. Add dependency in `build.gradle.kts`:
```kotlin
dependencies {
    testImplementation(project(":modules:shared:openapi-validation"))
}
```

2. Create compliance test class extending `OpenApiComplianceTestBase`

3. Write tests for each endpoint

---

## CI/CD Integration

### Workflow Overview

The contract testing workflow (`.github/workflows/contract-testing.yml`) runs on:
- Push to `master` or `develop`
- Pull requests to `master` or `develop`
- Manual trigger

### Jobs

| Job | Description | Duration |
|-----|-------------|----------|
| `consumer-contracts` | Run Angular Pact tests, upload pacts | ~5 min |
| `provider-verification` | Verify services against pacts | ~10 min |
| `openapi-validation` | Validate OpenAPI compliance | ~8 min |
| `contract-gate` | Merge gate - blocks if any job fails | ~1 min |

### Workflow Diagram

```
+-------------------+
|  consumer-contracts|
|  (Angular/Jest)   |
+---------+---------+
          |
          | Upload pact files
          v
+---------+---------+
| provider-verification|
| (matrix: patient,    |
|  care-gap services)  |
+---------+---------+
          |
          | parallel
          v
+---------+---------+
| openapi-validation |
| (matrix: patient)  |
+---------+---------+
          |
          v
+---------+---------+
|   contract-gate   |
|  (merge blocker)  |
+-------------------+
```

### PR Blocking

The `contract-gate` job is a **required status check**. PRs cannot merge if:
- Consumer contract tests fail
- Provider verification fails
- OpenAPI validation fails

### Manual Trigger

```bash
# Trigger workflow with pact publishing
gh workflow run contract-testing.yml --field publish_pacts=true
```

---

## Test Constants Reference

These constants must remain synchronized between consumer and provider tests.

### Tenant

| Constant | Value |
|----------|-------|
| Test Tenant ID | `test-tenant-contracts` |

### Patient IDs

| Constant | Value | Description |
|----------|-------|-------------|
| John Doe ID | `f47ac10b-58cc-4372-a567-0e02b2c3d479` | Primary test patient |
| Jane Smith ID | `a1b2c3d4-e5f6-7890-abcd-ef1234567890` | Secondary test patient |
| Non-existent ID | `00000000-0000-0000-0000-000000000000` | For 404 tests |

### Care Gap IDs

| Constant | Value | Description |
|----------|-------|-------------|
| HBA1C Gap ID | `550e8400-e29b-41d4-a716-446655440001` | Hemoglobin A1c test gap |
| BCS Gap ID | `550e8400-e29b-41d4-a716-446655440002` | Breast Cancer Screening gap |
| COL Gap ID | `550e8400-e29b-41d4-a716-446655440003` | Colorectal Cancer Screening gap |

### MRNs

| Constant | Value |
|----------|-------|
| John Doe MRN | `MRN-12345` |
| Jane Smith MRN | `MRN-67890` |

### Provider/Consumer Names

| Role | Name |
|------|------|
| Consumer | `ClinicalPortal` |
| Provider | `PatientService` |
| Provider | `CareGapService` |
| Provider | `QualityMeasureService` |

---

## Troubleshooting

### Common Issues

#### Consumer Tests Won't Run

```bash
# Check Jest can find tests
cd apps/clinical-portal
npx jest --listTests --config=jest.pact.config.js

# Ensure Pact dependencies are installed
npm install

# Run with verbose logging
npx jest --config jest.pact.config.js --runInBand --verbose
```

#### Provider State Not Found

**Error:** `No matching state handler found for "patient exists with id xyz"`

**Fix:** Ensure `@State` annotation string **exactly matches** consumer `.given()` string.

```java
// Consumer:
.given('patient exists with id f47ac10b-58cc-4372-a567-0e02b2c3d479')

// Provider (must match exactly):
@State("patient exists with id f47ac10b-58cc-4372-a567-0e02b2c3d479")
```

#### Pact Broker Connection Failed

```bash
# Check broker is running
docker compose -f docker/pact-broker/docker-compose.pact.yml ps

# Verify credentials
curl -u hdim:hdimcontract http://localhost:9292/diagnostic/status/heartbeat

# Check logs
docker compose -f docker/pact-broker/docker-compose.pact.yml logs pact-broker
```

#### OpenAPI Validation Failures

**Error:** `Response for GET /api/v1/patients does not match OpenAPI spec`

**Debug:**
1. Check the OpenAPI spec at `http://localhost:8084/v3/api-docs`
2. Compare actual response with spec definition
3. Common issues:
   - Missing required fields
   - Wrong field types
   - Different field names (camelCase vs snake_case)

#### Verification Timeout

```bash
# Increase timeout in provider test
@TestTemplate
@Timeout(value = 60, unit = TimeUnit.SECONDS)
void verifyPact(PactVerificationContext context) { ... }
```

### Debug Commands

```bash
# View generated pact files
cat apps/clinical-portal/pacts/ClinicalPortal-PatientService.json | jq .

# Check contract-testing module compiles
cd backend && ./gradlew :modules:shared:contract-testing:compileJava

# Check openapi-validation module compiles
cd backend && ./gradlew :modules:shared:openapi-validation:compileJava

# Run provider tests with debug logging
./gradlew :modules:services:patient-service:test --tests "*ProviderTest" --debug
```

### Getting Help

- **Contract Testing Concepts:** [Pact Documentation](https://docs.pact.io/)
- **OpenAPI Validation:** [Atlassian Swagger Request Validator](https://bitbucket.org/atlassian/swagger-request-validator)
- **HDIM Issues:** Create issue in repository with `contract-testing` label

---

## Coverage Summary

### Services with Contract Tests

| Service | Consumer Tests | Provider Tests | OpenAPI Validation |
|---------|---------------|----------------|-------------------|
| Patient Service | Yes | Yes | Yes |
| Care Gap Service | Yes | Yes | Planned |
| Quality Measure Service | Planned | Planned | Planned |
| FHIR Service | Planned | Planned | Planned |

### Next Steps

1. Add Quality Measure Service contracts
2. Add FHIR Service contracts
3. Expand OpenAPI validation to all services
4. Add contract tests for mutation operations (POST, PUT, DELETE)

---

*Last Updated: February 2026*
*Version: 1.0 - Phase 1 Contract Testing Implementation*
