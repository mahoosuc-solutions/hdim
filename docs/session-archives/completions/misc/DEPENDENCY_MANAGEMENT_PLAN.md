# HDIM Dependency Management Plan

**Date**: January 10, 2026
**Status**: Draft for Review
**Purpose**: Standardize dependency versions across all 28 microservices

---

## Executive Summary

The HDIM platform currently has **mixed dependency versions** across services due to:
1. Services being developed at different times
2. Spring Boot 3.5.9 being pulled in (newer than Spring Cloud supports)
3. Jackson 2.20.1 specified but causing build issues in some contexts
4. Need for unified dependency management via version catalog

**Recommendation**: Standardize all services on **Spring Boot 3.3.6** with **Jackson 2.18.2** (or latest 2.18.x) and **Spring Cloud 2023.0.6**.

---

## Current State Analysis

### Version Catalog (backend/gradle/libs.versions.toml)

```toml
[versions]
spring-boot = "3.3.6"     # Target version
spring-cloud = "2023.0.6"  # Compatible with 3.3.x
jackson = "2.20.1"         # TOO NEW - causing issues
```

### Discovered Issues

1. **Spring Boot Version Mismatch**
   - Version catalog specifies: 3.3.6
   - Some services pulling: 3.5.9 (from Spring Boot BOM)
   - Spring Cloud supports: 3.2.x - 3.3.x only

2. **Jackson Version Issue**
   - Specified: 2.20.1 (exists in Maven Central)
   - Build failures suggest dependency resolution conflicts
   - Spring Boot 3.3.6 uses Jackson 2.17.x by default

3. **Service Inconsistency**
   - 28 microservices may have different resolved versions
   - Shared modules enforce versions via resolutionStrategy
   - Need verification of actual runtime versions

---

## Recommended Dependency Versions

### Core Framework

| Dependency | Current | Recommended | Rationale |
|------------|---------|-------------|-----------|
| Spring Boot | 3.3.6 (catalog)<br/>3.5.9 (runtime) | **3.3.6** | Latest stable version supported by Spring Cloud 2023.0.x |
| Spring Cloud | 2023.0.6 | **2023.0.6** | Keep current - latest stable |
| Java | 21 | **21 LTS** | Keep current - long-term support |

### Data & Serialization

| Dependency | Current | Recommended | Rationale |
|------------|---------|-------------|-----------|
| Jackson | 2.20.1 | **2.18.2** | Latest stable 2.18.x; 2.20.x too new for ecosystem |
| PostgreSQL Driver | 42.7.7 | **42.7.7** | Keep current - latest stable |
| Hibernate | 6.6.1.Final | **6.6.1.Final** | Keep current - compatible with Spring Boot 3.3.6 |
| Liquibase | 4.29.2 | **4.29.2** | Keep current - working well |

### Healthcare Libraries

| Dependency | Current | Recommended | Rationale |
|------------|---------|-------------|-----------|
| HAPI FHIR | 7.6.0 | **7.6.0** | Keep current - latest R4 version |
| CQL Engine | 3.3.1 | **3.3.1** | Keep current - stable |

### Caching & Messaging

| Dependency | Current | Recommended | Rationale |
|------------|---------|-------------|-----------|
| Redis (Spring) | 3.5.7 | **3.5.7** | Keep current |
| Kafka | 3.8.0 | **3.8.0** | Keep current |
| Spring Kafka | 3.3.11 | **3.3.11** | Keep current |

### Observability

| Dependency | Current | Recommended | Rationale |
|------------|---------|-------------|-----------|
| Micrometer | 1.13.6 | **1.13.6** | Keep current |
| OpenTelemetry | 1.32.0 | **1.32.0** | Keep current |

---

## Root Cause Analysis: Spring Boot 3.5.9

### Why is 3.5.9 Being Pulled?

**Investigation needed**:
```bash
# Check what's pulling Spring Boot 3.5.9
cd backend
./gradlew :modules:services:care-gap-service:dependencies --configuration runtimeClasspath \
  | grep "spring-boot-starter-web"
```

**Possible causes**:
1. **Spring Boot BOM override** - A newer BOM is being imported somewhere
2. **Transitive dependency** - Another library depends on Spring Boot 3.5.x
3. **Gradle plugin version** - The Spring Boot Gradle plugin version differs from dependencies

**Fix**:
```kotlin
// In build.gradle.kts root file, enforce version:
subprojects {
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.springframework.boot") {
                useVersion(libs.versions.spring.boot.get())
            }
        }
    }
}
```

---

## Root Cause Analysis: Jackson 2.20.1

### Why is Jackson 2.20.1 Failing?

**Evidence**:
- Jackson 2.20.1 EXISTS in Maven Central (confirmed)
- Build fails with "Could not find jackson-annotations:2.20.1"
- Transitive dependency issue, not direct resolution failure

**Possible causes**:
1. **Version mismatch in BOM** - Spring Boot 3.3.6's BOM declares Jackson 2.17.x
2. **Conflicting resolution** - Our resolutionStrategy forces 2.20.1, but Spring Boot's BOM fights back
3. **Cache corruption** - Gradle dependency cache might be stale

**Recommended fix**:
```toml
# backend/gradle/libs.versions.toml
jackson = "2.18.2"  # Change from 2.20.1 to 2.18.2
```

**Rationale**:
- Jackson 2.18.x is mature and stable
- Compatible with Spring Boot 3.3.6's expectations
- Avoids bleeding-edge issues from 2.20.x
- Still gets security patches and features from 2.18 branch

---

## Implementation Plan

### Phase 1: Update Version Catalog (Week 1)

**File**: `backend/gradle/libs.versions.toml`

```toml
[versions]
spring-boot = "3.3.6"     # Enforce this version
spring-cloud = "2023.0.6" # Compatible
jackson = "2.18.2"        # Downgrade from 2.20.1
```

**Action**:
```bash
# Update version catalog
vim backend/gradle/libs.versions.toml

# Verify all services use catalog versions
./gradlew dependencies --configuration runtimeClasspath > deps-before.txt
```

### Phase 2: Enforce Spring Boot 3.3.6 (Week 1)

**File**: `backend/build.gradle.kts`

Add to `subprojects` block:
```kotlin
configurations.all {
    resolutionStrategy.eachDependency {
        // Enforce Spring Boot version from catalog
        if (requested.group == "org.springframework.boot") {
            useVersion(libs.versions.spring.boot.get())
        }
        // Already enforcing Jackson - just update catalog version
        if (requested.group == "com.fasterxml.jackson.core") {
            useVersion(libs.versions.jackson.get())
        }
    }
}
```

### Phase 3: Remove Spring Cloud Compatibility Check Workarounds (Week 1)

**After** Spring Boot is enforced at 3.3.6:

1. Remove from `care-gap-service/src/main/resources/application.yml`:
   ```yaml
   spring:
     cloud:
       compatibility-verifier:
         enabled: false  # REMOVE THIS
   ```

2. Rebuild and verify compatibility check passes

### Phase 4: Verify All Services (Week 2)

**For each of 28 services**:

1. **Check resolved versions**:
   ```bash
   ./gradlew :modules:services:SERVICE-NAME:dependencies \
     --configuration runtimeClasspath \
     | grep -E "spring-boot|jackson-databind"
   ```

2. **Verify build succeeds**:
   ```bash
   ./gradlew :modules:services:SERVICE-NAME:build
   ```

3. **Test Docker image**:
   ```bash
   docker compose build SERVICE-NAME
   docker compose up SERVICE-NAME
   # Check logs for Spring Boot version
   ```

### Phase 5: Update Documentation (Week 2)

**Files to update**:
- `CLAUDE.md` - Add dependency management section
- `DATABASE_ARCHITECTURE_MIGRATION_PLAN.md` - Note Jackson version change
- `README.md` - Update dependency versions in overview

---

## Service-by-Service Checklist

Track which services have been verified:

### Core Services (Priority 1)
- [ ] gateway-service
- [ ] fhir-service
- [ ] patient-service
- [ ] quality-measure-service
- [ ] cql-engine-service
- [ ] care-gap-service

### Clinical Services (Priority 2)
- [ ] consent-service
- [ ] notification-service
- [ ] event-router-service
- [ ] event-processing-service

### Integration Services (Priority 3)
- [ ] ehr-connector-service
- [ ] cms-connector-service
- [ ] qrda-export-service
- [ ] hcc-service
- [ ] prior-auth-service
- [ ] ecr-service

### Supporting Services (Priority 4)
- [ ] analytics-service
- [ ] predictive-analytics-service
- [ ] sdoh-service
- [ ] demo-seeding-service
- [ ] sales-automation-service
- [ ] documentation-service
- [ ] migration-workflow-service
- [ ] ai-assistant-service
- [ ] agent-runtime-service
- [ ] agent-builder-service
- [ ] approval-service
- [ ] gateway-admin-service
- [ ] gateway-clinical-service
- [ ] gateway-fhir-service

---

## Testing Strategy

### Unit Tests
```bash
# All services
./gradlew test

# Should pass with updated dependencies
```

### Integration Tests
```bash
# Docker Compose deployment
docker compose --profile core up -d

# Verify services start and connect
docker compose ps
docker compose logs | grep "Started.*Application"
```

### Dependency Verification
```bash
# Generate dependency report
./gradlew dependencyInsight \
  --configuration runtimeClasspath \
  --dependency spring-boot

# Should show consistent 3.3.6 across all services
```

---

## Rollback Plan

If issues arise:

1. **Revert version catalog**:
   ```bash
   git checkout HEAD^ -- backend/gradle/libs.versions.toml
   ```

2. **Revert build.gradle.kts changes**:
   ```bash
   git checkout HEAD^ -- backend/build.gradle.kts
   ```

3. **Rebuild**:
   ```bash
   ./gradlew clean build
   ```

---

## Success Criteria

✅ **All services build successfully** with:
- Spring Boot 3.3.6
- Jackson 2.18.2
- Spring Cloud 2023.0.6

✅ **All services start in Docker** with no compatibility errors

✅ **No dependency resolution failures** during build

✅ **Spring Cloud compatibility check passes** (can remove `enabled: false` workaround)

✅ **Patient-service JSONB fix works** (entity-migration sync validated)

---

## Next Steps

1. **Update version catalog** (Jackson 2.20.1 → 2.18.2)
2. **Add Spring Boot enforcement** to build.gradle.kts
3. **Test with 3 pilot services** (gateway, patient, quality-measure)
4. **Roll out to remaining 25 services**
5. **Remove compatibility check workarounds**
6. **Document final state** in CLAUDE.md

---

## Questions for Review

1. **Should we downgrade Jackson to 2.17.x** (Spring Boot default) instead of 2.18.2?
2. **Are there specific features in Jackson 2.20.1** we need, or can we use 2.18.x?
3. **Should we upgrade to Spring Boot 3.4.x** (latest) or stay on 3.3.6 (stable)?
4. **Do we need separate dependency versions** for different service groups?

---

**Author**: HDIM Platform Team
**Reviewers**: Architecture, DevOps, Security
**Status**: Awaiting approval to proceed with Phase 1
