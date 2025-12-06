# ADR-0006: API Gateway with Resilience Patterns

**Status**: Accepted
**Date**: 2025-12-06
**Decision Makers**: Architecture Team
**Consulted**: Platform Engineering, Security Team
**Informed**: All Development Teams

## Context

**Problem Statement**:
The HDIM API Gateway needed comprehensive resilience patterns:

1. No rate limiting to protect backend services from overload
2. Backend service failures could cascade to all clients
3. No centralized circuit breaking at gateway level
4. Missing request routing with service discovery

**Business Context**:
- External API consumers need predictable response times
- SLA guarantees require graceful degradation
- Cost control through rate limiting
- Multi-tenant fair usage policies

**Technical Context**:
- Spring Boot 3.x Gateway Service
- 5 backend microservices (CQL Engine, Quality Measure, FHIR, Patient, Care Gap)
- JWT authentication at gateway
- Need for both per-user and per-tenant rate limits

## Decision

**We will implement a comprehensive API Gateway with Resilience4j circuit breakers, Bucket4j rate limiting, and intelligent service routing.**

**Specific Implementation**:

1. **Rate Limiting Strategy**:
   - Per-user limits (authenticated requests)
   - Per-tenant limits (aggregate across users)
   - Per-IP limits (unauthenticated/fallback)
   - Different limits by endpoint tier (standard/premium)

2. **Circuit Breaker Per Service**:
   - Individual circuit breakers for each backend
   - Custom fallback responses per service
   - Health indicator integration

3. **Request Routing**:
   - Path-based routing to backend services
   - Load balancing across service instances
   - Retry with exponential backoff

4. **Filter Chain Order**:
   ```
   Request → RateLimitFilter → Auth → CircuitBreaker → Retry → Backend
   ```

## Alternatives Considered

### Alternative 1: Kong API Gateway
**Description**: Open-source API gateway with plugin architecture
**Pros**:
- Feature-rich out of the box
- Lua plugin ecosystem
- Built-in rate limiting, auth

**Cons**:
- Separate infrastructure to manage
- Learning curve for Lua plugins
- License costs for enterprise features

**Why Not Chosen**: Custom Spring Boot gateway provides more control and integrates with existing codebase

### Alternative 2: AWS API Gateway
**Description**: Fully managed AWS service
**Pros**:
- No infrastructure management
- Built-in throttling and caching
- Integration with AWS services

**Cons**:
- AWS lock-in
- Cost per request at scale
- Limited customization

**Why Not Chosen**: Need cloud-agnostic solution with custom business logic

### Alternative 3: Envoy Proxy
**Description**: High-performance edge proxy
**Pros**:
- Extremely performant
- Service mesh integration
- Advanced traffic management

**Cons**:
- Complex configuration (xDS)
- Requires sidecar pattern
- No native Spring integration

**Why Not Chosen**: Complexity overkill; Spring Boot gateway sufficient for current scale

## Consequences

### Positive Consequences
- **Protection**: Backend services protected from overload
- **Fairness**: Multi-tenant rate limiting ensures fair usage
- **Resilience**: Circuit breakers prevent cascade failures
- **Observability**: Centralized metrics and logging
- **Control**: Single point for security policies

### Negative Consequences
- **Single Point of Failure**: Gateway must be highly available
- **Latency**: Additional hop adds ~5-10ms
- **Complexity**: Must tune rate limits and circuit breakers
- **State**: Rate limiting requires shared state (Redis)

### Mitigation
- Deploy gateway with horizontal scaling (3+ instances)
- Redis cluster for rate limit state
- Circuit breaker metrics exposed via Actuator
- Load testing to tune configurations

## Configuration

### Rate Limiting (RateLimitFilter.java)
```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final Map<String, Bucket> userBuckets = new ConcurrentHashMap<>();

    private Bucket createBucket(RateLimitTier tier) {
        return Bucket.builder()
            .addLimit(Bandwidth.classic(tier.getRequestsPerSecond(),
                Refill.greedy(tier.getRequestsPerSecond(), Duration.ofSeconds(1))))
            .addLimit(Bandwidth.classic(tier.getBurstCapacity(),
                Refill.intervally(tier.getBurstCapacity(), Duration.ofMinutes(1))))
            .build();
    }
}
```

### Circuit Breaker Configuration
```yaml
resilience4j:
  circuitbreaker:
    instances:
      cqlEngine:
        registerHealthIndicator: true
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 3
        slidingWindowType: COUNT_BASED
        minimumNumberOfCalls: 5
      qualityMeasure:
        # ... similar config
      fhirService:
        # ... similar config
      patientService:
        # ... similar config
      careGapService:
        # ... similar config
```

### Service Routing
```java
@Service
public class ServiceRoutingService {

    @CircuitBreaker(name = "cqlEngine", fallbackMethod = "cqlEngineFallback")
    @Retry(name = "cqlEngine")
    @TimeLimiter(name = "cqlEngine")
    public CompletableFuture<ResponseEntity<String>> routeToCqlEngine(
            String path, HttpMethod method, String body, HttpHeaders headers) {
        return CompletableFuture.supplyAsync(() ->
            executeRequest(cqlEngineUrl + path, method, body, headers));
    }

    public CompletableFuture<ResponseEntity<String>> cqlEngineFallback(
            String path, HttpMethod method, String body,
            HttpHeaders headers, Throwable t) {
        return CompletableFuture.completedFuture(
            ResponseEntity.status(503)
                .body("{\"error\": \"CQL Engine temporarily unavailable\"}"));
    }
}
```

### Rate Limit Tiers
| Tier | Requests/Second | Burst | Use Case |
|------|-----------------|-------|----------|
| Standard | 100 | 150 | Regular API users |
| Premium | 500 | 750 | Enterprise tenants |
| Internal | 1000 | 1500 | Service-to-service |
| Anonymous | 10 | 20 | Unauthenticated |

## Implementation Plan

1. **Phase 1 (Completed)**: RateLimitFilter implementation
2. **Phase 2 (Completed)**: ServiceRoutingService with circuit breakers
3. **Phase 3 (Completed)**: Resilience4j configuration for all services
4. **Phase 4**: Redis-backed distributed rate limiting
5. **Phase 5**: Grafana dashboards for gateway metrics

## Files Created/Modified

**New Files**:
- `backend/modules/services/gateway-service/src/main/java/com/healthdata/gateway/filter/RateLimitFilter.java`
- `backend/modules/services/gateway-service/src/main/java/com/healthdata/gateway/service/ServiceRoutingService.java`

**Modified Files**:
- `backend/modules/services/gateway-service/build.gradle.kts` - Added Resilience4j, Bucket4j
- `backend/modules/services/gateway-service/src/main/resources/application.yml` - Full resilience config

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Gateway p99 latency | <50ms overhead | APM metrics |
| Rate limit rejections | <1% legitimate | Prometheus counter |
| Circuit breaker opens | <5/day | Health indicators |
| Gateway availability | 99.99% | Uptime monitoring |

## Related Decisions

- **Depends on**: [ADR-0003](0003-adopt-circuit-breaker-pattern.md) - Circuit breaker pattern
- **Related to**: [ADR-0002](0002-implement-tenant-isolation-security.md) - Tenant-aware rate limiting

## References

- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Bucket4j Rate Limiting](https://github.com/bucket4j/bucket4j)
- [API Gateway Pattern (Microsoft)](https://docs.microsoft.com/en-us/azure/architecture/microservices/design/gateway)
- [Rate Limiting Best Practices](https://cloud.google.com/architecture/rate-limiting-strategies-techniques)
