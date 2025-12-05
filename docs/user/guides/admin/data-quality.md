---
id: "user-data-quality"
title: "User Guide: Data Quality & Validation"
portalType: "user"
path: "user/guides/admin/data-quality.md"
category: "user-guide"
subcategory: "feature"
tags: ["data-quality", "validation", "data-integrity", "error-identification", "correction"]
summary: "Monitor data quality, identify errors, and implement corrections to ensure accurate patient records."
estimatedReadTime: 6
difficulty: "intermediate"
targetAudience: ["administrator", "quality-staff", "it-support"]
prerequisites: ["platform-navigation"]
relatedGuides: ["generating-reports", "clinical-documentation"]
lastUpdated: "2025-12-02"
---

# Data Quality & Validation

Accurate data is essential for clinical decisions, quality metrics, and regulatory compliance. This guide covers monitoring and improving data quality.

## Data Quality Dimensions

### Completeness
- Are required fields filled in?
- Is all relevant information documented?
- Missing required data identified

### Accuracy
- Is data correct?
- Does documentation match clinical findings?
- Are there obvious errors or inconsistencies?

### Consistency
- Is data consistent across records?
- Are definitions applied uniformly?
- No contradictions in same record?

### Timeliness
- Is data entered/updated promptly?
- Are encounters documented within required timeframe?
- Are results available when needed?

### Validity
- Does data conform to expected format?
- Are values within reasonable ranges?
- Does data match reference standards?

## Data Quality Dashboard

### Accessing Quality Metrics
1. Click **Admin** > **Data Quality**
2. View data quality dashboard showing:
   - Completeness percentage (% required fields filled)
   - Accuracy score (error detection)
   - Consistency score (no contradictions)
   - Timeliness (% documented within 24 hours)
   - Overall quality score (0-100)

### Interpreting Scores
- **90-100**: Excellent, minimal issues
- **80-89**: Good, some issues to address
- **70-79**: Fair, significant improvement needed
- **<70**: Poor, urgent attention required

## Identifying Data Quality Issues

### Automated Quality Checks
System runs automatic validations:
- **Missing required fields**: Alerts when required data missing
- **Out of range values**: Alerts if values seem wrong
  - Example: Age 200 years old
  - Example: Lab value extremely high/low
- **Duplicate records**: Identifies potential duplicate patients
- **Inconsistencies**: Flags contradictions
  - Example: Patient discharged but still hospitalized
  - Example: Medication allergy but taking that drug

### Quality Reports
1. From Data Quality, click **Quality Reports**
2. Generates comprehensive report of issues:
   - By severity (critical, major, minor)
   - By type (missing data, invalid values, etc.)
   - By timeframe (today, this week, this month)
   - By department/user
3. Shows trend (improving/declining)

### Manual Quality Review
1. Regular audits of random records
2. Check completeness of documentation
3. Verify accuracy of entries
4. Spot-check calculations
5. Document any issues found

## Addressing Data Quality Issues

### Missing Data
When required fields not filled:
1. Identify which fields missing
2. Contact appropriate user
3. Request completion
4. Verify correction entered
5. Re-run validation

**Prevention**: Require fields at time of documentation

### Inaccurate Data
When data appears wrong:
1. Verify with source (EHR, original order, patient)
2. If error confirmed, create correction
3. Document what was wrong and why
4. Re-run validation
5. Review for similar errors (pattern)

### Duplicate Records
When patient appears twice:
1. Verify both records are duplicates
2. Merge or deactivate duplicate
3. Consolidate history to primary record
4. Update patient assignment if needed
5. Verify integration complete

### Validation Rule Failures
When data violates rules:
1. Understand rule (is it appropriate?)
2. If rule error: Request rule adjustment
3. If data error: Correct data
4. Document exception if rule should be overridden
5. Track exceptions

## Data Correction Workflow

### Amendment Process
To correct documented errors:

1. **Identify error**: What's wrong?
2. **Access record**: Open patient chart
3. **Add amendment**:
   - Click **Add Amendment** on note
   - Explain correction
   - Provide accurate information
   - Document date/time
4. **Never delete**: Keep original for audit trail
5. **Document reason**: Why correction needed

### Bulk Data Corrections
For systematic issues (many records):

1. **Identify pattern**: What's the issue?
2. **Develop correction**: How to fix?
3. **Test on sample**: Verify approach works
4. **Apply to all**: Implement correction
5. **Validate**: Run quality checks
6. **Document**: Record all corrections made

## Data Integration Quality

### EHR Data Sync
If data comes from EHR:
1. Monitor sync frequency
2. Verify data arriving accurately
3. Check for missing data from EHR
4. Identify mapping issues
5. Escalate if sync broken

### Manual Data Entry Quality
For manually entered data:
1. Use consistent formats (dates, medication names, etc.)
2. Standardize terminology
3. Use pick-lists when available
4. Require specific data elements
5. Regular audits to catch errors

## Compliance and Auditing

### Regulatory Compliance
Data quality required for:
- **HIPAA**: Accurate patient records
- **HITECH**: Breach prevention
- **NCQA**: Quality measure accuracy
- **CMS**: Medicare/Medicaid requirements
- **State regulations**: Vary by state

### Audit Trails
System tracks:
- Who changed what data
- When changes made
- What previous value was
- Why it was changed (hopefully)

Essential for compliance audits.

### Compliance Reporting
1. Run compliance reports monthly
2. Show data quality metrics
3. Document corrections made
4. Track improvement
5. Report to leadership

## Best Practices

### Data Quality Excellence
1. ✅ Document completely and accurately
2. ✅ Use standard terminology
3. ✅ Follow data entry standards
4. ✅ Review data promptly (within 24 hours)
5. ✅ Correct errors immediately
6. ✅ Track corrections for patterns
7. ✅ Educate users on quality importance
8. ✅ Monitor trends

### User Education
- Train on data entry standards
- Explain why accuracy matters
- Show common errors
- Provide templates/examples
- Regular refresher training

## Troubleshooting

### "Quality Score Declining"
**Investigation**: What changed? New users? New workflow? System issue?
**Solution**: Identify cause, provide training, adjust processes

### "Many Data Validation Failures"
**Causes**: Rule too strict, misunderstanding, system error
**Solution**: Review rules, train users, adjust if needed

### "Duplicate Patient Records Not Merging"
**Solution**: Contact IT, may need manual merge, ensure no active orders

## See Also

- [Clinical Documentation](../workflows/clinical/clinical-docs.md)
- [Generating Reports](./generating-reports.md)

## Need Help?

**Support**: Data quality team, IT support, Administrator

---

**Last Updated**: December 2, 2025
**Document Version**: 1.0
