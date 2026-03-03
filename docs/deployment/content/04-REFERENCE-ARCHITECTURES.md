# HDIM Reference Architectures

Detailed architectural diagrams and specifications for each HDIM deployment model.

---

## Architecture 1: Single-Node On-Premise

**Use Case**: Pilots, small practices, development

```
┌─────────────────────────────────────────────────────────────────┐
│                        SINGLE SERVER                             │
│               (Ubuntu/CentOS, 4+ CPU, 16GB RAM)                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Docker Engine                               │   │
│  ├──────────────────────────────────────────────────────────┤   │
│  │                                                           │   │
│  │  ┌────────────────┐  ┌────────────────┐                │   │
│  │  │  Kong Gateway  │  │   HDIM Gateway │                │   │
│  │  │   (8000)       │→ │   (8001)       │                │   │
│  │  └────────────────┘  └────────────────┘                │   │
│  │         ↑                    ↓                          │   │
│  │    ┌────────────┐  ┌─────────────────────────┐         │   │
│  │    │   TLS      │  │  Service Router         │         │   │
│  │    │ Cert       │  │  ├─ Quality Measure 8087│         │   │
│  │    │ Management │  │  ├─ CQL Engine 8081    │         │   │
│  │    └────────────┘  │  ├─ Care Gap 8086      │         │   │
│  │                    │  ├─ Risk Adj 8088      │         │   │
│  │                    │  ├─ Patient 8084       │         │   │
│  │                    │  ├─ FHIR 8085          │         │   │
│  │                    │  └─ Other Services     │         │   │
│  │                    └─────────────────────────┘         │   │
│  │                            ↓                           │   │
│  │                    ┌─────────────────┐                │   │
│  │                    │   PostgreSQL    │                │   │
│  │                    │   (5435)        │                │   │
│  │                    │ Local Database  │                │   │
│  │                    └─────────────────┘                │   │
│  │                            ↓                           │   │
│  │                    ┌─────────────────┐                │   │
│  │                    │  Redis Cache    │                │   │
│  │                    │  (6380)         │                │   │
│  │                    └─────────────────┘                │   │
│  │                                                           │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Storage (500GB+ SSD)                                    │   │
│  │  ├─ PostgreSQL data files                               │   │
│  │  ├─ Logs and audit trails                               │   │
│  │  └─ Backup snapshots                                    │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
        ↑                                   ↓
     Clinical Portal              Your FHIR Server
     (Web Browser)                (Epic, Cerner, etc.)
```

**Specifications:**

```
Server Specs:
├─ CPU: 4 cores (minimum), 8 cores (recommended)
├─ RAM: 16GB (minimum), 32GB (recommended for growth)
├─ Storage: 500GB SSD (minimum), 1TB (recommended)
├─ Network: 100 Mbps (minimum), 1 Gbps (recommended)
├─ OS: Ubuntu 20.04+ LTS or CentOS 8+
└─ Availability: Single point of failure

Service Distribution:
├─ All services in single Docker Compose (26 containers)
├─ All services share same Docker network
├─ PostgreSQL: Local database on same host
├─ Redis: Local cache on same host
├─ Kafka: Single node (not recommended for production)
└─ Monitoring: Optional (Prometheus, Grafana)

Networking:
├─ Kong gateway listens on port 8000 (external)
├─ All services on internal Docker network
├─ No internet required (but helpful for setup)
└─ Firewall: Only port 8000 exposed externally

Data Storage:
├─ PostgreSQL: /var/lib/postgresql/data
├─ Redis: In-memory only (no persistence needed)
├─ Backups: Manual or cron-based snapshots
├─ Retention: 7-day rolling backups
└─ Encryption: Database-level SSL

Monitoring:
├─ Docker logs: docker logs <container>
├─ Health checks: Built-in, automated
├─ Alerts: Email or Slack (optional setup)
└─ Dashboards: Basic CLI or web-based
```

**Deployment Instructions:**

```bash
# 1. Provision server
# Ubuntu 20.04 LTS with 4 CPU, 16GB RAM, 500GB SSD

# 2. Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER
newgrp docker

# 3. Clone HDIM deployment
git clone <hdim-repo> hdim-deployment
cd hdim-deployment

# 4. Configure environment
cp .env.example .env
# Edit .env with your settings:
# - FHIR_SERVER_URL
# - AUTH_PROVIDER_URL
# - DOCKER_COMPOSE_PROFILE=full

# 5. Start services
docker-compose up -d

# 6. Verify services
docker-compose ps
curl http://localhost:8000/health

# 7. Configure integrations
# See: 02-INTEGRATION-PATTERNS.md
```

**Pros & Cons:**

✅ Pros:
- Simplest deployment (hours)
- Lowest cost
- Easy to manage
- Good for learning/testing
- Easy to backup and restore

❌ Cons:
- No high availability
- Single point of failure
- Manual backup/recovery
- Limited scalability
- Not suitable for production critical systems

---

## Architecture 2: Clustered On-Premise

**Use Case**: Production deployments, medium organizations

```
┌──────────────────────────────────────────────────────────────────┐
│                    NETWORK LOAD BALANCER                          │
│              (Hardware or Software, Port 8000)                    │
│            (Distributes traffic, Health checks)                   │
├──────────────────────────────────────────────────────────────────┤
│        ↓                 ↓                  ↓                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐               │
│  │  Server 1   │  │  Server 2   │  │  Server 3   │  ...          │
│  │ (4CPU,16GB) │  │ (4CPU,16GB) │  │ (4CPU,16GB) │               │
│  │             │  │             │  │             │               │
│  │ Docker +    │  │ Docker +    │  │ Docker +    │               │
│  │ HDIM        │  │ HDIM        │  │ HDIM        │               │
│  │ Services    │  │ Services    │  │ Services    │               │
│  │ (partial)   │  │ (partial)   │  │ (partial)   │               │
│  │             │  │             │  │             │               │
│  └─────────────┘  └─────────────┘  └─────────────┘               │
│        ↑                 ↑                  ↑                      │
│        └─────────────────┼──────────────────┘                     │
│                          ↓                                        │
│         ┌─────────────────────────────────┐                      │
│         │   PostgreSQL (Cluster)          │                      │
│         │   ├─ Primary server             │                      │
│         │   ├─ Replica servers (2+)       │                      │
│         │   ├─ Streaming replication      │                      │
│         │   └─ Automatic failover         │                      │
│         └─────────────────────────────────┘                      │
│                                                                    │
│         ┌─────────────────────────────────┐                      │
│         │   Redis Cluster                 │                      │
│         │   ├─ Multiple nodes              │                      │
│         │   ├─ High availability           │                      │
│         │   ├─ Persistence enabled        │                      │
│         │   └─ Sentinel for failover      │                      │
│         └─────────────────────────────────┘                      │
│                                                                    │
│         ┌─────────────────────────────────┐                      │
│         │   Kafka Cluster                 │                      │
│         │   ├─ 3+ brokers                  │                      │
│         │   ├─ Replication factor 3        │                      │
│         │   └─ Distributed partitions      │                      │
│         └─────────────────────────────────┘                      │
│                                                                    │
│         ┌─────────────────────────────────┐                      │
│         │   Shared Storage (NFS/SAN)      │                      │
│         │   ├─ Backup repository          │                      │
│         │   ├─ Shared configs             │                      │
│         │   └─ Audit log archive          │                      │
│         └─────────────────────────────────┘                      │
│                                                                    │
└──────────────────────────────────────────────────────────────────┘
        ↑                                   ↓
     Clinical Portal              Your FHIR Server
```

**Specifications:**

```
Infrastructure:
├─ Load Balancer (Hardware or Software)
│  ├─ Port 8000 (external)
│  ├─ Health checks every 10 seconds
│  ├─ Failover time: < 5 seconds
│  └─ Support for SSL/TLS termination
│
├─ Application Servers (3-5 servers)
│  ├─ Each: 4 CPU, 16GB RAM, 500GB SSD
│  ├─ Docker Compose with partial service list
│  ├─ High-memory services (CQL) get more RAM
│  └─ Services distributed across servers
│     ├─ Server 1: Kong, Gateway, Quality Measure
│     ├─ Server 2: CQL Engine, Care Gap
│     ├─ Server 3: Patient, Risk Adjustment
│     ├─ Server 4: FHIR, Supporting Services
│     └─ Server 5: Additional capacity (optional)
│
├─ Database Infrastructure
│  ├─ PostgreSQL Cluster (1 primary + 2+ replicas)
│  │  ├─ Each: 8+ CPU, 32GB+ RAM, 1TB+ SSD
│  │  ├─ Streaming replication
│  │  ├─ Automated failover (pg_auto_failover)
│  │  └─ Backups to NAS/SAN
│  │
│  ├─ Redis Cluster (3+ nodes)
│  │  ├─ Each: 4 CPU, 8GB RAM, 100GB SSD
│  │  ├─ RDB snapshots to disk
│  │  ├─ Persistence enabled
│  │  └─ Sentinel for automatic failover
│  │
│  └─ Kafka Cluster (3+ brokers)
│     ├─ Each: 4 CPU, 8GB RAM, 200GB SSD
│     ├─ Replication factor: 3
│     ├─ Distributed partitions (12+)
│     └─ Retention: 7 days

├─ Network Infrastructure
│  ├─ Layer 3 switch
│  ├─ VLANs for segmentation
│  ├─ Firewall rules (ingress/egress)
│  ├─ Jumbo frames (9000 MTU) for performance
│  └─ Network monitoring

├─ Storage Infrastructure
│  ├─ NFS or SAN for shared storage
│  ├─ Capacity: 5TB+ for backups
│  ├─ RAID 6 for redundancy
│  ├─ Automated backup copies
│  └─ Offsite backup replication

└─ Total Cost: ~$3-5K/month
```

**Service Distribution Example:**

```
Server 1 (Kong, Gateway, Load Balancing):
├─ Kong API Gateway (8000)
├─ HDIM Gateway Service (8001)
├─ Monitoring agent
└─ 2 CPU dedicated to these high-touch services

Server 2 (CPU-Intensive Services):
├─ CQL Engine Service (8081)
├─ Quality Measure Service (8087)
├─ 4 CPU cores allocated
└─ 8GB dedicated to JVM heap

Server 3 (Data Services):
├─ Patient Service (8084)
├─ FHIR Service (8085)
├─ Care Gap Service (8086)
└─ Monitoring agent

Server 4 (Additional Services):
├─ Risk Adjustment Service (8088)
├─ QRDA Export Service (8100)
├─ Supporting services
└─ Monitoring agent

Server 5 (Capacity):
├─ Failover for any service
├─ Batch jobs
└─ Development/testing

Database Servers (Separate):
├─ PostgreSQL Primary
├─ PostgreSQL Replica 1
├─ PostgreSQL Replica 2
├─ Redis Primary
├─ Redis Replica (Sentinel)
├─ Kafka Broker 1
├─ Kafka Broker 2
├─ Kafka Broker 3
└─ NAS/SAN for backups
```

**High Availability Features:**

```
Load Balancer:
├─ Active-Active or Active-Passive failover
├─ Health checks on all backend servers
├─ Connection draining on maintenance
└─ Sticky sessions for state management

Database:
├─ PostgreSQL streaming replication
├─ Automated failover via pg_auto_failover
├─ Point-in-time recovery (PITR)
├─ Off-site backups
└─ RPO: < 1 minute

Redis:
├─ Master-Slave replication
├─ Sentinel for automatic failover
├─ Persistence (RDB snapshots)
└─ Data backup

Services:
├─ Health checks (every 10 seconds)
├─ Automatic restart on failure
├─ Circuit breaker patterns
├─ Request timeouts and retries
└─ Graceful degradation

Monitoring:
├─ CPU, memory, disk usage alerts
├─ Network latency monitoring
├─ Database replication lag tracking
├─ Service availability checks
└─ Automated alerting (Slack, email, PagerDuty)
```

---

## Architecture 3: Kubernetes On-Premise

**Use Case**: Enterprise, large scale, multi-tenant

```
┌──────────────────────────────────────────────────────────────────┐
│                   KUBERNETES CLUSTER                              │
│                (3 Control Planes + 5-10 Workers)                 │
├──────────────────────────────────────────────────────────────────┤
│                                                                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │           INGRESS CONTROLLER (NGINX/HAProxy)             │   │
│  │  (External API endpoint, TLS termination, routing)       │   │
│  └──────────────────┬───────────────────────────────────────┘   │
│                     │ Route to services by path                  │
│  ┌──────────────────┴────────────────────────────────────────┐  │
│  │            KUBERNETES SERVICES (ClusterIP)                │  │
│  │  (Internal load balancing, DNS resolution)               │  │
│  │                                                            │  │
│  │  ├─ quality-measure-service (internal LB)               │  │
│  │  ├─ cql-engine-service (internal LB)                    │  │
│  │  ├─ care-gap-service (internal LB)                      │  │
│  │  ├─ fhir-service (internal LB)                          │  │
│  │  ├─ patient-service (internal LB)                       │  │
│  │  ├─ hdim-gateway-service (internal LB)                  │  │
│  │  └─ ... (other services)                                │  │
│  │                                                            │  │
│  └────────────────┬─────────────────────────────────────────┘  │
│                   │ Routes to pods                               │
│  ┌────────────────┴──────────────────────────────────────────┐  │
│  │         KUBERNETES PODS (Containers)                     │  │
│  │                                                            │  │
│  │  Pod Group 1: Quality Measure Service                   │  │
│  │  ├─ Replica 1 (on Worker 1)                              │  │
│  │  ├─ Replica 2 (on Worker 2)                              │  │
│  │  ├─ Replica 3 (on Worker 3)                              │  │
│  │  └─ HPA: Scale 2-10 based on CPU                         │  │
│  │                                                            │  │
│  │  Pod Group 2: CQL Engine Service                         │  │
│  │  ├─ Replica 1 (on Worker 2)                              │  │
│  │  ├─ Replica 2 (on Worker 3)                              │  │
│  │  ├─ Replica 3 (on Worker 4)                              │  │
│  │  └─ HPA: Scale 1-20 based on memory                      │  │
│  │                                                            │  │
│  │  ... (other service pod groups)                          │  │
│  │                                                            │  │
│  └────────────────┬──────────────────────────────────────────┘  │
│                   │                                               │
│  ┌────────────────┴──────────────────────────────────────────┐  │
│  │    PERSISTENT VOLUME (Kubernetes Storage)               │  │
│  │                                                            │  │
│  │  ├─ PostgreSQL StatefulSet                               │  │
│  │  │  ├─ Persistent Volume: 500GB                         │  │
│  │  │  ├─ Replicas: 3 (primary + 2 standby)               │  │
│  │  │  ├─ Services: PostgreSQL headless service            │  │
│  │  │  └─ Backup: Daily snapshots                          │  │
│  │  │                                                       │  │
│  │  ├─ Redis StatefulSet                                   │  │
│  │  │  ├─ Persistent Volume: 100GB                        │  │
│  │  │  ├─ Replicas: 3 (cluster mode)                      │  │
│  │  │  ├─ Services: Redis headless service                │  │
│  │  │  └─ Persistence: RDB snapshots                      │  │
│  │  │                                                       │  │
│  │  └─ Kafka StatefulSet                                   │  │
│  │     ├─ Persistent Volume: 200GB each                   │  │
│  │     ├─ Brokers: 3 replicas                             │  │
│  │     ├─ Services: Kafka headless service                │  │
│  │     └─ Replication: Factor 3                           │  │
│  │                                                            │  │
│  └────────────────┬──────────────────────────────────────────┘  │
│                   │ Attached to pods                             │
│  ┌────────────────┴──────────────────────────────────────────┐  │
│  │        STORAGE CLASS (Block or Network)                  │  │
│  │  (NFS, iSCSI, vSphere, etc.)                             │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │         MONITORING & LOGGING                              │   │
│  │  ├─ Prometheus (Metrics collection)                      │   │
│  │  ├─ Grafana (Dashboards)                                │   │
│  │  ├─ ELK Stack (Log aggregation)                         │   │
│  │  └─ Alerts (PagerDuty, Slack)                           │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │         NETWORK POLICIES & SECURITY                       │   │
│  │  ├─ Pod-to-pod: Only allowed connections                │   │
│  │  ├─ Ingress: TLS required                               │   │
│  │  ├─ Egress: Restricted outbound                         │   │
│  │  ├─ Network Policies: Tenant isolation                  │   │
│  │  └─ Service Accounts: RBAC enabled                      │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │         SECRETS & CONFIGURATION                           │   │
│  │  ├─ ConfigMaps: Non-sensitive config                    │   │
│  │  ├─ Secrets: Database passwords, API keys               │   │
│  │  ├─ Vault integration: Secret management                │   │
│  │  └─ Encrypted etcd: Kubernetes data store               │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │         KUBERNETES CONTROL PLANE                          │   │
│  │  ├─ API Server (3 replicas for HA)                      │   │
│  │  ├─ etcd (3 replicas, encrypted)                        │   │
│  │  ├─ Scheduler                                           │   │
│  │  └─ Controller Manager                                  │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                    │
└──────────────────────────────────────────────────────────────────┘
        ↑                                   ↓
     Clinical Portal              Your FHIR Server
```

**Specifications:**

```
Kubernetes Cluster:
├─ Control Plane:
│  ├─ 3 nodes (master nodes)
│  ├─ Each: 4 CPU, 8GB RAM, 100GB SSD
│  ├─ etcd: Encrypted, persistent storage
│  ├─ API server: TLS enabled
│  └─ High availability: Yes (3-way replication)
│
├─ Worker Nodes:
│  ├─ 5-10 nodes (scale based on workload)
│  ├─ Each: 4-8 CPU, 16-32GB RAM, 500GB SSD
│  ├─ Distribution: Spread across availability zones
│  ├─ Taints/labels: Workload-specific scheduling
│  └─ Auto-scaling: Up to 30+ nodes possible
│
├─ Networking:
│  ├─ Container network: Calico or Flannel
│  ├─ Service mesh: Optional (Istio, Linkerd)
│  ├─ Ingress controller: NGINX or HAProxy
│  ├─ Service load balancer: MetalLB (on-prem)
│  ├─ Network policies: Enabled for security
│  └─ Bandwidth: Gigabit or better
│
├─ Storage:
│  ├─ Persistent volumes: 2TB+ total
│  ├─ Storage class: Dynamic provisioning
│  ├─ Backend: NFS, iSCSI, or block storage
│  ├─ Replication: Yes (for HA)
│  └─ Backup: Automated snapshots
│
├─ Monitoring:
│  ├─ Prometheus: Metrics collection
│  ├─ Grafana: Dashboards
│  ├─ AlertManager: Alert routing
│  ├─ Logging: ELK or Fluentd
│  └─ Tracing: Jaeger (optional)
│
└─ Total Cost: $5-15K/month (depends on cloud provider)

Pod Specifications:
├─ Quality Measure Service
│  ├─ CPU: 100m - 500m
│  ├─ Memory: 512Mi - 2Gi
│  ├─ Replicas: 2-10 (HPA enabled)
│  ├─ Update strategy: Rolling
│  └─ Resource requests: Guaranteed QoS
│
├─ CQL Engine Service
│  ├─ CPU: 200m - 1000m
│  ├─ Memory: 1Gi - 4Gi (high memory due to JVM)
│  ├─ Replicas: 1-20 (HPA enabled)
│  ├─ Update strategy: Rolling
│  └─ Resource requests: Guaranteed QoS
│
└─ ... (other services with similar specs)

Horizontal Pod Autoscaling (HPA):
├─ Quality Measure Service:
│  ├─ Min replicas: 2
│  ├─ Max replicas: 10
│  ├─ Metric: CPU (70% threshold)
│  └─ Scale up: < 30 seconds
│
├─ CQL Engine Service:
│  ├─ Min replicas: 1
│  ├─ Max replicas: 20
│  ├─ Metric: Memory (80% threshold)
│  └─ Scale up: < 60 seconds
│
└─ Other services: Similar configurations
```

**Deployment Manifests (Example):**

```yaml
# Deployment: Quality Measure Service
apiVersion: apps/v1
kind: Deployment
metadata:
  name: quality-measure-service
  namespace: hdim
spec:
  replicas: 2
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: quality-measure-service
  template:
    metadata:
      labels:
        app: quality-measure-service
    spec:
      serviceAccountName: quality-measure-sa
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
      containers:
      - name: quality-measure-service
        image: hdim/quality-measure-service:1.0.0
        ports:
        - containerPort: 8087
        env:
        - name: JAVA_OPTS
          value: "-Xmx1g -Xms512m"
        - name: FHIR_SERVER_URL
          valueFrom:
            configMapKeyRef:
              name: hdim-config
              key: fhir.server.url
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: password
        resources:
          requests:
            cpu: 100m
            memory: 512Mi
          limits:
            cpu: 500m
            memory: 2Gi
        livenessProbe:
          httpGet:
            path: /health/live
            port: 8087
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /health/ready
            port: 8087
          initialDelaySeconds: 20
          periodSeconds: 5

---
# Horizontal Pod Autoscaler
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: quality-measure-hpa
  namespace: hdim
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: quality-measure-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

**Auto-Scaling Behavior:**

```
Normal Load:
├─ 2-3 replicas active
├─ 1-2 standby (for quick scale up)
└─ 95% CPU/memory usage: Low

High Load (e.g., batch measure evaluation):
├─ Detects CPU > 70%
├─ Adds 1 new pod per 30 seconds
├─ Reaches 5-10 pods in 2-3 minutes
├─ 70% CPU/memory usage: Maintained
└─ Requests queued but not dropped

Scale Down (after peak):
├─ Detects CPU < 50% for 5 minutes
├─ Removes 1 pod per 30 seconds
├─ Returns to 2-3 replicas
└─ Graceful termination (connection draining)
```

---

## Architecture 4: Hybrid Cloud

**Use Case**: Organizations with multi-region requirements

```
On-Premise Data Center
┌──────────────────────────────────────────┐
│     HDIM Gateway Layer (On-Prem)         │
│  (Kong + Gateway Service + Auth)         │
│                                          │
│  ├─ Kong API Gateway (8000)             │
│  │  └─ TLS termination                  │
│  │  └─ Auth validation                  │
│  │                                      │
│  ├─ HDIM Gateway Service (8001)         │
│  │  └─ Request routing                  │
│  │  └─ Circuit breaking                 │
│  │                                      │
│  └─ PostgreSQL (Local)                  │
│     └─ On-prem audit logs               │
│                                          │
└──────────────────────────────────────────┘
          │ HTTPS/VPN Tunnel
          │ (Encrypted, secure)
          ↓
Cloud Provider (AWS/Azure/GCP)
┌──────────────────────────────────────────┐
│  HDIM Core Services Layer (Cloud)        │
│  (Kubernetes Cluster)                    │
│                                          │
│  ├─ Quality Measure Service (auto-scale)│
│  ├─ CQL Engine Service (auto-scale)     │
│  ├─ Care Gap Service                    │
│  ├─ Risk Adjustment Service             │
│  │                                      │
│  ├─ PostgreSQL (Cloud-managed)          │
│  │  ├─ Hot data (last 3 months)        │
│  │  └─ Auto backups across regions     │
│  │                                      │
│  ├─ Redis Cache (Cloud-managed)         │
│  │  └─ Multi-AZ replication            │
│  │                                      │
│  └─ Kafka (Cloud-managed)               │
│     └─ Cross-region replication         │
│                                          │
└──────────────────────────────────────────┘
          ↑           ↓
    Your FHIR  Analytics Services
     Server    (Reporting, ML)
```

**Data Flow in Hybrid Setup:**

```
Request from Clinical Portal
    └─ On-Prem Gateway validates
    └─ Routes to Cloud services via VPN
    └─ Cloud CQL Engine evaluates
    └─ Quality Measure Service scores
    └─ Publishes event to Kafka
    └─ Returns result to On-Prem Gateway
    └─ On-Prem Gateway sends to portal

Benefits:
├─ Gateway stays on-prem (control)
├─ Compute auto-scales in cloud
├─ Data at rest in cloud (optional)
├─ Analytics in cloud-native tools
└─ On-prem and cloud fail over to each other
```

---

## Summary Comparison

| Architecture | Nodes | Services per Node | Load Balancer | HA | Cost | Complexity |
|---|---|---|---|---|---|---|
| **Single-Node** | 1 | 26 | None | No | Low | Low |
| **Clustered** | 5 | 5-7 | Required | Partial | Medium | Medium |
| **Kubernetes** | 3 CP + 5 WN | Dynamic | Ingress | Yes | Medium-High | High |
| **Hybrid** | On-Prem + Cloud | Dynamic | External | Yes | High | Very High |

Each architecture is suitable for different organizational sizes and requirements. Start with Single-Node for testing, move to Clustered for production, and scale to Kubernetes as you grow.

For detailed deployment instructions, see [Deployment Guides](./07-DEPLOYMENT-GUIDES.md).
