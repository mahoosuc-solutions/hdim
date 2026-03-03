# PHASE 3: WINS — Execution Plan
## April 1 – May 31, 2026

**Objective:** Deploy first pilots successfully, prove ROI with real customer data, publish case studies,
expand pipeline from Phase 2 momentum, and begin Series A positioning.

**Revenue Target:** $150–300K ARR committed by May 31
**Customer Target:** 3 pilots live, 2–3 proving measurable ROI

---

## PHASE 3 OVERVIEW

### Critical Path from Phase 2

Phase 3 launches with the assets from Phase 2 execution:

| Phase 2 Output | Phase 3 Dependency |
|----------------|-------------------|
| 1–2 pilot contracts signed (LOI/contract) | Customer 1 kickoff Apr 1 |
| 50–100 discovery calls completed | Warm pipeline to close 2 more pilots |
| Sales playbook validated | VP Sales running independently |
| Marketing website live | Lead capture funnel active |
| Sales collateral finalized | Proposal → close in < 2 weeks |

### Timeline at a Glance

```
APRIL                                    MAY
┌─────────────────────────────────────────────────────────────────┐
│ Week 1 (Apr 1-7)    │ Week 2 (Apr 8-14)  │ Week 3 (Apr 15-21) │
│ Customer 1 kickoff  │ Close pilot 2 & 3  │ C1 training done   │
│ Implementation eng  │ Pipeline expansion │ C1 production live │
├─────────────────────────────────────────────────────────────────┤
│ Week 4 (Apr 22-28)  │ Week 5 (Apr 29-May 5) │ Week 6 (May 6-12) │
│ C2+C3 kickoffs      │ Phase review (3 live) │ ROI data emerging  │
│ BDR onboarded       │ Feature backlog       │ Case study draft   │
├─────────────────────────────────────────────────────────────────┤
│ Week 7 (May 13-19)  │ Week 8 (May 20-26) │ Week 9 (May 27-31) │
│ Case study 1 live   │ Series A prep begins│ Phase 4 planning   │
│ C1 renewal convo    │ Investor updates    │ Recap + handoff    │
└─────────────────────────────────────────────────────────────────┘
```

### Success Metrics

| Metric | Target | Owner | Tracking |
|--------|--------|-------|---------|
| Pilots deployed | 3 | VP Sales + Customer Success | Implementation tracker |
| Pilots proving ROI | 2+ (≥35% gap closure improvement) | Customer Success | ROI dashboard |
| ARR committed | $150–300K | VP Sales | Sales CRM |
| Discovery calls (cumulative) | 100+ | Sales team | CRM |
| New pilots signed | 2–3 additional | VP Sales | CRM |
| Case studies published | 1–2 | Marketing | Content calendar |
| Website leads | 20–30/month | Marketing | Analytics |
| Customer NPS | 40+ | Customer Success | NPS survey |
| Series A investor updates sent | 3+ | CEO | Investor log |

---

## DETAILED WEEK-BY-WEEK EXECUTION

---

### WEEK 1: APRIL 1–7 — CUSTOMER 1 PILOT KICKOFF

**Theme:** Ship-day for the first customer. Perfect the implementation playbook.

#### Monday, April 1 — Customer 1 Kickoff Call

**Owner:** VP Customer Success + Integration Engineer
**Duration:** 90-minute call

**Agenda:**
1. Welcome and introductions (15 min)
2. Pilot success criteria agreement (15 min)
   - Gap closure improvement target: ≥35%
   - Data validation milestone: Week 2 (10K sample patients passing)
   - Go-live milestone: April 21
   - 30-day ROI report: May 1
3. Technical walkthrough: what we're building (15 min)
   - FHIR credential setup steps
   - Data extraction test plan
   - User account provisioning
4. Week 1 action items (30 min)
   - Customer IT contact confirmed
   - FHIR server credentials shared to HDIM secure channel
   - Test patient cohort (500–1K patients) identified
   - Admin user accounts requested
5. Communication cadence agreed (15 min)
   - Daily async standups (Slack or email)
   - Weekly 30-min check-in calls (Tuesdays)

**Deliverables by EOD:**
- [ ] Pilot success criteria signed off (email confirmation)
- [ ] FHIR credentials in HDIM secure vault
- [ ] Integration engineer assigned and introduced
- [ ] Customer Slack channel created

---

#### Tuesday–Friday, April 2–4 — Week 1 Integration Sprint

**Owner:** Integration Engineer
**Goal:** FHIR connection live, first data flowing

**Day-by-Day:**

| Day | Task | Success Criterion |
|-----|------|------------------|
| Tue Apr 2 | FHIR credential validation | GET /metadata returns CapabilityStatement |
| Tue Apr 2 | Test patient extraction (500 patients) | JSON bundle received, parsed |
| Wed Apr 3 | Full FHIR resource mapping | Patient, Condition, Observation, Encounter mapped |
| Wed Apr 3 | Data quality baseline report | Pass rate > 95% on demographics, diagnoses |
| Thu Apr 4 | HDIM services configured for customer tenant | Services start, health checks pass |
| Thu Apr 4 | First care gap evaluations running | ≥1 HEDIS measure evaluating correctly |
| Fri Apr 5 | Integration status report sent to customer | Report shows: records loaded, pass rate, gaps found |

**Integration Engineer Checklist:**
- [ ] FHIR R4 endpoint authenticated (OAuth2 or backend service JWT)
- [ ] Bulk data API enabled for initial full load
- [ ] Patient resource mapping: id, name, DOB, gender, insurance ID
- [ ] Condition resources: ICD-10 codes mapping to HEDIS logic
- [ ] Observation resources: lab results (HbA1c, lipids, etc.) mapping
- [ ] Encounter resources: visit dates, providers, settings
- [ ] HDIM multi-tenant schema: customer tenant ID created, row-level security verified
- [ ] Care gap evaluation: first measure evaluating (e.g., Diabetes HbA1c Control - CDC)

---

#### Thursday–Friday, April 3–5 — Pipeline Expansion Sprint

**Owner:** VP Sales + Sales Team
**Goal:** Convert Phase 2 warm leads to pilot conversations

**Target List Refresh:**
- Pull top 15 warm leads from Phase 2 discovery calls (expressed interest, not yet signed)
- Prioritize by: budget authority confirmed + HEDIS deadline pressure (annual cycle)
- Remove leads with: 12+ month decision timeline, no IT sponsorship

**Outreach Sequence (Days 3–5):**
- Email 1: "Following up — sharing Customer 1 pilot terms" (personalized, not template)
- LinkedIn message: "We kicked off our first pilot this week — sharing what we learned"
- Call attempt: Book 20-minute follow-up call (discovery to proposal)

**Deliverables by EOD Friday April 5:**
- [ ] 15 warm leads contacted
- [ ] 3–5 calls booked for Week 2
- [ ] 2 proposals drafted (based on Phase 2 discovery notes)

---

### WEEK 2: APRIL 8–14 — CLOSE PILOTS 2 AND 3

**Theme:** Pipeline conversion. Two more pilots signed before April 15.

#### Monday, April 7 — Customer 1 Week 1 Recap

**Format:** 30-minute call with customer IT + Quality leads

**Agenda:**
1. Week 1 data extract results (records loaded, quality pass rate)
2. First care gap sample — walk through 5 real patient gaps
3. Week 2 plan (full population load, all HEDIS measures enabled)
4. Open questions

**Success Checkpoint:**
- Data quality pass rate ≥ 95%? → Continue
- Pass rate 85–94%? → Root cause data quality issues, proceed with caveats
- Pass rate < 85%? → Escalate, pause Week 2 load, diagnose

---

#### Tuesday–Wednesday, April 8–9 — Pilot 2 Close

**Owner:** VP Sales
**Target:** Highest-scoring prospect from Phase 2 pipeline (budget confirmed, timeline urgent)

**Closing Process:**
1. Proposal review call (30 min) — walk the prospect through the 2-page pilot proposal
2. Objection handling (using `sales-objection-handler` skill for prep)
3. Contract sent same day
4. IT intro call scheduled for Week 3

**Pilot Proposal Contents (2 pages):**
- Section 1: Their situation (pain we heard, specific measures at risk)
- Section 2: Pilot scope (member count, EHR, timeline, success criteria)
- Section 3: Investment ($2,500/month × 3 months = $7,500 pilot)
- Section 4: What happens after pilot (ROI confirmed → annual contract at $8,500/month)
- Section 5: Timeline (kickoff date, go-live date, 30-day ROI report date)

---

#### Thursday–Friday, April 10–11 — Pilot 3 Close

**Owner:** VP Sales + CEO (if senior executive required)
**Strategy:** Same process as Pilot 2, with CEO on call if prospect is C-suite

**Qualification Checklist Before Proposing:**
- [ ] Budget owner identified (not just VP Quality — needs finance or C-suite sign-off)
- [ ] IT sponsorship confirmed (someone who can enable FHIR access)
- [ ] Timeline real (HEDIS submission deadline or Star Ratings cycle creating urgency)
- [ ] Member size confirmed (≥20K members for pilot ROI to be compelling)

---

#### April 8–11 (Parallel) — Customer 1 Full Population Load

**Owner:** Integration Engineer
**Milestone:** Full patient population loaded and validated by April 11

**Checklist:**
- [ ] Bulk data API load: full patient population
- [ ] Validation: demographics 95%+ pass, clinical data 90%+ pass
- [ ] All 52 HEDIS measures configured
- [ ] Coordinator dashboard seeded with real gap data
- [ ] Financial tracking dashboard: showing estimated quality bonus impact
- [ ] User accounts created for Quality Coordinator team (3–5 users)

---

### WEEK 3: APRIL 15–21 — CUSTOMER 1 TRAINING AND GO-LIVE

**Theme:** Customer 1 goes live. First real-world usage begins.

#### Monday–Tuesday, April 15–16 — Customer 1 Training

**Owner:** Customer Success Manager (or VP Customer Success)
**Duration:** 2 × 90-minute sessions

**Session 1: Quality Coordinator Team (April 15)**
- [ ] Dashboard walkthrough (care gap list, prioritization, patient detail view)
- [ ] Outreach workflow (start outreach, track progress, close gap)
- [ ] Provider notification preview (show the clinical narrative format)
- [ ] Financial dashboard (bonus tracking — show their numbers)
- [ ] Q&A + hands-on practice (each coordinator closes 3 test gaps)

**Session 2: Leadership Team (April 16)**
- [ ] Executive summary dashboard (for VP Quality, CMO)
- [ ] Financial impact tracking (month-to-date quality bonus capture)
- [ ] Gap closure trend reporting
- [ ] How to export for board reporting
- [ ] Success metrics review process (weekly check-in structure)

**Training Completion Checklist:**
- [ ] All coordinator accounts active, first login confirmed
- [ ] Each user has completed 3 practice gap closures
- [ ] Support contact confirmed (Slack + email)
- [ ] First week goals set: 20 real gaps reviewed per coordinator

---

#### Wednesday, April 17 — Customer 1 Production Go-Live

**Milestone:** Customer 1 is officially in production.

**Go-Live Checklist:**
- [ ] All training complete
- [ ] Production environment health checks passing (all HDIM services green)
- [ ] Data freshness: FHIR sync running on schedule (every 15 minutes or real-time)
- [ ] Monitoring alerts active (Prometheus + Grafana dashboards configured)
- [ ] Escalation path documented (customer contact → HDIM on-call contact)
- [ ] Go-live announcement sent to customer team

**CEO/VP Sales Action:**
- Send congratulatory email to customer executive sponsor
- Confirm 30-day ROI review date (May 17)
- Add to company announcement internally ("Customer 1 is live!")

---

#### Thursday–Friday, April 18–19 — Pilot 2 and 3 Kickoff Prep

**Owner:** Integration Engineer + Customer Success
**Goal:** Kickoff calls scheduled, integration prerequisites collected

**Pre-kickoff Checklist for Each Customer:**
- [ ] Technical questionnaire completed (EHR vendor, FHIR version, member count)
- [ ] FHIR credentials requested (or timeline for IT to provide)
- [ ] Test patient cohort identified (500 patients for initial validation)
- [ ] Pilot success criteria agreed (email confirmation)
- [ ] Kickoff call date set (April 22 for Pilot 2, April 24 for Pilot 3)

---

### WEEK 4: APRIL 22–28 — PILOTS 2 AND 3 KICK OFF + BDR ONBOARDED

**Theme:** Scale the implementation engine. Pipeline feeding itself.

#### Tuesday, April 22 — Pilot 2 Kickoff

**Same format as Pilot 1 Week 1 kickoff.** Integration Engineer runs the same playbook.
Integration timeline: April 22–May 5 (2 weeks to full load)
Training: May 6–7
Go-live: May 12

#### Thursday, April 24 — Pilot 3 Kickoff

**Same format.** Parallel track to Pilot 2.
Integration timeline: April 24–May 8
Training: May 9–11
Go-live: May 14

---

#### Wednesday, April 23 — BDR Onboarding

**Owner:** VP Sales
**Purpose:** Business Development Rep (BDR) joins to scale top-of-funnel lead generation

**BDR Week 1 Plan:**
- Day 1: Product overview + 3-hour demo walk-through with VP Sales
- Day 2: Shadow 3 discovery calls (observe, no speaking)
- Day 3: Shadow 2 more calls, debrief on what worked
- Day 4–5: Independent outreach begins (10–15 contacts/day)

**BDR Targets (May pipeline goal):**
- 25 new prospects contacted per week
- 5 qualified discovery calls booked per week for VP Sales
- Build Health Plan prospect list to 50 Tier 2 targets (Phase 3 pipeline)

---

#### Week 4 Parallel — Phase 3 Progress Review

**Date:** April 28 (Monday)
**Owner:** CEO + VP Sales
**Duration:** 60 minutes

**Agenda:**
1. Customer 1 status (live, early metrics)
2. Pilots 2 & 3 integration status (on track?)
3. Pipeline review (proposals out, discovery calls scheduled)
4. Revenue tracking ($7,500 confirmed from Pilot 1, next milestones?)
5. Feature backlog prioritization (what are customers asking for?)
6. May objectives set

---

### WEEK 5: APRIL 29 – MAY 5 — CUSTOMER 1 EARLY ROI DATA

**Theme:** First data that proves the business case. Use it.

#### Customer 1 — 2-Week Progress Check

**Date:** May 1
**Owner:** Customer Success
**Format:** 30-minute call

**Expected Data Points by May 1:**
- Coordinators using dashboard daily? (login frequency)
- Gaps reviewed: target 20/coordinator/day
- Gaps closed: target 15% improvement over baseline within 2 weeks
- Provider engagement: reply rate to HDIM narratives vs. legacy outreach

**If Metrics Are Strong:**
- Capture quotes ("This is the first week I felt ahead of gaps instead of behind them")
- Ask for reference call permission
- Accelerate renewal conversation timeline

**If Metrics Are Weak:**
- Root cause: Training gaps? Workflow adoption? Data quality?
- Emergency 2-hour training session within 3 days
- Daily check-in for 1 week

---

#### Feature Backlog Triage (May 1–2)

**Owner:** CEO + Engineering Lead
**Source:** Customer 1 feedback + Pilots 2 & 3 pre-kickoff surveys

**Framework: RICE Scoring**

| Feature Request | Reach | Impact | Confidence | Effort | Score |
|----------------|-------|--------|------------|--------|-------|
| Bulk outreach (batch close gaps) | High | High | High | Med | Prioritize |
| Mobile-friendly coordinator view | Med | Med | Med | Low | Consider |
| Custom measure builder UI | Low | High | Med | High | Defer |
| EHR alert (CDS Hook) integration | High | High | High | High | Phase 4 |

**Output:** May–June feature sprint plan (3 items max, 2-week sprints)

---

### WEEK 6–7: MAY 6–19 — PILOTS 2 AND 3 GO LIVE + CASE STUDY

**Theme:** Three customers live. First case study published.

#### May 6–7 — Pilot 2 Training
#### May 12 — Pilot 2 Production Go-Live
#### May 9–11 — Pilot 3 Training
#### May 14 — Pilot 3 Production Go-Live

---

#### May 13 — Case Study 1 Draft Complete

**Owner:** Marketing (or CEO ghostwriting)
**Source:** Customer 1 data (6 weeks of production usage)

**Case Study Structure (1 page + 1 data page):**

```
[CUSTOMER NAME] Achieves [X]% Gap Closure Improvement in 6 Weeks
with HDIM HealthData-in-Motion

THE CHALLENGE
[Customer type] — managing [X]K members across [EHR vendor] —
faced HEDIS deadline pressure with a [Y]% gap closure rate.
Coordinators spent [Z hours] per week on manual gap review.

THE APPROACH
HDIM deployed in 4 weeks alongside [EHR vendor] via FHIR R4.
Data: [X]K patients loaded, [Y]% data quality pass rate.
Coordinators trained on smart prioritization dashboard.

THE RESULTS (6 Weeks)
• Gap closure rate: [Y]% → [Z]% (+[delta] points)
• Coordinator time on gap review: [X hrs] → [Y hrs] (-40%)
• Quality bonus impact: $[X]M incremental (annualized)
• ROI on implementation: [X]×

[CUSTOMER QUOTE]
"[Direct quote from coordinator or VP Quality]"
— [Name, Title, Organization]
```

**Publication Plan:**
- PDF version: send to all active prospects + investor pipeline
- Landing page: add to HDIM website as social proof
- LinkedIn post: CEO shares key metrics (use `social:linkedin` skill)
- Email campaign: to 50 warm prospects ("See what happened in our first 6 weeks")

---

### WEEK 8: MAY 20–26 — SERIES A POSITIONING BEGINS

**Theme:** Turn wins into investor narrative. Start Series A pre-work.

#### May 20 — Investor Update #1 (Phase 3 Progress)

**Owner:** CEO
**Recipients:** Current investors + 3–5 strategic angels (warm)
**Format:** 1-page email + deck attachment

**Key Metrics to Lead With:**
- Customers live: 3
- ARR committed: $[X]K (Pilot 1 + 2 + 3 annualized)
- Gap closure improvement: [X]% average across customers
- NPS: [X] (survey results from Customer 1)
- Pipeline: [X] discovery calls, [Y] proposals out

**Narrative Arc:**
1. "We said we'd have 3 pilots by May 31. We have 3 live." (Execution)
2. "Customer 1 is showing [X]% gap closure improvement in 6 weeks." (Validation)
3. "ROI is [X]× on pilot investment — customers are converting to annual." (Unit economics)
4. "We're now building the Series A narrative. Targeting Dec 2026 close." (Forward momentum)

---

#### May 21–23 — Series A Metrics Baseline

**Owner:** CEO + Finance
**Goal:** Document the metrics investors will scrutinize

**Series A Readiness Checklist (Phase 3 Output):**

| Metric | Current | Series A Target (Dec 2026) |
|--------|---------|---------------------------|
| ARR | $[X]K | $500K–$1M |
| Customers | 3 pilots | 5–10 paying |
| NRR (Net Revenue Retention) | N/A (pilots) | >100% |
| CAC | $[X]K | < $50K |
| LTV | $[X]K (annualized) | > 3× CAC |
| Pilot → Annual conversion rate | N/A yet | Target: 80% |
| Time to value | 4 weeks | < 4 weeks |
| Gross margin | ~80% (software) | >75% |
| Churn | 0 (pilots) | < 5% annual |

**Action:** Fill in current numbers. Identify gaps. Plan May 26–31 to close gaps.

---

### WEEK 9: MAY 27–31 — PHASE 3 CLOSE + PHASE 4 PLANNING

**Theme:** Declare wins. Set up Phase 4.

#### May 29 — Phase 3 Final Recap

**Owner:** CEO + VP Sales
**Format:** 60-minute internal review

**Scorecard:**

| Metric | Target | Actual | Status |
|--------|--------|--------|--------|
| Pilots live | 3 | TBD | |
| Pilots proving ROI | 2+ | TBD | |
| ARR committed | $150–300K | TBD | |
| Case studies published | 1–2 | TBD | |
| Discovery calls (cumulative) | 100+ | TBD | |
| Website leads | 20–30/month | TBD | |
| Customer NPS | 40+ | TBD | |
| Series A investor updates sent | 3+ | TBD | |

**Phase 3 → Phase 4 Handoff:**
1. What worked in the sales playbook? (document → BDR training)
2. What features did customers actually use? (inform Phase 4 roadmap)
3. Which prospects are closest to annual contracts? (Phase 4 closes)
4. Team gaps? (hiring plan for June–July)

---

#### May 30–31 — Phase 4 Planning Sprint

**Owner:** CEO + Executive Team
**Outputs:**
- [ ] Phase 4 execution plan drafted (June–Dec 2026)
- [ ] Q2 hiring plan finalized (Customer Success engineer, BDR 2)
- [ ] Product roadmap for Q2–Q3 (3-month sprint plan)
- [ ] Series A timeline locked (target Dec 2026 raise)
- [ ] Board update prepared

---

## RESOURCE PLAN

### Team Capacity (April–May 2026)

| Role | April Allocation | May Allocation | Notes |
|------|-----------------|----------------|-------|
| CEO | 30% customers, 30% sales, 40% strategy | 20% customers, 20% sales, 60% Series A | |
| VP Sales | 60% closing pilots 2&3, 40% pipeline | 50% pipeline expansion, 50% customer success | |
| Integration Engineer | 100% Customer 1 (Apr), overlap C2+C3 | 80% C2+C3, 20% feature work | |
| Customer Success | 80% Customer 1 onboarding, 20% planning | 60% C2+C3, 40% ROI tracking | |
| BDR (new, Apr 23) | 40% ramp (onboarding) | 80% active outreach | |

### Budget Allocation (April–May)

| Category | April | May | Total |
|----------|-------|-----|-------|
| Sales team scaling (BDR) | $5K | $8K | $13K |
| Customer success | $10K | $10K | $20K |
| Marketing (case study, content) | $3K | $5K | $8K |
| Infrastructure / ops | $2K | $2K | $4K |
| Travel (customer kickoffs, if on-site) | $3K | $2K | $5K |
| **Total** | **$23K** | **$27K** | **$50K** |

---

## RISK REGISTER

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Customer 1 data quality issues delay go-live | Medium | High | Pre-load validation in Week 1; rollback plan if pass rate < 85% |
| Pilot 2 or 3 fails to close in Week 2 | Medium | High | Pipeline has 5+ warm leads; close 1 if 2 fall through |
| Customer 1 adoption low (coordinators not using) | Low | High | Daily check-ins Week 1–2; emergency training plan ready |
| Integration engineer overwhelmed (3 simultaneous) | Medium | Medium | Stagger kickoffs by 2 weeks; contract eng resource on standby |
| Feature requests scope-creep the roadmap | High | Medium | RICE framework triage; max 3 features per sprint |
| Series A investor interest soft | Low | Medium | Phase 3 metrics must prove unit economics; prepare financial model early |

---

## PHASE 3 SUCCESS DEFINITION

**Phase 3 is successful when:**

1. **3 pilots are live and actively used** — coordinators logging in daily, gaps being reviewed
2. **At least 2 pilots show measurable ROI** — ≥35% gap closure improvement vs. baseline
3. **ARR committed ≥ $150K** — Pilot 1 converting to annual, Pilots 2+3 confirmed
4. **Case Study 1 published** — with real customer data, real quote, real ROI numbers
5. **Series A narrative started** — investor update sent, metrics documented, Q4 timeline set
6. **Phase 4 fully planned** — team, product, pipeline all ready for June acceleration

---

## KEY DOCUMENTS (Phase 3 Inputs)

| Document | Location | Purpose |
|----------|----------|---------|
| Phase 2 Execution Plan | `docs/PHASE_2_TRACTION_EXECUTION_PLAN.md` | Week-by-week Phase 2 context |
| Year 1 Strategic Roadmap | `docs/YEAR_1_STRATEGIC_ROADMAP.md` | Phase 4 onward targets |
| Sales Discovery Call Script | `docs/PHASE_2_VP_SALES_DISCOVERY_CALL_SCRIPT.md` | VP Sales + BDR framework |
| Pilot Proposal Template | `docs/` | 2-page pilot proposal |
| ROI Calculator | Landing page `/` | Customer-facing financial model |
| Pricing Page | Landing page `/pricing` | Pilot → Annual path |

---

_Last Updated: March 2, 2026_
_Version: 1.0_
_Status: Active — Execution begins April 1, 2026_
_Owner: CEO + VP Sales_
