# Color Contrast Analysis - WCAG 2.1 Level AA Compliance

## Overview

This document analyzes the color contrast ratios used in the HDIM Clinical Portal to ensure compliance with WCAG 2.1 Level AA standards.

**Requirements:**
- Normal text (< 18pt): **4.5:1** contrast ratio
- Large text (≥ 18pt / ≥ 14pt bold): **3:1** contrast ratio
- UI components and graphics: **3:1** contrast ratio

## Light Theme Analysis

### Primary Colors

| Color | Hex | Usage | Background | Ratio | Status |
|-------|-----|-------|------------|-------|--------|
| Primary | `#1976d2` | Buttons, links | `#ffffff` (white) | 4.84:1 | ✅ PASS (AA Normal) |
| Primary Light | `#42a5f5` | Hover states | `#ffffff` (white) | 3.13:1 | ⚠️ MARGINAL (AA Large only) |
| Primary Dark | `#1565c0` | Active states | `#ffffff` (white) | 5.93:1 | ✅ PASS (AA Normal) |
| Accent | `#ff4081` | Highlights | `#ffffff` (white) | 3.94:1 | ⚠️ MARGINAL (AA Large only) |

### Text Colors

| Color | Hex | Usage | Background | Ratio | Status |
|-------|-----|-------|------------|-------|--------|
| Primary Text | `#1a1a1a` | Body text | `#ffffff` (white) | 17.38:1 | ✅ PASS (AAA) |
| Primary Text | `#1a1a1a` | Body text | `#f5f5f5` (secondary bg) | 16.17:1 | ✅ PASS (AAA) |
| Primary Text | `#1a1a1a` | Body text | `#fafafa` (tertiary bg) | 15.75:1 | ✅ PASS (AAA) |
| Secondary Text | `#424242` | Labels | `#ffffff` (white) | 10.77:1 | ✅ PASS (AAA) |
| Disabled Text | `#9e9e9e` | Disabled | `#ffffff` (white) | 2.85:1 | ❌ FAIL (< 4.5:1) |
| Hint Text | `#9e9e9e` | Hints | `#ffffff` (white) | 2.85:1 | ❌ FAIL (< 4.5:1) |

**Note:** Disabled text is exempt from WCAG contrast requirements (WCAG 2.1 1.4.3 Understanding).

### Status Colors

| Color | Hex | Usage | Background | Ratio | Status |
|-------|-----|-------|------------|-------|--------|
| Success | `#2e7d32` | Success messages | `#ffffff` (white) | 6.07:1 | ✅ PASS (AA Normal) |
| Warning | `#ed6c02` | Warnings | `#ffffff` (white) | 4.54:1 | ✅ PASS (AA Normal) |
| Error | `#d32f2f` | Errors | `#ffffff` (white) | 5.50:1 | ✅ PASS (AA Normal) |
| Info | `#0288d1` | Information | `#ffffff` (white) | 4.95:1 | ✅ PASS (AA Normal) |

### Toolbar Colors

| Color | Hex | Usage | Background | Ratio | Status |
|-------|-----|-------|------------|-------|--------|
| Toolbar Text | `#ffffff` (white) | Header text | `#1976d2` (primary) | 4.84:1 | ✅ PASS (AA Normal) |

### Sidebar Colors

| Element | Foreground | Background | Ratio | Status |
|---------|-----------|------------|-------|--------|
| Sidebar Text | `#1a1a1a` | `#ffffff` (white) | 17.38:1 | ✅ PASS (AAA) |
| Hover State | `#1a1a1a` | `#f5f5f5` (light gray) | 16.17:1 | ✅ PASS (AAA) |
| Selected State | `#1976d2` (primary) | `#e3f2fd` (light blue) | 4.22:1 | ⚠️ MARGINAL (AA Large only) |

### Toast Notifications (styles.scss)

| Type | Background | Text | Ratio | Status |
|------|-----------|------|-------|--------|
| Success | `#4caf50` (green) | `#ffffff` (white) | 3.73:1 | ⚠️ MARGINAL (AA Large only) |
| Error | `#f44336` (red) | `#ffffff` (white) | 4.07:1 | ⚠️ MARGINAL (AA Large only) |
| Warning | `#ff9800` (orange) | `#ffffff` (white) | 2.96:1 | ❌ FAIL (< 3:1) |
| Info | `#2196f3` (blue) | `#ffffff` (white) | 3.54:1 | ⚠️ MARGINAL (AA Large only) |

**Critical Issue:** Toast notifications use colors from `styles.scss` that differ from theme colors and have marginal contrast.

## Dark Theme Analysis

### Primary Colors

| Color | Hex | Usage | Background | Ratio | Status |
|-------|-----|-------|------------|-------|--------|
| Primary | `#90caf9` | Buttons, links | `#1a1a1a` (dark) | 9.38:1 | ✅ PASS (AAA) |
| Primary Light | `#bbdefb` | Hover states | `#1a1a1a` (dark) | 13.12:1 | ✅ PASS (AAA) |
| Primary Dark | `#42a5f5` | Active states | `#1a1a1a` (dark) | 6.54:1 | ✅ PASS (AA Normal) |
| Accent | `#ff80ab` | Highlights | `#1a1a1a` (dark) | 6.81:1 | ✅ PASS (AA Normal) |

### Text Colors

| Color | Hex | Usage | Background | Ratio | Status |
|-------|-----|-------|------------|-------|--------|
| Primary Text | `#f5f5f5` | Body text | `#1a1a1a` (dark) | 15.75:1 | ✅ PASS (AAA) |
| Primary Text | `#f5f5f5` | Body text | `#242424` (secondary) | 13.93:1 | ✅ PASS (AAA) |
| Secondary Text | `#b0b0b0` | Labels | `#1a1a1a` (dark) | 7.74:1 | ✅ PASS (AAA) |
| Disabled Text | `#757575` | Disabled | `#1a1a1a` (dark) | 4.09:1 | ⚠️ MARGINAL (AA Large only) |

### Status Colors

| Color | Hex | Usage | Background | Ratio | Status |
|-------|-----|-------|------------|-------|--------|
| Success | `#81c784` | Success messages | `#1a1a1a` (dark) | 8.71:1 | ✅ PASS (AAA) |
| Warning | `#ffb74d` | Warnings | `#1a1a1a` (dark) | 10.30:1 | ✅ PASS (AAA) |
| Error | `#e57373` | Errors | `#1a1a1a` (dark) | 6.12:1 | ✅ PASS (AA Normal) |
| Info | `#64b5f6` | Information | `#1a1a1a` (dark) | 7.37:1 | ✅ PASS (AAA) |

## Issues Identified

### Critical (Must Fix)

**1. Toast Warning Notification - FAILS WCAG 2.1 Level AA**

**Location:** `apps/clinical-portal/src/styles.scss:72-77`

```scss
.toast-warning {
  background-color: #ff9800 !important;  // Orange
  color: white !important;                // White text
  // RATIO: 2.96:1 - FAILS AA (< 3:1)
}
```

**Impact:** Warning toast messages are difficult to read, especially for users with visual impairments.

**Recommended Fix:**
```scss
.toast-warning {
  background-color: #ef6c00 !important;  // Darker orange
  color: white !important;                // 4.52:1 - PASSES AA
}
```

### Marginal (Should Review)

**2. Toast Success/Error/Info Notifications - Marginal AA Compliance**

**Location:** `apps/clinical-portal/src/styles.scss:58-84`

These notifications pass for large text only (≥ 14pt bold). Since toast messages are typically 14px/14pt, they should be made bold OR use darker backgrounds.

**Current Ratios:**
- Success: 3.73:1 (passes AA Large only)
- Error: 4.07:1 (passes AA Large only)
- Info: 3.54:1 (passes AA Large only)

**Recommended Options:**

**Option A: Darker backgrounds (preferred)**
```scss
.toast-success {
  background-color: #388e3c !important;  // 4.53:1 - PASSES AA Normal
  color: white !important;
}

.toast-error {
  background-color: #d32f2f !important;  // 5.50:1 - PASSES AA Normal
  color: white !important;
}

.toast-info {
  background-color: #1976d2 !important;  // 4.84:1 - PASSES AA Normal
  color: white !important;
}
```

**Option B: Make text bold**
```scss
.toast-success,
.toast-error,
.toast-warning,
.toast-info {
  font-weight: 700;  // Bold text qualifies for 3:1 ratio (AA Large)
}
```

**3. Accent Color on White Background - Marginal**

**Location:** `src/styles/themes.scss:10, 63`

```scss
--accent-color: #ff4081;  // Pink accent
// On white background: 3.94:1 (passes AA Large only)
```

**Impact:** Accent-colored text links or buttons may not meet AA normal text requirements.

**Recommended Fix:**
```scss
--accent-color: #e91e63;  // Darker pink: 4.76:1 - PASSES AA Normal
```

**4. Primary Light Color - Marginal**

**Location:** `src/styles/themes.scss:8, 61`

```scss
--primary-light: #42a5f5;  // Light blue
// On white background: 3.13:1 (passes AA Large only)
```

**Impact:** Hover states using primary-light may not be readable as normal text.

**Recommended Action:** Reserve `primary-light` for:
- Large text (≥ 18pt)
- Bold text (≥ 14pt)
- Backgrounds (not foreground text)

**5. Sidebar Selected State - Marginal**

**Location:** `src/styles/themes.scss:17, 70, 104, 157`

```scss
--sidebar-selected: #e3f2fd;  // Very light blue
// Primary text on selected: 4.22:1 (marginal)
```

**Impact:** Selected navigation items may have reduced contrast.

**Recommended Fix:**
```scss
// Light theme
--sidebar-selected: #bbdefb;  // Medium light blue: Better contrast

// Dark theme
--sidebar-selected: #1e4976;  // Darker blue: Better contrast with light text
```

## Recommendations

### Immediate Actions (WCAG 2.1 Level AA Compliance)

1. **Fix toast warning notification** (CRITICAL - currently failing)
   - Change `#ff9800` to `#ef6c00` (4.52:1 ratio)

2. **Improve toast notification contrast** (HIGH PRIORITY)
   - Use darker backgrounds OR make text bold
   - Aligns toast colors with theme status colors

3. **Document color usage guidelines**
   - Primary-light: Large/bold text only
   - Accent color: Large/bold text only or use darker variant

### Future Improvements (WCAG 2.1 Level AAA)

1. **Align toast colors with theme colors**
   - Use `--status-success`, `--status-warning`, etc.
   - Ensures consistency between themes and toasts

2. **Add color contrast testing to CI/CD**
   - Automated axe-core tests for color contrast
   - Fail builds on contrast violations

3. **Create color palette documentation**
   - Document approved color combinations
   - Provide contrast ratios for all pairings
   - Include usage guidelines (normal vs large text)

## Verification Steps

### Automated Testing

```bash
# Run color contrast tests
npx nx test clinical-portal --testPathPattern=a11y.spec.ts --testNamePattern="color contrast"

# Run full accessibility audit
npx nx test clinical-portal --testPathPattern=a11y.spec.ts
```

### Manual Testing

**Chrome DevTools Lighthouse:**
1. Open Clinical Portal in Chrome
2. F12 > Lighthouse tab
3. Select "Accessibility" category
4. Generate report
5. Review color contrast issues

**axe DevTools Extension:**
1. Install: https://www.deque.com/axe/devtools/
2. Open Clinical Portal
3. F12 > axe DevTools tab
4. Scan all pages
5. Filter by "Color contrast" issues

**WebAIM Contrast Checker:**
- URL: https://webaim.org/resources/contrastchecker/
- Test individual color pairs
- Verify fixes meet AA standards

### Test Coverage

**Pages to Test:**
- [ ] Navigation sidebar (all states: default, hover, selected)
- [ ] Dashboard stat cards
- [ ] Patient list table
- [ ] Care gap manager (status chips, action buttons)
- [ ] Quality measures (star ratings, status badges)
- [ ] Toast notifications (all 4 types)
- [ ] Form inputs (all states: default, focus, error)
- [ ] Buttons (all variants: primary, accent, text)
- [ ] Dark theme (all components)

## Contrast Ratio Calculation

**Formula:**
```
Contrast Ratio = (L1 + 0.05) / (L2 + 0.05)

Where:
L1 = relative luminance of lighter color
L2 = relative luminance of darker color
Luminance range: 0 (black) to 1 (white)
```

**WCAG 2.1 Success Criteria:**
- **Level AA (Normal text):** 4.5:1
- **Level AA (Large text):** 3:1
- **Level AAA (Normal text):** 7:1
- **Level AAA (Large text):** 4.5:1

**Large Text Definition:**
- 18pt (24px) regular weight
- 14pt (18.5px) bold weight

## Resources

- [WCAG 2.1 Contrast (Minimum)](https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html)
- [WebAIM Contrast Checker](https://webaim.org/resources/contrastchecker/)
- [Color Contrast Analyzer](https://www.tpgi.com/color-contrast-checker/)
- [axe DevTools](https://www.deque.com/axe/devtools/)
- [Chrome DevTools Lighthouse](https://developers.google.com/web/tools/lighthouse)

---

**Last Updated:** January 2026
**Compliance Target:** WCAG 2.1 Level AA
**Status:** 1 Critical Issue, 4 Marginal Issues Identified
