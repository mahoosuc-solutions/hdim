---
id: "user-physician-daily-workflow"
title: "User Guide: Physician Daily Workflow"
portalType: "user"
path: "user/guides/workflows/physician/daily-workflow.md"
category: "user-guide"
subcategory: "workflow"
tags: ["physician", "workflow", "daily-tasks", "patient-care", "clinical-operations"]
summary: "Complete guide to starting your day, reviewing patients, managing care gaps, and documenting clinical activities as a physician."
estimatedReadTime: 8
difficulty: "beginner"
targetAudience: ["physician", "provider"]
prerequisites: []
relatedGuides: ["managing-patient-panel", "responding-care-gaps", "quality-metrics"]
lastUpdated: "2025-12-02"
---

# Physician Daily Workflow

Your day in the HealthData in Motion platform is designed to maximize efficiency while ensuring comprehensive patient care. This guide walks you through the essential tasks from login through documentation.

## Quick Reference

| Task | Time | Priority | Frequency |
|------|------|----------|-----------|
| Login & System Check | 2 min | Daily | Every day |
| Patient Panel Review | 10-15 min | High | Daily |
| Care Gap Triage | 5-10 min | High | Daily |
| Patient Consultations | Variable | High | Per schedule |
| Care Gap Documentation | 2-5 min | High | As needed |
| End-of-Day Review | 5 min | Medium | Daily |

## Start of Day: First 15 Minutes

### Step 1: Login and Verify Access
1. Navigate to your organization's HealthData in Motion portal
2. Enter your username and password
3. Complete multi-factor authentication if required
4. Click **Sign In**
5. Verify you see your name and role in the top-right corner

**Pro Tip**: Keep your session secure by never sharing your login credentials. If you're using a shared workstation, always log out before stepping away.

### Step 2: Review System Status
1. Look for any red alert banners at the top of the page
2. If there's a system maintenance notice, note the timing
3. Check your **Notifications** icon (bell) in the top navigation
4. Count any critical alerts requiring immediate attention

**What to look for**:
- Red banners = System down or maintenance
- Orange warnings = Degraded performance or data sync delays
- Green checks = All systems normal

### Step 3: Check Your Notifications
1. Click the **Notifications** bell icon
2. Review items in order of severity:
   - **Critical** (red) - Patients needing immediate intervention
   - **High** (orange) - Important items needing same-day attention
   - **Standard** (blue) - Regular workflow items
3. Note any patient names that appear multiple times (high-risk patterns)

## Core Daily Tasks (15-30 Minutes)

### Review Your Patient Panel

**Access Your Panel**:
1. From the home dashboard, click **My Patients** or **Patient Panel**
2. The system displays your assigned patients
3. By default, patients are sorted by:
   - Risk level (highest risk first)
   - Last encounter date (most recent first)
   - Number of open care gaps

**Panel Dashboard View**:
- **Total Patients**: Your full panel size (right sidebar)
- **High-Risk Count**: Patients requiring proactive outreach
- **Open Care Gaps**: Total gaps across your panel
- **Next Appointments**: Upcoming visits in next 7 days
- **Lab Results Pending**: Awaiting your review/action

### Identify Priority Patients

The platform highlights priority patients using visual indicators:

**Risk Badges** (next to patient names):
- 🔴 **Red** = Critical risk (immediate attention needed)
- 🟠 **Orange** = High risk (today's priority)
- 🟡 **Yellow** = Moderate risk (this week)
- 🟢 **Green** = Stable (routine care)

**Care Gap Indicators**:
- Shows number of open gaps in parentheses: "John Smith (3)"
- Indicates clinical gaps needing your response
- See "Responding to Care Gaps" guide for details

### Filter Your Workload

Customize your view based on your schedule:

**Quick Filters** (top of patient list):
- **My Schedule Today**: Shows only patients with appointments today
- **Critical Alerts**: Shows only patients with urgent issues
- **New Results**: Shows patients with pending lab results
- **Overdue Items**: Shows patients with missed follow-ups

**Advanced Filters**:
1. Click **Advanced Filter** button
2. Filter by:
   - Patient age range
   - Specific chronic conditions
   - Last visit date
   - Insurance type
   - Care team assignment
3. Click **Apply Filters**
4. Use **Save Filter** to create a custom view for repeated use

## Managing Consultations Throughout the Day

### Before Patient Visit
1. Click on patient name to open **Patient Detail** page
2. Review the **Patient Summary** card:
   - Demographics
   - Active conditions
   - Current medications
   - Medication allergies
3. Check the **Recent Activity** section:
   - Last appointment notes
   - Recent lab results
   - Current care gaps
   - Active medications changes
4. Scan **Open Care Gaps** section (covered in care gap guide)
5. Review **Upcoming Tests/Orders** if any

### During Patient Visit
- Use the **Clinical Documentation** section (see Clinical Documentation guide)
- Document findings in real-time or after visit
- Note any new symptoms or conditions
- Discuss care gaps and treatment options with patient

### Post-Visit Documentation
1. Return to patient detail page
2. Click **Add Note** button
3. Select note type:
   - **Problem Focused Exam** (brief, specific visit)
   - **Office Visit** (standard encounter)
   - **Follow-up** (brief follow-up call/message)
4. Document your clinical findings
5. See Clinical Documentation guide for full details

## Care Gap Management During the Day

### Quick Gap Review
1. In your **Patient Panel**, look for numbers in parentheses next to names
2. Click patient name to see open care gaps
3. For each gap, you'll see:
   - Gap type (e.g., "Diabetes - A1C Test")
   - Patient status
   - Recommended action
   - Time since gap opened

### Gap Response Options

For each care gap, you have three primary response options:

**Option 1: Address the Gap**
- Document that you've provided the recommended service
- Example: "Patient completed A1C test today"
- Gap auto-closes after documentation

**Option 2: Refer Externally**
- Patient needs care from another provider
- Document the referral with details
- Example: "Referred to endocrinology for diabetes management"
- Track referral status

**Option 3: Patient Declined or Non-Adherent**
- Patient refused the care
- Document reason (if willing to share)
- Set follow-up date to attempt again
- Example: "Patient declined A1C test today; will attempt at next visit"

**Option 4: Clinical Exception**
- Gap doesn't apply to this patient
- Document clinical reason
- Example: "Patient had recent A1C test (within 3 months); gap closed"

See **Responding to Care Gaps** guide for step-by-step details.

## Quality Metrics Awareness

### Understanding Your Performance

Throughout your day, be aware of how your actions impact quality metrics:

**Commonly Tracked Measures**:
- **Preventive Care**: Screenings, vaccinations, health maintenance
- **Chronic Disease Management**: Lab monitoring, medication management
- **Mental Health Screening**: Annual screening for depression
- **Medication Safety**: Drug interactions, appropriate prescribing
- **Patient Experience**: Follow-up calls, care plan discussions

### How Your Work Impacts Metrics

Each care gap closure improves your quality performance. The system tracks:
1. **Gap Closure Rate**: % of gaps addressed vs. total
2. **Timeliness**: How quickly you address gaps (days from detection)
3. **Type Distribution**: Which types of gaps you address
4. **Patient Outcomes**: Health improvements from addressed gaps

See **Reviewing Quality Metrics** guide for detailed metric interpretation.

## Managing Orders and Referrals

### Creating Orders During the Day
1. From patient detail page, click **New Order** button
2. Select order type:
   - **Lab Order** (blood tests, urinalysis, etc.)
   - **Imaging Order** (X-ray, ultrasound, MRI, etc.)
   - **Other Services** (PT, pulmonology, etc.)
3. Select specific tests/services
4. Add clinical indication (why you're ordering)
5. Set urgency level (routine, urgent, STAT)
6. Designate ordering provider (usually yourself)
7. Click **Send Order**

**Orders immediately**:
- Appear in patient chart
- Get sent to relevant departments/labs
- Create alerts if urgent
- Generate follow-up reminders

### Creating Referrals
1. From patient detail page, click **New Referral**
2. Select specialty needed (Cardiology, Endocrinology, etc.)
3. Add clinical reason
4. Select external provider or organization
5. Set urgency level
6. Add any specific requests or clinical notes
7. Click **Send Referral**

**Referrals**:
- Get sent to external providers
- Create a tracking item for follow-up
- Generate reminders if no response in 7-14 days
- Closed when you document receipt of results

See **Creating Orders & Referrals** guide for complete details.

## End-of-Day Tasks (5 Minutes)

### Complete Outstanding Documentation
1. Review your **Open Notes** section
2. Any documentation started but not completed shows here
3. Complete any incomplete notes before logging off
4. Save all changes

### Review Unaddressed Notifications
1. Check your notifications again
2. Delegate any tasks that can wait until tomorrow
3. Note critical items for first thing tomorrow

### Final Checklist
- ✅ All patient encounters documented
- ✅ All orders/referrals sent
- ✅ All care gaps reviewed (even if deferred intentionally)
- ✅ No incomplete notes in "Open Notes"
- ✅ Reviewed tomorrow's schedule if relevant

### Logout Securely
1. Click your name in top-right corner
2. Select **Logout**
3. Close browser tab completely
4. On shared workstations, clear browser cache if prompted

## Common Daily Scenarios

### Scenario 1: Patient Arrives with Multiple Care Gaps
**Situation**: John arrives for office visit; system shows 3 open gaps

**Your Approach**:
1. Prioritize based on urgency (life-threatening vs. preventive)
2. Address immediate clinical need first
3. During visit, complete tests/screenings if possible
4. Document each gap address in notes
5. For gaps you can't address today, explain to patient and schedule follow-up

### Scenario 2: Lab Results Arrive Mid-Day
**Situation**: Patient's A1C test results are back while they're still in office

**Your Approach**:
1. Review results immediately
2. Discuss with patient if still present
3. Adjust medications/care plan if needed
4. Document your interpretation and plan
5. Schedule follow-up if needed
6. Close related care gap if results are within goal

### Scenario 3: Unexpected Critical Alert
**Situation**: Red alert notification appears for patient with dangerous lab value

**Your Approach**:
1. Immediately click alert to see details
2. Review patient's current medications and conditions
3. Determine if patient requires immediate contact/treatment
4. If urgent, call patient directly
5. If non-urgent but needs adjustment, schedule callback
6. Document all actions taken

## Pro Tips for Efficiency

### Keyboard Shortcuts
- **Ctrl+P**: Quick patient search
- **Ctrl+N**: New note
- **Ctrl+G**: Go to patient's care gaps
- **Ctrl+R**: Create referral
- **Ctrl+O**: Create order

### Mobile Workflow
- Can view patient panel on mobile
- Limited ability to document on mobile
- Good for reviewing alerts while away from desk
- See **Mobile App Features** guide for full mobile capabilities

### Time Management Tips
1. **Batch your reviews**: Review all notifications at start of day
2. **Use filters**: Show only your schedule for focused view
3. **Close gaps during visits**: Address preventive gaps while patient present
4. **Document immediately**: Don't let notes pile up until end of day
5. **Use templates**: Frequently used documentation has templates

## Troubleshooting Common Issues

### "Patient Panel Won't Load"
**Solution**:
1. Refresh your browser (Ctrl+R or Cmd+R)
2. Clear browser cache (see Performance Tips guide)
3. If persists, log out and log back in
4. Contact IT support if issue continues

### "Care Gap Shows Incorrect Status"
**Solution**:
1. Refresh patient detail page
2. Verify the gap hasn't already been closed by another provider
3. Check timestamp - gaps update every 15 minutes
4. Contact care gap team if you believe there's an error

### "Can't Create Order or Referral"
**Solution**:
1. Verify patient has complete demographic information
2. Confirm you have ordering privileges (check Permissions guide)
3. Try again after 5 minutes (system may be syncing data)
4. Contact IT support if continues

### "Missing Patients from My Panel"
**Solution**:
1. Check active filters (may be hiding some patients)
2. Click "Clear Filters" to see full panel
3. Verify patients are still assigned to you
4. Contact your care management coordinator

## See Also

- [Managing Your Patient Panel](../managing-patient-panel.md)
- [Responding to Care Gaps](../responding-care-gaps.md)
- [Creating Orders & Referrals](../orders-referrals.md)
- [Reviewing Quality Metrics](../quality-metrics.md)
- [Clinical Documentation](../../clinical/clinical-docs.md)
- [Platform Navigation Guide](../../getting-started/platform-navigation.md)

## Need Help?

### Self-Service Resources
- **Knowledge Base**: Search "physician daily workflow" in Help menu
- **Video Tutorials**: Available in Learning Center
- **Keyboard Shortcuts**: Press **?** to view all shortcuts in platform

### Support Contacts
- **Clinical Questions**: Your medical director or care management team
- **Technical Issues**: IT Support (internal extension or help desk)
- **Workflow Issues**: Your care management coordinator
- **Urgent Clinical Concerns**: Escalate according to your organizational protocols

---

**Last Updated**: December 2, 2025
**Document Version**: 1.0
**Feedback**: Have suggestions? Contact [documentation team]
