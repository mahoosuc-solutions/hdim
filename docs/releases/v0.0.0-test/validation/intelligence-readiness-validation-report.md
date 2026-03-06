# Intelligence Readiness Validation Report

## Overview
Validates intelligence deployment gates for event-processing-service:
- Liquibase migrations applied
- Kafka intelligence topics reachable
- Service and intelligence endpoint liveness

## Runtime Inputs
- VERSION: v0.0.0-test
- EVENT_SERVICE_BASE_URL: http://localhost:8083/events
- EVENT_TENANT_ID: tenant-a
- KAFKA_BOOTSTRAP_SERVERS: localhost:9094
- DB_CONTAINER: <none>
- DATABASE_URL: <none>
- CHECK_MIGRATIONS: false
- CHECK_KAFKA_TOPICS: false
- CHECK_ENDPOINTS: false

---
- ⚠️ Skipping migration validation (CHECK_MIGRATIONS=false)
- ⚠️ Skipping Kafka topic validation (CHECK_KAFKA_TOPICS=false)
- ⚠️ Skipping endpoint liveness validation (CHECK_ENDPOINTS=false)

### ✅ Overall Status: PASSED
