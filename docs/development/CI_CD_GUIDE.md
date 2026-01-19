# CI/CD PIPELINE GUIDE

Comprehensive guide to HDIM's GitHub Actions CI/CD pipelines.

**Last Updated**: January 19, 2026
**Status**: Phase 1.5 Blocker #4 - Pipeline Documentation
**Workflows**: 9 primary pipelines covering build, test, security, and deployment

---

## Overview

HDIM uses GitHub Actions for continuous integration and deployment across all 50+ microservices. This guide explains the pipeline architecture, workflow configurations, and best practices.

### Pipeline Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   Developer Commits Code                     │
└────────────────────────┬────────────────────────────────────┘
                         │
                ┌────────▼────────┐
                │  GitHub Actions │
                └────────┬────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
        ▼                ▼                ▼
    ┌────────┐      ┌────────┐      ┌──────────┐
    │  Build │      │  Test  │      │ Security │
    │ & Lint │      │Coverage│      │  Scan    │
    └────────┘      └────────┘      └──────────┘
        │                │                │
        └────────────────┼────────────────┘
                         │
                    ┌────▼────┐
                    │  Merge?  │
                    └────┬─────┘
                         │
            ┌────────────┴────────────┐
            │                         │
        ┌───▼──┐              ┌──────▼────┐
        │Stage │              │ Production │
        │Deply │              │  Deploy    │
        └──────┘              └───────────┘
```

### Core Workflows (9 Pipelines)

| Workflow | Trigger | Purpose | Status |
|----------|---------|---------|--------|
| **1. Build & Test** | Push to `main`, `develop`, PRs | Compile, unit tests, lint | Primary |
| **2. Integration Tests** | Push to `develop`, nightly | Database tests, E2E validation | Primary |
| **3. Code Quality** | All PRs | SonarQube analysis, coverage gates | Primary |
| **4. Security Scan** | All PRs, daily | SAST, dependency vulnerability check | Critical |
| **5. Container Build** | Push to `main`, release tags | Docker image build, registry push | Primary |
| **6. Staging Deploy** | PR merge to `develop` | Deploy to staging environment | Primary |
| **7. Production Deploy** | Release tagged, manual trigger | Canary → Blue/Green deployment | Critical |
| **8. Performance Test** | Nightly, on-demand | Load testing, benchmark comparison | Optional |
| **9. Dependency Update** | Weekly, scheduled | Automated dependency upgrades, PRs | Maintenance |

---

## Workflow #1: Build & Test Pipeline

The primary workflow that runs on every push and PR.

### Configuration

```yaml
# .github/workflows/build-test.yml
name: Build & Test

on:
  push:
    branches: [main, develop]
    paths:
      - 'backend/**'
      - '.github/workflows/build-test.yml'
  pull_request:
    branches: [main, develop]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        java-version: ['21']
    timeout-minutes: 60

    steps:
      - name: Checkout code
        uses: actions/checkout@v3
        with:
          fetch-depth: 0  # Required for SonarQube

      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: ${{ matrix.java-version }}
          distribution: 'temurin'
          cache: gradle

      - name: Build project
        run: ./gradlew build -x test --no-daemon
        working-directory: ./backend

      - name: Run unit tests
        run: ./gradlew test -x integrationTest --no-daemon
        working-directory: ./backend

      - name: Generate coverage report
        run: ./gradlew jacocoTestReport --no-daemon
        working-directory: ./backend

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-results
          path: backend/build/test-results/

      - name: Upload coverage report
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: coverage-report
          path: backend/build/reports/jacoco/

      - name: Comment PR with test results
        if: github.event_name == 'pull_request' && always()
        uses: dorny/test-reporter@v1
        with:
          name: Test Results
          path: 'backend/build/test-results/**/*.xml'
          reporter: 'java-junit'
```

### What This Workflow Does

✅ Compiles all services
✅ Runs 613 unit tests
✅ Generates code coverage report
✅ Uploads artifacts for analysis
✅ Comments PR with results

### Expected Duration

- **JDK 21**: ~45 minutes (with caching)
- **First run**: ~60 minutes (gradle wrapper download)

### Troubleshooting

```yaml
# If build times out (> 60 minutes):

# 1. Check Gradle cache
- name: Gradle Cache Status
  run: ./gradlew --status

# 2. Clean cache if needed
- name: Clean Gradle Cache
  run: ./gradlew cleanBuildCache

# 3. Enable parallel builds
- name: Build with parallelization
  run: ./gradlew build -x test --parallel --max-workers=4
```

---

## Workflow #2: Integration Tests

Database-heavy tests using real PostgreSQL and Kafka.

### Configuration

```yaml
# .github/workflows/integration-tests.yml
name: Integration Tests

on:
  push:
    branches: [develop, main]
  schedule:
    - cron: '0 2 * * *'  # Nightly at 2 AM UTC
  workflow_dispatch:

jobs:
  integration-tests:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_USER: testuser
          POSTGRES_PASSWORD: testpass
          POSTGRES_DB: test_db
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5435:5432

      kafka:
        image: confluentinc/cp-kafka:7.5.0
        env:
          KAFKA_ZOOKEEPER_CONNECT: localhost:2181
          KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9094
          KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'true'
        ports:
          - 9094:9094

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: gradle

      - name: Run integration tests
        run: ./gradlew integrationTest --no-daemon
        working-directory: ./backend
        env:
          POSTGRES_HOST: localhost
          POSTGRES_PORT: 5435
          KAFKA_BOOTSTRAP_SERVERS: localhost:9094

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: integration-test-results
          path: backend/build/test-results/

      - name: Notify on failure
        if: failure()
        uses: 8398a7/action-slack@v3
        with:
          status: ${{ job.status }}
          text: 'Integration tests failed on ${{ github.ref }}'
          webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

### Services Tested

- ✅ quality-measure-service (HEDIS calculation)
- ✅ patient-service (data access)
- ✅ cql-engine-service (CQL evaluation)
- ✅ care-gap-service (gap detection)
- ✅ fhir-service (FHIR R4 resources)

### Expected Duration

- ~30-45 minutes (with Testcontainers)

---

## Workflow #3: Code Quality (SonarQube)

SonarQube analysis for coverage gates and code smell detection.

### Configuration

```yaml
# .github/workflows/code-quality.yml
name: Code Quality

on:
  pull_request:
    branches: [main, develop]
  push:
    branches: [main, develop]

jobs:
  sonar:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3
        with:
          fetch-depth: 0  # Required for SonarQube

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          cache: gradle

      - name: Build and test
        run: ./gradlew build jacocoTestReport --no-daemon
        working-directory: ./backend

      - name: SonarQube Scan
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: |
          ./gradlew sonarqube \
            -Dsonar.projectKey=hdim-master \
            -Dsonar.host.url=https://sonarqube.example.com \
            -Dsonar.login=$SONAR_TOKEN \
            -Dsonar.scm.provider=git \
            -Dsonar.scm.forceReimport=true
        working-directory: ./backend
```

### Quality Gates

| Gate | Threshold | Status |
|------|-----------|--------|
| Coverage | >= 80% | Enforced |
| Duplicated Lines | < 5% | Warning |
| Code Smells | < 50 | Warning |
| Security Hotspots | 0 critical | Enforced |

### PR Comment

SonarQube automatically comments on PRs:

```
✅ Code Quality Check
├── Coverage: 82% ✓
├── Code Smells: 12 ⚠️
├── Duplications: 2.3% ✓
└── Security Issues: 0 ✓
```

---

## Workflow #4: Security Scanning

Multi-layer security checks for vulnerabilities.

### Configuration

```yaml
# .github/workflows/security-scan.yml
name: Security Scan

on:
  pull_request:
    branches: [main, develop]
  push:
    branches: [main, develop]
  schedule:
    - cron: '0 3 * * 0'  # Weekly Sunday 3 AM

jobs:
  dependency-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Run OWASP Dependency-Check
        uses: dependency-check/Dependency-Check_Action@main
        with:
          project: 'HDIM'
          path: '.'
          format: 'JSON'
          args: >
            --enableExperimental
            --enableRetired

      - name: Upload SARIF report
        uses: github/codeql-action/upload-sarif@v2
        with:
          sarif_file: dependency-check-report.sarif

  sast-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'

      - name: Run SpotBugs (SAST)
        run: ./gradlew spotbugsMain --no-daemon
        working-directory: ./backend

      - name: Upload findings
        uses: github/codeql-action/upload-sarif@v2
        if: always()
        with:
          sarif_file: backend/build/reports/spotbugs/main.sarif

  secret-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
        with:
          fetch-depth: 0

      - name: TruffleHog Secret Scan
        uses: trufflesecurity/trufflehog@main
        with:
          path: ./
          base: main
          head: HEAD
```

### Security Checks Performed

✅ Dependency vulnerabilities (CVE database)
✅ Static analysis (SpotBugs)
✅ Hardcoded secrets detection
✅ OWASP Top 10 issues

---

## Workflow #5: Container Build

Docker image building and registry push.

### Configuration

```yaml
# .github/workflows/container-build.yml
name: Container Build

on:
  push:
    branches: [main]
    tags: ['v*']
  workflow_dispatch:

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  build-matrix:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service:
          - patient-service
          - quality-measure-service
          - cql-engine-service
          - care-gap-service
          - fhir-service
          - gateway-service

    steps:
      - uses: actions/checkout@v3

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v2

      - name: Log in to Container Registry
        uses: docker/login-action@v2
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v4
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}-${{ matrix.service }}
          tags: |
            type=semver,pattern={{version}}
            type=semver,pattern={{major}}.{{minor}}
            type=sha
            type=ref,event=branch

      - name: Build and push
        uses: docker/build-push-action@v4
        with:
          context: backend/modules/services/${{ matrix.service }}
          push: ${{ github.event_name != 'pull_request' }}
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

      - name: Scan image with Trivy
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}-${{ matrix.service }}:${{ steps.meta.outputs.version }}
          format: 'sarif'
          output: 'trivy-results.sarif'

      - name: Upload Trivy results
        uses: github/codeql-action/upload-sarif@v2
        if: always()
        with:
          sarif_file: 'trivy-results.sarif'
```

### Image Management

```bash
# List built images
./gradlew docker images | grep hdim-master

# Manual image build
docker build -f docker/Dockerfile \
  -t ghcr.io/org/hdim-master-patient-service:v1.0.0 \
  backend/modules/services/patient-service/

# Push to registry
docker push ghcr.io/org/hdim-master-patient-service:v1.0.0
```

---

## Workflow #6: Staging Deployment

Deploy to staging environment on PR merge.

### Configuration

```yaml
# .github/workflows/staging-deploy.yml
name: Deploy to Staging

on:
  push:
    branches: [develop]
  workflow_dispatch:

jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: staging

    steps:
      - uses: actions/checkout@v3

      - name: Deploy with Helm
        env:
          KUBE_CONFIG: ${{ secrets.KUBE_CONFIG_STAGING }}
          DOCKER_REGISTRY: ${{ secrets.DOCKER_REGISTRY }}
        run: |
          # Setup kubectl
          mkdir -p $HOME/.kube
          echo "$KUBE_CONFIG" | base64 -d > $HOME/.kube/config

          # Deploy via Helm
          helm upgrade --install hdim ./k8s/helm/hdim \
            --namespace hdim-staging \
            --values ./k8s/helm/values-staging.yaml \
            --set docker.registry=$DOCKER_REGISTRY \
            --set docker.tag=${{ github.sha }} \
            --wait

      - name: Run smoke tests
        run: |
          npm install
          npx playwright test --grep @smoke
        working-directory: ./e2e
        env:
          BASE_URL: https://staging-api.example.com

      - name: Slack notification
        if: always()
        uses: 8398a7/action-slack@v3
        with:
          status: ${{ job.status }}
          text: 'Staging deployment ${{ job.status }}'
          webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

### Smoke Tests

Run before acceptance:

```typescript
// e2e/tests/smoke.spec.ts
test.describe('Smoke Tests @smoke', () => {
  test('API gateway should be healthy', async ({ page }) => {
    const response = await page.goto('https://api.staging.example.com/health');
    expect(response?.status()).toBe(200);
  });

  test('Should authenticate user', async ({ page }) => {
    await page.goto('https://staging.example.com/login');
    await page.fill('input[name="username"]', 'test@example.com');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button:has-text("Login")');
    await page.waitForURL(/dashboard/);
  });
});
```

---

## Workflow #7: Production Deployment

Canary deployment with automated rollback.

### Configuration

```yaml
# .github/workflows/production-deploy.yml
name: Deploy to Production

on:
  push:
    tags: ['v*']
  workflow_dispatch:
    inputs:
      deployment_strategy:
        description: 'Deployment strategy'
        required: true
        default: 'canary'
        type: choice
        options:
          - canary
          - blue-green
          - rolling

jobs:
  production-deploy:
    runs-on: ubuntu-latest
    environment: production
    timeout-minutes: 120

    steps:
      - uses: actions/checkout@v3

      - name: Deploy Canary
        if: github.event.inputs.deployment_strategy == 'canary'
        env:
          KUBE_CONFIG: ${{ secrets.KUBE_CONFIG_PROD }}
        run: |
          mkdir -p $HOME/.kube
          echo "$KUBE_CONFIG" | base64 -d > $HOME/.kube/config

          # Deploy 10% of traffic to new version
          kubectl patch virtualservice hdim \
            -p '{"spec":{"hosts":[{"name":"api.example.com","http":[{"match":[{"uri":{"prefix":"/"}}],"route":[{"destination":{"host":"hdim","subset":"v1"},"weight":90},{"destination":{"host":"hdim","subset":"v2"},"weight":10}]}]}}'

      - name: Monitor metrics (5 min)
        run: |
          for i in {1..30}; do
            kubectl get pods -n hdim -o wide
            kubectl top pod -n hdim
            sleep 10
          done

      - name: Check error rate
        run: |
          ERROR_RATE=$(curl -s http://prometheus:9090/api/v1/query \
            --data-urlencode 'query=rate(http_requests_total{status=~"5.."}[5m])' \
            | jq '.data.result[0].value[1]')

          if (( $(echo "$ERROR_RATE > 0.05" | bc -l) )); then
            echo "Error rate too high: $ERROR_RATE"
            exit 1
          fi

      - name: Promote to 100% (if successful)
        run: |
          kubectl patch virtualservice hdim \
            -p '{"spec":{"hosts":[{"name":"api.example.com","http":[{"match":[{"uri":{"prefix":"/"}}],"route":[{"destination":{"host":"hdim","subset":"v2"},"weight":100}]}]}}'

      - name: Rollback on failure
        if: failure()
        run: |
          kubectl rollout undo deployment/hdim -n hdim
          kubectl wait --for=condition=available --timeout=300s deployment/hdim -n hdim
```

### Pre-Deployment Checks

```yaml
jobs:
  pre-deployment-validation:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Validate manifests
        run: |
          kubectl apply --dry-run=client -f k8s/

      - name: Check database migrations
        run: |
          ./scripts/validate-liquibase-migrations.sh

      - name: Verify secrets
        run: |
          ./scripts/verify-secrets-in-vault.sh ${{ secrets.VAULT_TOKEN }}

      - name: Load test
        run: |
          npm install
          npx artillery run load-test.yml --output results.json
```

---

## Workflow #8: Performance Testing

Nightly load testing and benchmarking.

### Configuration

```yaml
# .github/workflows/performance-test.yml
name: Performance Tests

on:
  schedule:
    - cron: '0 4 * * *'  # Daily 4 AM UTC
  workflow_dispatch:

jobs:
  load-test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16-alpine
        # ... service config

    steps:
      - uses: actions/checkout@v3

      - name: Start services (Docker Compose)
        run: docker compose up -d --profile core

      - name: Wait for services
        run: |
          ./scripts/wait-for-services.sh \
            http://localhost:8087 \
            http://localhost:8084 \
            http://localhost:8085

      - name: Seed test data
        run: |
          npm install
          npx ts-node scripts/seed-test-data.ts \
            --patients 10000 \
            --measures 100

      - name: Run load test
        run: |
          npx artillery run performance/load-test.yml \
            --output results.json \
            --target http://localhost:8087

      - name: Compare with baseline
        run: |
          npm install
          npx ts-node scripts/compare-performance.ts \
            --current results.json \
            --baseline .perf-baseline.json \
            --threshold 10  # 10% degradation threshold

      - name: Update baseline
        if: github.event_name == 'workflow_dispatch'
        run: |
          cp results.json .perf-baseline.json
          git add .perf-baseline.json
          git commit -m "Update performance baseline"
          git push

      - name: Report results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: performance-results
          path: results.json
```

---

## Secrets Management

### Required Secrets

```yaml
# Settings > Secrets > Actions

SONAR_TOKEN              # SonarQube authentication
DOCKER_REGISTRY          # Docker registry credentials
KUBE_CONFIG_STAGING      # Kubernetes staging cluster config (base64)
KUBE_CONFIG_PROD         # Kubernetes production cluster config (base64)
SLACK_WEBHOOK            # Slack notifications webhook
VAULT_TOKEN              # HashiCorp Vault authentication
NPM_TOKEN                # npm registry token (if private packages)
```

### Using Secrets in Workflows

```yaml
- name: Authenticate to Docker Registry
  run: |
    echo ${{ secrets.DOCKER_REGISTRY }} | docker login -u ${{ github.actor }} --password-stdin

- name: Deploy to Kubernetes
  env:
    KUBE_CONFIG: ${{ secrets.KUBE_CONFIG_PROD }}
  run: |
    mkdir -p $HOME/.kube
    echo "$KUBE_CONFIG" | base64 -d > $HOME/.kube/config
    kubectl apply -f k8s/
```

### Adding Secrets (Admin)

```bash
# Via GitHub CLI
gh secret set SONAR_TOKEN --body "sonarqube-token-here"

# Via GitHub Web UI
Settings → Secrets and variables → Actions → New repository secret
```

---

## Build Caching

### Gradle Cache

```yaml
- name: Set up JDK with cache
  uses: actions/setup-java@v3
  with:
    java-version: '21'
    distribution: 'temurin'
    cache: gradle  # ✅ Enables automatic Gradle caching
```

### Docker Layer Caching

```yaml
- name: Set up Docker Buildx
  uses: docker/setup-buildx-action@v2

- name: Build with cache
  uses: docker/build-push-action@v4
  with:
    cache-from: type=gha    # GitHub Actions cache
    cache-to: type=gha,mode=max
```

### Cache Hit Rates

```yaml
- name: Print cache stats
  run: |
    ./gradlew --status | grep -i cache
```

Expected improvement: **30-50% faster builds** with caching enabled.

---

## Troubleshooting Common Issues

### Issue 1: Workflow Timeout

```yaml
# ERROR: The operation timed out

# FIX: Increase timeout
timeout-minutes: 120

# Or optimize job:
- name: Parallel builds
  run: ./gradlew build --parallel --max-workers=4
```

### Issue 2: Cache Miss

```yaml
# Cache keys not matching

# FIX: Check cache paths
- name: Debug cache
  run: |
    find ~/.gradle -type f -name "*.jar" | wc -l
    du -sh ~/.gradle/

# Clear cache if stale:
Settings → Caches → Clear repository caches
```

### Issue 3: Secret Not Available

```yaml
# ERROR: SONAR_TOKEN not set

# FIX: Verify secret name matches
# In workflow: ${{ secrets.SONAR_TOKEN }}
# In GitHub: Settings → Secrets → SONAR_TOKEN (case-sensitive)

# Debug:
- name: Check secrets
  run: |
    [ -z "$SONAR_TOKEN" ] && echo "NOT SET" || echo "SET"
  env:
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
```

### Issue 4: Service Connection Timeout

```yaml
# Kafka/PostgreSQL services fail to start

# FIX: Add health checks
services:
  postgres:
    options: >-
      --health-cmd pg_isready
      --health-interval 10s
      --health-timeout 5s
      --health-retries 5

# Wait for services
- name: Wait for services
  run: |
    until pg_isready -h localhost -p 5435; do sleep 1; done
```

---

## Best Practices

### 1. Workflow Organization

```yaml
# Keep workflows focused
# ✅ One concern per workflow
build-test.yml      # Build + unit tests
integration-tests.yml  # Integration tests
code-quality.yml    # SonarQube + coverage

# ❌ Don't combine unrelated jobs
# bad-workflow.yml   # Build, test, deploy, notify (too much)
```

### 2. Job Dependencies

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    # ... build steps

  test:
    needs: build  # Wait for build to complete
    runs-on: ubuntu-latest
    # ... test steps
```

### 3. Matrix Strategy for Multi-Service

```yaml
strategy:
  matrix:
    service:
      - patient-service
      - quality-measure-service
      - care-gap-service
  # Runs 3 jobs in parallel
```

### 4. Conditional Steps

```yaml
- name: Upload coverage
  if: always()  # Run even if tests fail
  uses: codecov/codecov-action@v3

- name: Deploy
  if: github.ref == 'refs/heads/main'  # Only on main
  run: ./deploy.sh

- name: Notify on failure
  if: failure()  # Only if previous steps failed
  run: notify-slack.sh
```

### 5. Artifact Management

```yaml
- name: Upload test results
  if: always()
  uses: actions/upload-artifact@v3
  with:
    name: test-results-${{ matrix.java-version }}
    path: build/test-results/
    retention-days: 30  # Auto-cleanup after 30 days
```

---

## Monitoring & Alerts

### Workflow Health Dashboard

```bash
# View all workflow runs
gh workflow list

# View recent runs
gh workflow view build-test.yml

# Check specific run details
gh run view <run-id>

# Stream logs
gh run view <run-id> --log
```

### Slack Notifications

```yaml
- name: Notify on failure
  if: failure()
  uses: 8398a7/action-slack@v3
  with:
    status: ${{ job.status }}
    text: |
      Workflow failed: ${{ github.workflow }}
      Branch: ${{ github.ref }}
      Commit: ${{ github.sha }}
      Author: ${{ github.actor }}
    webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

---

## Summary

HDIM's CI/CD pipeline ensures:

✅ **Quality**: Every PR tested and scanned
✅ **Safety**: Security checks before deployment
✅ **Speed**: Cached builds in ~45 minutes
✅ **Reliability**: Automated rollback on failure
✅ **Visibility**: PR comments with coverage/quality metrics
✅ **Automation**: From commit to production in hours

---

**Last Updated**: January 19, 2026
**Maintained by**: HDIM Platform Team
**Next Step**: Phase 2 - INTEGRATION_TESTING.md
