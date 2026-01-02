---
id: "user-physician-care-gaps"
title: "User Guide: Responding to Care Gaps"
portalType: "user"
path: "user/guides/workflows/physician/responding-care-gaps.md"
category: "user-guide"
subcategory: "workflow"
tags: ["physician", "care-gaps", "quality-measures", "clinical-actions", "gap-closure"]
summary: "Complete guide to identifying, understanding, and closing care gaps to improve quality measures and patient outcomes."
estimatedReadTime: 10
difficulty: "intermediate"
targetAudience: ["physician", "provider"]
prerequisites: ["daily-workflow", "managing-patient-panel"]
relatedGuides: ["daily-workflow", "quality-metrics", "creating-orders"]
lastUpdated: "2025-12-02"
---

# Responding to Care Gaps

Care gaps represent clinical opportunities where patients need specific services to meet quality standards and improve health outcomes. This guide explains how to identify, understand, and respond to care gaps in your patient panel.

## Quick Reference

| Activity | Time | Frequency | Impact |
|----------|------|-----------|--------|
| Review gaps in panel | 5-10 min | Daily | High |
| Understand gap context | 1-2 min | Per gap | High |
| Document gap response | 2-5 min | Per gap | Critical |
| Track closure status | 1 min | Daily | Medium |

## What Are Care Gaps?

### Definition
A care gap occurs when a patient hasn't received recommended clinical care based on evidence-based guidelines and quality measure criteria.

### Examples of Common Care Gaps
- **Preventive**: Diabetes patient due for annual A1C test
- **Screening**: 50+ year-old overdue for colorectal cancer screening
- **Medication**: Hypertension patient on suboptimal blood pressure medication
- **Follow-up**: Post-hospitalization patient needs follow-up appointment
- **Education**: Diabetic patient needs diabetes self-management education
- **Mental Health**: No documented depression screening in past 12 months

### Why They Matter
Closing care gaps:
- **Improves Quality**: Better alignment with evidence-based guidelines
- **Enhances Outcomes**: Preventive care reduces complications
- **Improves Measures**: Directly impacts quality metric performance
- **Helps Patients**: Prevents future health problems
- **Supports Payment**: Performance-based reimbursement depends on metrics

## Identifying Care Gaps

### In Your Patient Panel
1. From **Patient Panel** view, look for numbers in parentheses next to patient names
2. Numbers indicate count of open care gaps
3. Example: "John Smith (3)" = John has 3 open gaps
4. Red/orange risk badges often correlate with open gaps

### Patient Detail Page
1. Click on patient name to open detail page
2. Look for **Open Care Gaps** section (usually right sidebar)
3. Shows list of all open gaps for that patient
4. Each gap displays:
   - **Gap Type**: What care is needed (e.g., "A1C Test")
   - **Status**: Current state (Open/Pending/Addressed)
   - **Days Open**: How long gap has existed
   - **Associated Measure**: Which quality measure it affects
   - **Last Reviewed**: When you last looked at it

### From Notifications/Alerts
- System may send you alerts about new gaps
- Particularly high-priority gaps trigger notifications
- Access through **Notifications** bell icon
- See [Alerts & Notifications](../features/core/alerts.md) guide

## Understanding Care Gap Details

### Gap Context Information
When you click on a gap, you see detailed information:

**Clinical Context**:
- **Patient's Current Status**: Relevant clinical history
  - Example: "Last A1C: 8.2% (3 months ago)"
  - Example: "Patient has type 2 diabetes for 8 years"
  - Example: "Last mammogram: Never documented"
- **Guideline Reference**: Why this gap exists
  - Example: "ADA guideline: A1C testing every 6 months for all diabetics"
  - Example: "USPSTF: Colorectal screening every 10 years, starting age 50"
- **Performance Impact**: How this affects quality measures
  - "Diabetes: Comprehensive Care measure"
  - "Preventive Care Performance (25% of measure)"

**Timeline Information**:
- **Gap Opened**: When first identified
- **Days Since Identified**: How long patient hasn't had service
- **Previous Attempts**: Any prior documentation or referrals
- **Next Review Date**: When system will assess again

**Patient Information**:
- **Age and Demographics**: Relevant to guideline appropriateness
- **Active Conditions**: Comorbidities affecting recommendations
- **Current Medications**: Relevant to medication-related gaps
- **Recent Events**: Recent hospital stays, ED visits

### Risk Level Assessment
Gaps show priority level:
- **🔴 Critical**: Patient safety concern or acute need
- **🟠 High**: Important preventive care overdue
- **🟡 Medium**: Routine preventive care approaching due date
- **🟢 Low**: Not yet due, approaching due date

## Gap Response Options

### Option 1: Address the Gap (Complete the Service)

**Use when**: You can provide or arrange the needed service during patient visit

**Process**:
1. Click **Address Gap** button
2. Select **Completed Service** option
3. Enter details:
   - **Date Completed**: When service was provided (today or date of service)
   - **Service Type**: Confirm what was done
   - **Notes**: Optional clinical notes
   - Example: "Patient completed A1C blood draw today in clinic lab"
4. Click **Submit**
5. Gap automatically closes
6. Quality measure improves

**Documentation**: Service is now documented in patient record and quality measure is updated.

**Timeline**: Gap closes immediately and quality metric updates within 24 hours.

### Option 2: Refer Externally (Outside Provider)

**Use when**: Patient needs care from another provider/facility

**Process**:
1. Click **Address Gap** button
2. Select **Referral Needed** option
3. Complete referral details:
   - **Specialty/Service**: What care patient needs
   - **Provider/Facility**: Where patient should go
   - **Clinical Reason**: Why this care is needed
   - **Urgency**: Routine, Urgent, or STAT
   - **Special Instructions**: Any specific requests
   - Example: "Refer to Cardiology for echocardiogram; possible HF"
4. Click **Send Referral**
5. Referral is documented
6. Gap status changes to "Referred" (not yet closed)
7. System tracks referral follow-up

**Tracking**: System monitors referral for:
- **Response from provider**: When you receive results
- **Completion**: When you can close gap with results
- **Overdue**: If no response in 14 days
- **Reminders**: You get alerts if referral responses overdue

**When Gap Closes**:
- For most referrals: Gap closes when you document receiving results
- For some gaps: Gap closes when referral is sent (e.g., specialty consultation)

### Option 3: Patient Declined or Unable to Participate

**Use when**: Patient refuses the service or cannot participate for valid reason

**Process**:
1. Click **Address Gap** button
2. Select **Patient Declined/Unable** option
3. Provide details:
   - **Reason**: Why patient declined/unable
     - "Patient refused lab work"
     - "Patient can't afford $500 imaging cost"
     - "Patient has severe needle phobia"
     - "Patient traveling; will reschedule"
   - **Documentation**: Your clinical assessment
   - **Followup Date**: When to attempt again
     - Default: 30-90 days depending on gap type
     - Set specifically if appropriate
4. Click **Submit**
5. Gap remains open but is marked as "Pending Patient Compliance"
6. System will remind you to follow up at set date

**Important**: Documenting patient refusal requires thoughtful clinical judgment:
- Don't use as routine excuse for gaps
- Document genuine patient barriers
- Plan specific follow-up approach for next attempt
- Consider shared decision-making (patient may have valid concerns)

### Option 4: Clinical Exception (Gap Not Appropriate)

**Use when**: Gap doesn't apply to this patient due to clinical contraindication

**Process**:
1. Click **Address Gap** button
2. Select **Clinical Exception** option
3. Provide clinical justification:
   - **Exception Reason**: Specific clinical rationale
   - **Clinical Evidence**: Why service not appropriate
   - **Practitioner Approval**: Sometimes requires documentation
4. Examples that justify exceptions:
   - "Patient had recent A1C test 2 months ago (gap says 6 months); will retest in 4 months"
   - "Patient allergic to aspirin; can't use for cardiovascular prevention"
   - "Patient with palliative care goals; aggressive cancer screening not appropriate"
   - "Patient has advanced renal disease; can't use metformin"
5. Click **Submit**
6. Gap closes with clinical exception notation
7. Quality measure may not be impacted (depends on measure)

**Important**: Use exceptions judiciously:
- Must have sound clinical basis
- Document thoroughly
- May require justification if audited
- Audit compliance reviews high exception rates

## Care Gap Management Workflow

### Daily Gap Review (5-10 minutes)
**Recommended morning routine**:

1. Open **My Patients** panel
2. Look at your **Open Care Gaps** total (top right)
3. Filter panel by **Has Open Gaps = Yes**
4. Review your highest-risk patients first (red badges)
5. For each patient:
   - Assess: Can I address this today in clinic?
   - Plan: Which patients should I reach out to proactively?
   - Track: Are any critical gaps approaching due date?

### Weekly Focused Gap Management (20-30 minutes)
**Recommended weekly activity**:

1. Pull up **Population Health** view
2. See **Top Care Gaps** chart
3. For top 3-5 most common gaps, create action plan
4. For example, if "A1C Testing" is top gap:
   - Who in my panel needs A1C?
   - Can I do it at upcoming visits?
   - Who needs outreach to schedule?
   - Which referrals to send?
5. Schedule gap-specific outreach if needed
6. Document plan in your records

### Monthly Gap Analysis (30-45 minutes)
**Recommended monthly deep-dive**:

1. From **Population Health**, review trends:
   - Which gaps are increasing/decreasing?
   - Which gaps have best/worst closure rates?
   - Which patients keep getting same gaps repeatedly?
2. Assess your performance:
   - Gap closure rate vs. peer physicians
   - Types of gaps vs. panel composition
   - Closure timeline (how fast?)
3. Make targeted improvements:
   - Special outreach for problematic gaps
   - Clinic process changes (e.g., standing orders)
   - Patient education focused on high-gap areas
4. Document plan and track results

## Gap Closure Best Practices

### During Patient Visits
**Maximize gap closure opportunities**:

1. **Before Visit**: Review patient's open gaps
2. **During Visit**:
   - Discuss gaps during visit discussion
   - Ask what barriers patient has to care
   - Arrange services you can provide today
   - For other services, coordinate appropriate referrals
3. **Complete Documentation**: Document all services and plans
4. **Patient Education**: Explain why gaps matter

**Efficiency Tip**: Many gaps can close during single office visit:
- Order lab tests same visit
- Provide or document screening
- Refer externally
- Educate patient on importance

### Proactive Outreach
**For patients you won't see soon**:

1. Contact patient by phone/message
2. Explain: "We noticed you haven't had [service] recently"
3. Discuss benefits and any barriers
4. Arrange appropriate action:
   - Schedule clinic visit
   - Arrange external provider visit
   - Direct to walk-in lab
5. Document outreach attempt
6. Follow up if patient doesn't follow through

### Standing Orders
**For high-frequency gaps**:

Consider clinic-based solutions:
- **Standing Lab Orders**: Chronic disease management (e.g., annual A1C, annual lipid panel)
- **Standing Referrals**: Routine preventive care (e.g., annual mammogram standing order in EHR)
- **Standing Communication**: Automated reminders when gaps open
- **Standing Education**: Group classes for chronic disease management

Discuss with your clinic administrator; can significantly improve closure rates.

## Common Gap Scenarios

### Scenario 1: Diabetic Patient - Multiple Gaps
**Situation**: Mary, 62, type 2 diabetes
- Gap 1: A1C test (last one 8 months ago)
- Gap 2: Diabetes eye exam (last one 2 years ago)
- Gap 3: Diabetes foot exam (not documented in past 5 years)

**Your Approach**:
1. Schedule comprehensive diabetic visit
2. Order A1C test for this visit (close gap 1 immediately)
3. Refer to ophthalmology for dilated eye exam (send referral for gap 2)
4. Document foot exam during visit (close gap 3)
5. Schedule annual endocrinology if not seen recently
6. Discuss patient's knowledge of self-care (gaps may indicate education need)

**Outcome**: 3 gaps closed in coordinated care

### Scenario 2: Preventive Care Gaps - Asymptomatic Patient
**Situation**: Tom, 52, generally healthy
- Gap: Colorectal cancer screening (age 52, overdue per USPSTF)

**Your Approach**:
1. During next visit (even if for minor issue):
   - Discuss colorectal cancer screening importance
   - Ask about patient's preferences (colonoscopy vs. alternative)
   - Address any fears or concerns
2. If patient interested: Refer to gastroenterology or order appropriate test
3. If patient reluctant:
   - Document discussion and patient's preference
   - Schedule follow-up conversation in 6 months
   - Send reminder letter
4. Discuss family history (increases risk)

**Outcome**: Gap addressed through shared decision-making; patient engages with preventive care

### Scenario 3: Complex Patient with Multiple Issues
**Situation**: James, 76, multiple conditions, multiple open gaps
- Numerous preventive care gaps
- Multiple medication-related gaps
- Limited understanding of conditions

**Your Approach**:
1. Prioritize ruthlessly:
   - Critical safety issues first (e.g., drug interactions)
   - High-impact items next (e.g., fall risk assessment)
   - Routine preventive care last
2. Consider palliative approach:
   - At age 76 with multiple conditions, aggressive screening less important
   - Focus on quality of life
   - Make clinical exceptions for advanced age/frailty
3. Engage care team:
   - Assign case manager for coordination
   - Schedule care plan meeting
   - Regular follow-up for medication management
4. Document thoughtful prioritization (quality audits look for this)

**Outcome**: Appropriate, thoughtful gap management considering patient's overall situation

## Tracking Gap Closure

### Personal Performance View
1. From **Dashboard**, click **My Performance** card
2. View your gap closure metrics:
   - **Total Gaps Closed This Month**: Count
   - **Average Closure Time**: How many days between gap opening and closure
   - **Closure Rate**: % of gaps you close vs. total
   - **Top Gaps Closed**: Which types you're best at
   - **Gaps by Status**: Open, pending, closed breakdown
3. Compare to peers (with privacy protections)
4. Identify improvement opportunities

### Gap Trending
Track your progress over time:
1. From **Quality Metrics** section, view gap trends
2. Look for patterns:
   - Which gap types hard to close?
   - Which closure methods most effective?
   - Time of year affecting closure?
3. Use data to adjust your approach

## Troubleshooting Gap Issues

### "Gap Shows Incorrect Status"
**Causes**:
1. System not updated from recent action
2. Gap closed by another provider
3. EHR integration not synced

**Solution**:
1. Refresh page (Ctrl+R)
2. Check if another provider already addressed gap
3. Wait 15 minutes and check again
4. Contact IT if persists

### "Can't Close Gap - System Won't Accept Documentation"
**Causes**:
1. Missing required information
2. Date entered incorrectly
3. Permissions issue

**Solution**:
1. Check all required fields are completed (system shows red asterisks)
2. Verify date format (usually MM/DD/YYYY)
3. Try different browser
4. Contact IT support

### "Gap Keeps Reopening"
**Causes**:
1. EHR data triggers gap creation again
2. Duplicate orders in system
3. System configuration issue

**Solution**:
1. Check EHR for duplicate data
2. Ensure only one active order exists
3. Contact clinical IT or quality team
4. Request gap suppression if inappropriate

## See Also

- [Physician Daily Workflow](./daily-workflow.md)
- [Creating Orders & Referrals](./orders-referrals.md)
- [Reviewing Quality Metrics](./quality-metrics.md)
- [Clinical Documentation](../../clinical/clinical-docs.md)
- [Patient Health Overview](../../advanced/patient-health-overview.md)

## Need Help?

### Self-Service Resources
- **Help Guide**: Press **?** and search "care gaps"
- **Video Tutorials**: Learning Center > Care Gap Management
- **Peer Learning**: Connect with colleague for best practices

### Support Contacts
- **Clinical Questions**: Medical director or peer physician
- **Care Gap Configuration**: Quality and Compliance team
- **Technical Issues**: IT Help Desk
- **Workflow Questions**: Care management coordinator

---

**Last Updated**: December 2, 2025
**Document Version**: 1.0
**Feedback**: Have suggestions? Contact [documentation team]
