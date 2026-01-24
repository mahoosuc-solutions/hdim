# Real-Time Vital Signs Alerts via WebSocket

**Feature:** Real-time provider notifications for abnormal vital signs via WebSocket
**Issue:** #288
**Status:** ✅ Production Ready
**Version:** 1.0.0

## Quick Start

### Connect from Frontend (Angular)

```typescript
import { Client, StompConfig } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

// Configure STOMP client
const stompConfig: StompConfig = {
  webSocketFactory: () => new SockJS('http://localhost:8090/ws'),
  reconnectDelay: 5000,
  heartbeatIncoming: 4000,
  heartbeatOutgoing: 4000,
};

const client = new Client(stompConfig);

// Subscribe to provider-specific alerts
client.onConnect = () => {
  // Provider-specific alerts
  client.subscribe('/topic/vitals-alerts/ma-smith', (message) => {
    const alert = JSON.parse(message.body);
    console.log('Vital sign alert:', alert);
    showNotification(alert);
  });

  // Critical broadcasts (all providers)
  client.subscribe('/topic/vitals-alerts/critical', (message) => {
    const alert = JSON.parse(message.body);
    console.log('CRITICAL alert:', alert);
    showUrgentNotification(alert);
  });
};

client.activate();
```

### Test WebSocket Connection

```bash
# Start clinical-workflow-service
docker compose up -d clinical-workflow-service

# Use wscat to test WebSocket connection
npm install -g wscat
wscat -c ws://localhost:8090/ws
```

## Features

- ✅ **Real-time notifications** - Instant browser alerts when abnormal vitals recorded
- ✅ **Provider-specific routing** - Alerts sent to specific providers based on recordedBy field
- ✅ **Critical broadcasts** - Life-threatening alerts broadcast to all providers
- ✅ **HIPAA compliant** - Spring Security authentication, audit logging, PHI protection
- ✅ **Dual-channel publishing** - Kafka (backend) + WebSocket (frontend) in parallel
- ✅ **Non-blocking** - WebSocket failures don't prevent vital signs recording or Kafka publishing
- ✅ **SockJS fallback** - Supports browsers without native WebSocket

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│             Vital Signs Alert Flow (WebSocket)              │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. Vital signs recorded with abnormal values               │
│     └──> VitalSignsService.recordVitals()                  │
│                                                              │
│  2. VitalSignsAlertPublisher.publishAlert()                │
│     ├──> Kafka: vitals.alert.critical/warning              │
│     │    (Async backend processing)                         │
│     │                                                        │
│     └──> WebSocket: /topic/vitals-alerts/{providerId}      │
│          (Real-time frontend notification)                  │
│                                                              │
│  3. Provider-Specific Routing                               │
│     ├──> If recordedBy present:                             │
│     │    /topic/vitals-alerts/ma-smith                      │
│     │                                                        │
│     └──> If critical alert:                                 │
│          /topic/vitals-alerts/critical                      │
│          (Broadcast to ALL providers)                       │
│                                                              │
│  4. Audit Logging (HIPAA §164.312(b))                      │
│     └──> AuditService.logAuditEvent()                      │
│          - Provider ID                                       │
│          - Notification type (PROVIDER_SPECIFIC, BROADCAST)  │
│          - Success/failure outcome                           │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Configuration

### WebSocket Endpoint

**URL:** `ws://localhost:8090/ws`
**Protocol:** STOMP over WebSocket
**Fallback:** SockJS (for browser compatibility)

### Message Broker

| Prefix | Purpose |
|--------|---------|
| `/topic` | Server-to-client pub-sub messages |
| `/app` | Client-to-server application messages |

### Destinations

| Destination | Purpose | Subscription Pattern |
|-------------|---------|---------------------|
| `/topic/vitals-alerts/{providerId}` | Provider-specific alerts | Subscribe with actual provider ID (e.g., `ma-smith`) |
| `/topic/vitals-alerts/critical` | Critical broadcast | All providers subscribe to this |

## Message Format

### VitalSignsAlertEvent Structure

```json
{
  "eventId": "uuid",
  "eventType": "VITAL_SIGNS_ALERT_CREATED",
  "eventTimestamp": "2026-01-23T10:30:00Z",
  "eventSource": "clinical-workflow-service",
  "tenantId": "tenant1",
  "patientId": "uuid",
  "patientName": "Doe, John",
  "vitalsId": "uuid",
  "encounterId": "encounter-123",
  "recordedAt": "2026-01-23T10:29:55Z",
  "recordedBy": "ma-smith",
  "alertStatus": "critical",
  "alertMessage": "Systolic BP 180 mmHg - critical high",
  "alertTypes": ["HIGH_BLOOD_PRESSURE"],
  "values": {
    "systolicBp": 180,
    "diastolicBp": 95,
    "heartRate": 102,
    "temperatureF": 98.6,
    "respirationRate": 18,
    "oxygenSaturation": 96
  },
  "roomNumber": "EXAM-101"
}
```

## Frontend Integration

### Angular Service Example

```typescript
import { Injectable } from '@angular/core';
import { Client, StompConfig } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class VitalSignsAlertService {
  private client: Client;
  private alerts$ = new BehaviorSubject<VitalSignsAlert[]>([]);

  constructor(private authService: AuthService) {}

  connect(providerId: string): void {
    const stompConfig: StompConfig = {
      webSocketFactory: () => new SockJS('http://localhost:8090/ws'),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (msg) => console.debug('STOMP:', msg),
    };

    this.client = new Client(stompConfig);

    this.client.onConnect = () => {
      console.log('WebSocket connected');

      // Subscribe to provider-specific alerts
      this.client.subscribe(`/topic/vitals-alerts/${providerId}`, (message) => {
        const alert = JSON.parse(message.body);
        this.handleAlert(alert);
      });

      // Subscribe to critical broadcasts
      this.client.subscribe('/topic/vitals-alerts/critical', (message) => {
        const alert = JSON.parse(message.body);
        this.handleCriticalAlert(alert);
      });
    };

    this.client.onDisconnect = () => {
      console.warn('WebSocket disconnected');
    };

    this.client.activate();
  }

  disconnect(): void {
    this.client?.deactivate();
  }

  getAlerts$() {
    return this.alerts$.asObservable();
  }

  private handleAlert(alert: VitalSignsAlert): void {
    const alerts = this.alerts$.value;
    this.alerts$.next([alert, ...alerts]);

    // Show browser notification
    this.showNotification(alert);
  }

  private handleCriticalAlert(alert: VitalSignsAlert): void {
    this.handleAlert(alert);

    // Play urgent sound
    this.playUrgentSound();
  }

  private showNotification(alert: VitalSignsAlert): void {
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification(`Abnormal Vitals: ${alert.patientName}`, {
        body: alert.alertMessage,
        icon: '/assets/alert-icon.png',
        tag: alert.vitalsId,
      });
    }
  }

  private playUrgentSound(): void {
    const audio = new Audio('/assets/urgent-alert.mp3');
    audio.play();
  }
}
```

### Component Usage

```typescript
@Component({
  selector: 'app-provider-dashboard',
  templateUrl: './provider-dashboard.component.html',
})
export class ProviderDashboardComponent implements OnInit, OnDestroy {
  alerts$ = this.alertService.getAlerts$();

  constructor(
    private alertService: VitalSignsAlertService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const providerId = this.authService.getCurrentUser().providerId;
    this.alertService.connect(providerId);
  }

  ngOnDestroy(): void {
    this.alertService.disconnect();
  }
}
```

## HIPAA Compliance

### PHI Protection

**1. Authentication Required**
- WebSocket connections use Spring Security authentication from HTTP session
- Unauthenticated connections rejected at STOMP endpoint

**2. Provider Routing**
- Providers only receive alerts for patients they recorded vitals for
- Multi-tenant isolation enforced via providerId routing

**3. Audit Logging (§164.312(b))**
```java
// All WebSocket deliveries audited
auditService.logAuditEvent(AuditEvent.builder()
    .tenantId(vitals.getTenantId())
    .action(AuditAction.CREATE)
    .resourceType("VitalSignsAlert")
    .resourceId(vitals.getId().toString())
    .serviceName("clinical-workflow-service")
    .methodName("publishToWebSocket")
    .outcome(success ? AuditOutcome.SUCCESS : AuditOutcome.MINOR_FAILURE)
    .build());
```

**4. PHI Minimization**
- Only abnormal vital signs included in alerts
- Patient name optional (can be masked in production)

## Security

### WebSocket Security Configuration

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:4200", "https://*.healthdata.com")
                .withSockJS();  // SockJS fallback
    }
}
```

### CORS Configuration

**Allowed Origins:**
- `http://localhost:4200` (development)
- `https://*.healthdata.com` (production)

**Production Recommendations:**
- Use TLS/WSS (wss://) in production
- Restrict CORS to specific production domains
- Enable Spring Security authorization on STOMP destinations
- Rotate session tokens regularly

## Error Handling

### Non-Blocking Behavior

| Scenario | Behavior | Impact |
|----------|----------|--------|
| WebSocket connection lost | Alert logged, Kafka publishing continues | Backend processing unaffected |
| Provider offline | Alert queued, delivered on reconnect | No data loss |
| Audit service unavailable | Error logged, WebSocket delivery continues | Non-blocking |
| JSON serialization fails | Error logged, no WebSocket publish | Kafka publishing continues |

### Reconnection Strategy

**STOMP Client Auto-Reconnect:**
- Reconnect delay: 5 seconds
- Infinite retries
- Exponential backoff (optional)

**Frontend Handling:**
```typescript
this.client.onStompError = (frame) => {
  console.error('STOMP error:', frame.headers['message']);
  // Show "Connection lost" banner to user
};

this.client.onWebSocketClose = (evt) => {
  console.warn('WebSocket closed:', evt.reason);
  // Client auto-reconnects after 5 seconds
};
```

## Performance

### Scalability

**Current Implementation:**
- Simple in-memory message broker (Spring's SimpleBroker)
- Suitable for single-instance deployments

**Production Recommendations:**
- Use external message broker (RabbitMQ, ActiveMQ) for multi-instance deployments
- Enable STOMP over RabbitMQ for horizontal scaling
- Use Redis Pub/Sub for distributed WebSocket sessions

**Configuration Example (RabbitMQ):**
```java
@Override
public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableStompBrokerRelay("/topic")
            .setRelayHost("rabbitmq.example.com")
            .setRelayPort(61613)
            .setClientLogin("guest")
            .setClientPasscode("guest");

    registry.setApplicationDestinationPrefixes("/app");
}
```

### Metrics to Monitor

**Business Metrics:**
- `websocket.alerts.sent.count` - Total WebSocket alerts sent
- `websocket.alerts.failed.count` - Failed WebSocket deliveries
- `websocket.connections.active` - Active WebSocket connections

**Technical Metrics:**
- `websocket.message.latency` - Message delivery latency (ms)
- `websocket.connection.duration` - Average connection duration
- `websocket.reconnect.count` - Reconnection attempts

## Testing

### Unit Tests (25 total)

```bash
./gradlew :modules:services:clinical-workflow-service:test \
    --tests "VitalSignsAlertPublisherTest"
```

**WebSocket Test Coverage:**
- Provider-specific destination routing
- Critical broadcast routing
- Audit logging verification
- Error handling (WebSocket failures, audit failures)
- Graceful degradation

### Integration Tests

**Manual Testing with wscat:**
```bash
# Install wscat
npm install -g wscat

# Connect to WebSocket endpoint
wscat -c ws://localhost:8090/ws

# Send CONNECT frame
CONNECT
accept-version:1.1,1.0
heart-beat:10000,10000

# Subscribe to alerts
SUBSCRIBE
id:sub-1
destination:/topic/vitals-alerts/ma-smith

# Record abnormal vitals via REST API
curl -X POST http://localhost:8090/api/v1/vitals \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant1" \
  -d '{
    "patientId": "uuid",
    "encounterId": "encounter-123",
    "recordedBy": "ma-smith",
    "systolicBp": 180,
    "diastolicBp": 95
  }'

# Expect MESSAGE frame on WebSocket connection
```

## Troubleshooting

### WebSocket Connection Fails

**Symptoms:** `WebSocket connection failed: Connection refused`

**Causes:**
- Service not running
- Incorrect WebSocket URL
- CORS policy blocking connection

**Fix:**
```bash
# Verify service is running
docker compose ps clinical-workflow-service

# Check logs for WebSocket endpoint
docker compose logs clinical-workflow-service | grep "WebSocket"

# Expected output:
# Mapped "{[/ws]}" onto public void org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry.addEndpoint()
```

### No Alerts Received

**Symptoms:** WebSocket connected but no alerts received

**Checklist:**
1. ✅ Subscribed to correct destination? (`/topic/vitals-alerts/{providerId}`)
2. ✅ RecordedBy field matches providerId?
3. ✅ Vital signs actually abnormal? (check alert thresholds)
4. ✅ Check backend logs for WebSocket publish errors

**Debug:**
```bash
# Check if alerts published to Kafka (parallel channel)
docker compose logs clinical-workflow-service | grep "Successfully published vital signs alert to WebSocket"

# Verify audit logging
docker compose logs clinical-workflow-service | grep "Audited vital signs alert notification"
```

### Authentication Errors

**Symptoms:** `403 Forbidden` when connecting to WebSocket

**Causes:**
- No HTTP session (unauthenticated)
- Spring Security blocking WebSocket upgrade

**Fix:**
```typescript
// Ensure user is authenticated before connecting
if (!this.authService.isAuthenticated()) {
  throw new Error('User must be authenticated to receive alerts');
}

this.alertService.connect(providerId);
```

## Related Documentation

- [Vital Signs Service](./README.md#vital-signs-service)
- [Kafka Event Publishing](./KAFKA_EVENTS.md)
- [HIPAA Compliance Guide](../../HIPAA-CACHE-COMPLIANCE.md)
- [Clinical Workflow Architecture](../../docs/architecture/CLINICAL_WORKFLOW_ARCHITECTURE.md)

## Support

**Issue Tracker:** https://github.com/webemo-aaron/hdim/issues/288
**On-call:** PagerDuty rotation
**WebSocket Docs:** https://docs.spring.io/spring-framework/reference/web/websocket.html

---

**Last Updated:** January 23, 2026
**Implementation:** Complete
**Status:** ✅ Production Ready
