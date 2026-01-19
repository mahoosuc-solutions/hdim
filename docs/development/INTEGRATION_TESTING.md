# INTEGRATION TESTING GUIDE

Comprehensive integration testing standards for HDIM microservices using Testcontainers, real databases, and message queues.

**Last Updated**: January 19, 2026
**Status**: Phase 2, P1 Critical Guide
**Coverage**: Testcontainers, Database testing, Kafka testing, Contract testing basics, Async testing

---

## Overview

Integration tests validate that multiple components work together correctly. They use real infrastructure (PostgreSQL, Redis, Kafka) via **Testcontainers** instead of mocks, ensuring tests exercise actual service behavior.

### Testing Pyramid Reminder

```
Unit Tests (60-70%) → Fast, isolated, mocked dependencies
Integration Tests (20-30%) → Real containers, realistic scenarios
E2E Tests (5-10%) → Full workflows through user interface
```

### Integration Test Coverage

| Service                        | Status | Database | Kafka | Redis | Elasticsearch |
| ------------------------------ | ------ | -------- | ----- | ----- | ------------- |
| patient-service                | ✅     | Yes      | Yes   | Yes   | -             |
| quality-measure-service        | ✅     | Yes      | Yes   | -     | -             |
| care-gap-service               | ✅     | Yes      | Yes   | -     | -             |
| cql-engine-service             | ✅     | Yes      | -     | Yes   | -             |
| fhir-service                   | ✅     | Yes      | -     | -     | Yes           |
| patient-event-service          | 🔄     | Yes      | Yes   | -     | -             |
| quality-measure-event-service  | 🔄     | Yes      | Yes   | -     | -             |
| care-gap-event-service         | 🔄     | Yes      | Yes   | -     | -             |
| clinical-workflow-event-service| 🔄     | Yes      | Yes   | -     | -             |

---

## Getting Started with Testcontainers

### Dependencies

All services already include Testcontainers dependencies via the shared persistence module:

```gradle
// Included via modules/shared/infrastructure/persistence/build.gradle.kts
testImplementation("org.testcontainers:testcontainers:1.20.1")
testImplementation("org.testcontainers:postgresql:1.20.1")
testImplementation("org.testcontainers:kafka:1.20.1")
testImplementation("org.testcontainers:junit-jupiter:1.20.1")
testImplementation("org.awaitility:awaitility:4.14.1")
```

### Basic Testcontainers Setup

```java
@SpringBootTest
@Testcontainers
class PatientServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("test_db")
        .withUsername("test_user")
        .withPassword("test_password");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private PatientService patientService;

    @Test
    void shouldSaveAndRetrievePatient() {
        // Test implementation
    }
}
```

### Key Concepts

**@Testcontainers**: Enables automatic container lifecycle management
**@Container**: Declares a static container (shared across all tests in class)
**@DynamicPropertySource**: Injects container connection details into Spring properties

---

## Database Integration Testing

### Test Class Template

```java
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class PatientControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("test_db")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
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
    @WithMockUser(roles = "ADMIN")
    void shouldReturnPatient_WhenPatientExists() throws Exception {
        // GIVEN - Insert test data into real database
        Patient testPatient = Patient.builder()
            .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
            .tenantId("TENANT-001")
            .fhirId("PATIENT-123")
            .firstName("John")
            .lastName("Doe")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .createdAt(Instant.now())
            .build();
        patientRepository.save(testPatient);

        // WHEN - Call API endpoint
        mockMvc.perform(get("/api/v1/patients/550e8400-e29b-41d4-a716-446655440000")
                .header("X-Tenant-ID", "TENANT-001")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("John"))
            .andExpect(jsonPath("$.lastName").value("Doe"));

        // THEN - Verify database state
        Patient retrieved = patientRepository.findById(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
            .orElseThrow();
        assertThat(retrieved.getFirstName()).isEqualTo("John");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldEnforceTenantIsolation_WhenAccessingOtherTenantPatient() throws Exception {
        // GIVEN - Insert patient for different tenant
        Patient otherTenantPatient = Patient.builder()
            .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"))
            .tenantId("TENANT-OTHER")
            .fhirId("PATIENT-456")
            .firstName("Jane")
            .lastName("Smith")
            .dateOfBirth(LocalDate.of(1995, 5, 15))
            .createdAt(Instant.now())
            .build();
        patientRepository.save(otherTenantPatient);

        // WHEN/THEN - Should not access patient from different tenant
        mockMvc.perform(get("/api/v1/patients/550e8400-e29b-41d4-a716-446655440001")
                .header("X-Tenant-ID", "TENANT-001")  // Requesting as different tenant
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }
}
```

### Database Cleanup Strategies

#### Strategy 1: Delete All (Simple, Slow)

```java
@BeforeEach
void setUp() {
    patientRepository.deleteAll();  // Deletes all patients before each test
}
```

**Pros**: Predictable, isolated
**Cons**: Slower (if many records), violates foreign keys if not careful

#### Strategy 2: Truncate Tables (Faster, Careful)

```java
@BeforeEach
void setUp() {
    jdbcTemplate.execute("TRUNCATE TABLE patients CASCADE");
    // CASCADE truncates dependent tables
}
```

**Pros**: Faster than DELETE
**Cons**: Resets identity sequences, requires CASCADE understanding

#### Strategy 3: Database Transactions (Recommended)

```java
@SpringBootTest
@Transactional  // Rolls back after each test
@Testcontainers
class PatientServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = ...;

    @Test
    void shouldSavePatient() {
        // Database changes automatically rolled back after test
        patientRepository.save(testPatient);
        assertThat(patientRepository.count()).isEqualTo(1);
    }
    // After test: transaction rolled back, database clean
}
```

**Pros**: Fast, automatic cleanup, no side effects
**Cons**: Doesn't test transaction behavior (use sparingly with `@Transactional(propagation = NOT_SUPPORTED)`)

### Multi-Tenant Testing

```java
@Test
void shouldIsolateTenantData() {
    // Setup: Create data for multiple tenants
    Patient tenant1Patient = Patient.builder()
        .tenantId("TENANT-1")
        .fhirId("P1")
        .build();
    Patient tenant2Patient = Patient.builder()
        .tenantId("TENANT-2")
        .fhirId("P2")
        .build();
    patientRepository.saveAll(List.of(tenant1Patient, tenant2Patient));

    // Test: Each tenant sees only their data
    List<Patient> tenant1Patients = patientRepository.findByTenantId("TENANT-1");
    assertThat(tenant1Patients)
        .hasSize(1)
        .allMatch(p -> p.getTenantId().equals("TENANT-1"));

    List<Patient> tenant2Patients = patientRepository.findByTenantId("TENANT-2");
    assertThat(tenant2Patients)
        .hasSize(1)
        .allMatch(p -> p.getTenantId().equals("TENANT-2"));
}
```

### Seed Data Management

#### Approach 1: Fluent Builder

```java
class PatientTestDataBuilder {
    private UUID id = UUID.randomUUID();
    private String tenantId = "TENANT-DEFAULT";
    private String fhirId = "PATIENT-" + System.currentTimeMillis();
    private String firstName = "John";
    private String lastName = "Doe";
    private LocalDate dateOfBirth = LocalDate.of(1990, 1, 1);

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
            .fhirId(fhirId)
            .firstName(firstName)
            .lastName(lastName)
            .dateOfBirth(dateOfBirth)
            .createdAt(Instant.now())
            .build();
    }
}

// Usage in tests:
@Test
void test() {
    Patient patient = new PatientTestDataBuilder()
        .withTenantId("TENANT-CUSTOM")
        .withFirstName("Jane")
        .build();
}
```

#### Approach 2: Database Fixtures (SQL)

```java
@BeforeEach
void loadFixtures() {
    jdbcTemplate.execute("""
        INSERT INTO patients (id, tenant_id, fhir_id, first_name, last_name, date_of_birth, created_at)
        VALUES ('550e8400-e29b-41d4-a716-446655440000', 'TENANT-001', 'PATIENT-123', 'John', 'Doe', '1990-01-01', NOW())
    """);
}
```

---

## Kafka Integration Testing

### Embedded Kafka Setup

```java
@SpringBootTest
@Testcontainers
class PatientEventServiceIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private KafkaTemplate<String, PatientEvent> kafkaTemplate;

    @Autowired
    private PatientEventConsumer patientEventConsumer;

    @Autowired
    private PatientRepository patientRepository;

    @Test
    void shouldConsumePatientCreatedEvent() throws Exception {
        // GIVEN - Create test event
        PatientEvent event = PatientEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .tenantId("TENANT-001")
            .patientId("PATIENT-123")
            .eventType("PATIENT_CREATED")
            .firstName("John")
            .lastName("Doe")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .timestamp(Instant.now())
            .build();

        // WHEN - Publish event to Kafka
        kafkaTemplate.send("patient-events", event.getPatientId(), event);

        // THEN - Verify consumer processed event and saved to database
        await()
            .atMost(Duration.ofSeconds(5))
            .pollInterval(Duration.ofMillis(100))
            .until(() -> patientRepository.count() == 1);

        Patient savedPatient = patientRepository.findAll().get(0);
        assertThat(savedPatient)
            .extracting(Patient::getFirstName, Patient::getLastName)
            .containsExactly("John", "Doe");
    }
}
```

### Testing Kafka Producers

```java
@Test
void shouldPublishPatientCreatedEvent() throws InterruptedException {
    // GIVEN
    Patient newPatient = Patient.builder()
        .id(UUID.randomUUID())
        .tenantId("TENANT-001")
        .fhirId("PATIENT-789")
        .firstName("Alice")
        .lastName("Johnson")
        .dateOfBirth(LocalDate.of(1985, 3, 15))
        .createdAt(Instant.now())
        .build();

    // WHEN - Save patient, which should trigger event publication
    patientService.createPatient(newPatient, "TENANT-001");

    // THEN - Verify event was published to Kafka
    ConsumerRecord<String, PatientEvent> record = kafkaTestUtils.getSingleRecord(
        "patient-events",
        3000  // Wait max 3 seconds
    );

    PatientEvent publishedEvent = record.value();
    assertThat(publishedEvent)
        .extracting(PatientEvent::getEventType, PatientEvent::getPatientId)
        .containsExactly("PATIENT_CREATED", "PATIENT-789");
}
```

### Testing Kafka Consumers with Polling

```java
@Test
void shouldHandleMultipleConsecutiveEvents() throws Exception {
    // GIVEN - Publish multiple events
    PatientEvent event1 = createEvent("PATIENT-1", "John");
    PatientEvent event2 = createEvent("PATIENT-2", "Jane");
    PatientEvent event3 = createEvent("PATIENT-3", "Bob");

    kafkaTemplate.send("patient-events", event1.getPatientId(), event1);
    kafkaTemplate.send("patient-events", event2.getPatientId(), event2);
    kafkaTemplate.send("patient-events", event3.getPatientId(), event3);

    // WHEN/THEN - All events should be consumed and persisted
    await()
        .atMost(Duration.ofSeconds(10))
        .pollInterval(Duration.ofMillis(100))
        .until(() -> patientRepository.count() == 3);

    List<Patient> allPatients = patientRepository.findAll();
    assertThat(allPatients)
        .hasSize(3)
        .extracting(Patient::getFirstName)
        .containsExactlyInAnyOrder("John", "Jane", "Bob");
}
```

### Testing Kafka with Spring Cloud Stream

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class PatientStreamIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.stream.kafka.binder.brokers", kafka::getBootstrapServers);
    }

    @Autowired
    private StreamBridge streamBridge;

    @MockBean
    private PatientEventConsumer patientEventConsumer;

    @Test
    void shouldPublishAndConsumeViaStreamBridge() {
        // GIVEN
        PatientEvent event = new PatientEvent(/* ... */);

        // WHEN - Publish via StreamBridge
        streamBridge.send("patientEvents-out-0", event);

        // THEN
        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() ->
                verify(patientEventConsumer).handlePatientEvent(argThat(e ->
                    e.getPatientId().equals(event.getPatientId())
                ))
            );
    }
}
```

---

## Async Testing with Awaitility

Awaitility is essential for testing asynchronous operations like Kafka consumers, scheduled tasks, or background jobs.

### Basic Async Test Pattern

```java
@Test
void shouldProcessEventAsynchronously() {
    // GIVEN
    PatientEvent event = createEvent("PATIENT-123");

    // WHEN - Publish event
    kafkaTemplate.send("patient-events", event.getPatientId(), event);

    // THEN - Wait for async processing to complete
    await()
        .atMost(Duration.ofSeconds(5))
        .pollInterval(Duration.ofMillis(100))
        .untilAsserted(() ->
            assertThat(patientRepository.findById(UUID.fromString("550e8400-...")))
                .isPresent()
                .hasValueSatisfying(patient ->
                    assertThat(patient.getFirstName()).isEqualTo("John")
                )
        );
}
```

### Condition-Based Waiting

```java
@Test
void shouldCompleteAsyncTask_UsingCondition() {
    // Test using boolean condition
    ExecutorService executor = Executors.newSingleThreadExecutor();
    AtomicBoolean taskComplete = new AtomicBoolean(false);

    executor.submit(() -> {
        // Simulate async work
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        taskComplete.set(true);
    });

    await()
        .atMost(Duration.ofSeconds(5))
        .untilTrue(taskComplete);
}
```

### Timeout and Polling Configuration

```java
@Test
void shouldRespectTimeoutAndPollSettings() {
    await()
        .atMost(Duration.ofSeconds(10))           // Max wait time
        .pollInterval(Duration.ofMillis(100))     // Check every 100ms
        .pollDelay(Duration.ofMillis(50))         // Initial delay before first check
        .until(() -> someAsyncCondition());
}
```

### Testing Scheduled Tasks

```java
@Configuration
public class SchedulingConfig {
    @Bean
    public TaskScheduler taskScheduler() {
        return new ThreadPoolTaskScheduler();
    }
}

@Service
public class PatientSyncService {
    private final AtomicInteger syncCount = new AtomicInteger(0);

    @Scheduled(fixedRate = 1000)
    public void syncPatients() {
        syncCount.incrementAndGet();
    }

    public int getSyncCount() {
        return syncCount.get();
    }
}

@SpringBootTest
class ScheduledTaskIntegrationTest {
    @Autowired
    private PatientSyncService patientSyncService;

    @Test
    void shouldRunScheduledTask() {
        // GIVEN
        int initialCount = patientSyncService.getSyncCount();

        // WHEN/THEN - Task should run multiple times
        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() ->
                assertThat(patientSyncService.getSyncCount())
                    .isGreaterThan(initialCount + 2)
            );
    }
}
```

---

## Testing Redis Integration

### Redis Container Setup

```java
@SpringBootTest
@Testcontainers
class PatientCacheIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private RedisTemplate<String, Patient> redisTemplate;

    @Autowired
    private PatientService patientService;

    @Test
    void shouldCachePatient() {
        // GIVEN
        Patient patient = new PatientTestDataBuilder().build();

        // WHEN - Get patient (should cache it)
        patientService.getPatient(patient.getId().toString(), "TENANT-001");

        // THEN - Verify patient is in cache
        Patient cachedPatient = (Patient) redisTemplate
            .opsForValue()
            .get("patient:" + patient.getId());

        assertThat(cachedPatient).isEqualTo(patient);
    }

    @Test
    void shouldRespectCacheTTL() {
        // GIVEN - Cache has 5-minute TTL
        Patient patient = new PatientTestDataBuilder().build();
        redisTemplate.opsForValue().set(
            "patient:" + patient.getId(),
            patient,
            Duration.ofMinutes(5)
        );

        // WHEN - Check TTL
        Long ttl = redisTemplate.getExpire("patient:" + patient.getId());

        // THEN - TTL should be less than 5 minutes but close
        assertThat(ttl)
            .isGreaterThan(0)
            .isLessThanOrEqualTo(300);  // 300 seconds = 5 minutes
    }
}
```

---

## Testing Elasticsearch Integration

### Elasticsearch Container Setup

```java
@SpringBootTest
@Testcontainers
class FhirSearchIntegrationTest {

    @Container
    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
        DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.10.0")
    );

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", elasticsearch::getHttpSocketAddress);
    }

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private PatientSearchRepository patientSearchRepository;

    @Test
    void shouldSearchPatientsByName() {
        // GIVEN - Index test patient
        PatientDocument patient = PatientDocument.builder()
            .id("PATIENT-123")
            .tenantId("TENANT-001")
            .firstName("John")
            .lastName("Doe")
            .build();
        patientSearchRepository.save(patient);

        // WHEN - Search for patient
        List<PatientDocument> results = patientSearchRepository.findByFirstName("John");

        // THEN
        assertThat(results)
            .hasSize(1)
            .allMatch(p -> p.getFirstName().equals("John"));
    }
}
```

---

## Common Integration Test Failures & Solutions

### Issue 1: "Container Failed to Start"

```
ERROR: Unable to obtain a new connection, Timeout after waiting 120s for a container
```

**Causes:**
- Docker daemon not running
- Insufficient system resources
- Network connectivity issues

**Solutions:**
```bash
# Check Docker status
docker ps

# Start Docker (macOS)
open /Applications/Docker.app

# Check available resources
docker system df

# Remove dangling containers/images
docker system prune
```

### Issue 2: "Port Already in Use"

```
ERROR: Bind for 0.0.0.0:5432 failed: port is already allocated
```

**Cause:** Another PostgreSQL container still running

**Solution:**
```bash
# Find and stop conflicting container
docker ps -a | grep postgres
docker stop <container-id>
docker rm <container-id>
```

### Issue 3: "Kafka Broker Not Available"

```
ERROR: Broker may not be available
```

**Cause:** Kafka container didn't fully start before tests ran

**Solution:**
```java
@Test
void testWithBrokerWait() {
    // Add explicit wait for Kafka broker
    await()
        .atMost(Duration.ofSeconds(30))
        .until(() -> testRestTemplate.getForEntity(
            "http://localhost:" + embeddedKafka.getZookeeperServer().getPort(),
            String.class
        ).getStatusCode().is2xxSuccessful());
}
```

### Issue 4: "FOREIGN KEY Constraint Failed"

```
ERROR: insert or update on table "patients" violates foreign key constraint
```

**Cause:** Inserting data with invalid foreign key references

**Solution:**
```java
// Insert in correct order (parents first)
@BeforeEach
void setupData() {
    // Insert parent records first
    Organization org = organizationRepository.save(testOrg);

    // Then insert children
    Patient patient = Patient.builder()
        .organizationId(org.getId())  // Now this reference exists
        .build();
    patientRepository.save(patient);
}
```

### Issue 5: "Test Timeout" (Kafka Consumer Never Processes)

```
ERROR: Timed out waiting for Kafka message
```

**Cause:** Consumer not receiving messages due to:
- Wrong topic name
- Consumer not subscribed
- Message not published
- Deserialization error

**Solution:**
```java
@Test
void testWithDiagnostics() {
    // Enable Spring Kafka debug logging
    logger.info("Publishing event to topic: patient-events");

    kafkaTemplate.send("patient-events", "test-key", testEvent)
        .addCallback(
            success -> logger.info("Message sent successfully: " + success.getRecordMetadata()),
            failure -> logger.error("Message failed to send", failure)
        );

    // Verify with explicit consumer check
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> {
            logger.info("Checking if record was persisted...");
            assertThat(patientRepository.count()).isEqualTo(1);
        });
}
```

---

## Performance Considerations

### Slow Integration Tests

**Problem**: Integration tests are 10-100x slower than unit tests

**Optimization Strategies**:

#### 1. Share Containers Across Tests (Current Best Practice)

```java
@Testcontainers
class PatientServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    // Container shared across all @Test methods

    @Test
    void test1() { }  // Reuses container

    @Test
    void test2() { }  // Reuses container
}
```

#### 2. Use Test Fixtures Instead of Setup

```java
// ❌ SLOW - Rebuilds container for each test class
@Test
void test1() {
    new PostgreSQLContainer<>(...).start();
}

// ✅ FAST - Container shared
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(...);
```

#### 3. Batch Assertions

```java
// ❌ SLOW - Multiple separate assertions trigger database checks
assertThat(patient.getFirstName()).isEqualTo("John");
assertThat(patient.getLastName()).isEqualTo("Doe");
assertThat(patient.getAge()).isEqualTo(34);

// ✅ FAST - Single batch assertion
assertThat(patient)
    .extracting(Patient::getFirstName, Patient::getLastName, Patient::getAge)
    .containsExactly("John", "Doe", 34);
```

#### 4. Limit Transaction Scope

```java
// ❌ SLOW - Large transaction with many operations
@Transactional
@Test
void testMany() {
    for (int i = 0; i < 100; i++) {
        patientRepository.save(createPatient(i));
    }
}

// ✅ FASTER - Batch insert
@Test
void testMany() {
    List<Patient> patients = IntStream.range(0, 100)
        .mapToObj(this::createPatient)
        .collect(toList());
    patientRepository.saveAll(patients);
}
```

### Container Startup Time

| Container    | First Start | Subsequent | Total (10 tests) |
| ------------ | ----------- | ---------- | ---------------- |
| PostgreSQL   | ~5s         | ~0s        | ~5s              |
| Kafka        | ~8s         | ~0s        | ~8s              |
| Redis        | ~2s         | ~0s        | ~2s              |
| Elasticsearch| ~15s        | ~0s        | ~15s             |

**Total startup for full integration test: ~15 seconds one-time per test class**

---

## Multi-Container Orchestration

### Testing Multiple Services

```java
@SpringBootTest
@Testcontainers
class MultiServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> patientDb =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("patient_db");

    @Container
    static PostgreSQLContainer<?> qualityDb =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("quality_db");

    @Container
    static KafkaContainer kafka =
        new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // Patient service config
        registry.add("patient.datasource.url", patientDb::getJdbcUrl);
        registry.add("patient.datasource.username", patientDb::getUsername);
        registry.add("patient.datasource.password", patientDb::getPassword);

        // Quality service config
        registry.add("quality.datasource.url", qualityDb::getJdbcUrl);
        registry.add("quality.datasource.username", qualityDb::getUsername);
        registry.add("quality.datasource.password", qualityDb::getPassword);

        // Kafka config (shared)
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Test
    void shouldProcessPatientAndUpdateQualityMetrics() {
        // Simulate inter-service communication via Kafka
    }
}
```

---

## Best Practices Summary

### ✅ DO

- Use **@Transactional** for automatic cleanup
- Share containers across test methods with `@Container static`
- Use **Awaitility** for async operations (wait max 5 seconds)
- Test multi-tenant isolation explicitly
- Include both success and failure paths
- Use test data builders for complex objects
- Verify both service logic AND database state

### ❌ DON'T

- Create new containers in every test
- Mock external services (use real containers)
- Sleep arbitrary amounts (use Awaitility instead)
- Ignore foreign key violations during setup
- Test transaction behavior with `@Transactional` (use separate test)
- Share containers across test classes (causes order dependencies)
- Ignore Kafka deserialization errors

---

## Running Integration Tests

### All Integration Tests

```bash
cd backend
./gradlew test --tests "*IntegrationTest"
```

### Specific Service

```bash
./gradlew :modules:services:patient-service:test --tests "*IntegrationTest"
```

### With Detailed Logging

```bash
./gradlew test --tests "*IntegrationTest" -i --logging.level.org.testcontainers=DEBUG
```

### Performance Metrics

```bash
./gradlew test --tests "*IntegrationTest" --info 2>&1 | grep -E "Test|PASSED|FAILED"
```

---

## Next Steps

1. **Review existing integration tests** in patient-service, quality-measure-service, care-gap-service
2. **Apply these patterns** to Phase 5 event-driven services
3. **Add Kafka integration tests** to all event services
4. **Implement fixture-based testing** for complex multi-tenant scenarios
5. **Configure CI/CD** to run integration tests on every PR

---

## Related Documentation

- **TESTING_STRATEGY.md** - Unit testing standards and naming conventions
- **TEST_COVERAGE.md** - Code coverage measurement and JaCoCo configuration
- **LOCAL_SETUP.md** - Running services locally for manual integration testing
- **CI_CD_GUIDE.md** - Automated integration test execution in GitHub Actions

---

_Last Updated: January 19, 2026_
_Version: 1.0_
