# HealthData Platform - Deployment Scripts

## Overview

This directory contains production-ready deployment scripts for the HealthData Platform.

## Scripts

### 1. deploy-production.sh

Complete production deployment script that:
- Builds the application with Gradle
- Creates Docker image using multi-stage build
- Runs security scans (Trivy)
- Pushes image to registry
- Backs up current deployment
- Deploys to Kubernetes
- Waits for rollout completion
- Runs health checks and smoke tests

**Usage:**
```bash
# Basic usage
./deploy-production.sh

# With environment variables
VERSION=2.0.0 \
DOCKER_REGISTRY=docker.io \
DOCKER_NAMESPACE=healthdata \
KUBE_NAMESPACE=healthdata \
./deploy-production.sh
```

**Environment Variables:**
- `VERSION` - Image version tag (default: 2.0.0)
- `ENVIRONMENT` - Deployment environment (default: production)
- `DOCKER_REGISTRY` - Docker registry URL (default: docker.io)
- `DOCKER_NAMESPACE` - Docker namespace (default: healthdata)
- `IMAGE_NAME` - Image name (default: platform)
- `KUBE_CONTEXT` - Kubernetes context to use
- `KUBE_NAMESPACE` - Kubernetes namespace (default: healthdata)
- `DOCKER_USERNAME` - Registry username
- `DOCKER_PASSWORD` - Registry password

### 2. deploy-kubernetes.sh

Helm-based deployment script that:
- Validates prerequisites (Helm, kubectl)
- Creates namespace
- Creates secrets (interactive)
- Lints Helm chart
- Deploys/upgrades using Helm
- Waits for pods to be ready
- Runs tests
- Displays deployment status

**Usage:**
```bash
# Basic usage
./deploy-kubernetes.sh

# Dry run
DRY_RUN=true ./deploy-kubernetes.sh

# With custom values
RELEASE_NAME=healthdata \
NAMESPACE=healthdata \
VALUES_FILE=custom-values.yaml \
./deploy-kubernetes.sh

# Run tests after deployment
RUN_TESTS=true ./deploy-kubernetes.sh

# Show logs after deployment
SHOW_LOGS=true ./deploy-kubernetes.sh
```

**Environment Variables:**
- `RELEASE_NAME` - Helm release name (default: healthdata-platform)
- `NAMESPACE` - Kubernetes namespace (default: healthdata)
- `CHART_PATH` - Path to Helm chart
- `VALUES_FILE` - Path to values file
- `HELM_TIMEOUT` - Helm operation timeout (default: 600s)
- `DRY_RUN` - Run in dry-run mode (default: false)
- `EXTRA_VALUES_FILE` - Additional values file
- `IMAGE_TAG` - Override image tag
- `REPLICA_COUNT` - Override replica count
- `RUN_TESTS` - Run Helm tests (default: false)
- `SHOW_LOGS` - Show logs after deployment (default: false)

### 3. rollback.sh

Rollback deployment to previous version:
- Shows deployment and Helm history
- Displays current state
- Prompts for confirmation
- Backs up current state
- Performs rollback (Helm or kubectl)
- Waits for rollout
- Verifies rollback
- Shows status

**Usage:**
```bash
# Rollback to previous version
./rollback.sh

# Rollback to specific revision
REVISION=3 ./rollback.sh

# Show logs after rollback
SHOW_LOGS=true ./rollback.sh
```

**Environment Variables:**
- `NAMESPACE` - Kubernetes namespace (default: healthdata)
- `RELEASE_NAME` - Helm release name (default: healthdata-platform)
- `DEPLOYMENT_NAME` - Deployment name (default: healthdata-platform)
- `REVISION` - Specific revision to rollback to (0 = previous)
- `SHOW_LOGS` - Show logs after rollback (default: false)

## Prerequisites

### All Scripts
- Linux/macOS environment
- kubectl installed and configured
- Kubernetes cluster access

### deploy-production.sh
- Docker installed
- Gradle installed (or use ./gradlew)
- Registry credentials (if pushing)
- Trivy (optional, for security scanning)

### deploy-kubernetes.sh
- Helm 3.x installed
- Database password
- JWT secret
- Redis password

### rollback.sh
- Active deployment to rollback

## Security Notes

### Secrets Management

Never hardcode secrets in scripts. Use environment variables or secret management tools:

```bash
# From environment
export DB_PASSWORD="your_password"
export JWT_SECRET="your_jwt_secret"
export REDIS_PASSWORD="your_redis_password"

# From file (gitignored)
source .env.production

# From vault
export DB_PASSWORD=$(vault kv get -field=password secret/healthdata/db)
```

### Registry Authentication

```bash
# Docker login
echo "${DOCKER_PASSWORD}" | docker login "${DOCKER_REGISTRY}" -u "${DOCKER_USERNAME}" --password-stdin

# Or use credential helper
docker-credential-ecr-login
```

## Logging

All scripts log to:
```
/home/webemo-aaron/projects/healthdata-in-motion/healthdata-platform/logs/
```

Log files:
- `deployment-YYYYMMDD-HHMMSS.log` - Production deployment
- `helm-deployment-YYYYMMDD-HHMMSS.log` - Helm deployment
- `rollback-YYYYMMDD-HHMMSS.log` - Rollback

## Backups

Backups created in:
```
/home/webemo-aaron/projects/healthdata-in-motion/healthdata-platform/backups/
```

Backup directories:
- `deployment-YYYYMMDD-HHMMSS/` - Pre-deployment backups
- `rollback-backup-YYYYMMDD-HHMMSS/` - Pre-rollback backups

## CI/CD Integration

### GitLab CI

```yaml
deploy:production:
  stage: deploy
  script:
    - export VERSION=$CI_COMMIT_TAG
    - ./scripts/deployment/deploy-production.sh
  only:
    - tags
  environment:
    name: production
```

### GitHub Actions

```yaml
- name: Deploy to Production
  env:
    VERSION: ${{ github.ref_name }}
    DOCKER_USERNAME: ${{ secrets.DOCKER_USERNAME }}
    DOCKER_PASSWORD: ${{ secrets.DOCKER_PASSWORD }}
  run: |
    ./scripts/deployment/deploy-production.sh
```

### Jenkins

```groovy
stage('Deploy') {
    environment {
        VERSION = "${GIT_TAG}"
        DOCKER_CREDS = credentials('docker-registry')
    }
    steps {
        sh './scripts/deployment/deploy-production.sh'
    }
}
```

## Troubleshooting

### Script Permissions

```bash
chmod +x scripts/deployment/*.sh
```

### Debug Mode

```bash
bash -x ./deploy-production.sh
```

### Check Prerequisites

```bash
# Docker
docker version

# kubectl
kubectl version

# Helm
helm version

# Kubernetes connection
kubectl cluster-info
```

### Common Issues

**Image pull errors:**
- Verify registry credentials
- Check image name and tag
- Ensure registry is accessible

**Deployment timeout:**
- Increase HELM_TIMEOUT
- Check pod logs: `kubectl logs -n healthdata <pod-name>`
- Check events: `kubectl get events -n healthdata`

**Rollback fails:**
- Ensure there is a previous revision
- Check deployment history: `kubectl rollout history deployment/healthdata-platform -n healthdata`

## Support

For issues or questions:
- Documentation: See `DOCKER_KUBERNETES_DEPLOYMENT_GUIDE.md`
- Quick reference: See `DOCKER_KUBERNETES_QUICK_REFERENCE.md`
- GitHub: https://github.com/healthdata-in-motion/healthdata-platform
