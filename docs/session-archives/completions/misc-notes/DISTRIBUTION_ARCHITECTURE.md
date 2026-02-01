# HealthData-in-Motion - Distribution Architecture

**Version:** 2.0.0
**Last Updated:** 2025-11-26
**Status:** Production-Ready Distributed Architecture

---

## 📋 Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Service Inventory](#service-inventory)
3. [Network Topology](#network-topology)
4. [Data Flow](#data-flow)
5. [Scaling Strategy](#scaling-strategy)
6. [Deployment Options](#deployment-options)
7. [Security Architecture](#security-architecture)
8. [Monitoring & Observability](#monitoring--observability)

---

## 🏗️ Architecture Overview

### Current State
```
┌─────────────────────────────────────────────────────────────────┐
│                        Load Balancer / CDN                       │
│                    (Kong API Gateway - Port 8000)                │
└───────────────────────────┬─────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌───────────────┐   ┌──────────────┐   ┌──────────────┐
│   Frontend    │   │   Backend    │   │  Monitoring  │
│  (Angular)    │   │  Services    │   │   Stack      │
│  Port 4200    │   │  Spring Boot │   │  Prom/Grafana│
└───────────────┘   └──────┬───────┘   └──────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ CQL Engine   │  │  Quality     │  │  FHIR Mock   │
│ Service      │  │  Measure     │  │  Service     │
│ Port 8081    │  │  Port 8087   │  │  Port 8080   │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                 │
       └─────────────────┼─────────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
        ▼                ▼                ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  PostgreSQL  │  │    Redis     │  │    Kafka     │
│  Port 5435   │  │  Port 6380   │  │  Port 9094   │
└──────────────┘  └──────────────┘  └──────────────┘
```

### Distribution Strategy

**Tier 1: Edge Layer** (CDN/Load Balancing)
- **Purpose**: Global content delivery, SSL termination, DDoS protection
- **Components**: Kong API Gateway
- **Scaling**: Horizontal (multiple Kong instances with shared DB)

**Tier 2: Application Layer** (Stateless Services)
- **Purpose**: Business logic, API endpoints, user interfaces
- **Components**: Frontend, Backend microservices
- **Scaling**: Horizontal (container orchestration)
- **State**: Stateless - all state in data layer

**Tier 3: Data Layer** (Stateful Services)
- **Purpose**: Data persistence, caching, event streaming
- **Components**: PostgreSQL, Redis, Kafka
- **Scaling**: Vertical + replication/sharding
- **State**: Stateful - managed persistence

---

## 📦 Service Inventory

### Frontend Services

| Service | Port | Type | Scale | Resource Needs |
|---------|------|------|-------|----------------|
| **Clinical Portal** | 4200 | Angular SPA | Horizontal | CPU: 0.25, Mem: 512MB |

### Backend Services

| Service | Port | Type | Scale | Resource Needs |
|---------|------|------|-------|----------------|
| **Gateway Service** | 8001 | Spring Gateway | Horizontal | CPU: 0.5, Mem: 1GB |
| **CQL Engine** | 8081 | Spring Boot | Horizontal | CPU: 1.0, Mem: 2GB |
| **Quality Measure** | 8087 | Spring Boot | Horizontal | CPU: 0.75, Mem: 1.5GB |
| **FHIR Service** | 8080 | Spring Boot | Horizontal | CPU: 0.5, Mem: 1GB |

### Infrastructure Services

| Service | Port | Type | Scale | Resource Needs |
|---------|------|------|-------|----------------|
| **Kong Gateway** | 8000 | API Gateway | Horizontal | CPU: 0.5, Mem: 1GB |
| **PostgreSQL** | 5435 | RDBMS | Vertical + Read Replicas | CPU: 2.0, Mem: 4GB |
| **Redis** | 6380 | Cache | Vertical + Sentinel | CPU: 0.5, Mem: 2GB |
| **Kafka** | 9094 | Event Stream | Horizontal (Cluster) | CPU: 1.0, Mem: 2GB |
| **Zookeeper** | 2182 | Coordination | Horizontal (Cluster) | CPU: 0.25, Mem: 512MB |
| **Prometheus** | 9090 | Monitoring | Vertical | CPU: 0.5, Mem: 1GB |
| **Grafana** | 3001 | Visualization | Vertical | CPU: 0.25, Mem: 512MB |

**Total Resource Requirements (Single Instance):**
- CPU: ~8.5 cores
- Memory: ~17 GB
- Storage: ~100 GB (with data growth planning)

---

## 🌐 Network Topology

### Network Segmentation

```
┌─────────────────────────────────────────────────────────────┐
│                     Public Internet                          │
└────────────────────────┬────────────────────────────────────┘
                         │
                    ┌────▼────┐
                    │   CDN   │ (Optional: CloudFlare, CloudFront)
                    └────┬────┘
                         │
              ┌──────────▼──────────┐
              │  DMZ Network        │
              │  Kong Gateway       │
              │  (Public Ingress)   │
              └──────────┬──────────┘
                         │
              ┌──────────▼──────────┐
              │  Application Net    │
              │  - Frontend         │
              │  - Backend Services │
              └──────────┬──────────┘
                         │
              ┌──────────▼──────────┐
              │  Data Network       │
              │  - PostgreSQL       │
              │  - Redis            │
              │  - Kafka            │
              └─────────────────────┘
```

### Docker Network Configuration

- **healthdata-network**: Bridge network for all services
- **Isolation**: Services communicate only through defined APIs
- **Security**: No direct external access to data layer

### Port Mapping Strategy

**External Ports** (accessible from host):
- **8000**: Kong API Gateway (Public API)
- **3001**: Grafana Dashboard
- **9090**: Prometheus Metrics

**Internal Ports** (container-to-container only):
- Backend services communicate via internal network
- Database/Cache/Kafka only accessible to backend

---

## 🔄 Data Flow

### Request Flow

```
User Browser
    │
    │ HTTPS
    ▼
Kong Gateway (8000)
    │
    ├──► /api/cql/** ───────► CQL Engine (8081)
    │                              │
    ├──► /api/quality/** ──────► Quality Measure (8087)
    │                              │
    ├──► /api/fhir/** ─────────► FHIR Service (8080)
    │                              │
    └──► /* (SPA routes) ──────► Clinical Portal (4200)
                                   │
                                   └──► Static Assets
```

### Event Flow (Kafka)

```
CQL Engine ──► Kafka Topic: quality.evaluations.completed
                    │
                    ├──► Quality Measure Service (Consumer)
                    ├──► Audit Service (Consumer)
                    └──► Analytics Service (Consumer)
```

### Cache Strategy (Redis)

```
Request ──► Backend Service
                │
                ├──► Check Redis Cache
                │        │
                │        ├─ HIT ──► Return Cached Data
                │        │
                │        └─ MISS ──► Query Database
                │                        │
                │                        └──► Cache in Redis (TTL: 5min)
                │
                └──► Response
```

---

## 📈 Scaling Strategy

### Horizontal Scaling (Stateless Services)

**Frontend (Clinical Portal)**
```bash
# Scale to 3 instances
docker-compose up -d --scale clinical-portal=3

# Nginx load balancer distributes traffic
```

**Backend Services**
```bash
# Scale CQL Engine
docker-compose up -d --scale cql-engine=3

# Scale Quality Measure
docker-compose up -d --scale quality-measure=2
```

### Vertical Scaling (Stateful Services)

**PostgreSQL**
- Primary: 4 CPU, 8GB RAM
- Read Replica 1: 2 CPU, 4GB RAM
- Read Replica 2: 2 CPU, 4GB RAM

**Redis**
- Master: 2 CPU, 4GB RAM
- Redis Sentinel for failover

**Kafka**
- 3-node cluster: Each 1 CPU, 2GB RAM
- Topic partitioning for parallelism

### Auto-Scaling Configuration

**Kubernetes HPA (Horizontal Pod Autoscaler)**
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: cql-engine-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: cql-engine
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

---

## 🚀 Deployment Options

### Option 1: Docker Compose (Development/Small Production)

**Use Case**: Single-server deployments, development, small clinics
**Capacity**: ~100 concurrent users
**Cost**: $50-100/month (single VM)

```bash
# Deploy entire stack
docker-compose up -d

# Monitoring
docker-compose ps
docker-compose logs -f
```

### Option 2: Docker Swarm (Medium Production)

**Use Case**: Multi-server deployments, medium clinics
**Capacity**: ~500 concurrent users
**Cost**: $200-500/month (3-5 VMs)

```bash
# Initialize swarm
docker swarm init

# Deploy stack
docker stack deploy -c docker-compose.yml healthdata

# Scale services
docker service scale healthdata_cql-engine=3
```

### Option 3: Kubernetes (Large Production/Enterprise)

**Use Case**: Large hospitals, multi-tenant SaaS
**Capacity**: 10,000+ concurrent users
**Cost**: $1,000-5,000/month (managed Kubernetes)

**Deployment Targets:**
- AWS EKS
- Google GKE
- Azure AKS
- Self-managed K8s

```bash
# Deploy to Kubernetes
kubectl apply -f k8s/

# Monitor
kubectl get pods -n healthdata
kubectl get services -n healthdata
```

### Option 4: Serverless (Global SaaS)

**Use Case**: Multi-region, global deployment
**Capacity**: Unlimited (auto-scaling)
**Cost**: Pay-per-use (~$0.0001/request)

**Components:**
- Frontend: Vercel/Netlify/CloudFront + S3
- Backend: AWS Lambda/Cloud Run
- Database: Aurora Serverless/Cloud SQL
- Cache: ElastiCache/MemoryStore
- Events: AWS EventBridge/Cloud Pub/Sub

---

## 🔒 Security Architecture

### Network Security

```
┌─────────────────────────────────────────┐
│         Security Layers                 │
├─────────────────────────────────────────┤
│ 1. WAF (Web Application Firewall)      │
│    - DDoS protection                    │
│    - SQL injection prevention           │
│    - XSS protection                     │
├─────────────────────────────────────────┤
│ 2. Kong Gateway                         │
│    - Rate limiting                      │
│    - JWT validation                     │
│    - Request/Response transformation    │
├─────────────────────────────────────────┤
│ 3. Application Layer                    │
│    - Spring Security                    │
│    - HIPAA audit logging                │
│    - Multi-tenant isolation             │
├─────────────────────────────────────────┤
│ 4. Data Layer                           │
│    - Encryption at rest                 │
│    - Network isolation                  │
│    - Access control lists               │
└─────────────────────────────────────────┘
```

### Secrets Management

**Docker Secrets** (Swarm/Compose)
```bash
# Create secrets
echo "prod_db_password" | docker secret create db_password -

# Use in service
services:
  postgres:
    secrets:
      - db_password
```

**Kubernetes Secrets**
```bash
# Create secret
kubectl create secret generic db-credentials \
  --from-literal=username=healthdata \
  --from-literal=password=prod_password
```

**External Secrets Manager**
- AWS Secrets Manager
- Azure Key Vault
- HashiCorp Vault

---

## 📊 Monitoring & Observability

### Metrics Collection

**Prometheus Metrics:**
- **Application Metrics**: Request rate, latency, error rate
- **JVM Metrics**: Heap usage, GC, thread count
- **Business Metrics**: Evaluations/sec, patients processed
- **Infrastructure Metrics**: CPU, memory, disk, network

**Grafana Dashboards:**
1. **System Overview**: Overall health, service status
2. **CQL Engine**: Evaluation throughput, cache hit rate
3. **Database**: Query performance, connection pool
4. **Kafka**: Topic lag, message rate
5. **Business**: Daily evaluations, quality scores

### Logging Strategy

**Log Aggregation:**
```
Application Logs ──► Fluentd/Logstash ──► Elasticsearch ──► Kibana
```

**Log Levels:**
- **ERROR**: System errors requiring immediate attention
- **WARN**: Potential issues (cache miss, slow query)
- **INFO**: Business events (evaluation completed)
- **DEBUG**: Detailed debugging (disabled in production)

### Alerting

**Critical Alerts** (PagerDuty/Opsgenie):
- Service down >5 minutes
- Error rate >5%
- Database connection pool exhausted
- Disk usage >90%

**Warning Alerts** (Slack/Email):
- High latency (>2s p95)
- Cache hit rate <80%
- Kafka consumer lag >1000 messages

---

## 🗺️ Migration Path

### Phase 1: Docker Compose (Current) ✅
- Single-server deployment
- All services in docker-compose.yml
- Suitable for development and small deployments

### Phase 2: Add Frontend to Docker ⏳ (In Progress)
- Dockerize Angular app
- Add to docker-compose
- Configure Kong routing

### Phase 3: Production Hardening 📋 (Next)
- SSL/TLS certificates
- Secrets management
- Backup/restore procedures
- Disaster recovery plan

### Phase 4: Kubernetes Migration 🎯 (Future)
- Convert docker-compose to K8s manifests
- Helm charts for easy deployment
- CI/CD pipeline integration
- Multi-region deployment

---

## 📝 Quick Start Commands

### Local Development
```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f clinical-portal cql-engine

# Stop all services
docker-compose down
```

### Production Deployment
```bash
# Build production images
./scripts/build-production.sh

# Deploy to production
./scripts/deploy-production.sh

# Health check
./scripts/health-check.sh
```

### Scaling
```bash
# Scale horizontally
docker-compose up -d --scale cql-engine=3

# Check status
docker-compose ps
```

---

## 📞 Support & Troubleshooting

### Common Issues

**Issue**: Service won't start
```bash
# Check logs
docker-compose logs [service-name]

# Check health
docker-compose ps
```

**Issue**: High memory usage
```bash
# Check resource usage
docker stats

# Adjust memory limits in docker-compose.yml
```

**Issue**: Database connection pool exhausted
```bash
# Increase pool size in application.yml
spring.datasource.hikari.maximum-pool-size=20
```

---

## 🔄 Continuous Improvement

### Performance Optimization Roadmap
1. **Q1 2026**: Implement Redis caching layer
2. **Q2 2026**: Add read replicas for PostgreSQL
3. **Q3 2026**: Migrate to Kubernetes for auto-scaling
4. **Q4 2026**: Multi-region deployment

### Monitoring Improvements
1. Distributed tracing (Jaeger/Zipkin)
2. Real User Monitoring (RUM)
3. Synthetic monitoring
4. Log analytics with AI/ML

---

**Document Version:** 2.0.0
**Last Review:** 2025-11-26
**Next Review:** 2026-02-01
