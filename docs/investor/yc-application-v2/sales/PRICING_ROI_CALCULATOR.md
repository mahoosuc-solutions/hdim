# HDIM Pricing & ROI Calculator

> Tools for calculating customer investment, savings, and return on investment.

---

## Quick ROI Summary by Segment

| Segment | HDIM Cost | Estimated Value | ROI | Payback |
|---------|-----------|-----------------|-----|---------|
| Solo Practice | $588/year | $15,000-25,000 | 25-42x | <1 month |
| Small Practice | $3,588/year | $50,000-100,000 | 14-28x | <1 month |
| FQHC (5 sites) | $9,588/year | $150,000-300,000 | 15-31x | <1 month |
| Small ACO | $11,988/year | $200,000-500,000 | 17-42x | <1 month |
| Mid-size ACO | $29,988/year | $500,000-1,500,000 | 17-50x | <1 month |

---

## Section 1: Pricing Calculator

### 1.1 Pricing Tiers

| Tier | Monthly (Annual Billing) | Monthly (Monthly Billing) | Patient Limit | Users |
|------|--------------------------|---------------------------|---------------|-------|
| **Community** | $49/month | $59/month | 2,500 | 5 |
| **Professional** | $299/month | $349/month | 15,000 | 25 |
| **Enterprise** | $999/month | $1,199/month | 75,000 | Unlimited |
| **Enterprise Plus** | $2,499/month | N/A (annual only) | 200,000 | Unlimited |
| **Health System** | Custom | Custom | Unlimited | Unlimited |

### 1.2 Tier Selection Guide

**Use this decision tree:**

```
How many patients in your panel?
│
├─ Under 2,500 → Community ($49/mo)
│
├─ 2,500 - 15,000 → Professional ($299/mo)
│
├─ 15,000 - 75,000 → Enterprise ($999/mo)
│
├─ 75,000 - 200,000 → Enterprise Plus ($2,499/mo)
│
└─ Over 200,000 → Health System (Custom)
```

### 1.3 Add-On Pricing

| Add-On | Price | When to Include |
|--------|-------|-----------------|
| **Additional users** (Community/Professional) | $10/user/month | Over tier limit |
| **Custom measures** | $500/measure one-time | Proprietary quality metrics |
| **Dedicated CSM** (below Enterprise) | $500/month | High-touch needs |
| **SMART on FHIR embed** | Included Enterprise+ | Epic/Cerner integration |
| **SSO/SAML** | Included Enterprise+ | Enterprise identity |
| **White-labeling** | $1,000/month | Partner/reseller |
| **Private deployment** | +50% base price | Data residency requirements |

### 1.4 Discount Programs

| Program | Discount | Eligibility |
|---------|----------|-------------|
| **FQHC Discount** | 15% | Federally Qualified Health Center |
| **Rural Discount** | 15% | CAH or rural designation |
| **Annual Prepay** | 17% | Pay annually upfront |
| **Non-Profit** | 10% | 501(c)(3) status |
| **Multi-Year** | 10-20% | 2-3 year commitment |

---

## Section 2: Value Drivers

### 2.1 Primary Value Categories

#### A. Quality Incentive Capture

**MIPS Bonus (Medicare)**
- Top performers earn 5-9% bonus on Medicare revenue
- Typical primary care practice: $200K-$500K Medicare revenue
- Potential bonus: $10,000-$45,000/year

**ACO Shared Savings**
- Quality score affects shared savings percentage
- Each 1% quality improvement = ~$50K-$500K additional savings (size-dependent)
- HDIM customers average 10-15% quality score improvement

**HEDIS/Star Ratings (Commercial)**
- Payers pay quality bonuses: $2-$10 PMPM
- 5,000 lives x $5 PMPM x 12 months = $300,000/year

#### B. Penalty Avoidance

**MIPS Penalty**
- Bottom performers: up to -9% Medicare payment reduction
- Typical primary care practice at risk: $18,000-$45,000/year

**Commercial Payer Penalties**
- Poor quality = contract non-renewal or rate reduction
- Estimated risk: 2-5% of payer revenue

#### C. Operational Efficiency

**Staff Time Savings**
| Activity | Manual Time | With HDIM | Savings |
|----------|-------------|-----------|---------|
| Quality reporting | 20 hrs/week | 3 hrs/week | 17 hrs/week |
| Care gap identification | 10 hrs/week | 0 (automated) | 10 hrs/week |
| Chart chase | 5 hrs/week | 1 hr/week | 4 hrs/week |
| **Total** | 35 hrs/week | 4 hrs/week | **31 hrs/week** |

At $25/hour: 31 hrs x $25 x 52 weeks = **$40,300/year**

#### D. Revenue from Closed Care Gaps

| Care Gap | Revenue per Closure | Annual Opportunity |
|----------|--------------------|--------------------|
| AWV (Annual Wellness Visit) | $175 | $175 x uncaptured visits |
| Diabetic eye exam | $50-100 | $75 x diabetics without exam |
| Colonoscopy | $500-1,500 | $1,000 x patients due |
| Breast cancer screening | $150-300 | $225 x women 50-74 |

Typical practice captures 10-20% more gaps with real-time data.

---

## Section 3: ROI Calculation Formulas

### 3.1 Core Formulas

```
Annual Investment = (Monthly Price × 12) + One-Time Fees

Annual Value = Quality Incentives + Penalty Avoidance + Staff Savings + Gap Revenue

Net Annual Benefit = Annual Value - Annual Investment

ROI = (Annual Value - Annual Investment) / Annual Investment × 100

Payback Period (months) = Annual Investment / (Annual Value / 12)
```

### 3.2 Simplified Estimates by Segment

**Conservative estimates (low end of ranges):**

| Input | Solo Practice | Small Practice | FQHC | Small ACO | Mid-size ACO |
|-------|---------------|----------------|------|-----------|--------------|
| **Patients** | 1,200 | 4,500 | 22,000 | 8,500 | 42,000 |
| **Providers** | 1 | 6 | 18 | 35 | 120 |
| **HDIM Tier** | Community | Professional | Enterprise | Enterprise | Enterprise+ |
| **Annual Cost** | $588 | $3,588 | $9,588 | $11,988 | $29,988 |

---

## Section 4: Segment-Specific ROI Examples

### 4.1 Solo Practice - Dr. Martinez Family Medicine

**Profile:**
- 1 physician, 1 MA, 1 office manager
- 1,200 patients
- $300K Medicare revenue
- Currently uses spreadsheets

**HDIM Investment:**
- Tier: Community
- Monthly: $49
- Annual: $588

**Value Calculation:**

| Value Driver | Calculation | Annual Value |
|--------------|-------------|--------------|
| MIPS Bonus | $300K × 5% improvement = $15K, but conservative estimate | $8,000 |
| Staff Time Savings | 10 hrs/week × $25/hr × 52 weeks | $13,000 |
| Care Gap Revenue | 50 additional AWVs × $175 | $8,750 |
| **Total Value** | | **$29,750** |

**ROI Summary:**
- Annual Investment: $588
- Annual Value: $29,750
- Net Benefit: $29,162
- **ROI: 50.6x (5,060%)**
- Payback: 10 days

---

### 4.2 Small Practice - Riverside Primary Care

**Profile:**
- 6 physicians
- 4,500 patients
- $1.5M Medicare revenue
- Currently using athenahealth basic reports

**HDIM Investment:**
- Tier: Professional
- Monthly: $299
- Annual: $3,588

**Value Calculation:**

| Value Driver | Calculation | Annual Value |
|--------------|-------------|--------------|
| MIPS Bonus | $1.5M × 5% score improvement → ~$30K bonus improvement | $25,000 |
| Commercial Quality Bonus | $3 PMPM × 2,000 commercial lives × 12 months | $72,000 |
| Staff Time Savings | 20 hrs/week × $25/hr × 52 weeks | $26,000 |
| Care Gap Revenue | 150 additional AWVs × $175 | $26,250 |
| **Total Value** | | **$149,250** |

**ROI Summary:**
- Annual Investment: $3,588
- Annual Value: $149,250
- Net Benefit: $145,662
- **ROI: 41.6x (4,160%)**
- Payback: 9 days

---

### 4.3 FQHC - Community Health Partners

**Profile:**
- 5 sites, 18 providers
- 22,000 patients
- UDS reporting required
- Multiple payer contracts with quality components

**HDIM Investment:**
- Tier: Enterprise with FQHC discount (15%)
- Monthly: $999 × 0.85 = $849
- Annual: $10,188

**Value Calculation:**

| Value Driver | Calculation | Annual Value |
|--------------|-------------|--------------|
| UDS Performance | Improved rankings → additional grant funding | $50,000 |
| Medicaid Quality | $4 PMPM × 15,000 Medicaid × 12 months | $720,000 |
| Staff Time Savings | 40 hrs/week × $25/hr × 52 weeks | $52,000 |
| Provider Productivity | 0.5 hrs/day × 18 providers × 250 days × $80/hr | $180,000 |
| **Total Value** | | **$1,002,000** |

**ROI Summary:**
- Annual Investment: $10,188
- Annual Value: $1,002,000
- Net Benefit: $991,812
- **ROI: 98.4x (9,840%)**
- Payback: 4 days

*Note: FQHC value is high due to significant Medicaid quality incentives*

---

### 4.4 Small ACO - Coastal Care Partners

**Profile:**
- 12 practices, 35 providers
- 8,500 attributed lives
- MSSP ACO
- Currently using manual tracking + consultant

**HDIM Investment:**
- Tier: Enterprise
- Monthly: $999
- Annual: $11,988

**Value Calculation:**

| Value Driver | Calculation | Annual Value |
|--------------|-------------|--------------|
| Shared Savings Impact | 5% quality improvement → $200K additional savings | $200,000 |
| Consultant Replacement | Eliminate $5K/month quality consultant | $60,000 |
| Staff Time Savings | 60 hrs/week across practices × $25/hr × 52 weeks | $78,000 |
| Care Gap Revenue | 300 additional AWVs × $175 | $52,500 |
| **Total Value** | | **$390,500** |

**ROI Summary:**
- Annual Investment: $11,988
- Annual Value: $390,500
- Net Benefit: $378,512
- **ROI: 32.6x (3,260%)**
- Payback: 11 days

---

### 4.5 Mid-size ACO - Metro Health Alliance

**Profile:**
- 45 practices, 120 providers
- 42,000 attributed lives
- MSSP ACO + commercial value-based contracts
- Currently using Arcadia ($50K/month)

**HDIM Investment:**
- Tier: Enterprise Plus
- Monthly: $2,499
- Annual: $29,988

**Value Calculation:**

| Value Driver | Calculation | Annual Value |
|--------------|-------------|--------------|
| Vendor Cost Savings | $50K/month (Arcadia) - $2.5K/month (HDIM) | $570,000 |
| Shared Savings Impact | 5% quality improvement → $800K additional savings | $800,000 |
| Real-time Care Gaps | 2,000 additional gaps closed × $100 avg | $200,000 |
| Staff Time Savings | 100 hrs/week × $30/hr × 52 weeks | $156,000 |
| **Total Value** | | **$1,726,000** |

**ROI Summary:**
- Annual Investment: $29,988
- Annual Value: $1,726,000
- Net Benefit: $1,696,012
- **ROI: 57.6x (5,760%)**
- Payback: 6 days

*Note: Replacing Arcadia drives massive savings*

---

## Section 5: ROI Worksheet Template

### Customer Information

| Field | Value |
|-------|-------|
| Organization Name | _________________ |
| Type | [ ] Practice [ ] FQHC [ ] ACO [ ] Hospital [ ] IPA |
| Number of Patients | _________________ |
| Number of Providers | _________________ |
| Medicare Revenue | $________________ |
| Commercial Lives | _________________ |
| Current Quality Solution | _________________ |
| Current Monthly Spend | $________________ |

### HDIM Investment

| Line Item | Calculation | Amount |
|-----------|-------------|--------|
| Recommended Tier | (based on patients) | _________________ |
| Monthly Price | | $______________ |
| Discount Applied | (FQHC/Rural/Prepay) | _______________% |
| Adjusted Monthly | | $______________ |
| Annual Investment | Monthly × 12 | $______________ |

### Value Calculation

| Value Driver | Calculation | Annual Value |
|--------------|-------------|--------------|
| MIPS/Quality Bonus | Medicare × ____% improvement | $______________ |
| Penalty Avoidance | (if at risk) | $______________ |
| Commercial Quality | PMPM × Lives × 12 | $______________ |
| Staff Time Savings | Hours × Rate × 52 | $______________ |
| Vendor Replacement | Current spend - HDIM | $______________ |
| Care Gap Revenue | Gaps × Revenue | $______________ |
| **Total Annual Value** | | **$______________** |

### ROI Summary

| Metric | Calculation | Result |
|--------|-------------|--------|
| Annual Investment | | $______________ |
| Annual Value | | $______________ |
| Net Annual Benefit | Value - Investment | $______________ |
| ROI | (Value - Investment) / Investment | ______________x |
| Payback Period | Investment / (Value / 12) | ______________ days |

---

## Section 6: Sensitivity Analysis

### Best Case / Base Case / Worst Case

**For a typical Professional tier customer ($3,588/year):**

| Scenario | Annual Value | ROI | Payback |
|----------|--------------|-----|---------|
| **Worst Case** (10% of estimates) | $15,000 | 4.2x | 88 days |
| **Base Case** (50% of estimates) | $75,000 | 20.9x | 18 days |
| **Best Case** (100% of estimates) | $150,000 | 41.8x | 9 days |

**Key insight:** Even at 10% of projected value, ROI is still 4.2x (320% return).

### Break-Even Analysis

| Tier | Annual Cost | Break-Even Value | Break-Even as % of Typical Value |
|------|-------------|------------------|----------------------------------|
| Community | $588 | $588 | 2% |
| Professional | $3,588 | $3,588 | 2-4% |
| Enterprise | $11,988 | $11,988 | 3-4% |
| Enterprise Plus | $29,988 | $29,988 | 2-3% |

**Customers only need to capture 2-4% of projected value to break even.**

---

## Section 7: Comparison to Alternatives

### HDIM vs. Current Vendors

| Vendor | Typical Annual Cost | HDIM Annual Cost | Annual Savings |
|--------|--------------------|--------------------|----------------|
| Arcadia | $600,000-$1,800,000 | $12,000-$30,000 | $570,000-$1,770,000 |
| Innovaccer | $360,000-$1,200,000 | $12,000-$30,000 | $348,000-$1,170,000 |
| Healthec | $480,000-$1,440,000 | $12,000-$30,000 | $468,000-$1,410,000 |
| Azara | $36,000-$180,000 | $3,600-$12,000 | $32,400-$168,000 |
| Consultant | $60,000-$120,000 | $3,600-$12,000 | $56,400-$108,000 |

### HDIM vs. Manual/Spreadsheets

| Cost Category | Manual Approach | With HDIM | Savings |
|---------------|-----------------|-----------|---------|
| Staff Time | $40,000-$80,000/year | $5,000/year | $35,000-$75,000 |
| Missed Quality Bonuses | $20,000-$100,000/year | Captured | $20,000-$100,000 |
| Errors/Rework | $5,000-$15,000/year | Minimal | $5,000-$15,000 |
| HDIM Cost | $0 | $588-$12,000 | ($588-$12,000) |
| **Net Savings** | | | **$59,412-$188,000** |

---

## Section 8: Presenting ROI to Stakeholders

### For CFO/Financial Decision Makers

**Lead with:**
1. Net annual savings (hard dollars)
2. Payback period (days, not months)
3. Comparison to current spend
4. Three-year total cost of ownership

**Example pitch:**
> "Your current quality infrastructure costs approximately $75,000/year in staff time and consultant fees. HDIM replaces that for $3,588/year—a 95% reduction. Payback is under 30 days. Over three years, you save $214,000."

### For CMO/Clinical Decision Makers

**Lead with:**
1. Quality score improvement projections
2. Care gaps closed per month
3. Provider time saved
4. Patient outcome improvements

**Example pitch:**
> "HDIM customers see average quality score improvements of 10-15% within the first year. Your providers will have real-time care gap alerts during every patient visit—no more batch reports from yesterday. That means more screenings completed, more chronic conditions controlled, better patient outcomes."

### For IT Decision Makers

**Lead with:**
1. Implementation timeline
2. Integration complexity
3. Maintenance requirements
4. Security/compliance

**Example pitch:**
> "Implementation is 2-4 weeks, not 18 months. We connect via FHIR APIs—your team provides credentials, we do the rest. There's no software to install, no maintenance, no upgrades to manage. We're SOC 2 Type II [in progress], HIPAA compliant, and handle all security."

---

## Section 9: Objection Handling (Price-Related)

### "It's too expensive"

**Response framework:**
1. Acknowledge the concern
2. Calculate the cost of NOT buying
3. Show ROI math
4. Offer trial

**Example:**
> "I understand budget is a concern. Let me show you the cost of your current approach. You mentioned your quality coordinator spends 20 hours/week on manual reporting—that's $26,000/year. HDIM costs $3,588/year and reduces that to 3 hours/week. You save over $20,000 net. Would you like to try it for 30 days to validate?"

### "We don't have budget this year"

**Response:**
> "Most of our customers don't have a 'quality software' budget either—they reallocate from existing line items. Where is your quality spend today? Consulting fees? Staff overtime? Training? HDIM replaces those costs, often at a fraction of the price."

### "We can do this ourselves for free"

**Response:**
> "You can—and it'll cost you $40,000-$80,000/year in staff time based on typical practices. Plus, you won't have real-time data, automated measure calculations, or regulatory-ready reports. HDIM costs less than one FTE and does all of that."

---

## Appendix: Spreadsheet Template

**Download:** [HDIM_ROI_Calculator.xlsx] *(to be created)*

**Tabs:**
1. Customer Profile (input data)
2. Pricing Calculator (auto-selects tier)
3. Value Calculator (by category)
4. ROI Summary (one-page output)
5. Comparison (vs. alternatives)
6. Sensitivity Analysis

**Features:**
- Auto-tier selection based on patient count
- Discount application (FQHC, rural, annual)
- Dynamic value estimates based on segment benchmarks
- One-page printable summary
- PDF export for proposals

---

*Last Updated: December 2025*
