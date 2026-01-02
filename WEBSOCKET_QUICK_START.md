# WebSocket Analytics - Quick Start Guide

## 🚀 Quick Setup (5 minutes)

### Backend (CQL Engine Service)

1. **Configuration is already set** - No changes needed for local development
   - WebSocket enabled at `/ws/evaluation-progress`
   - Kafka disabled by default for local
   - Security permits WebSocket connections

2. **Start the service**:
   ```bash
   cd backend
   ./gradlew :modules:services:cql-engine-service:bootRun --args='--spring.profiles.active=local'
   ```

### Frontend

1. **Set environment variable** (optional, defaults to local):
   ```bash
   # Create/edit frontend/.env
   echo "VITE_WS_BASE_URL=ws://localhost:8081/cql-engine" > frontend/.env
   ```

2. **Add to your app** (already in App.tsx):
   ```tsx
   import { useWebSocket } from './hooks/useWebSocket';
   
   function App() {
     useWebSocket({
       tenantId: 'TENANT001',
       autoConnect: true,
       baseUrl: import.meta.env.VITE_WS_BASE_URL || 'ws://localhost:8081/cql-engine',
     });
     
     // Your existing components...
   }
   ```

3. **Start the frontend**:
   ```bash
   cd frontend
   npm run dev
   ```

## 📊 Using the Components

### Option 1: Complete Dashboard (Recommended)

```tsx
import { WebSocketDashboard } from './components/WebSocketDashboard';

function MyApp() {
  return <WebSocketDashboard />;
}
```

**Includes:**
- Overview metrics (batches, evaluations, success rate)
- Real-time event feed
- Statistical analytics
- Performance metrics
- Connection status

### Option 2: Individual Components

```tsx
import { RealTimeEventPanel } from './components/RealTimeEventPanel';

function MyApp() {
  return (
    <RealTimeEventPanel 
      maxEvents={100} 
      autoScroll={true} 
    />
  );
}
```

**Features:**
- Live event stream
- Pause/resume
- Export to JSON
- Real-time stats
- Color-coded events

## 🧪 Testing

### 1. Check Backend is Running
```bash
curl http://localhost:8081/cql-engine/actuator/health
```

### 2. Test WebSocket Connection (Browser Console)
```javascript
const ws = new WebSocket('ws://localhost:8081/cql-engine/ws/evaluation-progress?tenantId=TENANT001');
ws.onmessage = (e) => console.log('Received:', JSON.parse(e.data));
```

### 3. Trigger an Evaluation
```bash
curl -X POST http://localhost:8081/cql-engine/api/evaluations/batch \
  -H "Content-Type: application/json" \
  -d '{
    "measureId": "CMS124",
    "patientIds": ["patient-1", "patient-2"],
    "measurementPeriodStart": "2024-01-01",
    "measurementPeriodEnd": "2024-12-31"
  }'
```

### 4. Watch Events in Frontend
Open browser at http://localhost:5173 and observe:
- Connection status → "Connected" (green)
- Events appearing in real-time
- Statistics updating
- Batch progress tracking

## 🔑 With Authentication

```tsx
useWebSocket({
  tenantId: 'TENANT001',
  authToken: localStorage.getItem('authToken'), // Your JWT token
  autoConnect: true,
  baseUrl: 'ws://localhost:8081/cql-engine',
});
```

## 🐳 Docker Deployment

1. **Set environment variables**:
   ```bash
   # In docker-compose.yml or .env
   VISUALIZATION_KAFKA_ENABLED=true
   SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
   ```

2. **Frontend WebSocket URL**:
   ```bash
   VITE_WS_BASE_URL=ws://cql-engine-service:8081/cql-engine
   ```

3. **Start services**:
   ```bash
   docker-compose up -d
   ```

## 📝 Configuration Reference

### Backend Properties

```yaml
# application.yml
visualization:
  websocket:
    enabled: true
    allowed-origins: "http://localhost:4200,http://localhost:5173"
  kafka:
    enabled: false  # true for Docker/production
  batch-progress:
    emit-interval-seconds: 5
    emit-every-n-patients: 10
```

### Frontend Environment

```bash
# .env
VITE_WS_BASE_URL=ws://localhost:8081/cql-engine
```

## 🛠️ Troubleshooting

### WebSocket Won't Connect

**Check:**
1. Backend running? `curl http://localhost:8081/cql-engine/actuator/health`
2. Correct URL? Should be `ws://` not `http://`
3. CORS issue? Check allowed-origins includes your frontend URL
4. Browser console for errors

**Solution:**
```bash
# Verify WebSocket endpoint
curl -i -N \
  -H "Connection: Upgrade" \
  -H "Upgrade: websocket" \
  http://localhost:8081/cql-engine/ws/evaluation-progress
```

### No Events Received

**Check:**
1. Kafka enabled if using event consumer
2. Evaluations actually running
3. Backend logs for WebSocket handler messages

**Debug:**
```javascript
// Browser console
const ws = new WebSocket('ws://localhost:8081/cql-engine/ws/evaluation-progress');
ws.onopen = () => console.log('Connected!');
ws.onerror = (e) => console.error('Error:', e);
ws.onmessage = (e) => console.log('Message:', e.data);
```

### Connection Keeps Dropping

**Possible causes:**
1. Network instability → Auto-reconnect will handle
2. Backend restart → Auto-reconnect will handle
3. Firewall/proxy blocking WebSocket → Check network configuration

**Check reconnection:**
- Frontend status indicator shows "Reconnecting" → Working as expected
- Check browser console for reconnection attempts

## 📦 Key Files

### Backend
- `WebSocketConfig.java` - WebSocket endpoint configuration
- `EvaluationProgressWebSocketHandler.java` - Session management
- `CqlSecurityCustomizer.java` - Security configuration
- `application*.yml` - Properties configuration

### Frontend
- `websocket.service.ts` - WebSocket client service
- `useWebSocket.ts` - React hook
- `RealTimeEventPanel.tsx` - Event monitoring UI
- `WebSocketDashboard.tsx` - Complete dashboard
- `evaluationStore.ts` - State management

## 🎯 Common Use Cases

### 1. Monitor Active Evaluations
```tsx
<WebSocketDashboard />
```
Shows all active batches, real-time progress, and statistics.

### 2. Debug Evaluation Issues
```tsx
<RealTimeEventPanel maxEvents={200} autoScroll={true} />
```
See every event with timestamps, pause to inspect, export for analysis.

### 3. Track Performance
```tsx
<PerformanceMetricsPanel batchProgress={activeBatch} />
```
Monitor throughput, duration, and compliance in real-time.

### 4. Custom Analytics
```tsx
const allBatches = useEvaluationStore(selectAllBatches);
<AnalyticsPanel batches={allBatches} metric="successRate" />
```
Statistical analysis of batch performance.

## 🔍 Monitoring

### Frontend
- Connection status: Check dashboard indicator
- Event rate: Stats panel shows events/sec
- Success rate: Calculated in real-time

### Backend
- Active connections: GET `/api/visualization/connections`
- Health check: GET `/actuator/health`
- Logs: Check `cql-engine-service.log`

## 📚 Documentation

- **Complete Guide**: `WEBSOCKET_IMPLEMENTATION_GUIDE.md`
- **Completion Summary**: `WEBSOCKET_COMPLETION_SUMMARY.md`
- **This Quick Start**: `WEBSOCKET_QUICK_START.md`

## ✅ Checklist

Before deploying:
- [ ] Backend running and healthy
- [ ] Frontend can connect to WebSocket
- [ ] Test evaluation triggers events
- [ ] Events appear in dashboard
- [ ] Connection status indicator works
- [ ] Auto-reconnect tested
- [ ] CORS configured for production domain
- [ ] Authentication token integration tested (if needed)

## 🚦 Success Indicators

✅ **Working Correctly:**
- Connection status shows "Connected" (green)
- Events appear within 1-2 seconds
- Statistics update in real-time
- Auto-reconnect works after disconnect
- No console errors

❌ **Needs Investigation:**
- Connection status stuck on "Connecting"
- No events after triggering evaluation
- Console shows WebSocket errors
- Connection drops every few seconds

## 💡 Pro Tips

1. **Use pause button** when debugging to prevent event flood
2. **Export to JSON** for detailed analysis
3. **Monitor events/sec** to detect performance issues
4. **Check active batch progress** for real-time evaluation status
5. **Use tenant filtering** for multi-tenant environments

---

**Need Help?** Check the detailed documentation in:
- `WEBSOCKET_IMPLEMENTATION_GUIDE.md` - Complete technical guide
- `WEBSOCKET_COMPLETION_SUMMARY.md` - Feature checklist and testing

**Questions?** Open an issue or contact the development team.
