# HDIM Platform Upgrade Guide - v2.7.1-rc1

**From Version:** v2.7.0
**To Version:** v2.7.1-rc1
**Est. Upgrade Time:** 2-4 hours

---

## 📋 Pre-Upgrade Checklist

- [ ] Review [RELEASE_NOTES_v2.7.1-rc1.md](./RELEASE_NOTES_v2.7.1-rc1.md)
- [ ] Review [KNOWN_ISSUES_v2.7.1-rc1.md](./KNOWN_ISSUES_v2.7.1-rc1.md)
- [ ] Backup all PostgreSQL databases
- [ ] Backup Redis data (if persistent)
- [ ] Document current environment variables
- [ ] Schedule maintenance window
- [ ] Notify stakeholders

---

## 🔧 Prerequisite Changes

### Environment Variables

**New Variables:**
```bash
{NEW_ENV_VAR_1}={VALUE}  # Description
{NEW_ENV_VAR_2}={VALUE}  # Description
```

**Changed Variables:**
```bash
# OLD:
{OLD_VAR}={OLD_VALUE}

# NEW:
{NEW_VAR}={NEW_VALUE}
```

**Removed Variables:**
- `{REMOVED_VAR}` - No longer needed

### Infrastructure Requirements

- **Java:** {JAVA_VERSION} (verify: `java -version`)
- **Gradle:** {GRADLE_VERSION} (verify: `./gradlew --version`)
- **PostgreSQL:** {PG_VERSION}
- **Redis:** {REDIS_VERSION}
- **Kafka:** {KAFKA_VERSION}

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
git checkout v2.7.1-rc1
git pull origin v2.7.1-rc1
```

**Verification:**
```bash
git log -1  # Should show v2.7.1-rc1 tag
```

---

### Step 4: Update Environment Variables

Edit `.env` or `docker-compose.yml`:

```bash
# Add new variables
export {NEW_ENV_VAR_1}={VALUE}
export {NEW_ENV_VAR_2}={VALUE}

# Update changed variables
export {CHANGED_VAR}={NEW_VALUE}
```

**Verification:**
```bash
env | grep {VAR_PREFIX}
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

**Expected Migration Count:** 830

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
docker images | grep v2.7.1-rc1
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
  -d '{TEST_PATIENT_JSON}'

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
git checkout v2.7.0
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

{INCLUDE_BREAKING_CHANGE_MIGRATIONS_FROM_RELEASE_NOTES}

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

**Issues:** https://github.com/{ORG}/hdim/issues
**Contact:** HDIM Platform Team

---

**Estimated Upgrade Time:** 2-4 hours
**Downtime Required:** 30-60 minutes
