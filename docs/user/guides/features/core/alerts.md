---
id: "user-alerts-notifications"
title: "User Guide: Alerts & Notifications"
portalType: "user"
path: "user/guides/features/core/alerts.md"
category: "user-guide"
subcategory: "feature"
tags: ["alerts", "notifications", "clinical-alerts", "workflow-alerts", "alert-management"]
summary: "Understand different alert types, respond appropriately, and manage notification settings for your workflow."
estimatedReadTime: 6
difficulty: "beginner"
targetAudience: ["physician", "care-manager", "clinical-staff"]
prerequisites: ["platform-navigation", "daily-workflow"]
relatedGuides: ["dashboard-interpretation", "patient-communication"]
lastUpdated: "2025-12-02"
---

# Alerts & Notifications

The platform uses alerts and notifications to draw your attention to important patient events and workflow items. This guide explains alert types and how to respond appropriately.

## Alert Types

### Clinical Alerts (Most Urgent)
**Severity**: 🔴 CRITICAL

Clinical alerts indicate potential patient safety concerns:

**Examples**:
- Dangerous lab value (e.g., potassium 6.8 mEq/L)
- Medication interaction detected
- Allergic reaction suspected
- Abnormal heart rhythm
- Blood pressure crisis
- Hypoglycemia/hyperglycemia emergency
- New neurological symptoms

**Your Response**:
- **Immediate** (within minutes)
- Review alert details fully
- Contact patient immediately if necessary
- Contact physician/provider immediately
- Take clinical action as appropriate
- Document your response
- Do not delay

**Example Alert Display**:
```
🔴 CRITICAL ALERT
Patient: John Smith
Alert: Serum Potassium 6.8 mEq/L (CRITICAL HIGH)
Risk: Cardiac arrhythmia
Action: Recheck immediately; contact MD
Time: 2:45 PM today
```

### Patient Safety Alerts
**Severity**: 🟠 HIGH

Patient safety concerns (not immediately life-threatening but serious):

**Examples**:
- Patient reported fall (assess for injuries)
- Potential medication error
- Missed critical appointment
- ED visit or hospital admission
- Substance use relapse
- Suicidal ideation reported
- Domestic violence concern

**Your Response**:
- **Within hours** (1-4 hours)
- Contact patient to assess
- Take safety action (arrange help, report if needed)
- Involve team members as appropriate
- Adjust care plan
- Document thoroughly

### Care Coordination Alerts
**Severity**: 🟡 MEDIUM

Coordination items needing attention:

**Examples**:
- Care plan review due
- Referral response awaiting follow-up
- Patient has overdue appointment
- New diagnosis reported
- Medication change needs verification
- Task assignment requires action
- Care gap just opened

**Your Response**:
- **Within 1-3 days**
- Review alert details
- Take appropriate action
- Update care plan if needed
- Document actions

### Workflow Notifications
**Severity**: 🟢 LOW

Routine workflow items:

**Examples**:
- Message received from team
- Task assigned to you
- Patient canceled appointment
- New patient admitted to your panel
- Report is ready for review
- Reminder about upcoming event

**Your Response**:
- **As part of routine workflow**
- Review at your convenience
- Respond appropriately
- Can batch these together

## Accessing Alerts and Notifications

### Notification Bell Icon
1. Top navigation, right side, bell icon (🔔)
2. Number badge shows count of unread notifications
3. Click to open notification panel
4. Notifications listed with most recent first

### Full Notification Center
1. Click **Notifications** in main navigation
2. See all notifications (not just current)
3. Filter by:
   - **Type**: Clinical, Safety, Coordination, Workflow
   - **Status**: Unread, Read, Archived
   - **Date**: Today, This week, This month, Custom
   - **Patient**: Search by patient name
4. Sort by date, severity, or patient

### Patient-Specific Alerts
1. From patient detail page, see **Alerts & Notifications** section
2. Shows all alerts/notifications for that patient
3. Can see history of past alerts
4. Click alert for full details

## Understanding Alert Details

### What Each Alert Shows
Click any alert to see full details:

1. **Alert Type**: What kind of alert
2. **Severity Level**: 🔴🟠🟡🟢
3. **Patient Name**: Who it's about
4. **Alert Time**: When detected
5. **Clinical Details**: The actual finding
6. **Context**: Why it's important, what triggered it
7. **Suggested Action**: What you should do
8. **Related Data**: Labs, vitals, previous results for comparison
9. **Alert History**: If patient has had similar alerts before

### Example Alert Details
```
ALERT DETAILS
Type: Medication Interaction
Severity: HIGH (🟠)
Patient: Maria Garcia
Time: 3:20 PM Today
Details: Potential interaction between newly prescribed simvastatin (320mg)
         and existing itraconazole (antifungal). Both metabolized by CYP3A4.
         Risk: Increased statin levels → muscle damage risk.
Suggested Action: Contact prescribing provider immediately. Consider
                  alternative statin or antifungal.
Related: [View patient medications]
History: Patient had similar alert 2 years ago with pravastatin + azole
```

## Responding to Alerts

### Step-by-Step Response Process

**Step 1: Assess Severity**
- What is the alert type?
- How urgent is this really?
- Is it immediately life-threatening?

**Step 2: Understand Context**
- Why did this alert trigger?
- What's the patient's current status?
- Is this new or known issue?
- Any recent changes that explain this?

**Step 3: Take Action** (varies by type):
- **Clinical**: Contact patient/provider immediately
- **Safety**: Contact patient within hours
- **Coordination**: Schedule action within days
- **Workflow**: Incorporate into routine work

**Step 4: Document Response**
1. Click **Respond to Alert**
2. Document what you did:
   - Did you contact patient? What did they say?
   - Did you contact provider? What did they advise?
   - What actions taken?
   - What's the outcome/next step?
3. Provide enough detail for others to understand situation
4. Click **Save Response**

**Step 5: Close Alert** (when resolved)
1. Once action taken and documented
2. Click **Mark as Resolved**
3. Alert moves to closed/historical section
4. But remains visible for reference

### Alert Response Examples

**Example 1: Clinical Alert - High Potassium**
1. Read alert (detected at 2:45 PM)
2. Immediately open patient chart
3. Review: K=6.8 (critical)
4. Call patient immediately: "Please go to ER right now for repeat test"
5. Call on-call physician: "Patient K 6.8, sent to ER"
6. Document: "Patient contacted 2:50 PM, instructed to go to ER. On-call MD aware. Patient confirmed understanding. Directed to present EKG results to ER staff."
7. Mark alert as "Responded"

**Example 2: Care Coordination Alert - Referral Due**
1. Read alert (referral response expected by today)
2. Check patient chart for referral details
3. Call specialist's office: "Did you get Dr. Jones's referral for Mr. Brown?"
4. Learn: Response was sent but not yet in our system
5. Request they re-send or provide summary
6. Document: "Called cardiology 4/15 at 2 PM. Confirmed they completed echo. Report being faxed. Will follow up if not received by tomorrow."
7. Create follow-up task for tomorrow
8. Mark alert as "In Progress"

**Example 3: Workflow Alert - Task Assigned**
1. Read alert (new task assigned to you)
2. Review task details
3. Determine priority and timing
4. Schedule time to complete
5. When complete, mark task as done
6. Alert automatically closes

## Managing Notification Settings

### Customizing Which Alerts You Receive
1. Click your name (top right)
2. Select **Settings** or **Preferences**
3. Find **Notifications** section
4. For each alert type, choose:
   - **On**: You receive this alert
   - **Off**: You don't receive this alert
   - **High Priority Only**: Only very urgent instances

### Setting Delivery Method
1. From Notification Settings:
2. Choose how you receive alerts:
   - **In-Platform**: See bell icon, notification center
   - **Email**: Receive email notification
   - **Text/SMS**: Receive text message
   - **Phone**: Receive phone call (critical only)
3. Can set different methods for different alert types

### Setting Quiet Hours
1. From Notification Settings:
2. Set time periods when you don't want notifications:
   - After hours (e.g., 6 PM - 8 AM)
   - Days off
   - During meetings
3. Critical alerts still come through during quiet hours
4. Lower-priority alerts wait until quiet hours end

### Alert Frequency Control
Some alerts may repeat if unresolved:
1. Set how often you want to be reminded:
   - Once (just alert me once)
   - Hourly (remind me every hour)
   - Daily (once per day)
   - Every 4 hours
2. Prevents alert fatigue while ensuring visibility

## Dealing with Alert Fatigue

### Too Many Alerts?
If overwhelmed by alerts:

1. **Turn off non-critical alerts**:
   - Routine workflow notifications (can batch)
   - Very common alerts (alert overload)
   - False positive alerts

2. **Refocus on high-severity only**:
   - Keep clinical + safety alerts
   - Turn on others only as needed

3. **Batch review routine alerts**:
   - Turn off instant notification
   - Review in notification center once daily
   - Process in groups

4. **Report alert quality issues**:
   - Are certain alerts commonly false positives?
   - Are thresholds set too low?
   - Report to IT/quality team for adjustment

## Alert Best Practices

### Excellence with Alerts
✅ **DO**:
- Respond to critical alerts immediately
- Document what you did
- Understand why alert triggered
- Close alerts when resolved
- Adjust your settings as needed
- Learn which alerts are most important
- Take suggested actions seriously

❌ **DON'T**:
- Ignore alerts thinking they're false alarms
- Close alerts without taking action
- Delay on high-severity alerts
- Assume someone else will handle it
- Leave alerts open indefinitely
- Disable alerts without understanding why
- Dismiss patient-specific alerts without assessment

## Troubleshooting Alert Issues

### "I'm Not Receiving Alert I Should Be"
**Causes**:
1. Notification turned off in settings
2. Delivery method not set up
3. Alert didn't actually trigger (met criteria?)
4. System issue

**Solution**:
1. Check notification settings
2. Verify alert type is enabled
3. Review alert triggering criteria
4. Contact IT if continues

### "Receiving Too Many False Alarms"
**Causes**:
1. Alert threshold set too low
2. Known medication interaction not updated
3. System issue
4. Patient data error

**Solution**:
1. Review specific alert details
2. Contact clinical/IT team if threshold wrong
3. Request system update if data incorrect
4. Adjust your notification settings as needed

### "Can't Mark Alert as Resolved"
**Solution**:
1. Verify you have permission to close alert
2. Ensure you've documented response first
3. Try refreshing page
4. Contact IT if issue persists

## See Also

- [Platform Navigation Guide](../../getting-started/platform-navigation.md)
- [Dashboard Interpretation Guide](./dashboard.md)
- [Patient Communication](../clinical/patient-communication.md)

## Need Help?

### Self-Service Resources
- **Alert Help**: Hover over alert type for definition
- **Suggested Actions**: Read carefully (usually good guidance)
- **Video Tutorials**: Learning Center > Alert Management

### Support Contacts
- **Alert Questions**: Your supervisor or medical director
- **Clinical Interpretation**: Attending physician
- **Technical Issues**: IT Help Desk
- **Reporting False Alerts**: Quality team

---

**Last Updated**: December 2, 2025
**Document Version**: 1.0
**Feedback**: Have suggestions? Contact [documentation team]
