# Phase 6: Incident Response & On-Call Procedures

**Status**: ✅ Phase 6 Incident Response
**Date**: January 1, 2026
**Service**: CMS Connector Service

---

## 1. Incident Classification

### Severity Levels

| Level | Impact | MTTR Goal | Example |
|-------|--------|-----------|---------|
| **P1 - Critical** | Complete outage, data loss | <15 min | Service down, database corrupted |
| **P2 - High** | Significant degradation | <1 hour | Error rate >5%, latency doubled |
| **P3 - Medium** | Minor degradation | <4 hours | Error rate 1-5%, single feature broken |
| **P4 - Low** | Cosmetic or very minor | <24 hours | Typo, minor UI issue |

### Escalation Rules

```
Alert Triggered
    ↓
[On-Call Engineer evaluates]
    ├─ P1 → ESCALATE IMMEDIATELY
    │   ├─ Page Incident Commander
    │   ├─ Page Engineering Manager
    │   └─ Page Team Lead
    │
    ├─ P2 → ESCALATE if MTTR > 30 min
    │   ├─ Page Incident Commander
    │   └─ Notify Engineering Manager
    │
    ├─ P3 → Handle on-call
    │   └─ Notify in Slack
    │
    └─ P4 → Handle during business hours
        └─ Create ticket
```

---

## 2. Incident Notification & Alerting

### Alert to Incident Flow

```yaml
AlertManager
    ↓
[Evaluates severity]
    ├─ Critical
    │   ├─ PagerDuty: Page on-call engineer (SMS)
    │   ├─ Slack: #incidents channel (CRITICAL)
    │   └─ Email: Critical alerts group
    │
    ├─ Warning
    │   ├─ PagerDuty: Page on-call engineer (push notification)
    │   ├─ Slack: #incidents channel (WARN)
    │   └─ Email: Alerts group
    │
    └─ Info
        ├─ Slack: #monitoring channel
        └─ Grafana: Dashboard annotation
```

### On-Call Rotation Setup

```
Week 1: Engineer A
Week 2: Engineer B
Week 3: Engineer C
Week 4: Engineer D

Primary: On-call for the week
Secondary: Backup if primary unreachable
Manager: Escalation point

Setup in PagerDuty:
- Rotation: Weekly
- Escalation policy: 5 min to primary, 5 min to secondary, 5 min to manager
```

---

## 3. Incident Response Workflow

### Step 1: Acknowledge Alert (Immediately)

When receiving alert:

```bash
# On-call engineer receives:
# - SMS/Push notification from PagerDuty
# - Slack message in #incidents channel
# - Email with alert details

# Required actions (within 1 minute):
# [ ] Acknowledge alert in PagerDuty
# [ ] Check service health
curl https://cms-connector.example.com/api/v1/actuator/health

# [ ] Initial assessment: Is this a real issue?
#     YES → Continue to Step 2
#     NO → Resolve as false positive, update alert rules
```

### Step 2: Initial Triage (2-5 minutes)

```bash
#!/bin/bash
echo "=== INCIDENT TRIAGE ==="

# Assess severity
HEALTH=$(curl -s https://cms-connector.example.com/api/v1/actuator/health | jq -r '.status')
ERROR_RATE=$(curl -s http://prometheus:9090/api/v1/query?query='rate(http_requests_total{status="5xx"}[1m])' | jq '.data.result[0].value[1]')

echo "Service Status: $HEALTH"
echo "Error Rate: $ERROR_RATE"

if [ "$HEALTH" != "UP" ] || (( $(echo "$ERROR_RATE > 0.05" | bc -l) )); then
  echo "SEVERITY: P1 - CRITICAL"
  echo "ACTION: Declare incident, page incident commander"
else
  echo "SEVERITY: P2/P3 - Investigate further"
fi

# Gather initial data
echo ""
echo "=== Initial Data ==="
echo "Recent errors:"
docker logs cms-connector-service --tail 20 | grep ERROR

echo ""
echo "Database status:"
curl -s https://cms-connector.example.com/api/v1/actuator/health/db | jq '.status'

echo ""
echo "Cache status:"
curl -s https://cms-connector.example.com/api/v1/actuator/health/redis | jq '.status'
```

### Step 3: Declare Incident (P1 only)

Create incident in StatusPage/PagerDuty:

```
Title: CMS Connector Service Outage
Severity: Critical
Affected Component: API Service
Status: INVESTIGATING
Description: Service health checks failing, investigating root cause
```

### Step 4: Investigation (5-30 minutes)

**Parallel Investigation Tracks**:

**Track A: Application Health**
```bash
# Check service logs
docker logs cms-connector-service | grep -A 5 ERROR | tail -20

# Check if service is running
docker ps | grep cms-connector-service

# Check resource usage
docker stats cms-connector-service --no-stream

# Memory:
# - If 100% → OOM, need to restart
# - If >80% → Memory leak, may need restart

# CPU:
# - If 100% → High load, may need to scale
```

**Track B: Database Health**
```bash
# Can we connect?
psql -h prod-db -U healthdata -d healthdata_prod -c "SELECT 1"

# Are there connection issues?
SELECT count(*) FROM pg_stat_activity;

# Are there locks/deadlocks?
SELECT * FROM pg_locks WHERE NOT granted;

# Check recent slow queries
SELECT * FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 5;
```

**Track C: Dependencies**
```bash
# Check Redis
redis-cli -h prod-redis ping

# Check network connectivity
ping prod-db
ping prod-redis

# Check external services (if applicable)
curl https://external-api.example.com/health
```

**Track D: Monitoring**
```bash
# Check for relevant alerts in AlertManager
curl -s http://alertmanager:9093/api/v1/alerts | jq '.data'

# Check metrics in Prometheus
# - Error rate trending
# - Response time trending
# - Resource usage trending
# - Recent changes in metrics
```

### Step 5: Implement Fix

**Option A: Service Restart**
```bash
# If suspected memory leak or transient issue
docker restart cms-connector-service

# Monitor after restart
watch -n 2 'docker stats cms-connector-service --no-stream'

# Verify health
curl https://cms-connector.example.com/api/v1/actuator/health
```

**Option B: Scale Horizontally**
```bash
# If high load/CPU
# See OPERATIONS-RUNBOOK.md for scaling

# Add another instance
docker run -d --name cms-connector-service-2 ...
```

**Option C: Database Recovery**
```bash
# If database issue detected
# See BACKUP-AND-RECOVERY.md
```

**Option D: Rollback Deployment**
```bash
# If recent deployment caused issue
# See PRODUCTION-DEPLOYMENT.md Rollback Procedures
```

**Option E: Execute Remediation**
```bash
# If identified specific issue, implement fix
# e.g., kill hung connections, clear cache, etc.
```

### Step 6: Validation (After Fix)

```bash
#!/bin/bash
echo "=== POST-FIX VALIDATION ==="

# Check 1: Health
HEALTH=$(curl -s https://cms-connector.example.com/api/v1/actuator/health | jq -r '.status')
echo "Health: $HEALTH"
[ "$HEALTH" = "UP" ] || exit 1

# Check 2: Error Rate
ERROR_RATE=$(curl -s http://prometheus:9090/api/v1/query?query='rate(http_requests_total{status="5xx"}[1m])' | jq '.data.result[0].value[1]')
echo "Error Rate: $ERROR_RATE"
(( $(echo "$ERROR_RATE < 0.01" | bc -l) )) || exit 1

# Check 3: Response Time
P95=$(curl -s http://prometheus:9090/api/v1/query?query='histogram_quantile(0.95,rate(http_request_duration_seconds_bucket[5m]))' | jq '.data.result[0].value[1]')
echo "Response Time p95: ${P95}s"
(( $(echo "$P95 < 0.5" | bc -l) )) || exit 1

# Check 4: No recent errors
RECENT_ERRORS=$(docker logs cms-connector-service --since 5m | grep -c ERROR)
echo "Errors in last 5 min: $RECENT_ERRORS"
[ "$RECENT_ERRORS" = "0" ] && echo "✅ Incident resolved" || echo "⚠️ Some errors still present"
```

### Step 7: Close Incident

```
Update StatusPage:
- Status: RESOLVED
- Resolution: [Brief explanation]
- Impact: [Downtime duration, affected users]
- Root cause: [To be detailed in post-mortem]

Update PagerDuty:
- Mark incident as resolved
- Set timeline of events
```

### Step 8: Post-Incident Review (Next Day)

Schedule 30-minute review:

```
Attendees: Engineering team leads, on-call engineer, affected stakeholders

Agenda:
1. Timeline of events (5 min)
2. Root cause analysis (10 min)
3. What went well (5 min)
4. What could improve (5 min)
5. Action items (5 min)

Output: Public post-mortem document
```

---

## 4. Common Incident Scenarios

### Scenario 1: Service OOM (Out of Memory)

**Symptoms**: Container exits, no response, memory at 100%

**Diagnosis**:
```bash
docker logs cms-connector-service | tail -20 | grep -i "OutOfMemory\|OOM"
```

**Resolution**:
1. [ ] Acknowledge alert
2. [ ] Increase JVM memory limit or container memory
3. [ ] Restart service
4. [ ] Monitor memory trend
5. [ ] If still increasing → memory leak, schedule code review

### Scenario 2: Database Connection Pool Exhausted

**Symptoms**: "Connection pool exhausted" errors, slow response

**Diagnosis**:
```bash
psql -h prod-db -U healthdata -d healthdata_prod \
  -c "SELECT count(*) FROM pg_stat_activity;"
```

**Resolution**:
1. [ ] Acknowledge alert
2. [ ] Kill idle connections
3. [ ] Increase pool size
4. [ ] Monitor connection count
5. [ ] Check for connection leaks

### Scenario 3: Cascading Failure (External Dependency Down)

**Symptoms**: Circuit breaker open, timeout errors

**Diagnosis**:
```bash
# Check circuit breaker status
curl -s https://cms-connector.example.com/api/v1/actuator/health | \
  jq '.components | keys'
```

**Resolution**:
1. [ ] Verify external dependency status
2. [ ] If dependency recovers, circuit breaker auto-recovers
3. [ ] If dependency down long-term, implement fallback
4. [ ] Update status page

### Scenario 4: High Error Rate (Application Crash Loop)

**Symptoms**: Error rate >10%, alerts firing

**Diagnosis**:
```bash
docker logs cms-connector-service --tail 100 | grep -i "exception\|error" | sort | uniq -c | sort -rn
```

**Resolution**:
1. [ ] Check logs for error pattern
2. [ ] If recent deployment → rollback
3. [ ] If data corruption → restore from backup
4. [ ] If bad input → implement input validation

### Scenario 5: Network Issue (Can't Reach Service)

**Symptoms**: Connection refused, timeout

**Diagnosis**:
```bash
# Can other services reach it?
curl -I http://localhost:8081/api/v1/actuator/health

# Is it listening?
netstat -tlnp | grep 8081

# Network connectivity?
ping prod-redis
ping prod-db
```

**Resolution**:
1. [ ] Check if service is running
2. [ ] Check firewall rules
3. [ ] Check load balancer configuration
4. [ ] Check DNS resolution
5. [ ] Restart if unresponsive

---

## 5. Communication Template

### Initial Notification (First 5 minutes)

```
🚨 INCIDENT: CMS Connector Service

Severity: P1 - CRITICAL
Status: INVESTIGATING
Time: 2026-01-01 12:34:56 UTC

Impact:
- Service unavailable for 2 minutes
- ~500 failed requests
- API latency spiked to 5+ seconds

Investigating: [On-call engineer name]

We'll provide updates every 5 minutes.
```

### Update (Every 5-15 minutes)

```
🔄 UPDATE: CMS Connector Service (15:23 UTC)

Root Cause: Database connection pool exhausted
Action Taken: Increased pool size from 10 to 20

Current Status:
- Service: RECOVERING
- Error Rate: Declining (3% → 1%)
- Response Time: Improving

ETA to Resolution: 5 minutes
```

### Resolution

```
✅ RESOLVED: CMS Connector Service (15:28 UTC)

Root Cause: Query timeout caused connection pool to fill
Duration: 7 minutes total outage
Impact: ~1000 failed requests

Fix Applied:
- Increased connection pool size to 20
- Optimized slow query causing timeouts
- Increased query timeout from 30s to 60s

Service Status: NORMAL
All metrics: GREEN

Post-mortem: Tomorrow 10 AM UTC
```

---

## 6. Escalation Matrix

```
Level 1: On-Call Engineer (immediate response)
  - Can implement fixes
  - Can restart services
  - Can scale infrastructure
  - Can execute rollbacks

Level 2: Incident Commander (if MTTR > 15 min)
  - Coordinates multiple teams
  - Makes critical decisions
  - Updates stakeholders
  - Manages communications

Level 3: Engineering Manager (if MTTR > 30 min)
  - Activates war room
  - Coordinates with product/leadership
  - Approves unusual mitigations
  - Handles external communications

Level 4: VP Engineering (if severe data loss or data breach)
  - Activates incident response team
  - Handles executive communications
  - Coordinates legal/compliance
  - Prepares public statement
```

---

## 7. Tools & Automation

### PagerDuty Setup

```
Service: CMS Connector Service
  ├─ On-Call Schedule: Engineering Team (Weekly rotation)
  ├─ Escalation Policy:
  │  ├─ 5 min → Primary on-call
  │  ├─ 5 min → Secondary on-call
  │  └─ 5 min → Incident Commander
  └─ Integrations:
     ├─ AlertManager → Create incidents
     ├─ Slack → Notify #incidents
     └─ Email → Notify on-call team
```

### Slack Automation

```
Channel: #incidents

Bot integrations:
- PagerDuty → Post alert summaries
- Prometheus → Post firing alerts
- GitHub → Post deployment status
- Custom → Post service metrics

Commands:
- /incident start (create incident)
- /incident update (post update)
- /incident resolve (close incident)
- /severity P1 (set severity level)
```

### Runbooks

Create runbooks for common scenarios:
- [ ] Service OOM
- [ ] Database connection exhaustion
- [ ] High error rate
- [ ] Cascading failures
- [ ] Data inconsistency

---

## 8. Training & Drills

### On-Call Training

All engineers must complete:
- [ ] ReadMe review
- [ ] Alert system walkthrough
- [ ] Shadowing rotation (1 week)
- [ ] Dry-run incident (guided)
- [ ] Real incident handling (with backup)

### Monthly Incident Drills

```
Scenario: Service OOM incident
- Simulate service crash
- On-call engineer responds
- Follow procedures
- Measure MTTR
- Debrief
```

---

**Document Version**: 1.0
**Last Updated**: January 1, 2026
**Status**: ✅ Phase 6 Incident Response - Ready for Production
