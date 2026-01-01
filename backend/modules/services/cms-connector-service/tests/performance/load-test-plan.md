# CMS Connector Service - Load Testing Plan

**Phase**: Phase 5 Week 1  
**Duration**: 2 weeks  
**Objective**: Establish performance baselines and identify bottlenecks  

## Load Testing Strategy

### Test Scenarios

#### Scenario 1: Baseline Load
- **Users**: 10 concurrent
- **Duration**: 5 minutes
- **Think Time**: 2 seconds between requests
- **Endpoints Tested**:
  - `GET /api/v1/actuator/health` (10%)
  - `GET /api/v1/claims/{id}` (50%)
  - `POST /api/v1/claims/search` (30%)
  - `GET /api/v1/data/sync-status` (10%)

**Expected Results**:
- Response time p95 < 100ms
- Response time p99 < 200ms
- Error rate: 0%
- Throughput: > 10 req/s

#### Scenario 2: Normal Load
- **Users**: 100 concurrent (ramp up over 60s)
- **Duration**: 10 minutes
- **Think Time**: 1 second between requests
- **Load distribution**: Same as Scenario 1

**Expected Results**:
- Response time p95 < 500ms
- Response time p99 < 1000ms
- Error rate: < 0.1%
- Throughput: > 100 req/s

#### Scenario 3: Peak Load
- **Users**: 500 concurrent (ramp up over 120s)
- **Duration**: 15 minutes
- **Think Time**: 500ms between requests
- **Load distribution**: Same as Scenario 1

**Expected Results**:
- Response time p95 < 1000ms
- Response time p99 < 2000ms
- Error rate: < 1%
- Throughput: > 300 req/s

#### Scenario 4: Stress Test
- **Users**: Increase from 100 to 2000+ over 20 minutes
- **Duration**: Until system breaks or reaches limits
- **Think Time**: 0 (maximum load)
- **Endpoints**: Most common (GET /api/v1/claims/{id})

**Goal**: Identify breaking point and capacity limits

### Performance Metrics

**Response Time Percentiles**:
- p50 (Median): Should be < 50ms
- p95: Should be < 500ms under normal load
- p99: Should be < 1000ms under normal load

**Throughput**:
- Requests per second (req/s)
- Track trend as load increases

**Error Rate**:
- HTTP 5xx errors (server errors)
- Connection timeouts
- Socket errors

**Resource Utilization**:
- CPU usage (% of capacity)
- Memory usage (MB and % of heap)
- Database connection pool utilization
- Redis connection pool utilization

### Success Criteria

✅ **Pass**: All metrics meet expected values for all scenarios  
⚠️ **Warning**: Some metrics slightly above targets, requires optimization  
❌ **Fail**: Multiple metrics significantly above targets, investigation required  

---

## Test Execution Plan

### Pre-Test Checklist
- [ ] Application built and running
- [ ] Database populated with test data
- [ ] Redis cache warm and accessible
- [ ] Monitoring/metrics collection enabled
- [ ] Baseline metrics captured (zero load)
- [ ] JMeter installed and configured
- [ ] Test data files prepared
- [ ] Network connectivity verified

### Test Execution Steps

1. **Run Baseline Load Test** (Scenario 1)
   - Establish baseline performance
   - Verify no errors under light load
   - Expected: 5 minutes

2. **Run Normal Load Test** (Scenario 2)
   - Validate system under typical load
   - Check for any performance degradation
   - Expected: 10 minutes

3. **Run Peak Load Test** (Scenario 3)
   - Verify system handles peak traffic
   - Identify any threshold issues
   - Expected: 15 minutes

4. **Run Stress Test** (Scenario 4)
   - Find breaking point
   - Determine maximum capacity
   - Expected: 20+ minutes or until system breaks

### Post-Test Analysis

1. **Collect Metrics**
   - JMeter aggregate report
   - Response time distribution
   - Error logs
   - Resource utilization graphs

2. **Identify Bottlenecks**
   - Which operations are slowest?
   - Where does response time increase?
   - What causes errors?
   - Which resources are bottlenecks?

3. **Generate Report**
   - Document findings
   - Create visualizations
   - Compare to baseline
   - Identify optimization opportunities

4. **Plan Optimizations**
   - Database query optimization
   - Caching strategy improvements
   - Connection pool tuning
   - Resource allocation

---

## Performance Targets

### Response Times (p95)

| Endpoint | Target |
|----------|--------|
| GET /health | < 50ms |
| GET /claims/{id} | < 100ms |
| POST /claims/search | < 500ms |
| GET /sync-status | < 200ms |

### Throughput

| Load Level | Target Throughput |
|-----------|------------------|
| Baseline (10 users) | > 10 req/s |
| Normal (100 users) | > 100 req/s |
| Peak (500 users) | > 300 req/s |

### Resource Utilization (Peak Load)

| Resource | Target |
|----------|--------|
| CPU | < 80% |
| Memory | < 85% |
| DB Connections | < 90% of pool |
| Redis Connections | < 90% of pool |

---

## Tools & Setup

### JMeter Installation
```bash
# macOS
brew install jmeter

# Or Docker
docker run -it --rm -v $(pwd):/jmeter apache/jmeter:5.5 -v
```

### Test Data Preparation
```bash
# Generate test data
./tests/performance/generate-test-data.sh

# Populate database
./tests/performance/load-test-data.sh
```

### Baseline Capture
```bash
# Before any load tests, capture baseline metrics
./tests/performance/capture-baseline.sh
```

---

## Remediation Strategy

If performance doesn't meet targets:

1. **Analysis Phase** (Day 1)
   - Profile application
   - Identify slow queries
   - Check resource utilization

2. **Optimization Phase** (Days 2-3)
   - Add database indexes
   - Implement caching
   - Optimize queries
   - Tune connection pools

3. **Validation Phase** (Day 4)
   - Re-run load tests
   - Compare to baseline
   - Verify improvements
   - Document changes

4. **Escalation** (If needed)
   - Vertical scaling (larger instances)
   - Horizontal scaling (more instances)
   - Architecture changes (add caching layer)

---

## Test Data Requirements

**Minimum records needed**:
- Claims: 100,000
- Patients: 10,000
- Providers: 1,000
- Users: 100

**Realistic data distribution**:
- Recent claims (last 30 days): 40%
- Previous quarter: 35%
- Older claims: 25%

---

## Monitoring During Tests

Keep monitoring dashboard visible:
- Application metrics
- Database metrics
- System resources
- Network traffic
- Error logs

## Success Outcomes

After Phase 5 Week 1, you will have:
✅ Performance baseline established
✅ Bottlenecks identified
✅ Optimization recommendations documented
✅ Confidence in system performance under load
