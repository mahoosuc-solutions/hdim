# Care Gap Service

Automated care gap identification, tracking, and reporting for HEDIS/quality measure compliance.

## Purpose

Identifies and tracks gaps in care based on CQL evaluation results, addressing the challenge that:
- Quality measure compliance requires proactive identification of missing screenings, tests, and interventions
- Care teams need prioritized lists of gaps (high priority, overdue, upcoming)
- Population health management requires aggregated gap reports across patient panels
- Care gap closure tracking is essential for value-based care reimbursement

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Care Gap Service                              │
│                         (Port 8086)                              │
├─────────────────────────────────────────────────────────────────┤
│  Controller Layer                                                │
│  └── CareGapController (25+ REST endpoints)                     │
├─────────────────────────────────────────────────────────────────┤
│  Service Layer                                                   │
│  ├── CareGapIdentificationService                               │
│  │   ├── Identify all gaps         - Run all CQL libraries      │
│  │   ├── Identify by library       - Single measure evaluation  │
│  │   ├── Refresh gaps              - Re-evaluate patient        │
│  │   ├── Close gaps                - Mark as addressed          │
│  │   ├── Query by status           - Open, high priority        │
│  │   └── Statistics                - Gap counts, priorities     │
│  └── CareGapReportService                                       │
│      ├── Patient summaries         - Gap overview per patient   │
│      ├── Category grouping         - By measure category        │
│      ├── Priority grouping         - By urgency                 │
│      ├── Overdue gaps              - Past due date              │
│      ├── Upcoming gaps             - Due within N days          │
│      └── Population reports        - Tenant-wide analytics      │
├─────────────────────────────────────────────────────────────────┤
│  Repository Layer                                                │
│  └── CareGapRepository (JPA + custom queries)                   │
├─────────────────────────────────────────────────────────────────┤
│  Domain Entities                                                 │
│  └── CareGapEntity                                              │
│      ├── Gap Metadata         - Patient, measure, category      │
│      ├── Priority             - LOW, MEDIUM, HIGH, CRITICAL     │
│      ├── Status               - OPEN, CLOSED                    │
│      ├── Dates                - Identified, due, closed         │
│      └── Closure Tracking     - Reason, action, closed by       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Feign (HTTP) + Circuit Breaker
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  CQL Engine (8081)        Patient Service (8084)                │
│  - CQL evaluation          - Patient data aggregation           │
│  - Measure logic           - Health record access               │
└─────────────────────────────────────────────────────────────────┘
```

## API Endpoints

### Care Gap Identification
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/care-gap/identify?patient={id}` | Identify all gaps for patient |
| POST | `/care-gap/identify/{library}?patient={id}` | Identify gaps for measure |
| POST | `/care-gap/refresh?patient={id}` | Re-evaluate patient (refresh) |
| POST | `/care-gap/close?gapId={id}&closedBy={user}` | Close a care gap |

### Care Gap Queries
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/care-gap/open?patient={id}` | Open gaps for patient |
| GET | `/care-gap/high-priority?patient={id}` | High priority gaps |
| GET | `/care-gap/overdue?patient={id}` | Overdue gaps (past due date) |
| GET | `/care-gap/upcoming?patient={id}&days=30` | Due within N days |

### Statistics & Reports
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/care-gap/stats?patient={id}` | Gap statistics for patient |
| GET | `/care-gap/summary?patient={id}` | Gap summary for patient |
| GET | `/care-gap/by-category?patient={id}` | Gaps grouped by category |
| GET | `/care-gap/by-priority?patient={id}` | Gaps grouped by priority |
| GET | `/care-gap/population-report` | Tenant-wide gap report |

## Care Gap Entity Structure

```json
{
  "id": "uuid",
  "tenantId": "tenant-1",
  "patientId": "patient-123",
  "measureName": "Colorectal Cancer Screening",
  "measureId": "COL",
  "measureCategory": "Cancer Screening",
  "gapDescription": "Patient is due for colorectal cancer screening (age 55, last colonoscopy 2015)",
  "priority": "HIGH",  // LOW, MEDIUM, HIGH, CRITICAL
  "status": "OPEN",  // OPEN, CLOSED
  "identifiedDate": "2024-01-15T10:00:00Z",
  "dueDate": "2024-06-30",
  "closedDate": null,
  "closedBy": null,
  "closureReason": null,
  "closureAction": null,
  "cqlEvaluationId": "eval-uuid",
  "createdBy": "system"
}
```

## Gap Identification Logic

1. **Trigger**: Manual API call or scheduled job
2. **Evaluation**: CQL Engine evaluates all quality measures for patient
3. **Gap Detection**: If measure result = false, create CareGapEntity
4. **Priority Assignment**: Based on measure category, overdue status
5. **Notification**: Kafka event published for care team alerts

## Configuration

```yaml
server:
  port: 8086
  servlet:
    context-path: /care-gap

# Service integrations
cql:
  engine:
    url: http://localhost:8081/cql-engine
patient:
  service:
    url: http://localhost:8084/patient
fhir:
  server:
    url: http://localhost:8085/fhir

# Cache configuration
spring.cache:
  type: redis
  redis:
    time-to-live: 300000  # 5 minutes for gap results

# Kafka configuration
spring.kafka:
  bootstrap-servers: localhost:9092
  producer:
    key-serializer: org.apache.kafka.common.serialization.StringSerializer
    value-serializer: org.apache.kafka.common.serialization.StringSerializer
```

## Response Examples

**Gap Statistics**:

```json
{
  "totalGaps": 12,
  "openGaps": 8,
  "closedGaps": 4,
  "highPriorityGaps": 3,
  "overdueGaps": 2,
  "gapsByCategory": {
    "Cancer Screening": 3,
    "Diabetes Management": 2,
    "Preventive Care": 3
  }
}
```

**Population Report**:

```json
{
  "tenantId": "tenant-1",
  "totalPatients": 5000,
  "patientsWithGaps": 1200,
  "totalOpenGaps": 3500,
  "averageGapsPerPatient": 2.9,
  "topGapCategories": [
    {"category": "Cancer Screening", "count": 800},
    {"category": "Diabetes Management", "count": 600},
    {"category": "Preventive Care", "count": 500}
  ],
  "priorityDistribution": {
    "HIGH": 1000,
    "MEDIUM": 1500,
    "LOW": 1000
  }
}
```

## Dependencies

- **Spring Boot**: Web, JPA, Validation, Cache
- **Database**: PostgreSQL with Liquibase migrations
- **Cache**: Redis (5 min TTL for gap results)
- **HTTP Client**: OpenFeign for CQL Engine, Patient Service integration
- **Messaging**: Kafka for gap identification events
- **Resilience**: Circuit breakers for service failures

## Running Locally

```bash
# Start dependencies (CQL Engine, Patient Service)
docker compose up -d cql-engine-service patient-service

# From backend directory
./gradlew :modules:services:care-gap-service:bootRun

# Or via Docker
docker compose --profile care-gap up care-gap-service
```

## Testing

### Overview

Care Gap Service has comprehensive test coverage across 6 test types:

| Test Type | Files | Focus Area |
|-----------|-------|------------|
| Unit Tests | 5+ | Service layer, entity validation, business logic |
| Integration Tests | 4+ | Repository operations, API endpoints |
| Multi-Tenant Tests | 4+ | Tenant data isolation (HIPAA compliance) |
| RBAC Tests | 1+ | Role-based access control |
| Kafka Event Tests | 2+ | Event publishing verification |
| Performance Tests | 1+ | Query latency, batch operations |

### Quick Start

```bash
# Run all tests
./gradlew :modules:services:care-gap-service:test

# Run specific test suite
./gradlew :modules:services:care-gap-service:test --tests "*ServiceTest"
./gradlew :modules:services:care-gap-service:test --tests "*IntegrationTest"
./gradlew :modules:services:care-gap-service:test --tests "*MultiTenant*"

# Run with coverage
./gradlew :modules:services:care-gap-service:test jacocoTestReport

# View coverage report
open build/reports/jacoco/test/html/index.html
```

### Test Organization

```
src/test/java/com/healthdata/caregap/
├── config/
│   ├── BaseIntegrationTest.java           # TestContainers annotation
│   ├── TestCacheConfiguration.java        # In-memory cache for tests
│   └── CareGapSecurityConfigTest.java     # Security configuration tests
├── service/
│   ├── CareGapIdentificationServiceTest.java  # Gap identification logic (605 lines)
│   └── CareGapReportServiceTest.java          # Reporting logic
├── controller/
│   ├── CareGapControllerTest.java             # Unit tests with mocks
│   └── CareGapControllerIntegrationTest.java  # Full API tests
├── persistence/
│   ├── CareGapEntityTest.java                 # Entity validation
│   ├── CareGapRecommendationEntityTest.java   # Recommendation entity
│   └── CareGapClosureEntityTest.java          # Closure tracking
└── integration/
    ├── CareGapRepositoryIntegrationTest.java  # Repository queries (445 lines)
    ├── CareGapClosureRepositoryIntegrationTest.java
    └── CareGapRecommendationRepositoryIntegrationTest.java
```

### Unit Tests (Service Layer)

Tests care gap identification, closure, and statistics with mocked dependencies:

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Care Gap Identification Service Tests")
class CareGapIdentificationServiceTest {

    @Mock
    private CareGapRepository careGapRepository;

    @Mock
    private CqlEngineServiceClient cqlEngineServiceClient;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Captor
    private ArgumentCaptor<CareGapEntity> gapCaptor;

    @InjectMocks
    private CareGapIdentificationService service;

    private static final String TENANT_ID = "tenant-123";
    private static final UUID PATIENT_UUID = UUID.randomUUID();

    @Nested
    @DisplayName("Identify Care Gaps for Library Tests")
    class IdentifyGapsForLibraryTests {

        @Test
        @DisplayName("Should create gap when CQL indicates hasGap=true")
        void shouldCreateGapWhenHasGapTrue() {
            // Given - CQL evaluation result
            String cqlResult = """
                {"hasGap":true,"measureId":"HEDIS_CDC_A1C",
                 "measureName":"Diabetes A1C Control","priority":"high",
                 "gapDescription":"A1C test not performed"}""";
            when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), any(), isNull()))
                .thenReturn(cqlResult);
            when(careGapRepository.save(any(CareGapEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            // When
            List<CareGapEntity> gaps = service.identifyCareGapsForLibrary(
                TENANT_ID, PATIENT_UUID, "HEDIS_CDC_A1C", "test-user");

            // Then
            assertThat(gaps).hasSize(1);
            verify(careGapRepository).save(gapCaptor.capture());
            CareGapEntity savedGap = gapCaptor.getValue();
            assertThat(savedGap.getMeasureId()).isEqualTo("HEDIS_CDC_A1C");
            assertThat(savedGap.getPriority()).isEqualTo("high");
            assertThat(savedGap.getGapDescription()).isEqualTo("A1C test not performed");
        }

        @Test
        @DisplayName("Should not create gap when inNumerator=true")
        void shouldNotCreateGapWhenInNumerator() {
            // Given - Patient meets measure criteria
            when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), any(), isNull()))
                .thenReturn("{\"inNumerator\":true,\"measureId\":\"CMS130\"}");

            // When
            List<CareGapEntity> gaps = service.identifyCareGapsForLibrary(
                TENANT_ID, PATIENT_UUID, "CMS_COLORECTAL", "test-user");

            // Then - No gap created
            assertThat(gaps).isEmpty();
            verify(careGapRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Close Care Gap Tests")
    class CloseCareGapTests {

        @Test
        @DisplayName("Should close care gap and publish event")
        void shouldCloseCareGapSuccessfully() {
            // Given
            UUID gapId = UUID.randomUUID();
            CareGapEntity existingGap = CareGapEntity.builder()
                .id(gapId)
                .tenantId(TENANT_ID)
                .patientId(PATIENT_UUID)
                .measureId("CDC_A1C")
                .gapStatus("open")
                .build();

            when(careGapRepository.findByIdAndTenantId(gapId, TENANT_ID))
                .thenReturn(Optional.of(existingGap));
            when(careGapRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(kafkaTemplate.send(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

            // When
            CareGapEntity closedGap = service.closeCareGap(
                TENANT_ID, gapId, "clinician-1", "A1C test performed", "Lab order completed");

            // Then
            assertThat(closedGap.getGapStatus()).isEqualTo("CLOSED");
            assertThat(closedGap.getClosedBy()).isEqualTo("clinician-1");
            assertThat(closedGap.getClosedDate()).isNotNull();

            // Verify Kafka event
            verify(kafkaTemplate).send(eq("care-gap-closed"), anyString());
        }
    }
}
```

### Integration Tests (Repository Layer)

Tests database operations with real PostgreSQL via TestContainers:

```java
@BaseIntegrationTest
@DisplayName("CareGapRepository Integration Tests")
class CareGapRepositoryIntegrationTest {

    @Autowired
    private CareGapRepository careGapRepository;

    private static final String TENANT_ID = "test-tenant";
    private static final String OTHER_TENANT = "other-tenant";
    private static final UUID PATIENT_ID = UUID.randomUUID();

    @Nested
    @DisplayName("Status-Based Queries")
    class StatusBasedQueryTests {

        @Test
        @DisplayName("Should find open care gaps for patient")
        void shouldFindOpenGaps() {
            // Given - setup open and closed gaps
            CareGapEntity openGap = createCareGap(TENANT_ID, PATIENT_ID, "HEDIS_CDC", "OPEN");
            CareGapEntity closedGap = createCareGap(TENANT_ID, PATIENT_ID, "HEDIS_BCS", "CLOSED");
            careGapRepository.saveAll(List.of(openGap, closedGap));

            // When
            List<CareGapEntity> openGaps = careGapRepository.findOpenGapsByPatient(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(openGaps).hasSize(1);
            assertThat(openGaps).allMatch(g -> g.getGapStatus().equals("OPEN"));
        }

        @Test
        @DisplayName("Should find high priority open gaps")
        void shouldFindHighPriorityOpenGaps() {
            // Given
            CareGapEntity highPriority = createCareGap(TENANT_ID, PATIENT_ID, "CRITICAL", "OPEN");
            highPriority.setPriority("high");
            careGapRepository.save(highPriority);

            // When
            List<CareGapEntity> gaps = careGapRepository.findHighPriorityOpenGaps(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(gaps).allMatch(g -> g.getPriority().equals("high"));
        }
    }

    @Nested
    @DisplayName("Date-Based Queries")
    class DateBasedQueryTests {

        @Test
        @DisplayName("Should find overdue care gaps")
        void shouldFindOverdueGaps() {
            // Given - gap with past due date
            CareGapEntity overdueGap = createCareGap(TENANT_ID, PATIENT_ID, "HEDIS_COL", "OPEN");
            overdueGap.setDueDate(LocalDate.now().minusDays(7));
            careGapRepository.save(overdueGap);

            // When
            List<CareGapEntity> overdueGaps = careGapRepository.findOverdueGaps(TENANT_ID, LocalDate.now());

            // Then
            assertThat(overdueGaps).isNotEmpty();
            assertThat(overdueGaps).allMatch(g -> g.getDueDate().isBefore(LocalDate.now()));
        }

        @Test
        @DisplayName("Should find gaps due in date range")
        void shouldFindGapsDueInRange() {
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().plusDays(45);

            List<CareGapEntity> gaps = careGapRepository.findGapsDueInRange(
                TENANT_ID, PATIENT_ID, startDate, endDate);

            assertThat(gaps).allMatch(g ->
                !g.getDueDate().isBefore(startDate) && !g.getDueDate().isAfter(endDate));
        }
    }

    private CareGapEntity createCareGap(String tenantId, UUID patientId, String measureId, String status) {
        return CareGapEntity.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .measureId(measureId)
            .gapStatus(status)
            .priority("medium")
            .gapCategory("HEDIS")
            .dueDate(LocalDate.now().plusDays(30))
            .identifiedDate(Instant.now())
            .createdBy("test-system")
            .build();
    }
}
```

### Multi-Tenant Isolation Tests (HIPAA Compliance)

Verifies tenant data isolation at database layer:

```java
@Nested
@DisplayName("Multi-Tenant Isolation (HIPAA Compliance)")
class MultiTenantIsolationTests {

    @Test
    @DisplayName("Should isolate data between tenants")
    void shouldIsolateDataBetweenTenants() {
        // Given - gaps for different tenants with same patient ID
        CareGapEntity tenant1Gap = createCareGap(TENANT_ID, PATIENT_ID, "HEDIS_CDC", "OPEN");
        CareGapEntity tenant2Gap = createCareGap(OTHER_TENANT, PATIENT_ID, "HEDIS_CDC", "OPEN");
        careGapRepository.saveAll(List.of(tenant1Gap, tenant2Gap));

        // When - query each tenant
        List<CareGapEntity> tenant1Gaps = careGapRepository.findAllOpenGaps(TENANT_ID);
        List<CareGapEntity> tenant2Gaps = careGapRepository.findAllOpenGaps(OTHER_TENANT);

        // Then - no cross-tenant data leakage
        assertThat(tenant1Gaps).noneMatch(g -> g.getTenantId().equals(OTHER_TENANT));
        assertThat(tenant2Gaps).noneMatch(g -> g.getTenantId().equals(TENANT_ID));
    }

    @Test
    @DisplayName("Should not allow cross-tenant access via ID query")
    void shouldNotAllowCrossTenantAccessById() {
        CareGapEntity gap = createCareGap(TENANT_ID, PATIENT_ID, "HEDIS_CDC", "OPEN");
        gap = careGapRepository.save(gap);

        // Attempt to access with wrong tenant
        Optional<CareGapEntity> result = careGapRepository.findByIdAndTenantId(gap.getId(), OTHER_TENANT);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should count only tenant's own gaps")
    void shouldCountOnlyTenantOwnGaps() {
        // Create gaps for both tenants
        careGapRepository.save(createCareGap(TENANT_ID, PATIENT_ID, "GAP1", "OPEN"));
        careGapRepository.save(createCareGap(TENANT_ID, PATIENT_ID, "GAP2", "OPEN"));
        careGapRepository.save(createCareGap(OTHER_TENANT, PATIENT_ID, "GAP3", "OPEN"));

        // Verify counts are isolated
        long tenant1Count = careGapRepository.countOpenGaps(TENANT_ID, PATIENT_ID);
        long tenant2Count = careGapRepository.countOpenGaps(OTHER_TENANT, PATIENT_ID);

        assertThat(tenant1Count).isEqualTo(2);
        assertThat(tenant2Count).isEqualTo(1);
    }
}
```

### Kafka Event Testing

Verifies event publishing for care gap lifecycle:

```java
@Test
@DisplayName("Should publish gap identification event")
void shouldPublishGapIdentificationEvent() {
    // Given
    when(cqlEngineServiceClient.evaluateCql(anyString(), anyString(), any(), isNull()))
        .thenReturn("{\"hasGap\":true,\"measureId\":\"CDC\"}");
    when(careGapRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(kafkaTemplate.send(anyString(), anyString()))
        .thenReturn(CompletableFuture.completedFuture(null));

    // When
    service.identifyAllCareGaps(TENANT_ID, PATIENT_UUID, "test-user");

    // Then
    verify(kafkaTemplate).send(eq("care-gap-identified"), kafkaMessageCaptor.capture());
    String message = kafkaMessageCaptor.getValue();
    assertThat(message).contains(TENANT_ID);
    assertThat(message).contains(PATIENT_UUID.toString());
}

@Test
@DisplayName("Should handle Kafka errors gracefully")
void shouldHandleKafkaErrorsGracefully() {
    // Given - Kafka unavailable
    when(kafkaTemplate.send(anyString(), anyString()))
        .thenThrow(new RuntimeException("Kafka down"));

    // When - should not throw, gap creation should succeed
    CareGapEntity closedGap = service.closeCareGap(
        TENANT_ID, gapId, "user", "reason", "action");

    // Then - gap still closed despite Kafka failure
    assertThat(closedGap.getGapStatus()).isEqualTo("CLOSED");
}
```

### Test Configuration

**BaseIntegrationTest annotation** (simplified):
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import({TestCacheConfiguration.class})
public @interface BaseIntegrationTest {
}
```

**application-test.yml** configuration:
```yaml
spring:
  datasource:
    url: jdbc:tc:postgresql:16:///healthdata_caregap
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
  jpa:
    hibernate:
      ddl-auto: create-drop
  cache:
    type: simple  # In-memory cache for tests
  kafka:
    bootstrap-servers: ${spring.embedded.kafka.brokers:localhost:9092}

# Disable external service calls in tests
cql.engine.url: http://localhost:${wiremock.server.port:8081}/cql-engine
patient.service.url: http://localhost:${wiremock.server.port:8084}/patient
```

### Best Practices

- **HIPAA Compliance**: All test data uses synthetic patterns (TEST-PATIENT-xxx, TEST-MEASURE-xxx)
- **Tenant Isolation**: Every test verifies tenantId filtering in queries
- **Kafka Mocking**: Use `@MockBean KafkaTemplate` for unit tests
- **CQL Engine Mocking**: Mock CqlEngineServiceClient responses
- **Date Testing**: Use `LocalDate.now()` relative dates for robust tests
- **Transactional Rollback**: All integration tests rollback via `@Transactional`

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| TestContainers timeout | Docker not running | Start Docker Desktop |
| Kafka tests fail | Embedded Kafka not starting | Check spring-kafka-test dependency |
| Multi-tenant test fails | Missing tenantId filter | Add WHERE clause to repository query |
| CQL mock not working | Wrong argument matchers | Use `anyString()` or `eq()` consistently |
| Date query fails | Timezone issues | Use `LocalDate` not `Date` |

### Manual Testing (curl examples)

```bash
# Identify all care gaps for patient
curl -X POST http://localhost:8086/care-gap/identify?patient=p123 \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-User-Id: user-001" \
  -H "X-Auth-Roles: EVALUATOR"

# Get open care gaps
curl http://localhost:8086/care-gap/open?patient=p123 \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-Roles: VIEWER"

# Get high priority gaps
curl http://localhost:8086/care-gap/high-priority?patient=p123 \
  -H "X-Tenant-ID: tenant-1"

# Get overdue gaps
curl http://localhost:8086/care-gap/overdue?patient=p123 \
  -H "X-Tenant-ID: tenant-1"

# Close a gap
curl -X POST "http://localhost:8086/care-gap/close?gapId=gap-uuid&closedBy=provider-123" \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-Roles: ADMIN" \
  -d "closureReason=Screening completed&closureAction=Colonoscopy performed"

# Get population report
curl http://localhost:8086/care-gap/population-report \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-Auth-Roles: ANALYST"
```

## Use Cases

- **Quality Measure Reporting**: Identify gaps before CMS submission deadlines
- **Care Coordination**: Prioritized worklists for care managers
- **Value-Based Care**: Track closure rates for HEDIS/Stars improvement
- **Population Health**: Identify cohorts needing outreach (e.g., overdue cancer screenings)
