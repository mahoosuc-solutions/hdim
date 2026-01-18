# Phase 1.9 Implementation Blueprint: Authentication/Authorization for REST API

**Phase Status:** 🚀 Ready for TDD Swarm Implementation

**Parallel Teams:** 4 teams with dedicated git worktrees

**Timeline Estimate:** 2-3 days of parallel development

---

## Phase Vision

Phase 1.9 adds **enterprise-grade authentication and authorization** to the Phase 1.8 REST API Controllers. All 21 REST endpoints will enforce role-based access control via `@PreAuthorize` annotations, validating JWT tokens and tenant isolation at the security layer.

### Key Goals
1. ✅ Implement SecurityFilterChain for JWT token validation
2. ✅ Add `@PreAuthorize` annotations to all REST endpoints
3. ✅ Enforce role-based access control (ADMIN, EVALUATOR, ANALYST, VIEWER)
4. ✅ Create comprehensive security tests (15+ tests per team)
5. ✅ Validate multi-tenant security boundaries
6. ✅ Achieve 100% test passing rate

---

## Team Structure & Responsibilities

### Team 1: Security Configuration & @PreAuthorize Setup
**Worktree:** `phase1.9-team1-security-config`

**Deliverables:**
1. SecurityConfig class with SecurityFilterChain
2. JwtAuthenticationConverter for token parsing
3. @PreAuthorize annotations on PatientController & ObservationController
4. Role hierarchy configuration (SUPER_ADMIN > ADMIN > EVALUATOR > ANALYST > VIEWER)
5. Tests: 16 tests covering security configuration, role hierarchy, endpoint access

**Key Files to Create/Modify:**
```
backend/modules/services/query-api-service/src/main/java/com/healthdata/queryapi/
├── config/
│   └── SecurityConfig.java (NEW)
├── security/
│   └── JwtAuthenticationConverter.java (NEW)
└── api/v1/
    ├── PatientController.java (MODIFY - add @PreAuthorize)
    └── ObservationController.java (MODIFY - add @PreAuthorize)
```

**Implementation Details:**

```java
// SecurityConfig.java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        // Configure JWT authentication
        // Set up authorization rules
        // Configure CORS & CSRF
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // Configure JWT decoding
    }
}

// JwtAuthenticationConverter.java
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    // Extract roles from JWT token
    // Map to Spring Security authorities
}

// PatientController endpoints with @PreAuthorize
@PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'VIEWER')")
@GetMapping("/{patientId}")
public ResponseEntity<PatientResponse> getPatientById(...) { }

@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/mrn/{mrn}")
public ResponseEntity<PatientResponse> getPatientByMrn(...) { }
```

**Test Scenarios (16 tests):**
- ✅ SecurityFilterChain loads successfully
- ✅ JWT token is validated and parsed
- ✅ Authorities are extracted from token
- ✅ Invalid JWT tokens are rejected (401)
- ✅ Missing JWT token returns 401
- ✅ Role hierarchy is enforced
- ✅ ADMIN can access EVALUATOR endpoints
- ✅ VIEWER cannot access ADMIN endpoints
- ✅ PatientController @PreAuthorize rules applied
- ✅ ObservationController @PreAuthorize rules applied
- ✅ Tenant header still required with JWT auth
- ✅ Cross-tenant access prevented
- ✅ Method-level security working
- ✅ Request with valid JWT + tenant header succeeds
- ✅ Request with valid JWT but missing tenant fails
- ✅ Request with invalid JWT fails

---

### Team 2: JWT Token Validation & Extraction
**Worktree:** `phase1.9-team2-jwt-validation`

**Deliverables:**
1. JwtTokenProvider utility class
2. TokenValidationFilter for token extraction
3. Custom JwtAuthenticationEntryPoint for error responses
4. JWT token structure validation (sub, tenant_id, roles, exp)
5. Tests: 15 tests covering token parsing, validation, expiration

**Key Files to Create/Modify:**
```
backend/modules/services/query-api-service/src/main/java/com/healthdata/queryapi/
├── security/
│   ├── JwtTokenProvider.java (NEW)
│   ├── TokenValidationFilter.java (NEW)
│   └── JwtAuthenticationEntryPoint.java (NEW)
└── config/
    └── SecurityConfig.java (MODIFY - integrate filters)
```

**Implementation Details:**

```java
// JwtTokenProvider.java
@Component
public class JwtTokenProvider {

    public Authentication getAuthentication(Jwt jwt) {
        // Extract claims from token
        // Build authorities from roles claim
        // Return Authentication object
    }

    public boolean validateTokenStructure(Jwt jwt) {
        // Validate required claims: sub, tenant_id, roles, exp
        // Check token expiration
        // Verify issuer & audience
    }
}

// TokenValidationFilter.java
public class TokenValidationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws IOException {
        // Extract JWT from Authorization header
        // Validate token structure
        // Set SecurityContext with authentication
    }
}

// JwtAuthenticationEntryPoint.java
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                        HttpServletResponse response,
                        AuthenticationException authException) {
        // Return 401 with error details
        // Include error message in response body
    }
}
```

**JWT Token Structure:**
```json
{
  "sub": "user@example.com",
  "tenant_id": "tenant-001",
  "roles": ["ADMIN", "EVALUATOR"],
  "exp": 1699564800,
  "iat": 1699561200,
  "iss": "hdim-auth-service",
  "aud": "hdim-api"
}
```

**Test Scenarios (15 tests):**
- ✅ Extract token from Authorization header
- ✅ Reject missing Authorization header
- ✅ Reject malformed Authorization header
- ✅ Parse JWT claims successfully
- ✅ Validate token signature
- ✅ Reject expired tokens (401)
- ✅ Reject tokens with missing claims
- ✅ Extract user ID from 'sub' claim
- ✅ Extract tenant ID from 'tenant_id' claim
- ✅ Extract roles from 'roles' claim (array)
- ✅ Validate token not before (nbf) claim
- ✅ Convert JWT roles to Spring authorities
- ✅ Handle malformed JWT format
- ✅ Return proper error response for invalid token
- ✅ Authentication entry point returns 401 + error body

---

### Team 3: Role-Based Access Control for All Endpoints
**Worktree:** `phase1.9-team3-rbac-endpoints`

**Deliverables:**
1. @PreAuthorize on all 21 REST endpoints (ConditionController, CarePlanController)
2. Role-based endpoint access matrix
3. Tenant + role validation decorator
4. Custom @Secured annotations for complex rules
5. Tests: 16 tests covering endpoint access per role

**Key Files to Create/Modify:**
```
backend/modules/services/query-api-service/src/main/java/com/healthdata/queryapi/
├── api/v1/
│   ├── ConditionController.java (MODIFY - add @PreAuthorize)
│   └── CarePlanController.java (MODIFY - add @PreAuthorize)
├── security/
│   └── TenantRoleValidator.java (NEW)
└── annotation/
    └── SecuredEndpoint.java (NEW - optional meta-annotation)
```

**Endpoint Access Matrix:**

| Endpoint | ADMIN | EVALUATOR | ANALYST | VIEWER | DESCRIPTION |
|----------|-------|-----------|---------|--------|-------------|
| GET /patients/{id} | ✅ | ✅ | ✅ | ✅ | Read patient |
| GET /patients/mrn/{mrn} | ✅ | ✅ | ✅ | ✅ | Lookup by MRN |
| GET /observations/... | ✅ | ✅ | ✅ | ✅ | Read observations |
| GET /conditions/... | ✅ | ✅ | ✅ | ✅ | Read conditions |
| GET /care-plans/... | ✅ | ✅ | ✅ | ✅ | Read care plans |

**Implementation Details:**

```java
// ConditionController with @PreAuthorize
@RestController
@RequestMapping("/api/v1/conditions")
public class ConditionController {

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST', 'VIEWER')")
    public ResponseEntity<List<ConditionResponse>> getConditionsByPatient(...) { }

    @GetMapping("/icd/{icdCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST')")  // Analyst can filter by code
    public ResponseEntity<List<ConditionResponse>> getConditionsByIcdCode(...) { }

    @GetMapping("/patient/{patientId}/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")  // Only evaluators see active
    public ResponseEntity<List<ConditionResponse>> getActiveConditions(...) { }
}

// CarePlanController with @PreAuthorize
@RestController
@RequestMapping("/api/v1/care-plans")
public class CarePlanController {

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")  // Care plan is evaluator-focused
    public ResponseEntity<List<CarePlanResponse>> getCarePlansByPatient(...) { }

    @GetMapping("/coordinator/{coordinatorId}")
    @PreAuthorize("hasRole('ADMIN')")  // Only admins can filter by coordinator
    public ResponseEntity<List<CarePlanResponse>> getCarePlansByCoordinator(...) { }
}
```

**Test Scenarios (16 tests):**
- ✅ ADMIN access to all endpoints
- ✅ EVALUATOR access to allowed endpoints
- ✅ ANALYST access to allowed endpoints
- ✅ VIEWER access to allowed endpoints
- ✅ VIEWER denied access to restricted endpoints (403)
- ✅ Unauthorized (no role) returns 403
- ✅ Endpoint requires specific role returns 403 for insufficient role
- ✅ Multiple roles on same endpoint work
- ✅ Role inheritance working (ADMIN > EVALUATOR)
- ✅ Each controller endpoint properly secured
- ✅ PatientController all 6 endpoints secured
- ✅ ObservationController all 5 endpoints secured
- ✅ ConditionController all 4 endpoints secured
- ✅ CarePlanController all 6 endpoints secured
- ✅ Tenant header validation still enforced
- ✅ Cross-role requests properly denied

---

### Team 4: Comprehensive Security Tests & Integration
**Worktree:** `phase1.9-team4-security-tests`

**Deliverables:**
1. SecurityFilterChainTest (10 tests)
2. JwtTokenProviderTest (12 tests)
3. EndpointAuthorizationTest (20 tests covering all endpoints)
4. SecurityIntegrationTest (8 tests)
5. Total: 50 security-focused unit tests

**Key Files to Create/Modify:**
```
backend/modules/services/query-api-service/src/test/java/com/healthdata/queryapi/
├── config/
│   └── SecurityConfigTest.java (NEW)
├── security/
│   ├── JwtTokenProviderTest.java (NEW)
│   ├── TokenValidationFilterTest.java (NEW)
│   └── JwtAuthenticationEntryPointTest.java (NEW)
├── api/v1/
│   └── EndpointAuthorizationSecurityTest.java (NEW - comprehensive)
└── integration/
    └── SecurityIntegrationTest.java (NEW)
```

**Test Class Structure:**

```java
// SecurityConfigTest.java
@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {
    // 10 tests for SecurityFilterChain configuration
    // - Bean creation
    // - Filter ordering
    // - Authorization rules
}

// JwtTokenProviderTest.java
@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {
    // 12 tests for JWT handling
    // - Token parsing
    // - Claim extraction
    // - Role conversion
    // - Expiration checks
}

// EndpointAuthorizationSecurityTest.java
@ExtendWith(MockitoExtension.class)
class EndpointAuthorizationSecurityTest {
    // 20 tests for endpoint security
    // - ADMIN access to all endpoints
    // - EVALUATOR access to allowed endpoints
    // - VIEWER access limited endpoints
    // - Unauthorized returns 403
    // - All 21 endpoints covered
}

// SecurityIntegrationTest.java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    // 8 integration tests combining security layers
    // - Token validation + authorization
    // - Multi-tenant + security
    // - Error handling + security
}
```

**Test Coverage:**

```
SecurityConfigTest:                 10 tests
JwtTokenProviderTest:               12 tests
TokenValidationFilterTest:           8 tests
JwtAuthenticationEntryPointTest:     5 tests
EndpointAuthorizationSecurityTest:  20 tests (all 21 endpoints)
SecurityIntegrationTest:             8 tests
─────────────────────────────────────────────
TOTAL:                              63 security tests
```

**Test Scenarios Include:**
- ✅ Valid JWT token grants access
- ✅ Expired JWT token denied (401)
- ✅ Invalid signature denied (401)
- ✅ Missing JWT returns 401
- ✅ Role-based endpoint access (per endpoint)
- ✅ Insufficient role returns 403
- ✅ Token without required claims rejected
- ✅ Tenant validation with security
- ✅ Error responses formatted correctly
- ✅ Security headers set properly
- ✅ CORS configuration respected
- ✅ Cross-tenant access prevented

---

## Implementation Sequence

### Phase 1: Foundation (Days 1-2)
**Teams 1 & 2 in parallel**
- Team 1: SecurityConfig, JwtAuthenticationConverter, @PreAuthorize on 2 controllers
- Team 2: JwtTokenProvider, TokenValidationFilter, JwtAuthenticationEntryPoint

### Phase 2: Expansion (Days 2-3)
**Teams 3 & 4 in parallel**
- Team 3: Complete @PreAuthorize on remaining 2 controllers
- Team 4: Create comprehensive security test suite

### Phase 3: Validation & Merge (Day 3)
**All teams**
- Run cumulative test suite (should achieve 100% pass rate)
- Code review & quality checks
- Merge to master

---

## Test Results Expected

**Phase 1.9 Tests:** 63 security-focused tests
```
Team 1 Tests (SecurityConfig):       16 tests ✅
Team 2 Tests (JWT Validation):       15 tests ✅
Team 3 Tests (RBAC Endpoints):       16 tests ✅
Team 4 Tests (Integration):          50 tests ✅
─────────────────────────────────────────────
TOTAL Phase 1.9:                     63 tests ✅

Cumulative (Phase 1.3-1.9):          391 tests ✅
```

---

## Integration Checklist

- [ ] SecurityFilterChain properly integrated with Spring Security
- [ ] JwtAuthenticationConverter working with JwtDecoder
- [ ] All 21 endpoints have @PreAuthorize annotations
- [ ] Role hierarchy configured (SUPER_ADMIN > ADMIN > EVALUATOR > ANALYST > VIEWER)
- [ ] JWT token validation happening before authorization
- [ ] Tenant isolation still enforced at REST controller level
- [ ] Error responses (401, 403) properly formatted
- [ ] All 63 tests passing
- [ ] No regression in Phase 1.3-1.8 tests (301 tests still passing)
- [ ] Code reviewed by all 4 teams

---

## Next Phase Preview

**Phase 1.10: GraphQL API Layer** (Option 3 in roadmap)
- Standalone GraphQL service alongside REST API
- Query and mutation definitions
- GraphQL controller implementation
- Authorization at GraphQL resolver level
- ~4-5 weeks of development

---

## Kickoff Instructions

**For Team Leads:**

1. **Navigate to your worktree:**
   ```bash
   cd /home/webemo-aaron/projects/hdim-master/phase1.9-team{N}-{role}
   ```

2. **Create feature branch (optional):**
   ```bash
   git checkout -b team{N}-{task} phase1.9-team{N}-{role}
   ```

3. **Run baseline tests** (should have 301 passing):
   ```bash
   ./gradlew :modules:shared:infrastructure:event-sourcing:test
   ./gradlew :modules:services:query-api-service:test
   ```

4. **Begin implementation** following the team-specific deliverables above

5. **Commit frequently** with clear messages (TDD approach)

6. **Notify when complete** for merge coordination

---

**Status:** 🚀 Ready for TDD Swarm Implementation
**Estimated Completion:** 2-3 days with 4 parallel teams
**Success Criteria:** 63 security tests + 301 cumulative tests all passing, zero regressions
