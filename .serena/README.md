# HDIM Serena Configuration

## Overview

This directory contains Serena AI coding agent configuration for the HealthData-in-Motion (HDIM) platform.

Serena is now fully configured with:
- ✅ Enhanced project configuration
- ✅ 7 comprehensive memory modules
- ✅ 6 service-specific configurations
- ✅ 4 validation tools
- ✅ 2 automation workflows
- ✅ Interactive tools menu
- ✅ Initial prompt with critical HDIM context
- ✅ Language server support (Java + TypeScript)

---

## What is Serena?

Serena is an AI coding agent platform that provides:
- **Symbol-level code navigation** (classes, methods, interfaces)
- **Project-specific memory** (persistent knowledge base)
- **Language server integration** (autocomplete, refactoring)
- **Multi-file refactoring** capabilities
- **Onboarding & architecture understanding**

---

## Memory Modules Created

### 1. architecture-overview.md
**Purpose**: System architecture, 28 services, tech stack overview

**When to use**:
- Understanding system design
- Learning service dependencies
- Finding service ports and URLs
- Reviewing technology choices

**Key contents**:
- Tech stack summary (Java 21, Spring Boot 3.x, HAPI FHIR 7.x)
- All 28 services with ports and purposes
- Request flow diagrams
- Build status

### 2. hipaa-compliance-checklist.md
**Purpose**: PHI handling requirements and HIPAA compliance rules

**When to use**:
- BEFORE writing any PHI-related code
- During code reviews
- When implementing caching
- Before committing changes

**Key contents**:
- Cache TTL requirements (≤ 5 minutes)
- HTTP header requirements
- Audit logging patterns
- Multi-tenant isolation rules
- Pre-commit checklist

### 3. gateway-trust-auth.md
**Purpose**: Gateway trust authentication architecture (GOLD STANDARD)

**When to use**:
- Implementing service security
- Debugging authentication issues
- Reviewing auth patterns
- Migrating services

**Key contents**:
- Gateway trust flow
- Filter configuration examples
- Header reference
- Common mistakes to avoid
- Migration checklist

### 4. entity-migration-sync.md
**Purpose**: JPA entity and Liquibase migration synchronization

**When to use**:
- Creating new entities
- Modifying existing entities
- Debugging schema validation errors
- Before database changes

**Key contents**:
- Column type mapping reference
- Entity pattern examples
- Migration file templates
- Validation testing
- Troubleshooting guide

### 5. service-registry.md
**Purpose**: Complete catalog of all 28 services

**When to use**:
- Looking up service ports
- Understanding service dependencies
- Finding service documentation
- Planning integrations

**Key contents**:
- Service details (port, context, purpose)
- Dependency maps
- Quick commands per service
- Build status dashboard

### 6. common-patterns.md
**Purpose**: Standard coding patterns for HDIM

**When to use**:
- Writing new controllers
- Implementing services
- Creating repositories
- Configuring security
- Writing tests

**Key contents**:
- Controller pattern (with RBAC)
- Service pattern (with caching)
- Repository pattern (with multi-tenant)
- Entity pattern
- Testing patterns
- Security configuration

### 7. troubleshooting-guide.md
**Purpose**: Common issues and solutions

**When to use**:
- Debugging build failures
- Fixing Docker issues
- Resolving authentication problems
- Database errors
- Performance issues

**Key contents**:
- Build & compilation fixes
- Docker troubleshooting
- Auth debugging
- Database issues
- Health checks
- Emergency procedures

---

## Custom Tools & Workflows

HDIM includes custom automation tools and workflows to enforce best practices and streamline development.

### Interactive Tools Menu

Launch the interactive menu for quick access to all tools:

```bash
./.serena/hdim-tools.sh
```

### Validation Tools (4 total)

#### 1. HIPAA Compliance Checker
**Script**: `.serena/tools/check-hipaa-compliance.sh`

Scans code for HIPAA violations:
- Cache TTL configuration (≤ 5 minutes)
- Cache-Control headers on PHI endpoints
- PHI in log statements
- @Cacheable and @Audited annotations

```bash
./.serena/tools/check-hipaa-compliance.sh
```

#### 2. Multi-Tenant Query Checker
**Script**: `.serena/tools/check-multitenant-queries.sh`

Ensures database queries include tenant isolation:
- @Query annotations with tenantId filter
- Repository method naming (AndTenant suffix)
- Entities with tenantId field
- Controllers with X-Tenant-ID header

```bash
./.serena/tools/check-multitenant-queries.sh
```

#### 3. Entity-Migration Validator
**Script**: `.serena/tools/validate-entity-migration-sync.sh`

Validates JPA entities match Liquibase migrations:
- Runs EntityMigrationValidationTest for all services
- Reports schema mismatches
- Prevents production schema drift

```bash
./.serena/tools/validate-entity-migration-sync.sh
```

#### 4. Service Health Checker
**Script**: `.serena/tools/check-service-health.sh`

Checks health of all running services:
- Core services (gateway, cql-engine, patient, fhir, care-gap, quality-measure)
- Infrastructure (PostgreSQL, Redis, Kafka)
- Actuator health endpoints

```bash
./.serena/tools/check-service-health.sh
```

### Automation Workflows (2 total)

#### 1. Pre-Commit Check
**Script**: `.serena/workflows/pre-commit-check.sh`

Runs all validations before committing:
1. HIPAA compliance
2. Multi-tenant queries
3. Entity-migration sync
4. Build check
5. Code quality

```bash
./.serena/workflows/pre-commit-check.sh
```

**Integration with Git**:
```bash
echo "./.serena/workflows/pre-commit-check.sh" > .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

#### 2. New Service Setup
**Script**: `.serena/workflows/new-service-setup.sh`

Scaffolds a new microservice with:
- Complete directory structure
- Spring Boot configuration
- Security setup (Gateway Trust pattern)
- Entity-migration validation test
- Liquibase setup

```bash
./.serena/workflows/new-service-setup.sh prescription-service 8091
```

### Complete Documentation

See **`TOOLS_AND_WORKFLOWS.md`** for comprehensive documentation:
- Detailed tool descriptions
- Exit codes and error handling
- Example outputs
- Troubleshooting guides
- CI/CD integration examples

---

## How to Use Serena with HDIM

### Accessing Memories

When using Serena, you can access memories using:

```
read_memory architecture-overview.md
read_memory hipaa-compliance-checklist.md
read_memory gateway-trust-auth.md
```

Or list all available memories:

```
list_memories
```

### Common Workflows

#### 1. Starting a New Feature

```
# Load architecture context
read_memory architecture-overview.md

# Check relevant patterns
read_memory common-patterns.md

# Review compliance requirements
read_memory hipaa-compliance-checklist.md
```

#### 2. Debugging an Issue

```
# Check troubleshooting guide
read_memory troubleshooting-guide.md

# Look up service details
read_memory service-registry.md

# Review authentication if auth-related
read_memory gateway-trust-auth.md
```

#### 3. Database Work

```
# Review entity-migration rules
read_memory entity-migration-sync.md

# Check common patterns
read_memory common-patterns.md

# Validate changes before committing
```

#### 4. Code Review

```
# Load HIPAA checklist
read_memory hipaa-compliance-checklist.md

# Review common patterns
read_memory common-patterns.md

# Check entity-migration sync if DB changes
read_memory entity-migration-sync.md
```

---

## Initial Prompt

The `.serena/project.yml` file includes an `initial_prompt` that provides:
- Project overview and purpose
- Tech stack summary
- Core services list
- Critical requirements (HIPAA, auth, multi-tenant, entity-migration)
- Common commands
- Code review checklist
- Documentation references

This prompt is **automatically loaded** when Serena activates the HDIM project.

---

## Configuration Details

### Language Servers

Configured for:
- **Java** (primary language)
- **TypeScript** (Angular frontend)

### Ignored Paths

The following directories are ignored:
- `**/build/**` - Gradle build outputs
- `**/dist/**` - Frontend build outputs
- `**/node_modules/**` - NPM dependencies
- `**/.gradle/**` - Gradle cache
- `**/target/**` - Maven targets (if any)
- `**/.nx/**` - Nx cache
- `**/coverage/**` - Test coverage reports
- `**/.docker-tmp/**` - Docker temporary files

---

## Advanced Features

### Onboarding

Serena can perform project onboarding to discover structure:

```
onboarding
```

This will analyze:
- Project structure
- Build tasks (Gradle)
- Test commands
- Service dependencies

### Symbol Navigation

Find symbols across the codebase:

```
find_symbol PatientService
find_symbol TrustedHeaderAuthFilter
find_symbol QualityMeasureService
```

Find references to symbols:

```
find_referencing_symbols PatientRepository
```

### Memory Management

Create new memories for specific knowledge:

```
write_memory my-feature-notes.md
```

Delete memories no longer needed:

```
delete_memory outdated-notes.md
```

---

## Best Practices

### 1. Always Load Relevant Memories First

Before starting work:
- Review architecture-overview.md for system context
- Check hipaa-compliance-checklist.md for PHI work
- Load common-patterns.md for implementation guidance

### 2. Use Memories During Code Review

Reference memories when reviewing:
- HIPAA compliance
- Authentication patterns
- Entity-migration sync
- Common patterns

### 3. Keep Memories Updated

When architecture changes:
- Update relevant memory files
- Document new patterns
- Add troubleshooting entries

### 4. Combine with CLAUDE.md

Serena memories complement CLAUDE.md:
- **CLAUDE.md**: Complete coding guidelines (LAW)
- **Serena memories**: Quick reference for common patterns

---

## Integration with Mahoosuc Operating System

The HDIM project is part of the Mahoosuc Operating System with 283 slash commands and 4 skills.

Serena provides:
- Project-specific context
- Code-level navigation
- Symbol search and refactoring

Mahoosuc commands provide:
- Cross-tool automation
- Business workflows
- Content generation

Use both together for maximum productivity!

---

## Troubleshooting Serena

### Issue: Language server not starting

**Solution**:
```bash
restart_language_server
```

### Issue: Memories not loading

**Solution**:
```bash
list_memories  # Check if memories exist
read_memory architecture-overview.md  # Test reading
```

### Issue: Project not activated

**Solution**:
```bash
activate_project hdim-master
```

---

---

## Mahoosuc Operating System Integration

HDIM Serena seamlessly integrates with your Mahoosuc Operating System (283 commands + 4 skills).

### Installation

Install HDIM commands and skills into Mahoosuc:

```bash
cd .serena/mahoosuc-integration
./install.sh
```

This installs:
- **4 HDIM Commands**: `/hdim-validate`, `/hdim-service-create`, `/hdim-memory`, `/hdim-service`
- **1 HDIM Skill**: `hdim-dev` (development workflow with Serena integration)

### Quick Access

**Commands**:
```bash
/hdim-validate hipaa     # HIPAA compliance check
/hdim-memory patterns    # Access coding patterns
/hdim-service start      # Start all services
```

**Skills**:
```
Use hdim-dev skill: Implement patient care gap endpoint with HIPAA compliance
```

### Complete Documentation

See `.serena/mahoosuc-integration/`:
- **INTEGRATION_GUIDE.md** (16K) - Complete integration guide
- **QUICK_REFERENCE.md** (8K) - Quick reference card
- **EXAMPLE_WORKFLOWS.md** (16K) - Real-world workflows
- **commands/** (20K) - 4 HDIM slash commands
- **skills/** (16K) - 1 HDIM skill

### Benefits

**Before**: Separate systems
- Mahoosuc for general development
- Serena memories manually accessed
- No automated HDIM validation

**After**: Unified environment
- ✅ HDIM commands globally available
- ✅ Serena memories via `/hdim-memory`
- ✅ Automated validation via `/hdim-validate`
- ✅ Service management via `/hdim-service`
- ✅ Best of both worlds

---

## Resources

- **Serena Documentation**: Check Serena official docs for advanced features
- **CLAUDE.md**: `../CLAUDE.md` - Complete HDIM coding guidelines
- **Architecture**: `../docs/architecture/SYSTEM_ARCHITECTURE.md`
- **Memories**: `./.serena/memories/` - All knowledge modules
- **Mahoosuc Integration**: `./.serena/mahoosuc-integration/` - Commands, skills, workflows

---

## Next Steps

1. **Try Serena**: Activate the project and load a memory
2. **Explore**: Use `find_symbol` to navigate code
3. **Customize**: Add project-specific memories as needed
4. **Integrate**: Combine with Mahoosuc commands for workflows

---

*Last Updated: January 10, 2026*
*HDIM Serena Setup - Comprehensive Knowledge Base*
