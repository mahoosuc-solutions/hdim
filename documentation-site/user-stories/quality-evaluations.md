# Quality Evaluations User Stories

## Epic: Measure Selection

### US-QE-001: Browse Available Measures 🟢
**As a** clinical user,
**I want to** browse all available HEDIS quality measures,
**So that** I can select the appropriate measure for evaluation.

**Acceptance Criteria:**
- [ ] Display all 56+ HEDIS measures
- [ ] Show measure ID, name, and category
- [ ] Filter by category (Preventive, Chronic Disease, Behavioral Health, etc.)
- [ ] Only show measures with CQL libraries (evaluable measures)
- [ ] Search measures by name or ID

**Technical Notes:**
- Endpoint: `GET /evaluate/measures?evaluableOnly=true`
- Component: `evaluations.component.ts`

---

### US-QE-002: Filter Measures by Category 🟢
**As a** clinical user,
**I want to** filter quality measures by category,
**So that** I can quickly find relevant measures.

**Acceptance Criteria:**
- [ ] Categories: Preventive, Chronic Disease, Behavioral Health, Medication, Women's Health, Child/Adolescent
- [ ] Category filter persists during session
- [ ] Show count per category
- [ ] "All Categories" option to reset

---

### US-QE-003: Favorite Measures 🟢
**As a** clinical user,
**I want to** mark frequently used measures as favorites,
**So that** I can access them quickly.

**Acceptance Criteria:**
- [ ] Star icon to toggle favorite status
- [ ] Favorites appear in Quick Access panel
- [ ] Favorites persist in localStorage
- [ ] Maximum 10 favorites

**Technical Notes:**
- Service: `MeasureFavoritesService`
- Storage: localStorage

---

### US-QE-004: View Recent Measures 🟢
**As a** clinical user,
**I want to** see my recently used measures,
**So that** I can quickly repeat common evaluations.

**Acceptance Criteria:**
- [ ] Show last 5 measures used
- [ ] Displayed in Quick Access panel
- [ ] Click to select for new evaluation
- [ ] Updates automatically after each evaluation

---

## Epic: Single Patient Evaluation

### US-QE-005: Select Patient for Evaluation 🟢
**As a** clinical user,
**I want to** select a patient for quality measure evaluation,
**So that** I can assess their compliance.

**Acceptance Criteria:**
- [ ] Autocomplete search by name or MRN
- [ ] Show patient DOB and gender in dropdown
- [ ] Validate patient exists before submission
- [ ] Pre-populate from query parameter if provided

---

### US-QE-006: Submit Evaluation Request 🟢
**As a** clinical user,
**I want to** submit a quality measure evaluation for a patient,
**So that** their compliance status is calculated.

**Acceptance Criteria:**
- [ ] Submit button enabled when patient and measure selected
- [ ] Loading indicator during submission
- [ ] Success message with evaluation ID
- [ ] Error message with details on failure
- [ ] Result displayed immediately

**Technical Notes:**
- Endpoint: `POST /evaluate?library={measure}&patient={patientId}`
- Service: `EvaluationService.evaluatePatient()`

---

### US-QE-007: View Evaluation Result 🟢
**As a** clinical user,
**I want to** see the evaluation result immediately after submission,
**So that** I know the patient's compliance status.

**Acceptance Criteria:**
- [ ] Display outcome: Compliant, Non-Compliant, Not Eligible
- [ ] Color-coded result badge
- [ ] Show numerator/denominator status
- [ ] Show compliance rate percentage
- [ ] Show evaluation date and duration

**Result Display:**
| Outcome | Color | Description |
|---------|-------|-------------|
| Compliant | Green | Patient meets measure criteria |
| Non-Compliant | Red | Patient does not meet criteria |
| Not Eligible | Blue | Patient excluded from measure |

---

### US-QE-008: View Evaluation Details 🟢
**As a** clinical user,
**I want to** view detailed evaluation information,
**So that** I understand why the patient is/isn't compliant.

**Acceptance Criteria:**
- [ ] Show all evaluated criteria
- [ ] Show data used in calculation
- [ ] Show exclusion reason if not eligible
- [ ] Show care gap recommendations if non-compliant
- [ ] Link to relevant clinical data

---

## Epic: Evaluation History

### US-QE-009: View All Evaluations 🟢
**As a** clinical user,
**I want to** view all quality measure evaluations,
**So that** I can review historical results.

**Acceptance Criteria:**
- [ ] Paginated list (10, 25, 50, 100 per page)
- [ ] Sort by date (newest first default)
- [ ] Filter by outcome status
- [ ] Filter by measure category
- [ ] Filter by date range

---

### US-QE-010: Filter Evaluations by Outcome 🟢
**As a** clinical user,
**I want to** filter evaluations by outcome status,
**So that** I can focus on non-compliant patients.

**Acceptance Criteria:**
- [ ] Filter options: All, Compliant, Non-Compliant, Not Eligible
- [ ] Count shown per filter option
- [ ] Filter persists during session

---

### US-QE-011: Export Evaluations to CSV 🟢
**As a** clinical user,
**I want to** export evaluation results to CSV,
**So that** I can analyze data externally.

**Acceptance Criteria:**
- [ ] Export selected evaluations or all visible
- [ ] Include: Patient, Measure, Date, Outcome, Compliance Rate
- [ ] UTF-8 encoding
- [ ] Filename includes date

---

## Epic: Batch Evaluations

### US-QE-012: Run Batch Evaluation 🟢
**As a** clinical user,
**I want to** evaluate multiple patients at once,
**So that** I can efficiently assess population compliance.

**Acceptance Criteria:**
- [ ] Select multiple patients from list
- [ ] Select one or more measures
- [ ] Submit batch request
- [ ] Real-time progress updates via WebSocket
- [ ] Summary report on completion

**Technical Notes:**
- Uses WebSocket for progress updates
- Component: `BatchEvaluationDialogComponent`

---

### US-QE-013: Monitor Batch Progress 🟢
**As a** clinical user,
**I want to** see real-time progress of batch evaluations,
**So that** I know when the batch will complete.

**Acceptance Criteria:**
- [ ] Progress bar with percentage
- [ ] Count: X of Y patients evaluated
- [ ] Estimated time remaining
- [ ] Cancel button to abort batch
- [ ] Notification on completion

---

### US-QE-014: View Batch Results 🟢
**As a** clinical user,
**I want to** see aggregated results after batch evaluation,
**So that** I can understand population compliance.

**Acceptance Criteria:**
- [ ] Summary: Total evaluated, compliant, non-compliant, not eligible
- [ ] Compliance rate calculation
- [ ] Breakdown by measure
- [ ] Export results to CSV
- [ ] Link to individual results

---

### US-QE-015: Schedule Recurring Batch 🟡
**As an** administrator,
**I want to** schedule recurring batch evaluations,
**So that** compliance is calculated automatically.

**Acceptance Criteria:**
- [ ] Select patient population
- [ ] Select measures
- [ ] Set schedule (daily, weekly, monthly)
- [ ] Email notification on completion
- [ ] View scheduled batches

**Status:** Partial - Core batch works, scheduling in development
