# Runbook: Care Gap Processing Issues

**Severity:** Medium
**Response Time:** < 2 hours
**Alert Names:** `CqlEvaluationFailures`, `HighCareGapBacklog`, `LowComplianceRate`

## Symptoms

- CQL evaluations failing
- Care gap notifications not being generated
- Dashboard showing stale data
- Quality measure compliance not updating

## Impact Assessment

Care gap processing issues affect:
- Quality measure reporting
- Care gap dashboard accuracy
- Patient outreach recommendations
- Risk stratification scores

## Diagnosis

### 1. Check CQL Engine Status
```bash
kubectl get pods -n healthdata-prod -l app=cql-engine-service
kubectl logs deployment/cql-engine-service -n healthdata-prod --since=30m | grep -i "error\|exception\|failed"
```

### 2. Check Evaluation Queue
```bash
# Kafka consumer lag for CQL evaluations
kafka-consumer-groups.sh --bootstrap-server kafka:9092 \
  --describe --group cql-evaluation-consumer
```

### 3. Check Recent Evaluation Results
```sql
-- In quality_measure database
SELECT measure_id, status, count(*), max(evaluated_at)
FROM patient_evaluations
WHERE evaluated_at > now() - interval '1 hour'
GROUP BY measure_id, status;
```

### 4. Common Error Patterns

| Error | Cause | Solution |
|-------|-------|----------|
| `CQL parsing error` | Invalid CQL syntax | Check measure library |
| `Patient not found` | Missing FHIR data | Verify patient in FHIR server |
| `Terminology service unavailable` | ValueSet lookup failed | Check terminology service |
| `Timeout during evaluation` | Complex measure | Optimize CQL or increase timeout |
| `OutOfMemoryError` | Large cohort | Increase memory, batch smaller |

### 5. Check FHIR Data Availability
```bash
# Check if patient data exists
curl -s http://fhir-service:8080/Patient/<patient-id> | jq .resourceType

# Check if required resources exist
curl -s "http://fhir-service:8080/Observation?patient=<patient-id>&_count=1" | jq .total
```

### 6. Check Terminology Service
```bash
curl -s http://terminology-service:8080/ValueSet/\$validate-code?system=http://loinc.org&code=4548-4 | jq
```

## Mitigation Steps

### CQL Engine Errors

**Step 1: Restart CQL engine**
```bash
kubectl rollout restart deployment/cql-engine-service -n healthdata-prod
```

**Step 2: Check and clear error queue**
```bash
# Check dead letter queue
kafka-console-consumer.sh --bootstrap-server kafka:9092 \
  --topic cql-evaluation-dlq --from-beginning --max-messages 10
```

**Step 3: Retry failed evaluations**
```bash
# Trigger re-evaluation via API
curl -X POST http://cql-engine-service:8080/api/evaluations/retry-failed
```

### High Evaluation Backlog

**Step 1: Scale up CQL engine**
```bash
kubectl scale deployment/cql-engine-service --replicas=5 -n healthdata-prod
```

**Step 2: Check batch processing settings**
```yaml
# Verify batch size is appropriate
cql:
  batch:
    size: 100  # Adjust based on memory
    parallelism: 4
```

**Step 3: Prioritize critical measures**
```bash
# Trigger priority evaluation for specific measure
curl -X POST http://cql-engine-service:8080/api/evaluations/priority \
  -H "Content-Type: application/json" \
  -d '{"measureId": "CMS130v11", "priority": "HIGH"}'
```

### Missing Patient Data

**Step 1: Verify patient exists in FHIR**
```bash
curl -s http://fhir-service:8080/Patient/<patient-id> | jq
```

**Step 2: Check data ingestion pipeline**
```bash
kubectl logs deployment/cdr-processor-service -n healthdata-prod --since=30m | grep -i error
```

**Step 3: Trigger data refresh**
```bash
curl -X POST http://cdr-processor-service:8080/api/patients/<patient-id>/refresh
```

### Terminology Service Issues

**Step 1: Check service health**
```bash
curl -s http://terminology-service:8080/actuator/health | jq
```

**Step 2: Restart if unhealthy**
```bash
kubectl rollout restart deployment/terminology-service -n healthdata-prod
```

**Step 3: Verify ValueSet loading**
```bash
curl -s http://terminology-service:8080/ValueSet | jq '.entry | length'
```

### Stale Dashboard Data

**Step 1: Check last cache refresh**
```bash
curl -s http://quality-measure-service:8080/api/cache/status | jq
```

**Step 2: Force cache refresh**
```bash
curl -X POST http://quality-measure-service:8080/api/cache/refresh
```

**Step 3: Verify dashboard data updates**

## Recovery Verification

1. Evaluation success rate > 95%:
```sql
SELECT
  count(*) FILTER (WHERE status = 'COMPLETED') * 100.0 / count(*) as success_rate
FROM patient_evaluations
WHERE evaluated_at > now() - interval '1 hour';
```

2. No consumer lag:
```bash
kafka-consumer-groups.sh --bootstrap-server kafka:9092 \
  --describe --group cql-evaluation-consumer | grep -v "^GROUP"
# LAG column should be 0 or very low
```

3. Dashboard showing current data:
   - Check timestamp on dashboard
   - Verify recent evaluations appear

4. CQL engine healthy:
```bash
curl -s http://cql-engine-service:8080/actuator/health | jq
```

## Escalation

| Condition | Action |
|-----------|--------|
| CQL library errors | Escalate to clinical informatics |
| FHIR data quality issues | Escalate to data team |
| Persistent failures > 10% | Escalate to service owner |
| Terminology service down | Escalate to infrastructure |

## Post-Incident

- [ ] Review failed evaluations for patterns
- [ ] Check CQL measure definitions
- [ ] Verify data quality checks
- [ ] Update evaluation timeouts if needed
- [ ] Document any measure-specific issues
