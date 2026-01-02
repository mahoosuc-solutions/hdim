# Performance Runbook - CQL Engine Service

**Service**: CQL Engine Service
**Version**: 1.0.0
**Date**: 2025-10-31
**On-Call**: Performance Team

---

## Quick Reference

### Critical Performance Metrics

| Metric | Normal | Warning | Critical | Action |
|--------|--------|---------|----------|--------|
| **P95 Latency** | <300ms | 300-500ms | >500ms | Scale up |
| **Error Rate** | <0.1% | 0.1-1% | >1% | Investigate |
| **CPU Usage** | <70% | 70-85% | >85% | Scale up |
| **Memory Usage** | <75% | 75-90% | >90% | Restart/Scale |
| **Cache Hit Rate** | >80% | 60-80% | <60% | Warm cache |
| **Throughput** | >200/s | 100-200/s | <100/s | Check bottleneck |

### Emergency Contacts

- **On-Call Engineer**: Slack `#performance-oncall`
- **Platform Team**: Slack `#platform-team`
- **PagerDuty**: https://healthdata.pagerduty.com

### Quick Actions

```bash
# Check service health
curl http://localhost:8081/actuator/health | jq '.'

# Check current metrics
curl http://localhost:8081/actuator/metrics | jq '.names'

# View logs
docker-compose logs -f --tail=100 cql-engine-service

# Restart service
docker-compose restart cql-engine-service

# Scale up (Kubernetes)
kubectl scale deployment/cql-engine-service --replicas=5 -n healthdata-prod
```

---

## Table of Contents

1. [Performance Monitoring](#performance-monitoring)
2. [Incident Response](#incident-response)
3. [Common Performance Issues](#common-performance-issues)
4. [Capacity Management](#capacity-management)
5. [Performance Degradation](#performance-degradation)
6. [Emergency Procedures](#emergency-procedures)
7. [Post-Incident Review](#post-incident-review)

---

## Performance Monitoring

### Real-Time Monitoring

#### Grafana Dashboards

**Primary Dashboard**: CQL Engine - Service Overview
- URL: `https://grafana.healthdata.com/d/cql-engine-overview`
- Panels:
  - Request rate (req/s)
  - Latency (P50, P95, P99)
  - Error rate (%)
  - Active instances
  - Cache hit rate

**Resource Dashboard**: CQL Engine - Resources
- URL: `https://grafana.healthdata.com/d/cql-engine-resources`
- Panels:
  - CPU usage per instance
  - Memory usage (heap, non-heap)
  - GC pause time
  - Thread pool utilization
  - Database connections

#### Prometheus Queries

**Check Current Performance**:
```bash
# Request rate (last 5 minutes)
curl -G 'http://prometheus:9090/api/v1/query' \
  --data-urlencode 'query=rate(http_server_requests_seconds_count{job="cql-engine"}[5m])'

# P95 Latency
curl -G 'http://prometheus:9090/api/v1/query' \
  --data-urlencode 'query=histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{job="cql-engine"}[5m]))'

# Error rate
curl -G 'http://prometheus:9090/api/v1/query' \
  --data-urlencode 'query=rate(http_server_requests_seconds_count{job="cql-engine",status=~"5.."}[5m]) / rate(http_server_requests_seconds_count{job="cql-engine"}[5m])'

# Cache hit rate
curl -G 'http://prometheus:9090/api/v1/query' \
  --data-urlencode 'query=rate(cache_gets_total{result="hit"}[5m]) / rate(cache_gets_total[5m])'
```

### Health Checks

#### Application Health

```bash
# Overall health
curl http://localhost:8081/actuator/health | jq '.status'

# Liveness probe
curl http://localhost:8081/actuator/health/liveness

# Readiness probe
curl http://localhost:8081/actuator/health/readiness

# Component health (detailed)
curl http://localhost:8081/actuator/health | jq '.components'
```

Expected response:
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "redis": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}
```

#### Performance Health Checks

```bash
# Check if performance is degraded
# (Script checks multiple metrics and returns overall status)
./scripts/performance-health-check.sh
```

Creates script if not exists:
```bash
#!/bin/bash
# Returns: OK, WARNING, or CRITICAL

LATENCY=$(curl -s http://localhost:8081/actuator/metrics/http.server.requests | jq -r '.measurements[0].value')
ERROR_RATE=$(curl -s http://localhost:8081/actuator/metrics/http.server.requests | jq -r '.measurements | map(select(.statistic == "COUNT" and .value > 0)) | length')
CACHE_HIT=$(curl -s http://localhost:8081/actuator/metrics/cache.gets | jq -r '.measurements[0].value')

if (( $(echo "$LATENCY > 0.5" | bc -l) )); then
  echo "CRITICAL: Latency ${LATENCY}s exceeds 500ms"
  exit 2
elif (( $(echo "$LATENCY > 0.3" | bc -l) )); then
  echo "WARNING: Latency ${LATENCY}s exceeds 300ms"
  exit 1
elif (( $(echo "$CACHE_HIT < 60" | bc -l) )); then
  echo "WARNING: Cache hit rate ${CACHE_HIT}% below 60%"
  exit 1
else
  echo "OK: All performance metrics normal"
  exit 0
fi
```

---

## Incident Response

### Performance Incident Severity Levels

#### P0 - Critical (Immediate Response)
**Definition**: Service unavailable or severely degraded
- **SLO Impact**: Breaching all SLOs
- **User Impact**: Service unusable
- **Examples**:
  - Error rate >5%
  - P95 latency >2 seconds
  - All instances down
  - Complete cache failure

**Response Time**: < 15 minutes
**Escalation**: Immediate PagerDuty alert

#### P1 - High (Urgent Response)
**Definition**: Significant performance degradation
- **SLO Impact**: Breaching multiple SLOs
- **User Impact**: Slow performance affecting users
- **Examples**:
  - Error rate 1-5%
  - P95 latency 500ms-2s
  - >80% resource utilization
  - Cache hit rate <40%

**Response Time**: < 1 hour
**Escalation**: Slack alert to on-call

#### P2 - Medium (Standard Response)
**Definition**: Minor performance issues
- **SLO Impact**: Approaching SLO limits
- **User Impact**: Slightly degraded performance
- **Examples**:
  - Error rate 0.1-1%
  - P95 latency 300-500ms
  - 70-80% resource utilization
  - Cache hit rate 60-80%

**Response Time**: < 4 hours
**Escalation**: Slack notification

### Incident Response Process

#### 1. Acknowledge & Assess (5 minutes)

```bash
# Step 1: Acknowledge incident
# - Update PagerDuty status
# - Post in #incidents channel

# Step 2: Quick assessment
echo "=== Quick Performance Check ==="

# Check service status
echo "Service Status:"
curl -s http://localhost:8081/actuator/health | jq '.status'

# Check current metrics
echo -e "\nKey Metrics:"
curl -s http://localhost:8081/actuator/metrics/http.server.requests \
  | jq -r '.measurements[] | "\(.statistic): \(.value)"'

# Check resource usage
echo -e "\nResource Usage:"
docker stats --no-stream cql-engine-service \
  | awk '{print "CPU:", $3, "Memory:", $7}'

# Check errors in last 5 minutes
echo -e "\nRecent Errors:"
docker-compose logs --tail=50 cql-engine-service | grep -i error | head -10
```

#### 2. Diagnose Root Cause (15 minutes)

**Common Causes Checklist**:

- [ ] **Traffic Spike**: Check request rate in Grafana
  ```bash
  # Check request rate (last hour)
  curl -G 'http://prometheus:9090/api/v1/query' \
    --data-urlencode 'query=rate(http_server_requests_seconds_count[1h])'
  ```

- [ ] **Cache Failure**: Check Redis health
  ```bash
  docker-compose exec redis redis-cli ping
  docker-compose exec redis redis-cli INFO stats | grep hit_rate
  ```

- [ ] **Database Issues**: Check PostgreSQL
  ```bash
  docker-compose exec postgres pg_isready -U healthdata
  docker-compose exec postgres psql -U healthdata -d healthdata_cql \
    -c "SELECT * FROM pg_stat_activity WHERE state = 'active';"
  ```

- [ ] **Memory Leak**: Check heap usage trend
  ```bash
  curl http://localhost:8081/actuator/metrics/jvm.memory.used \
    | jq '.measurements[0].value / 1024 / 1024 | floor'
  ```

- [ ] **Thread Pool Exhaustion**: Check active threads
  ```bash
  curl http://localhost:8081/actuator/metrics/executor.active \
    | jq '.measurements[0].value'
  curl http://localhost:8081/actuator/metrics/executor.pool.size \
    | jq '.measurements[0].value'
  ```

- [ ] **External Service Latency**: Check FHIR service
  ```bash
  curl -w "\nTime: %{time_total}s\n" \
    http://fhir-server:8080/fhir/metadata
  ```

#### 3. Implement Fix (Varies)

**Quick Fixes** (< 5 minutes):

1. **Restart Service** (if memory leak or hung threads):
   ```bash
   docker-compose restart cql-engine-service
   # Wait 60 seconds for warmup
   sleep 60
   ./scripts/health-check.sh
   ```

2. **Scale Up** (if high load):
   ```bash
   # Docker Compose
   docker-compose up -d --scale cql-engine-service=5

   # Kubernetes
   kubectl scale deployment/cql-engine-service --replicas=5 -n healthdata-prod
   ```

3. **Warm Cache** (if cold cache):
   ```bash
   ./scripts/warm-cache.sh
   ```

4. **Increase Resources** (if resource constrained):
   ```bash
   # Edit docker-compose.yml to increase limits
   # Then restart
   docker-compose up -d
   ```

**Deeper Fixes** (Require investigation):

- Database query optimization
- Code changes for inefficient algorithms
- Infrastructure upgrades

#### 4. Verify Resolution (10 minutes)

```bash
# Check all metrics are back to normal
./scripts/performance-health-check.sh

# Monitor for 10 minutes
watch -n 30 './scripts/performance-health-check.sh'

# Check SLO compliance
curl http://prometheus:9090/api/v1/query \
  --data-urlencode 'query=histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))' \
  | jq '.data.result[0].value[1]'

# Verify error rate is back to normal
curl http://prometheus:9090/api/v1/query \
  --data-urlencode 'query=rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m])' \
  | jq '.data.result[0].value[1]'
```

#### 5. Document & Communicate (Ongoing)

- Update incident ticket with timeline
- Post status updates in #incidents every 30 minutes
- When resolved: Post final status and root cause
- Schedule post-incident review within 48 hours

---

## Common Performance Issues

### Issue 1: High Latency (P95 > 500ms)

**Symptoms**:
- Slow API responses
- User complaints
- Grafana shows elevated latency

**Diagnosis**:
```bash
# 1. Check if it's cache-related
curl http://localhost:8081/actuator/metrics/cache.gets \
  | jq '.measurements[] | select(.statistic=="VALUE") | .value'

# 2. Check thread pool
curl http://localhost:8081/actuator/metrics/executor.active

# 3. Check database connections
curl http://localhost:8081/actuator/metrics/hikaricp.connections.active

# 4. Check for slow queries
docker-compose exec postgres psql -U healthdata -d healthdata_cql \
  -c "SELECT query, mean_exec_time FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 5;"
```

**Quick Fixes**:

1. **Warm cache** (if cache hit rate <60%):
   ```bash
   ./scripts/warm-cache.sh
   ```

2. **Increase thread pool** (if maxed out):
   ```bash
   echo "MEASURE_EVALUATION_MAX_POOL_SIZE=100" >> .env
   docker-compose restart cql-engine-service
   ```

3. **Scale horizontally**:
   ```bash
   kubectl scale deployment/cql-engine-service --replicas=5
   ```

**Long-term Fix**:
- Optimize database queries (add indexes)
- Implement query result caching
- Consider read replicas for database

### Issue 2: High Error Rate (>1%)

**Symptoms**:
- 500 errors in logs
- Error rate metric elevated
- Failed requests

**Diagnosis**:
```bash
# Check recent errors
docker-compose logs --tail=100 cql-engine-service | grep -i "error\|exception"

# Check error breakdown
curl http://localhost:8081/actuator/metrics/http.server.requests \
  | jq '.availableTags[] | select(.tag=="status")'

# Check health of dependencies
docker-compose ps
```

**Quick Fixes**:

1. **If database connection errors**:
   ```bash
   docker-compose restart postgres
   sleep 10
   docker-compose restart cql-engine-service
   ```

2. **If Redis connection errors**:
   ```bash
   docker-compose restart redis
   docker-compose restart cql-engine-service
   ```

3. **If OOM errors**:
   ```bash
   # Increase memory limit
   # Edit docker-compose.yml: limits.memory: 4G
   docker-compose up -d
   ```

**Long-term Fix**:
- Implement circuit breakers
- Add retry logic with exponential backoff
- Improve error handling

### Issue 3: Low Throughput (<150 req/s)

**Symptoms**:
- Request queue building up
- Low requests/second metric
- CPU not maxed out

**Diagnosis**:
```bash
# Check thread pool utilization
curl http://localhost:8081/actuator/metrics/executor.active
curl http://localhost:8081/actuator/metrics/executor.pool.size

# Check connection pool
curl http://localhost:8081/actuator/metrics/hikaricp.connections.active
curl http://localhost:8081/actuator/metrics/hikaricp.connections

# Check for blocking operations
docker-compose exec cql-engine-service jstack 1 | grep -A 5 BLOCKED
```

**Quick Fixes**:

1. **Increase thread pool**:
   ```bash
   echo "MEASURE_EVALUATION_CORE_POOL_SIZE=20" >> .env
   echo "MEASURE_EVALUATION_MAX_POOL_SIZE=100" >> .env
   docker-compose restart cql-engine-service
   ```

2. **Increase connection pool**:
   ```bash
   echo "SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=40" >> .env
   docker-compose restart cql-engine-service
   ```

3. **Scale horizontally**:
   ```bash
   docker-compose up -d --scale cql-engine-service=3
   ```

**Long-term Fix**:
- Profile application to find bottlenecks
- Optimize database queries
- Implement async processing where possible

---

## Capacity Management

### Capacity Planning Process

#### 1. Monitor Trends (Weekly)

**Key Metrics to Track**:
- Average request rate (req/s)
- Peak request rate (req/s)
- P95 latency trend
- Resource utilization trend
- User growth rate

**Capacity Report**:
```bash
# Generate weekly capacity report
./scripts/capacity-report.sh > capacity-report-$(date +%Y-%m-%d).txt
```

Sample report:
```
HealthData CQL Engine - Capacity Report
Week of: 2025-10-31

Current Capacity:
  Instances: 3
  Max Throughput: 1,200 req/s
  Current Avg: 650 req/s
  Utilization: 54%

Peak Load (Last Week):
  Date: 2025-10-28 14:00
  Requests/sec: 980 req/s
  Utilization: 82%

Growth Trend:
  7-day avg: 650 req/s
  30-day avg: 580 req/s
  Growth rate: +12% month-over-month

Projection:
  Expected in 30 days: 730 req/s (61% utilization)
  Expected in 90 days: 920 req/s (77% utilization)
  Recommendation: Current capacity sufficient for 90 days
```

#### 2. Determine Capacity Needs

**Formula**:
```
Required Capacity = (Peak Load * 1.5) + Buffer

Where:
  Peak Load = Highest observed req/s in last 30 days
  1.5 = Headroom factor (50% overhead)
  Buffer = 200 req/s (minimum buffer)
```

**Example**:
```
Peak Load = 980 req/s
Required Capacity = (980 * 1.5) + 200 = 1,670 req/s

Current Capacity = 3 instances * 400 req/s = 1,200 req/s

Gap = 1,670 - 1,200 = 470 req/s
Required Additional Instances = ceil(470 / 400) = 2

New Total = 3 + 2 = 5 instances
```

#### 3. Plan Scaling Actions

**Scaling Decision Matrix**:

| Utilization | Action | Timeline |
|-------------|--------|----------|
| <50% | No action | Monitor |
| 50-70% | Plan scale-up | Within 30 days |
| 70-85% | Scale up soon | Within 7 days |
| >85% | Scale up now | Immediate |

#### 4. Execute Scaling

**Horizontal Scaling** (Recommended):
```bash
# Kubernetes (preferred)
kubectl scale deployment/cql-engine-service --replicas=5 -n healthdata-prod

# Docker Compose
docker-compose up -d --scale cql-engine-service=5

# Verify scaling
kubectl get pods -n healthdata-prod | grep cql-engine
```

**Vertical Scaling** (If needed):
```yaml
# Update kubernetes/production/kustomization.yaml
patches:
  - target:
      kind: Deployment
      name: cql-engine-service
    patch: |-
      - op: replace
        path: /spec/template/spec/containers/0/resources/requests/cpu
        value: 2000m
      - op: replace
        path: /spec/template/spec/containers/0/resources/limits/memory
        value: 4Gi

# Apply
kubectl apply -k kubernetes/production/
```

---

## Performance Degradation

### Gradual Degradation

**Symptoms**:
- Slow increase in latency over days/weeks
- Gradual decrease in throughput
- Increasing resource usage

**Common Causes**:
1. **Data growth** - Database tables growing
2. **Memory leak** - Slow memory increase
3. **Cache inefficiency** - TTL too short
4. **Query inefficiency** - Missing indexes

**Investigation**:
```bash
# Check database size
docker-compose exec postgres psql -U healthdata -d healthdata_cql \
  -c "SELECT pg_size_pretty(pg_database_size('healthdata_cql'));"

# Check table sizes
docker-compose exec postgres psql -U healthdata -d healthdata_cql \
  -c "SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size FROM pg_tables ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC LIMIT 10;"

# Check memory trend
curl http://prometheus:9090/api/v1/query_range \
  --data-urlencode 'query=jvm_memory_used_bytes{area="heap"}' \
  --data-urlencode 'start=-7d' \
  --data-urlencode 'end=now()' \
  --data-urlencode 'step=1h'
```

**Remediation**:
1. **Database maintenance**:
   ```bash
   docker-compose exec postgres psql -U healthdata -d healthdata_cql \
     -c "VACUUM ANALYZE;"
   ```

2. **Add missing indexes**:
   ```sql
   CREATE INDEX IF NOT EXISTS idx_cql_evaluation_patient_date
     ON cql_evaluation(patient_id, evaluation_date);
   ```

3. **Optimize cache TTL**:
   ```bash
   echo "SPRING_CACHE_REDIS_TIME_TO_LIVE=86400" >> .env
   docker-compose restart cql-engine-service
   ```

### Sudden Degradation

**Symptoms**:
- Abrupt increase in latency
- Sudden drop in throughput
- Spike in error rate

**Common Causes**:
1. **Traffic spike** - Unexpected load increase
2. **Deployment issue** - Bad code deploy
3. **Infrastructure issue** - Network/disk problems
4. **External service issue** - FHIR server slow

**Investigation**:
```bash
# Check recent deployments
kubectl rollout history deployment/cql-engine-service -n healthdata-prod

# Check for traffic spike
curl http://prometheus:9090/api/v1/query \
  --data-urlencode 'query=rate(http_server_requests_seconds_count[5m])'

# Check external services
curl -w "\nTime: %{time_total}s\n" http://fhir-server:8080/fhir/metadata
```

**Remediation**:
1. **If bad deployment**:
   ```bash
   kubectl rollout undo deployment/cql-engine-service -n healthdata-prod
   ```

2. **If traffic spike**:
   ```bash
   kubectl scale deployment/cql-engine-service --replicas=10 -n healthdata-prod
   ```

3. **If external service issue**:
   - Enable circuit breakers
   - Increase timeouts temporarily
   - Contact external service team

---

## Emergency Procedures

### Complete Service Outage

**Scenario**: All instances down, service unavailable

**Immediate Actions** (< 5 minutes):

1. **Assess Scope**:
   ```bash
   # Check all instances
   kubectl get pods -n healthdata-prod | grep cql-engine

   # Check health
   for pod in $(kubectl get pods -n healthdata-prod -l app=cql-engine-service -o name); do
     kubectl logs -n healthdata-prod $pod --tail=50 | grep -i "error\|fatal"
   done
   ```

2. **Check Dependencies**:
   ```bash
   # PostgreSQL
   kubectl get pods -n healthdata-prod | grep postgres

   # Redis
   kubectl get pods -n healthdata-prod | grep redis

   # Network
   kubectl get svc -n healthdata-prod
   ```

3. **Emergency Restart**:
   ```bash
   # Restart all pods
   kubectl rollout restart deployment/cql-engine-service -n healthdata-prod

   # Watch restart
   kubectl rollout status deployment/cql-engine-service -n healthdata-prod
   ```

4. **If Still Down - Rollback**:
   ```bash
   # Rollback to previous version
   kubectl rollout undo deployment/cql-engine-service -n healthdata-prod
   ```

5. **Communicate**:
   - Post in #incidents: "CQL Engine service is down. Investigating."
   - Update status page
   - Send customer notifications if SLA breached

### Database Performance Crisis

**Scenario**: Database causing severe performance issues

**Immediate Actions**:

1. **Identify Slow Queries**:
   ```bash
   docker-compose exec postgres psql -U healthdata -d healthdata_cql \
     -c "SELECT pid, now() - query_start AS duration, query FROM pg_stat_activity WHERE state = 'active' ORDER BY duration DESC;"
   ```

2. **Kill Long-Running Queries** (if blocking):
   ```bash
   # Get PID of blocking query
   docker-compose exec postgres psql -U healthdata -d healthdata_cql \
     -c "SELECT pg_terminate_backend(PID);"
   ```

3. **Increase Connection Pool** (temporary):
   ```bash
   kubectl set env deployment/cql-engine-service \
     SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=50 \
     -n healthdata-prod
   ```

4. **Enable Connection Pooling** (if not already):
   ```bash
   # Use PgBouncer for connection pooling
   kubectl apply -f kubernetes/postgres/pgbouncer.yaml
   ```

### Cache Complete Failure

**Scenario**: Redis down or corrupted

**Immediate Actions**:

1. **Verify Redis is down**:
   ```bash
   docker-compose exec redis redis-cli ping
   ```

2. **Restart Redis**:
   ```bash
   kubectl rollout restart statefulset/redis -n healthdata-prod
   ```

3. **Increase application timeout** (temporary):
   ```bash
   kubectl set env deployment/cql-engine-service \
     SPRING_REDIS_TIMEOUT=5000 \
     -n healthdata-prod
   ```

4. **Scale up application** (compensate for no cache):
   ```bash
   kubectl scale deployment/cql-engine-service --replicas=10 -n healthdata-prod
   ```

5. **Warm cache** (once Redis is back):
   ```bash
   ./scripts/warm-cache.sh
   ```

---

## Post-Incident Review

### Post-Incident Report Template

```markdown
# Performance Incident Report

**Incident ID**: INC-2025-XXXX
**Date**: 2025-10-31
**Severity**: P1
**Duration**: 45 minutes
**Impact**: Elevated latency affecting 30% of requests

## Timeline

- **14:00 UTC**: Alert triggered (P95 latency > 500ms)
- **14:05 UTC**: On-call engineer acknowledged
- **14:10 UTC**: Root cause identified (database connection pool exhausted)
- **14:15 UTC**: Mitigation applied (increased pool size)
- **14:25 UTC**: Metrics returned to normal
- **14:45 UTC**: Incident closed

## Root Cause

Database connection pool size (20) was insufficient for sustained load of 800 req/s.
At peak, all connections were in use, causing new requests to wait for available connections.

## Resolution

Increased connection pool size from 20 to 40 via environment variable:
```bash
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=40
```

## Impact

- **Requests Affected**: ~12,000 (estimated)
- **Users Affected**: ~500
- **SLO Breach**: P95 latency SLO breached for 25 minutes
- **Error Budget Burned**: 1.7% of monthly budget

## Prevention

1. **Immediate** (Done):
   - Increased connection pool size to 40
   - Added alerting for connection pool utilization >80%

2. **Short-term** (1 week):
   - Implement connection pool autoscaling
   - Add database read replicas
   - Improve connection leak detection

3. **Long-term** (1 month):
   - Implement query result caching
   - Optimize database queries
   - Review capacity planning process

## Lessons Learned

1. **What went well**:
   - Alert triggered quickly
   - Root cause identified within 10 minutes
   - Mitigation was straightforward

2. **What could be improved**:
   - Should have capacity tested before going to production
   - Need better visibility into connection pool metrics
   - Runbook could be more detailed

3. **Action Items**:
   - [ ] Add load testing to CI/CD pipeline
   - [ ] Create Grafana dashboard for connection pools
   - [ ] Update runbook with connection pool troubleshooting
   - [ ] Schedule capacity planning review
```

### Action Items Tracking

After each incident:

1. **Create tickets** for all action items
2. **Assign owners** and due dates
3. **Track in Jira** under "Performance" epic
4. **Review in weekly** performance team meeting
5. **Close when verified** in production

---

## Additional Resources

- [Performance Guide](../PERFORMANCE_GUIDE.md) - Comprehensive performance documentation
- [CQL Engine Service Docs](../CQL_ENGINE_SERVICE_COMPLETE.md) - Service documentation
- [Docker Troubleshooting](../DOCKER_TROUBLESHOOTING.md) - Docker-specific issues
- [Kubernetes README](../../kubernetes/README.md) - K8s deployment guide

---

**Runbook Version**: 1.0.0
**Last Updated**: 2025-10-31
**Next Review**: 2026-01-31
**Owner**: Performance Team
