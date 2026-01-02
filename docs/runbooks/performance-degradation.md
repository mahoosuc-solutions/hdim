# Runbook: Performance Degradation

**Severity:** High
**Response Time:** < 30 min
**Alert Names:** `HighLatencyP99`, `SlowDatabaseQueries`, `HighResponseTime`

## Symptoms

- API response times > 2s (normal < 200ms)
- P99 latency elevated
- User complaints about slowness
- Timeout errors in logs

## Impact Assessment

Check which endpoints are slow:
```bash
# Check Prometheus for slow endpoints
# In Grafana, navigate to service dashboard > Latency panel
```

## Diagnosis

### 1. Identify Slow Endpoints
```bash
# Check for slow requests in logs
kubectl logs deployment/<service-name> -n healthdata-prod --since=10m | \
  grep -i "took\|duration\|latency" | sort -t= -k2 -rn | head -20
```

### 2. Check Resource Usage
```bash
# CPU and memory
kubectl top pods -n healthdata-prod -l app=<service-name>

# Compare to limits
kubectl describe pod -n healthdata-prod -l app=<service-name> | grep -A5 "Limits:"
```

### 3. Check Database Performance
```bash
# Slow queries
psql -U hdim -c "SELECT query, calls, mean_time, total_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;"

# Active queries
psql -U hdim -c "SELECT pid, now() - query_start as duration, query
FROM pg_stat_activity
WHERE state = 'active'
ORDER BY duration DESC;"
```

### 4. Check JVM Metrics
```bash
# GC activity
curl -s http://<service>:8080/actuator/metrics/jvm.gc.pause | jq

# Heap usage
curl -s http://<service>:8080/actuator/metrics/jvm.memory.used | jq
```

### 5. Check External Dependencies
```bash
# Response times from FHIR server
curl -w "@curl-format.txt" -o /dev/null -s https://fhir-server/Patient/1

# curl-format.txt content:
#   time_total:  %{time_total}\n
```

### 6. Check Network
```bash
# DNS resolution time
kubectl exec -it deployment/<service-name> -n healthdata-prod -- \
  time nslookup postgres

# Network latency to database
kubectl exec -it deployment/<service-name> -n healthdata-prod -- \
  ping -c 5 postgres
```

## Mitigation Steps

### High CPU Usage

**Step 1: Scale horizontally**
```bash
kubectl scale deployment/<service-name> --replicas=5 -n healthdata-prod
```

**Step 2: Check for CPU-intensive operations**
```bash
# Thread dump
kubectl exec -it deployment/<service-name> -n healthdata-prod -- \
  jstack 1 > thread-dump.txt
```

### High Memory / GC Pressure

**Step 1: Check heap usage**
```bash
curl -s http://<service>:8080/actuator/metrics/jvm.memory.used?tag=area:heap | jq
```

**Step 2: Force GC (temporary relief)**
```bash
kubectl exec -it deployment/<service-name> -n healthdata-prod -- \
  jcmd 1 GC.run
```

**Step 3: Restart if OOM imminent**
```bash
kubectl rollout restart deployment/<service-name> -n healthdata-prod
```

### Slow Database Queries

**Step 1: Identify and kill long queries**
```sql
-- Find queries running > 5 minutes
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE state = 'active'
  AND query_start < now() - interval '5 minutes'
  AND pid <> pg_backend_pid();
```

**Step 2: Add missing indexes (requires DBA)**
```sql
-- Check for sequential scans
SELECT schemaname, relname, seq_scan, idx_scan
FROM pg_stat_user_tables
WHERE seq_scan > idx_scan * 10
ORDER BY seq_scan DESC;
```

**Step 3: Vacuum if needed**
```sql
VACUUM ANALYZE;
```

### Connection Pool Saturation

**Step 1: Check pool metrics**
```bash
curl -s http://<service>:8080/actuator/metrics/hikaricp.connections.active | jq
curl -s http://<service>:8080/actuator/metrics/hikaricp.connections.pending | jq
```

**Step 2: Increase pool size (requires config change)**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # Increase from default 10
```

**Step 3: Restart services to apply**

### External Service Slow

**Step 1: Check FHIR server health**
```bash
curl -s https://fhir-server/metadata | jq .status
```

**Step 2: Enable circuit breaker** (should be automatic)
- Check Resilience4j metrics in Grafana
- Circuit should open if failures > threshold

**Step 3: Consider caching**
- Check if cache hit rate is low
- Review cache TTL settings

## Recovery Verification

1. P99 latency returns to baseline (< 500ms):
```bash
# Query Prometheus
curl -s "http://prometheus:9090/api/v1/query?query=histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))"
```

2. No active slow queries:
```sql
SELECT count(*) FROM pg_stat_activity
WHERE state = 'active' AND query_start < now() - interval '30 seconds';
-- Should be 0 or very low
```

3. Resource usage normal:
```bash
kubectl top pods -n healthdata-prod -l app=<service-name>
# CPU < 80%, Memory < 80%
```

## Escalation

| Condition | Action |
|-----------|--------|
| Database query optimization needed | Escalate to DBA |
| External service consistently slow | Escalate to integration team |
| Need resource limit increase | Escalate to DevOps |
| Root cause unclear | Escalate to service owner |

## Post-Incident

- [ ] Profile slow endpoints
- [ ] Review database query plans
- [ ] Add caching where appropriate
- [ ] Update performance baselines
- [ ] Consider load testing
