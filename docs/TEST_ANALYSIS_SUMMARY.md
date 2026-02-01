# Test Failure Analysis - Quick Summary

**Date:** January 27, 2026
**Status:** Comprehensive analysis complete
**Test Suite:** 171 suites (93 passing, 78 failing = 54.4%)

---

## Quick Overview

```
171 Total Test Suites
├── ✅ 93 PASSING (54.4%)
└── ❌ 78 FAILING (45.6%)
    ├── 11 - Missing DI Providers (MatDialogRef)
    ├── 5 - Missing spyOn Global
    ├── 18 - Mock Data Mismatch
    ├── 6 - Missing DOM Elements
    ├── 5 - Logic/Computation Errors
    └── 32 - Unaccounted (Async/HTTP/Mocks)
```

---

## Failure Categories Ranked by Impact

### 🔴 HIGH PRIORITY (28 tests, 15-20 min fix)

**Category 1: Missing DI Providers (11 tests)**
- Error: `NG0201: No provider found for MatDialogRef`
- Component: CarePlanWorkflowComponent accessibility tests
- Fix: Add MatDialogRef and MAT_DIALOG_DATA to TestBed providers
- Time: 15 minutes
- Impact: +6.4% pass rate → 60.8%

**Category 2: Missing spyOn Global (5 tests)**
- Error: `ReferenceError: spyOn is not defined`
- Components: DistributionPeriodSlider, RangeThresholdSlider
- Fix: Replace `spyOn()` with `jest.spyOn()`
- Time: 5 minutes
- Impact: +2.9% pass rate → 63.7%

### 🟡 MEDIUM PRIORITY (24 tests, 3-6 hours fix)

**Category 3: Mock Data Mismatch (18 tests)**
- Error: `Expected: X, Received: Y`
- Issues:
  - Weight rebalancing not working (2 tests)
  - Period presets not applied (3 tests)
  - Color format mismatch (1 test)
  - CQL output format changed (2 tests)
  - Logger prefix duplication (1 test)
  - Precision/rounding issues (2 tests)
  - Bar width calculations (1 test)
  - Slider value mismatches (6 tests)
- Time: 2-4 hours
- Impact: +10.5% pass rate → 74.2%

**Category 4: Missing DOM Elements (6 tests)**
- Error: `Expected: truthy, Received: null`
- Issues:
  - Warning indicators not rendered (2 tests)
  - ARIA labels missing on form fields (2 tests)
  - Row selection checkboxes missing (1 test)
  - Active preset highlighting broken (1 test)
- Time: 1-2 hours
- Impact: +3.5% pass rate → 77.7%

### 🟠 COMPLEX (5 tests, 3-4 hours fix)

**Category 5: Logic/Computation Errors (5 tests)**
- Issues:
  - Validation logic missing/broken (1 test)
  - Preset selection not implemented (1 test)
  - Performance optimization needed (1 test)
  - 2 unknown logic errors
- Time: 3-4 hours
- Impact: +2.9% pass rate → 80.6%

### ⚪ INVESTIGATION NEEDED (32 tests)

**Category 6: Unaccounted Failures (32 tests)**
- Likely causes:
  - Async/timing issues (~15 tests)
  - HTTP mocking issues (~10 tests)
  - Service mock configuration (~5 tests)
  - State management issues (~2 tests)
- Time: Unknown (requires investigation)
- Impact: +18.7% potential pass rate → 99%

---

## Phased Implementation Roadmap

```
Phase 1 (15 min)          Phase 2 (5 min)         Phase 3 (2-4 hrs)
┌─────────────────┐      ┌─────────────────┐      ┌──────────────────┐
│ MatDialogRef    │      │ spyOn Global    │      │ Mock Data Updates│
│ providers       │      │ function        │      │ Logic fixes      │
│                 │      │                 │      │                  │
│ +11 tests ✅    │      │ +5 tests ✅     │      │ +18 tests ✅     │
│ 60.8% → 63.7%  │      │ 63.7% → 66.6%   │      │ 66.6% → 77.1%   │
└─────────────────┘      └─────────────────┘      └──────────────────┘
         │                        │                        │
         └────────────────────────┴────────────────────────┘
                                  │
                        Phase 4 (1-2 hrs)
                    ┌──────────────────────┐
                    │ DOM Elements & ARIA  │
                    │ attributes           │
                    │                      │
                    │ +6 tests ✅          │
                    │ 77.1% → 80.6%       │
                    └──────────────────────┘
                                  │
                        Phase 5 (3-4 hrs)
                    ┌──────────────────────┐
                    │ Logic Fixes          │
                    │ Validation           │
                    │ Presets              │
                    │ Performance          │
                    │                      │
                    │ +5 tests ✅          │
                    │ 80.6% → 83.5%       │
                    └──────────────────────┘
                                  │
                        Phase 6 (Unknown)
                    ┌──────────────────────┐
                    │ Async/HTTP/Services  │
                    │ investigation        │
                    │                      │
                    │ +32 tests ✅?        │
                    │ 83.5% → 99%?         │
                    └──────────────────────┘
```

---

## Component Breakdown

| Component | Failing Tests | Primary Issues |
|-----------|---------------|-----------------|
| CarePlanWorkflowComponent | 11 | Missing MatDialogRef provider |
| DistributionPeriodSliderComponent | 8 | Data mismatch, DOM elements, logic |
| RangeThresholdSliderComponent | 9 | Data mismatch, DOM elements, spyOn |
| PreVisitPlanningComponent | 1 | Missing ARIA labels |
| PatientsComponent | 1 | Missing checkboxes + ARIA |
| FormFieldComponent | 1 | Logger prefix duplication |
| Unknown/Other | 47 | Various (Async/HTTP/Services) |

---

## Most Critical Files to Fix

### 🔴 CRITICAL (15 min fix)
```
apps/clinical-portal/src/app/pages/dashboard/rn-dashboard/workflows/
  care-plan/care-plan-workflow.component.a11y.spec.ts
  → Add MatDialogRef provider
```

### 🟡 HIGH IMPACT (30+ tests, 3-6 hours)
```
apps/clinical-portal/src/app/pages/measure-builder/components/
  measure-config-slider/
    distribution-period-slider.component.ts (/spec.ts)
    range-threshold-slider.component.ts (/spec.ts)
  → Fix data mismatches, logic, DOM elements
```

### 🟡 MODERATE (8 tests, 2 hours)
```
apps/clinical-portal/src/app/pages/pre-visit-planning/
  pre-visit-planning.component.html
  → Add aria-label attributes

apps/clinical-portal/src/app/pages/patients/
  patients.component.html
  → Add checkboxes with aria-labels

apps/clinical-portal/src/app/shared/components/form-field/
  form-field.component.spec.ts
  → Update Logger output expectations
```

---

## Quick Reference: Common Fixes

### Fix #1: Add MatDialogRef Provider (11 tests)
```typescript
TestBed.configureTestingModule({
  imports: [ComponentClass, NoopAnimationsModule],
  providers: [
    {
      provide: MatDialogRef,
      useValue: { close: jasmine.createSpy('close') }
    },
    { provide: MAT_DIALOG_DATA, useValue: { /* test data */ } }
  ]
}).compileComponents();
```

### Fix #2: Use jest.spyOn() (5 tests)
```typescript
// BEFORE: spyOn(component.event, 'emit');
// AFTER:
jest.spyOn(component.event, 'emit');
```

### Fix #3: Update Mock Data (18 tests)
```typescript
// Compare expected vs actual output
// Update test expectations or component logic to match
// Common issues: weight totals, preset values, color formats
```

### Fix #4: Add ARIA Attributes (6 tests)
```html
<!-- BEFORE -->
<input type="text" [(ngModel)]="value" />

<!-- AFTER -->
<input type="text"
       [(ngModel)]="value"
       aria-label="Descriptive label" />
```

---

## Key Statistics

| Metric | Value |
|--------|-------|
| Total Test Suites | 171 |
| Passing | 93 (54.4%) |
| Failing | 78 (45.6%) |
| Compilation Errors | 0 ✅ |
| Import Errors | 0 ✅ |
| DI Timing Errors | 0 ✅ |
| Clear, Fixable Issues | 46 (59%) |
| Requires Investigation | 32 (41%) |
| Estimated Total Fix Time | 9-14 hours |
| Achievable Pass Rate (Phases 1-5) | 80.6% |
| Potential Pass Rate (w/ Phase 6) | 90-99% |

---

## Success Criteria

✅ **Phase 1 Complete:** 60%+ pass rate (11 tests fixed in 15 min)
✅ **Phase 2 Complete:** 63%+ pass rate (5 tests fixed in 5 min)
✅ **Phase 3 Complete:** 74%+ pass rate (18 tests fixed in 2-4 hrs)
✅ **Phase 4 Complete:** 77%+ pass rate (6 tests fixed in 1-2 hrs)
✅ **Phase 5 Complete:** 80%+ pass rate (5 tests fixed in 3-4 hrs)
⏳ **Phase 6 Target:** 90%+ pass rate (32+ tests fixed)

---

## Next Steps

1. **Immediate:** Review `TEST_FAILURE_ROOT_CAUSE_ANALYSIS.md` for detailed fixes
2. **Phase 1:** Add MatDialogRef providers (15 min, +11 tests)
3. **Phase 2:** Replace spyOn() with jest.spyOn() (5 min, +5 tests)
4. **Phase 3-5:** Implement remaining fixes by priority
5. **Phase 6:** Investigate remaining 32 tests with detailed error messages

---

_For detailed analysis, see: `docs/TEST_FAILURE_ROOT_CAUSE_ANALYSIS.md`_
