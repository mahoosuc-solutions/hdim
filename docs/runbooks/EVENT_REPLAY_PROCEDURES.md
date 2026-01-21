# Event Replay Procedures Runbook

Operational guide for replaying events to recalculate measures, recover from data errors, or rebuild projections in HDIM event services.

---

## Overview

**Event Replay** allows you to reprocess all historical events from the event store to:
- Recalculate quality measures (logic changed)
- Recover from projection corruption
- Rebuild read models from scratch
- Correct data errors retroactively

**Key principle**: Event store is immutable. Replay regenerates projections/read models without modifying events.

---

## When to Replay Events

### ✅ Valid Reasons to Replay

**Scenario 1: Measure Logic Change**
- New HEDIS measure algorithm deployed
- Old measure calculations incorrect
- Need to recalculate for all historical patients

**Scenario 2: Projection Corruption**
- Read model out of sync with event store
- Data inconsistency detected
- Manual intervention corrupted projection

**Scenario 3: New Feature**
- Adding new projection type
- Need historical data in new format
- Bootstrapping new service

**Scenario 4: Bug Fix**
- Event handler had bug (now fixed)
- Projections were built incorrectly
- Need to rebuild with corrected logic

### ❌ Invalid Reasons

- To "undo" legitimate business operations (use compensation events instead)
- To modify historical data (immutable by design)
- To work around other operational issues (address root cause)

---

## Pre-Replay Checklist

Before replaying events, verify:

```
SAFETY CHECKS:
☐ Have backup of current projections
☐ Understand why replay is needed (document reason)
☐ Event handler code changes reviewed and tested
☐ Know expected outcome (what will change?)
☐ Identified time window for replay (all events or date range?)
☐ Calculated replay duration (how long will it take?)

PERMISSIONS:
☐ Have ops/database access
☐ Team lead approval obtained
☐ Change logged in incident system
☐ Stakeholders notified (teams depending on projections)

COMMUNICATION:
☐ Announce replay in #operations Slack channel
☐ Expected downtime window communicated
☐ Rollback plan understood
☐ Point of contact available during replay
```

---

## Quick Start: Replay All Events

### Scenario: Measure Calculation Logic Changed

**Time needed**: 30 minutes to 2 hours (depending on event volume)

### Step 1: Deploy Updated Event Handler

```bash
# Assuming measure logic fixed in code
./gradlew :modules:services:quality-measure-event-service:build
docker compose up -d quality-measure-event-service

# Verify service started
docker compose logs quality-measure-event-service | grep "Started"
```

### Step 2: Clear Old Projections

```bash
# Connect to database
docker exec -it hdim-postgres psql -U healthdata -d quality_db

# Backup old data (CRITICAL!)
CREATE TABLE quality_measures_backup AS SELECT * FROM quality_measures_projection;

# Clear projections (will be rebuilt by replay)
DELETE FROM quality_measures_projection;
DELETE FROM event_handler_state;  # Resets handler to start from beginning
COMMIT;

# Verify cleared
SELECT COUNT(*) FROM quality_measures_projection;  # Should return 0
```

### Step 3: Reset Event Consumer Offset

```bash
# Connect to Kafka
docker exec -it kafka-1 bash

# Reset consumer group to earliest offset
kafka-consumer-groups --bootstrap-server kafka-1:9092 \
  --group quality-measure-handler \
  --topic quality-measure.events \
  --reset-offsets \
  --to-earliest \
  --execute

# Verify reset
kafka-consumer-groups --bootstrap-server kafka-1:9092 \
  --group quality-measure-handler \
  --describe
```

### Step 4: Monitor Replay Progress

```bash
# Watch handler logs
docker compose logs -f quality-measure-event-service | grep -E "Processing|ERROR|lag"

# Monitor projection table growth
docker exec -it hdim-postgres psql -U healthdata -d quality_db \
  -c "SELECT COUNT(*) as projection_count, NOW() FROM quality_measures_projection;" \
  -c "every 10 seconds"

# Monitor event consumer lag (if Kafka metrics available)
docker exec -it kafka-1 kafka-consumer-groups \
  --bootstrap-server kafka-1:9092 \
  --group quality-measure-handler \
  --describe
```

### Step 5: Verify Replay Completed

```bash
# Check final projection count matches event store
docker exec -it hdim-postgres psql -U healthdata -d quality_db

-- Get event count
SELECT COUNT(*) as event_count FROM quality_measure_events;

-- Get projection count
SELECT COUNT(*) as projection_count FROM quality_measures_projection;

-- These should match (approximately)
-- Query both, compare numbers
```

### Step 6: Validate Data Accuracy

```bash
# Sample check: Verify specific measure calculation
SELECT
  measure_id,
  patient_id,
  score,
  updated_at
FROM quality_measures_projection
WHERE measure_id = 'hedis_diabetes_control'
ORDER BY updated_at DESC
LIMIT 5;

# Compare with old backup to see what changed
SELECT
  measure_id,
  patient_id,
  score as new_score
FROM quality_measures_projection
WHERE measure_id = 'hedis_diabetes_control'
LIMIT 5;

SELECT
  measure_id,
  patient_id,
  score as old_score
FROM quality_measures_backup
WHERE measure_id = 'hedis_diabetes_control'
LIMIT 5;
```

### Step 7: Cleanup

```bash
-- Drop backup once verified safe
DROP TABLE quality_measures_backup;

-- Check handler state (should be at latest offset)
SELECT * FROM event_handler_state WHERE handler_name = 'quality-measure-handler';
```

---

## Partial Replay: Date Range

**Use when**: Only recent events are affected, don't want to replay entire history

### Example: Replay Last 7 Days

```bash
# Calculate timestamp for 7 days ago
docker exec -it hdim-postgres psql -U healthdata -d quality_db

-- Get event timestamp from 7 days ago
SELECT DISTINCT created_at
FROM quality_measure_events
WHERE created_at > NOW() - INTERVAL '7 days'
ORDER BY created_at ASC
LIMIT 1;

-- Let's say it returns: 2026-01-12 10:00:00

-- Clear only recent projections (last 7 days)
DELETE FROM quality_measures_projection
WHERE updated_at > NOW() - INTERVAL '7 days';

-- Clear handler state to restart from 7 days ago
UPDATE event_handler_state
SET last_processed_event_timestamp = '2026-01-12 10:00:00'
WHERE handler_name = 'quality-measure-handler';
COMMIT;
```

Then follow Steps 3-7 above. Handler will only process events newer than specified timestamp.

---

## Troubleshooting

### Issue: Replay is Very Slow

**Symptoms**: Projection count not growing, handler lagging far behind

**Causes**:
- Event volume too high (millions of events)
- Handler processing power limited (needs scaling)
- Database I/O bottleneck

**Solutions**:
```bash
# Option 1: Check handler performance
docker compose logs quality-measure-event-service | grep -i "processing time"

# Option 2: Scale handler to multiple instances
docker compose scale quality-measure-event-service=3

# Option 3: Increase batch size in handler config
# application.yml: kafka.consumer.fetch-min-bytes: 51200

# Option 4: Monitor CPU/memory
docker stats quality-measure-event-service
```

### Issue: Handler Crashes During Replay

**Symptoms**: Handler logs show error, container exits

**Causes**:
- Event format changed (old events incompatible with new handler)
- Database constraint violation
- Out of memory
- Deadlock in handler logic

**Solution**:
```bash
# 1. Check logs for root cause
docker compose logs quality-measure-event-service --tail 100

# 2. If event format issue:
#    - Add event versioning to handler
#    - Handle both old and new event formats
#    - Redeploy with backward compatibility

# 3. If memory issue:
#    - Increase handler JVM memory
#    - docker-compose.yml: JAVA_OPTS: "-Xmx2g"
#    - docker compose up -d quality-measure-event-service

# 4. Restart handler
docker compose restart quality-measure-event-service
```

### Issue: Replay Stuck (No Progress)

**Symptoms**: Handler running but projection count not increasing

**Causes**:
- Consumer offset not reset
- Handler paused
- Kafka broker issue

**Solution**:
```bash
# 1. Check handler is actually consuming
docker exec -it kafka-1 kafka-consumer-groups --bootstrap-server kafka-1:9092 \
  --group quality-measure-handler --describe
# Look for "CURRENT-OFFSET" column - should be increasing

# 2. If offset not increasing, reset it
kafka-consumer-groups --bootstrap-server kafka-1:9092 \
  --group quality-measure-handler \
  --reset-offsets --to-earliest --execute

# 3. Restart handler
docker compose restart quality-measure-event-service

# 4. Monitor progress
docker compose logs -f quality-measure-event-service
```

---

## Rollback Plan

If replay produces incorrect results:

### Option 1: Restore from Backup (Fast)

```bash
-- Assumes you have backup from Step 2
docker exec -it hdim-postgres psql -U healthdata -d quality_db

-- Drop bad projections
DROP TABLE quality_measures_projection;

-- Restore from backup
ALTER TABLE quality_measures_backup RENAME TO quality_measures_projection;

-- Verify count matches before replay
SELECT COUNT(*) FROM quality_measures_projection;
```

**Downtime**: ~5 minutes
**Data loss**: None (restored to pre-replay state)

### Option 2: Revert Code Change (Slower)

```bash
# If handler code change caused problem:
git revert <commit-hash>
./gradlew :modules:services:quality-measure-event-service:build
docker compose up -d quality-measure-event-service

# Clear bad projections again
# Clear handler state
# Re-run replay with corrected logic
```

**Downtime**: ~30 minutes
**Data loss**: None

---

## Monitoring During Replay

### Key Metrics to Watch

| Metric | Location | What to Look For |
|--------|----------|------------------|
| **Event Processing Rate** | Service logs | Should be consistent (X events/sec) |
| **Projection Growth** | Database query | Steadily increasing to match events |
| **Handler Lag** | Kafka consumer group | Should decrease to 0 |
| **Error Rate** | Service logs | Should be 0 (no errors) |
| **Memory Usage** | docker stats | Should stay <80% of limit |
| **Database Connections** | pg_stat_statements | Should be stable |

### Monitoring Queries

```bash
# Monitor projection growth every 10 seconds
watch -n 10 "docker exec -it hdim-postgres psql -U healthdata -d quality_db \
  -c \"SELECT COUNT(*) FROM quality_measures_projection;\""

# Monitor handler lag
watch -n 10 "docker exec -it kafka-1 kafka-consumer-groups \
  --bootstrap-server kafka-1:9092 \
  --group quality-measure-handler --describe | grep quality-measure.events"

# Monitor errors
docker compose logs quality-measure-event-service -f | grep -i error
```

---

## Replay Scripts (Automated)

### Script: Full Replay

```bash
#!/bin/bash
# replay-quality-measures.sh

SERVICE="quality-measure-event-service"
HANDLER_GROUP="quality-measure-handler"
TOPIC="quality-measure.events"
TABLE="quality_measures_projection"

echo "Starting full event replay for $SERVICE..."

# Step 1: Backup
echo "Backing up $TABLE..."
docker exec -it hdim-postgres psql -U healthdata -d quality_db \
  -c "CREATE TABLE ${TABLE}_backup_$(date +%s) AS SELECT * FROM $TABLE;"

# Step 2: Clear projections
echo "Clearing projections..."
docker exec -it hdim-postgres psql -U healthdata -d quality_db \
  -c "DELETE FROM $TABLE; DELETE FROM event_handler_state;"

# Step 3: Reset consumer offset
echo "Resetting Kafka consumer offset..."
docker exec -it kafka-1 kafka-consumer-groups --bootstrap-server kafka-1:9092 \
  --group "$HANDLER_GROUP" --topic "$TOPIC" \
  --reset-offsets --to-earliest --execute

# Step 4: Restart handler
echo "Restarting service..."
docker compose restart "$SERVICE"

# Step 5: Monitor progress
echo "Monitoring replay progress..."
for i in {1..300}; do  # Monitor for up to 5 hours
  COUNT=$(docker exec -it hdim-postgres psql -U healthdata -d quality_db \
    -tc "SELECT COUNT(*) FROM $TABLE;")
  EVENTS=$(docker exec -it hdim-postgres psql -U healthdata -d quality_db \
    -tc "SELECT COUNT(*) FROM quality_measure_events;")
  LAG=$(docker exec -it kafka-1 kafka-consumer-groups --bootstrap-server kafka-1:9092 \
    --group "$HANDLER_GROUP" --describe | grep "$TOPIC" | awk '{print $5}')

  echo "[$i/300] Projections: $COUNT / Events: $EVENTS, Lag: $LAG"

  if [ "$LAG" -eq 0 ] || [ "$COUNT" -eq "$EVENTS" ]; then
    echo "✅ Replay complete!"
    break
  fi

  sleep 60
done

echo "Done."
```

**Usage**:
```bash
chmod +x replay-quality-measures.sh
./replay-quality-measures.sh
```

---

## Safety Features

### Built-in Safeguards

1. **Immutable Events**: Cannot modify event store, only projections
2. **Backup Required**: Always backup before clearing projections
3. **Idempotency**: Replaying same event twice produces same result
4. **Version Checking**: Handler validates event format before processing
5. **Offset Tracking**: Consumer group tracks position, can restart from there

### Manual Safeguards

- Always backup before clearing projections
- Test on staging first
- Have rollback plan ready
- Monitor during replay
- Verify results match expectations
- Communicate before/after

---

## Examples

### Example 1: HEDIS Measure Logic Fixed

**Problem**: HEDIS diabetes control calculation was wrong
**Solution**: Fix logic in handler, replay to recalculate

```bash
# 1. Code change merged, handler deployed with fix
docker compose up -d quality-measure-event-service

# 2. Backup and clear
psql -c "CREATE TABLE measures_backup AS SELECT * FROM quality_measures_projection;"
psql -c "DELETE FROM quality_measures_projection; DELETE FROM event_handler_state;"

# 3. Reset Kafka offset
kafka-consumer-groups --reset-offsets --to-earliest --execute

# 4. Monitor progress
docker logs -f quality-measure-event-service

# 5. Verify calculations now correct
SELECT * FROM quality_measures_projection WHERE measure_id = 'hedis_diabetes_control';
```

### Example 2: Projection Corrupted

**Problem**: Data in projection table doesn't match events
**Solution**: Rebuild projection from event store

```bash
# Same as above - clearing and replaying rebuilds from events
# Events are source of truth, projections are derived
```

---

## Related Runbooks

- **[Projection Rebuilding](PROJECTION_REBUILDING.md)** - Detailed projection recovery
- **[Event Store Maintenance](EVENT_STORE_MAINTENANCE.md)** - Long-term event storage
- **[Event Sourcing Architecture](../architecture/EVENT_SOURCING_ARCHITECTURE.md)** - How event sourcing works

---

## Support

**Questions about event replay?**
- Consult `docs/architecture/EVENT_SOURCING_ARCHITECTURE.md`
- Ask in #operations Slack channel
- Contact platform team on-call

**Emergency**:
- Page operations on-call engineer
- Have this runbook ready
- Know the root cause of replay need

---

_Last Updated: January 19, 2026_
_Version: 1.0_
_Status: Production Ready_
