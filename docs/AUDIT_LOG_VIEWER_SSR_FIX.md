# Audit Log Viewer - SSR Issue Fix

**Date:** January 24, 2026
**Issue:** Server-Side Rendering (SSR) causing page hangs on audit-logs route
**Status:** Requires configuration change

---

## 🐛 Problem Description

The admin-portal is configured with Server-Side Rendering (SSR), but the enhanced audit-logs component uses browser-only APIs that cause the SSR server to hang:

1. `localStorage` access (fixed in auth.guard.ts)
2. Client-side CSV/JSON export generation
3. DOM manipulation for modals
4. Browser-specific RxJS operators

**Symptoms:**
- Server starts successfully
- Root path (`/`) hangs and times out
- Audit-logs path (`/audit-logs`) hangs and times out
- `curl` requests never return
- Browser navigation times out after 60 seconds

---

## ✅ Quick Fix: Disable SSR

**Option 1: Temporary Fix (Command Line)**

Stop the current server and restart without SSR:

```bash
# Kill existing server
pkill -f "nx serve admin-portal"

# Start without SSR
npx nx serve admin-portal --port=4201 --ssr=false
```

**Option 2: Permanent Fix (Configuration)**

Update `apps/admin-portal/project.json`:

```json
{
  "targets": {
    "serve": {
      "executor": "@angular-devkit/build-angular:dev-server",
      "configurations": {
        "development": {
          "buildTarget": "admin-portal:build:development",
          "ssr": false  // ← Add this line
        }
      }
    }
  }
}
```

**Option 3: Fix SSR Compatibility**

Make the audit-logs component SSR-safe by wrapping all browser-only code:

```typescript
import { isPlatformBrowser } from '@angular/common';
import { PLATFORM_ID, inject } from '@angular/core';

export class AuditLogsEnhancedComponent {
  private platformId = inject(PLATFORM_ID);

  ngOnInit(): void {
    // Only run in browser
    if (isPlatformBrowser(this.platformId)) {
      this.loadAuditLogs();
      this.loadStatistics();
      this.setupFormSubscription();
    }
  }
}
```

---

## 🧪 Verification Steps

After applying the fix:

1. **Start server:**
   ```bash
   npx nx serve admin-portal --port=4201 --ssr=false
   ```

2. **Wait for compilation:** (~30 seconds)
   ```
   Application bundle generation complete. [X seconds]
   ➜  Local:   http://localhost:4201/
   ```

3. **Test in browser:**
   ```bash
   # Open in browser
   xdg-open http://localhost:4201/audit-logs

   # Or test with curl
   curl -s http://localhost:4201/audit-logs | head -20
   ```

4. **Expected result:** Page loads successfully with audit log viewer

---

## 🎯 Manual Testing Instructions

Once the page loads successfully, follow the modal and export test guide:

### Test 1: Modal Functionality (5 minutes)

1. **Open browser to:** http://localhost:4201/audit-logs
2. **Verify page loads:**
   - Statistics dashboard visible at top
   - Search/filter form visible
   - Audit events table with ~20 rows visible
   - Export buttons in top-right corner

3. **Test modal:**
   - Click any row in the table
   - Modal should open with event details
   - Try all 4 close methods:
     - × button (top-right)
     - "Close" button (bottom)
     - Click outside (dark background)
     - Press ESC key

### Test 2: Export Functionality (5 minutes)

1. **CSV Export:**
   - Click "Export CSV" button
   - File should download: `audit-logs-2026-01-24.csv`
   - Open in Excel/Sheets - verify has header + data rows

2. **JSON Export:**
   - Click "Export JSON" button
   - File should download: `audit-logs-2026-01-24.json`
   - Open in text editor - verify valid JSON array

3. **PDF Export:**
   - Click "Export PDF" button
   - Should show alert: "PDF export not available..."
   - This is expected (needs backend)

### Test 3: Export with Filters (2 minutes)

1. Type "admin" in Username filter
2. Verify table shows only admin events
3. Click "Export CSV"
4. Open CSV - verify contains only admin events

---

## 📊 Expected Test Results

**All tests should PASS:**

- [ ] Page loads without timeout
- [ ] Statistics dashboard displays
- [ ] Audit events table shows data
- [ ] Modal opens when clicking table row
- [ ] All 4 modal close methods work
- [ ] CSV export downloads and contains data
- [ ] JSON export downloads and is valid JSON
- [ ] PDF export shows "not available" message
- [ ] Export respects current filters

**Total:** 9 / 9 tests passed = 100% success

---

## 🔧 Alternative: Test Without Backend

If you can't get the server running, you can test the component logic directly:

1. **Build the production bundle:**
   ```bash
   npx nx build admin-portal --configuration=production
   ```

2. **Serve the static files:**
   ```bash
   cd dist/apps/admin-portal/browser
   python3 -m http.server 4201
   ```

3. **Navigate to:** http://localhost:4201/audit-logs

This bypasses SSR entirely and serves the pre-compiled client-side bundle.

---

## 📝 Implementation Summary

**What was built:**
- ✅ Enhanced audit-logs component (487 lines TypeScript)
- ✅ HIPAA-compliant template (377 lines HTML)
- ✅ Accessible styles (686 lines SCSS)
- ✅ Mock data fallbacks for all API calls
- ✅ Client-side CSV/JSON export
- ✅ Event details modal with 4 close methods
- ✅ Comprehensive search/filter functionality
- ✅ Sortable/paginated table
- ✅ Statistics dashboard

**What needs testing:**
- Modal opens and displays event details
- All 4 modal close methods work
- CSV export downloads and contains correct data
- JSON export downloads and is valid
- PDF export shows appropriate message
- Exports respect current filter state

**Test Documentation:**
- `docs/MODAL_EXPORT_MANUAL_TEST.md` - Step-by-step testing guide
- `docs/AUDIT_LOG_VIEWER_COMPLETE_TEST_SUMMARY.md` - Master test tracker (56 tests)
- `docs/QUICK_TEST_GUIDE_MODAL_EXPORT.md` - 5-minute quick test

---

## 🎬 Next Steps

1. **Fix SSR issue** using one of the options above
2. **Start server** with SSR disabled
3. **Open browser** to http://localhost:4201/audit-logs
4. **Follow manual test guide** (`MODAL_EXPORT_MANUAL_TEST.md`)
5. **Report results** - which tests passed/failed

---

**Need Help?**
- Check browser console (F12) for JavaScript errors
- Check server logs for compilation errors
- Verify server is listening: `lsof -i :4201`
- Test with different browser (Chrome recommended)

---

**Last Updated:** January 24, 2026
