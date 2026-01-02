# CQL Engine Service - Known Issues

**Version**: 1.0.0
**Date**: 2025-10-31

## Critical Issues

None identified for core functionality.

## Known Limitations

### 1. CQL Evaluation API - GET Endpoint Returns 500 Error ✅ RESOLVED

**Endpoint**: `GET /api/v1/cql/evaluations`

**Status**: ✅ Fixed in v1.0.1 (2025-10-31)

**Root Cause**: Missing GET endpoint mapping at base path `/api/v1/cql/evaluations` in CqlEvaluationController

**Fix Applied**:
- Added `Page<CqlEvaluation> findByTenantId(String tenantId, Pageable pageable)` to CqlEvaluationRepository
- Added `getAllEvaluations(String tenantId, Pageable pageable)` method to CqlEvaluationService
- Added `@GetMapping` endpoint to CqlEvaluationController to handle GET requests at base path
- Updated docker-compose.yml to use image version 1.0.1

**Verification**: Endpoint now returns HTTP 200 with paginated response

---

### 2. HEDIS Measure REST API - Separate Microservice

**Status**: By design (microservices architecture)

**Description**:
- 52 HEDIS measures are implemented in cql-engine-service as Spring beans (@Component)
- Measures include: CDC, CBP, COL, BCS, CCS, AMM, IMA, WCV, FVA, etc.
- All measures extend `AbstractHedisMeasure` with `evaluate(tenantId, patientId)` method
- Caching configured via `@Cacheable` annotation (Redis-backed)

**Architecture**:
- `cql-engine-service`: Core measure evaluation engine (this service)
- `quality-measure-service`: REST API frontend (separate microservice)

**quality-measure-service Endpoints**:
- `POST /quality-measure/calculate` - Calculate specific HEDIS measure
- `GET /quality-measure/results` - Get all results for a patient
- `GET /quality-measure/score` - Get quality score summary
- `GET /quality-measure/report/patient` - Patient quality report
- `GET /quality-measure/report/population` - Population quality report

**Usage**: Deploy both services and call quality-measure-service API endpoints

---

### 3. Docker Health Check Returns "Unhealthy" ✅ RESOLVED

**Status**: ✅ Fixed in v1.0.1 (2025-10-31)

**Root Cause**: Health check command in docker-compose.yml didn't include context path `/cql-engine`

**Fix Applied**:
- Updated docker-compose.yml health check URL from:
  - `http://localhost:8081/actuator/health` (incorrect)
  - to: `http://localhost:8081/cql-engine/actuator/health` (correct)

**Verification**: Container now shows "healthy" status in `docker ps`

---

### 4. Swagger UI Not Publicly Accessible

**Status**: By design (authentication required)

**Description**:
- OpenAPI docs available at `/cql-engine/v3/api-docs` (public)
- Swagger UI at `/cql-engine/swagger-ui.html` requires authentication

**Configuration**: Update SecurityConfig to permit Swagger UI if needed

---

## Performance Notes

### Measured Performance (Single instance, Docker)

**Startup Time**: ~20-22 seconds
**Memory Usage**: ~450MB (steady state)
**Request Latency**:
- GET requests (database): 106-110ms
- POST requests (database): 110-120ms
- Actuator health: <10ms

**Expected Performance** (from design docs):
- Single measure evaluation (cached): <100ms
- Single measure evaluation (uncached): 200-500ms
- Throughput: 200-400 req/s per instance

---

## Infrastructure Dependencies

### Required Services
- **PostgreSQL** 15+: Primary database
- **Redis** 7.4+: Caching layer
- **HAPI FHIR** R4: FHIR resource provider (optional for HEDIS measures)

### Optional Services
- **Kafka**: Event streaming (not required for core functionality)
- **Zookeeper**: Kafka dependency (not required for core functionality)

---

## Configuration Issues Resolved

### ✅ Fixed in v1.0.0
1. **Database Schema Validation Failure**
   - Changed `spring.jpa.hibernate.ddl-auto` from `validate` to `update`
   - File: `application-docker.yml`

2. **FeignClient Bean Missing**
   - Added `@EnableFeignClients` to main application class
   - File: `CqlEngineServiceApplication.java`

3. **CSRF Protection Blocking POST/PUT/DELETE**
   - Created `SecurityConfig` to disable CSRF for REST API
   - File: `config/SecurityConfig.java`

---

## Reporting Issues

For bug reports and feature requests:
1. Check this document first
2. Review logs: `docker logs healthdata-cql-engine`
3. Check application-docker.yml configuration
4. Report issues with:
   - Service version
   - Docker logs (last 100 lines)
   - Request/response details
   - Environment configuration

---

## Next Release Priorities

1. Implement HEDIS Measure REST API controller (quality-measure-service)
2. Add measure evaluation performance metrics
3. Implement batch measure evaluation endpoint
4. Add comprehensive integration tests
5. Add automated end-to-end testing
