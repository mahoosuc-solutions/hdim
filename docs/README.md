# HDIM Documentation Portal

Welcome to the HealthData-in-Motion (HDIM) documentation! This is your central hub for finding information about the HDIM platform.

**Last Updated**: February 3, 2026
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
- [Event Sourcing Architecture](architecture/EVENT_SOURCING_ARCHITECTURE.md) ✨ NEW - CQRS pattern & event services
- [Gateway Architecture](architecture/GATEWAY_ARCHITECTURE.md) ✨ NEW - 4-gateway modularization
- [Service Dependencies](services/DEPENDENCY_MAP.md) - How services interact
- [Flow Diagrams](architecture/ROUND_TRIP_FLOWS.md) - Request flows through the system
- [Technology Decisions](architecture/decisions/) - Architecture decision records (21 ADRs)

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
- **Event Sourcing**: [CQRS pattern, event services, projections](architecture/EVENT_SOURCING_ARCHITECTURE.md) ✨ NEW
- **Gateway Design**: [Modularized 4-gateway architecture](architecture/GATEWAY_ARCHITECTURE.md) ✨ NEW
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
- **Docker Compose**: [18 configs, profiles, usage guide](../docker/README.md) ✨ NEW
- **Local Deployments**: [Local and staging setup](deployment/DOCKER_COMPOSE.md)
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
| [Investor Documentation](https://github.com/webemo-aaron/hdim-investor) 💼 | **Investor Package** - Pitch deck, due diligence, technical analysis |

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

## Architecture Patterns & Frameworks

### Pattern Decision Framework

Use this table to understand which architectural pattern to use for your feature:

| Pattern | Use When | Avoid When | Example Services | Guide |
|---------|----------|-----------|------------------|-------|
| **CRUD** | Simple create-read-update-delete operations with straightforward state | Complex state transitions, audit trails needed, or temporal queries required | patient-service, consent-service | [System Architecture](architecture/SYSTEM_ARCHITECTURE.md) |
| **Event Sourcing + CQRS** | Need immutable event log, audit trail, replay capability, or temporal analysis | Simple CRUD operations without complex state history | patient-event-service, quality-measure-event-service, care-gap-event-service, clinical-workflow-event-service | [Event Sourcing Architecture](architecture/EVENT_SOURCING_ARCHITECTURE.md) ✨ |
| **Gateway Pattern** | Need centralized authentication, routing, rate limiting, or API aggregation | Simple direct service-to-service communication only | gateway-service, gateway-admin-service, gateway-clinical-service, gateway-fhir-service | [Gateway Architecture](architecture/GATEWAY_ARCHITECTURE.md) ✨ |
| **Microservice Pattern** | Independent, loosely-coupled services with own databases | Tight coupling required or very simple monolithic application | All 51+ HDIM services | [System Architecture](architecture/SYSTEM_ARCHITECTURE.md) |
| **Repository Pattern** | Data access abstraction, multiple data sources, or testing | Simple single-database access with no abstraction needs | All services' persistence layers | [Coding Standards](../backend/docs/CODING_STANDARDS.md) |

### Event Sourcing Deep Dive

**When to use Event Sourcing:**
✅ Financial/healthcare transactions (audit trail critical)
✅ Need to answer "what happened?" questions
✅ Temporal queries (state at any point in time)
✅ Event-driven microservices
✅ CQRS read/write separation needed

**When NOT to use Event Sourcing:**
❌ Simple CRUD without audit requirements
❌ No need for event replay
❌ Performance-critical with simple queries
❌ Complex concurrent writes without eventual consistency

**HDIM Event Services** (Phase 5 - Oct 2025 - Jan 2026):
- **patient-event-service** - Patient lifecycle events
- **quality-measure-event-service** - Measure evaluation events
- **care-gap-event-service** - Care gap detection events
- **clinical-workflow-event-service** - Workflow orchestration events

### Gateway Pattern Deep Dive

**Why HDIM Split Gateway** (January 2026):
✅ Domain-driven design (Admin, Clinical, FHIR each have specialized needs)
✅ Independent scaling (Clinical gets high traffic, Admin moderate)
✅ Separate security policies (Admin stricter, Clinical optimized for UX)
✅ Code reuse (gateway-core eliminates duplication)

**4 Gateway Services:**
| Gateway | Port | Use Case | Specialization |
|---------|------|----------|-----------------|
| gateway-admin-service | 8002 | Configuration, user management, audit | Tenant config, approval workflows |
| gateway-clinical-service | 8003 | Patient data, measures, care gaps | Clinical workflows, high throughput |
| gateway-fhir-service | 8004 | FHIR-compliant data exchange | HL7 standards, external EHR integration |
| gateway-service | 8001 | General-purpose routing | Legacy, fallback |

### Choosing Your Pattern

**Decision Tree:**

```
Start: New Feature
  │
  ├─ "Do I need immutable event log?"
  │  └─ YES → Event Sourcing (+ CQRS for queries)
  │           See: EVENT_SOURCING_ARCHITECTURE.md
  │  └─ NO  → Continue
  │
  ├─ "Is this data exposed via APIs?"
  │  └─ YES → Use Gateway Pattern
  │           See: GATEWAY_ARCHITECTURE.md
  │  └─ NO  → Direct service communication (rare)
  │
  ├─ "Is this a new service?"
  │  └─ YES → Use Microservice Pattern
  │           - Own database
  │           - Independent deployments
  │           - Use Event Sourcing or CRUD as appropriate
  │  └─ NO  → Enhance existing service
  │
  └─ "Simple CRUD operations?"
     └─ YES → Standard Spring Boot CRUD
              - Repository pattern for data access
              - See: CODING_STANDARDS.md
     └─ NO  → Evaluate other patterns
```

### Architectural Decision Records (ADRs)

HDIM uses Architecture Decision Records to document why specific patterns were chosen:

- **ADR-001**: Chose Event Sourcing for clinical event services
- **ADR-002**: Chose gateway modularization for separation of concerns
- **ADR-003**: Chose Kafka for event streaming
- See [complete ADR list](architecture/decisions/) for all decisions

### Pattern Evolution in HDIM

| Phase | Pattern | Services Affected | Rationale |
|-------|---------|-------------------|-----------|
| **Phase 1** | CRUD + Direct Services | All initial services | Foundation, simple operations |
| **Phase 2** | Added Repository Pattern | All services | Data access abstraction |
| **Phase 3** | Liquibase Migrations | All services | Schema version control |
| **Phase 4** | Event Sourcing Architecture | New event services | Audit trail, temporal queries, replay |
| **Phase 5** | 4 Specialized Gateways | API Layer | Domain separation, independent scaling |
| **Phase 6+** | CQRS Queries | Event services | Denormalized read models |

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
