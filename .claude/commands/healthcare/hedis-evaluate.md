---
name: hedis:evaluate
description: Evaluate HEDIS quality measures for patients with comprehensive reporting
category: healthcare
priority: high
---

# HEDIS Quality Measure Evaluation

Evaluate HEDIS (Healthcare Effectiveness Data and Information Set) quality measures for individual patients or populations.

## Usage

```bash
/hedis:evaluate [options]
```

## Options

- `--measure <code>` - HEDIS measure code (e.g., BCS, COL, CDC-HbA1c)
- `--patient <id>` - Evaluate for specific patient
- `--population <cohort>` - Evaluate for patient cohort
- `--date <YYYY-MM-DD>` - Evaluation date (default: today)
- `--report-format <json|pdf|csv>` - Output format
- `--include-evidence` - Include clinical evidence in report
- `--gap-analysis` - Show care gaps for non-compliant patients

## Examples

### Evaluate Single Patient

```bash
/hedis:evaluate --measure BCS --patient 123e4567-e89b-12d3-a456-426614174000
```

Evaluates Breast Cancer Screening (BCS) measure for specific patient.

### Evaluate Population

```bash
/hedis:evaluate --measure CDC-HbA1c --population diabetes-cohort --gap-analysis
```

Evaluates HbA1c control for diabetes cohort with care gap identification.

### Generate Compliance Report

```bash
/hedis:evaluate --measure COL --population all-eligible --report-format pdf
```

Generates PDF report for Colorectal Cancer Screening compliance.

## HDIM Implementation

This command uses the following HDIM services:

1. **Quality Measure Service** (`http://localhost:8087`)
   - Retrieves measure definitions
   - Executes CQL evaluation logic

2. **CQL Engine Service** (`http://localhost:8081`)
   - Runs Clinical Quality Language expressions
   - Returns measure results

3. **Patient Service** (`http://localhost:8084`)
   - Fetches patient demographics
   - Retrieves clinical data

4. **Care Gap Service** (`http://localhost:8086`)
   - Identifies care gaps for non-compliant patients
   - Generates intervention recommendations

## Workflow

```mermaid
graph TD
    A[/hedis:evaluate] --> B{Single or Population?}
    B -->|Single| C[Fetch Patient Data]
    B -->|Population| D[Fetch Cohort Patients]
    C --> E[Load Measure Definition]
    D --> E
    E --> F[Execute CQL Evaluation]
    F --> G{Compliant?}
    G -->|Yes| H[Generate Success Report]
    G -->|No| I[Identify Care Gaps]
    I --> J[Generate Gap Report]
    H --> K[Return Results]
    J --> K
```

## Output Format

### JSON Response

```json
{
  "measureCode": "BCS",
  "measureName": "Breast Cancer Screening",
  "evaluationDate": "2026-01-25",
  "patient": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "name": "Eleanor Anderson",
    "age": 63
  },
  "result": {
    "compliant": false,
    "numerator": false,
    "denominator": true,
    "evidence": [
      {
        "type": "Observation",
        "code": "LOINC:24604-1",
        "date": "2023-06-15",
        "value": "Negative"
      }
    ]
  },
  "careGap": {
    "gapType": "OVERDUE",
    "daysOverdue": 60,
    "recommendation": "Schedule mammogram screening within 30 days"
  }
}
```

## Supported HEDIS Measures

| Code | Measure Name | Population |
|------|--------------|------------|
| BCS | Breast Cancer Screening | Women 50-74 |
| COL | Colorectal Cancer Screening | Adults 50-75 |
| CDC-HbA1c | Diabetes: HbA1c Control | Adults with diabetes |
| CDC-BP | Diabetes: Blood Pressure Control | Adults with diabetes |
| CBP | Controlling High Blood Pressure | Adults with hypertension |
| CHL | Chlamydia Screening | Sexually active women 16-24 |

## HIPAA Compliance

- All patient data access is logged via `AuditService`
- PHI is filtered from console output
- Results include audit trail with user ID and timestamp
- Multi-tenant isolation enforced (`tenantId` filtering)

## Performance

- Single patient evaluation: ~200-500ms
- Population evaluation (100 patients): ~5-10 seconds
- Results cached for 5 minutes (HIPAA-compliant TTL)

## Error Handling

- `MEASURE_NOT_FOUND` - Invalid HEDIS measure code
- `PATIENT_NOT_FOUND` - Patient ID not in database
- `INSUFFICIENT_DATA` - Missing clinical data for evaluation
- `CQL_EVALUATION_ERROR` - CQL execution failure

## Related Commands

- `/care-gap:detect` - Detect all care gaps for patient
- `/quality-measure:run` - Run custom quality measures
- `/cql:evaluate` - Execute raw CQL expressions
- `/patient:search` - Find patients by criteria

## See Also

- [HEDIS Technical Specifications](http://www.ncqa.org/hedis)
- [Quality Measure Service API](../backend/modules/services/quality-measure-service/README.md)
- [CQL Engine Documentation](../backend/modules/services/cql-engine-service/README.md)
