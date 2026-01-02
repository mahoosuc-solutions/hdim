# Manual Testing Guide - MRN Enhancement & Reports Feature

**Date:** November 15, 2025
**Status:** Ready for Testing
**Frontend URL:** http://localhost:4200

---

## Overview

This guide provides step-by-step instructions for manually testing:
1. **MRN Display Enhancement** - Assigning authority now shown with MRN
2. **Reports Feature** - Complete CRUD workflow with toast notifications
3. **Patient Selection** - Enhanced dialog with MRN authority display

---

## Prerequisites

### Backend Services
Verify all Docker containers are running:
```bash
docker ps --filter name=healthdata
```

Expected: 7 containers running (postgres, fhir, quality-measure, kafka, etc.)

### Frontend Dev Server
```bash
# Check if running
curl -s -o /dev/null -w "%{http_code}" http://localhost:4200/

# Should return: 200
```

### Test Data
8 test patients available (IDs 84-91) with complete MRN data.
See `TEST_DATA_REFERENCE.md` for full patient details.

---

## Test Suite 1: MRN Display Enhancement

### Test 1.1: Patient Detail Page - MRN with Authority

**Steps:**
1. Open browser to: http://localhost:4200
2. Navigate to "Patients" page
3. Click on first patient in the list
4. Locate the "Demographics" card

**Expected Results:**
- MRN field displays: `MRN001234 (hospital.example.org)`
- Assigning authority shown in smaller, muted text
- Authority displayed in parentheses
- Format: `{MRN_VALUE} ({DOMAIN})`

**Test with Multiple Patients:**
| Patient ID | MRN | Expected Authority Display |
|------------|-----|---------------------------|
| 84 | MRN001234 | hospital.example.org |
| 85 | MRN002567 | hospital.example.org |
| 86 | MRN003891 | hospital.example.org |

**Pass Criteria:**
- ✅ MRN displays correctly
- ✅ Authority displays in parentheses
- ✅ Authority text is smaller and muted
- ✅ No errors in browser console

**Screenshot Location:** (Take screenshot for documentation)

---

### Test 1.2: Patient Selection Dialog - MRN with Authority

**Steps:**
1. Navigate to "Reports" page
2. Click "Generate Patient Report" button
3. Patient Selection Dialog opens
4. Observe MRN display in patient list

**Expected Results:**
- Each patient row shows:
  ```
  James Michael Anderson
  MRN: MRN001234 (hospital.example.org)
  ```
- MRN line displays below patient name
- Authority shown in lighter gray color
- Format consistent across all patients

**Search Functionality:**
1. Type "MRN001234" in search box
2. Verify patient filters correctly
3. MRN with authority still displays

**Pass Criteria:**
- ✅ MRN displays with authority in dialog
- ✅ Search by MRN works correctly
- ✅ Authority text properly styled (muted)
- ✅ All 8 test patients show MRN correctly

---

### Test 1.3: Patients without MRN

**Steps:**
1. Open Patient Selection Dialog
2. Scroll to find patients without MRN (IDs < 84)

**Expected Results:**
- Patients without MRN show only name
- No "MRN: N/A" or empty MRN line
- No errors or broken layout

**Pass Criteria:**
- ✅ No MRN line displayed for patients without MRN
- ✅ Layout remains consistent

---

## Test Suite 2: Reports Feature - Complete Workflow

### Test 2.1: Generate Patient Report

**Steps:**
1. Navigate to Reports page → "Generate Reports" tab
2. Click "Generate Patient Report" button
3. Patient Selection Dialog opens
4. Search for "James" or "MRN001234"
5. Click "Select" button for James Anderson
6. Click "Confirm Selection"

**Expected Results:**
- ✅ Green toast notification appears: "Patient report generated successfully"
- ✅ Toast appears in bottom-right corner
- ✅ Toast auto-dismisses after 3 seconds
- ✅ Automatically navigates to "Saved Reports" tab
- ✅ New report appears at top of list
- ✅ Report shows:
  - Type: "PATIENT"
  - Patient: "James Michael Anderson"
  - Status: "COMPLETED"
  - Created date: Current date/time
  - Actions: View, CSV, Excel, Delete buttons

**Pass Criteria:**
- ✅ Toast notification displays correctly
- ✅ Report generated successfully
- ✅ Tab switches automatically
- ✅ Report appears in list
- ✅ All report details correct

---

### Test 2.2: Generate Population Report

**Steps:**
1. Navigate to Reports → "Generate Reports" tab
2. Click "Generate Population Report" button
3. Year Selection Dialog opens
4. Click "2025" quick button (or select from dropdown)
5. Click "Generate Report"

**Expected Results:**
- ✅ Green toast notification: "Population report generated successfully"
- ✅ Navigates to "Saved Reports" tab
- ✅ New population report appears
- ✅ Report shows:
  - Type: "POPULATION"
  - Year: "2025"
  - Status: "COMPLETED"

**Pass Criteria:**
- ✅ Toast notification displays
- ✅ Report generated for correct year
- ✅ Report type shows as POPULATION

---

### Test 2.3: View Report Details

**Steps:**
1. Navigate to "Saved Reports" tab
2. Click "View" button on any completed report
3. Report Detail Dialog opens

**Expected Results:**
- ✅ Dialog displays with 3 tabs:
  1. **Overview** - Report metadata (ID, type, status, dates)
  2. **Report Data** - Quality measure results in table
  3. **Raw JSON** - Complete JSON response
- ✅ Can switch between tabs
- ✅ Data displays correctly in each tab
- ✅ "Close" button works

**Pass Criteria:**
- ✅ All 3 tabs accessible
- ✅ Data displays in all tabs
- ✅ Dialog closes properly

---

### Test 2.4: Export to CSV

**Steps:**
1. Navigate to "Saved Reports" tab
2. Click "CSV" button on any report
3. Wait for download

**Expected Results:**
- ✅ Green toast notification: "Report exported to CSV"
- ✅ CSV file downloads immediately
- ✅ Filename format: `{report-name}.csv`
- ✅ Open CSV file - verify data present
- ✅ CSV contains report metadata and results

**Pass Criteria:**
- ✅ Toast displays
- ✅ File downloads successfully
- ✅ CSV file opens and contains data
- ✅ No errors in console

---

### Test 2.5: Export to Excel

**Steps:**
1. Navigate to "Saved Reports" tab
2. Click "Excel" button on any report
3. Wait for download

**Expected Results:**
- ✅ Green toast notification: "Report exported to Excel"
- ✅ XLSX file downloads immediately
- ✅ Filename format: `{report-name}.xlsx`
- ✅ Open in Excel/LibreOffice - verify formatted data
- ✅ Excel file contains properly formatted tables

**Pass Criteria:**
- ✅ Toast displays
- ✅ File downloads successfully
- ✅ Excel file opens correctly
- ✅ Data properly formatted

---

### Test 2.6: Delete Report with Confirmation

**Steps:**
1. Navigate to "Saved Reports" tab
2. Click "Delete" button on any report
3. Confirmation Dialog appears

**Expected Confirmation Dialog:**
- ✅ Warning icon displayed (red)
- ✅ Title: "Delete Report?"
- ✅ Message includes report name in bold
- ✅ Message: "This action cannot be undone."
- ✅ Two buttons:
  - "Cancel" (gray)
  - "Delete" (red)

**Test Cancel Path:**
1. Click "Cancel" button
2. **Expected:** Dialog closes, report NOT deleted
3. **Verify:** Report still in list

**Test Confirm Path:**
1. Click "Delete" button again on same report
2. Confirmation dialog appears
3. Click "Delete" (red button)

**Expected Results:**
- ✅ Green toast notification: "Report deleted successfully"
- ✅ Dialog closes immediately
- ✅ Report removed from list
- ✅ List refreshes automatically
- ✅ Toast auto-dismisses after 3 seconds

**Pass Criteria:**
- ✅ Confirmation dialog displays correctly
- ✅ Cancel works (no deletion)
- ✅ Delete works (report removed)
- ✅ Toast notification displays
- ✅ List updates immediately

---

### Test 2.7: Filter Reports by Type

**Steps:**
1. Generate at least 1 patient report and 1 population report
2. Navigate to "Saved Reports" tab
3. Click "Patient" filter button
4. Observe filtered list
5. Click "Population" filter button
6. Observe filtered list
7. Click "All Reports" filter button

**Expected Results:**
- ✅ "Patient" filter shows only PATIENT type reports
- ✅ "Population" filter shows only POPULATION type reports
- ✅ "All Reports" shows all reports
- ✅ Active filter button highlighted
- ✅ Report count updates correctly

**Pass Criteria:**
- ✅ Filters work correctly
- ✅ Counts accurate
- ✅ Active filter visually indicated

---

## Test Suite 3: Error Handling

### Test 3.1: Backend Unavailable

**Steps:**
1. Stop backend services: `docker compose down`
2. Attempt to generate patient report
3. Observe error handling

**Expected Results:**
- ✅ Red toast notification: "Failed to generate patient report"
- ✅ Toast duration: 5 seconds (longer for errors)
- ✅ User-friendly error message
- ✅ No application crash
- ✅ Can retry after restarting services

**Cleanup:**
1. Restart services: `docker compose up -d`
2. Verify functionality restored

---

### Test 3.2: Network Timeout

**Steps:**
1. Generate report for patient with complex data
2. Observe loading indicators

**Expected Results:**
- ✅ Loading spinner displays during request
- ✅ Toast notification on success or failure
- ✅ No hanging UI state
- ✅ Can cancel and retry

---

## Test Suite 4: Multiple Report Generation

### Test 4.1: Generate Multiple Reports in Sequence

**Steps:**
1. Generate patient report for James Anderson (ID: 84)
2. Wait for success toast
3. Generate patient report for Maria Rodriguez (ID: 85)
4. Wait for success toast
5. Generate patient report for David Chen (ID: 86)
6. Navigate to "Saved Reports" tab

**Expected Results:**
- ✅ 3 separate reports in list
- ✅ Each report has correct patient name
- ✅ All reports show status "COMPLETED"
- ✅ Reports ordered by creation date (newest first)
- ✅ All 3 reports functional (View, Export, Delete)

**Pass Criteria:**
- ✅ All reports generated successfully
- ✅ No duplicates or missing reports
- ✅ Correct ordering

---

## Test Suite 5: User Experience & Performance

### Test 5.1: Toast Notification Behavior

**Test Different Toast Types:**

**Success Toast:**
- Color: Green (#4caf50)
- Duration: 3 seconds
- Message: Positive action confirmation

**Error Toast:**
- Color: Red (#f44336)
- Duration: 5 seconds
- Message: Error description

**Test Interactions:**
1. Generate report (success toast)
2. Click "Close" button on toast
3. **Expected:** Toast dismisses immediately

**Pass Criteria:**
- ✅ Colors correct for each type
- ✅ Durations appropriate
- ✅ Manual dismiss works
- ✅ Positioned consistently (bottom-right)
- ✅ Readable text (white on color background)

---

### Test 5.2: Responsive Design

**Test Different Screen Sizes:**

**Desktop (1920x1080):**
- All features accessible
- Tables display fully
- No horizontal scrolling

**Tablet (768x1024):**
- Layout adapts
- Tables remain readable
- Touch targets appropriately sized

**Mobile (375x667):**
- Single column layout
- Buttons stack vertically
- Text remains readable

---

### Test 5.3: Loading States

**Verify Loading Indicators:**
1. Patient Selection Dialog - Shows spinner while loading patients
2. Report Generation - Button shows loading state
3. Report List - Shows spinner while fetching reports
4. Export Operations - Button disabled during export

**Pass Criteria:**
- ✅ All async operations show loading state
- ✅ Users cannot trigger duplicate actions
- ✅ Loading states clear after completion

---

## Test Suite 6: Accessibility

### Test 6.1: Keyboard Navigation

**Steps:**
1. Open Patient Selection Dialog
2. Use Tab key to navigate
3. Use Enter to select patient
4. Use Escape to close dialog

**Pass Criteria:**
- ✅ All interactive elements focusable
- ✅ Focus indicators visible
- ✅ Logical tab order
- ✅ Keyboard shortcuts work

---

### Test 6.2: Screen Reader Compatibility

**Test with Screen Reader:**
1. Navigate Reports page
2. Verify ARIA labels present
3. Verify meaningful announcements

**Pass Criteria:**
- ✅ All buttons have labels
- ✅ Dialogs announce properly
- ✅ Tables have headers
- ✅ Status changes announced

---

## Test Results Summary

### Session Information
- **Tester:** _____________
- **Date:** _____________
- **Browser:** _____________
- **OS:** _____________

### Test Results

| Test Suite | Test ID | Status | Notes |
|------------|---------|--------|-------|
| MRN Display | 1.1 | ⬜ Pass / ⬜ Fail | |
| MRN Display | 1.2 | ⬜ Pass / ⬜ Fail | |
| MRN Display | 1.3 | ⬜ Pass / ⬜ Fail | |
| Reports | 2.1 | ⬜ Pass / ⬜ Fail | |
| Reports | 2.2 | ⬜ Pass / ⬜ Fail | |
| Reports | 2.3 | ⬜ Pass / ⬜ Fail | |
| Reports | 2.4 | ⬜ Pass / ⬜ Fail | |
| Reports | 2.5 | ⬜ Pass / ⬜ Fail | |
| Reports | 2.6 | ⬜ Pass / ⬜ Fail | |
| Reports | 2.7 | ⬜ Pass / ⬜ Fail | |
| Error Handling | 3.1 | ⬜ Pass / ⬜ Fail | |
| Error Handling | 3.2 | ⬜ Pass / ⬜ Fail | |
| Multiple Reports | 4.1 | ⬜ Pass / ⬜ Fail | |
| UX/Performance | 5.1 | ⬜ Pass / ⬜ Fail | |
| UX/Performance | 5.2 | ⬜ Pass / ⬜ Fail | |
| UX/Performance | 5.3 | ⬜ Pass / ⬜ Fail | |
| Accessibility | 6.1 | ⬜ Pass / ⬜ Fail | |
| Accessibility | 6.2 | ⬜ Pass / ⬜ Fail | |

### Overall Status
- **Total Tests:** 18
- **Passed:** _____ / 18
- **Failed:** _____ / 18
- **Pass Rate:** _____ %

---

## Known Issues

Document any issues found during testing:

1. **Issue:** _______________
   - **Severity:** Critical / High / Medium / Low
   - **Steps to Reproduce:** _______________
   - **Expected:** _______________
   - **Actual:** _______________

---

## Browser Compatibility

Test in multiple browsers and document results:

| Browser | Version | Status | Notes |
|---------|---------|--------|-------|
| Chrome | _____ | ⬜ Pass / ⬜ Fail | |
| Firefox | _____ | ⬜ Pass / ⬜ Fail | |
| Safari | _____ | ⬜ Pass / ⬜ Fail | |
| Edge | _____ | ⬜ Pass / ⬜ Fail | |

---

## Performance Metrics

Document performance observations:

| Metric | Target | Actual | Status |
|--------|--------|--------|--------|
| Initial Page Load | < 2s | _____ | ⬜ Pass / ⬜ Fail |
| Report Generation | < 3s | _____ | ⬜ Pass / ⬜ Fail |
| Report List Load | < 1s | _____ | ⬜ Pass / ⬜ Fail |
| Export CSV | < 2s | _____ | ⬜ Pass / ⬜ Fail |
| Export Excel | < 3s | _____ | ⬜ Pass / ⬜ Fail |

---

## Sign-Off

### Tester Approval
- **Name:** _____________
- **Signature:** _____________
- **Date:** _____________
- **Recommendation:** ⬜ Approve for Production / ⬜ Requires Fixes

### Stakeholder Approval
- **Name:** _____________
- **Signature:** _____________
- **Date:** _____________
- **Approved:** ⬜ Yes / ⬜ No

---

## Appendix A: Quick Test Commands

### Backend Health Check
```bash
# Check all services
docker ps --filter name=healthdata

# Check quality measure service logs
docker logs healthdata-quality-measure --tail 50

# Test FHIR server
curl http://localhost:8083/fhir/Patient/84

# Test quality measure API
curl -H "X-Tenant-ID: tenant1" \
  http://localhost:8087/quality-measure/quality-measure/reports
```

### Frontend Build & Serve
```bash
# Production build
npx nx build clinical-portal --configuration=production

# Development serve
npx nx serve clinical-portal

# Check build size
du -sh dist/apps/clinical-portal/
```

---

## Appendix B: Test Patient Quick Reference

| ID | Name | MRN | Age | Gender |
|----|------|-----|-----|--------|
| 84 | James Anderson | MRN001234 | 60 | M |
| 85 | Maria Rodriguez | MRN002567 | 47 | F |
| 86 | David Chen | MRN003891 | 39 | M |
| 87 | Jennifer Williams | MRN004123 | 33 | F |
| 88 | Robert Taylor | MRN005456 | 67 | M |
| 89 | Patricia Martinez | MRN006789 | 54 | F |
| 90 | Michael Thompson | MRN007012 | 37 | M |
| 91 | Elizabeth Davis | MRN008345 | 30 | F |

All test patients have assigning authority: `http://hospital.example.org/patients`

---

**End of Manual Testing Guide**
