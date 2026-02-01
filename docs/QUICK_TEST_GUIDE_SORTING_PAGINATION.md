# Quick Test Guide: Sorting & Pagination (5 Minutes)

**URL:** http://localhost:4201/audit-logs

---

## ⚡ Quick Functionality Tests

### Test 1: Basic Sorting (1 minute)

**Steps:**
1. Look at the table - note the first event's timestamp
2. Click on "Timestamp" column header
3. Note the sort indicator (▲ or ▼)
4. Check if first event's timestamp changed

**✅ PASS if:**
- Sort indicator appears next to "Timestamp"
- Clicking toggles between ▲ (ASC) and ▼ (DESC)
- Table rows reorder based on sort direction
- Newest events first (DESC) or oldest events first (ASC)

**Example:**
```
Before: Jan 24, 2026 12:08 PM  (newest)
        Jan 24, 2026 11:30 AM
        Jan 24, 2026 10:15 AM

After click (ASC):
        Jan 23, 2026 8:00 AM   (oldest)
        Jan 23, 2026 9:15 AM
        Jan 24, 2026 10:15 AM
```

---

### Test 2: Multi-Column Sorting (1 minute)

**Steps:**
1. Click "User" column header
2. Note sort indicator moves to "User" column
3. Check if usernames are in alphabetical order
4. Click "Action" column
5. Verify sort switches to Action column

**✅ PASS if:**
- Only one column shows sort indicator at a time
- Clicking different column changes sort
- Previous column's indicator disappears
- Each column sorts its data type correctly:
  - User: Alphabetically (admin, analyst, evaluator)
  - Action: Alphabetically (CREATE, DELETE, READ, UPDATE)
  - Outcome: Alphabetically (FAILURE, PARTIAL, SUCCESS)
  - Duration: Numerically (2ms, 15ms, 98ms, 145ms)

---

### Test 3: Pagination Navigation (2 minutes)

**Steps:**
1. Scroll to bottom of table
2. Find pagination controls
3. Note current page indicator (should be page 1, highlighted in blue)
4. Click "Next »" button
5. Observe page change
6. Click "« Previous" button

**✅ PASS if:**
- Pagination shows: `« Previous [1] [2] [3] [4] [5] Next »`
- Current page is highlighted (blue background)
- "Previous" button is disabled (grayed out) on page 1
- Clicking "Next" loads page 2 with new events
- Page indicator updates to [2]
- "Previous" button becomes enabled on page 2
- Clicking "Previous" returns to page 1
- Table shows different events on each page

**Visual Check:**
```
Page 1: « Previous [1] [2] [3] [4] [5] Next »
                   ^^^
                  (blue)

Page 2: « Previous [1] [2] [3] [4] [5] Next »
                       ^^^
                      (blue)
```

---

### Test 4: Pagination Info (30 seconds)

**Steps:**
1. Look at pagination info text below page numbers
2. Note the range displayed
3. Navigate to page 2
4. Check updated range

**✅ PASS if:**
- Page 1 shows: "Showing 1-20 of [total] events"
- Page 2 shows: "Showing 21-40 of [total] events"
- Total count matches statistics dashboard
- Numbers use comma separators (e.g., 1,247)

---

### Test 5: Direct Page Navigation (30 seconds)

**Steps:**
1. Click on page number "3" directly
2. Observe jump to page 3
3. Click on page number "1" to return

**✅ PASS if:**
- Clicking page number navigates directly to that page
- Page 3 highlights in blue
- Table updates with events 41-60
- Can jump back to page 1 by clicking [1]

---

### Test 6: Sort Persistence During Pagination (1 minute)

**Steps:**
1. Click "Username" column to sort
2. Note sort indicator (▲)
3. Click "Next" to go to page 2
4. Check if sort indicator still shows on "Username"
5. Verify page 2 continues alphabetical order from page 1

**✅ PASS if:**
- Sort indicator remains on Username column after pagination
- Last username on page 1 < First username on page 2 (alphabetically)
- Sort order maintained across all pages
- Example:
  ```
  Page 1 last user:  admin@hdim.ai
  Page 2 first user: analyst@hdim.ai ✓ (alphabetically after admin)
  ```

---

## 🎯 Expected Results Summary

### Sorting Behavior
| Column | ASC Order | DESC Order |
|--------|-----------|------------|
| Timestamp | Oldest → Newest | Newest → Oldest |
| User | A → Z | Z → A |
| Action | A → Z | Z → A |
| Resource | A → Z | Z → A |
| Outcome | FAILURE → PARTIAL → SUCCESS | SUCCESS → PARTIAL → FAILURE |
| Service | A → Z | Z → A |
| Duration | 2ms → 145ms | 145ms → 2ms |

### Pagination Behavior
- **Page Size:** 20 events per page
- **Total Pages:** ~63 pages (for 1,247 mock events)
- **Page Numbers Shown:** Max 5 at a time
- **Navigation:** Previous/Next buttons + direct page numbers
- **Info Display:** "Showing X-Y of Z events"

---

## 🐛 Common Issues & Solutions

### Issue: Sort indicator doesn't appear
**Solution:** Check browser console (F12) for JavaScript errors

### Issue: Clicking column header doesn't sort
**Solution:** Verify "sortable" class is on header, check click handler

### Issue: Pagination buttons don't respond
**Solution:** Check if buttons are disabled (should be grayed out when unavailable)

### Issue: Page numbers don't highlight
**Solution:** Verify current page state is updating correctly

### Issue: Sort resets when paginating
**Solution:** This is a bug - sort should persist across pages

---

## ✅ Quick Pass/Fail Checklist

Go through these quickly:

- [ ] Can sort by clicking column headers
- [ ] Sort indicator (▲/▼) appears next to sorted column
- [ ] Only one column shows sort indicator at a time
- [ ] Clicking same header toggles ASC/DESC
- [ ] Pagination controls visible at bottom
- [ ] Current page is highlighted (blue background)
- [ ] "Next" button navigates to next page
- [ ] "Previous" button navigates to previous page
- [ ] "Previous" disabled on page 1
- [ ] "Next" disabled on last page
- [ ] Can click page numbers to jump directly
- [ ] Pagination info shows correct range
- [ ] Sort persists when navigating pages
- [ ] Different events show on each page (no duplicates)
- [ ] No console errors when sorting/paginating

**Total:** __ / 15 tests passed

---

## 🎬 Video Test Recording Suggestion

If recording a demo, show:
1. Click each sortable column (7 columns)
2. Navigate through pages 1 → 2 → 3 → 1
3. Sort by Username, then paginate to show persistence
4. Show pagination info updating

**Total demo time:** ~3 minutes

---

## 📊 Test Results

**Date Tested:** _______________
**Browser:** _______________
**Overall Result:** ⬜ PASS | ⬜ FAIL | ⬜ PARTIAL

**Notes:**


---

**Next Steps:**
- If PASS → Test Event Details Modal
- If FAIL → Document issues in GitHub #248
