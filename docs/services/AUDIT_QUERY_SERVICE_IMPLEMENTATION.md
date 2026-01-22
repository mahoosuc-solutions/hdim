# Audit Query Service - Implementation Status

**Last Updated:** January 22, 2026
**Status:** 🔴 BLOCKED - Service crash-looping at startup

---

## Current Blocker

The audit-query-service cannot start due to `AuditAutoConfiguration` from the shared audit module auto-registering ALL 8 repositories, including `ClinicalDecisionRepository`, which contains a query method with incompatible type usage:

```java
// From ClinicalDecisionRepository
@Query("""
    SELECT AVG(
        EXTRACT(EPOCH FROM (cde.reviewedAt - cde.eventTimestamp)) / 3600.0
    )
    FROM ClinicalDecisionEventEntity cde
    WHERE cde.reviewedAt IS NOT NULL
    AND cde.tenantId = :tenantId
""")
Double getAverageReviewTimeHours(@Param("tenantId") String tenantId);
```

**Error:** `FunctionArgumentException: Parameter 2 of function 'extract()' has type 'TEMPORAL', but argument is of type 'java.time.Duration' mapped to 'DURATION'`

The query attempts to use `EXTRACT(EPOCH FROM duration_field)`, but PostgreSQL's EXTRACT function requires TIMESTAMP/TIMESTAMPTZ types, not DURATION/INTERVAL types.

---

## Attempted Solutions

### ❌ Option 1: ddl-auto: update + Component Exclusions

**Changes Made:**
1. Set `spring.jpa.hibernate.ddl-auto: update` in application.yml
2. Added `spring.main.allow-bean-definition-overriding: true`
3. Excluded audit service and controller classes via `@ComponentScan.Filter`
4. Removed `com.healthdata.audit.repository` from `@EnableJpaRepositories`

**Result:** Service still crash-loops. `AuditAutoConfiguration`'s `@EnableJpaRepositories` auto-discovers all repositories in the `com.healthdata.audit.repository` package during Spring context initialization, triggering query validation before bean overriding can take effect.

**Root Cause:** Spring Data JPA validates repository query methods during context initialization, which happens BEFORE bean overriding. The problematic query in `ClinicalDecisionRepository` fails validation regardless of exclusion filters.

---

## Available Solutions

### ✅ Option 2: Architectural Refactoring (RECOMMENDED)

**Approach:** Separate entity packages to prevent accidental scanning.

**Changes Required:**

1. **Restructure audit module packages:**
   ```
   backend/modules/shared/audit/
   ├── entity/
   │   └── shared/
   │       └── AuditEventEntity.java          # Shared by all services
   │   └── service-specific/
   │       ├── QAReviewEntity.java            # qa-service only
   │       ├── AIAgentDecisionEventEntity.java # ai-agent-service only
   │       ├── ClinicalDecisionEventEntity.java # clinical-decision-service only
   │       └── ... (other service-specific entities)
   └── repository/
       └── shared/
           └── AuditEventRepository.java       # Shared by all services
       └── service-specific/
           └── ... (move all service-specific repos here)
   ```

2. **Update audit-query-service configuration:**
   ```java
   @EntityScan(basePackages = {
       "com.healthdata.audit.entity.shared",  // Only scan shared entities
       "com.healthdata.auditquery.persistence"
   })
   @EnableJpaRepositories(basePackages = {
       "com.healthdata.audit.repository.shared",  // Only shared repos
       "com.healthdata.auditquery.repository"
   })
   ```

3. **Update other services:**
   - ai-agent-service → scan `entity.shared` + `entity.service-specific.ai`
   - qa-service → scan `entity.shared` + `entity.service-specific.qa`
   - clinical-decision-service → scan `entity.shared` + `entity.service-specific.clinical`

**Benefits:**
- ✅ Clean architectural separation
- ✅ No accidental entity/repository discovery
- ✅ Each service only loads what it needs
- ✅ Follows Spring Boot best practices
- ✅ No schema pollution

**Effort:** ~2-3 hours (file moves + configuration updates)

---

### ⚠️ Option 3: Copy Shared Entity (QUICK FIX)

**Approach:** Copy `AuditEventEntity` into audit-query-service to avoid shared package scanning.

**Changes Required:**

1. **Copy entity to audit-query-service:**
   ```bash
   mkdir -p backend/modules/services/audit-query-service/src/main/java/com/healthdata/auditquery/entity
   cp backend/modules/shared/audit/src/main/java/com/healthdata/audit/entity/AuditEventEntity.java \
      backend/modules/services/audit-query-service/src/main/java/com/healthdata/auditquery/entity/
   ```

2. **Update package declaration:**
   ```java
   package com.healthdata.auditquery.entity;  // Changed from com.healthdata.audit.entity
   ```

3. **Update audit-query-service imports:**
   - Change all references from `com.healthdata.audit.entity.AuditEventEntity` to `com.healthdata.auditquery.entity.AuditEventEntity`

4. **Remove shared entity scanning:**
   ```java
   @EntityScan(basePackages = {
       "com.healthdata.auditquery.entity",      // Local copy
       "com.healthdata.auditquery.persistence"
   })
   @EnableJpaRepositories(basePackages = {
       "com.healthdata.auditquery.repository"
   })
   ```

5. **Set ddl-auto back to validate:**
   ```yaml
   spring:
     jpa:
       hibernate:
         ddl-auto: validate  # Restore strict validation
   ```

**Benefits:**
- ✅ Quick implementation (~30 minutes)
- ✅ Avoids architectural changes
- ✅ No schema pollution
- ✅ Service starts successfully

**Drawbacks:**
- ❌ Code duplication (entity maintained in 2 places)
- ❌ Technical debt (future changes require 2 updates)
- ❌ Not architecturally clean

**When to Use:** If you need the service running TODAY for testing/demo purposes.

---

## Recommendation

**Choose Option 2 (Architectural Refactoring)** if you have time for proper implementation.

**Choose Option 3 (Copy Entity)** if you need immediate deployment.

---

## Current Configuration State

### application.yml
```yaml
spring:
  application:
    name: audit-query-service
  main:
    allow-bean-definition-overriding: true

  jpa:
    hibernate:
      ddl-auto: update  # Currently set to update (Option 1 attempt)
```

### AuditQueryServiceApplication.java
```java
@ComponentScan(
    basePackages = {
        "com.healthdata.auditquery",
        "com.healthdata.audit",  // Still scanning shared audit package
        "com.healthdata.authentication",
        "com.healthdata.security",
        "com.healthdata.persistence"
    },
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.healthdata\\.audit\\.service\\..*"
        ),
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.healthdata\\.audit\\.controller\\..*"
        )
    }
)
@EntityScan(basePackages = {
    "com.healthdata.audit.entity",  // Scanning ALL entities (problem)
    "com.healthdata.auditquery.persistence"
})
@EnableJpaRepositories(basePackages = {
    "com.healthdata.auditquery.repository"
})
```

---

## Next Steps

**If choosing Option 2:**
1. Restructure audit module packages (entity/shared, entity/service-specific, repository/shared, repository/service-specific)
2. Update AuditAutoConfiguration to scan only shared packages
3. Update audit-query-service to scan only shared packages
4. Update all other services to scan shared + service-specific packages
5. Set `ddl-auto: validate` in audit-query-service
6. Test service startup

**If choosing Option 3:**
1. Copy AuditEventEntity to audit-query-service
2. Update package declaration and imports
3. Remove shared entity scanning from @EntityScan
4. Set `ddl-auto: validate` in application.yml
5. Rebuild and test service startup

---

## Related Files

- **Service Application:** `backend/modules/services/audit-query-service/src/main/java/com/healthdata/auditquery/AuditQueryServiceApplication.java`
- **Configuration:** `backend/modules/services/audit-query-service/src/main/resources/application.yml`
- **Docker Compose:** `docker-compose.yml` (audit-query-service section)
- **Shared Entities:** `backend/modules/shared/audit/src/main/java/com/healthdata/audit/entity/`
- **Shared Repositories:** `backend/modules/shared/audit/src/main/java/com/healthdata/audit/repository/`
- **AuditAutoConfiguration:** `backend/modules/shared/audit/src/main/java/com/healthdata/audit/config/AuditAutoConfiguration.java`
