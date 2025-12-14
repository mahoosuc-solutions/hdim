# HDIM Platform Load Tests

Load testing configuration for the HDIM Healthcare Platform using [Artillery](https://www.artillery.io/).

## Prerequisites

```bash
# Install Artillery globally
npm install -g artillery

# Or run via npx
npx artillery --version
```

## Test Files

| File | Description | Target Services |
|------|-------------|-----------------|
| `fhir-api-load-test.yml` | FHIR API endpoints (Patient, Observation) | fhir-service (8085) |
| `health-score-websocket-load-test.yml` | WebSocket health score updates | quality-measure-service (8087) |
| `population-calculation-load-test.yml` | Quality measure calculations | quality-measure-service (8087) |
| `full-platform-load-test.yml` | All 22 microservices via Gateway | gateway-service (8080) |

## Running Tests

### Quick Smoke Test
```bash
# Test FHIR API with minimal load
artillery quick --count 10 -n 20 http://localhost:8085/Patient
```

### Individual Service Tests
```bash
# FHIR API Load Test
artillery run fhir-api-load-test.yml

# WebSocket Load Test
artillery run health-score-websocket-load-test.yml

# Population Calculation Load Test
artillery run population-calculation-load-test.yml
```

### Full Platform Test
```bash
# Comprehensive test of all services
artillery run full-platform-load-test.yml
```

### With Report Output
```bash
artillery run --output results.json fhir-api-load-test.yml
artillery report results.json
```

## Test Phases

Each test follows this pattern:

| Phase | Duration | Request Rate | Purpose |
|-------|----------|--------------|---------|
| Warm up | 60s | 10-20 req/s | Initialize connections |
| Sustained | 300s | 100 req/s | Normal operation |
| Peak | 120s | 200-250 req/s | Stress test |
| Cool down | 60s | 50 req/s | Graceful wind-down |

## Success Criteria

| Metric | Target |
|--------|--------|
| FHIR API p95 latency | < 100ms |
| WebSocket connections | > 1000 concurrent |
| Population calc throughput | > 1000 patients/min |
| Health score update | < 5 seconds end-to-end |
| Error rate | < 0.1% |

## Environment Variables

Set before running tests:

```bash
export TARGET_HOST=http://localhost  # or staging URL
export TENANT_ID=test-tenant-001
export AUTH_TOKEN=your-jwt-token
```

## Docker Compose Integration

Run load tests in Docker:

```bash
# Start services
docker compose --profile full up -d

# Wait for services to be healthy
make health

# Run load tests
docker run --rm -it --network host \
  -v $(pwd)/load-tests:/tests \
  artilleryio/artillery:latest \
  run /tests/full-platform-load-test.yml
```

## Interpreting Results

Artillery outputs:
- **http.codes.2xx**: Successful requests
- **http.response_time.p95**: 95th percentile latency
- **vusers.completed**: Virtual users that completed scenarios
- **errors.ETIMEDOUT**: Timeout errors (should be 0)

Example healthy output:
```
All VUs finished. Total time: 10 minutes, 30 seconds

Summary:
  Scenarios launched:  50000
  Scenarios completed: 50000
  Requests completed:  150000
  Mean response/sec:   238.1
  Response time (msec):
    min: 2
    max: 892
    median: 15
    p95: 85
    p99: 234
```
