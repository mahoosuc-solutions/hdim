# Quick Test Guide: Event Details Modal & Export (5 Minutes)

**URL:** http://localhost:4201/audit-logs

---

## ⚡ Part A: Event Details Modal (2 minutes)

### Test 1: Open Modal (30 seconds)

**Steps:**
1. Look at the audit logs table
2. Click on ANY row (anywhere in the row)
3. Observe what happens

**✅ PASS if:**
- Modal dialog appears centered on screen
- Background darkens (semi-transparent overlay)
- Modal shows "Audit Event Details" header
- Close button (×) visible in top-right corner
- Modal contains event information

**Visual:**
```
       [Dark overlay behind]
    ┌─────────────────────────────┐
    │ Audit Event Details      × │
    ├─────────────────────────────┤
    │ Event ID:     evt-001       │
    │ Timestamp:    Jan 24...     │
    │ User:         admin@...     │
    │ Action:       [CREATE]      │
    │ Outcome:      [SUCCESS]     │
    │ ...                         │
    ├─────────────────────────────┤
    │                    [Close]  │
    └─────────────────────────────┘
```

---

### Test 2: Modal Content (1 minute)

**Steps:**
1. With modal open, scroll through all fields
2. Check if all information is visible

**✅ PASS if you see:**
- [ ] Event ID (looks like: evt-001)
- [ ] Timestamp (full date: Thursday, January 24, 2026...)
- [ ] User (email format: admin@hdim.ai)
- [ ] Role (e.g., ADMIN, EVALUATOR)
- [ ] Action (colored badge: CREATE, READ, UPDATE, etc.)
- [ ] Outcome (colored badge: SUCCESS, FAILURE, PARTIAL)
- [ ] Resource Type (e.g., PATIENT, CARE_GAP)
- [ ] Service Name (e.g., patient-service)
- [ ] IP Address (e.g., 192.168.1.100)
- [ ] User Agent (long browser string)

**Bonus fields (if present):**
- [ ] Duration (e.g., 145ms or 2.5s)
- [ ] Request Payload (JSON in gray box)
- [ ] Response Payload (JSON in gray box)
- [ ] Error Message (red box, only for FAILURE outcomes)

---

### Test 3: Close Modal (30 seconds)

**Try ALL 4 methods:**

1. **Click × button** (top-right)
   - ✓ Modal closes

2. **Click "Close" button** (bottom)
   - ✓ Modal closes

3. **Click outside modal** (dark background)
   - ✓ Modal closes

4. **Press ESC key**
   - ✓ Modal closes

**✅ PASS if:**
- All 4 methods close the modal
- Modal disappears completely
- Can scroll page again after closing
- No errors in browser console (F12)

---

## ⚡ Part B: Export Functionality (3 minutes)

### Test 4: Export CSV (1 minute)

**Steps:**
1. Look at top-right corner of page
2. Find "Export CSV" button
3. Click it
4. Check your browser's download folder

**✅ PASS if:**
- Download starts immediately
- File appears in downloads: `audit-logs-2026-01-24.csv`
- File opens in Excel/Google Sheets
- CSV contains:
  - Header row with column names
  - Data rows with event information
  - Properly formatted (no broken columns)

**Expected CSV Preview:**
```
ID,Timestamp,User,Action,Outcome,Resource...
evt-001,2026-01-24T12:08:45Z,admin@hdim.ai,CREATE,SUCCESS,PATIENT...
evt-002,2026-01-24T11:30:22Z,analyst@hdim.ai,READ,SUCCESS,CARE_GAP...
```

---

### Test 5: Export JSON (1 minute)

**Steps:**
1. Click "Export JSON" button
2. Wait for download
3. Open downloaded file in text editor (Notepad, VS Code, etc.)

**✅ PASS if:**
- Download starts immediately
- File appears: `audit-logs-2026-01-24.json`
- File opens in text editor
- JSON is formatted (pretty-printed, not minified)
- Contains array of event objects: `[{...}, {...}]`
- Valid JSON (can copy/paste into JSONLint.com)

**Expected JSON Preview:**
```json
[
  {
    "id": "evt-001",
    "timestamp": "2026-01-24T12:08:45.123Z",
    "username": "admin@hdim.ai",
    "action": "CREATE",
    "outcome": "SUCCESS",
    "resourceType": "PATIENT",
    ...
  },
  ...
]
```

---

### Test 6: Export PDF (1 minute)

**Steps:**
1. Click "Export PDF" button
2. Observe behavior

**✅ PASS if (Backend Running):**
- Download starts
- File appears: `audit-logs-2026-01-24.pdf`
- PDF opens in viewer
- Contains formatted report

**✅ PASS if (Backend NOT Running - Expected):**
- Alert/error message appears
- Message says: "PDF export not available. Please use CSV or JSON export."
- No file downloads
- This is EXPECTED behavior (PDF needs backend)

---

## ⚡ Bonus Test: Export with Filters (Optional - 1 minute)

**Steps:**
1. Apply a filter (e.g., username = "admin")
2. Verify table shows only admin events
3. Click "Export CSV"
4. Open CSV file
5. Check if CSV contains only admin events

**✅ PASS if:**
- CSV contains only filtered events
- Row count in CSV matches filtered table
- Export respects current filters

---

## Quick Checklist (Must All Work)

### Modal Tests
- [ ] Can open modal by clicking table row
- [ ] Modal shows all event details
- [ ] Can close modal with × button
- [ ] Can close modal with Close button
- [ ] Can close modal by clicking outside
- [ ] Can close modal with ESC key

### Export Tests
- [ ] CSV export downloads file
- [ ] CSV opens in Excel/Sheets
- [ ] JSON export downloads file
- [ ] JSON is valid and formatted
- [ ] PDF shows appropriate message (available or not)
- [ ] Export respects filters (bonus)

**Total:** __ / 12 tests passed

---

## 🎯 Expected File Downloads

After testing, you should have these files in your Downloads folder:

```
Downloads/
├── audit-logs-2026-01-24.csv       ← CSV export
├── audit-logs-2026-01-24.json      ← JSON export
└── audit-logs-2026-01-24.pdf       ← PDF (if backend running)
```

---

## 🐛 Common Issues & Solutions

### Modal doesn't open
- **Check:** Click directly on table row (not header)
- **Check:** Browser console (F12) for JavaScript errors

### Modal won't close
- **Try:** Refresh page (F5)
- **Check:** ESC key is working

### Export doesn't download
- **Check:** Browser download permissions
- **Check:** Pop-up blocker settings
- **Try:** Different browser (Chrome, Firefox)

### CSV/JSON is empty
- **Check:** Table has data (apply filters if needed)
- **Try:** Remove all filters to export all data

### PDF export fails
- **Expected:** Backend service not running (this is OK)
- **Message:** Should show "PDF export not available"
- **Fallback:** Use CSV or JSON instead

---

## 📊 Visual Reference

### Modal Layout
```
┌─────────────────────────────────────────┐
│ Audit Event Details                  × │ ← Can click to close
├─────────────────────────────────────────┤
│                                          │
│  Event ID:        evt-001               │
│  Timestamp:       Thursday, Jan 24...   │ ← Full timestamp
│  User:            admin@hdim.ai (admin) │ ← Email + ID
│  Role:            ADMIN                  │
│  Action:          [CREATE]              │ ← Green badge
│  Outcome:         [SUCCESS]             │ ← Green badge
│  Resource Type:   PATIENT                │
│  Resource ID:     patient-12345         │
│  Service:         patient-service        │
│  IP Address:      192.168.1.100        │ ← Monospace
│  User Agent:      Mozilla/5.0...        │
│  Duration:        145ms                  │
│                                          │
│  Request Payload:                        │
│  ┌────────────────────────────────────┐ │
│  │ {                                   │ │ ← Gray box
│  │   "patientId": "patient-12345"     │ │   Formatted JSON
│  │ }                                   │ │
│  └────────────────────────────────────┘ │
│                                          │
├─────────────────────────────────────────┤
│                              [Close]    │ ← Can click to close
└─────────────────────────────────────────┘

Click outside to close →  [Dark background]
```

### Export Buttons Location
```
┌─────────────────────────────────────────────┐
│  Audit Logs    [📥 CSV] [📄 JSON] [📑 PDF] │ ← Top right
├─────────────────────────────────────────────┤
│  [Statistics cards...]                       │
│  [Search filters...]                         │
│  [Table...]                                  │
└─────────────────────────────────────────────┘
```

---

## Test Results

**Date Tested:** _______________
**Browser:** _______________
**Overall Result:** ⬜ PASS | ⬜ FAIL | ⬜ PARTIAL

**Modal Tests:** __ / 6 passed
**Export Tests:** __ / 6 passed

**Notes:**


---

## Next Steps

- ✅ **If all tests pass:** Feature is fully functional!
- ⚠️ **If some tests fail:** Document issues and report
- 📝 **Complete testing:** See full test document for 20 detailed tests

**Full Test Document:** `AUDIT_LOG_VIEWER_TEST_MODAL_EXPORT.md`

---

**Last Updated:** January 24, 2026
