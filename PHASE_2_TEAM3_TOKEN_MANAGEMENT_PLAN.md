# Phase 2.0 Team 3: Token Management Architecture & Implementation Plan

**Team Lead:** Claude Code
**Phase:** 2.0 - Critical Security Features
**Status:** Planning → TDD Swarm Implementation
**Timeline:** Parallel sub-teams with worktrees

---

## Executive Summary

Phase 2.0 Team 3 implements secure token lifecycle management for the gateway service, including token refresh, revocation, and validation. This extends Phase 1.9's JWT authentication with stateful token management capabilities.

**Key Objectives:**
1. ✅ Token refresh capability (`POST /api/v1/auth/refresh`) with sliding window extension
2. ✅ Token revocation/blacklist (Redis-backed) for logout functionality
3. ✅ Token validation filter to check revocation status on protected endpoints
4. ✅ Comprehensive test coverage (80+ tests across 3 sub-teams)

---

## Architecture Overview

### Token Lifecycle

```
User Login (Phase 1.9)
    ↓
    Create tokens (access + refresh)
    ↓
    Store refresh token in Redis (expiry + metadata)
    ↓
Client uses access token
    ↓
    [Token expires in 15 minutes]
    ↓
Client sends refresh token → POST /api/v1/auth/refresh
    ↓
Validate refresh token (not revoked, not expired)
    ↓
Issue new access + refresh tokens
    ↓
Store new refresh token, revoke old one
    ↓
Client continues with new access token
    ↓
    [Sliding window: 30 minutes of inactivity = logout]
    ↓
User logs out → POST /api/v1/auth/logout
    ↓
Revoke all refresh tokens for user
    ↓
Revoke access token (add to blacklist)
    ↓
Client must re-authenticate
```

### Component Architecture

```
┌─────────────────────────────────────────────────────┐
│              Gateway Service (Port 8001)            │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │   Authentication Filters (existing)         │   │
│  │  - GatewayAuthenticationFilter              │   │
│  │  - AuditLoggingFilter                       │   │
│  │  - RateLimitingFilter                       │   │
│  └─────────────────────────────────────────────┘   │
│                      ↓                              │
│  ┌─────────────────────────────────────────────┐   │
│  │   TokenValidationFilter (NEW - Team 3.3)   │   │
│  │  - Check token not in blacklist             │   │
│  │  - Return 401 if revoked                    │   │
│  └─────────────────────────────────────────────┘   │
│                      ↓                              │
│  ┌─────────────────────────────────────────────┐   │
│  │   Endpoints (existing + new)                │   │
│  │  - POST /auth/login (existing)              │   │
│  │  - POST /auth/refresh (NEW - Team 3.1)     │   │
│  │  - POST /auth/logout (NEW - Team 3.2)      │   │
│  │  - POST /auth/revoke (NEW - Team 3.2)      │   │
│  └─────────────────────────────────────────────┘   │
│                      ↓                              │
│  ┌─────────────────────────────────────────────┐   │
│  │   Services (NEW)                            │   │
│  │  - TokenRefreshService (Team 3.1)           │   │
│  │  - TokenRevocationService (Team 3.2)        │   │
│  │  - TokenValidationService (Team 3.3)        │   │
│  └─────────────────────────────────────────────┘   │
│                      ↓                              │
│  ┌─────────────────────────────────────────────┐   │
│  │   Storage Backends                          │   │
│  │  - Redis: Refresh tokens + blacklist        │   │
│  │  - PostgreSQL: Token audit logs (optional)  │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## Sub-Team Structure (TDD Swarm)

### Team 3.1: Token Refresh Service
**Responsibility:** `/api/v1/auth/refresh` endpoint and refresh token management

**Deliverables:**
- `TokenRefreshRequest` DTO
- `TokenRefreshResponse` DTO
- `RefreshTokenEntity` JPA entity
- `RefreshTokenRepository` Spring Data repository
- `TokenRefreshService` with validation and extension logic
- `TokenRefreshController` REST endpoint
- Integration tests (20+ tests)
- Unit tests (15+ tests)

**Key Features:**
- ✅ Validate refresh token signature and expiry
- ✅ Check token not in revocation blacklist
- ✅ Issue new access + refresh tokens
- ✅ Sliding window extension (30 minutes inactivity)
- ✅ Revoke old refresh token (prevent reuse)
- ✅ Audit trail for token issuance
- ✅ Per-tenant token isolation

**API Contract:**
```
POST /api/v1/auth/refresh
Request:
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}

Response (200 OK):
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 900,
  "tokenType": "Bearer"
}

Error (401 Unauthorized):
{
  "error": "Invalid or expired refresh token",
  "code": "INVALID_REFRESH_TOKEN"
}

Error (403 Forbidden):
{
  "error": "Refresh token has been revoked",
  "code": "REVOKED_REFRESH_TOKEN"
}
```

**Database Schema (Liquibase):**
```sql
CREATE TABLE refresh_tokens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id VARCHAR(100) NOT NULL,
  user_id VARCHAR(255) NOT NULL,
  token_jti VARCHAR(255) NOT NULL UNIQUE,  -- JWT ID claim
  token_hash VARCHAR(255) NOT NULL,         -- SHA-256 hash for lookup
  expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
  last_used_at TIMESTAMP WITH TIME ZONE,
  revoked_at TIMESTAMP WITH TIME ZONE,
  revocation_reason VARCHAR(255),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_tenant_id ON refresh_tokens(tenant_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_jti ON refresh_tokens(token_jti);
```

---

### Team 3.2: Token Revocation Service
**Responsibility:** `/api/v1/auth/logout` and `/api/v1/auth/revoke` endpoints

**Deliverables:**
- `TokenRevocationRequest` DTO
- `TokenRevocationResponse` DTO
- `TokenRevocationService` with blacklist management
- `TokenRevocationController` REST endpoints
- Redis blacklist configuration
- Integration tests (20+ tests)
- Unit tests (15+ tests)

**Key Features:**
- ✅ Logout: Revoke all user tokens (both access and refresh)
- ✅ Selective revocation: Revoke specific device/session
- ✅ Automatic blacklist cleanup (TTL matches token expiry)
- ✅ Support for both access and refresh token revocation
- ✅ Audit log all revocations (user, reason, timestamp)
- ✅ Prevent token reuse after revocation
- ✅ Multi-tenant isolation

**Redis Blacklist Structure:**
```
Key Format: "token_blacklist:{jti}"
Value: {
  "userId": "user-123",
  "tenantId": "tenant-001",
  "revokedAt": "2025-01-17T15:30:00Z",
  "reason": "LOGOUT" | "COMPROMISE" | "ADMIN_REVOKE",
  "deviceId": "device-456" (optional)
}
TTL: Match token expiry time
```

**API Contracts:**

```
POST /api/v1/auth/logout
Headers:
  Authorization: Bearer {accessToken}
  X-Tenant-ID: tenant-001

Response (200 OK):
{
  "message": "Successfully logged out",
  "tokensRevoked": 3
}

---

POST /api/v1/auth/revoke
Headers:
  Authorization: Bearer {accessToken}
  X-Tenant-ID: tenant-001

Request:
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "reason": "COMPROMISE" | "LOGOUT" | "SESSION_TIMEOUT"
}

Response (200 OK):
{
  "message": "Token revoked successfully",
  "revokedAt": "2025-01-17T15:30:00Z"
}
```

**Revocation Reasons:**
- `LOGOUT` - User initiated logout
- `COMPROMISE` - Token suspected compromised
- `ADMIN_REVOKE` - Admin forced revocation
- `INACTIVITY` - Automatic revocation due to timeout
- `PASSWORD_CHANGE` - User changed password

---

### Team 3.3: Token Validation Filter & Service
**Responsibility:** Check token revocation status on protected endpoints

**Deliverables:**
- `TokenValidationService` with revocation checking
- `TokenValidationFilter` servlet filter
- `TokenRevocationCache` for performance (optional)
- Integration tests (20+ tests)
- Unit tests (15+ tests)

**Key Features:**
- ✅ Extract JTI claim from access token
- ✅ Check if JTI exists in blacklist (Redis)
- ✅ Return 401 if token is revoked
- ✅ Cache blacklist for performance (optional)
- ✅ Fail-open: Allow request if Redis unavailable
- ✅ Audit validation failures
- ✅ Support for token lifecycle tracking

**Filter Integration:**

```
Request Flow:
  1. AuditLoggingFilter
  2. RateLimitingFilter
  3. GatewayAuthenticationFilter (JWT validation)
  4. TokenValidationFilter (NEW - check blacklist)  ← Team 3.3
  5. Spring Security (authorization)
  6. Endpoint Handler
```

---

## TDD Implementation Strategy

### Phase 1: Test-First Development (Per Sub-Team)

**Team 3.1 (Refresh):**
1. Write integration tests for `/api/v1/auth/refresh` endpoint
2. Write unit tests for TokenRefreshService
3. Write unit tests for RefreshTokenRepository queries
4. Implement RefreshTokenEntity and Repository
5. Implement TokenRefreshService to pass tests
6. Implement TokenRefreshController to pass tests

**Team 3.2 (Revocation):**
1. Write integration tests for `/api/v1/auth/logout` endpoint
2. Write integration tests for `/api/v1/auth/revoke` endpoint
3. Write unit tests for TokenRevocationService
4. Write unit tests for Redis blacklist operations
5. Implement TokenRevocationService to pass tests
6. Implement TokenRevocationController to pass tests

**Team 3.3 (Validation):**
1. Write unit tests for TokenValidationFilter
2. Write unit tests for TokenValidationService
3. Write integration tests for token blacklist checking
4. Implement TokenValidationService to pass tests
5. Implement TokenValidationFilter to pass tests
6. Integrate into SecurityFilterChain

### Phase 2: Parallel Worktree Development

**Worktree Structure:**
```
master/                              (main branch)
  ├── feature/team3-refresh/         (Team 3.1 - Token Refresh)
  ├── feature/team3-revocation/      (Team 3.2 - Token Revocation)
  └── feature/team3-validation/      (Team 3.3 - Token Validation)
```

**Workflow:**
1. Each team creates worktree from master
2. Team writes tests first (failing)
3. Team implements features (tests pass)
4. Team rebases on latest master
5. Team verifies all tests pass
6. Team rebases into master branch (or creates PR)

---

## Test Plan (80+ Tests Total)

### Team 3.1: Token Refresh Tests (35 tests)

**Integration Tests (20):**
- ✅ Successful token refresh with valid refresh token
- ✅ Refresh extends session (sliding window)
- ✅ Old refresh token is revoked after use
- ✅ Return 401 for expired refresh token
- ✅ Return 403 for revoked refresh token
- ✅ Return 401 for invalid/malformed token
- ✅ Multi-tenant isolation (cannot use token from other tenant)
- ✅ Response includes new access and refresh tokens
- ✅ Response includes correct expiryIn (900 seconds)
- ✅ Response includes tokenType="Bearer"
- ✅ Refresh token contains correct claims (jti, sub, iat, exp)
- ✅ JTI is unique per refresh token
- ✅ Audit log created for refresh operation
- ✅ Rate limiting applies to refresh endpoint (100/min)
- ✅ Health check endpoint bypasses token validation
- ✅ Multiple simultaneous refresh requests handled
- ✅ Token refresh updates last_used_at timestamp
- ✅ Sliding window calculation (now + 30 min inactivity)
- ✅ Refresh token stored in Redis with expiry
- ✅ New tokens issued with updated timestamps

**Unit Tests (15):**
- ✅ TokenRefreshService validates token signature
- ✅ TokenRefreshService checks token not expired
- ✅ TokenRefreshService checks token not revoked
- ✅ TokenRefreshService extracts user ID from token
- ✅ TokenRefreshService extracts tenant ID from token
- ✅ TokenRefreshService generates new JTI
- ✅ TokenRefreshService verifies token in database
- ✅ TokenRefreshService revokes old token
- ✅ TokenRefreshService handles invalid JWT
- ✅ TokenRefreshService handles missing JTI claim
- ✅ RefreshTokenRepository finds by token hash
- ✅ RefreshTokenRepository finds non-revoked tokens
- ✅ RefreshTokenRepository updates revocation status
- ✅ RefreshTokenRepository queries by user + tenant
- ✅ RefreshTokenRepository cleans expired tokens

### Team 3.2: Token Revocation Tests (30 tests)

**Integration Tests (18):**
- ✅ Successful logout revokes all user tokens
- ✅ Logout returns 200 OK with revocation count
- ✅ Logout requires authentication (401 without token)
- ✅ Logout revokes both access and refresh tokens
- ✅ Revoke specific token via POST /auth/revoke
- ✅ Revoke requires valid reason parameter
- ✅ Multi-tenant isolation (cannot revoke other tenant's tokens)
- ✅ Revoked token cannot be used for refresh
- ✅ Revocation blacklist persists in Redis
- ✅ Blacklist entry has correct TTL (matches token expiry)
- ✅ Blacklist entry contains user ID and tenant ID
- ✅ Admin can force revoke user's tokens (with role)
- ✅ Revocation reason is stored and auditable
- ✅ Multiple logout attempts are idempotent
- ✅ Revoke endpoint requires Authorization header
- ✅ Revocation is immediate (no delay)
- ✅ Audit log records revocation reason
- ✅ Revocation count in response matches actual tokens

**Unit Tests (12):**
- ✅ TokenRevocationService revokes by JTI
- ✅ TokenRevocationService revokes all user tokens
- ✅ TokenRevocationService adds to Redis blacklist
- ✅ TokenRevocationService sets correct TTL
- ✅ TokenRevocationService stores revocation reason
- ✅ TokenRevocationService extracts JTI from token
- ✅ TokenRevocationService handles missing JTI
- ✅ TokenRevocationService validates revocation reason
- ✅ TokenRevocationService handles expired tokens
- ✅ TokenRevocationService logs revocation events
- ✅ Redis blacklist key format is correct
- ✅ Blacklist cleanup scheduled task runs

### Team 3.3: Token Validation Tests (25 tests)

**Integration Tests (15):**
- ✅ Protected endpoint rejects revoked access token
- ✅ Protected endpoint accepts non-revoked token
- ✅ Return 401 when accessing with revoked token
- ✅ ValidationFilter extracts JTI from token
- ✅ ValidationFilter checks Redis blacklist
- ✅ ValidationFilter continues if token not revoked
- ✅ ValidationFilter is called for all protected endpoints
- ✅ ValidationFilter skips excluded endpoints (/actuator/*)
- ✅ ValidationFilter fails open if Redis unavailable
- ✅ Multiple validation checks don't break request
- ✅ Audit log created for blacklist checks
- ✅ Validation filters before Spring Security
- ✅ Multi-tenant isolation in validation
- ✅ Token validation is fast (cache support)
- ✅ Concurrent token validation requests handled

**Unit Tests (10):**
- ✅ TokenValidationService checks blacklist
- ✅ TokenValidationService returns true for non-revoked
- ✅ TokenValidationService returns false for revoked
- ✅ TokenValidationService handles missing JTI
- ✅ TokenValidationService handles Redis error
- ✅ TokenValidationFilter extracts token from header
- ✅ TokenValidationFilter calls validation service
- ✅ TokenValidationFilter continues filter chain
- ✅ TokenValidationFilter returns 401 on blacklist hit
- ✅ TokenValidationCache invalidation works correctly

---

## Implementation Guidelines

### Code Style & Patterns

**Entity Pattern:**
```java
@Entity
@Table(name = "refresh_tokens")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, unique = true)
    private String tokenJti;  // JWT ID claim

    @Column(nullable = false)
    private String tokenHash; // SHA-256 for lookup

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant lastUsedAt;
    private Instant revokedAt;
    private String revocationReason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }
}
```

**Service Pattern:**
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TokenRefreshService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenRevocationService revocationService;
    private final AuditLogService auditLogService;

    @Transactional
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        // 1. Validate token signature
        // 2. Check not expired
        // 3. Check not revoked
        // 4. Verify in database
        // 5. Revoke old token
        // 6. Issue new tokens
        // 7. Audit log
        // 8. Return response
    }

    private RefreshToken validateRefreshToken(String token) {
        // Implementation
    }
}
```

**Controller Pattern:**
```java
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class TokenRefreshController {

    private final TokenRefreshService tokenRefreshService;

    @PostMapping("/refresh")
    @RateLimit(limit = 100, window = 60)  // 100 requests per minute
    public ResponseEntity<TokenRefreshResponse> refresh(
            @RequestBody TokenRefreshRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        TokenRefreshResponse response = tokenRefreshService.refreshToken(request);
        return ResponseEntity.ok(response);
    }
}
```

### Redis Key Naming Convention

```
# Refresh tokens (expiry = token expiry time)
refresh_token:{token_hash}
refresh_token:user:{userId}:{tenantId}:*

# Token blacklist (expiry = token expiry time)
token_blacklist:{jti}

# Blacklist cache (optional, expiry = 5 minutes)
blacklist_cache:{jti}

# Token revocation metadata
token_revocation:{jti}
```

### Error Codes

```java
enum TokenErrorCode {
    // Refresh errors
    INVALID_REFRESH_TOKEN(401),
    EXPIRED_REFRESH_TOKEN(401),
    REVOKED_REFRESH_TOKEN(403),
    TOKEN_GENERATION_FAILED(500),

    // Revocation errors
    TOKEN_ALREADY_REVOKED(409),
    INVALID_REVOCATION_REASON(400),
    REVOCATION_FAILED(500),

    // Validation errors
    INVALID_TOKEN_FORMAT(401),
    MISSING_JTI_CLAIM(401),
    TOKEN_VALIDATION_FAILED(500)
}
```

---

## Success Criteria

### Functional Requirements
- ✅ Token refresh endpoint operational with 100+/min rate limit
- ✅ Token revocation (logout) revokes all user sessions
- ✅ Token validation filter blocks revoked tokens
- ✅ Multi-tenant isolation enforced across all operations
- ✅ All tokens automatically expire after defined period
- ✅ Sliding window extension working (30 min inactivity)

### Testing Requirements
- ✅ 80+ tests covering all scenarios
- ✅ Integration tests with Spring context
- ✅ Unit tests with mocks
- ✅ 100% branch coverage for business logic
- ✅ All tests pass in CI/CD pipeline

### Security Requirements
- ✅ No plaintext tokens in Redis (hashed)
- ✅ JTI uniqueness enforced
- ✅ Revocation is immediate (no delays)
- ✅ Multi-tenant isolation verified
- ✅ Audit trail for all operations
- ✅ Rate limiting prevents token refresh abuse

### Performance Requirements
- ✅ Token refresh completes < 100ms
- ✅ Token validation < 50ms (with cache < 10ms)
- ✅ Logout handles 100+ requests/sec
- ✅ Redis queries optimized with indexes

---

## Liquibase Migration Strategy

**Team 3.1 Migration:**
```
0100-create-refresh-tokens-table.xml
```

**Team 3.2 Migration:**
```
0101-add-revocation-reason-column.xml (if needed)
```

**Team 3.3 Migration:**
```
None (uses existing refresh_tokens table)
```

---

## Worktree Commands

```bash
# Create worktrees from master
git worktree add ../team3-refresh feature/team3-refresh
git worktree add ../team3-revocation feature/team3-revocation
git worktree add ../team3-validation feature/team3-validation

# Each team works independently
cd ../team3-refresh
./gradlew test  # Run tests

# When ready, rebase and merge
git rebase master
git checkout master
git merge feature/team3-refresh

# Clean up worktree
git worktree remove ../team3-refresh
git branch -d feature/team3-refresh
```

---

## Timeline & Milestones

| Milestone | Task | ETA |
|-----------|------|-----|
| **M1** | Architecture planning | ✅ Complete |
| **M2** | Team 3.1 tests written | Next |
| **M3** | Team 3.1 implementation | Next |
| **M4** | Team 3.2 tests written | Parallel |
| **M5** | Team 3.2 implementation | Parallel |
| **M6** | Team 3.3 tests written | Parallel |
| **M7** | Team 3.3 implementation | Parallel |
| **M8** | Integration testing | Final |
| **M9** | All tests passing | Final |
| **M10** | Merge to master | Final |

---

## References

- **Phase 1.9:** Authentication & Authorization (140/140 tests passing)
- **Phase 2.0 Team 1:** API Rate Limiting (80+ tests)
- **Phase 2.0 Team 2:** Audit Logging (70+ tests)
- **CLAUDE.md:** Gateway Trust Architecture section
- **AUTHENTICATION_GUIDE.md:** Token structure and claims

---

_Architecture Plan: January 17, 2026_

---

## IMPLEMENTATION STATUS

### Phase 2.0 Team 3 - TDD Swarm Progress

**Overall Status:** 🟡 IN PROGRESS
**Teams Active:** 3 parallel worktrees
**Tests Written:** 35+ (Team 3.1 complete)

#### Team 3.1: Token Refresh ✅ COMMITTED
- **Status:** Ready for merge after CI/CD validation
- **Worktree:** `/home/webemo-aaron/projects/team3-refresh`
- **Branch:** `feature/team3-refresh`
- **Tests:** 35 written and committed
- **Files:** 14 committed (DTOs, entities, services, tests)
- **Commit:** `20d3774a` - "Phase 2.0 Team 3.1: Token Refresh - TDD First"
- **Next:** CI/CD pipeline validation, then merge to master

#### Team 3.2: Token Revocation 🟡 PREPARED
- **Status:** Worktree ready, tests phase starting
- **Worktree:** `/home/webemo-aaron/projects/team3-revocation`
- **Branch:** `feature/team3-revocation`
- **Next:** Write 30+ tests for revocation service

#### Team 3.3: Token Validation 🟡 PREPARED
- **Status:** Worktree ready, tests phase starting
- **Worktree:** `/home/webemo-aaron/projects/team3-validation`
- **Branch:** `feature/team3-validation`
- **Next:** Write 25+ tests for validation filter

### Worktree Commands

```bash
# View all worktrees
git -C /home/webemo-aaron/projects/hdim-master worktree list

# Switch to specific team
cd /home/webemo-aaron/projects/team3-refresh     # Team 3.1
cd /home/webemo-aaron/projects/team3-revocation  # Team 3.2
cd /home/webemo-aaron/projects/team3-validation  # Team 3.3

# Merge Team 3.1 back to master (when ready)
cd /home/webemo-aaron/projects/hdim-master
git merge feature/team3-refresh
```

_Last Updated: January 17, 2026 - TDD Swarm Implementation Started_
