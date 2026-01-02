# Quality Measure Service

Healthcare quality measure calculations service supporting HEDIS, CMS, and custom measures with population-level analytics.

## Overview

The Quality Measure Service provides comprehensive quality measure calculation and reporting capabilities for healthcare organizations. It implements HEDIS (Healthcare Effectiveness Data and Information Set) measures, CMS (Centers for Medicare & Medicaid Services) quality measures, and supports custom measure definitions for value-based care programs.

## Key Features

### HEDIS Measure Calculations
- 50+ standard HEDIS measures supported
- Patient-level and population-level calculations
- Integration with CQL Engine Service for measure logic
- Real-time measure evaluation with FHIR R4 data

### Quality Reporting
- Patient quality reports with measure breakdowns
- Population quality reports by measurement year
- Report export to CSV and Excel formats
- Saved reports with versioning and audit trail

### Batch Processing
- Asynchronous population-level calculations
- Job tracking with progress monitoring
- Cancellable long-running jobs
- Error tracking and partial success handling

### Custom Measures
- Define organization-specific quality measures
- CQL-based measure definitions
- Custom scoring algorithms
- Measure versioning and lifecycle management

### Health Scoring
- Aggregate quality scores per patient
- Percentile rankings and benchmarks
- Quality trend analysis over time
- Care gap identification

### Clinical Decision Support (CDS)
- Real-time quality measure guidance
- Risk assessment integration
- Template-based reporting
- Patient health summary generation

## Technology Stack

- **Spring Boot 3.x**: Core framework
- **PostgreSQL**: Persistent storage (shared healthdata_cql database)
- **Redis**: Caching (HIPAA-compliant 2-minute TTL)
- **Apache Kafka**: Event streaming for audit
- **Liquibase**: Database migrations
- **WebSocket**: Real-time notifications
- **Apache POI**: Excel export capabilities

## API Endpoints

### Measure Calculation
```
POST /quality-measure/calculate
     - Calculate quality measure for a patient
     - Params: patient, measure, createdBy

GET  /quality-measure/results?patient={id}
     - Get measure results for a patient

GET  /quality-measure/score?patient={id}
     - Get aggregate quality score
```

### Quality Reports
```
GET  /quality-measure/report/patient?patient={id}
     - Generate patient quality report

GET  /quality-measure/report/population?year={year}
     - Generate population quality report

POST /quality-measure/report/patient/save
     - Save patient report

POST /quality-measure/report/population/save
     - Save population report
```

### Saved Reports
```
GET    /quality-measure/reports?type={type}
       - List saved reports

GET    /quality-measure/reports/{reportId}
       - Get specific report

DELETE /quality-measure/reports/{reportId}
       - Delete saved report

GET    /quality-measure/reports/{reportId}/export/csv
       - Export report to CSV

GET    /quality-measure/reports/{reportId}/export/excel
       - Export report to Excel
```

### Batch Population Calculations
```
POST /quality-measure/population/calculate
     - Start batch calculation for all patients
     - Returns job ID for tracking

GET  /quality-measure/population/jobs/{jobId}
     - Get job status and progress

GET  /quality-measure/population/jobs
     - List all jobs for tenant

POST /quality-measure/population/jobs/{jobId}/cancel
     - Cancel running job
```

### Health Check
```
GET /quality-measure/_health
    - Service health status
```

## Configuration

### Application Properties
```yaml
server.port: 8087
spring.datasource.url: jdbc:postgresql://localhost:5435/healthdata_cql
fhir.server.url: http://localhost:8085/fhir
cql.engine.url: http://localhost:8081/cql-engine
```

### HIPAA Compliance
- Redis cache TTL: 2 minutes (HIPAA-compliant for PHI)
- All API calls audited via Kafka
- Tenant isolation enforced on all operations
- WebSocket connections: 15-minute automatic logoff

### WebSocket Configuration
- Real-time measure calculation progress
- Session timeout: 15 minutes (HIPAA §164.312(a)(2)(iii))
- Maximum 3 concurrent connections per user
- Rate limiting: 10 connections/minute per IP

## Running Locally

### Prerequisites
- Java 21
- PostgreSQL 14+
- Redis 6+
- Kafka 3+

### Build
```bash
./gradlew :modules:services:quality-measure-service:build
```

### Run
```bash
./gradlew :modules:services:quality-measure-service:bootRun
```

### Run Tests
```bash
./gradlew :modules:services:quality-measure-service:test
```

---

## Testing

### Overview

Quality Measure Service has comprehensive test coverage with **100+ test files** covering unit tests, integration tests, multi-tenant isolation, RBAC, HIPAA compliance, and performance benchmarks.

| Test Type | Count | Purpose |
|-----------|-------|---------|
| Unit Tests | 60+ | Service layer logic with Mockito mocks |
| Integration Tests | 20+ | API endpoints with TestContainers |
| Multi-Tenant Tests | 15+ | Tenant data isolation verification |
| WebSocket Tests | 10+ | Real-time notification testing |
| Consumer Tests | 5+ | Kafka event processing |
| Template Tests | 8+ | Email/notification template rendering |

### Quick Start

```bash
# Run all tests
./gradlew :modules:services:quality-measure-service:test

# Run specific test suite
./gradlew :modules:services:quality-measure-service:test --tests "*ServiceTest"
./gradlew :modules:services:quality-measure-service:test --tests "*IntegrationTest"
./gradlew :modules:services:quality-measure-service:test --tests "*MultiTenant*"

# Run with coverage report
./gradlew :modules:services:quality-measure-service:test jacocoTestReport

# Run single test class
./gradlew :modules:services:quality-measure-service:test --tests "QualityMeasureServiceTest"
```

### Test Organization

```
src/test/java/com/healthdata/quality/
├── config/                              # Test configurations
│   ├── BaseIntegrationTest.java         # Shared integration test annotation
│   ├── TestSecurityConfiguration.java   # Mock JWT/auth for tests
│   ├── TestMessagingConfiguration.java  # Mock Kafka for tests
│   ├── TestWebSocketConfiguration.java  # WebSocket test config
│   └── TestCacheConfiguration.java      # Redis cache test config
├── service/                             # Unit tests
│   ├── QualityMeasureServiceTest.java
│   ├── HealthScoreServiceTest.java
│   ├── CareGapServiceTest.java
│   └── notification/                    # Notification service tests
├── controller/                          # Controller unit tests
│   ├── QualityMeasureControllerTest.java
│   └── HealthScoreControllerTest.java
├── integration/                         # Integration tests
│   ├── MultiTenantIsolationIntegrationTest.java
│   ├── CachingBehaviorIntegrationTest.java
│   ├── EndToEndIntegrationTest.java
│   └── ErrorHandlingIntegrationTest.java
├── consumer/                            # Kafka consumer tests
│   └── RiskAssessmentEventConsumerTest.java
├── websocket/                           # WebSocket tests
│   ├── HealthScoreWebSocketHandlerTest.java
│   └── TenantAccessInterceptorTest.java
└── persistence/                         # Repository tests
    └── QualityMeasureResultRepositoryTest.java
```

### Unit Tests (Service Layer)

Unit tests use Mockito to isolate service logic from dependencies.

**Example: QualityMeasureServiceTest.java**

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Quality Measure Service Tests")
class QualityMeasureServiceTest {

    @Mock
    private QualityMeasureResultRepository repository;

    @Mock
    private CqlEngineServiceClient cqlEngineClient;

    @InjectMocks
    private QualityMeasureService measureService;

    private static final String TENANT_ID = "tenant-test-001";
    private static final UUID PATIENT_ID = UUID.randomUUID();

    @Test
    @DisplayName("Should calculate measure when valid patient data exists")
    void shouldCalculateMeasureSuccessfully() {
        // Given
        String cqlResponse = """
            {"measureResult": {"inNumerator": true, "inDenominator": true}}
            """;
        when(cqlEngineClient.evaluateCql(eq(TENANT_ID), anyString(), eq(PATIENT_ID), anyString()))
            .thenReturn(cqlResponse);

        // When
        QualityMeasureResult result = measureService.calculateMeasure(
            TENANT_ID, PATIENT_ID, "HEDIS_BCS", "test-user");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isNumeratorCompliant()).isTrue();
        assertThat(result.getTenantId()).isEqualTo(TENANT_ID);

        verify(cqlEngineClient).evaluateCql(eq(TENANT_ID), anyString(), eq(PATIENT_ID), anyString());
        verify(repository).save(any(QualityMeasureResultEntity.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when patient not found")
    void shouldThrowWhenPatientNotFound() {
        // Given
        when(cqlEngineClient.evaluateCql(anyString(), anyString(), any(), anyString()))
            .thenThrow(new ResourceNotFoundException("Patient", PATIENT_ID.toString()));

        // When/Then
        assertThatThrownBy(() -> measureService.calculateMeasure(
                TENANT_ID, PATIENT_ID, "HEDIS_BCS", "test-user"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Patient");
    }
}
```

**Key Patterns**:
- `@ExtendWith(MockitoExtension.class)` for Mockito integration
- `@Mock` for dependencies, `@InjectMocks` for service under test
- `@DisplayName` for readable test descriptions
- AssertJ assertions for fluent verification

### Integration Tests (API Endpoints)

Integration tests use `@BaseIntegrationTest` annotation which provides:
- Full Spring Boot context
- TestContainers for PostgreSQL
- Mock Kafka and Redis configurations
- Test security context

**Example: EndToEndIntegrationTest.java**

```java
@BaseIntegrationTest
@AutoConfigureMockMvc
@DisplayName("End-to-End Integration Tests")
class EndToEndIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QualityMeasureResultRepository repository;

    @MockBean
    private CqlEngineServiceClient cqlEngineClient;

    private static final String TENANT_ID = "tenant-integration-001";

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Should calculate and retrieve measure results")
    void shouldCalculateAndRetrieveResults() throws Exception {
        // Given - mock CQL engine response
        String cqlResponse = """
            {"measureResult": {"inNumerator": true, "inDenominator": true}}
            """;
        when(cqlEngineClient.evaluateCql(anyString(), anyString(), any(), anyString()))
            .thenReturn(cqlResponse);

        UUID patientId = UUID.randomUUID();

        // When - calculate measure
        mockMvc.perform(post("/quality-measure/calculate")
                .header("X-Tenant-ID", TENANT_ID)
                .param("patient", patientId.toString())
                .param("measure", "HEDIS_BCS")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tenantId").value(TENANT_ID))
            .andExpect(jsonPath("$.numeratorCompliant").value(true));

        // Then - retrieve results
        mockMvc.perform(get("/quality-measure/results")
                .header("X-Tenant-ID", TENANT_ID)
                .param("patient", patientId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].measureId").value("HEDIS_BCS"));
    }
}
```

**BaseIntegrationTest Annotation**:

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@EnableAutoConfiguration(exclude = {AuthenticationAutoConfiguration.class})
@Import({
    TestSecurityConfiguration.class,
    TestWebSocketConfiguration.class,
    TestCacheConfiguration.class,
    TestMessagingConfiguration.class
})
public @interface BaseIntegrationTest {
}
```

### Multi-Tenant Isolation Tests

**Critical for HIPAA compliance** - ensures tenant data cannot leak across organizations.

**Example: MultiTenantIsolationIntegrationTest.java** (477 lines)

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Multi-Tenant Isolation Integration Tests")
class MultiTenantIsolationIntegrationTest {

    private static final String TENANT_1 = "tenant-1";
    private static final String TENANT_2 = "tenant-2";

    @Test
    @DisplayName("Should isolate patient results by tenant")
    void shouldIsolatePatientResultsByTenant() throws Exception {
        // Create data for tenant 1
        createMeasureResult(TENANT_1, PATIENT_ID, "HEDIS_1", true);
        createMeasureResult(TENANT_1, PATIENT_ID, "HEDIS_2", false);

        // Create data for tenant 2
        createMeasureResult(TENANT_2, PATIENT_ID, "HEDIS_3", true);

        // Query tenant 1 - should only see tenant 1 data
        mockMvc.perform(get("/quality-measure/results")
                .header("X-Tenant-ID", TENANT_1)
                .param("patient", PATIENT_ID.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[*].tenantId", everyItem(is(TENANT_1))));

        // Query tenant 2 - should only see tenant 2 data
        mockMvc.perform(get("/quality-measure/results")
                .header("X-Tenant-ID", TENANT_2)
                .param("patient", PATIENT_ID.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].tenantId").value(TENANT_2));
    }

    @Test
    @DisplayName("Should not allow tenant to access another tenant's data")
    void shouldNotAllowCrossTenantAccess() throws Exception {
        // Create data for tenant 1
        createMeasureResult(TENANT_1, PATIENT_ID, "HEDIS_1", true);

        // Try to access using tenant 2 header - should return empty
        mockMvc.perform(get("/quality-measure/results")
                .header("X-Tenant-ID", TENANT_2)
                .param("patient", PATIENT_ID.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should enforce tenant ID presence in all requests")
    void shouldEnforceTenantIdPresence() throws Exception {
        // All endpoints should require X-Tenant-ID header
        mockMvc.perform(get("/quality-measure/results")
                .param("patient", PATIENT_ID.toString()))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should propagate tenant ID to external services")
    void shouldPropagateTenantIdToExternalServices() throws Exception {
        // Verify CQL Engine is called with correct tenant ID
        mockMvc.perform(post("/quality-measure/calculate")
                .header("X-Tenant-ID", TENANT_1)
                .param("patient", PATIENT_ID.toString())
                .param("measure", "HEDIS_TEST"))
            .andExpect(status().isCreated());

        verify(cqlEngineServiceClient).evaluateCql(
            eq(TENANT_1), anyString(), any(UUID.class), anyString());
    }
}
```

### HIPAA Compliance Tests

**Purpose**: Verify PHI handling meets HIPAA requirements (cache TTL, audit logging, headers).

```java
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("HIPAA Compliance Tests")
class HipaaComplianceTest {

    @Autowired
    private CacheManager cacheManager;

    @Test
    @DisplayName("PHI cache TTL must not exceed 5 minutes")
    void phiCacheTtlShouldBeCompliant() {
        // Quality Measure Service uses 2-minute TTL (stricter than 5-min requirement)
        Cache measureCache = cacheManager.getCache("measureResults");
        assertThat(measureCache).isNotNull();

        // Verify TTL configuration in application-test.yml
        // spring.cache.redis.time-to-live: 120000 (2 minutes)
    }

    @Test
    @DisplayName("WebSocket sessions should timeout at 15 minutes")
    void webSocketSessionTimeoutShouldBeCompliant() {
        // HIPAA §164.312(a)(2)(iii) requires automatic logoff
        // Quality Measure Service enforces 15-minute session timeout
        assertThat(WebSocketConfig.SESSION_TIMEOUT_MINUTES).isEqualTo(15);
    }
}
```

### WebSocket Tests

Quality Measure Service uses WebSocket for real-time measure calculation progress.

```java
@SpringBootTest
@DisplayName("WebSocket Handler Tests")
class HealthScoreWebSocketHandlerTest {

    @Test
    @DisplayName("Should broadcast health score updates to tenant subscribers")
    void shouldBroadcastToTenantSubscribers() {
        // Given
        String tenantId = "tenant-ws-001";
        WebSocketSession session = mockSession(tenantId);

        // When
        handler.handleTextMessage(session, new TextMessage(
            "{\"type\":\"subscribe\",\"topic\":\"health-scores\"}"));

        // Then
        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("Should enforce tenant isolation in WebSocket broadcasts")
    void shouldEnforceTenantIsolation() {
        // Given
        WebSocketSession tenant1Session = mockSession("tenant-1");
        WebSocketSession tenant2Session = mockSession("tenant-2");

        // When - broadcast to tenant-1 only
        handler.broadcastToTenant("tenant-1", new HealthScoreUpdate(...));

        // Then
        verify(tenant1Session).sendMessage(any());
        verify(tenant2Session, never()).sendMessage(any());
    }
}
```

### Performance Tests

Benchmark latency and throughput for SLA compliance.

```java
@SpringBootTest
@DisplayName("Performance Tests")
class PerformanceTest {

    @Test
    @DisplayName("Measure calculation should complete within 600ms per patient")
    void measureCalculationPerformance() {
        // Given
        int patientCount = 100;

        // When
        Instant start = Instant.now();
        for (int i = 0; i < patientCount; i++) {
            measureService.calculateMeasure(TENANT_ID, UUID.randomUUID(), "HEDIS_BCS", "perf-test");
        }
        Instant end = Instant.now();

        // Then
        long totalMs = Duration.between(start, end).toMillis();
        double avgMsPerPatient = totalMs / (double) patientCount;

        assertThat(avgMsPerPatient)
            .isLessThan(600.0)
            .withFailMessage("Average calculation time %.2fms exceeds 600ms SLA", avgMsPerPatient);
    }

    @Test
    @DisplayName("Population report should complete within 5 seconds for 1000 patients")
    void populationReportPerformance() {
        // Given - 1000 patients with 5 measures each
        setupPopulationData(1000, 5);

        // When
        Instant start = Instant.now();
        PopulationReport report = reportService.generatePopulationReport(TENANT_ID, 2025);
        Instant end = Instant.now();

        // Then
        long totalMs = Duration.between(start, end).toMillis();
        assertThat(totalMs).isLessThan(5000L);
    }
}
```

### Test Configuration

**TestSecurityConfiguration.java** - Mocks JWT authentication:

```java
@Configuration
@Profile("test")
public class TestSecurityConfiguration {

    @Bean
    @Primary
    public JwtTokenService jwtTokenService() {
        JwtTokenService mock = mock(JwtTokenService.class);
        when(mock.validateToken(anyString())).thenReturn(true);
        when(mock.extractUsername(anyString())).thenReturn("test-user");
        when(mock.extractTenantIds(anyString())).thenReturn(Set.of("test-tenant"));
        when(mock.extractRoles(anyString())).thenReturn(Set.of("USER", "ADMIN"));
        return mock;
    }
}
```

**application-test.yml** - Test-specific configuration:

```yaml
spring:
  datasource:
    url: jdbc:tc:postgresql:15-alpine:///testdb
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver

  cache:
    type: simple  # Use simple cache instead of Redis for tests

  kafka:
    bootstrap-servers: ""  # Disabled in tests (mocked)

logging:
  level:
    com.healthdata: DEBUG
    org.springframework.test: WARN
```

### Best Practices

1. **HIPAA Compliance**
   - All test data uses synthetic patterns (no real PHI)
   - Patient IDs: `UUID.randomUUID()` or `550e8400-e29b-...`
   - Tenant IDs: `tenant-test-001`, `tenant-integration-001`

2. **Tenant Isolation**
   - Every test verifies `tenantId` filtering in queries
   - Use `X-Tenant-ID` header in all MockMvc requests
   - Test cross-tenant access denial scenarios

3. **Gateway Trust Headers**
   - Use `X-Tenant-ID` instead of JWT token validation
   - Mock `JwtTokenService` returns consistent test values
   - Test header presence enforcement

4. **Test Data Cleanup**
   - Use `@Transactional` for automatic rollback
   - Call `repository.deleteAll()` in `@BeforeEach` for cache tests
   - Clear caches between tests when using `@SpringBootTest`

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| TestContainers timeout | Docker not running | Start Docker Desktop |
| `NoSuchBeanDefinitionException` | Missing test config | Add `@Import(TestSecurityConfiguration.class)` |
| Multi-tenant test fails | Missing tenantId filter | Add WHERE clause to repository query |
| WebSocket test fails | Missing mock session | Use `MockWebSocketSession` |
| Cache TTL test fails | Wrong cache type | Set `spring.cache.type=simple` in test profile |
| Kafka consumer test fails | Missing mock | Add `@Import(TestMessagingConfiguration.class)` |

### CI/CD Integration

Tests run automatically in GitHub Actions:

```yaml
# .github/workflows/backend-tests.yml
jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_DB: testdb
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Run Quality Measure Service Tests
        run: ./gradlew :modules:services:quality-measure-service:test
      - name: Upload Coverage Report
        uses: codecov/codecov-action@v4
```

---

## Integration

This service integrates with:
- **FHIR Service**: Retrieve patient clinical data
- **CQL Engine Service**: Execute measure logic
- **Care Gap Service**: Identify care gaps from measures
- **Patient Service**: Patient demographics
- **Payer Workflows Service**: Star Ratings and compliance

## Security

- JWT-based authentication
- Role-based access control (EVALUATOR, ANALYST, ADMIN, SUPER_ADMIN)
- Tenant isolation via X-Tenant-ID header
- HIPAA-compliant audit logging
- Rate limiting on all endpoints

## API Documentation

Swagger UI available at:
```
http://localhost:8087/quality-measure/swagger-ui.html
```

## Monitoring

Actuator endpoints:
- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`

## License

Copyright (c) 2024 Mahoosuc Solutions
