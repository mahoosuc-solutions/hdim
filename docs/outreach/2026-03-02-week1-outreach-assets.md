# Week 1 Investor Outreach — Ready-to-Send Assets

**Date:** 2026-03-02
**Status:** Ready to send
**Tracking doc:** [Week 1 Design](../plans/2026-03-02-investor-outreach-week1-design.md)

---

## Track 1 — Healthix Emails

### Email 1: Healthix CEO (Send Monday)

**To:** [CEO name] <[email]>
**Subject:** We spoke in December — here's what's changed

---

Hi [Name],

We had a call in December where I showed you an early prototype of
HealthData-in-Motion. It wasn't ready — the architecture was exploratory
and I knew it when you were looking at it.

Since then I've rebuilt the platform from scratch. What was a Node.js
prototype is now 51 Java/Spring Boot microservices with 1,771 commits,
52 HEDIS measures automated via a CQL engine, and 693 passing tests.
It runs on FHIR R4 natively and deploys in three minutes.

The core problem is the same one you know from the HIE side: care gaps
surface within seconds of a clinical event instead of showing up in
next year's annual report.

We've also built a RHEL 7 branch specifically for Healthix's
infrastructure. Our support model through Mahoosuc Solutions is
different from traditional vendors — we maintain a dedicated code
repository per customer, AI-managed with human review, so your team has
full visibility into every change made on your behalf. You're not buying
a black box; you're getting a fully supported, customer-scoped codebase.

Would you give me 20 minutes to show you where it landed? Your honest
reaction in December was useful — I'd value it again.

Aaron
[LinkedIn]

---

**Follow-up if no response by Thursday:**

Hi [Name] — wanted to make sure my note didn't get buried. Happy to
keep it to 15 minutes. Given you saw where it was in December, I think
the contrast would be worth your time.

---

### Email 2: Healthix CIO (Send Tuesday)

**To:** [CIO name] <[email]>
**Subject:** Re: our December call — want your honest technical read on where this landed

---

Hi [Name],

When we spoke in December I was showing you a Node.js prototype. I knew
at the time it wasn't where it needed to be architecturally.

Since then I rebuilt the platform completely. It's now 51 Java 21 /
Spring Boot 3.x microservices — event-driven, FHIR R4 native, 29
independent PostgreSQL databases with Liquibase schema management, and
11 Architecture Decision Records documenting the major design choices.
HIPAA compliance is architectural rather than bolted on: multi-tenant
isolation at the data layer, PHI audit logging, controlled cache TTL.

693 tests passing. 62 documented API endpoints (OpenAPI 3.0). 3-minute
Docker deployment.

We've built a RHEL 7 branch targeting Healthix's infrastructure
specifically. Our support model runs through a dedicated customer
repository managed by Mahoosuc Solutions — AI-managed code with
human-in-the-loop review on every change. Your team gets full
transparency into what's running, why each change was made, and a
complete audit trail. No vendor black box.

I'd value 20 minutes for your honest technical read on whether the
architecture holds up — specifically how it maps to how HIEs actually
surface data to health systems.

Aaron
[LinkedIn]

---

**Follow-up if no response by Thursday:**

Hi [Name] — just following up. If the December version set your
expectations low, that's fair — that's kind of the point. 15 minutes,
whatever works for your schedule.

---

## Track 2 — LinkedIn Comment Bank

**Rules:**
- Never mention HDIM or that you're building anything
- Add genuine insight, not validation
- 2-3 sentences max, reads like a practitioner not a marketer
- Post 1-2 per day, different targets on different days

---

### John Halamka (Mayo Clinic Platform)
*Engages on: FHIR interoperability, AI in healthcare, data governance, health equity*

**On FHIR adoption / interoperability:**
> "The implementation gap you're describing is real. From the HIE side we saw organizations technically capable of real-time exchange but organizationally structured around batch processing cycles — the tooling moved faster than the workflows and the incentives. The 'standards exist' problem is different from the 'standards are used' problem."

**On AI in clinical workflows:**
> "The governance piece is underweighted in most of these conversations. The model accuracy question gets all the attention, but the harder operational question is who owns the decision when the algorithm flags something a clinician disagrees with. That accountability structure doesn't exist yet at most health systems."

**On health data equity / access gaps:**
> "The data completeness issue compounds the equity issue in ways that are hard to see until you're downstream of it. Populations with lower care utilization have sparser records, which means quality measurement systematically underrepresents them — the denominator problem is as important as the numerator."

---

### Glen Tullman
*Engages on: health tech founder lessons, consumerism in healthcare, value-based care, Transcarent/Livongo retrospectives*

**On health tech founder lessons / what doesn't work:**
> "The 'if you build it they will come' failure mode is so consistent in health tech because the buyer and the user are almost never the same person. Employers buy it, employees have to use it, and the incentive structures don't align. That misalignment kills adoption even when the product is genuinely good."

**On consumerism in healthcare:**
> "The challenge with the consumer framing is that people don't want to be healthcare consumers — they want to not need healthcare. The products that win are the ones that either remove friction from an unavoidable interaction or make the unavoidable feel less like healthcare."

**On value-based care transition:**
> "The speed mismatch is the underrated barrier. Value-based contracts run on annual or multi-year timelines, but the data systems that would let you actually manage to those contracts often can't surface insights faster than a quarterly report. You can't manage what you can't see in time to act on it."

---

### Farzad Mostashari
*Engages on: ACOs, primary care policy, value-based care economics, health IT policy*

**On ACO performance / attribution challenges:**
> "Attribution is where so much value-based care strategy breaks down operationally. The theoretical care model assumes you know who your patients are continuously, but in practice you're working off retrospective claims data with a 3-6 month lag. You can't proactively manage a population you can only see in the rearview mirror."

**On primary care economics / sustainability:**
> "The sustainability question for independent primary care comes down to whether value-based contracts can close the gap fast enough. The problem is the transition period — practices have to absorb transformation costs while still running on fee-for-service revenue. The math doesn't work for most without bridge capital or risk-sharing arrangements that don't really exist yet."

**On health IT policy / interoperability mandates:**
> "The interesting thing about the TEFCA rollout is that the policy infrastructure is ahead of the operational readiness. The framework for nationwide exchange exists, but most organizations don't have the internal data architecture to take advantage of it yet. The mandate creates the forcing function; the gap is still the execution."

---

## Engagement Calendar (Week 1)

| Day | Action | Target | Status |
|-----|--------|--------|--------|
| Mon Mar 3 | Send CEO email | Healthix CEO | [ ] |
| Tue Mar 4 | Send CIO email | Healthix CIO | [ ] |
| Wed Mar 5 | LinkedIn comment #1 | Halamka | [ ] |
| Wed Mar 5 | LinkedIn comment #2 | Tullman | [ ] |
| Thu Mar 6 | Follow up if no response | CEO/CIO | [ ] |
| Thu Mar 6 | LinkedIn comment #3 | Mostashari | [ ] |
| Fri Mar 7 | LinkedIn comment #4 | Halamka or Tullman | [ ] |

---

## Tracking

| Contact | Type | Prior Contact | Sent | Responded | Call Scheduled | Notes |
|---------|------|---------------|------|-----------|----------------|-------|
| Healthix CEO | Re-engagement email | Dec 2025 call (prototype) | | | | |
| Healthix CIO | Re-engagement email | Dec 2025 call (prototype) | | | | |
| John Halamka | LinkedIn | None | | | | |
| Glen Tullman | LinkedIn | None | | | | |
| Farzad Mostashari | LinkedIn | None | | | | |

---

## Call Prep (When They Respond)

### 90-Second "What Changed" Answer

> "In December I showed you a Node.js prototype — I knew it wasn't ready. Since then I rebuilt the platform completely: 51 Java microservices, event-driven FHIR R4, 693 passing tests. The care gap detection now works in real-time against a live event stream, not a batch job."

### 20-Minute Demo Plan by Persona

**CEO:**
1. ROI calculator — customize to their health system size live on the call
2. Live care gap detection speed demo — event fires, gap surfaces in seconds

**CIO:**
1. Architecture diagram walkthrough + ADR overview
2. Swagger UI live endpoint demo (OpenAPI 3.0, 62 endpoints)
3. Multi-tenant isolation explanation — data layer, not application layer

### Explaining the Customer Repo Model (If Asked)

> "Mahoosuc Solutions maintains a dedicated GitHub repository for each
> customer. Your Healthix repo is scoped to your deployment — RHEL 7
> branch, your infrastructure. Every change goes through AI-assisted
> development with a human reviewing before anything ships. You can see
> every commit, every decision, every reason a change was made. That's
> the support model — not a ticket queue, a shared codebase."

**If they ask who reviews the AI output:**
> "I do, as the engineer of record. The AI handles the acceleration;
> a human owns the accountability. That's what 'humanity in the loop'
> means in practice."

### One Question to Close Each Call With

- **CEO:** "What would make this worth a pilot conversation with your quality team?"
- **CIO:** "Where does this break down from a data consumption standpoint — specifically how your member health systems pull from Healthix today?"
