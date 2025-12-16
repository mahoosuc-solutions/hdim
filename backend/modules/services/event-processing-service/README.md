# Event Processing Service

Kafka-based event-driven processing with dead letter queue management and real-time event monitoring.

## Purpose

Processes healthcare events asynchronously via Kafka messaging, addressing the challenge that:
- Clinical events (CQL evaluations, care gaps, data changes) need decoupled processing
- Failed events require retry logic with exponential backoff
- Dead letter queue (DLQ) needs monitoring and manual intervention capabilities
- Event processing metrics must be exposed via Prometheus

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                Event Processing Service                          │
│                         (Port 8083)                              │
├─────────────────────────────────────────────────────────────────┤
│  Controller Layer                                                │
│  └── DeadLetterQueueController (DLQ management API)             │
├─────────────────────────────────────────────────────────────────┤
│  Service Layer                                                   │
│  ├── DeadLetterQueueService                                     │
│  │   ├── Failed event tracking    - Store failed messages       │
│  │   ├── Retry management         - Mark for retry, exhaust     │
│  │   ├── Resolution tracking      - Mark as resolved            │
│  │   └── Statistics               - DLQ metrics, recent failures│
│  └── EventProcessorService (Kafka consumers)                    │
│      ├── CQL evaluation events                                  │
│      ├── Care gap events                                        │
│      └── FHIR resource events                                   │
├─────────────────────────────────────────────────────────────────┤
│  Repository Layer                                                │
│  └── DeadLetterQueueRepository                                  │
├─────────────────────────────────────────────────────────────────┤
│  Domain Entities                                                 │
│  └── DeadLetterQueueEntity                                      │
│      ├── Topic              - Kafka topic name                  │
│      ├── Payload            - Original message JSON             │
│      ├── Error Message      - Failure reason                    │
│      ├── Retry Count        - Number of retry attempts          │
│      └── Status             - FAILED, RETRY, EXHAUSTED, RESOLVED│
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Kafka Consumer/Producer
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Apache Kafka (Port 9092)                    │
│  Topics: evaluation.completed, care-gap.identified, etc.        │
└─────────────────────────────────────────────────────────────────┘
```

## API Endpoints

### Dead Letter Queue Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/dead-letter-queue/failed` | Get failed events (paginated) |
| GET | `/api/v1/dead-letter-queue/patient/{patientId}` | Failed events by patient |
| GET | `/api/v1/dead-letter-queue/topic/{topic}` | Failed events by topic |
| GET | `/api/v1/dead-letter-queue/exhausted` | Events needing intervention |
| GET | `/api/v1/dead-letter-queue/recent?hours=24` | Recent failures |
| GET | `/api/v1/dead-letter-queue/stats` | DLQ statistics |

### Event Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/dead-letter-queue/{dlqId}/retry` | Retry failed event |
| POST | `/api/v1/dead-letter-queue/{dlqId}/resolve` | Mark as resolved |
| POST | `/api/v1/dead-letter-queue/{dlqId}/exhaust` | Mark as exhausted |

## Kafka Topics

| Topic | Purpose | Producer | Consumer |
|-------|---------|----------|----------|
| `evaluation.started` | CQL evaluation started | cql-engine | event-processing |
| `evaluation.completed` | CQL evaluation done | cql-engine | event-processing, care-gap |
| `evaluation.failed` | CQL evaluation error | cql-engine | event-processing |
| `care-gap.identified` | Care gap found | care-gap | event-processing, notification |
| `patient.updated` | Patient data changed | fhir | event-processing, cache-invalidation |

## Configuration

```yaml
server:
  port: 8083
  servlet:
    context-path: /events

# Kafka configuration
spring.kafka:
  bootstrap-servers: localhost:9092
  consumer:
    group-id: event-processing-service
    auto-offset-reset: earliest
  producer:
    acks: all
    retries: 3

# Actuator (Prometheus metrics)
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

## Dead Letter Queue Entity

```json
{
  "id": "uuid",
  "tenantId": "tenant-1",
  "patientId": "patient-123",
  "topic": "evaluation.completed",
  "payload": "{\"evaluationId\":\"eval-123\"}",
  "errorMessage": "Connection timeout to FHIR service",
  "retryCount": 3,
  "status": "EXHAUSTED",
  "firstFailedAt": "2024-01-01T10:00:00Z",
  "lastRetryAt": "2024-01-01T10:15:00Z",
  "resolvedAt": null,
  "resolvedBy": null
}
```

## Dependencies

- **Spring Boot**: Web, JPA, Kafka
- **Database**: PostgreSQL with Liquibase migrations
- **Messaging**: Kafka for event streaming
- **Metrics**: Micrometer + Prometheus for monitoring
- **Resilience**: Resilience4j for circuit breakers

## Running Locally

```bash
# Start Kafka first
docker compose up -d kafka

# From backend directory
./gradlew :modules:services:event-processing-service:bootRun

# Or via Docker
docker compose --profile events up event-processing-service
```

## Testing

```bash
# Unit tests
./gradlew :modules:services:event-processing-service:test

# Get failed events
curl http://localhost:8083/events/api/v1/dead-letter-queue/failed \
  -H "X-Tenant-ID: tenant-1"

# Get DLQ statistics
curl http://localhost:8083/events/api/v1/dead-letter-queue/stats \
  -H "X-Tenant-ID: tenant-1"

# Retry failed event
curl -X POST http://localhost:8083/events/api/v1/dead-letter-queue/{dlqId}/retry

# Mark as resolved
curl -X POST http://localhost:8083/events/api/v1/dead-letter-queue/{dlqId}/resolve \
  -H "Content-Type: application/json" \
  -d '{"resolvedBy":"admin","notes":"Manually fixed"}'
```

## Monitoring

- **Prometheus metrics**: `/actuator/prometheus`
- **Health checks**: `/actuator/health`
- **DLQ dashboard**: Monitor exhausted events needing intervention
- **Event processing duration**: SLA tracking (10ms, 50ms, 100ms, 500ms, 1s)
