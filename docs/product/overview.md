# Health Data In Motion — Product Overview

Health Data In Motion is a **production-ready, Docker-native enterprise healthcare platform** that transforms healthcare interoperability, quality measurement, and care management into a scalable, cloud-agnostic solution. Built with distributed microservices architecture, it delivers enterprise-scale capabilities at startup costs.

## Value Proposition

- **Unified Exchange** – Consolidates FHIR, consent, quality measures, and analytics into a single platform.
- **Real-Time Intelligence** – Kafka-driven streaming surfaces care gaps, measures, and alerts within seconds.
- **Compliance Built-In** – HIPAA, 42 CFR Part 2, GDPR enforced via consent-aware services, audit logs, and zero-trust security.
- **Operator Efficiency** – Angular-powered admin portal delivers dashboards, API playgrounds, and service health in one view.
- **Production Ready** – Deploy in minutes with Docker, scale from 100 to 100,000+ users with proven architecture.
- **Cost Effective** – Entry point starting at $80-150/month, scales with usage, no vendor lock-in.

## Core Capabilities

### Clinical & Operational
- **FHIR Resource Management** – 150+ R4 resources, search bundles, validation, and audit trails.
- **Clinical Quality Measures** – CQL execution, 52 HEDIS measures, Star ratings, batch evaluation, real-time calculations.
- **Care Gap Management** – Automated detection, priority ranking, care team workflows, outcome tracking.
- **Patient Health Overview** – 360° patient view, risk stratification, mental health screening, medication management.
- **Consent & Privacy** – Policy engine supporting RBAC, ABAC, emergency access, granular consent enforcement.
- **Event Processing & Alerts** – Kafka consumers/processors for care gaps, webhook notifications, and DLQs.
- **Admin Portal Tools** – Live telemetry dashboards, service catalog, system health, and API playground with presets.

### Technical & Infrastructure
- **Docker-Native Deployment** – Single-command deployment (`docker compose up -d`), all 9 services containerized.
- **Distributed Microservices** – Independent scaling, fault isolation, zero-downtime updates.
- **Cloud-Agnostic** – Runs on any Docker-compatible infrastructure (AWS, Azure, GCP, on-premise).
- **Production Observability** – Built-in health checks, Prometheus metrics, centralized logging.
- **Horizontal Scaling** – Add instances as load increases, proven 100 → 100,000+ user path.
- **Multi-Tenant Architecture** – Secure data isolation, tenant-aware routing, consent enforcement.

## Audience & Personas

- **Healthcare Operators** – Monitor service health, track quality metrics, manage consents.
- **Clinical Teams** – Consume real-time care gap alerts, evaluate measure attainment.
- **Developers** – Test APIs through the playground, integrate with FHIR and streaming endpoints.
- **Security & Compliance** – Audit access, verify consent enforcement, manage incident response.
- **IT Leaders & Architects** – Evaluate deployment options, infrastructure requirements, scaling strategy.
- **Executives & Decision Makers** – Assess TCO, ROI projections, competitive differentiation.

## Distributed Architecture

Health Data In Motion is built on a **distributed microservices architecture** with 9 containerized services:

### Service Catalog

| Layer | Service | Purpose | Technology |
|-------|---------|---------|------------|
| **Frontend** | Clinical Portal | Angular SPA, clinical workflows | Angular 17+, Nginx |
| **API Gateway** | Gateway Service | Authentication, routing, rate limiting | Spring Boot, JWT |
| **Application** | CQL Engine | Clinical quality measure evaluation | HAPI FHIR CQL, Java 21 |
| | Quality Measure | Quality reporting, care gaps, patient health | Spring Boot, PostgreSQL |
| | FHIR Server | FHIR R4 resource management | HAPI FHIR Server |
| **Data** | PostgreSQL | Relational database (multi-tenant) | PostgreSQL 16 |
| | Redis | Session cache, query cache | Redis 7 |
| **Streaming** | Kafka | Event streaming, real-time processing | Apache Kafka 3.x |
| | Zookeeper | Kafka cluster coordination | Apache ZooKeeper |

### Architecture Characteristics

- **Loose Coupling** – Services communicate via REST APIs and Kafka events
- **Independent Deployment** – Each service can be updated without affecting others
- **Fault Isolation** – Service failures don't cascade to the entire system
- **Technology Diversity** – Choose best tool for each service (polyglot architecture)
- **Data Isolation** – Each service owns its data, multi-tenant aware
- **Observability** – Health checks, metrics, distributed tracing built-in

### Communication Patterns

```
Clinical Portal (Angular)
    ↓ HTTPS/REST
Gateway Service (Auth/Routing)
    ↓ Internal REST
┌───────────┬──────────────┬─────────────┐
│           │              │             │
CQL Engine  Quality Measure  FHIR Server
│           │              │
└───────────┴──────────────┴─────────────┘
            ↓ Kafka Events ↓
    Real-time Processing & Alerts
```

## Deployment Options

Health Data In Motion supports **three deployment tiers** to match your organization's scale and budget:

### Option 1: Docker Compose (Development & Small Production)
**Best for:** 10-500 concurrent users, single-server deployment

**Characteristics:**
- Single-command deployment: `docker compose up -d`
- All 9 services on one host
- Vertical scaling (add CPU/RAM as needed)
- Automated health monitoring
- Built-in backup scripts

**Infrastructure:**
- 8 CPU cores, 16 GB RAM, 100 GB SSD
- DigitalOcean, Linode, Hetzner, or on-premise

**Total Cost of Ownership (TCO):**
- Infrastructure: $80-150/month
- Deployment time: <10 minutes
- Operational overhead: Minimal (automated health checks)
- Scaling: Vertical (upgrade server resources)

### Option 2: Docker Swarm (Medium Production)
**Best for:** 500-5,000 concurrent users, multi-server deployment

**Characteristics:**
- High availability with service replication
- Load balancing across nodes
- Rolling updates with zero downtime
- Automated failover
- Horizontal scaling ready

**Infrastructure:**
- 3-5 nodes, 16+ CPU cores each, 32+ GB RAM each
- Distributed across availability zones

**Total Cost of Ownership (TCO):**
- Infrastructure: $300-800/month
- Deployment time: ~30 minutes (cluster setup)
- Operational overhead: Moderate (cluster management)
- Scaling: Horizontal (add nodes) + Vertical

### Option 3: Kubernetes (Enterprise Production)
**Best for:** 5,000-100,000+ concurrent users, global deployment

**Characteristics:**
- Auto-scaling based on CPU/memory/custom metrics
- Self-healing with automatic pod restarts
- Multi-region deployment
- Advanced networking (service mesh)
- GitOps deployment workflows

**Infrastructure:**
- Managed Kubernetes (EKS, AKS, GKE) or self-hosted
- Auto-scaling node pools
- Multi-zone/multi-region configurations

**Total Cost of Ownership (TCO):**
- Infrastructure: Variable ($1,000-10,000+/month based on scale)
- Deployment time: 1-2 hours (cluster + CI/CD setup)
- Operational overhead: Higher (requires K8s expertise)
- Scaling: Fully automated horizontal + vertical

### Deployment Comparison Matrix

| Feature | Docker Compose | Docker Swarm | Kubernetes |
|---------|---------------|--------------|------------|
| **Setup Complexity** | Low | Medium | High |
| **Operational Cost** | $80-150/mo | $300-800/mo | $1K-10K+/mo |
| **Concurrent Users** | 10-500 | 500-5,000 | 5K-100K+ |
| **High Availability** | No | Yes | Yes |
| **Auto-Scaling** | No | Manual | Automatic |
| **Zero-Downtime Updates** | No | Yes | Yes |
| **Multi-Region** | No | Limited | Yes |
| **Best For** | Dev, Small Prod | Medium Prod | Enterprise |

**See:** [DISTRIBUTION_ARCHITECTURE.md](../DISTRIBUTION_ARCHITECTURE.md) for detailed deployment guides and scaling strategies.

## Roadmap Themes

### Recently Completed
- ✅ **Distributed Docker Architecture** – Full containerization, production-ready deployment (Nov 2025)
- ✅ **Patient Health Overview** – Comprehensive patient dashboard with care gaps and risk scores (Nov 2025)
- ✅ **Custom Quality Measures** – Build and deploy custom CQL measures (Nov 2025)

### In Progress (Q1 2026)
- 🔄 **Kong API Gateway Integration** – Advanced rate limiting, analytics, developer portal
- 🔄 **Advanced AI/ML Predictions** – Risk stratification models, readmission prediction
- 🔄 **Enhanced Reporting** – Custom report builder, scheduled exports

### Planned (Q2 2026)
- 📋 **Mobile Apps** – iOS and Android native apps for clinical workflows
- 📋 **HL7 v2 Integration** – Support legacy ADT/ORM/ORU messages
- 📋 **Multi-language Support** – Spanish, French, Mandarin localization
- 📋 **Voice-Enabled Interfaces** – Hands-free data entry for providers

See `docs/product/roadmap.md` for the detailed quarterly roadmap capturing new service integrations, API enhancements, and UX improvements.
