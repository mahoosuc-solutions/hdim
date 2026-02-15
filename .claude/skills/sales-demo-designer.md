---
name: sales-demo-designer
description: Create persona-specific demo pathways (15/30/45-min) with feature prioritization and storytelling
---

# Sales Demo Designer 🎬

**Purpose:** Design killer demos tailored to each buyer persona. Learn what to show, when to show it, and why it matters.

**Use Cases:**
- "Design a 30-minute demo for a CMO at a health plan"
- "What features should I prioritize for a coordinator?"
- "Walk me through a 15-minute teaser demo"
- "How do I demo care gap workflow to a provider?"

---

## 🎯 The Demo Philosophy

### Core Principle: Show What They Care About

```
❌ WRONG: Show all 52 features
✅ RIGHT: Show the 3-4 features that solve their pain in 3-5 minutes

❌ WRONG: Technical deep-dive on architecture
✅ RIGHT: Business outcome (gap closure, coordinator time savings, ROI)

❌ WRONG: Scripted, pre-recorded walkthrough
✅ RIGHT: Interactive, responsive to their questions, real-time
```

### Demo Golden Rules

1. **Start with Outcome, Not Features**
   - Don't say: "Let me show you our FHIR R4 integration"
   - Say: "I want to show you how you'd find all high-risk patients needing follow-up in <10 seconds"

2. **Connect Every Feature to Their Pain**
   - Coordinator pain: Manual gap prioritization → Show smart prioritization feature
   - CMO pain: Reactive gap discovery → Show 30-60 day predictive detection
   - CFO pain: No real-time ROI visibility → Show financial tracking dashboard

3. **Interactive > Passive**
   - Pause and ask: "Does that workflow work for you?"
   - Let them drive the mouse: "Want to try clicking that to see patient details?"
   - Respond to their questions: "Good question—let me show you how we handle that"

4. **Time Discipline**
   - 15-min teaser: 3-4 core features, no details
   - 30-min standard: 5-6 features, depth, Q&A
   - 45-min deep-dive: Full workflow, edge cases, customization

5. **Always End with Financial Impact**
   - Don't leave them thinking: "That's cool"
   - Leave them thinking: "If this works, we save $500K annually"

---

## 📊 Demo Pathways by Persona

### Persona 1: CMO / VP Quality (⭐⭐⭐⭐⭐)

**Demo Duration:** 30 minutes (standard)

**Pain Points Being Addressed:**
- HEDIS deadline pressure ("We need to close gaps by Oct 31")
- Manual workflows eating time ("Our team spends 20% of time on gap prep")
- Reactive approach ("We find gaps AFTER submissions")
- Board visibility ("Board wants proof of ROI")

**Demo Flow (30 minutes)**

```
1. OPENER (1 min)
   "I want to show you three things today:
    • How HDIM finds gaps you're currently missing
    • How it prioritizes gaps for your coordinators
    • How it calculates your quality bonus impact in real-time
    Ready?"

2. THE PROBLEM (2 min)
   "Most health plans discover gaps like this: [show screenshot of typical workflow]
    You run reports weekly or monthly. By the time you see the gaps,
    you have limited time to close them before HEDIS submission.

    What if I could show you gaps 30-60 days BEFORE they impact your quality score?"

3. PREDICTIVE GAP DETECTION (5 min) 🔥 MAIN FEATURE
   [Live Demo]
   • Patient population view (10K members)
   • Red-flagged patients: High-risk gaps predicted
   • Click on patient → see gap prediction reasoning
   • Timeline view: "This gap needs closure by Oct 15 (before HEDIS submission)"

   Callout: "This patient is flagged because: Diabetes + HbA1c 8.2% (last test 8 months ago).
            Our AI predicts likelihood of failure = 87%. Recommend: Order A1C, adjust meds."

   CFO Moment: "This one gap represents $200 in quality bonus if we close it.
                Your population has 150 similar gaps we predicted this month."

4. COORDINATOR DASHBOARD (5 min) 🔥 SECOND FEATURE
   [Live Demo]
   • Show default view: "These are the top 3 gaps per patient, prioritized by ROI"
   • Filter by measure: "Only show gaps that impact YOUR top 5 quality measures"
   • Show prioritization logic: "We sort by: Financial impact × likelihood of closure × time to deadline"

   Callout: "Coordinators tell us this dashboard saves them 40% time on gap review.
             Instead of reviewing 20 generic gaps per patient, they focus on the 3 most important."

5. INTERVENTION TRACKING (4 min)
   [Live Demo]
   • Coordinator marks gap as "In Progress" (outreach started)
   • Timeline: "We track every day. If 7 days pass with no closure, we send reminder"
   • Gap closure: Coordinator marks "Closed" → system auto-updates quality bonus calculation

   Callout: "This is your closure tracking + audit trail. Board asks 'How many gaps closed?'
             You have the answer in seconds, not weeks of manual reporting."

6. FINANCIAL DASHBOARD (3 min) 🔥 THIRD FEATURE - THE CLOSER
   [Live Demo]
   • YTD Quality Bonus Tracking: "$18.2M captured vs. $22.1M potential"
   • Gap Opportunities: "150 gaps predicted this month × $200 average impact = $30K potential"
   • Trend: "Gap closure improved from 68% to 76% since HDIM deployment (Month 1)"

   Callout: "This is what the board sees. One number: financial impact of your quality program.
             Not 'we closed 150 gaps'—but '$1.2M in incremental quality bonus this month.'"

7. TIMELINE & NEXT STEPS (3 min)
   "Here's how this works:

   Week 1: Epic integration + load your patient population
   Week 2: You validate data accuracy + gap predictions
   Week 3-4: Full production deployment + 24/7 monitoring

   Total time to go-live: 4 weeks. Total cost: $30-50K.
   ROI: 2-3x in first month based on gap closure improvement alone.

   What would need to happen for you to schedule the integration next week?"

8. Q&A & CLOSE (2 min)
   [Let them ask questions. Be ready for:]
   - "How accurate is the prediction model?" → "87-92% (validated against manual review)"
   - "Can we customize the prioritization logic?" → "Yes, completely customizable"
   - "How do you handle multiple payers?" → "Multi-tenant, tenant data isolated, HIPAA verified"

**Materials Ready to Share:**
- [ ] Financial ROI calculation (spreadsheet)
- [ ] Pilot proposal (1-pager)
- [ ] 2-4 week implementation timeline
- [ ] Reference customer contact info
```

---

### Persona 2: Quality Coordinator (⭐⭐⭐⭐)

**Demo Duration:** 20 minutes (focused)

**Pain Points Being Addressed:**
- Manual gap list overwhelming ("I get 500 gaps per day")
- Provider engagement low ("Providers ignore our outreach")
- Time pressure ("I spend 6 hours/day on gap management")
- No prioritization ("I don't know which gaps matter most")

**Demo Flow (20 minutes)**

```
1. OPENER (1 min)
   "Today I want to show you how to go from drowning in 500 gaps per day
    to focusing on the 3-5 gaps that actually close. Sound good?"

2. THE PROBLEM (2 min)
   [Show generic gap list from their current system]
   "Right now, you get something like this every day:
   • 500 gaps listed
   • No prioritization
   • No context (why this patient? when does it need closure?)
   • Providers ignore most of it

   What if instead you got THIS?" [Show HDIM coordinator dashboard]

3. SMART GAP DASHBOARD (5 min) 🔥 MAIN FEATURE
   [Live Demo - Interactive]
   • "This patient: Mrs. Rodriguez, 72, Diabetes gap"
     - Why flagged: HbA1c 8.2% (8 months no test)
     - Impact: $200 bonus if closed
     - Time: Must close by Oct 1 (53 days)
     - Provider: Dr. Smith (contact: email + phone)

   • "Your action: Click 'Start Outreach'" [Show workflow]

   • "Provider gets: Clinical narrative (not generic alert)"
     - "Mrs. Rodriguez, 72, Diabetes (HbA1c 8.2%, last test 8 months ago)
       Recommend: Order A1C, adjust meds if indicated. Reply here when done."

   Callout: "Instead of 'Hey, close this gap' → providers get clinical context.
             They're 3x more likely to engage with this."

4. PRIORITIZATION LOGIC (3 min)
   "Here's what makes this different:

   Your top 3 patients today based on our algorithm:
   • Patient A: $500 bonus opportunity, 87% closure likelihood, due in 10 days
   • Patient B: $300 bonus opportunity, 92% closure likelihood, due in 15 days
   • Patient C: $150 bonus opportunity, 65% closure likelihood, due in 30 days

   If you have 2 hours, focus on A & B. You'll close 87-92% of them.

   Old way: You'd spend time on all 500, close 15-20%.
   New way: Focus on top 20, close 17-18 (87-90% of your effort)."

5. WORKFLOW TRACKING (3 min)
   [Live Demo]
   • "You mark 'Start Outreach' → system tracks time"
   • "Day 3: Provider hasn't responded → system sends reminder to YOU"
   • "You reply-all with clinical question → tracks engagement"
   • "Gap closed → system auto-updates your numbers"

   Callout: "All your work is tracked. Manager asks 'How many calls did you make?'
             You have the data. No manual logging."

6. TIME SAVINGS CALC (2 min)
   "Let's do the math:

   Old workflow: 500 gaps/day × 5 min review = 40 hours/week on gap review
   New workflow: 20 gaps/day × 2 min review = 7 hours/week on gap review

   You save 33 hours/week. That's:
   • 5 hours of your time back each day
   • More time on actual outreach (not busy work)
   • Fewer coordinators needed (or time for other projects)

   Most coordinators we work with say: 'This is the first day I felt like I could
   actually close gaps instead of drowning in lists.'"

7. PROVIDER ENGAGEMENT (2 min)
   [Show provider-facing messaging]
   "Providers tell us: 'Finally, an alert that makes sense.'

   Instead of generic warnings, they get:
   • Patient context (age, conditions, risk)
   • Clinical reasoning (why this matters)
   • Actionable recommendation (what to do)

   Our pilots see 3x higher provider engagement."

8. TIMELINE & CLOSE (2 min)
   "Here's what happens:

   Week 1: We connect to Epic + load your patient list
   Week 2: You test the dashboard with your team
   Week 3: Go-live, you start using it
   Week 4: You're saving 25+ hours/week

   How does that sound? Can we get your manager on a call next week to approve the pilot?"

**Materials Ready to Share:**
- [ ] Time savings calculator (spreadsheet)
- [ ] Coordinator job description change (freed-up hours)
- [ ] Provider engagement metrics
- [ ] Peer testimonial from coordinator at reference customer
```

---

### Persona 3: Healthcare Provider / Physician (⭐⭐⭐)

**Demo Duration:** 10 minutes (quick)

**Pain Points Being Addressed:**
- Alert fatigue ("I get 50 alerts/day, ignore most")
- Lack of patient context ("Generic message, not clinical")
- Workflow disruption ("One more thing to log into")
- Skepticism ("Does this actually help my patients?")

**Demo Flow (10 minutes)**

```
1. OPENER (1 min)
   "You get a lot of alerts. Most you ignore. I want to show you something different:
    an alert that actually has clinical value. OK?"

2. THE PROBLEM (1 min)
   "Traditional alert: 'Patient 12345 has open care gap: Diabetes HbA1c test.'"
   Provider reaction: [rolls eyes] Delete.

   Our alert: [Show example]"

3. HDIM PROVIDER ALERT (4 min) 🔥 MAIN FEATURE
   [Live Example - Patient Context Alert]

   "Mrs. Smith, 68, Type 2 Diabetes
    Current HbA1c: 8.2% (Target: <7%)
    Last Test: 8 months ago
    Risk: Uncontrolled, 90-day ROI: $200 quality bonus if controlled

    Recommendation: Order HbA1c test, adjust medications if A1C >7.5%

    [Respond here to close gap]"

   Provider perspective:
   • "OK, this is actually useful. I need to order an A1C anyway."
   • "The 90-day timeline helps me prioritize."
   • "I can respond right here (no new login needed)."

   Callout: "This is what providers tell us: First alert they actually want to act on.
             Not busy-work noise. Actual clinical decision support."

4. IMPACT (3 min)
   "Here's what happens:

   Clinic Workflow:
   • You see alert → 30 seconds to understand (vs. 5 minutes decoding generic message)
   • You order A1C (or delegate to nurse)
   • You respond 'Order placed' → gap closes
   • System tracks it (proves to hospital/payer that YOU helped)

   Your Benefits:
   • Better patient outcomes (gaps closed = better health)
   • Proof of your care quality (for hospital/payer contracts)
   • No extra work (fits into existing workflow)"

5. CLOSE (1 min)
   "So when your quality coordinator sends you an HDIM alert,
    you know it's worth reading. Clinical, not noise."

**Materials Ready to Share:**
- [ ] Sample alert (screenshot)
- [ ] Clinical evidence (outcomes study)
- [ ] Provider testimonial
- [ ] EHR integration details
```

---

### Persona 4: CFO / Finance Leader (⭐⭐⭐⭐⭐)

**Demo Duration:** 20 minutes (ROI-focused)

**Pain Points Being Addressed:**
- Quality program ROI unclear ("How much is this investment worth?")
- Competitive bonus pressure ("We're at 3.5 stars, want 4+")
- Board visibility ("Board wants quarterly results")
- Budget justification ("Why spend $50K on this vs. alternatives?")

**Demo Flow (20 minutes)**

```
1. OPENER (1 min)
   "I want to show you the exact financial impact of quality program improvements.
    In dollars. Not estimates. Month-by-month."

2. THE PROBLEM (2 min)
   "Right now, you probably know:
   • Current quality bonus: $18M (Star rating 3.5)
   • Target bonus: $22M (Star rating 4.0)
   • Gap: $4M missing

   What you probably DON'T know:
   • Which specific gaps would get you to $22M
   • Which gaps will actually close in your workflow
   • Monthly progress toward that $4M gap

   Traditional quality vendors give you reports. We give you decision data."

3. THE HDIM FINANCIAL MODEL (4 min)
   [Dashboard Demo]

   Current State:
   • Quality Bonus: $18.2M/year
   • Gap Closure Rate: 68%
   • Miss Rate: $3.8M annually

   HDIM Impact (Month 1):
   • Gap Closure Rate: 76% (+8 points)
   • Additional Bonuses: $1.2M incremental
   • Cost: $30-50K
   • ROI: 24-40x first year

   Callout: "Every 1-point improvement in quality score = $500K-$750K in bonuses.
            HDIM typically drives 5-10 point improvement. Math: $2.5-7.5M incremental revenue.
            On a $30-50K investment."

4. MONTH-BY-MONTH TRACKING (5 min) 🔥 MAIN FEATURE
   [Live Financial Dashboard]

   January 2026:
   • Quality Bonuses Captured: $1.8M
   • Predicted Gaps Identified: 450
   • Gaps Closed: 287 (64%)

   February 2026 (Month 2):
   • Quality Bonuses Captured: $2.1M (+16% vs Jan)
   • Predicted Gaps: 520
   • Gaps Closed: 405 (78%)

   March 2026 (Month 3):
   • Quality Bonuses Captured: $2.3M (+10% vs Feb)
   • Trend: Stabilizing at 76-78% closure rate
   • Projection: Annual impact = $27.6M (vs. current $18.2M = +$9.4M)

   Callout: "Not estimates. Real closure data. Real bonus tracking.
             Board asks 'What's our quality program worth?' You have the answer."

5. COMPETITIVE SCENARIO (4 min)
   "Two scenarios:

   Scenario A: Status Quo (No HDIM)
   • Year-end bonus: $18.2M
   • Star rating: 3.5
   • Competitive position: Middle of pack

   Scenario B: With HDIM (Month 1 deployment)
   • Year-end bonus: $27.6M (projected)
   • Star rating: 4.0+ (move to top quartile)
   • Competitive position: Top 20% of health plans
   • Differentiation: 'We improved quality 10 points in one year'

   Which scenario do you want the board seeing?"

6. IMPLEMENTATION TIMELINE (2 min)
   "Time to Revenue:

   Week 1-2: Epic integration + data validation
   Week 3-4: Production deployment
   Week 5 (Month 2): First quality bonus impact visible (vs. traditional quarterly reports)
   Month 3+: Compound benefits (coordinators trained, workflow optimized)

   Competitive advantage: Competitors take 6+ months. You're there in 4 weeks."

7. INVESTMENT DECISION (2 min)
   "Investment Summary:

   • Cost: $30-50K (or PPPM pricing: $0.20/member/month for 10K members = $24K/year)
   • Benefit Year 1: $9.4M+ incremental quality bonuses
   • ROI: 190-300x
   • Break-even: Week 2 (literally pays for itself on first gap closure)
   • Strategic benefit: Move from 3.5 to 4.0 stars (competitive win)

   I think the question isn't 'Can we afford HDIM?' but 'Can we afford NOT to?'

   Can we move this to the board for March budget approval?"

**Materials Ready to Share:**
- [ ] ROI calculator (interactive spreadsheet)
- [ ] Financial model (Month 1-12 projections)
- [ ] Competitive benchmark report
- [ ] Quality bonus impact analysis
- [ ] Case study: Similar-sized plan's results
```

---

### Persona 5: IT / Analytics Leader (⭐⭐⭐⭐)

**Demo Duration:** 30 minutes (technical)

**Pain Points Being Addressed:**
- Integration complexity ("8-12 week implementation horror stories")
- Data quality ("Garbage in, garbage out")
- Compliance burden ("Another system to audit")
- Scalability concerns ("Will it handle our member volume?")

**Demo Flow (30 minutes)**

```
1. OPENER (1 min)
   "I want to show you the architecture, integration, and data flow.
    Then I'll answer: Is this enterprise-ready for your scale?"

2. ARCHITECTURE OVERVIEW (4 min) 🔥 FIRST FEATURE
   [Technical Diagram]

   HDIM System Architecture:
   • Gateway (Kong-based, API rate limiting, load balancing)
   • 4 Specialized Services (Patient, Care Gap, FHIR, Quality Measure)
   • Event Bus (Apache Kafka, real-time events)
   • Multi-Tenant Database (PostgreSQL, row-level security)
   • Cache Layer (Redis, PHI-compliant 5-min TTL)

   Your Perspective:
   • Microservices (easier to maintain, scale independently)
   • Event-driven (real-time processing, not batch)
   • Cloud-native (Kubernetes-ready)
   • HIPAA-compliant (encryption, audit logs, data isolation)

   Callout: "This is modern healthcare architecture.
            Built for scale (enterprise-grade SLA 99.9%+).
            NOT legacy monolith."

3. DATA INTEGRATION (6 min) 🔥 SECOND FEATURE
   [Integration Flow Demo]

   "We support 3 data paths:

   Path 1: FHIR R4 (Epic 2015+, Cerner, any FHIR server)
   • Real-time REST API
   • Data flows continuously
   • Validation on arrival
   • Lag: <5 minutes

   Path 2: HL7 v2 (Claims, Lab, older EHR)
   • EDI standard format
   • Batch or real-time
   • Data validation + transformation
   • Lag: <1 hour

   Path 3: Bulk Data API (Initial load)
   • 100K patients in <5 minutes
   • Parallel processing
   • No server impact to Epic

   Integration Timeline:
   Week 1: Credential validation + data extraction test
   Week 2: Full data load validation (10K sample patients)
   Week 3-4: Production deployment + 1-week monitoring

   Typical integration: 2-4 weeks (vs. 8-12 with legacy vendors)"

4. DATA QUALITY & VALIDATION (4 min)
   [Live Dashboard]

   "For every patient we load, we validate:
   • Demographics (name, DOB, insurance ID)
   • Clinical data (diagnoses, medications, procedures)
   • Lab results (HbA1c, lipids, etc.)
   • Encounter data (dates, providers, settings)

   Real-time dashboard shows:
   • Records loaded: 10,000
   • Validation pass rate: 98.7%
   • Data quality issues: 130 (flagged for review)
   • Last update: 2 minutes ago

   Your assurance: Data is continuously validated.
   Not 'we assume it's good.' We PROVE it."

5. SECURITY & COMPLIANCE (5 min)
   [Compliance Checklist]

   "HIPAA Compliance:
   ✅ Data Encryption: TLS in transit, AES-256 at rest
   ✅ Access Controls: RBAC, MFA required
   ✅ Audit Logging: 100% API call logging (§164.312(b))
   ✅ Data Isolation: Row-level security per tenant
   ✅ PHI Handling: Cache TTL ≤ 5 minutes
   ✅ Incident Response: 24-hour breach notification

   Enterprise Requirements:
   ✅ SOC2 Type II Certified
   ✅ HIPAA Business Associate Agreement
   ✅ On-Premise Deployment Option (air-gapped if needed)
   ✅ Data Residency Options (US, EU, your data center)
   ✅ Audit Ready: 100% audit trail for any patient access

   Callout: 'You can show your compliance team this checklist.
            Nothing exotic. Enterprise healthcare standards.'"

6. SCALABILITY & PERFORMANCE (4 min)
   [Performance Metrics]

   "Your requirements:
   • 100K members minimum
   • <500ms care gap evaluation per patient
   • Real-time (not batch)
   • 99.9%+ uptime

   HDIM delivers:
   • Tested to 1M+ members
   • Average latency: <200ms per patient evaluation
   • Horizontal scalability (add servers, not code changes)
   • 99.9% uptime SLA (with redundancy)
   • Kubernetes deployment (auto-scaling)

   You can scale from 10K to 100K to 1M without re-architecting."

7. OPERATIONS & SUPPORT (3 min)
   "During Integration:
   • Dedicated integration engineer (4 weeks)
   • On-call support (24/7 for issues)
   • Weekly syncs on progress
   • Performance monitoring from Day 1

   Post-Launch:
   • Managed service (we handle ops)
   • Monthly updates (backward compatible)
   • Compliance updates (auto-applied)
   • 24/7 support (critical issues)
   • Quarterly business reviews (usage, optimization)"

8. CLOSE (3 min)
   "From IT perspective:
   • Integration: 2-4 weeks (proven process)
   • Operations: Managed service (not on your team)
   • Compliance: Enterprise-grade (auditable)
   • Scale: Enterprise-ready (no re-architecting)

   This is significantly simpler than building in-house
   and gives you enterprise-grade SLAs.

   Can we schedule a 1-hour technical deep-dive with your architecture team?"

**Materials Ready to Share:**
- [ ] Technical architecture diagram (exportable)
- [ ] Integration checklist (FHIR/HL7/Bulk API)
- [ ] Data validation report sample
- [ ] SOC2 audit report (redacted)
- [ ] HIPAA attestation letter
- [ ] Performance benchmarks
- [ ] On-premise deployment guide
```

---

## 🎬 The 15-Minute Teaser Demo

**Use When:** You have 15 minutes max, OR it's an initial exploratory call

**Flow:**

```
1. OPENER (1 min)
   "I want to show you something that takes 5 minutes to understand
    but is different from anything else in healthcare. OK?"

2. THE PROBLEM (1 min)
   [One screenshot of their current workflow - manual, reactive]
   "This is how most plans find gaps. Reactive. After the fact."

3. THE SOLUTION (5 min)
   [Live Demo of ONE core feature]
   • Show predictive gap detection OR
   • Show coordinator dashboard OR
   • Show financial tracking

   Pick the ONE that matches their role.

4. THE IMPACT (4 min)
   "Here's what this means to you: [specific outcome for their role]"
   • CMO: "35% improvement in gap closure"
   • Coordinator: "Save 25 hours/week"
   • CFO: "2.5-7.5M incremental quality bonus"
   • IT: "4-week implementation, not 12"

5. NEXT STEPS (3 min)
   "Want to see the full workflow? Let's schedule 30 minutes next week.
    I'll show you how this works from end-to-end."
```

**Goal:** They leave thinking: "That's interesting. I want to see more."

---

## 🚀 How to Deliver an HDIM Demo

### Before the Demo (Prep Checklist)

- [ ] **Know Their Role:** CMO? Coordinator? CFO? IT?
- [ ] **Know Their Pain:** What's their top 3 challenges?
- [ ] **Customize the Path:** Which 4-5 features will you show?
- [ ] **Have Data Ready:** Live environment with realistic data (not fake)
- [ ] **Test Technical:** Screen sharing, audio, internet (backup plan ready)
- [ ] **Know Your Talking Points:** 3-4 sentences for each feature
- [ ] **Prepare Materials:** ROI calculator, proposal, reference customer list

### During the Demo (Delivery Tips)

1. **Start Strong:** Open hook takes 30 seconds. Build credibility.
2. **Let Them Lead:** Ask "Does that resonate?" Pause for questions.
3. **Show, Don't Tell:** Click buttons. Let them see workflows, not slides.
4. **Connect Features to Pain:** "Remember you mentioned coordinator overwhelm? Watch how this solves that."
5. **Handle Questions:** "Great question. Let me show you how we handle that..." [demo the answer]
6. **End with Outcome:** "If this works for you, 35% better gap closure. Does that align with your goals?"

### After the Demo (Next Steps)

- [ ] **Send Materials:** ROI calculator, proposal, references
- [ ] **Schedule Follow-Up:** "Let's get your IT team on a 30-min call Friday to discuss integration"
- [ ] **Confirm Pilot Timeline:** "Can we kick off March 1st?"
- [ ] **Handle Objections:** Use sales-objection-handler skill
- [ ] **Get Stakeholder Alignment:** "Who else should we loop in?"

---

## 💡 Advanced Demo Techniques

### Technique 1: The "What If" Scenario
```
CMO says: "Our gap closure is stuck at 60%"

You respond: "What if you could see those gaps 30-60 days earlier?
             Let me show you our prediction model... [demo predictive feature]
             With early detection, most plans improve to 75-80%."

Effect: You've just connected their problem directly to your solution.
```

### Technique 2: The "Real Data" Moment
```
During coordinator dashboard demo:

You say: "This isn't simulated data. These are real gaps from a similar
         health plan. Watch what happens when coordinators prioritize by ROI..."
         [Show 3 high-priority gaps]

Effect: Credibility spike. "This is real, not a mockup."
```

### Technique 3: The "Peer Comparison"
```
CFO asks: "What have other plans seen?"

You respond: "Great question. [Competitor Plan]:
             • Year 1 gap closure: 68% → 76%
             • Quality bonus impact: +$1.2M Month 1
             • ROI: 24x on implementation cost

             They're your peer. Same member size. Similar quality program."

Effect: Reduces decision risk. "If they succeeded, we can too."
```

---

## 🚀 Ready to Demo?

**Try these prompts:**

1. "Design a 30-minute demo for a CMO at a 500K-member plan"
2. "Walk me through a 15-minute teaser - what should I show?"
3. "I'm demoing to a coordinator. What features matter most?"
4. "My CFO is skeptical on ROI. How do I demo financial impact?"

**Let's build a killer demo! 🎬**
