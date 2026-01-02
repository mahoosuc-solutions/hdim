# ADR-0006: Apache Kafka for Event Streaming

**Date**: 2024-Q3
**Status**: Accepted
**Deciders**: Architecture Team, Platform Engineering Team
**Technical Story**: Need reliable event streaming for asynchronous inter-service communication

---

## Context and Problem Statement

HDIM's 28 microservices require asynchronous communication for:

- Event-driven updates (patient data changes, measure evaluations)
- Decoupled service interactions (quality measure → care gap detection)
- Audit logging (HIPAA-compliant event trail)
- Real-time notifications (care gap alerts to providers)
- Data synchronization across services

The messaging solution must support:
- High throughput (10,000+ events/second)
- Durability (no message loss)
- Multi-tenant isolation
- Exactly-once or at-least-once delivery semantics
- Integration with Spring Boot ecosystem

---

## Decision Drivers

* **Reliability** - Messages must not be lost (HIPAA audit requirements)
* **Throughput** - Must handle peak loads during measure evaluation batches
* **Durability** - Event history retention for replay and debugging
* **Spring Boot integration** - Native support via Spring Kafka
* **Operational maturity** - Production-proven at scale
* **Multi-tenant support** - Topic-level or partition-level tenant isolation
* **Healthcare industry adoption** - Familiarity among healthcare engineering teams

---

## Considered Options

1. **Apache Kafka 3.x** - Distributed event streaming platform
2. **RabbitMQ** - Traditional message broker
3. **Amazon SQS/SNS** - AWS managed messaging
4. **Apache Pulsar** - Cloud-native messaging system
5. **Redis Streams** - Lightweight streaming on Redis

---

## Decision Outcome

**Chosen option**: "Apache Kafka 3.x"

**Rationale**: Apache Kafka provides the best combination of:
- Durability with configurable retention (critical for HIPAA audit trails)
- High throughput for batch processing scenarios
- Native Spring Boot integration via Spring Kafka
- Strong ecosystem for monitoring (Kafka UI, Prometheus exporters)
- Proven in healthcare production environments
- Topic-based multi-tenant isolation

---

## Consequences

### Positive

* **Durability**: Messages persisted to disk with configurable retention (7 days default)
* **Replay capability**: Can reprocess events for debugging or data recovery
* **Decoupling**: Services communicate without direct dependencies
* **Scalability**: Horizontal scaling via partitions
* **Spring integration**: Spring Kafka provides seamless integration
* **Monitoring**: Rich metrics via JMX and Prometheus exporters

### Negative

* **Operational complexity**: Requires ZooKeeper (or KRaft in newer versions)
* **Resource usage**: Higher memory/disk requirements than RabbitMQ
* **Learning curve**: Team needs Kafka-specific knowledge
* **Ordering guarantees**: Per-partition ordering requires careful partition key design

**Mitigations**:
- Use managed Kafka (Confluent Cloud, Amazon MSK) in production
- Document partition key strategies in service READMEs
- Provide team training on Kafka patterns

### Neutral

* Requires Java 11+ (aligns with our Java 21 stack)
* Docker Compose setup straightforward for development

---

## Pros and Cons of Options

### Option 1: Apache Kafka 3.x

Distributed event streaming platform from Apache.

| Criterion | Assessment |
|-----------|------------|
| Durability | **Good** - Persistent storage with configurable retention |
| Throughput | **Good** - Millions of messages/second proven |
| Spring Integration | **Good** - Spring Kafka is mature and well-documented |
| Multi-tenancy | **Good** - Topic-level isolation, ACLs available |
| Operational Complexity | **Neutral** - Requires cluster management |
| Healthcare Adoption | **Good** - Widely used in healthcare (Epic, Cerner integrations) |

**Summary**: Industry standard for event streaming with excellent durability and throughput.

---

### Option 2: RabbitMQ

Traditional AMQP message broker.

| Criterion | Assessment |
|-----------|------------|
| Durability | **Neutral** - Persistent queues available but less robust than Kafka |
| Throughput | **Neutral** - Good for moderate loads, not designed for massive scale |
| Spring Integration | **Good** - Spring AMQP is mature |
| Multi-tenancy | **Neutral** - Virtual hosts provide isolation |
| Operational Complexity | **Good** - Simpler than Kafka |
| Message Replay | **Bad** - No built-in replay capability |

**Summary**: Simpler but lacks durability and replay capabilities critical for healthcare.

---

### Option 3: Amazon SQS/SNS

AWS managed messaging services.

| Criterion | Assessment |
|-----------|------------|
| Durability | **Good** - AWS managed, highly durable |
| Throughput | **Good** - Scales automatically |
| Spring Integration | **Neutral** - Requires AWS SDK |
| Multi-tenancy | **Good** - Queue-level isolation |
| Vendor Lock-in | **Bad** - AWS-specific |
| Message Replay | **Bad** - No built-in replay (requires separate S3 archival) |

**Summary**: Good managed option but creates AWS dependency and lacks replay.

---

### Option 4: Apache Pulsar

Cloud-native messaging and streaming platform.

| Criterion | Assessment |
|-----------|------------|
| Durability | **Good** - BookKeeper provides strong durability |
| Throughput | **Good** - Designed for high throughput |
| Spring Integration | **Neutral** - Less mature than Spring Kafka |
| Multi-tenancy | **Good** - Native multi-tenancy support |
| Operational Complexity | **Bad** - More complex than Kafka |
| Healthcare Adoption | **Neutral** - Less common than Kafka in healthcare |

**Summary**: Technically capable but smaller ecosystem and less healthcare adoption.

---

### Option 5: Redis Streams

Lightweight streaming built into Redis.

| Criterion | Assessment |
|-----------|------------|
| Durability | **Neutral** - Depends on Redis persistence configuration |
| Throughput | **Good** - Redis is fast |
| Spring Integration | **Good** - Spring Data Redis supports Streams |
| Multi-tenancy | **Neutral** - Key-based isolation |
| Operational Complexity | **Good** - Already running Redis for caching |
| Feature Set | **Bad** - Limited compared to Kafka (no compaction, limited retention) |

**Summary**: Simple but lacks enterprise features needed for healthcare messaging.

---

## Implementation Notes

### Version Selected

**Apache Kafka 3.6.x** - Latest stable release

### Deployment Model

- **Development**: Single broker in Docker Compose (port 9094)
- **Production**: 3+ broker cluster with replication factor 3

### Topic Naming Convention

```
{domain}.{event-type}

Examples:
- patient.created
- patient.updated
- measure.evaluation.completed
- care-gap.detected
- audit.phi-access
```

### Partition Strategy

| Topic Pattern | Partition Key | Rationale |
|---------------|---------------|-----------|
| patient.* | patientId | Ensures patient event ordering |
| measure.* | tenantId + measureId | Tenant isolation, measure grouping |
| care-gap.* | tenantId + patientId | Tenant isolation, patient ordering |
| audit.* | tenantId | Tenant isolation |

### Configuration

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9094}
    producer:
      acks: all  # Durability: wait for all replicas
      retries: 3
      properties:
        enable.idempotence: true  # Exactly-once semantics
    consumer:
      auto-offset-reset: earliest
      enable-auto-commit: false  # Manual commit for reliability
      properties:
        isolation.level: read_committed

# Topic retention
kafka:
  topics:
    patient-events:
      retention-ms: 604800000  # 7 days
    audit-events:
      retention-ms: 2592000000  # 30 days (HIPAA requirement)
```

### Key Topics

| Topic | Purpose | Producers | Consumers |
|-------|---------|-----------|-----------|
| patient.events | Patient lifecycle events | Patient Service | Care Gap, Quality Measure, Analytics |
| measure.evaluation.completed | Measure results | Quality Measure | Care Gap, Analytics, Notification |
| care-gap.detected | New care gaps | Care Gap Service | Notification, Analytics |
| audit.phi-access | PHI access events | All services | Audit Service |
| notification.requests | Notification triggers | Various | Notification Service |

### Performance Targets

| Metric | Target | Actual (Dec 2024) |
|--------|--------|-------------------|
| Producer Throughput | 5,000 msg/sec | 6,200 msg/sec |
| Consumer Latency (p95) | <100ms | 75ms |
| End-to-End Latency | <500ms | 350ms |
| Message Loss Rate | 0% | 0% |

---

## Links

* [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
* [Spring Kafka Reference](https://docs.spring.io/spring-kafka/reference/)
* [Kafka Service Configuration](/backend/modules/shared/infrastructure/messaging/)
* Related: [ADR-0007 - PostgreSQL Database](ADR-0007-postgresql-database.md)
* Related: [ADR-0008 - Redis Caching](ADR-0008-redis-caching-strategy.md)

---

## Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2024-Q3 | Architecture Team | Initial decision |
| 1.1 | 2024-12-30 | Architecture Team | Added performance actuals, topic details |

---

*This ADR follows the template in `/docs/templates/ADR_TEMPLATE.md`*
