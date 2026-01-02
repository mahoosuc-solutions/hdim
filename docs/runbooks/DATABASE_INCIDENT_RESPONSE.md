# Database Incident Response Runbook

**Service**: HDIM Platform Databases
**Version**: 1.0.0
**Date**: 2025-12-24
**On-Call**: DBA Team / Platform Engineering

---

## Quick Reference

### Database Components

| Database | Purpose | Port | Default DB |
|----------|---------|------|------------|
| PostgreSQL (Primary) | Application data, FHIR resources | 5432 | healthdata |
| PostgreSQL (Replica) | Read replicas | 5433 | healthdata |
| Redis | Caching, sessions | 6379 | 0 |

### Critical Metrics

| Metric | Normal | Warning | Critical |
|--------|--------|---------|----------|
| Connection Pool Usage | <70% | 70-90% | >90% |
| Replication Lag | <1s | 1-5s | >5s |
| Query P95 | <100ms | 100-500ms | >500ms |
| Disk Usage | <70% | 70-85% | >85% |
| Active Connections | <80% max | 80-95% max | >95% max |

### Quick Commands

```bash
# Check PostgreSQL health
kubectl exec -it postgres-0 -n healthdata-prod -- pg_isready

# Check connections
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "SELECT count(*) FROM pg_stat_activity;"

# Check replication
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "SELECT * FROM pg_stat_replication;"

# Check Redis health
kubectl exec -it redis-0 -n healthdata-prod -- redis-cli ping
```

---

## Common Incidents

### 1. Connection Pool Exhaustion

**Symptoms**:
- "Connection pool exhausted" errors
- Application timeouts
- Slow response times

**Diagnosis**:
```bash
# Check current connections
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "
    SELECT count(*), state, wait_event_type
    FROM pg_stat_activity
    WHERE datname = 'healthdata'
    GROUP BY state, wait_event_type;"

# Find connection sources
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "
    SELECT client_addr, usename, count(*)
    FROM pg_stat_activity
    WHERE datname = 'healthdata'
    GROUP BY client_addr, usename
    ORDER BY count DESC;"

# Check for idle connections
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "
    SELECT pid, usename, state, state_change, query
    FROM pg_stat_activity
    WHERE datname = 'healthdata' AND state = 'idle'
    AND state_change < now() - interval '10 minutes';"
```

**Resolution**:
```bash
# 1. Kill idle connections older than 10 minutes
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "
    SELECT pg_terminate_backend(pid)
    FROM pg_stat_activity
    WHERE datname = 'healthdata'
    AND state = 'idle'
    AND state_change < now() - interval '10 minutes';"

# 2. Increase max_connections (temporary)
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U postgres -c "ALTER SYSTEM SET max_connections = 200;"
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U postgres -c "SELECT pg_reload_conf();"

# 3. Increase HikariCP pool size in application
kubectl set env deployment/fhir-service \
  SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=30 \
  -n healthdata-prod
```

### 2. Slow Queries

**Symptoms**:
- High latency on specific operations
- CPU spikes
- Lock contention

**Diagnosis**:
```bash
# Find slow queries
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "
    SELECT query, calls, mean_time, total_time
    FROM pg_stat_statements
    ORDER BY mean_time DESC
    LIMIT 10;"

# Find currently running slow queries
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "
    SELECT pid, now() - query_start AS duration, state, query
    FROM pg_stat_activity
    WHERE state = 'active' AND now() - query_start > interval '5 seconds'
    ORDER BY duration DESC;"

# Check for lock contention
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "
    SELECT blocked.pid AS blocked_pid,
           blocked.query AS blocked_query,
           blocking.pid AS blocking_pid,
           blocking.query AS blocking_query
    FROM pg_stat_activity blocked
    JOIN pg_locks bl ON blocked.pid = bl.pid
    JOIN pg_locks blck ON bl.locktype = blck.locktype
      AND bl.database = blck.database
      AND bl.relation = blck.relation
      AND bl.page = blck.page
      AND bl.tuple = blck.tuple
      AND bl.pid != blck.pid
    JOIN pg_stat_activity blocking ON blck.pid = blocking.pid
    WHERE NOT bl.granted;"
```

**Resolution**:
```bash
# 1. Kill blocking query (if safe)
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "SELECT pg_terminate_backend(<pid>);"

# 2. Add missing index (example)
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "
    CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_patient_identifier
    ON patient(identifier);"

# 3. Analyze table statistics
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "ANALYZE patient;"

# 4. Run vacuum
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "VACUUM ANALYZE patient;"
```

### 3. Replication Lag

**Symptoms**:
- Stale reads from replicas
- Replication lag metrics elevated
- Replica falling behind

**Diagnosis**:
```bash
# Check replication status on primary
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "
    SELECT client_addr, state, sent_lsn, write_lsn, flush_lsn, replay_lsn,
           (sent_lsn - replay_lsn) AS lag_bytes,
           replay_lag
    FROM pg_stat_replication;"

# Check WAL position
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "SELECT pg_current_wal_lsn();"

# Check on replica
kubectl exec -it postgres-replica-0 -n healthdata-prod -- \
  psql -U healthdata -c "SELECT pg_last_wal_receive_lsn(), pg_last_wal_replay_lsn();"

# Check replica recovery status
kubectl exec -it postgres-replica-0 -n healthdata-prod -- \
  psql -U healthdata -c "SELECT pg_is_in_recovery(), pg_last_xact_replay_timestamp();"
```

**Resolution**:
```bash
# 1. Check network between primary and replica
kubectl exec -it postgres-replica-0 -n healthdata-prod -- \
  ping postgres-0.postgres-headless

# 2. Increase wal_keep_size if needed
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U postgres -c "ALTER SYSTEM SET wal_keep_size = '2GB';"
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U postgres -c "SELECT pg_reload_conf();"

# 3. If replica is too far behind, rebuild it
# WARNING: This causes downtime for the replica
kubectl delete pod postgres-replica-0 -n healthdata-prod
# Wait for PVC to clear and pod to rebuild
```

### 4. Disk Space Full

**Symptoms**:
- Write errors
- "No space left on device" errors
- Service crashes

**Diagnosis**:
```bash
# Check disk usage
kubectl exec -it postgres-0 -n healthdata-prod -- df -h /var/lib/postgresql/data

# Check database sizes
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "
    SELECT datname, pg_size_pretty(pg_database_size(datname))
    FROM pg_database ORDER BY pg_database_size(datname) DESC;"

# Check table sizes
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "
    SELECT schemaname, tablename,
           pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size
    FROM pg_tables
    WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
    ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
    LIMIT 20;"

# Check WAL size
kubectl exec -it postgres-0 -n healthdata-prod -- \
  du -sh /var/lib/postgresql/data/pg_wal
```

**Resolution**:
```bash
# 1. Clean up old WAL files
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U postgres -c "CHECKPOINT;"

# 2. Remove old backups/dumps
kubectl exec -it postgres-0 -n healthdata-prod -- \
  find /var/lib/postgresql/backup -mtime +7 -delete

# 3. VACUUM to reclaim space
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "VACUUM FULL ANALYZE;"

# 4. Expand PVC (if supported by storage class)
kubectl patch pvc postgres-data-postgres-0 -n healthdata-prod \
  -p '{"spec":{"resources":{"requests":{"storage":"200Gi"}}}}'

# 5. Archive old data
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "
    DELETE FROM audit_log
    WHERE created_at < now() - interval '90 days';"
```

### 5. Database Corruption

**Symptoms**:
- "invalid page" errors
- Checksum failures
- Inconsistent query results

**Diagnosis**:
```bash
# Check for corruption
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "SELECT pg_catalog.pg_indexes_size('patient');"

# Run amcheck on indexes
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "
    CREATE EXTENSION IF NOT EXISTS amcheck;
    SELECT bt_index_check(c.oid)
    FROM pg_index i
    JOIN pg_class c ON c.oid = i.indexrelid
    WHERE c.relname = 'patient_pkey';"

# Check for data directory issues
kubectl exec -it postgres-0 -n healthdata-prod -- \
  pg_controldata /var/lib/postgresql/data | grep -i "state\|checkpoint"
```

**Resolution**:
```bash
# 1. If index corruption, reindex
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "REINDEX TABLE patient;"

# 2. If table corruption, try pg_dump/restore
kubectl exec -it postgres-0 -n healthdata-prod -- \
  pg_dump -U healthdata -t patient healthdata > /tmp/patient_backup.sql

# 3. If severe, restore from backup
# See Backup/Restore section below
```

### 6. Redis Failure

**Symptoms**:
- Cache misses
- Session losses
- Increased database load

**Diagnosis**:
```bash
# Check Redis health
kubectl exec -it redis-0 -n healthdata-prod -- redis-cli ping

# Check memory usage
kubectl exec -it redis-0 -n healthdata-prod -- redis-cli INFO memory

# Check connected clients
kubectl exec -it redis-0 -n healthdata-prod -- redis-cli CLIENT LIST

# Check slow log
kubectl exec -it redis-0 -n healthdata-prod -- redis-cli SLOWLOG GET 10
```

**Resolution**:
```bash
# 1. Restart Redis
kubectl rollout restart statefulset/redis -n healthdata-prod

# 2. If memory issue, flush and restart
kubectl exec -it redis-0 -n healthdata-prod -- redis-cli FLUSHALL
kubectl rollout restart statefulset/redis -n healthdata-prod

# 3. Increase memory limit
kubectl set resources statefulset/redis \
  --limits=memory=4Gi \
  -n healthdata-prod
```

---

## Backup and Recovery

### Backup Status Check

```bash
# Check latest backup
kubectl exec -it postgres-0 -n healthdata-prod -- \
  ls -la /var/lib/postgresql/backup/

# Check backup job status
kubectl get jobs -n healthdata-prod | grep backup

# View backup logs
kubectl logs -n healthdata-prod job/pg-backup-daily
```

### Point-in-Time Recovery (PITR)

```bash
# 1. Stop application traffic
kubectl scale deployment --all --replicas=0 -n healthdata-prod

# 2. Stop PostgreSQL
kubectl delete pod postgres-0 -n healthdata-prod

# 3. Restore base backup
kubectl exec -it postgres-restore-0 -n healthdata-prod -- \
  pg_basebackup -D /var/lib/postgresql/data-restore \
    -Fp -Xs -P -R -h backup-storage -U replicator

# 4. Configure recovery target
cat > /tmp/recovery.conf << EOF
restore_command = 'cp /var/lib/postgresql/wal_archive/%f %p'
recovery_target_time = '2025-12-24 10:00:00'
recovery_target_action = 'promote'
EOF

# 5. Start recovery
kubectl apply -f kubernetes/postgres-recovery.yaml

# 6. Monitor recovery
kubectl logs -f postgres-restore-0 -n healthdata-prod
```

### Emergency Restore

```bash
# For complete database loss, restore from backup
# 1. Get latest backup from S3
aws s3 cp s3://hdim-backups/postgres/latest.dump /tmp/

# 2. Create new PostgreSQL instance
kubectl apply -f kubernetes/postgres-emergency.yaml

# 3. Restore
kubectl exec -it postgres-emergency-0 -n healthdata-prod -- \
  pg_restore -U healthdata -d healthdata /tmp/latest.dump

# 4. Verify data
kubectl exec -it postgres-emergency-0 -n healthdata-prod -- \
  psql -U healthdata -c "SELECT count(*) FROM patient;"

# 5. Switch traffic
kubectl patch service postgres-headless -n healthdata-prod \
  -p '{"spec":{"selector":{"app":"postgres-emergency"}}}'
```

---

## Maintenance Procedures

### Vacuum and Analyze

```bash
# Regular maintenance (run weekly)
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "VACUUM ANALYZE;"

# Full vacuum for specific table (locks table!)
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "VACUUM FULL ANALYZE patient;"
```

### Index Maintenance

```bash
# Check index usage
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "
    SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read
    FROM pg_stat_user_indexes
    ORDER BY idx_scan DESC;"

# Find unused indexes
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "
    SELECT schemaname, tablename, indexname, pg_size_pretty(pg_relation_size(indexrelid))
    FROM pg_stat_user_indexes
    WHERE idx_scan = 0
    ORDER BY pg_relation_size(indexrelid) DESC;"

# Reindex online
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "REINDEX TABLE CONCURRENTLY patient;"
```

### Statistics Update

```bash
# Update statistics for query planner
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "
    ANALYZE VERBOSE patient;
    ANALYZE VERBOSE observation;
    ANALYZE VERBOSE condition;"
```

---

## Monitoring Queries

### Health Dashboard Queries

```sql
-- Active connections by state
SELECT state, count(*)
FROM pg_stat_activity
WHERE datname = 'healthdata'
GROUP BY state;

-- Top queries by total time
SELECT query, calls, total_time / 1000 as total_seconds, mean_time
FROM pg_stat_statements
ORDER BY total_time DESC
LIMIT 10;

-- Table bloat estimate
SELECT schemaname, tablename,
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as total_size,
       n_live_tup, n_dead_tup,
       ROUND(n_dead_tup * 100.0 / NULLIF(n_live_tup + n_dead_tup, 0), 1) as dead_pct
FROM pg_stat_user_tables
ORDER BY n_dead_tup DESC
LIMIT 10;

-- Replication lag
SELECT client_addr,
       state,
       pg_wal_lsn_diff(sent_lsn, replay_lsn) as lag_bytes,
       replay_lag
FROM pg_stat_replication;

-- Cache hit ratio
SELECT
  sum(heap_blks_hit) / NULLIF(sum(heap_blks_hit) + sum(heap_blks_read), 0) as ratio
FROM pg_statio_user_tables;
```

---

## Escalation

| Severity | Response Time | Escalation |
|----------|---------------|------------|
| **Database Down** | Immediate | DBA + Platform Lead + On-call |
| **Data Corruption** | < 15 min | DBA + Platform Lead + Security |
| **Performance Critical** | < 30 min | DBA + Platform |
| **Replication Issues** | < 1 hour | DBA |
| **Disk Space Warning** | < 4 hours | DBA |

---

**Runbook Version**: 1.0.0
**Last Updated**: 2025-12-24
**Next Review**: 2026-03-24
**Owner**: DBA Team / Platform Engineering
