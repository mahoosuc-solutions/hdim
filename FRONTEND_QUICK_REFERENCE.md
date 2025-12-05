# Frontend Quick Reference Guide

## Quick Navigation

### Key Files for Each Feature

#### EVALUATIONS
- **Angular Component:** `/apps/clinical-portal/src/app/pages/evaluations/evaluations.component.ts`
- **Service:** `/apps/clinical-portal/src/app/services/evaluation.service.ts`
- **Service:** `/apps/clinical-portal/src/app/services/measure.service.ts`
- **Models:** `/apps/clinical-portal/src/app/models/evaluation.model.ts`

**What it does:**
- Users select a measure and patient
- Submits evaluation request to Quality Measure Service
- Displays result with compliance status

**Related API:**
- POST `/quality-measure/calculate?patient={id}&measure={id}`
- GET `/api/v1/cql/libraries/active`
- GET `/Patient`

---

#### RESULTS
- **Angular Component:** `/apps/clinical-portal/src/app/pages/results/results.component.ts`
- **Service:** `/apps/clinical-portal/src/app/services/evaluation.service.ts`
- **Model:** `/apps/clinical-portal/src/app/models/quality-result.model.ts`

**What it does:**
- Load and display all quality measure results
- Filter by date, measure type, compliance status
- Sort and paginate results
- Export to CSV
- View result details

**Related API:**
- GET `/quality-measure/results?patient=`
- GET `/quality-measure/report/patient?patient={id}`

---

#### REPORTS
- **Angular Component:** `/apps/clinical-portal/src/app/pages/reports/reports.component.ts`
- **Service:** `/apps/clinical-portal/src/app/services/evaluation.service.ts`
- **Models:** `/apps/clinical-portal/src/app/models/quality-result.model.ts`

**Current Status:** PLACEHOLDER - No backend integration yet

**Available Methods (not yet used in UI):**
- `evaluationService.getPatientReport(patientId)`
- `evaluationService.getPopulationReport(year)`

**Related API:**
- GET `/quality-measure/report/patient?patient={id}`
- GET `/quality-measure/report/population?year={year}`

---

## Component Architecture

### Angular Clinical Portal
```
EvaluationsComponent
├── FormBuilder (measure + patient search)
├── MeasureService (load measures)
├── PatientService (load and search patients)
└── EvaluationService (submit evaluation)

ResultsComponent
├── FormBuilder (filters)
├── EvaluationService (load results)
├── Client-side filtering, sorting, pagination
└── Export functionality

ReportsComponent
└── [PLACEHOLDER - No implementation]
```

### React Frontend
```
App.tsx
├── useEvaluationStore (Zustand)
├── useWebSocket (WebSocket connection)
├── VirtualizedEventList (real-time events)
├── PerformanceMetricsPanel
├── AnalyticsPanel
├── BatchComparisonView
└── Modal dialogs for details/export
```

---

## Data Models

### QualityMeasureResult
```typescript
{
  id: string;                        // UUID
  patientId: string;
  measureId: string;                 // e.g., "CDC-A1C9"
  measureName: string;
  measureCategory: 'HEDIS'|'CMS'|'CUSTOM';
  numeratorCompliant: boolean;       // Meets criteria
  denominatorEligible: boolean;      // Is eligible
  complianceRate: number;            // 0-100%
  score: number;
  calculationDate: string;           // ISO date
  createdAt: string;
  createdBy: string;
}
```

### MeasureInfo (for dropdowns)
```typescript
{
  id: string;
  name: string;
  version: string;
  description?: string;
  category?: string;                 // HEDIS, CMS, CUSTOM
  displayName: string;               // For UI display
}
```

### PatientSummary (for search/display)
```typescript
{
  id: string;
  mrn?: string;
  fullName: string;
  firstName?: string;
  lastName?: string;
  dateOfBirth?: string;
  age?: number;
  gender?: string;
  status: 'Active'|'Inactive';
}
```

---

## Service Methods Quick Reference

### EvaluationService

**For Evaluations:**
```typescript
calculateQualityMeasure(patientId, measureId)
  // POST /quality-measure/calculate?patient={id}&measure={id}
```

**For Results:**
```typescript
getPatientResults(patientId)
  // GET /quality-measure/results?patient={patientId}

getQualityScore(patientId)
  // GET /quality-measure/score?patient={patientId}
```

**For Reports (Not yet implemented in UI):**
```typescript
getPatientReport(patientId)
  // GET /quality-measure/report/patient?patient={patientId}
  // Returns: QualityReport with gapsInCare array

getPopulationReport(year)
  // GET /quality-measure/report/population?year={year}
  // Returns: PopulationQualityReport
```

### MeasureService

```typescript
getActiveMeasuresInfo()
  // GET /api/v1/cql/libraries/active
  // Returns: MeasureInfo[] for dropdowns

getMeasureById(id)
getLatestVersion(name)
searchMeasures(query)
```

### PatientService

```typescript
getPatientsSummary()
  // GET /Patient?_count=100
  // Returns: PatientSummary[] for autocomplete

searchPatientsByName(name)
searchPatientsByIdentifier(mrn)
searchPatients(params)
```

---

## Routing

### Clinical Portal Routes
```
localhost:4200/dashboard        - Overview page
localhost:4200/evaluations      - Run evaluations
localhost:4200/results          - View results
localhost:4200/reports          - Reports (placeholder)
localhost:4200/patients         - Patient management
localhost:4200/visualization    - 3D visualizations
```

### React Frontend
```
localhost:5173/                 - Single page app
All navigation via modals and buttons
Keyboard shortcuts: Ctrl+? for help
```

---

## State Management

### Angular
- Component-level state with @Component() decorators
- RxJS Observables for async operations
- FormBuilder for form state

### React
- **Zustand store:** `useEvaluationStore` for evaluation events/batches
- **Zustand store:** `useUIStore` for UI state (panel visibility, toasts)
- **React hooks:** `useState` for local component state
- **Custom hooks:** `useWebSocket`, `useDarkMode`, `useSettings`, `useNotifications`

---

## API Server Endpoints

### CQL Engine Service (port 8081)
```
GET    /api/v1/cql/libraries/active
POST   /api/v1/cql/evaluations?libraryId={uuid}&patientId={id}
POST   /api/v1/cql/evaluations/batch?libraryId={uuid}
GET    /api/v1/cql/evaluations/patient/{patientId}

WebSocket: /ws/evaluation-progress?tenantId={tenantId}
```

### Quality Measure Service (port 8082)
```
POST   /quality-measure/calculate?patient={id}&measure={id}
GET    /quality-measure/results?patient={patientId}
GET    /quality-measure/score?patient={patientId}
GET    /quality-measure/report/patient?patient={patientId}
GET    /quality-measure/report/population?year={year}
```

### FHIR Server (port 8083)
```
GET    /Patient
GET    /Patient/{id}
GET    /Patient?name={name}
GET    /Patient?identifier={mrn}
GET    /Patient?birthdate={date}
```

---

## Common Tasks

### Add a new column to Results table
1. Edit ResultsComponent.displayedColumns array
2. Add data binding in template
3. Add sort logic in sortBy() method

### Add a new filter to Results
1. Add form control to filterForm in constructor
2. Add filter logic in applyFilters() method
3. Add UI element in template

### Add a new evaluation status
1. Update EvaluationStatus type in evaluation.model.ts
2. Update getStatusClass() and getStatusText() methods
3. Update filtering logic if needed

### Call a new backend API
1. Add method to appropriate service (evaluation/measure/patient)
2. Build URL using buildApiUrl() helper
3. Use http.get/post/put/delete
4. Handle Observable with rxjs operators
5. Call from component and handle response

### Export new data format
1. Extend generateCSVData() or exportToExcel() methods
2. Map QualityMeasureResult to desired format
3. Create blob and trigger download
4. Update filename

---

## Testing Files

### Angular Tests
```
/apps/clinical-portal/src/app/pages/evaluations/evaluations.component.spec.ts
/apps/clinical-portal/src/app/pages/results/results.component.spec.ts
/apps/clinical-portal/src/app/services/evaluation.service.spec.ts
/apps/clinical-portal/src/app/services/measure.service.spec.ts
```

### React Tests
```
/frontend/src/components/__tests__/
/frontend/src/services/__tests__/
/frontend/src/hooks/__tests__/
```

---

## Common Errors & Solutions

### "No measures found"
- Check if CQL Engine service is running on port 8081
- Check API_CONFIG in api.config.ts

### "No patients found"
- Check if FHIR server is running on port 8083
- Check Patient resource endpoint

### "Evaluation failed"
- Check Quality Measure Service on port 8082
- Check patient data exists in FHIR
- Check measure/library exists and is active

### WebSocket connection fails
- Check if CQL Engine service is running
- Check tenantId is being passed correctly
- Check firewall allows WebSocket connections

---

## Performance Considerations

### Results Component
- Pagination: 20 results per page (configurable)
- Client-side filtering/sorting (no backend pagination yet)
- Consider adding server-side pagination for large datasets

### Evaluations Component
- Measure list cached (doesn't reload on every visit)
- Patient autocomplete limits results to 10
- Debounce patient search if needed

### React Frontend
- VirtualizedEventList for performance with large event lists
- Memoized selectors in evaluationStore
- Event limit of 100 most recent events

---

## Browser Support

### Angular (Clinical Portal)
- Modern browsers (Chrome, Firefox, Safari, Edge)
- Requires ES2020+ support
- Material Design components

### React (Frontend)
- Modern browsers with WebSocket support
- Vite for fast HMR development
- Material-UI v5+

---

## Build & Deploy

### Angular
```bash
npm run build                    # Production build
npm run serve                    # Development server
```

### React
```bash
npm run dev                      # Development server
npm run build                    # Production build
npm run preview                  # Preview production build
```

---

## Documentation Files

Generated summary: `/FRONTEND_IMPLEMENTATION_SUMMARY.md`
This quick reference: `/FRONTEND_QUICK_REFERENCE.md`

For detailed information, see the full summary.

