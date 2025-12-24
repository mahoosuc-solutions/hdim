# HDIM Deployment Options

**Flexible Deployment for Every Organization**

*Version 1.0 | December 2025*

---

## Executive Summary

HDIM offers multiple deployment options to meet the security, compliance, and operational requirements of healthcare organizations of all sizes.

| Option | Best For | Timeline | Starting Cost |
|--------|----------|----------|---------------|
| **SaaS (Multi-Tenant)** | Most organizations | 1-2 weeks | $49/mo |
| **SaaS (Dedicated)** | Large ACOs, health systems | 2-4 weeks | $2,499/mo |
| **Private Cloud** | Strict data residency requirements | 4-6 weeks | $5,000/mo |
| **On-Premise** | Air-gapped, highly regulated | 8-12 weeks | $10,000/mo |

---

## 1. Deployment Options Overview

### 1.1 Comparison Matrix

| Feature | SaaS Multi-Tenant | SaaS Dedicated | Private Cloud | On-Premise |
|---------|-------------------|----------------|---------------|------------|
| **Infrastructure** | Shared | Dedicated | Customer cloud | Customer datacenter |
| **Data Isolation** | Logical | Physical | Physical | Physical |
| **Data Location** | US (AWS) | US (AWS) | Customer choice | Customer choice |
| **Maintenance** | HDIM | HDIM | Shared | Customer |
| **Updates** | Automatic | Scheduled | Scheduled | Manual |
| **Customization** | Limited | Moderate | High | Full |
| **SLA** | 99.9% | 99.95% | Negotiable | Customer-defined |
| **Setup Time** | 1-2 weeks | 2-4 weeks | 4-6 weeks | 8-12 weeks |
| **Minimum Contract** | Monthly | Annual | Annual | Annual |
| **Starting Price** | $49/mo | $2,499/mo | $5,000/mo | $10,000/mo |

### 1.2 Decision Tree

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     WHICH DEPLOYMENT IS RIGHT FOR YOU?                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   Do you require data to stay within your infrastructure?                   │
│                                                                             │
│       YES ─────────────────────────────────────────────► On-Premise         │
│        │                                                                    │
│       NO                                                                    │
│        │                                                                    │
│        ▼                                                                    │
│   Do you require a specific cloud provider or region?                       │
│                                                                             │
│       YES ─────────────────────────────────────────────► Private Cloud      │
│        │                                                                    │
│       NO                                                                    │
│        │                                                                    │
│        ▼                                                                    │
│   Do you require dedicated infrastructure (not shared)?                     │
│                                                                             │
│       YES ─────────────────────────────────────────────► SaaS Dedicated     │
│        │                                                                    │
│       NO                                                                    │
│        │                                                                    │
│        ▼                                                                    │
│   ┌─────────────────────────────────────────────────────────────────┐       │
│   │                      SaaS Multi-Tenant                          │       │
│   │           (Recommended for most organizations)                  │       │
│   └─────────────────────────────────────────────────────────────────┘       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. SaaS Multi-Tenant (Recommended)

### 2.1 Overview

The standard HDIM deployment. Your organization shares infrastructure with other customers, with complete logical data isolation.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         SAAS MULTI-TENANT                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                              INTERNET                                       │
│                                  │                                          │
│                                  ▼                                          │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                    HDIM Cloud (AWS US-East)                         │   │
│   │  ┌─────────────────────────────────────────────────────────────┐    │   │
│   │  │                     Load Balancer                           │    │   │
│   │  └─────────────────────────────────────────────────────────────┘    │   │
│   │                              │                                      │   │
│   │  ┌─────────────────────────────────────────────────────────────┐    │   │
│   │  │              Shared Application Layer                       │    │   │
│   │  │    ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐       │    │   │
│   │  │    │ Org A   │  │ Org B   │  │ Org C   │  │ Org D   │       │    │   │
│   │  │    │ (you)   │  │         │  │         │  │         │       │    │   │
│   │  │    └─────────┘  └─────────┘  └─────────┘  └─────────┘       │    │   │
│   │  └─────────────────────────────────────────────────────────────┘    │   │
│   │                              │                                      │   │
│   │  ┌─────────────────────────────────────────────────────────────┐    │   │
│   │  │              Shared Database (Row-Level Security)           │    │   │
│   │  │    ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐       │    │   │
│   │  │    │ Org A   │  │ Org B   │  │ Org C   │  │ Org D   │       │    │   │
│   │  │    │ Data    │  │ Data    │  │ Data    │  │ Data    │       │    │   │
│   │  │    └─────────┘  └─────────┘  └─────────┘  └─────────┘       │    │   │
│   │  └─────────────────────────────────────────────────────────────┘    │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Specifications

| Attribute | Details |
|-----------|---------|
| **Infrastructure** | AWS US-East-1 (N. Virginia) |
| **Data Isolation** | Row-level security, tenant ID on all tables |
| **Encryption** | AES-256 at rest, TLS 1.3 in transit |
| **Backups** | Daily automated, 30-day retention |
| **Disaster Recovery** | Cross-region replication, 4-hour RTO |
| **Updates** | Automatic, zero-downtime deployments |
| **SLA** | 99.9% uptime guarantee |

### 2.3 Security Controls

| Control | Implementation |
|---------|----------------|
| **Tenant Isolation** | PostgreSQL Row-Level Security (RLS) |
| **Data Access** | All queries filtered by tenant_id |
| **Cross-Tenant** | Impossible by design |
| **Encryption Keys** | Per-tenant data encryption keys |
| **Audit Logs** | Per-tenant, isolated |

### 2.4 Best For

- ✅ Small to mid-size organizations
- ✅ Quick deployment needed
- ✅ Budget-conscious organizations
- ✅ Standard compliance requirements
- ✅ Organizations without IT infrastructure

### 2.5 Limitations

- ❌ No custom data residency
- ❌ Shared infrastructure (logical isolation only)
- ❌ Limited customization
- ❌ Update schedule controlled by HDIM

### 2.6 Pricing

| Tier | Monthly | Annual |
|------|---------|--------|
| Community | $49 | $588 |
| Professional | $299 | $3,588 |
| Enterprise | $999 | $11,988 |

---

## 3. SaaS Dedicated

### 3.1 Overview

Dedicated HDIM infrastructure running in HDIM's cloud, isolated from other customers.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           SAAS DEDICATED                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                              INTERNET                                       │
│                                  │                                          │
│                                  ▼                                          │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                    HDIM Cloud (AWS US-East)                         │   │
│   │                                                                     │   │
│   │   ┌─────────────────────────────────────────────────────────────┐   │   │
│   │   │             YOUR DEDICATED ENVIRONMENT                      │   │   │
│   │   │                                                             │   │   │
│   │   │    ┌───────────────────────────────────────────────────┐    │   │   │
│   │   │    │              Your Load Balancer                   │    │   │   │
│   │   │    └───────────────────────────────────────────────────┘    │   │   │
│   │   │                           │                                 │   │   │
│   │   │    ┌───────────────────────────────────────────────────┐    │   │   │
│   │   │    │           Your Application Servers                │    │   │   │
│   │   │    │   ┌─────────┐  ┌─────────┐  ┌─────────┐           │    │   │   │
│   │   │    │   │ App 1   │  │ App 2   │  │ App 3   │           │    │   │   │
│   │   │    │   └─────────┘  └─────────┘  └─────────┘           │    │   │   │
│   │   │    └───────────────────────────────────────────────────┘    │   │   │
│   │   │                           │                                 │   │   │
│   │   │    ┌───────────────────────────────────────────────────┐    │   │   │
│   │   │    │             Your Dedicated Database               │    │   │   │
│   │   │    │                (Isolated RDS)                     │    │   │   │
│   │   │    └───────────────────────────────────────────────────┘    │   │   │
│   │   │                                                             │   │   │
│   │   └─────────────────────────────────────────────────────────────┘   │   │
│   │                                                                     │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Specifications

| Attribute | Details |
|-----------|---------|
| **Infrastructure** | Dedicated VPC, compute, and database |
| **Data Isolation** | Physical isolation (separate database) |
| **Region Options** | US-East, US-West, EU-West (on request) |
| **Encryption** | Dedicated KMS keys |
| **Backups** | Daily + on-demand, 90-day retention |
| **Disaster Recovery** | Cross-region, 1-hour RTO |
| **Updates** | Scheduled maintenance windows |
| **SLA** | 99.95% uptime guarantee |

### 3.3 Additional Features

| Feature | Included |
|---------|----------|
| Custom subdomain | yourorg.healthdatainmotion.com |
| IP allowlisting | ✅ |
| VPN connectivity | ✅ (additional cost) |
| Custom backup schedule | ✅ |
| Dedicated support | Named CSM |
| Uptime SLA | 99.95% |

### 3.4 Best For

- ✅ Large ACOs and health systems
- ✅ Organizations requiring physical data isolation
- ✅ Compliance requirements beyond standard
- ✅ Predictable, scheduled maintenance windows
- ✅ Higher uptime requirements

### 3.5 Limitations

- ❌ Still in HDIM-managed cloud
- ❌ Limited region options
- ❌ Higher cost
- ❌ Longer setup time

### 3.6 Pricing

| Component | Monthly |
|-----------|---------|
| Base platform | $2,499 |
| Per additional 50K patients | $500 |
| VPN connectivity | $200 |
| Custom domain SSL | Included |
| **Typical total** | **$2,499-5,000** |

---

## 4. Private Cloud

### 4.1 Overview

HDIM deployed in your cloud account (AWS, Azure, or GCP), managed by HDIM.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           PRIVATE CLOUD                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                              INTERNET                                       │
│                                  │                                          │
│                                  ▼                                          │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                 YOUR CLOUD ACCOUNT (AWS/Azure/GCP)                  │   │
│   │                                                                     │   │
│   │   ┌─────────────────────────────────────────────────────────────┐   │   │
│   │   │                    Your VPC/VNET                            │   │   │
│   │   │                                                             │   │   │
│   │   │    ┌─────────────────────────────────────────────────┐      │   │   │
│   │   │    │   HDIM Application (managed by HDIM)            │      │   │   │
│   │   │    │                                                 │      │   │   │
│   │   │    │   ┌─────────┐  ┌─────────┐  ┌─────────┐         │      │   │   │
│   │   │    │   │ K8s     │  │ App     │  │ CQL     │         │      │   │   │
│   │   │    │   │ Cluster │  │ Services│  │ Engine  │         │      │   │   │
│   │   │    │   └─────────┘  └─────────┘  └─────────┘         │      │   │   │
│   │   │    │                                                 │      │   │   │
│   │   │    └─────────────────────────────────────────────────┘      │   │   │
│   │   │                           │                                 │   │   │
│   │   │    ┌─────────────────────────────────────────────────┐      │   │   │
│   │   │    │   Your Database (in your account)               │      │   │   │
│   │   │    │   RDS / Azure SQL / Cloud SQL                   │      │   │   │
│   │   │    └─────────────────────────────────────────────────┘      │   │   │
│   │   │                                                             │   │   │
│   │   │    ┌───────────────────────────────────────────────────┐    │   │   │
│   │   │    │ Optional: Connect to your existing systems       │    │   │   │
│   │   │    │ • EHR  • Data Warehouse  • Identity Provider     │    │   │   │
│   │   │    └───────────────────────────────────────────────────┘    │   │   │
│   │   │                                                             │   │   │
│   │   └─────────────────────────────────────────────────────────────┘   │   │
│   │                                                                     │   │
│   │   ┌─────────────────────────────────────────────────────────────┐   │   │
│   │   │  HDIM Management Access (for updates & support)             │   │   │
│   │   │  • Secure VPN / PrivateLink                                 │   │   │
│   │   │  • Read-only monitoring                                     │   │   │
│   │   │  • Deployment automation                                    │   │   │
│   │   └─────────────────────────────────────────────────────────────┘   │   │
│   │                                                                     │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Supported Cloud Providers

| Provider | Support Level | Regions |
|----------|---------------|---------|
| **AWS** | Full support | All commercial regions |
| **Azure** | Full support | All commercial regions |
| **GCP** | Full support | All commercial regions |
| **AWS GovCloud** | Available | US-Gov-West, US-Gov-East |
| **Azure Government** | Available | US Gov regions |

### 4.3 Specifications

| Attribute | Details |
|-----------|---------|
| **Infrastructure** | Customer's cloud account |
| **Data Location** | Customer choice (any region) |
| **Management** | HDIM manages application, customer owns infra |
| **Access** | HDIM has limited, audited management access |
| **Updates** | Scheduled, customer-approved |
| **Networking** | Integrates with existing VPC/VNET |

### 4.4 Responsibility Model

| Component | Customer | HDIM |
|-----------|----------|------|
| Cloud account & billing | ✅ | |
| Network configuration | ✅ | Advise |
| Security groups/firewalls | ✅ | Advise |
| Database provisioning | ✅ | Advise |
| Kubernetes cluster | ✅ | Manage |
| HDIM application | | ✅ |
| Application updates | | ✅ |
| Monitoring & alerting | Shared | Shared |
| Backup execution | | ✅ |
| Backup storage | ✅ | |
| Incident response | Shared | Shared |

### 4.5 Requirements

**Minimum Infrastructure:**

| Component | Specification |
|-----------|---------------|
| Kubernetes | EKS, AKS, or GKE (1.27+) |
| Nodes | 3 nodes, 4 vCPU, 16GB RAM each |
| Database | PostgreSQL 14+ (RDS, Azure SQL, Cloud SQL) |
| Storage | 500GB SSD minimum |
| Load Balancer | ALB, Azure LB, or GCP LB |
| Networking | VPC with private subnets |

**Estimated Infrastructure Cost:**

| Component | AWS Estimate | Azure Estimate | GCP Estimate |
|-----------|--------------|----------------|--------------|
| Kubernetes (3 nodes) | $400/mo | $350/mo | $380/mo |
| Database (RDS/SQL) | $300/mo | $280/mo | $290/mo |
| Load Balancer | $25/mo | $20/mo | $25/mo |
| Storage | $50/mo | $45/mo | $50/mo |
| Data Transfer | $100/mo | $100/mo | $100/mo |
| **Total Infra** | **~$875/mo** | **~$795/mo** | **~$845/mo** |

### 4.6 Best For

- ✅ Organizations with existing cloud infrastructure
- ✅ Specific data residency requirements (EU, Canada, etc.)
- ✅ Integration with existing cloud services
- ✅ Organizations with cloud expertise
- ✅ Government/regulated entities with cloud mandates

### 4.7 Limitations

- ❌ Requires cloud expertise on customer side
- ❌ Customer responsible for infrastructure costs
- ❌ More complex setup
- ❌ Shared responsibility for incidents

### 4.8 Pricing

| Component | Monthly |
|-----------|---------|
| HDIM license | $5,000 |
| Per additional 100K patients | $1,000 |
| Implementation (one-time) | $10,000-25,000 |
| **Your infrastructure** | **~$800-2,000** |
| **Total typical** | **$5,800-8,000** |

---

## 5. On-Premise

### 5.1 Overview

HDIM deployed entirely within your datacenter, with optional HDIM support.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            ON-PREMISE                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                     YOUR DATACENTER                                 │   │
│   │                                                                     │   │
│   │   ┌─────────────────────────────────────────────────────────────┐   │   │
│   │   │                    Your Network                             │   │   │
│   │   │                                                             │   │   │
│   │   │    ┌─────────────────────────────────────────────────┐      │   │   │
│   │   │    │              Load Balancer                      │      │   │   │
│   │   │    └─────────────────────────────────────────────────┘      │   │   │
│   │   │                           │                                 │   │   │
│   │   │    ┌─────────────────────────────────────────────────┐      │   │   │
│   │   │    │         Kubernetes Cluster (or VMs)             │      │   │   │
│   │   │    │                                                 │      │   │   │
│   │   │    │   ┌─────────┐  ┌─────────┐  ┌─────────┐         │      │   │   │
│   │   │    │   │ HDIM    │  │ HDIM    │  │ HDIM    │         │      │   │   │
│   │   │    │   │ Portal  │  │ API     │  │ CQL     │         │      │   │   │
│   │   │    │   └─────────┘  └─────────┘  └─────────┘         │      │   │   │
│   │   │    │                                                 │      │   │   │
│   │   │    └─────────────────────────────────────────────────┘      │   │   │
│   │   │                           │                                 │   │   │
│   │   │    ┌─────────────────────────────────────────────────┐      │   │   │
│   │   │    │              PostgreSQL Database                │      │   │   │
│   │   │    └─────────────────────────────────────────────────┘      │   │   │
│   │   │                                                             │   │   │
│   │   │    ┌───────────────────────────────────────────────────┐    │   │   │
│   │   │    │ Integration with internal systems                 │    │   │   │
│   │   │    │ • EHR  • Active Directory  • Data Warehouse       │    │   │   │
│   │   │    └───────────────────────────────────────────────────┘    │   │   │
│   │   │                                                             │   │   │
│   │   └─────────────────────────────────────────────────────────────┘   │   │
│   │                                                                     │   │
│   │   ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐   │
│   │     OPTIONAL: HDIM Support Access (VPN)                             │
│   │   │ • Remote troubleshooting                                    │   │
│   │     • Update assistance                                             │
│   │   │ • Monitoring integration                                    │   │
│   │   └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘   │
│   │                                                                     │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│   ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐   │
│                               INTERNET                                      │
│   │                     (Optional - Air-gapped possible)                │   │
│   └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Deployment Options

| Option | Description | Internet Required |
|--------|-------------|-------------------|
| **Connected** | On-prem with internet for updates | Yes |
| **Hybrid** | On-prem with VPN to HDIM for support | Limited |
| **Air-Gapped** | Fully isolated, manual updates | No |

### 5.3 Specifications

| Attribute | Details |
|-----------|---------|
| **Delivery** | Docker images or Kubernetes manifests |
| **Database** | PostgreSQL 14+ (customer-managed) |
| **Updates** | Manual or semi-automated |
| **Support** | Remote (VPN) or on-site |
| **Licensing** | Perpetual or annual subscription |

### 5.4 Hardware Requirements

**Minimum (up to 50,000 patients):**

| Component | Specification |
|-----------|---------------|
| **Application Servers** | 3× (4 vCPU, 16GB RAM, 100GB SSD) |
| **Database Server** | 1× (8 vCPU, 32GB RAM, 500GB SSD) |
| **Load Balancer** | Hardware or software LB |
| **Total Compute** | 20 vCPU, 80GB RAM |
| **Total Storage** | 800GB SSD |

**Recommended (up to 200,000 patients):**

| Component | Specification |
|-----------|---------------|
| **Application Servers** | 6× (8 vCPU, 32GB RAM, 200GB SSD) |
| **Database Server** | 2× (16 vCPU, 64GB RAM, 1TB SSD) - Primary/Replica |
| **Load Balancer** | HA pair |
| **Total Compute** | 80 vCPU, 320GB RAM |
| **Total Storage** | 3.2TB SSD |

### 5.5 Software Requirements

| Component | Requirement |
|-----------|-------------|
| **Operating System** | RHEL 8+, Ubuntu 20.04+, or Rocky Linux 8+ |
| **Container Runtime** | Docker 20.10+ or containerd |
| **Orchestration** | Kubernetes 1.27+ (optional but recommended) |
| **Database** | PostgreSQL 14+ |
| **Java Runtime** | OpenJDK 17+ (included in containers) |
| **Reverse Proxy** | Nginx, HAProxy, or F5 |

### 5.6 Responsibility Model

| Component | Customer | HDIM |
|-----------|----------|------|
| Hardware procurement | ✅ | |
| OS installation & patching | ✅ | |
| Network configuration | ✅ | |
| Firewall rules | ✅ | |
| Kubernetes/Docker setup | ✅ | Advise |
| Database setup | ✅ | Advise |
| HDIM installation | ✅ | Support |
| HDIM configuration | Shared | Shared |
| Application updates | ✅ | Provide |
| Monitoring setup | ✅ | Advise |
| Backup configuration | ✅ | Advise |
| Security hardening | ✅ | Advise |
| Incident response | ✅ | Support |

### 5.7 Best For

- ✅ Organizations with strict data sovereignty requirements
- ✅ Air-gapped or highly secure environments
- ✅ Government/military deployments
- ✅ Organizations with existing datacenter infrastructure
- ✅ Maximum control requirements

### 5.8 Limitations

- ❌ Highest complexity
- ❌ Customer responsible for all infrastructure
- ❌ Manual update process
- ❌ Requires internal IT expertise
- ❌ Longer time to deploy
- ❌ Highest total cost of ownership

### 5.9 Pricing

| Component | Cost |
|-----------|------|
| **License Options:** | |
| Annual subscription | $10,000/month |
| Perpetual license | $500,000 (one-time) |
| Annual maintenance (perpetual) | $100,000/year |
| | |
| **Implementation:** | |
| Standard implementation | $50,000 |
| Complex implementation | $100,000+ |
| | |
| **Support Options:** | |
| Standard (business hours) | Included |
| Premium (24/7) | +$2,000/month |
| On-site support days | $2,500/day |

---

## 6. Data Residency Options

### 6.1 Available Regions

| Region | SaaS Multi | SaaS Dedicated | Private Cloud | On-Premise |
|--------|------------|----------------|---------------|------------|
| **US East** | ✅ Default | ✅ | ✅ | ✅ |
| **US West** | ❌ | ✅ | ✅ | ✅ |
| **Canada** | ❌ | ✅ | ✅ | ✅ |
| **EU (Ireland)** | ❌ | ✅ | ✅ | ✅ |
| **EU (Frankfurt)** | ❌ | ❌ | ✅ | ✅ |
| **UK** | ❌ | ❌ | ✅ | ✅ |
| **Australia** | ❌ | ❌ | ✅ | ✅ |
| **US GovCloud** | ❌ | ❌ | ✅ | ✅ |
| **Custom** | ❌ | ❌ | ✅ | ✅ |

### 6.2 Data Residency Guarantees

| Deployment | Guarantee |
|------------|-----------|
| SaaS Multi-Tenant | Data stored in US only |
| SaaS Dedicated | Data stored in selected region |
| Private Cloud | Data never leaves your account |
| On-Premise | Data never leaves your facility |

---

## 7. Implementation Process

### 7.1 SaaS Multi-Tenant (1-2 weeks)

```
Week 1                                          Week 2
──────                                          ──────
Day 1-2:    Account setup, user provisioning
Day 3-4:    Data import configuration
Day 5:      Initial data load
                                                Day 1-2:    Testing & validation
                                                Day 3-4:    User training
                                                Day 5:      Go-live
```

### 7.2 SaaS Dedicated (2-4 weeks)

```
Week 1              Week 2              Week 3              Week 4
──────              ──────              ──────              ──────
Environment         Application         Data migration      Testing
provisioning        deployment          & configuration     & training
                                                            Go-live
```

### 7.3 Private Cloud (4-6 weeks)

```
Week 1-2            Week 3              Week 4              Week 5-6
────────            ──────              ──────              ────────
Infrastructure      HDIM                Integration         Testing,
setup               deployment          configuration       training,
                                                            go-live
```

### 7.4 On-Premise (8-12 weeks)

```
Week 1-2        Week 3-4        Week 5-6        Week 7-8        Week 9-12
────────        ────────        ────────        ────────        ─────────
Planning &      Hardware        Software        Integration     Testing,
design          procurement     installation    & config        training,
                                                                go-live
```

---

## 8. Support & SLAs

### 8.1 Support Levels by Deployment

| Support Aspect | SaaS Multi | SaaS Dedicated | Private Cloud | On-Premise |
|----------------|------------|----------------|---------------|------------|
| **Email support** | ✅ | ✅ | ✅ | ✅ |
| **Phone support** | Pro+ | ✅ | ✅ | ✅ |
| **Response time (P1)** | 4 hours | 1 hour | 2 hours | 4 hours |
| **Response time (P2)** | 8 hours | 4 hours | 4 hours | 8 hours |
| **24/7 support** | ❌ | ✅ | Optional | Optional |
| **Dedicated CSM** | Enterprise+ | ✅ | ✅ | ✅ |
| **Quarterly reviews** | Enterprise+ | ✅ | ✅ | ✅ |

### 8.2 SLA Guarantees

| Deployment | Uptime SLA | Credits |
|------------|------------|---------|
| SaaS Multi-Tenant | 99.9% | 10% per 0.1% below |
| SaaS Dedicated | 99.95% | 10% per 0.05% below |
| Private Cloud | Negotiable | Per contract |
| On-Premise | N/A | Customer responsibility |

---

## 9. Migration Paths

### 9.1 Upgrading Between Deployments

| From | To | Complexity | Downtime |
|------|-----|------------|----------|
| SaaS Multi → SaaS Dedicated | Low | 2-4 hours |
| SaaS Dedicated → Private Cloud | Medium | 4-8 hours |
| SaaS → On-Premise | High | 1-2 days |
| Private Cloud → On-Premise | Medium | 4-8 hours |

### 9.2 Data Portability

All deployments support full data export:
- FHIR R4 format (patients, observations, conditions)
- CSV export (quality measures, care gaps)
- Database dump (PostgreSQL format)
- API bulk export

---

## 10. Recommendations by Organization Type

| Organization Type | Recommended Deployment | Rationale |
|-------------------|------------------------|-----------|
| Solo/small practice | SaaS Multi-Tenant | Simple, affordable |
| Medium practice | SaaS Multi-Tenant | Best value |
| FQHC (single site) | SaaS Multi-Tenant | Budget-friendly |
| FQHC (multi-site) | SaaS Multi-Tenant or Dedicated | Depends on size |
| Small ACO | SaaS Multi-Tenant | Quick start |
| Mid-size ACO | SaaS Dedicated | Better SLA, isolation |
| Large ACO | SaaS Dedicated or Private Cloud | Scale, customization |
| Regional health system | Private Cloud | Control, integration |
| Large health system | Private Cloud or On-Premise | Full control |
| Government/VA | Private Cloud (GovCloud) or On-Premise | Compliance |
| Military/classified | On-Premise (air-gapped) | Security |

---

## Appendix

### A. Frequently Asked Questions

**Q: Can I start with SaaS and move to on-premise later?**
A: Yes. All data can be exported and migrated. We support migration between any deployment options.

**Q: What if my data must stay in a specific country?**
A: Use Private Cloud or On-Premise deployment. Private Cloud supports most major cloud regions worldwide.

**Q: Do you support hybrid deployments?**
A: Yes. For example, you can keep PHI on-premise while using our SaaS for analytics (with de-identified data).

**Q: What about disaster recovery?**
A: All SaaS options include DR. Private Cloud and On-Premise DR is customer responsibility (we provide guidance).

**Q: Can you deploy to our existing Kubernetes cluster?**
A: Yes. Private Cloud can deploy to your existing EKS, AKS, or GKE clusters.

### B. Compliance by Deployment

| Compliance | SaaS Multi | SaaS Dedicated | Private Cloud | On-Premise |
|------------|------------|----------------|---------------|------------|
| HIPAA | ✅ | ✅ | ✅ | ✅ |
| SOC2 | ✅ (Q2 2025) | ✅ | Shared | Customer |
| HITRUST | Roadmap | Roadmap | Customer | Customer |
| FedRAMP | ❌ | ❌ | Possible | Possible |
| StateRAMP | ❌ | ❌ | Possible | Possible |

### C. Contact

For deployment discussions:
- **Sales:** sales@healthdatainmotion.com
- **Technical:** solutions@healthdatainmotion.com
- **Support:** support@healthdatainmotion.com

---

*Deployment Options Version: 1.0*
*Last Updated: December 2025*
*Contact: solutions@healthdatainmotion.com*
