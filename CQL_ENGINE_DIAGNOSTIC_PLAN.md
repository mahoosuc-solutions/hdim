# CQL Engine Connection Diagnostic & Remediation Plan

## Executive Summary

**Issue**: Quality Measure Evaluation service cannot connect to CQL Engine service
**Impact**: Evaluations failing to process
**Severity**: Critical - Blocks core HEDIS evaluation functionality
**Approach**: Automated diagnostic agent with systematic root cause analysis

---

## Phase 1: Initial Reconnaissance (5-10 minutes)

### 1.1 Service Status Validation

**Objective**: Verify both services are running and healthy

```bash
# Check running containers
docker ps --filter "name=cql-engine" --filter "name=quality-measure"

# Check service logs for startup errors
docker logs healthdata-cql-engine-service --tail 100 | grep -i "error\|exception\|failed"
docker logs healthdata-quality-measure-service --tail 100 | grep -i "error\|exception\|failed"

# Verify health endpoints
curl -s http://localhost:8081/cql-engine/actuator/health | jq
curl -s http://localhost:8087/quality-measure/actuator/health | jq
```

**Expected Findings**:
- Both containers running with status "Up"
- Health endpoints return `{"status":"UP"}`
- No critical errors in startup logs

**Red Flags**:
- ⚠️ Container status "Restarting" or "Exited"
- ⚠️ Health check shows DOWN
- ⚠️ Connection refused errors in logs
- ⚠️ Service not bound to expected port

---

## Phase 2: Network Connectivity Analysis (10-15 minutes)

### 2.1 Container Network Validation

**Objective**: Verify Docker network configuration and DNS resolution

```bash
# Verify both services are on the same network
docker inspect healthdata-cql-engine-service -f '{{range $k, $v := .NetworkSettings.Networks}}{{$k}} {{end}}'
docker inspect healthdata-quality-measure-service -f '{{range $k, $v := .NetworkSettings.Networks}}{{$k}} {{end}}'

# Test DNS resolution from quality-measure to cql-engine
docker exec healthdata-quality-measure-service nslookup cql-engine-service

# Test direct connectivity
docker exec healthdata-quality-measure-service curl -s http://cql-engine-service:8081/cql-engine/actuator/health
```

**Expected Findings**:
- Both services on `hdim-master_default` network
- DNS resolves `cql-engine-service` to container IP
- Direct curl succeeds with HTTP 200

**Red Flags**:
- ⚠️ Services on different networks
- ⚠️ DNS resolution fails
- ⚠️ Connection timeout or refused
- ⚠️ Network not found errors

### 2.2 Port Binding Verification

```bash
# Check CQL Engine port binding
docker port healthdata-cql-engine-service
netstat -tulpn | grep 8081

# Check if port is accessible from host
curl -v http://localhost:8081/cql-engine/actuator/health

# Verify no port conflicts
docker ps --format "table {{.Names}}\t{{.Ports}}" | grep 8081
```

---

## Phase 3: Configuration Audit (15-20 minutes)

### 3.1 Environment Variable Validation

**Objective**: Verify service URLs, timeouts, and authentication config

```bash
# Quality Measure Service configuration
docker exec healthdata-quality-measure-service env | grep -E "CQL_ENGINE|FHIR_SERVICE"

# CQL Engine Service configuration
docker exec healthdata-cql-engine-service env | grep -E "SPRING_|SERVER_"
```

**Critical Checks**:
- `CQL_ENGINE_SERVICE_URL` matches actual service location
- Timeout values are reasonable (30000ms default)
- No hardcoded `localhost` or `127.0.0.1` URLs
- Authentication credentials match if required

### 3.2 Application Configuration Review

**Files to examine**:
1. `backend/modules/services/quality-measure-service/src/main/resources/application.yml`
2. `backend/modules/services/cql-engine-service/src/main/resources/application.yml`
3. `docker-compose.yml` service definitions
4. `docker-compose.override.yml` environment overrides

**Key Configuration Points**:

```yaml
# quality-measure-service/application.yml
cql-engine:
  url: ${CQL_ENGINE_SERVICE_URL:http://cql-engine-service:8081/cql-engine}
  timeout: ${CQL_ENGINE_TIMEOUT:30000}
  retry:
    max-attempts: 3
    backoff: 1000

# Expected in docker-compose.yml
services:
  cql-engine-service:
    container_name: healthdata-cql-engine-service
    networks:
      - default
    ports:
      - "8081:8081"
```

---

## Phase 4: API Integration Validation (10-15 minutes)

### 4.1 API Contract Verification

**Objective**: Verify CQL Engine API is responding correctly

```bash
# Test CQL Engine API directly
curl -X POST http://localhost:8081/cql-engine/api/v1/evaluate \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: $(uuidgen)" \
  -d '{
    "cqlLibrary": "library Test version '\''1.0.0'\''",
    "expression": "true"
  }' | jq

# Check API documentation
curl -s http://localhost:8081/cql-engine/v3/api-docs | jq '.paths | keys'
```

**Expected Response**:
- HTTP 200 or 400 (if test data invalid)
- Valid JSON response
- No 5xx server errors

### 4.2 RestTemplate/WebClient Configuration

**Check Quality Measure Service client configuration**:

```bash
# Search for RestTemplate or WebClient beans
grep -r "RestTemplate\|WebClient" backend/modules/services/quality-measure-service/src/main/java/
```

**Common Issues**:
- Missing timeout configuration
- Incorrect base URL
- Missing error handling
- No retry logic

---

## Phase 5: Deep Diagnostic Analysis (15-20 minutes)

### 5.1 Trace Request Flow

**Enable DEBUG logging temporarily**:

```yaml
# Add to quality-measure-service application.yml
logging:
  level:
    org.springframework.web.client: DEBUG
    org.apache.http: DEBUG
    com.healthdata.quality: DEBUG
```

**Rebuild and restart**:
```bash
./gradlew :modules:services:quality-measure-service:bootJar
docker compose build quality-measure-service
docker compose up -d quality-measure-service
```

**Trigger evaluation and capture logs**:
```bash
# Trigger evaluation (use actual endpoint)
curl -X POST http://localhost:8087/quality-measure/api/v1/evaluations \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: $(uuidgen)" \
  -d '{...}' &

# Watch logs in real-time
docker logs -f healthdata-quality-measure-service
```

**Look for**:
- DNS resolution logs
- HTTP request details (method, URL, headers)
- Connection attempt logs
- Error stack traces

### 5.2 Service Dependency Chain

**Map the full dependency chain**:

```
User Request
  ↓
Gateway Service (if used)
  ↓
Quality Measure Service
  ↓
CQL Engine Service
  ↓
[Database/FHIR Service/etc.]
```

**Verify each hop**:
1. Quality Measure can reach CQL Engine
2. CQL Engine can reach its dependencies
3. No circular dependencies
4. All services have required secrets/credentials

---

## Phase 6: Root Cause Hypotheses & Testing

### Common Root Causes (ranked by probability)

#### 1. Service Discovery Failure (40% probability)
**Hypothesis**: Docker DNS not resolving service names correctly

**Test**:
```bash
docker exec healthdata-quality-measure-service ping -c 3 cql-engine-service
docker exec healthdata-quality-measure-service nslookup cql-engine-service
```

**Fix if confirmed**:
```yaml
# docker-compose.yml - explicit service links
services:
  quality-measure-service:
    depends_on:
      - cql-engine-service
    links:
      - cql-engine-service
```

#### 2. Incorrect Service URL Configuration (30% probability)
**Hypothesis**: Quality Measure service using wrong URL (localhost vs service name)

**Test**:
```bash
docker exec healthdata-quality-measure-service env | grep CQL_ENGINE_SERVICE_URL
```

**Fix if confirmed**:
```yaml
# docker-compose.override.yml
services:
  quality-measure-service:
    environment:
      CQL_ENGINE_SERVICE_URL: http://cql-engine-service:8081/cql-engine
```

#### 3. Network Isolation (15% probability)
**Hypothesis**: Services on different Docker networks

**Test**:
```bash
docker network ls
docker inspect $(docker network ls -q) | grep -A 10 "healthdata-cql-engine-service\|healthdata-quality-measure-service"
```

**Fix if confirmed**:
```bash
docker network connect hdim-master_default healthdata-quality-measure-service
```

#### 4. Port Binding Issues (10% probability)
**Hypothesis**: CQL Engine not listening on expected port inside container

**Test**:
```bash
docker exec healthdata-cql-engine-service netstat -tulpn | grep 8081
docker exec healthdata-cql-engine-service curl -s http://localhost:8081/cql-engine/actuator/health
```

**Fix if confirmed**:
Check `server.port` in application.yml matches Dockerfile EXPOSE

#### 5. Authentication/CORS Issues (5% probability)
**Hypothesis**: Request blocked by authentication or CORS policy

**Test**:
```bash
# Check for 401/403 responses
docker logs healthdata-cql-engine-service | grep -E "401|403|Unauthorized|Forbidden"
```

**Fix if confirmed**:
Add proper authentication headers or whitelist internal service calls

---

## Phase 7: Implementation & Verification

### 7.1 Apply Fix

Once root cause identified, implement fix with this workflow:

1. **Backup current state**:
   ```bash
   git status
   git stash push -m "Before CQL Engine fix"
   ```

2. **Apply configuration changes**:
   - Update `docker-compose.yml` or `application.yml`
   - Update environment variables in `.env`

3. **Rebuild affected services**:
   ```bash
   ./gradlew :modules:services:quality-measure-service:bootJar
   docker compose build quality-measure-service
   ```

4. **Restart with dependency order**:
   ```bash
   docker compose down quality-measure-service
   docker compose up -d cql-engine-service
   sleep 10  # Wait for CQL Engine to be ready
   docker compose up -d quality-measure-service
   ```

5. **Verify fix**:
   ```bash
   # Check logs for successful connection
   docker logs healthdata-quality-measure-service --tail 50 | grep -i "cql"

   # Test evaluation endpoint
   curl -X POST http://localhost:8087/quality-measure/api/v1/evaluations/test \
     -H "X-Tenant-ID: $(uuidgen)"
   ```

### 7.2 Post-Fix Validation Checklist

- [ ] Both services show `Status: UP` in health checks
- [ ] No connection errors in logs
- [ ] Test evaluation completes successfully
- [ ] Response time is acceptable (< 5 seconds for simple evaluation)
- [ ] All existing tests still pass
- [ ] Documentation updated with fix details

---

## Phase 8: Preventive Measures

### 8.1 Add Connection Health Checks

**Implement in Quality Measure Service**:

```java
@Component
public class CqlEngineHealthIndicator implements HealthIndicator {

    @Value("${cql-engine.url}")
    private String cqlEngineUrl;

    private final RestTemplate restTemplate;

    @Override
    public Health health() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                cqlEngineUrl + "/actuator/health",
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return Health.up()
                    .withDetail("cql-engine", "Connected")
                    .withDetail("url", cqlEngineUrl)
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("cql-engine", "Connection failed")
                .withDetail("error", e.getMessage())
                .build();
        }

        return Health.down().build();
    }
}
```

### 8.2 Add Retry Logic with Exponential Backoff

```java
@Configuration
public class CqlEngineClientConfig {

    @Bean
    public RestTemplate cqlEngineRestTemplate() {
        RestTemplate template = new RestTemplate();

        // Add timeout
        HttpComponentsClientHttpRequestFactory factory =
            new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);
        template.setRequestFactory(factory);

        // Add retry interceptor
        template.getInterceptors().add(new RetryInterceptor());

        return template;
    }
}
```

### 8.3 Add Observability

**Metrics to track**:
- CQL Engine connection success rate
- Average response time
- Timeout frequency
- Retry attempts

**Prometheus metrics**:
```java
@Component
public class CqlEngineMetrics {

    private final Counter requestsTotal;
    private final Counter requestsFailedTotal;
    private final Histogram requestDuration;

    public CqlEngineMetrics(MeterRegistry registry) {
        this.requestsTotal = Counter.builder("cql_engine_requests_total")
            .description("Total CQL Engine requests")
            .register(registry);

        this.requestsFailedTotal = Counter.builder("cql_engine_requests_failed_total")
            .description("Failed CQL Engine requests")
            .register(registry);

        this.requestDuration = Histogram.builder("cql_engine_request_duration_seconds")
            .description("CQL Engine request duration")
            .register(registry);
    }
}
```

---

## Phase 9: Documentation & Handoff

### 9.1 Create Runbook Entry

**Document in**: `docs/operations/RUNBOOK_CQL_ENGINE_CONNECTIVITY.md`

**Include**:
- Symptom description
- Root cause identified
- Fix applied
- Validation steps
- Monitoring alerts to set up
- Escalation path if issue recurs

### 9.2 Update Architecture Documentation

**Update**: `DISTRIBUTION_ARCHITECTURE.md`

**Add section**:
```markdown
## Service-to-Service Communication

### Quality Measure ↔ CQL Engine

**Protocol**: HTTP/REST
**Network**: Docker internal network
**Service Discovery**: Docker DNS (service name resolution)
**Timeout**: 30s
**Retry Policy**: 3 attempts with exponential backoff
**Health Check**: /actuator/health endpoint
**Monitoring**: Prometheus metrics at /actuator/prometheus
```

---

## Automated Diagnostic Script

Save as `scripts/diagnose-cql-connectivity.sh`:

```bash
#!/bin/bash

echo "======================================"
echo "CQL Engine Connectivity Diagnostic"
echo "======================================"
echo

echo "1. Checking service status..."
docker ps --filter "name=cql-engine" --filter "name=quality-measure" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo

echo "2. Checking health endpoints..."
echo "CQL Engine:"
curl -s http://localhost:8081/cql-engine/actuator/health | jq -r '.status' || echo "FAILED"
echo "Quality Measure:"
curl -s http://localhost:8087/quality-measure/actuator/health | jq -r '.status' || echo "FAILED"
echo

echo "3. Testing DNS resolution..."
docker exec healthdata-quality-measure-service nslookup cql-engine-service 2>&1 | grep -A 2 "Name:"
echo

echo "4. Testing connectivity..."
docker exec healthdata-quality-measure-service curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" http://cql-engine-service:8081/cql-engine/actuator/health
echo

echo "5. Checking configuration..."
echo "CQL_ENGINE_SERVICE_URL:"
docker exec healthdata-quality-measure-service env | grep CQL_ENGINE_SERVICE_URL
echo

echo "6. Recent errors in logs..."
echo "CQL Engine errors:"
docker logs healthdata-cql-engine-service --tail 20 | grep -i "error\|exception" | tail -5
echo
echo "Quality Measure errors:"
docker logs healthdata-quality-measure-service --tail 20 | grep -i "error\|exception\|cql" | tail -5
echo

echo "======================================"
echo "Diagnostic complete!"
echo "======================================"
```

---

## Quick Reference: Common Fixes

### Fix 1: Update service URL
```yaml
# docker-compose.override.yml
services:
  quality-measure-service:
    environment:
      CQL_ENGINE_SERVICE_URL: http://cql-engine-service:8081/cql-engine
```

### Fix 2: Add network dependency
```yaml
# docker-compose.yml
services:
  quality-measure-service:
    depends_on:
      cql-engine-service:
        condition: service_healthy
```

### Fix 3: Increase timeout
```yaml
# quality-measure-service/application.yml
cql-engine:
  timeout: 60000  # Increase from 30s to 60s
```

### Fix 4: Add retry logic
```yaml
# quality-measure-service/application.yml
resilience4j:
  retry:
    instances:
      cqlEngine:
        maxAttempts: 3
        waitDuration: 1000
        enableExponentialBackoff: true
```

---

## Success Criteria

✅ **Fix is successful when**:
1. Quality Measure service health check shows "UP"
2. CQL Engine service health check shows "UP"
3. Test evaluation completes without connection errors
4. Logs show successful CQL Engine API calls
5. No connection timeout errors for 24 hours
6. Monitoring dashboards show 99%+ connection success rate

---

## Escalation Path

If diagnostic plan doesn't resolve issue:

1. **Level 1** (1 hour): Re-run full diagnostic with DEBUG logging
2. **Level 2** (2 hours): Review Spring Boot auto-configuration, check for bean conflicts
3. **Level 3** (4 hours): Network packet capture with `tcpdump`, review Docker network drivers
4. **Level 4** (8 hours): Engage infrastructure team, review Docker daemon configuration
5. **Level 5** (24 hours): Consider architectural change (API Gateway, service mesh)

---

*Generated for HDIM v1.6.0 - Production/Enterprise Quality Tier*
