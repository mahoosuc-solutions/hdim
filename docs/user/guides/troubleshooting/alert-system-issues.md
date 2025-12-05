---
id: "user-troubleshooting-alerts"
title: "User Guide: Alert System Issues"
portalType: "user"
path: "user/guides/troubleshooting/alert-system-issues.md"
category: "user-guide"
subcategory: "troubleshooting"
tags: ["troubleshooting", "alerts", "notifications", "clinical-alerts", "technical-support"]
summary: "Solutions for missed alerts, false positives, alert fatigue, and notification delivery problems."
estimatedReadTime: 5
difficulty: "beginner"
targetAudience: ["physicians", "care-managers", "clinical-staff"]
prerequisites: ["common-issues", "alerts"]
relatedGuides: ["common-issues", "alerts", "getting-help"]
lastUpdated: "2025-12-02"
---

# Alert System Issues

This guide covers common alert and notification problems.

## Missing Alerts

### "Not Receiving Alerts"

**Problem**: Expected alert didn't arrive

**Causes**: Alert settings disabled, email filtered, threshold not met

**Solutions**:
1. **Check alert settings**: Are alerts turned on?
2. **Check spam folder**: Alert may be filtered
3. **Verify email address**: Correct email in settings?
4. **Verify alert criteria**: Does situation actually meet criteria?
5. **Check quiet hours**: Alerts may be disabled during certain hours
6. **Test alert**: Create test alert to verify system works
7. **Contact IT**: If settings correct but still not receiving

**Prevention**:
- Review alert settings monthly
- Test alert receipt occasionally
- Add email to contacts to avoid spam
- Verify quiet hours settings

### "Critical Alert Missed"

**Problem**: Critical patient alert not delivered

**URGENT**: Verify patient safety directly

**Solutions**:
1. **Contact patient directly**: Don't rely on alert
2. **Review EHR**: Check for urgent issues
3. **Alert supervisor**: Patient may need immediate attention
4. **Document issue**: Note that alert failed
5. **Contact IT immediately**: This is critical
6. **Verify notification settings**: Check alert configuration

**Prevention**:
- Don't rely solely on alerts
- Check system regularly
- Verify alert settings working
- Have backup notification method

## Alert Fatigue and False Positives

### "Receiving Too Many Alerts"

**Problem**: Overwhelmed with notifications

**Solutions**:
1. **Adjust thresholds**: Reduce low-value alerts
2. **Disable non-critical**: Keep only important ones
3. **Set quiet hours**: Disable during non-work times
4. **Group by patient**: Some systems can batch by patient
5. **Use smart filters**: Alert only for certain conditions
6. **Contact supervisor**: May need organization-level adjustment
7. **Review best practices**: May be receiving expected volume

### "Receiving False Alert"

**Problem**: Alert triggered but situation doesn't apply

**Causes**: Threshold set too low, data quality issue, incorrect mapping

**Solutions**:
1. **Document false positive**: What triggered alert?
2. **Check patient data**: Is data accurate?
3. **Review alert settings**: Is threshold appropriate?
4. **Contact IT**: May need to adjust alert logic
5. **Report pattern**: If multiple false alerts

**Prevention**:
- Set thresholds appropriately
- Verify data quality
- Monitor alert accuracy
- Report false positives promptly

### "Alert for Resolved Issue"

**Problem**: Alert about situation that's already been addressed

**Causes**: Alert delay, data sync lag, alert not cleared

**Solutions**:
1. **Note resolution time**: When was it actually fixed?
2. **Check sync timing**: May be showing old data
3. **Review alert logic**: Should it have been cleared?
4. **Contact IT**: If alert should have cleared automatically
5. **Manually clear**: May have option to dismiss

## Alert Configuration Issues

### "Can't Change Alert Settings"

**Problem**: Alert settings are locked or grayed out

**Causes**: Permission issue, configuration locked, settings not editable

**Solutions**:
1. **Check permissions**: Do you have permission to adjust?
2. **Contact supervisor**: May need admin to change
3. **Check alert type**: Some alerts may not be adjustable
4. **Verify your role**: Role determines alert access
5. **Contact IT**: If should have access

### "Alert Settings Not Saving"

**Problem**: Change alert settings but they don't stick

**Solutions**:
1. **Verify save completed**: Did you see confirmation?
2. **Try again**: Refresh page first
3. **Try different browser**: Browser issue?
4. **Clear cache**: Cached settings may be old
5. **Contact IT**: If consistently not saving

## Alert Delivery Issues

### "Alerts Not Arriving via Email"

**Problem**: Alerts configured for email but not receiving

**Solutions**:
1. **Check spam folder**: Most common location
2. **Verify email address**: Correct in settings?
3. **Add to contacts**: Prevent spam filtering
4. **Test delivery**: Send test alert
5. **Check email service**: Email system working?
6. **Contact IT**: If other emails working fine

### "Alerts Late or Delayed"

**Problem**: Alerts arriving much later than event occurred

**Causes**: System backlog, email delay, network issue

**Solutions**:
1. **Check system status**: Are alerts processing normally?
2. **Wait**: May catch up automatically
3. **Contact IT**: If consistently delayed
4. **Consider alternative**: Phone call for critical alerts

## Best Practices

### Alert Management
- ✅ Review settings regularly
- ✅ Set appropriate thresholds
- ✅ Don't ignore alerts
- ✅ Respond promptly to critical alerts
- ✅ Document false positives

### Alert Safety
- ✅ Don't rely solely on alerts
- ✅ Check system regularly
- ✅ Verify alerts working
- ✅ Have backup notification method
- ✅ Test alerts periodically

### Reducing Alert Fatigue
- ✅ Adjust thresholds appropriately
- ✅ Focus on actionable alerts
- ✅ Use quiet hours
- ✅ Group related alerts
- ✅ Escalate patterns to IT

## See Also

- [Alerts & Notifications](../features/core/alerts.md)
- [Common Issues & Solutions](./common-issues.md)
- [Clinical Alerts System](../workflows/clinical/clinical-alerts.md)
- [Getting Help & Support](../reference/getting-help.md)

## Need Help?

**For Alert Issues**: Contact IT Help Desk
**For Clinical Questions**: Contact clinical supervisor
**For Urgent Issues**: Escalate immediately

---

**Last Updated**: December 2, 2025
**Document Version**: 1.0
