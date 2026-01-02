---
id: "user-search-filtering"
title: "User Guide: Search & Filtering"
portalType: "user"
path: "user/guides/features/core/search.md"
category: "user-guide"
subcategory: "feature"
tags: ["search", "filtering", "patient-search", "advanced-search", "bulk-actions"]
summary: "Master search and filtering to quickly find patients and segment your panel for targeted work."
estimatedReadTime: 7
difficulty: "beginner"
targetAudience: ["all-roles"]
prerequisites: ["platform-navigation"]
relatedGuides: ["managing-patient-panel", "dashboard-interpretation"]
lastUpdated: "2025-12-02"
---

# Search & Filtering

Effective search and filtering lets you find patients quickly and organize your work. This guide covers all search and filtering options.

## Quick Search

### Accessing Quick Search
1. **Keyboard shortcut**: Press **Ctrl+P** (Windows) or **Cmd+P** (Mac)
2. **Icon**: Click search icon (magnifying glass) in top navigation
3. **Navigation**: Click **Search** in main menu

### Using Quick Search
1. Quick search box appears (with cursor ready)
2. Type what you're looking for:
   - **Patient name**: "John Smith" or "Smith"
   - **Partial name**: "Smit" (autocompletes)
   - **Date of birth**: "1/15/1950" or "01-15-1950"
   - **Medical record number**: "MRN12345" or "12345"
   - **Phone number**: "555-1234" (last 4 digits)
   - **Insurance ID**: Patient's insurance number
3. Results appear as you type
4. Click result to open patient detail
5. Only shows patients assigned to you

### Quick Search Tips
- **Forgiving matching**: Handles minor spelling errors
- **Autocomplete**: Suggestions as you type
- **Recent searches**: Shows frequently searched patients
- **Save to favorites**: Star icon on results for quick access
- **Advanced option**: Click "Advanced Search" link for more options

## Advanced Search

### Opening Advanced Search
1. From Quick Search, click **Advanced Search**
2. Or navigate to **Search** > **Advanced**
3. Page shows multiple search fields

### Advanced Search Criteria

**Patient Demographics**:
- **First Name**: Exact or partial
- **Last Name**: Exact or partial
- **Date of Birth**: Specific date or range
- **Age**: Minimum and/or maximum age
- **Gender**: Male, Female, Other
- **Address**: City, State, Zip
- **Phone**: Full or partial number
- **Email**: Full email address

**Medical Information**:
- **Medical Record Number**: Your system ID
- **Insurance ID**: From insurance card
- **Primary Diagnosis**: Search by condition
- **Active Conditions**: Multiple conditions (AND/OR logic)
- **Medication**: Currently taking specific drug
- **Lab Result**: Recent abnormal results
- **Vital Sign**: High/low values (BP, glucose, etc.)

**Care Status**:
- **Care Plan Status**: Active, Pending, Completed
- **Risk Level**: Red, Orange, Yellow, Green
- **Last Visit**: Date range (last seen)
- **Last Contact**: Date range
- **Assigned Care Manager**: Specific person
- **Assigned Physician**: Specific provider

**Advanced Filters**:
- **Insurance Type**: Medicare, Medicaid, Commercial, Uninsured
- **Language Preference**: Primary language spoken
- **Employment Status**: Working, Retired, Disabled, Student
- **Housing**: Stable, Unstable, Homeless
- **Social Support**: Has family, Lives alone, Has case manager

### Performing Advanced Search
1. Fill in desired criteria
2. Leave blank any criteria you don't want to filter by
3. Click **Search** button
4. Results display matching all criteria (AND logic by default)
5. Results show in list with key information
6. Click result to open full patient detail

### Combining Multiple Criteria
**Example Search**:
"Show me diabetic patients over 65 with A1C >8% last seen more than 3 months ago"

Criteria:
- Primary Diagnosis = Diabetes
- Age ≥ 65
- Lab Result = A1C > 8%
- Last Visit = More than 90 days ago
- Result: 12 patients needing intensive diabetes management

### Saving Advanced Searches
1. Set up desired search criteria
2. Click **Save Search** button
3. Name your search (e.g., "High-Risk Diabetics")
4. Choose to keep private or share with team
5. Saved search appears in your **Saved Searches** list
6. Click anytime to run same search again

## Filtering Patient Lists

### Overview of Filters
Filters show all patients matching criteria in your assigned panel.

**Where Filters Appear**:
- **Patient Panel** view (your patient list)
- **Patient Search Results** (after searching)
- **Population Health** (segment population)
- **Care Gaps** (show gaps meeting criteria)

### Basic Filters (Quick Application)

**At top of any patient list**:
1. **Condition Filter**: Select specific disease (Diabetes, HTN, COPD)
2. **Risk Level**: Red, Orange, Yellow, or Green
3. **Care Status**: With active plan, no plan, needs review
4. **Date Range**: Last visit, last contact (date selector)

Click any filter and list updates immediately.

### Advanced Filtering Panel
1. Click **Advanced Filters** button (funnel icon)
2. Detailed panel opens with many options
3. Apply multiple filters simultaneously:
   - Age range
   - Conditions (multiple selections)
   - Medications
   - Risk level
   - Care status
   - Engagement metrics
   - Social determinants
4. Click **Apply Filters**
5. List updates showing only matching patients

### Combining Filters
Build complex views:

**Example 1: High-Priority Diabetic Outreach**
- Conditions = Diabetes
- Risk Level = Red or Orange
- A1C = >7%
- Last Contact = More than 7 days ago
- Result: Diabetics not recently contacted with suboptimal control

**Example 2: Preventive Care Catch-Up**
- Age = 50-75
- Conditions = Any
- Open Care Gaps = Yes
- Last Preventive Screen = More than 1 year ago
- Result: Older patients overdue for screening

**Example 3: Complex Care Coordination**
- Number of Conditions = 3 or more
- Number of Medications = 5 or more
- Last Hospital Visit = Within 6 months
- Risk Level = Red
- Result: Complex patients needing intensive coordination

### Clearing Filters
1. Click **Clear All Filters** button
2. Returns to full patient list
3. Or click **X** on individual filter to remove one filter
4. List updates immediately

## Working with Search Results

### Result Display Options
1. **List View** (default): Patients in rows with key info
2. **Grid View**: Larger cards with more details (if available)
3. **Compact View**: Minimal info, more patients per screen

Change view: Click view icon (list/grid) button

### Result Columns (Customize)
1. Click **Customize Columns** button
2. Check/uncheck what you want to see:
   - Name, Age, Risk Level, Last Visit
   - Open Gaps, Care Plan Status
   - Next Appointment, Contact Info
3. Reorder by dragging columns
4. Click **Save**

### Sorting Results
Click column headers to sort:
- **Name**: A-Z or Z-A
- **Age**: Youngest or oldest first
- **Last Visit**: Most recent or longest ago
- **Open Gaps**: Most gaps first
- **Risk**: Red first or green first

Click header again to reverse sort direction.

### Exporting Results
1. From search results, click **Export** button
2. Choose format:
   - **CSV**: For Excel/spreadsheet
   - **PDF**: For printing/sharing
   - **Excel**: Native Excel format
   - **JSON**: For data integration
3. Download file to your computer
4. Use for external reports, printing, data analysis

## Bulk Actions

### Selecting Multiple Patients
1. Click checkbox next to patient name
2. Click multiple checkboxes to select several patients
3. Or click **Select All** to select all results
4. Selected count shows at top

### Available Bulk Actions
1. From selection, click **Bulk Actions** menu
2. Choose action:

**Care Management**:
- **Add to Care Plan**: Enroll multiple patients in existing plan
- **Assign to Care Manager**: Reassign group to care manager
- **Send Message**: Send notification to selected patients
- **Schedule Outreach**: Plan coordinated outreach

**Ordering**:
- **Generate Orders**: Order same test for all selected
- **Send Referrals**: Send same referral to all
- **Generate Reports**: Create report for group

**Exports**:
- **Export List**: Download patient list
- **Generate Bulk Report**: Create report for selected
- **Print Labels**: Print address/name labels

**Communications**:
- **Email Notification**: Send email to selected
- **Text Message**: Send text to selected
- **Schedule Calls**: Create call task for each

### Example Bulk Actions

**Scenario 1: Diabetes Population Screening**
1. Search: Diabetics with no recent A1C
2. Select All (24 patients)
3. Bulk Actions > Generate Orders
4. Choose: Hemoglobin A1C test
5. Select lab
6. System creates 24 orders
7. Labs get orders immediately

**Scenario 2: Vaccination Campaign**
1. Filter: Age 65+, no recent flu vaccine
2. Select all (156 patients)
3. Bulk Actions > Send Message
4. Template: "Time for your annual flu shot!"
5. Include clinic info and how to schedule
6. All 156 patients get message

**Scenario 3: Care Plan Enrollment**
1. Filter: New high-risk patients this month
2. Select all (8 patients)
3. Bulk Actions > Add to Care Plan
4. Choose: "Intensive Care Management Plan"
5. All 8 get enrollment
6. Care managers notified

## Smart Filters and Suggestions

### AI-Powered Suggestions
System sometimes suggests pre-built searches:
- "High-Risk Patients Needing Contact"
- "Patients with Overdue Preventive Care"
- "Recent ED/Hospital Admits"
- "Patients with Low Medication Adherence"

Click to run suggested search.

### Saved Search Library
Your organization may provide pre-built searches:
- "Diabetes Cohort - Poor Control"
- "Heart Failure - Recent Admission"
- "Mental Health - Active Treatment"
- "Preventive Care - Screening Due"

Access from **Saved Searches** list.

## Search Best Practices

### Efficiency Tips
1. **Use keyboard shortcut**: Ctrl+P for quick access
2. **Save frequent searches**: Build library of common searches
3. **Name searches clearly**: "High-Risk Diabetics Needing A1C" not "Search1"
4. **Combine approaches**: Use quick search for simple, advanced for complex
5. **Review regularly**: Check results weekly for changing populations

### Search Quality
1. **Start simple**: Add criteria incrementally
2. **Review results**: Do results make sense?
3. **Validate**: Click a few patients to verify accuracy
4. **Document criteria**: Note why you searched this way
5. **Share searches**: Help team find useful population segments

## Troubleshooting Search Issues

### "Search Not Finding Patient"
**Causes**:
1. Patient not assigned to you
2. Misspelled name or incorrect DOB
3. Name spelled differently in system
4. Patient deactivated/inactive

**Solution**:
1. Ask care coordinator about patient assignment
2. Try different spelling variations
3. Search by DOB only if name uncertain
4. Contact IT if patient truly should be in system

### "Search Results Seem Wrong"
**Causes**:
1. Filters applied that you didn't notice
2. Criteria not matching what you intended
3. Data not updated in system
4. System error

**Solution**:
1. Click **Clear All Filters**
2. Review filters being applied
3. Try again after data refresh (wait 15 min)
4. Contact IT if results consistently wrong

### "Can't Apply Certain Filters"
**Causes**:
1. No data for that criteria (no matching patients)
2. Permission restriction
3. Filter not available for your role

**Solution**:
1. Try different criteria
2. Check your role/permissions
3. Contact IT for permission questions

## See Also

- [Managing Your Patient Panel](../workflows/physician/managing-patient-panel.md)
- [Dashboard Interpretation Guide](./dashboard.md)
- [Patient Communication](../clinical/patient-communication.md)

## Need Help?

### Self-Service Resources
- **Quick Tips**: Hover over filter labels
- **Video Tutorials**: Learning Center > Search & Filtering
- **Example Searches**: Help menu shows common searches

### Support Contacts
- **How to Search For**: Your supervisor or mentor
- **Data Questions**: Quality team or IT
- **Technical Issues**: IT Help Desk
- **Permission Questions**: Your manager

---

**Last Updated**: December 2, 2025
**Document Version**: 1.0
**Feedback**: Have suggestions? Contact [documentation team]
