# HDIM v3.0.0-rc1 Operator Runbook

## Service Health Check Reference

| Service | Container Name | Port | Health Endpoint | Expected Response |
|---------|---------------|------|-----------------|-------------------|
| CoreHive Adapter | `healthdata-corehive-adapter` | 8120 | `/corehive-adapter/actuator/health` | `{"status":"UP"}` |
| Healthix Adapter | `healthdata-healthix-adapter` | 8121 | `/healthix-adapter/actuator/health` | `{"status":"UP"}` |
| HEDIS Adapter | `healthdata-hedis-adapter` | 8122 | `/hedis-adapter/actuator/health` | `{"status":"UP"}` |
| IHE Gateway | `healthdata-ihe-gateway` | 8125 | `/ihe-gateway/health` | `{"status":"UP","service":"ihe-gateway-service"}` |

### Quick Health Check (All Services)

```bash
echo "=== CoreHive Adapter ===" && \
curl -sf http://localhost:8120/corehive-adapter/actuator/health | jq -r .status && \
echo "=== Healthix Adapter ===" && \
curl -sf http://localhost:8121/healthix-adapter/actuator/health | jq -r .status && \
echo "=== HEDIS Adapter ===" && \
curl -sf http://localhost:8122/hedis-adapter/actuator/health | jq -r .status && \
echo "=== IHE Gateway ===" && \
curl -sf http://localhost:8125/ihe-gateway/health | jq -r .status
```

---

## Common Issues and Remediation

### 1. Adapter Won't Start

**Symptoms:** Container exits immediately or enters restart loop. Logs show connection errors.

**Diagnosis:**

```bash
# Check container status
docker ps -a --filter "name=healthdata-corehive-adapter"

# View startup logs
docker logs healthdata-corehive-adapter --tail 100
```

**Common Causes and Fixes:**

| Cause | Log Indicator | Fix |
|-------|--------------|-----|
| Database unavailable | `Connection refused: postgres:5432` | Ensure PostgreSQL is running and healthy; verify `POSTGRES_PASSWORD` is set |
| Database does not exist | `FATAL: database "corehive_adapter_db" does not exist` | Run `CREATE DATABASE corehive_adapter_db OWNER healthdata;` |
| Kafka unavailable | `Connection to node -1 could not be established` | Ensure Kafka is running at `kafka:29092` |
| Schema validation failure | `Schema-validation: missing table` | Liquibase migration may have failed; check for migration errors in logs |
| Port conflict | `Address already in use` | Check for conflicting processes on ports 8120-8122, 8125 |

**Recovery:**

```bash
# Restart after fixing configuration
docker compose -f docker-compose.yml -f docker-compose.external-integrations.yml \
  up -d corehive-adapter-service
```

---

### 2. Circuit Breaker OPEN

**Symptoms:** Adapter returns 503 errors. Logs show `CircuitBreaker 'externalService' is OPEN`.

**Diagnosis:**

```bash
# Check circuit breaker state via actuator
curl -s http://localhost:8120/corehive-adapter/actuator/health | jq '.components.circuitBreakers'

# Check recent error rate in logs
docker logs healthdata-corehive-adapter --since 10m 2>&1 | grep -c "CircuitBreaker"
```

**Common Causes and Fixes:**

| Cause | Fix |
|-------|-----|
| External service down | Verify the external service URL is reachable from the container |
| Network timeout | Increase `COREHIVE_TIMEOUT_MS` or check network connectivity |
| Authentication failure | Verify API key or credentials for the external service |

**Recovery:**

The circuit breaker will automatically transition from OPEN to HALF_OPEN after the configured wait duration (default: 60 seconds). If the next call succeeds, it closes. To force recovery:

```bash
# Restart the adapter to reset circuit breaker state
docker restart healthdata-corehive-adapter
```

---

### 3. Kafka Consumer Lag

**Symptoms:** Events are produced but not consumed. Dashboard shows increasing consumer lag.

**Diagnosis:**

```bash
# Check consumer group lag
docker exec -it hdim-kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group corehive-adapter-group \
  --describe

# Check for stuck consumers
docker exec -it hdim-kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --list | grep adapter
```

**Common Causes and Fixes:**

| Cause | Fix |
|-------|-----|
| Consumer crashed | Restart the adapter container |
| Deserialization error | Check Kafka topic message format; review dead-letter topic |
| Slow processing | Scale consumers or optimize processing logic |
| Topic does not exist | Verify `external.*` topics are created |

**Recovery:**

```bash
# Reset consumer offset (use with caution)
docker exec -it hdim-kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group corehive-adapter-group \
  --topic external.corehive.events \
  --reset-offsets --to-latest --execute
```

---

### 4. mTLS Failure (Healthix Adapter)

**Symptoms:** Healthix adapter returns SSL handshake errors. Logs show `SSLHandshakeException`.

**Diagnosis:**

```bash
# Check if mTLS is enabled
docker exec healthdata-healthix-adapter env | grep MTLS

# View SSL error details
docker logs healthdata-healthix-adapter --since 10m 2>&1 | grep -i "ssl\|tls\|certificate"
```

**Common Causes and Fixes:**

| Cause | Fix |
|-------|-----|
| Certificate expired | Renew client certificate and restart adapter |
| CA not trusted | Add Healthix CA certificate to the adapter's truststore |
| Wrong certificate | Verify `HEALTHIX_MTLS_ENABLED=true` and certificate paths |
| Hostname mismatch | Ensure certificate SAN matches `HEALTHIX_GATEWAY_URL` hostname |

**Recovery:**

```bash
# Disable mTLS temporarily for debugging
HEALTHIX_MTLS_ENABLED=false docker compose \
  -f docker-compose.yml -f docker-compose.external-integrations.yml \
  up -d healthix-adapter-service
```

---

### 5. IHE Transaction Failures

**Symptoms:** IHE gateway returns errors for ITI-18, ITI-41, ITI-43, ITI-38, ITI-39, or ITI-45 transactions.

**Diagnosis:**

```bash
# Check IHE gateway health
curl -s http://localhost:8125/ihe-gateway/health | jq .

# View transaction errors
docker logs healthdata-ihe-gateway --since 30m 2>&1 | grep -i "error\|fail\|exception"

# Check OpenTelemetry traces (if Jaeger is running)
curl -s "http://localhost:16686/api/traces?service=ihe-gateway-service&limit=10" | jq '.data[].spans[].operationName'
```

**Common Causes and Fixes:**

| Transaction | Cause | Fix |
|-------------|-------|-----|
| ITI-18 (Registry Query) | FHIR server unreachable | Verify `HEALTHIX_FHIR_URL` |
| ITI-41 (Document Provide) | Document repository down | Verify `HEALTHIX_DOCUMENT_URL` |
| ITI-43 (Document Retrieve) | Document not found | Check document identifier in request |
| ITI-45 (PIXv3 Query) | MPI unreachable | Verify `HEALTHIX_MPI_URL` |
| ITI-38 (XCA Query) | Responding gateway timeout | Check network to remote community |
| ITI-39 (XCA Retrieve) | Cross-community auth failure | Verify XCA trust configuration |

---

## Monitoring

### Prometheus Queries

#### Error Rate (per adapter, 5-minute window)

```promql
rate(http_server_requests_seconds_count{service=~"corehive-adapter|healthix-adapter|hedis-adapter|ihe-gateway", status=~"5.."}[5m])
/
rate(http_server_requests_seconds_count{service=~"corehive-adapter|healthix-adapter|hedis-adapter|ihe-gateway"}[5m])
```

#### PHI Events Blocked

```promql
sum(rate(phi_events_blocked_total{service=~".*adapter.*"}[5m])) by (service, phi_level)
```

#### Circuit Breaker State

```promql
resilience4j_circuitbreaker_state{service=~"corehive-adapter|healthix-adapter|hedis-adapter"}
# Values: 0=CLOSED (healthy), 1=OPEN (failing), 2=HALF_OPEN (recovering)
```

#### Kafka Consumer Lag

```promql
kafka_consumer_lag{group=~".*adapter.*"}
```

#### IHE Transaction Latency (p95)

```promql
histogram_quantile(0.95, rate(ihe_transaction_duration_seconds_bucket{service="ihe-gateway"}[5m]))
```

---

## Alert Thresholds

| Metric | Warning | Critical | Action |
|--------|---------|----------|--------|
| Error rate (5xx / total) | > 5% for 5 min | > 15% for 5 min | Check adapter logs, verify external service |
| Request latency (p95) | > 2s for 5 min | > 5s for 5 min | Check external service response time, network |
| Circuit breaker state | HALF_OPEN > 2 min | OPEN > 5 min | Investigate external service availability |
| Kafka consumer lag | > 1,000 messages | > 10,000 messages | Scale consumers, check processing errors |
| WebSocket connections | < 50% of expected | < 25% of expected | Check gateway routing, client connectivity |
| IHE transaction failures | > 3% for 10 min | > 10% for 10 min | Check IHE gateway logs, verify FHIR/MPI endpoints |

---

## Maintenance Commands

### Restart an Adapter

```bash
# Graceful restart (waits for in-flight requests to complete)
docker restart healthdata-corehive-adapter

# Full recreate (pulls latest config from environment)
docker compose -f docker-compose.yml -f docker-compose.external-integrations.yml \
  up -d --force-recreate corehive-adapter-service
```

### View Structured Logs

```bash
# Stream JSON logs for a specific adapter
docker logs -f healthdata-corehive-adapter 2>&1 | jq '.'

# Filter for errors only
docker logs healthdata-corehive-adapter 2>&1 | jq 'select(.level == "ERROR")'

# Filter by correlation ID
docker logs healthdata-corehive-adapter 2>&1 | jq 'select(.traceId == "abc123")'

# View IHE transaction logs
docker logs healthdata-ihe-gateway 2>&1 | jq 'select(.iheTransaction != null)'
```

### Run Smoke Test

```bash
# Execute the end-to-end smoke test script
./scripts/tests/smoke-test.sh

# Expected output: all endpoints return healthy status
# Exit code 0 = all checks passed
# Exit code 1 = one or more checks failed
```

### Run Performance Validation

```bash
# Execute the performance validation script
./scripts/tests/perf-validation.sh

# This script verifies:
# - Adapter response times are within SLA (< 200ms p95)
# - IHE transaction throughput meets baseline
# - No memory leaks under sustained load
# - Circuit breakers respond correctly to failure injection
```

---

## Escalation

If issues cannot be resolved using this runbook:

1. Collect diagnostic information: container logs, Prometheus metrics, and trace IDs
2. Check the Migration Guide: `docs/releases/v3.0.0-rc1/MIGRATION_GUIDE.md`
3. Review the IHE Conformance Statement: `docs/releases/v3.0.0-rc1/IHE_CONFORMANCE_STATEMENT.md`
4. Consult the main Troubleshooting Guide: `docs/troubleshooting/README.md`
