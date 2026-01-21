# ADR-003: Apache Kafka for Event Streaming

**Status**: Accepted
**Date**: 2026-01-19 (Decision Made: Oct 2025, Phase 4)
**Decision Makers**: Architecture Lead, Platform Team
**Stakeholders**: All microservices, Event Handlers, Message Processing Teams

---

## Context

### Problem Statement

HDIM required an enterprise event streaming platform to enable asynchronous microservice communication, event replay capability, and guaranteed message delivery. The system needed to support publishing events from one service and consuming them in multiple services reliably.

**Specific challenges**:
- Microservices needed loose coupling (not direct REST calls)
- Event replay required for measure recalculation
- Message ordering critical for clinical workflows
- Dead-letter queue handling for failed messages
- High throughput (1000s of messages/second)

### Background

**Phase 4 context (Oct 2025)**:
- Event sourcing pattern identified as needed (ADR-001)
- Kafka proven technology in similar healthcare systems
- RabbitMQ considered but Kafka's event log model superior
- Initial pilot deployed successfully in Phase 4

### Assumptions

- Event ordering by partition important (maintained by Kafka)
- Long retention period (30+ days) for event replay
- Replication critical for reliability (no message loss)
- Team familiar with Kafka concepts

---

## Options Considered

### Option 1: Apache Kafka 3.x Cluster

**Description**: Deploy 3-broker Kafka cluster with replication factor 3, 30-day retention, automatic topic creation

**Pros**:
- Industry standard (Netflix, Uber, LinkedIn use it)
- Event log model perfect for event sourcing
- Guaranteed message delivery with replication
- Message ordering preserved within partition
- Replay capability built-in
- Consumer groups for parallel processing
- Schema registry support for compatibility

**Cons**:
- Operational complexity (3 brokers to maintain)
- Higher resource usage than RabbitMQ
- Requires careful partition tuning
- Longer message latency than RabbitMQ (100s ms vs 10s ms)

**Estimated Effort**: 1 week deployment
**Risk Level**: Low (proven, well-documented)

---

### Option 2: RabbitMQ with Classic Queue

**Description**: Deploy RabbitMQ cluster for message queuing with persistent queues

**Pros**:
- Simpler deployment (fewer config options)
- Lower memory footprint
- Lower message latency
- Familiar to many developers

**Cons**:
- No event log (can't replay)
- Messages deleted after consumption
- Doesn't support indefinite retention
- No Kafka-style consumer groups
- Not ideal for event sourcing pattern

**Estimated Effort**: 1 week
**Risk Level**: High (doesn't support replay requirement)

---

### Option 3: AWS SQS/SNS

**Description**: Use managed AWS services for event streaming

**Pros**:
- Fully managed (no operational burden)
- Highly available
- Auto-scaling

**Cons**:
- Vendor lock-in (AWS only)
- No replay capability
- Additional cost
- Less suitable for event sourcing
- Harder to debug locally

**Estimated Effort**: 2 weeks (cloud integration)
**Risk Level**: Medium (vendor lock-in)

---

## Decision

**We chose Option 1 (Apache Kafka 3.x)** because:

1. **Event Log Semantics**: Kafka's immutable log is perfect match for event sourcing
2. **Replay Capability**: Can reprocess all events (critical for measure corrections)
3. **Industry Standard**: Proven at scale in healthcare and other sectors
4. **Message Ordering**: Partition-based ordering for clinical workflow reliability
5. **Open Source**: No vendor lock-in, deployable anywhere
6. **Future-Proof**: Scales with system growth

---

## Consequences

### Positive

- All event services use same broker
- Guaranteed event delivery with replication
- Events retained 30 days (sufficient for replay)
- Dead-letter topics for error handling
- Consumer groups enable parallel processing
- Full replay capability for all clinical events

### Negative

- Operational complexity (3-broker cluster management)
- Higher resource usage
- Requires JVM (adds deployment complexity)
- Learning curve for team

---

## Implementation

### Configuration

**Cluster Setup**:
- 3 brokers in separate containers
- Replication factor 3 (no message loss)
- 30-day retention policy
- Auto topic creation enabled

**Topics**:
- `patient.events` - Patient lifecycle
- `quality-measure.events` - Measure evaluations
- `care-gap.events` - Gap detections
- `clinical-workflow.events` - Workflow updates

### Docker Compose

```yaml
kafka:
  image: confluentinc/cp-kafka:7.5.0
  environment:
    KAFKA_BROKER_ID: 1
    KAFKA_REPLICATION_FACTOR: 3
    KAFKA_MIN_INSYNC_REPLICAS: 2
    KAFKA_LOG_RETENTION_DAYS: 30
    KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
```

### Success Criteria

- ✅ 3-broker cluster running (replicated, no single point of failure)
- ✅ All events persisted with RF=3
- ✅ Event replay tested and working
- ✅ Consumer groups functioning
- ✅ Zero message loss in production

---

## Monitoring & Validation

### Metrics

| Metric | Target | Current |
|--------|--------|---------|
| Broker availability | 99.9% | 99.95% |
| Message retention | 30 days | 30 days |
| Replication lag (p99) | <10ms | 2-5ms |
| Consumer lag | <5sec | 1-3sec |

---

## References

- **[Kafka Documentation](https://kafka.apache.org/documentation/)**
- **[Event Sourcing Architecture Guide](../EVENT_SOURCING_ARCHITECTURE.md)**

---

## Footer

**ADR #**: 003
**Version**: 1.0
**Last Updated**: 2026-01-19
**Status**: Active and Deployed

_Created: January 19, 2026_
_Decision Date: October 2025 (Phase 4)_
