# ADR-0003: Adopt Circuit Breaker Pattern with Resilience4j

**Status**: Accepted
**Date**: 2025-12-06
**Decision Makers**: Architecture Team
**Consulted**: Platform Engineering, SRE
**Informed**: All Development Teams

## Context

**Problem Statement**:
The HDIM microservices architecture lacked resilience patterns, creating cascading failure risks:

1. No circuit breakers for inter-service calls
2. No retry policies with exponential backoff
3. No rate limiting to protect services from overload
4. Single service failure could cascade to entire platform

**Business Context**:
- Healthcare platform requires 99.9% availability SLA
- HEDIS measure calculations are time-sensitive (quarterly deadlines)
- Service outages impact patient care gap notifications
- Downtime costs: ~$10K/hour in SLA penalties

**Technical Context**:
- 16+ Spring Boot microservices
- Synchronous REST calls between services
- External FHIR server dependencies
- CQL Engine with long-running evaluations

## Decision

**We will implement Resilience4j circuit breakers, retry policies, and rate limiters across all services that make external calls.**

**Specific Implementation**:

1. **Circuit Breaker Configuration**:
   - 50% failure rate threshold
   - 30-second wait in open state
   - 3 calls permitted in half-open state
   - Sliding window of 10 calls

2. **Retry Policy**:
   - 3 maximum attempts
   - 2-second base wait duration
   - 2x exponential backoff multiplier
   - Retry on: IOException, SocketTimeoutException

3. **Rate Limiter**:
   - 100 requests per second default
   - 5-second timeout for rate limit acquisition

## Alternatives Considered

### Alternative 1: Hystrix (Netflix)
**Description**: Netflix's original circuit breaker library
**Pros**:
- Battle-tested in production
- Large community
- Dashboard available

**Cons**:
- In maintenance mode (no new features)
- Heavier dependency footprint
- Thread pool isolation adds complexity

**Why Not Chosen**: Hystrix is deprecated; Resilience4j is recommended successor

### Alternative 2: Spring Cloud Circuit Breaker
**Description**: Spring's abstraction over circuit breaker implementations
**Pros**:
- Spring ecosystem integration
- Abstraction allows switching implementations
- Familiar Spring programming model

**Cons**:
- Additional abstraction layer
- Less fine-grained control
- Still needs underlying implementation

**Why Not Chosen**: Direct Resilience4j provides more control and simpler debugging

### Alternative 3: Service Mesh (Istio/Linkerd)
**Description**: Infrastructure-level resilience
**Pros**:
- Language-agnostic
- No code changes required
- Centralized policy management

**Cons**:
- Significant infrastructure overhead
- Kubernetes required
- Complex operations

**Why Not Chosen**: Too heavy for current deployment model; can be added later

## Consequences

### Positive Consequences
- **Fault Tolerance**: Service failures don't cascade
- **Graceful Degradation**: Fallback responses during outages
- **Self-Healing**: Automatic recovery when services restore
- **Visibility**: Health indicators expose circuit state

### Negative Consequences
- **Latency**: Circuit breaker adds small overhead (~1ms)
- **Complexity**: Need to tune thresholds per service
- **Testing**: Requires failure injection testing

### Mitigation
- Default configurations work for most services
- Actuator endpoints expose circuit breaker metrics
- Integration tests include failure scenarios

## Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      fhirService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 3
  retry:
    instances:
      fhirService:
        maxAttempts: 3
        waitDuration: 2s
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
```

## Implementation Plan

1. **Phase 1 (Completed)**: Add Resilience4j dependencies to core services
2. **Phase 2 (Completed)**: Configure circuit breakers in application.yml
3. **Phase 3**: Add Grafana dashboards for circuit breaker metrics
4. **Phase 4**: Chaos engineering tests (failure injection)

## Files Modified

**Dependencies Added**:
- `backend/modules/services/cql-engine-service/build.gradle.kts`
- `backend/modules/services/fhir-service/build.gradle.kts`
- `backend/modules/services/care-gap-service/build.gradle.kts`
- `backend/modules/services/patient-service/build.gradle.kts`
- `backend/modules/services/gateway-service/build.gradle.kts`

**Configuration Added**:
- `backend/modules/services/cql-engine-service/src/main/resources/application.yml`
- `backend/modules/services/gateway-service/src/main/resources/application.yml`

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Circuit open incidents | <5/day | Prometheus counter |
| Mean time to recovery | <60s | Circuit open duration |
| Cascading failures | 0 | Incident reports |
| Retry success rate | >90% | Retry metrics |

## Related Decisions

- **Related to**: [ADR-0006](0006-api-gateway-resilience.md) - Gateway resilience patterns

## References

- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Circuit Breaker Pattern (Martin Fowler)](https://martinfowler.com/bliki/CircuitBreaker.html)
- [Release It! (Michael Nygard)](https://pragprog.com/titles/mnee2/release-it-second-edition/)
