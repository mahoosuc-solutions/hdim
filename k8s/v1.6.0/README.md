# HDIM Kubernetes Deployment v1.6.0

## Overview

Kubernetes manifests for deploying HDIM Platform v1.6.0 with HIPAA-compliant configurations.

## Prerequisites

- Kubernetes 1.28+
- kubectl configured with cluster access
- PostgreSQL database accessible from cluster
- Redis accessible from cluster
- Container registry with HDIM images

## Quick Start

```bash
# 1. Create namespace and base resources
kubectl apply -f namespace.yaml

# 2. Create secrets (from template)
cp secrets-template.yaml secrets.yaml
# Edit secrets.yaml with actual values
kubectl apply -f secrets.yaml
rm secrets.yaml  # Don't leave secrets on disk

# 3. Apply ConfigMaps
kubectl apply -f configmap.yaml

# 4. Deploy services
kubectl apply -f cql-engine-deployment.yaml

# Or use kustomize:
kubectl apply -k .
```

## HIPAA Compliance

All deployments are configured for HIPAA compliance:

| Setting | Value | Requirement |
|---------|-------|-------------|
| Cache TTL | 300000ms (5 min) | PHI must not be cached >5 min |
| Pod Security | Non-root user | Principle of least privilege |
| Secrets | External | No hardcoded credentials |
| TLS | Required | Encryption in transit |

## Resource Requirements

| Service | CPU Request | CPU Limit | Memory Request | Memory Limit |
|---------|-------------|-----------|----------------|--------------|
| cql-engine | 500m | 1000m | 1Gi | 2Gi |
| fhir-service | 500m | 1000m | 1Gi | 2Gi |
| patient-service | 250m | 500m | 512Mi | 1Gi |
| quality-measure | 500m | 1000m | 1Gi | 2Gi |

## Scaling

Horizontal Pod Autoscaler (HPA) is configured for each service:

- **Min replicas:** 2 (high availability)
- **Max replicas:** 10
- **Scale up at:** 70% CPU or 80% memory

## Monitoring

Prometheus annotations are included on all pods:

```yaml
annotations:
  prometheus.io/scrape: "true"
  prometheus.io/port: "8081"
  prometheus.io/path: "/actuator/prometheus"
```

## Secrets Management

**Never commit secrets to git!**

Use one of these approaches:
1. External Secrets Operator
2. HashiCorp Vault
3. AWS Secrets Manager
4. Azure Key Vault

Example with kubectl:
```bash
kubectl create secret generic hdim-db-credentials \
  --namespace=hdim-production \
  --from-literal=username=healthdata \
  --from-literal=password=$(openssl rand -base64 24)
```

## Troubleshooting

### Check pod status
```bash
kubectl get pods -n hdim-production
kubectl describe pod <pod-name> -n hdim-production
```

### View logs
```bash
kubectl logs -f <pod-name> -n hdim-production
```

### Check health
```bash
kubectl exec -it <pod-name> -n hdim-production -- \
  wget -qO- http://localhost:8081/cql-engine/actuator/health
```

## Version History

- **v1.6.0** - HIPAA cache compliance update
- **v1.5.0** - Previous stable release
