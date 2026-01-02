# Patient Management User Stories

## Epic: Patient Search & Discovery

### US-PM-001: Search Patients by Name 🟢
**As a** clinical user,
**I want to** search for patients by name with fuzzy matching,
**So that** I can find patients even with minor spelling variations.

**Acceptance Criteria:**
- [ ] Search matches partial names ("Jon" finds "John")
- [ ] Search is case-insensitive
- [ ] Results update as user types (debounced 300ms)
- [ ] Maximum 100 results returned
- [ ] Results sorted by relevance

**Technical Notes:**
- Component: `patients.component.ts`
- Service: `PatientService.searchPatients()`
- Debounce: 300ms with RxJS

---

### US-PM-002: Search Patients by MRN 🟢
**As a** clinical user,
**I want to** search for patients by Medical Record Number (MRN),
**So that** I can quickly locate a specific patient record.

**Acceptance Criteria:**
- [ ] Exact MRN match returns single result
- [ ] Partial MRN search supported
- [ ] MRN displayed prominently in results

---

### US-PM-003: Filter Patients by Demographics 🟢
**As a** clinical user,
**I want to** filter the patient list by gender, age range, and status,
**So that** I can focus on specific patient populations.

**Acceptance Criteria:**
- [ ] Filter by gender (Male, Female, Other, Unknown)
- [ ] Filter by age range (0-17, 18-64, 65+)
- [ ] Filter by status (Active, Inactive)
- [ ] Multiple filters can be combined
- [ ] Filter state persists in localStorage

**Technical Notes:**
- Service: `FilterPersistenceService`
- Storage key: `patient-filters`

---

### US-PM-004: Sort Patient List 🟢
**As a** clinical user,
**I want to** sort the patient list by name, age, or last evaluation date,
**So that** I can organize patients based on my workflow needs.

**Acceptance Criteria:**
- [ ] Sort by Full Name (A-Z, Z-A)
- [ ] Sort by Age (youngest first, oldest first)
- [ ] Sort by Date of Birth
- [ ] Sort by Last Evaluation Date
- [ ] Sort indicator visible on column header

---

## Epic: Patient Details

### US-PM-005: View Patient Demographics 🟢
**As a** clinical user,
**I want to** view complete patient demographic information,
**So that** I can verify patient identity and contact information.

**Acceptance Criteria:**
- [ ] Display: Name, MRN, DOB, Age, Gender
- [ ] Display: Address, Phone, Email
- [ ] Display: Primary Care Provider
- [ ] Display: Insurance Information
- [ ] Display: Emergency Contact

**Technical Notes:**
- Route: `/patients/:id`
- Component: `PatientDetailComponent`

---

### US-PM-006: View Patient Evaluation History 🟢
**As a** clinical user,
**I want to** view all quality measure evaluations for a patient,
**So that** I can understand their compliance history.

**Acceptance Criteria:**
- [ ] List all evaluations sorted by date (newest first)
- [ ] Show: Measure name, date, outcome
- [ ] Color-coded outcomes (green=compliant, red=non-compliant)
- [ ] Link to detailed evaluation result
- [ ] Pagination for large histories (10 per page)

---

### US-PM-007: View Patient Care Gaps 🟢
**As a** clinical user,
**I want to** see all open care gaps for a patient,
**So that** I can address gaps during the visit.

**Acceptance Criteria:**
- [ ] List all open care gaps
- [ ] Show urgency level with color coding
- [ ] Show gap type and measure name
- [ ] Show days overdue
- [ ] Quick action to close gap

---

## Epic: Master Patient Index (MPI)

### US-PM-008: Detect Duplicate Patients 🟢
**As an** administrator,
**I want to** automatically detect potential duplicate patient records,
**So that** I can maintain data integrity.

**Acceptance Criteria:**
- [ ] Auto-detect duplicates with 85%+ confidence
- [ ] Display match score for each potential duplicate
- [ ] Group duplicates under master record
- [ ] Show duplicate count badge on patient row

**Technical Notes:**
- Service: `PatientDeduplicationService`
- Threshold: 85% match confidence

---

### US-PM-009: Link Duplicate Records 🟢
**As an** administrator,
**I want to** link duplicate patient records to a master record,
**So that** all patient data is consolidated.

**Acceptance Criteria:**
- [ ] Select master record from duplicates
- [ ] Link selected duplicates to master
- [ ] Merge clinical data under master
- [ ] Preserve audit trail of links

---

### US-PM-010: View Master Records Only 🟢
**As a** clinical user,
**I want to** filter the patient list to show only master records,
**So that** I don't see duplicate entries.

**Acceptance Criteria:**
- [ ] Toggle: "Show Master Records Only"
- [ ] Duplicate records hidden when enabled
- [ ] Badge shows linked duplicate count

---

## Epic: Patient Data Export

### US-PM-011: Export Patient List to CSV 🟢
**As an** administrator,
**I want to** export the patient list to CSV format,
**So that** I can analyze data in external tools.

**Acceptance Criteria:**
- [ ] Export visible columns only
- [ ] Include: MRN, Name, DOB, Gender, Status
- [ ] Filename includes date/time
- [ ] UTF-8 encoding for special characters

---

### US-PM-012: Bulk Select Patients 🟢
**As a** clinical user,
**I want to** select multiple patients for bulk operations,
**So that** I can perform actions efficiently.

**Acceptance Criteria:**
- [ ] Checkbox for individual selection
- [ ] "Select All" checkbox in header
- [ ] Selection count displayed
- [ ] Bulk actions menu appears when selected
- [ ] Clear selection button

**Bulk Actions Available:**
- Export selected to CSV
- Run batch evaluation
- Assign to care manager
