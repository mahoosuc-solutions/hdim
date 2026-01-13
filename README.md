# HealthData-in-Motion

**Enterprise Healthcare Interoperability & Quality Measurement Platform**

[![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?logo=docker)](https://www.docker.com/)
[![FHIR](https://img.shields.io/badge/FHIR-R4-orange)](https://www.hl7.org/fhir/)
[![HIPAA](https://img.shields.io/badge/HIPAA-Compliant-green)](https://www.hhs.gov/hipaa)
[![Tests](https://img.shields.io/badge/Tests-100%25%20Pass-brightgreen)](./backend/docs/PHASE_21_RELEASE_NOTES.md)
[![License](https://img.shields.io/badge/License-Proprietary-red)]()

> **Production-ready, Docker-native healthcare platform** that transforms quality measurement, care gap management, and clinical interoperability into a unified, scalable solution.

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

## 📚 Documentation

### Getting Started
- [Quick Start Guide](./QUICK_START.md) - Deploy in < 10 minutes
- [Installation Guide](./README_DEPLOYMENT.md) - Detailed setup
- [Authentication](./AUTHENTICATION_GUIDE.md) - User management
- [Configuration](./backend/README.md) - Service configuration

### Product & Features
- [Product Overview](./docs/product/overview.md) - Value propositions
- [Feature List](./PRODUCT_FEATURES.md) - Complete capabilities
- [Patient Health](./PATIENT_HEALTH_OVERVIEW_GUIDE.md) - Patient dashboard
- [Quality Measures](./CUSTOM_MEASURES_EXAMPLES.md) - CQL examples

### Architecture & Technical
- [Architecture Overview](./DISTRIBUTION_ARCHITECTURE.md) - System design
- [API Documentation](./BACKEND_API_SPECIFICATION.md) - REST APIs
- [Security Guide](./docs/PRODUCTION_SECURITY_GUIDE.md) - HIPAA compliance
- [Scaling Guide](./DISTRIBUTION_ARCHITECTURE.md#scaling) - Performance tuning

### Sales & Marketing
- [Sales Strategy](./CLINICAL_SALES_STRATEGY.md) - Target personas
- [ROI Calculator](./ROI_CALCULATOR_TEMPLATE.md) - Financial analysis
- [Case Studies](./CASE_STUDY_CLINICAL_IMPACT.md) - Customer success
- [Demo Scripts](./SALES_DEMO_SCRIPT.md) - Product demonstrations

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
# Backend: Run all tests (1,577 tests, 100% pass rate)
cd backend
./gradlew test

# Backend: Run specific service tests
./gradlew :modules:services:quality-measure-service:test

# Backend: Entity-migration validation (JPA ↔ Liquibase sync)
./gradlew test --tests "*EntityMigrationValidationTest"

# Backend: Generate coverage report (JaCoCo)
./gradlew jacocoTestReport
# Report: build/reports/jacoco/test/html/index.html

# Backend: Integration tests
./gradlew integrationTest

# Frontend: Unit tests
npx nx test clinical-portal

# Frontend: E2E tests
npx nx e2e clinical-portal-e2e

# Run all tests across entire platform
./gradlew test && npx nx run-many --target=test --all
```

**Quality Standards:**
- All PRs must maintain ≥99% test pass rate
- Service layer code must have ≥80% coverage
- Entity changes require migration validation
- Zero tolerance for flaky tests

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

## ✅ Quality & Testing

### Test Coverage & Reliability
- **100% Test Pass Rate** - 1,577/1,577 non-skipped tests passing
- **Comprehensive Coverage** - Unit, integration, and E2E tests
- **AI-Assisted Development** - 87% success rate in automated test fixes
- **Zero Flaky Tests** - Deterministic, reliable test execution
- **Continuous Validation** - Automated testing in CI/CD pipeline

### Testing Infrastructure
- **FHIR Mocking** - Isolated E2E tests without external dependencies
- **Gateway Trust Authentication** - Secure test fixtures for RBAC validation
- **Async Testing Patterns** - Proper timing calculations for race-free tests
- **Entity-Migration Validation** - Automatic JPA/Liquibase synchronization checks
- **Performance Testing** - JaCoCo coverage reporting (≥70% target)

### Recent Quality Improvements (Phase 21 - January 2026)
- ✅ Fixed 24 test failures across 6 categories
- ✅ Improved production code (fail-fast error handling)
- ✅ Enhanced test infrastructure (reusable FHIR mocking patterns)
- ✅ Eliminated external test dependencies (faster, deterministic execution)
- ✅ Comprehensive documentation (2,920 lines of release notes)

**Details:** See [Phase 21 Release Notes](./backend/docs/PHASE_21_RELEASE_NOTES.md)

### Quality Metrics
| Metric | Value | Target |
|--------|-------|--------|
| **Test Pass Rate** | 100% (1,577/1,577) | ≥99% |
| **Code Coverage** | ≥70% overall | ≥70% |
| **Service Coverage** | ≥80% service layer | ≥80% |
| **Build Success Rate** | 100% (34/34 services) | 100% |
| **Zero Known Critical Bugs** | ✅ | ✅ |

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

- **Documentation:** [docs/](./docs/)
- **Issues:** [GitHub Issues](https://github.com/your-org/healthdata-in-motion/issues)
- **Discussions:** [GitHub Discussions](https://github.com/your-org/healthdata-in-motion/discussions)
- **Email:** support@healthdata-in-motion.com

### Contributing

We welcome contributions! See [CONTRIBUTING.md](./CONTRIBUTING.md) for guidelines.

### Commercial Support

For production deployments, SLAs, and custom development:
- **Email:** enterprise@healthdata-in-motion.com
- **Phone:** 1-800-HEALTHCARE
- **Website:** https://healthdata-in-motion.com

---

## 📈 Roadmap

### Q1 2026
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
- ✅ **Quality-First** - 100% test pass rate, production-proven reliability

### vs. Build-Your-Own
- ✅ **Proven Architecture** - Production-tested
- ✅ **Complete Solution** - All quality measures included
- ✅ **Maintained & Supported** - Regular updates, patches
- ✅ **Time to Market** - Deploy in days, not months
- ✅ **Enterprise Testing** - Comprehensive test suite, CI/CD ready

### vs. SaaS-Only Solutions
- ✅ **Data Ownership** - Your data, your infrastructure
- ✅ **Customizable** - Modify and extend as needed
- ✅ **No Vendor Lock-in** - Standard Docker deployment
- ✅ **Cost Control** - Predictable, transparent costs
- ✅ **Transparent Quality** - Open test results, 100% pass rate verified

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

*Last Updated: January 12, 2026*
*Latest Release: Phase 21 - 100% Test Pass Rate Achievement*
