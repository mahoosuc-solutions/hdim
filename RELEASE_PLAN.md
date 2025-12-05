# CQL Engine Service - Release Plan
**Version**: 1.0.0
**Date**: 2025-10-31
**Approach**: Test-Driven Development with Phased Validation

## Overview
This plan defines a structured, incremental approach to validate and release the CQL Engine Service with 53 HEDIS measure implementations. Each phase has clear goals, pass/fail criteria, and rollback procedures.

---

## Phase 1: Rebuild Service with Fixes
**Duration**: 5-10 minutes
**Goal**: Build fresh JAR and Docker image with all fixes from recent commits

### Prerequisites
- ✅ All code fixes committed (5 commits: 6bf0b28 through 6c30eb5)
- ✅ @EnableFeignClients annotation added
- ✅ Redis configuration fixed (spring.data.redis.*)
- ✅ Build.gradle.kts dependencies updated

### Tasks
1. Clean build artifacts
2. Build fresh JAR with Gradle
3. Copy JAR to backend/app.jar
4. Build Docker image (healthdata/cql-engine-service:1.0.0)
5. Stop current container
6. Start new container with updated image

### Pass Criteria ✅
- [ ] JAR builds without errors
- [ ] JAR file size > 50MB (includes all dependencies)
- [ ] Docker image builds successfully
- [ ] Docker image tagged as healthdata/cql-engine-service:1.0.0
- [ ] Container starts within 30 seconds
- [ ] No exceptions in first 60 seconds of logs

### Fail Criteria ❌
- Build errors or compilation failures
- Missing dependencies in JAR
- Docker build fails
- Container crashes on startup
- FhirServiceClient bean creation errors

### Rollback
- Use previous working image if available
- Revert to commit before changes if critical failure

### Validation Commands
```bash
# Build JAR
cd backend && ./gradlew :modules:services:cql-engine-service:bootJar --no-daemon

# Verify JAR
ls -lh modules/services/cql-engine-service/build/libs/cql-engine-service.jar

# Build Docker
cp modules/services/cql-engine-service/build/libs/cql-engine-service.jar app.jar
docker build -t healthdata/cql-engine-service:1.0.0 --build-arg SERVICE_NAME=cql-engine-service -f Dockerfile .

# Start container
docker compose up -d --force-recreate cql-engine-service

# Check logs
docker logs healthdata-cql-engine --tail 50
```

---

## Phase 2: Verify Core Functionality
**Duration**: 5 minutes
**Goal**: Validate service startup, actuator endpoints, Redis connectivity, and Feign client initialization

### Prerequisites
- ✅ Phase 1 completed successfully
- ✅ Redis container running and healthy

### Tasks
1. Verify service startup completion
2. Test actuator health endpoint
3. Verify Redis connection in health details
4. Check Feign client beans registered
5. Verify 53 HEDIS measure beans loaded

### Pass Criteria ✅
- [ ] Service logs show "Started CqlEngineServiceApplication" within 20 seconds
- [ ] GET /actuator/health returns HTTP 200 (or 401 if auth enabled)
- [ ] Health response shows status: UP
- [ ] Redis component shows UP with version 7.x
- [ ] No "NoSuchBeanDefinitionException" errors in logs
- [ ] Logs show FeignClientFactoryBean registration
- [ ] Application context contains 53+ measure beans

### Fail Criteria ❌
- Service fails to start within 60 seconds
- Health endpoint returns 404 or 500
- Redis shows DOWN status
- FhirServiceClient bean missing
- Measure beans not registered

### Rollback
- Review logs for specific errors
- May need to adjust configuration in application-docker.yml
- Can run locally first to debug if Docker issues persist

### Validation Commands
```bash
# Check service startup
docker logs healthdata-cql-engine 2>&1 | grep "Started CqlEngineServiceApplication"

# Test health endpoint (may need auth)
curl -v http://localhost:8081/actuator/health

# Detailed health with auth
curl -u user:<password> http://localhost:8081/actuator/health

# Check Redis connection
docker exec healthdata-cql-engine wget -qO- http://localhost:8081/actuator/health | grep -i redis

# Check beans (from inside container or via actuator/beans if enabled)
docker exec healthdata-cql-engine wget -qO- http://localhost:8081/actuator/beans | grep -i "measure"
```

---

## Phase 3: Complete Infrastructure Stack
**Duration**: 15-20 minutes
**Goal**: Start all infrastructure services and verify inter-service connectivity

### Prerequisites
- ✅ Phase 2 completed successfully
- ✅ docker-compose.yml configured for all services

### Tasks
1. Start PostgreSQL with health check
2. Start Kafka and Zookeeper
3. Start FHIR mock service (HAPI FHIR)
4. Verify all service health checks pass
5. Test connectivity from CQL Engine to each service

### Pass Criteria ✅
- [ ] PostgreSQL: status healthy, accepts connections on port 5435
- [ ] Redis: status healthy (already verified)
- [ ] Zookeeper: status healthy, listening on 2181
- [ ] Kafka: status healthy, broker available on 9092
- [ ] FHIR mock: status healthy, /fhir/metadata returns CapabilityStatement
- [ ] CQL Engine: can connect to all services
- [ ] No connection refused errors in any service logs

### Fail Criteria ❌
- Any service fails health check after 2 minutes
- PostgreSQL refuses connections
- Kafka cannot connect to Zookeeper
- FHIR service returns 500 errors
- Network connectivity issues between containers

### Rollback
- Stop problematic service
- Review configuration
- Can run with minimal stack (Redis only) if needed

### Validation Commands
```bash
# Start all services
docker compose up -d

# Check all service status
docker compose ps

# Test PostgreSQL
docker exec healthdata-postgres pg_isready -U healthdata -d healthdata_cql

# Test Redis
docker exec healthdata-redis redis-cli ping

# Test Kafka
docker exec healthdata-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Test FHIR mock
curl http://localhost:8080/fhir/metadata | grep "CapabilityStatement"

# Check all health
for service in postgres redis kafka fhir-service-mock cql-engine-service; do
  echo "=== $service ==="
  docker compose ps $service
done
```

---

## Phase 4: API Endpoint Implementation
**Duration**: 20-30 minutes
**Goal**: Add REST API endpoints for measure evaluation and verify request/response flow

### Prerequisites
- ✅ Phase 3 completed successfully
- ✅ 53 HEDIS measures implemented and beans loaded
- ✅ FhirServiceClient operational

### Tasks
1. Review existing MeasureController implementation
2. Implement/verify POST /api/measures/evaluate endpoint
3. Implement/verify GET /api/measures endpoint (list available measures)
4. Add request validation
5. Add error handling and proper HTTP status codes
6. Test with sample requests

### Pass Criteria ✅
- [ ] GET /api/measures returns list of 53 measures
- [ ] Response includes measure codes, names, descriptions
- [ ] POST /api/measures/evaluate accepts valid request
- [ ] Request validation rejects invalid inputs
- [ ] 400 errors for bad requests with clear messages
- [ ] 404 errors for unknown measure codes
- [ ] 500 errors are logged with stack traces
- [ ] Swagger UI accessible at /swagger-ui.html

### Fail Criteria ❌
- Endpoints return 404 (not mapped)
- NullPointerException on valid requests
- No input validation
- Generic error messages without details
- Swagger UI not accessible

### Rollback
- Can disable controllers temporarily if breaking
- Service still functional for actuator/monitoring

### Validation Commands
```bash
# List available measures
curl http://localhost:8081/api/measures

# Evaluate CDC measure (example)
curl -X POST http://localhost:8081/api/measures/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "measureCode": "CDC",
    "patientId": "patient-123",
    "tenantId": "tenant-1",
    "measurementPeriodEnd": "2024-12-31"
  }'

# Test validation (should return 400)
curl -X POST http://localhost:8081/api/measures/evaluate \
  -H "Content-Type: application/json" \
  -d '{}'

# Check Swagger
curl http://localhost:8081/swagger-ui.html
```

### Expected Response Format
```json
{
  "measureCode": "CDC",
  "patientId": "patient-123",
  "tenantId": "tenant-1",
  "numerator": true,
  "denominator": true,
  "exclusion": false,
  "details": {
    "hba1c_tested": true,
    "bp_controlled": true,
    "eye_exam_done": false
  },
  "evaluationDate": "2025-10-31T12:00:00Z"
}
```

---

## Phase 5: Integration Testing
**Duration**: 30-45 minutes
**Goal**: End-to-end testing with real FHIR data and measure evaluation

### Prerequisites
- ✅ Phase 4 completed successfully
- ✅ FHIR mock service running
- ✅ Sample FHIR resources prepared

### Tasks
1. Load sample patient data into FHIR server
2. Load sample observations, conditions, procedures
3. Execute measure evaluation for each HEDIS measure category
4. Verify caching behavior (Redis)
5. Test concurrent requests (load testing)
6. Verify Kafka event publishing (if enabled)

### Pass Criteria ✅
- [ ] Successfully evaluate at least 5 different HEDIS measures
- [ ] FHIR client fetches patient data correctly
- [ ] Cached evaluations return within 100ms
- [ ] Uncached evaluations return within 500ms
- [ ] Concurrent requests (10 simultaneous) succeed
- [ ] No memory leaks after 100 evaluations
- [ ] Kafka events published for evaluations (if enabled)
- [ ] Cache invalidation works correctly

### Fail Criteria ❌
- FHIR client returns 404 for valid patient IDs
- Measure evaluation throws exceptions
- Cache not working (all requests hit FHIR server)
- Memory grows unbounded
- Concurrent requests cause deadlocks
- Kafka events not published

### Rollback
- Can continue with basic functionality testing
- May need to debug specific measure logic

### Validation Commands
```bash
# Load sample patient
curl -X POST http://localhost:8080/fhir/Patient \
  -H "Content-Type: application/fhir+json" \
  -d @test-data/patient-sample.json

# Evaluate multiple measures
for measure in CDC CBP BCS CCS COL; do
  echo "Testing $measure..."
  curl -X POST http://localhost:8081/api/measures/evaluate \
    -H "Content-Type: application/json" \
    -d "{\"measureCode\":\"$measure\",\"patientId\":\"test-patient-1\",\"tenantId\":\"tenant-1\",\"measurementPeriodEnd\":\"2024-12-31\"}"
done

# Check Redis cache
docker exec healthdata-redis redis-cli KEYS "cql-engine:*"

# Monitor performance
time curl -X POST http://localhost:8081/api/measures/evaluate -H "Content-Type: application/json" -d '...'

# Load test (if ab/siege available)
ab -n 100 -c 10 -p request.json -T application/json http://localhost:8081/api/measures/evaluate
```

---

## Phase 6: Release Preparation
**Duration**: 15-20 minutes
**Goal**: Finalize documentation, versioning, and deployment artifacts

### Prerequisites
- ✅ Phase 5 completed successfully
- ✅ All tests passing
- ✅ No critical bugs identified

### Tasks
1. Update version numbers (1.0.0)
2. Create release notes
3. Tag Docker image for production
4. Update deployment documentation
5. Create deployment checklist
6. Prepare rollback procedures
7. Document known limitations

### Pass Criteria ✅
- [ ] VERSION file updated to 1.0.0
- [ ] CHANGELOG.md created with all changes
- [ ] Docker image tagged with :1.0.0 and :latest
- [ ] DEPLOYMENT.md has step-by-step instructions
- [ ] Known issues documented
- [ ] Rollback procedure documented
- [ ] Performance benchmarks documented
- [ ] Security review completed

### Fail Criteria ❌
- Incomplete documentation
- Missing version tags
- No rollback plan
- Security vulnerabilities unaddressed

### Deliverables
- [ ] Docker image: healthdata/cql-engine-service:1.0.0
- [ ] CHANGELOG.md
- [ ] DEPLOYMENT.md
- [ ] ROLLBACK.md
- [ ] KNOWN_ISSUES.md
- [ ] Performance benchmarks
- [ ] Git tag: v1.0.0

### Validation Commands
```bash
# Tag Docker image
docker tag healthdata/cql-engine-service:1.0.0 healthdata/cql-engine-service:latest

# Create git tag
git tag -a v1.0.0 -m "Release version 1.0.0 - CQL Engine Service with 53 HEDIS measures"
git push origin v1.0.0

# Verify image
docker images | grep cql-engine-service

# Export image for distribution
docker save healthdata/cql-engine-service:1.0.0 | gzip > cql-engine-service-1.0.0.tar.gz
```

---

## Success Metrics

### Service Health
- Uptime: 99.9%
- Startup time: < 30 seconds
- Memory usage: < 2GB under normal load
- CPU usage: < 50% under normal load

### Performance
- Cached evaluation: < 100ms (p95)
- Uncached evaluation: < 500ms (p95)
- Throughput: > 200 req/s per instance
- Concurrent users: Support 50+ simultaneous evaluations

### Quality
- 0 critical bugs
- 0 P1 bugs at release
- All 53 HEDIS measures functional
- Test coverage: > 70%

---

## Risk Mitigation

### High-Risk Areas
1. **FHIR Client Integration**: May fail if FHIR server unavailable
   - Mitigation: Implement circuit breaker, fallback mechanisms

2. **Redis Connectivity**: Service depends on Redis for caching
   - Mitigation: Graceful degradation without cache

3. **Measure Logic**: Complex HEDIS calculations may have edge cases
   - Mitigation: Comprehensive test data, validation against reference implementation

### Rollback Strategy
- Keep previous working Docker image tagged
- Document configuration rollback steps
- Maintain database migration rollback scripts
- Have downtime communication plan ready

---

## Communication Plan

### Stakeholders
- Development Team
- QA Team
- DevOps Team
- Product Management
- Clinical SMEs (for measure validation)

### Status Updates
- After each phase completion
- Immediate notification of phase failures
- Daily summary during release process
- Final release announcement

---

## Appendix: Command Reference

### Quick Start
```bash
# Full stack startup
docker compose up -d

# Check all services
docker compose ps

# View logs
docker compose logs -f cql-engine-service

# Stop all
docker compose down
```

### Troubleshooting
```bash
# Rebuild single service
docker compose up -d --build --force-recreate cql-engine-service

# Check health
curl http://localhost:8081/actuator/health

# Access container
docker exec -it healthdata-cql-engine sh

# Check Redis
docker exec healthdata-redis redis-cli MONITOR
```
