# HealthData-in-Motion (HDIM)

**Enterprise Healthcare Interoperability & Quality Measurement Platform**

[![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?logo=docker)](https://www.docker.com/)
[![FHIR](https://img.shields.io/badge/FHIR-R4-orange)](https://www.hl7.org/fhir/)
[![HIPAA](https://img.shields.io/badge/HIPAA-Compliant-green)](https://www.hhs.gov/hipaa)
[![License](https://img.shields.io/badge/License-BSL%201.1-blue)](./LICENSE)

> **Production-ready, Docker-native healthcare platform** that transforms quality measurement, care gap management, and clinical interoperability into a unified, scalable solution.

## License Model

This project is **source-available** under the Business Source License 1.1.
See [LICENSE](./LICENSE) and [NOTICE](./NOTICE) for allowed use and commercial terms.
Production use requires a separate commercial agreement with Grateful House Inc.

- [docs/compliance/BSL_RELEASE_PLAN.md](./docs/compliance/BSL_RELEASE_PLAN.md)
- [docs/compliance/LICENSING-BOUNDARY.md](./docs/compliance/LICENSING-BOUNDARY.md)
- [docs/compliance/THIRD_PARTY_NOTICES.md](./docs/compliance/THIRD_PARTY_NOTICES.md)

## Security Reporting

Report vulnerabilities privately via the process in [SECURITY.md](./SECURITY.md).
Do not open public issues for undisclosed security vulnerabilities.

## What HDIM Provides

- FHIR R4 interoperability services
- Quality measure and care-gap workflows
- Multi-service event processing with Kafka
- Clinical and operations portals
- Deployment patterns for local, staging, and production environments

## Architecture

Primary components:

- `backend/` - Java/Spring service modules
- `apps/` - Web portals and frontends
- `mcp-edge-*` - Node.js edge sidecars and integrations
- `docker-compose*.yml` - Local and environment-specific orchestration
- `docs/` - Architecture, operations, compliance, and release documentation

See [docs/architecture](./docs/architecture) for design records and flow docs.

## Quick Start (Local)

Prerequisites:

- Docker + Docker Compose
- Node.js 20+
- Java 21 (for backend builds/tests)

Run core stack:

```bash
docker compose --profile core up -d
```

Common endpoints (default local):

- API gateway: `http://localhost:8080`
- Clinical portal: `http://localhost:4200`
- Grafana (when enabled): `http://localhost:3001`

## Documentation

- Main docs index: [docs/README.md](./docs/README.md)
- Deployment and runbooks: [docs/deployment](./docs/deployment)
- Clinical portal release path: [docs/release/CLINICAL_PORTAL_RELEASE_CANDIDATE.md](./docs/release/CLINICAL_PORTAL_RELEASE_CANDIDATE.md)
- Troubleshooting: [docs/troubleshooting](./docs/troubleshooting)
- Security policy: [SECURITY.md](./SECURITY.md)
- Contributing: [CONTRIBUTING.md](./CONTRIBUTING.md)
- Developer Quick Reference: [CLAUDE.md](./CLAUDE.md)

## Project Status

Repository is under active development with frequent updates.
For release and validation artifacts, see [docs/releases](./docs/releases).

## HDIM Ecosystem

| Repository | Purpose |
|------------|---------|
| **hdim** (this repo) | Core platform — backend services, API, landing page |
| [hdim-validation](https://github.com/mahoosuc-solutions/hdim-validation) | Validation demo — proves platform capabilities with synthetic FHIR data |
| [hdim-accelerator](https://github.com/mahoosuc-solutions/hdim-accelerator) | Provider starter toolkit — workflow integration templates and portal |

### Built With

| Project | License | Description |
|---------|---------|-------------|
| [ServiceHive](https://github.com/webemo-aaron/servicehive-ai) | Apache 2.0 | Open-source event mesh for AI-agent orchestration — the foundational agent framework powering HDIM's intelligence layer |

**Full changelog:** [CHANGELOG.md](./CHANGELOG.md) | **Release docs:** [docs/releases](./docs/releases)

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
git clone https://github.com/mahoosuc-solutions/hdim.git
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

## 🚀 Q1 2026 Release Materials

### For Investors 💼

**📦 [Investor Documentation Repository](https://github.com/webemo-aaron/hdim-investor)** - Clean, standalone package with all investor materials

**[INVESTOR-PITCH-DECK.md](./docs/INVESTOR-PITCH-DECK.md)** (38 KB, 25 slides)
- Problem statement & market opportunity ($18B TAM)
- HDIM solution overview (51 microservices, FHIR-native)
- Business model & financial projections
- Technology maturity & HIPAA compliance
- Series A funding ask ($5-7M, 9-13x return potential)

**[PHASE-7-FINAL-REPORT.md](./docs/PHASE-7-FINAL-REPORT.md)** (Technical due diligence)

### For Hospitals 🏥

**[HOSPITAL-DEPLOYMENT-GUIDE.md](./docs/HOSPITAL-DEPLOYMENT-GUIDE.md)** (18.5 KB)
- 7-step deployment procedure (~30 min)
- EHR integration patterns (Epic, Cerner)
- HIPAA compliance setup & operational runbooks
- Support SLAs & troubleshooting guide

**[PRODUCTION-READINESS-CHECKLIST.md](./docs/PRODUCTION-READINESS-CHECKLIST.md)** (12.8 KB)
- 95+ infrastructure, security, testing validations
- Hospital go-live checklist with sign-off

### For Everyone

**[RELEASE-2026-Q1-SUMMARY.md](./docs/RELEASE-2026-Q1-SUMMARY.md)** (19.5 KB)
- Complete project summary
- All 7 infrastructure phases documented
- Financial impact & metrics
- Go-to-market timeline

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

**👥 End Users:** [System Overview](./docs/architecture/SYSTEM_ARCHITECTURE.md) → [FAQ](./docs/troubleshooting/FAQ.md) → [Feature List](#-key-features)

**💼 Product/Sales:** [Feature List](#-key-features) → [Use Cases](#-use-cases) → [ROI Calculator](./docs/gtm/ROI_CALCULATOR_SPEC.md)

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

See [Deployment Runbook](./docs/DEPLOYMENT_RUNBOOK.md) for detailed deployment guides.

---

## 📚 Complete Documentation Index

### ✨ New Documentation Portals (Start Here!)

- **[Documentation Portal](./docs/README.md)** - Centralized navigation for all 1,411 docs
- **[Service Catalog](./docs/services/SERVICE_CATALOG.md)** - All 57 services with discovery features
- **[Troubleshooting Guide](./docs/troubleshooting/README.md)** - Decision trees for problem resolution
- **[Developer Guide](./CLAUDE.md)** - Essential reference for developers

### Getting Started

- [Quick Start Guide](./docs/QUICK_START_GUIDE.md) - Deploy in < 10 minutes
- [Installation Guide](./docs/development/LOCAL_SETUP.md) - Detailed setup
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
- [Feature List](#-key-features) - Complete capabilities
- [Clinical Portal User Guide](./docs/CLINICAL_PORTAL_USER_GUIDE.md) - Patient overview features
- [CQL Measure Library](./docs/measures/) - CQL examples and patterns
- [Use Cases](#-use-cases) - Industry-specific scenarios

### Sales & Marketing

- [GTM Strategy](./docs/gtm/) - Target personas, sales collateral, and messaging
- [ROI Calculator](./docs/gtm/ROI_CALCULATOR_SPEC.md) - Financial analysis specification
- [Marketing Content](./docs/marketing/) - Announcements and content index
- [Demo Scripts](./docs/marketing/demo/) - Product demonstrations

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

See [Production Security Guide](./docs/PRODUCTION_SECURITY_GUIDE.md) for complete security documentation.

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
| **Want to report a bug** | [GitHub Issues](https://github.com/mahoosuc-solutions/hdim/issues) |
| **Have general questions** | [GitHub Discussions](https://github.com/mahoosuc-solutions/hdim/discussions) |
| **Need immediate support** | sales@mahoosuc.solutions |

### Contributing

We welcome contributions! See [CONTRIBUTING.md](./CONTRIBUTING.md) for guidelines.

Before opening or merging PRs, also review:

- [PR Template](./.github/pull_request_template.md) - validation and merge-gate checklist
- [CI Branch Protection Checklist](./docs/runbooks/CI_BRANCH_PROTECTION_CHECKLIST.md) - required status checks and branch settings
- [Frontend Session Flow E2E Workflow](./.github/workflows/frontend-session-flow-e2e.yml) - session expiry browser gate on frontend changes
  - Includes both standard and external-auth session-expiry e2e checks.

### Commercial Support

For production deployments, SLAs, and custom development:
- **Email:** sales@mahoosuc.solutions
- **Phone:** 1-800-HEALTHCARE
- **Website:** https://healthdata-in-motion.com

---

## 📈 Roadmap

### ✅ Completed (January–March 2026)
- [x] **Phases 1–7**: Infrastructure modernization (90%+ faster feedback loops)
- [x] **API Documentation**: 62 endpoints with interactive Swagger UI
- [x] **HIE Data Pipeline**: End-to-end EHR/CDR/CMS → FHIR → CQL → care gap detection
- [x] **Contract Testing**: Consumer-driven Pact tests + OpenAPI validation
- [x] **Event Sourcing**: CQRS pattern with 4 event services
- [x] **Gateway Modularization**: 4-gateway architecture with shared gateway-core
- [x] **MCP Edge v0.1.0**: 3 sidecars (platform, devops, clinical), 123 tools, 1,307 tests
- [x] **System Remediation**: Tiers 1-4 (security, CI/CD, landing page, docs)
- [x] **Landing Page v3**: Persona-first design, Vercel deployment, SEO/accessibility
- [x] **Wave-1 Revenue**: Price transparency APIs, ADT handling, assurance runner

### Q2 2026 (Planned)
- [ ] MCP Edge Layer 2: Clinical workflow automation (MA intake, CDS hooks)
- [ ] Advanced AI/ML predictive care gap detection
- [ ] Mobile companion apps (iOS, Android)
- [ ] HL7 v2 bidirectional integration
- [ ] Enhanced reporting and analytics dashboards

See [Year 1 Strategic Roadmap](./docs/YEAR_1_STRATEGIC_ROADMAP.md) for detailed roadmap.

---

## 📜 License

Business Source License 1.1 (BSL 1.1).

See [LICENSE](./LICENSE) and [NOTICE](./NOTICE) for governing terms.

Non-production use (development, testing, evaluation, education) is permitted.
Production use requires a separate commercial agreement with Grateful House Inc.

Compliance references:
- [docs/compliance/BSL_RELEASE_PLAN.md](./docs/compliance/BSL_RELEASE_PLAN.md)
- [docs/compliance/LICENSING-BOUNDARY.md](./docs/compliance/LICENSING-BOUNDARY.md)
- [docs/compliance/THIRD_PARTY_NOTICES.md](./docs/compliance/THIRD_PARTY_NOTICES.md)

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
- **Email:** sales@mahoosuc.solutions
- **Sales:** sales@mahoosuc.solutions
- **Support:** sales@mahoosuc.solutions
- **LinkedIn:** [Company Page](https://linkedin.com/company/healthdata-in-motion)

---

**Built with ❤️ for better healthcare outcomes**

*Last Updated: March 10, 2026*
