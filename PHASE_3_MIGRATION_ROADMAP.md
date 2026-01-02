# Phase 3 Migration Status & Roadmap

**Date**: December 30, 2025
**Phase**: 3 of 4 (Remaining Services)
**Status**: Ready for planning

---

## Authentication Migration Status Summary

### ✅ Phase 1 & 2 Complete (10 Services)

**Gateway-Trust (TrustedHeaderAuthFilter) - 10 Services**:
1. ✅ Quality Measure Service (8087)
2. ✅ Care Gap Service (8086)
3. ✅ Patient Service (8084)
4. ✅ CQL Engine Service (8081)
5. ✅ Consent Service (8082)
6. ✅ Notification Service (8089)
7. ✅ Event Processing Service (8083)
8. ✅ FHIR Service (8085)

**No Auth Pattern (Gateway Trust at Layer) - 2 Services**:
9. ✅ Agent Builder Service (8096) - Pattern 3: No filter auth, gateway-handled tenant isolation
10. ✅ Agent Runtime Service (8088) - Pattern 3: No filter auth, gateway-handled tenant isolation

---

## Phase 3 Candidate Services (7 Services)

### Priority 1 - JWT to Gateway-Trust Migration (3 Services)

#### 1. ECR Service (8101)
- **Current**: JwtAuthenticationFilter
- **Status**: Ready for migration
- **Effort**: Low-Medium
- **Files to Modify**:
  - `backend/modules/services/ecr-service/src/main/java/com/healthdata/ecr/config/EcrSecurityConfig.java`
  - `docker-compose.yml` (ecr-service section)
- **Migration Path**: Standard gateway-trust pattern

#### 2. HCC Service (8105)
- **Current**: JwtAuthenticationFilter
- **Status**: Ready for migration
- **Effort**: Low-Medium
- **Files to Modify**:
  - `backend/modules/services/hcc-service/src/main/java/com/healthdata/hcc/config/HccSecurityConfig.java`
  - `docker-compose.yml` (hcc-service section)
- **Migration Path**: Standard gateway-trust pattern

#### 3. QRDA Export Service (8104)
- **Current**: JwtAuthenticationFilter
- **Status**: Ready for migration
- **Effort**: Low-Medium
- **Files to Modify**:
  - `backend/modules/services/qrda-export-service/src/main/java/com/healthdata/qrda/config/QrdaSecurityConfig.java`
  - `docker-compose.yml` (qrda-export-service section)
- **Migration Path**: Standard gateway-trust pattern (similar to Consent Service)

---

### Priority 2 - Verify/Update No-Auth Pattern Services (4 Services)

These services use the "No Auth Filter" pattern where the gateway handles authentication:

#### 4. EHR Connector Service (800X - TBD)
- **Current**: Custom security configuration
- **Status**: Verify pattern compliance
- **Action**: Verify X-Tenant-ID header validation
- **Files to Check**:
  - `backend/modules/services/ehr-connector-service/src/main/java/com/healthdata/ehr/config/SecurityConfig.java`

#### 5. Migration Workflow Service (800X - TBD)
- **Current**: Custom security configuration
- **Status**: Verify pattern compliance
- **Action**: Verify X-Tenant-ID header validation
- **Files to Check**:
  - `backend/modules/services/migration-workflow-service/src/main/java/com/healthdata/migration/config/MigrationSecurityConfig.java`

#### 6. Prior Auth Service (8102)
- **Current**: Custom security configuration
- **Status**: Verify pattern compliance
- **Action**: Verify X-Tenant-ID header validation
- **Files to Check**:
  - `backend/modules/services/prior-auth-service/src/main/java/com/healthdata/priorauth/config/PriorAuthSecurityConfig.java`

#### 7. Sales Automation Service (8106)
- **Current**: Custom security configuration
- **Status**: Verify pattern compliance
- **Action**: Verify X-Tenant-ID header validation
- **Files to Check**:
  - `backend/modules/services/sales-automation-service/src/main/java/com/healthdata/sales/config/SecurityConfig.java`

#### 8. SDOH Service (800X - TBD)
- **Current**: Custom security configuration
- **Status**: Verify pattern compliance
- **Action**: Verify X-Tenant-ID header validation
- **Files to Check**:
  - `backend/modules/services/sdoh-service/src/main/java/com/healthdata/sdoh/config/SdohSecurityConfig.java`

---

## Phase 3 Implementation Plan

### Step 1: Migrate Priority 1 Services (JWT → Gateway-Trust)
**Timeline**: 2-3 hours
**Services**: ECR, HCC, QRDA Export

```
For each service:
1. Update SecurityConfig.java
   - Replace JwtAuthenticationFilter import
   - Replace TenantAccessFilter import
   - Add @Value for gateway auth properties
   - Create TrustedHeaderAuthFilter bean
   - Create TrustedTenantAccessFilter bean
   - Update SecurityFilterChain

2. Update docker-compose.yml
   - Add GATEWAY_AUTH_DEV_MODE
   - Add GATEWAY_AUTH_SIGNING_SECRET

3. Build and test
   - ./gradlew :modules:services:SERVICE_NAME:build -x test
   - Verify no compilation errors

4. Deploy and verify
   - docker compose up -d SERVICE_NAME
   - Verify service health
   - Test authentication enforcement
```

### Step 2: Verify Priority 2 Services (No-Auth Pattern)
**Timeline**: 1-2 hours
**Services**: EHR Connector, Migration Workflow, Prior Auth, Sales Automation, SDOH

```
For each service:
1. Review SecurityConfig.java
   - Confirm it permits all at filter level
   - Verify X-Tenant-ID header validation at service layer
   - Check if gateway integration is documented

2. Verify deployment
   - Check if service is deployed via docker-compose
   - Verify service health
   - Check logs for authentication errors

3. Update if needed
   - Document gateway-trust dependency
   - Add X-Tenant-ID validation if missing
   - Update CLAUDE.md with service description
```

### Step 3: Comprehensive Testing
**Timeline**: 2-3 hours

```
1. Build all Phase 3 services
   - ./gradlew :modules:services:ecr-service:build \
              :modules:services:hcc-service:build \
              :modules:services:qrda-export-service:build

2. Deploy all Phase 3 services
   - docker compose up -d

3. Verify authentication
   - Health endpoints accessible (200 OK)
   - Protected endpoints require authentication (403 Forbidden)
   - Gateway headers validated
   - Multi-tenant isolation enforced

4. Run integration tests
   - Test cross-service communication
   - Verify Kafka messaging (if applicable)
   - Check database constraints
```

---

## Success Criteria for Phase 3

All services must meet these requirements:

- ✅ Security configuration updated (either gateway-trust or verified no-auth)
- ✅ All services compile without errors
- ✅ All services deploy and become healthy
- ✅ Authentication properly enforced
- ✅ Zero authentication-related errors in logs
- ✅ Multi-tenant isolation working
- ✅ Gateway integration verified
- ✅ Documentation updated (CLAUDE.md, deployment guides)

---

## Phase 3 Completion Criteria

**Phase 3 Complete When**:
- ✅ All 3 Priority 1 services migrated to gateway-trust
- ✅ All 5 Priority 2 services verified for no-auth pattern
- ✅ All 8 services deployed and healthy
- ✅ All tests passing
- ✅ 18/20+ total services migrated/verified
- ✅ Multi-tenant isolation verified across all services
- ✅ Documentation complete

---

## Timeline

| Phase | Services | Status | Target Date |
|-------|----------|--------|-------------|
| Phase 1 | 5 core services | ✅ COMPLETE | Dec 27 |
| Phase 2 | 5 supporting services | ✅ COMPLETE | Dec 30 |
| Phase 3 | 8 remaining services | 📋 READY | Jan 2-3 |
| Phase 4 | Infrastructure/optimization | 🔮 FUTURE | Jan 4+ |

---

## Recommendation

**Option 1: Continue with Phase 3 Immediately**
- Complete Priority 1 services (3 JWT migrations)
- Verify Priority 2 services (5 no-auth services)
- Total effort: 5-8 hours
- Benefit: Complete authentication migration for all backend services

**Option 2: Focus on Testing & Documentation**
- Write comprehensive test suite for Phase 1 & 2
- Update documentation for all migrated services
- Prepare performance testing
- Timeline: 1-2 days

**Option 3: Performance Optimization**
- Profile services for bottlenecks
- Implement caching improvements
- Optimize database queries
- Timeline: 2-3 days

---

## Notes

- **Gateway Service**: Intentionally uses JwtAuthenticationFilter (validates JWT tokens for other services)
- **Agent Services**: Use no-auth pattern by design (accessed via gateway only)
- **All Services**: Must validate X-Tenant-ID header at service layer for multi-tenant isolation

---

*Phase 3 Roadmap - December 30, 2025*
