---
id: "user-dashboard-interpretation"
title: "User Guide: Dashboard Interpretation Guide"
portalType: "user"
path: "user/guides/features/core/dashboard.md"
category: "user-guide"
subcategory: "feature"
tags: ["dashboard", "widgets", "metrics", "analytics", "visualization"]
summary: "Understand all dashboard widgets, interpret metrics, and customize your view for your specific role and needs."
estimatedReadTime: 8
difficulty: "beginner"
targetAudience: ["physician", "care-manager", "administrator"]
prerequisites: ["platform-navigation"]
relatedGuides: ["alerts-notifications", "search-filtering", "performance-scorecards"]
lastUpdated: "2025-12-02"
---

# Dashboard Interpretation Guide

Your dashboard provides at-a-glance insights into your patient panel, performance, and key metrics. This guide explains all dashboard widgets and how to use them effectively.

## Dashboard Layout

### Standard Dashboard Structure
Your dashboard has several sections (left to right, top to bottom):

**Left Sidebar** (vertical):
- Patient panel summary
- Quick actions
- Saved views
- Navigation shortcuts

**Main Content Area**:
- Multiple dashboard widgets in grid
- Customizable arrangement
- Different widgets for different roles
- Can scroll to see all widgets

**Right Sidebar** (optional):
- Alerts and notifications
- Quick stats
- Upcoming appointments/tasks
- Performance indicators

## Common Dashboard Widgets

### Your Patient Panel Widget
**Location**: Usually top-left

**Shows**:
- **Total Patients**: How many patients in your panel
- **Active Patients**: Seen in last 12 months
- **New Patients**: Added in last 30 days
- **High-Risk**: Needing intensive management
- **Overdue for Care**: Need appointments/tests

**How to Use**:
1. Click any number to filter panel by that category
2. Use to identify quick metrics
3. Monitor trends (new patients, high-risk count)
4. Click **View Full Panel** to drill deeper

### My Quality Performance Widget
**Location**: Top center (for physicians/administrators)

**Shows**:
- **Overall Quality Score**: Composite of major metrics (0-100)
- **Metrics Tracked**: How many different quality measures
- **Benchmark Comparison**: Your performance vs. target
- **Month over Month**: How you're trending
- **Top Performing Measures**: Where you're strongest
- **Needs Attention**: Where you lag

**How to Use**:
1. Click overall score to see detailed breakdown
2. Click individual metrics to drill down
3. Check trend monthly
4. Focus improvement efforts on weak areas
5. Celebrate strong performance

**Example**: "Your quality score is 82 (target: 85). You're strong in diabetes management (92) but need help with preventive screening (71). Click Preventive Screening for action plan."

### My Patients by Risk Widget
**Location**: Top right (for care managers)

**Shows**:
- **Red** (Critical): Count of highest-risk patients
- **Orange** (High): Count of high-risk patients
- **Yellow** (Moderate): Count of moderate-risk
- **Green** (Low): Count of low-risk
- **Visual Pie Chart**: Distribution across risk tiers
- **Trend**: Is average risk improving or declining?

**How to Use**:
1. Hover over each color to see count
2. Click color to filter panel by risk tier
3. Monitor pie chart (should be roughly normal distribution)
4. Watch trend (should be moving toward yellow/green)
5. Focus efforts on moving red → orange → yellow

**Example**: Red 8 (10%), Orange 20 (25%), Yellow 35 (45%), Green 15 (20%). Your focus areas: getting red patients to orange, preventing more admissions.

### Top Care Gaps Widget
**Location**: Center

**Shows**:
- **Most Common Gaps** in your panel (bar chart)
- **Gap Name**: What kind of gap (A1C testing, etc.)
- **Count**: How many patients with this gap
- **Closure Rate**: % you typically close this type
- **Trending**: Is gap count increasing or decreasing

**How to Use**:
1. See which gaps most prevalent in your population
2. Click top gap to see all patients with that gap
3. Plan targeted outreach for top gaps
4. If gap rate increasing, may need intervention
5. Focus improvement on high-count, low-closure gaps

**Example**: "A1C Testing is your top gap (24 patients, 60% closure rate). Diabetes Education is second (18 patients, 40% rate). Click A1C to contact those 24 patients."

### Pending Orders & Referrals Widget
**Location**: Lower left (for physicians)

**Shows**:
- **Lab Orders Pending**: Awaiting results
- **Imaging Orders**: In process
- **Referrals Pending**: Awaiting specialist response
- **Count by Status**: How many in each stage
- **Oldest Pending**: Highlights overdue items
- **Recent Additions**: New orders from today

**How to Use**:
1. Monitor pending items regularly
2. Click any item to see details
3. Follow up on very old pending orders
4. Identify if any patient needs attention based on pending tests
5. Plan next steps for pending referrals

### My Tasks Widget
**Location**: Lower center

**Shows**:
- **Due Today**: Tasks with today's deadline
- **Overdue**: Tasks past due date
- **This Week**: Tasks due within 7 days
- **Count by Priority**: High, Medium, Low
- **Recent Additions**: New tasks from overnight

**How to Use**:
1. Click **Due Today** to see immediate action items
2. Complete Due Today items first
3. Address Overdue items
4. Plan week based on This Week view
5. Click task to open and complete
6. Mark complete when done

**Example**: "3 tasks due today: Call John Smith (high priority), Submit care plan update (medium), Review referral response (low). Start with John."

### Recent Patient Updates Widget
**Location**: Right sidebar (if present)

**Shows**:
- **Lab Results**: New results from past 24 hours
- **ED Visits**: Patients who visited emergency department
- **Hospitalizations**: New admissions
- **Appointment Completions**: Visits completed yesterday
- **Documentation Added**: Recent notes/documentation
- **Medication Changes**: Recent prescription changes

**How to Use**:
1. Scan daily for significant changes
2. Red flags: ED visits, hospitalizations
3. Green flags: Lab results, completed visits
4. Click any item to open patient chart
5. Act on urgent items (ED/hospital)
6. Incorporate positive results into care planning

### Upcoming Appointments Widget
**Location**: Right sidebar (for providers/managers)

**Shows**:
- **Today's Appointments**: Patients you're seeing today
- **Tomorrow's Appointments**: Advance planning
- **This Week**: What's coming up
- **Time and Provider**: Scheduling details
- **Patient Info**: Name, age, reason (if documented)

**How to Use**:
1. Check first thing in morning
2. Prepare for day's patients
3. Review charts before each appointment
4. Note any special patient needs
5. Plan if extra time needed

## Widget Customization

### Arranging Widgets
1. Click **Customize Dashboard** button (usually top right)
2. Dashboard enters edit mode
3. **Drag widgets**: Click and drag to rearrange
4. **Resize widgets**: Use corner handles to resize
5. **Lock position**: Once arranged as you like, click **Lock**
6. Click **Done** when finished

### Adding/Removing Widgets
1. Click **Customize Dashboard**
2. See list of available widgets
3. Check box to add widget (appears on dashboard)
4. Uncheck to remove widget (disappears from dashboard)
5. Only include widgets you'll actually use
6. Click **Done**

### Saving Custom Dashboards
1. Arrange widgets as preferred
2. Click **Save As Custom View**
3. Give view a name ("My Daily Workflow", "Quick Review", etc.)
4. Save
5. Dashboard saved with that arrangement
6. Switch between saved views from dropdown

### Role-Based Default Dashboards
System provides default dashboards for each role:

**Physician Dashboard Includes**:
- Patient panel summary
- Quality performance
- Care gaps
- Pending orders
- My tasks
- Patient updates

**Care Manager Dashboard Includes**:
- Patient panel summary
- Patients by risk
- Top care gaps
- My tasks
- Recent patient updates
- Upcoming appointments/calls

**Administrator Dashboard Includes**:
- Team performance
- Quality metrics
- Population health
- Utilization trends
- Staff productivity
- Financial metrics

## Drill-Down Analysis

### Going Deeper
Most dashboard widgets are clickable:

**Example Drill-Down**:
1. **Dashboard**: See "A1C Testing - 24 patients"
2. **Click Widget**: Opens list of all 24 patients with A1C gap
3. **Click Patient**: Opens patient detail page
4. **Click Care Gap**: Shows specific gap details
5. **Click Action**: Take action (address gap, create task, etc.)

**This lets you go**: Widget → List → Patient → Specific Item → Action

### Exporting Data
Some widgets allow exporting:
1. Click **Export** (usually bottom of widget)
2. Choose format (CSV, PDF, Excel)
3. Download file
4. Use for external analysis, sharing, or reporting

## Time Period Controls

### Changing Time Ranges
Most widgets show data for default period (usually last 30 days or current month):

1. Look for **Time Period Selector** (often top of widget)
2. Options typically include:
   - Last 7 days
   - Last 30 days
   - Current month
   - Last quarter
   - Last year
   - Custom date range
3. Select desired period
4. Widget updates to show data for that period

### Using Trends
Compare periods to identify trends:
- **Month over month**: Is January better than December?
- **Year over year**: Is this year better than last year?
- **Recent trend**: Last 30 days vs. previous 30 days
- **Seasonal patterns**: Same time last year to account for seasonality

## Mobile Dashboard

### Mobile View
On mobile devices:
1. Dashboard simplifies to single column
2. Shows most critical widgets only
3. Large touch targets
4. Fewer details (space constraint)
5. Can still click to drill down

### What's Available on Mobile
- Patient panel summary
- My tasks
- Recent updates
- Upcoming appointments

### What's Limited on Mobile
- Complex visualizations may be simplified
- Drill-down still works but harder
- Customization limited
- Better to use desktop for detailed analysis

## Dashboard Best Practices

### Morning Routine with Dashboard
Recommended 5-minute start-of-day check:

1. **Scan Your Panel** (1 min): See risk distribution
2. **Check Tasks** (1 min): Identify due-today items
3. **Review Recent Updates** (1 min): Any urgent patient news
4. **Check Appointments** (1 min): Know what's coming
5. **Scan Alerts** (1 min): Any critical items

**Total**: 5 minutes of structured information

### Weekly Review
Recommended 10-15 minute weekly check (Friday afternoon):

1. Review quality metrics trend
2. Check care gap trends (improving or worsening?)
3. Assess task completion rate
4. Monitor risk tier distribution
5. Review utilization (ED/hospital visits)
6. Plan next week's priorities

### Monthly Analysis
Recommended 30-minute monthly deep-dive:

1. Comprehensive quality metric review
2. Compare to peers
3. Identify improvement opportunities
4. Check staffing/resource needs
5. Plan focused improvement initiatives
6. Document findings

## Troubleshooting Dashboard Issues

### "Widget Not Showing Expected Data"
**Causes**:
1. Time period filter set to wrong range
2. Widget showing wrong metric
3. Data not yet updated
4. Search/filter hiding data

**Solution**:
1. Check time period selector
2. Hover over widget for tooltip explaining data
3. Wait 15 minutes and refresh (data updates periodically)
4. Click to check if filters applied

### "Widget Looks Broken or Won't Update"
**Solution**:
1. Refresh browser (Ctrl+R)
2. Clear browser cache
3. Remove and re-add widget
4. Try different browser
5. Contact IT if persists

### "Can't Find Widget I Need"
**Solution**:
1. Click **Customize Dashboard**
2. Scroll through available widgets
3. Search widget list for keyword
4. Add desired widget
5. Some widgets role-specific (may not be available)

## See Also

- [Platform Navigation Guide](../../getting-started/platform-navigation.md)
- [Alerts & Notifications](./alerts.md)
- [Search & Filtering](./search.md)
- [Performance Scorecards](../reports/performance-scorecards.md)

## Need Help?

### Self-Service Resources
- **Widget Help**: Hover over widget name for tooltip
- **Video Tutorials**: Learning Center > Dashboard Basics
- **Customization Guide**: In-platform help on customization

### Support Contacts
- **Questions**: Help desk or supervisor
- **Technical Issues**: IT Support
- **Metric Questions**: Quality team

---

**Last Updated**: December 2, 2025
**Document Version**: 1.0
**Feedback**: Have suggestions? Contact [documentation team]
