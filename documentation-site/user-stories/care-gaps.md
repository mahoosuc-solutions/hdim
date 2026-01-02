# Care Gap Management User Stories

## Epic: Care Gap Discovery

### US-CG-001: View All Care Gaps 🟢
**As a** clinical user,
**I want to** view all identified care gaps across patients,
**So that** I can prioritize gap closure activities.

**Acceptance Criteria:**
- [ ] Display all open care gaps
- [ ] Show: Patient name, MRN, gap type, measure, urgency
- [ ] Paginated list (10, 25, 50, 100 per page)
- [ ] Sort by urgency (high first), date, patient name
- [ ] Total gap count displayed

**Technical Notes:**
- Component: `care-gap-manager.component.ts`
- Model: `CareGapAlert`

---

### US-CG-002: Filter Gaps by Urgency 🟢
**As a** clinical user,
**I want to** filter care gaps by urgency level,
**So that** I can focus on the most critical gaps first.

**Acceptance Criteria:**
- [ ] Filter options: All, High, Medium, Low
- [ ] Urgency calculated by days overdue:
  - High: >90 days overdue
  - Medium: 31-90 days overdue
  - Low: ≤30 days overdue
- [ ] Color-coded urgency badges
- [ ] Count per urgency level

---

### US-CG-003: Filter Gaps by Type 🟢
**As a** clinical user,
**I want to** filter care gaps by gap type,
**So that** I can focus on specific intervention categories.

**Acceptance Criteria:**
- [ ] Filter options: Screening, Medication, Lab, Assessment, Follow-up
- [ ] Gap type inferred from measure name
- [ ] Multiple types can be selected
- [ ] Count per type

---

### US-CG-004: Search Gaps by Patient 🟢
**As a** clinical user,
**I want to** search care gaps by patient name or MRN,
**So that** I can find gaps for a specific patient.

**Acceptance Criteria:**
- [ ] Search by patient name (partial match)
- [ ] Search by MRN (exact or partial)
- [ ] Results update as user types
- [ ] Clear search button

---

### US-CG-005: View Gap Details 🟢
**As a** clinical user,
**I want to** view detailed information about a care gap,
**So that** I understand what action is needed.

**Acceptance Criteria:**
- [ ] Show patient demographics
- [ ] Show measure description
- [ ] Show gap reason (why non-compliant)
- [ ] Show recommended action
- [ ] Show last evaluation date
- [ ] Show days overdue

---

## Epic: Care Gap Closure

### US-CG-006: Close Care Gap 🟢
**As a** clinical user,
**I want to** close a care gap with a reason,
**So that** the gap is documented as resolved.

**Acceptance Criteria:**
- [ ] Closure form opens on click
- [ ] Select closure reason:
  - Completed - Gap addressed
  - Not Applicable - Patient excluded
  - Patient Declined - Patient refused
  - Other - Custom reason
- [ ] Add closure notes
- [ ] Select closure date
- [ ] Reference document (optional)
- [ ] Gap removed from list on close

**Technical Notes:**
- Component: `CareGapClosureDialogComponent`
- Service: `CareGapService.closeGap()`

---

### US-CG-007: Bulk Close Care Gaps 🟢
**As a** clinical user,
**I want to** close multiple care gaps at once,
**So that** I can efficiently process similar gaps.

**Acceptance Criteria:**
- [ ] Checkbox to select multiple gaps
- [ ] "Bulk Close" button appears when selected
- [ ] Confirmation dialog with count
- [ ] Select single reason for all
- [ ] Progress indicator during closure
- [ ] Summary of closed gaps

---

### US-CG-008: Record Intervention 🟢
**As a** clinical user,
**I want to** record an intervention for a care gap,
**So that** the outreach attempt is documented.

**Acceptance Criteria:**
- [ ] Intervention types: Call, Email, Letter, Appointment, Referral, Note
- [ ] Record date and time
- [ ] Record outcome (Reached, No Answer, Left Message, etc.)
- [ ] Add notes
- [ ] Intervention history visible on gap

---

### US-CG-009: View Intervention History 🟢
**As a** clinical user,
**I want to** see all interventions for a care gap,
**So that** I know what outreach has been attempted.

**Acceptance Criteria:**
- [ ] List all interventions chronologically
- [ ] Show: Type, date, outcome, notes
- [ ] Show who recorded each intervention
- [ ] Count of interventions displayed

---

## Epic: Care Gap Analytics

### US-CG-010: View Gap Summary Statistics 🟢
**As a** clinical user,
**I want to** see summary statistics for care gaps,
**So that** I understand the overall gap situation.

**Acceptance Criteria:**
- [ ] Total open gaps
- [ ] Gaps by urgency (high/medium/low counts)
- [ ] Gaps by type (screening/medication/lab/etc.)
- [ ] Trends vs previous period
- [ ] Closure rate

---

### US-CG-011: View Dashboard Alerts 🟢
**As a** clinical user,
**I want to** see top care gap alerts on my dashboard,
**So that** I'm aware of urgent gaps immediately.

**Acceptance Criteria:**
- [ ] Top 10 most urgent gaps displayed
- [ ] High urgency gaps highlighted
- [ ] Quick action buttons
- [ ] Link to full care gap list

---

### US-CG-012: Track Closure Metrics 🟢
**As a** manager,
**I want to** track care gap closure metrics,
**So that** I can monitor team performance.

**Acceptance Criteria:**
- [ ] Gaps closed this period
- [ ] Average time to closure
- [ ] Closure rate by team member
- [ ] Comparison to benchmarks
- [ ] Trend chart

---

## Epic: Patient Care Gap View

### US-CG-013: View Patient's Care Gaps 🟢
**As a** clinical user,
**I want to** see all care gaps for a specific patient,
**So that** I can address them during the visit.

**Acceptance Criteria:**
- [ ] Navigate from patient detail
- [ ] List all open gaps for patient
- [ ] Quick close actions
- [ ] Link to related evaluations

---

### US-CG-014: Navigate to Patient from Gap 🟢
**As a** clinical user,
**I want to** navigate to a patient's detail page from a care gap,
**So that** I can review their full record.

**Acceptance Criteria:**
- [ ] "View Patient" button on gap row
- [ ] Opens patient detail in same tab
- [ ] Patient ID passed in URL
- [ ] Query parameter for gap context

**Technical Notes:**
- Route: `/patients/:patientId?gapId={gapId}`
- Fixed in v1.2.2 (was using incorrect route)
