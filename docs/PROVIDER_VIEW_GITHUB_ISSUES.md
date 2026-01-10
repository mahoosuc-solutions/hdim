# Provider View Implementation - GitHub Issues Backlog

## Executive Summary

This backlog defines **24 GitHub Issues** across 5 implementation tracks to enhance the Clinical Portal's provider view for **primary care physicians**. Building on the existing provider dashboard, measure builder, and care gap infrastructure, these issues will:

- **Reduce provider workflow time by 40%+** through optimized patient panel views and smart prioritization
- **Enable custom quality measure creation** with AI-assisted CQL generation and primary care templates
- **Surface actionable insights** via population health analytics and predictive care gap detection
- **Ensure HIPAA compliance** with proper PHI handling, audit logging, and multi-tenant isolation

## Existing Infrastructure (Already Implemented)

| Component | Location | Status |
|-----------|----------|--------|
| Provider Dashboard | `apps/clinical-portal/src/app/pages/dashboard/provider-dashboard/` | Partial - needs enhancement |
| Measure Builder | `apps/clinical-portal/src/app/pages/measure-builder/` | Functional - needs primary care templates |
| Care Gap Service | `apps/clinical-portal/src/app/services/care-gap.service.ts` | Functional - needs provider-specific views |
| CQL Editor Dialog | `apps/clinical-portal/src/app/pages/measure-builder/dialogs/cql-editor-dialog.component.ts` | Functional |
| Patient Service | `apps/clinical-portal/src/app/services/patient.service.ts` | Functional |

## Implementation Tracks Overview

```
Track 1: Foundation (6 issues)          → Track 2: Core Workflows (7 issues)
         ↓                                         ↓
Track 3: Custom Measure Builder (5 issues) ←──────┘
         ↓
Track 4: Insights & Analytics (4 issues)
         ↓
Track 5: Enhancement & Polish (2 issues)
```

**Critical Path**: Tracks 1-2 must complete before Track 3. Track 4 depends on Track 2. Track 5 can run in parallel after Track 2.

**Total Estimated Effort**: 12-16 weeks (6-8 sprints) with team of 3-4 developers

---

## Track 1: Foundation (Sprint 1-2)

### Issue #1: [Foundation] [Backend] Create Provider Panel Assignment API

**Priority**: High | **Estimate**: L | **Labels**: `provider-view`, `backend`, `track-1`, `hipaa-review`

**User Story**
As a **primary care provider**,
I want **my patient panel to be pre-filtered to only patients assigned to me**,
So that **I can focus on my patients without scrolling through the entire practice's population**.

**Acceptance Criteria**
```
Given a provider is logged in with valid gateway-trust headers,
When they access the provider dashboard,
Then only patients assigned to that provider are displayed in the panel.

Given a provider has panel assignment changes,
When the panel is updated by an administrator,
Then the provider dashboard reflects changes within 5 minutes (cache refresh).

Technical Requirements:
- [ ] Create `ProviderPanelController` in patient-service with endpoint `GET /api/v1/providers/{providerId}/panel`
- [ ] Implement provider-patient assignment table with Liquibase migration
- [ ] Use gateway-trust auth pattern (TrustedHeaderAuthFilter) - NOT direct JWT validation
- [ ] Filter all queries by tenantId from X-Auth-Tenant-Ids header
- [ ] Add @Audited annotation on all PHI access methods
- [ ] Cache provider panel for 5 minutes max (HIPAA compliance)
- [ ] Include Cache-Control: no-store headers on responses
```

**Technical Specifications**

**Affected Services**
- patient-service (8084): New controller and service
- gateway-service (8001): Route configuration

**API Contract**
```java
GET /api/v1/providers/{providerId}/panel
Headers:
  X-Auth-User-Id: {userId}
  X-Auth-Tenant-Ids: {tenantId}

Query Parameters:
  - page: int (default: 0)
  - size: int (default: 50)
  - riskLevel: string[] (optional: HIGH, MEDIUM, LOW)
  - hasOpenCareGaps: boolean (optional)

Response 200:
{
  "providerId": "uuid",
  "providerName": "Dr. Jane Smith, MD",
  "panelSize": 156,
  "patients": [
    {
      "id": "uuid",
      "fhirId": "Patient/123",
      "name": { "given": ["Robert"], "family": "Jackson" },
      "mrn": "MRN-301",
      "dateOfBirth": "1965-03-15",
      "riskScore": 85,
      "riskLevel": "HIGH",
      "openCareGaps": 3,
      "lastVisitDate": "2025-11-15",
      "nextScheduledVisit": "2026-01-20"
    }
  ],
  "pagination": {
    "page": 0,
    "size": 50,
    "totalElements": 156,
    "totalPages": 4
  }
}
```

**Database Changes**
```sql
-- Liquibase migration: 0051-create-provider-panel-assignment.xml
CREATE TABLE provider_panel_assignment (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id VARCHAR(50) NOT NULL,
  provider_id UUID NOT NULL,
  patient_id UUID NOT NULL,
  assigned_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  assignment_type VARCHAR(50) NOT NULL DEFAULT 'PRIMARY',
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE,
  UNIQUE(tenant_id, provider_id, patient_id)
);

CREATE INDEX idx_provider_panel_tenant_provider ON provider_panel_assignment(tenant_id, provider_id);
```

**HIPAA Compliance**
- [x] @Audited annotation on `getProviderPanel()` method
- [x] Cache TTL = 5 minutes (Redis TTL: 300 seconds)
- [x] Cache-Control: no-store, no-cache headers
- [x] Multi-tenant filtering: WHERE tenant_id = :tenantId

**Testing Requirements**
- [ ] Unit test: `ProviderPanelServiceTest` with mocked repositories
- [ ] Integration test: `ProviderPanelControllerIntegrationTest` with @SpringBootTest
- [ ] Multi-tenant isolation test: Verify 403 when accessing different tenant's panel
- [ ] RBAC test: Verify ADMIN and EVALUATOR roles can access
- [ ] Performance test: Panel load < 300ms for 200 patients

**Dependencies**
- Blocks: Issue #2, #3, #7

---

### Issue #2: [Foundation] [Frontend] Integrate Real Patient Names in Provider Dashboard

**Priority**: High | **Estimate**: M | **Labels**: `provider-view`, `frontend`, `track-1`

**User Story**
As a **provider**,
I want **to see patient names instead of "Patient {patientId}"**,
So that **I can quickly identify patients without looking up their records**.

**Acceptance Criteria**
```
Given care gaps are displayed on the provider dashboard,
When the dashboard loads,
Then patient names appear as "Last, First" format (e.g., "Jackson, Robert").

Given a patient name lookup fails,
When the dashboard displays care gaps,
Then a graceful fallback shows "Patient MRN-XXX" instead of crashing.

Technical Requirements:
- [ ] Update ProviderDashboardComponent to call new provider panel API
- [ ] Replace `Patient ${gap.patientId}` with actual patient names
- [ ] Implement patient name caching in PatientService
- [ ] Handle missing patient data gracefully
- [ ] Update unit tests to verify name display
```

**Technical Specifications**

**Affected Files**
- `apps/clinical-portal/src/app/pages/dashboard/provider-dashboard/provider-dashboard.component.ts`
- `apps/clinical-portal/src/app/services/patient.service.ts`

**Code Changes**
```typescript
// provider-dashboard.component.ts - Update mapCareGapToDisplay
private async mapCareGapToDisplay(gap: CareGap): Promise<HighPriorityCareGap> {
  const patient = await this.patientService.getPatientById(gap.patientId).toPromise();
  return {
    id: gap.id,
    patientName: patient ? `${patient.name.family}, ${patient.name.given.join(' ')}` : `Patient ${gap.patientId}`,
    patientMRN: patient?.mrn || gap.patientId,
    // ... rest of mapping
  };
}
```

**Testing Requirements**
- [ ] Unit test: Verify patient name formatting
- [ ] Integration test: Verify API integration
- [ ] Error handling test: Verify graceful fallback

**Dependencies**
- Requires: Issue #1 (Provider Panel API)
- Blocks: Issue #7

---

### Issue #3: [Foundation] [Backend] Optimize FHIR Queries for Primary Care Dashboard

**Priority**: High | **Estimate**: L | **Labels**: `provider-view`, `backend`, `track-1`, `performance`

**User Story**
As a **provider with 150+ patients**,
I want **the dashboard to load in under 2 seconds**,
So that **I don't waste time waiting and can focus on patient care**.

**Acceptance Criteria**
```
Given a provider accesses their dashboard,
When the page loads,
Then all data (care gaps, quality measures, pending results) loads in < 2 seconds.

Given FHIR queries are executed,
When fetching patient data,
Then queries are batched and parallelized for efficiency.

Technical Requirements:
- [ ] Implement batch FHIR queries using _include and _revinclude
- [ ] Add Redis caching for frequently accessed patient data (5-minute TTL)
- [ ] Create composite index on patient queries
- [ ] Use async/parallel execution for independent data loads
- [ ] Add query timing metrics to Prometheus
```

**Technical Specifications**

**Affected Services**
- fhir-service (8085): Query optimization
- quality-measure-service (8087): Batch result fetching
- care-gap-service (8086): High-priority gap queries

**Optimization Strategy**
```java
// Batch FHIR query instead of N+1
GET /fhir/Patient?_id=id1,id2,id3&_include=Patient:generalPractitioner
    &_revinclude=Condition:subject
    &_revinclude=Observation:subject

// Current: N+1 queries (slow)
// Proposed: 1 batch query (fast)
```

**Performance Targets**
| Metric | Current | Target |
|--------|---------|--------|
| Dashboard load time | 4-6s | < 2s |
| Care gaps query | 1.5s | < 500ms |
| Quality measures query | 1.2s | < 400ms |
| Patient panel query | 2s | < 600ms |

**Testing Requirements**
- [ ] Load test: 100 concurrent provider dashboards
- [ ] Performance regression test: Ensure no degradation
- [ ] Cache hit rate test: Verify 80%+ cache hits

**Dependencies**
- Blocks: Issue #7, #8

---

### Issue #4: [Foundation] [Backend] Create Provider-Specific Care Gap Prioritization API

**Priority**: High | **Estimate**: M | **Labels**: `provider-view`, `backend`, `track-1`, `hipaa-review`

**User Story**
As a **primary care provider**,
I want **care gaps prioritized by clinical urgency and my workflow**,
So that **I address the most critical gaps first and close them efficiently**.

**Acceptance Criteria**
```
Given a provider views their care gaps,
When gaps are displayed,
Then they are sorted by: (1) Clinical urgency, (2) Due date, (3) Intervention complexity.

Given a gap requires immediate action (e.g., overdue A1c > 9%),
When displayed on dashboard,
Then it appears with a "Critical" badge and red highlighting.

Technical Requirements:
- [ ] Create `GET /api/v1/providers/{providerId}/care-gaps/prioritized` endpoint
- [ ] Implement scoring algorithm: urgency (40%) + due date (30%) + intervention ease (30%)
- [ ] Add primary care-specific priority rules (A1c, BP, screenings)
- [ ] Include recommendation actions in response
- [ ] Add @Audited annotation for HIPAA compliance
```

**Technical Specifications**

**API Contract**
```java
GET /api/v1/providers/{providerId}/care-gaps/prioritized
Headers:
  X-Auth-User-Id: {userId}
  X-Auth-Tenant-Ids: {tenantId}

Query Parameters:
  - limit: int (default: 20)
  - category: string[] (optional: DIABETES, HYPERTENSION, SCREENING, BEHAVIORAL)

Response 200:
{
  "providerId": "uuid",
  "gaps": [
    {
      "id": "gap-uuid",
      "patientId": "patient-uuid",
      "patientName": "Jackson, Robert",
      "patientMRN": "MRN-301",
      "measureId": "CMS122v11",
      "measureName": "Diabetes: HbA1c Poor Control (>9%)",
      "category": "DIABETES",
      "priority": "CRITICAL",
      "priorityScore": 95,
      "clinicalContext": "HbA1c 9.8%, last test 2025-08-15",
      "dueDate": "2025-12-15",
      "daysOverdue": 22,
      "recommendedAction": {
        "type": "MEDICATION_REVIEW",
        "description": "Consider adding/adjusting diabetes medication",
        "timeEstimate": "10 minutes"
      },
      "quickActions": [
        { "id": "schedule-visit", "label": "Schedule Follow-up" },
        { "id": "order-lab", "label": "Order A1c" },
        { "id": "send-reminder", "label": "Send Patient Reminder" }
      ]
    }
  ],
  "summary": {
    "critical": 3,
    "high": 8,
    "medium": 12,
    "low": 5
  }
}
```

**Priority Scoring Algorithm**
```java
public int calculatePriorityScore(CareGap gap) {
    int score = 0;

    // Clinical urgency (40%)
    if (gap.isOverdue()) score += 40;
    else if (gap.getDaysUntilDue() < 14) score += 30;
    else if (gap.getDaysUntilDue() < 30) score += 20;

    // Due date proximity (30%)
    int daysFactor = Math.max(0, 30 - gap.getDaysUntilDue());
    score += (daysFactor * 30) / 30;

    // Intervention ease for primary care (30%)
    if (gap.canCloseWithLabOrder()) score += 25;
    else if (gap.canCloseWithMedReview()) score += 20;
    else if (gap.requiresSpecialistReferral()) score += 10;

    return Math.min(100, score);
}
```

**HIPAA Compliance**
- [x] @Audited annotation on prioritized gaps endpoint
- [x] Cache TTL = 5 minutes for PHI data
- [x] Multi-tenant filtering enforced

**Dependencies**
- Requires: Issue #1 (Provider Panel)
- Blocks: Issue #7

---

### Issue #5: [Foundation] [Frontend] Update Provider Dashboard Layout for Primary Care Workflow

**Priority**: Medium | **Estimate**: M | **Labels**: `provider-view`, `frontend`, `track-1`, `ux`

**User Story**
As a **primary care provider**,
I want **the dashboard organized by my daily workflow**,
So that **I can complete tasks efficiently without searching for information**.

**Acceptance Criteria**
```
Given a provider accesses the dashboard,
When the page loads,
Then the layout displays in workflow order:
  1. Critical alerts requiring immediate action
  2. Today's scheduled patients with care gaps
  3. Results awaiting signature
  4. Quality measure performance
  5. Quick actions panel

Technical Requirements:
- [ ] Reorganize dashboard sections by workflow priority
- [ ] Add "Today's Patients" section with appointment-care gap integration
- [ ] Create collapsible sections for secondary information
- [ ] Implement drag-and-drop section reordering (saved to localStorage)
- [ ] Add keyboard shortcuts for common actions
```

**Technical Specifications**

**Updated Layout Structure**
```html
<div class="provider-dashboard">
  <!-- Section 1: Critical Alerts (always visible) -->
  <app-critical-alert-banner [alerts]="criticalAlerts"></app-critical-alert-banner>

  <!-- Section 2: Today's Schedule with Care Gaps -->
  <mat-card class="todays-patients">
    <mat-card-header>
      <mat-card-title>Today's Schedule ({{ patientsScheduledToday }})</mat-card-title>
    </mat-card-header>
    <app-schedule-with-gaps [appointments]="todayAppointments"></app-schedule-with-gaps>
  </mat-card>

  <!-- Section 3: Results Awaiting Review -->
  <mat-card class="pending-results" [class.has-abnormal]="hasAbnormalResults">
    <!-- Existing pending results table -->
  </mat-card>

  <!-- Section 4: High Priority Care Gaps -->
  <mat-card class="care-gaps">
    <!-- Enhanced care gaps with quick actions -->
  </mat-card>

  <!-- Section 5: Quality Performance Summary -->
  <mat-card class="quality-measures">
    <!-- Compact quality measure cards -->
  </mat-card>
</div>
```

**New Components**
- `ScheduleWithGapsComponent` - Shows today's appointments with patient care gap indicators
- `QuickActionsPanelComponent` - Common actions (order lab, send reminder, schedule)

**Testing Requirements**
- [ ] Visual regression test for layout changes
- [ ] Accessibility audit (WCAG 2.1 AA)
- [ ] Responsive design test (tablet, mobile)

**Dependencies**
- Requires: Issue #2, #4
- Blocks: Issue #7

---

### Issue #6: [Foundation] [Backend] Implement Pre-Visit Planning Data Aggregation

**Priority**: Medium | **Estimate**: L | **Labels**: `provider-view`, `backend`, `track-1`, `hipaa-review`

**User Story**
As a **provider preparing for patient visits**,
I want **a comprehensive pre-visit summary for each scheduled patient**,
So that **I can review their care gaps, recent results, and recommended actions before they arrive**.

**Acceptance Criteria**
```
Given a provider views their schedule,
When they click "Pre-Visit Summary" for a patient,
Then they see:
  - Open care gaps with recommended closures
  - Recent lab results and trends
  - Medication list with adherence indicators
  - Last visit notes summary
  - Suggested agenda items for the visit

Technical Requirements:
- [ ] Create `GET /api/v1/providers/{providerId}/patients/{patientId}/pre-visit-summary` endpoint
- [ ] Aggregate data from fhir-service, care-gap-service, quality-measure-service
- [ ] Generate AI-suggested agenda based on care gaps and history
- [ ] Include time estimates for each agenda item
- [ ] Add @Audited annotation for comprehensive PHI access
```

**Technical Specifications**

**API Contract**
```java
GET /api/v1/providers/{providerId}/patients/{patientId}/pre-visit-summary
Headers:
  X-Auth-User-Id: {userId}
  X-Auth-Tenant-Ids: {tenantId}

Response 200:
{
  "patientId": "uuid",
  "patientName": "Jackson, Robert",
  "appointmentDate": "2026-01-10T09:00:00Z",
  "appointmentType": "Follow-up",
  "careGaps": [
    {
      "measureName": "Diabetes HbA1c Control",
      "priority": "HIGH",
      "recommendation": "Order A1c lab, review current medications"
    }
  ],
  "recentResults": [
    {
      "name": "HbA1c",
      "value": "8.2%",
      "date": "2025-10-15",
      "trend": "improving",
      "previousValue": "8.8%"
    }
  ],
  "medications": [
    {
      "name": "Metformin 1000mg",
      "frequency": "BID",
      "adherence": "good"
    }
  ],
  "suggestedAgenda": [
    { "topic": "Review diabetes management", "timeEstimate": "5 min", "priority": 1 },
    { "topic": "Discuss A1c improvement", "timeEstimate": "3 min", "priority": 2 },
    { "topic": "Address blood pressure", "timeEstimate": "4 min", "priority": 3 }
  ],
  "lastVisitSummary": "Patient reported improved diet adherence..."
}
```

**HIPAA Compliance**
- [x] @Audited annotation with eventType = "PRE_VISIT_SUMMARY_ACCESS"
- [x] Multi-tenant filtering on all data sources
- [x] No caching for pre-visit summaries (always fresh data)

**Dependencies**
- Requires: Issue #1, #3
- Blocks: Issue #8

---

## Track 2: Core Provider Workflows (Sprint 3-4)

### Issue #7: [Workflows] [Frontend] Implement Provider-Optimized Care Gap Closure Workflow

**Priority**: High | **Estimate**: L | **Labels**: `provider-view`, `frontend`, `track-2`, `ux`

**User Story**
As a **provider closing care gaps**,
I want **quick actions that complete common closures in 2-3 clicks**,
So that **I can close gaps efficiently during or after patient visits**.

**Acceptance Criteria**
```
Given a care gap is displayed on the dashboard,
When I click a quick action (e.g., "Order Lab"),
Then the system:
  1. Opens a pre-filled order dialog
  2. Allows me to confirm or modify
  3. Closes the gap automatically when order is placed

Given I close a gap manually,
When I select the closure reason,
Then common reasons are pre-populated based on gap type (e.g., "Lab ordered", "Patient declined").

Technical Requirements:
- [ ] Add quick action buttons to care gap rows
- [ ] Implement QuickActionDialog component for each action type
- [ ] Create smart closure reason suggestions based on measure type
- [ ] Add bulk closure capability for similar gaps
- [ ] Track closure time metrics for workflow optimization
```

**Technical Specifications**

**Quick Action Types**
| Gap Type | Quick Actions |
|----------|--------------|
| Screening (e.g., A1c) | Order Lab, Schedule Visit, Send Reminder |
| Medication Review | Review Meds, Refill Request, Schedule Visit |
| Preventive Care | Order Test, Schedule Procedure, Send Education |
| Behavioral Health | Schedule Screening, Refer to Specialist |

**Component Structure**
```typescript
// quick-action-dialog.component.ts
@Component({
  selector: 'app-quick-action-dialog',
  template: `
    <h2 mat-dialog-title>{{ actionTitle }}</h2>
    <mat-dialog-content>
      <ng-container [ngSwitch]="actionType">
        <app-order-lab-form *ngSwitchCase="'ORDER_LAB'" [patient]="patient" [measure]="measure"></app-order-lab-form>
        <app-schedule-visit-form *ngSwitchCase="'SCHEDULE_VISIT'" [patient]="patient"></app-schedule-visit-form>
        <app-send-reminder-form *ngSwitchCase="'SEND_REMINDER'" [patient]="patient"></app-send-reminder-form>
      </ng-container>
    </mat-dialog-content>
    <mat-dialog-actions>
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-raised-button color="primary" (click)="executeAction()">
        {{ actionLabel }}
      </button>
    </mat-dialog-actions>
  `
})
```

**Testing Requirements**
- [ ] E2E test: Complete care gap closure via quick action
- [ ] Unit test: Each quick action type
- [ ] Accessibility test: Keyboard navigation through workflow

**Dependencies**
- Requires: Issue #1, #2, #4, #5
- Blocks: Issue #10

---

### Issue #8: [Workflows] [Frontend] Create Pre-Visit Planning View

**Priority**: High | **Estimate**: M | **Labels**: `provider-view`, `frontend`, `track-2`

**User Story**
As a **provider**,
I want **a dedicated pre-visit planning view for tomorrow's patients**,
So that **I can prepare for visits in advance and optimize appointment time**.

**Acceptance Criteria**
```
Given I access the pre-visit planning view,
When I select a date (default: tomorrow),
Then I see a list of scheduled patients with:
  - Pre-visit summary card for each patient
  - Expandable details with full care gap list
  - Print/export option for paper-based workflow

Technical Requirements:
- [ ] Create PreVisitPlanningComponent with date picker
- [ ] Implement patient list with expandable summaries
- [ ] Add print stylesheet for paper summaries
- [ ] Enable bulk pre-visit summary export (PDF)
- [ ] Add "Mark as Prepared" checkbox per patient
```

**Technical Specifications**

**New Route**
```typescript
// app.routes.ts
{ path: 'pre-visit', component: PreVisitPlanningComponent, canActivate: [AuthGuard, RoleGuard], data: { roles: ['ADMIN', 'EVALUATOR'] } }
```

**Component Layout**
```
┌─────────────────────────────────────────────┐
│ Pre-Visit Planning                    [📅]  │
│ Date: January 10, 2026  │  8 patients       │
├─────────────────────────────────────────────┤
│ □ 9:00 AM - Jackson, Robert     [Expand ▼]  │
│   MRN-301 | DOB: 03/15/1965 | 3 care gaps   │
│   ├── Diabetes A1c Control (HIGH)           │
│   ├── Blood Pressure Check (MEDIUM)         │
│   └── Depression Screening (LOW)            │
│   [View Full Summary] [Print]               │
├─────────────────────────────────────────────┤
│ □ 9:30 AM - Garcia, Maria       [Expand ▼]  │
│   ...                                       │
└─────────────────────────────────────────────┘
[Print All Summaries] [Export PDF]
```

**Dependencies**
- Requires: Issue #6 (Pre-Visit Summary API)

---

### Issue #9: [Workflows] [Backend] Implement Bulk Result Signing API

**Priority**: Medium | **Estimate**: M | **Labels**: `provider-view`, `backend`, `track-2`, `hipaa-review`

**User Story**
As a **provider reviewing results**,
I want **to sign multiple normal results at once**,
So that **I can quickly clear my inbox and focus on abnormal results**.

**Acceptance Criteria**
```
Given I have 20 results awaiting signature,
When I filter to "Normal Results Only" and click "Sign All",
Then all selected results are signed with my credentials and timestamp.

Given I attempt to bulk sign abnormal results,
When I confirm the action,
Then I receive a warning and must acknowledge each abnormal result individually.

Technical Requirements:
- [ ] Create `POST /api/v1/results/bulk-sign` endpoint
- [ ] Implement safety check: abnormal results require individual acknowledgment
- [ ] Add signing audit trail with provider ID, timestamp, result IDs
- [ ] Send notifications to care team on result signing
- [ ] Add @Audited annotation for PHI modification
```

**Technical Specifications**

**API Contract**
```java
POST /api/v1/results/bulk-sign
Headers:
  X-Auth-User-Id: {userId}
  X-Auth-Tenant-Ids: {tenantId}

Request:
{
  "resultIds": ["uuid1", "uuid2", "uuid3"],
  "signatureType": "ELECTRONIC",
  "acknowledgments": [
    { "resultId": "uuid-abnormal", "acknowledged": true, "notes": "Reviewed, no action needed" }
  ]
}

Response 200:
{
  "signed": 18,
  "requiresAcknowledgment": 2,
  "failed": 0,
  "signedResults": [...],
  "pendingAbnormal": [
    { "resultId": "uuid-abnormal1", "patientName": "Jackson, Robert", "resultType": "HbA1c", "value": "9.8%" }
  ]
}
```

**HIPAA Compliance**
- [x] @Audited annotation on bulk signing with result IDs
- [x] Immutable audit trail for signatures
- [x] No caching of signing operations

**Dependencies**
- Blocks: Issue #10

---

### Issue #10: [Workflows] [Frontend] Enhance Results Review Interface

**Priority**: Medium | **Estimate**: M | **Labels**: `provider-view`, `frontend`, `track-2`

**User Story**
As a **provider reviewing results**,
I want **abnormal results highlighted with patient context**,
So that **I can quickly assess clinical significance and take action**.

**Acceptance Criteria**
```
Given results are displayed in the provider dashboard,
When a result is abnormal,
Then it is:
  - Highlighted in red/orange based on severity
  - Shows patient's relevant history (previous values, trend)
  - Displays suggested actions based on result type

Technical Requirements:
- [ ] Add abnormal result highlighting with severity levels
- [ ] Show result trend (↑ ↓ →) compared to previous
- [ ] Add inline patient context (age, conditions, medications)
- [ ] Implement result comparison view (current vs. previous)
- [ ] Add quick action buttons (contact patient, order follow-up, refer)
```

**Technical Specifications**

**Severity Levels**
| Level | Criteria | Display |
|-------|----------|---------|
| Critical | Panic values (e.g., K+ > 6.5) | Red background, banner alert |
| High | Significantly abnormal | Orange highlight |
| Moderate | Mildly abnormal | Yellow highlight |
| Normal | Within range | Standard display |

**Trend Calculation**
```typescript
calculateTrend(current: number, previous: number, normalRange: [number, number]): 'improving' | 'worsening' | 'stable' {
  const currentFromNormal = Math.abs(current - (normalRange[0] + normalRange[1]) / 2);
  const previousFromNormal = Math.abs(previous - (normalRange[0] + normalRange[1]) / 2);

  if (currentFromNormal < previousFromNormal - 0.1 * (normalRange[1] - normalRange[0])) return 'improving';
  if (currentFromNormal > previousFromNormal + 0.1 * (normalRange[1] - normalRange[0])) return 'worsening';
  return 'stable';
}
```

**Dependencies**
- Requires: Issue #9 (Bulk Signing API)

---

### Issue #11: [Workflows] [Backend] Create Provider Performance Metrics API

**Priority**: Medium | **Estimate**: M | **Labels**: `provider-view`, `backend`, `track-2`

**User Story**
As a **provider**,
I want **to see my quality measure performance compared to peers**,
So that **I can identify areas for improvement and track progress over time**.

**Acceptance Criteria**
```
Given I view my performance metrics,
When the data loads,
Then I see:
  - My compliance rates for each measure
  - Practice average for comparison
  - Trend over last 12 months
  - Ranking among providers (anonymized)

Technical Requirements:
- [ ] Create `GET /api/v1/providers/{providerId}/performance` endpoint
- [ ] Calculate provider-specific numerator/denominator from panel
- [ ] Aggregate practice-level averages (anonymized)
- [ ] Store historical performance for trend analysis
- [ ] Add percentile ranking (anonymized)
```

**Technical Specifications**

**API Contract**
```java
GET /api/v1/providers/{providerId}/performance
Headers:
  X-Auth-User-Id: {userId}
  X-Auth-Tenant-Ids: {tenantId}

Query Parameters:
  - measureIds: string[] (optional, default: all)
  - period: string (optional: YTD, LAST_12_MONTHS, LAST_QUARTER)

Response 200:
{
  "providerId": "uuid",
  "period": "LAST_12_MONTHS",
  "measures": [
    {
      "measureId": "CMS122v11",
      "measureName": "Diabetes: HbA1c Poor Control",
      "providerRate": 78.5,
      "practiceAverage": 72.3,
      "percentile": 75,
      "numerator": 157,
      "denominator": 200,
      "trend": [
        { "month": "2025-01", "rate": 70.2 },
        { "month": "2025-02", "rate": 72.1 },
        ...
      ]
    }
  ],
  "overallScore": 81.2,
  "improvementAreas": ["Blood Pressure Control", "Statin Therapy"]
}
```

**Dependencies**
- Requires: Issue #1 (Provider Panel)
- Blocks: Issue #18

---

### Issue #12: [Workflows] [Frontend] Add Risk Stratification View

**Priority**: Medium | **Estimate**: M | **Labels**: `provider-view`, `frontend`, `track-2`

**User Story**
As a **provider managing a panel of 150+ patients**,
I want **patients grouped by risk level with actionable insights**,
So that **I can prioritize high-risk patients and allocate my time effectively**.

**Acceptance Criteria**
```
Given I access the risk stratification view,
When the data loads,
Then I see patients grouped into:
  - Critical Risk (score 85-100): Immediate attention needed
  - High Risk (score 70-84): Weekly monitoring
  - Moderate Risk (score 50-69): Monthly review
  - Low Risk (score 0-49): Routine care

Technical Requirements:
- [ ] Create RiskStratificationComponent with patient grouping
- [ ] Implement risk score visualization (gauge, heatmap)
- [ ] Add drill-down to patient details from each group
- [ ] Show risk factors contributing to each patient's score
- [ ] Enable filtering by risk factor (diabetes, CHF, COPD)
```

**Technical Specifications**

**Risk Calculation Factors (Primary Care)**
| Factor | Weight | Data Source |
|--------|--------|-------------|
| HCC Risk Score | 25% | hcc-service |
| Open Care Gaps | 20% | care-gap-service |
| Recent ED Visits | 15% | fhir-service (Encounter) |
| Medication Non-Adherence | 15% | fhir-service (MedicationRequest) |
| Chronic Conditions | 15% | fhir-service (Condition) |
| SDOH Factors | 10% | sdoh-service |

**Dependencies**
- Requires: Issue #1, #3

---

### Issue #13: [Workflows] [Backend] Implement Smart Notification Preferences

**Priority**: Low | **Estimate**: S | **Labels**: `provider-view`, `backend`, `track-2`

**User Story**
As a **provider**,
I want **to customize when and how I receive notifications**,
So that **I'm alerted to critical items without being overwhelmed by routine updates**.

**Acceptance Criteria**
```
Given I configure my notification preferences,
When I save my settings,
Then I only receive notifications matching my preferences.

Notification Types:
- Critical results (always enabled, cannot disable)
- Care gap overdue (configurable)
- Quality measure updates (configurable)
- Patient message (configurable)

Technical Requirements:
- [ ] Create provider_notification_preferences table
- [ ] Implement preference API endpoints (GET/PUT)
- [ ] Integrate with existing notification service
- [ ] Add notification filtering based on preferences
```

**Dependencies**
- None (can be implemented independently)

---

## Track 3: Custom Measure Builder (Sprint 4-5)

### Issue #14: [Measures] [Frontend] Add Primary Care Measure Templates

**Priority**: High | **Estimate**: M | **Labels**: `provider-view`, `frontend`, `track-3`

**User Story**
As a **provider creating custom measures**,
I want **pre-built templates for common primary care measures**,
So that **I can create new measures quickly without writing CQL from scratch**.

**Acceptance Criteria**
```
Given I open the measure builder,
When I click "New Measure",
Then I see template options:
  - Diabetes Management (A1c, foot exam, eye exam)
  - Hypertension Control (BP targets by age)
  - Preventive Screenings (cancer, depression)
  - Medication Adherence (statins, antihypertensives)
  - Custom (blank template)

Technical Requirements:
- [ ] Create measure template library with 10+ primary care templates
- [ ] Implement template selection dialog with category filtering
- [ ] Pre-populate CQL, value sets, and metadata from template
- [ ] Allow customization after template selection
- [ ] Add "Clone from Existing" option
```

**Technical Specifications**

**Template Structure**
```typescript
interface MeasureTemplate {
  id: string;
  name: string;
  category: 'DIABETES' | 'HYPERTENSION' | 'SCREENING' | 'MEDICATION' | 'BEHAVIORAL';
  description: string;
  cqlTemplate: string;
  valueSetIds: string[];
  defaultPopulationCriteria: {
    initialPopulation: string;
    denominator: string;
    numerator: string;
    exclusions?: string;
  };
  customizableFields: string[]; // e.g., ["ageRange", "targetValue"]
}
```

**Template Library (Initial 10)**
1. Diabetes: HbA1c Control (<8%)
2. Diabetes: HbA1c Poor Control (>9%)
3. Hypertension: BP Control (<140/90)
4. Breast Cancer Screening
5. Colorectal Cancer Screening
6. Depression Screening (PHQ-9)
7. Statin Therapy for CVD Prevention
8. Annual Wellness Visit
9. Fall Risk Assessment (65+)
10. Tobacco Use Screening & Cessation

**Dependencies**
- Blocks: Issue #15

---

### Issue #15: [Measures] [Backend] Implement AI-Assisted CQL Generation

**Priority**: High | **Estimate**: XL | **Labels**: `provider-view`, `backend`, `track-3`, `ai`

**User Story**
As a **provider without CQL expertise**,
I want **AI assistance to generate CQL from natural language descriptions**,
So that **I can create custom measures without learning a programming language**.

**Acceptance Criteria**
```
Given I'm creating a custom measure,
When I describe my criteria in plain English (e.g., "Patients with diabetes who have not had an A1c test in the last 6 months"),
Then the AI generates valid CQL that I can review and modify.

Given the AI generates CQL,
When I review it,
Then I see:
  - Generated CQL code with syntax highlighting
  - Plain English explanation of what the CQL does
  - Validation status (syntax check)
  - Test results against sample patients

Technical Requirements:
- [ ] Integrate with AI assistant service for CQL generation
- [ ] Create CQL generation prompt templates for common patterns
- [ ] Implement CQL validation (syntax + semantic)
- [ ] Add "Explain CQL" feature to describe existing code
- [ ] Store successful generations as templates for future use
```

**Technical Specifications**

**AI Integration Flow**
```
User Input (Natural Language)
    → AI Service (prompt engineering)
    → Generated CQL
    → CQL Validator (cql-engine-service)
    → Test Execution (sample patients)
    → User Review & Approval
```

**API Contract**
```java
POST /api/v1/measures/ai/generate-cql
Headers:
  X-Auth-User-Id: {userId}
  X-Auth-Tenant-Ids: {tenantId}

Request:
{
  "description": "Patients aged 18-75 with type 2 diabetes who have not had an HbA1c test in the last 6 months",
  "measureType": "PROCESS",
  "context": {
    "existingConditions": ["diabetes"],
    "relevantValueSets": ["2.16.840.1.113883.3.464.1003.103.12.1001"]
  }
}

Response 200:
{
  "generatedCql": "library DiabetesA1cGap version '1.0'\n\nusing FHIR version '4.0.1'\n...",
  "explanation": "This measure identifies patients aged 18-75 with a diagnosis of type 2 diabetes...",
  "confidence": 0.92,
  "validationStatus": "VALID",
  "suggestedValueSets": [
    { "oid": "2.16.840.1.113883.3.464.1003.103.12.1001", "name": "Diabetes" },
    { "oid": "2.16.840.1.113883.3.464.1003.198.12.1013", "name": "HbA1c Laboratory Test" }
  ],
  "testResults": {
    "sampleSize": 10,
    "numerator": 3,
    "denominator": 10,
    "complianceRate": 30.0
  }
}
```

**Dependencies**
- Requires: Issue #14 (Templates)
- Blocks: Issue #16

---

### Issue #16: [Measures] [Frontend] Implement Live Patient Preview in Measure Builder

**Priority**: Medium | **Estimate**: M | **Labels**: `provider-view`, `frontend`, `track-3`

**User Story**
As a **provider building a custom measure**,
I want **real-time preview of which patients match my criteria**,
So that **I can validate my measure logic before publishing**.

**Acceptance Criteria**
```
Given I'm editing CQL in the measure builder,
When I click "Preview",
Then I see:
  - Count of patients matching initial population
  - Count meeting denominator criteria
  - Count meeting numerator criteria (compliant)
  - Sample patient list with match details

Technical Requirements:
- [ ] Add "Live Preview" panel to CQL editor dialog
- [ ] Implement real-time CQL evaluation against sample patients
- [ ] Show patient-level detail for debugging (why included/excluded)
- [ ] Add "Test with Specific Patient" option
- [ ] Limit preview to 100 patients for performance
```

**Technical Specifications**

**Preview Panel Layout**
```
┌─────────────────────────────────────────────┐
│ Live Preview (100 sample patients)          │
├─────────────────────────────────────────────┤
│ Initial Population:  156 patients           │
│ Denominator:         142 patients           │
│ Numerator:           98 patients            │
│ Compliance Rate:     69.0%                  │
├─────────────────────────────────────────────┤
│ Sample Matches:                             │
│ ✓ Jackson, Robert - MRN-301 (Compliant)     │
│ ✗ Garcia, Maria - MRN-302 (Non-compliant)   │
│   Reason: No A1c in last 6 months           │
│ ...                                         │
└─────────────────────────────────────────────┘
```

**Dependencies**
- Requires: Issue #15 (AI-Assisted CQL)

---

### Issue #17: [Measures] [Backend] Add Measure Versioning and Audit Trail

**Priority**: Medium | **Estimate**: M | **Labels**: `provider-view`, `backend`, `track-3`, `hipaa-review`

**User Story**
As a **quality administrator**,
I want **version history for all custom measures**,
So that **I can track changes, roll back if needed, and maintain compliance audit trail**.

**Acceptance Criteria**
```
Given a measure is modified,
When the changes are saved,
Then:
  - A new version is created (semantic versioning)
  - Previous version is preserved (immutable)
  - Change author and timestamp are recorded
  - Diff view shows what changed

Technical Requirements:
- [ ] Implement measure versioning (major.minor.patch)
- [ ] Store version history in measure_versions table
- [ ] Create diff comparison API
- [ ] Add version selector in measure builder UI
- [ ] Enable rollback to previous version
```

**Technical Specifications**

**Database Schema**
```sql
-- Liquibase migration: 0055-create-measure-versions.xml
CREATE TABLE measure_versions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  measure_id UUID NOT NULL REFERENCES custom_measures(id),
  version VARCHAR(20) NOT NULL,
  cql_text TEXT NOT NULL,
  value_sets JSONB,
  metadata JSONB,
  created_by UUID NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  change_summary TEXT,
  UNIQUE(measure_id, version)
);

CREATE INDEX idx_measure_versions_measure ON measure_versions(measure_id);
```

**HIPAA Compliance**
- [x] Immutable version records (no DELETE allowed)
- [x] Audit trail for all version changes
- [x] Author tracking required

**Dependencies**
- Blocks: Issue #18

---

### Issue #18: [Measures] [Frontend] Create Measure Performance Comparison Dashboard

**Priority**: Medium | **Estimate**: M | **Labels**: `provider-view`, `frontend`, `track-3`

**User Story**
As a **provider**,
I want **to compare performance across my custom measures**,
So that **I can identify which measures are driving care gaps and focus improvement efforts**.

**Acceptance Criteria**
```
Given I have multiple custom measures,
When I access the comparison dashboard,
Then I see:
  - Side-by-side compliance rates for all measures
  - Trend lines over last 12 months
  - Patient overlap analysis (patients in multiple measure denominators)
  - Drill-down to patient lists per measure

Technical Requirements:
- [ ] Create MeasureComparisonComponent with chart library
- [ ] Implement measure selection (up to 5 measures)
- [ ] Add Venn diagram for patient overlap
- [ ] Enable export to PDF/PNG for reporting
- [ ] Add benchmark comparison (CMS targets)
```

**Technical Specifications**

**Visualization Types**
1. Bar chart: Compliance rates comparison
2. Line chart: Trend over time
3. Venn diagram: Patient overlap
4. Data table: Detailed metrics

**Dependencies**
- Requires: Issue #11 (Provider Performance API), Issue #17 (Versioning)

---

## Track 4: Insights & Analytics (Sprint 5-6)

### Issue #19: [Insights] [Backend] Implement Population Health Insights Engine

**Priority**: High | **Estimate**: XL | **Labels**: `provider-view`, `backend`, `track-4`, `ai`

**User Story**
As a **provider analyzing my patient panel**,
I want **AI-generated insights about population health patterns**,
So that **I can identify systemic issues and target interventions effectively**.

**Acceptance Criteria**
```
Given I access the insights dashboard,
When AI analysis completes,
Then I see insights like:
  - "15 patients with diabetes have not been seen in 6+ months"
  - "Blood pressure control has declined 8% since adding new patients"
  - "Patients aged 40-50 have 2x more care gaps than other age groups"

Technical Requirements:
- [ ] Create insights engine using predictive-analytics-service
- [ ] Implement pattern detection algorithms
- [ ] Generate natural language insight summaries
- [ ] Add insight prioritization by impact
- [ ] Enable insight dismissal with reason tracking
```

**Technical Specifications**

**Insight Types**
| Type | Detection Logic | Example |
|------|-----------------|---------|
| Care Gap Cluster | >10 patients with same gap | "23 patients need colorectal screening" |
| Performance Trend | >5% change in 30 days | "A1c control improving (+7%)" |
| At-Risk Population | Risk score increase | "12 patients moved to high-risk" |
| Intervention Opportunity | Similar gaps, similar patients | "Batch outreach for flu vaccines" |

**API Contract**
```java
GET /api/v1/providers/{providerId}/insights
Headers:
  X-Auth-User-Id: {userId}
  X-Auth-Tenant-Ids: {tenantId}

Response 200:
{
  "providerId": "uuid",
  "generatedAt": "2026-01-06T10:30:00Z",
  "insights": [
    {
      "id": "insight-uuid",
      "type": "CARE_GAP_CLUSTER",
      "title": "Colorectal Cancer Screening Gap",
      "description": "23 patients aged 50-75 have not completed colorectal cancer screening",
      "impact": "HIGH",
      "affectedPatients": 23,
      "suggestedAction": {
        "type": "BATCH_OUTREACH",
        "description": "Send screening reminder to all 23 patients"
      },
      "metrics": {
        "potentialComplianceImprovement": 15.3
      }
    }
  ],
  "summary": {
    "highImpact": 3,
    "mediumImpact": 5,
    "lowImpact": 8
  }
}
```

**Dependencies**
- Requires: Track 2 completion

---

### Issue #20: [Insights] [Frontend] Create Population Health Insights Dashboard

**Priority**: High | **Estimate**: L | **Labels**: `provider-view`, `frontend`, `track-4`

**User Story**
As a **provider**,
I want **a visual dashboard showing population health insights**,
So that **I can quickly understand my panel's health status and identify improvement opportunities**.

**Acceptance Criteria**
```
Given I access the insights dashboard,
When the page loads,
Then I see:
  - Insight cards ranked by impact
  - Population health summary metrics
  - Interactive charts (care gap distribution, risk pyramid)
  - Action buttons for each insight

Technical Requirements:
- [ ] Create InsightsDashboardComponent with card-based layout
- [ ] Implement insight filtering (by type, impact, status)
- [ ] Add interactive charts using ngx-charts
- [ ] Enable one-click actions from insight cards
- [ ] Add insight history with dismissed insights
```

**Technical Specifications**

**Dashboard Layout**
```
┌─────────────────────────────────────────────────────┐
│ Population Health Insights                          │
│ Panel: 156 patients | Last updated: 5 min ago       │
├─────────────────────────────────────────────────────┤
│ [HIGH IMPACT] [MEDIUM] [LOW] [DISMISSED]            │
├───────────────────┬─────────────────────────────────┤
│ Insight Cards     │ Population Summary              │
│ ┌───────────────┐ │ ┌─────────────────────────────┐ │
│ │ Screening Gap │ │ │ Risk Pyramid               │ │
│ │ 23 patients   │ │ │    ▲ High: 23 (15%)       │ │
│ │ [Take Action] │ │ │   ▲▲ Medium: 58 (37%)    │ │
│ │               │ │ │  ▲▲▲ Low: 75 (48%)       │ │
│ └───────────────┘ │ └─────────────────────────────┘ │
│ ┌───────────────┐ │ ┌─────────────────────────────┐ │
│ │ A1c Trend ↓   │ │ │ Care Gap Distribution      │ │
│ │ -8% this month│ │ │ [Bar Chart]                │ │
│ │ [View Details]│ │ │                            │ │
│ └───────────────┘ │ └─────────────────────────────┘ │
└───────────────────┴─────────────────────────────────┘
```

**Dependencies**
- Requires: Issue #19 (Insights Engine)

---

### Issue #21: [Insights] [Backend] Implement Predictive Care Gap Detection

**Priority**: Medium | **Estimate**: L | **Labels**: `provider-view`, `backend`, `track-4`, `ai`

**User Story**
As a **provider**,
I want **to see patients who are at risk of developing care gaps**,
So that **I can intervene proactively before gaps occur**.

**Acceptance Criteria**
```
Given the predictive model runs,
When I view predicted care gaps,
Then I see patients who are:
  - Likely to miss upcoming screenings (based on history)
  - At risk of medication non-adherence
  - Trending toward quality measure failure

Technical Requirements:
- [ ] Create predictive model using patient history patterns
- [ ] Implement risk scoring for each measure type
- [ ] Generate "Predicted Gap" alerts 30 days before likely occurrence
- [ ] Track prediction accuracy for model improvement
- [ ] Add explanation for why patient was flagged
```

**Technical Specifications**

**Prediction Factors**
| Factor | Weight | Description |
|--------|--------|-------------|
| Historical Pattern | 40% | Past gap history for same measure |
| Appointment Adherence | 25% | Show rate for scheduled visits |
| Medication Refills | 20% | Timely refill patterns |
| Similar Patient Behavior | 15% | Clustering with known non-compliant patients |

**API Contract**
```java
GET /api/v1/providers/{providerId}/predicted-gaps
Headers:
  X-Auth-User-Id: {userId}
  X-Auth-Tenant-Ids: {tenantId}

Response 200:
{
  "predictions": [
    {
      "patientId": "uuid",
      "patientName": "Jackson, Robert",
      "measureId": "CMS122v11",
      "measureName": "Diabetes A1c Control",
      "currentStatus": "COMPLIANT",
      "predictedGapDate": "2026-02-15",
      "riskScore": 78,
      "riskFactors": [
        { "factor": "Missed last 2 lab appointments", "contribution": 45 },
        { "factor": "Similar patients had 80% gap rate", "contribution": 33 }
      ],
      "recommendedIntervention": "Schedule proactive A1c lab"
    }
  ]
}
```

**Dependencies**
- Requires: Issue #19 (Insights Engine)

---

### Issue #22: [Insights] [Frontend] Add Custom Report Builder

**Priority**: Medium | **Estimate**: L | **Labels**: `provider-view`, `frontend`, `track-4`

**User Story**
As a **provider**,
I want **to create custom reports combining quality measures and patient data**,
So that **I can generate reports for staff meetings, quality committees, and personal tracking**.

**Acceptance Criteria**
```
Given I access the report builder,
When I configure a report,
Then I can:
  - Select metrics to include (measures, care gaps, patient counts)
  - Choose date range and comparison period
  - Filter by patient attributes (age, risk level, condition)
  - Export to PDF, Excel, or print

Technical Requirements:
- [ ] Create ReportBuilderComponent with drag-and-drop sections
- [ ] Implement report template saving and sharing
- [ ] Add scheduled report delivery via email
- [ ] Enable report comparison (this month vs. last month)
- [ ] Store report history for trend analysis
```

**Technical Specifications**

**Report Sections**
1. Executive Summary (auto-generated)
2. Quality Measure Performance (table + charts)
3. Care Gap Analysis (by type, by patient)
4. Patient Risk Stratification
5. Custom Metrics (user-defined)

**Export Formats**
- PDF (print-ready)
- Excel (data analysis)
- PNG (chart images for presentations)

**Dependencies**
- Requires: Issue #11 (Provider Performance API)

---

## Track 5: Enhancement & Polish (Sprint 6-7)

### Issue #23: [Enhancement] [Frontend] Implement Keyboard Shortcuts for Provider Workflows

**Priority**: Low | **Estimate**: S | **Labels**: `provider-view`, `frontend`, `track-5`, `ux`

**User Story**
As a **power user provider**,
I want **keyboard shortcuts for common actions**,
So that **I can navigate and complete tasks faster without using the mouse**.

**Acceptance Criteria**
```
Given I'm on the provider dashboard,
When I press keyboard shortcuts,
Then I can:
  - Navigate between sections (Tab, Shift+Tab)
  - Open quick action dialogs (Ctrl+1 = Order Lab, etc.)
  - Sign results (Ctrl+S)
  - Refresh data (F5 or Ctrl+R)
  - Open help (?)

Technical Requirements:
- [ ] Implement global keyboard shortcut service
- [ ] Add shortcut hints on hover for action buttons
- [ ] Create keyboard shortcut help modal (? key)
- [ ] Allow shortcut customization in user preferences
- [ ] Ensure shortcuts don't conflict with browser defaults
```

**Technical Specifications**

**Default Shortcuts**
| Shortcut | Action |
|----------|--------|
| `?` | Open help/shortcuts modal |
| `Ctrl+R` | Refresh dashboard |
| `Ctrl+S` | Sign selected result |
| `Ctrl+Shift+A` | Sign all normal results |
| `Ctrl+1` | Quick action: Order Lab |
| `Ctrl+2` | Quick action: Schedule Visit |
| `Ctrl+F` | Focus search |
| `Esc` | Close dialog |

**Dependencies**
- None (standalone enhancement)

---

### Issue #24: [Enhancement] [Frontend] Add Provider Dashboard Help System

**Priority**: Low | **Estimate**: S | **Labels**: `provider-view`, `frontend`, `track-5`, `documentation`

**User Story**
As a **new provider user**,
I want **contextual help and tooltips throughout the dashboard**,
So that **I can learn how to use features without reading external documentation**.

**Acceptance Criteria**
```
Given I'm new to the provider dashboard,
When I hover over UI elements,
Then I see helpful tooltips explaining the feature.

Given I click the help icon,
When the help panel opens,
Then I see:
  - Feature overview for current page
  - Step-by-step guides for common tasks
  - Video tutorials (links)
  - Contact support option

Technical Requirements:
- [ ] Add tooltips to all major UI elements
- [ ] Create HelpPanelComponent with contextual content
- [ ] Implement "What's New" banner for feature updates
- [ ] Add guided tour for first-time users (optional)
- [ ] Integrate with knowledge base (existing)
```

**Technical Specifications**

**Help Content Structure**
```typescript
interface HelpContent {
  pageId: string;
  title: string;
  overview: string;
  features: { name: string; description: string; tip: string }[];
  tutorials: { title: string; url: string; duration: string }[];
  faq: { question: string; answer: string }[];
}
```

**Dependencies**
- None (standalone enhancement)

---

## Implementation Roadmap

### Sprint 1-2: Foundation (Issues #1-6)
**Goal**: Establish provider panel infrastructure and optimize data access

| Issue | Title | Estimate | Dependencies |
|-------|-------|----------|--------------|
| #1 | Provider Panel Assignment API | L | None |
| #2 | Real Patient Names Integration | M | #1 |
| #3 | FHIR Query Optimization | L | None |
| #4 | Provider Care Gap Prioritization API | M | #1 |
| #5 | Dashboard Layout Update | M | #2, #4 |
| #6 | Pre-Visit Planning API | L | #1, #3 |

**Sprint 1 Focus**: Issues #1, #3 (backend foundation)
**Sprint 2 Focus**: Issues #2, #4, #5, #6 (API integration + UI)

### Sprint 3-4: Core Workflows (Issues #7-13)
**Goal**: Optimize daily provider workflows and result management

| Issue | Title | Estimate | Dependencies |
|-------|-------|----------|--------------|
| #7 | Care Gap Closure Workflow | L | #1, #2, #4, #5 |
| #8 | Pre-Visit Planning View | M | #6 |
| #9 | Bulk Result Signing API | M | None |
| #10 | Results Review Interface | M | #9 |
| #11 | Provider Performance API | M | #1 |
| #12 | Risk Stratification View | M | #1, #3 |
| #13 | Notification Preferences | S | None |

**Sprint 3 Focus**: Issues #7, #8, #9 (high priority workflows)
**Sprint 4 Focus**: Issues #10, #11, #12, #13 (enhancement + metrics)

### Sprint 4-5: Custom Measure Builder (Issues #14-18)
**Goal**: Enable custom quality measure creation with AI assistance

| Issue | Title | Estimate | Dependencies |
|-------|-------|----------|--------------|
| #14 | Primary Care Measure Templates | M | None |
| #15 | AI-Assisted CQL Generation | XL | #14 |
| #16 | Live Patient Preview | M | #15 |
| #17 | Measure Versioning | M | None |
| #18 | Measure Comparison Dashboard | M | #11, #17 |

**Sprint 4 Focus**: Issues #14, #17 (templates + versioning)
**Sprint 5 Focus**: Issues #15, #16, #18 (AI + preview)

### Sprint 5-6: Insights & Analytics (Issues #19-22)
**Goal**: Surface actionable population health insights

| Issue | Title | Estimate | Dependencies |
|-------|-------|----------|--------------|
| #19 | Population Health Insights Engine | XL | Track 2 |
| #20 | Insights Dashboard | L | #19 |
| #21 | Predictive Care Gap Detection | L | #19 |
| #22 | Custom Report Builder | L | #11 |

**Sprint 5 Focus**: Issues #19, #20 (insights foundation)
**Sprint 6 Focus**: Issues #21, #22 (predictions + reports)

### Sprint 6-7: Enhancement & Polish (Issues #23-24)
**Goal**: Improve usability and documentation

| Issue | Title | Estimate | Dependencies |
|-------|-------|----------|--------------|
| #23 | Keyboard Shortcuts | S | None |
| #24 | Help System | S | None |

**Can run in parallel with Sprint 5-6**

---

## Success Metrics

### Provider Efficiency
| Metric | Baseline | Target | Measurement |
|--------|----------|--------|-------------|
| Dashboard load time | 4-6s | < 2s | Performance monitoring |
| Care gap closure time | 5 min | < 2 min | User session tracking |
| Results signing time | 3 min | < 1 min | Audit logs |
| Pre-visit prep time | 10 min | < 5 min | User survey |

### Quality Outcomes
| Metric | Baseline | Target | Measurement |
|--------|----------|--------|-------------|
| Care gap closure rate | 65% | 80% | Quality reports |
| Custom measures created | 0 | 10+/quarter | Database metrics |
| Provider NPS | N/A | >70 | User survey |
| AI CQL generation accuracy | N/A | >85% | Validation testing |

### Platform Adoption
| Metric | Baseline | Target | Measurement |
|--------|----------|--------|-------------|
| Daily active providers | N/A | 80% | Analytics |
| Pre-visit planning usage | 0% | 60% | Feature analytics |
| Insights dashboard visits | N/A | 3x/week | Analytics |

---

## Risk Mitigation

### Technical Risks
| Risk | Mitigation |
|------|------------|
| AI CQL generation quality | Start with templates, validate all generated CQL before execution |
| Performance degradation with large panels | Implement pagination, caching, async loading |
| FHIR query complexity | Use batch queries, optimize indexes, cache results |

### Compliance Risks
| Risk | Mitigation |
|------|------------|
| PHI exposure | All issues include HIPAA compliance checklist |
| Audit trail gaps | @Audited annotations mandatory for PHI access |
| Multi-tenant data leakage | Tenant filtering required in all queries |

### User Adoption Risks
| Risk | Mitigation |
|------|------------|
| Workflow disruption | Gradual rollout with feedback loops |
| Learning curve | Contextual help, tooltips, tutorials |
| Feature overload | Phased release, configurable dashboards |

---

## Next Steps

1. **Product Review**: PM reviews backlog, adjusts priorities based on provider feedback
2. **Technical Planning**: Architects review API contracts, identify shared components
3. **Sprint Planning**: Team estimates story points, commits to Sprint 1
4. **GitHub Setup**: Create issues with labels, link to milestones
5. **Kickoff**: Begin Issue #1 (Provider Panel Assignment API)

**Recommended Sprint 1 Focus**:
- Issue #1: Provider Panel Assignment API (Backend) - **Critical Path**
- Issue #3: FHIR Query Optimization (Backend) - **Performance Foundation**

---

*Generated with PromptCraft Elite Provider View Planning Prompt*
*Version: 1.0 | Date: January 6, 2026*
