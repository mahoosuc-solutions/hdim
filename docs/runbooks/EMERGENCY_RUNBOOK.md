# HDIM Emergency Runbook

Last updated: March 6, 2026 | Version: 1.0

Quick-reference emergency procedures for production incidents. For detailed troubleshooting, see [Troubleshooting Guide](../troubleshooting/README.md). For deployment rollback, see [Deployment Runbook](../DEPLOYMENT_RUNBOOK.md).

---

## Incident Severity Classification

| Severity | Definition | Response Time | Escalation | Communication |
|----------|-----------|---------------|------------|---------------|
| **P1 - Critical** | Service down, data loss risk, PHI breach suspected | 15 min | Immediate: Engineering Lead + VP Eng | Every 15 min to stakeholders |
| **P2 - Major** | Degraded performance (>5x latency), single tenant impacted, partial outage | 30 min | 30 min: Engineering Lead | Every 30 min to stakeholders |
| **P3 - Minor** | Non-critical feature broken, workaround available | 4 hours | Next business day | Daily summary |
| **P4 - Low** | Cosmetic issue, minor inconvenience | Next sprint | N/A | Release notes |

**Escalation triggers** (auto-upgrade severity):
- P2 unresolved for 1 hour -> P1
- P3 affecting >1 tenant -> P2
- Any suspected PHI exposure -> P1

---

## Incident Command Structure

```
Incident Commander (IC)
  |-- Communications Lead (status updates, stakeholder comms)
  |-- Technical Lead (diagnosis, remediation)
       |-- On-call engineer(s)
```

**IC responsibilities:** Owns decision-making. Declares severity. Approves risky actions. Manages handoffs.

**Shift handoff checklist:**
1. Current severity and timeline of events
2. What's been tried and what failed
3. Current hypothesis and next action
4. Who has been notified
5. Open action items

---

## Quick Decision Trees

### Service is down

```
Service health check failing?
  |-- Yes: Check logs -> docker compose logs -f SERVICE | tail -100
  |    |-- OOM / Exit 137 -> Restart with more memory: Increase JVM heap in docker-compose.yml
  |    |-- DB connection refused -> Jump to "Database Recovery"
  |    |-- Kafka timeout -> Jump to "Kafka Recovery"
  |    |-- Spring context failure -> Check recent config changes, roll back if needed
  |-- No (healthy but errors): Check error rate in Grafana -> http://localhost:3001
       |-- 401/403 spike -> Jump to "Gateway/Auth Recovery"
       |-- 500 errors -> Check application logs for stack trace
       |-- Timeout errors -> Jump to "Performance Emergency"
```

### Suspected PHI breach

```
1. STOP - Do not destroy evidence
2. Preserve audit logs: docker compose exec hdim-postgres pg_dump -t audit_log > audit_backup_$(date +%s).sql
3. Isolate affected service: docker compose stop SERVICE
4. Notify IC + Compliance Officer immediately
5. Follow HIPAA Incident Response Plan: docs/compliance/INCIDENT_RESPONSE_PLAN.md
6. Begin evidence collection (see "Evidence Preservation" below)
```

---

## Production Hotfix Procedure

**When to hotfix:** P1 or P2 that cannot wait for next scheduled release.

```bash
# 1. Create hotfix branch from latest tag
git fetch --tags
git checkout -b hotfix/ISSUE-ID $(git describe --tags --abbrev=0)

# 2. Apply minimal fix (smallest possible change)
# Edit files...

# 3. Run minimum test gate
cd backend
./gradlew testUnit --no-daemon          # Must pass
./gradlew :modules:services:AFFECTED_SERVICE:test --no-daemon  # Must pass

# 4. Build and deploy affected service only
./gradlew :modules:services:AFFECTED_SERVICE:bootJar -x test
docker compose build AFFECTED_SERVICE
docker compose up -d AFFECTED_SERVICE

# 5. Verify fix
docker compose logs -f AFFECTED_SERVICE | head -50
curl -f http://localhost:PORT/actuator/health

# 6. Merge hotfix back to master
git checkout master
git merge hotfix/ISSUE-ID
git tag -a vX.Y.Z-hotfix.1 -m "Hotfix: ISSUE-ID description"
git push origin master --tags

# 7. Delete hotfix branch
git branch -d hotfix/ISSUE-ID
```

**Hotfix testing minimums:**
- Unit tests for affected service must pass
- Manual smoke test of the specific fix
- Health check passes
- No new errors in logs for 5 minutes

---

## Service-Specific Recovery

### Database Recovery (PostgreSQL)

**Connection pool exhaustion:**
```bash
# Check active connections
docker compose exec hdim-postgres psql -U healthdata -c "SELECT count(*) FROM pg_stat_activity;"

# Kill idle connections older than 5 minutes
docker compose exec hdim-postgres psql -U healthdata -c \
  "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE state = 'idle' AND state_change < now() - interval '5 minutes';"

# Restart affected service to reset pool
docker compose restart AFFECTED_SERVICE
```

**Slow query remediation:**
```bash
# Find long-running queries
docker compose exec hdim-postgres psql -U healthdata -c \
  "SELECT pid, now() - pg_stat_activity.query_start AS duration, query FROM pg_stat_activity WHERE state != 'idle' ORDER BY duration DESC LIMIT 10;"

# Kill a specific long-running query (use pid from above)
docker compose exec hdim-postgres psql -U healthdata -c "SELECT pg_cancel_backend(PID);"

# If pg_cancel_backend doesn't work (query won't stop):
docker compose exec hdim-postgres psql -U healthdata -c "SELECT pg_terminate_backend(PID);"
```

**Point-in-time recovery:**
```bash
# Stop affected services first
docker compose stop patient-service care-gap-service fhir-service

# Restore from backup (adjust timestamp)
docker compose exec hdim-postgres psql -U healthdata -c \
  "SELECT pg_is_in_recovery();"  # Verify not already recovering

# Use pg_dump backup if available
docker compose exec hdim-postgres pg_restore -d healthdata_db /backups/latest.dump

# Restart services
docker compose start patient-service care-gap-service fhir-service
```

**Liquibase migration rollback (emergency):**
```bash
# Check current migration state
docker compose exec hdim-postgres psql -U healthdata -d SERVICE_db -c \
  "SELECT id, filename, dateexecuted FROM databasechangelog ORDER BY dateexecuted DESC LIMIT 5;"

# Rollback last changeset (service must have rollback directives)
cd backend
./gradlew :modules:services:SERVICE:rollbackCount -PliquibaseCommandValue=1
```

### Kafka Recovery

**Consumer lag spike:**
```bash
# Check consumer group lag
docker compose exec kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --describe --group SERVICE-group

# If lag is caused by poison message, skip to next offset
docker compose exec kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --group SERVICE-group \
  --topic TOPIC --reset-offsets --shift-by 1 --execute
```

**Reset consumer to timestamp (replay from known-good point):**
```bash
docker compose exec kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --group SERVICE-group \
  --topic TOPIC --reset-offsets --to-datetime 2026-03-06T00:00:00.000 --execute
```

**Broker not responding:**
```bash
# Check broker health
docker compose logs kafka | tail -50

# Restart Kafka (will trigger consumer rebalance)
docker compose restart kafka

# Wait for ISR (in-sync replicas) recovery
docker compose exec kafka kafka-topics.sh \
  --bootstrap-server localhost:9092 --describe --under-replicated-partitions
```

### Redis Cache Recovery

**Targeted cache invalidation:**
```bash
# Flush specific key pattern (e.g., all patient cache)
docker compose exec redis redis-cli KEYS "patient:*" | xargs -I{} docker compose exec redis redis-cli DEL {}

# Flush specific database
docker compose exec redis redis-cli -n 0 FLUSHDB

# Full flush (last resort - causes cache warming storm)
docker compose exec redis redis-cli FLUSHALL
```

**Memory pressure:**
```bash
# Check memory usage
docker compose exec redis redis-cli INFO memory | grep used_memory_human

# Check eviction stats
docker compose exec redis redis-cli INFO stats | grep evicted_keys

# Force eviction of expired keys
docker compose exec redis redis-cli --scan --pattern "*" | head -1000 | xargs -I{} docker compose exec redis redis-cli TTL {}
```

**Cache bypass (serve fresh data while cache is unhealthy):**
```bash
# Restart affected service with cache disabled
docker compose stop AFFECTED_SERVICE
SPRING_CACHE_TYPE=none docker compose up -d AFFECTED_SERVICE
# Remember to re-enable cache after Redis recovers
```

### Gateway/Auth Recovery

**JWT signing key rotation (emergency):**
```bash
# Generate new JWT secret
NEW_SECRET=$(openssl rand -hex 64)

# Update gateway config
# Edit .env: JWT_SECRET=$NEW_SECRET

# Restart gateway (all active sessions will be invalidated)
docker compose restart gateway

# Note: All users will need to re-authenticate
```

**Rate limiter stuck/blocking legitimate traffic:**
```bash
# Clear rate limit counters in Redis
docker compose exec redis redis-cli KEYS "rate_limit:*" | xargs -I{} docker compose exec redis redis-cli DEL {}

# Restart gateway
docker compose restart gateway
```

### FHIR/External System Recovery

**FHIR server connectivity failure:**
```bash
# Test connectivity
curl -f http://localhost:8085/fhir/metadata

# Check if external FHIR endpoints are reachable
curl -sf https://EXTERNAL_FHIR_SERVER/.well-known/smart-configuration

# Restart FHIR service with extended timeouts
FHIR_CLIENT_TIMEOUT_MS=30000 docker compose up -d fhir-service
```

### CQL Evaluation Recovery

**Stalled evaluation:**
```bash
# Check CQL engine health
curl -f http://localhost:8081/actuator/health

# Check for stuck Kafka consumers
docker compose exec kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --describe --group cql-engine-group

# Restart CQL engine (evaluations will resume from last committed offset)
docker compose restart cql-engine-service
```

---

## Multi-Tenant Incident Handling

**Isolate a single tenant (performance impact):**
```bash
# Option 1: Rate-limit at gateway level
# Add tenant-specific rate limit in Kong/gateway config

# Option 2: Identify and kill tenant's heavy queries
docker compose exec hdim-postgres psql -U healthdata -c \
  "SELECT pid, query FROM pg_stat_activity WHERE query LIKE '%tenant_id%TENANT_ID%' AND state = 'active';"
```

**Suspected cross-tenant data bleed:**
1. Immediately classify as P1
2. Preserve audit logs for both tenants
3. Run tenant isolation verification:
```bash
# Check if any queries lack tenant_id filter
docker compose exec hdim-postgres psql -U healthdata -c \
  "SELECT schemaname, relname, seq_scan, idx_scan FROM pg_stat_user_tables WHERE schemaname = 'public' ORDER BY seq_scan DESC LIMIT 20;"
```
4. Follow PHI breach procedures if confirmed

---

## Performance Emergency

**P95 latency spike (>5x normal):**
```bash
# 1. Check which service is slow
for port in 8081 8084 8085 8086 8087; do
  echo "Port $port: $(curl -s -o /dev/null -w '%{time_total}' http://localhost:$port/actuator/health)"
done

# 2. Check database query performance
docker compose exec hdim-postgres psql -U healthdata -c \
  "SELECT calls, mean_exec_time, query FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 10;"

# 3. Check JVM heap pressure
curl -s http://localhost:PORT/actuator/metrics/jvm.memory.used | jq '.measurements[0].value'

# 4. Emergency: Enable circuit breaker / shed load
# Restart service with reduced thread pool
SPRING_TASK_EXECUTION_POOL_CORE_SIZE=2 docker compose up -d AFFECTED_SERVICE
```

---

## Evidence Preservation

For P1 incidents and any suspected breach:

```bash
# Create incident evidence directory
INCIDENT_ID="INC-$(date +%Y%m%d-%H%M%S)"
mkdir -p /tmp/evidence/$INCIDENT_ID

# Capture service logs
docker compose logs --since 1h > /tmp/evidence/$INCIDENT_ID/all-logs.txt

# Capture database state
docker compose exec hdim-postgres pg_dump -U healthdata > /tmp/evidence/$INCIDENT_ID/db-snapshot.sql

# Capture audit logs
docker compose exec hdim-postgres psql -U healthdata -c \
  "COPY (SELECT * FROM audit_log WHERE created_at > now() - interval '2 hours') TO STDOUT WITH CSV HEADER" \
  > /tmp/evidence/$INCIDENT_ID/audit-log.csv

# Capture running state
docker compose ps > /tmp/evidence/$INCIDENT_ID/docker-state.txt
docker stats --no-stream > /tmp/evidence/$INCIDENT_ID/resource-usage.txt

# Capture Kafka consumer state
docker compose exec kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --describe --all-groups \
  > /tmp/evidence/$INCIDENT_ID/kafka-consumers.txt
```

---

## Communication Templates

### P1 - Initial Notification (within 15 minutes)

```
Subject: [P1 INCIDENT] HDIM - {Brief Description}

Severity: P1 - Critical
Started: {HH:MM UTC}
Impact: {What is affected - which tenants, which features}
Status: Investigating
IC: {Name}

Next update: {HH:MM UTC} (15 min)
War room: {link/bridge}
```

### P1 - Resolution Notification

```
Subject: [RESOLVED] HDIM - {Brief Description}

Severity: P1 - Critical
Duration: {start} to {end} ({X} minutes)
Impact: {Summary of what was affected}
Root Cause: {1-sentence summary}
Resolution: {What was done to fix it}

Postmortem scheduled: {date/time}
```

### Status Page Update (during incident)

```
[HH:MM UTC] Investigating: We are aware of {issue description} affecting {scope}.
[HH:MM UTC] Identified: Root cause identified as {brief cause}. Working on fix.
[HH:MM UTC] Monitoring: Fix deployed. Monitoring for stability.
[HH:MM UTC] Resolved: Issue has been resolved. All services operating normally.
```

---

## Post-Incident Review Template

Schedule within 48 hours of P1/P2 resolution. Blameless — focus on systems, not individuals.

```markdown
# Post-Incident Review: INC-YYYYMMDD-HHMMSS

## Summary
- **Duration:** {start} to {end}
- **Severity:** P{N}
- **Impact:** {users/tenants affected, features impacted}
- **Detection:** {How was it detected? Alert, customer report, internal?}
- **Resolution:** {1-sentence summary}

## Timeline (UTC)
| Time | Event |
|------|-------|
| HH:MM | First alert / customer report |
| HH:MM | IC declared, investigation started |
| HH:MM | Root cause identified |
| HH:MM | Fix deployed |
| HH:MM | Monitoring confirmed stable |
| HH:MM | Incident closed |

## Root Cause Analysis
{5 Whys or equivalent}

## What Went Well
- {item}

## What Could Be Improved
- {item}

## Action Items
| Action | Owner | Priority | Due Date |
|--------|-------|----------|----------|
| {item} | {name} | P1/P2/P3 | {date} |

## Lessons Learned
- {item}
```

---

## Rollback Quick Reference

See [Rollback Procedures](../ROLLBACK_PROCEDURES.md) for full details.

```bash
# Quick rollback to last known-good tag
LAST_TAG=$(git describe --tags --abbrev=0 HEAD~1)
git checkout $LAST_TAG

# Rebuild and deploy affected service
cd backend
./gradlew :modules:services:AFFECTED_SERVICE:bootJar -x test
docker compose build AFFECTED_SERVICE
docker compose up -d AFFECTED_SERVICE

# Verify
curl -f http://localhost:PORT/actuator/health
```

---

## Related Documentation

| Document | Purpose |
|----------|---------|
| [Troubleshooting Guide](../troubleshooting/README.md) | Symptom-based diagnosis decision trees |
| [Deployment Runbook](../DEPLOYMENT_RUNBOOK.md) | Standard deployment and rollback procedures |
| [Rollback Procedures](../ROLLBACK_PROCEDURES.md) | Detailed rollback steps |
| [HIPAA Incident Response](../compliance/INCIDENT_RESPONSE_PLAN.md) | PHI breach response procedures |
| [Production Security Guide](../PRODUCTION_SECURITY_GUIDE.md) | Security incident classification |
| [Disaster Recovery](../product/02-architecture/disaster-recovery.md) | DR/BC procedures, RTO/RPO targets |
| [On-Call Runbooks](./ON_CALL_RUNBOOKS.md) | Service-specific on-call procedures |
