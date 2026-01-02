---
id: "user-physician-patient-management"
title: "User Guide: Managing Your Patient Panel"
portalType: "user"
path: "user/guides/workflows/physician/managing-patient-panel.md"
category: "user-guide"
subcategory: "workflow"
tags: ["physician", "patient-panel", "population-health", "patient-search", "panel-management"]
summary: "Learn how to view, search, filter, and organize your patient panel for efficient population health management."
estimatedReadTime: 7
difficulty: "beginner"
targetAudience: ["physician", "provider"]
prerequisites: ["daily-workflow"]
relatedGuides: ["daily-workflow", "responding-care-gaps", "quality-metrics"]
lastUpdated: "2025-12-02"
---

# Managing Your Patient Panel

Your patient panel represents all patients assigned to your care. The HealthData in Motion platform provides multiple ways to view, search, and organize this panel for efficient management.

## Quick Reference

| Feature | Use Case | Time Savings |
|---------|----------|--------------|
| Patient Panel View | See all assigned patients at once | 1-2 min setup |
| Quick Search | Find specific patient quickly | 30 seconds |
| Advanced Filters | Segment panel by characteristics | 2-3 min |
| Saved Filters | Reuse custom views | 1 min per view |
| Population Health View | Understand overall panel status | 5-10 min |
| Risk Stratification | Identify highest-need patients | 3-5 min |

## Accessing Your Patient Panel

### Method 1: From Dashboard
1. Click the **Dashboard** link in main navigation
2. Look for **My Patient Panel** card on the right side
3. Click **View Full Panel**
4. Full panel loads with default sorting

### Method 2: Direct Navigation
1. Click **Patients** in main navigation menu
2. System automatically filters to show only your patients
3. You see the same full panel view

### Method 3: Sidebar Quick Access
1. Look for **Assigned Patients** in left sidebar
2. Click to open a quick panel preview
3. Click **View All** to expand to full view

## Understanding Your Panel View

### Key Metrics at the Top
Your panel summary shows critical information:

**Left Side**:
- **Total Patients**: Your full panel size
- **Active Patients**: Patients seen in last 12 months
- **New Patients**: Added in last 30 days
- **Inactive Patients**: Not seen in 12+ months

**Right Side**:
- **High-Risk Patients**: Count needing proactive outreach
- **Open Care Gaps**: Total gaps across entire panel
- **Patients with Overdue Items**: Preventive care or tests due
- **Next 7-Day Appointments**: Upcoming visits

### Patient List Columns

The main table shows each patient with these columns (left to right):

| Column | Shows | What It Means |
|--------|-------|--------------|
| Risk Badge | 🔴🟠🟡🟢 | Severity of health needs |
| Patient Name | Full name | (Gap count shown in parentheses) |
| Age/DOB | Age or birth date | For quick demographic reference |
| Next Appt | Date of next visit | When you'll see them next |
| Open Gaps | Number | Clinical care gaps needing attention |
| Last Visit | Days/months ago | Recency of last encounter |
| Key Condition | Primary diagnosis | Main chronic condition |
| Contact Status | Icons | Recent contact attempts/outcomes |

### Default Sorting
Patients are sorted by default:
1. **Risk Level** (red patients first, then orange, yellow, green)
2. **Number of Open Gaps** (most gaps first)
3. **Days Since Last Visit** (longest ago first)

This puts highest-priority patients at the top.

## Basic Search

### Quick Patient Search
For finding a specific patient:

1. Click the **Search** icon in the top navigation (magnifying glass)
2. Type patient information:
   - **Full name**: "John Smith"
   - **Last name only**: "Smith"
   - **Partial name**: "Smit" (system autocompletes)
   - **Date of birth**: "1/15/1950"
   - **Medical record number**: "MRN12345"
3. Results appear as you type
4. Click patient name to open their detail page

**Search Tips**:
- Search is forgiving (handles misspellings to some degree)
- Shows only patients assigned to you
- If patient not in your panel, you see a message
- Request access from your care coordinator if patient not visible

### Keyboard Shortcut
Press **Ctrl+P** (Windows) or **Cmd+P** (Mac) to open quick search from anywhere in the platform.

## Advanced Filtering

### Opening Advanced Filters
1. From your patient panel, click **Advanced Filters** button (funnel icon)
2. Panel opens on the right side with filter options
3. Select your criteria
4. Click **Apply Filters** at bottom
5. Panel refreshes showing only matching patients

### Available Filters

**Demographic Filters**:
- **Age Range**: Filter to specific age groups (e.g., 65+)
- **Gender**: Male, Female, Other
- **Insurance Type**: Medicare, Medicaid, Commercial, Uninsured
- **Primary Language**: English, Spanish, etc.
- **Distance from Clinic**: Within 5, 10, 25 miles

**Clinical Filters**:
- **Conditions**: Select specific diagnoses (Diabetes, HTN, COPD, etc.)
- **Multiple Conditions**: Choose patients with 2+ conditions
- **Medications**: Filter by current medication (e.g., ACE inhibitor)
- **Recent Hospitalizations**: Admitted in last 30, 60, 90 days
- **ED Visits**: Emergency department use in last period

**Care Status Filters**:
- **Risk Level**: Red, Orange, Yellow, Green only
- **Open Care Gaps**: With gaps, without gaps, or specific gap types
- **Care Plan Status**: Active plan, no plan, requires update
- **Follow-Up Status**: Overdue, due soon, current
- **Appointment Status**: Has appointment, needs appointment

**Engagement Filters**:
- **Last Contact**: Days/weeks since contact attempt
- **Contact Success Rate**: % of successful outreach attempts
- **Medication Adherence**: Estimated adherence level
- **Preventive Screening**: Due, overdue, or current

**Care Team Filters**:
- **Assigned Care Manager**: Specific team member
- **Team**: Entire care team
- **Primary Condition Manager**: Specialist managing specific disease

### Combining Multiple Filters
You can combine filters to create detailed views:

**Example 1: High-Priority Preventive Care**
- Risk Level = Orange or Red
- Age Range = 65+
- Open Care Gaps = Yes
- Conditions = Diabetes
- Result: Elderly diabetic patients needing immediate care

**Example 2: New to Your Panel**
- New Patients = Yes (last 30 days)
- Risk Level = All
- Result: All recently assigned patients

**Example 3: Medication Adherence Focus**
- Conditions = Hypertension
- Medication Adherence = Low
- Last Contact = More than 7 days ago
- Result: Patients struggling with BP med adherence, not recently contacted

## Saving Custom Filters

### Create a Saved Filter
1. Set up your desired filters (see Advanced Filtering above)
2. Click **Save This Filter** button at bottom of filter panel
3. Enter a descriptive name:
   - "Diabetes Follow-up Needed"
   - "High-Risk 65+ Patients"
   - "New Patients This Week"
4. Click **Save**
5. Filter is now available in your filter menu

### Using Saved Filters
1. Click **Advanced Filters** button
2. Look for **Saved Filters** section
3. Click filter name to apply it immediately
4. Panel reloads with saved filter criteria

### Managing Saved Filters
1. In **Saved Filters** section, click **Edit** icon next to filter name
2. Options:
   - **Update**: Modify the filter criteria
   - **Rename**: Change the filter name
   - **Delete**: Remove the filter
   - **Share**: (if available) Share with other providers
3. Click your preferred option

## Population Health View

### Understanding Your Panel Health

For a broader view of your entire panel:

1. From your patient panel, click **Population Health** button (line chart icon)
2. View displays aggregate statistics about all your patients

**Key Metrics Shown**:

**Overall Health Scores**:
- **Average Health Score**: Overall panel health (0-100 scale)
- **Distribution**: How many patients in each risk category
- **Trend**: Whether panel health is improving/declining over time

**Condition Distribution** (pie chart):
- **Top Conditions**: Most prevalent diagnoses in your panel
- **Click to Filter**: Click any condition to filter main panel to those patients

**Care Gap Summary** (bar chart):
- **Most Common Gaps**: Which gaps appear most in your panel
- **Closure Rates**: How quickly gaps are typically closed
- **Trending**: Gap rates over time

**Quality Metrics**:
- **Current Performance**: How your panel performs vs. benchmarks
- **Improvement Needed**: Areas where panel lags benchmarks
- **Strengths**: Areas where panel exceeds benchmarks

**Engagement Metrics**:
- **Appointment Compliance**: % of scheduled appointments kept
- **Medication Adherence**: Estimated overall adherence
- **Outreach Success**: % of successful patient contacts
- **Preventive Screening**: % up-to-date with recommended screenings

### Using Population Health for Decision-Making
Use this view to:
- Identify which conditions are most prevalent in your panel
- Focus improvement efforts on highest-impact areas
- Compare your performance against peer physicians
- Plan proactive outreach campaigns
- Identify training needs for your team

## Risk Stratification

### Understanding Risk Levels
Patients are automatically assigned risk levels based on:
- **Severity of Conditions**: Critical conditions = higher risk
- **Number of Conditions**: Multiple conditions = higher risk
- **Recent Hospitalizations**: Recent ED/hospital use = higher risk
- **Medication Complexity**: Complex regimens = higher risk
- **Engagement Level**: Non-compliant patients = higher risk
- **Social Determinants**: Housing instability, food insecurity, etc.

### Risk Categories

**🔴 Red (Critical Risk)**
- Requires immediate attention
- Examples: Recently hospitalized, unstable condition, multiple emergencies
- Action: Contact today if possible
- Volume: Typically 5-15% of panel

**🟠 Orange (High Risk)**
- Needs proactive outreach this week
- Examples: Uncontrolled chronic disease, medication non-compliance
- Action: Contact within 3-5 days
- Volume: Typically 15-25% of panel

**🟡 Yellow (Moderate Risk)**
- Routine management this month
- Examples: Stable chronic disease, preventive care due
- Action: Contact within 2-4 weeks
- Volume: Typically 30-40% of panel

**🟢 Green (Low Risk)**
- Stable, routine annual care
- Examples: Healthy, preventive care current
- Action: Routine annual contact
- Volume: Typically 30-40% of panel

### Viewing Risk Factors
To understand why a patient has a specific risk level:

1. Click patient name in panel
2. Open **Patient Detail** page
3. Look for **Risk Assessment** card
4. Shows:
   - Overall risk score (0-100)
   - Primary risk factors (top 3)
   - Recent events affecting risk
   - Recommended interventions
5. Click **View Full Risk Assessment** for detailed analysis

## Sorting and Organization

### Sort Options
Click on column headers to sort by that column:
- **Patient Name**: Alphabetical A-Z
- **Age**: Youngest to oldest
- **Next Appointment**: Soonest first
- **Open Gaps**: Most gaps first
- **Last Visit**: Most recent or longest ago
- Click again to reverse sort direction

### Grouping Patients
Some views allow grouping:

1. Click **Group By** button (if available)
2. Choose grouping option:
   - By Risk Level (groups red, orange, yellow, green together)
   - By Condition (groups by primary diagnosis)
   - By Care Manager (groups by assigned care manager)
   - By Care Plan Status
3. Patients reorganize with visual grouping lines

### Bulk Actions
Select multiple patients for actions:

1. Check boxes next to patient names
2. At top, click **Bulk Actions** menu
3. Available options:
   - **Assign to Care Manager**: Reassign selected patients
   - **Add to Care Plan**: Enroll in existing care plan
   - **Export List**: Download patient list
   - **Generate Report**: Create report for selected patients
   - **Send Message**: Send notification to patients/team

## Common Panel Tasks

### Task 1: Find All Diabetic Patients Due for A1C Testing
1. Use **Advanced Filters**
2. Set:
   - Conditions = Diabetes
   - Open Care Gaps = Yes, type "A1C Test"
   - Result: Filtered to diabetic patients with A1C gap
3. Consider saving as "Diabetes A1C Due"

### Task 2: Identify Your Highest-Risk 10 Patients for This Week
1. Use **Advanced Filters**
2. Set:
   - Risk Level = Red or Orange only
   - Result: Top-priority patients
3. Sort by "Last Contact" (longest ago first)
4. Plan outreach for 10 patients at top of list

### Task 3: Get Demographic Profile of Your Panel
1. Use **Population Health View**
2. Review patient age distribution
3. Note racial/ethnic composition
4. Understand insurance mix
5. Identify trends affecting your panel

### Task 4: Find Patients with Specific Medication
1. Use **Quick Search** if looking for one patient
2. Use **Advanced Filters** > Medications if looking for all patients on specific drug
3. Useful for medication recalls or safety alerts

## Mobile Patient Panel Access

### On Mobile Devices
1. Login to platform via mobile browser or app
2. Tap **Patients** in navigation
3. Panel loads in mobile-optimized view
4. Can:
   - View patient list with risk badges
   - Search for specific patients
   - Apply basic filters
   - Click patient to view summary
5. Cannot:
   - Create orders or referrals
   - Document detailed clinical notes
   - Use advanced filters (use desktop)

**Note**: For detailed work, use desktop view. Mobile is best for quick reviews.

## Troubleshooting Panel Issues

### "Patients Missing from Panel"
**Causes**:
1. Active filters hiding patients (click "Clear Filters")
2. Patient reassigned to another provider (check with coordinator)
3. Patient deactivated or no longer assigned
4. Permissions changed

**Solution**:
1. Check current filters (see Filters menu)
2. Clear filters and try again
3. Contact care management coordinator
4. Request access if needed

### "Patient Information Outdated"
**Causes**:
1. Recent admission/ED visit not yet synced
2. EHR system updating data
3. Manual data entry not yet processed
4. System sync delay

**Solution**:
1. Refresh your browser (Ctrl+R)
2. Clear browser cache
3. Wait 15 minutes and check again
4. Patient detail page is most current
5. Contact IT if issue persists

### "Search Not Finding Patient"
**Causes**:
1. Patient not assigned to you
2. Misspelled name or incorrect DOB
3. Patient deactivated/inactive
4. System permissions issue

**Solution**:
1. Try different spelling variations
2. Search by DOB only
3. Search by Medical Record Number if known
4. Contact your coordinator
5. Contact IT for permissions check

### "Performance Slow with Large Panel"
**Causes**:
1. Very large panel (1000+ patients) loading all at once
2. Filters taking time to process
3. Browser cache issues
4. Network connectivity

**Solution**:
1. Use filters to reduce displayed patients
2. Apply a saved filter for faster loading
3. Clear browser cache
4. Refresh page
5. Contact IT if issue continues

## See Also

- [Physician Daily Workflow](./daily-workflow.md)
- [Responding to Care Gaps](./responding-care-gaps.md)
- [Quality Metrics](./quality-metrics.md)
- [Population Health Analytics](../../advanced/population-health.md)
- [Patient Health Overview](../../advanced/patient-health-overview.md)

## Need Help?

### Self-Service Resources
- **In-Platform Help**: Click **?** and search "patient panel"
- **Video Tutorials**: Learning Center > Patient Management
- **Quick Tips**: Hover over field labels for inline help

### Support Contacts
- **Clinical Questions**: Care management team
- **Technical Issues**: IT Support Help Desk
- **Panel Assignment Questions**: Care coordinator
- **Data Quality Issues**: Quality team or IT

---

**Last Updated**: December 2, 2025
**Document Version**: 1.0
**Feedback**: Have suggestions? Contact [documentation team]
