# Runbook: High Error Rate

**Severity:** High
**Response Time:** < 30 min
**Alert Names:** `HighErrorRate`, `High5xxRate`, `HighLatencyP99`

## Symptoms

- Error rate > 1% (warning) or > 5% (critical)
- Increased 5xx responses in Grafana
- User-reported failures
- Elevated exception counts in logs

## Impact Assessment

Check which endpoints are affected:
```bash
# Check recent errors in logs
kubectl logs deployment/<service-name> -n healthdata-prod --since=10m | grep -i error | head -50
```

## Diagnosis

### 1. Identify Error Source
```bash
# Check Prometheus for error rates by endpoint
# In Grafana, navigate to service dashboard > Error Rate panel

# Or query directly:
curl -s "http://prometheus:9090/api/v1/query?query=sum(rate(http_server_requests_seconds_count{status=~'5..'}[5m])) by (uri)"
```

### 2. Check Service Logs
```bash
# Get error logs
kubectl logs deployment/<service-name> -n healthdata-prod --since=15m | grep -E "ERROR|Exception|WARN"

# Check for patterns
kubectl logs deployment/<service-name> -n healthdata-prod --since=15m | grep -i error | sort | uniq -c | sort -rn | head -20
```

### 3. Common Error Patterns

| Error | Likely Cause | Quick Fix |
|-------|--------------|-----------|
| `Connection refused` | Downstream service down | Check dependency status |
| `Timeout` | Slow dependency or query | Scale up, optimize query |
| `OutOfMemory` | Memory leak or spike | Restart, increase limits |
| `NullPointerException` | Bug in code | Rollback to previous version |
| `JwtException` | Token expired/invalid | Check auth service |
| `FhirException` | FHIR server issue | Check fhir-service |
| `CqlException` | CQL evaluation error | Check patient data quality |

### 4. Check Dependencies
```bash
# Health of all dependencies
curl -s http://<service>:8080/actuator/health | jq '.components | to_entries[] | select(.value.status != "UP")'
```

### 5. Check Recent Deployments
```bash
kubectl rollout history deployment/<service-name> -n healthdata-prod
```

## Mitigation Steps

### Recent Deployment Caused Issues

**Step 1: Rollback**
```bash
kubectl rollout undo deployment/<service-name> -n healthdata-prod
```

**Step 2: Verify error rate drops**
- Check Grafana error rate panel
- Wait 2-3 minutes for metrics to update

### Downstream Dependency Issue

**Step 1: Identify failing dependency**
```bash
# Check which dependency is failing
kubectl logs deployment/<service-name> -n healthdata-prod --since=5m | grep -i "connection\|timeout\|refused"
```

**Step 2: Fix or restart dependency**
See relevant runbook for the failing service.

**Step 3: Consider circuit breaker**
If dependency is slow but not down, circuit breaker should activate automatically.

### Load-Related Errors

**Step 1: Scale up**
```bash
kubectl scale deployment/<service-name> --replicas=5 -n healthdata-prod
```

**Step 2: Check if HPA is configured**
```bash
kubectl get hpa -n healthdata-prod
```

### Memory-Related Errors

**Step 1: Check memory usage**
```bash
kubectl top pods -n healthdata-prod -l app=<service-name>
```

**Step 2: Restart to clear memory**
```bash
kubectl rollout restart deployment/<service-name> -n healthdata-prod
```

**Step 3: Consider increasing limits**
```yaml
resources:
  limits:
    memory: "2Gi"  # Increase from current
```

## Recovery Verification

1. Error rate returns to baseline (< 0.1%):
   - Check Grafana service dashboard
   - Query: `sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))`

2. No new errors in logs:
```bash
kubectl logs deployment/<service-name> -n healthdata-prod --since=5m | grep -c ERROR
# Should be near 0
```

3. Health check passing:
```bash
curl -s http://<service>:8080/actuator/health | jq .status
# Should be "UP"
```

## Escalation

| Condition | Action |
|-----------|--------|
| Error rate > 50% | Escalate immediately to team lead |
| Cannot identify root cause | Escalate to service owner |
| Rollback didn't help | Escalate to team lead |
| Database errors | See database-issues runbook |

## Post-Incident

- [ ] Identify root cause
- [ ] Create bug ticket if code issue
- [ ] Add missing monitoring/alerting
- [ ] Update runbook with new error patterns
