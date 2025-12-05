# Clinical Portal - Design Specification

**Version**: 1.0
**Date**: November 13, 2025
**Status**: Design Phase

---

## Overview

The Clinical Portal is a web application for healthcare providers to manage patients, submit quality measure evaluations, and track clinical quality metrics.

---

## User Personas

### Primary Users
- **Clinicians** - Physicians, nurse practitioners who evaluate patient quality measures
- **Clinical Staff** - Medical assistants who manage patient data
- **Practice Managers** - View quality metrics and compliance reports

---

## Core Features

### 1. Dashboard (Landing Page)
**Route**: `/dashboard`

**Purpose**: Provide at-a-glance view of practice performance

**Metrics Displayed**:
- Total Patients in System
- Pending Evaluations
- Recent Evaluation Results (Success Rate)
- Compliance Rate by Measure
- Recent Activity Timeline

**Components**:
- Summary Cards (4 key metrics)
- Recent Evaluations Table (last 10)
- Quality Measures Chart (compliance by measure type)
- Quick Actions Panel (Submit New Evaluation, View Patients, etc.)

---

### 2. Patient Management
**Route**: `/patients`

**Purpose**: Browse, search, and manage patient records

**Features**:
- Patient List (paginated, virtualized for performance)
- Search by Name, MRN, Date of Birth
- Filter by Status, Last Evaluation Date
- Patient Details View (modal/side panel)
- Add New Patient (FHIR Patient resource)
- View Patient's FHIR Resources
- View Patient's Evaluation History

**Data Model** (FHIR Patient):
```json
{
  "resourceType": "Patient",
  "id": "patient-123",
  "identifier": [{ "system": "MRN", "value": "12345" }],
  "name": [{ "family": "Smith", "given": ["John"] }],
  "birthDate": "1970-01-01",
  "gender": "male"
}
```

**Table Columns**:
- Name
- MRN (Medical Record Number)
- Date of Birth
- Gender
- Last Evaluation
- Status (Active/Inactive)
- Actions (View, Evaluate)

---

### 3. Quality Measure Evaluations
**Route**: `/evaluations`

**Purpose**: Submit patients for quality measure evaluation

**Features**:

#### 3.1 Single Patient Evaluation
- Select Patient (autocomplete search)
- Select Quality Measure (dropdown)
- Select Measurement Period (date range)
- Optional: Add clinical notes
- Submit for evaluation
- Real-time status updates via WebSocket

#### 3.2 Batch Evaluation
- Select Multiple Patients (checkbox selection)
- Select Quality Measure
- Select Measurement Period
- Submit batch
- Track batch progress
- View batch results

**Quality Measures Available**:
- Diabetes: HbA1c Control
- Hypertension: Blood Pressure Control
- Preventive Care: Colorectal Cancer Screening
- Preventive Care: Breast Cancer Screening
- (Additional measures from CQL Library)

**Evaluation Request Payload**:
```json
{
  "patientId": "patient-123",
  "measureId": "diabetes-hba1c-control",
  "measurementPeriod": {
    "start": "2024-01-01",
    "end": "2024-12-31"
  },
  "tenantId": "practice-001",
  "notes": "Annual evaluation"
}
```

---

### 4. Evaluation Results
**Route**: `/results`

**Purpose**: View detailed evaluation results and analytics

**Features**:

#### 4.1 Results List
- Filter by Date Range
- Filter by Measure Type
- Filter by Status (Pass/Fail)
- Sort by Date, Patient, Measure
- Export to CSV/Excel

#### 4.2 Result Details View
- Patient Information
- Measure Details
- Evaluation Outcome (Numerator/Denominator)
- Clinical Reasoning (from CQL evaluation)
- FHIR Resources Used
- Evaluation Timestamp
- Evaluator Information

#### 4.3 Results Analytics
- Success Rate Trends (line chart)
- Compliance by Measure (bar chart)
- Pass/Fail Distribution (pie chart)
- Performance vs Target (gauge)

**Result Data Model**:
```json
{
  "evaluationId": "eval-456",
  "patientId": "patient-123",
  "patientName": "John Smith",
  "measureId": "diabetes-hba1c-control",
  "measureName": "Diabetes: HbA1c Control",
  "status": "COMPLETED",
  "outcome": {
    "numerator": true,
    "denominator": true,
    "compliant": true,
    "complianceRate": 95.2
  },
  "evaluationDate": "2024-11-13T10:30:00Z",
  "measurementPeriod": {
    "start": "2024-01-01",
    "end": "2024-12-31"
  }
}
```

---

### 5. Reports
**Route**: `/reports`

**Purpose**: Generate compliance and quality reports

**Report Types**:
1. **Practice Compliance Report**
   - Overall compliance rate
   - Compliance by measure
   - Trends over time
   - Comparison to benchmarks

2. **Patient Compliance Report**
   - Individual patient quality metrics
   - Gaps in care identification
   - Recommended actions

3. **Measure-Specific Report**
   - Deep dive into specific quality measure
   - Patient-level details
   - Clinical insights

**Export Formats**: PDF, CSV, Excel

---

## Technical Architecture

### Frontend Stack
- **Framework**: Angular 19+ (Standalone Components)
- **UI Library**: Angular Material
- **State Management**: Signals + RxJS
- **Charts**: Chart.js or ApexCharts
- **HTTP Client**: Angular HttpClient
- **WebSocket**: RxStomp

### Backend Integration

#### API Endpoints

**CQL Engine Service** (http://localhost:8081/cql-engine)
```
POST   /api/v1/evaluation/submit          - Submit single evaluation
POST   /api/v1/evaluation/batch            - Submit batch evaluation
GET    /api/v1/evaluation/{id}             - Get evaluation result
GET    /api/v1/evaluation                  - List evaluations
WS     /ws/evaluation-progress             - Real-time updates
```

**Quality Measure Service** (http://localhost:8087/quality-measure)
```
GET    /api/v1/measures                    - List quality measures
GET    /api/v1/measures/{id}               - Get measure details
GET    /api/v1/cql-libraries               - List CQL libraries
```

**FHIR Mock Service** (http://localhost:8083)
```
GET    /Patient                            - List patients
GET    /Patient/{id}                       - Get patient
POST   /Patient                            - Create patient
GET    /Observation?patient={id}           - Get patient observations
GET    /Condition?patient={id}             - Get patient conditions
```

### WebSocket Integration
- Connect to `/ws/evaluation-progress` on CQL Engine
- Subscribe to evaluation updates
- Display real-time progress in UI
- Show toast notifications on completion

---

## UI/UX Design

### Layout Structure
```
┌─────────────────────────────────────────────────────┐
│ [App Bar]                                           │
│  🏥 Clinical Portal | Practice: Main St Clinic     │
│  [Dashboard] [Patients] [Evaluations] [Results]    │
│                              [Profile] [Help] [🔔]  │
└─────────────────────────────────────────────────────┘
│                                                     │
│  [Main Content Area]                               │
│  - Dashboard / Patients / Evaluations / Results    │
│                                                     │
│                                                     │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### Color Scheme
- **Primary**: Blue (#1976D2) - Medical/Clinical theme
- **Accent**: Teal (#00BCD4) - Positive actions
- **Success**: Green (#4CAF50) - Compliant/Pass
- **Warning**: Orange (#FF9800) - Needs attention
- **Error**: Red (#F44336) - Non-compliant/Fail
- **Background**: Light Gray (#F5F5F5)

### Typography
- **Headings**: Roboto Bold
- **Body**: Roboto Regular
- **Code/Data**: Roboto Mono

---

## Data Services

### PatientService
```typescript
class PatientService {
  getPatients(params?: SearchParams): Observable<Patient[]>
  getPatient(id: string): Observable<Patient>
  searchPatients(query: string): Observable<Patient[]>
  createPatient(patient: Patient): Observable<Patient>
  updatePatient(id: string, patient: Patient): Observable<Patient>
}
```

### EvaluationService
```typescript
class EvaluationService {
  submitEvaluation(request: EvaluationRequest): Observable<Evaluation>
  submitBatchEvaluation(requests: EvaluationRequest[]): Observable<BatchResult>
  getEvaluation(id: string): Observable<Evaluation>
  listEvaluations(params?: FilterParams): Observable<Evaluation[]>
  connectWebSocket(): Observable<EvaluationEvent>
}
```

### MeasureService
```typescript
class MeasureService {
  getMeasures(): Observable<QualityMeasure[]>
  getMeasure(id: string): Observable<QualityMeasure>
  getCqlLibraries(): Observable<CqlLibrary[]>
}
```

---

## State Management

### Dashboard State
- practiceMetrics: PracticeMetrics
- recentEvaluations: Evaluation[]
- loading: boolean

### Patient State
- patients: Patient[]
- selectedPatient: Patient | null
- searchQuery: string
- filters: PatientFilters
- totalCount: number
- currentPage: number

### Evaluation State
- activeEvaluations: Evaluation[]
- selectedMeasure: QualityMeasure | null
- batchProgress: BatchProgress | null
- wsConnected: boolean

---

## Performance Considerations

1. **Virtualized Lists**: Use CDK Virtual Scroll for large patient lists
2. **Lazy Loading**: Load routes on demand
3. **Pagination**: Server-side pagination for large datasets
4. **Caching**: Cache quality measures and patient data (5 min TTL)
5. **Debouncing**: 300ms debounce on search inputs
6. **WebSocket**: Efficient real-time updates without polling

---

## Security

1. **Authentication**: JWT tokens from backend
2. **Authorization**: Role-based access (CLINICIAN, STAFF, MANAGER)
3. **HIPAA Compliance**:
   - No PHI in logs
   - Secure transmission (HTTPS/WSS)
   - Session timeout (15 minutes)
   - Audit logging on all patient access

---

## Accessibility

- **WCAG 2.1 AA Compliance**
- Keyboard navigation support
- Screen reader friendly
- High contrast mode
- Focus indicators
- ARIA labels on all interactive elements

---

## Development Phases

### Phase 1: Core Setup (Current)
- [x] Project scaffolding
- [ ] Angular Material integration
- [ ] Core layout and navigation
- [ ] HTTP services setup
- [ ] WebSocket integration

### Phase 2: Dashboard & Patients
- [ ] Dashboard with practice metrics
- [ ] Patient list with search/filter
- [ ] Patient details view
- [ ] Patient creation form

### Phase 3: Evaluations
- [ ] Single patient evaluation form
- [ ] Batch evaluation interface
- [ ] Real-time progress tracking
- [ ] WebSocket status updates

### Phase 4: Results & Analytics
- [ ] Results list with filtering
- [ ] Result details view
- [ ] Analytics dashboard
- [ ] Charts and visualizations

### Phase 5: Reports & Export
- [ ] Report generation
- [ ] Export to CSV/Excel/PDF
- [ ] Scheduled reports

### Phase 6: Polish & Testing
- [ ] E2E tests
- [ ] Accessibility audit
- [ ] Performance optimization
- [ ] User acceptance testing

---

## Success Metrics

- Page load time < 2 seconds
- Patient search response < 500ms
- Evaluation submission response < 1 second
- WebSocket connection stable > 99.9%
- Zero PHI leaks in logs
- WCAG 2.1 AA compliant

---

## Next Steps

1. Install Angular Material
2. Create core layout components
3. Set up routing
4. Implement HTTP services
5. Build dashboard page
6. Implement patient management

---

## Notes

- Integration with existing backend services (CQL Engine, Quality Measure)
- Reuse WebSocket patterns from React dashboard
- Follow FHIR R4 standards for patient data
- Support multi-tenancy (practice-level isolation)
