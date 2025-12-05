---
id: "user-troubleshooting-reports"
title: "User Guide: Report Generation Issues"
portalType: "user"
path: "user/guides/troubleshooting/report-generation-issues.md"
category: "user-guide"
subcategory: "troubleshooting"
tags: ["troubleshooting", "reports", "data-export", "technical-support"]
summary: "Solutions for report generation failures, missing data, export problems, and scheduling issues."
estimatedReadTime: 7
difficulty: "intermediate"
targetAudience: ["physicians", "administrators", "care-managers"]
prerequisites: ["common-issues", "generating-reports"]
relatedGuides: ["generating-reports", "common-issues", "dashboard-analytics"]
lastUpdated: "2025-12-02"
---

# Report Generation Issues

This guide covers common report problems and solutions.

## Report Generation Failures

### "Report Generation Failed"

**Problem**: "Error generating report" message

**Causes**: Query error, data issue, timeout, server error

**Solutions** (in order):
1. **Try again**: May be temporary server issue
2. **Reduce date range**: Smaller reports may work
3. **Try predefined report**: Custom settings may be issue
4. **Check filters**: Remove complex filters
5. **Contact IT**: If consistently failing
6. **Check browser**: Try different browser
7. **Wait and retry**: Server may be temporarily busy

**Prevention**:
- Start with narrow date ranges
- Use standard filters
- Avoid extremely large datasets
- Generate during off-peak hours

### "Report Times Out"

**Problem**: Report generation starts but never completes

**Causes**: Large dataset, complex query, network timeout, server overload

**Solutions**:
1. **Stop generation**: Don't wait indefinitely
2. **Reduce date range**: Smaller datasets complete faster
3. **Simplify filters**: Remove complex criteria
4. **Exclude certain metrics**: May speed up generation
5. **Generate multiple smaller reports**: Break into chunks
6. **Try different time**: Server may be less busy
7. **Contact IT**: If consistently timing out

**Timeline**: Standard reports should complete in 5-30 minutes

**Prevention**:
- Use smaller date ranges for large populations
- Schedule large reports during off-hours
- Exclude unnecessary metrics

## Missing Data in Reports

### "Report Shows No Data"

**Problem**: Report generates but empty or very few records

**Causes**: Filters too restrictive, wrong date range, data not loaded, query error

**Solutions**:
1. **Check date range**: Are dates correct?
2. **Verify filters applied**: Remove filters one by one
3. **Check baseline population**: Does population have data?
4. **Refresh data**: System may not have latest data
5. **Verify permissions**: Can you see this data?
6. **Try simpler report**: Verify base data exists
7. **Contact IT**: If data should exist

**Prevention**:
- Start with broad filters
- Add filters one at a time
- Test baseline report first

### "Report Missing Specific Records"

**Problem**: You know records exist but they're not in report

**Solutions**:
1. **Check filters**: Specific filter excluding them?
2. **Check status field**: May be excluding inactive records
3. **Check date range**: Records may fall outside range
4. **Verify record criteria**: Do they meet selection criteria?
5. **Check permissions**: Can you access these records?
6. **Export raw data**: Compare with source system
7. **Contact IT**: If records definitely should be included

### "Calculated Fields Show Wrong Numbers"

**Problem**: Sum, average, or other calculations incorrect

**Causes**: Data quality issue, calculation error, filter issue

**Solutions**:
1. **Verify source data**: Check raw data in system
2. **Check calculation settings**: Is calculation correct?
3. **Verify filters**: May be excluding some data
4. **Check date range**: May exclude relevant records
5. **Compare with EHR**: Verify against source
6. **Review calculation formula**: Is it what you expect?
7. **Contact IT**: If calculation appears wrong

## Export and Download Issues

### "Can't Export Report"

**Problem**: Export button not working or grayed out

**Causes**: Permission issue, file format not supported, report generation error

**Solutions**:
1. **Verify permissions**: Do you have export access?
2. **Try different format**: Excel, CSV, or PDF
3. **Check report generation**: Did report finish first?
4. **Refresh page**: Clear cached state
5. **Try different browser**: Browser-specific issue?
6. **Clear browser cache**: Stored data causing conflict
7. **Contact IT**: If still not working

### "Export File Corrupted"

**Problem**: Downloaded file won't open or is corrupted

**Solutions**:
1. **Try downloading again**: File may be incomplete
2. **Try different format**: Regenerate in Excel instead of CSV
3. **Check file size**: Should be reasonable size
4. **Use different program**: Try different application
5. **Contact IT**: If file definitely corrupted

**Prevention**:
- Verify download completed
- Check file size
- Test file opens before relying on it

### "Export Takes Very Long"

**Problem**: Export starting but taking excessive time

**Solutions**:
1. **Wait up to 10 minutes**: Large exports are slow
2. **Don't close browser**: Export will stop
3. **Check file size**: Very large exports slow
4. **Export smaller subset**: Break into multiple exports
5. **Try different format**: Some formats faster than others
6. **Contact IT**: If significantly delayed

**Timeline**: Exports typically 30 seconds - 5 minutes

## Scheduled Report Issues

### "Scheduled Report Not Delivered"

**Problem**: Email with report didn't arrive

**Causes**: Schedule not set correctly, email address wrong, email filtered

**Solutions**:
1. **Check email spam folder**: May be filtered
2. **Verify email address**: Is it correct?
3. **Verify schedule**: Is report actually scheduled?
4. **Check run time**: Did scheduled time already pass?
5. **Manually generate**: Run report manually to test
6. **Verify report content**: Does report generate successfully?
7. **Contact IT**: If email should have arrived

### "Report Scheduled But Not Running"

**Problem**: Schedule configured but report not generating

**Causes**: Schedule not saved, time not reached, report error, schedule disabled

**Solutions**:
1. **Verify schedule saved**: Check schedule settings
2. **Check current time**: Has scheduled time passed?
3. **Verify frequency**: Daily, weekly, monthly?
4. **Check report settings**: Can you manually run it?
5. **Check logs**: IT can see if schedule ran
6. **Recreate schedule**: Delete and re-add
7. **Contact IT**: If schedule configured but not running

### "Receiving Too Many Report Emails"

**Problem**: Getting duplicate or excessive scheduled reports

**Solutions**:
1. **Check schedule frequency**: How often is it set?
2. **Cancel duplicates**: Remove extra schedules
3. **Check recipients**: Are you listed multiple times?
4. **Verify one schedule**: Only one should be active
5. **Contact IT**: If receiving extras from system error

## Report Customization Issues

### "Custom Report Parameters Not Working"

**Problem**: Changing report parameters doesn't change results

**Solutions**:
1. **Verify parameter saved**: Did it apply?
2. **Refresh report**: Generate with new parameters
3. **Reset to defaults**: Try recreating parameter
4. **Check syntax**: Parameter format correct?
5. **Try simpler parameter**: Test basic parameter
6. **Contact IT**: If parameter not working

### "Can't Save Custom Report"

**Problem**: Save button not working

**Solutions**:
1. **Verify permissions**: Can you save reports?
2. **Try different name**: Special characters in name?
3. **Check existing**: Report with this name already?
4. **Simplify settings**: Too complex configuration?
5. **Refresh page**: Clear cached state
6. **Try in different browser**: Browser issue?
7. **Contact IT**: If can't save

### "Saved Report Configuration Lost"

**Problem**: Custom report settings reset or disappeared

**Causes**: Session timeout, report deleted, settings corrupted

**Solutions**:
1. **Check if report exists**: Still in your saved reports?
2. **Look in recycle bin**: May be temporarily deleted
3. **Contact IT**: Can recover deleted reports
4. **Recreate settings**: Save again with same name
5. **Check permissions**: Did access change?

## Performance Issues with Reports

### "Report Page Very Slow"

**Problem**: Report interface lags or freezes

**Solutions**:
1. **Close other tabs**: Free up browser memory
2. **Refresh page**: Clear cached data
3. **Clear browser cache**: Full cache clear
4. **Try smaller report**: Reduce complexity
5. **Try different browser**: Browser-specific issue
6. **Restart browser**: Fresh session
7. **Contact IT**: If consistently slow

### "Can't Interact with Report"

**Problem**: Clicking buttons or filters not responding

**Solutions**:
1. **Wait a moment**: May be processing
2. **Refresh page**: Clear state
3. **Try single action**: Do one thing at a time
4. **Reduce complexity**: Use fewer filters
5. **Close other tabs**: Free resources
6. **Try different browser**: Browser issue?
7. **Contact IT**: If unresponsive

## Best Practices

### Report Generation
- ✅ Start with narrow date ranges
- ✅ Test filters before running full report
- ✅ Generate large reports during off-hours
- ✅ Verify data before relying on report
- ✅ Compare with source data if numbers questionable

### Export Best Practices
- ✅ Verify download completed
- ✅ Test file opens successfully
- ✅ Check row counts match
- ✅ Don't share reports with protected health info
- ✅ Secure exported data appropriately

### Scheduled Reports
- ✅ Verify schedule actually configured
- ✅ Check spam folder first time
- ✅ Test manually first
- ✅ Monitor delivery
- ✅ Update recipients when staff changes

## See Also

- [Generating Reports](../features/reports/generating-reports.md)
- [Dashboard Analytics](../features/reports/dashboard-analytics.md)
- [Common Issues & Solutions](./common-issues.md)
- [Getting Help & Support](../reference/getting-help.md)

## Need Help?

**For Report Issues**: Contact IT Help Desk
**For Data Questions**: Contact analytics team
**For Urgent Issues**: Escalate to IT supervisor

---

**Last Updated**: December 2, 2025
**Document Version**: 1.0
