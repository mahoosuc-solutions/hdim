# PERFORMANCE TESTING GUIDE

Load testing, benchmarking, and performance evaluation standards for HDIM microservices.

**Last Updated**: January 19, 2026
**Status**: Phase 2, P1 Critical Guide
**Focus**: Artillery load testing, benchmark comparisons, performance regression detection

---

## Overview

Performance testing validates that HDIM services meet throughput, latency, and resource consumption targets under realistic load. This guide standardizes performance evaluation across all 50+ microservices.

### Performance Testing Pyramid

```
Unit Performance Tests (30%) → Algorithm efficiency
Load Tests (50%) → Real-world throughput/latency
Spike/Stress Tests (20%) → Breaking point discovery
```

### Current State

- ✅ 50+ microservices deployed
- ⚠️ No unified performance testing strategy
- ⚠️ Baseline metrics not established
- 📊 Need performance regression detection in CI/CD

---

## Performance Testing Scenarios

### Scenario 1: Normal Load (Baseline)

```yaml
# Load that represents typical production usage
Request Rate: 100 requests/second per service
Duration: 5 minutes
Expected Response Time: <100ms (p95)
Expected Throughput: 100+ req/sec
Expected Error Rate: <1%
```

### Scenario 2: Peak Load (Black Friday/Quarterly)

```yaml
# Load during peak usage periods
Request Rate: 500 requests/second per service
Duration: 15 minutes
Expected Response Time: <500ms (p95)
Expected Throughput: 500+ req/sec
Expected Error Rate: <2%
```

### Scenario 3: Spike Test (Flash Event)

```yaml
# Sudden traffic spike (e.g., marketing campaign launch)
Request Rate: 1000 requests/second for 2 minutes
Then: Drop to 100 req/sec
Duration: 10 minutes total
Expected Recovery Time: <2 minutes
```

### Scenario 4: Sustained Load (Stress)

```yaml
# Long-running test to find memory leaks
Request Rate: 200 requests/second
Duration: 60 minutes
Expected Memory: Stable (no growth > 10% per hour)
Expected Response Time: Consistent (no degradation)
```

---

## Arsenal: Load Testing Tools

### Tool Comparison

| Tool         | Language  | Use Case                | Ease of Use | Scalability |
| ------------ | --------- | ----------------------- | ----------- | ----------- |
| **Artillery**| JavaScript| API load testing        | Easy        | Good        |
| **JMeter**   | Java      | Complex test scenarios  | Medium      | Excellent   |
| **Locust**   | Python    | Distributed load gen    | Medium      | Excellent   |
| **K6**       | Go        | Developer-friendly      | Easy        | Good        |

**HDIM Recommendation**: **Artillery** (JavaScript/Node.js)

**Reasons:**
- Easy YAML/JavaScript syntax
- Built-in metrics (latency, throughput, errors)
- JSON reporting for CI/CD integration
- Docker containerized for consistency

---

## Artillery Setup

### Installation

```bash
# Install Artillery globally
npm install -g artillery

# Verify installation
artillery --version
```

### Basic Load Test Configuration

```yaml
# artillery.yml - Simple baseline test

config:
  target: "http://localhost:8084"  # Patient service
  phases:
    - duration: 60
      arrivalRate: 10
      name: "Baseline load"

scenarios:
  - name: "Get patient by ID"
    flow:
      - get:
          url: "/api/v1/patients/550e8400-e29b-41d4-a716-446655440000"
          headers:
            X-Tenant-ID: "TENANT-001"
```

### Running the Test

```bash
# Run load test
artillery run artillery.yml

# Generate HTML report
artillery run artillery.yml --output results.json
artillery report results.json --output report.html

# View results
open report.html
```

---

## Advanced Load Test Scenarios

### Scenario 1: Patient Service Baseline

```yaml
# Load test for patient-service

config:
  target: "http://localhost:8084"
  phases:
    - duration: 60          # Warm up
      arrivalRate: 5
      name: "Warm-up"
    - duration: 300         # Main test (5 minutes)
      arrivalRate: 100
      name: "Baseline load"
    - duration: 60          # Cool down
      arrivalRate: 5
      name: "Cool-down"

  defaults:
    headers:
      X-Tenant-ID: "TENANT-001"
      Content-Type: "application/json"

  variables:
    patientIds:
      - "550e8400-e29b-41d4-a716-446655440000"
      - "550e8400-e29b-41d4-a716-446655440001"
      - "550e8400-e29b-41d4-a716-446655440002"

scenarios:
  - name: "Patient Service Baseline"
    weight: 100
    flow:
      # 50% Read (GET)
      - get:
          url: "/api/v1/patients/{{ $randomChoiceFromArray(patientIds) }}"
          capture:
            json: "$.id"
            as: "patientId"

      # 30% Search
      - get:
          url: "/api/v1/patients?firstName=John&limit=10"

      # 20% Create
      - post:
          url: "/api/v1/patients"
          json:
            firstName: "Load Test"
            lastName: "Patient"
            dateOfBirth: "1990-01-01"
          capture:
            json: "$.id"
            as: "newPatientId"
```

### Scenario 2: Quality Measure Service with CQL Evaluation

```yaml
# Complex load test with CQL evaluation

config:
  target: "http://localhost:8087"
  phases:
    - duration: 120
      arrivalRate: 20
      name: "Baseline - Measure Evaluation"
    - duration: 300
      arrivalRate: 100
      name: "Peak - Measure Evaluation"

  variables:
    measureIds:
      - "HEDIS-DIABETES-CONTROL"
      - "HEDIS-HYPERTENSION-CONTROL"
      - "HEDIS-MAMMOGRAPHY"
    patientIds:
      - "550e8400-e29b-41d4-a716-446655440000"
      - "550e8400-e29b-41d4-a716-446655440001"

scenarios:
  - name: "CQL Measure Evaluation"
    flow:
      # Evaluate measure for patient
      - post:
          url: "/api/v1/measures/{{ $randomChoiceFromArray(measureIds) }}/evaluate"
          json:
            patientId: "{{ $randomChoiceFromArray(patientIds) }}"
            dateOfService: "2025-01-19"
          expect:
            - statusCode: [200, 201]
            - hasProperty: result

      # Get evaluation result
      - get:
          url: "/api/v1/measures/{{ measureId }}/results"
          capture:
            json: "$.evaluationId"
            as: "evaluationId"
          expect:
            - statusCode: 200

      # Get measure definition
      - get:
          url: "/api/v1/measures/{{ $randomChoiceFromArray(measureIds) }}"
          expect:
            - statusCode: 200
            - hasProperty: cql
```

### Scenario 3: Multi-Service Orchestration

```yaml
# Load test simulating real user workflows across multiple services

config:
  # Run against gateway (entry point)
  target: "http://localhost:8001"
  phases:
    - duration: 300
      arrivalRate: 50
      name: "Complete workflow load test"

  variables:
    patientIds: ["P1", "P2", "P3", "P4", "P5"]
    measures: ["DIABETES", "HYPERTENSION", "MAMMOGRAPHY"]

scenarios:
  - name: "Complete Quality Measure Workflow"
    weight: 100
    flow:
      # 1. Get patient details
      - get:
          url: "/patient/api/v1/patients/{{ $randomChoiceFromArray(patientIds) }}"
          capture:
            json: "$.id"
            as: "currentPatient"

      # 2. Get patient conditions
      - get:
          url: "/fhir/Condition?patient={{ currentPatient }}"
          expect:
            - statusCode: 200

      # 3. Evaluate quality measure
      - post:
          url: "/quality-measure/api/v1/measures/{{ $randomChoiceFromArray(measures) }}/evaluate"
          json:
            patientId: "{{ currentPatient }}"
          capture:
            json: "$.evaluationId"
            as: "evaluationId"

      # 4. Check care gaps
      - get:
          url: "/care-gap/api/v1/gaps?patientId={{ currentPatient }}"

      # 5. Get analytics/reports
      - get:
          url: "/analytics/api/v1/reports/patient/{{ currentPatient }}"
```

---

## Performance Benchmarking

### Establishing Baselines

Run before any performance optimizations to establish baseline metrics:

```bash
# Run baseline test
artillery run baseline-test.yml --output baseline-results.json

# Generate report
artillery report baseline-results.json

# Save baseline metrics
cat > PERFORMANCE_BASELINE.md << EOF
# Performance Baseline - January 19, 2025

## Patient Service (Baseline Load: 100 req/sec)
- p95 latency: 85ms
- p99 latency: 150ms
- Throughput: 99.8 req/sec
- Error rate: 0.2%
- Memory usage: 450MB (stable)

## Quality Measure Service (Baseline Load: 50 req/sec)
- p95 latency: 240ms (CQL evaluation)
- p99 latency: 520ms
- Throughput: 49.9 req/sec
- Error rate: 0.1%
- Memory usage: 650MB (stable)
EOF
```

### Comparing Changes

```bash
# Run after optimization
artillery run baseline-test.yml --output optimized-results.json

# Create comparison report
cat > performance-comparison.json << EOF
{
  "metric": "patient-service-get",
  "baseline": {
    "p95": 85,
    "p99": 150,
    "throughput": 99.8
  },
  "optimized": {
    "p95": 65,
    "p99": 120,
    "throughput": 105.2
  },
  "improvement": {
    "p95": "23.5% faster",
    "p99": "20% faster",
    "throughput": "5.4% higher"
  }
}
EOF
```

---

## CI/CD Integration

### GitHub Actions Workflow for Performance Tests

```yaml
# .github/workflows/performance-tests.yml

name: Performance Tests

on:
  schedule:
    # Run nightly to detect regressions
    - cron: '0 2 * * *'
  workflow_dispatch:  # Manual trigger

jobs:
  performance-tests:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_PASSWORD: password

      redis:
        image: redis:7-alpine

      kafka:
        image: confluentinc/cp-kafka:7.5.0

    steps:
      - uses: actions/checkout@v3

      - name: Build all services
        run: |
          ./gradlew buildAllServices -x test

      - name: Start Docker Compose
        run: docker compose up -d

      - name: Wait for services to be healthy
        run: |
          for i in {1..30}; do
            if curl -f http://localhost:8084/actuator/health && \
               curl -f http://localhost:8087/actuator/health; then
              echo "Services are healthy"
              exit 0
            fi
            echo "Waiting for services... ($i/30)"
            sleep 2
          done
          exit 1

      - name: Install Artillery
        run: npm install -g artillery

      - name: Run Performance Tests
        run: |
          cd backend
          artillery run \
            load-tests/patient-service-baseline.yml \
            --output results-patient.json
          artillery run \
            load-tests/quality-measure-baseline.yml \
            --output results-quality.json

      - name: Generate Performance Reports
        if: always()
        run: |
          artillery report \
            results-patient.json \
            --output report-patient.html
          artillery report \
            results-quality.json \
            --output report-quality.html

      - name: Compare with Baseline
        run: |
          ./backend/scripts/compare-performance-metrics.sh \
            baseline-metrics.json \
            results-patient.json \
            results-quality.json

      - name: Fail if regression detected
        run: |
          if [ $REGRESSION_DETECTED -eq 1 ]; then
            echo "Performance regression detected!"
            exit 1
          fi

      - name: Upload Reports
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: performance-test-reports
          path: |
            report-patient.html
            report-quality.html
            performance-comparison.json
```

---

## JMeter for Complex Scenarios

For more complex load testing scenarios with state management and custom logic:

### JMeter Installation

```bash
# macOS
brew install jmeter

# Ubuntu
sudo apt install jmeter

# Verify
jmeter --version
```

### Sample JMeter Test Plan (XML)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" enabled="true">
      <elementProp name="TestPlan.user_defined_variables"/>
      <stringProp name="TestPlan.name">Patient Service Load Test</stringProp>
    </TestPlan>

    <ThreadGroup guiclass="ThreadGroupGui">
      <stringProp name="ThreadGroup.num_threads">100</stringProp>
      <stringProp name="ThreadGroup.ramp_time">60</stringProp>
      <elementProp name="ThreadGroup.main_controller" name="LoopController"/>
    </ThreadGroup>

    <HTTPSampler guiclass="HttpTestSampleGui">
      <elementProp name="HTTPsampler.Arguments"/>
      <stringProp name="HTTPSampler.domain">localhost</stringProp>
      <intProp name="HTTPSampler.port">8084</intProp>
      <stringProp name="HTTPSampler.path">/api/v1/patients/550e8400-e29b-41d4-a716-446655440000</stringProp>
      <stringProp name="HTTPSampler.method">GET</stringProp>
    </HTTPSampler>

    <!-- Results aggregator and reporter -->
    <ResultCollector guiclass="StatVisualizer">
      <stringProp name="ResultCollector.filename">results.jtl</stringProp>
    </ResultCollector>
  </hashTree>
</jmeterTestPlan>
```

### Running JMeter Tests

```bash
# CLI (non-GUI) mode (recommended for CI/CD)
jmeter -n -t patient-service-load.jmx -l results.jtl -j log.txt

# Generate HTML report
jmeter -g results.jtl -o html-report/
```

---

## Performance Monitoring During Tests

### Metrics to Track

```yaml
# Key metrics for all services
latency:
  p50: "Response time 50th percentile"
  p95: "Response time 95th percentile"  # SLA target
  p99: "Response time 99th percentile"

throughput:
  requests_per_sec: "Sustainable RPS"
  success_rate: "% successful requests"
  error_rate: "% failed requests"

resource_usage:
  cpu: "CPU utilization %"
  memory: "Memory usage MB"
  network: "Network I/O bandwidth"

database:
  query_time: "Database query time"
  connection_pool: "Active connections"
  slow_queries: "Queries > 500ms"

cache:
  hit_rate: "Cache hit %"
  eviction_rate: "Cache evictions/sec"
```

### Monitoring During Artillery Test

```bash
# Terminal 1: Run Artillery
artillery run load-test.yml

# Terminal 2: Monitor service metrics (in another terminal)
watch -n 1 'curl -s http://localhost:8084/actuator/metrics | jq'

# Terminal 3: Monitor Docker resource usage
docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}"
```

### Prometheus Queries for Performance Analysis

```promql
# Average response time
avg(http_server_requests_seconds_sum{service="patient-service"})
/ avg(http_server_requests_seconds_count{service="patient-service"})

# 95th percentile latency
histogram_quantile(0.95, http_server_requests_seconds{service="patient-service"})

# Request rate
rate(http_server_requests_seconds_count{service="patient-service"}[1m])

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[1m])

# Memory usage growth
rate(jvm_memory_used_bytes{service="patient-service"}[5m])
```

---

## Performance Regression Detection

### Automated Threshold Checking

```bash
#!/bin/bash
# scripts/check-performance-thresholds.sh

RESULTS_FILE=$1
TOLERANCE=${2:-0.1}  # 10% tolerance by default

# Extract metrics from Artillery JSON results
P95=$(jq '.aggregate.latency.p95' $RESULTS_FILE)
P99=$(jq '.aggregate.latency.p99' $RESULTS_FILE)
THROUGHPUT=$(jq '.aggregate.rps.mean' $RESULTS_FILE)
ERROR_RATE=$(jq '.aggregate.codes."5xx" / .aggregate.total' $RESULTS_FILE)

# Define thresholds
P95_THRESHOLD=100
P99_THRESHOLD=200
THROUGHPUT_THRESHOLD=100
ERROR_RATE_THRESHOLD=0.01  # 1%

# Check thresholds
FAILED=0

if (( $(echo "$P95 > $P95_THRESHOLD" | bc -l) )); then
  echo "❌ P95 latency FAILED: ${P95}ms (threshold: ${P95_THRESHOLD}ms)"
  FAILED=1
fi

if (( $(echo "$P99 > $P99_THRESHOLD" | bc -l) )); then
  echo "❌ P99 latency FAILED: ${P99}ms (threshold: ${P99_THRESHOLD}ms)"
  FAILED=1
fi

if (( $(echo "$THROUGHPUT < $THROUGHPUT_THRESHOLD" | bc -l) )); then
  echo "❌ Throughput FAILED: ${THROUGHPUT} req/sec (minimum: ${THROUGHPUT_THRESHOLD})"
  FAILED=1
fi

if (( $(echo "$ERROR_RATE > $ERROR_RATE_THRESHOLD" | bc -l) )); then
  echo "❌ Error rate FAILED: ${ERROR_RATE} (maximum: ${ERROR_RATE_THRESHOLD})"
  FAILED=1
fi

if [ $FAILED -eq 0 ]; then
  echo "✅ All performance thresholds met"
  exit 0
else
  echo "❌ Performance regression detected"
  exit 1
fi
```

### Integration into Pipeline

```yaml
# In .github/workflows/performance-tests.yml
- name: Check Performance Thresholds
  run: |
    ./backend/scripts/check-performance-thresholds.sh \
      results-patient.json \
      0.1  # 10% tolerance
```

---

## Performance Testing Best Practices

### ✅ DO

- **Establish baselines** before making optimizations
- **Test realistic scenarios** that match production traffic
- **Run tests consistently** in same environment
- **Test incrementally** (add load gradually)
- **Monitor during tests** for resource bottlenecks
- **Save results** with timestamps for trend analysis
- **Automate regression detection** in CI/CD
- **Test under failure conditions** (network delay, database down)

### ❌ DON'T

- **Run load tests in production** (against real production data)
- **Use production credentials** in test scripts
- **Ignore memory leaks** or gradual degradation
- **Run from single location** (use distributed load if possible)
- **Test with unrealistic data** (use production-like datasets)
- **Skip warmup phase** (JVM/cache needs time to stabilize)
- **Mix test scenarios** (run each scenario separately first)

---

## Performance Testing by Service

### Service-Specific Targets

| Service                      | p95 Latency | Throughput | Notes                    |
| ---------------------------- | ----------- | ---------- | ------------------------ |
| patient-service              | <100ms      | >100 req/s | Read-heavy workload      |
| quality-measure-service      | <500ms      | >50 req/s  | CQL evaluation intensive |
| care-gap-service             | <400ms      | >75 req/s  | Algorithm-heavy          |
| cql-engine-service           | <200ms      | >150 req/s | Evaluation engine        |
| fhir-service                 | <150ms      | >100 req/s | FHIR resource lookups    |
| gateway-\*-service           | <50ms       | >1000 req/s| Message routing only     |
| event-driven services        | <100ms      | >500 req/s | Async processing         |

---

## Reporting Performance Results

### HTML Report Template

```html
<!DOCTYPE html>
<html>
<head>
    <title>Performance Test Report</title>
    <style>
        body { font-family: Arial, sans-serif; }
        .metric { border: 1px solid #ccc; padding: 10px; margin: 5px; }
        .pass { background-color: #d4edda; }
        .fail { background-color: #f8d7da; }
    </style>
</head>
<body>
    <h1>Performance Test Report</h1>
    <p>Date: <span id="date"></span></p>
    <p>Service: <span id="service"></span></p>
    <p>Load: <span id="load"></span></p>

    <h2>Results Summary</h2>
    <div class="metric">
        <strong>P95 Latency:</strong> <span id="p95">--</span> ms
        <span id="p95-status" class="pass">✓</span>
    </div>
    <div class="metric">
        <strong>Throughput:</strong> <span id="throughput">--</span> req/sec
        <span id="throughput-status" class="pass">✓</span>
    </div>
    <div class="metric">
        <strong>Error Rate:</strong> <span id="error-rate">--</span> %
        <span id="error-status" class="pass">✓</span>
    </div>

    <h2>Detailed Metrics</h2>
    <pre id="metrics-json"></pre>
</body>
</html>
```

---

## Related Documentation

- **INTEGRATION_TESTING.md** - Testing with real containers
- **TESTING_STRATEGY.md** - Unit test standards
- **MONITORING_OBSERVABILITY.md** - Production monitoring
- **LOCAL_SETUP.md** - Running services locally
- **CI_CD_GUIDE.md** - Pipeline integration

---

## Next Steps

1. **Establish baselines** for all core services (patient, quality-measure, care-gap)
2. **Create load test scenarios** matching real workflows
3. **Integrate into CI/CD** for nightly regression detection
4. **Set SLA targets** for each service based on business requirements
5. **Create performance tuning guide** based on bottleneck findings

---

_Last Updated: January 19, 2026_
_Version: 1.0_
