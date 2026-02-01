# ADR-008: OpenTelemetry for Distributed Tracing

**Status**: Accepted
**Date**: 2026-01-19 (Decision Made: Phase 5, Jan 2026)
**Decision Makers**: Architecture Lead, Platform Team
**Stakeholders**: All Microservices, DevOps, Observability Team

---

## Context

### Problem Statement

HDIM had 51 microservices communicating via HTTP, Kafka, and direct calls. Tracing a single user request across all services was impossible without manual log aggregation. Needed automatic trace propagation and visualization.

---

## Options Considered

### Option 1: OpenTelemetry (Industry Standard)

**Description**: Implement OpenTelemetry with Jaeger visualization for distributed tracing

**Pros**:
- Automatic trace propagation (no code changes)
- Works with HTTP (Feign), Kafka, gRPC
- Vendor-neutral (can switch backends)
- Jaeger UI for visualization
- Environment-specific sampling (100% dev, 50% staging, 10% prod)

**Cons**:
- Additional operational overhead
- Jaeger deployment required
- Learning curve for teams

**Risk Level**: Low (proven, open standard)

---

### Option 2: Splunk/DataDog (Managed)

**Description**: Use managed observability platform

**Pros**:
- Fully managed
- Advanced analytics

**Cons**:
- Vendor lock-in
- Cost per transaction
- Less control

**Risk Level**: Medium (cost, vendor lock-in)

---

## Decision

**We chose Option 1 (OpenTelemetry + Jaeger)** because:

1. **Automatic Propagation**: Trace ID flows through all services without code changes
2. **Open Standard**: Not locked to any vendor
3. **Cost-Effective**: Open-source Jaeger
4. **Complete Visibility**: See full request path across 51 services
5. **Sampling Control**: Per-environment sampling for cost efficiency

---

## Implementation

### Configuration

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% dev, 0.5 staging, 0.1 prod
```

### Sampling Rates

| Environment | Rate | Reason |
|-------------|------|--------|
| Development | 100% | Full visibility for debugging |
| Staging | 50% | Balance visibility + cost |
| Production | 10% | Cost-efficient monitoring |

### Kafka Integration

```yaml
spring:
  kafka:
    producer:
      properties:
        interceptor.classes: com.healthdata.tracing.KafkaProducerTraceInterceptor
    consumer:
      properties:
        interceptor.classes: com.healthdata.tracing.KafkaConsumerTraceInterceptor
```

### Jaeger UI

Access at: `http://localhost:16686`

---

## Success Criteria

- ✅ Trace ID flows across all 51 services
- ✅ Jaeger UI shows complete request paths
- ✅ Sampling rates configured per environment
- ✅ <1ms overhead for tracing
- ✅ 100% of services instrumented

---

## References

- **[Distributed Tracing Guide](../../backend/docs/DISTRIBUTED_TRACING_GUIDE.md)**
- **[OpenTelemetry Documentation](https://opentelemetry.io/)**

---

## Footer

**ADR #**: 008
**Version**: 1.0
**Status**: Active and Deployed
**Last Updated**: 2026-01-19

_Decision Date: Phase 5 (January 2026)_
_Visualization: Jaeger UI (http://localhost:16686)_
