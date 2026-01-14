# Testing Architecture - Lightweight and Heavyweight Tests

## Overview

All services follow a consistent testing pattern with two categories of tests:

1. **Lightweight Tests** - Fast, isolated unit tests with mocks
2. **Heavyweight Tests** - Full integration tests with real infrastructure (Docker/Testcontainers)

## Test Categories

### Lightweight Tests (Unit Tests)

**Characteristics:**
- ✅ Fast execution (< 1 second per test)
- ✅ No external dependencies (Docker, databases, Kafka)
- ✅ Use mocks for external services
- ✅ Isolated and deterministic
- ✅ Run on every build

**Naming Convention:**
- `*Test.java` - Standard unit tests
- Example: `CareGapAuditIntegrationTest.java`

**Configuration:**
- Use `@ExtendWith(MockitoExtension.class)` for mocking
- Mock external dependencies with `@Mock`
- No Spring context loading

**Example:**
```java
@ExtendWith(MockitoExtension.class)
class CareGapAuditIntegrationTest {
    @Mock
    private AIAuditEventPublisher auditPublisher;
    
    @Test
    void shouldPublishEventWithAgentId() {
        // Test with mocked publisher
    }
}
```

### Heavyweight Tests (Integration Tests)

**Characteristics:**
- ⚠️ Slower execution (requires Docker/Testcontainers)
- ✅ Real infrastructure (Kafka, PostgreSQL, Redis)
- ✅ End-to-end verification
- ✅ Run in CI/CD or before releases
- ✅ Requires Docker to be running

**Naming Convention:**
- `*HeavyweightTest.java` - Integration tests with real infrastructure
- `*IntegrationTest.java` - Integration tests (may use mocks)
- Example: `CareGapAuditIntegrationHeavyweightTest.java`

**Configuration:**
- Use `@SpringBootTest` for full context
- Use `@Testcontainers` for Docker containers
- Use `@BaseIntegrationTest` annotation (if available)
- Configure via `@DynamicPropertySource`

**Example:**
```java
@BaseIntegrationTest
@Testcontainers
class CareGapAuditIntegrationHeavyweightTest {
    @Container
    static KafkaContainer kafka = new KafkaContainer(...);
    
    @Test
    void shouldPublishEventToKafka() {
        // Test with real Kafka
    }
}
```

## Test Organization

### Directory Structure

```
src/test/java/com/healthdata/{service}/
├── service/
│   ├── ServiceTest.java              # Lightweight unit tests
│   └── ServiceHeavyweightTest.java   # Heavyweight integration tests
├── controller/
│   ├── ControllerTest.java           # Lightweight API tests (MockMvc)
│   └── ControllerIntegrationTest.java # Heavyweight API tests (TestRestTemplate)
├── config/
│   ├── BaseIntegrationTest.java      # Base annotation for integration tests
│   └── TestSecurityConfiguration.java # Test configuration with mocks
└── integration/
    └── EndToEndTest.java             # Full end-to-end tests
```

### Test Execution

**Lightweight Tests (Default):**
```bash
# Run all lightweight tests (default)
./gradlew test

# Run specific lightweight test
./gradlew test --tests "*Test"
```

**Heavyweight Tests:**
```bash
# Run heavyweight tests (requires Docker)
./gradlew test --tests "*HeavyweightTest"

# Run all integration tests
./gradlew test --tests "*IntegrationTest"
```

## Service-Specific Patterns

### Care Gap Service

**Lightweight:**
- `CareGapAuditIntegrationTest.java` - Unit tests with mocked publisher
- `CareGapIdentificationServiceTest.java` - Service unit tests

**Heavyweight:**
- `CareGapAuditIntegrationHeavyweightTest.java` - Kafka integration tests
- `CareGapDetectionE2ETest.java` - End-to-end tests

### CQL Engine Service

**Lightweight:**
- `CqlAuditIntegrationTest.java` - Unit tests with mocked publisher
- `CqlEvaluationServiceTest.java` - Service unit tests

**Heavyweight:**
- `CqlAuditIntegrationHeavyweightTest.java` - Kafka integration tests
- `CqlEvaluationControllerIntegrationTest.java` - API integration tests

## Test Configuration Patterns

### Base Integration Test Annotation

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import({TestSecurityConfiguration.class, TestCacheConfiguration.class})
public @interface BaseIntegrationTest {
}
```

### Test Security Configuration

```java
@TestConfiguration
@Profile("test")
public class TestSecurityConfiguration {
    @MockBean
    private JwtTokenService jwtTokenService;
    
    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;
}
```

### Testcontainers Setup

```java
@Testcontainers
class MyHeavyweightTest {
    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.8.0"));
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"));
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
    }
}
```

## Best Practices

### 1. Test Separation

- ✅ Keep lightweight and heavyweight tests in separate files
- ✅ Use clear naming conventions
- ✅ Document which tests require Docker

### 2. Mock Strategy

- ✅ Lightweight: Mock all external dependencies
- ✅ Heavyweight: Use real infrastructure via Testcontainers
- ✅ Never mix mocks and real infrastructure in the same test

### 3. Test Data

- ✅ Lightweight: Use simple test data
- ✅ Heavyweight: Use realistic test data
- ✅ Clean up test data after tests

### 4. Performance

- ✅ Lightweight tests should run in < 1 second
- ✅ Heavyweight tests may take 10-30 seconds
- ✅ Use `@Transactional` for database cleanup in integration tests

### 5. CI/CD Integration

- ✅ Run lightweight tests on every commit
- ✅ Run heavyweight tests in CI/CD pipeline
- ✅ Skip heavyweight tests if Docker is not available

## Gradle Configuration

### Test Tasks

```kotlin
// Lightweight tests (default)
tasks.test {
    useJUnitPlatform()
    exclude("**/*HeavyweightTest.class")
}

// Heavyweight tests (separate task)
tasks.register<Test>("heavyweightTest") {
    useJUnitPlatform()
    include("**/*HeavyweightTest.class")
    systemProperty("testcontainers.reuse.enable", "true")
}
```

### Test Dependencies

```kotlin
dependencies {
    // Lightweight testing
    testImplementation(libs.bundles.testing)  // JUnit, Mockito, AssertJ
    
    // Heavyweight testing
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.testcontainers.junit.jupiter)
}
```

## Migration Guide

### Converting Existing Tests

1. **Identify test type:**
   - If it uses mocks → Lightweight
   - If it uses real infrastructure → Heavyweight

2. **Rename files:**
   - Lightweight: `*Test.java`
   - Heavyweight: `*HeavyweightTest.java` or `*IntegrationTest.java`

3. **Update configuration:**
   - Lightweight: Use `@ExtendWith(MockitoExtension.class)`
   - Heavyweight: Use `@BaseIntegrationTest` or `@SpringBootTest` with `@Testcontainers`

4. **Update documentation:**
   - Document which tests require Docker
   - Add to CI/CD pipeline if needed

## Examples

### Lightweight Test Example

```java
@ExtendWith(MockitoExtension.class)
class CareGapAuditIntegrationTest {
    @Mock
    private AIAuditEventPublisher auditPublisher;
    
    @Test
    void shouldPublishEventWithAgentId() {
        // Arrange
        when(auditPublisher.publishAIDecision(any()))
            .thenReturn(CompletableFuture.completedFuture(null));
        
        // Act
        auditIntegration.publishCareGapIdentificationEvent(...);
        
        // Assert
        verify(auditPublisher).publishAIDecision(argThat(event -> 
            event.getAgentId().equals("care-gap-identifier")));
    }
}
```

### Heavyweight Test Example

```java
@BaseIntegrationTest
@Testcontainers
class CareGapAuditIntegrationHeavyweightTest {
    @Container
    static KafkaContainer kafka = new KafkaContainer(...);
    
    @Autowired
    private CareGapAuditIntegration auditIntegration;
    
    @Test
    void shouldPublishEventToKafka() {
        // Act
        auditIntegration.publishCareGapIdentificationEvent(...);
        
        // Assert - verify event in Kafka
        ConsumerRecord<String, String> record = consumeFromKafka();
        assertThat(record.key()).isEqualTo("tenant-123:care-gap-identifier");
    }
}
```

## Summary

- **Lightweight Tests**: Fast, isolated, use mocks, run on every build
- **Heavyweight Tests**: Slower, use real infrastructure, run in CI/CD
- **Clear Separation**: Different files, naming conventions, and execution paths
- **Consistent Pattern**: Same approach across all services

