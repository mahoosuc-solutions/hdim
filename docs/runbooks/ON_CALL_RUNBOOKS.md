# HDIM On-Call Runbooks — Pilot Environment

Consolidated incident response runbooks for the HDIM pilot environment.
All commands assume Docker Compose unless a K8s alternative is noted.

---

## Quick Reference

| Incident | Severity | SLA (Restore) | Primary Contact | Escalation Contact |
|---|---|---|---|---|
| Gateway Down | **Critical** | < 15 min | On-call engineer | Engineering Lead + CTO |
| Kafka Consumer Lag Spike | **High** | < 30 min | On-call engineer | Platform Lead |
| Service OOM / CrashLoopBackOff | **High** | < 30 min | On-call engineer | Engineering Lead |
| DB Connection Pool Exhaustion | **Critical** | < 15 min | On-call engineer | Engineering Lead + DBA |
| HIPAA Audit: PHI Access Anomaly | **Critical** | < 15 min acknowledge; < 2 hr contain | On-call engineer + Security Lead | CISO + Legal |

**Alert channels:** Slack `#critical-alerts` | PagerDuty on-call rotation
**Status page updates:** Required within 5 minutes of P1/P2 declaration

---

## Runbook 1: Gateway Down

> All services unreachable through port 8001; gateway health check fails.

### Symptoms

- Prometheus alert: `GatewayHealthCheckFailed` firing
- HTTP requests to `http://<host>:8001` returning connection refused or 502/503
- Grafana dashboard "Gateway Request Rate" shows zero traffic
- Downstream services log `Connection refused` or `No route to host` when calling APIs
- Clinical Portal shows "Service Unavailable" across all pages

### Immediate Triage (< 5 min)

```bash
# 1. Confirm gateway container state
docker compose ps hdim-gateway

# 2. Quick HTTP check
curl -sf http://localhost:8001/health || echo "GATEWAY DOWN"

# 3. Check last 50 log lines for fatal errors
docker compose logs --tail=50 hdim-gateway
```

### Root Cause Investigation

```bash
# Port binding conflict
ss -tlnp | grep 8001

# OOM kill (check exit code — 137 = OOM)
docker inspect hdim-gateway --format='{{.State.ExitCode}} {{.State.Error}}'

# Configuration or Kong plugin errors
docker compose logs hdim-gateway | grep -i "error\|fatal\|panic"

# Verify dependent services (gateway requires these to start cleanly)
docker compose ps hdim-postgres redis kafka

# Kong admin API (if Kong is the gateway implementation)
curl http://localhost:8444/status 2>/dev/null || echo "Kong admin unreachable"

# K8s alternative
kubectl get pod -l app=hdim-gateway -n healthdata-prod
kubectl describe pod -l app=hdim-gateway -n healthdata-prod | tail -30
kubectl logs -l app=hdim-gateway -n healthdata-prod --previous
```

### Resolution Steps

1. **Restart the gateway container:**

   ```bash
   docker compose restart hdim-gateway
   sleep 10
   curl -sf http://localhost:8001/health && echo "RESTORED"
   ```

2. **If restart fails — rebuild and redeploy:**

   ```bash
   docker compose stop hdim-gateway
   docker compose rm -f hdim-gateway
   docker compose up -d hdim-gateway
   docker compose logs -f hdim-gateway | head -60
   ```

3. **If configuration is corrupt — roll back to last known-good config:**

   ```bash
   git log --oneline -10 -- docker/gateway/
   git diff HEAD~1 HEAD -- docker/gateway/
   # If bad config identified:
   git checkout HEAD~1 -- docker/gateway/kong.yml
   docker compose restart hdim-gateway
   ```

4. **If upstream dependency is missing (Postgres/Redis not ready):**

   ```bash
   docker compose up -d hdim-postgres redis
   sleep 15
   docker compose restart hdim-gateway
   ```

5. **Validate restoration:**

   ```bash
   curl http://localhost:8001/health
   curl http://localhost:8001/api/v1/patients -H "X-Tenant-ID: test" \
     -H "Authorization: Bearer $TEST_TOKEN" -s -o /dev/null -w "%{http_code}"
   ```

### Escalation

Page Engineering Lead if:
- Gateway does not restore within 15 minutes
- Exit code 137 (OOM) repeats after restart — memory budget must be increased
- Configuration rollback does not resolve the issue
- Any pilot customer is actively affected

### Post-Incident

- Document in `#incident-log`: timeline, root cause, resolution command(s)
- Open a GitHub issue tagged `severity:critical gateway`
- If OOM: update `docker-compose.yml` memory limit and file PR before next shift ends
- Update Grafana alert threshold if it fired too late or too early

---

## Runbook 2: Kafka Consumer Lag Spike

> Consumer group lag exceeds 10,000 messages; real-time event processing stalled.

### Symptoms

- Prometheus alert: `KafkaConsumerLagHigh` (threshold: 10,000 messages)
- Grafana panel "Consumer Group Lag" showing rapid growth or plateau at high value
- Care gap events, quality measure evaluations, or FHIR notifications delayed or not arriving
- Service logs show `poll() timeout` or consumer rebalancing loops
- Patient-facing features show stale data that does not update

### Immediate Triage (< 5 min)

```bash
# Check consumer group lag across all groups
docker compose exec kafka \
  kafka-consumer-groups.sh --bootstrap-server localhost:9094 \
  --list | xargs -I{} kafka-consumer-groups.sh \
  --bootstrap-server localhost:9094 --describe --group {}

# Identify which topic/partition is lagging
docker compose exec kafka \
  kafka-consumer-groups.sh --bootstrap-server localhost:9094 \
  --describe --group hdim-care-gap-consumers
```

### Root Cause Investigation

```bash
# Check consumer service logs for errors or crash loops
docker compose logs --tail=100 care-gap-service | grep -i "error\|exception\|rebalance"
docker compose logs --tail=100 quality-measure-service | grep -i "error\|exception"

# Check Kafka broker health
docker compose ps kafka
docker compose logs --tail=50 kafka | grep -i "error\|warn"

# Check topic partition count vs consumer count
docker compose exec kafka \
  kafka-topics.sh --bootstrap-server localhost:9094 \
  --describe --topic care-gap-events

# Check if a consumer is stuck on a poison message
docker compose exec kafka \
  kafka-consumer-groups.sh --bootstrap-server localhost:9094 \
  --describe --group hdim-care-gap-consumers --verbose

# K8s alternative
kubectl logs -l app=care-gap-service -n healthdata-prod --tail=100 | grep -i "exception\|rebalance"
kubectl top pod -l app=care-gap-service -n healthdata-prod
```

### Resolution Steps

1. **If consumer service is down — restart it:**

   ```bash
   docker compose restart care-gap-service
   # Watch lag recover
   watch -n 5 "docker compose exec kafka kafka-consumer-groups.sh \
     --bootstrap-server localhost:9094 --describe --group hdim-care-gap-consumers"
   ```

2. **If consumer is alive but not making progress (poison message suspected):**

   ```bash
   # Identify current committed offset vs log-end offset
   docker compose exec kafka \
     kafka-consumer-groups.sh --bootstrap-server localhost:9094 \
     --describe --group hdim-care-gap-consumers

   # Skip the bad message by resetting offset on the stuck partition
   # CAUTION: confirm with Engineering Lead before skipping in production
   docker compose exec kafka \
     kafka-consumer-groups.sh --bootstrap-server localhost:9094 \
     --group hdim-care-gap-consumers --topic care-gap-events \
     --reset-offsets --to-offset <CURRENT+1> --partition <PARTITION_ID> --execute
   ```

3. **If lag is due to sustained throughput spike — scale consumers (K8s):**

   ```bash
   kubectl scale deployment care-gap-service --replicas=3 -n healthdata-prod
   # Verify additional consumers join the group
   kubectl logs -l app=care-gap-service -n healthdata-prod --tail=30 | grep "Joined group"
   ```

4. **Verify lag is recovering:**

   ```bash
   # Lag should decrease by at least 1,000 messages per minute under normal load
   docker compose exec kafka \
     kafka-consumer-groups.sh --bootstrap-server localhost:9094 \
     --describe --group hdim-care-gap-consumers
   ```

### Escalation

Page Platform Lead if:
- Lag continues to grow 10 minutes after consumer restart
- Poison message skip is needed in production (requires second approval)
- Kafka broker itself is unhealthy (disk full, broker not elected)
- Lag exceeds 100,000 messages (data integrity risk)

### Post-Incident

- Record which consumer group and topic were affected
- Note whether the lag recovered naturally or required manual offset reset
- If offset was reset: document which messages were skipped and why
- File GitHub issue for dead-letter queue implementation if not already present

---

## Runbook 3: Service OOM / CrashLoopBackOff

> A service container is restarting repeatedly due to out-of-memory errors or unhandled fatal exceptions.

### Symptoms

- Docker: `docker compose ps` shows `Restarting` status for a service
- K8s: `kubectl get pods` shows `CrashLoopBackOff` or `OOMKilled`
- Prometheus alert: `ServiceCrashLoopDetected` or `ContainerOOMKilled`
- Service logs end abruptly with no shutdown log line, or show `java.lang.OutOfMemoryError`
- Dependent services return 503 when calling the crashed service
- Grafana JVM heap graph shows sawtooth pattern (heap fills, crash, reset)

### Immediate Triage (< 5 min)

```bash
# Docker: check which services are restarting
docker compose ps | grep -i "restart\|exit"

# Get exit code (137 = OOM kill, 1 = unhandled exception)
docker inspect <service-name> --format='Exit: {{.State.ExitCode}}  Error: {{.State.Error}}'

# Last logs before crash
docker compose logs --tail=80 <service-name>

# K8s
kubectl get pods -n healthdata-prod
kubectl describe pod <pod-name> -n healthdata-prod | grep -A10 "Last State\|OOMKilled\|Reason"
kubectl logs <pod-name> -n healthdata-prod --previous | tail -80
```

### Root Cause Investigation

```bash
# Java OOM: look for heap dump trigger or GC overhead limit
docker compose logs <service-name> | grep -i "OutOfMemoryError\|GC overhead\|heap space"

# Check configured JVM memory vs container limit
docker inspect <service-name> | jq '.[0].HostConfig.Memory'
# Compare to JAVA_OPTS in docker-compose.yml:
grep -A5 "<service-name>" docker-compose.yml | grep JAVA_OPTS

# Check if a specific endpoint is triggering unbounded data loading
docker compose logs <service-name> | grep -B5 "OutOfMemoryError"

# K8s: check resource requests/limits
kubectl get pod <pod-name> -n healthdata-prod -o json | \
  jq '.spec.containers[].resources'

# Prometheus: memory usage trend before crash
# Query: jvm_memory_used_bytes{service="<service-name>"}
# View in Grafana: http://localhost:3001  Dashboard: JVM Overview
```

**Common culprits by service:**

| Service | Port | Common OOM Cause |
|---|---|---|
| patient-service | 8084 | Unbounded patient query (missing pagination) |
| fhir-service | 8085 | Large FHIR Bundle serialization |
| care-gap-service | 8086 | CQL evaluation loading full patient history |
| quality-measure-service | 8087 | Batch evaluation of all measures simultaneously |

### Resolution Steps

1. **Immediate stabilization — restart with increased memory:**

   ```bash
   # Edit docker-compose.yml or override:
   # environment:
   #   JAVA_OPTS: "-Xms512m -Xmx1g"
   # deploy:
   #   resources:
   #     limits:
   #       memory: 1.5G

   docker compose stop <service-name>
   docker compose up -d <service-name>
   docker compose logs -f <service-name> | head -40
   ```

2. **K8s: patch memory limit immediately:**

   ```bash
   kubectl set resources deployment/<service-name> \
     --limits=memory=1.5Gi --requests=memory=768Mi \
     -n healthdata-prod
   kubectl rollout status deployment/<service-name> -n healthdata-prod
   ```

3. **Enable heap dump on OOM for root cause analysis (non-prod):**

   ```yaml
   # In docker-compose.yml environment:
   JAVA_OPTS: "-Xmx768m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof"
   ```

   ```bash
   docker compose up -d <service-name>
   # After next OOM crash, extract dump:
   docker cp <service-name>:/tmp/heapdump.hprof ./heapdump-$(date +%Y%m%d-%H%M).hprof
   ```

4. **Validate recovery:**

   ```bash
   docker compose ps <service-name>
   curl http://localhost:<PORT>/actuator/health
   # Watch JVM heap in Grafana for 5 minutes after restart
   ```

### Escalation

Page Engineering Lead if:
- Crash loop continues after increasing memory to 2G
- Exit code is not 137 (non-OOM crash — may indicate data corruption or misconfiguration)
- Multiple services are crashing simultaneously
- Pilot customer data may have been lost or corrupted (check Kafka for uncommitted events)

### Post-Incident

- Record service name, exit code, crash time, and memory limit at time of crash
- Attach relevant log snippet to GitHub issue
- File a code-level fix PR (pagination, streaming, lazy loading) — memory increases are temporary
- Update Prometheus alert threshold if it fired too late

---

## Runbook 4: Database Connection Pool Exhaustion

> HikariCP "Connection is not available, request timed out" errors; services returning HTTP 500s.

### Symptoms

- Service logs: `HikariPool-1 - Connection is not available, request timed out after 30000ms`
- Grafana: "DB Connection Pool Active" at 100% (all connections consumed)
- Prometheus alert: `HikariPoolExhausted` firing
- HTTP endpoints returning 500 across multiple services
- Postgres log: `FATAL: remaining connection slots are reserved for non-replication superuser connections`
- `docker compose logs <service>` shows `Unable to acquire JDBC Connection`

### Immediate Triage (< 5 min)

```bash
# Check active connections to Postgres right now
docker compose exec hdim-postgres psql -U healthdata -c \
  "SELECT count(*), state, wait_event_type, wait_event \
   FROM pg_stat_activity WHERE datname IS NOT NULL \
   GROUP BY state, wait_event_type, wait_event ORDER BY count DESC;"

# Check total connection limit vs current
docker compose exec hdim-postgres psql -U healthdata -c \
  "SELECT max_conn, used, res_for_super, max_conn - used - res_for_super AS available \
   FROM (SELECT count(*) used FROM pg_stat_activity) t, \
   (SELECT setting::int max_conn FROM pg_settings WHERE name='max_connections') q, \
   (SELECT setting::int res_for_super FROM pg_settings WHERE name='superuser_reserved_connections') s;"

# Which service is holding the most connections?
docker compose exec hdim-postgres psql -U healthdata -c \
  "SELECT application_name, count(*) FROM pg_stat_activity \
   GROUP BY application_name ORDER BY count DESC;"
```

### Root Cause Investigation

```bash
# Long-running queries holding connections
docker compose exec hdim-postgres psql -U healthdata -c \
  "SELECT pid, now() - pg_stat_activity.query_start AS duration, query, state \
   FROM pg_stat_activity \
   WHERE state != 'idle' AND query_start < now() - interval '30 seconds' \
   ORDER BY duration DESC;"

# Idle connections not being returned to pool
docker compose exec hdim-postgres psql -U healthdata -c \
  "SELECT application_name, count(*), state FROM pg_stat_activity \
   WHERE state = 'idle in transaction' GROUP BY application_name, state;"

# Check HikariCP pool config in each service
docker compose exec <service-name> env | grep -i "hikari\|pool\|jdbc\|maximum-pool"

# Locks blocking queries
docker compose exec hdim-postgres psql -U healthdata -c \
  "SELECT blocked_locks.pid AS blocked_pid, blocking_locks.pid AS blocking_pid, \
   blocked_activity.query AS blocked_statement \
   FROM pg_catalog.pg_locks blocked_locks \
   JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_locks.pid = blocked_activity.pid \
   JOIN pg_catalog.pg_locks blocking_locks ON blocking_locks.locktype = blocked_locks.locktype \
     AND blocking_locks.granted AND NOT blocked_locks.granted \
   LIMIT 20;"

# K8s: check which pod is the offender
kubectl logs -l app=patient-service -n healthdata-prod --tail=50 | grep -i "HikariPool\|connection"
```

### Resolution Steps

1. **Kill long-running or idle-in-transaction connections (emergency only):**

   ```bash
   # List candidates
   docker compose exec hdim-postgres psql -U healthdata -c \
     "SELECT pid, query_start, state, query FROM pg_stat_activity \
      WHERE state = 'idle in transaction' AND query_start < now() - interval '2 minutes';"

   # Terminate specific PID (replace <pid>)
   docker compose exec hdim-postgres psql -U healthdata -c \
     "SELECT pg_terminate_backend(<pid>);"

   # Terminate all idle-in-transaction older than 2 minutes (batch)
   docker compose exec hdim-postgres psql -U healthdata -c \
     "SELECT pg_terminate_backend(pid) FROM pg_stat_activity \
      WHERE state = 'idle in transaction' AND query_start < now() - interval '2 minutes';"
   ```

2. **Restart the offending service to release its pool:**

   ```bash
   docker compose restart <service-name>
   # Confirm connections drop
   docker compose exec hdim-postgres psql -U healthdata -c \
     "SELECT application_name, count(*) FROM pg_stat_activity GROUP BY application_name ORDER BY count DESC;"
   ```

3. **Temporarily increase pool size (short-term relief, not a fix):**

   ```bash
   # Add to docker-compose.yml environment for the offending service:
   # SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE: 30
   docker compose stop <service-name>
   docker compose up -d <service-name>
   ```

4. **Increase Postgres max_connections if all services are legitimately under load:**

   ```bash
   # Current setting
   docker compose exec hdim-postgres psql -U healthdata -c \
     "SHOW max_connections;"

   # Set in postgres config (requires restart)
   docker compose exec hdim-postgres psql -U healthdata -c \
     "ALTER SYSTEM SET max_connections = 300;"
   docker compose restart hdim-postgres
   # NOTE: All services will reconnect — expect 30-60s disruption
   ```

5. **Validate recovery:**

   ```bash
   curl http://localhost:8084/actuator/health
   curl http://localhost:8086/actuator/health
   # Confirm pool metrics in Grafana: "HikariCP Active Connections" < 80% of pool size
   ```

### Escalation

Page Engineering Lead if:
- Connection leak persists after service restart
- `pg_terminate_backend` calls are needed more than once in an hour
- Postgres itself crashes or runs out of disk
- Any pilot tenant reports data loss or failed writes

### Post-Incident

- Record which service caused the exhaustion and at what time
- Document how many connections were killed and the query text of blocked statements
- File a code-level fix: ensure all `@Transactional` methods complete promptly, add connection timeout logs
- Review HikariCP `connectionTimeout`, `idleTimeout`, and `maxLifetime` settings across all services
- Add Grafana alert for pool utilization > 80% (proactive threshold)

---

## Runbook 5: HIPAA Audit — PHI Access Anomaly

> Unusual volume or pattern of PHI access events detected; potential unauthorized access or data exfiltration.

### Symptoms

- Prometheus alert: `PHIAccessAnomalyDetected` — access rate > 3x baseline for a tenant
- Grafana: "PHI Access Events" panel shows unexpected spike outside business hours
- Audit service logs show a single user or service account accessing hundreds of patient records in minutes
- Failed authentication attempts followed immediately by successful bulk PHI access
- `X-Tenant-ID` header in access logs does not match the authenticated user's assigned tenant
- Clinical Portal audit trail shows access from an unfamiliar IP or geographic region

**This is a potential HIPAA Breach — treat with confidentiality. Do not discuss in public Slack channels.**

### Immediate Triage (< 5 min)

```bash
# 1. Identify scope: which tenant and resource type is affected
docker compose logs --tail=200 audit-service | grep -i "PHI_ACCESS\|PATIENT_ACCESS" | \
  awk '{print $0}' | sort | uniq -c | sort -rn | head -20

# 2. Identify the user/token generating the access volume
docker compose logs --tail=500 patient-service | grep -i "X-Auth-User\|tenantId" | \
  grep -v "health\|actuator" | sort | uniq -c | sort -rn | head -20

# 3. Check if access is still ongoing
docker compose logs -f patient-service | grep -i "PHI_ACCESS\|getPatient\|findByIdAndTenant"
```

### Root Cause Investigation

```bash
# Query audit log for access events in the anomaly window
# Replace <START_TIME> and <TENANT_ID> with actual values
docker compose logs audit-service --since="<START_TIME>" | \
  grep "PHI_ACCESS" | grep "<TENANT_ID>" | \
  jq -r '[.timestamp, .userId, .resourceType, .resourceId, .sourceIp] | @tsv' 2>/dev/null

# Identify source IP addresses
docker compose logs hdim-gateway --since="<START_TIME>" | \
  grep -v "health\|actuator" | awk '{print $1}' | sort | uniq -c | sort -rn | head -20

# Check if a valid JWT was used (vs token replay / forged header)
docker compose logs hdim-gateway --since="<START_TIME>" | grep -i "401\|403\|invalid token\|expired"

# Verify tenant isolation — did any query cross tenant boundaries?
docker compose logs patient-service --since="<START_TIME>" | \
  grep -i "tenantId\|X-Tenant-ID" | grep -v "<EXPECTED_TENANT_ID>"

# K8s: pull audit logs from audit-service pod
kubectl logs -l app=audit-service -n healthdata-prod --since=30m | grep PHI_ACCESS | \
  sort | uniq -c | sort -rn | head -30

# Check if a service account (not a human user) is the source
# Service-to-service calls use X-Auth-Roles: SERVICE
docker compose logs patient-service --since="<START_TIME>" | grep "X-Auth-Roles: SERVICE"
```

### Resolution Steps

**Step 1 — Contain (< 15 minutes from detection):**

```bash
# If specific user JWT is identified, revoke their session in Redis
docker compose exec redis redis-cli -p 6380 \
  KEYS "session:*" | xargs -I{} redis-cli -p 6380 GET {} | grep "<USER_ID>"

# Revoke the identified session token
docker compose exec redis redis-cli -p 6380 DEL "session:<SESSION_KEY>"

# If source IP is identified, block at gateway level (Kong or nginx)
# Add to gateway config or use Kong Admin API:
curl -X POST http://localhost:8444/plugins \
  --data "name=ip-restriction" \
  --data "config.deny[]=<MALICIOUS_IP>"
```

**Step 2 — Preserve evidence:**

```bash
# Capture complete audit log for the anomaly window BEFORE any log rotation
docker compose logs audit-service --since="<INCIDENT_START>" > \
  /tmp/phi-anomaly-$(date +%Y%m%d-%H%M)-audit.log

docker compose logs patient-service --since="<INCIDENT_START>" > \
  /tmp/phi-anomaly-$(date +%Y%m%d-%H%M)-patient.log

docker compose logs hdim-gateway --since="<INCIDENT_START>" > \
  /tmp/phi-anomaly-$(date +%Y%m%d-%H%M)-gateway.log

# Record the exact time range and affected resources
echo "Incident window: <START> to <END>" >> /tmp/phi-anomaly-summary.txt
echo "Tenant affected: <TENANT_ID>" >> /tmp/phi-anomaly-summary.txt
echo "Records accessed: <COUNT>" >> /tmp/phi-anomaly-summary.txt
```

**Step 3 — Assess breach scope:**

- Count distinct patient IDs accessed during the anomaly window
- Determine if the access was from an authenticated internal user, external IP, or compromised service account
- Determine if any PHI was written to logs, exported, or transmitted outside the system
- Cross-reference with Jaeger traces: `http://localhost:16686` — search by `user.id=<USER_ID>`

**Step 4 — Notify (time-sensitive):**

- Immediately notify: Security Lead, CISO, Legal
- If more than 500 patient records are affected: HIPAA breach notification rules apply (60-day clock starts)
- Do NOT notify the suspected user until Legal clears it
- Update incident log in private channel `#security-incidents` (not `#critical-alerts`)

### Escalation

Page CISO and Legal immediately if:
- Any PHI access occurred outside the authenticated user's assigned tenant
- More than 500 patient records were accessed in the anomaly window
- Access originated from an external IP not associated with a pilot customer
- A service account token appears to have been stolen or replayed
- Any PHI appears in application logs (potential secondary exposure)

**This runbook does not replace the formal HIPAA Breach Assessment Process. Legal must be involved for any confirmed or suspected breach.**

### Post-Incident

- Complete HIPAA Breach Risk Assessment within 24 hours (Legal-led)
- Preserve all log files for minimum 6 years (HIPAA retention requirement)
- File a security incident report in the ticketing system (private, restricted access)
- Conduct a 48-hour postmortem: how was anomaly detected, what was the root cause, what controls failed
- Implement or tune Prometheus alert thresholds if the anomaly was not caught promptly
- Review and update JWT expiry, session TTLs, and rate limiting on PHI endpoints
- If a human user caused the anomaly: HR and Legal determine next steps (not on-call engineer)

---

## Appendix: Common Diagnostic Commands

```bash
# Overall system health snapshot
docker compose ps
curl http://localhost:9090/api/v1/query?query=up | jq '.data.result[] | {job:.metric.job, up:.value[1]}'

# All service health endpoints
for port in 8084 8085 8086 8087; do
  echo -n "Port $port: "
  curl -sf http://localhost:$port/actuator/health | jq -r '.status' 2>/dev/null || echo "UNREACHABLE"
done

# Grafana (dashboards): http://localhost:3001
# Prometheus: http://localhost:9090
# Jaeger (traces): http://localhost:16686

# Redis health
docker compose exec redis redis-cli -p 6380 PING

# Kafka broker health
docker compose exec kafka kafka-broker-api-versions.sh --bootstrap-server localhost:9094

# Postgres connectivity
docker compose exec hdim-postgres psql -U healthdata -c "SELECT 1;"
```

---

_Runbooks version: 1.0_
_Last updated: February 2026_
_Owner: Engineering On-Call Team_
_Review cycle: Monthly or after any P1/P2 incident_
