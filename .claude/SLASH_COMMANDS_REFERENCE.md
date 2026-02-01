---
This file was created by merging and customizing Mahoosuc Operating System commands for HDIM's healthcare platform
---

# HDIM Slash Commands Reference

Comprehensive documentation of all available slash commands for HDIM (HealthData-in-Motion) platform development and operations.

## 🏥 Healthcare Commands

### HEDIS Quality Measures

- **`/hedis:evaluate --measure <code> --patient <id>`**
  Evaluate HEDIS quality measures for individual patients or populations

- **`/hedis:gap-analysis --population <cohort> [--measures all|specific]`**
  Identify care gaps across patient populations with intervention recommendations

- **`/hedis:report --measure <code> --period <YYYY> --format pdf|csv`**
  Generate HEDIS compliance reports for quality measure certification

### FHIR Operations

- **`/fhir:validate --resource <type> [--profile url]`**
  Validate FHIR R4 resources against base profiles and implementation guides

- **`/fhir:search --resource-type <type> --criteria <params>`**
  Search FHIR resources using standard search parameters

- **`/fhir:bundle-create --type <transaction|batch> --resources <list>`**
  Create FHIR bundles for bulk operations and data exchange

### Care Gap Management

- **`/care-gap:detect --patient <id> [--measures all|specific]`**
  Detect care gaps for patients using HEDIS measures and clinical guidelines

- **`/care-gap:close --gap-id <id> --intervention <type>`**
  Close care gaps with documented interventions and audit trail

- **`/care-gap:bulk-action --gap-ids <list> --action <close|assign|prioritize>`**
  Perform bulk operations on multiple care gaps (Issue #241 complete)

### Quality Measures

- **`/quality-measure:run --measure-id <id> --population <cohort>`**
  Execute CQL-based quality measure evaluations

- **`/quality-measure:test --cql-file <path> --test-data <file>`**
  Test CQL measure logic with sample patient data

### Patient Operations

- **`/patient:search --criteria <name|mrn|dob> [--tenant <id>]`**
  Search patients across multi-tenant database with privacy filters

- **`/patient:merge --source-id <id> --target-id <id>`**
  Merge duplicate patient records with audit trail

## 💾 Database Commands

### Migration Management

- **`/db:migrate --service <name> [--validate-only]`**
  Execute Liquibase migrations with entity-migration synchronization

- **`/db:validate --service <name> [--fix-drift]`**
  Validate JPA entities match Liquibase schema definitions

- **`/db:rollback --service <name> --count <N>`**
  Rollback database migrations with automatic schema validation

- **`/db:tenant --action <create|delete|migrate> --tenant-id <id>`**
  Manage multi-tenant database operations and isolation

### Backup & Restore

- **`/db:backup --service <name> --retention <days>`**
  Create encrypted database backups with HIPAA compliance

- **`/db:restore --service <name> --backup-id <id>`**
  Restore database from backup with tenant isolation verification

## 🚀 DevOps Commands

### Deployment

- **`/devops:deploy <environment> [--services <list>]`**
  Deploy HDIM microservices with HIPAA compliance validation

- **`/devops:rollback --service <name> --to-version <tag>`**
  Emergency rollback to previous stable version

- **`/devops:monitor [service-name] [--metrics cpu|memory|requests]`**
  Monitor production deployments with real-time Prometheus metrics

### Infrastructure

- **`/devops:cost-analyze [--project <gcp-project>] [--period <days>]`**
  GCP cost analysis with service breakdown and optimization recommendations

- **`/devops:debug --service <name> --issue <error|latency|crash>`**
  Debug production issues with distributed tracing and log analysis

## 🔐 Authentication & Security

### Authentication

- **`/auth:test [--endpoint login|logout|refresh] [--coverage]`**
  Test JWT authentication with gateway trust validation

- **`/auth:audit [--framework hipaa|soc2]`**
  Security audit of authentication system with compliance checks

- **`/auth:rotate-keys [--key-type jwt|api] [--graceful-transition]`**
  Rotate JWT signing keys with zero-downtime transition

### HIPAA Compliance

- **`/hipaa:scan [--service <name>] [--report-format pdf|json]`**
  Scan codebase for HIPAA compliance violations (PHI exposure, cache TTL, audit logging)

- **`/hipaa:audit-trail --patient-id <id> [--period <days>]`**
  Generate PHI access audit trail for compliance verification

## 🧪 Testing Commands

### Integration Testing

- **`/test:integration --service <name> [--coverage]`**
  Run integration tests with Spring Boot test containers

- **`/test:e2e --workflow <name> [--browser chrome|firefox]`**
  Execute end-to-end tests with Playwright browser automation

### Performance Testing

- **`/test:performance --endpoint <url> --requests <N> [--concurrent <N>]`**
  Load test API endpoints with Gatling or k6

- **`/test:baseline --service <name> --save-results`**
  Establish performance baselines for regression detection

## 📊 API Documentation

### OpenAPI Generation

- **`/api:docs --service <name> [--output-format yaml|json]`**
  Generate OpenAPI 3.0 documentation from SpringDoc annotations

- **`/api:test --service <name> [--coverage]`**
  Test API endpoints against OpenAPI specifications

### Gateway Management

- **`/api:gateway --action <add-route|update-route|delete-route>`**
  Configure Kong API Gateway routes and plugins

## 📈 Analytics & Monitoring

### Metrics

- **`/analytics:dashboard --service <name> [--period <hours>]`**
  Display Grafana dashboards for service metrics

- **`/analytics:report --type <performance|usage|errors> --format pdf`**
  Generate analytics reports from Prometheus data

### Distributed Tracing

- **`/trace:analyze --trace-id <id>`**
  Analyze distributed traces across microservices

- **`/trace:search --service <name> --duration-gt <ms>`**
  Search for slow traces using OpenTelemetry data

## 🏗️ Build & CI/CD

### Gradle Operations

- **`/build:service --name <service> [--skip-tests]`**
  Build single HDIM service with Gradle

- **`/build:all [--parallel] [--max-workers <N>]`**
  Build all 51 HDIM services with dependency resolution

### Docker Operations

- **`/docker:build --service <name> [--no-cache]`**
  Build Docker images for HDIM microservices

- **`/docker:compose --action <up|down|logs> [--services <list>]`**
  Manage Docker Compose orchestration

## 📝 Documentation

### Code Generation

- **`/docs:entity --name <EntityName> --service <service-name>`**
  Generate JPA entity with Liquibase migration and validation test

- **`/docs:controller --name <ControllerName> --service <service-name>`**
  Generate REST controller with OpenAPI annotations

### Architecture Documentation

- **`/docs:architecture --output-format <mermaid|plantuml>`**
  Generate architecture diagrams from service dependencies

## 🎯 Development Utilities

### Code Quality

- **`/code:lint --service <name> [--fix]`**
  Run Checkstyle linting with auto-fix

- **`/code:format --service <name> [--verify]`**
  Format code with Google Java Format

### Event Sourcing

- **`/event:replay --aggregate-id <id> [--from-version <N>]`**
  Replay event sourcing events for debugging

- **`/event:projection --name <projection-name> --rebuild`**
  Rebuild read model projections from event store

## Command Categories Summary

| Category | Commands | Priority |
|----------|----------|----------|
| Healthcare | 12 | High |
| Database | 6 | High |
| DevOps | 5 | High |
| Authentication | 5 | High |
| Testing | 4 | Medium |
| API | 3 | Medium |
| Analytics | 3 | Medium |
| Build & CI/CD | 4 | Medium |
| Documentation | 3 | Low |
| Development Utilities | 3 | Low |

**Total:** 48 commands

## Command Naming Conventions

- **Format:** `/category:action [--options]`
- **Categories:** Lowercase, hyphenated (e.g., `care-gap`, `quality-measure`)
- **Actions:** Lowercase, hyphenated (e.g., `bulk-action`, `gap-analysis`)
- **Options:** Long-form flags (e.g., `--service`, `--format`)

## Usage Examples

### Complete HEDIS Evaluation Workflow

```bash
# 1. Detect care gaps
/care-gap:detect --patient 123e4567 --measures all

# 2. Evaluate specific HEDIS measure
/hedis:evaluate --measure BCS --patient 123e4567

# 3. Close identified gap
/care-gap:close --gap-id abc-123 --intervention APPOINTMENT_SCHEDULED

# 4. Generate compliance report
/hedis:report --measure BCS --period 2026 --format pdf
```

### Production Deployment Workflow

```bash
# 1. Validate database migrations
/db:validate --service patient-service

# 2. Run integration tests
/test:integration --service patient-service --coverage

# 3. Deploy to staging
/devops:deploy staging --services patient-service,care-gap-service

# 4. Deploy to production
/devops:deploy production --strategy blue-green

# 5. Monitor deployment
/devops:monitor --metrics requests,errors,latency
```

## Getting Help

For detailed documentation on any command:

```bash
# Read command markdown file
cat .claude/commands/healthcare/hedis-evaluate.md

# List all commands in category
ls .claude/commands/healthcare/
```

## Command Implementation Status

### ✅ Implemented (3 commands)
- `/hedis:evaluate` - Complete with examples
- `/db:migrate` - Complete with validation
- `/devops:deploy` - Complete with HIPAA checks

### 🚧 In Progress (0 commands)

### 📋 Planned (45 commands)
- All remaining commands from categories above

## Integration with Mahoosuc OS

This command set was created by:
1. Copying Mahoosuc OS command structure and patterns
2. Adapting DevOps, Auth, Testing, and Analytics commands for HDIM
3. Creating new Healthcare, Database, and API commands specific to HDIM
4. Maintaining consistent naming and validation patterns

**Source:** `/mnt/wdblack/dev/projects/mahoosuc-operating-system/.claude/`

## Next Steps

1. **Complete Healthcare Commands** - Implement remaining FHIR, Care Gap, and Quality Measure commands
2. **Add Testing Commands** - Integration, E2E, and HIPAA compliance testing
3. **Expand DevOps** - Cost analysis, monitoring, and debugging commands
4. **Document Validation Schemas** - Create JSON schemas for command output validation
5. **Build Command Registry** - YAML registry for agent discovery

---

_Last Updated: January 25, 2026_
_Version: 1.0.0_
_Total Commands: 48 (3 implemented, 45 planned)_
