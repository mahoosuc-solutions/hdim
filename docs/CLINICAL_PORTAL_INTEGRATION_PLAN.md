# Clinical Portal Integration Plan

**Status:** Planning Phase
**Created:** January 22, 2026
**Author:** HDIM Platform Team

---

## Executive Summary

This document outlines the integration plan for connecting the HDIM Clinical Portal (React + TypeScript frontend) with the backend microservices architecture. The frontend currently exists as a **CQL Engine Visualization Dashboard** and needs to be expanded to support the full clinical workflow.

**Current State:**
- ✅ Frontend exists at `/frontend/` (React 19.1.1 + TypeScript + Vite)
- ✅ WebSocket integration complete (CQL Engine real-time updates)
- ✅ Material-UI 7.3.4 design system in place
- ✅ Zustand state management configured
- ✅ Development server runs on port 3000 with API proxy configuration
- ✅ Three event services running healthy (quality-measure, care-gap, clinical-workflow)
- ✅ Event Store service operational (8090)
- ✅ Gateway service running (8080)

**Goal:** Expand the dashboard to a full Clinical Portal with:
- Patient management
- Care gap tracking and closure
- Quality measure evaluation and reporting
- Clinical workflow management
- FHIR resource access
- Real-time updates across all modules

---

## Current Frontend Architecture

### Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | React | 19.1.1 |
| Language | TypeScript | 5.6.3 |
| Build Tool | Vite | 6.0.5 |
| UI Library | Material-UI (MUI) | 7.3.4 |
| State Management | Zustand | 5.0.8 |
| WebSocket | @stomp/stompjs | 7.0.0 |
| Charting | Recharts | 2.15.0 |

### Existing Features (Phase 2.1 - CQL Engine Dashboard)

✅ **Connection Management:**
- WebSocket connection to CQL Engine (ws://localhost:8081/cql-engine)
- Automatic reconnection with exponential backoff
- Connection status indicator in AppBar
- Tenant-based filtering (default: TENANT001)

✅ **Real-Time Event Streaming:**
- Batch evaluation progress tracking
- Event type filtering (EVALUATION_STARTED, EVALUATION_COMPLETED, etc.)
- Measure-based filtering
- Search functionality across events

✅ **Data Visualization:**
- Performance metrics panel with gauges
- Historical trends chart (line/bar/area charts)
- Batch comparison view (2+ batches)
- Multi-batch statistical comparison (3+ batches)
- Analytics panel with statistical insights

✅ **User Experience:**
- Dark mode toggle
- Keyboard shortcuts (Ctrl+?, Ctrl+B, Ctrl+E, etc.)
- Toast notifications
- Browser notifications (opt-in)
- Settings panel
- Advanced export (CSV, JSON, Excel)

### Current API Configuration

**File:** `frontend/vite.config.ts`

```typescript
server: {
  port: 3000,
  host: true,
  proxy: {
    '/cql-engine': {
      target: 'http://localhost:8081',
      changeOrigin: true,
      ws: true, // WebSocket support
    },
    '/quality-measure': {
      target: 'http://localhost:8087',
      changeOrigin: true,
    },
    '/api/sales': {
      target: 'http://localhost:8106',
      changeOrigin: true,
      rewrite: (path) => `/sales-automation${path}`,
    }
  }
}
```

**Environment Variables (App.tsx):**
- `VITE_WS_BASE_URL` - WebSocket base URL (default: ws://localhost:8081/cql-engine)
- `VITE_API_URL` - REST API base URL (not currently used)

---

## Backend Services Status

### Currently Running Services

| Service | Port | Context Path | Status | Purpose |
|---------|------|-------------|--------|---------|
| **Gateway Service** | 8080 | `/` | ✅ Healthy | API Gateway, JWT validation, routing |
| **Event Store Service** | 8090 | `/event-store` | ✅ Healthy | Immutable event log, event sourcing |
| **Quality Measure Event Service** | 8112 | `/quality-measure-event` | ✅ Healthy | Quality measure projections (CQRS read model) |
| **Care Gap Event Service** | 8111 | `/care-gap-event` | ✅ Healthy | Care gap projections (CQRS read model) |
| **Clinical Workflow Event Service** | 8113 | `/clinical-workflow-event` | ✅ Healthy | Workflow projections (CQRS read model) |

### Infrastructure Services

| Service | Port | Purpose |
|---------|------|---------|
| PostgreSQL | 5435 | Primary database (29 databases) |
| Redis | 6380 | Cache layer (5-minute TTL for PHI) |
| Kafka | 9094 | Event streaming platform |
| Zookeeper | 2182 | Kafka coordination |
| Jaeger | 4317-4318, 6831, 14250 | Distributed tracing |

### Services NOT Running (Required for Full Portal)

| Service | Port | Context Path | Purpose | Priority |
|---------|------|-------------|---------|----------|
| CQL Engine Service | 8081 | `/cql-engine` | CQL evaluation, WebSocket streaming | ⭐ CRITICAL |
| Patient Service | 8084 | `/patient` | Patient CRUD, demographics | ⭐ CRITICAL |
| FHIR Service | 8085 | `/fhir` | FHIR R4 resource access | ⭐ CRITICAL |
| Care Gap Service | 8086 | `/care-gap` | Care gap detection, closure | ⭐ CRITICAL |
| Quality Measure Service | 8087 | `/quality-measure` | HEDIS measure evaluation | ⭐ CRITICAL |
| HCC Service | 8091 | `/hcc` | Risk adjustment, HCC coding | High |
| Analytics Service | 8088 | `/analytics` | Cross-service analytics | High |
| Agent Runtime Service | 8092 | `/agent-runtime` | AI agent execution | Medium |
| Agent Builder Service | 8095 | `/agent-builder` | AI agent creation | Medium |
| Approval Service | 8096 | `/approval` | Workflow approvals | Medium |

---

## Integration Requirements

### Phase 1: Core Services Startup (Week 1)

**Objective:** Start the 5 critical services needed for basic portal functionality.

#### 1.1 Start CQL Engine Service (Port 8081)

**Why:** Frontend expects WebSocket connection at `ws://localhost:8081/cql-engine/ws/evaluation-progress`

**Steps:**
```bash
# Build service
cd backend
./gradlew :modules:services:cql-engine-service:bootJar -x test

# Build Docker image
docker compose build cql-engine-service

# Start service
docker compose up -d cql-engine-service

# Verify health
docker compose logs -f cql-engine-service | head -50
curl http://localhost:8081/cql-engine/actuator/health
```

**Dependencies:**
- PostgreSQL database: `cql_engine_db`
- Kafka topics: `cql.evaluation.events`
- Event Store Service (already running)

#### 1.2 Start Patient Service (Port 8084)

**Why:** Patient demographics and multi-tenant patient management.

**Steps:**
```bash
./gradlew :modules:services:patient-service:bootJar -x test
docker compose build patient-service
docker compose up -d patient-service
docker compose logs -f patient-service | head -50
```

**Dependencies:**
- PostgreSQL database: `patient_db`
- FHIR Service (for resource conversion)

#### 1.3 Start FHIR Service (Port 8085)

**Why:** FHIR R4 resource access for clinical data.

**Steps:**
```bash
./gradlew :modules:services:fhir-service:bootJar -x test
docker compose build fhir-service
docker compose up -d fhir-service
```

**Dependencies:**
- PostgreSQL database: `fhir_db`
- HAPI FHIR libraries

#### 1.4 Start Care Gap Service (Port 8086)

**Why:** Care gap detection and closure workflows.

**Steps:**
```bash
./gradlew :modules:services:care-gap-service:bootJar -x test
docker compose build care-gap-service
docker compose up -d care-gap-service
```

**Dependencies:**
- PostgreSQL database: `care_gap_db`
- Patient Service (8084)
- FHIR Service (8085)
- CQL Engine Service (8081)
- Quality Measure Service (8087)

#### 1.5 Start Quality Measure Service (Port 8087)

**Why:** HEDIS quality measure evaluation.

**Steps:**
```bash
./gradlew :modules:services:quality-measure-service:bootJar -x test
docker compose build quality-measure-service
docker compose up -d quality-measure-service
```

**Dependencies:**
- PostgreSQL database: `quality_measure_db`
- CQL Engine Service (8081)
- Patient Service (8084)

**Success Criteria:**
- [ ] All 5 services show healthy status: `docker compose ps`
- [ ] WebSocket connects from frontend to CQL Engine
- [ ] Gateway routes requests to all 5 services
- [ ] Distributed tracing spans appear in Jaeger

---

### Phase 2: Frontend API Integration (Week 2)

**Objective:** Connect frontend to backend REST APIs via gateway.

#### 2.1 Add API Service Layer

Create service files for each backend service:

**File:** `frontend/src/services/api.config.ts`
```typescript
export const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export const ENDPOINTS = {
  // Gateway routes (via gateway-service:8080)
  PATIENT: `${API_BASE_URL}/patient`,
  FHIR: `${API_BASE_URL}/fhir`,
  CARE_GAP: `${API_BASE_URL}/care-gap`,
  QUALITY_MEASURE: `${API_BASE_URL}/quality-measure`,
  CQL_ENGINE: `${API_BASE_URL}/cql-engine`,

  // WebSocket endpoint (direct connection)
  WS_CQL_ENGINE: import.meta.env.VITE_WS_BASE_URL || 'ws://localhost:8081/cql-engine',
};
```

**New Service Files:**
- `frontend/src/services/patient.service.ts` - Patient CRUD
- `frontend/src/services/fhir.service.ts` - FHIR resource access
- `frontend/src/services/careGap.service.ts` - Care gap management
- `frontend/src/services/qualityMeasure.service.ts` - Quality measure evaluation
- `frontend/src/services/cqlEngine.service.ts` - CQL evaluation (REST API)

#### 2.2 Add Authentication Integration

**Gateway JWT Flow:**
1. User logs in → Gateway validates credentials
2. Gateway issues JWT token
3. Frontend stores JWT in localStorage/sessionStorage
4. Frontend includes JWT in Authorization header for all API calls
5. Gateway validates JWT and injects `X-Auth-UserId`, `X-Auth-Roles`, `X-Tenant-ID` headers
6. Backend services trust gateway headers (no JWT validation)

**Implementation:**
```typescript
// frontend/src/services/auth.service.ts
export class AuthService {
  private static TOKEN_KEY = 'hdim_jwt_token';

  async login(username: string, password: string): Promise<User> {
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });

    const data = await response.json();
    localStorage.setItem(this.TOKEN_KEY, data.token);
    return data.user;
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
  }
}

// Add interceptor to all API calls
export const apiClient = axios.create({
  baseURL: API_BASE_URL,
});

apiClient.interceptors.request.use((config) => {
  const token = authService.getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

#### 2.3 Update Vite Proxy Configuration

**File:** `frontend/vite.config.ts`

```typescript
server: {
  port: 3000,
  host: true,
  proxy: {
    // Route ALL API calls through gateway
    '/api': {
      target: 'http://localhost:8080', // Gateway
      changeOrigin: true,
      secure: false,
    },
    // Direct WebSocket connection to CQL Engine (bypasses gateway)
    '/cql-engine/ws': {
      target: 'ws://localhost:8081',
      changeOrigin: true,
      ws: true,
    }
  }
}
```

#### 2.4 Add Environment Configuration

**File:** `frontend/.env.development`
```bash
# API Configuration
VITE_API_URL=http://localhost:8080
VITE_WS_BASE_URL=ws://localhost:8081/cql-engine

# Tenant Configuration
VITE_DEFAULT_TENANT_ID=TENANT001

# Feature Flags
VITE_ENABLE_DARK_MODE=true
VITE_ENABLE_NOTIFICATIONS=true
VITE_ENABLE_ANALYTICS=true
```

**File:** `frontend/.env.production`
```bash
# API Configuration
VITE_API_URL=https://api.healthdata.example.com
VITE_WS_BASE_URL=wss://api.healthdata.example.com/cql-engine

# Tenant Configuration
VITE_DEFAULT_TENANT_ID=TENANT001

# Feature Flags
VITE_ENABLE_DARK_MODE=true
VITE_ENABLE_NOTIFICATIONS=true
VITE_ENABLE_ANALYTICS=true
```

**Success Criteria:**
- [ ] API service layer created for all 5 services
- [ ] Authentication flow implemented with JWT storage
- [ ] Vite proxy routes all API calls through gateway
- [ ] Environment variables configured for dev/prod
- [ ] API calls include Authorization header automatically

---

### Phase 3: Frontend Feature Expansion (Week 3-4)

**Objective:** Add new UI modules beyond CQL Engine dashboard.

#### 3.1 Patient Management Module

**Components:**
- `PatientList.tsx` - Patient search and listing
- `PatientDetail.tsx` - Patient demographics and history
- `PatientForm.tsx` - Create/edit patient
- `PatientCareGaps.tsx` - Care gaps for specific patient

**API Integration:**
- GET `/patient/api/v1/patients?tenantId=TENANT001` - List patients
- GET `/patient/api/v1/patients/{patientId}` - Get patient details
- POST `/patient/api/v1/patients` - Create patient
- PUT `/patient/api/v1/patients/{patientId}` - Update patient
- GET `/patient/api/v1/patients/{patientId}/care-gaps` - Get patient care gaps

#### 3.2 Care Gap Management Module

**Components:**
- `CareGapList.tsx` - Care gap dashboard (filterable by severity, status)
- `CareGapDetail.tsx` - Care gap details with closure workflow
- `CareGapClosureForm.tsx` - Document care gap closure
- `PopulationHealth.tsx` - Population health metrics (critical/high/medium/low counts)

**API Integration:**
- GET `/care-gap/api/v1/care-gaps?tenantId=TENANT001&status=OPEN` - List care gaps
- GET `/care-gap/api/v1/care-gaps/{gapId}` - Get care gap details
- POST `/care-gap/api/v1/care-gaps/{gapId}/close` - Close care gap
- GET `/care-gap/api/v1/population-health?tenantId=TENANT001` - Population health metrics

**WebSocket Integration:**
- Subscribe to care gap events via Kafka → Event Store → WebSocket
- Real-time updates when gaps detected/closed

#### 3.3 Quality Measure Module

**Components:**
- `MeasureList.tsx` - List available HEDIS measures
- `MeasureEvaluation.tsx` - Run measure evaluation for cohort
- `MeasureResults.tsx` - View measure results (numerator, denominator, compliance rate)
- `CohortCompliance.tsx` - Cohort-level compliance metrics

**API Integration:**
- GET `/quality-measure/api/v1/measures?tenantId=TENANT001` - List measures
- POST `/quality-measure/api/v1/measures/{measureId}/evaluate` - Evaluate measure
- GET `/quality-measure/api/v1/measures/{measureId}/results?tenantId=TENANT001` - Get results
- GET `/quality-measure/api/v1/cohort-compliance?measureCode=CMS122&tenantId=TENANT001` - Cohort compliance

**WebSocket Integration:**
- Real-time evaluation progress (already implemented for CQL Engine)
- Batch evaluation completion notifications

#### 3.4 Clinical Workflow Module

**Components:**
- `WorkflowList.tsx` - List active workflows (appointment scheduling, care coordination, etc.)
- `WorkflowDetail.tsx` - Workflow status, progress, assigned user
- `WorkflowAssignment.tsx` - Assign/reassign workflow
- `WorkflowProgress.tsx` - Update workflow progress

**API Integration:**
- GET `/clinical-workflow-event/api/v1/workflows?tenantId=TENANT001&status=IN_PROGRESS` - List workflows
- GET `/clinical-workflow-event/api/v1/workflows/{workflowId}` - Get workflow details
- POST `/clinical-workflow-event/api/v1/workflows/{workflowId}/assign` - Assign workflow
- PUT `/clinical-workflow-event/api/v1/workflows/{workflowId}/progress` - Update progress

**WebSocket Integration:**
- Real-time workflow status updates (started, assigned, completed, cancelled)
- Alert on blocking issues or reviews required

#### 3.5 Navigation and Routing

**Update:** `frontend/src/App.tsx`

Add React Router for multi-page navigation:

```typescript
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';

function App() {
  return (
    <BrowserRouter>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <Box sx={{ display: 'flex' }}>
          {/* Sidebar Navigation */}
          <Drawer variant="permanent">
            <List>
              <ListItem button component={Link} to="/dashboard">
                <DashboardIcon /> Dashboard
              </ListItem>
              <ListItem button component={Link} to="/patients">
                <PeopleIcon /> Patients
              </ListItem>
              <ListItem button component={Link} to="/care-gaps">
                <WarningIcon /> Care Gaps
              </ListItem>
              <ListItem button component={Link} to="/measures">
                <AssessmentIcon /> Quality Measures
              </ListItem>
              <ListItem button component={Link} to="/workflows">
                <WorkflowIcon /> Workflows
              </ListItem>
            </List>
          </Drawer>

          {/* Main Content */}
          <Box component="main" sx={{ flexGrow: 1, p: 3 }}>
            <Routes>
              <Route path="/" element={<Navigate to="/dashboard" />} />
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/patients" element={<PatientListPage />} />
              <Route path="/patients/:patientId" element={<PatientDetailPage />} />
              <Route path="/care-gaps" element={<CareGapListPage />} />
              <Route path="/care-gaps/:gapId" element={<CareGapDetailPage />} />
              <Route path="/measures" element={<MeasureListPage />} />
              <Route path="/measures/:measureId" element={<MeasureEvaluationPage />} />
              <Route path="/workflows" element={<WorkflowListPage />} />
              <Route path="/workflows/:workflowId" element={<WorkflowDetailPage />} />
            </Routes>
          </Box>
        </Box>
      </ThemeProvider>
    </BrowserRouter>
  );
}
```

**Success Criteria:**
- [ ] Patient management UI complete with CRUD operations
- [ ] Care gap dashboard shows real-time updates
- [ ] Quality measure evaluation runs and displays results
- [ ] Clinical workflow module tracks progress
- [ ] Navigation works across all modules
- [ ] WebSocket updates trigger UI refreshes

---

### Phase 4: HIPAA Compliance & Security (Week 5)

**Objective:** Ensure frontend meets HIPAA requirements for PHI handling.

#### 4.1 Session Timeout Implementation

**Already Implemented:** 15-minute idle timeout with 2-minute warning (see `App.tsx`)

**Verify:**
- Activity listeners (click, keypress, mousemove, scroll) reset timer
- Warning modal appears 2 minutes before logout
- "Stay Logged In" button extends session
- Automatic logout redirects to login page

#### 4.2 Audit Logging

**Backend:** Already implemented via `@Audited` annotation on PHI access methods.

**Frontend Requirement:** Log all PHI access in browser for forensic analysis.

**Implementation:**
```typescript
// frontend/src/services/audit.service.ts
export class AuditService {
  private static AUDIT_LOG_KEY = 'hdim_audit_log';

  logAccess(resourceType: string, resourceId: string, action: string): void {
    const auditEntry = {
      timestamp: new Date().toISOString(),
      userId: authService.getCurrentUserId(),
      tenantId: authService.getCurrentTenantId(),
      resourceType,
      resourceId,
      action,
    };

    // Store in localStorage (could also send to backend)
    const logs = this.getAuditLogs();
    logs.push(auditEntry);
    localStorage.setItem(this.AUDIT_LOG_KEY, JSON.stringify(logs));
  }

  getAuditLogs(): AuditEntry[] {
    const logs = localStorage.getItem(this.AUDIT_LOG_KEY);
    return logs ? JSON.parse(logs) : [];
  }
}

// Usage in patient.service.ts
export class PatientService {
  async getPatient(patientId: string): Promise<Patient> {
    const response = await apiClient.get(`/patient/api/v1/patients/${patientId}`);

    // Log PHI access
    auditService.logAccess('Patient', patientId, 'READ');

    return response.data;
  }
}
```

#### 4.3 Cache Control Headers

**Backend:** Already implemented with `Cache-Control: no-store, no-cache, must-revalidate` on PHI endpoints.

**Frontend Verification:**
```typescript
// Verify headers in API responses
apiClient.interceptors.response.use((response) => {
  const cacheControl = response.headers['cache-control'];

  // Warn if PHI endpoint missing cache control
  if (response.config.url?.includes('/patient') &&
      !cacheControl?.includes('no-store')) {
    console.warn('PHI endpoint missing Cache-Control header:', response.config.url);
  }

  return response;
});
```

#### 4.4 Secure Data Handling

**Best Practices:**
- ❌ Never log PHI to console (use LoggerService instead)
- ❌ Never store PHI in localStorage/sessionStorage
- ✅ Use in-memory state management (Zustand) for PHI
- ✅ Clear sensitive data on logout
- ✅ Use HTTPS in production (wss:// for WebSocket)

**Success Criteria:**
- [ ] Session timeout active and tested
- [ ] Audit logging records all PHI access
- [ ] Cache-Control headers verified on all PHI endpoints
- [ ] No PHI stored in browser storage
- [ ] HTTPS/WSS enforced in production

---

### Phase 5: Testing & Deployment (Week 6)

**Objective:** End-to-end testing and production deployment.

#### 5.1 Integration Testing

**Test Scenarios:**
1. **Authentication Flow:**
   - Login with valid credentials → JWT stored
   - API calls include Authorization header
   - Invalid JWT → redirect to login
   - Session timeout → automatic logout

2. **Patient Management:**
   - List patients (multi-tenant isolation)
   - Create new patient
   - View patient details
   - Update patient demographics

3. **Care Gap Workflow:**
   - View open care gaps
   - Close care gap with documentation
   - Real-time WebSocket updates on gap closure
   - Population health metrics update

4. **Quality Measure Evaluation:**
   - Select measure (e.g., CMS122)
   - Run evaluation for cohort
   - View real-time progress via WebSocket
   - Display results (numerator, denominator, compliance rate)

5. **Clinical Workflow Management:**
   - View active workflows
   - Assign workflow to user
   - Update workflow progress
   - Complete workflow

#### 5.2 Performance Testing

**Metrics to Measure:**
- WebSocket connection latency (<100ms)
- API response time (<500ms for read, <2s for write)
- Page load time (<3s)
- Event streaming throughput (1000+ events/sec)

**Tools:**
- Lighthouse (performance score)
- React DevTools Profiler
- Chrome DevTools Network tab
- k6 load testing (for backend)

#### 5.3 Docker Compose Updates

**Add Frontend Service:**

**File:** `docker-compose.yml`
```yaml
services:
  # ... existing services ...

  clinical-portal:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    ports:
      - "3000:3000"
    environment:
      - VITE_API_URL=http://gateway-service:8080
      - VITE_WS_BASE_URL=ws://cql-engine-service:8081/cql-engine
      - VITE_DEFAULT_TENANT_ID=TENANT001
    depends_on:
      - gateway-service
      - cql-engine-service
    networks:
      - hdim-network
```

**Frontend Dockerfile:**

**File:** `frontend/Dockerfile`
```dockerfile
# Build stage
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Production stage
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

**Nginx Configuration:**

**File:** `frontend/nginx.conf`
```nginx
server {
  listen 80;
  server_name localhost;

  # Frontend static files
  location / {
    root /usr/share/nginx/html;
    index index.html;
    try_files $uri $uri/ /index.html;
  }

  # Proxy API calls to gateway
  location /api/ {
    proxy_pass http://gateway-service:8080/;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection 'upgrade';
    proxy_set_header Host $host;
    proxy_cache_bypass $http_upgrade;
  }

  # Proxy WebSocket to CQL Engine
  location /cql-engine/ws/ {
    proxy_pass http://cql-engine-service:8081/cql-engine/ws/;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection 'upgrade';
  }
}
```

#### 5.4 Production Deployment Checklist

**Pre-Deployment:**
- [ ] All backend services healthy
- [ ] Frontend build passes (`npm run build`)
- [ ] Environment variables configured for production
- [ ] HTTPS/WSS certificates installed
- [ ] Database migrations applied
- [ ] Redis cache configured with 5-minute TTL for PHI
- [ ] Distributed tracing enabled (Jaeger)

**Deployment:**
- [ ] Deploy backend services first
- [ ] Verify gateway routes to all services
- [ ] Deploy frontend last
- [ ] Run smoke tests (login, view patient, evaluate measure)
- [ ] Monitor logs for errors
- [ ] Check distributed traces in Jaeger

**Post-Deployment:**
- [ ] User acceptance testing (UAT)
- [ ] Performance monitoring (Prometheus + Grafana)
- [ ] Error tracking (Sentry or similar)
- [ ] HIPAA compliance audit

**Success Criteria:**
- [ ] All integration tests pass
- [ ] Performance metrics meet SLA (<500ms API, <100ms WebSocket)
- [ ] Docker Compose starts all services successfully
- [ ] Production deployment successful with zero downtime
- [ ] HIPAA compliance verified

---

## Risk Mitigation

### Risk 1: Service Startup Failures

**Likelihood:** High (based on recent entity-migration issues)
**Impact:** Blocks integration

**Mitigation:**
1. ✅ **Already Done:** Fixed entity-migration validation for event services
2. **Before Building:** Run `./scripts/validate-before-docker-build.sh`
3. **During Build:** Build ONE service at a time (avoid system overload)
4. **After Build:** Verify health endpoints before proceeding
5. **If Failed:** Check logs (`docker compose logs -f SERVICE`), fix schema issues, rebuild

### Risk 2: WebSocket Connection Issues

**Likelihood:** Medium
**Impact:** No real-time updates

**Mitigation:**
1. Verify CQL Engine service is running and healthy
2. Test WebSocket endpoint directly: `wscat -c ws://localhost:8081/cql-engine/ws/evaluation-progress?tenantId=TENANT001`
3. Check CORS configuration in CQL Engine
4. Verify Vite proxy configuration for WebSocket passthrough
5. Add connection retry logic with exponential backoff (already implemented)

### Risk 3: CORS Issues

**Likelihood:** Medium
**Impact:** API calls blocked by browser

**Mitigation:**
1. **Option 1:** Use Vite proxy for development (already configured)
2. **Option 2:** Configure CORS in gateway service:
   ```yaml
   spring:
     cloud:
       gateway:
         globalcors:
           cors-configurations:
             '[/**]':
               allowed-origins: "http://localhost:3000"
               allowed-methods: ["GET", "POST", "PUT", "DELETE"]
               allowed-headers: "*"
               allow-credentials: true
   ```
3. **Option 3:** Deploy frontend and backend on same domain in production

### Risk 4: Authentication Integration Issues

**Likelihood:** Medium
**Impact:** Users cannot log in

**Mitigation:**
1. Verify gateway JWT validation is working
2. Test login endpoint: `curl -X POST http://localhost:8080/auth/login -d '{"username":"admin","password":"password"}'`
3. Verify JWT includes required claims (userId, roles, tenantId)
4. Test JWT expiration and refresh token flow
5. Add comprehensive error handling in frontend auth service

### Risk 5: Multi-Tenant Data Isolation Failures

**Likelihood:** Low (already enforced at database level)
**Impact:** CRITICAL - PHI exposure across tenants

**Mitigation:**
1. ✅ **Already Done:** All repositories filter by tenantId
2. Verify gateway injects `X-Tenant-ID` header from JWT
3. Test tenant isolation: Try accessing data from Tenant A while logged in as Tenant B
4. Add integration tests for tenant isolation
5. Monitor audit logs for cross-tenant access attempts

---

## Success Metrics

### Technical Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Backend Service Availability | 99.9% | Docker health checks |
| API Response Time (Read) | <500ms | Prometheus metrics |
| API Response Time (Write) | <2s | Prometheus metrics |
| WebSocket Latency | <100ms | Client-side timing |
| Event Streaming Throughput | >1000 events/sec | Kafka metrics |
| Frontend Page Load Time | <3s | Lighthouse score |
| Test Coverage (Backend) | >80% | JaCoCo report |
| Test Coverage (Frontend) | >70% | Vitest coverage |

### User Experience Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Login Success Rate | >99% | Application logs |
| Session Timeout Compliance | 100% | HIPAA audit |
| Real-Time Update Delay | <2s | User perception |
| Dashboard Load Time | <5s | User perception |
| Mobile Responsiveness | 100% | Material-UI responsive design |

### HIPAA Compliance Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| PHI Cache TTL | ≤5 minutes | Redis config |
| Audit Log Coverage | 100% | Backend audit records |
| Session Timeout | 15 minutes idle | Frontend implementation |
| HTTPS/WSS Enforcement | 100% in production | Configuration |
| Multi-Tenant Isolation | Zero cross-tenant access | Integration tests |

---

## Timeline Summary

| Phase | Duration | Key Deliverables |
|-------|----------|-----------------|
| Phase 1: Core Services Startup | Week 1 | 5 critical services running healthy |
| Phase 2: Frontend API Integration | Week 2 | API service layer, authentication, proxy config |
| Phase 3: Feature Expansion | Week 3-4 | Patient, care gap, measure, workflow modules |
| Phase 4: HIPAA Compliance | Week 5 | Session timeout, audit logging, secure data handling |
| Phase 5: Testing & Deployment | Week 6 | Integration tests, Docker Compose, production deploy |

**Total Duration:** 6 weeks
**Buffer:** Add 2 weeks for unexpected issues (entity-migration fixes, schema changes, etc.)
**Estimated Completion:** March 5, 2026

---

## Next Steps

### Immediate Actions (This Week)

1. **Start Core Services (Monday):**
   ```bash
   # Pre-build validation
   ./scripts/validate-before-docker-build.sh

   # Download dependencies locally
   cd backend
   ./gradlew downloadDependencies --no-daemon

   # Build and start services ONE AT A TIME
   ./gradlew :modules:services:cql-engine-service:bootJar -x test
   docker compose build cql-engine-service
   docker compose up -d cql-engine-service

   # Verify health before proceeding
   docker compose logs -f cql-engine-service | head -50
   curl http://localhost:8081/cql-engine/actuator/health

   # Repeat for patient-service, fhir-service, care-gap-service, quality-measure-service
   ```

2. **Verify WebSocket Connection (Tuesday):**
   ```bash
   # Start frontend dev server
   cd frontend
   npm install
   npm run dev

   # Open browser: http://localhost:3000
   # Check connection status indicator in AppBar
   # Verify "CONNECTED" status appears
   ```

3. **Create API Service Layer (Wednesday):**
   - Create `frontend/src/services/api.config.ts`
   - Create `frontend/src/services/patient.service.ts`
   - Create `frontend/src/services/careGap.service.ts`
   - Create `frontend/src/services/qualityMeasure.service.ts`
   - Test API calls with sample data

4. **Implement Authentication (Thursday):**
   - Create `frontend/src/services/auth.service.ts`
   - Add JWT storage and retrieval
   - Add Authorization header interceptor
   - Create login page component
   - Test login flow with backend

5. **Add First Feature Module (Friday):**
   - Create patient management module
   - Implement patient list and detail pages
   - Test CRUD operations
   - Verify multi-tenant isolation

### Ongoing Tasks

- **Daily:** Check service health (`docker compose ps`)
- **Daily:** Monitor logs for errors (`docker compose logs -f`)
- **Weekly:** Run entity-migration validation tests
- **Weekly:** Review HIPAA compliance checklist
- **Bi-weekly:** Performance testing and optimization

---

## References

### Documentation

- **[CLAUDE.md](../CLAUDE.md)** - Quick reference guide
- **[Service Catalog](./services/SERVICE_CATALOG.md)** - All 50+ services and ports
- **[Gateway Architecture](./architecture/GATEWAY_ARCHITECTURE.md)** - Authentication flows
- **[Event Sourcing Architecture](./architecture/EVENT_SOURCING_ARCHITECTURE.md)** - CQRS pattern
- **[Database Architecture Guide](../backend/docs/DATABASE_ARCHITECTURE_GUIDE.md)** - Schema management
- **[Entity-Migration Guide](../backend/docs/ENTITY_MIGRATION_GUIDE.md)** - Validation best practices
- **[HIPAA Compliance Guide](../backend/HIPAA-CACHE-COMPLIANCE.md)** - PHI handling requirements

### Key Files

**Backend:**
- `backend/modules/gateway/gateway-service/src/main/resources/application.yml` - Gateway routing
- `backend/modules/services/*/src/main/resources/application.yml` - Service configurations
- `docker-compose.yml` - All service definitions

**Frontend:**
- `frontend/vite.config.ts` - Proxy and build configuration
- `frontend/package.json` - Dependencies and scripts
- `frontend/src/App.tsx` - Main application component
- `frontend/src/services/websocket.service.ts` - WebSocket client

---

## Appendix A: Backend Service Endpoints

### CQL Engine Service (Port 8081)

**Context Path:** `/cql-engine`

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/v1/evaluate` | Evaluate CQL expression |
| POST | `/api/v1/batch-evaluate` | Evaluate CQL for patient cohort |
| GET | `/api/v1/batches/{batchId}` | Get batch evaluation status |
| WS | `/ws/evaluation-progress` | Real-time evaluation progress |

### Patient Service (Port 8084)

**Context Path:** `/patient`

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/v1/patients?tenantId={tenantId}` | List patients |
| GET | `/api/v1/patients/{patientId}` | Get patient details |
| POST | `/api/v1/patients` | Create patient |
| PUT | `/api/v1/patients/{patientId}` | Update patient |
| DELETE | `/api/v1/patients/{patientId}` | Delete patient |

### FHIR Service (Port 8085)

**Context Path:** `/fhir`

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/Patient/{patientId}` | Get FHIR Patient resource |
| GET | `/Observation?patient={patientId}` | Get patient observations |
| GET | `/Condition?patient={patientId}` | Get patient conditions |
| POST | `/Patient` | Create FHIR Patient resource |

### Care Gap Service (Port 8086)

**Context Path:** `/care-gap`

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/v1/care-gaps?tenantId={tenantId}&status=OPEN` | List care gaps |
| GET | `/api/v1/care-gaps/{gapId}` | Get care gap details |
| POST | `/api/v1/care-gaps/{gapId}/close` | Close care gap |
| GET | `/api/v1/population-health?tenantId={tenantId}` | Population health metrics |

### Quality Measure Service (Port 8087)

**Context Path:** `/quality-measure`

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/v1/measures?tenantId={tenantId}` | List measures |
| POST | `/api/v1/measures/{measureId}/evaluate` | Evaluate measure |
| GET | `/api/v1/measures/{measureId}/results?tenantId={tenantId}` | Get results |
| GET | `/api/v1/cohort-compliance?measureCode={code}&tenantId={tenantId}` | Cohort compliance |

---

_Last Updated: January 22, 2026_
_Version: 1.0_
