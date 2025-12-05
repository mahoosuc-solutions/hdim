---
id: "user-troubleshooting-integration"
title: "User Guide: Integration Issues"
portalType: "user"
path: "user/guides/troubleshooting/integration-issues.md"
category: "user-guide"
subcategory: "troubleshooting"
tags: ["troubleshooting", "integration", "ehr-sync", "data-sync", "technical-support"]
summary: "Solutions for EHR integration failures, data transmission errors, and synchronization problems."
estimatedReadTime: 6
difficulty: "intermediate"
targetAudience: ["administrators", "it-support", "clinical-staff"]
prerequisites: ["common-issues", "patient-data-issues"]
relatedGuides: ["patient-data-issues", "common-issues", "getting-help"]
lastUpdated: "2025-12-02"
---

# Integration Issues

This guide covers EHR integration and data synchronization problems.

## EHR Sync Not Working

### "Data Not Syncing from EHR"

**Problem**: EHR changes not appearing in platform

**Causes**: Sync broken, network issue, integration paused, mapping error

**Solutions**:
1. **Check sync status**: IT dashboard shows sync health
2. **Wait for sync cycle**: Syncs happen hourly or every 4 hours
3. **Manual sync trigger**: IT can force immediate sync
4. **Check EHR system**: Is EHR up and accessible?
5. **Verify data in EHR**: Does updated data exist there?
6. **Check network connectivity**: Between systems
7. **Contact IT**: If data should have synced

**Timeline**: Standard syncs every 1-4 hours

### "Sync Stopped Working Suddenly"

**Problem**: Sync was working, now stopped

**Causes**: Integration paused, credentials expired, network issue, system error

**Solutions**:
1. **Check IT dashboard**: Verify sync status
2. **Check logs**: IT can review sync logs
3. **Verify EHR access**: Can platform still connect?
4. **Check credentials**: API keys/passwords may have expired
5. **Check network**: Network connectivity between systems
6. **Restart integration**: IT may need to restart
7. **Contact IT immediately**: This is urgent

**Timeline**: Should resume within 15-30 minutes

## Data Transmission Errors

### "Sync Error: Connection Failed"

**Problem**: Platform can't connect to EHR

**Causes**: Network down, EHR offline, firewall blocking, credentials wrong

**Solutions**:
1. **Wait 5 minutes**: Temporary connectivity issue
2. **Check EHR status**: Is EHR system running?
3. **Check network**: Is internet connection stable?
4. **Verify credentials**: Has anything changed in EHR?
5. **Check firewall**: IT may need to adjust
6. **Contact IT**: Need to investigate connection
7. **Escalate to EHR team**: If EHR infrastructure issue

**Prevention**:
- Monitor sync status regularly
- Report issues immediately
- Keep IT informed of EHR changes

### "Sync Error: Data Format Issue"

**Problem**: Sync failing with data format error

**Causes**: Data in EHR doesn't match expected format, mapping issue

**Solutions**:
1. **Document error details**: Note exact error message
2. **Identify problem data**: Which patient/record causing issue?
3. **Verify in EHR**: Check data format there
4. **Contact IT**: Will need to adjust mapping
5. **Don't workaround**: Proper mapping needed

### "Sync Error: Timeout"

**Problem**: Sync starts but times out before completing

**Causes**: Large dataset, slow network, server overloaded

**Solutions**:
1. **Wait for next sync**: May complete in next cycle
2. **Check network**: Verify connection stability
3. **Check EHR load**: EHR may be busy
4. **Contact IT**: May need to adjust timeout settings
5. **Reduce batch size**: IT can sync smaller chunks

## Missing or Incomplete Synced Data

### "Some Fields Not Syncing"

**Problem**: Most data syncs but specific fields always missing

**Causes**: Field mapping not configured, field doesn't exist in EHR, format issue

**Solutions**:
1. **Identify which fields**: Document exactly what's missing
2. **Verify in EHR**: Do those fields exist in source?
3. **Check mapping**: IT verifies field mapping configured
4. **Contact IT**: May need to add field to mapping
5. **Workaround**: May need to enter data manually

**Timeline**: Mapping fixes take 1-2 days

### "Patient Data Syncing But Encounters Not"

**Problem**: Patient demographics sync but visit/encounter data missing

**Causes**: Different sync schedule, encounters not mapped, permissions issue

**Solutions**:
1. **Check sync status**: Are encounter syncs running?
2. **Verify mapping**: Are encounters mapped to platform?
3. **Check encounter status**: Encounters may need finalization
4. **Wait for sync**: May need 4+ hours for encounters
5. **Contact IT**: If encounters should be syncing

### "Lab Results Not Syncing"

**Problem**: Labs ordered in EHR but results not appearing

**Causes**: Lab system not integrated, result not marked final, mapping issue

**Solutions**:
1. **Verify in EHR**: Check if result there
2. **Check result status**: May be preliminary
3. **Wait for results**: May not be completed yet
4. **Contact lab**: Verify result sent to EHR
5. **Contact IT**: If should be syncing but isn't

## Integration Configuration Issues

### "Wrong Data Syncing"

**Problem**: Wrong facility/patient group data syncing

**Causes**: Integration misconfigured, wrong credentials

**Solutions**:
1. **Stop sync immediately**: Prevent data corruption
2. **Document configuration**: What's wrong?
3. **Contact IT immediately**: Critical issue
4. **Verify EHR connection**: Check which system connected
5. **Reconfigure integration**: May need restart

### "Duplicate Data After Sync"

**Problem**: Data appears twice after integration runs

**Causes**: Mapping error, sync ran twice, de-duplication failed

**Solutions**:
1. **Don't attempt manual cleanup**: Let IT handle
2. **Contact IT**: Report immediately
3. **Document duplicates**: Which records/data duplicated?
4. **Prevent manual edits**: May need to merge
5. **IT will merge**: They have proper tools

## Best Practices

### Monitoring Sync
- ✅ Check sync status regularly (daily)
- ✅ Monitor for error notifications
- ✅ Report issues immediately
- ✅ Don't manually correct sync issues
- ✅ Escalate to IT for investigation

### Integration Stability
- ✅ Keep network stable
- ✅ Maintain EHR system properly
- ✅ Update credentials promptly
- ✅ Communicate system changes to IT
- ✅ Avoid manual workarounds

### Data Quality
- ✅ Ensure clean EHR data
- ✅ Fix data issues at source (EHR)
- ✅ Verify sync completeness
- ✅ Monitor for inconsistencies
- ✅ Report discrepancies promptly

## See Also

- [Patient Data Issues](./patient-data-issues.md)
- [Common Issues & Solutions](./common-issues.md)
- [Data Quality & Validation](../admin/data-quality.md)
- [Getting Help & Support](../reference/getting-help.md)

## Need Help?

**For Sync Issues**: Contact IT Help Desk immediately
**For EHR Issues**: Contact EHR administration
**For Urgent Issues**: Escalate to IT director

---

**Last Updated**: December 2, 2025
**Document Version**: 1.0
