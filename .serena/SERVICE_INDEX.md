# HDIM Service-Specific Serena Configurations

## Overview

Service-specific Serena configurations allow focused development on individual services with tailored context, while maintaining access to parent project knowledge.

## Available Service Configurations

### Core Services (6 configured)

| Service | Port | Path | Serena Config |
|---------|------|------|---------------|
| **quality-measure-service** | 8087 | `/quality-measure` | `backend/modules/services/quality-measure-service/.serena/` |
| **cql-engine-service** | 8081 | `/cql-engine` | `backend/modules/services/cql-engine-service/.serena/` |
| **fhir-service** | 8085 | `/fhir` | `backend/modules/services/fhir-service/.serena/` |
| **patient-service** | 8084 | `/patient` | `backend/modules/services/patient-service/.serena/` |
| **care-gap-service** | 8086 | `/care-gap` | `backend/modules/services/care-gap-service/.serena/` |
| **gateway-service** | 8001 | `/` | `backend/modules/services/gateway-service/.serena/` |

---

## How to Use Service-Specific Configurations

### Activating a Service Project

```bash
# Navigate to service directory
cd backend/modules/services/quality-measure-service

# Activate in Serena
activate_project quality-measure-service
```

### What Each Service Config Provides

Each service configuration includes:
- **Service-specific context**: Purpose, responsibilities, dependencies
- **API endpoints**: Complete endpoint reference
- **Key entities**: Domain models and database tables
- **Build commands**: Service-specific Gradle commands
- **Docker commands**: Service startup and debugging
- **Common tasks**: Typical development workflows
- **Testing patterns**: Service-specific test examples
- **Troubleshooting**: Common issues and solutions
- **Security checklist**: Service-specific security requirements
- **Parent memory links**: Access to root project knowledge

---

## Service Details

### 1. quality-measure-service

**Purpose**: HEDIS quality measure evaluation engine

**Key Features**:
- Quality measure calculation
- Measure result storage
- Population stratification
- Integration with cql-engine and fhir-service

**When to use this config**:
- Implementing HEDIS measures
- Debugging measure evaluation
- Adding measure definitions
- Performance tuning measure calculations

**Initial prompt includes**:
- Package structure
- Key entities (QualityMeasure, MeasureResult, MeasurePopulation)
- API endpoints
- HIPAA compliance for measure results
- Multi-tenant isolation patterns

---

### 2. cql-engine-service

**Purpose**: CQL (Clinical Quality Language) expression evaluation

**Key Features**:
- Parse and evaluate CQL expressions
- Manage CQL libraries
- Value set handling
- FHIR-based data retrieval

**When to use this config**:
- Writing CQL expressions
- Debugging CQL evaluation errors
- Adding CQL libraries
- Performance optimization for CQL

**Initial prompt includes**:
- CQL evaluation flow
- Common CQL patterns (age checks, diagnosis, medications)
- Library management
- Value set loading
- FHIR data integration

---

### 3. fhir-service

**Purpose**: FHIR R4 resource server and data management

**Key Features**:
- FHIR R4 CRUD operations
- Search parameters
- Bundle processing
- Resource validation
- Multi-tenant data isolation

**When to use this config**:
- Implementing FHIR resource providers
- Adding search parameters
- Debugging FHIR validation
- Performance tuning FHIR queries

**Initial prompt includes**:
- Supported FHIR resources (14 resource types)
- FHIR API endpoints
- Search examples
- HAPI FHIR configuration
- HIPAA compliance for FHIR resources

---

### 4. patient-service

**Purpose**: Patient demographics and master data management

**Key Features**:
- Patient demographics (wraps FHIR Patient)
- Enrollment tracking
- Identity resolution
- Patient search

**When to use this config**:
- Patient data management
- Enrollment workflows
- Identity matching logic
- Patient search optimization

**Initial prompt includes**:
- Patient API endpoints
- HIPAA compliance (PHI handling)
- Multi-tenant isolation
- Integration with fhir-service

---

### 5. care-gap-service

**Purpose**: Care gap detection and tracking

**Key Features**:
- Gap identification from measure results
- Gap prioritization
- Closure tracking
- Outreach recommendations

**When to use this config**:
- Care gap workflows
- Gap prioritization logic
- Event-driven processing (Kafka)
- Reporting

**Initial prompt includes**:
- Care gap entities (CareGap, CareGapClosure)
- Gap lifecycle (OPEN → CLOSED)
- Kafka integration
- API endpoints for gap management

---

### 6. gateway-service

**Purpose**: API gateway and authentication

**Key Features**:
- JWT validation (ONLY component that validates JWT)
- Inject X-Auth-* headers
- Request routing
- Rate limiting

**When to use this config**:
- Authentication implementation
- Request routing
- HMAC signature generation
- Security troubleshooting

**Initial prompt includes**:
- Gateway trust architecture (CRITICAL)
- JWT validation flow
- Header injection
- HMAC signing for production
- Routing configuration

**⚠️ CRITICAL**: This service is the security foundation. All changes must be carefully reviewed.

---

## Workflow Examples

### Example 1: Working on Quality Measure Evaluation

```bash
# Navigate to service
cd backend/modules/services/quality-measure-service

# Activate service project in Serena
activate_project quality-measure-service

# Read service-specific context (automatically loaded via initial_prompt)

# Access parent memories if needed
read_memory ../../../../.serena/memories/hipaa-compliance-checklist.md

# Find symbols in service
find_symbol QualityMeasureService
find_symbol MeasureResultRepository

# Make changes...

# Run service-specific tests
execute_shell_command "./gradlew :modules:services:quality-measure-service:test"
```

### Example 2: Debugging CQL Evaluation

```bash
# Navigate and activate
cd backend/modules/services/cql-engine-service
activate_project cql-engine-service

# Service context includes CQL patterns and troubleshooting

# Find CQL evaluation code
find_symbol CqlEvaluationService

# Access troubleshooting guide
read_memory ../../../../.serena/memories/troubleshooting-guide.md

# Test CQL expression
execute_shell_command "curl -X POST http://localhost:8081/cql-engine/api/v1/evaluate ..."
```

### Example 3: Implementing FHIR Resource Provider

```bash
# Navigate and activate
cd backend/modules/services/fhir-service
activate_project fhir-service

# Find existing resource providers as examples
find_symbol PatientResourceProvider

# Access common patterns
read_memory ../../../../.serena/memories/common-patterns.md

# Implement new resource provider...

# Test FHIR endpoint
execute_shell_command "curl http://localhost:8085/fhir/metadata"
```

---

## Parent Project Memories (Available to All Services)

All service configurations link back to root project memories:

- **architecture-overview.md** - System architecture, 28 services
- **hipaa-compliance-checklist.md** - PHI handling, cache TTL
- **gateway-trust-auth.md** - Authentication patterns
- **entity-migration-sync.md** - JPA/Liquibase sync
- **service-registry.md** - Complete service catalog
- **common-patterns.md** - Standard coding patterns
- **troubleshooting-guide.md** - Common issues

Access from any service:
```
read_memory ../../../../.serena/memories/architecture-overview.md
```

---

## Adding More Service Configurations

To create a Serena config for additional services:

1. Create `.serena/` directory in service root
2. Copy and customize `project.yml` from existing service
3. Update `initial_prompt` with service-specific context
4. Add service to this index

### Template Structure

```yaml
languages:
- java

encoding: "utf-8"
ignore_all_files_in_gitignore: true

ignored_paths:
  - "**/build/**"
  - "**/.gradle/**"

read_only: false
excluded_tools: []

initial_prompt: |
  # [Service Name] - [Purpose]

  ## Service Overview
  Port: [PORT]
  Context: [PATH]
  Purpose: [DESCRIPTION]

  ## Key Responsibilities
  - ...

  ## Dependencies
  - ...

  ## Critical Requirements
  - HIPAA compliance
  - Multi-tenant isolation
  - Gateway trust authentication
  - Entity-migration sync

  ## Parent Memories
  - ../../../../.serena/memories/...

project_name: "[service-name]"
included_optional_tools: []
```

---

## Benefits of Service-Specific Configurations

### 1. Focused Context
- Service-specific API endpoints
- Relevant entities and repositories
- Service dependencies
- Common tasks for that service

### 2. Faster Onboarding
- New developers can quickly understand a service
- Clear examples and patterns
- Troubleshooting specific to service

### 3. Better Symbol Navigation
- Find symbols within service scope
- Avoid clutter from other services
- Faster code navigation

### 4. Consistent with Parent
- All services link to parent memories
- Shared patterns and guidelines
- Centralized HIPAA/security rules

---

## Root vs Service Configuration

### When to Use Root Configuration
- Working across multiple services
- System architecture changes
- Adding new services
- Infrastructure updates
- Documentation updates

**Activate**: From project root
```
activate_project hdim-master
```

### When to Use Service Configuration
- Focused development on single service
- Service-specific features
- Debugging service issues
- Service-specific testing

**Activate**: From service directory
```
cd backend/modules/services/[service-name]
activate_project [service-name]
```

---

## Quick Reference

### Service Ports
- 8001: gateway-service
- 8081: cql-engine-service
- 8084: patient-service
- 8085: fhir-service
- 8086: care-gap-service
- 8087: quality-measure-service

### Service Dependencies
```
gateway-service (8001)
  └─> quality-measure-service (8087)
        └─> cql-engine-service (8081)
              └─> fhir-service (8085)
  └─> patient-service (8084)
        └─> fhir-service (8085)
  └─> care-gap-service (8086)
        └─> quality-measure-service (8087)
        └─> patient-service (8084)
```

### Build Commands (from service directory)
```bash
# Build
./gradlew build

# Run
./gradlew bootRun

# Test
./gradlew test

# Entity validation
./gradlew test --tests "*EntityMigrationValidationTest"
```

---

## Resources

- **Root Serena Config**: `/.serena/README.md`
- **Parent Memories**: `/.serena/memories/`
- **Service Configs**: `/backend/modules/services/[service]/.serena/`
- **CLAUDE.md**: Complete coding guidelines

---

*Last Updated: January 10, 2026*
*HDIM Service-Specific Serena Configurations*
