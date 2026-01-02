# CQL Engine Service - Real-Time Visualization System

## Overview

The CQL Engine Service now includes comprehensive real-time visualization infrastructure for monitoring batch evaluation progress, individual evaluations, and system health.

## Architecture

```
┌─────────────────────┐
│ MeasureTemplate     │
│ Engine              │
│ - Batch Evaluation  │
│ - Progress Tracking │
└──────┬──────────────┘
       │ Publishes Events
       ▼
┌─────────────────────┐
│ Kafka Event         │
│ Producer            │
│ - JSON Serialization│
│ - Async Publishing  │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│ Kafka Topics        │
│ - batch.progress    │ ◄─── PRIMARY VISUALIZATION EVENT
│ - evaluation.started│
│ - evaluation.       │
│   completed         │
│ - evaluation.failed │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│ Kafka Event         │
│ Consumer            │
│ - Topic Listeners   │
│ - Event Routing     │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│ WebSocket Handler   │
│ - Session Mgmt      │
│ - Tenant Filtering  │
│ - JSON Broadcasting │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│ Frontend Clients    │
│ - Real-time Updates │
│ - Progress Bars     │
│ - Live Metrics      │
└─────────────────────┘
```

## Features Implemented

### 1. Event Domain Models
**Location:** `com.healthdata.cql.event`

- **EvaluationEvent** - Base interface with EventType enum
- **EvaluationStartedEvent** - Published when evaluation begins
- **EvaluationCompletedEvent** - Published on success with full results
- **EvaluationFailedEvent** - Published on errors with categorization
- **BatchProgressEvent** - Real-time batch progress (PRIMARY EVENT)

### 2. Kafka Event Producer
**Location:** `com.healthdata.cql.event.EvaluationEventProducer`

- Async event publishing with CompletableFuture
- Tenant-based partitioning for evaluations
- Batch-based partitioning for progress events
- JSON serialization with Spring Kafka
- Idempotent producer with exactly-once semantics

**Configuration:** `com.healthdata.cql.config.KafkaProducerConfig`

### 3. Instrumented Evaluation Engine
**Location:** `com.healthdata.cql.engine.MeasureTemplateEngine`

- Event publishing at all workflow stages
- Batch progress tracking with atomic counters
- Scheduled progress reporting (every 5 seconds)
- Event-driven progress (every 10 patients)
- Real-time metrics:
  - Average duration per evaluation
  - Current throughput (evals/second)
  - Estimated time remaining (ETA)
  - Cumulative compliance rate

### 4. WebSocket Infrastructure
**Components:**

- **WebSocketConfig** - CORS-enabled WebSocket endpoint
- **EvaluationProgressWebSocketHandler** - Session management with tenant filtering
- **Endpoint:** `ws://localhost:8082/ws/evaluation-progress`

**Features:**
- Multi-tenant session management
- Query parameter tenant filtering: `?tenantId=YOUR_TENANT_ID`
- Automatic session cleanup
- Welcome messages on connection

### 5. Kafka-to-WebSocket Bridge
**Location:** `com.healthdata.cql.consumer.EvaluationEventConsumer`

- Listens to all event topics
- Extracts tenant information
- Forwards events to WebSocket clients
- 3 concurrent consumer threads

**Configuration:** `com.healthdata.cql.config.KafkaConsumerConfig`

### 6. Visualization REST API
**Location:** `com.healthdata.cql.controller.VisualizationController`

**Endpoints:**
- `GET /api/visualization/connections` - WebSocket connection stats
- `GET /api/visualization/connections/{tenantId}` - Tenant-specific stats
- `GET /api/visualization/config` - Visualization configuration
- `GET /api/visualization/health` - System health check
- `GET /api/visualization/sample-message` - Example WebSocket message

## Configuration

### application-local.yml

```yaml
# Kafka Configuration
spring.kafka:
  bootstrap-servers: localhost:9092
  producer:
    key-serializer: org.apache.kafka.common.serialization.StringSerializer
    value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    properties:
      spring.json.add.type.headers: false

# Visualization Configuration
visualization:
  websocket:
    enabled: true
    allowed-origins:
      - http://localhost:4200  # Angular dev server
      - http://localhost:3000  # React dev server
      - http://localhost:8082  # Same origin
  kafka:
    topics:
      evaluation-started: "evaluation.started"
      evaluation-completed: "evaluation.completed"
      evaluation-failed: "evaluation.failed"
      batch-progress: "batch.progress"
  batch-progress:
    emit-interval-seconds: 5    # Emit progress every 5 seconds
    emit-every-n-patients: 10   # Or every 10 patients (whichever first)
```

## Usage

### Frontend WebSocket Connection

```javascript
// Connect to WebSocket (with tenant filtering)
const ws = new WebSocket('ws://localhost:8082/ws/evaluation-progress?tenantId=TENANT001');

// Handle connection established
ws.onopen = () => {
  console.log('Connected to evaluation progress stream');
};

// Handle incoming messages
ws.onmessage = (event) => {
  const message = JSON.parse(event.data);

  if (message.type === 'EVALUATION_EVENT') {
    const eventData = message.data;

    // Handle batch progress events
    if (eventData.eventType === 'BATCH_PROGRESS') {
      updateProgressBar(
        eventData.completedCount / eventData.totalPatients * 100
      );
      updateMetrics({
        throughput: eventData.currentThroughput,
        eta: eventData.estimatedTimeRemainingMs,
        complianceRate: eventData.cumulativeComplianceRate
      });
    }
  }
};

// Handle errors
ws.onerror = (error) => {
  console.error('WebSocket error:', error);
};

// Handle disconnection
ws.onclose = () => {
  console.log('Disconnected from evaluation progress stream');
};
```

### REST API Examples

```bash
# Get WebSocket connection stats
curl http://localhost:8082/api/visualization/connections

# Get tenant-specific connection count
curl http://localhost:8082/api/visualization/connections/TENANT001

# Get visualization configuration
curl http://localhost:8082/api/visualization/config

# Health check
curl http://localhost:8082/api/visualization/health

# Get sample WebSocket message format
curl http://localhost:8082/api/visualization/sample-message
```

## Event Message Formats

### Batch Progress Event (Primary)
```json
{
  "type": "EVALUATION_EVENT",
  "timestamp": 1699564800000,
  "data": {
    "eventType": "BATCH_PROGRESS",
    "batchId": "550e8400-e29b-41d4-a716-446655440000",
    "tenantId": "TENANT001",
    "measureId": "HEDIS-CDC",
    "measureName": "Comprehensive Diabetes Care",
    "totalPatients": 1000,
    "completedCount": 450,
    "successCount": 445,
    "failedCount": 5,
    "pendingCount": 550,
    "avgDurationMs": 125.5,
    "currentThroughput": 3.6,
    "elapsedTimeMs": 125000,
    "estimatedTimeRemainingMs": 152778,
    "denominatorCount": 380,
    "numeratorCount": 285,
    "cumulativeComplianceRate": 75.0
  }
}
```

### Evaluation Completed Event
```json
{
  "type": "EVALUATION_EVENT",
  "timestamp": 1699564800000,
  "data": {
    "eventType": "EVALUATION_COMPLETED",
    "evaluationId": "123e4567-e89b-12d3-a456-426614174000",
    "tenantId": "TENANT001",
    "measureId": "HEDIS-CDC",
    "patientId": "patient-123",
    "inDenominator": true,
    "inNumerator": true,
    "complianceRate": 1.0,
    "score": 100.0,
    "durationMs": 125,
    "evidence": { ... },
    "careGapCount": 0
  }
}
```

## Performance Characteristics

### Batch Progress Emission
- **Time-based:** Every 5 seconds (configurable)
- **Event-based:** Every 10 patients (configurable)
- **Strategy:** Whichever comes first

### Kafka Producer
- **Compression:** Snappy
- **Batch Size:** 16KB
- **Linger:** 10ms
- **Idempotence:** Enabled
- **Acks:** 1 (leader acknowledgment)

### Kafka Consumer
- **Group ID:** `cql-engine-visualization-group`
- **Auto-commit:** Enabled (1 second interval)
- **Concurrency:** 3 threads
- **Offset:** Latest (real-time only)

### WebSocket
- **Protocol:** WebSocket (RFC 6455)
- **Message Format:** JSON
- **Session Management:** ConcurrentHashMap
- **CORS:** Configurable origins

## Testing the Visualization System

### 1. Start Required Services

```bash
# Start PostgreSQL and Redis
cd docker
docker-compose up -d

# Start Kafka (if not in docker-compose)
# See infrastructure/kafka/docker-compose.yml
```

### 2. Start CQL Engine Service

```bash
cd backend
./gradlew :modules:services:cql-engine-service:bootRun --args='--spring.profiles.active=local'
```

### 3. Test WebSocket Connection

Open browser console and run:
```javascript
const ws = new WebSocket('ws://localhost:8082/ws/evaluation-progress?tenantId=TENANT001');
ws.onmessage = (e) => console.log(JSON.parse(e.data));
```

### 4. Trigger Batch Evaluation

```bash
# Use the evaluation API to start a batch evaluation
curl -X POST http://localhost:8082/api/evaluations/batch \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: TENANT001" \
  -d '{
    "measureId": "HEDIS-CDC",
    "patientIds": ["patient-1", "patient-2", ..., "patient-100"]
  }'
```

### 5. Monitor Progress

Watch the browser console for real-time progress events:
- Initial welcome message
- Batch progress events every 5 seconds or 10 patients
- Individual evaluation completion events
- Final batch completion

## Next Steps (Future Enhancements)

### Phase 2: Frontend SPA (Weeks 2-4)
- React/Angular visualization dashboard
- Real-time progress bars with ETA
- Live evaluation feed
- Error monitoring panel
- Historical analytics charts

### Phase 3: Advanced Features (Weeks 5-7)
- Historical data persistence (PostgreSQL)
- Time-series analytics (InfluxDB)
- Performance dashboards
- Alerting and notifications
- Multi-tenant admin console

## Troubleshooting

### WebSocket Connection Fails
- Check CORS configuration in `application-local.yml`
- Verify WebSocket endpoint is accessible
- Check browser console for error messages

### No Events Received
- Verify Kafka is running and accessible
- Check Kafka topics exist: `evaluation.started`, `batch.progress`, etc.
- Verify consumer group is active
- Check logs for Kafka connection errors

### High Memory Usage
- Adjust Kafka consumer concurrency
- Tune batch size and linger settings
- Implement WebSocket rate limiting

## Files Created/Modified

### New Files
1. `com.healthdata.cql.event.EvaluationEvent` - Base event interface
2. `com.healthdata.cql.event.EvaluationStartedEvent`
3. `com.healthdata.cql.event.EvaluationCompletedEvent`
4. `com.healthdata.cql.event.EvaluationFailedEvent`
5. `com.healthdata.cql.event.BatchProgressEvent` - PRIMARY EVENT
6. `com.healthdata.cql.event.EvaluationEventProducer` - Kafka producer
7. `com.healthdata.cql.config.KafkaProducerConfig`
8. `com.healthdata.cql.config.KafkaConsumerConfig`
9. `com.healthdata.cql.config.WebSocketConfig`
10. `com.healthdata.cql.websocket.EvaluationProgressWebSocketHandler`
11. `com.healthdata.cql.consumer.EvaluationEventConsumer`
12. `com.healthdata.cql.controller.VisualizationController`

### Modified Files
1. `build.gradle.kts` - Added WebSocket dependency
2. `MeasureTemplateEngine.java` - Instrumented with event publishing
3. `application-local.yml` - Added visualization config

## API Documentation

Full API documentation is available at:
- **Swagger UI:** http://localhost:8082/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8082/v3/api-docs

## Support

For issues or questions, check:
- Application logs: `logs/cql-engine-service.log`
- Kafka consumer lag: `kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group cql-engine-visualization-group`
- WebSocket connections: `GET /api/visualization/connections`
