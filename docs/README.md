# HDIM Documentation Portal

Welcome to the HealthData-in-Motion (HDIM) documentation! This is your central hub for finding information about the HDIM platform.

**Last Updated**: January 19, 2026
**Total Documentation Files**: 1,411 markdown files organized across architecture, services, operations, and user guides

---

## Quick Navigation

### Getting Started (5-15 minutes)
- **New to HDIM?** Start here → [Developer Quick Reference](../CLAUDE.md)
- **Setting up locally?** See Local Development section in [CLAUDE.md](../CLAUDE.md)
- **First deployment?** See Docker section in [CLAUDE.md](../CLAUDE.md)
- **Common tasks?** See Common Commands in [CLAUDE.md](../CLAUDE.md)

### By Role

**👨‍💻 Developers**
- [Developer Quick Reference](../CLAUDE.md) - Essential guide for all developers
- [Backend Technical Guides](../backend/docs/README.md) ✨ NEW - Build, Database, Code, Commands
- [Architecture Decision Records](architecture/decisions/) - Why we chose specific technologies
- [API Reference](api/) - OpenAPI specs for all services
- [Service Catalog](services/SERVICE_CATALOG.md) - All 50+ microservices

**🏗️ Architects & Leads**
- [System Architecture](architecture/SYSTEM_ARCHITECTURE.md) - Complete platform overview
- [Service Dependencies](services/DEPENDENCY_MAP.md) - How services interact
- [Flow Diagrams](architecture/ROUND_TRIP_FLOWS.md) - Request flows through the system

**🚀 DevOps & Operations**
- [Deployment Guide](deployment/) - Docker, Kubernetes, production setup
- [Runbooks](runbooks/) - Step-by-step guides for operational tasks
- [Operations Guide](operations/) - Monitoring, logging, backup, troubleshooting

**👥 End Users**
- [System Architecture Overview](architecture/SYSTEM_ARCHITECTURE.md) - Understand what HDIM does
- [FAQ](troubleshooting/FAQ.md) - Answers to common questions

---

## By Topic

### Architecture & Design
- **System Overview**: [Complete architecture](architecture/SYSTEM_ARCHITECTURE.md)
- **Flow Diagrams**: [Request flows and interactions](architecture/ROUND_TRIP_FLOWS.md)
- **Platform Flows**: [High-level business processes](architecture/PLATFORM_FLOW_OVERVIEW.md)
- **Technology Decisions**: [21 Architecture Decision Records](architecture/decisions/)
- **Database Architecture**: [29 databases, schema management](architecture/database/)

### Services
- **Service Catalog**: [All 50+ microservices](services/SERVICE_CATALOG.md) ✨ NEW
- **Service Ports**: [Port mappings and endpoints](services/PORT_REFERENCE.md)
- **Service Dependencies**: [How services interact](services/DEPENDENCY_MAP.md)

### APIs & Integration
- **API Catalog**: [OpenAPI specs for all services](api/)
- **Authentication**: [Gateway Trust Architecture](../backend/docs/GATEWAY_TRUST_ARCHITECTURE.md)
- **Integration Guide**: [External system integration](development/INTEGRATION_GUIDE.md)

### Database
- **Database Architecture**: [Schema design and evolution](../backend/docs/DATABASE_ARCHITECTURE_GUIDE.md) ✨ NEW
- **Migrations**: [Liquibase workflow](../backend/docs/LIQUIBASE_DEVELOPMENT_WORKFLOW.md) ⭐ CRITICAL
- **Entity-Migration Sync**: [Keeping entities in sync](../backend/docs/ENTITY_MIGRATION_GUIDE.md)
- **Schema Management**: [Liquibase migrations, rollbacks, troubleshooting](../backend/docs/DATABASE_ARCHITECTURE_GUIDE.md)

### Security & Compliance
- **HIPAA Compliance**: [Protected Health Information handling](../backend/HIPAA-CACHE-COMPLIANCE.md)
- **Authentication**: [User authentication and authorization](../backend/docs/GATEWAY_TRUST_ARCHITECTURE.md)
- **Production Hardening**: [Security checklist for production](../docs/PRODUCTION_SECURITY_GUIDE.md)

### Development
- **Quick Reference**: [Developer Quick Reference](../CLAUDE.md) - Start here!
- **Coding Standards**: [Code patterns and conventions](../backend/docs/CODING_STANDARDS.md) ✨ NEW
- **Command Reference**: [Gradle, Docker, PostgreSQL commands](../backend/docs/COMMAND_REFERENCE.md) ✨ NEW
- **Testing Guide**: [Unit, integration, and E2E testing](development/TESTING_GUIDE.md)
- **Debugging**: [Tools and techniques for debugging](development/DEBUGGING.md)
- **Distributed Tracing**: [Understanding request flows with Jaeger](../backend/docs/DISTRIBUTED_TRACING_GUIDE.md)

### Deployment & Operations
- **Build Management**: [Build commands, strategies, troubleshooting](../backend/docs/BUILD_MANAGEMENT_GUIDE.md) ✨ NEW
- **Docker Compose**: [Local and staging deployments](deployment/DOCKER_COMPOSE.md)
- **Kubernetes**: [Production deployment](deployment/KUBERNETES.md)
- **Production Checklist**: [Pre-production validation](deployment/PRODUCTION_CHECKLIST.md)
- **Monitoring**: [Prometheus and Grafana setup](operations/MONITORING.md)
- **Logging**: [Log aggregation and analysis](operations/LOGGING.md)

### Troubleshooting
- **Troubleshooting Index**: [Decision tree for common issues](troubleshooting/) ✨ NEW
- **FAQ**: [Frequently asked questions](troubleshooting/FAQ.md)
- **Service Runbooks**: [19 operational runbooks](runbooks/)

---

## Root-Level Critical Documents

| Document | Purpose |
|----------|---------|
| [CLAUDE.md](../CLAUDE.md) | **Developer Quick Reference** - Everything you need to know |
| [README.md](../README.md) | **Project Overview** - High-level project information |
| [CONTRIBUTING.md](../CONTRIBUTING.md) | **Contribution Guidelines** - How to contribute |
| [CHANGELOG.md](../CHANGELOG.md) | **Release History** - What changed in each release |

---

## Backend Technical Documentation

In-depth technical guides for backend development:

| Document | Purpose |
|----------|---------|
| [HIPAA Compliance](../backend/HIPAA-CACHE-COMPLIANCE.md) | **CRITICAL** - PHI handling requirements |
| [Gateway Trust Architecture](../backend/docs/GATEWAY_TRUST_ARCHITECTURE.md) | Authentication design |
| [Distributed Tracing Guide](../backend/docs/DISTRIBUTED_TRACING_GUIDE.md) | Request tracing with Jaeger |
| [Entity-Migration Sync](../backend/docs/ENTITY_MIGRATION_GUIDE.md) | Database entity patterns |
| [Liquibase Workflow](../backend/docs/LIQUIBASE_DEVELOPMENT_WORKFLOW.md) | Database migration best practices |

---

## Service Documentation

Each of the 50+ microservices has detailed documentation in its README:

### Core Services
- [quality-measure-service](../backend/modules/services/quality-measure-service/README.md) - HEDIS measures and CQL
- [patient-service](../backend/modules/services/patient-service/README.md) - Patient demographics
- [cql-engine-service](../backend/modules/services/cql-engine-service/README.md) - CQL evaluation
- [fhir-service](../backend/modules/services/fhir-service/README.md) - FHIR R4 resources
- [care-gap-service](../backend/modules/services/care-gap-service/README.md) - Care gap detection

### All Services
→ [Service Catalog](services/SERVICE_CATALOG.md) ✨ Complete list with links and descriptions

---

## Key Statistics

- **Services**: 50+ microservices (all documented)
- **Databases**: 29 separate PostgreSQL databases
- **APIs**: 200+ REST endpoints
- **Documentation Files**: 1,411 markdown files (organized and indexed)
- **Architecture Decisions**: 21 documented in ADRs
- **Operational Runbooks**: 19 covering common scenarios

---

## Recent Updates

- **January 19, 2026**: New modular documentation structure with centralized navigation ✨
- **January 12, 2026**: Liquibase Development Workflow guide
- **January 10, 2026**: Database architecture standardization complete
- See [Archive](archive/) for historical documentation

---

## Documentation Map

```
docs/
├── README.md (this file) - Documentation portal
├── architecture/ - System design and decisions
│   ├── SYSTEM_ARCHITECTURE.md - Complete overview
│   ├── PLATFORM_FLOW_OVERVIEW.md - Business processes
│   ├── ROUND_TRIP_FLOWS.md - Request flows
│   ├── decisions/ - 21 Architecture Decision Records
│   └── database/ - 29 database schemas
├── services/ - Microservice documentation
│   ├── SERVICE_CATALOG.md - All 50+ services ✨ NEW
│   ├── PORT_REFERENCE.md - Service ports
│   └── DEPENDENCY_MAP.md - Service interactions
├── api/ - API documentation
├── development/ - Developer guides
├── deployment/ - Deployment procedures
├── operations/ - Operational guides
├── runbooks/ - 19 operational runbooks
├── security/ - Security and compliance
├── user/ - End user documentation
└── troubleshooting/ - Problem resolution
    └── README.md - Decision tree ✨ NEW
```

---

## Getting Help

### Need Quick Answers?
→ [CLAUDE.md](../CLAUDE.md) - Developer reference

### Having Technical Issues?
→ [Troubleshooting Guide](troubleshooting/)

### Looking for a Service?
→ [Service Catalog](services/SERVICE_CATALOG.md)

### Want to Contribute?
→ [CONTRIBUTING.md](../CONTRIBUTING.md)

---

**Last Updated**: January 19, 2026
**Maintained by**: HDIM Platform Team
**Status**: Active - Phase 1 Documentation Reorganization
