# Runbook: Service Down

**Severity:** Critical
**Response Time:** Immediate (< 15 min)
**Alert Name:** `ApplicationDown`, `ServiceUnavailable`

## Symptoms

- Service health check failing
- 503 errors from gateway
- Prometheus target down
- PagerDuty critical alert

## Impact Assessment

| Service | Impact |
|---------|--------|
| gateway-service | All API traffic blocked |
| cql-engine-service | Quality measure evaluations fail |
| fhir-service | No patient data access |
| patient-service | Patient lookups fail |
| care-gap-service | Care gap detection stops |

## Diagnosis

### 1. Check Pod Status
```bash
# Kubernetes
kubectl get pods -n healthdata-prod -l app=<service-name>
kubectl describe pod <pod-name> -n healthdata-prod

# Docker
docker compose ps <service-name>
```

### 2. Check Logs
```bash
# Kubernetes - get last 100 lines
kubectl logs deployment/<service-name> -n healthdata-prod --tail=100

# Check for crash loops
kubectl get pods -n healthdata-prod | grep <service-name>
# Look for: CrashLoopBackOff, Error, OOMKilled

# Docker
docker compose logs --tail=100 <service-name>
```

### 3. Common Error Patterns

| Log Pattern | Cause | Solution |
|-------------|-------|----------|
| `OutOfMemoryError` | JVM heap exhausted | Increase memory limits |
| `Connection refused: postgres` | Database unreachable | Check database runbook |
| `Connection refused: kafka` | Kafka unreachable | Check Kafka runbook |
| `No healthy upstream` | Dependency down | Restart dependency first |
| `SIGKILL` | OOM killer | Increase memory limits |

## Mitigation Steps

### Step 1: Immediate Restart
```bash
# Kubernetes
kubectl rollout restart deployment/<service-name> -n healthdata-prod

# Docker
docker compose restart <service-name>
```

### Step 2: Check Dependencies
```bash
# Verify database
kubectl exec -it deployment/<service-name> -n healthdata-prod -- \
  curl -s http://localhost:8080/actuator/health | jq .components.db

# Verify Kafka
kubectl exec -it deployment/<service-name> -n healthdata-prod -- \
  curl -s http://localhost:8080/actuator/health | jq .components.kafka
```

### Step 3: Scale Up (if load related)
```bash
kubectl scale deployment/<service-name> --replicas=5 -n healthdata-prod
```

### Step 4: Check Resource Limits
```bash
kubectl top pods -n healthdata-prod -l app=<service-name>
kubectl describe pod <pod-name> -n healthdata-prod | grep -A5 "Limits:"
```

### Step 5: Rollback (if recent deploy)
```bash
# Check deployment history
kubectl rollout history deployment/<service-name> -n healthdata-prod

# Rollback to previous version
kubectl rollout undo deployment/<service-name> -n healthdata-prod
```

## Recovery Verification

1. Check health endpoint:
```bash
curl -s https://api.healthdata-platform.io/<service>/actuator/health
```

2. Verify in Grafana:
   - Service availability graph returns to 100%
   - Error rate drops to baseline

3. Check Prometheus targets:
   - Navigate to Prometheus > Status > Targets
   - Verify service target is UP

## Escalation

| Condition | Action |
|-----------|--------|
| Service won't start after 3 restarts | Escalate to team lead |
| Database dependency issue | Escalate to DBA |
| Memory/CPU limits need increase | Escalate to DevOps |
| Unknown error pattern | Escalate to service owner |

## Post-Incident

- [ ] Update status page when resolved
- [ ] Create incident ticket
- [ ] Schedule postmortem if outage > 30 min
- [ ] Document any new failure modes
