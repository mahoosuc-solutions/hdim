# HDIM Flexible Deployment Options Guide

**Audience**: Chief Technology Officers, Infrastructure Architects, Cloud/On-Prem Decision Makers
**Purpose**: Demonstrate HDIM's deployment flexibility and help organizations choose the optimal deployment model
**Last Updated**: January 2026

---

## Executive Summary

Healthcare organizations face unique deployment challenges: data residency requirements, existing infrastructure investments, varying IT capabilities, and regulatory constraints. Unlike cloud-only competitors, **HDIM offers true deployment flexibility**—deploy where your data needs to be, scale how your organization grows, and control what matters most.

### Key Differentiators

| Capability | HDIM | Epic Healthy Planet | Optum One | Arcadia |
|------------|------|---------------------|-----------|---------|
| **Full On-Premise Option** | Yes | No (Cloud Only) | No (SaaS Only) | Limited |
| **Cloud Deployment** | Yes (AWS/Azure/GCP) | Yes | Yes | Yes |
| **Hybrid Cloud** | Yes | No | No | Limited |
| **Air-Gapped Networks** | Yes | No | No | No |
| **Single-Tenant Isolation** | Yes | Shared | Shared | Limited |
| **Bring Your Own Infrastructure** | Yes | No | No | No |

### The "Start Small, Scale Up" Promise

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        HDIM ADOPTION PATHWAY                                     │
│                                                                                  │
│   PILOT                    PRODUCTION                    ENTERPRISE             │
│   ─────────────────────→   ─────────────────────────→   ─────────────────────   │
│                                                                                  │
│   Single-Node              Clustered                    Kubernetes/Hybrid       │
│   1 server                 3-5 servers                  10+ nodes               │
│   <50K patients            50K-500K patients            500K-5M+ patients       │
│   $200-500/mo              $2-5K/mo                     $5-20K/mo               │
│   2-3 hours setup          1-2 days setup               3-5 days setup          │
│                                                                                  │
│   ✓ Prove value            ✓ Production ready           ✓ Auto-scaling          │
│   ✓ Test integration       ✓ High availability          ✓ Multi-region DR       │
│   ✓ Train users            ✓ Full measure set           ✓ Multi-tenant          │
│                                                                                  │
│   No commitment required—migrate data and config when ready to upgrade          │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## Table of Contents

1. [Why Deployment Flexibility Matters](#why-deployment-flexibility-matters)
2. [Five Deployment Models](#five-deployment-models)
   - [Model 1: Single-Node (Pilot/Dev)](#model-1-single-node-pilotdev)
   - [Model 2: Clustered (Production)](#model-2-clustered-production)
   - [Model 3: Kubernetes (Enterprise)](#model-3-kubernetes-enterprise)
   - [Model 4: Hybrid Cloud](#model-4-hybrid-cloud)
   - [Model 5: SaaS (HDIM-Hosted)](#model-5-saas-hdim-hosted)
3. [Deployment Comparison Matrix](#deployment-comparison-matrix)
4. [Cloud vs On-Premise Decision Guide](#cloud-vs-on-premise-decision-guide)
5. [Modular Service Activation](#modular-service-activation)
6. [Architecture Diagrams](#architecture-diagrams)
7. [Migration Paths](#migration-paths)
8. [Total Cost of Ownership (TCO)](#total-cost-of-ownership-tco)
9. [Industry Terminology Reference](#industry-terminology-reference)
10. [Decision Framework](#decision-framework)

---

## Why Deployment Flexibility Matters

### The Healthcare IT Reality

Healthcare organizations don't fit a one-size-fits-all deployment model:

**Data Residency Requirements**
- State laws requiring PHI to remain in-state (e.g., Texas, California)
- Country-specific regulations (GDPR for EU patients)
- Contractual obligations with payers requiring on-premise data storage

**Existing Infrastructure Investments**
- Data centers with unutilized capacity
- Existing Kubernetes clusters seeking workloads
- VMware/vSphere environments already in production
- Long-term hardware contracts with remaining obligations

**Regulatory Constraints**
- HIPAA BAA requirements limiting cloud provider options
- FedRAMP requirements for government healthcare
- HITRUST certification timelines
- State-specific security frameworks

**IT Team Capabilities**
- Teams comfortable with Docker but not Kubernetes
- Organizations with strong cloud skills but limited on-prem experience
- Lean IT teams needing managed services

### HDIM's Approach: Your Infrastructure, Your Choice

HDIM is architected for portability from the ground up:

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           HDIM PORTABILITY ARCHITECTURE                          │
│                                                                                  │
│    ┌───────────────────────────────────────────────────────────────────────┐    │
│    │                         APPLICATION LAYER                              │    │
│    │  Quality Measure │ CQL Engine │ Care Gap │ FHIR │ Analytics │ Gateway │    │
│    └───────────────────────────────────────────────────────────────────────┘    │
│                                     │                                            │
│    ┌───────────────────────────────────────────────────────────────────────┐    │
│    │                         CONTAINER LAYER                                │    │
│    │              Docker containers (identical across all models)           │    │
│    └───────────────────────────────────────────────────────────────────────┘    │
│                                     │                                            │
│    ┌─────────────┬─────────────┬─────────────┬─────────────┬─────────────┐      │
│    │  Docker     │  Docker     │ Kubernetes  │   Hybrid    │  Managed    │      │
│    │  Compose    │  Swarm      │   (K8s)     │             │    SaaS     │      │
│    │             │             │             │             │             │      │
│    │ Single-Node │  Clustered  │  Enterprise │ Multi-Cloud │ HDIM-Hosted │      │
│    └─────────────┴─────────────┴─────────────┴─────────────┴─────────────┘      │
│                                     │                                            │
│    ┌───────────────────────────────────────────────────────────────────────┐    │
│    │                      INFRASTRUCTURE LAYER                              │    │
│    │   On-Premise │ Private Cloud │ AWS │ Azure │ GCP │ Air-Gapped │ Hybrid │    │
│    └───────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘

Same containers, same configuration, same APIs—different infrastructure
```

**Key Architectural Decisions Enabling Portability:**

1. **Containerized Services**: All 28 HDIM microservices run in Docker containers with identical images across all deployment models

2. **External Configuration**: All environment-specific settings externalized via environment variables and ConfigMaps

3. **Standard Data Layer**: PostgreSQL and Redis with standard interfaces (no proprietary cloud locks)

4. **API-First Design**: REST APIs work identically regardless of deployment model

5. **Helm Charts + Docker Compose**: Same services deployable via `docker-compose.yml` or Kubernetes Helm charts

---

## Five Deployment Models

### Model 1: Single-Node (Pilot/Dev)

**Best For**: Pilot projects, development environments, proof-of-concept, small organizations (<50K patients)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         SINGLE-NODE ARCHITECTURE                                 │
│                                                                                  │
│    ┌─────────────────────────────────────────────────────────────────────────┐  │
│    │                        SINGLE SERVER                                     │  │
│    │                     (4 CPU, 16GB RAM, 500GB SSD)                        │  │
│    │  ┌──────────────────────────────────────────────────────────────────┐  │  │
│    │  │                    Docker Compose                                 │  │  │
│    │  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐   │  │  │
│    │  │  │ Gateway │ │  FHIR   │ │ Quality │ │   CQL   │ │Care Gap │   │  │  │
│    │  │  │ :8001   │ │ :8085   │ │ Measure │ │ Engine  │ │ :8086   │   │  │  │
│    │  │  └────┬────┘ └────┬────┘ │ :8087   │ │ :8081   │ └────┬────┘   │  │  │
│    │  │       │           │      └────┬────┘ └────┬────┘      │        │  │  │
│    │  │       └───────────┴──────────┴───────────┴────────────┘        │  │  │
│    │  │                              │                                  │  │  │
│    │  │  ┌─────────────────────────┬┴────────────────────────────────┐ │  │  │
│    │  │  │      PostgreSQL 15      │           Redis 7                │ │  │  │
│    │  │  │        (Data)           │          (Cache)                 │ │  │  │
│    │  │  │        :5432            │           :6379                  │ │  │  │
│    │  │  └─────────────────────────┴─────────────────────────────────┘ │  │  │
│    │  └──────────────────────────────────────────────────────────────────┘  │  │
│    └─────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│    EXTERNAL CONNECTIONS:                                                        │
│    ├─ EHR FHIR Endpoint (Epic, Cerner, etc.)                                   │
│    ├─ Identity Provider (SSO/OIDC)                                             │
│    └─ Clinical Portal (Web Browser)                                            │
└─────────────────────────────────────────────────────────────────────────────────┘
```

#### Specifications

| Attribute | Specification |
|-----------|---------------|
| **Infrastructure** | 1 server (physical or VM) |
| **Minimum Requirements** | 4 CPU cores, 16GB RAM, 500GB SSD |
| **Recommended Requirements** | 8 CPU cores, 32GB RAM, 1TB NVMe |
| **Operating System** | Ubuntu 22.04 LTS, RHEL 8+, or similar |
| **Container Runtime** | Docker 24.0+ with Docker Compose 2.x |
| **Patient Capacity** | Up to 50,000 patients |
| **Concurrent Users** | 50-500 clinical users |
| **High Availability** | No (single point of failure) |
| **Disaster Recovery** | Manual backup/restore |
| **Setup Time** | 2-3 hours |
| **Monthly Cost** | $200-500 (cloud VM) or $0 (existing hardware) |

#### When to Choose Single-Node

**Choose this model if:**
- You're evaluating HDIM before committing to full deployment
- You have fewer than 50,000 patients
- Acceptable downtime is measured in hours (not minutes)
- You have limited IT resources (1-2 people)
- Budget is constrained
- You need to deploy quickly for a pilot or POC

**Do NOT choose this model if:**
- This is a production clinical system
- You require high availability
- You have more than 50,000 patients
- Zero data loss is critical

#### Quick Start Commands

```bash
# 1. Clone deployment repository
git clone https://github.com/healthdata-im/hdim-deploy.git
cd hdim-deploy/single-node

# 2. Configure environment
cp .env.example .env
# Edit .env with your FHIR server, OAuth2, and database credentials

# 3. Start all services
docker compose up -d

# 4. Verify health
docker compose ps
curl http://localhost:8001/health

# 5. Access Clinical Portal
# Open browser to http://localhost:8001
```

#### Cost Breakdown

| Cost Category | Monthly Estimate | Notes |
|---------------|------------------|-------|
| **Cloud VM (AWS/Azure/GCP)** | $200-400 | t3.xlarge or equivalent |
| **On-Premise Hardware** | $0 (if existing) | Depreciation not included |
| **Storage** | $50-100 | 500GB SSD |
| **Backup** | $20-50 | S3/Blob storage |
| **Monitoring** | $0-50 | Basic or open-source tools |
| **Total** | **$200-500/month** | |

---

### Model 2: Clustered (Production)

**Best For**: Production deployments, medium to large health systems (50K-500K patients), organizations requiring high availability

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         CLUSTERED ARCHITECTURE                                   │
│                                                                                  │
│                           ┌─────────────────┐                                   │
│                           │  Load Balancer  │                                   │
│                           │  (HAProxy/NGINX)│                                   │
│                           │     :443        │                                   │
│                           └────────┬────────┘                                   │
│                                    │                                            │
│            ┌───────────────────────┼───────────────────────┐                   │
│            │                       │                       │                    │
│    ┌───────▼───────┐       ┌───────▼───────┐       ┌───────▼───────┐          │
│    │   Server 1    │       │   Server 2    │       │   Server 3    │          │
│    │ (8 CPU/32GB)  │       │ (8 CPU/32GB)  │       │ (8 CPU/32GB)  │          │
│    │               │       │               │       │               │           │
│    │ ┌───────────┐ │       │ ┌───────────┐ │       │ ┌───────────┐ │          │
│    │ │  Gateway  │ │       │ │  Gateway  │ │       │ │  Gateway  │ │          │
│    │ │   FHIR    │ │       │ │   FHIR    │ │       │ │   FHIR    │ │          │
│    │ │  Quality  │ │       │ │  Quality  │ │       │ │  Quality  │ │          │
│    │ │   CQL     │ │       │ │   CQL     │ │       │ │   CQL     │ │          │
│    │ │ Care Gap  │ │       │ │ Care Gap  │ │       │ │ Care Gap  │ │          │
│    │ └───────────┘ │       │ └───────────┘ │       │ └───────────┘ │          │
│    └───────┬───────┘       └───────┬───────┘       └───────┬───────┘          │
│            │                       │                       │                    │
│            └───────────────────────┼───────────────────────┘                   │
│                                    │                                            │
│    ┌───────────────────────────────┴───────────────────────────────────┐       │
│    │                        DATA LAYER                                  │       │
│    │  ┌────────────────────────┐    ┌────────────────────────┐        │       │
│    │  │    PostgreSQL 15       │    │      Redis Sentinel     │        │       │
│    │  │  Primary + Replica     │    │    Primary + 2 Replica  │        │       │
│    │  │  Streaming Replication │    │   Automatic Failover    │        │       │
│    │  └────────────────────────┘    └────────────────────────┘        │       │
│    └───────────────────────────────────────────────────────────────────┘       │
│                                                                                  │
│    HIGH AVAILABILITY:                                                           │
│    ├─ Service-level failover (LB health checks)                                │
│    ├─ Database replication (sync/async)                                        │
│    ├─ Cache failover (Sentinel monitors)                                       │
│    └─ Failover time: 30 seconds - 2 minutes                                    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

#### Specifications

| Attribute | Specification |
|-----------|---------------|
| **Infrastructure** | 3-5 application servers + data layer |
| **Application Servers** | 8 CPU cores, 32GB RAM each |
| **Database Server** | 8 CPU cores, 64GB RAM, 1TB NVMe (primary + replica) |
| **Cache Server** | 4 CPU cores, 16GB RAM (3 Redis Sentinel nodes) |
| **Load Balancer** | HAProxy, NGINX, or hardware LB |
| **Patient Capacity** | 50,000 - 500,000 patients |
| **Concurrent Users** | 500 - 2,000 clinical users |
| **High Availability** | Yes (service-level with automatic failover) |
| **Disaster Recovery** | Automated with failover to replica |
| **Setup Time** | 1-2 days |
| **Monthly Cost** | $2,000 - $5,000 |

#### When to Choose Clustered

**Choose this model if:**
- This is a production clinical system
- You have 50,000 - 500,000 patients
- You need high availability (acceptable downtime: minutes)
- You have a data center or multiple servers available
- Your IT team has basic DevOps capabilities (2-3 people)
- You want the best balance of reliability, cost, and complexity

**Do NOT choose this model if:**
- You need auto-scaling for unpredictable workloads
- You require multi-region disaster recovery
- You're operating a multi-tenant SaaS platform
- Your DevOps team is very limited or very advanced

#### Infrastructure Requirements

**Application Servers (3-5 servers):**
```yaml
# Per application server
resources:
  cpu: 8 cores
  memory: 32GB RAM
  storage: 500GB SSD
  network: Gigabit Ethernet (10GbE recommended)
  os: Ubuntu 22.04 LTS or RHEL 8+

services_per_server:
  - gateway-service (2 instances)
  - fhir-service (2 instances)
  - quality-measure-service (2 instances)
  - cql-engine-service (2 instances)
  - care-gap-service (1 instance)
  - patient-service (1 instance)
  - analytics-service (1 instance)
```

**Database Layer:**
```yaml
# PostgreSQL (Primary + Replica)
postgresql:
  primary:
    cpu: 8 cores
    memory: 64GB RAM
    storage: 1TB NVMe SSD (RAID 10)
    replication: streaming (synchronous or async)
  replica:
    cpu: 8 cores
    memory: 64GB RAM
    storage: 1TB NVMe SSD
    purpose: read scaling, failover

# Redis Sentinel (3 nodes)
redis:
  sentinel_nodes: 3
  memory_per_node: 8GB
  maxmemory_policy: allkeys-lru
  persistence: AOF (appendfsync everysec)
```

**Load Balancer:**
```yaml
# HAProxy configuration
haproxy:
  algorithm: roundrobin
  health_check:
    interval: 5s
    timeout: 2s
    threshold: 3
  ssl_termination: true
  sticky_sessions: false  # Stateless services
```

#### Cost Breakdown

| Cost Category | Monthly Estimate | Notes |
|---------------|------------------|-------|
| **Application Servers (3x)** | $900-1,500 | Cloud VMs or co-located hardware |
| **Database Server (2x)** | $600-1,200 | Primary + replica |
| **Redis Sentinel (3x)** | $150-300 | Smaller instances |
| **Load Balancer** | $200-500 | HAProxy or cloud LB |
| **Storage (backups)** | $100-300 | Daily snapshots |
| **Monitoring** | $100-200 | Prometheus + Grafana |
| **Total** | **$2,000-5,000/month** | |

#### High Availability Configuration

**PostgreSQL Streaming Replication:**
```yaml
# postgresql.conf (primary)
wal_level = replica
max_wal_senders = 3
synchronous_commit = on
synchronous_standby_names = 'replica1'

# recovery.conf (replica)
primary_conninfo = 'host=primary port=5432 user=replicator'
restore_command = 'cp /archive/%f %p'
trigger_file = '/tmp/postgresql.trigger'
```

**Redis Sentinel Configuration:**
```yaml
# sentinel.conf
sentinel monitor hdim-master 10.0.0.10 6379 2
sentinel down-after-milliseconds hdim-master 5000
sentinel failover-timeout hdim-master 60000
sentinel parallel-syncs hdim-master 1
```

**HAProxy Health Checks:**
```yaml
# haproxy.cfg
backend hdim_gateway
    balance roundrobin
    option httpchk GET /health
    http-check expect status 200
    server gw1 10.0.0.21:8001 check inter 5s fall 3 rise 2
    server gw2 10.0.0.22:8001 check inter 5s fall 3 rise 2
    server gw3 10.0.0.23:8001 check inter 5s fall 3 rise 2
```

---

### Model 3: Kubernetes (Enterprise)

**Best For**: Enterprise deployments, multi-tenant platforms, organizations with existing Kubernetes infrastructure (500K-5M+ patients)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         KUBERNETES ARCHITECTURE                                  │
│                                                                                  │
│                     ┌─────────────────────────────────────┐                     │
│                     │        Ingress Controller           │                     │
│                     │      (NGINX / Traefik / Kong)       │                     │
│                     └──────────────────┬──────────────────┘                     │
│                                        │                                        │
│    ┌───────────────────────────────────┴───────────────────────────────────┐   │
│    │                      KUBERNETES CLUSTER                                │   │
│    │                                                                        │   │
│    │  ┌─────────────────────────────────────────────────────────────────┐  │   │
│    │  │                     CONTROL PLANE (3 nodes)                      │  │   │
│    │  │  ┌─────────┐  ┌─────────┐  ┌─────────┐                         │  │   │
│    │  │  │ kube-   │  │ kube-   │  │ kube-   │  etcd cluster           │  │   │
│    │  │  │apiserver│  │scheduler│  │ controller│ (distributed)          │  │   │
│    │  │  └─────────┘  └─────────┘  └─────────┘                         │  │   │
│    │  └─────────────────────────────────────────────────────────────────┘  │   │
│    │                                                                        │   │
│    │  ┌─────────────────────────────────────────────────────────────────┐  │   │
│    │  │                     WORKER NODES (5-10+)                         │  │   │
│    │  │                                                                  │  │   │
│    │  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │  │   │
│    │  │  │ gateway     │  │ fhir        │  │ quality-    │ Deployments │  │   │
│    │  │  │ (3 replicas)│  │ (3 replicas)│  │ measure     │ with HPA    │  │   │
│    │  │  └─────────────┘  └─────────────┘  │ (3 replicas)│             │  │   │
│    │  │                                    └─────────────┘             │  │   │
│    │  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │  │   │
│    │  │  │ cql-engine  │  │ care-gap    │  │ analytics   │             │  │   │
│    │  │  │ (5 replicas)│  │ (2 replicas)│  │ (2 replicas)│             │  │   │
│    │  │  └─────────────┘  └─────────────┘  └─────────────┘             │  │   │
│    │  │                                                                  │  │   │
│    │  │  ┌───────────────────────────────────────────────────────────┐ │  │   │
│    │  │  │                   STATEFULSETS                             │ │  │   │
│    │  │  │  ┌─────────────────┐     ┌─────────────────┐              │ │  │   │
│    │  │  │  │  PostgreSQL     │     │    Redis        │              │ │  │   │
│    │  │  │  │  (StatefulSet)  │     │  (StatefulSet)  │              │ │  │   │
│    │  │  │  │  Primary+Replica│     │  Sentinel HA    │              │ │  │   │
│    │  │  │  └─────────────────┘     └─────────────────┘              │ │  │   │
│    │  │  └───────────────────────────────────────────────────────────┘ │  │   │
│    │  └─────────────────────────────────────────────────────────────────┘  │   │
│    │                                                                        │   │
│    │  ┌─────────────────────────────────────────────────────────────────┐  │   │
│    │  │                    PERSISTENT STORAGE                            │  │   │
│    │  │  StorageClass: fast-ssd (provisioner: kubernetes.io/aws-ebs)   │  │   │
│    │  │  PersistentVolumeClaims for PostgreSQL, Redis, Kafka           │  │   │
│    │  └─────────────────────────────────────────────────────────────────┘  │   │
│    └────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│    KUBERNETES FEATURES LEVERAGED:                                               │
│    ├─ HorizontalPodAutoscaler (HPA): Auto-scale based on CPU/memory            │
│    ├─ PodDisruptionBudget: Ensure availability during upgrades                 │
│    ├─ NetworkPolicies: Micro-segmentation for security                         │
│    ├─ ServiceMesh (optional): Istio for mTLS, observability                    │
│    └─ Velero: Cluster backup and disaster recovery                             │
└─────────────────────────────────────────────────────────────────────────────────┘
```

#### Specifications

| Attribute | Specification |
|-----------|---------------|
| **Infrastructure** | Kubernetes cluster (3 control + 5-10+ worker nodes) |
| **Control Plane Nodes** | 4 CPU cores, 8GB RAM each (3 nodes for HA) |
| **Worker Nodes** | 8 CPU cores, 32GB RAM each (5-10+ nodes) |
| **Storage Class** | SSD-backed persistent volumes (gp3, Premium SSD) |
| **Container Network** | Calico, Cilium, or Flannel |
| **Ingress Controller** | NGINX, Traefik, or Kong |
| **Patient Capacity** | 500,000 - 5,000,000+ patients |
| **Concurrent Users** | 2,000 - 10,000+ clinical users |
| **High Availability** | Yes (automatic pod rescheduling, self-healing) |
| **Disaster Recovery** | Automated with Velero, multi-region possible |
| **Setup Time** | 3-5 days (with existing K8s cluster) |
| **Monthly Cost** | $5,000 - $15,000 |

#### When to Choose Kubernetes

**Choose this model if:**
- You have 500,000+ patients
- You require auto-scaling for variable workloads
- You operate a multi-tenant SaaS platform
- You have an existing Kubernetes cluster or cloud-managed K8s (EKS, AKS, GKE)
- Your DevOps team has strong Kubernetes experience (4+ people)
- You need zero-downtime deployments
- You require pod-level security isolation

**Do NOT choose this model if:**
- You're a small organization (overkill complexity)
- Your IT team lacks Kubernetes experience
- You need to deploy very quickly (<3 days)
- Budget is severely constrained

#### Helm Chart Configuration

**values.yaml (core configuration):**
```yaml
# HDIM Kubernetes Deployment - values.yaml

global:
  imagePullSecrets:
    - name: hdim-registry
  storageClass: fast-ssd

# Gateway Service
gateway:
  replicas: 3
  image:
    repository: hdim/gateway-service
    tag: "1.0.0"
  resources:
    requests:
      cpu: "500m"
      memory: "1Gi"
    limits:
      cpu: "2"
      memory: "4Gi"
  autoscaling:
    enabled: true
    minReplicas: 3
    maxReplicas: 10
    targetCPUUtilizationPercentage: 70

# FHIR Service
fhir:
  replicas: 3
  resources:
    requests:
      cpu: "1"
      memory: "2Gi"
    limits:
      cpu: "4"
      memory: "8Gi"
  autoscaling:
    enabled: true
    minReplicas: 3
    maxReplicas: 15
    targetCPUUtilizationPercentage: 60

# CQL Engine Service (compute-intensive)
cqlEngine:
  replicas: 5
  resources:
    requests:
      cpu: "2"
      memory: "4Gi"
    limits:
      cpu: "8"
      memory: "16Gi"
  autoscaling:
    enabled: true
    minReplicas: 5
    maxReplicas: 30
    targetCPUUtilizationPercentage: 50

# Quality Measure Service
qualityMeasure:
  replicas: 3
  resources:
    requests:
      cpu: "1"
      memory: "2Gi"
    limits:
      cpu: "4"
      memory: "8Gi"

# Care Gap Service
careGap:
  replicas: 2
  resources:
    requests:
      cpu: "500m"
      memory: "1Gi"
    limits:
      cpu: "2"
      memory: "4Gi"

# PostgreSQL (StatefulSet)
postgresql:
  enabled: true
  architecture: replication
  primary:
    resources:
      requests:
        cpu: "4"
        memory: "16Gi"
      limits:
        cpu: "8"
        memory: "32Gi"
    persistence:
      size: 1Ti
      storageClass: fast-ssd
  readReplicas:
    replicaCount: 2
    resources:
      requests:
        cpu: "2"
        memory: "8Gi"
      limits:
        cpu: "4"
        memory: "16Gi"

# Redis (StatefulSet with Sentinel)
redis:
  enabled: true
  architecture: replication
  sentinel:
    enabled: true
    masterSet: hdim-master
  master:
    resources:
      requests:
        cpu: "1"
        memory: "4Gi"
      limits:
        cpu: "2"
        memory: "8Gi"
  replica:
    replicaCount: 2

# Network Policies
networkPolicies:
  enabled: true

# Pod Disruption Budgets
podDisruptionBudget:
  enabled: true
  minAvailable: 2  # Ensure at least 2 pods during disruptions
```

**HorizontalPodAutoscaler Example:**
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: cql-engine-hpa
  namespace: hdim
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: cql-engine-service
  minReplicas: 5
  maxReplicas: 30
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 50
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 70
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Percent
        value: 100
        periodSeconds: 60
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 50
        periodSeconds: 120
```

#### Cost Breakdown

| Cost Category | Monthly Estimate | Notes |
|---------------|------------------|-------|
| **Control Plane** | $200-500 | Managed K8s (EKS, AKS, GKE) or self-managed |
| **Worker Nodes (7x)** | $2,100-4,200 | 8 vCPU, 32GB instances |
| **Database (managed)** | $1,000-2,500 | RDS PostgreSQL or self-managed StatefulSet |
| **Redis (managed)** | $200-500 | ElastiCache or self-managed |
| **Storage (PV)** | $500-1,000 | SSD persistent volumes |
| **Load Balancer** | $200-500 | Cloud ALB/NLB |
| **Monitoring** | $300-500 | Prometheus, Grafana, logs |
| **Backup (Velero)** | $100-300 | S3 snapshots |
| **Total** | **$5,000-15,000/month** | |

---

### Model 4: Hybrid Cloud

**Best For**: Organizations with multi-region requirements, data residency constraints, existing on-premise investments combined with cloud elasticity

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           HYBRID CLOUD ARCHITECTURE                              │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                           ON-PREMISE                                      │   │
│  │                      (Your Data Center)                                   │   │
│  │                                                                           │   │
│  │   ┌─────────────┐     ┌─────────────┐     ┌─────────────┐               │   │
│  │   │   Gateway   │     │    FHIR     │     │    Kong     │               │   │
│  │   │  (Primary)  │────▶│   Server    │────▶│  API GW    │               │   │
│  │   │             │     │  (PHI Data) │     │ (Auth/Rate) │               │   │
│  │   └──────┬──────┘     └──────┬──────┘     └─────────────┘               │   │
│  │          │                    │                                          │   │
│  │          │    ┌───────────────┴────────────────┐                        │   │
│  │          │    │     PostgreSQL Primary         │                        │   │
│  │          │    │     (All PHI remains on-prem)  │                        │   │
│  │          │    └───────────────┬────────────────┘                        │   │
│  │          │                    │                                          │   │
│  │          │            Encrypted VPN / Direct Connect                     │   │
│  └──────────┼────────────────────┼──────────────────────────────────────────┘   │
│             │                    │                                              │
│             │    ════════════════╪═══════════════                              │
│             │         SECURE CONNECTION                                        │
│             │    ════════════════╪═══════════════                              │
│             │                    │                                              │
│  ┌──────────┼────────────────────┼──────────────────────────────────────────┐   │
│  │          │                    │              CLOUD (AWS/Azure/GCP)        │   │
│  │          ▼                    ▼                                           │   │
│  │   ┌─────────────┐     ┌─────────────┐                                    │   │
│  │   │   Quality   │     │    CQL      │                                    │   │
│  │   │   Measure   │     │   Engine    │     ┌─────────────┐               │   │
│  │   │  (Compute)  │────▶│  (Compute)  │────▶│  Analytics  │               │   │
│  │   └─────────────┘     └─────────────┘     │ (Reporting) │               │   │
│  │          │                    │            └─────────────┘               │   │
│  │          │                    │                                          │   │
│  │   ┌──────┴────────────────────┴──────┐                                   │   │
│  │   │         Kubernetes Cluster        │                                   │   │
│  │   │   - Auto-scaling compute          │                                   │   │
│  │   │   - Read replica (non-PHI cache)  │                                   │   │
│  │   │   - Burst capacity for evaluations│                                   │   │
│  │   └───────────────────────────────────┘                                   │   │
│  │                                                                           │   │
│  │   Multi-Region Disaster Recovery:                                        │   │
│  │   ├─ Region 1: US-East (Primary cloud services)                         │   │
│  │   └─ Region 2: US-West (Standby for DR)                                 │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│   DATA FLOW:                                                                    │
│   1. Clinical portal connects to on-prem gateway                               │
│   2. Gateway authenticates via on-prem Kong                                    │
│   3. PHI queries served from on-prem PostgreSQL                                │
│   4. Measure evaluation requests routed to cloud K8s                           │
│   5. Cloud services use anonymized/tokenized patient references                │
│   6. Results returned to on-prem for final PHI resolution                      │
└─────────────────────────────────────────────────────────────────────────────────┘
```

#### Specifications

| Attribute | Specification |
|-----------|---------------|
| **On-Premise Component** | Gateway + FHIR + PostgreSQL (1-3 servers) |
| **Cloud Component** | Kubernetes cluster (5+ worker nodes) |
| **Connection** | VPN tunnel or AWS Direct Connect / Azure ExpressRoute |
| **Patient Capacity** | Any (design for your scale) |
| **High Availability** | Yes (cloud auto-scaling + on-prem HA) |
| **Disaster Recovery** | Multi-region (cloud) + on-prem backup |
| **Data Residency** | PHI on-premise, compute in cloud |
| **Setup Time** | 1-2 weeks |
| **Monthly Cost** | $10,000 - $20,000 |

#### When to Choose Hybrid Cloud

**Choose this model if:**
- You have data residency requirements (PHI must stay on-premise)
- You need burst compute capacity for large evaluations
- You want to leverage cloud auto-scaling without moving PHI
- You have existing on-premise investments to protect
- You require multi-region disaster recovery
- You're operating a national health network

**Do NOT choose this model if:**
- You can fully commit to cloud (simpler architecture)
- Your IT team is small (<4 people)
- Budget is constrained
- Low latency is critical (<10ms for all operations)

#### Network Architecture

**VPN Configuration (Site-to-Site):**
```yaml
# AWS VPN Gateway Example
vpn_gateway:
  type: aws_vpn_gateway
  vpc_id: vpc-xxxxx

customer_gateway:
  type: aws_customer_gateway
  bgp_asn: 65000
  ip_address: "203.0.113.1"  # Your on-prem public IP

vpn_connection:
  type: aws_vpn_connection
  customer_gateway_id: cgw-xxxxx
  vpn_gateway_id: vgw-xxxxx
  type: ipsec.1
  tunnel1_inside_cidr: "169.254.10.0/30"
  tunnel2_inside_cidr: "169.254.11.0/30"
```

**Direct Connect (High Throughput):**
```yaml
# AWS Direct Connect for low-latency, high-bandwidth
direct_connect:
  connection:
    bandwidth: 1Gbps  # or 10Gbps for enterprise
    location: "EqDC2"  # Equinix DC2
  virtual_interface:
    vlan: 101
    bgp_asn: 65000
    address_family: ipv4
```

#### Cost Breakdown

| Cost Category | Monthly Estimate | Notes |
|---------------|------------------|-------|
| **On-Premise (Gateway/FHIR/DB)** | $2,000-4,000 | 3 servers + storage |
| **Cloud Kubernetes** | $4,000-8,000 | 5-7 worker nodes |
| **VPN/Direct Connect** | $500-2,000 | VPN ($500) or DC ($1,500+) |
| **Cloud Database (replica)** | $500-1,500 | Non-PHI cache |
| **Cloud Storage** | $300-800 | Logs, backups |
| **Monitoring** | $400-800 | Multi-site monitoring |
| **Total** | **$10,000-20,000/month** | |

---

### Model 5: SaaS (HDIM-Hosted)

**Best For**: Fastest time-to-value, minimal IT overhead, organizations wanting to focus on clinical outcomes rather than infrastructure

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          SAAS (HDIM-HOSTED) ARCHITECTURE                         │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                         YOUR ORGANIZATION                                 │   │
│  │                                                                           │   │
│  │   ┌─────────────┐     ┌─────────────┐     ┌─────────────┐               │   │
│  │   │  Clinical   │     │    EHR      │     │   Identity  │               │   │
│  │   │  Portal     │     │   (Epic,    │     │  Provider   │               │   │
│  │   │ (Browser)   │     │   Cerner)   │     │   (Okta,    │               │   │
│  │   └──────┬──────┘     └──────┬──────┘     │    AD)      │               │   │
│  │          │                   │            └──────┬──────┘               │   │
│  │          │                   │                   │                       │   │
│  └──────────┼───────────────────┼───────────────────┼───────────────────────┘   │
│             │                   │                   │                           │
│             │    ═══════════════╪═══════════════════╪═══════════════            │
│             │          HTTPS (TLS 1.3) / SFTP / FHIR API                        │
│             │    ═══════════════╪═══════════════════╪═══════════════            │
│             │                   │                   │                           │
│  ┌──────────┼───────────────────┼───────────────────┼───────────────────────┐   │
│  │          ▼                   ▼                   ▼                        │   │
│  │                       HDIM-HOSTED CLOUD                                   │   │
│  │                    (Fully Managed by HDIM)                               │   │
│  │                                                                           │   │
│  │   ┌─────────────────────────────────────────────────────────────────┐   │   │
│  │   │                      TENANT ISOLATION                            │   │   │
│  │   │  ┌─────────────────────────────────────────────────────────┐   │   │   │
│  │   │  │                  YOUR TENANT (Isolated)                  │   │   │   │
│  │   │  │                                                          │   │   │   │
│  │   │  │   Gateway ─▶ FHIR ─▶ Quality ─▶ CQL ─▶ Care Gap        │   │   │   │
│  │   │  │                                                          │   │   │   │
│  │   │  │   ┌───────────────────┐   ┌───────────────────┐        │   │   │   │
│  │   │  │   │  Your Database    │   │   Your Cache      │        │   │   │   │
│  │   │  │   │  (Dedicated)      │   │   (Isolated)      │        │   │   │   │
│  │   │  │   └───────────────────┘   └───────────────────┘        │   │   │   │
│  │   │  └──────────────────────────────────────────────────────────┘   │   │   │
│  │   │                                                                  │   │   │
│  │   │  ┌───────────────────────────────────────────────────────────┐ │   │   │
│  │   │  │  Other Tenants (Isolated - No shared PHI access)          │ │   │   │
│  │   │  └───────────────────────────────────────────────────────────┘ │   │   │
│  │   └─────────────────────────────────────────────────────────────────┘   │   │
│  │                                                                           │   │
│  │   INCLUDED IN SAAS:                                                      │   │
│  │   ├─ 99.9% SLA uptime guarantee                                          │   │
│  │   ├─ Automatic updates and patches                                       │   │
│  │   ├─ 24/7 monitoring and alerting                                        │   │
│  │   ├─ Daily backups with 30-day retention                                 │   │
│  │   ├─ Multi-region disaster recovery (RTO <1 hour)                        │   │
│  │   ├─ SOC 2 Type II certified                                             │   │
│  │   ├─ HITRUST CSF certified                                               │   │
│  │   └─ BAA included                                                        │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│   ONBOARDING PROCESS:                                                           │
│   Day 1: Sign BAA, provision tenant                                            │
│   Day 2: Configure SSO integration                                             │
│   Day 3: Configure FHIR endpoint connection                                    │
│   Day 4-5: Data validation and user training                                   │
│   Day 5: Go-live with first measure evaluation                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

#### Specifications

| Attribute | Specification |
|-----------|---------------|
| **Infrastructure** | Fully managed by HDIM |
| **Your Responsibility** | FHIR endpoint access, SSO config, user training |
| **Patient Capacity** | Up to 1,000,000+ per tenant |
| **Concurrent Users** | Unlimited (fair use policy) |
| **High Availability** | 99.9% SLA |
| **Disaster Recovery** | Multi-region, RTO <1 hour, RPO <1 hour |
| **Security** | SOC 2 Type II, HITRUST CSF, HIPAA BAA |
| **Updates** | Automatic (zero-downtime deployments) |
| **Setup Time** | 1-5 days |
| **Monthly Cost** | $3,000 - $8,000 (tenant-based pricing) |

#### When to Choose SaaS

**Choose this model if:**
- You want the fastest time-to-value
- Your IT team is small or focused on other priorities
- You don't have existing infrastructure investments to protect
- You prefer predictable monthly costs
- You want automatic updates and maintenance
- You can accept cloud-only PHI storage

**Do NOT choose this model if:**
- You have strict data residency requirements (PHI must stay on-premise)
- You require air-gapped deployment
- You need extensive customization beyond configuration
- You prefer maximum control over infrastructure

#### Pricing Tiers

| Tier | Patient Volume | Measures | Users | Price/Month |
|------|---------------|----------|-------|-------------|
| **Starter** | Up to 25,000 | 10 | 25 | $3,000 |
| **Professional** | 25,000 - 100,000 | 25 | 100 | $5,000 |
| **Enterprise** | 100,000 - 500,000 | 50 | 500 | $7,000 |
| **Custom** | 500,000+ | Unlimited | Unlimited | Custom pricing |

**What's Included:**
- All HDIM services (Quality Measure, CQL Engine, Care Gap, Analytics, etc.)
- Dedicated tenant database (no shared PHI)
- Standard integrations (Epic, Cerner, athenahealth, generic FHIR)
- Standard SSO (SAML 2.0, OIDC)
- Email support (Professional), phone support (Enterprise)
- Quarterly business reviews (Enterprise)

#### Cost Comparison

| Cost Category | SaaS | Self-Hosted (Clustered) |
|---------------|------|-------------------------|
| **Monthly Fee** | $5,000 | $0 |
| **Infrastructure** | $0 | $3,500 |
| **Personnel (0.5 FTE)** | $0 | $5,000 |
| **Monitoring Tools** | $0 | $300 |
| **Backup/DR** | $0 | $500 |
| **Total Monthly** | **$5,000** | **$9,300** |
| **Year 1 Total** | **$60,000** | **$111,600** |

*SaaS removes the hidden costs of self-management.*

---

## Deployment Comparison Matrix

### Quick Reference Table

| Attribute | Single-Node | Clustered | Kubernetes | Hybrid | SaaS |
|-----------|-------------|-----------|------------|--------|------|
| **Best For** | Pilot/POC | Production | Enterprise | Multi-Region | Speed |
| **Patient Capacity** | <50K | 50K-500K | 500K-5M+ | Any | <1M |
| **Concurrent Users** | <500 | 500-2K | 2K-10K+ | Any | Any |
| **Setup Time** | 2-3 hours | 1-2 days | 3-5 days | 1-2 weeks | 1-5 days |
| **Monthly Cost** | $200-500 | $2-5K | $5-15K | $10-20K | $3-8K |
| **High Availability** | No | Yes | Yes | Yes | Yes |
| **Auto-Scaling** | No | No | Yes | Yes (cloud) | Yes |
| **Multi-Tenant** | No | Yes | Yes | Yes | Yes |
| **Data Residency** | On-prem | On-prem | On-prem/Cloud | Split | Cloud |
| **DevOps Required** | Minimal | Basic | Advanced | Advanced | None |
| **IT Team Size** | 1-2 | 2-3 | 4+ | 4+ | 0-1 |

### Performance Characteristics

| Metric | Single-Node | Clustered | Kubernetes | Hybrid | SaaS |
|--------|-------------|-----------|------------|--------|------|
| **CQL Eval (per patient)** | <600ms | <500ms | <400ms | <450ms | <500ms |
| **FHIR Query (p95)** | <200ms | <150ms | <100ms | <150ms | <150ms |
| **Throughput (patients/hour)** | 6K | 50K | 200K+ | 150K+ | 100K+ |
| **Cache Hit Rate** | >60% | >80% | >90% | >85% | >85% |
| **Failover Time** | Manual | 30s-2min | <30s | <30s | <30s |
| **Recovery Point (RPO)** | Hours | Minutes | Minutes | Minutes | Minutes |
| **Recovery Time (RTO)** | Hours | Minutes | Minutes | Minutes | <1 hour |

---

## Cloud vs On-Premise Decision Guide

### Decision Framework

```
START
  │
  ├─ Do you have data residency requirements?
  │   ├─ YES → Consider ON-PREMISE or HYBRID
  │   └─ NO  → Continue
  │
  ├─ Do you have existing on-premise infrastructure?
  │   ├─ YES with capacity → Consider ON-PREMISE (leverage existing)
  │   └─ NO or limited    → Continue
  │
  ├─ What's your IT team's cloud expertise?
  │   ├─ Strong cloud skills   → Consider CLOUD or SAAS
  │   ├─ Strong on-prem skills → Consider ON-PREMISE
  │   └─ Limited skills        → Consider SAAS
  │
  ├─ Do you need burst capacity for large evaluations?
  │   ├─ YES → Consider CLOUD or HYBRID
  │   └─ NO  → Continue
  │
  ├─ What's your budget preference?
  │   ├─ CapEx (upfront)    → ON-PREMISE
  │   ├─ OpEx (monthly)     → CLOUD or SAAS
  │   └─ Mixed              → HYBRID
  │
  └─ Final Decision: [ON-PREMISE | CLOUD | HYBRID | SAAS]
```

### Detailed Comparison

| Factor | On-Premise | Cloud | Hybrid |
|--------|------------|-------|--------|
| **Data Location** | Your datacenter | Cloud provider | Split |
| **Capital Expense** | High (hardware) | None | Medium |
| **Operating Expense** | Lower long-term | Higher short-term | Mixed |
| **Scalability** | Manual | Automatic | Automatic (cloud part) |
| **Maintenance** | Your team | Cloud provider | Split |
| **Compliance Control** | Full | Shared responsibility | Full (on-prem) |
| **Network Latency** | Lowest | Depends on region | Variable |
| **Vendor Lock-In** | None | Moderate | Low |
| **Disaster Recovery** | Your responsibility | Cloud-managed | Mixed |

### When Cloud is Better

1. **Variable Workloads**: Annual HEDIS measure runs create 10x load spikes
2. **Multi-Region**: Need presence in multiple geographic regions
3. **Limited IT Staff**: Can't dedicate resources to infrastructure management
4. **Rapid Scaling**: Growing patient population, uncertain future scale
5. **No Data Center**: Don't have existing on-premise infrastructure

### When On-Premise is Better

1. **Data Residency**: Legal requirements for PHI location
2. **Existing Investment**: Significant infrastructure already in place
3. **Long-Term Cost**: Plan to operate for 5+ years (TCO favors on-prem)
4. **Control Requirements**: Need full control over hardware, networking, security
5. **Air-Gapped Networks**: Government or high-security environments

---

## Modular Service Activation

### The "Pay for What You Need" Model

HDIM services are modular—enable only what you need, add more later.

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        HDIM SERVICE ACTIVATION MATRIX                            │
│                                                                                  │
│   ┌───────────────────────────────────────────────────────────────────────────┐ │
│   │                         CORE SERVICES (Required)                          │ │
│   │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐          │ │
│   │  │    Gateway      │  │     FHIR        │  │    Patient      │          │ │
│   │  │   Service       │  │    Service      │  │    Service      │          │ │
│   │  │ (Auth, Routing) │  │ (Data Access)   │  │ (Patient Mgmt)  │          │ │
│   │  └─────────────────┘  └─────────────────┘  └─────────────────┘          │ │
│   └───────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│   ┌───────────────────────────────────────────────────────────────────────────┐ │
│   │                       QUALITY SERVICES (Common)                           │ │
│   │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐          │ │
│   │  │  Quality        │  │    CQL          │  │   Care Gap      │          │ │
│   │  │  Measure        │  │   Engine        │  │    Service      │          │ │
│   │  │ (HEDIS/MIPS)    │  │ (Evaluation)    │  │  (Gap Closure)  │          │ │
│   │  └─────────────────┘  └─────────────────┘  └─────────────────┘          │ │
│   └───────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│   ┌───────────────────────────────────────────────────────────────────────────┐ │
│   │                      ADVANCED SERVICES (Optional)                         │ │
│   │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐          │ │
│   │  │   Analytics     │  │  Predictive     │  │     HCC         │          │ │
│   │  │   Service       │  │  Analytics      │  │   (Risk Adj)    │          │ │
│   │  │ (Reporting)     │  │ (ML Models)     │  │ (HCC v28)       │          │ │
│   │  └─────────────────┘  └─────────────────┘  └─────────────────┘          │ │
│   │                                                                           │ │
│   │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐          │ │
│   │  │   Prior Auth    │  │     SDOH        │  │     QRDA        │          │ │
│   │  │   Service       │  │    Service      │  │    Export       │          │ │
│   │  │ (Automation)    │  │ (Social Det.)   │  │ (CMS Submission)│          │ │
│   │  └─────────────────┘  └─────────────────┘  └─────────────────┘          │ │
│   └───────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│   ┌───────────────────────────────────────────────────────────────────────────┐ │
│   │                     INTEGRATION SERVICES (As Needed)                      │ │
│   │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐          │ │
│   │  │  EHR Connector  │  │   ADT Event     │  │    Consent      │          │ │
│   │  │ (Epic, Cerner)  │  │   Processor     │  │    Service      │          │ │
│   │  │                 │  │ (HL7 v2 Msgs)   │  │ (Patient Auth)  │          │ │
│   │  └─────────────────┘  └─────────────────┘  └─────────────────┘          │ │
│   └───────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Service Bundles

**Starter Bundle** (Basic Quality Measurement)
- Gateway Service
- FHIR Service
- Patient Service
- Quality Measure Service
- CQL Engine Service
- *Monthly Cost Multiplier: 1.0x*

**Standard Bundle** (Quality + Care Gaps)
- *All Starter Bundle services*
- Care Gap Service
- Analytics Service
- *Monthly Cost Multiplier: 1.3x*

**Advanced Bundle** (Full Platform)
- *All Standard Bundle services*
- Predictive Analytics Service
- HCC (Risk Adjustment) Service
- QRDA Export Service
- SDOH Service
- *Monthly Cost Multiplier: 1.8x*

**Enterprise Bundle** (Multi-Tenant Platform)
- *All Advanced Bundle services*
- Prior Auth Service
- EHR Connector Service (multi-EHR)
- ADT Event Processor
- Consent Service
- *Monthly Cost Multiplier: 2.5x*

### Configuration Example

**docker-compose.yml (Enable only selected services):**
```yaml
version: '3.8'

services:
  # CORE (Always enabled)
  gateway:
    image: hdim/gateway-service:latest
    profiles: ["core"]

  fhir:
    image: hdim/fhir-service:latest
    profiles: ["core"]

  patient:
    image: hdim/patient-service:latest
    profiles: ["core"]

  # QUALITY (Enable with --profile quality)
  quality-measure:
    image: hdim/quality-measure-service:latest
    profiles: ["quality"]

  cql-engine:
    image: hdim/cql-engine-service:latest
    profiles: ["quality"]

  care-gap:
    image: hdim/care-gap-service:latest
    profiles: ["quality"]

  # ADVANCED (Enable with --profile advanced)
  analytics:
    image: hdim/analytics-service:latest
    profiles: ["advanced"]

  predictive:
    image: hdim/predictive-analytics-service:latest
    profiles: ["advanced"]

  hcc:
    image: hdim/hcc-service:latest
    profiles: ["advanced"]

# Start with:
# docker compose --profile core --profile quality up -d
```

---

## Architecture Diagrams

### Complete HDIM Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              HDIM COMPLETE ARCHITECTURE                                  │
│                                                                                          │
│     EXTERNAL SYSTEMS                                                                     │
│    ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌───────────┐           │
│    │  Epic   │ │ Cerner  │ │ Athena  │ │ Generic │ │  HIE    │ │ CMS BCDA  │           │
│    │ (FHIR)  │ │ (FHIR)  │ │ (FHIR)  │ │ (FHIR)  │ │(CommonW)│ │  (Bulk)   │           │
│    └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └─────┬─────┘           │
│         │           │           │           │           │             │                 │
│         └───────────┴───────────┴─────┬─────┴───────────┴─────────────┘                 │
│                                       │                                                  │
│                               ┌───────▼───────┐                                         │
│                               │ EHR Connector │   Multi-EHR Adapter                     │
│                               │   Service     │   (OAuth2, Rate Limiting)               │
│                               └───────┬───────┘                                         │
│                                       │                                                  │
│    ┌──────────────────────────────────┼──────────────────────────────────────────────┐  │
│    │                         API GATEWAY LAYER                                        │  │
│    │                                                                                  │  │
│    │   ┌────────────────┐      ┌────────────────┐      ┌────────────────┐           │  │
│    │   │   Kong API     │──────│    Gateway     │──────│    Identity    │           │  │
│    │   │   Gateway      │      │    Service     │      │    Provider    │           │  │
│    │   │ (Rate Limit,   │      │ (Auth, Route,  │      │  (JWT, OIDC,   │           │  │
│    │   │  TLS, WAF)     │      │  Tenant)       │      │   SAML)        │           │  │
│    │   └────────────────┘      └───────┬────────┘      └────────────────┘           │  │
│    │                                   │                                             │  │
│    └───────────────────────────────────┼─────────────────────────────────────────────┘  │
│                                        │                                                 │
│    ┌───────────────────────────────────┼─────────────────────────────────────────────┐  │
│    │                      MICROSERVICES LAYER (28 Services)                          │  │
│    │                                                                                  │  │
│    │   ┌─────────────────────────────────────────────────────────────────────────┐  │  │
│    │   │                           CORE SERVICES                                  │  │  │
│    │   │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐               │  │  │
│    │   │  │   FHIR   │  │ Patient  │  │ Clinical │  │ Provider │               │  │  │
│    │   │  │ Service  │  │ Service  │  │ Service  │  │ Service  │               │  │  │
│    │   │  │  :8085   │  │  :8084   │  │  :8088   │  │  :8089   │               │  │  │
│    │   │  └──────────┘  └──────────┘  └──────────┘  └──────────┘               │  │  │
│    │   └─────────────────────────────────────────────────────────────────────────┘  │  │
│    │                                                                                  │  │
│    │   ┌─────────────────────────────────────────────────────────────────────────┐  │  │
│    │   │                         QUALITY SERVICES                                 │  │  │
│    │   │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐               │  │  │
│    │   │  │ Quality  │  │   CQL    │  │ Care Gap │  │ Analytics│               │  │  │
│    │   │  │ Measure  │  │  Engine  │  │ Service  │  │ Service  │               │  │  │
│    │   │  │  :8087   │  │  :8081   │  │  :8086   │  │  :8090   │               │  │  │
│    │   │  └──────────┘  └──────────┘  └──────────┘  └──────────┘               │  │  │
│    │   └─────────────────────────────────────────────────────────────────────────┘  │  │
│    │                                                                                  │  │
│    │   ┌─────────────────────────────────────────────────────────────────────────┐  │  │
│    │   │                        ADVANCED SERVICES                                 │  │  │
│    │   │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐               │  │  │
│    │   │  │Predictive│  │   HCC    │  │   QRDA   │  │   SDOH   │               │  │  │
│    │   │  │Analytics │  │ Service  │  │  Export  │  │ Service  │               │  │  │
│    │   │  │  :8091   │  │  :8092   │  │  :8093   │  │  :8094   │               │  │  │
│    │   │  └──────────┘  └──────────┘  └──────────┘  └──────────┘               │  │  │
│    │   └─────────────────────────────────────────────────────────────────────────┘  │  │
│    │                                                                                  │  │
│    └──────────────────────────────────┬──────────────────────────────────────────────┘  │
│                                       │                                                  │
│    ┌──────────────────────────────────┼──────────────────────────────────────────────┐  │
│    │                            DATA LAYER                                            │  │
│    │                                                                                  │  │
│    │   ┌────────────────────────┐  ┌────────────────────────┐  ┌──────────────────┐ │  │
│    │   │     PostgreSQL 15      │  │       Redis 7          │  │     Kafka 3      │ │  │
│    │   │                        │  │                        │  │                  │ │  │
│    │   │  Multi-tenant schemas  │  │  PHI Cache (5min TTL)  │  │  Event streaming │ │  │
│    │   │  Streaming replication │  │  Session cache         │  │  Audit events    │ │  │
│    │   │  Point-in-time recovery│  │  Query cache           │  │  Integration     │ │  │
│    │   │                        │  │                        │  │                  │ │  │
│    │   │       :5432            │  │        :6379           │  │      :9092       │ │  │
│    │   └────────────────────────┘  └────────────────────────┘  └──────────────────┘ │  │
│    │                                                                                  │  │
│    └──────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                          │
│    ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│    │                          OBSERVABILITY LAYER                                     │  │
│    │                                                                                  │  │
│    │   ┌────────────────┐  ┌────────────────┐  ┌────────────────┐                   │  │
│    │   │   Prometheus   │  │    Grafana     │  │   ELK Stack    │                   │  │
│    │   │   (Metrics)    │  │  (Dashboards)  │  │    (Logs)      │                   │  │
│    │   │    :9090       │  │    :3001       │  │    :5601       │                   │  │
│    │   └────────────────┘  └────────────────┘  └────────────────┘                   │  │
│    │                                                                                  │  │
│    └──────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                          │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

### Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                               HDIM DATA FLOW                                             │
│                                                                                          │
│    STEP 1: AUTHENTICATION                                                               │
│    ┌──────────┐     ┌──────────┐     ┌──────────┐                                       │
│    │ Clinical │────▶│  Kong    │────▶│  Gateway │                                       │
│    │  Portal  │     │ Gateway  │     │ Service  │                                       │
│    │          │     │ (TLS/WAF)│     │ (JWT Val)│                                       │
│    └──────────┘     └──────────┘     └─────┬────┘                                       │
│                                            │                                             │
│                                            ▼                                             │
│    ┌─────────────────────────────────────────────────────────────────────────────────┐  │
│    │                      HDIM TRUST HEADERS                                          │  │
│    │   X-Auth-User-Id: user-123                                                       │  │
│    │   X-Auth-Tenant-Ids: TENANT001,TENANT002                                        │  │
│    │   X-Auth-Roles: ADMIN,EVALUATOR                                                 │  │
│    │   X-Auth-Validated: <HMAC signature>                                            │  │
│    └─────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                          │
│    STEP 2: MEASURE EVALUATION REQUEST                                                   │
│    ┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐                     │
│    │ Gateway  │────▶│ Quality  │────▶│   CQL    │────▶│   FHIR   │                     │
│    │ Service  │     │ Measure  │     │  Engine  │     │ Service  │                     │
│    │          │     │ Service  │     │          │     │          │                     │
│    └──────────┘     └────┬─────┘     └────┬─────┘     └────┬─────┘                     │
│                          │                │                │                            │
│                          │                │                │                            │
│    STEP 3: DATA RETRIEVAL                 │                │                            │
│                          │                │                ▼                            │
│    ┌──────────┐     ┌────┴─────┐     ┌────┴─────┐     ┌──────────┐     ┌──────────┐   │
│    │   EHR    │◀────│   EHR    │◀────│ Patient  │────▶│  Redis   │────▶│PostgreSQL│   │
│    │ (Epic)   │     │Connector │     │  Data    │     │ (Cache)  │     │ (Store)  │   │
│    └──────────┘     └──────────┘     └──────────┘     └──────────┘     └──────────┘   │
│                                                                                          │
│    STEP 4: CQL EVALUATION (per patient)                                                 │
│    ┌─────────────────────────────────────────────────────────────────────────────────┐  │
│    │                                                                                  │  │
│    │    Input: Patient FHIR Bundle (Conditions, Observations, Medications, etc.)     │  │
│    │                                     │                                            │  │
│    │                                     ▼                                            │  │
│    │    ┌─────────────────────────────────────────────────────────────────────────┐ │  │
│    │    │                        CQL ENGINE                                        │ │  │
│    │    │                                                                          │ │  │
│    │    │   library "DiabetesControl" version '1.0.0'                             │ │  │
│    │    │                                                                          │ │  │
│    │    │   define "In Initial Population":                                        │ │  │
│    │    │     AgeInYearsAt(start of "Measurement Period") >= 18                   │ │  │
│    │    │     and exists [Condition: "Diabetes Diagnosis"]                         │ │  │
│    │    │                                                                          │ │  │
│    │    │   define "In Denominator":                                               │ │  │
│    │    │     "In Initial Population"                                              │ │  │
│    │    │                                                                          │ │  │
│    │    │   define "In Numerator":                                                 │ │  │
│    │    │     exists [Observation: "HbA1c Test"]                                   │ │  │
│    │    │       where result < 7 '%'                                               │ │  │
│    │    │                                                                          │ │  │
│    │    └─────────────────────────────────────────────────────────────────────────┘ │  │
│    │                                     │                                            │  │
│    │                                     ▼                                            │  │
│    │    Output: MeasureReport (FHIR R4)                                              │  │
│    │      - initialPopulation: true                                                  │  │
│    │      - denominator: true                                                        │  │
│    │      - numerator: false (HbA1c = 7.2%)                                         │  │
│    │      - careGap: "HbA1c control needed"                                         │  │
│    │                                                                                  │  │
│    └─────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                          │
│    STEP 5: RESPONSE & CARE GAP                                                          │
│    ┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐                     │
│    │ Clinical │◀────│ Gateway  │◀────│ Quality  │◀────│ Care Gap │                     │
│    │ Portal   │     │ Service  │     │ Measure  │     │ Service  │                     │
│    │          │     │          │     │          │     │          │                     │
│    │ Display: │     └──────────┘     └──────────┘     └──────────┘                     │
│    │ - Score  │                                                                         │
│    │ - Gaps   │                                                                         │
│    │ - Actions│                                                                         │
│    └──────────┘                                                                         │
│                                                                                          │
│    PERFORMANCE TARGETS:                                                                 │
│    ├─ Step 1-2: <50ms (Auth + Routing)                                                 │
│    ├─ Step 3: <200ms (FHIR Data Retrieval, cache hit)                                  │
│    ├─ Step 4: <300ms (CQL Evaluation per patient)                                      │
│    ├─ Step 5: <50ms (Response formatting)                                              │
│    └─ Total: <500ms (per-patient measure evaluation)                                   │
│                                                                                          │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## Migration Paths

### Path 1: Pilot → Production

**Scenario**: You deployed Single-Node for a pilot, now ready to scale to production.

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                        PILOT TO PRODUCTION MIGRATION                                     │
│                                                                                          │
│   WEEK 1-4: PILOT                     WEEK 5-6: MIGRATION          WEEK 7-8: PRODUCTION  │
│   (Single-Node)                       (Parallel Run)               (Clustered)           │
│                                                                                          │
│   ┌─────────────────┐                 ┌─────────────────┐          ┌─────────────────┐  │
│   │ Single Server   │                 │ Single + Cluster │          │ Cluster Only    │  │
│   │                 │ ───Migrate───▶  │ (parallel)       │ ───Cut───▶│                 │  │
│   │ - 5K patients   │    data         │                  │  over    │ - 500K patients │  │
│   │ - 5 measures    │                 │ - Sync data      │          │ - 50 measures   │  │
│   │ - 50 users      │                 │ - Validate       │          │ - 500 users     │  │
│   │                 │                 │ - Test failover  │          │ - HA enabled    │  │
│   └─────────────────┘                 └─────────────────┘          └─────────────────┘  │
│                                                                                          │
│   MIGRATION STEPS:                                                                       │
│   1. Provision clustered infrastructure (3-5 servers, LB, replica DB)                   │
│   2. Deploy HDIM services on cluster (same Docker images)                               │
│   3. Export data from pilot PostgreSQL (pg_dump)                                        │
│   4. Import data to cluster PostgreSQL (pg_restore)                                     │
│   5. Configure Redis replication                                                        │
│   6. Update DNS to point to cluster LB                                                  │
│   7. Run parallel validation (both systems)                                             │
│   8. Cutover (DNS switch)                                                               │
│   9. Decommission pilot server                                                          │
│                                                                                          │
│   DATA MIGRATION COMMANDS:                                                              │
│   # Export from pilot                                                                   │
│   pg_dump -h pilot-server -U hdim hdim_db > hdim_backup.sql                            │
│                                                                                          │
│   # Import to production                                                                │
│   psql -h cluster-primary -U hdim hdim_db < hdim_backup.sql                            │
│                                                                                          │
│   # Sync Redis cache (optional - will rebuild automatically)                            │
│   redis-cli --rdb dump.rdb                                                              │
│                                                                                          │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

**Timeline**: 2-4 weeks
**Downtime**: <1 hour (DNS cutover)
**Risk Level**: Low

---

### Path 2: Cloud → On-Premise Migration

**Scenario**: Started in cloud, now need to move PHI on-premise due to data residency requirements.

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                         CLOUD TO ON-PREMISE MIGRATION                                    │
│                                                                                          │
│   MONTH 1: PARALLEL                   MONTH 2: MIGRATE PHI         MONTH 3: DECOMMISSION│
│                                                                                          │
│   ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│   │                                  CLOUD                                           │   │
│   │   ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐                              │   │
│   │   │ Gateway │ │  FHIR   │ │ Quality │ │   CQL   │                              │   │
│   │   │         │ │         │ │ Measure │ │ Engine  │                              │   │
│   │   └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘                              │   │
│   │        │           │           │           │                                    │   │
│   │        └───────────┴───────────┴───────────┘                                    │   │
│   │                         │                                                        │   │
│   │   ┌─────────────────────┴─────────────────────┐                                │   │
│   │   │              PostgreSQL (RDS)              │                                │   │
│   │   │              (All PHI data)                │────▶ Export                   │   │
│   │   └───────────────────────────────────────────┘                                │   │
│   └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                              │                                           │
│                                    Encrypted │ Transfer                                  │
│                                              │ (pgdump over VPN)                        │
│                                              ▼                                           │
│   ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│   │                               ON-PREMISE                                         │   │
│   │                                                                                  │   │
│   │   ┌─────────┐ ┌─────────┐                                                       │   │
│   │   │ Gateway │ │  FHIR   │  ◀── Deploy identical containers                     │   │
│   │   │ (New)   │ │ (New)   │                                                       │   │
│   │   └────┬────┘ └────┬────┘                                                       │   │
│   │        │           │                                                             │   │
│   │   ┌────┴───────────┴────┐                                                       │   │
│   │   │   PostgreSQL        │ ◀── Import PHI data                                   │   │
│   │   │   (On-Premise)      │     (All PHI now on-prem)                            │   │
│   │   └─────────────────────┘                                                       │   │
│   │                                                                                  │   │
│   │   Quality Measure + CQL Engine: KEEP IN CLOUD (Hybrid)                         │   │
│   │   OR                                                                            │   │
│   │   Move everything on-prem (Full Migration)                                      │   │
│   └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
│   MIGRATION CHECKLIST:                                                                   │
│   □ Provision on-premise infrastructure                                                 │
│   □ Configure VPN/Direct Connect to cloud                                               │
│   □ Deploy HDIM services on-premise (identical versions)                                │
│   □ Set up PostgreSQL replication (cloud → on-prem)                                    │
│   □ Validate data synchronization                                                       │
│   □ Update Gateway to route FHIR requests to on-prem                                   │
│   □ Monitor for 1 week in parallel                                                      │
│   □ Cut over DNS to on-prem gateway                                                    │
│   □ Verify data residency compliance                                                   │
│   □ Decommission cloud database (securely delete PHI)                                  │
│                                                                                          │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

**Timeline**: 2-3 months
**Downtime**: <4 hours (during final cutover)
**Risk Level**: Medium (data transfer complexity)

---

### Path 3: Service-by-Service Activation

**Scenario**: Start with minimal services, add capabilities as needs grow.

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                       SERVICE-BY-SERVICE ACTIVATION PATH                                 │
│                                                                                          │
│   QUARTER 1                QUARTER 2                QUARTER 3                QUARTER 4   │
│   ───────────              ───────────              ───────────              ───────────  │
│                                                                                          │
│   BASELINE                 + CARE GAPS              + ANALYTICS              + ADVANCED   │
│   ┌─────────────┐          ┌─────────────┐          ┌─────────────┐          ┌─────────┐ │
│   │ Gateway     │          │ Care Gap    │          │ Analytics   │          │Predictive│ │
│   │ FHIR        │          │ Service     │          │ Service     │          │Analytics │ │
│   │ Patient     │          │             │          │             │          │          │ │
│   │ Quality     │          │ ★ Gap       │          │ ★ Dashboards│          │ ★ ML     │ │
│   │ Measure     │          │   Closure   │          │ ★ Reports   │          │   Models │ │
│   │ CQL Engine  │          │   Workflows │          │ ★ Exports   │          │ ★ Risk   │ │
│   └─────────────┘          └─────────────┘          └─────────────┘          │   Scores │ │
│                                                                               └─────────┘ │
│   Capability:              Capability:              Capability:              Capability:  │
│   - HEDIS measures         - Care gap alerts        - Executive reports      - Risk      │
│   - Patient evaluation     - Worklist management    - Trend analysis         stratifica- │
│   - Basic reporting        - Intervention tracking  - QRDA export            tion        │
│                                                                               - HCC coding│
│   Cost: $3K/mo             + $1K/mo = $4K/mo       + $1K/mo = $5K/mo       + $2K/mo=$7K │
│                                                                                          │
│   ACTIVATION PROCESS (per service):                                                      │
│   1. Update docker-compose.yml to include new service                                   │
│   2. Run database migrations for new service                                            │
│   3. Configure service in application.yml                                               │
│   4. Restart affected services                                                          │
│   5. Validate integration with existing services                                        │
│   6. Train users on new capabilities                                                    │
│                                                                                          │
│   EXAMPLE: Adding Care Gap Service                                                       │
│   ─────────────────────────────────                                                     │
│   # docker-compose.yml addition                                                          │
│   services:                                                                              │
│     care-gap:                                                                            │
│       image: hdim/care-gap-service:latest                                               │
│       environment:                                                                       │
│         - SPRING_PROFILES_ACTIVE=production                                             │
│         - DATABASE_URL=jdbc:postgresql://db:5432/hdim                                   │
│       depends_on:                                                                        │
│         - quality-measure                                                                │
│         - fhir                                                                           │
│                                                                                          │
│   # Apply                                                                                │
│   docker compose up -d care-gap                                                          │
│   docker compose exec care-gap ./migrate.sh                                             │
│                                                                                          │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

**Timeline**: 4+ quarters (gradual)
**Downtime**: 0 (rolling service additions)
**Risk Level**: Very Low

---

## Total Cost of Ownership (TCO)

### 3-Year TCO Calculator

**Formula:**
```
TCO = Infrastructure + Personnel + Licensing + Maintenance + Opportunity Cost
```

### TCO Comparison: 500K Patient Organization

| Cost Category | Single-Node | Clustered | Kubernetes | SaaS |
|---------------|-------------|-----------|------------|------|
| **Year 1** | | | | |
| Infrastructure | $6,000 | $48,000 | $120,000 | $0 |
| Personnel (FTE) | $20,000 | $75,000 | $150,000 | $0 |
| Setup/Migration | $5,000 | $15,000 | $40,000 | $5,000 |
| Licensing | $0 | $0 | $0 | $72,000 |
| **Year 1 Total** | **$31,000** | **$138,000** | **$310,000** | **$77,000** |
| | | | | |
| **Year 2** | | | | |
| Infrastructure | $6,000 | $48,000 | $120,000 | $0 |
| Personnel | $20,000 | $75,000 | $150,000 | $0 |
| Maintenance | $3,000 | $10,000 | $25,000 | $0 |
| Licensing | $0 | $0 | $0 | $72,000 |
| **Year 2 Total** | **$29,000** | **$133,000** | **$295,000** | **$72,000** |
| | | | | |
| **Year 3** | | | | |
| Infrastructure | $6,000 | $48,000 | $120,000 | $0 |
| Personnel | $20,000 | $75,000 | $150,000 | $0 |
| Maintenance | $3,000 | $10,000 | $25,000 | $0 |
| Licensing | $0 | $0 | $0 | $72,000 |
| **Year 3 Total** | **$29,000** | **$133,000** | **$295,000** | **$72,000** |
| | | | | |
| **3-YEAR TCO** | **$89,000** | **$404,000** | **$900,000** | **$221,000** |

### TCO Insights

**Single-Node**: Lowest cost but not suitable for production
- Best for: Pilots, POCs, small organizations
- Hidden cost: Risk of downtime (no HA)

**Clustered**: Best balance for production
- Best for: Medium-large organizations with IT staff
- Hidden cost: Personnel time for operations

**Kubernetes**: Highest control, highest cost
- Best for: Enterprise with existing K8s, multi-tenant platforms
- Hidden cost: Learning curve, complexity

**SaaS**: Predictable costs, fastest time-to-value
- Best for: Organizations wanting to focus on clinical outcomes
- Hidden cost: Less customization control

### Build vs Buy Decision Matrix

| Factor | Build (Self-Hosted) | Buy (SaaS) |
|--------|---------------------|------------|
| **Upfront Investment** | High | Low |
| **Monthly Operating Cost** | Variable | Fixed |
| **Time to Value** | Weeks-Months | Days |
| **Control** | Full | Limited |
| **Customization** | Unlimited | Configuration only |
| **Compliance Control** | Full | Shared responsibility |
| **Vendor Dependency** | Low | High |
| **IT Staff Required** | 2-6 FTEs | 0-1 FTE |
| **Best For** | Large enterprises, data residency | SMB, rapid deployment |

---

## Industry Terminology Reference

### Container & Orchestration Terms

| Term | Definition | HDIM Usage |
|------|------------|------------|
| **Docker** | Container runtime | All HDIM services run in Docker containers |
| **Docker Compose** | Multi-container orchestration | Single-node and clustered deployments |
| **Kubernetes (K8s)** | Container orchestration platform | Enterprise deployments |
| **Helm** | K8s package manager | HDIM Helm charts for K8s deployment |
| **StatefulSet** | K8s workload for stateful apps | PostgreSQL, Redis in K8s |
| **HPA** | Horizontal Pod Autoscaler | Auto-scale CQL Engine based on CPU |
| **PVC** | Persistent Volume Claim | Database storage in K8s |
| **Ingress** | K8s HTTP routing | Kong or NGINX ingress controller |

### Database Terms

| Term | Definition | HDIM Usage |
|------|------------|------------|
| **PostgreSQL** | Relational database | Primary data store (multi-tenant) |
| **Streaming Replication** | Real-time database sync | HA and read scaling |
| **Point-in-Time Recovery** | Restore to specific moment | Disaster recovery |
| **Connection Pooling** | Efficient DB connections | PgBouncer for connection management |
| **Multi-Tenant Schema** | Isolated tenant data | Separate schema per tenant |

### Cache & Messaging Terms

| Term | Definition | HDIM Usage |
|------|------------|------------|
| **Redis** | In-memory data store | PHI caching (5min TTL for HIPAA) |
| **Sentinel** | Redis HA manager | Automatic Redis failover |
| **Kafka** | Event streaming platform | Audit events, integration messages |
| **Consumer Group** | Kafka parallel processing | Multiple services consuming events |

### Networking Terms

| Term | Definition | HDIM Usage |
|------|------------|------------|
| **Load Balancer** | Traffic distribution | HAProxy, NGINX, cloud ALB |
| **TLS 1.3** | Transport encryption | All external connections |
| **mTLS** | Mutual TLS authentication | Service-to-service in K8s (Istio) |
| **VPN** | Virtual Private Network | Cloud-to-on-prem connectivity |
| **Direct Connect** | Dedicated cloud link | Low-latency hybrid deployments |

### Observability Terms

| Term | Definition | HDIM Usage |
|------|------------|------------|
| **Prometheus** | Metrics collection | Service metrics, alerting |
| **Grafana** | Visualization platform | Dashboards, operational views |
| **ELK Stack** | Log aggregation | Elasticsearch, Logstash, Kibana |
| **Velero** | K8s backup tool | Cluster backup and disaster recovery |

---

## Implementation Validation Runbook

Use the shared validation runbook for both on-prem and cloud-hosted deployments. It enforces tenant hardblock behavior, UUID-only FHIR data, and core service health checks.

Path: `docs/deployment/IMPLEMENTATION_VALIDATION_RUNBOOK.md`
Quick run: `bash ./scripts/validate-deployment.sh`

## Decision Framework

### Quick Decision Path

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              DEPLOYMENT DECISION TREE                                    │
│                                                                                          │
│   START                                                                                  │
│     │                                                                                    │
│     ├─▶ Is this a pilot/POC? ─────────────────────────────────────▶ YES ─▶ SINGLE-NODE  │
│     │                                                                                    │
│     ├─▶ Do you have data residency requirements? ──────────────────▶ YES ─▶ ON-PREMISE  │
│     │                                               │                      or HYBRID    │
│     │                                               └─▶ NO ─┐                           │
│     │                                                       │                           │
│     ├─▶ Do you want minimal IT overhead? ──────────────────────────▶ YES ─▶ SAAS       │
│     │                                               │                                    │
│     │                                               └─▶ NO ─┐                           │
│     │                                                       │                           │
│     ├─▶ Do you have >500K patients? ───────────────────────────────▶ YES ─▶ KUBERNETES │
│     │                                               │                      or HYBRID    │
│     │                                               └─▶ NO ─┐                           │
│     │                                                       │                           │
│     ├─▶ Do you have existing Kubernetes infrastructure? ───▶ YES ─▶ KUBERNETES         │
│     │                                               │                                    │
│     │                                               └─▶ NO ─┐                           │
│     │                                                       │                           │
│     └─▶ DEFAULT RECOMMENDATION ────────────────────────────────────▶ CLUSTERED         │
│                                                                                          │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

### Detailed Assessment Checklist

**Complete this checklist to determine your optimal deployment model:**

**A. Scale Requirements**
- [ ] Current patient population: ____________
- [ ] Projected 3-year patient population: ____________
- [ ] Number of concurrent clinical users: ____________
- [ ] Number of measures to evaluate: ____________

**B. Availability Requirements**
- [ ] Acceptable downtime per month: ____________
- [ ] RTO (Recovery Time Objective): ____________
- [ ] RPO (Recovery Point Objective): ____________
- [ ] Multi-region required: Yes / No

**C. Infrastructure Constraints**
- [ ] Data residency requirements: Yes / No / Uncertain
- [ ] Existing on-premise infrastructure: Yes / No
- [ ] Existing Kubernetes cluster: Yes / No
- [ ] Cloud subscriptions available: AWS / Azure / GCP / None

**D. Team Capabilities**
- [ ] IT team size: ____________
- [ ] DevOps experience level: None / Basic / Advanced
- [ ] Kubernetes experience: None / Basic / Advanced
- [ ] Cloud experience: None / Basic / Advanced

**E. Budget & Timeline**
- [ ] Budget for Year 1: ____________
- [ ] Ongoing monthly budget: ____________
- [ ] CapEx vs OpEx preference: ____________
- [ ] Time to first deployment: ____________

**F. Compliance & Licensing**
- [ ] License compliance verified (docs/compliance/THIRD_PARTY_NOTICES.md)

**Scoring:**
- Mostly A answers (high scale): Kubernetes or Hybrid
- Strong B requirements (high availability): Clustered, Kubernetes, or SaaS
- C constraints (data residency): On-Premise or Hybrid
- Limited D capabilities (small team): SaaS or Single-Node
- E constraints (tight budget): Single-Node or SaaS

---

## Appendix: Deployment Commands

### Single-Node Quick Start

```bash
# Clone and configure
git clone https://github.com/healthdata-im/hdim-deploy.git
cd hdim-deploy/single-node
cp .env.example .env
nano .env  # Configure your settings

# Start services
docker compose up -d

# Verify health
docker compose ps
curl http://localhost:8001/health

# View logs
docker compose logs -f

# Stop services
docker compose down
```

### Clustered Deployment

```bash
# On each application server
git clone https://github.com/healthdata-im/hdim-deploy.git
cd hdim-deploy/clustered
cp .env.example .env
nano .env  # Configure with LB address, DB address

# Start services
docker compose up -d

# Configure load balancer
# (HAProxy, NGINX, or cloud LB - see docs/lb-config.md)
```

### Kubernetes Deployment

```bash
# Add HDIM Helm repository
helm repo add hdim https://charts.hdim.io
helm repo update

# Install with custom values
helm install hdim hdim/hdim \
  --namespace hdim \
  --create-namespace \
  -f values.yaml

# Check deployment
kubectl get pods -n hdim
kubectl get svc -n hdim

# View service logs
kubectl logs -f deployment/quality-measure-service -n hdim
```

---

## Contact & Support

**Technical Questions**: solutions@hdim.io
**Sales Inquiries**: sales@hdim.io
**Support Portal**: support.hdim.io

**Documentation Updates**: This guide is updated quarterly. Check https://docs.hdim.io for the latest version.

---

*Document Version: 2.0*
*Last Updated: January 2026*
*Classification: Public*
