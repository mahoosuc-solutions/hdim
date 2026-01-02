# Kubernetes Deployment Configuration

Production-ready Kubernetes manifests for deploying the CQL Engine Service to Kubernetes clusters.

## Directory Structure

```
kubernetes/
├── base/                           # Base Kubernetes resources
│   ├── cql-engine-deployment.yaml  # Main deployment
│   ├── cql-engine-service.yaml     # Service (ClusterIP)
│   ├── cql-engine-configmap.yaml   # Configuration
│   ├── cql-engine-secret.yaml      # Secrets (CHANGE IN PROD!)
│   ├── cql-engine-ingress.yaml     # Ingress rules
│   ├── cql-engine-hpa.yaml         # Horizontal Pod Autoscaler
│   ├── cql-engine-serviceaccount.yaml  # RBAC
│   ├── cql-engine-pdb.yaml         # Pod Disruption Budget
│   └── kustomization.yaml          # Base kustomization
├── production/                     # Production overlay
│   ├── kustomization.yaml          # Production patches
│   └── namespace.yaml              # Production namespace + quotas
├── staging/                        # Staging overlay
│   ├── kustomization.yaml          # Staging patches
│   └── namespace.yaml              # Staging namespace + quotas
└── README.md                       # This file
```

## Quick Start

### Prerequisites

1. **kubectl** - Kubernetes command-line tool
```bash
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl
sudo mv kubectl /usr/local/bin/
```

2. **kustomize** - Kubernetes configuration management
```bash
curl -s "https://raw.githubusercontent.com/kubernetes-sigs/kustomize/master/hack/install_kustomize.sh" | bash
sudo mv kustomize /usr/local/bin/
```

3. **Access to Kubernetes cluster**
```bash
kubectl cluster-info
kubectl get nodes
```

### Deploy to Staging

```bash
# Build and validate manifests
kustomize build kubernetes/staging

# Apply to cluster
kubectl apply -k kubernetes/staging

# Watch deployment
kubectl get pods -n healthdata-staging -w

# Check service status
kubectl get all -n healthdata-staging
```

### Deploy to Production

```bash
# IMPORTANT: Update secrets first!
kubectl create secret generic cql-engine-secrets \
  --from-literal=database.password='ACTUAL_PASSWORD' \
  --from-literal=jwt.secret='ACTUAL_JWT_SECRET' \
  -n healthdata-prod \
  --dry-run=client -o yaml | kubectl apply -f -

# Build and validate
kustomize build kubernetes/production

# Apply to cluster
kubectl apply -k kubernetes/production

# Monitor rollout
kubectl rollout status deployment/cql-engine-service -n healthdata-prod

# Check all resources
kubectl get all,ingress,hpa,pdb -n healthdata-prod
```

## Configuration

### Environment-Specific Settings

| Setting | Staging | Production |
|---------|---------|------------|
| **Namespace** | healthdata-staging | healthdata-prod |
| **Replicas (min)** | 2 | 5 |
| **Replicas (max)** | 5 | 20 |
| **CPU Request** | 250m | 1000m |
| **CPU Limit** | 1000m | 4000m |
| **Memory Request** | 512Mi | 2Gi |
| **Memory Limit** | 1Gi | 4Gi |
| **PDB MinAvailable** | 1 | 3 |
| **Logging Level** | DEBUG | INFO |
| **Thread Pool Max** | 50 | 100 |

### Secrets Management

**CRITICAL**: Never commit actual secrets to git!

#### Option 1: kubectl create secret
```bash
kubectl create secret generic cql-engine-secrets \
  --from-literal=database.username='healthdata' \
  --from-literal=database.password='SECURE_PASSWORD' \
  --from-literal=redis.password='REDIS_PASSWORD' \
  --from-literal=jwt.secret='JWT_SECRET_KEY' \
  --from-literal=encryption.key='32_CHAR_ENCRYPTION_KEY_HERE' \
  -n healthdata-prod
```

#### Option 2: External Secrets Operator (Recommended)
```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: cql-engine-secrets
  namespace: healthdata-prod
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secrets-manager
    kind: SecretStore
  target:
    name: cql-engine-secrets
  data:
  - secretKey: database.password
    remoteRef:
      key: healthdata/prod/database
      property: password
  - secretKey: jwt.secret
    remoteRef:
      key: healthdata/prod/jwt
      property: secret
```

#### Option 3: Sealed Secrets
```bash
# Install kubeseal
kubectl apply -f https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.24.0/controller.yaml

# Seal a secret
kubectl create secret generic cql-engine-secrets \
  --from-literal=database.password='SECURE_PASSWORD' \
  --dry-run=client -o yaml | \
  kubeseal -o yaml > sealed-secret.yaml

# Apply sealed secret
kubectl apply -f sealed-secret.yaml
```

### ConfigMaps

Edit configuration in `base/cql-engine-configmap.yaml` or override via Kustomize:

```yaml
# kubernetes/production/kustomization.yaml
configMapGenerator:
  - name: cql-engine-env-config
    behavior: merge
    literals:
      - MEASURE_EVALUATION_MAX_POOL_SIZE=100
      - LOGGING_LEVEL_COM_HEALTHDATA_CQL=INFO
```

## Deployment Commands

### View Generated Manifests
```bash
# Staging
kustomize build kubernetes/staging > staging-manifests.yaml

# Production
kustomize build kubernetes/production > production-manifests.yaml
```

### Apply Changes
```bash
# Staging
kubectl apply -k kubernetes/staging

# Production (with confirmation)
kubectl apply -k kubernetes/production --dry-run=server
kubectl apply -k kubernetes/production
```

### Monitor Deployment
```bash
# Watch rollout
kubectl rollout status deployment/cql-engine-service -n healthdata-prod

# View pods
kubectl get pods -n healthdata-prod -l app=cql-engine-service

# View logs
kubectl logs -f -n healthdata-prod -l app=cql-engine-service --max-log-requests=10

# View events
kubectl get events -n healthdata-prod --sort-by='.lastTimestamp'
```

### Scaling
```bash
# Manual scale
kubectl scale deployment cql-engine-service --replicas=10 -n healthdata-prod

# Check HPA status
kubectl get hpa -n healthdata-prod

# Describe HPA
kubectl describe hpa cql-engine-hpa -n healthdata-prod
```

### Rollback
```bash
# View rollout history
kubectl rollout history deployment/cql-engine-service -n healthdata-prod

# Rollback to previous version
kubectl rollout undo deployment/cql-engine-service -n healthdata-prod

# Rollback to specific revision
kubectl rollout undo deployment/cql-engine-service --to-revision=3 -n healthdata-prod
```

## Health Checks

### Liveness Probe
- Endpoint: `/actuator/health/liveness`
- Initial Delay: 90s
- Period: 30s
- Timeout: 10s
- Failure Threshold: 3

### Readiness Probe
- Endpoint: `/actuator/health/readiness`
- Initial Delay: 60s
- Period: 10s
- Timeout: 5s
- Failure Threshold: 3

### Startup Probe
- Endpoint: `/actuator/health/liveness`
- Initial Delay: 30s
- Period: 10s
- Timeout: 5s
- Failure Threshold: 12 (up to 2 minutes)

## Troubleshooting

### Pod Not Starting
```bash
# Check pod status
kubectl get pods -n healthdata-prod

# Describe pod
kubectl describe pod <pod-name> -n healthdata-prod

# View logs
kubectl logs <pod-name> -n healthdata-prod

# Check events
kubectl get events -n healthdata-prod --field-selector involvedObject.name=<pod-name>
```

### Service Not Accessible
```bash
# Check service
kubectl get svc -n healthdata-prod

# Check endpoints
kubectl get endpoints -n healthdata-prod

# Port forward for testing
kubectl port-forward svc/cql-engine-service 8081:8081 -n healthdata-prod

# Test locally
curl http://localhost:8081/actuator/health
```

### Ingress Issues
```bash
# Check ingress
kubectl get ingress -n healthdata-prod

# Describe ingress
kubectl describe ingress cql-engine-ingress -n healthdata-prod

# Check ingress controller logs
kubectl logs -n ingress-nginx -l app.kubernetes.io/name=ingress-nginx
```

### HPA Not Scaling
```bash
# Check HPA status
kubectl get hpa -n healthdata-prod

# Describe HPA
kubectl describe hpa cql-engine-hpa -n healthdata-prod

# Check metrics server
kubectl top nodes
kubectl top pods -n healthdata-prod

# Verify metrics server is running
kubectl get deployment metrics-server -n kube-system
```

### ConfigMap/Secret Changes Not Applied
```bash
# Restart deployment to pick up changes
kubectl rollout restart deployment/cql-engine-service -n healthdata-prod

# Verify ConfigMap
kubectl get configmap cql-engine-config -n healthdata-prod -o yaml

# Verify Secret
kubectl get secret cql-engine-secrets -n healthdata-prod -o yaml
```

## Monitoring

### Prometheus Metrics
Metrics endpoint: `/actuator/prometheus`

Exposed via service annotation:
```yaml
prometheus.io/scrape: "true"
prometheus.io/port: "8081"
prometheus.io/path: "/actuator/prometheus"
```

### Key Metrics to Monitor
```promql
# Request rate
rate(http_server_requests_seconds_count[5m])

# Response time (P95)
histogram_quantile(0.95, http_server_requests_seconds_bucket)

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# CPU usage
container_cpu_usage_seconds_total{pod=~"cql-engine.*"}

# Memory usage
container_memory_usage_bytes{pod=~"cql-engine.*"}

# Measure evaluation metrics
rate(measure_evaluation_total[5m])
histogram_quantile(0.95, measure_evaluation_duration_seconds_bucket)
```

## ⚡ Performance

📊 **For comprehensive performance documentation, see**: [PERFORMANCE_GUIDE.md](../docs/PERFORMANCE_GUIDE.md)

### Performance Overview

| Environment | Target Load | CPU/Memory | Replicas | Expected Throughput |
|-------------|-------------|------------|----------|---------------------|
| **Staging** | <500 req/s | 250m-1000m / 512Mi-1Gi | 2-5 | 800-2,000 req/s |
| **Production** | <2000 req/s | 1000m-4000m / 2Gi-4Gi | 5-20 | 2,000-8,000 req/s |

**Key Performance Targets:**
- **P95 Latency**: <300ms
- **P99 Latency**: <500ms
- **Success Rate**: >99.9%
- **Cache Hit Rate**: >85%

### Resource Optimization

#### 1. CPU and Memory Tuning

**Light Workload** (<200 req/s per pod):
```yaml
resources:
  requests:
    cpu: 250m
    memory: 512Mi
  limits:
    cpu: 1000m
    memory: 1Gi
```

**Medium Workload** (200-400 req/s per pod):
```yaml
resources:
  requests:
    cpu: 500m
    memory: 1Gi
  limits:
    cpu: 2000m
    memory: 2Gi
```

**Heavy Workload** (400-600 req/s per pod):
```yaml
resources:
  requests:
    cpu: 1000m
    memory: 2Gi
  limits:
    cpu: 4000m
    memory: 4Gi
```

#### 2. JVM Optimization

Update ConfigMap for optimal JVM settings:

```yaml
# kubernetes/production/kustomization.yaml
configMapGenerator:
  - name: cql-engine-env-config
    behavior: merge
    literals:
      # JVM Memory (use 75% of container memory)
      - JAVA_OPTS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0

      # Garbage Collection (G1GC for predictable pauses)
      - JAVA_OPTS_GC=-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:G1HeapRegionSize=4M

      # Thread Pool (adjust based on load)
      - MEASURE_EVALUATION_CORE_POOL_SIZE=10
      - MEASURE_EVALUATION_MAX_POOL_SIZE=50
      - MEASURE_EVALUATION_QUEUE_CAPACITY=1000

      # Connection Pools
      - SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=20
      - SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=5
```

#### 3. Redis Caching Optimization

**Verify Redis is deployed**:
```bash
kubectl get pods -n healthdata-prod -l app=redis
```

**Check cache performance**:
```bash
# Get cache hit rate
kubectl exec -n healthdata-prod deployment/redis -- redis-cli INFO stats | grep hit_rate

# Monitor cache operations
kubectl exec -n healthdata-prod deployment/redis -- redis-cli MONITOR
```

**Optimal Redis configuration** (add to redis ConfigMap):
```yaml
maxmemory: 2gb
maxmemory-policy: allkeys-lru
tcp-keepalive: 60
timeout: 300
```

### Horizontal Pod Autoscaling (HPA)

#### CPU-Based Autoscaling (Default)

The included HPA (`base/cql-engine-hpa.yaml`) scales based on CPU:

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: cql-engine-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: cql-engine-service
  minReplicas: 2  # Staging
  maxReplicas: 5  # Staging
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

**Production HPA** (higher limits):
```yaml
# kubernetes/production/hpa-patch.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: cql-engine-hpa
spec:
  minReplicas: 5
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

#### Custom Metrics Autoscaling

For more advanced scaling based on application metrics:

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: cql-engine-hpa-custom
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: cql-engine-service
  minReplicas: 5
  maxReplicas: 20
  metrics:
  # CPU baseline
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70

  # Request rate (requires Prometheus adapter)
  - type: Pods
    pods:
      metric:
        name: http_requests_per_second
      target:
        type: AverageValue
        averageValue: "300"  # Scale at 300 req/s per pod

  # P95 Latency (scale if latency increases)
  - type: Pods
    pods:
      metric:
        name: http_request_duration_p95
      target:
        type: AverageValue
        averageValue: "250m"  # Scale if P95 > 250ms

  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300  # Wait 5min before scaling down
      policies:
      - type: Percent
        value: 50  # Scale down max 50% of pods at once
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 60  # Scale up quickly
      policies:
      - type: Percent
        value: 100  # Double pods if needed
        periodSeconds: 30
      - type: Pods
        value: 4  # Or add 4 pods
        periodSeconds: 30
      selectPolicy: Max  # Use the more aggressive policy
```

### Performance Monitoring in Kubernetes

#### Deploy Prometheus & Grafana

```bash
# Install Prometheus Operator
kubectl create namespace monitoring
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --set prometheus.prometheusSpec.serviceMonitorSelectorNilUsesHelmValues=false

# Port forward Grafana
kubectl port-forward -n monitoring svc/prometheus-grafana 3000:80

# Access Grafana at http://localhost:3000
# Default credentials: admin / prom-operator
```

#### Key Performance Dashboards

Import these Grafana dashboard IDs:
- **6417**: Kubernetes Cluster Monitoring
- **315**: Kubernetes Cluster Monitoring (Prometheus)
- **12900**: Spring Boot Statistics
- **4701**: JVM (Micrometer)

#### Performance Metrics Queries

```bash
# Check current HPA status
kubectl get hpa -n healthdata-prod -w

# View pod resource usage
kubectl top pods -n healthdata-prod -l app=cql-engine-service

# View node resource usage
kubectl top nodes

# Get detailed pod metrics
kubectl describe pod <pod-name> -n healthdata-prod | grep -A 5 "Requests:"
```

#### Prometheus Queries for Performance

```promql
# Average request rate across all pods
sum(rate(http_server_requests_seconds_count{namespace="healthdata-prod"}[5m]))

# P95 latency by endpoint
histogram_quantile(0.95,
  sum(rate(http_server_requests_seconds_bucket{namespace="healthdata-prod"}[5m])) by (uri, le)
)

# Error rate percentage
(
  sum(rate(http_server_requests_seconds_count{namespace="healthdata-prod",status=~"5.."}[5m]))
  /
  sum(rate(http_server_requests_seconds_count{namespace="healthdata-prod"}[5m]))
) * 100

# Cache hit rate
(
  redis_keyspace_hits_total /
  (redis_keyspace_hits_total + redis_keyspace_misses_total)
) * 100

# JVM memory usage
jvm_memory_used_bytes{namespace="healthdata-prod",area="heap"} /
jvm_memory_max_bytes{namespace="healthdata-prod",area="heap"} * 100

# Thread pool utilization
measure_evaluation_thread_pool_active_threads /
measure_evaluation_thread_pool_max_threads * 100
```

### Scaling Strategies

#### 1. Vertical Scaling (Scale Up)

Increase resources for individual pods:

```bash
# Update production deployment with more resources
kubectl patch deployment cql-engine-service -n healthdata-prod -p '{
  "spec": {
    "template": {
      "spec": {
        "containers": [{
          "name": "cql-engine-service",
          "resources": {
            "requests": {"cpu": "2000m", "memory": "4Gi"},
            "limits": {"cpu": "4000m", "memory": "8Gi"}
          }
        }]
      }
    }
  }
}'
```

**When to use**: Single pod is hitting CPU/memory limits but overall cluster capacity is available.

#### 2. Horizontal Scaling (Scale Out)

Add more pod replicas:

```bash
# Manual horizontal scale
kubectl scale deployment cql-engine-service --replicas=15 -n healthdata-prod

# Or update HPA limits
kubectl patch hpa cql-engine-hpa -n healthdata-prod -p '{
  "spec": {
    "maxReplicas": 30
  }
}'
```

**When to use**: CPU/memory per pod is healthy but total load exceeds capacity.

#### 3. Cluster Scaling (Scale Out Infrastructure)

Add more nodes to cluster:

```bash
# AWS EKS example
eksctl scale nodegroup --cluster=healthdata-prod --name=ng-workers --nodes=10

# GKE example
gcloud container clusters resize healthdata-prod --num-nodes=10

# Check node status
kubectl get nodes
kubectl top nodes
```

**When to use**: All pods are healthy but HPA cannot scale due to insufficient cluster resources.

#### Scaling Decision Matrix

| Symptom | CPU Usage | Memory Usage | Pod Count | Action |
|---------|-----------|--------------|-----------|--------|
| High latency | >80% | <70% | <Max | Increase CPU limits or add replicas |
| OOMKilled pods | Any | >90% | Any | Increase memory limits |
| Pending pods | Any | Any | At Max | Add cluster nodes |
| Low throughput | <50% | <50% | At Max | Scale down replicas to reduce costs |
| Cache misses | Any | Any | Any | Scale Redis or increase cache memory |

### Performance Testing in Kubernetes

#### Load Test from Outside Cluster

```bash
# Get ingress URL
INGRESS_URL=$(kubectl get ingress cql-engine-ingress -n healthdata-prod -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')

# Run load test
kubectl run load-test --image=jordi/ab --rm -it --restart=Never -- \
  -n 10000 -c 100 -k \
  http://${INGRESS_URL}/api/v1/measures/evaluate
```

#### Load Test from Inside Cluster

```bash
# Deploy load test pod
kubectl run load-test -n healthdata-prod --image=williamyeh/hey --rm -it --restart=Never -- \
  -z 60s -c 50 -q 10 \
  http://cql-engine-service:8081/actuator/health

# Or use Apache Bench
kubectl run ab-test -n healthdata-prod --image=jordi/ab --rm -it --restart=Never -- \
  -n 10000 -c 100 \
  http://cql-engine-service:8081/actuator/health
```

#### Monitor During Load Test

```bash
# Watch HPA scaling
watch kubectl get hpa,pods -n healthdata-prod

# Monitor resource usage
watch kubectl top pods -n healthdata-prod

# Check logs for errors
kubectl logs -f -n healthdata-prod -l app=cql-engine-service --max-log-requests=10 --tail=50
```

### Performance Troubleshooting

#### High CPU Usage

```bash
# Check which pods are using most CPU
kubectl top pods -n healthdata-prod --sort-by=cpu

# Get thread dump from high-CPU pod
kubectl exec -n healthdata-prod <pod-name> -- jstack 1 > thread-dump.txt

# Check if GC is excessive
kubectl logs <pod-name> -n healthdata-prod | grep "GC"
```

**Solutions**:
- Increase CPU limits
- Add more replicas (horizontal scaling)
- Optimize thread pool settings
- Review code for CPU-intensive operations

#### High Memory Usage

```bash
# Check memory usage
kubectl top pods -n healthdata-prod --sort-by=memory

# Get heap dump (if OOM)
kubectl exec -n healthdata-prod <pod-name> -- jmap -dump:format=b,file=/tmp/heap.hprof 1

# Copy heap dump locally
kubectl cp healthdata-prod/<pod-name>:/tmp/heap.hprof ./heap.hprof
```

**Solutions**:
- Increase memory limits
- Reduce cache sizes
- Optimize connection pool sizes
- Check for memory leaks

#### Slow Response Times

```bash
# Check P95/P99 latency in metrics
kubectl port-forward -n healthdata-prod svc/cql-engine-service 8081:8081
curl http://localhost:8081/actuator/metrics/http.server.requests

# Check database connection pool
curl http://localhost:8081/actuator/metrics/hikaricp.connections.active

# Check Redis connection
kubectl exec -n healthdata-prod deployment/redis -- redis-cli PING
```

**Solutions**:
- Check Redis cache hit rate
- Optimize database queries
- Increase connection pool sizes
- Add more replicas

#### HPA Not Scaling

```bash
# Check HPA status
kubectl describe hpa cql-engine-hpa -n healthdata-prod

# Verify metrics server
kubectl get apiservice v1beta1.metrics.k8s.io -o yaml

# Check metrics availability
kubectl top pods -n healthdata-prod
```

**Solutions**:
- Install/restart metrics-server
- Verify resource requests are set
- Check HPA metrics collection
- Review HPA configuration

### Performance Best Practices

1. **Always set resource requests and limits** - Required for proper scheduling and HPA
2. **Use HPA with appropriate thresholds** - Start scaling before hitting limits (70-80% CPU)
3. **Enable Pod Disruption Budgets (PDB)** - Maintain availability during scaling events
4. **Monitor cache hit rates** - >85% cache hit rate is critical for performance
5. **Use readiness probes correctly** - Don't send traffic to pods that aren't ready
6. **Implement graceful shutdown** - Allow in-flight requests to complete (30-60s)
7. **Use anti-affinity rules** - Spread pods across nodes for better resilience
8. **Monitor and alert on key metrics** - P95 latency, error rate, cache hits
9. **Load test before production** - Validate performance under realistic conditions
10. **Keep resource limits realistic** - Too low = throttling, too high = wasted resources

### Additional Performance Resources

- **Performance Guide**: [docs/PERFORMANCE_GUIDE.md](../docs/PERFORMANCE_GUIDE.md) - Comprehensive performance documentation
- **Performance Runbook**: [docs/runbooks/PERFORMANCE_RUNBOOK.md](../docs/runbooks/PERFORMANCE_RUNBOOK.md) - Operations procedures
- **Docker Performance**: [DOCKER_README.md](../DOCKER_README.md#performance) - Container-level optimization

## Security

### Pod Security
- Runs as non-root user (UID 1001)
- Drops all capabilities
- Read-only root filesystem where possible
- Security context enforced

### Network Policies
```bash
# Apply network policy (if needed)
kubectl apply -f kubernetes/base/network-policy.yaml
```

### RBAC
- ServiceAccount created per deployment
- Minimal permissions granted (read-only for ConfigMaps/Secrets)
- RoleBinding scoped to namespace

## Production Checklist

Before deploying to production:

- [ ] Update all secrets in `cql-engine-secret.yaml`
- [ ] Configure proper database credentials
- [ ] Set up external secrets management (AWS Secrets Manager, Vault, etc.)
- [ ] Configure TLS certificates for Ingress
- [ ] Set up proper DNS for ingress host
- [ ] Configure resource quotas and limits
- [ ] Enable Pod Security Policies/Admission
- [ ] Set up monitoring (Prometheus/Grafana)
- [ ] Configure log aggregation (ELK, Loki, etc.)
- [ ] Set up alerting rules
- [ ] Configure backup strategy for persistent data
- [ ] Test disaster recovery procedures
- [ ] Review and apply network policies
- [ ] Enable audit logging
- [ ] Configure pod security standards
- [ ] Test autoscaling behavior
- [ ] Perform load testing
- [ ] Review and tune JVM settings
- [ ] Configure proper ingress rate limiting
- [ ] Set up WAF rules (if applicable)
- [ ] Review RBAC permissions

## Additional Resources

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Kustomize Documentation](https://kustomize.io/)
- [Spring Boot Kubernetes Guide](https://spring.io/guides/gs/spring-boot-kubernetes/)
- [Kubernetes Best Practices](https://kubernetes.io/docs/concepts/configuration/overview/)

## Support

For issues or questions:
1. Check logs: `kubectl logs -n <namespace> -l app=cql-engine-service`
2. Review events: `kubectl get events -n <namespace>`
3. Check health: `kubectl get pods,svc,ingress -n <namespace>`
4. Refer to main documentation: [DOCKER_README.md](../DOCKER_README.md)
