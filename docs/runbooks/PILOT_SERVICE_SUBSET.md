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
