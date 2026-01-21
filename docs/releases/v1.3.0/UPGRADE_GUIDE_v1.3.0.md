# HDIM Platform Upgrade Guide - v1.3.0

**From Version:** v1.2.0
**To Version:** v1.3.0
**Est. Upgrade Time:** 2-4 hours

---

## 📋 Pre-Upgrade Checklist

- [ ] Review [RELEASE_NOTES_v1.3.0.md](./RELEASE_NOTES_v1.3.0.md)
- [ ] Review [KNOWN_ISSUES_v1.3.0.md](./KNOWN_ISSUES_v1.3.0.md)
- [ ] Backup all PostgreSQL databases
- [ ] Backup Redis data (if persistent)
- [ ] Document current environment variables
- [ ] Schedule maintenance window
- [ ] Notify stakeholders

---

## 🔧 Prerequisite Changes

### Environment Variables

**New Variables for CQRS Event Services:**
```bash
# CQRS Event Service Ports
PATIENT_EVENT_SERVICE_PORT=8110
CARE_GAP_EVENT_SERVICE_PORT=8111
QUALITY_MEASURE_EVENT_SERVICE_PORT=8112
CLINICAL_WORKFLOW_EVENT_SERVICE_PORT=8113

# Kafka Topics for CQRS Events
KAFKA_TOPIC_PATIENT_EVENTS=patient-events
KAFKA_TOPIC_CARE_GAP_EVENTS=care-gap-events
KAFKA_TOPIC_QUALITY_EVENTS=quality-events
KAFKA_TOPIC_WORKFLOW_EVENTS=workflow-events

# Event Store Configuration
EVENT_STORE_ENABLED=true
EVENT_STORE_RETENTION_DAYS=365
```

**No Changed Variables** - All existing environment variables remain the same

**No Removed Variables** - v1.3.0 is fully backward compatible

### Infrastructure Requirements

- **Java:** 21 (LTS) - Eclipse Temurin (verify: `java -version`)
- **Gradle:** 8.11+ (verify: `./gradlew --version`)
- **PostgreSQL:** 16-alpine (verify: `docker compose exec postgres psql --version`)
- **Redis:** 7-alpine (verify: `docker compose exec redis redis-server --version`)
- **Kafka:** 3.6 / Confluent 7.5.0 (verify: `docker compose exec kafka kafka-broker-api-versions`)
- **Docker:** 24.0+ (verify: `docker --version`)
- **Docker Compose:** 2.20+ (verify: `docker compose version`)

---

## 🚀 Upgrade Steps

### Step 1: Stop All Services

```bash
docker compose down
```

**Verification:**
```bash
docker compose ps  # Should show no running containers
```

---

### Step 2: Backup Databases

```bash
# Backup all PostgreSQL databases
for db in gateway_db fhir_db patient_db quality_db cql_db caregap_db; do
  docker exec healthdata-postgres pg_dump -U healthdata $db > backup_${db}_$(date +%Y%m%d).sql
done

# Backup Redis (if applicable)
docker exec healthdata-redis redis-cli SAVE
docker cp healthdata-redis:/data/dump.rdb backup_redis_$(date +%Y%m%d).rdb
```

**Verification:**
```bash
ls -lh backup_*.sql backup_*.rdb
```

---

### Step 3: Pull Latest Code

```bash
git fetch origin
git checkout v1.3.0
git pull origin v1.3.0
```

**Verification:**
```bash
git log -1  # Should show v1.3.0 tag
```

---

### Step 4: Update Environment Variables

Edit `.env` or `docker-compose.yml`:

```bash
# Add new CQRS event service variables
export PATIENT_EVENT_SERVICE_PORT=8110
export CARE_GAP_EVENT_SERVICE_PORT=8111
export QUALITY_MEASURE_EVENT_SERVICE_PORT=8112
export CLINICAL_WORKFLOW_EVENT_SERVICE_PORT=8113

export KAFKA_TOPIC_PATIENT_EVENTS=patient-events
export KAFKA_TOPIC_CARE_GAP_EVENTS=care-gap-events
export KAFKA_TOPIC_QUALITY_EVENTS=quality-events
export KAFKA_TOPIC_WORKFLOW_EVENTS=workflow-events

export EVENT_STORE_ENABLED=true
export EVENT_STORE_RETENTION_DAYS=365

# No variables changed - v1.3.0 is backward compatible
```

**Verification:**
```bash
env | grep -E "(EVENT_SERVICE_PORT|KAFKA_TOPIC|EVENT_STORE)"
```

---

### Step 5: Run Database Migrations

```bash
# Start PostgreSQL only
docker compose up -d postgres

# Wait for PostgreSQL to be ready
sleep 10

# Run Liquibase migrations for each service
cd backend
./gradlew update  # Runs all Liquibase updates

# Verify migrations
for db in gateway_db fhir_db patient_db quality_db cql_db caregap_db; do
  echo "Checking $db..."
  docker exec healthdata-postgres psql -U healthdata -d $db -c "SELECT COUNT(*) FROM databasechangelog;"
done
```

**Expected Migration Count:** 157

**Verification:**
```bash
# Check for migration errors
docker compose logs postgres | grep -i error
```

---

### Step 6: Build Updated Docker Images

```bash
docker compose build
```

**Verification:**
```bash
docker images | grep v1.3.0
```

---

### Step 7: Start Services

```bash
# Start infrastructure services first
docker compose up -d postgres redis kafka

# Wait for infrastructure to be healthy
sleep 30

# Start application services
docker compose up -d
```

**Verification:**
```bash
docker compose ps  # All services should be "healthy"
```

---

### Step 8: Verify Service Health

```bash
# Check all service health endpoints
for port in 8001 8080 8081 8084 8085 8086 8087; do
  echo "Checking port $port..."
  curl -s http://localhost:$port/actuator/health | jq .
done
```

**Expected:** All services return `{"status":"UP"}`

---

### Step 9: Run Smoke Tests

```bash
# Test patient service
curl -X POST http://localhost:8001/patient/api/v1/patients \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: test-tenant" \
  -H "X-Auth-Validated: true" \
  -d '{
    "firstName": "John",
    "lastName": "Smith",
    "dateOfBirth": "1980-01-15",
    "gender": "M",
    "email": "john.smith@example.com"
  }'

# Test quality measure service
curl http://localhost:8001/quality-measure/api/v1/measures \
  -H "X-Tenant-ID: test-tenant" \
  -H "X-Auth-Validated: true"
```

---

### Step 10: Monitor Logs

```bash
# Monitor all services for 5 minutes
docker compose logs -f --tail=100

# Check for errors
docker compose logs | grep -i "error\|exception" | grep -v "ClassNotFoundException" | wc -l
```

**Expected:** Zero critical errors

---

## 🔄 Rollback Procedure

If upgrade fails, follow these steps to rollback:

### Step 1: Stop Services

```bash
docker compose down
```

### Step 2: Restore Database Backups

```bash
for db in gateway_db fhir_db patient_db quality_db cql_db caregap_db; do
  docker exec -i healthdata-postgres psql -U healthdata -d $db < backup_${db}_$(date +%Y%m%d).sql
done
```

### Step 3: Checkout Previous Version

```bash
git checkout v1.2.0
```

### Step 4: Rebuild and Start

```bash
docker compose build
docker compose up -d
```

### Step 5: Verify Rollback

```bash
docker compose ps
curl http://localhost:8001/actuator/health
```

---

## 🚨 Breaking Changes Migration

**No breaking changes in this release.**

All changes are backward compatible:
- CQRS services are additive (new services: ports 8110-8113)
- Database-config migration is transparent (auto-detection)
- Test infrastructure changes are internal only
- Gateway trust authentication already standardized

**API Compatibility:** 100% backward compatible with v1.2.0 clients

**Configuration Changes:** All new environment variables are optional (services will use defaults if not specified)

---

## ✅ Post-Upgrade Verification

- [ ] All services healthy
- [ ] All health endpoints return `UP`
- [ ] Smoke tests passing
- [ ] No critical errors in logs
- [ ] Database migration count matches expected
- [ ] Performance metrics within acceptable range
- [ ] Distributed tracing working (check Jaeger)
- [ ] Stakeholders notified of completion

---

## 📞 Support

**Issues:** https://github.com/webemo-aaron/hdim/issues
**Contact:** HDIM Platform Team

---

**Estimated Upgrade Time:** 2-4 hours
**Downtime Required:** 30-60 minutes
