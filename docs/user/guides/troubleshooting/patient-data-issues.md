---
id: "user-troubleshooting-patient-data"
title: "User Guide: Patient Data Issues"
portalType: "user"
path: "user/guides/troubleshooting/patient-data-issues.md"
category: "user-guide"
subcategory: "troubleshooting"
tags: ["troubleshooting", "patient-data", "data-quality", "synchronization", "technical-support"]
summary: "Solutions for common patient data problems including missing records, duplicates, incorrect data, and synchronization delays."
estimatedReadTime: 8
difficulty: "intermediate"
targetAudience: ["physicians", "care-managers", "clinical-staff", "administrators"]
prerequisites: ["common-issues", "platform-navigation"]
relatedGuides: ["common-issues", "data-quality", "patient-panel"]
lastUpdated: "2025-12-02"
---

# Patient Data Issues

This guide covers common patient data problems and their solutions.

## Missing Patient Records

### "Patient Not Showing in System"

**Problem**: Cannot find patient in platform

**Possible Causes**: Patient not loaded yet, wrong search method, patient archived, permissions issue

**Solutions** (in order):
1. **Verify patient exists in EHR**: Check if patient in source system
2. **Try different search method**:
   - Search by MRN instead of name
   - Search by DOB if uncertain of spelling
   - Search by phone number
3. **Check for alternate spellings**: Try phonetic variations
4. **Check patient status**: Patient may be archived or inactive
5. **Verify permissions**: Do you have access to this patient?
6. **Contact supervisor**: If patient should be assigned to you
7. **Contact IT**: If patient definitely should exist

**Timeline**: New patients typically load within 1-4 hours of EHR entry

**Prevention**:
- Verify patient info in EHR before expecting in platform
- Use MRN for certain match
- Create new patient record only after thorough search

### "Patient Was Here Yesterday, Now Gone"

**Problem**: Patient previously visible now disappeared

**Possible Causes**: Patient record archived, duplicate merged, permissions changed, soft delete

**Solutions**:
1. **Check recent searches**: Patient may still be accessible via history
2. **Search by MRN**: May help locate if name changed
3. **Verify permissions**: Check if access changed
4. **Ask supervisor**: May have been reassigned
5. **Check archive status**: IT can verify if archived
6. **Search deleted records**: IT may recover if needed

**Prevention**:
- Note patient MRN for future reference
- Document patient assignment changes
- Alert supervisor before removing patients

## Duplicate Records

### "Patient Appears Multiple Times"

**Problem**: Same patient has 2+ records in system

**Causes**: Data sync error, duplicate entry, migration issue

**Solutions**:
1. **Identify primary record**: Use record with most complete data
2. **Note MRN of each record**: For IT reference
3. **Use primary record for care**: Don't split work between records
4. **Report to IT**: They can merge duplicates
5. **Don't create new record**: Avoid creating third record
6. **Flag as duplicate**: System may have flag option

**Timeline**: Merges typically take 24-48 hours

**Prevention**:
- Search thoroughly before adding new patient
- Check multiple name spellings
- Check alternate birth dates (different people, same name)
- Use MRN if available

**During merge**: Data will be combined, visits consolidated, orders unified

### "Different Demographics in Duplicate Records"

**Problem**: Two patient records with conflicting information (different DOB, address, etc.)

**Solutions**:
1. **Verify in EHR**: Which information is correct in source?
2. **Note discrepancies**: Document specific conflicts
3. **Contact records team**: They can clarify
4. **Report to IT**: Include both MRNs and discrepancies
5. **Use more complete record**: Work in record with better data

## Missing or Incorrect Patient Data

### "Patient Demographics Wrong (Name, DOB, Address)"

**Problem**: Patient information inaccurate in platform

**Causes**: Entry error, sync issue, old data, multiple records

**Solutions**:
1. **Verify in EHR**: Is it correct in source system?
2. **If wrong in EHR**: Update EHR first (platform reads from source)
3. **If correct in EHR**: Wait for sync (usually 1-4 hours)
4. **Refresh page**: Clear cached data
5. **Contact IT**: If mismatch persists between systems
6. **Don't manually edit**: Platform updates from EHR

**Prevention**:
- Review demographics at patient intake
- Correct EHR data promptly
- Verify phone/email before outreach

### "Insurance Information Missing"

**Problem**: Insurance fields blank or incomplete

**Causes**: Not loaded yet, data sync delay, insurance not on file in EHR

**Solutions**:
1. **Verify in EHR**: Check if insurance recorded there
2. **If in EHR**: Wait for sync (4-24 hours)
3. **If not in EHR**: Add insurance to EHR first
4. **Check coverage dates**: May have expired
5. **Refresh patient record**: Clear old cache
6. **Contact records team**: If data should exist

**Timeline**: Insurance updates sync within 4-24 hours

**Prevention**:
- Verify insurance at each visit
- Update EHR immediately
- Flag expired coverage

### "Patient Address or Contact Info Wrong"

**Problem**: Address, phone, or email incorrect

**Solutions**:
1. **Verify correct information**: Do you have right number?
2. **Update in EHR**: That's where it syncs from
3. **Refresh in platform**: May take 1-4 hours to appear
4. **Contact patient**: Confirm correct information
5. **Flag for follow-up**: Contact team can update

**Prevention**:
- Confirm patient contact at each visit
- Update EHR immediately
- Document any changes in visit notes

## Medical History Issues

### "Medications Not Showing"

**Problem**: Medication list missing or incomplete

**Causes**: Not loaded yet, sync delay, med not recorded in EHR, old meds

**Solutions**:
1. **Check medication dates**: Current meds only shown?
2. **Scroll in list**: May be more below visible
3. **Verify in EHR**: Is medication recorded there?
4. **Check medication status**: Stopped/discontinued meds may be hidden
5. **Refresh page**: Clear cached medication list
6. **Wait for sync**: EHR updates every 1-4 hours
7. **Contact pharmacy**: If recently filled but not showing

**Timeline**: New medications appear within 4 hours of EHR entry

**Prevention**:
- Have patient bring medication list
- Reconcile at each visit
- Record all OTC and supplements

### "Conditions/Diagnoses Missing"

**Problem**: Patient diagnosis list incomplete

**Causes**: Recently diagnosed, not recorded in EHR, sync delay, multiple records

**Solutions**:
1. **Check diagnosis date**: May not have synced yet
2. **Verify in EHR**: Is diagnosis recorded?
3. **Check for inactive conditions**: May be hidden
4. **Refresh page**: Clear cache
5. **Wait for sync**: Takes 1-4 hours typically
6. **Contact documentation team**: If diagnosis not recorded

**Timeline**: New diagnoses appear within 1-4 hours

### "Allergies Not Showing"

**Problem**: Medication or food allergies missing

**CRITICAL**: This is a safety issue

**Immediate Solutions**:
1. **Verify with patient**: Ask directly about allergies
2. **Check EHR immediately**: Is it recorded there?
3. **If missing from EHR**: Add immediately
4. **Alert team**: Don't prescribe until verified
5. **Document in visit note**: Record patient report

**Prevention**:
- Always ask about allergies at visits
- Reconcile allergy list
- Verify accuracy regularly

## Visit and Order History Issues

### "Missing Visits or Appointments"

**Problem**: Patient visit not showing in history

**Causes**: Not synced yet, visit entered incorrectly, different date, future appointment

**Solutions**:
1. **Check date range**: Is visit within selected dates?
2. **Scroll in list**: May need to scroll for older visits
3. **Check status**: May be draft or pending
4. **Verify in EHR**: Visit recorded there?
5. **Refresh page**: Clear cache
6. **Wait for sync**: May take 1-4 hours
7. **Contact documentation team**: If visit created but not appearing

**Timeline**: Visits appear within 1-4 hours of being finalized

### "Lab Orders Not Appearing"

**Problem**: Ordered lab not showing in patient's order history

**Solutions**:
1. **Verify order placed**: Did submission succeed?
2. **Check order status**: May be pending
3. **Check date range**: Filter may be hiding
4. **Refresh page**: Clear cache
5. **Contact lab**: Verify they received order
6. **Wait for sync**: Orders sync every 1-4 hours
7. **Check EHR**: Verify order in source system

**Timeline**: Orders appear within 15 minutes locally, 4 hours from EHR

### "Lab Results Delayed"

**Problem**: Lab ordered but results not appearing

**Causes**: Results still pending, processing, data entry delay

**Solutions**:
1. **Verify order placed**: Check EHR
2. **Contact lab**: Ask for status
3. **Check expected timeline**: Labs take 24-72+ hours
4. **Verify demographics match**: Lab must match patient info
5. **Wait for sync**: Results sync every 1-4 hours
6. **Check result status**: May be pending clinician review
7. **Contact lab director**: If significantly delayed

**Timeline**: Most results 24-72 hours; critical results 1-2 hours

**Prevention**:
- Confirm demographics before ordering
- Provide clear patient identifiers
- Follow up if results delayed beyond expected

## Data Synchronization Issues

### "Data Hasn't Updated from EHR"

**Problem**: EHR was updated but platform hasn't refreshed

**Causes**: Sync scheduled for different time, sync broken, network issue

**Solutions**:
1. **Refresh page**: Simple refresh may load data
2. **Wait for sync window**: Syncs typically happen hourly
3. **Check sync status**: IT can verify sync running
4. **Manual sync request**: May trigger immediate sync
5. **Check network**: Verify internet connection stable
6. **Try later**: Data may arrive in next sync cycle

**Timeline**: Syncs happen every 1-4 hours typically

### "Sync Error Notification"

**Problem**: See "synchronization error" message

**Solutions**:
1. **Don't panic**: Error usually resolves automatically
2. **Wait 5 minutes**: Sync may retry automatically
3. **Refresh page**: Try loading again
4. **Check internet**: Verify connection stable
5. **Try different browser**: Test if browser-specific
6. **Contact IT**: If error persists more than 30 minutes

**Prevention**:
- Maintain stable internet connection
- Check sync status before critical actions
- Don't work during scheduled sync windows if possible

### "Patient Record Locked for Sync"

**Problem**: Can't edit patient data - "record locked"

**Causes**: Active synchronization, batch update, data migration

**Solutions**:
1. **Wait 5 minutes**: Lock usually releases
2. **Refresh page**: May clear lock
3. **Try different patient**: Verify just this patient
4. **Contact IT**: If locked more than 15 minutes
5. **Note timestamp**: When lock started

**Timeline**: Locks typically last 5-15 minutes

## Best Practices

### Data Entry Quality
- ✅ Verify patient identity before recording data
- ✅ Use standard abbreviations and formats
- ✅ Record data immediately after events
- ✅ Don't estimate or guess patient information
- ✅ Review and correct errors promptly

### Sync Best Practices
- ✅ Check sync status before critical decisions
- ✅ Refresh regularly (every 2-4 hours)
- ✅ Don't make decisions on stale data
- ✅ Verify data in EHR if urgent
- ✅ Report data discrepancies promptly

### Data Quality
- ✅ Report inconsistencies to IT
- ✅ Identify duplicates immediately
- ✅ Correct source data in EHR
- ✅ Don't work around missing data
- ✅ Escalate urgent data issues

## See Also

- [Common Issues & Solutions](./common-issues.md)
- [Data Quality & Validation](../admin/data-quality.md)
- [Patient Panel Management](../workflows/physician/managing-patient-panel.md)
- [Getting Help & Support](../reference/getting-help.md)

## Need Help?

**For Data Issues**: Contact IT Help Desk or data quality team
**For Merge Requests**: Contact IT with patient MRNs
**For Urgent Issues**: Escalate to IT supervisor

---

**Last Updated**: December 2, 2025
**Document Version**: 1.0
