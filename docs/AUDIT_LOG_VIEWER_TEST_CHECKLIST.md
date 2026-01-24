# Enhanced Audit Log Viewer - Test Checklist

**Date:** January 24, 2026
**Tester:** Manual Testing Guide
**URL:** http://localhost:4201/audit-logs

---

## Test Environment

- [x] Admin portal running on http://localhost:4201
- [x] Browser opened to audit logs page
- [x] Using mock data (backend not required)

---

## Test 1: Page Load & Initial State

**Objective:** Verify page loads correctly with mock data

### Steps:
1. Navigate to http://localhost:4201/audit-logs
2. Wait for page to load completely

### Expected Results:
- [ ] Statistics dashboard displays 5 stat cards
- [ ] Total events shows a number (should be ~1,247 from mock data)
- [ ] Success/Failure/Partial counts are visible
- [ ] Search filters section is visible
- [ ] Table displays at least 2 rows of audit events
- [ ] Table shows columns: Timestamp, User, Action, Resource, Outcome, Service, Duration, Actions
- [ ] Export buttons visible in header (CSV, JSON, PDF)
- [ ] No console errors in browser DevTools (F12)

### Status: ⬜ Not Started | ✅ Passed | ❌ Failed

---

## Test 2: Full-Text Search

**Objective:** Verify search box filters events across all fields

### Steps:
1. Click in the "Search" input box
2. Type "admin" (lowercase)
3. Wait 500ms for debounce
4. Observe table results

### Expected Results:
- [ ] Search input accepts text
- [ ] After 500ms delay, table filters automatically
- [ ] Table shows only events containing "admin" in ANY field
- [ ] Event count in table decreases
- [ ] Statistics update to reflect filtered results

### Test Cases:
| Search Term | Expected Behavior |
|-------------|-------------------|
| "admin" | Shows events with username "admin@hdim.ai" |
| "patient" | Shows events with resourceType "PATIENT" |
| "CREATE" | Shows events with action "CREATE" |
| "192.168" | Shows events from that IP range |

### Status: ⬜ Not Started | ✅ Passed | ❌ Failed

---

## Test 3: Username Filter

**Objective:** Verify username filter works independently

### Steps:
1. Clear the search box (if any text present)
2. In the "Username" field, type "analyst"
3. Click "Apply Filters" button
4. Observe table results

### Expected Results:
- [ ] Table filters to show only events from users with "analyst" in username
- [ ] Statistics update
- [ ] Other filters remain unchanged
- [ ] Clear indication of filtered state

### Status: ⬜ Not Started | ✅ Passed | ❌ Failed

---

## Test 4: Role Filter

**Objective:** Verify role filter works

### Steps:
1. Click "Reset Filters" button
2. In the "Role" field, type "ADMIN"
3. Click "Apply Filters" button
4. Observe results

### Expected Results:
- [ ] Table shows only events from users with role "ADMIN"
- [ ] Role badges in User column all show "ADMIN"
- [ ] Statistics reflect filtered data

### Status: ⬜ Not Started | ✅ Passed | ❌ Failed

---

## Test 5: Resource Type Filter

**Objective:** Verify resource type filtering

### Steps:
1. Click "Reset Filters"
2. In "Resource Type" field, type "PATIENT"
3. Click "Apply Filters"
4. Check Resource column

### Expected Results:
- [ ] All visible events have resourceType = "PATIENT"
- [ ] Table filters correctly
- [ ] Statistics update

### Status: ⬜ Not Started | ✅ Passed | ❌ Failed

---

## Test 6: Date Range Filter

**Objective:** Verify date range filtering works

### Steps:
1. Click "Reset Filters"
2. Click on "Start Date" input
3. Select today's date, set time to 00:00
4. Click on "End Date" input
5. Select today's date, set time to 23:59
6. Click "Apply Filters"

### Expected Results:
- [ ] Date picker opens for both fields
- [ ] Selected dates display correctly
- [ ] Table filters to show only events within date range
- [ ] Events outside date range are hidden

### Status: ⬜ Not Started | ✅ Passed | ❌ Failed

---

## Test 7: Multi-Select Actions Filter

**Objective:** Verify action multi-select filter

### Steps:
1. Click "Reset Filters"
2. Click on "Actions" dropdown
3. Hold Ctrl/Cmd and select multiple actions:
   - CREATE
   - READ
   - UPDATE
4. Click "Apply Filters"
5. Observe results

### Expected Results:
- [ ] Multi-select allows selecting multiple options
- [ ] Table shows only events with selected actions
- [ ] Action badges in table match selected filters
- [ ] Statistics update correctly

### Status: ⬜ Not Started | ✅ Passed | ❌ Failed

---

## Test 8: Multi-Select Outcomes Filter

**Objective:** Verify outcome multi-select filter

### Steps:
1. Click "Reset Filters"
2. Click on "Outcomes" dropdown
3. Select "SUCCESS" and "FAILURE" (hold Ctrl/Cmd)
4. Click "Apply Filters"
5. Check Outcome column

### Expected Results:
- [ ] Can select multiple outcomes
- [ ] Table shows only SUCCESS or FAILURE events
- [ ] PARTIAL events are hidden
- [ ] Outcome badges match filter

### Status: ⬜ Not Started | ✅ Passed | ❌ Failed

---

## Test 9: Combined Filters

**Objective:** Verify multiple filters work together

### Steps:
1. Click "Reset Filters"
2. Enter username: "admin"
3. Select actions: CREATE, UPDATE
4. Select outcome: SUCCESS
5. Click "Apply Filters"

### Expected Results:
- [ ] Table shows events matching ALL criteria:
  - Username contains "admin" AND
  - Action is CREATE or UPDATE AND
  - Outcome is SUCCESS
- [ ] Results are intersection (AND), not union (OR)
- [ ] Statistics reflect combined filters

### Status: ⬜ Not Started | ✅ Passed | ❌ Failed

---

## Test 10: Reset Filters

**Objective:** Verify reset button clears all filters

### Steps:
1. Apply several filters (username, actions, date range)
2. Click "Reset Filters" button
3. Observe all filter fields

### Expected Results:
- [ ] All text inputs clear to empty
- [ ] All dropdowns reset to default (no selection)
- [ ] Date inputs clear
- [ ] Table shows all events (unfiltered)
- [ ] Statistics return to full dataset counts
- [ ] Table automatically reloads

### Status: ⬜ Not Started | ✅ Passed | ❌ Failed

---

## Test 11: Search Debouncing

**Objective:** Verify search doesn't trigger on every keystroke

### Steps:
1. Click "Reset Filters"
2. Type "admin" quickly in search box (within 500ms)
3. Watch network tab in DevTools (F12)
4. Count number of API calls

### Expected Results:
- [ ] Only 1 API call after typing stops
- [ ] No API call for each character typed
- [ ] 500ms delay before search triggers
- [ ] Results update after debounce period

### Status: ⬜ Not Started | ✅ Passed | ❌ Failed

---

## Test 12: Filter Persistence

**Objective:** Verify filters persist during pagination/sorting

### Steps:
1. Apply username filter: "analyst"
2. Click column header to sort
3. Check if filter is still applied
4. Navigate to page 2 (if available)
5. Check if filter is still applied

### Expected Results:
- [ ] Filters remain active after sorting
- [ ] Filters remain active after pagination
- [ ] Filter inputs show selected values
- [ ] Table continues to show filtered results

### Status: ⬜ Not Started | ✅ Passed | ❌ Failed

---

## Test 13: Service Name Filter

**Objective:** Verify service name filtering

### Steps:
1. Click "Reset Filters"
2. In "Service" field, type "patient-service"
3. Click "Apply Filters"
4. Check Service column in table

### Expected Results:
- [ ] Table shows only events from "patient-service"
- [ ] Other services are hidden
- [ ] Service names in table match filter

### Status: ⬜ Not Started | ✅ Passed | ❌ Failed

---

## Test 14: Empty Results

**Objective:** Verify empty state displays correctly

### Steps:
1. Click "Reset Filters"
2. In search box, type "xxxxxxx" (gibberish)
3. Wait for debounce
4. Observe results

### Expected Results:
- [ ] Table is empty (no rows)
- [ ] Empty state message displays:
  - Icon: 📋
  - Title: "No Audit Events Found"
  - Message: "No events match your current filter criteria..."
- [ ] Statistics show 0 events
- [ ] No loading spinner visible

### Status: ⬜ Not Started | ✅ Passed | ❌ Failed

---

## Test 15: Filter Validation

**Objective:** Verify invalid date ranges are handled

### Steps:
1. Click "Reset Filters"
2. Set "End Date" to yesterday
3. Set "Start Date" to today
4. Click "Apply Filters"
5. Observe behavior

### Expected Results:
- [ ] Either shows validation error OR
- [ ] Returns empty results (no events in invalid range) OR
- [ ] Auto-corrects date range
- [ ] User is informed of issue

### Status: ⬜ Not Started | ✅ Passed | ❌ Failed

---

## Test 16: Browser Console Check

**Objective:** Verify no JavaScript errors during filtering

### Steps:
1. Open browser DevTools (F12)
2. Go to Console tab
3. Click "Reset Filters"
4. Apply various filter combinations
5. Check for errors

### Expected Results:
- [ ] No red error messages in console
- [ ] No warnings about failed API calls
- [ ] Only expected network requests (search, statistics)
- [ ] No "undefined" or "null" errors

### Status: ⬜ Not Started | ✅ Passed | ❌ Failed

---

## Test Results Summary

### Pass/Fail Count
- Total Tests: 16
- Passed: ___
- Failed: ___
- Not Started: ___
- Pass Rate: ___%

### Critical Issues Found
(List any blocking issues here)

1.
2.
3.

### Minor Issues Found
(List cosmetic or minor issues here)

1.
2.
3.

### Notes
(Any additional observations)


---

## Next Steps Based on Results

### If All Tests Pass ✅
- [ ] Proceed to Test Checklist 2: Table Functionality (sorting, pagination)
- [ ] Proceed to Test Checklist 3: Event Details Modal
- [ ] Proceed to Test Checklist 4: Export Functionality
- [ ] Proceed to Test Checklist 5: Accessibility Testing

### If Any Tests Fail ❌
- [ ] Document issues in GitHub Issue #248
- [ ] Check browser console for errors
- [ ] Verify mock data is loading correctly
- [ ] Test in different browser (Chrome, Firefox, Safari)
- [ ] Report to development team

---

**Tested By:** _______________
**Date:** _______________
**Browser:** _______________ (Version: _______)
**OS:** _______________
**Result:** ⬜ PASS | ⬜ FAIL | ⬜ PARTIAL

---

## Quick Command Reference

```bash
# Check if server is running
lsof -ti:4201

# View server logs
tail -50 /tmp/claude/-mnt-wdblack-dev-projects-hdim-master/tasks/b39aebb.output

# Restart server if needed
nx serve admin-portal --port=4201

# Open in browser
xdg-open http://localhost:4201/audit-logs
```

---

**Last Updated:** January 24, 2026
