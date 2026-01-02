---
id: "user-measure-builder"
title: "User Guide: Measure Builder Basics"
portalType: "user"
path: "user/guides/advanced/measure-builder.md"
category: "user-guide"
subcategory: "feature"
tags: ["measure-builder", "custom-measures", "metric-creation", "quality-measures"]
summary: "Create and test custom quality measures tailored to your organization's clinical and operational needs."
estimatedReadTime: 6
difficulty: "advanced"
targetAudience: ["quality-analyst", "administrator"]
prerequisites: ["quality-metrics", "generating-reports"]
relatedGuides: ["quality-metrics", "custom-reports"]
lastUpdated: "2025-12-02"
---

# Measure Builder Basics

The measure builder enables creation of custom quality measures specific to your organization's needs.

## Understanding Measures

### Measure Components
- **Name**: What's being measured
- **Description**: What it evaluates
- **Numerator**: Who achieved the metric (count)
- **Denominator**: Who's eligible (count)
- **Calculation**: Numerator/Denominator = %
- **Target**: What % is acceptable
- **Frequency**: How often measured (monthly, quarterly, annually)

### Types of Measures
- **Process**: Did we do the service? (% screened)
- **Outcome**: Did patient improve? (% with controlled BP)
- **Structural**: Do we have resources? (% with EHR)
- **Balancing**: Are there unintended consequences?

## Creating a Custom Measure

### Step 1: Define the Measure
1. Click **Measure Builder**
2. Click **Create New Measure**
3. Enter:
   - **Measure Name**: Clear, specific
   - **Description**: What it measures
   - **Measure Type**: Process, Outcome, etc.
   - **Frequency**: How often calculated

### Step 2: Define Numerator
"Who achieved the goal?"

1. Click **Define Numerator**
2. Select criteria:
   - **Patient condition**: Diabetes, hypertension, etc.
   - **Service completed**: Test, screening, education, etc.
   - **Result achieved**: BP <130/80, A1C <7%, etc.
   - **Timeframe**: Within what period?
3. Example: "Diabetic patients with A1C test performed within 12 months"

### Step 3: Define Denominator
"Who's eligible?"

1. Click **Define Denominator**
2. Select inclusion criteria:
   - **Condition**: Who has the condition?
   - **Active status**: Patient still active?
   - **Date range**: Who was active during measurement period?
   - **Exclusions**: Anyone to exclude? (palliative care, age)
3. Example: "All diabetic patients active during 12-month period"

### Step 4: Test Measure
1. Click **Test Measure**
2. System runs measure on sample data
3. Shows:
   - Number in numerator
   - Number in denominator
   - Resulting percentage
   - Patient list matching criteria
4. Verify results make sense

### Step 5: Publish Measure
1. Review measure definition
2. Set target percentage
3. Assign owner/lead
4. Click **Publish**
5. Measure appears in quality reporting

## Predefined Measures

### Accessing Predefined Measures
1. From Measure Builder, click **Predefined Measures**
2. Browse available measures by category
3. Understand measure definitions
4. Ensure understanding before using

### Using Predefined Measures
- Provides standardized measurement
- Comparable to industry benchmarks
- Already tested and validated
- Recommended for standard metrics
- Customization possible if needed

## Managing Measures

### Editing Measures
1. From Measure Builder, select measure
2. Click **Edit**
3. Change:
   - Numerator/denominator criteria
   - Target percentage
   - Frequency
   - Description
4. Click **Save**
5. Changes apply to future calculations

### Retiring Measures
1. Select measure
2. Click **Retire**
3. Measure no longer calculates
4. Historical data preserved
5. Useful when measure no longer relevant

### Copying Measures
1. Select existing measure
2. Click **Copy**
3. Modify for new use
4. Save as new measure
5. Faster than creating from scratch

## Measure Validation

### Testing Accuracy
Before publishing:
1. Run measure on full dataset
2. Manually verify sample of patients
3. Compare results to other data sources
4. Check for outliers
5. Validate numerator/denominator logic

### Common Issues
- **Too broad**: Measures too many patients
- **Too narrow**: Measures too few patients
- **Unclear**: Criteria ambiguous
- **Wrong result**: Logic error in definition
- **Inconsistent**: Different from other data sources

## Performance Monitoring

### Measuring Measure Quality
Track:
- Validity (measures what it intends)
- Reliability (consistent results)
- Responsiveness (changes with intervention)
- Feasibility (practical to measure)

### Adjusting Measures
If measure not working:
1. Analyze why (data issue? logic issue?)
2. Adjust criteria if needed
3. Re-test
4. Communicate changes to stakeholders
5. Document reason for changes

## Best Practices

### Measure Creation Excellence
1. ✅ Define clearly and specifically
2. ✅ Test thoroughly before publishing
3. ✅ Use standardized terminology
4. ✅ Align with clinical evidence
5. ✅ Start with simple measures
6. ✅ Validate against source data
7. ✅ Monitor for data quality
8. ✅ Update as needs change

### Common Mistakes
❌ Poorly defined numerator/denominator
❌ Not testing before publication
❌ Measures too complex
❌ Misaligned with organizational priorities
❌ Not shared with stakeholders
❌ Never reviewed or updated

## Advanced Features

### Stratification
Measure performance by:
- Provider/department
- Patient demographics
- Risk tier
- Insurance type
- Care team

### Benchmark Comparisons
Compare your measure to:
- Internal historical performance
- Peer organizations
- National benchmarks
- Industry standards

## Troubleshooting

### "Measure Result Doesn't Seem Right"
**Solution**: Verify numerator/denominator definition, test on sample, check data quality

### "Can't Create Measure With Specific Criteria"
**Solution**: May need advanced measure builder, contact analyst team

## See Also

- [Quality Metrics](../workflows/physician/quality-metrics.md)
- [Custom Reports](./custom-reports.md)
- [Generating Reports](../reports/generating-reports.md)

## Need Help?

**Support**: Quality analyst, analytics team

---

**Last Updated**: December 2, 2025
**Document Version**: 1.0
