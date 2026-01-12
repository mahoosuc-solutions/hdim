# Feature Specification: Gateway Authentication Centralization

## Overview

Centralize all authentication logic at the API Gateway, eliminating distributed security configurations across 21 backend services. Services will trust the gateway's validation and receive authenticated user context via trusted headers, removing ~90% duplicate security code while hardening production security.

## User Story

**As a** platform operator
**I want** all authentication handled at the gateway
**So that** I have a single point of security enforcement, reduced code duplication, and easier security audits

## Priority: Critical

## Requirements

### Functional Requirements

1. **Gateway Authentication Filter**
   - Validate all incoming JWT tokens at gateway before routing
   - Extract user context (userId, tenantIds, roles) from validated tokens
   - Inject trusted headers (`X-User-Id`, `X-Tenant-Ids`, `X-User-Roles`, `X-Auth-Validated`)
   - Reject invalid/expired tokens with consistent 401 responses

2. **Service Trust Model**
   - Backend services trust gateway-injected headers without re-validation
   - Services extract user context from trusted headers only
   - Internal service-to-service calls bypass gateway auth (use internal network trust)

3. **Public Path Registry**
   - Centralized configuration of public (unauthenticated) paths
   - Per-service public path declarations in gateway config
   - Health checks, actuator endpoints excluded by default

4. **Security Hardening**
   - Remove all "demo mode" / "permitAll" configurations
   - Enforce authentication in all environments (dev, staging, prod)
   - Single security profile (eliminate test vs. prod divergence)

5. **Backward Compatibility**
   - Services continue to work during migration (dual-mode support)
   - Gradual rollout via feature flag
   - No breaking changes to existing API contracts

### Non-Functional Requirements

| Requirement | Target |
|-------------|--------|
| **Latency** | < 5ms overhead for token validation at gateway |
| **Availability** | No single point of failure (gateway clustering) |
| **Security** | HIPAA compliant, SOC 2 aligned |
| **Maintainability** | Single security config location |
| **Testability** | Integration tests for all auth scenarios |

## Technical Design

### Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        API Gateway                               │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  GatewayAuthenticationFilter                                 ││
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  ││
│  │  │ JWT Validator │→│ Claims Extract│→│ Header Injector   │  ││
│  │  └──────────────┘  └──────────────┘  └──────────────────┘  ││
│  └─────────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  Public Path Registry                                        ││
│  │  - /api/auth/login, /api/auth/register (always public)      ││
│  │  - /actuator/health (health checks)                          ││
│  │  - Per-service public paths from config                      ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
                              │
           Trusted Headers: X-User-Id, X-Tenant-Ids, X-User-Roles
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Backend Services                             │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  TrustedHeaderAuthFilter (new shared component)              ││
│  │  - Extract user context from X-* headers                     ││
│  │  - Populate SecurityContext                                  ││
│  │  - Reject requests without X-Auth-Validated header           ││
│  └─────────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  TenantAccessFilter (existing, unchanged)                    ││
│  │  - Enforce tenant isolation using extracted tenant IDs       ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

### Components

#### New Components

| Component | Location | Responsibility |
|-----------|----------|----------------|
| `GatewayAuthenticationWebFilter` | gateway-service | Validates JWT, injects trusted headers |
| `PublicPathRegistry` | gateway-service | Manages public (unauthenticated) paths |
| `TrustedHeaderAuthFilter` | shared/authentication | Extracts user context from gateway headers |
| `AuthHeaderConstants` | shared/authentication | Header name constants (`X-User-Id`, etc.) |
| `GatewayAuthProperties` | gateway-service | Configuration properties class |

#### Modified Components

| Component | Change |
|-----------|--------|
| `GatewaySecurityConfig` | Remove demo mode, add central auth filter |
| `JwtAuthenticationFilter` | Deprecate in services, use at gateway only |
| Service SecurityConfigs (8+) | Replace with minimal TrustedHeaderAuthFilter |

#### Removed Components

| Component | Reason |
|-----------|--------|
| `JwtTokenService` (cql-engine duplicate) | Use shared module only |
| Service-specific CORS configs | Centralize at gateway |
| Demo/test security profiles | Single production profile |

### Database Changes

None required. Authentication uses existing `users`, `user_roles`, `tenants` tables.

### API Changes

| Type | Endpoint | Change |
|------|----------|--------|
| **Headers** | All requests | Add `X-User-Id`, `X-Tenant-Ids`, `X-User-Roles`, `X-Auth-Validated` |
| **Response** | 401 errors | Standardize error format across all services |

### Configuration Changes

**Gateway application.yml:**
```yaml
gateway:
  auth:
    enabled: true
    enforced: true  # Changed from false
    trusted-header-prefix: "X-"
    public-paths:
      global:
        - /api/auth/**
        - /actuator/health
        - /actuator/info
      fhir-service:
        - /fhir/metadata
      cql-engine-service:
        - /cql-engine/health
```

## Implementation Plan

### Phase 1: Foundation (Preparation)

- [ ] Create feature branch `feature/gateway-auth-centralization`
- [ ] Add `AuthHeaderConstants` to shared authentication module
- [ ] Create `TrustedHeaderAuthFilter` in shared authentication module
- [ ] Create `PublicPathRegistry` interface and YAML-based implementation
- [ ] Add `GatewayAuthProperties` configuration class
- [ ] Write unit tests for new shared components

### Phase 2: Gateway Implementation

- [ ] Create `GatewayAuthenticationWebFilter` (Spring Cloud Gateway WebFilter)
- [ ] Implement JWT validation at gateway (reuse `JwtTokenService`)
- [ ] Implement header injection logic
- [ ] Update `GatewaySecurityConfig` to use new filter
- [ ] Configure public paths via `application.yml`
- [ ] Remove demo mode flags (`gateway.auth.enforced=true`)
- [ ] Write gateway integration tests

### Phase 3: Service Migration (Iterative)

**For each of 21 services:**
- [ ] Add `TrustedHeaderAuthFilter` to service security config
- [ ] Remove `JwtAuthenticationFilter` from service
- [ ] Remove duplicate CORS configuration
- [ ] Remove service-specific `SecurityConfig` boilerplate
- [ ] Update service tests to use trusted header mocking
- [ ] Verify tenant isolation still works

**Priority order:**
1. fhir-service (most traffic)
2. patient-service (core service)
3. quality-measure-service (complex, has WebSocket)
4. cql-engine-service (has duplicate JwtTokenService)
5. Remaining 17 services

### Phase 4: Cleanup & Hardening

- [ ] Delete duplicate `JwtTokenService` from cql-engine-service
- [ ] Remove all test/demo security profiles from services
- [ ] Centralize WebSocket authentication interceptors to shared module
- [ ] Add gateway rate limiting for auth endpoints
- [ ] Implement structured audit logging for auth events
- [ ] Security review and penetration testing

### Phase 5: Documentation & Rollout

- [ ] Update API documentation with auth header requirements
- [ ] Create runbook for auth troubleshooting
- [ ] Update Kubernetes configs for production
- [ ] Deploy to staging, run integration tests
- [ ] Gradual production rollout (canary → full)
- [ ] Monitor auth metrics and error rates

## Testing Strategy

### Unit Tests

- `GatewayAuthenticationWebFilter` - Token validation, header injection
- `TrustedHeaderAuthFilter` - Header extraction, context population
- `PublicPathRegistry` - Path matching logic
- `AuthHeaderConstants` - Header name validation

### Integration Tests

| Scenario | Expected Result |
|----------|-----------------|
| Valid JWT → protected endpoint | 200, user context available |
| Expired JWT → protected endpoint | 401, consistent error format |
| No token → protected endpoint | 401, consistent error format |
| No token → public endpoint | 200, request proceeds |
| Valid JWT → tenant-isolated data | Only user's tenant data returned |
| Forged X-Auth-Validated header | Rejected (gateway strips external headers) |

### E2E Tests

- Login → Access protected resource → Logout flow
- Multi-tenant user switching tenants
- Token refresh flow
- WebSocket authentication handshake

### Security Tests

- Header injection attack (external X-User-Id)
- JWT algorithm confusion attack
- Token replay after logout
- Tenant isolation bypass attempts

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Security config LoC reduction | > 80% | Lines of security code in services |
| Auth latency overhead | < 5ms | P99 gateway auth filter time |
| Failed auth attempts | < 0.1% | (401s / total requests) |
| Security findings | 0 critical | Penetration test results |
| Service migration | 21/21 | Services using trusted headers |

## Risks & Mitigation

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Gateway becomes SPOF | High | Medium | Deploy gateway in HA cluster (3+ replicas) |
| Header injection attacks | High | Low | Gateway strips all X-Auth-* headers from incoming requests |
| Migration breaks service | Medium | Medium | Feature flag for dual-mode (accept both JWT and trusted headers) |
| Performance degradation | Medium | Low | Benchmark before/after, cache token validation |
| Token validation library bug | High | Low | Use well-maintained library (jjwt), keep updated |
| Incomplete service migration | Medium | Medium | Track migration in todo list, automated test coverage |

## Rollback Plan

1. **Feature flag**: Set `gateway.auth.enforced=false` to return to passthrough mode
2. **Service rollback**: Each service can re-enable local JWT validation independently
3. **Config rollback**: Keep previous `application.yml` versions in git for quick revert
4. **Database**: No database changes, no rollback needed

## Dependencies

| Dependency | Status | Notes |
|------------|--------|-------|
| Shared authentication module | ✅ Exists | Add new components here |
| Spring Cloud Gateway | ✅ In use | WebFilter API available |
| Redis (token blacklist) | ⚠️ Optional | For logout/token revocation |
| Kubernetes secrets | ✅ Configured | JWT secret already in k8s |

## Timeline Estimate

| Phase | Effort |
|-------|--------|
| Phase 1: Foundation | 2-3 days |
| Phase 2: Gateway Implementation | 3-4 days |
| Phase 3: Service Migration | 5-7 days (21 services) |
| Phase 4: Cleanup & Hardening | 2-3 days |
| Phase 5: Documentation & Rollout | 2-3 days |
| **Total** | **14-20 days** |

## Approval Checklist

- [ ] Tech Lead approval
- [ ] Security review sign-off
- [ ] DevOps review (Kubernetes changes)
- [ ] QA test plan approval
- [ ] Ready for implementation
- [ ] License compliance verified (docs/compliance/THIRD_PARTY_NOTICES.md)

---

**Created**: 2025-12-22
**Owner**: Platform Team
**Status**: Planning
**Feature Branch**: `feature/gateway-auth-centralization`

---

## Appendix: Files to Modify

### Gateway Service
- `backend/modules/services/gateway-service/src/main/java/com/healthdata/gateway/config/GatewaySecurityConfig.java`
- `backend/modules/services/gateway-service/src/main/java/com/healthdata/gateway/filter/GatewayAuthenticationWebFilter.java` (new)
- `backend/modules/services/gateway-service/src/main/java/com/healthdata/gateway/config/GatewayAuthProperties.java` (new)
- `backend/modules/services/gateway-service/src/main/resources/application.yml`
- `backend/modules/services/gateway-service/src/main/resources/application-kubernetes.yml`

### Shared Authentication Module
- `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/filter/TrustedHeaderAuthFilter.java` (new)
- `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/constants/AuthHeaderConstants.java` (new)

### Services to Migrate (21 total)
- `fhir-service/src/main/java/.../config/FhirSecurityConfig.java`
- `patient-service/src/main/java/.../config/PatientSecurityConfig.java`
- `quality-measure-service/src/main/java/.../config/QualityMeasureSecurityConfig.java`
- `cql-engine-service/src/main/java/.../config/CqlSecurityConfig.java`
- `cql-engine-service/src/main/java/.../security/JwtTokenService.java` (DELETE)
- ... (17 more services)

### Test Files
- Add integration tests for gateway auth filter
- Update service tests to mock trusted headers
- Add security penetration test scenarios
