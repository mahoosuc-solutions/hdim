# Accessibility Audit Report

**Application:** HealthData-in-Motion Clinical Portal
**Version:** 1.0.0
**Audit Date:** _____________
**Auditor:** _____________
**Standard:** WCAG 2.1 Level AA

---

## Executive Summary

This document provides a comprehensive accessibility audit for the HealthData-in-Motion Clinical Portal. The application must meet WCAG 2.1 Level AA standards to ensure compliance with accessibility regulations and provide equal access to all users, including those with disabilities.

**Target Compliance:** WCAG 2.1 Level AA
**Current Status:** ___ % Compliant
**Critical Issues:** ___
**High Priority Issues:** ___
**Medium Priority Issues:** ___
**Low Priority Issues:** ___

---

## 1. Automated Accessibility Testing

### 1.1 Setup and Installation

```bash
# Navigate to project root
cd /home/webemo-aaron/projects/healthdata-in-motion

# Install automated testing tools
npm install -D @axe-core/cli lighthouse pa11y axe-core @axe-core/playwright

# Install globally for command-line usage
npm install -g @axe-core/cli lighthouse pa11y
```

### 1.2 Running Axe Accessibility Tests

**Command-line testing:**

```bash
# Start the application first
cd apps/clinical-portal
npm start
# Application runs on http://localhost:4200

# In another terminal, run axe tests
npx axe http://localhost:4200 --save axe-results.json

# Test specific pages
npx axe http://localhost:4200/dashboard --save axe-dashboard.json
npx axe http://localhost:4200/patients --save axe-patients.json
npx axe http://localhost:4200/evaluations --save axe-evaluations.json
npx axe http://localhost:4200/results --save axe-results-page.json
npx axe http://localhost:4200/reports --save axe-reports.json
npx axe http://localhost:4200/measure-builder --save axe-measure-builder.json
```

**Interpreting results:**
- **Critical:** Must fix immediately (impacts core functionality)
- **Serious:** Should fix (significant accessibility barrier)
- **Moderate:** Should fix when possible
- **Minor:** Nice to fix (minor improvements)

### 1.3 Running Lighthouse Audits

```bash
# Run full Lighthouse audit
npx lighthouse http://localhost:4200 \
  --output html \
  --output json \
  --output-path ./lighthouse-report \
  --chrome-flags="--headless" \
  --only-categories=accessibility

# Run for specific pages
npx lighthouse http://localhost:4200/dashboard --output html --output-path ./lighthouse-dashboard.html --only-categories=accessibility
npx lighthouse http://localhost:4200/patients --output html --output-path ./lighthouse-patients.html --only-categories=accessibility
npx lighthouse http://localhost:4200/reports --output html --output-path ./lighthouse-reports.html --only-categories=accessibility
```

**Expected Lighthouse Accessibility Score:** 90+ / 100

**Key metrics:**
- Proper heading structure
- Sufficient color contrast
- ARIA attributes used correctly
- Form labels associated
- Images have alt text

### 1.4 Running Pa11y Tests

```bash
# Install Pa11y globally
npm install -g pa11y

# Run Pa11y with WCAG 2.1 AA standard
pa11y http://localhost:4200 --standard WCAG2AA --reporter cli

# Run on all pages and save results
pa11y http://localhost:4200/dashboard --standard WCAG2AA > pa11y-dashboard.txt
pa11y http://localhost:4200/patients --standard WCAG2AA > pa11y-patients.txt
pa11y http://localhost:4200/evaluations --standard WCAG2AA > pa11y-evaluations.txt
pa11y http://localhost:4200/results --standard WCAG2AA > pa11y-results.txt
pa11y http://localhost:4200/reports --standard WCAG2AA > pa11y-reports.txt
pa11y http://localhost:4200/measure-builder --standard WCAG2AA > pa11y-measure-builder.txt

# Run with screenshots
pa11y http://localhost:4200 --standard WCAG2AA --reporter cli --screen-capture screenshots/
```

### 1.5 Automated Test Results Summary

| Page | Axe Score | Lighthouse Score | Pa11y Issues | Status |
|------|-----------|------------------|--------------|--------|
| Dashboard | ___ | ___ | ___ | [ ] |
| Patients | ___ | ___ | ___ | [ ] |
| Evaluations | ___ | ___ | ___ | [ ] |
| Results | ___ | ___ | ___ | [ ] |
| Reports | ___ | ___ | ___ | [ ] |
| Measure Builder | ___ | ___ | ___ | [ ] |

**Common Issues Found:**
- [ ] _______________________________
- [ ] _______________________________
- [ ] _______________________________

---

## 2. Manual Accessibility Testing

### 2.1 Keyboard Navigation

**Test Procedure:**
1. Disconnect mouse
2. Use only keyboard to navigate
3. Document any functionality that's inaccessible

**Tab Key Navigation:**

**Dashboard:**
- [ ] Tab moves focus to first interactive element
- [ ] Tab order is logical (top to bottom, left to right)
- [ ] All interactive elements reachable via Tab
- [ ] Shift+Tab moves focus backward
- [ ] No keyboard traps
- [ ] Skip navigation link present and functional
- [ ] Focus visible on all elements

**Patients Page:**
- [ ] Can tab to search/filter inputs
- [ ] Can tab to table rows
- [ ] Can tab to checkboxes
- [ ] Can tab to action buttons
- [ ] Can tab to pagination controls
- [ ] Master toggle checkbox accessible
- [ ] Bulk actions toolbar accessible

**Evaluations Page:**
- [ ] Can navigate to all filters
- [ ] Date pickers keyboard accessible
- [ ] Dropdowns navigable with keyboard
- [ ] Can activate "Run Evaluation" button
- [ ] Form submission possible via keyboard

**Results Page:**
- [ ] Can navigate table
- [ ] Can access filter controls
- [ ] Export buttons accessible
- [ ] Charts navigable (if interactive)

**Reports Page:**
- [ ] Can navigate report cards
- [ ] Can open report details
- [ ] Can trigger export actions
- [ ] Dialog modals keyboard accessible

**Measure Builder:**
- [ ] Monaco editor keyboard accessible
- [ ] Can tab through all editor controls
- [ ] Can use keyboard shortcuts in editor
- [ ] Test button accessible
- [ ] Results panel keyboard navigable

**Enter/Space Key Activation:**
- [ ] **Enter** activates buttons
- [ ] **Space** activates buttons
- [ ] **Space** toggles checkboxes
- [ ] **Enter** submits forms
- [ ] **Space** opens dropdowns

**Arrow Key Navigation:**
- [ ] Arrow keys navigate dropdown options
- [ ] Arrow keys navigate date picker
- [ ] Arrow keys navigate radio button groups
- [ ] Arrow keys navigate tabs (if applicable)

**Escape Key:**
- [ ] Escape closes dialogs
- [ ] Escape closes dropdowns
- [ ] Escape cancels inline edits
- [ ] Escape clears search (optional)

**Focus Management:**
- [ ] Focus moves to dialog when opened
- [ ] Focus trapped within dialog
- [ ] Focus returns to trigger element on dialog close
- [ ] Focus visible after page load
- [ ] Focus not lost during dynamic updates
- [ ] Focus indicator has 3:1 contrast ratio

**Keyboard Shortcuts (if implemented):**
- [ ] Shortcuts documented
- [ ] Shortcuts don't conflict with browser/screen reader
- [ ] Shortcuts customizable or disableable
- [ ] Shortcut help available (? key or similar)

### 2.2 Screen Reader Testing

**Testing with NVDA (Windows - Free):**

```
Download: https://www.nvaccess.org/download/
```

**Testing with JAWS (Windows - Commercial):**

```
Download: https://www.freedomscientific.com/products/software/jaws/
```

**Testing with VoiceOver (macOS/iOS - Built-in):**

```
Enable: System Preferences > Accessibility > VoiceOver
Shortcut: Cmd + F5
```

**Testing with TalkBack (Android - Built-in):**

```
Enable: Settings > Accessibility > TalkBack
```

#### Screen Reader Test Checklist

**General Navigation:**
- [ ] Page title announced on load
- [ ] Heading structure makes sense when announced
- [ ] Landmarks (navigation, main, aside, footer) identified
- [ ] Lists properly identified as lists
- [ ] Tables identified as tables
- [ ] Table headers associated with cells

**Interactive Elements:**
- [ ] Buttons announced as buttons with clear labels
- [ ] Links announced as links with descriptive text
- [ ] Form inputs announced with labels
- [ ] Checkboxes announced with state (checked/unchecked)
- [ ] Radio buttons announced with state
- [ ] Dropdowns announced with selected value
- [ ] Required fields announced

**Dynamic Content:**
- [ ] Live regions announce updates
- [ ] Loading states announced
- [ ] Error messages announced
- [ ] Success messages announced
- [ ] Modal dialogs announced
- [ ] Toast notifications announced
- [ ] Progress indicators announced

**Images and Icons:**
- [ ] Decorative images have empty alt text or aria-hidden
- [ ] Informative images have descriptive alt text
- [ ] Icon buttons have accessible labels (aria-label)
- [ ] Logo has appropriate alt text
- [ ] Charts have text alternatives or detailed descriptions

**Tables:**
- [ ] Table caption announced
- [ ] Column headers announced
- [ ] Row headers announced (if applicable)
- [ ] Cell contents announced clearly
- [ ] Table navigation works (if complex table)

**Forms:**
- [ ] Form purpose announced
- [ ] Field labels announced
- [ ] Required fields indicated
- [ ] Error messages associated with fields
- [ ] Field hints/descriptions announced
- [ ] Submit button clearly labeled

**Specific Page Tests:**

**Dashboard (VoiceOver/NVDA):**
- [ ] "Dashboard" heading announced
- [ ] Metric cards announced with values
- [ ] Charts have text alternatives
- [ ] Navigation landmarks identified
- [ ] Interactive widgets accessible

**Patients Page:**
- [ ] Table announced as table
- [ ] Column headers announced
- [ ] Row data announced clearly
- [ ] Checkbox state announced
- [ ] "X of Y patients" announced
- [ ] Pagination controls labeled

**Dialogs:**
- [ ] Dialog role announced
- [ ] Dialog title announced
- [ ] Form fields within dialog accessible
- [ ] Close button announced
- [ ] Action buttons clearly labeled

### 2.3 Color Contrast Testing

**Tools:**
- Chrome DevTools: Lighthouse audit
- Color Contrast Analyzer: https://www.tpgi.com/color-contrast-checker/
- WebAIM Contrast Checker: https://webaim.org/resources/contrastchecker/

**WCAG 2.1 Level AA Requirements:**
- Normal text (< 18pt): 4.5:1 contrast ratio
- Large text (≥ 18pt or 14pt bold): 3:1 contrast ratio
- UI components and graphics: 3:1 contrast ratio

**Elements to Test:**

**Text Elements:**
- [ ] Body text (#333333 on #FFFFFF): ___ : 1 | Pass: [ ]
- [ ] Headings: ___ : 1 | Pass: [ ]
- [ ] Links (default): ___ : 1 | Pass: [ ]
- [ ] Links (hover): ___ : 1 | Pass: [ ]
- [ ] Button text: ___ : 1 | Pass: [ ]
- [ ] Table headers: ___ : 1 | Pass: [ ]
- [ ] Table cells: ___ : 1 | Pass: [ ]
- [ ] Form labels: ___ : 1 | Pass: [ ]
- [ ] Placeholder text: ___ : 1 | Pass: [ ]
- [ ] Helper text: ___ : 1 | Pass: [ ]
- [ ] Error messages: ___ : 1 | Pass: [ ]
- [ ] Success messages: ___ : 1 | Pass: [ ]

**UI Components:**
- [ ] Primary button: ___ : 1 | Pass: [ ]
- [ ] Secondary button: ___ : 1 | Pass: [ ]
- [ ] Button hover state: ___ : 1 | Pass: [ ]
- [ ] Input borders: ___ : 1 | Pass: [ ]
- [ ] Input focus outline: ___ : 1 | Pass: [ ]
- [ ] Checkbox border: ___ : 1 | Pass: [ ]
- [ ] Selected row highlight: ___ : 1 | Pass: [ ]
- [ ] Active/selected state: ___ : 1 | Pass: [ ]
- [ ] Disabled state: ___ : 1 | Pass: [ ] (or visually distinct)

**Charts:**
- [ ] Chart colors distinguishable: [ ]
- [ ] Chart text readable: ___ : 1 | Pass: [ ]
- [ ] Chart legend readable: ___ : 1 | Pass: [ ]
- [ ] Chart colors accessible to colorblind users: [ ]

**Icons:**
- [ ] Icon-only buttons have sufficient contrast: ___ : 1 | Pass: [ ]
- [ ] Icons paired with text or labels: [ ]

**Test with Color Blindness Simulators:**
- [ ] Test with Protanopia (red-blind)
- [ ] Test with Deuteranopia (green-blind)
- [ ] Test with Tritanopia (blue-blind)
- [ ] All information still perceivable

**Tool:** Chrome extension "Colorblinding" or "Let's get color blind"

### 2.4 ARIA Compliance

**ARIA Landmark Roles:**
- [ ] `<header>` has role="banner" or is <header> element
- [ ] `<nav>` has role="navigation" or is <nav> element
- [ ] `<main>` has role="main" or is <main> element
- [ ] `<aside>` has role="complementary" or is <aside> element
- [ ] `<footer>` has role="contentinfo" or is <footer> element

**ARIA Labels and Descriptions:**
- [ ] Icon-only buttons have `aria-label`
- [ ] Form inputs have associated labels (explicit or aria-label)
- [ ] Complex widgets have `aria-describedby` for additional context
- [ ] Decorative images have `aria-hidden="true"` or empty alt
- [ ] Informative images have alt text or `aria-label`

**ARIA States:**
- [ ] Expandable sections have `aria-expanded="true/false"`
- [ ] Toggles have `aria-pressed="true/false"`
- [ ] Checkboxes have `aria-checked="true/false/mixed"`
- [ ] Disabled elements have `aria-disabled="true"` or disabled attribute
- [ ] Hidden content has `aria-hidden="true"` or hidden attribute

**ARIA Live Regions:**
- [ ] Toast notifications in `aria-live="polite"` region
- [ ] Critical alerts in `aria-live="assertive"` region
- [ ] Loading indicators have `aria-busy="true"`
- [ ] Status updates announced via `role="status"`
- [ ] Error summaries in `role="alert"`

**ARIA for Tables:**
- [ ] Sortable columns have `aria-sort="ascending/descending/none"`
- [ ] Row selection has `aria-selected="true/false"`
- [ ] Grid widgets use `role="grid"` with proper structure

**ARIA for Dialogs:**
- [ ] Dialogs have `role="dialog"` or `role="alertdialog"`
- [ ] Dialog title has `aria-labelledby`
- [ ] Dialog description has `aria-describedby`
- [ ] Focus trapped within dialog
- [ ] `aria-modal="true"` on dialog

**ARIA for Forms:**
- [ ] Required fields have `aria-required="true"`
- [ ] Invalid fields have `aria-invalid="true"`
- [ ] Error messages have `aria-describedby` linking to field
- [ ] Field groups use `<fieldset>` and `<legend>` or `role="group"` with `aria-labelledby`

**Custom Widgets:**
- [ ] Proper ARIA roles used
- [ ] Keyboard interaction follows ARIA Authoring Practices Guide
- [ ] State changes announced

**Review for Over-Use of ARIA:**
- [ ] ARIA only used when semantic HTML insufficient
- [ ] No ARIA better than bad ARIA
- [ ] Roles don't conflict with native semantics

### 2.5 Semantic HTML

**Document Structure:**
- [ ] Proper DOCTYPE
- [ ] Language declared (`<html lang="en">`)
- [ ] Page title descriptive and unique per page
- [ ] Meaningful heading hierarchy (h1 -> h2 -> h3, no skipped levels)
- [ ] Only one h1 per page

**Heading Structure Outline:**

**Dashboard:**
```
h1: Dashboard
  h2: Key Metrics
  h2: Compliance Trends
  h2: Recent Activity
```
- [ ] Verified correct

**Patients:**
```
h1: Patients
  h2: Patient List (or visually-hidden)
  h2: Filters (or visually-hidden)
```
- [ ] Verified correct

**Forms:**
- [ ] `<form>` element used
- [ ] `<label>` elements associated with inputs
  - [ ] Explicit association (for attribute)
  - [ ] Or implicit (label wraps input)
- [ ] `<fieldset>` and `<legend>` for grouped inputs
- [ ] `<button>` for buttons (not `<div>` with click handler)
- [ ] Appropriate input types (email, tel, date, number)

**Lists:**
- [ ] Navigation uses `<ul>` and `<li>`
- [ ] Unordered lists use `<ul>`
- [ ] Ordered lists use `<ol>`
- [ ] Description lists use `<dl>`, `<dt>`, `<dd>`

**Tables:**
- [ ] Data tables use `<table>`
- [ ] Table headers use `<th>`
- [ ] Table data cells use `<td>`
- [ ] `<caption>` provides table summary
- [ ] `scope` attribute on headers (col/row)
- [ ] Complex tables use `id` and `headers` attributes

**Links vs Buttons:**
- [ ] Links (`<a>`) navigate to different page/location
- [ ] Buttons (`<button>`) perform action on current page
- [ ] No `<div>` or `<span>` with click handlers masquerading as buttons

**Images:**
- [ ] `<img>` elements have alt attribute
- [ ] Decorative images have alt=""
- [ ] Informative images have descriptive alt text
- [ ] Complex images have longer descriptions (aria-describedby)

---

## 3. WCAG 2.1 Level AA Checklist

### Principle 1: Perceivable

**1.1 Text Alternatives**
- [ ] 1.1.1 Non-text Content: All images, icons, and graphics have appropriate text alternatives (Level A)

**1.2 Time-based Media**
- [ ] 1.2.1 Audio-only and Video-only (Prerecorded): Alternatives provided (Level A) - N/A
- [ ] 1.2.2 Captions (Prerecorded): Captions for videos (Level A) - N/A
- [ ] 1.2.3 Audio Description or Media Alternative (Prerecorded): Alternative provided (Level A) - N/A
- [ ] 1.2.4 Captions (Live): Live captions provided (Level AA) - N/A
- [ ] 1.2.5 Audio Description (Prerecorded): Audio description for videos (Level AA) - N/A

**1.3 Adaptable**
- [ ] 1.3.1 Info and Relationships: Semantic structure programmatically determinable (Level A)
- [ ] 1.3.2 Meaningful Sequence: Reading order is logical (Level A)
- [ ] 1.3.3 Sensory Characteristics: Instructions don't rely solely on shape, size, location, orientation, or sound (Level A)
- [ ] 1.3.4 Orientation: Content not restricted to single display orientation (Level AA)
- [ ] 1.3.5 Identify Input Purpose: Input purpose can be programmatically determined (Level AA)

**1.4 Distinguishable**
- [ ] 1.4.1 Use of Color: Color not used as only visual means of conveying information (Level A)
- [ ] 1.4.2 Audio Control: Mechanism to pause/stop/control audio (Level A) - N/A
- [ ] 1.4.3 Contrast (Minimum): 4.5:1 for normal text, 3:1 for large text (Level AA)
- [ ] 1.4.4 Resize Text: Text can be resized to 200% without loss of content or functionality (Level AA)
- [ ] 1.4.5 Images of Text: Text used instead of images of text (Level AA)
- [ ] 1.4.10 Reflow: Content reflows without horizontal scrolling at 320px width (Level AA)
- [ ] 1.4.11 Non-text Contrast: 3:1 contrast for UI components and graphics (Level AA)
- [ ] 1.4.12 Text Spacing: Text spacing can be adjusted without loss of content (Level AA)
- [ ] 1.4.13 Content on Hover or Focus: Dismissible, hoverable, persistent (Level AA)

### Principle 2: Operable

**2.1 Keyboard Accessible**
- [ ] 2.1.1 Keyboard: All functionality available via keyboard (Level A)
- [ ] 2.1.2 No Keyboard Trap: Focus can be moved away from all components (Level A)
- [ ] 2.1.4 Character Key Shortcuts: Shortcuts can be turned off or remapped (Level A)

**2.2 Enough Time**
- [ ] 2.2.1 Timing Adjustable: Time limits can be adjusted (Level A)
- [ ] 2.2.2 Pause, Stop, Hide: Moving, blinking, scrolling content can be paused (Level A)

**2.3 Seizures and Physical Reactions**
- [ ] 2.3.1 Three Flashes or Below Threshold: No content flashes more than 3 times per second (Level A)

**2.4 Navigable**
- [ ] 2.4.1 Bypass Blocks: Skip navigation link or landmarks (Level A)
- [ ] 2.4.2 Page Titled: Pages have descriptive titles (Level A)
- [ ] 2.4.3 Focus Order: Focus order is logical (Level A)
- [ ] 2.4.4 Link Purpose (In Context): Link purpose clear from link text or context (Level A)
- [ ] 2.4.5 Multiple Ways: Multiple ways to find pages (sitemap, search, nav) (Level AA)
- [ ] 2.4.6 Headings and Labels: Headings and labels are descriptive (Level AA)
- [ ] 2.4.7 Focus Visible: Keyboard focus indicator is visible (Level AA)

**2.5 Input Modalities**
- [ ] 2.5.1 Pointer Gestures: All multi-point or path-based gestures have single-pointer alternative (Level A)
- [ ] 2.5.2 Pointer Cancellation: Click activation on up event or can be aborted (Level A)
- [ ] 2.5.3 Label in Name: Accessible name includes visible text label (Level A)
- [ ] 2.5.4 Motion Actuation: Motion-based actions can be disabled and have UI alternative (Level A) - N/A

### Principle 3: Understandable

**3.1 Readable**
- [ ] 3.1.1 Language of Page: Page language identified (Level A)
- [ ] 3.1.2 Language of Parts: Language changes identified (Level AA) - If applicable

**3.2 Predictable**
- [ ] 3.2.1 On Focus: Focus doesn't trigger unexpected context change (Level A)
- [ ] 3.2.2 On Input: Input doesn't trigger unexpected context change (Level A)
- [ ] 3.2.3 Consistent Navigation: Navigation is consistent across pages (Level AA)
- [ ] 3.2.4 Consistent Identification: Components with same functionality identified consistently (Level AA)

**3.3 Input Assistance**
- [ ] 3.3.1 Error Identification: Errors identified and described to user (Level A)
- [ ] 3.3.2 Labels or Instructions: Labels or instructions provided for inputs (Level A)
- [ ] 3.3.3 Error Suggestion: Suggestions provided for fixing errors (Level AA)
- [ ] 3.3.4 Error Prevention (Legal, Financial, Data): Submissions are reversible, checked, or confirmed (Level AA)

### Principle 4: Robust

**4.1 Compatible**
- [ ] 4.1.1 Parsing: HTML is valid (Level A)
- [ ] 4.1.2 Name, Role, Value: All components have accessible name and role (Level A)
- [ ] 4.1.3 Status Messages: Status messages programmatically determinable (Level AA)

---

## 4. Component-Specific Accessibility

### Dashboard Components

**Metric Cards:**
- [ ] Accessible name for each card
- [ ] Values announced by screen reader
- [ ] Trend indicators have text alternatives
- [ ] Color not sole indicator of status

**Charts (ngx-charts):**
- [ ] Chart has accessible title
- [ ] Data table alternative provided or `<title>` and `<desc>` in SVG
- [ ] Chart colors have sufficient contrast
- [ ] Chart legend accessible
- [ ] Interactive elements keyboard accessible

### Table Components (Material Table)

**Patients, Evaluations, Results Tables:**
- [ ] Table has `<caption>` or `aria-label`
- [ ] Column headers use `<th>`
- [ ] Sort buttons keyboard accessible
- [ ] Sort direction announced (aria-sort)
- [ ] Row selection announced (aria-selected)
- [ ] Pagination controls labeled
- [ ] Filter inputs labeled
- [ ] Empty state has meaningful message

### Form Components

**Input Fields:**
- [ ] Label associated with input
- [ ] Required fields indicated
- [ ] Error messages associated (aria-describedby)
- [ ] Error messages visible and announced
- [ ] Helper text available

**Dropdowns (mat-select):**
- [ ] Label associated
- [ ] Current selection announced
- [ ] Dropdown options navigable with arrow keys
- [ ] Selection announced on change

**Date Pickers (mat-datepicker):**
- [ ] Label associated
- [ ] Keyboard accessible (Enter to open, Esc to close, arrows to navigate)
- [ ] Selected date announced
- [ ] Format instructions provided

**Checkboxes:**
- [ ] Label associated
- [ ] State announced (checked/unchecked)
- [ ] Indeterminate state announced (if applicable)

### Dialog Components

**All Dialogs:**
- [ ] `role="dialog"` or `role="alertdialog"`
- [ ] `aria-labelledby` points to dialog title
- [ ] `aria-describedby` points to dialog description (if applicable)
- [ ] `aria-modal="true"`
- [ ] Focus moves to dialog on open
- [ ] Focus trapped within dialog
- [ ] Focus returns to trigger on close
- [ ] Esc key closes dialog
- [ ] Close button accessible

### Loading and Progress Indicators

**Loading Spinners:**
- [ ] `role="status"` or `aria-live="polite"`
- [ ] Loading message announced
- [ ] Not sole indicator (text also present)

**Progress Bars:**
- [ ] `role="progressbar"`
- [ ] `aria-valuenow`, `aria-valuemin`, `aria-valuemax`
- [ ] Percentage announced

---

## 5. Mobile Accessibility

### Touch Target Size

**WCAG 2.1 Success Criterion 2.5.5 (Level AAA - Best Practice):**
- Minimum touch target: 44x44 CSS pixels

**Elements to Check:**
- [ ] Buttons: ≥ 44px height
- [ ] Links: ≥ 44px touch area
- [ ] Checkboxes: ≥ 44px touch area
- [ ] Radio buttons: ≥ 44px touch area
- [ ] Dropdown triggers: ≥ 44px
- [ ] Icon buttons: ≥ 44px
- [ ] Table row selection: Adequate touch area

### Mobile Screen Reader Testing

**iOS VoiceOver:**
- [ ] Enable VoiceOver (Settings > Accessibility > VoiceOver)
- [ ] Test all pages
- [ ] Verify gestures work (swipe to navigate, double-tap to activate)
- [ ] Verify content announced properly

**Android TalkBack:**
- [ ] Enable TalkBack (Settings > Accessibility > TalkBack)
- [ ] Test all pages
- [ ] Verify gestures work
- [ ] Verify content announced properly

### Zoom and Orientation

- [ ] Content readable at 200% zoom
- [ ] No horizontal scrolling required at 320px viewport
- [ ] Content adapts to portrait and landscape
- [ ] No orientation lock (unless essential)

---

## 6. Assistive Technology Testing Matrix

| Feature | NVDA | JAWS | VoiceOver (macOS) | VoiceOver (iOS) | TalkBack | Status |
|---------|------|------|-------------------|-----------------|----------|--------|
| Page navigation | [ ] | [ ] | [ ] | [ ] | [ ] | |
| Form completion | [ ] | [ ] | [ ] | [ ] | [ ] | |
| Table navigation | [ ] | [ ] | [ ] | [ ] | [ ] | |
| Dialog interaction | [ ] | [ ] | [ ] | [ ] | [ ] | |
| Chart reading | [ ] | [ ] | [ ] | [ ] | [ ] | |
| Error handling | [ ] | [ ] | [ ] | [ ] | [ ] | |
| Dynamic updates | [ ] | [ ] | [ ] | [ ] | [ ] | |

---

## 7. Remediation Priorities

### Critical (Fix Immediately)

**Issue:** _______________________________________
**WCAG Criterion:** _____________
**Impact:** Blocks core functionality for users with disabilities
**Affected Pages:** _____________
**Remediation:** _______________________________________

**Issue:** _______________________________________
**WCAG Criterion:** _____________
**Impact:** _______________________________________
**Affected Pages:** _____________
**Remediation:** _______________________________________

### High Priority (Fix Before Release)

**Issue:** _______________________________________
**WCAG Criterion:** _____________
**Impact:** _______________________________________
**Affected Pages:** _____________
**Remediation:** _______________________________________

### Medium Priority (Fix Soon)

**Issue:** _______________________________________
**WCAG Criterion:** _____________
**Impact:** _______________________________________
**Affected Pages:** _____________
**Remediation:** _______________________________________

### Low Priority (Enhancement)

**Issue:** _______________________________________
**WCAG Criterion:** _____________
**Impact:** _______________________________________
**Affected Pages:** _____________
**Remediation:** _______________________________________

---

## 8. Testing Scripts

### Automated E2E Accessibility Tests

Create: `apps/clinical-portal-e2e/src/e2e/accessibility.spec.ts`

```typescript
import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

test.describe('Accessibility tests', () => {
  test('Dashboard should not have accessibility violations', async ({ page }) => {
    await page.goto('http://localhost:4200/dashboard');

    const accessibilityScanResults = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
      .analyze();

    expect(accessibilityScanResults.violations).toEqual([]);
  });

  test('Patients page should not have accessibility violations', async ({ page }) => {
    await page.goto('http://localhost:4200/patients');

    const accessibilityScanResults = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
      .analyze();

    expect(accessibilityScanResults.violations).toEqual([]);
  });

  test('Evaluations page should not have accessibility violations', async ({ page }) => {
    await page.goto('http://localhost:4200/evaluations');

    const accessibilityScanResults = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
      .analyze();

    expect(accessibilityScanResults.violations).toEqual([]);
  });

  test('Results page should not have accessibility violations', async ({ page }) => {
    await page.goto('http://localhost:4200/results');

    const accessibilityScanResults = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
      .analyze();

    expect(accessibilityScanResults.violations).toEqual([]);
  });

  test('Reports page should not have accessibility violations', async ({ page }) => {
    await page.goto('http://localhost:4200/reports');

    const accessibilityScanResults = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
      .analyze();

    expect(accessibilityScanResults.violations).toEqual([]);
  });

  test('Keyboard navigation should work on Patients page', async ({ page }) => {
    await page.goto('http://localhost:4200/patients');

    // Tab through interactive elements
    await page.keyboard.press('Tab');
    let focusedElement = await page.evaluate(() => document.activeElement?.tagName);
    expect(focusedElement).toBeTruthy();

    // Ensure focus visible
    const hasFocusIndicator = await page.evaluate(() => {
      const el = document.activeElement;
      const styles = window.getComputedStyle(el!);
      return styles.outline !== 'none' || styles.boxShadow !== 'none';
    });
    expect(hasFocusIndicator).toBe(true);
  });

  test('Dialog should trap focus', async ({ page }) => {
    await page.goto('http://localhost:4200/patients');

    // Open a dialog (adjust selector as needed)
    await page.click('button[aria-label="Add Patient"]');

    // Wait for dialog
    await page.waitForSelector('[role="dialog"]');

    // Verify focus is in dialog
    const focusInDialog = await page.evaluate(() => {
      const dialog = document.querySelector('[role="dialog"]');
      const activeEl = document.activeElement;
      return dialog?.contains(activeEl) ?? false;
    });
    expect(focusInDialog).toBe(true);

    // Tab multiple times and verify focus stays in dialog
    for (let i = 0; i < 10; i++) {
      await page.keyboard.press('Tab');
    }

    const focusStillInDialog = await page.evaluate(() => {
      const dialog = document.querySelector('[role="dialog"]');
      const activeEl = document.activeElement;
      return dialog?.contains(activeEl) ?? false;
    });
    expect(focusStillInDialog).toBe(true);
  });
});
```

**Run tests:**
```bash
npx playwright test accessibility.spec.ts
```

---

## 9. Compliance Statement

**Target Compliance Level:** WCAG 2.1 Level AA

**Current Status:** [ ] Compliant | [ ] Partially Compliant | [ ] Non-Compliant

**Known Issues:**
1. _______________________________________
2. _______________________________________
3. _______________________________________

**Planned Remediation Date:** _____________

**Accessibility Conformance Statement:**

The HealthData-in-Motion Clinical Portal aims to conform to WCAG 2.1 Level AA. The following parts of the WCAG 2.1 Level AA standard are not yet fully supported:

- _______________________________________
- _______________________________________

We are committed to addressing these issues by [date].

---

## 10. Ongoing Accessibility

### Development Guidelines

**For Developers:**
- [ ] Install axe DevTools browser extension
- [ ] Run automated checks during development
- [ ] Test with keyboard before committing
- [ ] Use semantic HTML by default
- [ ] Follow ARIA Authoring Practices Guide
- [ ] Test with screen reader monthly

### Code Review Checklist

- [ ] Semantic HTML used
- [ ] ARIA attributes correct
- [ ] Keyboard navigation works
- [ ] Focus management correct
- [ ] Color contrast sufficient
- [ ] Alt text provided
- [ ] Labels associated with inputs
- [ ] Automated tests passing

### Regular Audits

- [ ] Run automated scans weekly
- [ ] Manual keyboard testing monthly
- [ ] Screen reader testing quarterly
- [ ] Full WCAG audit annually
- [ ] User testing with people with disabilities annually

---

## 11. Resources

**WCAG 2.1 Guidelines:**
https://www.w3.org/WAI/WCAG21/quickref/

**ARIA Authoring Practices Guide:**
https://www.w3.org/WAI/ARIA/apg/

**WebAIM Resources:**
https://webaim.org/

**Angular Material Accessibility:**
https://material.angular.io/cdk/a11y/overview

**Testing Tools:**
- axe DevTools: https://www.deque.com/axe/devtools/
- WAVE: https://wave.webaim.org/
- Lighthouse: Built into Chrome DevTools
- NVDA: https://www.nvaccess.org/
- VoiceOver: Built into macOS/iOS

---

## Sign-Off

**Accessibility Specialist:** _____________________ **Date:** _________

**QA Lead:** _____________________ **Date:** _________

**Product Manager:** _____________________ **Date:** _________

**Legal/Compliance:** _____________________ **Date:** _________

---

## Appendix: Common Accessibility Patterns

### Skip Navigation Link

```html
<a href="#main-content" class="skip-link">Skip to main content</a>

<main id="main-content">
  <!-- Page content -->
</main>
```

```css
.skip-link {
  position: absolute;
  top: -40px;
  left: 0;
  background: #000;
  color: #fff;
  padding: 8px;
  z-index: 100;
}

.skip-link:focus {
  top: 0;
}
```

### Accessible Icon Button

```html
<button aria-label="Delete patient" type="button">
  <mat-icon>delete</mat-icon>
</button>
```

### Accessible Loading State

```html
<div role="status" aria-live="polite" aria-label="Loading patients">
  <mat-spinner></mat-spinner>
  <span class="sr-only">Loading patients...</span>
</div>
```

```css
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border-width: 0;
}
```

### Accessible Form with Validation

```html
<form>
  <mat-form-field>
    <mat-label>Patient Name</mat-label>
    <input matInput
           id="patient-name"
           [(ngModel)]="patientName"
           required
           aria-required="true"
           aria-invalid="{{nameInvalid}}"
           aria-describedby="name-error">
    <mat-error id="name-error" *ngIf="nameInvalid">
      Patient name is required
    </mat-error>
  </mat-form-field>
</form>
```

### Accessible Data Table

```html
<table aria-label="Patient list">
  <caption class="sr-only">List of patients with demographics and compliance status</caption>
  <thead>
    <tr>
      <th scope="col">
        <button (click)="sort('name')" [attr.aria-sort]="sortState('name')">
          Name
          <mat-icon *ngIf="sortedBy === 'name'">arrow_{{sortDirection}}</mat-icon>
        </button>
      </th>
      <th scope="col">MRN</th>
      <th scope="col">Status</th>
    </tr>
  </thead>
  <tbody>
    <tr *ngFor="let patient of patients" [attr.aria-selected]="patient.selected">
      <td>{{patient.name}}</td>
      <td>{{patient.mrn}}</td>
      <td>{{patient.status}}</td>
    </tr>
  </tbody>
</table>
```
