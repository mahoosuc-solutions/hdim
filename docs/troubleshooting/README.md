# HDIM Troubleshooting Guide

Central decision tree for diagnosing and resolving issues in the HealthData-in-Motion platform.

**Last Updated**: January 19, 2026
**Status**: Complete troubleshooting index with decision trees

---

## Quick Symptom Finder

Use Ctrl+F (Cmd+F) to search for your symptom:

- "Service won't start"
- "Database connection"
- "Liquibase migration"
- "Docker error"
- "API not responding"
- "Performance slow"
- "Authentication failed"
- "Out of memory"

---

## Decision Tree: "My Service Won't Start"

```
┌─ Is the error in the logs?
│  ├─ YES ─→ [Go to: Analyzing Error Messages]
│  └─ NO  ─→ [Go to: Silent Service Failure]
│
├─ Does the error mention "Liquibase"?
│  ├─ YES ─→ [Go to: Liquibase Errors]
│  └─ NO  ─→ [Go to: Non-Liquibase Startup Errors]
│
├─ Does the error mention "relation already exists"?
│  ├─ YES ─→ [Go to: Database Schema Conflicts]
│  └─ NO  ─→ [Go to: Other Database Errors]
│
└─ Is this a Docker issue?
   ├─ YES ─→ [Go to: Docker Problems]
   └─ NO  ─→ [Go to: Spring Boot Startup Errors]
```

---

## Problem Categories

### 1. Liquibase Migration Errors

**Problem: "Schema-validation: missing table"**

```
ERROR: Schema-validation: missing table [patients]
```

**Diagnosis:**
- Entity exists but migration is missing
- Migration hasn't run yet
- Database is in wrong state

**Solutions:**

1. **Check if migration exists:**
   ```bash
   ls backend/modules/services/YOUR-SERVICE/src/main/resources/db/changelog/
   ```

2. **Create missing migration if needed:**
   - See `backend/docs/LIQUIBASE_DEVELOPMENT_WORKFLOW.md`

3. **Force migration run:**
   ```bash
   # Clear lock if stuck
   ./backend/scripts/clear-liquibase-locks.sh SERVICE_db

   # Restart service
   docker compose down YOUR-SERVICE
   docker compose up -d YOUR-SERVICE
   ```

4. **Last resort - reset and rebuild:**
   ```bash
   # Drop and recreate database
   docker exec healthdata-postgres psql -U healthdata -c "DROP DATABASE IF EXISTS SERVICE_db;"
   docker exec healthdata-postgres psql -U healthdata -c "CREATE DATABASE SERVICE_db;"

   # Rebuild and restart
   docker compose build --no-cache YOUR-SERVICE
   docker compose up -d YOUR-SERVICE
   ```

---

**Problem: "Liquibase update failed"**

```
ERROR: Liquibase failed during execution.
Migration file validation failed.
```

**Diagnosis:**
- XML schema validation error
- Invalid Liquibase syntax
- Bad database state

**Solutions:**

1. **Validate XML schema:**
   ```bash
   # Check changeset syntax
   grep -n "changeset\|createTable\|addColumn" \
     backend/modules/services/YOUR-SERVICE/src/main/resources/db/changelog/*.xml
   ```

2. **Look for common XML errors:**
   - Invalid `<preConditions>` placement (invalid in 4.20)
   - Unclosed XML tags
   - Invalid attribute combinations

3. **Check migration file structure:**
   - See example in `backend/docs/LIQUIBASE_DEVELOPMENT_WORKFLOW.md`

4. **Validate changeset ID is unique:**
   ```bash
   grep -h "changeSet id=" backend/modules/services/YOUR-SERVICE/src/main/resources/db/changelog/*.xml | sort | uniq -d
   ```
   If any output, you have duplicate IDs - fix them.

5. **Run validation test:**
   ```bash
   ./gradlew :modules:services:YOUR-SERVICE:test --tests "*EntityMigrationValidationTest"
   ```

---

**Problem: "databasechangeloglock is locked"**

```
ERROR: Could not acquire change log lock.
The database is locked by another process.
```

**Diagnosis:**
- Previous migration crashed
- Lock wasn't released
- Another service is running migrations

**Solutions:**

1. **Check lock status:**
   ```bash
   ./backend/scripts/clear-liquibase-locks.sh
   ```

2. **Clear specific lock:**
   ```bash
   ./backend/scripts/clear-liquibase-locks.sh SERVICE_name SERVICE_db
   ```

3. **Manual recovery (last resort):**
   ```bash
   docker exec healthdata-postgres psql -U healthdata -d SERVICE_db << EOF
   UPDATE databasechangeloglock SET LOCKED=false WHERE ID=1;
   EOF
   ```

4. **Verify it worked:**
   ```bash
   docker exec healthdata-postgres psql -U healthdata -d SERVICE_db \
     -c "SELECT * FROM databasechangeloglock;"
   ```

---

### 2. Database Connection Errors

**Problem: "Cannot connect to PostgreSQL"**

```
ERROR: Failed to obtain JDBC Connection
Connection refused: localhost:5435
```

**Diagnosis:**
- PostgreSQL container not running
- Port mismapped
- Network issue

**Solutions:**

1. **Check if PostgreSQL is running:**
   ```bash
   docker ps | grep postgres
   ```

2. **Start PostgreSQL:**
   ```bash
   docker compose up -d postgres
   ```

3. **Wait for PostgreSQL to be ready:**
   ```bash
   docker exec healthdata-postgres pg_isready -U healthdata
   # Output: accepting connections
   ```

4. **Check connection details:**
   - Host: localhost (or docker hostname if in container)
   - Port: 5435 (mapped from 5432)
   - Database: Check application.yml
   - User: healthdata (default)
   - Password: healthdata_password (default)

5. **Test connection manually:**
   ```bash
   docker exec -it healthdata-postgres psql -U healthdata -d postgres -c "SELECT 1;"
   ```

6. **If still failing, check environment variables:**
   ```bash
   docker exec YOUR-SERVICE printenv | grep -i postgres
   ```

---

**Problem: "Too many connections to database"**

```
ERROR: FATAL: sorry, too many clients already
```

**Diagnosis:**
- Connection pool exhausted
- Services not releasing connections
- Database max connections too low

**Solutions:**

1. **Check active connections:**
   ```bash
   docker exec healthdata-postgres psql -U healthdata -d postgres << EOF
   SELECT datname, count(*) as connections
   FROM pg_stat_activity
   GROUP BY datname
   ORDER BY connections DESC;
   EOF
   ```

2. **Kill idle connections:**
   ```bash
   docker exec healthdata-postgres psql -U healthdata -d postgres << EOF
   SELECT pg_terminate_backend(pid) FROM pg_stat_activity
   WHERE state = 'idle' AND query_start < NOW() - INTERVAL '5 minutes';
   EOF
   ```

3. **Restart all services to reset pools:**
   ```bash
   docker compose restart
   ```

4. **Check connection pool settings:**
   - Look in application.yml for HikariCP configuration
   - Default: 10 connections per service
   - See `DATABASE_CONFIG_ADOPTION_GUIDE.md` for tuning

---

### 3. Docker Problems

**Problem: "Docker build fails"**

```
ERROR: failed to solve
```

**Diagnosis:**
- Stale layer cache
- Missing base image
- Gradle build failure
- Incorrect Dockerfile

**Solutions:**

1. **Force full rebuild without cache:**
   ```bash
   docker compose build --no-cache YOUR-SERVICE
   ```

2. **Clear all Docker cache:**
   ```bash
   docker system prune -a --volumes
   ```

3. **Check Gradle build succeeds locally:**
   ```bash
   cd backend
   ./gradlew :modules:services:YOUR-SERVICE:build -x test
   ```

4. **Verify Dockerfile exists:**
   ```bash
   ls backend/modules/services/YOUR-SERVICE/Dockerfile
   ```

5. **Check base image availability:**
   ```bash
   docker pull openjdk:21-jdk-slim
   ```

---

**Problem: "Container exits immediately"**

```
docker ps shows the container status as "Exited (1)"
```

**Diagnosis:**
- Application crashed on startup
- Misconfiguration
- Missing environment variables

**Solutions:**

1. **Check container logs:**
   ```bash
   docker compose logs --tail=50 YOUR-SERVICE
   ```

2. **Look for startup errors:**
   - Liquibase errors
   - Missing environment variables
   - Port conflicts
   - Memory limits

3. **Restart with foreground logging:**
   ```bash
   docker compose up YOUR-SERVICE
   # Watch logs in real-time, Ctrl+C to stop
   ```

4. **Increase memory if needed:**
   ```yaml
   # docker-compose.yml
   services:
     YOUR-SERVICE:
       environment:
         JAVA_OPTS: "-Xms512m -Xmx1024m"
   ```

5. **Check environment variables:**
   ```bash
   docker compose exec YOUR-SERVICE printenv | head -20
   ```

---

**Problem: "Port already in use"**

```
ERROR: Port 8087 is already in use
```

**Diagnosis:**
- Service already running on same port
- Process on host using port
- Docker port mismapping

**Solutions:**

1. **Find process using port:**
   ```bash
   lsof -i :8087  # On Mac/Linux
   netstat -ano | findstr :8087  # On Windows
   ```

2. **Kill process (if it's old):**
   ```bash
   kill -9 PID
   ```

3. **Check service configuration:**
   - Verify port in service application.yml: `server.port: 8087`
   - Verify docker-compose.yml port mapping: `"8087:8087"`

4. **Use different port temporarily:**
   ```bash
   docker compose down YOUR-SERVICE
   # Modify docker-compose.yml to use different port
   docker compose up -d YOUR-SERVICE
   ```

---

### 4. API and Network Errors

**Problem: "API endpoint returns 404"**

```
HTTP 404 Not Found
```

**Diagnosis:**
- Service not running
- Wrong endpoint URL
- Context path mismapped
- API path changed

**Solutions:**

1. **Verify service is running:**
   ```bash
   docker ps | grep SERVICE
   ```

2. **Verify service started successfully:**
   ```bash
   docker compose logs SERVICE | grep -i "started\|failed\|error"
   ```

3. **Check service port and context path:**
   ```bash
   # From SERVICE_CATALOG.md
   # Example: quality-measure-service
   # Port: 8087
   # Context: /quality-measure
   # URL: http://localhost:8087/quality-measure/api/...
   ```

4. **Verify API endpoint exists:**
   - Check service README: `backend/modules/services/SERVICE/README.md`
   - Check OpenAPI spec: `docs/api/SERVICE-openapi.json`

5. **Check gateway is running:**
   ```bash
   docker ps | grep gateway
   docker compose logs gateway
   ```

---

**Problem: "API returns 401 Unauthorized"**

```
HTTP 401 Unauthorized
```

**Diagnosis:**
- Missing authentication token
- Invalid token
- Expired token
- Missing required headers

**Solutions:**

1. **Verify authentication is configured:**
   ```bash
   # Check if service uses gateway-trust auth
   grep -r "TrustedHeaderAuthFilter" backend/modules/services/YOUR-SERVICE/
   ```

2. **Get valid token:**
   ```bash
   # Request token from gateway
   curl -X POST http://localhost:8001/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"test_admin","password":"password123"}'
   ```

3. **Include token in request:**
   ```bash
   curl http://localhost:8087/quality-measure/api/v1/measures \
     -H "Authorization: Bearer YOUR_TOKEN_HERE"
   ```

4. **Check token expiration:**
   - Default: 15 minutes
   - See gateway configuration for TTL

5. **For development, use test users:**
   ```
   Username: test_admin
   Password: password123
   Role: ADMIN
   ```

---

**Problem: "API returns 403 Forbidden"**

```
HTTP 403 Forbidden - Access Denied
```

**Diagnosis:**
- Correct authentication but insufficient permissions
- Wrong role
- Tenant access denied
- Resource ownership issue

**Solutions:**

1. **Check user roles:**
   ```bash
   # Token should contain roles
   curl http://localhost:8001/auth/me \
     -H "Authorization: Bearer TOKEN"
   ```

2. **Verify endpoint permission:**
   ```bash
   # Check @PreAuthorize annotations in controller
   grep -A2 "@PreAuthorize" \
     backend/modules/services/YOUR-SERVICE/src/main/java/com/healthdata/*/api/*.java
   ```

3. **Use appropriate role:**
   - ADMIN: Full access
   - EVALUATOR: Can run evaluations
   - ANALYST: Read-only analytics
   - VIEWER: Read-only basic access

4. **Check tenant access:**
   - Verify X-Tenant-ID header matches user's authorized tenants
   - See `backend/docs/GATEWAY_TRUST_ARCHITECTURE.md`

---

### 5. Performance Issues

**Problem: "Application is slow"**

```
Response times > 5 seconds
High CPU or memory usage
```

**Diagnosis:**
- Database slow queries
- Missing indexes
- Cache disabled
- Inefficient code
- Insufficient resources

**Solutions:**

1. **Check database slow queries:**
   ```bash
   docker exec healthdata-postgres psql -U healthdata -d YOUR_db << EOF
   -- Find slow queries
   SELECT query, mean_exec_time, calls
   FROM pg_stat_statements
   WHERE mean_exec_time > 1000  -- > 1 second
   ORDER BY mean_exec_time DESC
   LIMIT 10;
   EOF
   ```

2. **Check if indexes exist:**
   ```bash
   docker exec healthdata-postgres psql -U healthdata -d YOUR_db << EOF
   -- List indexes
   SELECT * FROM pg_indexes
   WHERE tablename = 'your_table';
   EOF
   ```

3. **Create missing indexes:**
   - Add to Liquibase migration
   - See migration examples in `LIQUIBASE_DEVELOPMENT_WORKFLOW.md`

4. **Check Redis cache:**
   ```bash
   docker exec healthdata-redis redis-cli INFO stats
   docker exec healthdata-redis redis-cli DBSIZE
   ```

5. **Clear cache if needed:**
   ```bash
   docker exec healthdata-redis redis-cli FLUSHALL
   docker compose restart YOUR-SERVICE  # Rebuild cache
   ```

6. **Monitor with Grafana:**
   - Access at: http://localhost:3001
   - Check CPU, memory, request times
   - See `operations/MONITORING.md`

7. **Check resource limits:**
   ```bash
   docker stats
   # Look for % usage
   ```

---

**Problem: "Out of memory"**

```
ERROR: java.lang.OutOfMemoryError: Java heap space
```

**Diagnosis:**
- Heap size too small
- Memory leak
- Too much data in memory

**Solutions:**

1. **Increase JVM heap size:**
   ```yaml
   # docker-compose.yml
   services:
     YOUR-SERVICE:
       environment:
         JAVA_OPTS: "-Xms512m -Xmx2048m"  # Increase -Xmx
   ```

2. **Identify memory leak:**
   ```bash
   # Capture heap dump
   docker exec YOUR-SERVICE jmap -dump:live,format=b,file=/heap.bin 1

   # Analyze offline (requires eclipse MAT or similar)
   ```

3. **Optimize query results:**
   - Add pagination to list endpoints
   - Use database cursors instead of loading all
   - Implement lazy loading for related entities

4. **Monitor memory in Grafana:**
   - Dashboard: JVM Memory
   - Watch for consistent growth (leak indicator)

---

### 6. Authentication & Authorization Errors

**Problem: "JWT token validation failed"**

```
ERROR: Invalid JWT token
Unable to extract claims from token
```

**Diagnosis:**
- Token corrupted
- Token signed with wrong key
- Token expired
- Token from different environment

**Solutions:**

1. **Verify gateway is running:**
   ```bash
   docker ps | grep gateway
   docker compose logs gateway | tail -20
   ```

2. **Get fresh token:**
   ```bash
   curl -X POST http://localhost:8001/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"test_admin","password":"password123"}'
   ```

3. **Check token signing key configuration:**
   ```bash
   docker compose exec gateway printenv | grep JWT
   ```

4. **Verify token not expired:**
   ```bash
   # Decode token (base64 decode middle section)
   # Check "exp" claim
   ```

---

**Problem: "Tenant access denied"**

```
ERROR: User does not have access to this tenant
```

**Diagnosis:**
- User not assigned to tenant
- X-Tenant-ID header missing or wrong
- Gateway not validating tenant access

**Solutions:**

1. **Verify X-Tenant-ID header:**
   ```bash
   curl http://localhost:8087/quality-measure/api/v1/measures \
     -H "Authorization: Bearer TOKEN" \
     -H "X-Tenant-ID: TENANT_ID"
   ```

2. **Check user's authorized tenants:**
   ```bash
   # Ask gateway for user info
   curl http://localhost:8001/auth/me \
     -H "Authorization: Bearer TOKEN" | jq .tenant_ids
   ```

3. **Verify database user-tenant mapping:**
   ```bash
   docker exec healthdata-postgres psql -U healthdata -d gateway_db << EOF
   SELECT * FROM user_tenants WHERE user_id = 'USER_ID';
   EOF
   ```

---

### 7. Data Issues

**Problem: "No data returned from API"**

```
Empty result set {}
```

**Diagnosis:**
- No data in database
- Data filtered out (tenant, permissions)
- Query issue
- Caching issue

**Solutions:**

1. **Verify data exists:**
   ```bash
   docker exec healthdata-postgres psql -U healthdata -d TENANT_db << EOF
   SELECT COUNT(*) FROM table_name;
   EOF
   ```

2. **Check if filtered by tenant:**
   ```bash
   # Include X-Tenant-ID header
   curl http://localhost:8087/api/v1/endpoint \
     -H "X-Tenant-ID: TENANT_ID" \
     -H "Authorization: Bearer TOKEN"
   ```

3. **Check cache:**
   ```bash
   docker exec healthdata-redis redis-cli KEYS "*"
   ```

4. **Clear cache and retry:**
   ```bash
   docker exec healthdata-redis redis-cli FLUSHALL
   # Retry request
   ```

5. **Check database query directly:**
   ```bash
   docker exec healthdata-postgres psql -U healthdata -d TENANT_db << EOF
   -- Run same query service uses
   SELECT * FROM patients WHERE tenant_id = 'TENANT_ID' LIMIT 10;
   EOF
   ```

---

### 8. Kafka and Event Streaming Issues

**Problem: "Kafka broker not available"**

```
ERROR: Connection refused for topic: patients
```

**Diagnosis:**
- Kafka not running
- Zookeeper not running
- Network issue

**Solutions:**

1. **Check Kafka and Zookeeper are running:**
   ```bash
   docker ps | grep -E "kafka|zookeeper"
   ```

2. **Start if needed:**
   ```bash
   docker compose up -d zookeeper kafka
   ```

3. **Wait for Kafka to be ready:**
   ```bash
   docker compose logs kafka | grep -i "started"
   ```

4. **Test Kafka connectivity:**
   ```bash
   docker exec healthdata-kafka kafka-broker-api-versions.sh --bootstrap-server localhost:9092
   ```

5. **Check service Kafka configuration:**
   ```bash
   docker compose exec YOUR-SERVICE printenv | grep KAFKA
   ```

---

**Problem: "Message not being consumed"**

```
Message sent to Kafka but not processed by consumer service
```

**Diagnosis:**
- Consumer not running
- Consumer not subscribed to topic
- Message format mismatch
- Consumer lag

**Solutions:**

1. **Verify consumer service is running:**
   ```bash
   docker ps | grep SERVICE
   docker compose logs SERVICE | grep -i "subscribed\|listening"
   ```

2. **Check consumer group:**
   ```bash
   docker exec healthdata-kafka kafka-consumer-groups.sh \
     --bootstrap-server localhost:9092 \
     --list
   ```

3. **Check consumer lag:**
   ```bash
   docker exec healthdata-kafka kafka-consumer-groups.sh \
     --bootstrap-server localhost:9092 \
     --group YOUR_GROUP \
     --describe
   ```

4. **Check topic exists:**
   ```bash
   docker exec healthdata-kafka kafka-topics.sh \
     --bootstrap-server localhost:9092 \
     --list
   ```

---

## Advanced Troubleshooting

### Checking Service Health

```bash
# Check if service is responsive
curl http://localhost:SERVICE_PORT/actuator/health

# Output should show:
# {"status":"UP","components":{...}}
```

### Viewing Complete Logs

```bash
# Last 100 lines
docker compose logs --tail=100 YOUR-SERVICE

# Follow logs in real-time
docker compose logs -f YOUR-SERVICE

# From specific time
docker compose logs --since 10m YOUR-SERVICE

# Search logs for errors
docker compose logs YOUR-SERVICE | grep -i error
```

### Checking Environment Variables

```bash
# All variables in service
docker compose exec YOUR-SERVICE printenv | sort

# Search for specific variable
docker compose exec YOUR-SERVICE printenv | grep DATABASE
```

### Database Inspection

```bash
# Connect to database
docker exec -it healthdata-postgres psql -U healthdata -d SERVICE_db

# List tables
\dt

# Describe table
\d table_name

# Show create table
pg_dump -U healthdata -d SERVICE_db --table=table_name --schema-only

# Exit
\q
```

### Distributed Tracing

For request tracing across services:

```bash
# Access Jaeger at http://localhost:16686

# Query examples:
# - Find slow requests: service=SERVICE duration>5s
# - Find errors: service=SERVICE error=true
# - Find by operation: operation.name=GET.*/patients
```

---

## Getting More Help

| Issue Type                    | Resource                                        |
|-------------------------------|------------------------------------------------|
| Liquibase configuration       | `backend/docs/LIQUIBASE_DEVELOPMENT_WORKFLOW.md` |
| Database architecture         | `docs/architecture/database/`                   |
| Service-specific issues       | `backend/modules/services/SERVICE/README.md`    |
| API specification             | `docs/api/SERVICE-openapi.json`                 |
| Deployment procedures         | `docs/deployment/`                              |
| Operations & monitoring       | `docs/operations/`                              |
| Authentication architecture   | `backend/docs/GATEWAY_TRUST_ARCHITECTURE.md`    |
| Distributed tracing           | `backend/docs/DISTRIBUTED_TRACING_GUIDE.md`     |
| Database configuration        | `backend/docs/DATABASE_CONFIG_ADOPTION_GUIDE.md` |
| Service catalog               | `docs/services/SERVICE_CATALOG.md`              |
| General documentation         | `docs/README.md`                                |

---

**Last Updated**: January 19, 2026
**Maintained by**: HDIM Platform Team
**Status**: Complete - Production-ready troubleshooting guide with decision trees
