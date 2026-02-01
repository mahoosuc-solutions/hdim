# Manual Test Guide: Modal & Export Features

**Date:** January 24, 2026
**URL:** http://localhost:4201/audit-logs
**Focus:** Event Details Modal + Export Functionality

---

## 🎯 Test Objectives

1. Verify event details modal opens and displays all information
2. Test all 4 modal closing methods
3. Verify CSV export downloads and contains correct data
4. Verify JSON export downloads and is properly formatted
5. Test PDF export shows appropriate message

**Expected Duration:** 10-15 minutes

---

## ✅ Pre-Test Checklist

Before starting, verify:
- [ ] Browser is open to http://localhost:4201/audit-logs
- [ ] Page loaded successfully (no white screen or errors)
- [ ] Statistics dashboard visible at top (shows "Total Events: 1,247")
- [ ] Audit events table visible with data (should see ~20 rows)
- [ ] Export buttons visible in top-right corner

If any of the above fail, check browser console (F12) for errors.

---

## 🧪 Test 1: Open Event Details Modal (2 minutes)

### Steps:
1. Look at the audit events table (below the search filters)
2. Identify ANY row in the table
3. **Click anywhere in that row** (not on a button)
4. Observe what happens

### ✅ Expected Results:

**Modal Should Open:**
- [ ] Semi-transparent dark overlay appears behind modal
- [ ] Modal dialog centered on screen
- [ ] Modal header shows "Audit Event Details"
- [ ] Close button (×) visible in top-right corner of modal
- [ ] Modal content displays event information
- [ ] Page scrolling disabled (background is locked)

**Visual Check:**
```
┌─────────────────────────────────────────┐
│ Audit Event Details                  × │ ← Close button
├─────────────────────────────────────────┤
│ Event ID:     evt-XXX                   │
│ Timestamp:    Thursday, January 24...   │
│ User:         admin@hdim.ai (admin)     │
│ Role:         ADMIN                     │
│ Action:       [CREATE] ← Green badge    │
│ Outcome:      [SUCCESS] ← Green badge   │
│ ...                                     │
└─────────────────────────────────────────┘
```

### ❌ If Modal Doesn't Open:
- Check browser console (F12) for JavaScript errors
- Try clicking a different row
- Refresh page (F5) and try again
- Check that `selectedEvent` is being set in component

---

## 🧪 Test 2: Verify Modal Content (3 minutes)

### Steps:
1. With modal open, **scroll through all fields**
2. Read each section carefully
3. Check if data looks realistic

### ✅ Expected Results:

**Basic Metadata:**
- [ ] Event ID displayed (format: `evt-XXX`)
- [ ] Timestamp shows full date/time (Thursday, January 24, 2026...)
- [ ] User email shown (e.g., `admin@hdim.ai`)
- [ ] User ID shown in parentheses (e.g., `(admin)`)
- [ ] Role displayed (ADMIN, EVALUATOR, ANALYST, or VIEWER)

**Action & Outcome:**
- [ ] Action badge displayed with color (CREATE=green, READ=blue, etc.)
- [ ] Outcome badge displayed (SUCCESS=green, FAILURE=red, PARTIAL=yellow)

**Resource Information:**
- [ ] Resource Type shown (PATIENT, CARE_GAP, EVALUATION, etc.)
- [ ] Resource ID shown (if applicable)
- [ ] Service Name shown (e.g., `patient-service`)

**Technical Details:**
- [ ] IP Address displayed (format: `192.168.X.X`)
- [ ] User Agent shown (long browser string)
- [ ] Duration shown if available (e.g., `145ms` or `2.5s`)

**Payloads (Optional - may not be present for all events):**
- [ ] Request Payload section (gray box with formatted JSON)
- [ ] Response Payload section (gray box with formatted JSON)
- [ ] Error Message (red box, only for FAILURE outcomes)

### 📸 Visual Reference:

```
Event ID:        evt-001
Timestamp:       Thursday, January 24, 2026 at 12:08:45 PM
User:            admin@hdim.ai (admin)
Role:            ADMIN
Action:          [CREATE]     ← Green badge
Outcome:         [SUCCESS]    ← Green badge
Resource Type:   PATIENT
Resource ID:     patient-12345
Service:         patient-service
IP Address:      192.168.1.100
User Agent:      Mozilla/5.0 (X11; Linux x86_64)...
Duration:        145ms

Request Payload:
┌────────────────────────────────────┐
│ {                                  │
│   "patientId": "patient-12345",    │
│   "firstName": "John",             │
│   "lastName": "Doe"                │
│ }                                  │
└────────────────────────────────────┘
```

---

## 🧪 Test 3: Close Modal (4 Ways) (2 minutes)

Modal should close using **ALL 4 methods**. Test each one:

### Method 1: Click × Button
1. **Click the × button** in top-right corner of modal
2. ✅ Modal should disappear
3. ✅ Dark overlay should disappear
4. ✅ Can scroll page again

### Method 2: Click "Close" Button
1. **Open modal again** (click any table row)
2. **Scroll to bottom of modal**
3. **Click the "Close" button**
4. ✅ Modal should disappear

### Method 3: Click Outside Modal (Background)
1. **Open modal again**
2. **Click the dark background** (outside the white modal box)
3. ✅ Modal should disappear

### Method 4: Press ESC Key
1. **Open modal again**
2. **Press the ESC key** on keyboard
3. ✅ Modal should disappear

### ✅ All Methods Must Work:
- [ ] × button closes modal
- [ ] "Close" button closes modal
- [ ] Clicking outside closes modal
- [ ] ESC key closes modal

### ❌ If Any Method Fails:
- Check browser console for errors
- Verify you're clicking inside the overlay (not on the modal itself for Method 3)
- Try refreshing the page

---

## 🧪 Test 4: Export CSV (2 minutes)

### Steps:
1. Look at **top-right corner** of the page
2. Find the **"Export CSV"** button (📥 CSV icon)
3. **Click "Export CSV"**
4. Wait for download to complete
5. **Check your Downloads folder**

### ✅ Expected Results:

**Download Behavior:**
- [ ] Download starts immediately (within 1 second)
- [ ] Browser shows download progress bar
- [ ] File appears in Downloads folder
- [ ] Filename format: `audit-logs-2026-01-24.csv`

**File Verification:**
1. Open the CSV file in **Excel, Google Sheets, or text editor**
2. Verify contents:

```csv
ID,Timestamp,User,Action,Outcome,Resource Type,Service,IP Address
evt-001,2026-01-24T12:08:45Z,admin@hdim.ai,CREATE,SUCCESS,PATIENT,patient-service,192.168.1.100
evt-002,2026-01-24T11:30:22Z,analyst@hdim.ai,READ,SUCCESS,CARE_GAP,care-gap-service,192.168.1.101
...
```

**CSV Quality Checks:**
- [ ] Header row present with column names
- [ ] Data rows present (should match number shown in table)
- [ ] Columns properly separated (no broken/merged columns)
- [ ] Dates in ISO 8601 format (`2026-01-24T12:08:45Z`)
- [ ] No formatting issues (quotes escaped correctly)

### ❌ If CSV Export Fails:
- Check browser console for errors
- Check Downloads folder permissions
- Verify pop-up blocker didn't block download
- Try a different browser (Chrome, Firefox)

---

## 🧪 Test 5: Export JSON (2 minutes)

### Steps:
1. **Click "Export JSON"** button (📄 JSON icon)
2. Wait for download
3. **Check Downloads folder**
4. Open JSON file in **text editor** (VS Code, Notepad++, etc.)

### ✅ Expected Results:

**Download Behavior:**
- [ ] Download starts immediately
- [ ] File appears: `audit-logs-2026-01-24.json`
- [ ] File size reasonable (not empty)

**JSON Verification:**
1. Open in text editor
2. Check formatting:

```json
[
  {
    "id": "evt-001",
    "timestamp": "2026-01-24T12:08:45.123Z",
    "username": "admin@hdim.ai",
    "userId": "admin",
    "role": "ADMIN",
    "action": "CREATE",
    "outcome": "SUCCESS",
    "resourceType": "PATIENT",
    "resourceId": "patient-12345",
    "serviceName": "patient-service",
    "ipAddress": "192.168.1.100",
    "userAgent": "Mozilla/5.0...",
    "durationMs": 145
  },
  ...
]
```

**JSON Quality Checks:**
- [ ] Valid JSON syntax (can copy/paste into https://jsonlint.com)
- [ ] Pretty-printed (formatted with indentation, not minified)
- [ ] Array of objects: `[{...}, {...}, ...]`
- [ ] All expected fields present in each object
- [ ] No syntax errors (unmatched braces, commas, etc.)

### ❌ If JSON Export Fails:
- Same troubleshooting as CSV export
- Validate JSON at https://jsonlint.com if formatting looks wrong

---

## 🧪 Test 6: Export PDF (2 minutes)

### Steps:
1. **Click "Export PDF"** button (📑 PDF icon)
2. Observe behavior

### ✅ Expected Results (Two Scenarios):

**Scenario A: Backend Running (audit-query-service on port 8093)**
- [ ] Download starts
- [ ] File appears: `audit-logs-2026-01-24.pdf`
- [ ] PDF opens in viewer
- [ ] Contains formatted audit log report

**Scenario B: Backend NOT Running (Expected in Development)**
- [ ] Alert/toast message appears
- [ ] Message says: **"PDF export not available. Please use CSV or JSON export."**
- [ ] No file downloads
- [ ] This is EXPECTED behavior (PDF requires backend)

### 📝 Notes:
- PDF export requires the audit-query-service backend running
- In development with mock data, you'll likely see Scenario B
- This is intentional - CSV/JSON work client-side, PDF needs backend

---

## 🧪 Test 7: Export with Filters (BONUS - 2 minutes)

### Steps:
1. **Apply a filter** (e.g., type "admin" in Username field)
2. Verify table shows **only admin events**
3. **Click "Export CSV"**
4. Open CSV file
5. Check if CSV contains **only admin events**

### ✅ Expected Results:
- [ ] CSV row count matches filtered table count
- [ ] All CSV rows have username = "admin@hdim.ai"
- [ ] Export respects current filter state
- [ ] CSV doesn't contain events from other users

This verifies exports respect the current filter/search state.

---

## 📊 Final Verification Checklist

After completing all tests:

### Modal Tests (3/7 total tests)
- [ ] Modal opens when clicking table row
- [ ] Modal displays all event details correctly
- [ ] All 4 close methods work (×, Close button, outside click, ESC)

### Export Tests (4/7 total tests)
- [ ] CSV export downloads and contains correct data
- [ ] CSV opens properly in Excel/Sheets
- [ ] JSON export downloads and is valid/formatted
- [ ] PDF shows appropriate message (available or not)
- [ ] Export respects current filters (bonus test)

**Total Tests Passed:** __ / 7

---

## 🎉 Success Criteria

**Minimum Requirements for PASS:**
- ✅ Modal opens and closes (all 4 methods)
- ✅ Modal displays event details
- ✅ CSV export downloads and contains data
- ✅ JSON export downloads and is valid

**Full PASS:**
- ✅ All 7 tests pass
- ✅ No browser console errors
- ✅ Exports respect filters

---

## 🐛 Troubleshooting

### Modal won't open
- **Check:** Browser console (F12) for JavaScript errors
- **Try:** Click different rows
- **Try:** Refresh page (F5)

### Export doesn't download
- **Check:** Browser download permissions
- **Check:** Pop-up blocker settings
- **Try:** Different browser (Chrome recommended)

### CSV/JSON is empty
- **Check:** Table has data visible
- **Try:** Remove all filters to export all data

### PDF export fails
- **Expected:** Backend not running (use CSV/JSON instead)
- **Check:** Is audit-query-service running on port 8093?

---

## 📝 Test Results

**Date Tested:** _______________
**Browser:** _______________
**Tester:** _______________

**Results:**
- Modal Tests: __ / 3 passed
- Export Tests: __ / 4 passed
- **Overall: __ / 7 passed**

**Status:** ⬜ PASS | ⬜ FAIL | ⬜ PARTIAL

**Issues Found:**

1. _______________________________________________
2. _______________________________________________
3. _______________________________________________

**Notes:**



---

**Next Steps:**
- ✅ If all pass: Feature is ready for backend integration testing
- ⚠️ If some fail: Document issues and report to development team
- 📝 Complete comprehensive testing: See `AUDIT_LOG_VIEWER_COMPLETE_TEST_SUMMARY.md`

---

**Last Updated:** January 24, 2026
