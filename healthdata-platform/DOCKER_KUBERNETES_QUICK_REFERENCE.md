# HealthData Platform - Docker & Kubernetes Quick Reference

## Quick Commands

### Docker

```bash
# Build production image
docker build -f Dockerfile.production -t healthdata/platform:2.0.0 .

# Run with Docker Compose
docker-compose -f docker-compose.prod.yml up -d

# View logs
docker-compose logs -f healthdata-platform

# Stop all services
docker-compose -f docker-compose.prod.yml down

# Health check
curl http://localhost:8080/actuator/health
```

### Kubernetes

```bash
# Deploy everything
kubectl apply -f k8s/

# Check status
kubectl get all -n healthdata

# View logs
kubectl logs -n healthdata -l app=healthdata-platform -f

# Scale deployment
kubectl scale deployment healthdata-platform -n healthdata --replicas=5

# Restart deployment
kubectl rollout restart deployment healthdata-platform -n healthdata

# Rollback
kubectl rollout undo deployment healthdata-platform -n healthdata
```

### Helm

```bash
# Install
helm install healthdata-platform ./helm/healthdata-platform -n healthdata --create-namespace

# Upgrade
helm upgrade healthdata-platform ./helm/healthdata-platform -n healthdata

# Rollback
helm rollback healthdata-platform -n healthdata

# Uninstall
helm uninstall healthdata-platform -n healthdata

# View values
helm get values healthdata-platform -n healthdata
```

## Deployment Scripts

```bash
# Production deployment (Docker + K8s)
./scripts/deployment/deploy-production.sh

# Helm deployment
./scripts/deployment/deploy-kubernetes.sh

# Rollback
./scripts/deployment/rollback.sh

# Dry run
DRY_RUN=true ./scripts/deployment/deploy-kubernetes.sh
```

## Environment Variables

```bash
# Required
DB_PASSWORD=your_secure_password
JWT_SECRET=your_jwt_secret_256_bits
REDIS_PASSWORD=your_redis_password

# Optional
VERSION=2.0.0
ENVIRONMENT=production
KUBE_NAMESPACE=healthdata
REPLICA_COUNT=3
```

## File Structure

```
.
├── Dockerfile.production          # Production Docker image
├── docker-compose.prod.yml        # Production compose file
├── nginx.conf                     # Nginx reverse proxy config
├── k8s/                          # Kubernetes manifests
│   ├── namespace.yaml
│   ├── secrets.yaml
│   ├── configmap.yaml
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── ingress.yaml
│   ├── hpa.yaml
│   └── serviceaccount.yaml
├── helm/                         # Helm chart
│   └── healthdata-platform/
│       ├── Chart.yaml
│       ├── values.yaml
│       └── templates/
└── scripts/deployment/           # Deployment scripts
    ├── deploy-production.sh
    ├── deploy-kubernetes.sh
    └── rollback.sh
```

## Troubleshooting

```bash
# Pod not starting
kubectl describe pod <pod-name> -n healthdata

# Check events
kubectl get events -n healthdata --sort-by='.lastTimestamp'

# Check resources
kubectl top pods -n healthdata

# Debug container
kubectl exec -it <pod-name> -n healthdata -- /bin/sh

# Port forward
kubectl port-forward -n healthdata svc/healthdata-platform 8080:8080

# View previous logs
kubectl logs <pod-name> -n healthdata --previous
```

## Health Checks

```bash
# Liveness
curl http://localhost:8080/actuator/health/liveness

# Readiness
curl http://localhost:8080/actuator/health/readiness

# Metrics
curl http://localhost:8080/actuator/prometheus
```

## Security

```bash
# Create secrets
kubectl create secret generic healthdata-secrets \
  -n healthdata \
  --from-literal=db-password='xxx' \
  --from-literal=jwt-secret='xxx' \
  --from-literal=redis-password='xxx'

# View secrets (base64 encoded)
kubectl get secret healthdata-secrets -n healthdata -o yaml

# Decode secret
kubectl get secret healthdata-secrets -n healthdata -o jsonpath='{.data.db-password}' | base64 -d
```

## Scaling

```bash
# Manual scaling
kubectl scale deployment healthdata-platform -n healthdata --replicas=10

# Auto-scaling (HPA)
kubectl autoscale deployment healthdata-platform \
  -n healthdata \
  --min=3 \
  --max=10 \
  --cpu-percent=70

# View HPA status
kubectl get hpa -n healthdata
```

## Monitoring

```bash
# Prometheus metrics
kubectl port-forward -n healthdata svc/healthdata-platform 8080:8080
curl http://localhost:8080/actuator/prometheus

# Application info
curl http://localhost:8080/actuator/info

# All actuator endpoints
curl http://localhost:8080/actuator
```
