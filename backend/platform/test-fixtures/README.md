# HDIM Test Fixtures Module

**Purpose**: Shared test utilities and fixtures for consistent, HIPAA-compliant testing across all 28 HDIM microservices.
**Version**: 1.0
**Last Updated**: December 31, 2025

---

## Overview

The `platform:test-fixtures` module provides reusable test utilities that eliminate duplication across service test suites and enforce consistent testing patterns. This module is designed to be imported as a `testImplementation` dependency in any HDIM backend service.

### Key Components

| Component | Package | Purpose |
|-----------|---------|---------|
| `BaseTestContainersConfiguration` | `containers` | Shared TestContainers for PostgreSQL, Redis, Kafka |
| `SyntheticDataGenerator` | `data` | HIPAA-compliant synthetic FHIR resource generators |
| `GatewayTrustTestHeaders` | `security` | Gateway Trust authentication header utilities |

---

## Installation

Add to your service's `build.gradle.kts`:

```kotlin
dependencies {
    testImplementation(projects.platform.testFixtures)
}
```

---

## Usage

### TestContainers Configuration

```java
import com.healthdata.testfixtures.containers.BaseTestContainersConfiguration;

@SpringBootTest
@Import(BaseTestContainersConfiguration.class)
@Testcontainers
class MyIntegrationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configure all infrastructure (PostgreSQL, Redis, Kafka)
        BaseTestContainersConfiguration.configureAll(registry);

        // Or configure individually
        BaseTestContainersConfiguration.configurePostgres(registry);
        BaseTestContainersConfiguration.configureRedis(registry);
        BaseTestContainersConfiguration.configureKafka(registry);
    }

    @Test
    void shouldIntegrateWithDatabase() {
        // Test with real PostgreSQL container
    }
}
```

### Synthetic Data Generation

```java
import com.healthdata.testfixtures.data.SyntheticDataGenerator;

class PatientServiceTest {

    @Test
    void shouldCreatePatient() {
        // Create a synthetic patient - clearly marked as test data
        Patient patient = SyntheticDataGenerator.createPatient("tenant-001");

        // MRN follows synthetic pattern: TEST-MRN-XXXXXX
        assertThat(patient.getIdentifierFirstRep().getValue())
            .startsWith("TEST-MRN-");

        // Name follows synthetic pattern: Test-Patient-xxx
        assertThat(patient.getNameFirstRep().getGivenAsSingleString())
            .startsWith("Test-");
    }

    @Test
    void shouldCreateRelatedResources() {
        String tenantId = "tenant-001";
        Patient patient = SyntheticDataGenerator.createPatient(tenantId);

        // Create related FHIR resources
        Observation bp = SyntheticDataGenerator.createBloodPressureObservation(tenantId, patient);
        Observation hba1c = SyntheticDataGenerator.createHbA1cObservation(tenantId, patient);
        Condition diabetes = SyntheticDataGenerator.createDiabetesCondition(tenantId, patient);

        // All resources include tenant context
        assertThat(bp.getMeta().getTag()).anyMatch(tag ->
            tag.getSystem().equals("urn:hdim:tenant") && tag.getCode().equals(tenantId));
    }
}
```

### Gateway Trust Headers

```java
import com.healthdata.testfixtures.security.GatewayTrustTestHeaders;

class MeasureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminCanCreateMeasure() throws Exception {
        mockMvc.perform(post("/api/v1/measures")
            .headers(GatewayTrustTestHeaders.adminHeaders("tenant-001"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{...}"))
            .andExpect(status().isCreated());
    }

    @Test
    void viewerCannotCreateMeasure() throws Exception {
        mockMvc.perform(post("/api/v1/measures")
            .headers(GatewayTrustTestHeaders.viewerHeaders("tenant-001"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{...}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void customRoleConfiguration() throws Exception {
        mockMvc.perform(get("/api/v1/measures")
            .headers(GatewayTrustTestHeaders.builder()
                .tenantId("tenant-001")
                .userId("user-123")
                .username("custom.user@test.hdim.io")
                .roles("EVALUATOR", "ANALYST")
                .tenantIds("tenant-001", "tenant-002") // Multi-tenant access
                .build()))
            .andExpect(status().isOk());
    }
}
```

---

## HIPAA Compliance

### Synthetic Data Patterns

All generated data uses clearly synthetic patterns that cannot be confused with real PHI:

| Data Type | Pattern | Example |
|-----------|---------|---------|
| MRN | `TEST-MRN-XXXXXX` | `TEST-MRN-100042` |
| Patient Name | `Test-Patient-xxx Suffix-xxxxxx` | `Test-Patient Chen-100001` |
| User ID | `user-test-XXXXXX` | `user-test-100001` |
| Tenant ID | `tenant-test-XXX` | `tenant-test-001` |

### Container Isolation

TestContainers are ephemeral - all data is destroyed when tests complete:
- No persistent storage
- Fresh database for each test run
- Automatic cleanup on JVM exit

### Cache TTL Verification

Use Redis container for HIPAA-compliant cache TTL testing:

```java
@Test
void phiCacheTtlShouldNotExceedFiveMinutes() {
    RedisContainer redis = BaseTestContainersConfiguration.getRedisContainer();

    // Verify PHI cache TTL configuration
    try (Jedis jedis = new Jedis(redis.getHost(), redis.getMappedPort(6379))) {
        jedis.setex("phi:patient:123", 300, "test-data");
        Long ttl = jedis.ttl("phi:patient:123");

        assertThat(ttl)
            .isLessThanOrEqualTo(300L)
            .withFailMessage("PHI cache TTL exceeds 5 minutes (HIPAA violation)");
    }
}
```

---

## Gateway Trust Headers Reference

### Available Header Methods

| Method | Role(s) | Use Case |
|--------|---------|----------|
| `superAdminHeaders()` | SUPER_ADMIN, ADMIN, EVALUATOR, ANALYST, VIEWER | System-wide administrative operations |
| `adminHeaders()` | ADMIN, EVALUATOR, ANALYST, VIEWER | Tenant-level administration |
| `evaluatorHeaders()` | EVALUATOR, ANALYST, VIEWER | Running quality measure evaluations |
| `analystHeaders()` | ANALYST, VIEWER | Viewing reports and analytics |
| `viewerHeaders()` | VIEWER | Read-only access |
| `unauthenticatedHeaders()` | None | Testing authentication enforcement |

### Header Names

| Header | Description |
|--------|-------------|
| `X-Tenant-ID` | Current tenant context for the request |
| `X-Auth-User-Id` | User's UUID |
| `X-Auth-Username` | User's login name |
| `X-Auth-Tenant-Ids` | Comma-separated list of authorized tenants |
| `X-Auth-Roles` | Comma-separated list of user roles |
| `X-Auth-Validated` | HMAC signature (production only) |

---

## Testing Patterns

### Multi-Tenant Isolation

```java
@Test
void shouldIsolateTenantData() {
    String tenant1 = "tenant-001";
    String tenant2 = "tenant-002";

    Patient patient1 = SyntheticDataGenerator.createPatient(tenant1);
    Patient patient2 = SyntheticDataGenerator.createPatient(tenant2);

    patientRepository.save(patient1);
    patientRepository.save(patient2);

    // Verify tenant isolation
    List<Patient> tenant1Patients = patientRepository.findByTenantId(tenant1);
    assertThat(tenant1Patients)
        .contains(patient1)
        .doesNotContain(patient2);
}
```

### RBAC Testing Matrix

```java
@ParameterizedTest
@CsvSource({
    "ADMIN, POST, /api/v1/measures, 201",
    "EVALUATOR, POST, /api/v1/measures, 403",
    "VIEWER, POST, /api/v1/measures, 403",
    "ADMIN, GET, /api/v1/measures, 200",
    "EVALUATOR, GET, /api/v1/measures, 200",
    "VIEWER, GET, /api/v1/measures, 200"
})
void shouldEnforceRbac(String role, String method, String path, int expectedStatus) throws Exception {
    HttpHeaders headers = switch (role) {
        case "ADMIN" -> GatewayTrustTestHeaders.adminHeaders("tenant-001");
        case "EVALUATOR" -> GatewayTrustTestHeaders.evaluatorHeaders("tenant-001");
        case "VIEWER" -> GatewayTrustTestHeaders.viewerHeaders("tenant-001");
        default -> throw new IllegalArgumentException("Unknown role: " + role);
    };

    mockMvc.perform(request(HttpMethod.valueOf(method), path)
        .headers(headers))
        .andExpect(status().is(expectedStatus));
}
```

---

## Module Dependencies

```
platform:test-fixtures
├── spring-boot-starter-test
├── spring-boot-starter-web
├── testcontainers (PostgreSQL, Redis, Kafka)
├── junit-jupiter
├── mockito-core
├── assertj-core
├── hapi-fhir-structures-r4
└── commons-lang3
```

---

## References

- [HIPAA Cache Compliance](/backend/HIPAA-CACHE-COMPLIANCE.md)
- [Gateway Trust Architecture](/backend/docs/GATEWAY_TRUST_ARCHITECTURE.md)
- [CLAUDE.md](/CLAUDE.md) - Project conventions
- [Testing Documentation in Service READMEs](/backend/modules/services/*/README.md)

---

## Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-12-31 | Engineering | Initial creation with TestContainers, SyntheticDataGenerator, GatewayTrustTestHeaders |

---

*This module is the authoritative source for shared test utilities in HDIM.*
