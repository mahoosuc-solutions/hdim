# Chaos Engineering Experiments

## Overview

This directory contains chaos engineering experiments to validate the HDIM platform's resilience.

## Prerequisites

1. Start the chaos engineering stack:
   ```bash
   docker-compose -f docker-compose.chaos.yml up -d
   ```

2. Ensure all HDIM services are running

3. Have monitoring dashboards open (Grafana)

## Experiment Categories

### 1. Network Chaos

**Latency Injection**
```bash
# Add 500ms latency to PostgreSQL
curl -X POST http://localhost:8474/proxies/postgres/toxics \
  -H "Content-Type: application/json" \
  -d '{"name":"latency","type":"latency","attributes":{"latency":500}}'

# Remove latency
curl -X DELETE http://localhost:8474/proxies/postgres/toxics/latency
```

**Bandwidth Limitation**
```bash
# Limit Redis bandwidth to 1KB/s
curl -X POST http://localhost:8474/proxies/redis/toxics \
  -H "Content-Type: application/json" \
  -d '{"name":"bandwidth","type":"bandwidth","attributes":{"rate":1}}'
```

**Connection Reset**
```bash
# Reset 50% of Kafka connections
curl -X POST http://localhost:8474/proxies/kafka/toxics \
  -H "Content-Type: application/json" \
  -d '{"name":"reset","type":"reset_peer","toxicity":0.5}'
```

### 2. Service Chaos

**Kill Service Container**
```bash
# Kill FHIR service
docker kill fhir-service

# Observe circuit breaker behavior
# Check gateway fallback responses

# Restart service
docker-compose up -d fhir-service
```

**CPU Stress**
```bash
# Stress CQL Engine with CPU load
docker exec cql-engine-service stress --cpu 4 --timeout 60s
```

**Memory Pressure**
```bash
# Force OOM scenario (use carefully)
docker exec cql-engine-service stress --vm 1 --vm-bytes 2G --timeout 30s
```

### 3. Database Chaos

**Primary Failover**
```bash
# Simulate primary database failure
docker stop postgres-primary

# Observe:
# - Connection errors in logs
# - Circuit breaker opens
# - Retry behavior
# - Replica promotion (if using pg_repmgr)

# Restore
docker start postgres-primary
```

**Slow Queries**
```bash
# Add 2s latency to database
curl -X POST http://localhost:8474/proxies/postgres/toxics \
  -H "Content-Type: application/json" \
  -d '{"name":"slow-query","type":"latency","attributes":{"latency":2000}}'
```

### 4. Cache Chaos

**Redis Failure**
```bash
# Kill Redis
docker kill redis-master

# Observe:
# - Cache miss handling
# - Database load increase
# - Response time degradation
# - Circuit breaker on cache

# Restore
docker-compose up -d redis-master
```

**Cache Eviction Storm**
```bash
# Flush all Redis data
docker exec redis-master redis-cli FLUSHALL

# Observe cache rebuild behavior
```

## Experiment Playbooks

### Playbook 1: Service Dependency Failure

**Objective**: Verify graceful degradation when FHIR service is unavailable

**Steps**:
1. Start baseline metrics collection
2. Generate load on quality-measure-service
3. Kill fhir-service container
4. Observe for 5 minutes
5. Verify circuit breaker opened
6. Check fallback responses
7. Restart fhir-service
8. Verify recovery

**Expected Behavior**:
- [ ] Circuit breaker opens within 30 seconds
- [ ] Fallback responses return HTTP 503
- [ ] No cascading failures
- [ ] Recovery within 60 seconds of service restart

### Playbook 2: Database Latency Spike

**Objective**: Verify timeout handling and retry behavior

**Steps**:
1. Inject 3s latency to PostgreSQL
2. Send requests to patient-service
3. Observe timeout behavior
4. Verify retry attempts
5. Check thread pool saturation
6. Remove latency
7. Verify recovery

**Expected Behavior**:
- [ ] Requests timeout after configured duration
- [ ] Retry with backoff occurs
- [ ] Circuit breaker opens if failures persist
- [ ] No thread pool exhaustion

### Playbook 3: Full Zone Failure

**Objective**: Simulate availability zone outage

**Steps**:
1. Stop all services in "zone-a" (simulated)
2. Verify traffic routes to "zone-b"
3. Check data consistency
4. Restore zone-a services
5. Verify rebalancing

**Expected Behavior**:
- [ ] Automatic failover to healthy zone
- [ ] No data loss
- [ ] User impact <30 seconds

## Steady State Hypothesis

Before each experiment, define the steady state:

| Metric | Normal Value | Alert Threshold |
|--------|--------------|-----------------|
| API success rate | >99.9% | <99% |
| P99 latency | <500ms | >2000ms |
| Circuit breaker state | CLOSED | OPEN |
| Error rate | <0.1% | >1% |

## Running Automated Experiments

```bash
# Run all experiments
./run-chaos-experiments.sh

# Run specific category
./run-chaos-experiments.sh --category network

# Run in dry-run mode
./run-chaos-experiments.sh --dry-run
```

## Results Documentation

After each experiment, document:

1. **Date/Time**: When experiment ran
2. **Experiment**: What was tested
3. **Hypothesis**: Expected behavior
4. **Actual Result**: What happened
5. **Pass/Fail**: Did system behave correctly?
6. **Findings**: Any unexpected behavior
7. **Action Items**: Improvements needed

## Safety Guidelines

1. **Never run in production without approval**
2. Always have rollback plan ready
3. Start with small blast radius
4. Monitor actively during experiments
5. Have kill switch to stop experiment
6. Document everything
7. Notify on-call before starting
