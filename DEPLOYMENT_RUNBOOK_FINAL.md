# Production Deployment Runbook
**Event-Driven Health Assessment Platform**
**Version:** 1.0
**Last Updated:** November 25, 2025

---

## Pre-Deployment Checklist

### Environment Validation
- [ ] PostgreSQL 16+ installed and running
- [ ] Java 21+ installed
- [ ] Docker and Docker Compose installed
- [ ] Redis installed (optional, for session caching)
- [ ] SSL certificates obtained
- [ ] DNS configured
- [ ] Firewall rules configured
- [ ] Monitoring tools configured (Prometheus, Grafana)
- [ ] Backup strategy validated

### Configuration Files
- [ ] Production database credentials secured
- [ ] JWT secrets generated (strong, unique)
- [ ] FHIR server endpoint configured
- [ ] CORS origins whitelisted
- [ ] Rate limiting configured
- [ ] Log aggregation configured

---

## Database Migration Order

### Phase 1: Event Processing Service (Foundation)

**Service:** Event Processing Service
**Database:** `event_processing_db`
**Port:** 8089

```bash
# Step 1: Create database
psql -U postgres -c "CREATE DATABASE event_processing_db;"

# Step 2: Run migrations
cd backend/modules/services/event-processing-service
./gradlew liquibase:update

# Step 3: Verify tables
psql -U postgres -d event_processing_db -c "\dt"

# Expected tables:
# - events
# - event_subscriptions
# - dead_letter_queue
# - databasechangelog
# - databasechangeloglock
```

**Validation:**
```bash
# Check event store indexes
psql -U postgres -d event_processing_db << 'EOF'
SELECT schemaname, tablename, indexname
FROM pg_indexes
WHERE tablename IN ('events', 'event_subscriptions', 'dead_letter_queue')
ORDER BY tablename, indexname;
EOF

# Expected: 7+ indexes on events table
```

---

### Phase 2: FHIR Service (Write Model)

**Service:** FHIR Service
**Database:** `fhir_db`
**Port:** 8082

```bash
# Step 1: Create database
psql -U postgres -c "CREATE DATABASE fhir_db;"

# Step 2: Run migrations
cd backend/modules/services/fhir-service
./gradlew liquibase:update

# Step 3: Verify FHIR resource tables
psql -U postgres -d fhir_db -c "\dt"

# Expected tables:
# - patients
# - observations
# - conditions
# - medication_requests
# - encounters
# - procedures
# - allergy_intolerances
# - immunizations
```

**Validation:**
```bash
# Check GIN indexes on JSONB columns
psql -U postgres -d fhir_db << 'EOF'
SELECT
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE indexdef LIKE '%USING gin%'
ORDER BY tablename;
EOF

# Expected: 8 GIN indexes (one per resource table)
```

---

### Phase 3: Patient Service (Demographics)

**Service:** Patient Service
**Database:** `patient_db`
**Port:** 8084

```bash
# Step 1: Create database
psql -U postgres -c "CREATE DATABASE patient_db;"

# Step 2: Run migrations
cd backend/modules/services/patient-service
./gradlew liquibase:update

# Expected tables:
# - patient_demographics
# - patient_insurance
# - patient_risk_scores
```

**Validation:**
```bash
# Check tenant_id + MRN unique constraint
psql -U postgres -d patient_db << 'EOF'
SELECT conname, contype, pg_get_constraintdef(oid)
FROM pg_constraint
WHERE conrelid = 'patient_demographics'::regclass;
EOF

# Expected: UNIQUE (tenant_id, mrn) WHERE mrn IS NOT NULL
```

---

### Phase 4: CQL Engine Service (Calculation Engine)

**Service:** CQL Engine Service
**Database:** `cql_engine_db`
**Port:** 8081

```bash
# Step 1: Create database
psql -U postgres -c "CREATE DATABASE cql_engine_db;"

# Step 2: Run migrations
cd backend/modules/services/cql-engine-service
./gradlew liquibase:update

# Expected tables:
# - cql_libraries
# - cql_evaluations
# - value_sets
```

**Validation:**
```bash
# Verify JSONB conversion
psql -U postgres -d cql_engine_db << 'EOF'
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'cql_evaluations'
  AND column_name IN ('result', 'context_data', 'compiled_elm');
EOF

# Expected: All should be 'jsonb'
```

---

### Phase 5: Quality Measure Service (Read Model)

**Service:** Quality Measure Service
**Database:** `quality_measure_db`
**Port:** 8087

```bash
# Step 1: Create database
psql -U postgres -c "CREATE DATABASE quality_measure_db;"

# Step 2: Run migrations
cd backend/modules/services/quality-measure-service
./gradlew liquibase:update

# Expected tables:
# - quality_measure_results
# - saved_reports
# - custom_measures
# - mental_health_assessments
# - care_gaps
# - risk_assessments
# - health_scores
# - health_score_history
```

**Validation:**
```bash
# Check all GIN indexes
psql -U postgres -d quality_measure_db << 'EOF'
SELECT indexname, tablename
FROM pg_indexes
WHERE indexdef LIKE '%USING gin%'
ORDER BY tablename;
EOF

# Expected: 5 GIN indexes
# - risk_assessments: risk_factors, predicted_outcomes, recommendations
# - quality_measure_results: cql_result
# - custom_measures: value_sets
```

---

### Phase 6: Care Gap Service

**Service:** Care Gap Service
**Database:** `care_gap_db`
**Port:** 8086

```bash
# Step 1: Create database
psql -U postgres -c "CREATE DATABASE care_gap_db;"

# Step 2: Run migrations
cd backend/modules/services/care-gap-service
./gradlew liquibase:update

# Expected tables:
# - care_gaps
# - care_gap_recommendations
# - care_gap_closures
```

---

### Phase 7: Analytics Service

**Service:** Analytics Service
**Database:** `analytics_db`
**Port:** 8088

```bash
# Step 1: Create database
psql -U postgres -c "CREATE DATABASE analytics_db;"

# Step 2: Run migrations
cd backend/modules/services/analytics-service
./gradlew liquibase:update

# Expected tables:
# - analytics_metrics
# - analytics_reports
# - star_ratings
```

---

### Phase 8: Consent Service (HIPAA Compliance)

**Service:** Consent Service
**Database:** `consent_db`
**Port:** 8085

```bash
# Step 1: Create database
psql -U postgres -c "CREATE DATABASE consent_db;"

# Step 2: Run migrations
cd backend/modules/services/consent-service
./gradlew liquibase:update

# Expected tables:
# - consents
# - consent_policies
# - consent_history
```

---

## Service Startup Order

### 1. Start Infrastructure Services

```bash
# PostgreSQL (should already be running)
sudo systemctl status postgresql

# Redis (optional, for session caching)
sudo systemctl start redis
sudo systemctl status redis
```

---

### 2. Start Core Services

**Order is critical for proper dependency resolution.**

```bash
# 1. Event Processing Service (8089)
cd backend/modules/services/event-processing-service
./gradlew bootRun

# Wait for startup (check logs for "Started EventProcessingServiceApplication")

# 2. FHIR Service (8082)
cd backend/modules/services/fhir-service
./gradlew bootRun

# 3. Patient Service (8084)
cd backend/modules/services/patient-service
./gradlew bootRun

# 4. CQL Engine Service (8081)
cd backend/modules/services/cql-engine-service
./gradlew bootRun

# 5. Quality Measure Service (8087)
cd backend/modules/services/quality-measure-service
./gradlew bootRun

# 6. Care Gap Service (8086)
cd backend/modules/services/care-gap-service
./gradlew bootRun

# 7. Analytics Service (8088)
cd backend/modules/services/analytics-service
./gradlew bootRun

# 8. Consent Service (8085)
cd backend/modules/services/consent-service
./gradlew bootRun

# 9. Gateway Service (9000) - LAST
cd backend/modules/services/gateway-service
./gradlew bootRun
```

---

### 3. Health Check Validation

```bash
# Check all services are healthy
#!/bin/bash

SERVICES=(
  "Gateway:9000"
  "CQL Engine:8081"
  "FHIR:8082"
  "Patient:8084"
  "Consent:8085"
  "Care Gap:8086"
  "Quality Measure:8087"
  "Analytics:8088"
  "Event Processing:8089"
)

for service in "${SERVICES[@]}"; do
  name="${service%%:*}"
  port="${service##*:}"

  echo -n "Checking $name ($port)... "

  if curl -s -f "http://localhost:$port/actuator/health" > /dev/null; then
    echo "✓ UP"
  else
    echo "✗ DOWN"
  fi
done
```

---

## Docker Deployment (Alternative)

### Using Docker Compose

```bash
# Build all services
cd /home/webemo-aaron/projects/healthdata-in-motion
docker-compose build

# Start all services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f

# Expected output:
# gateway-service        | Started GatewayServiceApplication
# cql-engine-service     | Started CqlEngineServiceApplication
# quality-measure-service| Started QualityMeasureServiceApplication
# fhir-service          | Started FhirServiceApplication
# patient-service       | Started PatientServiceApplication
# consent-service       | Started ConsentServiceApplication
# care-gap-service      | Started CareGapServiceApplication
# analytics-service     | Started AnalyticsServiceApplication
# event-processing-service | Started EventProcessingServiceApplication
# postgres              | database system is ready to accept connections
# redis                 | Ready to accept connections
```

---

## Post-Deployment Validation

### 1. Database Validation

```bash
# Run validation script
psql -U postgres << 'EOF'
-- Check all databases exist
SELECT datname FROM pg_database
WHERE datname IN (
  'event_processing_db',
  'fhir_db',
  'patient_db',
  'cql_engine_db',
  'quality_measure_db',
  'care_gap_db',
  'analytics_db',
  'consent_db'
);

-- Expected: 8 rows
EOF
```

### 2. Table Count Validation

```bash
# Count tables in each database
for db in event_processing_db fhir_db patient_db cql_engine_db quality_measure_db care_gap_db analytics_db consent_db; do
  count=$(psql -U postgres -d $db -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE';")
  echo "$db: $count tables"
done

# Expected counts:
# event_processing_db: 3+ tables
# fhir_db: 8+ tables
# patient_db: 3+ tables
# cql_engine_db: 3+ tables
# quality_measure_db: 8+ tables
# care_gap_db: 3+ tables
# analytics_db: 3+ tables
# consent_db: 3+ tables
```

### 3. Index Validation

```bash
# Count GIN indexes across all databases
psql -U postgres << 'EOF'
\c event_processing_db
SELECT COUNT(*) as gin_indexes FROM pg_indexes WHERE indexdef LIKE '%USING gin%';

\c fhir_db
SELECT COUNT(*) as gin_indexes FROM pg_indexes WHERE indexdef LIKE '%USING gin%';

\c quality_measure_db
SELECT COUNT(*) as gin_indexes FROM pg_indexes WHERE indexdef LIKE '%USING gin%';

\c cql_engine_db
SELECT COUNT(*) as gin_indexes FROM pg_indexes WHERE indexdef LIKE '%USING gin%';

\c patient_db
SELECT COUNT(*) as gin_indexes FROM pg_indexes WHERE indexdef LIKE '%USING gin%';
EOF

# Expected total: 21 GIN indexes
```

### 4. API Endpoint Validation

```bash
# Test authentication
curl -X POST http://localhost:9000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "demo.doctor",
    "password": "Demo123!@#"
  }'

# Expected: JWT token response

# Test authenticated endpoint
TOKEN="<access_token_from_above>"

curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:9000/api/quality/quality-measure/results

# Expected: 200 OK (empty array initially)
```

---

## Performance Tuning

### PostgreSQL Configuration

```sql
-- Recommended postgresql.conf settings for production

-- Memory
shared_buffers = 4GB
effective_cache_size = 12GB
maintenance_work_mem = 1GB
work_mem = 64MB

-- Parallelism
max_parallel_workers_per_gather = 4
max_parallel_workers = 8
max_worker_processes = 8

-- WAL
wal_buffers = 16MB
min_wal_size = 2GB
max_wal_size = 8GB

-- Checkpoint
checkpoint_completion_target = 0.9
checkpoint_timeout = 15min

-- Autovacuum
autovacuum = on
autovacuum_max_workers = 3
autovacuum_naptime = 20s

-- Statistics
track_counts = on
track_activities = on
track_io_timing = on
```

### Run ANALYZE on all tables

```bash
psql -U postgres << 'EOF'
\c event_processing_db
ANALYZE;

\c fhir_db
ANALYZE;

\c patient_db
ANALYZE;

\c cql_engine_db
ANALYZE;

\c quality_measure_db
ANALYZE;

\c care_gap_db
ANALYZE;

\c analytics_db
ANALYZE;

\c consent_db
ANALYZE;
EOF
```

---

## Monitoring Setup

### 1. Enable Actuator Metrics

All services expose metrics at `/actuator/metrics`

```bash
# Check available metrics
curl http://localhost:8087/actuator/metrics

# Specific metric examples:
curl http://localhost:8087/actuator/metrics/jvm.memory.used
curl http://localhost:8087/actuator/metrics/http.server.requests
curl http://localhost:8087/actuator/metrics/jdbc.connections.active
```

### 2. Prometheus Configuration

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'gateway'
    static_configs:
      - targets: ['localhost:9000']
    metrics_path: '/actuator/prometheus'

  - job_name: 'cql-engine'
    static_configs:
      - targets: ['localhost:8081']
    metrics_path: '/actuator/prometheus'

  - job_name: 'quality-measure'
    static_configs:
      - targets: ['localhost:8087']
    metrics_path: '/actuator/prometheus'

  # Add all other services...
```

### 3. Key Metrics to Monitor

**Application Metrics:**
- `http_server_requests_seconds_sum` - Request latency
- `jvm_memory_used_bytes` - Memory usage
- `jdbc_connections_active` - Database connections
- `events_processed_total` - Event processing rate
- `care_gaps_created_total` - Care gap generation
- `quality_measures_calculated_total` - Measure calculation rate

**Database Metrics:**
- Table sizes: `pg_total_relation_size()`
- Index bloat: `pg_stat_user_indexes`
- Query performance: `pg_stat_statements`
- Lock contention: `pg_locks`

---

## Backup Strategy

### 1. Database Backups

```bash
#!/bin/bash
# backup-databases.sh

BACKUP_DIR="/var/backups/healthdata"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

# Backup all databases
for db in event_processing_db fhir_db patient_db cql_engine_db quality_measure_db care_gap_db analytics_db consent_db; do
  pg_dump -U postgres -Fc $db > $BACKUP_DIR/${db}_${DATE}.dump

  # Keep only last 7 days
  find $BACKUP_DIR -name "${db}_*.dump" -mtime +7 -delete
done

# Backup schema only (for documentation)
for db in event_processing_db fhir_db patient_db cql_engine_db quality_measure_db care_gap_db analytics_db consent_db; do
  pg_dump -U postgres --schema-only $db > $BACKUP_DIR/${db}_schema_${DATE}.sql
done
```

### 2. Event Store Archival

```sql
-- Archive old events (older than 90 days)
-- Run monthly

CREATE TABLE IF NOT EXISTS events_archive (LIKE events INCLUDING ALL);

-- Move to archive
INSERT INTO events_archive
SELECT * FROM events
WHERE timestamp < NOW() - INTERVAL '90 days'
  AND processed = true;

-- Verify count
SELECT
  (SELECT COUNT(*) FROM events WHERE timestamp < NOW() - INTERVAL '90 days') as to_archive,
  (SELECT COUNT(*) FROM events_archive) as archived;

-- Delete from main table (only after verification)
-- DELETE FROM events
-- WHERE timestamp < NOW() - INTERVAL '90 days'
--   AND processed = true;
```

---

## Rollback Plan

### If Migration Fails

```bash
# Rollback last changeset
cd backend/modules/services/quality-measure-service
./gradlew liquibase:rollback -PliquibaseCommandValue=1

# Rollback to specific tag
./gradlew liquibase:rollback -PliquibaseCommandValue=baseline-v1.0

# Rollback by date
./gradlew liquibase:rollback -PliquibaseCommandValue="2025-11-24"
```

### If Service Fails to Start

```bash
# 1. Check logs
docker-compose logs -f quality-measure-service

# 2. Restart single service
docker-compose restart quality-measure-service

# 3. Full rollback
docker-compose down
git checkout <previous-tag>
docker-compose up -d
```

### Database Restore

```bash
# Restore from backup
pg_restore -U postgres -d quality_measure_db /var/backups/healthdata/quality_measure_db_20251125_120000.dump

# Or from SQL dump
psql -U postgres -d quality_measure_db < /var/backups/healthdata/quality_measure_db_schema_20251125_120000.sql
```

---

## Security Hardening

### 1. Enable Row-Level Security

```sql
-- Event store
ALTER TABLE events ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_events ON events
  USING (tenant_id = current_setting('app.tenant_id', true));

-- Quality measure results
ALTER TABLE quality_measure_results ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_qm ON quality_measure_results
  USING (tenant_id = current_setting('app.tenant_id', true));

-- Care gaps
ALTER TABLE care_gaps ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_cg ON care_gaps
  USING (tenant_id = current_setting('app.tenant_id', true));

-- Apply to all tenant-isolated tables
```

### 2. Database User Permissions

```sql
-- Create service-specific users
CREATE USER event_processing_user WITH PASSWORD '<strong-password>';
CREATE USER fhir_user WITH PASSWORD '<strong-password>';
CREATE USER quality_measure_user WITH PASSWORD '<strong-password>';

-- Grant minimal permissions
GRANT CONNECT ON DATABASE event_processing_db TO event_processing_user;
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO event_processing_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO event_processing_user;

-- Repeat for all services
```

### 3. SSL/TLS Configuration

```properties
# application-production.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/quality_measure_db?sslmode=require

server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: healthdata
```

---

## Troubleshooting Guide

### Issue: Service won't start

**Symptoms:** Service fails immediately after `./gradlew bootRun`

**Diagnosis:**
```bash
# Check logs
tail -f logs/spring.log

# Check if database is accessible
psql -U postgres -d quality_measure_db -c "SELECT 1;"

# Check if port is already in use
netstat -tuln | grep 8087
```

**Solutions:**
1. Verify database connection string
2. Check if migrations completed successfully
3. Kill process using the port: `kill -9 $(lsof -ti:8087)`

---

### Issue: Migration fails with "column already exists"

**Symptoms:** Liquibase error during migration

**Diagnosis:**
```sql
SELECT * FROM databasechangelog ORDER BY orderexecuted DESC LIMIT 10;
```

**Solutions:**
1. Check if migration was partially applied
2. Rollback and re-run:
   ```bash
   ./gradlew liquibase:rollback -PliquibaseCommandValue=1
   ./gradlew liquibase:update
   ```

---

### Issue: Slow query performance

**Symptoms:** API requests taking >5 seconds

**Diagnosis:**
```sql
-- Enable query logging
ALTER DATABASE quality_measure_db SET log_min_duration_statement = 1000;

-- Check slow queries
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;
```

**Solutions:**
1. Run ANALYZE on tables
2. Check if indexes exist:
   ```sql
   SELECT tablename, indexname FROM pg_indexes WHERE tablename = 'care_gaps';
   ```
3. Add missing indexes if needed

---

## Deployment Completion Checklist

- [ ] All 8 databases created
- [ ] All migrations executed successfully
- [ ] All 48 tables created
- [ ] 21 GIN indexes verified
- [ ] All services started and healthy
- [ ] Health checks passing
- [ ] Authentication working
- [ ] Sample data loaded
- [ ] Backups configured
- [ ] Monitoring configured
- [ ] SSL/TLS enabled
- [ ] Row-level security enabled
- [ ] Documentation updated
- [ ] Team trained on operations

---

**Deployment Runbook Prepared By:** Platform Engineering Team
**Date:** November 25, 2025
**Next Review:** Post-production deployment
