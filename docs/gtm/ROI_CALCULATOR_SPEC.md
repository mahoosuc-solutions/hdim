# HDIM ROI Calculator Specification

## Overview

Interactive web-based tool for prospects to calculate their potential ROI from HDIM implementation.

---

## User Interface

### Input Section

#### Organization Profile

| Field | Type | Options/Validation |
|-------|------|-------------------|
| Organization Type | Dropdown | ACO, Health System, HIE, Payer, FQHC |
| Patient/Member Population | Number | 1,000 - 1,000,000 |
| Number of Practices/Facilities | Number | 1 - 500 |
| Number of EHR Systems | Number | 1 - 20 |
| State(s) of Operation | Multi-select | 50 states |

#### Current Quality Performance

| Field | Type | Range |
|-------|------|-------|
| Current Overall Quality Score | Slider | 0% - 100% (default: 70%) |
| Current Star Rating (if applicable) | Dropdown | 2.0 - 5.0 Stars |
| HEDIS Measures Tracked | Number | 0 - 52 |
| Manual Reporting Hours/Month | Number | 0 - 200 |

#### Quality Program Participation

| Field | Type | Options |
|-------|------|---------|
| MSSP/ACO REACH | Checkbox | Yes/No |
| Medicare Advantage | Checkbox | Yes/No |
| Commercial Quality Contracts | Checkbox | Yes/No |
| Medicaid Quality Programs | Checkbox | Yes/No |

#### Current Challenges (Multi-select)

- [ ] Manual quality reporting
- [ ] Multiple EHR systems
- [ ] Care gap identification
- [ ] Mental health screening
- [ ] Provider engagement
- [ ] Real-time visibility
- [ ] Quality bonus attainment

---

## Calculation Logic

### Quality Score Improvement

```javascript
// Base improvement by organization type
const baseImprovement = {
  'ACO': 0.25,          // 25% average improvement
  'Health System': 0.23,
  'HIE': 0.20,
  'Payer': 0.28,
  'FQHC': 0.22
};

// Adjustment factors
const ehrComplexityFactor = 1 + (numEHRs - 1) * 0.02;  // More EHRs = more improvement potential
const baselineGapFactor = (100 - currentScore) / 30;    // Lower baseline = more room to improve

// Projected improvement
const projectedImprovement = baseImprovement[orgType] * ehrComplexityFactor * baselineGapFactor;
const projectedScore = Math.min(currentScore * (1 + projectedImprovement), 95);
```

### Financial Value Calculations

#### Shared Savings (ACO)

```javascript
// Average shared savings per quality point improvement
const savingsPerPoint = {
  'small': 25000,     // < 10,000 attributed lives
  'medium': 75000,    // 10,000 - 50,000
  'large': 150000     // > 50,000
};

const qualityPointsGained = projectedScore - currentScore;
const sharedSavingsValue = qualityPointsGained * savingsPerPoint[sizeCategory];
```

#### Star Rating Bonus (Medicare Advantage)

```javascript
// Annual bonus per member by star rating
const starBonusPerMember = {
  '3.0': 0,
  '3.5': 0,
  '4.0': 850,
  '4.5': 1100,
  '5.0': 1350
};

// Project star rating improvement
const currentStarValue = starBonusPerMember[currentRating];
const projectedRating = calculateProjectedRating(currentRating, projectedScore);
const projectedStarValue = starBonusPerMember[projectedRating];

const starBonusImprovement = (projectedStarValue - currentStarValue) * memberCount;
```

#### Administrative Savings

```javascript
// Hours saved per month
const hoursReduction = manualHours * 0.67;  // 67% reduction in manual effort
const hourlyRate = 75;  // Blended rate for quality staff
const annualAdminSavings = hoursReduction * hourlyRate * 12;
```

#### Care Gap Closure Value

```javascript
// Value per closed care gap (varies by measure type)
const gapClosureValue = {
  'preventive': 85,
  'chronic': 125,
  'behavioral': 200
};

// Estimated gaps per patient
const gapsPerPatient = 0.3;
const totalGaps = patientCount * gapsPerPatient;
const closureRateImprovement = 0.35;  // 35% more gaps closed
const additionalClosures = totalGaps * closureRateImprovement;

const gapClosureROI = additionalClosures * weightedAverageGapValue;
```

---

## Output Section

### Summary Dashboard

```
┌─────────────────────────────────────────────────────────────┐
│                    YOUR HDIM ROI ESTIMATE                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  QUALITY IMPROVEMENT                                        │
│  ──────────────────                                         │
│  Current Score:        70%  →  Projected:  87%  (+24%)     │
│  Star Rating:         3.5★  →  Projected: 4.5★  (+1.0)     │
│                                                             │
│  FINANCIAL IMPACT (Annual)                                  │
│  ─────────────────────────                                  │
│  Quality Bonuses:                        $2,400,000         │
│  Administrative Savings:                   $180,000         │
│  Care Gap Closure Value:                   $450,000         │
│  ──────────────────────────────────────────────────         │
│  TOTAL ANNUAL VALUE:                     $3,030,000         │
│                                                             │
│  INVESTMENT                                                 │
│  ──────────                                                 │
│  HDIM Platform (Year 1):                   $48,000          │
│                                                             │
│  ROI METRICS                                                │
│  ───────────                                                │
│  Return on Investment:                      6,213%          │
│  Payback Period:                            6 days          │
│  Net Value (Year 1):                     $2,982,000         │
│  3-Year NPV:                             $8,700,000         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Detailed Breakdown

| Category | Year 1 | Year 2 | Year 3 | 3-Year Total |
|----------|--------|--------|--------|--------------|
| Quality Bonuses | $2,400,000 | $2,640,000 | $2,900,000 | $7,940,000 |
| Admin Savings | $180,000 | $180,000 | $180,000 | $540,000 |
| Gap Closure | $450,000 | $495,000 | $540,000 | $1,485,000 |
| **Total Value** | **$3,030,000** | **$3,315,000** | **$3,620,000** | **$9,965,000** |
| HDIM Investment | ($48,000) | ($30,000) | ($30,000) | ($108,000) |
| **Net Value** | **$2,982,000** | **$3,285,000** | **$3,590,000** | **$9,857,000** |

### Comparison Chart

Visual bar chart comparing:
- Current state costs/penalties
- HDIM investment
- Projected returns
- Net gain

---

## Technical Implementation

### Technology Stack

| Component | Technology |
|-----------|------------|
| Frontend | React or Vue.js |
| Styling | Tailwind CSS |
| Charts | Chart.js or Recharts |
| PDF Export | jsPDF |
| Hosting | Vercel or Netlify |

### API Integration

```javascript
// Optional: Send results to CRM
const submitToSalesforce = async (results, contact) => {
  await fetch('/api/roi-submission', {
    method: 'POST',
    body: JSON.stringify({
      organizationName: contact.company,
      email: contact.email,
      calculatedROI: results.roi,
      projectedValue: results.totalValue,
      timestamp: new Date().toISOString()
    })
  });
};
```

### Lead Capture

Before showing results, capture:
- Email address (required)
- Organization name (required)
- Name (optional)
- Phone (optional)
- "I want to schedule a demo" checkbox

---

## PDF Report Generation

### Report Contents

1. **Cover Page**
   - Organization name
   - Date generated
   - HDIM branding

2. **Executive Summary**
   - Key ROI metrics
   - Quality improvement projection
   - Investment recommendation

3. **Detailed Analysis**
   - Input assumptions
   - Calculation methodology
   - Year-over-year projections

4. **Comparison**
   - HDIM vs. status quo
   - HDIM vs. competitors (if applicable)

5. **Next Steps**
   - Schedule demo CTA
   - Contact information
   - Case study links

---

## Validation Rules

### Input Validation

| Field | Rule |
|-------|------|
| Patient Count | Must be positive integer |
| Quality Score | 0-100% |
| Hours | 0-200 |
| Facilities | 1-500 |

### Sanity Checks

- ROI > 10,000% triggers warning: "These results are exceptional. Schedule a call to validate assumptions."
- Negative ROI (impossible with current model, but safeguard)
- Unrealistic improvement projections capped at 95% quality score

---

## Analytics Tracking

### Events to Track

| Event | Data |
|-------|------|
| Calculator Opened | Source, UTM params |
| Inputs Changed | Field name, new value |
| Calculation Run | All inputs, results |
| PDF Downloaded | Email, org name |
| Demo Requested | Email, org name, phone |

### Conversion Funnel

1. Landing page visit
2. Calculator started
3. Inputs completed
4. Results viewed
5. PDF downloaded
6. Demo requested

---

## A/B Testing Opportunities

| Element | Variant A | Variant B |
|---------|-----------|-----------|
| CTA Button | "Calculate My ROI" | "See My Savings" |
| Results Display | Numbers first | Chart first |
| Lead Capture | Before results | After results |
| PDF Option | Automatic | On request |

---

## Deployment

### URL Structure
- Production: `https://roi.healthdata-in-motion.com`
- Staging: `https://roi-staging.healthdata-in-motion.com`

### Embed Options
- Standalone page
- iframe embed for website
- Modal popup trigger

### Performance Targets
- Time to interactive: < 2s
- Calculation time: < 100ms
- PDF generation: < 3s
