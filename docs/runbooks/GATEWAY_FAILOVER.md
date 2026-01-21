# Gateway Failover Procedures Runbook

Operational guide for handling gateway service failures, performing failover to backup gateways, and recovering from outages with minimal downtime.

---

## Overview

HDIM deploys **4 specialized gateway services** that serve as the entry point for all client requests:

```
├── Gateway Admin (port 8002) → Admin operations
├── Gateway Clinical (port 8003) → Clinical workflows
├── Gateway FHIR (port 8004) → FHIR R4 resources
└── Gateway General (port 8001) → General queries, reports
```

**Each gateway**:
- Validates JWT tokens
- Injects X-Auth-* headers (trusted by backend services)
- Routes requests to appropriate backend services
- Handles rate limiting and circuit breakers
- Maintains session state in Redis

**Failover strategy**:
- Individual gateway failures isolated (only that gateway type affected)
- DNS round-robin across gateway instances (for HA deployments)
- Redis session replication (sticky sessions maintained)
- Automatic health checks and service restart

---

## Architecture

### Gateway Request Flow

```
Client Request
    ↓
┌─────────────────────────────────────────┐
│ Load Balancer (if HA deployment)        │
│ Routes to gateway instance 1 or 2       │
└────┬────────────────────────────────────┘
     ↓
┌─────────────────────────────────────────┐
│ Gateway Service (8001-8004)             │
│ 1. Validate JWT token                   │
│ 2. Extract claims (user_id, roles)      │
│ 3. Inject X-Auth-* headers              │
│ 4. Sign with HMAC (trust proof)         │
│ 5. Route to backend service             │
└────┬────────────────────────────────────┘
     ↓
┌─────────────────────────────────────────┐
│ Backend Service (8080-8087)             │
│ 1. Verify X-Auth-* header signature     │
│ 2. Extract user/tenant from headers     │
│ 3. Process request with tenant context  │
│ 4. Return response to gateway           │
└────┬────────────────────────────────────┘
     ↓
Response to Client
```

### Session Management

Gateway sessions stored in Redis:
- **Session key**: `session:{sessionId}`
- **Value**: Serialized authentication state
- **TTL**: 15 minutes (sliding window)
- **Replication**: All gateway instances can read/write

### State that Must Survive Failover

| State | Storage | Replication | Recovery |
|-------|---------|-------------|----------|
| **JWT Claims** | Memory (gateway) | NO | Re-auth required |
| **Session Data** | Redis | YES | Automatic |
| **Rate Limit State** | Redis | YES | Automatic |
| **Circuit Breaker State** | Memory (gateway) | NO | Resets on restart |

---

## When Failover Is Needed

### ✅ When to Trigger Manual Failover

**Scenario 1: Gateway Service Crashed**
- Health check fails for 2+ consecutive checks (30 seconds)
- Container exited abnormally
- Service not responding on port

**Scenario 2: Gateway Hung/Unresponsive**
- Service running but not processing requests
- High latency (>30 seconds) on requests
- Connection timeouts from clients

**Scenario 3: Gateway Memory Leak**
- Memory usage steadily increasing
- Approaching JVM heap limit
- Garbage collection pauses increasing

**Scenario 4: Cascading Failure**
- Gateway causing backend service overload
- Circuit breaker open to most backends
- Errors propagating through system

### ❌ When NOT to Failover

- Brief intermittent errors (wait for automatic recovery)
- Client-side issues (invalid JWT, bad request)
- Network latency spikes <1 minute
- Single failed request (retry client-side)

---

## Pre-Failover Checklist

Before manually triggering failover, verify:

```
DIAGNOSTIC CHECKS:
☐ Confirmed gateway is actually down (not network latency)
☐ Checked gateway logs for error messages
☐ Verified gateway health endpoint responds: curl gateway:8001/actuator/health
☐ Checked Redis connectivity (session state intact)
☐ Identified which gateway failed (admin/clinical/FHIR/general)

IMPACT ASSESSMENT:
☐ Active sessions will be lost (clients need to re-authenticate)
☐ In-flight requests will fail (clients should retry)
☐ Downtime window estimated (usually <5 minutes)
☐ Identified affected users/services

COMMUNICATION:
☐ Incident opened in tracking system
☐ Team notified in #operations channel
☐ Monitoring disabled (prevent noise during recovery)
☐ Point of contact assigned

READINESS:
☐ Have admin access to Kubernetes/Docker
☐ Have access to logs and metrics
☐ Know gateway service name (gateway-admin-service, etc.)
☐ Know which backend services this gateway connects to
```

---

## Quick Start: Single Gateway Failover

### Scenario: Gateway Clinical (port 8003) Unresponsive

**Time needed**: 2-5 minutes
**Downtime**: 1-2 minutes per client session

### Step 1: Confirm Gateway Failure

```bash
# Check health endpoint
curl -v http://localhost:8003/actuator/health
# Expected: HTTP 200 with {"status":"UP"}
# Actual: Connection timeout or HTTP 503

# Check logs for errors
docker compose logs clinical-gateway-service --tail 50 | tail -20

# Expected error patterns:
# - OutOfMemoryError: Java heap space
# - Connection refused (to backend)
# - JwtValidationException (broken token validation)

# Check if container is running
docker compose ps clinical-gateway-service
# Expected status: Up
# Actual status: Exited or paused
```

### Step 2: Stop Gateway Service

```bash
# Stop the failing gateway
docker compose stop clinical-gateway-service

# Verify it stopped
docker compose ps clinical-gateway-service
# Status should be "Exited (0)"
```

### Step 3: Clear Gateway State (Optional but Recommended)

```bash
# Clear Redis session data for this gateway (forces re-auth)
docker exec -it hdim-redis redis-cli

# Option A: Clear all sessions (nuclear option)
FLUSHALL

# Option B: Clear only this gateway's sessions (if namespaced)
DEL session:clinical-*
EVAL "return redis.call('del',KEYS[1])" 1 'session:clinical-*'

# Verify cleared
DBSIZE  # Should be much lower

# Exit redis-cli
EXIT
```

### Step 4: Restart Gateway Service

```bash
# Start fresh instance
docker compose up -d clinical-gateway-service

# Verify it started
docker compose logs clinical-gateway-service | grep -E "Started|UP|ERROR" | head -20

# Expected output:
# 2026-01-19 14:15:23 - Started GatewayServiceApplication
# 2026-01-19 14:15:24 - Server initialized with 10 threads
```

### Step 5: Verify Gateway Health

```bash
# Check health endpoint
for i in {1..10}; do
  curl -s http://localhost:8003/actuator/health | jq .
  sleep 2
done

# Once health = UP, gateway is ready

# Check if backend services are reachable
curl -v http://localhost:8003/api/v1/patients \
  -H "Authorization: Bearer <VALID_JWT>"

# Expected: 200 OK
# If 503: Backend service not responding
```

### Step 6: Notify Clients

```bash
# Post to Slack
curl -X POST https://hooks.slack.com/... -d '{
  "text": "✅ Clinical Gateway (8003) Failover Complete",
  "details": "Service restarted at 14:15 UTC. Clients must re-authenticate."
}'

# Inform users to:
# - Refresh browser/app
# - Sign in again (old sessions invalidated)
# - Expect normal latency within 2-3 minutes
```

---

## Full Gateway Failover (All Instances)

### Scenario: Complete Gateway Failure (All 4 gateways down)

**Time needed**: 10-15 minutes
**Impact**: Complete API outage (serious incident)

```bash
#!/bin/bash
# full-gateway-failover.sh

GATEWAYS=("gateway-general-service" "gateway-admin-service" "gateway-clinical-service" "gateway-fhir-service")
PORTS=(8001 8002 8003 8004)

echo "Starting full gateway failover..."
echo "WARNING: This will cause API outage during restart!"

# 1. Backup session state
echo "Backing up session state..."
docker exec -it hdim-redis redis-cli BGSAVE
sleep 10

# 2. Stop all gateways
echo "Stopping all gateways..."
for GATEWAY in "${GATEWAYS[@]}"; do
  echo "  Stopping $GATEWAY..."
  docker compose stop "$GATEWAY"
done

# 3. Wait for graceful shutdown
sleep 10

# 4. Clear stale state (optional)
echo "Clearing stale Redis state..."
docker exec -it hdim-redis redis-cli <<EOF
# Keep important state, clear sessions
DEL session:*
DBSIZE
EOF

# 5. Start all gateways in sequence (avoid thundering herd)
echo "Starting gateways in sequence..."
for GATEWAY in "${GATEWAYS[@]}"; do
  echo "  Starting $GATEWAY..."
  docker compose up -d "$GATEWAY"
  sleep 15  # Wait for service to be ready
done

# 6. Verify all are healthy
echo "Verifying gateway health..."
for PORT in "${PORTS[@]}"; do
  echo "  Checking port $PORT..."
  for i in {1..10}; do
    HEALTH=$(curl -s http://localhost:$PORT/actuator/health 2>/dev/null | jq -r '.status' 2>/dev/null)
    if [ "$HEALTH" == "UP" ]; then
      echo "    ✅ Port $PORT: UP"
      break
    else
      echo "    ⏳ Port $PORT: Not ready ($i/10)"
      sleep 5
    fi
  done
done

echo "✅ Full gateway failover complete!"
echo "Clients must re-authenticate."
```

---

## Handling Specific Failure Scenarios

### Scenario A: Memory Leak (Gradual Degradation)

**Symptoms**:
- Memory usage growing over hours
- Garbage collection taking longer
- Response times increasing
- Eventually: OutOfMemoryError and crash

**Response**:

```bash
# 1. Monitor memory trend
docker stats clinical-gateway-service --no-stream | grep -E "MEM|CONTAINER"

# 2. If approaching limit (>85% of -Xmx), restart before crash
docker compose restart clinical-gateway-service

# 3. Analyze heap dump to find leak
docker exec clinical-gateway-service jmap -dump:live,format=b,file=/tmp/heap.dump 1

# 4. Download heap dump and analyze locally
docker cp clinical-gateway-service:/tmp/heap.dump ./gateway-heap.dump

# 5. Use Eclipse MAT or jhat to find memory hog
jhat -J-Xmx4g gateway-heap.dump

# 6. Create ticket with findings for dev team to fix
```

### Scenario B: Circuit Breaker Open (Cascading Failure)

**Symptoms**:
- Backend service unreachable or slow
- Gateway circuit breaker opens
- All requests to that backend return 503
- Gateway still running but mostly failing

**Response**:

```bash
# 1. Check which backend is causing issue
docker compose logs clinical-gateway-service | grep "Circuit.*OPEN\|fallback\|timeout"

# 2. Identify the failing backend service
docker compose logs patient-service --tail 50 | head -20

# 3. Restart the struggling backend service
docker compose restart patient-service

# 4. Wait for it to stabilize
sleep 30

# 5. Monitor circuit breaker recovery
docker compose logs clinical-gateway-service | grep -E "Circuit.*HALF_OPEN|Circuit.*CLOSED"

# Once circuit closes, gateway recovers automatically
```

### Scenario C: JWT Validation Broken

**Symptoms**:
- All authentication requests failing
- Logs show JwtValidationException or signature mismatch
- Invalid token responses to clients

**Response**:

```bash
# 1. Check if JWT secret is loaded
docker exec clinical-gateway-service env | grep JWT_SECRET

# If empty:
# 2. Check if secret was provided correctly
docker compose logs clinical-gateway-service | grep "JWT_SECRET\|secret"

# 3. If secret is wrong, update environment
# Edit docker-compose.yml and set correct JWT_SECRET
vi docker-compose.yml

# 4. Restart gateway with correct secret
docker compose up -d clinical-gateway-service

# 5. Verify JWT validation works
curl -v http://localhost:8003/api/v1/patients \
  -H "Authorization: Bearer <VALID_JWT_TOKEN>"
# Should get 200 OK, not 401
```

---

## Monitoring During Failover

### Key Health Checks

```bash
#!/bin/bash
# Check gateway health

echo "=== Gateway Health Check ==="
echo "Time: $(date)"
echo ""

# 1. Service running?
echo "1. Service Status:"
docker compose ps clinical-gateway-service | tail -1

# 2. Port responding?
echo "2. Port Response:"
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://localhost:8003/actuator/health

# 3. Can validate JWT?
echo "3. JWT Validation:"
VALID_JWT="<get from auth service>"
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://localhost:8003/api/v1/patients \
  -H "Authorization: Bearer $VALID_JWT"

# 4. Redis session state?
echo "4. Redis Sessions:"
docker exec hdim-redis redis-cli DBSIZE

# 5. Memory/CPU?
echo "5. Resource Usage:"
docker stats --no-stream clinical-gateway-service | tail -1

# 6. Errors in logs?
echo "6. Recent Errors:"
docker compose logs clinical-gateway-service --since 5m | grep -i error | wc -l
```

---

## Rollback & Recovery

### Option 1: Revert to Previous Version

```bash
# 1. Check recent versions
docker image ls | grep gateway

# 2. Get previous image ID
PREVIOUS_IMAGE="gateway-clinical-service:v1.24.0"

# 3. Start previous version
docker compose up -d clinical-gateway-service
# Edit docker-compose.yml to specify PREVIOUS_IMAGE first

# 4. Verify it works
curl http://localhost:8003/actuator/health

# 5. If successful, it becomes the "current" version
# 6. Investigate what was wrong with new version
```

### Option 2: Restore Session State from Backup

```bash
# If Redis was cleared and you need sessions back:

# 1. Stop Redis
docker compose stop hdim-redis

# 2. Restore from backup (created during failover)
docker exec hdim-redis redis-cli BGREWRITEAOF
# Or restore RDB file manually

# 3. Restart
docker compose up -d hdim-redis

# 4. Verify sessions restored
docker exec hdim-redis redis-cli DBSIZE
```

---

## Prevention: High Availability Deployment

For production HA, consider:

### 1. Multiple Gateway Instances

```yaml
# docker-compose.yml
services:
  clinical-gateway-service-1:
    image: gateway-clinical:latest
    ports:
      - "8003:8003"
    environment:
      SERVER_PORT: 8003

  clinical-gateway-service-2:
    image: gateway-clinical:latest
    ports:
      - "8103:8003"  # Alternate port
    environment:
      SERVER_PORT: 8003
      INSTANCE_ID: 2

  # Load balancer routes to both
```

### 2. Health Check Configuration

```yaml
# docker-compose.yml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8003/actuator/health"]
  interval: 30s
  timeout: 5s
  retries: 2
  start_period: 60s
```

### 3. Automatic Restart on Failure

```yaml
# docker-compose.yml
restart_policy:
  condition: on-failure
  delay: 5s
  max_attempts: 3
```

### 4. Load Balancer Configuration

```nginx
# nginx.conf
upstream gateway_clinical {
    server localhost:8003;
    server localhost:8103;  # Backup instance
}

server {
    listen 443 ssl;
    server_name api.hdim.local;

    location /api/v1/ {
        proxy_pass http://gateway_clinical;
        proxy_connect_timeout 5s;
        proxy_send_timeout 10s;
        proxy_read_timeout 10s;
    }
}
```

---

## Post-Failover: Root Cause Analysis

After failover completes and service is stable:

```bash
#!/bin/bash
# 1. Collect logs
docker compose logs clinical-gateway-service > gateway-logs-$(date +%Y%m%d_%H%M%S).txt

# 2. Collect metrics
docker stats --no-stream --all > docker-stats-$(date +%Y%m%d_%H%M%S).txt

# 3. Check system resources
free -h > system-memory-$(date +%Y%m%d_%H%M%S).txt
df -h > disk-usage-$(date +%Y%m%d_%H%M%S).txt

# 4. Document timeline
cat > incident-report-$(date +%Y%m%d_%H%M%S).md <<EOF
# Incident Report

## Summary
Clinical Gateway failed at 14:10 UTC, recovered at 14:15 UTC (5 min downtime)

## Timeline
- 14:10: Gateway health check started failing
- 14:12: Manual investigation confirmed service down
- 14:13: Gateway service restarted
- 14:15: All health checks passing, service recovered
- 14:17: Sessions restored, clients re-authenticated

## Root Cause
[To be determined - check logs]

## Action Items
- [ ] Fix root cause in code
- [ ] Deploy patched version
- [ ] Monitor memory/performance
- [ ] Review circuit breaker thresholds
- [ ] Update alerting thresholds
EOF

# 5. Notify team
echo "Incident analysis files created. Review and create tickets."
```

---

## Related Runbooks

- **[Event Replay Procedures](EVENT_REPLAY_PROCEDURES.md)** - Recalculate measures
- **[Projection Rebuilding](PROJECTION_REBUILDING.md)** - Rebuild read models
- **[Event Store Maintenance](EVENT_STORE_MAINTENANCE.md)** - Manage event storage

---

## Support

**Gateway failover questions?**
- Post in #operations Slack channel
- Review service logs with `docker compose logs <service>`
- Check gateway port responsiveness with curl
- Contact platform on-call engineer

**Emergency**:
- Page operations engineer if failover doesn't resolve
- Have logs ready for analysis
- Know the failure timeline

---

_Last Updated: January 19, 2026_
_Version: 1.0_
_Status: Production Ready_
