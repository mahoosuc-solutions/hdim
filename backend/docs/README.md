# Backend Technical Documentation Index

---
**Navigation:** [Main CLAUDE.md](../../CLAUDE.md) | [Documentation Portal](../../docs/README.md)
---

## 📚 Quick Navigation

### Getting Started
- **New to HDIM?** Start with [CLAUDE.md](../../CLAUDE.md)
- **Questions?** Check [Troubleshooting Guide](../../docs/troubleshooting/README.md)
- **Need a command?** See [Command Reference](./COMMAND_REFERENCE.md)

### Essential Guides (Start Here)

| Guide | Purpose | When to Use |
|-------|---------|------------|
| [Build Management Guide](./BUILD_MANAGEMENT_GUIDE.md) | Building services, Docker images, troubleshooting | Building services, debugging build failures |
| [Database Architecture Guide](./DATABASE_ARCHITECTURE_GUIDE.md) | Schema management, Liquibase migrations, troubleshooting | Creating entities, modifying schemas |
| [Coding Standards](./CODING_STANDARDS.md) | Code patterns, conventions, architecture | Writing service code, following HDIM patterns |
| [Command Reference](./COMMAND_REFERENCE.md) | Gradle, Docker, PostgreSQL commands | Running tests, starting services, querying data |

### Deep-Dive Guides

| Guide | Purpose | Focus Area |
|-------|---------|-----------|
| [Liquibase Development Workflow](./LIQUIBASE_DEVELOPMENT_WORKFLOW.md) ⭐ CRITICAL | Database migration procedures | Daily database development |
| [Gateway Trust Architecture](./GATEWAY_TRUST_ARCHITECTURE.md) | Security & authentication | Authorization & multi-tenancy |
| [Distributed Tracing Guide](./DISTRIBUTED_TRACING_GUIDE.md) | OpenTelemetry setup | Observability & monitoring |
| [Entity Migration Guide](./ENTITY_MIGRATION_GUIDE.md) | JPA/Liquibase synchronization | Entity-migration coupling |
| [Database Migration Runbook](./DATABASE_MIGRATION_RUNBOOK.md) | Operational procedures | Production deployments |
| [HIPAA Cache Compliance](../HIPAA-CACHE-COMPLIANCE.md) | PHI handling requirements | Cache TTL, sensitive data |

---

## 🎯 By Task

### I need to...

#### Build & Deploy
1. **Build a single service** → [Build Management Guide](./BUILD_MANAGEMENT_GUIDE.md) - Golden Rules
2. **Debug build failures** → [Build Management Guide](./BUILD_MANAGEMENT_GUIDE.md) - Troubleshooting
3. **Run all tests** → [Command Reference](./COMMAND_REFERENCE.md) - Testing section
4. **Create a Docker image** → [Build Management Guide](./BUILD_MANAGEMENT_GUIDE.md) - Workflow

#### Database Development
1. **Create a new entity** → [Database Architecture Guide](./DATABASE_ARCHITECTURE_GUIDE.md) - Migration Workflow Scenario 1
2. **Add a column to table** → [Database Architecture Guide](./DATABASE_ARCHITECTURE_GUIDE.md) - Scenario 2
3. **Troubleshoot schema issues** → [Database Architecture Guide](./DATABASE_ARCHITECTURE_GUIDE.md) - Troubleshooting
4. **Understand migrations** → [Liquibase Development Workflow](./LIQUIBASE_DEVELOPMENT_WORKFLOW.md)
5. **Migrate from Flyway** → [Database Architecture Guide](./DATABASE_ARCHITECTURE_GUIDE.md) - Overview
6. **Connect to database** → [Command Reference](./COMMAND_REFERENCE.md) - PostgreSQL Commands

#### Code Development
1. **Write a new service** → [Coding Standards](./CODING_STANDARDS.md) - Follow layer patterns
2. **Create REST endpoint** → [Coding Standards](./CODING_STANDARDS.md) - Controller Pattern
3. **Implement repository** → [Coding Standards](./CODING_STANDARDS.md) - Repository Pattern
4. **Handle exceptions** → [Coding Standards](./CODING_STANDARDS.md) - Exception Handling
5. **Validate inputs** → [Coding Standards](./CODING_STANDARDS.md) - Validation section

#### Security & Compliance
1. **Implement authentication** → [Gateway Trust Architecture](./GATEWAY_TRUST_ARCHITECTURE.md)
2. **Handle PHI data** → [HIPAA Cache Compliance](../HIPAA-CACHE-COMPLIANCE.md)
3. **Debug authorization** → [Gateway Trust Architecture](./GATEWAY_TRUST_ARCHITECTURE.md) - Troubleshooting

#### Operations & Troubleshooting
1. **Service won't start** → [Command Reference](./COMMAND_REFERENCE.md) - Quick Reference
2. **Database is locked** → [Database Architecture Guide](./DATABASE_ARCHITECTURE_GUIDE.md) - Troubleshooting
3. **Slow queries** → [Command Reference](./COMMAND_REFERENCE.md) - PostgreSQL Commands
4. **Check service health** → [Command Reference](./COMMAND_REFERENCE.md) - System Commands

#### Observability
1. **Add distributed tracing** → [Distributed Tracing Guide](./DISTRIBUTED_TRACING_GUIDE.md)
2. **Monitor request flows** → [Distributed Tracing Guide](./DISTRIBUTED_TRACING_GUIDE.md) - Jaeger UI

---

## 📋 By Technology

### Gradle
- [Build Management Guide](./BUILD_MANAGEMENT_GUIDE.md) - Build commands
- [Command Reference](./COMMAND_REFERENCE.md) - Gradle Commands section

### Docker Compose
- [Build Management Guide](./BUILD_MANAGEMENT_GUIDE.md) - Docker Compose Workflow
- [Command Reference](./COMMAND_REFERENCE.md) - Docker Compose Commands section

### PostgreSQL
- [Database Architecture Guide](./DATABASE_ARCHITECTURE_GUIDE.md) - Database setup
- [Command Reference](./COMMAND_REFERENCE.md) - PostgreSQL Commands section

### Liquibase
- [Liquibase Development Workflow](./LIQUIBASE_DEVELOPMENT_WORKFLOW.md) ⭐ CRITICAL
- [Database Architecture Guide](./DATABASE_ARCHITECTURE_GUIDE.md) - Migration standards
- [Entity Migration Guide](./ENTITY_MIGRATION_GUIDE.md) - Complete reference

### Spring Boot
- [Coding Standards](./CODING_STANDARDS.md) - Service patterns
- [Gateway Trust Architecture](./GATEWAY_TRUST_ARCHITECTURE.md) - Security configuration

### OpenTelemetry
- [Distributed Tracing Guide](./DISTRIBUTED_TRACING_GUIDE.md)

---

## 🚨 Critical Documentation

These documents are essential before development:

1. **[CLAUDE.md](../../CLAUDE.md)** - Project overview, essential rules, quick reference
2. **[Liquibase Development Workflow](./LIQUIBASE_DEVELOPMENT_WORKFLOW.md)** ⭐ - Database best practices
3. **[Gateway Trust Architecture](./GATEWAY_TRUST_ARCHITECTURE.md)** - Authentication gold standard
4. **[HIPAA Cache Compliance](../HIPAA-CACHE-COMPLIANCE.md)** - PHI handling requirements
5. **[Coding Standards](./CODING_STANDARDS.md)** - Code patterns and conventions

---

## 📊 Documentation Statistics

| Category | Count | Status |
|----------|-------|--------|
| **Backend Guides** | 7 new + 8 existing | ✅ Complete |
| **Frontend Guides** | 5+ | See `frontend/docs/` |
| **Operations Guides** | 10+ | See `docs/operations/` |
| **Architecture Docs** | 21 ADRs | See `docs/architecture/` |
| **Troubleshooting** | Decision trees | See `docs/troubleshooting/` |

---

## 🔗 Related Documentation Portals

- **[Main Documentation Portal](../../docs/README.md)** - 1,411 files, role-based navigation
- **[Service Catalog](../../docs/services/SERVICE_CATALOG.md)** - 50+ microservices with ports and APIs
- **[Troubleshooting Guide](../../docs/troubleshooting/README.md)** - Problem resolution trees
- **[System Architecture](../../docs/architecture/SYSTEM_ARCHITECTURE.md)** - Complete platform design

---

## 📞 Questions?

1. **First check:** [Troubleshooting Guide](../../docs/troubleshooting/README.md)
2. **Search:** Command-F to find relevant docs
3. **Navigate:** Use the quick links above to find your task
4. **Ask team:** If still stuck, ask your team

---

## 📝 Document Versions

| Guide | Updated | Version |
|-------|---------|---------|
| [Build Management Guide](./BUILD_MANAGEMENT_GUIDE.md) | Jan 19, 2026 | 1.0 |
| [Database Architecture Guide](./DATABASE_ARCHITECTURE_GUIDE.md) | Jan 19, 2026 | 1.0 |
| [Coding Standards](./CODING_STANDARDS.md) | Jan 19, 2026 | 1.0 |
| [Command Reference](./COMMAND_REFERENCE.md) | Jan 19, 2026 | 1.0 |
| [Liquibase Development Workflow](./LIQUIBASE_DEVELOPMENT_WORKFLOW.md) | See file | Current |
| [Gateway Trust Architecture](./GATEWAY_TRUST_ARCHITECTURE.md) | See file | Current |
| [Distributed Tracing Guide](./DISTRIBUTED_TRACING_GUIDE.md) | See file | Current |

---

_Last Updated: January 19, 2026_
_Version: 1.0 - Modularization Complete: 4 new guides created, content extracted from CLAUDE.md_