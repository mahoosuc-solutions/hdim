# HDIM Platform v1.2.0 - Production Deployment Checklist

**Release Version:** v1.2.0
**Target Deployment Date:** January 25, 2026
**Created:** January 11, 2026
**Owner:** HDIM Platform Team

---

## Table of Contents

1. [Pre-Deployment Preparation](#pre-deployment-preparation)
2. [Infrastructure Readiness](#infrastructure-readiness)
3. [Backup & Rollback](#backup--rollback)
4. [Deployment Steps](#deployment-steps)
5. [Post-Deployment Validation](#post-deployment-validation)
6. [Rollback Procedure](#rollback-procedure)
7. [Communication Plan](#communication-plan)

---

## Overview

This checklist ensures a safe, validated deployment of HDIM Platform v1.2.0 to production. All items must be checked off before, during, and after deployment.

**Deployment Window:** 2-4 hours (estimated)
**Downtime Window:** 15-30 minutes (database migrations + service restart)
**Rollback Time:** 30-45 minutes (if needed)

---

## Pre-Deployment Preparation

### Documentation Review

- [ ] **Read RELEASE_NOTES_v1.2.0.md** - Understand all changes, new features, breaking changes
- [ ] **Read UPGRADE_GUIDE_v1.2.0.md** - Review step-by-step upgrade instructions
- [ ] **Read KNOWN_ISSUES_v1.2.0.md** - Understand known limitations and workarounds
- [ ] **Review this deployment checklist** - Ensure all team members understand responsibilities

**Assigned to:** `__________________`
**Completed:** `____/____/____`

### Team Coordination

- [ ] **Deployment lead assigned** - Single point of contact for deployment decisions
- [ ] **Database admin available** - For migration monitoring and rollback if needed
- [ ] **On-call engineer available** - For post-deployment monitoring (first 48 hours)
- [ ] **Communication channel established** - Slack/Teams channel for deployment coordination

**Deployment Lead:** `__________________`
**DBA:** `__________________`
**On-Call Engineer:** `__________________`

### Stakeholder Communication

- [ ] **Maintenance window scheduled** - Minimum 7 days advance notice
- [ ] **Stakeholders notified** - Email sent to all users about maintenance window
- [ ] **Support team briefed** - Support team understands new features and potential issues
- [ ] **Change request approved** - Change management process followed (if applicable)

**Notification Sent:** `____/____/____`
**Change Request ID:** `__________________`

---

## Infrastructure Readiness

### System Requirements Validation

- [ ] **Java 21 installed** - Verify: `java -version` shows 21.x
- [ ] **Docker 24.0+ installed** - Verify: `docker --version` shows 24.0+
- [ ] **Docker Compose v2 installed** - Verify: `docker compose version` shows 2.x+
- [ ] **PostgreSQL 16 running** - Verify: `docker exec healthdata-postgres psql -U healthdata -c "SELECT version();"`
- [ ] **Disk space sufficient** - Minimum 5GB free for backups and images
- [ ] **Memory available** - Minimum 16GB RAM (8GB for services, 8GB for overhead)

**Verified by:** `__________________`
**Date:** `____/____/____`

### Network Requirements

- [ ] **Port 16686 available** - Jaeger UI (new in v1.2.0)
- [ ] **Port 4318 available** - OTLP HTTP receiver (new in v1.2.0)
- [ ] **Port 8107 available** - notification-service (CHANGED from 8089)
- [ ] **All existing ports available** - 8080-8105 (except 8089)
- [ ] **Firewall rules updated** - Allow new Jaeger ports if needed
- [ ] **Load balancer configured** - Update notification-service from 8089 to 8107

**Network Admin:** `__________________`
**Verified:** `____/____/____`

### Dependencies Check

- [ ] **Jaeger container image pulled** - `docker pull jaegertracing/all-in-one:latest`
- [ ] **PostgreSQL pg_trgm extension available** - Required for quality-measure migrations
- [ ] **Redis 7.2 running** - Verify: `redis-cli ping` returns PONG
- [ ] **Kafka 3.6 running** - Verify: `kafka-topics.sh --list` works

**Verified by:** `__________________`
**Date:** `____/____/____`

---

## Backup & Rollback

### Database Backup (CRITICAL)

**Backup Command:**
```bash
# Create backup directory
BACKUP_DIR="/backup/hdim/v1.2.0-$(date +%Y%m%d_%H%M%S)"
mkdir -p $BACKUP_DIR

# Backup all HDIM databases (29 databases)
docker exec healthdata-postgres pg_dumpall -U healthdata > $BACKUP_DIR/hdim_full_backup.sql

# Verify backup
ls -lh $BACKUP_DIR/hdim_full_backup.sql
# Should show non-zero file size

# Test backup integrity
head -50 $BACKUP_DIR/hdim_full_backup.sql
# Should show PostgreSQL dump header
```

**Checklist:**
- [ ] **Full database backup completed** - All 29 databases
- [ ] **Backup file size verified** - Non-zero, expected size (typically 500MB-2GB)
- [ ] **Backup integrity tested** - Header visible, no corruption
- [ ] **Backup location documented** - Path recorded: `__________________`
- [ ] **Backup accessible by rollback team** - Permissions verified

**Backup Location:** `__________________`
**Backup Size:** `__________________`
**Backup Time:** `____:____ (UTC)`
**Backup Verified by:** `__________________`

### Configuration Backup

```bash
# Backup docker-compose.yml
cp docker-compose.yml $BACKUP_DIR/docker-compose.yml.v1.1.0

# Backup environment files
cp .env $BACKUP_DIR/.env.v1.1.0 2>/dev/null || echo "No .env file"

# Backup all service application.yml files
find backend/modules/services -name "application*.yml" -exec cp --parents {} $BACKUP_DIR \;

# Document current git commit
git rev-parse HEAD > $BACKUP_DIR/v1.1.0_commit.txt

# Document current Docker images
docker compose ps --format json > $BACKUP_DIR/v1.1.0_containers.json
```

**Checklist:**
- [ ] **docker-compose.yml backed up**
- [ ] **Environment files backed up**
- [ ] **Service configs backed up**
- [ ] **Git commit documented**
- [ ] **Docker images documented**

**Verified by:** `__________________`
**Date:** `____/____/____`

### Rollback Plan Documentation

- [ ] **Rollback procedure reviewed** - See [Rollback Procedure](#rollback-procedure) section
- [ ] **Rollback time estimated** - 30-45 minutes
- [ ] **Rollback approval process defined** - Who can authorize rollback?
- [ ] **Rollback decision criteria** - What conditions trigger rollback?

**Rollback Criteria:**
- Database migration fails and cannot be fixed within 15 minutes
- More than 3 critical services fail health checks after restart
- Data loss or corruption detected
- New features cause production incidents affecting >10% of users

**Rollback Authority:** `__________________`
**Approved by:** `__________________`

---

## Deployment Steps

### Step 1: Pre-Deployment Validation

**Time Estimate:** 15 minutes

- [ ] **All pre-deployment checklists completed** - Review all sections above
- [ ] **Stakeholders notified** - "Starting deployment now" message sent
- [ ] **Monitoring systems active** - Grafana, Prometheus, alerting enabled
- [ ] **Support tickets paused** - No new deployments during maintenance window

**Started by:** `__________________`
**Time:** `____:____ (UTC)`

### Step 2: Stop Running Services

**Time Estimate:** 5 minutes

```bash
# Navigate to project root
cd /mnt/wd-black/dev/projects/hdim-master

# Stop all services (keeps volumes/data intact)
docker compose down

# Verify all containers stopped
docker compose ps
# Should show no running containers
```

**Checklist:**
- [ ] **All services stopped** - `docker compose ps` shows nothing running
- [ ] **Volumes preserved** - Database data not deleted
- [ ] **Stop time documented**

**Stopped by:** `__________________`
**Time:** `____:____ (UTC)`
**Downtime Started:** `____:____ (UTC)`

### Step 3: Pull Latest Code

**Time Estimate:** 2 minutes

```bash
# Fetch latest changes
git fetch origin

# View available tags
git tag | grep v1.2

# Checkout v1.2.0 tag
git checkout v1.2.0

# Verify correct version
git describe --tags
# Should show: v1.2.0
```

**Checklist:**
- [ ] **Git tag v1.2.0 checked out**
- [ ] **Version verified** - `git describe --tags` shows v1.2.0
- [ ] **No local modifications** - `git status` shows clean working tree

**Verified by:** `__________________`
**Time:** `____:____ (UTC)`

### Step 4: Verify docker-compose.yml Changes

**Time Estimate:** 5 minutes

**Critical Changes to Verify:**

1. **Jaeger Service Added (NEW)**
   ```yaml
   jaeger:
     image: jaegertracing/all-in-one:latest
     ports:
       - "16686:16686"  # Jaeger UI
       - "4318:4318"    # OTLP HTTP receiver
   ```
   - [ ] Jaeger service present in docker-compose.yml
   - [ ] Ports 16686 and 4318 exposed

2. **notification-service Port Changed (BREAKING)**
   ```yaml
   notification-service:
     ports:
       - "8107:8107"  # Was 8089
   ```
   - [ ] Port changed from 8089 to 8107
   - [ ] Load balancer updated to use port 8107

3. **All Services Have OTLP Configuration (NEW)**
   ```yaml
   environment:
     OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318/v1/traces
     OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf
     OTEL_SERVICE_NAME: <service-name>
     _JAVA_OPTIONS: "-Djava.net.preferIPv4Stack=true"
   ```
   - [ ] All 11 Java services have complete OTLP configuration
   - [ ] Endpoints point to `http://jaeger:4318/v1/traces` (not just `http://jaeger:4318`)
   - [ ] IPv4 stack preference flag present

4. **notification-service Configuration (CRITICAL)**
   ```yaml
   environment:
     SPRING_JPA_HIBERNATE_DDL_AUTO: validate  # Was "create"
     SPRING_LIQUIBASE_ENABLED: true
   ```
   - [ ] DDL auto set to "validate" (NOT "create")
   - [ ] Liquibase enabled

**Verified by:** `__________________`
**Time:** `____:____ (UTC)`

### Step 5: Rebuild Docker Images

**Time Estimate:** 10-15 minutes

```bash
# Rebuild all services with new code
docker compose --profile core build

# This may take 10-15 minutes depending on server specs
# Expected output: "Building <service-name>... DONE" for each service
```

**Checklist:**
- [ ] **All services built successfully** - No build errors
- [ ] **Build time documented** - `__________________`
- [ ] **New images tagged** - Verify: `docker images | grep hdim`

**Built by:** `__________________`
**Build Time:** `____:____ (UTC)` to `____:____ (UTC)`
**Duration:** `______` minutes

### Step 6: Start Infrastructure Services

**Time Estimate:** 3 minutes

```bash
# Start infrastructure services first
docker compose up -d postgres redis kafka jaeger

# Wait for infrastructure to be ready
sleep 30

# Verify infrastructure health
docker compose ps postgres redis kafka jaeger
# All should show "running (healthy)"
```

**Checklist:**
- [ ] **PostgreSQL running** - Status: healthy
- [ ] **Redis running** - Status: healthy
- [ ] **Kafka running** - Status: healthy
- [ ] **Jaeger running** - Status: healthy (NEW)
- [ ] **All infrastructure services healthy**

**Started by:** `__________________`
**Time:** `____:____ (UTC)`

### Step 7: Run Database Migrations

**Time Estimate:** 5 minutes

**Migrations will run automatically when quality-measure-service starts.**

```bash
# Start quality-measure-service (triggers Liquibase migrations)
docker compose up -d quality-measure-service

# Monitor migration progress
docker compose logs -f quality-measure-service | grep -i liquibase

# Expected output:
# "Liquibase: Successfully acquired change log lock"
# "Liquibase: Reading from quality_db.databasechangelog"
# "Liquibase: Running Changeset: db/changelog/0034-create-patient-measure-assignments.xml"
# "Liquibase: Running Changeset: db/changelog/0035-create-patient-measure-overrides.xml"
# ... (5 more migrations)
# "Liquibase: Successfully released change log lock"
```

**Checklist:**
- [ ] **Migration started** - Liquibase logs visible
- [ ] **All 7 migrations executed** - Changesets 0034-0040
- [ ] **No migration errors** - No "ERROR" or "FAILED" in logs
- [ ] **Change log lock released** - Migration completed successfully
- [ ] **quality-measure-service healthy** - Health check passes

**Monitored by:** `__________________`
**Migration Start:** `____:____ (UTC)`
**Migration Complete:** `____:____ (UTC)`
**Duration:** `______` minutes

**Verify Migrations:**
```bash
# Check if all 7 new tables were created
docker exec healthdata-postgres psql -U healthdata -d quality_db -c "
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND (table_name LIKE 'patient_measure%'
    OR table_name LIKE 'measure_config%'
    OR table_name LIKE 'measure_execution%'
    OR table_name LIKE 'measure_modification%')
ORDER BY table_name;"

# Expected tables (7):
# - measure_config_profiles
# - measure_execution_history
# - measure_modification_audit
# - patient_measure_assignments
# - patient_measure_eligibility_cache
# - patient_measure_overrides
# - patient_profile_assignments
```

- [ ] **All 7 tables created** - Count verified: `______`
- [ ] **Table schemas correct** - No missing columns

**Verified by:** `__________________`

### Step 8: Start All Core Services

**Time Estimate:** 5 minutes

```bash
# Start core services
docker compose --profile core up -d

# Monitor startup logs
docker compose logs -f --tail=50

# Wait for services to be healthy
sleep 60
```

**Checklist:**
- [ ] **All services started** - `docker compose ps` shows all services running
- [ ] **No startup errors** - Logs show successful initialization
- [ ] **Health checks passing** - All services show "healthy" status

**Started by:** `__________________`
**Time:** `____:____ (UTC)`
**Downtime Ended:** `____:____ (UTC)`
**Total Downtime:** `______` minutes

### Step 9: Verify OTLP Traces

**Time Estimate:** 5 minutes

```bash
# Access Jaeger UI
open http://localhost:16686
# Or use curl to verify Jaeger is responding
curl -s http://localhost:16686 | head -20

# Trigger sample request to quality-measure-service
curl -H "X-Tenant-ID: TENANT-001" \
     -H "X-Auth-User-Id: 00000000-0000-0000-0000-000000000001" \
     http://localhost:8087/quality-measure/actuator/health

# Wait 10 seconds for trace propagation
sleep 10

# Check Jaeger UI for traces:
# 1. Service dropdown: Select "quality-measure-service"
# 2. Click "Find Traces"
# 3. Should see recent traces with health check operation
```

**Checklist:**
- [ ] **Jaeger UI accessible** - http://localhost:16686 loads
- [ ] **Services visible in dropdown** - All 11 services listed
- [ ] **Traces visible** - Recent traces displayed for quality-measure-service
- [ ] **No OTLP export errors** - Service logs show successful trace export

**Verified by:** `__________________`
**Time:** `____:____ (UTC)`

---

## Post-Deployment Validation

### Health Check Validation

**Time Estimate:** 10 minutes

```bash
# Check all service health endpoints
for port in 8080 8081 8084 8085 8086 8087 8107; do
  echo "Checking port $port..."
  curl -s http://localhost:$port/actuator/health | jq '.status'
done

# All should return "UP"
```

**Checklist:**
- [ ] **gateway-service (8080)** - Status: UP
- [ ] **cql-engine-service (8081)** - Status: UP
- [ ] **patient-service (8084)** - Status: UP
- [ ] **fhir-service (8085)** - Status: UP
- [ ] **care-gap-service (8086)** - Status: UP
- [ ] **quality-measure-service (8087)** - Status: UP
- [ ] **notification-service (8107)** - Status: UP (NEW PORT)

**Verified by:** `__________________`
**Time:** `____:____ (UTC)`

### New API Endpoint Testing

**Time Estimate:** 15 minutes

**Test Measure Assignment API (NEW in v1.2.0):**

```bash
# Get patient measure assignments (should return 200 OK even if empty)
curl -X GET \
  "http://localhost:8087/quality-measure/patients/00000000-0000-0000-0000-000000000001/measure-assignments" \
  -H "X-Tenant-ID: TENANT-001" \
  -H "X-Auth-User-Id: 00000000-0000-0000-0000-000000000001" \
  -H "X-Auth-Roles: ADMIN,EVALUATOR"

# Expected: 200 OK, JSON array (may be empty)
```

**Checklist:**
- [ ] **GET measure assignments** - Returns 200 OK
- [ ] **POST measure assignment** - Returns 201 Created (if tested)
- [ ] **GET measure overrides** - Returns 200 OK
- [ ] **Authentication enforced** - 401 without auth headers

**Tested by:** `__________________`
**Time:** `____:____ (UTC)`

### Data Integrity Verification

**Time Estimate:** 10 minutes

```bash
# Check record counts in existing tables
docker exec healthdata-postgres psql -U healthdata -d quality_db -c "
SELECT
  (SELECT COUNT(*) FROM quality_measures) as quality_measures,
  (SELECT COUNT(*) FROM care_gaps) as care_gaps,
  (SELECT COUNT(*) FROM measure_evaluations) as measure_evaluations;"

# Compare with pre-upgrade counts
# Should match exactly (no data loss)
```

**Checklist:**
- [ ] **quality_measures count matches** - Pre: `______`, Post: `______`
- [ ] **care_gaps count matches** - Pre: `______`, Post: `______`
- [ ] **measure_evaluations count matches** - Pre: `______`, Post: `______`
- [ ] **No data loss detected**

**Verified by:** `__________________`
**Time:** `____:____ (UTC)`

### Service Log Review

**Time Estimate:** 15 minutes

```bash
# Review logs for errors
for service in quality-measure-service notification-service cql-engine-service; do
  echo "=== $service logs ==="
  docker compose logs $service | grep -i "error\|exception\|failed" | tail -20
done
```

**Checklist:**
- [ ] **No critical errors** - No ERROR level logs in first 5 minutes
- [ ] **No startup exceptions** - Services initialized successfully
- [ ] **No OTLP export errors** - Traces sent successfully to Jaeger
- [ ] **No database connection errors** - All services connected to PostgreSQL
- [ ] **No migration errors** - Liquibase migrations successful

**Reviewed by:** `__________________`
**Time:** `____:____ (UTC)`

### Performance Metrics

**Time Estimate:** 10 minutes

```bash
# Check response times for critical endpoints
time curl -s http://localhost:8087/quality-measure/actuator/health > /dev/null
# Should be <500ms

# Check database connection pool
docker compose logs quality-measure-service | grep -i "hikari"
# Should show healthy connection pool

# Check memory usage
docker stats --no-stream quality-measure-service
# Should be <2GB memory usage
```

**Checklist:**
- [ ] **Health endpoint response <500ms** - Measured: `______` ms
- [ ] **HikariCP connection pool healthy** - No exhaustion warnings
- [ ] **Memory usage normal** - <2GB per service
- [ ] **No connection pool leaks** - notification-service stable

**Verified by:** `__________________`
**Time:** `____:____ (UTC)`

### Monitoring & Alerting

**Time Estimate:** 10 minutes

- [ ] **Grafana dashboards updated** - New Jaeger metrics visible
- [ ] **Prometheus targets healthy** - All services scraped successfully
- [ ] **Alert rules active** - No false positives triggered
- [ ] **On-call engineer notified** - Deployment complete, monitoring active

**Verified by:** `__________________`
**Time:** `____:____ (UTC)`

---

## Post-Deployment Communication

### Internal Notification

- [ ] **Deployment team notified** - "Deployment successful" message sent
- [ ] **Support team notified** - New features and known issues shared
- [ ] **On-call handoff completed** - Monitoring responsibilities transferred

**Notified by:** `__________________`
**Time:** `____:____ (UTC)`

### External Notification

- [ ] **Stakeholders notified** - "Maintenance complete" email sent
- [ ] **Users notified** - System available, new features announced
- [ ] **Release notes published** - RELEASE_NOTES_v1.2.0.md shared

**Notified by:** `__________________`
**Time:** `____:____ (UTC)`

---

## Rollback Procedure

**⚠️ USE ONLY IF DEPLOYMENT FAILS OR CRITICAL ISSUES OCCUR**

### Rollback Decision Criteria

Initiate rollback if ANY of the following occur:
- Database migration fails and cannot be fixed within 15 minutes
- More than 3 core services fail health checks after restart
- Data loss or corruption detected in database verification
- New features cause production incidents affecting >10% of users
- Critical security vulnerability discovered in v1.2.0

**Rollback Authority:** (See [Backup & Rollback](#backup--rollback) section)

### Rollback Steps

**Time Estimate:** 30-45 minutes

#### Step 1: Stop All Services

```bash
docker compose down
```

- [ ] **All services stopped**

#### Step 2: Restore Database

```bash
# Locate backup
BACKUP_DIR="/backup/hdim/v1.2.0-YYYYMMDD_HHMMSS"  # Use actual backup path

# Stop PostgreSQL
docker compose down postgres

# Start PostgreSQL
docker compose up -d postgres
sleep 10

# Restore database
docker exec -i healthdata-postgres psql -U healthdata < $BACKUP_DIR/hdim_full_backup.sql

# Verify restoration
docker exec healthdata-postgres psql -U healthdata -d quality_db -c "SELECT COUNT(*) FROM quality_measures;"
```

- [ ] **Database restored from backup**
- [ ] **Record counts verified**

#### Step 3: Revert Code

```bash
# Checkout v1.1.0 tag
git checkout v1.1.0

# Verify version
git describe --tags
# Should show: v1.1.0

# Restore docker-compose.yml backup
cp $BACKUP_DIR/docker-compose.yml.v1.1.0 docker-compose.yml
```

- [ ] **Code reverted to v1.1.0**
- [ ] **docker-compose.yml restored**

#### Step 4: Rebuild Services

```bash
# Rebuild with v1.1.0 code
docker compose --profile core build
```

- [ ] **Services rebuilt with v1.1.0**

#### Step 5: Restart Services

```bash
# Start infrastructure
docker compose up -d postgres redis kafka

# Start core services (no Jaeger in v1.1.0)
docker compose --profile core up -d
```

- [ ] **All services started**
- [ ] **Health checks passing**

#### Step 6: Verify Rollback

```bash
# Check services are running v1.1.0
docker compose ps

# Verify git version
git describe --tags
# Should show: v1.1.0

# Test critical endpoints
curl http://localhost:8087/quality-measure/actuator/health
curl http://localhost:8089/notification/actuator/health  # Note: Port 8089 in v1.1.0
```

- [ ] **Version confirmed as v1.1.0**
- [ ] **All services healthy**
- [ ] **Critical endpoints responding**

#### Step 7: Post-Rollback Communication

- [ ] **Stakeholders notified** - Rollback completed
- [ ] **Root cause analysis initiated** - Understand deployment failure
- [ ] **Incident report created** - Document issues and resolution
- [ ] **Post-mortem scheduled** - Review what went wrong

**Rollback Completed by:** `__________________`
**Time:** `____:____ (UTC)`

---

## Success Criteria

Deployment is considered successful when:

- [ ] **All pre-deployment checks completed**
- [ ] **Database backups completed and verified**
- [ ] **All 7 database migrations executed successfully**
- [ ] **All 13 services running and healthy**
- [ ] **No data loss detected (record counts match)**
- [ ] **Jaeger UI accessible and receiving traces**
- [ ] **New API endpoints responding correctly**
- [ ] **No critical errors in service logs**
- [ ] **Performance metrics acceptable**
- [ ] **Stakeholders notified of completion**

**Deployment Status:** ⬜ Success  ⬜ Partial  ⬜ Failed  ⬜ Rolled Back

**Approved by:** `__________________`
**Date:** `____/____/____`
**Time:** `____:____ (UTC)`

---

## Appendix

### Key Contacts

| Role | Name | Email | Phone | Slack |
|------|------|-------|-------|-------|
| Deployment Lead | | | | |
| Database Admin | | | | |
| On-Call Engineer | | | | |
| Platform Team Lead | | | | |
| Security Team | | | | |

### Reference Documentation

- [Release Notes](RELEASE_NOTES_v1.2.0.md)
- [Upgrade Guide](UPGRADE_GUIDE_v1.2.0.md)
- [Known Issues](KNOWN_ISSUES_v1.2.0.md)
- [System Architecture](docs/architecture/SYSTEM_ARCHITECTURE.md)
- [Production Security Guide](docs/PRODUCTION_SECURITY_GUIDE.md)
- [Rollback Runbook](docs/operations/ROLLBACK_RUNBOOK.md)

### Deployment Timeline

| Phase | Estimated Time | Actual Time | Notes |
|-------|---------------|-------------|-------|
| Pre-deployment prep | 60 min | | |
| Service shutdown | 5 min | | |
| Code update | 2 min | | |
| Image rebuild | 15 min | | |
| Database migration | 5 min | | |
| Service startup | 5 min | | |
| Validation | 40 min | | |
| **Total** | **132 min (~2.2 hours)** | | |

**Actual Deployment Duration:** `______` hours `______` minutes

---

## Post-Deployment 48-Hour Monitoring

### First 24 Hours

- [ ] **Hour 1:** Monitor for immediate errors, crashes, or anomalies
- [ ] **Hour 6:** Review error rates, latency, throughput metrics
- [ ] **Hour 12:** Check for database performance degradation
- [ ] **Hour 24:** Full metrics review, user feedback analysis

### 24-48 Hours

- [ ] **Day 2:** Monitor for delayed issues (cache expiration, periodic jobs)
- [ ] **Day 2:** Review on-call incidents related to v1.2.0
- [ ] **Day 2:** Collect user feedback on new features
- [ ] **Day 2:** Performance trending analysis

**Monitoring Lead:** `__________________`
**Monitoring Period:** `____/____/____` to `____/____/____`

---

**Document Version:** 1.0
**Status:** Ready for Use
**Last Updated:** January 11, 2026
**Maintained By:** HDIM Platform Team
