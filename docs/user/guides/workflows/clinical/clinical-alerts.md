---
id: "user-clinical-alerts"
title: "User Guide: Clinical Alerts System"
portalType: "user"
path: "user/guides/workflows/clinical/clinical-alerts.md"
category: "user-guide"
subcategory: "workflow"
tags: ["clinical-alerts", "safety-alerts", "abnormal-values", "patient-safety"]
summary: "Understand clinical alert types, respond appropriately, and manage patient safety through the alert system."
estimatedReadTime: 6
difficulty: "intermediate"
targetAudience: ["physician", "nurse", "clinical-staff"]
prerequisites: ["clinical-documentation", "lab-results", "medication-management"]
relatedGuides: ["lab-results", "medication-management", "patient-communication", "alerts-notifications"]
lastUpdated: "2025-12-02"
---

# Clinical Alerts System

Clinical alerts notify you of patient safety concerns that require immediate attention. This guide explains alert types and appropriate responses.

## Types of Clinical Alerts

### Critical Lab Values
**Severity**: 🔴 CRITICAL

Values that are dangerous or life-threatening:

**Examples**:
- Potassium <2.8 or >6.5 mEq/L (cardiac arrhythmia risk)
- Glucose <50 or >500 mg/dL (seizure or DKA risk)
- Hemoglobin <7 g/dL (severe anemia)
- Troponin elevation (heart attack)
- Blood cultures positive (infection)
- Prothrombin time critically elevated (bleeding risk)

**Your Response**:
1. **Immediately**: Verify result accuracy (not lab error)
2. **Immediately**: Contact patient by phone
3. **Immediately**: Contact attending physician
4. **Urgent**: Plan immediate intervention (ER, urgent visit, medication)
5. **Document**: All actions taken

### Medication Safety Alerts
**Severity**: 🔴 to 🟠 (Critical to High)

Medication-related safety concerns:

**Examples**:
- **Drug Interaction Critical**: Warfarin + NSAID (bleeding risk)
- **Allergy Alert**: Prescribed penicillin to patient allergic to penicillins
- **Dosing Error**: Very high dose inappropriate for patient
- **Contraindication**: ACE inhibitor in patient with kidney disease
- **Duplicate Therapy**: Prescribed two statin medications
- **Monitored Parameter**: Statin prescribed but no baseline liver function

**Your Response**:
1. **Review alert details**: Understand the specific concern
2. **Assess urgency**: How immediate is the danger?
3. **Decide action**:
   - **Critical**: Cancel prescription, contact patient immediately
   - **Major**: Change to alternative medication if possible
   - **Moderate**: Prescribe with monitoring plan
4. **Document**: What you did and why

**Example Response**:
Alert: "Drug interaction - Warfarin + Ibuprofen (bleeding risk)"
Action: "Cancelled ibuprofen. Prescribed acetaminophen instead. Patient aware. Will recheck INR in 1 week."

### Abnormal Lab Result Alerts
**Severity**: 🟠 HIGH (usually)

Abnormal laboratory findings requiring action:

**Examples**:
- High blood glucose (>300 mg/dL)
- High blood pressure reading (>180/110 mmHg)
- Low hemoglobin (significant anemia)
- Thyroid abnormality
- Kidney function decline
- Liver function abnormality

**Your Response**:
1. **Review result**: Understand what's abnormal
2. **Assess clinical significance**: How urgent is this?
3. **Contact patient**: Discuss result and plan
4. **Order follow-up**: Additional testing if needed
5. **Adjust treatment**: Medication changes, lifestyle changes
6. **Document**: Assessment and plan

### Drug Allergy Alerts
**Severity**: 🔴 CRITICAL

Patient has documented allergy to medication/class:

**Examples**:
- Patient allergic to penicillin; trying to prescribe amoxicillin
- Patient allergic to ACE inhibitors; prescribed lisinopril
- Patient allergic to sulfa drugs; prescribed TMP-SMX

**Your Response**:
1. **Verify allergy**: Is allergy accurate? (Not just intolerance?)
2. **Choose alternative**: Select medication from different class
3. **If must use despite allergy**: Document compelling clinical reason
4. **Contact patient**: Warn of allergy; explain plan
5. **Monitor carefully**: Watch for allergic reaction

### Patient Safety Concerns
**Severity**: 🟠 to 🔴 (High to Critical)

Other safety concerns identified by system:

**Examples**:
- **Fall risk**: Patient with dizziness on blood pressure medication
- **Overdose risk**: Very high opioid dose
- **Suicide risk**: Patient with depression and access to means
- **Abuse concern**: Suspicious injuries or pattern
- **Neglect**: Vulnerable patient with concerning living situation

**Your Response** (varies by concern):
1. Assess severity
2. Contact patient if appropriate
3. Involve support services (social work, psychiatry, etc.)
4. Document thoroughly
5. Create safety plan
6. Follow up closely

## Responding to Clinical Alerts

### Alert Display
When alert is issued:
1. **Notification**: Bell icon with number badge (red if critical)
2. **Alert details**: Click to see full information
3. **Patient name**: Who the alert is about
4. **Alert type**: What kind of concern
5. **Specific finding**: What triggered alert
6. **Recommended action**: What system suggests
7. **Context**: Related information (recent changes, history)

### Response Process

**For Critical Alerts**:
1. **Stop what you're doing**: Immediate attention
2. **Open alert**: Read full details
3. **Verify accuracy**: Is this correct? (lab error? wrong patient?)
4. **Contact patient**: By phone immediately
5. **Assess**: Is patient symptomatic? Does patient need ER?
6. **Contact provider**: If not already done
7. **Plan intervention**: What will be done immediately?
8. **Document**: Click **Respond to Alert**, detail your actions
9. **Mark resolved**: When situation is addressed

**For High-Priority Alerts**:
1. **Review alert**: Within 1-2 hours
2. **Contact patient**: Phone call or message
3. **Discuss plan**: What will be done?
4. **Order follow-up**: Additional testing, appointment, medication
5. **Document response**: What you did and why
6. **Mark resolved**: When action complete

**For Standard Alerts**:
1. **Review**: Within same business day
2. **Incorporate into care**: Standard workflow
3. **Document**: In clinical note if action needed
4. **Monitor**: Track if ongoing issue

### Documenting Alert Response
1. Click alert in notification center
2. Click **Respond** or **Add Response**
3. Document:
   - **What you did**: Contacted patient, changed medication, etc.
   - **Patient response**: What patient said/agreed to
   - **Clinical assessment**: Your interpretation
   - **Plan**: What happens next
   - **Follow-up**: When you'll reassess

**Example Documentation**:
"Critical K alert: K 6.8 mEq/L. Contacted patient 3:45 PM. Patient reports some weakness, no chest pain. Advised to go to ER for repeat K and EKG. On-call MD aware. Patient instructed to present with this result to ER."

## Managing False Alarms

### When Alerts Are Incorrect

**Possible Issues**:
- Lab error (sample hemolyzed, wrong patient, equipment error)
- Old result arriving late (not actually new)
- Threshold set too low
- Patient variation (individual baseline is different)
- System error

### Handling False Alarms

1. **Verify**: Confirm alert information is accurate
2. **Follow up**: Contact lab if lab error suspected
3. **Assess patient**: Is patient actually okay?
4. **Document**: Note that alert was false alarm
5. **Escalate if pattern**: Report to quality team if recurring

**Don't**: Dismiss alerts without verification

### Reporting Alert Issues
If certain alerts frequently false:
1. Document pattern
2. Report to IT/quality team
3. Provide specific examples
4. Request review/adjustment of alert criteria

## Alert Fatigue

### Problem: Too Many Alerts
If overwhelmed by alerts:
- Alerts lose clinical significance
- Risk of missing true emergencies
- Provider burnout
- Decreased safety

### Solutions

**1. Turn Off Non-Critical Alerts**:
- Review your alert settings
- Disable lowest-value alerts
- Keep critical/high-priority only
- Better to miss low-value alert than miss critical one

**2. Adjust Thresholds**:
- Some alerts have adjustable criteria
- If threshold too low, increase it
- Example: Only alert if glucose >400 (not >300)
- Balances safety with alert volume

**3. Batch Review**:
- Instead of instant notification
- Review alerts once per shift
- Process in groups
- More efficient workflow

**4. Targeted Monitoring**:
- For certain patients, increase monitoring
- For stable patients, reduce frequency
- Risk-based approach

**5. System Improvement**:
- Report high-volume, low-value alerts
- Request system adjustments
- Collaborate on better alert design
- Work toward smarter alerting

## Special Situations

### Alert During Off-Hours
If critical alert arrives when off-duty:

1. **On-call system**: Coverage protocols
2. **Escalation**: Alert goes to on-call provider
3. **Your responsibility**: If you're on-call, respond; if not, you're not responsible
4. **Hand-off**: Next morning, review what happened

### Alert for Patient You Don't Know
If alert for patient not regularly seen by you:

1. **Verify patient assignment**: Who's responsible?
2. **Contact covering provider**: If you're covering
3. **Escalate**: To attending/supervisor if unclear
4. **Document**: Who you contacted and actions taken

### Conflicting Alerts
If two alerts seem to contradict (e.g., low K and high K):

1. **Verify both results**: Get accurate lab values
2. **Assess which is current**: Most recent is accurate
3. **Check for lab error**: Were tests run correctly?
4. **Contact lab if unclear**: Verify results
5. **Use clinical judgment**: What does patient actually need?

## Alert Settings and Preferences

### Customizing Alerts
1. Click your name (top right)
2. Select **Settings** or **Preferences**
3. Find **Clinical Alerts** section
4. For each alert type:
   - **Turn On/Off**: Enable or disable
   - **Threshold**: Adjust if available
   - **Delivery**: Email, phone, in-platform
5. Save preferences

### Alert Notifications
Configure how you receive alerts:
- **In-platform**: See bell icon in platform
- **Email**: Receive email notification
- **Text/SMS**: Receive text message (usually critical only)
- **Phone**: Receive phone call (critical only, on-call)

### Quiet Hours
Set times when you don't want alerts:
- After-hours (e.g., 6 PM - 8 AM)
- Days off
- Vacation
- Critical alerts still come through

**Remember**: Setting quiet hours doesn't stop critical alerts (safety first)

## Best Practices

### Alert Response Excellence
1. ✅ Treat critical alerts as emergencies
2. ✅ Respond within timeframe (immediate, same-day, etc.)
3. ✅ Verify alert accuracy before acting
4. ✅ Contact patient appropriately
5. ✅ Document response thoroughly
6. ✅ Follow up on outcomes
7. ✅ Report systematic issues
8. ✅ Don't dismiss alerts without investigation
9. ✅ Escalate when needed
10. ✅ Continually improve alert handling

### Common Mistakes to Avoid
❌ **Dismissing alerts without reviewing**
❌ **Not contacting patient about critical findings**
❌ **Acting on false alarms without verification**
❌ **Not documenting what you did**
❌ **Ignoring patterns (false alarms, missed alerts)**
❌ **Not following up on plan**
❌ **Alert fatigue leading to missed critical alerts**

## Troubleshooting Alert Issues

### "Not Receiving Alerts I Should Get"
**Solution**:
1. Check alert settings (may be turned off)
2. Verify notification method configured
3. Check quiet hours setting
4. Test alert by asking IT
5. Contact IT if issue persists

### "Receiving Too Many Low-Value Alerts"
**Solution**:
1. Review alert settings
2. Disable lowest-value alerts
3. Request threshold adjustment if available
4. Report to quality team if system-wide issue
5. Consider batch review instead of instant

### "Got Alert for Patient Not Assigned to Me"
**Solution**:
1. Verify who's actually responsible
2. Contact covering provider if you're covering
3. Escalate to supervisor if unclear
4. Document who you notified
5. Don't take responsibility if not appropriate

## See Also

- [Lab Results & Review](./lab-results.md)
- [Medication Management](./medications.md)
- [Patient Communication](./patient-communication.md)
- [Alerts & Notifications](../features/core/alerts.md)

## Need Help?

### Self-Service Resources
- **Alert Definitions**: Click alert type for details
- **Response Guidance**: Built-in suggestions
- **Examples**: Review past alert responses
- **Settings Help**: In-platform configuration guide

### Support Contacts
- **Clinical Questions**: Attending physician
- **Alert Configuration**: IT or quality team
- **Safety Concerns**: Medical director
- **Technical Issues**: IT Help Desk

---

**Last Updated**: December 2, 2025
**Document Version**: 1.0
**Feedback**: Have suggestions? Contact [documentation team]
