# Phase 17: Infrastructure Requirements for Remaining Services

**Date:** 2025-11-06
**Status:** REQUIRES ACTION
**Priority:** HIGH

## Overview

This document outlines the infrastructure requirements to complete tenant isolation implementation across all backend services. Two services require additional work beyond the security fix already applied.

## Service Summary

| Service | Tenant Isolation Fix | Database Setup | User Infrastructure | Status |
|---------|---------------------|----------------|---------------------|---------|
| **cql-engine-service** | ✅ Applied & Tested | ✅ Complete | ✅ Complete | **PRODUCTION READY** |
| **care-gap-service** | ✅ Applied | ❌ Missing DB | ✅ Complete | **BLOCKED - Needs Database** |
| **quality-measure-service** | ❌ N/A | ❌ Missing DB | ❌ Missing | **BLOCKED - Needs Full Auth** |

---

## 1. care-gap-service Infrastructure Requirements

### Current Status
- **Security Fix:** ✅ Applied (commit 8983487)
- **Code:** Fully implemented with User entity, TenantAccessFilter, SecurityConfig
- **Database:** ❌ NOT CONFIGURED
- **Blocking Issue:** Database infrastructure not set up in docker-compose.yml

### What Exists
```java
// User entity and authentication infrastructure present
backend/modules/services/care-gap-service/src/main/java/com/healthdata/caregap/
├── entity/
│   ├── User.java                    ✅ EXISTS
│   └── UserRole.java                ✅ EXISTS
├── repository/
│   └── UserRepository.java          ✅ EXISTS
├── security/
│   ├── CustomUserDetailsService.java    ✅ EXISTS
│   └── TenantAccessFilter.java          ✅ EXISTS (FIXED)
└── config/
    └── SecurityConfig.java              ✅ EXISTS (FIXED)
```

### What's Missing

#### Database Configuration
**Current docker-compose.yml:**
```yaml
postgres:
  container_name: healthdata-postgres
  ports:
    - "5435:5432"
  environment:
    POSTGRES_DB: healthdata_cql  # ← Only CQL database exists
```

**care-gap-service expects:**
```yaml
# From application.yml
datasource:
  url: jdbc:postgresql://localhost:5435/healthdata_care_gap  # ← This DB doesn't exist
  username: healthdata
  password: dev_password
```

### Solution Options

#### Option A: Shared Database (RECOMMENDED - Quick)
**Pros:**
- Minimal infrastructure changes
- Reuses existing database container
- Care gap data isolated in separate schema/tables
- Users shared between services (single source of truth)

**Cons:**
- Not true service isolation
- Cross-service database dependency

**Implementation:**
1. Update care-gap-service `application.yml` to use `healthdata_cql` database
2. Start service to run Liquibase migrations
3. Seed test users (or share users from cql-engine-service)
4. Test tenant isolation

```yaml
# backend/modules/services/care-gap-service/src/main/resources/application.yml
datasource:
  url: jdbc:postgresql://localhost:5435/healthdata_cql  # Changed from healthdata_care_gap
  username: healthdata
  password: ${DB_PASSWORD:dev_password}
```

**Estimated Effort:** 30 minutes

#### Option B: Separate Database (TRUE ISOLATION)
**Pros:**
- True service isolation
- Independent scaling
- Follows microservices best practices

**Cons:**
- Requires docker-compose changes
- Duplicate user data across services
- More complex infrastructure

**Implementation:**
1. Add new PostgreSQL service to docker-compose.yml
```yaml
care-gap-postgres:
  image: postgres:16-alpine
  container_name: healthdata-care-gap-postgres
  ports:
    - "5436:5432"  # Different port
  environment:
    POSTGRES_DB: healthdata_care_gap
    POSTGRES_USER: healthdata
    POSTGRES_PASSWORD: dev_password
```

2. Update care-gap-service application.yml port to 5436
3. Start database container
4. Start service to run migrations
5. Seed test users
6. Test tenant isolation

**Estimated Effort:** 1-2 hours

### Recommended Action
**Use Option A (Shared Database)** for immediate deployment. Migrate to Option B when implementing centralized authentication service (Phase 18+).

---

## 2. quality-measure-service Infrastructure Requirements

### Current Status
- **Security Fix:** N/A (no tenant isolation infrastructure)
- **Code:** Missing entire authentication system
- **Database:** Configured but only has measure tables
- **Blocking Issue:** No User entity, no TenantAccessFilter, no authentication

### What Exists
```
backend/modules/services/quality-measure-service/
├── src/main/resources/db/changelog/
│   └── 0001-create-quality-measure-results-table.xml  ✅ Only measure tables
├── config/
│   └── SecurityConfig.java  ⚠️ Has Basic Auth enabled but NO user store
└── entity/
    └── QualityMeasureResultEntity.java  ✅ Only measure entities
```

### What's Missing

#### Complete Authentication Infrastructure
- ❌ User entity
- ❌ UserRole entity
- ❌ UserRepository
- ❌ CustomUserDetailsService
- ❌ TenantAccessFilter
- ❌ Liquibase migrations for user tables
- ❌ Test user seed data

### Solution Options

#### Option 1: Duplicate Authentication Infrastructure (QUICK - RECOMMENDED)
**Implementation Steps:**

1. **Copy authentication code from cql-engine-service:**
   ```bash
   # Copy entities
   cp backend/modules/services/cql-engine-service/src/main/java/com/healthdata/cql/entity/User.java \
      backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/entity/

   cp backend/modules/services/cql-engine-service/src/main/java/com/healthdata/cql/entity/UserRole.java \
      backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/entity/

   # Copy repository
   cp backend/modules/services/cql-engine-service/src/main/java/com/healthdata/cql/repository/UserRepository.java \
      backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/repository/

   # Copy security classes
   cp backend/modules/services/cql-engine-service/src/main/java/com/healthdata/cql/security/CustomUserDetailsService.java \
      backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/security/

   cp backend/modules/services/cql-engine-service/src/main/java/com/healthdata/cql/security/TenantAccessFilter.java \
      backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/security/
   ```

2. **Update package names** in copied files from `com.healthdata.cql.*` to `com.healthdata.quality.*`

3. **Copy Liquibase migrations for user tables:**
   ```bash
   cp backend/modules/services/cql-engine-service/src/main/resources/db/migration/V001__create_users_table.sql \
      backend/modules/services/quality-measure-service/src/main/resources/db/changelog/0002-create-users-tables.xml
   ```

4. **Update SecurityConfig.java** to add TenantAccessFilter:
   ```java
   // Add to constructor
   private final TenantAccessFilter tenantAccessFilter;

   // Add filter chain positioning (with CORRECT filter class)
   .addFilterAfter(tenantAccessFilter, org.springframework.security.web.authentication.www.BasicAuthenticationFilter.class);
   ```

5. **Update database configuration** to use shared database:
   ```yaml
   # Use same database as cql-engine-service for shared users
   datasource:
     url: jdbc:postgresql://localhost:5435/healthdata_cql
   ```

6. **Start service** to run migrations

7. **Test tenant isolation** (users already seeded from cql-engine-service)

**Estimated Effort:** 2-3 hours

**Pros:**
- Fast implementation
- Reuses proven code
- Matches existing architecture
- Shared user database with cql-engine-service (single source of truth)

**Cons:**
- Code duplication
- Long-term maintenance burden

#### Option 2: Shared User Database with JPA Configuration (MODERATE)
**Implementation Steps:**

1. Configure dual data sources:
   - Primary: `healthdata_quality_measure` for measure data
   - Secondary: `healthdata_cql` for user authentication

2. Create User entities that reference cql database

3. Implement TenantAccessFilter

4. Update SecurityConfig

**Estimated Effort:** 4-6 hours

**Pros:**
- No code duplication
- Single source of truth for users

**Cons:**
- Complex JPA configuration
- Cross-database dependencies
- Violates service isolation

#### Option 3: Centralized Authentication Service (BEST LONG-TERM)
See `PHASE_17_SECURITY_AUDIT.md` for full details on implementing centralized auth service with JWT tokens.

**Estimated Effort:** 1-2 days

**Recommendation:** Not for Phase 17 - defer to Phase 18+

### Recommended Action
**Use Option 1 (Duplicate Infrastructure)** with shared database for immediate deployment. This provides:
- Quick implementation
- Production-ready security
- Shared users with cql-engine-service
- Foundation for future JWT migration

---

## 3. Docker Compose Infrastructure Summary

### Current State
```yaml
# docker-compose.yml
postgres:
  container_name: healthdata-postgres
  ports:
    - "5435:5432"
  environment:
    POSTGRES_DB: healthdata_cql  # Only one database
```

**Services using this database:**
- ✅ cql-engine-service (port 5433 via separate container)
- ✅ Can be used by care-gap-service (share database)
- ✅ Can be used by quality-measure-service (share database)

### Proposed Shared Database Approach

**All services use `healthdata_cql` database:**
```
healthdata_cql (port 5435)
├── CQL Engine tables (cql_libraries, cql_evaluations, value_sets)
├── User tables (users, user_roles, user_tenant_access) ← SHARED
├── Care Gap tables (care_gaps, gap_analysis_results)
└── Quality Measure tables (quality_measure_results)
```

**Advantages:**
- Single database for development
- Shared user authentication
- Simple infrastructure
- Fast to implement

**Database Sizing:**
- Users: < 1000 rows (shared)
- CQL data: 10k-100k rows
- Care gap data: 100k-1M rows
- Quality measure data: 1M-10M rows
- **Total:** Small enough for single Postgres instance

### Future State (Phase 18+)

**Centralized Authentication Service:**
```
auth-service-postgres (port 5437)
└── User tables only

cql-engine-postgres (port 5433)
└── CQL data only

care-gap-postgres (port 5436)
└── Care gap data only

quality-measure-postgres (port 5435)
└── Quality measure data only
```

All services validate JWT tokens from auth-service, no local user storage.

---

## 4. Implementation Roadmap

### Phase 17 (IMMEDIATE - This Week)

**Priority 1: care-gap-service Database**
- [ ] Decision: Use Option A (shared database)
- [ ] Update application.yml database URL
- [ ] Start service and verify Liquibase migrations
- [ ] Test tenant isolation with shared users
- [ ] Commit and document

**Estimated Time:** 30 minutes

**Priority 2: quality-measure-service Authentication**
- [ ] Copy authentication infrastructure from cql-engine-service
- [ ] Update package names
- [ ] Copy/adapt Liquibase migrations
- [ ] Update SecurityConfig to add TenantAccessFilter
- [ ] Configure shared database
- [ ] Start service and verify migrations
- [ ] Test tenant isolation
- [ ] Commit and document

**Estimated Time:** 2-3 hours

**Total Phase 17 Effort:** 3-4 hours

### Phase 18 (NEXT SPRINT - Next 2 Weeks)

**Add Security Testing**
- [ ] Create integration tests for tenant isolation
- [ ] Add tests to CI/CD pipeline
- [ ] Implement security monitoring
- [ ] Add alert on tenant violations

**Estimated Time:** 1 day

### Phase 19 (NEXT QUARTER - 1-2 Months)

**Design Centralized Authentication**
- [ ] Architecture design for auth-service
- [ ] JWT token specification
- [ ] Migration plan from Basic Auth
- [ ] Database separation strategy

**Estimated Time:** 2-3 days (design only)

### Phase 20+ (LONG-TERM - 3-6 Months)

**Implement Centralized Authentication**
- [ ] Build auth-service
- [ ] Implement JWT generation/validation
- [ ] Migrate services one by one
- [ ] Separate databases
- [ ] Deprecate Basic Auth
- [ ] OAuth2/OIDC integration

**Estimated Time:** 2-3 weeks

---

## 5. Decision Matrix

### For care-gap-service

| Factor | Shared DB (Option A) | Separate DB (Option B) |
|--------|---------------------|------------------------|
| **Implementation Time** | ⭐⭐⭐⭐⭐ (30 min) | ⭐⭐⭐ (1-2 hours) |
| **Service Isolation** | ⭐⭐ (coupled) | ⭐⭐⭐⭐⭐ (isolated) |
| **Operational Complexity** | ⭐⭐⭐⭐⭐ (simple) | ⭐⭐⭐ (more containers) |
| **Production Ready** | ✅ Yes | ✅ Yes |
| **Future Migration Path** | ✅ Easy to JWT | ✅ Easy to JWT |

**RECOMMENDATION:** Option A (Shared Database) - fast path to production

### For quality-measure-service

| Factor | Duplicate (Option 1) | Dual Datasource (Option 2) | Auth Service (Option 3) |
|--------|---------------------|---------------------------|------------------------|
| **Implementation Time** | ⭐⭐⭐⭐ (2-3 hours) | ⭐⭐⭐ (4-6 hours) | ⭐ (1-2 days) |
| **Code Maintenance** | ⭐⭐ (duplication) | ⭐⭐⭐⭐ (no duplication) | ⭐⭐⭐⭐⭐ (best) |
| **Operational Complexity** | ⭐⭐⭐⭐⭐ (simple) | ⭐⭐⭐ (complex JPA) | ⭐⭐ (new service) |
| **Production Ready** | ✅ Today | ✅ Today | ❌ Weeks away |
| **Future Migration Path** | ✅ Easy to JWT | ⭐⭐⭐ Harder | N/A (is the future) |

**RECOMMENDATION:** Option 1 (Duplicate Infrastructure) - balance of speed and quality

---

## 6. Risk Assessment

### Risks of Shared Database Approach

| Risk | Severity | Mitigation |
|------|----------|------------|
| **Service coupling** | MEDIUM | Plan migration to separate DBs in Phase 19+ |
| **Scale limitations** | LOW | Single Postgres handles expected load |
| **User data consistency** | LOW | Actually a benefit - single source of truth |
| **Migration complexity** | LOW | JWT migration is service-by-service |

### Risks of Delayed Implementation

| Risk | Severity | Impact |
|------|----------|--------|
| **care-gap-service unusable** | HIGH | Service cannot start without database |
| **quality-measure-service insecure** | CRITICAL | No authentication = data breach |
| **HIPAA compliance** | CRITICAL | Multi-tenancy not enforced |
| **Regulatory audit failure** | CRITICAL | Could block production deployment |

### Conclusion
**Benefits of implementing Phase 17 FAR OUTWEIGH risks.** Shared database is pragmatic solution for now.

---

## 7. Testing Checklist

### care-gap-service Testing
- [ ] Service starts successfully
- [ ] Liquibase migrations execute
- [ ] Users accessible from shared database
- [ ] Tenant isolation blocks unauthorized access (403)
- [ ] Tenant isolation allows authorized access (200)
- [ ] Multi-tenant users can access all their tenants
- [ ] Logs show TenantAccessFilter executing after authentication

### quality-measure-service Testing
- [ ] Service starts successfully
- [ ] Liquibase migrations execute
- [ ] User tables created
- [ ] Users accessible
- [ ] Authentication works (401 without credentials)
- [ ] Tenant isolation blocks unauthorized access (403)
- [ ] Tenant isolation allows authorized access (200)
- [ ] Multi-tenant users can access all their tenants
- [ ] Logs show TenantAccessFilter executing after authentication

---

## 8. Next Steps

1. **Get stakeholder approval** on shared database approach
2. **Implement care-gap-service database** (30 minutes)
3. **Implement quality-measure-service authentication** (2-3 hours)
4. **Test thoroughly** (1 hour)
5. **Document and commit** (30 minutes)
6. **Begin planning Phase 18** security testing

**Total remaining work: ~4-5 hours**

---

## Appendix A: File Checklist for quality-measure-service

Files to create/modify:

```
backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/
├── entity/
│   ├── User.java                          ← CREATE (copy from cql-engine-service)
│   └── UserRole.java                      ← CREATE (copy from cql-engine-service)
├── repository/
│   └── UserRepository.java                ← CREATE (copy from cql-engine-service)
├── security/
│   ├── CustomUserDetailsService.java      ← CREATE (copy from cql-engine-service)
│   └── TenantAccessFilter.java            ← CREATE (copy from cql-engine-service)
└── config/
    └── SecurityConfig.java                ← MODIFY (add TenantAccessFilter)

backend/modules/services/quality-measure-service/src/main/resources/
├── db/changelog/
│   ├── 0002-create-users-table.xml        ← CREATE (adapt from cql-engine-service)
│   ├── 0003-create-user-roles-table.xml   ← CREATE (adapt from cql-engine-service)
│   ├── 0004-create-user-tenant-access.xml ← CREATE (adapt from cql-engine-service)
│   ├── 0005-seed-test-users.xml           ← CREATE (adapt from cql-engine-service)
│   └── db.changelog-master.xml            ← MODIFY (include new migrations)
└── application.yml                         ← MODIFY (update database URL)
```

---

**Document Version:** 1.0
**Last Updated:** 2025-11-06
**Status:** Awaiting Implementation
**Owner:** Security Team
