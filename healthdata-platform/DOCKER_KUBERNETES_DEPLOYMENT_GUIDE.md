# HealthData Platform - Docker & Kubernetes Deployment Guide

## Table of Contents
- [Overview](#overview)
- [Docker Deployment](#docker-deployment)
- [Kubernetes Deployment](#kubernetes-deployment)
- [Helm Deployment](#helm-deployment)
- [Production Checklist](#production-checklist)
- [Monitoring & Observability](#monitoring--observability)
- [Troubleshooting](#troubleshooting)

---

## Overview

The HealthData Platform provides production-ready containerization and orchestration configurations:

- **Multi-stage Docker builds** for optimized image sizes
- **Kubernetes manifests** for declarative infrastructure
- **Helm charts** for templated deployments
- **Automated deployment scripts** for CI/CD integration
- **Security hardening** following industry best practices
- **Auto-scaling** based on CPU/memory metrics
- **High availability** with pod anti-affinity rules

### Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Load Balancer                        │
│                   (Ingress/NGINX)                       │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│              HealthData Platform Pods (3+)              │
│                  (Auto-Scaling)                         │
└─────────────────────────────────────────────────────────┘
        │               │               │
        ▼               ▼               ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│  PostgreSQL  │ │    Redis     │ │    Kafka     │
│   Database   │ │    Cache     │ │   Messaging  │
└──────────────┘ └──────────────┘ └──────────────┘
```

---

## Docker Deployment

### Prerequisites

- Docker 20.10+
- Docker Compose 2.0+
- 4GB RAM minimum
- 20GB disk space

### Quick Start with Docker Compose

1. **Clone the repository:**
```bash
git clone https://github.com/healthdata-in-motion/healthdata-platform.git
cd healthdata-platform
```

2. **Create environment file:**
```bash
cat > .env << EOF
# Database
DB_NAME=healthdata
DB_USER=healthdata
DB_PASSWORD=your_secure_password_here

# Redis
REDIS_PASSWORD=your_redis_password_here

# Security
JWT_SECRET=your_jwt_secret_at_least_256_bits_here

# Application
VERSION=2.0.0
ENVIRONMENT=production
EOF
```

3. **Build and start services:**
```bash
# Production deployment
docker-compose -f docker-compose.prod.yml up -d

# View logs
docker-compose -f docker-compose.prod.yml logs -f

# Check health
curl http://localhost:8080/actuator/health
```

### Production Docker Build

Build optimized production image:

```bash
# Build with multi-stage Dockerfile
docker build -f Dockerfile.production -t healthdata/platform:2.0.0 .

# Run container
docker run -d \
  --name healthdata-platform \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/healthdata \
  -e SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD} \
  -e JWT_SECRET=${JWT_SECRET} \
  healthdata/platform:2.0.0
```

### Docker Image Features

✅ Multi-stage build (builder + runtime)
✅ Non-root user (healthdata:1000)
✅ JVM optimization for containers
✅ Health checks built-in
✅ Security scanning ready
✅ Size optimized (~300MB)

---

## Kubernetes Deployment

### Prerequisites

- Kubernetes 1.24+
- kubectl configured
- 3 worker nodes minimum (for HA)
- Ingress controller (NGINX recommended)
- cert-manager (for TLS)

### Manual Kubernetes Deployment

1. **Create namespace:**
```bash
kubectl apply -f k8s/namespace.yaml
```

2. **Create secrets:**
```bash
# Create from literals
kubectl create secret generic healthdata-secrets \
  -n healthdata \
  --from-literal=db-password='your_password' \
  --from-literal=jwt-secret='your_jwt_secret' \
  --from-literal=redis-password='your_redis_password'

# Or from file
kubectl apply -f k8s/secrets.yaml
```

3. **Apply configurations:**
```bash
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/serviceaccount.yaml
```

4. **Deploy application:**
```bash
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/ingress.yaml
kubectl apply -f k8s/hpa.yaml
```

5. **Verify deployment:**
```bash
# Check pods
kubectl get pods -n healthdata

# Check deployment status
kubectl rollout status deployment/healthdata-platform -n healthdata

# Check services
kubectl get svc -n healthdata

# View logs
kubectl logs -n healthdata -l app=healthdata-platform -f
```

### Using Deployment Script

```bash
# Set environment variables
export DOCKER_REGISTRY=docker.io
export DOCKER_NAMESPACE=healthdata
export VERSION=2.0.0
export KUBE_NAMESPACE=healthdata

# Run deployment
./scripts/deployment/deploy-production.sh
```

---

## Helm Deployment

### Prerequisites

- Helm 3.0+
- kubectl configured
- Kubernetes cluster access

### Quick Start with Helm

1. **Install using Helm:**
```bash
# Add repository (if published)
helm repo add healthdata https://charts.healthdata-platform.io
helm repo update

# Or install from local chart
cd helm/healthdata-platform

# Dry run to preview
helm install healthdata-platform . \
  --namespace healthdata \
  --create-namespace \
  --dry-run --debug

# Install
helm install healthdata-platform . \
  --namespace healthdata \
  --create-namespace \
  --set image.tag=2.0.0
```

2. **Create secrets before installation:**
```bash
kubectl create secret generic healthdata-secrets \
  -n healthdata \
  --from-literal=db-password='your_password' \
  --from-literal=jwt-secret='your_jwt_secret' \
  --from-literal=redis-password='your_redis_password'
```

3. **Customize values:**
```bash
# Create custom values file
cat > custom-values.yaml << EOF
replicaCount: 5

image:
  tag: "2.0.0"

ingress:
  enabled: true
  hosts:
    - host: healthdata.yourdomain.com
      paths:
        - path: /
          pathType: Prefix

resources:
  limits:
    cpu: 4000m
    memory: 4Gi
  requests:
    cpu: 1000m
    memory: 1Gi

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 20
  targetCPUUtilizationPercentage: 60
EOF

# Install with custom values
helm install healthdata-platform . \
  -f custom-values.yaml \
  -n healthdata \
  --create-namespace
```

### Using Helm Deployment Script

```bash
# Set environment variables
export NAMESPACE=healthdata
export RELEASE_NAME=healthdata-platform
export IMAGE_TAG=2.0.0

# Run deployment
./scripts/deployment/deploy-kubernetes.sh

# Dry run mode
DRY_RUN=true ./scripts/deployment/deploy-kubernetes.sh
```

### Helm Commands Reference

```bash
# View release status
helm status healthdata-platform -n healthdata

# View release history
helm history healthdata-platform -n healthdata

# Upgrade release
helm upgrade healthdata-platform . \
  -n healthdata \
  -f values.yaml

# Rollback to previous version
helm rollback healthdata-platform -n healthdata

# Rollback to specific revision
helm rollback healthdata-platform 3 -n healthdata

# Uninstall release
helm uninstall healthdata-platform -n healthdata
```

---

## Production Checklist

### Before Deployment

- [ ] Review and update all secrets
- [ ] Configure ingress domain and TLS certificates
- [ ] Set appropriate resource limits
- [ ] Configure database connection pooling
- [ ] Enable monitoring and logging
- [ ] Set up backup procedures
- [ ] Review security policies
- [ ] Configure network policies
- [ ] Set up alert rules

### Security Hardening

```yaml
# Pod Security Standards
apiVersion: v1
kind: Namespace
metadata:
  name: healthdata
  labels:
    pod-security.kubernetes.io/enforce: restricted
    pod-security.kubernetes.io/audit: restricted
    pod-security.kubernetes.io/warn: restricted
```

### Resource Planning

| Component | CPU Request | CPU Limit | Memory Request | Memory Limit |
|-----------|-------------|-----------|----------------|--------------|
| Application | 500m | 2000m | 512Mi | 2Gi |
| PostgreSQL | 500m | 2000m | 512Mi | 2Gi |
| Redis | 250m | 1000m | 256Mi | 1Gi |
| Kafka | 250m | 1000m | 512Mi | 1Gi |

### High Availability Configuration

```yaml
# Minimum 3 replicas
replicaCount: 3

# Pod Disruption Budget
podDisruptionBudget:
  enabled: true
  minAvailable: 2

# Pod Anti-Affinity
affinity:
  podAntiAffinity:
    preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 100
        podAffinityTerm:
          topologyKey: kubernetes.io/hostname
```

---

## Monitoring & Observability

### Health Endpoints

```bash
# Liveness probe
curl http://localhost:8080/actuator/health/liveness

# Readiness probe
curl http://localhost:8080/actuator/health/readiness

# Full health
curl http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus
```

### Prometheus Integration

```yaml
# ServiceMonitor for Prometheus Operator
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: healthdata-platform
  namespace: healthdata
spec:
  selector:
    matchLabels:
      app: healthdata-platform
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
```

### Logging

```bash
# View application logs
kubectl logs -n healthdata -l app=healthdata-platform -f

# View logs from specific pod
kubectl logs -n healthdata <pod-name> -f

# View logs from previous pod instance
kubectl logs -n healthdata <pod-name> --previous

# View logs with timestamps
kubectl logs -n healthdata <pod-name> -f --timestamps
```

---

## Troubleshooting

### Common Issues

#### Pods Not Starting

```bash
# Check pod status
kubectl get pods -n healthdata

# Describe pod for events
kubectl describe pod <pod-name> -n healthdata

# Check pod logs
kubectl logs <pod-name> -n healthdata

# Check init container logs
kubectl logs <pod-name> -n healthdata -c wait-for-postgres
```

#### Database Connection Issues

```bash
# Test database connectivity
kubectl run -it --rm debug \
  --image=postgres:16 \
  --restart=Never \
  --namespace=healthdata -- \
  psql -h healthdata-postgres -U healthdata -d healthdata

# Check database service
kubectl get svc healthdata-postgres -n healthdata

# Check database endpoints
kubectl get endpoints healthdata-postgres -n healthdata
```

#### Image Pull Errors

```bash
# Check image pull secrets
kubectl get secrets -n healthdata

# Describe deployment for pull errors
kubectl describe deployment healthdata-platform -n healthdata

# Manually pull image
docker pull healthdata/platform:2.0.0
```

#### Resource Constraints

```bash
# Check resource usage
kubectl top pods -n healthdata

# Check node resources
kubectl top nodes

# Describe node for pressure
kubectl describe node <node-name>
```

### Rollback Procedures

```bash
# Using rollback script
./scripts/deployment/rollback.sh

# Manual kubectl rollback
kubectl rollout undo deployment/healthdata-platform -n healthdata

# Rollback to specific revision
kubectl rollout undo deployment/healthdata-platform -n healthdata --to-revision=2

# Rollback using Helm
helm rollback healthdata-platform -n healthdata
```

### Debug Commands

```bash
# Execute shell in pod
kubectl exec -it <pod-name> -n healthdata -- /bin/sh

# Port forward to local machine
kubectl port-forward -n healthdata svc/healthdata-platform 8080:8080

# Check events
kubectl get events -n healthdata --sort-by='.lastTimestamp'

# Check resource quotas
kubectl describe quota -n healthdata

# Check network policies
kubectl get networkpolicies -n healthdata
```

---

## Performance Tuning

### JVM Tuning

```bash
# In Dockerfile.production or deployment
ENV JAVA_OPTS="-XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:InitialRAMPercentage=50.0 \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseStringDeduplication \
  -XX:+ParallelRefProcEnabled"
```

### Database Connection Pool

```yaml
# In ConfigMap
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE: "20"
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE: "5"
SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT: "30000"
```

### Auto-Scaling Configuration

```yaml
autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
  targetMemoryUtilizationPercentage: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 10
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
```

---

## Support

For issues, questions, or contributions:
- GitHub: https://github.com/healthdata-in-motion/healthdata-platform
- Email: team@healthdata-platform.io
- Documentation: https://docs.healthdata-platform.io

---

## License

MIT License - See LICENSE file for details
