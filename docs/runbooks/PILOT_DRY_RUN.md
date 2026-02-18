# Pilot Dry-Run Procedure

**Run before the first customer is onboarded.** Complete all steps and sign off on the go/no-go checklist at the bottom before March 1 launch.

---

## Prerequisites

- Pilot K8s cluster (or Docker Compose host) is provisioned and accessible
- `alertmanager-credentials` secret is deployed (see `PILOT_SERVICE_SUBSET.md`)
- DNS / hostname for the pilot environment is configured
- A test tenant ID is reserved (e.g. `dry-run-tenant`)

---

## Step 1 — Deploy Pilot Overlay

```bash
# Kubernetes (recommended for pilot)
kubectl apply -k k8s/overlays/pilot/
kubectl rollout status deployment -n hdim-pilot --timeout=300s

# Docker Compose (single-host fallback)
./scripts/deploy.sh pilot --instance dry-run --dry-run   # preview first
./scripts/deploy.sh pilot --instance dry-run             # actual deploy
```

Expected: all 10 pilot services reach `Running` / `healthy` state within 5 minutes.

---

## Step 2 — Verify Core Services

```bash
# Health checks — all must return {"status":"UP"}
for port in 8001 8084 8085 8086 8087; do
  echo -n "Port $port: "
  curl -sf http://localhost:$port/actuator/health | jq -r .status
done
```

| Service | Port | Expected |
|---------|------|----------|
| Gateway | 8001 | UP |
| Patient | 8084 | UP |
| FHIR | 8085 | UP |
| Care Gap | 8086 | UP |
| Quality Measure | 8087 | UP |

---

## Step 3 — Seed Test Data

```bash
# Load synthetic pilot data (does not touch production)
./scripts/seed-pilot-data.sh --tenant dry-run-tenant --patients 50

# Verify data loaded
curl -s -H "X-Tenant-ID: dry-run-tenant" \
  http://localhost:8084/patient/api/v1/patients?pageSize=5 | jq '.totalElements'
# Expected: 50
```

---

## Step 4 — End-to-End Clinical Workflow

```bash
# 1. Retrieve a patient
PATIENT_ID=$(curl -s -H "X-Tenant-ID: dry-run-tenant" \
  "http://localhost:8084/patient/api/v1/patients?pageSize=1" \
  | jq -r '.content[0].id')
echo "Patient: $PATIENT_ID"

# 2. Evaluate care gaps
curl -s -H "X-Tenant-ID: dry-run-tenant" \
  "http://localhost:8086/care-gap/api/v1/care-gaps?patientId=$PATIENT_ID" \
  | jq '.totalElements'

# 3. Run CQL quality measure evaluation
curl -s -X POST \
  -H "X-Tenant-ID: dry-run-tenant" \
  -H "Content-Type: application/json" \
  -d "{\"patientId\": \"$PATIENT_ID\", \"measureId\": \"HbA1c-control\"}" \
  http://localhost:8087/quality-measure/api/v1/measures/evaluate \
  | jq '.status'

# 4. Retrieve FHIR Patient record
curl -s -H "X-Tenant-ID: dry-run-tenant" \
  "http://localhost:8085/fhir/Patient/$PATIENT_ID" \
  | jq '.resourceType'
# Expected: "Patient"
```

All 4 steps must succeed without errors.

---

## Step 5 — Observability Check

```bash
# Verify traces are appearing in Jaeger
open http://localhost:16686  # or kubectl port-forward svc/jaeger-query 16686:16686

# Verify metrics in Prometheus
curl -s "http://localhost:9090/api/v1/query?query=up" \
  | jq '[.data.result[] | {job: .metric.job, up: .value[1]}]'
# Expected: all HDIM services show up="1"

# Verify Grafana dashboard loads
open http://localhost:3001  # admin / hdim-grafana-secret
```

---

## Step 6 — Load Smoke Test

```bash
# Run k6 smoke (1 VU, verifies SLO scripts work end-to-end)
AUTH_TOKEN="<pilot-jwt-token>" \
TENANT_ID="dry-run-tenant" \
  ./load-tests/run-load-tests.sh --smoke

# Expected: all scenarios PASSED, no threshold violations
```

---

## Step 7 — Multi-Tenant Isolation Verification

```bash
# Create a second tenant
TENANT_B="isolation-test-tenant"

# Verify tenant A cannot see tenant B data
curl -s -H "X-Tenant-ID: dry-run-tenant" \
  "http://localhost:8084/patient/api/v1/patients?pageSize=100" \
  | jq --arg tid "$TENANT_B" '.content[] | select(.tenantId == $tid) | .id' \
  | wc -l
# Expected: 0  (no cross-tenant data leakage)
```

---

## Step 8 — Alert Verification

```bash
# Trigger a test alert by stopping a service temporarily
docker compose stop quality-measure-service   # or kubectl scale --replicas=0

# Wait 2 minutes, verify Slack notification received
# (requires SLACK_WEBHOOK_URL configured in alertmanager-credentials)

# Restore
docker compose start quality-measure-service   # or kubectl scale --replicas=1
```

---

## Go / No-Go Checklist

Sign off each item before pilot launch:

| Check | Owner | Status |
|-------|-------|--------|
| All 10 services healthy | Engineering | ☐ |
| Seed data loaded (50 patients) | Engineering | ☐ |
| End-to-end workflow passes (steps 1-4) | Engineering | ☐ |
| Jaeger traces visible | Engineering | ☐ |
| Prometheus metrics all `up=1` | Engineering | ☐ |
| Grafana dashboard accessible | Engineering | ☐ |
| k6 smoke passes (no threshold violations) | Engineering | ☐ |
| Multi-tenant isolation confirmed | Engineering | ☐ |
| Slack alert received from test trigger | Engineering | ☐ |
| On-call rotation active (pager assigned) | Engineering Lead | ☐ |
| Customer success rep briefed | Customer Success | ☐ |
| Pilot customer onboarding guide sent | Customer Success | ☐ |

**All items must be checked before the pilot customer receives their credentials.**

---

## Rollback

If dry-run reveals a blocking issue:

```bash
# K8s
kubectl delete -k k8s/overlays/pilot/

# Docker Compose
docker compose -p hdim-dry-run down -v
```

Document the issue in GitHub and reschedule. Do not onboard customer until resolved.
