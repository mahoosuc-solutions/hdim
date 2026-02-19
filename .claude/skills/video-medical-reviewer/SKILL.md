---
name: video-medical-reviewer
description: Validates clinical accuracy, HEDIS compliance, and regulatory safety of HDIM Remotion marketing video content. Reviews every text field in a RoleStoryConfig for medical plausibility, correct terminology, accurate statistics, and HIPAA-safe marketing patterns. Use when reviewing video content before rendering.
---

# Video Medical Reviewer

## What This Skill Does

Reviews a complete `RoleStoryConfig` for clinical accuracy, regulatory compliance, and healthcare domain correctness. Every text field is validated against real HEDIS specifications, CMS definitions, and healthcare workflow realities.

**This is a quality gate.** Content cannot proceed to visual design or rendering until all fields pass review.

## When This Skill Activates

- "Review the [role] video for medical accuracy"
- "Validate the clinical claims in [role] config"
- "Check HEDIS codes in the video content"
- When invoked by the `/video:render-role-video` command (Agent 3 in the pipeline)

## Review Checklist

For EVERY text field in the config, validate against these 7 checks:

### 1. HEDIS Measure Accuracy

Verify that all HEDIS measure codes reference real CMS/NCQA measures:

| Code Used | Must Match | Population |
|---|---|---|
| BCS / BCS-E | Breast Cancer Screening | Women 50-74 |
| CDC / HbA1c | Comprehensive Diabetes Care | Adults 18-75 with diabetes |
| CBP | Controlling High Blood Pressure | Adults 18-85 |
| COL | Colorectal Cancer Screening | Adults 45-75 |
| CIS | Childhood Immunization Status | Children turning 2 |
| WCV | Well-Child Visits (first 30 months) | Children 0-30 months |
| AWC | Adolescent Well-Care Visits | Adolescents 12-21 |
| PPC | Prenatal and Postpartum Care | Pregnant women |
| FUM / FUH | Follow-Up After ED Visit / Hospitalization | All ages |
| CWP | Appropriate Testing for Pharyngitis | Children 3-17 |
| SPC | Statin Therapy — Cardiovascular | Adults with ASCVD |
| SPD | Statin Therapy — Diabetes | Adults with diabetes |

**Reject** if a measure code doesn't match a real HEDIS MY2024 measure.

### 2. Clinical Plausibility

Verify that patient scenarios make medical sense:

- Age ranges match screening guidelines (e.g., mammography 50-74 per USPSTF, colonoscopy 45-75)
- Conditions and medications are compatible (e.g., HbA1c is for diabetes, not heart disease)
- Lab values are realistic (HbA1c range: 4.0-14.0%, typical target <7.0% or <8.0%)
- Time intervals are plausible (a mammogram isn't "60 days overdue" if the patient is 25)
- Treatment recommendations align with clinical guidelines

### 3. Statistics Validation

Verify that all numeric claims are defensible:

| Claim Type | Validation Method |
|---|---|
| "X% of gaps take >Y days" | Must be calculable from typical care gap workflow timelines |
| "X% time savings" | Must be derivable from described workflow improvement |
| "$X million penalty" | Must match HHS OCR enforcement data ranges |
| "X patients/members" | Must be plausible for pilot scale (1,000-10,000 members) |
| "X% improvement" | Must be a range (e.g., "35-40%") unless citing a specific study |

**Accept ranges** like "35-40%" (honest). **Reject** false precision like "73.2%" without a primary source.

### 4. Terminology Correctness

Verify healthcare terms are used correctly:

| Term | Correct Usage | Common Error |
|---|---|---|
| Care gap | A specific measure where a patient is non-compliant | "Quality gap" (informal) |
| Measure | A HEDIS/CMS quality measure with defined numerator/denominator | "Metric" (ambiguous in healthcare context) |
| Star rating | CMS Medicare Advantage quality rating (1-5 stars) | "Quality score" (too vague) |
| QRDA | Quality Reporting Document Architecture (I=individual, III=aggregate) | "Quality report" (informal) |
| CQL | Clinical Quality Language (measure logic definition) | "Query language" (misleading) |
| Numerator / Denominator | Patients meeting criteria / Eligible population | "Pass/fail" (oversimplified) |
| Value-based care | Payment models tied to quality outcomes | "Pay for performance" (subset only) |

### 5. Regulatory Safety (HIPAA in Marketing)

Verify that marketing content does not create HIPAA exposure:

- **No real patient names in text overlays or captions** — use clinical descriptions ("mammography screening overdue") not "Eleanor Anderson"
- **No specific dates of birth or MRNs** in caption text
- **No facility names** that could identify real organizations
- **Acceptable:** Screenshot images from demo environment may show fictional patient names (Eleanor Anderson, Michael Chen). This is fine in screenshots — only TEXT overlays and captions must be HIPAA-safe.

### 6. CMS Alignment

Verify measure descriptions match current CMS definitions:

- HEDIS MY2024 specifications (current measurement year)
- ECDS transition timeline: 2025 pilot measures, 2026 expansion, 2030 target for full digital
- Star rating calculation references are accurate (Part C and Part D weights)
- Quality bonus payment descriptions match MA program rules

### 7. Workflow Realism

Verify that described actions match what each role actually does:

| Role | CAN Do | CANNOT Do |
|---|---|---|
| Care Manager | Close gaps, coordinate outreach, schedule screenings, track patient panels | Write CQL, configure FHIR endpoints, modify measure definitions |
| CMO | View dashboards, set quality priorities, approve strategies, present to board | Close individual gaps, run evaluations, configure systems |
| Quality Analyst | Run measure evaluations, generate QRDA reports, analyze trends, validate data | Make clinical decisions, close gaps, prescribe treatments |
| Provider | Order tests, prescribe medications, document encounters, review clinical summaries | Configure systems, run batch evaluations, manage other users |
| Data Analyst | Build population reports, analyze risk stratification, create visualizations | Make clinical decisions, close care gaps, manage patients |
| Admin | Configure tenants, manage users/roles, view audit logs, monitor system health | Run clinical evaluations, close gaps, make treatment decisions |
| AI User | Query AI assistant, review AI recommendations, accept/reject AI suggestions | Override clinical guidelines, bypass quality gates, auto-approve actions |

## Per-Field Review Output

For each text field, assign one status:

```
APPROVED — Clinically accurate, no changes needed
REVISED  — Accuracy issue found, replacement text provided
REJECTED — Fundamental inaccuracy, cannot be fixed with minor edit (return to clinical-writer)
```

**Output format:**

```markdown
## Medical Review: [Role] Video Config

### Summary
- Fields reviewed: [N]
- APPROVED: [N]
- REVISED: [N]
- REJECTED: [N]
- **Gate status:** PASS / FAIL (FAIL if any REJECTED remain)

### Field Reviews

| Field | Status | Note |
|---|---|---|
| `titleSlide.headline` | APPROVED | Question accurately reflects persona pain point |
| `problemSlide.metric` | REVISED | Changed "73%" to "70-75%" — range more defensible without primary source |
| `scenes[3].narrativeCaption` | REVISED | Removed patient name from caption text |
| ... | ... | ... |

### Revisions Applied
[For each REVISED field, show original and revised text with explanation]

### Rejection Details
[For each REJECTED field, explain why and what the clinical-writer should fix]
```

## Feedback Loop

If any field is `REJECTED`:
1. Return the entire config to `video-clinical-writer` with rejection details
2. Clinical writer revises ONLY the rejected fields
3. Medical reviewer re-reviews ONLY the revised fields
4. Repeat until all fields are `APPROVED` or `REVISED`

Maximum iterations: 3. If still failing after 3 rounds, flag for human review.

## Integration

- **Input from:** `video-clinical-writer` (complete config with all text fields)
- **Output to:** `video-visual-designer` (validated config, all text finalized)
- **Depends on:** `hdim-customer-scenarios` (workflow validation reference)
