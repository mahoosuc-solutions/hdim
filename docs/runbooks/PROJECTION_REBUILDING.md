# Projection Rebuilding Runbook

Operational guide for rebuilding projections (read models) from scratch when corruption, data loss, or service migration requires complete reconstruction from the event store.

---

## Overview

**Projection Rebuilding** allows you to:
- Recover from projection corruption (data doesn't match event store)
- Migrate projections to new schema/format
- Bootstrap new projection types
- Clean slate after data quality issues
- Test projection logic in production-like environment

**Key principle**: Event store is the source of truth. Projections are derived from events and can be safely rebuilt.

---

## When to Rebuild Projections

### ✅ Valid Reasons to Rebuild

**Scenario 1: Projection Corruption**
- Data in projection doesn't match event store
- Queries return inconsistent results
- Manual data edits corrupted state
- Database recovery from backup

**Scenario 2: Schema Migration**
- Adding new columns to projection
- Changing data types
- Denormalizing data differently
- Introducing new indexes

**Scenario 3: New Projection Type**
- Adding new read model for different query pattern
- Supporting new UI feature
- Enabling analytics on historical data
- Creating specialized search index (Elasticsearch)

**Scenario 4: Bug Fix**
- Projection logic had bug (now fixed)
- Projections built incorrectly
- Need to recalculate from events

**Scenario 5: Service Migration**
- Moving projection to new service
- Changing database (PostgreSQL → MongoDB)
- Consolidating multiple projections
- Testing new projection implementation

### ❌ Invalid Reasons

- To "undo" business data (use compensation events instead)
- To work around other operational issues (address root cause)
- Routine maintenance (use event replay instead)

---

## Projection Rebuild vs Event Replay

**When to use which?**

| Scenario | Use Event Replay | Use Projection Rebuild |
|----------|-----------------|----------------------|
| Measure logic changed | ✅ | ❌ |
| Add new projection type | ❌ | ✅ |
| Projection schema changed | ❌ | ✅ |
| Data corruption detected | ✅ Optional | ✅ Preferred |
| Bulk fix needed | ✅ | ❌ |
| Handler logic bugfix | ✅ | ❌ |

**Key difference**:
- **Event Replay**: Re-runs event handler logic on existing events (fixes calculation bugs)
- **Projection Rebuild**: Drops projection, creates fresh schema, repopulates from events (fixes structural issues)

---

## Pre-Rebuild Checklist

Before rebuilding projections, verify:

```
SAFETY CHECKS:
☐ Have backup of current projection (full table export)
☐ Understand why rebuild is needed (document reason)
☐ Projection schema finalized (no mid-rebuild changes)
☐ Event handler code tested and reviewed
☐ Know expected outcome (what will change?)
☐ Identified projections to rebuild (all or specific?)
☐ Calculated rebuild duration (how long will it take?)

PERMISSIONS:
☐ Have ops/database access
☐ Have application restart permissions
☐ Team lead approval obtained
☐ Change logged in incident system
☐ Stakeholders notified (teams depending on projections)

COMMUNICATION:
☐ Announce rebuild in #operations Slack channel
☐ Expected downtime window communicated
☐ Rollback plan understood
☐ Point of contact available during rebuild

TESTING:
☐ Projection schema tested locally
☐ Event handler tested with sample events
☐ Row count verification queries prepared
☐ Data quality sample queries prepared
```

---

## Quick Start: Rebuild Single Projection

### Scenario: Rebuild Care Gap Projections After Schema Change

**Time needed**: 15 minutes to 2 hours (depending on event volume)

### Step 1: Backup Current Projection

```bash
# Connect to database
docker exec -it hdim-postgres psql -U healthdata -d caregap_db

# Create timestamped backup
CREATE TABLE care_gaps_projection_backup_$(date +%Y%m%d_%H%M%S) AS
  SELECT * FROM care_gaps_projection;

# Verify backup
SELECT COUNT(*) as backup_count FROM care_gaps_projection_backup_20260119_143022;

COMMIT;
```

### Step 2: Create New Projection Schema

```bash
# Option A: Apply Liquibase migration with new schema
./gradlew :modules:services:care-gap-event-service:bootRun

# Liquibase will create/alter projection table according to changesets

# Option B: Manually create schema if not using Liquibase
docker exec -it hdim-postgres psql -U healthdata -d caregap_db

-- Drop old table
DROP TABLE care_gaps_projection;

-- Create new schema
CREATE TABLE care_gaps_projection (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    care_gap_id VARCHAR(100) NOT NULL,
    patient_id VARCHAR(100) NOT NULL,
    gap_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'OPEN',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    -- NEW COLUMN
    risk_score DECIMAL(3,2),
    -- NEW COLUMN
    intervention_type VARCHAR(100)
);

-- Indexes for performance
CREATE INDEX idx_care_gaps_tenant_id ON care_gaps_projection(tenant_id);
CREATE INDEX idx_care_gaps_patient_id ON care_gaps_projection(patient_id);
CREATE INDEX idx_care_gaps_status ON care_gaps_projection(status);

COMMIT;
```

### Step 3: Clear Event Handler State

```bash
# Reset handler to start from beginning
docker exec -it hdim-postgres psql -U healthdata -d caregap_db

-- Option A: Reset to very beginning
DELETE FROM event_handler_state
WHERE handler_name = 'care-gap-projection-handler';

-- Option B: Reset to specific timestamp (if only rebuilding recent events)
UPDATE event_handler_state
SET last_processed_event_timestamp = '2026-01-10 00:00:00'
WHERE handler_name = 'care-gap-projection-handler';

COMMIT;
```

### Step 4: Reset Kafka Consumer Offset

```bash
# Connect to Kafka
docker exec -it kafka-1 bash

# Reset consumer group to earliest
kafka-consumer-groups --bootstrap-server kafka-1:9092 \
  --group care-gap-projection-handler \
  --topic care-gap.events \
  --reset-offsets \
  --to-earliest \
  --execute

# Verify reset
kafka-consumer-groups --bootstrap-server kafka-1:9092 \
  --group care-gap-projection-handler \
  --describe
# Should show LAG=0 (will increase as events process)
```

### Step 5: Restart Event Handler Service

```bash
# Restart the service
docker compose restart care-gap-event-service

# Verify it started
docker compose logs care-gap-event-service | grep -E "Started|ERROR" | head -20

# Watch for projection building
docker compose logs -f care-gap-event-service | grep -E "Processing|Projection|ERROR"
```

### Step 6: Monitor Rebuild Progress

```bash
# Terminal 1: Watch projection growth
watch -n 5 "docker exec -it hdim-postgres psql -U healthdata -d caregap_db \
  -tc \"SELECT COUNT(*) FROM care_gaps_projection;\""

# Terminal 2: Watch handler lag
watch -n 5 "docker exec -it kafka-1 kafka-consumer-groups \
  --bootstrap-server kafka-1:9092 \
  --group care-gap-projection-handler \
  --describe | grep care-gap.events"

# Terminal 3: Watch for errors
docker compose logs -f care-gap-event-service | grep -i error
```

**What to look for**:
- Projection count increasing steadily
- Handler LAG decreasing (approaching 0)
- No ERROR messages in logs
- Memory usage stable

### Step 7: Verify Rebuild Complete

```bash
docker exec -it hdim-postgres psql -U healthdata -d caregap_db

-- Check event count
SELECT COUNT(*) as event_count FROM care_gap_events;

-- Check projection count
SELECT COUNT(*) as projection_count FROM care_gaps_projection;

-- Check for NULL values (might indicate incomplete projections)
SELECT COUNT(*) as incomplete_projections
FROM care_gaps_projection
WHERE status IS NULL;

-- These counts should be related (approximately match after rebuild)
```

### Step 8: Validate Data Quality

```bash
docker exec -it hdim-postgres psql -U healthdata -d caregap_db

-- Check for expected care gap types
SELECT gap_type, COUNT(*)
FROM care_gaps_projection
GROUP BY gap_type;

-- Check status distribution
SELECT status, COUNT(*)
FROM care_gaps_projection
GROUP BY status;

-- Compare with backup (sample check)
SELECT COUNT(*) as new_count FROM care_gaps_projection WHERE tenant_id = 'TENANT001';
SELECT COUNT(*) as old_count FROM care_gaps_projection_backup_20260119_143022 WHERE tenant_id = 'TENANT001';

-- If counts match, rebuild successful
```

### Step 9: Cleanup

```bash
docker exec -it hdim-postgres psql -U healthdata -d caregap_db

-- Drop old backup table once verified
DROP TABLE care_gaps_projection_backup_20260119_143022;

-- Check handler state is updated
SELECT * FROM event_handler_state
WHERE handler_name = 'care-gap-projection-handler';

COMMIT;
```

---

## Rebuilding Multiple Projections

### Scenario: Rebuild All Projections for a Service

When service has multiple projections needing rebuild:

```bash
#!/bin/bash
# rebuild-all-projections.sh

SERVICE="care-gap-event-service"
DATABASE="caregap_db"
PROJECTIONS=("care_gaps_projection" "care_gap_metrics_projection" "care_gap_alerts_projection")

echo "Backing up all projections for $SERVICE..."
for PROJ in "${PROJECTIONS[@]}"; do
  echo "  Backing up $PROJ..."
  docker exec -it hdim-postgres psql -U healthdata -d "$DATABASE" \
    -c "CREATE TABLE ${PROJ}_backup_$(date +%s) AS SELECT * FROM $PROJ;"
done

echo "Clearing all projections..."
for PROJ in "${PROJECTIONS[@]}"; do
  echo "  Clearing $PROJ..."
  docker exec -it hdim-postgres psql -U healthdata -d "$DATABASE" \
    -c "DELETE FROM $PROJ;"
done

echo "Resetting event handler state..."
docker exec -it hdim-postgres psql -U healthdata -d "$DATABASE" \
  -c "DELETE FROM event_handler_state WHERE handler_name LIKE '$SERVICE%';"

echo "Resetting Kafka offsets..."
for TOPIC in "care-gap.events"; do
  docker exec -it kafka-1 kafka-consumer-groups --bootstrap-server kafka-1:9092 \
    --group "care-gap-projection-handler" \
    --topic "$TOPIC" \
    --reset-offsets \
    --to-earliest \
    --execute
done

echo "Restarting service..."
docker compose restart "$SERVICE"

echo "Monitoring rebuild..."
for i in {1..300}; do
  COUNTS=""
  for PROJ in "${PROJECTIONS[@]}"; do
    COUNT=$(docker exec -it hdim-postgres psql -U healthdata -d "$DATABASE" \
      -tc "SELECT COUNT(*) FROM $PROJ;")
    COUNTS="$COUNTS $PROJ=$COUNT"
  done
  echo "[$i/300]$COUNTS"
  sleep 10
done

echo "Rebuild complete. Verify data quality."
```

---

## Troubleshooting

### Issue: Rebuild Never Starts

**Symptoms**: Projection table empty after hours, handler logs show nothing

**Causes**:
- Kafka offset not reset
- Handler not consuming events
- Event handler crashed

**Solution**:

```bash
# 1. Check Kafka consumer status
docker exec -it kafka-1 kafka-consumer-groups --bootstrap-server kafka-1:9092 \
  --group care-gap-projection-handler --describe
# Look at CURRENT-OFFSET column - should be increasing

# 2. Check handler logs for errors
docker compose logs care-gap-event-service --tail 50 | grep -i error

# 3. If offset not increasing, reset it
docker exec -it kafka-1 kafka-consumer-groups --bootstrap-server kafka-1:9092 \
  --group care-gap-projection-handler \
  --reset-offsets --to-earliest --execute

# 4. Restart handler
docker compose restart care-gap-event-service

# 5. Monitor
docker compose logs -f care-gap-event-service
```

### Issue: Rebuild is Slow

**Symptoms**: Projection count increasing very slowly, hours to complete

**Causes**:
- Event volume very high (millions of events)
- Handler processing power limited
- Database I/O bottleneck
- Kafka batch size too small

**Solution**:

```bash
# Option 1: Check handler performance
docker compose logs care-gap-event-service | grep "Processing time"

# Option 2: Scale handler to multiple instances (if supported)
docker compose scale care-gap-event-service=3

# Option 3: Increase Kafka batch size in application.yml
spring:
  kafka:
    consumer:
      max-poll-records: 1000  # Default is 500
      fetch-min-bytes: 51200  # Default is 1
      fetch-max-wait-ms: 500  # Wait up to 500ms for batch

# Option 4: Monitor resource usage
docker stats care-gap-event-service

# Option 5: If CPU is bottleneck, add thread pool
spring:
  kafka:
    listener:
      concurrency: 5  # Process 5 messages in parallel
```

### Issue: Handler Crashes During Rebuild

**Symptoms**: Handler logs show exception, container exits

**Causes**:
- Event format incompatibility with new projection schema
- Database constraint violation (unique, not null, etc.)
- Out of memory
- Logic error in projection builder

**Solution**:

```bash
# 1. Check error logs
docker compose logs care-gap-event-service --tail 100 | grep -A 5 ERROR

# 2. If schema mismatch:
#    - Review migration file for correct column types
#    - Verify handler code matches new schema
#    - Test with sample events locally

# 3. If constraint violation:
#    - Check for duplicate keys
#    - Verify NOT NULL constraints are met
#    - Review projection builder logic

# 4. If memory error:
#    - Increase handler memory
#    - Update docker-compose.yml:
#      environment:
#        JAVA_OPTS: "-Xmx2g"

# 5. Fix, rebuild image, restart
docker compose restart care-gap-event-service
```

### Issue: Projection Row Count Doesn't Match Event Count

**Symptoms**: Projection has 50,000 rows, but 100,000 events in event store

**Causes**:
- Events filtered during projection (intentional or bug)
- Events for multiple aggregates in one table (duplicates on purpose)
- Partial rebuild (only recent events)
- Projection logic groups/aggregates events

**Solution**:

```bash
# Verify intentional filtering
docker exec -it hdim-postgres psql -U healthdata -d caregap_db

-- Check if projection filters events
SELECT COUNT(DISTINCT event_id) FROM care_gaps_projection;
-- If less than total events, some are filtered

-- Check event distribution
SELECT event_type, COUNT(*) FROM care_gap_events GROUP BY event_type;
SELECT COUNT(DISTINCT event_type) FROM care_gaps_projection;

-- If some event types missing, that might be expected
-- Example: CareGapClosedEvent doesn't create projection row

-- Query projection logic in code:
-- See: CareGapProjectionHandler.on(CareGapCreatedEvent)
-- Some events intentionally don't add rows
```

---

## Data Quality Validation

### Post-Rebuild Verification

```bash
# 1. Row count check
PROJECTION_COUNT=$(docker exec -it hdim-postgres psql -U healthdata -d caregap_db \
  -tc "SELECT COUNT(*) FROM care_gaps_projection;")
echo "Projection rows: $PROJECTION_COUNT"

# 2. Completeness check (no NULL in required fields)
NULL_COUNT=$(docker exec -it hdim-postgres psql -U healthdata -d caregap_db \
  -tc "SELECT COUNT(*) FROM care_gaps_projection WHERE status IS NULL;")
echo "Incomplete rows (NULL status): $NULL_COUNT"
if [ $NULL_COUNT -gt 0 ]; then
  echo "WARNING: Found incomplete projections"
fi

# 3. Tenant distribution check
docker exec -it hdim-postgres psql -U healthdata -d caregap_db \
  -c "SELECT tenant_id, COUNT(*) FROM care_gaps_projection GROUP BY tenant_id ORDER BY COUNT(*) DESC;"

# 4. Status distribution check
docker exec -it hdim-postgres psql -U healthdata -d caregap_db \
  -c "SELECT status, COUNT(*) FROM care_gaps_projection GROUP BY status;"

# 5. Timestamp reasonableness check
docker exec -it hdim-postgres psql -U healthdata -d caregap_db \
  -c "SELECT MIN(created_at) as oldest, MAX(created_at) as newest FROM care_gaps_projection;"

# 6. Index verification
docker exec -it hdim-postgres psql -U healthdata -d caregap_db \
  -c "\d care_gaps_projection"
```

---

## Rollback Plan

If rebuild produces incorrect data:

### Option 1: Restore from Backup (Fast - 5 min)

```bash
docker exec -it hdim-postgres psql -U healthdata -d caregap_db

-- Drop bad projection
DROP TABLE care_gaps_projection;

-- Restore from backup
ALTER TABLE care_gaps_projection_backup_20260119_143022
RENAME TO care_gaps_projection;

-- Verify
SELECT COUNT(*) FROM care_gaps_projection;

COMMIT;
```

**Downtime**: ~5 minutes
**Data loss**: None (restored to pre-rebuild state)

### Option 2: Selective Row Restore (Targeted - 10 min)

```bash
-- If only some rows are wrong, restore selectively
INSERT INTO care_gaps_projection (id, tenant_id, care_gap_id, ...)
SELECT id, tenant_id, care_gap_id, ...
FROM care_gaps_projection_backup_20260119_143022
WHERE tenant_id = 'TENANT_WITH_BAD_DATA';
```

### Option 3: Rebuild Again (Slowest - depends on volume)

If backup is also bad or too old:

```bash
# Follow rebuild procedure again, but be more careful
# - Verify handler code
# - Test with small subset of events first
# - Run data quality checks after each step
```

---

## Monitoring During Rebuild

### Key Metrics to Track

| Metric | What to Look For | Tool |
|--------|------------------|------|
| **Projection Growth** | Steadily increasing | `watch` + SQL count query |
| **Handler Lag** | Decreasing to 0 | Kafka consumer-groups |
| **Error Rate** | Should be 0 | Service logs |
| **Memory Usage** | Should stay <80% | `docker stats` |
| **CPU Usage** | Moderate (30-70%) | `docker stats` |
| **Processing Rate** | X rows/sec (consistent) | Service logs + SQL |

### Monitoring Script

```bash
#!/bin/bash
# monitor-projection-rebuild.sh

SERVICE="care-gap-event-service"
DATABASE="caregap_db"
PROJECTION="care_gaps_projection"
HANDLER_GROUP="care-gap-projection-handler"
TOPIC="care-gap.events"

echo "=== Projection Rebuild Monitoring ==="
echo "Start time: $(date)"

for i in {1..300}; do
  clear
  echo "=== Rebuild Progress [$i/300] ==="
  echo "Timestamp: $(date)"
  echo ""

  # Projection count
  PROJ_COUNT=$(docker exec -it hdim-postgres psql -U healthdata -d "$DATABASE" \
    -tc "SELECT COUNT(*) FROM $PROJECTION;")
  echo "Projection rows: $PROJ_COUNT"

  # Event count (for reference)
  EVENT_COUNT=$(docker exec -it hdim-postgres psql -U healthdata -d "$DATABASE" \
    -tc "SELECT COUNT(*) FROM care_gap_events;")
  echo "Total events: $EVENT_COUNT"

  # Handler lag
  LAG=$(docker exec -it kafka-1 kafka-consumer-groups --bootstrap-server kafka-1:9092 \
    --group "$HANDLER_GROUP" --describe 2>/dev/null | grep "$TOPIC" | awk '{print $5}')
  echo "Handler lag: $LAG"

  # Error count in logs
  ERROR_COUNT=$(docker compose logs "$SERVICE" --since 5m 2>/dev/null | grep -i error | wc -l)
  echo "Recent errors: $ERROR_COUNT"

  # Memory/CPU
  STATS=$(docker stats --no-stream "$SERVICE" 2>/dev/null | tail -1)
  echo "Stats: $STATS"

  echo ""
  if [ "$LAG" == "0" ] || [ "$PROJ_COUNT" == "$EVENT_COUNT" ]; then
    echo "✅ Rebuild complete!"
    break
  fi

  sleep 10
done

echo "End time: $(date)"
```

---

## Related Runbooks

- **[Event Replay Procedures](EVENT_REPLAY_PROCEDURES.md)** - Re-running event handlers for logic changes
- **[Event Store Maintenance](EVENT_STORE_MAINTENANCE.md)** - Long-term event storage management
- **[Event Sourcing Architecture](../architecture/diagrams/event-sourcing-dataflow.md)** - How event sourcing works

---

## Support

**Questions about projection rebuilding?**
- Consult `docs/architecture/diagrams/event-sourcing-dataflow.md`
- Review relevant projection handler in service code
- Ask in #operations Slack channel
- Contact platform team on-call

**Emergency**:
- Page operations on-call engineer
- Have this runbook ready
- Know the projection that needs rebuilding
- Know the reason for rebuild

---

_Last Updated: January 19, 2026_
_Version: 1.0_
_Status: Production Ready_
