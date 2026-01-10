# HDIM Troubleshooting Guide

## Common Issues and Solutions

---

## Build & Compilation Issues

### Issue: Gradle build fails with "Could not resolve dependencies"

**Symptoms:**
```
> Could not resolve com.healthdata:shared-domain:1.0.0
```

**Solution:**
```bash
# Clean and rebuild
cd backend
./gradlew clean build --refresh-dependencies

# If still failing, check settings.gradle.kts includes all modules
```

### Issue: Java version mismatch

**Symptoms:**
```
Unsupported class file major version 65
```

**Solution:**
```bash
# Verify Java 21
java -version

# Should show: openjdk version "21.x.x"
# If not, install Java 21 and set JAVA_HOME
```

### Issue: Entity migration validation test fails

**Symptoms:**
```
Schema-validation: missing column [tenant_id] in table [patients]
```

**Solution:**
1. Check if Liquibase migration exists for the table
2. Run migrations: `./gradlew bootRun` (applies Liquibase)
3. Verify `ddl-auto: validate` in application.yml
4. See `entity-migration-sync.md` for complete guide

---

## Docker & Container Issues

### Issue: Container fails to start

**Symptoms:**
```
ERROR: for hdim-postgres  Cannot start service postgres: driver failed
```

**Solution:**
```bash
# Check if port is already in use
lsof -i :5435

# Kill existing process or change port in docker-compose.yml
docker compose down -v
docker compose up -d
```

### Issue: Database connection refused

**Symptoms:**
```
org.postgresql.util.PSQLException: Connection refused
```

**Solution:**
```bash
# Check if PostgreSQL container is running
docker compose ps

# Check logs
docker compose logs postgres

# Verify connection settings in application.yml
# POSTGRES_HOST=localhost (not postgres when running locally)
# POSTGRES_PORT=5435
```

### Issue: Redis connection timeout

**Symptoms:**
```
Unable to connect to Redis at localhost:6380
```

**Solution:**
```bash
# Verify Redis is running
docker compose ps redis

# Test connection
docker exec -it hdim-redis redis-cli ping
# Should return: PONG

# Check application.yml redis config
```

---

## Authentication & Authorization Issues

### Issue: 401 Unauthorized

**Symptoms:**
```
HTTP 401: Missing required header: X-Auth-User-Id
```

**Cause:** Request not routed through gateway

**Solution:**
- All requests MUST go through gateway (port 8001)
- Backend services (8084-8110) should NOT be accessed directly
- Correct: `http://localhost:8001/patient/api/v1/patients/123`
- Wrong: `http://localhost:8084/api/v1/patients/123`

### Issue: 403 Forbidden - Tenant access denied

**Symptoms:**
```
Access denied to tenant: TENANT002
```

**Cause:** User not authorized for requested tenant

**Solution:**
```bash
# Check JWT claims include tenant
# X-Auth-Tenant-Ids header must contain requested tenant

# Verify in gateway logs
docker compose logs gateway-service | grep "X-Auth-Tenant-Ids"

# Test with correct tenant
curl -H "X-Tenant-ID: TENANT001" \
     -H "Authorization: Bearer <token>" \
     http://localhost:8001/patient/api/v1/patients/123
```

### Issue: HMAC validation failed

**Symptoms:**
```
Invalid X-Auth-Validated signature
```

**Solution:**
```bash
# Development: Enable dev mode
GATEWAY_AUTH_DEV_MODE=true

# Production: Verify secret matches
GATEWAY_AUTH_SIGNING_SECRET=<same-in-gateway-and-services>
```

---

## Database Issues

### Issue: Liquibase migration fails

**Symptoms:**
```
Validation Failed:
1 change sets check sum mismatch
```

**Cause:** Existing migration was modified

**Solution:**
- NEVER modify applied migrations
- Create a new migration to fix issues
- See `entity-migration-sync.md`

### Issue: Duplicate key violation

**Symptoms:**
```
PSQLException: duplicate key value violates unique constraint
```

**Solution:**
```java
// Check for existing record before insert
Optional<Patient> existing = patientRepository
    .findByFhirIdAndTenant(fhirId, tenantId);

if (existing.isPresent()) {
    // Update instead of insert
}
```

### Issue: Connection pool exhausted

**Symptoms:**
```
HikariPool-1 - Connection is not available
```

**Solution:**
```yaml
# Increase pool size in application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # Increase from 10
      minimum-idle: 5
      connection-timeout: 30000
```

---

## Cache Issues

### Issue: PHI cache TTL exceeds 5 minutes

**Symptoms:**
- HIPAA compliance violation
- Audit findings

**Solution:**
```yaml
# Verify Redis TTL config
spring:
  cache:
    redis:
      time-to-live: 300000  # MUST be <= 300000 (5 minutes)
```

```bash
# Check actual Redis TTL
redis-cli
> TTL patientData::12345
# Should return value <= 300 seconds
```

### Issue: Cache not working

**Symptoms:**
- Database hit on every request
- Slow performance

**Solution:**
```java
// Verify @Cacheable annotation
@Cacheable(value = "patientData", key = "#patientId")
public PatientResponse getPatient(String patientId, String tenantId) {
    // ...
}

// Check Redis is configured in application.yml
spring:
  cache:
    type: redis
```

---

## Multi-Tenant Issues

### Issue: Data leaked across tenants

**Symptoms:**
- User sees data from different tenant
- Security vulnerability

**Solution:**
```java
// ALWAYS filter by tenantId in repository
@Query("SELECT p FROM Patient p WHERE p.id = :id AND p.tenantId = :tenantId")
Optional<Patient> findByIdAndTenant(@Param("id") String id, @Param("tenantId") String tenantId);

// NEVER do this:
@Query("SELECT p FROM Patient p WHERE p.id = :id")  // ❌ Missing tenant filter!
```

### Issue: Tenant filter not applied

**Symptoms:**
```
User from TENANT001 can access TENANT002 data
```

**Solution:**
1. Verify `TrustedTenantAccessFilter` is configured
2. Check `@RequestHeader("X-Tenant-ID")` in controller
3. Add unit test for tenant isolation

---

## FHIR Issues

### Issue: FHIR resource parsing fails

**Symptoms:**
```
ca.uhn.fhir.parser.DataFormatException: Invalid JSON content
```

**Solution:**
```java
// Use HAPI FHIR parser
FhirContext ctx = FhirContext.forR4();
IParser parser = ctx.newJsonParser();
parser.setStripVersionsFromReferences(false);
parser.setPrettyPrint(true);

Patient patient = parser.parseResource(Patient.class, jsonString);
```

### Issue: Value set not found

**Symptoms:**
```
ValueSet with URL 'http://...' not found
```

**Solution:**
```bash
# Load value sets via Liquibase
# See: cql-engine-service/resources/db/changelog/0014-seed-essential-value-sets.xml
```

---

## CQL Evaluation Issues

### Issue: CQL evaluation fails

**Symptoms:**
```
Error evaluating CQL: Unknown identifier 'InDemographic'
```

**Solution:**
1. Verify CQL library is loaded
2. Check FHIR data is available
3. Validate CQL syntax

```bash
# Test CQL evaluation
curl -X POST http://localhost:8081/cql-engine/api/v1/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "cql": "define InDemographic: AgeInYearsAt(start of MeasurementPeriod) >= 18",
    "patientId": "patient-123"
  }'
```

---

## Performance Issues

### Issue: Slow API response times

**Symptoms:**
- Response times > 1 second
- Timeouts

**Diagnosis:**
```bash
# Check Prometheus metrics
curl http://localhost:9090/metrics | grep http_server_requests

# Check database query performance
docker exec -it hdim-postgres psql -U healthdata -d healthdata_qm
SELECT * FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 10;
```

**Solutions:**
1. Add database indexes
2. Enable query caching
3. Optimize N+1 queries
4. Use `@EntityGraph` for eager loading

### Issue: High memory usage

**Symptoms:**
```
OutOfMemoryError: Java heap space
```

**Solution:**
```yaml
# Increase JVM memory in docker-compose.yml
environment:
  JAVA_OPTS: "-Xmx2g -Xms512m"
```

---

## Gateway Issues

### Issue: Request routing fails

**Symptoms:**
```
404 Not Found from gateway
```

**Solution:**
```bash
# Check gateway routing configuration
docker compose logs gateway-service

# Verify service is registered and healthy
curl http://localhost:8001/actuator/health

# Check Kong configuration if using Kong
curl http://localhost:8001/routes
```

### Issue: Rate limiting blocking requests

**Symptoms:**
```
429 Too Many Requests
```

**Solution:**
```yaml
# Adjust rate limits in gateway configuration
rate-limit:
  requests-per-second: 100
  burst-size: 200
```

---

## Kafka Issues

### Issue: Message not consumed

**Symptoms:**
- Events published but not processed
- Lag increasing

**Solution:**
```bash
# Check consumer group
docker exec -it hdim-kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9094 \
  --describe --group quality-measure-consumer

# Check topic
docker exec -it hdim-kafka kafka-topics.sh \
  --bootstrap-server localhost:9094 \
  --describe --topic quality-measure-results
```

### Issue: Kafka broker not available

**Symptoms:**
```
TimeoutException: Failed to update metadata after 60000 ms
```

**Solution:**
```bash
# Verify Kafka is running
docker compose ps kafka

# Check logs
docker compose logs kafka

# Verify connection
docker exec -it hdim-kafka kafka-broker-api-versions.sh \
  --bootstrap-server localhost:9094
```

---

## Testing Issues

### Issue: Integration tests fail with database errors

**Symptoms:**
```
org.testcontainers.containers.ContainerLaunchException
```

**Solution:**
```bash
# Verify Docker is running
docker ps

# Clean up test containers
docker rm -f $(docker ps -aq --filter "name=test")

# Run tests with fresh containers
./gradlew clean test
```

### Issue: Mock not working in unit test

**Symptoms:**
- NullPointerException in test
- Mock returns null

**Solution:**
```java
// Ensure @ExtendWith(MockitoExtension.class)
@ExtendWith(MockitoExtension.class)
class MyServiceTest {

    @Mock
    private MyRepository myRepository;

    @InjectMocks
    private MyService myService;

    @BeforeEach
    void setUp() {
        // Setup mocks
        when(myRepository.findById(any())).thenReturn(Optional.of(testData));
    }
}
```

---

## Logging & Debugging

### Enable Debug Logging

```yaml
# application.yml
logging:
  level:
    com.healthdata: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### View Logs

```bash
# Docker logs
docker compose logs -f quality-measure-service

# Follow logs
docker compose logs -f --tail=100 quality-measure-service

# Grep for errors
docker compose logs quality-measure-service | grep ERROR
```

### Debug SQL Queries

```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
```

---

## Health Checks

### Service Health

```bash
# Check individual service
curl http://localhost:8087/actuator/health

# Check all services
for port in 8081 8084 8085 8086 8087; do
  echo "Port $port:"
  curl -s http://localhost:$port/actuator/health | jq '.status'
done
```

### Database Health

```bash
# PostgreSQL
docker exec hdim-postgres pg_isready -U healthdata

# Redis
docker exec hdim-redis redis-cli ping
```

### Kafka Health

```bash
docker exec hdim-kafka kafka-broker-api-versions.sh \
  --bootstrap-server localhost:9094
```

---

## Emergency Procedures

### Complete System Reset

```bash
# WARNING: Destroys all data!
docker compose down -v
docker system prune -a
docker compose up -d
```

### Backup Database

```bash
# Backup PostgreSQL
docker exec hdim-postgres pg_dump -U healthdata healthdata_qm > backup.sql

# Restore
docker exec -i hdim-postgres psql -U healthdata healthdata_qm < backup.sql
```

### Service Restart

```bash
# Restart single service
docker compose restart quality-measure-service

# Restart all services
docker compose restart
```

---

## Getting Help

### Check Documentation
1. `CLAUDE.md` - Coding guidelines
2. `docs/architecture/SYSTEM_ARCHITECTURE.md` - System design
3. `backend/docs/` - Technical documentation
4. Service-specific README files

### Check Logs
1. Application logs in `logs/` directory
2. Docker logs: `docker compose logs <service>`
3. Audit logs in database

### Common Log Locations
- `/var/log/hdim/` (production)
- `logs/` (development)
- Docker stdout (containerized)

---

## Quick Reference

### Ports
- Gateway: 8001
- CQL Engine: 8081
- Patient: 8084
- FHIR: 8085
- Care Gap: 8086
- Quality Measure: 8087
- PostgreSQL: 5435
- Redis: 6380
- Kafka: 9094
- Prometheus: 9090
- Grafana: 3001

### Test Users
| Username | Password | Role |
|----------|----------|------|
| test_admin | password123 | ADMIN |
| test_evaluator | password123 | EVALUATOR |
| test_viewer | password123 | VIEWER |

### Common Commands
```bash
# Build
./gradlew build

# Run service
./gradlew :modules:services:quality-measure-service:bootRun

# Docker up
docker compose up -d

# View logs
docker compose logs -f <service>

# Health check
curl http://localhost:8087/actuator/health
```
