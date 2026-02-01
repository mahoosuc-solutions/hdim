# Measure Builder - Troubleshooting Guide

**Version:** 1.0
**Last Updated:** January 18, 2026
**Level:** All Users
**Contact:** support@healthdatainmotion.com

---

## Quick Reference

| Issue | Symptom | Quick Fix |
|-------|---------|-----------|
| Slow Performance | Canvas takes 5+ seconds | Clear browser cache, refresh |
| Save Fails | Spinning wheel, no success | Check internet, retry save |
| CQL Won't Generate | Error message appears | Validate all blocks connected |
| Slider Stuck | Won't move, unresponsive | Try different browser |
| Validation Errors | Red X marks on blocks | Review error messages, fix issues |

---

## Common Issues & Solutions

### Issue 1: Performance - Slow Canvas Rendering

**Symptoms:**
- Canvas takes 5+ seconds to load
- Dragging blocks is slow/laggy
- Frequent freezing when adding blocks

**Root Causes:**
- 150+ algorithm blocks (normal rendering limit)
- Browser running out of memory
- Low-end device or laptop
- Too many browser tabs/applications open

**Solutions:**

✅ **Solution A: Browser Cache** (Fixes 60% of cases)
```
1. Clear browser cache:
   - Chrome: Ctrl+Shift+Delete → Clear all time
   - Firefox: Ctrl+Shift+Delete → Everything
   - Safari: Develop → Empty Caches

2. Refresh page: Ctrl+F5 (hard refresh)
3. Retry measure
```

✅ **Solution B: System Resources** (Fixes 30% of cases)
```
1. Close unnecessary applications
2. Close extra browser tabs
3. Restart your browser
4. Restart your computer if still slow
```

✅ **Solution C: Switch to Canvas Mode** (For 150+ blocks)
```
The system automatically switches to Canvas rendering
for 150+ blocks (2-3x faster performance).

This happens automatically - no action needed.

If still slow with Canvas, try Solution A & B.
```

### Issue 2: Save Function Fails

**Symptoms:**
- "Saving..." message spins forever
- No success message after 30 seconds
- Browser console shows network errors

**Root Causes:**
- Internet connection dropped
- Server temporarily unavailable
- Browser tab lost focus
- Session expired

**Solutions:**

✅ **Solution A: Connection Check**
```
1. Verify internet connection:
   - Open new tab, visit google.com
   - Should load in <3 seconds

2. If no internet:
   - Connect to WiFi/LAN
   - Try again

3. If internet works:
   - Retry save (Ctrl+S)
```

✅ **Solution B: Retry Save**
```
1. Wait 10 seconds
2. Click Save button again (or Ctrl+S)
3. If still fails, take a screenshot of error
4. Contact support with screenshot
```

✅ **Solution C: Session Expired**
```
1. If you see "Session expired" message:
   - Refresh page (F5)
   - Log in again
   - Your draft is saved, resume editing
```

### Issue 3: CQL Generation Fails

**Symptoms:**
- "Cannot Generate CQL" error message
- CQL viewer shows red error text
- Cannot publish measure

**Root Causes:**
- Blocks not properly connected
- Required block fields are empty
- Invalid block configuration
- Population is empty

**Solutions:**

✅ **Solution A: Check Block Connections**
```
1. Review your algorithm on canvas:
   - Each block should have incoming/outgoing connections
   - No dead-end blocks
   - No circular loops

2. Red X on blocks = configuration error
   - Click red X for error details
   - Fix highlighted field

3. Retry CQL generation
```

✅ **Solution B: Validate Population**
```
1. Click "Population" section at top
2. Ensure at least one Condition block exists
3. Condition should not be empty
4. Try generating CQL again
```

✅ **Solution C: Full Validation**
```
1. Click "Validate" button
2. Fix all issues shown (wait for green checkmarks)
3. Click "Generate CQL"
4. Should work now
```

### Issue 4: Slider Won't Move or Respond

**Symptoms:**
- Slider appears but won't drag
- Click on slider does nothing
- Browser console shows JavaScript errors

**Root Causes:**
- Browser zoom level not 100%
- Browser compatibility issue
- JavaScript disabled
- Browser plugin conflict

**Solutions:**

✅ **Solution A: Browser Zoom**
```
1. Check zoom level (usually shown as 100% in address bar)
2. Reset to 100%:
   - Chrome/Edge: Ctrl+0
   - Firefox: Ctrl+0
   - Safari: Cmd+0

3. Refresh page (Ctrl+F5)
4. Try slider again
```

✅ **Solution B: Different Browser**
```
If not working after zoom reset:

1. Try different browser:
   - Chrome (recommended)
   - Firefox
   - Safari
   - Edge

2. If works in another browser:
   - Original browser has compatibility issue
   - Try clearing cache in that browser
   - Or update browser to latest version
```

✅ **Solution C: JavaScript Check**
```
1. Ensure JavaScript is enabled:
   - Chrome: Settings → Privacy → JavaScript (check Allow)
   - Firefox: about:config → javascript.enabled = true
   - Safari: Develop → Disable JavaScript (uncheck)

2. Refresh page
3. Try slider again
```

### Issue 5: Validation Error - "Circular Reference Detected"

**Symptoms:**
- Validation fails with "Circular reference" error
- Cannot publish measure
- Error doesn't show which blocks are problematic

**Root Causes:**
- Block connects back to itself (intentional or accidental)
- Feedback loop in logic
- Invalid block ordering

**Solutions:**

✅ **Solution A: Visual Inspection**
```
1. Look at your canvas:
   - Blocks should flow left to right
   - No lines looping backwards
   - No block connecting to itself

2. If you find a loop:
   - Delete the problematic connection
   - Restructure logic to flow forward
```

✅ **Solution B: Restructure Logic**
```
Instead of: A→B→A (circular)

Use proper flow:
   [Condition A]
        ↓
   [Procedure B]
        ↓
   [Observation C]
        ↓
   [Result]
```

### Issue 6: Measure Won't Publish

**Symptoms:**
- Publish button greyed out or missing
- "Cannot publish" error appears
- System blocks publish action

**Root Causes:**
- Validation hasn't passed
- Required fields are empty
- Complexity rating too high
- Insufficient permissions

**Solutions:**

✅ **Solution A: Run Validation First**
```
1. Click "Validate" button
2. Wait for all checks to complete
3. Should see green checkmarks on all checks
4. Then try Publish
```

✅ **Solution B: Complete All Fields**
```
1. Measure name: Must not be empty
2. Category: Must select one
3. Description: Should have meaningful text
4. Blocks: At least one valid block
5. Retry publish
```

✅ **Solution C: Check Permissions**
```
If publish button is greyed out:

1. Your role may not have permission
2. Contact your administrator
3. They can grant EVALUATOR or higher role
4. Or manually publish on your behalf
```

### Issue 7: Browser Shows Blank Canvas

**Symptoms:**
- Measure opens but canvas area is blank
- No blocks visible
- Browser console has JavaScript errors

**Root Causes:**
- Browser not supported
- JavaScript error occurred
- Measure data corrupted
- SVG rendering disabled

**Solutions:**

✅ **Solution A: Supported Browser**
```
Use one of these supported browsers:
✅ Chrome 90+
✅ Firefox 88+
✅ Safari 14+
✅ Edge 90+

If using older browser:
- Update to latest version
- Clear cache after updating
- Retry measure
```

✅ **Solution B: Browser Console Check**
```
1. Open browser console (F12)
2. Look for red error messages
3. If you see errors:
   - Screenshot the errors
   - Email to support@healthdatainmotion.com
   - Include your browser version
```

✅ **Solution C: Refresh & Retry**
```
1. Hard refresh: Ctrl+Shift+F5
2. Close and reopen browser
3. If still blank:
   - Try different browser
   - Clear all cache for this website
```

### Issue 8: Measure Takes Too Long to Load

**Symptoms:**
- Loading spinner shows for 1+ minute
- "Measure loading..." message persists
- Browser appears frozen

**Root Causes:**
- Large measure with 200+ blocks
- Slow internet connection
- Server overloaded
- Browser processing power insufficient

**Solutions:**

✅ **Solution A: Wait Longer**
```
Large measures (150+ blocks) can take 30-60 seconds.
- Do NOT refresh during load
- Do NOT close tab
- Wait up to 2 minutes
- If still loading after 2 min, go to Solution B
```

✅ **Solution B: Network Check**
```
1. Check internet speed:
   - speedtest.net
   - Should be 10+ Mbps

2. If slower:
   - Move closer to router
   - Connect to WiFi instead of cellular
   - Restart router
   - Retry loading measure
```

✅ **Solution C: Try Later**
```
If server is overloaded:
- Try again in 15 minutes
- Or contact support
- Could be temporary maintenance
```

---

## Advanced Troubleshooting

### Enable Debug Mode

```
1. Open browser console (F12)
2. Paste this code:
   localStorage.setItem('DEBUG_MEASURE_BUILDER', 'true');

3. Refresh page
4. Watch console for detailed logs
5. Helpful when reporting bugs
```

### Export Measure for Support

```
1. In measure editor:
   - Click Help menu
   - Select "Export Measure"

2. System creates JSON file
3. Send file to support@healthdatainmotion.com
4. Support can analyze and help
```

### Check System Status

```
Visit: https://status.healthdatainmotion.com

Shows:
- System health status
- Known issues
- Planned maintenance
- Service status
```

---

## Performance Optimization Tips

### 1. Block Count Strategy

```
✅ Recommended:
- Simple measures: 5-20 blocks
- Moderate measures: 20-75 blocks
- Complex measures: 75-150 blocks
- Very complex: 150+ blocks (uses Canvas)

⏱️ Expected load times:
- < 50 blocks: < 2 seconds
- 50-150 blocks: 3-10 seconds
- 150-300 blocks: 15-30 seconds
- 300+ blocks: 30-60 seconds+
```

### 2. Browser Optimization

```
✅ For best performance:
1. Close unnecessary tabs (each tab uses RAM)
2. Use ad blocker (reduces distractions)
3. Disable browser extensions (except essentials)
4. Restart browser every 4 hours
5. Keep browser updated
```

### 3. System Optimization

```
✅ On your computer:
1. Close unnecessary applications
2. Restart computer weekly
3. Ensure 2+ GB free RAM
4. Use SSD (not spinning disk)
5. Keep OS updated
```

---

## When to Contact Support

**Contact support if:**

- Issue persists after trying all solutions above
- You see unexpected error messages
- Performance is severely degraded
- Data appears to be lost
- Measure won't save after multiple attempts

**How to contact:**

📧 **Email:** support@healthdatainmotion.com
💬 **Chat:** Click chat icon in bottom-right
📞 **Phone:** 1-800-HDI-HELP (1-800-434-4357)
🕐 **Hours:** 8am-6pm ET, Monday-Friday

**When contacting support, include:**

1. Description of the issue
2. Steps to reproduce
3. Browser name and version (Help → About)
4. Screenshots of errors
5. Measure export (if applicable)
6. System information (OS, RAM available)

---

## FAQ

**Q: Will my measure be lost if I close the tab?**
A: No! Measure is auto-saved every 30 seconds. Your work is safe.

**Q: Can I work on the same measure from multiple tabs?**
A: Not recommended. Last save wins. Use only one tab per measure.

**Q: How do I recover a deleted measure?**
A: Contact administrator. Deleted measures can be restored from backups for 30 days.

**Q: Why is my measure read-only?**
A: Published measures are read-only. Create a new version to edit (click "New Version").

**Q: Can I share measures with colleagues?**
A: Yes! Click Share button to grant access to specific people or teams.

**Q: What's the maximum complexity allowed?**
A: Usually 10 (high complexity). Ask administrator if you need higher.

**Q: Can I export my measure as CQL code?**
A: Yes! Click "View CQL" then "Copy" to copy the generated code.

---

## Related Documents

- **Getting Started:** `docs/user/guides/MEASURE_BUILDER_GETTING_STARTED.md`
- **Administration:** `docs/admin/MEASURE_BUILDER_ADMINISTRATION.md`
- **API Guide:** `backend/MEASURE_BUILDER_API.md`

---

**Status:** ✅ Complete
**Last Updated:** January 18, 2026
**Next Update:** After user feedback
