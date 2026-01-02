# [Customer Name] - [Organization Type]

> [One-line description of this integration scenario]

## Profile

| Attribute | Value |
|-----------|-------|
| **Organization Type** | [Solo Practice / Small Practice / FQHC / CAH / ACO / Health System / IPA] |
| **Size** | [# providers, # sites, # patients] |
| **Location** | [Geographic description] |
| **EHR System(s)** | [List of EHRs in use] |
| **Quality Programs** | [MIPS, ACO REACH, UDS, etc.] |
| **IT Capabilities** | [None / Basic / Moderate / Advanced] |
| **HDIM Tier** | [Community / Professional / Enterprise / Enterprise Plus / Health System] |
| **Monthly Cost** | [$X/month] |

## Challenge

### Current State
[Describe how they currently manage quality reporting]

### Pain Points
- [ ] [Pain point 1]
- [ ] [Pain point 2]
- [ ] [Pain point 3]

### Why HDIM
[Why this customer chose HDIM over alternatives]

---

## Solution Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        [CUSTOMER NAME]                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  [DATA SOURCE 1]  ───►  [INTEGRATION METHOD]  ───►  HDIM Cloud     │
│                                                                     │
│  [DATA SOURCE 2]  ───►  [INTEGRATION METHOD]  ───►      │          │
│                                                          ▼          │
│                                              ┌───────────────────┐  │
│                                              │ Quality Dashboard │  │
│                                              │ • Measure scores  │  │
│                                              │ • Care gaps       │  │
│                                              │ • Patient lists   │  │
│                                              └───────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Integration Configuration

### Method: [CSV Upload / FHIR API / n8n Workflow / Private Cloud]

| Setting | Value |
|---------|-------|
| **Authentication** | [OAuth 2.0 / API Key / Basic Auth / N/A] |
| **Sync Frequency** | [Manual / Daily / Real-time] |
| **Data Format** | [FHIR R4 / CSV / HL7v2 via n8n] |
| **Direction** | [Inbound only / Bidirectional] |

### Data Sources Connected

| Source | Type | Data Elements | Sync Frequency |
|--------|------|---------------|----------------|
| [Source 1] | [EHR/Lab/Claims] | [Demographics, Conditions, etc.] | [Frequency] |
| [Source 2] | [EHR/Lab/Claims] | [Demographics, Conditions, etc.] | [Frequency] |

---

## Sample Data Payload

### [Describe what this payload represents]

```json
{
  // Reference: _shared/FHIR_PAYLOADS.md#[section]
  // Or include specific payload here
}
```

---

## Implementation Steps

### Day 1: Discovery
- [ ] [Step 1]
- [ ] [Step 2]

### Day 2-3: Configuration
- [ ] [Step 1]
- [ ] [Step 2]

### Day 4-5: Initial Sync
- [ ] [Step 1]
- [ ] [Step 2]

### Day 6-7: Validation
- [ ] [Step 1]
- [ ] [Step 2]

### Day 8: Go-Live
- [ ] [Step 1]
- [ ] [Step 2]

---

## Measures Enabled

### Relevant HEDIS Measures

| Measure | Code | Baseline | Target | Impact |
|---------|------|----------|--------|--------|
| [Measure 1] | [Code] | [%] | [%] | [$] |
| [Measure 2] | [Code] | [%] | [%] | [$] |

### Quality Program Alignment

| Program | Relevant Measures | Reporting Deadline |
|---------|-------------------|-------------------|
| [MIPS/ACO/UDS] | [Measures] | [Date] |

---

## Expected Outcomes

### Time Savings

| Task | Before HDIM | After HDIM | Savings |
|------|-------------|------------|---------|
| [Task 1] | [X hours/week] | [Y hours/week] | [Z hours/week] |
| [Task 2] | [X hours/week] | [Y hours/week] | [Z hours/week] |
| **Total** | | | **[Total] hours/week** |

### Quality Improvement

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| Quality Score | [X%] | [Y%] | +[Z%] |
| Care Gaps Closed | [X/month] | [Y/month] | +[Z%] |
| Patient Outreach | [X/month] | [Y/month] | +[Z%] |

### Financial Impact

| Category | Annual Value |
|----------|--------------|
| Quality bonus improvement | $[X] |
| Penalty avoidance | $[X] |
| Staff time savings | $[X] |
| **Total Annual Value** | **$[X]** |

### ROI Calculation

```
Annual Value:        $[X]
Annual HDIM Cost:    $[Y]
Net Annual Benefit:  $[X-Y]
ROI:                 [X/Y]x
Payback Period:      [Z] months
```

---

## Pricing Summary

| Component | Cost |
|-----------|------|
| Monthly subscription | $[X]/month |
| Integration setup (if applicable) | $[X] one-time |
| Applicable discounts | -$[X] ([Type] discount) |
| **Total Monthly** | **$[X]/month** |
| **Annual Cost** | **$[X]/year** |

---

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Data sync uptime | [X%] | HDIM monitoring |
| Quality score improvement | +[X%] | HDIM dashboard |
| Staff time reduction | [X%] | Customer survey |
| User satisfaction | [X/5] | NPS survey |

---

## Related Resources

- [Shared FHIR Payloads](../_shared/FHIR_PAYLOADS.md)
- [n8n Workflow Examples](../_shared/N8N_WORKFLOWS.md)
- [Implementation Checklist](../_templates/IMPLEMENTATION_CHECKLIST.md)
- [Measure Sets by Segment](../_shared/MEASURE_SETS.md)

---

*Last Updated: [Date]*
*Version: 1.0*
