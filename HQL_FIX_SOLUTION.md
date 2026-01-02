# HealthData in Motion - HQL Error Resolution & Platform Startup Guide

## Problem Summary
The platform was failing to start due to an HQL/JPA query validation error in the `NotificationHistoryRepository`. The specific issue was with the `getAverageDeliveryTimeSeconds` method which used PostgreSQL-specific syntax that wasn't compatible with HQL.

## Root Cause Analysis

### Issue Details
- **Location**: `NotificationHistoryRepository.getAverageDeliveryTimeSeconds()`
- **Error**: Query validation failed for EXTRACT(EPOCH FROM ...) function
- **Reason**: The query used PostgreSQL-specific syntax without marking it as a native query

### Original Problematic Query
```java
@Query("""
    SELECT AVG(EXTRACT(EPOCH FROM (n.deliveredAt - n.sentAt)))
    FROM NotificationHistoryEntity n
    WHERE n.channel = :channel
      AND n.deliveredAt IS NOT NULL
      AND n.sentAt BETWEEN :startDate AND :endDate
    """)
```

## Solution Applied

### Fix Implementation
Changed the query to use native SQL with proper table/column names:

```java
@Query(value = """
    SELECT AVG(EXTRACT(EPOCH FROM (n.delivered_at - n.sent_at)))
    FROM notification_history n
    WHERE n.channel = :channel
      AND n.delivered_at IS NOT NULL
      AND n.sent_at BETWEEN :startDate AND :endDate
    """, nativeQuery = true)
```

### Key Changes
1. Added `value =` and `nativeQuery = true` to mark as native SQL
2. Changed entity/property names to table/column names:
   - `NotificationHistoryEntity` → `notification_history`
   - `deliveredAt` → `delivered_at`
   - `sentAt` → `sent_at`

## Steps to Resolve and Start Platform

### 1. Fix the Repository
```bash
# Edit the repository file
vi backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/persistence/NotificationHistoryRepository.java

# Apply the fix shown above
```

### 2. Rebuild the Service
```bash
cd backend
./gradlew :modules:services:quality-measure-service:build -x test
```

### 3. Rebuild Docker Image
```bash
cd ..
docker compose build quality-measure-service
```

### 4. Restart Services
```bash
docker compose down
./start-platform.sh --build
```

## Verification Steps

### Check Service Health
```bash
# Quality Measure Service (note the context path)
curl http://localhost:8087/quality-measure/actuator/health | jq '.'

# Other services
curl http://localhost:8081/actuator/health | jq '.'  # FHIR
curl http://localhost:8086/actuator/health | jq '.'  # CQL Engine
curl http://localhost:8080/actuator/health | jq '.'  # Gateway
```

### Check Logs
```bash
docker logs healthdata-quality-measure-service --tail 50
```

### Expected Output
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

## Best Practices for Future Development

### 1. Query Annotations
- Use `@Query` with HQL for portable queries
- Use `@Query(nativeQuery = true)` for database-specific features
- Always use snake_case for native queries, camelCase for HQL

### 2. Testing Recommendations
- Test repository methods before deployment
- Use @DataJpaTest for repository testing
- Validate queries work with actual database

### 3. Common Pitfalls to Avoid
- Mixing HQL and SQL syntax
- Using database functions without native query flag
- Incorrect entity/table name references

## Additional Configuration Notes

### Service Context Paths
The quality-measure-service uses a context path:
- Base URL: `http://localhost:8087/quality-measure`
- Health: `http://localhost:8087/quality-measure/actuator/health`
- APIs: `http://localhost:8087/quality-measure/api/...`

### Docker Compose Updates
- Java version: Updated to `eclipse-temurin:21-jdk-alpine`
- Health checks: Adjusted for less strict Kafka dependency
- Multiple databases: Each service has its own database

## Quick Commands Reference

```bash
# Full platform start
./start-platform.sh --build

# Start with frontend
./start-platform.sh --build --with-frontend

# Diagnose issues
./diagnose-issues.sh

# View all service statuses
docker compose ps

# Check all health endpoints
for port in 8080 8081 8086 8087; do
  echo "Port $port: $(curl -s http://localhost:$port/actuator/health 2>/dev/null | jq -r '.status' || echo 'N/A')"
done

# Clean restart
docker compose down -v
./start-platform.sh --build
```

## Result
✅ Platform is now fully operational with all services running successfully:
- PostgreSQL with multiple databases
- Redis cache
- Kafka messaging
- All microservices healthy
- Proper service communication established