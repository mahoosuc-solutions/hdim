# Jaeger Elasticsearch Migration Runbook

**Scope:** Replace Jaeger's Badger on-disk storage with Elasticsearch for persistent,
production-grade trace retention.

**Applies to:** Pilot deployments and all GA environments.

---

## When to Use This

| Environment | Storage | File |
|-------------|---------|------|
| Local development / CI | Badger (default) | `docker-compose.observability.yml` |
| Pilot customer deployments | **Elasticsearch** | `docker-compose.observability-production.yml` |
| Kubernetes production | **Elasticsearch** | `k8s/monitoring/jaeger-elasticsearch.yaml` |

Switch to Elasticsearch before onboarding the first pilot customer. Badger data is
lost whenever the container is removed; Elasticsearch data persists across restarts
and container recreations.

---

## Prerequisites

- Docker host with at least **8 GB RAM** available (Elasticsearch alone needs 2-4 GB)
- Ports **9200, 5601, 4317, 4318, 14268, 16686** not occupied by other processes
- For Kubernetes: `kubectl` access to the `hdim-production` namespace and a default
  StorageClass that supports `ReadWriteOnce` (10 Gi per Elasticsearch node)

---

## Deploy — Docker Compose

```bash
# 1. Stop the dev observability stack if running
docker compose -f docker-compose.observability.yml down

# 2. Start the production observability stack
docker compose -f docker-compose.observability-production.yml up -d

# 3. Run alongside main HDIM services so spans flow in automatically
#    (services already export to http://jaeger:4318/v1/traces)
docker compose -f docker-compose.yml -f docker-compose.observability-production.yml up -d
```

---

## Deploy — Kubernetes

```bash
# 1. Ensure the namespace exists
kubectl create namespace hdim-production --dry-run=client -o yaml | kubectl apply -f -

# 2. Apply the manifest
kubectl apply -f k8s/monitoring/jaeger-elasticsearch.yaml

# 3. Wait for Elasticsearch to become ready (may take 60-90 seconds)
kubectl rollout status statefulset/elasticsearch -n hdim-production

# 4. Wait for the collector and query deployments
kubectl rollout status deployment/jaeger-collector -n hdim-production
kubectl rollout status deployment/jaeger-query -n hdim-production
```

Update each service's `OTEL_EXPORTER_OTLP_ENDPOINT` to point to the in-cluster
collector service:

```yaml
env:
  - name: OTEL_EXPORTER_OTLP_ENDPOINT
    value: http://jaeger-collector.hdim-production.svc.cluster.local:4318/v1/traces
```

---

## Verify

### Docker Compose

```bash
# Check Elasticsearch cluster health (green or yellow is OK for single-node)
curl http://localhost:9200/_cluster/health | python3 -m json.tool

# Confirm Jaeger indices exist after a few spans have been received
curl http://localhost:9200/_cat/indices?v | grep hdim

# Open Jaeger UI and verify traces appear
open http://localhost:16686

# Open Kibana for raw index inspection (optional)
open http://localhost:5601
```

### Kubernetes

```bash
# Port-forward and open Jaeger UI
kubectl port-forward svc/jaeger-query 16686:16686 -n hdim-production &
open http://localhost:16686

# Check Elasticsearch from inside the cluster
kubectl exec -it statefulset/elasticsearch -n hdim-production -- \
  curl -s http://localhost:9200/_cluster/health | python3 -m json.tool

# List Jaeger-created indices
kubectl exec -it statefulset/elasticsearch -n hdim-production -- \
  curl -s http://localhost:9200/_cat/indices?v | grep hdim
```

A healthy response from `/_cluster/health` looks like:

```json
{
  "cluster_name": "hdim-traces",
  "status": "yellow",
  "number_of_nodes": 1,
  ...
}
```

`yellow` is normal for a single-node cluster (replica shards cannot be assigned).
`green` requires 3+ nodes.

---

## Rollback Procedure

### Docker Compose

```bash
# 1. Stop the production stack
docker compose -f docker-compose.observability-production.yml down

# 2. Restart the dev stack (Badger)
docker compose -f docker-compose.observability.yml up -d
```

Note: traces stored in Elasticsearch are NOT migrated back to Badger.

### Kubernetes

```bash
# Remove the Elasticsearch-backed resources
kubectl delete -f k8s/monitoring/jaeger-elasticsearch.yaml

# Re-apply the original Badger-backed Jaeger (dev fallback only)
kubectl apply -f k8s/monitoring/jaeger.yaml
```

---

## Index Lifecycle Management (Post-Pilot)

By default Jaeger writes daily indices with no automatic expiry. For GA deployments
configure an Elasticsearch ILM policy to delete indices older than your retention
window (e.g., 30 days):

```bash
curl -X PUT http://localhost:9200/_ilm/policy/jaeger-traces -H 'Content-Type: application/json' -d '{
  "policy": {
    "phases": {
      "delete": {
        "min_age": "30d",
        "actions": { "delete": {} }
      }
    }
  }
}'
```

Then apply the policy to the `hdim-*` index template. See the
[Elasticsearch ILM documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/index-lifecycle-management.html)
for full details.

---

## Upgrading Elasticsearch (Single-node to Multi-node)

Before scaling beyond 10 pilot customers or before Series A, upgrade to a 3-node
Elasticsearch cluster (or use the ECK Operator for Kubernetes). Key steps:

1. Add 2 additional Elasticsearch pods to the StatefulSet (`replicas: 3`)
2. Set `cluster.initial_master_nodes` environment variable listing all 3 pod names
3. Change `discovery.type` to `zen` (remove the `single-node` override)
4. Increase the PVC `storage` request per node to match expected volume

---

## Related Files

| File | Purpose |
|------|---------|
| `docker-compose.observability.yml` | Dev/test stack (Badger, ephemeral) |
| `docker-compose.observability-production.yml` | Pilot/prod stack (Elasticsearch) |
| `k8s/monitoring/jaeger.yaml` | K8s dev manifest (Badger) |
| `k8s/monitoring/jaeger-elasticsearch.yaml` | K8s prod manifest (Elasticsearch) |
| `k8s/monitoring/kustomization.yaml` | Kustomize overlay entry point |

---

_Last updated: February 2026_
