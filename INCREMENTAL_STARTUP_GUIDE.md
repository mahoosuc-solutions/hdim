# Incremental Backend & Frontend Startup Guide

## Current Status ✅

**Backend Services (Running):**
- ✅ PostgreSQL: `localhost:5435` (healthy)
- ✅ Redis: `localhost:6380` (healthy)
- ✅ Kafka: `localhost:9092-9093` (healthy)
- ✅ Zookeeper: `localhost:2181` (healthy)
- ✅ CQL Engine Service: `localhost:8081` (healthy)
- ✅ Quality Measure Service: `localhost:8087` (healthy)
- ⚠️ FHIR Service (HAPI): `localhost:8080` (unhealthy - but optional for testing)

**Frontend (Ready to Start):**
- ✅ React 19 + TypeScript + Vite
- ✅ Material-UI v7 + Recharts + Zustand
- ✅ All components built and ready
- ⚠️ Needs backend URL configuration

---

## Phase 1: Verify Backend Services (5 minutes)

### Step 1.1: Test Backend Endpoints

```bash
# Test CQL Engine Service health
curl http://localhost:8081/cql-engine/actuator/health

# Expected: {"status":"UP"}

# Test Visualization endpoints
curl http://localhost:8081/cql-engine/api/visualization/health

# Expected: {"websocket":"UP","kafkaConsumers":"UP",...}

# Test Quality Measure Service
curl http://localhost:8087/quality-measure/actuator/health

# Expected: {"status":"UP"}
```

### Step 1.2: Check WebSocket Endpoint Availability

```bash
# Test WebSocket endpoint (will fail with "Upgrade Required" - that's good!)
curl http://localhost:8081/cql-engine/ws/evaluation-progress

# Expected: HTTP 400 or upgrade required message
```

### Step 1.3: Check Docker Logs

```bash
# Check CQL Engine logs for any errors
docker logs healthdata-cql-engine --tail 50

# Check Quality Measure Service logs
docker logs healthdata-quality-measure --tail 50

# Check for Kafka connection
docker logs healthdata-cql-engine | grep -i kafka
```

**✅ Success Criteria:**
- CQL Engine health returns `UP`
- Visualization health returns `websocket: UP`
- No critical errors in logs

---

## Phase 2: Configure Frontend for Backend (10 minutes)

### Step 2.1: Create Frontend Environment Configuration

The frontend needs to know where the backend is running. Create `.env` files:

```bash
cd frontend

# Create .env file for local development
cat > .env.local << 'EOF'
# Backend API Configuration
VITE_API_BASE_URL=http://localhost:8081/cql-engine
VITE_WS_BASE_URL=ws://localhost:8081/cql-engine

# Quality Measure Service
VITE_QUALITY_MEASURE_URL=http://localhost:8087/quality-measure

# Default tenant for testing
VITE_DEFAULT_TENANT_ID=TENANT001

# Feature flags
VITE_ENABLE_NOTIFICATIONS=true
VITE_ENABLE_DARK_MODE=true
EOF
```

### Step 2.2: Update Vite Configuration for Proxy (Optional)

This helps avoid CORS issues during development:

```bash
# Update vite.config.ts
cat > vite.config.ts << 'EOF'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/cql-engine': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false,
        ws: true, // Enable WebSocket proxying
      },
      '/quality-measure': {
        target: 'http://localhost:8087',
        changeOrigin: true,
        secure: false,
      }
    }
  }
})
EOF
```

### Step 2.3: Update WebSocket Connection URL in Code

Check if the frontend is using the correct WebSocket URL:

```typescript
// frontend/src/hooks/useWebSocket.ts should use:
const wsUrl = import.meta.env.VITE_WS_BASE_URL || 'ws://localhost:8081/cql-engine';
```

**✅ Success Criteria:**
- `.env.local` created with backend URLs
- `vite.config.ts` configured with proxy (optional)
- Environment variables accessible in code

---

## Phase 3: Start Frontend Development Server (2 minutes)

### Step 3.1: Install Dependencies (if needed)

```bash
cd frontend

# Check if node_modules exists
ls node_modules > /dev/null 2>&1 && echo "Dependencies installed ✅" || npm install
```

### Step 3.2: Start Vite Development Server

```bash
npm run dev
```

**Expected output:**
```
  VITE v7.x.x  ready in XXX ms

  ➜  Local:   http://localhost:3000/
  ➜  Network: use --host to expose
  ➜  press h + enter to show help
```

### Step 3.3: Open Browser

```bash
# Open in default browser
xdg-open http://localhost:3000  # Linux
# OR
open http://localhost:3000      # macOS
```

**✅ Success Criteria:**
- Vite dev server starts without errors
- Browser opens dashboard
- No console errors related to missing dependencies

---

## Phase 4: Verify Frontend-Backend Connection (5 minutes)

### Step 4.1: Check WebSocket Connection Status

In the browser:
1. Open dashboard at `http://localhost:3000`
2. Check top-right corner for **Connection Status** indicator
3. Should show:
   - 🟢 **Connected** (green circle) - Good!
   - 🟡 **Connecting...** (yellow) - Wait a few seconds
   - 🔴 **Disconnected** (red) - Check console for errors

### Step 4.2: Open Browser DevTools

```
Press F12 or Ctrl+Shift+I (Cmd+Option+I on Mac)
```

**Check Console Tab:**
- Look for WebSocket connection messages
- Should see: `WebSocket connected to ws://localhost:8081/cql-engine/ws/evaluation-progress?tenantId=TENANT001`
- No CORS errors
- No 404 errors

**Check Network Tab:**
- Filter by "WS" (WebSocket)
- Should see active WebSocket connection
- Status: `101 Switching Protocols` (successful upgrade)

### Step 4.3: Test API Endpoints

In browser console:
```javascript
// Test visualization config endpoint
fetch('http://localhost:8081/cql-engine/api/visualization/config')
  .then(r => r.json())
  .then(data => console.log('Config:', data))
  .catch(e => console.error('Error:', e));

// Test connection stats
fetch('http://localhost:8081/cql-engine/api/visualization/connections')
  .then(r => r.json())
  .then(data => console.log('Connections:', data))
  .catch(e => console.error('Error:', e));
```

**✅ Success Criteria:**
- WebSocket shows `Connected` status
- Console shows successful WebSocket connection
- API fetch requests return data without CORS errors
- Connection stats show at least 1 active connection

---

## Phase 5: Trigger Test Evaluation (10 minutes)

Now let's trigger an actual evaluation to see events flowing:

### Step 5.1: Create Test Evaluation via API

```bash
# First, check if any CQL libraries exist
curl http://localhost:8081/cql-engine/api/cql/libraries \
  -H "X-Tenant-ID: TENANT001" \
  | jq '.content[] | {id, libraryName, version}'

# If libraries exist, trigger evaluation with a library ID
# Replace <LIBRARY_ID> with actual ID from above
curl -X POST http://localhost:8081/cql-engine/api/cql/evaluations \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: TENANT001" \
  -d '{
    "libraryId": "<LIBRARY_ID>",
    "patientId": "test-patient-001"
  }'
```

### Step 5.2: Watch Frontend for Events

In the browser dashboard:
1. **Summary Statistics** should increment
2. **Recent Events** section should show new events
3. **Batch Progress** (if batch evaluation) should update

### Step 5.3: Monitor WebSocket Messages

In browser DevTools console:
```javascript
// You should see incoming WebSocket messages logged by the app
// Look for messages like:
// {
//   type: "EVALUATION_EVENT",
//   timestamp: 1699564800000,
//   data: { eventType: "EVALUATION_COMPLETED", ... }
// }
```

**✅ Success Criteria:**
- Events appear in frontend dashboard
- Statistics update in real-time
- No errors in browser console
- WebSocket shows active message flow

---

## Phase 6: Trigger Batch Evaluation (Optional - 15 minutes)

For a more impressive demo, trigger a batch evaluation:

### Step 6.1: Create Mock Patient IDs

```bash
# Generate array of patient IDs
cat > /tmp/batch_request.json << 'EOF'
{
  "libraryId": "<LIBRARY_ID>",
  "patientIds": [
    "patient-001", "patient-002", "patient-003", "patient-004", "patient-005",
    "patient-006", "patient-007", "patient-008", "patient-009", "patient-010",
    "patient-011", "patient-012", "patient-013", "patient-014", "patient-015",
    "patient-016", "patient-017", "patient-018", "patient-019", "patient-020"
  ]
}
EOF
```

### Step 6.2: Trigger Batch Evaluation

```bash
# Replace <LIBRARY_ID> with actual library ID
curl -X POST http://localhost:8081/cql-engine/api/cql/evaluations/batch \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: TENANT001" \
  -d @/tmp/batch_request.json
```

### Step 6.3: Watch Real-Time Progress

In the frontend dashboard:
- **Batch Progress Card** should appear
- Progress bar animates from 0% to 100%
- **ETA** (estimated time) displayed
- **Throughput** (evaluations/second) shown
- **Live Event Feed** scrolls with individual evaluations
- **Performance Charts** update in real-time

**✅ Success Criteria:**
- Batch progress card appears and updates
- Progress bar reaches 100%
- All statistics update correctly
- Performance metrics visible

---

## Troubleshooting Guide

### Issue 1: WebSocket Connection Fails

**Symptoms:**
- Red "Disconnected" status in dashboard
- Console error: `WebSocket connection to 'ws://localhost:8081/...' failed`

**Solutions:**
1. Check CQL Engine service is running:
   ```bash
   docker ps | grep healthdata-cql-engine
   curl http://localhost:8081/cql-engine/actuator/health
   ```

2. Check WebSocket endpoint is configured:
   ```bash
   docker logs healthdata-cql-engine | grep -i websocket
   ```

3. Verify WebSocket port is correct in frontend:
   - Check `frontend/.env.local` has `VITE_WS_BASE_URL=ws://localhost:8081/cql-engine`
   - Check `frontend/src/hooks/useWebSocket.ts` uses correct URL

4. Check CORS/proxy configuration in `vite.config.ts`

### Issue 2: CORS Errors

**Symptoms:**
- Console error: `Access to fetch at 'http://localhost:8081' ... has been blocked by CORS policy`

**Solutions:**
1. **Option A:** Use Vite proxy (recommended for development):
   - Ensure `vite.config.ts` has proxy configuration (see Phase 2.2)
   - Update frontend to use relative URLs: `/cql-engine/api/...` instead of `http://localhost:8081/cql-engine/api/...`

2. **Option B:** Enable CORS in backend:
   - Check `backend/.../SecurityConfig.java` allows `http://localhost:3000`
   - Restart backend services after changes

### Issue 3: No Events Appearing

**Symptoms:**
- WebSocket connected but no events in dashboard
- "Recent Events" section stays empty

**Solutions:**
1. Check Kafka is running and healthy:
   ```bash
   docker ps | grep kafka
   docker logs healthdata-kafka --tail 20
   ```

2. Verify Kafka consumer is active:
   ```bash
   docker logs healthdata-cql-engine | grep -i "kafka consumer"
   ```

3. Check backend is publishing events:
   ```bash
   docker logs healthdata-cql-engine | grep -i "publishing event"
   ```

4. Manually trigger an evaluation (see Phase 5)

### Issue 4: Frontend Build Errors

**Symptoms:**
- `npm run dev` fails with TypeScript errors
- Missing module errors

**Solutions:**
1. Clear cache and reinstall:
   ```bash
   cd frontend
   rm -rf node_modules package-lock.json
   npm install
   ```

2. Check Node.js version (needs 18+):
   ```bash
   node --version  # Should be v18.x or higher
   ```

3. Update TypeScript definitions:
   ```bash
   npm install --save-dev @types/node @types/react @types/react-dom
   ```

### Issue 5: Backend Services Not Healthy

**Symptoms:**
- Docker health checks failing
- Services restarting repeatedly

**Solutions:**
1. Check service logs:
   ```bash
   docker logs healthdata-cql-engine
   docker logs healthdata-postgres
   docker logs healthdata-redis
   docker logs healthdata-kafka
   ```

2. Restart unhealthy services:
   ```bash
   docker restart healthdata-cql-engine
   ```

3. Restart entire stack (nuclear option):
   ```bash
   docker-compose down
   docker-compose up -d
   ```

---

## Quick Reference: Common Commands

### Backend
```bash
# Check all service health
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# View CQL Engine logs
docker logs -f healthdata-cql-engine

# Restart CQL Engine
docker restart healthdata-cql-engine

# Test backend health
curl http://localhost:8081/cql-engine/actuator/health
```

### Frontend
```bash
# Start development server
cd frontend && npm run dev

# Run tests
npm test

# Build for production
npm run build

# Preview production build
npm run preview
```

### Testing
```bash
# Test WebSocket endpoint
wscat -c "ws://localhost:8081/cql-engine/ws/evaluation-progress?tenantId=TENANT001"

# Test visualization API
curl http://localhost:8081/cql-engine/api/visualization/config | jq

# Get connection stats
curl http://localhost:8081/cql-engine/api/visualization/connections | jq
```

---

## Success Validation Checklist

Before proceeding to production or advanced features, verify:

### Backend
- [ ] All Docker containers healthy (green in `docker ps`)
- [ ] CQL Engine `/actuator/health` returns `UP`
- [ ] Visualization `/api/visualization/health` returns `websocket: UP`
- [ ] Kafka consumer logs show active consumption
- [ ] No critical errors in logs

### Frontend
- [ ] `npm run dev` starts without errors
- [ ] Dashboard loads at `http://localhost:3000`
- [ ] WebSocket shows "Connected" status (green indicator)
- [ ] No console errors in browser DevTools
- [ ] API requests succeed (no CORS errors)

### Integration
- [ ] Manual evaluation triggers events in dashboard
- [ ] Statistics update in real-time
- [ ] Event list populates with evaluations
- [ ] Batch evaluation shows progress bar
- [ ] WebSocket messages visible in DevTools Network tab

---

## Next Steps After Successful Startup

Once everything is running:

1. **Explore the Dashboard:**
   - Click on individual events to see details
   - Try filtering events by type or measure
   - Use search to find specific patient IDs
   - Export events to CSV

2. **Test Batch Evaluations:**
   - Trigger batch with 50-100 patients
   - Watch real-time progress updates
   - Compare multiple batches
   - Check performance metrics

3. **Customize Settings:**
   - Click Settings icon (top-right)
   - Enable/disable notifications
   - Adjust auto-refresh interval
   - Configure alert thresholds

4. **Advanced Features:**
   - Use keyboard shortcuts (Ctrl+?)
   - Compare multiple batches
   - View historical trends
   - Export advanced reports

5. **Production Readiness:**
   - Update environment variables for production
   - Configure authentication
   - Set up monitoring and alerts
   - Deploy to cloud infrastructure

---

## Support & Resources

**Documentation:**
- Backend: `backend/modules/services/cql-engine-service/VISUALIZATION_README.md`
- Frontend Plan: `docs/VISUALIZATION_IMPLEMENTATION_PLAN.md`
- Template Engine: `backend/modules/services/cql-engine-service/TEMPLATE_ENGINE_README.md`

**Endpoints:**
- CQL Engine API: http://localhost:8081/cql-engine/swagger-ui.html
- Quality Measure API: http://localhost:8087/quality-measure/swagger-ui.html
- Prometheus Metrics: http://localhost:9090
- Grafana Dashboards: http://localhost:3001 (admin/admin)

**WebSocket Endpoint:**
- ws://localhost:8081/cql-engine/ws/evaluation-progress?tenantId=TENANT001

---

**Last Updated:** 2025-01-04
**Version:** 1.0.0
**Status:** ✅ Ready for Use
