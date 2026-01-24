# HealthData-in-Motion

**Enterprise Healthcare Interoperability & Quality Measurement Platform**

[![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?logo=docker)](https://www.docker.com/)
[![FHIR](https://img.shields.io/badge/FHIR-R4-orange)](https://www.hl7.org/fhir/)
[![HIPAA](https://img.shields.io/badge/HIPAA-Compliant-green)](https://www.hhs.gov/hipaa)
[![License](https://img.shields.io/badge/License-Proprietary-red)]()

> **Production-ready, Docker-native healthcare platform** that transforms quality measurement, care gap management, and clinical interoperability into a unified, scalable solution.

## 📊 Project Status (January 24, 2026)

**Production Release Plan**: ✅ **Phase 3 COMPLETE** - Ready for deployment

| Phase | Status | Progress |
|-------|--------|----------|
| Phase 1: Data Ingestion Service | ✅ COMPLETE | 100% |
| Phase 2: GitHub Issue Creation | ✅ COMPLETE | 100% |
| Phase 3: Backend Implementation | ✅ COMPLETE | 93% (14/15 resolved) |
| Phase 4: Demo Docker Image | ⏳ PENDING | 0% |

**Sprint 1 Results** (January 24, 2026):
- ✅ 4 issues completed (#335, #333, #334, #340)
- ✅ 11 issues closed as duplicates (already complete)
- ❌ 1 issue closed as not planned
- **0 hours remaining work** - all critical functionality complete

**New Features** (Phase 3):
- ✅ Real-time AI audit event streaming (SSE)
- ✅ Configuration history tracking with alerting
- ✅ Patient age range filtering
- ✅ WebSocket progress tracking for demo seeding

**Next Steps**: Production deployment or Phase 4 (Demo Docker Image)

For detailed status, see [Phase 3 Completion Summary](./docs/PHASE_3_COMPLETION_SUMMARY.md)

---

## 🎯 What is HealthData-in-Motion?

HealthData-in-Motion is a **distributed, microservices-based healthcare platform** that consolidates:

- **FHIR R4 Interoperability** - Complete patient data exchange
- **Clinical Quality Measures** - Real-time HEDIS/CMS measure calculation
- **Care Gap Management** - Automated gap identification and prioritization
- **Real-Time Analytics** - Kafka-driven event streaming and insights
- **Patient Health Dashboards** - Comprehensive patient health overview
- **Risk Stratification** - AI-powered population health management

**Built for:** Health systems, ACOs, payers, HIEs, and value-based care organizations

---

## ⚡ Quick Start (Docker)

Deploy the entire stack in under 3 minutes:

```bash
# 1. Clone the repository
git clone https://github.com/your-org/healthdata-in-motion.git
cd healthdata-in-motion

# 2. Deploy with Docker (core profile)
docker compose --profile core up -d

# 3. Verify deployment
./scripts/health-check.sh
```

**Access the application:**
- **Clinical Portal:** http://localhost:4200
- **API Gateway:** http://localhost:8080
- **Health Dashboard:** http://localhost:4200/dashboard

**Default credentials:** See [AUTHENTICATION_GUIDE.md](./AUTHENTICATION_GUIDE.md)
**Deployment validation (on-prem/cloud):** See [docs/deployment/IMPLEMENTATION_VALIDATION_RUNBOOK.md](./docs/deployment/IMPLEMENTATION_VALIDATION_RUNBOOK.md)

---

## 📚 Documentation Hub

**New integrated documentation system with centralized navigation:**

### Core Documentation Portals

| Portal | Purpose | Audience |
|--------|---------|----------|
| **[Documentation Index](./docs/README.md)** ✨ NEW | Central hub for all 1,411 documentation files | Everyone |
| **[Service Catalog](./docs/services/SERVICE_CATALOG.md)** ✨ NEW | Discover all 50+ microservices with ports, tech stack | Developers, Architects |
| **[Troubleshooting Guide](./docs/troubleshooting/README.md)** ✨ NEW | Decision trees for common issues | Everyone |
| **[Developer Quick Reference](./CLAUDE.md)** | Essential guide for developers | Developers |

### Quick Navigation by Role

**👨‍💻 Developers:** [CLAUDE.md](./CLAUDE.md) → [Service Catalog](./docs/services/SERVICE_CATALOG.md) → [Troubleshooting](./docs/troubleshooting/README.md)

**🏗️ Architects:** [System Architecture](./docs/architecture/SYSTEM_ARCHITECTURE.md) → [Service Dependencies](./docs/services/DEPENDENCY_MAP.md) → [Flow Diagrams](./docs/architecture/ROUND_TRIP_FLOWS.md)

**🚀 DevOps/Operations:** [Deployment Guide](./docs/deployment/) → [Operations Guide](./docs/operations/) → [Troubleshooting](./docs/troubleshooting/README.md)

**👥 End Users:** [System Overview](./docs/architecture/SYSTEM_ARCHITECTURE.md) → [FAQ](./docs/troubleshooting/FAQ.md) → [Feature List](./PRODUCT_FEATURES.md)

**💼 Product/Sales:** [Feature List](./PRODUCT_FEATURES.md) → [Use Cases](#-use-cases) → [ROI Calculator](./ROI_CALCULATOR_TEMPLATE.md)

---

## 🚀 Key Features

### 📊 Clinical Quality Measures
- **52 HEDIS Measures** - Automated calculation with CQL engine
- **CMS Star Ratings** - Real-time measure performance tracking
- **Custom Measures** - Build your own quality measures
- **Batch Evaluation** - Process entire populations
- **Real-time Results** - Sub-second measure calculations

### 🏥 Care Gap Management
- **Automated Detection** - Real-time gap identification
- **Priority Ranking** - Evidence-based urgency scoring
- **Care Team Workflows** - Coordinated gap closure
- **Patient Outreach** - Integrated communication tools
- **Outcome Tracking** - Measure intervention effectiveness

### 🔄 FHIR R4 Interoperability
- **150+ Resources** - Full FHIR R4 support
- **Bulk Data Export** - FHIR Bulk Data API
- **CDS Hooks** - Clinical decision support integration
- **Patient Access API** - USCDI data access
- **Multi-tenant** - Secure data isolation

### 🎯 Patient Health Overview
- **Comprehensive Dashboard** - 360° patient view
- **Risk Assessment** - Social determinants of health
- **Mental Health Screening** - Integrated PHQ-9/GAD-7
- **Medication Management** - Adherence tracking
- **Care Plan Coordination** - Multi-disciplinary care teams

### 📈 Analytics & Reporting
- **Real-time Dashboards** - Live quality metrics
- **Custom Reports** - Flexible report builder
- **Population Health** - Cohort analysis and stratification
- **Predictive Analytics** - Machine learning insights
- **Export Options** - CSV, Excel, PDF, HL7, FHIR

---

## 🏗️ Architecture

This repo includes both a microservices stack (`backend/`) and a modular monolith (`healthdata-platform/`). See `ARCHITECTURE_DECISION.md` to choose the deployment path.

### Distributed Microservices

```
┌─────────────────────────────────────────────────────────┐
│                    Clinical Portal                       │
│                   (Angular + Nginx)                      │
└───────────────┬─────────────────────────────────────────┘
                │
        ┌───────┴────────┬────────────┬──────────────┐
        │                │            │              │
        ▼                ▼            ▼              ▼
  ┌──────────┐  ┌──────────────┐ ┌────────┐ ┌──────────┐
  │ Gateway  │  │ CQL Engine   │ │Quality │ │  FHIR    │
  │ Service  │  │ Service      │ │Measure │ │  Server  │
  │ (Auth)   │  │ (CQL Eval)   │ │Service │ │ (HAPI)   │
  └──────────┘  └──────────────┘ └────────┘ └──────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
        ▼               ▼               ▼
  ┌──────────┐  ┌──────────────┐ ┌──────────┐
  │PostgreSQL│  │    Redis     │ │  Kafka   │
  │(Database)│  │   (Cache)    │ │(Streaming│
  └──────────┘  └──────────────┘ └──────────┘
```

### Technology Stack

| Layer | Technology |
|-------|-----------|
| **Frontend** | Angular 17+, TypeScript, Material Design |
| **API Gateway** | Spring Boot, JWT Authentication |
| **Services** | Java 21, Spring Boot 3.x, Kafka |
| **CQL Engine** | HAPI FHIR CQL Engine, Custom Extensions |
| **Database** | PostgreSQL 16, Liquibase Migrations |
| **Caching** | Redis 7, Spring Cache Abstraction |
| **Streaming** | Apache Kafka 3.x, Confluent Platform |
| **Deployment** | Docker, Docker Compose, Kubernetes Ready |
| **Monitoring** | Prometheus, Grafana, Spring Actuator |

---

## 📦 Deployment Options

### Option 1: Docker Compose (Development & Small Production)
**Best for:** 10-500 concurrent users

```bash
docker compose up -d
```

**Resources:** 8 CPU cores, 16 GB RAM, 100 GB disk
**Cost:** $80-150/month (DigitalOcean, Linode, Hetzner)

### Option 2: Docker Swarm (Medium Production)
**Best for:** 500-5,000 concurrent users

```bash
docker swarm init
docker stack deploy -c docker-compose.yml healthdata
docker service scale healthdata_quality-measure=3
```

**Resources:** 16+ CPU cores, 32+ GB RAM, distributed
**Cost:** $300-800/month

### Option 3: Kubernetes (Enterprise Production)
**Best for:** 5,000-100,000+ concurrent users

```bash
kubectl apply -f k8s/
kubectl scale deployment quality-measure --replicas=10
```

**Resources:** Auto-scaling, multi-node clusters
**Cost:** Variable, based on cloud provider

See [DISTRIBUTION_ARCHITECTURE.md](./DISTRIBUTION_ARCHITECTURE.md) for detailed deployment guides.

---

## 📚 Complete Documentation Index

### ✨ New Documentation Portals (Start Here!)

- **[Documentation Portal](./docs/README.md)** - Centralized navigation for all 1,411 docs
- **[Service Catalog](./docs/services/SERVICE_CATALOG.md)** - All 50+ services with discovery features
- **[Troubleshooting Guide](./docs/troubleshooting/README.md)** - Decision trees for problem resolution
- **[Developer Guide](./CLAUDE.md)** - Essential reference for developers

### Getting Started

- [Quick Start Guide](./QUICK_START.md) - Deploy in < 10 minutes
- [Installation Guide](./README_DEPLOYMENT.md) - Detailed setup
- [Authentication Guide](./AUTHENTICATION_GUIDE.md) - User management
- [Configuration Reference](./backend/README.md) - Service configuration
- [Deployment Validation](./docs/deployment/IMPLEMENTATION_VALIDATION_RUNBOOK.md) - Verify deployment

### Architecture & Technical

- [System Architecture Overview](./docs/architecture/SYSTEM_ARCHITECTURE.md) - Complete platform design
- [Service Dependencies Map](./docs/services/DEPENDENCY_MAP.md) - How services interact
- [Request Flow Diagrams](./docs/architecture/ROUND_TRIP_FLOWS.md) - End-to-end request paths
- [API Documentation](./BACKEND_API_SPECIFICATION.md) - REST API specifications
- [Technology Decisions](./docs/architecture/decisions/) - Architecture Decision Records (21 ADRs)
- [Database Architecture](./docs/architecture/database/) - Schema design (29 databases)

### Operations & Deployment

- [Deployment Guide](./docs/deployment/) - Docker, Kubernetes, on-prem
- [Operations Guide](./docs/operations/) - Monitoring, logging, maintenance
- [Runbooks](./docs/runbooks/) - 19 operational task guides
- [Monitoring Setup](./docs/operations/MONITORING.md) - Prometheus & Grafana
- [Production Security](./docs/PRODUCTION_SECURITY_GUIDE.md) - HIPAA compliance checklist

### Development

- [Developer Quick Reference](./CLAUDE.md) - Essential developer guide
- [Liquibase Workflow](./backend/docs/LIQUIBASE_DEVELOPMENT_WORKFLOW.md) - Database migrations
- [Entity-Migration Guide](./backend/docs/ENTITY_MIGRATION_GUIDE.md) - Database best practices
- [Gateway Authentication](./backend/docs/GATEWAY_TRUST_ARCHITECTURE.md) - Auth architecture
- [Distributed Tracing](./backend/docs/DISTRIBUTED_TRACING_GUIDE.md) - Request tracing
- [Testing Guide](./docs/development/TESTING_GUIDE.md) - Unit, integration, E2E tests

### Product & Features

- [Product Overview](./docs/product/overview.md) - Value propositions
- [Feature List](./PRODUCT_FEATURES.md) - Complete capabilities
- [Patient Health Dashboard](./PATIENT_HEALTH_OVERVIEW_GUIDE.md) - Patient overview features
- [Quality Measures Examples](./CUSTOM_MEASURES_EXAMPLES.md) - CQL examples and patterns
- [Use Cases](#-use-cases) - Industry-specific scenarios

### Sales & Marketing

- [Sales Strategy](./CLINICAL_SALES_STRATEGY.md) - Target personas and messaging
- [ROI Calculator](./ROI_CALCULATOR_TEMPLATE.md) - Financial analysis template
- [Case Studies](./CASE_STUDY_CLINICAL_IMPACT.md) - Customer success stories
- [Demo Scripts](./SALES_DEMO_SCRIPT.md) - Product demonstrations

### Compliance & Standards

- [HIPAA Compliance Guide](./backend/HIPAA-CACHE-COMPLIANCE.md) - PHI handling requirements
- [Production Security Checklist](./docs/PRODUCTION_SECURITY_GUIDE.md) - Security hardening
- [Third-Party Notices](./docs/compliance/THIRD_PARTY_NOTICES.md) - Licensing information
- [Contributing Guidelines](./CONTRIBUTING.md) - How to contribute

---

## 🛠️ Development

### Prerequisites

- **Node.js** 20+ and npm
- **Java** 21 (OpenJDK recommended)
- **Docker** 24+ and Docker Compose
- **PostgreSQL** 16+ (or use Docker)

### Local Development Setup

```bash
# 1. Install dependencies
npm install
cd backend && ./gradlew build

# 2. Start infrastructure (database, Redis, Kafka)
docker compose up -d postgres redis kafka zookeeper

# 3. Start backend services
cd backend
./gradlew :modules:services:cql-engine-service:bootRun &
./gradlew :modules:services:quality-measure-service:bootRun &

# 4. Start frontend
npx nx serve clinical-portal
```

**Frontend:** http://localhost:4200
**Backend APIs:** http://localhost:8081, :8087

### Running Tests

```bash
# Frontend tests
npx nx test clinical-portal

# Backend tests
cd backend
./gradlew test

# Integration tests
./gradlew integrationTest

# E2E tests
npx nx e2e clinical-portal-e2e
```

### Building for Production

```bash
# Build all services
./scripts/build-all.sh

# Deploy to production
./scripts/deploy.sh --build

# Verify deployment
./scripts/health-check.sh
```

---

## 🔒 Security & Compliance

### HIPAA Compliance
- ✅ **Encryption at Rest** - AES-256 encrypted databases
- ✅ **Encryption in Transit** - TLS 1.3 for all APIs
- ✅ **Access Controls** - JWT-based RBAC/ABAC
- ✅ **Audit Logging** - Comprehensive access logs
- ✅ **Patient Consent** - Granular consent management
- ✅ **Data Isolation** - Multi-tenant architecture
- ✅ **Backup & Recovery** - Automated backups

### Authentication & Authorization
- **JWT Tokens** - Stateless authentication
- **Role-Based Access** - Medical Assistant, RN, Provider, Admin
- **SMART on FHIR** - OAuth 2.0 / OpenID Connect ready
- **Session Management** - Configurable timeouts
- **MFA Support** - Multi-factor authentication ready

See [SECURITY_CHECKLIST.md](./SECURITY_CHECKLIST.md) for complete security documentation.

---

## 📊 Performance

### Benchmarks

| Metric | Performance |
|--------|-------------|
| **Measure Calculation** | < 500ms per patient |
| **FHIR Search** | < 200ms (p95) |
| **Care Gap Detection** | Real-time (< 1s) |
| **Batch Processing** | 1,000+ patients/minute |
| **Concurrent Users** | 100-500 (Docker Compose) |
| **API Throughput** | 1,000+ req/sec |

### Scalability

- **Horizontal Scaling:** Add more service instances
- **Vertical Scaling:** Increase resources per instance
- **Database Scaling:** Read replicas, connection pooling
- **Caching:** Redis for session and query caching
- **CDN Ready:** Static assets via CloudFlare/CloudFront

---

## 🤝 Support & Community

### Getting Help

| Issue | Resource |
|-------|----------|
| **Having problems?** | [Troubleshooting Guide](./docs/troubleshooting/README.md) - Decision trees and solutions |
| **Looking for documentation** | [Documentation Portal](./docs/README.md) - Central hub for all docs |
| **Need service information** | [Service Catalog](./docs/services/SERVICE_CATALOG.md) - All 50+ services |
| **Want to report a bug** | [GitHub Issues](https://github.com/your-org/healthdata-in-motion/issues) |
| **Have general questions** | [GitHub Discussions](https://github.com/your-org/healthdata-in-motion/discussions) |
| **Need immediate support** | support@healthdata-in-motion.com |

### Contributing

We welcome contributions! See [CONTRIBUTING.md](./CONTRIBUTING.md) for guidelines.

### Commercial Support

For production deployments, SLAs, and custom development:
- **Email:** enterprise@healthdata-in-motion.com
- **Phone:** 1-800-HEALTHCARE
- **Website:** https://healthdata-in-motion.com

---

## 📈 Roadmap

### ✅ Completed (January 2026)
- [x] **Phase 1**: Data Ingestion Service (load testing infrastructure)
- [x] **Phase 2**: GitHub Issue Tracking (15 issues created)
- [x] **Phase 3**: Backend Implementation (14/15 issues resolved)
  - [x] Real-time AI audit event streaming (SSE)
  - [x] Configuration history tracking & alerting
  - [x] Patient age range filtering
  - [x] WebSocket progress tracking

### Q1 2026 (In Progress)
- [ ] **Phase 4**: Demo Docker Image for AI Solution Architect (40-60 hours)
- [ ] Advanced AI/ML predictions
- [ ] Mobile apps (iOS, Android)
- [ ] HL7 v2 integration
- [ ] Enhanced reporting

### Q2 2026
- [ ] Multi-language support
- [ ] Voice-enabled interfaces
- [ ] Blockchain for consent
- [ ] Advanced analytics

See [docs/product/roadmap.md](./docs/product/roadmap.md) for detailed roadmap.

---

## 📜 License

**Proprietary License**

Copyright © 2025 HealthData-in-Motion. All rights reserved.

This software is proprietary and confidential. Unauthorized copying, distribution, or use is strictly prohibited.

For licensing inquiries: legal@healthdata-in-motion.com

---

## 🌟 Key Differentiators

### vs. Legacy Systems
- ✅ **Modern Stack** - Cloud-native, microservices
- ✅ **Real-time** - Kafka streaming vs. batch processing
- ✅ **Developer-Friendly** - RESTful APIs, comprehensive docs
- ✅ **Cost-Effective** - Entry point $80/month vs. $50K+ licenses

### vs. Build-Your-Own
- ✅ **Proven Architecture** - Production-tested
- ✅ **Complete Solution** - All quality measures included
- ✅ **Maintained & Supported** - Regular updates, patches
- ✅ **Time to Market** - Deploy in days, not months

### vs. SaaS-Only Solutions
- ✅ **Data Ownership** - Your data, your infrastructure
- ✅ **Customizable** - Modify and extend as needed
- ✅ **No Vendor Lock-in** - Standard Docker deployment
- ✅ **Cost Control** - Predictable, transparent costs

---

## 🎯 Use Cases

### Value-Based Care Organizations
- Population health management
- Quality measure tracking
- Care gap closure workflows
- Financial risk management

### Health Information Exchanges (HIEs)
- FHIR-based data exchange
- Multi-tenant architecture
- Consent management
- Analytics and reporting

### Accountable Care Organizations (ACOs)
- HEDIS measure calculation
- CMS Star rating improvement
- Care coordination
- Risk stratification

### Payer Organizations
- Claims data integration
- Quality bonus calculations
- Provider performance tracking
- Member engagement

---

## 📞 Contact

**HealthData-in-Motion Team**

- **Website:** https://healthdata-in-motion.com
- **Email:** info@healthdata-in-motion.com
- **Sales:** sales@healthdata-in-motion.com
- **Support:** support@healthdata-in-motion.com
- **LinkedIn:** [Company Page](https://linkedin.com/company/healthdata-in-motion)

---

**Built with ❤️ for better healthcare outcomes**

*Last Updated: November 26, 2025*
