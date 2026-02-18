# HDIM Pilot Service Subset

**Runbook:** Pilot Deployment
**Audience:** Engineering, Customer Success, Solutions Engineering
**Last Updated:** February 2026
**Owner:** Engineering Lead

---

## Overview

The HDIM pilot deploys **10 core services** — the minimum set required to deliver end-to-end HEDIS quality measure evaluation for a health plan with up to ~5,000 attributed members. The remaining 11 services (AI/agent, predictive analytics, SDOH, workflow, EHR connector) are deferred to the GA release in Phase 2.

This subset was chosen to:
1. **Minimize time-to-value** — customers see results within 2 weeks of deployment
2. **Minimize risk** — fewer moving parts during a high-stakes pilot
3. **Minimize cost** — single-replica sizing cuts cloud spend ~60% vs production
4. **Preserve upgrade path** — the Kustomize overlay composes cleanly on top of the same base

---

## Pilot Service Catalog

### Tier 1 — Entry Point & FHIR Foundation

| Service | Port | Purpose | Pilot Included |
|---------|------|---------|----------------|
| `gateway-service` | 8001 | API entry point, JWT validation, routing | ✅ Yes |
| `fhir-service` | 8085 | FHIR R4 resource store, SMART on FHIR | ✅ Yes |

**Rationale:** Every external call enters through the gateway. The FHIR store is the canonical patient record source.

### Tier 2 — Patient & Clinical Data

| Service | Port | Purpose | Pilot Included |
|---------|------|---------|----------------|
| `patient-service` | 8084 | Patient demographics, multi-tenant records | ✅ Yes |
| `consent-service` | 8086 | Patient consent management | ✅ Yes |

**Rationale:** Patient records and consent status are required before any quality measure evaluation can run. Consent is included for HIPAA compliance (cannot run evaluations on non-consented patients).

### Tier 3 — Quality Measure Pipeline

| Service | Port | Purpose | Pilot Included |
|---------|------|---------|----------------|
| `cql-engine-service` | 8081 | CQL clinical logic execution (HEDIS rules) | ✅ Yes |
| `quality-measure-service` | 8087 | HEDIS measure orchestration and results | ✅ Yes |
| `care-gap-service` | 8086 | Care gap detection, tracking, closure | ✅ Yes |

**Rationale:** This is the core revenue-generating pipeline. The CQL engine runs HEDIS logic; the quality measure service orchestrates evaluation and persists results; the care gap service produces the actionable output customers consume.

### Tier 4 — Event & Audit Infrastructure

| Service | Port | Purpose | Pilot Included |
|---------|------|---------|----------------|
| `event-processing-service` | — | HIPAA audit event ingestion | ✅ Yes |
| `event-router-service` | — | Event routing and fanout to consumers | ✅ Yes |

**Rationale:** HIPAA §164.312(b) requires audit controls on all PHI access. The event services provide the audit trail. These are not optional.

### Tier 5 — Data Ingestion

| Service | Port | Purpose | Pilot Included |
|---------|------|---------|----------------|
| `cdr-processor-service` | — | Clinical Document Registry (CDR) processing | ✅ Yes |

**Rationale:** Customers ingest clinical data via CDR feeds (834 files, HL7v2, ADT notifications). Without CDR processing, the pilot has no data.

---

## Services Deferred to GA (Phase 2)

| Service | Deferral Reason |
|---------|----------------|
| `agent-runtime-service` | AI agents (Phase 2 feature) |
| `ai-assistant-service` | AI chat UI (Phase 2 feature) |
| `agent-builder-service` | Agent authoring (Phase 2 feature) |
| `analytics-service` | Historical trend analytics (valuable but not blocking) |
| `predictive-analytics-service` | ML risk scores (requires historical data first) |
| `sdoh-service` | Social determinants (Phase 2 enrichment) |
| `approval-service` | Care gap approval workflows (Phase 2 workflow) |
| `payer-workflows-service` | Payer-specific workflows (Phase 2 workflow) |
| `data-enrichment-service` | Data quality enrichment (Phase 2) |
| `ehr-connector-service` | Direct EHR integration (Phase 2, after pilot validates FHIR path) |
| `documentation-service` | Internal docs (not customer-facing) |

---

## Infrastructure Requirements

| Component | Pilot Sizing | Production Sizing |
|-----------|-------------|------------------|
| PostgreSQL | 1 instance, 50GB | 3 instances HA, 500GB+ |
| Redis | 1 instance, 4GB | 3-node cluster, 16GB+ |
| Kafka | 1 broker, 3 partitions | 3-broker cluster, replication factor 3 |
| Kubernetes nodes | 2 × 4 vCPU / 16GB | 5+ × 8 vCPU / 32GB |

**Total estimated pilot cloud cost:** ~$800–1,200/month (3-node cluster, AWS EKS)

---

## Deployment

```bash
# Deploy pilot overlay
kubectl apply -k k8s/overlays/pilot/

# Verify all 10 services are running
kubectl -n hdim-pilot get pods

# Check service health
kubectl -n hdim-pilot get pods -o wide | grep -v Running
```

### Expected pod count
```
gateway-service-xxx        1/1   Running
fhir-service-xxx           1/1   Running
patient-service-xxx        1/1   Running
consent-service-xxx        1/1   Running
cql-engine-service-xxx     1/1   Running
quality-measure-service-xxx 1/1  Running
care-gap-service-xxx       1/1   Running
event-processing-service-xxx 1/1 Running
event-router-service-xxx   1/1   Running
cdr-processor-service-xxx  1/1   Running
postgres-xxx               1/1   Running
redis-xxx                  1/1   Running
kafka-xxx                  1/1   Running
```

---

## Pilot → Production Upgrade Path

When converting a pilot to production:

1. **Increase replicas** — Set `replicas: 2` for all core services, `replicas: 3` for gateway and fhir-service
2. **Apply PDBs** — Add PodDisruptionBudgets for all services (see `k8s/overlays/production/pdb.yaml`)
3. **Enable HA infrastructure** — Promote PostgreSQL, Redis, and Kafka to HA configurations
4. **Activate remaining services** — Switch overlay from `pilot` → `production` (adds all 11 deferred services)
5. **Wire Alertmanager** — Connect Slack/PagerDuty notification channels
6. **Configure long-term Jaeger storage** — Switch from Badger (ephemeral) to Elasticsearch backend

```bash
# Switch from pilot to production
kubectl delete -k k8s/overlays/pilot/
kubectl apply -k k8s/overlays/production/
```

---

## Observable SLOs (Pilot Commitments)

During pilot, the following SLOs are contractually committed and observable in Jaeger:

| SLO | Target | Alert Threshold |
|-----|--------|----------------|
| Care gap detection P95 latency | < 500ms | > 400ms for 5 min |
| Quality measure evaluation P95 latency | < 2s | > 1.6s for 5 min |
| API availability | ≥ 99.5% | < 99.9% for 15 min |
| Patient record retrieval P95 | < 200ms | > 160ms for 5 min |

Customers can verify SLO compliance in real time at: `http://jaeger:16686` (pilot namespace proxy).

---

## Success Criteria for Pilot → GA Conversion

The pilot is considered successful and ready for GA conversion when:

- [ ] Customer has ingested 1,000+ attributed members via CDR
- [ ] At least 3 HEDIS measures evaluated end-to-end with results stored
- [ ] Care gaps identified and displayed in customer UI or via API
- [ ] Zero P1 security incidents during pilot period (≥ 2 weeks)
- [ ] All observable SLOs met for ≥ 2 consecutive weeks
- [ ] Customer signs LOI or MSA for production contract

---

## Troubleshooting

For service-specific issues during pilot, see the relevant runbooks:

- [FHIR Service](./fhir-service.md)
- [Care Gap Processing](./care-gap-processing.md)
- [CQL Engine](./cql-engine-service.md)
- [Event Processing](./event-processing-service.md)
- [Database Issues](./database-issues.md)
- [Kafka Issues](./kafka-issues.md)
- [Performance Degradation](./performance-degradation.md)
- [High Error Rate](./high-error-rate.md)
- [Incident Response](./INCIDENT_RESPONSE.md)

---

## Alertmanager Credentials Setup

**Audience:** DevOps / operator deploying monitoring for the first time
**Required before:** Running `kubectl apply -k k8s/overlays/pilot/` or `k8s/overlays/production/`

Alertmanager reads notification channel credentials (Slack, PagerDuty, email SMTP) from a
Kubernetes Secret named `alertmanager-credentials` in the `hdim-production` namespace. The
Deployment in `k8s/monitoring/alertmanager.yaml` injects each value as an environment variable;
`monitoring/alertmanager-production.yml` references them as `${VAR_NAME}`. **The Secret must
exist before the Alertmanager pod starts — it is not created automatically.**

### Step 1: Copy and fill in the credentials file

```bash
# From the repository root
cp monitoring/.env.alertmanager.example monitoring/.env.alertmanager
```

Open `monitoring/.env.alertmanager` and replace every placeholder value. The required variables
are:

| Variable | Where to get it |
|---|---|
| `SLACK_WEBHOOK_URL` | Slack App console (see Step 2 below) |
| `PAGERDUTY_INTEGRATION_KEY` | PagerDuty > Services > \<General on-call service\> > Integrations > Prometheus |
| `PAGERDUTY_SECURITY_KEY` | PagerDuty > Services > \<Security service\> > Integrations > Prometheus |
| `PAGERDUTY_COMPLIANCE_KEY` | PagerDuty > Services > \<Compliance service\> > Integrations > Prometheus |
| `EMAIL_USERNAME` | SMTP account address (e.g. `alerts@your-domain.com`) |
| `EMAIL_PASSWORD` | App password for the SMTP account (not your login password) |

> `ONCALL_EMAIL`, `LEADERSHIP_EMAIL`, `COMPLIANCE_EMAIL`, `DATABASE_TEAM_EMAIL`, and
> `DEVOPS_EMAIL` are only needed for Docker Compose local testing (the `docker compose up alertmanager`
> workflow documented inside `.env.alertmanager.example`). They are **not** mounted into the
> Kubernetes pod and do not need to be set for Kubernetes deployments.

### Step 2: Get a Slack Webhook URL

Create a Slack App at <https://api.slack.com/apps>, enable **Incoming Webhooks**, install it to
your workspace, and copy the generated `https://hooks.slack.com/services/…` URL into
`SLACK_WEBHOOK_URL`.

### Step 3: Create the Kubernetes Secret

Run the following command from the repository root **with the filled-in** `.env.alertmanager` file:

```bash
kubectl create secret generic alertmanager-credentials \
  --from-literal=SLACK_WEBHOOK_URL="${SLACK_WEBHOOK_URL}" \
  --from-literal=PAGERDUTY_INTEGRATION_KEY="${PAGERDUTY_INTEGRATION_KEY}" \
  --from-literal=PAGERDUTY_SECURITY_KEY="${PAGERDUTY_SECURITY_KEY}" \
  --from-literal=PAGERDUTY_COMPLIANCE_KEY="${PAGERDUTY_COMPLIANCE_KEY}" \
  --from-literal=EMAIL_USERNAME="${EMAIL_USERNAME}" \
  --from-literal=EMAIL_PASSWORD="${EMAIL_PASSWORD}" \
  -n hdim-production
```

Or, using the `--from-env-file` shorthand (reads from the file directly without needing the vars
exported into your shell):

```bash
kubectl create secret generic alertmanager-credentials \
  --from-env-file=monitoring/.env.alertmanager \
  -n hdim-production
```

> **Note:** `--from-env-file` only picks up lines in `KEY=VALUE` format and silently ignores
> comment lines and blank lines, so the example file format is compatible as-is.

To update an existing Secret after rotating credentials, delete and re-create it, then restart
the pod:

```bash
kubectl delete secret alertmanager-credentials -n hdim-production
kubectl create secret generic alertmanager-credentials \
  --from-env-file=monitoring/.env.alertmanager \
  -n hdim-production
kubectl rollout restart deployment/alertmanager -n hdim-production
```

### Step 4: Verify the Secret was applied

```bash
# Confirm all 6 keys exist and are non-empty
kubectl describe secret alertmanager-credentials -n hdim-production
```

Expected output (values are redacted by Kubernetes):

```
Name:         alertmanager-credentials
Namespace:    hdim-production
Type:         Opaque

Data
====
EMAIL_PASSWORD:             <N bytes>
EMAIL_USERNAME:             <N bytes>
PAGERDUTY_COMPLIANCE_KEY:   32 bytes
PAGERDUTY_INTEGRATION_KEY:  32 bytes
PAGERDUTY_SECURITY_KEY:     32 bytes
SLACK_WEBHOOK_URL:          <N bytes>
```

If any key is missing, delete the secret and re-create it (see above).

To confirm the running pod has picked up the values:

```bash
# Pod must be Running before this works
kubectl exec deployment/alertmanager -n hdim-production -- \
  sh -c 'echo "Slack URL prefix: ${SLACK_WEBHOOK_URL:0:30}…"'
```

Check the Alertmanager API for a clean config load:

```bash
kubectl exec deployment/alertmanager -n hdim-production -- \
  wget -qO- http://localhost:9093/-/ready && echo "Alertmanager is ready"
```

### What happens if the Secret is missing

If the `alertmanager-credentials` Secret does not exist when the Deployment starts, Kubernetes
will refuse to schedule the Alertmanager pod — it enters `CreateContainerConfigError` and never
reaches `Running`. Prometheus will continue to fire alerts and evaluate rules normally, but
**all notifications are silently dropped**: no Slack messages, no PagerDuty pages, and no emails
are delivered. On-call engineers will not receive any alert for production incidents or SLO
breaches until the Secret is created and the pod is restarted.

