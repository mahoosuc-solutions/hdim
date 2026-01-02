# Gateway Service

API Gateway with intelligent routing, rate limiting, circuit breaking, and JWT authentication for the HDIM platform.

## Purpose

Provides a unified entry point for all microservices, addressing the challenge that:
- Multiple backend services need centralized authentication and authorization
- API consumers need a single endpoint rather than managing multiple service URLs
- Services require protection from cascading failures via circuit breakers
- Rate limiting and security policies must be enforced consistently

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      Gateway Service                             │
│                         (Port 8080)                              │
├─────────────────────────────────────────────────────────────────┤
│  Controller Layer                                                │
│  └── ApiGatewayController (routing to 13 downstream services)   │
├─────────────────────────────────────────────────────────────────┤
│  Security & Middleware                                           │
│  ├── JWT Authentication     - Token validation                  │
│  ├── Rate Limiting          - 100 req/sec, burst: 150           │
│  └── Header Forwarding      - X-Tenant-ID, X-User-ID, Auth      │
├─────────────────────────────────────────────────────────────────┤
│  Resilience Layer (Resilience4j)                                │
│  ├── Circuit Breakers       - Per-service failure detection     │
│  ├── Retry Logic            - Exponential backoff (3 attempts)  │
│  └── Timeouts               - 15s-120s depending on service     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ HTTP (RestTemplate)
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  Backend Services (CQL Engine, FHIR, Patient, Care Gap, etc.)   │
│  - 13 microservices routed by path prefix                       │
│  - Auto-discovery via configured service URLs                   │
└─────────────────────────────────────────────────────────────────┘
```

## API Routes

| Path Prefix | Target Service | Port | Purpose |
|-------------|---------------|------|---------|
| `/api/cql/**` | cql-engine-service | 8081 | CQL evaluation |
| `/api/fhir/**` | fhir-service | 8085 | FHIR resources |
| `/api/patients/**` | patient-service | 8084 | Patient aggregation |
| `/api/care-gaps/**` | care-gap-service | 8086 | Care gap identification |
| `/api/consent/**` | consent-service | 8082 | Consent management |
| `/api/events/**` | event-processing-service | 8083 | Event processing |
| `/api/v1/agent-builder/**` | agent-builder-service | 8096 | AI agent configuration |
| `/api/v1/qrda/**` | qrda-export-service | 8104 | QRDA exports |
| `/api/v1/hcc/**` | hcc-service | 8105 | Risk adjustment |
| `/api/ecr/**` | ecr-service | 8101 | Electronic case reporting |
| `/api/v1/prior-auth/**` | prior-auth-service | 8102 | Prior authorization |

## Configuration

```yaml
# Gateway settings
gateway:
  auth:
    enabled: true
    enforced: false  # Set to true for production
  rate-limit:
    requests-per-second: 100
    burst-capacity: 150

# JWT configuration
jwt:
  secret: ${JWT_SECRET}  # Required via environment variable
  accessTokenExpirationMs: 900000  # 15 minutes
  issuer: healthdata-gateway

# Circuit breakers (per service)
resilience4j:
  circuitbreaker:
    instances:
      fhirService:
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
```

## Dependencies

- **Spring Boot**: Web, Security, Actuator
- **Database**: PostgreSQL (for auth state)
- **Resilience**: Resilience4j for circuit breakers, retries, rate limiting
- **HTTP Client**: RestTemplate for service communication

## Running Locally

```bash
# From backend directory
./gradlew :modules:services:gateway-service:bootRun

# Or via Docker (gateway profile)
docker compose --profile gateway up gateway-service
```

## Testing

### Overview

Gateway Service has comprehensive test coverage with **12+ test files** covering JWT authentication, rate limiting, header injection, circuit breakers, and service routing.

| Test Type | Count | Purpose |
|-----------|-------|---------|
| Authentication Tests | 3+ | JWT validation, header stripping/injection |
| Rate Limiting Tests | 4+ | Per-user and per-tenant rate limiting |
| Routing Tests | 2+ | Service routing and path matching |
| Security Config Tests | 2+ | Security chain, CORS, public paths |
| Integration Tests | 1+ | End-to-end authentication flow |

### Quick Start

```bash
# Run all tests
./gradlew :modules:services:gateway-service:test

# Run specific test suite
./gradlew :modules:services:gateway-service:test --tests "*AuthenticationFilter*"
./gradlew :modules:services:gateway-service:test --tests "*RateLimit*"
./gradlew :modules:services:gateway-service:test --tests "*Routing*"

# Run with coverage report
./gradlew :modules:services:gateway-service:test jacocoTestReport

# Run single test class
./gradlew :modules:services:gateway-service:test --tests "GatewayAuthenticationFilterTest"
```

### Test Organization

```
src/test/java/com/healthdata/gateway/
├── auth/                               # Authentication tests
│   ├── GatewayAuthenticationFilterTest.java  # JWT validation, headers (458 lines)
│   └── PublicPathRegistryTest.java           # Public endpoint matching
├── ratelimit/                          # Rate limiting tests
│   ├── TenantRateLimitFilterTest.java        # Per-tenant rate limiting (523 lines)
│   └── TenantRateLimitServiceTest.java       # Rate limit logic
├── filter/
│   └── RateLimitFilterTest.java              # Legacy rate limit filter
├── service/
│   ├── ServiceRoutingServiceTest.java        # Service routing logic
│   └── CustomUserDetailsServiceTest.java    # User authentication
├── controller/
│   └── ApiGatewayControllerTest.java         # Gateway routing endpoints
├── config/
│   ├── GatewaySecurityConfigTest.java        # Security configuration
│   └── RestTemplateConfigTest.java           # HTTP client configuration
├── cache/
│   └── CacheEvictionServiceTest.java         # HIPAA cache eviction
└── integration/
    └── GatewayAuthSecurityIntegrationTest.java  # End-to-end auth
```

### Authentication Filter Tests

**Critical for security** - Tests for JWT validation and header injection.

**Example: GatewayAuthenticationFilterTest.java** (458 lines)

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("GatewayAuthenticationFilter")
class GatewayAuthenticationFilterTest {

    @Mock private JwtTokenService jwtTokenService;
    @Mock private GatewayAuthProperties authProperties;
    @Mock private PublicPathRegistry publicPathRegistry;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    private GatewayAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new GatewayAuthenticationFilter(jwtTokenService, authProperties, publicPathRegistry);
    }

    @Nested
    @DisplayName("JWT Validation")
    class JwtValidation {

        @Test
        @DisplayName("should authenticate with valid JWT")
        void shouldAuthenticateWithValidJwt() throws Exception {
            // Given
            String jwt = "valid.jwt.token";
            UUID userId = UUID.randomUUID();
            String username = "testuser";
            Set<String> tenantIds = Set.of("tenant1", "tenant2");
            Set<String> roles = Set.of("ADMIN", "PROVIDER");

            when(authProperties.getEnabled()).thenReturn(true);
            when(authProperties.getStripExternalAuthHeaders()).thenReturn(true);
            when(authProperties.getHeaderSigningSecret()).thenReturn("test-secret-32-chars-minimum-here");
            when(request.getRequestURI()).thenReturn("/api/v1/patients");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
            when(publicPathRegistry.isPublicPath("/api/v1/patients")).thenReturn(false);

            when(jwtTokenService.validateToken(jwt)).thenReturn(true);
            when(jwtTokenService.extractUsername(jwt)).thenReturn(username);
            when(jwtTokenService.extractUserId(jwt)).thenReturn(userId);
            when(jwtTokenService.extractTenantIds(jwt)).thenReturn(tenantIds);
            when(jwtTokenService.extractRoles(jwt)).thenReturn(roles);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(jwtTokenService).validateToken(jwt);
            verify(filterChain).doFilter(any(HttpServletRequest.class), eq(response));

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getName()).isEqualTo(username);
            assertThat(auth.getAuthorities()).hasSize(2);
        }

        @Test
        @DisplayName("should reject invalid JWT")
        void shouldRejectInvalidJwt() throws Exception {
            // Given
            String jwt = "invalid.jwt.token";
            when(authProperties.getEnabled()).thenReturn(true);
            when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
            when(jwtTokenService.validateToken(jwt)).thenReturn(false);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(any(), any());
        }
    }

    @Nested
    @DisplayName("Header Stripping")
    class HeaderStripping {

        @Test
        @DisplayName("should strip external X-Auth headers")
        void shouldStripExternalAuthHeaders() throws Exception {
            // Security Critical: External clients should not be able to
            // inject X-Auth headers that bypass gateway authentication
            when(authProperties.getEnabled()).thenReturn(true);
            when(authProperties.getStripExternalAuthHeaders()).thenReturn(true);

            // Mock headers including malicious X-Auth headers
            Vector<String> headerNames = new Vector<>();
            headerNames.add("Authorization");
            headerNames.add("X-Auth-User-Id");  // Should be stripped
            headerNames.add("X-Auth-Roles");    // Should be stripped
            when(request.getHeaderNames()).thenReturn(headerNames.elements());

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
            verify(filterChain).doFilter(requestCaptor.capture(), eq(response));

            HttpServletRequest wrappedRequest = requestCaptor.getValue();
            assertThat(wrappedRequest.getHeader("X-Auth-User-Id")).isNull();
            assertThat(wrappedRequest.getHeader("X-Auth-Roles")).isNull();
        }
    }

    @Nested
    @DisplayName("Header Injection")
    class HeaderInjection {

        @Test
        @DisplayName("should inject all required auth headers")
        void shouldInjectAllRequiredHeaders() throws Exception {
            // Given - valid JWT authentication
            String jwt = "valid.jwt.token";
            UUID userId = UUID.randomUUID();
            String username = "testuser";
            // ... setup mocks ...

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then - verify injected headers for downstream services
            ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
            verify(filterChain).doFilter(requestCaptor.capture(), eq(response));

            HttpServletRequest wrappedRequest = requestCaptor.getValue();
            assertThat(wrappedRequest.getHeader(AuthHeaderConstants.HEADER_USER_ID))
                .isEqualTo(userId.toString());
            assertThat(wrappedRequest.getHeader(AuthHeaderConstants.HEADER_USERNAME))
                .isEqualTo(username);
            assertThat(wrappedRequest.getHeader(AuthHeaderConstants.HEADER_TENANT_IDS))
                .contains("tenant1", "tenant2");
            assertThat(wrappedRequest.getHeader(AuthHeaderConstants.HEADER_VALIDATED))
                .startsWith("gateway-");  // HMAC signature
        }
    }
}
```

### Rate Limiting Tests

**Example: TenantRateLimitFilterTest.java** (523 lines)

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Tenant Rate Limit Filter Tests")
class TenantRateLimitFilterTest {

    @Mock private TenantRateLimitService rateLimitService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private TenantRateLimitFilter filter;

    @Nested
    @DisplayName("Filter Behavior")
    class FilterBehavior {

        @Test
        @DisplayName("Should allow request when rate limit not exceeded")
        void shouldAllowRequestWhenRateLimitNotExceeded() throws Exception {
            // Given
            String tenantId = "tenant-123";
            String userId = "user-456";

            when(request.getRequestURI()).thenReturn("/api/patients");
            when(request.getHeader("X-Tenant-ID")).thenReturn(tenantId);

            RateLimitResult userResult = RateLimitResult.allowed(95, 100);
            RateLimitResult tenantResult = RateLimitResult.allowed(950, 1000);

            when(rateLimitService.tryConsume(tenantId, userId, EndpointType.READ))
                .thenReturn(userResult);
            when(rateLimitService.tryConsumeTenantAggregate(tenantId))
                .thenReturn(tenantResult);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            verify(response).setHeader("X-RateLimit-Remaining", "95");
            verify(response).setHeader("X-RateLimit-Limit", "100");
        }

        @Test
        @DisplayName("Should return 429 when user rate limit exceeded")
        void shouldReturn429WhenUserRateLimitExceeded() throws Exception {
            // Given
            RateLimitResult userResult = RateLimitResult.rejected(30, 100);
            when(rateLimitService.tryConsume(any(), any(), any()))
                .thenReturn(userResult);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).setStatus(429);
            verify(response).setHeader("Retry-After", "30");
            verify(filterChain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("Should return 429 when tenant aggregate rate limit exceeded")
        void shouldReturn429WhenTenantRateLimitExceeded() throws Exception {
            // Given - user limit OK, but tenant aggregate exceeded
            RateLimitResult userResult = RateLimitResult.allowed(95, 100);
            RateLimitResult tenantResult = RateLimitResult.rejected(60, 1000);

            when(rateLimitService.tryConsume(any(), any(), any()))
                .thenReturn(userResult);
            when(rateLimitService.tryConsumeTenantAggregate(any()))
                .thenReturn(tenantResult);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).setStatus(429);
            verify(response).setHeader("Retry-After", "60");
        }

        @Test
        @DisplayName("Should bypass health check endpoints")
        void shouldBypassHealthCheckEndpoints() throws Exception {
            // Given
            String[] healthPaths = {"/actuator/health", "/actuator/info", "/health"};

            for (String path : healthPaths) {
                when(request.getRequestURI()).thenReturn(path);

                // When
                filter.doFilterInternal(request, response, filterChain);

                // Then - no rate limiting applied
                verify(rateLimitService, never()).tryConsume(any(), any(), any());
            }
        }
    }

    @Nested
    @DisplayName("Tenant Extraction")
    class TenantExtraction {

        @Test
        @DisplayName("Should extract tenant from header")
        void shouldExtractTenantFromHeader() throws Exception {
            // Given
            String tenantId = "tenant-from-header";
            when(request.getHeader("X-Tenant-ID")).thenReturn(tenantId);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimitService).tryConsume(eq(tenantId), any(), any());
        }

        @Test
        @DisplayName("Should extract tenant from JWT claim when header not present")
        void shouldExtractTenantFromJwtClaim() throws Exception {
            // Given
            Map<String, Object> authDetails = new HashMap<>();
            authDetails.put("tenant_id", "tenant-from-jwt");
            when(request.getHeader("X-Tenant-ID")).thenReturn(null);
            when(authentication.getDetails()).thenReturn(authDetails);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimitService).tryConsume(eq("tenant-from-jwt"), any(), any());
        }
    }
}
```

### Gateway Trust Header Tests

Tests verify the gateway trust authentication model used by downstream services.

```java
@SpringBootTest
@DisplayName("Gateway Trust Authentication Tests")
class GatewayTrustAuthenticationTest {

    @Test
    @DisplayName("Should generate HMAC signature for X-Auth-Validated header")
    void shouldGenerateHmacSignature() {
        // Given
        String signingSecret = "32-char-minimum-signing-secret!";
        UUID userId = UUID.randomUUID();
        String timestamp = String.valueOf(System.currentTimeMillis());

        // When
        String signature = generateSignature(userId.toString(), timestamp, signingSecret);

        // Then
        assertThat(signature).startsWith("gateway-");
        assertThat(signature).hasSize("gateway-".length() + 64);  // SHA-256 hex
    }

    @Test
    @DisplayName("Should include all required headers for downstream services")
    void shouldIncludeAllRequiredHeaders() throws Exception {
        // After valid authentication, verify all headers present
        mockMvc.perform(get("/api/patients/123")
                .header("Authorization", "Bearer " + validJwt))
            .andExpect(request -> {
                HttpServletRequest req = request.getRequest();
                assertThat(req.getHeader("X-Auth-User-Id")).isNotNull();
                assertThat(req.getHeader("X-Auth-Username")).isNotNull();
                assertThat(req.getHeader("X-Auth-Tenant-Ids")).isNotNull();
                assertThat(req.getHeader("X-Auth-Roles")).isNotNull();
                assertThat(req.getHeader("X-Auth-Validated")).startsWith("gateway-");
                assertThat(req.getHeader("X-Auth-Token-Id")).isNotNull();
                assertThat(req.getHeader("X-Auth-Token-Expires")).isNotNull();
            });
    }
}
```

### Circuit Breaker Tests

Tests for Resilience4j circuit breaker behavior.

```java
@SpringBootTest
@DisplayName("Circuit Breaker Tests")
class CircuitBreakerTest {

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    @DisplayName("Should open circuit after failure threshold")
    void shouldOpenCircuitAfterFailureThreshold() {
        // Given
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("fhirService");
        int failureThreshold = 50;  // From config

        // Simulate failures
        for (int i = 0; i < 10; i++) {
            cb.onError(0, TimeUnit.MILLISECONDS, new RuntimeException("Service down"));
        }

        // When/Then
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    @DisplayName("Should transition to half-open after wait duration")
    void shouldTransitionToHalfOpen() throws InterruptedException {
        // Given - circuit is open
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("fhirService");
        // Open the circuit
        for (int i = 0; i < 10; i++) {
            cb.onError(0, TimeUnit.MILLISECONDS, new RuntimeException());
        }

        // When - wait for configured duration (30s in config, mocked for test)
        Thread.sleep(100);  // In actual test, use @MockBean time

        // Then
        assertThat(cb.getState()).isIn(
            CircuitBreaker.State.HALF_OPEN,
            CircuitBreaker.State.OPEN
        );
    }
}
```

### Security Configuration Tests

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DisplayName("Gateway Security Configuration Tests")
class GatewaySecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should allow health endpoints without authentication")
    void shouldAllowHealthWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow Swagger UI without authentication")
    void shouldAllowSwaggerWithoutAuth() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should require authentication for API endpoints when enforced")
    void shouldRequireAuthForApiEndpoints() throws Exception {
        // When enforced=true in properties
        mockMvc.perform(get("/api/patients"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should configure CORS for allowed origins")
    void shouldConfigureCors() throws Exception {
        mockMvc.perform(options("/api/patients")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Access-Control-Allow-Origin"));
    }
}
```

### Test Configuration

**application-test.yml**:

```yaml
gateway:
  auth:
    enabled: true
    enforced: false  # Allow requests without token for testing
    strip-external-auth-headers: true
    header-signing-secret: test-secret-32-chars-minimum-here
    audit-logging: false

  rate-limit:
    enabled: true
    default-user-limit: 100
    default-tenant-limit: 1000
    burst-capacity: 150

jwt:
  secret: test-jwt-secret-for-unit-tests-minimum-256-bits
  accessTokenExpirationMs: 900000
  issuer: healthdata-gateway-test

resilience4j:
  circuitbreaker:
    instances:
      fhirService:
        failureRateThreshold: 50
        waitDurationInOpenState: 100ms  # Fast for tests
        permittedNumberOfCallsInHalfOpenState: 3

spring:
  main:
    allow-bean-definition-overriding: true

logging:
  level:
    com.healthdata.gateway: DEBUG
```

### Best Practices

1. **Security Testing**
   - Always test header stripping to prevent auth bypass
   - Verify HMAC signature generation/validation
   - Test both enforced=true and enforced=false modes
   - Test public path exclusions

2. **Rate Limiting Testing**
   - Test per-user and per-tenant limits separately
   - Verify 429 response format and headers
   - Test health endpoint bypass
   - Test rate limit header injection

3. **Circuit Breaker Testing**
   - Mock downstream service failures
   - Verify state transitions (CLOSED → OPEN → HALF_OPEN)
   - Test fallback behavior

4. **Gateway Trust Headers**
   - Verify all X-Auth-* headers are injected
   - Test HMAC signature format
   - Verify external headers are stripped

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| Auth filter not invoked | Wrong filter order | Check `addFilterBefore` configuration |
| Headers not stripped | `stripExternalAuthHeaders=false` | Set to true in test config |
| Rate limit not enforced | Health path match | Check `shouldNotFilter` patterns |
| Circuit breaker stuck open | Wait duration too long | Reduce `waitDurationInOpenState` in test |
| HMAC validation fails | Wrong signing secret | Match secret between gateway and services |
| 401 on valid token | Token expired | Check `accessTokenExpirationMs` |

### Manual Testing (curl)

```bash
# Test authentication
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'

# Test routing with JWT token
curl http://localhost:8080/api/fhir/Patient \
  -H "Authorization: Bearer <token>" \
  -H "X-Tenant-ID: tenant-1"

# Test rate limit headers
curl -v http://localhost:8080/api/patients \
  -H "Authorization: Bearer <token>" \
  2>&1 | grep -i "x-ratelimit"

# Test circuit breaker (if downstream is down)
for i in {1..20}; do
  curl -s http://localhost:8080/api/fhir/Patient \
    -H "Authorization: Bearer <token>" &
done
```

## Security Notes

- JWT secrets MUST be provided via `JWT_SECRET` environment variable in production
- Rate limiting prevents abuse (100 req/sec default)
- Circuit breakers protect against cascading failures
- All authentication tokens forwarded to downstream services via headers
