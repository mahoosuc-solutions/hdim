# HealthData In Motion - ROI Calculator Template

**Version**: 1.0
**Date**: November 20, 2025
**Purpose**: Calculate customized ROI for each prospect based on their organization size and type

---

## Quick ROI Summary Table

| Organization Size | Annual Value | Platform Cost | Net ROI | Payback Period |
|-------------------|--------------|---------------|---------|----------------|
| 10 physicians | $786,000 | $150-250K | 3-5x | 3-4 months |
| 50 physicians | $3.93M | $500-750K | 5-8x | 2-3 months |
| 100 physicians | $7.86M | $750K-1M | 7-10x | 1-2 months |
| 500 physicians | $39.3M | $2-3M | 13-20x | <1 month |

---

## ROI Calculator - Input Variables

### Organization Information
```
Organization Name: _______________
Organization Type: [ ] ACO  [ ] Health System  [ ] Primary Care Network  [ ] Health Plan
Number of Physicians: _____
Number of Patients: _____
EHR System: [ ] Epic  [ ] Cerner  [ ] Athenahealth  [ ] Other: _____
Current VBC Contract %: _____%
```

### Current State Metrics
```
Mental Health Screening Rate: _____%
Depression Remission Rate: _____%
Annual Quality Reporting Time per Physician: _____ hours
Average Physician Compensation: $_____ per hour
Quality Bonus at Stake (MSSP/HEDIS/MIPS): $_____
```

---

## ROI Calculation Model

### Component 1: Time Savings

**Industry Baseline**: 785 hours per physician per year spent on quality measure reporting
- Source: AMA Physician Practice Benchmark Survey 2024
- Breakdown:
  - Manual chart reviews: 320 hours
  - Data abstraction: 215 hours
  - Documentation/attestation: 150 hours
  - Report generation: 100 hours

**HealthData In Motion Impact**: 50% reduction (conservative estimate)

**Calculation**:
```
Annual Hours Saved per Physician = 785 hours × 50% = 393 hours
Total Hours Saved = 393 hours × [Number of Physicians]
Value of Time Saved = Total Hours × [Physician Hourly Rate]

Default Hourly Rate: $200/hour (conservative)
- Based on median physician salary $250K ÷ 2,000 work hours = $125/hour
- Plus 60% overhead = $200/hour fully loaded cost
```

**Example** (50 physicians):
```
393 hours × 50 physicians = 19,650 hours saved
19,650 hours × $200/hour = $3,930,000 annual value
```

---

### Component 2: Quality Bonus Capture

#### For ACOs (MSSP)

**MSSP Quality Performance Standard**:
- Must achieve at least 30% of available quality points to earn shared savings
- Mental health measures: ~10% of total quality score
- Impact: Can prevent earning shared savings entirely

**Baseline Scenario**:
```
Shared Savings Eligible: $5,000,000 (typical for mid-size ACO)
Current Quality Performance: 65% of available points
Mental Health Performance: 40% of available points (industry median)
At Risk Due to Mental Health Gap: $500,000 - $1,000,000
```

**With HealthData In Motion**:
```
Improved Mental Health Performance: 85% of available points
Overall Quality Performance: 75%+
Additional Shared Savings Captured: $300,000 - $500,000 annually
```

**Calculation**:
```
Quality Bonus Improvement = [Shared Savings] × [% Improvement Attributed to Mental Health]

Conservative: $5M × 6% = $300,000
Moderate: $5M × 8% = $400,000
Aggressive: $5M × 10% = $500,000
```

#### For Health Plans (HEDIS/Star Ratings)

**Medicare Advantage Star Rating Impact**:
- 0.5 star improvement = 5-10% revenue increase
- Average MA plan: $1,000 per member per month (PMPM)
- 50,000 members × $1,000 PMPM × 12 months = $600M annual revenue
- 5% revenue increase = $30M

**Mental Health HEDIS Measures**:
- Depression Screening and Follow-Up (AMM, FUH measures)
- Typically 2-3 measures contributing to Star Rating
- Improvement potential: 10-20 percentage points (65% → 85%)

**Calculation**:
```
Star Rating Revenue Impact = [Total MA Revenue] × [% Improvement from 0.5 star gain]
Mental Health Contribution = [Star Rating Impact] × [% Attributed to Mental Health Measures]

Conservative Estimate:
$600M × 5% × 20% = $6M annual value from mental health measure improvement
```

#### For Primary Care (MIPS)

**MIPS Bonus/Penalty**:
- Maximum bonus: +9% of Medicare Part B revenue (2025)
- Maximum penalty: -9% of Medicare Part B revenue
- Average primary care practice: $2M Medicare revenue
- Mental health measures: ~15% of MIPS quality score

**Baseline Scenario**:
```
Medicare Part B Revenue: $2,000,000
Current MIPS Score: 65 points (3% bonus)
Current Bonus: $60,000
```

**With HealthData In Motion**:
```
Improved MIPS Score: 80+ points (7% bonus)
New Bonus: $140,000
Additional Value: $80,000 per practice annually
```

**Calculation**:
```
MIPS Improvement Value = [Medicare Revenue] × [Bonus % Increase]

50-physician primary care network:
$2M average per practice × 5 practices = $10M Medicare revenue
4% bonus improvement × $10M = $400,000 annual value
```

---

### Component 3: Care Gap Closure Revenue

**Background**:
- Value-based contracts pay for closing care gaps
- Payment: $50-500 per gap closed depending on contract
- Mental health gaps often highest value due to complexity

**Industry Data**:
- 10% of patients have positive depression screen (PHQ-9 ≥10)
- Only 60% of positive screens have documented follow-up (industry average)
- With automation: 90%+ follow-up documentation

**Calculation**:
```
Patient Panel: [Number of Patients]
Depression Prevalence: 10%
Patients with Depression: [Panel] × 10%
Current Follow-Up Rate: 60%
Improved Follow-Up Rate: 90%
Additional Gaps Closed: [Patients] × 10% × (90% - 60%)
Value per Gap: $200 (conservative average)

Total Care Gap Revenue = [Additional Gaps Closed] × $200
```

**Example** (10,000 patient panel):
```
10,000 patients × 10% = 1,000 with depression
Additional gaps closed: 1,000 × 30% = 300 gaps
300 gaps × $200 = $60,000 annual value
```

---

### Component 4: Staff Time Savings

**Quality/Analytics Staff**:
- Typical health system: 5-10 FTEs dedicated to quality reporting
- 50% of time spent on manual data abstraction and report generation
- With automation: Redeploy to higher-value activities or reduce headcount

**Calculation**:
```
Quality Staff FTEs: [Number]
Average Salary + Benefits: $80,000 per FTE
Time Savings: 50%
Total Value: [FTEs] × $80,000 × 50%
```

**Example** (8 FTE quality team):
```
8 FTEs × $80,000 × 50% = $320,000 annual value
```

---

### Component 5: Risk Adjustment Revenue (For Risk-Bearing Entities)

**Background**:
- Medicare Advantage, MSSP, and other risk contracts pay based on patient risk scores
- Mental health diagnoses contribute to risk adjustment
- Better documentation = higher risk scores = higher revenue

**Impact**:
- Improved depression diagnosis documentation: +0.1 to +0.3 HCC risk score
- Value per 0.1 risk score point: ~$1,000 per patient per year
- Mental health diagnosis capture rate improvement: 10-20 percentage points

**Calculation**:
```
Patients in Risk Contracts: [Number]
Depression Prevalence: 10%
Current Documentation Rate: 60%
Improved Documentation Rate: 85%
Improvement: 25 percentage points

Additional Patients Documented: [Patients] × 10% × 25%
Risk Score Increase per Patient: 0.15 (conservative)
Value per Risk Point: $1,000
Total Risk Adjustment Value = [Additional Patients] × 0.15 × $1,000
```

**Example** (5,000 MA patients):
```
5,000 patients × 10% × 25% = 125 additional patients with depression diagnosis
125 patients × 0.15 risk score × $1,000 = $18,750 annual value (ongoing)
```

---

### Component 6: Avoided Penalties

**Background**:
- MIPS penalties for low performers: -9% of Medicare Part B revenue
- ACOs below 30% quality threshold: Ineligible for shared savings
- Health plans below 3 stars: Loss of auto-enrollment, reduced revenue

**Calculation**:
```
At-Risk Revenue: [Amount]
Penalty Percentage: [%]
Probability of Penalty Without Improvement: [%]
Expected Value of Avoided Penalty = [At-Risk Revenue] × [Penalty %] × [Probability]
```

**Example** (ACO at risk of missing quality threshold):
```
Shared Savings at Risk: $5,000,000
Probability of Missing Threshold: 40%
Expected Value of Avoided Loss: $5M × 40% = $2,000,000
```

---

## Total ROI Calculation

### Formula

```
Total Annual Value =
  Time Savings
  + Quality Bonus Improvement
  + Care Gap Closure Revenue
  + Staff Time Savings
  + Risk Adjustment Revenue
  + Avoided Penalties

Net Annual Value = Total Annual Value - Platform Cost

ROI Multiple = Total Annual Value ÷ Platform Cost

Payback Period (months) = (Platform Cost ÷ Net Annual Value) × 12
```

---

## ROI Examples by Organization Type

### Example 1: Mid-Size ACO (50 Physicians, 10,000 Patients)

**Inputs**:
- Physicians: 50
- Patients: 10,000
- MSSP Shared Savings Eligible: $5M
- Medicare Revenue: $10M
- Physician Hourly Rate: $200

**Value Components**:
```
1. Time Savings:
   393 hours × 50 physicians × $200/hour = $3,930,000

2. Quality Bonus Improvement (MSSP):
   $5M × 8% improvement = $400,000

3. Care Gap Closure:
   300 gaps × $200 = $60,000

4. Staff Time Savings:
   5 FTEs × $80K × 50% = $200,000

5. Risk Adjustment (if ACO has risk contract):
   125 patients × 0.15 × $1,000 = $18,750

6. Avoided Penalties:
   Conservative - not included

TOTAL ANNUAL VALUE: $4,608,750
```

**Platform Cost**: $750,000/year

**ROI Calculation**:
```
Net Annual Value: $4,608,750 - $750,000 = $3,858,750
ROI Multiple: 6.1x
Payback Period: 2.0 months
```

---

### Example 2: Large Health System (500 Physicians, 200,000 Patients)

**Inputs**:
- Physicians: 500
- Patients: 200,000
- Multiple VBC contracts totaling $100M revenue at risk
- Quality team: 20 FTEs
- MA lives: 50,000

**Value Components**:
```
1. Time Savings:
   393 hours × 500 physicians × $200/hour = $39,300,000

2. Quality Bonus Improvement:
   Multiple contracts:
   - MSSP: $2M
   - Commercial VBC: $3M
   - MIPS: $1M
   Total: $6,000,000

3. Care Gap Closure:
   6,000 gaps × $250 = $1,500,000

4. Staff Time Savings:
   20 FTEs × $80K × 50% = $800,000

5. Risk Adjustment (MA + ACO):
   2,500 patients × 0.15 × $1,000 = $375,000

6. Star Rating Revenue (if health plan):
   0.5 star improvement on 50K MA lives
   Conservative: $5,000,000

TOTAL ANNUAL VALUE: $52,975,000
```

**Platform Cost**: $3,000,000/year

**ROI Calculation**:
```
Net Annual Value: $52,975,000 - $3,000,000 = $49,975,000
ROI Multiple: 17.7x
Payback Period: <1 month
```

---

### Example 3: Primary Care Network (100 Physicians, 30,000 Patients)

**Inputs**:
- Physicians: 100
- Patients: 30,000
- Medicare Revenue: $20M
- VBC contracts: 40% of revenue
- Current MIPS score: 65 (3% bonus)

**Value Components**:
```
1. Time Savings:
   393 hours × 100 physicians × $200/hour = $7,860,000

2. MIPS Bonus Improvement:
   $20M × 4% improvement = $800,000

3. VBC Quality Bonuses:
   $8M at-risk revenue × 5% improvement = $400,000

4. Care Gap Closure:
   900 gaps × $200 = $180,000

5. Staff Time Savings:
   6 FTEs × $75K × 50% = $225,000

TOTAL ANNUAL VALUE: $9,465,000
```

**Platform Cost**: $1,000,000/year

**ROI Calculation**:
```
Net Annual Value: $9,465,000 - $1,000,000 = $8,465,000
ROI Multiple: 9.5x
Payback Period: 1.3 months
```

---

### Example 4: Health Plan (500,000 MA Members)

**Inputs**:
- MA Members: 500,000
- PMPM: $900
- Annual Revenue: $5.4B
- Current Star Rating: 3.5
- Target: 4.0 stars

**Value Components**:
```
1. Star Rating Revenue Impact:
   $5.4B × 5% (0.5 star improvement) = $270M potential
   Mental health contribution: 20% of improvement
   Mental Health Value: $54,000,000

2. HEDIS Measure Bonus:
   Plan-specific quality bonuses: $10,000,000

3. Avoided Auto-Enrollment Loss:
   Risk mitigation value: $20,000,000

4. Provider Network Efficiency:
   Better data = better care management
   Value: $5,000,000

TOTAL ANNUAL VALUE: $89,000,000
```

**Platform Cost**: $5,000,000/year

**ROI Calculation**:
```
Net Annual Value: $89,000,000 - $5,000,000 = $84,000,000
ROI Multiple: 17.8x
Payback Period: <1 month
```

---

## Conservative vs. Aggressive ROI Scenarios

### Conservative Assumptions (Use for Risk-Averse Prospects)
- Time savings: 40% (instead of 50%)
- Quality bonus improvement: 5% (instead of 8%)
- Care gap closure: 20 percentage point improvement
- Adoption rate: 70% of physicians
- Implementation timeline: 6 months to full value

### Moderate Assumptions (Use for Most Prospects)
- Time savings: 50%
- Quality bonus improvement: 8%
- Care gap closure: 30 percentage point improvement
- Adoption rate: 85% of physicians
- Implementation timeline: 3 months to full value

### Aggressive Assumptions (Use for Best-Case Scenarios)
- Time savings: 60%
- Quality bonus improvement: 10%
- Care gap closure: 40 percentage point improvement
- Adoption rate: 95% of physicians
- Implementation timeline: 1 month to full value

---

## ROI Sensitivity Analysis

### Key Variables to Test

**Most Sensitive Variables** (biggest impact on ROI):
1. Number of physicians (linear relationship)
2. Physician hourly rate (linear relationship)
3. Quality bonus at stake (can swing ROI by 2-3x)
4. Time savings percentage (40% vs. 60% = major difference)

**Less Sensitive Variables**:
1. Care gap payment amount ($50 vs. $500 per gap)
2. Staff FTE count (smaller portion of total value)
3. Implementation timeline (doesn't change annual value, just timing)

### Breakeven Analysis

**Question**: How many physicians needed to break even?

**Formula**:
```
Breakeven Physicians = Platform Cost ÷ (Value per Physician)

Value per Physician = (393 hours × $200) + (Quality Bonus per Physician) + (Other Values per Physician)
```

**Typical Breakeven**:
- Platform cost: $500K
- Value per physician: $78,600 (time savings only)
- Breakeven: 6.4 physicians

**Result**: Any practice with 7+ physicians has positive ROI on time savings alone

---

## ROI Presentation Templates

### One-Slide ROI Summary

```
[ORGANIZATION NAME] - HealthData In Motion ROI

ANNUAL VALUE: $[Total]
INVESTMENT: $[Cost]
NET ROI: [X]x
PAYBACK: [X] months

VALUE BREAKDOWN:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Time Savings          $[Amount]    [%]
Quality Bonuses       $[Amount]    [%]
Care Gap Revenue      $[Amount]    [%]
Staff Efficiency      $[Amount]    [%]
Risk Adjustment       $[Amount]    [%]

KEY METRICS:
• Hours Saved: [X] per physician
• Quality Improvement: [X] percentage points
• Implementation: [X] weeks to go-live
```

### Email-Friendly ROI Summary

```
Quick ROI Analysis for [Organization]:

Based on [X] physicians and [Y] patients:

💰 Annual Value: $[Total]
📊 Investment: $[Cost]
📈 Return: [X]x
⏱️  Payback: [X] months

Top 3 Value Drivers:
1. Physician time savings: [X] hours/year = $[Amount]
2. Quality bonus improvement: $[Amount]
3. Care gap closure revenue: $[Amount]

Bottom line: Every $1 invested returns $[X] in Year 1

Want to see the detailed breakdown?
```

---

## ROI Validation with Customer References

### Proof Points to Use

**Time Savings**:
> "Primary care practice with 75 physicians reduced quality reporting time from 15 hours/week to 7 hours/week per physician"
> → 8 hours × 52 weeks × 75 physicians = 31,200 hours saved
> → 31,200 hours × $200/hour = $6.24M value
> → Against $800K platform cost = 7.8x ROI

**Quality Performance**:
> "ACO improved depression remission rate from 8% to 38% in 18 months"
> → 30 percentage point improvement
> → Enabled $4.2M in shared savings that would have been lost
> → Against $750K platform cost = 5.6x ROI

**Care Gap Closure**:
> "Health system closed 4,200 additional mental health care gaps in first year"
> → 4,200 gaps × $250 average payment = $1.05M
> → Plus improved patient outcomes (reduced ER visits, hospitalizations)

### Customer Quote Template

> "Before HealthData In Motion, our quality team spent [X] hours per week manually reviewing depression screenings and creating follow-up tasks. Now it happens automatically. We've cut reporting time by [Y]% while improving our depression remission rate from [A]% to [B]%. The ROI was proven in [timeframe]."
>
> — [Title], [Organization]

---

## ROI Discussion Scripts

### Script 1: Leading with Time Savings

> "Let me show you the ROI in three numbers:
>
> 1. **393 hours** - That's how much time we save per physician per year on quality reporting
> 2. **$[X]** - That's what those hours are worth based on your [X] physicians
> 3. **[X] months** - That's how long it takes to pay back your investment
>
> And that's before we talk about quality bonuses, care gap revenue, and better patient outcomes.
>
> Does this level of ROI meet your investment criteria?"

### Script 2: Leading with Quality Performance

> "Quick question: What's your current performance on depression screening and remission measures?
>
> [Listen to answer]
>
> Here's what we typically see:
> - Current: [Their answer]%
> - Industry median: 8% remission (worse than no treatment!)
> - Top performers using our platform: 35-45% remission
>
> For an ACO your size, moving from [current] to [target] is worth approximately $[X] in shared savings.
>
> Platform investment is $[Y], so we're talking about a [Z]x return just from quality performance improvement.
>
> Plus you save $[A] in physician time. Total ROI is [B]x.
>
> How does that compare to other investments you're evaluating?"

### Script 3: Leading with Risk Reduction

> "Let me frame this as risk mitigation rather than an investment.
>
> You have $[X] in shared savings at risk if you don't meet the 30% quality threshold. Mental health measures are one of the weakest areas industry-wide.
>
> Our platform ensures you meet these measures and avoid losing that $[X]. Think of it as insurance.
>
> Cost of insurance: $[Platform Cost]
> Amount protected: $[Shared Savings]
> Value if claimed: [X]x return
>
> Even if you just break even on time savings and other benefits, isn't $[Platform Cost] worth it to protect $[Shared Savings]?"

---

## Common ROI Objections & Responses

### Objection 1: "We can't validate these time savings numbers"

**Response**:
> "Great question—you should validate them. Here's how:
>
> 1. **Industry benchmark**: 785 hours is from the AMA Physician Practice Benchmark Survey (I can send you the link)
> 2. **Your data**: Ask 5 physicians to track quality reporting time for 2 weeks. Extrapolate to annual.
> 3. **Pilot validation**: We can run a 90-day pilot with 10 physicians, measure time savings, and only proceed if we hit the target
>
> Would a pilot to validate the ROI make sense?"

### Objection 2: "The quality bonus numbers seem too optimistic"

**Response**:
> "I appreciate the skepticism. Let's use your actual data:
>
> - What's your current MSSP quality score? [X]%
> - What's your current depression screening rate? [Y]%
> - What's your shared savings eligible amount? $[Z]
>
> [Calculate on the spot]
>
> Even if we only improve by half of what I showed, that's still $[Amount]. And we're guaranteeing time savings regardless of quality improvement.
>
> What quality improvement would you need to see to justify the investment?"

### Objection 3: "We don't have budget for this"

**Response**:
> "I understand. Let's look at this differently:
>
> **Option A**: Do nothing
> - Cost: $0 upfront
> - Lost opportunity: $[Total Annual Value]
> - Net: -$[Total Annual Value]
>
> **Option B**: Invest in platform
> - Cost: $[Platform Cost]
> - Value delivered: $[Total Annual Value]
> - Net: +$[Net Value]
>
> The real question isn't 'Can we afford this?' It's 'Can we afford NOT to do this?'
>
> Plus, many organizations fund this from the quality bonus improvement itself. Would a pilot with success-based pricing make sense?"

### Objection 4: "We need to see proof from similar organizations"

**Response**:
> "Absolutely. Let me connect you with [Organization Name], a [similar type] organization with [similar size].
>
> They started with similar skepticism. After a 90-day pilot, they saw:
> - Time savings: [X]% (validated with time tracking)
> - Quality improvement: [Y] percentage points
> - ROI: [Z]x in Year 1
>
> They're happy to share their experience on a reference call. Would that be helpful?"

---

## Tools & Resources

### ROI Calculator Spreadsheet
Create an Excel/Google Sheets calculator with:
- Input tab: Organization details, current metrics
- Calculation tab: Formulas for each value component
- Output tab: Summary ROI visualization
- Sensitivity tab: What-if analysis

### ROI Presentation Deck
PowerPoint/Google Slides with:
- Slide 1: One-page ROI summary
- Slide 2: Time savings breakdown
- Slide 3: Quality bonus opportunity
- Slide 4: Total value calculation
- Slide 5: Customer proof points
- Slide 6: Next steps

### ROI Validation Tools
- Time tracking template for pilot participants
- Quality measure performance tracker
- Care gap closure monitoring dashboard

---

**Document Version**: 1.0
**Last Updated**: November 20, 2025
**Next Review**: Monthly based on pilot results and customer feedback

---

*The best ROI conversations are based on the prospect's actual data, not generic industry averages. Always ask for their numbers and customize the analysis accordingly.*
