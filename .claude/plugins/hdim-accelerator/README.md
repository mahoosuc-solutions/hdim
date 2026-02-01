# HDIM Accelerator Plugin

**Version:** 3.0.0
**Author:** HDIM Platform Team
**Repository:** hdim-master

Comprehensive Claude Code plugin for HDIM (HealthData-in-Motion) platform development, featuring 7 proactive technology agents (Spring Boot, Spring Security, Redis, PostgreSQL, Kafka, Docker, GCP), entity-migration synchronization, service scaffolding, FHIR resource generation, CQRS event services, and build/space management.

---

## Overview

The HDIM Accelerator plugin automates repetitive development tasks in the HDIM healthcare interoperability platform, reducing development time by 85-95% for common operations while ensuring compliance with platform standards.

**Key Features:**
- ✅ **Proactive Validation** - Auto-validates entity-migration sync after every change
- ✅ **Service Scaffolding** - Complete microservice generation (1-2 hours → 5 min)
- ✅ **Entity-Migration Sync** - Synchronized JPA entity + Liquibase migration (15-20 min → 2 min)
- ✅ **CQRS Event Services** - Complete event-driven service generation (2 hours → 5 min)
- ✅ **FHIR R4 Support** - HAPI FHIR 7.x integration patterns
- ✅ **Security Patterns** - Gateway trust authentication pre-configured
- ✅ **HIPAA Compliance** - PHI handling and cache controls enforced
- ✅ **Multi-Tenant** - Tenant isolation patterns built-in
- ✅ **Build Management** - Intelligent cleanup with space reporting

---

## Installation

The plugin is located in `.claude/plugins/hdim-accelerator/` and is automatically discovered by Claude Code.

**Verify Installation:**
```bash
# Check plugin is loaded
ls .claude/plugins/hdim-accelerator/plugin.json

# List available commands
/help
```

---

## Commands

### Database Migrations

#### `/add-entity <service-name> <entity-name> [description]`

Generate JPA entity with synchronized Liquibase migration.

**Examples:**
```bash
# Add Appointment entity to patient-service
/add-entity patient-service Appointment "Manages patient appointments"

# Add MeasureResult entity to quality-measure-service
/add-entity quality-measure-service MeasureResult "Stores quality measure results"
```

**What It Does:**
- Creates JPA entity with tenant isolation
- Generates synchronized Liquibase migration
- Creates repository with tenant-filtered queries
- Updates db.changelog-master.xml
- Runs EntityMigrationValidationTest

**Time Savings:** 15-20 min → ~2 min (88% faster)

---

#### `/add-migration <service-name> "<description>"`

Generate standalone Liquibase migration for schema changes.

**Examples:**
```bash
# Add status column to patients table
/add-migration patient-service "add-status-to-patients"

# Create index on last_name
/add-migration patient-service "add-index-on-patient-last-name"
```

**What It Does:**
- Finds next sequential migration number
- Creates migration file with rollback SQL template
- Updates db.changelog-master.xml
- Validates rollback coverage

**Time Savings:** 10-15 min → ~1 min (90% faster)

---

#### `/validate-schema <service-name|all>`

Run EntityMigrationValidationTest to verify entity-migration synchronization.

**Examples:**
```bash
# Validate specific service
/validate-schema patient-service

# Validate all services
/validate-schema all
```

**What It Does:**
- Runs EntityMigrationValidationTest
- Checks 100% rollback SQL coverage
- Verifies `ddl-auto: validate` setting
- Reports schema drift issues with fixes

**Prevents:** Production schema drift (like RefreshToken bug)

---

### Service Development

#### `/create-service <service-name> <port> [description]`

Generate complete HDIM microservice with security, persistence, and Docker configuration.

**Examples:**
```bash
# Create appointment scheduling service
/create-service appointment-service 8090 "Manages patient appointments"

# Create medication tracking service
/create-service medication-service 8091 "Tracks patient medications"
```

**What It Does:**
- Creates full package structure
- Generates SecurityConfig (gateway trust pre-configured)
- Sets up PostgreSQL + Liquibase
- Creates Dockerfile + docker-compose entry
- Generates build.gradle.kts with version catalog
- Creates multi-profile application.yml
- Adds README with service documentation

**Time Savings:** 1-2 hours → ~5 min (95% faster)

---

### FHIR Development

#### `/fhir-resource <service-name> <resource-type>`

Generate FHIR R4 resource endpoint with HAPI FHIR 7.x integration.

**Examples:**
```bash
# Add Patient resource endpoint
/fhir-resource fhir-service Patient

# Add Observation resource endpoint
/fhir-resource fhir-service Observation
```

**What It Does:**
- Generates FHIR controller (CRUD + Search)
- Creates FHIR service with HAPI FHIR client
- Adds persistence entity/repository
- Creates sample FHIR JSON fixtures
- Generates integration tests

**Time Savings:** 30-45 min → ~5 min (89% faster)

---

### Event-Driven Services

#### `/create-event-service <service-name> <port> <domain> [description]`

Generate complete CQRS event service with Kafka integration and denormalized projections.

**Examples:**
```bash
# Create patient event service
/create-event-service patient-event-service 8110 patient "Patient lifecycle event projections"

# Create care gap event service
/create-event-service care-gap-event-service 8111 caregap "Care gap detection event projections"
```

**What It Does:**
- Creates Spring Boot application with @EnableKafka
- Generates projection entity with denormalized read model
- Creates repository with tenant-filtered queries
- Generates Kafka event listeners (idempotent handlers)
- Creates REST controller for querying projections
- Generates statistics DTO for aggregations
- Sets up Kafka consumer configuration
- Creates Liquibase migrations for projection tables
- Generates Dockerfile with multi-stage build
- Creates integration test with Testcontainers (Kafka + PostgreSQL)

**Architecture:**
```
Command Service → Kafka Topic → Event Service → Projection (Denormalized)
                                       ↓
                                  Query API (<100ms)
```

**Performance:**
- Query response: < 100ms (single-table reads)
- Eventual consistency: < 500ms (Kafka → Projection)
- Idempotent event processing (prevents duplicate projections)

**Time Savings:** 2 hours → ~5 min (95% faster)

---

### Build & Space Management

#### `/clean-build <scope>`

Intelligent Gradle build cleanup with disk space savings reporting.

**Scopes:**
- `all` - Clean all modules (38 services + shared)
- `service <name>` - Clean specific service only
- `shared` - Clean shared modules only
- `unused` - Smart detection of unused artifacts (>7 days)

**Examples:**
```bash
# Clean unused artifacts (safe, recommended)
/clean-build unused

# Clean specific service
/clean-build service patient-service

# Clean all modules
/clean-build all
```

**What It Does:**
- Removes build/ directories (per scope)
- Identifies obsolete Gradle cache (>30 days)
- Reports space reclaimed
- Checks for dangling Docker images
- Provides recommendations (build cache, daemon settings)

**Typical Savings:**
- Unused builds: 200-500MB
- Gradle cache: 50-200MB
- Total: 250-700MB

**Time:** ~10-30 seconds

---

#### `/docker-prune`

Comprehensive Docker cleanup for 38+ microservices with confirmation and space reporting.

**Examples:**
```bash
# Safe cleanup with confirmation
/docker-prune

# See what would be removed
/docker-prune --dry-run
```

**What It Does:**
- Shows stopped containers (with sizes)
- Shows dangling images (intermediate build layers)
- Shows unused HDIM service images
- Shows unused volumes
- Shows old build cache (>7 days)
- Asks for confirmation before deletion
- Reports total space reclaimed

**Safety Features:**
- ✅ Shows what will be removed BEFORE deleting
- ✅ Requires confirmation
- ✅ Skips running containers
- ✅ Skips images in docker-compose.yml
- ✅ Separate confirmation for HDIM images

**Typical Savings:**
- Development: 2-6GB
- CI/CD: 5-15GB
- Long-running: 10-30GB

**Critical for HDIM:**
- 38 services × ~350MB = ~13GB base images
- With versions: 25-40GB
- Weekly cleanup recommended

**Time:** ~30-60 seconds

---

### Release Management

#### `/release-validation <version>`

Execute comprehensive 5-phase release validation workflow using Ralph Wiggum autonomous loops.

**Examples:**
```bash
# Validate release v1.3.0
/release-validation v1.3.0

# Validate major version
/release-validation v2.0.0
```

**What It Does:**
- **Phase 1: Code Quality & Testing** (5 tasks)
  - Entity-migration synchronization
  - Full test suite execution
  - HIPAA compliance validation
  - Code coverage analysis
  - ddl-auto setting validation

- **Phase 2: Documentation & Examples** (5 tasks)
  - Auto-generates RELEASE_NOTES from git log
  - Auto-generates UPGRADE_GUIDE with steps
  - Auto-generates VERSION_MATRIX with all dependencies
  - Creates PRODUCTION_DEPLOYMENT_CHECKLIST
  - Creates KNOWN_ISSUES template

- **Phase 3: Integration Testing** (3 tasks)
  - Jaeger distributed tracing validation
  - HikariCP connection pool timing formula check
  - Kafka trace propagation validation
  - Gateway trust authentication validation

- **Phase 4: Deployment Readiness** (4 tasks)
  - Docker image build & security validation
  - Health check configuration validation
  - Environment variable security scan
  - Production deployment checklist review

- **Phase 5: Final Release Preparation** (3 tasks)
  - Version matrix completeness validation
  - Git repository status validation
  - Final documentation review
  - Git tag creation instructions

**Prerequisites:**
- Ralph Wiggum plugin installed
- Clean git working directory
- Docker running
- Backend builds successfully

**Workflow:**
```bash
# 1. Start validation
/release-validation v1.3.0

# 2. Script generates documentation automatically:
#    - RELEASE_NOTES_v1.3.0.md
#    - UPGRADE_GUIDE_v1.3.0.md
#    - VERSION_MATRIX_v1.3.0.md
#    - PRODUCTION_DEPLOYMENT_CHECKLIST_v1.3.0.md
#    - KNOWN_ISSUES_v1.3.0.md

# 3. Execute 5 ralph-loops (copy/paste commands shown)
#    - Each phase runs autonomously
#    - Human confirmation between phases

# 4. Review validation reports in:
#    - docs/releases/v1.3.0/validation/

# 5. Address any failures and re-run validations

# 6. Create git tag when all phases pass
git tag -a v1.3.0 -m "Release v1.3.0"
git push origin v1.3.0
```

**Validation Scripts:**
- `test-entity-migration-sync.sh` - Entity-migration synchronization
- `run-full-test-suite.sh` - Full Gradle test suite + JaCoCo coverage
- `validate-hipaa-compliance.sh` - PHI cache TTL, headers, audit logging
- `validate-jaeger-integration.sh` - OpenTelemetry OTLP configuration
- `validate-hikaricp-config.sh` - Connection pool timing formula
- `validate-kafka-tracing.sh` - Kafka interceptors and type headers
- `validate-gateway-trust-auth.sh` - Gateway trust authentication
- `build-and-validate-images.sh` - Docker security validation
- `validate-health-checks.sh` - Health check configuration
- `validate-environment-vars.sh` - Environment variable security
- `validate-version-matrix.sh` - Version matrix completeness
- `validate-git-status.sh` - Git repository readiness

**Generated Artifacts:**
```
docs/releases/v1.3.0/
├── RELEASE_NOTES_v1.3.0.md
├── UPGRADE_GUIDE_v1.3.0.md
├── VERSION_MATRIX_v1.3.0.md
├── PRODUCTION_DEPLOYMENT_CHECKLIST_v1.3.0.md
├── KNOWN_ISSUES_v1.3.0.md
├── validation/
│   ├── entity-migration-report.md
│   ├── test-coverage-report.md
│   ├── hipaa-compliance-report.md
│   ├── jaeger-integration-report.md
│   ├── hikaricp-config-report.md
│   ├── kafka-tracing-report.md
│   ├── gateway-trust-auth-report.md
│   ├── docker-image-manifest.json
│   ├── health-check-report.md
│   ├── environment-security-report.md
│   ├── version-matrix-validation.md
│   └── git-status-report.md
└── logs/
    ├── validation-execution-[timestamp].log
    └── workflow-summary.md
```

**Critical Validations:**
- ✅ 100% entity-migration synchronization
- ✅ HIPAA cache TTL ≤ 5 minutes
- ✅ HikariCP timing: max-lifetime ≥ 6 × idle-timeout
- ✅ Kafka type headers disabled
- ✅ Gateway trust authentication pattern
- ✅ No hardcoded secrets in docker-compose.yml
- ✅ All services have health checks
- ✅ Non-root Docker user (UID 1001)
- ✅ All documentation generated and reviewed
- ✅ Git status clean and ready for tagging

**Execution Mode:**
- **Semi-Automated** - Autonomous execution within phases
- **Human Confirmation** - Between each of 5 phases
- **Estimated Duration** - 90-120 minutes total

**Time Savings:**
- Manual release validation: 4-6 hours
- Automated workflow: 90-120 minutes
- **Savings:** ~70% time reduction

**Quality Improvements:**
- ✅ Comprehensive validation coverage (20 tasks)
- ✅ Automated documentation generation
- ✅ Consistent release process
- ✅ Audit trail (execution logs)
- ✅ Production safety (all validations must pass)

---

## Skills

Skills provide comprehensive knowledge bases for specific HDIM development topics.

### `database-migrations`

Liquibase best practices for HDIM platform.

**Topics:**
- Sequential migration numbering
- Entity-migration synchronization
- Rollback SQL patterns
- Multi-tenant schema patterns
- Column type mappings
- Troubleshooting guide

**Invoke:** Automatically loaded when working with migrations

---

### `gateway-trust-auth`

Gateway trust authentication patterns for HDIM microservices.

**Topics:**
- SecurityConfig setup
- Trusted header validation
- Role-based access control
- Multi-tenant security
- Testing with GatewayTrustTestHeaders
- Common pitfalls

**Invoke:** Automatically loaded when working with security

---

### `cqrs-event-driven`

CQRS and event sourcing patterns for HDIM platform.

**Topics:**
- Command-Query Responsibility Segregation architecture
- Kafka event streaming patterns
- Denormalized projection design
- Idempotent event handling
- Eventual consistency (<500ms SLA)
- Multi-tenant event isolation
- Performance optimization (<100ms queries)
- Testing with Testcontainers (Kafka + PostgreSQL)

**Invoke:** Automatically loaded when working with event services

---

### Additional Skills (Created as Needed)

- `fhir-development` - HAPI FHIR 7.x patterns
- `quality-measures` - HEDIS measure + CQL implementation
- `hipaa-compliance` - PHI handling requirements
- `testcontainers-integration` - Testing patterns

---

## Agents

Specialized agents perform complex tasks autonomously.

### `migration-validator` (Proactive)

**Runs Automatically When:**
- Any `*Entity.java` file is modified
- Any `db/changelog/*.xml` file is modified
- db.changelog-master.xml is modified

**What It Does:**
- Validates entity-migration synchronization
- Checks 100% rollback SQL coverage
- Verifies `ddl-auto: validate` setting
- Reports issues with actionable fix recommendations

**Prevents:** Production schema drift (RefreshToken bug)

**Manual Invocation:** Called automatically by `/validate-schema` command

---

### Additional Agents (Created as Needed)

- `service-generator` - Automated service scaffolding
- `fhir-assistant` - FHIR resource implementation guidance
- `security-auditor` - Proactive security validation
- `test-stabilizer` - Test failure diagnosis

---

## Technology Agents (Phase 3)

Phase 3 introduces 7 proactive technology-specific agents that automatically validate configuration and enforce HDIM platform best practices whenever related files are modified.

### `spring-boot-agent` (Proactive)

**Runs Automatically When:**
- `application*.yml` modified (any profile: dev, staging, prod, test)
- `build.gradle.kts` modified
- `*Application.java` modified (Spring Boot main class)
- `*Config.java` modified (Spring configuration classes)

**What It Does:**
- Validates Spring Boot profile configuration (dev/staging/prod sampling rates)
- Enforces `ddl-auto: validate` (NEVER create/update - prevents data loss)
- Validates Actuator endpoint security
- Checks traffic tier configuration (HIGH/MEDIUM/LOW connection pools)
- Validates dependency version consistency with version catalog
- Verifies distributed tracing configuration
- Checks health check endpoint configuration

**Critical Validations:**
- ✅ `ddl-auto: validate` in ALL environments
- ✅ Profile-specific configurations (dev=1.0, staging=0.5, prod=0.1 sampling)
- ✅ Actuator endpoints secured or exposed correctly
- ✅ HikariCP connection pool timing formula

**Manual Invocation:** Use Task tool with `subagent_type: "hdim-accelerator:spring-boot-agent"`

---

### `spring-security-agent` (Proactive)

**Runs Automatically When:**
- `*SecurityConfig.java` modified
- `*Filter.java` modified (authentication/authorization filters)
- `*Controller.java` modified (checks @PreAuthorize annotations)

**What It Does:**
- Validates gateway trust authentication pattern (no JWT validation in backend)
- Enforces filter ordering: TrustedHeaderAuthFilter BEFORE UsernamePasswordAuthenticationFilter
- Checks @PreAuthorize annotations on all endpoints
- Validates X-Tenant-ID header extraction
- Ensures 404 (not 403) for tenant isolation failures
- Validates role-based access control patterns

**Critical Validations:**
- ✅ SecurityConfig uses TrustedHeaderAuthFilter + TrustedTenantAccessFilter
- ✅ All endpoints have @PreAuthorize annotations
- ✅ Controllers extract X-Tenant-ID from headers
- ✅ Tests use GatewayTrustTestHeaders

**Manual Invocation:** Use Task tool with `subagent_type: "hdim-accelerator:spring-security-agent"`

---

### `redis-agent` (Proactive)

**Runs Automatically When:**
- `application*.yml` modified (spring.cache or spring.data.redis sections)
- `*Service.java` modified (checks @Cacheable annotations)
- Redis configuration classes modified

**What It Does:**
- Enforces HIPAA-compliant cache TTL (≤5 minutes, recommends 2 minutes)
- Validates cache keys include tenantId prefix
- Checks Cache-Control: no-store headers on PHI responses
- Validates HAPI FHIR serialization compatibility
- Checks Redis connection pool configuration
- Validates cache statistics configuration

**Critical Validations:**
- ✅ TTL ≤ 300,000ms (5 min), recommended 120,000ms (2 min)
- ✅ Cache keys format: `tenantId + ':' + resourceId`
- ✅ PHI responses include `Cache-Control: no-store, no-cache`
- ✅ @Cacheable annotations specify cache name and key

**Manual Invocation:** Use Task tool with `subagent_type: "hdim-accelerator:redis-agent"`

---

### `postgres-agent` (Proactive)

**Runs Automatically When:**
- `application*.yml` modified (spring.datasource or spring.jpa sections)
- `*Entity.java` modified (JPA entities)
- `db/changelog/**/*.xml` modified (Liquibase migrations)
- Database configuration classes modified

**What It Does:**
- Validates HikariCP timing formula: `max-lifetime ≥ 6x idle-timeout`
- Enforces traffic tier configuration (HIGH=50, MEDIUM=20, LOW=10 connections)
- Validates entity-migration synchronization
- Checks 100% Liquibase rollback SQL coverage
- Validates multi-tenant tenant_id indexes
- Checks connection pool leak detection

**Critical Validations:**
- ✅ HikariCP timing: idle-timeout=300s, max-lifetime=1800s (6x), keepalive=240s
- ✅ Traffic tier correctly configured per service
- ✅ All entities have synchronized Liquibase migrations
- ✅ All changesets have explicit `<rollback>` tags
- ✅ All tables have `tenant_id` column with index

**Manual Invocation:** Use Task tool with `subagent_type: "hdim-accelerator:postgres-agent"`

---

### `kafka-agent` (Proactive)

**Runs Automatically When:**
- `application*.yml` modified (spring.kafka section)
- `*Listener.java` modified (Kafka message listeners)
- Files using KafkaTemplate modified
- Event/message model classes modified

**What It Does:**
- Validates producer type headers disabled: `spring.json.add.type.headers: false`
- Validates consumer ignores type headers: `spring.json.use.type.headers: false`
- Checks topic naming convention: `{domain}.{past-tense-action}`
- Validates idempotent event listeners (find-or-create pattern)
- Checks event version tracking
- Validates multi-tenant event isolation

**Critical Validations:**
- ✅ Type headers DISABLED (prevents ClassNotFoundException)
- ✅ Topics follow naming: patient.created, caregap.updated, etc.
- ✅ Event listeners are idempotent (duplicate events handled)
- ✅ Events include tenant_id for isolation
- ✅ Consumer error handling configured

**Manual Invocation:** Use Task tool with `subagent_type: "hdim-accelerator:kafka-agent"`

---

### `docker-agent` (Proactive)

**Runs Automatically When:**
- `Dockerfile` modified (any location)
- `docker-compose*.yml` modified
- `.dockerignore` modified
- Health check endpoints in `application*.yml` modified

**What It Does:**
- Validates non-root user (UID 1001 required)
- Checks health check timing (interval, timeout, retries, start-period)
- Validates JVM container optimization (MaxRAMPercentage=75.0)
- Checks IPv4 stack preference (_JAVA_OPTIONS)
- Validates service dependency ordering (depends_on with conditions)
- Checks proper image tagging

**Critical Validations:**
- ✅ Containers run as non-root user (UID 1001)
- ✅ Health checks configured: interval=30s, timeout=10s, retries=3, start-period=60s
- ✅ JVM options: `-XX:MaxRAMPercentage=75.0 -Djava.net.preferIPv4Stack=true`
- ✅ Service dependencies use `condition: service_healthy`

**Manual Invocation:** Use Task tool with `subagent_type: "hdim-accelerator:docker-agent"`

---

### `gcp-agent` (Proactive)

**Runs Automatically When:**
- `scripts/gcp-*.sh` modified (GCP deployment scripts)
- `infrastructure/**/*.tf` modified (Terraform IaC)
- `k8s/**/*.yaml` modified (Kubernetes manifests)
- GCP deployment configuration modified

**What It Does:**
- Generates Terraform Infrastructure as Code modules
- Converts docker-compose.yml to Kubernetes manifests
- Validates GCP quota (CPUs, IP addresses, disk)
- Checks IAM least-privilege permissions
- Validates multi-region deployment for HA
- Provides cost optimization recommendations (preemptible nodes)
- Validates network security (VPC firewall rules)

**Critical Validations:**
- ✅ IAM uses least-privilege roles (NOT roles/owner)
- ✅ Multi-region deployment configured for HA
- ✅ GCP quotas validated before deployment
- ✅ VPC firewall rules follow least-access principle
- ✅ Service accounts use workload identity

**Manual Invocation:** Use Task tool with `subagent_type: "hdim-accelerator:gcp-agent"`

**IaC Generation:**
```bash
# Generate Terraform for GKE cluster
# GCP agent creates: main.tf, variables.tf, outputs.tf
# Includes: GKE cluster, node pools, Cloud SQL, Memorystore, VPC

# Generate Kubernetes manifests from docker-compose.yml
# GCP agent creates: deployments, services, configmaps, secrets
```

---

## Templates

Reusable code templates for rapid development.

**Available Templates:**

### Core Service Templates
- `entity/entity-template.java` - JPA entity with tenant isolation
- `entity/repository-template.java` - Repository with tenant-filtered queries
- `migration/migration-template.xml` - Liquibase changeset with rollback
- `controller/controller-template.java` - REST controller with security
- `test/integration-test-template.java` - Integration test with Testcontainers

### Event Service Templates
- `event-service/application/application-template.java` - Spring Boot app with @EnableKafka
- `event-service/projection/projection-template.java` - Denormalized projection entity
- `event-service/projection/projection-repository-template.java` - Projection repository
- `event-service/listener/event-listener-template.java` - Kafka consumer with idempotency
- `event-service/controller/projection-controller-template.java` - Query API controller
- `event-service/dto/statistics-dto-template.java` - Aggregation statistics DTO
- `event-service/config/application-yml-template.yml` - Kafka consumer config
- `event-service/migration/projection-migration-template.xml` - Projection table migration
- `event-service/test/integration-test-template.java` - Testcontainers test (Kafka + DB)

**Location:** `.claude/plugins/hdim-accelerator/templates/`

---

## Configuration

### Plugin Settings

**Edit:** `.claude/plugins/hdim-accelerator/plugin.json`

**Available Settings:**

| Setting | Default | Description |
|---------|---------|-------------|
| `proactive_validation` | `true` | Enable automatic schema validation after entity/migration changes |
| `auto_generate_tests` | `true` | Automatically generate test stubs for new components |
| `hipaa_strict_mode` | `true` | Enforce strict HIPAA compliance checks (PHI cache TTL, audit logging) |

**Modify Settings:**
```bash
# Disable proactive validation (not recommended)
# Edit plugin.json: "proactive_validation": false
```

---

## Workflows

### Creating a New Entity

**Typical Flow:**
```bash
# 1. Generate entity with synchronized migration
/add-entity patient-service Appointment "Manages appointments"

# 2. migration-validator agent runs automatically
#    - Validates entity-migration sync
#    - Checks rollback SQL
#    - Reports success or issues

# 3. Customize entity fields
#    - Edit Appointment.java to add domain fields
#    - Edit migration XML to add corresponding columns

# 4. Re-validate
/validate-schema patient-service

# 5. Create service layer
#    - AppointmentService.java
#    - AppointmentRequest/Response DTOs

# 6. Create controller (optional - use template)
#    - AppointmentController.java

# 7. Create tests
#    - AppointmentControllerIntegrationTest.java

# 8. Build and test
./gradlew :modules:services:patient-service:test
```

**Time:** ~10-15 minutes (down from 1+ hour)

---

### Creating a New Microservice

**Typical Flow:**
```bash
# 1. Create service scaffold
/create-service medication-service 8091 "Tracks patient medications"

# 2. Add first entity
/add-entity medication-service Medication "Stores medication data"

# 3. Add additional entities as needed
/add-entity medication-service Prescription "Stores prescription data"

# 4. Build service
./gradlew :modules:services:medication-service:build

# 5. Start service
docker compose up medication-service

# 6. Test service
./gradlew :modules:services:medication-service:test

# 7. Access service
# Health: http://localhost:8091/medication/actuator/health
```

**Time:** ~30-45 minutes (down from 2-4 hours)

---

### Adding FHIR Resource

**Typical Flow:**
```bash
# 1. Generate FHIR resource endpoint
/fhir-resource fhir-service MedicationRequest

# 2. Create persistence entity
/add-entity fhir-service FhirMedicationRequestEntity

# 3. Customize FHIR operations
#    - Edit MedicationRequestService.java
#    - Add custom search parameters

# 4. Add sample FHIR data
#    - Edit test/resources/fhir/MedicationRequest-example.json

# 5. Test FHIR endpoint
./gradlew :modules:services:fhir-service:test

# 6. Validate with FHIR validator (optional)
```

**Time:** ~15-20 minutes (down from 1+ hour)

---

### Creating an Event-Driven Service

**Typical Flow:**
```bash
# 1. Generate CQRS event service
/create-event-service patient-event-service 8110 patient "Patient lifecycle projections"

# 2. Customize projection entity
#    - Edit PatientProjection.java to add denormalized fields
#    - Common fields: aggregated counts, pre-calculated values, flattened relationships

# 3. Update event listeners
#    - Edit PatientEventListener.java
#    - Add handlers for additional events (patient.updated, patient.deleted)
#    - Ensure idempotency (find-or-create pattern)

# 4. Add custom queries to repository
#    - Edit PatientProjectionRepository.java
#    - Add findByTenantIdAndStatus, findByTenantIdAndAgeRange, etc.

# 5. Expose query endpoints
#    - Edit PatientProjectionController.java
#    - Add GET endpoints for custom queries

# 6. Update Kafka topics
#    - Edit application.yml
#    - Add additional topic subscriptions

# 7. Build service
./gradlew :modules:services:patient-event-service:build

# 8. Start service (requires Kafka)
docker compose up kafka postgres
docker compose up patient-event-service

# 9. Test event flow
./gradlew :modules:services:patient-event-service:test

# 10. Verify projection updates
curl http://localhost:8110/patient-event/actuator/health
curl http://localhost:8110/patient-event/api/v1/projections/stats \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-Auth-Validated: true"
```

**Time:** ~15-20 minutes (down from 2+ hours)

**Architecture Benefits:**
- ✅ Query response < 100ms (single-table reads)
- ✅ Eventual consistency < 500ms (Kafka → Projection)
- ✅ Idempotent processing (duplicate events handled)
- ✅ Denormalized data (no joins needed)
- ✅ Multi-tenant isolation (tenant_id filtering)

---

## Best Practices

### Entity-Migration Synchronization

**DO:**
- ✅ Use `/add-entity` for new entities (creates both entity and migration)
- ✅ Run `/validate-schema` before committing
- ✅ Commit entity and migration together
- ✅ Let migration-validator agent run automatically

**DON'T:**
- ❌ Create entity without migration
- ❌ Modify existing migrations
- ❌ Use `ddl-auto: create` or `update`
- ❌ Skip validation tests

### Multi-Tenant Security

**DO:**
- ✅ Include `tenant_id` in every table
- ✅ Filter all queries by tenant
- ✅ Create index on `tenant_id`
- ✅ Return 404 (not 403) for unauthorized access

**DON'T:**
- ❌ Skip tenant isolation
- ❌ Return 403 for wrong tenant (information disclosure)
- ❌ Hardcode tenant IDs

### Gateway Trust Authentication

**DO:**
- ✅ Use TrustedHeaderAuthFilter + TrustedTenantAccessFilter
- ✅ Add @PreAuthorize on all endpoints
- ✅ Extract X-Tenant-ID header in controllers
- ✅ Use GatewayTrustTestHeaders in tests

**DON'T:**
- ❌ Validate JWT in backend services
- ❌ Query database for user/tenant data
- ❌ Create custom authentication filters

---

## Troubleshooting

### "EntityMigrationValidationTest fails"

**Cause:** Entity and migration out of sync

**Fix:**
1. Read error message (missing table, wrong column type, etc.)
2. Use column type mapping from `database-migrations` skill
3. Update entity OR migration to match
4. Re-run `/validate-schema SERVICE-NAME`

### "Rollback coverage < 100%"

**Cause:** Migration missing `<rollback>` tag

**Fix:**
1. Open migration file
2. Add explicit `<rollback>` tag
3. Use rollback pattern from `database-migrations` skill
4. Re-run `/validate-schema SERVICE-NAME`

### "Service won't start - authentication fails"

**Cause:** Missing SecurityConfig or filter order wrong

**Fix:**
1. Ensure SecurityConfig extends proper pattern (see `gateway-trust-auth` skill)
2. Verify filter order: TrustedHeaderAuthFilter BEFORE UsernamePasswordAuthenticationFilter
3. Check TrustedTenantAccessFilter AFTER TrustedHeaderAuthFilter

### "Tests return 401 Unauthorized"

**Cause:** Missing X-Auth-Validated header in tests

**Fix:**
```java
// Use GatewayTrustTestHeaders builder
GatewayTrustTestHeaders testHeaders = GatewayTrustTestHeaders.builder()
        .userId(UUID.randomUUID())
        .username("test_admin")
        .tenantIds("tenant-001")
        .roles("ADMIN,EVALUATOR")
        .build();

mockMvc.perform(get("/api/v1/resources/123")
                .headers(testHeaders.toHttpHeaders()))
        .andExpect(status().isOk());
```

---

## Performance Metrics

### Time Savings per Task

| Task | Before | After | Savings |
|------|--------|-------|---------|
| Create Entity + Migration | 15-20 min | ~2 min | 88% |
| Create Microservice | 1-2 hours | ~5 min | 95% |
| Create Event Service | 2 hours | ~5 min | 95% |
| Add FHIR Resource | 30-45 min | ~5 min | 89% |
| Add Migration Only | 10-15 min | ~1 min | 90% |
| Validate Schema | Manual, varies | Automatic | 100% |
| Clean Build Artifacts | 5-10 min | ~10 sec | 95% |
| Docker Cleanup | 10-15 min | ~30 sec | 96% |

### Quality Improvements

- ✅ **Zero Schema Drift** - Automatic validation prevents production issues
- ✅ **100% Rollback Coverage** - Enforced in all migrations
- ✅ **Consistent Security** - Gateway trust templates ensure compliance
- ✅ **HIPAA Compliance** - PHI cache controls enforced
- ✅ **Multi-Tenant Isolation** - Built into all templates

---

## Examples

### Complete Entity Lifecycle

```bash
# Create entity with migration
/add-entity patient-service Insurance "Stores patient insurance data"

# Validation runs automatically - reports success

# Add custom fields (edit files manually)
# - Insurance.java: Add provider, policyNumber, etc.
# - Migration XML: Add corresponding columns

# Re-validate after changes
/validate-schema patient-service

# Create service layer (manual)
# - InsuranceService.java
# - InsuranceRequest/Response DTOs

# Create controller (manual or use template)
# - InsuranceController.java

# Create tests (use template)
# - InsuranceControllerIntegrationTest.java

# Build and test
./gradlew :modules:services:patient-service:test
```

### Complete Service Lifecycle

```bash
# Create new service
/create-service billing-service 8092 "Handles medical billing"

# Add domain entities
/add-entity billing-service Claim "Stores insurance claims"
/add-entity billing-service Payment "Stores payment transactions"
/add-entity billing-service Invoice "Stores billing invoices"

# Customize each entity (edit files)

# Build service
./gradlew :modules:services:billing-service:build

# Start service
docker compose up billing-service

# Verify health
curl http://localhost:8092/billing/actuator/health

# Run tests
./gradlew :modules:services:billing-service:test
```

---

## Documentation

### HDIM Platform Docs

- **System Architecture:** `docs/architecture/SYSTEM_ARCHITECTURE.md`
- **Gateway Trust Auth:** `backend/docs/GATEWAY_TRUST_ARCHITECTURE.md` (gold standard)
- **Entity-Migration Guide:** `backend/docs/ENTITY_MIGRATION_GUIDE.md`
- **Database Runbook:** `backend/docs/DATABASE_MIGRATION_RUNBOOK.md`
- **HIPAA Compliance:** `backend/HIPAA-CACHE-COMPLIANCE.md`

### Plugin Docs

- **Commands:** `.claude/plugins/hdim-accelerator/commands/*.md`
- **Skills:** `.claude/plugins/hdim-accelerator/skills/*.md`
- **Agents:** `.claude/plugins/hdim-accelerator/agents/*.md`
- **Templates:** `.claude/plugins/hdim-accelerator/templates/`

---

## Contributing

### Adding New Commands

1. Create command file: `commands/new-command.md`
2. Add frontmatter with arguments
3. Write implementation instructions
4. Update `plugin.json` components list
5. Test command execution

### Adding New Skills

1. Create skill file: `skills/new-skill.md`
2. Add `<skill_instructions>` block
3. Document patterns and best practices
4. Update `plugin.json` components list

### Adding New Agents

1. Create agent file: `agents/new-agent.md`
2. Define `when_to_use` condition
3. Write agent instructions
4. Update `plugin.json` components list

---

## License

Internal HDIM platform tooling. Not for public distribution.

---

## Support

**Issues:** Report in HDIM project tracker
**Questions:** Contact HDIM platform team
**Documentation:** See links above

---

**Version History:**
- **3.0.0** (2026-01-20) - Added 7 proactive technology agents (Spring Boot, Spring Security, Redis, PostgreSQL, Kafka, Docker, GCP) with comprehensive hook system (PreToolUse, PostToolUse, Stop, SessionStart), plus comprehensive release validation workflow with Ralph Wiggum autonomous loops (20 validation tasks, 12 scripts, auto-generated documentation)
- **2.0.0** (2026-01-20) - Added CQRS event service generation, build/space management commands
- **1.0.0** (2026-01-20) - Initial release with entity-migration automation, service scaffolding, and FHIR support

---

*Last Updated: 2026-01-20*
