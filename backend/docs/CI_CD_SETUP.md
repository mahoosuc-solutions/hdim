# CI/CD Pipeline Setup Guide

**Status:** ✅ Implemented (Phase 1 - Core Pipeline)
**Last Updated:** January 24, 2026

## Overview

The HDIM backend uses GitHub Actions for continuous integration and deployment, providing automated build, test, and security scanning for all 87 Gradle projects.

**Pipeline Location:** `.github/workflows/backend-ci.yml`

---

## Pipeline Stages

### Stage 1: Build & Test (45 minutes)

**Triggers:**
- Push to `master` or `develop` branches
- Pull requests to `master` or `develop`
- Manual workflow dispatch

**Services:**
- PostgreSQL 16 (test database)
- Redis 7 (caching layer)

**Steps:**
1. **Checkout & Setup**
   - Fetch full repository history
   - Set up JDK 21 (Temurin)
   - Cache Gradle dependencies

2. **Build**
   ```bash
   ./gradlew build -x test --no-daemon --console=plain
   ```
   - Compiles all 87 services
   - Validates Liquibase migrations
   - Checks code quality

3. **Unit Tests**
   ```bash
   ./gradlew test --no-daemon --console=plain --continue
   ```
   - Runs JUnit tests for all services
   - Connects to PostgreSQL and Redis test services
   - Continues on failure to collect all results

4. **Test Reporting**
   - Publishes test results as GitHub Check
   - Comments on PRs with test failures
   - Uploads coverage reports (JaCoCo)
   - Retains artifacts for 30 days

**Artifacts:**
- `build-artifacts` - All service JAR files (7 days retention)
- `test-coverage` - JaCoCo reports and test results (30 days)

---

### Stage 2: Security Scan (20 minutes)

**Runs:** After successful build & test

**Tools:**
1. **Trivy** - Filesystem vulnerability scanner
   - Scans all dependencies and code
   - Detects CRITICAL and HIGH severity issues
   - Outputs SARIF format for GitHub Security

2. **OWASP Dependency Check** (optional)
   - Analyzes Java dependencies for CVEs
   - Generates HTML report
   - Continues on failure (non-blocking)

**Artifacts:**
- `security-reports` - Trivy SARIF + OWASP HTML (30 days)
- GitHub Security tab populated with findings

---

### Stage 3: Code Quality (20 minutes)

**Runs:** Only on push to `master` or `develop`

**Requires:** SonarQube configuration (optional)

**Analysis:**
- Code coverage metrics
- Code smells and technical debt
- Security hotspots
- Duplication analysis

**Configuration:**
```yaml
secrets:
  SONAR_TOKEN: <your-token>
  SONAR_HOST_URL: https://sonarcloud.io
```

If `SONAR_TOKEN` is not set, this stage is skipped gracefully.

---

## Required GitHub Secrets

### Mandatory (None)
The pipeline works out-of-the-box without any secrets.

### Optional
| Secret | Purpose | Used In |
|--------|---------|---------|
| `SONAR_TOKEN` | SonarQube authentication | Code Quality stage |
| `SONAR_HOST_URL` | SonarQube server URL | Code Quality stage |

**To add secrets:** Settings → Secrets and variables → Actions → New repository secret

---

## Build Status Badge

Add this to your README.md:

```markdown
![Backend CI](https://github.com/YOUR_ORG/hdim-master/workflows/Backend%20CI%2FCD%20Pipeline/badge.svg)
```

---

## Local Development Testing

Test the build locally before pushing:

```bash
# Full build
./gradlew build --console=plain

# Run tests with services
docker compose up -d postgres redis
export DATABASE_URL=jdbc:postgresql://localhost:5435/test_db
export SPRING_DATA_REDIS_HOST=localhost
./gradlew test --console=plain

# Security scan
./gradlew dependencyCheckAnalyze
```

---

## Troubleshooting

### Build Fails: "Connection refused to PostgreSQL"

**Cause:** Test database service not healthy
**Fix:** Pipeline waits for health checks automatically. If issue persists, check PostgreSQL service configuration in workflow.

---

### Build Fails: "Could not resolve dependencies"

**Cause:** Gradle cache corruption or network issues
**Fix:**
1. Check if dependencies are available in Maven Central
2. Clear GitHub Actions cache: Settings → Actions → Caches → Delete all

---

### Tests Fail Locally but Pass in CI

**Cause:** Environment differences (database versions, Redis config)
**Fix:**
```bash
# Use exact CI versions
docker compose -f docker-compose.ci.yml up -d
```

---

### Security Scan False Positives

**Cause:** Trivy reports vulnerabilities in transitive dependencies
**Fix:**
1. Review findings in GitHub Security tab
2. Update dependencies: `./gradlew dependencyUpdates`
3. If unavoidable, create `.trivyignore` file

---

## Performance Optimization

### Current Timings
- Build & Test: ~45 minutes (87 services)
- Security Scan: ~20 minutes
- Code Quality: ~20 minutes
- **Total:** ~1 hour 25 minutes

### Optimization Strategies

**1. Selective Testing (Not Implemented)**
```yaml
# Only test changed services
- name: Detect changed services
  id: changes
  uses: dorny/paths-filter@v2
  with:
    filters: .github/service-filters.yml

- name: Run tests for changed services
  run: ./gradlew ${{ steps.changes.outputs.services }}:test
```

**2. Parallel Service Builds (Future)**
```yaml
strategy:
  matrix:
    service: [patient-service, care-gap-service, ...]
```

**3. Gradle Build Cache (Future)**
- Use GitHub Actions cache for Gradle build cache
- Share cache across jobs

---

## Future Enhancements

### Phase 2: Docker Image Builds
- [ ] Build Docker images for each service
- [ ] Push to GitHub Container Registry (GHCR)
- [ ] Tag with commit SHA and branch name
- [ ] Use Docker layer caching

### Phase 3: Deployment Automation
- [ ] Auto-deploy `develop` → Staging
- [ ] Manual approval for `master` → Production
- [ ] Kubernetes deployment manifests
- [ ] Smoke tests after deployment

### Phase 4: Advanced Features
- [ ] Integration tests with Testcontainers
- [ ] Performance benchmarking
- [ ] API contract testing
- [ ] Database migration testing

---

## Related Documentation

- [Build Management Guide](./BUILD_MANAGEMENT_GUIDE.md)
- [Testing Strategy](./TESTING_STRATEGY.md)
- [Deployment Runbook](../docs/DEPLOYMENT_RUNBOOK.md)
- [Security Scanning Guide](./SECURITY_SCANNING.md)

---

## Issue Reference

- **Issue:** #270 - CI/CD Pipeline - Backend
- **Status:** Phase 1 Complete ✅
- **Implementation Date:** January 24, 2026
