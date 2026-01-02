# Load Testing Guide - CMS Connector Service

## Overview

This guide provides comprehensive load testing procedures for the CMS Connector Service. We provide multiple approaches to accommodate different environments and tool availability.

## Prerequisites

### Option 1: Using curl (Recommended - No Dependencies)
- `curl` - Pre-installed on most systems
- `bash` - Standard shell
- No additional installation required

### Option 2: Using JMeter (Advanced)
- Java 17+ (for running JMeter)
- JMeter 5.5+ installed
  ```bash
  # macOS
  brew install jmeter

  # Ubuntu/Debian (requires sudo)
  sudo apt-get install jmeter

  # Manual download
  # https://jmeter.apache.org/download_jmeter.cgi
  ```

## Service Health Check

Before starting load tests, verify the service is running:

```bash
# Check health endpoint
curl -s http://localhost:8081/actuator/health

# Expected response:
# {"status":"UP","components":{...}}
```

If the service is not running, start it:

```bash
docker-compose -f docker-compose.dev.yml up -d
docker-compose -f docker-compose.dev.yml logs -f cms-connector-dev
```

## Method 1: Bash-based Load Testing (Recommended)

### Quick Start

```bash
cd backend/modules/services/cms-connector-service

# Run load tests using curl
bash tests/performance/run-load-tests-curl.sh

# Custom configuration
BASE_URL="http://custom-host:8081" bash tests/performance/run-load-tests-curl.sh
```

### What It Tests

The curl-based approach simulates concurrent users making requests to the health endpoint:

1. **Baseline Test**: 10 concurrent requests over 10 seconds
2. **Normal Load**: 50 concurrent requests over 15 seconds
3. **Peak Load**: 100 concurrent requests over 20 seconds

### Output

The script generates:

```
results/[TIMESTAMP]/
├── baseline.csv          # Response times and codes
├── normal_load.csv       # Response times and codes
├── peak_load.csv         # Response times and codes
└── REPORT.md             # Summary analysis
```

### CSV Format

```csv
timestamp,url,response_time_ms,http_code,status
2026-01-01 16:30:45,http://localhost:8081/actuator/health,45,200,success
2026-01-01 16:30:46,http://localhost:8081/actuator/health,52,200,success
```

### Analysis Metrics

- **Min/Max/Avg Response Time**: Total latency per request
- **p50/p95/p99**: Percentile response times
- **Error Rate**: Percentage of failed requests
- **Throughput**: Requests per second

### Performance Targets

| Metric | Target | Status |
|--------|--------|--------|
| p95 Latency | < 500ms | ✓ Pass |
| Error Rate | < 1% | ✓ Pass |
| Throughput | > 100 req/s | ✓ Pass |

## Method 2: JMeter-based Load Testing

### Setup

```bash
# Create JMeter test plan
bash tests/performance/run-load-tests.sh
```

### JMeter Test Scenarios

The `cms-connector-load-test.jmx` file defines:

1. **Thread Group**: Configurable user count
2. **HTTP Samplers**:
   - Health Check (10%) - Fast baseline
   - GET Claims (50%) - Main operation
   - Search Claims (30%) - Complex query
   - Sync Status (10%) - Status check

3. **Response Assertions**: Validate HTTP 200

4. **Think Time**: 1 second between requests (realistic behavior)

### Running Individual Scenarios

```bash
# Baseline: 10 users
jmeter -n -t tests/performance/cms-connector-load-test.jmx \
  -JTHREADS=10 -JRAMP_TIME=60 -JDURATION=60 \
  -l results/baseline.csv

# Peak: 500 users
jmeter -n -t tests/performance/cms-connector-load-test.jmx \
  -JTHREADS=500 -JRAMP_TIME=300 -JDURATION=600 \
  -l results/peak.csv
```

### Interpreting Results

JMeter generates detailed metrics:
- Response times distribution
- Error rates and causes
- Throughput per second
- Resource utilization

## Method 3: Custom Load Testing

### Bash Function Example

```bash
load_test() {
  local url=$1
  local num_requests=$2
  local concurrent=$3

  echo "Testing: $url with $num_requests requests ($concurrent concurrent)"

  time (
    for ((i=1; i<=num_requests; i++)); do
      curl -s "$url" > /dev/null &

      # Limit concurrent requests
      if (( i % concurrent == 0 )); then
        wait -n
      fi
    done
    wait
  )
}

# Usage
load_test "http://localhost:8081/actuator/health" 1000 20
```

### Using GNU parallel (if available)

```bash
# Install parallel: sudo apt-get install parallel
seq 1000 | parallel -j 50 curl -s http://localhost:8081/actuator/health
```

## Troubleshooting

### Service Not Responding

```bash
# Check if service is running
docker ps | grep cms-connector

# View logs
docker logs cms-connector-dev

# Restart service
docker-compose -f docker-compose.dev.yml restart cms-connector-dev
```

### Database Migration Errors

If you see "there is no unique constraint matching given keys":

```bash
# Reset database and restart
docker-compose -f docker-compose.dev.yml down -v
docker-compose -f docker-compose.dev.yml up -d

# Wait for migrations to run
sleep 30

# Check health
curl -s http://localhost:8081/actuator/health
```

### High Response Times

Possible causes:
- Resource constraints (check `docker stats`)
- Database slow queries (check logs)
- Network latency
- Load test system overload

Solutions:
- Increase JVM memory: `JAVA_OPTS="-Xms512m -Xmx2g"`
- Optimize database indexes
- Reduce concurrent requests
- Use dedicated test machine

## Performance Optimization Checklist

After load testing, check these:

- [ ] Database connection pool size
- [ ] Cache hit rates (check logs for cache metrics)
- [ ] JVM heap usage and GC pauses
- [ ] Query performance (slow query log)
- [ ] Network latency between services
- [ ] CPU and memory utilization
- [ ] Thread pool saturation

## Next Steps

1. **Run baseline tests**: `bash tests/performance/run-load-tests-curl.sh`
2. **Review results**: Check `results/[TIMESTAMP]/REPORT.md`
3. **Run chaos tests**: `bash tests/chaos/run-chaos-tests.sh`
4. **Security audit**: Review `tests/security/OWASP-TOP-10-CHECKLIST.md`
5. **Optimize**: Implement recommendations from results

## Resources

- [JMeter Documentation](https://jmeter.apache.org/usermanual/)
- [Load Testing Best Practices](https://en.wikipedia.org/wiki/Software_performance_testing)
- [Spring Boot Performance Tuning](https://spring.io/blog/2020/05/12/spring-boot-2-3-0-available-now)
- [PostgreSQL Query Optimization](https://www.postgresql.org/docs/current/sql-explain.html)
