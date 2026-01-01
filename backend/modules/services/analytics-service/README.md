# Analytics Service

Healthcare analytics and business intelligence service providing dashboards, KPIs, reports, and alerts for value-based care programs.

## Overview

The Analytics Service aggregates data from quality measures, HCC risk scores, and care gaps to provide comprehensive analytics dashboards and KPI tracking for healthcare organizations. It supports customizable dashboards, scheduled reports, trend analysis, and real-time alerting.

## Key Features

### Dashboard Management
- Create and manage custom dashboards
- Widget-based composition (charts, tables, metrics)
- User-specific and organization-wide dashboards
- Role-based dashboard access control
- Real-time dashboard updates

### KPI Tracking
- Quality measure KPIs (HEDIS completion rates)
- HCC risk adjustment KPIs (RAF scores, gaps)
- Care gap KPIs (open gaps, closure rates)
- Trend analysis over configurable time periods
- Automated KPI snapshot capture

### Report Generation
- Define custom report templates
- Parameterized report execution
- Scheduled report generation
- Report execution history and versioning
- Export to multiple formats (PDF, Excel, CSV)

### Real-Time Alerts
- Threshold-based alerting
- Multi-channel notifications (email, SMS, webhook)
- Alert acknowledgment and resolution tracking
- Alert escalation rules
- Alert history and audit trail

### Data Aggregation
- Population-level statistics
- Cohort analysis and segmentation
- Time-series metric aggregation
- Cross-measure correlation analysis

## Technology Stack

- **Spring Boot 3.x**: Core framework
- **PostgreSQL**: Analytics data warehouse
- **Redis**: Caching and real-time updates
- **Apache Kafka**: Event streaming
- **Feign Clients**: Service integration
- **Resilience4j**: Circuit breakers and retry logic

## API Endpoints

### Dashboards
```
GET    /api/analytics/dashboards
       - Get all dashboards for tenant

GET    /api/analytics/dashboards/accessible
       - Get dashboards accessible to current user

GET    /api/analytics/dashboards/{id}
       - Get specific dashboard

POST   /api/analytics/dashboards
       - Create new dashboard

PUT    /api/analytics/dashboards/{id}
       - Update dashboard

DELETE /api/analytics/dashboards/{id}
       - Delete dashboard
```

### Widgets
```
POST   /api/analytics/dashboards/widgets
       - Add widget to dashboard

PUT    /api/analytics/dashboards/widgets/{id}
       - Update widget configuration

DELETE /api/analytics/dashboards/widgets/{id}
       - Remove widget from dashboard
```

### KPIs
```
GET /api/analytics/kpis
    - Get all KPIs for tenant

GET /api/analytics/kpis/quality
    - Get quality measure KPIs

GET /api/analytics/kpis/hcc
    - Get HCC risk adjustment KPIs

GET /api/analytics/kpis/care-gaps
    - Get care gap KPIs

GET /api/analytics/kpis/trends?metricType={type}&days={days}
    - Get KPI trends over time

GET /api/analytics/kpis/statistics
    - Get snapshot statistics

POST /api/analytics/kpis/capture
     - Trigger KPI snapshot capture
```

### Reports
```
GET  /api/analytics/reports
     - List all reports for tenant

GET  /api/analytics/reports/{id}
     - Get specific report definition

POST /api/analytics/reports
     - Create new report template

PUT  /api/analytics/reports/{id}
     - Update report template

DELETE /api/analytics/reports/{id}
       - Delete report

POST /api/analytics/reports/{id}/execute
     - Execute report with parameters

GET  /api/analytics/reports/{id}/executions
     - Get report execution history

GET  /api/analytics/reports/executions/{executionId}
     - Get specific execution result
```

### Alerts
```
GET    /api/analytics/alerts
       - Get all alerts for tenant

GET    /api/analytics/alerts/active
       - Get active/unacknowledged alerts

POST   /api/analytics/alerts
       - Create new alert rule

PUT    /api/analytics/alerts/{id}
       - Update alert rule

DELETE /api/analytics/alerts/{id}
       - Delete alert rule

POST   /api/analytics/alerts/{id}/acknowledge
       - Acknowledge an alert
```

## Configuration

### Application Properties
```yaml
server.port: 8092
spring.datasource.url: jdbc:postgresql://localhost:5435/healthdata_analytics
spring.cache.type: redis
spring.cache.redis.time-to-live: 300000  # 5 minutes
```

### Service Integration
```yaml
feign.client.config:
  quality-measure-service:
    url: http://localhost:8087
  hcc-service:
    url: http://localhost:8091
  care-gap-service:
    url: http://localhost:8086
```

### Resilience
```yaml
resilience4j:
  circuitbreaker:
    instances:
      analyticsDefault:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
```

## Running Locally

### Prerequisites
- Java 21
- PostgreSQL 14+
- Redis 6+
- Kafka 3+

### Build
```bash
./gradlew :modules:services:analytics-service:build
```

### Run
```bash
./gradlew :modules:services:analytics-service:bootRun
```

### Run Tests
```bash
./gradlew :modules:services:analytics-service:test
```

---

## Testing

### Overview

Analytics Service has comprehensive test coverage across 5 test suites with ~2,700 lines of test code, covering KPI aggregation, dashboard management, alert evaluation, report generation, and metric snapshot operations. The service demonstrates unique testing patterns for service fallback mechanisms when external services (quality-measure, HCC, care-gap) are unavailable.

### Quick Start

```bash
# Run all tests
./gradlew :modules:services:analytics-service:test

# Run specific test suite
./gradlew :modules:services:analytics-service:test --tests "*KpiServiceTest"
./gradlew :modules:services:analytics-service:test --tests "*DashboardServiceTest"
./gradlew :modules:services:analytics-service:test --tests "*AlertServiceTest"
./gradlew :modules:services:analytics-service:test --tests "*ReportServiceTest"
./gradlew :modules:services:analytics-service:test --tests "*MetricAggregationServiceTest"

# Run with coverage
./gradlew :modules:services:analytics-service:test jacocoTestReport

# Run only unit tests (fast, no containers)
./gradlew :modules:services:analytics-service:test --tests "*ServiceTest"
```

### Test Organization

```
src/test/java/com/healthdata/analytics/
├── service/
│   ├── KpiServiceTest.java              # KPI aggregation, fallbacks, trends (475 lines)
│   ├── DashboardServiceTest.java        # Dashboard/widget CRUD (495 lines)
│   ├── AlertServiceTest.java            # Alert rules, evaluation, cooldown (716 lines)
│   ├── ReportServiceTest.java           # Report CRUD, execution (612 lines)
│   └── MetricAggregationServiceTest.java # Snapshots, cleanup (369 lines)
├── integration/
│   ├── AnalyticsControllerIntegrationTest.java
│   ├── MultiTenantIsolationTest.java
│   └── KpiAggregationIntegrationTest.java
└── config/
    └── TestCacheConfiguration.java
```

### Unit Tests (Service Layer)

The service layer tests use Mockito for isolated unit testing with extensive use of nested test classes for organization.

**KpiServiceTest.java** - KPI aggregation with external service mocking:
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("KPI Service Tests")
class KpiServiceTest {

    @Mock
    private QualityMeasureClient qualityMeasureClient;

    @Mock
    private HccClient hccClient;

    @Mock
    private CareGapClient careGapClient;

    @Mock
    private MetricSnapshotRepository snapshotRepository;

    private KpiService service;

    private static final String TENANT_ID = "tenant-123";

    @BeforeEach
    void setUp() {
        service = new KpiService(qualityMeasureClient, hccClient, careGapClient, snapshotRepository);
    }

    @Nested
    @DisplayName("Get All KPIs Tests")
    class GetAllKpisTests {

        @Test
        @DisplayName("Should aggregate all KPI categories")
        void shouldAggregateAllKpis() {
            // Given - Mock all three external services
            Map<String, Object> qualitySummary = Map.of(
                    "overallScore", 85.5,
                    "starRating", 4.0,
                    "measuresMet", 15
            );
            Map<String, Object> hccSummary = Map.of(
                    "averageRafScore", 1.25,
                    "rafScoreGap", 0.15,
                    "suspectedHccCount", 45
            );
            Map<String, Object> careGapSummary = Map.of(
                    "openGapCount", 120,
                    "closureRate", 75.5,
                    "highPriorityCount", 25
            );

            when(qualityMeasureClient.getMeasureSummary(TENANT_ID)).thenReturn(qualitySummary);
            when(hccClient.getRafScoreSummary(TENANT_ID)).thenReturn(hccSummary);
            when(careGapClient.getCareGapSummary(TENANT_ID)).thenReturn(careGapSummary);
            when(snapshotRepository.findLatestSnapshots(any(), any(), any(), any()))
                    .thenReturn(List.of());

            // When
            Map<String, Object> result = service.getAllKpis(TENANT_ID);

            // Then
            assertThat(result).containsKeys("quality", "hcc", "careGaps", "asOfDate");
            assertThat((List<?>) result.get("quality")).isNotEmpty();
            assertThat((List<?>) result.get("hcc")).isNotEmpty();
            assertThat((List<?>) result.get("careGaps")).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Get Trends Tests")
    class GetTrendsTests {

        @Test
        @DisplayName("Should return trend data for metric type")
        void shouldReturnTrendData() {
            // Given
            LocalDate today = LocalDate.now();
            List<MetricSnapshotEntity> snapshots = List.of(
                    createSnapshotWithDate("Quality Score", 80.0, today.minusDays(6)),
                    createSnapshotWithDate("Quality Score", 82.0, today.minusDays(4)),
                    createSnapshotWithDate("Quality Score", 85.0, today.minusDays(2)),
                    createSnapshotWithDate("Quality Score", 88.0, today)
            );

            when(snapshotRepository.findByTenantIdAndMetricTypeAndSnapshotDateBetween(
                    eq(TENANT_ID), eq("QUALITY_SCORE"), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(snapshots);

            // When
            List<KpiSummaryDto> result = service.getTrends(TENANT_ID, "QUALITY_SCORE", 7);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTrendData()).hasSize(4);
        }

        @Test
        @DisplayName("Should calculate change percentage from historical data")
        void shouldCalculateChangePercent() {
            // Given
            LocalDate today = LocalDate.now();
            List<MetricSnapshotEntity> snapshots = List.of(
                    createSnapshotWithDate("Quality Score", 80.0, today.minusDays(3)),
                    createSnapshotWithDate("Quality Score", 100.0, today)
            );

            when(snapshotRepository.findByTenantIdAndMetricTypeAndSnapshotDateBetween(
                    eq(TENANT_ID), eq("QUALITY_SCORE"), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(snapshots);

            // When
            List<KpiSummaryDto> result = service.getTrends(TENANT_ID, "QUALITY_SCORE", 7);

            // Then - 25% increase from 80 to 100
            assertThat(result.get(0).getChangePercent()).isEqualTo(BigDecimal.valueOf(25.00).setScale(2));
        }
    }
}
```

**AlertServiceTest.java** - Alert evaluation with cooldown period testing:
```java
@Nested
@DisplayName("Check Alerts Tests")
class CheckAlertsTests {

    @Test
    @DisplayName("Should trigger alert when condition is met")
    void shouldTriggerAlertWhenConditionMet() {
        // Given - Alert rule with GT condition, threshold 80
        AlertRuleEntity rule = createAlertRuleWithCooldown("Quality Alert", "GT",
                BigDecimal.valueOf(80), null); // Never triggered before

        when(alertRepository.findByTenantIdAndIsActiveTrue(TENANT_ID))
                .thenReturn(List.of(rule));

        List<KpiSummaryDto> qualityKpis = List.of(
                KpiSummaryDto.builder()
                        .metricType("QUALITY_SCORE")
                        .metricName("Overall Score")
                        .currentValue(BigDecimal.valueOf(95)) // Above threshold
                        .asOfDate(LocalDate.now())
                        .build()
        );

        when(kpiService.getAllKpis(TENANT_ID)).thenReturn(Map.of(
                "quality", qualityKpis,
                "hcc", List.of(),
                "careGaps", List.of()
        ));
        when(alertRepository.save(any(AlertRuleEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // When
        List<AlertDto> result = service.checkAlerts(TENANT_ID);

        // Then
        assertThat(result).hasSize(1);
        verify(alertRepository).save(any(AlertRuleEntity.class));
    }

    @Test
    @DisplayName("Should respect cooldown period")
    void shouldRespectCooldownPeriod() {
        // Given - Alert triggered 30 min ago, cooldown 60 min
        AlertRuleEntity rule = createAlertRuleWithCooldown("Quality Alert", "GT",
                BigDecimal.valueOf(80), LocalDateTime.now().minusMinutes(30));

        when(alertRepository.findByTenantIdAndIsActiveTrue(TENANT_ID))
                .thenReturn(List.of(rule));

        List<KpiSummaryDto> qualityKpis = List.of(
                KpiSummaryDto.builder()
                        .metricType("QUALITY_SCORE")
                        .metricName("Overall Score")
                        .currentValue(BigDecimal.valueOf(95)) // Above threshold
                        .asOfDate(LocalDate.now())
                        .build()
        );

        when(kpiService.getAllKpis(TENANT_ID)).thenReturn(Map.of(
                "quality", qualityKpis,
                "hcc", List.of(),
                "careGaps", List.of()
        ));

        // When
        List<AlertDto> result = service.checkAlerts(TENANT_ID);

        // Then - Should be blocked by cooldown
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should evaluate LT condition correctly")
    void shouldEvaluateLTCondition() {
        // Given - Low quality alert, threshold 70
        AlertRuleEntity rule = createAlertRuleWithCooldown("Low Quality Alert", "LT",
                BigDecimal.valueOf(70), null);

        // ... test that LT (less than) condition triggers correctly
    }

    @Test
    @DisplayName("Should evaluate EQ condition correctly")
    void shouldEvaluateEQCondition() {
        // Given - Exact match alert
        AlertRuleEntity rule = createAlertRuleWithCooldown("Exact Alert", "EQ",
                BigDecimal.valueOf(100), null);

        // ... test that EQ (equals) condition triggers correctly
    }
}
```

### Service Fallback Tests (Resilience Pattern)

A unique testing pattern in analytics-service is validating fallback behavior when external services fail:

```java
@Nested
@DisplayName("Fallback Tests")
class FallbackTests {

    @Test
    @DisplayName("Should use fallback when client fails")
    void shouldUseFallbackOnFailure() {
        // Given - Quality measure service throws exception
        when(qualityMeasureClient.getMeasureSummary(TENANT_ID))
                .thenThrow(new RuntimeException("Connection failed"));

        // Historical snapshots exist
        when(snapshotRepository.findDistinctMetricNames(TENANT_ID, "QUALITY_SCORE"))
                .thenReturn(List.of("Overall Quality Score"));
        when(snapshotRepository.findLatestSnapshots(eq(TENANT_ID), eq("QUALITY_SCORE"),
                any(), any(PageRequest.class)))
                .thenReturn(List.of(createSnapshot("Overall Quality Score", 85.0)));

        // When
        List<KpiSummaryDto> result = service.getQualityKpis(TENANT_ID);

        // Then - Should return historical data instead of failing
        assertThat(result).isNotEmpty();
        verify(snapshotRepository).findDistinctMetricNames(TENANT_ID, "QUALITY_SCORE");
    }

    @Test
    @DisplayName("Should return historical data on HCC fallback")
    void shouldReturnHistoricalOnHccFallback() {
        // Given
        when(hccClient.getRafScoreSummary(TENANT_ID))
                .thenThrow(new RuntimeException("HCC service unavailable"));
        when(snapshotRepository.findDistinctMetricNames(TENANT_ID, "RAF_SCORE"))
                .thenReturn(List.of("Average RAF Score"));
        when(snapshotRepository.findLatestSnapshots(eq(TENANT_ID), eq("RAF_SCORE"),
                any(), any(PageRequest.class)))
                .thenReturn(List.of(createSnapshot("Average RAF Score", 1.25)));

        // When
        List<KpiSummaryDto> result = service.getHccKpisFallback(TENANT_ID,
                new RuntimeException("Service unavailable"));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCurrentValue()).isEqualTo(BigDecimal.valueOf(1.25));
    }

    @Test
    @DisplayName("Should return empty list when no historical data")
    void shouldReturnEmptyWhenNoHistoricalData() {
        // Given
        when(snapshotRepository.findDistinctMetricNames(TENANT_ID, "QUALITY_SCORE"))
                .thenReturn(List.of());

        // When
        List<KpiSummaryDto> result = service.getQualityKpisFallback(TENANT_ID,
                new RuntimeException("Service unavailable"));

        // Then
        assertThat(result).isEmpty();
    }
}
```

### Integration Tests (API Endpoints)

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestCacheConfiguration.class)
@Testcontainers
class AnalyticsControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    private static final String TENANT_ID = "tenant-integration-001";

    @Test
    void shouldReturnDashboards() throws Exception {
        mockMvc.perform(get("/api/analytics/dashboards")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "user-001")
                .header("X-Auth-Roles", "ANALYST"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldCreateDashboard() throws Exception {
        String dashboardJson = """
            {
                "name": "Quality Overview",
                "description": "HEDIS quality metrics dashboard",
                "isDefault": true,
                "isShared": false
            }
            """;

        mockMvc.perform(post("/api/analytics/dashboards")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "user-001")
                .header("X-Auth-Roles", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(dashboardJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Quality Overview"))
            .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    void shouldReturnKpiTrends() throws Exception {
        mockMvc.perform(get("/api/analytics/kpis/trends")
                .param("metricType", "QUALITY_SCORE")
                .param("days", "30")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "user-001")
                .header("X-Auth-Roles", "ANALYST"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldExecuteReport() throws Exception {
        // First create a report
        String reportJson = """
            {
                "name": "Monthly Quality Report",
                "reportType": "QUALITY",
                "outputFormat": "PDF"
            }
            """;

        String reportResult = mockMvc.perform(post("/api/analytics/reports")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "user-001")
                .header("X-Auth-Roles", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reportJson))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String reportId = JsonPath.parse(reportResult).read("$.id");

        // Then execute it
        mockMvc.perform(post("/api/analytics/reports/" + reportId + "/execute")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "user-001")
                .header("X-Auth-Roles", "ANALYST")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
```

### Multi-Tenant Isolation Tests

```java
@SpringBootTest
@Testcontainers
@DisplayName("Multi-Tenant Isolation Tests")
class MultiTenantIsolationTest {

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private AlertRuleRepository alertRepository;

    @Autowired
    private MetricSnapshotRepository snapshotRepository;

    private static final String TENANT_A = "tenant-alpha";
    private static final String TENANT_B = "tenant-beta";

    @BeforeEach
    void setUp() {
        dashboardRepository.deleteAll();
        alertRepository.deleteAll();
        snapshotRepository.deleteAll();
    }

    @Nested
    @DisplayName("Dashboard Isolation Tests")
    class DashboardIsolationTests {

        @Test
        @DisplayName("Should only return dashboards for specified tenant")
        void shouldIsolateDashboards() {
            // Given
            DashboardEntity dashA = createDashboard(TENANT_A, "Tenant A Dashboard");
            DashboardEntity dashB = createDashboard(TENANT_B, "Tenant B Dashboard");
            dashboardRepository.saveAll(List.of(dashA, dashB));

            // When
            List<DashboardEntity> tenantADashboards = dashboardRepository.findByTenantId(TENANT_A);
            List<DashboardEntity> tenantBDashboards = dashboardRepository.findByTenantId(TENANT_B);

            // Then
            assertThat(tenantADashboards)
                    .hasSize(1)
                    .allMatch(d -> TENANT_A.equals(d.getTenantId()));
            assertThat(tenantBDashboards)
                    .hasSize(1)
                    .allMatch(d -> TENANT_B.equals(d.getTenantId()));

            // Cross-tenant verification
            assertThat(tenantADashboards.get(0).getName()).isEqualTo("Tenant A Dashboard");
            assertThat(tenantBDashboards.get(0).getName()).isEqualTo("Tenant B Dashboard");
        }
    }

    @Nested
    @DisplayName("Alert Rule Isolation Tests")
    class AlertRuleIsolationTests {

        @Test
        @DisplayName("Should only evaluate alerts for specified tenant")
        void shouldIsolateAlertEvaluation() {
            // Given
            AlertRuleEntity alertA = createAlertRule(TENANT_A, "Quality Alert A", BigDecimal.valueOf(80));
            AlertRuleEntity alertB = createAlertRule(TENANT_B, "Quality Alert B", BigDecimal.valueOf(90));
            alertRepository.saveAll(List.of(alertA, alertB));

            // When
            List<AlertRuleEntity> tenantAAlerts = alertRepository.findByTenantIdAndIsActiveTrue(TENANT_A);

            // Then
            assertThat(tenantAAlerts)
                    .hasSize(1)
                    .allMatch(a -> TENANT_A.equals(a.getTenantId()))
                    .allMatch(a -> a.getThresholdValue().equals(BigDecimal.valueOf(80)));
        }
    }

    @Nested
    @DisplayName("Metric Snapshot Isolation Tests")
    class MetricSnapshotIsolationTests {

        @Test
        @DisplayName("Should isolate metric snapshots by tenant")
        void shouldIsolateSnapshots() {
            // Given
            MetricSnapshotEntity snapA = createSnapshot(TENANT_A, "QUALITY_SCORE", BigDecimal.valueOf(85));
            MetricSnapshotEntity snapB = createSnapshot(TENANT_B, "QUALITY_SCORE", BigDecimal.valueOf(72));
            snapshotRepository.saveAll(List.of(snapA, snapB));

            // When
            List<MetricSnapshotEntity> tenantASnapshots = snapshotRepository
                    .findByTenantIdAndMetricTypeAndSnapshotDateBetween(
                            TENANT_A, "QUALITY_SCORE",
                            LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));

            // Then
            assertThat(tenantASnapshots)
                    .hasSize(1)
                    .allMatch(s -> TENANT_A.equals(s.getTenantId()))
                    .allMatch(s -> s.getMetricValue().equals(BigDecimal.valueOf(85)));
        }

        @Test
        @DisplayName("Should not leak sensitive KPI data across tenants")
        void shouldNotLeakKpiDataAcrossTenants() {
            // Given - Tenant A has sensitive financial metrics
            MetricSnapshotEntity revenueA = createSnapshot(TENANT_A, "REVENUE", BigDecimal.valueOf(1500000));
            revenueA.setDimensions(Map.of("contract", "Contract-A-Secret"));

            MetricSnapshotEntity revenueB = createSnapshot(TENANT_B, "REVENUE", BigDecimal.valueOf(800000));
            revenueB.setDimensions(Map.of("contract", "Contract-B"));

            snapshotRepository.saveAll(List.of(revenueA, revenueB));

            // When - Tenant B queries revenue
            List<MetricSnapshotEntity> tenantBRevenue = snapshotRepository
                    .findByTenantIdAndMetricTypeAndSnapshotDateBetween(
                            TENANT_B, "REVENUE",
                            LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));

            // Then - Should only see Tenant B's revenue
            assertThat(tenantBRevenue)
                    .hasSize(1)
                    .allMatch(s -> !s.getDimensions().toString().contains("Secret"));
        }
    }
}
```

### RBAC/Permission Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AnalyticsRbacTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String TENANT_ID = "tenant-rbac-001";

    @Nested
    @DisplayName("Dashboard Access Control")
    class DashboardAccessControl {

        @Test
        @DisplayName("Admin can create dashboards")
        void adminCanCreateDashboards() throws Exception {
            mockMvc.perform(post("/api/analytics/dashboards")
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "admin-001")
                    .header("X-Auth-Roles", "ADMIN")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\": \"Admin Dashboard\"}"))
                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Analyst can view dashboards but not create")
        void analystCanViewNotCreate() throws Exception {
            // Can view
            mockMvc.perform(get("/api/analytics/dashboards")
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "analyst-001")
                    .header("X-Auth-Roles", "ANALYST"))
                .andExpect(status().isOk());

            // Cannot create
            mockMvc.perform(post("/api/analytics/dashboards")
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "analyst-001")
                    .header("X-Auth-Roles", "ANALYST")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\": \"Analyst Dashboard\"}"))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Viewer has read-only access")
        void viewerHasReadOnlyAccess() throws Exception {
            // Can view KPIs
            mockMvc.perform(get("/api/analytics/kpis")
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "viewer-001")
                    .header("X-Auth-Roles", "VIEWER"))
                .andExpect(status().isOk());

            // Cannot create alerts
            mockMvc.perform(post("/api/analytics/alerts")
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "viewer-001")
                    .header("X-Auth-Roles", "VIEWER")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\": \"Viewer Alert\"}"))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Alert Management Access Control")
    class AlertAccessControl {

        @Test
        @DisplayName("Admin can manage alert rules")
        void adminCanManageAlerts() throws Exception {
            mockMvc.perform(post("/api/analytics/alerts")
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "admin-001")
                    .header("X-Auth-Roles", "ADMIN")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "name": "Quality Alert",
                            "metricType": "QUALITY_SCORE",
                            "conditionOperator": "LT",
                            "thresholdValue": 70,
                            "severity": "HIGH"
                        }
                        """))
                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Analyst can view but not create alerts")
        void analystCannotCreateAlerts() throws Exception {
            mockMvc.perform(post("/api/analytics/alerts")
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "analyst-001")
                    .header("X-Auth-Roles", "ANALYST")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\": \"Analyst Alert\"}"))
                .andExpect(status().isForbidden());
        }
    }
}
```

### HIPAA Compliance Tests

```java
@SpringBootTest
@Testcontainers
@DisplayName("HIPAA Compliance Tests")
class AnalyticsHipaaComplianceTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private MockMvc mockMvc;

    private static final String TENANT_ID = "tenant-hipaa-001";

    @Nested
    @DisplayName("Cache TTL Compliance")
    class CacheTtlComplianceTests {

        @Test
        @DisplayName("KPI cache TTL should not exceed 5 minutes")
        void kpiCacheTtlShouldBeCompliant() {
            // Given
            Cache kpiCache = cacheManager.getCache("kpi-summaries");

            // Then
            assertThat(kpiCache).isNotNull();

            if (kpiCache instanceof RedisCache) {
                RedisCacheConfiguration config = ((RedisCache) kpiCache).getCacheConfiguration();
                assertThat(config.getTtl().getSeconds())
                        .isLessThanOrEqualTo(300L)
                        .withFailMessage("KPI cache TTL exceeds 5 minutes (HIPAA violation)");
            }
        }

        @Test
        @DisplayName("Dashboard cache TTL should not exceed 5 minutes")
        void dashboardCacheTtlShouldBeCompliant() {
            Cache dashboardCache = cacheManager.getCache("dashboards");

            assertThat(dashboardCache).isNotNull();

            if (dashboardCache instanceof RedisCache) {
                RedisCacheConfiguration config = ((RedisCache) dashboardCache).getCacheConfiguration();
                assertThat(config.getTtl().getSeconds())
                        .isLessThanOrEqualTo(300L)
                        .withFailMessage("Dashboard cache TTL exceeds 5 minutes (HIPAA violation)");
            }
        }
    }

    @Nested
    @DisplayName("No-Cache Headers")
    class NoCacheHeadersTests {

        @Test
        @DisplayName("KPI responses should include no-cache headers")
        void kpiResponsesShouldIncludeNoCacheHeaders() throws Exception {
            mockMvc.perform(get("/api/analytics/kpis")
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "user-001")
                    .header("X-Auth-Roles", "ANALYST"))
                .andExpect(header().string("Cache-Control",
                        containsString("no-store")))
                .andExpect(header().string("Pragma", "no-cache"));
        }

        @Test
        @DisplayName("Report results should include no-cache headers")
        void reportResultsShouldIncludeNoCacheHeaders() throws Exception {
            mockMvc.perform(get("/api/analytics/reports/123/executions/456")
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-Auth-User-Id", "user-001")
                    .header("X-Auth-Roles", "ANALYST"))
                .andExpect(header().string("Cache-Control",
                        allOf(
                                containsString("no-store"),
                                containsString("no-cache"),
                                containsString("must-revalidate")
                        )));
        }
    }

    @Nested
    @DisplayName("Synthetic Data Validation")
    class SyntheticDataValidationTests {

        @Test
        @DisplayName("Test metric snapshots should use synthetic patterns")
        void testDataShouldBeSynthetic() {
            // Verify test data generators use synthetic patterns
            MetricSnapshotEntity snapshot = createTestSnapshot();

            assertThat(snapshot.getTenantId())
                    .matches("tenant-test-\\d{3}|tenant-\\d{3}")
                    .withFailMessage("Tenant ID should follow synthetic pattern");

            assertThat(snapshot.getMetricName())
                    .doesNotContainIgnoringCase("patient")
                    .doesNotContainIgnoringCase("social security")
                    .withFailMessage("Metric names in tests should not contain PHI-related terms");
        }
    }
}
```

### Performance Tests

```java
@SpringBootTest
@Testcontainers
@DisplayName("Performance Tests")
class AnalyticsPerformanceTest {

    @Autowired
    private KpiService kpiService;

    @Autowired
    private MetricSnapshotRepository snapshotRepository;

    private static final String TENANT_ID = "tenant-perf-001";

    @Test
    @DisplayName("KPI aggregation should complete within 500ms")
    void kpiAggregationPerformance() {
        // Given - Pre-populate snapshots
        List<MetricSnapshotEntity> snapshots = IntStream.range(0, 1000)
                .mapToObj(i -> createSnapshot(TENANT_ID, "QUALITY_SCORE", BigDecimal.valueOf(70 + (i % 30))))
                .collect(Collectors.toList());
        snapshotRepository.saveAll(snapshots);

        // When
        Instant start = Instant.now();
        Map<String, Object> result = kpiService.getAllKpis(TENANT_ID);
        Instant end = Instant.now();

        // Then
        long duration = Duration.between(start, end).toMillis();
        assertThat(duration)
                .isLessThan(500L)
                .withFailMessage("KPI aggregation took %dms (exceeds 500ms SLA)", duration);

        System.out.printf("KPI aggregation: %dms%n", duration);
    }

    @Test
    @DisplayName("Trend calculation should complete within 200ms for 90-day range")
    void trendCalculationPerformance() {
        // Given - 90 days of snapshots
        LocalDate today = LocalDate.now();
        List<MetricSnapshotEntity> snapshots = IntStream.range(0, 90)
                .mapToObj(i -> {
                    MetricSnapshotEntity snapshot = createSnapshot(TENANT_ID, "QUALITY_SCORE",
                            BigDecimal.valueOf(75 + (i % 15)));
                    snapshot.setSnapshotDate(today.minusDays(i));
                    return snapshot;
                })
                .collect(Collectors.toList());
        snapshotRepository.saveAll(snapshots);

        // When
        Instant start = Instant.now();
        List<KpiSummaryDto> trends = kpiService.getTrends(TENANT_ID, "QUALITY_SCORE", 90);
        Instant end = Instant.now();

        // Then
        long duration = Duration.between(start, end).toMillis();
        assertThat(duration)
                .isLessThan(200L)
                .withFailMessage("Trend calculation took %dms (exceeds 200ms SLA)", duration);

        System.out.printf("Trend calculation (90 days): %dms, %d data points%n",
                duration, trends.get(0).getTrendData().size());
    }

    @Test
    @DisplayName("Dashboard widget loading should complete within 300ms")
    void dashboardLoadingPerformance() {
        // Given - Dashboard with multiple widgets
        DashboardEntity dashboard = createDashboardWithWidgets(TENANT_ID, 10);
        dashboardRepository.save(dashboard);

        // When
        List<Long> latencies = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Instant start = Instant.now();
            dashboardService.getDashboard(dashboard.getId(), TENANT_ID);
            Instant end = Instant.now();
            latencies.add(Duration.between(start, end).toMillis());
        }

        // Then
        Collections.sort(latencies);
        long p95 = latencies.get(94);
        assertThat(p95)
                .isLessThan(300L)
                .withFailMessage("Dashboard loading p95 %dms exceeds 300ms SLA", p95);

        System.out.printf("Dashboard loading: p50=%dms, p95=%dms, p99=%dms%n",
                latencies.get(49), p95, latencies.get(98));
    }
}
```

### Test Configuration

**TestCacheConfiguration.java** - HIPAA-compliant 5-minute TTL:
```java
@Configuration
public class TestCacheConfiguration {

    @Bean
    public CacheManager cacheManager() {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))  // HIPAA: 5 minutes max for analytics data
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(redisConnectionFactory())
                .cacheDefaults(config)
                .withCacheConfiguration("kpi-summaries", config)
                .withCacheConfiguration("dashboards", config)
                .withCacheConfiguration("alerts", config)
                .build();
    }
}
```

### Best Practices

| Practice | Description |
|----------|-------------|
| **Feign Client Mocking** | Mock external service clients (QualityMeasureClient, HccClient, CareGapClient) to test aggregation logic independently |
| **Fallback Testing** | Always test resilience fallbacks when external services fail - return historical data from snapshots |
| **Cooldown Period Testing** | Test alert cooldown logic to prevent notification spam |
| **Trend Calculation Testing** | Verify change percentage calculations with boundary cases (division by zero, negative changes) |
| **Async Execution Testing** | Test report execution status transitions (PENDING → RUNNING → COMPLETED/FAILED) |
| **Widget Ordering** | Verify dashboard widgets are returned in correct position order (positionY, positionX) |
| **Synthetic Data** | Use clearly synthetic tenant IDs (tenant-test-xxx) and metric values |
| **Time-Based Queries** | Test date range queries with edge cases (same day, crossing month boundaries) |

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| `FeignException.NotFound` in tests | External service mock not configured | Add `@MockBean` for all Feign clients |
| Cache tests fail | Redis container not started | Verify `@Testcontainers` and `@Container` annotations |
| Trend data empty | No snapshots in date range | Ensure test data spans the queried date range |
| Alert not triggering | Cooldown period active | Set `lastTriggeredAt` to null or past cooldown period |
| Report execution stuck | Async executor not configured | Add `@EnableAsync` to test configuration |
| Multi-tenant data leakage | Missing tenantId in repository query | Verify `findByTenantId*` methods are used |
| Dashboard widgets missing | Widgets not linked to dashboard | Verify `dashboardId` is set on widget entities |

### Manual Testing

**Get All KPIs:**
```bash
curl -X GET "http://localhost:8092/api/analytics/kpis" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-Auth-User-Id: user-001" \
  -H "X-Auth-Roles: ANALYST"
```

**Get KPI Trends:**
```bash
curl -X GET "http://localhost:8092/api/analytics/kpis/trends?metricType=QUALITY_SCORE&days=30" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-Auth-User-Id: user-001" \
  -H "X-Auth-Roles: ANALYST"
```

**Create Dashboard:**
```bash
curl -X POST "http://localhost:8092/api/analytics/dashboards" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-Auth-User-Id: user-001" \
  -H "X-Auth-Roles: ADMIN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Quality Overview",
    "description": "HEDIS quality metrics dashboard",
    "isDefault": true,
    "isShared": true
  }'
```

**Add Widget to Dashboard:**
```bash
curl -X POST "http://localhost:8092/api/analytics/dashboards/widgets" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-Auth-User-Id: user-001" \
  -H "X-Auth-Roles: ADMIN" \
  -H "Content-Type: application/json" \
  -d '{
    "dashboardId": "uuid-here",
    "widgetType": "CHART",
    "title": "Quality Score Trend",
    "positionX": 0,
    "positionY": 0,
    "width": 6,
    "height": 4,
    "configuration": {
      "chartType": "line",
      "metricType": "QUALITY_SCORE",
      "days": 30
    }
  }'
```

**Create Alert Rule:**
```bash
curl -X POST "http://localhost:8092/api/analytics/alerts" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-Auth-User-Id: user-001" \
  -H "X-Auth-Roles: ADMIN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Low Quality Score Alert",
    "metricType": "QUALITY_SCORE",
    "metricName": "Overall Score",
    "conditionOperator": "LT",
    "thresholdValue": 70,
    "severity": "HIGH",
    "cooldownMinutes": 60,
    "isActive": true
  }'
```

**Execute Report:**
```bash
curl -X POST "http://localhost:8092/api/analytics/reports/{reportId}/execute" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-Auth-User-Id: user-001" \
  -H "X-Auth-Roles: ANALYST" \
  -H "Content-Type: application/json" \
  -d '{
    "startDate": "2024-01-01",
    "endDate": "2024-12-31",
    "format": "PDF"
  }'
```

**Capture KPI Snapshot:**
```bash
curl -X POST "http://localhost:8092/api/analytics/kpis/capture" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-Auth-User-Id: user-001" \
  -H "X-Auth-Roles: ADMIN"
```

---

## Integration

This service integrates with:
- **Quality Measure Service**: HEDIS measure results
- **HCC Service**: Risk adjustment scores
- **Care Gap Service**: Care gap data
- **Predictive Analytics**: Risk predictions and forecasts

## Security

- JWT-based authentication
- Role-based access control (USER, ANALYST, ADMIN)
- Tenant isolation via X-Tenant-ID header
- Dashboard-level access controls
- Audit logging for sensitive operations

## Data Refresh

### KPI Snapshots
- Automated capture via scheduled jobs
- On-demand capture via API
- Configurable snapshot intervals
- Historical trend preservation

### Cache Strategy
- 5-minute TTL for KPI data
- Immediate invalidation on data updates
- Redis-based distributed caching

## API Documentation

Swagger UI available at:
```
http://localhost:8092/analytics/swagger-ui.html
```

## Monitoring

Actuator endpoints:
- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`

## License

Copyright (c) 2024 Mahoosuc Solutions
