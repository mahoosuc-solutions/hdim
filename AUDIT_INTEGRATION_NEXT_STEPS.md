# Audit Integration - Next Steps

## ✅ Completed

1. **Fixed Audit Integration Issues**
   - Added `agentId` field to `CareGapAuditIntegration` and `CqlAuditIntegration`
   - Fixed compilation errors in test files
   - Created comprehensive test suite (lightweight + heavyweight)

2. **Established Testing Architecture**
   - Documented lightweight/heavyweight test patterns
   - Created `BaseIntegrationTest` annotation
   - Set up `TestAuditConfiguration` for test infrastructure

3. **Built and Deployed Services**
   - Built JAR files for `care-gap-service` and `cql-engine-service`
   - Created Docker images
   - Deployed services successfully
   - Services are healthy and running

## 🎯 Immediate Next Steps

### 1. Verify Audit Integration End-to-End

**Test that audit events are being published to Kafka:**

```bash
# Check if Kafka topic exists
docker exec healthdata-kafka kafka-topics --bootstrap-server localhost:29092 --list | grep ai.agent.decisions

# Consume events from Kafka
docker exec healthdata-kafka kafka-console-consumer \
  --bootstrap-server localhost:29092 \
  --topic ai.agent.decisions \
  --from-beginning \
  --max-messages 5
```

**Trigger an audit event:**

```bash
# Test care-gap-service audit event
curl -X POST http://localhost:8086/care-gap/api/v1/gaps \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: test-tenant-123" \
  -d '{
    "patientId": "patient-456",
    "measureId": "HEDIS_CDC_A1C"
  }'

# Test cql-engine-service audit event
curl -X POST http://localhost:8081/cql-engine/api/v1/evaluations \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: test-tenant-123" \
  -d '{
    "patientId": "patient-456",
    "measureId": "HEDIS_CDC_A1C"
  }'
```

**Verify partition key format:**
- Events should have partition key: `tenantId:agentId`
- For care-gap: `test-tenant-123:care-gap-identifier`
- For cql-engine: `test-tenant-123:cql-engine`

### 2. Fix Gateway Service Port Conflict

**Option A: Stop conflicting process**
```bash
# Find what's using port 8080
sudo lsof -i :8080
# or
sudo netstat -tulpn | grep :8080

# Stop the process or change its port
```

**Option B: Change gateway port in docker-compose.yml**
```yaml
gateway-service:
  ports:
    - "8080:8080"  # Change to "8087:8080" or another port
```

**Option C: Build and start gateway service**
```bash
cd /home/webemo-aaron/projects/hdim-master/backend
./gradlew :modules:services:gateway-service:bootJar --no-daemon
docker build -t healthdata/gateway-service:latest \
  -f modules/services/gateway-service/Dockerfile .
docker compose start gateway-service
```

### 3. Run Test Suite

**Run lightweight tests (fast, no Docker required):**
```bash
cd /home/webemo-aaron/projects/hdim-master/backend
./gradlew :modules:services:care-gap-service:test --tests "*Test" --exclude-task "*HeavyweightTest"
./gradlew :modules:services:cql-engine-service:test --tests "*Test" --exclude-task "*HeavyweightTest"
```

**Run heavyweight tests (requires Docker):**
```bash
# Ensure Docker is running
docker ps

# Run heavyweight integration tests
./gradlew :modules:services:care-gap-service:test --tests "*HeavyweightTest"
./gradlew :modules:services:cql-engine-service:test --tests "*HeavyweightTest"
```

**Run all tests:**
```bash
./gradlew :modules:services:care-gap-service:test
./gradlew :modules:services:cql-engine-service:test
```

### 4. Monitor Service Logs

**Watch for audit event publishing:**
```bash
# Care Gap Service logs
docker logs -f healthdata-care-gap-service | grep -i "audit\|agentId"

# CQL Engine Service logs
docker logs -f healthdata-cql-engine-service | grep -i "audit\|agentId"

# Kafka logs (if events are being published)
docker logs -f healthdata-kafka | grep -i "ai.agent.decisions"
```

**Check for errors:**
```bash
docker logs healthdata-care-gap-service 2>&1 | grep -i "error\|exception" | tail -20
docker logs healthdata-cql-engine-service 2>&1 | grep -i "error\|exception" | tail -20
```

## 🔄 Medium-Term Improvements

### 1. Extend Audit Integration to Other Services

**Services that might need audit integration:**
- `quality-measure-service` - Quality measure calculations
- `patient-service` - Patient data access decisions
- `fhir-service` - FHIR resource access decisions
- `analytics-service` - Analytics query decisions
- `ai-assistant-service` - AI assistant interactions

**Pattern to follow:**
1. Create `*AuditIntegration.java` class
2. Inject `AIAuditEventPublisher`
3. Add `agentId` field to event builders
4. Create lightweight unit tests
5. Create heavyweight integration tests

### 2. Improve Test Coverage

**Current coverage:**
- ✅ Unit tests for audit integration classes
- ✅ Integration tests with Kafka (heavyweight)
- ⚠️ End-to-end API tests with audit verification

**Add:**
- End-to-end API tests that verify audit events are published
- Performance tests for audit event publishing
- Load tests to ensure audit doesn't impact service performance

### 3. Add Monitoring and Alerting

**Metrics to track:**
- Audit event publishing rate
- Audit event publishing failures
- Kafka partition distribution
- Event processing latency

**Add Prometheus metrics:**
```java
@Timed(name = "audit.publish.duration", description = "Time to publish audit event")
@Counted(name = "audit.publish.total", description = "Total audit events published")
public void publishAIDecision(AIAgentDecisionEvent event) { ... }
```

### 4. Documentation Updates

**Update service READMEs:**
- Document audit integration
- Add examples of audit events
- Document how to test audit integration
- Add troubleshooting guide

**Create audit integration guide:**
- How to add audit integration to a new service
- Best practices for audit event design
- Testing strategies
- Common pitfalls

## 🚀 Long-Term Enhancements

### 1. Audit Event Query Interface

**Create API to query audit events:**
- Query by tenant, agent, time range
- Search by correlation ID
- Filter by decision type
- Export audit logs

### 2. Audit Event Analytics Dashboard

**Visualize:**
- Audit event volume over time
- Agent decision distribution
- Confidence score trends
- Error rates by service

### 3. Compliance Reporting

**Generate reports for:**
- SOC 2 compliance (CC7.2, CC8.1)
- HIPAA compliance (45 CFR § 164.312(b))
- Custom compliance requirements

### 4. Event Replay and Testing

**Features:**
- Replay audit events for testing
- Create test scenarios from real events
- Validate event structure
- Test event consumers

## 📋 Checklist

### Immediate (Today)
- [ ] Verify audit events are published to Kafka
- [ ] Test partition key format (`tenantId:agentId`)
- [ ] Fix gateway service port conflict
- [ ] Run lightweight test suite
- [ ] Check service logs for errors

### Short-Term (This Week)
- [ ] Run heavyweight test suite
- [ ] Add monitoring/metrics for audit publishing
- [ ] Update service documentation
- [ ] Create audit integration guide
- [ ] Test end-to-end audit flow

### Medium-Term (This Month)
- [ ] Extend audit integration to other services
- [ ] Add API tests with audit verification
- [ ] Set up alerting for audit failures
- [ ] Create audit event query interface
- [ ] Build audit analytics dashboard

## 🔍 Troubleshooting

### Audit Events Not Appearing in Kafka

**Check:**
1. Kafka is healthy: `docker compose ps | grep kafka`
2. Topic exists: `docker exec healthdata-kafka kafka-topics --list`
3. Service logs for errors: `docker logs healthdata-care-gap-service | grep -i error`
4. Kafka connectivity: Check `SPRING_KAFKA_BOOTSTRAP_SERVERS` environment variable

### Services Not Starting

**Check:**
1. Database connectivity: `docker logs healthdata-postgres`
2. Redis connectivity: `docker logs healthdata-redis`
3. Port conflicts: `lsof -i :8086` or `lsof -i :8081`
4. Resource limits: `docker stats`

### Tests Failing

**For lightweight tests:**
- Ensure mocks are properly configured
- Check test data setup
- Verify assertions match expected values

**For heavyweight tests:**
- Ensure Docker is running
- Check Testcontainers can pull images
- Verify Kafka container starts successfully
- Check test timeout settings

## 📚 Resources

- **Testing Architecture**: `backend/TESTING_ARCHITECTURE.md`
- **Audit Integration Summary**: `AUDIT_INTEGRATION_FIX_SUMMARY.md`
- **Service Integration**: `SERVICE_INTEGRATION_COMPLETE.md`
- **Docker Compose**: `docker-compose.yml`

## 🎯 Success Criteria

**Audit Integration is successful when:**
1. ✅ All audit events include `agentId` field
2. ✅ Events are published to Kafka with correct partition keys
3. ✅ All tests pass (lightweight + heavyweight)
4. ✅ Services run without errors
5. ✅ Events can be consumed and verified
6. ✅ Documentation is complete
7. ✅ Monitoring is in place

---

**Last Updated**: 2026-01-13
**Status**: Services deployed, ready for verification
