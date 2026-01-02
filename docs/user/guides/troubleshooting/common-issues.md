---
id: "user-troubleshooting-common"
title: "User Guide: Common Issues & Solutions"
portalType: "user"
path: "user/guides/troubleshooting/common-issues.md"
category: "user-guide"
subcategory: "troubleshooting"
tags: ["troubleshooting", "common-issues", "technical-support", "problem-solving"]
summary: "Solutions to the most common platform issues including login, data loading, performance, and sync problems."
estimatedReadTime: 8
difficulty: "beginner"
targetAudience: ["all-roles"]
prerequisites: ["platform-navigation"]
relatedGuides: ["getting-help", "performance-tips"]
lastUpdated: "2025-12-02"
---

# Common Issues & Solutions

This guide covers the most frequently reported issues and their solutions.

## Login and Access Issues

### "I Can't Log In"

**Problem**: Invalid credentials or account not working

**Solutions** (in order):
1. **Verify username/email**: Ensure you're using correct email
2. **Reset password**:
   - Click "Forgot Password"
   - Enter email
   - Follow reset link
   - Create new password
3. **Check MFA**: If MFA enabled, ensure receiving codes
4. **Try different browser**: Browser compatibility issue
5. **Clear browser cache**: Stored data causing conflict
6. **Contact IT**: If still not working

**Prevention**:
- Write down your email clearly
- Use strong password you'll remember
- Enable MFA on trusted device

### "I Lost Access Suddenly"

**Causes**: Account deactivated, permissions changed, session expired

**Solutions**:
1. **Log out completely**: Close browser entirely
2. **Log back in**: Fresh session may resolve
3. **Check account status**: Contact supervisor
4. **Request password reset**: IT can reset
5. **Verify MFA**: May need to re-register authenticator

### "MFA Code Not Arriving"

**Problem**: Can't log in because MFA code not received

**Solutions**:
1. **Check spam folder**: Code email in spam
2. **Request new code**: System can resend
3. **Switch MFA method**: Try SMS instead of email
4. **Contact IT**: May need to reset MFA

## Data Loading Issues

### "Patient Data Not Loading"

**Problem**: Patient detail page blank or spinning

**Causes**: Network issue, browser cache, system lag

**Solutions**:
1. **Refresh page**: Press Ctrl+R (or Cmd+R on Mac)
2. **Clear browser cache**:
   - Chrome: Settings > Privacy > Clear cache
   - Firefox: Settings > Privacy > Clear Data
   - Safari: Develop > Empty Caches
3. **Try different browser**: Safari, Chrome, Firefox
4. **Check internet**: Verify connection is working
5. **Wait a few minutes**: System may be under load
6. **Contact IT**: If page stays blank

**Prevention**:
- Avoid excessive tabs open
- Clear cache weekly
- Use modern browser (updated versions)

### "Dashboard Widgets Not Showing Data"

**Problem**: Dashboard loads but widgets empty

**Solutions**:
1. **Refresh page**: Ctrl+R
2. **Check date range filter**: May be set to future dates
3. **Verify filters**: Clear all filters
4. **Wait for data update**: May take 15-30 minutes
5. **Check different dashboard**: Verify other dashboards work

### "Appointments Not Showing in Calendar"

**Problem**: Calendar appears empty

**Solutions**:
1. **Check date range**: Calendar may be showing wrong month
2. **Verify patient selection**: Ensure patient selected
3. **Check filters**: May have hidden appointments
4. **Refresh page**: New appointments may not show
5. **Verify in patient detail**: Appointments visible elsewhere?

## Performance Issues

### "System Running Very Slowly"

**Problem**: Pages taking long to load, system feels sluggish

**Causes**: Browser cache, too many tabs, internet connection, system resources

**Solutions** (try in order):
1. **Close unnecessary tabs**: Browser running many tabs
2. **Clear browser cache**: Settings > Privacy > Clear cache
3. **Restart browser**: Fresh session
4. **Restart computer**: Free up system resources
5. **Check internet**: Use speed test site
6. **Try mobile hotspot**: Verify it's not your network
7. **Use different computer**: Verify it's not your device

**Prevention**:
- Keep only 5-10 browser tabs open
- Clear cache weekly
- Close browser when not using platform
- Restart computer daily if heavy user
- Use wired internet if possible (faster than WiFi)

### "Scrolling Jumpy or Laggy"

**Problem**: Page scrolling not smooth

**Solutions**:
1. **Clear browser cache**: Most common cause
2. **Disable browser extensions**: Extensions slow scrolling
3. **Use simpler theme**: Reduce visual complexity
4. **Check for browser updates**: Update browser
5. **Try different browser**: Verify issue not browser-specific

### "Buttons/Links Take Long to Respond"

**Problem**: Click takes several seconds to respond

**Solutions**:
1. **Wait a few seconds**: System processing request
2. **Don't double-click**: Double-clicking creates duplicate requests
3. **Check internet connection**: Speed test online
4. **Refresh page**: Clear pending requests
5. **Try again**: May be temporary

## Sync and Data Issues

### "Data Not Syncing from EHR"

**Problem**: EHR data not appearing in platform

**Causes**: Sync broken, network issue, data mapping problem

**Solutions**:
1. **Wait 30 minutes**: Sync may be scheduled
2. **Check sync status**: IT can verify sync running
3. **Verify data in EHR**: Confirm data exists in source
4. **Check date range**: Data may be older than search
5. **Contact IT**: If sync genuinely broken

**Timeline**: Typically syncs hourly or every 4 hours

### "Patient Appears Twice in System"

**Problem**: Duplicate patient records

**Solutions**:
1. **Report to IT**: Duplicates need manual merge
2. **Use primary record**: Work with existing record
3. **Flag duplicate**: Mark as duplicate in system
4. **Don't create new**: Avoid adding third record

**Prevention**:
- Search thoroughly before adding patient
- Check alternate spellings
- Check birth dates (same name, different person)

### "Lab Results Not Showing Up"

**Problem**: Ordered lab, but results not in system

**Solutions**:
1. **Check with lab**: Verify they received order
2. **Verify patient demographics**: Match between systems
3. **Wait for result**: Labs take time to process
4. **Check EHR**: Results may be in source system
5. **Contact lab directly**: If significantly delayed

**Timeline**: Most labs 24-48 hours, some longer

## Notification Issues

### "Not Receiving Alerts/Notifications"

**Problem**: Missing alerts about patient events

**Solutions**:
1. **Check settings**: Verify notifications turned on
2. **Check spam folder**: Alerts may go to spam
3. **Verify email correct**: Alert settings using right email
4. **Test alert**: Try creating test alert
5. **Check quiet hours**: Notifications disabled during quiet hours

**Prevention**:
- Review notification settings monthly
- Test alert receipt occasionally
- Add to contacts (avoid spam filter)

### "Receiving Too Many Notifications"

**Problem**: Overwhelmed with alerts

**Solutions**:
1. **Adjust threshold settings**: Reduce low-value alerts
2. **Turn off non-critical alerts**: Keep only important ones
3. **Set quiet hours**: Disable during non-work times
4. **Batch review**: Check once per shift instead of constantly
5. **Contact admin**: If alert settings need adjustment

## Permission and Access Issues

### "Can't Access Patient Records"

**Problem**: Getting "Access Denied" message

**Causes**: Patient not assigned to you, permissions missing

**Solutions**:
1. **Verify patient assigned to you**: Check with coordinator
2. **Verify your permissions**: Check user settings
3. **Request access**: Submit access request if needed
4. **Check spelling**: Right patient name?
5. **Contact supervisor**: If permissions issue

### "Can't Perform Certain Actions"

**Problem**: Button grayed out or "Not Authorized" message

**Causes**: Role doesn't have permission for that action

**Solutions**:
1. **Verify your role**: What role are you assigned?
2. **Understand role limits**: Some roles limited by design
3. **Request permission change**: If you need access
4. **Use different user**: Higher permission user if available
5. **Contact administrator**: If you need higher permissions

## Search and Filter Issues

### "Search Not Finding Patient"

**Problem**: Patient not showing up in search

**Solutions**:
1. **Check spelling**: Try alternate spellings
2. **Search by DOB**: If name spelling unclear
3. **Search by MRN**: If you know medical record number
4. **Verify assignment**: Patient assigned to you?
5. **Check filters**: Active filters hiding results

### "Filter Results Seem Wrong"

**Problem**: Filtered list showing unexpected results

**Solutions**:
1. **Clear all filters**: Reset to see all patients
2. **Add filters one at a time**: Identify which one causing issue
3. **Verify filter criteria**: Is that what you actually meant?
4. **Check date ranges**: Common source of confusion
5. **Refresh page**: Data may be out of sync

## Best Practices

### Preventing Issues
1. ✅ Keep browser updated
2. ✅ Clear cache weekly
3. ✅ Don't use too many tabs
4. ✅ Use strong password
5. ✅ Report issues quickly
6. ✅ Document error messages

### When Issues Occur
1. ✅ Try basic solutions first (refresh, clear cache)
2. ✅ Restart browser/computer
3. ✅ Try different browser
4. ✅ Note exact error message
5. ✅ Contact IT with details

## See Also

- [Getting Help & Support](./getting-help.md)
- [Performance Tips](../reference/performance-tips.md)
- [Troubleshooting Index](./index.md)

## Need Help?

**Support**: IT Help Desk, supervisor

---

**Last Updated**: December 2, 2025
**Document Version**: 1.0
