# CQRS Integration Testing Guide

## Overview

This guide provides comprehensive instructions for validating the CQRS (Command Query Responsibility Segregation) event-driven architecture across all four projection services:

- **Patient Event Service** (Port 8110)
- **Care Gap Event Service** (Port 8111)
- **Quality Measure Event Service** (Port 8112)
- **Clinical Workflow Event Service** (Port 8113)

## Architecture Overview

```
Write-Side Services          Kafka Event Stream        Read-Side Projections
(Not yet integrated)    →    (patient.events, etc.)  →  (Denormalized Projections)
                                                           ↓
                                                    Query Endpoints (< 100ms SLA)
```

**Key Patterns:**
- **Event Sourcing**: Immutable event log as source of truth
- **Event Projection**: Kafka consumers transform events into denormalized read models
- **Eventual Consistency**: Projections update 100-500ms after event publication
- **Multi-Tenancy**: All queries filtered by `tenant_id`
- **Cache Compliance**: PHI cached with 5-minute TTL in Redis

---

## Phase 8.1: Verify Service Health

### Quick Health Check

```bash
# Check all 4 services are running
docker compose ps | grep event

# Expected output:
# healthdata-patient-event-service             Up
# healthdata-care-gap-event-service            Up
# healthdata-quality-measure-event-service     Up
# healthdata-clinical-workflow-event-service   Up
```

### Individual Service Health Endpoints

```bash
# Patient Event Service
curl http://localhost:8110/patient-event/actuator/health | jq .

# Care Gap Event Service
curl http://localhost:8111/care-gap-event/actuator/health | jq .

# Quality Measure Event Service
curl http://localhost:8112/quality-measure-event/actuator/health | jq .

# Clinical Workflow Event Service
curl http://localhost:8113/clinical-workflow-event/actuator/health | jq .
```

**Expected Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "kafka": {"status": "UP"},
    "redis": {"status": "UP"}
  }
}
```

---

## Phase 8.2: Verify Kafka Setup

### Check Kafka Topics

```bash
# List all Kafka topics
docker exec healthdata-kafka kafka-topics.sh --bootstrap-server kafka:9092 --list

# Expected output (topics auto-created on first use):
# patient.events
# care-gap.events
# quality-measure.events
# workflow.events
```

### Create Topics Manually (if needed)

```bash
# Create patient events topic
docker exec healthdata-kafka kafka-topics.sh \
  --create \
  --bootstrap-server kafka:9092 \
  --topic patient.events \
  --partitions 3 \
  --replication-factor 1 \
  --if-not-exists

# Create other topics similarly
docker exec healthdata-kafka kafka-topics.sh \
  --create \
  --bootstrap-server kafka:9092 \
  --topic care-gap.events \
  --partitions 3 \
  --replication-factor 1 \
  --if-not-exists

docker exec healthdata-kafka kafka-topics.sh \
  --create \
  --bootstrap-server kafka:9092 \
  --topic quality-measure.events \
  --partitions 3 \
  --replication-factor 1 \
  --if-not-exists

docker exec healthdata-kafka kafka-topics.sh \
  --create \
  --bootstrap-server kafka:9092 \
  --topic workflow.events \
  --partitions 3 \
  --replication-factor 1 \
  --if-not-exists
```

---

## Phase 8.3: Verify Database Setup

### Check Event Service Databases

```bash
# Connect to PostgreSQL
docker exec -it healthdata-postgres psql -U healthdata

# List all databases
\l

# Expected output should include:
# patient_event_db             | healthdata | UTF8
# care_gap_event_db            | healthdata | UTF8
# quality_event_db             | healthdata | UTF8
# clinical_workflow_event_db   | healthdata | UTF8
```

### Check Projection Tables

```bash
# Connect to patient event database
docker exec -it healthdata-postgres psql -U healthdata -d patient_event_db

# List tables
\dt

# Check patient_projections table structure
\d patient_projections

# Expected columns: id, tenant_id, patient_id, first_name, last_name, date_of_birth, gender, etc.
```

### Verify Multi-Tenant Isolation

```sql
-- Check for proper tenant filtering in queries
SELECT * FROM patient_projections WHERE tenant_id = 'tenant-1' LIMIT 5;

-- Verify indexes on tenant_id (critical for performance)
SELECT schemaname, tablename, indexname
FROM pg_indexes
WHERE tablename = 'patient_projections';

-- Expected: idx_patient_projections_tenant_id should exist
```

---

## Phase 8.4: Run Integration Tests

### Automated Test Suite

The project includes a comprehensive integration test script:

```bash
# From project root
cd backend
./scripts/test-cqrs-event-flow.sh
```

**What the script tests:**
1. ✅ Service health checks (all 4 services)
2. ✅ Kafka connectivity and topic access
3. ✅ Database connectivity (all 4 event databases)
4. ✅ Projection table existence
5. ✅ Consumer group registration
6. ✅ Event publishing to Kafka
7. ✅ Event projection (eventual consistency)
8. ✅ Multi-tenant isolation
9. ✅ Cache-Control headers (PHI protection)
10. ✅ Eventual consistency timing (100-500ms SLA)

### Java Integration Tests

```bash
# Run patient event service integration tests
./gradlew :modules:services:patient-event-service:test --tests "*CQRSEventFlowIntegrationTest"

# Run all service tests
./gradlew test
```

---

## Phase 8.5: Manual Event Flow Testing

### Publish Test Patient Event

```bash
# Create test event
PATIENT_ID="test-patient-$(date +%s)"
EVENT_JSON=$(cat <<EOF
{
  "eventId": "$(uuidgen)",
  "tenantId": "test-tenant-manual",
  "patientId": "$PATIENT_ID",
  "firstName": "Test",
  "lastName": "Patient",
  "dateOfBirth": "1980-01-15",
  "gender": "M",
  "eventTimestamp": "$(date -u +%Y-%m-%dT%H:%M:%S.000Z)",
  "eventType": "PATIENT_CREATED"
}
EOF
)

# Publish to Kafka
echo "$EVENT_JSON" | docker exec -i healthdata-kafka kafka-console-producer.sh \
  --broker-list kafka:9092 \
  --topic patient.events
```

### Verify Event Was Projected

```bash
# Wait for eventual consistency (100-500ms)
sleep 2

# Query the projection
curl -X GET \
  "http://localhost:8110/patient-event/api/v1/projections/patients?limit=100" \
  -H "X-Tenant-ID: test-tenant-manual" \
  -H "Content-Type: application/json" | jq .

# Expected: Response should include the test patient
```

### Monitor Projection Updates

```bash
# Watch logs in real-time
docker compose logs -f patient-event-service | grep -E "(received|projection|processed|updated)"

# In another terminal, publish events and observe real-time updates
```

---

## Phase 8.6: Validate Multi-Tenant Isolation

### Create Multi-Tenant Test Data

```bash
# SQL script to insert test data for multiple tenants
docker exec -it healthdata-postgres psql -U healthdata -d patient_event_db <<EOF
-- Insert patients for tenant 1
INSERT INTO patient_projections
  (id, tenant_id, patient_id, first_name, last_name, date_of_birth, gender, created_at)
VALUES
  (gen_random_uuid(), 'tenant-1', 'patient-1a', 'Alice', 'Tenant1', '1980-01-15', 'F', NOW()),
  (gen_random_uuid(), 'tenant-1', 'patient-1b', 'Bob', 'Tenant1', '1975-06-20', 'M', NOW());

-- Insert patients for tenant 2
INSERT INTO patient_projections
  (id, tenant_id, patient_id, first_name, last_name, date_of_birth, gender, created_at)
VALUES
  (gen_random_uuid(), 'tenant-2', 'patient-2a', 'Charlie', 'Tenant2', '1985-03-10', 'M', NOW()),
  (gen_random_uuid(), 'tenant-2', 'patient-2b', 'Diana', 'Tenant2', '1990-09-25', 'F', NOW());

-- Verify data
SELECT tenant_id, COUNT(*) as patient_count FROM patient_projections GROUP BY tenant_id;
EOF
```

### Query Each Tenant Separately

```bash
# Query Tenant 1
curl -X GET \
  "http://localhost:8110/patient-event/api/v1/projections/patients" \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/json" | jq '.data[] | {tenantId, patientId, firstName}'

# Expected: Only patients for tenant-1

# Query Tenant 2
curl -X GET \
  "http://localhost:8110/patient-event/api/v1/projections/patients" \
  -H "X-Tenant-ID: tenant-2" \
  -H "Content-Type: application/json" | jq '.data[] | {tenantId, patientId, firstName}'

# Expected: Only patients for tenant-2
```

### Verify Isolation at Database Level

```bash
# Attempt to query without tenant filtering (should be prevented by application code)
docker exec -it healthdata-postgres psql -U healthdata -d patient_event_db \
  -c "SELECT COUNT(*) FROM patient_projections WHERE tenant_id = 'tenant-1';"

# Verify indexes for performance
docker exec -it healthdata-postgres psql -U healthdata -d patient_event_db \
  -c "SELECT * FROM pg_stat_user_indexes WHERE relname = 'patient_projections';"
```

---

## Phase 8.7: Cache Behavior Validation

### Verify Cache Headers on PHI Endpoints

```bash
# Query with verbose headers
curl -v -X GET \
  "http://localhost:8110/patient-event/api/v1/projections/patients" \
  -H "X-Tenant-ID: test-tenant" \
  2>&1 | grep -E "(Cache-Control|Pragma|Expires|Set-Cookie)"

# Expected headers:
# Cache-Control: no-store, no-cache, must-revalidate
# Pragma: no-cache
```

### Monitor Redis Cache Usage

```bash
# Connect to Redis CLI
docker exec -it healthdata-redis redis-cli

# Check cache stats
> INFO stats

# View cache entries
> KEYS "patient_projections::*"

# Check TTL on cache entries (should be <= 5 minutes = 300 seconds)
> TTL "patient_projections::tenant-1"

# Exit
> EXIT
```

---

## Phase 8.8: Performance Benchmarking

### Query Response Time Measurement

```bash
# Measure query response time with 100+ records
time curl -X GET \
  "http://localhost:8110/patient-event/api/v1/projections/patients?limit=100" \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/json" | jq '.' > /dev/null

# Expected: real < 100ms (CQRS denormalized read model advantage)
```

### Load Testing with Apache Bench

```bash
# Run 1000 requests with 10 concurrent connections
ab -n 1000 -c 10 -H "X-Tenant-ID: tenant-1" \
  "http://localhost:8110/patient-event/api/v1/projections/patients?limit=100"

# Expected metrics:
# - Mean response time: < 100ms
# - 95th percentile: < 200ms
# - 99th percentile: < 500ms
# - Requests per second: > 100 req/sec
```

---

## Phase 8.9: Eventual Consistency Timing

### Test Event-to-Projection Latency

```bash
#!/bin/bash
# Measure time from event publication to projection visibility

TENANT_ID="timing-test-$(date +%s)"
PATIENT_ID="patient-timing-$(date +%s)"

# Start timestamp
START_TIME=$(date +%s%3N)

# Publish event
EVENT_JSON=$(cat <<EOF
{
  "eventId": "$(uuidgen)",
  "tenantId": "$TENANT_ID",
  "patientId": "$PATIENT_ID",
  "firstName": "Timing",
  "lastName": "Test",
  "dateOfBirth": "1980-01-15",
  "gender": "M",
  "eventTimestamp": "$(date -u +%Y-%m-%dT%H:%M:%S.000Z)",
  "eventType": "PATIENT_CREATED"
}
EOF
)

echo "$EVENT_JSON" | docker exec -i healthdata-kafka kafka-console-producer.sh \
  --broker-list kafka:9092 \
  --topic patient.events

# Poll for projection
MAX_ATTEMPTS=50  # 5 seconds max
ATTEMPT=0

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
  RESPONSE=$(curl -s -H "X-Tenant-ID: $TENANT_ID" \
    "http://localhost:8110/patient-event/api/v1/projections/patients")

  if echo "$RESPONSE" | grep -q "$PATIENT_ID"; then
    END_TIME=$(date +%s%3N)
    LATENCY=$((END_TIME - START_TIME))
    echo "✓ Event projected in ${LATENCY}ms"

    if [ $LATENCY -lt 500 ]; then
      echo "✓ Within target SLA (100-500ms)"
    else
      echo "⚠ Exceeded SLA target (${LATENCY}ms > 500ms)"
    fi
    exit 0
  fi

  sleep 0.1
  ((ATTEMPT++))
done

echo "✗ Event not projected within 5 second timeout"
exit 1
```

---

## Phase 8.10: Monitoring and Observability

### View Service Logs

```bash
# View patient event service logs
docker compose logs -f patient-event-service

# View care gap event service logs
docker compose logs -f care-gap-event-service

# View quality measure event service logs
docker compose logs -f quality-measure-event-service

# View clinical workflow event service logs
docker compose logs -f clinical-workflow-event-service

# Follow all services simultaneously
docker compose logs -f patient-event-service care-gap-event-service quality-measure-event-service clinical-workflow-event-service
```

### Monitor Kafka Consumer Lag

```bash
# Check consumer group lag
docker exec healthdata-kafka kafka-consumer-groups.sh \
  --bootstrap-server kafka:9092 \
  --group patient-event-service \
  --describe

# Expected output shows:
# TOPIC      PARTITION CURRENT-OFFSET LOG-END-OFFSET LAG
# patient... 0         5              5              0    (no lag = ideal)
```

### Database Query Monitoring

```bash
# Connect to database
docker exec -it healthdata-postgres psql -U healthdata -d patient_event_db

# Check active queries
SELECT pid, query, state FROM pg_stat_activity WHERE state != 'idle';

# Check table statistics
SELECT schemaname, tablename, n_live_tup, n_dead_tup
FROM pg_stat_user_tables ORDER BY n_live_tup DESC;

# Vacuum and analyze for performance
VACUUM ANALYZE patient_projections;
```

---

## Phase 8.11: Troubleshooting

### Common Issues and Solutions

#### Issue: Services Not Starting

```bash
# Check service logs
docker compose logs patient-event-service

# Common causes:
# 1. Database connection refused
docker compose logs postgres

# 2. Kafka not ready
docker compose logs kafka

# 3. Redis not ready
docker compose logs redis

# Solution: Restart all services
docker compose down
docker compose up -d --build
```

#### Issue: Events Not Being Projected

```bash
# Check if Kafka has messages
docker exec healthdata-kafka kafka-console-consumer.sh \
  --bootstrap-server kafka:9092 \
  --topic patient.events \
  --from-beginning \
  --max-messages 10

# Check consumer group lag
docker exec healthdata-kafka kafka-consumer-groups.sh \
  --bootstrap-server kafka:9092 \
  --group patient-event-service \
  --describe

# Check service logs for errors
docker compose logs patient-event-service | grep -i error

# Check database connectivity
docker exec -it healthdata-postgres psql -U healthdata -d patient_event_db -c "SELECT 1"
```

#### Issue: Queries Taking > 100ms

```bash
# Check for missing indexes
docker exec -it healthdata-postgres psql -U healthdata -d patient_event_db \
  -c "SELECT * FROM pg_stat_user_indexes WHERE relname = 'patient_projections';"

# If indexes missing, create them
docker exec -it healthdata-postgres psql -U healthdata -d patient_event_db <<EOF
CREATE INDEX idx_patient_projections_tenant_id ON patient_projections(tenant_id);
CREATE INDEX idx_patient_projections_patient_id ON patient_projections(patient_id);
ANALYZE patient_projections;
EOF

# Verify query plan
EXPLAIN ANALYZE SELECT * FROM patient_projections WHERE tenant_id = 'tenant-1' LIMIT 10;
```

---

## Success Criteria (Phase 8 Complete)

- [x] All 4 services running and healthy
- [x] Kafka topics created and accessible
- [x] Event databases created with proper schema
- [x] Projection tables contain denormalized data
- [x] Multi-tenant isolation enforced (queries filtered by tenant_id)
- [x] Cache headers present on PHI endpoints
- [x] Eventual consistency timing < 500ms
- [x] Query response time < 100ms
- [x] Integration tests passing
- [x] Kafka consumer groups registered
- [x] No silent failures or errors in logs

---

## Next Phase (9): Performance Validation

After integration testing is complete:

1. **Load Testing**: 1000+ concurrent users
2. **Stress Testing**: System limits and recovery
3. **Cache Hit Rate**: Monitor Redis effectiveness
4. **Connection Pooling**: Verify HikariCP configuration
5. **Query Optimization**: Review slow query logs

---

## Documentation References

- `CQRS_ARCHITECTURE_OVERVIEW.md` - High-level CQRS pattern
- `DISTRIBUTED_TRACING_GUIDE.md` - OpenTelemetry tracing
- `DATABASE_MIGRATION_RUNBOOK.md` - Database schema management
- `GATEWAY_TRUST_ARCHITECTURE.md` - Authentication flow

---

*Last Updated: January 20, 2026*
*Status: Phase 8 - Integration Testing (In Progress)*
