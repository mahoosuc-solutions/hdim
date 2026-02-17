---
name: devops:deploy
description: Orchestrate a full HDIM deployment — pre-flight, migrations, build, deploy, health-check, smoke-test, rollback-on-failure. Accepts environment + instance + target parameters and actually runs the commands.
category: devops
priority: high
---

# HDIM Deployment Orchestrator

You are orchestrating an HDIM deployment. Follow each phase in strict order. Do not skip phases unless the flag is explicitly set. Announce each phase before executing it. If any phase fails, execute rollback immediately.

## Command Signature

```
/devops:deploy <environment> [--instance <name>] [--target compose|k8s|auto] [--services <list>] [--strategy rolling|blue-green|recreate] [--skip-tests] [--dry-run]
```

## Parse Arguments First

Read the user's arguments and set these variables for the session:

| Variable         | Default     | Source                         |
|------------------|-------------|--------------------------------|
| `ENV`            | (required)  | first positional arg           |
| `INSTANCE`       | `default`   | `--instance <name>`            |
| `TARGET`         | `auto`      | `--target compose|k8s|auto`    |
| `SERVICES`       | (all)       | `--services svc1,svc2`         |
| `STRATEGY`       | `rolling`   | `--strategy <name>`            |
| `SKIP_TESTS`     | false       | `--skip-tests`                 |
| `DRY_RUN`        | false       | `--dry-run`                    |

Derived:
- `COMPOSE_PROJECT` = `hdim-${INSTANCE}`
- `K8S_NAMESPACE` = `hdim-${ENV}-${INSTANCE}`

## Environment → Toolchain Mapping

| Environment  | Docker Compose files                                                                     | K8s overlay             |
|--------------|------------------------------------------------------------------------------------------|-------------------------|
| `dev`        | `docker-compose.yml`                                                                     | n/a                     |
| `demo`       | `docker-compose.demo.yml`                                                                | n/a                     |
| `staging`    | `docker-compose.staging.yml -f docker-compose.observability.yml`                        | `k8s/overlays/staging/` |
| `pilot`      | `docker-compose.minimal-clinical.yml`                                                    | `k8s/overlays/pilot/`   |
| `production` | `docker-compose.production.yml -f docker-compose.ha.yml -f docker-compose.observability.yml` | `k8s/overlays/production/` |

If `--target auto`, detect: if `kubectl cluster-info` succeeds → use `k8s`; otherwise → use `compose`.

---

## Phase 1: Pre-flight Validation

**Announce:** "Running Phase 1: Pre-flight validation..."

Run each check. Abort the deployment if any check fails (unless --dry-run).

### 1a. Repository sanity

```bash
# Confirm we're at the repo root
ls backend/
```

If `backend/` is missing, stop: "Run this command from the HDIM repo root."

### 1b. Git state (production only)

If ENV is `production`:

```bash
git status --porcelain
git rev-parse --abbrev-ref HEAD
```

- If `git status` output is non-empty → abort: "Production requires a clean working directory."
- If branch is not `master` → abort: "Production deployments must be from the master branch."

### 1c. Tool availability

For compose target:
```bash
docker --version
docker compose version
```

For k8s target:
```bash
kubectl version --client
kubectl cluster-info
```

### 1d. Schema + HIPAA pre-flight

```bash
./scripts/validate-before-docker-build.sh
```

If this script exits non-zero, abort. Do not proceed to deployment.

---

## Phase 2: Database Migration Validation

**Announce:** "Running Phase 2: Database migration validation..."

```bash
cd backend && ./gradlew test --tests '*EntityMigrationValidationTest' -x javadoc --continue
```

If any test fails, abort the deployment. Migrations must pass before any container is started.

For Kubernetes: also confirm that Liquibase init containers are present in the service manifests.

---

## Phase 3: Build (Docker Compose only)

**Announce:** "Running Phase 3: Building Docker images..."

Skip this phase for Kubernetes targets (CI/CD pipeline manages image builds and pushes).

### 3a. Pre-cache Gradle dependencies

```bash
cd backend && ./gradlew downloadDependencies --no-daemon
```

### 3b. Build images

For all services:
```bash
docker compose -p hdim-${INSTANCE} -f <compose-files> build
```

For production, add `--no-cache`:
```bash
docker compose -p hdim-${INSTANCE} -f <compose-files> build --no-cache
```

For selected services only (if --services was specified):
```bash
docker compose -p hdim-${INSTANCE} -f <compose-files> build ${SERVICES//,/ }
```

---

## Phase 4: Deploy

**Announce:** "Running Phase 4: Deploying to ${ENV} (instance=${INSTANCE}, target=${TARGET}, strategy=${STRATEGY})..."

### Docker Compose (rolling — default)

```bash
docker compose -p hdim-${INSTANCE} -f <compose-files> up -d --remove-orphans
```

### Docker Compose (recreate)

```bash
docker compose -p hdim-${INSTANCE} -f <compose-files> down --remove-orphans
docker compose -p hdim-${INSTANCE} -f <compose-files> up -d
```

### Kubernetes (default instance)

```bash
kubectl apply -k k8s/overlays/${ENV}/
kubectl rollout status deployment --timeout=300s --namespace hdim-${ENV}
```

### Kubernetes (named instance — namespace override)

```bash
kubectl apply -k k8s/overlays/${ENV}/ --namespace ${K8S_NAMESPACE}
kubectl rollout status deployment --timeout=300s --namespace ${K8S_NAMESPACE}
```

---

## Phase 5: Health Checks

**Announce:** "Running Phase 5: Health checks..."

For Kubernetes: Phase 4's `kubectl rollout status` already confirms readiness. Report success and continue.

For Docker Compose: poll each core service. Do NOT use `sleep` — poll with retries:

```bash
# Poll pattern: 12 retries × 5s = 60s max per service
for service in gateway patient fhir care-gap quality-measure; do
  curl -sf http://localhost:<PORT>/actuator/health --retry 12 --retry-delay 5 --retry-all-errors -o /dev/null
done
```

Core service ports:
| Service           | Port | Health URL                                         |
|-------------------|------|----------------------------------------------------|
| API Gateway       | 8001 | `http://localhost:8001/actuator/health`            |
| Patient Service   | 8084 | `http://localhost:8084/patient/actuator/health`    |
| FHIR Service      | 8085 | `http://localhost:8085/fhir/actuator/health`       |
| Care Gap Service  | 8086 | `http://localhost:8086/care-gap/actuator/health`   |
| Quality Measure   | 8087 | `http://localhost:8087/quality-measure/actuator/health` |

For staging/pilot/production also check (non-fatal warnings if unavailable):
| Service    | URL                                |
|------------|------------------------------------|
| Prometheus | `http://localhost:9090/-/healthy`  |
| Grafana    | `http://localhost:3000/api/health` |
| Jaeger     | `http://localhost:16686/api/traces`|

**If any core service fails health checks → trigger Rollback (below) and stop.**

---

## Rollback Procedure

**Trigger when:** Phase 5 health check fails or Phase 6 smoke tests fail.

**Announce:** "⚠️ ROLLBACK TRIGGERED — reverting deployment..."

### Docker Compose rollback

```bash
docker compose -p hdim-${INSTANCE} -f <compose-files> down --remove-orphans
```

Then notify the user: "Services stopped. Redeploy the previous version by re-running with the previous image tag or reverting the git commit."

### Kubernetes rollback

```bash
kubectl rollout undo deployment --namespace hdim-${ENV}-${INSTANCE}
kubectl rollout status deployment --timeout=120s --namespace hdim-${ENV}-${INSTANCE}
```

---

## Phase 6: Smoke Tests

**Announce:** "Running Phase 6: Smoke tests..."

Skip if `--skip-tests` was specified (warn: "Smoke tests skipped. Validate manually.").

```bash
./scripts/smoke-tests.sh \
  --environment ${ENV} \
  --gateway http://localhost:8001 \
  --tenant ${INSTANCE}
```

For dev environments, add `--quick` to run core-only tests:
```bash
./scripts/smoke-tests.sh --environment dev --gateway http://localhost:8001 --quick
```

If smoke tests fail → trigger Rollback.

---

## Phase 7: Post-Deployment Report

**Announce:** "Running Phase 7: Post-deployment report..."

```bash
mkdir -p deployments/
```

Write a JSON report to `deployments/hdim-${ENV}-${INSTANCE}-<timestamp>.json`:

```bash
cat > deployments/hdim-${ENV}-${INSTANCE}-$(date +%Y%m%d-%H%M%S).json <<EOF
{
  "environment": "${ENV}",
  "instance": "${INSTANCE}",
  "target": "${TARGET}",
  "strategy": "${STRATEGY}",
  "compose_project": "hdim-${INSTANCE}",
  "k8s_namespace": "hdim-${ENV}-${INSTANCE}",
  "git_commit": "$(git rev-parse --short HEAD)",
  "deployed_at": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "smoke_tests_skipped": ${SKIP_TESTS},
  "dry_run": ${DRY_RUN}
}
EOF
```

Then print access URLs:

**For dev/demo:**
- Frontend: http://localhost:4200
- API Gateway: http://localhost:8001
- Patient Swagger: http://localhost:8084/patient/swagger-ui/index.html
- FHIR Swagger: http://localhost:8085/fhir/swagger-ui/index.html
- Care Gap Swagger: http://localhost:8086/care-gap/swagger-ui/index.html
- Quality Measure Swagger: http://localhost:8087/quality-measure/swagger-ui/index.html

**For staging/pilot/production:**
- Frontend: http://localhost:4200
- API Gateway: http://localhost:8001
- Grafana: http://localhost:3000 (admin/admin)
- Prometheus: http://localhost:9090
- Jaeger: http://localhost:16686

Print helpful follow-up commands:
```bash
# View live logs
docker compose -p hdim-${INSTANCE} -f <compose-files> logs -f

# Check status
docker compose -p hdim-${INSTANCE} -f <compose-files> ps

# Stop environment
docker compose -p hdim-${INSTANCE} -f <compose-files> down
```

---

## Dry-Run Mode

When `--dry-run` is specified:
- Print every command that **would** be executed, prefixed with `[DRY-RUN]`
- Do not execute any shell commands
- Skip health polling (print what would be polled)
- Still write the JSON report with `"dry_run": true`

---

## Examples

```bash
# 1. Development — local default stack
/devops:deploy dev

# 2. Staging — named instance, all services
/devops:deploy staging --instance staging-1

# 3. Pilot — customer tenant on Kubernetes (dry-run first)
/devops:deploy pilot --instance acme-health --target k8s --dry-run

# 4. Pilot — customer tenant on Kubernetes (live)
/devops:deploy pilot --instance acme-health --target k8s

# 5. Production — rolling deploy, specific services only
/devops:deploy production --instance prod --services patient-service,care-gap-service --strategy rolling

# 6. Demo environment — skip smoke tests for quick iteration
/devops:deploy demo --skip-tests
```

---

## Related Commands

- `/devops:monitor` — Monitor production health dashboards
- `/db:migrate` — Execute database migrations independently
- `/test:integration` — Run integration test suite
- `/auth:audit` — Audit authentication and HIPAA compliance

## See Also

- [Deployment Runbook](../../docs/DEPLOYMENT_RUNBOOK.md)
- [Build Management Guide](../../backend/docs/BUILD_MANAGEMENT_GUIDE.md)
- [HIPAA Compliance Guide](../../backend/HIPAA-CACHE-COMPLIANCE.md)
- [Service Catalog](../../docs/services/SERVICE_CATALOG.md)
- [scripts/deploy.sh](../../scripts/deploy.sh) — backing shell script
