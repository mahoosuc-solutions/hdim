# Test Infrastructure Module

Shared test infrastructure for HDIM services providing Testcontainers, base test classes, and test utilities.

## Overview

This module provides:
- **Singleton Testcontainers** - Shared Kafka, PostgreSQL, Redis containers
- **Base Test Annotations** - Standardized test class annotations
- **Audit Test Utilities** - Event verification and capture utilities
- **Test Data Builders** - Builders for creating test data
- **Test Configurations** - Shared test configurations

## Quick Start

### Add Dependency

Add to your service's `build.gradle.kts`:

```kotlin
dependencies {
    testImplementation(project(":modules:shared:test-infrastructure"))
}
```

### Use Base Test Annotations

#### Unit Tests (Lightweight)

```java
@BaseUnitTest
class MyServiceTest {
    @Mock
    private ExternalService externalService;
    
    @InjectMocks
    private MyService myService;
    
    @Test
    void shouldDoSomething() {
        // Fast, isolated test with mocks
    }
}
```

#### Integration Tests

```java
@BaseIntegrationTest
class MyServiceIntegrationTest {
    @Autowired
    private MyService myService;
    
    @MockBean
    private ExternalService externalService;
    
    @Test
    void shouldIntegrateWithDatabase() {
        // Test with Spring context
    }
}
```

#### Heavyweight Tests (Testcontainers)

```java
@BaseHeavyweightTest
class MyServiceHeavyweightTest {
    @Container
    static KafkaContainer kafka = SharedKafkaContainer.getInstance();
    
    @DynamicPropertySource
    static void configureKafka(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", 
            SharedKafkaContainer::getBootstrapServers);
    }
    
    @Test
    void shouldPublishToRealKafka() {
        // Test with real Kafka
    }
}
```

## Shared Containers

### Kafka

```java
// Get singleton instance
KafkaContainer kafka = SharedKafkaContainer.getInstance();

// Get bootstrap servers
String bootstrapServers = SharedKafkaContainer.getBootstrapServers();
```

### PostgreSQL

```java
// Get singleton instance
PostgreSQLContainer<?> postgres = SharedPostgresContainer.getInstance();

// Get connection details
String jdbcUrl = SharedPostgresContainer.getJdbcUrl();
String username = SharedPostgresContainer.getUsername();
String password = SharedPostgresContainer.getPassword();
```

### Redis

```java
// Get singleton instance
RedisContainer redis = SharedRedisContainer.getInstance();

// Get connection details
String host = SharedRedisContainer.getHost();
Integer port = SharedRedisContainer.getPort();
```

## Audit Test Utilities

### AuditEventVerifier

For heavyweight tests with real Kafka:

```java
@Test
void shouldPublishAuditEvent() {
    // Setup consumer
    KafkaConsumer<String, String> consumer = createConsumer();
    AuditEventVerifier verifier = new AuditEventVerifier(consumer, "ai.agent.decisions");
    
    // Trigger event
    service.doSomething();
    
    // Wait and verify
    ConsumerRecord<String, String> record = verifier.waitForEvent(
        r -> r.key().contains("tenant-123")
    );
    
    AuditEventVerifier.verifyPartitionKey(record, "tenant-123", "agent-id");
    verifier.close();
}
```

### AuditEventCaptor

For unit tests with mocked publisher:

```java
@Test
void shouldPublishEvent() {
    // Setup
    @Mock
    private AIAuditEventPublisher publisher;
    
    @Captor
    private ArgumentCaptor<AIAgentDecisionEvent> eventCaptor;
    
    // When
    service.doSomething();
    
    // Then
    verify(publisher).publishAIDecision(eventCaptor.capture());
    AIAgentDecisionEvent event = eventCaptor.getValue();
    
    AuditEventCaptor.verifyAgentId(event, "expected-agent-id");
    AuditEventCaptor.verifyTenantId(event, "tenant-123");
    AuditEventCaptor.verifyRequiredFields(event);
}
```

## Test Data Builders

### AuditEventBuilder

```java
AIAgentDecisionEvent event = AuditEventBuilder.create()
    .tenantId("tenant-123")
    .agentId("test-agent")
    .agentType(AIAgentDecisionEvent.AgentType.CQL_ENGINE)
    .decisionType(AIAgentDecisionEvent.DecisionType.MEASURE_MET)
    .resourceId("patient-123")
    .confidenceScore(0.95)
    .reasoning("Test reasoning")
    .inputMetric("key", "value")
    .build();
```

## Test Configurations

### TestContainersConfig

Automatically configures all containers:

```java
@SpringBootTest
@Import(TestContainersConfig.class)
class MyHeavyweightTest {
    // Kafka, PostgreSQL, Redis automatically configured
}
```

### MockAuditConfig

Mocks audit components for unit tests:

```java
@SpringBootTest
@Import(MockAuditConfig.class)
class MyUnitTest {
    @Autowired
    private AIAuditEventPublisher publisher; // Mocked
}
```

## Test Naming Conventions

| Pattern | Type | Characteristics |
|---------|------|-----------------|
| `*Test.java` | Unit | Fast, mocked, no external dependencies |
| `*IntegrationTest.java` | Integration | Spring context, may use mocks |
| `*HeavyweightTest.java` | Heavyweight | Testcontainers, real infrastructure |
| `*E2ETest.java` | End-to-end | Full pipeline verification |

## Performance

- **Singleton containers** - Start once, reuse across all tests
- **Container reuse** - Containers persist between test runs (requires Testcontainers 1.20+)
- **Parallel execution** - Tests can run in parallel with shared containers

## Best Practices

1. **Use appropriate test type**
   - Unit tests for business logic
   - Integration tests for Spring components
   - Heavyweight tests for infrastructure integration

2. **Leverage shared containers**
   - Don't create new containers in each test
   - Use singleton instances for better performance

3. **Clean up after tests**
   - Use `@Transactional` for database tests
   - Reset mocks between tests
   - Close consumers/producers

4. **Verify audit events**
   - Always verify agentId, tenantId, decisionType
   - Check partition keys for Kafka
   - Verify event ordering when needed

## Examples

See test classes in:
- `care-gap-service/src/test/java/com/healthdata/caregap/service/`
- `cql-engine-service/src/test/java/com/healthdata/cql/service/`

## Contributing

When adding new test utilities:
1. Follow existing patterns
2. Add comprehensive JavaDoc
3. Provide usage examples
4. Update this README

