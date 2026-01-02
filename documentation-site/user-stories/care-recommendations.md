# Care Recommendations User Stories

## Epic: Recommendation Discovery

### US-CR-001: View All Recommendations 🟢
**As a** clinical user,
**I want to** view all care recommendations across patients,
**So that** I can prioritize interventions.

**Acceptance Criteria:**
- [ ] Display all recommendations
- [ ] Show: Patient, category, urgency, status, due date
- [ ] Paginated list with configurable page size
- [ ] Sort by urgency, due date, patient name
- [ ] Total count displayed

**Technical Notes:**
- Component: `care-recommendations.component.ts`
- Store: NgRx for state management

---

### US-CR-002: Filter by Urgency 🟢
**As a** clinical user,
**I want to** filter recommendations by urgency level,
**So that** I can focus on time-sensitive items.

**Acceptance Criteria:**
- [ ] Urgency levels: Emergent, Urgent, Soon, Routine
- [ ] Multi-select filter
- [ ] Color-coded urgency badges:
  - Emergent: Red
  - Urgent: Orange
  - Soon: Yellow
  - Routine: Blue
- [ ] Count per urgency level

---

### US-CR-003: Filter by Category 🟢
**As a** clinical user,
**I want to** filter recommendations by category,
**So that** I can focus on specific care areas.

**Acceptance Criteria:**
- [ ] Categories: Preventive, Chronic Disease, Medication, Mental Health, SDOH
- [ ] Multi-select filter
- [ ] Category icons displayed
- [ ] Count per category

---

### US-CR-004: Filter by Patient Risk 🟢
**As a** clinical user,
**I want to** filter recommendations by patient risk level,
**So that** I can prioritize high-risk patients.

**Acceptance Criteria:**
- [ ] Risk levels: Critical, High, Moderate, Low
- [ ] Multi-select filter
- [ ] Risk badge displayed on patient
- [ ] Count per risk level

---

### US-CR-005: Filter by Status 🟢
**As a** clinical user,
**I want to** filter recommendations by status,
**So that** I can see what needs action.

**Acceptance Criteria:**
- [ ] Status options: Pending, In Progress, Completed, Declined
- [ ] Multi-select filter
- [ ] Status badge displayed
- [ ] Count per status

---

### US-CR-006: Search Recommendations 🟢
**As a** clinical user,
**I want to** search recommendations by patient name or MRN,
**So that** I can find specific patient recommendations.

**Acceptance Criteria:**
- [ ] Search by patient name (partial)
- [ ] Search by MRN
- [ ] Results update as user types
- [ ] Clear search button

---

## Epic: Recommendation Views

### US-CR-007: Switch View Modes 🟢
**As a** clinical user,
**I want to** switch between list, grid, and kanban views,
**So that** I can visualize recommendations in my preferred format.

**Acceptance Criteria:**
- [ ] List view: Traditional table format
- [ ] Grid view: Card-based layout
- [ ] Kanban view: Grouped by status columns
- [ ] View preference persists
- [ ] Toggle buttons in toolbar

---

### US-CR-008: Group Recommendations 🟢
**As a** clinical user,
**I want to** group recommendations by various dimensions,
**So that** I can see patterns.

**Acceptance Criteria:**
- [ ] Group by: Urgency, Category, Risk Level, Status
- [ ] Group headers with counts
- [ ] Expand/collapse groups
- [ ] Available in kanban view

---

## Epic: Recommendation Actions

### US-CR-009: Accept Recommendation 🟢
**As a** clinical user,
**I want to** accept a pending recommendation,
**So that** it moves to in-progress status.

**Acceptance Criteria:**
- [ ] "Accept" action available on pending items
- [ ] Status changes to "In Progress"
- [ ] Timestamp recorded
- [ ] User recorded as assignee

---

### US-CR-010: Complete Recommendation 🟢
**As a** clinical user,
**I want to** mark a recommendation as completed,
**So that** it's documented as addressed.

**Acceptance Criteria:**
- [ ] "Complete" action on in-progress items
- [ ] Add completion notes
- [ ] Status changes to "Completed"
- [ ] Completion date recorded

---

### US-CR-011: Decline Recommendation 🟢
**As a** clinical user,
**I want to** decline a recommendation with a reason,
**So that** inappropriate recommendations are documented.

**Acceptance Criteria:**
- [ ] "Decline" action available
- [ ] Select decline reason
- [ ] Add notes
- [ ] Status changes to "Declined"

---

### US-CR-012: Bulk Update Status 🟢
**As a** clinical user,
**I want to** update multiple recommendations at once,
**So that** I can process similar items efficiently.

**Acceptance Criteria:**
- [ ] Select multiple recommendations
- [ ] Bulk actions: Accept All, Complete All, Decline All
- [ ] Confirmation dialog with count
- [ ] Progress indicator

---

## Epic: Navigation

### US-CR-013: Navigate to Patient 🟢
**As a** clinical user,
**I want to** navigate to a patient's detail page from a recommendation,
**So that** I can review their full record.

**Acceptance Criteria:**
- [ ] Patient name is clickable link
- [ ] Opens patient detail page
- [ ] Recommendation context preserved

---

### US-CR-014: View Statistics Panel 🟢
**As a** clinical user,
**I want to** see summary statistics for recommendations,
**So that** I understand the overall workload.

**Acceptance Criteria:**
- [ ] Total recommendations
- [ ] Count by status
- [ ] Count by urgency
- [ ] Trends vs previous period
