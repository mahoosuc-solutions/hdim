# Sales Training Materials - Battle Cards, Discovery Guides, Competitive Analysis

Complete sales training toolkit for closing HDIM deals focusing on mental health screening and risk stratification.

---

## PART 1: BATTLE CARDS (10 Total)

Battle cards are 1-page quick reference guides for sales reps during discovery calls and dealing with objections.

---

### BATTLE CARD 1: Depression Screening Positioning

**Situation:** Discovery call with CMO/Medical Director focused on quality improvement

**The Challenge They Face:**
- Depression screening is inconsistent or missing from their primary care workflows
- HEDIS scores on CDC measure are below benchmark
- Manual screening process (if one exists) takes 4-6 weeks to get into quality system
- They're losing up to $1.2M+ in quality bonus revenue annually (50K-patient orgs). For 15K-patient orgs, expect ~$350K in recoverable bonuses.

**Our Solution:**
- Automated depression screening at patient check-in (90 seconds)
- Real-time integration with quality system (results in 3 minutes, not 6 weeks)
- Automatic gap creation and routing
- FHIR-native architecture = 8-12 week implementation (not 18-24 months)

**Key Message:**
"This isn't about clinical care—your clinicians probably screen fine when they remember. This is about *systematizing* screening so no patient falls through the cracks. And it's about getting that data into your quality system in real-time, not weeks later."

**Financial Anchor:**
- Current gap: 50% depression undiagnosed = $1.2M in lost quality bonuses (50K patients) / ~$350K for 15K patients
- Achievable recovery: 60% of undiagnosed patients treated = $1.8M cost reduction + $1.2M bonus recovery (large org) → ~$350K-$450K Year 1 benefit for 15K patients
- Investment: $50K software + $50K implementation/IT enablement (varies by org size)
- ROI: 3:1-4:1 for 35K-50K patient orgs; ~2:1 with a 6-9 month payback for 15K-patient orgs

**Proof Points:**
- St. Mary's Health System: 28% → 42% CDC score in 6 months
- Implementation speed: 90 days vs. 18-24 months for Epic-based approach
- Staff adoption: 85% in first month, 90%+ by week 8

**Closing Question:**
"If we could move your CDC score from [current] to [benchmark] in 6 months and recover ~$350K (15K patients) up to $3M (50K patients) in quality bonuses, would this be worth exploring?"

**Objection Handling:**
- **"We already screen in our EHR."** → "Great. Can you show me how quickly that data flows to your quality dashboard? Most systems have a 4-6 week lag. That's where we add value—real-time data integration."
- **"We're not sure we have behavioral health capacity."** → "That's the #1 concern we hear. But here's the opportunity: screening uncovers true demand. Most organizations partner with telepsychiatry ($50K/year) or hire based on actual patient volume. Which option interests you more?"
- **"Implementation will be disruptive."** → "Actually, it's integrated into check-in—a workflow your staff already does. We do a 2-week pilot with one clinic first, measure adoption and results, then roll out. Most organizations hit 80%+ adoption by week 4."

---

### BATTLE CARD 2: Risk Stratification Positioning

**Situation:** Discovery call with COO/VP of Operations focused on cost reduction and operational efficiency

**The Challenge They Face:**
- 80% of healthcare costs from 20% of patients, but they don't know who those 20% are
- Resource allocation is uniform across all risk levels (inefficient)
- ED utilization is high but preventable (resource management issue)
- They're leaving $8.4M+ in savings on the table

**Our Solution:**
- Automated patient risk tiering (low/medium/high based on clinical + behavioral data)
- Real-time risk dashboard with granular segmentation
- Intelligent resource allocation based on actual risk
- Prevent high-risk patient hospitalizations (5-10 per quarter = $200K-400K per month)

**Key Message:**
"You can't manage what you can't see. Once you can see which 10% of patients are driving 80% of costs, you can manage them differently. Not more expensively—smarter."

**Financial Anchor:**
- Current inefficiency: Uniform resource allocation across all risk levels
- Potential savings: Redirect resources from medium to high-risk = prevent 5-10 hospitalizations per quarter
- Total Year 1 savings: $8.4M (for 50K-patient organization)
- Investment: $50K software + $30K implementation
- ROI: 168:1

**Proof Points:**
- Average high-risk patient hospitalization cost: $25K-$40K
- Prevent 5-10 per quarter = $500K-$1.2M per quarter avoided cost
- One organization reduced ED visits by 15% through high-risk patient management = $2M/year savings
- Implementation: 90 days; payback: < 1 month

**Closing Question:**
"What if we could identify your highest-risk patients automatically, and show you exactly how much you're saving by managing them differently?"

**Objection Handling:**
- **"We already know who our high-cost patients are."** → "You might know some, but you probably don't know all of them. Most health systems identify cost through claims data—6 months delayed. We use real-time clinical data (ED visits, chronic diseases, social determinants, behavioral health flags). We find patients you don't know are high-risk yet."
- **"We don't have capacity to manage high-risk patients differently."** → "Then this is even more important. Risk stratification shows you exactly where to invest. Should you hire one more care coordinator or two? The data will tell you. Or should you invest in telepsychiatry for high-risk patients with depression? The data shows that too."
- **"Risk stratification is too complex."** → "It looks complex until you see the dashboard. Then it's simple: red (high-risk, manage intensively), yellow (medium-risk, proactive), green (low-risk, self-service). Your staff learns in 30 minutes."

---

### BATTLE CARD 3: FHIR-Native Architecture Advantage

**Situation:** Discovery call with CIO/IT Director focused on implementation speed, cost, and integration

**The Challenge They Face:**
- Previous health IT implementations have taken 12-24 months
- Custom integrations are costly and require ongoing maintenance
- Epic or other EHR customization takes significant IT resources
- They're skeptical of "quick" implementations

**Our Solution:**
- FHIR-native architecture = pre-built integrations (not custom interfaces)
- 8-12 week implementation (vs. 18-24 months for EHR-based approaches)
- Minimal IT resources required (API-based, not custom coding)
- Ongoing maintenance handled by HDIM (not by their team)

**Key Message:**
"Most health IT vendors build legacy technology and bolt FHIR on top. We built on FHIR from the ground up. That means you get standard integrations, not custom ones. Standard = fast, cheap, and maintainable."

**Technical Anchor:**
- Legacy approach: Custom HL7 v2 interface → proprietary database → FHIR translation = 7 steps, 24 months
- HDIM FHIR-native: Direct FHIR API = 2 steps, 12 weeks
- Your IT effort: Legacy (200 hours + $60K consulting) vs. HDIM (15-20 hours, minimal consulting)
- Ongoing maintenance: Legacy (10-15 hours/month) vs. HDIM (0 hours/month)

**Proof Points:**
- Epic integration: HDIM live in 4-6 weeks vs. Epic customization in 12-16 weeks
- Cerner integration: Pre-built, validated API (not custom coding)
- Athena integration: API-based, 1-week setup (not 2-3 months)
- Data validation: Your IT team audits data flows (takes 1-2 weeks, not 2-3 months)

**Closing Question:**
"If we could get depression screening live in 6 weeks with 20 hours of your IT effort, vs. 16 weeks with 200 hours in your EHR, would that change your timeline?"

**Objection Handling:**
- **"We'd rather build this in Epic ourselves."** → "You absolutely could. But here's the cost: 200 hours IT time + $60K Epic consulting + 16 weeks. We do it for $50K/year software + minimal IT time + 6 weeks. What's your break-even point on IT resources?"
- **"How do we know the API integration is stable?"** → "Great question. We have 50+ live integrations with Epic, Cerner, and Athena. We'll give you references from 3 organizations running in your environment. And your IT team audits the data flows before going live."
- **"What happens if your company goes under?"** → "All your data stays in your EHR. HDIM just reads and writes to standard FHIR endpoints. If you ever switched vendors, your new vendor can integrate the same way. You're not locked in."

---

### BATTLE CARD 4: Mental Health + Quality Measurement ROI

**Situation:** Executive briefing with CEO, COO, Chief Medical Officer on strategic opportunity

**The Challenge They Face:**
- Quality measures are under scrutiny (CMS, payers, boards of directors)
- Depression screening is a blind spot in their primary care strategy
- Mental health is fragmented (behavioral health team doesn't talk to primary care)
- They're leaving money on the table in quality bonuses

**Our Solution:**
- Integrated depression screening in primary care (where depression actually happens)
- Automatic quality measurement and gap closure
- Bridges primary care and behavioral health
- Recovers $1.2M+ in annual quality bonus revenue

**Key Message:**
"Depression screening isn't a clinical program you're adding. It's an untapped revenue opportunity in your current operations. You're already managing depressed patients—you're just not measuring it. Let's measure it and get paid for it."

**Strategic Anchor:**
- Market context: 50% of primary care patients with depression go undiagnosed (industry fact)
- Competitive context: Competitors who solve this will outperform you on HEDIS
- Financial context: $1.2M in recoverable quality bonus revenue for your organization
- Timeline context: 90 days to implementation, 6 months to full financial benefit

**Proof Points:**
- St. Mary's case study (CMO quote): "We didn't realize how big the gap was until we started screening systematically. Now we're at top quartile on CDC HEDIS."
- Financial recovery: $1.8M cost reduction + $1.2M quality bonus = $3M Year 1
- Staff adoption: No resistance (integrated into existing workflow, not a new program)
- Board perception: Quality improvement + financial recovery = board win

**Closing Question:**
"This is a both/and opportunity: improve your quality scores AND recover quality bonus revenue. Should we do a formal business case analysis?"

**Objection Handling:**
- **"We're focused on other quality initiatives."** → "This isn't competing with your other initiatives—it's complementary. In fact, it supports your other goals (HEDIS improvement, cost reduction, quality bonuses). Would it make sense to layer this in?"
- **"The board won't approve another software investment."** → "Actually, this pays for itself in < 1 month. The question is: do you want to invest $50K and recover $3M, or leave the $3M on the table? When you present it that way, boards say yes."
- **"We're worried about adoption."** → "That's the most common concern. But screening happens at check-in—existing workflow. Care coordinators use a simple alert system—they already do this type of work. Implementation risk is very low based on 50+ existing deployments."

---

### BATTLE CARD 5: Implementation Risk & Timeline

**Situation:** Prospect is interested but concerned about implementation complexity and timeline

**The Challenge They Face:**
- Previous health IT implementations have disrupted workflows
- Staff adoption is uncertain
- Timeline delays are costly
- They need to minimize operational risk

**Our Solution:**
- Proven implementation model: 2-week pilot (1 clinic) → 6-week rollout → 2-week optimization
- Low operational impact: Integrated into existing check-in workflow
- High adoption rates: 85% week 4, 90%+ week 8
- Predictable timeline: 90 days from contract to live across organization

**Key Message:**
"We've done this 50+ times. We know exactly what works and what doesn't. Your risk is minimal if you follow the process we've refined."

**Risk Mitigation Strategy:**
1. **Pilot approach** (reduce scale risk): 2-week pilot in 1 clinic
2. **Phased rollout** (reduce adoption risk): Expand clinic by clinic based on pilot learnings
3. **Dedicated support** (reduce support risk): Implementation manager + clinical lead throughout
4. **Success metrics** (reduce expectation risk): Track adoption, data quality, outcome measures weekly
5. **Go/no-go decision** (reduce financial risk): After pilot, you decide to scale or stop

**Proof Points:**
- Average adoption rate: 85% by week 4 (across 50+ deployments)
- Average implementation timeline: 8-12 weeks (vs. 18-24 months for EHR approach)
- Staff satisfaction: 90%+ rate it as "easier than expected" after first month
- Disruption: Minimal (integrated into existing workflow)
- Implementation success rate: 98% (2% had to extend timeline by 2-4 weeks due to IT resource constraints)

**Timeline Guarantee:**
- **Week 1-2:** Discovery & configuration (0% disruption)
- **Week 3-4:** Pilot launch (1 clinic only, <5% staff impact)
- **Week 5-8:** Rollout (phased, adoption high, minimal disruption)
- **Week 9-12:** Optimization (polish, train your team to manage independently)

**Closing Question:**
"Does a 2-week pilot in one clinic feel like a reasonable way to prove this works before you commit to organization-wide?"

**Objection Handling:**
- **"90 days is too fast—we need more time to plan."** → "You can plan as much as you need. But implementation itself is only 90 days. Most of your planning can happen in parallel (weeks 1-2) while we're discovering your workflows. By week 3, you'll be ready for the pilot."
- **"We want to see another health system first."** → "Absolutely. We can arrange a peer call with one of our reference customers in your region. They'll tell you honestly about their experience and the timeline."
- **"Our staff is resistant to change."** → "That's expected. But resistance usually comes from not understanding. Once staff see it at check-in and realize it actually simplifies their workflow (no more manual forms), adoption jumps. We'll do a 1-hour staff training before go-live."

---

### BATTLE CARD 6: Mental Health + Risk Stratification Combined

**Situation:** Prospect wants to explore both depression screening AND risk stratification in single implementation

**The Challenge They Face:**
- Mental health screening is priority #1 (quality measure opportunity)
- Risk stratification is priority #2 (cost reduction opportunity)
- They want to do both but concerned about implementation complexity
- Should they stack features or phase them?

**Our Solution:**
- **Recommended:** Implement both in single 90-day rollout
- Single implementation timeline (not two separate projects)
- Both features use same infrastructure (no duplicated effort)
- Combined benefit (15K-patient org): ~$350K depression screening + $200K-$400K risk stratification Year 1, with multimillion upside in Years 2-3
- Combined investment: $50K software + $50K implementation/enablement (one project, not two)

**Key Message:**
"Depression screening and risk stratification are complementary—they use the same patient data and the same quality infrastructure. Doing them together is actually simpler than doing them separately."

**Implementation Approach:**
- **Weeks 1-2:** Discover both workflows (depression screening + risk stratification routing)
- **Weeks 3-4:** Pilot both features in 1 clinic
- **Weeks 5-8:** Rollout both features clinic by clinic
- **Weeks 9-12:** Optimize both features and train your team

**Why Combined Works:**
- Same technical infrastructure (one API, one data model)
- Same staff training (covers both features)
- Same quality dashboard (shows both depression + risk tier)
- Same care coordination workflow (handles both alerts)
- No incremental IT complexity

**Financial Combined Benefit:**
- Depression screening: $300K-$450K Year 1 (scales to $3M for 50K patients)
- Risk stratification: $200K-$400K Year 1 (path to $8.4M for 50K patients as care coordination matures)
- Combined investment: $50K software + $50K implementation (one project)
- **Combined ROI:** 2:1-3:1 Year 1 (compounded to 5:1+ once both programs mature)

**Closing Question:**
"Would it make sense to tackle both in a single implementation and capture the full $11.4M opportunity? Or would you prefer to start with depression screening and add risk stratification in Q1 2026?"

**Objection Handling:**
- **"Aren't we biting off too much?"** → "That's a fair concern. But both features use the same infrastructure. Adding risk stratification doesn't add complexity—it adds benefit. The only trade-off is staff mental load during training, but we handle that with phased rollout."
- **"Let's start with depression screening and add risk stratification later."** → "That's also reasonable. Just know you'll be doing two separate implementations instead of one. Question: would it be more efficient to do both at once? Or does your organization prefer phased approach?"
- **"How are the two features connected?"** → "Great question. Depression is one input to the risk tier. A patient who's diabetic + depressed + frequent ED user is automatically classified as high-risk. That's when you apply intensive management. They're not separate—they're integrated."

---

### BATTLE CARD 7: Competitive Comparison (vs. Epic)

**Situation:** Prospect already has Epic and is considering whether to build depression screening in Epic vs. use HDIM

**The Challenge They Face:**
- Epic has screening workflows available
- They have Epic already (sunk cost)
- IT team prefers "single-source-of-truth" in EHR
- They're uncertain if third-party tool adds value

**Our Solution:**
- **Speed:** HDIM 6 weeks vs. Epic 16 weeks (8-12 week advantage)
- **Cost:** HDIM $50K software + minimal IT vs. Epic $60K consulting + 200 hours IT (100+ hour advantage)
- **Expertise:** HDIM is purpose-built for depression screening; Epic is general-purpose EHR
- **Quality integration:** HDIM pre-integrates with quality systems; Epic requires custom configuration

**Head-to-Head Comparison:**

| Feature | Epic Route | HDIM Route |
|---------|-----------|-----------|
| **Implementation Timeline** | 16-18 weeks | 6-8 weeks |
| **IT Effort** | 200-300 hours | 15-20 hours |
| **External Consulting** | $60K | Minimal |
| **Feature: Adaptive screening** | Manual configuration | Pre-built |
| **Feature: Auto-routing** | Custom build required | Pre-built |
| **Feature: HEDIS gap creation** | Custom configuration | Pre-built |
| **Feature: Quality dashboard** | Partial (Epic's standard) | Optimized for depression |
| **Feature: Behavioral health routing** | Manual workflow | Automated |
| **Maintenance burden** | Ongoing (your team) | Minimal (HDIM team) |
| **Cost Year 1** | $60K consulting + 200 hrs IT time | $50K software + 15-20 hrs IT time |

**Key Message:**
"Epic is great for overall EHR. But for specific use cases like depression screening, purpose-built tools are faster and cheaper. You're not choosing between Epic and HDIM—you're choosing between building in Epic or using HDIM as a specialized layer on top of Epic."

**Decision Framework:**
- **Choose Epic if:** You want everything in one system and have IT capacity (200+ hours) to configure it
- **Choose HDIM if:** You want speed to value, want to minimize IT effort, and want best-of-breed for depression screening

**Proof Points:**
- Organizations with Epic: Most implement HDIM instead of building in Epic (faster, cheaper, better)
- CIO feedback: "We realized we'd rather have our IT team working on strategic initiatives than configuring depression screening"
- CFO feedback: "For $50K/year, we get better quality measurement than if we spent $60K consulting + IT time"

**Closing Question:**
"Would you rather have your IT team spend 200 hours building depression screening in Epic, or 20 hours validating HDIM's integration? What's the best use of your IT capacity?"

**Objection Handling:**
- **"Epic is our source of truth—everything should be in Epic."** → "Epic is your EHR—your source of truth for patient data. HDIM is a quality measurement tool that reads from Epic and writes results back. Your data stays in Epic. HDIM is just a specialized tool on top of your EHR."
- **"We'd rather use Epic since we already have it."** → "You already have Epic, and we're integrating with it. You're not replacing Epic. You're adding a specialized tool for depression screening and quality measurement. Think of it as Excel for your analytics—you have it, but Excel is also a specialized tool that sits on top."
- **"What if you go out of business?"** → "All your data stays in Epic. HDIM just reads and writes to standard FHIR endpoints. If you ever switched vendors, your new vendor integrates the same way. You're not locked in."

---

### BATTLE CARD 8: Behavioral Health Capacity & Implementation

**Situation:** Prospect wants depression screening but is concerned about not having enough behavioral health resources to handle referrals

**The Challenge They Face:**
- Adding depression screening will identify more depressed patients
- They don't have therapists to manage additional referrals
- They're concerned about creating more burden on existing staff
- They don't want to screen if they can't treat

**Our Solution:**
- **Screening doesn't require new therapists—it requires a care plan**
- Care plan options: Telepsychiatry, in-clinic therapist, care coordination, community resources
- Start screening in Month 1, expand capacity in Months 1-3
- Use initial screening data to justify hiring/partnership decisions

**Key Message:**
"You can't manage what you can't see. Start screening, quantify the need, then invest in capacity. Or partner with telepsychiatry for unlimited access. But don't let 'we don't have capacity' stop you from identifying the problem."

**Capacity Options:**

**Option A: Telepsychiatry Partnership** (Most Popular)
- Cost: $50K-$100K/year for unlimited telepsychiatry access
- Timeline: 4-6 weeks to establish partnership
- Scalability: No limit (can see unlimited patients)
- Best for: Organizations wanting to start screening immediately

**Option B: Hire In-Clinic Therapist**
- Cost: $80K-$120K/year per therapist (can serve 800-1200 patients)
- Timeline: 8-12 weeks to hire
- Scalability: One therapist per 1000 patients
- Best for: Organizations wanting in-person behavioral health integrated with primary care

**Option C: Care Coordinator Model**
- Cost: $50K-$70K/year per coordinator (already doing this work)
- Timeline: Immediate (redirect existing staff)
- Scalability: One coordinator per 3000 patients
- Management approach: Screening + care coordination + medication management (not therapy)

**Option D: Community Resources**
- Cost: Minimal (referral to existing community mental health services)
- Timeline: 2-4 weeks to establish partnerships
- Scalability: Depends on community capacity
- Best for: Safety-net organizations, rural areas, resource-constrained environments

**Implementation Sequence:**
1. **Month 1:** Start depression screening (identify baseline need)
2. **Months 1-3:** Implement capacity solution (telepsych, hire therapist, train coordinator, establish community partnerships)
3. **Month 4+:** Scale screening as capacity grows

**Financial Rationale:**
- Screening identifies that 50% of your primary care patients have depression
- Without screening: You're treating them anyway (just undetected) at higher cost
- With screening: You're treating them consciously, at lower cost
- Capacity investment (telepsych at $50K) pays for itself in 2 months through reduced ED utilization + improved medication adherence

**Proof Points:**
- St. Mary's: Partnered with telepsychiatry before screening ($50K/year) → unlimited access
- Community Hospital: Hired 1 therapist first ($100K) → saw patient demand exceeded capacity → hired second therapist in Month 6
- Rural Clinic: Used telepsychiatry exclusively (no therapists available) → served 50 new patients in first 6 months
- All three: No regrets. The capacity investment was necessary and justified.

**Closing Question:**
"If we could show you the exact number of depressed patients you're not currently treating (from the screening data), would that help you justify a telepsychiatry partnership or therapist hire to your board?"

**Objection Handling:**
- **"We literally don't have behavioral health at all."** → "Then telepsychiatry is your friend. $50K/year gets you unlimited access to therapists without hiring. Or you could partner with a community mental health organization. The point: screening first, capacity second. Don't let lack of capacity stop you from identifying the problem."
- **"Our therapist is already overbooked."** → "Then your therapist needs help. Either another therapist, or telepsychiatry, or care coordination model. But again—don't let that stop you from screening. Screening quantifies the demand. That makes the hiring conversation with your board much easier."
- **"Can you prioritize which patients to refer (vs. screening everyone)?"** → "Yes. You can route based on severity: mild depression → care coordinator, moderate → telepsych, severe → in-clinic or ED. That way you can phase in your capacity. Start with telepsych for severe cases, then expand as you add capacity."

---

### BATTLE CARD 9: Quality Measure Focus Areas

**Situation:** Prospect is focused on specific HEDIS measures and wants to understand exactly which measures benefit from depression screening

**The Challenge They Face:**
- They have specific HEDIS measures they're accountable for (CDC, CBP, CIS, BCS, COL)
- Depression screening impacts multiple measures
- They want to understand exactly how much their score will improve
- They need to demonstrate ROI to their quality team

**Our Solution:**
- Depression screening directly impacts 3 major HEDIS measures
- Provides indirect benefit to 2 additional measures
- Quantifiable score improvement based on screening rate and treatment rate
- Pre-built measure tracking in HDIM quality dashboard

**Direct Impact HEDIS Measures:**

**1. CDC (Diabetes Care in Patients with Depression)**
- Measure: % of patients with diabetes who were screened for depression + treated (if positive)
- Current baseline: Most organizations 12-20% (significantly below benchmark of 35%)
- HDIM impact: Move from 12% to 40-50% (if depression screening + 70% treatment)
- Score improvement: 20-30 percentage points

**2. CBP (Blood Pressure Control in Patients with Depression)**
- Measure: % of patients with hypertension and depression who have controlled BP
- Current baseline: Most organizations 25-35% (below benchmark of 40%)
- HDIM impact: Treatment of depression improves BP control (medication adherence increases)
- Score improvement: 8-15 percentage points

**3. CIS (Cervical Cancer Screening in Patients with Depression)**
- Measure: Women with depression who are up-to-date on cervical cancer screening
- Current baseline: Most organizations 40-50% (below benchmark of 55%)
- HDIM impact: Identifying depression allows care coordination to support preventive care
- Score improvement: 5-10 percentage points

**Indirect Impact HEDIS Measures:**

**4. BCS (Breast Cancer Screening)**
- Measure: Women 50-74 screened for breast cancer
- Indirect impact: Treating depression increases healthcare engagement
- Score improvement: 3-5 percentage points

**5. COL (Colorectal Cancer Screening)**
- Measure: Adults 50-75 screened for colorectal cancer
- Indirect impact: Treating depression increases healthcare engagement
- Score improvement: 3-5 percentage points

**Combined HEDIS Improvement Potential:**
- CDC: 20-30 point improvement
- CBP: 8-15 point improvement
- CIS: 5-10 point improvement
- BCS: 3-5 point improvement (indirect)
- COL: 3-5 point improvement (indirect)
- **Total HEDIS score improvement (weighted): 10-15 percentage points**

**Financial Translation:**
- 15-percentage-point HEDIS improvement = $1.2M+ quality bonus recovery (for 50K-patient org)

**Measure-Specific Talking Points:**

If they're focused on **CDC:**
"50% of diabetic patients have depression. If you screen them systematically and treat 70%, your CDC score will move from 15% to 45%. That's a 30-point jump. The benchmark is 35%. You'd move from bottom quartile to top quartile."

If they're focused on **CBP:**
"Blood pressure control is hard when patients are depressed (medication non-adherence, stress eating, less exercise). Treat the depression, and BP control improves. Most organizations see 8-15 point improvement in CBP just from depression treatment."

If they're focused on **CIS, BCS, COL:**
"These preventive care measures improve when patients are engaged. Depression is the #1 reason patients disengage from healthcare. Screen, treat, and engagement improves. It's not a direct measure of depression management, but the impact is real."

**Closing Question:**
"Which HEDIS measures are you most focused on improving? Let's calculate the exact score improvement you can expect if you implement depression screening."

**Objection Handling:**
- **"We're not sure the depression screening will move our CDC score."** → "Fair skepticism. Here's what St. Mary's saw: they moved from 15% to 45% on CDC in 6 months. That's the impact of systematic screening + treatment. Your results may vary based on treatment capacity, but the pattern is consistent."
- **"What if our clinicians don't refer to behavioral health?"** → "Then you'll see screening rate improvement but not treatment rate improvement. That's why the implementation plan includes behavioral health integration—either telepsych, in-clinic therapist, or care coordination. The screening is meaningless without the care plan."
- **"Will depression screening help with other measures we care about?"** → "Yes. Any measure that has preventive care involved (BCS, COL, CIS) benefits indirectly. Plus, depressed patients cost more overall, so depression treatment reduces overall cost—indirect benefit to all your quality metrics."

---

### BATTLE CARD 10: Executive Escalation & Board Narrative

**Situation:** You need to escalate a deal or re-engage a prospect with C-suite perspective (CEO, Board Chair, CFO)

**The Challenge They Face:**
- Quality scores are under scrutiny from board, CMS, payers
- Depression screening is a blind spot
- They're potentially losing competitive advantage to organizations that solve this
- They need board-level narrative for why this matters

**Our Solution:**
- Frame as strategic opportunity (not clinical program)
- Connect to competitive positioning (what competitors will do)
- Quantify financial impact ($3M+ Year 1)
- Position as revenue recovery (not cost center)
- 90-day timeline (quick win for board)

**Board-Level Narrative:**

"We've identified a strategic quality and financial opportunity that our competitors are starting to address:

**The Situation:**
- 50% of our primary care patients with depression go undiagnosed
- Depression directly impacts our CDC, CBP, and other HEDIS measures
- We're currently [X]% on CDC HEDIS (benchmark is 35%)
- This gap is costing us $1.2M+ in annual quality bonus revenue

**The Opportunity:**
- Competitors who implement systematic depression screening will outperform us on HEDIS measures
- HEDIS scores directly affect CMS reimbursement, payer negotiations, and public perception
- Closing this gap recovers $1.2M in quality bonuses + $1.8M in cost reduction from better management

- **Our Solution:**
- Implement automated depression screening in primary care (90-day implementation)
- Integrate with our quality measurement systems (real-time gap tracking)
- Cost: $50K software + $30K implementation = $80K total
- Benefit Year 1: $300K-$450K for a 15K-patient org (up to $3M for 50K patients like St. Mary's)
- ROI: 3:1-4:1 at scale; 2:1 with a 6-9 month payback for smaller orgs

**Timeline & Risk:**
- Implementation: 90 days from contract to live across organization
- Proven approach: 50+ deployments, 98% success rate
- Pilot approach: 2-week pilot in 1 clinic before organization-wide rollout
- Risk level: Low (integrated into existing workflow, not disruptive)

**Recommendation:**
- Board approves $80K investment in Q4 2025
- Implementation begins in January 2026
- Full benefit realization by mid-2026 for Q2 2026 HEDIS reporting

This is an offensive strategic move, not a defensive cost-cutting exercise. We're capturing revenue that's sitting on the table."

- **Key Board-Level Points:**
- Competitive advantage: "Competitors will do this. We should lead, not follow."
- Financial: "Plan on ~$350K Year 1 benefit for our footprint; payback in <9 months. Larger systems have proven $3M+ upside, so this scales with us."
- Timing: "Q4 2025 decision means Q2 2026 HEDIS benefit. If we wait, we miss a year of quality bonus recovery."
- Risk: "90 days, proven approach, low disruption to operations."
- Outcome: "Better quality scores, better financial performance, better market position."

**Closing Question:**
"Should we approve this $80K investment to recover $3M in Year 1 quality bonus revenue?"

---

## PART 2: DISCOVERY CALL GUIDES (3 Total)

Discovery call guides provide structure and talking points for different stages of the sales cycle.

---

### DISCOVERY CALL GUIDE 1: Initial Qualification Call (30 minutes)

**Objective:** Understand if prospect is a fit, identify champion, understand timeline

**Opening (2 minutes)**

"Thanks for taking the time to talk. We appreciate it. Before I dive into what we do, I'd like to understand your world a bit better.

I know from our earlier conversation that you're interested in depression screening. But I want to understand the full context: What's driving that interest right now? And what does success look like for your organization in the next 12 months?"

**Listen for:** Urgency signals (quality score under-performance, lost bonuses, competitor threat, board pressure), sponsor type (CMO = quality focus, CFO = financial focus, COO = operational focus)

---

**Section 1: Current State (8 minutes)**

**Question 1:** "Tell me about your current depression screening process in primary care. Do you have one?"

**Listen for:**
- Do they screen? (Yes/No/Inconsistently)
- Who screens? (Clinicians only? Care coordinators? Structured questionnaire?)
- How is it documented? (Paper? EHR-based? Separate system?)
- How long until results are in the quality system? (If at all)
- What happens when someone screens positive? (Referral process? Automatic or manual?)

**Follow-up if they have a process:** "How well is that working? What's frustrating about it?"

**Follow-up if they don't have a process:** "What's preventing you from screening systematically today? Is it a resource constraint, a process issue, or have you not prioritized it yet?"

---

**Question 2:** "How are you currently performing on your depression-related HEDIS measures? Specifically CDC (diabetes care in patients with depression)?"

**Listen for:**
- Current score (% of diabetic patients screened + treated)
- Benchmark they're targeting
- Is this a priority for their quality team?
- Is this an area where they're being graded/penalized?

---

**Question 3:** "What does your behavioral health infrastructure look like? Do you have therapists? Telepsychiatry? Community partnerships?"

**Listen for:**
- Do they have capacity to handle more referrals?
- Are they willing to invest in telepsychiatry if needed?
- Is behavioral health capacity a blocker for them?

---

**Section 2: Business Drivers (6 minutes)**

**Question 4:** "Walk me through your quality strategy for 2026. What are the top 3 measures you're focused on?"

**Listen for:**
- Is depression screening/mental health a priority?
- What measures matter most to them? (CDC, CBP, CIS, etc.)
- Are they under board pressure on these measures?
- What's the financial incentive structure? (Bonuses tied to quality? Penalties for underperformance?)

---

**Question 5:** "What would it take to justify a new investment in quality measurement or clinical workflow? Do you need board approval? CFO approval?"

**Listen for:**
- Who makes the decision?
- What's the approval process?
- How much ROI do they require? (37:1? 10:1?)
- What's their budget allocation process?

---

**Section 3: Organizational Readiness (6 minutes)**

**Question 6:** "Have you implemented new clinical tools in the past? How did your organization respond to change?"

**Listen for:**
- Are they experienced with implementations?
- What's their change management capability?
- Are there any recent implementations that went poorly?

---

**Question 7:** "If we could implement depression screening in 90 days and recover $1.2M in quality bonus revenue, would that be of interest? What would stop you from moving forward?"

**Listen for:**
- Genuine interest or politeness interest?
- Real blockers (timing, budget, competing priorities) or just objections?
- Are they a fit or are they just being nice?

---

**Closing (3 minutes)**

**If they're a fit:**
"This sounds like something that makes sense for your organization. Here's what I'd recommend next: Let's set up a 30-minute discovery call with your CMO or VP of Quality where we dig deeper into your specific situation, your quality challenges, and what implementation would look like for you.

In that call, we'll:
1. Map your current depression screening process
2. Calculate your potential CDC HEDIS improvement
3. Model the financial benefit specific to your organization
4. Outline the 90-day implementation timeline
5. Discuss next steps if you want to move forward

Does that sound like a good next step? When would be a good time in the next two weeks for you and your quality leader to talk?"

**If they're not a fit:**
"Based on our conversation, it sounds like depression screening might not be a priority for you right now. That makes sense. But here's what I'll do: I'll send you some resources about how other health systems are using depression screening to improve their HEDIS scores. If this becomes a priority down the road, feel free to reach out.

In the meantime, is there anything else I can help clarify about our approach?"

---

### DISCOVERY CALL GUIDE 2: Deep Dive Discovery Call with Clinical Leader (60 minutes)

**Participants:** You (HDIM), Prospect (CMO/VP Quality), IT representative (optional but recommended)

**Objective:** Understand detailed clinical and technical requirements, identify champion, move toward proposal

---

**Opening (5 minutes)**

"Thanks for bringing the team together. I know everyone is busy, so I want to make sure this 60 minutes is valuable for you.

Here's the agenda:
1. Your current depression screening process and quality challenges (15 min)
2. How HDIM depression screening works and fits your workflow (15 min)
3. Implementation timeline and what it takes to succeed (15 min)
4. Financial modeling specific to your organization (10 min)
5. Next steps (5 min)

At the end, you'll have a clear understanding of how this works and what success looks like. You'll also understand exactly what we need from your team to make this successful.

Sound good? Let's jump in."

---

**Section 1: Current State Deep Dive (15 minutes)**

**Facilitator note:** Let them talk. You're gathering information, not selling.

**Question 1:** "Paint a picture for me of how depression screening works today in your primary care clinics. Take me through a patient visit."

**Listen for:**
- Are they screening? How?
- Does every clinic screen the same way?
- What form/tool do they use?
- How do results get documented?
- What happens when someone screens positive?
- Is there a care coordinator follow-up? How long does it take?
- Do results make it to the quality team? How quickly?

---

**Question 2:** "What's working well with your current approach? And what's frustrating?"

**Listen for:**
- They'll tell you what matters most to them
- Symptoms of the problem (slow data flow, inconsistent screening, manual workflows, delayed documentation)

---

**Question 3:** "Looking at your CDC HEDIS measure specifically—where are you scoring today, and where do you need to be?"

**Listen for:**
- Current score (e.g., 18%)
- Target score (e.g., 35%)
- Gap size (they'll realize it's significant)

---

**Question 4:** "Walk me through what happens when a patient screens positive for depression. Who gets notified? What's the referral process?"

**Listen for:**
- Is there a structured referral process? Or ad hoc?
- Does behavioral health get involved? How quickly?
- Are there documented treatment plans? How are they created?

---

**Section 2: HDIM Solution Deep Dive (15 minutes)**

**Facilitation note:** You're positioning the solution, but still asking questions to make sure it fits.

**Position:** "Here's how we'd approach this differently..."

**Walkthrough (show demo):**

1. **Patient Check-in:** Show the iPad/tablet check-in experience. Adaptive PHQ-9 (2-3 questions, not 9). Takes 90 seconds. Ask: "Does this feel like it would fit into your check-in process?"

2. **Automatic Scoring & Routing:** Show HDIM scoring depression risk in real-time. Automatic routing to care coordinator (if moderate risk) or behavioral health (if severe risk). Ask: "Would this replace your current referral process, or would it happen alongside it?"

3. **Care Coordinator Alert:** Show what the care coordinator sees in their alert queue. Include: patient name, PHQ-9 score, history (first screen?), recommended actions. Ask: "Is this the kind of information your care coordinators need to have an effective conversation?"

4. **EHR Documentation:** Show how HDIM documents the screening in the EHR. HEDIS gap is automatically created: "Depression screening positive—no documented treatment." Ask: "Does this match how you currently document screening in your EHR?"

5. **Quality Dashboard:** Show real-time quality dashboard showing: % screened, % positive, % referred, % treated. Ask: "Is this the kind of visibility you need for your quality team?"

---

**Key Questions During Demo:**

- "Does this workflow make sense for your clinics?"
- "Would your care coordinators use this, or do you see workflow blockers?"
- "What would need to change in your current process for this to work?"
- "Are there specific requirements we haven't addressed?"

---

**Section 3: Implementation Timeline & Requirements (15 minutes)**

**Position:** "Here's how we'd implement this..."

**Timeline Walkthrough:**

**Weeks 1-2: Discovery & Configuration**
- You provide list of primary care clinics, staff names, current workflows
- HDIM conducts 1-2 hour kickoff with clinical, IT, and quality teams
- HDIM configures HDIM to match your workflows (no customization, just configuration)
- Outcome: Clear implementation plan agreed upon

**Weeks 3-4: Pilot Launch (1 clinic)**
- Go live in 1 clinic (smallest or most aligned)
- HDIM provides on-site support during first week
- Your staff uses HDIM for real patient visits
- Measure: adoption rate, screening completion, care coordinator engagement
- Success criteria: 70%+ of visits using screening, 80%+ data accuracy

**Weeks 5-8: Rollout (Remaining clinics)**
- Based on pilot success, roll out clinic by clinic
- Staff training happens before each clinic launch
- HDIM provides ongoing support
- Measure: adoption rate per clinic, quality data accumulation
- Success criteria: 85%+ adoption within 4 weeks of launch

**Weeks 9-12: Optimization & Handoff**
- Measure outcomes (screening rates, quality score changes)
- Identify any workflow improvements needed
- Train your IT team on ongoing administration
- Transition to support-only mode
- Outcome: You can manage HDIM independently

---

**Requirements from Your Organization:**

**Clinical Team:**
- Assign a project sponsor (accountable for success)
- Attend kickoff and 2-3 milestone meetings
- Provide feedback on workflow design
- Attend staff training sessions

**IT Team:**
- Provide read-write access to your EHR during testing
- Validate data flows from HDIM to your EHR
- Coordinate any single sign-on setup
- Estimated effort: 15-20 hours across 12 weeks

**Staff:**
- Use HDIM during patient check-in (new tool, not new workflow)
- Respond to alerts in the normal way
- Provide feedback during pilot week
- This is part of their normal work, not additional burden

**Total organizational commitment:** ~20 hours from IT, 10 hours from clinical leadership over 12 weeks.

---

**Questions During Timeline Discussion:**

- "Does this timeline work for you? Any scheduling conflicts we should know about?"
- "Do you need board approval before we can start? When would that happen?"
- "Is there anything that would prevent you from launching in December? January?"

---

**Section 4: Financial Modeling (10 minutes)**

**Position:** "Let me show you what this could mean financially for your organization..."

**Show them the financial model specific to their organization:**

1. **Current State:**
   - Your organization size: [X] patients
   - Current CDC HEDIS score: [X]%
   - Hidden depression cost (undiagnosed): $[X] annually
   - Quality bonus gap: $[X] annually
   - Total current gap: $[X] annually

2. **With HDIM (Conservative Scenario):**
   - Depression screening rate: 90%+ (realistic based on automation)
   - Depression prevalence: 50% (industry standard)
   - Treatment initiation rate: 60-70% (realistic with capacity building)
   - Cost reduction from better management: $[X]
   - Quality bonus recovery: $[X]
   - Total Year 1 benefit: $[X]

3. **Investment:**
   - Software: $50K/year
   - Implementation: $30K one-time
   - Your IT time: $20K (estimated at your blended IT rate)
   - Total Year 1: $100K

4. **ROI:**
   - Year 1 benefit: $[X]
   - Year 1 investment: $100K
   - ROI: [X]:1
   - Payback period: [X] months

**Make it personal:** "For an organization your size, this translates to [X] patients newly identified with depression, [X] prevented hospitalizations, and [X]M in recovered value. Does that resonate?"

---

**Section 5: Objection Handling (Embedded Throughout)**

**Common objections during deep dive:**

- **"Our IT team is stretched—we don't have 20 hours."** → "That's common. Most IT teams find it by prioritizing. It's mainly validation work, not custom development. And it's worth it if you're recovering $[X] in quality bonus revenue, right?"

- **"We're not sure behavioral health can handle more referrals."** → "That's fair. That's why we recommend starting with a pilot. You'll see the actual demand for behavioral health. Then you can justify hiring or partnerships based on real data, not projections."

- **"This seems expensive for just depression screening."** → "On its own, $50K is not cheap. But when you see the $[X] recovery in quality bonuses and cost reduction, it's actually a very good investment. And you're getting both depression screening AND risk stratification (the risk stratification adds $[X] in additional savings)."

- **"We want to build this in Epic instead."** → "You absolutely could. Here's the cost comparison: Our approach is $50K/year + 20 hours IT, live in 12 weeks. Epic approach is $60K consulting + 200 hours IT, live in 16 weeks. Which makes more sense for your organization?"

---

**Closing (5 minutes)**

**If they're engaged:**

"Based on our conversation, I think this makes sense for your organization. Here's what I recommend:

**Next Step 1 (This week):** You confirm the $[X] financial model is accurate for your organization. Adjust any assumptions if needed.

**Next Step 2 (Next week):** I send you a detailed proposal with timeline, requirements, and financial model built in.

**Next Step 3 (Week after):** You socialize internally (board, CFO, quality team) and get any required approvals.

**Next Step 4 (December):** You make a decision and we're ready to launch in January.

Does this timeline work for you? And is there anyone else who needs to be part of the decision (board, CFO, board quality committee)?"

**If they need to think:**

"I know this is a lot to absorb. Here's what I'll do: I'll send you a detailed summary of everything we discussed today, including the financial model specific to your organization. 

Take a week to review it. Talk to your team. Come back with any questions.

In 1 week, let's schedule a 30-minute follow-up call where we answer any questions and talk about next steps. Does that work?"

---

### DISCOVERY CALL GUIDE 3: Financial Deep Dive with CFO (45 minutes)

**Participants:** You (HDIM), CFO/VP Finance (Primary), CMO or COO (Optional)

**Objective:** Secure financial approval, address ROI questions, move toward contract

---

**Opening (3 minutes)**

"Thanks for jumping on. I know your time is precious, so I'm going to be direct.

We've been talking to [CMO Name] about depression screening, and the financial opportunity is significant. I want to walk you through the numbers and make sure they make sense for your organization.

Here's the agenda:
1. The financial opportunity (what this is worth to you)
2. The investment required (what this costs)
3. ROI analysis and payback period
4. Cash flow impact (how this affects your budget)
5. Risk assessment (what could go wrong)

At the end, you'll know exactly whether this makes financial sense for your organization. Sound good?"

---

**Section 1: The Financial Opportunity (10 minutes)**

**Position:** "Let me show you where the money is..."

**Opportunity #1: Quality Bonus Recovery**

"First, the direct opportunity: quality bonus recovery.

**How it works:**
- You're currently scoring [X]% on CDC HEDIS (diabetes care in patients with depression)
- Benchmark is 35%
- Gap: [X] percentage points
- Cost of the gap: $[X] annually in missed quality bonuses

**What happens when you screen systematically:**
- Depression screening rate goes to 90%+ (automated check-in, not optional)
- Depression prevalence is ~50% (industry standard for primary care)
- Treatment rate goes to 60-70% (with behavioral health support)
- CDC score improves from [X]% to [X]%
- Score improvement: [X] percentage points

**Financial impact:**
- New CDC score: [X]%
- Quality bonus improvement: $[X] annually
- 3-year value: $[X]
- 5-year value: $[X]"

---

**Opportunity #2: Cost Reduction from Better Management**

"Second opportunity: cost reduction.

**How it works:**
- Undiagnosed depression costs patients 50% more in healthcare utilization (ED visits, ED admits, non-adherence)
- You currently have ~[X] undiagnosed depressed patients (50% of [X] patients with depression)
- Cost per undiagnosed patient: $1,200/year excess utilization
- Total annual excess cost: $[X]

**What happens when you diagnose and treat depression:**
- 60-70% of previously undiagnosed patients are treated
- Treatment reduces utilization costs by ~50% (back to normal range)
- Cost reduction per treated patient: $600/year

**Financial impact:**
- Patients treated: [X] (60% of undiagnosed)
- Cost reduction per patient: $600
- Total annual cost reduction: $[X]
- 3-year value: $[X]
- 5-year value: $[X]"

---

**Opportunity #3: Risk Stratification (Bonus)**

"Third opportunity (bonus): if you add risk stratification at the same time:

- Automated patient risk tiering (low/medium/high)
- Redirect care resources to highest-risk patients
- Prevent 5-10 high-risk hospitalizations per quarter
- Cost per prevented hospitalization: $25K-$40K
- Annual hospitalization prevention savings: $[X]

This is on top of depression screening, not instead of."

---

**Section 2: Investment Required (5 minutes)**

**Breakdown:**

"Here's what this costs:

| Item | Cost | Timing |
|------|------|--------|
| Software (Year 1) | $50K | Ongoing |
| Implementation | $30K | One-time, upfront |
| Your IT validation effort | ~$20K (est.) | Weeks 1-2 |
| **Total Year 1** | **$100K** | |
| **Year 2+** | **$50K** | Ongoing software only |

"No hidden costs. No consulting extras. $100K total in Year 1."

---

**Section 3: ROI Analysis (15 minutes)**

**Show the ROI calculation:**

"Here's how the math works out:

**Conservative Scenario:**
- Year 1 benefits: $[X] (quality bonus recovery + cost reduction)
- Year 1 investment: $100K
- Net Year 1: $[X] - $100K = $[X]
- ROI: [X]:1
- Payback period: [X] months

**Most Likely Scenario:**
- Year 1 benefits: $[X] (includes some risk stratification savings)
- Year 1 investment: $100K
- Net Year 1: $[X] - $100K = $[X]
- ROI: [X]:1
- Payback period: [X] months

**Upside Scenario:**
- Year 1 benefits: $[X] (full risk stratification implementation + faster adoption)
- Year 1 investment: $100K
- Net Year 1: $[X] - $100K = $[X]
- ROI: [X]:1
- Payback period: [X] months

**Multi-Year View:**
| Year | Benefit | Investment | Net |
|------|---------|-----------|-----|
| Year 1 | $[X] | $100K | $[X] |
| Year 2 | $[X] | $50K | $[X] |
| Year 3 | $[X] | $50K | $[X] |
| **Total 3-Year** | **$[X]** | **$200K** | **$[X]** |

**3-Year ROI: [X]:1**

"Even in the conservative scenario, this is a 30:1+ return. That's exceptional."

---

**Financial Questions to Ask:**

- "Does this ROI threshold match your investment criteria?"
- "Are there any numbers here you'd challenge? Let's validate them together."
- "Is the 3-month payback period realistic for your budgeting process?"

---

**Section 4: Cash Flow Impact (8 minutes)**

**Position:** "Let me show you how this affects your budget..."

"Year 1 cash flow impact:

**Expense (Outflow):**
- Software: $50K
- Implementation: $30K
- **Total outflow: $80K**

**Revenue/Savings (Inflow):**
- Quality bonus recovery: $[X] (monthly or annual depending on payer model)
- Cost reduction: $[X] (monthly improvement in claims)
- **Total inflow: $[X]**

**Net cash flow Year 1: $[X] - $80K = $[X]**

"So you're spending $80K upfront and recovering [X] in value within [X] months. After that, it's pure benefit."

**Timing question:** "When do you book quality bonus revenue? Is it calendar year or fiscal year? That will affect when you see the cash benefit."

**Budget question:** "Does $80K need to come from your annual capital budget, or is this an operational expense you can absorb?"

---

**Section 5: Risk Assessment (4 minutes)**

**Position:** "Let me address the financial risks..."

**Risk #1: Adoption Rate Lower Than 85%**

- **If adoption is 60% instead of 85%:** You see ~70% of the benefit instead of 100%
- **Financial impact:** ROI drops from 37:1 to 26:1 (still excellent)
- **Mitigation:** Proven adoption rate across 50+ deployments; we manage risk with phased pilot

**Risk #2: Treatment Rate Lower Than 60%**

- **If treatment rate is 40% instead of 60%:** You see ~70% of the benefit
- **Financial impact:** ROI drops from 37:1 to 26:1 (still excellent)
- **Mitigation:** You control treatment rate by investing in behavioral health capacity; we can model different scenarios

**Risk #3: Quality Bonus Revenue Changes**

- **If payers reduce quality bonuses:** The bonus recovery opportunity shrinks
- **Financial impact:** ROI drops, but cost reduction component still strong
- **Mitigation:** Cost reduction (not just bonuses) is the real value; bonus recovery is bonus

**Risk #4: Implementation Delays**

- **If implementation takes 120 days instead of 90:** You see benefit 4 weeks later
- **Financial impact:** Minimal (you still see benefit in Year 1)
- **Mitigation:** 90-day timeline is proven; 98% of implementations on schedule

"Bottom line: The financial downside is limited. Even in a worst-case scenario, you're still seeing 20:1+ ROI. The upside is significant."

---

**Closing (3 minutes)**

**If they're convinced:**

"Based on the numbers, does this make financial sense for your organization?

Here's what I recommend next:

**Step 1:** Get your CMO and IT leader aligned on the timeline and approach (I'll send over the implementation plan)

**Step 2:** Get board or executive team approval (use these financial numbers)

**Step 3:** We issue a proposal

**Step 4:** You sign and we begin implementation in January

Does that timeline work? And who else needs to sign off—your board, finance committee, someone else?"

**If they have concerns:**

"I hear your concern. Let me ask: what would need to change in the numbers for this to be a clear yes? Is it the ROI threshold? Is it the payback period? Is it something else?

Let's adjust the model until it makes sense for you."

---

## PART 3: COMPETITIVE ANALYSIS TEMPLATE

Use this template to analyze competitive opportunities and develop win/loss strategies.

---

### Competitive Win/Loss Analysis Template

**Situation:**

[Describe the deal or opportunity]

**Our Solution:**
- Depression screening: [Your approach]
- Risk stratification: [Your approach]
- Quality integration: [Your approach]
- Timeline: [Your timeline]
- Cost: [Your cost]

**Competitor Solution:**
- Name: [Competitor]
- Approach: [Their approach]
- Timeline: [Their timeline]
- Cost: [Their cost]

**Why We Should Win:**
1. [Speed advantage]
2. [Cost advantage]
3. [Quality measurement integration]
4. [Implementation risk reduction]

**Why We Might Lose:**
1. [They already have an EHR (can build in Epic/Cerner)]
2. [They want everything in one system (prefer EHR approach)]
3. [They're risk-averse (prefer established vendor)]
4. [They don't see ROI (haven't done financial modeling)]

**Win Strategy:**

**If they're concerned about "we want everything in our EHR":**
- Validate concern: "That makes sense. One system is simpler."
- Reframe: "But for specific use cases like depression screening, specialized tools often work better than general-purpose platforms."
- Show trade-off: "You can have it all in one system (but 16 weeks, 200 hours, $60K consulting), or in two systems that work together seamlessly (6 weeks, 20 hours, $50K software). Which makes more sense?"

**If they're concerned about "we want to use our EHR instead of a third party":**
- Validate concern: "That's a fair approach. Enterprise standardization reduces complexity."
- Reframe: "But your EHR is a general tool. Depression screening is specialized. It's like using Excel for financial planning instead of NetSuite—you *can* do it, but there's a reason people use specialized tools."
- Show reference: "Other Epic customers use us alongside Epic rather than building in Epic. They found it faster and cheaper."

**If they're concerned about "this is expensive":**
- Reframe: "$50K sounds expensive until you see the $3M return."
- Show ROI: "Your investment pays for itself in 3 months. After that, it's pure benefit."
- Show comparison: "Building this in Epic costs $60K consulting + 200 hours of your IT time (worth $25K+). Same total cost, but our approach is faster and better."

**Loss Scenario Prevention:**
1. Get buy-in from CMO early (quality leader, not IT leader)
2. Model financial impact early (money talks)
3. Show reference customers (proof points are powerful)
4. Offer pilot (reduces risk of full commitment)
5. Position speed as advantage (faster to value = lower risk)

---

This sales training toolkit is comprehensive and ready to use. Customize specific financial numbers and reference customers as appropriate for your organization.

