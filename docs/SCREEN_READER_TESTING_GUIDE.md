# Screen Reader Testing Guide - Clinical Portal

**Date:** January 24, 2026
**Status:** ✅ **READY FOR TESTING**
**WCAG Target:** Level AA Compliance
**Context:** Task #4 - Screen reader compatibility testing with NVDA/JAWS

---

## Executive Summary

The Clinical Portal has comprehensive accessibility infrastructure with **404 ARIA attributes across 78 files**. This guide provides step-by-step procedures for validating screen reader compatibility using NVDA (free) and JAWS (commercial) on Windows.

**Current Accessibility Status:**
- ✅ Skip navigation links implemented
- ✅ ARIA labels on interactive elements (buttons, links, form fields)
- ✅ Keyboard navigation support (tabindex, enter/space handlers)
- ✅ Semantic HTML with proper landmark roles
- ✅ Focus management and indicators
- ⏳ Screen reader testing pending (this document)

---

## Screen Reader Setup

### NVDA (NonVisual Desktop Access) - Free

**Installation:**
1. Download from: https://www.nvaccess.org/download/
2. Run installer (NVDA 2024.x or later)
3. Choose "Install NVDA on this computer"
4. Launch NVDA

**Basic NVDA Commands:**
| Command | Action |
|---------|--------|
| `Insert + Down Arrow` | Start continuous reading (say all) |
| `Insert + F7` | Elements list (headings, links, landmarks) |
| `H` | Next heading |
| `Shift + H` | Previous heading |
| `D` | Next landmark region |
| `B` | Next button |
| `E` | Next edit field |
| `K` | Next link |
| `Tab` | Next focusable element |
| `Insert + T` | Read window title |
| `Insert + B` | Read status bar |
| `Insert + F12` | Date and time |

**NVDA Configuration:**
- Settings → Speech → Voice: Microsoft David Desktop (or eSpeak)
- Settings → Braille → Display: None (unless using braille display)
- Settings → Document Formatting: Enable "Report headings", "Report landmarks", "Report tables"

---

### JAWS (Job Access With Speech) - Commercial

**Installation:**
1. Download 90-day trial: https://support.freedomscientific.com/Downloads/JAWS
2. Run installer (JAWS 2024 or later)
3. Activate trial license
4. Launch JAWS

**Basic JAWS Commands:**
| Command | Action |
|---------|--------|
| `Insert + Down Arrow` | Say all (continuous reading) |
| `Insert + F6` | List headings |
| `Insert + F5` | List form fields |
| `Insert + F7` | List links |
| `R` | Next region/landmark |
| `H` | Next heading |
| `B` | Next button |
| `E` | Next edit box |
| `F` | Next form field |
| `Tab` | Next focusable element |
| `Insert + T` | Read window title |
| `Insert + Page Down` | Read current window |

**JAWS Configuration:**
- Options → Basics → Typing Echo: Characters
- Options → Basics → Reading: Highlight current word
- Options → Advanced → Web/HTML/PDF: Forms Mode Manual

---

## Testing Procedure

### 1. Browser Setup

**Recommended Browsers:**
- **Chrome** (best NVDA support)
- **Firefox** (good NVDA support)
- **Edge** (good JAWS support)

**Browser Configuration:**
1. Navigate to: `http://localhost:4200`
2. Ensure zoom is 100%
3. Clear cache and reload
4. Disable browser extensions (especially ad blockers)

---

### 2. Page-by-Page Testing Checklist

#### A. Login Page (`/login`)

**NVDA Tests:**
- [ ] Page announces title: "Login - Clinical Portal"
- [ ] Form fields announce labels: "Username", "Password"
- [ ] "Login" button is announced as clickable
- [ ] Error messages are announced when validation fails
- [ ] Tab order is logical (username → password → login button)

**JAWS Tests:**
- [ ] `Insert + F5` lists all form fields correctly
- [ ] Form field types announced (edit, password)
- [ ] Required fields announced as "required"
- [ ] Focus indicators visible on all fields

**Expected Behavior:**
```
Focus on Username field:
NVDA: "Username, edit, blank"
JAWS: "Username edit, type in text"

Tab to Password field:
NVDA: "Password, password edit, blank"
JAWS: "Password edit, type in text"

Enter invalid credentials:
NVDA: "Error: Invalid username or password"
JAWS: "Alert: Invalid username or password"
```

---

#### B. Dashboard (`/dashboard`)

**NVDA Tests:**
- [ ] Skip link announces: "Skip to main content, link"
- [ ] Activating skip link jumps to main content region
- [ ] Navigation menu announces: "Clinical Portal navigation"
- [ ] Each nav item announces icon + label + description
- [ ] Statistics cards announce values and labels
- [ ] Charts announce accessible descriptions (or data tables)
- [ ] "Say all" reads page content in logical order

**JAWS Tests:**
- [ ] `Insert + F6` lists all headings (Page title, section titles)
- [ ] `R` navigates through landmarks (navigation, main, complementary)
- [ ] `Insert + F7` lists all links in navigation
- [ ] Focus visible on all interactive elements

**Expected Behavior:**
```
Page load:
NVDA: "Dashboard - Clinical Portal, main landmark"
JAWS: "Dashboard, Clinical Portal, main region"

Navigate to first stat card:
NVDA: "Total Patients, 1,234, statistic"
JAWS: "Total Patients, 1,234"

Focus navigation menu item:
NVDA: "Patients, link, Patient Management - View and manage patient records"
JAWS: "Patients link, Patient Management, View and manage patient records"
```

---

#### C. Patients Page (`/patients`)

**NVDA Tests:**
- [ ] Page heading announces: "Patient Management, heading level 1"
- [ ] Search field announces: "Search patients by name or medical record number, searchbox"
- [ ] Filter dropdowns announce labels and values
- [ ] Table header row announces column names
- [ ] Table data cells announce row and column context
- [ ] Action buttons announce: "View full details for [Patient Name]"
- [ ] Pagination announces: "Page 1 of 10, showing 1 to 20 of 187 patients"

**JAWS Tests:**
- [ ] `Insert + F5` lists all form fields (search, filters)
- [ ] Table navigation: `Ctrl + Alt + Arrow Keys` to navigate cells
- [ ] Table headers read with each cell: "MRN, John Doe, 123456"
- [ ] Row actions announced with patient context

**Expected Behavior:**
```
Focus search field:
NVDA: "Search patients by name or medical record number, searchbox, edit"
JAWS: "Search by name or M R N, searchbox edit, type in text"

Focus first table row:
NVDA: "MRN 123456, Name John Doe, Date of Birth 1980-05-15, Age 45, row 1 of 20"
JAWS: "Row 1, M R N 123456, Name John Doe, Date of Birth May 15 1980, Age 45"

Focus action button:
NVDA: "View full details for John Doe, button"
JAWS: "View full details for John Doe, button"
```

---

#### D. Patient Detail Page (`/patients/:id`)

**NVDA Tests:**
- [ ] Page heading announces patient name
- [ ] Section headings announce: "Demographics", "Contact Information", "Clinical Summary"
- [ ] Data labels and values announced: "Name: John Doe", "MRN: 123456"
- [ ] Tabs announce: "Demographics, tab, 1 of 4, selected"
- [ ] Document viewer announces: "Clinical document viewer, region"
- [ ] Care gaps list announces count and items

**JAWS Tests:**
- [ ] `Ctrl + Insert + Arrow` navigates through sections
- [ ] `T` navigates through tabs
- [ ] Tab panel content announced when tab activated
- [ ] Focus returns to tab list after tabbing through panel

**Expected Behavior:**
```
Focus Demographics tab:
NVDA: "Demographics, tab, 1 of 4, selected"
JAWS: "Demographics tab, selected, 1 of 4"

Read patient name field:
NVDA: "Name: John Doe"
JAWS: "Name, John Doe"

Navigate to Care Gaps section:
NVDA: "Care Gaps, heading level 2, 3 open care gaps"
JAWS: "Care Gaps, heading level 2, 3 items"
```

---

#### E. Care Gaps Page (`/care-gaps`)

**NVDA Tests:**
- [ ] Filter panel announces: "Care gap filters, region"
- [ ] Status badges announce: "Open, status", "Closed, status"
- [ ] Priority indicators announce: "High priority, critical"
- [ ] Action buttons context-aware: "Address care gap for diabetes screening, patient John Doe"
- [ ] Expandable rows announce state: "collapsed" / "expanded"

**JAWS Tests:**
- [ ] `Insert + F5` lists all filter controls
- [ ] Expandable rows use proper aria-expanded
- [ ] Focus management when expanding rows
- [ ] Bulk actions toolbar announced when rows selected

**Expected Behavior:**
```
Focus care gap row:
NVDA: "Diabetes screening overdue, High priority, Patient John Doe, Age 65, button, collapsed"
JAWS: "Diabetes screening overdue, High priority, Patient John Doe, Age 65, button collapsed"

Expand row (Enter key):
NVDA: "Expanded, Care gap details, Recommended action: Schedule HbA1c test"
JAWS: "Expanded, Care gap details region, Recommended action, Schedule H b A 1 c test"
```

---

#### F. Quality Measures Page (`/quality-measures`)

**NVDA Tests:**
- [ ] Measure cards announce: "Diabetes HbA1c Control, quality measure"
- [ ] Compliance rate announces: "Compliance rate: 87.5%"
- [ ] Progress bars announce: "87.5% complete"
- [ ] Measure details dialog announces title and content
- [ ] CQL logic viewer announces code with syntax

**JAWS Tests:**
- [ ] `Insert + F7` lists all measure links
- [ ] Measure metadata announced (numerator, denominator, exclusions)
- [ ] Charts have accessible data table alternatives
- [ ] Modal dialogs announce title and role

**Expected Behavior:**
```
Focus measure card:
NVDA: "Diabetes HbA1c Control, quality measure, Compliance rate 87.5%, 175 compliant, 25 non-compliant"
JAWS: "Diabetes H b A 1 c Control, quality measure, Compliance rate 87 point 5 percent"

Open measure details:
NVDA: "Measure details dialog, Diabetes HbA1c Control, heading level 1"
JAWS: "Measure details dialog, Diabetes H b A 1 c Control heading level 1"
```

---

### 3. Cross-Cutting Feature Tests

#### Navigation and Landmarks

**Tests:**
- [ ] Skip links work (focus jumps to main content)
- [ ] `D` (NVDA) or `R` (JAWS) navigates through landmarks
- [ ] Landmarks announced with proper names: "navigation", "main", "complementary"
- [ ] Breadcrumbs announce current page and navigation path

**Expected Landmarks:**
```
Dashboard page:
1. "Skip to main content, link" (skip link)
2. "Clinical Portal navigation, navigation landmark"
3. "Main content, main landmark"
4. "Statistics summary, complementary landmark"
```

---

#### Forms and Validation

**Tests:**
- [ ] All form fields have associated labels
- [ ] Required fields announced as "required"
- [ ] Error messages linked to fields with `aria-describedby`
- [ ] Inline validation errors announced immediately
- [ ] Form submission success announced

**Error Announcement Test:**
```html
<input aria-describedby="email-error" aria-invalid="true">
<span id="email-error" role="alert">Invalid email format</span>

Expected:
NVDA: "Email, edit, invalid entry, Invalid email format"
JAWS: "Email edit, invalid entry, Invalid email format"
```

---

#### Tables

**Tests:**
- [ ] Table headers (`<th>`) associated with data cells
- [ ] Column headers announced with each cell
- [ ] Row headers announced for multi-dimensional tables
- [ ] Sortable columns announce sort state: "sorted ascending"
- [ ] Empty tables announce: "No data available"

**Table Navigation Test:**
```
Navigate Patient table:
NVDA: Use arrow keys, announces "MRN column, row 1, 123456"
JAWS: Ctrl+Alt+Arrow keys, announces "M R N, 123456"

Sort by Name column:
NVDA: "Name, column header, sorted ascending, button"
JAWS: "Name column header, sorted ascending button"
```

---

#### Buttons and Links

**Tests:**
- [ ] All buttons have accessible names (text, `aria-label`, or `aria-labelledby`)
- [ ] Icon-only buttons have `aria-label`
- [ ] Links announce purpose: "View patient details for John Doe"
- [ ] Disabled buttons announced as "unavailable" or "disabled"
- [ ] Loading states announced: "Loading, please wait"

**Icon Button Test:**
```html
<button mat-icon-button aria-label="View full details for John Doe">
  <mat-icon>visibility</mat-icon>
</button>

Expected:
NVDA: "View full details for John Doe, button"
JAWS: "View full details for John Doe button"
```

---

#### Dynamic Content Updates

**Tests:**
- [ ] Loading spinners announce: "Loading patients, please wait"
- [ ] Live regions (`aria-live="polite"`) announce updates
- [ ] Error banners announced immediately with `role="alert"`
- [ ] Success messages announced
- [ ] Auto-save status announced: "Changes saved automatically"

**Live Region Test:**
```html
<div role="status" aria-live="polite">
  Loading patients...
</div>

Expected:
NVDA: (after brief delay) "Loading patients"
JAWS: (after brief delay) "Loading patients"
```

---

### 4. Automated Accessibility Audit

**Using axe DevTools:**

1. Install browser extension:
   - Chrome: https://chrome.google.com/webstore/detail/axe-devtools-web-accessibility-testing/lhdoppojpmngadmnindnejefpokejbdd
   - Firefox: https://addons.mozilla.org/en-US/firefox/addon/axe-devtools/

2. Run scan on each page:
   - Open DevTools (F12)
   - Navigate to "axe DevTools" tab
   - Click "Scan ALL of my page"
   - Review violations and best practices

3. Document violations:
   ```
   Page: /patients
   Critical Issues: 0
   Serious Issues: 0
   Moderate Issues: 2 (missing aria-label on icon buttons)
   Minor Issues: 5 (color contrast warnings)
   ```

**Using WAVE (WebAIM):**

1. Install browser extension:
   - Chrome: https://chrome.google.com/webstore/detail/wave-evaluation-tool/jbbplnpkjmmeebjpijfedlgcdilocofh
   - Firefox: https://addons.mozilla.org/en-US/firefox/addon/wave-accessibility-tool/

2. Run evaluation:
   - Click WAVE icon
   - Review errors (red), alerts (yellow), features (green)
   - Check color contrast

---

### 5. Keyboard Navigation Testing

**Global Keyboard Tests:**
- [ ] `Tab` moves focus forward through interactive elements
- [ ] `Shift + Tab` moves focus backward
- [ ] `Enter` activates buttons and links
- [ ] `Space` activates buttons (not links)
- [ ] `Escape` closes dialogs and menus
- [ ] `Arrow keys` navigate through menus, tabs, radio groups
- [ ] No keyboard traps (can always Tab out)
- [ ] Focus visible on all elements (outline or highlight)

**Page-Specific Keyboard Tests:**

**Patients Table:**
- [ ] `Tab` moves through filters, search, table rows, pagination
- [ ] `Enter` on row opens patient details
- [ ] `Space` on checkbox selects row
- [ ] `Arrow keys` navigate table cells (JAWS table mode)

**Care Gaps:**
- [ ] `Enter` expands/collapses care gap details
- [ ] `Tab` moves through expanded content
- [ ] `Escape` collapses expanded row

**Quality Measures:**
- [ ] `Enter` on measure card opens details dialog
- [ ] `Escape` closes dialog
- [ ] `Tab` cycles through dialog content
- [ ] Focus returns to trigger button on close

---

### 6. Mobile Screen Reader Testing (Optional)

**iOS VoiceOver:**
1. Settings → Accessibility → VoiceOver → Enable
2. Navigate with swipe gestures
3. Test responsive design on iPhone/iPad

**Android TalkBack:**
1. Settings → Accessibility → TalkBack → Enable
2. Navigate with swipe gestures
3. Test responsive design on Android devices

---

## Common Issues and Fixes

### Issue 1: Icon-Only Buttons Not Announced

**Problem:**
```html
<button mat-icon-button (click)="deletePatient()">
  <mat-icon>delete</mat-icon>
</button>
```
**Screen reader says:** "Button" (no context)

**Fix:**
```html
<button mat-icon-button aria-label="Delete patient John Doe" (click)="deletePatient()">
  <mat-icon aria-hidden="true">delete</mat-icon>
</button>
```
**Screen reader says:** "Delete patient John Doe, button"

---

### Issue 2: Table Action Buttons Missing Context

**Problem:**
```html
<button mat-icon-button (click)="viewPatient(patient)">
  <mat-icon>visibility</mat-icon>
</button>
```
**Screen reader says:** "Button" (which patient?)

**Fix:**
```html
<button mat-icon-button [attr.aria-label]="'View patient ' + patient.fullName">
  <mat-icon aria-hidden="true">visibility</mat-icon>
</button>
```
**Screen reader says:** "View patient John Doe, button"

---

### Issue 3: Loading States Not Announced

**Problem:**
```html
<div *ngIf="loading">
  <mat-spinner></mat-spinner>
</div>
```
**Screen reader says:** (nothing)

**Fix:**
```html
<div *ngIf="loading" role="status" aria-live="polite">
  <mat-spinner></mat-spinner>
  <span class="sr-only">Loading patients, please wait...</span>
</div>

<!-- CSS -->
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
```
**Screen reader says:** "Loading patients, please wait"

---

### Issue 4: Form Errors Not Linked to Fields

**Problem:**
```html
<input matInput [(ngModel)]="patientName">
<div *ngIf="errors.name" class="error">Name is required</div>
```
**Screen reader says:** (error not announced)

**Fix:**
```html
<input matInput
       [(ngModel)]="patientName"
       [attr.aria-describedby]="errors.name ? 'name-error' : null"
       [attr.aria-invalid]="errors.name ? 'true' : null">
<div *ngIf="errors.name" id="name-error" role="alert">Name is required</div>
```
**Screen reader says:** "Patient name, edit, invalid entry, Name is required"

---

## Testing Results Template

```markdown
# Screen Reader Testing Results - Clinical Portal

**Date:** [DATE]
**Tester:** [NAME]
**Screen Reader:** NVDA 2024.1 / JAWS 2024
**Browser:** Chrome 120

## Summary

- **Pages Tested:** 6
- **Critical Issues:** 0
- **Serious Issues:** 2
- **Moderate Issues:** 5
- **Minor Issues:** 10
- **WCAG 2.1 Level AA Compliance:** ✅ Pass / ⚠️ Pass with exceptions / ❌ Fail

## Detailed Results

### Dashboard (`/dashboard`)

**Status:** ✅ PASS

**NVDA Tests:**
- [x] Skip link works
- [x] Navigation announces properly
- [x] Statistics cards readable
- [ ] Charts need accessible descriptions

**Issues Found:**
- Minor: Chart lacks aria-label (adds context for screen reader users)

---

### Patients Page (`/patients`)

**Status:** ⚠️ PASS WITH EXCEPTIONS

**NVDA Tests:**
- [x] Search field announces properly
- [x] Table navigation works
- [ ] Action buttons missing context for 4/8 buttons

**Issues Found:**
- Serious: "View", "Edit", "Delete" buttons in table lack patient context

**Recommended Fixes:**
- Add aria-label to all action buttons: "View patient [Name]"

---

[Continue for all pages...]
```

---

## Next Steps

1. **Immediate Testing (1-2 hours):**
   - Install NVDA
   - Test Dashboard, Patients, Care Gaps pages
   - Document critical issues

2. **Comprehensive Testing (4-6 hours):**
   - Test all 6 major pages
   - Run automated audits (axe, WAVE)
   - Document all issues with severity ratings

3. **Remediation (varies by findings):**
   - Fix critical issues immediately
   - Plan serious issues for next sprint
   - Defer moderate/minor issues

4. **Validation Testing:**
   - Re-test fixed issues
   - Verify WCAG 2.1 Level AA compliance
   - Update accessibility documentation

---

## Resources

**Tools:**
- NVDA: https://www.nvaccess.org/
- JAWS: https://www.freedomscientific.com/products/software/jaws/
- axe DevTools: https://www.deque.com/axe/devtools/
- WAVE: https://wave.webaim.org/extension/

**Documentation:**
- WCAG 2.1 Guidelines: https://www.w3.org/WAI/WCAG21/quickref/
- ARIA Practices: https://www.w3.org/WAI/ARIA/apg/
- WebAIM Screen Reader Testing: https://webaim.org/articles/screenreader_testing/

**Training:**
- NVDA User Guide: https://www.nvaccess.org/files/nvda/documentation/userGuide.html
- JAWS Training: https://www.freedomscientific.com/training/jaws/

---

**Status:** ✅ **READY FOR TESTING**
**Estimated Testing Time:** 1-2 hours (basic), 4-6 hours (comprehensive)
**Recommended Next Step:** Install NVDA, test Dashboard and Patients pages, document findings

---

_Document Created:_ January 24, 2026
_Author:_ Claude Code
_Task:_ #4 - Screen reader testing with NVDA/JAWS
