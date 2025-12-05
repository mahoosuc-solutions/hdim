---
id: "product-measure-library"
title: "Quality Measure Library & CQL Specifications"
portalType: "product"
path: "product/02-architecture/measure-library.md"
category: "architecture"
subcategory: "measures"
tags: ["quality-measures", "HEDIS", "CMS-measures", "CQL", "clinical-quality-language"]
summary: "Complete quality measure library and CQL specifications for HealthData in Motion. Includes 50+ standard measures, custom measure creation, CQL logic specifications, and measure management."
estimatedReadTime: 10
difficulty: "advanced"
targetAudience: ["clinical-informaticist", "quality-officer", "measure-developer"]
owner: "Product Architecture"
reviewCycle: "quarterly"
status: "published"
version: "1.0"
seoKeywords: ["quality measures", "HEDIS", "CMS measures", "CQL", "measure calculation", "clinical quality"]
relatedDocuments: ["core-capabilities", "reporting-analytics", "data-model", "clinical-workflows"]
lastUpdated: "2025-12-01"
---

# Quality Measure Library & CQL Specifications

## Executive Summary

HealthData in Motion includes **50+ pre-configured quality measures** from HEDIS, CMS, and other standards bodies, plus a **measure builder** for custom measures. All measures use **CQL (Clinical Quality Language)** for standardized, executable logic.

**Measure Coverage**:
- 35+ HEDIS measures (healthcare performance)
- 20+ CMS measures (Medicare/Medicaid quality)
- 10+ custom measure templates (organizational metrics)
- 100% eCQM-compatible CQL specifications

## Standard Measure Categories

### Preventive Care (10 measures)
- Cancer screenings (breast, colorectal, cervical)
- Preventive care visits
- Immunizations (flu, pneumonia, other vaccines)
- Blood pressure screening
- Diabetes screening

### Chronic Disease Management (15 measures)
- Diabetes HbA1c control
- Hypertension control
- Hyperlipidemia management
- Asthma/COPD management
- Heart failure management
- CAD medication management

### Behavioral Health (8 measures)
- Depression screening and treatment
- Substance use disorder treatment
- Suicide risk assessment
- Antidepressant medication management
- Bipolar disorder management

### Care Coordination (7 measures)
- Medication reconciliation
- Transitional care follow-up
- Hospital readmission rates
- ED utilization
- Care plan documentation

### Patient Safety (5 measures)
- Drug interaction screening
- Duplicate therapy prevention
- Medication allergies identified
- Safety culture assessment
- Adverse event tracking

## HEDIS Measures Included

**Medical Care (32 measures)**:
- Diabetes HbA1c Control (<8%)
- Hypertension Control (<140/90)
- Hyperlipidemia - Statin Therapy
- CAD - Aspirin Use
- Asthma Controller Medication
- COPD Medication Use
- Osteoporosis Management
- Antidepressant Medication Management

**Behavioral Health (3 measures)**:
- Depression Screening
- Bipolar Disorder Management
- Substance Abuse Treatment

**Preventive Care (8 measures)**:
- Breast Cancer Screening
- Colorectal Cancer Screening
- Cervical Cancer Screening
- Chlamydia Screening
- Flu Vaccinations
- Pneumococcal Vaccinations

**Access/Timeliness (4 measures)**:
- Initiation of Alcohol Dependence Treatment
- Engagement with Alcohol Dependence Treatment
- Follow-up After Mental Health Crisis
- Follow-up After Hospitalization for Mental Illness

## CQL Logic Examples

### Simple Measure: Breast Cancer Screening

```cql
define "Denominator":
  AgeInYearsAt(end of "Measurement Period") >= 50
  and AgeInYearsAt(end of "Measurement Period") < 75
  and exists (InpatientEncounter)

define "Numerator":
  exists (
    ["DiagnosticReport": "Mammography"] Mammo
    where Mammo.date during "Measurement Period - 2 years"
  )
```

### Complex Measure: Diabetes HbA1c Control

```cql
define "Denominator":
  AgeInYearsAt(end of "Measurement Period") >= 18
  and exists (
    Conditions C
    where C.condition is in "Diabetes"
  )

define "Numerator":
  exists (
    ["Observation": "HbA1c"] HbA1c
    where HbA1c.value < 8
    and HbA1c.date during "Measurement Period - 1 year"
  )
```

## Measure Builder

**Features**:
- Drag-and-drop measure definition UI
- Pre-built clinical logic components
- CPT/HCPCS code selection
- Population definition and filtering
- Calculation logic (numerator, denominator, exclusions)
- Testing against sample populations
- Version control and deployment

**Measure Components**:
- Initial Population (all eligible patients)
- Denominator (subset requiring intervention)
- Numerator (those meeting criteria)
- Denominator Exclusions
- Denominator Exceptions
- Stratification

## Measure Customization

**Organization-Specific Measures**:
- Custom definitions for organizational goals
- Local clinical pathways
- Organizational quality initiatives
- Department-specific metrics
- Provider-level performance tracking

**Examples**:
- "% of diabetic patients with foot exam documented"
- "% of patients with mental health screening completed"
- "Average time from admission to first provider assessment"
- "% of care gaps closed within 30 days"

## Measure Calculation Engine

**Calculation Process**:
1. **Data Extraction**: Pull relevant patient data
2. **Population Filtering**: Apply denominator logic
3. **Criteria Evaluation**: Check if criteria met
4. **Stratification**: Break down by subpopulations
5. **Aggregation**: Calculate percentages and rates
6. **Trending**: Compare to prior periods
7. **Benchmarking**: Compare to standards

**Performance**: <2 seconds per patient, 1M patients in 33 hours

## Measure Management

### Measure Governance
- **Approval Process**: Define and approve new measures
- **Version Control**: Track measure changes over time
- **Documentation**: Clinical rationale and specifications
- **Maintenance**: Update for coding/standard changes
- **Deprecation**: Retire obsolete measures

### Measure Performance
- **Real-time Calculation**: Updated continuously
- **Trending**: Historical performance trending
- **Benchmarking**: Compare to internal/external standards
- **Drill-down**: Identify specific patients not meeting criteria
- **Stratification**: Performance by demographics, provider, department

## Measure Standards

**Standards Compliance**:
- eCQM (Electronic Clinical Quality Measure) compliant
- FHIR-based measure definitions
- CQL 1.3+ language
- NQF (National Quality Forum) alignment where applicable
- CMS Measures specifications

**Measurement Period**:
- Calendar year (Jan 1 - Dec 31)
- Rolling 12-month (last 365 days)
- Rolling measurement (continuous)

## Measure Reporting

### Measure Performance Reports
- Measure performance by month/quarter/year
- Trending over time
- Comparison to benchmarks
- Subpopulation analysis
- Geographic/departmental breakdown

### Quality Improvement Dashboards
- Measure performance scorecards
- Gap identification
- Improvement initiatives tracking
- Provider performance ranking
- Quality metric trending

## Measure Validation

### Quality Checks
- Face validity: Does measure make clinical sense?
- Construct validity: Does it measure what intended?
- Discriminant validity: Does it differentiate performance?
- Sensitivity/Specificity: Does it correctly identify cases?

### Data Quality Validation
- Denominator complete: All eligible patients identified
- Numerator accurate: Criteria correctly applied
- Exclusions appropriate: Exclusion logic valid
- No missing data: Required fields populated

## Conclusion

HealthData in Motion's comprehensive measure library and CQL specifications provide healthcare organizations with standardized, validated quality measurement capabilities. Whether using pre-built measures or creating custom measures, organizations can measure and improve clinical quality effectively.

**Next Steps**:
- See [Core Capabilities](core-capabilities.md) for feature overview
- Review [Reporting & Analytics](reporting-analytics.md) for measure reporting
- Check [Data Model](data-model.md) for data specifications
