# HDIM Performance Testing with k6

**Status:** ✅ Ready for Execution
**Framework:** k6 (Grafana k6)
**Execution:** Docker-based (no installation required)
**Created:** January 25, 2026

---

## Overview

Comprehensive performance and load testing suite for the HDIM platform using k6. Tests validate performance SLAs, identify bottlenecks, and ensure the system can handle production load.

### Test Coverage

1. **API Performance Tests** - Response time validation
2. **Load Testing** - Concurrent user simulation (100/500/1000 users)
3. **Stress Testing** - System limits and breaking points
4. **Endurance Testing** - Sustained load over time
5. **Spike Testing** - Sudden traffic increases

---

## Quick Start

### Prerequisites

```bash
# Docker must be installed and running
docker --version

# Application must be running
curl http://localhost:18080/actuator/health
```

### Running Tests

```bash
# Run single performance test
docker run --rm -i --network=host grafana/k6 run - < tests/performance/api-gateway-performance.js

# Run load test (100 concurrent users)
docker run --rm -i --network=host grafana/k6 run - < tests/performance/load-test-normal.js

# Run stress test (1000+ users)
docker run --rm -i --network=host grafana/k6 run - < tests/performance/load-test-stress.js

# Run with HTML report
docker run --rm -i --network=host -v $(pwd)/reports:/reports grafana/k6 run --out json=/reports/results.json - < tests/performance/api-gateway-performance.js
```

### Using Helper Script

```bash
# Run all performance tests
./tests/performance/run-tests.sh

# Run specific test category
./tests/performance/run-tests.sh api
./tests/performance/run-tests.sh load
./tests/performance/run-tests.sh stress
```

---

## Test Files

| File | Purpose | Duration | Users |
|------|---------|----------|-------|
| `api-gateway-performance.js` | API response time validation | 2 min | 10 |
| `patient-service-performance.js` | Patient API performance | 2 min | 10 |
| `care-gap-performance.js` | Care gap evaluation performance | 3 min | 20 |
| `real-time-metrics-performance.js` | Monitoring dashboard performance | 2 min | 10 |
| `load-test-normal.js` | Normal load (100 users) | 5 min | 100 |
| `load-test-peak.js` | Peak load (500 users) | 10 min | 500 |
| `load-test-stress.js` | Stress test (1000+ users) | 15 min | 1000+ |
| `endurance-test.js` | Sustained load (30 min) | 30 min | 200 |
| `spike-test.js` | Sudden traffic spikes | 5 min | 10-500 |

---

## Performance SLAs

### API Response Times

| Endpoint | P95 Target | P99 Target |
|----------|-----------|------------|
| GET /patients/{id} | < 200ms | < 500ms |
| GET /care-gaps | < 300ms | < 800ms |
| POST /quality-measures/evaluate | < 2000ms | < 5000ms |
| GET /metrics/real-time | < 500ms | < 1000ms |
| Gateway routing | < 50ms | < 100ms |

### Throughput Targets

| Operation | Target RPS |
|-----------|-----------|
| Patient lookups | > 1000 |
| Care gap queries | > 500 |
| Metric queries | > 200 |
| Quality evaluations | > 50 |

### Resource Utilization

| Resource | Normal Load | Peak Load |
|----------|------------|-----------|
| CPU Usage | < 60% | < 85% |
| Memory Usage | < 70% | < 90% |
| Database Connections | < 80% pool | < 95% pool |
| Redis Memory | < 80% | < 90% |

---

## Test Scenarios

### Scenario 1: API Gateway Performance

**Goal:** Validate gateway routing performance
**Users:** 10 concurrent
**Duration:** 2 minutes
**Endpoints:** Health, patient search, care gaps

**Success Criteria:**
- P95 response time < 100ms
- 0% error rate
- Gateway CPU < 40%

### Scenario 2: Patient Service Performance

**Goal:** Validate patient API performance
**Users:** 10 concurrent
**Duration:** 2 minutes
**Operations:** Get patient, search patients, update patient

**Success Criteria:**
- P95 response time < 200ms
- Throughput > 500 RPS
- 0% error rate

### Scenario 3: Care Gap Performance

**Goal:** Validate care gap evaluation performance
**Users:** 20 concurrent
**Duration:** 3 minutes
**Operations:** List care gaps, evaluate measure, close gap

**Success Criteria:**
- P95 response time < 300ms
- Evaluation time < 2000ms
- 0% error rate

### Scenario 4: Real-Time Metrics Performance

**Goal:** Validate monitoring dashboard performance
**Users:** 10 concurrent
**Duration:** 2 minutes
**Operations:** Fetch metrics, query Prometheus, render dashboard

**Success Criteria:**
- P95 response time < 500ms
- Dashboard load < 1000ms
- 0% error rate

### Scenario 5: Normal Load Test

**Goal:** Validate system under normal production load
**Users:** 100 concurrent
**Duration:** 5 minutes
**Mix:** 60% reads, 30% writes, 10% complex operations

**Success Criteria:**
- P95 response time < 500ms
- Error rate < 0.1%
- CPU usage < 60%
- Memory usage < 70%

### Scenario 6: Peak Load Test

**Goal:** Validate system under peak production load
**Users:** 500 concurrent
**Duration:** 10 minutes
**Mix:** 70% reads, 20% writes, 10% complex operations

**Success Criteria:**
- P95 response time < 1000ms
- Error rate < 1%
- CPU usage < 85%
- Memory usage < 90%
- No service crashes

### Scenario 7: Stress Test

**Goal:** Find system breaking point
**Users:** Ramp from 100 to 1500
**Duration:** 15 minutes
**Mix:** Realistic production mix

**Success Criteria:**
- Identify max concurrent users
- Identify bottleneck resources
- Graceful degradation (no crashes)
- Error rate < 5% until breaking point

### Scenario 8: Endurance Test

**Goal:** Validate system stability over time
**Users:** 200 concurrent
**Duration:** 30 minutes
**Mix:** Realistic production mix

**Success Criteria:**
- No memory leaks
- No performance degradation
- Stable response times
- No connection pool exhaustion

### Scenario 9: Spike Test

**Goal:** Validate system recovery from traffic spikes
**Users:** 10 → 500 → 10 (instant)
**Duration:** 5 minutes
**Pattern:** Normal → Spike → Normal

**Success Criteria:**
- System handles spike gracefully
- Recovery time < 30 seconds
- No crashes or data loss

---

## Metrics Collected

### Response Time Metrics

- **http_req_duration** - Total request duration
- **http_req_waiting** - Time waiting for response
- **http_req_connecting** - Time establishing connection
- **http_req_tls_handshaking** - TLS handshake time
- **http_req_sending** - Time sending request
- **http_req_receiving** - Time receiving response

### Throughput Metrics

- **http_reqs** - Total HTTP requests
- **http_req_rate** - Requests per second
- **data_sent** - Data sent (bytes)
- **data_received** - Data received (bytes)

### Error Metrics

- **http_req_failed** - Failed requests
- **errors** - Error count by type
- **checks** - Validation check pass/fail

### Custom Metrics

- **patient_search_duration** - Patient search time
- **care_gap_evaluation_duration** - Care gap evaluation time
- **quality_measure_execution_duration** - Quality measure execution time
- **database_query_duration** - Database query time
- **cache_hit_rate** - Redis cache hit rate

---

## Thresholds & Alerts

### Critical Thresholds (Fail Test)

```javascript
export const options = {
  thresholds: {
    // Response time
    'http_req_duration': ['p(95)<500', 'p(99)<1000'],

    // Error rate
    'http_req_failed': ['rate<0.01'], // < 1% errors

    // Throughput
    'http_reqs': ['rate>100'], // > 100 RPS

    // Checks
    'checks': ['rate>0.95'], // > 95% checks pass
  }
};
```

### Warning Thresholds (Report Only)

- P95 response time > 300ms
- Error rate > 0.1%
- CPU usage > 70%
- Memory usage > 80%
- Database connections > 70% pool

---

## CI/CD Integration

### GitHub Actions Workflow

```yaml
name: Performance Tests

on:
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM
  workflow_dispatch:      # Manual trigger

jobs:
  performance-tests:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v3

      - name: Start HDIM services
        run: docker compose up -d

      - name: Wait for services
        run: ./scripts/wait-for-services.sh

      - name: Run performance tests
        run: ./tests/performance/run-tests.sh

      - name: Upload results
        uses: actions/upload-artifact@v3
        with:
          name: performance-results
          path: tests/performance/reports/

      - name: Comment on PR
        if: github.event_name == 'pull_request'
        run: ./scripts/comment-performance-results.sh
```

---

## Grafana Integration

### Dashboard Setup

1. **Install Grafana k6 Plugin**
```bash
docker compose exec grafana grafana-cli plugins install grafana-k6-app
```

2. **Configure InfluxDB Data Source**
```bash
# k6 can output to InfluxDB for visualization
docker run --rm -i --network=host \
  grafana/k6 run --out influxdb=http://localhost:8086/k6 \
  - < tests/performance/api-gateway-performance.js
```

3. **Import k6 Dashboard**
- Import dashboard ID: 2587
- Configure data source: InfluxDB

### Real-Time Monitoring

```bash
# Run test with live dashboard
k6 run --out influxdb=http://localhost:8086/k6 \
  tests/performance/load-test-normal.js
```

---

## Best Practices

### 1. Test Data Preparation

```javascript
// Use setup() to prepare test data
export function setup() {
  const authToken = authenticateUser();
  const testPatients = createTestPatients(100);
  return { authToken, testPatients };
}
```

### 2. Realistic User Behavior

```javascript
// Simulate think time
export default function(data) {
  http.get(`/patients/${randomPatient()}`);
  sleep(randomBetween(1, 3)); // User think time
}
```

### 3. Graceful Ramp-Up

```javascript
export const options = {
  stages: [
    { duration: '2m', target: 100 },  // Ramp up
    { duration: '5m', target: 100 },  // Stay at 100
    { duration: '2m', target: 0 },    // Ramp down
  ]
};
```

### 4. Custom Metrics

```javascript
import { Trend } from 'k6/metrics';
const patientSearchDuration = new Trend('patient_search_duration');

export default function() {
  const start = Date.now();
  const res = http.get('/patients/search?name=John');
  patientSearchDuration.add(Date.now() - start);
}
```

### 5. Checks vs Thresholds

```javascript
// Use checks for validation
check(res, {
  'status is 200': (r) => r.status === 200,
  'response time < 200ms': (r) => r.timings.duration < 200,
});

// Use thresholds for pass/fail
export const options = {
  thresholds: {
    'http_req_duration': ['p(95)<500'],
  }
};
```

---

## Troubleshooting

### Issue 1: High Error Rate

**Symptom:** Error rate > 1%
**Possible Causes:**
- Database connection pool exhausted
- Services not scaled properly
- Network timeouts

**Solution:**
```bash
# Check service logs
docker compose logs patient-service

# Check database connections
docker exec hdim-postgres psql -c "SELECT count(*) FROM pg_stat_activity;"

# Scale services
docker compose up -d --scale patient-service=3
```

### Issue 2: Slow Response Times

**Symptom:** P95 > SLA target
**Possible Causes:**
- Database query optimization needed
- Cache misses
- Resource contention

**Solution:**
```bash
# Check slow queries
docker exec hdim-postgres psql -c "SELECT * FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 10;"

# Check cache hit rate
docker exec hdim-redis redis-cli INFO stats | grep hit_rate

# Check resource usage
docker stats
```

### Issue 3: Memory Leaks

**Symptom:** Memory usage increases over time
**Possible Causes:**
- Connection leaks
- Cache not evicting
- Object retention

**Solution:**
```bash
# Monitor heap usage
docker exec patient-service jcmd 1 GC.heap_info

# Force GC
docker exec patient-service jcmd 1 GC.run

# Check for leaks
docker exec patient-service jcmd 1 GC.class_histogram
```

---

## Reporting

### HTML Report Generation

```bash
# Run test with JSON output
docker run --rm -i --network=host \
  -v $(pwd)/reports:/reports \
  grafana/k6 run --out json=/reports/results.json \
  - < tests/performance/api-gateway-performance.js

# Convert to HTML (requires k6-reporter)
npm install -g k6-html-reporter
k6-html-reporter reports/results.json
```

### CSV Export

```bash
# Export metrics to CSV
docker run --rm -i --network=host \
  -v $(pwd)/reports:/reports \
  grafana/k6 run --out csv=/reports/results.csv \
  - < tests/performance/api-gateway-performance.js
```

### Summary Report

```javascript
export function handleSummary(data) {
  return {
    'summary.json': JSON.stringify(data),
    'summary.html': htmlReport(data),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}
```

---

## Performance Baseline (To Be Established)

| Metric | Baseline | Current | Status |
|--------|----------|---------|--------|
| API Gateway P95 | TBD | - | ⏳ Pending |
| Patient Service P95 | TBD | - | ⏳ Pending |
| Care Gap Service P95 | TBD | - | ⏳ Pending |
| Max Concurrent Users | TBD | - | ⏳ Pending |
| Max RPS | TBD | - | ⏳ Pending |
| CPU at 100 users | TBD | - | ⏳ Pending |
| Memory at 100 users | TBD | - | ⏳ Pending |

**Baseline will be established on first run**

---

## Next Steps

1. ✅ **Install k6** - Docker-based execution
2. ⏳ **Run baseline tests** - Establish performance metrics
3. ⏳ **Document baselines** - Record in this README
4. ⏳ **Set up CI/CD** - Automated daily runs
5. ⏳ **Create Grafana dashboards** - Real-time visualization
6. ⏳ **Performance tuning** - Optimize based on results

---

**Last Updated:** January 25, 2026
**Maintainer:** HDIM Development Team
**Version:** 1.0.0
