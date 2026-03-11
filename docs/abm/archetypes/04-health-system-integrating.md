# Archetype 4: Health System — Integrating Post-M&A

## Target Persona
- **Title:** Chief Medical Officer (CMO) or Chief Information Officer (CIO)
- **Org Type:** Regional multi-hospital health system, 3-8 hospitals, 200-800 employed + affiliated physicians, post-acquisition integration underway
- **Reports To:** CEO, Health System Board of Directors
- **Measured On:** System-wide quality rankings (CMS Hospital Compare, Leapfrog), MIPS composite score, payer contract quality tier performance, physician satisfaction, CMS penalty avoidance, post-M&A integration milestones
- **What Keeps Them Up at Night:** "We acquired two physician groups and a community hospital last year. They're on Meditech and eClinicalWorks. We're on Epic. My quality team has been manually pulling reports from three systems for six months and we still can't produce a single system-wide diabetes control rate. Meanwhile, our Value-Based Purchasing penalties cost us $8.3 million last year because we couldn't see the problems across facilities until it was too late."

## Population Profile
- **Size Range:** 100,000-450,000 patients across 3-8 facilities and 40-120 practice sites
- **Payer Mix:** Commercial 42%, Medicare (FFS + MA) 33%, Medicaid 18%, Self-Pay/Other 7%
- **Chronic Condition Prevalence:** Hypertension 38%, Diabetes 22%, Depression 16%, Obesity 34%, COPD 9%, CHF 7%, Asthma 11%
- **Key Measures:** MIPS Quality (CMS-eligible clinician measures), Hospital VBP measures (AMI, HF, Pneumonia, Surgical), Payer contract quality tiers (HEDIS-based), system-wide preventive care benchmarks, readmission rates by facility, provider-level quality variation

## Demo Seed Parameters
- **Patient Count:** 3,600 (representative sample of 280,000 across 4 facilities)
- **Payer Distribution:** Commercial 42%, Medicare 33%, Medicaid 18%, Self-Pay 7%
- **Measures with Gap Rates:**
  | Measure | Current Rate | Target Rate | Open Gaps | Recent Closures |
  |---------|-------------|-------------|-----------|-----------------|
  | HbA1c Control (<8%) | 58% (system avg) | 70% | 14,200 | 67 (weekend) |
  | Controlling High Blood Pressure | 62% (system avg) | 72% | 11,800 | 43 (weekend) |
  | Breast Cancer Screening | 66% (system avg) | 76% | 9,400 | 0 |
  | Colorectal Cancer Screening | 59% (system avg) | 70% | 13,100 | 0 |
  | Depression Screening & Follow-Up | 51% (system avg) | 65% | 16,800 | 28 (Friday) |
  | Statin Therapy for CVD | 69% (system avg) | 78% | 7,200 | 34 (weekend refills) |
  | All-Cause Readmission | 16.2% (system avg) | <14.0% | 890 (at-risk) | 5 (avoided) |
  | MIPS Composite Score | 62/100 | 80/100 | N/A | N/A |
- **Provider Roster:** 340 employed physicians, 180 affiliated physicians, 4 EHR instances (Epic at flagship + 1 hospital, Meditech at acquired hospital, eClinicalWorks at acquired physician group, athenahealth at affiliated practices), 12 quality coordinators, 8 care managers, 3 quality analysts
- **Pre-Seeded "Wins":** First unified dashboard live — all 4 EHR systems normalized into single FHIR data lake over the weekend. Facility A (Epic flagship) and Facility C (Meditech) data reconciled: 340 patients appeared in both systems, duplicates merged. 67 HbA1c gaps closed from Friday labs across all facilities. 5 readmissions avoided through weekend discharge follow-up calls.
- **Facility-Level Variation (the story):**
  | Facility | EHR | Diabetes Control | BP Control | Depression Screening |
  |----------|-----|-----------------|------------|---------------------|
  | Facility A (Flagship) | Epic | 68% | 71% | 62% |
  | Facility B (Acquired Hospital) | Meditech | 43% | 52% | 34% |
  | Facility C (Community Hospital) | Epic | 61% | 65% | 55% |
  | Facility D (Physician Group) | eClinicalWorks | 52% | 58% | 41% |

## Monday Morning Scenario Script

### Opening (What They See)
Dr. Park logs in at 7:30 AM. For the first time, she sees a system-wide quality dashboard that spans all four facilities. The header reads: "280,000 patients | 4 facilities | 520 providers | Unified Quality View." A heat map shows each facility as a colored block — Facility A is mostly green, Facility B is mostly red, Facility C is mixed, Facility D is yellow. A system-wide quality composite score reads "62/100 MIPS" with a target line at 80.

### The Good News
"This is what you've been asking for since the acquisition closed 14 months ago. One dashboard. Four EHR systems. Every patient, every provider, every measure — unified. Your quality team has been spending 60% of their time pulling and reconciling reports from Epic, Meditech, and eCW. That ended this weekend. The FHIR integration pipeline normalized all four data sources into a single patient view. We even caught 340 duplicate patients who exist in both Facility A and Facility C — same patients, different medical record numbers, previously double-counted in your quality denominators."

### The Opportunity
"Now, look at Facility B." Click into the facility drill-down. "Diabetes control at 43%. That's 25 points below Facility A. This isn't a surprise — you suspected it — but this is the first time you can see it quantified, with the same measure definition applied to both facilities. No more 'Meditech calculates it differently' debates."

Click into the provider-level view within Facility B. "Dr. Liu at Facility B has a 61% diabetes control rate — comparable to Facility A's average. Dr. Stevens has 34%. Same patient panel complexity, same EHR, same formulary. The gap isn't the facility — it's specific providers who need support. And HDIM shows you exactly which patients on Dr. Stevens' panel have open HbA1c gaps and when their next appointments are."

### The "Aha Moment"
Click into the Payer Contract Impact view. "You have 6 commercial payer contracts with quality-tiered reimbursement. Anthem's contract pays a 3% bonus for hitting 70% diabetes control system-wide. You're at 58%. But look at this — if you bring Facility B from 43% to the system average of 58%, your system-wide rate jumps to 62%. If you bring Facility B to Facility A's level of 68%, your system-wide rate hits 67%. You're 3 points from the Anthem bonus. That's $2.1 million annually. And the patients who need the most help are concentrated in one facility with one set of providers."

Now toggle to the VBP penalty view. "Your hospitals absorbed $8.3 million in Value-Based Purchasing penalties last year. $5.1 million of that came from Facility B alone — mostly readmissions and process-of-care measures. With unified visibility, you can target Facility B's specific failure points instead of running system-wide initiatives that waste resources at facilities that are already performing."

### Talking Points at Each Screen
1. **[System Dashboard]** — "This is the view your board has been asking for. One number per measure. Across all facilities. Updated daily. No more conflicting reports from each facility's quality team."
2. **[Facility Heat Map]** — "Red isn't failure — it's opportunity. Facility B just needed visibility. They didn't know they were at 43% because Meditech doesn't calculate HbA1c control the same way Epic does. Now everyone is on the same scorecard."
3. **[Provider Breakdown]** — "520 providers. Ranked by measure. Same CQL logic applied regardless of which EHR they use. When Dr. Liu sees she's outperforming Dr. Stevens by 27 points on the same measure, the conversation shifts from 'the system is broken' to 'what's working and how do we spread it.'"
4. **[Payer Contract Impact]** — "Six payer contracts, $14 million in quality-contingent revenue. This view maps every open care gap to a dollar amount. Your CFO and your CMO are finally looking at the same screen."

### Objection Pre-emption
The demo naturally answers: (1) "We're investing in Epic enterprise-wide, just need to wait" — the 18-24 month EHR migration timeline means 2 years of blind spots; HDIM provides unified quality visibility now, and continues adding value even after full Epic rollout by incorporating claims and external data. (2) "Our IT team can build this" — the FHIR normalization across 4 EHRs, CQL measure execution, and provider-level attribution is 18+ months of custom development; HDIM deploys in weeks. (3) "We'll upset the acquired physicians" — the provider scorecard shows peer comparison, not punishment; physicians respond to transparency. (4) "We already have Health Catalyst" — Health Catalyst is a data warehouse, not a real-time CQL engine; it shows you the past, HDIM shows you today.

## Pain Hooks (By Trigger Type)

### Star Rating Drop
Reframe for health systems: "CMS Hospital Compare just updated and Facility B dropped from 3 to 2 stars. That's public. It affects physician recruiting, patient volume, and payer negotiations. HDIM gives you a unified view across all facilities so you can see problems developing at Facility B before they show up on a CMS scorecard 6 months later."

### Quality Report Published
"Your annual MIPS score came in at 62/100. The penalty threshold is 75. That's a 4% payment reduction across all Medicare Part B claims for 520 providers. At your Medicare volume, that's $3.8 million. HDIM's provider-level MIPS tracking shows which individual clinicians are pulling the composite down — and what each one needs to close to move the system score."

### Leadership Change
"New CEO is 90 days in, inherited 3 acquisitions they didn't make, and needs to report integration progress to the board. Quality integration is the hardest piece — it proves the clinical value of the acquisition, not just the financial engineering. Hand them a unified quality dashboard that spans all facilities. That's the integration story the board wants to hear."

### VBC Contract Announcement
"Your largest commercial payer just announced a shift to quality-tiered reimbursement. Tier 1 (top quality) gets 5% bonus. Tier 3 (lowest) gets 3% penalty. Facility B's performance is dragging your system average into Tier 2. With HDIM, you identify the exact measures and providers pulling you down, target interventions, and move the system into Tier 1 before the contract year begins."

### M&A / Expansion
"You're evaluating a 5th acquisition — a 60-physician multi-specialty group on Greenway. Your due diligence needs quality baseline data. HDIM can ingest their clinical data via FHIR, calculate quality measures before the LOI is signed, and show you the quality gap — and cost — of bringing them to system standards. That's M&A intelligence your competitors don't have."

### Conference Presentation
"You're presenting at ACHE on post-acquisition quality integration. Your case study: 4 EHR systems, 520 providers, unified quality measurement in 6 weeks. The room is full of health system executives who just completed or are planning acquisitions. They all face the same multi-EHR quality problem."

## Overlay Fields
| Field | What Changes Per Target |
|-------|------------------------|
| Organization Name | System dashboard header, facility names, payer contract references |
| Patient Volume | Seed data scales (3,600 seed = 280K base; adjust for 100K-450K). Facility counts and gap numbers scale proportionally |
| Priority Measures | Lead with their worst-performing facility's weakest measures. If post-M&A, emphasize cross-EHR unification. If MIPS-focused, lead with composite score |
| Geography | State-specific payer names (Anthem/BCBS/Aetna), regional quality benchmarks, local competing health systems for context |
| Opening Narrative | Tailored from dossier: "You acquired [Facility B] 14 months ago. Your board asked for a unified quality report in Q1. Here it is — and here's what it shows about where to invest." |

## Competitive Positioning
- **What They're Likely Using Today:** Epic Healthy Planet (for Epic-only facilities), Health Catalyst (data warehouse + analytics), internal IT team with SQL/Tableau dashboards, facility-specific quality reporting tools, manual Excel reconciliation across EHRs
- **Why HDIM Is Different:**
  - Multi-EHR quality unification: official CQL logic applied consistently across Epic, Cerner, Meditech, eCW, athena — Epic Healthy Planet only works with Epic data
  - Facility and provider-level benchmarking with case-mix normalization — apples-to-apples comparison across acquired entities
  - Payer contract quality mapping: ties every open care gap to a dollar amount across all quality-contingent contracts — Health Catalyst shows trends, HDIM shows financial impact
  - Deployment in weeks, not quarters — no 18-month data warehouse build; FHIR R4 native integration connects to existing EHR APIs
- **Displacement Messaging:** "Epic Healthy Planet is excellent — for Epic data. But you have Meditech at Facility B and eClinicalWorks at the physician group. Healthy Planet can't see those patients. Health Catalyst can warehouse the data, but building CQL measure logic on top of a warehouse is a 12-month custom project. HDIM connects to all four EHRs, executes official NCQA CQL specifications against FHIR-normalized data, and gives you a unified quality dashboard in weeks. Your Epic investment is protected — HDIM enhances it by adding the 40% of your patient population that Epic can't see."
