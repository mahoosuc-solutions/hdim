# HDIM Custom Tools & Workflows

## Overview

Custom automation tools and workflows for HDIM development to enforce best practices, ensure compliance, and streamline common tasks.

---

## Quick Start

### Interactive Menu

Run the interactive tools menu:

```bash
./.serena/hdim-tools.sh
```

This provides a menu-driven interface for all tools and workflows.

### Direct Tool Execution

Run tools directly:

```bash
# Validation tools
./.serena/tools/check-hipaa-compliance.sh
./.serena/tools/check-multitenant-queries.sh
./.serena/tools/validate-entity-migration-sync.sh
./.serena/tools/check-service-health.sh

# Workflows
./.serena/workflows/pre-commit-check.sh
./.serena/workflows/new-service-setup.sh [service-name] [port]
```

---

## Validation Tools

### 1. HIPAA Compliance Checker

**Purpose**: Scan code for common HIPAA compliance violations

**Script**: `.serena/tools/check-hipaa-compliance.sh`

**Checks**:
- ✅ Cache TTL configuration (≤ 5 minutes)
- ✅ Cache-Control headers on PHI endpoints
- ✅ PHI in log statements
- ✅ @Cacheable annotations
- ✅ @Audited annotations on PHI access

**Usage**:
```bash
./.serena/tools/check-hipaa-compliance.sh
```

**Exit Codes**:
- `0`: No issues found
- `1`: Compliance issues detected

**Example Output**:
```
🔍 HDIM HIPAA Compliance Checker
================================

1️⃣  Checking cache TTL configuration...
✅ Cache TTL configuration looks good

2️⃣  Checking for Cache-Control headers on PHI endpoints...
✅ Cache-Control headers appear to be in use

3️⃣  Checking for PHI in log statements...
✅ No obvious PHI in log statements

4️⃣  Checking @Cacheable annotations...
   Found 5 @Cacheable annotations
   ⚠️  Verify each has TTL ≤ 5 minutes in application.yml

5️⃣  Checking for @Audited annotations on PHI access...
   Found 3 patient access methods
   Found 3 with @Audited annotation

================================
📊 Summary:
   Errors:   0
   Warnings: 1

⚠️  Warnings found. Manual review recommended.
```

**When to Run**:
- Before committing PHI-related code
- As part of pre-commit checks
- During code review
- After adding caching

---

### 2. Multi-Tenant Query Checker

**Purpose**: Ensure all database queries include tenant isolation

**Script**: `.serena/tools/check-multitenant-queries.sh`

**Checks**:
- ✅ @Query annotations include `tenantId` filter
- ✅ Repository methods use `AndTenant` naming
- ✅ JPA entities include `tenantId` field
- ✅ Controllers use `X-Tenant-ID` header

**Usage**:
```bash
./.serena/tools/check-multitenant-queries.sh
```

**Exit Codes**:
- `0`: All queries properly isolated
- `1`: Multi-tenant violations found

**Example Output**:
```
🔍 HDIM Multi-Tenant Query Checker
===================================

1️⃣  Checking @Query annotations for tenant filtering...
✅ All @Query annotations include tenant filtering

2️⃣  Checking repository method naming...
✅ Repository method naming looks good

3️⃣  Checking entities for tenantId field...
✅ All entities include tenantId field

4️⃣  Checking controllers for X-Tenant-ID header...
✅ All controllers use X-Tenant-ID header

===================================
📊 Summary:
   Errors:   0 (queries missing tenant filter)
   Warnings: 0 (naming/field issues)

✅ Multi-tenant isolation looks good.
```

**When to Run**:
- Before committing repository changes
- After adding new entities
- After creating new queries
- During security reviews

**Common Issues**:
```java
// ❌ BAD - Missing tenant filter
@Query("SELECT p FROM Patient p WHERE p.id = :id")
Optional<Patient> findById(@Param("id") String id);

// ✅ GOOD - Includes tenant filter
@Query("SELECT p FROM Patient p WHERE p.id = :id AND p.tenantId = :tenantId")
Optional<Patient> findByIdAndTenant(@Param("id") String id, @Param("tenantId") String tenantId);
```

---

### 3. Entity-Migration Validator

**Purpose**: Verify JPA entities match Liquibase migrations

**Script**: `.serena/tools/validate-entity-migration-sync.sh`

**Checks**:
- ✅ Runs `EntityMigrationValidationTest` for all services
- ✅ Validates Hibernate schema against database
- ✅ Reports sync issues

**Usage**:
```bash
./.serena/tools/validate-entity-migration-sync.sh
```

**Exit Codes**:
- `0`: All entities synchronized
- `1`: Schema mismatches detected

**Example Output**:
```
🔍 HDIM Entity-Migration Synchronization Validator
==================================================

📦 Validating: authentication
---
   Running: ./gradlew :modules:shared:infrastructure:authentication:test --tests "*EntityMigrationValidationTest"
✅ PASSED - Entities match migrations

📦 Validating: patient-service
---
   Running: ./gradlew :modules:services:patient-service:test --tests "*EntityMigrationValidationTest"
✅ PASSED - Entities match migrations

📦 Validating: quality-measure-service
---
   Running: ./gradlew :modules:services:quality-measure-service:test --tests "*EntityMigrationValidationTest"
✅ PASSED - Entities match migrations

==================================================
📊 Validation Summary:
   ✅ Passed:  3
   ❌ Failed:  0
   ⏭️  Skipped: 0

✅ All entity-migration validations passed!
```

**When to Run**:
- Before committing entity changes
- After creating migrations
- After modifying @Column annotations
- As part of CI/CD pipeline

**How to Fix Issues**:
1. Check entity `@Column` annotations match migration types
2. Ensure `ddl-auto: validate` in application.yml
3. Create new migration if schema changes needed
4. See: `.serena/memories/entity-migration-sync.md`

---

### 4. Service Health Checker

**Purpose**: Check health status of all running services

**Script**: `.serena/tools/check-service-health.sh`

**Checks**:
- ✅ Core services (gateway, cql-engine, patient, fhir, care-gap, quality-measure)
- ✅ Infrastructure (PostgreSQL, Redis, Kafka)
- ✅ Actuator health endpoints

**Usage**:
```bash
./.serena/tools/check-service-health.sh
```

**Exit Codes**:
- `0`: All services healthy
- `1`: Some services offline or degraded

**Example Output**:
```
🏥 HDIM Service Health Checker
==============================

🔍 Checking Core Services...
---
gateway-service (8001):        ✅ HEALTHY
cql-engine-service (8081):     ✅ HEALTHY
patient-service (8084):        ✅ HEALTHY
fhir-service (8085):           ✅ HEALTHY
care-gap-service (8086):       ✅ HEALTHY
quality-measure-service (8087): ✅ HEALTHY

🔧 Checking Infrastructure...
---
postgres (5435):               ✅ RUNNING
redis (6380):                  ✅ RUNNING
kafka (9094):                  ✅ RUNNING

==============================
📊 Health Summary:
   ✅ Healthy:  9
   ⚠️  Degraded: 0
   ❌ Offline:  0

✅ All services are healthy!
```

**When to Run**:
- After starting services
- Before running integration tests
- During troubleshooting
- In CI/CD health checks

**Quick Fixes**:
```bash
# Start all services
docker compose up -d

# Check logs
docker compose logs -f [service-name]

# Restart service
docker compose restart [service-name]
```

---

## Workflows

### 1. Pre-Commit Check Workflow

**Purpose**: Run all validation checks before committing

**Script**: `.serena/workflows/pre-commit-check.sh`

**Runs**:
1. HIPAA compliance check
2. Multi-tenant query check
3. Entity-migration validation
4. Build check
5. Code quality (optional)

**Usage**:
```bash
./.serena/workflows/pre-commit-check.sh
```

**Exit Codes**:
- `0`: All checks passed, safe to commit
- `1`: Some checks failed, fix before committing

**Example Output**:
```
🔍 HDIM Pre-Commit Validation
==============================

1️⃣  Running HIPAA compliance check...
   ✅ HIPAA compliance passed

2️⃣  Running multi-tenant query check...
   ✅ Multi-tenant queries passed

3️⃣  Running entity-migration validation...
   ✅ Entity-migration sync passed

4️⃣  Running build check...
   ✅ Build successful

5️⃣  Code quality checks...
   ⏭️  Skipped (configure spotbugs/checkstyle if needed)

==============================
📊 Pre-Commit Summary:

✅ All checks passed! Safe to commit.

💡 Commit checklist:
   [ ] Meaningful commit message
   [ ] Co-authored-by Claude tag
   [ ] No secrets in code
   [ ] Tests updated
```

**When to Run**:
- Before every commit
- After significant changes
- Before creating pull requests

**Integration with Git**:
```bash
# Add to git hook
echo "./.serena/workflows/pre-commit-check.sh" > .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

---

### 2. New Service Setup Workflow

**Purpose**: Scaffold a new HDIM microservice with proper structure

**Script**: `.serena/workflows/new-service-setup.sh`

**Usage**:
```bash
./.serena/workflows/new-service-setup.sh <service-name> <port>
```

**Example**:
```bash
./.serena/workflows/new-service-setup.sh prescription-service 8091
```

**Creates**:
- ✅ Directory structure (api, application, domain, config, infrastructure)
- ✅ `build.gradle.kts` with dependencies
- ✅ `application.yml` with proper configuration
- ✅ Liquibase master changelog
- ✅ Spring Boot application class
- ✅ Security configuration (Gateway Trust pattern)
- ✅ Entity-migration validation test
- ✅ Updates `settings.gradle.kts`

**Directory Structure Created**:
```
backend/modules/services/prescription-service/
├── src/
│   ├── main/
│   │   ├── java/com/healthdata/prescriptionservice/
│   │   │   ├── api/v1/            # REST controllers
│   │   │   ├── application/       # Services
│   │   │   ├── domain/
│   │   │   │   ├── model/         # Entities
│   │   │   │   └── repository/    # Repositories
│   │   │   ├── config/            # Spring config
│   │   │   └── infrastructure/    # External integrations
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/changelog/      # Liquibase migrations
│   └── test/
│       └── java/com/healthdata/prescriptionservice/
│           └── EntityMigrationValidationTest.java
└── build.gradle.kts
```

**Next Steps**:
1. Add service to `docker-compose.yml`
2. Create initial migration in `db/changelog/`
3. Define domain entities
4. Implement API controllers
5. Add service to `.serena/SERVICE_INDEX.md`

**Build & Run**:
```bash
cd backend
./gradlew :modules:services:prescription-service:build
./gradlew :modules:services:prescription-service:bootRun
```

---

## Interactive Tools Menu

### Launch Interactive Menu

```bash
./.serena/hdim-tools.sh
```

### Menu Options

```
🔧 HDIM Development Tools
=========================

Validation Tools:
  1) Check HIPAA Compliance
  2) Check Multi-Tenant Queries
  3) Validate Entity-Migration Sync
  4) Check Service Health
  5) Run Pre-Commit Checks (ALL)

Workflows:
  6) Create New Service
  7) Run All Services
  8) Stop All Services
  9) View Service Logs

Quick References:
  h) HIPAA Compliance Checklist
  a) Gateway Trust Auth Guide
  e) Entity-Migration Sync Guide
  s) Service Registry

  q) Quit
```

### Quick Reference Access

The menu provides instant access to Serena memories:
- **h**: HIPAA compliance checklist
- **a**: Gateway trust authentication guide
- **e**: Entity-migration sync guide
- **s**: Service registry

---

## CI/CD Integration

### GitHub Actions Example

```yaml
name: HDIM Validation

on: [push, pull_request]

jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: HIPAA Compliance
        run: ./.serena/tools/check-hipaa-compliance.sh

      - name: Multi-Tenant Queries
        run: ./.serena/tools/check-multitenant-queries.sh

      - name: Entity-Migration Sync
        run: ./.serena/tools/validate-entity-migration-sync.sh

      - name: Build
        run: cd backend && ./gradlew build
```

---

## Troubleshooting

### Scripts Not Executable

```bash
chmod +x .serena/tools/*.sh
chmod +x .serena/workflows/*.sh
chmod +x .serena/hdim-tools.sh
```

### Tool Fails with Permission Error

Ensure Docker is running and you have permissions:
```bash
docker ps
```

### Pre-Commit Check Always Fails

Run individual tools to identify specific issue:
```bash
./.serena/tools/check-hipaa-compliance.sh
./.serena/tools/check-multitenant-queries.sh
./.serena/tools/validate-entity-migration-sync.sh
```

---

## Best Practices

### 1. Run Pre-Commit Check Before Every Commit

```bash
./.serena/workflows/pre-commit-check.sh && git commit -m "Your message"
```

### 2. Check Service Health After Starting

```bash
docker compose up -d
./.serena/tools/check-service-health.sh
```

### 3. Validate Entities After Changes

```bash
# After modifying entities
./.serena/tools/validate-entity-migration-sync.sh
```

### 4. Use Interactive Menu for Quick Access

```bash
# Keep this running in a terminal
./.serena/hdim-tools.sh
```

---

## Future Enhancements

Potential additions:
- [ ] Security scanning (OWASP dependency check)
- [ ] Code coverage reporting
- [ ] Performance profiling
- [ ] API documentation generation
- [ ] Database migration rollback scripts
- [ ] Load testing automation

---

## Resources

- **Serena Memories**: `.serena/memories/`
- **Service Index**: `.serena/SERVICE_INDEX.md`
- **CLAUDE.md**: Complete coding guidelines
- **Tools Source**: `.serena/tools/`
- **Workflows Source**: `.serena/workflows/`

---

*Last Updated: January 10, 2026*
*HDIM Custom Tools & Workflows*
