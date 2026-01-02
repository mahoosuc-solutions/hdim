# CI/CD Pipeline - Quick Reference Guide

## 🚀 Quick Start

### Running Workflows Locally

**Test Backend:**
```bash
cd backend
./gradlew test
```

**Test Frontend:**
```bash
# Angular
cd apps/clinical-portal && npm test

# React
cd frontend && npm test
```

**Build Docker Images:**
```bash
# All services
docker-compose build

# Specific service
docker build -f backend/modules/services/fhir-service/Dockerfile \
  -t fhir-service:latest backend/modules/services/fhir-service
```

### Deployment Commands

**Deploy to Staging:**
```bash
./scripts/deploy-kubernetes.sh staging latest
```

**Deploy to Production:**
```bash
./scripts/deploy-kubernetes.sh production v1.2.3
```

**Run Smoke Tests:**
```bash
./scripts/smoke-tests.sh https://staging.healthdata.example.com
```

**Rollback Deployment:**
```bash
./scripts/rollback.sh production
```

---

## 📋 Workflow Overview

| Workflow | Trigger | Duration | Purpose |
|----------|---------|----------|---------|
| **CI** | Push, PR | ~15 min | Build, test, validate |
| **Security** | Push, PR, Weekly | ~12 min | Vulnerability scanning |
| **Deploy** | Push to main | ~20 min | Deploy to staging/prod |
| **PR Checks** | PR opened/updated | ~3 min | Validate PR quality |
| **Release** | Tag push | ~25 min | Create release artifacts |

---

## 🔧 GitHub Secrets Required

```bash
# Required
GITHUB_TOKEN          # Automatic
SONAR_TOKEN           # SonarQube token
SONAR_HOST_URL        # SonarQube URL

# Deployment
KUBE_CONFIG_STAGING     # Kubernetes config (base64)
KUBE_CONFIG_PRODUCTION  # Kubernetes config (base64)

# Notifications
SLACK_WEBHOOK         # Slack webhook URL
```

**Setting Secrets:**
```bash
gh secret set SONAR_TOKEN < token.txt
gh secret set SLACK_WEBHOOK < webhook.txt
```

---

## 🧪 Testing Strategy

### Test Levels

1. **Unit Tests** - Run on every push
2. **Integration Tests** - Run on every push
3. **Smoke Tests** - Run after deployment
4. **Security Tests** - Run on push + weekly

### Coverage Targets

- Backend: 80%+
- Frontend: 70%+
- Integration: 60%+

---

## 🔒 Security Scanning Tools

| Tool | Purpose | Frequency |
|------|---------|-----------|
| **CodeQL** | Static code analysis | Every push |
| **Trivy** | Container scanning | Every push |
| **OWASP DC** | Dependency check | Every push |
| **NPM Audit** | JS dependencies | Every push |
| **TruffleHog** | Secret scanning | Every push |
| **SonarQube** | Code quality | Push to main |

---

## 📦 Deployment Environments

### Staging
- **URL:** https://staging.healthdata.example.com
- **Auto-deploy:** On merge to develop
- **Approval:** Not required

### Production
- **URL:** https://healthdata.example.com
- **Auto-deploy:** On merge to main
- **Approval:** Required (2 reviewers)

---

## 🚨 Emergency Procedures

### Rollback Production

**Quick Rollback:**
```bash
# Using script
./scripts/rollback.sh production

# Using kubectl
kubectl rollout undo deployment/healthdata-platform -n production
```

**Verify Rollback:**
```bash
kubectl rollout status deployment/healthdata-platform -n production
./scripts/smoke-tests.sh https://healthdata.example.com
```

### Disable Auto-Deploy

**Temporarily disable workflow:**
1. Go to Actions → Workflows → Deploy to Production
2. Click "Disable workflow"

**Re-enable when ready**

---

## 📊 Monitoring

### View Workflow Runs

**Web UI:**
- Go to Actions tab
- Select workflow
- View run details

**CLI:**
```bash
# List runs
gh run list

# View specific run
gh run view <run-id>

# Download artifacts
gh run download <run-id>
```

### Check Deployment Status

```bash
# Kubernetes
kubectl get deployments -n production
kubectl get pods -n production

# Health check
curl https://healthdata.example.com/actuator/health
```

---

## 🔄 Release Process

### Creating a Release

**1. Tag the version:**
```bash
git tag -a v1.2.3 -m "Release version 1.2.3"
git push origin v1.2.3
```

**2. Workflow automatically:**
- Builds all services
- Creates Docker images
- Generates release notes
- Publishes GitHub release

**3. Deploy to production:**
- Automatic after release creation
- Requires approval

### Version Numbering

- **Major:** Breaking changes (v2.0.0)
- **Minor:** New features (v1.3.0)
- **Patch:** Bug fixes (v1.2.5)

---

## 🛠️ Troubleshooting

### Build Failures

**Check logs:**
```bash
gh run view <run-id> --log-failed
```

**Common issues:**
- Cache corruption → Clear cache
- Test failures → Check test data
- Dependency issues → Update dependencies

### Deployment Failures

**Check pod status:**
```bash
kubectl get pods -n production
kubectl describe pod <pod-name> -n production
kubectl logs <pod-name> -n production
```

**Common issues:**
- Image pull errors → Check registry
- Resource limits → Adjust resources
- ConfigMap missing → Apply configs

### Rollback Issues

**Verify revisions exist:**
```bash
kubectl rollout history deployment/healthdata-platform -n production
```

**Manual rollback:**
```bash
kubectl rollout undo deployment/healthdata-platform -n production --to-revision=5
```

---

## 📞 Support Contacts

- **DevOps Team:** #devops-support
- **Security Team:** #security-alerts
- **On-call:** Refer to PagerDuty

---

## 📚 Additional Resources

- **Full Documentation:** [CICD_IMPLEMENTATION_COMPLETE.md](./CICD_IMPLEMENTATION_COMPLETE.md)
- **GitHub Actions Docs:** https://docs.github.com/actions
- **Kubernetes Docs:** https://kubernetes.io/docs
- **Security Policies:** [SECURITY.md](./SECURITY.md)

---

**Last Updated:** 2025-12-01
**Maintained By:** Agent 4B - TDD Swarm
