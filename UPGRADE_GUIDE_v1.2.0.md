# HDIM Platform v1.2.0 Upgrade Guide

**Upgrade Path:** v1.1.0 → v1.2.0
**Estimated Time:** 30-60 minutes
**Downtime Required:** ~5-10 minutes (recommended)
**Difficulty:** Moderate

---

## Table of Contents

1. [Pre-Upgrade Checklist](#pre-upgrade-checklist)
2. [Prerequisites](#prerequisites)
3. [Backup Procedures](#backup-procedures)
4. [Upgrade Steps](#upgrade-steps)
5. [Configuration Changes](#configuration-changes)
6. [Database Migration](#database-migration)
7. [Post-Upgrade Validation](#post-upgrade-validation)
8. [Rollback Procedures](#rollback-procedures)
9. [Troubleshooting](#troubleshooting)

---

## Pre-Upgrade Checklist

Before beginning the upgrade, ensure you have:

- [ ] **Backup completed** (database, configuration files, environment variables)
- [ ] **Current version verified**: Confirm you're running v1.1.0
- [ ] **Downtime window scheduled**: Notify stakeholders of maintenance window
- [ ] **Rollback plan documented**: Know how to revert if issues occur
- [ ] **Access verified**: SSH/kubectl access to production environment
- [ ] **Disk space checked**: At least 2GB free for database backup
- [ ] **Review release notes**: Read `RELEASE_NOTES_v1.2.0.md` thoroughly
- [ ] **Test environment validated**: Run upgrade in staging first

---

## Prerequisites

### System Requirements

- **Java**: 21 LTS (Eclipse Temurin recommended)
- **Docker**: 24.0+ with Compose V2
- **PostgreSQL**: 16-alpine (via Docker or standalone)
- **Disk Space**: 2GB+ free for database backups
- **Memory**: Ensure adequate RAM for Jaeger (new dependency)

### Required Tools

```bash
# Verify Java version
java -version
# Should show: openjdk version "21.x.x"

# Verify Docker version
docker --version
docker compose version
# Should show: Docker version 24.0+ and Docker Compose version v2.x+

# Verify Git version
git --version
```

### Network Requirements

- **Port 16686**: Jaeger UI (new)
- **Port 4318**: OTLP HTTP receiver (new)
- Existing ports must remain available (8080-8107)

---

## Backup Procedures

### 1. Database Backup

**Critical:** Back up all databases before upgrading.

```bash
# Set backup directory
BACKUP_DIR="/backup/hdim/$(date +%Y%m%d_%H%M%S)"
mkdir -p $BACKUP_DIR

# Backup all HDIM databases
docker exec healthdata-postgres pg_dumpall -U healthdata > $BACKUP_DIR/hdim_full_backup.sql

# Verify backup
ls -lh $BACKUP_DIR/hdim_full_backup.sql
# Should show non-zero file size

# Test backup integrity (optional but recommended)
head -20 $BACKUP_DIR/hdim_full_backup.sql
# Should show PostgreSQL dump header
```

### 2. Configuration Backup

```bash
# Backup docker-compose.yml
cp docker-compose.yml $BACKUP_DIR/docker-compose.yml.backup

# Backup environment files
cp .env $BACKUP_DIR/.env.backup 2>/dev/null || echo "No .env file"

# Backup application configs
find backend/modules/services -name "application*.yml" -exec cp --parents {} $BACKUP_DIR \;
```

### 3. Document Current State

```bash
# Save current git commit
git rev-parse HEAD > $BACKUP_DIR/current_commit.txt

# Save current Docker images
docker compose ps --format json > $BACKUP_DIR/current_containers.json

# Save current environment variables
docker compose config > $BACKUP_DIR/current_config.yml
```

---

## Upgrade Steps

### Step 1: Stop Running Services

```bash
# Navigate to project root
cd /mnt/wd-black/dev/projects/hdim-master

# Stop all services (keeps volumes/data intact)
docker compose down

# Verify all containers stopped
docker compose ps
# Should show no running containers
```

### Step 2: Pull Latest Code

```bash
# Fetch latest changes
git fetch origin

# View available tags
git tag | grep v1.2

# Checkout v1.2.0
git checkout v1.2.0

# Verify correct version
git describe --tags
# Should show: v1.2.0
```

### Step 3: Update docker-compose.yml

**Add Jaeger Service** (if not already present):

```yaml
# Add to docker-compose.yml in the services section

  jaeger:
    image: jaegertracing/all-in-one:latest
    container_name: healthdata-jaeger
    restart: unless-stopped
    ports:
      - "16686:16686"  # Jaeger UI
      - "4318:4318"    # OTLP HTTP receiver (required)
      - "14268:14268"  # Jaeger collector HTTP (optional)
    environment:
      - COLLECTOR_OTLP_ENABLED=true
      - LOG_LEVEL=info
    networks:
      - healthdata-network
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:14269/"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 10s
```

**Update Environment Variables** (already done if using v1.2.0 tag):

The following services should have OTLP configuration:
- cql-engine-service
- patient-service
- fhir-service
- quality-measure-service
- care-gap-service
- notification-service
- analytics-service
- event-processing-service
- qrda-export-service
- hcc-service
- gateway-service

Example configuration (already in v1.2.0):
```yaml
environment:
  OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318/v1/traces
  OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf
  OTEL_SERVICE_NAME: service-name-here
  _JAVA_OPTIONS: "-Djava.net.preferIPv4Stack=true"
```

### Step 4: Review Critical Configuration Changes

**notification-service** (CRITICAL - prevents data loss):

```yaml
# Ensure these settings are present in docker-compose.yml
notification-service:
  environment:
    SPRING_JPA_HIBERNATE_DDL_AUTO: validate  # Was "create" - NOW "validate"
    SPRING_LIQUIBASE_ENABLED: true           # Must be true
    SPRING_DATASOURCE_HIKARI_MAX_LIFETIME: 300000  # 5 minutes (was 30 min)
    SPRING_DATASOURCE_HIKARI_KEEPALIVE_TIME: 30000  # 30 seconds (new)
  ports:
    - "8107:8107"  # Was 8089 - NOW 8107
```

---

## Configuration Changes

### Environment Variables to Add

**All Java Services** (11 services total):

```bash
# Add to each service's environment section in docker-compose.yml
OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318/v1/traces
OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf
OTEL_SERVICE_NAME: <service-name>
_JAVA_OPTIONS: "-Djava.net.preferIPv4Stack=true"
```

**No Application.yml Changes Required:**
- Services read OTLP configuration from environment variables
- No manual edits to `application.yml` files needed

---

## Database Migration

### Automatic Migration (Recommended)

Liquibase migrations run automatically on service startup.

```bash
# Start PostgreSQL first
docker compose up -d postgres

# Wait for PostgreSQL to be ready
sleep 10

# Start quality-measure-service (triggers migrations)
docker compose up -d quality-measure-service

# Monitor migration progress
docker compose logs -f quality-measure-service | grep -i liquibase

# Expected output:
# "Liquibase: Successfully acquired change log lock"
# "Liquibase: Reading from quality_db.databasechangelog"
# "Liquibase: Running Changeset: db/changelog/0034-create-patient-measure-assignments.xml"
# ...
# "Liquibase: Successfully released change log lock"
```

### Verify Migrations

```bash
# Check if all 7 new tables were created
docker exec healthdata-postgres psql -U healthdata -d quality_db -c "
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name LIKE 'patient_measure%'
  OR table_name LIKE 'measure_%'
ORDER BY table_name;"

# Expected tables:
# - measure_config_profiles
# - measure_execution_history
# - measure_modification_audit
# - patient_measure_assignments
# - patient_measure_eligibility_cache
# - patient_measure_overrides
# - patient_profile_assignments
```

### Check Migration History

```bash
# View applied changesets
docker exec healthdata-postgres psql -U healthdata -d quality_db -c "
SELECT id, author, filename, dateexecuted, orderexecuted
FROM databasechangelog
WHERE filename LIKE '%003%' OR filename LIKE '%004%'
ORDER BY orderexecuted DESC
LIMIT 10;"
```

---

## Step 5: Rebuild Services

```bash
# Rebuild all services with new code
docker compose --profile core build

# This may take 5-10 minutes
# Expected output: Building service-name... DONE
```

---

## Step 6: Start All Services

```bash
# Start infrastructure services first
docker compose up -d postgres redis kafka jaeger

# Wait for infrastructure to be ready
sleep 30

# Start core services
docker compose --profile core up -d

# Monitor startup
docker compose logs -f --tail=50
```

---

## Post-Upgrade Validation

### 1. Verify All Services Started

```bash
# Check service health
docker compose ps

# All services should show "running (healthy)"
# If any service shows "unhealthy", check logs:
docker compose logs <service-name>
```

### 2. Verify Database Migrations

```bash
# Check quality-measure-service logs for migration success
docker compose logs quality-measure-service | grep -i "liquibase\|migration"

# Should see:
# "Liquibase update successful"
# No "ERROR" or "FAILED" messages
```

### 3. Verify OTLP Traces in Jaeger

```bash
# Access Jaeger UI
open http://localhost:16686

# Or use curl
curl -s http://localhost:16686 | head -20

# Test trace export:
# 1. Make a request to any service endpoint
curl -H "X-Tenant-ID: TENANT-001" http://localhost:8087/quality-measure/actuator/health

# 2. Check Jaeger UI for traces
# - Service dropdown: Select "quality-measure-service"
# - Click "Find Traces"
# - Should see recent traces
```

### 4. Verify New API Endpoints

```bash
# Test measure assignment endpoint (requires authentication)
curl -X GET \
  "http://localhost:8087/quality-measure/patients/00000000-0000-0000-0000-000000000001/measure-assignments" \
  -H "X-Tenant-ID: TENANT-001" \
  -H "Authorization: Bearer <your-jwt-token>"

# Should return 200 OK (even if empty array)

# Test measure override endpoint
curl -X GET \
  "http://localhost:8087/quality-measure/patients/00000000-0000-0000-0000-000000000001/measure-overrides?measureId=00000000-0000-0000-0000-000000000002" \
  -H "X-Tenant-ID: TENANT-001" \
  -H "Authorization: Bearer <your-jwt-token>"

# Should return 200 OK
```

### 5. Verify Service Health Endpoints

```bash
# Check all service health endpoints
for port in 8080 8081 8084 8085 8086 8087 8107; do
  echo "Checking port $port..."
  curl -s http://localhost:$port/actuator/health | jq '.status'
done

# All should return "UP"
```

### 6. Verify No Data Loss

```bash
# Check record counts in existing tables
docker exec healthdata-postgres psql -U healthdata -d quality_db -c "
SELECT
  (SELECT COUNT(*) FROM quality_measures) as quality_measures,
  (SELECT COUNT(*) FROM care_gaps) as care_gaps,
  (SELECT COUNT(*) FROM measure_evaluations) as measure_evaluations;"

# Compare with pre-upgrade counts
# Should match (no data loss)
```

---

## Rollback Procedures

If upgrade fails or issues occur, follow these steps:

### Step 1: Stop All Services

```bash
docker compose down
```

### Step 2: Restore Database

```bash
# Restore from backup
BACKUP_DIR="/backup/hdim/<timestamp>"
docker exec -i healthdata-postgres psql -U healthdata < $BACKUP_DIR/hdim_full_backup.sql

# Verify restoration
docker exec healthdata-postgres psql -U healthdata -d quality_db -c "SELECT COUNT(*) FROM quality_measures;"
```

### Step 3: Revert Code

```bash
# Checkout previous version
git checkout v1.1.0

# Restore docker-compose.yml backup
cp $BACKUP_DIR/docker-compose.yml.backup docker-compose.yml
```

### Step 4: Restart Services

```bash
# Rebuild with v1.1.0 code
docker compose --profile core build

# Start services
docker compose --profile core up -d
```

### Step 5: Verify Rollback

```bash
# Check services are running v1.1.0
docker compose ps
git describe --tags
# Should show: v1.1.0
```

---

## Troubleshooting

### Issue: Liquibase Migration Fails

**Symptom:** Service fails to start, logs show "Liquibase validation failed"

**Solution:**
```bash
# Check database state
docker exec healthdata-postgres psql -U healthdata -d quality_db -c "
SELECT * FROM databasechangelog ORDER BY orderexecuted DESC LIMIT 5;"

# If migration partially applied, rollback manually
# (see Liquibase rollback documentation)

# Alternatively, restore from backup and retry
```

---

### Issue: Jaeger Not Receiving Traces

**Symptom:** Jaeger UI shows no traces

**Solution:**
```bash
# 1. Verify Jaeger is running
docker compose ps jaeger

# 2. Check Jaeger logs
docker compose logs jaeger | grep -i "otlp\|error"

# 3. Verify OTLP endpoint reachable from service
docker exec healthdata-quality-measure-service wget -O- http://jaeger:4318/v1/traces 2>&1

# Should see: HTTP request sent, awaiting response... 405 Method Not Allowed
# (405 is expected for GET - POST is required, but confirms endpoint is reachable)

# 4. Check service environment variables
docker exec healthdata-quality-measure-service env | grep OTEL

# Should show:
# OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4318/v1/traces
# OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
# OTEL_SERVICE_NAME=quality-measure-service
```

---

### Issue: notification-service Fails to Start

**Symptom:** notification-service exits immediately

**Solution:**
```bash
# Check logs for DDL auto issue
docker compose logs notification-service | grep -i "ddl\|hibernate\|liquibase"

# Ensure correct configuration:
docker exec healthdata-notification-service env | grep -E "DDL_AUTO|LIQUIBASE"

# Should show:
# SPRING_JPA_HIBERNATE_DDL_AUTO=validate
# SPRING_LIQUIBASE_ENABLED=true

# If incorrect, update docker-compose.yml and restart
```

---

### Issue: Port 8089 Connection Refused

**Symptom:** Health checks fail, can't connect to notification-service on 8089

**Solution:**
```bash
# notification-service port changed to 8107 in v1.2.0
# Update any scripts or monitoring that use port 8089

# Verify correct port
docker compose ps notification-service
# Should show: 0.0.0.0:8107->8107/tcp
```

---

### Issue: Out of Memory Errors

**Symptom:** Services crash with OOM errors after adding Jaeger

**Solution:**
```bash
# Increase Docker memory limit
# Docker Desktop: Settings → Resources → Memory (increase to 8GB+)

# Or reduce service memory limits in docker-compose.yml
# Jaeger requires ~512MB RAM
```

---

## Performance Validation

### Database Query Performance

```bash
# Test new index usage
docker exec healthdata-postgres psql -U healthdata -d quality_db -c "
EXPLAIN ANALYZE
SELECT * FROM patient_measure_assignments
WHERE tenant_id = 'TENANT-001'
  AND patient_id = '00000000-0000-0000-0000-000000000001'
  AND active = true;"

# Should show "Index Scan" not "Seq Scan"
```

### Trace Overhead

```bash
# Compare response times before/after OTLP
# Measure baseline (health endpoint)
time curl -s http://localhost:8087/quality-measure/actuator/health > /dev/null

# Should be <100ms (tracing adds minimal overhead)
```

---

## Post-Upgrade Checklist

- [ ] All services running and healthy
- [ ] Database migrations completed successfully
- [ ] New tables created (7 tables)
- [ ] Jaeger UI accessible (http://localhost:16686)
- [ ] Traces visible in Jaeger for all 11 services
- [ ] New API endpoints accessible
- [ ] No data loss (record counts match pre-upgrade)
- [ ] Service logs show no errors
- [ ] Performance metrics acceptable
- [ ] Monitoring/alerting updated for new endpoints
- [ ] Stakeholders notified of completion

---

## Support

If you encounter issues not covered in this guide:

1. **Check Known Issues**: Review `KNOWN_ISSUES_v1.2.0.md`
2. **Review Logs**: `docker compose logs -f <service-name>`
3. **File GitHub Issue**: https://github.com/webemo-aaron/hdim/issues
4. **Contact Support**: HDIM Platform Team

---

## Additional Resources

- [Release Notes](RELEASE_NOTES_v1.2.0.md)
- [Known Issues](KNOWN_ISSUES_v1.2.0.md)
- [OTLP Configuration Summary](OTLP_PLATFORM_CONFIGURATION_SUMMARY.md)
- [Database Migration Runbook](backend/docs/DATABASE_MIGRATION_RUNBOOK.md)
- [Jaeger Documentation](https://www.jaegertracing.io/docs/latest/)

---

**Upgrade completed successfully? Great! Welcome to HDIM Platform v1.2.0! 🎉**

---

**Last Updated:** January 11, 2026
**Version:** 1.0
**Status:** Ready for Use
