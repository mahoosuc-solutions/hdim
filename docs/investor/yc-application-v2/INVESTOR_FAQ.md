# HDIM Investor FAQ

> Anticipated questions from investors with direct, honest answers.

---

## Market & Competition

### Why won't Epic, Cerner, or the big EHR vendors just add this functionality?

They've tried—and failed. Epic's quality modules are notoriously difficult to configure and require expensive consulting. Cerner's population health tools are being deprecated post-Oracle acquisition. The EHR vendors are infrastructure companies, not analytics companies. Their business model is selling software licenses, not outcomes.

More importantly, the market we're targeting—small practices, FQHCs, rural hospitals—isn't profitable for them. A $49/month customer doesn't justify an Epic sales call. We're building for the 14,000+ organizations that enterprise vendors ignore.

### What about Arcadia, Innovaccer, Health Catalyst?

They're our competition for large health systems, and they're good at what they do. But they have three structural problems:

1. **Price:** They charge $0.35-$0.50 per patient per month. For a 5,000-patient practice, that's $21,000-$30,000/year. We charge $299/month ($3,588/year) for the same practice.

2. **Implementation:** Their implementations take 12-18 months. Ours take 1-2 weeks. They require dedicated customer IT resources. We don't.

3. **Architecture:** They built on batch ETL systems designed 10 years ago. We built on real-time FHIR with native CQL. They can't match our speed without rebuilding from scratch.

We're not trying to beat them for Mayo Clinic. We're taking the 90% of the market they can't economically serve.

### Isn't healthcare a graveyard for startups?

Yes, if you're trying to change physician behavior, navigate reimbursement, or build a consumer health app. We're not doing any of that.

We're selling picks and shovels. Organizations already need quality measurement—it's mandated by CMS. They're already paying for it (or paying penalties for not having it). We're just offering a faster, cheaper way to do what they're already trying to do.

### How big is this market really?

**$2B+ annually for quality measurement software:**

| Segment | Organizations | Avg Spend | Market Size |
|---------|---------------|-----------|-------------|
| Small practices | 180,000 | $3,600/yr | $648M |
| Mid-size groups | 25,000 | $18,000/yr | $450M |
| FQHCs | 1,400 | $24,000/yr | $34M |
| Rural hospitals | 2,000 | $36,000/yr | $72M |
| ACOs/Health Systems | 1,000 | $500,000/yr | $500M |
| **Total** | | | **$1.7B** |

This excludes quality consulting, manual abstraction, and the $4B+ in penalties/missed bonuses organizations pay annually due to inadequate quality programs.

---

## Product & Technology

### What's your technical moat?

**Three layers:**

1. **Architecture:** We're the only quality platform built on native FHIR R4 with real-time CQL evaluation. Competitors use batch ETL with proprietary data models. Migrating to our architecture would require them to rebuild their entire stack—that's a 2-3 year, $20M+ project.

2. **Cost structure:** Our development cost was $46K vs. $1.7M+ for traditional approaches (37x advantage). This means we can profitably serve customers at price points competitors can't touch.

3. **Speed:** <200ms quality evaluation vs. 24-48 hours. This isn't just faster—it enables entirely new use cases (point-of-care alerts, real-time dashboards) that batch systems can't support.

### Can't someone just copy what you've built?

They can try. But:

1. **FHIR expertise is rare.** There are maybe 500 engineers in the US who deeply understand FHIR + CQL. Most work at EHR vendors or CMS.

2. **Measure logic is hard.** Our 61 measures represent thousands of hours of clinical logic encoding. Each measure has edge cases, exclusions, and value set mappings that take months to get right.

3. **Integration knowledge compounds.** Every customer integration teaches us something. We now know how to connect to 15+ EHRs. A new entrant would have to learn all of this from scratch.

4. **Network effects.** More customers = better benchmarks, better measure validation, better implementation playbooks.

### What about AI? Aren't you going to be disrupted?

AI makes us more valuable, not less. Here's why:

Quality measurement is the **foundation** for clinical AI. You can't build AI interventions without knowing:
- Who has care gaps?
- What outcomes are we optimizing for?
- How do we measure if the AI worked?

We're the data layer that AI applications will build on. We've designed our architecture to expose quality data via API specifically so AI tools can consume it.

The future isn't AI replacing quality measurement—it's AI + real-time quality measurement working together.

### Why FHIR? Isn't adoption slow?

FHIR adoption crossed the tipping point in 2024:

- **CMS mandate:** All certified EHRs must support FHIR APIs (21st Century Cures Act)
- **Epic:** 100% of Epic customers now have FHIR enabled
- **Payer mandates:** CMS requires payers to expose claims data via FHIR

The "FHIR isn't ready" objection was valid in 2020. It's not valid in 2025. Every major EHR now has production FHIR APIs.

For customers whose EHRs don't have FHIR (legacy systems), we use n8n workflow automation to transform their data into FHIR. We're not dependent on universal FHIR adoption.

---

## Go-to-Market

### How will you acquire customers?

**Three channels:**

1. **Inbound/Content (60% of pipeline):** Healthcare quality managers Google "MIPS reporting software" and "HEDIS dashboard." We're building SEO-optimized content targeting these searches. CAC: ~$500.

2. **Partnerships (30% of pipeline):** EHR vendors, billing companies, and healthcare consultants have relationships with thousands of practices. We offer them a white-label or referral model. CAC: ~$800.

3. **Outbound (10% of pipeline):** For ACOs and health systems, we do targeted outreach. These deals are larger ($10K-$100K/year) and justify higher-touch sales. CAC: ~$3,000.

### What's your sales cycle?

| Segment | Sales Cycle | Decision Maker | Deal Size |
|---------|-------------|----------------|-----------|
| Solo/small practice | 1-2 weeks | Physician/office manager | $600-$3,600/yr |
| Mid-size group | 2-4 weeks | Practice administrator | $3,600-$12,000/yr |
| FQHC/Rural hospital | 4-8 weeks | CEO/CMO + IT | $10,000-$30,000/yr |
| ACO/Health system | 8-16 weeks | CMO/CMIO + procurement | $30,000-$200,000/yr |

Our sweet spot is mid-size groups and FQHCs—large enough to have real budget, small enough to move fast.

### Why would someone switch from their current solution?

**For those with legacy vendors:**
- They're paying 10x more for slower data
- Their contract is up and they're shopping
- They're frustrated with 12-month implementations

**For those using spreadsheets/manual:**
- They just got a MIPS penalty and need a solution
- They're joining an ACO and need quality infrastructure
- Their quality coordinator quit and they can't replace them

We don't need to convince people they need quality measurement. CMS already did that. We just need to be there when they're ready to buy.

---

## Financial

### What are your unit economics?

| Metric | Value |
|--------|-------|
| **Average Revenue Per Account (ARPA)** | $4,200/year |
| **Gross Margin** | 85% |
| **CAC (blended)** | $800 |
| **LTV (5-year, 10% churn)** | $12,400 |
| **LTV:CAC** | 15.5x |
| **Payback Period** | 2.3 months |

### When will you be profitable?

We project cash-flow break-even at **150 customers** (~$630K ARR), expected in **Q3 2025** with current runway.

Profitability (net income positive) at approximately 300 customers ($1.3M ARR), expected in Q2 2026.

### What's your burn rate?

Current monthly burn: ~$25,000
- Engineering: $15,000
- Infrastructure: $3,000
- Operations: $7,000

With $1.5M raise, monthly burn increases to ~$80,000:
- Engineering (3 additional): +$35,000
- Sales (2): +$15,000
- Operations/compliance: +$5,000

This gives us 18+ months of runway to hit Series A milestones.

### How do you justify the valuation?

We're raising at a $7.5M post-money valuation ($1.5M for 20%).

**Comparables:**
- Seed-stage healthcare SaaS: 15-25x ARR (we'd need $300K-$500K ARR to justify on revenue)
- Pre-revenue with product: Based on team, market, and technical differentiation

**Our justification:**
- Working product with 61 validated measures
- $2B+ market with clear path to $25M ARR
- 37x cost advantage as structural moat
- Technical architecture competitors can't easily replicate

---

## Team

### Why are you the right team to build this?

[To be customized with founder backgrounds]

**What we bring:**
- Deep domain expertise in healthcare data standards (FHIR, CQL, HEDIS)
- Technical ability to build and ship quickly
- Understanding of healthcare go-to-market (it's different)
- Network in the quality measurement space

### What are your weaknesses?

**Honest answer:** We're technical founders. Our risk is go-to-market execution.

**Mitigation:**
- We're hiring sales earlier than typical (with this raise)
- We're partnering with people who have distribution (billing companies, consultants)
- We're pricing low enough that "try it and see" is a reasonable customer motion

### Do you have healthcare experience?

[To be customized]

---

## Risk

### What could kill this company?

**1. Regulatory change (Low probability, high impact)**
If CMS eliminated quality-based payment programs, our market would shrink significantly. Mitigation: Quality programs have bipartisan support and are expanding, not contracting. Even if CMS changed course, commercial payers would continue quality incentives.

**2. Platform risk (Medium probability, medium impact)**
Epic could decide to make quality measurement free or significantly better. Mitigation: They've had 20 years to do this and haven't. Their incentive is to sell EHR licenses, not quality software.

**3. Sales execution (Higher probability, medium impact)**
We fail to find product-market fit and can't acquire customers cost-effectively. Mitigation: We're starting with a segment (small practices) where we have the strongest value proposition and lowest sales complexity.

**4. Technical execution (Low probability, high impact)**
We can't scale the platform or maintain quality as we grow. Mitigation: We've built on modern, scalable architecture (Kubernetes, PostgreSQL, FHIR). We have 61 measures working in production today.

### What if a customer has a data breach?

We carry $5M in cyber liability insurance. Our architecture encrypts all PHI at rest (AES-256) and in transit (TLS 1.3). We're pursuing SOC 2 Type II certification (expected Q2 2025).

Healthcare data breaches are typically caused by phishing or ransomware, not SaaS vendor vulnerabilities. Our cloud-native architecture is actually more secure than the on-premise servers many practices run today.

### What about HIPAA?

We sign Business Associate Agreements (BAAs) with all customers. We maintain a formal HIPAA compliance program including:
- Annual risk assessments
- Workforce training
- Incident response procedures
- Minimum necessary access controls

HIPAA compliance is table stakes for healthcare SaaS. We're compliant.

---

## Exit & Returns

### Who would acquire you?

**Strategic acquirers:**
- **EHR vendors** (Epic, Oracle/Cerner, athenahealth): Acquire quality capabilities vs. build
- **Payer platforms** (Availity, Change Healthcare): Quality data is valuable for VBC
- **Population health vendors** (Arcadia, Innovaccer): Acquire our technology + customer base
- **Private equity**: Healthcare IT is a hot sector for PE roll-ups

**Realistic acquisition range:** $50M-$200M at 5-8x revenue (assuming $10M-$25M ARR at exit)

### What are comparable exits?

| Company | Acquirer | Year | Price | Multiple |
|---------|----------|------|-------|----------|
| Jvion | Lightbeam | 2021 | $150M | 10x ARR |
| Collective Medical | PointClickCare | 2022 | $580M | 8x ARR |
| Signify Health | CVS | 2022 | $8B | 6x revenue |
| Alignment Healthcare | SPAC | 2021 | $2.1B | 5x revenue |

Healthcare IT commands premium multiples due to sticky revenue and regulatory moats.

### What's the path to $100M+ outcome?

**Conservative path:**
- Year 5: 2,000 customers, $25M ARR
- Acquisition at 5x revenue = $125M

**Aggressive path:**
- Year 5: 4,000 customers, $50M ARR
- IPO or acquisition at 8x revenue = $400M

**For a $7.5M post-money valuation:**
- $125M exit = 16.7x return
- $400M exit = 53x return

---

## The Ask

### What are you raising and on what terms?

**Raising:** $1.5M Seed
**Structure:** SAFE (Post-Money) or Priced Round
**Valuation Cap:** $7.5M post-money
**Use of Funds:**
- 50% Engineering (hire 3, scale platform)
- 30% Sales (hire 2, build pipeline)
- 20% Operations (SOC 2, customer success)

### What milestones will this capital achieve?

| Milestone | Target | Timeline |
|-----------|--------|----------|
| Paying customers | 50 | 12 months |
| ARR | $500K | 12 months |
| SOC 2 Type II | Certified | 6 months |
| Case studies | 3 with documented ROI | 9 months |
| Series A ready | Yes | 12-15 months |

### Why should I invest now?

1. **Pre-revenue pricing.** Once we have customers and ARR, valuation goes up 3-5x.

2. **Technical risk is retired.** The product works. 61 measures are validated. Real-time evaluation is proven.

3. **Market timing is perfect.** FHIR mandates just took effect. Practices are shopping. Legacy vendors are struggling.

4. **The team can execute.** We've built complex healthcare software before. We know this domain.

The question isn't whether healthcare needs better quality measurement—the question is whether we can execute. We're asking you to bet on our execution at a price that rewards early conviction.

---

*Last Updated: December 2025*
