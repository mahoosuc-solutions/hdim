# Dashboard User Stories

## Epic: Master Dashboard

### US-DB-001: View Dashboard Overview 🟢
**As a** clinical user,
**I want to** see a high-level overview of quality metrics on login,
**So that** I immediately understand the current state.

**Acceptance Criteria:**
- [ ] Dashboard loads as default view after login
- [ ] Key metrics displayed prominently
- [ ] Cached data for fast loading
- [ ] Refresh button to update

**Key Metrics Displayed:**
- Total patients
- Total evaluations
- Overall compliance rate
- Open care gaps

---

### US-DB-002: View Recent Activity 🟢
**As a** clinical user,
**I want to** see recent evaluation activity,
**So that** I know what's been happening.

**Acceptance Criteria:**
- [ ] List of 10 most recent evaluations
- [ ] Show: Patient, measure, date, outcome
- [ ] Click to view details
- [ ] Auto-refresh every 5 minutes

---

### US-DB-003: View Care Gap Summary 🟢
**As a** clinical user,
**I want to** see a summary of care gaps on the dashboard,
**So that** I'm aware of urgent gaps.

**Acceptance Criteria:**
- [ ] Top 10 care gaps by urgency
- [ ] Urgency-coded badges
- [ ] Quick action buttons
- [ ] Link to full care gap list

---

### US-DB-004: Switch Dashboard View 🟢
**As a** clinical user,
**I want to** switch between role-specific dashboard views,
**So that** I can see information relevant to my role.

**Acceptance Criteria:**
- [ ] Dashboard selector dropdown
- [ ] Options: Overview, Provider, RN, MA
- [ ] Remember last selection
- [ ] Role-appropriate access control

---

## Epic: Provider Dashboard

### US-DB-005: View High-Priority Care Gaps 🟢
**As a** provider,
**I want to** see high-priority care gaps requiring clinical decisions,
**So that** I can address them during patient visits.

**Acceptance Criteria:**
- [ ] Gaps requiring clinical action
- [ ] Grouped by risk level (critical, high, moderate)
- [ ] Patient name and gap type
- [ ] Days overdue
- [ ] Quick action: Address Gap

---

### US-DB-006: View Quality Measure Performance 🟢
**As a** provider,
**I want to** see my quality measure performance,
**So that** I can track my metrics.

**Acceptance Criteria:**
- [ ] Top measures displayed
- [ ] Performance % vs target
- [ ] Trend indicator (up/down/stable)
- [ ] Color-coded performance

---

### US-DB-007: View Pending Results 🟢
**As a** provider,
**I want to** see pending lab/imaging results requiring review,
**So that** I don't miss abnormal findings.

**Acceptance Criteria:**
- [ ] List of results needing review
- [ ] Abnormal results highlighted
- [ ] Patient name and result type
- [ ] Quick action: Review, Sign

---

### US-DB-008: View Today's Schedule 🟢
**As a** provider,
**I want to** see my appointment schedule for today,
**So that** I can prepare for visits.

**Acceptance Criteria:**
- [ ] Today's appointments listed
- [ ] Patient name and time
- [ ] Visit type
- [ ] Link to patient detail

---

## Epic: RN Dashboard

### US-DB-009: View Assigned Care Gaps 🟢
**As an** RN,
**I want to** see care gaps assigned to me,
**So that** I can prioritize my outreach.

**Acceptance Criteria:**
- [ ] Gaps assigned to current user
- [ ] Sorted by priority
- [ ] Category icons
- [ ] Quick action: Address, Complete

---

### US-DB-010: View Outreach Tasks 🟢
**As an** RN,
**I want to** see pending patient outreach tasks,
**So that** I can coordinate care.

**Acceptance Criteria:**
- [ ] Patients needing contact
- [ ] Contact method (call, email, letter)
- [ ] Last attempt date
- [ ] Quick action: Log Outreach

---

### US-DB-011: View Education Tasks 🟢
**As an** RN,
**I want to** see pending patient education tasks,
**So that** I can ensure patients receive education.

**Acceptance Criteria:**
- [ ] Patients needing education
- [ ] Education topic
- [ ] Priority level
- [ ] Quick action: Complete Education

---

### US-DB-012: Track Medication Reconciliation 🟢
**As an** RN,
**I want to** track medication reconciliation tasks,
**So that** patients have accurate medication lists.

**Acceptance Criteria:**
- [ ] Patients needing med rec
- [ ] Last reconciliation date
- [ ] Discrepancy alerts
- [ ] Quick action: Complete Med Rec

---

## Epic: MA Dashboard

### US-DB-013: View Today's Schedule 🟢
**As an** MA,
**I want to** see today's patient schedule,
**So that** I can prepare for visits.

**Acceptance Criteria:**
- [ ] All patients scheduled today
- [ ] Appointment time and type
- [ ] Check-in status
- [ ] Quick action: Check In

---

### US-DB-014: Track Check-In Status 🟢
**As an** MA,
**I want to** see which patients have checked in,
**So that** I know who's ready for vitals.

**Acceptance Criteria:**
- [ ] Check-in status indicator
- [ ] Time since check-in
- [ ] Room assignment
- [ ] Quick action: Assign Room

---

### US-DB-015: View Pre-Visit Tasks 🟢
**As an** MA,
**I want to** see pre-visit tasks for scheduled patients,
**So that** I can prepare the chart.

**Acceptance Criteria:**
- [ ] Tasks per patient
- [ ] Task type (forms, records, etc.)
- [ ] Completion status
- [ ] Quick action: Complete Task

---

### US-DB-016: Record Vitals 🟢
**As an** MA,
**I want to** see which patients need vitals recorded,
**So that** I can capture measurements.

**Acceptance Criteria:**
- [ ] Patients waiting for vitals
- [ ] Time in room
- [ ] Last vitals date
- [ ] Quick action: Record Vitals

---

## Epic: Dashboard Customization

### US-DB-017: Customize Dashboard Layout 🟡
**As a** clinical user,
**I want to** customize my dashboard layout,
**So that** I see the most relevant information.

**Acceptance Criteria:**
- [ ] Drag and drop widgets
- [ ] Show/hide widgets
- [ ] Resize widgets
- [ ] Save layout preferences

**Status:** Partial - Widget show/hide available

---

### US-DB-018: Set Dashboard Alerts 🟡
**As a** clinical user,
**I want to** configure dashboard alerts,
**So that** I'm notified of important events.

**Acceptance Criteria:**
- [ ] Configure alert thresholds
- [ ] Enable/disable alert types
- [ ] Choose notification method
- [ ] Alert history view

**Status:** Partial - Basic alerts available
