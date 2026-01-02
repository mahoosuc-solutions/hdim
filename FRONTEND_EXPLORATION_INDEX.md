# Frontend Codebase Exploration Index

## Overview

This index documents the comprehensive exploration of the frontend codebase for the Health Data in Motion project, focusing on **Evaluations**, **Results**, and **Reports** features across both Angular and React implementations.

## Generated Documents

### 1. FRONTEND_IMPLEMENTATION_SUMMARY.md
**Comprehensive Technical Deep Dive (Main Document)**

Covers:
- Complete architecture overview
- Detailed component documentation
- Service layer analysis
- TypeScript type definitions
- Material UI component usage
- API endpoint mapping
- Identified gaps and recommendations

**Best for:** Understanding the full system, architecture decisions, and implementation details.

**Size:** ~200 KB, 15+ sections

### 2. FRONTEND_QUICK_REFERENCE.md
**Developer Quick Reference Guide**

Covers:
- Key file locations for each feature
- Component architecture diagrams
- Data model quick reference
- Service method signatures
- Routing information
- State management patterns
- Common tasks and solutions
- Troubleshooting guide

**Best for:** Quick lookup during development, common patterns, file navigation.

**Size:** ~50 KB, practical examples

## Feature Status Summary

### EVALUATIONS: FULLY IMPLEMENTED

**Angular Implementation:**
- Component: `/apps/clinical-portal/src/app/pages/evaluations/evaluations.component.ts`
- Fully functional form-based evaluation submission
- Measure and patient selection with autocomplete
- Real-time result display

**React Implementation:**
- Zustand store for real-time batch evaluation tracking
- WebSocket integration for live progress updates
- Event streaming and filtering

**Status:** READY FOR PRODUCTION

---

### RESULTS: FULLY IMPLEMENTED

**Angular Implementation:**
- Component: `/apps/clinical-portal/src/app/pages/results/results.component.ts`
- Full CRUD operations
- Advanced filtering and sorting
- Pagination (20 results/page)
- CSV/Excel export
- Result detail viewing

**React Implementation:**
- Event stream display
- Real-time analytics
- Batch comparison

**Status:** READY FOR PRODUCTION

---

### REPORTS: PLACEHOLDER ONLY

**Angular Implementation:**
- Component: `/apps/clinical-portal/src/app/pages/reports/reports.component.ts`
- UI only: Three placeholder cards with buttons
- NO backend integration
- NO functionality implemented

**Available Backend Methods:**
- `evaluationService.getPatientReport(patientId)` - NOT USED
- `evaluationService.getPopulationReport(year)` - NOT USED

**Status:** AWAITING IMPLEMENTATION

**Required Work:**
- Create report detail components
- Implement form for report generation
- Add date range selection
- Integrate with backend APIs
- Add export functionality (PDF, Excel)
- Implement report scheduling

---

## Directory Structure

```
Health Data in Motion/
├── apps/clinical-portal/              # Angular Application
│   ├── src/app/
│   │   ├── pages/
│   │   │   ├── evaluations/          # FULLY IMPLEMENTED
│   │   │   ├── results/              # FULLY IMPLEMENTED
│   │   │   ├── reports/              # PLACEHOLDER
│   │   │   ├── dashboard/
│   │   │   ├── patients/
│   │   │   └── visualization/
│   │   ├── services/
│   │   │   ├── evaluation.service.ts
│   │   │   ├── measure.service.ts
│   │   │   └── patient.service.ts
│   │   ├── models/
│   │   │   ├── evaluation.model.ts
│   │   │   ├── quality-result.model.ts
│   │   │   ├── cql-library.model.ts
│   │   │   └── patient.model.ts
│   │   ├── interceptors/
│   │   └── app.routes.ts
│   ├── jest.config.ts
│   └── playwright.config.ts
│
├── frontend/                          # React Application
│   ├── src/
│   │   ├── App.tsx
│   │   ├── components/               # 20+ visualization components
│   │   ├── services/
│   │   │   ├── websocket.service.ts
│   │   │   └── export.service.ts
│   │   ├── store/
│   │   │   ├── evaluationStore.ts    # Zustand
│   │   │   └── uiStore.ts            # Zustand
│   │   ├── hooks/
│   │   ├── types/
│   │   │   └── events.ts
│   │   ├── main.tsx
│   │   └── index.css
│   ├── vite.config.ts
│   └── package.json
│
└── FRONTEND_EXPLORATION_INDEX.md (this file)
    FRONTEND_IMPLEMENTATION_SUMMARY.md (detailed docs)
    FRONTEND_QUICK_REFERENCE.md (quick lookup)
```

## Key APIs at a Glance

### CQL Engine Service (Port 8081)
```
GET    /api/v1/cql/libraries/active               → MeasureInfo[]
POST   /api/v1/cql/evaluations                    → CqlEvaluation
POST   /api/v1/cql/evaluations/batch              → BatchEvaluationResponse
WS     /ws/evaluation-progress                    → Real-time events
```

### Quality Measure Service (Port 8082)
```
POST   /quality-measure/calculate                 → QualityMeasureResult
GET    /quality-measure/results                   → QualityMeasureResult[]
GET    /quality-measure/report/patient            → QualityReport
GET    /quality-measure/report/population         → PopulationQualityReport
```

### FHIR Server (Port 8083)
```
GET    /Patient                                   → Bundle<Patient>
GET    /Patient/{id}                              → Patient
GET    /Patient?name={query}                      → Bundle<Patient>
GET    /Patient?identifier={mrn}                  → Bundle<Patient>
```

## Component Dependencies Map

### Evaluations Flow
```
EvaluationsComponent
  ├── MeasureService.getActiveMeasuresInfo()
  ├── PatientService.getPatientsSummary()
  └── EvaluationService.calculateQualityMeasure()
      └── QualityMeasureResult display
```

### Results Flow
```
ResultsComponent
  ├── EvaluationService.getPatientResults()
  │   └── QualityMeasureResult[]
  ├── Client-side filtering/sorting
  ├── Pagination logic
  └── Export (CSV)
```

### Reports Flow (NOT IMPLEMENTED)
```
ReportsComponent
  └── [Placeholder cards only]
      ├── getPatientReport()        [Available but unused]
      └── getPopulationReport()     [Available but unused]
```

## Technology Stack

### Angular Clinical Portal
- **Framework:** Angular 17+ (Standalone Components)
- **UI Library:** Material Design (Material Angular)
- **State Management:** Component-level state + RxJS
- **Forms:** Reactive Forms (FormBuilder)
- **HTTP:** HttpClient with interceptors
- **Testing:** Jest + Jasmine
- **E2E:** Playwright

### React Frontend
- **Framework:** React 18+
- **Build Tool:** Vite
- **UI Library:** Material-UI v5
- **State Management:** Zustand
- **HTTP:** Fetch API
- **Charts:** Recharts
- **Real-time:** WebSocket
- **Testing:** Vitest + React Testing Library

## Data Flow Diagrams

### Evaluation Submission Flow
```
User Input → FormBuilder → EvaluationService → Quality Measure Service → QualityMeasureResult → Component Display
```

### Results Loading & Filtering
```
Component Init → EvaluationService.getPatientResults() → In-Memory Array → Filter/Sort/Paginate → UI Display
```

### Real-time Batch Monitoring (React)
```
WebSocket Connect → Event Stream → evaluationStore (Zustand) → Component Subscriptions → UI Updates
```

## Integration Points

### Backend Services Used
1. **CQL Engine Service** (Port 8081)
   - Measure/library management
   - Evaluation execution
   - Real-time progress streaming

2. **Quality Measure Service** (Port 8082)
   - Quality measure calculation
   - Result persistence
   - Report generation

3. **FHIR Server** (Port 8083)
   - Patient data retrieval
   - FHIR compliance

## Known Limitations & TODOs

### Current Limitations
- Reports feature is placeholder only
- Client-side pagination (no server-side pagination for large datasets)
- No evaluation scheduling/retry mechanism
- No batch evaluation UI form
- Limited report customization

### Performance Notes
- VirtualizedEventList handles large event streams efficiently
- Results pagination set to 20 items/page
- Patient autocomplete limited to 10 suggestions
- Recent events limited to 100 in memory

## Testing Coverage

### Angular Services
- evaluation.service.spec.ts: Complete HTTP mock tests
- measure.service.spec.ts: API endpoint tests
- patient.service.spec.ts: FHIR parsing tests

### Angular Components
- evaluations.component.spec.ts: Form submission, validation
- results.component.spec.ts: Filtering, sorting, export
- dashboard.component.spec.ts: Data visualization

### React Components
- VirtualizedEventList tests
- WebSocket integration tests
- Store selector tests

## Getting Started with Each Feature

### For EVALUATIONS Development
1. Read: FRONTEND_QUICK_REFERENCE.md → EVALUATIONS section
2. Check: `/apps/clinical-portal/src/app/pages/evaluations/`
3. Understand: EvaluationService and MeasureService
4. Test: Run evaluations.component.spec.ts

### For RESULTS Development
1. Read: FRONTEND_QUICK_REFERENCE.md → RESULTS section
2. Check: `/apps/clinical-portal/src/app/pages/results/`
3. Understand: Filtering, sorting, pagination logic
4. Test: Run results.component.spec.ts

### For REPORTS Development
1. Read: FRONTEND_QUICK_REFERENCE.md → REPORTS section
2. Check: `/apps/clinical-portal/src/app/pages/reports/`
3. Read: Quality result and report models
4. Implement: Report detail components
5. Integrate: Backend APIs (getPatientReport, getPopulationReport)

## Version Information

- **Last Updated:** November 14, 2025
- **Angular Version:** 17+ (Standalone Components)
- **React Version:** 18+
- **Material Design:** v14+
- **Material-UI:** v5+

## Quick Links to Key Files

### Essential Reading Order
1. This file (FRONTEND_EXPLORATION_INDEX.md)
2. FRONTEND_QUICK_REFERENCE.md (15 min read)
3. FRONTEND_IMPLEMENTATION_SUMMARY.md (30 min read)

### Most Important Files
- `/apps/clinical-portal/src/app/pages/evaluations/evaluations.component.ts` - Evaluation submission
- `/apps/clinical-portal/src/app/pages/results/results.component.ts` - Results management
- `/apps/clinical-portal/src/app/services/evaluation.service.ts` - Backend communication
- `/frontend/src/store/evaluationStore.ts` - Real-time state
- `/apps/clinical-portal/src/app/models/quality-result.model.ts` - Data models

### Key Type Definitions
- `QualityMeasureResult` - Main result data structure
- `QualityReport` - Patient-level reports
- `PopulationQualityReport` - Practice-wide reports
- `BatchProgressEvent` - Real-time batch progress
- `PatientSummary` - Simplified patient for UI

## Notes for New Developers

1. **Two separate UIs exist** - Angular for business features, React for monitoring
2. **Material Design is consistent** across both implementations
3. **FHIR integration** is clean and well-abstracted in PatientService
4. **WebSocket** only implemented in React frontend (not in Angular app)
5. **State management differs** - Angular uses component state, React uses Zustand
6. **Reports feature** needs development work before production use

## Support & Questions

For questions about specific implementations, refer to:
- Component: See specific component.ts and component.spec.ts files
- Service: See service.ts and service.spec.ts files
- Types: See models/ directory
- API: See api.config.ts and endpoint builder functions

---

**Document Generated:** 2025-11-14
**Codebase State:** Current as of latest git status
**Completeness:** Evaluations & Results 100%, Reports 0%

