# Frontend Codebase Exploration: Evaluations, Results, and Reports

## Executive Summary

The codebase contains **two separate frontend implementations**:

1. **Clinical Portal** (Angular/TypeScript) - The primary UI with full feature implementation
2. **Frontend** (React with Vite) - A real-time evaluation dashboard focused on WebSocket events

Both applications work together but serve different purposes. Here's a comprehensive breakdown.

---

## Architecture Overview

```
Health Data in Motion
├── apps/clinical-portal/           (Angular - Main Clinical Application)
│   ├── src/app/
│   │   ├── pages/
│   │   │   ├── dashboard/          (Overview and KPIs)
│   │   │   ├── evaluations/        (Run quality measure evaluations)
│   │   │   ├── results/            (View historical evaluation results)
│   │   │   ├── reports/            (Generate compliance reports)
│   │   │   ├── patients/           (Patient management)
│   │   │   └── visualization/      (3D visualizations with Three.js)
│   │   ├── services/
│   │   │   ├── evaluation.service.ts
│   │   │   ├── measure.service.ts
│   │   │   └── patient.service.ts
│   │   ├── models/
│   │   │   ├── evaluation.model.ts
│   │   │   ├── quality-result.model.ts
│   │   │   ├── cql-library.model.ts
│   │   │   └── patient.model.ts
│   │   └── app.routes.ts
│
└── frontend/                        (React - Real-time Dashboard)
    ├── src/
    │   ├── App.tsx                  (Main dashboard component)
    │   ├── components/              (All visualization components)
    │   ├── services/
    │   │   ├── websocket.service.ts
    │   │   └── export.service.ts
    │   ├── store/
    │   │   ├── evaluationStore.ts   (Zustand state management)
    │   │   └── uiStore.ts
    │   ├── types/events.ts          (Event type definitions)
    │   └── hooks/
    │       ├── useWebSocket.ts
    │       ├── useDarkMode.ts
    │       └── useSettings.ts
```

---

## 1. EVALUATIONS

### Clinical Portal (Angular)

**Component:** `/apps/clinical-portal/src/app/pages/evaluations/evaluations.component.ts`

**Purpose:** Allows clinicians to submit quality measure evaluations for individual patients

**Key Features:**
- Measure selection dropdown (loaded from CQL Engine)
- Patient autocomplete search
- Form validation
- Real-time evaluation submission
- Result display with compliance status

**State Management:**
```typescript
// Component state
evaluationForm: FormGroup;
measures: MeasureInfo[] = [];
patients: PatientSummary[] = [];
selectedPatient: PatientSummary | null = null;
evaluationResult: QualityMeasureResult | null = null;
submitting = false;
```

**Data Flow:**
1. Load active measures from CQL Engine Service
2. Load patients from FHIR server
3. User selects measure and searches for patient
4. Submit evaluation via EvaluationService.calculateQualityMeasure()
5. Display result with compliance status

**Material UI Components Used:**
- MatFormFieldModule, MatInputModule
- MatSelectModule, MatAutocompleteModule
- MatProgressSpinnerModule
- MatCardModule, MatButtonModule

**API Endpoints:**
- GET `/api/v1/cql/libraries/active` - Load measures
- GET `/Patient` - Load patients
- POST `/quality-measure/calculate?patient={id}&measure={id}` - Calculate measure

### Frontend (React)

**Store:** `/frontend/src/store/evaluationStore.ts`

**Purpose:** Real-time state management for batch evaluations via WebSocket

**Key Features:**
- Batch progress tracking
- Event filtering and history
- Compliance rate calculations
- Connection status management

**Zustand Store State:**
```typescript
interface EvaluationState {
  connectionStatus: ConnectionStatus;
  batchProgress: Map<string, BatchProgressEvent>;
  activeBatchId: string | null;
  recentEvents: AnyEvaluationEvent[];
  totalEvaluationsCompleted: number;
  totalEvaluationsFailed: number;
  averageComplianceRate: number;
  eventFilters: EventFilters;
}
```

**Event Types (from WebSocket):**
- `EVALUATION_STARTED`
- `EVALUATION_COMPLETED`
- `EVALUATION_FAILED`
- `BATCH_STARTED`
- `BATCH_PROGRESS`
- `BATCH_COMPLETED`

---

## 2. RESULTS

### Clinical Portal (Angular)

**Component:** `/apps/clinical-portal/src/app/pages/results/results.component.ts`

**Purpose:** Display and analyze historical quality measure evaluation results

**Key Features:**
- Paginated results table (20 per page)
- Advanced filtering by date range, measure type, and compliance status
- Sorting by multiple columns
- CSV/Excel export
- Results detail panel
- Compliance statistics (compliant, non-compliant, not eligible)
- Results grouped by measure category

**State Management:**
```typescript
filterForm: FormGroup;
results: QualityMeasureResult[] = [];
filteredResults: QualityMeasureResult[] = [];
currentPage = 0;
pageSize = 20;
totalResults = 0;
selectedResult: QualityMeasureResult | null = null;
showDetailsPanel = false;
currentSortColumn: string | null = null;
currentSortDirection: 'asc' | 'desc' | null = null;
```

**Display Columns:**
- calculationDate
- patientId
- measureName
- measureCategory
- outcome (compliance status)
- complianceRate
- actions (view details, export)

**Data Model - QualityMeasureResult:**
```typescript
interface QualityMeasureResult {
  id: string;                      // UUID
  tenantId: string;
  patientId: string;
  measureId: string;               // e.g., "CDC-A1C9"
  measureName: string;
  measureCategory: 'HEDIS' | 'CMS' | 'CUSTOM';
  measureYear: number;
  numeratorCompliant: boolean;     // Patient meets criteria
  denominatorEligible: boolean;    // Patient is eligible
  complianceRate: number;          // 0.0 to 100.0
  score: number;
  calculationDate: string;         // ISO date
  cqlLibrary?: string;
  createdAt: string;
  createdBy: string;
  version: number;
}
```

**Methods:**
- `loadResults()` - Load all results via EvaluationService.getPatientResults('')
- `applyFilters()` - Filter by date, measure, status
- `sortBy(column, direction)` - Client-side sorting
- `getPaginatedResults()` - Get current page data
- `getComplianceStats()` - Calculate statistics
- `groupByMeasureType()` - Group results by category
- `exportToCSV()` / `exportToExcel()` - Export functionality

**API Endpoints:**
- GET `/quality-measure/results?patient=` - Get all results
- GET `/Patient/{id}` - Get patient details for result
- POST `/quality-measure/report/patient?patient={id}` - Get detailed patient report

### Frontend (React)

**The React frontend does NOT have a dedicated Results page.** Instead, it displays real-time evaluation results through:

1. **VirtualizedEventList** component - Shows completed evaluations
2. **Event details modal** - Displays individual evaluation results
3. **Analytics panel** - Shows aggregated compliance metrics
4. **Batch comparison view** - Compares results across batches

The data comes from WebSocket events (`EVALUATION_COMPLETED` events).

---

## 3. REPORTS

### Clinical Portal (Angular)

**Component:** `/apps/clinical-portal/src/app/pages/reports/reports.component.ts`

**Current Status:** PLACEHOLDER IMPLEMENTATION ONLY

**Purpose:** Generate and view compliance reports at practice and population levels

**Current UI (Placeholder):**
```
Reports Page
├── Practice Compliance Report
│   └── "View Report" button
├── Patient Compliance Report
│   └── "View Report" button
└── Measure-Specific Report
    └── "View Report" button
```

**IMPORTANT:** This component currently has no backend integration. It's a card-based UI with placeholder buttons.

**Data Models (Defined but not used in UI yet):**

```typescript
// Patient Quality Report
interface QualityReport {
  patientId: string;
  patientName?: string;
  reportDate: string;
  overallScore: QualityScore;
  measureResults: QualityMeasureResult[];
  gapsInCare: GapInCare[];
}

// Population Quality Report
interface PopulationQualityReport {
  year: number;
  totalPatients: number;
  reportDate: string;
  overallCompliance: number;
  measureSummaries: MeasureSummary[];
}

// Gap in Care
interface GapInCare {
  measureId: string;
  measureName: string;
  description: string;
  recommendation: string;
}
```

**Available EvaluationService Methods for Reports:**
- `getPatientReport(patientId)` - Get patient-specific report
- `getPopulationReport(year)` - Get practice-wide report
- `checkHealth()` - Check Quality Measure Service status

**API Endpoints:**
- GET `/quality-measure/report/patient?patient={patientId}`
- GET `/quality-measure/report/population?year={year}`

### Frontend (React)

**No reports implementation** in React frontend. This is future work.

---

## 4. SUPPORTING SERVICES

### Angular Services

#### EvaluationService
**File:** `/apps/clinical-portal/src/app/services/evaluation.service.ts`

**Purpose:** Bridge between UI and backend microservices

**CQL Engine Endpoints:**
```typescript
// Single evaluation
submitEvaluation(libraryId, patientId, contextData)
// POST /api/v1/cql/evaluations?libraryId={uuid}&patientId={id}

// Batch evaluation
batchEvaluate(libraryId, patientIds)
// POST /api/v1/cql/evaluations/batch?libraryId={uuid}

// Get evaluations
getPatientEvaluations(patientId)
getLibraryEvaluations(libraryId)
getAllEvaluations(page, size)
```

**Quality Measure Service Endpoints:**
```typescript
// Calculate measure
calculateQualityMeasure(patientId, measureId)
// POST /quality-measure/calculate?patient={id}&measure={id}

// Get results
getPatientResults(patientId)
// GET /quality-measure/results?patient={patientId}

// Get score
getQualityScore(patientId)
// GET /quality-measure/score?patient={patientId}

// Get reports
getPatientReport(patientId)
// GET /quality-measure/report/patient?patient={patientId}

getPopulationReport(year)
// GET /quality-measure/report/population?year={year}
```

#### MeasureService
**File:** `/apps/clinical-portal/src/app/services/measure.service.ts`

**Purpose:** Manage CQL libraries/measures

**Key Methods:**
```typescript
getActiveMeasures()              // All active measures
getActiveMeasuresInfo()          // MeasureInfo objects for UI
getMeasureById(id)
getMeasureByName(name, version)
getLatestVersion(name)
getAllVersions(name)
searchMeasures(query)
```

#### PatientService
**File:** `/apps/clinical-portal/src/app/services/patient.service.ts`

**Purpose:** FHIR Patient resource operations

**Key Methods:**
```typescript
getPatients(count)
getPatient(id)
searchPatientsByName(name)
searchPatientsByIdentifier(mrn)
searchPatients(params)
getPatientsSummary()              // Simplified for UI display
toPatientSummary(patient)         // Convert FHIR to UI model
```

### React Services

#### WebSocketService
**File:** `/frontend/src/services/websocket.service.ts`

**Purpose:** Real-time connection to CQL Engine evaluation progress

**Features:**
- Automatic reconnection with exponential backoff
- Tenant-based filtering
- Event routing
- Connection state management

**Connection Parameters:**
```typescript
baseUrl: 'ws://localhost:8081/cql-engine'
endpoint: '/ws/evaluation-progress'
queryParam: ?tenantId={tenantId}
```

**Event Handlers:**
```typescript
onEvent(handler)    // Subscribe to evaluation events
onStatus(handler)   // Subscribe to connection status changes
onError(handler)    // Subscribe to error events
```

---

## 5. ROUTING & NAVIGATION

### Clinical Portal Routes
**File:** `/apps/clinical-portal/src/app/app.routes.ts`

```
/ → /dashboard (default)
├── /dashboard               (DashboardComponent)
├── /patients                (PatientsComponent)
├── /evaluations             (EvaluationsComponent)
├── /results                 (ResultsComponent)
├── /reports                 (ReportsComponent)
└── /visualization           (VisualizationLayoutComponent)
    ├── /live-monitor        (LiveBatchMonitorComponent)
    ├── /quality-constellation (QualityConstellationComponent)
    ├── /flow-network        (FlowNetworkComponent)
    └── /measure-matrix      (MeasureMatrixComponent)
```

### React Frontend
**File:** `/frontend/src/App.tsx`

Single-page application with modal-based navigation. No traditional routing. All features accessible via:
- Top app bar buttons
- Quick actions panel
- Modal dialogs
- Keyboard shortcuts (Ctrl+?)

---

## 6. TYPE DEFINITIONS

### Evaluation Types
```typescript
interface CqlEvaluation {
  id: string;
  tenantId: string;
  library?: {
    id: string;
    name: string;
    version: string;
  };
  patientId: string;
  contextData?: Record<string, any>;
  evaluationResult?: Record<string, any>;
  status: 'SUCCESS' | 'FAILED' | 'PENDING';
  errorMessage?: string;
  durationMs?: number;
  evaluationDate: string;
  createdAt: string;
}

interface EvaluationRequest {
  libraryId: string;
  patientId: string;
  contextData?: Record<string, any>;
}

interface BatchEvaluationRequest {
  libraryId: string;
  patientIds: string[];
  contextData?: Record<string, any>;
}
```

### Quality Result Types
```typescript
interface QualityMeasureResult {
  id: string;
  tenantId: string;
  patientId: string;
  measureId: string;
  measureName: string;
  measureCategory: 'HEDIS' | 'CMS' | 'CUSTOM';
  numeratorCompliant: boolean;
  denominatorEligible: boolean;
  complianceRate: number;  // 0-100
  score: number;
  calculationDate: string;
  createdAt: string;
  createdBy: string;
  version: number;
}

interface QualityScore {
  totalMeasures: number;
  compliantMeasures: number;
  scorePercentage: number;
}
```

### Event Types (WebSocket - React)
```typescript
enum EventType {
  EVALUATION_STARTED,
  EVALUATION_COMPLETED,
  EVALUATION_FAILED,
  BATCH_STARTED,
  BATCH_PROGRESS,
  BATCH_COMPLETED,
  CACHE_HIT,
  CACHE_MISS,
  TEMPLATE_LOADED
}

interface BatchProgressEvent {
  eventType: EventType.BATCH_PROGRESS;
  batchId: string;
  tenantId: string;
  measureId: string;
  measureName: string;
  totalPatients: number;
  completedCount: number;
  successCount: number;
  failedCount: number;
  percentComplete: number;     // 0-100
  avgDurationMs: number;
  currentThroughput: number;   // evals/sec
  denominatorCount: number;
  numeratorCount: number;
  cumulativeComplianceRate: number;  // 0-100
}
```

---

## 7. MATERIAL UI & COMPONENTS

### Material Angular Components Used
- **Forms:** MatFormFieldModule, MatInputModule, MatSelectModule, MatAutocompleteModule
- **Table:** MatTableModule, MatDatepickerModule
- **UI:** MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatChipsModule
- **Navigation:** Navigation component with Material Design

### React Material-UI Components
- **Layout:** AppBar, Toolbar, Container, Card, CardContent
- **Inputs:** TextField, Select
- **Display:** Table, List, Chart libraries
- **Dialogs:** Dialog, DialogTitle, DialogContent
- **Icons:** Material-UI Icons (Assessment, Settings, Help, Compare, etc.)

### Custom React Components (Frontend)
- VirtualizedEventList
- BatchSelector
- PerformanceMetricsPanel
- AnalyticsPanel
- BatchComparisonView
- MultiBatchComparison
- TrendsChart
- ComplianceGauge
- DurationHistogram
- ThroughputChart
- EventDetailsModal
- AdvancedExportDialog
- SearchBar
- ExportButton
- DarkModeToggle
- ConnectionStatus
- KeyboardShortcutsPanel
- SettingsPanel

---

## 8. API CONFIGURATION

### CQL Engine Service
**Base URL:** `http://localhost:8081`
**Context:** `/api/v1/cql`

**Endpoints:**
- Libraries: `/libraries`, `/libraries/active`, `/libraries/{id}`, `/libraries/by-name/{name}`
- Evaluations: `/evaluations`, `/evaluations/batch`, `/evaluations/patient/{id}`

### FHIR Mock Server
**Base URL:** `http://localhost:8083`

**Endpoints:**
- Patients: `/Patient`, `/Patient/{id}`
- Search: Query parameters (name, identifier, birthdate, gender)

### Quality Measure Service
**Base URL:** `http://localhost:8082`
**Context:** `/quality-measure`

**Endpoints:**
- Calculate: `/calculate?patient={id}&measure={id}`
- Results: `/results?patient={id}`
- Score: `/score?patient={id}`
- Reports: `/report/patient?patient={id}`, `/report/population?year={year}`

---

## 9. KEY OBSERVATIONS & GAPS

### Implemented Features
✓ Evaluation submission (single patients)
✓ Results viewing and filtering
✓ Patient search and selection
✓ Measure selection
✓ Real-time batch progress tracking (React)
✓ CSV/Excel export
✓ Compliance statistics
✓ Patient reports API integration

### Missing/Placeholder Features
✗ Reports UI - Currently just placeholder cards, no actual report generation
✗ Batch evaluation submission UI (available via API but no UI form)
✗ Advanced filtering options (date ranges work, but limited)
✗ Report scheduling/automation
✗ Care gap detail view
✗ Measure-specific deep dives

### Technical Considerations
1. **Two separate frontends** - Angular for business logic, React for real-time monitoring
2. **WebSocket integration** - Only in React app, not in Angular clinical portal
3. **State management** - Angular uses component state, React uses Zustand
4. **Material Design** - Consistently used across both apps
5. **FHIR compliance** - Patient service fully implements FHIR Bundle pattern

---

## 10. RECOMMENDED NEXT STEPS

### For Reports Feature
1. Create report detail components
2. Implement report generation form
3. Add date range selection
4. Implement report export (PDF, Excel)
5. Add email delivery option
6. Create report scheduling UI

### For Evaluations Feature
1. Implement batch evaluation submission form
2. Add evaluation scheduling
3. Implement evaluation history/retry
4. Add evidence detail viewer

### For Results Feature
1. Add chart/visualization for compliance trends
2. Implement result caching
3. Add result comparison tools
4. Implement care gap recommendations display

---

## File Locations Summary

### Angular Clinical Portal
- Components: `/apps/clinical-portal/src/app/pages/`
- Services: `/apps/clinical-portal/src/app/services/`
- Models: `/apps/clinical-portal/src/app/models/`
- Routes: `/apps/clinical-portal/src/app/app.routes.ts`
- Templates: `/apps/clinical-portal/src/app/pages/*/**.component.html`

### React Frontend
- Main App: `/frontend/src/App.tsx`
- Components: `/frontend/src/components/`
- Services: `/frontend/src/services/`
- Store: `/frontend/src/store/`
- Types: `/frontend/src/types/`
- Hooks: `/frontend/src/hooks/`

