---
id: "user-workflow-automation"
title: "User Guide: Workflow Automation"
portalType: "user"
path: "user/guides/advanced/workflow-automation.md"
category: "user-guide"
subcategory: "feature"
tags: ["automation", "workflow-rules", "scheduled-tasks", "efficiency", "time-saving"]
summary: "Set up automated workflows and scheduled tasks to streamline repetitive processes and improve efficiency."
estimatedReadTime: 5
difficulty: "advanced"
targetAudience: ["administrator", "manager", "power-user"]
prerequisites: ["platform-navigation"]
relatedGuides: []
lastUpdated: "2025-12-02"
---

# Workflow Automation

Workflow automation reduces manual work, improves consistency, and frees time for high-value activities.

## Types of Automation

### Rule-Based Automation
**When X happens, do Y**:
- When patient admitted to hospital → Create urgent care plan
- When A1C test ordered → Set reminder to follow up
- When medication allergy identified → Alert prescriber
- When care gap closes → Update quality metric

### Scheduled Automation
**Do X at specific time**:
- Daily: Generate report of new admissions
- Weekly: Send performance reports to team
- Monthly: Run quality measure calculations
- Quarterly: Risk score updates

### Notification Automation
**Alert about important events**:
- New lab result available
- Care gap threshold reached
- Patient not scheduled for preventive service
- Medication due for refill

## Setting Up Automation Rules

### Accessing Automation
1. Click **Admin** > **Workflow Automation**
2. Click **Create New Rule**

### Defining Trigger
"When this happens..."
- Patient status change (admission, discharge)
- Lab result arrives
- Care gap created/closed
- Medication changed
- Appointment scheduled/missed
- Time-based (anniversary of diagnosis, etc.)

### Defining Action
"Then do this..."
- Create task (assign to person, set due date)
- Send notification (alert specific team member)
- Generate report
- Update status
- Create care plan item
- Send message to patient

### Setting Conditions
Optional: Add conditions
- Only if patient is high-risk
- Only if condition is diabetes
- Only if no recent visit
- Only on weekdays
- Only between certain hours

### Example Automation Rule
**Rule: ED Frequent User Alert**
- **Trigger**: ED visit
- **Condition**: If patient has 3+ ED visits in past 90 days
- **Action**:
  - Create task for care manager
  - Send alert to physician
  - Flag patient in system
- **Result**: High ED users identified automatically for intervention

## Managing Automation Rules

### Activating/Deactivating Rules
- Rules can be turned on/off
- Useful if rule needs adjustment
- Allows testing before full activation

### Monitoring Rule Execution
1. View logs of what automation triggered
2. How many times rule executed
3. How many succeeded/failed
4. Adjust if needed

### Updating Rules
If rule needs change:
1. Edit rule definition
2. Test changes on sample
3. Deploy when ready
4. Monitor results

### Retiring Rules
When rule no longer needed:
- Deactivate rule
- Data remains (not deleted)
- Can be reactivated if needed

## Scheduled Tasks

### Setting Up Scheduled Tasks
Tasks that run automatically:

**Example 1: Daily New Admission Report**
- **Schedule**: Every day at 7 AM
- **Action**: Generate report of yesterday's admissions
- **Recipients**: Care management team
- **Result**: Team starts day knowing new patients to address

**Example 2: Weekly Performance Report**
- **Schedule**: Every Friday at 5 PM
- **Action**: Generate quality metric performance
- **Recipients**: Physician, medical director
- **Result**: Weekly performance review

**Example 3: Monthly Risk Score Update**
- **Schedule**: 1st of every month at midnight
- **Action**: Recalculate all patient risk scores
- **Result**: Latest risk data available for care planning

### Scheduling Best Practices
- Choose off-hours when possible (don't impact users)
- Early morning for reports for morning review
- Afternoon/evening for automation completing tasks
- Test on small sample first
- Monitor first execution for issues

## Notification Automation

### Alert Thresholds
Set automation to alert when:
- X% of population below benchmark
- Y number of care gaps opened
- Z% patients non-adherent
- Specific lab value out of range

### Who Gets Notified
- Specific role (all care managers)
- Specific person
- Group/team
- By patient assignment

## Common Automation Scenarios

### Scenario 1: Hospital Discharge Follow-Up
**Goal**: Ensure all discharged patients get contacted

**Automation**:
- Trigger: Hospital discharge
- Action: Create high-priority task for care manager
- Condition: If patient is high-risk
- Result: Care manager follows up within 24 hours

### Scenario 2: Care Gap Population Outreach
**Goal**: Manage high-volume gaps efficiently

**Automation**:
- Trigger: Care gap created
- Action: Create outreach task
- Condition: If gap type = "A1C test"
- Assign to: Next available care manager
- Result: Gaps routed automatically without manual assignment

### Scenario 3: Quality Metric Monitoring
**Goal**: Track metric performance in real-time

**Automation**:
- Trigger: Monthly (1st day of month)
- Action: Calculate quality metrics
- Report to: Physician, medical director
- Result: Metrics current without manual running

## Performance Optimization

### Efficiency Gains
Automation can save:
- **Time**: Elimination of manual tasks
- **Consistency**: Same process every time
- **Accuracy**: Reduction of human error
- **Timeliness**: Immediate vs. waiting for human

### Measuring Impact
Track:
- Time saved (hours/week)
- Consistency (% following same process)
- Accuracy (% of automations correct)
- Adoption (% of target achieved)

## Best Practices

### Automation Excellence
1. ✅ Start with high-volume, repetitive tasks
2. ✅ Test thoroughly before full deployment
3. ✅ Monitor execution regularly
4. ✅ Adjust based on results
5. ✅ Communicate to affected users
6. ✅ Document rules clearly
7. ✅ Retire unused rules
8. ✅ Measure impact

### Common Mistakes
❌ Over-automating (losing human judgment)
❌ Not testing before deployment
❌ Setting and forgetting (not monitoring)
❌ Too many rules (confusing/conflicting)
❌ Poor rule design (catching wrong patients)

## Troubleshooting

### "Automation Rule Not Executing"
**Causes**: Rule deactivated, condition not met, system issue
**Solution**: Verify rule is active, check conditions, test manually

### "Too Many Automations Creating Noise"
**Solution**: Prioritize which are most important, adjust thresholds, retire low-value ones

### "Automation Doing Wrong Thing"
**Solution**: Review rule definition, check conditions, adjust as needed

## See Also

- [Creating Orders](../workflows/physician/orders-referrals.md)
- [Care Plan Management](../workflows/care-manager/care-plan-management.md)

## Need Help?

**Support**: IT Administrator, system analyst

---

**Last Updated**: December 2, 2025
**Document Version**: 1.0
