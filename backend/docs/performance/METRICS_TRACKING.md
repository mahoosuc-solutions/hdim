# Performance Metrics Tracking Template

**Track optimization progress and validate improvements**

---

## Baseline Metrics (Pre-Optimization)

Record these metrics before implementing optimizations.

### Database Performance

| Metric | Value | Date Measured | Tool |
|--------|-------|---------------|------|
| Average query time | _____ms | __________ | `pg_stat_statements` |
| P95 query time | _____ms | __________ | `pg_stat_statements` |
| Slow queries (>100ms) | _____ | __________ | `pg_stat_statements` |
| Cache hit ratio | _____% | __________ | `pg_stat_database` |
| Active connections | _____ | __________ | `pg_stat_activity` |
| Connection pool usage | _____% | __________ | Hikari metrics |

```sql
-- Run these queries to get baseline

-- Average query time
SELECT mean_exec_time FROM pg_stat_statements
WHERE query LIKE '%agent_configurations%'
ORDER BY mean_exec_time DESC LIMIT 1;

-- Cache hit ratio
SELECT
  sum(blks_hit)*100 / (sum(blks_hit) + sum(blks_read)) as cache_hit_ratio
FROM pg_stat_database
WHERE datname = 'healthdata_db';

-- Active connections
SELECT count(*) FROM pg_stat_activity WHERE state = 'active';
```

---

### API Response Times

| Endpoint | P50 | P95 | P99 | Requests/sec | Date |
|----------|-----|-----|-----|--------------|------|
| GET /agents | ___ms | ___ms | ___ms | ___ | _____ |
| GET /agents/{id} | ___ms | ___ms | ___ms | ___ | _____ |
| POST /agents | ___ms | ___ms | ___ms | ___ | _____ |
| PUT /agents/{id} | ___ms | ___ms | ___ms | ___ | _____ |
| GET /agents/{id}/versions | ___ms | ___ms | ___ms | ___ | _____ |
| POST /agents/{id}/test/start | ___ms | ___ms | ___ms | ___ | _____ |
| GET /templates | ___ms | ___ms | ___ms | ___ | _____ |
| POST /{agentType}/execute | ___ms | ___ms | ___ms | ___ | _____ |

```bash
# Use k6 to gather baseline metrics
k6 run --out json=baseline-metrics.json load-test.js

# Or use Apache Bench
ab -n 1000 -c 10 http://localhost:8096/api/v1/agent-builder/agents
```

---

### Redis Performance

| Metric | Value | Date Measured |
|--------|-------|---------------|
| Cache hit rate | _____% | __________ |
| Average GET latency | _____ms | __________ |
| Memory usage | _____MB | __________ |
| Evicted keys | _____ | __________ |
| Connected clients | _____ | __________ |

```bash
# Redis metrics
redis-cli INFO stats | grep hit_rate
redis-cli INFO memory | grep used_memory_human
redis-cli INFO stats | grep evicted_keys
redis-cli CLIENT LIST | wc -l
```

---

### JVM & Application Metrics

| Metric | Value | Date Measured |
|--------|-------|---------------|
| Heap memory used | _____MB | __________ |
| GC pause time (avg) | _____ms | __________ |
| Thread count | _____ | __________ |
| Request throughput | _____req/s | __________ |
| Error rate | _____% | __________ |

```bash
# Get metrics from actuator
curl http://localhost:8096/actuator/metrics/jvm.memory.used | jq
curl http://localhost:8096/actuator/metrics/jvm.gc.pause | jq
curl http://localhost:8096/actuator/metrics/http.server.requests | jq
```

---

## Post-Optimization Metrics

### Week 1: Critical Optimizations

**Implemented:**
- [ ] GIN indexes for JSONB columns (1.1)
- [ ] Fixed N+1 query patterns (1.2)
- [ ] Redis caching layer (1.3)
- [ ] Optimized snapshot serialization (1.4)

**Results:**

| Metric | Baseline | After Optimization | Improvement | Target Met? |
|--------|----------|-------------------|-------------|-------------|
| List agents P95 | ___ms | ___ms | ___% | ☐ Yes ☐ No |
| Get agent P95 | ___ms | ___ms | ___% | ☐ Yes ☐ No |
| Version history P95 | ___ms | ___ms | ___% | ☐ Yes ☐ No |
| Template lookup P95 | ___ms | ___ms | ___% | ☐ Yes ☐ No |
| DB query avg time | ___ms | ___ms | ___% | ☐ Yes ☐ No |
| Cache hit rate | ___% | ___% | ___% | ☐ Yes ☐ No |

**Issues Encountered:**
-

**Adjustments Made:**
-

---

### Week 2: High Priority Optimizations

**Implemented:**
- [ ] Async processing (2.1)
- [ ] Connection pool tuning (2.2)
- [ ] Cache warming strategy (2.3)
- [ ] Additional database indexes (2.4)

**Results:**

| Metric | After Week 1 | After Week 2 | Improvement | Target Met? |
|--------|--------------|--------------|-------------|-------------|
| Concurrent users supported | ___ | ___ | ___% | ☐ Yes ☐ No |
| Test message P95 | ___ms | ___ms | ___% | ☐ Yes ☐ No |
| Agent execute P95 | ___ms | ___ms | ___% | ☐ Yes ☐ No |
| Connection pool usage | ___% | ___% | ___% | ☐ Yes ☐ No |
| Thread pool queue depth | ___ | ___ | ___% | ☐ Yes ☐ No |

**Issues Encountered:**
-

**Adjustments Made:**
-

---

### Week 3: Medium Priority + Validation

**Implemented:**
- [ ] Feign client optimization (3.1)
- [ ] Pagination improvements (3.2)
- [ ] Response compression (3.3)
- [ ] Redis memory optimization (3.4)
- [ ] Load testing validation

**Load Test Results:**

| Scenario | VUs | Duration | P95 Latency | Error Rate | Throughput | Pass? |
|----------|-----|----------|-------------|------------|------------|-------|
| Normal load | 50 | 5min | ___ms | ___% | ___req/s | ☐ |
| Peak load | 100 | 5min | ___ms | ___% | ___req/s | ☐ |
| Stress test | 200 | 3min | ___ms | ___% | ___req/s | ☐ |
| Spike test | 500 | 1min | ___ms | ___% | ___req/s | ☐ |

**Target: P95 < 3000ms, Error rate < 1%, No connection pool exhaustion**

---

## Continuous Monitoring

### Daily Checks

```bash
# Check slow queries
psql -d healthdata_db -c "
  SELECT query, mean_exec_time, calls
  FROM pg_stat_statements
  WHERE mean_exec_time > 100
  ORDER BY mean_exec_time DESC
  LIMIT 10;
"

# Check cache hit rate
redis-cli INFO stats | grep keyspace_hits

# Check error rate
curl http://localhost:8096/actuator/metrics/http.server.requests | \
  jq '.measurements[] | select(.statistic == "COUNT")'
```

### Weekly Review

- [ ] Review Grafana dashboards
- [ ] Check Prometheus alerts
- [ ] Analyze slow query log
- [ ] Review application logs for errors
- [ ] Check Redis memory usage trend
- [ ] Validate connection pool metrics

### Monthly Deep Dive

- [ ] Full load test suite
- [ ] Query performance regression check
- [ ] Cache efficiency analysis
- [ ] Resource utilization trends
- [ ] Capacity planning review

---

## Success Criteria Checklist

### Performance Targets

- [ ] P95 latency < 3s for all agent operations
- [ ] Support 500+ concurrent users per service
- [ ] Database query time < 100ms for 95% of queries
- [ ] Cache hit ratio > 80% for frequently accessed data

### Operational Metrics

- [ ] No connection pool exhaustion under peak load
- [ ] Zero Redis memory eviction errors
- [ ] < 1% error rate under sustained load
- [ ] Successful rollback test completed

### Documentation

- [ ] All optimizations documented
- [ ] Monitoring dashboards created
- [ ] Runbooks updated
- [ ] Team training completed

---

## Alerting Thresholds

Configure these alerts in Prometheus/Grafana:

| Alert | Threshold | Severity | Action |
|-------|-----------|----------|--------|
| High P95 latency | > 3s for 5min | Warning | Investigate slow queries |
| Critical P95 latency | > 5s for 2min | Critical | Page on-call |
| Connection pool exhausted | Pending > 5 for 2min | Critical | Scale up / investigate |
| High error rate | > 1% for 5min | Warning | Check logs |
| Cache hit rate low | < 70% for 10min | Warning | Review cache config |
| Redis memory high | > 80% for 10min | Warning | Check TTL / eviction |
| Database CPU high | > 80% for 5min | Warning | Check slow queries |

---

## Grafana Dashboard Panels

### Panel 1: API Response Times
```promql
histogram_quantile(0.95,
  rate(http_server_requests_seconds_bucket{
    application="agent-builder-service"
  }[5m])
)
```

### Panel 2: Database Query Performance
```promql
rate(hikari_connections_usage_seconds_sum[5m]) /
rate(hikari_connections_usage_seconds_count[5m])
```

### Panel 3: Cache Hit Rate
```promql
rate(cache_gets_hits_total[5m]) /
rate(cache_gets_total[5m]) * 100
```

### Panel 4: JVM Memory Usage
```promql
jvm_memory_used_bytes{
  application="agent-builder-service",
  area="heap"
}
```

---

## Appendix: Metric Collection Scripts

### Script 1: Collect All Metrics

```bash
#!/bin/bash
# collect-metrics.sh

DATE=$(date +%Y%m%d-%H%M%S)
OUTPUT_DIR="metrics-${DATE}"
mkdir -p $OUTPUT_DIR

# Database metrics
psql -d healthdata_db -c "
  SELECT * FROM pg_stat_statements
  WHERE mean_exec_time > 100
  ORDER BY mean_exec_time DESC;
" > $OUTPUT_DIR/slow-queries.txt

# Redis metrics
redis-cli INFO > $OUTPUT_DIR/redis-info.txt

# Application metrics
curl -s http://localhost:8096/actuator/metrics | jq > $OUTPUT_DIR/app-metrics.json

# Load test
k6 run --out json=$OUTPUT_DIR/k6-results.json load-test.js

echo "Metrics collected in $OUTPUT_DIR"
```

### Script 2: Compare Metrics

```bash
#!/bin/bash
# compare-metrics.sh

BEFORE=$1
AFTER=$2

echo "=== Comparison: $BEFORE vs $AFTER ==="

# Compare slow query counts
echo "Slow queries before: $(wc -l < $BEFORE/slow-queries.txt)"
echo "Slow queries after: $(wc -l < $AFTER/slow-queries.txt)"

# Compare Redis hit rates
echo "Redis hit rate before: $(grep keyspace_hits $BEFORE/redis-info.txt)"
echo "Redis hit rate after: $(grep keyspace_hits $AFTER/redis-info.txt)"
```

---

**Document Owner:** Performance Engineering Team
**Last Updated:** 2025-12-06
**Review Frequency:** Weekly during optimization phase, Monthly thereafter
