# Hardened Development Ports Guide

**Created:** November 25, 2025
**Purpose:** Non-standard port configuration for enhanced development security

---

## 🔒 Security Rationale

Using non-standard ports in development provides several security benefits:

1. **Reduces Attack Surface** - Automated scanners target default ports (5432, 6379, 9092, etc.)
2. **Port Conflict Prevention** - Avoids conflicts with other local services and projects
3. **Security Through Obscurity** - Additional layer (not primary security, but helpful)
4. **Professional Development Practice** - Mirrors production security hardening

---

## 🗺️ Port Mapping Table

| Service | Default Port | Hardened Port | Protocol | Access |
|---------|--------------|---------------|----------|--------|
| **PostgreSQL** | 5432 | **6000** | TCP | Database |
| **Redis** | 6379 | **6001** | TCP | Cache |
| **Zookeeper** | 2181 | **6002** | TCP | Kafka Coordination |
| **Kafka** | 9092 | **6003** | TCP | Event Streaming |
| **Kafka (Host)** | 9093 | **6004** | TCP | Host Connections |
| **FHIR Server** | 8080 | **6005** | HTTP | HAPI FHIR API |
| **CQL Engine** | 8081 | **6006** | HTTP | Quality Measures |
| **Quality Measure** | 8087 | **6007** | HTTP | Reports API |
| **FHIR Service** | 8082 | **6008** | HTTP | FHIR Gateway |
| **Patient Service** | 8083 | **6009** | HTTP | Patient API |
| **Angular Frontend** | 4200 | **4200** | HTTP | Clinical Portal |

---

## 🚀 Quick Start

### 1. Start Hardened Stack

```bash
# Stop any existing containers
docker-compose down

# Start hardened stack
docker-compose -f docker-compose.dev-hardened.yml up -d

# View logs
docker-compose -f docker-compose.dev-hardened.yml logs -f

# Check health
docker-compose -f docker-compose.dev-hardened.yml ps
```

### 2. Verify Connectivity

```bash
# PostgreSQL (Port 6000)
docker exec -it healthdata-postgres psql -U healthdata -d healthdata_cql -c "SELECT version();"

# Redis (Port 6001)
docker exec -it healthdata-redis redis-cli -a dev_redis_password ping

# CQL Engine Health (Port 6006)
curl http://localhost:6006/cql-engine/actuator/health | jq

# Quality Measure Health (Port 6007)
curl http://localhost:6007/quality-measure/actuator/health | jq

# FHIR Server (Port 6005)
curl http://localhost:6005/fhir/metadata | jq '.fhirVersion'
```

### 3. Seed Test Data

```bash
# Connect to PostgreSQL on port 6000
docker exec -i healthdata-postgres psql -U healthdata -d healthdata_cql < seed-test-data-for-ux-testing.sql

# Verify data
docker exec -it healthdata-postgres psql -U healthdata -d healthdata_cql -c \
  "SELECT COUNT(*) as test_patients FROM patient WHERE id LIKE 'test-patient-%';"
```

---

## 🔧 Connection Strings

### PostgreSQL

**From Host:**
```bash
psql -h localhost -p 6000 -U healthdata -d healthdata_cql
```

**JDBC URL:**
```
jdbc:postgresql://localhost:6000/healthdata_cql?user=healthdata&password=dev_password
```

**Environment Variable:**
```bash
export DATABASE_URL="postgresql://healthdata:dev_password@localhost:6000/healthdata_cql"
```

**DBeaver/DataGrip:**
- Host: `localhost`
- Port: `6000`
- Database: `healthdata_cql`
- User: `healthdata`
- Password: `dev_password`

### Redis

**From Host:**
```bash
redis-cli -h localhost -p 6001 -a dev_redis_password
```

**Connection String:**
```
redis://:dev_redis_password@localhost:6001
```

**Test Connection:**
```bash
redis-cli -h localhost -p 6001 -a dev_redis_password ping
# Response: PONG
```

### Kafka

**Bootstrap Servers:**
```
localhost:6003
```

**Test Topic List:**
```bash
docker exec -it healthdata-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --list
```

### HTTP Services

**CQL Engine API:**
```bash
# Health check
curl http://localhost:6006/cql-engine/actuator/health

# Metrics
curl http://localhost:6006/cql-engine/actuator/metrics

# API Docs (if Swagger enabled)
open http://localhost:6006/cql-engine/swagger-ui.html
```

**Quality Measure API:**
```bash
# Health check
curl http://localhost:6007/quality-measure/actuator/health

# Patient health overview (requires auth)
curl -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:6007/quality-measure/patient-health/test-patient-001
```

**FHIR Server (HAPI):**
```bash
# Metadata
curl http://localhost:6005/fhir/metadata

# Search patients
curl http://localhost:6005/fhir/Patient

# Get specific patient
curl http://localhost:6005/fhir/Patient/test-patient-001
```

---

## 🔐 Security Credentials

### PostgreSQL
- **User:** `healthdata`
- **Password:** `dev_password`
- **Database:** `healthdata_cql`

### Redis
- **Password:** `dev_redis_password`
- **Note:** Password is REQUIRED (not default Redis behavior)

### JWT
- **Secret:** `dev_jwt_secret_key_for_docker_environment_minimum_256_bits_required_for_hs512_algorithm`
- **Algorithm:** HS512
- **Note:** 256+ bits required for HS512

---

## 📝 Updating Application Configuration

### Frontend (Angular)

Update [apps/clinical-portal/src/app/config/api.config.ts](apps/clinical-portal/src/app/config/api.config.ts):

```typescript
export const API_CONFIG = {
  cqlEngineBaseUrl: 'http://localhost:6006/cql-engine',
  qualityMeasureBaseUrl: 'http://localhost:6007/quality-measure',
  fhirServiceBaseUrl: 'http://localhost:6008/fhir-service',
  patientServiceBaseUrl: 'http://localhost:6009/patient-service',
  fhirServerBaseUrl: 'http://localhost:6005/fhir',
};
```

### Backend Services

**Spring Boot `application-local.yml`:**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:6000/healthdata_cql
    username: healthdata
    password: dev_password

  data:
    redis:
      host: localhost
      port: 6001
      password: dev_redis_password

  kafka:
    bootstrap-servers: localhost:6003

# Service URLs
services:
  cql-engine:
    url: http://localhost:6006/cql-engine
  quality-measure:
    url: http://localhost:6007/quality-measure
  fhir-service:
    url: http://localhost:6008/fhir-service
  fhir-server:
    url: http://localhost:6005/fhir
```

---

## 🧪 Testing with Hardened Ports

### Playwright E2E Tests

Update [apps/clinical-portal-e2e/playwright.config.ts](apps/clinical-portal-e2e/playwright.config.ts):

```typescript
export default defineConfig({
  use: {
    baseURL: 'http://localhost:4200',
  },

  webServer: {
    command: 'npx nx serve clinical-portal',
    port: 4200,
    reuseExistingServer: !process.env.CI,
  },
});
```

**Note:** Frontend still uses port 4200 (standard for Angular dev server).

### Integration Tests

```bash
# Set environment variables for tests
export DATABASE_URL="postgresql://healthdata:dev_password@localhost:6000/healthdata_cql"
export REDIS_URL="redis://:dev_redis_password@localhost:6001"
export KAFKA_BOOTSTRAP_SERVERS="localhost:6003"

# Run integration tests
cd backend
./gradlew test -Dspring.profiles.active=test
```

---

## 🐳 Docker Commands

### Start Services

```bash
# Start all services
docker-compose -f docker-compose.dev-hardened.yml up -d

# Start specific service
docker-compose -f docker-compose.dev-hardened.yml up -d postgres

# Start and follow logs
docker-compose -f docker-compose.dev-hardened.yml up
```

### Stop Services

```bash
# Stop all services
docker-compose -f docker-compose.dev-hardened.yml down

# Stop and remove volumes (CAUTION: deletes data)
docker-compose -f docker-compose.dev-hardened.yml down -v

# Stop specific service
docker-compose -f docker-compose.dev-hardened.yml stop postgres
```

### View Logs

```bash
# All services
docker-compose -f docker-compose.dev-hardened.yml logs -f

# Specific service
docker-compose -f docker-compose.dev-hardened.yml logs -f postgres

# Last 100 lines
docker-compose -f docker-compose.dev-hardened.yml logs --tail=100 postgres
```

### Health Checks

```bash
# Check all service health
docker-compose -f docker-compose.dev-hardened.yml ps

# Detailed health for specific service
docker inspect healthdata-postgres | jq '.[0].State.Health'
```

---

## 🔄 Migration from Default Ports

### Step 1: Export Data (Optional)

```bash
# Backup existing data if needed
docker exec healthdata-postgres pg_dump -U healthdata healthdata_cql > backup.sql
```

### Step 2: Stop Old Stack

```bash
docker-compose down
```

### Step 3: Start Hardened Stack

```bash
docker-compose -f docker-compose.dev-hardened.yml up -d
```

### Step 4: Restore Data (Optional)

```bash
# Restore from backup
docker exec -i healthdata-postgres psql -U healthdata -d healthdata_cql < backup.sql
```

### Step 5: Update Application Config

Update all connection strings in:
- Frontend: `apps/clinical-portal/src/app/config/api.config.ts`
- Backend: `application-local.yml` files
- Test configs: Playwright, integration tests
- Environment variables: `.env` files

---

## 🚨 Troubleshooting

### Port Already in Use

```bash
# Find process using port 6000
lsof -i:6000

# Kill process
kill -9 <PID>

# Or use different port in docker-compose
```

### Cannot Connect to Database

```bash
# Check container is running
docker ps | grep healthdata-postgres

# Check logs
docker logs healthdata-postgres

# Test connection
docker exec -it healthdata-postgres psql -U healthdata -d healthdata_cql -c "SELECT 1;"
```

### Redis Authentication Failed

```bash
# Verify password
docker exec -it healthdata-redis redis-cli -a dev_redis_password ping

# If no password set, check docker-compose environment
docker inspect healthdata-redis | jq '.[0].Config.Cmd'
```

### Service Won't Start

```bash
# Check dependencies
docker-compose -f docker-compose.dev-hardened.yml ps

# View service logs
docker logs healthdata-cql-engine

# Check health status
docker inspect healthdata-cql-engine | jq '.[0].State.Health'
```

---

## 📊 Performance Monitoring

### Database Connections

```sql
-- Check active connections
SELECT count(*) as connections, state
FROM pg_stat_activity
WHERE datname = 'healthdata_cql'
GROUP BY state;

-- Check slow queries
SELECT pid, age(clock_timestamp(), query_start), usename, query
FROM pg_stat_activity
WHERE query != '<IDLE>' AND query NOT ILIKE '%pg_stat_activity%'
ORDER BY query_start desc;
```

### Redis Monitoring

```bash
# Monitor commands in real-time
docker exec -it healthdata-redis redis-cli -a dev_redis_password monitor

# Get stats
docker exec -it healthdata-redis redis-cli -a dev_redis_password info stats

# Check memory usage
docker exec -it healthdata-redis redis-cli -a dev_redis_password info memory
```

### Kafka Topics

```bash
# List topics
docker exec healthdata-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --list

# Describe topic
docker exec healthdata-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic audit-events

# Consumer groups
docker exec healthdata-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --list
```

---

## 🎯 Next Steps

1. **Start Hardened Stack:**
   ```bash
   docker-compose -f docker-compose.dev-hardened.yml up -d
   ```

2. **Seed Test Data:**
   ```bash
   docker exec -i healthdata-postgres psql -U healthdata -d healthdata_cql < seed-test-data-for-ux-testing.sql
   ```

3. **Update Frontend Config:**
   - Edit `apps/clinical-portal/src/app/config/api.config.ts`
   - Change all API URLs to use ports 6005-6009

4. **Run Playwright Tests:**
   ```bash
   cd apps/clinical-portal-e2e
   npx playwright test phase1-improvements-validation.spec.ts
   ```

5. **Verify Phase 1 Improvements:**
   - Care gaps card should display with test data
   - Quick action buttons should be visible
   - Patient search should work with fuzzy matching

---

## 📚 Related Documentation

- [Phase 1 Final Summary](PHASE_1_FINAL_SUMMARY.md)
- [Playwright Test Results](PHASE_1_PLAYWRIGHT_TEST_RESULTS.md)
- [Test Data Seed Script](seed-test-data-for-ux-testing.sql)
- [Docker Compose (Standard Ports)](docker-compose.yml)
- [Docker Compose (Hardened Ports)](docker-compose.dev-hardened.yml)

---

**Security Note:** These ports are for **development only**. Production environments should use:
- Proper network segmentation
- Firewall rules
- VPN/bastion access
- Secrets management (Vault, AWS Secrets Manager)
- TLS/SSL encryption
- Strong authentication (not hardcoded passwords)

---

**Created:** November 25, 2025
**Status:** ✅ Ready for Use
**Tested:** PostgreSQL, Redis, Kafka, All HTTP services
