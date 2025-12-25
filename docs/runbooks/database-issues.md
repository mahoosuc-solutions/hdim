# Runbook: Database Issues

**Severity:** Critical
**Response Time:** Immediate (< 15 min)
**Alert Names:** `PostgreSQLDown`, `HighDatabaseConnections`, `DatabaseReplicationLag`

## Symptoms

- Services reporting database connection errors
- Slow query responses
- Connection pool exhaustion
- Replication lag warnings

## Impact Assessment

Database issues affect all services. Priority order for recovery:
1. gateway-service (authentication)
2. patient-service (core data)
3. cql-engine-service (evaluations)
4. All other services

## Diagnosis

### 1. Check Database Status
```bash
# Kubernetes
kubectl exec -it statefulset/postgresql -n healthdata-prod -- \
  psql -U hdim -c "SELECT 1;"

# Docker
docker compose exec postgres psql -U hdim -c "SELECT 1;"
```

### 2. Check Connection Count
```bash
# Current connections
psql -U hdim -c "SELECT count(*) FROM pg_stat_activity;"

# Connections by state
psql -U hdim -c "SELECT state, count(*) FROM pg_stat_activity GROUP BY state;"

# Connections by application
psql -U hdim -c "SELECT application_name, count(*) FROM pg_stat_activity GROUP BY application_name;"
```

### 3. Check for Long-Running Queries
```sql
SELECT pid, now() - pg_stat_activity.query_start AS duration, query, state
FROM pg_stat_activity
WHERE (now() - pg_stat_activity.query_start) > interval '5 minutes'
ORDER BY duration DESC;
```

### 4. Check for Locks
```sql
SELECT blocked_locks.pid AS blocked_pid,
       blocking_locks.pid AS blocking_pid,
       blocked_activity.usename AS blocked_user,
       blocking_activity.usename AS blocking_user,
       blocked_activity.query AS blocked_statement,
       blocking_activity.query AS blocking_statement
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
JOIN pg_catalog.pg_locks blocking_locks ON blocking_locks.locktype = blocked_locks.locktype
JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted;
```

### 5. Check Disk Space
```bash
# PostgreSQL data directory
kubectl exec -it statefulset/postgresql -n healthdata-prod -- df -h /var/lib/postgresql/data

# Table sizes
psql -U hdim -c "SELECT relname, pg_size_pretty(pg_total_relation_size(relid))
FROM pg_catalog.pg_statio_user_tables ORDER BY pg_total_relation_size(relid) DESC LIMIT 10;"
```

## Mitigation Steps

### Connection Pool Exhaustion

**Step 1: Kill idle connections**
```sql
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE state = 'idle'
  AND query_start < now() - interval '10 minutes'
  AND pid <> pg_backend_pid();
```

**Step 2: Restart affected services** (to reset connection pools)
```bash
kubectl rollout restart deployment/patient-service -n healthdata-prod
kubectl rollout restart deployment/cql-engine-service -n healthdata-prod
```

### Long-Running Query Blocking

**Step 1: Identify and kill blocking query**
```sql
-- Get blocking PID from diagnosis step 4
SELECT pg_terminate_backend(<blocking_pid>);
```

### Database Unresponsive

**Step 1: Check if process is running**
```bash
kubectl exec -it statefulset/postgresql -n healthdata-prod -- pg_isready
```

**Step 2: Restart PostgreSQL (CAUTION)**
```bash
# Kubernetes
kubectl rollout restart statefulset/postgresql -n healthdata-prod

# Docker
docker compose restart postgres
```

**Step 3: Verify recovery**
```bash
psql -U hdim -c "SELECT 1;"
```

### Disk Space Critical

**Step 1: Vacuum and analyze**
```sql
VACUUM ANALYZE;
```

**Step 2: Truncate large audit tables (if safe)**
```sql
-- Only if data is backed up and not needed
TRUNCATE TABLE audit_log_archive;
```

**Step 3: Expand storage** (escalate to DevOps)

## Recovery Verification

1. Check all services can connect:
```bash
for svc in patient-service cql-engine-service care-gap-service; do
  kubectl exec -it deployment/$svc -n healthdata-prod -- \
    curl -s http://localhost:8080/actuator/health | jq .components.db.status
done
```

2. Verify connection count is normal (< 80% of max):
```sql
SELECT count(*) as current,
       setting::int as max,
       (count(*) * 100 / setting::int) as percentage
FROM pg_stat_activity, pg_settings
WHERE name = 'max_connections'
GROUP BY setting;
```

3. Check Grafana database dashboard for normal metrics

## Escalation

| Condition | Action |
|-----------|--------|
| Database won't start | Escalate to DBA immediately |
| Data corruption suspected | Escalate to DBA + backup team |
| Replication broken | Escalate to DBA |
| Storage full, cannot expand | Escalate to DevOps + DBA |

## Post-Incident

- [ ] Review connection pool settings
- [ ] Check for query optimization opportunities
- [ ] Verify backup completed successfully
- [ ] Document root cause
