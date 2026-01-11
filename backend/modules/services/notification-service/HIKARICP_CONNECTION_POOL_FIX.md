# Notification Service - HikariCP Connection Pool Fix

**Date:** 2026-01-11
**Issue:** Database connection pool exhaustion - all connections being closed, causing 20+ second health check delays
**Status:** 🔧 IN PROGRESS

---

## Problem Summary

The notification-service is experiencing critical database connection pool issues:

1. **Closed Connections**: All 5 pool connections repeatedly failing validation with "This connection has been closed"
2. **Slow Health Checks**: Health endpoint taking 13-21 seconds to respond (should be <1 second)
3. **Frequent Occurrence**: Happening every 5-10 minutes, affecting service reliability

### Root Cause

**HikariCP Configuration Mismatch with PostgreSQL/Docker Networking:**

- **Current maxLifetime**: 1800000ms (30 minutes) - Too long
- **Network Reality**: PostgreSQL or Docker is closing idle connections after ~5 minutes
- **Result**: HikariCP tries to use stale connections that were closed by PostgreSQL

---

## Error Logs

```
2026-01-11T15:59:45.241Z WARN 1 --- [notification-service] [nio-8107-exec-1]
com.zaxxer.hikari.pool.PoolBase : HikariPool-1 - Failed to validate connection
org.postgresql.jdbc.PgConnection@181b7c02 (This connection has been closed.).
Possibly consider using a shorter maxLifetime value.

2026-01-11T15:55:54.870Z WARN 1 --- [notification-service] [nio-8107-exec-3]
o.s.b.a.health.HealthEndpointSupport : Health contributor
org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator (db) took 21272ms to respond
```

**Pattern:** Service cycles through all 5 connections in pool, all fail, must create new connections.

---

## Fixes Applied

### 1. application.yml - HikariCP Tuning

**File:** `/backend/modules/services/notification-service/src/main/resources/application.yml`
**Lines:** 14-26

**BEFORE:**
```yaml
hikari:
  maximum-pool-size: 10
  minimum-idle: 5
  connection-timeout: 30000
  idle-timeout: 600000        # 10 minutes
  max-lifetime: 1800000       # 30 minutes
```

**AFTER:**
```yaml
hikari:
  maximum-pool-size: 10
  minimum-idle: 5
  connection-timeout: 20000      # Reduced from 30s to 20s
  idle-timeout: 300000           # Reduced from 10min to 5min (matches network timeout)
  max-lifetime: 300000           # CRITICAL FIX: 5min (matches PostgreSQL/Docker TCP timeout)
  keepalive-time: 240000         # NEW: 4min (proactive keepalive before timeout)
  leak-detection-threshold: 60000  # NEW: 60s leak detection
  validation-timeout: 5000       # NEW: 5s validation timeout (fail fast)
```

### Key Changes:

1. **maxLifetime reduced to 5 minutes** - Matches observed network timeout
2. **keepalive-time added (4 minutes)** - Proactively tests connections before timeout
3. **leak-detection-threshold (60 seconds)** - Detects connection leaks early
4. **validation-timeout (5 seconds)** - Fails fast on dead connections

---

## How It Works

### HikariCP Connection Lifecycle (After Fix)

```
0:00 → Connection created
4:00 → Keepalive test (connection still alive?)
5:00 → Connection recycled (maxLifetime reached)
```

**Old Behavior (30min maxLifetime):**
```
0:00 → Connection created
5:00 → Network closes TCP connection (idle timeout)
10:00+ → HikariCP tries to use closed connection → FAILS
```

**New Behavior (5min maxLifetime + 4min keepalive):**
```
0:00 → Connection created
4:00 → Keepalive test → SUCCESS (connection refreshed)
5:00 → Connection recycled before network timeout
```

---

## PostgreSQL Connection Settings (Reference)

PostgreSQL default timeouts (can vary by installation):
- `tcp_keepalives_idle`: 7200s (2 hours) - Not relevant here
- `tcp_keepalives_interval`: 75s
- `idle_in_transaction_session_timeout`: 0 (disabled)
- **Docker TCP keepalive**: ~5 minutes (varies by kernel/Docker version)

**Conclusion:** The 5-minute timeout is likely from Docker's TCP layer, not PostgreSQL settings.

---

## Testing

### 1. Verify Configuration
```bash
docker exec -it healthdata-notification-service sh -c \
  "cat application.yml | grep -A10 hikari"
```

### 2. Monitor Connection Pool
```bash
# Watch for connection warnings
docker logs -f healthdata-notification-service | grep -i "hikari\|connection"
```

### 3. Health Check Performance
```bash
# Should respond in <1 second
time curl -s http://localhost:8107/notification/actuator/health | jq .
```

### 4. Trigger Load
```bash
# Send test notifications to exercise connection pool
for i in {1..20}; do
  curl -X POST http://localhost:8107/notification/api/notifications \
    -H "X-Tenant-ID: acme-health" \
    -H "X-Auth-User-Id: $(uuidgen)" \
    -H "Content-Type: application/json" \
    -d '{"type":"email","subject":"Test","body":"Test notification"}'
  sleep 2
done
```

---

## Best Practices Applied

1. **Match Network Reality** - maxLifetime set to observed timeout (5 minutes)
2. **Proactive Keepalive** - Test connections at 4 minutes (before 5-minute timeout)
3. **Fail Fast** - Short validation timeout prevents long waits
4. **Leak Detection** - Catch resource leaks early
5. **Connection Recycling** - Refresh connections before they become stale

---

## Related Configuration

The notification-service also requires these fixes (separate issues):

1. **OTLP Configuration** - Missing protocol and IPv4 preference (see OTLP_CONFIGURATION.md)
2. **Database Schema Management** - Using `ddl-auto: create` instead of Liquibase (CRITICAL!)
3. **Hardcoded Credentials** - Password in application.yml (should use env vars)

---

## Implementation Status

- ✅ Root cause identified
- ✅ Fix designed
- ⏳ Awaiting deployment (requires service restart)
- ⏳ Testing pending

---

**References:**
- HikariCP Best Practices: https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing
- PostgreSQL Connection Pooling: https://www.postgresql.org/docs/current/runtime-config-connection.html
- Docker TCP Keepalive: https://docs.docker.com/engine/reference/commandline/dockerd/#daemon-configuration-file
