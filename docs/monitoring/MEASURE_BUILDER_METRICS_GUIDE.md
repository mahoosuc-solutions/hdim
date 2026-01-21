# Measure Builder - Metrics & Monitoring Guide

**Version:** 1.0
**Last Updated:** January 18, 2026
**Audience:** DevOps, SRE, Monitoring Teams
**Format:** Production Monitoring Guide

---

## Key Performance Indicators (KPIs)

### Response Time Metrics

| Metric | Definition | Target | Warning | Critical |
|--------|-----------|--------|---------|----------|
| **P50 API Response Time** | Median API response time | <100ms | >150ms | >200ms |
| **P95 API Response Time** | 95th percentile response time | <500ms | >750ms | >1000ms |
| **P99 API Response Time** | 99th percentile response time | <1000ms | >1500ms | >2000ms |
| **SVG Render Time** | Time to render blocks <150 | <50ms | >100ms | >150ms |
| **Canvas Render Time** | Time to render blocks 150+ | <200ms | >300ms | >500ms |
| **CQL Generation Time** | Time to generate CQL | <500ms | >750ms | >1000ms |

### Availability & Reliability

| Metric | Definition | Target | Warning | Critical |
|--------|-----------|--------|---------|----------|
| **Uptime** | Service availability | 99.9% | <99.5% | <99% |
| **Error Rate** | Percentage of 5xx errors | <0.1% | >0.5% | >1% |
| **Success Rate** | Percentage of successful requests | >99.9% | <99.5% | <99% |
| **Measure Publish Success** | % of publishes that succeed | 99.9% | <99% | <98% |
| **Validation Success** | % of validations that complete | 99.8% | <99% | <98% |

### Resource Utilization

| Metric | Definition | Target | Warning | Critical |
|--------|-----------|--------|---------|----------|
| **CPU Usage** | Container CPU usage | <60% | >75% | >90% |
| **Memory Usage** | Container memory usage | <70% | >80% | >90% |
| **Disk Space** | Available disk space | >30% | <15% | <5% |
| **Database Connections** | Active DB connections | <50 | >75 | >100 |
| **Connection Pool** | % of pool in use | <60% | >75% | >90% |

### Business Metrics

| Metric | Definition | Target | Trend |
|--------|-----------|--------|-------|
| **Measures Created/Day** | New measures created | >10 | Should increase |
| **Measures Published/Day** | Published measures | >5 | Should increase |
| **Evaluations Run/Day** | Total evaluations | >100 | Should increase |
| **Active Users/Day** | Users accessing system | >50 | Should increase |
| **Avg Measure Complexity** | Average complexity score | 5-7 | Monitor trend |

---

## Prometheus Queries

### Response Time Queries

```promql
# Average response time (last 5 minutes)
avg(http_request_duration_seconds{job="measure-builder"})

# P95 response time
histogram_quantile(0.95, http_request_duration_seconds{job="measure-builder"})

# P99 response time
histogram_quantile(0.99, http_request_duration_seconds{job="measure-builder"})

# Specific endpoint performance
histogram_quantile(0.95, http_request_duration_seconds{endpoint="/api/v1/measures"})
```

### Error Rate Queries

```promql
# Overall error rate (5xx)
rate(http_requests_total{status=~"5.."}[5m])

# Error rate by endpoint
rate(http_requests_total{endpoint="/api/v1/measures",status=~"5.."}[5m])

# 4xx (client error) rate
rate(http_requests_total{status=~"4.."}[5m])

# Total request rate
rate(http_requests_total[5m])
```

### Resource Utilization Queries

```promql
# CPU usage
rate(container_cpu_usage_seconds_total{name="measure-builder"}[5m])

# Memory usage percent
container_memory_usage_bytes{name="measure-builder"} / 1024 / 1024 / 1024 * 100

# Disk space available
node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes * 100

# Active database connections
db_hikaricp_connections_active{pool="measure-builder"}
```

### Business Metric Queries

```promql
# Measures created per day
increase(measure_created_total[1d])

# Measures published per day
increase(measure_published_total[1d])

# Evaluations run per day
increase(evaluation_executed_total[1d])

# Unique users per day
count(distinct(user_id))
```

---

## Performance Budgets

### User-Facing Budgets

```
API Response Time (P95):     <500ms
Measure Load Time:           <5 seconds
CQL Generation:              <1 second
UI Interaction Latency:      <100ms
Page Render Time:            <3 seconds
```

### Internal Budgets

```
Database Query Response:     <100ms
Cache Hit Latency:           <5ms
Cache Miss Latency:          <50ms
Message Queue Latency:       <100ms
Background Job Processing:   <30 seconds
```

---

## Alert Thresholds

### Critical Alerts (Page On-Call)

```
Error Rate > 1%              → Immediate page
Service Down                 → Immediate page
Database Connection Pool > 100 → Immediate page
Publish Failures > 5% in 5min → Immediate page
Memory Usage > 90%           → Page within 5 min
Disk Space < 5%              → Page within 10 min
```

### Warning Alerts (Notify Team)

```
Error Rate > 0.5% in 5 min   → Slack notification
P95 Response > 750ms         → Slack notification
CPU Usage > 75%              → Slack notification
Memory Usage > 80%           → Slack notification
Validation Failures > 10%    → Team notification
```

### Info Alerts (Log Only)

```
Unusual traffic patterns
Slow query detected
Cache eviction rate high
Unusual user behavior
Database maintenance complete
```

---

## Grafana Dashboards

### Main Dashboard Panels

**1. Service Health (Gauge Panel)**
- Green: Up
- Red: Down
- Shows: Last check time, uptime percentage

**2. Request Rate (Graph Panel)**
- X-axis: Time
- Y-axis: Requests/sec
- Lines: By status code (2xx, 4xx, 5xx)
- Update: Every 30 seconds

**3. Response Time Percentiles (Graph Panel)**
- Lines: P50, P95, P99
- Target bands: Green (<500ms), yellow (<750ms), red (>750ms)

**4. Error Rate (Gauge Panel)**
- Shows: Current error rate %
- Threshold: 1%

**5. Resource Usage (Stacked Bar)**
- CPU %
- Memory %
- Disk %

**6. Database Connections (Line Graph)**
- Active connections
- Threshold line: 100
- Warning line: 75

**7. Business Metrics (Stat Panel)**
- Measures Created Today
- Active Users Today
- Evaluations Run Today
- Avg Measure Complexity

**8. Top 10 Slow Endpoints (Table)**
- Endpoint path
- Average response time
- 95th percentile
- Error rate

---

## Standard Views

### Operations View (For DevOps)

Focus: Infrastructure health
- CPU, Memory, Disk usage
- Database connections
- Service uptime
- Alert status

### Engineering View (For Developers)

Focus: Application performance
- Request rates by endpoint
- Response time percentiles
- Error rates by type
- Slow query logs

### Business View (For Management)

Focus: Business metrics
- Daily usage metrics
- User growth
- Feature adoption
- Performance trends

---

## Incident Scenarios & Response

### Scenario 1: High Error Rate (> 1%)

**Detection:** Alert fires when error_rate > 1% for 5 minutes

**Investigation (5 min):**
```
1. Check recent deployments
2. Review error logs
3. Check database status
4. Check external dependencies
```

**Mitigation (5-15 min):**
```
- Rollback recent changes if applicable
- Scale up service instances
- Clear cache if applicable
- Restart service if hung
```

### Scenario 2: Slow Response Times (P95 > 750ms)

**Detection:** Alert when P95 > 750ms for 5 minutes

**Investigation:**
```
1. Identify slow endpoints
2. Check database query performance
3. Review application logs
4. Check resource utilization
```

**Mitigation:**
```
- Add database indexes
- Increase cache TTL
- Optimize queries
- Scale up database if needed
```

### Scenario 3: Database Connection Pool Exhaustion

**Detection:** Alert when active connections > 100

**Investigation:**
```
1. Check connection pool settings
2. Review active connections by query
3. Check for connection leaks
```

**Mitigation:**
```
1. Increase connection pool size
2. Restart service to clear stale connections
3. Optimize queries to release connections faster
4. Review application for connection leaks
```

---

## Monitoring Best Practices

### 1. Maintain Baseline Metrics

```
Establish what "normal" looks like:
- CPU usage: 30-50%
- Memory usage: 50-70%
- Error rate: < 0.01%
- Response time P95: 200-400ms

Review baselines monthly
Alert when deviating significantly
```

### 2. Alert Fatigue Prevention

```
✅ DO:
- Set thresholds based on SLOs
- Group related alerts
- Use severity levels
- Auto-remediate when possible

❌ DON'T:
- Alert on every minor change
- Set unrealistic thresholds
- Create too many alert channels
- Ignore warnings
```

### 3. On-Call Preparation

```
✅ Prepare:
- Document runbooks
- Share alert playbooks
- Schedule on-call rotation
- Practice incident response

Before going on-call:
- Read all runbooks
- Understand dashboards
- Know escalation procedures
```

### 4. Continuous Improvement

```
Weekly review:
- Alert accuracy (false positives/negatives)
- Response time to incidents
- MTTR (Mean Time To Recovery)
- Learning opportunities

Monthly review:
- Update thresholds based on data
- Refine alert rules
- Update runbooks
- Share learnings with team
```

---

## Related Documents

- **Incident Response:** `docs/runbooks/MEASURE_BUILDER_INCIDENT_RESPONSE.md`
- **Alert Rules:** `docker/prometheus/alerts/measure-builder-alerts.yml`
- **Deployment:** `docs/runbooks/MEASURE_BUILDER_STAGING_DEPLOYMENT.md`
- **Administration:** `docs/admin/MEASURE_BUILDER_ADMINISTRATION.md`

---

**Status:** ✅ Complete
**Last Updated:** January 18, 2026
**Contact:** monitoring@healthdatainmotion.com
