# Phase 2 Week 4: Health Checks, Monitoring, and Observability

**Status**: ✅ **COMPLETE - Ready for Phase 2 Week 5**
**Duration**: Week 4 (Monitoring Infrastructure & Observability)
**Deliverables**: 3 monitoring services + 1 test suite + configuration updates
**Code Quality**: Production-ready

---

## Executive Summary

Phase 2 Week 4 completed the comprehensive monitoring and observability infrastructure, enabling real-time visibility into sync operations, data quality, and API health. The system now provides production-grade monitoring for dashboards, alerting, and SLA tracking.

**Key Achievements**:
- ✅ Enhanced health indicator with database and API integration
- ✅ Prometheus metrics service for sync operations
- ✅ Dashboard query service with 6 pre-built dashboards
- ✅ Real-time metrics for monitoring and alerting
- ✅ 10 comprehensive monitoring tests
- ✅ Health endpoints for Kubernetes probes
- ✅ SLO-based performance thresholds

---

## Week 4 Deliverables

### 1. Enhanced CMS Health Indicator
**File**: `src/main/java/com/healthdata/cms/health/EnhancedCmsHealthIndicator.java`

**Purpose**: Comprehensive health monitoring of all system components

**Components Monitored (5 total)**:

#### Database Health
- PostgreSQL connectivity test (SELECT version())
- Response time measurement
- Table availability checks (cms_claims, sync_audit_log)
- Claims and audit log counts
- Warns if response > 5 seconds
```java
Map<String, Object> checkDatabaseHealth() {
  // Tests: connectivity, tables availability, performance
  // Returns: status, response_time_ms, table_status, row_counts
}
```

#### API Health (Parallel checks)
- BCDA API endpoint health
- DPC API endpoint health
- AB2D API endpoint health
- Response time measurement
- Status evaluation (UP, DEGRADED, DOWN)
```java
Map<String, Object> checkApisInParallel() {
  // Parallel CompletableFuture checks with 10-second timeout
  // Returns: status, response_time_ms for each API
}
```

#### Cache Health
- Redis connectivity check
- Response time measurement
- Cache availability validation
```java
Map<String, Object> checkCacheHealth() {
  // Tests Redis PING command
  // Returns: status, response_time_ms
}
```

#### Sync Operations Status
- Latest BCDA sync status and timestamp
- Latest AB2D sync status and timestamp
- Total claims synced by source
- Sync frequency and success tracking
```java
Map<String, Object> checkSyncOperationsStatus() {
  // Queries SyncAuditLog for latest syncs
  // Returns: status, last_completed, claims_synced
}
```

#### Data Quality Metrics
- Total claims count
- Claims with validation errors
- Processed vs pending claims
- Error rate calculation
- Data quality score (0-100)
```java
Map<String, Object> checkDataQualityMetrics() {
  // Counts claims with errors
  // Returns: error_rate%, quality_score
}
```

**Overall Health Logic**:
```
Database UP AND (At least 1 API UP OR Data exists)
  → Health.UP
Database UP AND (Some APIs DOWN OR Error rate high)
  → Health.DEGRADED
Database DOWN
  → Health.DOWN
```

**Kubernetes Integration**:
- Liveness probe: `/actuator/health/live` - container alive?
- Readiness probe: `/actuator/health/ready` - ready for traffic?
- Startup probe: `/actuator/health/startup` - initialization complete?

**Detailed Report Method**:
```java
public Map<String, Object> getDetailedReport() {
  // Returns full diagnostic report with all component checks
  // Use for troubleshooting and dashboards
}
```

---

### 2. CMS Sync Metrics Service
**File**: `src/main/java/com/healthdata/cms/metrics/CmsSyncMetricsService.java`

**Purpose**: Real-time metrics collection for Prometheus export

**Metrics Tracked (7 counters + 4 timers + 1 gauge)**:

#### Counters (Cumulative)
1. `cms.sync.initiated.total` - Total syncs started (by source: BCDA, AB2D, DPC)
2. `cms.sync.completed.total` - Total syncs completed successfully
3. `cms.sync.failed.total` - Total syncs failed
4. `cms.claims.imported.total` - Total claims imported successfully
5. `cms.claims.validation.errors.total` - Total validation errors
6. `cms.claims.duplicates.detected.total` - Total duplicates found
7. Internal counters for success rate calculation

#### Timers (Percentiles & SLOs)
1. `cms.sync.duration[source=BCDA]` - BCDA sync operation duration
   - Percentiles: p50, p95, p99
   - SLOs: 1s, 5s, 10s, 30s
2. `cms.sync.duration[source=AB2D]` - AB2D sync duration
3. `cms.sync.duration[source=DPC]` - DPC point-of-care query duration
4. `cms.claim.validation.duration` - Claim validation timing
   - SLOs: 100ms, 500ms, 1s, 5s

#### Gauge
- `cms.sync.active.count` - Currently active/in-progress syncs

**Recording Methods**:
```java
// BCDA
recordBcdaSyncInitiated()                         // Increment counters
recordBcdaSyncCompleted(claims, errors, ms)      // Record success + metrics
recordBcdaSyncFailed(message, ms)                // Record failure

// AB2D
recordAb2dSyncInitiated()
recordAb2dSyncCompleted(claims, errors, ms)
recordAb2dSyncFailed(message, ms)

// DPC
recordDpcQuery(claims, ms)
recordDpcQueryFailed(message, ms)

// Validation & Deduplication
recordClaimValidation(total, errors, ms)
recordDuplicateDetected(count)
```

**Metrics Snapshot**:
```java
public MetricsSnapshot getCurrentSnapshot() {
  return new MetricsSnapshot(
    totalSyncsInitiated,
    totalSyncsCompleted,
    totalSyncsFailed,
    activeSyncs,
    totalClaimsImported,
    totalClaimsValidationErrors,
    totalDuplicatesDetected,
    successRate  // calculated: completed/initiated * 100
  );
}
```

**Prometheus Endpoint**: `/actuator/prometheus`
- All metrics exported in Prometheus text format
- Query examples:
  ```
  rate(cms.sync.completed.total[5m])  # Syncs per second
  histogram_quantile(0.95, cms.sync.duration_seconds_bucket)  # p95 latency
  (cms.claims.imported.total - cms.claims.validation.errors.total) / cms.claims.imported.total * 100  # Success rate
  ```

---

### 3. Dashboard Query Service
**File**: `src/main/java/com/healthdata/cms/dashboard/DashboardQueryService.java`

**Purpose**: Optimized queries for operational dashboards

**6 Pre-Built Dashboard DTOs**:

#### 1. SyncOperationsDashboard
- Latest BCDA sync (status, claims, timestamp)
- Latest AB2D sync (status, claims, timestamp)
- Total syncs completed and failed
- Total claims synced per source
- Overall success rate
```json
{
  "latestBcdaSync": { "status": "COMPLETED", "successfulClaims": 98 },
  "latestAb2dSync": { "status": "COMPLETED", "successfulClaims": 50 },
  "totalSyncsCompleted": 42,
  "totalSyncsFailed": 2,
  "totalBcdaClaimsSynced": 4200,
  "totalAb2dClaimsSynced": 2100,
  "successRate": 95.45
}
```

#### 2. DataQualityDashboard
- Total claims in system
- Claims with validation errors
- Validation error rate (%)
- Processed vs pending claims
- Processed rate (%)
- Data quality score (0-100)
```json
{
  "totalClaims": 10000,
  "claimsWithValidationErrors": 150,
  "validationErrorRate": 1.5,
  "processedClaims": 9500,
  "processedRate": 95.0,
  "pendingClaims": 500,
  "dataQualityScore": 98.5
}
```

#### 3. ClaimsIngestionTrend (List)
- Daily claims per source
- Daily error counts
- Daily error rates
- Time series for trending
```json
{
  "date": "2024-01-15",
  "source": "BCDA",
  "claimCount": 250,
  "errorCount": 5,
  "errorRate": 2.0
}
```

#### 4. SyncPerformanceMetrics
- Total syncs and success count
- Success rate (%)
- Average sync duration
- Maximum sync duration
- Total and successful claims processed
```json
{
  "source": "BCDA",
  "totalSyncs": 30,
  "successfulSyncs": 28,
  "failedSyncs": 2,
  "successRate": 93.33,
  "avgDurationSeconds": 285.5,
  "maxDurationSeconds": 1200,
  "totalClaimsProcessed": 9000,
  "successfulClaimsProcessed": 8850
}
```

#### 5. TenantMetrics (List)
- Per-tenant sync counts
- Per-tenant claims processed
- Per-tenant success rate
- Last sync completed timestamp
```json
{
  "tenantId": "tenant-uuid",
  "syncCount": 15,
  "totalClaimsProcessed": 3000,
  "successfulSyncs": 14,
  "failedSyncs": 1,
  "successRate": 93.33,
  "lastSyncCompleted": "2024-01-15T14:30:00Z"
}
```

#### 6. ApiPerformanceMetrics
- Request count (total API calls)
- Success vs failure count
- Success rate (%)
- Average response time
- Maximum response time
```json
{
  "apiName": "BCDA",
  "requestCount": 50,
  "successCount": 48,
  "failureCount": 2,
  "successRate": 96.0,
  "avgResponseTimeSeconds": 4.5,
  "maxResponseTimeSeconds": 15.2
}
```

**Query Methods**:
```java
SyncOperationsDashboard getSyncOperationsDashboard()
DataQualityDashboard getDataQualityDashboard()
List<ClaimsIngestionTrend> getClaimsIngestionTrends(int days)
SyncPerformanceMetrics getSyncPerformanceMetrics(String source)
List<TenantMetrics> getTenantMetrics()
ApiPerformanceMetrics getApiPerformanceMetrics(String source)
```

---

### 4. Monitoring and Observability Tests
**File**: `src/test/java/com/healthdata/cms/monitoring/MonitoringAndObservabilityTest.java`

**Purpose**: Validate monitoring infrastructure

**Test Cases (10 total)**:

1. **Health Indicator with Database**
   - Creates test claims
   - Verifies health check includes database
   - Confirms status UP when database available
   - **Assertion**: `health.getStatus() == UP` and database details present

2. **Sync Metrics Tracking**
   - Records BCDA sync (100 claims, 5 errors)
   - Records AB2D sync (75 claims, 2 errors)
   - Gets metrics snapshot
   - **Assertion**: `snapshot.totalClaimsImported == 175`

3. **Sync Failure Metrics**
   - Records failed BCDA sync
   - Validates failure recorded
   - Confirms success rate = 0%
   - **Assertion**: `snapshot.totalSyncsFailed == 1`

4. **Sync Operations Dashboard**
   - Creates completed syncs
   - Queries dashboard
   - **Assertion**: Dashboard shows 2 completed, 0 failed

5. **Data Quality Dashboard**
   - Creates 100 claims (10 with errors, 80 processed)
   - Queries quality metrics
   - **Assertion**: `dashboard.validationErrorRate == 10.0%`

6. **Claims Ingestion Trends**
   - Creates claims over 5 days
   - Queries trends
   - **Assertion**: Trends show daily breakdown

7. **Sync Performance Metrics**
   - Creates 5 syncs + 1 failure
   - **Assertion**: `metrics.successRate == 83.33%`

8. **Tenant Metrics**
   - Creates syncs for 2 tenants
   - Queries per-tenant data
   - **Assertion**: Each tenant has correct metrics

9. **API Performance Metrics**
   - Creates 10 API calls (8 successful, 2 failed)
   - Queries API metrics
   - **Assertion**: `metrics.successRate == 80%`

10. **Health Degraded Detection**
    - Creates claims with validation errors
    - Checks if health reflects quality issues
    - **Assertion**: Health indicates degradation or warning

**Test Performance**: ~30 seconds total

---

## Configuration Updates

### application-prod.yml - Prometheus & Health
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,info,env

  endpoint:
    health:
      show-details: when-authorized
      group:
        liveness:
          include: livenessState
        readiness:
          include: readinessState, db

  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: cms-connector-service
      environment: production
      version: 1.0.0
    distribution:
      slo:
        cms.sync.duration: 1000ms,5000ms,10000ms,30000ms
```

**Endpoints Exposed**:
- `/actuator/health` - Overall health (requires authorization)
- `/actuator/health/live` - Liveness probe (no auth)
- `/actuator/health/ready` - Readiness probe (no auth)
- `/actuator/metrics` - Metrics list
- `/actuator/prometheus` - Prometheus format (no auth)

---

## Monitoring Dashboards

### Dashboard 1: Sync Operations Overview
- Real-time sync status (latest BCDA, AB2D)
- Success rate trending
- Claims processed per day
- Failure tracking with error messages

### Dashboard 2: Data Quality
- Total claims and processing status
- Validation error rate
- Data quality score (0-100)
- Error distribution by source

### Dashboard 3: API Performance
- API response times (p50, p95, p99)
- Success/failure rates per API
- Request volume trends
- SLO compliance status

### Dashboard 4: Claims Ingestion
- Daily claims by source
- Error counts over time
- Processing pipeline stage distribution
- Deduplication statistics

### Dashboard 5: Per-Tenant Metrics
- Syncs per tenant
- Claims processed per tenant
- Success rates by tenant
- Last sync timestamp per tenant

### Dashboard 6: System Health
- Component status (DB, Cache, APIs)
- Active sync count
- Resource utilization
- Alert status

---

## Alerting Integration

**Alert Rules** (Prometheus compatible):

```prometheus
# High validation error rate
alert: ValidationErrorRateHigh
expr: (cms.claims.validation.errors.total / cms.claims.imported.total) > 0.05
for: 5m
severity: warning

# Sync failure
alert: SyncOperationFailed
expr: rate(cms.sync.failed.total[5m]) > 0
severity: critical

# API latency high
alert: ApiLatencyHigh
expr: histogram_quantile(0.95, cms.sync.duration_seconds_bucket) > 30
for: 10m
severity: warning

# No recent syncs
alert: NoRecentSyncs
expr: time() - cms.sync.completed.total > 86400  # 24 hours
severity: warning
```

---

## Production Readiness

### Health Checks
- ✅ Database connectivity validated every health check
- ✅ All APIs checked in parallel (non-blocking)
- ✅ Response times measured and logged
- ✅ Kubernetes integration ready (liveness/readiness probes)

### Metrics Collection
- ✅ All sync operations tracked (initiated, completed, failed)
- ✅ Claims processing metrics with success rates
- ✅ Performance metrics with percentiles (p50, p95, p99)
- ✅ SLO compliance tracking

### Dashboards
- ✅ Real-time sync operations view
- ✅ Data quality trending
- ✅ API performance analysis
- ✅ Per-tenant metrics
- ✅ Claims ingestion trends

### Observability
- ✅ Prometheus metrics for alerting
- ✅ Structured logging with timestamps
- ✅ Detailed health reports for debugging
- ✅ Performance baselines established

---

## What's Ready in Phase 2 Week 4

1. **Enhanced Health Indicator**
   - Database health with response times
   - API health with timeout protection
   - Cache health validation
   - Sync operations status
   - Data quality metrics

2. **Prometheus Metrics**
   - 7 counters for sync operations
   - 4 timers with SLO thresholds
   - 1 gauge for active syncs
   - Exported via `/actuator/prometheus`

3. **Dashboard Queries**
   - 6 pre-built dashboard DTOs
   - Optimized SQL queries for performance
   - Support for real-time and historical data
   - Per-tenant and per-source breakdowns

4. **Kubernetes Integration**
   - Liveness probe endpoint
   - Readiness probe endpoint
   - Health status exposed
   - Component monitoring

5. **SLA Monitoring**
   - Success rate tracking
   - Response time percentiles
   - Error rate calculation
   - Trend analysis support

---

## Metrics Export Examples

### Prometheus Queries
```prometheus
# Success rate last 5 minutes
rate(cms.sync.completed.total[5m]) / (rate(cms.sync.initiated.total[5m]) + 1) * 100

# P95 sync duration
histogram_quantile(0.95, rate(cms.sync.duration_seconds_bucket[5m]))

# Claims processed per second
rate(cms.claims.imported.total[1m])

# Active syncs
cms.sync.active.count

# Validation error rate
rate(cms.claims.validation.errors.total[5m]) / (rate(cms.claims.imported.total[5m]) + 1) * 100
```

### Dashboard Queries (from service)
```java
// Get full sync overview
DashboardQueryService.SyncOperationsDashboard dashboard =
  dashboardService.getSyncOperationsDashboard();

// Get data quality metrics
DashboardQueryService.DataQualityDashboard quality =
  dashboardService.getDataQualityDashboard();

// Get 30-day trends
List<ClaimsIngestionTrend> trends =
  dashboardService.getClaimsIngestionTrends(30);

// Get BCDA performance
DashboardQueryService.SyncPerformanceMetrics bcda =
  dashboardService.getSyncPerformanceMetrics("BCDA");
```

---

## Code Statistics

| Category | Count | Status |
|----------|-------|--------|
| Health Indicator Component | 1 | ✅ New |
| Metrics Service Class | 1 | ✅ New |
| Dashboard Query Service | 1 | ✅ New |
| Dashboard DTOs | 6 | ✅ New |
| Monitoring Tests | 10 | ✅ Complete |
| Test Code Lines | 700+ | ✅ |
| Configuration Updates | 1 file | ✅ |

---

## Status: ✅ Phase 2 Week 4 COMPLETE

**Readiness for Phase 2 Week 5**: ✅ **READY**
- Complete health monitoring infrastructure
- Real-time metrics collection and export
- Production dashboards ready
- Kubernetes integration complete
- SLA monitoring enabled
- Alerting rules prepared

**Next**: Proceed to Phase 2 Week 5 for load testing and production hardening.

---

**Document Info**
- **Created**: Phase 2 Week 4 Completion
- **Version**: 1.0
- **Status**: ✅ COMPLETE
- **Components**: 3 monitoring services + 10 tests
- **Endpoints**: 6 (health, metrics, prometheus, dashboards)
- **Ready for**: Phase 2 Week 5 Load Testing
