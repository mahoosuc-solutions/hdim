# HDIM K3s Demo Deployment — Design Spec

**Date:** 2026-03-10
**Status:** Approved
**Goal:** Deploy the HDIM demo stack to K3s on Hetzner for always-on sales demos at HIMSS and beyond. Validate locally first.

---

## Architecture Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Orchestration | K3s | Lightweight K8s, auto-healing pods, Traefik built-in |
| Node topology | Single node now, architected for 3 | No hostPath, proper resource requests, anti-affinity ready |
| Ingress + TLS | Traefik (K3s default) + Let's Encrypt | Auto-HTTPS, subdomain routing, basic auth middleware |
| Storage (now) | Hetzner Cloud Volumes (CSI driver) | Network-attached, survives node replacement |
| Storage (later) | Longhorn | Distributed replication when scaling to 3 nodes |
| GitOps | FluxCD | Push manifests to git, cluster converges. No SSH deploy. |
| Registry | GHCR (GitHub Container Registry) | Already configured in deploy-docker.yml, free for public BSL repo |
| JVM sizing | MaxRAMPercentage (dynamic) | JVM reads cgroup limits, no hardcoded Xmx drift |
| Local validation | K3d (K3s-in-Docker) | Full K3s cluster inside Docker on dev laptop |

---

## Subdomain Routing

Wildcard DNS: `*.healthdatainmotion.com` → Hetzner IP (local: `127.0.0.1` via /etc/hosts)

| Subdomain | Backend | Port | Purpose | Auth |
|-----------|---------|------|---------|------|
| `demo.healthdatainmotion.com` | clinical-portal | 80 | Main UI — prospect logs in here | App login |
| `api.healthdatainmotion.com` | gateway-edge | 8080 | All API traffic, login, Swagger UI | JWT |
| `fhir.healthdatainmotion.com` | fhir-service | 8085 | Direct FHIR R4 endpoint | Open (metadata) |
| `traces.healthdatainmotion.com` | jaeger | 16686 | Distributed tracing UI | Basic auth |
| `docs.healthdatainmotion.com` | gateway-edge | 8080 | Swagger API docs (path: /swagger-ui/) | Basic auth |
| `ops.healthdatainmotion.com` | ops-service | 4710 | Deployment console, seeding status | Basic auth |

---

## Repository Layout

```
k8s/demo/
├── README.md                          # Quick-start guide
├── kustomization.yaml                 # Base kustomization
├── namespace.yaml                     # hdim-demo namespace
├── configmap.yaml                     # Shared env config
├── secrets.yaml                       # Sealed/template secrets
│
├── infrastructure/
│   ├── kustomization.yaml
│   ├── postgres.yaml                  # StatefulSet + PVC + Service
│   ├── redis.yaml                     # Deployment + Service
│   └── kafka.yaml                     # StatefulSet + PVC + Service
│
├── gateways/
│   ├── kustomization.yaml
│   ├── gateway-admin.yaml             # Deployment + Service
│   ├── gateway-fhir.yaml              # Deployment + Service
│   ├── gateway-clinical.yaml          # Deployment + Service
│   └── gateway-edge.yaml              # Deployment + Service (nginx)
│
├── services/
│   ├── kustomization.yaml
│   ├── fhir-service.yaml              # Deployment + Service
│   ├── patient-service.yaml           # Deployment + Service
│   ├── cql-engine-service.yaml        # Deployment + Service
│   ├── quality-measure-service.yaml   # Deployment + Service
│   ├── care-gap-service.yaml          # Deployment + Service
│   ├── event-processing-service.yaml  # Deployment + Service
│   ├── audit-query-service.yaml       # Deployment + Service
│   ├── hcc-service.yaml               # Deployment + Service
│   └── demo-seeding-service.yaml      # Deployment + Service + PVC
│
├── frontend/
│   ├── kustomization.yaml
│   └── clinical-portal.yaml           # Deployment + Service
│
├── observability/
│   ├── kustomization.yaml
│   └── jaeger.yaml                    # Deployment + Service
│
├── ops/
│   ├── kustomization.yaml
│   └── ops-service.yaml               # Deployment + Service
│
├── ingress/
│   ├── kustomization.yaml
│   ├── ingress-public.yaml            # demo + api + fhir subdomains
│   ├── ingress-internal.yaml          # traces + docs + ops (basic auth)
│   └── basic-auth-secret.yaml         # htpasswd for internal tools
│
├── flux/                              # FluxCD bootstrap
│   ├── gotk-components.yaml           # FluxCD controllers (generated)
│   ├── gotk-sync.yaml                 # GitRepository + Kustomization
│   └── kustomization.yaml
│
└── local/                             # Local validation overlay
    ├── kustomization.yaml             # Patches for k3d (no TLS, local ports)
    ├── k3d-cluster.yaml               # k3d cluster config
    └── hosts-entries.txt              # /etc/hosts lines for local testing
```

---

## K8s Resource Design

### Pod Resource Budgets

Every pod gets explicit requests and limits so the scheduler works correctly at multi-node.

| Pod | CPU Request | CPU Limit | Memory Request | Memory Limit | JVM MaxRAM% |
|-----|------------|-----------|---------------|-------------|-------------|
| postgres | 250m | 500m | 384Mi | 512Mi | — |
| redis | 100m | 250m | 64Mi | 128Mi | — |
| kafka | 250m | 1000m | 512Mi | 1024Mi | — |
| jaeger | 100m | 250m | 128Mi | 256Mi | — |
| gateway-admin | 100m | 500m | 256Mi | 512Mi | 75% |
| gateway-fhir | 100m | 500m | 256Mi | 512Mi | 75% |
| gateway-clinical | 100m | 500m | 256Mi | 512Mi | 75% |
| gateway-edge | 50m | 200m | 32Mi | 64Mi | — |
| fhir-service | 500m | 1000m | 1024Mi | 2048Mi | 85% |
| cql-engine | 200m | 500m | 256Mi | 512Mi | 75% |
| patient-service | 200m | 500m | 256Mi | 512Mi | 75% |
| quality-measure | 200m | 500m | 256Mi | 512Mi | 75% |
| care-gap-service | 200m | 500m | 256Mi | 512Mi | 75% |
| event-processing | 200m | 500m | 256Mi | 512Mi | 75% |
| audit-query | 200m | 500m | 256Mi | 512Mi | 75% |
| hcc-service | 200m | 500m | 256Mi | 512Mi | 75% |
| demo-seeding | 200m | 500m | 256Mi | 512Mi | 75% |
| ops-service | 50m | 200m | 64Mi | 256Mi | — |
| clinical-portal | 50m | 200m | 64Mi | 256Mi | — |
| **Total Requests** | **3.3 cores** | | **4.5 GB** | | |
| **Total Limits** | **8.6 cores** | | **~9.8 GB** | | |

Fits comfortably on CCX33 (8 vCPU, 32GB). Request totals well under capacity for scheduling headroom.

### Storage

| PVC | Size | StorageClass | Used By |
|-----|------|-------------|---------|
| postgres-data | 20Gi | hcloud-volumes (prod) / local-path (local) | postgres |
| kafka-data | 10Gi | hcloud-volumes (prod) / local-path (local) | kafka |
| demo-snapshots | 5Gi | hcloud-volumes (prod) / local-path (local) | demo-seeding |

### Pod Anti-Affinity (Ready, Not Enforced)

All service deployments include `preferredDuringSchedulingIgnoredDuringExecution` anti-affinity labels. On a single node, the scheduler ignores these. When nodes are added, pods spread automatically.

```yaml
affinity:
  podAntiAffinity:
    preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 100
        podAffinityTerm:
          labelSelector:
            matchExpressions:
              - key: app.kubernetes.io/name
                operator: In
                values: ["patient-service"]
          topologyKey: kubernetes.io/hostname
```

---

## Ingress Design

### Public Ingress (no basic auth)

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: hdim-public
  annotations:
    traefik.ingress.kubernetes.io/router.entrypoints: websecure
    cert-manager.io/cluster-issuer: letsencrypt-prod   # Hetzner only
spec:
  rules:
    - host: demo.healthdatainmotion.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: clinical-portal
                port:
                  number: 80
    - host: api.healthdatainmotion.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: gateway-edge
                port:
                  number: 8080
    - host: fhir.healthdatainmotion.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: fhir-service
                port:
                  number: 8085
```

### Internal Ingress (basic auth protected)

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: hdim-internal
  annotations:
    traefik.ingress.kubernetes.io/router.entrypoints: websecure
    traefik.ingress.kubernetes.io/router.middlewares: hdim-demo-basic-auth@kubernetescrd
spec:
  rules:
    - host: traces.healthdatainmotion.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: jaeger
                port:
                  number: 16686
    - host: ops.healthdatainmotion.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: ops-service
                port:
                  number: 4710
```

---

## FluxCD GitOps

### How It Works

1. FluxCD controllers run on the K3s cluster
2. A `GitRepository` resource watches `mahoosuc-solutions/hdim` on branch `main`
3. A `Kustomization` resource points to `k8s/demo/`
4. When manifests change in git, FluxCD reconciles the cluster within 1 minute
5. Image update automation watches GHCR for new tags and patches manifests

### Reconciliation Flow

```
git push (manifests) → GitHub → FluxCD polls (1m interval)
                                    ↓
                              kubectl apply -k k8s/demo/
                                    ↓
                              pods rolling-update
```

### Image Automation

```
GitHub Actions builds image → pushes to ghcr.io/mahoosuc-solutions/hdim/<service>:v3.0.1
                                    ↓
FluxCD ImagePolicy detects new tag → patches deployment manifest → commits back to git
                                    ↓
FluxCD Kustomization detects manifest change → applies to cluster
```

---

## Local Validation (K3d)

### What Is K3d

K3d runs K3s inside Docker containers on your laptop. You get a real K3s cluster with kubectl, Traefik ingress, and all the same behavior — without a VM or cloud account.

### Local Validation Steps

```bash
# 1. Install k3d (one-time)
curl -s https://raw.githubusercontent.com/k3d-io/k3d/main/install.sh | bash

# 2. Create cluster from config
k3d cluster create hdim-demo --config k8s/demo/local/k3d-cluster.yaml

# 3. Apply manifests (local overlay disables TLS, uses local-path storage)
kubectl apply -k k8s/demo/local/

# 4. Add /etc/hosts entries for subdomain routing
cat k8s/demo/local/hosts-entries.txt | sudo tee -a /etc/hosts

# 5. Wait for pods
kubectl -n hdim-demo wait --for=condition=ready pod --all --timeout=600s

# 6. Validate
#    http://demo.healthdatainmotion.com  → Clinical Portal
#    http://api.healthdatainmotion.com   → Gateway (Swagger at /swagger-ui/)
#    http://traces.healthdatainmotion.com → Jaeger (basic auth)
#    http://fhir.healthdatainmotion.com/metadata → FHIR conformance

# 7. Run validation script
./scripts/k3s-validate-demo.sh --local
```

### K3d Cluster Config

```yaml
# k8s/demo/local/k3d-cluster.yaml
apiVersion: k3d.io/v1alpha5
kind: Simple
metadata:
  name: hdim-demo
servers: 1
agents: 0
ports:
  - port: 80:80
    nodeFilters: [loadbalancer]
  - port: 443:443
    nodeFilters: [loadbalancer]
options:
  k3d:
    wait: true
    timeout: 120s
  k3s:
    extraArgs:
      - arg: --disable=traefik     # We'll install Traefik separately for config control
        nodeFilters: [server:*]
  kubeconfig:
    updateDefaultKubeconfig: true
    switchCurrentContext: true
```

### Local Overlay Patches

The `k8s/demo/local/kustomization.yaml` patches:
- Removes TLS/cert-manager annotations from ingress
- Switches StorageClass from `hcloud-volumes` to `local-path`
- Sets image pull policy to `IfNotPresent` (use locally built images)
- Disables FluxCD components (not needed locally)

---

## Hetzner Production Setup

### One-Time Provisioning

```bash
# 1. Create server
hcloud server create \
  --name hdim-demo \
  --type ccx33 \
  --image ubuntu-22.04 \
  --location fsn1 \
  --ssh-key your-key

# 2. Create volumes
hcloud volume create --name hdim-pg-data --size 20 --server hdim-demo
hcloud volume create --name hdim-kafka-data --size 10 --server hdim-demo
hcloud volume create --name hdim-snapshots --size 5 --server hdim-demo

# 3. Install K3s
curl -sfL https://get.k3s.io | sh -

# 4. Install FluxCD
flux bootstrap github \
  --owner=mahoosuc-solutions \
  --repository=hdim \
  --path=k8s/demo \
  --branch=main

# 5. DNS: *.healthdatainmotion.com → server IP (single A record)
```

After step 4, FluxCD takes over. All subsequent changes happen via `git push`.

### Firewall

```bash
hcloud firewall create --name hdim-demo-fw
hcloud firewall add-rule hdim-demo-fw --direction in --protocol tcp --port 80 --source-ips 0.0.0.0/0
hcloud firewall add-rule hdim-demo-fw --direction in --protocol tcp --port 443 --source-ips 0.0.0.0/0
hcloud firewall add-rule hdim-demo-fw --direction in --protocol tcp --port 6443 --source-ips YOUR_IP/32  # kubectl
hcloud firewall apply-to-resource hdim-demo-fw --type server --server hdim-demo
```

Only 80, 443, and 6443 (kubectl from your IP) exposed. All service ports are internal to the cluster.

---

## Scaling to 3 Nodes (Future)

When ready:

1. Create 2 additional CCX33 servers
2. Join as K3s agents: `curl -sfL https://get.k3s.io | K3S_URL=https://control:6443 K3S_TOKEN=xxx sh -`
3. Install Longhorn: `kubectl apply -f https://raw.githubusercontent.com/longhorn/longhorn/v1.6.0/deploy/longhorn.yaml`
4. Migrate PVCs from hcloud-volumes to Longhorn
5. Pod anti-affinity kicks in automatically — pods spread across nodes

---

## Scripts

| Script | Purpose |
|--------|---------|
| `scripts/k3s-create-cluster.sh` | Hetzner server + K3s + FluxCD bootstrap |
| `scripts/k3s-validate-demo.sh` | Validates all pods, ingress, auth flow, data presence (works local + remote) |
| `scripts/k3s-local-up.sh` | Creates k3d cluster, applies local overlay, adds /etc/hosts |
| `scripts/k3s-local-down.sh` | Tears down k3d cluster, removes /etc/hosts entries |

---

## Implementation Order

1. **K8s manifests** — namespace, configmap, secrets, all 19 service deployments+services
2. **Ingress** — public + internal with basic auth
3. **Local overlay** — k3d config, local patches, hosts file
4. **Validation script** — `k3s-validate-demo.sh` (reuses logic from `gcp-validate-demo.sh`)
5. **Local smoke test** — k3d up, apply, validate, tear down
6. **FluxCD bootstrap** — flux config, image automation policies
7. **Hetzner provisioning script** — server + volumes + K3s + flux bootstrap
8. **DNS + TLS** — wildcard record, cert-manager cluster issuer

Steps 1-5 are local-only. No cloud account needed until step 7.
