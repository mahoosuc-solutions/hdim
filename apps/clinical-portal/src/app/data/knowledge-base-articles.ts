/**
 * Knowledge Base Articles
 *
 * Comprehensive knowledge base content for the Clinical Portal
 */

import { KBArticle } from '../services/knowledge-base.service';

export const KB_ARTICLES: KBArticle[] = [
  // ============================================================================
  // GETTING STARTED
  // ============================================================================
  {
    id: 'welcome-to-clinical-portal',
    title: 'Welcome to Clinical Portal',
    category: 'getting-started',
    tags: ['introduction', 'overview', 'getting-started'],
    roles: [],
    summary:
      'An introduction to the Clinical Portal and its key features for healthcare quality management.',
    content: `# Welcome to Clinical Portal

The Clinical Portal is a comprehensive healthcare quality management platform that helps healthcare organizations track, evaluate, and improve quality measure performance.

## Key Features

### 1. Patient Management
- Search and view patient demographics
- Track clinical history and encounters
- Master Patient Index (MPI) for duplicate detection
- FHIR R4 compliant data storage

### 2. Quality Measure Evaluation
- Evaluate patients against HEDIS and CMS quality measures
- Real-time calculation using Clinical Quality Language (CQL)
- Batch processing for population-level analysis
- Detailed evaluation results with stratification

### 3. Custom Measure Builder
- Create custom quality measures
- Write and test CQL expressions
- Publish measures for organization-wide use
- Version control and audit trail

### 4. Reporting & Analytics
- Generate patient-level quality reports
- Population summary reports
- Export to CSV, PDF, and Excel
- Data visualization and trend analysis

### 5. Real-Time Monitoring
- Live batch processing visualization
- WebSocket-based updates
- Performance metrics and analytics
- 3D data flow visualization

## Getting Started

1. **Explore the Dashboard** - View key metrics and recent activity
2. **Search Patients** - Find and review patient clinical data
3. **Run Evaluations** - Evaluate patients against quality measures
4. **View Results** - Analyze evaluation outcomes
5. **Generate Reports** - Create shareable quality reports

## User Roles

- **Clinician**: View patient data, run evaluations, generate reports
- **Quality Manager**: Manage measures, analyze population trends, create custom reports
- **Analyst**: Access advanced analytics, export data, create visualizations
- **Admin**: User management, system configuration, audit logs

## Need Help?

- Browse this **Knowledge Base** for detailed guides
- Use the **Help Tooltips** (?) on each page
- Open the **Help Panel** for contextual assistance
- Ask the **AI Assistant** for personalized recommendations
`,
    relatedArticles: [
      'dashboard-guide',
      'patients-guide',
      'evaluations-guide',
    ],
    lastUpdated: new Date('2025-11-19'),
    views: 0,
    estimatedReadTime: 5,
  },

  // ============================================================================
  // PAGE GUIDES
  // ============================================================================
  {
    id: 'dashboard-guide',
    title: 'Dashboard Overview',
    category: 'page-guides',
    tags: ['dashboard', 'overview', 'statistics', 'metrics'],
    roles: [],
    summary:
      'Learn how to use the dashboard to monitor quality measure performance and recent activity.',
    content: `# Dashboard Overview

The Dashboard provides a real-time overview of your organization's quality measure performance and recent evaluation activity.

## Key Metrics

### Total Patients
The total number of patients in your system with complete demographic and clinical data.

### Active Measures
Quality measures currently configured and available for evaluation. Includes both standard measures (HEDIS, CMS) and custom measures.

### Recent Evaluations
Number of quality measure evaluations run in the last 30 days. Click to view detailed results.

### Quality Score
Overall quality performance across all active measures. Calculated as the percentage of patients meeting quality criteria.

## Recent Activity

### Evaluation History
View the 10 most recent evaluations with:
- Patient name (MRN)
- Measure evaluated
- Result (Met/Not Met/Excluded)
- Evaluation date and time
- Evaluated by (user)

Click any row to view detailed evaluation results.

### Quick Actions

- **Refresh Data**: Reload dashboard statistics
- **Export Summary**: Download dashboard metrics as CSV
- **View Trends**: See performance trends over time
- **Run Batch**: Start batch evaluation for population

## Filters

- **Date Range**: Filter activity by date (Last 7 days, 30 days, 90 days, Custom)
- **Measure Type**: Show specific measure categories (HEDIS, CMS, Custom)
- **Status**: Filter by evaluation status (All, Met, Not Met, Excluded)

## Tips

- Set a default date range in user preferences for consistent views
- Pin favorite measures to the dashboard for quick access
- Enable auto-refresh to see real-time updates during batch processing
- Use keyboard shortcuts: R (refresh), E (export), B (batch)
`,
    relatedArticles: [
      'evaluations-guide',
      'results-guide',
      'understanding-quality-scores',
    ],
    lastUpdated: new Date('2025-11-19'),
    views: 0,
    estimatedReadTime: 4,
  },

  {
    id: 'patients-guide',
    title: 'Patient Management Guide',
    category: 'page-guides',
    tags: ['patients', 'search', 'mpi', 'demographics', 'fhir'],
    roles: [],
    summary:
      'Comprehensive guide to searching, viewing, and managing patient records in the Clinical Portal.',
    content: `# Patient Management Guide

The Patients page allows you to search, view, and manage patient records with FHIR R4 compliant data.

## Searching for Patients

### Quick Search
Use the search box at the top to find patients by:
- **Name**: First name, last name, or full name
- **MRN**: Medical Record Number
- **Date of Birth**: YYYY-MM-DD format
- **Phone**: Any format (numbers only)

### Advanced Filters
Click "Advanced Filters" to search by:
- **Age Range**: Min and max age
- **Gender**: Male, Female, Other, Unknown
- **City/State**: Geographic location
- **Insurance**: Primary payer
- **Active Status**: Active patients only

### Search Tips
- Use partial names (e.g., "John" finds "John Smith", "Johnny Doe")
- MRN search is exact match
- Phone search removes formatting automatically
- Results are sorted by relevance, then alphabetically

## Patient List View

### Columns Displayed
- **Name**: Full name with MRN in parentheses
- **Date of Birth**: DOB with calculated age
- **Gender**: M/F/O/U
- **Contact**: Phone number
- **City**: Primary address city
- **Status**: Active/Inactive indicator

### Actions
- **View Details**: Click any row to see full patient record
- **Quick Evaluate**: Click evaluate icon to run quality measures
- **Export**: Download patient list as CSV

## Patient Detail View

Click any patient to see:

### Demographics Tab
- Full name, MRN, SSN (masked)
- Date of birth, age, gender
- Full address and contact information
- Preferred language, ethnicity, race
- Emergency contact

### Clinical Data Tab
- **Conditions**: Active and historical diagnoses with ICD-10 codes
- **Medications**: Current medications with dosage and frequency
- **Allergies**: Known allergies and reactions
- **Procedures**: Past procedures with CPT codes and dates
- **Observations**: Vital signs, lab results, BMI

### Encounters Tab
- Visit history with dates and types
- Encounter diagnoses
- Providers involved
- Facility/location

### Quality Tab
- Recent quality measure evaluations
- Met/Not Met/Excluded status for each measure
- Detailed evaluation rationale
- Historical performance trends

### Documents Tab
- Uploaded documents (PDFs, images)
- Clinical notes
- External records
- Test results

## Master Patient Index (MPI)

### Duplicate Detection
The system automatically detects potential duplicate patient records using:
- Name matching (Levenshtein distance)
- Date of birth exact match
- Address similarity
- Phone number match
- SSN match (if available)

### Similarity Threshold
- **85%+ similarity**: Flagged as potential duplicate
- **95%+ similarity**: High-confidence duplicate

### Merging Duplicates
1. Review flagged duplicates in "Potential Duplicates" tab
2. Compare side-by-side patient records
3. Select "primary" record to keep
4. Click "Merge Records"
5. System combines clinical data and maintains audit trail

### Prevention
- Always search before creating new patient
- Use MRN when available
- Verify demographics match exactly
- Review MPI warnings before saving

## Adding New Patients

Click "Add Patient" button to create new record:

1. **Required Fields**:
   - First name, Last name
   - Date of birth
   - Gender
   - MRN (auto-generated if blank)

2. **Recommended Fields**:
   - Phone number
   - Address
   - Insurance information

3. **Optional Fields**:
   - SSN, ethnicity, race
   - Preferred language
   - Emergency contact

4. **Validation**:
   - System checks for duplicates before saving
   - Required fields are validated
   - Phone/email format validation

## Importing Patient Data

### FHIR Import
Upload FHIR Bundle (JSON) with Patient resources:
- Supports FHIR R4 standard
- Validates resources before import
- Creates or updates existing patients
- Imports related resources (Conditions, Procedures, etc.)

### CSV Import
Upload CSV file with patient demographics:
- Download CSV template first
- Required columns: FirstName, LastName, DOB, Gender
- Optional columns: MRN, Phone, Address, Insurance
- System validates and reports errors

### Integration
Connect external EHR systems via:
- HL7 v2 ADT messages
- FHIR API endpoints
- Custom integration adapters

## Best Practices

1. **Search First**: Always search before creating new patients
2. **Verify MPI**: Review duplicate warnings carefully
3. **Keep Data Current**: Update demographics when changes occur
4. **Document Sources**: Note where clinical data came from
5. **Regular Audits**: Review data quality monthly
6. **Train Staff**: Ensure consistent data entry practices
`,
    relatedArticles: [
      'fhir-resources-explained',
      'mpi-guide',
      'how-to-search-patients',
      'importing-patient-data',
    ],
    lastUpdated: new Date('2025-11-19'),
    views: 0,
    estimatedReadTime: 10,
    videoUrl: 'https://example.com/videos/patient-management',
  },

  {
    id: 'evaluations-guide',
    title: 'Quality Measure Evaluations',
    category: 'page-guides',
    tags: [
      'evaluations',
      'quality-measures',
      'cql',
      'hedis',
      'cms',
      'batch',
    ],
    roles: [],
    summary:
      'Learn how to evaluate patients against quality measures using CQL-based evaluation engine.',
    content: `# Quality Measure Evaluations

The Evaluations page allows you to assess patients against quality measures to determine if they meet clinical quality criteria.

## Understanding Quality Measures

### Measure Components

Every quality measure has these populations:

1. **Initial Population (IP)**
   - All patients considered for the measure
   - Example: All patients aged 18-75 with diabetes

2. **Denominator (D)**
   - Patients who should receive the quality intervention
   - Subset of Initial Population
   - Example: Diabetics without ESRD or hospice care

3. **Numerator (N)**
   - Patients who received the quality intervention
   - Example: Diabetics with HbA1c test in measurement period

4. **Denominator Exclusions (DE)**
   - Patients excluded from quality measurement
   - Example: Patients in hospice or with advanced illness

5. **Denominator Exceptions (DX)**
   - Valid reasons for not meeting measure
   - Example: Medical contraindication or patient refusal

### Measure Types

- **HEDIS Measures**: Healthcare Effectiveness Data and Information Set
  - HBD: Hemoglobin A1c Testing for Diabetics
  - CBP: Controlling High Blood Pressure
  - CCS: Cervical Cancer Screening
  - COL: Colorectal Cancer Screening
  - BCS: Breast Cancer Screening

- **CMS Measures**: Centers for Medicare & Medicaid Services
  - CMS122: Diabetes HbA1c Poor Control
  - CMS124: Cervical Cancer Screening
  - CMS125: Breast Cancer Screening
  - CMS130: Colorectal Cancer Screening

- **Custom Measures**: Organization-specific measures
  - Created in Measure Builder
  - Use CQL for logic definition

## Running Single Patient Evaluation

### Step-by-Step

1. **Select Patient**
   - Click "Select Patient" dropdown
   - Search by name or MRN
   - Or click "Evaluate" from Patients page

2. **Choose Measure**
   - Select from available measures
   - Filter by category (HEDIS, CMS, Custom)
   - View measure description

3. **Set Measurement Period**
   - Default: Current calendar year
   - Or select custom date range
   - Must be valid for measure specification

4. **Run Evaluation**
   - Click "Evaluate Patient"
   - System executes CQL logic
   - Results displayed in 2-5 seconds

### Interpreting Results

**Met**: Patient satisfies numerator criteria
- ✓ Green checkmark
- All required criteria fulfilled
- Patient receives quality credit

**Not Met**: Patient does not meet numerator
- ✗ Red X
- Missing one or more criteria
- Opportunity for quality improvement

**Excluded**: Patient in denominator exclusions
- ⊘ Yellow excluded icon
- Not counted in measure calculation
- Valid clinical reason for exclusion

**Not Applicable**: Patient not in denominator
- ⓘ Gray info icon
- Measure doesn't apply to this patient
- Not in target population

### Detailed Results

Click "View Details" to see:
- Population membership (IP, D, N, DE, DX)
- Supporting evidence (codes, dates, values)
- CQL expression evaluation trace
- Gap closure recommendations

## Batch Evaluations

### Running Batch Jobs

Evaluate multiple patients at once:

1. **Select Patients**
   - All patients (full population)
   - Filtered list (by age, condition, etc.)
   - Uploaded patient list (CSV)
   - Saved cohort

2. **Choose Measures**
   - Single measure
   - Multiple measures (up to 20)
   - Measure set (pre-defined bundle)

3. **Configure Job**
   - Set measurement period
   - Enable parallel processing
   - Set priority (normal/high)

4. **Submit and Monitor**
   - Click "Run Batch Evaluation"
   - Monitor progress in real-time
   - Receive notification when complete

### Batch Progress Monitoring

The batch monitor shows:
- **Progress Bar**: Overall completion percentage
- **Status**: Queued → Running → Complete
- **Stats**: Processed / Total patients
- **Errors**: Failed evaluations with reasons
- **ETA**: Estimated time to completion
- **Live Updates**: WebSocket real-time refresh

### Performance Optimization

- **Parallel Processing**: Evaluates multiple patients simultaneously
- **Caching**: Reuses value sets and terminology
- **Chunking**: Processes patients in batches of 100
- **Resource Limits**: Maximum 10,000 patients per batch

## Real-Time Visualization

Enable 3D visualization to see:
- Data flow through CQL evaluation engine
- Resource usage and bottlenecks
- Patient evaluation progress
- Error patterns and anomalies

Access via: Dashboard → "Open Visualization" → "Live Batch Monitor"

## Troubleshooting Evaluations

### Common Issues

**"Patient not in initial population"**
- Patient doesn't meet basic criteria
- Check age, diagnosis codes, enrollment
- Review measure specifications

**"Missing required data"**
- Patient record lacks necessary information
- Example: No HbA1c lab result
- Add missing data and re-evaluate

**"Evaluation timeout"**
- CQL expression too complex
- Patient has large dataset
- Contact admin to increase timeout

**"CQL execution error"**
- Measure definition has issues
- Report to Quality Manager
- Use alternative measure if available

## Best Practices

1. **Understand the Measure**: Read specifications carefully
2. **Verify Data Quality**: Ensure patient data is complete
3. **Use Correct Period**: Measurement period matters
4. **Review Exclusions**: Don't overlook valid exclusions
5. **Document Gaps**: Note missing data for follow-up
6. **Batch for Populations**: Use batch for efficiency
7. **Monitor Progress**: Watch batch jobs for errors
8. **Export Results**: Save evaluation data for reporting
`,
    relatedArticles: [
      'results-guide',
      'cql-language-guide',
      'hedis-measures-explained',
      'cms-measures-explained',
      'troubleshooting-evaluations',
    ],
    lastUpdated: new Date('2025-11-19'),
    views: 0,
    estimatedReadTime: 12,
    codeExamples: [
      {
        title: 'Example CQL Expression',
        language: 'cql',
        code: `library DiabetesHbA1cTesting version '1.0.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

// Initial Population: Adults 18-75 with diabetes
define "Initial Population":
  AgeInYearsAt(start of "Measurement Period") >= 18
    and AgeInYearsAt(start of "Measurement Period") <= 75
    and exists("Diabetes Diagnosis")

// Denominator: Same as IP
define "Denominator":
  "Initial Population"

// Numerator: Had HbA1c test during measurement period
define "Numerator":
  exists("HbA1c Tests During Measurement Period")

define "Diabetes Diagnosis":
  [Condition: "Diabetes"] C
    where C.clinicalStatus ~ "active"
      and C.verificationStatus ~ "confirmed"

define "HbA1c Tests During Measurement Period":
  [Observation: "HbA1c Laboratory Test"] O
    where O.effective during "Measurement Period"
      and O.status in {'final', 'amended', 'corrected'}`,
        description:
          'CQL logic for Diabetes HbA1c Testing quality measure',
      },
    ],
  },

  {
    id: 'results-guide',
    title: 'Viewing and Analyzing Evaluation Results',
    category: 'page-guides',
    tags: ['results', 'evaluation-results', 'analytics', 'export'],
    roles: [],
    summary:
      'Learn how to view, filter, analyze, and export quality measure evaluation results.',
    content: `# Viewing and Analyzing Evaluation Results

The Results page displays outcomes from quality measure evaluations and provides tools for analysis and reporting.

## Results Table

### Columns Explained

**Patient**: Name and MRN
- Click to view patient detail
- Hover for quick demographics preview

**Measure**: Quality measure name and ID
- HEDIS, CMS, or custom measure
- Click for measure specifications

**Result**: Evaluation outcome
- **Met** (✓): Patient meets quality criteria
- **Not Met** (✗): Patient doesn't meet criteria
- **Excluded** (⊘): Patient validly excluded
- **Not Applicable** (ⓘ): Measure doesn't apply

**Score**: Measure-specific score (if applicable)
- Example: HbA1c value for diabetes measure
- Blood pressure reading for hypertension measure
- Percentage, numeric value, or boolean

**Date**: When evaluation was performed
- Sortable by newest/oldest
- Filterable by date range

**Evaluated By**: User who ran evaluation
- Helpful for audit trail
- Track who performed each assessment

### Actions

**View Details** (eye icon)
- See full evaluation breakdown
- Population membership details
- Supporting evidence and codes
- Gap closure opportunities

**Re-evaluate** (refresh icon)
- Run evaluation again with updated data
- Use after adding missing information
- Confirms data entry corrections

**Export** (download icon)
- Download individual result as PDF
- Include full evaluation details
- Attach to patient chart

## Filtering Results

### Quick Filters

**Result Status**
- Met only
- Not Met only
- Excluded only
- Not Applicable only
- All results (default)

**Measure Type**
- HEDIS measures
- CMS measures
- Custom measures
- All types (default)

**Date Range**
- Last 7 days
- Last 30 days
- Last 90 days
- Current year
- Custom range

### Advanced Filters

Click "Advanced Filters" for:

**Patient Demographics**
- Age range
- Gender
- Geographic location
- Insurance type

**Measure Characteristics**
- Measure category
- Steward (NCQA, CMS, Custom)
- Domain (prevention, chronic care, etc.)

**Result Characteristics**
- Score range (if applicable)
- Gap status (has gaps vs. no gaps)
- Priority level

## Analyzing Results

### Summary Statistics

Top of page shows:
- **Total Evaluations**: Count of all results
- **Met Rate**: Percentage meeting criteria
- **Gap Count**: Number with opportunities
- **Trend**: Performance vs. previous period

### Group By Views

Change view to group results:

**By Measure**
- See all patients for each measure
- Compare performance across measures
- Identify lowest-performing measures

**By Patient**
- See all measures for each patient
- Identify patients with multiple gaps
- Prioritize outreach by patient

**By Result**
- Group all Met results together
- Focus on Not Met for gap closure
- Review exclusions for accuracy

### Sorting

Sort results by:
- Patient name (A-Z or Z-A)
- Measure name (A-Z or Z-A)
- Result status (Met first, Not Met first)
- Date (newest or oldest)
- Score (highest or lowest)

## Gap Analysis

### Identifying Opportunities

**Gap Indicator**
- Results marked with gap icon
- Indicates missing data or missing intervention
- Clicking shows gap details

**Gap Types**

1. **Data Gap**
   - Required information not documented
   - Example: Missing HbA1c test result
   - Action: Order test or import result

2. **Care Gap**
   - Intervention not performed
   - Example: Overdue for screening
   - Action: Schedule appointment

3. **Documentation Gap**
   - Care provided but not documented
   - Example: Test done but not recorded
   - Action: Add documentation

### Gap Closure Workflow

1. **Identify Gap**: Review "Not Met" results
2. **Determine Gap Type**: Data, care, or documentation
3. **Take Action**: Order test, schedule visit, or add documentation
4. **Verify**: Re-run evaluation to confirm closure
5. **Document**: Note actions taken in patient chart

## Exporting Results

### Export Options

**CSV Export**
- Click "Export to CSV" button
- All results or filtered subset
- Opens in Excel, Google Sheets, etc.
- Includes all columns plus measure details

**PDF Export**
- Individual result details
- Formatted for printing
- Suitable for patient chart
- Includes evaluation rationale

**Excel Export**
- Advanced export with multiple sheets
- Summary statistics sheet
- Detailed results sheet
- Gap analysis sheet
- Charts and visualizations

**API Export**
- JSON format for system integration
- FHIR MeasureReport resources
- Programmatic access
- Real-time data sync

### Export Customization

Before exporting, configure:
- **Columns**: Select which fields to include
- **Format**: Choose CSV, Excel, or PDF
- **Grouping**: Organize by patient, measure, or result
- **Filters**: Export only selected subset
- **Scheduling**: Set up automatic exports (admin only)

## Detailed Result View

Click "View Details" on any result to see:

### Population Membership
- ✓ Initial Population: Yes/No with criteria
- ✓ Denominator: Yes/No with criteria
- ✓ Numerator: Yes/No with criteria
- ✓ Exclusions: Yes/No with reason
- ✓ Exceptions: Yes/No with reason

### Supporting Evidence

**Clinical Data**
- Diagnosis codes with dates
- Procedure codes with dates
- Lab results with values and dates
- Medication orders with dates

**Evaluation Logic**
- CQL expression evaluation trace
- Step-by-step logic breakdown
- Boolean results for each criterion
- Data retrieval queries

**References**
- Measure specification document
- Clinical guidelines
- Value set definitions
- Coding system references

### Gap Closure Recommendations

If result is "Not Met", system suggests:
- What data is missing
- What intervention is needed
- When to re-evaluate
- Priority level (high/medium/low)

## Performance Trends

### Trend Charts

View performance over time:
- Line chart showing met rate by month
- Bar chart comparing measures
- Pie chart of result distribution
- Trend direction (improving/declining/stable)

### Comparison Views

Compare results:
- Current period vs. previous period
- Your organization vs. benchmarks
- Different patient cohorts
- Different providers or locations

## Best Practices

1. **Filter Strategically**: Focus on actionable results
2. **Prioritize Gaps**: Start with high-impact opportunities
3. **Document Actions**: Note what you did to close gaps
4. **Re-evaluate Promptly**: Confirm gap closure after action
5. **Export Regularly**: Keep offline records for audits
6. **Review Trends**: Monitor month-over-month performance
7. **Use Grouping**: Organize results for easier analysis
8. **Share with Team**: Export and distribute to care teams
`,
    relatedArticles: [
      'evaluations-guide',
      'gap-closure-strategies',
      'exporting-results',
      'understanding-quality-scores',
    ],
    lastUpdated: new Date('2025-11-19'),
    views: 0,
    estimatedReadTime: 10,
  },

  {
    id: 'reports-guide',
    title: 'Generating Quality Reports',
    category: 'page-guides',
    tags: ['reports', 'patient-report', 'population-report', 'export'],
    roles: [],
    summary:
      'Learn how to create, customize, and export patient-level and population-level quality reports.',
    content: `# Generating Quality Reports

The Reports page allows you to create comprehensive quality reports for individual patients or entire populations.

## Report Types

### Patient-Level Reports

Generate detailed quality report for a single patient:

**Contents:**
- Patient demographics
- All quality measure evaluations
- Met/Not Met status for each measure
- Gap analysis with recommendations
- Historical performance trends
- Care plan recommendations

**Use Cases:**
- Share with patient during visit
- Include in patient chart
- Discuss with care team
- Quality improvement outreach

### Population-Level Reports

Generate summary report for a group of patients:

**Contents:**
- Population statistics (size, demographics)
- Measure performance rates
- Numerator and denominator counts
- Stratification by demographics
- Trend analysis over time
- Benchmark comparisons

**Use Cases:**
- Quality program reporting
- Performance dashboards
- Regulatory submissions (HEDIS, MIPS, etc.)
- Board presentations
- Payer reporting

## Creating Patient Reports

### Step-by-Step

1. **Select Report Type**: Click "Patient Report"

2. **Choose Patient**:
   - Search by name or MRN
   - Or select from recent patients
   - System loads all evaluations for patient

3. **Select Measures**:
   - All measures (default)
   - Specific measure set (HEDIS, CMS, Custom)
   - Individual measures (multi-select)

4. **Set Date Range**:
   - Current year (default)
   - Last 12 months
   - Custom range
   - All time

5. **Customize Report**:
   - Include gaps: Yes/No
   - Include recommendations: Yes/No
   - Include trends: Yes/No
   - Include supporting evidence: Yes/No

6. **Generate Report**:
   - Click "Generate Patient Report"
   - Processing takes 5-10 seconds
   - Preview appears on screen

7. **Export or Save**:
   - Download as PDF
   - Save to patient chart
   - Email to patient (if enabled)
   - Print directly

### Report Sections

**Header**
- Patient name, DOB, MRN
- Report date and time
- Reporting organization logo
- Measurement period

**Summary**
- Total measures evaluated
- Number Met, Not Met, Excluded
- Overall quality score
- Comparison to previous period

**Measure Details**
For each measure:
- Measure name and description
- Result (Met/Not Met/Excluded)
- Score (if applicable)
- Evaluation date
- Supporting evidence
- Gap analysis (if Not Met)

**Recommendations**
- Prioritized list of quality improvement opportunities
- Suggested interventions
- Timeline for completion
- Follow-up actions

**Appendix**
- Measure specifications
- Value set definitions
- Clinical guidelines
- Contact information

## Creating Population Reports

### Step-by-Step

1. **Select Report Type**: Click "Population Report"

2. **Define Population**:
   - All patients (full population)
   - Filtered cohort (by demographics, conditions, etc.)
   - Uploaded patient list (CSV)
   - Saved cohort from previous analysis

3. **Select Measures**:
   - All active measures
   - HEDIS measure set
   - CMS measure set
   - Custom measure set
   - Individual measures

4. **Set Reporting Period**:
   - Current calendar year
   - Current measurement year (HEDIS)
   - Custom date range
   - Fiscal year

5. **Configure Stratifications**:
   - By age group (0-17, 18-64, 65+)
   - By gender (M/F/Other)
   - By race/ethnicity
   - By insurance type
   - By location (zip, city, county)
   - By provider/practice

6. **Set Benchmarks** (optional):
   - National HEDIS benchmarks
   - Regional averages
   - Previous year performance
   - Custom targets

7. **Generate Report**:
   - Click "Generate Population Report"
   - Processing time varies by population size
   - Typical: 30 seconds to 2 minutes
   - Email notification when complete

8. **Review and Export**:
   - Preview report in browser
   - Download as PDF or Excel
   - Schedule recurring generation
   - Share with stakeholders

### Report Sections

**Executive Summary**
- Population overview (size, demographics)
- Key performance indicators
- Top performing measures
- Measures needing improvement
- Trend vs. previous period

**Measure Performance**
Table for each measure:
- Numerator count
- Denominator count
- Exclusion count
- Performance rate (%)
- Benchmark comparison
- Star rating (if applicable)

**Stratification Analysis**
Performance broken down by:
- Age groups
- Gender
- Race/ethnicity
- Geographic location
- Insurance type
- Provider/practice

**Trend Analysis**
- Month-over-month performance
- Year-over-year comparison
- Seasonal patterns
- Improvement trajectory
- Forecasting

**Gap Analysis**
- Total gaps identified
- Gap breakdown by measure
- Gap closure opportunities
- Estimated impact of closing gaps
- Prioritized action plan

**Statistical Appendix**
- Methodology notes
- Confidence intervals
- Statistical significance
- Data quality indicators
- Exclusion criteria

## Saved Reports

### Managing Reports

**Save Report**
- Click "Save Report" after generation
- Enter descriptive name
- Add tags for organization
- Set access permissions

**View Saved Reports**
- Browse all saved reports
- Filter by type, date, author
- Search by name or tags
- Sort by creation date

**Actions**
- Re-run with updated data
- Duplicate and modify
- Delete obsolete reports
- Export in different format
- Schedule recurring generation

### Report Scheduling

Set up automatic report generation:

1. **Create Report Template**: Define all parameters
2. **Save Template**: Give it a name
3. **Set Schedule**:
   - Frequency (daily, weekly, monthly, quarterly)
   - Day of week/month
   - Time of day
   - Recipients (email list)
4. **Enable Schedule**: Turn on automatic generation

Reports run automatically and are:
- Emailed to recipients
- Saved to report library
- Available for download

## Exporting Reports

### Export Formats

**PDF**
- Professional formatting
- Suitable for printing
- Includes charts and tables
- Best for: Sharing with stakeholders, presentations

**Excel**
- Multiple sheets (Summary, Details, Trends)
- Pivot tables for analysis
- Raw data included
- Best for: Further analysis, custom charts

**CSV**
- Simple tabular format
- Importable to other systems
- No formatting
- Best for: Data integration, programmatic access

**Word**
- Editable document
- Customizable formatting
- Add narrative sections
- Best for: Custom reports, documentation

**PowerPoint**
- Presentation-ready slides
- Executive summary format
- Charts on separate slides
- Best for: Board meetings, stakeholder presentations

### Export Options

Configure export:
- **Page Size**: Letter, Legal, A4
- **Orientation**: Portrait or Landscape
- **Include**: Specific sections to include/exclude
- **Branding**: Add organization logo and colors
- **Watermark**: Mark as draft, confidential, etc.

## Custom Report Templates

### Creating Templates

Administrators can create reusable report templates:

1. **Define Structure**: Choose sections and layout
2. **Set Defaults**: Default filters, measures, periods
3. **Add Branding**: Logo, colors, fonts
4. **Configure Calculations**: Custom metrics and KPIs
5. **Test Template**: Generate sample report
6. **Publish**: Make available to users

### Using Templates

Users select from available templates:
- Standard Patient Report
- Standard Population Report
- HEDIS Annual Report
- CMS MIPS Report
- Custom Organization Templates

Templates provide consistent formatting and ensure all required information is included.

## Best Practices

1. **Choose Right Report Type**: Patient for individual, Population for aggregate
2. **Set Appropriate Period**: Match regulatory requirements
3. **Include Context**: Add narrative explanations
4. **Use Stratifications**: Identify disparities and patterns
5. **Compare to Benchmarks**: Show relative performance
6. **Show Trends**: Demonstrate improvement over time
7. **Highlight Gaps**: Focus on opportunities
8. **Schedule Regular Reports**: Automate recurring needs
9. **Review Before Sharing**: Verify accuracy and completeness
10. **Archive Reports**: Keep for compliance and historical analysis
`,
    relatedArticles: [
      'results-guide',
      'exporting-results',
      'report-customization',
      'scheduling-reports',
    ],
    lastUpdated: new Date('2025-11-19'),
    views: 0,
    estimatedReadTime: 12,
  },

  {
    id: 'measure-builder-guide',
    title: 'Building Custom Quality Measures',
    category: 'page-guides',
    tags: [
      'measure-builder',
      'cql',
      'custom-measures',
      'quality-measures',
    ],
    roles: ['quality-manager', 'admin'],
    summary:
      'Learn how to create, test, and publish custom quality measures using the Measure Builder and CQL.',
    content: `# Building Custom Quality Measures

The Measure Builder allows quality managers and administrators to create organization-specific quality measures using Clinical Quality Language (CQL).

## When to Create Custom Measures

Use custom measures for:

**Organization-Specific Quality Goals**
- Internal quality improvement initiatives
- Specialty-specific quality metrics
- Local population health needs

**Payer-Specific Requirements**
- Value-based contracts
- Pay-for-performance programs
- Custom quality metrics from commercial payers

**Research and Innovation**
- Pilot quality programs
- Testing new quality concepts
- Research studies

**Regulatory Gaps**
- Measure needs not met by standard HEDIS/CMS measures
- State-specific requirements
- Accreditation body requirements

## Measure Builder Interface

### Main Sections

**Measure Details** (Left panel)
- Basic information
- Metadata
- Populations

**CQL Editor** (Center panel)
- Code editor with syntax highlighting
- Auto-completion
- Error checking
- Line numbers

**Test Panel** (Right panel)
- Test patient selection
- Test results
- Evaluation trace
- Debug information

**Actions** (Top toolbar)
- Save draft
- Validate CQL
- Test measure
- Publish measure
- Version history

## Creating a New Measure

### Step 1: Basic Information

Click "New Measure" and enter:

**Required Fields:**
- **Measure Name**: Descriptive name (e.g., "Annual Wellness Visit Completion")
- **Measure ID**: Unique identifier (e.g., "ORG-AWV-001")
- **Version**: Start with 1.0.0
- **Status**: Draft

**Optional Fields:**
- **Description**: Detailed explanation of what measure assesses
- **Rationale**: Clinical reasoning and evidence base
- **Guidance**: Instructions for data collection and evaluation
- **Improvement Notation**: Higher or lower scores are better

### Step 2: Define Populations

Every measure needs these populations:

**Initial Population**
- Who is eligible for the measure?
- Example: "All patients aged 18-75"

**Denominator**
- Who should receive the intervention?
- Usually same as or subset of Initial Population
- Example: "All eligible patients with diabetes"

**Numerator**
- Who received the intervention?
- Example: "Diabetic patients with HbA1c test in measurement period"

**Denominator Exclusions** (Optional)
- Who should be excluded from measurement?
- Example: "Patients in hospice or with ESRD"

**Denominator Exceptions** (Optional)
- Valid reasons for not meeting measure
- Example: "Medical contraindication or patient refusal"

### Step 3: Write CQL Logic

Click "Edit CQL" to open the code editor.

#### CQL Structure

Every measure has this structure:

\`\`\`cql
library MeasureName version '1.0.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'
include Common version '1.0.0' called Common

// Value sets
valueset "Diabetes": 'http://example.org/valuesets/diabetes'
valueset "HbA1c Test": 'http://example.org/valuesets/hba1c'

// Parameters
parameter "Measurement Period" Interval<DateTime>

// Initial Population
define "Initial Population":
  AgeInYearsAt(start of "Measurement Period") >= 18
    and AgeInYearsAt(start of "Measurement Period") <= 75
    and exists("Diabetes Diagnosis")

// Denominator
define "Denominator":
  "Initial Population"

// Numerator
define "Numerator":
  exists("HbA1c Test During Measurement Period")

// Helper definitions
define "Diabetes Diagnosis":
  [Condition: "Diabetes"] C
    where C.clinicalStatus ~ "active"
      and C.verificationStatus ~ "confirmed"

define "HbA1c Test During Measurement Period":
  [Observation: "HbA1c Test"] O
    where O.effective during "Measurement Period"
      and O.status in {'final', 'amended', 'corrected'}
\`\`\`

#### CQL Editor Features

**Syntax Highlighting**
- Keywords in blue
- Strings in green
- Comments in gray
- Errors in red underline

**Auto-completion** (Ctrl+Space)
- FHIR resource types
- Element names
- Function names
- Value set references

**Error Checking**
- Real-time syntax validation
- Type checking
- Reference validation
- Error messages with line numbers

**Code Snippets**
- Common patterns
- Population templates
- FHIR queries
- Date/time logic

### Step 4: Test the Measure

**Select Test Patient**
1. Click "Select Test Patient"
2. Choose patient with relevant clinical data
3. Patient data loads in test panel

**Run Evaluation**
1. Click "Test Measure"
2. System executes CQL against patient data
3. Results display in test panel

**Review Results**
- Initial Population: Yes/No
- Denominator: Yes/No
- Numerator: Yes/No
- Exclusions: Yes/No
- Exceptions: Yes/No

**Debug Trace**
- Step-by-step evaluation
- Data retrieved for each expression
- Boolean results for each criterion
- Helps identify logic issues

**Test Multiple Patients**
- Test with different scenarios
- Positive cases (should be in numerator)
- Negative cases (should not be in numerator)
- Edge cases (exclusions, exceptions)

### Step 5: Validate

Click "Validate Measure" to check:

**CQL Syntax**
- Valid CQL grammar
- Correct data types
- No undefined references

**FHIR Compliance**
- Valid resource types
- Correct element paths
- Proper code systems

**Logic Completeness**
- All required populations defined
- No circular references
- Reachable definitions

**Value Sets**
- All value sets exist
- Correct OIDs or URLs
- Accessible by evaluation engine

**Performance**
- Estimated execution time
- Resource usage
- Optimization suggestions

### Step 6: Publish

When measure is ready:

1. **Final Review**: Double-check all components
2. **Click "Publish Measure"**
3. **Confirm**: System asks for confirmation
4. **Status Changes**: From "Draft" to "Active"
5. **Available**: Measure now appears in Evaluations page

**Publishing Checklist:**
- ✓ All populations defined
- ✓ CQL syntax valid
- ✓ Tested with multiple patients
- ✓ Validation passed
- ✓ Measure details complete
- ✓ Approval obtained (if required)

## Managing Measures

### Measure Library

View all custom measures:
- **Active**: Published and ready to use
- **Draft**: Work in progress
- **Retired**: No longer active
- **Archived**: Historical versions

### Editing Measures

**Edit Draft**
- Make changes freely
- Test and validate
- Publish when ready

**Edit Active Measure**
- Creates new version (e.g., 1.0.0 → 1.1.0)
- Original version remains active
- Publish new version to replace

**Versioning Strategy**
- **Major** (1.x.x → 2.x.x): Breaking changes to logic
- **Minor** (x.1.x → x.2.x): New features, backward compatible
- **Patch** (x.x.1 → x.x.2): Bug fixes, clarifications

### Retiring Measures

When measure is no longer needed:
1. Select measure
2. Click "Retire Measure"
3. Enter retirement reason
4. Confirm

Retired measures:
- No longer available for new evaluations
- Historical evaluations remain
- Can be reactivated if needed

## CQL Best Practices

### 1. Use Descriptive Names

**Good:**
\`\`\`cql
define "Patients with Diabetes Diagnosis":
  [Condition: "Diabetes"] C
    where C.clinicalStatus ~ "active"
\`\`\`

**Bad:**
\`\`\`cql
define "X":
  [Condition: "Diabetes"] C
    where C.clinicalStatus ~ "active"
\`\`\`

### 2. Break Down Complex Logic

**Good:**
\`\`\`cql
define "Qualifying Patients":
  "Age 18 to 75"
    and "Has Diabetes"
    and "Not in Hospice"

define "Age 18 to 75":
  AgeInYearsAt(start of "Measurement Period") between 18 and 75
\`\`\`

**Bad:**
\`\`\`cql
define "Qualifying Patients":
  AgeInYearsAt(start of "Measurement Period") between 18 and 75
    and exists([Condition: "Diabetes"] C where C.clinicalStatus ~ "active")
    and not exists([Encounter: "Hospice"])
\`\`\`

### 3. Comment Your Code

\`\`\`cql
// Check for HbA1c test during measurement period
// Includes only final, amended, or corrected results
// Excludes cancelled or entered-in-error results
define "HbA1c Test During Measurement Period":
  [Observation: "HbA1c Test"] O
    where O.effective during "Measurement Period"
      and O.status in {'final', 'amended', 'corrected'}
\`\`\`

### 4. Reuse Common Logic

Create a shared library:

\`\`\`cql
library CommonLogic version '1.0.0'

define function "IsActive"(condition Condition):
  condition.clinicalStatus ~ "active"
    and condition.verificationStatus ~ "confirmed"

define function "DuringPeriod"(date DateTime):
  date during "Measurement Period"
\`\`\`

Then include and use:

\`\`\`cql
include CommonLogic version '1.0.0' called CL

define "Active Diabetes":
  [Condition: "Diabetes"] C
    where CL."IsActive"(C)
\`\`\`

### 5. Handle Missing Data

\`\`\`cql
define "HbA1c Value":
  First(
    [Observation: "HbA1c Test"] O
      where O.effective during "Measurement Period"
      sort by effective desc
  ).value as Quantity

define "HbA1c Poor Control":
  "HbA1c Value" > 9 '%'
    or "HbA1c Value" is null  // Handle missing value
\`\`\`

## Troubleshooting

### Common Errors

**"Cannot resolve reference to identifier X"**
- Definition not defined before use
- Typo in definition name
- Missing include statement

**"Type mismatch"**
- Wrong data type used
- Need type cast
- Check FHIR element types

**"No matches for operator"**
- Operator not defined for types
- Check operator compatibility
- Use proper type conversions

**"Value set not found"**
- Value set doesn't exist
- Wrong OID or URL
- Not loaded in terminology service

### Getting Help

- **CQL Documentation**: Full language specification
- **FHIR Documentation**: Resource and element definitions
- **Value Set Repository**: Browse available value sets
- **Example Measures**: Library of working examples
- **Community Forum**: Ask questions and share solutions

## Advanced Topics

### Performance Optimization
- Use indexes on frequently queried fields
- Limit result sets with "First" function
- Cache expensive calculations
- Avoid nested loops

### Integration with EHR
- Map EHR codes to value sets
- Handle custom extensions
- Deal with incomplete data
- Synchronize updates

### Quality Assurance
- Peer review process
- Regression testing
- Version control
- Change management

### Regulatory Compliance
- Documentation requirements
- Audit trail
- Approval workflows
- Measure certification
`,
    relatedArticles: [
      'cql-language-guide',
      'fhir-resources-explained',
      'value-sets-guide',
      'testing-custom-measures',
    ],
    lastUpdated: new Date('2025-11-19'),
    views: 0,
    estimatedReadTime: 15,
    codeExamples: [
      {
        title: 'Complete Measure Example',
        language: 'cql',
        code: `library AnnualWellnessVisit version '1.0.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

valueset "Annual Wellness Visit": 'http://example.org/valuesets/awv'
valueset "Preventive Care Services": 'http://example.org/valuesets/preventive'

parameter "Measurement Period" Interval<DateTime>

define "Initial Population":
  AgeInYearsAt(start of "Measurement Period") >= 18

define "Denominator":
  "Initial Population"

define "Numerator":
  exists("Annual Wellness Visit During Measurement Period")
    or exists("Preventive Care Visit During Measurement Period")

define "Annual Wellness Visit During Measurement Period":
  [Encounter: "Annual Wellness Visit"] E
    where E.period during "Measurement Period"
      and E.status = 'finished'

define "Preventive Care Visit During Measurement Period":
  [Encounter: "Preventive Care Services"] E
    where E.period during "Measurement Period"
      and E.status = 'finished'`,
        description: 'Complete CQL for Annual Wellness Visit measure',
      },
    ],
  },

  // Continue with more articles in next part...
  // Due to length, I'll create a summary of remaining articles

  {
    id: 'ai-assistant-guide',
    title: 'Using the AI Assistant',
    category: 'page-guides',
    tags: ['ai-assistant', 'analysis', 'recommendations', 'help'],
    roles: [],
    summary:
      'Learn how to use the AI Assistant for automated analysis, recommendations, and contextual help.',
    content: `# Using the AI Assistant

The AI Assistant provides intelligent analysis of user interactions, identifies UI/UX issues, and offers actionable recommendations.

## What is the AI Assistant?

The AI Assistant automatically:
- **Tracks** all user interactions across the application
- **Analyzes** patterns, errors, and performance issues
- **Recommends** improvements and solutions
- **Assists** through conversational chat interface

## AI Dashboard

Access via: **Navigation → AI Assistant**

### Statistics Overview

**Total Interactions**
- Count of all tracked user actions
- Includes clicks, page loads, form submissions
- Updated in real-time

**Error Rate**
- Percentage of failed interactions
- Helps identify problematic features
- Target: <5% error rate

**Recommendations**
- Number of AI-generated suggestions
- Prioritized by severity
- Actionable improvements

**Critical Issues**
- High-priority problems requiring immediate attention
- Security vulnerabilities, data errors, UX blockers

### Recommendation Cards

Each recommendation shows:
- **Title**: Brief description of issue
- **Severity**: Low, Medium, High, Critical
- **Category**: UI, UX, Accessibility, Performance, Testing
- **Description**: Detailed explanation
- **Affected Components**: Which pages/features are impacted
- **Implementation Steps**: How to fix the issue
- **Code Example**: Sample code for implementation
- **Estimated Impact**: Expected improvement from fix

### AI Chat Panel

Click "Ask AI Assistant" to open chat interface.

**Ask Questions Like:**
- "How can I improve the UI?"
- "What accessibility issues exist?"
- "Why is the dashboard slow?"
- "What should I test next?"
- "How do I close quality gaps faster?"

**Quick Action Buttons:**
- Improve UI
- Analyze Accessibility
- Performance Bottlenecks
- Testing Suggestions

**Chat Features:**
- Context-aware responses
- References knowledge base articles
- Provides code examples
- Links to relevant documentation

## Analysis Types

### UI Improvement Analysis

Detects:
- High error rates (>20%)
- Frequent failures
- User friction points
- Confusing interfaces

**Example Finding:**
"Patient search has 35% error rate. Add input validation and better error messages."

### UX Enhancement Analysis

Detects:
- Slow interactions (>3 seconds)
- Unused features (<20% usage)
- Workflow bottlenecks
- Navigation issues

**Example Finding:**
"Dashboard takes 4.5s to load. Implement caching and lazy loading."

### Accessibility Analysis

Detects:
- Missing keyboard navigation
- ARIA compliance issues
- Screen reader problems
- Color contrast issues

**Example Finding:**
"Submit button not keyboard accessible. Add tabindex and onKeyPress handler."

### Performance Analysis

Detects:
- Slow API calls
- Memory leaks
- Rendering bottlenecks
- Large bundle sizes

**Example Finding:**
"Patient list re-renders on every keystroke. Add debouncing and memoization."

### Testing Gaps Analysis

Detects:
- High-traffic features without tests
- Error-prone components lacking coverage
- Critical paths missing E2E tests

**Example Finding:**
"Evaluation submission has 15% error rate but no unit tests. Add test coverage."

## Data Management

### Export Interactions
- Download all tracked interactions as JSON
- Useful for offline analysis
- Import into analytics tools
- Share with development team

### Import Interactions
- Upload previously exported data
- Combine data from multiple sessions
- Analyze historical trends

### Clear Data
- Reset all tracking data
- Fresh start for new analysis period
- Cannot be undone

## Best Practices

1. **Review Regularly**: Check AI Dashboard weekly
2. **Act on Critical Issues**: Address high-priority items first
3. **Track Progress**: Export data before and after fixes
4. **Ask Questions**: Use chat for specific guidance
5. **Share Insights**: Distribute recommendations to team
6. **Monitor Trends**: Watch error rates over time

## Privacy & Security

- All data stays local (no external AI calls by default)
- Can enable OpenAI/Claude integration (optional)
- No PHI/PII is sent to AI services
- Interactions are anonymized
- Data cleared on logout (configurable)
`,
    relatedArticles: [
      'ai-code-review-guide',
      'interpreting-recommendations',
      'ai-chat-tips',
    ],
    lastUpdated: new Date('2025-11-19'),
    views: 0,
    estimatedReadTime: 8,
  },

  // Domain Knowledge Articles
  {
    id: 'fhir-resources-explained',
    title: 'FHIR Resources Explained',
    category: 'domain-knowledge',
    tags: ['fhir', 'hl7', 'interoperability', 'data-standards'],
    roles: [],
    summary:
      'Comprehensive guide to FHIR R4 resources used in the Clinical Portal.',
    content: `# FHIR Resources Explained

Fast Healthcare Interoperability Resources (FHIR) is an HL7 standard for exchanging healthcare information electronically.

## What is FHIR?

FHIR (pronounced "fire") defines:
- **Resources**: Common healthcare data elements (Patient, Observation, Medication, etc.)
- **APIs**: RESTful interfaces for data exchange
- **Data Types**: Standard formats (dates, codes, quantities, etc.)
- **Terminology**: Code systems and value sets

## FHIR Version

Clinical Portal uses **FHIR R4** (version 4.0.1), the current stable release.

## Core Resources Used

### Patient

Represents demographic and administrative information about a person receiving care.

**Key Elements:**
- \`identifier\`: MRN, SSN, etc.
- \`name\`: First, last, middle names
- \`gender\`: male | female | other | unknown
- \`birthDate\`: Date of birth (YYYY-MM-DD)
- \`address\`: Street, city, state, zip
- \`telecom\`: Phone, email
- \`extension\`: Race, ethnicity, preferred language

**Example:**
\`\`\`json
{
  "resourceType": "Patient",
  "id": "example-patient-123",
  "identifier": [
    {
      "system": "http://hospital.example.org/mrn",
      "value": "MRN-12345"
    }
  ],
  "name": [
    {
      "family": "Smith",
      "given": ["John", "Robert"]
    }
  ],
  "gender": "male",
  "birthDate": "1965-03-15",
  "address": [
    {
      "line": ["123 Main St"],
      "city": "Springfield",
      "state": "IL",
      "postalCode": "62701"
    }
  ]
}
\`\`\`

### Condition

Records clinical problems, diagnoses, or health concerns.

**Key Elements:**
- \`code\`: Diagnosis code (ICD-10, SNOMED)
- \`subject\`: Reference to Patient
- \`onsetDateTime\`: When condition started
- \`clinicalStatus\`: active | inactive | resolved
- \`verificationStatus\`: confirmed | provisional | differential

**Example:**
\`\`\`json
{
  "resourceType": "Condition",
  "id": "diabetes-condition",
  "clinicalStatus": {
    "coding": [{
      "system": "http://terminology.hl7.org/CodeSystem/condition-clinical",
      "code": "active"
    }]
  },
  "code": {
    "coding": [{
      "system": "http://hl7.org/fhir/sid/icd-10-cm",
      "code": "E11.9",
      "display": "Type 2 diabetes mellitus without complications"
    }]
  },
  "subject": {
    "reference": "Patient/example-patient-123"
  },
  "onsetDateTime": "2020-06-15"
}
\`\`\`

### Observation

Measurements and simple assertions made about a patient.

**Types:**
- Vital signs (BP, temp, pulse, SpO2)
- Lab results (HbA1c, cholesterol, glucose)
- Social history (smoking status, alcohol use)
- Physical findings (BMI, weight, height)

**Key Elements:**
- \`code\`: What was observed (LOINC code)
- \`subject\`: Reference to Patient
- \`effectiveDateTime\`: When observed
- \`value[x]\`: Result (quantity, string, boolean, etc.)
- \`status\`: final | preliminary | amended

**Example:**
\`\`\`json
{
  "resourceType": "Observation",
  "id": "hba1c-result",
  "status": "final",
  "code": {
    "coding": [{
      "system": "http://loinc.org",
      "code": "4548-4",
      "display": "Hemoglobin A1c/Hemoglobin.total in Blood"
    }]
  },
  "subject": {
    "reference": "Patient/example-patient-123"
  },
  "effectiveDateTime": "2025-11-15T09:30:00Z",
  "valueQuantity": {
    "value": 7.2,
    "unit": "%",
    "system": "http://unitsofmeasure.org",
    "code": "%"
  }
}
\`\`\`

### Medication & MedicationRequest

Records medications and orders.

**MedicationRequest:**
- Prescriptions and medication orders
- Dosage, frequency, route
- Start/stop dates

**Key Elements:**
- \`medicationCodeableConcept\`: Drug code (RxNorm)
- \`subject\`: Reference to Patient
- \`dosageInstruction\`: How to take medication
- \`status\`: active | completed | stopped

**Example:**
\`\`\`json
{
  "resourceType": "MedicationRequest",
  "id": "metformin-prescription",
  "status": "active",
  "intent": "order",
  "medicationCodeableConcept": {
    "coding": [{
      "system": "http://www.nlm.nih.gov/research/umls/rxnorm",
      "code": "860975",
      "display": "Metformin 500 MG Oral Tablet"
    }]
  },
  "subject": {
    "reference": "Patient/example-patient-123"
  },
  "dosageInstruction": [{
    "text": "Take 1 tablet by mouth twice daily with meals",
    "timing": {
      "repeat": {
        "frequency": 2,
        "period": 1,
        "periodUnit": "d"
      }
    }
  }]
}
\`\`\`

### Procedure

Records procedures performed on patients.

**Key Elements:**
- \`code\`: Procedure code (CPT, SNOMED)
- \`subject\`: Reference to Patient
- \`performedDateTime\`: When performed
- \`status\`: completed | in-progress | not-done

**Example:**
\`\`\`json
{
  "resourceType": "Procedure",
  "id": "colonoscopy-procedure",
  "status": "completed",
  "code": {
    "coding": [{
      "system": "http://www.ama-assn.org/go/cpt",
      "code": "45378",
      "display": "Colonoscopy, flexible"
    }]
  },
  "subject": {
    "reference": "Patient/example-patient-123"
  },
  "performedDateTime": "2024-08-20T10:00:00Z"
}
\`\`\`

### Encounter

Interaction between patient and healthcare provider.

**Types:**
- Office visit
- Hospitalization
- Emergency visit
- Telehealth

**Key Elements:**
- \`class\`: ambulatory | inpatient | emergency
- \`type\`: Encounter type codes
- \`period\`: Start and end time
- \`participant\`: Providers involved

### AllergyIntolerance

Records allergies and intolerances.

**Key Elements:**
- \`code\`: What patient is allergic to
- \`reaction\`: Manifestation (rash, anaphylaxis, etc.)
- \`criticality\`: low | high | unable-to-assess

## FHIR Data Types

### CodeableConcept
Coded value with optional text:
\`\`\`json
{
  "coding": [{
    "system": "http://loinc.org",
    "code": "4548-4",
    "display": "Hemoglobin A1c"
  }],
  "text": "HbA1c test"
}
\`\`\`

### Quantity
Numerical value with unit:
\`\`\`json
{
  "value": 7.2,
  "unit": "%",
  "system": "http://unitsofmeasure.org",
  "code": "%"
}
\`\`\`

### Period
Time interval:
\`\`\`json
{
  "start": "2025-01-01T00:00:00Z",
  "end": "2025-12-31T23:59:59Z"
}
\`\`\`

### Reference
Link to another resource:
\`\`\`json
{
  "reference": "Patient/example-patient-123",
  "display": "John Smith"
}
\`\`\`

## Search and Queries

FHIR supports RESTful searches:

**Get all patients:**
\`GET /fhir/Patient\`

**Search by name:**
\`GET /fhir/Patient?name=Smith\`

**Search by MRN:**
\`GET /fhir/Patient?identifier=MRN-12345\`

**Get patient's conditions:**
\`GET /fhir/Condition?subject=Patient/123\`

**Search observations by code and date:**
\`GET /fhir/Observation?code=4548-4&date=ge2025-01-01\`

## Best Practices

1. **Use Standard Codes**: LOINC for labs, ICD-10 for diagnoses, CPT for procedures
2. **Include Display Text**: Makes data readable
3. **Set Proper Status**: Distinguish active from historical data
4. **Link Resources**: Use references to connect related data
5. **Validate Data**: Ensure conformance to FHIR specification
6. **Version Carefully**: Be aware of R4 vs. earlier versions

## Resources

- [FHIR R4 Specification](https://hl7.org/fhir/R4/)
- [FHIR Resource Index](https://hl7.org/fhir/R4/resourcelist.html)
- [FHIR Data Types](https://hl7.org/fhir/R4/datatypes.html)
- [FHIR Search](https://hl7.org/fhir/R4/search.html)
`,
    relatedArticles: [
      'patients-guide',
      'importing-patient-data',
      'cql-language-guide',
    ],
    lastUpdated: new Date('2025-11-19'),
    views: 0,
    estimatedReadTime: 12,
  },

  //  Continue with more domain knowledge articles...
  {
    id: 'hedis-measures-explained',
    title: 'HEDIS Measures Explained',
    category: 'domain-knowledge',
    tags: ['hedis', 'ncqa', 'quality-measures', 'performance'],
    roles: [],
    summary:
      'Comprehensive guide to HEDIS quality measures and their use in healthcare quality assessment.',
    content: `# HEDIS Measures Explained

HEDIS (Healthcare Effectiveness Data and Information Set) is a tool used by health plans to measure performance on important dimensions of care and service.

## What is HEDIS?

**Developed by**: National Committee for Quality Assurance (NCQA)

**Purpose**: Standardize quality measurement across health plans

**Use**: Compare health plan performance, quality improvement, value-based payment

## HEDIS Domains

### Effectiveness of Care
- Prevention and screening
- Respiratory conditions
- Cardiovascular conditions
- Diabetes care
- Musculoskeletal conditions
- Behavioral health

### Access/Availability of Care
- Adults' access to preventive/ambulatory health services
- Children and adolescents' access to care
- Initiation and engagement of treatment

### Experience of Care
- CAHPS (Consumer Assessment of Healthcare Providers and Systems)
- Member satisfaction surveys

### Utilization
- Inpatient utilization
- Antibiotic utilization
- Opioid utilization

### Relative Resource Use
- Cost measures
- Resource utilization

## Common HEDIS Measures in Clinical Portal

### HBD: Hemoglobin A1c Testing for Diabetics
- **Domain**: Diabetes Care
- **Description**: % of diabetics 18-75 who had HbA1c test
- **Measurement Period**: Calendar year
- **Target**: >90%

### CBP: Controlling High Blood Pressure
- **Domain**: Cardiovascular Care
- **Description**: % of adults 18-85 with hypertension whose BP is <140/90
- **Measurement Period**: Calendar year
- **Target**: >70%

### CCS: Cervical Cancer Screening
- **Domain**: Prevention and Screening
- **Description**: % of women 21-64 who were screened for cervical cancer
- **Measurement Period**: 3 years
- **Target**: >80%

### COL: Colorectal Cancer Screening
- **Domain**: Prevention and Screening
- **Description**: % of adults 50-75 who had appropriate screening
- **Measurement Period**: Varies by test type
- **Target**: >75%

### BCS: Breast Cancer Screening
- **Domain**: Prevention and Screening
- **Description**: % of women 50-74 who had mammogram in last 2 years
- **Measurement Period**: 2 years
- **Target**: >75%

## HEDIS Measurement Year

- **Runs**: January 1 - December 31
- **Reporting**: Data submitted following measurement year
- **Audits**: Random sample audited by NCQA

## HEDIS Data Sources

### Administrative Data
- Claims data
- Enrollment data
- Pharmacy data

### Medical Record Review (MRR)
- Chart abstraction
- Hybrid method (combines administrative + MRR)

### Supplemental Data
- Lab results
- Biometric data
- Patient-reported data

## HEDIS Star Ratings

Health plans receive star ratings (1-5) for each measure:
- **5 Stars**: 90th percentile or above
- **4 Stars**: 75th-90th percentile
- **3 Stars**: 50th-75th percentile
- **2 Stars**: 25th-50th percentile
- **1 Star**: Below 25th percentile

## HEDIS vs. Other Measures

**HEDIS vs. CMS**:
- HEDIS: Commercial and Medicaid plans
- CMS: Medicare plans (but some overlap)

**HEDIS vs. MIPS**:
- HEDIS: Plan-level
- MIPS: Physician-level

**HEDIS vs. Core Measures**:
- HEDIS: Ambulatory/preventive care focus
- Core Measures: Hospital care focus

## Best Practices

1. **Understand Specifications**: Read measure technical specs
2. **Know Data Sources**: Administrative vs. medical record
3. **Document Thoroughly**: Ensure all care is documented
4. **Close Gaps Early**: Don't wait until end of year
5. **Track Performance**: Monitor rates throughout year
6. **Focus on Hybrid**: Medical record review can improve rates
`,
    relatedArticles: [
      'cms-measures-explained',
      'evaluations-guide',
      'understanding-quality-scores',
    ],
    lastUpdated: new Date('2025-11-19'),
    views: 0,
    estimatedReadTime: 8,
  },

  {
    id: 'cql-language-guide',
    title: 'Clinical Quality Language (CQL) Guide',
    category: 'domain-knowledge',
    tags: ['cql', 'quality-measures', 'hl7', 'coding'],
    roles: ['quality-manager', 'admin'],
    summary:
      'Learn the fundamentals of Clinical Quality Language for writing quality measure logic.',
    content: `# Clinical Quality Language (CQL) Guide

CQL is a high-level, domain-specific language focused on clinical quality applications.

## What is CQL?

**Full Name**: Clinical Quality Language
**Developed by**: HL7 (Health Level Seven International)
**Purpose**: Express clinical knowledge in a computable format
**Use Cases**: Quality measures, clinical decision support, cohort definitions

## CQL Basics

### Structure

Every CQL library has:
1. **Header**: Library name and version
2. **Using**: FHIR/QDM/QICore version
3. **Includes**: Referenced libraries
4. **Value Sets**: Code sets
5. **Parameters**: Input values
6. **Definitions**: Logic expressions

### Example Library

\`\`\`cql
library DiabetesQuality version '1.0.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

valueset "Diabetes": 'http://example.org/diabetes'

parameter "Measurement Period" Interval<DateTime>

define "Has Diabetes":
  exists([Condition: "Diabetes"])
\`\`\`

## Data Types

### Primitives
- **Boolean**: true, false
- **Integer**: 1, 2, 3
- **Decimal**: 1.5, 3.14
- **String**: 'text', "text"
- **DateTime**: @2025-11-19T10:30:00
- **Date**: @2025-11-19
- **Time**: @T10:30:00

### Structured Types
- **Interval**: Interval[@2025-01-01, @2025-12-31]
- **List**: {1, 2, 3}, {'a', 'b', 'c'}
- **Tuple**: Tuple{name: 'John', age: 30}
- **Code**: Code '4548-4' from "LOINC"
- **Quantity**: 5 'mg', 120 'mm[Hg]'

## Operators

### Comparison
- **Equality**: =, ~, !=
- **Ordering**: <, <=, >, >=
- **Null**: is null, is not null

### Logical
- **AND**: and
- **OR**: or
- **NOT**: not
- **XOR**: xor
- **IMPLIES**: implies

### Arithmetic
- **Basic**: +, -, *, /
- **Modulo**: mod
- **Power**: ^
- **Negation**: -x

### Date/Time
- **Duration**: years between, months between, days between
- **Comparison**: before, after, during, overlaps
- **Extraction**: year of, month of, day of

## FHIR Queries

### Basic Retrieve

\`\`\`cql
[Condition]  // All conditions
[Condition: "Diabetes"]  // Conditions with diabetes code
[Observation: "HbA1c"]  // HbA1c observations
\`\`\`

### With Where Clause

\`\`\`cql
[Condition: "Diabetes"] C
  where C.clinicalStatus ~ "active"
\`\`\`

### Multiple Conditions

\`\`\`cql
[Observation: "HbA1c"] O
  where O.effective during "Measurement Period"
    and O.status = 'final'
    and O.value > 9 '%'
\`\`\`

## Common Patterns

### Age Calculation

\`\`\`cql
define "Patient Age":
  AgeInYearsAt(start of "Measurement Period")

define "Adults":
  "Patient Age" >= 18
\`\`\`

### Existence Check

\`\`\`cql
define "Has Diabetes":
  exists([Condition: "Diabetes"] C
    where C.clinicalStatus ~ "active")
\`\`\`

### Most Recent

\`\`\`cql
define "Most Recent HbA1c":
  First(
    [Observation: "HbA1c"] O
      where O.effective during "Measurement Period"
      sort by effective desc
  )
\`\`\`

### Date Ranges

\`\`\`cql
define "Measurement Period":
  Interval[@2025-01-01T00:00:00, @2025-12-31T23:59:59]

define "Last Two Years":
  Interval[Today() - 2 years, Today()]
\`\`\`

### Union/Intersect

\`\`\`cql
define "All Diabetes Codes":
  [Condition: "Type 1 Diabetes"]
    union [Condition: "Type 2 Diabetes"]

define "Patients in Both":
  "Group A" intersect "Group B"
\`\`\`

## Functions

### Built-in Functions

**Lists:**
- Count(), Length(), First(), Last()
- Exists(), AllTrue(), AnyTrue()
- Distinct(), Flatten()

**Aggregates:**
- Sum(), Min(), Max(), Avg()

**Strings:**
- Combine(), Split(), Substring()
- StartsWith(), EndsWith(), Matches()

**Dates:**
- Today(), Now(), TimeOfDay()
- DurationInDays(), DurationInYears()

### Custom Functions

\`\`\`cql
define function "IsActive"(condition Condition):
  condition.clinicalStatus ~ "active"
    and condition.verificationStatus ~ "confirmed"

define "Active Conditions":
  [Condition] C where IsActive(C)
\`\`\`

## Best Practices

1. **Use Descriptive Names**: "Has Diabetes" not "X"
2. **Break Down Logic**: Multiple small definitions
3. **Comment Liberally**: Explain intent
4. **Reuse Definitions**: Don't repeat logic
5. **Handle Nulls**: Check for null/missing data
6. **Test Thoroughly**: Test edge cases
7. **Version Libraries**: Track changes

## Common Errors

**"Cannot resolve reference"**
- Definition used before declared
- Typo in name
- Missing include

**"Type mismatch"**
- Wrong data type
- Need type cast

**"Infinite recursion"**
- Definition references itself
- Circular dependencies

## Resources

- [CQL Specification](https://cql.hl7.org/)
- [CQL Author's Guide](https://cql.hl7.org/authorsguide.html)
- [FHIR CQL Documentation](https://hl7.org/fhir/cql.html)
`,
    relatedArticles: [
      'measure-builder-guide',
      'fhir-resources-explained',
      'testing-custom-measures',
    ],
    lastUpdated: new Date('2025-11-19'),
    views: 0,
    estimatedReadTime: 10,
  },

  {
    id: 'mpi-guide',
    title: 'Master Patient Index (MPI) Guide',
    category: 'domain-knowledge',
    tags: ['mpi', 'patient-matching', 'duplicates', 'data-quality'],
    roles: [],
    summary:
      'Understand how the Master Patient Index detects and merges duplicate patient records.',
    content: `# Master Patient Index (MPI) Guide

The Master Patient Index prevents duplicate patient records and links records for the same patient across systems.

## What is MPI?

**Purpose**: Ensure each real-world patient has exactly one record

**Functions**:
- Detect potential duplicates
- Link related records
- Merge duplicate records
- Maintain audit trail

## How Duplicate Detection Works

### Matching Algorithm

The MPI uses multiple data points:

1. **Name Matching** (40% weight)
   - Levenshtein distance algorithm
   - Handles typos and variations
   - Nicknames (Bob = Robert, Jim = James)

2. **Date of Birth** (30% weight)
   - Exact match required for high score
   - Transposed digits detected (01/15 vs 01/51)

3. **Address** (15% weight)
   - Street, city, zip comparison
   - Fuzzy matching for variations

4. **Phone** (10% weight)
   - Removes formatting
   - Compares digits only

5. **SSN** (5% weight)
   - If available, strong indicator
   - Partial match (last 4 digits)

### Similarity Scoring

**95-100%**: Almost certainly duplicate
- Flag for immediate review
- Prevent new record creation

**85-94%**: Likely duplicate
- Show warning to user
- Allow override with justification

**70-84%**: Possible duplicate
- Display in "potential matches"
- User decision required

**<70%**: Unlikely duplicate
- No warning shown
- Records remain separate

## Duplicate Prevention

### During Patient Creation

1. User enters patient data
2. System searches for similar records
3. If match found (>85%), shows warning:
   - "Potential duplicate found"
   - Side-by-side comparison
   - Options: Use existing, Create new, Merge

### Best Practices

**Always search first:**
- Search by name, DOB, MRN
- Review results carefully
- Use existing record if found

**Verify demographics:**
- Confirm spelling is correct
- Use legal name (not nickname)
- Verify DOB format (MM/DD/YYYY)

**Check MPI warnings:**
- Don't ignore duplicate alerts
- Review suggested matches
- Justify if creating anyway

## Reviewing Potential Duplicates

### Finding Duplicates

Navigate to: **Patients → Potential Duplicates**

List shows:
- Pair of potential duplicate records
- Similarity score
- Key demographic differences
- Date flagged

### Side-by-Side Comparison

Click "Review" to see:
- Name, DOB, gender
- Address, phone, email
- MRN, SSN (masked)
- Clinical data summary
- Creation date and source

### Making a Decision

**If Same Person:**
1. Select "Merge Records"
2. Choose primary record (data to keep)
3. Review merge plan
4. Confirm merge
5. System combines data

**If Different People:**
1. Select "Not a Match"
2. Provide reason
3. Records marked as reviewed
4. Won't be flagged again

**If Uncertain:**
1. Select "Review Later"
2. Assign to quality team
3. Investigate further
4. Make final decision

## Merging Records

### Merge Process

1. **Select Primary**: Record to keep
2. **Review Data**: What will be merged
3. **Confirm**: No undo available
4. **System Actions**:
   - Combines clinical data
   - Links encounters
   - Updates references
   - Maintains audit trail
   - Deactivates duplicate record

### What Gets Merged

**Demographics:**
- Primary record demographics kept
- Secondary record data archived

**Clinical Data:**
- All conditions combined
- All observations combined
- All medications combined
- All procedures combined
- All encounters linked to primary

**Evaluations:**
- All quality evaluations linked to primary
- Historical results preserved

**Documents:**
- All documents linked to primary
- No data loss

### Audit Trail

Every merge creates audit record:
- Date and time
- User who performed merge
- Records involved (primary + secondary)
- Data merged
- Reason for merge

## Preventing Duplicates

### Data Entry Standards

**Name Format:**
- Last name, First name, Middle
- No nicknames (use legal name)
- Capitalize properly
- No extra spaces

**DOB Format:**
- MM/DD/YYYY
- Verify with patient
- Check year (1965 vs 1956)

**Phone Format:**
- (XXX) XXX-XXXX
- No extensions in main field
- Verify digits

### Integration

**HL7 Feeds:**
- ADT messages checked against MPI
- Auto-link if high confidence match
- Flag for review if uncertain

**FHIR API:**
- POST /Patient validates against MPI
- Returns existing patient if duplicate
- Prevents programmatic duplicates

## Troubleshooting

**"Why wasn't duplicate detected?"**
- Different name spelling
- Different DOB
- Insufficient matching data
- Score below 85% threshold

**"False positive matches"**
- Common names
- Same DOB by coincidence
- Siblings in same household
- Mark as "Not a Match"

**"Can't merge records"**
- Records from different tenants
- Test vs. production data
- Contact administrator

## MPI Configuration

(Admin only)

**Matching Rules:**
- Adjust weights for data elements
- Set similarity thresholds
- Configure blocking criteria

**Auto-Merge:**
- Enable/disable automatic merging
- Set confidence threshold (95%+)
- Require manual review

**Performance:**
- Index optimization
- Background processing
- Batch duplicate detection

## Best Practices

1. **Search Before Create**: Always check for existing patient
2. **Use Consistent Format**: Standardize data entry
3. **Review Duplicates Weekly**: Don't let backlog grow
4. **Train Staff**: Ensure understanding of importance
5. **Monitor Quality**: Track duplicate rate over time
6. **Audit Regularly**: Review merged records monthly
`,
    relatedArticles: [
      'patients-guide',
      'data-quality-guide',
      'importing-patient-data',
    ],
    lastUpdated: new Date('2025-11-19'),
    views: 0,
    estimatedReadTime: 12,
  },

  // HOW-TO GUIDES
  {
    id: 'how-to-search-patients',
    title: 'How to Search for Patients Effectively',
    category: 'how-to',
    tags: ['patients', 'search', 'how-to', 'tutorial'],
    roles: [],
    summary:
      'Step-by-step guide to searching for patients using various methods and filters.',
    content: `# How to Search for Patients Effectively

Learn the most efficient ways to find patient records in the Clinical Portal.

## Quick Search (Most Common)

### By Name

1. Click in search box at top of Patients page
2. Type patient's name:
   - Last name only: "Smith"
   - First and last: "John Smith"
   - Last, first: "Smith, John"
3. Results appear as you type
4. Click patient to view details

**Tips:**
- Partial names work ("Joh" finds "John", "Johnny")
- Not case-sensitive
- Handles extra spaces
- Try different name orders if not found

### By MRN

1. Click in search box
2. Type MRN: "MRN-12345"
3. Exact match required
4. Results show immediately

**Tips:**
- Include any prefixes (MRN-, PT-, etc.)
- No partial match (must be complete MRN)
- Fastest search method if MRN known

### By Date of Birth

1. Click in search box
2. Type DOB in format: YYYY-MM-DD
3. Example: "1965-03-15"
4. Shows all patients with that DOB

**Tips:**
- Must use YYYY-MM-DD format
- Month and day must be 2 digits (03, not 3)
- May return multiple results if common DOB

### By Phone

1. Click in search box
2. Type phone number (any format):
   - (555) 123-4567
   - 555-123-4567
   - 5551234567
3. System removes formatting automatically

**Tips:**
- Numbers only are matched
- Formatting doesn't matter
- Partial numbers don't work (need full number)

## Advanced Search

Click "Advanced Filters" button for more options.

### Age Range

1. Select "Age Range" filter
2. Enter minimum age: "18"
3. Enter maximum age: "64"
4. Click "Apply"
5. Shows patients in that age range

**Use Cases:**
- Find adults for annual wellness visits
- Identify pediatric patients
- Age-specific quality measures

### Gender

1. Select "Gender" filter
2. Choose: Male, Female, Other, Unknown
3. Multiple selection allowed
4. Click "Apply"

**Use Cases:**
- Gender-specific screenings (mammography, cervical cancer)
- Population stratification
- Demographic analysis

### Location

1. Select "City" or "State" filter
2. Type or select location
3. Click "Apply"

**Use Cases:**
- Outreach campaigns by area
- Provider assignment by geography
- Regional quality reporting

### Insurance

1. Select "Insurance" filter
2. Choose payer:
   - Medicare
   - Medicaid
   - Commercial plans
   - Self-pay
3. Click "Apply"

**Use Cases:**
- Value-based contract populations
- Payer-specific quality measures
- Billing and eligibility verification

### Active Status

1. Toggle "Active Patients Only"
2. Excludes deceased and inactive patients
3. Default: ON (show active only)

**Use Cases:**
- Current patient panel
- Exclude deceased from quality measures
- Active care management lists

## Combining Filters

Use multiple filters for precise results:

**Example 1: Diabetic Adults**
- Age: 18-75
- Has Condition: Diabetes
- Active: Yes

**Example 2: Medicare Women 50+**
- Age: 50+
- Gender: Female
- Insurance: Medicare
- Active: Yes

**Example 3: Local Pediatric Patients**
- Age: 0-17
- City: Springfield
- Active: Yes

## Saving Search Criteria

### Create Saved Search

1. Configure filters
2. Click "Save Search"
3. Enter name: "Diabetic Adults 18-75"
4. Add description (optional)
5. Click "Save"

### Use Saved Search

1. Click "Saved Searches" dropdown
2. Select saved search
3. Filters applied automatically
4. Results displayed

### Manage Saved Searches

- Edit: Modify filters and save
- Rename: Change search name
- Delete: Remove unused searches
- Share: Make available to team (admin only)

## Export Search Results

1. Configure search/filters
2. Click "Export" button
3. Choose format:
   - CSV (Excel)
   - PDF (printable list)
   - FHIR Bundle (JSON)
4. Download file

**CSV Includes:**
- Name, MRN, DOB, Gender
- Address, Phone, Email
- Insurance, Status
- PCP, Last Visit

## Keyboard Shortcuts

- **F** or **/**: Focus search box
- **Enter**: Execute search
- **Esc**: Clear search
- **↓/↑**: Navigate results
- **Enter**: Open selected patient

## Troubleshooting

**"No results found"**
- Check spelling
- Try partial name
- Use different search term (DOB, MRN)
- Check if patient exists

**"Too many results"**
- Use more specific search ("John Smith" not just "Smith")
- Add filters (age, city, insurance)
- Use MRN for exact match

**"Search is slow"**
- Large result set (10,000+)
- Add filters to narrow results
- Use specific search terms
- Report to IT if consistently slow

## Best Practices

1. **Try Name First**: Quickest for most searches
2. **Use MRN When Available**: Fastest and most accurate
3. **Verify Results**: Confirm correct patient before proceeding
4. **Save Common Searches**: Reuse frequently used filters
5. **Export for Outreach**: Download lists for care gaps
6. **Check MPI Warnings**: Watch for potential duplicates
`,
    relatedArticles: [
      'patients-guide',
      'mpi-guide',
      'exporting-patient-lists',
    ],
    lastUpdated: new Date('2025-11-19'),
    views: 0,
    estimatedReadTime: 8,
  },

  {
    id: 'how-to-close-quality-gaps',
    title: 'How to Close Quality Gaps',
    category: 'how-to',
    tags: ['quality-gaps', 'improvement', 'workflow', 'how-to'],
    roles: [],
    summary:
      'Systematic approach to identifying and closing quality measure gaps.',
    content: `# How to Close Quality Gaps

A step-by-step workflow for improving quality measure performance.

## Step 1: Identify Gaps

### Run Population Evaluation

1. Navigate to **Evaluations** page
2. Click "Batch Evaluation"
3. Select:
   - All patients or filtered cohort
   - Measures to evaluate
   - Current measurement period
4. Click "Run Batch"
5. Wait for completion (5-30 minutes)

### Review Results

1. Go to **Results** page
2. Filter by:
   - Result: "Not Met"
   - Measure: Select specific measure
3. Sort by: Priority or Patient Name
4. Export list for tracking

## Step 2: Prioritize Opportunities

### High-Priority Gaps

Focus on patients with:
- **Multiple gaps** across measures
- **Easy closures** (missing documentation only)
- **High impact** (preventive screenings)
- **Upcoming appointments** (opportunity to address)

### Prioritization Matrix

**High Priority / Easy:**
- Missing HbA1c test (order in office)
- Overdue mammogram (schedule)
- Missing BP reading (measure at visit)

**High Priority / Difficult:**
- Uncontrolled diabetes (lifestyle + medication)
- Smoking cessation (counseling + resources)
- Complex care coordination

**Low Priority:**
- Far from due date
- Patient non-compliant historically
- Low impact on overall scores

## Step 3: Determine Gap Type

### Data Gap (Documentation Missing)

**Signs:**
- Care was provided
- Not documented in EHR
- Old record in paper chart
- Done at outside facility

**Actions:**
- Review paper charts
- Request external records
- Contact patient to verify
- Enter missing data
- Re-evaluate

### Care Gap (Service Not Provided)

**Signs:**
- Test not done
- Screening overdue
- Medication not prescribed
- Follow-up not scheduled

**Actions:**
- Order test/screening
- Schedule appointment
- Prescribe medication
- Refer to specialist
- Document plan

### Patient Barriers

**Signs:**
- Transportation issues
- Financial barriers
- Language/cultural barriers
- Health literacy
- Non-compliance

**Actions:**
- Provide resources (ride services)
- Financial assistance programs
- Language services
- Patient education
- Motivational interviewing

## Step 4: Take Action

### For Data Gaps

**Import External Records:**
1. Request records from other providers
2. Review and extract relevant data
3. Enter into EHR system
4. Link to patient chart
5. Re-run evaluation

**Document Completed Care:**
1. Search patient chart for evidence
2. Enter into appropriate field:
   - Lab results → Observations
   - Procedures → Procedures
   - Diagnoses → Conditions
3. Include date and source
4. Re-run evaluation to confirm

### For Care Gaps

**Order Tests:**
1. Open patient chart
2. Click "Orders"
3. Select test (HbA1c, lipid panel, etc.)
4. Choose lab and priority
5. Print or send electronically
6. Schedule lab appointment
7. Track completion

**Schedule Screenings:**
1. Review screening due dates
2. Call patient to schedule
3. Provide patient education
4. Send reminder before appointment
5. Confirm completion
6. Enter results

**Prescribe Medications:**
1. Review current medications
2. Add/adjust as needed
3. Send to pharmacy
4. Document in chart
5. Schedule follow-up

## Step 5: Document & Re-evaluate

### Document Actions

In patient chart, note:
- What gap was identified
- Actions taken
- Date of service
- Follow-up plan
- Any barriers encountered

### Re-evaluate Patient

1. Navigate to **Evaluations**
2. Select patient
3. Choose measure
4. Click "Evaluate"
5. Verify result changed to "Met"
6. If still "Not Met", investigate further

### Track Progress

- Update gap tracking spreadsheet
- Note closure date
- Calculate closure rate
- Share success with team

## Step 6: Monitor and Follow-Up

### Set Reminders

For ongoing gaps:
- Schedule follow-up appointment
- Set reminder for test results
- Flag chart for next visit
- Add to care team task list

### Trend Analysis

Monthly, review:
- Total gaps identified
- Gaps closed
- Closure rate by measure
- Improvement over time
- Barriers encountered

## Workflow by Measure Type

### Preventive Screenings (Mammogram, Colonoscopy)

1. Identify overdue patients
2. Call to schedule
3. Provide education and prep instructions
4. Send reminders
5. Confirm completion
6. Enter results
7. Schedule next screening

### Lab Testing (HbA1c, Lipid Panel)

1. Identify patients needing test
2. Order test in system
3. Schedule lab appointment (or walk-in)
4. Track result completion
5. Review results with patient
6. Document in chart
7. Re-evaluate quality measure

### Chronic Disease Control (BP, HbA1c)

1. Review current status
2. Assess medication adherence
3. Adjust treatment plan
4. Provide lifestyle counseling
5. Schedule follow-up
6. Track progress over time
7. Document improvements

## Best Practices

1. **Systematic Approach**: Work through gap list methodically
2. **Start Easy**: Quick wins build momentum
3. **Batch Similar Gaps**: Call all overdue mammograms together
4. **Document Immediately**: Don't delay data entry
5. **Re-evaluate Promptly**: Confirm gap closure
6. **Address Barriers**: Help patients overcome obstacles
7. **Celebrate Progress**: Share success with team
8. **Monitor Trends**: Track month-over-month improvement

## Common Mistakes to Avoid

1. **Waiting Until Year-End**: Close gaps throughout year
2. **Focusing Only on Easy Cases**: Address all patients
3. **Poor Documentation**: Ensure data quality
4. **Not Re-evaluating**: Always confirm closure
5. **Ignoring Patient Barriers**: Address root causes
6. **No Follow-up**: Track completion of scheduled services

## Tools and Resources

- **Gap Reports**: Pre-built gap lists by measure
- **Patient Outreach Templates**: Letters and call scripts
- **Scheduling Tools**: Batch appointment scheduling
- **Care Coordination**: Task assignment and tracking
- **Analytics Dashboard**: Gap closure trending
`,
    relatedArticles: [
      'evaluations-guide',
      'results-guide',
      'gap-analysis-strategies',
    ],
    lastUpdated: new Date('2025-11-19'),
    views: 0,
    estimatedReadTime: 12,
  },

  // TROUBLESHOOTING
  {
    id: 'troubleshooting-evaluations',
    title: 'Troubleshooting Evaluation Errors',
    category: 'troubleshooting',
    tags: ['troubleshooting', 'errors', 'evaluations', 'debugging'],
    roles: [],
    summary:
      'Common evaluation errors and how to resolve them.',
    content: `# Troubleshooting Evaluation Errors

Solutions to common problems when running quality measure evaluations.

## "Patient not in initial population"

**Meaning**: Patient doesn't meet basic eligibility criteria for the measure.

**Common Causes:**
- Patient outside age range
- Missing required diagnosis
- Not enrolled during measurement period
- Doesn't meet inclusion criteria

**How to Fix:**
1. Review measure specifications
2. Check patient age at start of measurement period
3. Verify required diagnoses are present
4. Confirm enrollment/eligibility dates
5. Check for exclusion criteria

**Example:**
Measure requires age 18-75, but patient is 17.
→ Wait until birthday or patient isn't eligible

## "Missing required data"

**Meaning**: Patient record lacks data needed for evaluation.

**Common Missing Data:**
- No diagnostic codes
- Missing lab results
- No procedure records
- No medication history

**How to Fix:**
1. Review what CQL is looking for
2. Check if data exists elsewhere (paper chart, external records)
3. Import/enter missing data
4. Re-run evaluation
5. If data doesn't exist, patient may not meet measure

**Example:**
Looking for HbA1c test, but none in system.
→ Check if test done elsewhere
→ Import result or order new test

## "Evaluation timeout"

**Meaning**: CQL evaluation took too long (>30 seconds default).

**Common Causes:**
- Patient has very large dataset (10+ years)
- Complex CQL with nested loops
- Inefficient value set queries
- System performance issues

**How to Fix:**
1. Check patient record size (# of observations, conditions)
2. Report to admin if consistently timing out
3. Admin can increase timeout limit
4. May need to optimize CQL (for custom measures)

**Workaround:**
- Try during off-peak hours
- Reduce measurement period if possible
- Contact support if problem persists

## "CQL execution error"

**Meaning**: Problem in the measure's CQL logic.

**Common Causes:**
- Undefined value set
- Type mismatch in expression
- Null reference error
- Invalid FHIR path

**How to Fix:**
1. Note the exact error message
2. Report to Quality Manager (for custom measures)
3. Use alternative measure if available
4. Admin may need to fix CQL

**Not User-Fixable**: This is a measure definition problem, not patient data problem.

## "Value set not found"

**Meaning**: Measure references a code set that doesn't exist in the system.

**Common Causes:**
- Value set not loaded in terminology service
- Incorrect value set OID/URL
- Typo in value set reference

**How to Fix:**
1. Report to administrator
2. Admin needs to load missing value set
3. Or fix value set reference in measure

**Workaround**: Use different measure if available.

## "No results returned"

**Meaning**: Evaluation ran but produced no result.

**Common Causes:**
- Patient has no clinical data
- Data is in wrong format
- Incorrect FHIR resource mapping
- System integration issue

**How to Fix:**
1. Verify patient has clinical data (conditions, observations, etc.)
2. Check that data is in FHIR format
3. Verify data source is working (EHR integration)
4. Contact support if issue persists

## "Permission denied"

**Meaning**: User doesn't have rights to evaluate this measure or patient.

**Common Causes:**
- Measure restricted to certain roles
- Patient in different organization/tenant
- User account permissions

**How to Fix:**
1. Contact administrator
2. Request necessary permissions
3. Verify you're in correct organization/tenant

## Batch Evaluation Issues

### "Batch job failed"

**Check:**
1. Go to batch monitor
2. View error logs
3. Look for patterns (all failing vs. some failing)

**Common Causes:**
- System overload
- Database connection lost
- Invalid measure configuration

**How to Fix:**
1. Retry batch with smaller population
2. Run during off-peak hours
3. Contact support with error logs

### "Some patients failed"

**Check:**
1. Download failed patient list
2. Review error messages
3. Identify common patterns

**Common Reasons:**
- Individual patient data issues
- Timeouts on complex patients
- Missing data for specific patients

**How to Fix:**
1. Evaluate failed patients individually
2. Fix data issues
3. Re-run batch with failed patients only

## Performance Issues

### "Evaluation is very slow"

**Typical Times:**
- Single patient: 2-5 seconds
- Batch 100 patients: 30-60 seconds
- Batch 1000 patients: 5-10 minutes

**If Slower:**
1. Check system status
2. Verify database performance
3. Check network connection
4. Report to IT if consistently slow

### "System freezing during evaluation"

**Immediate Actions:**
1. Wait 60 seconds
2. Refresh browser
3. Clear browser cache
4. Try different browser

**If Problem Persists:**
1. Check browser console for errors
2. Report to IT with error details
3. Try from different computer

## Getting Help

### Before Contacting Support

Gather this information:
1. Patient MRN (if single patient)
2. Measure name/ID
3. Exact error message
4. Screenshots
5. Steps to reproduce
6. When problem started

### Contact Methods

**IT Support:**
- System errors
- Performance issues
- Access problems

**Quality Manager:**
- Measure definition issues
- CQL errors
- Value set problems

**Help Desk:**
- General questions
- User account issues
- Training needs
`,
    relatedArticles: [
      'evaluations-guide',
      'cql-language-guide',
      'system-requirements',
    ],
    lastUpdated: new Date('2025-11-19'),
    views: 0,
    estimatedReadTime: 10,
  },

  // FAQ
  {
    id: 'faq-general',
    title: 'Frequently Asked Questions (FAQ)',
    category: 'faq',
    tags: ['faq', 'help', 'common-questions'],
    roles: [],
    summary:
      'Answers to frequently asked questions about the Clinical Portal.',
    content: `# Frequently Asked Questions (FAQ)

## General

**Q: What browsers are supported?**
A: Chrome, Firefox, Edge, Safari (latest 2 versions). Internet Explorer is not supported.

**Q: Can I use this on my phone or tablet?**
A: Yes, the portal is mobile-responsive. Some features work better on desktop.

**Q: How do I reset my password?**
A: Click "Forgot Password" on login page, or contact your administrator.

**Q: Can I access this from home?**
A: Depends on your organization's security policy. Contact IT.

**Q: How often is data updated?**
A: Real-time for manual entry. EHR integrations sync every 15-60 minutes.

## Patients

**Q: How do I add a new patient?**
A: Navigate to Patients page, click "Add Patient", fill out form. System checks for duplicates.

**Q: Can I merge duplicate patients?**
A: Yes, if you have permissions. Go to Patients → Potential Duplicates → Select pair → Merge.

**Q: How do I import patients from Excel?**
A: Use Patients → Import → Upload CSV. Download template first for correct format.

**Q: What if a patient's information is wrong?**
A: Click patient → Edit → Update information → Save. Changes are audited.

**Q: Can I delete a patient?**
A: No, patients can only be marked inactive. Contact administrator for data removal requests.

## Quality Measures

**Q: What's the difference between HEDIS and CMS measures?**
A: HEDIS is for commercial/Medicaid plans (NCQA). CMS is for Medicare (CMS). Some measures overlap.

**Q: How often should I run evaluations?**
A: At least monthly for gap closure tracking. Weekly for active quality improvement campaigns.

**Q: Can I create my own quality measures?**
A: Yes, if you have Quality Manager role. Use Measure Builder to create custom measures with CQL.

**Q: Why did a patient's result change from Met to Not Met?**
A: Measurement period changed, data was corrected, or measure specifications updated.

**Q: What does "Excluded" mean?**
A: Patient has valid clinical reason to be excluded from measure (e.g., hospice, advanced illness).

## Evaluations

**Q: How long does an evaluation take?**
A: Single patient: 2-5 seconds. Batch (100 patients): 30-60 seconds. Batch (1000+): 5-10 minutes.

**Q: Can I stop a batch evaluation?**
A: Yes, click "Cancel" in batch monitor. Already-processed patients will have results.

**Q: Why did my evaluation fail?**
A: Common reasons: missing data, patient not eligible, timeout, or system error. See error message for details.

**Q: Can I re-evaluate a patient?**
A: Yes, click "Re-evaluate" in Results page. Use after adding missing data or correcting errors.

**Q: Do evaluations cost anything?**
A: No, evaluations are included. Large batch jobs may have resource limits.

## Results & Reports

**Q: How do I export results?**
A: Click "Export" button, choose format (CSV/Excel/PDF), download file.

**Q: Can I schedule automatic reports?**
A: Yes, if administrator has enabled. Go to Reports → Schedule → Set frequency and recipients.

**Q: How long are results stored?**
A: Indefinitely. Historical results are preserved even if measure changes.

**Q: Can I share reports with patients?**
A: Depends on organizational policy. Patient-level reports can be printed or exported.

**Q: Why don't my numbers match the payer's report?**
A: Different measurement periods, data sources, or specification versions. Verify alignment with payer.

## Technical Issues

**Q: Page won't load or is blank**
A: Refresh browser (Ctrl+F5), clear cache, try different browser, contact IT if persists.

**Q: I got an error message**
A: Note exact message, screenshot if possible, contact Help Desk with details.

**Q: System is running slowly**
A: Check internet connection, try different browser, report to IT if widespread.

**Q: My changes aren't saving**
A: Check for error messages, verify you have edit permissions, try again after refreshing.

**Q: I can't see a patient/measure that should exist**
A: Check filters are not hiding it, verify you have permissions, contact administrator.

## Data & Security

**Q: Is my data secure?**
A: Yes. HIPAA-compliant encryption, role-based access control, audit logging.

**Q: Who can see patient data?**
A: Only users with appropriate permissions within your organization.

**Q: Is data backed up?**
A: Yes, automatic backups daily. Disaster recovery plan in place.

**Q: Can I download all my data?**
A: Yes, export functions available. Large exports may require administrator assistance.

**Q: How long do you keep my data?**
A: Per your organization's data retention policy, typically 7-10 years minimum.

## Training & Support

**Q: Is training available?**
A: Yes. Browse this Knowledge Base, watch video tutorials, or request live training from administrator.

**Q: Where can I learn CQL?**
A: See CQL Language Guide in Knowledge Base, HL7 CQL documentation, or request training.

**Q: How do I report a bug?**
A: Contact Help Desk with steps to reproduce, screenshots, and error messages.

**Q: Can I request a new feature?**
A: Yes, submit feature request to administrator or product team.

**Q: Who do I contact for help?**
- Technical issues: IT Support
- Quality measure questions: Quality Manager
- Training: Education team
- General questions: Help Desk
`,
    relatedArticles: [
      'welcome-to-clinical-portal',
      'troubleshooting-evaluations',
      'system-requirements',
    ],
    lastUpdated: new Date('2025-11-19'),
    views: 0,
    estimatedReadTime: 8,
  },
];
