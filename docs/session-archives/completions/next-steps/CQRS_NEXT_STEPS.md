# CQRS Event-Driven Architecture - Next Steps (Phase 7+)

**Status**: All 4 CQRS services complete and ready for Docker integration
**Completion Time**: Estimated 2-4 hours for full integration and testing

---

## Quick Summary of Completed Services

| Service | Port | Database | Status |
|---------|------|----------|--------|
| patient-event-service | 8110 | patient_event_db | ✅ Complete |
| care-gap-event-service | 8111 | care_gap_event_db | ✅ Complete |
| quality-measure-event-service | 8112 | quality_event_db | ✅ Complete |
| clinical-workflow-event-service | 8113 | clinical_workflow_event_db | ✅ Complete |

---

## Phase 7: Docker Integration (Next)

### Step 1: Update docker-compose.yml

Add the following service definitions:

```yaml
# Clinical Workflow Event Service (CQRS Read Model)
clinical-workflow-event-service:
  build:
    context: .
    dockerfile: backend/modules/services/clinical-workflow-event-service/Dockerfile
  container_name: hdim-clinical-workflow-event-service
  ports:
    - "8113:8113"
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5435/clinical_workflow_event_db
    SPRING_DATASOURCE_USERNAME: healthdata
    SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:-healthdata}
    KAFKA_BOOTSTRAP_SERVERS: kafka:9094
    SERVER_PORT: 8113
    SPRING_PROFILES_ACTIVE: docker
  depends_on:
    postgres:
      condition: service_healthy
    kafka:
      condition: service_healthy
  networks:
    - hdim-network
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8113/clinical-workflow-event/api/v1/workflow-projections/health"]
    interval: 30s
    timeout: 10s
    retries: 3
    start_period: 40s

# Similarly for other 3 event services:
# patient-event-service (8110)
# care-gap-event-service (8111)
# quality-measure-event-service (8112)
```

### Step 2: Update PostgreSQL Initialization Script

Add database creation for event services in `docker/postgres/init-multi-db.sh`:

```bash
# Create event service databases
createdb -h localhost -U healthdata patient_event_db
createdb -h localhost -U healthdata care_gap_event_db
createdb -h localhost -U healthdata quality_event_db
createdb -h localhost -U healthdata clinical_workflow_event_db

# Grant privileges
psql -h localhost -U healthdata <<EOF
  GRANT ALL PRIVILEGES ON DATABASE patient_event_db TO healthdata;
  GRANT ALL PRIVILEGES ON DATABASE care_gap_event_db TO healthdata;
  GRANT ALL PRIVILEGES ON DATABASE quality_event_db TO healthdata;
  GRANT ALL PRIVILEGES ON DATABASE clinical_workflow_event_db TO healthdata;
EOF
```

### Step 3: Create Kafka Topics

```bash
# Run inside Kafka container:
kafka-topics.sh --create \
  --bootstrap-server localhost:9094 \
  --topic patient.created \
  --partitions 3 \
  --replication-factor 1

kafka-topics.sh --create \
  --bootstrap-server localhost:9094 \
  --topic patient.updated \
  --partitions 3 \
  --replication-factor 1

# ... (full list of topics below)
```

#### Complete Kafka Topics List

**Patient Events**:
- patient.created
- patient.updated
- patient.status.changed
- risk-assessment.updated
- mental-health.updated
- clinical-alert.triggered
- clinical-alert.resolved
- care-gap.identified
- care-gap.closed

**Care Gap Events**:
- care-gap.identified
- care-gap.closed
- care-gap.auto-closed
- care-gap.priority.changed
- care-gap.waived
- care-gap.assigned
- care-gap.due-date-updated

**Quality Measure Events**:
- measure.evaluated
- measure.score.updated
- measure.compliance.changed
- measure.numerator.updated
- measure.denominator.updated
- measure.exclusion.updated

**Workflow Events**:
- workflow.started
- workflow.assigned
- workflow.reassigned
- workflow.progress.updated
- workflow.completed
- workflow.cancelled
- workflow.review.required
- workflow.blocking.issue

### Step 4: Docker Compose Build & Test

```bash
# Build all services (will compile Java source code)
docker compose build

# Start core services first
docker compose --profile core up -d

# Check PostgreSQL is ready
docker compose exec postgres psql -U healthdata -l

# Check Kafka is ready
docker compose exec kafka kafka-topics.sh --list --bootstrap-server localhost:9094

# Start event services
docker compose up -d patient-event-service care-gap-event-service \
                     quality-measure-event-service clinical-workflow-event-service

# Monitor logs
docker compose logs -f clinical-workflow-event-service
```

---

## Phase 8: Integration Testing

### Step 1: Verify Service Health

```bash
# Test patient-event-service health
curl http://localhost:8110/patient-event/api/v1/patient-projections/health

# Test care-gap-event-service health
curl http://localhost:8111/care-gap-event/api/v1/care-gap-projections/health

# Test quality-measure-event-service health
curl http://localhost:8112/quality-measure-event/api/v1/measure-projections/health

# Test clinical-workflow-event-service health
curl http://localhost:8113/clinical-workflow-event/api/v1/workflow-projections/health
```

Expected response:
```json
"Clinical workflow event service is healthy"
```

### Step 2: Test Event Publishing

From patient-service or care-gap-service, publish a test event:

```bash
# Example: Publish patient.created event
curl -X POST http://localhost:8001/api/v1/events \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: demo-tenant" \
  -d '{
    "eventType": "patient.created",
    "patientId": "550e8400-e29b-41d4-a716-446655440000",
    "tenantId": "demo-tenant",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

### Step 3: Verify Projection Updates

```bash
# Wait 1-2 seconds for eventual consistency

# Query patient projection (should exist)
curl http://localhost:8110/patient-event/api/v1/patient-projections/550e8400-e29b-41d4-a716-446655440000 \
  -H "X-Tenant-ID: demo-tenant"

# Expected response:
{
  "id": "...",
  "tenantId": "demo-tenant",
  "patientId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ACTIVE",
  "firstName": "John",
  "lastName": "Doe",
  "activeAlertsCount": 0,
  "openCareGapsCount": 0
}
```

### Step 4: Load Testing

```bash
# Using Apache Bench to test query performance:
ab -n 1000 -c 10 \
  -H "X-Tenant-ID: demo-tenant" \
  http://localhost:8113/clinical-workflow-event/api/v1/workflow-projections/stats

# Expected: Response times < 100ms
# Typical: 20-50ms for well-indexed queries
```

---

## Phase 9: Production Hardening

### Security Updates

1. **Enable Kafka SSL/TLS**:
```yaml
spring:
  kafka:
    security:
      protocol: SSL
    ssl:
      key-store-location: file:/secrets/kafka-keystore.jks
      key-store-password: ${KAFKA_SSL_KEYSTORE_PASSWORD}
```

2. **Secure Redis Connection**:
```yaml
spring:
  redis:
    ssl: true
    host: redis.internal
    password: ${REDIS_PASSWORD}
```

3. **Configure Database SSL**:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5435/clinical_workflow_event_db?sslmode=require
```

### Monitoring Setup

1. **Prometheus Metrics**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

2. **Grafana Dashboards**:
- Create dashboard for event lag (publishing → projection update time)
- Monitor Kafka consumer lag per service
- Track database query performance metrics
- Alert on eventual consistency delays > 5 seconds

3. **Distributed Tracing**:
```yaml
management:
  tracing:
    sampling:
      probability: 0.1  # 10% sampling in production
```

### Disaster Recovery

1. **Event Log Backup**:
```bash
# Kafka backup strategy
# Retention: 30 days for audit trail
kafka-configs.sh --bootstrap-server kafka:9094 \
  --alter \
  --entity-type topics \
  --entity-name patient.created \
  --add-config retention.ms=2592000000
```

2. **Projection Rebuild Procedure**:
```bash
# If projections become corrupted:
# 1. Truncate projection tables
docker compose exec postgres psql -U healthdata -d patient_event_db \
  -c "TRUNCATE TABLE patient_projections;"

# 2. Reset Kafka consumer group offset to earliest
kafka-consumer-groups.sh --bootstrap-server kafka:9094 \
  --group patient-event-service \
  --reset-offsets \
  --to-earliest \
  --execute \
  --all-topics

# 3. Restart event service to replay all events
docker compose restart patient-event-service

# 4. Monitor logs until catch-up
docker compose logs -f patient-event-service | grep "Successfully processed"
```

---

## Troubleshooting Guide

### Service Won't Start: Table Already Exists

**Symptom**:
```
org.postgresql.util.PSQLException: relation "workflow_projections" already exists
```

**Solution**:
- Liquibase migration includes `<preConditions onFail="MARK_RAN">`
- This error should NOT occur, but if it does:
  ```bash
  docker compose exec postgres psql -U healthdata -d clinical_workflow_event_db \
    -c "DROP TABLE IF EXISTS workflow_projections CASCADE;"
  docker compose restart clinical-workflow-event-service
  ```

### Service Won't Connect to Kafka

**Symptom**:
```
java.util.concurrent.ExecutionException: org.apache.kafka.common.errors.UnknownHostException: kafka
```

**Solution**:
- Verify Kafka container is running: `docker compose ps kafka`
- Check Docker network: `docker network inspect hdim-network`
- Verify bootstrap server: `KAFKA_BOOTSTRAP_SERVERS: kafka:9094` (not localhost)

### Projections Not Updating

**Symptom**:
```
Query returns empty results after publishing event
```

**Solution**:
1. Check consumer group lag:
   ```bash
   kafka-consumer-groups.sh --bootstrap-server localhost:9094 \
     --group clinical-workflow-event-service \
     --describe
   ```

2. Check consumer logs:
   ```bash
   docker compose logs clinical-workflow-event-service | grep "Processing workflow"
   ```

3. Verify Kafka topic has messages:
   ```bash
   kafka-console-consumer.sh --bootstrap-server localhost:9094 \
     --topic workflow.started \
     --from-beginning \
     --max-messages 5
   ```

### Performance Issues

**Symptom**: Queries taking > 500ms

**Solution**:
1. Check database indexes exist:
   ```bash
   docker compose exec postgres psql -U healthdata -d clinical_workflow_event_db \
     -c "\di"  # List indexes
   ```

2. Run EXPLAIN ANALYZE:
   ```bash
   docker compose exec postgres psql -U healthdata -d clinical_workflow_event_db << EOF
   EXPLAIN ANALYZE
   SELECT * FROM workflow_projections
   WHERE tenant_id = 'demo-tenant' AND status = 'IN_PROGRESS';
   EOF
   ```

3. Rebuild table indexes:
   ```bash
   docker compose exec postgres psql -U healthdata -d clinical_workflow_event_db \
     -c "REINDEX TABLE workflow_projections;"
   ```

---

## Checklist for Completion

### Phase 7: Docker Integration
- [ ] Updated docker-compose.yml with all 4 event services
- [ ] Updated PostgreSQL init script with 4 event databases
- [ ] Created all Kafka topics
- [ ] Services start without errors
- [ ] All health checks passing

### Phase 8: Integration Testing
- [ ] Service health endpoints responding
- [ ] Can publish test events from write-side services
- [ ] Projections update correctly within 1-2 seconds
- [ ] Queries return expected results
- [ ] Multi-tenant isolation verified
- [ ] Load test confirms < 100ms query times

### Phase 9: Production Hardening
- [ ] SSL/TLS configured for Kafka
- [ ] Redis secured with authentication
- [ ] Database SSL enabled
- [ ] Prometheus metrics exposed
- [ ] Grafana dashboards created
- [ ] Distributed tracing configured
- [ ] Disaster recovery procedures documented
- [ ] Runbook for projection rebuild created

---

## Build Command for All Services

```bash
cd backend

# Build all CQRS event services
./gradlew \
  :modules:services:patient-event-service:build \
  :modules:services:care-gap-event-service:build \
  :modules:services:quality-measure-event-service:build \
  :modules:services:clinical-workflow-event-service:build \
  -x test --no-daemon
```

---

## Docker Compose Start Commands

```bash
# Start entire platform with event services
docker compose up -d

# Start only core services
docker compose --profile core up -d

# Start only event services (after core is running)
docker compose up -d patient-event-service care-gap-event-service \
                     quality-measure-event-service clinical-workflow-event-service

# View logs for all event services
docker compose logs -f patient-event-service care-gap-event-service \
                      quality-measure-event-service clinical-workflow-event-service

# Check service status
docker compose ps
```

---

## Metrics to Monitor

Once running, these metrics indicate healthy system:

1. **Kafka Consumer Lag**
   - Should be < 10 messages
   - Indicates projections are caught up

2. **Event Processing Latency**
   - Should be 100-500ms (publish → projection)
   - Indicates eventual consistency timing

3. **Query Response Times**
   - Should be 20-100ms for standard queries
   - Indicates CQRS performance benefit

4. **Cache Hit Rate**
   - Should be > 80%
   - Indicates good Redis utilization

5. **Database Connection Pool**
   - Active connections should be < max pool size
   - Indicates healthy connection management

---

## References

- **CQRS Implementation Complete**: `CQRS_IMPLEMENTATION_COMPLETE.md`
- **CQRS Progress Document**: `CQRS_IMPLEMENTATION_PROGRESS.md`
- **Patient Event Service**: `backend/modules/services/patient-event-service/`
- **Care Gap Event Service**: `backend/modules/services/care-gap-event-service/`
- **Quality Measure Event Service**: `backend/modules/services/quality-measure-event-service/`
- **Clinical Workflow Event Service**: `backend/modules/services/clinical-workflow-event-service/`

---

**Next Action**: Begin Phase 7 by updating docker-compose.yml and testing service startup
