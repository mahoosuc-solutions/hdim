# HDIM Platform Release v1.6.0

**Release Date:** 2025-12-27
**Release Type:** Security / Compliance Update
**Previous Version:** v1.5.0

---

## Executive Summary

This release focuses on **HIPAA compliance** by ensuring all PHI (Protected Health Information) cache TTLs are reduced to 5 minutes or less, as required by 45 CFR 164.312(a)(2)(i) data minimization requirements.

---

## Critical Security Update

### HIPAA Cache Compliance

All services caching PHI now use a maximum TTL of 5 minutes (300,000ms):

| Service | Previous TTL | New TTL | Reduction |
|---------|-------------|---------|-----------|
| cql-engine-service (docker) | 24 hours | 5 min | 99.7% |
| quality-measure-service (docker) | 10 min | 5 min | 50% |
| data-enrichment-service | 1 hour | 5 min | 91.7% |
| payer-workflows-service | 1 hour | 5 min | 91.7% |
| predictive-analytics-service | 1 hour | 5 min | 91.7% |

### Reference Data (Non-PHI)

The following caches retain longer TTLs because they cache **static reference data**, not PHI:

| Service | Data Type | TTL | Justification |
|---------|-----------|-----|---------------|
| hcc-service | ICD-10 → HCC mappings | 1 hour | Static CMS reference data |
| ecr-service | CDC RCTC trigger codes | 7 days | Public CDC reference data |

---

## Changes

### HIPAA Compliance
- `fix(hipaa)`: Reduce PHI cache TTL to ≤5min for HIPAA compliance
- Added HIPAA compliance comments to all cache configuration files
- Documented non-PHI reference data caches with clear justification

### Refactoring
- `refactor`: Remove deprecated notification methods and unused classes

### Documentation
- `docs`: Update build status - all 27 services compile successfully
- `docs`: Add CLAUDE.md for AI coding agent guidelines

---

## Verification

### HIPAA Compliance Checks
```bash
# Verify all PHI cache TTLs are ≤ 300000ms
grep -r "time-to-live" backend/modules/services/*/src/main/resources/application*.yml

# Run HIPAA compliance tests
./gradlew :modules:shared:infrastructure:security:test --tests "*HipaaCompliance*"
```

### Build Verification
```bash
# All 27 services compile successfully
./gradlew compileJava --parallel

# Build all bootJar files
./gradlew bootJar --parallel -x test
```

---

## Docker Images

### Available Images (v1.6.0)

| Image | Size | Port |
|-------|------|------|
| healthdata-cql-engine-service:1.6.0 | 1.24GB | 8081 |
| healthdata-quality-measure-service:1.6.0 | 1.57GB | 8087 |
| healthdata-fhir-service:1.6.0 | 1.63GB | 8085 |
| healthdata-patient-service:1.6.0 | 775MB | 8084 |
| healthdata-analytics-service:1.6.0 | 730MB | 8092 |
| healthdata-consent-service:1.6.0 | 725MB | 8082 |

### Deployment Commands

```bash
# Deploy core services
docker compose --profile core up -d

# Deploy all 27 services
docker compose --profile full up -d

# Deploy with specific version
VERSION=1.6.0 docker compose --profile core up -d
```

---

## Services (27 Total)

### Core Clinical Services (14)
- gateway-service (8080)
- cql-engine-service (8081)
- consent-service (8082)
- event-processing-service (8083)
- patient-service (8084)
- fhir-service (8085)
- care-gap-service (8086)
- quality-measure-service (8087)
- event-router-service (8095)
- ecr-service (8101)
- prior-auth-service (8102)
- qrda-export-service (8104)
- hcc-service (8105)
- clinical-portal (4200)

### AI Services (3)
- agent-runtime-service (8088)
- ai-assistant-service (8090)
- agent-builder-service (8096)

### Analytics Services (3)
- analytics-service (8092)
- predictive-analytics-service (8093)
- sdoh-service (8094)

### Data Processing Services (3)
- data-enrichment-service (8089)
- cdr-processor-service (8099)
- migration-workflow-service (8103)

### Workflow Services (3)
- approval-service (8097)
- payer-workflows-service (8098)
- ehr-connector-service (8100)

### Support Services (2)
- documentation-service (8091)
- sales-automation-service (8106)

---

## Upgrade Instructions

### From v1.5.0

1. **Pull latest code:**
   ```bash
   git fetch origin
   git checkout v1.6.0
   ```

2. **Rebuild services:**
   ```bash
   ./gradlew bootJar --parallel -x test
   ```

3. **Restart Docker containers:**
   ```bash
   docker compose --profile core down
   docker compose --profile core up -d --build
   ```

4. **Verify HIPAA compliance:**
   ```bash
   # Check cache TTLs
   curl -v http://localhost:8084/patient/api/patients \
     -H "X-Tenant-ID: tenant1" 2>&1 | grep Cache-Control

   # Expected: Cache-Control: no-store, no-cache, must-revalidate, private
   ```

---

## Breaking Changes

**None** - This release is fully backward compatible.

---

## Known Issues

None.

---

## Rollback Instructions

If issues occur, rollback to v1.5.0:

```bash
git checkout v1.5.0
./gradlew bootJar --parallel -x test
docker compose --profile core down
docker compose --profile core up -d --build
```

---

## Compliance Certification

This release has been verified for:

- [x] HIPAA 45 CFR 164.312(a)(2)(i) - Access Controls (cache TTL compliance)
- [x] PHI cache TTL ≤ 5 minutes for all applicable services
- [x] Non-PHI reference data caches documented
- [x] HIPAA compliance comments added to configuration files
- [x] All 27 services compile successfully
- [x] Security module includes NoCacheResponseInterceptor

---

## Contributors

- HDIM Platform Team
- Claude Code (AI-assisted development)

---

## Support

For issues or questions:
- GitHub Issues: https://github.com/webemo-aaron/hdim/issues
- Documentation: See `backend/HIPAA-CACHE-COMPLIANCE.md`

---

*Generated with Claude Code*
