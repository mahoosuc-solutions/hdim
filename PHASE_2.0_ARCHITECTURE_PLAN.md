# Phase 2.0: Critical Security Features Architecture Plan

**Phase:** 2.0
**Focus:** API Rate Limiting, Audit Logging, Token Management
**Status:** Planning
**Estimated Duration:** 3-4 weeks
**Team Structure:** 3 parallel teams + 1 integration/testing team

---

## Executive Overview

Phase 2.0 implements three critical security features that build directly on Phase 1.9's authentication foundation:

1. **API Rate Limiting** - Prevent brute force attacks and DDoS
2. **Audit Logging** - Track all access for compliance (HIPAA)
3. **Token Management** - Refresh, revocation, and lifecycle management

These features are interdependent and will be implemented in parallel teams with careful integration points.

---

## Architecture Overview

```
HTTP Request
    ↓
Spring Boot Application
    ↓
SecurityFilterChain (Phase 1.9)
    ├─ CorsFilter
    ├─ JwtAuthenticationFilter (Phase 1.9)
    │
    ├─ RateLimitingFilter (Phase 2.0 - NEW)
    │  ├─ Redis-based token bucket algorithm
    │  ├─ Per-tenant rate limits
    │  ├─ Per-role rate limits
    │  └─ Returns 429 Too Many Requests
    │
    ├─ AuditLoggingFilter (Phase 2.0 - NEW)
    │  ├─ Captures request metadata
    │  ├─ Logs before routing to controller
    │  ├─ Includes user, tenant, role, endpoint
    │  └─ Async logging to prevent performance impact
    │
    ├─ AuthorizationFilter (Phase 1.9)
    └─ ExceptionTranslationFilter
    ↓
@PreAuthorize (Phase 1.9)
    ↓
Controller Business Logic
    ↓
Response + AuditLog (async)
```

---

## Team 1: API Rate Limiting

### Objectives
- Prevent brute force attacks on authentication endpoints
- Implement sliding window rate limiting using Redis
- Support per-tenant and per-role rate limits
- Return proper 429 HTTP status with retry-after header

### Architecture

```
RateLimitingFilter
├─ Configuration
│  ├─ Default: 1000 requests per minute (global)
│  ├─ Authentication endpoints: 10 requests per minute
│  ├─ Per-tenant override capability
│  └─ Per-role premium limits (ADMIN: 2x, EVALUATOR: 1.5x)
│
├─ Redis Backend
│  ├─ Key format: "ratelimit:{clientId}:{endpoint}"
│  ├─ Token bucket sliding window
│  ├─ TTL: 60 seconds (or configurable)
│  └─ Atomic operations (INCR, EXPIRE)
│
├─ Client Identification
│  ├─ User ID from JWT
│  ├─ Or IP address if unauthenticated
│  ├─ Tenant ID from X-Tenant-ID header
│  └─ Endpoint path from request
│
└─ Response Handling
   ├─ Success: X-RateLimit-* headers
   ├─ Limit exceeded: 429 Too Many Requests
   ├─ Retry-After header: seconds until reset
   └─ X-RateLimit-Remaining: requests left
```

### Configuration Structure

```yaml
# application.yml
security:
  rate-limiting:
    enabled: true
    backend: redis

    # Global defaults
    default-limit-per-minute: 1000

    # Endpoint-specific overrides
    endpoints:
      - path: /auth/login
        limit-per-minute: 10
        description: "Prevent brute force login attacks"

      - path: /auth/refresh
        limit-per-minute: 100
        description: "Token refresh endpoint"

      - path: /api/v1/**
        limit-per-minute: 1000
        description: "Standard API endpoints"

    # Per-role multipliers
    role-multipliers:
      SUPER_ADMIN: 10x
      ADMIN: 2x
      EVALUATOR: 1.5x
      ANALYST: 1x
      VIEWER: 0.5x

    # Per-tenant overrides
    tenant-overrides:
      TENANT_PREMIUM_001: 5000
      TENANT_STANDARD_002: 1000
```

### Key Components

**RateLimitingFilter.java:**
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final RateLimitConfiguration config;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Extract client identifier
        String clientId = extractClientId(request);
        String endpoint = request.getRequestURI();
        String tenantId = request.getHeader("X-Tenant-ID");

        // Check rate limit
        RateLimitResult result = rateLimitService.checkLimit(
            clientId, endpoint, tenantId);

        // Add response headers
        response.setHeader("X-RateLimit-Limit",
            String.valueOf(result.getLimit()));
        response.setHeader("X-RateLimit-Remaining",
            String.valueOf(result.getRemaining()));
        response.setHeader("X-RateLimit-Reset",
            String.valueOf(result.getResetTime()));

        if (!result.isAllowed()) {
            // Return 429 Too Many Requests
            response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            response.setHeader("Retry-After",
                String.valueOf(result.getRetryAfterSeconds()));

            ErrorResponse error = ErrorResponse.builder()
                .error("Rate Limit Exceeded")
                .message("Too many requests. Retry after " +
                    result.getRetryAfterSeconds() + " seconds")
                .build();

            response.getWriter().write(
                objectMapper.writeValueAsString(error));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractClientId(HttpServletRequest request) {
        // First try JWT user ID
        String userId = extractUserIdFromJwt(request);
        if (userId != null) {
            return userId;
        }
        // Fall back to IP address
        return extractClientIp(request);
    }
}
```

**RateLimitService.java:**
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RateLimitConfiguration config;

    public RateLimitResult checkLimit(
            String clientId, String endpoint, String tenantId) {

        // Get limit configuration
        RateLimitConfig endpointConfig =
            config.getConfigForEndpoint(endpoint);
        int limit = endpointConfig.getLimitPerMinute();

        // Apply role multiplier if authenticated
        limit = applyRoleMultiplier(limit, extractRole());

        // Apply tenant override if exists
        if (config.hasTenantOverride(tenantId)) {
            limit = config.getTenantLimit(tenantId);
        }

        // Redis key format
        String key = String.format("ratelimit:%s:%s",
            clientId, endpoint);

        // Atomic increment
        long current = redisTemplate.opsForValue()
            .increment(key);

        // Set expiration on first request
        if (current == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }

        // Build result
        return RateLimitResult.builder()
            .limit(limit)
            .current(current)
            .remaining(Math.max(0, limit - current))
            .isAllowed(current <= limit)
            .resetTime(getResetTime(key))
            .retryAfterSeconds(
                getRetryAfterSeconds(key, limit, current))
            .build();
    }
}
```

### Testing Strategy

```java
@WebMvcTest
@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowFirstRequest() {
        // First request should succeed
        mockMvc.perform(get("/api/v1/patients/123")
                .header("X-Tenant-ID", "tenant-001"))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-RateLimit-Remaining"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void shouldEnforcePerRoleLimit() {
        // VIEWER has 0.5x multiplier - lower limit
        // Make enough requests to exceed VIEWER limit
        // Should get 429
    }

    @Test
    void shouldAllow10LoginAttemptsPerMinute() {
        // Login endpoint has special limit
        // 11th attempt should get 429
    }

    @Test
    void shouldReturnProperRetryAfterHeader() {
        // When rate limited, should include Retry-After
        mockMvc.perform(get("/api/v1/patients/123")
                .header("X-Tenant-ID", "tenant-001"))
            .andExpect(header().exists("Retry-After"));
    }
}
```

---

## Team 2: Audit Logging

### Objectives
- Track all endpoint access for compliance
- Log authentication attempts (successful and failed)
- HIPAA-compliant logging without storing PHI in logs
- Async logging to prevent performance impact

### Architecture

```
AuditLoggingFilter
├─ Captures Request
│  ├─ Timestamp (precise to millisecond)
│  ├─ User ID / Anonymous
│  ├─ Tenant ID
│  ├─ Role(s)
│  ├─ HTTP Method + Path
│  ├─ Request parameters (sanitized)
│  ├─ Client IP
│  └─ User-Agent
│
├─ Captures Response
│  ├─ HTTP Status Code
│  ├─ Response time (ms)
│  ├─ Success/Failure
│  └─ Authorization result (allowed/denied)
│
├─ Async Processing
│  ├─ Non-blocking writes to audit log
│  ├─ Queue-based processing
│  ├─ Batch writes for performance
│  └─ Fallback if async queue fails
│
└─ Storage Backends
   ├─ PostgreSQL (structured queries)
   ├─ Elasticsearch (searchable logs)
   ├─ File system (backup/compliance)
   └─ Syslog (remote logging)
```

### Audit Log Schema

```sql
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Request metadata
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    request_path TEXT NOT NULL,
    query_parameters TEXT,  -- JSON

    -- User/Security context
    user_id VARCHAR(255),
    username VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    roles VARCHAR(255),  -- Comma-separated

    -- Network
    client_ip VARCHAR(45),  -- IPv4/IPv6
    user_agent TEXT,

    -- Response
    http_status_code INTEGER,
    response_time_ms INTEGER,

    -- Authorization
    authorization_allowed BOOLEAN,
    required_role VARCHAR(255),

    -- Additional context
    error_message TEXT,
    trace_id VARCHAR(255),  -- OpenTelemetry trace ID

    -- Indexes
    INDEX idx_timestamp (timestamp),
    INDEX idx_user_id (user_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_http_status (http_status_code),
    INDEX idx_trace_id (trace_id)
);
```

### Key Components

**AuditLoggingFilter.java:**
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLoggingFilter extends OncePerRequestFilter {

    private final AuditLogService auditLogService;
    private final AuditLogConfiguration config;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        // Wrap response to capture status code
        HttpServletResponseWrapper responseWrapper =
            new HttpServletResponseWrapper(response);

        // Wrap request to capture body if needed
        HttpServletRequestWrapper requestWrapper =
            new HttpServletRequestWrapper(request);

        AuditLogEntry entry = AuditLogEntry.builder()
            .timestamp(Instant.now())
            .httpMethod(request.getMethod())
            .requestPath(request.getRequestURI())
            .queryParameters(request.getQueryString())
            .clientIp(extractClientIp(request))
            .userAgent(request.getHeader("User-Agent"))
            .traceId(extractTraceId(request))
            .tenantId(request.getHeader("X-Tenant-ID"))
            .build();

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long endTime = System.currentTimeMillis();

            // Populate response data
            entry.setHttpStatusCode(responseWrapper.getStatus());
            entry.setResponseTimeMs(endTime - startTime);
            entry.setSuccess(isSuccessStatus(responseWrapper.getStatus()));

            // Extract security context
            populateSecurityContext(entry);

            // Submit async logging
            auditLogService.logAsync(entry);
        }
    }

    private void populateSecurityContext(AuditLogEntry entry) {
        Authentication auth = SecurityContextHolder
            .getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()) {
            entry.setUserId(extractUserId(auth));
            entry.setUsername(auth.getName());
            entry.setRoles(auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(",")));
        }
    }
}
```

**AuditLogService.java:**
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository repository;
    private final AuditLogQueueService queueService;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    public void logAsync(AuditLogEntry entry) {
        try {
            // Save to database
            AuditLog savedLog = repository.save(
                AuditLog.fromEntry(entry));

            // Publish event for other systems
            eventPublisher.publishEvent(
                new AuditLogCreatedEvent(savedLog));

            // Log to file if configured
            if (config.isFileLoggingEnabled()) {
                logToFile(entry);
            }

            // Send to Elasticsearch if configured
            if (config.isElasticsearchEnabled()) {
                sendToElasticsearch(entry);
            }
        } catch (Exception e) {
            // Fall back to queue if primary fails
            log.warn("Failed to log audit entry, queueing", e);
            queueService.enqueue(entry);
        }
    }

    public Page<AuditLog> searchLogs(AuditLogSearchCriteria criteria) {
        // Support rich querying
        // - By user ID, tenant ID, date range
        // - By HTTP status, authorization result
        // - By trace ID (OpenTelemetry integration)
    }
}
```

### HIPAA Compliance

```java
@Component
public class AuditLogSanitizer {

    // Do NOT log these fields
    private static final Set<String> RESTRICTED_FIELDS =
        Set.of("password", "ssn", "dob", "mrn", "medical_record");

    public void sanitizeEntry(AuditLogEntry entry) {
        // Remove any PHI from query parameters
        if (entry.getQueryParameters() != null) {
            String sanitized = redactPhiFields(
                entry.getQueryParameters());
            entry.setQueryParameters(sanitized);
        }

        // Ensure patient IDs are logged (needed for audit trail)
        // but not medical data
        // - OK: patient_id=12345
        // - NOT OK: diagnosis=Type2Diabetes
    }

    private String redactPhiFields(String params) {
        // Replace sensitive values with [REDACTED]
        for (String field : RESTRICTED_FIELDS) {
            params = params.replaceAll(
                field + "=[^&]*",
                field + "=[REDACTED]");
        }
        return params;
    }
}
```

### Testing Strategy

```java
@WebMvcTest
class AuditLoggingFilterTest {

    @Test
    @WithMockUser(username = "john.doe", roles = "ADMIN")
    void shouldCaptureSuccessfulAuthorizedRequest() {
        mockMvc.perform(get("/api/v1/patients/123")
                .header("X-Tenant-ID", "tenant-001"))
            .andExpect(status().isOk());

        // Verify audit log created
        AuditLog log = auditLogRepository.findLatest();
        assertThat(log.getUsername()).isEqualTo("john.doe");
        assertThat(log.getHttpStatusCode()).isEqualTo(200);
        assertThat(log.isSuccess()).isTrue();
    }

    @Test
    void shouldCaptureUnauthorizedRequest() {
        mockMvc.perform(get("/api/v1/patients/123")
                .header("X-Tenant-ID", "tenant-001"))
            .andExpect(status().isUnauthorized());

        AuditLog log = auditLogRepository.findLatest();
        assertThat(log.getHttpStatusCode()).isEqualTo(401);
        assertThat(log.getUserId()).isNull();
    }

    @Test
    void shouldSanitizePhiFromLogs() {
        mockMvc.perform(get("/api/v1/patients?ssn=123-45-6789")
                .header("X-Tenant-ID", "tenant-001"))
            .andExpect(status().isUnauthorized());

        AuditLog log = auditLogRepository.findLatest();
        assertThat(log.getQueryParameters())
            .contains("ssn=[REDACTED]")
            .doesNotContain("123-45-6789");
    }

    @Test
    void shouldCaptureResponseTime() {
        mockMvc.perform(get("/api/v1/patients/123")
                .header("X-Tenant-ID", "tenant-001"))
            .andExpect(status().isOk());

        AuditLog log = auditLogRepository.findLatest();
        assertThat(log.getResponseTimeMs())
            .isGreaterThan(0)
            .isLessThan(5000);
    }
}
```

---

## Team 3: Token Management

### Objectives
- Implement token refresh endpoint
- Add token revocation/blacklist capability
- Implement sliding window token extension
- Support token versioning for security updates

### Architecture

```
TokenManagementService
├─ Token Refresh (/auth/refresh)
│  ├─ Validate refresh token signature
│  ├─ Check if not in blacklist
│  ├─ Check if not expired
│  ├─ Generate new access token
│  ├─ Optionally rotate refresh token
│  └─ Return new token pair
│
├─ Token Revocation (/auth/revoke)
│  ├─ Add token to blacklist in Redis
│  ├─ Set expiration = token exp claim
│  ├─ Log revocation in audit trail
│  └─ Return 200 OK
│
├─ Token Blacklist (Redis)
│  ├─ Key: "blacklist:{jti}"
│  ├─ Value: revocation_reason
│  ├─ TTL: token.exp - now
│  └─ Checked on every request
│
├─ Sliding Window Extension
│  ├─ Track token creation time
│  ├─ Auto-extend on activity (optional)
│  ├─ Max session duration (hard limit)
│  └─ Configurable window sizes
│
└─ Token Versioning
   ├─ Version claim in JWT
   ├─ Revoke all tokens < version
   ├─ Security update capability
   └─ Backward compatibility mode
```

### Configuration Structure

```yaml
# application.yml
security:
  jwt:
    # Token lifetimes
    access-token-ttl-minutes: 15
    refresh-token-ttl-days: 7

    # Refresh strategy
    refresh:
      enabled: true
      rotate-on-refresh: true  # Issue new refresh token
      max-refresh-count: 10    # Max times to refresh

    # Revocation
    revocation:
      enabled: true
      blacklist-backend: redis
      storage-ttl: "token-exp"  # Clear when token expires

    # Sliding window
    sliding-window:
      enabled: false
      extension-minutes: 5
      max-session-duration-hours: 8

    # Versioning
    versioning:
      enabled: true
      current-version: 2
      deprecated-versions: [1]
```

### Key Components

**TokenRefreshEndpoint.java:**
```java
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class TokenRefreshEndpoint {

    private final TokenManagementService tokenService;
    private final AuditLogService auditLogService;

    @PostMapping("/refresh")
    @PreAuthorize("permitAll()")
    public ResponseEntity<TokenResponse> refresh(
            @RequestBody RefreshTokenRequest request) {

        try {
            // Validate refresh token
            RefreshToken refreshToken =
                tokenService.validateRefreshToken(
                    request.getRefreshToken());

            // Generate new token pair
            TokenPair newTokens = tokenService.refreshTokens(
                refreshToken);

            // Log successful refresh
            auditLogService.logTokenRefresh(
                refreshToken.getUserId(),
                refreshToken.getTenantId());

            return ResponseEntity.ok(
                TokenResponse.fromTokenPair(newTokens));

        } catch (InvalidTokenException e) {
            // Log failed refresh attempt
            auditLogService.logFailedTokenRefresh(
                request.getRefreshToken(),
                e.getMessage());

            return ResponseEntity.status(401)
                .body(ErrorResponse.builder()
                    .error("Invalid Refresh Token")
                    .message("Refresh token is invalid or expired")
                    .build());
        }
    }

    @PostMapping("/revoke")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> revoke(
            @RequestBody RevokeTokenRequest request) {

        String jti = extractJti(request.getToken());
        String userId = getCurrentUserId();

        // Revoke token
        tokenService.revokeToken(jti,
            "User requested revocation");

        // Log revocation
        auditLogService.logTokenRevocation(userId, jti);

        return ResponseEntity.ok().build();
    }
}
```

**TokenManagementService.java:**
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenManagementService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final RefreshTokenRepository repository;
    private final TokenBlacklistService blacklistService;

    public TokenPair refreshTokens(RefreshToken refreshToken) {
        // Extract user claims from refresh token
        JwtClaimsSet claims = extractClaims(refreshToken);

        // Generate new access token
        String newAccessToken = generateAccessToken(claims);

        // Optionally rotate refresh token
        String newRefreshToken = refreshToken.getToken();
        if (config.isRotateOnRefresh()) {
            newRefreshToken = generateRefreshToken(claims);

            // Invalidate old refresh token
            repository.delete(refreshToken);
        }

        // Track refresh count for audit
        refreshToken.incrementRefreshCount();
        repository.save(refreshToken);

        return TokenPair.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .expiresIn(config.getAccessTokenTtlMinutes() * 60)
            .build();
    }

    public void revokeToken(String jti, String reason) {
        // Add to blacklist with expiration
        String blacklistKey = "blacklist:" + jti;
        long ttlSeconds = calculateTtl(jti);

        redisTemplate.opsForValue().set(
            blacklistKey, reason,
            Duration.ofSeconds(ttlSeconds));

        log.info("Token {} revoked: {}", jti, reason);
    }

    public boolean isTokenBlacklisted(String jti) {
        String blacklistKey = "blacklist:" + jti;
        return redisTemplate.hasKey(blacklistKey);
    }

    public void validateTokenNotBlacklisted(String jti) {
        if (isTokenBlacklisted(jti)) {
            throw new InvalidTokenException(
                "Token has been revoked");
        }
    }

    public RefreshToken validateRefreshToken(String token) {
        // Validate signature
        Jwt jwt = jwtDecoder.decode(token);

        // Check blacklist
        String jti = jwt.getClaimAsString("jti");
        validateTokenNotBlacklisted(jti);

        // Check repository
        String userId = jwt.getSubject();
        return repository.findByUserIdAndToken(userId, token)
            .orElseThrow(() -> new InvalidTokenException(
                "Refresh token not found"));
    }
}
```

**TokenBlacklistFilter.java:**
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistFilter extends OncePerRequestFilter {

    private final TokenManagementService tokenService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // After JWT authentication, check if token is blacklisted
        try {
            Authentication auth = SecurityContextHolder
                .getContext().getAuthentication();

            if (auth instanceof JwtAuthenticationToken) {
                JwtAuthenticationToken jwtAuth =
                    (JwtAuthenticationToken) auth;

                String jti = extractJti(jwtAuth.getToken());

                if (tokenService.isTokenBlacklisted(jti)) {
                    // Token is revoked
                    SecurityContextHolder.clearContext();

                    response.setStatus(
                        HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write(
                        "Token has been revoked");
                    return;
                }
            }
        } catch (Exception e) {
            log.error("Error checking token blacklist", e);
            // Continue with request if blacklist check fails
        }

        filterChain.doFilter(request, response);
    }
}
```

### Sliding Window Token Extension

```java
@Service
@RequiredArgsConstructor
public class SlidingWindowTokenService {

    public TokenRefreshResponse extendTokenIfNecessary(
            String token, HttpServletResponse response) {

        Jwt jwt = decodeToken(token);
        Instant tokenCreated = jwt.getIssuedAt();
        Instant now = Instant.now();

        // Check if token is within sliding window
        Duration tokenAge = Duration.between(tokenCreated, now);
        Duration slidingWindow = config.getSlidingWindowDuration();

        if (tokenAge.compareTo(slidingWindow) > 0) {
            // Token is old enough to refresh
            return refreshAndExtendToken(jwt);
        }

        // Token is still fresh, no refresh needed
        return null;
    }
}
```

### Testing Strategy

```java
@WebMvcTest
class TokenManagementTest {

    @Test
    void shouldRefreshValidRefreshToken() {
        String refreshToken = createValidRefreshToken();

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RefreshTokenRequest(refreshToken))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").exists())
            .andExpect(jsonPath("$.refresh_token").exists())
            .andExpect(jsonPath("$.expires_in").value(900)); // 15 min
    }

    @Test
    void shouldRejectExpiredRefreshToken() {
        String expiredToken = createExpiredRefreshToken();

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RefreshTokenRequest(expiredToken))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error")
                .value("Invalid Refresh Token"));
    }

    @Test
    void shouldRejectRevokedToken() {
        String token = createValidToken();
        tokenService.revokeToken(extractJti(token), "Test");

        mockMvc.perform(get("/api/v1/patients/123")
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-ID", "tenant-001"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldExtendTokenWithinSlidingWindow() {
        String token = createTokenCreatedMinutesAgo(4);

        TokenRefreshResponse response =
            slidingWindowService.extendTokenIfNecessary(token);

        assertThat(response).isNotNull();
        assertThat(response.getNewAccessToken()).isNotEmpty();
    }
}
```

---

## Integration Points

### 1. Filter Chain Order

```java
@Configuration
@RequiredArgsConstructor
public class SecurityFilterChainConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
            // Order matters!
            .addFilterBefore(corsFilter,
                CorsFilter.class)

            .addFilterBefore(jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class)

            .addFilterAfter(tokenBlacklistFilter,    // Phase 2.0
                JwtAuthenticationFilter.class)

            .addFilterAfter(rateLimitingFilter,      // Phase 2.0
                TokenBlacklistFilter.class)

            .addFilterAfter(auditLoggingFilter,      // Phase 2.0
                RateLimitingFilter.class)

            .addFilterAfter(authorizationFilter,
                AuditLoggingFilter.class);

        return http.build();
    }
}
```

### 2. Error Handling Integration

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(
            RateLimitExceededException e) {
        return ResponseEntity
            .status(429)  // Too Many Requests
            .body(ErrorResponse.builder()
                .error("Rate Limit Exceeded")
                .message(e.getMessage())
                .retryAfter(e.getRetryAfterSeconds())
                .build());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(
            InvalidTokenException e) {
        return ResponseEntity
            .status(401)  // Unauthorized
            .body(ErrorResponse.builder()
                .error("Invalid Token")
                .message(e.getMessage())
                .build());
    }
}
```

### 3. OpenTelemetry Integration

All three features emit spans:

```
TokenRefresh Span
├─ Attributes: user_id, tenant_id, token_version
├─ Events: token_validated, token_generated, stored

RateLimitCheck Span
├─ Attributes: client_id, endpoint, limit, current
├─ Events: limit_checked, limit_exceeded

AuditLog Span
├─ Attributes: user_id, tenant_id, http_method, status
├─ Events: request_received, response_sent
```

---

## Implementation Sequence

### Week 1: Core Infrastructure
- [ ] Create Redis configuration for rate limiting & token blacklist
- [ ] Set up PostgreSQL audit log schema
- [ ] Implement base filter classes

### Week 2: Team 1 - Rate Limiting
- [ ] Implement RateLimitingFilter
- [ ] Create RateLimitService with Redis backend
- [ ] Add configuration management
- [ ] Write comprehensive tests

### Week 2: Team 2 - Audit Logging
- [ ] Implement AuditLoggingFilter
- [ ] Create AuditLogService
- [ ] Add PHI sanitization
- [ ] Implement async logging queue

### Week 3: Team 3 - Token Management
- [ ] Implement token refresh endpoint
- [ ] Create token revocation/blacklist
- [ ] Add TokenManagementService
- [ ] Implement sliding window extension

### Week 3: Integration & Testing
- [ ] Integration tests for all 3 features
- [ ] End-to-end scenario testing
- [ ] Performance benchmarking
- [ ] Load testing

### Week 4: Documentation & Deployment
- [ ] Write implementation guide
- [ ] Create operations runbook
- [ ] Performance tuning
- [ ] Deployment to staging

---

## Success Criteria

### Rate Limiting
- ✅ 429 returned when limit exceeded
- ✅ Per-role and per-tenant limits working
- ✅ Performance impact < 5ms per request
- ✅ Redis failover handled gracefully

### Audit Logging
- ✅ All endpoint access logged
- ✅ PHI fields properly sanitized
- ✅ Async logging operational
- ✅ Elasticsearch integration working

### Token Management
- ✅ Refresh endpoint operational
- ✅ Token revocation working
- ✅ Blacklist checked on every request
- ✅ Sliding window extension optional

### Overall
- ✅ All 140+ Phase 1.9 tests still passing
- ✅ 200+ new tests for Phase 2.0 features
- ✅ No breaking changes to existing APIs
- ✅ Documentation complete
- ✅ Performance benchmarks met

---

## Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| Redis rate limit bottleneck | Use pipelining, cluster configuration |
| Async audit logs lost | Implement disk queue fallback |
| Token refresh complexity | Comprehensive test coverage |
| Performance degradation | Performance tests in each sprint |
| Token blacklist memory | Automatic TTL cleanup in Redis |

---

## Success Metrics

- **Performance:** < 5ms overhead per request
- **Reliability:** 99.9% uptime for auth services
- **Compliance:** 100% audit log coverage, HIPAA compliant
- **Security:** Zero token bypass incidents
- **Testing:** 95%+ code coverage

---

_Next Steps: Begin Team 1 (Rate Limiting) implementation_
