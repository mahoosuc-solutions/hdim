# Runbook: Resource Exhaustion (Memory/CPU)

**Severity:** Medium
**Response Time:** < 2 hours
**Alert Names:** `HighMemoryUsage`, `HighCpuUsage`, `PodOOMKilled`

## Symptoms

- Memory usage > 80%
- CPU usage > 80%
- Pod restarts due to OOMKilled
- Slow response times

## Diagnosis

### 1. Check Resource Usage
```bash
# All pods
kubectl top pods -n healthdata-prod

# Specific service
kubectl top pods -n healthdata-prod -l app=<service-name>

# Node-level
kubectl top nodes
```

### 2. Check Pod Events
```bash
kubectl describe pod <pod-name> -n healthdata-prod | grep -A10 Events
# Look for: OOMKilled, Evicted
```

### 3. Check Container Limits
```bash
kubectl get deployment <service-name> -n healthdata-prod -o jsonpath='{.spec.template.spec.containers[0].resources}'
```

### 4. JVM Memory Analysis
```bash
# Heap usage
curl -s http://<service>:8080/actuator/metrics/jvm.memory.used?tag=area:heap | jq

# Non-heap (metaspace)
curl -s http://<service>:8080/actuator/metrics/jvm.memory.used?tag=area:nonheap | jq

# GC metrics
curl -s http://<service>:8080/actuator/metrics/jvm.gc.pause | jq
```

### 5. Check for Memory Leaks
```bash
# Heap dump (CAUTION: can affect performance)
kubectl exec -it deployment/<service-name> -n healthdata-prod -- \
  jmap -dump:format=b,file=/tmp/heap.hprof 1

# Copy heap dump locally for analysis
kubectl cp <pod-name>:/tmp/heap.hprof ./heap.hprof -n healthdata-prod
```

## Mitigation Steps

### High Memory Usage

**Step 1: Restart affected pods**
```bash
kubectl rollout restart deployment/<service-name> -n healthdata-prod
```

**Step 2: Scale horizontally to distribute load**
```bash
kubectl scale deployment/<service-name> --replicas=5 -n healthdata-prod
```

**Step 3: Increase memory limits (if justified)**
```bash
kubectl patch deployment <service-name> -n healthdata-prod -p \
  '{"spec":{"template":{"spec":{"containers":[{"name":"<container>","resources":{"limits":{"memory":"2Gi"}}}]}}}}'
```

### High CPU Usage

**Step 1: Identify hot threads**
```bash
kubectl exec -it deployment/<service-name> -n healthdata-prod -- \
  jstack 1 | grep -A 20 "RUNNABLE"
```

**Step 2: Scale horizontally**
```bash
kubectl scale deployment/<service-name> --replicas=5 -n healthdata-prod
```

**Step 3: Check for CPU-intensive operations**
- CQL evaluation during peak hours
- Batch processing jobs
- Large data exports

### OOMKilled Pods

**Step 1: Check what triggered OOM**
```bash
kubectl describe pod <pod-name> -n healthdata-prod | grep -i oom
kubectl logs <pod-name> -n healthdata-prod --previous | tail -100
```

**Step 2: Increase memory limit**
```yaml
resources:
  limits:
    memory: "2Gi"  # Increase
  requests:
    memory: "1Gi"
```

**Step 3: Tune JVM heap**
```yaml
env:
  - name: JAVA_OPTS
    value: "-Xmx1536m -Xms1024m"  # Leave room for non-heap
```

### Node Resource Pressure

**Step 1: Check node status**
```bash
kubectl describe node <node-name> | grep -A5 Conditions
```

**Step 2: Evict non-critical pods**
```bash
kubectl cordon <node-name>  # Prevent new pods
kubectl drain <node-name> --ignore-daemonsets  # Evict pods
```

**Step 3: Add nodes (escalate to DevOps)**

## Resource Sizing Guidelines

| Service | Memory Request | Memory Limit | CPU Request | CPU Limit |
|---------|---------------|--------------|-------------|-----------|
| gateway-service | 512Mi | 1Gi | 250m | 1000m |
| cql-engine-service | 1Gi | 2Gi | 500m | 2000m |
| patient-service | 512Mi | 1Gi | 250m | 1000m |
| fhir-service | 1Gi | 2Gi | 500m | 2000m |
| analytics-service | 1Gi | 2Gi | 500m | 2000m |
| Other services | 256Mi | 512Mi | 100m | 500m |

## Recovery Verification

1. Resource usage within limits:
```bash
kubectl top pods -n healthdata-prod -l app=<service-name>
# CPU < 70%, Memory < 70%
```

2. No recent OOMKilled events:
```bash
kubectl get events -n healthdata-prod --field-selector reason=OOMKilled --sort-by='.lastTimestamp'
```

3. Service responding normally:
```bash
curl -s http://<service>:8080/actuator/health | jq .status
```

## Escalation

| Condition | Action |
|-----------|--------|
| Memory leak suspected | Escalate to service owner with heap dump |
| Node capacity exhausted | Escalate to DevOps for node scaling |
| Consistent OOM despite tuning | Escalate for architecture review |

## Post-Incident

- [ ] Review resource limits
- [ ] Check for memory leaks
- [ ] Update HPA thresholds if needed
- [ ] Consider vertical pod autoscaling
- [ ] Add resource usage monitoring
