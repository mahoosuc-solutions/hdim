# Risk Stratification & Analytics - Demo Script

**Scenario**: Population-Level Risk Analysis
**Duration**: 3-4 minutes
**Target Audience**: ACO Leadership, CFOs, Medical Directors
**Value Proposition**: Identify high-risk patients before costly events, optimize resource allocation

---

## Pre-Recording Setup

### System State
- [ ] Demo environment running
- [ ] Logged in as: `demo_analyst@acmehealth.com`
- [ ] Tenant: "Acme Health Plan"
- [ ] Demo mode enabled
- [ ] Scenario loaded: `./demo-cli load-scenario risk-stratification`
- [ ] Population: 5,000 patients with HCC scores

### Browser Setup
- [ ] URL: `http://localhost:4200/risk-stratification?demo=true`
- [ ] Resolution: 1920x1080

---

## Narration Script

### INTRO (0:00 - 0:30)

**[Screen: Risk Stratification Dashboard]**

> "Healthcare costs are concentrated in a small percentage of patients. HDIM's risk stratification helps you identify and proactively manage these high-risk patients before costly events occur."

---

## Step-by-Step Actions

### STEP 1: Population Risk Overview (0:30 - 1:00)

**Action**: View Risk Distribution Dashboard

**Narration**:
> "Here's our population risk distribution. Of our 5,000 patients, 10% are high-risk, 30% moderate, and 60% low-risk.
>
> That top 10% - just 500 patients - accounts for 55% of our total healthcare spend. These are the patients we need to manage proactively."

**Screen Elements**:
```
┌─────────────────────────────────────────────────────────────┐
│ POPULATION RISK STRATIFICATION          Total: 5,000 pts   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  HIGH RISK (HCC > 2.0)     ███████████              10%    │
│  500 patients              $12.4M annual spend      55%    │
│                                                             │
│  MODERATE RISK (1.0-2.0)   ████████████████████    30%    │
│  1,500 patients            $6.8M annual spend       30%    │
│                                                             │
│  LOW RISK (< 1.0)          ██████████████████████  60%    │
│  3,000 patients            $3.4M annual spend       15%    │
│                                                             │
└─────────────────────────────────────────────────────────────┘

Key Metrics:
• Average HCC Score: 1.24
• Predicted Annual Cost: $22.6M
• 30-Day Readmission Risk: 12.3%
• Cost Avoidance Opportunity: $4.5M
```

---

### STEP 2: Rising Risk Patients (1:00 - 1:30)

**Action**: Click "Rising Risk" tab

**Narration**:
> "Even more valuable is identifying patients whose risk is increasing. These 47 patients have seen their HCC scores rise more than 20% in the past 90 days.
>
> Early intervention here can prevent hospitalizations and ER visits."

**Key Metrics**:
- 47 patients with rising risk
- Average risk increase: 28%
- Top risk drivers: Uncontrolled diabetes, CHF exacerbations

---

### STEP 3: Drill Into High-Risk Cohort (1:30 - 2:30)

**Action**: Click "View High-Risk Patients"

**Narration**:
> "Let's look at our high-risk cohort. We can sort by HCC score, predicted cost, or care gap count.
>
> Notice Emma Johnson at the top - HCC of 3.2, predicted annual cost of $48,000, and 6 open care gaps. She's exactly the type of patient who benefits from intensive care management."

**Screen Elements**:
```
┌─────────────────────────────────────────────────────────────┐
│ HIGH RISK PATIENTS (500)                    Sort by: HCC ▼ │
├─────────────────────────────────────────────────────────────┤
│ Patient          HCC    Predicted Cost  Care Gaps  Status  │
│ ─────────────────────────────────────────────────────────── │
│ Emma Johnson     3.2    $48,000         6          ⚠ Alert │
│ Robert Williams  2.9    $42,000         4          Managed │
│ Patricia Davis   2.7    $38,500         5          New     │
│ James Brown      2.6    $36,000         3          Managed │
│ ...                                                         │
└─────────────────────────────────────────────────────────────┘
```

---

### STEP 4: Cost Avoidance Analysis (2:30 - 3:00)

**Action**: Click "Cost Avoidance" tab

**Narration**:
> "Our predictive model estimates $4.5 million in cost avoidance opportunity. By closing care gaps and managing these high-risk patients proactively, we can prevent an estimated 120 hospitalizations and 340 ER visits this year.
>
> That's real ROI for your value-based contracts."

**Key Metrics**:
- Potential cost avoidance: $4.5M
- Preventable hospitalizations: 120
- Preventable ER visits: 340
- ROI on care management investment: 3.2x

---

### STEP 5: Export & Action (3:00 - 3:30)

**Action**: Click "Create Care Campaign"

**Narration**:
> "Now we can take action. I'll create a targeted outreach campaign for these high-risk patients - assigning them to care managers, scheduling wellness visits, and enrolling them in chronic care programs.
>
> The system tracks outcomes so you can measure the impact of your interventions."

---

### CLOSING (3:30 - 4:00)

**Narration**:
> "HDIM's risk stratification transforms how you manage population health. You move from reactive care to proactive intervention, reducing costs while improving outcomes.
>
> Our customers report 15-25% reduction in hospitalizations and $200-400 PMPM savings for high-risk patients."

---

## Risk Model Details

### HCC Categories
| Score Range | Risk Level | % of Population | % of Spend |
|-------------|------------|-----------------|------------|
| < 1.0 | Low | 60% | 15% |
| 1.0 - 2.0 | Moderate | 30% | 30% |
| > 2.0 | High | 10% | 55% |

### Primary Risk Drivers
1. Diabetes with complications (E11.x)
2. CHF (I50.x)
3. COPD (J44.x)
4. CKD Stage 3-5 (N18.x)
5. Morbid obesity (E66.01)

---

## Performance Metrics

| Action | Target Time |
|--------|-------------|
| Population summary | < 2s |
| Risk drill-down | < 1s |
| Patient list load | < 1s |
| Cost analysis | < 2s |

---

**Last Updated**: January 2026
