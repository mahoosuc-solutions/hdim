# HDIM Discovery Demo Script
## 30-Minute Guided Walkthrough — Clinical Quality Platform

**Version:** 1.0 (February 2026)
**Audience:** VP Sales, Customer Success, Founders
**Demo Environment:** `hdim-demo-*` containers (demo_admin credentials)
**Platform URL:** http://localhost:4200 (or your live demo hostname)

---

## Before the Call (5 minutes prep)

### Pre-call checklist
- [ ] Demo containers running: `docker compose -f docker-compose.demo.yml up -d`
- [ ] Seed data loaded (500+ patients, care gaps, quality scores visible)
- [ ] Browser: Chrome, logged in as `demo_admin` / `Demo@12345`
- [ ] Tabs pre-open: Dashboard, Patient List, Care Gaps, Quality Measures, Grafana
- [ ] Jaeger open in background: http://localhost:16686
- [ ] Know the prospect's role before dialing (CMO vs. Coordinator vs. IT = different angles)

### Quick data verify (30 seconds)
```bash
# Confirm seed data is healthy
curl -s -H "X-Tenant-ID: demo-tenant" http://localhost:8084/patient/api/v1/patients?pageSize=1 \
  | jq '{total: .totalElements, sample: .content[0].name}'
# Expected: total > 100, sample patient name visible
```

---

## The 30-Minute Call Structure

| Minute | Section | Purpose |
|--------|---------|---------|
| 0–5 | Discovery questions | Understand their pain before showing anything |
| 5–8 | Platform overview (1 screen) | Orient them without overwhelming |
| 8–15 | Core workflow walkthrough | Patient → Care Gaps → Quality Score |
| 15–20 | Role-specific value | Tailor to their title/pain |
| 20–25 | Observable SLOs | The differentiator (live Jaeger/Grafana) |
| 25–30 | Next steps + fit assessment | Honest close |

---

## SECTION 1: Discovery Questions (Minutes 0–5)

**Do not share your screen yet.** Listen and take notes. These questions shape which parts of the platform to emphasize.

### Opening
> "Before I show you anything, I want to make sure I'm showing you the right things. Can you tell me a bit about where your quality program is today?"

### Diagnostic questions (pick 2–3 based on their role)

**For CMO / VP Quality:**
> "What's your current Star rating, and where do you need it to be by end of year?"
> "How far in advance do you typically know about care gaps before HEDIS submission?"
> "When your board asks for the ROI on your quality program — what do you tell them?"

**For Quality Coordinator:**
> "Walk me through what your Monday morning looks like — how do you decide which care gaps to work on first?"
> "How do you coordinate provider outreach today — phone, EHR messaging, a portal?"
> "How long does it take your team to produce the HEDIS submission package?"

**For CFO / Finance:**
> "Do you have visibility into how much quality bonus revenue you're leaving on the table?"
> "What did your quality program cost last year, and what did you get back in bonus capture?"

**For IT / Analytics:**
> "What's your FHIR adoption status? Are you on FHIR R4 yet?"
> "What year are you targeting for ECDS readiness?"
> "How many EHR systems are you integrating across your network?"

### What to listen for
- **Star rating target** → drives how to frame gap closure ROI
- **HEDIS deadline pressure** → drives urgency messaging
- **Manual vs. automated** → drives efficiency gain framing
- **Existing vendor** → "We work alongside or replace, depending on what's working"

---

## SECTION 2: Platform Overview — 1 Screen (Minutes 5–8)

**Share screen. Open the main dashboard.**

> "Let me orient you to what you're looking at. This is the HDIM Clinical Quality Platform — one view, live data, across all of your quality programs."

**Point to key areas on the dashboard:**

```
TOP-LEFT: Patient population summary
  → "You're looking at [X] members across [Y] tenants in this demo.
     In your environment, this is your real population."

CENTER: Care gap summary panels
  → "These are active care gaps — prioritized by a combination of
     clinical risk, engagement likelihood, and HEDIS submission impact.
     Not a flat list. A prioritized worklist."

RIGHT PANEL: Quality measure scores
  → "These are your measure-level scores in real time — not from
     last quarter's report, not from your vendor's portal.
     From your data, right now."

BOTTOM: SLO performance indicators
  → "We'll come back to these. This is the part our customers
     call their favorite — real-time proof that we're doing what we said."
```

**Transition:**
> "Let me show you how a quality coordinator actually uses this on a Monday morning."

---

## SECTION 3: Core Clinical Workflow (Minutes 8–15)

This is the heart of the demo. Walk through **one patient's story** — not a feature list.

### Step 1: Start with a high-risk patient (2 minutes)

Navigate to **Patient List** (or search for "Maria Garcia" / first patient in the seeded list).

> "Let's say Maria Garcia is one of your diabetic members. She's 58, has Medicare Advantage, and your team just got a flag that she's approaching a critical window for HbA1c control."

Open the patient record. Point to the risk score and flags.

> "HDIM has already calculated her composite risk — it's pulling from her claims history, her FHIR clinical record, and her prior care gap closure patterns. Your coordinator doesn't have to calculate this. It surfaces automatically."

**What to highlight:**
- Risk score is automated, not manual
- Multi-source data (claims + FHIR + prior history)
- HIPAA-compliant display (audit trail happening in the background)

---

### Step 2: See her care gaps (2 minutes)

Click into **Care Gaps** tab for this patient.

> "Here are Maria's active care gaps. Notice what's different from what you're probably used to — these aren't just measure codes. Each gap has three things attached:
> - The clinical context: why this gap matters for Maria specifically
> - The closure likelihood: based on her prior engagement history
> - The financial value: how much this gap is worth to your Star rating"

Point to the **top gap** (e.g., HbA1c-control).

> "HbA1c control is her highest-priority gap right now. Her last test was 8 months ago, her current result is 8.2% — above the 7% target for HEDIS. Closing this gap in the next 6 weeks is worth approximately [X points] toward your Star rating."

**Key differentiator to name:**
> "Most vendors give you a gap code. We give you the clinical reason and the financial impact. That's the difference between a list your coordinators scroll past and a list they act on."

---

### Step 3: Show the coordinator's action (2 minutes)

> "Now here's what your coordinator does. Instead of picking up the phone, building an EHR message from scratch, or waiting for the provider to notice — HDIM generates the outreach automatically."

Navigate to the **Outreach / Notification panel** for this patient.

> "This message was pre-generated using the patient's clinical context. The provider reads: 'Maria Garcia — diabetes management. Last HbA1c 8.2% in June 2025. HEDIS submission window closes February 28. Recommend: order A1C lab, consider medication adjustment.' That's not a generic alert. That's a clinical note."

Point to the one-click action:

> "One click from your coordinator queues this for delivery — EHR message, patient portal notification, or SMS depending on what's configured for this member. No manual typing. No context switching."

---

### Step 4: Quality measure score (2 minutes)

Navigate to **Quality Measures** view.

> "Now zoom out. That one gap closure for Maria — it's tracked here, in real time, against your measure targets."

Point to the **HbA1c-control measure panel**.

> "Before we closed this gap: measure compliance was 64%. After it's resolved: it moves to 65.3%. Doesn't sound like much for one patient — but across your 50,000 member plan, every point is real revenue."

Click on the measure to show the **financial impact calculation** (if available in demo seed data).

> "This is what your CFO has been asking for. Not 'we closed X gaps.' But 'we captured $Y in quality bonus revenue, and here's exactly which gaps drove it.'"

**Pause here.**
> "Does this match the kind of visibility your team has been looking for?"

*(Listen. Note their response — it shapes the SLO section.)*

---

## SECTION 4: Role-Specific Value (Minutes 15–20)

Based on their title and what you learned in Section 1, pick **one of these paths**.

---

### Path A: CMO / VP Quality (Star rating, strategic ROI)

Navigate to **Quality Dashboard → Star Rating Projection panel**.

> "This is your projected Star rating, updated every time a gap closes. Today you're tracking at 3.4 stars. Based on the gaps in your pipeline — if your team closes 40% of the active gaps in the next 90 days — this model projects you at 3.8 stars by submission."

> "That half-point improvement is worth approximately 3.4% of your Medicare Advantage bonus revenue. For a plan your size, that's [X million dollars]. That's the answer you give your board."

**CMO takeaway line:**
> "Your current vendor tells you your score at the end of the year. We tell you your projected score today, and we show you exactly which gaps to close to get there."

---

### Path B: Quality Coordinator (Daily workflow, time savings)

Stay on the **Care Gap Worklist**.

> "Your coordinators today are probably starting Monday with a spreadsheet or a portal export. They manually prioritize, manually draft outreach, manually track which providers responded."

> "In HDIM, Monday morning looks like this: 15 gaps need action this week, ranked by urgency. Pre-drafted outreach for each one. Click to send, or route to the right coordinator. Progress tracked automatically."

> "We've seen quality teams cut their HEDIS prep time by 40%. That's not automation hype — that's coordinators doing 2-day tasks in 4 hours because the system did the research."

**Coordinator takeaway line:**
> "HDIM is the system your coordinators will actually use — because it tells them exactly what to do next, not just what's open."

---

### Path C: IT / Analytics (FHIR, integration, security)

Navigate to **Admin → System Health** or open Swagger UI in a new tab.

> "From an integration standpoint, HDIM is FHIR R4 native. You push patient data via FHIR Bundles or RESTful FHIR endpoints — no custom ETL, no proprietary file formats."

> "Here's the FHIR endpoint for Patient resources — standard R4 format, returns compliant bundles. This is what ECDS will require by 2030. You're building toward that standard now."

Show the multi-tenant isolation point:

> "Every tenant — every health plan, every provider group — has database-level isolation. Not application-layer filtering. Actual separate schemas. PHI cannot cross tenant boundaries."

**IT takeaway line:**
> "HDIM is FHIR-first, not FHIR-retrofitted. Integration takes weeks, not months. And ECDS readiness is built in — not a future roadmap item."

---

### Path D: CFO / Finance (ROI, bonus capture, board reporting)

Navigate to **Dashboard → Quality Bonus Tracker** (or Grafana financial panel).

> "Let me show you the dashboard your finance team has been waiting for."

> "Quality bonuses captured this month: $[X]. Quality bonuses remaining in the pipeline: $[Y]. Measure-level breakdown: HbA1c control is your highest-value open opportunity."

> "When your board asks 'what's the ROI on our quality program?' — this is the answer. Not a clinical metric. A dollar figure. Updated every day."

> "The pilot model is straightforward: you define success criteria — Stars improvement, gap closure rate, bonus capture target. We commit to them in writing, with automatic service credits if we miss. You see progress in this dashboard, not in our quarterly report."

**CFO takeaway line:**
> "You're not buying a quality tool. You're buying a quality bonus engine with a financial guarantee."

---

## SECTION 5: Observable SLOs — The Differentiator (Minutes 20–25)

This is HDIM's most distinctive capability. Show it deliberately.

> "Before we wrap up, I want to show you something that no other quality vendor offers. This is why we're different — not in our slides, but in how we work with customers."

**Open Jaeger:** http://localhost:16686

> "This is Jaeger — an open-source distributed tracing system. Every request through our platform is traced here: how long it took, which services processed it, where any latency came from."

> "The reason I'm showing you this is: you can see it. Not us. You. In your pilot contract, we commit to observable SLOs — if our care gap API doesn't return results in under 200ms at the 95th percentile, you see it here before we do."

Click on a recent trace and show the waterfall.

> "This trace shows a care gap query. 87 milliseconds end-to-end. Our SLO is 200ms. We're running at 43% of our SLO — and you can verify that yourself, any time, from your own browser."

**Open Grafana:** http://localhost:3001

> "This is Grafana. Your pilot dashboard. Your engineering team — or just your VP Quality — can bookmark this page and watch our performance in real time."

Point to the response time panel.

> "P95 latency. Availability. Error rate. These are our SLO commitments, visible to you, not just to us. If we miss — automatic service credits kick in. We write that into the contract before you sign."

**SLO differentiator line:**
> "Every other vendor shows you a dashboard in their portal that they control. We give you access to our instrumentation. That's not confidence — that's proof."

---

## SECTION 6: Next Steps + Honest Close (Minutes 25–30)

**Do not oversell. Be direct.**

> "That's the platform. Let me be honest with you about fit."

### If it's a strong fit:

> "Based on what you told me — [their specific pain point] — I think HDIM is a strong match. Here's what I'd suggest:
>
> Within 48 hours, I'll send you a written fit assessment — what we heard, where we'd focus in your environment, and a proposed pilot structure. No surprise timelines.
>
> The pilot is 60–90 days. Defined success criteria before you sign. You'll have live HEDIS baselines in 30 days — not 90."

**Schedule before hanging up:**
> "Can we put a 30-minute call on the calendar for [2-3 days out] to walk through the fit assessment together?"

---

### If there are questions / objections:

**"We already have a quality vendor."**
> "That's the most common thing we hear. Most of our prospects do. The question we'd ask is: does that vendor give you real-time financial ROI visibility? Does it show you projected Star ratings today? We work alongside existing tools or replace them — but we make that recommendation in writing after the discovery call, not on the call."

**"What's the price?"**
> "It's member-volume based. We build a model specifically for your population after this call — no generic pricing. I can have a preliminary model to you within 48 hours based on your member count."

**"Our IT team needs to approve any new integrations."**
> "Absolutely — let's loop them in early. Our integration is FHIR R4 and API-first — no proprietary connectors. Your IT team will want the FHIR spec and security documentation before any decision. I'll include both in the fit assessment."

**"We're not ready to make a decision right now."**
> "That's completely fine. The next step isn't a decision — it's a written fit assessment from us, no obligation. If it helps your team evaluate options, that's the point."

---

### If it's not a fit:

> "Based on what you've shared, I want to be honest — [specific reason HDIM isn't a fit]. I don't want to waste your time on a pilot that won't solve what you're actually trying to fix. I'd recommend [alternative approach or vendor type] for your situation."

*(This builds trust. They'll remember you didn't oversell them.)*

---

## Post-Call: 48-Hour Follow-Up Template

**Subject:** HDIM Fit Assessment — [Company] Quality Program

> Hi [Name],
>
> Thanks for the conversation today. Here's what I heard:
>
> **Your situation:**
> - [Current Star rating and target]
> - [Key workflow pain: manual, reactive, etc.]
> - [Biggest near-term pressure: HEDIS deadline, Star improvement, ROI visibility]
>
> **Where HDIM fits:**
> - [Specific capability that addresses their pain]
> - [Expected outcome: X% gap closure improvement, Y projected Star improvement]
>
> **Where HDIM doesn't fit (if applicable):**
> - [Honest limitation]
>
> **Proposed pilot structure:**
> - Duration: 60–90 days
> - Success criteria: [specific to their goals]
> - Timeline: Pilot environment in 2 weeks, first HEDIS baselines in 30 days
> - SLO contract: Observable, automatic service credits
>
> **Next step:** 30-minute call [date/time] to walk through this together.
>
> — [Your name]
> sales@mahoosuc.solutions

---

## Demo Environment Reference

### Credentials
| Role | Username | Password | What They See |
|------|----------|----------|---------------|
| Admin | `demo_admin` | `Demo@12345` | Full platform, all tenants |
| CMO | `demo_cmo` | `Demo@12345` | Executive dashboard, quality scores |
| Coordinator | `demo_coordinator` | `Demo@12345` | Care gap worklist, outreach tools |
| Provider | `demo_provider` | `Demo@12345` | Patient-level care summaries |
| Analyst | `demo_analyst` | `Demo@12345` | Reports, measure trends |

### Key Demo URLs
| Purpose | URL |
|---------|-----|
| Main portal | http://localhost:4200 |
| FHIR Patient API | http://localhost:8085/fhir/Patient |
| Patient $everything | http://localhost:8085/fhir/Patient/{id}/$everything |
| Swagger UI (FHIR) | http://localhost:8085/fhir/swagger-ui/index.html |
| Swagger UI (Patient) | http://localhost:8084/patient/swagger-ui/index.html |
| Jaeger tracing | http://localhost:16686 |
| Grafana dashboard | http://localhost:3001 (admin / hdim-grafana-secret) |
| Prometheus metrics | http://localhost:9090 |

### Seed data summary
| Data type | Count | Tenant |
|-----------|-------|--------|
| Patients | 500 | demo-tenant |
| Care gaps (open) | ~1,200 | demo-tenant |
| Quality measures | 12 HEDIS measures | demo-tenant |
| FHIR resources | 14 resource types per patient | demo-tenant |

### If something breaks mid-demo
```bash
# Quick health check
for port in 8084 8085 8086 8087; do
  echo -n "Port $port: "
  curl -sf http://localhost:$port/actuator/health | jq -r .status 2>/dev/null || echo "DOWN"
done

# Restart all demo services
docker compose -f docker-compose.demo.yml restart

# Re-seed data if empty
./scripts/seed-demo-data.sh --tenant demo-tenant --patients 500
```

---

## Key Talking Points by Audience

### For every call
- "We define success criteria before you sign — no surprises."
- "Observable SLOs: you verify our performance yourself, not in our quarterly report."
- "Pilot results in 30 days, not 90."

### CMO / VP Quality
- "Projected Star rating today — not last year's result."
- "Which gaps to close to get to 4 stars — specific, prioritized, financial."
- "Quality program becomes provider partnership, not compliance burden."

### Quality Coordinator
- "15 gaps to work on Monday morning, ranked, with outreach pre-drafted."
- "40% reduction in HEDIS prep time — documented in pilot readout."
- "Coordinators use it because it tells them exactly what to do next."

### CFO / Finance
- "Quality bonus capture tracked to the dollar, visible every day."
- "Board answer: 'We captured $Y in bonuses. Our cost was $X. ROI is Z%.'"
- "Automatic service credits if SLOs are missed — it's in the contract."

### IT / Analytics
- "FHIR R4 native — ECDS-ready by design, not by retrofit."
- "Database-level tenant isolation — not application-layer filtering."
- "API-first — your team integrates in weeks, not months."
- "29 independent databases, Liquibase migrations, Hibernate validation."

---

## What Not to Say

| Don't say | Say instead |
|-----------|-------------|
| "We use AI to..." | "The platform automatically prioritizes gaps by [specific criteria]" |
| "We're HIPAA compliant" | "Here's the audit trail running right now — every PHI access logged" |
| "We'll close more gaps" | "In the pilot, we define what 'success' means together before you sign" |
| "Our platform is enterprise-grade" | "Here's the database-level tenant isolation — you can verify it in the schema" |
| "Trust us" | "You verify it yourself — that's what the Jaeger dashboard is for" |
| "Pricing depends on many factors" | "It's member-volume based. I'll have a preliminary model to you in 48 hours." |

---

*Last updated: February 19, 2026*
*Maintain this document with learnings from each discovery call.*
