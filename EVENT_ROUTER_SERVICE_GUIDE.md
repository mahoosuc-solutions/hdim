# Event Router Service - Complete Implementation Guide

## Overview

The Event Router Service is a critical component of the HealthData in Motion platform that routes FHIR events from source topics to appropriate target topics based on configurable rules. It provides priority-based routing, event filtering, transformation, and comprehensive monitoring.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Event Router Service                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐        │
│  │   Kafka      │──>│   Event      │──>│  Priority    │        │
│  │  Consumers   │   │   Router     │   │   Queue      │        │
│  └──────────────┘   └──────────────┘   └──────────────┘        │
│                           │                     │                │
│                           v                     v                │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐        │
│  │  Filtering   │   │Transformation│   │Load Balancing│        │
│  │   Service    │   │   Service    │   │   Service    │        │
│  └──────────────┘   └──────────────┘   └──────────────┘        │
│                           │                     │                │
│                           v                     v                │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐        │
│  │   Kafka      │   │     DLQ      │   │   Metrics    │        │
│  │  Producers   │   │   Service    │   │   Service    │        │
│  └──────────────┘   └──────────────┘   └──────────────┘        │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

## Features

### 1. **Event Routing by Type**
- Routes events from source topics to target topics based on event type
- Supports multiple routing rules per source topic
- Multi-tenant isolation

### 2. **Priority Queue**
- Four priority levels: CRITICAL > HIGH > MEDIUM > LOW
- Events processed in priority order
- FIFO within same priority level
- Configurable queue size limits

### 3. **Event Filtering**
- JSON-based filter expressions
- Supports operators: `$gte`, `$gt`, `$lte`, `$lt`, `$ne`, `$in`, `$exists`, `$regex`
- Nested field matching
- Complex boolean logic

### 4. **Event Transformation**
- Field enrichment (add timestamps, metadata)
- Field renaming and removal
- Data type conversion
- Field masking for sensitive data
- Object flattening
- Custom JavaScript transformations

### 5. **Dead Letter Queue (DLQ)**
- Failed events sent to DLQ topic
- Persistence to database
- Retry mechanism with configurable limits
- Failure reason tracking

### 6. **Load Balancing**
- Round-robin distribution
- Tenant-based partitioning
- Key-based partitioning
- Least-loaded consumer selection
- Consumer health tracking

### 7. **Metrics & Monitoring**
- Prometheus metrics export
- Real-time event counts by topic and priority
- Routing latency tracking
- Error rate calculation
- Throughput monitoring

### 8. **Health Checks**
- Liveness probe
- Readiness probe
- Queue health monitoring
- Detailed health status

## Database Schema

### event_routing_rules
```sql
CREATE TABLE event_routing_rules (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    description TEXT,
    source_topic VARCHAR(255) NOT NULL,
    target_topic VARCHAR(255) NOT NULL,
    filter_expression TEXT,
    priority VARCHAR(50) DEFAULT 'MEDIUM' NOT NULL,
    transformation_script TEXT,
    enabled BOOLEAN DEFAULT true NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_tenant_source_enabled ON event_routing_rules(tenant_id, source_topic, enabled);
```

### dead_letter_events
```sql
CREATE TABLE dead_letter_events (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    source_topic VARCHAR(255),
    original_payload TEXT,
    failure_reason TEXT,
    retry_count INTEGER DEFAULT 0,
    failed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_retry_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_tenant_failed_at ON dead_letter_events(tenant_id, failed_at);
```

## Configuration

### application.yml
```yaml
event-router:
  source-topics:
    - fhir.patient.created
    - fhir.patient.updated
    - fhir.observation.created

  dlq-topic: event-router.dlq

  priority-queue:
    max-size: 50000
    batch-size: 100
    poll-interval-ms: 100

  routing:
    default-priority: MEDIUM
    enable-async: true
    thread-pool-size: 10
```

## Routing Rules Examples

### Example 1: Simple Routing
```sql
INSERT INTO event_routing_rules (tenant_id, rule_name, source_topic, target_topic, priority, enabled)
VALUES ('tenant1', 'Patient to Processing', 'fhir.patient.created', 'patient.processing', 'HIGH', true);
```

### Example 2: Filtered Routing
```sql
INSERT INTO event_routing_rules (
    tenant_id, rule_name, source_topic, target_topic,
    filter_expression, priority, enabled
)
VALUES (
    'tenant1',
    'Urgent Patients Only',
    'fhir.patient.created',
    'urgent.processing',
    '{"urgent": true, "priority": {"$in": ["critical", "high"]}}',
    'CRITICAL',
    true
);
```

### Example 3: With Transformation
```sql
INSERT INTO event_routing_rules (
    tenant_id, rule_name, source_topic, target_topic,
    transformation_script, priority, enabled
)
VALUES (
    'tenant1',
    'Enrich and Route',
    'fhir.patient.created',
    'enriched.processing',
    'enrichment:add-timestamp,add-source|mask:ssn,email',
    'MEDIUM',
    true
);
```

### Example 4: Complex Filter with Nested Fields
```sql
INSERT INTO event_routing_rules (
    tenant_id, rule_name, source_topic, target_topic,
    filter_expression, priority, enabled
)
VALUES (
    'tenant1',
    'High-Risk Patients',
    'fhir.patient.updated',
    'risk-assessment.queue',
    '{"patient.riskScore": {"$gte": 80}, "patient.address.state": {"$in": ["MA", "NY", "CA"]}}',
    'HIGH',
    true
);
```

## Filter Expression Syntax

### Equality
```json
{"status": "active"}
```

### Comparison Operators
```json
{
  "age": {"$gte": 18},
  "score": {"$gt": 75}
}
```

### IN Operator
```json
{
  "status": {"$in": ["active", "pending", "completed"]}
}
```

### Exists Operator
```json
{
  "email": {"$exists": true}
}
```

### Regex Pattern
```json
{
  "email": {"$regex": ".*@healthdata\\.com$"}
}
```

### Complex Nested
```json
{
  "patient.demographics.age": {"$gte": 65},
  "patient.conditions": {"$exists": true},
  "priority": {"$in": ["high", "critical"]}
}
```

## Transformation Script Syntax

### Enrichment
```
enrichment:add-timestamp,add-source
```

### Rename Fields
```
rename:old_field->new_field,another_old->another_new
```

### Remove Fields
```
remove:temp_field,internal_id
```

### Type Conversion
```
convert:age->integer,score->double,active->boolean
```

### Masking
```
mask:ssn,email,phone
```

### Flatten Objects
```
flatten:patient
```

### JavaScript Transformation
```
js:payload.fullName = payload.firstName + ' ' + payload.lastName
```

### Chained Transformations
```
enrichment:add-timestamp|rename:old->new|mask:ssn|remove:temp
```

## API Endpoints

### Health Check
```bash
GET /actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "eventRouter": {
      "status": "UP",
      "details": {
        "queueSize": 42,
        "queueHealthy": true,
        "totalRoutedEvents": 1000,
        "eventsPerSecond": 15.5,
        "errorRate": 0.01
      }
    }
  }
}
```

### Metrics
```bash
GET /actuator/metrics/event.router.routed
```

### Prometheus Metrics
```bash
GET /actuator/prometheus
```

## Metrics

### Available Metrics

1. **event.router.routed** - Count of successfully routed events
   - Tags: topic, priority

2. **event.router.filtered** - Count of filtered events
   - Tags: topic

3. **event.router.unrouted** - Count of unrouted events
   - Tags: topic

4. **event.router.dlq** - Count of DLQ events
   - Tags: topic

5. **event.router.latency** - Routing latency histogram
   - Tags: topic

## Integration Guide

### 1. Add Routing Rule

```java
RoutingRuleEntity rule = new RoutingRuleEntity();
rule.setTenantId("tenant1");
rule.setRuleName("My Routing Rule");
rule.setSourceTopic("fhir.patient.created");
rule.setTargetTopic("patient.processing");
rule.setPriority(Priority.HIGH);
rule.setEnabled(true);
ruleRepository.save(rule);
```

### 2. Send Event to Source Topic

```java
EventMessage event = new EventMessage();
event.setEventType("PATIENT_CREATED");
event.setTenantId("tenant1");
event.setSourceTopic("fhir.patient.created");
event.setPayload(Map.of("patientId", "123", "name", "John Doe"));

kafkaTemplate.send("fhir.patient.created", objectMapper.writeValueAsString(event));
```

### 3. Consume from Target Topic

```java
@KafkaListener(topics = "patient.processing", groupId = "processing-group")
public void consumeProcessingEvent(String message) {
    EventMessage event = objectMapper.readValue(message, EventMessage.class);
    // Process event
}
```

### 4. Monitor DLQ

```java
@KafkaListener(topics = "event-router.dlq", groupId = "dlq-monitor")
public void monitorDlq(String message) {
    DeadLetterEvent dlqEvent = objectMapper.readValue(message, DeadLetterEvent.class);
    log.error("Event failed: {}", dlqEvent.getFailureReason());
}
```

## Testing

### Run All Tests
```bash
cd backend/modules/services/event-router-service
./gradlew test
```

### Test Coverage
- EventRouterTest: 8 tests
- PriorityQueueServiceTest: 8 tests
- EventFilterServiceTest: 12 tests
- EventTransformationServiceTest: 11 tests
- DeadLetterQueueServiceTest: 8 tests
- LoadBalancingServiceTest: 8 tests
- RouteMetricsServiceTest: 11 tests
- MultiTenantRoutingIntegrationTest: 8 tests
- EventRouterHealthCheckTest: 9 tests

**Total: 83+ tests**

## Deployment

### Docker Build
```bash
cd backend/modules/services/event-router-service
./gradlew build
docker build -t event-router-service:1.0.0 .
```

### Docker Run
```bash
docker run -d \
  -p 8087:8087 \
  -e DB_PASSWORD=secret \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  event-router-service:1.0.0
```

### Docker Compose
```yaml
event-router-service:
  image: event-router-service:1.0.0
  ports:
    - "8087:8087"
  environment:
    - SPRING_PROFILES_ACTIVE=docker
    - DB_PASSWORD=${DB_PASSWORD}
    - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
  depends_on:
    - postgres
    - kafka
```

## Performance Tuning

### Kafka Consumer Configuration
```yaml
spring.kafka.consumer:
  max-poll-records: 500  # Increase for higher throughput
  fetch-min-size: 1024   # Minimum bytes per fetch
  fetch-max-wait: 500    # Max wait time for fetch
```

### Thread Pool Configuration
```yaml
event-router.routing:
  thread-pool-size: 20   # Increase for parallel processing
  enable-async: true
```

### Queue Configuration
```yaml
event-router.priority-queue:
  max-size: 100000       # Increase for higher capacity
  batch-size: 500        # Larger batches
```

## Monitoring & Alerting

### Recommended Alerts

1. **High Queue Size**
   ```promql
   event_router_queue_size > 40000
   ```

2. **High Error Rate**
   ```promql
   rate(event_router_dlq_total[5m]) > 10
   ```

3. **High Latency**
   ```promql
   histogram_quantile(0.95, rate(event_router_latency_bucket[5m])) > 1000
   ```

4. **Service Down**
   ```promql
   up{job="event-router-service"} == 0
   ```

## Troubleshooting

### Issue: Events not routing
**Solution:** Check routing rules are enabled and match source topic

### Issue: High DLQ count
**Solution:** Review filter expressions and transformation scripts

### Issue: High latency
**Solution:** Increase thread pool size, check Kafka consumer lag

### Issue: Queue overflow
**Solution:** Increase max-size, add more consumer instances

## Best Practices

1. **Rule Design**
   - Keep filter expressions simple
   - Test transformations thoroughly
   - Use appropriate priority levels
   - Enable rules only when ready

2. **Performance**
   - Monitor queue size regularly
   - Scale horizontally for high throughput
   - Use batch processing where possible
   - Optimize filter expressions

3. **Reliability**
   - Always configure DLQ monitoring
   - Set appropriate retry limits
   - Log routing failures
   - Monitor metrics continuously

4. **Security**
   - Mask sensitive fields in transformations
   - Validate filter expressions
   - Use tenant isolation
   - Encrypt data at rest

## Support

For issues or questions:
- Review logs: `/actuator/logfile`
- Check metrics: `/actuator/metrics`
- Health status: `/actuator/health`
- GitHub Issues: [Project Issues](https://github.com/healthdata/issues)

---

**Version:** 1.0.0
**Last Updated:** 2025-11-25
**Status:** ✅ Production Ready
