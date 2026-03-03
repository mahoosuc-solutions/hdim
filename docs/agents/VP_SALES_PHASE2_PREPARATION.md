# VP Sales Phase 2 Preparation Guide

**Timeline:** Feb 15-28, 2026 (Must be complete before March 1 pilot launch)
**Critical Path Item:** YES - Blocking dependency for 50-100 discovery calls
**Owner:** VP Sales Lead + Product Lead
**Duration:** 2-3 days of intensive training + 1-2 weeks of practice

---

## Why This Matters

**Observable SLOs are HDIM's primary competitive differentiator.** Traditional healthcare vendors make unverifiable promises. HDIM offers proof via real-time trace data customers can see in Jaeger dashboard.

Your job is to position this correctly and close deals based on transparency, not hype.

**Success Metric:** First 50-100 discovery calls (Mar 1-31) result in 1-2 LOI signings and $50-100K committed revenue.

---

## What You'll Learn

### 1. Observable SLOs (The Core Pitch)

**What It Is:**
HDIM defines 4 specific, measurable performance targets that customers can verify in real-time via Jaeger dashboard. When we miss them, automatic service credits are issued (5-10% monthly).

**Why It Matters to Customers:**
- Reduces risk (verifiable guarantees, not vendor promises)
- Builds trust (transparency instead of opacity)
- Enables accountability (automatic remediation if issues occur)
- Allows independent verification (third-party audits possible)

### 2. The 4 Observable Metrics

#### Metric #1: Star Rating Calculation
**What It Measures:** Overall system performance when calculating member star ratings
**Why It Matters:** Physician trust depends on accurate, timely star calculations for network selection
**Phase 1 Baseline (Mar 1-31):**
- P50: 400-600ms
- P95: 1000-1500ms
- P99: 1500-2000ms

**Phase 2 Guarantee (Apr 1+):**
- P99: < 2 seconds
- Monthly Compliance: 99.5%
- Breach Credit: 5-10% monthly discount

**Your Talking Point:**
> "Your star rating calculations complete in under 2 seconds. Physicians see accurate ratings instantly. If we ever miss this target, automatic service credit appears on your invoice—no disputes."

---

#### Metric #2: Care Gap Detection
**What It Measures:** Speed of identifying clinical care gaps (HbA1c screening, breast cancer screening, etc.)
**Why It Matters:** Rapid identification enables faster interventions and better outcomes
**Phase 1 Baseline (Mar 1-31):**
- P50: 800-1200ms
- P95: 2500-3500ms
- P99: 3500-5000ms

**Phase 2 Guarantee (Apr 1+):**
- P99: < 5 seconds
- Monthly Compliance: 99.5%
- Breach Credit: 5-10% monthly discount

**Your Talking Point:**
> "Care gaps identified in under 5 seconds. Care coordinators get alerts immediately. No waiting, no delays. You can see the actual traces showing exactly where time is spent."

---

#### Metric #3: FHIR Patient Data Fetch
**What It Measures:** Speed of retrieving patient FHIR data (demographics, diagnoses, medications, labs)
**Why It Matters:** Clinicians need immediate patient context when making decisions
**Phase 1 Baseline (Mar 1-31):**
- P50: 80-120ms
- P95: 200-350ms
- P99: 350-500ms

**Phase 2 Guarantee (Apr 1+):**
- P99: < 500ms
- Monthly Compliance: 99.8%
- Breach Credit: 5-10% monthly discount

**Your Talking Point:**
> "Patient data available in under 500ms. Clinicians don't wait. Every trace is captured and visible—you can see the exact millisecond where the database query happened."

---

#### Metric #4: Compliance Report Generation
**What It Measures:** Speed of generating HEDIS compliance reports (deadline-critical)
**Why It Matters:** Regulators have strict reporting deadlines; delays risk penalties
**Phase 1 Baseline (Mar 1-31):**
- P50: 8-12 seconds
- P95: 15-20 seconds
- P99: 20-30 seconds

**Phase 2 Guarantee (Apr 1+):**
- P99: < 30 seconds
- Monthly Compliance: 99.0%
- Breach Credit: 5-10% monthly discount

**Your Talking Point:**
> "Compliance reports generated in under 30 seconds. Meeting regulatory deadlines is guaranteed. Every report execution is traced—you can see exactly where the 30 seconds went if something slows down."

---

## Discovery Call Script (30 Minutes)

### Opening (2 minutes)

**Purpose:** Establish credibility and differentiation

> "Thanks for taking the call. I want to tell you about something we're doing that's different from every other healthcare software vendor you talk to. Most vendors promise performance—'we're fast, we're reliable'—but they won't let you see the actual data. HDIM is the opposite. We put all our performance data in your dashboard. You can see every trace. If we miss our promises, you automatically get service credits. No disputes, no negotiation. This is transparency in healthcare software."

### Problem Discussion (5 minutes)

**Purpose:** Understand customer pain points, validate opportunity

**Questions to Ask:**
1. "What's your biggest pain point with your current quality measurement system?"
2. "How fast do you need care gaps to be identified?"
3. "How much time do your teams spend waiting for reports or data?"
4. "How confident are you in your current vendor's performance claims?"
5. "What would it mean to you to independently verify system performance?"

**Listen For:**
- Slow system = bottleneck
- Can't trust current vendor = trust opportunity
- Manual workarounds = inefficiency
- Compliance deadline pressure = urgency

### Solution Overview (8 minutes)

**Purpose:** Position HDIM as the solution

> "HDIM handles everything your team needs: care gap identification, HEDIS measure evaluation, star rating calculations, compliance reporting. Everything you need in one platform. But here's what makes us different: every operation is traced. We measure exact performance down to the millisecond. You get a Jaeger dashboard where you can see every operation. Want to know where those 5 seconds went in care gap detection? Look at the trace. Want to verify we're meeting our SLOs? Check the dashboard. Want independent verification? Pull the traces and have a third party analyze them. That's the difference between vendor promises and verifiable proof."

**Key Points to Hit:**
1. HDIM covers all quality measurement workflows
2. Real-time observability via Jaeger dashboard
3. SLOs are observable, not promissory
4. Automatic service credits for breaches
5. Independence of verification

### Live Dashboard Demo (10 minutes)

**Purpose:** Show proof that this actually works

**Preparation Required:**
- [ ] Staging Jaeger instance deployed and running
- [ ] Sample traces pre-loaded (star rating, care gaps, patient fetch, compliance report)
- [ ] Credentials generated for prospect demo access
- [ ] Dashboard stable and fast (<2 second load time)

**Demo Flow:**
1. **Service Selection** (1 min)
   - Open Jaeger UI
   - Show "Services" dropdown
   - Highlight: "payer-workflows-service, patient-service, care-gap-service, quality-measure-service"
   - Point out: "These are the 4 core services. Every trace goes into this dashboard."

2. **Operation Filtering** (1 min)
   - Select "care-gap-service"
   - Show "Operations" list (detect-gaps, evaluate-hba1c, etc.)
   - Point out: "You can filter by specific operation to see performance of that feature alone."

3. **Healthy Trace Example** (2 min)
   - Select a fast care-gap detection trace (1.2 seconds)
   - Show latency histogram: "P50: 1000ms, P99: 2000ms, all well within our 5-second target."
   - Expand trace waterfall:
     - HTTP request → Database query → RPC call → Response (show timing for each)
     - Point out: "See how the database query took 800ms? That's normal. The RPC call took 200ms. The HTTP overhead is minimal."

4. **Slow Trace Example** (2 min)
   - Select a slower care-gap trace (4.5 seconds)
   - Show it still meets SLO target but was slower
   - Expand trace:
     - Database query took 3.5 seconds (due to complex join)
     - RPC call took 800ms
     - Point out: "This is still under 5 seconds, but if we see many like this, we could optimize the query."

5. **Error Trace Example** (2 min)
   - Select an error trace (e.g., timeout)
   - Show error in trace (red indicator)
   - Point out: "This patient lookup timed out after 10 seconds. It's captured here. We can see exactly what happened."
   - Explain: "If we ever miss an SLO due to errors like this, you automatically get a service credit. No discussion needed."

6. **Export & Verification** (2 min)
   - Show how to export trace data (JSON)
   - Point out: "You can give this to a third party for independent verification. No vendor lock-in on data."
   - Explain: "This data proves our performance. Not internal metrics you have to trust—actual traced execution."

### SLO Positioning (3 minutes)

**Purpose:** Explain what SLOs mean and why they're different

> "Most vendors give you an SLA—a legal promise they won't answer questions about. HDIM gives you SLOs—observable targets you can verify yourself. Here's how it works:
>
> **Phase 1 (March 1-31):** We establish baseline. You see what our system actually performs like under your workload. We measure the exact numbers.
>
> **Phase 2 (April 1+):** We guarantee performance based on Phase 1 baselines. If star rating calculation P99 is under 2 seconds, we guarantee it. If we miss, you automatically get 5-10% monthly service credit.
>
> The key difference: You can see the traces. You can verify. You can have third parties verify. This builds trust because it's based on data, not vendor promises."

**Objection Handlers:**

**Q: "How do I know these traces aren't cherry-picked?"**
A: "You have read-only access to 100% of traces for 30 days. You can see the full distribution. If you want, pull 100 random traces and analyze them yourself. Or hire a third party to audit our traces. That's why observable SLOs matter—you can verify independently."

**Q: "What if your system is slow?"**
A: "Phase 1 establishes baseline—so we only commit to targets we know are achievable. Phase 2 penalties are real: automatic service credits if we miss. We're confident enough to put money on it."

**Q: "Why should I trust HDIM if a competitor promises faster?"**
A: "Because you can't verify their promises. We're saying 'Here's our actual performance in your environment.' That's better than anyone's promise."

### Closing (2 minutes)

**Purpose:** Move to next step

> "Here's what I'd suggest: Let's set up a 2-week pilot. You get access to our dashboard. We'll measure baseline performance. You see exactly what the system does. Then we know whether our guaranteed SLOs (Phase 2) are right for you. Sound good?"

**If Yes:**
- "Great. Let me send you pilot contract and dashboard credentials. Next week we do the onboarding call. You'll have your dashboard running."

**If Maybe:**
- "Fair enough. Let me send you some case studies and data. Let's reconnect in a week. Any questions in the meantime, my door is open."

**If No:**
- "I understand. If things change or you want to revisit, let me know. Happy to help."

---

## Key Talking Points

### 1. Competitive Positioning

**HDIM vs Traditional Vendors:**

| Aspect | HDIM | Traditional |
|--------|------|-------------|
| Performance Claims | "Here's your trace data" | "Trust us" |
| Verification | Customer can see 100% | Internal metrics only |
| SLO Breaches | Automatic credits | Negotiate disputes |
| Independence | Third-party auditable | Vendor-controlled |
| Risk | Low (verifiable) | High (unverifiable) |

**Your Pitch:**
> "Healthcare vendors have been saying 'Trust us' for 30 years. We're saying 'Here's the evidence.' That's the difference."

### 2. ROI & Value

**What Customers Get:**
1. **Faster Care Gap Closure** → Better patient outcomes → Better STAR ratings → Higher bonuses
2. **Reduced Operational Risk** → Verifiable performance → Audit evidence → Compliance confidence
3. **Reduced Negotiation Overhead** → Automatic SLO credits → No vendor disputes → More time on actual work
4. **Increased System Confidence** → Transparent data → Trust in technology → Easier adoption

**Your Pitch:**
> "Faster care gaps → better outcomes. Verifiable performance → audit evidence. Automatic credits → no fighting with vendors. That's why HDIM customers see ROI in the first quarter."

### 3. Use Cases (By Persona)

**CMO/VP Quality:**
> "Your HEDIS scores depend on rapid care identification. HDIM's care gaps are detected in under 5 seconds. You can see the traces proving it. If we miss, you get service credit automatically. Removes the excuse that the system was slow."

**CFO/Finance:**
> "You pay more when quality scores improve. HDIM's observable performance means you can be confident you're paying for real improvements, not hoping the vendor is doing their job. Plus, service credits reduce your costs."

**CIO/IT Director:**
> "Every trace is stored in your dashboard. You can audit our performance independently. No black box. If you need to integrate or optimize, you have complete visibility into what the system is doing."

**VP Care Coordination:**
> "Your teams spend time waiting for slow reports. HDIM reports generate in under 30 seconds. You see bottlenecks in traces. Faster throughput = better coordination = better outcomes."

---

## Objection Handlers

### Objection #1: "We're not sure we need real-time observability"

**Root Issue:** Customer doesn't understand the value of transparency.

**Your Response:**
> "Fair question. Most healthcare systems don't think they need observability until something breaks. But think about it differently: What if you could see exactly where every second goes in your most critical workflows? Would that change how you optimize? Would that help with compliance audits? Would that reduce your dependency on vendor support? That's what observability does."

**Follow-up:**
> "In Phase 1, we'll show you the actual data. You'll see where time is spent. I bet you'll find optimization opportunities you didn't know existed."

---

### Objection #2: "Your SLOs might not apply to our workload"

**Root Issue:** Customer is right—Phase 1 proves actual performance.

**Your Response:**
> "That's exactly why we have Phase 1. For the first month, we measure actual performance in your environment. You see the real P99s. Then Phase 2 we guarantee based on what you actually see. We're not promising something unrealistic. We're guaranteeing what we can prove."

---

### Objection #3: "Other vendors offer similar performance"

**Root Issue:** Customer doesn't understand the transparency advantage.

**Your Response:**
> "They might. But ask them this: Can you see the traces proving it? Can you give your audit team access? Can you have a third party verify independently? Most vendors answer 'no' to all three. We answer 'yes' to all three. That's the difference between promises and proof."

---

### Objection #4: "We've been burned by vendors before"

**Root Issue:** Customer has trust issues—understandable in healthcare.

**Your Response:**
> "I hear you. That's exactly why we built HDIM the way we did. We're not asking you to trust our word. Trust the data instead. You get 30-day trace history. You can audit yourself. That's how we rebuild trust—with transparency, not promises."

---

### Objection #5: "What if the dashboard is down?"

**Root Issue:** Valid technical concern—dashboard uptime matters.

**Your Response:**
> "Jaeger is highly available. But honestly, if the dashboard went down, we wouldn't charge you for that period. SLOs require observability. No observability = no way to measure = you get automatic credit. That's built into the contract."

---

## Preparation Checklist (For Feb 20-25 Training)

### Knowledge
- [ ] Read: `PHASE2_PILOT_OBSERVABILITY_DASHBOARD.md` (customer guide)
- [ ] Read: `PHASE2_PILOT_CONTRACT_SLO_LANGUAGE.md` (legal terms)
- [ ] Read: `PHASE2_COMPLETE_SUMMARY.md` (infrastructure overview)
- [ ] Understand: All 4 SLO metrics and their targets
- [ ] Understand: Phase 1 (baseline) vs Phase 2 (guarantee) structure
- [ ] Understand: Service credit calculation (5-10% monthly)
- [ ] Understand: Competitive advantages vs traditional vendors

### Skills
- [ ] Demo Skill: Navigate Jaeger dashboard smoothly
- [ ] Demo Skill: Interpret trace waterfall (identify bottlenecks)
- [ ] Demo Skill: Explain P50/P95/P99 latency metrics
- [ ] Demo Skill: Answer technical questions about traces
- [ ] Sales Skill: Deliver 30-minute discovery call script flawlessly
- [ ] Sales Skill: Handle 5 common objections effectively
- [ ] Sales Skill: Position SLOs as differentiation

### Tools & Access
- [ ] [ ] Jaeger dashboard access (staging environment)
- [ ] [ ] Prospect credentials generated
- [ ] [ ] Demo script reviewed and practiced
- [ ] [ ] Sales collateral printed/digital ready
- [ ] [ ] Customer contract template available
- [ ] [ ] Sample traces pre-loaded in Jaeger (for demo)

### Practice
- [ ] [ ] Full script rehearsal (mock call with colleague)
- [ ] [ ] Jaeger demo walkthrough (smooth, <10 minutes)
- [ ] [ ] Handle mock objections (confident responses)
- [ ] [ ] Pitch observable SLOs (clear, compelling)
- [ ] [ ] Close on pilot (clear next steps)

---

## First Month Targets (March 1-31)

### Activity Goals
- **Discovery Calls:** 50-100 calls
- **Qualified Prospects:** 10-15
- **LOI Prepared:** 3-5
- **LOI Signed:** 1-2

### Revenue Goals
- **Total Committed:** $50-100K
- **Average Deal Size:** $50K
- **Close Rate:** 5-10% of calls → pilot

### Success Metrics
- **Positioning Resonance:** >80% of prospects understand observable SLOs
- **Demo Effectiveness:** >70% request pilot after live dashboard demo
- **Objection Handling:** >80% of objections resolved in call
- **Customer Satisfaction:** >90% recommend to peers

---

## Resources During Pilot

### Support During March Calls
- **Product Lead:** Available for technical questions (Slack #sales-support)
- **Engineering Lead:** Available for deep technical dives (by request)
- **Customer Success:** Coordination for pilot onboarding
- **Marketing:** Case study materials, competitive data

### Ongoing Training
- **Weekly Sync:** Friday 2 PM (debrief, feedback, refinement)
- **Slack Channel:** #phase2-execution (live updates, wins, blockers)
- **Recorded Demos:** Examples of good/great calls (for learning)
- **Script Refinement:** Iterate based on real prospect feedback

---

## Final Thoughts

**You're about to sell the most transparent healthcare software product ever created.** Traditional competitors have had opacity as their advantage (vendors always say "trust us").

HDIM inverts that. **Transparency is now the advantage.** Customers can verify we deliver. Competitors can't match that without completely redesigning their product.

**Your job is to make healthcare customers understand what transparency means and why they should demand it from all their vendors going forward.**

Good luck. Let's go close some deals.

---

**Generated:** February 14, 2026
**Training Timeline:** Feb 20-25, 2026
**Pilot Execution:** Mar 1-31, 2026
**First Target:** 50-100 discovery calls, 1-2 LOI signings, $50-100K revenue

