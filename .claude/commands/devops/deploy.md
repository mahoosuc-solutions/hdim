---
name: devops:deploy
description: Deploy HDIM microservices to multi-environment infrastructure with HIPAA-compliant safety checks
category: devops
priority: high
---

# Deploy HDIM Microservices

Deploy HDIM healthcare platform to production, staging, or development environments with comprehensive safety checks and HIPAA compliance validation.

## Usage

```bash
/devops:deploy <environment> [options]
```

## Options

- `<environment>` - Target environment (production, staging, development)
- `--services <list>` - Specific services to deploy (default: all)
- `--skip-tests` - Skip test execution (NOT recommended for production)
- `--skip-backup` - Skip database backup (NOT recommended for production)
- `--strategy <blue-green|rolling|recreate>` - Deployment strategy (default: rolling)

## Examples

### Deploy All Services to Staging

```bash
/devops:deploy staging
```

Deploys all 51 HDIM microservices to staging environment.

### Deploy Specific Services to Production

```bash
/devops:deploy production --services patient-service,care-gap-service,fhir-service
```

Deploys only Patient, Care Gap, and FHIR services to production.

### Blue-Green Deployment

```bash
/devops:deploy production --strategy blue-green
```

Uses blue-green deployment strategy for zero-downtime production deployment.

## HDIM Deployment Workflow

```mermaid
graph TD
    A[/devops:deploy] --> B{Environment?}
    B -->|Production| C[Require Explicit Approval]
    B -->|Staging/Dev| D[Skip Approval]
    C --> E[Pre-Deployment Checks]
    D --> E
    E --> F[Database Migrations]
    F --> G[Backup Databases]
    G --> H[Build Docker Images]
    H --> I[Run Tests]
    I --> J{Tests Pass?}
    J -->|No| K[Abort Deployment]
    J -->|Yes| L[Deploy Microservices]
    L --> M[Health Checks]
    M --> N{All Healthy?}
    N -->|No| O[Rollback]
    N -->|Yes| P[HIPAA Compliance Scan]
    P --> Q[Post-Deployment Verification]
    Q --> R[Generate Audit Report]
```

## Pre-Deployment Checks

### 1. Git Repository Validation

- Clean working directory (no uncommitted changes)
- Correct branch for environment:
  - **Production:** `master` branch only
  - **Staging:** `master` or `develop`
  - **Development:** any branch

### 2. Database Migration Validation

```bash
# Validate all service migrations
./gradlew :modules:services:patient-service:test --tests "*EntityMigrationValidationTest"
./gradlew :modules:services:care-gap-service:test --tests "*EntityMigrationValidationTest"
# ... (all 29 databases)
```

### 3. Build Verification

```bash
# Build all services
./gradlew build -x test

# Build Docker images
docker-compose build
```

### 4. HIPAA Compliance Check

- PHI encryption enabled
- Cache TTL ≤ 5 minutes
- Audit logging configured
- Multi-tenant isolation verified

## Deployment Strategies

### Rolling Deployment (Default)

- Deploy services one at a time
- Wait for health checks before next service
- Automatic rollback on failure
- Zero downtime for stateless services

### Blue-Green Deployment

- Deploy to new environment (green)
- Run smoke tests on green
- Switch traffic from blue to green
- Keep blue as rollback target

### Recreate Deployment

- Stop all services
- Deploy new versions
- Start all services
- Faster but causes downtime

## Service Deployment Order

HDIM services deployed in dependency order:

```
1. Infrastructure Services
   - PostgreSQL (29 databases)
   - Redis (cache layer)
   - Kafka (message broker)
   - Kong (API Gateway)

2. Core Services
   - patient-service
   - fhir-service
   - patient-event-service

3. Domain Services
   - quality-measure-service
   - cql-engine-service
   - care-gap-service
   - risk-stratification-service

4. Integration Services
   - hl7v2-adapter-service
   - ccda-import-service
   - documentation-service

5. Frontend Applications
   - clinical-portal (Angular)
   - admin-portal (Angular)
```

## Health Checks

Each service must pass health checks before deployment continues:

```bash
# HTTP Health Endpoint
GET http://localhost:PORT/actuator/health

# Expected Response
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" },
    "kafka": { "status": "UP" }
  }
}
```

### Critical Health Checks

1. **Database Connectivity**
   - All 29 databases reachable
   - Connection pool healthy
   - Tenant isolation verified

2. **Cache Layer**
   - Redis connection active
   - PHI cache TTL ≤ 5 minutes
   - Cache encryption enabled

3. **Message Broker**
   - Kafka brokers available
   - Event topics exist
   - Consumer groups active

4. **API Gateway**
   - Kong routes configured
   - JWT validation working
   - Rate limiting active

## Smoke Tests

Post-deployment validation:

1. **Patient Service**
   ```bash
   curl -H "Authorization: Bearer $TOKEN" \
        -H "X-Tenant-ID: test-tenant" \
        http://gateway:8001/api/v1/patients/test-patient-id
   ```

2. **FHIR Service**
   ```bash
   curl http://gateway:8001/fhir/Patient/test-patient-id
   ```

3. **Care Gap Service**
   ```bash
   curl -H "Authorization: Bearer $TOKEN" \
        -H "X-Tenant-ID: test-tenant" \
        http://gateway:8001/api/v1/care-gaps?status=OPEN
   ```

4. **Quality Measure Service**
   ```bash
   curl -H "Authorization: Bearer $TOKEN" \
        http://gateway:8001/api/v1/quality-measures
   ```

## Rollback Procedures

### Automatic Rollback Triggers

- Health checks fail after 3 retries
- Smoke tests fail
- Error rate > 5% within 5 minutes
- Database migration failure

### Manual Rollback

```bash
# Rollback specific service
/devops:rollback patient-service --to-version v2.5.1

# Rollback all services
/devops:rollback --all --to-tag production-2026-01-24

# Emergency rollback (immediate)
/devops:rollback --emergency --all
```

### Rollback Steps

1. Stop traffic to failed services
2. Revert Docker images to previous tags
3. Rollback database migrations
4. Restore Redis cache (if needed)
5. Verify rollback success
6. Resume traffic

## HIPAA Compliance Validation

Post-deployment HIPAA checks:

1. **Audit Logging**
   - `@Audited` annotations present
   - Audit events flowing to Kafka
   - Audit database receiving events

2. **PHI Encryption**
   - Database encryption at rest enabled
   - TLS for all service-to-service communication
   - Cache encryption enabled

3. **Access Controls**
   - JWT validation working
   - RBAC enforced (`@PreAuthorize`)
   - Multi-tenant isolation verified

4. **Session Management**
   - Session timeout ≤ 15 minutes
   - Auto-logout working
   - Session audit logging active

## Deployment Report

Generated at: `deployments/hdim_${ENVIRONMENT}_${TIMESTAMP}.json`

```json
{
  "environment": "production",
  "deployment_status": "success",
  "deployment_id": "hdim-prod-2026-01-25-001",
  "deployment_strategy": "rolling",
  "services_deployed": [
    {
      "name": "patient-service",
      "version": "v2.6.0",
      "status": "healthy",
      "health_check_passed": true,
      "deployment_duration_seconds": 45
    },
    {
      "name": "care-gap-service",
      "version": "v2.6.0",
      "status": "healthy",
      "health_check_passed": true,
      "deployment_duration_seconds": 52
    }
  ],
  "database_migrations": {
    "executed": 12,
    "rollback_tags_created": 12,
    "validation_passed": true
  },
  "hipaa_compliance": {
    "audit_logging": "enabled",
    "phi_encryption": "enabled",
    "cache_ttl_compliant": true,
    "access_controls": "verified"
  },
  "smoke_tests": {
    "patient_api": "passed",
    "fhir_api": "passed",
    "care_gap_api": "passed",
    "quality_measure_api": "passed",
    "authentication": "passed"
  },
  "rollback_plan": {
    "backup_created": true,
    "rollback_tag": "production-2026-01-25-pre-deployment",
    "rollback_tested": true
  },
  "deployed_at": "2026-01-25T12:34:56Z",
  "deployed_by": "aaron@mahoosuc.solutions",
  "git_commit": "2ed6c14d",
  "total_duration_seconds": 487
}
```

## Production Deployment Checklist

Before deploying to production:

- [ ] All tests passing (`./gradlew test`)
- [ ] Database migrations validated
- [ ] HIPAA compliance verified
- [ ] Staging deployment successful
- [ ] Rollback plan tested
- [ ] Team notified of deployment window
- [ ] On-call engineer available
- [ ] Database backups created
- [ ] Git tag created (`v2.6.0`)
- [ ] Deployment approval obtained

## Performance

- **Single service deployment:** 30-60 seconds
- **All 51 services (rolling):** 8-12 minutes
- **Blue-green deployment:** 15-20 minutes
- **Rollback operation:** 2-5 minutes

## Related Commands

- `/devops:monitor` - Monitor production health
- `/devops:rollback` - Emergency rollback
- `/db:migrate` - Execute database migrations
- `/test:integration` - Run integration tests
- `/auth:audit` - Audit authentication system

## See Also

- [Deployment Runbook](../docs/DEPLOYMENT_RUNBOOK.md)
- [Build Management Guide](../backend/docs/BUILD_MANAGEMENT_GUIDE.md)
- [HIPAA Compliance Guide](../backend/HIPAA-CACHE-COMPLIANCE.md)
- [Service Catalog](../docs/services/SERVICE_CATALOG.md)
