# WebSocket-Driven Analytics Implementation Guide

## Overview

This implementation provides a complete WebSocket-driven real-time analytics and event monitoring system for the CQL Engine Service. The system streams evaluation progress, batch updates, and performance metrics to frontend clients in real-time.

## Architecture

```
┌─────────────────┐         ┌──────────────────┐         ┌────────────────┐
│                 │         │                  │         │                │
│  CQL Engine     │─Kafka──▶│  WebSocket       │◀────WS──│  Frontend      │
│  Service        │         │  Handler         │         │  Dashboard     │
│                 │         │                  │         │                │
└─────────────────┘         └──────────────────┘         └────────────────┘
```

### Backend Components

1. **WebSocketConfig** (`backend/modules/services/cql-engine-service/src/main/java/com/healthdata/cql/config/WebSocketConfig.java`)
   - Configures WebSocket endpoint: `/ws/evaluation-progress`
   - CORS configuration for frontend origins
   - Enabled/disabled via `visualization.websocket.enabled` property

2. **EvaluationProgressWebSocketHandler** (`backend/.../websocket/EvaluationProgressWebSocketHandler.java`)
   - Manages WebSocket sessions
   - Multi-tenant filtering via query parameter
   - Broadcasts evaluation events to connected clients
   - Thread-safe session tracking

3. **EvaluationEventConsumer** (`backend/.../consumer/EvaluationEventConsumer.java`)
   - Kafka consumer that bridges evaluation events to WebSocket
   - Listens to: `evaluation.started`, `evaluation.completed`, `evaluation.failed`, `batch.progress`
   - Enabled via `visualization.kafka.enabled` property

4. **Security Configuration** (`backend/.../config/CqlSecurityCustomizer.java`)
   - WebSocket endpoints (`/ws/**`) are permitted without JWT authentication
   - Authentication can be added via query params or custom handshake interceptor
   - CORS configured for frontend origins

### Frontend Components

1. **WebSocket Service** (`frontend/src/services/websocket.service.ts`)
   - Singleton WebSocket client with auto-reconnection
   - Exponential backoff (1s → 30s max)
   - Event-based message routing
   - Authentication token support via query params
   - Tenant filtering support

2. **useWebSocket Hook** (`frontend/src/hooks/useWebSocket.ts`)
   - React hook for WebSocket lifecycle management
   - Auto-connect/disconnect based on component lifecycle
   - Integrates with Zustand evaluation store

3. **RealTimeEventPanel** (`frontend/src/components/RealTimeEventPanel.tsx`)
   - Live event feed with filtering and search
   - Real-time statistics (events/sec, success rate)
   - Pause/resume, auto-scroll, export capabilities
   - Color-coded event severity

4. **WebSocketDashboard** (`frontend/src/components/WebSocketDashboard.tsx`)
   - Complete analytics dashboard
   - Overview metrics (active batches, success rate, compliance)
   - Tabbed interface: Events, Analytics, Performance
   - Connection status monitoring

5. **AnalyticsPanel** (`frontend/src/components/AnalyticsPanel.tsx`)
   - Statistical analysis of batch metrics
   - Mean, median, std dev, percentiles
   - Outlier detection
   - Multiple metric types (success rate, compliance, duration, throughput)

## Configuration

### Backend Configuration

#### application.yml (Local Development)
```yaml
visualization:
  websocket:
    enabled: true
    allowed-origins: "http://localhost:4200,http://localhost:3000,http://localhost:5173,http://localhost:8082"
  kafka:
    enabled: false  # Disabled for local without Kafka
    topics:
      evaluation-started: "evaluation.started"
      evaluation-completed: "evaluation.completed"
      evaluation-failed: "evaluation.failed"
      batch-progress: "batch.progress"
  batch-progress:
    emit-interval-seconds: 5
    emit-every-n-patients: 10
```

#### application-docker.yml (Docker/Production)
```yaml
visualization:
  websocket:
    enabled: true
    allowed-origins: "http://localhost:4200,http://localhost:3000,http://localhost:5173,http://localhost:8082,http://localhost:3002"
  kafka:
    enabled: ${VISUALIZATION_KAFKA_ENABLED:true}  # Enable Kafka consumer
    topics:
      evaluation-started: "evaluation.started"
      evaluation-completed: "evaluation.completed"
      evaluation-failed: "evaluation.failed"
      batch-progress: "batch.progress"
  batch-progress:
    emit-interval-seconds: 5
    emit-every-n-patients: 10
```

### Frontend Configuration

#### Environment Variables (.env)
```bash
# WebSocket connection URL
VITE_WS_BASE_URL=ws://localhost:8081/cql-engine

# For Docker deployment
# VITE_WS_BASE_URL=ws://cql-engine-service:8081/cql-engine
```

## Usage

### Basic Integration

```tsx
import { WebSocketDashboard } from './components/WebSocketDashboard';
import { useWebSocket } from './hooks/useWebSocket';

function App() {
  // Initialize WebSocket connection
  useWebSocket({
    tenantId: 'TENANT001',
    autoConnect: true,
    baseUrl: import.meta.env.VITE_WS_BASE_URL || 'ws://localhost:8081/cql-engine',
  });

  return <WebSocketDashboard />;
}
```

### With Authentication

```tsx
import { useWebSocket } from './hooks/useWebSocket';

function App() {
  const authToken = localStorage.getItem('authToken');

  useWebSocket({
    tenantId: 'TENANT001',
    autoConnect: true,
    baseUrl: 'ws://localhost:8081/cql-engine',
    authToken: authToken || undefined,
  });

  return <WebSocketDashboard />;
}
```

### Using Individual Components

```tsx
import { RealTimeEventPanel } from './components/RealTimeEventPanel';
import { AnalyticsPanel } from './components/AnalyticsPanel';
import { useEvaluationStore, selectAllBatches } from './store/evaluationStore';

function CustomDashboard() {
  const allBatches = useEvaluationStore(selectAllBatches);

  return (
    <Grid container spacing={2}>
      <Grid item xs={12} md={6}>
        <RealTimeEventPanel maxEvents={100} autoScroll={true} />
      </Grid>
      <Grid item xs={12} md={6}>
        <AnalyticsPanel batches={allBatches} metric="successRate" />
      </Grid>
    </Grid>
  );
}
```

## WebSocket Message Format

### Client → Server (Connection)
```
ws://localhost:8081/cql-engine/ws/evaluation-progress?tenantId=TENANT001&token=<auth-token>
```

### Server → Client (Welcome)
```json
{
  "type": "CONNECTION_ESTABLISHED",
  "sessionId": "abc-123",
  "tenantId": "TENANT001",
  "message": "Connected to evaluation progress stream"
}
```

### Server → Client (Evaluation Event)
```json
{
  "type": "EVALUATION_EVENT",
  "data": {
    "type": "BATCH_PROGRESS",
    "batchId": "batch-123",
    "totalPatients": 100,
    "completedCount": 50,
    "successCount": 48,
    "failedCount": 2,
    "percentComplete": 50.0,
    "currentThroughput": 10.5,
    "avgDurationMs": 1250,
    "cumulativeComplianceRate": 85.5,
    "measureName": "CMS124",
    "timestamp": 1700000000000
  },
  "timestamp": 1700000000000
}
```

## Testing

### Manual Testing

1. **Start Backend**:
   ```bash
   cd backend
   ./gradlew :modules:services:cql-engine-service:bootRun --args='--spring.profiles.active=local'
   ```

2. **Start Frontend**:
   ```bash
   cd frontend
   npm run dev
   ```

3. **Trigger Evaluation**:
   ```bash
   curl -X POST http://localhost:8081/cql-engine/api/evaluations/batch \
     -H "Content-Type: application/json" \
     -d '{
       "measureId": "CMS124",
       "patientIds": ["patient-1", "patient-2", "patient-3"],
       "measurementPeriodStart": "2024-01-01",
       "measurementPeriodEnd": "2024-12-31"
     }'
   ```

4. **Observe WebSocket Events** in the frontend dashboard

### WebSocket Client Testing (Browser Console)

```javascript
// Connect to WebSocket
const ws = new WebSocket('ws://localhost:8081/cql-engine/ws/evaluation-progress?tenantId=TENANT001');

// Handle messages
ws.onmessage = (event) => {
  console.log('Received:', JSON.parse(event.data));
};

ws.onopen = () => {
  console.log('Connected');
};

ws.onerror = (error) => {
  console.error('Error:', error);
};
```

## Features

### Real-Time Event Panel
- ✅ Live event feed with auto-scroll
- ✅ Pause/resume event stream
- ✅ Event type filtering
- ✅ Search/filter capabilities
- ✅ Color-coded severity (success, error, info)
- ✅ Real-time statistics (events/sec, success rate)
- ✅ Export to JSON

### Analytics Panel
- ✅ Statistical analysis (mean, median, std dev)
- ✅ Percentile analysis (Q1, Q3, IQR)
- ✅ Outlier detection
- ✅ Multiple metrics (success rate, compliance, duration, throughput)
- ✅ Visual data representation

### WebSocket Service
- ✅ Auto-reconnection with exponential backoff
- ✅ Connection status monitoring
- ✅ Authentication token support
- ✅ Multi-tenant filtering
- ✅ Event-based message routing
- ✅ Singleton pattern for efficient resource use

## Security Considerations

### Current Implementation
- WebSocket endpoints are **public** (no JWT required)
- Authentication can be added via query params
- CORS configured for known origins only
- Session-based tenant filtering

### Future Enhancements
- JWT authentication in WebSocket handshake
- Custom handshake interceptor for token validation
- Role-based access control for event streams
- Encrypted WebSocket connections (WSS)

## Deployment

### Local Development
```bash
# Backend
cd backend
./gradlew :modules:services:cql-engine-service:bootRun --args='--spring.profiles.active=local'

# Frontend
cd frontend
npm run dev
```

### Docker
```bash
# Set environment variables
export VISUALIZATION_KAFKA_ENABLED=true
export SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# Start services
docker-compose up -d cql-engine-service

# Frontend (update .env)
VITE_WS_BASE_URL=ws://localhost:8081/cql-engine
npm run dev
```

### Kubernetes
```yaml
# ConfigMap
apiVersion: v1
kind: ConfigMap
metadata:
  name: cql-engine-config
data:
  visualization.websocket.enabled: "true"
  visualization.kafka.enabled: "true"
  visualization.websocket.allowed-origins: "https://app.example.com,https://dashboard.example.com"

# Service
apiVersion: v1
kind: Service
metadata:
  name: cql-engine-service
spec:
  ports:
    - name: http
      port: 8081
      targetPort: 8081
```

## Troubleshooting

### WebSocket Connection Fails
1. Check backend is running: `curl http://localhost:8081/cql-engine/actuator/health`
2. Verify WebSocket enabled: Check `visualization.websocket.enabled=true` in config
3. Check CORS origins include your frontend URL
4. Inspect browser console for connection errors

### No Events Received
1. Verify Kafka consumer is enabled: `visualization.kafka.enabled=true`
2. Check Kafka topics exist and have messages
3. Verify evaluation service is publishing to Kafka topics
4. Check backend logs for WebSocket handler messages

### Authentication Issues
1. Verify token is being passed in query params
2. Check security configuration permits `/ws/**` endpoints
3. Implement custom handshake interceptor if JWT validation needed

## Performance Considerations

### Backend
- WebSocket sessions are stored in ConcurrentHashMap (thread-safe)
- Kafka consumer runs in separate thread pool
- Message broadcasting is non-blocking
- Consider scaling WebSocket handler horizontally with sticky sessions

### Frontend
- Virtual scrolling for large event lists (react-window)
- Event buffer limited to 100 most recent events
- Auto-scroll can be disabled for better performance
- Statistics calculated with memoization (useMemo)

## Monitoring

### Backend Metrics
- Active WebSocket connections: `/api/visualization/connections`
- Connection count by tenant: `/api/visualization/connections/{tenantId}`
- Kafka consumer lag: Check consumer group metrics

### Frontend Monitoring
- Connection status displayed in dashboard
- Event throughput (events/sec) calculated in real-time
- Success rate and compliance metrics tracked
- Error events highlighted in red

## Next Steps

1. **Add JWT Authentication**: Implement WebSocket handshake interceptor with JWT validation
2. **Add Message Filtering**: Allow clients to subscribe to specific measure IDs or patient IDs
3. **Add Historical Playback**: Stream historical events for debugging/analysis
4. **Add Rate Limiting**: Prevent overwhelming clients with too many events
5. **Add Compression**: Use WebSocket compression for large message payloads
6. **Add Binary Protocol**: Consider Protocol Buffers for more efficient serialization

## References

- [Spring WebSocket Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#websocket)
- [React WebSocket Integration](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)
- [Zustand State Management](https://github.com/pmndrs/zustand)
- [Material-UI Components](https://mui.com/material-ui/getting-started/)
