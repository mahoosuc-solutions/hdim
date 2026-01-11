# HDIM Load Testing Suite

Comprehensive load testing framework for HDIM authentication and API endpoints using Apache Bench.

## Prerequisites

### Required Tools

```bash
# Ubuntu/Debian
sudo apt-get install apache2-utils jq curl

# macOS
brew install apache2-utils jq curl

# Verify installation
ab -V
jq --version
```

### Running Services

Ensure HDIM services are running:

```bash
# Start all services
cd /path/to/hdim-master
docker compose up -d

# Verify gateway is responding
curl http://localhost:8001/actuator/health
```

## Quick Start

```bash
# Run all tests
./run-load-tests.sh all

# Run specific test
./run-load-tests.sh login
./run-load-tests.sh token-refresh
./run-load-tests.sh rate-limit
./run-load-tests.sh concurrent
./run-load-tests.sh health
```

## Available Tests

### 1. Login Performance Test

Tests authentication endpoint under various load conditions.

**Command**:
```bash
./run-load-tests.sh login
```

**Scenarios**:
- Light load: 100 requests, 10 concurrent
- Medium load: 1,000 requests, 50 concurrent

**Expected Results**:
- RPS: > 100 requests/second
- P95 latency: < 200ms
- Success rate: > 99%

### 2. Token Refresh Performance

Tests JWT refresh token endpoint.

**Command**:
```bash
./run-load-tests.sh token-refresh
```

**Measures**:
- Refresh token generation speed
- Redis cache hit rate
- Token validation latency

### 3. Rate Limiting Behavior

Validates rate limiting enforcement.

**Command**:
```bash
./run-load-tests.sh rate-limit
```

**Validates**:
- Rate limits are enforced
- HTTP 429 responses returned
- Tenant-specific limits applied

### 4. Concurrent User Load

Simulates realistic concurrent user scenarios.

**Command**:
```bash
./run-load-tests.sh concurrent
```

**Scenario**:
- 10,000 requests
- 100 concurrent users
- Sustained load

### 5. System Health Monitoring

Monitors system health under continuous load.

**Command**:
```bash
./run-load-tests.sh health
```

**Monitors**:
- Service health endpoints
- Response times
- Error rates

## Configuration

Override defaults with environment variables:

```bash
# Change target URL
export GATEWAY_URL=http://localhost:8001
./run-load-tests.sh all

# Custom concurrency levels
export CONCURRENCY_HEAVY=200
export REQUESTS_HEAVY=20000
./run-load-tests.sh concurrent
```

## Understanding Results

### Key Metrics

```
Requests per second (RPS):
  - Target: > 100 RPS for login
  - Good: 200-500 RPS
  - Excellent: > 500 RPS

Time per request:
  - Target: < 200ms (mean)
  - Warning: > 500ms
  - Critical: > 1000ms

Failed requests:
  - Target: < 1%
  - Warning: > 5%
  - Critical: > 10%
```

### Example Output

```
Requests per second:    342.51 [#/sec] (mean)
Time per request:       29.196 [ms] (mean)
Time per request:       2.920 [ms] (mean, across all concurrent requests)
Transfer rate:          98.43 [Kbytes/sec] received

Percentage of the requests served within a certain time (ms)
  50%     25
  66%     28
  75%     30
  80%     32
  90%     38
  95%     45
  98%     58
  99%     68
 100%    125 (longest request)
```

### Interpreting P95/P99 Latencies

- **P50 (median)**: Half of requests faster than this
- **P95**: 95% of requests faster than this (key SLA metric)
- **P99**: 99% of requests faster than this (tail latency)
- **P100 (max)**: Slowest request

## Results Directory

Test results are saved to `./results/YYYYMMDD_HHMMSS/`:

```
results/20260110_203000/
├── PERFORMANCE_REPORT.md        # Summary report
├── login_light.txt               # Login light load results
├── login_medium.txt              # Login medium load results
├── login_light.tsv               # Timing data (gnuplot format)
├── refresh_light.txt             # Token refresh results
├── ratelimit.txt                 # Rate limiting results
├── concurrent.txt                # Concurrent load results
└── *.json                        # Test payloads
```

## Troubleshooting

### Connection Refused

```bash
# Check if services are running
docker compose ps

# Check gateway logs
docker compose logs gateway-service

# Verify network connectivity
curl -v http://localhost:8001/actuator/health
```

### High Failure Rate

```bash
# Check service logs for errors
docker compose logs -f

# Verify database connectivity
docker exec hdim-postgres pg_isready

# Check Redis connectivity
docker exec hdim-redis redis-cli ping
```

### Rate Limiting Not Working

```bash
# Verify rate limiting configuration
curl http://localhost:8001/actuator/configprops | jq '.["rate-limiting"]'

# Check Redis rate limit keys
docker exec hdim-redis redis-cli KEYS "rate-limit:*"
```

## Performance Baselines

### Development Environment

- **Hardware**: Local Docker on developer laptop
- **Expected RPS**: 100-200 RPS
- **Expected P95**: < 300ms

### Staging Environment

- **Hardware**: AWS t3.medium (2 vCPU, 4GB RAM)
- **Expected RPS**: 300-500 RPS
- **Expected P95**: < 200ms

### Production Environment

- **Hardware**: AWS c5.xlarge (4 vCPU, 8GB RAM)
- **Target RPS**: > 1000 RPS
- **Target P95**: < 100ms

## Advanced Usage

### Custom Test Scenarios

Create custom test scripts by modifying `run-load-tests.sh`:

```bash
# Example: Test specific endpoint
test_custom_endpoint() {
    ab -n 1000 -c 50 \
        -H "Authorization: Bearer $TOKEN" \
        "$GATEWAY_URL/api/v1/patients" \
        > "$RESULTS_DIR/custom_test.txt"
}
```

### Continuous Load Testing

Run tests in CI/CD pipeline:

```bash
# GitHub Actions example
- name: Run load tests
  run: |
    docker compose up -d
    sleep 10  # Wait for services
    ./testing/load-testing/run-load-tests.sh all

    # Fail if RPS < threshold
    RPS=$(grep "Requests per second" results/*/login_light.txt | awk '{print $4}')
    if (( $(echo "$RPS < 100" | bc -l) )); then
      echo "Performance degradation: RPS=$RPS"
      exit 1
    fi
```

### Integration with Monitoring

Export metrics to Prometheus/Grafana:

```bash
# Parse results and expose as metrics
# See: https://prometheus.io/docs/instrumenting/pushing/
```

## Best Practices

1. **Baseline First**: Run tests on known-good code to establish baseline
2. **Isolate Variables**: Test one change at a time
3. **Multiple Runs**: Run each test 3-5 times, use median
4. **Resource Monitoring**: Monitor CPU/memory during tests
5. **Realistic Scenarios**: Use production-like data and patterns

## Security Notes

- Tests use test credentials only
- Do NOT run against production without approval
- Rate limiting may block your IP during aggressive testing
- Use dedicated test environment for heavy load testing

## References

- [Apache Bench Documentation](https://httpd.apache.org/docs/2.4/programs/ab.html)
- [HDIM Architecture](../../docs/architecture/SYSTEM_ARCHITECTURE.md)
- [Performance Tuning Guide](../../docs/PERFORMANCE_TUNING.md)

---

**Last Updated**: January 2026
**Maintained By**: Platform Engineering Team
