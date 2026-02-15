# AI Sales Character System - Quick Start Guide

**Status:** ✅ Live and Ready to Use
**Created:** February 12, 2026
**Environment:** Claude Code (use with `@` mentions or direct skill invocation)

---

## 🚀 TL;DR - Get Started in 60 Seconds

You now have 4 AI sales coaching skills in your Claude Code environment:

| Skill | When to Use | Command |
|-------|------------|---------|
| **sales-discovery-coach** | Practice discovery calls | "Practice a call with a CMO" |
| **sales-objection-handler** | Handle customer objections | "Customer says we're too expensive" |
| **sales-demo-designer** | Design persona demos | "Build a 30-min demo for a CFO" |
| **sales-manager** | Analyze pipeline & strategy | "Score my pipeline: [list deals]" |

**Example Usage:**
```
You: "Practice a discovery call - you're a CMO at a 500K-member health plan"
Coach: [Activates CMO persona, asks opening question]
Coach: "Thanks for taking the call. What prompted this conversation?"
You: [Your response]
Coach: [Provides feedback on pain discovery, qualification, etc.]
```

---

## 📚 The 4 Skills Explained

### Skill 1: Discovery Coach 🎯
**What it does:** Roleplay customer personas and provide feedback on discovery call quality

**5 Available Personas:**
- CMO / VP Quality (primary buyer, HEDIS deadline focus)
- Quality Coordinator (operational, time-pressed)
- Healthcare Provider (clinical, alert-fatigued)
- CFO / Finance Leader (ROI-focused, numbers-driven)
- IT / Analytics Leader (technical, security-focused)

**Scoring Rubric:**
- Pain Discovery (0-20 pts): Did you uncover real pain?
- Qualification (0-20 pts): Is this a real prospect?
- Objection Handling (0-15 pts): How well did you handle concerns?
- Next Steps (0-20 pts): Did you get commitment?
- Credibility & Rapport (0-25 pts): Did they trust you?

**How to Use:**
```
"Practice a discovery call with a CMO at a 500K-member health plan"
"Coach me on this discovery call transcript: [paste]"
"What's a good follow-up question about gap prioritization?"
```

**What You'll Get:**
- Live persona roleplay (or feedback on real transcripts)
- Real-time coaching on questions
- 0-100 call score with specific feedback
- Suggestions for improvement

---

### Skill 2: Objection Handler 🛡️
**What it does:** Turn customer objections into opportunities with reframes and competitive positioning

**6 Common Objections:**
1. **"Your pricing is too high"** (60% of conversations)
   - Reframes: Speed-to-impact, feature comparison, hidden cost analysis
   - Competitive comparisons: Epic, Optum, build-in-house

2. **"We could build this ourselves"** (40% of conversations)
   - Reframes: Opportunity cost, hidden infrastructure costs, data complexity
   - Build vs. buy scorecard

3. **"Security & HIPAA concerns"** (80% of conversations)
   - Address: HIPAA verification, data isolation, encryption
   - Risk mitigation: Phased pilot approach

4. **"Integration is too complex"** (50% of conversations)
   - Address: Pre-built connectors, 2-4 week timeline
   - Data integration decision tree

5. **"We need more time to decide"** (70% of conversations)
   - Reframes: Cost of delay, time to impact, competitive urgency

6. **"We don't have budget"** (50% of conversations)
   - Reframes: ROI-driven reallocation, pilot approach, phased deployment

**How to Use:**
```
"Customer just said 'We'd rather build this ourselves'"
"How do I handle: 'Your pricing seems high compared to Epic'"
"Patient says: 'We're concerned about HIPAA compliance'"
```

**What You'll Get:**
- Specific reframe language (copy-paste ready)
- Competitive positioning data
- ROI calculations
- Checklists for handling each objection

---

### Skill 3: Demo Designer 🎬
**What it does:** Create persona-specific demo pathways (15-min teaser, 30-min standard, 45-min deep-dive)

**5 Persona Demos:**

| Persona | Duration | Main Focus | Features Shown |
|---------|----------|------------|-----------------|
| CMO | 30 min | Predictive gap detection + financial impact | Prediction engine, coordinator dashboard, financial dashboard |
| Coordinator | 20 min | Time savings + provider engagement | Smart dashboard, workflow tracking, engagement metrics |
| Provider | 10 min | Clinical utility + ease of use | Alert examples, patient context, quick response |
| CFO | 20 min | ROI + competitive advantage | Month-by-month tracking, bonus capture, scenario comparison |
| IT | 30 min | Architecture + integration + compliance | System design, data flow, security model, integration timeline |

**Demo Structure (for each persona):**
1. Opening (credibility, differentiation)
2. Problem statement (show their pain)
3. Solution demo (live feature walkthrough)
4. Proof & validation (case study, metrics)
5. Next steps & close (specific action + timeline)

**How to Use:**
```
"Design a 30-minute demo for a CMO at a 300K-member plan"
"What features should I prioritize for a coordinator?"
"Walk me through a 15-minute teaser demo"
"How do I demo to an IT leader?"
```

**What You'll Get:**
- Complete demo scripts (with timing)
- Talking points for each feature
- Interactive elements (what to ask)
- Materials lists (what to have ready)
- Story/narrative frameworks

---

### Skill 4: Sales Manager 👔
**What it does:** Strategic advisor - analyze pipeline, validate strategy, provide coaching

**Strategic Capabilities:**

1. **Market Positioning Check**
   - Primary persona fit: CMO (health plan), focus on HEDIS
   - Secondary personas: Coordinators, IT (influencers)
   - Financial buyer: CFO (needed for approval)
   - Timeline: Focus narrow in Q1, expand in Q2+

2. **Deal Scoring Framework** (0-100 scale)
   - Buyer quality (25 pts): Is this the right person with budget?
   - Pain alignment (25 pts): Does their pain match our solution?
   - Competitive position (20 pts): Are we the frontrunner?
   - Implementation readiness (15 pts): Can they deploy in 4 weeks?
   - Momentum (15 pts): Is this moving forward?

3. **Pipeline Analysis**
   - Stage-by-stage breakdown (discovery → demo → scoping → closed)
   - Probability by stage (20% → 40% → 70% → 90%)
   - Expected revenue calculation
   - Red flags to watch

4. **Messaging Validation**
   - Give transcripts → Get feedback on effectiveness
   - Identify what's working vs. not working
   - Specific recommendations for improvement

5. **Series A Readiness Scorecard**
   - Market validation (40% weight): TAM, SAM, customer interviews ✅
   - Traction & revenue (40%): MRR, customer count, growth rate 🔴 (CRITICAL GAP)
   - Team & execution (10%): VP Sales, technical team, advisors ⚠️
   - Product & compliance (10%): HIPAA, SOC2, uptime ✅

**How to Use:**
```
"Should I focus on health plans or ACOs first in March?"
"Here's my pipeline: [list deals]. Which should I prioritize?"
"Coach me on this customer transcript: [paste]"
"Are we ready for Series A fundraising?"
"Score my 5 deals and tell me what to do"
```

**What You'll Get:**
- Deal scoring (prioritization)
- Pipeline forecast (revenue projections)
- Messaging feedback (what's working)
- Strategic recommendations (focus areas)
- Coaching on execution

---

## 🎯 Usage Patterns

### Pattern 1: Pre-Call Preparation (30 min)

```
Step 1: Identify the persona
- "Who will I be talking to? CMO, CFO, Coordinator?"

Step 2: Design the demo
- "Build a [15/30/45]-min demo for a [persona]"
- Skill: sales-demo-designer

Step 3: Prep for objections
- "What objections might they raise?"
- Skill: sales-objection-handler

Step 4: Practice
- "Let me practice a discovery call"
- Skill: sales-discovery-coach
```

### Pattern 2: Post-Call Coaching (15 min)

```
Step 1: Share transcript
- "Here's my discovery call transcript"
- Skill: sales-discovery-coach

Step 2: Get feedback
- "Score my call and tell me what I missed"
- Returns: 0-100 score + specific feedback

Step 3: Learn from it
- "How should I have handled the price objection they raised?"
- Skill: sales-objection-handler

Step 4: Iterate
- "OK, let me practice that response"
- Skill: sales-discovery-coach (again, with better answer)
```

### Pattern 3: Pipeline Analysis (45 min)

```
Step 1: List deals
- "I have 5 deals in pipeline: [list with size/stage/pain]"

Step 2: Get prioritization
- "Score these and tell me which to focus on"
- Skill: sales-manager

Step 3: Analyze messaging
- "Here are transcripts from 3 calls. Is our messaging working?"
- Skill: sales-manager (messaging validation)

Step 4: Strategic coaching
- "What should our March focus be?"
- Skill: sales-manager (strategic input)
```

### Pattern 4: Daily Coaching (10 min)

```
Use any of the 4 skills for quick questions:

Discovery: "What's a good question about care gap prioritization?"
Objections: "How do I handle a budget constraint?"
Demos: "What features matter most for a coordinator?"
Strategy: "Is this deal worth pursuing?"
```

---

## 💡 Tips for Success

### 1. Be Specific in Your Prompts
❌ "Practice a discovery call"
✅ "Practice a discovery call with a CMO at a 500K-member health plan focused on HEDIS quality bonus"

### 2. Provide Context
❌ "How do I handle this objection?"
✅ "Customer said 'We could build this ourselves.' Here's our conversation so far: [context]. How should I respond?"

### 3. Use for Iteration, Not Just Learning
- Do: Practice → Get feedback → Iterate → Practice again
- Don't: Watch a skill and assume you've learned it

### 4. Combine Skills
- Before call: Use demo-designer + objection-handler to prep
- During call: Use discovery-coach to practice
- After call: Use discovery-coach for feedback + sales-manager for strategic insight

### 5. Share Results With Team
- Record your practice sessions (or take notes)
- Share "what worked" learnings with VP Sales
- Build institutional knowledge, not just individual learning

---

## 📊 Success Metrics - How to Know It's Working

| Metric | Target | Timeline |
|--------|--------|----------|
| Discovery calls completed | 50-100 | March (Week 1-4) |
| Average call score | 80+ | March (Week 2+) |
| Objection handling confidence | 8/10 | March (ongoing) |
| Pipeline close rate | 20-30% | May (Q2 start) |
| Customer testimonial score | 4.5/5 | May (post-pilot) |
| Series A readiness | 7+/10 | August |

---

## 🚀 Quick Command Reference

### Discovery Coach
```
"Practice a discovery call with a [CMO/Coordinator/Provider/CFO/IT]"
"Coach me on this call transcript: [paste]"
"What's a good discovery question about [topic]?"
"Score my call and give me feedback"
```

### Objection Handler
```
"Customer says [objection]. How do I respond?"
"Coach me through a price objection"
"How do I handle: 'We could build this ourselves'"
"What's the competitive positioning vs. Epic?"
```

### Demo Designer
```
"Design a [15/30/45]-minute demo for a [persona]"
"What features should I show a [persona]?"
"Walk me through a teaser demo"
"How do I demo care gap workflow?"
```

### Sales Manager
```
"Score my pipeline: [list deals]"
"Is our messaging working? [paste transcripts]"
"Should I focus on [option A] or [option B]?"
"Are we ready for Series A?"
"Analyze my deal [name]: [description]"
```

---

## 📚 Additional Resources

### Knowledge Base (Used by Skills)
- `PHASE_2_VP_SALES_DISCOVERY_CALL_SCRIPT.md` - Discovery framework
- `PHASE_2_VP_SALES_PERSONA_QUICK_REFERENCE.md` - Persona profiles
- `PHASE_2_CUSTOMER_PERSONA_VALIDATION.md` - Validated buyer personas
- `CUSTOMER_REQUIREMENTS_ANALYSIS.md` - 15 customer interviews
- `SALES_COLLATERAL_TEMPLATES.md` - 8 playbooks + competitive battlecards
- `GTM_STRATEGY_2026.md` - Full go-to-market strategy
- `YEAR_1_STRATEGIC_ROADMAP.md` - 12-month execution plan

### Related Documentation
- **Fundraising:** `PHASE_2_TRACTION_EXECUTION_PLAN.md`
- **Metrics:** `PHASE_2_POSITIONING_VALIDATION.md`
- **Competitive:** Competitive analysis in `GTM_STRATEGY_2026.md`

---

## ✅ Implementation Checklist

- [ ] **Day 1:** Try one skill (start with discovery-coach)
- [ ] **Day 2:** Practice a discovery call with each persona
- [ ] **Day 3:** Handle 6 objections (one objection type per day)
- [ ] **Day 4:** Design 5 demos (one per persona)
- [ ] **Day 5:** Analyze your pipeline and get deal scores
- [ ] **Week 2:** Use skills before real customer calls
- [ ] **Week 3:** Share learnings with VP Sales
- [ ] **Week 4:** Evaluate effectiveness and iterate

---

## 🎓 What Happens Next?

### Phase 2 (March 1-31, 2026): Execution
- Use these skills for daily coaching
- Target: 50-100 discovery calls
- Close: 1-2 pilot LOIs by March 31

### Phase 2+ Roadmap:
- **Phase 2 (March):** Skills help drive discovery calls → 1-2 LOIs
- **Phase 3 (April-May):** Skills help scale pilots → 3 LOIs, $150K ARR
- **Phase 4 (June-Aug):** Skills help expand customer base → 5-7 customers, $300-500K ARR
- **Phase 5 (Sept-Oct):** Series A readiness → 8-10 customers, $500K-$750K ARR
- **Phase 6+ (Nov-Dec):** Series A execution → Close $3-5M Series A

---

## 📞 Getting Help

**If a skill doesn't do what you need:**
1. Try rephrasing the question more specifically
2. Provide more context (e.g., paste transcripts, list deals)
3. Ask for a specific framework (e.g., "Give me the deal scoring rubric")

**If you want to extend the skills:**
- Each skill has 400-530 lines of detailed content
- You can customize personas, objections, or frameworks
- Ask Claude for modifications or extensions

---

**Ready to become a sales superstar? Pick a skill and start! 🚀**

**Recommended First Step:**
```
"Let me practice a discovery call with a CMO at a 500K-member health plan.
I want to practice my opening and pain discovery questions."
```

**Go! 💪**
