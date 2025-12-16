# Event Router Service

Multi-tenant event routing service with priority-based processing, filtering, and transformation capabilities.

## Purpose

The Event Router Service provides intelligent routing of healthcare events across the HDIM platform. It consumes events from multiple source topics, applies tenant-specific filtering and transformation rules, and routes events to appropriate destination topics based on configurable routing rules.

## Key Features

- **Multi-tenant Event Routing**: Tenant-isolated routing rules and event processing
- **Priority Queue**: Priority-based event processing with configurable batch sizes
- **Event Filtering**: JSONPath-based filtering expressions for conditional routing
- **Event Transformation**: Script-based event transformation before routing
- **Load Balancing**: Distributed event processing with load balancing support
- **Dead Letter Queue**: Automatic DLQ handling for failed events
- **Metrics & Monitoring**: Real-time routing metrics, error rates, and throughput tracking
- **Circuit Breaker**: Resilience4j circuit breaker for fault tolerance
- **Kafka Integration**: High-throughput Kafka-based event streaming

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Health check with queue status and routing metrics |
| GET | `/actuator/metrics` | Service metrics and performance indicators |
| GET | `/actuator/prometheus` | Prometheus-compatible metrics export |

## Configuration

### Application Properties

```yaml
server:
  port: 8095

event-router:
  source-topics:
    - fhir.patient.created
    - fhir.patient.updated
    - fhir.observation.created
    - fhir.encounter.created
    - fhir.medication.created
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

### Database

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/healthdata_event_router
    username: healthdata
    password: ${SPRING_DATASOURCE_PASSWORD}
```

### Kafka

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: event-router-consumer-group
    producer:
      acks: all
      retries: 3
```

## Core Components

### EventRouter
Main routing service that matches events to routing rules, applies filters and transformations, and publishes to target topics.

### PriorityQueueService
Manages priority-based event queue with high/medium/low priority levels.

### EventFilterService
Evaluates filter expressions against event payloads using JSONPath.

### EventTransformationService
Applies script-based transformations to events before routing.

### RouteMetricsService
Tracks routing metrics including events routed, filtered, unrouted, latency, and error rates.

## Running Locally

### Start the Service

```bash
./gradlew :modules:services:event-router-service:bootRun
```

### Running Tests

```bash
./gradlew :modules:services:event-router-service:test
```

### Building

```bash
./gradlew :modules:services:event-router-service:build
```

## Testing

The service includes comprehensive integration tests covering:
- Multi-tenant routing isolation
- Priority-based processing
- Event filtering and transformation
- Load balancing across consumers
- Circuit breaker behavior
- Metrics collection

## Health Monitoring

The health indicator exposes:
- Queue size and health status
- Total routed events
- Total filtered events
- Total unrouted events
- Events per second
- Error rate percentage

## Authors

HDIM Development Team

## License

Proprietary - Mahoosuc Solutions
