# WebSocket Health Scores - Quick Start Guide

## 🚀 Quick Connection

### JavaScript/TypeScript (Browser)

```javascript
const socket = new WebSocket('ws://localhost:8087/quality-measure/ws/health-scores?tenantId=TENANT001');

socket.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log('Received:', message.type, message.data);
};
```

### Angular Service (Production-Ready)

```typescript
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class HealthScoreWebSocketService {
  private socket: WebSocket | null = null;
  private updates$ = new BehaviorSubject<any>(null);

  connect(tenantId: string): void {
    this.socket = new WebSocket(
      `ws://localhost:8087/quality-measure/ws/health-scores?tenantId=${tenantId}`
    );

    this.socket.onmessage = (event) => {
      const msg = JSON.parse(event.data);
      if (msg.type === 'HEALTH_SCORE_UPDATE' || msg.type === 'SIGNIFICANT_CHANGE') {
        this.updates$.next(msg.data);
      }
    };
  }

  disconnect(): void {
    this.socket?.close();
  }

  getUpdates(): Observable<any> {
    return this.updates$.asObservable();
  }
}
```

### React Hook

```typescript
import { useEffect, useState } from 'react';

export function useHealthScoreWebSocket(tenantId: string) {
  const [update, setUpdate] = useState(null);

  useEffect(() => {
    const ws = new WebSocket(
      `ws://localhost:8087/quality-measure/ws/health-scores?tenantId=${tenantId}`
    );

    ws.onmessage = (e) => {
      const msg = JSON.parse(e.data);
      if (msg.type === 'HEALTH_SCORE_UPDATE') setUpdate(msg.data);
    };

    return () => ws.close();
  }, [tenantId]);

  return update;
}
```

## 📨 Message Types

### Connection Established
```json
{
  "type": "CONNECTION_ESTABLISHED",
  "sessionId": "abc123",
  "tenantId": "TENANT001"
}
```

### Health Score Update
```json
{
  "type": "HEALTH_SCORE_UPDATE",
  "data": {
    "patientId": "patient-123",
    "overallScore": 75.5,
    "scoreDelta": 3.5
  }
}
```

### Significant Change Alert
```json
{
  "type": "SIGNIFICANT_CHANGE",
  "data": {
    "patientId": "patient-123",
    "overallScore": 45.0,
    "scoreDelta": -30.0,
    "changeReason": "Large decline in mental health score"
  },
  "priority": "high"
}
```

## 🧪 Testing

### Command Line (wscat)

```bash
# Install
npm install -g wscat

# Connect
wscat -c "ws://localhost:8087/quality-measure/ws/health-scores?tenantId=TENANT001"

# Trigger update (in another terminal)
curl -X POST http://localhost:8087/quality-measure/api/v1/health-scores \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: TENANT001" \
  -d '{
    "patientId": "patient-123",
    "physicalHealthScore": 75.0,
    "mentalHealthScore": 65.0,
    "socialDeterminantsScore": 80.0,
    "preventiveCareScore": 85.0,
    "chronicDiseaseScore": 70.0
  }'
```

### Browser Console

```javascript
const ws = new WebSocket('ws://localhost:8087/quality-measure/ws/health-scores?tenantId=TENANT001');
ws.onmessage = (e) => console.log(JSON.parse(e.data));
```

## ⚙️ Configuration

### application.yml

```yaml
websocket:
  enabled: true
  allowed-origins: http://localhost:4200,http://localhost:3000
```

### CORS

Add your frontend URL to `allowed-origins` to enable connections.

## 🔧 Troubleshooting

### Connection Refused
- Check service is running on port 8087
- Verify `websocket.enabled: true` in config

### No Messages Received
- Verify `tenantId` matches
- Check server logs for broadcast errors
- Ensure health score calculations are triggered

### CORS Error
- Add your origin to `websocket.allowed-origins`
- Example: `http://localhost:4200`

## 📚 Full Documentation

See [WEBSOCKET_HEALTH_SCORES.md](./WEBSOCKET_HEALTH_SCORES.md) for:
- Complete API reference
- Security considerations
- Production deployment guide
- Advanced examples
- Monitoring and scaling

## 🎯 Key Features

✅ Real-time health score updates
✅ Significant change alerts (±10 points)
✅ Multi-tenant isolation
✅ Auto-reconnect support
✅ Error handling
✅ Production-ready

## 🏗️ Architecture

```
Health Score Service
  ↓
  Calculates Score
  ↓
  ├─→ Saves to Database
  ├─→ Publishes Kafka Event
  └─→ Broadcasts WebSocket ← Your Frontend Connects Here
       ↓
       Filtered by Tenant
       ↓
       Connected Clients Receive Update
```

## 🔐 Security

- Tenant-based filtering (data isolation)
- CORS protection
- HTTP session authentication
- Automatic session cleanup

## 📊 Production Metrics

Monitor:
- Active connections per tenant
- Broadcast rate
- Connection errors
- Message delivery latency

---

**Need Help?** See full documentation in `WEBSOCKET_HEALTH_SCORES.md`
