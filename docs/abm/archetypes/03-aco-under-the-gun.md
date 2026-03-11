# Archetype 3: ACO — Under the Gun

## Target Persona
- **Title:** Quality Director / Director of Population Health
- **Org Type:** MSSP ACO or ACO REACH participant, 30-120 independent practice sites
- **Reports To:** ACO Board of Governors, CMO, participating provider leadership
- **Measured On:** Quality gate pass/fail, shared savings distribution, benchmark performance, provider engagement rate, CMS Web Interface / eCQM submission accuracy
- **What Keeps Them Up at Night:** "We have 60 days until our quality reporting deadline. We're passing 8 out of 10 quality gates, but failing 2. If we miss the quality threshold, we forfeit $4.2 million in shared savings that 120 physicians are counting on. And half of them use different EHRs, so I can't even get a clean count of flu shots across the network."

## Population Profile
- **Size Range:** 30,000-75,000 attributed Medicare beneficiaries
- **Payer Mix:** 100% Traditional Medicare (attributed via ACO REACH or MSSP), but many patients also carry commercial or Medicaid coverage that generates data in other systems
- **Chronic Condition Prevalence:** Hypertension 48%, Diabetes 26%, CHF 10%, COPD 12%, Depression 17%, CKD 14%, Osteoporosis 9%
- **Key Measures:** Preventive Care — Flu Vaccination, Preventive Care — Colorectal Cancer Screening, Diabetes — HbA1c Control, Controlling High Blood Pressure, Depression Screening & Follow-Up, Statin Therapy for CVD, Tobacco Screening & Cessation, Falls Risk Screening, ACO-specific all-cause readmission, Patient Experience (CAHPS)

## Demo Seed Parameters
- **Patient Count:** 1,800 (representative sample of 52,000 attributed lives)
- **Payer Distribution:** Traditional Medicare 100% (dual-eligible subset 22%)
- **Measures with Gap Rates:**
  | Measure | Current Rate | Target Rate | Open Gaps | Recent Closures |
  |---------|-------------|-------------|-----------|-----------------|
  | Flu Vaccination | 61% | 70% | 1,404 | 87 (weekend pharmacy) |
  | Colorectal Cancer Screening | 63% | 72% | 1,190 | 0 |
  | HbA1c Control (<8%) | 59% | 68% | 1,612 | 22 (weekend labs) |
  | Controlling High Blood Pressure | 64% | 72% | 1,247 | 15 (weekend clinic) |
  | Depression Screening & Follow-Up | 48% | 60% | 2,314 | 31 (Friday screenings) |
  | Statin Therapy for CVD | 72% | 80% | 924 | 18 (weekend refills) |
  | Tobacco Screening & Cessation | 71% | 82% | 1,073 | 42 (nurse visits Friday) |
  | Falls Risk Screening | 55% | 65% | 1,890 | 0 |
  | All-Cause Readmission | 15.1% | <14.0% | 287 (at-risk) | 2 (avoided) |
  | CAHPS Composite | 78th %ile | 85th %ile | N/A | N/A |
- **Provider Roster:** 120 PCPs across 34 practice sites, 4 EHR systems (Epic 45%, Cerner 25%, athenahealth 20%, eClinicalWorks 10%), 6 ACO care coordinators, 2 quality analysts, 1 pharmacist
- **Pre-Seeded "Wins":** 87 flu vaccination gaps closed via weekend pharmacy immunization events (captured from pharmacy claims feed). 42 tobacco screenings completed Friday afternoon (nurse-driven workflow at 3 high-volume sites). 22 HbA1c results returned from Thursday lab draws. 2 readmissions avoided — patients discharged Friday received Saturday follow-up calls from ACO care coordinators.

## Monday Morning Scenario Script

### Opening (What They See)
Marcus logs in at 8:15 AM. The Quality Gate Scorecard is front and center: a 10-measure grid showing 8 green checkmarks and 2 red Xs. A countdown timer reads "60 days to reporting deadline." Below the scorecard, a trend line shows the two failing measures — Flu Vaccination (61%, need 70%) and Falls Risk Screening (55%, need 65%) — with projected trajectories based on current closure rates. The flu line is trending up; falls risk is flat.

### The Good News
"Start with the wins. Your pharmacy immunization partnership is paying off — 87 flu shots captured over the weekend from CVS and Walgreens data flowing in automatically. Your flu rate just moved from 60.4% to 61.1%. At this pace, you'll cross the 70% threshold by day 45. That's 15 days of cushion. And look here — 42 tobacco screenings from Friday. Your nurse-driven workflow at the Elm Street, Oak Park, and River Road sites is working. Tobacco screening went from 70.2% to 71.3% in one day."

### The Opportunity
"Here's the problem: Falls Risk Screening is stuck at 55% and it's not moving. You need 65% — that's 1,890 open gaps to close in 60 days. But here's what HDIM shows you that your current system can't." Click into the provider-level breakdown. "Dr. Morrison's practice in the Epic cluster has a 74% falls screening rate — she built a standing order set. Dr. Keller's practice on eClinicalWorks has 31%. Same Medicare population. The difference is workflow, not patient complexity."

Click into the cross-EHR aggregation view. "This is why your current spreadsheet approach is failing. You're pulling reports from 4 different EHRs, each calculating falls screening differently. Epic counts the screening tool. eCW counts a diagnosis code. athena counts a questionnaire. HDIM runs the official CQL logic against FHIR-normalized data from all four systems. One number. One truth."

### The "Aha Moment"
Click into the Shared Savings Impact Calculator. The screen shows: "Current quality gate status: 8/10 passing. Shared savings at risk: $4.2 million." Toggle the flu vaccination rate to 70%. One gate turns green: 9/10. Toggle falls risk to 65%. Both gates green: 10/10. The savings amount unlocks: "$4.2 million distributed to 120 participating providers."

"Marcus, this is the slide for your Thursday board call. Two measures. 1,404 flu shots and 1,890 falls screenings. 60 days. If you close these gaps, 120 physicians split $4.2 million. Here's the outreach list for each measure, broken down by practice site, with the patients who have appointments in the next 2 weeks at the top."

### Talking Points at Each Screen
1. **[Quality Gate Scorecard]** — "This replaces the quarterly Excel update your board has been getting. It's live. Every provider can see their own gates, their own patients, their own contribution to the shared savings pool."
2. **[Care Gap List]** — "1,404 patients need flu shots. But 380 of them have primary care appointments in the next 14 days. That's your standing order list. Print it, hand it to the MAs, flu shots happen at check-in. No extra visits needed."
3. **[Provider Breakdown]** — "Dr. Morrison is at 74%. Dr. Keller is at 31%. When you show independent physicians their own data compared to peers, behavior changes. This isn't a report card — it's a conversation starter."
4. **[Cross-EHR View]** — "Four EHRs. One quality number. This is what your ACO was supposed to deliver from day one. One source of truth that doesn't depend on which EHR a practice chose in 2015."

### Objection Pre-emption
The demo naturally answers: (1) "Our independent docs won't engage" — the provider-level scorecard with peer comparison drives engagement through transparency, not mandates; 85% provider adoption when insights are embedded in their EHR. (2) "We're mid-reporting year, it's too late" — the 60-day countdown and prioritized outreach list show it's not too late, but every week of delay costs gap closures. (3) "We already pull reports from each EHR" — the cross-EHR view shows why that approach produces different answers than the official CQL logic. (4) "CMS is changing the reporting requirements" — HDIM's CQL engine updates to APP Plus measures (growing from 6 to 8 to 9 to 11 by 2028) as CMS publishes them.

## Pain Hooks (By Trigger Type)

### Star Rating Drop
Reframe for ACOs: "CMS published your ACO quality results and you're below the shared savings threshold on 2 gates. Your participating providers are asking where their bonus checks are. HDIM shows you exactly which measures failed, by how much, and what the closure plan looks like for next year — so you can walk into the provider meeting with a solution, not an apology."

### Quality Report Published
"Your ACO's public quality results just landed on the CMS Compare tool. Two measures are below the 50th percentile. Your referring physicians see this. Your patients' families see this. HDIM gives you a real-time quality position so you're never surprised by your own public data again."

### Leadership Change
"New ACO Medical Director starts in 30 days. They want to understand quality performance across all 34 practice sites before their first board meeting. Hand them a login. Four EHRs, 120 providers, 52,000 patients — one dashboard. They'll know more about your quality position on day one than the previous Medical Director knew after a year."

### VBC Contract Announcement
"CMS just announced ACO REACH Model changes: APP Plus is expanding from 6 to 8 measures this year, 9 next year, 11 by 2028. Each new measure is a new gate to pass. Your spreadsheet-and-EHR-report approach barely handles 6 measures across 4 EHRs. How will it handle 11? HDIM's CQL engine adds measures as CMS publishes the specifications — no custom development required."

### M&A / Expansion
"Three new practice sites want to join your ACO — they bring 8,000 attributed lives and a fifth EHR (Meditech). Your quality analysts are already maxed out reconciling 4 EHRs. HDIM adds the fifth data source, recalculates all measures across the expanded network, and shows you the quality impact of the new practices before you sign the participation agreement."

### Conference Presentation
"You're presenting at NAACOS on multi-EHR quality aggregation. Your case study shows how you unified quality measurement across Epic, Cerner, athenahealth, and eClinicalWorks — with one CQL engine producing one number per measure. Every ACO Quality Director in the room has the same problem. They'll want to know your vendor."

## Overlay Fields
| Field | What Changes Per Target |
|-------|------------------------|
| Organization Name | Quality Gate Scorecard header, provider scorecards, board report branding |
| Patient Volume | Seed data scales (1,800 seed = 52K base; adjust for 30K-75K). Gap counts and savings dollars scale proportionally |
| Priority Measures | Lead with their failing quality gates. If flu + falls, use default. If diabetes + depression, reorder the narrative |
| Geography | State-specific immunization registries, regional pharmacy partners, local health department flu clinic schedules |
| Opening Narrative | Tailored from dossier: "You have 60 days until the CMS reporting deadline. Two quality gates are failing. $4.2 million in shared savings depends on 1,404 flu shots and 1,890 falls screenings. Here's the plan." |

## Competitive Positioning
- **What They're Likely Using Today:** Arcadia (population health analytics), Innovaccer (data activation platform), manual EHR reporting + Excel reconciliation, legacy quality vendor for CMS Web Interface submission
- **Why HDIM Is Different:**
  - Cross-EHR CQL execution: one measure calculation across Epic, Cerner, athena, eCW, Meditech — Arcadia aggregates data but doesn't execute official CQL specifications
  - Provider-level quality scorecards with peer comparison — Innovaccer focuses on population analytics, not provider-level accountability
  - Real-time quality gate tracking with shared savings impact — most ACO tools show measures in isolation, not tied to financial outcomes
  - APP Plus ready: as CMS expands from 6 to 11 measures by 2028 and transitions from CMS Web Interface to eCQM reporting, HDIM's CQL engine updates measure specifications without custom development
- **Displacement Messaging:** "Arcadia gives you a data lake. Innovaccer gives you analytics. Neither one executes the official NCQA CQL specifications across your 4 EHR systems. When CMS audits your quality submission, you need a platform that calculates measures the same way CMS does — not one that approximates. And as APP Plus grows from 6 to 11 measures and the CMS Web Interface sunsets, your quality infrastructure needs to be eCQM-native. That's what HDIM was built for."
